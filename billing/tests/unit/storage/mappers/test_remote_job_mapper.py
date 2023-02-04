from datetime import datetime, timezone

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.enums import RemoteJobState, RemoteJobType
from billing.yandex_pay.yandex_pay.core.entities.remote_job import RemoteJob


@pytest.fixture
def remote_job_entity():
    return RemoteJob(
        job_type=RemoteJobType.MASTERCARD_CREATE_TOKEN,
        external_job_id='tsp-job-id',
        state=RemoteJobState.PENDING,
        run_at=datetime(2000, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
    )


@pytest.mark.asyncio
async def test_create(storage, remote_job_entity):
    created = await storage.remote_job.create(remote_job_entity)
    remote_job_entity.created = created.created
    remote_job_entity.updated = created.updated
    remote_job_entity.run_at = created.run_at
    remote_job_entity.remote_job_id = created.remote_job_id
    assert_that(
        created,
        equal_to(remote_job_entity),
    )


@pytest.mark.asyncio
async def test_get(storage, remote_job_entity):
    created = await storage.remote_job.create(remote_job_entity)
    assert_that(
        await storage.remote_job.get(created.remote_job_id),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(RemoteJob.DoesNotExist):
        await storage.remote_job.get(1)


@pytest.mark.asyncio
async def test_save(storage, remote_job_entity):
    created = await storage.remote_job.create(remote_job_entity)
    created.run_at = datetime(2001, 1, 1, 0, 0, 0, tzinfo=timezone.utc)

    saved = await storage.remote_job.save(created)
    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )
