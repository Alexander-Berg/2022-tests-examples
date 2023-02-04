from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, greater_than, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.api.schemas.order import OrderSchema
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.update_status import UpdateOrderStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions.entities import CoreExceptionMessage, CoreExceptionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    ClassicOrderStatus,
    OrderEventStatus,
    PaymentMethodType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order


@pytest.fixture
def time_now():
    return utcnow()


@pytest.fixture
def message_id():
    return 'fake_message_id'


@pytest.fixture(params=list(ClassicOrderStatus))
def status(request):
    return request.param


@pytest.fixture
def json_body(time_now, status):
    return {
        'event_time': time_now.isoformat(sep=' '),
        'status': status.value,
    }


class TestSynchronousUpdateOrderStatus:
    @pytest.fixture(autouse=True)
    def turn_async_update_off(self, yandex_pay_plus_settings):
        yandex_pay_plus_settings.ASYNC_ORDER_STATUS_UPDATE_ENABLED = False

    @pytest.fixture
    def fake_order_schema(self, time_now, status):
        return dict(
            order_id=321654,
            uid=122333,
            message_id='fake_message_id',
            currency='XTS',
            amount=Decimal('100'),
            cashback=Decimal('10'),
            cashback_category=Decimal('0.1'),
            psp_id=uuid4(),
            merchant_id=uuid4(),
            status=status,
            created=time_now,
            updated=time_now,
            order_event_processed=time_now,
            payment_method_type=PaymentMethodType.CARD,
        )

    @pytest.fixture
    def fake_order(self, fake_order_schema):
        return Order(**fake_order_schema)

    @pytest.fixture
    def expected_response(self, fake_order):
        return {
            'status': 'success',
            'code': 200,
            'data': OrderSchema().dump(fake_order).data,
        }

    @pytest.fixture
    def mocked_action(self, mock_action, fake_order):
        return mock_action(UpdateOrderStatusAction, fake_order)

    @pytest.mark.asyncio
    async def test_update_order(
        self, mocked_action, app, json_body, message_id, status, time_now, expected_response
    ):
        json_body['recurring'] = None
        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)

        mocked_action.assert_called_once_with(
            message_id=message_id,
            status=status,
            event_time=time_now,
            amount=None,
            reason_code=None,
            reason=None,
            payment_id='',
            recurring=None,
        )

        assert_that(response.status, equal_to(200))
        assert_that(await response.json(), equal_to(expected_response))

    @pytest.mark.asyncio
    async def test_update_order_with_optionals(
        self, mocked_action, app, json_body, message_id, status, time_now, expected_response
    ):
        reason = 'fake_reason'
        reason_code = 'fake_reason_code'
        json_body.update(reason=reason, reason_code=reason_code, amount='10', recurring=True)

        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)

        mocked_action.assert_called_once_with(
            message_id=message_id,
            status=status,
            event_time=time_now,
            amount=Decimal('10'),
            reason_code=reason_code,
            reason=reason,
            payment_id='',
            recurring=True,
        )

        assert_that(response.status, equal_to(200))
        assert_that(await response.json(), equal_to(expected_response))

    @pytest.mark.asyncio
    async def test_naive_event_time_not_accepted(
        self, mocked_action, app, json_body, message_id, time_now
    ):
        json_body['event_time'] = time_now.replace(tzinfo=None).isoformat(sep=' ')

        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)

        mocked_action.assert_not_called()

        assert_that(response.status, equal_to(400))
        expected = {
            'status': CoreExceptionStatus.FAIL.value,
            'code': 400,
            'data': {
                'params': {'event_time': ['Not a timezone-aware datetime.']},
                'message': CoreExceptionMessage.BAD_FORMAT.value,
            }
        }
        assert_that(await response.json(), equal_to(expected))


class TestAsyncUpdateOrderStatus:
    @pytest.fixture(autouse=True)
    def turn_async_update_on(self, yandex_pay_plus_settings):
        yandex_pay_plus_settings.ASYNC_ORDER_STATUS_UPDATE_ENABLED = True

    @pytest.fixture
    def expected_response(self):
        return {
            'status': 'success',
            'code': 200,
            'data': {},
        }

    @pytest.mark.asyncio
    async def test_update_order(
        self, app, json_body, message_id, status, time_now, expected_response, storage
    ):
        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)

        assert_that(response.status, equal_to(200))
        assert_that(await response.json(), equal_to(expected_response))

        event = await storage.order_status_event.find_ensure_one(
            filters={'message_id': message_id}
        )

        assert_that(
            event,
            has_properties(
                message_id=message_id,
                event_time=time_now,
                order_status=status,
                event_status=OrderEventStatus.PENDING,
                params=equal_to({}),
                details=equal_to({}),
                run_at=greater_than(utcnow()),
            )
        )

    @pytest.mark.asyncio
    async def test_update_order_with_optionals(
        self, app, json_body, message_id, expected_response, storage
    ):
        reason = 'fake_reason'
        reason_code = 'fake_reason_code'
        json_body.update(reason=reason, reason_code=reason_code, amount='10')

        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)

        assert_that(response.status, equal_to(200))
        assert_that(await response.json(), equal_to(expected_response))

        event = await storage.order_status_event.find_ensure_one(
            filters={'message_id': message_id}
        )

        assert_that(
            event,
            has_properties(
                message_id=message_id,
                params=equal_to(
                    {'amount': '10', 'reason': reason, 'reason_code': reason_code}
                ),
                details=equal_to({}),
            )
        )

    @pytest.mark.asyncio
    async def test_duplicate_events_not_accepted(self, app, json_body, message_id, status):
        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)
        assert_that(response.status, equal_to(200))

        new_status = next(each for each in ClassicOrderStatus if each != status)
        json_body['status'] = new_status.value
        response = await app.patch(f'/api/v1/orders/{message_id}/status', json=json_body)

        assert_that(response.status, equal_to(409))
        expected_response = {
            'data': {'message': 'ORDER_EVENT_ALREADY_EXISTS'},
            'status': 'fail',
            'code': 409,
        }
        assert_that(await response.json(), equal_to(expected_response))
