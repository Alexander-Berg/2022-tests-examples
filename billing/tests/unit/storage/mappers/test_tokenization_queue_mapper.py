from datetime import datetime, timezone

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.enums import TokenizationQueueState
from billing.yandex_pay.yandex_pay.core.entities.tokenization_queue import TokenizationQueue


@pytest.fixture
async def card(storage, card_entity):
    return await storage.card.create(card_entity)


@pytest.fixture
async def merchant(storage, merchant_entity):
    return await storage.merchant.create(merchant_entity)


@pytest.fixture
def tokenization_queue_entity(card, merchant):
    return TokenizationQueue(
        card_id=card.card_id,
        merchant_id=merchant.merchant_id,
        params={'bin': '123456'},
        state=TokenizationQueueState.PENDING,
        run_at=datetime(2000, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
    )


@pytest.mark.asyncio
async def test_create(storage, tokenization_queue_entity, card, merchant):
    created = await storage.tokenization_queue.create(tokenization_queue_entity)
    tokenization_queue_entity.created = created.created
    tokenization_queue_entity.updated = created.updated
    tokenization_queue_entity.run_at = created.run_at
    tokenization_queue_entity.tokenization_queue_id = created.tokenization_queue_id
    assert_that(
        created,
        equal_to(tokenization_queue_entity),
    )


@pytest.mark.asyncio
async def test_get(storage, tokenization_queue_entity):
    created = await storage.tokenization_queue.create(tokenization_queue_entity)
    assert_that(
        await storage.tokenization_queue.get(created.tokenization_queue_id),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(TokenizationQueue.DoesNotExist):
        await storage.tokenization_queue.get(1)


@pytest.mark.asyncio
async def test_save(storage, tokenization_queue_entity):
    created = await storage.tokenization_queue.create(tokenization_queue_entity)
    created.run_at = datetime(2001, 1, 1, 0, 0, 0, tzinfo=timezone.utc)

    saved = await storage.tokenization_queue.save(created)
    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )
