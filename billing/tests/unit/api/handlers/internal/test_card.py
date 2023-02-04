from datetime import datetime

import pytest

from sendr_utils import utcnow

from billing.yandex_pay.yandex_pay.core.actions.card.get import GetUserCardByCardIdAction
from billing.yandex_pay.yandex_pay.core.actions.card.list import GetUserCardsAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card, ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank


@pytest.fixture
def session_info_uid():
    return 10


@pytest.fixture
def user(session_info_uid):
    return User(session_info_uid)


@pytest.fixture
def user_card() -> UserCard:
    return UserCard(
        card_id='some_id',
        owner_uid=10,
        card_network=CardNetwork.MASTERCARD,
        last4='4122',
        allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
        created=utcnow(),
        last_paid=utcnow(),
        issuer_bank=IssuerBank.ALFABANK,
        card=Card(
            trust_card_id='trust_card_id',
            owner_uid=10,
            tsp='visa',
            last4='4122',
            expire=datetime(2022, 1, 1),
        ),
        bin='222333',
        expiration_date=ExpirationDate(month=10, year=2031),
        is_removed=True,
        trust_card_id='trust_card_id',
    )


def card_json(card: UserCard):
    return {
        'card_id': str(card.card_id),
        'last4': card.last4,
        'card_network': card.card_network.value,
        'issuer_bank': IssuerBank.ALFABANK.value,
        'expiration_date': {'month': 10, 'year': 2031},
        'trust_card_id': 'trust_card_id',
    }


@pytest.mark.asyncio
async def test_get_user_cards(
    internal_app,
    mock_action,
    mock_internal_tvm,
    user,
    user_card,
):
    get_user_cards = mock_action(GetUserCardsAction, [user_card, user_card])

    r = await internal_app.get('api/internal/v1/user/cards', params={'uid': user.uid})
    json_body = await r.json()

    assert r.status == 200
    assert json_body == {
        'code': 200,
        'status': 'success',
        'data': {
            'cards': [
                card_json(user_card),
                card_json(user_card),
            ]
        }
    }
    get_user_cards.assert_called_once_with(user=user)


@pytest.mark.asyncio
async def test_get_user_card_by_card_id(
    internal_app,
    mock_action,
    mock_internal_tvm,
    user,
    user_card,
):
    get_user_cards_by_id = mock_action(GetUserCardByCardIdAction, user_card)

    r = await internal_app.get(f'api/internal/v1/user/cards/{user_card.card_id}', params={'uid': user.uid})
    json_body = await r.json()

    assert r.status == 200
    assert json_body == {
        'code': 200,
        'status': 'success',
        'data': card_json(user_card),
    }
    get_user_cards_by_id.assert_called_once_with(user=user, card_id=user_card.card_id, skip_trust_if_possible=False)
