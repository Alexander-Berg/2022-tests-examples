# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime
import pytest

from balance import mapper
from balance import muzzle_util as ut
from balance import reverse_partners as rp
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.constants import *
from tests import object_builder as ob
from tests.balance_tests.rev_partners.common import (
    ADFOX_COEFFICIENT_SCALE_CODE,
    ADFOX_COST_SCALE_CODE,
    ADFOX_DEFAULT_SCALE_CODE,
    TEST_ADFOX_COST_PRODUCT_ID,
    TEST_ADFOX_DEFAULT_PRODUCT_ID,
    TEST_ADFOX_MAIN_PRODUCT_WITH_COEFFICIENT,
    _create_scale,
    adfox_contract_terms,
    check_qty,
    check_sum,
    gen_contract,
    generate_acts,
)


def test_adfox_repayment_coefficient_discount(session):
    def adfox_compl(contract, on_dt):
        return [(TEST_ADFOX_MAIN_PRODUCT_WITH_COEFFICIENT, D(1500))]

    rp.compl_map[ServiceId.ADFOX] = adfox_compl
    points = [(0, 1), (1000, 2), (2000, 3)]
    _create_scale(
        session, ADFOX_COEFFICIENT_SCALE_CODE, points, y_unit_id=COEFFICIENT_UNIT_ID
    )

    contract = gen_contract(
        session, postpay=True, con_func=lambda c: adfox_contract_terms(c)
    )
    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 5, 1))
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.ADFOX], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == D("1500")
    assert acts[0].rows[0].act_sum == ut.round(D((1500 / D(1000)) * 2 * D("1.18")), 2)
    repayment = acts[0].invoice
    fictive = repayment.fictives[0]
    assert repayment.total_sum == fictive.total_sum


def test_adfox_non_resident(session):
    def adfox_compl(contract, on_dt):
        return [(TEST_ADFOX_COST_PRODUCT_ID, 2000)]

    rp.compl_map[ServiceId.ADFOX] = adfox_compl
    points = [(0, 1), (1000, 2), (2000, 3)]
    _create_scale(session, ADFOX_COST_SCALE_CODE, points, y_unit_id=ADFOX_COST_UNIT_ID)

    contract = gen_contract(
        session, postpay=True, con_func=lambda c: adfox_contract_terms(c, resident=0)
    )
    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 5, 1))
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.ADFOX], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == D("3")
    assert acts[0].rows[0].act_sum == D("3")
    repayment = acts[0].invoice
    fictive = repayment.fictives[0]
    assert repayment.total_sum == fictive.total_sum
    assert repayment.paysys.id == 11069


def test_adfox_acts_for_same_month(session):
    def adfox_compl(contract, on_dt):
        return [(TEST_ADFOX_MAIN_PRODUCT_WITH_COEFFICIENT, D(1500))]

    rp.compl_map[ServiceId.ADFOX] = adfox_compl
    points = [(0, 1), (1000, 2), (2000, 3)]
    _create_scale(
        session, ADFOX_COEFFICIENT_SCALE_CODE, points, y_unit_id=COEFFICIENT_UNIT_ID
    )

    contract = gen_contract(
        session, postpay=True, con_func=lambda c: adfox_contract_terms(c)
    )
    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 5, 1))
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.ADFOX], a_m)
    res = rpc.process_and_enqueue_act()
    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1

    res2 = rpc.process_and_enqueue_act()
    assert res2 is None


def test_adfox_defaults(session):
    def adfox_compl(contract, on_dt):
        return [
            (TEST_ADFOX_MAIN_PRODUCT_WITH_COEFFICIENT, 10000),
            (TEST_ADFOX_DEFAULT_PRODUCT_ID, 5000),
        ]

    COEFFICIENT = D(2)

    rp.compl_map[ServiceId.ADFOX] = adfox_compl
    main_points = [(0, 1), (10000, COEFFICIENT), (20000, 3)]
    _create_scale(
        session,
        ADFOX_COEFFICIENT_SCALE_CODE,
        main_points,
        y_unit_id=COEFFICIENT_UNIT_ID,
    )

    default_points = [(0, 1), (10000, COEFFICIENT), (20000, 3)]
    _create_scale(
        session, ADFOX_DEFAULT_SCALE_CODE, default_points, y_unit_id=COEFFICIENT_UNIT_ID
    )

    contract = gen_contract(
        session, postpay=True, con_func=lambda c: adfox_contract_terms(c)
    )
    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 5, 1))
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.ADFOX], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 2
    check_qty(acts[0], adfox_compl(None, None))
    check_sum(
        acts[0], adfox_compl(None, None), price=D("0.001") * D("1.18") * COEFFICIENT
    )


def test_zero_invoice(session):
    """Do not create invoice and acts with zero effective_sum"""

    def adfox_compl(contract, on_dt):
        return [
            (TEST_ADFOX_MAIN_PRODUCT_WITH_COEFFICIENT, 2),
            (TEST_ADFOX_DEFAULT_PRODUCT_ID, 1),
        ]

    rp.compl_map[ServiceId.ADFOX] = adfox_compl
    main_points = [(0, 1), (10000, 2), (20000, 3)]
    _create_scale(
        session,
        ADFOX_COEFFICIENT_SCALE_CODE,
        main_points,
        y_unit_id=COEFFICIENT_UNIT_ID,
    )

    default_points = [(0, 1), (10000, 2), (20000, 3)]
    _create_scale(
        session, ADFOX_DEFAULT_SCALE_CODE, default_points, y_unit_id=COEFFICIENT_UNIT_ID
    )

    contract = gen_contract(
        session, postpay=True, con_func=lambda c: adfox_contract_terms(c)
    )
    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 5, 1))
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.ADFOX], a_m)

    assert rpc.process_and_enqueue_act() == None


@pytest.mark.skip(reason="depends on t_scale, for manual testing only")
def test_adfox_offer(session):
    """Test adfox scales"""

    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type="ur").build(session).obj
    product = session.query(mapper.Product).getone(506513)
    basket = ob.BasketBuilder(
        rows=[
            ob.BasketItemBuilder(
                quantity=1500000, order=ob.OrderBuilder(client=client, product=product)
            ),
            ob.BasketItemBuilder(
                quantity=2000000, order=ob.OrderBuilder(client=client, product=product)
            ),
            ob.BasketItemBuilder(
                quantity=19500000, order=ob.OrderBuilder(client=client, product=product)
            ),
        ]
    )

    request = ob.RequestBuilder(basket=basket).build(session).obj

    inv = ob.InvoiceBuilder(request=request, person=person).build(session).obj
    InvoiceTurnOn(inv, manual=True).do()
    assert inv.rows[0].quantity == D("1500000")
    assert inv.rows[0].discount_pct == D("0")
    assert inv.rows[0].amount == D("1300") * D("1.18")
    assert inv.rows[0].amount - inv.rows[0].amount_nds == D("1300")

    assert inv.rows[1].amount - inv.rows[1].amount_nds == D("1500")
    assert inv.rows[1].discount_pct == D("0")

    assert inv.rows[2].amount == D("8300") * D("1.18")
    assert inv.rows[2].amount - inv.rows[2].amount_nds == D("8300")
    assert inv.rows[2].discount_pct == D("0")


@pytest.mark.skip(reason="depends on t_scale, for manual testing only")
def test_adfox_offer_sng(session):
    client = ob.ClientBuilder(region_id=187).build(session).obj
    person = ob.PersonBuilder(client=client, type="yt").build(session).obj
    product = session.query(mapper.Product).getone(506513)
    basket = ob.BasketBuilder(
        rows=[
            ob.BasketItemBuilder(
                quantity=1500000,
                order=ob.OrderBuilder(service_id=102, client=client, product=product),
            ),
            ob.BasketItemBuilder(
                quantity=2000000,
                order=ob.OrderBuilder(service_id=102, client=client, product=product),
            ),
        ]
    )

    request = ob.RequestBuilder(basket=basket).build(session).obj

    inv = ob.InvoiceBuilder(request=request, person=person).build(session).obj
    InvoiceTurnOn(inv, manual=True).do()

    assert inv.rows[0].tax_policy_pct.nds_pct == D("18")
    assert inv.rows[0].discount_pct == D("0")
    assert inv.rows[0].amount == D("1073.80")
    assert inv.rows[0].amount - inv.rows[0].amount_nds == D("910")

    assert inv.rows[1].amount == D("1239.00")
    assert inv.rows[1].amount - inv.rows[1].amount_nds == D("1050")
    assert inv.rows[1].discount_pct == D("31.82")


def test_adfox_dmp(session):
    PRODUCT_ID = 508334
    QTY = D("10000")
    PRICE = D("30")
    COEFF2019 = D("1.10")
    NDS = D("18")
    PRICE_DT = datetime.datetime(2017, 5, 3)
    MONTH_DT = datetime.datetime(2017, 6, 1)

    def adfox_compl(contract, on_dt):
        return [(PRODUCT_ID, QTY, None, PRICE_DT)]

    rp.compl_map[ServiceId.ADFOX] = adfox_compl

    contract = gen_contract(
        session, postpay=True, con_func=lambda c: adfox_contract_terms(c)
    )
    a_m = mapper.ActMonth(for_month=MONTH_DT)
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.ADFOX], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2

    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])

    repayment = acts[0].invoice
    fictive = repayment.fictives[0]
    assert repayment.total_sum == fictive.total_sum

    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == QTY
    assert acts[0].rows[0].act_sum == QTY / D("1000") * PRICE * COEFF2019 * (
        1 + NDS / 100
    )


def test_adfox_dmp_tax_change(session):
    PRODUCT_ID = 508334
    QTY = D("10000")
    PRICE = D("30")
    COEFF2019 = D("1.10")
    NEW_NDS = D("20")
    PRICE_DT = datetime.datetime(2018, 12, 1)
    MONTH_DT = datetime.datetime(2019, 2, 1)

    def adfox_compl(contract, on_dt):
        return [(PRODUCT_ID, QTY, None, PRICE_DT), (PRODUCT_ID, QTY, None, MONTH_DT)]

    rp.compl_map[ServiceId.ADFOX] = adfox_compl

    contract = gen_contract(
        session, postpay=True, con_func=lambda c: adfox_contract_terms(c)
    )
    a_m = mapper.ActMonth(for_month=MONTH_DT)
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.ADFOX], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])

    repayment = acts[0].invoice
    fictive = repayment.fictives[0]
    assert repayment.total_sum == fictive.total_sum
    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == 2 * QTY
    assert acts[0].rows[0].act_sum == 2 * QTY / D("1000") * PRICE * COEFF2019 * (
        1 + NEW_NDS / 100
    )
