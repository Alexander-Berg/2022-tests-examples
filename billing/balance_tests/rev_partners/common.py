# -*- coding: utf-8 -*-

from decimal import Decimal as D
import uuid
import datetime
import mock
import pytest
import sqlalchemy as sa
from datetime import timedelta

import balance.actions.acts as a_a
from balance import constants as const
from balance import contractpage, mapper
from balance import muzzle_util as ut
from balance import reverse_partners as rp
from balance.constants import *
from balance.providers.personal_acc_manager import PersonalAccountManager
from butils import logger
from tests import object_builder as ob

ADFOX_DEFAULT_SCALE_CODE = u"test_default_scale"
ADFOX_COEFFICIENT_SCALE_CODE = u"test_shows_coefficient_scale"
ADFOX_COST_SCALE_CODE = u"test_qty_cost_scale"
TEST_ADFOX_COST_PRODUCT_ID = 505177
TEST_ADFOX_DEFAULT_PRODUCT_ID = 504400
TEST_ADFOX_MAIN_PRODUCT_WITH_COEFFICIENT = 505170
TEST_ADFOX_DISCOUNT_PRODUCT_ID = 508212

TAXI_RF_111_MIN_ACT_PRODUCT = 666503352
TAXI_RF_128_MIN_ACT_PRODUCT = 666505142
TAXI_RF_MIN_ACT_AMOUNT = 1
TAXI_MIN_ACT_PARAMS = {
    13: {
        "RUB": {
            "products": {
                111: TAXI_RF_111_MIN_ACT_PRODUCT,
                128: TAXI_RF_128_MIN_ACT_PRODUCT,
            },
            "amount": TAXI_RF_MIN_ACT_AMOUNT,
        }
    }
}
TAXI_TLOG_MIGRATION_PARAMS = {"taxi_use_tlog_completions": 1}
TAXI_COMMISSION_SERVICE_CODE = "YANDEX_SERVICE"

log = logger.get_logger("test_revpartners")


def listget(lst, index, default=None):
    try:
        return lst[index]
    except IndexError:
        return default


# Допиливать по мере надобности, сейчас эмулирует только задачу в статусе new
class ItaskMock(mock.MagicMock):
    status = "new"
    available_actions = ["open"]
    inst_dt = ut.month_first_day(datetime.datetime.now())


class MncloseMock(mock.MagicMock):
    _itasks = list()

    def itask(self, name):
        needed_itasks = filter(lambda it: it.name == name, self._itasks)
        if len(needed_itasks) == 1:
            return needed_itasks[0]
        elif len(needed_itasks) == 0:
            itask = ItaskMock()
            itask.name = name
            self._itasks.append(itask)
            return itask
        else:
            raise Exception('Too many itasks with name "{name}"'.format(name=name))

    def open(self, task):
        task.status = "opened"


@pytest.fixture
def mnclose():
    yield MncloseMock()


def clean_monthproc(contract):
    exp = contract.client.exports["MONTH_PROC"]
    contract.session.refresh(exp)
    contract.session.delete(exp)
    contract.session.flush()


def generic_terms(contract, services):
    services = set(services)
    contract.col0.services = services
    contract.col0.firm = 1


def taxi_terms(contract):
    contract.col0.partner_commission_type = 2  # commission_pct mode
    contract.col0.partner_commission_pct = 8
    contract.col0.service_min_cost = 0
    contract.col0.firm = 13
    contract.col0.services = {const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_PAYMENT}
    return contract


def corp_taxi_terms(contract, min_cost=0, no_min_cost_wo_service=True):
    contract.col0.firm = 13
    contract.col0.dt = datetime.datetime(2019, 4, 1)
    contract.col0.no_min_cost_wo_service = int(no_min_cost_wo_service)
    if contract.col0.payment_type == POSTPAY_PAYMENT_TYPE:
        contract.col0.service_min_cost = min_cost
    elif contract.col0.payment_type == PREPAY_PAYMENT_TYPE:
        contract.col0.advance_payment_sum = min_cost
    contract.col0.services = {
        const.ServiceId.TAXI_CORP,
        const.ServiceId.TAXI_CORP_CLIENTS,
    }
    return contract


def red_market_ag_terms(contract):
    contract.col0.firm = 7
    contract.col0.services = {const.ServiceId.RED_SRV}
    return contract


def blue_market_srv_terms(contract):
    contract.col0.firm = 111
    contract.col0.services = {const.ServiceId.BLUE_SRV}
    return contract


def taxi_uber_terms(contract):
    contract = taxi_terms(contract)
    contract.col0.services = {
        const.ServiceId.TAXI_CASH,
        const.ServiceId.TAXI_PAYMENT,
        const.ServiceId.UBER_PAYMENT,
        const.ServiceId.UBER_PAYMENT_ROAMING,
    }

    return contract


def arguments_decorator(**kw):
    def decorator(func):
        def wrapped(*args, **kwargs):
            kwargs.update(kw)
            return func(*args, **kwargs)

        return wrapped

    return decorator


def update_contract_collateral(contract, **kwargs):
    for k, v in kwargs.items():
        setattr(contract.col0, k, v)


def taxi_ccard_combo_contract(contract, service_min_cost):
    contract = taxi_terms(contract)
    if contract.col0.payment_type == POSTPAY_PAYMENT_TYPE:
        contract.col0.service_min_cost = service_min_cost
    else:
        contract.col0.advance_payment_sum = service_min_cost
    contract.col0.services = {
        const.ServiceId.TAXI_CASH,
        const.ServiceId.TAXI_PAYMENT,
        const.ServiceId.TAXI_CARD,
    }
    return contract


def taxi_prepay_only_commission_contract(contract, service_min_cost):
    contract = taxi_terms(contract)
    contract.col0.advance_payment_sum = service_min_cost
    contract.col0.services = {const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD}
    return contract


def taxi_terms_apx(contract, service_min_cost):
    contract = taxi_terms(contract)
    if contract.col0.payment_type == POSTPAY_PAYMENT_TYPE:
        contract.col0.service_min_cost = service_min_cost
    else:
        contract.col0.advance_payment_sum = service_min_cost
    return contract


def multiship_terms(contract):
    contract.col0.services = {
        const.ServiceId.MULTISHIP_DELIVERY,
        const.ServiceId.MULTISHIP_PAYMENT,
    }


def dsp_terms(contract, service_min_cost=0, test_period_duration=0):
    # Contract date >= date when DSP and APX products exists
    contract.col0.dt = datetime.datetime(2015, 1, 1)
    contract.col0.is_signed = datetime.datetime(2015, 1, 4)
    contract.col0.services = {const.ServiceId.DSP}
    contract.col0.service_min_cost = service_min_cost
    contract.col0.test_period_duration = test_period_duration


def adfox_contract_terms(contract, resident=1):
    contract.col0.services = {const.ServiceId.ADFOX}
    contract.col0.adfox_products = {
        TEST_ADFOX_MAIN_PRODUCT_WITH_COEFFICIENT: {
            u"account": u"",
            u"scale": ADFOX_COEFFICIENT_SCALE_CODE,
        },
        TEST_ADFOX_COST_PRODUCT_ID: {u"account": u"", u"scale": ADFOX_COST_SCALE_CODE},
        TEST_ADFOX_DEFAULT_PRODUCT_ID: {
            u"account": u"",
            u"scale": ADFOX_DEFAULT_SCALE_CODE,
        },
    }

    if not resident:
        contract.col0.deal_passport = datetime.datetime(2015, 1, 1)
        contract.client = ob.ClientBuilder().build(contract.session).obj
        contract.person = (
            ob.PersonBuilder(client=contract.client, type="yt")
            .build(contract.session)
            .obj
        )


def adfox_new_offer_terms(contract):
    cont = adfox_contract_terms(contract)
    cont.col0.vip_client = 1
    cont.col0.discount_product_id = TEST_ADFOX_DISCOUNT_PRODUCT_ID
    return cont


def zaxi_terms(contract, link_contract_id):
    contract.col0.services = {const.ServiceId.ZAXI}
    contract.col0.link_contract_id = link_contract_id


def zaxi_corp_terms(contract):
    contract.col0.services = {const.ServiceId.ZAXI}


# ========== from ReversePartnersBase ======================
def check_qty(act, completions):
    for r in act.rows:
        expect_qty = [x[1] for x in completions if r.order.product.id in x][0]
        assert r.act_qty == expect_qty


def check_sum(act, completions, discount_pct=D(0), price=D(1)):
    for r in act.rows:
        expect_sum = [
            (x[1] - x[1] * discount_pct * D("0.01")) * price
            for x in completions
            if r.order.product.id in x
        ][0]
        assert r.act_sum == expect_sum


def _create_scale(
    session, scale_code, points, namespace="adfox", y_unit_id=None, x_unit_id=799
):
    test_scale = mapper.StaircaseScale(
        namespace=namespace, code=scale_code, x_unit_id=x_unit_id, y_unit_id=y_unit_id
    )
    for (x, y) in points:
        session.add(
            mapper.ScalePoint(
                namespace=namespace,
                scale_code=scale_code,
                start_dt=datetime.datetime(2015, 1, 1),
                x=x,
                y=y,
            )
        )
    session.merge(test_scale)
    session.flush()


def gen_contract(
    session,
    postpay=False,
    personal_account=False,
    con_func=None,
    begin_dt=datetime.datetime(2014, 1, 1),
    finish_dt=None,
    client=None,
):
    from billing.contract_iface import contract_meta

    contract = mapper.Contract(ctype=contract_meta.ContractTypes(type="GENERAL"))
    session.add(contract)
    contract.client = client or ob.ClientBuilder().build(session).obj
    contract.person = (
        ob.PersonBuilder(client=contract.client, type="ur").build(session).obj
    )
    contract.col0.dt = begin_dt
    contract.col0.finish_dt = finish_dt

    contract.col0.firm = 1
    contract.col0.manager_code = 1122
    contract.col0.commission = 0
    contract.col0.payment_type = (
        POSTPAY_PAYMENT_TYPE if postpay else PREPAY_PAYMENT_TYPE
    )
    contract.col0.personal_account = 1 if personal_account else 0
    contract.col0.currency = 810
    contract.external_id = contract.create_new_eid()
    if postpay:
        contract.col0.partner_credit = 1
    contract.col0.is_signed = begin_dt + timedelta(3)

    if con_func:
        con_func(contract)

    contract.external_id = contract.create_new_eid()

    cp = contractpage.ContractPage(session, contract.id)
    cp.create_personal_accounts()
    session.flush()

    return contract


# создание данных tpts, так чтобы транзакции были как в отрезке закрытия, так и вне его
def get_multiple_tpts_data(begin_dt, end_dt, act_dt):
    return [
        {"dt": begin_dt - timedelta(1), "yandex_reward": 1},
        {"dt": begin_dt + timedelta(1), "yandex_reward": 2},
        {"dt": act_dt - timedelta(1), "yandex_reward": 4},
        {"dt": act_dt + timedelta(1), "yandex_reward": 8},
        {"dt": end_dt - timedelta(1), "yandex_reward": 16},
    ]


def get_invoice_eid(contract, service_code):
    return (
        PersonalAccountManager(contract.session)
        .for_contract(contract)
        .for_service_code(service_code)
        .get(auto_create=False)
        .external_id
    )


def gen_tpts_for_contract(contract, tpts=()):
    session = contract.session
    with session.begin():
        for item in tpts:
            invoice_eid = None
            if "service_code" in item:
                invoice_eid = get_invoice_eid(contract, item.get("service_code"))

            next_id = session.execute(
                'select bo.s_request_order_id.nextval from dual'
            ).scalar()
            session.add(
                mapper.ThirdPartyTransaction(
                    id=next_id,
                    service_id=item.get("service_id"),
                    product_id=item.get("product_id"),
                    contract_id=contract.id,
                    invoice_eid=invoice_eid,
                    amount=item.get("amount", 100),
                    yandex_reward=item.get("yandex_reward", 10),
                    dt=item.get("dt", contract.col0.dt + timedelta(days=1)),
                    paysys_type_cc=item.get("paysys_type_cc"),
                    payment_type=item.get("payment_type"),
                    transaction_type=item.get("transaction_type", "payment"),
                )
            )

def gen_actotron_rows_for_contract(contract, rows=()):
    session = contract.session
    with session.begin():
        for row in rows:
            session.add(
                mapper.ActOTronActRows(
                    act_row_id=str(uuid.uuid4()),
                    contract_id=contract.id,
                    client_id=contract.client.id,
                    **row
                )
            )


def generate_acts(contract, act_month, dps, invoices, with_begin_dt=False):
    return a_a.ActAccounter(
        contract.client, act_month, dps=dps, invoices=invoices, force=1
    ).do()


def get_pa(session, contract, service_code=None):
    paysys = rp.get_paysys(contract, const.ServiceId.TAXI_CASH)
    return (
        PersonalAccountManager(session)
        .for_contract(contract)
        .for_paysys(paysys)
        .for_service_code(service_code)
        .get(auto_create=False)
    )


def gen_acts(rpc):
    res = rpc.process_and_enqueue_act()
    assert len(res) == 2
    acts = generate_acts(rpc.contract, rpc.act_month, dps=res[0], invoices=res[1])
    return acts


# ========== end of ReversePartnersBase ============


def real_completions_aggr(
    contract, on_dt, commission_type, from_dt, service_id, completions
):
    """
    compl_format:
      [(service_id, commission_sum, order_type, dt, promocode_sum, subsidy_sum)]
    Открутки группируются по order_type, в реальном коде группировка идет по продуктам,
    из-за этого в тестах возможно использовать только уникальные по продуктам order_type"""
    if not from_dt:
        from_dt = datetime.datetime(1999, 1, 1)
    completions = filter(
        lambda x: x[0] == service_id and on_dt > x[3] >= from_dt, completions
    )
    order_type_key = lambda x: x[2]
    completions = sorted(completions, key=order_type_key)
    gb = ut.groupby(completions, key=order_type_key)
    for order_type, group in gb:
        # group = [list(gr) if len(gr) == 6 else list(gr) + [D('0')] for gr in group]
        group = list(group)
        product = rp.get_product(service_id, contract, order_type=order_type)
        (promo_subt_order,) = {
            pp.promo_subt_order
            for pp in contract.session.query(mapper.PartnerProduct)
            .filter_by(product_mdh_id=product.mdh_id)
            .all()
        }
        commission_sum = sum(x[1] for x in group)
        promocode_sum = sum(x[4] for x in group)
        subsidy_sum = sum(x[5] for x in group)
        if commission_type == "promocode_sum":
            qty = promocode_sum
        elif commission_type == "subsidy_sum":
            qty = subsidy_sum
        else:
            qty = commission_sum
        yield ut.Struct(
            requested_comm_type_sum=qty,
            promocode_sum=promocode_sum,
            subsidy_sum=subsidy_sum,
            product_id=product.id,
            promo_subt_order=promo_subt_order,
        )


def real_completions_aggr_tlog(
    contract, on_dt, on_transaction_dt, from_dt, service_id, completions
):
    """
    compl_format:
      [(service_id, amount, completion_type, dt, from_dt)]
    Открутки группируются по completion_type, в реальном коде группировка идет по продуктам,
    из-за этого в тестах возможно использовать только уникальные по продуктам order_type"""
    order_types_mapping = {"subvention": "order"}
    if not from_dt:
        from_dt = datetime.datetime(1999, 1, 1)
    completions = filter(
        lambda x: x[0] == service_id and (on_dt > x[3] >= from_dt), completions
    )
    if on_transaction_dt:
        completions = filter(lambda x: on_transaction_dt > x[4], completions)
    order_type_key = lambda x: x[2]
    completions = sorted(completions, key=order_type_key)
    gb = ut.groupby(completions, key=order_type_key)
    for order_type, group in gb:
        group = list(group)
        product = rp.get_product(
            service_id,
            contract,
            order_type=order_types_mapping.get(order_type, order_type),
        )
        (promo_subt_order,) = {
            pp.promo_subt_order
            for pp in contract.session.query(mapper.PartnerProduct)
            .filter_by(product_mdh_id=product.mdh_id)
            .all()
        }
        amount = sum(x[1] for x in group)
        tlog_last_transaction_id = None
        if group and len(group[0]) == 6:
            tlog_last_transaction_id = max(ut.nvl(x[5], 0) for x in group)
        yield ut.Struct(
            amount=amount,
            completion_type=order_type,
            product_id=product.id,
            promo_subt_order=promo_subt_order,
            last_transaction_id=tlog_last_transaction_id,
        )


def compose_get_contract_func(service_id):
    return arguments_decorator(services={service_id})(update_contract_collateral)


SERVICES_INFO = {
    service_name: (
        [service_id],
        compose_get_contract_func(service_id),
    )
    for service_name, service_id in {
        "TicketsToEvents": ServiceId.TICKETS_TO_EVENTS,
        "Music": ServiceId.MUSIC,
        "MusicMediaServices": ServiceId.MUSIC_MEDIASERVICES,
    }.items()
}
# base case for RevPartnersTransactionsBase
SERVICES_INFO.update({"Base": ([], None)})

