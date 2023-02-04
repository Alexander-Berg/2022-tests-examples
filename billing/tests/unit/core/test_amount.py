from decimal import Decimal

import pytest
from pay.lib.utils.exceptions import InvalidAmountError

import billing.yandex_pay.yandex_pay.core.amount
from billing.yandex_pay.yandex_pay.core.amount import denormalize_amount, normalize_amount
from billing.yandex_pay.yandex_pay.core.exceptions import CoreInvalidAmountError


class TestNormalizeAmount:
    def test_calls_amount_to_minor_units(self, mocker):
        mock = mocker.patch.object(
            billing.yandex_pay.yandex_pay.core.amount,
            'amount_to_minor_units',
            mocker.Mock(),
        )

        normalize_amount(Decimal('1.00'), 'XTS', min_allowed=1)

        mock.assert_called_once_with(amount=Decimal('1.00'), currency='XTS', min_allowed=1)

    def test_invalid_amount(self, mocker):
        mocker.patch.object(
            billing.yandex_pay.yandex_pay.core.amount,
            'amount_to_minor_units',
            mocker.Mock(side_effect=InvalidAmountError),
        )

        with pytest.raises(CoreInvalidAmountError):
            normalize_amount(Decimal('1'), 'XTS')


def test_denormalize_amount_calls_amount_from_minor_units(mocker):
    mock = mocker.patch.object(
        billing.yandex_pay.yandex_pay.core.amount,
        'amount_from_minor_units',
        mocker.Mock(),
    )

    denormalize_amount(amount=100, currency='XTS')

    mock.assert_called_once_with(minor_units=100, currency='XTS')
