import re
import urllib.parse

import pytest
from aiohttp.web import json_response
from smb.common.http_client import UnknownResponse

from maps_adv.common.avatars import AvatarsClient, AvatarsInstallation, PutImageResult

pytestmark = [pytest.mark.asyncio]


def make_error_data(description: str):
    return {"attrs": {}, "description": description, "status": "error"}


@pytest.fixture
def mock_response(aresponses):
    return lambda h: aresponses.add(
        "avatars-write.server", re.compile("/put-*"), "GET", h
    )


@pytest.mark.parametrize(
    "image_url, expected_request_image_url",
    [
        ("http://image-url/kek.png", "http://image-url/kek.png"),
        ("http://путь/kek.png", "http://%D0%BF%D1%83%D1%82%D1%8C/kek.png"),
    ],
)
@pytest.mark.parametrize("timeout", [None, 10])
async def test_sends_correct_request(
    client, mock_response, image_url, expected_request_image_url, timeout
):
    request_url = None
    request_body = None

    async def handler(request):
        nonlocal request_url, request_body
        request_url = str(request.url)
        request_body = await request.read()
        return json_response(response_data)

    mock_response(handler)

    await client.put_image_by_url(
        image_url=image_url, image_name="green-leaves", timeout=timeout
    )

    assert (
        request_url
        == f"http://avatars-write.server/put-avatars-namespace/green-leaves?url={expected_request_image_url}"  # noqa
    )
    assert request_body == b""


async def test_sends_request_without_image_name_if_it_is_not_set(client, mock_response):
    request_url_path = None

    async def handler(request):
        nonlocal request_url_path
        parsed_url = urllib.parse.urlparse(str(request.url))
        request_url_path = parsed_url.path
        return json_response(response_data)

    mock_response(handler)

    await client.put_image_by_url(image_url="http://image-url/kek.png")

    assert request_url_path == "/put-avatars-namespace"


async def test_return_data(client, mock_response):
    mock_response(json_response(response_data))

    got = await client.put_image_by_url(image_url="http://image-url/kek.png")

    assert got == PutImageResult(
        root_url="http://avatars-outer-read.server",
        response=response_data,
    )


async def test_return_data_for_inner_namespace(mocker, aiotvm, mock_response):
    mock_response(json_response(response_data))
    mocker.patch.dict(
        "maps_adv.common.avatars.avatars_installations",
        {
            "debug": AvatarsInstallation(
                outer_read_url="http://avatars-outer-read.server",
                inner_read_url="http://avatars-inner-read.server",
                write_url="http://avatars-write.server",
            )
        },
    )
    client = await AvatarsClient(
        installation="debug",  # noqa
        namespace="avatars-namespace",
        inner=True,
        tvm_client=aiotvm,
        tvm_destination="avatars",
    )

    got = await client.put_image_by_url(image_url="http://image-url/kek.png")

    assert got == PutImageResult(
        root_url="http://avatars-inner-read.server",
        response=response_data,
    )


@pytest.mark.parametrize(
    "response_status, response_description",
    [
        (400, "incorrect path"),
        (401, "auth failed"),
        (403, "update is prohibited"),
        (
            409,
            "The image is found in blacklist",
        ),
        (415, "Face was found on the image"),
        (
            429,
            "exceed rate limiter",
        ),
        (
            434,
            "N4util4curl16attemps_are_overE: file downloader ran out of attempts: "
            "url='http://image-url/kek.png'",
        ),
    ],
)
async def test_raises_exception_if_request_does_not_succeed(
    client, mock_response, response_status, response_description
):
    mock_response(
        json_response(make_error_data(response_description), status=response_status)
    )

    with pytest.raises(UnknownResponse) as exc:
        await client.put_image_by_url(image_url="http://image-url/kek.png")

    assert exc.value.status == response_status
    assert exc.value.body == str.encode(
        '{"attrs": {}, "description": "'
        + response_description
        + '", "status": "error"}'
    )


async def test_put_image_result_returns_valid_url_template():
    result = PutImageResult(root_url="http://somerooturl.ru", response=response_data)

    assert (
        result.url_template
        == "http://somerooturl.ru/get-avatars-namespace/603/green-leaves/%s"
    )


async def test_put_image_result_returns_valid_orig_url():
    result = PutImageResult(root_url="http://somerooturl.ru", response=response_data)

    assert (
        result.orig_url
        == "http://somerooturl.ru/get-avatars-namespace/603/green-leaves/orig"
    )


response_data = {
    "group-id": 603,
    "meta": {
        "Faces": [[492, 134, 413, 413]],
        "OldPornoProbability": 0.5294117647058824,
        "PortraitType": "PT_MAIN_PORTRAIT",
        "orig-size": {"x": 1024, "y": 640},
    },
    "sizes": {
        "orig": {
            "height": 640,
            "path": "/get-avatars-namespace/603/green-leaves/orig",
            "width": 1024,
        },
        "sizename": {
            "height": 200,
            "path": "/get-avatars-namespace/603/green-leaves/sizename",
            "width": 200,
        },
    },
}
