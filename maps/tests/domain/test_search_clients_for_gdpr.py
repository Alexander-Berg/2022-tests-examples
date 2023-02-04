import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.search_clients_for_gdpr(passport_uid=123)

    dm.check_clients_existence_by_passport.assert_called_with(123)


@pytest.mark.parametrize("dm_response", [True, False])
async def test_returns_response_from_dm(domain, dm, dm_response):
    dm.check_clients_existence_by_passport.coro.return_value = dm_response

    got = await domain.search_clients_for_gdpr(passport_uid=123)

    assert got == dm_response
