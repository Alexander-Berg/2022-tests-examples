import logging
from decimal import Decimal

import pytest

from hamcrest import assert_that, contains, equal_to, has_entries, has_item, has_properties, instance_of

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.get_install_reward import GetNFCInstallRewardAmountAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.get_trusted_user_devices import (
    GetTrustedUserDevicesAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.scoring.check_user_score import CheckUserScoreAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.user import User
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import BadUserScoreError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.nfc_install_reward import NFCInstallReward

USERS = tuple(User(uid) for uid in [2, 10, 15, 19])
REWARD_AMOUNTS = (Decimal('0'), Decimal('100'), Decimal('200'), Decimal('300'))


@pytest.fixture
def device_id(rands):
    return rands()


@pytest.fixture(autouse=True)
def mock_user_score_check_action(mock_action):
    mock_action(CheckUserScoreAction)


@pytest.fixture(autouse=True)
def mock_get_trusted_devices_action(mock_action, device_id):
    return mock_action(GetTrustedUserDevicesAction, [device_id])


@pytest.mark.asyncio
@pytest.mark.parametrize('user,expected_amount', zip(USERS, REWARD_AMOUNTS))
async def test_get_reward_amount(device_id, user, expected_amount):
    amount = await GetNFCInstallRewardAmountAction(
        user=user,
        device_id=device_id,
    ).run()

    assert_that(amount, instance_of(Decimal))
    assert_that(amount, equal_to(expected_amount))


@pytest.mark.asyncio
async def test_call_logged(device_id, dummy_logs):
    amount = await GetNFCInstallRewardAmountAction(
        user=User(100),
        device_id=device_id,
    ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        contains(
            has_properties(
                message='NFC_INSTALL_REWARD_CALCULATION_REQUESTED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=100,
                    device_id=device_id,
                )
            ),
            has_properties(
                message='NFC_INSTALL_REWARD_CALCULATED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=100,
                    device_id=device_id,
                    reward_amount=amount,
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('user', USERS)
async def test_reward_globally_disabled(device_id, user, mocker, dummy_logs):
    mocker.patch.object(GetNFCInstallRewardAmountAction, 'reward_enabled', False)

    amount = await GetNFCInstallRewardAmountAction(
        user=user,
        device_id=device_id,
    ).run()

    assert_that(amount, equal_to(Decimal(0)))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='NFC_INSTALL_REWARD_GLOBALLY_DISABLED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    device_id=device_id,
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('user', USERS)
async def test_reward_already_granted(storage, device_id, user, dummy_logs):
    await storage.customer.create(Customer(uid=user.uid))
    reward = await storage.nfc_install_reward.create(
        NFCInstallReward(uid=user.uid, device_id=device_id, amount=Decimal('100'))
    )

    amount = await GetNFCInstallRewardAmountAction(
        user=user,
        device_id=device_id,
    ).run()

    assert_that(amount, equal_to(Decimal(0)))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='NFC_INSTALL_REWARD_ALREADY_GRANTED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    device_id=device_id,
                    existing_rewards=[reward],
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('user,reward_amount', zip(USERS, REWARD_AMOUNTS))
async def test_bad_user_score(storage, device_id, user, reward_amount, dummy_logs, mock_action):
    mock_action(CheckUserScoreAction, BadUserScoreError)

    amount = await GetNFCInstallRewardAmountAction(
        user=user,
        device_id=device_id,
    ).run()

    assert_that(amount, equal_to(Decimal(0)))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='NFC_INSTALL_REWARD_BAD_USER_SCORE',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    device_id=device_id,
                    reward_amount=reward_amount,
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('user', USERS)
async def test_no_reward_values(storage, device_id, user, dummy_logs, mocker):
    mocker.patch.object(
        GetNFCInstallRewardAmountAction, 'reward_values', tuple()
    )

    amount = await GetNFCInstallRewardAmountAction(
        user=user,
        device_id=device_id,
    ).run()

    assert_that(amount, equal_to(Decimal(0)))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='NFC_INSTALL_REWARD_CALCULATED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    device_id=device_id,
                    reward_amount=amount,
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('user', USERS)
async def test_device_not_trusted(device_id, user, mock_action, dummy_logs, rands):
    another_device = rands()
    mock_action(GetTrustedUserDevicesAction, [another_device])

    amount = await GetNFCInstallRewardAmountAction(
        user=user,
        device_id=device_id,
    ).run()

    assert_that(amount, equal_to(Decimal(0)))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='NFC_USER_DEVICE_NOT_TRUSTED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    device_id=device_id,
                    trusted_device_ids=[another_device],
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('user,expected_amount', zip(USERS, REWARD_AMOUNTS))
async def test_device_check_turned_off(
    device_id, user, expected_amount, mocker, mock_get_trusted_devices_action
):
    mocker.patch.object(GetNFCInstallRewardAmountAction, 'check_trusted_devices', False)

    amount = await GetNFCInstallRewardAmountAction(
        user=user,
        device_id=device_id,
    ).run()

    assert_that(amount, equal_to(expected_amount))
    mock_get_trusted_devices_action.assert_not_called()
