import pytest

from maps_adv.export.lib.pipeline.xml.transform.polygon import polygon_transform

pytestmark = [pytest.mark.asyncio]


async def test_will_transform_polygon_as_expected():
    polygon = dict(
        id="id:1",
        points=[
            {"latitude": 1.1, "longitude": 2.2},
            {"latitude": 2.2, "longitude": 1.1},
            {"latitude": 1.1, "longitude": 1.1},
            {"latitude": 1.1, "longitude": 2.2},
        ],
    )

    result = await polygon_transform(polygon)

    assert result == dict(
        id="id:1", polygon="POLYGON ((2.2 1.1, 1.1 2.2, 1.1 1.1, 2.2 1.1))"
    )
