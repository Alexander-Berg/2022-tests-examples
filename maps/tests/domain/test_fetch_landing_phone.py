import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm(domain, dm):
    await domain.fetch_landing_phone("54321")

    dm.fetch_landing_phone.assert_called_with("54321")


@pytest.mark.parametrize(
    "phone",
    (
        [None],
        ["+7 (495) 739-70-00"],
    ),
)
async def test_returns_data(phone, domain, dm):
    dm.fetch_landing_phone.coro.return_value = phone
    got = await domain.fetch_landing_phone("54321")

    assert got == {"phone": phone}
