import re

import pytest

from hamcrest import assert_that, equal_to, match_equality, not_none

from billing.yandex_pay.yandex_pay.core.actions.wallet.sup_registration import RegisterInstallationAction
from billing.yandex_pay.yandex_pay.core.entities.user import User


@pytest.fixture
def api_url():
    return '/api/mobile/v1/wallet/app/register_push_token'


@pytest.fixture(autouse=True)
def mock_authentication(mocker, uid):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=User(uid)))


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def request_json():
    return {
        'app_id': 'some_app_id',
        'app_version': 'some_app_version',
        'hardware_id': 'some_hardware_id',
        'push_token': 'some_push_token',
        'platform': 'some_platform',
        'device_name': 'some_device_name',
        'zone_id': 'some_zone_id',
        'notify_disabled': False,
        'active': True,
        'install_id': 'some_install_id',
        'device_id': 'some_device_id',
        'vendor_device_id': 'some_vendor_device_id',
    }


@pytest.fixture
def expected_empty_response_body():
    return {
        'status': 'success',
        'code': 200,
        'data': {},
    }


@pytest.fixture
def expected_invalid_push_token_response_body():
    return {
        'status': 'fail',
        'code': 409,
        'data': {
            'message': 'INVALID_PUSH_TOKEN',
        },
    }


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'is_huawei', [True, False]
)
async def test_should_register_installation(
    app,
    api_url,
    mocker,
    request_json,
    expected_empty_response_body,
    aioresponses_mocker,
    yandex_pay_settings,
    is_huawei,
):
    request_json['is_huawei'] = is_huawei
    aioresponses_mocker.post(
        re.compile(f'^{yandex_pay_settings.SUP_URL}/v2/registrations$'),
        status=200,
    )
    action_ctor_spy = mocker.spy(RegisterInstallationAction, '__init__')

    r = await app.post(
        api_url,
        json=request_json,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_empty_response_body))
    action_ctor_spy.assert_called_once_with(match_equality(not_none()), **request_json)


@pytest.mark.asyncio
async def test_should_pass_false_is_huawei_if_missing(
    app,
    api_url,
    mocker,
    request_json,
    aioresponses_mocker,
    yandex_pay_settings,
):
    aioresponses_mocker.post(
        re.compile(f'^{yandex_pay_settings.SUP_URL}/v2/registrations$'),
        status=200,
    )
    action_ctor_spy = mocker.spy(RegisterInstallationAction, '__init__')

    r = await app.post(
        api_url,
        json=request_json,
    )

    assert_that(r.status, equal_to(200))
    action_ctor_spy.assert_called_once_with(match_equality(not_none()), is_huawei=False, **request_json)


@pytest.mark.asyncio
async def test_should_respond_invalid_push_token_from_sup(
    app,
    api_url,
    aioresponses_mocker,
    yandex_pay_settings,
    request_json,
    expected_invalid_push_token_response_body,
):
    aioresponses_mocker.post(
        re.compile(f'^{yandex_pay_settings.SUP_URL}/v2/registrations$'),
        status=409,
    )

    r = await app.post(
        api_url,
        json=request_json,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(409))
    assert_that(json_body, equal_to(expected_invalid_push_token_response_body))
