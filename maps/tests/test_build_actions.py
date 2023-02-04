from dataclasses import dataclass, field
from datetime import datetime
import mongomock
import pytest
import time
from typing import List, Optional

from maps.garden.libs_server.build import build_defs
from maps.garden.libs_server.build.build_actions import BuildActionStorage
from maps.garden.libs_server.common.errors import ConflictException


@pytest.fixture
def storage():
    mongodb = mongomock.MongoClient(tz_aware=True).db
    return BuildActionStorage(mongodb)


def test_save_build_action(storage):
    action = build_defs.BuildAction(
        module_name="test1",
        build_id=1,
        operation=build_defs.BuildOperationString.CREATE,
        created_by="fake_user",
        build_creation_data=build_defs.BuildCreationData(
            contour_name="contour",
            sources=[build_defs.Source(
                name="source1",
                version=build_defs.SourceVersion(key="1"),
                properties={})
            ],
            extras={"prop2": "value2"},
            output_resources_keys={"key1"},
            module_version="1234"))
    action_id = storage.create(action)

    saved_action = storage.find()[0]
    assert saved_action.module_name == action.module_name
    assert saved_action.build_id == action.build_id
    assert saved_action.operation == action.operation
    assert saved_action.created_by == action.created_by
    assert saved_action.build_creation_data == action.build_creation_data
    assert isinstance(saved_action.created_at, datetime)
    assert saved_action.completed_at is None
    assert saved_action.completion_message is None

    storage.update(action_id, build_defs.BuildActionStatus.COMPLETED, "OK")
    saved_action = storage.find()[0]
    assert saved_action.module_name == action.module_name
    assert saved_action.build_id == action.build_id
    assert saved_action.operation == action.operation
    assert saved_action.created_by == action.created_by
    assert saved_action.build_creation_data == action.build_creation_data
    assert isinstance(saved_action.created_at, datetime)
    assert isinstance(saved_action.completed_at, datetime)
    assert saved_action.completion_message == "OK"


def test_find_build_action(storage):
    action_id1 = storage.create(build_defs.BuildAction(
        module_name="name1",
        build_id=1,
        action_status=build_defs.BuildActionStatus.COMPLETED,
        operation=build_defs.BuildOperationString.RESTART,
    ))

    time.sleep(0.001)  # Ensure created_at differs from that of action_id1
    action_id2 = storage.create(build_defs.BuildAction(
        module_name="name1",
        build_id=1,
        action_status=build_defs.BuildActionStatus.CREATED,
        build_creation_data=build_defs.BuildCreationData(
            contour_name="contour1",
            extras={"release_name": "1234"},
            module_version="",
        ),
        operation=build_defs.BuildOperationString.CREATE,
    ))
    time.sleep(0.001)  # Ensure created_at differs from that of action_id2
    action_id3 = storage.create(build_defs.BuildAction(
        module_name="name2",
        build_id=2,
        action_status=build_defs.BuildActionStatus.CREATED,
        build_creation_data=build_defs.BuildCreationData(
            contour_name="contour2",
            extras={"release_name": "2345"},
            module_version="",
        ),
        operation=build_defs.BuildOperationString.CREATE,
    ))

    assert _extract_action_ids(storage.find(limit=4)) == [action_id1, action_id2, action_id3]
    assert _extract_action_ids(storage.find()) == [action_id1]

    assert _extract_action_ids(storage.find(
        action_status=build_defs.BuildActionStatus.CREATED,
        limit=3)) == [action_id2, action_id3]

    assert _extract_action_ids(storage.find(build_ids=[1], limit=3)) == [action_id1, action_id2]

    assert _extract_action_ids(storage.find(module_name="name1", limit=0)) == [action_id1, action_id2]

    assert _extract_action_ids(storage.find(contour_names=["contour2"], limit=0)) == [action_id3]

    assert _extract_action_ids(storage.find(
        operation=build_defs.BuildOperationString.CREATE,
        limit=0)) == [action_id2, action_id3]

    assert _extract_action_ids(storage.find(extras={"release_name": "1234"}, limit=0)) == [action_id2]

    storage.remove(module_name="name1", build_id=1)
    assert _extract_action_ids(storage.find(limit=4)) == [action_id3]


@dataclass
class ActionCreationInfo:
    action_status: build_defs.BuildActionStatus = build_defs.BuildActionStatus.COMPLETED
    source_keys: List[str] = field(default_factory=list)
    contour_name: str = "contour"
    module_name: str = "name"
    release_name: Optional[str] = None


@pytest.mark.parametrize(
    ("existing_action", "new_action", "is_exception"),
    [
        (ActionCreationInfo(
            source_keys=["1", "2"]),
         ActionCreationInfo(
            action_status=build_defs.BuildActionStatus.CREATED,
            source_keys=["2", "1"]),
         True),  # ConflictException due to same source keys
        (ActionCreationInfo(
            source_keys=["1", "2"]),
         ActionCreationInfo(
            action_status=build_defs.BuildActionStatus.CREATED,
            contour_name="another_contour",
            source_keys=["2", "1"]),
         False),  # No exception due to a different contour name
        (ActionCreationInfo(
            source_keys=["1", "2"]),
         ActionCreationInfo(
            module_name="another_name",
            action_status=build_defs.BuildActionStatus.CREATED,
            source_keys=["2", "1"]),
         False),  # No exception due to a different module name
        (ActionCreationInfo(
            action_status=build_defs.BuildActionStatus.FAILED,
            source_keys=["1", "2"]),
         ActionCreationInfo(
            action_status=build_defs.BuildActionStatus.CREATED,
            source_keys=["2", "1"]),
         False),  # No exception due to the failed status of the conflicting action
        (ActionCreationInfo(
            release_name="release1",
            source_keys=["1", "2"]),
         ActionCreationInfo(
            release_name="release2",
            action_status=build_defs.BuildActionStatus.CREATED,
            source_keys=["2", "1"]),
         False),  # No exception due to different release names
        (ActionCreationInfo(
            release_name="release1",
            source_keys=["1", "2"]),
         ActionCreationInfo(
            release_name="release1",
            action_status=build_defs.BuildActionStatus.CREATED,
            source_keys=["1", "2", "3"]),
         True),  # release_name conflict
    ],
)
def test_build_creation_source_keys_conflict(existing_action, new_action, is_exception, storage):
    storage.create(
        build_defs.BuildAction(
            module_name=existing_action.module_name,
            build_id=1,
            operation=build_defs.BuildOperationString.CREATE,
            action_status=existing_action.action_status,
            build_creation_data=build_defs.BuildCreationData(
                contour_name=existing_action.contour_name,
                sources=[
                    build_defs.Source(name="src", version=build_defs.SourceVersion(key=k))
                    for k in existing_action.source_keys
                ],
                module_version="",
                extras=({"release_name": existing_action.release_name} if existing_action.release_name else {}))))
    action = build_defs.BuildAction(
        module_name=new_action.module_name,
        build_id=2,
        operation=build_defs.BuildOperationString.CREATE,
        action_status=new_action.action_status,
        build_creation_data=build_defs.BuildCreationData(
            contour_name=new_action.contour_name,
            sources=[
                build_defs.Source(name="src", version=build_defs.SourceVersion(key=k))
                for k in new_action.source_keys
            ],
            module_version="",
            extras=({"release_name": new_action.release_name} if new_action.release_name else {})))

    if is_exception:
        with pytest.raises(ConflictException):
            storage.create(action)
    else:
        storage.create(action)


def test_ongoing_non_create_build_action_conflict(storage):
    storage.create(
        build_defs.BuildAction(
            module_name="name",
            build_id=1,
            operation=build_defs.BuildOperationString.RESTART))
    with pytest.raises(ConflictException):
        storage.create(
            build_defs.BuildAction(
                module_name="name",
                build_id=1,
                operation=build_defs.BuildOperationString.CANCEL))


def _extract_action_ids(build_actions):
    return [x.action_id for x in build_actions]
