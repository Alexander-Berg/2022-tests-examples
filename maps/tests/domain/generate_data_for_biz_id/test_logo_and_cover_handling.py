import asyncio
from unittest import mock

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.tests import PutImageResultMock

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.usefixtures("logging_warning"),
]


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_uses_avatars_client_to_store_cover_and_logo(
    domain, avatars_client, params
):
    await domain.generate_data_for_biz_id(**params)

    avatars_client.put_image_by_url.assert_has_calls(
        [
            mock.call(image_url="https://images.ru/logo/orig", timeout=3),
            mock.call(image_url="https://images.ru/tpl1/orig", timeout=3),
        ]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_stores_logo_and_cover_from_avatars(domain, dm, avatars_client, params):
    avatars_client.put_image_by_url.coro.side_effect = [
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]

    await domain.generate_data_for_biz_id(**params)

    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["cover"]
        == "http://avatars-outer-read.server/get-store/802/red-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_works_well_if_no_logo_provided(
    domain, dm, geosearch, avatars_client, params
):
    geosearch.resolve_org.coro.return_value.logo = None

    await domain.generate_data_for_biz_id(**params)

    avatars_client.put_image_by_url.assert_called_once_with(
        image_url="https://images.ru/tpl1/orig", timeout=3
    )
    assert (
        "logo" not in dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    )
    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["cover"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_works_well_if_photo_for_cover_not_provided(
    domain, dm, geosearch, avatars_client, params
):
    geosearch.resolve_org.coro.return_value.cover = None

    await domain.generate_data_for_biz_id(**params)

    avatars_client.put_image_by_url.assert_called_once_with(
        image_url="https://images.ru/logo/orig", timeout=3
    )
    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert (
        "cover" not in dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_works_well_if_nor_logo_nor_photo_for_cover_provided(
    domain, dm, geosearch, avatars_client, params
):
    geosearch.resolve_org.coro.return_value.logo = None
    geosearch.resolve_org.coro.return_value.cover = None

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    # will not try to upload or reload images
    avatars_client.put_image_by_url.assert_not_called()
    assert (
        "logo" not in dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    )
    assert (
        "cover" not in dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_cover_if_logo_upload_errors(domain, dm, avatars_client, params):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        avatars_client.put_image_by_url.coro.return_value,
    ]

    try:
        await domain.generate_data_for_biz_id(**params)
    except Exception:
        pytest.fail("Should not raise if avatars client fails")

    assert (
        "logo" not in dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    )
    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["cover"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_logo_if_cover_upload_errors(domain, dm, avatars_client, params):
    avatars_client.put_image_by_url.coro.side_effect = [
        avatars_client.put_image_by_url.coro.return_value,
        Exception(),
    ]

    try:
        await domain.generate_data_for_biz_id(**params)
    except Exception:
        pytest.fail("Should not raise if avatars client fails")

    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert (
        "cover" not in dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_tolerates_logo_and_cover_upload_and_reload_errors(
    domain, dm, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = Exception()

    try:
        await domain.generate_data_for_biz_id(**params)
        await asyncio.sleep(0.1)
    except Exception:
        pytest.fail("Should not raise if avatars client failed")

    assert (
        "logo" not in dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    )
    assert (
        "cover" not in dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_logs_all_logo_upload_and_reload_errors(
    caplog, domain, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        avatars_client.put_image_by_url.coro.return_value,
        Exception(),
    ]

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    assert caplog.messages.count("Failed to upload logo: Exception") == 2


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_logs_all_cover_upload_and_reload_errors(
    caplog, domain, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        avatars_client.put_image_by_url.coro.return_value,
        Exception(),
        Exception(),
    ]

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    assert caplog.messages.count("Failed to upload cover: Exception") == 2


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_try_to_reload_if_initial_upload_successful(
    domain, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    assert avatars_client.put_image_by_url.call_count == 2


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_uses_avatars_client_to_reload_cover_and_logo(
    domain, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = Exception()

    await domain.generate_data_for_biz_id(**params)
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
async def test_updates_logo_and_cover_for_both_versions_from_avatars_if_reload_successful(  # noqa E501
    domain, dm, avatars_client, params
):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        Exception(),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    dm.save_landing_data_for_biz_id.assert_any_call(
        biz_id=15,
        landing_data={
            "name": Any(str),
            "categories": Any(list),
            "contacts": Any(dict),
            "extras": Any(dict),
            "preferences": Any(dict),
            "blocks_options": Any(dict),
            "cover": "http://avatars-outer-read.server/get-store/802/red-leaves/%s",
            "logo": "http://avatars-outer-read.server/get-store/603/green-leaves/%s",
        },
        version=LandingVersion.STABLE,
    )
    dm.save_landing_data_for_biz_id.assert_any_call(
        biz_id=15,
        landing_data={
            "name": Any(str),
            "categories": Any(list),
            "contacts": Any(dict),
            "extras": Any(dict),
            "preferences": Any(dict),
            "blocks_options": Any(dict),
            "cover": "http://avatars-outer-read.server/get-store/802/red-leaves/%s",
            "logo": "http://avatars-outer-read.server/get-store/603/green-leaves/%s",
        },
        version=LandingVersion.UNSTABLE,
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_logo_if_reload_successful(domain, dm, avatars_client, params):
    avatars_client.put_image_by_url.coro.side_effect = [
        Exception(),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
    ]

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["cover"]
        == "http://avatars-outer-read.server/get-store/802/red-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_cover_if_reload_seccessful(domain, dm, avatars_client, params):
    avatars_client.put_image_by_url.coro.side_effect = [
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/603/green-leaves/%s",  # noqa
        ),
        Exception(),
        PutImageResultMock(
            url_template="http://avatars-outer-read.server/get-store/802/red-leaves/%s",
        ),
    ]

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["logo"]
        == "http://avatars-outer-read.server/get-store/603/green-leaves/%s"
    )
    assert (
        dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]["cover"]
        == "http://avatars-outer-read.server/get-store/802/red-leaves/%s"
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_reload_logo_from_avatars_if_previous_upload_was_successful(
    domain, dm, avatars_client, params
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

    await domain.generate_data_for_biz_id(**params)
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
    domain, dm, avatars_client, params
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

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    avatars_client.put_image_by_url.assert_has_calls(
        [
            mock.call(image_url="https://images.ru/logo/orig", timeout=3),
            mock.call(image_url="https://images.ru/tpl1/orig", timeout=3),
            mock.call(image_url="https://images.ru/logo/orig", timeout=None),
        ]
    )


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_try_to_update_landing_if_reload_fails(
    domain, avatars_client, params, dm
):
    avatars_client.put_image_by_url.coro.side_effect = Exception()

    await domain.generate_data_for_biz_id(**params)
    await asyncio.sleep(0.1)

    # counts only initial landing data saving
    assert dm.save_landing_data_for_biz_id.call_count == 2
