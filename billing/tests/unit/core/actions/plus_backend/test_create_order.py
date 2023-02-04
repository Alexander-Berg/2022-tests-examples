import logging
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_auth.entities import AuthenticationMethod
from sendr_interactions.exceptions import InteractionResponseError
from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_entries, has_length, has_properties, none

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.antifraud.cashback import CheckCashbackAllowedAction
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.create_order import YandexPayPlusCreateOrderAction
from billing.yandex_pay.yandex_pay.core.entities.enums import CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import PaymentMerchant
from billing.yandex_pay.yandex_pay.core.entities.task import Task
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import CoreInvalidCurrencyError, CoreOrderCreationError
from billing.yandex_pay.yandex_pay.interactions import YandexPayPlusClient
from billing.yandex_pay.yandex_pay.interactions.plus_backend.entities import (
    OrderStatus, PlusOrder, YandexPayPlusMerchant
)
from billing.yandex_pay.yandex_pay.utils.stats import pay_plus_create_order_failures


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def tvm_ticket(rands):
    return rands()


@pytest.fixture
def login_id():
    return 'some_login_id'


@pytest.fixture
def user(uid, tvm_ticket, login_id):
    return User(uid, tvm_ticket, login_id, AuthenticationMethod.SESSION)


@pytest.fixture
def order_id(randn):
    return randn()


@pytest.fixture
def message_id(rands):
    return rands()


@pytest.fixture
def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
def merchant():
    return PaymentMerchant(
        id=uuid4(),
        name='the-name',
        url='https://url.test',
    )


@pytest.fixture
def psp_id():
    return uuid4()


@pytest.fixture
def card_id():
    return uuid4()


@pytest.fixture
def payment_order():
    return {
        'id': 'order_id',
        'items': None,
        'total': {
            'label': None,
            'amount': '10.0',
        },
    }


@pytest.fixture
def plus_order(order_id, uid, message_id, psp_id, merchant):
    return PlusOrder(
        order_id=order_id,
        uid=uid,
        message_id=message_id,
        currency='xts',
        amount=Decimal('100.00'),
        cashback=Decimal('5.00'),
        cashback_category=Decimal('0.05'),
        status=OrderStatus.NEW,
        psp_id=psp_id,
        merchant_id=merchant.id,
        payment_method_type=PaymentMethodType.CARD,
    )


@pytest.fixture(autouse=True)
def mocked_create_order_request(mocker, plus_order):
    mock = mocker.AsyncMock(return_value=plus_order)
    return mocker.patch.object(YandexPayPlusClient, 'create_order', mock)


@pytest.fixture(autouse=True)
def mock_check_cashback_allowed_action(mock_action):
    return mock_action(CheckCashbackAllowedAction, True)


@pytest.mark.asyncio
@pytest.mark.parametrize('cashback_category_id', [None, '0.05'])
async def test_serialize_kwargs(
    user,
    message_id,
    merchant,
    psp_id,
    trust_card_id,
    card_id,
    payment_order,
    cashback_category_id,
    storage,
    tvm_ticket,
):
    amount = Decimal('1.00')
    await YandexPayPlusCreateOrderAction(
        user=user,
        message_id=message_id,
        merchant=merchant,
        psp_id=psp_id,
        currency='xts',
        amount=amount,
        trust_card_id=trust_card_id,
        last4='1234',
        country_code='RUS',
        order_basket=payment_order,
        user_agent='agent',
        user_ip='ip',
        cashback_category_id=cashback_category_id,
        card_id=card_id,
        antifraud_external_id='ext_id',
        payment_method_type=PaymentMethodType.SPLIT,
    ).run_async()

    filters = {'action_name': YandexPayPlusCreateOrderAction.action_name}
    [task] = await alist(storage.task.find(filters=filters))
    assert_that(
        task,
        has_properties(
            state=TaskState.PENDING,
            params=has_entries(
                max_retries=10,
                action_kwargs=dict(
                    user={
                        'uid': user.uid,
                        'login_id': user.login_id,
                        'tvm_ticket': tvm_ticket,
                        'auth_method': AuthenticationMethod.SESSION.value,
                        'is_yandexoid': False,
                    },
                    message_id=message_id,
                    amount='1.00',
                    user_ip='ip',
                    currency='xts',
                    psp_id=str(psp_id),
                    merchant={
                        'id': str(merchant.id),
                        'name': merchant.name,
                        'url': merchant.url,
                    },
                    user_agent='agent',
                    trust_card_id=trust_card_id,
                    last4='1234',
                    country_code='RUS',
                    order_basket=payment_order,
                    cashback_category_id=cashback_category_id,
                    card_id=str(card_id),
                    antifraud_external_id='ext_id',
                    payment_method_type=PaymentMethodType.SPLIT.value,
                )
            )
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('cashback_category_id', [None, '0.05'])
async def test_deserialize_kwargs(
    user,
    message_id,
    merchant,
    psp_id,
    trust_card_id,
    card_id,
    payment_order,
    cashback_category_id,
    storage,
    tvm_ticket,
):
    amount = Decimal('1.00')
    await YandexPayPlusCreateOrderAction(
        user=user,
        message_id=message_id,
        merchant=merchant,
        psp_id=psp_id,
        currency='xts',
        amount=amount,
        trust_card_id=trust_card_id,
        last4='1234',
        country_code='RUS',
        order_basket=payment_order,
        user_agent='agent',
        user_ip='ip',
        cashback_category_id=cashback_category_id,
        card_id=card_id,
        antifraud_external_id='ext_id',
        payment_method_type=PaymentMethodType.CARD,
    ).run_async()

    filters = {'action_name': YandexPayPlusCreateOrderAction.action_name}
    [task] = await alist(storage.task.find(filters=filters))

    action_kwargs = task.params['action_kwargs']
    # popping it to check the task backwards compatibility without payment_method_type
    action_kwargs.pop('payment_method_type')
    loaded = YandexPayPlusCreateOrderAction(
        **YandexPayPlusCreateOrderAction.deserialize_kwargs(action_kwargs)
    )
    assert_that(
        loaded,
        has_properties(
            user=User(
                uid=user.uid,
                login_id=user.login_id,
                tvm_ticket=tvm_ticket,
                auth_method=AuthenticationMethod.SESSION,
            ),
            message_id=message_id,
            merchant=merchant,
            psp_id=psp_id,
            currency='xts',
            amount=amount,
            trust_card_id=trust_card_id,
            last4='1234',
            country_code='RUS',
            order_basket=payment_order,
            user_agent='agent',
            user_ip='ip',
            cashback_category_id=cashback_category_id,
            card_id=card_id,
            antifraud_external_id='ext_id',
            payment_method_type=PaymentMethodType.CARD,
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('payment_method_type', list(PaymentMethodType))
async def test_create_order(
    user,
    message_id,
    merchant,
    psp_id,
    payment_order,
    card_id,
    trust_card_id,
    mocked_create_order_request,
    plus_order,
    payment_method_type,
):
    amount = Decimal('1.00')

    result = await YandexPayPlusCreateOrderAction(
        user=user,
        message_id=message_id,
        merchant=merchant,
        psp_id=psp_id,
        currency='rub',
        amount=amount,
        trust_card_id=trust_card_id,
        last4='1234',
        country_code='RUS',
        order_basket=payment_order,
        user_agent='agent',
        user_ip='ip',
        cashback_category_id='0.05',
        card_network=CardNetwork.VISA,
        card_id=card_id,
        antifraud_external_id='ext_id',
        payment_method_type=payment_method_type,
    ).run()

    assert_that(result, equal_to(plus_order))
    mocked_create_order_request.assert_awaited_once_with(
        uid=user.uid,
        message_id=message_id,
        merchant=YandexPayPlusMerchant(
            id=merchant.id,
            name=merchant.name,
            url=merchant.url,
        ),
        psp_id=psp_id,
        currency='RUB',
        amount=amount,
        trust_card_id=trust_card_id,
        last4='1234',
        country_code='RUS',
        order_basket=payment_order,
        cashback_category_id='0.05',
        card_network='VISA',
        card_id=card_id,
        antifraud_external_id='ext_id',
        payment_method_type=payment_method_type,
    )


@pytest.mark.asyncio
async def test_action_call_logged(
    user, message_id, merchant, psp_id, order_id, trust_card_id, caplog, dummy_logger, card_id, plus_order
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    amount = Decimal('1.00')
    currency = 'rub'

    await YandexPayPlusCreateOrderAction(
        user=user,
        message_id=message_id,
        merchant=merchant,
        psp_id=psp_id,
        currency=currency,
        amount=amount,
        trust_card_id=trust_card_id,
        last4='1234',
        country_code='RUS',
        order_basket={},
        user_agent='agent',
        user_ip='ip',
        card_id=card_id,
        payment_method_type=PaymentMethodType.CARD,
    ).run()

    logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(logs, has_length(1))
    assert_that(
        logs[0],
        has_properties(
            message='Order created in plus backend',
            levelno=logging.INFO,
            _context=has_entries(
                uid=user.uid,
                message_id=message_id,
                merchant=merchant,
                psp_id=psp_id,
                currency=currency.upper(),
                amount=amount,
                trust_card_id=trust_card_id,
                plus_backend_order=plus_order,
                card_id=card_id,
                payment_method_type=PaymentMethodType.CARD,
            ),
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'exception,extra_log_params,log_message,core_exception',
    [
        (
            InteractionResponseError(
                message='test',
                status_code=500,
                method='post',
                service=YandexPayPlusClient.SERVICE,
                params={'foo': 'bar'},
            ),
            dict(message='test', status_code=500, params={'foo': 'bar'}),
            'Failed to create an order',
            CoreOrderCreationError,
        ),
    ]
)
async def test_errors_logged(
    user,
    message_id,
    merchant,
    psp_id,
    order_id,
    trust_card_id,
    caplog,
    dummy_logger,
    mocked_create_order_request,
    exception,
    extra_log_params,
    log_message,
    core_exception,
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)
    mocked_create_order_request.side_effect = exception

    with pytest.raises(core_exception):
        await YandexPayPlusCreateOrderAction(
            user=user,
            message_id=message_id,
            merchant=merchant,
            psp_id=psp_id,
            currency='RUB',
            amount=Decimal('1.00'),
            trust_card_id=trust_card_id,
            last4='1234',
            country_code='RUS',
            order_basket={},
            user_agent='agent',
            user_ip='ip',
            payment_method_type=PaymentMethodType.CARD,
        ).run()

    logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(logs, has_length(1))
    assert_that(
        logs[0],
        has_properties(
            message=log_message,
            levelno=logging.ERROR,
            _context=has_entries(
                uid=user.uid,
                message_id=message_id,
                merchant=merchant,
                psp_id=psp_id,
                currency='RUB',
                amount=Decimal('1.00'),
                trust_card_id=trust_card_id,
                payment_method_type=PaymentMethodType.CARD,
                **extra_log_params,
            ),
        )
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('currency', ['XTS'])
async def test_alternative_currency_not_allowed(
    user,
    message_id,
    merchant,
    psp_id,
    order_id,
    trust_card_id,
    currency,
):
    with pytest.raises(CoreInvalidCurrencyError):
        await YandexPayPlusCreateOrderAction(
            user=user,
            message_id=message_id,
            merchant=merchant,
            psp_id=psp_id,
            currency=currency,
            amount=Decimal('1.00'),
            trust_card_id=trust_card_id,
            last4='1234',
            country_code='RUS',
            order_basket={},
            user_agent='agent',
            user_ip='ip',
            payment_method_type=PaymentMethodType.CARD,
        ).run()


@pytest.mark.asyncio
async def test_calls_check_allowed_by_antifraud_with_expected_args(
    user,
    message_id,
    merchant,
    psp_id,
    order_id,
    trust_card_id,
    mock_check_cashback_allowed_action,
    mocker,
):
    run_mock = mocker.patch.object(
        CheckCashbackAllowedAction, 'run', mocker.AsyncMock(return_value=True)
    )

    await YandexPayPlusCreateOrderAction(
        user=user,
        message_id=message_id,
        merchant=merchant,
        psp_id=psp_id,
        currency='RUB',
        amount=Decimal('1.00'),
        trust_card_id=trust_card_id,
        last4='1234',
        country_code='RUS',
        order_basket={},
        user_agent='agent007',
        user_ip='ip42',
        payment_method_type=PaymentMethodType.CARD,
    ).run()

    mock_check_cashback_allowed_action.assert_called_once_with(
        user=user,
        external_id=f'cashback_{message_id}',
        amount=Decimal('1.00'),
        trust_card_id=trust_card_id,
        user_agent='agent007',
        user_ip='ip42',
        currency_number='643',
    )
    run_mock.assert_awaited_once()


@pytest.mark.asyncio
async def test_should_not_create_order_if_denied_by_antifraud(
    user,
    message_id,
    merchant,
    psp_id,
    trust_card_id,
    mocked_create_order_request,
    mock_action,
):
    mock_action(CheckCashbackAllowedAction, False)

    result = await YandexPayPlusCreateOrderAction(
        user=user,
        message_id=message_id,
        merchant=merchant,
        psp_id=psp_id,
        currency='RUB',
        amount=Decimal('1.0'),
        trust_card_id=trust_card_id,
        last4='1234',
        country_code='RUS',
        order_basket={},
        user_agent='agent',
        user_ip='ip',
        payment_method_type=PaymentMethodType.CARD,
    ).run()

    assert_that(result, none())
    mocked_create_order_request.assert_not_awaited()


@pytest.mark.asyncio
@pytest.mark.parametrize('reason', [None, 'fake_reason'])
async def test_report_task_failure(caplog, dummy_logger, mocker, reason):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    mock_counter_inc = mocker.patch.object(pay_plus_create_order_failures, 'inc')
    task = mocker.Mock(spec=Task)
    await YandexPayPlusCreateOrderAction.report_task_failure(
        task=task, reason=reason
    )

    mock_counter_inc.assert_called_once_with()
    [log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        log,
        has_properties(
            message='Failed to create plus cashback order',
            levelno=logging.ERROR,
            _context=has_entries(
                task=task,
                reason=reason
            ),
        )
    )
