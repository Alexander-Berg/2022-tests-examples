import enum
import json
import typing as tp
from dataclasses import asdict, dataclass, field, MISSING
from datetime import datetime

from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.pylibs.fixtures.fixture import FixtureClass
from maps.pylibs.fixtures.mock_session import (
    MockSession, Response, ApiMethodParams, Matchers
)
from maps.pylibs.fixtures.sandbox.base_task import TBaseTask


class JobSentinels(enum.Enum):
    revision = enum.auto()


@dataclass
class TestResult:

    class Status(enum.EnumMeta):
        OK = 'OK'
        NOT_CHECKED = 'NOT_CHECKED'

    task_id: tp.Union[int, str] = ""  # TestEnv returns empty string if there is no value :(
    revision: int = field(default_factory=lambda: MISSING)
    status: str = field(default=Status.OK)


@dataclass
class TestenvJob(tp.Generic[TBaseTask]):
    task: tp.Type[TBaseTask]
    results: tp.Dict[int, TestResult] = field(default_factory=dict)
    params: tp.Dict[str, str] = field(default_factory=dict)


@dataclass
class Database:
    jobs: tp.Dict[str, TestenvJob] = field(default_factory=dict)
    is_started: bool = False
    started_at: str = field(default_factory=lambda: datetime.now().isoformat())
    restricted: bool = False


RESULTS_LIMIT = 1000000  # default TestEnv limit


class TestenvFixture(FixtureClass):
    """
        Testenv fixture class for emulating accesses to Testenv API.
    """
    @dataclass
    class Storage:
        databases: tp.Dict[str, Database] = field(default_factory=dict)

    testenv_storage: 'TestenvFixture.Storage'

    def database(self, database_name: str) -> tp.Optional[Database]:
        return self.testenv_storage.databases.get(database_name, None)

    def add_databases(self, **databases: Database) -> None:
        self.testenv_storage.databases.update(databases)

    def restrict_access_to_database(self, database_name: str) -> None:
        assert database_name in self.testenv_storage.databases, f'No such database: {database_name}'
        self.testenv_storage.databases[database_name].restricted = True

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.endswith('testenv.yandex-team.ru/handlers/startdb'))
    def startdb(api_params: ApiMethodParams) -> Response:
        assert api_params.method == 'POST'

        testenv_storage: TestenvFixture.Storage = api_params.storage[TestenvFixture]

        database_name = api_params.request_kwargs['data']['database']

        database = testenv_storage.databases.get(database_name, None)
        if not database:
            return Response(text='Database not found', status_code=404)

        if database.restricted:
            return Response(text='You don\'t have access to this method.', status_code=403)

        if database.is_started:
            return Response(text='Database is started already')

        database.is_started = True
        database.started_at = datetime.now().isoformat()
        return Response(text='Database started successfully')

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.endswith('testenv.yandex-team.ru/handlers/getTests'))
    def get_tests(api_params: ApiMethodParams) -> Response:
        assert api_params.method == 'GET'
        params = api_params.request_kwargs['params']
        if 'database' not in params:
            return Response('Missing parameters: database', status_code=404)
        database_name = params['database']

        testenv_storage: TestenvFixture.Storage = api_params.storage[TestenvFixture]

        database = testenv_storage.databases.get(database_name, None)
        if not database:
            return Response(text='Database not found', status_code=404)
        return Response(','.join(database.jobs), status_code=200)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.endswith('testenv.yandex-team.ru/handlers/runJob'))
    def run_job(api_params: ApiMethodParams) -> Response:
        assert api_params.method == 'POST'
        params = api_params.request_kwargs['data']
        assert {'database', 'job_name', 'revisions'} == set(params)
        database_name = params['database']
        job_name = params['job_name']
        revision = int(params['revisions'])

        testenv_storage: TestenvFixture.Storage = api_params.storage[TestenvFixture]
        arcadia_fixture = TestenvFixture._get_instance_of(api_params.storage, fixture_type=ArcadiaFixture)

        if database_name not in testenv_storage.databases:
            return Response(f'cannot find database "{database_name}"', status_code=404)

        database = testenv_storage.databases[database_name]
        if database.restricted:
            return Response(text='You don\'t have access to this method.', status_code=403)

        if not arcadia_fixture.is_trunk_revision(revision):
            # Status code is strange: TESTENV-3993
            return Response(f'cannot find revision {revision} in db', status_code=500)

        assert job_name in database.jobs
        job = database.jobs[job_name]
        for param_name, param_value in job.params.items():
            if param_value == JobSentinels.revision:
                job.params[param_name] = str(revision)
        sandbox_task = job.task(parameters=job.task.Parameters(**job.params))
        sandbox_task.on_execute()
        test_result = TestResult(task_id=sandbox_task.id, revision=revision, status=TestResult.Status.OK)
        job.results[revision] = test_result
        return Response('', status_code=200)

    @staticmethod
    @MockSession.register_api(
        url_matcher=Matchers.endswith('testenv.yandex-team.ru/handlers/grids/testResults'))
    def test_results(api_params: ApiMethodParams) -> Response:
        assert api_params.method == 'GET'
        params = api_params.request_kwargs['params']
        if 'database' not in params:
            return Response('Missing parameters: database', status_code=404)
        database: str = params['database']
        test_name: tp.Optional[str] = params.get('test_name')
        maybe_revision: tp.Optional[str] = params.get('revision')
        revision: tp.Optional[int] = None
        if maybe_revision:
            revision = int(maybe_revision)
        # TODO: Raise timeout error if no revision is passed?

        testenv_storage: TestenvFixture.Storage = api_params.storage[TestenvFixture]
        jobs = testenv_storage.databases[database].jobs
        found_jobs = jobs
        if test_name:
            if test_name not in jobs:
                found_jobs = []
            else:
                found_jobs = [jobs[test_name]]

        results = []
        for job in found_jobs:
            if revision:
                not_checked = TestResult(revision=revision, status=TestResult.Status.NOT_CHECKED)
                result = job.results.get(revision, not_checked)
                results.append(result)
            else:
                last_checked_revision = max(job.results)
                for revision in range(1, last_checked_revision + 1):
                    not_checked = TestResult(revision=revision, status=TestResult.Status.NOT_CHECKED)
                    result = job.results.get(revision, not_checked)
                    results.append(result)

        response = {
            'rows': [asdict(result) for result in results[:RESULTS_LIMIT]],
            'results': RESULTS_LIMIT
        }
        return Response(json.dumps(response))
