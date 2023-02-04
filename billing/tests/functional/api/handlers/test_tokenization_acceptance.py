import re
from datetime import timezone

import pytest

from sendr_auth import CsrfChecker
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.tokenization_acceptance import TokenizationAcceptance
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/tokenization/acceptance',
        APIKind.MOBILE: '/api/mobile/v1/tokenization/acceptance',
    }[api_kind]


@pytest.fixture
def authentication(api_kind, app, yandex_pay_settings, session_uid, aioresponses_mocker):
    if api_kind == APIKind.WEB:
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=sessionid.*'),
            status=200,
            payload={'status': {'value': 'VALID'}, 'uid': {'value': session_uid}, 'login_id': 'login_id'}
        )

        key = app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
        return {
            'headers': {
                yandex_pay_settings.API_CSRF_TOKEN_HEADER: CsrfChecker.generate_token(
                    timestamp=int(utcnow().timestamp()),
                    key=key,
                    user=User(session_uid),
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
            'oauth': {'uid': session_uid, 'client_id': 'client_id'},
            'login_id': 'login_id',
        }
    )
    return {
        'headers': {
            'Authorization': 'OAuth 123',
        }
    }


@pytest.fixture
def session_uid(unique_rand, randn):
    return unique_rand(randn)


def get_expected_json_body(acceptance):
    return {
        'code': 200,
        'status': 'success',
        'data':
            {
                'accepted': True,
                'accept_date': acceptance.accept_date.astimezone(timezone.utc).isoformat(),
            }
    }


@pytest.fixture
def expected_conflict_json_body():
    return {
        'code': 409,
        'status': 'fail',
        'data':
            {
                'message': 'ALREADY_EXIST',
            }
    }


@pytest.fixture
def expected_no_acceptance_json_body():
    return {
        'code': 200,
        'status': 'success',
        'data':
            {
                'accepted': False,
            }
    }


@pytest.mark.asyncio
async def test_handler_should_respond_with_created_result(
    app,
    api_url,
    authentication,
    storage,
    session_uid,
):
    r = await app.post(api_url, **authentication)
    json_body = await r.json()

    created = await storage.tokenization_acceptance.get(session_uid)

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(get_expected_json_body(created)))


@pytest.mark.asyncio
async def test_handler_should_respond_conflict_on_duplication(
    app,
    storage,
    api_url,
    authentication,
    session_uid,
    expected_conflict_json_body,
):
    await storage.tokenization_acceptance.create(
        TokenizationAcceptance(
            uid=session_uid,
            user_ip='test',
        )
    )

    r = await app.post(api_url, **authentication)
    json_body = await r.json()

    assert_that(r.status, equal_to(409))
    assert_that(json_body, equal_to(expected_conflict_json_body))


@pytest.mark.asyncio
async def test_handler_should_respond_get_with_exist_acceptance(
    app,
    storage,
    api_url,
    authentication,
    session_uid,
):
    exist_acceptance = await storage.tokenization_acceptance.create(
        TokenizationAcceptance(
            uid=session_uid,
            user_ip='test',
        )
    )

    r = await app.get(api_url, **authentication)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(get_expected_json_body(exist_acceptance)))


@pytest.mark.asyncio
async def test_handler_should_respond_not_accepted_if_acceptance_not_exist(
    app,
    api_url,
    authentication,
    expected_no_acceptance_json_body,
):
    r = await app.get(api_url, **authentication)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_no_acceptance_json_body))
