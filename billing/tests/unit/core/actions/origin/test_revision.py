from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.check import (
    CheckHasNoApprovedModerationAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.create import CreateOriginModerationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.dismiss import DismissModerationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.revision import BumpOriginRevisionAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import OriginNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration


@pytest.mark.asyncio
async def test_updates_revision(storage, origin):
    await BumpOriginRevisionAction(origin.origin_id).run()

    updated_origin = await storage.origin.get(origin.origin_id)
    assert_that(updated_origin.revision, equal_to(origin.revision + 1))


@pytest.mark.asyncio
async def test_calls_dismiss_moderation(origin, mock_dismiss_moderation, moderation):
    await BumpOriginRevisionAction(origin.origin_id).run()

    mock_dismiss_moderation.assert_run_once_with(moderation)


@pytest.mark.asyncio
async def test_calls_check_has_no_approved_moderation(origin, mock_check_has_no_approved_moderation, moderation):
    await BumpOriginRevisionAction(origin.origin_id).run()

    mock_check_has_no_approved_moderation.assert_run_once_with(origin_id=origin.origin_id)


@pytest.mark.asyncio
async def test_when_moderation_not_found__does_not_call_dismiss_moderation(origin, mock_dismiss_moderation):
    await BumpOriginRevisionAction(origin.origin_id).run()

    mock_dismiss_moderation.assert_not_called()


@pytest.mark.asyncio
async def test_when_origin_not_found__raises_error():
    with pytest.raises(OriginNotFoundError):
        await BumpOriginRevisionAction(uuid4()).run()


@pytest.fixture(autouse=True)
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(origin_id=uuid4(), origin='https://a.test', merchant_id=merchant.merchant_id)
    )


@pytest.fixture
async def moderation(storage, origin):
    return await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(), origin_id=origin.origin_id, revision=origin.revision, ticket='TICKET-1'
        )
    )


@pytest.fixture(autouse=True)
def mock_dismiss_moderation(mock_action):
    return mock_action(DismissModerationAction)


@pytest.fixture(autouse=True)
def mock_create_moderation(mock_action):
    return mock_action(CreateOriginModerationAction)


@pytest.fixture(autouse=True)
def mock_check_has_no_approved_moderation(mock_action):
    return mock_action(CheckHasNoApprovedModerationAction)
