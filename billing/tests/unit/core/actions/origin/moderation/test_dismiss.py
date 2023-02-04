from uuid import uuid4

import pytest

from sendr_interactions.clients.startrek.entities import TicketResolutions, TicketTransitions

from hamcrest import assert_that, equal_to, has_property

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.dismiss import DismissModerationAction
from billing.yandex_pay_admin.yandex_pay_admin.interactions.startrek import StartrekClient
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration


@pytest.mark.asyncio
async def test_returned(storage, moderation):
    returned = await DismissModerationAction(moderation).run()

    assert_that(
        returned,
        equal_to(await storage.origin_moderation.get(moderation.origin_moderation_id)),
    )


@pytest.mark.asyncio
async def test_calls_startrek(moderation, mock_startrek):
    await DismissModerationAction(moderation).run()

    mock_startrek.assert_awaited_once_with(
        issue_id='TICKET-1',
        transition_id=TicketTransitions.CLOSE,
        resolution_key=TicketResolutions.DONT_DO,
    )


@pytest.mark.asyncio
async def test_updates_moderation(moderation, storage):
    await DismissModerationAction(moderation).run()

    assert_that(
        await storage.origin_moderation.get(moderation.origin_moderation_id),
        has_property('ignored', True),
    )


@pytest.mark.asyncio
async def test_when_moderation_is_already_ignored__does_not_call_startrek(storage, moderation, mock_startrek):
    moderation.ignored = True
    moderation = await storage.origin_moderation.save(moderation)

    await DismissModerationAction(moderation).run()

    mock_startrek.assert_not_called()


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
async def mock_startrek(mocker):
    return mocker.patch.object(StartrekClient, 'execute_issue_transition', mocker.AsyncMock())
