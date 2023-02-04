import pytest

from maps_adv.geosmb.doorman.server.lib.enums import OrderByField

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.list_suggest_clients(
        biz_id=123, search_field=OrderByField.EMAIL, search_string="email", limit=10
    )

    dm.list_suggest_clients.assert_called_with(
        biz_id=123, search_field=OrderByField.EMAIL, search_string="email", limit=10
    )


async def test_returns_client_details(domain, dm):
    dm_result = [
        dict(
            id=111,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            first_name="client_first_name",
            last_name="client_last_name",
        )
    ]
    dm.list_suggest_clients.coro.return_value = dm_result

    got = await domain.list_suggest_clients(
        biz_id=123, search_field=OrderByField.EMAIL, search_string="email", limit=10
    )

    assert got == dm_result
