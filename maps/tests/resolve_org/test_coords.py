from decimal import Decimal

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_coords(client, mock_resolve_org, make_response):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.coords == (Decimal("11.22"), Decimal("22.33"))
    assert result.latitude == Decimal("11.22")
    assert result.longitude == Decimal("22.33")


async def test_coords_optional(client, mock_resolve_org, make_response):
    resp = make_response()
    resp.reply.geo_object[0].ClearField("geometry")
    mock_resolve_org(resp)

    result = await client.resolve_org(12345)

    assert result.coords is None
    assert result.latitude is None
    assert result.longitude is None
