import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.user.create import CreateUserAction
from pay.bill_payments.bill_payments.core.actions.user.ensure import EnsureUserAction


@pytest.mark.asyncio
async def test_returns_created_user(storage, mock_create_user, created_user):
    returned = await EnsureUserAction(uid=777000).run()

    assert_that(returned, equal_to(created_user))


@pytest.mark.asyncio
async def test_calls_create_user(storage, mock_create_user):
    await EnsureUserAction(uid=777000).run()

    mock_create_user.assert_called_once_with(uid=777000)


@pytest.mark.asyncio
async def test_when_already_exists__returns_existing_user(storage):
    created = await CreateUserAction(uid=777000).run()

    returned = await EnsureUserAction(uid=777000).run()

    assert_that(returned, equal_to(created))


@pytest.fixture
def created_user(mocker):
    return mocker.Mock()


@pytest.fixture
def mock_create_user(mock_action, created_user):
    return mock_action(CreateUserAction, created_user)
