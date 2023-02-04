from datetime import date, datetime, timezone
from uuid import uuid4

import pytest

from sendr_utils import alist

from pay.bill_payments.bill_payments.core.actions.bill.create import (
    BillNotification,
    CreateBillFromNotificationAction,
    CreateBillsFromManyNotificationsAction,
)
from pay.bill_payments.bill_payments.interactions.kazna.entities import DepartmentType, DocumentCode, SinglePayerDoc
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus
from pay.bill_payments.bill_payments.utils.stats import bill_notification_failures


class TestCreateBillsFromNotificationActions:
    @pytest.fixture
    def document_id(self):
        return uuid4()

    @pytest.fixture
    def bill_notification(self, user):
        return {
            'supplier_bill_id': 'supplier_bill_id',
            'subscription_id': user.subscription_id,
            'payer_doc': SinglePayerDoc(code=DocumentCode.DRIVER_LICENSE, value='10'),
            'dep_type': DepartmentType.GIBDD,
            'bill_date': datetime(2020, 11, 25, 13, 30, 59, tzinfo=timezone.utc),
            'purpose': 'purpose',
            'kbk': 'kbk',
            'payee_name': 'payee_name',
            'amount': 10000,
            'payer_name': 'payer_name',
            'amount_to_pay': 5000,
            'discount_size': '50',
            'discount_date': date(2021, 11, 25),
        }

    @pytest.fixture
    def expected_bill(self, user, document_id):
        return Bill(
            uid=user.uid,
            supplier_bill_id='supplier_bill_id',
            document_id=document_id,
            status=BillStatus.NEW,
            amount=10000,
            amount_to_pay=5000,
            bill_date=datetime(2020, 11, 25, 13, 30, 59, tzinfo=timezone.utc),
            dep_type=DepartmentType.GIBDD,
            purpose='purpose',
            kbk='kbk',
            payee_name='payee_name',
            payer_name='payer_name',
            discount_size='50',
            discount_date=date(2021, 11, 25),
        )

    @pytest.fixture(autouse=True)
    async def ensure_doc(self, user, bill_notification, storage, document_id):
        await storage.document.create(
            Document(
                document_id=document_id,
                uid=user.uid,
                code=DocumentCode.DRIVER_LICENSE,
                value=bill_notification['payer_doc'].value,
            )
        )

    @pytest.mark.asyncio
    async def test_should_create_notification_task(self, bill_notification, storage, user):
        await CreateBillFromNotificationAction(**bill_notification).run()

        bill = (await storage.bill.find_by_uid(user.uid))[0]
        tasks = await alist(storage.task.find(filters={'action_name': 'notify_user_new_bill'}))
        task_kwargs = tasks[0].params['action_kwargs']

        assert len(tasks) == 1
        assert task_kwargs['bill_id'] == str(bill.bill_id)

    @pytest.mark.asyncio
    async def test_should_not_create_second_notification_task(self, bill_notification, storage):
        await CreateBillFromNotificationAction(**bill_notification).run()
        await CreateBillFromNotificationAction(**bill_notification).run()

        tasks = await alist(storage.task.find(filters={'action_name': 'notify_user_new_bill'}))

        assert len(tasks) == 1

    @pytest.mark.asyncio
    async def test_should_create_new_bill(self, bill_notification, storage, user, expected_bill):
        await CreateBillFromNotificationAction(**bill_notification).run()

        bill = (await storage.bill.find_by_uid(user.uid))[0]
        expected_bill.bill_id = bill.bill_id
        expected_bill.created = bill.created
        expected_bill.updated = bill.updated

        assert bill == expected_bill

    @pytest.mark.asyncio
    async def test_should_fill_amount_to_pay_if_not_passed(self, bill_notification, storage, user):
        bill_notification.pop('amount_to_pay')
        await CreateBillFromNotificationAction(**bill_notification).run()

        bill = (await storage.bill.find_by_uid(user.uid))[0]

        assert bill.amount_to_pay == 10000

    @pytest.mark.asyncio
    async def test_should_not_create_if_subscription_not_found(self, bill_notification, storage, user):
        metric_before = bill_notification_failures.get()
        bill_notification['subscription_id'] = 'not_exists'
        await CreateBillFromNotificationAction(**bill_notification).run()

        bills = await storage.bill.find_by_uid(user.uid)
        metric_after = bill_notification_failures.get()

        assert len(bills) == 0
        assert metric_after[0][1] - metric_before[0][1] == 1

    @pytest.mark.asyncio
    async def test_should_not_create_duplications(self, bill_notification, storage, user):
        await CreateBillFromNotificationAction(**bill_notification).run()
        await CreateBillFromNotificationAction(**bill_notification).run()

        bills = await storage.bill.find_by_uid(user.uid)

        assert len(bills) == 1

    @pytest.mark.asyncio
    async def test_should_not_create_if_no_document_match(self, bill_notification, storage, user):
        metric_before = bill_notification_failures.get()
        bill_notification['payer_doc'].value = '25'
        await CreateBillFromNotificationAction(**bill_notification).run()

        bills = await storage.bill.find_by_uid(user.uid)
        metric_after = bill_notification_failures.get()

        assert len(bills) == 0
        assert metric_after[0][1] - metric_before[0][1] == 1

    @pytest.mark.asyncio
    async def test_should_match_document_code(self, bill_notification, storage, user, expected_bill):
        bill_notification['payer_doc'].code = DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE
        await CreateBillFromNotificationAction(**bill_notification).run()

        bills = await storage.bill.find_by_uid(user.uid)

        assert len(bills) == 0

    @pytest.mark.asyncio
    async def test_should_call_with_expected_args_from_many_action(self, mock_action, bill_notification, user):
        action_mocker = mock_action(CreateBillFromNotificationAction, None)

        await CreateBillsFromManyNotificationsAction(notifications=[BillNotification(**bill_notification)]).run()

        action_mocker.assert_called_once_with(
            supplier_bill_id='supplier_bill_id',
            subscription_id=user.subscription_id,
            payer_doc=SinglePayerDoc(code=DocumentCode.DRIVER_LICENSE, value='10'),
            dep_type=DepartmentType.GIBDD,
            bill_date=datetime(2020, 11, 25, 13, 30, 59, tzinfo=timezone.utc),
            purpose='purpose',
            kbk='kbk',
            payee_name='payee_name',
            amount=10000,
            payer_name='payer_name',
            amount_to_pay=5000,
            discount_size='50',
            discount_date=date(2021, 11, 25),
        )

    def test_should_run_in_tran(self):
        assert CreateBillsFromManyNotificationsAction.transact
