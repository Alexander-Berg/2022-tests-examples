# -*- coding: utf-8 -*-

import datetime
import pytest
import hamcrest as hm

from butils.decimal_unit import DecimalUnit as DU
from balance import muzzle_util as ut
from balance import exc
from balance.mapper import Invoice
from billing.contract_iface import ContractTypeId
from balance.constants import PaymentMethodIDs, PREPAY_PAYMENT_TYPE
from tests.balance_tests.invoices.invoice_factory.invoice_factory_common import (create_paysys, create_client,
                                                                                 create_person,
                                                                                 create_person_category, create_firm,
                                                                                 create_currency,
                                                                                 create_service, create_request,
                                                                                 create_currency_rate, create_manager,
                                                                                 create_order, create_product,
                                                                                 create_contract)

BANK = PaymentMethodIDs.bank
NOW = datetime.datetime.now()
DIRECT_DISCOUNT_TYPE = 7
DIRECT_COMMISSION_TYPE = 7


def check_invoice_order(invoice_order, request_order):
    assert invoice_order.order == request_order.order


@pytest.mark.parametrize('paysys_nds', [0, 1])
def test_paysys_attrs(session, client, firm, currency, paysys_nds):
    """Валюту и флаг ндс записываем в шапку счета из способа оплаты"""
    request = create_request(session, client, dt=NOW)
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=paysys_nds)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])

    invoice.update_head_attributes()
    assert invoice.currency == paysys.currency
    assert invoice.iso_currency == currency.iso_code
    assert invoice.nds == paysys_nds


def test_currency_rate(session, client, firm, currency):
    """Валюту и флаг ндс записываем в шапку счета из способа оплаты"""
    request = create_request(session, client, dt=NOW)
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=1)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])

    invoice.update_head_attributes()
    assert invoice.currency_rate == DU('100.0000000000', ['RUB'], [currency.char_code])
    assert invoice.internal_rate == DU('100.0000000000', ['RUB'], ['FISH'])


def test_manager(session, client, firm, currency, manager):
    request = create_request(session, client, orders=[create_order(session, client, manager=manager)])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])

    invoice.update_head_attributes()
    assert invoice.manager == manager


def test_several_managers(session, client, firm, currency, manager):
    request = create_request(session, client, orders=[create_order(session, client, manager=create_manager(session)),
                                                      create_order(session, client, manager=create_manager(session))]
                             )
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])

    invoice.update_head_attributes()
    assert invoice.manager is None
    hm.assert_that(
        invoice.invoice_orders,
        hm.contains_inanyorder(*[
            hm.has_properties('manager', r_o.order.manager)
            for r_o in request.request_orders
        ]),
    )


def test_empty_discount_type(session, client, firm, currency):
    request = create_request(session, client,
                             orders=[create_order(session, client,
                                                  product=create_product(session,
                                                                         media_discount=0))])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])

    invoice.update_head_attributes()
    assert invoice.discount_type == 0


def test_same_discount_type(session, client, firm, currency):
    request = create_request(session, client,
                             orders=[create_order(session, client,
                                                  product=create_product(session,
                                                                         media_discount=666)),
                                     create_order(session, client,
                                                  product=create_product(session,
                                                                         media_discount=666))
                                     ])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])

    invoice.update_head_attributes()
    assert invoice.discount_type == 666


@pytest.mark.parametrize('temporary', [True, False])
def test_several_discount_types(session, client, firm, currency, temporary):
    request = create_request(session, client,
                             orders=[create_order(session, client,
                                                  product=create_product(session,
                                                                         media_discount=666)),
                                     create_order(session, client,
                                                  product=create_product(session,
                                                                         media_discount=667))
                                     ])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=temporary,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])
    if temporary:
        invoice.update_head_attributes()
        assert invoice.discount_type is None
    else:
        with pytest.raises(exc.MULTIPLE_DISCOUNT_TYPES):
            invoice.update_head_attributes()


@pytest.mark.parametrize('discount_types', [[DIRECT_DISCOUNT_TYPE, 3],
                                            [4, 5]])
@pytest.mark.parametrize('temporary', [True, False])
def test_several_discount_types_auto_overdraft(session, client, firm, currency, discount_types, temporary):
    orders = [create_order(session, client,
                           product=create_product(session,
                                                  media_discount=discount_type)) for discount_type in discount_types]

    request = create_request(session, client, orders=orders)
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=temporary,
                      is_auto_overdraft=True, basket_rows=[ro.basket_item() for ro in request.request_orders])
    invoice.update_head_attributes()
    if DIRECT_DISCOUNT_TYPE in discount_types:
        assert invoice.discount_type == DIRECT_DISCOUNT_TYPE
    else:
        assert invoice.discount_type == min(discount_types)


def test_empty_commission_type(session, client, firm, currency):
    request = create_request(session, client,
                             orders=[create_order(session, client,
                                                  product=create_product(session,
                                                                         commission_type=0))])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])

    invoice.update_head_attributes()
    assert invoice.commission_type == 0


def test_same_commission_types(session, client, firm, currency):
    request = create_request(session, client,
                             orders=[create_order(session, client,
                                                  product=create_product(session,
                                                                         commission_type=666)),
                                     create_order(session, client,
                                                  product=create_product(session,
                                                                         commission_type=666))
                                     ])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])

    invoice.update_head_attributes()
    assert invoice.commission_type == 666


@pytest.mark.parametrize('commission_types', [[DIRECT_COMMISSION_TYPE, 3],
                                              [4, 5]])
@pytest.mark.parametrize('temporary', [True, False])
def test_several_commission_types_auto_overdraft(session, client, firm, currency, commission_types, temporary):
    orders = [
        create_order(session, client,
                     product=create_product(session, commission_type=commission_type))
        for commission_type in commission_types]

    request = create_request(session, client, orders=orders)
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=temporary,
                      is_auto_overdraft=True, basket_rows=[ro.basket_item() for ro in request.request_orders])
    invoice.update_head_attributes()
    if DIRECT_COMMISSION_TYPE in commission_types:
        assert invoice.commission_type == DIRECT_COMMISSION_TYPE
    else:
        assert invoice.commission_type == min(commission_types)


@pytest.mark.parametrize('temporary', [True, False])
def test_several_commission_types(session, client, firm, currency, temporary):
    request = create_request(session, client,
                             orders=[create_order(session, client,
                                                  product=create_product(session,
                                                                         commission_type=666)),
                                     create_order(session, client,
                                                  product=create_product(session,
                                                                         commission_type=667))
                                     ])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=temporary,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])
    if temporary:
        invoice.update_head_attributes()
        assert invoice.commission_type is None
    else:
        with pytest.raises(exc.MULTIPLE_COMMISSION_TYPES):
            invoice.update_head_attributes()


@pytest.mark.parametrize('crossfirm', [True, False])
def test_several_commission_types_crossfirm(session, client, firm, currency, crossfirm):
    request = create_request(session, client,
                             orders=[create_order(session, client,
                                                  product=create_product(session,
                                                                         commission_type=666)),
                                     create_order(session, client,
                                                  product=create_product(session,
                                                                         commission_type=667))
                                     ])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders],
                      crossfirm=crossfirm)
    if crossfirm:
        invoice.update_head_attributes()
        assert invoice.commission_type is None
    else:
        with pytest.raises(exc.MULTIPLE_COMMISSION_TYPES):
            invoice.update_head_attributes()


@pytest.mark.parametrize('unilateral_values', [[1, 1],
                                               [0, 1]])
def test_unilateral_services(session, client, currency, unilateral_values):
    firm = create_firm(session, unilateral=1)
    orders = [create_order(session, client, service=create_service(session, unilateral=unilateral))
              for unilateral in unilateral_values]
    request = create_request(session, client,
                             orders=orders)
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])
    invoice.update_head_attributes()
    if all(unilateral_values):
        assert invoice.unilateral == 1
    else:
        assert invoice.unilateral == 0


@pytest.mark.parametrize('unilateral_firm', [0, 1])
def test_unilateral_firm(session, client, currency, unilateral_firm):
    firm = create_firm(session, unilateral=unilateral_firm)
    request = create_request(session, client,
                             orders=[create_order(session, client,
                                                  service=create_service(session, unilateral=1))])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])
    invoice.update_head_attributes()
    if unilateral_firm:
        assert invoice.unilateral == 1
    else:
        assert invoice.unilateral == 0


@pytest.mark.parametrize('unilateral_contract', [0, 1])
def test_unilateral_contract(session, client, currency, unilateral_contract):
    firm = create_firm(session, unilateral=1)
    service = create_service(session, unilateral=1)
    request = create_request(session, client,
                             orders=[create_order(session, client, service=service)])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=0)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               unilateral=unilateral_contract, client=client, payment_type=PREPAY_PAYMENT_TYPE,
                               personal_account=True, services={service.id}, is_signed=NOW, person=person,
                               currency=currency.num_code)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False, person=person,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])
    invoice.assign_contract(contract)
    invoice.update_head_attributes()
    if unilateral_contract:
        assert invoice.unilateral == 1
    else:
        assert invoice.unilateral == 0
