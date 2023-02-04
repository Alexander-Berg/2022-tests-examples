import datetime
from uuid import uuid4

import pytest
from dateutil.tz import tzutc

from pay.bill_payments.bill_payments.core.actions.bill.create import (
    BillNotification,
    CreateBillsFromManyNotificationsAction,
)
from pay.bill_payments.bill_payments.interactions.kazna.entities import DepartmentType, DocumentCode, SinglePayerDoc
from pay.bill_payments.bill_payments.storage.entities.document import Document


@pytest.fixture
def uri():
    return '/webhooks/kazna/bill'


@pytest.fixture
def expected_response_body():
    return {
        'code': 200,
        'status': 'success',
        'data': [],
    }


@pytest.fixture
def bill_request_body(user):
    return [
        {
            'supplierBillID': 'supplier_bill_id',
            'subscribe': user.subscription_id,
            'payerDoc': {
                'code': '22',
                'value': '10',
            },
            'depType': 'gibdd',
            'billDate': '2021-10-23T00:00:00Z',
            'purpose': 'purpose',
            'kbk': 'kbk',
            'payeeName': 'payee_name',
            'amount': 10000,
            'payerName': 'payer_name',
            'amountToPay': 5000,
            'additionalDataDiscountSize': '50',
            'additionalDataDiscountDate': '2021-10-30',
        }
    ]


@pytest.fixture
def optional_bill_request_body(bill_request_body):
    bill = bill_request_body[0]
    bill.pop('amountToPay')
    bill.pop('payerName')
    bill.pop('additionalDataDiscountSize')
    bill.pop('additionalDataDiscountDate')
    return bill_request_body


@pytest.fixture
def document_id():
    return uuid4()


@pytest.fixture
async def create_doc(user, storage, document_id):
    await storage.document.create(
        Document(
            document_id=document_id,
            uid=user.uid,
            code=DocumentCode.DRIVER_LICENSE,
            value='10',
        )
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('create_doc')
async def test_should_work_and_create_bill(uri, bill_request_body, storage, user, app):
    resp = await app.post(uri, json=bill_request_body)

    bills = await storage.bill.find_by_uid(user.uid)

    assert resp.status == 200
    assert len(bills) == 1
    assert bills[0].supplier_bill_id == bill_request_body[0]['supplierBillID']


@pytest.mark.asyncio
async def test_should_work_with_whole_fields(app, user, uri, mock_action, bill_request_body, expected_response_body):
    action_mocker = mock_action(
        CreateBillsFromManyNotificationsAction,
        None,
    )

    resp = await app.post(uri, json=bill_request_body)

    assert resp.status == 200
    action_mocker.assert_called_once_with(
        notifications=[
            BillNotification(
                supplier_bill_id='supplier_bill_id',
                subscription_id=user.subscription_id,
                payer_doc=SinglePayerDoc(code=DocumentCode.DRIVER_LICENSE, value='10'),
                dep_type=DepartmentType.GIBDD,
                bill_date=datetime.datetime(2021, 10, 23, 0, 0, tzinfo=tzutc()),
                purpose='purpose',
                kbk='kbk',
                payee_name='payee_name',
                amount=10000,
                payer_name='payer_name',
                amount_to_pay=5000,
                discount_size='50',
                discount_date=datetime.date(2021, 10, 30),
            ),
        ],
    )


@pytest.mark.asyncio
async def test_should_work_with_optional_fields(
    app, uri, mock_action, optional_bill_request_body, expected_response_body, user
):
    action_mocker = mock_action(
        CreateBillsFromManyNotificationsAction,
        None,
    )

    resp = await app.post(uri, json=optional_bill_request_body)

    assert resp.status == 200
    action_mocker.assert_called_once_with(
        notifications=[
            BillNotification(
                supplier_bill_id='supplier_bill_id',
                subscription_id=user.subscription_id,
                payer_doc=SinglePayerDoc(code=DocumentCode.DRIVER_LICENSE, value='10'),
                dep_type=DepartmentType.GIBDD,
                bill_date=datetime.datetime(2021, 10, 23, 0, 0, tzinfo=tzutc()),
                purpose='purpose',
                kbk='kbk',
                payee_name='payee_name',
                amount=10000,
                payer_name=None,
                amount_to_pay=None,
                discount_size=None,
                discount_date=None,
            ),
        ],
    )
