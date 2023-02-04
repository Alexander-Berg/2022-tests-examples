from decimal import Decimal
from typing import Any, Optional

import pytest

from billing.library.python.calculator.analytics import CommissionAnalytic as RewardAnalytic
from billing.library.python.calculator.analytics import PaymentAnalytic
from billing.library.python.calculator.values import PaymentMethodID

from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from ...test_data.manifests import gen_const_aw_calculator
from ...test_data.payment.builder import build_transaction
from ...test_data.payment.const import (
    CLIENT_ID,
    CONTRACT_ID,
    NOW_WITH_TZ,
    SERVICE_PRODUCT_EXTERNAL_ID,
    TERMINAL_ID,
    to_timestamp,
)
from ...test_data.payment.generated_data import (
    gen_event_with_composite_method,
    gen_event_with_composite_with_refunds_method,
    gen_event_with_composite_with_reversal_refund_method,
    gen_event_with_currency_conversion,
    gen_event_with_diod_keys,
    gen_event_with_full_reversal_refund_method,
    gen_event_with_multiple_refunds_method,
    gen_event_with_multiple_rows_method,
    gen_event_with_partial_reversal_refund_method,
    gen_event_with_refund_with_multiple_rows_method,
    gen_event_with_rows_and_refunds_method,
)
from .base import BaseTestPaymentAction


SETTINGS = _parse_settings(
    {
        "namespace": "trust",
        "endpoint": "payment",
        "currency_conversion": {"USD": "RUB"},  # configurable by form of the event
        "accounts_mapping": [
            [[PaymentMethodID.CARD, "payment"], ["cashless", "PaymentAnalytic", "credit"]],
            [[PaymentMethodID.CARD, "refund"], ["cashless_refunds", "PaymentAnalytic", "debit"]],
            [[PaymentMethodID.AGENT_REWARDS, "payment"], ["agent_rewards", "RewardAnalytic", "debit"]],
            [[PaymentMethodID.AGENT_REWARDS, "refund"], ["agent_reward_refunds", "RewardAnalytic", "credit"]],
        ],
        "aw_calculator": gen_const_aw_calculator(),
    }
)

REWARD_AMOUNT = "10.00"
NOW_TS = to_timestamp(NOW_WITH_TZ)

PAYMENT_ANALYTIC = PaymentAnalytic(
    client_id=CLIENT_ID, contract_id=CONTRACT_ID, currency="RUB", terminal_id=TERMINAL_ID
)


def build_reward_analytic(product: Optional[str] = SERVICE_PRODUCT_EXTERNAL_ID) -> RewardAnalytic:
    return RewardAnalytic(client_id=CLIENT_ID, contract_id=CONTRACT_ID, currency="RUB", product=product)


class TestPaymentAction(BaseTestPaymentAction):
    SETTINGS = SETTINGS

    @pytest.mark.parametrize(
        "input_method,expected_transactions,diod_keys_number",
        [
            (
                gen_event_with_multiple_rows_method(),
                [
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("20.1"), "credit"),
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal(50.0), "credit"),
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("30.5"), "credit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                ],
                3,
            ),
            (
                gen_event_with_rows_and_refunds_method(),
                [
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("30.1"), "credit"),
                    build_transaction("cashless_refunds", PAYMENT_ANALYTIC, Decimal("25.2"), "debit", NOW_TS),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                    build_transaction(
                        "agent_reward_refunds", build_reward_analytic(), Decimal(REWARD_AMOUNT), "credit", NOW_TS
                    ),
                ],
                2,
            ),
            (
                gen_event_with_refund_with_multiple_rows_method(),
                [
                    build_transaction("cashless_refunds", PAYMENT_ANALYTIC, Decimal("25.2"), "debit", NOW_TS),
                    build_transaction("cashless_refunds", PAYMENT_ANALYTIC, Decimal("25.4"), "debit", NOW_TS),
                    build_transaction(
                        "agent_reward_refunds", build_reward_analytic(), Decimal(REWARD_AMOUNT), "credit", NOW_TS
                    ),
                    build_transaction(
                        "agent_reward_refunds", build_reward_analytic(), Decimal(REWARD_AMOUNT), "credit", NOW_TS
                    ),
                ],
                2,
            ),
            (
                gen_event_with_multiple_refunds_method(),
                [
                    build_transaction("cashless_refunds", PAYMENT_ANALYTIC, Decimal("25.2"), "debit", NOW_TS),
                    build_transaction("cashless_refunds", PAYMENT_ANALYTIC, Decimal("25.4"), "debit", NOW_TS),
                    build_transaction("cashless_refunds", PAYMENT_ANALYTIC, Decimal(20.0), "debit", NOW_TS),
                    build_transaction(
                        "agent_reward_refunds", build_reward_analytic(), Decimal(REWARD_AMOUNT), "credit", NOW_TS
                    ),
                    build_transaction(
                        "agent_reward_refunds", build_reward_analytic(), Decimal(REWARD_AMOUNT), "credit", NOW_TS
                    ),
                    build_transaction(
                        "agent_reward_refunds", build_reward_analytic(), Decimal(REWARD_AMOUNT), "credit", NOW_TS
                    ),
                ],
                3,
            ),
            (
                gen_event_with_partial_reversal_refund_method(),
                [
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("4.8"), "credit"),
                    build_transaction(
                        "agent_rewards",
                        build_reward_analytic(SERVICE_PRODUCT_EXTERNAL_ID),
                        Decimal(REWARD_AMOUNT),
                        "debit",
                    ),
                ],
                1,
            ),
            (gen_event_with_full_reversal_refund_method(), [], 0),
            (
                gen_event_with_composite_method(),
                [
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("20.1"), "credit"),
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal(50.0), "credit"),
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("30.5"), "credit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                ],
                3,
            ),
            (
                gen_event_with_composite_with_refunds_method(),
                [
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("20.1"), "credit"),
                    build_transaction("cashless_refunds", PAYMENT_ANALYTIC, Decimal(20.0), "debit", NOW_TS),
                    build_transaction("cashless_refunds", PAYMENT_ANALYTIC, Decimal("19.1"), "debit", NOW_TS),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                    build_transaction(
                        "agent_reward_refunds", build_reward_analytic(), Decimal(REWARD_AMOUNT), "credit", NOW_TS
                    ),
                    build_transaction(
                        "agent_reward_refunds", build_reward_analytic(), Decimal(REWARD_AMOUNT), "credit", NOW_TS
                    ),
                ],
                3,
            ),
            (
                gen_event_with_composite_with_reversal_refund_method(),
                [
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("50.6"), "credit"),
                    build_transaction(
                        "agent_rewards",
                        build_reward_analytic(SERVICE_PRODUCT_EXTERNAL_ID),
                        Decimal(REWARD_AMOUNT),
                        "debit",
                    ),
                ],
                1,
            ),
            (
                gen_event_with_diod_keys(),
                [
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("50"), "credit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                ],
                3,
            ),
            (
                gen_event_with_currency_conversion(),
                [
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("50"), "credit"),
                    build_transaction("cashless", PAYMENT_ANALYTIC, Decimal("75"), "credit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                    build_transaction("agent_rewards", build_reward_analytic(), Decimal(REWARD_AMOUNT), "debit"),
                ],
                2,
            ),
        ],
    )
    def test_action(
        self, input_method: dict, expected_transactions: list[dict[str, Any]], diod_keys_number: int
    ) -> None:
        _ = self.base_test_action(input_method, expected_transactions, diod_keys_number)
