import pytest

pytestmark = [pytest.mark.asyncio]


async def test_no_data(dm):
    assert {} == await dm.fetch_avatars(["url"])
    assert {} == await dm.fetch_avatars([])


async def test_avatars(factory, dm):
    await factory.create_avatars(
        source_url="uu", avatars_group_id=18, avatars_name="nn"
    )
    await factory.create_avatars(
        source_url="vv", avatars_group_id=19, avatars_name="mm"
    )

    assert await dm.fetch_avatars(["uu", "v"]) == {"uu": (18, "nn")}
