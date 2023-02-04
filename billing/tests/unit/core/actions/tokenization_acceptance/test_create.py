import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.tokenization_acceptance.create import CreateTokenizationAcceptanceAction
from billing.yandex_pay.yandex_pay.core.entities.tokenization_acceptance import TokenizationAcceptance
from billing.yandex_pay.yandex_pay.core.exceptions import CoreDuplicationError


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
        'user_ip': acceptance.user_ip,
    }


@pytest.fixture
def action(acceptance_params) -> CreateTokenizationAcceptanceAction:
    return CreateTokenizationAcceptanceAction(**acceptance_params)


@pytest.mark.asyncio
async def test_can_create(action, storage):
    returned = await action.run()
    from_storage = await storage.tokenization_acceptance.get(returned.uid)

    assert_that(
        returned,
        equal_to(from_storage),
    )


@pytest.mark.asyncio
async def test_should_raise_duplication_error_if_exist(action, acceptance_from_storage):
    with pytest.raises(CoreDuplicationError):
        await action.run()
