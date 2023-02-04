# -*- coding: utf-8 -*-
"""Tests for order-request-invoice chain (before any money has been received).
See BALANCE-4462

Objects attributes and methods:
    TestClientAttributes
    TestOrderAttributes
    TestRequestAttributes
    TestInvoiceAttributes
"""
import pytest

from balance.mapper import *
from balance import offer

from tests.base import BalanceTest
from tests.object_builder import *

D = decimal.Decimal
CLIENT_TYPE_ID = 4  # ZAO


@pytest.fixture
def client(session):
    return ClientBuilder().build(session).obj


@pytest.fixture
def person(request, session, client):
    type = getattr(request, 'param', 'ur')
    return PersonBuilder(client=client, type=type).build(session).obj


@pytest.fixture
def contract(session, agency, person):
    return _create_contract(session, agency, person)


@pytest.fixture
def agency(session, client):
    client.is_agency = True
    session.flush()
    return client


def _create_order(session, client, product_id=DIRECT_PRODUCT_ID, product=None, service_id=7):
    product = product or Getter(mapper.Product, product_id).build(session).obj
    return OrderBuilder(
        product=product,
        service=Getter(mapper.Service, service_id),
        client=client,
        agency=client.agency
    ).build(session).obj


def _create_request(session, client, orders_qty=None):
    if orders_qty is None:
        orders_qty = [(_create_order(session, client, 10))]

    basket = BasketBuilder(
        client=client,
        rows=[
            BasketItemBuilder(order=o, quantity=qty)
            for o, qty in orders_qty
        ]
    )
    return RequestBuilder(basket=basket).build(session).obj


def _pay_on_credit(request, contract, paysys_id=1003):
    coreobj = core.Core(request.session)
    inv, = coreobj.pay_on_credit(
        request_id=request.id,
        paysys_id=paysys_id,
        person_id=contract.person.id,
        contract_id=contract.id
    )
    request.session.flush()
    return inv


def session_reload(session, *objects):
    "Save, flush, clear and reload by id, return a list"
    for obj in objects:
        session.add(obj)
    session.flush()
    session.expire_all()
    #    session.clear()
    #    for obj in objects:
    #        obj = session.query(obj.__class__).get(obj.id)
    return objects


def instance_pk(inst):
    # return instance primary key value
    return orm.object_mapper(inst).identity_key_from_instance(inst)[1]


def _create_contract(session, agency, person):
    contract = ContractBuilder(
        dt=datetime.datetime.now() - datetime.timedelta(days=66),
        client=agency,
        person=person,
        commission=1,
        payment_type=3,
        credit_type=1,
        payment_term=30,
        payment_term_max=60,
        personal_account=1,
        personal_account_fictive=1,
        currency=810,
        lift_credit_on_payment=1,
        commission_type=57,
        repayment_on_consume=1,
        credit_limit_single=1666666,
        services={7},
        is_signed=datetime.datetime.now(),
        firm=1,
    ).build(session).obj
    session.flush()
    return contract


class TestObjectAttributes(BalanceTest):
    """Base class for trivial attributes tests"""

    builder = None  # Builder class object
    defaults = {}  # Data for _test_defaults
    settable_attrs = {}  # Data for _test_attr_setting
    strict = False  # If False, treat 0 and None as equivalent in tests

    def _test_defaults(self, obj=None):
        """Test if defaults are set correctly on object creation"""
        if not obj:
            obj = self.builder().build(self.session).obj
        msg = "%s set to %s instead of %s"
        for key in self.defaults:
            obj_val, def_val = obj.__getattribute__(key), self.defaults[key]
            if isinstance(obj_val, DomainObject):
                obj_val = instance_pk(obj_val)
                def_val = instance_pk(def_val)
            if self.strict:
                self.assertEqual(obj_val, def_val,
                                 msg=msg % (key, obj_val, def_val))
            else:
                self.assert_((not obj_val and not def_val) or obj_val == def_val,
                             msg % (key, obj_val, def_val))

    def _test_attr_setting(self, obj=None):
        """Test if attributes can be set"""
        s = self.session
        if not obj:
            obj = self.builder().build(self.session).obj
        for (key, val) in self.settable_attrs.iteritems():
            obj.__setattr__(key, val)
            obj, = session_reload(s, obj)
            obj_val = obj.__getattribute__(key)
            if isinstance(val, DomainObject):
                val = instance_pk(val)
                obj_val = instance_pk(obj_val)
            self.assertEqual(val, obj_val,
                             msg="%s set to %s instead of %s" % (key, repr(obj_val), repr(val)))


class TestOrderAttributes(TestObjectAttributes):
    """Test case for Orders attributes"""

    builder = OrderBuilder
    # According to
    # https://wiki.yandex-team.ru/Balance/DBdescription/Orders#h18265-11
    defaults = {'consume_sum': 0,
                'completion_sum': 0,
                'consume_qty': 0,
                'completion_qty': 0,
                'seqnum': 0}
    settable_attrs = {'completion_sum': 20,
                      'completion_qty': 10,
                      'text': 'New text',
                      'begin_dt': datetime.datetime(2108, 6, 7, 10, 0)}

    def test_defaults(self): return self._test_defaults()

    def test_attr_setting(self): return self._test_attr_setting()

    def test_shipment_creation(self):
        o = OrderBuilder().build(self.session).obj
        assert o.shipment


class TestRequestAttributes(TestObjectAttributes):
    """Test case for Request attributes"""

    builder = RequestBuilder
    defaults = {  # 'client': set by setUp
        'desired_invoice_dt': datetime.datetime(2006, 2, 2),
    }

    @property
    def settable_attrs(self):
        passport_id = PassportBuilder.construct(self.session).passport_id
        return {
            'passport_id': passport_id,
            'desired_invoice_dt': datetime.datetime(2108, 6, 7, 10, 0)
        }

    def test_defaults(self):
        s = self.session
        obj_builder = self.builder(basket=BasketBuilder(dt=datetime.datetime(2006, 2, 2)))
        obj = obj_builder.build(s).obj
        self.defaults['client'] = obj.client
        self._test_defaults(obj)
        dbrows = set((row.order.id, int(row.quantity), int(row.u_discount_pct))
                     for row in obj.request_orders)
        constrows = set((row.order.id, int(row.quantity), int(row.desired_discount_pct)) for row in
                        obj_builder.b.basket.rows.obj)
        self.assertEqual(dbrows, constrows)
        for row in obj.request_orders:
            self.assertEqual(row.request, obj)

    def test_attr_setting(self):
        self._test_attr_setting()

    # Now two tests to check if discount_pct is set correctly
    # depending on u_discount_pct (in request_order) and manual_discount
    # (in product)

    def test_discount_pct1(self):
        s = self.session
        order_builder = OrderBuilder()
        order_builder.b.product.b.manual_discount = 0
        order = order_builder.build(s).obj
        # row format is (order, quantity, price, discount_pct)
        request = self.builder(basket=BasketBuilder(client=order.client,
                                                    rows=[BasketItemBuilder(order=order, quantity=1,
                                                                            desired_discount_pct=10)])).build(s)
        invoice = InvoiceBuilder(request=request).build(self.session).obj
        self.assertEqual(invoice.invoice_orders[0].discount_pct, 0)


class TestInvoiceAttributes(TestObjectAttributes):
    """Test case for Invoice attributes"""

    builder = InvoiceBuilder
    defaults = {'credit': 0,
                'consume_sum': 0,
                'hidden': 0,
                'market_postpay': 0,
                'overdraft': 0,
                'receipt_sum': 0,
                'status_id': 0,
                'nds': 1,
                'nds_pct': 20}
    settable_attrs = {'agency_discount_pct': 1,
                      'credit': 1,
                      'currency_rate': 36,
                      'currency': 'RUR',
                      'dt': datetime.datetime(2108, 6, 7, 10, 0),
                      'effective_sum': 100,
                      'usd_rate': 28,
                      'internal_rate': 30,
                      'hidden': 2,
                      'market_postpay': 1,
                      'nds': 1,
                      'nds_pct': 17,
                      'overdraft': 1,
                      'status_id': 2,
                      'rur_sum': 10000,
                      'total_sum': 11000}

    def test_attr_setting(self):
        return self._test_attr_setting()

    def try_clients(self, client_head, clients_row, do_equivalent=True):
        """Try to create invoice for given clients and equivalent copies"""
        self._try_clients(client_head, clients_row)
        if do_equivalent:
            self._try_equivalent_clients(client_head, clients_row)

    def _try_equivalent_clients(self, client_head, clients_row):
        def equivalent_client(client):
            if isinstance(client, (list, tuple)):
                return [equivalent_client(cl) for cl in client]
            if isinstance(client, ClientBuilder):
                client = client.obj
            eq_client = ClientBuilder.construct(self.session, is_agency=client.is_agency)
            if not client.is_agency and client.agency:
                eq_client.attach_to_agency(client.agency)
            eq_client.make_equivalent(client)
            return eq_client

        self._try_clients(equivalent_client(client_head), equivalent_client(clients_row))

    def _try_clients(self, client_head, clients_row):
        """Try to create in invoice with given client in the header and
           given clients in its rows"""
        product = ProductBuilder()
        request = RequestBuilder(basket=BasketBuilder(client=client_head, rows=[
            BasketItemBuilder(
                order=(OrderBuilder(client=cl[0], agency=cl[1], product=product) if isinstance(cl, (list, tuple))
                       else OrderBuilder(client=cl, product=product)), quantity=2, desired_discount_pct=10)
            for cl in clients_row]))
        invoice = InvoiceBuilder(request=request).build(self.session).obj
        self._test_defaults(invoice)

    # Tests for creation of invoices with various combinations of clients
    # in header and rows

    def test_create_with_clients1(self):
        # Header: agency
        # Rows: itself + its subclients
        agency1 = Client(is_agency=1)
        subclient11 = Client(agency=agency1)
        subclient12 = Client(agency=agency1)
        self.try_clients(agency1, [agency1, (subclient11, agency1), (subclient12, agency1)])

    def test_create_with_clients2(self):
        # Header: agency
        # Rows: another agency
        agency1 = Client(is_agency=1)
        subclient11 = Client(agency=agency1)
        agency2 = Client(is_agency=1)
        self.assertRaises(exc.INVALID_PARAM, self.try_clients, agency1, [subclient11, agency2])

    def test_create_with_clients3(self):
        # Header: agency
        # Rows: direct client
        client1 = Client()
        agency1 = Client(is_agency=1)
        subclient11 = Client(agency=agency1)
        self.assertRaises(exc.INVALID_PARAM, self.try_clients, agency1, [subclient11, client1])

    def test_create_with_clients4(self):
        # Header: agency
        # Rows: anoter agency's subclient
        agency1 = Client(is_agency=1)
        subclient11 = Client(agency=agency1)
        agency2 = Client(is_agency=1)
        subclient21 = Client(agency=agency2)
        self.assertRaises(exc.INVALID_PARAM, self.try_clients, agency1, [subclient11, subclient21])

    def test_create_with_clients5(self):
        # Header: direct client
        # Rows: itself x3
        client = Client()
        self._try_clients(client, [client] * 3)

    def test_create_with_clients6(self):
        # Header: direct client
        # Rows: another direct client
        client1, client2 = Client(), Client()
        self.assertRaises(exc.INVALID_PARAM, self.try_clients, client1, [client1, client2])

    def test_create_with_clients7(self):
        # Header: direct client
        # Rows: agency
        client, agency = Client(), Client(is_agency=1)
        self.assertRaises(exc.INVALID_PARAM, self.try_clients, client, [client, agency])

    def test_create_with_clients8(self):
        # Header: direct client
        # Rows: some agency's subclient
        client, agency = Client(), Client(is_agency=1)
        subclient = Client(agency=agency)
        self.assertRaises(exc.INVALID_PARAM, self.try_clients, client, [client, subclient])

    def test_create_with_clients9(self):
        # Header: subclient
        # Rows: anything (itself)
        agency = Client(is_agency=1)
        subclient = Client(agency=agency)
        self.try_clients(subclient, [subclient, subclient])
        # self.assertRaises(exc.INVALID_PARAM, self.try_clients, subclient, [subclient, subclient])

    def test_create_with_clients10(self):
        # Header: no client
        # Rows: one client 3 times
        client = Client()
        self.try_clients(client, [client] * 3)

    def test_create_with_clients11(self):
        # Header: no client
        # Rows: 2 clients + 1 subclient
        client = Client()
        agency = Client(is_agency=1)
        subclient = Client(agency=agency)
        self.assertRaises(exc.INVALID_PARAM, self.try_clients, client, [client, client, subclient])

    def test_create_with_clients12(self):
        # Header: no client
        # Rows: 2 clients + 1 agency
        client = Client()
        agency = Client(is_agency=1)
        self.assertRaises(exc.INVALID_PARAM, self.try_clients, client, [client, client, agency])

    def test_create_with_clients13(self):
        # Subagencies
        agency = ClientBuilder(name='agency', is_agency=1)
        client = ClientBuilder(name='client').build(self.session)  # we have freedom of clients
        superagency = ClientBuilder(name='superagency', is_agency=1)
        con = ContractBuilder(client=superagency)
        bsub = GenericBuilder(Subagency, client=agency, agency_contract=con)
        superagency._other.bsub = bsub
        bsub.build(self.session)
        self.try_clients(superagency, [[client, agency]], do_equivalent=False)

    def test_offer_type_id(self):
        invoice = self.builder().build(self.session).obj
        self.assertIsNotNone(invoice.offer_type_id)
        self.assertEqual(invoice.offer_type_id, invoice.offer_type)
        self.assertEqual(invoice.get_contractless_offer_type(), offer.from_invoice(invoice))


class TestCanManualTurnOn(object):
    def test_charge_note(self, session, contract):
        order = _create_order(session, contract.client)
        request = _create_request(session, contract.client, [(order, 10)])
        paysys = Getter(mapper.Paysys, 1001).build(session).obj
        pa = _pay_on_credit(request, contract)

        invoice = InvoiceFactory.create(
            request,
            paysys,
            contract=contract,
            type='charge_note',
            temporary=False
        )
        session.flush()
        assert invoice.can_manual_turn_on is False

    @pytest.mark.parametrize(
        'paysys_id, result',
        [
            (1001, True),
            (1002, False)
        ]
    )
    def test_prepayment_paysys(self, session, client, person, paysys_id, result):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])
        paysys = Getter(mapper.Paysys, paysys_id).build(session).obj
        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()
        assert invoice.can_manual_turn_on is result


class TestQiwiPayments(TestObjectAttributes):
    builder = InvoiceBuilder

    def testCreate(self):
        obj = self.builder(paysys=Getter(Paysys, 1024)).build(self.session).obj
        self.assert_(obj is not None)
        payment = QiwiPayment(obj, '911', datetime.timedelta(days=1))
        self.session.add(payment)
        self.session.flush()
        self.assertEqual('QIWI_CAB', payment.paysys_code)
