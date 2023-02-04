from datetime import datetime, timedelta

import psycopg2
import pytest

from sendr_utils import utcnow

from hamcrest import all_of, assert_that, equal_to, greater_than, has_property

from billing.yandex_pay.yandex_pay.core.entities.tokenization_acceptance import TokenizationAcceptance
from billing.yandex_pay.yandex_pay.tests.matchers import close_to_datetime


@pytest.fixture
def tokenization_acceptance_entity() -> TokenizationAcceptance:
    return TokenizationAcceptance(
        uid=2323232,
        accept_date=datetime(2020, 10, 10, 10),
        user_ip='some_ip',
    )


@pytest.mark.asyncio
async def test_create(storage, tokenization_acceptance_entity):
    created = await storage.tokenization_acceptance.create(tokenization_acceptance_entity)
    tokenization_acceptance_entity.created = created.created
    tokenization_acceptance_entity.updated = created.updated
    tokenization_acceptance_entity.accept_date = created.accept_date

    assert_that(
        created,
        equal_to(tokenization_acceptance_entity),
    )


@pytest.mark.asyncio
async def test_get(storage, tokenization_acceptance_entity):
    created = await storage.tokenization_acceptance.create(tokenization_acceptance_entity)

    assert_that(
        await storage.tokenization_acceptance.get(created.uid),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(TokenizationAcceptance.DoesNotExist):
        await storage.tokenization_acceptance.get(1)


@pytest.mark.asyncio
async def test_should_autofill_fields_on_create(storage, tokenization_acceptance_entity):
    created = await storage.tokenization_acceptance.create(tokenization_acceptance_entity)

    assert_that(
        created,
        all_of(
            has_property('created', close_to_datetime(utcnow(), timedelta(minutes=1))),
            has_property('updated', close_to_datetime(utcnow(), timedelta(minutes=1))),
            has_property('accept_date', close_to_datetime(utcnow(), timedelta(minutes=1))),
        )
    )


@pytest.mark.asyncio
async def test_can_update_ip(storage, tokenization_acceptance_entity):
    created = await storage.tokenization_acceptance.create(tokenization_acceptance_entity)
    created.user_ip = 'changed_ip'

    updated = await storage.tokenization_acceptance.save(created)
    created.updated = updated.updated
    created.accept_date = updated.accept_date

    assert_that(
        updated,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_should_refresh_updated_field_on_save(storage, tokenization_acceptance_entity, mocker):
    mocker.patch('sqlalchemy.func.now', wraps=lambda: utcnow())
    created = await storage.tokenization_acceptance.create(tokenization_acceptance_entity)

    updated = await storage.tokenization_acceptance.save(created)
    after_first_save_updated_field = updated.updated

    updated = await storage.tokenization_acceptance.save(updated)
    after_second_save_updated_field = updated.updated

    assert_that(
        after_second_save_updated_field,
        greater_than(after_first_save_updated_field),
    )


@pytest.mark.asyncio
async def test_should_not_let_change_accept_date_on_save(storage, tokenization_acceptance_entity, mocker):
    mocker.patch('sqlalchemy.func.now', wraps=lambda: utcnow())
    created = await storage.tokenization_acceptance.create(tokenization_acceptance_entity)
    accept_date_after_creating = created.accept_date
    created.accept_date = utcnow() - timedelta(days=10)

    updated = await storage.tokenization_acceptance.save(created)

    assert_that(
        accept_date_after_creating,
        equal_to(updated.accept_date),
    )


@pytest.mark.asyncio
async def test_acceptance_is_unique_for_uid(storage, tokenization_acceptance_entity):
    await storage.tokenization_acceptance.create(tokenization_acceptance_entity)

    with pytest.raises(psycopg2.errors.UniqueViolation):
        await storage.tokenization_acceptance.create(tokenization_acceptance_entity)
