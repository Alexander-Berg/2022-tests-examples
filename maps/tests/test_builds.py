import datetime as dt
import mongomock
import pytest
import pytz

from maps.garden.libs_server.build.build_actions import BuildActionStorage
from maps.garden.libs_server.build.build_manager import BuildManager, next_release_name
from maps.garden.libs_server.build.builds_storage import BuildsStorage
from maps.garden.libs_server.build import build_defs
from maps.garden.libs_server.log_storage.module_event_storage import ModuleEventStorage


def test_list_all_with_pending_build_actions():
    mongodb = mongomock.MongoClient(tz_aware=True).db
    builds_manager = BuildManager(mongodb, ModuleEventStorage(mongodb))
    build_actions_storage = BuildActionStorage(mongodb)
    builds_storage = BuildsStorage(mongodb)

    builds_storage.save(build_defs.Build(
        name="name1",
        id=1,
        contour_name="contour_name",
    ))
    builds_storage.save(build_defs.Build(
        name="name1",
        id=2,
        contour_name="contour_name",
        status=build_defs.BuildStatus(
            string=build_defs.BuildStatusString.SCHEDULING)))
    builds_storage.save(build_defs.Build(
        name="name2",
        id=1,
        contour_name="contour_name",
        status=build_defs.BuildStatus(
            string=build_defs.BuildStatusString.COMPLETED)))

    build_actions_storage.create(build_defs.BuildAction(
        module_name="name1", build_id=1,
        action_status=build_defs.BuildActionStatus.COMPLETED,
        operation=build_defs.BuildOperationString.CREATE,
        build_creation_data=build_defs.BuildCreationData(
            contour_name="",
            sources=[],
            extras={"release_name": "1"},
            module_version="",
            output_resources_keys=set())))
    build_actions_storage.create(build_defs.BuildAction(
        module_name="name1", build_id=3,
        action_status=build_defs.BuildActionStatus.CREATED,
        operation=build_defs.BuildOperationString.CREATE,
        created_by="user",
        build_creation_data=build_defs.BuildCreationData(
            contour_name="contour1",
            sources=[],
            extras={"release_name": "2"},
            module_version="123",
            output_resources_keys=set())))

    build_actions_storage.create(build_defs.BuildAction(
        module_name="name2", build_id=1,
        action_status=build_defs.BuildActionStatus.CREATED,
        operation=build_defs.BuildOperationString.RESTART))
    build_actions_storage.create(build_defs.BuildAction(
        module_name="name2", build_id=2,
        action_status=build_defs.BuildActionStatus.CREATED,
        operation=build_defs.BuildOperationString.CREATE,
        build_creation_data=build_defs.BuildCreationData(
            contour_name="",
            sources=[],
            extras={"release_name": "3"},
            module_version="",
            output_resources_keys=set())))

    # Only builds filtered by name.
    builds = builds_manager.list_all(name="name1")
    assert len(builds) == 2
    assert builds[0].id == 1
    assert builds[1].id == 2

    # Only builds filtered by name and operations.
    builds = builds_manager.list_all(
        name="name2", operations=[build_defs.BuildOperationString.RESTART])
    assert len(builds) == 1
    assert builds[0].id == 1

    # Only builds filtered by name and operations.
    # Actions with operation 'restart' are not included.
    builds = builds_manager.list_all(
        name="name2", operations=[build_defs.BuildOperationString.RESTART], include_pending_actions=True)
    assert len(builds) == 1
    assert builds[0].id == 1

    # Only builds filtered by name and statuses.
    builds = builds_manager.list_all(
        name="name2", statuses=[build_defs.BuildStatusString.COMPLETED])
    assert len(builds) == 1
    assert builds[0].id == 1

    # Only builds filtered by name and statuses.
    # Actions with status 'completed' are not included
    builds = builds_manager.list_all(
        name="name2", statuses=[build_defs.BuildStatusString.COMPLETED], include_pending_actions=True)
    assert len(builds) == 1
    assert builds[0].id == 1

    # Builds + pending create actions filtered by name and statuses.
    builds = builds_manager.list_all(
        name="name1",
        statuses=[
            build_defs.BuildStatusString.WAITING,
            build_defs.BuildStatusString.SCHEDULING,
        ],
        include_pending_actions=True,
    )
    assert len(builds) == 2
    assert builds[0].id == 2
    assert builds[1].id == 3

    # Builds + pending create actions filtered by name.
    builds = builds_manager.list_all(name="name1", include_pending_actions=True)
    assert len(builds) == 3
    assert builds[0].id == 1
    assert builds[1].id == 2
    assert builds[2].id == 3
    assert builds[2].name == "name1"
    assert builds[2].contour_name == "contour1"
    assert builds[2].author == "user"
    assert builds[2].status.string == build_defs.BuildStatusString.WAITING
    assert builds[2].module_version == "123"
    assert builds[2].sources == []
    assert builds[2].extras == {"release_name": "2"}
    assert builds[2].request_id is None
    assert builds[2].fill_missing_policy is None
    assert isinstance(builds[2].created_at, dt.datetime)


@pytest.mark.freeze_time(dt.datetime(2016, 11, 23, 15, 25, 22, tzinfo=pytz.utc))
def test_next_release_name():
    def build_doc(release_name):
        return {'extras': {'release_name': release_name}}

    builds = [build_doc('16.11.23-0')]
    assert next_release_name(builds, '16.11.23-') == '16.11.23-1'

    builds = [build_doc('16.11.23-3'), build_doc('16.11.23-8')]
    assert next_release_name(builds, '16.11.23-') == '16.11.23-9'

    builds = [build_doc('16.11.23-aa'), build_doc('16.11.23-0')]
    assert next_release_name(builds, '16.11.23-') == '16.11.23-1'

    mongodb = mongomock.MongoClient(tz_aware=True).db
    builds_storage = BuildsStorage(mongodb)

    # relevant builds
    builds_storage.save(build_defs.Build(name="n1", contour_name="c1", id=1, extras={"release_name": "16.11.23-1"}))
    builds_storage.save(build_defs.Build(name="n1", contour_name="c1", id=2, extras={"release_name": "16.11.23-2"}))

    # irrelevant builds
    builds_storage.save(build_defs.Build(name="n2", contour_name="c1", id=3, extras={"release_name": "16.11.23-3"}))
    builds_storage.save(build_defs.Build(name="n1", contour_name="c2", id=4, extras={"release_name": "16.11.23-4"}))

    builds_manager = BuildManager(mongodb, ModuleEventStorage(mongodb))
    assert builds_manager.next_release_name(module_name="n1", contour_name="c1") == "16.11.23-3"
