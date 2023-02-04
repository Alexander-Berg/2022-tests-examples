import copy
import os
import tempfile

import pytest
from unittest import mock
from os.path import join
import http.client

from maps.garden.sdk.core import Resource, Version, GardenError
from maps.garden.sdk.resources import DirResource
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.sandbox.entity import SandboxEntity
from maps.garden.sdk.sandbox import resources, ResourceVerifyError, UploadToSandboxTask, SandboxResource
from maps.garden.sdk.sandbox.proto import resource_pb2 as sandbox_resource_proto


def test_there_and_back_again_empty_sandbox_resource(requests_mock):
    sandbox_resource = resources.SandboxResource(
        "test_sandbox_resource"
    )
    proto = sandbox_resource.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(sandbox_resource_proto.sandboxResource)

    assert proto.name == sandbox_resource.name
    assert proto.state == resource_proto.Resource.State.NOT_CREATED

    decoded_resource = Resource.from_proto(proto)
    assert isinstance(decoded_resource, resources.SandboxResource)
    assert decoded_resource.name == sandbox_resource.name
    assert decoded_resource.sandbox_entity is None


def test_there_and_back_again_filled_sandbox_resource(requests_mock):
    entity = SandboxEntity(
        resource_info={
            "id": 31337
        }
    )
    sandbox_resource = resources.SandboxResource("test_sandbox_resource")
    sandbox_resource.sandbox_entity = entity

    proto = sandbox_resource.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(sandbox_resource_proto.sandboxResource)

    assert proto.name == sandbox_resource.name
    assert proto.state == resource_proto.Resource.State.NOT_CREATED

    requests_mock.get(
        "https://sandbox.yandex-team.ru:443/api/v1.0/resource/31337",
        json=entity.resource_info
    )

    decoded_resource = Resource.from_proto(proto)
    assert isinstance(decoded_resource, resources.SandboxResource)
    assert decoded_resource.name == sandbox_resource.name
    assert decoded_resource.resource_id == sandbox_resource.resource_id


def test_there_and_back_again_filled_sandbox_release_status(requests_mock):
    entity = SandboxEntity(
        resource_info={
            "id": 31337
        }
    )
    release_status = resources.SandboxReleaseStatusResource("test_release_status")
    release_status.sandbox_entity = entity

    proto = release_status.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(sandbox_resource_proto.sandboxReleaseStatus)

    assert proto.name == release_status.name
    assert proto.state == resource_proto.Resource.State.NOT_CREATED

    requests_mock.get(
        "https://sandbox.yandex-team.ru:443/api/v1.0/resource/31337",
        json=entity.resource_info
    )

    decoded_resource = Resource.from_proto(proto)
    assert isinstance(decoded_resource, resources.SandboxReleaseStatusResource)
    assert decoded_resource.name == release_status.name
    assert decoded_resource.sandbox_entity.resource_id == release_status.sandbox_entity.resource_id


def test_remove_not_existing_resource(requests_mock, environment_settings):
    entity = SandboxEntity(
        resource_info={
            "id": 31337
        }
    )
    release_status = resources.SandboxReleaseStatusResource("test_release_status")
    release_status.sandbox_entity = entity
    release_status.load_environment_settings(environment_settings)
    requests_mock.get("https://sandbox.yandex-team.ru/api/v1.0/resource/31337", status_code=http.client.NOT_FOUND)
    release_status.remove()


def test_sandbox_resource_deepcopy(environment_settings, requests_mock):
    entity = SandboxEntity(
        resource_info={
            "id": 31337
        }
    )
    sandbox_resource = resources.SandboxResource("test_sandbox_resource")
    sandbox_resource.sandbox_entity = entity
    sandbox_resource.load_environment_settings(environment_settings)

    assert id(sandbox_resource) != id(copy.deepcopy(sandbox_resource))


@mock.patch("maps.garden.sdk.sandbox.tasks.SandboxStorage")
def test_task_upload_resource(sandbox_storage_mock, environment_settings, requests_mock):
    tempdir = tempfile.mkdtemp()
    index_dir = join(tempdir, "index")
    os.mkdir(index_dir)
    content = bytes("Test content\n"*10, "utf-8")
    with open(join(index_dir, "foo.txt"), "wb") as f:
        f.write(content)

    input_resource = DirResource(
        name="input_dir",
        dirname_template="",
        doc="Geocoder index")
    input_resource._working_dir = index_dir
    input_resource.size["bytes"] = len(content)
    resource_id = 123
    sandbox_storage_mock.return_value.upload_to_sandbox.return_value = {"resource_id": resource_id}

    upload_task = UploadToSandboxTask("TEST_TASK_RESOURCE",
                                      extra_attributes={"region": "RU", "version": "tv"})

    sandbox_resource = SandboxResource(name="sandbox_resource", doc="DESCRIPTION")
    sandbox_resource.version = Version(properties={"region": "RU"})
    upload_task.load_environment_settings(environment_settings)

    requests_mock.get(f"https://sandbox.yandex-team.ru/api/v1.0/resource/{resource_id}", json={"size": len(content)})
    upload_task(input_resource, sandbox_resource)

    def verify_failed(input_dir):
        raise ResourceVerifyError("Wrong")

    with pytest.raises(GardenError):
        fail_task = UploadToSandboxTask("TEST_TASK_RESOURCE",
                                        input_resource_verifier=verify_failed)
        fail_task(input_resource, sandbox_resource)
