# -*- coding: utf-8 -*-

import datetime
import decimal

import pytest
import mock
import hamcrest

from balance import mapper
from balance import core
from balance.actions.invoice_create import InvoiceFactory
from balance.actions import acts as a_a
from balance import muzzle_util as ut
from balance import exc
from balance.constants import *

from tests import object_builder as ob

D = decimal.Decimal


@pytest.fixture
def paysys(request, session):
    id_ = getattr(request, 'param', 1002)
    return ob.Getter(mapper.Paysys, id_).build(session).obj


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def agency(session, client):
    client.is_agency = True
    session.flush()
    return client


@pytest.fixture
def person(request, session, client):
    type = getattr(request, 'param', 'ur')
    return ob.PersonBuilder(client=client, type=type).build(session).obj


@pytest.fixture
def subclients(request, session, agency):
    num = getattr(request, 'param', 1)
    return [
        ob.ClientBuilder(agency=agency, fullname='666').build(session).obj
        for _ in range(num)
    ]


@pytest.fixture
def subclients_nonres(session, subclients):
    for sc in subclients:
        sc.is_sublclient_non_resident = 1
        sc.non_resident_currency_payment = 'USD'
    session.flush()
    return subclients


@pytest.fixture
def subclient(subclients):
    subclient, = subclients
    return subclient


@pytest.fixture
def subclient_nonres(subclients_nonres):
    subclient, = subclients_nonres
    return subclient


def _create_contract(session, agency, person):
    contract = ob.ContractBuilder(
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


@pytest.fixture
def contract(session, agency, person):
    return _create_contract(session, agency, person)


@pytest.fixture
def old_postpay_contract(session, contract):
    contract.col0.personal_account_fictive = 0
    contract.col0.personal_account = 0
    session.flush()
    return contract


def _create_order(session, client, product_id=DIRECT_PRODUCT_ID, product=None, service_id=7):
    product = product or ob.Getter(mapper.Product, product_id).build(session).obj
    return ob.OrderBuilder(
        product=product,
        service=ob.Getter(mapper.Service, service_id),
        client=client,
        agency=client.agency
    ).build(session).obj


def _create_request(session, client, orders_qty=None):
    if orders_qty is None:
        orders_qty = [(_create_order(session, client, 10))]

    basket = ob.BasketBuilder(
        client=client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=qty)
            for o, qty in orders_qty
        ]
    )
    return ob.RequestBuilder(basket=basket).build(session).obj


def _create_request_repayment(session, contract, order, qty, w_fictive=False):
    fictive_request = _create_request(session, contract.client, [(order, qty)])
    fictive = _pay_on_credit(fictive_request, contract)

    repayment_request = _create_request(session, contract.client, [(order, qty)])
    repayment_request.fictive_invoices = [fictive]
    session.flush()

    if w_fictive:
        return repayment_request, fictive_request
    else:
        return repayment_request


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


def _generate_y_invoice(pa, order, qty):
    now = datetime.datetime.now()
    order.calculate_consumption(now, {order.shipment_type: qty})
    act, = a_a.ActAccounter(
        pa.client,
        mapper.ActMonth(for_month=now),
        invoices=[pa.id], dps=[],
        force=1
    ).do()
    invoice = act.invoice
    pa.session.flush()
    return invoice


class TestInvoiceTypes(object):

    def _do_common_asserts(self, invoice, client, person, contract, paysys_id, order=None, sum_=300):
        tpp = invoice.session.query(mapper.TaxPolicy).getone(TAX_POLICY_RUSSIA_RESIDENT)
        paysys = ob.Getter(mapper.Paysys, paysys_id).build(invoice.session).obj

        assert invoice.external_id is not None
        assert invoice.effective_sum == sum_
        assert invoice.total_sum == sum_
        assert invoice.consume_sum == 0
        assert invoice.nds_pct == tpp.pct_by_date(invoice.dt).nds_pct

        assert invoice.client == client
        assert invoice.person == person
        assert invoice.contract == contract
        assert invoice.paysys_id == paysys.id
        assert invoice.currency == paysys.currency
        assert invoice.bank_details_id is not None
        assert invoice.client.mru_paychoice

        if order:
            assert invoice.amount == sum_
            assert invoice.amount_nds is not None
            assert invoice.amount_nsp is not None
            assert len(invoice.invoice_orders) == 1
            assert invoice.invoice_orders[0].order == order
            assert invoice.invoice_orders[0].quantity == sum_ / 30
            assert invoice.invoice_orders[0].amount == sum_
            assert invoice._service_ids == {str(order.service_id): InvoiceServiceCacheType.invoice_orders}
        else:
            assert invoice.amount is None
            assert invoice.amount_nds is None
            assert invoice.amount_nsp is None
            assert len(invoice.invoice_orders) == 0
            assert invoice._service_ids == {'0': 0}

        assert len(invoice.consumes) == 0
        if invoice.person:
            assert invoice.person.firms == [invoice.firm]


    def test_y(self, session, contract, subclient):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])
        pa = _pay_on_credit(request, contract)
        invoice = _generate_y_invoice(pa, order, 10)

        assert isinstance(invoice, mapper.YInvoice)
        assert invoice.credit == 1
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.dt == mapper.ActMonth(for_month=datetime.datetime.now()).document_dt
        assert invoice.exports['OEBS'].state == ExportState.enqueued
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1003, order)

    def test_charge_note_type(self, session, contract, paysys):
        order = _create_order(session, contract.client)
        request = _create_request(session, contract.client, [(order, 10)])
        pa = _pay_on_credit(request, contract)

        invoice = InvoiceFactory.create(
            request,
            paysys,
            contract=contract,
            type='charge_note',
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.ChargeNote)
        assert invoice.credit == 0
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.charge_invoice == pa
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, order)

    def test_charge_note_desired_invoice_type(self, session, contract, paysys):
        order = _create_order(session, contract.client)
        pa_request = _create_request(session, contract.client, [(order, 10)])
        pa = _pay_on_credit(pa_request, contract)

        request = _create_request(session, contract.client, [(order, 10)])
        request.invoice_desired_type = 'charge_note'
        invoice = InvoiceFactory.create(
            request,
            paysys,
            contract=contract,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.ChargeNote)
        assert invoice.credit == 0
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.charge_invoice == pa
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, order)

    @pytest.mark.parametrize('credit', [0, 2], ids=['wo_credit', 'credit_2'])
    def test_charge_note_prepay_personal_account(self, session, contract, paysys, credit):
        contract.col0.personal_account_fictive = 0
        session.flush()

        order = _create_order(session, contract.client)
        request = _create_request(session, contract.client, [(order, 10)])
        pa = _pay_on_credit(request, contract)

        contract.col0.payment_type = PREPAY_PAYMENT_TYPE
        session.flush()

        invoice = InvoiceFactory.create(
            request,
            paysys,
            contract=contract,
            temporary=False,
            credit=credit
        )
        session.flush()

        assert isinstance(invoice, mapper.ChargeNote)
        assert invoice.credit == 0
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.charge_invoice == pa
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, order)

    def test_fpa(self, session, contract, paysys):
        invoice = InvoiceFactory.create(
            request=None,
            paysys=paysys,
            contract=contract,
            postpay=2,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.FictivePersonalAccount)
        assert invoice.credit == 0
        assert invoice.postpay == 1
        assert invoice.overdraft == 0
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, None, 0)

    def test_pa(self, session, contract, paysys):
        invoice = InvoiceFactory.create(
            request=None,
            paysys=paysys,
            contract=contract,
            postpay=1,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.PersonalAccount)
        assert invoice.credit == 0
        assert invoice.postpay == 1
        assert invoice.overdraft == 0
        assert invoice.exports['OEBS'].state == ExportState.enqueued
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, None, 0)

    def test_fictive(self, session, old_postpay_contract, subclient, paysys):
        contract = old_postpay_contract
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])
        alt_request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            orig_request=alt_request,
            contract=old_postpay_contract,
            person=contract.person,
            paysys=paysys,
            credit=2,
            status_id=666,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.FictiveInvoice)
        assert invoice.credit == 2
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.repayment is None
        assert invoice.status_id == 666
        assert invoice.deferpay is not None
        assert invoice.deferpay.effective_sum == 300
        assert invoice.deferpay.orig_request == alt_request
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, order)

    def test_repayment(self, session, old_postpay_contract, subclient, paysys):
        contract = old_postpay_contract
        order = _create_order(session, subclient)
        repayment_request, fictive_request = _create_request_repayment(session, old_postpay_contract, order, 10, True)
        session.flush()

        invoice = InvoiceFactory.create(
            request=repayment_request,
            orig_request=fictive_request,
            contract=contract,
            paysys=paysys,
            credit=1,
            status_id=666,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.RepaymentInvoice)
        assert invoice.credit == 1
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.status_id == 666
        assert invoice.exports['OEBS'].state == ExportState.enqueued
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, order)

    def test_overdraft(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            overdraft=1,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.OverdraftInvoice)
        assert invoice.credit == 0
        assert invoice.postpay == 0
        assert invoice.overdraft == 1
        assert invoice.exports['OEBS'].state == ExportState.enqueued
        self._do_common_asserts(invoice, client, person, None, 1002, order)

    def test_bonus_account(self, session, client, paysys):
        invoice = mapper.BonusAccount.get_or_create_bonus_account(session, client, paysys)

        assert isinstance(invoice, mapper.BonusAccount)
        assert invoice.credit == 0
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.contract is None
        self._do_common_asserts(invoice, client, None, None, 1002, None, 0)

    def test_prepayment_wo_contract(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.Invoice)
        assert invoice.credit == 0
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.exports['OEBS'].state == ExportState.enqueued
        self._do_common_asserts(invoice, client, person, None, 1002, order)

    def test_prepayment_w_contract(self, session, contract, subclient, paysys):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.Invoice)
        assert invoice.credit == 0
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.exports['OEBS'].state == ExportState.enqueued
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, order)

    def test_prepayment_pa_35(self, session, contract, subclient, paysys):
        contract.col0.personal_account_fictive = 0
        contract.col0.services = {7, 35}
        session.flush()

        order = _create_order(session, subclient, service_id=35)
        request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            postpay=1,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.Invoice)
        assert invoice.credit == 0
        assert invoice.postpay == 0
        assert invoice.overdraft == 0
        assert invoice.exports['OEBS'].state == ExportState.enqueued
        self._do_common_asserts(invoice, contract.client, contract.person, contract, 1002, order)


@pytest.mark.charge_note_register
class TestChargeInvoiceRegisterCreation(object):
    def _do_common_asserts(self, invoice, client, person, contract, paysys_id, sum_):
        assert isinstance(invoice, mapper.ChargeNoteRegister)

        paysys = ob.Getter(mapper.Paysys, paysys_id).build(invoice.session).obj
        tp = invoice.session.query(mapper.TaxPolicy).getone(TAX_POLICY_RUSSIA_RESIDENT)
        tpp = tp.pct_by_date(invoice.dt)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                external_id=hamcrest.is_not(None),
                client=client,
                person=person,
                contract=contract,
                currency=paysys.currency,
                bank_details_id=hamcrest.is_not(None),
                effective_sum=sum_,
                total_sum=sum_,
                amount=sum_,
                amount_nds=tpp.nds_from(sum_),
                amount_nsp=0,
                consume_sum=0,
                nds_pct=tpp.nds_pct,
            )
        )

    def test_with_fictive_personal_account(self, session, contract, subclient, paysys):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])
        pa = _pay_on_credit(request, contract)
        y = _generate_y_invoice(pa, order, 10)

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=8)],
                register_rows=[
                    ob.BasketRegisterRowBuilder(ref_invoice=y),
                ]
            )
        )

        with pytest.raises(exc.REGISTER_WITH_FICTIVE_ACCOUNT):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                type='charge_note_register',
                temporary=False
            )

    def test_with_old_personal_account(self, session, contract, subclient, paysys):
        contract.col0.personal_account_fictive = 0
        client = contract.client
        person = contract.person
        order = _create_order(session, subclient)

        request1 = _create_request(session, client, [(order, 4)])
        request2 = _create_request(session, client, [(order, 6)])
        old_invoice1 = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request1)
        old_invoice2 = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request2)

        pa = _pay_on_credit(request1, contract)

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=8)],
                register_rows=[
                    ob.BasketRegisterRowBuilder(ref_invoice=old_invoice1),
                    ob.BasketRegisterRowBuilder(ref_invoice=old_invoice2),
                ]
            )
        )

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            type='charge_note_register',
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.ChargeNoteRegister)
        assert invoice.charge_invoice is pa
        assert invoice._service_ids == {str(order.service_id): InvoiceServiceCacheType.invoice_orders}

        self._do_common_asserts(invoice, contract.client, contract.person, contract, paysys.id, 540)
        hamcrest.assert_that(
            invoice.invoice_orders,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    order=order,
                    quantity=8,
                    amount=8 * 30,
                )
            )
        )
        hamcrest.assert_that(
            invoice.register_rows,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    ref_invoice=old_invoice1,
                    amount=120,
                    is_internal=0
                ),
                hamcrest.has_properties(
                    ref_invoice=old_invoice2,
                    amount=180,
                    is_internal=0
                ),
                hamcrest.has_properties(
                    ref_invoice=pa,
                    amount=240,
                    amount_nds=40,
                    is_internal=1
                )
            )
        )

    def test_without_personal_account(self, session, client, person, paysys):
        order = _create_order(session, client)
        request1 = _create_request(session, client, [(order, 4)])
        request2 = _create_request(session, client, [(order, 6)])
        old_invoice1 = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request1)
        old_invoice2 = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request2)

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=client,
                rows=[],
                register_rows=[
                    ob.BasketRegisterRowBuilder(ref_invoice=old_invoice1),
                    ob.BasketRegisterRowBuilder(ref_invoice=old_invoice2),
                ]
            )
        )

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            type='charge_note_register',
            temporary=False
        )
        session.flush()

        assert invoice.charge_invoice is None
        assert invoice._service_ids == {str(order.service_id): InvoiceServiceCacheType.invoice_orders}

        self._do_common_asserts(invoice, client, person, None, paysys.id, 300)
        hamcrest.assert_that(invoice.invoice_orders, hamcrest.empty())
        hamcrest.assert_that(
            invoice.register_rows,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    ref_invoice=old_invoice1,
                    amount=120,
                    is_internal=0
                ),
                hamcrest.has_properties(
                    ref_invoice=old_invoice2,
                    amount=180,
                    is_internal=0
                ),
            )
        )

    @pytest.mark.parametrize(
        'paysys_id, is_instant',
        [
            pytest.param(1001, 0, id='bank'),
            pytest.param(1002, 1, id='card'),
        ]
    )
    def test_paysys_type(self, session, contract, subclient, paysys_id, is_instant):
        paysys = ob.Getter(mapper.Paysys, paysys_id).build(session).obj
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])
        pa = _pay_on_credit(request, contract)
        y = _generate_y_invoice(pa, order, 7)

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[],
                register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=y)]
            )
        )

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            type='charge_note_register',
            temporary=False
        )
        session.flush()

        self._do_common_asserts(invoice, contract.client, contract.person, contract, paysys.id, 210)

        if is_instant:
            assert not invoice.payments
            assert invoice.external_id.startswith(u'ОПЛ-')
            assert 'OEBS' not in invoice.exports
        else:
            payment, = invoice.payments
            assert isinstance(payment, mapper.BankPayment)
            assert payment.amount == 210
            assert invoice.external_id == u'ОПЛ-%s' % payment.id
            assert invoice.exports['OEBS'].state == ExportState.enqueued

    def test_without_personal_account_with_orders(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])
        old_invoice = ob.InvoiceBuilder(
            paysys=paysys,
            person=person,
            request=request
        ).build(session).obj

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=1)],
                register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=old_invoice)]
            )
        )

        with pytest.raises(exc.INVOICE_NOT_FOUND) as exc_info:
            InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys,
                type='charge_note_register',
                temporary=False
            )
        assert 'Invoice with ID Personal account with' in exc_info.value.msg

    def test_same_person(self, session, client, paysys):
        person1 = ob.PersonBuilder.construct(session, client=client, type='ph')
        person2 = ob.PersonBuilder.construct(session, client=client, type='ph')

        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])
        old_invoice = ob.InvoiceBuilder(
            paysys=paysys,
            person=person1,
            request=request
        ).build(session).obj

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=client,
                rows=[],
                register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=old_invoice)]
            )
        )

        with pytest.raises(exc.MULTIPLE_REGISTER_PERSONS):
            InvoiceFactory.create(
                request=request,
                person=person2,
                paysys=paysys,
                type='charge_note_register',
                temporary=False
            )

    def test_same_currency(self, session, client):
        person = ob.PersonBuilder.construct(session, client=client, type='ur')
        paysys1 = ob.Getter(mapper.Paysys, 1003).build(session).obj
        paysys2 = ob.Getter(mapper.Paysys, 1011).build(session).obj

        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])
        old_invoice = ob.InvoiceBuilder(
            paysys=paysys1,
            person=person,
            request=request
        ).build(session).obj

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=client,
                rows=[],
                register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=old_invoice)]
            )
        )

        with pytest.raises(exc.MULTIPLE_REGISTER_CURRENCIES):
            InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys2,
                type='charge_note_register',
                temporary=False
            )

    def test_same_firm(self, session, client):
        person = ob.PersonBuilder.construct(session, client=client, type='ur')
        paysys = ob.Getter(mapper.Paysys, 1003).build(session).obj

        order_direct = _create_order(session, client, service_id=ServiceId.DIRECT)
        request_direct = _create_request(session, client, [(order_direct, 10)])
        invoice_direct = ob.InvoiceBuilder(
            paysys=paysys,
            person=person,
            request=request_direct
        ).build(session).obj

        order_market = _create_order(session, client, service_id=ServiceId.MARKET)
        request_market = _create_request(session, client, [(order_market, 10)])
        invoice_market = ob.InvoiceBuilder(
            paysys=paysys,
            person=person,
            request=request_market
        ).build(session).obj

        request = ob.RequestBuilder.construct(
            session,
            firm_id=FirmId.YANDEX_OOO,
            basket=ob.BasketBuilder(
                client=client,
                rows=[],
                register_rows=[
                    ob.BasketRegisterRowBuilder(ref_invoice=invoice_direct),
                    ob.BasketRegisterRowBuilder(ref_invoice=invoice_market),
                ]
            )
        )

        with pytest.raises(exc.MULTIPLE_REGISTER_FIRMS):
            InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys,
                type='charge_note_register',
                temporary=False
            )

    def test_same_tax(self, session, client, person, paysys):
        invoices = []
        for pct in [20, 30]:
            tax_policy = ob.TaxPolicyBuilder.construct(session, tax_pcts=[pct])
            product = ob.ProductBuilder.construct(session, taxes=tax_policy, prices=[('RUR', 100)])
            order = _create_order(session, client, product.id)
            request = _create_request(session, client, [(order, 10)])
            old_invoice = ob.InvoiceBuilder.construct(session, paysys=paysys, person=person, request=request)
            invoices.append(old_invoice)

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=client,
                rows=[],
                register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=i) for i in invoices]
            )
        )

        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys,
                type='charge_note_register',
                temporary=False
            )
        assert 'Attempt to create invoice with different taxes' in exc_info.value.msg


class TestCustomAttributes(object):
    def test_default_state(self, session, contract, subclient, paysys):
        order = _create_order(session, subclient)
        start_dt = datetime.datetime.now().replace(microsecond=0)
        request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        end_dt = datetime.datetime.now()
        session.flush()
        assert invoice.crossfirm is False
        assert invoice.with_begin_dt is False
        assert invoice.full_render is False
        assert invoice.only_actable_at_month_close is False
        assert invoice.is_docs_detailed is False
        assert invoice.is_docs_separated is False
        assert invoice.is_interservice_transfer_disallowed is False
        assert invoice.is_sublclient_non_resident is False
        assert invoice.subclient_non_resident_currency_payment is None
        assert start_dt <= invoice.dt <= end_dt
        assert invoice.discount_type == DIRECT_DISCOUNT_TYPE
        assert invoice.commission_type == DIRECT_DISCOUNT_TYPE

    def test_crossfirm(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            crossfirm=1,
            temporary=False
        )
        session.flush()
        assert invoice.crossfirm == 1
        assert invoice.discount_type == DIRECT_DISCOUNT_TYPE
        assert invoice.commission_type == 0

    def test_crossfirm_multiple_commissions(self, session, client, person, paysys):
        order1 = _create_order(session, client, DIRECT_PRODUCT_RUB_ID)
        order2 = _create_order(session, client, DIRECT_MEDIA_PRODUCT_RUB_ID)
        request = _create_request(session, client, [(order1, 10), (order2, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            crossfirm=1,
            temporary=False
        )
        session.flush()
        assert invoice.crossfirm == 1
        assert invoice.discount_type == DIRECT_DISCOUNT_TYPE
        assert invoice.commission_type == 0

    def test_with_begin_dt(self, session, old_postpay_contract, subclient, paysys):
        contract = old_postpay_contract
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])
        alt_request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            orig_request=alt_request,
            contract=old_postpay_contract,
            person=contract.person,
            paysys=paysys,
            credit=2,
            with_begin_dt=1,
            temporary=False
        )
        session.flush()

        assert invoice.with_begin_dt is True

    def test_subclient_id_default(self, session, contract, paysys):
        invoice = InvoiceFactory.create(
            request=None,
            paysys=paysys,
            contract=contract,
            postpay=2,
            temporary=False
        )
        session.flush()

        assert invoice.subclient_id is None
        assert invoice.subclient is None

    def test_subclient_id(self, session, contract, subclient, paysys):
        invoice = InvoiceFactory.create(
            request=None,
            paysys=paysys,
            contract=contract,
            postpay=2,
            subclient_id=subclient.id,
            temporary=False
        )
        session.flush()

        assert invoice.subclient_id == subclient.id
        assert invoice.subclient == subclient

    def test_full_render(self, session, client, person, paysys):
        product = ob.ProductBuilder(
            full_render=1,
            media_discount=7,
            commission_type=7,
        ).build(session).obj
        order1 = _create_order(session, client, product=product)
        order2 = _create_order(session, client)
        request = _create_request(session, client, [(order1, 1), (order2, 1)])

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()
        assert invoice.full_render == 1

    def test_only_actable_at_month_close(self, session, contract, subclient, paysys):
        contract.col0.supercommission_bonus = {150}
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.only_actable_at_month_close is True

    def test_is_docs_detailed_client(self, session, client, person, paysys):
        client.is_docs_detailed = 1
        client.is_docs_separated = 1
        session.flush()

        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.is_docs_detailed is True
        assert invoice.is_docs_separated is True

    def test_is_docs_detailed_agency(self, session, contract, subclient, paysys):
        subclient.agencies_printable_doc_types = {contract.client_id: (1, 1)}
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.is_docs_detailed is True
        assert invoice.is_docs_separated is True

    def test_is_interservice_transfer_disallowed(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 1)])

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            overdraft=1,
            temporary=False
        )
        session.flush()

        assert invoice.is_interservice_transfer_disallowed is True

    @pytest.mark.parametrize('subclients', [2], indirect=['subclients'], ids=['^_^'])
    def test_subclient_nonresident(self, session, contract, subclients_nonres, paysys):
        orders = []
        for subclient in subclients_nonres:
            orders.append((_create_order(session, subclient), 10))

        contract.col0.non_resident_clients = 1
        session.flush()

        request = _create_request(session, contract.client, orders)

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.is_sublclient_non_resident is True
        assert invoice.subclient_non_resident_currency_payment == 'USD'

    def test_subclient_nonresident_partner(self, session, contract, subclient_nonres, paysys):
        contract.col0.non_resident_clients = 1
        contract.col0.services = {ServiceId.MUSIC}
        session.flush()

        order = _create_order(session, subclient_nonres, service_id=ServiceId.MUSIC)
        request = _create_request(session, contract.client, [(order, 1)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.is_sublclient_non_resident is False
        assert invoice.subclient_non_resident_currency_payment is None

    def test_dt(self, session, contract, subclient, paysys):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])
        dt = ut.trunc_date(datetime.datetime.now() - datetime.timedelta(10))

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False,
            dt=dt
        )
        session.flush()

        assert invoice.dt == dt

    def test_request_invoice_dt(self, session, contract, subclient, paysys):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])
        dt = ut.trunc_date(datetime.datetime.now() - datetime.timedelta(10))
        request.desired_invoice_dt = dt
        session.flush()

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False,
            dt=dt
        )
        session.flush()

        assert invoice.dt == dt


class TestChecks(object):
    def test_check_prepay_pa(self, session, contract, subclient, paysys):
        contract.col0.personal_account_fictive = 0
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])

        with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                temporary=False
            )

    def test_check_prepay_pa_crossfirm(self, session, contract, subclient, paysys):
        contract.col0.personal_account_fictive = 0
        contract.col0.services = {35}
        contract.col0.commission = 61
        contract.col0.commission_type = None
        session.flush()

        order = _create_order(session, subclient, service_id=35)
        request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            crossfirm=1,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.Invoice)
        assert invoice.contract == contract
        assert invoice.effective_sum == 300
        assert invoice.crossfirm == 1

    def test_check_prepay_fpa(self, session, contract, subclient, paysys):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        assert isinstance(invoice, mapper.Invoice)
        assert invoice.effective_sum == 300
        assert invoice.contract == contract

    def test_check_prepay_fpa_taxi(self, session, contract, subclient, paysys):
        contract.col0.firm = FirmId.TAXI
        del contract.__dict__['firm']
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])

        with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                temporary=False
            )

    @pytest.mark.parametrize('subclients', [2], indirect=['subclients'], ids=['^_^'])
    def test_check_subclient_nonresident_rows_diff_state(self, session, contract, subclients, paysys):
        sc1, sc2 = subclients
        sc1.non_resident_currency_payment = 'USD'
        sc1.is_sublclient_non_resident = 1

        contract.col0.non_resident_clients = 1
        session.flush()

        o1 = _create_order(session, sc1)
        o2 = _create_order(session, sc2)
        request = _create_request(session, contract.client, [(o1, 1), (o2, 1)])

        with pytest.raises(exc.INVOICE_HAS_MIXED_NON_RESIDENT):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                temporary=False
            )

    @pytest.mark.parametrize('subclients', [2], indirect=['subclients'], ids=['^_^'])
    def test_check_subclient_nonresident_rows_diff_currency(self, session, contract, subclients, paysys):
        sc1, sc2 = subclients
        sc1.non_resident_currency_payment = 'USD'
        sc1.is_sublclient_non_resident = 1
        sc2.non_resident_currency_payment = 'EUR'
        sc2.is_sublclient_non_resident = 1

        contract.col0.non_resident_clients = 1
        session.flush()

        o1 = _create_order(session, sc1)
        o2 = _create_order(session, sc2)
        request = _create_request(session, contract.client, [(o1, 1), (o2, 1)])

        with pytest.raises(exc.INVOICE_HAS_MIXED_NON_RESIDENT):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                temporary=False
            )

    def test_check_subclient_nonresident_wo_contract(self, session, client, person, paysys):
        client.non_resident_currency_payment = 'USD'
        client.is_sublclient_non_resident = 1
        client.fullname = '666'
        session.flush()

        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 1)])

        with pytest.raises(exc.INVOICE_HASNOT_NON_RESIDENT_CONTRACT):
            InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys,
                temporary=False
            )

    def test_check_subclient_nonresident_wrong_contract(self, session, contract, subclient_nonres, paysys):
        order = _create_order(session, subclient_nonres)
        request = _create_request(session, contract.client, [(order, 1)])

        with pytest.raises(exc.INVOICE_HASNOT_NON_RESIDENT_CONTRACT):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                temporary=False
            )

    def test_check_subclient_nonresident_partner(self, session, client, person, paysys):
        client.non_resident_currency_payment = 'USD'
        client.is_sublclient_non_resident = 1
        client.fullname = '666'
        session.flush()

        order = _create_order(session, client, service_id=ServiceId.MUSIC)
        request = _create_request(session, client, [(order, 1)])

        invoice =InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.Invoice)
        assert invoice.client == client
        assert invoice.is_sublclient_non_resident is False
        assert invoice.subclient_non_resident_currency_payment is None

    def test_check_deal_passport_ofert(self, session, client, person, paysys):
        session.config.__dict__['DEAL_PASSPORT_OFERT_LIMIT'] = 666
        person.inn = '999666'
        session.flush()

        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 100)])

        with pytest.raises(exc.DEAL_PASSPORT_REQUIRED):
            InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys,
                temporary=False
            )

    def test_check_deal_passport_contract(self, session, contract, subclient, paysys):
        session.config.__dict__['DEAL_PASSPORT_OFERT_LIMIT'] = 666
        contract.person.inn = '999666'
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 100)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.Invoice)
        assert invoice.effective_sum == 3000

    def test_check_comm_type_services(self, session, contract, subclient, paysys):
        contract.col0.services = {70}
        session.flush()

        order = _create_order(session, subclient, service_id=70)
        request = _create_request(session, contract.client, [(order, 100)])

        with pytest.raises(exc.COMMISSION_TYPE_NOT_ALLOWED_BY_CONTRACT):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                temporary=False
            )

    def test_check_comm_type_services_crossfirm(self, session, contract, subclient, paysys):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 100)])

        with pytest.raises(exc.CROSSFIRM_NOT_ALLOWED_WITH_COMMISSION_CONTRACT):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                crossfirm=1,
                temporary=False
            )

    def test_check_comm_type_contract(self, session, contract, subclient, paysys):
        contract.col0.commission_type = 37
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 100)])

        with pytest.raises(exc.COMMISSION_TYPE_NOT_ALLOWED_BY_CONTRACT):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                temporary=False
            )


class TestFirmSelect(object):
    def test_forced_firm(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 1)])
        firm = ob.Getter(mapper.Firm, FirmId.MARKET).build(session).obj

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            firm=firm,
            temporary=False
        )
        session.flush()

        assert invoice.firm_id == FirmId.MARKET

    def test_request_firm(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 1)])
        request.firm_id = FirmId.TAXI
        session.flush()

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.firm_id == FirmId.TAXI

    def test_contract_firm(self, session, contract, subclient, paysys):
        contract.col0.firm = FirmId.VERTIKALI
        del contract.__dict__['firm']
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 1)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.firm_id == FirmId.VERTIKALI

    def test_contract_mismatch(self, session, contract, subclient, paysys):
        contract.col0.firm = FirmId.VERTIKALI
        del contract.__dict__['firm']
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 1)])
        firm = ob.Getter(mapper.Firm, 111).build(session).obj

        with pytest.raises(exc.INVALID_PARAM):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                firm=firm,
                temporary=False
            )

    def test_pay_policy(self, session, client, person, paysys):
        order = _create_order(session, client, service_id=11)
        request = _create_request(session, client, [(order, 1)])
        session.flush()

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.firm_id == FirmId.MARKET


class TestPaysysAdjustment(object):
    def test_paysys_prepayment(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 1)])
        request.firm_id = FirmId.MARKET

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.paysys.cc == paysys.cc
        assert invoice.paysys.firm_id == FirmId.MARKET

    def test_paysys_prepayment_partners(self, session, client, person, paysys):
        order = _create_order(session, client, service_id=23)
        request = _create_request(session, client, [(order, 1)])
        request.firm_id = FirmId.MARKET

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.firm_id == FirmId.MARKET
        assert invoice.paysys.cc == paysys.cc
        assert invoice.paysys.firm_id == FirmId.YANDEX_OOO

    def test_paysys_fictive_subclient_nonres(self, session, old_postpay_contract, subclient_nonres, paysys):
        old_postpay_contract.col0.non_resident_clients = 1
        session.flush()

        order = _create_order(session, subclient_nonres)
        request = _create_request(session, old_postpay_contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=old_postpay_contract,
            paysys=paysys,
            credit=2,
            temporary=False
        )
        session.flush()

        assert invoice.paysys.cc == 'nr_rur'

    @pytest.mark.parametrize('person', ['yt'], indirect=['person'], ids=['>_>'])
    @pytest.mark.usefixtures('person')
    def test_paysys_fictive_yt(self, session, old_postpay_contract, subclient, paysys):
        order = _create_order(session, subclient)
        request = _create_request(session, old_postpay_contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=old_postpay_contract,
            paysys=paysys,
            credit=2,
            temporary=False
        )
        session.flush()

        assert invoice.paysys.cc == 'rur_wo_nds'

    @pytest.mark.parametrize('person', ['yt'], indirect=['person'], ids=['>_>'])
    def test_paysys_fictive_yt_wo_contract(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            client=client,
            person=person,
            paysys=paysys,
            credit=2,
            temporary=False
        )
        session.flush()

        assert isinstance(invoice, mapper.FictiveInvoice)
        assert invoice.paysys.cc == 'rur_wo_nds'

    def test_paysys_repayment_subclient_nonres(self, session, old_postpay_contract, subclient_nonres, paysys):
        old_postpay_contract.col0.non_resident_clients = 1
        session.flush()

        order = _create_order(session, subclient_nonres)
        repayment_request = _create_request_repayment(session, old_postpay_contract, order, 10)
        session.flush()

        invoice = InvoiceFactory.create(
            request=repayment_request,
            contract=old_postpay_contract,
            paysys=paysys,
            credit=1,
            temporary=False
        )
        session.flush()

        assert invoice.paysys.cc == 'nr_usd'

    @pytest.mark.parametrize('person', ['yt'], indirect=['person'], ids=['>_>'])
    @pytest.mark.usefixtures('person')
    def test_paysys_repayment_yt(self, session, old_postpay_contract, subclient, paysys):
        order = _create_order(session, subclient)
        repayment_request = _create_request_repayment(session, old_postpay_contract, order, 10)
        session.flush()

        invoice = InvoiceFactory.create(
            request=repayment_request,
            contract=old_postpay_contract,
            paysys=paysys,
            credit=1,
            temporary=False
        )
        session.flush()

        assert invoice.paysys.cc == 'rur_wo_nds'

    def test_paysys_yinvoice_subclient_nonres(self, session, contract, subclient_nonres):
        contract.col0.non_resident_clients = 1
        session.flush()

        order = _create_order(session, subclient_nonres)
        request = _create_request(session, contract.client, [(order, 10)])
        pa = _pay_on_credit(request, contract)
        invoice = _generate_y_invoice(pa, order, 10)

        assert invoice.paysys.cc == 'nr_usd'
        assert invoice.currency == 'USD'
        assert invoice.external_id[0] == 'S'

    @pytest.mark.parametrize('person', ['yt'], indirect=['person'], ids=['>_>'])
    @pytest.mark.usefixtures('person')
    def test_paysys_yinvoice_yt(self, session, contract, subclient):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])
        pa = _pay_on_credit(request, contract)
        invoice = _generate_y_invoice(pa, order, 10)

        assert invoice.paysys.cc == 'rur_wo_nds'


class TestContractAssignment(object):
    def test_subagency_ofert(self, session, contract, paysys):
        subagency = ob.ClientBuilder(is_agency=1).build(session).obj
        subclient = ob.ClientBuilder(agency=subagency).build(session).obj
        subperson = ob.PersonBuilder(client=subagency, type='ur').build(session).obj

        subagency_obj = mapper.Subagency(client=subagency, agency_contract=contract)
        session.add(subagency_obj)
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, subagency, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            person=subperson,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.contract == contract
        assert invoice.client == contract.client
        assert invoice.person == subperson

    def test_subagency_contract(self, session, contract, paysys):
        subagency = ob.ClientBuilder(is_agency=1).build(session).obj
        subclient = ob.ClientBuilder(agency=subagency).build(session).obj
        subperson = ob.PersonBuilder(client=subagency, type='ur').build(session).obj
        subcontract = _create_contract(session, subagency, subperson)

        subagency_obj = mapper.Subagency(client=subagency, agency_contract=contract)
        session.add(subagency_obj)
        session.flush()

        order = _create_order(session, subclient)
        request = _create_request(session, subagency, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=subcontract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.contract == contract
        assert invoice.client == contract.client
        assert invoice.person == subperson

    def test_prepayment_not_matching(self, session, contract, paysys, subclient):
        contract.col0.commission = 61
        contract.col0.commission_type = None
        session.flush()

        order = _create_order(session, subclient, service_id=70)
        request = _create_request(session, contract.client, [(order, 10)])

        with pytest.raises(exc.ILLEGAL_CONTRACT):
             InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                temporary=False
            )

    def test_overdraft(self, session, contract, paysys, subclient):
        order = _create_order(session, subclient)
        request = _create_request(session, contract.client, [(order, 10)])

        with pytest.raises(exc.CANNOT_ASSIGN_CONTRACT_TO_OVERDRAFT):
            InvoiceFactory.create(
                request=request,
                contract=contract,
                paysys=paysys,
                overdraft=1,
                temporary=False
            )

    def test_partner(self, session, contract, paysys, subclient):
        contract.col0.commission = 61
        contract.col0.commission_type = None
        session.flush()

        order = _create_order(session, subclient, service_id=23)
        request = _create_request(session, contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=contract,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.contract == contract

    def test_fictive_wo_parent_dp(self, session, old_postpay_contract, subclient, paysys):
        old_postpay_contract.col0.commission = 61
        old_postpay_contract.col0.commission_type = None
        session.flush()

        order = _create_order(session, subclient, service_id=70)
        request = _create_request(session, old_postpay_contract.client, [(order, 10)])

        with pytest.raises(exc.ILLEGAL_CONTRACT):
            InvoiceFactory.create(
                request=request,
                contract=old_postpay_contract,
                paysys=paysys,
                credit=2,
                temporary=False
            )

    def test_repayment_w_parent_dp(self, session, old_postpay_contract, subclient, paysys):
        old_postpay_contract.col0.commission = 61
        old_postpay_contract.col0.commission_type = None
        old_postpay_contract.col0.services = {70}
        session.flush()

        order = _create_order(session, subclient, service_id=70)
        prev_request = _create_request(session, old_postpay_contract.client, [(order, 10)])
        prev_fictive = _pay_on_credit(prev_request, old_postpay_contract)

        old_postpay_contract.col0.services = {7}
        session.flush()

        request = _create_request(session, old_postpay_contract.client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            contract=old_postpay_contract,
            paysys=paysys,
            credit=2,
            parent_deferpay=prev_fictive.deferpay,
            temporary=False,
        )
        session.flush()

        assert invoice.contract == old_postpay_contract
        assert invoice.deferpay.parent_deferpay == prev_fictive.deferpay

    def test_repayment_w_full_repayment(self, session, old_postpay_contract, subclient, paysys):
        order = _create_order(session, subclient)
        repayment_request = _create_request_repayment(session, old_postpay_contract, order, 10)

        old_postpay_contract.col0.services = {70}
        old_postpay_contract.col0.repayment_on_consume = False
        old_postpay_contract.client.full_repayment = 1
        session.flush()

        with pytest.raises(exc.ILLEGAL_CONTRACT):
            InvoiceFactory.create(
                request=repayment_request,
                contract=old_postpay_contract,
                paysys=paysys,
                credit=1,
                temporary=False
            )

    def test_repayment_wo_full_repayment(self, session, old_postpay_contract, subclient, paysys):
        order = _create_order(session, subclient)
        repayment_request = _create_request_repayment(session, old_postpay_contract, order, 10)

        old_postpay_contract.col0.services = {70}
        old_postpay_contract.col0.repayment_on_consume = False
        old_postpay_contract.client.full_repayment = 0
        session.flush()

        invoice = InvoiceFactory.create(
            request=repayment_request,
            contract=old_postpay_contract,
            paysys=paysys,
            credit=1,
            temporary=False
        )

        assert invoice.contract == old_postpay_contract

    def test_repayment_w_repayment_on_consume(self, session, old_postpay_contract, subclient, paysys):
        order = _create_order(session, subclient)
        repayment_request = _create_request_repayment(session, old_postpay_contract, order, 10)

        old_postpay_contract.col0.services = {70}
        old_postpay_contract.col0.repayment_on_consume = True
        old_postpay_contract.client.full_repayment = 1
        session.flush()

        invoice = InvoiceFactory.create(
            request=repayment_request,
            contract=old_postpay_contract,
            paysys=paysys,
            credit=1,
            temporary=False
        )

        assert invoice.contract == old_postpay_contract


class TestPatchQty(object):
    def test_factory_flag(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            patch_amount_to_qty=True,
            temporary=False
        )
        session.flush()

        assert invoice.effective_sum == 10
        assert invoice.invoice_orders[0].amount == 10
        assert invoice.invoice_orders[0].quantity == ut.round(D(10) / D(30), 6)

    def test_request_patch_amount_to_qty(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])
        request.patch_amount_to_qty = 1
        session.flush()

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.effective_sum == 10
        assert invoice.invoice_orders[0].amount == 10
        assert invoice.invoice_orders[0].quantity == ut.round(D(10) / D(30), 6)

    def test_force_amount(self, session, client, person, paysys):
        order = _create_order(session, client)
        request = _create_request(session, client, [(order, 10)])
        request.force_amount = 1
        session.flush()

        invoice = InvoiceFactory.create(
            request=request,
            person=person,
            paysys=paysys,
            temporary=False
        )
        session.flush()

        assert invoice.effective_sum == 10
        assert invoice.invoice_orders[0].amount == 10
        assert invoice.invoice_orders[0].quantity == ut.round(D(10) / D(30), 6)


class TestSeparatedDocuments(object):

    @staticmethod
    def _setup_agency_with_clients(session):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        ob.PersonBuilder.construct(session, client=agency)
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)
        order1 = _create_order(session, client1)
        order2 = _create_order(session, client2)
        request = _create_request(session, agency, [(order1, 10), (order2, 20)])

        return request, agency, client1, client2

    @pytest.mark.parametrize(
        'is_docs_separated',
        [
            pytest.param(True, id='is_docs_separated_true'),
            pytest.param(False, id='is_docs_separated_false'),
        ]
    )
    def test_does_not_raise_for_client_without_agency(self, session, client, person, paysys, is_docs_separated):
        client.is_docs_separated = is_docs_separated
        order1, order2 = _create_order(session, client), _create_order(session, client)
        request = _create_request(session, client, [(order1, 10), (order2, 20)])

        try:
            InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys,
                temporary=False
            )
        except exc.NONSEPARATED_DOCUMENTS:
            pytest.fail("Should not raise NONSEPARATED_DOCUMENTS for for single subclient")

    @pytest.mark.parametrize(
        'agency_is_docs_separated',
        [
            pytest.param(True, id='agency_is_docs_separated_true'),
            pytest.param(False, id='agency_is_docs_separated_false'),
        ]
    )
    @pytest.mark.parametrize(
        'client_is_docs_separated',
        [
            pytest.param(True, id='client_is_docs_separated_true'),
            pytest.param(False, id='client_is_docs_separated_false'),
            pytest.param(None, id='client_is_docs_separated_none'),
        ]
    )
    def test_does_not_raise_for_single_subclient(
        self,
        session,
        paysys,
        agency_is_docs_separated,
        client_is_docs_separated
    ):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        person = ob.PersonBuilder.construct(session, client=agency)
        client = ob.ClientBuilder.construct(session, agency=agency)
        order1 = _create_order(session, client)
        order2 = _create_order(session, client)
        request = _create_request(session, agency, [(order1, 10), (order2, 20)])

        agency.is_docs_separated = agency_is_docs_separated
        if client_is_docs_separated is not None:
            client.agencies_printable_doc_types = {
                str(client.agency.id): (True, client_is_docs_separated)}

        try:
            InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys,
                temporary=False
            )
        except exc.NONSEPARATED_DOCUMENTS:
            pytest.fail(
                "Should not raise NONSEPARATED_DOCUMENTS for for single subclient")

    @pytest.mark.parametrize(
        'client_is_docs_separated',
        [
            pytest.param(None, id='client_is_docs_separated_none'),
            pytest.param(False, id='client_is_docs_separated_false'),
        ]
    )
    def test_does_not_raise_if_documents_are_not_separated(self, session, paysys, client_is_docs_separated):
        request, agency, client, _ = self._setup_agency_with_clients(session)

        if client_is_docs_separated is not None:
            client.agencies_printable_doc_types = {str(agency.id): (True, client_is_docs_separated)}

        try:
            InvoiceFactory.create(
                request=request,
                person=agency.get_persons()[0],
                paysys=paysys,
                temporary=False
            )
        except exc.NONSEPARATED_DOCUMENTS:
            pytest.fail(
                "Should not raise NONSEPARATED_DOCUMENTS for for single subclient")

    @pytest.mark.parametrize(
        ('agency_is_docs_separated', 'client_is_docs_separated'),
        [
            pytest.param(True, None, id='agency_is_docs_separated_true_1'),
            pytest.param(True, False, id='agency_is_docs_separated_true_2'),
            pytest.param(False, True, id='client_is_docs_separated_true'),
            pytest.param(True, True, id='all_is_docs_separated_true'),
        ]
    )
    def test_raises_if_separated_doc_for_multiple_subclients(
        self, session, paysys, agency_is_docs_separated, client_is_docs_separated
    ):
        request, agency, client1, client2 = self._setup_agency_with_clients(session)

        agency.is_docs_separated = agency_is_docs_separated
        if client_is_docs_separated is not None:
            client1.agencies_printable_doc_types = {
                str(agency.id): (True, client_is_docs_separated)}

        with pytest.raises(exc.NONSEPARATED_DOCUMENTS) as exc_info:
            InvoiceFactory.create(
                request=request,
                person=agency.get_persons()[0],
                paysys=paysys,
                temporary=False
            )

        assert sorted(exc_info.value.clients) == sorted([client1, client2])


@pytest.mark.parametrize(
    'test_env, env_type, is_ok',
    [
        (0, 'prod', True),
        (0, 'test', True),
        (1, 'prod', False),
        (1, 'test', True),
        (1, 'dev', True),
    ]
)
def test_firm_test_env(session, app, client, person, test_env, env_type, is_ok):
    firm = ob.FirmBuilder.construct(
        session,
        country=ob.Getter(mapper.Country, RegionId.RUSSIA),
        test_env=test_env
    )
    service = ob.ServiceBuilder.construct(session)
    pay_policy_part_id = ob.create_pay_policy_service(session, service.id, firm.id, [('RUB', PaymentMethodIDs.bank)])
    ob.create_pay_policy_region(session, pay_policy_part_id, RegionId.RUSSIA)
    paysys = ob.PaysysBuilder.construct(
        session,
        firm=firm,
        category=person.type,
        currency='RUR',
        iso_currency='RUB',
        payment_method_id=PaymentMethodIDs.bank,
    )

    order = _create_order(session, client, service_id=service.id)
    request = _create_request(session, client, [(order, 10)])

    def _cr_inv():
        with mock.patch.object(app, 'get_current_env_type', return_value=env_type):
            return InvoiceFactory.create(
                request=request,
                person=person,
                paysys=paysys,
                temporary=False
            )

    if is_ok:
        inv = _cr_inv()
        assert inv.firm == firm
    else:
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            _cr_inv()
        assert "Can't create invoice with test firm" in exc_info.value.msg
