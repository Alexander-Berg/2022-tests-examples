import re
import uuid
from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import aiohttp.client_exceptions
import pytest

from sendr_interactions.exceptions import InteractionResponseError
from sendr_utils import json_value

from hamcrest import assert_that, equal_to, has_entries, none, not_, not_none

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.interactions import YandexPayPlusClient
from billing.yandex_pay.yandex_pay.interactions.plus_backend.entities import (
    OrderStatus, PlusOrder, YandexPayPlusMerchant
)
from billing.yandex_pay.yandex_pay.interactions.plus_backend.exceptions import (
    BaseYandexPayPlusInteractionError, OrderAlreadyExistsInteractionError, OrderEventAlreadyExistsError
)


@pytest.fixture
async def plus_client(create_client) -> YandexPayPlusClient:
    client = create_client(YandexPayPlusClient)
    yield client
    await client.close()


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def tvm_ticket(rands):
    return rands()


class TestCreateOrder:
    currency = 'RUB'

    @pytest.fixture
    def order_id(self, randn):
        return randn()

    @pytest.fixture
    def message_id(self, rands):
        return rands()

    @pytest.fixture
    def merchant(self):
        return YandexPayPlusMerchant(
            id=uuid4(),
            name='merchant-name',
            url='http://url.test',
        )

    @pytest.fixture
    def psp_id(self):
        return uuid4()

    @pytest.fixture
    def url(self, yandex_pay_settings):
        return re.compile(f'^{yandex_pay_settings.PAY_PLUS_API_URL}/api/v1/orders$')

    @pytest.fixture
    def plus_order(self, order_id, uid, message_id, psp_id, merchant):
        return PlusOrder(
            order_id=order_id,
            uid=uid,
            message_id=message_id,
            currency=self.currency,
            amount=Decimal('100.00'),
            cashback=Decimal('5.00'),
            cashback_category=Decimal('0.05'),
            status=OrderStatus.NEW,
            psp_id=psp_id,
            merchant_id=merchant.id,
            payment_method_type=PaymentMethodType.CARD,
        )

    @pytest.fixture
    def mock_backend(self, aioresponses_mocker, url, plus_order):
        return aioresponses_mocker.post(
            url=url,
            status=200,
            payload={'data': json_value(plus_order)},
        )

    @pytest.fixture
    def params(self, uid, message_id, merchant, psp_id):
        return dict(
            uid=uid,
            message_id=message_id,
            merchant=merchant,
            psp_id=psp_id,
            currency=self.currency,
            amount=Decimal('1.00'),
            trust_card_id='card-x123abc',
            last4='1234',
            country_code='RUS',
            order_basket={
                'id': 'some_id',
            },
            card_network='visa',
            cashback_category_id='0.5',
            card_id=uuid.UUID('084c1313-f2aa-412c-b73f-83623514ef40'),
            antifraud_external_id='ext_id',
            payment_method_type=PaymentMethodType.CARD,
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('payment_method_type', list(PaymentMethodType))
    async def test_order_created(
        self,
        plus_client,
        uid,
        message_id,
        merchant,
        psp_id,
        params,
        mock_backend,
        plus_order,
        payment_method_type,
    ):
        params['payment_method_type'] = payment_method_type

        response = await plus_client.create_order(**params)

        assert_that(response, equal_to(plus_order))

        mock_backend.assert_called_once()
        assert_that(
            mock_backend.call_args.kwargs['json'],
            equal_to(
                {
                    'uid': uid,
                    'message_id': message_id,
                    'merchant': {
                        'id': str(merchant.id),
                        'name': 'merchant-name',
                        'url': 'http://url.test',
                    },
                    'psp_id': str(psp_id),
                    'currency': self.currency,
                    'amount': '1.00',
                    'trust_card_id': 'card-x123abc',
                    'last4': '1234',
                    'country_code': 'RUS',
                    'order_basket': {
                        'id': 'some_id',
                    },
                    'card_network': 'visa',
                    'cashback_category_id': '0.5',
                    'card_id': '084c1313-f2aa-412c-b73f-83623514ef40',
                    'antifraud_external_id': 'ext_id',
                    'payment_method_type': payment_method_type.value,
                },
            )
        )

    @pytest.mark.asyncio
    async def test_none_card_id(
        self,
        plus_client,
        uid,
        message_id,
        merchant,
        psp_id,
        mock_backend,
        params,
    ):
        params['card_id'] = None

        await plus_client.create_order(**params)

        mock_backend.assert_called_once()
        assert_that(
            mock_backend.call_args.kwargs['json'],
            equal_to(
                {
                    'uid': uid,
                    'message_id': message_id,
                    'merchant': {
                        'id': str(merchant.id),
                        'name': 'merchant-name',
                        'url': 'http://url.test',
                    },
                    'psp_id': str(psp_id),
                    'currency': self.currency,
                    'amount': '1.00',
                    'trust_card_id': 'card-x123abc',
                    'last4': '1234',
                    'country_code': 'RUS',
                    'order_basket': {
                        'id': 'some_id',
                    },
                    'card_network': 'visa',
                    'cashback_category_id': '0.5',
                    'card_id': None,
                    'antifraud_external_id': 'ext_id',
                    'payment_method_type': PaymentMethodType.CARD.value,
                },
            )
        )

    @pytest.mark.asyncio
    async def test_order_already_exists(
        self,
        plus_client,
        aioresponses_mocker,
        params,
        url,
    ):
        aioresponses_mocker.post(
            url=url,
            status=409,
            payload={'data': {'message': 'ORDER_ALREADY_EXISTS'}},
        )

        with pytest.raises(OrderAlreadyExistsInteractionError):
            await plus_client.create_order(**params)

    @pytest.mark.asyncio
    async def test_backend_throws_generic_error(
        self,
        plus_client,
        aioresponses_mocker,
        params,
        url,
    ):
        aioresponses_mocker.post(
            url=url,
            status=409,
            body='not a json',
            content_type='text/plain',
        )

        with pytest.raises(InteractionResponseError):
            await plus_client.create_order(**params)


class TestGetUserCashbackAmount:
    @pytest.fixture
    def params(self, tvm_ticket):
        return {
            'user_ticket': tvm_ticket,
            'merchant': YandexPayPlusMerchant(
                id=uuid4(),
                name='merchant-name',
                url='http://url.test'
            ),
            'psp_id': uuid4(),
            'currency': 'XTS',
            'amount': Decimal('100'),
        }

    @pytest.mark.asyncio
    async def test_success(
        self,
        plus_client,
        aioresponses_mocker,
        params,
        tvm_ticket,
    ):
        mock = aioresponses_mocker.post(
            re.compile(r'^https?://.+/api/v1/cashback$'),
            status=200,
            payload={'status': 'success', 'code': 200, 'data': {'category': '0.05', 'amount': '100'}},
        )

        response = await plus_client.get_user_cashback_amount(**params)

        assert_that(
            response,
            equal_to({'category': '0.05', 'amount': '100'})
        )
        mock.assert_called_once()
        _, call_kwargs = mock.call_args_list[0]
        assert_that(
            call_kwargs,
            has_entries(
                json=has_entries(
                    amount='100',
                    psp_id=str(params['psp_id']),
                    currency='XTS',
                    merchant={
                        'id': str(params['merchant'].id),
                        'name': 'merchant-name',
                        'url': 'http://url.test',
                    },
                    trust_card_id=None,
                ),
                headers=has_entries({
                    'x-ya-user-ticket': tvm_ticket,
                })
            )
        )

    @pytest.mark.asyncio
    async def test_no_ticket(
        self,
        plus_client,
        aioresponses_mocker,
        params,
    ):
        mock = aioresponses_mocker.post(
            re.compile(r'^https?://.+/api/v1/cashback$'),
            status=200,
            payload={'status': 'success', 'code': 200, 'data': {'category': '0.05', 'amount': '100'}},
        )
        params['user_ticket'] = None

        await plus_client.get_user_cashback_amount(**params)

        mock.assert_called_once()
        _, call_kwargs = mock.call_args_list[0]
        assert_that(
            call_kwargs,
            has_entries(
                headers=not_(
                    has_entries({
                        'x-ya-user-ticket': not_none(),
                    })
                )
            )
        )


class TestUpdateOrderStatus:
    @pytest.fixture
    def message_id(self):
        return (
            '1:gAAAAABg_oVT6LWvuqqQqNQ-4yW2Pfilykabou_Je37nRuirXXqgAWz0VV55a3bnKc-'
            'qC9z31Lvqj7EWhJjR4Wd9oyRjDg8pqJ_99QVeGSQkwHxugqHlJf31dn7lKtCAcfIrxycjynm6'
        )

    @pytest.fixture
    def url(self, yandex_pay_settings, message_id):
        return re.compile(
            f'^{yandex_pay_settings.PAY_PLUS_API_URL}/api/v1/orders/{message_id}/status$'
        )

    @pytest.mark.asyncio
    async def test_success(
        self,
        plus_client,
        aioresponses_mocker,
        url,
        message_id,
    ):
        mock = aioresponses_mocker.patch(
            url=url,
            status=200,
            payload={},
        )

        response = await plus_client.update_order_status(
            message_id=message_id,
            amount=Decimal('42.00'),
            event_time=datetime(2021, 1, 2, 3, 4, 5, tzinfo=timezone.utc),
            status=OrderStatus.SUCCESS,
            payment_id='123',
            recurring=True,
        )

        assert_that(response, none())

        mock.assert_called_once()
        _, call_kwargs = mock.call_args_list[0]
        assert_that(
            call_kwargs,
            has_entries(
                json=equal_to(dict(
                    amount=str('42.00'),
                    status='success',
                    event_time='2021-01-02T03:04:05+00:00',
                    payment_id='123',
                    recurring=True,
                )),
            )
        )

    @pytest.mark.asyncio
    async def test_empty_amount(
        self,
        plus_client,
        aioresponses_mocker,
        url,
        message_id,
    ):
        mock = aioresponses_mocker.patch(
            url=url,
            status=200,
            payload={},
        )

        response = await plus_client.update_order_status(
            message_id=message_id,
            amount=None,
            event_time=datetime(2021, 1, 2, 3, 4, 5, tzinfo=timezone.utc),
            status=OrderStatus.SUCCESS,
        )

        assert_that(response, none())

        mock.assert_called_once()
        _, call_kwargs = mock.call_args_list[0]
        assert_that(
            call_kwargs,
            has_entries(
                json=has_entries(
                    amount=None,
                    status='success',
                    event_time='2021-01-02T03:04:05+00:00',
                ),
            )
        )

    @pytest.mark.asyncio
    async def test_order_event_already_exists(
        self, plus_client, aioresponses_mocker, url, message_id
    ):
        aioresponses_mocker.patch(
            url=url,
            status=400,
            payload={
                'status': 'fail',
                'code': 400,
                'data': {'message': 'ORDER_EVENT_ALREADY_EXISTS'},
            },
        )

        with pytest.raises(OrderEventAlreadyExistsError):
            await plus_client.update_order_status(
                message_id=message_id,
                amount=Decimal('42.00'),
                event_time=datetime(2021, 1, 2, 3, 4, 5, tzinfo=timezone.utc),
                status=OrderStatus.SUCCESS,
            )

    @pytest.mark.asyncio
    async def test_evil_message_id(
        self,
        plus_client,
        aioresponses_mocker,
    ):
        evil_message_id = '../capture/the/base'
        mock = aioresponses_mocker.patch(
            re.compile(r'^.+/api/v1/capture/the/base/status$'),
            status=200,
            payload={'status': 'success', 'code': 200, 'data': {'result': 'all our base is belongs to you'}},
        )

        with pytest.raises(aiohttp.client_exceptions.ClientConnectionError):
            await plus_client.update_order_status(
                message_id=evil_message_id,
                amount=Decimal('42.00'),
                event_time=datetime(2021, 1, 2, 3, 4, 5, tzinfo=timezone.utc),
                status=OrderStatus.SUCCESS,
            )

        mock.assert_not_called()

    @pytest.mark.asyncio
    async def test_should_log_not_found_response_body(
        self,
        plus_client,
        aioresponses_mocker,
        url,
        message_id,
        mocked_logger,
    ):
        aioresponses_mocker.patch(
            url=url,
            status=404,
            payload={
                'data': {
                    'message': 'not_found'
                }
            },
        )
        plus_client.logger = mocked_logger

        with pytest.raises(InteractionResponseError):
            await plus_client.update_order_status(
                message_id=message_id,
                amount=Decimal('42.00'),
                event_time=datetime(2021, 1, 2, 3, 4, 5, tzinfo=timezone.utc),
                status=OrderStatus.SUCCESS,
            )

        mocked_logger.error.assert_called_once_with('Unsuccessful interaction response body logged')


@pytest.mark.asyncio
async def test_error_representation(plus_client, aioresponses_mocker):
    aioresponses_mocker.post(
        url='https://test.test',
        status=400,
        payload={'status': 'fail', 'code': 400, 'data': {'message': 'BAD_FORMAT', 'params': 'stuff here'}},
    )

    with pytest.raises(BaseYandexPayPlusInteractionError) as exc_info:
        await plus_client.post(
            'interaction_method',
            url='https://test.test'
        )

    exc_str = str(exc_info.value)
    assert 'BAD_FORMAT' in exc_str and 'stuff here' in exc_str
