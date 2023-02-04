from datetime import datetime
from typing import List

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.card.list import GetUserCardsAction
from billing.yandex_pay.yandex_pay.core.actions.card.sync import SyncUserCardAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card, ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind
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
            expire=datetime(2022, 1, 1, 0, 0, 0, 0)
        ),
        bin='222333',
        expiration_date=ExpirationDate(
            month=10,
            year=2031,
        ),
        is_removed=True,
        trust_card_id='trust_card_id',
    )


@pytest.fixture(autouse=True)
def mock_authentication(mocker, user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=user))


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


class TestGetUserCards:
    @pytest.fixture
    def api_url(self, api_kind):
        return {
            APIKind.WEB: '/api/v1/user_cards',
            APIKind.MOBILE: '/api/mobile/v1/user_cards',
        }[api_kind]

    @pytest.fixture
    def cards_result(self, user_card) -> List[UserCard]:
        return [user_card]

    @pytest.fixture
    def mock_get_user_cards(self, mock_action, cards_result):
        return mock_action(GetUserCardsAction, cards_result)

    @pytest.fixture
    def expected_json_body(self, cards_result):
        card = cards_result[0]
        return {
            'code': 200,
            'status': 'success',
            'data':
                {
                    'cards':
                        [{
                            'id': str(card.card_id),
                            'uid': card.owner_uid,
                            'allowed_auth_methods': ['CLOUD_TOKEN'],
                            'bin': card.bin,
                            'last4': card.last4,
                            'card_network': card.card_network.value,
                            'issuer_bank': IssuerBank.ALFABANK.value,
                            'card_art': {},
                            'expiration_date': {
                                'month': 10,
                                'year': 2031,
                            },
                            'trust_card_id': 'trust_card_id',
                        }]
                }
        }

    @pytest.mark.asyncio
    async def test_handler_should_return_cards_from_action(
        self,
        app,
        api_url,
        expected_json_body,
        mock_get_user_cards,
    ):
        r = await app.get(api_url)
        json_body = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_json_body))

    @pytest.mark.asyncio
    async def test_session_info_uid(self, app, api_url, mocker, mock_get_user_cards, user):
        method_mock = mocker.patch.object(GetUserCardsAction, '__init__', return_value=None)
        await app.get(api_url)
        method_mock.assert_called_once_with(user=user, forbidden_card_networks=None)


class TestSyncUserCard:
    @pytest.fixture
    def api_url(self, api_kind):
        return {
            APIKind.WEB: '/api/v1/sync_user_card',
            APIKind.MOBILE: '/api/mobile/v1/sync_user_card',
        }[api_kind]

    @pytest.mark.asyncio
    async def test_returned(
        self,
        app,
        api_url,
        user,
        mock_action,
        user_card,
    ):
        mock_action(SyncUserCardAction, user_card)

        r = await app.post(api_url, json={'card_id': 'trust_card_id'})
        json_body = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(
            json_body['data'],
            equal_to(
                dict(
                    id='some_id',
                    card_network=CardNetwork.MASTERCARD.value,
                    uid=10,
                    allowed_auth_methods=['CLOUD_TOKEN'],
                    last4='4122',
                    issuer_bank=IssuerBank.ALFABANK.value,
                    card_art={},
                    bin='222333',
                    is_removed=True,
                    is_expired=False,
                    expiration_date={
                        'month': 10,
                        'year': 2031,
                    },
                    trust_card_id='trust_card_id',
                )
            )
        )

    @pytest.mark.asyncio
    async def test_calls_action(
        self,
        app,
        api_url,
        user,
        mock_action,
        user_card,
    ):
        sync_card_mock = mock_action(SyncUserCardAction, user_card)

        await app.post(api_url, json={'card_id': 'trust_card_id'})

        sync_card_mock.assert_called_once_with(
            user=user,
            card_id='trust_card_id',
        )


class TestAllowedBins:
    @pytest.fixture
    def api_url(self, api_kind):
        return {
            APIKind.WEB: '/api/v1/bins/allowed',
            APIKind.MOBILE: '/api/mobile/v1/bins/allowed',
        }[api_kind]

    @pytest.fixture
    def expected_bins(self):
        return [
            '222333',
            '5512525125',
        ]

    @pytest.fixture
    def expected_json_body(self, expected_bins):
        return {
            'code': 200,
            'status': 'success',
            'data': {
                'bins': expected_bins,
            },
        }

    @pytest.fixture
    def replace_allowed_bins(self, mocker, expected_bins):
        mocker.patch('billing.yandex_pay.yandex_pay.api.handlers.form.card.ALLOWED_BINS', expected_bins)

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('replace_allowed_bins')
    async def test_should_respond_with_allowed_bins_list(
        self,
        app,
        api_url,
        storage,
        expected_json_body,
    ):
        r = await app.get(api_url)
        json_body = await r.json()

        assert r.status == 200
        assert json_body == expected_json_body
