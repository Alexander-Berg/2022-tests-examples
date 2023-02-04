from uuid import uuid4

import pytest

from pay.bill_payments.bill_payments.core.actions.bill.force_search import ForceSearchBillsAction
from pay.bill_payments.bill_payments.core.actions.bill.search import SearchFinesAction
from pay.bill_payments.bill_payments.core.exceptions import UserNotFoundError
from pay.bill_payments.bill_payments.storage.entities.user import User


@pytest.fixture(autouse=True)
def mock_schedule(mocker):
    return mocker.patch.object(SearchFinesAction, 'schedule')


@pytest.fixture
async def user(storage, randn, unique_rand):
    return await storage.user.create(
        User(uid=unique_rand(randn), subscription_id=str(uuid4()), revision=1, synced_revision=1)
    )


@pytest.mark.asyncio
async def test_scheduled(user, storage, mock_schedule):
    await ForceSearchBillsAction(uid=user.uid).run()

    mock_schedule.assert_called_once_with(storage, uid=user.uid)


@pytest.mark.asyncio
async def test_already_scheduled(storage, user, mock_schedule):
    user.synced_revision = 0
    user = await storage.user.save(user)

    await ForceSearchBillsAction(uid=user.uid).run()

    mock_schedule.assert_not_called()


@pytest.mark.asyncio
async def test_user_not_found(storage, randn, mock_schedule):
    with pytest.raises(UserNotFoundError):
        await ForceSearchBillsAction(uid=randn()).run()
