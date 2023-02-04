import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.clear_clients_for_gdpr(passport_uid=123)

    dm.clear_clients_by_passport.assert_called_with(123)


async def test_returns_removed_clients(domain, dm):
    dm_result = [dict(client_id=1, biz_id=111), dict(client_id=2, biz_id=222)]
    dm.clear_clients_by_passport.coro.return_value = dm_result

    got = await domain.clear_clients_for_gdpr(passport_uid=123)

    assert got == dm_result
