import asyncio
import pytest

from maps.infra.sedem.machine.lib.cli_settings_api import CliSettings, CliSettingsApi
from maps.infra.sedem.machine.tests.typing import MongoFixture, CliSettingsFactory


@pytest.mark.asyncio
async def test_load_settings(mongo: MongoFixture,
                             cli_settings_factory: CliSettingsFactory) -> None:
    await cli_settings_factory(min_valid_version=1)

    async with await mongo.async_client.start_session() as session:
        settings = await CliSettingsApi(session).load_settings()

    assert settings.min_valid_version == 1


@pytest.mark.asyncio
async def test_insert_new_settings(mongo: MongoFixture) -> None:
    async with await mongo.async_client.start_session() as session:
        await CliSettingsApi(session).update_settings(CliSettings(
            min_valid_version=100500,
        ))

        settings = await CliSettingsApi(session).load_settings()

    assert settings.min_valid_version == 100500


@pytest.mark.asyncio
async def test_update_existing_settings(mongo: MongoFixture,
                                        cli_settings_factory: CliSettingsFactory) -> None:
    await cli_settings_factory(min_valid_version=1)

    async with await mongo.async_client.start_session() as session:
        await CliSettingsApi(session).update_settings(CliSettings(
            min_valid_version=100500,
        ))

        settings = await CliSettingsApi(session).load_settings()

    assert settings.min_valid_version == 100500


@pytest.mark.asyncio
async def test_concurrent_update_settings(mongo: MongoFixture,
                                          cli_settings_factory: CliSettingsFactory) -> None:
    await cli_settings_factory(min_valid_version=1)

    async with await mongo.async_client.start_session() as session1:
        async with await mongo.async_client.start_session() as session2:
            await asyncio.gather(
                CliSettingsApi(session1).update_settings(CliSettings(
                    min_valid_version=10,
                )),
                CliSettingsApi(session2).update_settings(CliSettings(
                    min_valid_version=20,
                )),
            )

    async with await mongo.async_client.start_session() as session:
        settings = await CliSettingsApi(session).load_settings()
        assert settings.min_valid_version in {10, 20}

        collection = session.client.get_database().cli_settings
        assert 1 == await collection.count_documents({}, session=session)
