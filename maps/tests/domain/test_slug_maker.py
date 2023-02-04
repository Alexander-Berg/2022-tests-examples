from typing import Iterator
import pytest

from maps_adv.geosmb.landlord.server.lib.domain import SlugMaker
from maps_adv.geosmb.landlord.server.lib.exceptions import SlugInUse


def build_slug_maker(attempts: int) -> SlugMaker:
    attempt = attempts

    async def check(_: str) -> bool:
        nonlocal attempt
        if attempt:
            attempt -= 1
            return False
        return True

    return SlugMaker(check)


def build_templates(attempts: int) -> Iterator[str]:
    return (f'{{}}-{attempt}' for attempt in range(attempts))


async def test_make_slug():
    assert 'slug' == await (build_slug_maker(0)).make_slug('slug', [])
    assert 'slug' == await (build_slug_maker(0)).make_slug('slug', build_templates(1))
    assert 'slug-0' == await (build_slug_maker(1)).make_slug('slug', build_templates(1))
    assert 'slug-0' == await (build_slug_maker(1)).make_slug('slug', build_templates(2))
    assert 'slug-1' == await (build_slug_maker(2)).make_slug('slug', build_templates(2))

    with pytest.raises(SlugInUse):
        await (build_slug_maker(1)).make_slug('slug', [])

    with pytest.raises(SlugInUse):
        await (build_slug_maker(2)).make_slug('slug', ['{}-smth'])
