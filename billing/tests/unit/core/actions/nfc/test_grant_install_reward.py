from decimal import Decimal
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.create_transaction import EnsureCashbackAccountAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.grant_install_reward import GrantNFCInstallRewardAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions import BlackBoxClient, TrustPaymentsClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments import TrustPayment, YandexPlusAccount
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.enums import TrustPaymentStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import NFCRewardTransactionState
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.nfc_install_reward import NFCInstallReward


@pytest.fixture
def reward_id():
    return uuid4()


@pytest.fixture
def uid():
    return 322


@pytest.fixture
def trust_account_id():
    return str(uuid4())


@pytest.fixture
async def customer(storage, uid):
    return await storage.customer.create(Customer(uid=uid))


@pytest.fixture
async def reward(reward_id, storage, customer: Customer):
    return await storage.nfc_install_reward.create(
        NFCInstallReward(uid=customer.uid, device_id='some_id', amount=Decimal('10.0'), reward_id=reward_id)
    )


@pytest.fixture
def purchase_token():
    return 'purchase_token'


@pytest.fixture
def billing_payload(yandex_pay_plus_settings):
    return {
        'cashback_service': 'yapay',
        'cashback_type': 'nontransaction',
        'has_plus': 'true',
        'service_id': '1024',
        'issuer': 'marketing',
        'campaign_name': yandex_pay_plus_settings.CASHBACK_TRANSACTION_CAMPAIGN_NAME,
        'ticket': 'NEWSERVICE-1591',
        'product_id': yandex_pay_plus_settings.CASHBACK_TRANSACTION_PRODUCT_ID,
    }


@pytest.fixture(autouse=True)
def mock_ensure_cashback_account_action(mock_action, uid, trust_account_id):
    return mock_action(
        EnsureCashbackAccountAction,
        action_result=CashbackAccount(
            uid=uid,
            currency='RUB',
            trust_account_id=trust_account_id,
        ),
    )


@pytest.fixture(autouse=True)
def mock_have_plus(mocker):
    return mocker.patch.object(BlackBoxClient, 'have_plus', mocker.AsyncMock(return_value=True))


@pytest.fixture(autouse=True)
def mock_create_plus_transaction(mocker, purchase_token):
    return mocker.patch.object(
        TrustPaymentsClient,
        'create_plus_transaction',
        mocker.AsyncMock(return_value=purchase_token)
    )


@pytest.fixture(autouse=True)
def mock_start_plus_transaction(mocker):
    return mocker.patch.object(
        TrustPaymentsClient,
        'start_plus_transaction',
        mocker.AsyncMock(return_value=TrustPayment(payment_status=TrustPaymentStatus.STARTED))
    )


def test_serialize(reward_id):
    action = GrantNFCInstallRewardAction(reward_id=reward_id)
    assert_that(
        GrantNFCInstallRewardAction.serialize_kwargs(action._init_kwargs),
        equal_to({'reward_id': str(reward_id)})
    )


def test_deserialize(reward_id):
    action = GrantNFCInstallRewardAction(reward_id=reward_id)
    serialized = GrantNFCInstallRewardAction.serialize_kwargs(action._init_kwargs)
    deserialized = GrantNFCInstallRewardAction.deserialize_kwargs(serialized)

    action_from_serialized = GrantNFCInstallRewardAction(**deserialized)
    assert_that(action._init_kwargs, equal_to(action_from_serialized._init_kwargs))


@pytest.mark.asyncio
async def test_should_call_has_plus_with_expected_uid(reward: NFCInstallReward, mock_have_plus):
    await GrantNFCInstallRewardAction(reward_id=reward.reward_id).run()

    mock_have_plus.assert_awaited_once_with(uid=reward.uid, user_ip='127.0.0.1')


@pytest.mark.asyncio
async def test_should_call_ensure_cashback_account_action_with_expected_args(
    reward: NFCInstallReward,
    mock_ensure_cashback_account_action,
):
    await GrantNFCInstallRewardAction(reward_id=reward.reward_id).run()

    mock_ensure_cashback_account_action.assert_called_once_with(uid=reward.uid, currency='RUB')


@pytest.mark.asyncio
async def test_should_not_create_transaction_for_zero_reward_amount(reward: NFCInstallReward, storage):
    reward.amount = Decimal('0')
    await storage.nfc_install_reward.save(reward)

    await GrantNFCInstallRewardAction(reward_id=reward.reward_id).run()

    reward = await storage.nfc_install_reward.get(reward.reward_id)
    assert reward.transaction_state == NFCRewardTransactionState.NOT_CREATED
    assert reward.trust_purchase_token is None


@pytest.mark.asyncio
async def test_should_commit_created_transaction_if_start_fails(
    reward: NFCInstallReward,
    storage,
    mocker,
    purchase_token,
):
    mocker.patch.object(
        TrustPaymentsClient,
        'start_plus_transaction',
        mocker.AsyncMock(side_effect=Exception)
    )

    with pytest.raises(Exception):
        await GrantNFCInstallRewardAction(reward_id=reward.reward_id).run()

    reward = await storage.nfc_install_reward.get(reward.reward_id)
    assert reward.transaction_state == NFCRewardTransactionState.CREATED
    assert reward.trust_purchase_token == purchase_token


@pytest.mark.asyncio
async def test_should_resume_transaction_if_it_was_created_but_not_started(
    reward: NFCInstallReward,
    storage,
    mock_start_plus_transaction,
):
    reward.transaction_state = NFCRewardTransactionState.CREATED
    reward.trust_purchase_token = 'token'
    await storage.nfc_install_reward.save(reward)

    await GrantNFCInstallRewardAction(reward_id=reward.reward_id).run()

    reward = await storage.nfc_install_reward.get(reward.reward_id)
    assert reward.transaction_state == NFCRewardTransactionState.STARTED
    mock_start_plus_transaction.assert_awaited_once()


@pytest.mark.asyncio
async def test_should_create_and_start_transaction_with_expected_args(
    reward: NFCInstallReward,
    mock_create_plus_transaction,
    mock_start_plus_transaction,
    billing_payload,
    purchase_token,
    trust_account_id,
    yandex_pay_plus_settings,
):
    await GrantNFCInstallRewardAction(reward_id=reward.reward_id).run()

    mock_create_plus_transaction.assert_awaited_once_with(
        account=YandexPlusAccount(
            account_id=trust_account_id,
            uid=reward.uid,
            currency='RUB',
        ),
        product_id=yandex_pay_plus_settings.CASHBACK_TRANSACTION_PRODUCT_ID,
        amount=10,
        billing_payload=billing_payload,
    )
    mock_start_plus_transaction.assert_awaited_once_with(purchase_token=purchase_token)


@pytest.mark.asyncio
async def test_should_do_nothing_if_transaction_already_started(
    reward: NFCInstallReward,
    storage,
    mock_create_plus_transaction,
    mock_start_plus_transaction,
    mock_have_plus,
):
    reward.transaction_state = NFCRewardTransactionState.STARTED
    await storage.nfc_install_reward.save(reward)

    await GrantNFCInstallRewardAction(reward_id=reward.reward_id).run()

    mock_create_plus_transaction.assert_not_awaited()
    mock_start_plus_transaction.assert_not_awaited()
    mock_have_plus.assert_not_awaited()
