import logging
import re
from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest
from aiohttp import TCPConnector

from sendr_utils import without_none

from hamcrest import assert_that, equal_to, has_entries, has_items, has_properties, match_equality

from pay.lib.interactions.split.client import AbstractYandexSplitClient
from pay.lib.interactions.split.entities import (
    YandexSplitConsumer,
    YandexSplitOrder,
    YandexSplitOrderCheckoutInfo,
    YandexSplitOrderMeta,
    YandexSplitOrderService,
    YandexSplitOrderServiceType,
    YandexSplitOrderStatus,
    YandexSplitPayment,
    YandexSplitPaymentPlan,
    YandexSplitPaymentPlanDetails,
    YandexSplitPaymentPlanStatus,
    YandexSplitPaymentStatus,
)
from pay.lib.interactions.split.exceptions import YandexSplitNoPlansAvailableError, YandexSplitResponseError

SPLIT_URL = 'https://bnpl.fintech.tst.yandex.net/yandex'


class YandexTestSplitClient(AbstractYandexSplitClient):
    BASE_URL = SPLIT_URL
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    REQUEST_TIMEOUT = 5.0
    CONNECTOR = TCPConnector()


@pytest.fixture
async def split_client(create_interaction_client) -> AbstractYandexSplitClient:
    client = create_interaction_client(YandexTestSplitClient)
    yield client
    await client.close()


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def currency():
    return 'XTS'


@pytest.fixture
def amount():
    return Decimal('2052.00')


@pytest.fixture
def plan_id():
    return '1'


class TestGetPaymentPlans:
    @pytest.fixture
    def url(self):
        return re.compile(f'^{SPLIT_URL}/plan/check$')

    @pytest.fixture
    def split_response(self):
        return {
            "plans": [
                {
                    "class_name": "regular_instalment_plan",
                    "constructor": "test",
                    "status": "draft",
                    "details": {
                        "deposit": "0.00",
                        "payments": [
                            {"status": "coming", "datetime": "2021-12-09T09:27:45Z", "amount": "1026.00"},
                            {"status": "coming", "datetime": "2021-12-09T09:37:45Z", "amount": "1026.00"},
                        ],
                    },
                    "user_id": "dd9b8d73-f2db-593d-5722-2d6edbdb42ba",
                    "sum": "2052.00",
                    "id": "c3b60686-b791-65d9-c069-817c20bcde9d",
                },
                {
                    "class_name": "regular_instalment_plan",
                    "constructor": "fast_loan_transfer_sums",
                    "status": "draft",
                    "details": {
                        "deposit": "1000.00",
                        "payments": [
                            {"status": "coming", "datetime": "2021-12-09T09:27:45Z", "amount": "526.00"},
                            {"status": "coming", "datetime": "2021-12-09T09:28:45Z", "amount": "526.00"},
                        ],
                    },
                    "user_id": "dd9b8d73-f2db-593d-5722-2d6edbdb42ba",
                    "sum": "2052.00",
                    "id": "898330cd-d091-4387-e035-65bb43db4762",
                },
            ]
        }

    @pytest.fixture
    def expected_client_response(self):
        user_id = 'dd9b8d73-f2db-593d-5722-2d6edbdb42ba'
        class_name = 'regular_instalment_plan'
        dt = datetime(2021, 12, 9, 9, 27, 45, tzinfo=timezone.utc)
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
                            datetime=dt + timedelta(minutes=delta),
                        )
                        for delta in (0, 10)
                    ],
                ),
            ),
            YandexSplitPaymentPlan(
                id='898330cd-d091-4387-e035-65bb43db4762',
                user_id=user_id,
                class_name=class_name,
                constructor='fast_loan_transfer_sums',
                status=YandexSplitPaymentPlanStatus.DRAFT,
                sum=Decimal('2052.00'),
                details=YandexSplitPaymentPlanDetails(
                    deposit=Decimal('1000'),
                    payments=[
                        YandexSplitPayment(
                            amount=Decimal('526.00'),
                            status=YandexSplitPaymentStatus.COMING,
                            datetime=dt + timedelta(minutes=delta),
                        )
                        for delta in (0, 1)
                    ],
                ),
            ),
        ]

    @pytest.fixture(autouse=True)
    def mock_split(self, aioresponses_mocker, url, split_response):
        return aioresponses_mocker.post(
            url=url,
            status=200,
            payload=split_response,
        )

    @pytest.mark.asyncio
    async def test_get_payment_plans(self, split_client, uid, currency, amount, expected_client_response):
        result = await split_client.get_payment_plans(uid, currency, amount, 'merchant_id')

        assert_that(result, equal_to(expected_client_response))

    @pytest.mark.asyncio
    async def test_split_called(self, mock_split, split_client, uid, currency, amount, mocker):
        merchant_id = 'merchant_id'
        login_id = 'login_id'

        await split_client.get_payment_plans(uid, currency, amount, merchant_id, login_id=login_id)

        mock_split.assert_called_once_with(
            json={'services': [{'type': 'loan', 'amount': '2052.00', 'currency': 'XTS', 'items': []}]},
            headers=match_equality(
                has_entries({'X-Yandex-Uid': str(uid), 'X-Merchant-Id': merchant_id, 'X-Login-Id': login_id})
            ),
            timeout=mocker.ANY,
        )

    @pytest.mark.asyncio
    async def test_call_logged(self, split_client, uid, currency, amount, dummy_logs):
        merchant_id = 'merchant_id'
        await split_client.get_payment_plans(uid, currency, amount, merchant_id)

        logs = dummy_logs()
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='GET_SPLIT_PAYMENT_PLANS',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=uid,
                        payload={'services': [{'type': 'loan', 'amount': '2052.00', 'currency': 'XTS', 'items': []}]},
                        merchant_id=merchant_id,
                    ),
                )
            ),
        )


class TestCreateOrder:
    @pytest.fixture
    def login_id(self, rands):
        return rands()

    @pytest.fixture
    def external_order_id(self, rands):
        return rands()

    @pytest.fixture
    def url(self):
        return re.compile(f'^{SPLIT_URL}/order/create$')

    @pytest.fixture
    def split_response(self):
        return {
            'order_id': '78c4924e-bf2e-1f41-fbe8-33800c2bca16',
            'checkout_url': 'https://test.bnpl.yandex.ru/checkout/78c4924e-bf2e-1f41-fbe8-33800c2bca16',
        }

    @pytest.fixture(autouse=True)
    def mock_split(self, aioresponses_mocker, url, split_response):
        return aioresponses_mocker.post(
            url=url,
            status=200,
            payload=split_response,
        )

    @pytest.mark.asyncio
    async def test_create_order(
        self, split_client, split_response, uid, login_id, currency, amount, external_order_id, plan_id
    ):
        expected = YandexSplitOrderCheckoutInfo(**split_response)

        result = await split_client.create_order(
            uid, login_id, currency, amount, external_order_id, 'merchant_id', plan_id=plan_id
        )

        assert_that(result, equal_to(expected))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('plus_points', [None, Decimal('10.0')])
    async def test_split_called(
        self, mock_split, split_client, uid, login_id, currency, amount, external_order_id, plus_points, mocker, plan_id
    ):
        merchant_id = 'merchant_id'
        trust_card_id = 'card-x0123'

        await split_client.create_order(
            uid,
            login_id,
            currency,
            amount,
            external_order_id,
            merchant_id,
            trust_card_id=trust_card_id,
            plus_points=plus_points,
            plan_id=plan_id,
        )

        consumer_meta = without_none(
            {
                'order_ids': [external_order_id],
                'plus_points': str(plus_points) if plus_points is not None else None,
            }
        )
        payload = {
            'order_meta': {
                'external_id': external_order_id,
                'card_id': trust_card_id,
                'plan_id': plan_id,
                'consumer_meta': consumer_meta,
            },
            'services': [
                {'type': 'loan', 'amount': '2052.00', 'currency': 'XTS', 'items': []},
            ],
        }
        mock_split.assert_called_once_with(
            json=payload,
            headers=match_equality(
                has_entries(
                    {
                        'X-Yandex-Uid': str(uid),
                        'X-Merchant-Id': merchant_id,
                        'X-Login-Id': login_id,
                    }
                )
            ),
            timeout=mocker.ANY,
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('plus_points', [None, Decimal('10.0')])
    async def test_call_logged(
        self, split_client, uid, login_id, currency, amount, external_order_id, dummy_logs, plus_points
    ):
        consumer_meta = without_none(
            {
                'order_ids': [external_order_id],
                'plus_points': str(plus_points) if plus_points is not None else None,
            }
        )
        payload = {
            'order_meta': {'external_id': external_order_id, 'consumer_meta': consumer_meta},
            'services': [
                {'type': 'loan', 'amount': '2052.00', 'currency': 'XTS', 'items': []},
            ],
        }
        merchant_id = 'merchant_id'

        await split_client.create_order(
            uid,
            login_id,
            currency,
            amount,
            external_order_id,
            merchant_id,
            plus_points=plus_points,
        )

        logs = dummy_logs()
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='CREATE_SPLIT_ORDER',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=uid,
                        payload=payload,
                        merchant_id=merchant_id,
                        plus_points=plus_points,
                    ),
                )
            ),
        )


class TestGetOrderInfo:
    @pytest.fixture
    def external_order_id(self, rands):
        return rands()

    @pytest.fixture
    def url(self):
        return re.compile(f'^{SPLIT_URL}/order/info.*$')

    @pytest.fixture
    def split_response(self, currency, external_order_id):
        return {
            'services': [
                {
                    'order_id': '78c4924e-bf2e-1f41-fbe8-33800c2bca16',
                    'currency': currency,
                    'type': 'loan',
                    'items': [],
                    'amount': '2052.00',
                    'service_id': 'fa5a2477-2ca9-fb93-1385-9259e4348122',
                }
            ],
            'status': 'failed',
            'consumer': {'title': 'Yandex.Pay', 'id': 'pay'},
            'order_meta': {
                'order_ids': [],
                'order_id': '78c4924e-bf2e-1f41-fbe8-33800c2bca16',
                'created_at': '2021-12-15T11:45:35Z',
                'merchant_id': 'pay',
                'user_id': 'c0b6f712-fc38-96d2-5767-2087aa5573c8',
                'external_id': external_order_id,
            },
        }

    @pytest.fixture
    def expected_client_response(self, currency, amount, external_order_id):
        return YandexSplitOrder(
            services=[
                YandexSplitOrderService(
                    type=YandexSplitOrderServiceType.LOAN,
                    currency=currency,
                    amount=amount,
                    items=[],
                )
            ],
            status=YandexSplitOrderStatus.FAILED,
            order_meta=YandexSplitOrderMeta(
                order_id='78c4924e-bf2e-1f41-fbe8-33800c2bca16',
                user_id='c0b6f712-fc38-96d2-5767-2087aa5573c8',
                external_id=external_order_id,
                merchant_id='pay',
                created_at=datetime(2021, 12, 15, 11, 45, 35, tzinfo=timezone.utc),
            ),
            consumer=YandexSplitConsumer(
                title='Yandex.Pay',
                id='pay',
            ),
        )

    @pytest.fixture(autouse=True)
    def mock_split(self, aioresponses_mocker, url, split_response):
        return aioresponses_mocker.get(
            url=url,
            status=200,
            payload=split_response,
        )

    @pytest.mark.asyncio
    async def test_get_order_info(self, split_client, uid, external_order_id, expected_client_response):
        result = await split_client.get_order_info(uid, external_order_id)

        assert_that(result, equal_to(expected_client_response))

    @pytest.mark.asyncio
    async def test_split_called(self, mock_split, split_client, uid, external_order_id, mocker):
        await split_client.get_order_info(uid, external_order_id)

        mock_split.assert_called_once_with(
            params={'external_id': external_order_id},
            headers=match_equality(
                has_entries(
                    {
                        'X-Yandex-Uid': str(uid),
                    }
                )
            ),
            timeout=mocker.ANY,
        )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'code,error_cls',
    [
        ('no_plans_available', YandexSplitNoPlansAvailableError),
        ('i-am-not-mapped', YandexSplitResponseError),
    ],
)
async def test_errors_properly_mapped(aioresponses_mocker, code, error_cls, split_client):
    url = f'{SPLIT_URL}/order/info'
    response = {
        'error_details': [],
        'details': {
            'http_code': 400,
            'debug_message': '/handlers.yandex.order.info.Unknown user',
            'details': '',
            'errors': ['400'],
            'meta_code': 400,
        },
        'code': code,
        'message': 'Error message',
    }
    aioresponses_mocker.get(url=url, status=400, payload=response)

    with pytest.raises(error_cls) as exc_info:
        await split_client.get('test_get', url)

    assert_that(exc_info.value, has_properties(message='Error message', params=response, status_code=400))
