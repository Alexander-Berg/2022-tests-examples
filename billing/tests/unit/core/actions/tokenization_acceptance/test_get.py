import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.tokenization_acceptance.get import GetTokenizationAcceptanceAction
from billing.yandex_pay.yandex_pay.core.entities.tokenization_acceptance import TokenizationAcceptance


@pytest.fixture
def acceptance() -> TokenizationAcceptance:
    return TokenizationAcceptance(
        uid=212323,
        user_ip='some_ip',
    )


@pytest.fixture
async def acceptance_from_storage(storage, acceptance) -> TokenizationAcceptance:
    return await storage.tokenization_acceptance.create(acceptance)


@pytest.fixture
def acceptance_params(acceptance):
    return {
        'uid': acceptance.uid,
    }


@pytest.fixture
def action(acceptance_params) -> GetTokenizationAcceptanceAction:
    return GetTokenizationAcceptanceAction(**acceptance_params)


@pytest.mark.asyncio
async def test_can_get(action, acceptance_from_storage):
    returned = await action.run()

    assert_that(
        returned,
        equal_to(acceptance_from_storage),
    )


@pytest.mark.asyncio
async def test_should_return_none_if_not_exist(action):
    returned = await GetTokenizationAcceptanceAction(uid=1).run()

    assert_that(
        returned,
        equal_to(None),
    )
