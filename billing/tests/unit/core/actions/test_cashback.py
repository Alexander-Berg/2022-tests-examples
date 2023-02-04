from decimal import Decimal
from uuid import UUID, uuid4

import pytest
from pay.lib.entities.payment_sheet import PaymentOrder, PaymentOrderTotal

from hamcrest import assert_that, equal_to, has_entry, not_none
from hamcrest.library.integration import match_equality

from billing.yandex_pay.yandex_pay.conf import settings
from billing.yandex_pay.yandex_pay.core.actions.antifraud.cashback import CheckCashbackAllowedAction
from billing.yandex_pay.yandex_pay.core.actions.card.get import GetUserCardByCardIdAction
from billing.yandex_pay.yandex_pay.core.actions.cashback import GetCashbackAction
from billing.yandex_pay.yandex_pay.core.entities.card import UserCard
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import (
    PaymentMerchant, PaymentMethod, PaymentMethodType, PaymentSheet
)
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreCardNotFoundError, CoreInvalidCurrencyError, CoreMerchantNotFoundError, CoreMissingCardPaymentMethodError,
    CorePSPNotFoundError
)
from billing.yandex_pay.yandex_pay.interactions.plus_backend import YandexPayPlusClient
from billing.yandex_pay.yandex_pay.interactions.plus_backend.entities import YandexPayPlusMerchant

FAKE_UID = 123987
FAKE_TVM_TICKET = 'fake_tvm_user_ticket'
FAKE_LOGIN_ID = 'fake_login_id'


@pytest.fixture
def sheet():
    return PaymentSheet(
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
                method_type=PaymentMethodType.CARD,
                gateway='gw-id',
                gateway_merchant_id='yandex-payments',
                allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
            PaymentMethod(
                method_type=PaymentMethodType.CASH,
            )
        ],
        order=PaymentOrder(
            id='order-id',
            total=PaymentOrderTotal(
                amount=Decimal('100.00'),
            ),
        ),
    )


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(
        PSP(
            psp_external_id='gw-id',
            psp_id=uuid4(),
            public_key='pubkey',
            public_key_signature='pubkeysig',
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
    return {'category': Decimal('0.05'), 'amount': Decimal('5.0')}


@pytest.fixture
def fake_user():
    return User(FAKE_UID, FAKE_TVM_TICKET, FAKE_LOGIN_ID)


@pytest.fixture(autouse=True)
def mock_plus_client(mocker, fake_cashback_limit):
    return mocker.patch.object(
        YandexPayPlusClient,
        'get_user_cashback_amount',
        mocker.AsyncMock(return_value=fake_cashback_limit),
    )


@pytest.fixture(autouse=True)
def mock_check_cashback_allowed_action(mock_action):
    return mock_action(CheckCashbackAllowedAction, True)


@pytest.mark.asyncio
async def test_get_cashback(fake_user, fake_cashback_limit, sheet, merchant, psp):
    result = await GetCashbackAction(
        sheet=sheet,
        user_agent='agent007',
        user_ip='ip42',
        user=fake_user,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))


@pytest.mark.asyncio
async def test_cashback_without_card_payment_method(fake_user, sheet, merchant, psp):
    sheet.payment_methods = sheet.payment_methods[1:]
    with pytest.raises(CoreMissingCardPaymentMethodError):
        await GetCashbackAction(
            sheet=sheet,
            user_agent='agent007',
            user_ip='ip42',
            user=fake_user,
        ).run()


@pytest.mark.asyncio
async def test_zero_cashback_for_cash_payment(fake_user, sheet, mock_plus_client):
    result = await GetCashbackAction(
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        payment_method_type=PaymentMethodType.CASH,
        user=fake_user,
    ).run()

    assert_that(result, equal_to(dict(category=Decimal('0'), amount=Decimal('0'))))
    mock_plus_client.assert_not_called()


@pytest.mark.asyncio
async def test_get_cashback_with_card(
    fake_user, fake_cashback_limit, sheet, merchant, psp, mocker, mock_plus_client
):
    trust_card_id = 'card-x123abc'
    fake_user_card = mocker.Mock(spec=UserCard, trust_card_id=trust_card_id)
    mock_get_card = mocker.patch.object(
        GetUserCardByCardIdAction, 'run', mocker.AsyncMock(return_value=fake_user_card)
    )

    result = await GetCashbackAction(
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
        card_id=trust_card_id,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))
    mock_get_card.assert_awaited_once_with()

    mock_plus_client.assert_called_once()
    _, call_kwargs = mock_plus_client.call_args
    assert_that(call_kwargs, has_entry('trust_card_id', trust_card_id))


@pytest.mark.asyncio
async def test_do_not_resolve_trust(
    fake_user, fake_cashback_limit, sheet, merchant, psp, mocker, mock_plus_client, mock_action
):
    trust_card_id = 'card-x123abc'
    mock_get_card = mock_action(GetUserCardByCardIdAction)

    await GetCashbackAction(
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
        card_id=trust_card_id,
    ).run()

    mock_get_card.assert_called_once_with(
        fake_user,
        trust_card_id,
        skip_trust_if_possible=False
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('cashback_category_id', [None, '0.05'])
async def test_get_cashback_with_category_id(
    fake_user,
    fake_cashback_limit,
    sheet,
    merchant,
    psp,
    mock_plus_client,
    cashback_category_id,
):
    result = await GetCashbackAction(
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
        cashback_category_id=cashback_category_id,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))

    mock_plus_client.assert_called_once()
    _, call_kwargs = mock_plus_client.call_args
    assert_that(call_kwargs, has_entry('cashback_category_id', cashback_category_id))


@pytest.mark.asyncio
async def test_get_cashback__when_user_is_none(fake_cashback_limit, sheet, merchant, psp):
    result = await GetCashbackAction(
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        user=None,
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))


@pytest.mark.asyncio
async def test_plus_client_was_called(fake_user, mock_plus_client, sheet, merchant, psp):
    await GetCashbackAction(
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
    ).run()

    mock_plus_client.assert_called_once_with(
        user_ticket=fake_user.tvm_ticket,
        merchant=YandexPayPlusMerchant(
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
@pytest.mark.parametrize('currency', settings.ALLOWED_CURRENCIES)
async def test_get_cashback_succeeds_for_all_allowed_currencies(
    fake_user, mocker, mock_plus_client, sheet, merchant, psp, currency
):
    trust_card_id = 'card-x123abc'
    fake_user_card = mocker.Mock(spec=UserCard, trust_card_id=trust_card_id)
    mocker.patch.object(GetUserCardByCardIdAction, 'run', mocker.AsyncMock(return_value=fake_user_card))
    mocker.patch.object(CheckCashbackAllowedAction, 'run', mocker.AsyncMock(return_value=True))

    sheet.currency_code = currency
    await GetCashbackAction(
        sheet=sheet,
        user_agent='agent007',
        user_ip='ip',
        user=fake_user,
        card_id=trust_card_id,
    ).run()

    mock_plus_client.assert_called_once_with(
        user_ticket=fake_user.tvm_ticket,
        merchant=YandexPayPlusMerchant(
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
async def test_other_currencies_not_allowed(fake_user, mock_plus_client, sheet, merchant, psp):
    sheet.currency_code = 'SUR'
    with pytest.raises(CoreInvalidCurrencyError):
        await GetCashbackAction(
            sheet=sheet,
            user_agent='agent',
            user_ip='ip',
            user=fake_user,
        ).run()

    mock_plus_client.assert_not_called()


@pytest.mark.asyncio
async def test_merchant_not_found(fake_user, mock_plus_client, sheet, psp):
    with pytest.raises(CoreMerchantNotFoundError):
        await GetCashbackAction(
            sheet=sheet,
            user_agent='agent',
            user_ip='ip',
            user=fake_user,
        ).run()


@pytest.mark.asyncio
async def test_psp_not_found(fake_user, mock_plus_client, sheet, merchant):
    with pytest.raises(CorePSPNotFoundError):
        await GetCashbackAction(
            sheet=sheet,
            user_agent='agent',
            user_ip='ip',
            user=fake_user,
        ).run()


@pytest.mark.asyncio
async def test_card_ignored_if_user_not_given(
    mock_plus_client, sheet, merchant, psp, mocker, fake_cashback_limit
):
    mock_get_card = mocker.AsyncMock()
    mocker.patch.object(GetUserCardByCardIdAction, 'run', mock_get_card)

    result = await GetCashbackAction(
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        user=None,
        card_id='fake_card',
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))

    mock_plus_client.assert_called_once()
    _, kwargs = mock_plus_client.call_args
    assert_that(kwargs, has_entry('trust_card_id', None))

    mock_get_card.assert_not_awaited()


@pytest.mark.asyncio
async def test_card_ignored_if_not_found(
    mock_plus_client, sheet, merchant, psp, fake_user, mocker, fake_cashback_limit
):
    mock_get_card = mocker.AsyncMock(side_effect=CoreCardNotFoundError)
    mocker.patch.object(GetUserCardByCardIdAction, 'run', mock_get_card)

    result = await GetCashbackAction(
        sheet=sheet,
        user_agent='agent',
        user_ip='ip',
        user=fake_user,
        card_id='fake_card',
    ).run()

    assert_that(result, equal_to(fake_cashback_limit))

    mock_plus_client.assert_called_once()
    _, kwargs = mock_plus_client.call_args
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
    fake_user_card = mocker.Mock(spec=UserCard, trust_card_id=trust_card_id)
    mocker.patch.object(
        GetUserCardByCardIdAction, 'run', mocker.AsyncMock(return_value=fake_user_card)
    )
    run_mock = mocker.patch.object(
        CheckCashbackAllowedAction, 'run', mocker.AsyncMock(return_value=True)
    )

    await GetCashbackAction(
        sheet=sheet,
        user_agent='agent007',
        user_ip='ip42',
        user=fake_user,
        card_id=trust_card_id,
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
    await GetCashbackAction(
        sheet=sheet,
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
    mock_plus_client,
):
    trust_card_id = 'card-x123abc'
    fake_user_card = mocker.Mock(spec=UserCard, trust_card_id=trust_card_id)
    mocker.patch.object(
        GetUserCardByCardIdAction, 'run', mocker.AsyncMock(return_value=fake_user_card)
    )
    mocker.patch.object(
        CheckCashbackAllowedAction, 'run', mocker.AsyncMock(return_value=False)
    )

    cashback = await GetCashbackAction(
        sheet=sheet,
        user_agent='agent007',
        user_ip='ip42',
        user=fake_user,
        card_id=trust_card_id,
    ).run()

    assert cashback == {
        'category': Decimal('0'),
        'amount': Decimal('0'),
    }
    mock_plus_client.assert_not_called()
