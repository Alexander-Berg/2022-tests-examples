import asyncio

import pytest

from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def mock_warden(mocker):
    _create = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.create_task", new_callable=coro_mock
    )
    _create.coro.return_value = {"task_id": 1, "status": "accepted", "time_limit": 2}

    _update = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.update_task", new_callable=coro_mock
    )
    _update.coro.return_value = {}

    return _create, _update


@pytest.fixture(autouse=True)
def mock_export_coupon_promotions(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "CouponPromotionsYtExportTask.__call__",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_replicate_yt_table(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "ReplicateCouponPromotionsTableYtTask.__call__",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_orgs_with_coupons_yt_export(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "OrgsWithCouponsYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_export_orders_poi_data(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "OrdersPoiYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_generate_data_for_snippet(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "GenerateDataForSnippet.__await__"
    )


@pytest.fixture(autouse=True)
def mock_orgs_with_booking_yt_export(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "OrgsWithBookingYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_loyalty_items_yt_export(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "LoyaltyItemsYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_business_coupons_yt_export(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "BusinessCouponsYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_export_coupons_to_review(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.domains.coupons_domain."
        "CouponsDomain.export_coupons_to_review",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_export_reviewed_to_loyalty(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.domains.coupons_domain."
        "CouponsDomain.export_coupons_reviewed_to_loyalty",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_coupons_with_segments_yt_export(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "CouponsWithSegmentsYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_generate_coupons_poi_data(mocker):
    return mocker.patch(
        "maps_adv.geosmb.crane_operator.server.lib.tasks."
        "GenerateCouponsPoiDataTask.__await__"
    )


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = [
        "geosmb_crane_operator__export_orgs_with_coupons_to_yt",
        "geosmb_crane_operator__export_orgs_with_booking_to_yt",
        "geosmb_crane_operator__export_loyalty_items_to_yt",
        "geosmb_crane_operator__export_business_coupons_to_yt",
        "geosmb_crane_operator__export_coupons_to_review",
        "geosmb_crane_operator__export_reviewed_to_loyalty",
        "geosmb_crane_operator__generate_data_for_snippet",
        "geosmb_crane_operator__export_orders_poi_data",
        "geosmb_crane_operator__export_coupons_with_segments",
        "geosmb_crane_operator__generate_coupons_poi_data",
        "geosmb_crane_operator__export_coupon_promotions",
    ]

    return config


async def test_orgs_export_task_called(api, mock_orgs_with_coupons_yt_export):
    await asyncio.sleep(0.5)

    assert mock_orgs_with_coupons_yt_export.called


async def test_orgs_with_booking_export_task_called(
    api, mock_orgs_with_booking_yt_export
):
    await asyncio.sleep(0.5)

    assert mock_orgs_with_booking_yt_export.called


async def test_loyalty_items_export_task_called(api, mock_loyalty_items_yt_export):
    await asyncio.sleep(0.5)

    assert mock_loyalty_items_yt_export.called


async def test_business_coupons_export_task_called(
    api, mock_business_coupons_yt_export
):
    await asyncio.sleep(0.5)

    assert mock_business_coupons_yt_export.called


async def test_calls_export_coupons_to_review_task(api, mock_export_coupons_to_review):
    await asyncio.sleep(0.5)

    assert mock_export_coupons_to_review.called


async def test_calls_submit_review_results(api, mock_export_reviewed_to_loyalty):
    await asyncio.sleep(0.5)

    assert mock_export_reviewed_to_loyalty.called


async def test_generate_data_for_snippet_called(api, mock_generate_data_for_snippet):
    await asyncio.sleep(0.5)

    assert mock_generate_data_for_snippet.called


async def test_export_orders_poi_data_called(api, mock_export_orders_poi_data):
    await asyncio.sleep(0.5)

    assert mock_export_orders_poi_data.called


async def test_calls_export_coupons_with_segments_task(
    api, mock_coupons_with_segments_yt_export
):
    await asyncio.sleep(0.5)

    assert mock_coupons_with_segments_yt_export.called


async def test_calls_generate_coupons_poi_data_task(
    api, mock_generate_coupons_poi_data
):
    await asyncio.sleep(0.5)

    assert mock_generate_coupons_poi_data.called


async def test_calls_export_coupon_promotions_tasks(
    api, mock_export_coupon_promotions, mock_replicate_yt_table
):
    await asyncio.sleep(0.5)

    assert mock_export_coupon_promotions.called
    assert mock_replicate_yt_table.called
