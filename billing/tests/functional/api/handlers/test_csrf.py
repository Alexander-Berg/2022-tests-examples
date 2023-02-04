import re

import pytest

from sendr_auth import CsrfChecker
from sendr_utils import utcnow

from hamcrest import anything, assert_that, close_to, equal_to, has_entry, less_than_or_equal_to

from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.file_storage import FileStorage

FAKE_UID = 333


@pytest.fixture
def user():
    return User(FAKE_UID)


@pytest.fixture
def cookies():
    return {'yandexuid': '999', 'Session_id': 'xxx'}


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'settings_to_overwrite', [{'API_SWAGGER_ENABLED': True}], indirect=True
)
async def test_returns_valid_csrf_token(
    app, cookies, yandex_pay_settings, aioresponses_mocker, user
):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*'),
        status=200,
        payload={'status': {'value': 'VALID'}, 'uid': {'value': str(FAKE_UID)}, 'login_id': 'login_id'}
    )

    response = await app.post('/api/csrf_token', cookies=cookies)
    assert_that(response.status, equal_to(200))
    json_body = await response.json()

    token_match = has_entry('token', anything())
    assert_that(json_body, has_entry('data', token_match))

    actual_token = json_body['data']['token']
    timestamp_from_token = int(actual_token.rsplit(':', 1)[1])
    current_timestamp = int(utcnow().timestamp())
    # [now - 2 seconds] <= [token timestamp] <= now
    assert_that(timestamp_from_token, close_to(current_timestamp, delta=2))
    assert_that(timestamp_from_token, less_than_or_equal_to(current_timestamp))

    expected_token = CsrfChecker.generate_token(
        key=FileStorage().csrf_anti_forgery_key.get_actual_key(),
        user=user,
        yandexuid=cookies['yandexuid'],
        timestamp=timestamp_from_token,
    )
    assert_that(actual_token, equal_to(expected_token))
