from hashlib import md5
from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.conf import settings
from pay.bill_payments.bill_payments.core.actions.transaction.update_status import UpdateTransactionStatusAction
from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode, PaymentStatusCode
from pay.bill_payments.bill_payments.storage.entities.bill import Bill, BillStatus
from pay.bill_payments.bill_payments.storage.entities.bill_order import BillOrder
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.storage.entities.enums import TransactionStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order, OrderStatus
from pay.bill_payments.bill_payments.storage.entities.transaction import PayerData, Transaction


@pytest.fixture
async def document(user, storage):
    return await storage.document.create(
        Document(
            uid=user.uid,
            document_id=uuid4(),
            code=DocumentCode.DRIVER_LICENSE,
            value='123',
        )
    )


@pytest.fixture
async def order(storage, auth_user):
    return await storage.order.create(
        Order(
            uid=auth_user.uid,
            order_id=uuid4(),
            status=OrderStatus.NEW,
        )
    )


@pytest.fixture
async def bill(storage, auth_user, document):
    return await storage.bill.create(
        Bill(
            uid=auth_user.uid,
            document_id=document.document_id,
            supplier_bill_id=str(uuid4()),
            status=BillStatus.NEW,
            amount=100,
            amount_to_pay=20,
            bill_date=utcnow(),
        )
    )


@pytest.fixture
async def transaction(storage, order):
    return await storage.transaction.create(
        Transaction(
            transaction_id=uuid4(),
            order_id=order.order_id,
            status=TransactionStatus.NEW,
            amount=100,
            external_payment_id='123',
            payer_data=PayerData(payer_full_name='test'),
        )
    )


@pytest.fixture
async def bill_order(storage, order, bill):
    return await storage.bill_order.create(
        BillOrder(
            order_id=order.order_id,
            bill_id=bill.bill_id,
        )
    )


@pytest.fixture
def request_data(transaction):
    return {
        'paymentID': 17447,
        'orderID': str(transaction.transaction_id),
        'status': {'code': '20', 'name': 'auth'},
        'depType': 'gibdd',
        'kvit': True,
        'amount': 50000,
        'totalSum': 51150,
        'payType': 'card',
        'paymentParams': {'supplierBillID': '18810177200855150244'},
        'result': [
            {
                'DepartmentalInfo.DrawerStatus': '20',
                'DepartmentalInfo.CBC': '18811630020016000140',
                'DepartmentalInfo.OKATO': '86701000',
                'DepartmentalInfo.PaytReason': '0',
                'DepartmentalInfo.TaxPeriod': '0',
                'DepartmentalInfo.DocNo': '24;1111111111',
                'DepartmentalInfo.DocDate': '26.02.2021',
                'DepartmentalInfo.TaxPaytKind': '0',
                'PayerName': 'ООО НКО «МОБИ.ДЕНЬГИ-тест»//Иванов Иван Иванович//',
                'PayerInn': '0',
                'PayerKPP': '0',
                'PayeeName': 'Управление федерального казначейства',
                'PayeePersonalAcc': '03100643000000010200',
                'PayeeCorrespAcc': '40102810545370000068',
                'PayeeBic': '018142016',
                'PayeeInn': '1001041280',
                'PayeeKPP': '100101001',
                'Purpose': 'ШТРАФ ПО АДМИНИСТРАТИВНОМУ ПРАВОНАРУШЕНИЮ, № 18810886586956606898',
                'AccDocNo': '706',
            }
        ],
        'sign': md5(f'{settings.KAZNA_SALT}17447auth'.encode('utf-8')).hexdigest(),
    }


@pytest.mark.asyncio
async def test_bills_update_payment_status(mocker, storage, app, transaction, request_data, bill, bill_order, order):
    update_status_spy = mocker.spy(UpdateTransactionStatusAction, '__init__')
    resp = await app.post(
        '/webhooks/kazna/payment_status',
        json=request_data,
        raise_for_status=True,
    )
    json_body = await resp.json()

    assert_that(resp.status, equal_to(200))
    assert_that(
        json_body,
        equal_to({'status': 'success', 'data': {}, 'code': 200}),
    )
    update_status_spy.assert_called_once_with(
        mocker.ANY, transaction_id=str(transaction.transaction_id), status=PaymentStatusCode.AUTH
    )

    transaction_updated = await storage.transaction.get(transaction.transaction_id)
    bill_updated = await storage.bill.get(bill.bill_id)
    order_updated = await storage.order.get(order.order_id)

    assert transaction_updated.status == TransactionStatus.PAID
    assert bill_updated.status == BillStatus.PAID
    assert order_updated.status == OrderStatus.PAID


@pytest.mark.asyncio
async def test_bills_update_payment_status_bad_signature(app, request_data):
    request_data['sign'] = '123456'

    resp = await app.post(
        '/webhooks/kazna/payment_status',
        json=request_data,
    )
    json_body = await resp.json()

    assert_that(resp.status, equal_to(403))
    assert_that(
        json_body,
        equal_to({'data': {'message': 'Forbidden'}, 'status': 'fail', 'code': 403}),
    )
