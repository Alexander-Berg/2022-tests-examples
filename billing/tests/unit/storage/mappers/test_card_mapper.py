import uuid
from datetime import datetime, timedelta, timezone

import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_length, has_property, none, not_none

from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import ImageType, TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.image import Image


@pytest.fixture
async def merchant(storage, merchant_entity):
    return await storage.merchant.create(merchant_entity)


@pytest.fixture
async def card(storage, card_entity):
    return await storage.card.create(card_entity)


@pytest.fixture
def enrollment_entity(card: Card, merchant):
    return Enrollment(
        card_id=card.card_id,
        merchant_id=None,
        tsp_card_id='tsp-card-id',
        tsp_token_id='tsp-token-id',
        tsp_token_status=TSPTokenStatus.ACTIVE,
        card_last4=card.last4,
    )


@pytest.mark.asyncio
async def test_create(storage, card_entity):
    created = await storage.card.create(card_entity)
    card_entity.card_id = created.card_id
    card_entity.created = created.created
    card_entity.updated = created.updated
    assert created.revision == 0
    assert_that(
        created,
        equal_to(card_entity),
    )


@pytest.mark.asyncio
async def test_get(storage, card_entity):
    created = await storage.card.create(card_entity)

    actual = await storage.card.get(created.card_id)

    assert_that(
        actual,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Card.DoesNotExist):
        await storage.card.get(uuid.uuid4())


@pytest.mark.asyncio
async def test_save(storage, card_entity):
    created = await storage.card.create(card_entity)
    created.trust_card_id = '123'
    saved = await storage.card.save(created)

    assert saved.revision == created.revision + 1

    created.revision = saved.revision
    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_save_updates_revision_each_time(storage, card_entity):
    card_entity.trust_card_id = '123'

    created = await storage.card.create(card_entity)
    assert created.revision == 0

    saved = await storage.card.save(created)
    assert saved.revision == 1

    saved: Card = await storage.card.save(saved)
    assert saved.revision == 2

    fetched = await storage.card.get_by_card_id_and_uid(card_id=saved.card_id, owner_uid=saved.owner_uid)
    assert fetched.revision == 2


class TestCardFind:
    @pytest.mark.asyncio
    async def test_with_enrollment_join(self, storage, card, enrollment_entity):
        enrollment = await storage.enrollment.create(enrollment_entity)

        card.enrollment = enrollment
        assert_that(
            await alist(storage.card.find(join_enrollment=True)),
            equal_to([card]),
        )

    @pytest.mark.asyncio
    async def test_without_enrollment_join(self, storage, card, enrollment_entity):
        await storage.enrollment.create(enrollment_entity)

        assert_that(card, has_property('enrollment', none()))
        assert_that(
            await alist(storage.card.find(join_enrollment=False)),
            equal_to([card]),
        )

    @pytest.mark.asyncio
    async def test_enrollment_not_joined_if_merchant_not_none(
        self, storage, card, merchant, enrollment_entity
    ):
        enrollment_entity.merchant_id = merchant.merchant_id
        await storage.enrollment.create(enrollment_entity)

        assert_that(card, has_property('enrollment', none()))
        assert_that(
            await alist(storage.card.find(join_enrollment=True)),
            equal_to([card]),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('exclude_inactive', [True, False])
    async def test_enrollment_joined_even_if_expired(
        self, storage, card, enrollment_entity, exclude_inactive
    ):
        enrollment_entity.expire = utcnow() - timedelta(minutes=5)
        enrollment = await storage.enrollment.create(enrollment_entity)

        card.enrollment = enrollment
        returned = await alist(
            storage.card.find(join_enrollment=True, exclude_inactive=exclude_inactive)
        )
        assert_that(returned, equal_to([card]))

    @pytest.mark.asyncio
    async def test_card_not_returned_if_expired(self, storage, card_entity):
        card_entity.expire = utcnow() - timedelta(minutes=5)
        await storage.card.create(card_entity)

        assert_that(
            await alist(storage.card.find(exclude_inactive=True)),
            has_length(0),
        )

    @pytest.mark.asyncio
    async def test_card_not_returned_if_marked_as_removed(self, storage, card_entity):
        card_entity.is_removed = True
        await storage.card.create(card_entity)

        assert_that(
            await alist(storage.card.find(exclude_inactive=True)),
            has_length(0),
        )

    @pytest.mark.asyncio
    async def test_filter_by_tsp_type(self, storage, rands):
        cards = []
        for tsp_type in [TSPType.VISA, TSPType.MASTERCARD]:
            card = await storage.card.create(Card(
                trust_card_id=rands(),
                owner_uid=5555,
                tsp=tsp_type,
                expire=datetime(2000, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                last4='1234'
            ))
            cards.append(card)
        assert_that(
            await alist(storage.card.find(tsp=TSPType.VISA)),
            equal_to([cards[0]]),
        )

    @pytest.mark.asyncio
    async def test_filter_by_owner(self, storage, rands):
        cards = []
        for owner_uid in range(2):
            card = await storage.card.create(Card(
                trust_card_id=rands(),
                owner_uid=owner_uid,
                tsp=TSPType.VISA,
                expire=datetime(2000, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                last4='1234'
            ))
            cards.append(card)
        assert_that(
            await alist(storage.card.find(owner_uid=0)),
            equal_to([cards[0]]),
        )


@pytest.mark.asyncio
async def test_get_by_card_id_and_uid(storage, card_entity):
    created = await storage.card.create(card_entity)
    assert_that(
        await storage.card.get_by_card_id_and_uid(created.card_id, created.owner_uid),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_enrollment_join_is_outer(storage):
    await storage.card.create(Card(
        trust_card_id='trust-card-id',
        owner_uid=5555,
        tsp=TSPType.VISA,
        expire=datetime(2000, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
        last4='1234'
    ))
    cards = await alist(storage.card.find(join_enrollment=True))
    assert_that(cards, has_length(1))
    assert_that(cards[0], has_property('enrollment', none()))


@pytest.mark.asyncio
async def test_get_by_card_id_and_uid_when_not_found(storage, card_entity):
    with pytest.raises(Card.DoesNotExist):
        await storage.card.get_by_card_id_and_uid(uuid.uuid4(), 0)


@pytest.mark.asyncio
async def test_can_create_image_entry_for_given_card(storage, card_entity):
    created = await storage.card.create(card_entity)
    assert_that(
        created,
        has_property('image_id', none())
    )

    image = await storage.image.create(
        Image(ImageType.MASTERCARD_CARD_IMAGE, 'external_image_id')
    )
    assert_that(
        image,
        has_property('image_id', not_none())
    )
    created.image_id = image.image_id
    await storage.card.save(created)

    loaded = await storage.card.get(created.card_id)
    assert_that(
        loaded,
        has_property('image_id', equal_to(image.image_id))
    )
