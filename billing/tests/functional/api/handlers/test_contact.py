import re

import pytest
from aioresponses import CallbackResult

from sendr_auth import CsrfChecker
from sendr_utils import utcnow

from hamcrest import assert_that, contains, equal_to, has_entries

from billing.yandex_pay.yandex_pay.core.entities.user import User

FAKE_USER_TICKET = 'fake_tvm_user_ticket'
CONTACTS = [
    {
        'id': 'uid/140093777/pay/1121991',
        'owner_service': 'pay',
        'first_name': 'Ivan',
        'second_name': 'Ivanovich',
        'last_name': 'Ivanov',
        'email': 'ivanov@ya.ru',
        'phone_number': '+71234567890',
    },
    {
        'id': 'uid/140093777/pay/1121992',
        'owner_service': 'market',
        'first_name': 'Petr',
        'second_name': None,
        'last_name': 'Ivanov',
        'email': 'not valid email',
        'phone_number': '+71234567890',
    },
]


@pytest.fixture
def user():
    return User(113000)


@pytest.fixture
def params():
    res = dict(CONTACTS[0])
    del res['id']
    del res['owner_service']
    return res


@pytest.fixture
def api_url(yandex_pay_settings):
    return f'{yandex_pay_settings.PASSPORT_ADDRESSES_URL}/contact'


@pytest.fixture
def authentication(app, yandex_pay_settings, user, aioresponses_mocker):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=sessionid.*'),
        status=200,
        payload={
            'status': {'value': 'VALID'},
            'uid': {'value': user.uid},
            'login_id': 'login_id',
            'user_ticket': FAKE_USER_TICKET,
        }
    )

    key = app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
    return {
        'headers': {
            yandex_pay_settings.API_CSRF_TOKEN_HEADER: CsrfChecker.generate_token(
                timestamp=int(utcnow().timestamp()),
                key=key,
                user=user,
                yandexuid='yandexuid'
            ),
        },
        'cookies': {
            'Session_id': 'sessionid',
            'yandexuid': 'yandexuid',
        },
    }


@pytest.mark.asyncio
async def test_contact_collection(app, authentication, user, api_url, aioresponses_mocker):
    mock = aioresponses_mocker.get(
        f'{api_url}/list?user_id={user.uid}&user_type=uid',
        status=200,
        payload={
            'contacts': CONTACTS
        }
    )
    r = await app.get('api/v1/contacts', **authentication)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body['data']['results'], contains(has_entries(CONTACTS[0])))

    assert_that(
        mock.call_args_list[0][1],
        has_entries({
            'headers': has_entries({
                'x-ya-user-ticket': FAKE_USER_TICKET,
            })
        })
    )


@pytest.fixture
async def upsert_callback(params):
    def callback(url, **kwargs):
        assert_that(
            kwargs,
            has_entries({
                'json': has_entries(params),
                'headers': has_entries({
                    'x-ya-user-ticket': FAKE_USER_TICKET,
                }),
            })
        )

        return CallbackResult(payload={
            'status': 'ok',
            **kwargs['json'],
            'id': 'uid/1/pay/1',
        })
    return callback


@pytest.mark.asyncio
async def test_create_contact(app, authentication, user, params, api_url, aioresponses_mocker, upsert_callback):
    aioresponses_mocker.post(
        f'{api_url}/create?user_id={user.uid}&user_type=uid',
        status=200,
        callback=upsert_callback,
    )
    r = await app.post('api/v1/contacts', json=params, **authentication)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body['data']['contact'], has_entries(params))


@pytest.mark.asyncio
async def test_update_contact(app, authentication, user, params, api_url, aioresponses_mocker, upsert_callback):
    aioresponses_mocker.post(
        f'{api_url}/update?user_id={user.uid}&user_type=uid&id=uid/1/pay/1',
        status=200,
        callback=upsert_callback,
    )
    r = await app.put('api/v1/contacts/uid/1/pay/1', json=params, **authentication)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body['data']['contact'], has_entries(params))


@pytest.mark.asyncio
async def test_request_schema(app, authentication, user):
    params = {
        'last_name': ' ',
        'email': 'email'
    }
    r = await app.post('api/v1/contacts', json=params, **authentication)
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(json_body['data']['params'], has_entries({
        'first_name': ['Missing data for required field.'],
        'last_name': ['String should not be empty.'],
        'email': ['Not a valid email address.'],
        'phone_number': ['Missing data for required field.'],
    }))


@pytest.mark.asyncio
async def test_get_contact_by_id(app, authentication, user, api_url, aioresponses_mocker):
    mock = aioresponses_mocker.get(
        f'{api_url}/get?user_id={user.uid}&user_type=uid&id=uid/1/pay/1',
        status=200,
        payload=CONTACTS[0]
    )
    r = await app.get('api/v1/contacts/uid/1/pay/1', **authentication)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body['data']['contact'], has_entries(CONTACTS[0]))

    assert_that(
        mock.call_args_list[0][1],
        has_entries({
            'headers': has_entries({
                'x-ya-user-ticket': FAKE_USER_TICKET,
            })
        })
    )


@pytest.mark.asyncio
async def test_contact_not_found(app, authentication, user, api_url, aioresponses_mocker):
    aioresponses_mocker.get(
        f'{api_url}/get?user_id={user.uid}&user_type=uid&id=uid/1/pay/1',
        status=404,
        payload={
            'status': 'error'
        }
    )
    r = await app.get('api/v1/contacts/uid/1/pay/1', **authentication)

    assert_that(r.status, equal_to(404))


@pytest.mark.asyncio
async def test_delete_contact(app, authentication, user, api_url, aioresponses_mocker):
    mock = aioresponses_mocker.get(
        f'{api_url}/delete?user_id={user.uid}&user_type=uid&id=uid/1/pay/1',
        status=200,
        payload={
            'status': 'ok'
        }
    )
    r = await app.delete('api/v1/contacts/uid/1/pay/1', **authentication)
    assert_that(r.status, equal_to(204))

    assert_that(
        mock.call_args_list[0][1],
        has_entries({
            'headers': has_entries({
                'x-ya-user-ticket': FAKE_USER_TICKET,
            })
        })
    )
