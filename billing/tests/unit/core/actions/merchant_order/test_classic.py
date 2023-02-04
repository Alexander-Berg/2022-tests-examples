import uuid
from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.card import Card, CardNetwork, ExpirationDate, IssuerBank
from pay.lib.entities.order import PaymentMethodType as CheckoutPaymentMethodType
from pay.lib.entities.payment_sheet import PaymentMerchant

from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action, spy_action

from hamcrest import assert_that, has_entries

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.classic import (
    CreateCorrespondingClassicOrderAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.create import CreateOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions.yandex_pay import YandexPayClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import PaymentMethodType as ClassicPaymentMethodType

fixtures_dummy_use_for_linters = [explain_call_asserts, mock_action, spy_action]


@pytest.mark.asyncio
async def test_returned(mock_order, params):
    returned = await CreateCorrespondingClassicOrderAction(**params).run()

    assert_that(
        returned,
        equal_to(mock_order),
    )


@pytest.mark.asyncio
async def test_calls_create_classic_order(checkout_order, mock_create_order_action, params):
    await CreateCorrespondingClassicOrderAction(**params).run()

    mock_create_order_action.assert_run_once_with(
        uid=checkout_order.uid,
        message_id=params['message_id'],
        currency=checkout_order.currency_code,
        amount=checkout_order.authorize_amount,
        psp_id=params['psp_id'],
        merchant=PaymentMerchant(
            id=checkout_order.merchant_id,
            name='NOT_APPLICABLE',
        ),
        trust_card_id='card-x12345678',
        payment_method_type=ClassicPaymentMethodType.CARD,
        last4='0000',
        antifraud_external_id=None,
        card_id=uuid.UUID('977ee45c-cbef-4088-9be2-cd36624e3640'),
        card_network=CardNetwork.MIR.value,
    )


@pytest.mark.asyncio
async def test_creates_classic_order__when_ypay_card_id_is_undefined(mocker, mock_create_order_action, params):
    mocker.patch.object(
        YandexPayClient,
        'get_user_card',
        mocker.AsyncMock(
            return_value=Card(
                card_id='card-x12345678',
                trust_card_id='card-x12345678',
                card_network=CardNetwork.MIR,
                issuer_bank=IssuerBank.BANKROSSIYA,
                expiration_date=ExpirationDate(2000, 1),
                last4='0000',
            ),
        ),
    )

    await CreateCorrespondingClassicOrderAction(**params).run()

    assert_that(
        mock_create_order_action.call_args.kwargs,
        has_entries(
            {
                'card_id': None,
                'trust_card_id': 'card-x12345678',
            }
        ),
    )


@pytest.mark.parametrize(
    'checkout_method, classic_method',
    [
        (
            checkout_method,
            {
                CheckoutPaymentMethodType.CARD: ClassicPaymentMethodType.CARD,
                CheckoutPaymentMethodType.CARD_ON_DELIVERY: ClassicPaymentMethodType.CARD_ON_DELIVERY,
                CheckoutPaymentMethodType.SPLIT: ClassicPaymentMethodType.SPLIT,
                CheckoutPaymentMethodType.CASH_ON_DELIVERY: ClassicPaymentMethodType.CASH_ON_DELIVERY,
            }[checkout_method],
        )
        for checkout_method in CheckoutPaymentMethodType
    ],
)
@pytest.mark.asyncio
async def test_payment_methods(
    storage, checkout_order, mock_create_order_action, params, checkout_method, classic_method
):
    params['checkout_order'] = await storage.checkout_order.save(
        replace(checkout_order, payment_method_type=checkout_method)
    )

    await CreateCorrespondingClassicOrderAction(**params).run()

    assert_that(
        mock_create_order_action.call_args.kwargs,
        has_entries(
            {
                'payment_method_type': classic_method,
            }
        ),
    )


@pytest.fixture
def params(checkout_order):
    return {
        'checkout_order': checkout_order,
        'message_id': 'msgid',
        'psp_id': uuid.UUID('00000000-0000-0000-0000-000000000000'),
        'card_id': 'input-card-id',
    }


@pytest.fixture
def mock_order():
    return object()


@pytest.fixture(autouse=True)
def mock_create_order_action(mock_action, mock_order):
    return mock_action(CreateOrderAction, return_value=mock_order)


@pytest.fixture(autouse=True)
def mock_get_user_card(mocker):
    return mocker.patch.object(
        YandexPayClient,
        'get_user_card',
        mocker.AsyncMock(
            return_value=Card(
                card_id='977ee45c-cbef-4088-9be2-cd36624e3640',
                trust_card_id='card-x12345678',
                card_network=CardNetwork.MIR,
                issuer_bank=IssuerBank.BANKROSSIYA,
                expiration_date=ExpirationDate(2000, 1),
                last4='0000',
            ),
        ),
    )


@pytest.fixture
async def checkout_order(storage, stored_checkout_order):
    return await storage.checkout_order.save(
        replace(
            stored_checkout_order,
            payment_method_type=CheckoutPaymentMethodType.CARD,
            authorize_amount=Decimal('123.45'),
            order_amount=Decimal('123.46'),
        )
    )
