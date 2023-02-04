from dataclasses import asdict
from typing import Any, ClassVar

from hamcrest import assert_that, contains_inanyorder, equal_to, has_entries

from billing.library.python.calculator.models.transaction import TransactionModel

from billing.hot.calculators.trust.calculator.core.actions.payment import PaymentAction
from billing.hot.calculators.trust.calculator.core.models.method import to_payment_method


class BaseTestPaymentAction:
    SETTINGS: ClassVar

    def base_test_action(
        self, input_method: dict, expected_transactions: list[dict[str, Any]], diod_keys_number: int
    ) -> list[TransactionModel]:

        payment_method = to_payment_method(**input_method)
        action = PaymentAction(settings=self.SETTINGS)

        result = action.run(payment_method)

        result_transactions = []
        for batch in result.client_transactions:
            result_transactions.extend(batch.transactions)

        assert_that(result.event, equal_to(payment_method.event))
        assert_that(  # type: ignore
            [asdict(transaction) for transaction in result_transactions],
            contains_inanyorder(*[has_entries(expected) for expected in expected_transactions]),
        )
        assert len(result.params.keys) == diod_keys_number

        return result_transactions
