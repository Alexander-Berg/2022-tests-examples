# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime

from balance import constants as const
from balance import mapper
from balance import muzzle_util as ut
from balance import reverse_partners as rp
from balance.actions.invoice_create import InvoiceFactory
from balance.actions.invoice_turnon import InvoiceTurnOn
from tests import object_builder as ob
from tests.balance_tests.rev_partners.common import (
    gen_contract,
    taxi_terms_apx,
)


def real_completions(
    contract, on_dt, commission_type, from_dt, service_id, completions
):
    """
    compl_format:
      [(service_id, commission_sum, order_type, dt, promocode_sum, subsidy_sum)]
    subsidy_sum - опционально, проставим 0, если не передан
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
        # докинем 0 в субсидии, если их не передали, чтобы не править все тесты.
        group = [list(gr) if len(gr) == 6 else list(gr) + [D("0")] for gr in group]
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


def mock_completions(completions):
    def get_taxi_completions(contract, on_dt, commission_type, from_dt, service_id):
        return real_completions(
            contract, on_dt, commission_type, from_dt, service_id, completions
        )

    rp.get_taxi_completions = get_taxi_completions
    # rp.compl_map[ServiceId.TAXI_CASH] = service_completions


def get_order_act(acts):
    order_services = (const.ServiceId.TAXI_CASH, const.ServiceId.TAXI_CARD)
    a = [
        a
        for a in acts
        if [at for at in a.rows if at.order.product.engine_id in order_services]
    ]
    assert len(a) == 1, "Zero or more than one order act {}".format(a)
    return a[-1]


def get_reward_act(acts):
    order_act = get_order_act(acts)
    a = [a for a in acts if a != order_act]
    assert len(a) == 1, "Zero or more than one reward act"
    return a[-1]


def get_promocode_acts(acts):
    s = [a for a in acts if a.type == "internal"]
    return s


def create_chargenote(session, contract, order, qty_sum):
    request = (
        ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=qty_sum)],
            ),
            invoice_desired_type="change_note",
        )
        .build(session)
        .obj
    )

    inv = InvoiceFactory.create(
        request,
        session.query(mapper.Paysys).get(1003),
        contract.person,
        status_id=0,
        credit=0,
        temporary=False,
        contract=contract,
    )
    return inv


def test_prepay_chargenote(session):
    contract = gen_contract(
        session,
        postpay=False,
        personal_account=True,
        con_func=lambda c: taxi_terms_apx(c, 400),
    )
    # rpc = rp.ReversePartnerCalc(contract, ServiceId.TAXI_CASH, None)
    order = rp.get_order(const.ServiceId.TAXI_CASH, contract, ua_root=True)
    qty_sum = 100
    inv = create_chargenote(session, contract, order, qty_sum)
    assert inv.type == "charge_note"
    assert inv.charge_invoice.contract == contract
    assert inv.external_id == inv.charge_invoice.external_id
    InvoiceTurnOn(inv).do()
    assert inv.charge_invoice.receipt_sum == qty_sum


def create_product(session, service_id, firm_id, *args, **kwargs):
    product_unit = ob.Getter(mapper.ProductUnit, 850)
    product_builder = ob.ProductBuilder(
        engine_id=service_id,
        firm_id=firm_id,
        dt=datetime.datetime(2011, 1, 1),
        unit=product_unit,
        name="Taxi test payment product",
        englishname="Taxi test payment product",
        **kwargs
    )
    return product_builder.build(session).obj


def create_partner_product(session, product):
    partner_product = mapper.PartnerProduct(
        product_mdh_id=product.mdh_id,
        service_id=product.engine_id,
        currency_iso_code=product.unit.iso_currency,
        unified_account_root=1,
    )
    session.add(partner_product)
    session.flush()
    return partner_product


def create_pay_service(session, service_id):
    tp_service = mapper.ThirdPartyService(
        id=service_id,
        postauth_check=1,
        postauth_ready_check=0,
        reward_refund=0,
        get_commission_from="COMMISSION_CATEGORY",
        agent_report=1,
    )
    session.merge(tp_service)
