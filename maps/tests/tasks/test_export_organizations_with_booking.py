import pytest

from maps_adv.geosmb.crane_operator.server.lib.tasks import OrgsWithBookingYtExportTask

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def task(config, coupons_domain):
    return OrgsWithBookingYtExportTask(config=config, coupons_domain=coupons_domain)


async def test_creates_expected_table(task, mock_yt):
    await task

    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/orgs-with-booking-dir"


async def test_creates_table_with_expected_schema(task, mock_yt):
    await task

    assert mock_yt["create"].call_args[1] == dict(
        attributes={
            "schema": [
                dict(name="permalink", type="uint64", required=True),
                dict(name="booking_url", type="string", required=True),
            ]
        }
    )


async def test_writes_data_as_expected(task, mock_yt, config, facade):
    facade.get_organizations_with_booking.seq = [
        [
            dict(biz_id=f"biz_id_{idx}", permalink=f"{idx}", booking_url=f"url_{idx}")
            for idx in range(5)
        ]
    ]

    await task

    mock_yt["write_table"].assert_called_with(
        config["ORGS_WITH_BOOKING_YT_EXPORT_TABLE"],
        [
            dict(permalink=0, booking_url="url_0"),
            dict(permalink=1, booking_url="url_1"),
            dict(permalink=2, booking_url="url_2"),
            dict(permalink=3, booking_url="url_3"),
            dict(permalink=4, booking_url="url_4"),
        ],
    )
