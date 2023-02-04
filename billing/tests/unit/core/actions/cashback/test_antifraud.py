from datetime import datetime
from decimal import Decimal

import pytest
from pay.lib.interactions.antifraud.entities import CashbackAntifraudStatus

from hamcrest import close_to, match_equality

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.antifraud import CheckCashbackAllowedAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions.antifraud import AntifraudClient


@pytest.fixture
def user(entity_auth_user):
    entity_auth_user.login_id = 'login_id'
    return entity_auth_user


@pytest.fixture
def trust_card_id():
    return 'some_trust_card_id'


@pytest.fixture
def mock_antifraud_client(mocker):
    return mocker.patch.object(
        AntifraudClient,
        'get_cashback_status',
        mocker.AsyncMock(return_value=CashbackAntifraudStatus.OK),
    )


@pytest.mark.asyncio
async def test_calls_antifraud_client_with_expected_args(
    user, trust_card_id, mock_antifraud_client
):
    await CheckCashbackAllowedAction(
        user=user,
        external_id='some_id',
        amount=Decimal('10.0'),
        trust_card_id='some_trust_card_id',
        user_agent='agent007',
        user_ip='ip322',
        currency_number='777',
    ).run()

    mock_antifraud_client.assert_awaited_once_with(
        external_id='some_id',
        amount=1000,
        trust_card_id='some_trust_card_id',
        timestamp=match_equality(close_to(datetime.utcnow().timestamp() * 1000, 60)),
        user_agent='agent007',
        user_ip='ip322',
        uid=user.uid,
        login_id=user.login_id,
        currency_number='777',
        device_id=None,
    )


@pytest.mark.asyncio
async def test_should_return_true_on_ok_status(
    user, trust_card_id, mock_antifraud_client
):
    result = await CheckCashbackAllowedAction(
        user=user,
        external_id='some_id',
        amount=10,
        trust_card_id='some_trust_card_id',
        user_agent='agent007',
        user_ip='ip322',
        currency_number='777',
    ).run()

    assert result is True


@pytest.mark.asyncio
async def test_should_return_true_and_should_not_call_antifraud_client_if_disabled(
    user, mock_antifraud_client, yandex_pay_plus_settings
):
    yandex_pay_plus_settings.CASHBACK_ANTIFRAUD_ENABLED = False
    result = await CheckCashbackAllowedAction(
        user=user,
        external_id='some_id',
        amount=10,
        trust_card_id='some_trust_card_id',
        user_agent='agent007',
        user_ip='ip322',
        currency_number='777',
    ).run()

    mock_antifraud_client.assert_not_awaited()
    assert result is True


@pytest.mark.asyncio
@pytest.mark.parametrize('antifraud_status', (CashbackAntifraudStatus.ERROR, CashbackAntifraudStatus.DENY))
async def test_should_return_false_on_bad_antifraud_statuses(
    user, mock_antifraud_client, yandex_pay_plus_settings, antifraud_status
):
    mock_antifraud_client.return_value = antifraud_status
    result = await CheckCashbackAllowedAction(
        user=user,
        external_id='some_id',
        amount=10,
        trust_card_id='some_trust_card_id',
        user_agent='agent007',
        user_ip='ip322',
        currency_number='777',
    ).run()

    assert result is False
