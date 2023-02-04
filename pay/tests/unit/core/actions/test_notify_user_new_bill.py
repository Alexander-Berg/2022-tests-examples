from datetime import date, datetime
from uuid import uuid4

import pytest

from sendr_interactions.clients.sup.entities import Data, Notification, PushRequest, PushResponse

from hamcrest import match_equality, not_none

from pay.bill_payments.bill_payments.core.actions.notify_user_new_bill import NotifyUserNewBillAction
from pay.bill_payments.bill_payments.interactions.kazna.entities import DepartmentType, DocumentCode
from pay.bill_payments.bill_payments.interactions.sup import SUPClient
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus


@pytest.fixture
async def ensure_doc(user, bill_notification, storage):
    await storage.document.create(
        Document(
            document_id=uuid4(),
            uid=user.uid,
            code=DocumentCode.DRIVER_LICENSE,
            value='111111',
        )
    )


@pytest.fixture
async def bill(storage, user, document):
    return await storage.bill.create(
        Bill(
            uid=user.uid,
            supplier_bill_id='supplier_bill_id',
            document_id=document.document_id,
            status=BillStatus.NEW,
            amount=10000,
            amount_to_pay=5000,
            bill_date=datetime(2020, 11, 25, 13, 30, 59),
            dep_type=DepartmentType.GIBDD,
            purpose='purpose',
            kbk='kbk',
            payee_name='payee_name',
            payer_name='payer_name',
            discount_size='50',
            discount_date=date(2021, 11, 25),
        )
    )


@pytest.mark.asyncio
async def test_notify_user_new_bill_with_discount(mocker, bill, bill_payments_settings):
    sup_mock = mocker.patch.object(
        SUPClient,
        'send_push',
        mocker.AsyncMock(return_value=PushResponse(id='123', receiver=['456'], data={}, request_time=123)),
    )
    await NotifyUserNewBillAction(bill_id=bill.bill_id).run()

    sup_mock.assert_called_once_with(
        PushRequest(
            receiver=[f'uid:{bill.uid}'],
            project=bill_payments_settings.BILL_SUP_PROJECT,
            data=Data(
                push_id='bill_payments',
                transit_id=match_equality(not_none()),
                topic_push=bill_payments_settings.BILL_SUP_TOPIC_PUSH,
            ),
            notification=Notification(
                title='Штраф ГИБДД',
                body='У вас есть неоплаченный штраф на сумму 100.00 рублей. Его можно оплатить со скидкой 50% до 25.11.2021',
                link='ya-search-app-open://?uri=https%3A%2F%2Fpassport.yandex.ru%2Forder-history%2Fgibdd',
            ),
            schedule='now',
            ttl=3600,
        )
    )


@pytest.mark.asyncio
async def test_notify_user_new_bill_without_discount(storage, mocker, bill: Bill, bill_payments_settings):
    sup_mock = mocker.patch.object(
        SUPClient,
        'send_push',
        mocker.AsyncMock(return_value=PushResponse(id='123', receiver=['456'], data={}, request_time=123)),
    )
    bill.discount_date = None
    bill.discount_size = None
    bill.amount = 10001
    await storage.bill.save(bill)

    await NotifyUserNewBillAction(bill_id=bill.bill_id).run()

    sup_mock.assert_called_once_with(
        PushRequest(
            receiver=[f'uid:{bill.uid}'],
            project=bill_payments_settings.BILL_SUP_PROJECT,
            data=Data(
                push_id='bill_payments',
                transit_id=match_equality(not_none()),
                topic_push=bill_payments_settings.BILL_SUP_TOPIC_PUSH,
            ),
            notification=Notification(
                title='Штраф ГИБДД',
                body='У вас есть неоплаченный штраф на сумму 100.01 рублей. Его нужно оплатить до 03.02.2021',
                link='ya-search-app-open://?uri=https%3A%2F%2Fpassport.yandex.ru%2Forder-history%2Fgibdd',
            ),
            schedule='now',
            ttl=3600,
        )
    )
