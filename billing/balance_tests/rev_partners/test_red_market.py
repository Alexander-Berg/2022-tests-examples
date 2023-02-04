# -*- coding: utf-8 -*-
from decimal import Decimal as D

import datetime

from balance import constants as const
from balance import contractpage, mapper
from balance import muzzle_util as ut
from balance import scheme
from balance.constants import *
from balance.processors.month_proc import create_act_accounter
from cluster_tools import generate_partner_acts as gpa
from tests import object_builder as ob
from tests.balance_tests.rev_partners.common import (
    get_pa,
    red_market_ag_terms,
)


def get_order_act(acts):
    order_services = (const.ServiceId.RED_SRV,)
    a = [
        a
        for a in acts
        if [at for at in a.rows if at.order.product.engine_id in order_services]
    ]
    # TODO выкинуть ассерты из get метода
    assert len(a) == 1, "Zero or more than one order act {}".format(a)
    return a[-1]


def gen_red_market_contract(
    session,
    postpay=False,
    personal_account=False,
    con_func=None,
    client=None,
    dt=None,
    finish_dt=None,
):

    from billing.contract_iface import contract_meta

    contract = mapper.Contract(ctype=contract_meta.ContractTypes(type="GENERAL"))
    session.add(contract)
    if not client:
        client = ob.ClientBuilder().build(session).obj
    contract.client = client
    contract.person = (
        ob.PersonBuilder(client=contract.client, type="sw_yt").build(session).obj
    )
    if not dt:
        dt = datetime.datetime(2018, 9, 1)
    contract.col0.dt = dt

    contract.col0.firm = 7
    contract.col0.manager_code = 1122
    contract.col0.commission = 9
    contract.col0.payment_type = (
        POSTPAY_PAYMENT_TYPE if postpay else PREPAY_PAYMENT_TYPE
    )
    contract.col0.personal_account = 1 if personal_account else 0
    contract.col0.currency = 840
    contract.col0.payment_term = 15
    if postpay:
        contract.col0.partner_credit = 1
    contract.col0.is_signed = datetime.datetime(2014, 1, 4)

    if finish_dt:
        contract.col0.finish_dt = finish_dt

    if con_func:
        con_func(contract)

    contract.external_id = contract.create_new_eid()

    cp = contractpage.ContractPage(session, contract.id)
    cp.create_personal_accounts()
    session.flush()

    return contract


def prepare_contracts(session):
    contract0 = gen_red_market_contract(
        session,
        postpay=True,
        personal_account=True,
        con_func=red_market_ag_terms,
        dt=datetime.datetime(2018, 9, 1),
        finish_dt=datetime.datetime(2018, 9, 16),
    )
    get_pa(session, contract0)

    contract1 = gen_red_market_contract(
        session,
        postpay=True,
        personal_account=True,
        con_func=red_market_ag_terms,
        dt=datetime.datetime(2018, 9, 16),
        client=contract0.client,
    )
    get_pa(session, contract1)
    return contract0, contract1


def prepare_completions():
    return [
        ut.Struct(
            dt=datetime.datetime(2018, 9, 1), amount=D("10.5544"), product_id=509146
        ),
        ut.Struct(dt=datetime.datetime(2018, 9, 15), amount=D("1"), product_id=509146),
        ut.Struct(
            dt=datetime.datetime(2018, 9, 16), amount=D("11.7779"), product_id=509146
        ),
        ut.Struct(dt=datetime.datetime(2018, 9, 30), amount=D("1"), product_id=509146),
    ]


def make_completions(session, client, compl_struct):
    # compl_struct = ut.Struct(dt, amount, product_id)
    for compl in compl_struct:
        session.execute(
            scheme.partner_product_completion.insert(
                {
                    "dt": compl.dt,
                    "client_id": client.id,
                    "service_id": const.ServiceId.RED_SRV,
                    "amount": compl.amount,
                    "product_id": compl.product_id,
                }
            )
        )


def filter_and_group_completions_for_contract(completions, contract):
    cs = contract.current_signed()
    from_dt = cs.dt
    to_dt = getattr(cs, "finish_dt", None)
    filtered = filter(
        lambda compl: (compl.dt >= from_dt) and (not to_dt or compl.dt < to_dt),
        completions,
    )
    return [
        (product_id, ut.round00(sum([sg.amount for sg in gr])))
        for product_id, gr in ut.groupby(filtered, key=lambda compl: compl.product_id)
    ]


def execute_test(contract, same_client_contract=None):
    first_month = datetime.datetime(2018, 9, 1)
    session = contract.session
    act_month = mapper.ActMonth(for_month=first_month)
    compls = prepare_completions()
    make_completions(session, contract.client, compls)

    contracts = filter(None, [contract, same_client_contract])
    for contract in contracts:
        gpa.RevPartnerGenerator(contract).generate(act_month)
        session.flush()

        export_object = (
            session.query(mapper.Export)
            .filter(
                (mapper.Export.type == "MONTH_PROC")
                & (mapper.Export.classname == "Client")
                & (mapper.Export.state == ExportState.enqueued)
                & (mapper.Export.object_id == contract.client.id)
            )
            .one()
        )
        # без рефреша не подтягивает реальные данные в объект
        session.refresh(export_object)

        split_act_creation = session.config.get("ACT_SPLIT_ACT_CREATION", False)
        s_input = ut.Struct(export_object.input)
        act_accounter = create_act_accounter(
            contract.client, s_input, session, split_act_creation=split_act_creation
        )
        acts = act_accounter.do(skip_cut_agava=False)
        session.flush()
        export_object.skip_export()
        session.flush()

        completions = filter_and_group_completions_for_contract(compls, contract)
        order_act = get_order_act(acts)
        assert len(order_act.rows) == len(completions)
        assert sum(r.act_qty for r in order_act.rows) == sum(
            x[1] for x in list(completions)
        )


def test_red_market_same_month_contracts_gen_acts(session):
    contract, same_client_contract = prepare_contracts(session)
    execute_test(contract, same_client_contract=same_client_contract)
