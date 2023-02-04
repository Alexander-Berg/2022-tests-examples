import asyncio
from unittest import mock

import pytest

from maps_adv.geosmb.landlord.proto import generate_pb2
from maps_adv.geosmb.landlord.server.tests import PutImageResultMock

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.usefixtures("logging_warning"),
]

URL = "/v1/generate_landing_data/"


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_uses_avatars_client_to_store_cover_and_logo(api, avatars_client, params):
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    avatars_client.put_image_by_url.assert_has_calls(
        [
            mock.call(image_url="https://images.ru/logo/orig", timeout=3),
            mock.call(image_url="https://images.ru/tpl1/orig", timeout=3),
        ]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_stores_logo_and_cover_from_avatars(api, factory, avatars_client, params):
    avatars_client.put_image_by_url.coro.side_effect = [
        PutImageResultMock(
            url_template=(
                "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
            ),
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert (
        landing_datas[0]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert (
        landing_datas[0]["cover"]
        == "http://avatars-outer-read.server/get-store/802/red-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_works_well_if_no_logo_provided(
    api, factory, geosearch, avatars_client, params
):
    geosearch.resolve_org.coro.return_value.logo = None

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    avatars_client.put_image_by_url.assert_called_once_with(
        image_url="https://images.ru/tpl1/orig", timeout=3
    )
    landing_datas = await factory.list_all_landing_data()
    assert landing_datas[0]["logo"] is None
    assert (
        landing_datas[0]["cover"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_works_well_if_photo_for_cover_not_provided(
    api, factory, geosearch, avatars_client, params
):
    geosearch.resolve_org.coro.return_value.cover = None

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    avatars_client.put_image_by_url.assert_called_once_with(
        image_url="https://images.ru/logo/orig", timeout=3
    )
    landing_datas = await factory.list_all_landing_data()
    assert (
        landing_datas[0]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert landing_datas[0]["cover"] is None


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_works_well_if_nor_logo_nor_photo_for_cover_provided(
    api, factory, geosearch, avatars_client, params
):
    geosearch.resolve_org.coro.return_value.logo = None
    geosearch.resolve_org.coro.return_value.cover = None

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    # will not try to upload or reload images
    avatars_client.put_image_by_url.assert_not_called()
    landing_datas = await factory.list_all_landing_data()
    assert landing_datas[0]["logo"] is None
    assert landing_datas[0]["cover"] is None


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_cover_if_logo_upload_errors(api, factory, avatars_client, params):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        avatars_client.put_image_by_url.coro.return_value,
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert landing_datas[0]["logo"] is None
    assert (
        landing_datas[0]["cover"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_logo_if_cover_upload_errors(api, factory, avatars_client, params):
    avatars_client.put_image_by_url.coro.side_effect = [
        avatars_client.put_image_by_url.coro.return_value,
        Exception(),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert (
        landing_datas[0]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert landing_datas[0]["cover"] is None


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_tolerates_logo_and_cover_upload_and_reload_errors(
    api, factory, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = Exception()

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    landing_datas = await factory.list_all_landing_data()
    assert landing_datas[0]["logo"] is None
    assert landing_datas[0]["cover"] is None


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_logs_all_logo_upload_and_reload_errors(
    api, caplog, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        avatars_client.put_image_by_url.coro.return_value,
        Exception(),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    assert caplog.messages.count("Failed to upload logo: Exception") == 2


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_logs_all_cover_upload_and_reload_errors(
    api, caplog, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        avatars_client.put_image_by_url.coro.return_value,
        Exception(),
        Exception(),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    assert caplog.messages.count("Failed to upload cover: Exception") == 2


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_try_to_reload_if_initial_upload_successful(
    api, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    assert avatars_client.put_image_by_url.call_count == 2


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_uses_avatars_client_to_reload_cover_and_logo(
    api, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = Exception()

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    avatars_client.put_image_by_url.assert_has_calls(
        [
            # initial upload which fails
            mock.call(image_url="https://images.ru/logo/orig", timeout=3),
            mock.call(image_url="https://images.ru/tpl1/orig", timeout=3),
            # reload attempts
            mock.call(image_url="https://images.ru/logo/orig", timeout=None),
            mock.call(image_url="https://images.ru/tpl1/orig", timeout=None),
        ]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_stores_logo_and_cover_from_avatars_if_reloaded_successfully(
    api, factory, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        Exception(),
        PutImageResultMock(
            url_template=(
                "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
            ),
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    landing_datas = await factory.list_all_landing_data()
    assert (
        landing_datas[0]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert (
        landing_datas[0]["cover"]
        == "http://avatars-outer-read.server/get-store/802/red-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_reload_logo_from_avatars_if_previous_upload_was_successful(
    api, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
        Exception(),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    avatars_client.put_image_by_url.assert_has_calls(
        [
            mock.call(image_url="https://images.ru/logo/orig", timeout=3),
            mock.call(image_url="https://images.ru/tpl1/orig", timeout=3),
            mock.call(image_url="https://images.ru/tpl1/orig", timeout=None),
        ]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_reload_cover_if_previous_upload_was_successful(
    api, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    avatars_client.put_image_by_url.assert_has_calls(
        [
            mock.call(image_url="https://images.ru/logo/orig", timeout=3),
            mock.call(image_url="https://images.ru/tpl1/orig", timeout=3),
            mock.call(image_url="https://images.ru/logo/orig", timeout=None),
        ]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_cover_if_reloaded_successfully(
    api, factory, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
        Exception(),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    landing_datas = await factory.list_all_landing_data()
    assert (
        landing_datas[0]["logo"]
        == "http://avatars-outer-read.server/get-store/802/red-leaves/%s"
    )
    assert (
        landing_datas[0]["cover"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_logo_if_reloaded_successfully(
    api, factory, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )
    await asyncio.sleep(0.1)

    landing_datas = await factory.list_all_landing_data()
    assert (
        landing_datas[0]["logo"]
        == "http://avatars-outer-read.server/get-store/802/red-leaves/%s"
    )
    assert (
        landing_datas[0]["cover"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
