import json
import pytest

import yatest.common
from urllib.parse import urlencode
from maps.garden.sdk.sandbox import storage, batch_client, errors


def _content(file_path):
    with open(yatest.common.test_source_path(file_path), 'r') as f:
        return json.loads(f.read())


SANDBOX_RESOURCE_TYPE = 'MASSTRANSIT_GTFS_SOURCE'
TOKEN = 'FAKE_TOKEN'
OWNER = 'MAPS_GARDEN'


def test_latest_gtfs_source(requests_mock):
    client = storage.SandboxStorage(TOKEN, OWNER)
    city = 'lisbon13'

    params = {
        'type': SANDBOX_RESOURCE_TYPE,
        'state': 'READY',
        'limit': 1,
        'order': '-id',
        'offset': 0,
        'attrs': json.dumps({'city': city}),
        'owner': 'MAPS_GARDEN'
    }
    requests_mock.get(f'https://sandbox.yandex-team.ru/api/v1.0/resource?{urlencode(params)}',
                      json=_content('data/sandbox_query_lastest_reply.json'))
    latest_source = client.get_latest_resource_info(type=SANDBOX_RESOURCE_TYPE, attrs={'city': city})
    assert latest_source['http']['proxy'] == 'https://proxy.sandbox.yandex-team.ru/247708801'
    assert latest_source['attributes']['city'] == city
    assert latest_source['attributes']['created'] == '2016-10-25T18:48:11'


def test_all_gtfs_sources(requests_mock):
    client = storage.SandboxStorage(TOKEN, OWNER)
    params = {
        'type': SANDBOX_RESOURCE_TYPE,
        'state': 'READY',
        'limit': batch_client.DEFAULT_READ_BATCH_SIZE,
        'order': '-id',
        'offset': 0,
        'owner': 'MAPS_GARDEN'
    }
    requests_mock.get(
        f'https://sandbox.yandex-team.ru/api/v1.0/resource?{urlencode(params)}',
        json=_content('data/sandbox_query_all_reply.json')
    )
    all_sources = client.get_all_resources_infos(type=SANDBOX_RESOURCE_TYPE)
    assert len(all_sources) == 3


def test_all_gtfs_sources_with_limit(requests_mock):
    client = storage.SandboxStorage(TOKEN, OWNER)

    params = {
        'type': SANDBOX_RESOURCE_TYPE,
        'state': 'READY',
        'limit': 5,
        'order': '-id',
        'offset': 0,
        'owner': 'MAPS_GARDEN'
    }
    requests_mock.get(
        f'https://sandbox.yandex-team.ru/api/v1.0/resource?{urlencode(params)}',
        json=_content('data/sandbox_query_all_reply.json')
    )
    all_sources = client.get_all_resources_infos(type=SANDBOX_RESOURCE_TYPE, limit=5)
    assert len(all_sources) == 3


def test_get_child_task_ids(requests_mock):
    client = storage.SandboxStorage(TOKEN, OWNER)
    task_id = '123'
    requests_mock.get(
        f'https://sandbox.yandex-team.ru/api/v1.0/task/{task_id}/children',
        json=_content('data/sadbox_query_task_children.json')
    )
    ids = client._get_child_task_ids(task_id)
    assert ids == {1029949432}


def test_upload_to_sandbox_error(mocker):
    client = storage.SandboxStorage(TOKEN, OWNER)
    subprocess_run_mock = mocker.patch("maps.garden.sdk.sandbox.storage.subprocess.run")
    subprocess_run_mock.return_value.stdout = "Some Error happen"

    arc_mock = mocker.patch("maps.garden.sdk.sandbox.storage.ArcClient", autospec=True)
    arc_mock(oauth_token="fake_token").get_file.return_value = b"some_ya_file_content"
    with pytest.raises(errors.SandboxUploadError):
        client.upload_to_sandbox(
            arc_auth_token="some_arc_token",
            to_upload="some file name",
            upload_from_string="some file content"
        )
