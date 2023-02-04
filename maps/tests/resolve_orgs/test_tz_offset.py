from datetime import timedelta

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_tz_offset(client, mock_resolve_orgs, make_multi_response):
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.tz_offset for r in result] == [
        timedelta(seconds=10800),
        timedelta(seconds=5400),
    ]


async def test_tz_offset_is_optional(
    client, mock_resolve_orgs, make_multi_response, business_go_meta_multi
):
    business_go_meta_multi[0].open_hours.ClearField("tz_offset")
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].tz_offset is None
