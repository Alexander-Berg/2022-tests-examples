import typing as tp
from unittest.mock import MagicMock, AsyncMock, create_autospec

import pytest

from maps.infra.sedem.machine.lib.acceptance_api import AcceptanceApi
from maps.infra.sedem.machine.lib.background_task_api import TasksApi
from maps.infra.sedem.machine.lib.juggler import AsyncJugglerEventsApi
from maps.infra.sedem.machine.lib.lock import MongoLock
from maps.infra.sedem.machine.lib.release_api import ReleaseApi
from maps.infra.sedem.machine.lib.job_manager import JobManager
from maps.infra.sedem.machine.lib.config_api import ServiceConfigApi
from maps.infra.sedem.machine.lib.deploy_api import DeployApi
from maps.infra.sedem.machine.lib.sandbox import AcceptanceSandboxClient
from maps.infra.sedem.machine.lib.startrek import StartrekClient


class MockJobManager(JobManager):
    def __init__(self):
        self.mongo = MagicMock()
        self.acceptance_sandbox_client = create_autospec(AcceptanceSandboxClient, instance=True)
        self.startrek = create_autospec(StartrekClient, instance=True)
        self.juggler = create_autospec(AsyncJugglerEventsApi, instance=True)

    @property
    def client(self):
        mock = AsyncMock()
        mock.start_session = AsyncMock()
        return mock


class MockApi:
    def __init__(self, api: tp.Type):
        self.api = create_autospec(api, instance=True)

    def __call__(self, *args, **kwargs):
        return self.api

    def __getattr__(self, item):
        return self.api.__getattr__(item)


@pytest.fixture(scope='function', autouse=True)
async def acceptance_api(monkeypatch):
    mock = MockApi(AcceptanceApi)
    monkeypatch.setattr('maps.infra.sedem.machine.lib.job_manager.AcceptanceApi', mock)
    return mock


@pytest.fixture(scope='function', autouse=True)
async def release_api(monkeypatch):
    mock = MockApi(ReleaseApi)
    monkeypatch.setattr('maps.infra.sedem.machine.lib.job_manager.ReleaseApi', mock)
    return mock


@pytest.fixture(scope='function')
async def job_manager(monkeypatch, acceptance_api):
    monkeypatch.setattr('maps.infra.sedem.machine.lib.job_manager.MongoLock', create_autospec(MongoLock))
    return MockJobManager()


@pytest.fixture(scope='function', autouse=True)
async def deploy_api(monkeypatch):
    mock = MockApi(DeployApi)
    monkeypatch.setattr('maps.infra.sedem.machine.lib.job_manager.DeployApi', mock)
    return mock


@pytest.fixture(scope='function', autouse=True)
async def config_api(monkeypatch):
    mock = MockApi(ServiceConfigApi)
    monkeypatch.setattr('maps.infra.sedem.machine.lib.job_manager.ServiceConfigApi', mock)
    return mock


@pytest.fixture(scope='function', autouse=True)
async def task_api(monkeypatch):
    mock = MockApi(TasksApi)
    monkeypatch.setattr('maps.infra.sedem.machine.lib.job_manager.TasksApi', mock)
    return mock
