from datetime import timedelta
from typing import Iterable
from uuid import UUID

import pytest

from sendr_pytest.matchers import close_to_datetime
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, greater_than, instance_of, match_equality

from pay.bill_payments.bill_payments.storage.mappers.base import BaseMapperCRUD, EntityType


class BaseMapperTests:
    @pytest.fixture
    def entity(self) -> EntityType:
        raise NotImplementedError

    @pytest.fixture
    def mapper(self, storage) -> BaseMapperCRUD:
        raise NotImplementedError

    @pytest.fixture
    def autofill_with_now(self) -> Iterable[str]:
        return ['created', 'updated']

    @pytest.fixture
    def autofill_with_uuid(self):
        return []

    @pytest.mark.asyncio
    async def test_create(self, mapper, entity, autofill_with_now, autofill_with_uuid):
        created = await mapper.create(entity)
        delta = timedelta(seconds=20)
        for attr in autofill_with_now:
            setattr(
                entity,
                attr,
                match_equality(close_to_datetime(utcnow(), delta=delta)),
            )
        for attr in autofill_with_uuid:
            setattr(
                entity,
                attr,
                match_equality(instance_of(UUID)),
            )

        assert_that(
            created,
            equal_to(entity),
        )

    @pytest.mark.asyncio
    async def test_should_refresh_updated_field_on_save(self, mapper, entity, mocker):
        mocker.patch('sqlalchemy.func.now', wraps=lambda: utcnow())
        created = await mapper.create(entity)

        updated = await mapper.save(created)
        after_first_save_updated_field = updated.updated

        updated = await mapper.save(updated)
        after_second_save_updated_field = updated.updated

        assert_that(
            after_second_save_updated_field,
            greater_than(after_first_save_updated_field),
        )
