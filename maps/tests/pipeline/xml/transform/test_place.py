import pytest

from maps_adv.export.lib.core.client.old_geoadv import OrgPlace
from maps_adv.export.lib.pipeline.xml.transform.place import (
    GenericPlace,
    place_transform,
)
from maps_adv.points.client.lib import ResultPoint

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ["place", "expected"],
    [
        [
            ResultPoint(id=123, latitude=123.45, longitude=45.123),
            GenericPlace(
                id="123",
                latitude=123.45,
                longitude=45.123,
                title=[],
                address=[],
                permalink=None,
            ),
        ],
        [
            OrgPlace(
                permalink=123,
                title="en title",
                address="en address",
                latitude=123.45,
                longitude=45.123,
            ),
            GenericPlace(
                id="altay:123",
                permalink=123,
                title=[{"value": "en title"}],
                address=[{"value": "en address"}],
                latitude=123.45,
                longitude=45.123,
            ),
        ],
        [
            OrgPlace(
                permalink=123,
                title={"en": "en title", "ru": "ru title"},
                address={"en": "en address", "ru": "ru address"},
                latitude=123.45,
                longitude=45.123,
            ),
            GenericPlace(
                id="altay:123",
                permalink=123,
                title=[
                    {"value": "en title", "lang": "en"},
                    {"value": "ru title", "lang": "ru"},
                ],
                address=[
                    {"value": "en address", "lang": "en"},
                    {"value": "ru address", "lang": "ru"},
                ],
                latitude=123.45,
                longitude=45.123,
            ),
        ],
    ],
)
async def test_will_transform_place_as_expected(place, expected):
    assert await place_transform(place) == expected
