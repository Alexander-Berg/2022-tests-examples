# -*- coding: utf-8 -*-

import collections
import datetime
import uuid
import decimal

import pytest
import hamcrest

from balance import mapper
from balance.actions import invoice_transfer as a_it
from balance import exc
from balance.constants import *

from tests import object_builder as ob
from tests.balance_tests.invoice_transfer.common import (
    create_contract,
    create_invoice,
    create_order,
    create_person,
)
from tests.balance_tests.invoice_refunds.common import create_bank_cpf

D = decimal.Decimal

PAYSYS_ID_BANK = 1001
PAYSYS_ID_BANK_KZ = 1021


pytestmark = [
    pytest.mark.invoice_transfer,
]


def create_cpf_src_invoice(session, src_invoice, amount=None, operation_type=OebsOperationType.INSERT, dt=None):
    from tests.balance_tests.invoice_refunds import common
    if amount is None:
        amount = src_invoice.total_sum
    cpf = common.create_ocpf(src_invoice, amount, operation_type=operation_type, dt=dt)
    cpf.inn = 'inn'
    cpf.customer_name = 'customer_name'
    cpf.bik = 'bik'
    cpf.account_name = 'account_name'
    cpf.source_id = uuid.uuid4().int
    cpf.cash_receipt_number = uuid.uuid4().hex
    session.flush()
    session.expire_all()


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def src_invoice(client):
    invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
    create_cpf_src_invoice(client.session, invoice)
    return invoice


@pytest.fixture
def dst_invoice(client, src_invoice):
    invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_invoice.person)
    return invoice


@pytest.fixture
def bank_cpf(client):
    invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
    return create_bank_cpf(invoice, 100)


class TestCheckAvailability(object):
    @staticmethod
    def _assert_fail(src_invoice, dst_invoice, exception, msg):
        invoice_tranfer_manager = a_it.InvoiceTransferManager(src_invoice, dst_invoice)
        hamcrest.assert_that(
            hamcrest.calling(invoice_tranfer_manager.check_availability).with_args(),
            hamcrest.raises(exception, msg)
        )

    def test_ok(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)

        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).check_availability() is True

    @pytest.mark.permissions
    def test_fail_perms(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        ob.create_passport(session, patch_session=True)

        self._assert_fail(src_invoice, dst_invoice, exc.PERMISSION_DENIED, 'no permission DoInvoiceTransfer')

    @pytest.mark.permissions
    def test_fail_withdraw_perms(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(src_invoice.effective_sum)
        src_invoice.turn_on_rows()
        role = ob.create_role(session, PermissionCode.DO_INVOICE_TRANSFER)
        ob.create_passport(session, role, patch_session=True)

        self._assert_fail(src_invoice, dst_invoice, exc.NOT_UNUSED_FUNDS_FOR_INVOICE_TRANSFER,
                          'No unused funds and withdraw not allowed')

    @pytest.mark.parametrize(
        'lock_type',
        [
            pytest.param(InvoiceReceiptLockType.TRANSFER, id='UNUSED_FUNDS_LOCK_TRANSFER'),
            pytest.param(InvoiceReceiptLockType.REFUND, id='UNUSED_FUNDS_LOCK_REFUND'),
        ]
    )
    def test_ok_unused_funds_lock(self, session, src_invoice, dst_invoice, lock_type):
        src_invoice.unused_funds_lock = lock_type
        src_invoice.create_receipt(100)

        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).check_availability() is True

    def test_ok_overdraft_debt(self, session, client):
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, overdraft=1)
        create_cpf_src_invoice(client.session, src_invoice, amount=100)
        src_invoice.create_receipt(100)

        src_invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
        src_invoice.turn_on_rows()
        src_invoice.session.flush()

        assert a_it.InvoiceTransferManager(src_invoice).check_availability() is True

    def test_fail_overdraft_debt(self, session, client):
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, overdraft=1)
        create_cpf_src_invoice(client.session, src_invoice, amount=50)
        src_invoice.create_receipt(50)

        src_invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
        src_invoice.turn_on_rows()
        src_invoice.session.flush()

        self._assert_fail(src_invoice, None, exc.INVOICE_TRANSFER_FORBIDDEN_OVERDRAFT_DEBT,
                          'due to overdraft debt')

    def test_fail_invoice_transfer_to_itself(self, session, src_invoice):
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, src_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_TO_THE_SAME_INVOICE,
                          'to itself')

    def test_fail_ur_inn(self, session, client):
        src_person = create_person(client, type='ur', inn=str(ob.generate_int(12)))
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_person)
        dst_person = create_person(client, type='ur', inn=str(ob.generate_int(12)))
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=dst_person)
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_INN,
                          'forbidden for ur persons with different inn')

    def test_fail_byu_unp(self, session, client):
        src_person = create_person(client, type='byu', inn=str(ob.generate_int(12)))
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_person)
        dst_person = create_person(client, type='byu', inn=str(ob.generate_int(12)))
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=dst_person)
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_UNP,
                          'forbidden for byu persons with different unp')

    def test_fail_kzu_kz_in(self, session, client):
        src_person = create_person(client, type='kzu', kz_in=str(ob.generate_int(12)))
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_person)
        dst_person = create_person(client, type='kzu', kz_in=str(ob.generate_int(12)))
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=dst_person)
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_BIN,
                          'forbidden for kzu persons with different BIN')

    def test_fail_different_ph(self, session, src_invoice, client):
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_PERSONS,
                          'forbidden for different persons')

    def test_fail_different_usu(self, session, client):
        src_person = create_person(client, type='usu')
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_person)
        dst_person = create_person(client, type='usu')
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=dst_person)
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_PERSONS,
                          'forbidden for different persons')

    def test_fail_different_nds(self, session, src_invoice, dst_invoice):
        dst_invoice.nds_pct = 12
        session.flush()
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_NDS,
                          'forbidden for invoices with different nds')

    def test_fail_different_firms(self, session, client, src_invoice):
        dst_order = create_order(client, MARKET_FISH_PRODUCT_ID)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_invoice.person, orders=[dst_order])
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_FIRMS,
                          'forbidden for invoices with different firms')

    def test_fail_different_currencies(self, session, client):
        person = create_person(client, type='kzu')
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=person)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK_KZ, 100, person=person)
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_CURRENCIES,
                          'forbidden for invoices with different currencies')

    def test_ok_contracts(self, session, client):
        contract = create_contract(client)
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=contract.person, contract=contract)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=contract.person, contract=contract)
        src_invoice.create_receipt(100)

        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).check_availability() is True

    def test_fail_different_contracts(self, session, client):
        src_contract = create_contract(client)
        dst_contract = create_contract(client, person=src_contract.person)
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_contract.person, contract=src_contract)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=dst_contract.person, contract=dst_contract)
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_CONTRACTS,
                          'forbidden for invoices with different contracts')

    def test_ok_different_services(self, session, client, src_invoice):
        dst_order_1 = create_order(client)
        dst_order_2 = create_order(client, service_id=35)
        dst_invoice = create_invoice(
            client, PAYSYS_ID_BANK, 100, person=src_invoice.person, orders=[dst_order_1, dst_order_2]
        )
        src_invoice.create_receipt(100)

        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).check_availability() is True

    def test_fail_different_services(self, session, client, src_invoice):
        dst_order = create_order(client, GEOCON_PRODUCT_ID)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_invoice.person, orders=[dst_order])
        src_invoice.create_receipt(100)

        self._assert_fail(src_invoice, dst_invoice, exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_SERVICES,
                          'forbidden for invoices with different services')

    def test_ok_contracts_different_services(self, session, client):
        contract = create_contract(client, services={ServiceId.DIRECT, ServiceId.GEOCON})
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=contract.person, contract=contract)
        dst_order = create_order(client, GEOCON_PRODUCT_ID)
        dst_invoice = create_invoice(
            client, PAYSYS_ID_BANK, 100, person=contract.person, orders=[dst_order], contract=contract
        )
        src_invoice.create_receipt(100)

        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).check_availability() is True


class TestGetAvailableInvoiceTransferSum(object):
    def test_new(self, src_invoice, dst_invoice):
        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).get_available_invoice_transfer_sum() == 0

    def test_unused(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).get_available_invoice_transfer_sum() == 100

    def test_not_dst_invoice(self, session, src_invoice):
        src_invoice.create_receipt(100)
        assert a_it.InvoiceTransferManager(src_invoice, None).get_available_invoice_transfer_sum() == 100

    def test_consumed(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        src_invoice.turn_on_rows()

        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).get_available_invoice_transfer_sum() == 100

    def test_completed(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        order = src_invoice.invoice_orders[0].order
        src_invoice.transfer(order)
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 66})
        session.flush()
        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).get_available_invoice_transfer_sum() == 34

    def test_completed_cashback(self, session, client):
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, cashback_bonus=100)
        create_cpf_src_invoice(session, src_invoice)
        src_invoice.create_receipt(100)
        src_invoice.turn_on_rows()

        order = src_invoice.invoice_orders[0].order
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 49})
        session.flush()
        assert a_it.InvoiceTransferManager(src_invoice).get_available_invoice_transfer_sum() == D('151') / D('2')

    def test_completed_promocode(self, session, client):
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, promocode_discount_pct=50)
        create_cpf_src_invoice(session, src_invoice)
        src_invoice.create_receipt(100)
        src_invoice.turn_on_rows(apply_promocode=True)

        for c in src_invoice.consumes:
            assert c.discount_obj.promo_code

        order = src_invoice.invoice_orders[0].order
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 49})
        session.flush()
        assert a_it.InvoiceTransferManager(src_invoice).get_available_invoice_transfer_sum() == D('51') / D('2')

    def test_completed_part_payment(self, session, client):
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
        create_cpf_src_invoice(session, src_invoice, 50)
        src_invoice.create_receipt(100)
        src_invoice.turn_on_rows()

        order = src_invoice.invoice_orders[0].order
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 49})
        session.flush()
        assert a_it.InvoiceTransferManager(src_invoice).get_available_invoice_transfer_sum() == 1

    def test_completed_part_payment_fish(self, session, client):
        order = create_order(client, product=DIRECT_PRODUCT_ID)
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, orders=[order])
        create_cpf_src_invoice(session, src_invoice, 33)
        src_invoice.create_receipt(100)
        src_invoice.turn_on_rows()

        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 1})
        session.flush()
        assert a_it.InvoiceTransferManager(src_invoice).get_available_invoice_transfer_sum() == 3

    def test_overacted(self, session, client):
        order1 = create_order(client)
        order2 = create_order(client)
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, orders=[order1, order2])
        create_cpf_src_invoice(session, src_invoice)
        src_invoice.create_receipt(src_invoice.effective_sum)
        src_invoice.turn_on_rows()

        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
        dst_invoice.person = src_invoice.person
        session.flush()

        order1.calculate_consumption(datetime.datetime.now(), {order1.shipment_type: 55})
        src_invoice.generate_act(backdate=datetime.datetime.now(), force=1)
        order1.calculate_consumption(datetime.datetime.now(), {order1.shipment_type: 0})

        order2.calculate_consumption(datetime.datetime.now(), {order2.shipment_type: 35})
        session.flush()

        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).get_available_invoice_transfer_sum() == 110

    @pytest.mark.parametrize(
        'payment_sum, expected_available_sum',
        [
            pytest.param(300, 0),
            pytest.param(345, 45),
        ]
    )
    def test_y_invoice(self, session, client, payment_sum, expected_available_sum):
        from tests.balance_tests.core.core_common import create_y_invoice
        person = create_person(client)
        y_invoice = create_y_invoice(session, person, None)
        create_cpf_src_invoice(session, y_invoice, payment_sum)
        y_invoice.create_receipt(y_invoice.effective_sum)

        assert a_it.InvoiceTransferManager(y_invoice).get_available_invoice_transfer_sum() == expected_available_sum

    @pytest.mark.parametrize(
        'invoice_transfer_status, result',
        [
            pytest.param(InvoiceTransferStatus.not_exported, 66),
            pytest.param(InvoiceTransferStatus.exported, 66),
            pytest.param(InvoiceTransferStatus.export_failed, 66),
            pytest.param(InvoiceTransferStatus.in_progress, 66),
            pytest.param(InvoiceTransferStatus.successful, 100),
            pytest.param(InvoiceTransferStatus.failed_unlocked, 100),
        ]
    )
    def test_transfer_in_progress(self, session, src_invoice, dst_invoice, invoice_transfer_status, result):
        invoice_sum = 100
        consume_sum = 30

        invoice_transfer = ob.InvoiceTransferBuilder(
            src_invoice=src_invoice,
            dst_invoice=dst_invoice,
            amount=34
        ).build(session).obj
        invoice_transfer.set_status(invoice_transfer_status)
        session.flush()

        src_invoice.create_receipt(invoice_sum)
        src_invoice.transfer(src_invoice.invoice_orders[0].order, TransferMode.src, consume_sum)

        assert src_invoice.unused_funds == (result - consume_sum)
        assert src_invoice._locked_sum == (invoice_sum - result)
        assert a_it.InvoiceTransferManager(src_invoice).get_available_invoice_transfer_sum() == result

    def test_refund_in_progress(self, session, bank_cpf):
        ob.InvoiceRefundBuilder(
            invoice=bank_cpf.invoice,
            payment_id=bank_cpf.id,
            amount=34
        ).build(session)

        bank_cpf.invoice.create_receipt(100)
        bank_cpf.invoice.transfer(bank_cpf.invoice.invoice_orders[0].order, TransferMode.src, 30)

        assert bank_cpf.invoice.unused_funds == 36
        assert bank_cpf.invoice._locked_sum == 34
        assert a_it.InvoiceTransferManager(bank_cpf.invoice).get_available_invoice_transfer_sum() == 66

    def test_transfer_and_refund_in_progress(self, session, bank_cpf):
        ob.InvoiceRefundBuilder(
            invoice=bank_cpf.invoice,
            payment_id=bank_cpf.id,
            amount=D('19.33')
        ).build(session)

        dst_invoice = create_invoice(bank_cpf.invoice.client, PAYSYS_ID_BANK, 100)
        dst_invoice.person = bank_cpf.invoice.person
        session.flush()

        ob.InvoiceTransferBuilder(
            src_invoice=bank_cpf.invoice,
            dst_invoice=dst_invoice,
            amount=D('19.18')
        ).build(session)

        bank_cpf.invoice.create_receipt(100)
        bank_cpf.invoice.transfer(bank_cpf.invoice.invoice_orders[0].order, TransferMode.src, 30)

        assert bank_cpf.invoice.unused_funds == D('31.49')
        assert bank_cpf.invoice._locked_sum == D('38.51')
        assert a_it.InvoiceTransferManager(bank_cpf.invoice).get_available_invoice_transfer_sum() == D('61.49')

    @pytest.mark.parametrize(
        'skip, result',
        [
            pytest.param(False, 0, id='dont_skip'),
            pytest.param(True, 100, id='skip'),
        ]
    )
    def test_unavailable(self, session, client, src_invoice, skip, result):
        src_invoice.create_receipt(100)
        src_invoice.turn_on_rows()
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100)

        assert a_it.InvoiceTransferManager(src_invoice, dst_invoice).get_available_invoice_transfer_sum(
            skip_availability_check=skip) == result


class TestCreate(object):
    def test_wo_withdraw(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        order = src_invoice.invoice_orders[0].order
        src_invoice.transfer(order, TransferMode.src, 30)
        invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(40)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                src_invoice=src_invoice,
                dst_invoice=dst_invoice,
                amount=40,
                status_code=InvoiceTransferStatus.not_exported,
                exports=hamcrest.all_of(
                    hamcrest.has_length(1),
                    hamcrest.has_entry('OEBS_API', hamcrest.has_properties(state=0, rate=0))
                ),
            )
        )
        hamcrest.assert_that(
            src_invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=30,
                locked_sum=40,
            )
        )

    def test_w_withdraw(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        order = src_invoice.invoice_orders[0].order
        src_invoice.transfer(order, TransferMode.src, 70)
        invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(40)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                src_invoice=src_invoice,
                dst_invoice=dst_invoice,
                amount=40,
                status_code=InvoiceTransferStatus.not_exported,
                exports=hamcrest.all_of(
                    hamcrest.has_length(1),
                    hamcrest.has_entry('OEBS_API', hamcrest.has_properties(state=0, rate=0))
                ),
            )
        )
        hamcrest.assert_that(
            src_invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=60,
                locked_sum=40,
            )
        )

    def test_w_withdraw_instant(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        order = src_invoice.invoice_orders[0].order
        src_invoice.transfer(order, TransferMode.src, 70)
        invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(0, True)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                src_invoice=src_invoice,
                dst_invoice=dst_invoice,
                amount=100,
                status_code=InvoiceTransferStatus.not_exported,
                exports=hamcrest.all_of(
                    hamcrest.has_length(1),
                    hamcrest.has_entry('OEBS_API', hamcrest.has_properties(state=0, rate=0))
                ),
            )
        )
        hamcrest.assert_that(
            src_invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=0,
                locked_sum=100,
            )
        )

    def test_fail_not_enough_funds(self, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_INVOICE_TRANSFER):
            a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(D('100.01'))

    def test_fail_negative_amount(self, session, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_INVOICE_TRANSFER):
            a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(-1)

    def test_fail_withdraw_permissions(self, session, src_invoice, dst_invoice):
        order = src_invoice.invoice_orders[0].order
        order.service_id = ServiceId.MEDIA_SELLING
        session.flush()

        src_invoice.create_receipt(100)
        src_invoice.transfer(order, TransferMode.src, 70)
        with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_INVOICE_TRANSFER):
            a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(40)

    def test_fail_desync(self, src_invoice, dst_invoice):
        src_invoice.create_receipt(100)
        order = src_invoice.invoice_orders[0].order
        src_invoice.transfer(order, TransferMode.src, 30)
        with pytest.raises(exc.DESYNCHRONIZED_INVOICE_TRANSFER_DATA) as exc_info:
            a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(40, previous_available_invoice_transfer_sum=12)
        assert 'Available sum for transfer changed for invoice' in exc_info.value.msg

    def test_ok_online_payment_ci(self, session, client):
        session.config.__dict__['ONLINE_TRANSFER_DELAY'] = 1
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_invoice.person)
        ocpf_dt = datetime.datetime.now() - datetime.timedelta(days=2)
        create_cpf_src_invoice(session, src_invoice, 100, OebsOperationType.ONLINE, ocpf_dt - datetime.timedelta(days=2))
        create_cpf_src_invoice(session, src_invoice, 100, OebsOperationType.ONLINE, ocpf_dt)
        src_invoice.create_receipt(100)
        invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(100)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                src_invoice=src_invoice,
                dst_invoice=dst_invoice,
                amount=100,
                status_code=InvoiceTransferStatus.not_exported,
                exports=hamcrest.all_of(
                    hamcrest.has_length(1),
                    hamcrest.has_entry('OEBS_API', hamcrest.has_properties(state=0, rate=0))
                ),
            )
        )
        hamcrest.assert_that(
            src_invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=0,
                locked_sum=100,
            )
        )

    def test_fail_online_payment_ci(self, session, client):
        session.config.__dict__['ONLINE_TRANSFER_DELAY'] = 1
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_invoice.person)
        ocpf_dt = datetime.datetime.now()
        create_cpf_src_invoice(session, src_invoice, 100, OebsOperationType.ONLINE, ocpf_dt - datetime.timedelta(days=2))
        create_cpf_src_invoice(session, src_invoice, 100, OebsOperationType.ONLINE, ocpf_dt)
        src_invoice.create_receipt(100)
        with pytest.raises(exc.INVOICE_TRANSFER_FORBIDDEN_ONLINE_PAYMENT) as exc_info:
            invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(100)
        assert 'Invoice transfer is not allowed from invoice' in exc_info.value.msg
        assert 'because less than 1 days passed since the last ONLINE payment' in exc_info.value.msg

    def test_ok_online_payment_ai(self, session, client):
        session.config.__dict__['ONLINE_TRANSFER_DELAY'] = 1
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_invoice.person)
        ocpf_dt = datetime.datetime.now()
        create_cpf_src_invoice(session, src_invoice, 100, OebsOperationType.ONLINE, ocpf_dt - datetime.timedelta(days=2))
        create_cpf_src_invoice(session, src_invoice, 100, OebsOperationType.ONLINE, ocpf_dt)
        src_invoice.create_receipt(100)
        invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice, as_admin=True).create(100)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                src_invoice=src_invoice,
                dst_invoice=dst_invoice,
                amount=100,
                status_code=InvoiceTransferStatus.not_exported,
                exports=hamcrest.all_of(
                    hamcrest.has_length(1),
                    hamcrest.has_entry('OEBS_API', hamcrest.has_properties(state=0, rate=0))
                ),
            )
        )
        hamcrest.assert_that(
            src_invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=0,
                locked_sum=100,
            )
        )

    def test_fail_legal_entities_different_clients(self, session, client):
        other_client = ob.ClientBuilder.construct(session, service=ServiceId.DIRECT)
        src_person =  create_person(client, type='ur', inn=str(ob.generate_int(12)))
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_person)
        dst_person =  create_person(other_client, type='ur', inn=str(ob.generate_int(12)))
        dst_invoice = create_invoice(other_client, PAYSYS_ID_BANK, 100, person=dst_person)
        create_cpf_src_invoice(session, src_invoice, 100)
        src_invoice.create_receipt(100)
        with pytest.raises(exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_CLIENTS) as exc_info:
            invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(100)
        assert "forbidden for different clients" in exc_info.value.msg

    def test_fail_legal_entities_different_person_types(self, session, client):
        src_person =  create_person(client, type='ur', inn=str(ob.generate_int(12)))
        dst_person =  create_person(client, type='byu', inn=str(ob.generate_int(12)))
        src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_person)
        dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=dst_person)
        create_cpf_src_invoice(session, src_invoice, 100)
        src_invoice.create_receipt(100)
        with pytest.raises(exc.INVOICE_TRANSFER_FORBIDDEN_DIFFERENT_PERSON_TYPES) as exc_info:
            invoice_transfer = a_it.InvoiceTransferManager(src_invoice, dst_invoice).create(100)
        assert "forbidden for persons with different types ('ur' and 'byu')" in exc_info.value.msg

