# -*- coding: utf-8 -*-
import collections
import uuid
import datetime
import functools
import pytest
import itertools
import hamcrest
import mock

from balance import mapper, muzzle_util as ut
from balance.constants import *
from balance.queue_processor import QueueProcessor
from balance import core
from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.actions import single_account
from balance.exc import (
    DEFER_INVOICE_TRANSFER_ERROR,
    DEFER_INVOICE_TRANSFER_ERROR_NOT_EXPORTED,
    INVOICE_TRANSFER_SUM_ERROR,
)
from balance.processors import process_payments
from balance.actions.process_completions import ProcessCompletions
from butils.exc import INVALID_PARAM

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_refund,
    create_invoice,
    create_invoice_transfer,
    create_personal_account,
    create_order,
    create_charge_note_register,
    create_credit_contract,
)

PAYSYS_ID_BANK = 1001
PAYSYS_ID_CARD = 1002
PAYSYS_ID_BANK_UR = 1003
PAYSYS_ID_CARD_UR = 1033
ON_DT = ut.trunc_date(datetime.datetime.now())


QueueProcessorPatch = collections.namedtuple(
    'QueueProcessorPatch',
    ['queue_name', 'class_name', 'new_handler']
)


@pytest.fixture()
def patched_queue_processor(request):
    queue_name, class_name, new_handler = request.param
    queue_processor = QueueProcessor(queue_name)
    old_handler = queue_processor.type_processors[class_name]
    queue_processor.type_processors[class_name] = new_handler
    yield queue_processor
    queue_processor.type_processors[class_name] = old_handler


def handle_invoice_invalid_param(immutable_invoice):
    raise INVALID_PARAM('multiple cash_payment_facts with key 666')


def create_payment(invoice):
    payment = ob.CardPaymentBuilder(invoice=invoice).build(invoice.session).obj
    payment.receipt_sum = 0
    invoice.session.flush()
    return payment


def create_payment_ocpf(payment, amount=None, invoice=None):
    cpf = ob.OebsCashPaymentFactBuilder(
        amount=amount or payment.amount,
        invoice=invoice or payment.invoice,
        operation_type=OebsOperationType.ACTIVITY,
        orig_id=payment.id
    ).build(payment.session).obj
    payment.session.expire_all()  # триггер
    return cpf


def create_ocpf(invoice, amount, operation_type=OebsOperationType.ONLINE, orig_id=None):
    cpf = ob.OebsCashPaymentFactBuilder(
        amount=amount,
        invoice=invoice,
        operation_type=operation_type,
        orig_id=orig_id
    ).build(invoice.session).obj
    invoice.session.expire_all()  # триггер bo.tr_oebs_cp_fact_change
    return cpf


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def order(client):
    return create_order(client)


@pytest.fixture
def service(session):
    service = ob.ServiceBuilder.construct(session)
    service.balance_service.extra_pay = True
    service.balance_service.chargenote_scheme = True
    session.flush()

    return service


def mk_shipment(order, qty, dt=None, money=None, force=True):
    shipment_info = {
        order.shipment_type: qty,
    }
    if order.shipment_type != 'Money':
        shipment_info['Money'] = money

    order.shipment.update(dt or datetime.datetime.now(), shipment_info)
    prev_deny_shipment = order.shipment.deny_shipment
    order.shipment.deny_shipment = None
    ProcessCompletions(order, force_log_tariff_processing=force).process_completions(skip_deny_shipment=True)
    order.shipment.deny_shipment = prev_deny_shipment
    order.session.flush()


@pytest.fixture()
def mock_batch_processor():
    patch_path = 'balance.util.ParallelBatchProcessor.process_batches'
    calls = []

    def _process_batches(_s, func, batches, **kw):
        calls.append(batches)
        return itertools.chain(*map(functools.partial(func, **kw), batches))

    with mock.patch(patch_path, _process_batches):
        yield calls


def create_credit_invoice(session, contract, orders):
    request = ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=contract.client,
            rows=[
                ob.BasketItemBuilder(
                    quantity=qty,
                    order=o,
                )
                for o, qty in orders
            ],
        ),
    )
    inv, = core.Core(session).pay_on_credit(request.id, PAYSYS_ID_BANK, contract.person_id, contract.id)
    return inv


class TestHandlePaymentsBank(object):
    def test_new(self, session):
        invoice = create_invoice(session, 100, PAYSYS_ID_BANK)
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        invoice.receipt_sum_1c = 100

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                confirmed_amount=100,
                receipt_sum=100,
                consume_sum=100,
            )
        )

    def test_increase(self, session, client, order):
        invoice = create_invoice(session, paysys_id=PAYSYS_ID_BANK, client=client, orders=[(order, 100)])
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        invoice.receipt_sum_1c = 100
        invoice.create_receipt(70)
        invoice.transfer(order, TransferMode.all)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                confirmed_amount=100,
                receipt_sum=100,
                consume_sum=70,
            )
        )

    def test_decrease_orderless(self, session, client, order):
        invoice = create_invoice(session, paysys_id=PAYSYS_ID_BANK, client=client, orders=[(order, 100)])
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        invoice.receipt_sum_1c = 70
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.src, 70)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=70,
                confirmed_amount=70,
                receipt_sum=70,
                consume_sum=70,
            )
        )

    def test_decrease_consumed(self, session, client, order):
        invoice = create_invoice(session, paysys_id=PAYSYS_ID_BANK, client=client, orders=[(order, 100)])
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        invoice.receipt_sum_1c = 70
        invoice.create_receipt(100)
        invoice.transfer(order, TransferMode.src, 100)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=70,
                confirmed_amount=70,
                receipt_sum=100,
                consume_sum=100,
            )
        )

    @pytest.mark.parametrize(
        'is_pa_turned_on',
        [
            pytest.param(False, id='pa_not_turned_on'),
            pytest.param(True, id='pa_turned_on'),
        ]
    )
    def test_pa_charge_note_cpf(self, session, service, is_pa_turned_on):
        pa = create_personal_account(session, service_id=service.id)
        pa.enqueue('PROCESS_PAYMENTS')
        export_obj = pa.exports['PROCESS_PAYMENTS']
        order = create_order(pa.client, service_id=service.id)
        if is_pa_turned_on:
            pa.create_receipt(100)

        charge_note = create_invoice(
            session,
            paysys_id=PAYSYS_ID_BANK,
            contract=pa.contract,
            orders=[(order, 100)],
            requested_type='charge_note',
            turn_on_rows=True
        )
        payment, = charge_note.payments
        create_payment_ocpf(payment, 100, pa)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=100,
                consume_sum=100
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=100
            )
        )
        hamcrest.assert_that(
            charge_note,
            hamcrest.has_properties(
                receipt_sum=0,
                consume_sum=0
            )
        )

    @pytest.mark.parametrize(
        'batch_size',
        [3, 10],
    )
    def test_paid_amount(self, mock_batch_processor, swith_paid_amount_flag, session, client, order, batch_size):
        session.config.__dict__['INVOICE_PAID_AMOUNT_PROCESSOR_BATCH_SIZE'] = batch_size

        orders = [(create_order(client), 10) for _i in range(10)]
        invoice = create_invoice(session, 100, PAYSYS_ID_BANK, client=client, orders=orders)
        invoice.turn_on_rows()
        for o, qty in orders:
            mk_shipment(o, qty)
        act, = invoice.generate_act(force=True)

        hamcrest.assert_that(
            act,
            hamcrest.has_properties(
                amount=100,
                paid_amount=0,
            ),
        )

        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        invoice.receipt_sum_1c = 100
        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                confirmed_amount=100,
                receipt_sum=100,
                consume_sum=100,
            )
        )

        session.refresh(act)
        hamcrest.assert_that(
            act,
            hamcrest.has_properties(
                amount=100,
                paid_amount=100,
                rows=hamcrest.contains(*[
                    hamcrest.has_properties(amount=10, paid_amount=10)
                    for _i in range(10)
                ]),
            ),
        )

    @pytest.mark.usefixtures('mock_batch_processor')
    @pytest.mark.usefixtures('swith_paid_amount_flag')
    def test_paid_amount_for_market(self, session):
        agency = ob.ClientBuilder.construct(session, is_agency=True)
        client = ob.ClientBuilder.construct(session, agency=agency)

        order = create_order(client, agency=agency, service_id=ServiceId.MARKET, product_id=MARKET_FISH_PRODUCT_ID)
        contract = create_credit_contract(session, client=agency, services={ServiceId.MARKET}, credit_limit_single=100500, personal_account_fictive=0, auto_credit=1)

        invoice = create_credit_invoice(session, contract=contract, orders=[(order, 1)])
        invoice.turn_on_rows()

        mk_shipment(order, 1)
        act, = invoice.generate_act(force=True)
        assert len(invoice.deferpays) == 1

        invoice.receipt_sum_1c = 100

        invoice.update_paid_amount()
        assert len(invoice.deferpays) == 2
        hamcrest.assert_that(
            invoice.deferpays,
            hamcrest.contains(
                hamcrest.has_properties(effective_sum=30),
                hamcrest.has_properties(effective_sum=30),
            ),
        )

    @pytest.mark.parametrize(
        'receipt_sums, paid_amounts',
        [
            pytest.param([45, 45], [45, 0], id='first uncompleted'),
            pytest.param([50, 50], [50, 0], id='first completed'),
            pytest.param([75, 75], [50, 25], id='second uncompleted'),
            pytest.param([100, 100], [50, 50], id='fully completed'),
            pytest.param([50, 40], [40, 0], id='first reversed'),
            pytest.param([100, 75], [50, 25], id='second reversed'),
            pytest.param([100, 45], [45, 0], id='both reversed'),
        ],
    )
    def test_paid_amount_w_several_acts(self, mock_batch_processor, swith_paid_amount_flag, session, receipt_sums, paid_amounts):
        agency = ob.ClientBuilder.construct(session, is_agency=True, is_docs_separated=True)
        client = ob.ClientBuilder.construct(session, agency=agency)
        o1, o2 = [create_order(client, agency=agency) for _i in range(2)]
        invoice = create_invoice(session, 100, PAYSYS_ID_BANK, client=agency, orders=[(o1, 50), (o2, 50)])
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(on_dt=invoice.dt)

        mk_shipment(o1, 50)
        act1, = invoice.generate_act(force=True)

        mk_shipment(o2, 100)
        act2, = invoice.generate_act(force=True)

        hamcrest.assert_that(
            [act1, act2],
            hamcrest.contains(
                hamcrest.has_properties(amount=50, paid_amount=0),
                hamcrest.has_properties(amount=50, paid_amount=0),
            ),
        )

        invoice.receipt_sum_1c = receipt_sums[0]
        invoice.update_paid_amount()

        invoice.receipt_sum_1c = receipt_sums[1]
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=receipt_sums[1],
                confirmed_amount=receipt_sums[1],
                receipt_sum=100,
                consume_sum=100,
            )
        )

        session.expire_all()
        hamcrest.assert_that(
            [act1, act2],
            hamcrest.contains(*[
                hamcrest.has_properties(
                    amount=50,
                    paid_amount=paid_amount,
                    rows=hamcrest.contains(hamcrest.has_properties(amount=50, paid_amount=paid_amount)),
                )
                for paid_amount in paid_amounts
            ]),
        )

    def test_our_fault(self, mock_batch_processor, swith_paid_amount_flag, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service_id=service.id)
        pa.transfer(order, mode=1, sum=100, skip_check=True)

        mk_shipment(order, 100)
        act, = pa.generate_act(force=True)
        bad_debt = ob.BadDebtActBuilder.construct(session, act=act, our_fault=True)

        pa.receipt_sum_1c = 100
        session.flush()

        pa.enqueue('PROCESS_PAYMENTS')
        export_obj = pa.exports['PROCESS_PAYMENTS']
        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                confirmed_amount=100,
                receipt_sum=100,
                consume_sum=100,
            )
        )

        session.expire_all()
        hamcrest.assert_that(
            act,
            hamcrest.has_properties(
                amount=100,
                paid_amount=0,
                rows=hamcrest.contains(hamcrest.has_properties(amount=100, paid_amount=0)),
            ),
        )


class TestHandlePaymentsInstant(object):
    def test_new_turned_on(self, session):
        invoice = create_invoice(session, 100, PAYSYS_ID_CARD)
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']

        payment = create_payment(invoice)
        payment.turn_on()

        create_payment_ocpf(payment)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum_1c=100,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                receipt_sum=100,
                confirmed_amount=100,
                consume_sum=100,
            )
        )

    def test_new_invoice_not_turned(self, session, client, order):
        invoice = create_invoice(session, 100, PAYSYS_ID_CARD)
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']

        payment = create_payment(invoice)
        invoice.create_receipt(100)
        payment.receipt_sum = 100
        session.flush()

        create_payment_ocpf(payment)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum_1c=100,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                receipt_sum=100,
                confirmed_amount=100,
                consume_sum=100,
            )
        )

    def test_new_payment_not_turned(self, session):
        invoice = create_invoice(session, 100, PAYSYS_ID_CARD)
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']

        payment = create_payment(invoice)
        create_payment_ocpf(payment)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum_1c=100,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                receipt_sum=100,
                confirmed_amount=100,
                consume_sum=100,
            )
        )

    def test_new_unlinked(self, session):
        invoice = create_invoice(session, 100, PAYSYS_ID_CARD)
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']

        # Есть мгновенный платёж, счёт не включен, и приходит CPF не связанный с платежом
        # Счёт должен включиться если хватает денег с платежа, но CPF не учитывается
        payment = create_payment(invoice)
        invoice.create_receipt(100)
        payment.receipt_sum = 100
        session.flush()
        create_ocpf(invoice, 100)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum_1c=None,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=100,
                receipt_sum=100,
                confirmed_amount=200,
                consume_sum=100,
            )
        )

    def test_decrease(self, session, client, order):
        invoice = create_invoice(session, paysys_id=PAYSYS_ID_CARD, client=client, orders=[(order, 100)])
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']

        # Симулируем ручной возврат через ОЕБС - средства на беззаказье, приходит минус по счёту
        # orig_id в отрицательных строках похоже не бывает
        payment = create_payment(invoice)
        invoice.create_receipt(100)
        payment.receipt_sum = 100
        invoice.transfer(order, TransferMode.src, 70)
        create_payment_ocpf(payment)
        create_ocpf(invoice, -30)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum_1c=100,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=70,
                receipt_sum=100,
                confirmed_amount=70,
                consume_sum=70,
            )
        )

    @pytest.mark.parametrize(
        'is_pa_turned_on',
        [
            pytest.param(False, id='pa_not_turned_on'),
            pytest.param(True, id='pa_turned_on'),
        ]
    )
    def test_pa_charge_note(self, session, service, is_pa_turned_on):
        pa = create_personal_account(session, service_id=service.id)
        pa.enqueue('PROCESS_PAYMENTS')
        export_obj = pa.exports['PROCESS_PAYMENTS']

        order = create_order(pa.client, service_id=service.id)
        if is_pa_turned_on:
            pa.create_receipt(100)

        charge_note = create_invoice(
            session,
            paysys_id=PAYSYS_ID_CARD,
            contract=pa.contract,
            orders=[(order, 100)],
            requested_type='charge_note'
        )
        payment = create_payment(charge_note)
        payment.receipt_sum = 100
        session.flush()

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=0,
                consume_sum=0
            )
        )
        hamcrest.assert_that(
            charge_note,
            hamcrest.has_properties(
                receipt_sum=0,
                consume_sum=0
            )
        )

    @pytest.mark.parametrize(
        'is_pa_turned_on',
        [
            pytest.param(False, id='pa_not_turned_on'),
            pytest.param(True, id='pa_turned_on'),
        ]
    )
    def test_pa_charge_note_cpf(self, session, service, is_pa_turned_on):
        pa = create_personal_account(session, service_id=service.id)
        pa.enqueue('PROCESS_PAYMENTS')
        export_obj = pa.exports['PROCESS_PAYMENTS']

        order = create_order(pa.client, service_id=service.id)
        if is_pa_turned_on:
            pa.create_receipt(100)

        charge_note = create_invoice(
            session,
            paysys_id=PAYSYS_ID_CARD,
            contract=pa.contract,
            orders=[(order, 100)],
            requested_type='charge_note'
        )
        payment = create_payment(charge_note)
        payment.receipt_sum = 100
        create_payment_ocpf(payment, 100, pa)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=100,
                consume_sum=0,
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum=100,
                receipt_sum_1c=100
            )
        )
        hamcrest.assert_that(
            charge_note,
            hamcrest.has_properties(
                receipt_sum=0,
                consume_sum=0
            )
        )


@pytest.mark.invoice_refunds
class TestHandleRefunds(object):
    @staticmethod
    def _create_bank_invoice(client, order):
        invoice = create_invoice(client.session, paysys_id=PAYSYS_ID_BANK, client=client, orders=[(order, 100)])
        cpf = create_ocpf(invoice, 100)
        # внутри create_ocpf триггер bo.tr_oebs_cp_fact_change
        # вызвал enqueue в очередь PROCESS_PAYMENTS.
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        invoice.create_receipt(100)
        return invoice, export_obj, cpf

    @staticmethod
    def _create_instant_invoice(client, order):
        invoice = create_invoice(client.session, paysys_id=PAYSYS_ID_CARD, client=client, orders=[(order, 100)])
        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        payment = create_payment(invoice)
        payment.receipt_sum = 100
        invoice.create_receipt(100)
        cpf = create_payment_ocpf(payment)
        return invoice, export_obj, cpf, payment

    @staticmethod
    def _create_refund(invoice, cpf, amount):
        refund = create_refund(invoice, amount, InvoiceRefundStatus.exported, cpf)
        refund.payload = {
            'entity_id': uuid.uuid4().int,
            'payment_num': uuid.uuid4().hex,
            'payment_date': ON_DT.strftime('%d.%m.%Y')
        }
        invoice.session.flush()
        return refund

    @staticmethod
    def _create_refund_cpf(refund):
        ref_cpf = create_ocpf(refund.invoice, -refund.amount)
        ref_cpf.entity_id = refund.id
        ref_cpf.payment_number = refund.payload['payment_num']
        ref_cpf.payment_date = ON_DT
        refund.session.flush()
        return ref_cpf

    def test_bank(self, session, client, order):
        invoice, export_obj, cpf = self._create_bank_invoice(client, order)

        refunds = []
        for ref_sum in [20, 25]:
            refund = self._create_refund(invoice, cpf, ref_sum)
            self._create_refund_cpf(refund)
            refunds.append(refund)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            refunds,
            hamcrest.only_contains(
                hamcrest.has_properties(
                    status_code=InvoiceRefundStatus.successful,
                )
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=55,
                receipt_sum=55,
                confirmed_amount=55,
                consume_sum=0,
            )
        )

    def test_bank_partial(self, session, client, order):
        invoice, export_obj, cpf = self._create_bank_invoice(client, order)

        refund_ok = self._create_refund(invoice, cpf, 34)
        self._create_refund_cpf(refund_ok)
        refund_wait = self._create_refund(invoice, cpf, 32)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            refund_ok,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.successful,
            )
        )
        hamcrest.assert_that(
            refund_wait,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.exported,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=66,
                receipt_sum=66,
                confirmed_amount=66,
                locked_sum=32,
                consume_sum=0,
            )
        )

    def test_instant(self, session, client, order):
        invoice, export_obj, cpf, payment = self._create_instant_invoice(client, order)

        refunds = []
        for ref_sum in [15, 20]:
            refund = self._create_refund(invoice, cpf, ref_sum)
            self._create_refund_cpf(refund)
            refunds.append(refund)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            refunds,
            hamcrest.only_contains(
                hamcrest.has_properties(
                    status_code=InvoiceRefundStatus.successful,
                )
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum_1c=100,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=65,
                receipt_sum=65,
                confirmed_amount=65,
                consume_sum=0,
            )
        )

    def test_instant_partial(self, session, client, order):
        invoice, export_obj, cpf, payment = self._create_instant_invoice(client, order)

        refund_ok = self._create_refund(invoice, cpf, 34)
        self._create_refund_cpf(refund_ok)
        refund_wait = self._create_refund(invoice, cpf, 32)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            refund_ok,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.successful,
            )
        )
        hamcrest.assert_that(
            refund_wait,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.exported,
            )
        )
        hamcrest.assert_that(
            payment,
            hamcrest.has_properties(
                receipt_sum_1c=100,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=66,
                receipt_sum=66,
                confirmed_amount=66,
                locked_sum=32,
                consume_sum=0,
            )
        )

    def test_reconciled(self, session, client, order):
        invoice, export_obj, cpf = self._create_bank_invoice(client, order)

        refund = self._create_refund(invoice, cpf, 10)
        refund.set_status(InvoiceRefundStatus.oebs_reconciled)
        self._create_refund_cpf(refund)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.successful_reconciled,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=90,
                receipt_sum=90,
                confirmed_amount=90,
                consume_sum=0,
            )
        )

    def test_bank_waiting_other_cpf(self, session, client, order):
        invoice, export_obj, cpf = self._create_bank_invoice(client, order)

        refund_ok = self._create_refund(invoice, cpf, 30)
        create_ocpf(invoice, -30)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            refund_ok,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.exported,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=70,
                receipt_sum=70,
                confirmed_amount=70,
                locked_sum=30,
                consume_sum=0,
            )
        )

    def test_instant_waiting_other_cpf(self, session, client, order):
        invoice, export_obj, cpf, payment = self._create_instant_invoice(client, order)

        refund_ok = self._create_refund(invoice, cpf, 30)
        create_ocpf(invoice, -30)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            refund_ok,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.exported,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=70,
                receipt_sum=100,
                confirmed_amount=70,
                locked_sum=30,
                consume_sum=0,
            )
        )

    @pytest.fixture()
    def duplicate_cpfs_export(self, session, client, order):
        invoice, export_obj, cpf = self._create_bank_invoice(client, order)

        refund = self._create_refund(invoice, cpf, 30)
        self._create_refund_cpf(refund)
        self._create_refund_cpf(refund)
        return export_obj

    def test_duplicate_cpfs_processing(
        self, session, duplicate_cpfs_export,
    ):
        handler = QueueProcessor('PROCESS_PAYMENTS').get_processor(duplicate_cpfs_export)

        with pytest.raises(INVALID_PARAM) as excinfo:
            handler(duplicate_cpfs_export)

        assert "multiple cash_payment_facts with key" in excinfo.value.msg

    @pytest.mark.parametrize(
        'patched_queue_processor',
        [
            QueueProcessorPatch(
                queue_name='PROCESS_PAYMENTS',
                class_name='Invoice',
                new_handler=handle_invoice_invalid_param,
            ),
        ],
        indirect=['patched_queue_processor']
    )
    def test_duplicate_cpfs_failure(
        self, session, duplicate_cpfs_export, patched_queue_processor
    ):

        patched_queue_processor.process_one(duplicate_cpfs_export)
        session.flush()

        assert duplicate_cpfs_export.state == 0
        assert duplicate_cpfs_export.rate == 1
        assert "multiple cash_payment_facts with key" in duplicate_cpfs_export.error

    @pytest.mark.parametrize(
        'refund_status, req_locked_sum',
        [
            (InvoiceRefundStatus.export_failed, 10),
            (InvoiceRefundStatus.failed, 10),
            (InvoiceRefundStatus.failed_unlocked, 0),
        ]
    )
    def test_prev_refund_failed(self, session, client, order, refund_status, req_locked_sum):
        invoice = create_invoice(client.session, paysys_id=PAYSYS_ID_BANK, client=client, orders=[(order, 100)])
        cpf = create_ocpf(invoice, 100)
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        invoice.create_receipt(100)
        finished_refund = self._create_refund(invoice, cpf, 10)
        finished_refund.set_status(refund_status)

        refund = self._create_refund(invoice, cpf, 20)
        self._create_refund_cpf(refund)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)
        session.flush()

        assert export_obj.state == 1
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.successful,
            )
        )
        hamcrest.assert_that(
            finished_refund,
            hamcrest.has_properties(
                status_code=refund_status,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=80,
                receipt_sum=80,
                confirmed_amount=80,
                locked_sum=req_locked_sum,
                consume_sum=0,
            )
        )

    def test_prev_refund_ok(self, session, client, order):
        invoice = create_invoice(client.session, paysys_id=PAYSYS_ID_BANK, client=client, orders=[(order, 100)])
        cpf = create_ocpf(invoice, 100)
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        finished_refund = self._create_refund(invoice, cpf, 10)
        finished_refund.set_status(InvoiceRefundStatus.successful)
        self._create_refund_cpf(finished_refund)
        invoice.create_receipt(90)

        refund = self._create_refund(invoice, cpf, 20)
        self._create_refund_cpf(refund)

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)
        session.flush()

        assert export_obj.state == 1
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.successful,
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum_1c=70,
                receipt_sum=70,
                confirmed_amount=70,
                locked_sum=0,
                consume_sum=0,
            )
        )


@pytest.mark.invoice_transfer
class TestHandleInvoiceTransfer(object):
    @staticmethod
    def _create_invoices(
        client,
        order,
        src_invoice_paysys_id=PAYSYS_ID_BANK,
        dst_invoice_paysys_id=PAYSYS_ID_BANK,
    ):
        src_invoice = create_invoice(
            client.session,
            paysys_id=src_invoice_paysys_id,
            client=client,
            orders=[(order, 100)]
        )

        if src_invoice_paysys_id == PAYSYS_ID_CARD:
            src_invoice.enqueue('PROCESS_PAYMENTS')
            payment = create_payment(src_invoice)
            payment.receipt_sum = 100
            cpf = create_payment_ocpf(payment)
        elif src_invoice_paysys_id == PAYSYS_ID_BANK:
            cpf = create_ocpf(src_invoice, 100)
            # внутри create_ocpf триггер bo.tr_oebs_cp_fact_change
            # вызвал enqueue в очередь PROCESS_PAYMENTS.

        src_invoice.create_receipt(100)

        dst_invoice = create_invoice(
            client.session,
            paysys_id=dst_invoice_paysys_id,
            client=client,
            person=src_invoice.person,
            orders=[(order, 55)])
        return src_invoice, dst_invoice, cpf

    @staticmethod
    def _create_invoice_transfer(session, src_invoice, dst_invoice, amount, status=InvoiceTransferStatus.exported):
        return create_invoice_transfer(session, src_invoice, dst_invoice, amount, status)

    @staticmethod
    def _create_cpf_from(invoice_transfer, amount=None):
        if amount is None:
            amount = -invoice_transfer.amount
        create_ocpf(
            invoice_transfer.src_invoice,
            amount=-amount,
            operation_type=OebsOperationType.ACTIVITY,
            orig_id=invoice_transfer.id,
        )
        # внутри create_ocpf триггер bo.tr_oebs_cp_fact_change
        # вызвал enqueue в очередь PROCESS_PAYMENTS.

    @staticmethod
    def _create_cpf_to(invoice_transfer, amount=None):
        if amount is None:
            amount = invoice_transfer.amount
        create_ocpf(
            invoice_transfer.dst_invoice,
            amount=amount,
            operation_type=OebsOperationType.ACTIVITY,
            orig_id=invoice_transfer.id,
        )
        # внутри create_ocpf триггер bo.tr_oebs_cp_fact_change
        # вызвал enqueue в очередь PROCESS_PAYMENTS.

    @pytest.mark.parametrize(
        'paysys_id, payment_method_id',
        [
            (PAYSYS_ID_CARD, PaymentMethodIDs.credit_card),
            (PAYSYS_ID_BANK, PaymentMethodIDs.bank)
        ]
    )
    def test_from(self, session, client, order, paysys_id, payment_method_id):
        src_invoice, dst_invoice, _ = self._create_invoices(client, order, src_invoice_paysys_id=paysys_id)
        src_invoice.transfer(order, TransferMode.src, 23)
        invoice_transfer = self._create_invoice_transfer(session, src_invoice, dst_invoice, 55)
        self._create_cpf_from(invoice_transfer, 45)
        self._create_cpf_from(invoice_transfer, 10)
        export_obj = src_invoice.exports['PROCESS_PAYMENTS']

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                    status_code=InvoiceTransferStatus.in_progress,
                )
        )
        hamcrest.assert_that(
            src_invoice,
            hamcrest.has_properties(
                receipt_sum_1c=45,
                receipt_sum=45,
                confirmed_amount=45,
                consume_sum=23,
                payment_method_id=payment_method_id,
                paysys_id=paysys_id,
            )
        )

    @pytest.mark.parametrize(
        'paysys_id, payment_method_id',
        [
            (PAYSYS_ID_CARD, PaymentMethodIDs.credit_card),
            (PAYSYS_ID_BANK, PaymentMethodIDs.bank)
        ]
    )
    def test_to(self, session, client, order, paysys_id, payment_method_id):
        src_invoice, dst_invoice, _ = self._create_invoices(client, order, dst_invoice_paysys_id=paysys_id)
        invoice_transfer = self._create_invoice_transfer(
            session, src_invoice, dst_invoice, 55, status=InvoiceTransferStatus.in_progress
        )
        self._create_cpf_to(invoice_transfer, 35)
        self._create_cpf_to(invoice_transfer, 20)
        export_obj = dst_invoice.exports['PROCESS_PAYMENTS']

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                status_code=InvoiceTransferStatus.successful,
            )
        )
        hamcrest.assert_that(
            dst_invoice,
            hamcrest.has_properties(
                receipt_sum_1c=55,
                receipt_sum=55,
                confirmed_amount=55,
                consume_sum=55,
                payment_method_id=payment_method_id,
                paysys_id=paysys_id,
            )
        )

    @pytest.mark.parametrize(
        'status',
        [
            InvoiceTransferStatus.exported,
            InvoiceTransferStatus.not_exported
        ]
    )
    def test_to_deferred(self, session, client, order, status):
        src_invoice, dst_invoice, _ = self._create_invoices(client, order)
        invoice_transfer = self._create_invoice_transfer(session, src_invoice, dst_invoice, 35, status)
        self._create_cpf_to(invoice_transfer, 35)
        export_obj = dst_invoice.exports['PROCESS_PAYMENTS']

        handler = QueueProcessor('PROCESS_PAYMENTS').get_processor(export_obj)

        with pytest.raises(DEFER_INVOICE_TRANSFER_ERROR):
            handler(export_obj)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                status_code=status,
            )
        )
        hamcrest.assert_that(
            dst_invoice,
            hamcrest.has_properties(
                receipt_sum_1c=35,
                receipt_sum=0,
                consume_sum=0,
                payment_method_id=PaymentMethodIDs.bank,
                paysys_id=PAYSYS_ID_BANK,
            )
        )

    def test_from_deferred(self, session, client, order):
        src_invoice, dst_invoice, _ = self._create_invoices(client, order)
        invoice_transfer = self._create_invoice_transfer(session, src_invoice, dst_invoice, 35, InvoiceTransferStatus.not_exported)
        self._create_cpf_from(invoice_transfer, 35)
        export_obj = src_invoice.exports['PROCESS_PAYMENTS']

        handler = QueueProcessor('PROCESS_PAYMENTS').get_processor(export_obj)

        with pytest.raises(DEFER_INVOICE_TRANSFER_ERROR_NOT_EXPORTED):
            handler(export_obj)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                status_code=InvoiceTransferStatus.not_exported,
            )
        )

    def test_from_fail_sum(self, session, client, order):
        src_invoice, dst_invoice, _ = self._create_invoices(client, order)
        invoice_transfer = self._create_invoice_transfer(session, src_invoice, dst_invoice, 70)
        self._create_cpf_from(invoice_transfer, 45)
        self._create_cpf_from(invoice_transfer, 10)
        export_obj = src_invoice.exports['PROCESS_PAYMENTS']

        handler = QueueProcessor('PROCESS_PAYMENTS').get_processor(export_obj)

        with pytest.raises(INVOICE_TRANSFER_SUM_ERROR):
            handler(export_obj)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                status_code=InvoiceTransferStatus.exported,
            )
        )
        hamcrest.assert_that(
            src_invoice,
            hamcrest.has_properties(
                receipt_sum_1c=45,
                receipt_sum=100,
                consume_sum=0,
                payment_method_id=PaymentMethodIDs.bank,
                paysys_id=PAYSYS_ID_BANK,
            )
        )

    def test_to_fail_sum(self, session, client, order):
        src_invoice, dst_invoice, _ = self._create_invoices(client, order)
        invoice_transfer = self._create_invoice_transfer(
            session, src_invoice, dst_invoice, 70, status=InvoiceTransferStatus.in_progress
        )
        self._create_cpf_to(invoice_transfer, 35)
        self._create_cpf_to(invoice_transfer, 20)
        export_obj = dst_invoice.exports['PROCESS_PAYMENTS']

        handler = QueueProcessor('PROCESS_PAYMENTS').get_processor(export_obj)

        with pytest.raises(INVOICE_TRANSFER_SUM_ERROR):
            handler(export_obj)

        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                status_code=InvoiceTransferStatus.in_progress,
            )
        )
        hamcrest.assert_that(
            dst_invoice,
            hamcrest.has_properties(
                receipt_sum_1c=55,
                receipt_sum=0,
                consume_sum=0,
                payment_method_id=PaymentMethodIDs.bank,
                paysys_id=PAYSYS_ID_BANK,
            )
        )

    def test_invoice_transfer_and_refund(self, session, client, order):
        src_invoice, dst_invoice, cpf = self._create_invoices(client, order)
        invoice_transfer = self._create_invoice_transfer(session, src_invoice, dst_invoice, 55)
        self._create_cpf_from(invoice_transfer, 45)
        self._create_cpf_from(invoice_transfer, 10)
        export_obj = src_invoice.exports['PROCESS_PAYMENTS']

        refund = create_refund(src_invoice, 17, InvoiceRefundStatus.exported, cpf)
        refund.payload = {
            'entity_id': uuid.uuid4().int,
            'payment_num': uuid.uuid4().hex,
            'payment_date': ON_DT.strftime('%d.%m.%Y')
        }
        src_invoice.session.flush()
        ref_cpf = create_ocpf(refund.invoice, -refund.amount)
        ref_cpf.entity_id = refund.id
        ref_cpf.payment_number = refund.payload['payment_num']
        ref_cpf.payment_date = ON_DT
        refund.session.flush()

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == 1
        hamcrest.assert_that(
            invoice_transfer,
            hamcrest.has_properties(
                    status_code=InvoiceTransferStatus.in_progress,
                )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                status_code=InvoiceRefundStatus.successful,
            )
        )
        hamcrest.assert_that(
            src_invoice,
            hamcrest.has_properties(
                receipt_sum_1c=28,
                receipt_sum=28,
                confirmed_amount=28,
                consume_sum=0,
            )
        )


@pytest.mark.single_account
@pytest.mark.charge_note_register
class TestSingleAccountNotifyAboutFreeFunds(object):
    @pytest.fixture(params=[False, True], ids=['charge_note', 'charge_note_register'])
    def charge_note_type(self, request, session):
        session.config.__dict__['FORCE_CHARGE_NOTE_REGISTER_FOR_SINGLE_ACCOUNT'] = request.param

    @pytest.fixture(autouse=True)
    def enable_single_account_for_all_services(self, session):
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True

    @pytest.fixture(name='personal_account')
    def create_personal_account(self, session):
        person_type = list(single_account.availability.ALLOWED_LEGAL_ENTITY_PERSON_CATEGORIES)[0]
        client = ob.ClientBuilder(with_single_account=True).build(session).obj
        person = ob.PersonBuilder(
            client=client, name='Sponge Bob',
            email='s.bob@nickelodeon.com',
            type=person_type
        ).build(session).obj
        single_account.prepare.process_client(client)
        personal_account = PersonalAccountManager(session).\
            for_person(person).\
            for_single_account(client.single_account_number).\
            get(auto_create=False)
        return personal_account

    @staticmethod
    def patch_notify_about_free_funds():
        notify_about_free_funds_path = \
            'balance.actions.invoice_turnon.single_account.notifications.notify_about_free_funds'
        return mock.patch(notify_about_free_funds_path)

    @staticmethod
    def check_notify_about_free_funds_call(notify_about_free_funds_mock, expected_free_funds_delta):
        notify_about_free_funds_mock.assert_called_once()
        free_funds_delta = notify_about_free_funds_mock.call_args[0][2]
        assert free_funds_delta == expected_free_funds_delta

    @pytest.mark.usefixtures('charge_note_type')
    def test_charge_note_overpayment(self, session, personal_account):
        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(client=personal_account.client)
        ).build(session).obj
        charge_note, = core.Core(session).create_invoice(
            request_id=request.id,
            paysys_id=PAYSYS_ID_BANK_UR,
            person_id=personal_account.person_id
        )

        payment, = charge_note.payments
        overpayment = 100
        create_payment_ocpf(payment, charge_note.total_sum + overpayment, personal_account)

        with self.patch_notify_about_free_funds() as notify_about_free_funds_mock:
            process_payments.handle_invoice(personal_account)

        self.check_notify_about_free_funds_call(notify_about_free_funds_mock, overpayment)

    def test_charge_note_register(self, session, personal_account):
        order = ob.OrderBuilder.construct(
            session,
            client=personal_account.client,
            product_id=DIRECT_PRODUCT_RUB_ID
        )
        invoice = create_invoice(session, person=personal_account.person, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            PAYSYS_ID_BANK_UR,
            person=personal_account.person,
            orders=[(order, 100)],
            invoices=[invoice],
            single_account_number=personal_account.single_account_number
        )
        payment, = charge_note.payments

        create_payment_ocpf(payment, 120, personal_account)
        create_payment_ocpf(payment, 200, invoice)

        with self.patch_notify_about_free_funds() as notify_about_free_funds_mock:
            process_payments.handle_invoice(personal_account)

        self.check_notify_about_free_funds_call(notify_about_free_funds_mock, 20)

    def test_payment_without_orig_id(self, session, personal_account):
        overpayment = 100
        create_ocpf(personal_account, overpayment)

        with self.patch_notify_about_free_funds() as notify_about_free_funds_mock:
            process_payments.handle_invoice(personal_account)

        self.check_notify_about_free_funds_call(notify_about_free_funds_mock, overpayment)

    def test_successful_termination_on_notify_error(self, session, personal_account):
        overpayment = 100
        create_ocpf(personal_account, overpayment)

        with self.patch_notify_about_free_funds() as notify_about_free_funds_mock:
            notify_about_free_funds_mock.side_effect = RuntimeError('Mr. Krabs is angry !')
            with session.begin_nested():
                process_payments.handle_invoice(personal_account)

        notify_about_free_funds_mock.assert_called_once()

        # Убеждаемся, что основная транзакция успешно завершилась
        session.refresh(personal_account)
        assert personal_account.unused_funds == overpayment


@pytest.mark.charge_note_register
class TestChargeNoteRegisterBank(object):
    def test_pa_bank_empty(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, paysys_id=PAYSYS_ID_BANK_UR, contract=pa.contract, orders=[(order, 200)])

        create_charge_note_register(
            PAYSYS_ID_BANK_UR,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )

        pa.enqueue('PROCESS_PAYMENTS')
        export_obj = pa.exports['PROCESS_PAYMENTS']
        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == ExportState.exported
        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=0,
                consume_sum=0
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=0,
                consume_sum=0
            )
        )

    @pytest.mark.parametrize(
        'invoice_paysys_id, '
        'process_pa, '
        'invoice_sum, '
        'pa_sum, '
        'invoice_payment, '
        'pa_payment, '
        'invoice_consume_sum, '
        'pa_consume_sum',
        [
            pytest.param(PAYSYS_ID_BANK_UR, True, 200, 100, 200, 100, 0, 100, id='pa_full_bank'),
            pytest.param(PAYSYS_ID_BANK_UR, True, 200, 100, 0, 100, 0, 100, id='pa_partial_pa_bank'),
            pytest.param(PAYSYS_ID_BANK_UR, True, 200, 100, 200, 0, 0, 0, id='pa_partial_invoice_bank'),
            pytest.param(PAYSYS_ID_BANK_UR, False, 200, 100, 200, 100, 200, 100, id='invoice_full_bank'),
            pytest.param(PAYSYS_ID_BANK_UR, False, 200, 100, 0, 100, 0, 100, id='invoice_partial_pa_bank'),
            pytest.param(PAYSYS_ID_BANK_UR, False, 200, 100, 200, 0, 200, 0, id='invoice_partial_invoice_bank'),
            pytest.param(PAYSYS_ID_CARD_UR, True, 200, 100, 200, 100, 0, 100, id='pa_full_card'),
            pytest.param(PAYSYS_ID_CARD_UR, True, 200, 100, 0, 100, 0, 100, id='pa_partial_pa_card'),
            pytest.param(PAYSYS_ID_CARD_UR, True, 200, 100, 200, 0, 0, 0, id='pa_partial_invoice_card'),
            pytest.param(PAYSYS_ID_CARD_UR, False, 200, 100, 200, 100, 0, 100, id='invoice_full_card'),
            pytest.param(PAYSYS_ID_CARD_UR, False, 200, 100, 0, 100, 0, 100, id='invoice_partial_pa_card'),
            pytest.param(PAYSYS_ID_CARD_UR, False, 200, 100, 200, 0, 0, 0, id='invoice_partial_invoice_card'),

            pytest.param(PAYSYS_ID_BANK_UR, True, 200, 100, 600, 300, 0, 100, id='pa_overpay_bank'),
            pytest.param(PAYSYS_ID_BANK_UR, False, 200, 100, 600, 300, 200, 100, id='invoice_overpay_bank'),
            pytest.param(PAYSYS_ID_CARD_UR, True, 200, 100, 600, 300, 0, 100, id='pa_overpay_card'),
            pytest.param(PAYSYS_ID_CARD_UR, False, 200, 100, 600, 300, 0, 100, id='invoice_overpay_card'),
        ]
    )
    def test_single(self,
                    session,
                    service,
                    process_pa,
                    invoice_paysys_id,
                    invoice_sum,
                    pa_sum,
                    invoice_payment,
                    pa_payment,
                    invoice_consume_sum,
                    pa_consume_sum):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(
            session,
            paysys_id=invoice_paysys_id,
            contract=pa.contract,
            orders=[(order, invoice_sum)]
        )

        charge_note = create_charge_note_register(
            PAYSYS_ID_BANK_UR,
            contract=pa.contract,
            orders=[(order, pa_sum)],
            invoices=[invoice]
        )
        payment, = charge_note.payments
        if pa_payment:
            create_payment_ocpf(payment, pa_payment, pa)
        if invoice_payment:
            create_payment_ocpf(payment, invoice_payment, invoice)

        if process_pa:
            pa.enqueue('PROCESS_PAYMENTS')
            export_obj = pa.exports['PROCESS_PAYMENTS']
        else:
            invoice.enqueue('PROCESS_PAYMENTS')
            export_obj = invoice.exports['PROCESS_PAYMENTS']

        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == ExportState.exported
        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=pa_payment,
                consume_sum=pa_consume_sum
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=invoice_payment,
                consume_sum=invoice_consume_sum
            )
        )

    def test_consecutive(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(
            session,
            paysys_id=PAYSYS_ID_BANK_UR,
            contract=pa.contract,
            orders=[(order, 200)]
        )
        pa.enqueue('PROCESS_PAYMENTS')
        invoice.enqueue('PROCESS_PAYMENTS')

        charge_note = create_charge_note_register(
            PAYSYS_ID_BANK_UR,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        payment, = charge_note.payments

        create_payment_ocpf(payment, 100, pa)
        QueueProcessor('PROCESS_PAYMENTS').process_one(pa.exports['PROCESS_PAYMENTS'])

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=0,
                consume_sum=0
            )
        )

        create_payment_ocpf(payment, 200, invoice)
        QueueProcessor('PROCESS_PAYMENTS').process_one(invoice.exports['PROCESS_PAYMENTS'])

        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=200,
                consume_sum=200
            )
        )

        hamcrest.assert_that(
            charge_note.register_rows,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    ref_invoice=pa,
                    receipt_sum=100,
                    receipt_sum_1c=100,
                ),
                hamcrest.has_properties(
                    ref_invoice=invoice,
                    receipt_sum=200,
                    receipt_sum_1c=200,
                )
            )
        )


@pytest.mark.charge_note_register
class TestChargeNoteRegisterInstant(object):
    def test_pa_wo_cpf(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, paysys_id=PAYSYS_ID_BANK_UR, contract=pa.contract, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            PAYSYS_ID_CARD_UR,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        payment = create_payment(charge_note)
        payment.turn_on()

        pa.enqueue('PROCESS_PAYMENTS')
        export_obj = pa.exports['PROCESS_PAYMENTS']
        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == ExportState.exported
        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=200,
                consume_sum=0
            )
        )

    def test_pa_w_cpf(self, session, service):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, paysys_id=PAYSYS_ID_BANK_UR, contract=pa.contract, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            PAYSYS_ID_CARD_UR,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        payment = create_payment(charge_note)
        payment.turn_on()
        create_payment_ocpf(payment, 100, pa)
        create_payment_ocpf(payment, 200, invoice)

        pa.enqueue('PROCESS_PAYMENTS')
        export_obj = pa.exports['PROCESS_PAYMENTS']
        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == ExportState.exported
        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=200,
                consume_sum=0
            )
        )

    @pytest.mark.parametrize(
        'paysys_id, invoice_turnon',
        [
            pytest.param(PAYSYS_ID_BANK_UR, False, id='bank'),
            pytest.param(PAYSYS_ID_CARD_UR, True, id='card'),
        ]
    )
    def test_invoice_wo_cpf(self, session, service, paysys_id, invoice_turnon):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, paysys_id=paysys_id, contract=pa.contract, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            PAYSYS_ID_CARD_UR,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        payment = create_payment(charge_note)
        payment.turn_on()

        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == ExportState.exported
        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100
            )
        )
        # предоплатные счета с банковским способом оплаты включаются строго по receipt_sum_1c
        # это не так критично, учитывая что на практике предоплатные счета в реестры попадать не должны
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=200,
                consume_sum=200 if invoice_turnon else 0
            )
        )

    @pytest.mark.parametrize(
        'paysys_id',
        [
            pytest.param(PAYSYS_ID_BANK_UR, id='bank'),
            pytest.param(PAYSYS_ID_CARD_UR, id='card'),
        ]
    )
    def test_invoice_w_cpf(self, session, service, paysys_id):
        pa = create_personal_account(session, service_id=service.id)
        order = create_order(pa.client, service.id)
        invoice = create_invoice(session, paysys_id=paysys_id, contract=pa.contract, orders=[(order, 200)])

        charge_note = create_charge_note_register(
            PAYSYS_ID_CARD_UR,
            contract=pa.contract,
            orders=[(order, 100)],
            invoices=[invoice]
        )
        payment = create_payment(charge_note)
        payment.turn_on()
        create_payment_ocpf(payment, 100, pa)
        create_payment_ocpf(payment, 200, invoice)

        invoice.enqueue('PROCESS_PAYMENTS')
        export_obj = invoice.exports['PROCESS_PAYMENTS']
        QueueProcessor('PROCESS_PAYMENTS').process_one(export_obj)

        assert export_obj.state == ExportState.exported
        hamcrest.assert_that(
            pa,
            hamcrest.has_properties(
                receipt_sum=100,
                consume_sum=100
            )
        )
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                receipt_sum=200,
                consume_sum=200
            )
        )
