import uuid

import sqlalchemy as sa
import psycopg2
import pytest

from hamcrest import all_of, assert_that, contains_inanyorder, equal_to, has_entries, has_property, instance_of


def is_uuid4():
    """
    Это hamcrest matcher
    * Проверяет, что uuid имеет версию 4
        В миграции это достигается с помощью `placing '4' from 13`
    * Проверяет, что variant тоже валидный генерится.
        За это в миграции отвечает `placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17`
        Сорри, что так жутко выглядит. Суть такова: старшие биты 16-ричной цифры под номером 17 в
        uuid'е отвечает за некоторый "тип" uuid'а, который может влиять на интерпретацию остальных битов.
        По RFC https://tools.ietf.org/html/rfc4122#section-4.1.1 актуальным является тип 10xx т.е. шестнадцатеричные
        цифры [8, 9, a, b]
    """
    return all_of(
        instance_of(uuid.UUID),
        has_property('version', equal_to(4)),
        has_property('variant', equal_to(uuid.RFC_4122)),
    )


class TestMigration15:
    """
    Предназначение миграции - изменить тип у psp_id с int на uuid.
    UUID'ы генерятся рандомные. Качество рандома, думаю, не имеет значения.
    Генерится uuid 4 версии. На всякий случай. А то вдруг где-нибудь что-нибудь когда-нибудь?
    """

    @pytest.fixture(autouse=True)
    async def setup(self, migrate, pool):
        migrate('14')

        async with pool.acquire() as conn:
            await conn.execute(
                'insert into yandex_pay.psp (psp_id, psp_external_id, public_key) values (%s, %s, %s), (%s, %s, %s)',
                (
                    1, 'acme', '',
                    2, 'horn-hooves', '',
                ),
            )
            await conn.execute(
                'insert into yandex_pay.psp_auth_keys (psp_id, psp_key_id, key) values (%s, %s, %s), (%s, %s, %s)',
                (
                    1, 1, 'acme-key',
                    2, 1, 'horn-hooves-key',
                ),
            )

        migrate('15')

    @pytest.mark.asyncio
    async def test_psp_migrated_well(self, pool):
        async with pool.acquire() as conn:
            result = await conn.execute('select psp_external_id, psp_id from yandex_pay.psp')
            psp = await result.fetchall()

            assert_that(
                psp,
                contains_inanyorder(
                    has_entries({
                        'psp_external_id': 'acme',
                        'psp_id': is_uuid4(),
                    }),
                    has_entries({
                        'psp_external_id': 'horn-hooves',
                        'psp_id': is_uuid4(),
                    }),
                )
            )

    @pytest.mark.asyncio
    async def test_psp_keys_migrated_well(self, pool):
        async with pool.acquire() as conn:
            result = await conn.execute('select psp_id, psp_external_id from yandex_pay.psp')
            psp = {row[1]: row[0] async for row in result}
            result = await conn.execute('select psp_id, psp_key_id, key from yandex_pay.psp_auth_keys')
            keys = await result.fetchall()
            assert_that(
                keys,
                contains_inanyorder(
                    (psp['acme'], 1, 'acme-key'),
                    (psp['horn-hooves'], 1, 'horn-hooves-key'),
                )
            )

    @pytest.mark.asyncio
    async def test_mapping_is_fine(self, pool):
        """
        Проверим, что старый код не сломается из-за миграции.
        """
        metadata = sa.MetaData(schema='yandex_pay')
        t_psp = sa.Table(
            'psp', metadata,
            sa.Column('psp_id', sa.Integer())
        )

        async with pool.acquire() as conn:
            result = await conn.execute(t_psp.select())
            ids = [row['psp_id'] async for row in result]
            assert_that(
                ids,
                contains_inanyorder(is_uuid4(), is_uuid4())
            )

    @pytest.mark.asyncio
    async def test_psp_id_has_no_default(self, pool):
        async with pool.acquire() as conn:
            with pytest.raises(psycopg2.errors.NotNullViolation) as exc_info:
                await conn.execute("insert into yandex_pay.psp (psp_external_id, public_key) values ('foobar', 'baz')")

            assert 'null value in column "psp_id" violates not-null constraint' in str(exc_info.value)
