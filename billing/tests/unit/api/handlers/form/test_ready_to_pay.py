from uuid import UUID

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_entry, has_item

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.ready_to_pay import ReadyToPayAction, ReadyToPayResult
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import PaymentMethod
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind
from billing.yandex_pay.yandex_pay.utils.user_agent import UserAgentInfo

ACTUAL_UID = 222


@pytest.fixture
def user():
    return User(ACTUAL_UID)


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/is_ready_to_pay',
        APIKind.MOBILE: '/api/mobile/v1/is_ready_to_pay',
    }[api_kind]


@pytest.fixture
def mock_authentication(mocker, user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=user))


@pytest.fixture
def ready_to_pay_result() -> ReadyToPayResult:
    return {
        'is_ready_to_pay': True,
    }


@pytest.fixture
def request_json():
    return {
        'merchant_origin': 'https://best-shop.ru',
        'merchant_id': '50fd0b78-0630-4f24-a532-9e1aac5ea859',
        'existing_payment_method_required': True,
        'payment_methods': [
            {
                'type': 'CARD',
                'gateway': 'yandex-trust',
                'gateway_merchant_id': 'some_merchant',
                'allowed_auth_methods': ['CLOUD_TOKEN'],
                'allowed_card_networks': ['MASTERCARD'],
            }
        ],
    }


@pytest.fixture
def req(app, api_url, request_maker, request_json):
    return request_maker(
        client=app,
        method='post',
        path=api_url,
        json=request_json,
        headers={'User-Agent': ''},
    )


@pytest.mark.asyncio
async def test_handler_loads_request_and_dumps_response(
    req, mock_authentication, mock_action, ready_to_pay_result
):
    """
    Хендлер принимает запрос, десериализует, передает в экшен, результат сериализует и возвращает.
    """
    mock_action(ReadyToPayAction, ready_to_pay_result)

    r, json = await req.make()

    assert_that(r.status, equal_to(200))
    assert_that(json, equal_to({
        'code': 200,
        'status': 'success',
        'data': {
            'is_ready_to_pay': True,
        },
    }))


@pytest.mark.asyncio
async def test_action_receives_correct_arguments(
    req,
    mocker,
    mock_authentication,
    api_kind,
    mock_action,
    ready_to_pay_result,
    yandexpay_app,
    user,
):
    """
    Хендлер принимает запрос, десериализует, передает в экшен, результат сериализует и возвращает.
    """
    mock = mock_action(ReadyToPayAction, ready_to_pay_result)

    user_agent_info = UserAgentInfo('', {})
    mocker.patch.object(yandexpay_app.user_agent_middleware.user_agent_detector, 'detect', return_value=user_agent_info)

    await req.make()

    _, kwargs = mock.call_args
    assert_that(kwargs, equal_to(dict(
        user=user,
        merchant_id=UUID('50fd0b78-0630-4f24-a532-9e1aac5ea859'),
        merchant_origin='https://best-shop.ru',
        existing_payment_method_required=True,
        user_agent_info=user_agent_info,
        payment_methods=[PaymentMethod(
            method_type=PaymentMethodType.CARD,
            gateway='yandex-trust',
            gateway_merchant_id='some_merchant',
            allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
            allowed_card_networks=[CardNetwork.MASTERCARD],
        )],
        validate_origin=api_kind != APIKind.MOBILE,
        check_user_agent=api_kind != APIKind.MOBILE,
        forbidden_card_networks=None,
    )))


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'settings_to_overwrite', [{'API_WEB_CHECK_USER_AGENT': False}], indirect=True
)
async def test_user_agent_disable_via_settings(
    req, mock_action, ready_to_pay_result, yandexpay_app
):
    mock = mock_action(ReadyToPayAction, ready_to_pay_result)

    await req.make()

    _, kwargs = mock.call_args
    assert_that(kwargs, has_entry('check_user_agent', False))


@pytest.mark.asyncio
async def test_action_receives_correct_arguments_if_existing_payment_methods_are_not_required(
    req, api_kind, mocker, mock_action, ready_to_pay_result, yandexpay_app,
):
    """
    Хендлер принимает запрос, десериализует, передает в экшен, результат сериализует и возвращает.
    """
    mock = mock_action(ReadyToPayAction, ready_to_pay_result)

    user_agent_info = UserAgentInfo('', {})
    mocker.patch.object(yandexpay_app.user_agent_middleware.user_agent_detector, 'detect', return_value=user_agent_info)

    req.json['existing_payment_method_required'] = False
    req.json['payment_methods'] = []

    await req.make()

    _, kwargs = mock.call_args
    assert_that(kwargs, equal_to(dict(
        user=None,
        merchant_id=UUID('50fd0b78-0630-4f24-a532-9e1aac5ea859'),
        merchant_origin='https://best-shop.ru',
        existing_payment_method_required=False,
        payment_methods=[],
        validate_origin=api_kind != APIKind.MOBILE,
        user_agent_info=user_agent_info,
        check_user_agent=api_kind != APIKind.MOBILE,
        forbidden_card_networks=None,
    )))


@pytest.mark.parametrize('api_kind', [APIKind.WEB])
@pytest.mark.asyncio
async def test_calls_user_agent_detector_and_passes_result_to_action(
    req, mock_action, yandexpay_app, mocker
):
    action_mock = mock_action(ReadyToPayAction, ready_to_pay_result)

    user_agent_info = UserAgentInfo('Bless You!', {'BrowserName': 'Chrome', 'OSFamily': 'Windows'})
    detector_mock = mocker.patch.object(
        yandexpay_app.user_agent_middleware.user_agent_detector, 'detect', return_value=user_agent_info)

    req.headers['User-Agent'] = 'Bless You!'

    await req.make()

    detector_mock.assert_called_once_with('Bless You!')

    _, call_kwargs = action_mock.call_args
    assert_that(call_kwargs, has_entries({'user_agent_info': user_agent_info}))


class TestSchemaValidation:
    @pytest.mark.asyncio
    async def test_validates_gateway_merchant_id(
        self, req, mock_authentication, request_json, mock_action, ready_to_pay_result
    ):
        mock_action(ReadyToPayAction, ready_to_pay_result)
        req.json['payment_methods'][0]['gateway_merchant_id'] = ''

        r, json = await req.make()

        assert_that(r.status, equal_to(400))
        assert_that(
            json['data']['params']['payment_methods']['0'],
            has_entries({
                '_schema': has_item(has_entries({
                    'gateway_merchant_id': has_item(equal_to('String should not be empty.')),
                })),
            }),
        )

    @pytest.mark.parametrize('payment_methods', [None, []])
    @pytest.mark.asyncio
    async def test_empty_payment_methods_not_allowed_when_check_is_required(
        self, req, mock_authentication, mock_action, request_json, ready_to_pay_result, payment_methods
    ):
        mock_action(ReadyToPayAction, ready_to_pay_result)

        req.json['existing_payment_method_required'] = True
        req.json['payment_methods'] = payment_methods

        r, json = await req.make()

        assert_that(r.status, equal_to(400))
        assert_that(json, equal_to({
            'code': 400,
            'status': 'fail',
            'data': {
                'params': {
                    'payment_methods': ['Can not be empty.']
                },
                'message': 'BAD_REQUEST',
            }
        }))

    @pytest.mark.asyncio
    async def test_empty_payment_methods_is_required_field_when_check_is_required(
        self, req, mock_authentication, mock_action, request_json, ready_to_pay_result
    ):
        mock_action(ReadyToPayAction, ready_to_pay_result)

        req.json['existing_payment_method_required'] = True
        req.json.pop('payment_methods', None)

        r, json = await req.make()

        assert_that(r.status, equal_to(400))

    @pytest.mark.parametrize('payment_methods', [None, []])
    @pytest.mark.asyncio
    async def test_empty_payment_methods_allowed_when_check_is_not_required(
        self, req, mock_authentication, mock_action, request_json, ready_to_pay_result, payment_methods
    ):
        mock_action(ReadyToPayAction, ready_to_pay_result)

        req.json['existing_payment_method_required'] = False
        req.json['payment_methods'] = payment_methods

        r, json = await req.make()

        assert_that(r.status, equal_to(200))
        assert_that(json, equal_to({
            'code': 200,
            'status': 'success',
            'data': {
                'is_ready_to_pay': True,
            },
        }))

    @pytest.mark.asyncio
    async def test_validates_merchant_id(
        self, req, mock_authentication, mock_action, request_json, ready_to_pay_result
    ):
        mock_action(ReadyToPayAction, ready_to_pay_result)

        req.json['merchant_id'] = 'bad_value'

        r, json = await req.make()

        assert_that(r.status, equal_to(400))
        assert_that(json, equal_to({
            'code': 400,
            'status': 'fail',
            'data': {
                'params': {
                    'merchant_id': ['Not a valid UUID.'],
                },
                'message': 'BAD_REQUEST',
            }
        }))

    @pytest.mark.asyncio
    async def test_validates_merchant_origin(
        self, req, mock_authentication, mock_action, request_json, ready_to_pay_result
    ):
        mock_action(ReadyToPayAction, ready_to_pay_result)

        req.json['merchant_origin'] = 'not a url'

        r, json = await req.make()

        assert_that(r.status, equal_to(400))
        assert_that(json, equal_to({
            'code': 400,
            'status': 'fail',
            'data': {
                'params': {
                    'merchant_origin': ['Not a valid Origin.'],
                },
                'message': 'BAD_REQUEST',
            }
        }))
