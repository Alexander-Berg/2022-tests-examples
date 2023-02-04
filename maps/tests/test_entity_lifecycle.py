import http.client
import json
import os
import os.path
import sys

import pytest

from maps.garden.sdk.sandbox.entity import SandboxEntity
from maps.pylibs.utils.lib import filesystem

from maps.garden.sdk.sandbox import entity, storage


RESOURCE_DATA_DIR = "data"

TEST_RESOURCE_ID = "31337"
TEST_RESOURCE_TYPE = "OTHER_RESOURCE"
TEST_RESOURCE_DESCRIPTION = "Test resource"
TEST_RESOURCE_ATTRIBUTES = {
    "attribute_one": 1,
    "ttl": "inf"
}

TEST_SANDBOX_TOKEN = "ANY_WILL_DO"
TEST_SANDBOX_GROUP = "MAPS_GARDEN"
TEST_RESOURCE_INFO = {
    "resource_id": TEST_RESOURCE_ID,
    "state": "READY",
    "type": TEST_RESOURCE_TYPE,
    "file_name": RESOURCE_DATA_DIR,
    "description": TEST_RESOURCE_DESCRIPTION,
    "task": {
        "id": "FAKE_TASK_ID"
    },
    "download_link": "FAKE_LINK"
}


class FakeArcClient:
    def __init__(self, token) -> None:
        pass

    def get_file(self, file_path):
        return b"some_file_content"


def test_entity_lifecycle(mocker, requests_mock):
    mocker.patch("maps.garden.sdk.sandbox.storage.ArcClient", FakeArcClient)
    named_temp_file_mock = mocker.patch("maps.garden.sdk.sandbox.storage.tempfile.NamedTemporaryFile", autospec=True)
    mocker.patch("maps.garden.sdk.sandbox.storage.os.chmod", autospec=True)
    process_mock = mocker.patch("maps.garden.sdk.sandbox.storage.subprocess", spec=["run", "PIPE"])
    named_temp_file_mock().__enter__().name = "ya"
    with filesystem.temporary_directory() as tempdir:
        data_dir = os.path.join(tempdir, RESOURCE_DATA_DIR)
        os.mkdir(data_dir)
        with open(os.path.join(data_dir, "foo.txt"), "wt") as f:
            f.write("ololo\n")

        process_mock.run.return_value.stdout = json.dumps(TEST_RESOURCE_INFO)

        sandbox_resource = storage.SandboxStorage(TEST_SANDBOX_TOKEN, TEST_SANDBOX_GROUP).upload_to_sandbox(
            "some_arc_token",
            RESOURCE_DATA_DIR,
            tempdir,
            resource_type=TEST_RESOURCE_TYPE,
            description=TEST_RESOURCE_DESCRIPTION,
            attributes=TEST_RESOURCE_ATTRIBUTES,
        )

        process_mock.run.assert_called_once_with([
            "ya",
            "upload",
            "--do-not-remove",
            "--owner", TEST_SANDBOX_GROUP,
            "--json-output",
            "--type", TEST_RESOURCE_TYPE,
            "--description", TEST_RESOURCE_DESCRIPTION,
            "--attr",
            "attribute_one=1",
            '--attr',
            "ttl=" + TEST_RESOURCE_ATTRIBUTES["ttl"],
            RESOURCE_DATA_DIR,
            "--token", TEST_SANDBOX_TOKEN,
        ], cwd=tempdir, stderr=sys.stderr, stdout=process_mock.PIPE, input=None, encoding="utf-8", check=True)

        sandbox_entity = SandboxEntity.from_resource_id(sandbox_resource["resource_id"])
        sandbox_entity.set_token(TEST_SANDBOX_TOKEN)

        requests_mock.get("https://sandbox.yandex-team.ru/api/v1.0/resource/31337", json=TEST_RESOURCE_INFO)
        assert sandbox_entity.physically_exists

        assert sandbox_entity.resource_id == TEST_RESOURCE_ID
        assert sandbox_entity.resource_info["type"] == TEST_RESOURCE_TYPE
        assert sandbox_entity.resource_info["file_name"] == RESOURCE_DATA_DIR
        assert sandbox_entity.resource_info["description"] == TEST_RESOURCE_DESCRIPTION

        with pytest.raises(ValueError):
            sandbox_entity.release_task(
                release_type="INVALID",
                subject="test release",
                message="test release message"
            )

        requests_mock.post("https://sandbox.yandex-team.ru/api/v1.0/release")
        sandbox_entity.release_task(
            release_type="testing",
            subject="test release",
            message="test release message"
        )

        requests_mock.put("https://sandbox.yandex-team.ru/api/v1.0/batch/resources/delete",
                          json=[{"status": "SUCCESS",
                                 "message": "The resource is successfully deleted.",
                                 "id": 31337}]
                          )
        sandbox_entity.remove()

        requests_mock.delete("https://sandbox.yandex-team.ru/api/v1.0/resource/31337/attribute/ttl")
        sandbox_entity.remove(force=True)

        requests_mock.get("https://sandbox.yandex-team.ru/api/v1.0/resource/31337", status_code=http.client.NOT_FOUND)
        fresh_entity = entity.SandboxEntity.from_resource_id(int(sandbox_entity.resource_id))
        fresh_entity.set_token(TEST_SANDBOX_TOKEN)
        fresh_entity.update_resource_info()
        assert not fresh_entity.physically_exists
