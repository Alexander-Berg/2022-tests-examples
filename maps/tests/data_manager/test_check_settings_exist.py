import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_true_if_settings_exist(dm, factory):
    await factory.create_org_settings(biz_id=123)

    result = await dm.check_settings_exist(biz_id=123)

    assert result is True


async def test_returns_false_if_settings_do_not_exist(dm):
    result = await dm.check_settings_exist(biz_id=123)

    assert result is False
