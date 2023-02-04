import logging
from decimal import Decimal
from uuid import UUID, uuid4

import pytest
from pay.lib.entities.enums import PaymentItemType
from pay.lib.entities.payment_sheet import (
    MITOptionsType, PaymentItemQuantity, PaymentOrder, PaymentOrderItem, PaymentOrderTotal
)

from hamcrest import assert_that, has_entries, has_items, has_properties

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.conf import settings
from billing.yandex_pay.yandex_pay.core.actions.payment_sheet.validate_sheet import ValidatePaymentSheetAction
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import (
    MITOptions, PaymentMerchant, PaymentMethod, PaymentSheet
)
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreAmountMismatchError, CoreInsecureMerchantOriginSchemaError, CoreInvalidAmountError, CoreInvalidCountryError,
    CoreInvalidCurrencyError, CoreInvalidVersionError, CoreMerchantAccountError, CoreMerchantNotFoundError,
    CoreMerchantOriginNotFound, CorePSPAccountError, CorePSPNotFoundError
)
from billing.yandex_pay.yandex_pay.tests.matchers import convert_then_match

PSP_EXTERNAL_ID = 'yandex-trust'
MERCHANT_ID = UUID('789b29e6-d8f2-4e14-8c3f-33679ca590e3')
MERCHANT_ORIGIN = 'https://market.yandex.ru:443'


@pytest.fixture
def correct_sheet():
    return PaymentSheet(
        merchant=PaymentMerchant(
            id=MERCHANT_ID,
            name='merchant-name',
        ),
        version=2,
        currency_code='rub',
        country_code='ru',
        payment_methods=[
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway=PSP_EXTERNAL_ID,
                gateway_merchant_id='yandex-payments',
                allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
        ],
        order=PaymentOrder(
            id='order-id',
            total=PaymentOrderTotal(
                amount=Decimal('1.00'),
            ),
        ),
    )


@pytest.fixture
def correct_sheet_pan_only():
    return PaymentSheet(
        merchant=PaymentMerchant(
            id=MERCHANT_ID,
            name='merchant-name',
        ),
        version=2,
        currency_code='RUB',
        country_code='RU',
        payment_methods=[
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway=PSP_EXTERNAL_ID,
                gateway_merchant_id='yandex-payments',
                allowed_auth_methods=[AuthMethod.PAN_ONLY],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
        ],
        order=PaymentOrder(
            id='order-id',
            total=PaymentOrderTotal(
                amount=Decimal('1.00'),
            ),
        ),
    )


@pytest.fixture(autouse=True)
async def merchant(storage):
    return await storage.merchant.create(
        Merchant(
            merchant_id=MERCHANT_ID,
            name='the-name',
        )
    )


@pytest.fixture(autouse=True)
async def merchant_origin(storage, merchant: Merchant):
    return await storage.merchant_origin.create(
        MerchantOrigin(
            merchant_id=merchant.merchant_id,
            origin=MERCHANT_ORIGIN,
        )
    )


@pytest.fixture(autouse=True)
async def psp(storage):
    return await create_psp_entity(
        storage,
        PSP(
            psp_id=uuid4(),
            psp_external_id=PSP_EXTERNAL_ID,
            public_key='public-key',
            public_key_signature='public-key-signature',
        )
    )


class TestValidateAction:
    @pytest.mark.asyncio
    async def test_error_because_of_unknown_psp(self, correct_sheet):
        correct_sheet.payment_methods = correct_sheet.payment_methods[:1]
        correct_sheet.payment_methods[0].gateway = 'go kiss yourself!'
        with pytest.raises(CorePSPNotFoundError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_of_invalid_currency(self, correct_sheet):
        correct_sheet.currency_code = 'voucher'
        with pytest.raises(CoreInvalidCurrencyError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_of_invalid_country(self, correct_sheet):
        correct_sheet.country_code = 'wonderland'
        with pytest.raises(CoreInvalidCountryError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_of_too_many_floating_point_digits_in_amount(
        self, correct_sheet
    ):
        correct_sheet.order.total.amount = Decimal('1.001')
        with pytest.raises(CoreInvalidAmountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_of_zero_amount(self, correct_sheet):
        correct_sheet.order.total.amount = Decimal('0.0')
        with pytest.raises(CoreInvalidAmountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_of_too_many_floating_point_digits_in_item_amount(
        self, correct_sheet
    ):
        correct_sheet.order.items = [PaymentOrderItem(amount=Decimal('1.001'), label='hi')]
        with pytest.raises(CoreInvalidAmountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validates_zero_item_amount(self, correct_sheet):
        correct_sheet.order.items = [correct_sheet.order.total, PaymentOrderItem(amount=Decimal('0.0'), label='hi')]
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_total_amount_differs_from_items_amount(
        self, correct_sheet
    ):
        correct_sheet.order.total.amount = Decimal('123.45')
        correct_sheet.order.items = [PaymentOrderItem(amount=Decimal('543.21'), label='hi')]

        with pytest.raises(CoreAmountMismatchError) as exc_info:
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

        assert_that(
            exc_info.value,
            has_properties({
                'params': has_entries({
                    'order_total_amount': convert_then_match(Decimal, Decimal('123.45')),
                    'items_total_amount': convert_then_match(Decimal, Decimal('543.21')),
                    'description': 'Order total amount must be equal to the sum of item amounts.',
                })
            })
        )

    @pytest.mark.asyncio
    async def test_validation_failed_because_of_negative_amount(self, correct_sheet):
        correct_sheet.order.total.amount = Decimal('-100.0')
        with pytest.raises(CoreInvalidAmountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_of_invalid_sheet_version(
        self, correct_sheet_pan_only
    ):
        correct_sheet_pan_only.version = 1
        with pytest.raises(CoreInvalidVersionError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet_pan_only,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_merchant_not_found(self, correct_sheet):
        correct_sheet.merchant.id = uuid4()

        with pytest.raises(CoreMerchantNotFoundError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validation_failed_because_of_unknown_psp(self, correct_sheet):
        correct_sheet.payment_methods = correct_sheet.payment_methods[:1]
        correct_sheet.payment_methods[0].gateway = 'go kiss yourself!'
        with pytest.raises(CorePSPNotFoundError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_validates_correct_sheet(self, correct_sheet):
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'payment_method',
        [
            PaymentMethod(method_type=method_type)
            for method_type in PaymentMethodType
            if method_type != PaymentMethodType.CARD
        ],
    )
    async def test_validates_correct_non_card_sheet(self, correct_sheet, payment_method):
        correct_sheet.payment_methods = [payment_method]
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    async def test_validates_sheet_with_all_methods(self, correct_sheet):
        correct_sheet.payment_methods.extend(
            [
                PaymentMethod(method_type=method_type)
                for method_type in PaymentMethodType
                if method_type != PaymentMethodType.CARD
            ],
        )
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('currency', settings.ALLOWED_CURRENCIES)
    async def test_validation_passes_for_all_allowed_currencies(
        self, correct_sheet, currency
    ):
        correct_sheet.currency_code = currency
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('country', settings.ALLOWED_COUNTRIES)
    async def test_validation_passes_for_all_allowed_countries(
        self, correct_sheet, country
    ):
        correct_sheet.country_code = country
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    async def test_merchant_origin_not_found_error_for_none_value(
        self, yandex_pay_settings, correct_sheet
    ):
        """
        Если проверка домена обязательна,
        то передача None интерпретируется как неопознанный домен.
        """
        yandex_pay_settings.MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY = True
        with pytest.raises(CoreMerchantOriginNotFound):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=None,
            ).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('merchant_origin', [
        None,
        'https://someshop.yandex.ru',
        'https://someshop.yandex.ru:443',
    ])
    async def test_merchant_origin_can_be_none_if_validation_is_not_required(
        self, yandex_pay_settings, merchant_origin, correct_sheet
    ):
        """
        Если проверка домена не обязательна,
        то передача None, http схемы, не существующего домена - не проверяется.
        """
        yandex_pay_settings.MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY = False
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=merchant_origin,
        ).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'merchant_origin',
        [None, 'https://someshop.yandex.ru', 'https://someshop.yandex.ru:443']
    )
    async def test_merchant_origin_not_found_error(
        self, yandex_pay_settings, merchant_origin, correct_sheet
    ):
        yandex_pay_settings.MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY = True
        with pytest.raises(CoreMerchantOriginNotFound):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=merchant_origin,
            ).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('merchant_origin', [
        'http://market.yandex.ru',
        'ftp://market.yandex.ru',
        'market.yandex.ru',
        'market.yandex.ru:9000',
    ])
    async def test_merchant_origin_insecure_schema_error(
        self, yandex_pay_settings, merchant_origin, correct_sheet
    ):
        """
        Если передан какой-то ориджин, но схема не HTTPS,
        то будет ошибка схемы в ориджине.
        """
        yandex_pay_settings.MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY = True
        with pytest.raises(CoreInsecureMerchantOriginSchemaError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=merchant_origin,
            ).run()

    @pytest.mark.asyncio
    async def test_should_raise_on_blocked_psp(self, correct_sheet, psp, storage):
        psp.is_blocked = True
        await storage.psp.save(psp)

        with pytest.raises(CorePSPAccountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_should_raise_on_blocked_merchant(self, correct_sheet, merchant, storage):
        merchant.is_blocked = True
        await storage.merchant.save(merchant)

        with pytest.raises(CoreMerchantAccountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_should_raise_on_blocked_merchant_origin(
        self, yandex_pay_settings, correct_sheet, merchant_origin, storage
    ):
        yandex_pay_settings.MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY = True
        merchant_origin.is_blocked = True
        await storage.merchant_origin.save(merchant_origin)

        with pytest.raises(CoreMerchantAccountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.asyncio
    async def test_discount_should_decrease_total(self, correct_sheet):
        correct_sheet.order.total.amount = Decimal('100.00')
        correct_sheet.order.items = [
            PaymentOrderItem(amount=Decimal('-23.45'), label='discount', type=PaymentItemType.DISCOUNT),
            PaymentOrderItem(amount=Decimal('123.45'), label='some item')
        ]
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    async def test_discount_should_be_less_than_item(self, correct_sheet):
        correct_sheet.order.total.amount = Decimal('-10.00')
        correct_sheet.order.items = [
            PaymentOrderItem(amount=Decimal('-20.00'), label='discount', type=PaymentItemType.DISCOUNT),
            PaymentOrderItem(amount=Decimal('10.00'), label='some item')
        ]
        with pytest.raises(CoreInvalidAmountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.parametrize(
        'mit_options',
        (
            pytest.param(None, id='general-case'),
            pytest.param(MITOptions(type=MITOptionsType.RECURRING, optional=True), id='RECURRING+optional'),
        )
    )
    @pytest.mark.asyncio
    async def test_amount_should_be_gt_zero(self, correct_sheet, mit_options):
        correct_sheet.mit_options = mit_options
        correct_sheet.order.total.amount = Decimal('0.00')
        correct_sheet.order.items = [
            PaymentOrderItem(amount=Decimal('-10.00'), label='discount', type=PaymentItemType.DISCOUNT),
            PaymentOrderItem(
                amount=Decimal('10.00'), label='item', quantity=PaymentItemQuantity(count=10, label='шт')
            ),
        ]
        with pytest.raises(CoreInvalidAmountError):
            await ValidatePaymentSheetAction(
                sheet=correct_sheet,
                merchant_origin=MERCHANT_ORIGIN,
            ).run()

    @pytest.mark.parametrize(
        'mit_options',
        (
            pytest.param(MITOptions(type=MITOptionsType.RECURRING, optional=False), id='RECURRING+!optional'),
            pytest.param(MITOptions(type=MITOptionsType.DEFERRED), id='DEFERRED'),
        )
    )
    @pytest.mark.asyncio
    async def test_amount_zero_is_fine_for_some_mit_cases(self, correct_sheet, mit_options):
        correct_sheet.mit_options = mit_options
        correct_sheet.order.total.amount = Decimal('0.00')
        correct_sheet.order.items = [
            PaymentOrderItem(amount=Decimal('0.00'), label='Taxi'),
        ]

        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    async def test_pickup_amount_can_be_zero(self, correct_sheet):
        correct_sheet.order.total.amount = Decimal('100.00')
        correct_sheet.order.items = [
            PaymentOrderItem(amount=Decimal('-23.45'), label='discount', type=PaymentItemType.DISCOUNT),
            PaymentOrderItem(amount=Decimal('123.45'), label='some item'),
            PaymentOrderItem(
                amount=Decimal('0.00'), label='pickup', type=PaymentItemType.PICKUP
            )
        ]
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

    @pytest.mark.asyncio
    async def test_call_logged(self, correct_sheet, dummy_logs, psp):
        await ValidatePaymentSheetAction(
            sheet=correct_sheet,
            merchant_origin=MERCHANT_ORIGIN,
        ).run()

        logs = dummy_logs()
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='Payment sheet validated',
                    levelno=logging.INFO,
                    _context=has_entries(
                        psp=psp,
                        sheet=correct_sheet,
                        validate_origin=True,
                        merchant_origin=MERCHANT_ORIGIN,
                    )
                )
            ),
        )
