from datetime import datetime, timezone
from decimal import Decimal
from uuid import UUID

import pytest
from pay.lib.entities.payment_sheet import PaymentMerchant as Merchant

from sendr_tvm.common import TicketCheckResult
from sendr_tvm.qloud_async_tvm import QTickerChecker

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.create import CreateOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions.entities import CoreExceptionMessage, CoreExceptionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import ClassicOrderStatus, PaymentMethodType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order


class TestPostOrder:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(CreateOrderAction)

    @pytest.fixture
    def uid(self, randn):
        return randn()

    @pytest.fixture
    def json_body(self, uid):
        return {
            'uid': uid,
            'currency': 'XTS',
            'amount': '10.5',
            'message_id': '1:messageid',
            'psp_id': '4d870e21-1952-47fc-816a-f93cf57184b4',
            'merchant': {
                'id': '933c5784-9685-4db0-832a-90cb61b9646a',
                'name': 'the-name',
                'url': 'https://url.test',
            },
            'trust_card_id': 'card-x123abc',
            'card_id': '8841aef1-1d7d-4f09-af3f-b65239391511',
            'last4': '1234',
            'antifraud_external_id': 'antifraud_id',
            'card_network': 'visa',
            'country_code': 'RUS',
            'order_basket': {
                'id': 'item_id',
            },
        }

    @pytest.mark.asyncio
    @pytest.mark.parametrize('cashback_category_id', [None, '0.1'])
    async def test_params(self, action, app, json_body, uid, cashback_category_id):
        json_body['cashback_category_id'] = cashback_category_id

        await app.post('/api/v1/orders', json=json_body, raise_for_status=True)

        action.assert_called_once_with(
            uid=uid,
            currency='XTS',
            amount=Decimal('10.5'),
            message_id='1:messageid',
            psp_id=UUID('4d870e21-1952-47fc-816a-f93cf57184b4'),
            merchant=Merchant(
                id=UUID('933c5784-9685-4db0-832a-90cb61b9646a'),
                name='the-name',
                url='https://url.test'
            ),
            trust_card_id='card-x123abc',
            cashback_category_id=cashback_category_id,
            card_id=UUID('8841aef1-1d7d-4f09-af3f-b65239391511'),
            last4='1234',
            antifraud_external_id='antifraud_id',
            card_network='visa',
            country_code='RUS',
            order_basket={
                'id': 'item_id',
            },
            payment_method_type=PaymentMethodType.CARD,  # default is set by the schema, not json_body
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('payment_method_type', list(PaymentMethodType))
    async def test_payment_method_type_set_explicitly(self, action, app, json_body, payment_method_type):
        json_body['payment_method_type'] = payment_method_type.value

        await app.post('/api/v1/orders', json=json_body, raise_for_status=True)

        action.assert_called_once()
        assert_that(
            action.call_args.kwargs,
            has_entries(payment_method_type=payment_method_type),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('provide_user_ticket', [True, False])
    async def test_uid_in_body_takes_precedence_over_tvm_ticket(
        self, provide_user_ticket, mocker, tvm_service_id, action, app, json_body, uid
    ):
        if not provide_user_ticket:
            mocker.patch.object(
                QTickerChecker,
                'check_headers',
                mocker.AsyncMock(
                    return_value=TicketCheckResult({'src': tvm_service_id}, None),
                ),
            )

        resp = await app.post('/api/v1/orders', json=json_body)
        assert_that(resp.status, equal_to(200))
        action.assert_called_once_with(
            uid=uid,
            currency='XTS',
            amount=Decimal('10.5'),
            message_id='1:messageid',
            psp_id=UUID('4d870e21-1952-47fc-816a-f93cf57184b4'),
            merchant=Merchant(
                id=UUID('933c5784-9685-4db0-832a-90cb61b9646a'),
                name='the-name',
                url='https://url.test',
            ),
            trust_card_id='card-x123abc',
            cashback_category_id=None,
            card_id=UUID('8841aef1-1d7d-4f09-af3f-b65239391511'),
            last4='1234',
            antifraud_external_id='antifraud_id',
            card_network='visa',
            country_code='RUS',
            order_basket={
                'id': 'item_id',
            },
            payment_method_type=PaymentMethodType.CARD,
        )

    @pytest.mark.asyncio
    async def test_dump_only_fields_are_not_loaded(self, action, app, json_body, uid):
        json_body.update({
            'cashback': '15.0',
            'status': 'new',
            'order_id': 1,
            'merchant_id': '111111111',
            'closed': '2021-01-01T00:00:00+00:00',
            'created': '2021-01-01T00:00:00+00:00',
            'updated': '2021-01-01T00:00:00+00:00',
        })
        await app.post('/api/v1/orders', json=json_body)

        action.assert_called_once_with(
            uid=uid,
            currency='XTS',
            amount=Decimal('10.5'),
            message_id='1:messageid',
            psp_id=UUID('4d870e21-1952-47fc-816a-f93cf57184b4'),
            merchant=Merchant(
                id=UUID('933c5784-9685-4db0-832a-90cb61b9646a'),
                name='the-name',
                url='https://url.test',
            ),
            trust_card_id='card-x123abc',
            cashback_category_id=None,
            card_id=UUID('8841aef1-1d7d-4f09-af3f-b65239391511'),
            last4='1234',
            antifraud_external_id='antifraud_id',
            card_network='visa',
            country_code='RUS',
            order_basket={
                'id': 'item_id',
            },
            payment_method_type=PaymentMethodType.CARD,
        )

    @pytest.mark.asyncio
    async def test_empty_merchant_url_is_ok(self, action, app, json_body, uid):
        json_body.update({
            'merchant': {
                'id': json_body['merchant']['id'],
                'name': json_body['merchant']['name'],
                'url': None,
            }
        })
        await app.post('/api/v1/orders', json=json_body)

        action.assert_called_once_with(
            uid=uid,
            currency='XTS',
            amount=Decimal('10.5'),
            message_id='1:messageid',
            psp_id=UUID('4d870e21-1952-47fc-816a-f93cf57184b4'),
            merchant=Merchant(
                id=UUID('933c5784-9685-4db0-832a-90cb61b9646a'),
                name='the-name',
                url=None,
            ),
            trust_card_id='card-x123abc',
            cashback_category_id=None,
            card_id=UUID('8841aef1-1d7d-4f09-af3f-b65239391511'),
            last4='1234',
            antifraud_external_id='antifraud_id',
            card_network='visa',
            country_code='RUS',
            order_basket={
                'id': 'item_id',
            },
            payment_method_type=PaymentMethodType.CARD,
        )

    @pytest.mark.parametrize('body_patch, expected_validation_errors', (
        ({'currency': 'XX'}, {'currency': ['Not a valid ISO 4217 alpha code.']}),
        ({'currency': '555'}, {'currency': ['Not a valid ISO 4217 alpha code.']}),
        ({'amount': 'amount'}, {'amount': ['Not a valid number.']}),
        ({'psp_id': 'idid'}, {'psp_id': ['Not a valid UUID.']}),
        (
            {'merchant': {'id': 'idid'}},
            {
                'merchant': {
                    'id': ['Not a valid UUID.'],
                    'name': ['Missing data for required field.'],
                    'url': ['Missing data for required field.']
                }
            }
        ),
        ({'trust_card_id': None}, {'trust_card_id': ['Field may not be null.']}),
        ({'uid': 'YY'}, {'uid': ['Not a valid integer.']}),
        ({'uid': None}, {'uid': ['Field may not be null.']}),
    ))
    @pytest.mark.asyncio
    async def test_invalid_field_format(self, action, app, json_body, body_patch, expected_validation_errors):
        json_body.update(body_patch)

        r = await app.post('/api/v1/orders', json=json_body)

        await check_validation_errors(r, expected_validation_errors)

    @pytest.mark.parametrize('not_none_field', ('currency', 'amount', 'psp_id', 'merchant'))
    @pytest.mark.asyncio
    async def test_not_none(self, action, app, json_body, not_none_field):
        json_body[not_none_field] = None

        r = await app.post('/api/v1/orders', json=json_body)

        await check_validation_errors(r, {not_none_field: ['Field may not be null.']})

    @pytest.mark.asyncio
    async def test_returned_order(self, action, app, json_body, mock_action):
        mock_action(
            CreateOrderAction,
            Order(
                uid=555,
                message_id='msgid',
                currency='XTS',
                amount=Decimal('100.0'),
                cashback=Decimal('10.0'),
                cashback_category=Decimal('0.01'),
                status=ClassicOrderStatus.NEW,
                psp_id=UUID('4d870e21-1952-47fc-816a-f93cf57184b4'),
                merchant_id=UUID('933c5784-9685-4db0-832a-90cb61b9646a'),
                order_id=10,
                closed=datetime(2021, 1, 2, 0, 5, 10, tzinfo=timezone.utc),
                created=datetime(2021, 1, 2, 0, 5, 10, tzinfo=timezone.utc),
                updated=datetime(2021, 1, 2, 0, 5, 10, tzinfo=timezone.utc),
                trust_card_id='card-x123abc',
                card_id=UUID('4d870e21-1952-47fc-816a-f93cf57184b4'),
                payment_method_type=PaymentMethodType.CARD,
            )
        )

        r = await app.post('/api/v1/orders', json=json_body)
        data = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(
            data,
            equal_to({
                'status': 'success',
                'code': 200,
                'data': {
                    'uid': 555,
                    'message_id': 'msgid',
                    'currency': 'XTS',
                    'amount': '100.0',
                    'cashback': '10.0',
                    'cashback_category': '0.01',
                    'status': ClassicOrderStatus.NEW.value,
                    'psp_id': '4d870e21-1952-47fc-816a-f93cf57184b4',
                    'merchant_id': '933c5784-9685-4db0-832a-90cb61b9646a',
                    'order_id': 10,
                    'closed': '2021-01-02T00:05:10+00:00',
                    'created': '2021-01-02T00:05:10+00:00',
                    'updated': '2021-01-02T00:05:10+00:00',
                    'trust_card_id': 'card-x123abc',
                    'card_id': '4d870e21-1952-47fc-816a-f93cf57184b4',
                    'payment_method_type': PaymentMethodType.CARD.value,
                }
            })
        )


async def check_validation_errors(response, expected_error_params):
    data = await response.json()

    assert_that(response.status, equal_to(400))
    assert_that(
        data,
        has_entries({
            'status': CoreExceptionStatus.FAIL.value,
            'code': 400,
            'data': has_entries({
                'message': CoreExceptionMessage.BAD_FORMAT.value,
                'params': expected_error_params,
            }),
        })
    )
