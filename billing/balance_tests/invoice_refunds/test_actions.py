# -*- coding: utf-8 -*-

import collections
import datetime
import uuid
import decimal

import pytest
import hamcrest

from balance import mapper
from balance.actions import invoice_refunds as a_ir
from balance import exc
from balance.constants import *

from tests import object_builder as ob
from tests.balance_tests.invoice_refunds.common import (
    create_order,
    create_invoice,
    create_ocpf,
    create_bank_cpf,
    create_ym_payment,
    create_wm_payment,
    create_payment_ocpf,
    create_trust_payment,
)
from tests.balance_tests.invoices.unused_funds.common import get_unused_funds_cache

D = decimal.Decimal

PAYSYS_ID_YM = 1000
PAYSYS_ID_BANK = 1001
PAYSYS_ID_CARD = 1002
PAYSYS_ID_BANK_UR = 1003
PAYSYS_ID_BANK_NONRES = 1014
PAYSYS_ID_BANK_KZ = 1021
PAYSYS_ID_WM = 1052


pytestmark = [
    pytest.mark.invoice_refunds,
]


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def order(client):
    return create_order(client)


@pytest.fixture
def bank_cpf(session, client, order):
    invoice = create_invoice(client, order, PAYSYS_ID_BANK, 100)
    return create_bank_cpf(invoice, 100)


@pytest.fixture
def ym_cpf(session, client, order):
    invoice = create_invoice(client, order, PAYSYS_ID_YM, 100)
    payment = create_ym_payment(invoice)
    cpf = create_payment_ocpf(payment)
    cpf.source_id = uuid.uuid4().int
    cpf.cash_receipt_number = 'cash_receipt_number'
    session.flush()
    return cpf


@pytest.fixture
def wm_cpf(session, client, order):
    invoice = create_invoice(client, order, PAYSYS_ID_WM, 100)
    payment = create_wm_payment(invoice)
    cpf = create_payment_ocpf(payment)
    cpf.source_id = uuid.uuid4().int
    cpf.cash_receipt_number = 'cash_receipt_number'
    session.flush()
    return cpf


@pytest.fixture
def trust_cpf(session, client, order):
    invoice = create_invoice(client, order, PAYSYS_ID_CARD, 100)
    payment = create_trust_payment(invoice)
    cpf = create_payment_ocpf(payment)
    cpf.source_id = uuid.uuid4().int
    cpf.cash_receipt_number = 'cash_receipt_number'
    session.flush()
    return cpf


@pytest.mark.parametrize('strict', [False, True], ids=['not_strict', 'strict'])
class TestCheckAvailability(object):
    @staticmethod
    def _assert_fail(cpf, strict, exception, msg, add_reqs=SENTINEL):
        ref_manager = a_ir.InvoiceRefundManager(cpf, add_reqs)

        if strict:
            hamcrest.assert_that(
                hamcrest.calling(ref_manager.check_availability).with_args(strict),
                hamcrest.raises(exception, msg)
            )
        else:
            assert ref_manager.check_availability(strict) is False

    @pytest.mark.parametrize(
        'add_reqs',
        [SENTINEL, None],
        ids=['wo_add_reqs', 'w_add_reqs']
    )
    @pytest.mark.parametrize('operation_type', [OebsOperationType.ONLINE, OebsOperationType.INSERT])
    def test_ok_bank(self, session, bank_cpf, add_reqs, operation_type, strict):
        bank_cpf.operation_type = operation_type
        bank_cpf.invoice.create_receipt(100)

        assert a_ir.InvoiceRefundManager(bank_cpf, add_reqs).check_availability(strict) is True

    @pytest.mark.parametrize(
        'add_reqs',
        [SENTINEL, {'transaction_num': 666, 'wallet_num': 'abc'}],
        ids=['wo_add_reqs', 'w_add_reqs']
    )
    def test_ok_ym(self, ym_cpf, add_reqs, strict):
        ym_cpf.invoice.create_receipt(100)
        assert a_ir.InvoiceRefundManager(ym_cpf, add_reqs).check_availability(strict) is True

    def test_ok_ym_builtin_requisites(self, session, ym_cpf, strict):
        payment = session.query(mapper.Payment).get(ym_cpf.orig_id)
        payment.user_account = 'aedqweqweqw'
        payment.transaction_id = uuid.uuid4().hex
        ym_cpf.invoice.create_receipt(100)

        assert a_ir.InvoiceRefundManager(ym_cpf, None).check_availability(strict) is True

    @pytest.mark.parametrize(
        'add_reqs',
        [SENTINEL, {'transaction_num': 666}],
        ids=['wo_add_reqs', 'w_add_reqs']
    )
    def test_ok_wm(self, wm_cpf, add_reqs, strict):
        wm_cpf.invoice.create_receipt(100)
        assert a_ir.InvoiceRefundManager(wm_cpf, add_reqs).check_availability(strict) is True

    def test_ok_wm_builtin_requisites(self, session, wm_cpf, strict):
        payment = session.query(mapper.Payment).get(wm_cpf.orig_id)
        payment.transaction_id = uuid.uuid4().hex
        wm_cpf.invoice.create_receipt(100)

        assert a_ir.InvoiceRefundManager(wm_cpf, None).check_availability(strict) is True

    def test_ok_trust(self, session, trust_cpf, strict):
        payment = session.query(mapper.Payment).get(trust_cpf.orig_id)
        payment.transaction_id = uuid.uuid4().hex
        trust_cpf.invoice.create_receipt(100)

        assert a_ir.InvoiceRefundManager(trust_cpf, None).check_availability(strict) is True

    @pytest.mark.permissions
    def test_fail_perms(self, session, bank_cpf, strict):
        bank_cpf.invoice.create_receipt(100)
        ob.create_passport(session, patch_session=True)

        self._assert_fail(bank_cpf, strict, exc.PERMISSION_DENIED, 'no permission DoInvoiceRefunds')

    @pytest.mark.permissions
    def test_fail_perms_trust(self, session, trust_cpf, strict):
        payment = session.query(mapper.Payment).get(trust_cpf.orig_id)
        payment.transaction_id = uuid.uuid4().hex
        trust_cpf.invoice.create_receipt(100)

        role = ob.create_role(session, PermissionCode.DO_INVOICE_REFUNDS)
        ob.create_passport(session, role, patch_session=True)

        self._assert_fail(trust_cpf, strict, exc.PERMISSION_DENIED, 'no permission DoInvoiceRefundsTrust')

    @pytest.mark.permissions
    def test_fail_withdraw_perms(self, session, bank_cpf, strict):
        invoice = bank_cpf.invoice
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()
        role = ob.create_role(session, PermissionCode.DO_INVOICE_REFUNDS)
        ob.create_passport(session, role, patch_session=True)

        self._assert_fail(bank_cpf, strict, exc.INVALID_PARAM, 'No unused funds and withdraw not allowed')

    def test_fail_refund(self, session, bank_cpf, strict):
        bank_cpf.amount = -bank_cpf.amount
        bank_cpf.invoice.create_receipt(100)

        self._assert_fail(bank_cpf, strict, exc.NOT_ALLOWED_REFUND, 'payment is a refund')

    def test_fail_avans(self, session, bank_cpf, strict):
        bank_cpf.operation_type = OebsOperationType.SF_AVANS
        bank_cpf.invoice.create_receipt(100)

        self._assert_fail(bank_cpf, strict, exc.NOT_ALLOWED_REFUND, 'payment is a SF_AVANS')

    def test_fail_no_invoice(self, session, strict):
        cpf = ob.OebsCashPaymentFactBuilder(
            amount=666,
            receipt_number=uuid.uuid4().bytes,
            operation_type=OebsOperationType.INSERT
        ).build(session).obj

        self._assert_fail(cpf, strict, exc.NOT_ALLOWED_REFUND, "payment doesn't have an invoice")

    @pytest.mark.parametrize(
        'paysys_id, msg',
        [
            (PAYSYS_ID_BANK_NONRES, 'available only for residents of Russia, but got nonresident of 225'),
            (PAYSYS_ID_BANK_KZ, 'available only for residents of Russia, but got resident of 159'),
        ],
        ids=['nonres', 'country']
    )
    def test_fail_country_resident(self, client, order, paysys_id, msg, strict):
        invoice = create_invoice(client, order, paysys_id, 100)
        invoice.create_receipt(100)
        cpf = create_ocpf(invoice, invoice.effective_sum)

        self._assert_fail(cpf, strict, exc.NOT_ALLOWED_REFUND, msg)

    def test_fail_invoice_type(self, session, client, order, strict):
        contract = ob.create_credit_contract(session)
        contract.col0.personal_account_fictive = 0
        order = create_order(contract.client)
        basket = ob.BasketBuilder(
            client=contract.client,
            rows=[ob.BasketItemBuilder(quantity=6666, order=order)]
        )
        paysys = ob.Getter(mapper.Paysys, PAYSYS_ID_BANK_UR).build(session).obj
        pa, = ob.PayOnCreditCase(session).pay_on_credit(basket, contract, paysys)

        cpf = create_ocpf(pa, 100)
        self._assert_fail(
            cpf, strict, exc.NOT_ALLOWED_REFUND,
            "allowed only for prepayment and overdraft, got personal_account"
        )

    @pytest.mark.parametrize(
        'lock_type',
        [
            pytest.param(InvoiceReceiptLockType.TRANSFER, id='UNUSED_FUNDS_LOCK_TRANSFER'),
            pytest.param(InvoiceReceiptLockType.REFUND, id='UNUSED_FUNDS_LOCK_REFUND'),
        ]
    )
    def test_ok_unused_funds_lock(self, session, bank_cpf, strict, lock_type):
        bank_cpf.invoice.unused_funds_lock = lock_type
        bank_cpf.invoice.create_receipt(100)

        assert a_ir.InvoiceRefundManager(bank_cpf, None).check_availability(strict) is True

    def test_fail_unused_funds_lock_overdraft(self, session, bank_cpf, strict):
        bank_cpf.invoice.unused_funds_lock = InvoiceReceiptLockType.OVERDRAFT
        bank_cpf.invoice.create_receipt(100)

        self._assert_fail(bank_cpf, strict, exc.NOT_ALLOWED_REFUND, 'invoice funds are locked by overdue overdraft')

    def test_fail_bank_cpf_type(self, session, bank_cpf, strict):
        bank_cpf.operation_type = 'OFFLINE'
        bank_cpf.invoice.create_receipt(100)

        self._assert_fail(bank_cpf, strict, exc.NOT_ALLOWED_REFUND, 'unsupported payment type')

    @pytest.mark.parametrize(
        'req_name, msg',
        [
            pytest.param(
                'source_id', 'Invalid requisites for refund with handler OEBSBankPaymentHandler: '
                             'missing source_id',
                id='source_id'
            ),
            pytest.param(
                'cash_receipt_number', 'Invalid requisites for refund with handler OEBSBankPaymentHandler: '
                                       'missing receipt_number',
                id='receipt_number'
            ),
            pytest.param(
                'account_name', 'Invalid requisites for refund with handler OEBSBankPaymentHandler: '
                                'missing account',
                id='account_name'
            ),
            pytest.param(
                'customer_name', 'Invalid requisites for refund with handler OEBSBankPaymentHandler: '
                                 'missing customer_name',
                id='customer_name'
            ),
            pytest.param(
                'inn', 'Invalid requisites for refund with handler OEBSBankPaymentHandler: '
                       'missing inn',
                id='inn'
            ),
            pytest.param(
                'bik', 'Invalid requisites for refund with handler OEBSBankPaymentHandler: '
                       'missing bik',
                id='bik'
            ),
        ]
    )
    def test_fail_bank_builtin_reqs(self, session, bank_cpf, req_name, msg, strict):
        setattr(bank_cpf, req_name, None)
        bank_cpf.invoice.create_receipt(100)

        self._assert_fail(bank_cpf, strict, exc.INVALID_REFUND_REQUISITES, msg, None)

    def test_fail_bank_unmatched_inn(self, session, bank_cpf, strict):
        bank_cpf.invoice.person.inn = '123456789'
        bank_cpf.inn = '987654321'
        bank_cpf.invoice.create_receipt(100)

        self._assert_fail(
            bank_cpf, strict, exc.INVALID_REFUND_REQUISITES,
            "Invalid requisites for refund with handler OEBSBankPaymentHandler:"
            " payer's inn doesn't match inn of invoice's person"
        )

    def test_fail_oebs_instant_no_orig_id(self, session, ym_cpf, strict):
        ym_cpf.orig_id = None
        ym_cpf.invoice.create_receipt(100)

        self._assert_fail(ym_cpf, strict, exc.NOT_ALLOWED_REFUND, 'unsupported payment type')

    def test_fail_oebs_instant_no_payment(self, session, ym_cpf, strict):
        ym_cpf.orig_id = uuid.uuid4().int
        ym_cpf.invoice.create_receipt(100)

        self._assert_fail(ym_cpf, strict, exc.NOT_ALLOWED_REFUND, 'unsupported payment type')

    def test_fail_oebs_instant_cpf_type(self, session, ym_cpf, strict):
        ym_cpf.operation_type = 'PASSIVITY'
        ym_cpf.invoice.create_receipt(100)

        self._assert_fail(ym_cpf, strict, exc.NOT_ALLOWED_REFUND, 'unsupported payment type')

    @pytest.mark.parametrize('payment_method', [PaymentMethodIDs.credit_card, None])
    def test_fail_oebs_instant_payment_type(self, session, client, order, payment_method, strict):
        invoice = create_invoice(client, order, PAYSYS_ID_CARD, 100)
        payment = ob.CardPaymentBuilder(invoice=invoice).build(session).obj
        payment.payment_method_id = payment_method
        cpf = create_payment_ocpf(payment)
        invoice.create_receipt(100)

        self._assert_fail(cpf, strict, exc.NOT_ALLOWED_REFUND, 'unsupported payment type')

    @pytest.mark.parametrize(
        'req_name, msg',
        [
            pytest.param(
                'source_id', 'Invalid requisites for refund with handler OEBSYandexMoneyPaymentHandler: missing source_id',
                id='source_id'
            ),
            pytest.param(
                'cash_receipt_number', 'Invalid requisites for refund with handler OEBSYandexMoneyPaymentHandler: missing receipt_number',
                id='receipt_number'
            ),
        ]
    )
    def test_fail_ym_builtin_reqs(self, session, ym_cpf, req_name, msg, strict):
        setattr(ym_cpf, req_name, None)
        ym_cpf.invoice.create_receipt(100)

        self._assert_fail(
            ym_cpf,
            strict,
            exc.INVALID_REFUND_REQUISITES,
            msg,
            {'transaction_num': 666, 'wallet_num': 'abc'}
        )

    @pytest.mark.parametrize(
        'add_reqs, msg',
        [
            pytest.param(
                None,
                'Invalid requisites for refund with handler OEBSYandexMoneyPaymentHandler: missing wallet_num, transaction_num',
                id='none'
            ),
            pytest.param(
                {},
                'Invalid requisites for refund with handler OEBSYandexMoneyPaymentHandler: missing wallet_num, transaction_num',
                id='empty_dict'
            ),
            pytest.param(
                {'wallet_num': 666},
                'Invalid requisites for refund with handler OEBSYandexMoneyPaymentHandler: missing transaction_num',
                id='wo_payment'
            ),
            pytest.param(
                {'transaction_num': 6},
                'Invalid requisites for refund with handler OEBSYandexMoneyPaymentHandler: missing wallet_num',
                id='wo_wallet'
            ),
        ],
    )
    def test_fail_ym_requisites(self, ym_cpf, add_reqs, msg, strict):
        ym_cpf.invoice.create_receipt(100)
        self._assert_fail(ym_cpf, strict, exc.INVALID_REFUND_REQUISITES, msg, add_reqs)

    @pytest.mark.parametrize(
        'req_name, msg',
        [
            pytest.param(
                'source_id', 'Invalid requisites for refund with handler OEBSWebMoneyPaymentHandler: missing source_id',
                id='source_id'
            ),
            pytest.param(
                'cash_receipt_number', 'Invalid requisites for refund with handler OEBSWebMoneyPaymentHandler: '
                                       'missing receipt_number',
                id='receipt_number'
            ),
        ]
    )
    def test_fail_wm_builtin_reqs(self, session, wm_cpf, req_name, msg, strict):
        setattr(wm_cpf, req_name, None)
        wm_cpf.invoice.create_receipt(100)

        self._assert_fail(
            wm_cpf,
            strict,
            exc.INVALID_REFUND_REQUISITES,
            msg,
            {'transaction_num': 666}
        )

    @pytest.mark.parametrize(
        'add_reqs, msg',
        [
            pytest.param(
                None,
                'Invalid requisites for refund with handler OEBSWebMoneyPaymentHandler: missing transaction_num',
                id='none'
            ),
            pytest.param(
                {},
                'Invalid requisites for refund with handler OEBSWebMoneyPaymentHandler: missing transaction_num',
                id='empty_dict'
            ),
        ],
    )
    def test_fail_wm_requisites(self, wm_cpf, add_reqs, msg, strict):
        wm_cpf.invoice.create_receipt(100)
        self._assert_fail(wm_cpf, strict, exc.INVALID_REFUND_REQUISITES, msg, add_reqs)

    def test_fail_trust_requisites(self, session, trust_cpf, strict):
        trust_cpf.invoice.create_receipt(100)

        msg = 'Invalid requisites for refund with handler TrustPaymentHandler: missing purchase_token'
        self._assert_fail(trust_cpf, strict, exc.INVALID_REFUND_REQUISITES, msg, None)


class TestRefundableAmount(object):
    def test_new(self, bank_cpf):
        assert a_ir.InvoiceRefundManager(bank_cpf).get_refundable_amount() == 0

    def test_unused(self, bank_cpf):
        bank_cpf.invoice.create_receipt(100)
        assert a_ir.InvoiceRefundManager(bank_cpf).get_refundable_amount() == 100

    def test_consumed(self, bank_cpf):
        bank_cpf.invoice.create_receipt(100)
        bank_cpf.invoice.turn_on_rows()

        assert a_ir.InvoiceRefundManager(bank_cpf).get_refundable_amount() == 100

    def test_completed(self, session, bank_cpf, order):
        bank_cpf.invoice.create_receipt(100)
        bank_cpf.invoice.transfer(order)
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 66})
        session.flush()

        assert a_ir.InvoiceRefundManager(bank_cpf).get_refundable_amount() == 34

    def test_overacted(self, session, client):
        order1 = create_order(client)
        order2 = create_order(client)
        invoice = create_invoice(client, [order1, order2], PAYSYS_ID_BANK, 100)
        cpf = create_bank_cpf(invoice, invoice.effective_sum)
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        order1.calculate_consumption(datetime.datetime.now(), {order1.shipment_type: 66})
        invoice.generate_act(backdate=datetime.datetime.now(), force=1)
        order1.calculate_consumption(datetime.datetime.now(), {order1.shipment_type: 0})

        order2.calculate_consumption(datetime.datetime.now(), {order2.shipment_type: 34})
        session.flush()

        assert a_ir.InvoiceRefundManager(cpf).get_refundable_amount() == 100

    def test_refund_in_progress(self, session, bank_cpf, order):
        refund = ob.InvoiceRefundBuilder(
            invoice=bank_cpf.invoice,
            payment_id=bank_cpf.id,
            amount=34
        ).build(session).obj
        refund.set_status(InvoiceRefundStatus.exported)
        session.flush()

        bank_cpf.invoice.create_receipt(100)
        bank_cpf.invoice.transfer(order, TransferMode.src, 30)

        assert a_ir.InvoiceRefundManager(bank_cpf).get_refundable_amount() == 66

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
    def test_invoice_transfers_from_in_progress(self, session, bank_cpf, order, invoice_transfer_status, result):
        invoice_sum = 100
        consume_sum = 30

        dst_invoice = create_invoice(bank_cpf.invoice.client, order, PAYSYS_ID_BANK)
        bank_cpf.invoice.create_receipt(invoice_sum)
        bank_cpf.invoice.transfer(bank_cpf.invoice.invoice_orders[0].order, TransferMode.src, consume_sum)

        invoice_transfer = ob.InvoiceTransferBuilder(
            src_invoice=bank_cpf.invoice,
            dst_invoice=dst_invoice,
            amount=34
        ).build(session).obj
        invoice_transfer.set_status(invoice_transfer_status)
        session.flush()

        assert bank_cpf.invoice.unused_funds == result - consume_sum
        assert (
            get_unused_funds_cache(session, bank_cpf.invoice.id)[0].unused_funds
            == bank_cpf.invoice.unused_funds_invoice_currency
        )
        assert bank_cpf.invoice._locked_sum == (invoice_sum - result)
        assert a_ir.InvoiceRefundManager(bank_cpf).get_refundable_amount() == result

    def test_invoice_transfers_to_in_progress(self, session, bank_cpf, order):
        src_invoice = create_invoice(bank_cpf.invoice.client, order, PAYSYS_ID_BANK)
        invoice_transfer = ob.InvoiceTransferBuilder(
            src_invoice=src_invoice,
            dst_invoice=bank_cpf.invoice,
            amount=34
        ).build(session).obj
        session.flush()

        bank_cpf.invoice.create_receipt(100)
        bank_cpf.invoice.transfer(bank_cpf.invoice.invoice_orders[0].order, TransferMode.src, 30)
        bank_cpf.orig_id = invoice_transfer.id
        session.flush()

        assert bank_cpf.invoice.unused_funds == 70
        assert bank_cpf.invoice._locked_sum == 0
        assert a_ir.InvoiceRefundManager(bank_cpf).get_refundable_amount() == 0

    @pytest.mark.parametrize(
        'skip, result',
        [
            pytest.param(False, 0, id='dont_skip'),
            pytest.param(True, 100, id='skip'),
        ]
    )
    def test_unavailable(self, session, bank_cpf, skip, result):
        bank_cpf.invoice.create_receipt(100)
        bank_cpf.invoice.turn_on_rows()

        ob.create_passport(session, patch_session=True)  # убираем все права вообще, возврат недоступен
        assert a_ir.InvoiceRefundManager(bank_cpf).get_refundable_amount(skip_availability_check=skip) == result


class TestCreate(object):
    def test_wo_withdraw(self, bank_cpf, order):
        invoice = bank_cpf.invoice
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.src, 30)
        refund = a_ir.InvoiceRefundManager(bank_cpf, None).create(40)

        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                invoice=bank_cpf.invoice,
                cash_payment_fact=bank_cpf,
                amount=40,
                status_code=InvoiceRefundStatus.not_exported,
                payload=None,
                exports=hamcrest.all_of(
                    hamcrest.has_length(1),
                    hamcrest.has_entry('OEBS_API', hamcrest.has_properties(state=0, rate=0))
                ),
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=30,
                locked_sum=40,
            )
        )

    def test_w_withdraw(self, bank_cpf, order):
        invoice = bank_cpf.invoice
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.src, 70)
        refund = a_ir.InvoiceRefundManager(bank_cpf, None).create(40)

        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                invoice=bank_cpf.invoice,
                cash_payment_fact=bank_cpf,
                amount=40,
                status_code=InvoiceRefundStatus.not_exported,
                payload=None,
                exports=hamcrest.all_of(
                    hamcrest.has_length(1),
                    hamcrest.has_entry('OEBS_API', hamcrest.has_properties(state=0, rate=0))
                ),
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=60,
                locked_sum=40,
            )
        )

    def test_w_withdraw_instant(self, ym_cpf, order):
        invoice = ym_cpf.invoice
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.all)
        refund = a_ir.InvoiceRefundManager(ym_cpf, {'transaction_num': 666, 'wallet_num': 'qeqweqweqw'}).create(100)

        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                invoice=ym_cpf.invoice,
                cash_payment_fact=ym_cpf,
                amount=100,
                status_code=InvoiceRefundStatus.not_exported,
                payload={'transaction_num': 666, 'wallet_num': 'qeqweqweqw'},
                exports=hamcrest.all_of(
                    hamcrest.has_length(1),
                    hamcrest.has_entry('OEBS_API', hamcrest.has_properties(state=0, rate=0))
                ),
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=0,
                locked_sum=100,
            )
        )

    def test_trust(self, session, trust_cpf):
        payment = session.query(mapper.Payment).get(trust_cpf.orig_id)
        payment.transaction_id = uuid.uuid4().hex

        invoice = trust_cpf.invoice
        invoice.create_receipt(100)

        refund = a_ir.InvoiceRefundManager(trust_cpf, None).create(40)

        hamcrest.assert_that(
            refund,
            hamcrest.all_of(
                hamcrest.instance_of(mapper.TrustApiCPFInvoiceRefund),
                hamcrest.has_properties(
                    invoice=trust_cpf.invoice,
                    cash_payment_fact=trust_cpf,
                    amount=40,
                    status_code=InvoiceRefundStatus.uninitialized,
                    payload=None,
                    exports=hamcrest.all_of(
                        hamcrest.has_length(1),
                        hamcrest.has_entry('TRUST_API', hamcrest.has_properties(state=0, rate=0))
                    ),
                )
            )
        )

    def test_fail_requisites(self, ym_cpf, order):
        invoice = ym_cpf.invoice
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.all)

        with pytest.raises(exc.INVALID_REFUND_REQUISITES) as exc_info:
            a_ir.InvoiceRefundManager(ym_cpf, {'transaction_num': 666}).create(100)

        assert 'missing wallet_num' in exc_info.value.msg

    def test_fail_handler_check(self, bank_cpf, order):
        invoice = bank_cpf.invoice
        invoice.person.inn = '1231231'
        invoice.create_receipt(100)
        with pytest.raises(exc.INVALID_REFUND_REQUISITES) as exc_info:
            a_ir.InvoiceRefundManager(bank_cpf, None).create(100)
        assert "payer's inn doesn't match inn of invoice's person" in exc_info.value.msg

    def test_fail_not_enough_funds(self, bank_cpf, order):
        invoice = bank_cpf.invoice
        invoice.create_receipt(100)
        with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_REFUND):
            a_ir.InvoiceRefundManager(bank_cpf, None).create(D('100.01'))

    def test_fail_negative_amount(self, session, bank_cpf, order):
        invoice = bank_cpf.invoice
        invoice.create_receipt(100)
        session.flush()
        with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_REFUND):
            a_ir.InvoiceRefundManager(bank_cpf, None).create(-1)

    def test_fail_withdraw_permissions(self, bank_cpf, order):
        invoice = bank_cpf.invoice
        order.service_id = ServiceId.MEDIA_SELLING
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.src, 70)
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            a_ir.InvoiceRefundManager(bank_cpf, None).create(40)
        assert 'with disallowed services for payment type' in exc_info.value.msg

    def test_fail_desync(self, bank_cpf, order):
        invoice = bank_cpf.invoice
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.src, 30)
        with pytest.raises(exc.DESYNCHRONIZED_REFUND_DATA) as exc_info:
            a_ir.InvoiceRefundManager(bank_cpf, None).create(40, 66)
        assert 'Available sum changed for payment' in exc_info.value.msg


class TestGetEditableRequisites(object):
    def test_bank(self, bank_cpf):
        assert a_ir.InvoiceRefundManager(bank_cpf).get_editable_requisites() == []

    def test_ym(self, ym_cpf):
        assert a_ir.InvoiceRefundManager(ym_cpf).get_editable_requisites() == ['wallet_num', 'transaction_num']

    def test_wm(self, wm_cpf):
        assert a_ir.InvoiceRefundManager(wm_cpf).get_editable_requisites() == ['transaction_num']

    def test_trust(self, trust_cpf):
        assert a_ir.InvoiceRefundManager(trust_cpf).get_editable_requisites() == []


class TestGetRequisites(object):
    def test_bank(self, bank_cpf):
        requisites = a_ir.InvoiceRefundManager(bank_cpf).get_requisites()
        assert requisites == {
            'source_id': bank_cpf.source_id,
            'receipt_number': 'cash_receipt_number',
            'operation_type': OebsOperationType.ONLINE,
            'account': 'account_name',
            'bik': 'bik',
            'inn': 'inn',
            'customer_name': 'customer_name',
        }

    def test_ym_builtin(self, session, ym_cpf):
        payment = session.query(mapper.Payment).get(ym_cpf.orig_id)
        payment.user_account = uuid.uuid4().hex
        payment.transaction_id = uuid.uuid4().hex
        session.flush()

        requisites = a_ir.InvoiceRefundManager(ym_cpf).get_requisites()
        assert requisites == {
            'source_id': ym_cpf.source_id,
            'receipt_number': 'cash_receipt_number',
            'operation_type': OebsOperationType.ACTIVITY,
            'wallet_num': payment.user_account,
            'transaction_num': payment.transaction_id,
        }

    def test_ym_add(self, ym_cpf):
        requisites = a_ir.InvoiceRefundManager(
            ym_cpf,
            {'wallet_num': '1234', 'transaction_num': '4321'}
        ).get_requisites()
        assert requisites == {
            'source_id': ym_cpf.source_id,
            'receipt_number': 'cash_receipt_number',
            'operation_type': OebsOperationType.ACTIVITY,
            'wallet_num': '1234',
            'transaction_num': '4321',
        }

    def test_wm_builtin(self, session, wm_cpf):
        payment = session.query(mapper.Payment).get(wm_cpf.orig_id)
        payment.transaction_id = uuid.uuid4().hex
        session.flush()

        requisites = a_ir.InvoiceRefundManager(wm_cpf).get_requisites()
        assert requisites == {
            'source_id': wm_cpf.source_id,
            'receipt_number': 'cash_receipt_number',
            'operation_type': OebsOperationType.ACTIVITY,
            'transaction_num': payment.transaction_id,
        }

    def test_wm_add(self, wm_cpf):
        requisites = a_ir.InvoiceRefundManager(wm_cpf, {'transaction_num': '4321'}).get_requisites()
        assert requisites == {
            'source_id': wm_cpf.source_id,
            'receipt_number': 'cash_receipt_number',
            'operation_type': OebsOperationType.ACTIVITY,
            'transaction_num': '4321',
        }

    def test_trust(self, session, trust_cpf, order):
        payment = session.query(mapper.Payment).get(trust_cpf.orig_id)
        payment.transaction_id = uuid.uuid4().hex
        session.flush()

        requisites = a_ir.InvoiceRefundManager(trust_cpf, None).get_requisites()
        external_requisites = a_ir.InvoiceRefundManager(trust_cpf, None).get_external_requisites()
        assert requisites == {
            'purchase_token': payment.transaction_id,
            'service_token': order.service.token,
            'passport_id': session.oper_id,
        }
        assert external_requisites == {
            'purchase_token': payment.transaction_id,
        }
