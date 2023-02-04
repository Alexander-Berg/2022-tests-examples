# -*- coding: utf-8 -*-

import datetime

import pytest

from balance import mapper
import balance.actions.acts as a_a
from balance.actions import consumption as a_c
from balance.providers.personal_acc_manager import PersonalAccountManager

from tests import object_builder as ob


@pytest.fixture
def credit_contract(session):
    return ob.create_credit_contract(session)


@pytest.mark.invoice_services_cache
class TestGetServices(object):
    def test_wo_cache_rows_unloaded(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj
        del invoice._service_ids
        session.flush()
        session.expire_all()

        assert {s.id for s in invoice.get_services(from_consumes=False)} == {7}

    def test_wo_cache_rows_loaded(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj
        del invoice._service_ids
        session.flush()
        session.expire_all()
        invoice.invoice_orders

        assert {s.id for s in invoice.get_services(from_consumes=False)} == {7}

    def test_w_cache_rows(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj
        invoice._service_ids = {'7': 1, '67': 2, '77': 3}
        session.flush()
        session.expire_all()

        assert {s.id for s in invoice.get_services(from_consumes=False)} == {7, 77}

    def test_w_cache_rows_empty(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj
        invoice._service_ids = {'0': 666}
        session.flush()
        session.expire_all()

        assert invoice.get_services(from_consumes=False) == set()

    def test_wo_cache_consumes(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj
        del invoice._service_ids
        session.flush()
        session.expire_all()
        invoice.turn_on_rows()

        assert {s.id for s in invoice.get_services(from_consumes=True)} == {7}

    def test_w_cache_consumes(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj
        invoice._service_ids = {'7': 1, '67': 2, '77': 3}
        session.flush()

        assert {s.id for s in invoice.get_services(from_consumes=True)} == {67, 77}

    @pytest.mark.parametrize('w_cache', [False, True], ids=['wo_cache', 'w_cache'])
    @pytest.mark.parametrize('from_consumes', [True, False], ids=['from_consumes', 'from_rows'])
    def test_yinvoice(self, session, credit_contract, w_cache, from_consumes):
        agency = credit_contract.client
        order = ob.OrderBuilder(client=ob.ClientBuilder(agency=agency), agency=agency).build(session).obj
        basket = ob.BasketBuilder(
            client=agency,
            rows=[ob.BasketItemBuilder(quantity=6666, order=order)]
        )

        paysys = session.query(mapper.Paysys).getone(1003)
        pa, = ob.PayOnCreditCase(session).pay_on_credit(basket, credit_contract, paysys)

        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 100})
        month = mapper.ActMonth(for_month=datetime.datetime.now())
        act, = a_a.ActAccounter(agency, month, dps=[], invoices=[pa.id], force=1).do()
        y_invoice = act.invoice

        if w_cache:
            y_invoice._service_ids = {'7': 1, '77': 1}
        else:
            del y_invoice._service_ids
        session.flush()

        services = {s.id for s in y_invoice.get_services(from_consumes)}

        assert {s.id for s in pa.get_services(from_consumes=False)} == set()
        assert {s.id for s in pa.get_services(from_consumes=True)} == {7}
        if w_cache:
            assert services == {7, 77}
        else:
            assert services == {7}


@pytest.mark.invoice_services_cache
class TestServiceCacheUpdate(object):
    def test_empty(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj
        del invoice._service_ids
        session.flush()
        session.expire_all()

        a_c.refresh_services_cache(invoice, [7])
        assert invoice._service_ids == {}

    def test_multiple_new(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj

        a_c.refresh_services_cache(invoice, [7, 77, 67])
        assert invoice._service_ids == {'7': 3, '77': 2, '67': 2}

    def test_existing(self, session):
        invoice = ob.InvoiceBuilder().build(session).obj
        invoice.turn_on_rows()
        assert invoice._service_ids == {'7': 3}

        a_c.refresh_services_cache(invoice, [7])
        assert invoice._service_ids == {'7': 3}

    def test_new_fpa(self, session, credit_contract):
        paysys = session.query(mapper.Paysys).getone(1003)
        pa = PersonalAccountManager(session).for_contract(credit_contract).for_paysys(paysys).get()
        assert pa._service_ids == {'0': 0}

        a_c.refresh_services_cache(pa, [7, 77])

        assert pa._service_ids == {'7': 2, '77': 2}
