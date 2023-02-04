from copy import deepcopy
from decimal import Decimal
from typing import Any, cast

import pytest

from billing.library.python.calculator.analytics import ContractAnalytic, PaymentAnalytic
from billing.library.python.calculator.test_utils.builder import (
    gen_general_contract,
    gen_trust_order,
    gen_trust_service_product,
)
from billing.library.python.calculator.values import PaymentMethodID

from billing.hot.calculators.trust.calculator.core.configurable_blocks.contract_filters.const import ContractFilterType
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from ...test_data.payment.builder import build_transaction, gen_payment, gen_references, gen_trust_payment_row
from ...test_data.payment.const import CLIENT_ID, CONTRACT_ID, CURRENCY, FIRM_ID, SERVICE_ID, TERMINAL_ID, YESTERDAY
from ...test_data.payment.generated_data import FIRM, MIGRATION_INFO
from .base import BaseTestPaymentAction


SETTINGS = _parse_settings(
    {
        "namespace": "trust",
        "endpoint": "payment",
        "accounts_mapping": [
            [[PaymentMethodID.CARD, "payment"], ["cashless", "PaymentAnalytic", "credit"]],
            [[PaymentMethodID.CARD, "refund"], ["cashless_refunds", "PaymentAnalytic", "debit"]],
            [[PaymentMethodID.MOBILE, "payment"], ["cashless", "ContractAnalytic", "debit"]],
            [[PaymentMethodID.MOBILE, "refund"], ["cashless_refunds", "ContractAnalytic", "credit"]],
        ],
        "contract_filters": [
            {
                "type": ContractFilterType.CommissionContractFilter,
                "arguments": {"nds_to_contract_type": {"nds_20": 0, "nds_none": 72}},
            }
        ],
    }
)


def gen_product(id_: int, client_id: int) -> dict[str, Any]:
    return gen_trust_service_product(id=id_, partner_id=client_id, external_id=str(id_))


def gen_order(id_: int) -> dict[str, Any]:
    return gen_trust_order(service_product_id=id_, service_order_id=str(id_), service_product_external_id=str(id_))


def gen_contract(id_: int, client_id: int, commission: int) -> dict[str, Any]:
    return cast(
        dict[str, Any],
        gen_general_contract(
            id_,
            client_id=client_id,
            person_id=client_id,
            services=[SERVICE_ID],
            firm=FIRM_ID,
            dt=YESTERDAY,
            currency=CURRENCY,
            commission=commission,
        ),
    )


def build_payment_analytic(contract_id: int) -> PaymentAnalytic:
    return PaymentAnalytic(CLIENT_ID, contract_id, CURRENCY, TERMINAL_ID)


CONTRACT_ANALYTIC = ContractAnalytic(CLIENT_ID, CONTRACT_ID, CURRENCY)

EVENT_WITH_ROWS_WITH_DIFFERENT_FISCAL_NDS_METHOD = {
    "event": gen_payment(
        amount=Decimal(20),
        postauth_amount=Decimal(20),
        service_id=SERVICE_ID,
        currency="RUB",
        payment_method_id=PaymentMethodID.CARD,
        products=[gen_product(1, CLIENT_ID), gen_product(2, CLIENT_ID)],
        rows=[
            gen_trust_payment_row(id=1, amount=Decimal(10), order=gen_order(id_=1), fiscal_nds="nds_20"),
            gen_trust_payment_row(id=2, amount=Decimal(10), order=gen_order(id_=2), fiscal_nds="nds_none"),
        ],
    ),
    "references": gen_references(
        contracts=[gen_contract(1, CLIENT_ID, commission=0), gen_contract(2, CLIENT_ID, commission=72)],
        firms=[FIRM],
        migration_info=[MIGRATION_INFO],
        lock=None,
    ),
}


class TestPaymentAction(BaseTestPaymentAction):
    SETTINGS = SETTINGS

    @pytest.mark.parametrize(
        "input_method,expected_transactions",
        [
            (
                deepcopy(EVENT_WITH_ROWS_WITH_DIFFERENT_FISCAL_NDS_METHOD),
                [
                    build_transaction("cashless", build_payment_analytic(1), Decimal("10"), "credit"),
                    build_transaction("cashless", build_payment_analytic(2), Decimal("10"), "credit"),
                ],
            ),
        ],
    )
    def test_action(self, input_method: dict, expected_transactions: list[dict[str, Any]]) -> None:
        _ = self.base_test_action(input_method, expected_transactions, len(expected_transactions))
