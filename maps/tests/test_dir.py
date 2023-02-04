import http.client

from maps.garden.sdk.core import OptionalResource, Version
from maps.garden.sdk.resources import PythonResource
from maps.garden.sdk.resources.dir import DirResource


def test_wrong_dir_key(garden_client):
    response = garden_client.get("dir/test123456789test")
    assert response.status_code == http.client.NOT_FOUND


def test_optional_dir_resource(garden_client, resource_storage):
    optional_dir_resource = OptionalResource(DirResource("test", "test"))
    optional_dir_resource.version = Version(properties={"release_name": "0.0.0-0"})
    resource_storage.save(optional_dir_resource)
    response = garden_client.get(f"dir/{optional_dir_resource.key}")
    assert response.status_code == http.client.OK


def test_optional_not_dir_resource(garden_client, resource_storage):
    optional_python_resource = OptionalResource(PythonResource(name="test"))
    optional_python_resource.version = Version(properties={"release_name": "0.0.0-0"})
    resource_storage.save(optional_python_resource)
    response = garden_client.get(f"dir/{optional_python_resource.key}")
    assert response.status_code == http.client.BAD_REQUEST


def test_dir_resource(garden_client, resource_storage, db):
    dir_resource = DirResource("test", "test")
    dir_resource.version = Version(properties={"release_name": "0.0.0-0"})
    resource_storage.save(dir_resource)
    response = garden_client.get(f"dir/{dir_resource.key}")
    assert response.status_code == http.client.OK


def test_not_dir_resource(garden_client, resource_storage):
    python_resource = PythonResource(name="test")
    python_resource.version = Version(properties={"release_name": "0.0.0-0"})
    resource_storage.save(python_resource)
    response = garden_client.get(f"dir/{python_resource.key}")
    assert response.status_code == http.client.BAD_REQUEST
