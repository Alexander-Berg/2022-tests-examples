import uuid
from datetime import datetime, timezone

import pytest
from psycopg2.errors import UniqueViolation

from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to, has_length, has_property, none

from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant


@pytest.fixture
async def card(storage, card_entity):
    return await storage.card.create(card_entity)


@pytest.fixture
async def card2(storage):
    card_entity = Card(
        trust_card_id=str(uuid.uuid4()),
        owner_uid=5555,
        tsp=TSPType.VISA,
        expire=datetime(2000, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
        last4='1234',
    )
    return await storage.card.create(card_entity)


@pytest.fixture
async def merchant(storage, merchant_entity):
    return await storage.merchant.create(merchant_entity)


@pytest.mark.asyncio
async def test_create(storage, enrollment_entity, card, merchant):
    created = await storage.enrollment.create(enrollment_entity)
    enrollment_entity.created = created.created
    enrollment_entity.updated = created.updated
    enrollment_entity.enrollment_id = created.enrollment_id

    assert_that(created, equal_to(enrollment_entity))
    assert_that(created, has_property('expire', none()))


@pytest.mark.asyncio
async def test_get(storage, enrollment_entity):
    created = await storage.enrollment.create(enrollment_entity)
    assert_that(
        await storage.enrollment.get(enrollment_entity.enrollment_id),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Enrollment.DoesNotExist):
        await storage.enrollment.get(uuid.uuid4())


@pytest.mark.asyncio
async def test_save(storage, enrollment_entity):
    created = await storage.enrollment.create(enrollment_entity)
    created.tsp_card_id = 'other-tsp-card-id'

    saved = await storage.enrollment.save(created)
    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_can_not_create_two_or_more_enrollments_for_single_card(storage, enrollment_entity):
    await storage.enrollment.create(enrollment_entity)
    enrollment_entity.enrollment_id = None

    with pytest.raises(UniqueViolation):
        await storage.enrollment.create(enrollment_entity)


@pytest.mark.asyncio
async def test_can_create_with_expiration_set(storage, enrollment_entity):
    time_now = utcnow()
    enrollment_entity.expire = time_now
    created = await storage.enrollment.create(enrollment_entity)
    enrollment_entity.created = created.created
    enrollment_entity.updated = created.updated
    enrollment_entity.enrollment_id = created.enrollment_id

    assert_that(created, equal_to(enrollment_entity))


@pytest.mark.asyncio
async def test_can_get_enrollment_by_card_id_and_merchant_id(storage, enrollment_entity):
    await storage.enrollment.create(enrollment_entity)

    enrollment_by_card_id = await storage.enrollment.get_by_card_id_and_merchant_id(
        card_id=enrollment_entity.card_id,
        merchant_id=enrollment_entity.merchant_id,
    )

    assert_that(enrollment_by_card_id.enrollment_id, equal_to(enrollment_entity.enrollment_id))


@pytest.mark.asyncio
async def test_can_get_by_tsp_token_id(storage, enrollment_entity):
    card = await storage.card.get(enrollment_entity.card_id)
    await storage.enrollment.create(enrollment_entity)
    enrollment = await storage.enrollment.get_by_tsp_token_id(
        tsp=card.tsp,
        tsp_token_id=enrollment_entity.tsp_token_id
    )
    assert_that(enrollment.enrollment_id, equal_to(enrollment_entity.enrollment_id))


@pytest.mark.asyncio
async def test_can_not_get_by_tsp_token_id_with_wrong_tsp(storage, enrollment_entity):
    card = await storage.card.get(enrollment_entity.card_id)
    tsp = TSPType.VISA
    if card.tsp == TSPType.VISA:
        tsp = TSPType.MASTERCARD

    await storage.enrollment.create(enrollment_entity)

    with pytest.raises(Enrollment.DoesNotExist):
        await storage.enrollment.get_by_tsp_token_id(
            tsp=tsp,
            tsp_token_id=enrollment_entity.tsp_token_id
        )


@pytest.mark.asyncio
async def test_can_get_by_tsp_card_id(storage, enrollment_entity):
    card = await storage.card.get(enrollment_entity.card_id)
    await storage.enrollment.create(enrollment_entity)
    enrollments = await storage.enrollment.get_by_tsp_card_id(
        tsp=card.tsp,
        tsp_card_id=enrollment_entity.tsp_card_id
    )
    assert_that(len(enrollments), equal_to(1))
    assert_that(enrollments[0].enrollment_id, equal_to(enrollment_entity.enrollment_id))


@pytest.mark.asyncio
async def test_can_not_get_by_tsp_card_id(storage, enrollment_entity):
    card = await storage.card.get(enrollment_entity.card_id)
    await storage.enrollment.create(enrollment_entity)
    with pytest.raises(Enrollment.DoesNotExist):
        await storage.enrollment.get_by_tsp_card_id(
            tsp=card.tsp,
            tsp_card_id='doesnotexist'
        )


@pytest.mark.asyncio
async def test_can_get_few_enrollments_by_tsp_card_id(storage, card, card2):
    tsp_card_id = str(uuid.uuid4())

    e1 = await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_card_id=tsp_card_id,
            tsp_token_id=str(uuid.uuid4()),
            tsp_token_status=TSPTokenStatus.ACTIVE,
            card_last4=card.last4,
        )
    )

    e2 = await storage.enrollment.create(
        Enrollment(
            card_id=card2.card_id,
            merchant_id=None,
            tsp_card_id=tsp_card_id,
            tsp_token_id=str(uuid.uuid4()),
            tsp_token_status=TSPTokenStatus.ACTIVE,
            card_last4=card2.last4,
        )
    )

    enrollments = await storage.enrollment.get_by_tsp_card_id(
        tsp=card.tsp,
        tsp_card_id=tsp_card_id
    )

    assert_that(enrollments, has_length(2))
    assert_that(
        enrollments,
        contains_inanyorder(
            has_property('enrollment_id', e1.enrollment_id),
            has_property('enrollment_id', e2.enrollment_id),
        ),
    )


@pytest.mark.asyncio
async def test_should_raise_exception_on_get_by_card_id_and_merchant_id_if_not_exist(storage):
    with pytest.raises(Enrollment.DoesNotExist):
        await storage.enrollment.get_by_card_id_and_merchant_id(card_id=uuid.uuid4(), merchant_id=uuid.uuid4())


class TestFindByOwnerUidForDefaultMerchant:
    @pytest.mark.asyncio
    async def test_fetches_enrollment_with_default_merchant(self, storage, card: Card):
        enrollment_for_default_merchant = await storage.enrollment.create(Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_card_id='tsp-card-id',
            tsp_token_id='tsp-token-id',
            tsp_token_status=TSPTokenStatus.ACTIVE,
            card_last4=card.last4,
        ))

        filtered = await alist(storage.enrollment.find_common_by_owner_uid(
            owner_uid=card.owner_uid),
        )

        assert_that(filtered, has_length(1))

        fetched_enrollment = filtered[0]
        assert_that(
            fetched_enrollment,
            equal_to(enrollment_for_default_merchant)
        )

    @pytest.mark.asyncio
    async def test_not_fetches_enrollment_with_non_default_merchant(self, storage, card: Card, merchant: Merchant):
        await storage.enrollment.create(Enrollment(
            card_id=card.card_id,
            merchant_id=merchant.merchant_id,
            tsp_card_id='tsp-card-id',
            tsp_token_id='tsp-token-id',
            tsp_token_status=TSPTokenStatus.ACTIVE,
            card_last4=card.last4,
        ))

        filtered = await alist(storage.enrollment.find_common_by_owner_uid(
            owner_uid=card.owner_uid),
        )

        assert_that(filtered, has_length(0))
