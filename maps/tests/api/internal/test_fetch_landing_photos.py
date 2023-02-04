import pytest

from maps_adv.geosmb.landlord.proto import organization_details_pb2
from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_landing_photos/"


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_return_structured_landing_photos(api, factory, requested_version, existing_version):
    data_id = await factory.insert_landing_data(
        photos=[{"id": "1", "url": "url1"}],
    )
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{existing_version: data_id}
    )

    got = await api.post(
        URL,
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
            )
        ]
    )


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_return_unstructured_landing_photos(api, factory, requested_version, existing_version):
    data_id = await factory.insert_landing_data(
        photos=["url1", "url2"],
    )
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{existing_version: data_id}
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingPhotoInput(
            biz_id=15, version=requested_version
        ),
        decode_as=landing_details_pb2.ShowLandingPhotoOutput,
        expected_status=200,
    )

    assert got == landing_details_pb2.ShowLandingPhotoOutput(
        photos=[
            landing_details_pb2.Photo(
                url="url1",
                hidden=False,
            ),
            landing_details_pb2.Photo(
                url="url2",
                hidden=False,
            ),
        ]
    )
