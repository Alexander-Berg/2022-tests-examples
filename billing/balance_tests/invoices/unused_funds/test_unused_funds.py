# -*- coding: utf-8 -*-

import datetime
import decimal

import mock
import pytest
import hamcrest

from balance import mapper
from balance import exc
from balance import core
from balance.actions import single_account, invoice_turnon
from balance.actions import unused_funds as a_uf
from balance.constants import *

from butils import decimal_unit

from tests import object_builder as ob

from tests.balance_tests.invoices.invoice_common import (
    create_refund,
    create_product,
)
from tests.balance_tests.invoices.unused_funds.common import (
    create_order,
    create_invoice,
    create_request,
    PAYSYS_ID_CARD,
    PAYSYS_ID_BANK,
)

D = decimal.Decimal
DU = decimal_unit.DecimalUnit


class TestInvoiceAttributes(object):
    @pytest.mark.parametrize('overdraft', [0, 1], ids=['prepayment', 'overdraft'])
    @pytest.mark.parametrize(
        'invoice_sum, receipt_sum, consume_sum, required_sum',
        [
            pytest.param(100, 0, 0, 0),
            pytest.param(100, 0, 40, 0),
            pytest.param(100, 30, 40, 0),
            pytest.param(100, 50, 40, 10),
            pytest.param(100, 100, 40, 60),
            pytest.param(100, 120, 40, 80),
        ]
    )
    def test_w_receipt(self, session, client, order, invoice_sum, receipt_sum, consume_sum, required_sum, overdraft):
        invoice = create_invoice(session, client, order, invoice_sum, overdraft)
        if receipt_sum:
            invoice.create_receipt(receipt_sum)
        if consume_sum:
            invoice.transfer(order, TransferMode.src, consume_sum, skip_check=True)

        assert invoice.unused_funds == required_sum

    @pytest.mark.parametrize(
        'status, required_sum',
        [
            (None, 66),
            (InvoiceRefundStatus.not_exported, 66),
            (InvoiceRefundStatus.exported, 66),
            (InvoiceRefundStatus.export_failed, 66),
            (InvoiceRefundStatus.failed, 66),
            (InvoiceRefundStatus.successful, 100),
            (InvoiceRefundStatus.failed_unlocked, 100),
        ]
    )
    def test_w_receipt_refund(self, session, client, order, status, required_sum):
        invoice = create_invoice(session, client, order, 100)
        invoice.create_receipt(100)
        create_refund(invoice, 34, status)

        assert invoice.unused_funds == required_sum

    @pytest.mark.parametrize(
        'refund_sum, required_sum',
        [
            (40, 70),
            (70, 40)
        ]
    )
    def test_overpay_refund_partial_consume(self, session, client, order, refund_sum, required_sum):
        invoice = create_invoice(session, client, order, 100)
        invoice.create_receipt(150)
        invoice.transfer(order, TransferMode.src, 40)
        create_refund(invoice, refund_sum)

        assert invoice.unused_funds == required_sum

    def test_currency_rub(self, session, client, order):

        rates = {
            'RUB': 1,
            'EUR': DU('78', 'RUB', 'EUR'),
            'USD': DU('68', 'RUB', 'USD'),
        }
        with ob.patched_currency([rates]):
            invoice = create_invoice(session, client, order, 100, paysys_id=1023)  # eur_wo_nds
        invoice.create_receipt(100)
        create_refund(invoice, 34)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                unused_funds=DU(66, 'FISH'),
                unused_funds_rub=DU(66 * 78, 'RUB'),
            )
        )


class TestTransfer(object):
    def test_invoice_ok(self, session, client):
        order1 = create_order(client)
        order2 = create_order(client)
        invoice = create_invoice(session, client, [order1, order2], 50)
        invoice.create_receipt(100)

        a_uf.InvoiceTransfer(invoice).transfer()

        assert invoice.consume_sum == 100
        assert order1.consume_qty == 50
        assert order2.consume_qty == 50

    def test_invoice_not_enough(self, session, client):
        order1 = create_order(client)
        order2 = create_order(client)
        invoice = create_invoice(session, client, [order1, order2], 50)
        invoice.create_receipt(99)

        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            a_uf.InvoiceTransfer(invoice).transfer()
        assert 'unused_funds_transfer - unused_funds' in exc_info.value.msg

        assert invoice.consume_sum == 0
        assert order1.consume_qty == 0
        assert order2.consume_qty == 0

    def test_order_ok(self, session, client):
        order1 = create_order(client)
        order2 = create_order(client)
        invoice = create_invoice(session, client, [order1, order2], 100)
        invoice.create_receipt(200)

        a_uf.InvoiceTransfer(invoice).transfer(order1)

        assert invoice.consume_sum == 200
        assert order1.consume_qty == 200
        assert order2.consume_qty == 0

    def test_invoice_not_enough_refund(self, client, order, invoice):
        invoice.create_receipt(100)
        create_refund(invoice, 1)

        with pytest.raises(exc.INVALID_PARAM):
            a_uf.InvoiceTransfer(invoice).transfer()

    def test_order_refund(self, client, order, invoice):
        invoice.create_receipt(100)
        create_refund(invoice, 1)

        a_uf.InvoiceTransfer(invoice).transfer(order)

        assert invoice.consume_sum == 99
        assert order.consume_qty == 99


class TestTransferTargets(object):
    def test_locked(self, session, client, order, invoice):
        invoice.create_receipt(100)
        session.flush()

        patcher = mock.patch(
            'balance.actions.unused_funds.transfer.InvoiceTransfer.check_conditions',
            return_value=False
        )
        with patcher as m:
            assert a_uf.InvoiceTransfer(invoice).targets == []

        assert m.call_count == 1
        assert m.call_args[1]['is_admin'] is True

    def test_consumed(self, client, order, invoice):
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.src, 70)

        assert a_uf.InvoiceTransfer(invoice).targets == [order]

    def test_unconsumed(self, client, order, invoice):
        invoice.create_receipt(100)

        assert a_uf.InvoiceTransfer(invoice).targets == [order, invoice]

    def test_unconsumed_refund(self, client, order, invoice):
        invoice.create_receipt(100)
        create_refund(invoice, 1)

        assert a_uf.InvoiceTransfer(invoice).targets == [order]

    def test_unconsumed_refund_overpay(self, client, order, invoice):
        invoice.create_receipt(101)
        create_refund(invoice, 1)

        assert a_uf.InvoiceTransfer(invoice).targets == [order, invoice]

    def test_not_enough_for_one(self, session, client):
        product1 = create_product(session, 50, unit_id=DAY_UNIT_ID)
        order1 = create_order(client, product_id=product1.id)
        product2 = create_product(session, 50, unit_id=AUCTION_UNIT_ID)
        order2 = create_order(client, product_id=product2.id)
        invoice = create_invoice(session, client, [order1, order2], 100)
        invoice.create_receipt(40)

        assert a_uf.InvoiceTransfer(invoice).targets == [order2]

    def test_not_enough_for_one_w_type_rate(self, session, client):
        product1 = create_product(session, 100, unit_id=SHOWS_1000_UNIT_ID)
        order1 = create_order(client, product_id=product1.id)
        product2 = create_product(session, 200, unit_id=SHOWS_1000_UNIT_ID)
        order2 = create_order(client, product_id=product2.id)
        invoice = create_invoice(session, client, [order1, order2], 100)
        invoice.create_receipt(D('0.1'))

        assert a_uf.InvoiceTransfer(invoice).targets == [order1]

    @pytest.mark.single_account
    def test_single_account(self, session):
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = [ServiceId.MKB]

        client = ob.ClientBuilder.construct(session, with_single_account=True)
        person = ob.PersonBuilder.construct(session, client=client, type='ph')
        single_account.prepare.process_client(client)
        invoice, = person.invoices
        invoice.create_receipt(666)

        order = create_order(client, service_id=ServiceId.MKB)
        create_order(client, service_id=ServiceId.DIRECT)

        assert a_uf.InvoiceTransfer(invoice).targets == [order]


class TestPersonRubSumQuery(object):

    @pytest.mark.parametrize(
        'invoice_sums, consume_sums, receipt_sums, res_sum',
        [
            pytest.param([100, 100], [0, 0], [100, 200], 300, id='unconsumed'),
            pytest.param([100, 100], [100, 100], [100, 200], 100, id='consumed'),
            pytest.param([100, 100], [100, 100], [10, 110], 10, id='underpaid'),
        ]
    )
    def test_sum(self, session, client, person, invoice_sums, consume_sums, receipt_sums, res_sum):
        for invoice_sum, consume_sum, receipt_sum in zip(invoice_sums, consume_sums, receipt_sums):
            invoice = create_invoice(session, client, quantity=invoice_sum, person=person)
            if consume_sum:
                io, = invoice.invoice_orders
                invoice.transfer(io.order, TransferMode.src, consume_sum, skip_check=True)
            if receipt_sum:
                invoice.create_receipt(receipt_sum)

        assert a_uf.queries.get_person_rub_sum(person, {ServiceId.DIRECT}) == res_sum

    @pytest.mark.invoice_refunds
    def test_refund(self, session, client, person):
        invoice = create_invoice(session, client, quantity=100, person=person)
        invoice.create_receipt(100)
        create_refund(invoice, 30)

        assert a_uf.queries.get_person_rub_sum(person, {ServiceId.DIRECT}) == 70

    def test_person_filter(self, session, client):
        persons = []
        for invoice_sum in [100, 200, 300]:
            person = ob.PersonBuilder.construct(session, client=client)
            invoice = create_invoice(session, client, quantity=invoice_sum, person=person, overdraft=1)
            invoice.create_receipt(invoice_sum)
            persons.append(person)

        assert a_uf.queries.get_person_rub_sum(persons[1], {ServiceId.DIRECT}) == 200

    def test_overdraft(self, session, client, person):
        invoice = create_invoice(session, client, quantity=100, person=person, overdraft=1)
        invoice.create_receipt(100)

        assert a_uf.queries.get_person_rub_sum(person, {ServiceId.DIRECT}) == 100

    def test_credit(self, session, client, person):
        invoice = create_invoice(session, client, quantity=100, person=person)
        invoice.credit = 1
        invoice.create_receipt(100)

        assert a_uf.queries.get_person_rub_sum(person, {ServiceId.DIRECT}) == 0

    def test_service(self, session, client, person):
        for service_id, invoice_sum in [(ServiceId.DIRECT, 100), (ServiceId.MARKET, 200)]:
            order = create_order(client, service_id)
            invoice = create_invoice(session, client, order, quantity=invoice_sum, person=person)
            invoice.create_receipt(invoice_sum)

        assert a_uf.queries.get_person_rub_sum(person, {ServiceId.MARKET}) == 200

    def test_by_invoice(self, session, client, person):
        invoices = []
        for invoice_sum in [100, 200, 300]:
            invoice = create_invoice(session, client, quantity=invoice_sum, person=person)
            invoice.create_receipt(invoice_sum)
            invoices.append(invoice)

        assert a_uf.queries.get_person_rub_sum(person, {ServiceId.DIRECT}, invoices[1]) == 400

    def test_currency(self, session, client):
        person = ob.PersonBuilder.construct(session, client=client, type='yt')

        product = ob.ProductBuilder.construct(session, price=1, currency='USD')
        order = create_order(client, ServiceId.DIRECT, product.id)

        with ob.patched_currency([{'RUR': 1, 'RUB': 1, 'USD': 70}]):
            invoice = create_invoice(session, client, order, 10, paysys_id=1013, person=person)
        invoice.create_receipt(invoice.effective_sum)

        assert a_uf.queries.get_person_rub_sum(person, {ServiceId.DIRECT}) == 700

    def test_single_account(self, session):
        client = ob.ClientBuilder(with_single_account=True).build(session).obj
        person = ob.PersonBuilder(client=client, type='ur').build(session).obj
        single_account.prepare.process_client(client)
        (personal_account, _), = client.get_single_account_subaccounts()

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                rows=[
                    ob.BasketItemBuilder(
                        quantity=100,
                        order=ob.OrderBuilder(
                            client=client,
                            product=ob.ProductBuilder(
                                price=1,
                                engine_id=ServiceId.DIRECT
                            ),
                            service_id=ServiceId.DIRECT
                        )
                    )
                ]
            )
        ).build(session).obj

        invoice = ob.InvoiceBuilder(
            dt=datetime.datetime.now(),
            person=person,
            paysys=ob.Getter(mapper.Paysys, 1003).build(session).obj,
            request=request,
            single_account_number=client.single_account_number,
            type='charge_note'
        ).build(session).obj

        invoice_turnon.InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()

        personal_account.create_receipt(200)

        assert personal_account.receipt_sum == 300
        assert personal_account.consume_sum == 100
        assert personal_account.unused_funds == 200
        assert a_uf.queries.get_person_rub_sum(person, {ServiceId.DIRECT}) == 0


class TestClientInvoicesQuery(object):
    @pytest.mark.parametrize(
        'invoice_sum, consume_sum, receipt_sum, is_found',
        [
            pytest.param(100, 99, 100, True, id='overpaid'),
            pytest.param(100, 100, 100, False, id='consumed'),
            pytest.param(100, 100, 99, False, id='underpaid'),
        ]
    )
    def test_sum(self, session, client, person, invoice_sum, consume_sum, receipt_sum, is_found):
        invoice = create_invoice(session, client, person=person, quantity=invoice_sum)
        io, = invoice.invoice_orders
        invoice.transfer(io.order, TransferMode.src, consume_sum, skip_check=True)
        invoice.create_receipt(receipt_sum)

        if is_found:
            assert a_uf.queries.get_client_invoices(client) == [invoice]
        else:
            assert a_uf.queries.get_client_invoices(client) == []

    @pytest.mark.invoice_refunds
    @pytest.mark.parametrize(
        'initial_sum, refund_sum, is_found',
        [
            (100, 30, True),
            (100, 100, False),
        ]
    )
    def test_refund(self, session, client, person, initial_sum, refund_sum, is_found):
        invoice = create_invoice(session, client, quantity=initial_sum, person=person)
        invoice.create_receipt(initial_sum)
        create_refund(invoice, refund_sum)

        if is_found:
            assert a_uf.queries.get_client_invoices(client) == [invoice]
        else:
            assert a_uf.queries.get_client_invoices(client) == []

    def test_multiple_persons(self, session, client):
        invoices = []
        for idx in range(3):
            i = create_invoice(session, client)
            i.create_receipt(i.effective_sum)
            invoices.append(i)

        hamcrest.assert_that(
            a_uf.queries.get_client_invoices(client),
            hamcrest.contains_inanyorder(*invoices)
        )

    def test_agency(self, session):
        client = ob.ClientBuilder.construct(session, is_agency=1)
        invoice = create_invoice(session, client)
        invoice.create_receipt(invoice.effective_sum)

        assert a_uf.queries.get_client_invoices(client) == []

    def test_overdraft(self, session, client):
        invoice = create_invoice(session, client, overdraft=1)
        invoice.create_receipt(invoice.effective_sum)

        assert a_uf.queries.get_client_invoices(client) == [invoice]

    def test_credit(self, session, client):
        invoice = create_invoice(session, client, overdraft=1)
        invoice.credit = 1
        invoice.create_receipt(invoice.effective_sum)

        assert a_uf.queries.get_client_invoices(client) == []

    @pytest.mark.parametrize(
        'paysys_id, is_found',
        [
            pytest.param(1001, True, id='ph'),
            pytest.param(1003, True, id='ur'),
            pytest.param(1002, False, id='as'),
            pytest.param(1033, False, id='cc_ur'),
        ]
    )
    def test_paysys(self, session, client, paysys_id, is_found):
        invoice = create_invoice(session, client, paysys_id=paysys_id)
        invoice.create_receipt(invoice.effective_sum)

        if is_found:
            assert a_uf.queries.get_client_invoices(client) == [invoice]
        else:
            assert a_uf.queries.get_client_invoices(client) == []

    @pytest.mark.parametrize(
        'unused_funds_lock, is_found',
        [
            (InvoiceReceiptLockType.OFF, True),
            (InvoiceReceiptLockType.TRANSFER, False),
            (InvoiceReceiptLockType.REFUND, False),
            (InvoiceReceiptLockType.OVERDRAFT, False),
        ]
    )
    def test_unused_funds_lock(self, session, client, unused_funds_lock, is_found):
        invoice = create_invoice(session, client, overdraft=1)
        invoice.unused_funds_lock = unused_funds_lock
        invoice.create_receipt(invoice.effective_sum)

        if is_found:
            assert a_uf.queries.get_client_invoices(client) == [invoice]
        else:
            assert a_uf.queries.get_client_invoices(client) == []


class TestTransferConditions(object):
    @staticmethod
    def create_invoice(order, is_spa=False, paysys_id=PAYSYS_ID_BANK):
        session = order.session
        client = order.client

        if is_spa:
            session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = [order.service_id]
            client.single_account_number = single_account.prepare.generate_single_account_number(session)
            session.flush()
            single_account.prepare.process_client(client)
            request = create_request(client, order)
            person = ob.PersonBuilder(client=client).build(order.session).obj
            invoice, = core.Core(session).create_invoice(request.id, paysys_id, person.id)
            assert invoice.is_for_single_account
            return invoice
        else:
            return create_invoice(session, client, order, paysys_id=paysys_id)

    @pytest.mark.parametrize('is_admin', [False, True])
    @pytest.mark.parametrize('is_spa', [False, True])
    def test_ok(self, order, is_admin, is_spa):
        invoice = self.create_invoice(order, is_spa)

        assert a_uf.InvoiceTransfer(invoice).check_conditions(is_admin) is True

    @pytest.mark.parametrize('is_admin', [False, True])
    @pytest.mark.parametrize('is_spa', [False, True])
    def test_hidden(self, session, order, is_admin, is_spa):
        invoice = self.create_invoice(order, is_spa)
        invoice.hidden = 2
        session.flush()

        assert a_uf.InvoiceTransfer(invoice).check_conditions(is_admin) is False

    @pytest.mark.parametrize('is_admin', [False, True])
    def test_credit(self, session, order, is_admin):
        invoice = self.create_invoice(order)
        invoice.credit = 1
        session.flush()

        assert a_uf.InvoiceTransfer(invoice).check_conditions(is_admin) is False

    @pytest.mark.parametrize(
        'is_admin, is_spa, paysys_id, result',
        [
            (False, False, PAYSYS_ID_BANK, True),
            (False, False, PAYSYS_ID_CARD, False),
            (False, True, PAYSYS_ID_CARD, True),
            (True, False, PAYSYS_ID_CARD, True),
            (True, True, PAYSYS_ID_CARD, True),
        ]
    )
    def test_paysys(self, order, is_admin, is_spa, paysys_id, result):
        invoice = self.create_invoice(order, is_spa, paysys_id)

        assert a_uf.InvoiceTransfer(invoice).check_conditions(is_admin) is result

    @pytest.mark.parametrize(
        'is_admin, is_spa, result',
        [
            (False, False, False),
            (False, True, True),
            (True, False, True),
            (True, True, True),
        ]
    )
    def test_postpay(self, session, order, is_admin, is_spa, result):
        invoice = self.create_invoice(order, is_spa)
        invoice.postpay = 1
        session.flush()

        assert a_uf.InvoiceTransfer(invoice).check_conditions(is_admin) is result

    @pytest.mark.parametrize('is_spa', [False, True])
    @pytest.mark.parametrize(
        'is_admin, result',
        [
            (False, False),
            (True, True),
        ]
    )
    def test_agency(self, session, order, is_admin, is_spa, result):
        order.client.is_agency = True
        session.flush()
        invoice = self.create_invoice(order, is_spa)

        assert a_uf.InvoiceTransfer(invoice).check_conditions(is_admin) is result

    @pytest.mark.parametrize('is_spa', [False, True])
    @pytest.mark.parametrize(
        'is_admin, result',
        [
            (False, False),
            (True, True),
        ]
    )
    def test_expired_invoice(self, order, is_admin, is_spa, result):
        invoice = self.create_invoice(order, is_spa)

        with mock.patch('balance.mapper.clients.Client.has_expired_invoice', return_value=True):
            assert a_uf.InvoiceTransfer(invoice).check_conditions(is_admin) is result

    @pytest.mark.parametrize('is_spa', [False, True])
    @pytest.mark.parametrize(
        'unused_funds_lock', [
            InvoiceReceiptLockType.OVERDRAFT,
            InvoiceReceiptLockType.REFUND,
            InvoiceReceiptLockType.TRANSFER,
        ])
    @pytest.mark.parametrize(
        'is_admin, result',
        [
            (False, False),
            (True, True),
        ]
    )
    def test_lock(self, session, order, unused_funds_lock, is_admin, is_spa, result):
        invoice = self.create_invoice(order, is_spa)
        invoice.unused_funds_lock = unused_funds_lock
        session.flush()

        with mock.patch('balance.mapper.clients.Client.has_expired_invoice', return_value=True):
            assert a_uf.InvoiceTransfer(invoice).check_conditions(is_admin) is result


@pytest.mark.permissions
class TestTransferPermission(object):
    @pytest.mark.parametrize(
        'role_firm_id, res',
        [
            (-1, False),
            (None, True),
            (FirmId.YANDEX_OOO, True),
            (FirmId.DRIVE, False),
        ],
    )
    def test_role(self, session, role_firm_id, res):
        role = ob.create_role(session, (PermissionCode.TRANSFER_UNUSED_FUNDS, {ConstraintTypes.firm_id: None}))
        roles = []
        if role_firm_id != -1:
            roles.append((role, {ConstraintTypes.firm_id: role_firm_id}))
        ob.set_roles(session, session.passport, roles)

        invoice = create_invoice(session, firm_id=FirmId.YANDEX_OOO)
        assert a_uf.InvoiceTransfer(invoice).is_allowed == res

    @pytest.mark.parametrize(
        'owns',
        [True, False],
    )
    def test_client(self, session, owns):
        role = ob.Getter(mapper.Role, RoleName.CLIENT).build(session).obj
        ob.set_roles(session, session.passport, [role])
        invoice = create_invoice(session)

        if owns:
            session.passport.link_to_client(invoice.client)

        assert a_uf.InvoiceTransfer(invoice).is_allowed == owns
