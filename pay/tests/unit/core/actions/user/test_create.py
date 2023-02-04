import pytest

from hamcrest import assert_that, equal_to, match_equality, not_none

from pay.bill_payments.bill_payments.core.actions.user.create import CreateUserAction
from pay.bill_payments.bill_payments.core.exceptions import UserAlreadyExistsError
from pay.bill_payments.bill_payments.storage.entities.user import User


@pytest.mark.asyncio
async def test_creates():
    user = await CreateUserAction(uid=777000).run()

    assert_that(
        user,
        equal_to(
            User(
                uid=777000,
                subscription_id=match_equality(not_none()),
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
            )
        ),
    )


@pytest.mark.asyncio
async def test_returns_created(storage):
    returned_user = await CreateUserAction(uid=777000).run()

    expected_user = await storage.user.get(777000)
    assert_that(returned_user, equal_to(expected_user))


@pytest.mark.asyncio
async def test_when_already_exists(storage):
    await CreateUserAction(uid=777000).run()

    with pytest.raises(UserAlreadyExistsError):
        await CreateUserAction(uid=777000).run()
