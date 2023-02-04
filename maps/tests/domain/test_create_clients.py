import pytest

from maps_adv.geosmb.doorman.server.lib.enums import Source

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


create_params = dict(
    biz_id=123,
    source=Source.CRM_INTERFACE,
    label="feb-2021",
    clients=[
        dict(
            first_name="Ivan",
            last_name="Volkov",
            phone="1111111111",
            email="new@yandex.ru",
            comment="new comment",
        )
    ],
)


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.create_clients(**create_params)

    dm.create_clients.assert_called_with(**create_params)


async def test_returns_client_details(domain, dm):
    dm_result = dict(total_created=1, total_merged=2)
    dm.create_clients.coro.return_value = dm_result

    got = await domain.create_clients(**create_params)

    assert got == dm_result
