# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm
from decimal import Decimal as D

from balance import constants as cst
from tests import object_builder as ob


@pytest.fixture(name='order')
def create_order(session):
    return ob.OrderBuilder.construct(session)


@pytest.fixture(name='invoice')
def create_invoice(session, order=None, firm_id=cst.FirmId.YANDEX_OOO, payment_method_id=cst.PaymentMethodIDs.bank):
    order = order or ob.OrderBuilder()
    invoice_params = {}
    if payment_method_id:
        paysys = ob.PaysysBuilder.construct(
            session,
            firm_id=firm_id,
            currency='RUR',
            iso_currency='RUB',
            payment_method_id=payment_method_id,
        )
        invoice_params['paysys'] = paysys
    request = ob.RequestBuilder.construct(
        session,
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            rows=[ob.BasketItemBuilder(order=order, quantity=1)],
        )
    )
    invoice_params['request'] = request

    invoice = ob.InvoiceBuilder.construct(session, **invoice_params)
    return invoice


class TestFirmIdsCache(object):
    """Кеширование firm_id при создании consume в таблицу t_order_invoice_cache
    """
    def test_save_firm_id_to_cache(self, session, order):
        assert order.firm_ids == []
        firm_ids = [
            cst.FirmId.YANDEX_OOO,
            cst.FirmId.CLOUD,
        ]
        invoices = [
            create_invoice(session, order=order, firm_id=firm_id)
            for firm_id in firm_ids
        ]
        assert order.firm_ids == []
        for invoice in invoices:
            invoice.turn_on_rows()

        hm.assert_that(order.firm_ids, hm.has_length(2))
        hm.assert_that(
            order.firm_ids,
            hm.contains_inanyorder(*firm_ids),
        )

    def test_reverse(self, session):
        """При переносе на другой заказ в нем кешируется фирма из счета"""
        order1 = create_order(session)
        order2 = create_order(session)

        invoice = create_invoice(session, order=order1, firm_id=cst.FirmId.MARKET, payment_method_id=None)
        invoice.manual_turn_on(D('666.66'))

        hm.assert_that(order1.firm_ids, hm.has_length(1))
        hm.assert_that(
            order1.firm_ids,
            hm.equal_to([cst.FirmId.MARKET]),
        )
        hm.assert_that(order2.firm_ids, hm.has_length(0))

        order1.transfer(order2)
        hm.assert_that(order2.firm_ids, hm.has_length(1))
        hm.assert_that(
            order2.firm_ids,
            hm.equal_to([cst.FirmId.MARKET]),
        )

    def test_not_save_firm_id_for_sertificate_invoice(self, session, order):
        """Не кешируем фирму для сертификатов"""
        assert order.firm_ids == []
        firm_ids = [
            cst.FirmId.YANDEX_OOO,
            cst.FirmId.CLOUD,
        ]
        invoices = [
            create_invoice(session, order=order, firm_id=firm_id, payment_method_id=cst.PaymentMethodIDs.certificate)
            for firm_id in firm_ids
        ]
        for invoice in invoices:
            invoice.turn_on_rows()

        assert order.firm_ids == []
