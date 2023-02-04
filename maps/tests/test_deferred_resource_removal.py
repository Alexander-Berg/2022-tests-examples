import datetime as dt
import pytest

from maps.garden.sdk.core import Version
from maps.garden.sdk.core.optional import OptionalResource, make_empty_resource
from maps.garden.sdk.resources import PythonResource

from maps.garden.libs_server.resource_storage.deferred_removal import DeferredRemovalRecord
from maps.garden.libs_server.resource_storage.exceptions import NotFoundInStorageError


@pytest.fixture
def mocked_python_resource(mocker):
    mocker.patch.object(
        PythonResource,
        "allow_deferred_removal",
        new_callable=mocker.PropertyMock(return_value=True),
    )


def _execute_deferred_removal(deferred_remover_process):
    deferred_remover_process.run_deferred_removal_once()


DR_A = PythonResource("A")
DR_A.version = Version(properties={"date": "dateA"})

DR_B = PythonResource("B")
DR_B.version = Version(properties={"date": "dateB"})


def test_deferred_overwrite(mocker, resource_storage, deferred_remover_process, mocked_python_resource):
    mocked_remove = mocker.patch.object(PythonResource, "remove")

    res1 = PythonResource("A")
    res1.version = Version(properties={"date": "dateA"})

    res2 = PythonResource("A")
    res2.version = Version(properties={"date": "dateA"})

    assert res1.key == res2.key
    resource_storage.save(res1)

    assert not mocked_remove.called

    resource_storage.save(res2)
    _execute_deferred_removal(deferred_remover_process)

    assert mocked_remove.call_count == 1


def test_deferred_removal(mocker, resource_storage, deferred_remover_process,
                          db, mocked_python_resource):
    mocked_remove = mocker.patch.object(PythonResource, "remove")

    assert db.deferred_removal_resources.count({"obsolete": False}) == 0
    resource_storage.save(DR_A)
    resource_storage.save(DR_B)

    del resource_storage[DR_A.key]
    with pytest.raises(NotFoundInStorageError):
        del resource_storage[DR_A.key]
    del resource_storage[DR_B.key]

    assert mocked_remove.call_count == 0
    assert db.deferred_removal_resources.count({"obsolete": False}) == 2

    _execute_deferred_removal(deferred_remover_process)

    assert db.deferred_removal_resources.count({"obsolete": False}) == 0
    assert db.deferred_removal_resources.find().count() == 0
    assert mocked_remove.call_count == 2


def test_optional_and_empty_resource(mocker, resource_storage, deferred_remover_process, mocked_python_resource):
    mocked_remove = mocker.patch.object(PythonResource, "remove")

    opt = OptionalResource(DR_A)
    empty = make_empty_resource(name="empty")
    empty.key = empty.calculate_key()

    resource_storage.save(opt)
    resource_storage.save(empty)
    del resource_storage[opt.key]
    del resource_storage[empty.key]

    _execute_deferred_removal(deferred_remover_process)

    assert mocked_remove.call_count == 1

    mocked_remove = mocker.patch.object(PythonResource, "remove", side_effect=Exception("Expected exception"))
    resource_storage.save(opt)
    del resource_storage[opt.key]
    assert mocked_remove.call_count == 0


def test_deferred_removal_failures(
    mocker,
    resource_storage,
    deferred_remover_process,
    db,
    mocked_python_resource,
):
    mocked_remove = mocker.patch.object(PythonResource, "remove", side_effect=Exception("Expected exception"))

    resource_storage.save(DR_A)
    resource_storage.save(DR_B)
    del resource_storage[DR_A.key]
    del resource_storage[DR_B.key]

    _execute_deferred_removal(deferred_remover_process)

    assert mocked_remove.call_count >= 2
    assert db.deferred_removal_resources.count({"obsolete": False}) == 2

    for doc in db.deferred_removal_resources.find():
        assert doc["attempts_count"] >= 1
        assert "Expected exception" in doc["last_exception"]["message"]


def test_deferred_removal_timeout(mocker, resource_storage, deferred_removal_config,
                                  mocked_python_resource, db, deferred_remover_process):
    mocker.patch.object(PythonResource, "remove", side_effect=Exception("Expected exception"))
    resource_storage.save(DR_A)
    del resource_storage[DR_A.key]

    for doc in db.deferred_removal_resources.find():
        record = DeferredRemovalRecord.parse_obj(doc)
        record.inserted_at -= dt.timedelta(seconds=deferred_removal_config["deferred_removal"]["give_up_timeout"])
        db.deferred_removal_resources.replace_one(
            {"_id": record.mongo_id}, record.dict())

    _execute_deferred_removal(deferred_remover_process)

    assert db.deferred_removal_resources.count({"obsolete": False}) == 0
    assert db.deferred_removal_resources.count({"obsolete": True}) == 1
