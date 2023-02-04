import asyncio
import hashlib
import json
import os
import pytest
import responses
from unittest.mock import Mock
from pyfakefs.fake_filesystem_unittest import Patcher

from maps_adv.common.third_party_clients.juggler import JugglerClient
from maps_adv.export.lib.core.client.sandbox import (
    SandboxClient,
    SandboxClientException,
)


@pytest.fixture
def attrs():
    return {
        "key1": "value1",
        "key2": "value2",
    }


def coro_mock():
    coro = Mock(name="CoroutineResult")
    corofunc = Mock(name="CoroutineFunction", side_effect=asyncio.coroutine(coro))
    corofunc.coro = coro
    return corofunc


@pytest.fixture
async def mock_juggler_client(mocker):
    return mocker.patch(
        "maps_adv.common.third_party_clients.juggler.JugglerClient._request",
        new_callable=coro_mock,
    ).coro


@responses.activate
def test_success(config, tmp_path, attrs, mock_juggler_client):
    # Fix for pytest 7 update.
    # Pytest try to access disk betwen fixture call and run test via Path objects
    # So replace fs fixture from pyfakefs with direct call Patcher
    # See https://github.com/jmcgeheeiv/pyfakefs/issues/666
    with Patcher() as pather:
        fs = pather.fs
        fs.create_file(os.path.join(tmp_path, config.FILENAME_XML), contents="payload")
        responses.add(
            responses.POST,
            "https://example.com/api/v1.0/task",
            json=dict(id=0),
            status=200,
        )
        responses.add(
            responses.PUT,
            "https://example.com/api/v1.0/batch/tasks/start",
            json=[dict(status="SUCCESS")],
            status=200,
        )
        responses.add(
            responses.GET,
            "https://example.com/api/v1.0/task/0",
            json=dict(
                status="PREPARING",
                output_parameters=dict(resource=1),
            ),
            status=200,
        )
        responses.add(
            responses.HEAD,
            "https://example.com/upload/0",
            body="",
            status=200,
        )

        sha1 = hashlib.sha1()

        def consume_body(req):
            nonlocal sha1
            # last chunk contains sha1 of the client, we ignore it
            prev = next(req.body, None)
            while prev:
                chunk = next(req.body, None)
                if chunk:
                    sha1.update(prev)
                prev = chunk
            return True, ""

        responses.add(
            responses.PUT,
            "https://example.com/upload/0",
            body="size",
            status=200,
            match=[consume_body],
        )
        responses.add(
            responses.GET,
            "https://example.com/api/v1.0/task/0",
            json=dict(status="SUCCESS"),
            status=200,
        )
        responses.add_callback(
            responses.GET,
            "https://example.com/api/v1.0/task/0/context",
            callback=lambda request: (
                200,
                {},
                json.dumps(
                    dict(upload=dict(received=10240, checksum=sha1.hexdigest()))
                ),
            ),
        )
        responses.add(
            responses.GET,
            "https://example.com/api/v1.0/resource/1",
            json=dict(
                skynet_id=0,
                md5="",
                http=dict(proxy="bla"),
            ),
            status=200,
        )
        mock_juggler_client.return_value = {"success": True, "events": [{"code": 200}]}
        asyncio.run(
            SandboxClient(
                config,
                attrs,
                tmp_path,
                JugglerClient(config.JUGGLER_EVENTS_URL, config.NANNY_SERVICE_ID),
            ).upload()
        )


@responses.activate
def test_failure(config, tmp_path, attrs):
    with pytest.raises(SandboxClientException) as exc_info:
        asyncio.run(
            SandboxClient(
                config,
                attrs,
                tmp_path,
                JugglerClient(config.JUGGLER_EVENTS_URL, config.NANNY_SERVICE_ID),
            ).upload()
        )

    assert not exc_info.value.args
