import re

import pytest

from sendr_auth import CsrfChecker
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind
from billing.yandex_pay.yandex_pay.utils.normalize_banks import normalize_card_issuer


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/user_cards',
        APIKind.MOBILE: '/api/mobile/v1/user_cards',
    }[api_kind]


@pytest.fixture
def authentication(api_kind, app, yandex_pay_settings, owner_uid, aioresponses_mocker):
    if api_kind == APIKind.WEB:
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=sessionid.*'),
            status=200,
            payload={'status': {'value': 'VALID'}, 'uid': {'value': owner_uid}, 'login_id': 'login_id'},
        )

        key = app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
        return {
            'headers': {
                yandex_pay_settings.API_CSRF_TOKEN_HEADER: CsrfChecker.generate_token(
                    timestamp=int(utcnow().timestamp()),
                    key=key,
                    user=User(owner_uid),
                    yandexuid='yandexuid'
                ),
            },
            'cookies': {
                'Session_id': 'sessionid',
                'yandexuid': 'yandexuid',
            },
        }

    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=oauth.*'),
        status=200,
        payload={
            'status': {'value': 'VALID'},
            'oauth': {'uid': owner_uid, 'client_id': 'client_id'},
            'login_id': 'login_id',
        },
    )
    return {
        'headers': {
            'Authorization': 'OAuth 123',
        }
    }


@pytest.fixture
def owner_uid(randn):
    return randn()


@pytest.fixture
def expected_json_body(owner_uid, yandex_pay_settings):

    def get_id(card_id, trust_card_id, auth):
        if auth == [AuthMethod.PAN_ONLY]:
            return trust_card_id
        return card_id

    cards = []
    for card in yandex_pay_settings.SANDBOX_CARDS:
        card_art = {}
        cards.append(
            {
                'card_network': card['card_network'].value.upper(),
                'last4': card['last4'],
                'bin': card['pan'][:6],
                'card_art': card_art,
                'issuer_bank': normalize_card_issuer(card['bank']).value,
                'id': get_id(str(card['card_id']), card['trust_card_id'], card['auth']),
                'uid': owner_uid,
                'allowed_auth_methods': [a.value for a in card['auth']],
                'expiration_date': {
                    'month': card['expire'].month,
                    'year': card['expire'].year,
                },
                'trust_card_id': card['trust_card_id'],
            }
        )
    return {
        'code': 200,
        'status': 'success',
        'data': {
            'cards': cards
        }
    }


@pytest.mark.asyncio
async def test_should_respond_user_cards(
    sandbox_app,
    api_url,
    authentication,
    yandex_pay_settings,
    aioresponses_mocker,
    expected_json_body,
    owner_uid,
):
    r = await sandbox_app.get(api_url, **authentication)
    json_body = await r.json()
    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_json_body))
