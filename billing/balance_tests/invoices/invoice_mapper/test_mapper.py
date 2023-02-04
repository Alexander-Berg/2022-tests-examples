import datetime

import decimal
from butils.decimal_unit import DecimalUnit as DU
from balance.mapper import Invoice, InvoiceOrder
from balance.constants import PaymentMethodIDs
from tests.balance_tests.invoices.invoice_factory.invoice_factory_common import (create_paysys, create_client,
                                                                                 create_person_category, create_firm,
                                                                                 create_currency,
                                                                                 create_service, create_request)

BANK = PaymentMethodIDs.bank
NOW = datetime.datetime.now()


def check_invoice_order(invoice_order, request_order):
    assert invoice_order.order == request_order.order
    assert invoice_order.quantity == DU('0', 'QTY')
    assert invoice_order.initial_quantity == DU('1', 'QTY')
    assert invoice_order.product == request_order.order.product
    assert invoice_order.type_rate == request_order.order.product.unit.type_rate


def test_1(session, client, firm, currency):
    request = create_request(session, client, dt=NOW)
    paysys = create_paysys(session, prefix='MYA')
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])
    assert invoice.dt == NOW
    assert invoice.client == request.client
    assert invoice.agency_discount_pct is None
    assert invoice.adjust_qty == 0
    assert len(invoice.invoice_orders) == 1
    check_invoice_order(invoice.invoice_orders[0], request.request_orders[0])
    assert invoice.credit == 0
    assert invoice.receipt_sum == decimal.Decimal(0)
    assert invoice.total_act_sum == decimal.Decimal(0)
    assert invoice.receipt_sum_1c == decimal.Decimal(0)
    assert invoice.consume_sum == decimal.Decimal(0)
    assert invoice.total_sum == decimal.Decimal(0)
    assert invoice.internal_rate is None
    assert invoice.extern == 0
    assert invoice.promo_code is None
    assert invoice.temporary is False
    assert invoice.person is None
    assert invoice.request == request
    assert invoice.paysys == paysys
    assert invoice.payment_method == paysys.payment_method
    assert invoice.legal_entity == paysys.person_category.ur
    assert invoice.resident == paysys.person_category.resident
    assert invoice.firm == firm
    assert invoice.base_iso_currency == firm.default_currency
    assert invoice.currency_rate_src == firm.currency_rate_src
    assert invoice.request_seq == 1
    assert invoice.external_id == '{}-{}-1'.format(paysys.prefix, request.id)
    assert invoice._service_ids == {'{}'.format(request.request_orders[0].order.service.id): 1}
