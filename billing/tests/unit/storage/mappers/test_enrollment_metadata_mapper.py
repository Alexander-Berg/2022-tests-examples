from uuid import uuid4

import psycopg2
import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_properties, none

from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment, EnrollmentMetadata


@pytest.fixture
async def card(storage, card_entity):
    return await storage.card.create(card_entity)


@pytest.fixture
async def merchant(storage, merchant_entity):
    return await storage.merchant.create(merchant_entity)


@pytest.fixture
async def enrollment(storage, enrollment_entity) -> Enrollment:
    return await storage.enrollment.create(enrollment_entity)


@pytest.fixture
def fake_metadata():
    return {
        'metadata': 'fake',
        'contains': [1, None],
    }


@pytest.mark.asyncio
async def test_create(storage, enrollment):
    created = await storage.enrollment_metadata.create(
        EnrollmentMetadata(enrollment_id=enrollment.enrollment_id)
    )
    assert_that(
        created,
        has_properties(
            enrollment_id=equal_to(enrollment.enrollment_id),
            raw_tsp_metadata=equal_to({}),
            event_timestamp=none()
        )
    )


@pytest.mark.asyncio
async def test_create_missing_enrollment(storage):
    pattern = 'Key .* is not present in table "enrollments"'
    with pytest.raises(psycopg2.IntegrityError, match=pattern):
        await storage.enrollment_metadata.create(
            EnrollmentMetadata(enrollment_id=uuid4())
        )


@pytest.mark.asyncio
async def test_get(storage, enrollment):
    created = await storage.enrollment_metadata.create(
        EnrollmentMetadata(enrollment_id=enrollment.enrollment_id)
    )
    assert_that(
        await storage.enrollment_metadata.get(created.enrollment_id),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(EnrollmentMetadata.DoesNotExist):
        await storage.enrollment_metadata.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, enrollment, fake_metadata):
    created = await storage.enrollment_metadata.create(
        EnrollmentMetadata(enrollment_id=enrollment.enrollment_id)
    )
    created.raw_tsp_metadata = fake_metadata
    created.event_timestamp = utcnow()

    saved = await storage.enrollment_metadata.save(created)
    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )
