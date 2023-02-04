import pytest

from sendr_auth import User

from billing.yandex_pay.yandex_pay.core.actions.wallet.beta import CheckBetaIsAllowedAction


@pytest.mark.asyncio
async def test_should_return_true_if_in_white_list(yandex_pay_settings):
    result = await CheckBetaIsAllowedAction(User(yandex_pay_settings.NFC_BETA_UID_WHITELIST[0])).run()

    assert result


@pytest.mark.asyncio
async def test_should_return_true_if_is_yandexoid():
    result = await CheckBetaIsAllowedAction(User(uid=11, is_yandexoid=True)).run()

    assert result


@pytest.mark.asyncio
async def test_should_return_false_if_does_not_match_criteria():
    result = await CheckBetaIsAllowedAction(User(22)).run()

    assert not result
