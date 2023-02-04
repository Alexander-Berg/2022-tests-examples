import uuid
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Optional

import pytest
import sqlalchemy as sa

from sendr_aiopg.data_mapper import SelectableDataMapper, TableDataDumper
from sendr_aiopg.query_builder import CRUDQueries

from hamcrest import assert_that, has_properties, instance_of

from billing.yandex_pay.yandex_pay.core.entities.base import Entity, autogenerate_uuid
from billing.yandex_pay.yandex_pay.storage.mappers.base import BaseMapperCRUD
from billing.yandex_pay.yandex_pay.storage.types import UUID

metadata = sa.MetaData(schema='yandex_pay')


entity_with_uuid = sa.Table(
    'entity_with_uuid', metadata,
    sa.Column('uuid_id', UUID(), primary_key=True, nullable=False),
    sa.Column('seq_id', sa.BigInteger(), primary_key=True, nullable=False, autoincrement=True),
    sa.Column('created', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated', sa.DateTime(timezone=True), nullable=False),
)


@dataclass
class EntityWithUUID(Entity):
    uuid_id: Optional[uuid.UUID] = field(default=None, metadata=autogenerate_uuid())
    seq_id: Optional[int] = None
    created: Optional[datetime] = None
    updated: Optional[datetime] = None


class EntityDataMapper(SelectableDataMapper):
    entity_class = EntityWithUUID
    selectable = entity_with_uuid


class EntityDataDumper(TableDataDumper):
    entity_class = EntityWithUUID
    table = entity_with_uuid


class Mapper(BaseMapperCRUD[Entity]):
    model = Entity

    _builder = CRUDQueries(
        base=entity_with_uuid,
        id_fields=('uuid_id', 'seq_id'),
        mapper_cls=EntityDataMapper,
        dumper_cls=EntityDataDumper,
    )


@pytest.fixture
async def create_table(dbconn):
    await dbconn.execute("""
    create table yandex_pay.entity_with_uuid (
        uuid_id uuid,
        seq_id bigserial,
        created timestamptz default now(),
        updated timestamptz default now(),
        primary key (uuid_id, seq_id)
    )
    """)


@pytest.fixture
def mapper(dbconn, create_table, dummy_logger):
    return Mapper(connection=dbconn, logger=dummy_logger)


@pytest.fixture
def entity():
    return EntityWithUUID()


@pytest.fixture
async def persistent_entity(entity, mapper):
    return await mapper.create(entity)


@pytest.mark.asyncio
async def test_create(mapper, dbconn, entity):
    entity.uuid_id = uuid.uuid4()
    entity.seq_id = 422
    now = (await (await dbconn.execute('select now()')).first())[0]
    assert_that(
        await mapper.create(entity),
        has_properties({
            'uuid_id': entity.uuid_id,
            'seq_id': entity.seq_id,
            'created': now,
            'updated': now,
        })
    )


@pytest.mark.asyncio
async def test_create_without_id_supplied__generates_id(mapper, entity):
    assert_that(
        await mapper.create(entity),
        has_properties({
            'uuid_id': instance_of(uuid.UUID),
            'seq_id': instance_of(int),
        })
    )


@pytest.mark.asyncio
async def test_create_with_additional_args__raises_type_error(mapper, entity):
    with pytest.raises(TypeError):
        await mapper.create(entity, 1)


@pytest.mark.asyncio
async def test_create_with_additional_kwargs__raises_type_error(mapper, entity):
    with pytest.raises(TypeError):
        await mapper.create(entity, foo='bar')


@pytest.mark.asyncio
async def test_save__updates_updated(mocker, mapper, persistent_entity):
    created_at = persistent_entity.created
    now = created_at + timedelta(days=1)
    mocker.patch('billing.yandex_pay.yandex_pay.storage.mappers.base.func.now', mocker.Mock(return_value=now))

    assert_that(
        await mapper.save(persistent_entity),
        has_properties({
            'updated': now,
            'created': created_at,
        })
    )


@pytest.mark.asyncio
async def test_save_does_not_update_omited_fields(mapper, persistent_entity):
    entity = await mapper.get(persistent_entity.uuid_id, persistent_entity.seq_id)
    entity.created = persistent_entity.created - timedelta(days=1)

    assert_that(
        await mapper.save(entity),
        has_properties({
            'created': persistent_entity.created,
        })
    )
