import copy
import uuid
from dataclasses import replace
from datetime import datetime, timezone
from decimal import Decimal

import pytest
from freezegun import freeze_time
from pay.lib.entities.enums import OperationStatus, ShippingMethodType
from pay.lib.entities.operation import OperationType
from pay.lib.entities.order import PaymentStatus

from sendr_pytest.matchers import convert_then_match, equal_to
from sendr_utils import alist

from hamcrest import assert_that, has_entries, has_length, has_properties, has_property, match_equality

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.create_claim import CreateDeliveryClaimAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.status_event.create import CreateOrderStatusEventAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.base import ProcessingAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.notify import NotifyOperationFailedAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import OperationTypeMismatchError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageShippingMethod
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    ClassicOrderStatus,
    OrderEventSource,
    TaskType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    Transaction,
    TransactionData,
    TransactionStatus,
    TransactionThreeDSData,
)


class SpoilingProcessingAction(ProcessingAction, operation_type=OperationType.AUTHORIZE):
    def __init__(self, mock, **kwargs):
        super().__init__(**kwargs)
        self.mock = mock

    async def _handle(self):
        pass


@pytest.mark.asyncio
async def test_checks_operation_type(required_params):
    class NoOpProcessingAction(ProcessingAction, operation_type=OperationType.AUTHORIZE):
        async def _handle(self):
            pass

    required_params['operation'].operation_type = OperationType.CAPTURE
    with pytest.raises(OperationTypeMismatchError):
        await NoOpProcessingAction(**required_params).run()


class TestGetIntegration:
    class SpoilIntegrationAction(SpoilingProcessingAction, operation_type=OperationType.AUTHORIZE):
        async def _handle(self):
            self.mock(await self._get_integration())

    @pytest.mark.asyncio
    async def test_when_not_passed__loads(self, mocker, required_params, integration):
        mock = mocker.Mock()

        await self.SpoilIntegrationAction(**required_params, mock=mock).run()

        mock.assert_called_once_with(integration)

    @pytest.mark.asyncio
    async def test_when_passed__uses_passed(self, mocker, required_params):
        mock = mocker.Mock()
        mock_integration = mocker.Mock()

        await self.SpoilIntegrationAction(**required_params, mock=mock, integration=mock_integration).run()

        mock.assert_called_once_with(mock_integration)


@pytest.mark.asyncio
async def test_get_psp(mocker, required_params, psp):
    class SpoilIntegrationAction(SpoilingProcessingAction, operation_type=OperationType.AUTHORIZE):
        async def _handle(self):
            self.mock(await self._get_psp())

    mock = mocker.Mock()

    await SpoilIntegrationAction(**required_params, mock=mock).run()

    mock.assert_called_once_with(psp)


@pytest.mark.parametrize('save, expected_version', ((True, 666), (False, 1)))
@pytest.mark.asyncio
async def test_saves_transaction(storage, transaction, save, expected_version, operation):
    class SpoilTransactionSaveAction(ProcessingAction, operation_type=OperationType.AUTHORIZE):
        async def _handle(self):
            self.transaction.version = 666

    transaction.version = 1
    transaction = await storage.transaction.save(transaction)

    await SpoilTransactionSaveAction(transaction=transaction, operation=operation, save=save).run()

    assert_that(await storage.transaction.get(transaction.transaction_id), has_property('version', expected_version))


@pytest.mark.parametrize('save, expected_reason', ((True, 'altered-reason'), (False, 'reason')))
@pytest.mark.asyncio
async def test_saves_operation(storage, transaction, save, expected_reason, operation):
    class SpoilOperationSaveAction(ProcessingAction, operation_type=OperationType.AUTHORIZE):
        async def _handle(self):
            self.operation.reason = 'altered-reason'

    operation.reason = 'reason'
    operation = await storage.order_operation.save(operation)

    await SpoilOperationSaveAction(transaction=transaction, operation=operation, save=save).run()

    assert_that(
        await storage.order_operation.get(operation.operation_id),
        has_property('reason', expected_reason),
    )


@pytest.mark.parametrize('save, expected_status', ((True, PaymentStatus.AUTHORIZED), (False, PaymentStatus.PENDING)))
@pytest.mark.asyncio
async def test_saves_order(storage, transaction, save, expected_status, operation):
    class SpoilOrderSaveAction(ProcessingAction, operation_type=OperationType.AUTHORIZE):
        async def _handle(self):
            self.transaction.order.payment_status = PaymentStatus.AUTHORIZED

    await SpoilOrderSaveAction(transaction=transaction, operation=operation, save=save).run()

    assert_that(
        await storage.checkout_order.get(transaction.order.checkout_order_id),
        has_property('payment_status', expected_status),
    )


@pytest.mark.parametrize(
    'op_status, op_type',
    [
        (OperationStatus.FAIL, OperationType.AUTHORIZE),
        (OperationStatus.SUCCESS, OperationType.AUTHORIZE),
        (OperationStatus.SUCCESS, OperationType.CAPTURE),
        (OperationStatus.SUCCESS, OperationType.REFUND),
    ],
)
@pytest.mark.asyncio
async def test_updates_order(storage, transaction, operation, op_status, op_type):
    initial_order = copy.deepcopy(transaction.order)
    should_update = op_status == OperationStatus.SUCCESS
    operation.operation_type = op_type

    class SpoilOrderUpdateAction(ProcessingAction, operation_type=op_type):
        async def _handle(self):
            await self._update_operation(operation_status=op_status)

    result = await SpoilOrderUpdateAction(transaction=transaction, operation=operation, save=False).run()

    assert_that(result.transaction.order != initial_order, equal_to(should_update))
    if should_update:
        assert_that(
            result.transaction.order,
            has_properties(
                cart=operation.cart,
                shipping_method=operation.shipping_method,
                order_amount=Decimal('480.00'),
                authorize_amount=operation.amount if op_type == OperationType.AUTHORIZE else None,
                capture_amount=operation.amount if op_type == OperationType.CAPTURE else None,
            ),
        )


@pytest.mark.parametrize('operation_type', set(OperationType) - {OperationType.AUTHORIZE})
@pytest.mark.asyncio
@freeze_time('2022-02-22')
async def test_notifies_merchant_on_operation_failure(
    storage, transaction, operation, operation_type, mock_notify_merchant
):
    class SpoilOperationSaveAction(ProcessingAction, operation_type=operation_type):
        async def _handle(self):
            self.operation.status = OperationStatus.FAIL

    operation.operation_type = operation_type
    operation = await storage.order_operation.save(operation)

    await SpoilOperationSaveAction(transaction=transaction, operation=operation, save=False).run()

    mock_notify_merchant.assert_called_once_with(
        operation_id=operation.operation_id, event_time=datetime(2022, 2, 22, tzinfo=timezone.utc)
    )


class TestClassicOrderStatus:
    def UpdatingOperationAction(self, op_type, op_status):
        class UpdatingOperationAction(ProcessingAction, operation_type=op_type):
            async def _handle(self):
                self.transaction.message_id = 'yp-msgid'
                await self._update_operation(operation_status=op_status, reason='the-reason')

        return UpdatingOperationAction

    @pytest.mark.parametrize(
        'op_type, op_status, expected_classic_status, expected_reason',
        [
            (OperationType.AUTHORIZE, OperationStatus.SUCCESS, ClassicOrderStatus.HOLD, None),
            (OperationType.CAPTURE, OperationStatus.SUCCESS, ClassicOrderStatus.SUCCESS, None),
            (OperationType.VOID, OperationStatus.SUCCESS, ClassicOrderStatus.REVERSE, None),
            (OperationType.REFUND, OperationStatus.SUCCESS, ClassicOrderStatus.REFUND, None),
            (OperationType.AUTHORIZE, OperationStatus.FAIL, ClassicOrderStatus.FAIL, 'the-reason'),
        ],
    )
    @pytest.mark.asyncio
    async def test_updates_status(
        self, mocker, storage, transaction, operation, op_type, op_status, expected_classic_status, expected_reason
    ):
        operation = await storage.order_operation.save(replace(operation, operation_type=op_type))

        await self.UpdatingOperationAction(op_type=op_type, op_status=op_status)(
            transaction=transaction, operation=operation, save=False
        ).run()

        [task] = await alist(
            storage.task.find(
                filters={'task_type': TaskType.RUN_ACTION, 'action_name': CreateOrderStatusEventAction.action_name},
                order=('-created',),
            )
        )
        assert_that(
            task.params,
            has_entries(
                action_kwargs=dict(
                    message_id='yp-msgid',
                    status=expected_classic_status.value,
                    event_time=match_equality(
                        convert_then_match(datetime.fromisoformat, equal_to(operation.created)),
                    ),
                    amount='555.55',
                    reason_code=expected_reason,
                    reason=None,
                    source=OrderEventSource.CHECKOUT.value,
                ),
            ),
        )

    @pytest.mark.parametrize(
        'op_type, op_status',
        [
            (OperationType.CAPTURE, OperationStatus.FAIL),
            (OperationType.VOID, OperationStatus.FAIL),
            (OperationType.REFUND, OperationStatus.FAIL),
            (OperationType.AUTHORIZE, OperationStatus.PENDING),
            (OperationType.CAPTURE, OperationStatus.PENDING),
            (OperationType.VOID, OperationStatus.PENDING),
            (OperationType.REFUND, OperationStatus.PENDING),
        ],
    )
    @pytest.mark.asyncio
    async def test_does_not_update_status(self, storage, transaction, operation, op_type, op_status):
        operation = await storage.order_operation.save(replace(operation, operation_type=op_type))

        await self.UpdatingOperationAction(op_type=op_type, op_status=op_status)(
            transaction=transaction, operation=operation, save=False
        ).run()

        tasks = await alist(
            storage.task.find(
                filters={'task_type': TaskType.RUN_ACTION, 'action_name': CreateOrderStatusEventAction.action_name},
                order=('-created',),
            )
        )
        assert_that(tasks, has_length(0))


class TestProcessDelivery:
    def UpdatingOperationAction(self, op_status):
        class UpdatingOperationAction(ProcessingAction, operation_type=OperationType.AUTHORIZE):
            async def _handle(self):
                await self._update_operation(operation_status=op_status)

        return UpdatingOperationAction

    @pytest.fixture
    def autoaccept(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    async def merchant(self, storage, stored_merchant, autoaccept):
        stored_merchant.delivery_integration_params = DeliveryIntegrationParams(
            yandex_delivery=YandexDeliveryParams(oauth_token='secret', autoaccept=autoaccept),
        )
        return await storage.merchant.save(stored_merchant)

    @pytest.mark.parametrize('autoaccept,shipping_method', [(True, 'yd_shipping_method')])
    @pytest.mark.asyncio
    async def test_calls_create_claim(self, storage, required_params):
        await self.UpdatingOperationAction(OperationStatus.SUCCESS)(**required_params).run()

        [task] = await alist(
            storage.task.find(
                filters={'task_type': TaskType.RUN_ACTION, 'action_name': CreateDeliveryClaimAction.action_name},
            )
        )
        assert_that(
            task.params,
            has_entries(
                action_kwargs=dict(
                    merchant_id=str(required_params['operation'].merchant_id),
                    checkout_order_id=str(required_params['operation'].checkout_order_id),
                ),
            ),
        )

    @pytest.mark.parametrize(
        'autoaccept, shipping_method, op_status',
        [
            (True, 'courier_shipping_method', OperationStatus.SUCCESS),
            (True, 'yd_shipping_method', OperationStatus.FAIL),
            (False, 'yd_shipping_method', OperationStatus.SUCCESS),
        ],
    )
    @pytest.mark.asyncio
    async def test_does_not_call_create_claim(self, storage, operation, required_params, op_status):
        await self.UpdatingOperationAction(op_status)(**required_params).run()

        tasks = await alist(
            storage.task.find(
                filters={'task_type': TaskType.RUN_ACTION, 'action_name': CreateDeliveryClaimAction.action_name},
            )
        )
        assert_that(len(tasks), equal_to(0))


@pytest.fixture
def required_params(transaction, operation):
    return {
        'transaction': transaction,
        'operation': operation,
    }


@pytest.fixture(autouse=True)
def mock_notify_merchant(mock_action):  # noqa
    return mock_action(NotifyOperationFailedAction)


@pytest.fixture
async def transaction(storage, stored_checkout_order, entity_threeds_authentication_request, integration):
    transaction = await storage.transaction.create(
        Transaction(
            transaction_id=uuid.UUID('ac3a67e1-9df1-4612-bfdc-c82a3c549c14'),
            checkout_order_id=stored_checkout_order.checkout_order_id,
            integration_id=integration.integration_id,
            status=TransactionStatus.NEW,
            card_id='card-x1234',
            data=TransactionData(
                user_ip='192.0.2.1',
                threeds=TransactionThreeDSData(authentication_request=entity_threeds_authentication_request),
            ),
            version=1,
        )
    )
    transaction.order = stored_checkout_order
    return transaction


@pytest.fixture
def shipping_method(request):
    return getattr(request, 'param', 'courier_shipping_method')


@pytest.fixture
def courier_shipping_method(entity_courier_option):
    return StorageShippingMethod(
        method_type=ShippingMethodType.COURIER,
        courier_option=entity_courier_option,
    )


@pytest.fixture
def yd_shipping_method(entity_yd_option):
    return StorageShippingMethod(
        method_type=ShippingMethodType.YANDEX_DELIVERY,
        yandex_delivery_option=entity_yd_option,
    )


@pytest.fixture
async def operation(storage, stored_operation, shipping_method, request):
    return await storage.order_operation.save(
        replace(
            stored_operation,
            operation_type=OperationType.AUTHORIZE,
            amount=Decimal('555.55'),
            shipping_method=request.getfixturevalue(shipping_method),
        )
    )


@pytest.fixture
async def integration(storage, psp, stored_merchant):
    return await storage.integration.create(
        Integration(
            merchant_id=stored_merchant.merchant_id,
            psp_id=psp.psp_id,
            status=IntegrationStatus.DEPLOYED,
            creds=Integration.encrypt_creds({}),
        )
    )


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(PSP(psp_external_id='payture', psp_id=uuid.uuid4()))
