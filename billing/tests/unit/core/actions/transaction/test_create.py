import uuid
from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.order import PaymentMethodType
from pay.lib.entities.threeds import ThreeDSBrowserData, ThreeDSBrowserDataHeaders, ThreeDSBrowserDataPayload
from pay.lib.interactions.split.entities import YandexSplitOrderCheckoutInfo

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action
from sendr_utils import alist

from hamcrest import assert_that, has_entries, has_property, match_equality, not_none

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.classic import (
    CreateCorrespondingClassicOrderAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.hold import HoldAsyncableAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.create import CreateTransactionAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import IntegrationNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.split import SplitClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import ClassicOrderStatus, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import PaymentMethodType as ClassicPaymentMethodType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TaskType, TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    SplitOrderMetaData,
    SplitTransactionData,
    ThreeDS2AuthenticationRequest,
    ThreeDS2MethodData,
    Transaction,
    TransactionData,
    TransactionThreeDSData,
)

fixtures_dummy_use_for_linters = [mock_action, explain_call_asserts]


@pytest.mark.asyncio
async def test_when_already_exists__returns_existing(params, stored_checkout_order, integration):
    transaction = await CreateTransactionAction(**params).run()

    new_transaction = await CreateTransactionAction(**params).run()

    assert_that(transaction, equal_to(new_transaction))


class TestCardPaymentMethod:
    @pytest.mark.asyncio
    async def test_returned(self, params, stored_checkout_order, integration):
        transaction = await CreateTransactionAction(**params).run()

        assert_that(
            transaction,
            equal_to(
                ensure_all_fields(
                    Transaction,
                    transaction_id=match_equality(not_none()),
                    checkout_order_id=stored_checkout_order.checkout_order_id,
                    status=TransactionStatus.NEW,
                    payment_method=PaymentMethodType.CARD,
                    data=TransactionData(
                        user_ip='192.0.2.1',
                        threeds=TransactionThreeDSData(
                            threeds2_method_data=ThreeDS2MethodData(
                                threeds_method_notification_url=(
                                    f'https://pay.yandex.test/3ds/method-result/{transaction.transaction_id}'
                                )
                            ),
                            authentication_request=ThreeDS2AuthenticationRequest(
                                challenge_notification_url=(
                                    f'https://pay.yandex.test/3ds/challenge-result/{transaction.transaction_id}'
                                ),
                                browser_data=ThreeDSBrowserData(
                                    accept_header='accept_header',
                                    ip='192.0.2.1',
                                    user_agent='user_agent',
                                    java_enabled=False,
                                    language='language',
                                    screen_color_depth=24,
                                    screen_height=1080,
                                    screen_width=1920,
                                    window_height=480,
                                    window_width=640,
                                    timezone=-180,
                                ),
                            )
                        ),
                    ),
                    integration_id=integration.integration_id,
                    card_id='card-x1234',
                    card_last4=None,
                    card_network=None,
                    reason=None,
                    message_id=None,
                    version=1,
                    order=None,
                    created=match_equality(not_none()),
                    updated=match_equality(not_none()),
                )
            )
        )

    @pytest.mark.asyncio
    async def test_creates_operation(self, params, storage, integration, stored_merchant, stored_checkout_order):
        transaction = await CreateTransactionAction(**params).run()

        operation = await storage.order_operation.find_ensure_one(
            filters=dict(
                checkout_order_id=transaction.checkout_order_id
            )
        )
        assert_that(
            operation,
            equal_to(
                ensure_all_fields(
                    Operation,
                    operation_id=match_equality(not_none()),
                    checkout_order_id=transaction.checkout_order_id,
                    order_id=stored_checkout_order.order_id,
                    merchant_id=stored_merchant.merchant_id,
                    amount=Decimal('123.45'),
                    operation_type=OperationType.AUTHORIZE,
                    status=OperationStatus.PENDING,
                    external_operation_id=None,
                    reason=None,
                    params={},
                    cart=stored_checkout_order.cart,
                    shipping_method=stored_checkout_order.shipping_method,
                    created=match_equality(not_none()),
                    updated=match_equality(not_none()),
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_schedules_hold_operation(self, params, storage, integration):
        transaction = await CreateTransactionAction(**params).run()

        [task] = await alist(
            storage.task.find(
                filters=dict(
                    task_type=TaskType.RUN_ACTION,
                    action_name=HoldAsyncableAction.action_name,
                )
            )
        )
        operation = await storage.order_operation.find_ensure_one(
            filters=dict(
                checkout_order_id=transaction.checkout_order_id
            )
        )
        assert_that(
            task,
            has_property('params', has_entries({
                'action_kwargs': {
                    'transaction_id': str(transaction.transaction_id),
                    'operation_id': str(operation.operation_id),
                }
            }))
        )

    @pytest.mark.asyncio
    async def test_integration_not_found(self, params, storage):
        with pytest.raises(IntegrationNotFoundError):
            await CreateTransactionAction(**params).run()


class TestSplitPaymentMethod:
    @pytest.mark.asyncio
    async def test_returned(self, params, checkout_order, stored_merchant):
        transaction = await CreateTransactionAction(**params).run()

        assert_that(
            transaction,
            equal_to(
                ensure_all_fields(
                    Transaction,
                    transaction_id=match_equality(not_none()),
                    checkout_order_id=checkout_order.checkout_order_id,
                    status=TransactionStatus.NEW,
                    payment_method=PaymentMethodType.SPLIT,
                    data=TransactionData(
                        user_ip='192.0.2.1',
                        threeds=TransactionThreeDSData(
                            authentication_request=ThreeDS2AuthenticationRequest(
                                challenge_notification_url=f'https://pay.yandex.test/3ds/challenge-result/{transaction.transaction_id}',  # noqa
                                browser_data=ThreeDSBrowserData(
                                    accept_header='accept_header',
                                    ip='192.0.2.1',
                                    user_agent='user_agent',
                                    java_enabled=False,
                                    language='language',
                                    screen_color_depth=24,
                                    screen_height=1080,
                                    screen_width=1920,
                                    window_height=480,
                                    window_width=640,
                                    timezone=-180,
                                ),
                            )
                        ),
                        split=SplitTransactionData(
                            order_meta=SplitOrderMetaData(
                                order_id='split-order-id',
                            ),
                            checkout_url='https://split-checkout-url.test',
                            plan_id='1',
                        )
                    ),
                    integration_id=None,
                    card_id='card-x1234',
                    card_last4=None,
                    card_network=None,
                    reason=None,
                    message_id=f'2:{stored_merchant.merchant_id}_merchant-order-id',
                    version=1,
                    order=None,
                    created=match_equality(not_none()),
                    updated=match_equality(not_none()),
                )
            )
        )

    @pytest.mark.asyncio
    async def test_calls_create_classic_order(
        self, params, checkout_order, stored_merchant, mock_create_classic_order, yandex_pay_plus_settings
    ):
        await CreateTransactionAction(**params).run()

        mock_create_classic_order.assert_run_once_with(
            checkout_order=checkout_order,
            card_id='card-x1234',
            message_id=f'2:{stored_merchant.merchant_id}_merchant-order-id',
            psp_id=uuid.UUID(yandex_pay_plus_settings.SPLIT_PSP_ID),
        )

    @pytest.mark.asyncio
    async def test_calls_split_create_order(
        self,
        storage,
        params,
        checkout_order,
        stored_merchant,
        mock_split_create_order,
        yandex_pay_plus_settings,
        entity_auth_user,
    ):
        await CreateTransactionAction(**params).run()

        mock_split_create_order.assert_awaited_once_with(
            uid=entity_auth_user.uid,
            login_id=entity_auth_user.login_id,
            currency='XTS',
            amount=Decimal('333.33'),
            external_order_id='merchant-order-id',
            trust_card_id='trust-card-id-x1234',
            merchant_id=stored_merchant.split_merchant_id,
            plus_points=Decimal('555.35'),
            plan_id='1',
        )

    @pytest.fixture(autouse=True)
    def mock_split_create_order(self, mocker):
        return mocker.patch.object(
            SplitClient,
            'create_order',
            mocker.AsyncMock(
                return_value=YandexSplitOrderCheckoutInfo(
                    order_id='split-order-id',
                    checkout_url='https://split-checkout-url.test'
                ),
            ),
        )

    @pytest.fixture(autouse=True)
    def mock_create_classic_order(self, mock_action):
        return mock_action(
            CreateCorrespondingClassicOrderAction,
            return_value=Order(
                uid=1,
                message_id='',
                currency='XTS',
                amount=Decimal('0'),
                cashback=Decimal('555.35'),
                cashback_category=Decimal('0'),
                status=ClassicOrderStatus.NEW,
                psp_id=uuid.UUID('00000000-0000-0000-0000-000000000000'),
                merchant_id=uuid.UUID('00000000-0000-0000-0000-000000000000'),
                payment_method_type=ClassicPaymentMethodType.SPLIT,
                trust_card_id='trust-card-id-x1234',
            )
        )

    @pytest.fixture
    async def checkout_order(self, storage, checkout_order):
        return await storage.checkout_order.save(
            replace(
                checkout_order,
                authorize_amount=Decimal('333.33'),
                payment_method_type=PaymentMethodType.SPLIT,
            )
        )


@pytest.fixture
async def stored_merchant(storage, entity_merchant):
    return await storage.merchant.create(replace(entity_merchant, split_merchant_id='smi'))


@pytest.fixture
async def integration(storage, psp, stored_merchant):
    return await storage.integration.create(
        Integration(
            merchant_id=stored_merchant.merchant_id,
            psp_id=psp.psp_id,
            status=IntegrationStatus.DEPLOYED,
            creds=Integration.encrypt_creds({
                'key': 'the-key',
                'password': 'the-password',
                'gateway_merchant_id': 'gw-mid',
            }),
        )
    )


@pytest.fixture
async def psp(storage, rands):
    return await storage.psp.create(
        PSP(psp_external_id=rands(), psp_id=uuid.uuid4())
    )


@pytest.fixture
async def checkout_order(storage, stored_checkout_order):
    return await storage.checkout_order.save(
        replace(
            stored_checkout_order,
            order_id='merchant-order-id',
            payment_method_type=PaymentMethodType.CARD,
        )
    )


@pytest.fixture
def params(entity_auth_user, checkout_order, entity_threeds_authentication_request):
    return dict(
        user=entity_auth_user,
        checkout_order_id=checkout_order.checkout_order_id,
        threeds_headers=ThreeDSBrowserDataHeaders(
            accept_header='accept_header',
            ip='192.0.2.1',
            user_agent='user_agent',
        ),
        threeds_payload=ThreeDSBrowserDataPayload(
            java_enabled=False,
            language='language',
            screen_color_depth=24,
            screen_height=1080,
            screen_width=1920,
            window_height=480,
            window_width=640,
            timezone=-180,
        ),
        challenge_return_path='https://passport-chaas-return-path.test',
        card_id='card-x1234',
        split_plan_id='1',
    )


@pytest.fixture(autouse=True)
def mock_3ds_notification_url(yandex_pay_plus_settings):
    yandex_pay_plus_settings.THREEDS_NOTIFICATION_URL = 'https://pay.yandex.test'
