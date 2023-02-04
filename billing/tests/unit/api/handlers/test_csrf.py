import pytest

from hamcrest import assert_that, equal_to, has_entries, has_entry

from billing.yandex_pay.yandex_pay.core.entities.user import User

FAKE_UID = 333


@pytest.fixture
def user():
    return User(FAKE_UID)


@pytest.fixture(autouse=True)
def mock_session_info(user, mocker):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=user))


@pytest.fixture
def cookies():
    return {'yandexuid': '999'}


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'settings_to_overwrite', [{'API_SWAGGER_ENABLED': True}], indirect=True
)
async def test_handler_returns_valid_csrf_token(app, cookies, user, mocker):
    fake_token = 'fake token'
    mocked_token_gen = mocker.patch(
        'sendr_auth.CsrfChecker.generate_token',
        return_value=fake_token,
    )

    response = await app.post('api/csrf_token', cookies=cookies)
    assert_that(response.status, equal_to(200))

    json_body = await response.json()
    assert_that(json_body, has_entry('data', {'token': fake_token}))
    mocked_token_gen.assert_called_once()
    _, call_kwargs = mocked_token_gen.call_args
    assert_that(call_kwargs, has_entries(user=user, yandexuid=cookies['yandexuid']))


@pytest.mark.asyncio
async def test_csrf_route_unavailable_if_swagger_off(app, cookies):
    response = await app.post('api/csrf_token', cookies=cookies)
    assert_that(response.status, equal_to(404))
