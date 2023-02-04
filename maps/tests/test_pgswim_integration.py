from unittest.mock import Mock

import aiohttp
import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.common.lasagna import DbAutomigrationError, Lasagna

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def migrator_mock(mocker):
    return mocker.patch(
        "smb.common.pgswim.lib.migrator.migrator.Migrator.upgrade",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def run_app_mock(mocker):
    mocker.patch("aiohttp.web.run_app", lambda a, *args, **kwargs: a)


class SampleApp(Lasagna):
    async def _setup_layers(self, db):
        return aiohttp.web.Application()


async def test_uses_defined_swim_engine_cls(loop, migrator_mock):
    mock = Mock()

    class Engine:
        @classmethod
        async def create(cls, *args):
            return mock(*args)

    class App(SampleApp):
        SWIM_ENGINE_CLS = Engine

    app = App({"DATABASE_URL": "url"})
    await app.run()

    mock.assert_called_with("url")


@pytest.mark.parametrize("automigration_params", [{}, {"DB_AUTOMIGRATE": True}])
async def test_autoupgrades_db(automigration_params, loop, migrator_mock):
    mock = Mock()

    class Engine:
        @classmethod
        async def create(cls, *args):
            return mock(*args)

    class App(SampleApp):
        SWIM_ENGINE_CLS = Engine
        MIGRATIONS_PATH = "path"

    app = App({"DATABASE_URL": "url", **automigration_params})
    await app.run()

    migrator_mock.assert_called_once()


@pytest.mark.parametrize("migration_path", [None, ""])
@pytest.mark.parametrize("automigration_params", [{}, {"DB_AUTOMIGRATE": True}])
async def test_raises_if_migration_path_not_configured(
    automigration_params, migration_path, loop, migrator_mock
):
    mock = Mock()

    class Engine:
        @classmethod
        async def create(cls, *args):
            return mock(*args)

    class App(SampleApp):
        SWIM_ENGINE_CLS = Engine
        MIGRATIONS_PATH = migration_path

    app = App({"DATABASE_URL": "url", **automigration_params})

    with pytest.raises(
        DbAutomigrationError, match="Migrations path is not configured."
    ):
        await app.run()


@pytest.mark.parametrize("migration_path", [None, "path"])
async def test_does_not_autoupgrade_db_if_automigrate_is_false(
    migration_path, loop, migrator_mock
):
    mock = Mock()

    class Engine:
        @classmethod
        async def create(cls, *args):
            return mock(*args)

    class App(SampleApp):
        SWIM_ENGINE_CLS = Engine
        MIGRATIONS_PATH = migration_path

    app = App({"DATABASE_URL": "url", "DB_AUTOMIGRATE": False})
    await app.run()

    migrator_mock.assert_not_called()


@pytest.mark.parametrize("migration_path", [None, "path"])
async def test_works_correctly_if_swim_engine_is_none(
    migration_path, loop, migrator_mock
):
    mock = Mock()

    class App(Lasagna):
        SWIM_ENGINE_CLS = None
        MIGRATIONS_PATH = migration_path

        async def _setup_layers(self, db):
            mock(db)
            return aiohttp.web.Application()

    app = App({})
    await app.run()

    mock.assert_called_with(None)
    migrator_mock.assert_not_called()
