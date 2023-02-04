from dataclasses import replace
from datetime import datetime, timezone
from typing import Iterable
from uuid import uuid4

import psycopg2
import pytest

from sendr_aiopg import BaseMapperCRUD

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestOriginModerationMapper(BaseMapperTests):
    @pytest.fixture
    def autofill_with_now(self) -> Iterable[str]:
        return ['created', 'updated', 'finalize_at']

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        entity = await mapper.create(entity)

        assert_that(
            await mapper.get(entity.origin_moderation_id),
            equal_to(entity),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper, entity):
        with pytest.raises(OriginModeration.DoesNotExist):
            await mapper.get(entity.origin_moderation_id)

    @pytest.mark.asyncio
    async def test_save(self, mapper, entity, origin):
        created = await mapper.create(entity)

        saved = await mapper.save(
            replace(
                created,
                ticket='TICKET-2',
                ignored=True,
                resolved=True,
                approved=False,
                revision=555,
                reason={'son': 'rea'},
            )
        )
        assert_that(
            saved,
            equal_to(
                OriginModeration(
                    origin_moderation_id=created.origin_moderation_id,
                    origin_id=created.origin_id,
                    ticket='TICKET-2',
                    ignored=True,
                    resolved=True,
                    approved=False,
                    revision=555,
                    reason={'son': 'rea'},
                    created=created.created,
                    updated=saved.updated,
                    finalize_at=saved.finalize_at,
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        entity = await mapper.create(entity)

        await mapper.delete(entity)

        with pytest.raises(OriginModeration.DoesNotExist):
            await mapper.get(entity.origin_moderation_id)

    @pytest.mark.asyncio
    async def test_find_last(self, mapper, entity):
        entity = await mapper.create(entity)
        entity.created = datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
        await BaseMapperCRUD.save(mapper, entity)

        second_entity = replace(entity, origin_moderation_id=uuid4(), revision=2)
        second_entity = await mapper.create(second_entity)
        second_entity.created = datetime(2021, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
        await BaseMapperCRUD.save(mapper, second_entity)

        found = await mapper.find_last_by_origin_id(origin_id=entity.origin_id)
        assert_that(
            found,
            equal_to(second_entity),
        )

    @pytest.mark.asyncio
    async def test_find_last_not_ignored(self, mapper, entity):
        entity = await mapper.create(entity)
        entity.created = datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
        await BaseMapperCRUD.save(mapper, entity)

        second_entity = replace(entity, origin_moderation_id=uuid4(), revision=2, ignored=True)
        second_entity = await mapper.create(second_entity)
        second_entity.created = datetime(2021, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
        await BaseMapperCRUD.save(mapper, second_entity)

        found = await mapper.find_last_by_origin_id(origin_id=entity.origin_id, ignored=False)
        assert_that(
            found,
            equal_to(entity),
        )

    @pytest.mark.asyncio
    async def test_find_last_approved(self, mapper, entity):
        entity = await mapper.create(entity)
        entity.created = datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
        await BaseMapperCRUD.save(mapper, entity)

        second_entity = replace(
            entity, origin_moderation_id=uuid4(), revision=2, ignored=True, approved=True, resolved=True
        )
        second_entity = await mapper.create(second_entity)
        second_entity.created = datetime(2021, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
        await BaseMapperCRUD.save(mapper, second_entity)

        found = await mapper.find_last_by_origin_id(origin_id=entity.origin_id, approved=True, resolved=True)
        assert_that(
            found,
            equal_to(second_entity),
        )

    @pytest.mark.asyncio
    async def test_find_by_origin_id_and_revision(self, mapper, entity):
        entity = await mapper.create(entity)

        second_entity = replace(entity, origin_moderation_id=uuid4(), revision=2)
        second_entity = await mapper.create(second_entity)

        found = await mapper.find_by_origin_id_and_revision(origin_id=entity.origin_id, revision=entity.revision)
        assert_that(
            found,
            equal_to(entity),
        )

    @pytest.mark.asyncio
    async def test_find_by_origin_id(self, mapper, entity, origin, storage):
        entity = await mapper.create(entity)

        other_origin = replace(origin, origin_id=uuid4(), origin='b.test')
        other_origin = await storage.origin.create(other_origin)

        second_entity = replace(entity, origin_moderation_id=uuid4(), origin_id=other_origin.origin_id)
        second_entity = await mapper.create(second_entity)

        found = await mapper.find_by_origin_id(origin_id=entity.origin_id)
        assert_that(
            found,
            equal_to([entity]),
        )

    @pytest.mark.asyncio
    async def test_find_first_to_finalize(self, mapper, entity):
        entity = await mapper.create(entity)

        loaded = await mapper.find_first_to_finalize()
        assert_that(loaded, equal_to(entity))

    @pytest.mark.asyncio
    async def test_find_first_to_finalize__moderations_not_found(self, mapper):
        with pytest.raises(OriginModeration.DoesNotExist):
            await mapper.find_first_to_finalize()

    @pytest.mark.asyncio
    async def test_find_first_to_finalize__moderation_skipped_if_ticket_not_set(self, mapper, entity):
        entity.ticket = ''
        await mapper.create(entity)

        with pytest.raises(OriginModeration.DoesNotExist):
            await mapper.find_first_to_finalize()

    @pytest.mark.asyncio
    async def test_primary_key(self, mapper, entity):
        await mapper.create(entity)
        with pytest.raises(OriginModeration.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_origin(self, mapper, entity):
        entity.origin_id = uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_unique_on_origin_id_and_revision(self, mapper, entity):
        await mapper.create(entity)

        with pytest.raises(OriginModeration.AlreadyExists):
            await mapper.create(entity)

    @pytest.fixture
    def mapper(self, storage):
        return storage.origin_moderation

    @pytest.fixture
    def entity(self, origin):
        return OriginModeration(
            origin_moderation_id=uuid4(),
            origin_id=origin.origin_id,
            ticket='TICKET-1',
            ignored=False,
            revision=1,
            resolved=False,
            approved=True,
            reason={'rea': 'son'},
        )

    @pytest.fixture
    async def origin(self, storage, merchant):
        return await storage.origin.create(Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='a.test'))
