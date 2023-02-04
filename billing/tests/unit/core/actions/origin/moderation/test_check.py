from uuid import uuid4

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.check import (
    CheckHasNoApprovedModerationAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import OriginHasApprovedModerationError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration


@pytest.mark.asyncio
async def test_when_no_moderation__should_not_raise(storage, origin):
    await CheckHasNoApprovedModerationAction(origin_id=origin.origin_id).run()


@pytest.mark.parametrize('resolved', (True, False))
@pytest.mark.asyncio
async def test_when_moderation_is_not_approved__should_not_raise(storage, origin, resolved):
    # should not raise
    await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(),
            origin_id=origin.origin_id,
            ticket='',
            revision=origin.revision,
            ignored=False,
            resolved=resolved,
            approved=False,
        )
    )

    await CheckHasNoApprovedModerationAction(origin_id=origin.origin_id).run()


@pytest.mark.asyncio
async def test_when_moderation_is_approved__raises(storage, origin):
    # should not raise
    await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(),
            origin_id=origin.origin_id,
            ticket='',
            revision=origin.revision,
            ignored=False,
            resolved=True,
            approved=True,
        )
    )

    with pytest.raises(OriginHasApprovedModerationError):
        await CheckHasNoApprovedModerationAction(origin_id=origin.origin_id).run()


@pytest.fixture
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='https://a.test', revision=777)
    )
