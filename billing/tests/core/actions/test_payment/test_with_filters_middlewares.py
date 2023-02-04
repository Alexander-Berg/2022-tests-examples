from copy import deepcopy
from decimal import Decimal
from typing import Any, cast

import hamcrest as hm
import pytest

from billing.library.python.calculator.analytics import ContractAnalytic
from billing.library.python.calculator.services.account import AnalyticConfig
from billing.library.python.calculator.test_utils.builder import (
    gen_general_contract,
    gen_trust_order,
    gen_trust_service_product,
)
from billing.library.python.calculator.values import PaymentMethodID

from billing.hot.calculators.trust.calculator.core.configurable_blocks.middlewares.const import MiddlewareType
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from ...test_data.payment.builder import build_transaction, gen_payment, gen_references, gen_trust_payment_row
from ...test_data.payment.const import CLIENT_ID, CONTRACT_ID, CURRENCY, FIRM_ID, SERVICE_ID, YESTERDAY
from ...test_data.payment.generated_data import FIRM, MIGRATION_INFO
from .base import BaseTestPaymentAction


SETTINGS = _parse_settings(
    {
        "namespace": "trust",
        "endpoint": "payment",
        "accounts_mapping": [
            [[PaymentMethodID.CARD, "payment"], ["cashless", "ContractAnalytic", "credit"]],
            [[PaymentMethodID.CARD, "refund"], ["cashless_refunds", "ContractAnalytic", "debit"]],
            [[PaymentMethodID.MOBILE, "payment"], ["cashless", "ContractAnalytic", "credit"]],
            [[PaymentMethodID.MOBILE, "refund"], ["cashless_refunds", "ContractAnalytic", "debit"]],
        ],
        "row_middlewares": [
            {"type": MiddlewareType.FillCommonTsMiddleware, "arguments": {}},
            {"type": MiddlewareType.FillFromRowAdapterMiddleware, "arguments": {"src": "dry_run"}},
            {"type": MiddlewareType.FillFromRowAdapterMiddleware, "arguments": {"src": "service_id"}},
            {
                "type": MiddlewareType.FillFromRowAdapterMiddleware,
                "arguments": {"src": "product_id", "dst": "product_mdh_id"},
            },
            {"type": MiddlewareType.FillTaxPolicyMiddleware, "arguments": {}},
            {"type": MiddlewareType.FillAmountWoVatMiddleware, "arguments": {}},
        ],
    }
)


def gen_product(id_: int) -> dict[str, Any]:
    return gen_trust_service_product(partner_id=CLIENT_ID, id=id_, external_id=str(id_))


def gen_order(id_: int) -> dict[str, Any]:
    return gen_trust_order(service_product_id=id_, service_order_id=str(id_), service_product_external_id=str(id_))


def gen_contract(id_: int, client_id: int = CLIENT_ID) -> dict[str, Any]:
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
        ),
    )


ORDER_1, ORDER_2 = gen_order(1), gen_order(2)
PRODUCT_1, PRODUCT_2 = gen_product(1), gen_product(2)

COMPOSITE_EVENT_WITH_ROWS_WITH_DIFFERENT_SERVICE_FEE_METHOD = {
    "event": gen_payment(
        amount=Decimal(10),
        postauth_amount=Decimal(10),
        payment_method="composite",
        currency="RUB",
        composite_components=[  # rows in this component should be filtered by payment_method
            gen_payment(
                amount=Decimal(5),
                postauth_amount=Decimal(5),
                payment_method_id=PaymentMethodID.YANDEX_ACCOUNT_TOPUP,
                currency="RUB",
                service_id=SERVICE_ID,
                products=[PRODUCT_1],
                rows=[
                    gen_trust_payment_row(id=1, amount=Decimal(10), order=ORDER_1),
                    gen_trust_payment_row(id=2, amount=Decimal(10), order=ORDER_1),
                ],
            ),
            gen_payment(
                amount=Decimal(5),
                postauth_amount=Decimal(5),
                payment_method_id=PaymentMethodID.MOBILE,
                currency="RUB",
                service_id=SERVICE_ID,
                products=[PRODUCT_2],
                rows=[
                    gen_trust_payment_row(id=3, amount=Decimal(10), order=ORDER_2),
                    gen_trust_payment_row(id=4, amount=Decimal(10), order=ORDER_2),
                ],
            ),
        ],
    ),
    "references": gen_references(
        contracts=[gen_contract(id_=1)],
        firms=[FIRM],
        migration_info=[MIGRATION_INFO],
        lock=None,
    ),
}

COMPOSITE_EVENT_WITH_ONE_INACTIVE_ROW = deepcopy(COMPOSITE_EVENT_WITH_ROWS_WITH_DIFFERENT_SERVICE_FEE_METHOD)
COMPOSITE_EVENT_WITH_ONE_INACTIVE_ROW['event']['composite_components'][0]['payment_dt'] = None


CONTRACT_ANALYTIC = ContractAnalytic(CLIENT_ID, CONTRACT_ID, CURRENCY)


def build_transaction_with_info(
    transaction_type: str, analytic: AnalyticConfig, amount: Decimal, operation_type: str
) -> dict[str, Any]:
    tariffer_payload = {
        "common_ts": hm.instance_of(int),
        "firm_id": FIRM_ID,
        "tax_policy_id": hm.instance_of(int),
        "tax_policy_pct": hm.is_not(None),
        "amount_wo_vat": hm.instance_of(Decimal),
        "dry_run": False,
        "service_id": SERVICE_ID,
        "product_mdh_id": "2",
    }

    return build_transaction(
        amount=amount,
        analytic=analytic,
        transaction_type=transaction_type,
        operation_type=operation_type,
        info=hm.has_entries({"tariffer_payload": hm.has_entries(tariffer_payload)}),
    )


class TestPaymentAction(BaseTestPaymentAction):
    SETTINGS = SETTINGS

    @pytest.mark.parametrize(
        "input_method,expected_transactions,diod_keys_number",
        [
            pytest.param(
                deepcopy(COMPOSITE_EVENT_WITH_ROWS_WITH_DIFFERENT_SERVICE_FEE_METHOD),
                [
                    build_transaction_with_info("cashless", CONTRACT_ANALYTIC, Decimal("10"), "credit"),
                    build_transaction_with_info("cashless", CONTRACT_ANALYTIC, Decimal("10"), "credit"),
                ],
                4,
                id='Default filters'
            ),
            pytest.param(
                deepcopy(COMPOSITE_EVENT_WITH_ONE_INACTIVE_ROW),
                [],
                0,
                id='Skip all if one of the rows is inactive'
            ),
        ],
    )
    def test_action(
        self, input_method: dict, expected_transactions: list[dict[str, Any]], diod_keys_number: int
    ) -> None:
        _ = self.base_test_action(input_method, expected_transactions, diod_keys_number)
