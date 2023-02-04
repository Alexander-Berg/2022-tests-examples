from unittest import mock
import pytest

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import FileResource, PythonResource

from maps.garden.libs_server.resource_storage.dangling_resource_manager import DanglingResourceStorage


def _execute_remover(dangling_resources_removing_process):
    dangling_resources_removing_process._delay_executor.scheduler.execute_background_task()


@pytest.mark.parametrize("resource_count", [1, 10])
def test_removing_process(db, resource_storage,
                          dangling_resources_removing_process, resource_count):
    dangling_storage = DanglingResourceStorage(db)

    with mock.patch.object(PythonResource, "remove", side_effect=Exception("Can't remove dangling")):
        for i in range(resource_count):
            dangling_resource = PythonResource(name=str(i))
            dangling_resource.version = Version(properties={})
            resource_storage.save(dangling_resource)
            try:
                del resource_storage[dangling_resource.key]
            except Exception:
                pass
        _execute_remover(dangling_resources_removing_process)
        assert len(list(dangling_storage.get_all())) == resource_count

    _execute_remover(dangling_resources_removing_process)
    assert len(list(dangling_storage.get_all())) == 0


def test_removing_process_exc_fields(mocker, db, resource_storage,
                                     dangling_resources_removing_process):
    mocker.patch.object(PythonResource, "remove", side_effect=Exception("Can't remove dangling"))

    dangling_resource = PythonResource(name="A")
    dangling_resource.version = Version(properties={})
    resource_storage.save(dangling_resource)
    try:
        del resource_storage[dangling_resource.key]
    except Exception:
        pass

    dangling_storage = DanglingResourceStorage(db)

    _execute_remover(dangling_resources_removing_process)
    assert len(list(dangling_storage.get_all())) == 1
    _execute_remover(dangling_resources_removing_process)
    assert next(dangling_storage.get_all()).delete_try_count is not None
    _execute_remover(dangling_resources_removing_process)
    assert next(dangling_storage.get_all()).delete_try_count >= 1
    assert "Can't remove dangling" in next(dangling_storage.get_all()).last_exception.message


def _create_resource(storage, file_resource_settings, name="simple_file"):
    path, content = ("file.ext", b"aaaaa")
    resource = FileResource(name, path)
    resource.version = Version()
    resource.load_environment_settings(file_resource_settings)

    with resource.open("wb") as file:
        file.write(content)

    resource.logged_commit()
    assert resource.physically_exists
    storage.save(resource)
    return resource


def test_removing_process_file(mocker, db, resource_storage,
                               dangling_resources_removing_process, test_env):
    mocker.patch.object(
        FileResource,
        "allow_deferred_removal",
        return_value=False,
        new_callable=mocker.PropertyMock
    )
    dangling_resource = _create_resource(resource_storage, test_env)

    dangling_storage = DanglingResourceStorage(db)

    with mock.patch.object(FileResource, "remove", side_effect=Exception("Can't remove dangling")):
        resource_storage.save(dangling_resource)
        try:
            del resource_storage[dangling_resource.key]
        except Exception:
            pass

        assert dangling_resource.physically_exists
        _execute_remover(dangling_resources_removing_process)
        assert len(list(dangling_storage.get_all())) == 1
        dang_docs = list(dangling_storage.get_all())
        assert len(dang_docs) == 1
        assert dang_docs[0].resource.key == dangling_resource.key

    _execute_remover(dangling_resources_removing_process)
    assert len(list(dangling_storage.get_all())) == 0
    assert not dangling_resource.physically_exists


def test_removing_process_recreate_with_same_key(mocker, db, resource_storage, test_env,
                                                 dangling_resources_removing_process):
    mocker.patch.object(FileResource, "remove", side_effect=Exception("Can't remove dangling"))
    mocker.patch.object(
        FileResource,
        "allow_deferred_removal",
        return_value=False,
        new_callable=mocker.PropertyMock
    )
    file_resource = _create_resource(resource_storage, test_env)

    dangling_storage = DanglingResourceStorage(db)

    key = file_resource.key
    try:
        del resource_storage[key]
    except Exception:
        pass

    _execute_remover(dangling_resources_removing_process)
    assert lambda: len(list(dangling_storage.get_all())) == 1
    another_file_resource = _create_resource(resource_storage, test_env)
    assert another_file_resource.key == key

    _execute_remover(dangling_resources_removing_process)
    assert len(list(dangling_storage.get_all())) == 0
    assert another_file_resource.physically_exists


def test_removing_process_recreate_already_removed(mocker, db, resource_storage, test_env,
                                                   dangling_resources_removing_process):
    mocker.patch.object(
        FileResource,
        "allow_deferred_removal",
        return_value=False,
        new_callable=mocker.PropertyMock
    )
    file_resource = _create_resource(resource_storage, test_env)

    dangling_storage = DanglingResourceStorage(db)

    with mock.patch.object(FileResource, "remove", side_effect=Exception("Can't remove dangling")):
        key = file_resource.key
        try:
            del resource_storage[key]
        except Exception:
            pass

        _execute_remover(dangling_resources_removing_process)
        assert len(list(dangling_storage.get_all())) == 1

    file_resource.remove()
    assert not file_resource.physically_exists

    _execute_remover(dangling_resources_removing_process)
    assert len(list(dangling_storage.get_all())) == 0
