import pytest

from sendr_aiohttp import Url
from sendr_auth.entities import AuthenticationMethod, User

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.base import BaseHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.mixins.restrict_auth_method import RestrictAuthMethodMixin
from billing.yandex_pay_plus.yandex_pay_plus.api.public_app import YandexPayPlusPublicApplication


@pytest.fixture(params=list(AuthenticationMethod))
def auth_method(request):
    return request.param


@pytest.fixture
def handler(auth_method):
    class ExampleHandler(RestrictAuthMethodMixin, BaseHandler):
        allowed_auth_method = auth_method

        async def get(self):
            return self.make_response({'status': 'success'})

    return ExampleHandler


@pytest.fixture
def application(db_engine, handler, mocker) -> YandexPayPlusPublicApplication:
    # overrides the application fixture from common_conftest.py
    urls = [*YandexPayPlusPublicApplication._urls, (Url('/path', handler, 'v_test'),)]
    mocker.patch.object(YandexPayPlusPublicApplication, '_urls', urls)
    return YandexPayPlusPublicApplication(db_engine=db_engine)


@pytest.fixture
def fake_user(randn, auth_method):
    return User(randn(), auth_method=auth_method)


@pytest.fixture(autouse=True)
def mock_authentication(mocker, fake_user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=fake_user))


@pytest.mark.asyncio
async def test_correct_method_allowed(app):
    r = await app.get('/path', raise_for_status=True)

    assert_that(
        await r.json(),
        equal_to({'status': 'success'}),
    )


@pytest.mark.asyncio
async def test_invalid_method_forbidden(app, fake_user, auth_method):
    invalid_method = next(each for each in AuthenticationMethod if each != auth_method)
    fake_user.auth_method = invalid_method

    r = await app.get('/path')

    assert_that(r.status, equal_to(401))
    assert_that(
        await r.json(),
        equal_to({'data': {'message': 'ACCESS_DENIED'}, 'status': 'fail', 'code': 401}),
    )


@pytest.mark.asyncio
async def test_missing_user_forbidden(app, mocker):
    mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=None))

    r = await app.get('/path')

    assert_that(r.status, equal_to(401))
    assert_that(
        await r.json(),
        equal_to({'data': {'message': 'ACCESS_DENIED'}, 'status': 'fail', 'code': 401}),
    )
