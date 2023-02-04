from datetime import timedelta

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_tz_offset(client, mock_resolve_org, make_response):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.tz_offset == timedelta(seconds=10800)


async def test_tz_offset_is_optional(
    client, mock_resolve_org, make_response, business_go_meta
):
    business_go_meta.open_hours.ClearField("tz_offset")
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.tz_offset is None
