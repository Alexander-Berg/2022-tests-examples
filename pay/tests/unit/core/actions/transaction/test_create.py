from dataclasses import replace
from datetime import timedelta

import pytest
import yarl

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, match_equality, not_none

from pay.bill_payments.bill_payments.core.actions.transaction.create import CreateTransactionAction
from pay.bill_payments.bill_payments.core.entities.mpi_3ds_info import MPI3DSInfo
from pay.bill_payments.bill_payments.core.exceptions import (
    BillAlreadyPaidError,
    CoreFailError,
    MixedDepartmentsError,
    OrderAlreadyPaidError,
    OrderNotFoundError,
    PaymentMethodNotSupportedError,
)
from pay.bill_payments.bill_payments.interactions.kazna import KaznaClient
from pay.bill_payments.bill_payments.interactions.kazna.entities import (
    TDS,
    DepartmentType,
    DeviceChannel,
    MpiExtInfo,
    PayerParams,
    PaymentParams,
    PayRequest,
    PayResponse,
    PayType,
)
from pay.bill_payments.bill_payments.interactions.kazna.exceptions import KaznaAPIError, KaznaAPIErrorCode
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.bill_order import BillOrder
from pay.bill_payments.bill_payments.storage.entities.enums import (
    BillStatus,
    OrderStatus,
    PaymentMethodType,
    TransactionStatus,
)
from pay.bill_payments.bill_payments.storage.entities.order import Order
from pay.bill_payments.bill_payments.storage.entities.transaction import PayerData, Transaction

TOTAL_AMOUNT = int((20000 + 30000 * 0.5) + 2000 * 2)  # 39000


@pytest.fixture
async def first_bill(storage, user, document):
    return await storage.bill.create(
        Bill(
            uid=user.uid,
            supplier_bill_id='bill-1',
            document_id=document.document_id,
            dep_type=DepartmentType.GIBDD,
            status=BillStatus.NEW,
            amount=20000,
            amount_to_pay=20000,
            bill_date=utcnow(),
        )
    )


@pytest.fixture
async def second_bill(storage, user, first_bill, document):
    return await storage.bill.create(
        replace(
            first_bill,
            supplier_bill_id='bill-2',
            bill_id=None,
            amount=30000,
            amount_to_pay=30000,
            discount_size='50',
            discount_date=(utcnow() + timedelta(days=3)).date(),
        )
    )


@pytest.fixture
async def order(storage, user, first_bill, second_bill):
    order = await storage.order.create(Order(status=OrderStatus.NEW, uid=user.uid))

    await storage.bill_order.create(BillOrder(order_id=order.order_id, bill_id=first_bill.bill_id))
    await storage.bill_order.create(BillOrder(order_id=order.order_id, bill_id=second_bill.bill_id))

    return order


@pytest.fixture
def params(order):
    return {
        'uid': order.uid,
        'order_id': order.order_id,
        'payment_method': PaymentMethodType.YANDEX_PAY,
        'payer_full_name': 'Иванов Иван Иванович',
        'payment_token': 'TOKEN',
        'mpi_3ds_info': MPI3DSInfo(
            browser_accept_header='ACCEPT_HEADER',
            browser_color_depth=24,
            browser_ip='192.0.2.1',
            browser_language='ru',
            browser_screen_height=1080,
            browser_screen_width=1920,
            browser_tz='-180',
            browser_user_agent='USER_AGENT',
            browser_javascript_enabled=True,
            window_width=640,
            window_height=480,
        ),
        'return_url': 'https://return-url.test',
    }


@pytest.fixture(autouse=True)
def mock_kazna(mocker):
    return mocker.patch.object(
        KaznaClient,
        'pay',
        mocker.AsyncMock(
            return_value=PayResponse(
                payment_id=44444,
                tds=TDS(
                    acs_url='https://acs.test',
                    creq='creq',
                ),
            )
        ),
    )


@pytest.mark.asyncio
async def test_calls_kazna(params, mock_kazna, order, first_bill, second_bill):
    transaction = await CreateTransactionAction(**params).run()

    mock_kazna.assert_awaited_once_with(
        PayRequest(
            order_id=str(transaction.transaction_id),
            kvit=False,
            payer_params=PayerParams(fio='Иванов Иван Иванович'),
            payment_params=PaymentParams(supplier_bill_id=[first_bill.supplier_bill_id, second_bill.supplier_bill_id]),
            sign=None,
            pay_type=PayType.YANDEXPAY,
            dep_type=DepartmentType.GIBDD,
            amount=TOTAL_AMOUNT,
            mpi_ext_info=MpiExtInfo(
                notification_url='https://notification.invalid',
                browser_accept_header='ACCEPT_HEADER',
                browser_color_depth=24,
                browser_ip='192.0.2.1',
                browser_language='ru',
                browser_screen_height=1080,
                browser_screen_width=1920,
                browser_tz='-180',
                browser_user_agent='USER_AGENT',
                device_channel=DeviceChannel.BROWSER,
                browser_java_enabled=True,
                window_width=640,
                window_height=480,
                tds_notification_url='https://tds.notification.invalid',
            ),
            yp_token='TOKEN',
            return_url='https://return-url.test',
        )
    )


@pytest.mark.parametrize(
    'yenv_type, infered_dep_type',
    (
        pytest.param('development', DepartmentType.GIBDD, id='development-should-fallback'),
        pytest.param('testing', DepartmentType.GIBDD, id='development-should-fallback'),
        pytest.param('production', DepartmentType.UNKNOWN, id='production-should-NOT-fallback'),
        pytest.param('load', DepartmentType.UNKNOWN, id='load-should-NOT-fallback'),
        pytest.param('sandbox', DepartmentType.UNKNOWN, id='sandbox-should-NOT-fallback'),
    ),
)
@pytest.mark.asyncio
async def test_unknown_dep_fallbacks_to_gibdd(
    mocker, storage, params, mock_kazna, order, first_bill, second_bill, yenv_type, infered_dep_type
):
    mocker.patch('yenv.type', yenv_type)
    first_bill = await storage.bill.save(replace(first_bill, dep_type=DepartmentType.UNKNOWN))
    second_bill = await storage.bill.save(replace(second_bill, dep_type=DepartmentType.UNKNOWN))

    await CreateTransactionAction(**params).run()

    [pay_request] = mock_kazna.call_args.args
    assert_that(pay_request.dep_type, equal_to(infered_dep_type))


@pytest.mark.asyncio
async def test_returns_transaction(params, order):
    returned = await CreateTransactionAction(**params).run()

    assert_that(
        returned,
        equal_to(
            Transaction(
                order_id=order.order_id,
                status=TransactionStatus.NEW,
                amount=TOTAL_AMOUNT,
                external_payment_id='44444',
                payer_data=PayerData(payer_full_name='Иванов Иван Иванович'),
                transaction_id=match_equality(not_none()),
                payment_method=PaymentMethodType.YANDEX_PAY,
                acs_url=yarl.URL('https://acs.test?creq=creq'),
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
            ),
        ),
    )


@pytest.mark.asyncio
async def test_stores_transaction_in_db(storage, params):
    returned = await CreateTransactionAction(**params).run()

    stored = await storage.transaction.get(returned.transaction_id)
    stored.acs_url = returned.acs_url

    assert_that(returned, equal_to(stored))


@pytest.mark.asyncio
async def test_when_dep_types_are_mixed__raises_error(storage, params, second_bill):
    second_bill = replace(second_bill, dep_type=DepartmentType.FNS)
    second_bill = await storage.bill.save(second_bill)

    with pytest.raises(MixedDepartmentsError):
        await CreateTransactionAction(**params).run()


@pytest.mark.parametrize('status', set(TransactionStatus) - {TransactionStatus.NEW, TransactionStatus.CANCELLED})
@pytest.mark.asyncio
async def test_when_already_completed(storage, params, order, status):
    transaction = await CreateTransactionAction(**params).run()
    await storage.transaction.save(replace(transaction, status=status))

    with pytest.raises(OrderAlreadyPaidError):
        await CreateTransactionAction(**params).run()


@pytest.mark.asyncio
async def test_cancels_previously_started_transactions(storage, params, order):
    transaction = await CreateTransactionAction(**params).run()

    await CreateTransactionAction(**params).run()

    transaction = await storage.transaction.get(transaction.transaction_id)
    assert_that(transaction.status, equal_to(TransactionStatus.CANCELLED))


@pytest.mark.asyncio
async def test_no_tds(mocker, params, order):
    mocker.patch.object(
        KaznaClient,
        'pay',
        mocker.AsyncMock(return_value=PayResponse(payment_id=44444)),
    )

    transaction = await CreateTransactionAction(**params).run()

    assert_that(transaction.acs_url, equal_to(None))


@pytest.mark.asyncio
async def test_authorizes_user(storage, params, order):
    params['uid'] = order.uid + 1
    with pytest.raises(OrderNotFoundError):
        await CreateTransactionAction(**params).run()


@pytest.mark.asyncio
async def test_unknown_payment_method(storage, params, order):
    params['payment_method'] = 'card'
    with pytest.raises(PaymentMethodNotSupportedError):
        await CreateTransactionAction(**params).run()


@pytest.mark.parametrize(
    'code, expected_exc',
    (
        (KaznaAPIErrorCode.PAYMENT_ALREADY_EXISTS, BillAlreadyPaidError),
        (None, CoreFailError),
    ),
)
@pytest.mark.asyncio
async def test_kazna_errors(storage, mocker, params, order, code, expected_exc):
    mocker.patch.object(
        KaznaClient,
        'pay',
        mocker.AsyncMock(
            side_effect=KaznaAPIError(
                method='method',
                service='service',
                status_code=200,
                params={'code': code},
            )
        ),
    )

    with pytest.raises(expected_exc):
        await CreateTransactionAction(**params).run()
