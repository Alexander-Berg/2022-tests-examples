import pytest

from maps_adv.geosmb.tuner.proto.permissions_pb2 import (
    FetchPermissionsInput,
    FetchPermissionsOutput,
    PermissionFlag as PermissionFlagProto
)
from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio]

URL = "/v2/fetch_permissions/"


async def test_returns_permissions(api, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[PermissionFlag.READ_REQUESTS])

    result = await api.post(
        URL,
        proto=FetchPermissionsInput(biz_id=123),
        decode_as=FetchPermissionsOutput,
        expected_status=200,
    )

    assert result == FetchPermissionsOutput(
        permissions=[
            dict(
                biz_id=123,
                passport_uid=456,
                flags=[PermissionFlagProto.READ_REQUESTS]
            )
        ],
    )


async def test_returns_empty_array_if_no_data(api, factory):
    result = await api.post(
        URL,
        proto=FetchPermissionsInput(biz_id=123),
        decode_as=FetchPermissionsOutput,
        expected_status=200,
    )

    assert result == FetchPermissionsOutput(
        permissions=[]
    )


async def test_does_not_return_other_business_settings(api, factory):
    await factory.create_permission(biz_id=123, passport_uid=456)
    await factory.create_permission(biz_id=111, passport_uid=456)

    result = await api.post(
        URL,
        proto=FetchPermissionsInput(biz_id=123),
        decode_as=FetchPermissionsOutput,
        expected_status=200,
    )

    assert result == FetchPermissionsOutput(
        permissions=[
            dict(
                biz_id=123,
                passport_uid=456,
                flags=[PermissionFlagProto.READ_REQUESTS]
            )
        ],
    )
