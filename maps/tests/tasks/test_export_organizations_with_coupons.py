import pytest

from maps_adv.geosmb.crane_operator.server.lib.tasks import OrgsWithCouponsYtExportTask

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def task(config, coupons_domain):
    return OrgsWithCouponsYtExportTask(config=config, coupons_domain=coupons_domain)


async def test_creates_expected_table(task, mock_yt):
    await task

    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/orgs-with-coupons-dir"


async def test_creates_table_with_expected_schema(task, mock_yt):
    await task

    assert mock_yt["create"].call_args[1] == dict(
        attributes={
            "schema": [
                dict(name="permalink", type="uint64", required=True),
                dict(name="showcase", type="any"),
            ]
        }
    )


async def test_writes_data_as_expected(task, mock_yt, config, facade):
    facade.get_organizations_with_coupons.seq = [
        [
            dict(biz_id=f"biz_id_{idx}", permalink=f"{idx}", showcase_url=f"url_{idx}")
            for idx in range(5)
        ]
    ]

    await task

    mock_yt["write_table"].assert_called_with(
        config["ORGS_WITH_COUPONS_YT_EXPORT_TABLE"],
        [
            dict(permalink=0, showcase={"type": "BOOKING", "value": "url_0"}),
            dict(permalink=1, showcase={"type": "BOOKING", "value": "url_1"}),
            dict(permalink=2, showcase={"type": "BOOKING", "value": "url_2"}),
            dict(permalink=3, showcase={"type": "BOOKING", "value": "url_3"}),
            dict(permalink=4, showcase={"type": "BOOKING", "value": "url_4"}),
        ],
    )
