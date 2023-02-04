import copy
from decimal import Decimal
from uuid import UUID, uuid4

import pytest
from pay.lib.entities.card import Card
from pay.lib.entities.enums import AuthMethod, CardNetwork, PaymentMethodType
from pay.lib.entities.payment_sheet import (
    ClassicPaymentMethodType,
    PaymentMerchant,
    PaymentMethod,
    PaymentOrder,
    PaymentOrderTotal,
    PaymentSheet,
)
from pay.lib.interactions.yandex_pay.exceptions import CardNotFoundError

from hamcrest import assert_that, equal_to, has_entry, not_none
from hamcrest.library.integration import match_equality

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.antifraud import CheckCashbackAllowedAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.get import GetCashbackAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.get_adapter import GetCashbackAdapterAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.cashback import Cashback, CashbackRequest, CheckoutSheet
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreMerchantNotFoundError,
    CorePSPNotFoundError,
    CurrencyNotSupportedError,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions import YandexPayClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP

CHECKOUT_SHEET = CheckoutSheet(
    merchant_id=UUID('789b29e6-d8f2-4e14-8c3f-33679ca590e3'),
    cart_total=Decimal('100.00'),
    currency_code='rub',
)
PAYMENT_SHEET = PaymentSheet(
    merchant=PaymentMerchant(
        id=UUID('789b29e6-d8f2-4e14-8c3f-33679ca590e3'),
        name='merchant-name',
        url='http://site.test',
    ),
    version=2,
    currency_code='rub',
    country_code='ru',
    payment_methods=[
        PaymentMethod(
            method_type=ClassicPaymentMethodType.CARD,
            gateway='gw-id',
            gateway_merchant_id='yandex-payments',
            allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
            allowed_card_networks=[CardNetwork.MASTERCARD],
        ),
        PaymentMethod(
            method_type=ClassicPaymentMethodType.CASH,
        ),
    ],
    order=PaymentOrder(
        id='order-id',
        total=PaymentOrderTotal(
            amount=Decimal('100.00'),
        ),
    ),
)


@pytest.fixture
def sheet():
    return copy.deepcopy(PAYMENT_SHEET)


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(
        PSP(
            psp_external_id='gw-id',
            psp_id=uuid4(),
        )
    )


@pytest.fixture
async def merchant(storage, sheet):
    return await storage.merchant.create(
        Merchant(
            merchant_id=sheet.merchant.id,
            name='whatever',
        )
    )


@pytest.fixture
def fake_cashback_limit():
    return Cashback(category=Decimal('0.05'), amount=Decimal('5.0'), order_limit=Decimal('3000.0'))


@pytest.fixture
def fake_user(entity_auth_user):
    return entity_auth_user


@pytest.fixture(autouse=True)
def mock_get_cashback(mock_action, fake_cashback_limit):
    return mock_action(GetCashbackAction, fake_cashback_limit)


@pytest.fixture(autouse=True)
def mock_get_card(mocker):
    return mocker.patch.object(
        YandexPayClient,
        'get_user_card',
        mocker.AsyncMock(return_value=mocker.Mock(spec=Card, trust_card_id='card-x123abc')),
    )


@pytest.fixture(autouse=True)
def mock_check_cashback_allowed_action(mock_action):
    return mock_action(CheckCashbackAllowedAction, True)


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'params',
    (
        CashbackRequest(sheet=PAYMENT_SHEET),
        CashbackRequest(checkout_sheet=CHECKOUT_SHEET),
    ),
)
async def test_get_cashback(fake_user, fake_cashback_limit, params, merchant, psp):
    result = await GetCashbackAdapterAction(
        cashbackrequest=params,
        user_agent='agent007',
        user_ip='ip42',
        user=fake_user,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))


@pytest.mark.asyncio
async def test_cashback_without_card_payment_method(fake_user, sheet, merchant, psp):
    sheet.payment_methods = sheet.payment_methods[1:]
    result = await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet),
        user_agent='agent007',
        user_ip='ip42',
        user=fake_user,
    ).run()
    assert_that(
        result, equal_to(Cashback(amount=Decimal('5.0'), category=Decimal('0.05'), order_limit=Decimal('3000.0')))
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'payment_method_type', (PaymentMethodType.CASH_ON_DELIVERY, PaymentMethodType.CARD_ON_DELIVERY)
)
async def test_zero_cashback_for_cash_payment(fake_user, sheet, mock_get_cashback, payment_method_type):
    result = await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(
            sheet=sheet,
            payment_method_type=payment_method_type,
        ),
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
    ).run()

    assert_that(result, equal_to(Cashback(category=Decimal('0'), amount=Decimal('0'), order_limit=Decimal('0'))))
    mock_get_cashback.assert_not_called()


@pytest.mark.asyncio
async def test_get_cashback_with_card(
    fake_user, fake_cashback_limit, sheet, merchant, psp, mock_get_cashback, mock_get_card
):
    trust_card_id = 'card-x123abc'

    result = await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(
            sheet=sheet,
            card_id=trust_card_id,
        ),
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))
    mock_get_card.assert_awaited_once_with(uid=fake_user.uid, card_id=trust_card_id)

    mock_get_cashback.assert_called_once()
    _, call_kwargs = mock_get_cashback.call_args
    assert_that(call_kwargs, has_entry('trust_card_id', trust_card_id))


@pytest.mark.asyncio
@pytest.mark.parametrize('cashback_category_id', [None, '0.05'])
async def test_get_cashback_with_category_id(
    fake_user,
    fake_cashback_limit,
    sheet,
    merchant,
    psp,
    mock_get_cashback,
    cashback_category_id,
):
    result = await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet),
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
        cashback_category_id=cashback_category_id,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))

    mock_get_cashback.assert_called_once()
    _, call_kwargs = mock_get_cashback.call_args
    assert_that(call_kwargs, has_entry('cashback_category_id', cashback_category_id))


@pytest.mark.asyncio
async def test_get_cashback__when_user_is_none(fake_cashback_limit, sheet, merchant, psp):
    result = await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet),
        user_agent='agent',
        user_ip='ip',
        user=None,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))


@pytest.mark.asyncio
async def test_plus_client_was_called(fake_user, mock_get_cashback, sheet, merchant, psp):
    await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet),
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
    ).run()

    mock_get_cashback.assert_called_once_with(
        uid=fake_user.uid,
        merchant=PaymentMerchant(
            id=sheet.merchant.id,
            name='merchant-name',
            url='http://site.test',
        ),
        psp_id=psp.psp_id,
        currency='RUB',
        amount=Decimal('100'),
        trust_card_id=None,
        cashback_category_id=None,
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('currency', ('RUB', 'USD', 'EUR', 'GBP'))
async def test_get_cashback_succeeds_for_all_allowed_currencies(
    fake_user, mocker, mock_get_cashback, sheet, merchant, psp, currency
):
    trust_card_id = 'card-x123abc'
    mocker.patch.object(CheckCashbackAllowedAction, 'run', mocker.AsyncMock(return_value=True))

    sheet.currency_code = currency
    await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet, card_id=trust_card_id),
        user_agent='agent007',
        user_ip='ip',
        user=fake_user,
    ).run()

    mock_get_cashback.assert_called_once_with(
        uid=fake_user.uid,
        merchant=PaymentMerchant(
            id=sheet.merchant.id,
            name='merchant-name',
            url='http://site.test',
        ),
        psp_id=psp.psp_id,
        currency=currency,
        amount=Decimal('100'),
        trust_card_id=trust_card_id,
        cashback_category_id=None,
    )


@pytest.mark.asyncio
async def test_other_currencies_not_allowed(fake_user, mock_get_cashback, sheet, merchant, psp):
    sheet.currency_code = 'XTS'
    trust_card_id = 'card-x123abc'
    with pytest.raises(CurrencyNotSupportedError):
        await GetCashbackAdapterAction(
            cashbackrequest=CashbackRequest(sheet=sheet, card_id=trust_card_id),
            user_agent='agent',
            user_ip='ip',
            user=fake_user,
        ).run()

    mock_get_cashback.assert_not_called()


@pytest.mark.asyncio
async def test_merchant_not_found(fake_user, mock_get_cashback, sheet, psp):
    with pytest.raises(CoreMerchantNotFoundError):
        await GetCashbackAdapterAction(
            cashbackrequest=CashbackRequest(sheet=sheet),
            user_agent='agent',
            user_ip='ip',
            user=fake_user,
        ).run()


@pytest.mark.asyncio
async def test_psp_not_found(fake_user, mock_get_cashback, sheet, merchant):
    with pytest.raises(CorePSPNotFoundError):
        await GetCashbackAdapterAction(
            cashbackrequest=CashbackRequest(sheet=sheet),
            user_agent='agent',
            user_ip='ip',
            user=fake_user,
        ).run()


@pytest.mark.asyncio
async def test_card_ignored_if_user_not_given(
    mock_get_cashback, sheet, merchant, psp, fake_cashback_limit, mock_get_card
):

    result = await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet, card_id='fake_card'),
        user_agent='agent',
        user_ip='ip',
        user=None,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))

    mock_get_cashback.assert_called_once()
    _, kwargs = mock_get_cashback.call_args
    assert_that(kwargs, has_entry('trust_card_id', None))

    mock_get_card.assert_not_awaited()


@pytest.mark.asyncio
async def test_card_ignored_if_not_found(
    mock_get_cashback, sheet, merchant, psp, fake_user, mocker, fake_cashback_limit
):
    mock_get_card = mocker.patch.object(
        YandexPayClient,
        'get_user_card',
        mocker.AsyncMock(side_effect=CardNotFoundError(status_code=404, service='x', method='x')),
    )

    result = await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet, card_id='fake_card'),
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))

    mock_get_cashback.assert_called_once()
    _, kwargs = mock_get_cashback.call_args
    assert_that(kwargs, has_entry('trust_card_id', None))

    mock_get_card.assert_awaited_once()


@pytest.mark.asyncio
async def test_calls_check_allowed_by_antifraud_with_expected_args(
    fake_user,
    sheet,
    mock_check_cashback_allowed_action,
    psp,
    merchant,
    mocker,
):
    trust_card_id = 'card-x123abc'
    run_mock = mocker.patch.object(CheckCashbackAllowedAction, 'run', mocker.AsyncMock(return_value=True))

    await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet, card_id=trust_card_id),
        user_agent='agent007',
        user_ip='ip42',
        user=fake_user,
    ).run()

    mock_check_cashback_allowed_action.assert_called_once_with(
        user=fake_user,
        external_id=match_equality(not_none()),
        amount=Decimal('100.00'),
        trust_card_id=trust_card_id,
        user_agent='agent007',
        user_ip='ip42',
        currency_number='643',
    )
    run_mock.assert_awaited_once()


@pytest.mark.asyncio
async def test_should_not_check_cashback_antifraud_if_no_card_id_was_passed(
    fake_user,
    sheet,
    mock_check_cashback_allowed_action,
    psp,
    merchant,
):
    await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet),
        user_agent='agent007',
        user_ip='ip42',
        user=fake_user,
    ).run()

    mock_check_cashback_allowed_action.assert_not_called()


@pytest.mark.asyncio
async def test_should_return_zero_cashback_if_denied_by_antifraud(
    fake_user,
    sheet,
    mock_check_cashback_allowed_action,
    psp,
    merchant,
    mocker,
    mock_get_cashback,
):
    trust_card_id = 'card-x123abc'
    mocker.patch.object(CheckCashbackAllowedAction, 'run', mocker.AsyncMock(return_value=False))

    cashback = await GetCashbackAdapterAction(
        cashbackrequest=CashbackRequest(sheet=sheet, card_id=trust_card_id),
        user_agent='agent007',
        user_ip='ip42',
        user=fake_user,
    ).run()

    assert cashback == Cashback(
        category=Decimal('0'),
        amount=Decimal('0'),
        order_limit=Decimal('0'),
    )
    mock_get_cashback.assert_not_called()
