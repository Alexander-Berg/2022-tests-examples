from collections import defaultdict
import datetime
from unittest import mock
import mongomock
import queue
import time
import pytest
import pytz

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType

from maps.garden.libs_server.graph.compound_callback import CompoundCallback
from maps.garden.libs_server.common.scheduler import Scheduler, SchedulerAdaptor
from maps.garden.libs_server.resource_storage.storage import ResourceStorage
from maps.garden.libs_server.resource_storage.deferred_removal import DeferredResourcesRemoverProcess
from maps.garden.libs_server.graph.request_storage import (
    RequestStatusString, RequestStatus, Request, FailedTask)
from maps.garden.libs_server.application import state
from maps.garden.libs_server.build.build_manager import BuildManager
from maps.garden.libs_server.build.build_defs import BuildStatusString
from maps.garden.libs_server.common.errors import (
    DuplicateReleaseNameException, ConflictException)
from maps.garden.libs_server.log_storage.module_event_storage import ModuleEventStorage
from maps.garden.libs_server.test_utils.module_mocks import ModuleManagerMock
from maps.garden.libs_server.test_utils.task_handler_stubs import EnvironmentSettingsProviderSimple

from maps.garden.scheduler.lib.builds_manipulator import ResourcesTransition
from maps.garden.scheduler.lib.builds_scheduler import BuildsScheduler
from maps.garden.scheduler.lib.event_bus import EventBus, Event
from maps.garden.scheduler.lib.graph_manager import GraphManager

import logging
logger = logging.getLogger("garden.server")


class MockRequestHandler:
    def __init__(self):
        self._requests = {}
        self._on_request_status_changed = CompoundCallback()
        self._failed = []
        self._completed = []
        self._next_request_id = 0
        self._fail_on_scheduling = False
        self._has_failed_tasks = False

    def update_progress(self):
        for r in self._requests.values():
            if r.id in self._failed:
                r.status.string = RequestStatusString.FAILED
                self.on_request_status_changed(r)
            elif r.id in self._completed:
                r.status.string = RequestStatusString.COMPLETED
                self.on_request_status_changed(r)
            elif self._has_failed_tasks:
                r.status.time_of_first_error = datetime.datetime.now(pytz.utc)
                r.status.failed_tasks = [FailedTask(error=b'')]
                self.on_request_status_changed(r)

    def complete(self, request_id):
        if request_id in self._requests:
            self._completed.append(request_id)
        else:
            raise Exception("Unknown request {0}".format(request_id))

    def fail(self, request_id):
        if request_id in self._requests:
            self._failed.append(request_id)
        else:
            raise Exception("Unknown request {0}".format(request_id))

    def fail_on_scheduling(self, value):
        self._fail_on_scheduling = value

    def has_failed_tasks(self, value):
        self._has_failed_tasks = value

    @property
    def on_request_status_changed(self):
        return self._on_request_status_changed

    def startup_done(self):
        pass

    def status(self, request_id):
        r = self._requests.get(request_id, None)
        if r is None:
            return None
        return r.status

    def get_field(self, request_id, field):
        if field == "status":
            return self.status(request_id)
        raise NotImplementedError("No mock handler for field {}".format(field))

    def output_resources_keys(self, request_id):
        return set()

    def handle(
            self, input_name_to_version, target_names,
            lock_callback=None,
            module_name=None, build_id=None, module_version=None,
            contour_name=None):
        if self._fail_on_scheduling:
            raise Exception("Expected exception on scheduling")
        request = Request(
            status=RequestStatus(string=RequestStatusString.IN_PROGRESS))

        request.id = self._next_request_id
        request.status.start_time = datetime.datetime.now(pytz.utc)
        self._next_request_id += 1
        self._requests[request.id] = request

        if lock_callback:
            lock_callback(request.id, set())

        self.on_request_status_changed(request)

        return request.id

    def cancel(self, request_id):
        if request_id in self._requests:
            self._requests[request_id].status.string = RequestStatusString.CANCELLED

    def forget(self, request_id):
        time.sleep(0.1)
        if request_id in self._requests:
            del self._requests[request_id]


class ThreadExecutorMock:
    """
    This class is needed to avoid background threads and use of sleeps in tests.
    """
    def __init__(self, name: str, **kwargs):
        self.tasks = queue.Queue()

    def stop(self):
        pass

    def __call__(self, cmd, task_weight=None):
        self.tasks.put(cmd)

    def _process_one_task(self):
        task = self.tasks.get()
        try:
            task()
        except Exception:
            logger.exception("Background task failed")
        finally:
            self.tasks.task_done()

    def process_all_tasks(self):
        while not self.tasks.empty():
            self._process_one_task()


class GardenMock:
    def __init__(self):
        self.failed_builds = []

    def start(self, db):
        state._globals.module_manager = ModuleManagerMock(
            defaultdict(lambda: ModuleTraits(name="name", type=ModuleType.MAP))
        )

        state._globals.event_bus = EventBus()
        state._globals.event_bus.add_callback(
            Event.BUILD_FAILED,
            lambda build: self.failed_builds.append(build),
        )
        state._globals.module_event_storage = ModuleEventStorage(db)

        self._scheduler = Scheduler()
        self._delay_executor = SchedulerAdaptor(self._scheduler)

        self.request_handler = MockRequestHandler()
        state._globals.request_storage = self.request_handler
        state._globals.request_handler = self.request_handler

        graph_manager = GraphManager(state.module_manager(), regions=[("cis1", "yandex")])

        state._globals.build_manager = BuildManager(
            db,
            module_event_storage=state._globals.module_event_storage,
        )

        environment_settings_provider = EnvironmentSettingsProviderSimple(environment_settings={})

        state._globals.storage = ResourceStorage(
            db,
            environment_settings_provider=environment_settings_provider,
        )
        self.deffered_remover = DeferredResourcesRemoverProcess(
            db,
            server_settings={"deferred_removal": {"interval": 600, "give_up_timeout": 864000}},
            environment_settings_provider=environment_settings_provider,
            delay_executor=self._delay_executor,
        )
        self.builds_scheduler = BuildsScheduler(graph_manager, self._delay_executor)

        state._globals.notification_manager = mock.Mock()

        self._scheduler.__enter__()
        self.builds_scheduler.__enter__()
        self.deffered_remover.__enter__()

    def stop(self):
        self.builds_scheduler.__exit__(None, None, None)
        self._scheduler.__exit__(None, None, None)
        self.deffered_remover.__exit__(None, None, None)


class Helper:
    def __init__(self):
        self._db = mongomock.MongoClient(tz_aware=True).db

        self._garden = GardenMock()
        self._garden.start(self._db)
        self.process_all_tasks()

    def restart_server(self):
        """
        Immitates restart of the garden server
        """
        self._garden.stop()
        self._garden = GardenMock()
        self._garden.start(self._db)
        self.process_all_tasks()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self._garden.stop()

    @property
    def request_handler(self):
        return self._garden.request_handler

    def get_mongo_builds_count(self):
        """
        Returns number of builds stored in mongo
        """
        return self._db.builds.count_documents({})

    def process_all_tasks(self):
        self._garden.builds_scheduler._process_build_actions()
        self._garden.builds_scheduler._executor.process_all_tasks()

    def complete_tasks(self, request_id):
        self.process_all_tasks()
        self.request_handler.complete(request_id)
        self.request_handler.update_progress()
        self.process_all_tasks()

    def fail_tasks(self, request_id):
        self.process_all_tasks()
        self.request_handler.fail(request_id)
        self.request_handler.update_progress()
        self.process_all_tasks()

    def add_build(
        self,
        name,
        username="me",
        sources=[],
        extras=None,
        contour_name=None
    ):
        return state.build_manager().add(
            name=name,
            contour_name=(contour_name or "contour_name"),
            username=username,
            sources=sources,
            extras=extras or {},
            module_version="test"
        )

    def get_failed_builds(self):
        return self._garden.failed_builds

    def wait_for_build_removal(self, expected_builds_count):
        self._garden.builds_scheduler._builds_remover._executor.process_all_tasks()
        assert self.get_mongo_builds_count() == expected_builds_count


@pytest.fixture
def helper(mocker):
    mocker.patch("maps.garden.libs_server.common.thread_executor.ThreadExecutor", new=ThreadExecutorMock)
    mocker.patch("maps.garden.scheduler.lib.builds_remover.BuildRemoverExecutor", new=ThreadExecutorMock)
    mocker.patch("maps.garden.scheduler.lib.builds_manipulator.BuildsManipulator._get_resources_transitions", return_value=[ResourcesTransition(set(), set())])

    with Helper() as helper:
        yield helper


def test_get_build_by_request_id(helper):
    """
    Test retrieving of build by request id
    """
    b = helper.add_build(name="test")
    helper.process_all_tasks()
    assert helper.get_mongo_builds_count() == 1

    build = state.build_manager().find(b.name, b.id)
    assert build.request_id is not None
    check = state.build_manager().get_build_by_request_id(build.request_id)
    assert build == check


def test_builds_operations(helper):
    """
    Test basic operations functionality of Builds
    """
    assert helper.get_mongo_builds_count() == 0

    b = helper.add_build(name="test")

    helper.process_all_tasks()
    assert helper.get_mongo_builds_count() == 1

    state.build_manager().restart(b)
    helper.process_all_tasks()
    assert helper.get_mongo_builds_count() == 1

    state.build_manager().cancel(b)
    helper.process_all_tasks()
    assert helper.get_mongo_builds_count() == 1

    state.build_manager().remove(b)
    helper.process_all_tasks()

    helper.wait_for_build_removal(expected_builds_count=0)


def test_build_release_name_duplicates(helper):
    """
    Tests that existing build with same name and release_name
    is not modified
    """

    helper.add_build(name="test", extras={"release_name": "1"}, contour_name="test1")
    helper.add_build(name="test", extras={"release_name": "1"}, contour_name="test2")

    helper.process_all_tasks()
    assert helper.get_mongo_builds_count() == 2

    with pytest.raises(DuplicateReleaseNameException):
        helper.add_build(name="test", username="another me", extras={"release_name": "1"}, contour_name="test1")

    assert helper.get_mongo_builds_count() == 2


def test_removed_build_cannot_be_restarted(helper):
    """
    Tests that all operations added after 'remove' operation
    are handled gracefully
    """
    b = helper.add_build(name="test")

    helper.process_all_tasks()

    state.build_manager().remove(b)

    with pytest.raises(ConflictException):
        state.build_manager().restart(build=b, username=None)

    helper.process_all_tasks()

    helper.wait_for_build_removal(expected_builds_count=0)


def test_remove_after_restart(helper):
    """
    Tests that queued 'remove' operations are performed after restart
    """
    b = helper.add_build(name="test")
    helper.process_all_tasks()
    state.build_manager().remove(b)
    helper.restart_server()
    assert helper.get_mongo_builds_count() == 1
    helper.process_all_tasks()

    helper.wait_for_build_removal(expected_builds_count=0)


def test_completed_build(helper):
    """
    Tests builds that should succeed
    """
    build = helper.add_build(name="test")
    helper.process_all_tasks()
    build = state.build_manager().find(build.name, build.id)
    assert build.request_id is not None
    helper.complete_tasks(build.request_id)
    build = state.build_manager().find(build.name, build.id)
    assert build.status.string == RequestStatusString.COMPLETED
    assert helper.get_mongo_builds_count() == 1


def test_failed_build(helper):
    """
    Tests builds that should fail
    """
    build = helper.add_build(name="test")
    helper.process_all_tasks()
    build = state.build_manager().find(build.name, build.id)
    assert build.request_id is not None
    helper.fail_tasks(build.request_id)
    build = state.build_manager().find(build.name, build.id)
    assert build.status.string == RequestStatusString.FAILED
    assert helper.get_mongo_builds_count() == 1

    assert [b.id for b in helper.get_failed_builds()] == [build.id]


def test_schedule_after_restart(helper):
    """
    Tests that queued 'create' operations are performed after restart
    """
    b1 = helper.add_build(name="test")
    b2 = helper.add_build(name="test2")
    b3 = helper.add_build(name="test3")
    helper.restart_server()

    assert helper.get_mongo_builds_count() == 3
    for build in [b1, b2, b3]:
        build = state.build_manager().find(build.name, build.id)
        assert build.status.string == BuildStatusString.IN_PROGRESS
        helper.complete_tasks(build.request_id)
        build = state.build_manager().find(build.name, build.id)
        assert build.status.string == BuildStatusString.COMPLETED
    assert helper.get_mongo_builds_count() == 3


def test_completed_and_failed_builds_are_not_restarted(helper):
    """
    Tests that 'failed' and 'completed' builds are not restarted after server restart
    """
    b1 = helper.add_build(name="test")
    b2 = helper.add_build(name="test2")
    b3 = helper.add_build(name="test3")

    helper.process_all_tasks()
    assert helper.get_mongo_builds_count() == 3
    b1 = state.build_manager().find(b1.name, b1.id)
    b2 = state.build_manager().find(b2.name, b2.id)
    b3 = state.build_manager().find(b3.name, b3.id)

    helper.complete_tasks(b1.request_id)
    helper.fail_tasks(b2.request_id)
    helper.complete_tasks(b3.request_id)
    helper.restart_server()

    b1 = state.build_manager().find(b1.name, b1.id)
    b2 = state.build_manager().find(b2.name, b2.id)
    b3 = state.build_manager().find(b3.name, b3.id)

    assert b1.status.string == RequestStatusString.COMPLETED
    assert b2.status.string == RequestStatusString.FAILED
    assert b3.status.string == RequestStatusString.COMPLETED

    assert helper.get_failed_builds() == []  # No callback after restart


def test_error_on_scheduling(helper):
    """
    Tests that exceptions on scheduling are handled gracefully
    """
    helper.request_handler.fail_on_scheduling(True)
    build = helper.add_build(name="test")
    helper.process_all_tasks()
    build = state.build_manager().find(build.name, build.id)
    assert build.status.string == BuildStatusString.FAILED

    assert [b.id for b in helper.get_failed_builds()] == [build.id]


def test_restore_build_with_failed_tasks(helper):
    """
    Tests that if build has failed tasks and was in progress on
    server restart it will be restarted again.
    """
    helper.request_handler.has_failed_tasks(True)
    build = helper.add_build(name="test")
    helper.process_all_tasks()
    build = state.build_manager().find(build.name, build.id)
    assert build.status.string == BuildStatusString.IN_PROGRESS
    helper.restart_server()
    helper.process_all_tasks()
    assert build.status.string == BuildStatusString.IN_PROGRESS


def test_clearing_status_error(helper):
    """
    Tests that after failed build requesting new build operation
    clears old errors.
    """
    helper.request_handler.fail_on_scheduling(True)
    build = helper.add_build(name="test")
    helper.process_all_tasks()
    build = state.build_manager().find(build.name, build.id)
    assert build.status.exception
    helper.request_handler.fail_on_scheduling(False)
    state.build_manager().restart(build)
    helper.process_all_tasks()
    build = state.build_manager().find(build.name, build.id)
    assert not build.status.exception
    helper.complete_tasks(build.request_id)
    build = state.build_manager().find(build.name, build.id)
    assert not build.status.exception
