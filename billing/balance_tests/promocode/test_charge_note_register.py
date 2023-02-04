# -*- coding: utf-8 -*-

from decimal import Decimal as D

import pytest
import hamcrest

from balance import exc
from balance import mapper
from balance import constants as cst
from balance.actions import promocodes
from balance.actions import single_account
from balance.actions.invoice_turnon import InvoiceTurnOn

from tests import object_builder as ob
from tests.balance_tests.invoices import invoice_common
from tests.balance_tests.promocode.common import (
    create_promocode as common_create_promocode,
    reserve_promocode,
    create_order,
    create_invoice,
)

pytestmark = [
    pytest.mark.promo_code,
    pytest.mark.charge_note_register,
]


def test_check_zero_minimal_amount(session, promocode, invoice):
    promocode.group.minimal_amounts = {}
    request = ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=invoice.client,
            rows=[],
            register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=invoice)]
        )
    )
    charge_note = ob.InvoiceBuilder.construct(
        session,
        person=invoice.person,
        request=request,
        type='charge_note_register',
    )

    with pytest.raises(exc.INVALID_PC_NO_MATCHING_ROWS) as exc_info:
        promocodes.check_promo_code_invoice(promocode, charge_note)
    assert 'ID_PC_NO_MATCHING_ROWS' in exc_info.value.msg


def create_promocode(client, on_create, adjust_qty, bonus):
    session = client.session
    promocode = common_create_promocode(session, dict(
        calc_class_name='FixedSumBonusPromoCodeGroup',
        calc_params={
            'currency_bonuses': {'RUB': bonus},
            'adjust_quantity': adjust_qty,
            'apply_on_create': on_create,
        }
    ))
    reserve_promocode(session, promocode, client)
    return promocode


def create_invoice_order(session, invoice_sum):
    session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True

    client = ob.ClientBuilder.construct(session, with_single_account=True)
    order = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)
    invoice = create_invoice(session, invoice_sum, order.client, [order])
    single_account.prepare.process_client(client)
    return invoice, order


@pytest.mark.parametrize(
    'invoice_sum, qty, bonus, adjust_qty, res_total_sum, res_row_sum, res_row_qty, res_pct',
    [
        pytest.param(100, 100, D('41.666667'), True, 200, 100, D('149.9999'), D('33.3333'), id='w_adjust_qty'),
        pytest.param(100, 100, D('41.666667'), False, 150, 50, 100, 50, id='wo_adjust_qty'),
    ]
)
def test_create_charge_note(session,
                            invoice_sum,
                            qty,
                            bonus,
                            adjust_qty,
                            res_total_sum,
                            res_row_sum,
                            res_row_qty,
                            res_pct):
    invoice, order = create_invoice_order(session, invoice_sum)
    promocode = create_promocode(invoice.client, True, adjust_qty, bonus)

    charge_note = invoice_common.create_charge_note_register(
        invoice.paysys_id,
        invoice.person,
        [(order, qty)],
        [invoice],
        single_account_number=invoice.client.single_account_number
    )

    hamcrest.assert_that(
        charge_note,
        hamcrest.has_properties(
            promo_code=promocode,
            total_sum=res_total_sum,
            invoice_orders=hamcrest.contains(
                hamcrest.has_properties(
                    amount=res_row_sum,
                    quantity=res_row_qty,
                    discount_pct=res_pct,
                    discount_obj=mapper.DiscountObj(0, res_pct, promocode)
                )
            )
        )
    )


def test_turn_on_charge_note(session):
    invoice, order = create_invoice_order(session, 100)
    charge_note = invoice_common.create_charge_note_register(
        invoice.paysys_id,
        invoice.person,
        [(order, 100)],
        [invoice],
        single_account_number=invoice.client.single_account_number
    )
    pa = charge_note.charge_invoice

    promocode = create_promocode(invoice.client, False, True, D('41.666667'))
    InvoiceTurnOn(charge_note).do()

    hamcrest.assert_that(
        charge_note,
        hamcrest.has_properties(
            promo_code=promocode,
            total_sum=200,
        )
    )
    hamcrest.assert_that(
        pa,
        hamcrest.has_properties(
            consume_sum=100,
            consumes=hamcrest.contains(
                hamcrest.has_properties(
                    consume_sum=100,
                    consume_qty=D('149.9999'),
                    discount_pct=D('33.3333'),
                    discount_obj=mapper.DiscountObj(0, D('33.3333'), promocode),
                )
            )
        )
    )


def test_tear_promocode_off(session):
    invoice, order = create_invoice_order(session, 100)
    charge_note = invoice_common.create_charge_note_register(
        invoice.paysys_id,
        invoice.person,
        [(order, 100)],
        [invoice],
        single_account_number=invoice.client.single_account_number
    )
    pa = charge_note.charge_invoice

    promocode = create_promocode(invoice.client, False, True, D('41.666667'))
    InvoiceTurnOn(charge_note).do()

    promocodes.tear_promocode_off(session, pa)

    hamcrest.assert_that(
        pa,
        hamcrest.has_properties(
            consume_sum=100,
            consumes=hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    current_sum=0,
                    current_qty=0,
                    discount_obj=mapper.DiscountObj(0, D('33.3333'), promocode),
                ),
                hamcrest.has_properties(
                    current_sum=100,
                    current_qty=100,
                    discount_obj=mapper.DiscountObj(),
                )
            )
        )
    )
