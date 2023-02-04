import pytest

from maps_adv.geosmb.tuner.proto.permissions_pb2 import (
    UpdatePermissionInput,
    UpdatePermissionOutput,
    PermissionFlag as PermissionFlagProto
)
from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio]

URL = "/v2/update_permission/"


async def test_returns_updated_permissions(api, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[PermissionFlag.READ_REQUESTS])

    result = await api.post(
        URL,
        proto=UpdatePermissionInput(
            biz_id=123,
            passport_uid=456,
            flags=[PermissionFlagProto.READ_REQUESTS]
        ),
        decode_as=UpdatePermissionOutput,
        expected_status=200,
    )

    assert result == UpdatePermissionOutput(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlagProto.READ_REQUESTS]
    )


async def test_returns_updated_permissions_empty(api, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[PermissionFlag.READ_REQUESTS])

    result = await api.post(
        URL,
        proto=UpdatePermissionInput(
            biz_id=123,
            passport_uid=456,
            flags=[]
        ),
        decode_as=UpdatePermissionOutput,
        expected_status=200,
    )

    assert result == UpdatePermissionOutput(
        biz_id=123,
        passport_uid=456,
    )


@pytest.mark.parametrize(
    "proto_flags, enum_flags",
    [
        ([PermissionFlagProto.READ_REQUESTS], [PermissionFlag.READ_REQUESTS]),
        ([], [])
    ],
)
async def test_updates_data_in_db(proto_flags, enum_flags, api, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[PermissionFlag.READ_REQUESTS])

    await api.post(
        URL,
        proto=UpdatePermissionInput(
            biz_id=123,
            passport_uid=456,
            flags=proto_flags
        ),
        expected_status=200,
    )

    result = await factory.fetch_permissions(biz_id=123)

    assert result == [
        dict(
            biz_id=123,
            passport_uid=456,
            flags=enum_flags
        )
    ]
