import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.change_log import ChangeLog
from billing.yandex_pay.yandex_pay.core.entities.enums import OperationKind
from billing.yandex_pay.yandex_pay.storage.exceptions import SaveNotSupportedStorageError


@pytest.fixture
def change_log_entity():
    return ChangeLog(
        operation_kind=OperationKind.ENROLLMENT_CREATED,
        data={'a': 'b', 'c': 1},
    )


@pytest.mark.asyncio
async def test_create(storage, change_log_entity):
    created = await storage.change_log.create(change_log_entity)
    change_log_entity.event_id = created.event_id
    change_log_entity.created = created.created
    change_log_entity.updated = created.updated

    assert_that(
        created,
        equal_to(change_log_entity),
    )


@pytest.mark.asyncio
async def test_save(storage, change_log_entity):
    created = await storage.change_log.create(change_log_entity)
    with pytest.raises(SaveNotSupportedStorageError):
        await storage.change_log.save(created)
