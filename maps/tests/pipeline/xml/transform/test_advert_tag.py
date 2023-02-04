import pytest

from maps_adv.export.lib.pipeline.xml.transform.base import advert_tag_transform, advert_tag_hash

pytestmark = [pytest.mark.asyncio]


async def test_will_transform_advert_tag_as_expected():
    advert_tag = dict(id="id:1", companies=[1, 2, 3])

    result = await advert_tag_transform(advert_tag)

    assert result == dict(id="id:1", companies=[1, 2, 3])


async def test_will_hash_advert_tag_as_expected():
    advert_tags = [
        dict(id="id:1", companies=[1, 2, 3]),
        dict(id="id:2", companies=[4, 5, 6]),
    ]
    expected_hash = "4024ec5565b7bd3e6406cd5301e0f2b9"
    assert advert_tag_hash(advert_tags) == expected_hash

    advert_tags[0]["companies"].reverse()
    assert advert_tag_hash(advert_tags) == expected_hash

    advert_tags.reverse()
    assert advert_tag_hash(advert_tags) == expected_hash

    advert_tags.append(advert_tags[0])
    assert advert_tag_hash(advert_tags) == expected_hash
