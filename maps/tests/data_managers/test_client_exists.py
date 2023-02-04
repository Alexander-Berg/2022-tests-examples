import pytest

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "params, expected_result",
    [
        (dict(client_id=111, biz_id=222), True),
        (dict(client_id=999, biz_id=222), False),
        (dict(client_id=111, biz_id=999), False),
    ],
)
async def test_returns_client_existence(factory, dm, params, expected_result):
    await factory.create_client(client_id=111, biz_id=222)

    got = await dm.client_exists(**params)

    assert got == expected_result
