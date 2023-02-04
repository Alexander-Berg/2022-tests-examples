from decimal import Decimal

import pytest

from hamcrest import assert_that, equal_to

from pay.lib.utils.currency import amount_from_minor_units, amount_to_minor_units
from pay.lib.utils.exceptions import InvalidAmountError


class TestAmountToMinorUnits:
    @pytest.mark.parametrize(
        'amount,currency,expected_minors',
        [
            (Decimal('10.11'), None, 1011),  # babel defaults to 2 minor units
            (Decimal('10.12'), 'XTS', 1012),
            (Decimal('10.2'), 'RUB', 1020),
            (Decimal('10'), 'BYN', 1000),
            (Decimal('20.00'), 'JPY', 20),  # yen has no minor units
            (Decimal('10.321'), 'BHD', 10321),  # dinar has 3 minor units
            (Decimal('10.4'), 'MRU', 1040),  # weird thing divided into five subunits
        ],
    )
    def test_success(self, amount, currency, expected_minors):
        assert_that(amount_to_minor_units(amount, currency), equal_to(expected_minors))

    @pytest.mark.parametrize(
        'amount,currency',
        [
            (Decimal('10.111'), None),  # babel defaults to 2 minor units
            (Decimal('10.111'), 'XTS'),
            (Decimal('10.111'), 'RUB'),
            (Decimal('10.1'), 'JPY'),  # yen has no minor units
            (Decimal('10.1111'), 'BHD'),  # dinar has 3 minor units
        ],
    )
    def test_invalid_amount(self, amount, currency):
        with pytest.raises(InvalidAmountError):
            amount_to_minor_units(amount, currency)

    def test_min_allowed_check_success(self):
        amount_to_minor_units(Decimal('0.42'), 'XTS', 42)

    def test_min_allowed_check_failed(self):
        with pytest.raises(InvalidAmountError):
            amount_to_minor_units(Decimal('0.42'), 'XTS', 43)


class TestAmountFromMinorUnits:
    @pytest.mark.parametrize(
        'minors,currency,expected_amount',
        [
            (1011, None, Decimal('10.11')),  # babel defaults to 2 minor units
            (1012, 'XTS', Decimal('10.12')),
            (1020, 'RUB', Decimal('10.20')),
            (1000, 'BYN', Decimal('10.00')),
            (20, 'JPY', Decimal('20')),  # yen has no minor units
            (10321, 'BHD', Decimal('10.321')),  # dinar has 3 minor units
            (1040, 'MRU', Decimal('10.4')),  # weird thing divided into five subunits
        ],
    )
    def test_success(self, minors, currency, expected_amount):
        assert_that(amount_from_minor_units(minors, currency), equal_to(expected_amount))
