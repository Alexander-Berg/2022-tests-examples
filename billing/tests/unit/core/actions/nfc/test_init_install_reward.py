import logging
from decimal import Decimal

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties, instance_of

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.get_install_reward import GetNFCInstallRewardAmountAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.grant_install_reward import GrantNFCInstallRewardAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.init_install_reward import InitNFCInstallRewardAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.scoring.check_user_score import CheckUserScoreAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.user import User
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    BadUserScoreError,
    InvalidNFCRewardAmountError,
    NFCRewardAlreadyExistsError,
)

REWARD_AMOUNT = Decimal('100')


@pytest.fixture
def device_id(rands):
    return rands()


@pytest.fixture(autouse=True)
def mock_get_reward_action(mock_action):
    return mock_action(GetNFCInstallRewardAmountAction, REWARD_AMOUNT)


@pytest.fixture(autouse=True)
def mock_user_score_check_action(mock_action):
    mock_action(CheckUserScoreAction)


@pytest.fixture
def user(randn):
    return User(uid=randn())


@pytest.mark.asyncio
async def test_success(user, device_id, mock_get_reward_action):
    amount = await InitNFCInstallRewardAction(user=user, device_id=device_id).run()

    assert_that(amount, instance_of(Decimal))
    assert_that(amount, equal_to(REWARD_AMOUNT))
    mock_get_reward_action.assert_called_once_with(user=user, device_id=device_id)


@pytest.mark.asyncio
async def test_action_creates_reward_record(storage, user, device_id):
    await InitNFCInstallRewardAction(user=user, device_id=device_id).run()

    [reward] = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
        uid=user.uid,
        device_id=device_id,
    )
    assert_that(reward, has_properties(uid=user.uid, device_id=device_id, amount=REWARD_AMOUNT))


@pytest.mark.asyncio
async def test_init_reward_creates_grant_task(storage, user, device_id):
    await InitNFCInstallRewardAction(user=user, device_id=device_id).run()
    [reward] = await alist(storage.nfc_install_reward.find(filters={'device_id': device_id}))

    filters = {
        'task_type': 'run_action',
        'action_name': GrantNFCInstallRewardAction.action_name,
    }
    [task] = await alist(storage.task.find(filters=filters))
    assert_that(
        task,
        has_properties(
            state=TaskState.PENDING,
            params=has_entries(
                max_retries=GrantNFCInstallRewardAction.max_retries,
                action_kwargs=has_entries(
                    reward_id=str(reward.reward_id),
                )
            )
        )
    )


@pytest.mark.asyncio
async def test_call_logged(storage, user, device_id, dummy_logs):
    amount = await InitNFCInstallRewardAction(user=user, device_id=device_id).run()

    [reward] = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
        uid=user.uid,
        device_id=device_id,
    )
    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='NFC_INSTALL_REWARD_INITIALIZED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    device_id=device_id,
                    reward_amount=amount,
                    reward_id=reward.reward_id,
                    reward=reward,
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('amount', [Decimal('0'), Decimal('-1')])
async def test_cannot_init_nonpositive_reward(user, device_id, mock_action, amount):
    mock_action(GetNFCInstallRewardAmountAction, amount)

    with pytest.raises(InvalidNFCRewardAmountError) as exc_info:
        await InitNFCInstallRewardAction(user=user, device_id=device_id).run()

    assert_that(exc_info.value.params, has_entries(amount=convert_then_match(Decimal, amount)))


@pytest.mark.asyncio
async def test_cannot_init_reward_exceeding_max_allowed_amount(user, device_id, mock_action):
    amount = InitNFCInstallRewardAction.max_allowed_reward_amount + Decimal('0.01')
    mock_action(GetNFCInstallRewardAmountAction, amount)

    with pytest.raises(InvalidNFCRewardAmountError) as exc_info:
        await InitNFCInstallRewardAction(user=user, device_id=device_id).run()

    assert_that(exc_info.value.params, has_entries(amount=convert_then_match(Decimal, amount)))


@pytest.mark.asyncio
async def test_reward_already_granted(user, device_id):
    await InitNFCInstallRewardAction(user=user, device_id=device_id).run()

    with pytest.raises(NFCRewardAlreadyExistsError):
        await InitNFCInstallRewardAction(user=user, device_id=device_id).run()


@pytest.mark.asyncio
async def test_bad_user_score(user, device_id, mock_action):
    mock_action(CheckUserScoreAction, BadUserScoreError)

    with pytest.raises(BadUserScoreError):
        await InitNFCInstallRewardAction(user=user, device_id=device_id).run()
