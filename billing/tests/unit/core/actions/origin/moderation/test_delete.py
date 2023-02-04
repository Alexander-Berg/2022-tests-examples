from uuid import uuid4

import pytest

from hamcrest import assert_that, has_property

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.delete import (
    DeleteOriginModerationsAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration


@pytest.mark.asyncio
async def test_deletes_origin_moderation(storage, partner, origin):
    first_moderation = await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(),
            origin_id=origin.origin_id,
            ticket='TICKET-1',
            revision=1,
        )
    )
    second_moderation = await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(),
            origin_id=origin.origin_id,
            ticket='TICKET-2',
            revision=2,
        )
    )

    await DeleteOriginModerationsAction(origin.origin_id).run()

    with pytest.raises(OriginModeration.DoesNotExist):
        await storage.origin_moderation.get(first_moderation.origin_moderation_id)
    with pytest.raises(OriginModeration.DoesNotExist):
        await storage.origin_moderation.get(second_moderation.origin_moderation_id)


@pytest.mark.asyncio
async def test_does_not_delete_alien_moderation(storage, partner, origin, alien_origin):
    alien_moderation = await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(),
            origin_id=alien_origin.origin_id,
            ticket='TICKET-3',
            revision=1,
        )
    )

    await DeleteOriginModerationsAction(origin.origin_id).run()

    assert_that(
        await storage.origin_moderation.get(alien_moderation.origin_moderation_id),
        has_property('ticket', 'TICKET-3'),
    )


@pytest.fixture
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='https://a.test')
    )


@pytest.fixture
async def alien_origin(storage, partner, merchant):
    return await storage.origin.create(Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='b.test'))
