import inject
import typing as tp
import urllib.parse
from dataclasses import dataclass, field
from functools import cached_property

import requests
import pymongo

from aiohttp import web
from maps.infra.pycare.test_utils import start_background_jobs, cleanup_background_jobs
from maps.infra.sedem.machine.lib.arc import ArcClient
from maps.infra.sedem.machine.lib.candidate_api import ReleaseCandidate
from maps.infra.sedem.machine.lib.config_api import ServiceConfig
from maps.infra.sedem.machine.lib.endpoints import sedem, sedem_machine
from maps.infra.sedem.machine.lib.event_manager import EventManager
from maps.infra.sedem.machine.lib.job_manager import JobManager
from maps.infra.sedem.machine.lib.mongodb import MongoDb
from maps.infra.sedem.machine.lib.release_api import Release
from maps.infra.sedem.machine.lib.release_manager import ReleaseManager
from maps.infra.sedem.machine.lib.sandbox import HotfixSandboxClient, AcceptanceSandboxClient, ProxySandboxClient
from maps.infra.sedem.machine.lib.startrek import StartrekClient
from maps.infra.sedem.machine.lib.token import Token
from maps.infra.sedem.machine.lib.user_info import UserInfo
from maps.infra.sedem.machine.mongo.initialization import init_mongo
from maps.infra.sedem.proto import sedem_pb2 as machine_proto
from maps.pylibs.fixtures.fixture import FixtureClass
from maps.pylibs.fixtures.mock_session import (
    MockSession, Response, ApiMethodParams, Matchers
)
from maps.pylibs.fixtures.mongo_fixture import MongoFixture, AsyncIOMotorDatabase
from maps.pylibs.fixtures.sandbox.tasks import (
    SedemMachineReleaseHotfixDefinition
)
from maps.pylibs.infrastructure_api.abc.abc import ABC
from maps.pylibs.infrastructure_api.nanny.proto_api import NannyClient


class MockChangelogCollector:
    async def generate_major_changelog(self, **kwargs) -> str:
        return 'Empty changelog for tests'

    async def generate_hotfix_changelog(self, **kwargs) -> str:
        return 'Empty changelog for tests'


class MockApp:

    def __init__(self):
        self.mongo = MongoDb()
        self.arc_client = ArcClient()
        self.proxy_sandbox_client = ProxySandboxClient()

        hotfix_sandbox_client = HotfixSandboxClient()
        acceptance_sandbox_client = AcceptanceSandboxClient()
        abc_client = ABC(oauth_token=Token.get())
        startrek_client = StartrekClient(abc_client)
        changelog_collector = MockChangelogCollector()

        inject.clear_and_configure(lambda binder: binder.bind(NannyClient, NannyClient(oauth_token=Token.get())))

        self.job_manager = JobManager(
            mongo=self.mongo,
            arc_client=self.arc_client,
            startrek_client=startrek_client,
            abc_client=abc_client,
            hotfix_sandbox_client=hotfix_sandbox_client,
            acceptance_sandbox_client=acceptance_sandbox_client,
            changelog_collector=changelog_collector,
        )
        self.release_manager = ReleaseManager(self.mongo)
        self.event_manager = EventManager(
            mongo=self.mongo,
            job_manager=self.job_manager
        )

    def __getitem__(self, item):
        if item == MongoDb.APP_KEY:
            return self.mongo
        if item == ArcClient.APP_KEY:
            return self.arc_client
        if item == ReleaseManager.APP_KEY:
            return self.release_manager
        if item == EventManager.APP_KEY:
            return self.event_manager
        if item == JobManager.APP_KEY:
            return self.job_manager
        if item == ProxySandboxClient.APP_KEY:
            return self.proxy_sandbox_client
        raise KeyError(item)


@dataclass
class MockWebRequest:
    body: bytes
    query: tp.Dict[str, str]
    app: tp.Optional[MockApp] = None
    match_info: tp.Dict[str, str] = field(default_factory=dict)
    headers: tp.Dict[str, str] = field(default_factory=dict)

    @staticmethod
    def from_request_kwargs(request_kwargs: tp.Dict[str, tp.Any]) -> 'MockWebRequest':
        request_params = request_kwargs['params']
        query_string = urllib.parse.urlencode(request_params)
        query = dict(urllib.parse.parse_qsl(query_string))
        request_data = request_kwargs.get('data')
        request_headers = request_kwargs.get('headers')
        return MockWebRequest(
            body=request_data,
            headers=request_headers,
            query=query
        )

    async def read(self) -> bytes:
        return self.body


async def mock_user_info(request: MockWebRequest) -> UserInfo:
    if 'Authorization' not in request.headers:
        raise web.HTTPUnauthorized()
    return UserInfo(uid='1120000000052721', login='robot-maps-sandbox', has_bug_icon=False)


class MachineFixture(FixtureClass):
    """
        Sedem Machine fixture class for emulating accesses to Sedem API.
    """
    @dataclass
    class Storage:
        latest_cli_version: int = 0
        running: bool = True

        @cached_property
        def app(self):
            return MockApp()

    machine_storage: 'MachineFixture.Storage'

    def mongo(self) -> MongoFixture:
        return self._instance_of(MongoFixture)

    def unittest_mongo_config(self) -> tp.Dict[str, str]:
        mongo_storage = self.mongo().mongo_storage
        return dict(
            hosts=f'localhost:{mongo_storage.mongo_port}',
            database=mongo_storage.mongo_db,
            replicaSet=mongo_storage.mongo_rs,
            user='',
            password='',
        )

    def run_until_complete(self, coro: tp.Coroutine) -> tp.Any:
        mongo_storage = self.mongo().mongo_storage
        return mongo_storage.run_until_complete(coro)

    @staticmethod
    def invoke_machine_handle(machine_handle, api_params: ApiMethodParams) -> Response:
        machine_storage: MachineFixture.Storage = api_params.storage[MachineFixture]
        mongo_storage: MongoFixture.Storage = api_params.storage[MongoFixture]

        if not machine_storage.running:
            raise requests.exceptions.ConnectionError()

        request = MockWebRequest.from_request_kwargs(api_params.request_kwargs)
        request.app = machine_storage.app
        response = mongo_storage.run_until_complete(machine_handle(request))
        if response.status >= 400:
            return Response(text=response.text, status_code=response.status)
        return Response(content=response.body, status_code=response.status)

    @staticmethod
    def wait_for_release_create_background_jobs(api_params: ApiMethodParams) -> None:
        mongo_storage: MongoFixture.Storage = api_params.storage[MongoFixture]
        machine_storage: MachineFixture.Storage = api_params.storage[MachineFixture]
        app = machine_storage.app

        async def wait_jobs():
            app.job_manager.complete_major_releases_job.run_soon()
            await app.job_manager.complete_major_releases_job.wait_next_iteration()
            app.job_manager.generate_major_changelog_job.run_soon()
            await app.job_manager.generate_major_changelog_job.wait_next_iteration()
        mongo_storage.run_until_complete(wait_jobs())

    @staticmethod
    def wait_for_release_hotfix_background_jobs(api_params: ApiMethodParams) -> None:
        mongo_storage: MongoFixture.Storage = api_params.storage[MongoFixture]
        machine_storage: MachineFixture.Storage = api_params.storage[MachineFixture]
        app = machine_storage.app

        async def wait_jobs():
            await app.job_manager.merge_hotfixes_job.wait_next_iteration()
            await app.job_manager.start_drafted_tasks_job.wait_next_iteration()
            await app.job_manager.handle_events_job.wait_next_iteration()
            if SedemMachineReleaseHotfixDefinition.successful_execution:
                await app.job_manager.handle_events_job.wait_next_iteration()
        mongo_storage.run_until_complete(wait_jobs())

    @staticmethod
    def wait_for_release_deploy_background_jobs(api_params: ApiMethodParams) -> None:
        mongo_storage: MongoFixture.Storage = api_params.storage[MongoFixture]
        machine_storage: MachineFixture.Storage = api_params.storage[MachineFixture]
        app = machine_storage.app

        async def wait_jobs():
            app.job_manager.create_approval_forms_job.run_soon()
            await app.job_manager.create_approval_forms_job.wait_next_iteration()
        mongo_storage.run_until_complete(wait_jobs())

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/configuration/update'))
    def configuration_update(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/configuration/update'"""
        assert api_params.method == 'POST'
        assert api_params.request_kwargs['headers']['host'] == 'core-sedem.maps.yandex.net'
        return MachineFixture.invoke_machine_handle(sedem.configuration_update, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/candidate/list'))
    def release_candidate_list(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/candidate/list'"""
        assert api_params.method == 'GET'
        result = MachineFixture.invoke_machine_handle(sedem_machine.release_candidate_list, api_params=api_params)
        return result

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/candidate/lookup'))
    def candidate_lookup(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/candidate/lookup'"""
        assert api_params.method == 'GET'
        result = MachineFixture.invoke_machine_handle(sedem_machine.candidate_lookup, api_params=api_params)
        return result

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/configuration/get'))
    def configuration_get(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/configuration/get'"""
        assert api_params.method == 'GET'
        return MachineFixture.invoke_machine_handle(sedem_machine.configuration_get, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/create'))
    def release_create(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/release/create'"""
        assert api_params.method == 'POST'
        response = MachineFixture.invoke_machine_handle(sedem_machine.release_create, api_params=api_params)
        if response.status_code < 400:
            MachineFixture.wait_for_release_create_background_jobs(api_params)
        return response

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/lookup'))
    def release_lookup(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/release/lookup'"""
        assert api_params.method == 'GET'
        return MachineFixture.invoke_machine_handle(sedem_machine.release_lookup, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/hotfix'))
    def release_hotfix(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/release/hotfix'"""
        assert api_params.method == 'POST'
        response = MachineFixture.invoke_machine_handle(sedem_machine.release_hotfix, api_params=api_params)
        if response.status_code < 400:
            MachineFixture.wait_for_release_hotfix_background_jobs(api_params)
        return response

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/get'))
    def release_get(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/release/get'"""
        assert api_params.method == 'GET'
        return MachineFixture.invoke_machine_handle(sedem_machine.release_get, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/deploy_validate'))
    def release_deploy_validate(api_params: ApiMethodParams) -> Response:
        """Represents method v1/release/deploy_validate"""
        assert api_params.method == 'POST'
        return MachineFixture.invoke_machine_handle(sedem_machine.release_deploy_validate, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/deploy_prepare'))
    def release_deploy_prepare(api_params: ApiMethodParams) -> Response:
        """Represents method v1/release/deploy_validate"""
        assert api_params.method == 'POST'
        response = MachineFixture.invoke_machine_handle(sedem_machine.release_deploy_prepare, api_params=api_params)
        MachineFixture.wait_for_release_deploy_background_jobs(api_params)
        return response

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/deploy_commit'))
    def release_deploy_commit(api_params: ApiMethodParams) -> Response:
        """Represents method v1/release/deploy_validate"""
        assert api_params.method == 'POST'
        return MachineFixture.invoke_machine_handle(sedem_machine.release_deploy_commit, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/set_message'))
    def release_set_message(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/release/set_message'"""
        assert api_params.method == 'POST'
        return MachineFixture.invoke_machine_handle(sedem_machine.release_set_message, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/release/reject'))
    def release_reject(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/release/reject'"""
        assert api_params.method == 'POST'
        return MachineFixture.invoke_machine_handle(sedem_machine.release_reject, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/sandbox_template/create'))
    def sandbox_template_create(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/sandbox_template/create'"""
        assert api_params.method == 'POST'
        return MachineFixture.invoke_machine_handle(sedem_machine.sandbox_template_create, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/events/hotfix'))
    def events_hotfix(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/events/hotfix'"""
        assert api_params.method == 'POST'
        assert api_params.request_kwargs['headers']['host'] == 'core-sedem.maps.yandex.net'
        return MachineFixture.invoke_machine_handle(sedem.events_hotfix, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/events/build'))
    def events_build(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/events/build'"""
        assert api_params.method == 'POST'
        assert api_params.request_kwargs['headers']['host'] == 'core-sedem.maps.yandex.net'
        return MachineFixture.invoke_machine_handle(sedem.events_build, api_params=api_params)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.regex(r'.+core-sedem-machine.maps.yandex.net/v1/cli/info'))
    def cli_info(api_params: ApiMethodParams) -> Response:
        """Represents method 'v1/cli/info'"""
        assert api_params.method == 'GET'
        machine_storage: MachineFixture.Storage = api_params.storage[MachineFixture]

        if not machine_storage.running:
            raise requests.exceptions.ConnectionError()

        request_params = api_params.request_kwargs['params']
        version = int(request_params['version'])
        latest_cli_version = machine_storage.latest_cli_version

        response = machine_proto.CliInfoResponse()
        response.latest_version = latest_cli_version
        response.need_update = version < latest_cli_version
        content = response.SerializeToString()
        return Response(content=content)

    def get_app(self) -> MockApp:
        return self.machine_storage.app

    def get_db_instance(self) -> AsyncIOMotorDatabase:
        app = self.get_app()
        mongo = app[MongoDb.APP_KEY]
        return mongo.db_instance()

    def set_latest_cli_version(self, version: int) -> None:
        self.machine_storage.latest_cli_version = version

    def shut_down(self) -> None:
        self.machine_storage.running = False

    def start_up(self) -> None:
        self.machine_storage.running = True

    def add_service_config(self, config: ServiceConfig) -> None:
        db = self.get_db_instance()
        self.run_until_complete(db.service_config.insert_one(config.to_mongo()))

    def add_candidate(self, candidate: ReleaseCandidate) -> None:
        db = self.get_db_instance()
        self.run_until_complete(db.release_candidate.insert_one(candidate.to_mongo()))

    def add_release(self, release: Release) -> None:
        db = self.get_db_instance()
        self.run_until_complete(db.release.insert_one(release.to_mongo()))

    def releases(self, service_name: str) -> list[Release]:
        db = self.get_db_instance()

        async def fetch_releases(db) -> list[Release]:
            release_docs = await (
                db.release
                    .find({'service_name': service_name})
                    .sort((
                        ('major', pymongo.DESCENDING),
                        ('minor', pymongo.DESCENDING)
                    ))
                    .to_list(length=None)
            )
            return [
                Release.build_from_mongo(release_doc)
                for release_doc in release_docs
            ]

        return self.run_until_complete(fetch_releases(db))

    def wait_for_approval_update(self):
        app = self.machine_storage.app

        async def wait_jobs(app):
            app.job_manager.update_approvals_job.run_soon()
            await app.job_manager.update_approvals_job.wait_next_iteration()
        self.run_until_complete(wait_jobs(app))

    def wait_for_deploy_status_update(self):
        app = self.machine_storage.app

        async def wait_jobs(app):
            app.job_manager.update_deploy_statuses_job.run_soon()
            await app.job_manager.update_deploy_statuses_job.wait_next_iteration()

        self.run_until_complete(wait_jobs(app))

    @staticmethod
    def enable(fixture_factory, mock_session, monkeypatch) -> 'MachineFixture':
        fixture_factory(MongoFixture)  # ensure mongo mocked before creation of machine fixture
        fixture = MachineFixture(storage=mock_session.storage)

        monkeypatch.setattr(MongoDb, '_db_config', fixture.unittest_mongo_config())
        monkeypatch.setattr(MongoDb, '_db_client', fixture.get_db_instance().client)
        monkeypatch.setattr(UserInfo, 'from_oauth', mock_user_info)
        init_mongo(mongo_uri=MongoDb.connection_string())

        fixture.run_until_complete(start_background_jobs())
        return fixture

    def disable(self) -> None:
        self.run_until_complete(cleanup_background_jobs())
