from typing import Union

import pytest

from billing.library.python.calculator.exceptions import UnknownMethodTypeError

from billing.hot.calculators.bnpl.calculator.core.actions.cashless import (
    ProcessPaymentAction, ProcessRefundAction,
)
from billing.hot.calculators.bnpl.calculator.core.actions.payout import ProcessPayoutAction
from billing.hot.calculators.bnpl.calculator.core.actions.resolver import resolve_action_on_method
from billing.hot.calculators.bnpl.calculator.core.models.method import (
    PaymentMethod, RefundMethod, PayoutMethod,
)


class TestResolveActionOnMethod:
    @pytest.mark.parametrize(
        'method, expected_type', [
            ('payment', ProcessPaymentAction),
            ('refund', ProcessRefundAction),
            ('payout', ProcessPayoutAction),
        ],
        indirect=['method']
    )
    def test_well_known_method_actions(
        self,
        method: Union[PaymentMethod, RefundMethod, PayoutMethod],
        expected_type: type,
    ):
        action = resolve_action_on_method(method)

        assert isinstance(action, expected_type)

    def test_unknown_method_type(self):
        with pytest.raises(UnknownMethodTypeError):
            _ = resolve_action_on_method({})
