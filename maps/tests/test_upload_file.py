import re
import urllib.parse
from datetime import timedelta

import pytest
from aiohttp.web import Response
from lxml import etree

from maps_adv.common.helpers import Any
from maps_adv.common.mds import MDSClient, MDSInstallation, UploadFileResult

pytestmark = [pytest.mark.asyncio]


response_data = b"""<?xml version="1.0" encoding="utf-8"?>
<post obj="mds-namespace.kek.csv" id="81d8ba78666dd3d1" groups="3" size="100" key="221/kek.csv">
  <complete addr="141.8.145.55:1032" path="/src/storage/8/data-0.0" group="223" status="0"/>
  <complete addr="141.8.145.116:1032" path="/srv/storage/8/data-0.0" group="221" status="0"/>
  <complete addr="141.8.145.119:1029" path="/srv/storage/5/data-0.0" group="225" status="0"/>
  <written>3</written>
</post>
"""  # noqa


@pytest.fixture
def mock_response(aresponses):
    return lambda h: aresponses.add(
        "mds-write.server", re.compile("/upload-*"), "POST", h
    )


@pytest.mark.parametrize(
    "expire_params, expected_expire",
    [
        (dict(), dict()),
        (dict(expire=timedelta(days=1)), dict(expire="86400s")),
        (dict(expire=timedelta(weeks=1)), dict(expire="604800s")),
    ],
)
async def test_sends_correct_request(
    client, mock_response, expire_params, expected_expire
):
    request_path = None
    request_body = None
    request_params = None

    async def handler(request):
        nonlocal request_path, request_body, request_params
        request_path = str(request.path)
        request_body = await request.read()
        request_params = dict(request.query)
        return Response(body=response_data, content_type="text/xml")

    mock_response(handler)

    await client.upload_file(
        file_content=b"some file content", file_name="kek.csv", **expire_params
    )

    assert request_path == "/upload-mds-namespace/kek.csv"
    assert request_body == b"some file content"
    assert request_params == expected_expire


async def test_sends_request_without_file_name_if_it_is_not_set(client, mock_response):
    request_url_path = None

    async def handler(request):
        nonlocal request_url_path
        parsed_url = urllib.parse.urlparse(str(request.url))
        request_url_path = parsed_url.path
        return Response(body=response_data, content_type="text/xml")

    mock_response(handler)

    await client.upload_file(file_content=b"some file content")

    assert request_url_path == "/upload-mds-namespace"


async def test_return_data(client, mock_response):
    mock_response(Response(body=response_data, content_type="text/xml"))

    got = await client.upload_file(file_content=b"some file content")

    assert got == UploadFileResult(
        root_url="http://mds-outer-read.server",
        response=Any(etree._Element),
        namespace="mds-namespace",
    )


async def test_return_data_for_inner_namespace(mocker, aiotvm, mock_response):
    mock_response(Response(body=response_data, content_type="text/xml"))
    mocker.patch.dict(
        "maps_adv.common.mds.mds_installations",
        {
            "debug": MDSInstallation(
                outer_read_url="http://mds-outer-read.server",
                inner_read_url="http://mds-inner-read.server",
                write_url="http://mds-write.server",
            )
        },
    )

    async with MDSClient(
        installation="debug",  # noqa
        namespace="mds-namespace",
        tvm_client=aiotvm,
        tvm_destination="mds",
        inner=True,
    ) as client:
        got = await client.upload_file(file_content=b"some file content")

    assert got == UploadFileResult(
        root_url="http://mds-inner-read.server",
        response=Any(etree._Element),
        namespace="mds-namespace",
    )


async def test_upload_file_result_returns_valid_file_url():
    result = UploadFileResult(
        root_url="http://mds-outer-read.server",
        response=etree.XML(response_data),
        namespace="mds-namespace",
    )

    assert (
        result.download_link
        == "http://mds-outer-read.server/get-mds-namespace/221/kek.csv?disposition=1"
    )
