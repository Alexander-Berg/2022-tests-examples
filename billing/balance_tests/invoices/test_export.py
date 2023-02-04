# -*- coding: utf-8 -*-

import datetime
import mock
import pytest
# from conftest import (_create_request,
#                       create_chargenote,
#                       create_person)
from tests import object_builder as ob
from balance import mapper, constants as cst
from balance.actions.invoice_create import InvoiceFactory
from butils.decimal_unit import DecimalUnit as DU

NOW = datetime.datetime.now()

TRUST_REFUND_AMOUNT = DU('150', 'RUB')
PAYMENT_AMOUNT = DU('666', 'RUB')
LOGICAL_PAYMENT_AMOUNT = DU('444', 'RUB')
INVOICE_REFUND_AMOUNT = DU('777', 'RUB')
CHARGE_INVOICE_AMOUNT = DU('888', 'RUB')


def test_export_charge_note_register(session):
    order = ob.OrderBuilder(
        product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_ID).build(session).obj,
        service=ob.Getter(mapper.Service, cst.ServiceId.DIRECT)
    ).build(session).obj
    client = order.client
    person = ob.PersonBuilder.construct(session, type='ph', client=client)
    basket = ob.BasketBuilder(client=client,
                              rows=[ob.BasketItemBuilder(order=o, quantity=qty)
                                    for o, qty in [(order, 10)]])
    request_obj = ob.RequestBuilder(basket=basket).build(session).obj
    paysys = ob.Getter(mapper.Paysys, 1001).build(session).obj
    charge_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
    property_mock_charge_note_personal_account = mock.PropertyMock(return_value=charge_invoice)
    with mock.patch('balance.actions.invoice_create.InvoiceFactory.charge_note_personal_account',
                    new_callable=property_mock_charge_note_personal_account):
        ref_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request_obj)
        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=client,
                rows=[],
                register_rows=[
                    ob.BasketRegisterRowBuilder(ref_invoice=ref_invoice),
                ]
            )
        )

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            type='charge_note_register',
            temporary=False,
        )
    invoice.register_rows[0].is_internal = 0
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Invoice': 1}
    invoice.export()

    assert invoice.exportable == ['OEBS']


@pytest.mark.parametrize('with_new_export', [1, 0])
def test_export_prepayment(session, with_new_export):
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Invoice': 0}
    invoice = ob.InvoiceBuilder.construct(session)
    assert invoice.exportable == {'OEBS', 'PROCESS_PAYMENTS'}
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Invoice': with_new_export}
    invoice.export()
    if with_new_export:
        assert invoice.exportable == {'OEBS_API', 'PROCESS_PAYMENTS'}
    else:
        assert invoice.exportable == {'OEBS', 'PROCESS_PAYMENTS'}
