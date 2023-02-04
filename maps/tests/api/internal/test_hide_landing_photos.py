import pytest

from maps_adv.geosmb.landlord.proto import organization_details_pb2
from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

HIDE_URL = "/v1/hide_landing_photos/"
FETCH_URL = "/v1/fetch_landing_photos/"


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_hide_landing_photos(api, factory, requested_version, existing_version):
    data_id = await factory.insert_landing_data(
        photos=[
            {"id": "1", "url": "url1"},
            {"id": "2", "url": "url2"},
            {"id": "3", "url": "url3"},
        ],
        photo_settings={
            "hidden_ids": ["2"]
        }
    )
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{existing_version: data_id}
    )

    await api.post(
        HIDE_URL,
        proto=landing_details_pb2.HideLandingPhotoInput(
            biz_id=15, version=requested_version, photo_id_to_hidden={"2": False, "3": True}
        ),
        decode_as=landing_details_pb2.ShowLandingPhotoOutput,
        expected_status=200,
    )

    got = await api.post(
        FETCH_URL,
        proto=landing_details_pb2.ShowLandingPhotoInput(
            biz_id=15, version=requested_version
        ),
        decode_as=landing_details_pb2.ShowLandingPhotoOutput,
        expected_status=200,
    )

    assert got == landing_details_pb2.ShowLandingPhotoOutput(
        photos=[
            landing_details_pb2.Photo(
                id="1",
                url="url1",
                hidden=False,
            ),
            landing_details_pb2.Photo(
                id="2",
                url="url2",
                hidden=False,
            ),
            landing_details_pb2.Photo(
                id="3",
                url="url3",
                hidden=True,
            ),
        ]
    )
