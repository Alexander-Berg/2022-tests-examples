import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.bulk_tokenization_state import BulkTokenizationState


@pytest.fixture
def bulk_tokenization_state_entity():
    return BulkTokenizationState(key="test_key")


@pytest.mark.asyncio
async def test_create(storage, bulk_tokenization_state_entity):
    created = await storage.bulk_tokenization_state.create(bulk_tokenization_state_entity)

    bulk_tokenization_state_entity.created = created.created
    bulk_tokenization_state_entity.updated = created.updated
    assert_that(
        created,
        equal_to(bulk_tokenization_state_entity),
    )


@pytest.mark.asyncio
async def test_get(storage, bulk_tokenization_state_entity):
    created = await storage.bulk_tokenization_state.create(bulk_tokenization_state_entity)

    assert_that(
        await storage.bulk_tokenization_state.get(bulk_tokenization_state_entity.key),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(BulkTokenizationState.DoesNotExist):
        await storage.bulk_tokenization_state.get("not_exist")


@pytest.mark.asyncio
async def test_save(storage, bulk_tokenization_state_entity):
    created = await storage.bulk_tokenization_state.create(bulk_tokenization_state_entity)
    created.last_processed_uid = 55555
    created.has_finished = True

    saved = await storage.bulk_tokenization_state.save(created)
    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )
