from datetime import datetime, timedelta

import pytest

from billing.yandex_pay.yandex_pay.core.actions.tokenization.mastercard import MastercardTokenizationAction
from billing.yandex_pay.yandex_pay.core.actions.tokenization.sync_user_tokens import SyncUserTokensAction
from billing.yandex_pay.yandex_pay.core.actions.tokenization.visa import VisaTokenizationAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import CoreSyncTokensError
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank

MODULE_PREFIX = 'billing.yandex_pay.yandex_pay.core.actions.tokenization.sync_user_tokens.'


@pytest.fixture(params=(TSPType.MASTERCARD, TSPType.VISA))
def tsp_type(request):
    return request.param


@pytest.fixture
def owner_uid():
    return 42


@pytest.fixture
def user(owner_uid):
    return User(owner_uid)


@pytest.fixture
async def card(storage, owner_uid, tsp_type):
    return await storage.card.create(Card(
        trust_card_id='test',
        owner_uid=owner_uid,
        tsp=tsp_type,
        expire=datetime.utcnow() + timedelta(days=1),
        last4='2222',
        issuer_bank='ALFA',
    ))


@pytest.fixture
def enrollment_entity(card: Card) -> Enrollment:
    return Enrollment(
        card_id=card.card_id,
        merchant_id=None,
        tsp_card_id='1',
        tsp_token_id='1',
        tsp_token_status=TSPTokenStatus.ACTIVE,
        card_last4=card.last4,
        expire=card.expire,
    )


@pytest.fixture(autouse=True)
def mock_sync_user_cards_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(MODULE_PREFIX + 'SyncUserCardsFromTrustAction')
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


def get_mock_mastercard_tokenization_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(MODULE_PREFIX + 'MastercardTokenizationAction')
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


def get_mock_visa_tokenization_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(MODULE_PREFIX + 'VisaTokenizationAction')
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


def get_mock_mastercard_delete_token_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(MODULE_PREFIX + 'MastercardDeleteTokenAction')
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


def get_mock_visa_delete_token_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(MODULE_PREFIX + 'VisaDeleteTokenAction')
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


@pytest.fixture
def mock_tokenization_action(tsp_type, mocker):
    if tsp_type == TSPType.MASTERCARD:
        return get_mock_mastercard_tokenization_action(mocker)
    elif tsp_type == TSPType.VISA:
        return get_mock_visa_tokenization_action(mocker)


@pytest.fixture
def mock_delete_token_action(tsp_type, mocker):
    if tsp_type == TSPType.MASTERCARD:
        return get_mock_mastercard_delete_token_action(mocker)
    elif tsp_type == TSPType.VISA:
        return get_mock_visa_delete_token_action(mocker)


@pytest.mark.asyncio
async def test_should_sync_cards_before_tokenization(mock_sync_user_cards_action, user):
    await SyncUserTokensAction(user=user).run()

    mock_sync_user_cards_action.assert_called_once_with(user=user)
    mock_sync_user_cards_action.return_value.run.assert_awaited_once()


@pytest.mark.asyncio
async def test_should_tokenize_if_token_not_exists(
    card: Card, user, mock_tokenization_action, storage
):
    await SyncUserTokensAction(user=user).run()

    mock_tokenization_action.assert_called_once_with(card_id=card.card_id)
    mock_tokenization_action.return_value.run.assert_awaited_once()


@pytest.mark.asyncio
async def test_should_not_tokenize_if_tokenization_disabled(
    user, mock_tokenization_action, mock_delete_token_action, yandex_pay_settings
):
    yandex_pay_settings.TASKQ_SYNC_CARDS_TOKENIZATION_ENABLED = 0

    await SyncUserTokensAction(user=user).run()

    mock_tokenization_action.assert_not_called()
    mock_delete_token_action.assert_not_called()


@pytest.mark.asyncio
async def test_should_not_tokenize_if_token_exists(
    card: Card, user, mock_tokenization_action, storage, enrollment_entity
):
    await storage.enrollment.create(enrollment_entity)

    await SyncUserTokensAction(user=user).run()

    mock_tokenization_action.assert_not_called()


@pytest.mark.asyncio
async def test_should_not_tokenize_removed_card(
    card, user, storage, mock_tokenization_action
):
    card.is_removed = True
    await storage.card.save(card)

    await SyncUserTokensAction(user=user).run()

    mock_tokenization_action.assert_not_called()


@pytest.mark.asyncio
async def test_should_not_tokenize_expired_card(
    card, user, storage, mock_tokenization_action
):
    card.expire = datetime.utcnow() - timedelta(days=1)
    await storage.card.save(card)

    await SyncUserTokensAction(user=user).run()

    mock_tokenization_action.assert_not_called()


@pytest.mark.asyncio
async def test_should_detokenize_card(
    mock_delete_token_action,
    card: Card,
    storage,
    user,
    mock_tokenization_action,
    enrollment_entity,
):
    card.is_removed = True
    await storage.card.save(card)
    enrollment = await storage.enrollment.create(enrollment_entity)

    await SyncUserTokensAction(user=user).run()

    mock_delete_token_action.assert_called_once_with(enrollment=enrollment, force_delete=True)
    mock_delete_token_action.return_value.run.assert_awaited_once()
    mock_tokenization_action.assert_not_called()


@pytest.mark.asyncio
async def test_should_not_detokenize_card_if_it_is_not_removed(
    mock_delete_token_action,
    card: Card,
    storage,
    user,
    mock_tokenization_action,
    enrollment_entity,
):
    await storage.enrollment.create(enrollment_entity)

    await SyncUserTokensAction(user=user).run()

    mock_delete_token_action.assert_not_called()
    mock_tokenization_action.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize('token_status', [t for t in TSPTokenStatus if t != TSPTokenStatus.ACTIVE])
async def test_should_not_detokenize_card_with_not_active_enrollment(
    mock_delete_token_action,
    card: Card,
    storage,
    user,
    token_status,
    enrollment_entity,
):
    card.is_removed = True
    await storage.card.save(card)
    enrollment_entity.tsp_token_status = token_status
    await storage.enrollment.create(enrollment_entity)

    await SyncUserTokensAction(user=user).run()

    mock_delete_token_action.assert_not_called()


@pytest.mark.asyncio
async def test_should_defer_exception_and_process_all_cards(
    tsp_type,
    owner_uid,
    user,
    card,
    enrollment_entity,
    storage,
    mock_tokenization_action,
    mock_delete_token_action,
):
    mock_tokenization_action.side_effect = Exception
    mock_delete_token_action.side_effect = Exception
    card.is_removed = True
    await storage.card.save(card)
    await storage.enrollment.create(enrollment_entity)
    await storage.card.create(Card(
        trust_card_id='another_trust_id',
        owner_uid=owner_uid,
        tsp=tsp_type,
        expire=datetime.utcnow() + timedelta(days=1),
        last4='3333',
        issuer_bank='TINKOFF',
    ))

    with pytest.raises(CoreSyncTokensError):
        await SyncUserTokensAction(user=user).run()

    mock_tokenization_action.assert_called_once()
    mock_delete_token_action.assert_called_once()


@pytest.mark.asyncio
@pytest.mark.parametrize('tsp,issuer', [
    (TSPType.MASTERCARD, IssuerBank.VTB),
    (TSPType.VISA, IssuerBank.VTB),
    (TSPType.MASTERCARD, IssuerBank.SBERBANK),
])
async def test_should_not_tokenize_if_bank_and_tsp_in_blacklist(
    user, tsp, issuer, storage, owner_uid, mock_action, yandex_pay_settings
):
    visa = mock_action(VisaTokenizationAction)
    mastercard = mock_action(MastercardTokenizationAction)

    yandex_pay_settings.BANKS_TSP_TOKENIZATION_BLACKLIST = {
        'VTB': ['visa', 'mastercard'],
        'SBERBANK': ['mastercard'],
    }

    await storage.card.create(Card(
        trust_card_id='test',
        owner_uid=owner_uid,
        tsp=tsp,
        expire=datetime.utcnow() + timedelta(days=1),
        last4='2222',
        issuer_bank=issuer.name,
    ))

    await SyncUserTokensAction(user=user).run()

    visa.assert_not_called()
    mastercard.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize('tsp,issuer', [
    (TSPType.MASTERCARD, IssuerBank.TINKOFF),
    (TSPType.VISA, IssuerBank.TINKOFF),
    (TSPType.VISA, IssuerBank.ALFABANK),
])
async def test_should_tokenize_if_bank_and_tsp_not_in_blacklist(
    user, tsp, issuer, storage, owner_uid, mock_action, yandex_pay_settings
):
    visa = mock_action(VisaTokenizationAction)
    mastercard = mock_action(MastercardTokenizationAction)

    yandex_pay_settings.BANKS_TSP_TOKENIZATION_BLACKLIST = {
        'VTB': ['visa', 'mastercard'],
        'SBERBANK': ['mastercard'],
    }

    await storage.card.create(Card(
        trust_card_id='test',
        owner_uid=owner_uid,
        tsp=tsp,
        expire=datetime.utcnow() + timedelta(days=1),
        last4='2222',
        issuer_bank=issuer.name,
    ))

    await SyncUserTokensAction(user=user).run()

    if tsp == TSPType.VISA:
        visa.assert_called_once()
    if tsp == TSPType.MASTERCARD:
        mastercard.assert_called_once()
