# -*- coding: utf-8 -*-

from collections import defaultdict
from decimal import Decimal as D

import datetime
from dateutil.relativedelta import relativedelta

from balance import constants as const
from balance import mapper
from balance import muzzle_util as ut
from balance import scheme
from balance.constants import *
from balance.processors.month_proc import create_act_accounter
from cluster_tools import generate_partner_acts as gpa
from tests.balance_tests.rev_partners.common import (
    blue_market_srv_terms,
    gen_contract,
)
from tests.balance_tests.rev_partners.test_taxi import get_order_act

product_mapping = {
    "fee": 508942,
    "sorting": 511173,
}


def get_order_act(acts):
    order_services = [
        const.ServiceId.BLUE_SRV,
    ]
    a = [
        a
        for a in acts
        if [at for at in a.rows if at.order.product.engine_id in order_services]
    ]
    assert len(a) == 1, "Zero or more than one order act {}".format(a)
    return a[-1]


def prepare_contract(session, postpay=False, con_func=None):
    """Продукты в открутках. предоплата"""
    if not con_func:
        con_func = lambda c: blue_market_srv_terms(c)
    contract = gen_contract(
        session, postpay=postpay, personal_account=True, con_func=con_func
    )

    return contract


def get_tlog_timeline_notch(obj):
    session = obj.session
    notch = (
        session.query(mapper.TLogTimeline)
        .filter(mapper.TLogTimeline.object_id == obj.id)
        .filter(mapper.TLogTimeline.classname == obj.__class__.__name__)
        .one()
    )
    return notch


def prepare_compls_pp(border_dt):
    before_border = border_dt - relativedelta(days=3)
    return [
        ut.Struct(dt=before_border, amount=D("12.55"), product_id=508942),
        ut.Struct(dt=before_border, amount=D("14.65"), product_id=511173),
        ut.Struct(dt=before_border, amount=D("17.33"), product_id=511173),
        ut.Struct(dt=border_dt, amount=D("6666666"), product_id=508942),
        ut.Struct(
            dt=border_dt + relativedelta(days=3), amount=D("6666666"), product_id=511173
        ),
    ]


def make_completions_pp(session, client, compl_struct):
    # compl_struct = ut.Struct(dt, amount, product_id)
    for compl in compl_struct:
        session.execute(
            scheme.partner_product_completion.insert(
                {
                    "dt": compl.dt,
                    "client_id": client.id,
                    "service_id": const.ServiceId.BLUE_SRV,
                    "amount": compl.amount,
                    "product_id": compl.product_id,
                }
            )
        )


def prepare_compls_tlog(border_dt):
    before_border = border_dt - relativedelta(days=3)
    return [
        ut.Struct(dt=border_dt, amount=D("19.75"), type="fee", last_transaction_id=50),
        ut.Struct(
            dt=border_dt, amount=D("21.34"), type="sorting", last_transaction_id=60
        ),
        ut.Struct(
            dt=border_dt + relativedelta(days=3),
            amount=D("23.75"),
            type="sorting",
            last_transaction_id=70,
        ),
        ut.Struct(
            dt=before_border, amount=D("23.49"), type="fee", last_transaction_id=30
        ),
        ut.Struct(
            dt=before_border, amount=D("78.12"), type="sorting", last_transaction_id=100
        ),
    ]


def make_completions_tlog(session, client, compl_struct):
    # compl_struct = ut.Struct(dt, amount, product_id, transaction_id)
    for compl in compl_struct:
        session.execute(
            scheme.partner_stat_aggr_tlog.insert(
                {
                    "completion_src": "blue_market_aggr_tlog",
                    "src_dt": datetime.datetime(2020, 1, 1),
                    "dt": compl.dt,
                    "client_id": client.id,
                    "currency": "RUB",
                    "service_id": const.ServiceId.BLUE_SRV,
                    "amount": compl.amount,
                    "type": compl.type,
                    "last_transaction_id": compl.last_transaction_id,
                    "nds": 1,
                }
            )
        )


def filter_and_group_completions_for_contract(compls_pp, compls_tlog, border_dt):
    product_sum = defaultdict(lambda: D("0"))
    for row in compls_pp:
        if row.dt < border_dt:
            product_sum[row.product_id] += row.amount

    max_last_transaction_id = -1
    for row in compls_tlog:
        product_sum[product_mapping[row.type]] += row.amount
        max_last_transaction_id = max(max_last_transaction_id, row.last_transaction_id)

    return max_last_transaction_id, product_sum


def do_test(session, postpay=False):
    COMPL_TLOG_START_DATE_STR = "2020-03-20"
    COMPL_TLOG_START_DATE = datetime.datetime.strptime(
        COMPL_TLOG_START_DATE_STR, "%Y-%m-%d"
    )
    session.config.__dict__["TLOG_BLUE_MARKET_CONFIG"] = {
        "completion-tlog-start-date": COMPL_TLOG_START_DATE_STR
    }
    contract = prepare_contract(session, postpay)
    act_month = mapper.ActMonth(for_month=COMPL_TLOG_START_DATE)

    compls_pp = prepare_compls_pp(COMPL_TLOG_START_DATE)
    make_completions_pp(session, contract.client, compls_pp)

    compls_tlog = prepare_compls_tlog(COMPL_TLOG_START_DATE)
    make_completions_tlog(session, contract.client, compls_tlog)

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

    (
        expected_max_last_transaction_id,
        product_sum,
    ) = filter_and_group_completions_for_contract(
        compls_pp, compls_tlog, COMPL_TLOG_START_DATE
    )
    expected_sums = frozenset(
        (product_id, amount) for product_id, amount in product_sum.items()
    )
    order_act = get_order_act(acts)
    real_sums = frozenset(
        (at.order.product.id, at.act_qty.as_decimal()) for at in order_act.rows
    )
    assert expected_sums == real_sums

    pa = order_act.invoice

    timeline_notch = get_tlog_timeline_notch(pa)
    assert expected_max_last_transaction_id == timeline_notch.last_transaction_id


def test(session):
    do_test(session, postpay=True)
