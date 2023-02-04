from typing import Final

import hamcrest as hm
import pytest

from billing.library.python.calculator.analytics import CommissionAnalytic as RewardAnalytic
from billing.library.python.calculator.analytics import ContractAnalytic, PaymentAnalytic
from billing.library.python.calculator.exceptions import InvalidConfigError
from billing.library.python.calculator.values import PaymentMethodID

from billing.hot.calculators.trust.calculator.core.accounts import AnalyticBuilder, TransactionToAccountMapper
from billing.hot.calculators.trust.calculator.core.adapters import EventAdapter
from billing.hot.calculators.trust.calculator.core.const import TransactionType
from billing.hot.calculators.trust.calculator.core.models.method import to_payment_method
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from .test_data.payment.generated_data import gen_event_with_one_row_method


SETTINGS = _parse_settings(
    {
        "namespace": "trust",
        "endpoint": "payment",
        "accounts_mapping": [
            [[PaymentMethodID.CARD, "payment"], ["cashless", "ContractAnalytic", "credit"]],
            [[PaymentMethodID.CARD, "refund"], ["cashless_refunds", "ContractAnalytic", "debit"]],
            [[PaymentMethodID.SAMSUNG_PAY, "payment"], ["cashless", "PaymentAnalytic", "credit"]],
            [[PaymentMethodID.SAMSUNG_PAY, "refund"], ["cashless_refunds", "PaymentAnalytic", "debit"]],
            [[PaymentMethodID.YANDEX_ACCOUNT_WITHDRAW, "payment"], ["agent_rewards", "RewardAnalytic", "credit"]],
            [[PaymentMethodID.YANDEX_ACCOUNT_WITHDRAW, "refund"], ["agent_reward_refunds", "RewardAnalytic", "debit"]],
        ],
    }
)

method = to_payment_method(**gen_event_with_one_row_method())
row_adapter = EventAdapter(method.event, SETTINGS, method.references).payments[0]


def test_analytic_builder() -> None:
    builder = AnalyticBuilder()

    contract_analytic = builder.build(row_adapter, "ContractAnalytic")
    hm.assert_that(contract_analytic, hm.instance_of(ContractAnalytic))

    contract_analytic = builder.build(row_adapter, "PaymentAnalytic")
    hm.assert_that(contract_analytic, hm.instance_of(PaymentAnalytic))

    contract_analytic = builder.build(row_adapter, "RewardAnalytic")
    hm.assert_that(contract_analytic, hm.instance_of(RewardAnalytic))


class TestTransactionToAccountMapper:
    instance: Final[TransactionToAccountMapper] = TransactionToAccountMapper(SETTINGS.accounts_mapping)

    @pytest.mark.parametrize(
        "payment_method,transaction_type,result",
        [
            [
                PaymentMethodID.CARD,
                TransactionType.PAYMENT,
                (
                    "cashless",
                    "ContractAnalytic",
                    "credit",
                ),
            ],
            [
                PaymentMethodID.CARD,
                TransactionType.REFUND,
                ("cashless_refunds", "ContractAnalytic", "debit"),
            ],
            [
                PaymentMethodID.SAMSUNG_PAY,
                TransactionType.PAYMENT,
                ("cashless", "PaymentAnalytic", "credit"),
            ],
            [
                PaymentMethodID.YANDEX_ACCOUNT_WITHDRAW,
                TransactionType.REFUND,
                ("agent_reward_refunds", "RewardAnalytic", "debit"),
            ],
        ],
    )
    def test_mapping(
        self, payment_method: PaymentMethodID, transaction_type: TransactionType, result: list[str]
    ) -> None:
        assert self.instance.get_account_info(payment_method, transaction_type) == result

    def test_no_match(self) -> None:
        with pytest.raises(InvalidConfigError):
            self.instance.get_account_info(PaymentMethodID.BANK, TransactionType.PAYMENT)
