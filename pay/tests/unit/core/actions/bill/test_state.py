from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains_inanyorder, match_equality, not_none

from pay.bill_payments.bill_payments.core.actions.bill.state import GetBillsStateAction
from pay.bill_payments.bill_payments.core.actions.user.get import GetUserStateAction, UserState
from pay.bill_payments.bill_payments.storage import BillMapper
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus


def get_random_bill(user, document):
    return Bill(
        uid=user.uid,
        document_id=document.document_id,
        supplier_bill_id=str(uuid4()),
        status=BillStatus.NEW,
        amount=100,
        amount_to_pay=20,
        bill_date=utcnow(),
    )


class TestBillStateAction:
    @pytest.fixture
    def user_state(self):
        return UserState.COMPLETED

    @pytest.fixture
    def mock_get_user_state(self, mock_action, user_state):
        return mock_action(GetUserStateAction, user_state)

    @pytest.mark.asyncio
    async def test_should_fill_result_with_user_state(self, mock_get_user_state, user_state, user):
        bills_state = await GetBillsStateAction(uid=user.uid).run()

        assert bills_state.state == user_state
        mock_get_user_state.assert_called_once_with(uid=user.uid)

    @pytest.mark.asyncio
    async def test_should_return_bills(self, storage, user, document):
        bills = [await storage.bill.create(get_random_bill(user, document)) for _ in range(4)]

        bills_state = await GetBillsStateAction(uid=user.uid).run()

        assert_that(bills_state.bills, contains_inanyorder(*bills))

    @pytest.mark.asyncio
    async def test_should_call_storage_with_expected_args(self, mocker):
        storage_spy = mocker.spy(BillMapper, 'find_latest_unpaid_by_uid')

        await GetBillsStateAction(uid=42).run()

        storage_spy.assert_awaited_once_with(match_equality(not_none()), uid=42, limit=200)
