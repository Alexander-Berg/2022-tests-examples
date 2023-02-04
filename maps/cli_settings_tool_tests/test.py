import asyncio
import logging

import click
import pytest
from click.testing import CliRunner

from maps.infra.sedem.machine import cli_settings
from maps.infra.sedem.machine.lib.cli_settings_api import CliSettingsApi, CliSettings
from maps.infra.sedem.lib.oauth import SEDEM_OAUTH_ENV_VAR
from maps.infra.sedem.machine.tests.typing import MongoFixture, CliSettingsFactory
from maps.pylibs.infrastructure_api.arc.arc_client import ArcClient, Commit


logger = logging.getLogger(__name__)


@pytest.fixture(autouse=True)
def mock_oauth_token(monkeypatch) -> None:
    monkeypatch.setenv(SEDEM_OAUTH_ENV_VAR, 'fake-token')


@pytest.fixture(autouse=True)
def patch_db_uri(monkeypatch, mongo: MongoFixture) -> None:
    monkeypatch.setattr(cli_settings, 'deduce_mongo_uri', lambda **_: mongo.uri)


def invoke_tool_command(*args: str) -> str:
    runner = CliRunner()
    result = runner.invoke(cli_settings.tool, ('-e', 'testing') + args)
    if result.exception:
        raise result.exception
    logger.info(result.output)
    assert result.exit_code == 0
    return click.unstyle(result.output)


def test_show_settings(monkeypatch,
                       cli_settings_factory: CliSettingsFactory) -> None:
    asyncio.run(cli_settings_factory(min_valid_version=42))

    monkeypatch.setattr(
        ArcClient, 'try_get_commit',
        lambda *args, **kwargs: Commit(Oid='oid', SvnRevision=42, Author='john-doe', Message='Commit message'),
    )
    assert invoke_tool_command('show') == (
        'Current CLI settings\n'
        'Minimal valid version: r42 (@john-doe) Commit message\n'
    )


def test_deprecate_version(monkeypatch,
                           mongo: MongoFixture,
                           cli_settings_factory: CliSettingsFactory) -> None:
    asyncio.run(cli_settings_factory(min_valid_version=1))

    monkeypatch.setattr(
        ArcClient, 'try_get_commit',
        lambda *args, **kwargs: Commit(Oid='oid', SvnRevision=42),
    )
    invoke_tool_command('deprecate', '42')

    async def load_settings() -> CliSettings:
        async with await mongo.async_client.start_session() as session:
            return await CliSettingsApi(session).load_settings()

    settings = asyncio.run(load_settings())
    assert settings.min_valid_version == 42


def test_deprecate_invalid_version(monkeypatch) -> None:
    monkeypatch.setattr(
        ArcClient, 'try_get_commit', lambda *args, **kwargs: None,  # non-existing commit
    )
    with pytest.raises(cli_settings.ToolError, match='Commit r42 not found in trunk'):
        invoke_tool_command('deprecate', '42')
