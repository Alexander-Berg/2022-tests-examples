import asyncio
import io
import threading
import typing as tp

import pytest
import requests
from aiohttp import web, ClientTimeout
from aiohttp.test_utils import TestClient

from maps.infra.sedem.client.machine_api import MachineApi
from maps.infra.sedem.client.sedem_api import SedemApi
from maps.infra.sedem.machine.lib.arc import ArcClient, Commit
from maps.infra.sedem.machine.lib.endpoints import sedem, sedem_machine  # noqa import machine endpoints for fixture
from maps.infra.sedem.machine.lib.mongodb import MongoDb
from maps.infra.sedem.machine.tests.typing import (
    CommitFactory,
    MongoFixture,
)


T = tp.TypeVar('T')


class MockArcClient:
    def __init__(self) -> None:
        self._commits = []

    def add_commit(self, commit: Commit) -> None:
        self._commits.append(commit)

    async def try_get_commit(self,
                             commit_hash: tp.Optional[str] = None,
                             trunk_svn_revision: tp.Optional[int] = None,
                             branch_name: tp.Optional[str] = None,
                             tag_name: tp.Optional[str] = None) -> tp.Optional[Commit]:
        assert not (branch_name or tag_name), 'Requesting commit by branch or tag not supported'
        for commit in self._commits:
            if commit.Oid == commit_hash:
                return commit
            if commit.SvnRevision == trunk_svn_revision:
                return commit
        return None


@pytest.fixture(scope='function')
def arc_client() -> MockArcClient:
    return MockArcClient()


@pytest.fixture(scope='function')
def commit_factory(commit_factory: CommitFactory,
                   arc_client: MockArcClient) -> CommitFactory:
    def make_commit(arc_hash: str, *, revision: int = 12345) -> Commit:
        commit = commit_factory(
            arc_hash=arc_hash,
            revision=revision,
        )
        arc_client.add_commit(commit)
        return commit
    return make_commit


@pytest.fixture(scope='function')
async def test_application(test_application: web.Application,
                           arc_client: MockArcClient,
                           mongo: MongoFixture,
                           monkeypatch):
    monkeypatch.setattr(MongoDb, '_db_client', mongo.async_client)
    monkeypatch.setattr(MongoDb, '_db_config', dict(
        hosts=f'localhost:{mongo.port}',
        database=mongo.db_name,
        replicaset=mongo.replica_set,
        user='',
        password='',
    ))
    test_application[MongoDb.APP_KEY] = MongoDb()
    test_application[ArcClient.APP_KEY] = arc_client
    # TODO: add another fake clients
    return test_application


@pytest.fixture(scope='function', autouse=True)
async def mock_machine_clients(monkeypatch, test_client: TestClient) -> None:
    loop = asyncio.get_event_loop()

    def await_in_main_loop(coroutine: tp.Awaitable[T]) -> T:
        return asyncio.run_coroutine_threadsafe(coroutine, loop=loop).result()

    def mock_request(self: tp.Union[MachineApi, SedemApi],
                     method: str,
                     url_suffix: str,
                     **kwargs) -> requests.Response:
        assert threading.current_thread() is not threading.main_thread(), (
            'Missing asyncio.to_thread while using MachineApi/SedemApi method'
        )

        if (timeout := kwargs.get('timeout')) and isinstance(timeout, tuple):
            kwargs['timeout'] = ClientTimeout(
                connect=timeout[0],
                total=timeout[1],
            )

        response = await_in_main_loop(test_client.request(
            method=method,
            path=f'v1/{url_suffix}',
            **kwargs,
        ))
        content = await_in_main_loop(response.read())

        requests_response = requests.Response()
        requests_response.status_code = response.status
        requests_response.raw = io.BytesIO(content)

        return requests_response

    monkeypatch.setattr(MachineApi, 'request', mock_request)
    monkeypatch.setattr(SedemApi, 'request', mock_request)


@pytest.fixture(scope='function')
def machine_api(mock_machine_clients) -> MachineApi:
    return MachineApi('fake-oauth')


@pytest.fixture(scope='function')
def sedem_api(mock_machine_clients) -> SedemApi:
    return SedemApi('fake-oauth')
