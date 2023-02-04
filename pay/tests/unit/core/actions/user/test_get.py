import pytest

from pay.bill_payments.bill_payments.core.actions.user.get import GetUserStateAction, UserState
from pay.bill_payments.bill_payments.storage.entities.user import User


@pytest.mark.asyncio
async def test_should_return_unregistered_state_if_user_not_exists():
    user_state = await GetUserStateAction(uid=322).run()

    assert user_state == UserState.UNREGISTERED


@pytest.mark.asyncio
async def test_should_return_syncing_state_if_revision_less(storage):
    await storage.user.create(User(uid=12, revision=1, synced_revision=0))

    user_state = await GetUserStateAction(uid=12).run()

    assert user_state == UserState.SYNCING


@pytest.mark.asyncio
async def test_should_return_completed_state_if_revision_equal(storage):
    await storage.user.create(User(uid=12, revision=1, synced_revision=1))

    user_state = await GetUserStateAction(uid=12).run()

    assert user_state == UserState.COMPLETED
