from decimal import Decimal

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_coords(client, mock_resolve_orgs, make_multi_response):
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].coords == (Decimal("11.22"), Decimal("22.33"))
    assert result[0].latitude == Decimal("11.22")
    assert result[0].longitude == Decimal("22.33")
    assert result[1].coords == (Decimal("22.33"), Decimal("44.55"))
    assert result[1].latitude == Decimal("22.33")
    assert result[1].longitude == Decimal("44.55")


async def test_coords_optional(client, mock_resolve_orgs, make_multi_response):
    resp = make_multi_response()
    resp.reply.geo_object[0].ClearField("geometry")
    mock_resolve_orgs(resp)

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].coords is None
    assert result[0].latitude is None
    assert result[0].longitude is None
