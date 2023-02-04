import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_servers_of_successful_operations(factory, dm):
    request_id = await factory.create_request()
    await factory.create_operation(
        request_id=request_id, service_name="miracle", is_success=True
    )
    await factory.create_operation(
        request_id=request_id, service_name="rainbow", is_success=True
    )
    await factory.create_operation(
        request_id=request_id, service_name="buggy", is_success=False
    )

    got = await dm.list_processed_services(request_id=request_id)

    assert got == {"miracle", "rainbow"}


async def test_returns_nothing_if_no_successful_operations(factory, dm):
    request_id = await factory.create_request()

    got = await dm.list_processed_services(request_id=request_id)

    assert got == set()


async def test_returns_nothing_for_unknown_request(factory, dm):
    got = await dm.list_processed_services(request_id=111)

    assert got == set()


async def test_ignores_operations_of_alien_request(factory, dm):
    await factory.create_request(request_id=111)
    await factory.create_operation(
        request_id=111, service_name="miracle", is_success=True
    )

    got = await dm.list_processed_services(request_id=222)

    assert got == set()
