import pytest

from maps_adv.geosmb.landlord.proto import organization_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_landing_data/"


@pytest.mark.parametrize(
    ("false_option", "cleared_field"),
    [
        ("show_cover", "cover"),
        ("show_logo", "logo"),
        ("show_schedule", "schedule"),
        ("show_services", "services"),
        ("show_reviews", "rating"),
        ("show_extras", "extras"),
    ],
)
async def test_respects_blocks_options_for_removable_fields(
    api, factory, landing_data, false_option, cleared_field
):
    landing_data["blocks_options"][false_option] = False
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.HasField(cleared_field)


async def test_respects_block_options_for_list_fields(api, factory, landing_data):
    landing_data["blocks_options"]["show_photos"] = False
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert list(getattr(got, "photos")) == []  # noqa


async def test_respects_block_options_for_address_and_map(api, factory, landing_data):
    landing_data["blocks_options"]["show_map_and_address"] = False
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.contacts.HasField("geo")
