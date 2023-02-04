import pytest

pytestmark = [pytest.mark.asyncio]


async def test_fetch_max_geoproduct_id(factory, dm):
    client_id = await factory.create_client()
    for geoproduct_id in (22, 33, 11, None):
        await factory.create_call_event(client_id, geoproduct_id=geoproduct_id)

    got = await dm.fetch_max_geoproduct_id_for_call_events()

    assert got == 33


async def test_fetch_none_if_no_geoproduct_id_set(factory, dm):
    client_id = await factory.create_client()
    await factory.create_call_event(client_id, geoproduct_id=None)

    got = await dm.fetch_max_geoproduct_id_for_call_events()

    assert got is None


async def test_fetch_none_if_no_events(factory, dm):
    got = await dm.fetch_max_geoproduct_id_for_call_events()

    assert got is None
