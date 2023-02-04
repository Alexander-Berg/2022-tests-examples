from dataclasses import replace
from datetime import datetime, timezone
from decimal import Decimal

import pytest

from hamcrest import assert_that, equal_to

from pay.lib.entities.enums import DeliveryCategory, PaymentMethodType
from pay.lib.entities.exceptions import InvalidYandexDeliveryOptionError
from pay.lib.entities.shipping import ShippingMethod, ShippingMethodType, YandexDeliveryOption


class TestShippingMethod:
    @pytest.mark.parametrize(
        'shipping_method, has_one',
        (
            (ShippingMethod(ShippingMethodType.COURIER, courier_option=object()), True),
            (ShippingMethod(ShippingMethodType.COURIER, pickup_option=object()), True),
            (ShippingMethod(ShippingMethodType.COURIER, yandex_delivery_option=object()), True),
            (
                ShippingMethod(
                    ShippingMethodType.COURIER,
                    yandex_delivery_option=object(),
                    courier_option=object(),
                    pickup_option=object(),
                ),
                False,
            ),
            (
                ShippingMethod(
                    ShippingMethodType.COURIER,
                    yandex_delivery_option=object(),
                    courier_option=object(),
                ),
                False,
            ),
        ),
    )
    def test_has_exactly_one_option(self, shipping_method, has_one):
        assert shipping_method.has_exactly_one_option() == has_one

    @pytest.mark.parametrize(
        'method_type, name',
        [
            (
                method_type,
                {
                    ShippingMethodType.COURIER: 'courier_option',
                    ShippingMethodType.DIRECT: 'courier_option',
                    ShippingMethodType.PICKUP: 'pickup_option',
                    ShippingMethodType.YANDEX_DELIVERY: 'yandex_delivery_option',
                }[method_type],
            )
            for method_type in ShippingMethodType
        ],
    )
    def test_get_option_name(self, method_type, name):
        shipping_method = ShippingMethod(
            method_type=method_type,
        )
        assert_that(shipping_method.get_option_name(), equal_to(name))

    def test_get_option(self, mocker):
        option = mocker.Mock()
        shipping_method = ShippingMethod(
            method_type=ShippingMethodType.COURIER,
            courier_option=option,
        )

        assert_that(shipping_method.get_option(), equal_to(option))


class TestYandexDeliveryOption:
    NOW = datetime.now(tz=timezone.utc)

    def test_validate_valid(self, valid):
        valid.validate()

    @pytest.mark.parametrize(
        'patch',
        (
            ({'from_datetime': NOW, 'to_datetime': NOW, 'category': DeliveryCategory.EXPRESS}),
            ({'from_datetime': None, 'to_datetime': None, 'category': DeliveryCategory.TODAY}),
            ({'category': DeliveryCategory.STANDARD}),
        ),
    )
    def test_validate_invalid(self, valid, patch):
        invalid = replace(valid, **patch)

        with pytest.raises(InvalidYandexDeliveryOptionError):
            invalid.validate()

    @pytest.fixture
    def valid(self):
        return YandexDeliveryOption(
            yandex_delivery_option_id='option-id',
            title='title',
            amount=Decimal('10'),
            category=DeliveryCategory.EXPRESS,
            receipt=None,
            allowed_payment_methods=[PaymentMethodType.CARD_ON_DELIVERY],
        )
