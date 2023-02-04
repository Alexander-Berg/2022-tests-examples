from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest
from pay.lib.interactions.split.entities import (
    YandexSplitPayment, YandexSplitPaymentPlan, YandexSplitPaymentPlanDetails, YandexSplitPaymentPlanStatus,
    YandexSplitPaymentStatus
)

from sendr_pytest.matchers import convert_then_match

from hamcrest import assert_that, equal_to, match_equality

from billing.yandex_pay.yandex_pay.api.schemas.common.checkout import PaymentSheetSchema
from billing.yandex_pay.yandex_pay.core.actions.split.get_payment_plans import GetPaymentPlansAction
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind


def match_datetime(dt):
    return match_equality(convert_then_match(datetime.fromisoformat, dt))


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/split/get-plans',
        APIKind.MOBILE: '/api/mobile/v1/split/get-plans',
    }[api_kind]


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def login_id(rands):
    return rands()


@pytest.fixture
def fake_user(uid, login_id):
    return User(uid, None, login_id)


@pytest.fixture(autouse=True)
def mock_authentication(mocker, fake_user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=fake_user))


@pytest.fixture
def plan_start_time():
    return datetime(2021, 12, 9, 9, 27, 45, tzinfo=timezone.utc)


@pytest.fixture
def action_response(plan_start_time):
    user_id = 'dd9b8d73-f2db-593d-5722-2d6edbdb42ba'
    class_name = 'regular_instalment_plan'
    return [
        YandexSplitPaymentPlan(
            id='c3b60686-b791-65d9-c069-817c20bcde9d',
            user_id=user_id,
            class_name=class_name,
            constructor='test',
            status=YandexSplitPaymentPlanStatus.DRAFT,
            sum=Decimal('2052.00'),
            details=YandexSplitPaymentPlanDetails(
                deposit=Decimal('0'),
                payments=[
                    YandexSplitPayment(
                        amount=Decimal('1026.00'),
                        status=YandexSplitPaymentStatus.COMING,
                        datetime=plan_start_time + timedelta(minutes=delta),
                    )
                    for delta in (0, 10)
                ]
            )
        )
    ]


@pytest.fixture(autouse=True)
def mock_get_plans_action(mock_action, action_response):
    return mock_action(GetPaymentPlansAction, action_response)


@pytest.fixture
def request_params():
    return {
        'sheet': {
            'version': 2,
            'currency_code': 'xts',
            'country_code': 'ru',
            'merchant': {
                'id': '50fd0b78-0630-4f24-a532-9e1aac5ea859',
                'name': 'merchant-name',
                'url': 'https://url.test',
            },
            'payment_methods': [{'type': 'SPLIT'}],
            'order': {
                'id': 'order-id',
                'total': {
                    'amount': '2052.00',
                    'label': 'total_label',
                },
            },
        },
    }


@pytest.fixture
def expected_json_body(plan_start_time):
    return {
        "data": {
            "plans": [
                {
                    "id": "c3b60686-b791-65d9-c069-817c20bcde9d",
                    "payments": [
                        {
                            "amount": "1026.00",
                            "status": "coming",
                            "datetime": match_datetime(plan_start_time),
                        },
                        {
                            "amount": "1026.00",
                            "status": "coming",
                            "datetime": match_datetime(plan_start_time + timedelta(minutes=10)),
                        },
                    ],
                    "sum": "2052.00"
                }
            ]
        },
        "status": "success",
        "code": 200,
    }


@pytest.mark.asyncio
async def test_handler_should_return_payment_plan(app, api_url, request_params, expected_json_body):
    r = await app.post(api_url, json=request_params)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_json_body))


@pytest.mark.asyncio
async def test_action_called(app, api_url, request_params, fake_user, mock_get_plans_action):
    await app.post(api_url, json=request_params)

    mock_get_plans_action.assert_called_once_with(
        user=fake_user,
        sheet=PaymentSheetSchema().load(request_params['sheet']).data,
    )


@pytest.mark.asyncio
async def test_authentication_performed(app, api_url, request_params, mock_authentication):
    r = await app.post(api_url, json=request_params)

    assert_that(r.status, equal_to(200))
    mock_authentication.assert_called_once()
