from datetime import datetime, timedelta, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
import yarl

from sendr_interactions.clients.blackbox.entities import Email, EmailsMode, UIDData, UserInfo
from sendr_pytest.matchers import convert_then_match

from hamcrest import assert_that, equal_to, has_entries, has_properties, match_equality, none, not_none, starts_with

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.receipt import SendOrderReceiptAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.email.send import SendTransactionalEmailAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    InvalidOrderStatusError,
    InvalidRenderContextError,
    OrderNotFoundError,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions import BlackBoxClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import ClassicOrderStatus, PaymentMethodType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order, OrderData
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transactional_email import TransactionalEmail


@pytest.fixture
async def customer(storage):
    return await CreateCustomerAction(uid=500).run()


@pytest.fixture
async def order(customer):
    return Order(
        uid=customer.uid,
        message_id='1:msgid',
        currency='XTS',
        amount=Decimal('100'),
        cashback=Decimal('10'),
        cashback_category=Decimal('0.1'),
        status=ClassicOrderStatus.SUCCESS,
        merchant_id=uuid4(),
        psp_id=uuid4(),
        pretty_id='1635411464-8537',
        data=OrderData(
            last4='1234',
            order_cashback_limit=Decimal('10'),
            merchant_name='name',
            merchant_url='https://url.test'
        ),
        payment_method_type=PaymentMethodType.CARD,
    )


@pytest.fixture
async def order_hold(customer):
    return Order(
        uid=customer.uid,
        message_id='1:msgid',
        currency='XTS',
        amount=Decimal('100'),
        cashback=Decimal('10'),
        cashback_category=Decimal('0.1'),
        status=ClassicOrderStatus.HOLD,
        merchant_id=uuid4(),
        psp_id=uuid4(),
        pretty_id='1635411464-8537',
        data=OrderData(
            last4='1234',
            order_cashback_limit=Decimal('10'),
            merchant_name='name',
            merchant_url='https://url.test'
        ),
        payment_method_type=PaymentMethodType.CARD,
    )


@pytest.fixture
async def transactional_email(storage, rands):
    return await storage.transactional_email.create(
        TransactionalEmail(
            idempotency_key=rands(),
            email='',
            render_context={},
            sender_campaign_slug='',
            reply_email='',
            has_user_generated_content=True,
        )
    )


@pytest.fixture
def default_email():
    return 'email@example.tld'


@pytest.fixture(autouse=True)
def mock_get_email(mocker, default_email):
    user_info = UserInfo(
        uid_data=UIDData(value=500),
        address_list=[Email(address=default_email, default=True, native=False)],
    )
    return mocker.patch.object(
        BlackBoxClient, 'get_user_info', mocker.AsyncMock(return_value=user_info)
    )


@pytest.fixture(autouse=True)
def mock_send_email_action(mock_action, transactional_email):
    return mock_action(SendTransactionalEmailAction, transactional_email)


@pytest.mark.parametrize(
    'order_status', (
        pytest.param(ClassicOrderStatus.SUCCESS, id='status_SUCCESS'),
        pytest.param(ClassicOrderStatus.HOLD, id='status_HOLD'),
    )
)
@pytest.mark.asyncio
async def test_calls_send_email_action(
    storage, order, mock_send_email_action, mock_get_email, yandex_pay_plus_settings, default_email, order_status
):
    order.status = order_status
    await storage.order.create(order)

    await SendOrderReceiptAction(uid=order.uid, order_id=order.order_id).run()

    mock_send_email_action.assert_called_once_with(
        sender_campaign_slug=yandex_pay_plus_settings.SENDER_CAMPAIGN_SLUG_RECEIPT,
        idempotency_key=f'receipt:500:{order.order_id}',
        render_context=match_equality(not_none()),
        email=default_email,
        has_user_generated_content=True,
    )
    mock_get_email.assert_awaited_once_with(
        uid=order.uid,
        emails_mode=EmailsMode.DEFAULT,
        user_ip=BlackBoxClient.SERVERSIDE_USER_IP,
    )


@pytest.mark.parametrize(
    'order_status', (
        pytest.param(ClassicOrderStatus.SUCCESS, id='status_SUCCESS'),
        pytest.param(ClassicOrderStatus.HOLD, id='status_HOLD'),
    )
)
@pytest.mark.asyncio
async def test_sets_order_receipt_transactional_email_id(storage, order, transactional_email, order_status):
    order.status = order_status
    await storage.order.create(order)

    await SendOrderReceiptAction(uid=order.uid, order_id=order.order_id).run()

    order = await storage.order.get(order.uid, order.order_id)
    assert_that(order.receipt_email_id, equal_to(transactional_email.transactional_email_id))


@pytest.mark.asyncio
async def test_invalid_status(storage, order):
    order.status = ClassicOrderStatus.NEW
    await storage.order.create(order)

    with pytest.raises(InvalidOrderStatusError):
        await SendOrderReceiptAction(uid=order.uid, order_id=order.order_id).run()


@pytest.mark.asyncio
async def test_order_not_found(order):
    with pytest.raises(OrderNotFoundError):
        await SendOrderReceiptAction(uid=order.uid, order_id=order.order_id).run()


@pytest.mark.parametrize(
    'last4', (
        pytest.param('', id="last4=''"),
        pytest.param(None, id='last4=None'),
    )
)
@pytest.mark.asyncio
async def test_invalid_render_context(storage, order, last4):
    order.data.last4 = last4
    await storage.order.create(order)

    with pytest.raises(InvalidRenderContextError):
        await SendOrderReceiptAction(uid=order.uid, order_id=order.order_id).run()


class TestRenderContext:
    TIMEZONE_MSK = timezone(timedelta(hours=3))

    @pytest.fixture
    def expected_render_context(self):
        return {
            'last4': '1234',
            'amount': '100,00\xa0XTS',
            'created_at': '22 октября 2021, 20:13',
            'cashback': '10',
            'order_id': '1635411464-8537',
            'merchant_site': 'url.test',
            'has_cashback': True,
            'hit_cashback_limit': False,
            'maillist_subscription_url': match_equality(
                convert_then_match(
                    yarl.URL,
                    has_properties(
                        scheme='https',
                        host='test.pay.yandex.ru',
                        path='/api/v1/maillist/subscription',
                        query=has_entries(token=starts_with('1:')),
                    )
                )
            ),
        }

    @pytest.fixture
    async def created_order(self, storage, order):
        return await storage.order.create(order)

    @pytest.fixture
    def get_actual_render_context(self, mock_send_email_action):
        def _get_actual_render_context():
            return mock_send_email_action.call_args[1]['render_context']
        return _get_actual_render_context

    @pytest.mark.asyncio
    async def test_renders_context(
        self, storage, created_order, get_actual_render_context, expected_render_context
    ):
        created_order = await storage.order.forge_created_datetime(
            created_order.uid, created_order.order_id,
            created=datetime(2021, 10, 22, 20, 13, 45, 123456, tzinfo=self.TIMEZONE_MSK),
        )

        await SendOrderReceiptAction(uid=created_order.uid, order_id=created_order.order_id).run()

        assert_that(
            get_actual_render_context(),
            equal_to(expected_render_context)
        )

    @pytest.mark.parametrize(
        'dt, expected_repr', (
            pytest.param(
                datetime(2021, 10, 22, 9, 13, 45, 123456, tzinfo=TIMEZONE_MSK), '22 октября 2021, 09:13',
                id='am',
            ),
            pytest.param(
                datetime(2021, 10, 22, 20, 13, 45, 123456, tzinfo=TIMEZONE_MSK), '22 октября 2021, 20:13',
                id='pm',
            ),
            pytest.param(
                datetime(2021, 10, 22, 20, 13, 45, 123456, tzinfo=timezone.utc), '22 октября 2021, 23:13',
                id='UTC converted to MSK',
            ),
            pytest.param(
                datetime(2021, 10, 1, 20, 13, 45, 123456, tzinfo=TIMEZONE_MSK), '1 октября 2021, 20:13',
                id='1 digit day',
            ),
        )
    )
    @pytest.mark.asyncio
    async def test_render_datetime(self, storage, created_order, dt, expected_repr, get_actual_render_context):
        created_order = await storage.order.forge_created_datetime(
            created_order.uid, created_order.order_id, created=dt
        )

        await SendOrderReceiptAction(uid=created_order.uid, order_id=created_order.order_id).run()

        assert_that(
            get_actual_render_context()['created_at'],
            equal_to(expected_repr)
        )

    @pytest.mark.parametrize(
        'amount, currency, expected_repr', (
            pytest.param(
                Decimal('100'), 'RUB', '100,00\xa0₽',
                id='RUB 100',
            ),
            pytest.param(
                Decimal('1000'), 'RUB', '1\xa0000,00\xa0₽',
                id='RUB spacers',
            ),
            pytest.param(
                Decimal('100.123'), 'RUB', '100,12\xa0₽',
                id='RUB minors',
            ),
            pytest.param(
                Decimal('100'), 'BYN', '100,00\xa0BYN',
                id='BYN 100',
            ),
            pytest.param(
                Decimal('1000'), 'BYN', '1\xa0000,00\xa0BYN',
                id='BYN spacers',
            ),
            pytest.param(
                Decimal('100.123'), 'BYN', '100,12\xa0BYN',
                id='BYN minors',
            ),
            pytest.param(
                Decimal('100'), 'USD', '100,00\xa0$',
                id='USD 100',
            ),
            pytest.param(
                Decimal('1000'), 'USD', '1\xa0000,00\xa0$',
                id='USD spacers',
            ),
            pytest.param(
                Decimal('100.123'), 'USD', '100,12\xa0$',
                id='USD minors',
            ),
            pytest.param(
                Decimal('100'), 'EUR', '100,00\xa0€',
                id='EUR 100',
            ),
            pytest.param(
                Decimal('1000'), 'EUR', '1\xa0000,00\xa0€',
                id='EUR spacers',
            ),
            pytest.param(
                Decimal('100.123'), 'EUR', '100,12\xa0€',
                id='EUR minors',
            ),
        )
    )
    @pytest.mark.asyncio
    async def test_render_amount(
        self, storage, created_order, currency, amount, expected_repr, get_actual_render_context
    ):
        created_order.amount = amount
        created_order.currency = currency
        created_order = await storage.order.save(created_order)

        await SendOrderReceiptAction(uid=created_order.uid, order_id=created_order.order_id).run()

        assert_that(
            get_actual_render_context()['amount'],
            equal_to(expected_repr)
        )

    @pytest.mark.parametrize(
        'merchant_url', (
            pytest.param(None, id='None'),
            pytest.param('', id='empty'),
            pytest.param('/foo/bar', id='relative path'),
            pytest.param('hello', id='word'),
            pytest.param('hello world', id='sentence'),
        )
    )
    @pytest.mark.asyncio
    async def test_bad_merchant_site_coersed_to_none(
        self, storage, created_order, merchant_url, get_actual_render_context
    ):
        created_order.data.merchant_url = merchant_url
        created_order = await storage.order.save(created_order)

        await SendOrderReceiptAction(uid=created_order.uid, order_id=created_order.order_id).run()

        assert_that(
            get_actual_render_context()['merchant_site'],
            none(),
        )

    @pytest.mark.parametrize(
        'merchant_url, expected_repr', (
            pytest.param(
                'https://site.test', 'site.test',
                id='absolute url',
            ),
            pytest.param(
                'https://user:password@site.test/pa/th?qu=ery#fragment', 'site.test',
                id='full absolute url',
            ),
            pytest.param(
                'site.test', 'site.test',
                id='hostname',
            ),
            pytest.param(
                'https://site.test:1234', 'site.test:1234',
                id='hostname with custom port',
            ),
        )
    )
    @pytest.mark.asyncio
    async def test_render_merchant_site(
        self, storage, created_order, merchant_url, expected_repr, get_actual_render_context
    ):
        created_order.data.merchant_url = merchant_url
        created_order = await storage.order.save(created_order)
        await SendOrderReceiptAction(uid=created_order.uid, order_id=created_order.order_id).run()

        assert_that(
            get_actual_render_context()['merchant_site'],
            equal_to(expected_repr)
        )

    @pytest.mark.asyncio
    async def test_render_cashback(self, storage, created_order, get_actual_render_context):
        created_order.cashback = Decimal('10.5')
        created_order = await storage.order.save(created_order)
        await SendOrderReceiptAction(uid=created_order.uid, order_id=created_order.order_id).run()

        assert_that(
            get_actual_render_context()['cashback'],
            equal_to('10')
        )

    @pytest.mark.parametrize(
        'cashback, expected_has_cashback', (
            (Decimal('10'), True),
            (Decimal('0'), False),
        ),
    )
    @pytest.mark.asyncio
    async def test_has_cashback(
        self, storage, created_order, get_actual_render_context, cashback, expected_has_cashback,
    ):
        created_order.cashback = cashback
        created_order = await storage.order.save(created_order)
        await SendOrderReceiptAction(uid=created_order.uid, order_id=created_order.order_id).run()

        assert_that(
            get_actual_render_context()['has_cashback'],
            equal_to(expected_has_cashback)
        )

    @pytest.mark.parametrize(
        'cashback, order_cashback_limit, expected_hit_cashback_limit', (
            (Decimal('10'), Decimal('10'), False),
            (Decimal('9'), Decimal('10'), True),
        ),
    )
    @pytest.mark.asyncio
    async def test_hit_cashback_limit(
        self,
        storage,
        created_order,
        get_actual_render_context,
        cashback,
        order_cashback_limit,
        expected_hit_cashback_limit,
    ):
        created_order.cashback = cashback
        created_order.data.order_cashback_limit = order_cashback_limit
        created_order = await storage.order.save(created_order)
        await SendOrderReceiptAction(uid=created_order.uid, order_id=created_order.order_id).run()

        assert_that(
            get_actual_render_context()['hit_cashback_limit'],
            equal_to(expected_hit_cashback_limit)
        )
