# -*- coding: utf-8 -*-

import datetime
import mock
import pytest

from balance import muzzle_util as ut
from balance.actions.invoice_create import InvoiceFactory
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.mapper import Service, Country, Currency
from balance.constants import FirmId, RegionId, ServiceId
from balance.xmlizer import getxmlizer, xml_text
from balance.utils.xml2json import xml2json_auto
from tests.balance_tests.xmlizer.xmlizer_common import (
    create_invoice, create_product, create_request, create_order, create_client,
    create_price_tax_rate, create_currency, core_obj, create_person,
    create_contract)

from tests.object_builder import Getter


def test_smoke(session, currency):
    session = session
    client = create_client(session)
    request = create_request(session, orders=[create_order(session, client=client,
                                                           service=Getter(Service, 35))],
                             firm_id=FirmId.YANDEX_OOO)
    create_price_tax_rate(session, product=request.request_orders[0].product,
                          country=Getter(Country, RegionId.RUSSIA), currency=Getter(Currency, 'RUR').build(session).obj)
    invoice = create_invoice(session, request=request)
    invoice.upd_contract_dt = datetime.datetime(2007, 10, 11)
    invoice_xmlized = getxmlizer(invoice).xmlize()

    assert invoice_xmlized.find('upd-contract-dt').text == '2007-10-11T00:00:00'

    assert invoice_xmlized.find('can-be-closed').text == u'0'
    InvoiceTurnOn(invoice, manual=True).do()
    invoice_xmlized = getxmlizer(invoice).xmlize()
    assert invoice_xmlized.find('can-be-closed').text == u'1'

    invoice.close_invoice(datetime.datetime.now())
    invoice_xmlized = getxmlizer(invoice).xmlize()

    assert invoice_xmlized.find('can-be-closed').text == u'0'

    request = create_request(session, orders=[create_order(session, client=client,
                                                           service=Getter(Service, 10))],
                             firm_id=FirmId.YANDEX_OOO)
    create_price_tax_rate(session,
                          product=request.request_orders[0].product,
                          country=Getter(Country, RegionId.RUSSIA),
                          currency=Getter(Currency, 'RUR').build(session).obj)
    invoice = create_invoice(session, request=request)
    assert getxmlizer(invoice).xmlize().find('can-be-closed').text == u'0'


def test_invoice_repayment(session, core_obj):
    client = create_client(session)
    request = create_request(session, orders=[create_order(session, client=client,
                                                           service=Getter(Service, ServiceId.DIRECT))],
                             firm_id=FirmId.YANDEX_OOO)

    person = create_person(session, client=client, type="ph")
    create_price_tax_rate(session,
                          product=request.request_orders[0].product,
                          country=Getter(Country, RegionId.RUSSIA),
                          currency=Getter(Currency, 'RUR').build(session).obj)

    contract = create_contract(session,
                               client=request.client,
                               person=person,
                               commission=0,
                               payment_type=3,
                               credit_limit={
                                   request.request_orders[0].product.activity_type.id: '9' * 20,
                               },
                               services=ServiceId.DIRECT,
                               is_signed=datetime.datetime.now(),
                               firm=1,
                               )
    invoice = create_invoice(session, person=person, request=request, contract=contract)
    session.flush()
    invoices = core_obj.pay_on_credit(
        request_id=request.id,
        paysys_id=invoice.paysys.id,
        person_id=person.id,
        contract_id=contract.id
    )
    invoices = sorted(invoices, key=lambda o: o.id)
    rep_request = core_obj.create_repayment_request(
        dt=datetime.datetime.now(), invoices=invoices
    )
    session.flush()
    rep_invoice = InvoiceFactory.create(
        request=rep_request, paysys=invoice.paysys, person=person,
        credit=1, status_id=5, temporary=False
    )
    InvoiceTurnOn(rep_invoice, manual=True).do()
    session.flush()
    session.expire_all()

    test_inv = getxmlizer(rep_invoice).xmlize().findall('fictive-invoices/invoice')
    for i in xrange(len(test_inv)):
        inv = rep_invoice.fictives[i]
        testinv = lambda x: test_inv[i].findall(x)[0].text

        assert xml_text(inv.id) == testinv('id')
        assert xml_text(inv.dt) == testinv('dt')
        assert xml_text(inv.external_id) == testinv('external-id')
        assert xml_text(inv.receipt_sum) == testinv('consume-sum')

        assert xml_text(ut.dsum(c.current_sum for c in inv.consumes)) == testinv('current-sum')
        assert xml_text(ut.dsum(c.completion_sum for c in inv.consumes)) == testinv('completion-sum')
        assert xml_text(ut.dsum(c.act_sum for c in inv.consumes)) == testinv('act-sum')


@pytest.mark.parametrize('is_pcp_alterable', [True, False])
def test_is_pcp_alterable(session, currency, is_pcp_alterable):
    session = session
    client = create_client(session)
    request = create_request(session, orders=[create_order(session, client=client,
                                                           service=Getter(Service, 35))],
                             firm_id=FirmId.YANDEX_OOO)
    create_price_tax_rate(session, product=request.request_orders[0].product,
                          country=Getter(Country, RegionId.RUSSIA), currency=Getter(Currency, 'RUR').build(session).obj)
    patch_is_pcp_alterable = mock.patch('balance.providers.invoice_alterable.is_pcp_alterable',
                                        return_value=is_pcp_alterable)
    invoice = create_invoice(session, request=request)
    with patch_is_pcp_alterable:
        invoice_json = xml2json_auto(getxmlizer(invoice).xmlize())
    # "изменить способ оплаты" на success и странице счета в КИ(?)
    assert invoice_json['is-alterable']['pcp-by-client'] == '1' if is_pcp_alterable else '0'
    # "Изменить способ оплаты и плательщика" в АИ
    assert invoice_json['is-alterable']['pcp'] == '1' if is_pcp_alterable else '0'
