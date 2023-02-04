import asyncio
from asyncio.runners import _cancel_all_tasks

import pytest
from lazy import lazy

from maps.infra.sedem.cli.modules.api import global_api
from maps.infra.sedem.cli.lib import utils
from maps.infra.sedem.cli.lib.release import layout, deprecation
from maps.infra.sedem.cli.lib.release.releasers import meta
from maps.infra.sedem.cli.tests.release.fixtures.rendering_fixture import RenderingFixture
from maps.infra.sedem.lib.oauth import SEDEM_OAUTH_ENV_VAR
from maps.infra.sedem.common.release.vcs_version import ArcClient
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.pylibs.fixtures.conftest import fixture_factory  # noqa


def patched_asyncio_run(main):
    if not asyncio.iscoroutine(main):
        raise ValueError("a coroutine was expected, got {!r}".format(main))

    try:
        loop = asyncio.get_event_loop()
        if loop.is_closed():
            raise RuntimeError()
        loop_created = False
    except RuntimeError:
        loop = asyncio.new_event_loop()
        loop_created = True

    try:
        return loop.run_until_complete(main)
    finally:
        if loop_created:
            try:
                _cancel_all_tasks(loop)
                loop.run_until_complete(loop.shutdown_asyncgens())
            finally:
                asyncio.set_event_loop(None)
                loop.close()


@pytest.fixture(scope='function', autouse=True)
def asyncio_run(monkeypatch):
    monkeypatch.setattr(asyncio, 'run', patched_asyncio_run)


@pytest.fixture(scope='function', autouse=True)
def sedem_fixture(monkeypatch):
    monkeypatch.setattr(utils, 'ya_owner', lambda path: None)  # Freezes tests to death.
    monkeypatch.setattr(ArcClient, '_cache', dict())
    monkeypatch.setenv(SEDEM_OAUTH_ENV_VAR, 'fake-secret')
    deprecation.cli_info.cache_clear()


@pytest.fixture(scope='function', autouse=True)
def vcs_fixture(monkeypatch, fixture_factory):  # noqa: F811
    fixture_factory(ArcadiaFixture)  # ensure arcadia mocked
    lazy.invalidate(global_api, 'arc')  # arc client is mocked by stubbing globals used in its constructor


@pytest.fixture(scope='function', autouse=True)
def rendering_fixture(monkeypatch):
    fixture = RenderingFixture()
    monkeypatch.setattr(layout, 'render_tables', fixture.render_tables)
    monkeypatch.setattr(meta, 'render_tables', fixture.render_tables)
    return fixture
