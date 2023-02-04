import pytest

from maps_adv.geosmb.tuner.proto.permissions_pb2 import (
    CheckPermissionInput,
    CheckPermissionOutput,
    PermissionFlag as PermissionFlagProto
)
from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio]

URL = "/v2/check_permission/"


async def test_returns_permission_check_true(api, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[PermissionFlag.READ_REQUESTS])

    result = await api.post(
        URL,
        proto=CheckPermissionInput(biz_id=123, passport_uid=456, flag=PermissionFlagProto.READ_REQUESTS),
        decode_as=CheckPermissionOutput,
        expected_status=200,
    )

    assert result == CheckPermissionOutput(
        has_permission=True
    )


async def test_returns_not_exists_permission_check_false(api, factory):
    result = await api.post(
        URL,
        proto=CheckPermissionInput(biz_id=123, passport_uid=456, flag=PermissionFlagProto.READ_REQUESTS),
        decode_as=CheckPermissionOutput,
        expected_status=200,
    )

    assert result == CheckPermissionOutput(
        has_permission=False
    )


async def test_returns_empty_permission_check_false(api, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[])

    result = await api.post(
        URL,
        proto=CheckPermissionInput(biz_id=123, passport_uid=456, flag=PermissionFlagProto.READ_REQUESTS),
        decode_as=CheckPermissionOutput,
        expected_status=200,
    )

    assert result == CheckPermissionOutput(
        has_permission=False
    )


async def test_returns_other_biz_permission_check_false(api, factory):
    await factory.create_permission(biz_id=111, passport_uid=456)

    result = await api.post(
        URL,
        proto=CheckPermissionInput(biz_id=123, passport_uid=456, flag=PermissionFlagProto.READ_REQUESTS),
        decode_as=CheckPermissionOutput,
        expected_status=200,
    )

    assert result == CheckPermissionOutput(
        has_permission=False
    )
