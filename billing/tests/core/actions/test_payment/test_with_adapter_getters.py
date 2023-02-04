from copy import deepcopy
from decimal import Decimal
from typing import Any, cast

import pytest

from billing.library.python.calculator.analytics import CommissionAnalytic as RewardAnalytic
from billing.library.python.calculator.test_utils.builder import (
    gen_general_contract,
    gen_trust_order,
    gen_trust_service_product,
)
from billing.library.python.calculator.values import PaymentMethodID

from billing.hot.calculators.trust.calculator.core.configurable_blocks.adapters import AdapterType
from billing.hot.calculators.trust.calculator.core.configurable_blocks.getters import GetterType
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from ...test_data.payment.builder import build_transaction, gen_payment, gen_references, gen_trust_payment_row
from ...test_data.payment.const import CONTRACT_ID, CURRENCY, FIRM_ID, SERVICE_ID, YESTERDAY
from ...test_data.payment.generated_data import FIRM, MIGRATION_INFO
from .base import BaseTestPaymentAction


CLIENT_ID = 33043275

SETTINGS = _parse_settings(
    {
        "namespace": "trust",
        "endpoint": "payment",
        "accounts_mapping": [
            [[PaymentMethodID.CARD, "payment"], ["cashless", "RewardAnalytic", "credit"]],
            [[PaymentMethodID.CARD, "refund"], ["cashless_refunds", "RewardAnalytic", "debit"]],
            [[PaymentMethodID.MOBILE, "payment"], ["cashless", "RewardAnalytic", "credit"]],
            [[PaymentMethodID.MOBILE, "refund"], ["cashless_refunds", "RewardAnalytic", "debit"]],
        ],
        "adapter_getters": {
            "type": AdapterType.DefaultAdapter,
            "arguments": {},
            "getters": {
                "__service_fee_1": {"type": GetterType.ConstGetter, "arguments": {"const": 1}},
                "__service_fee_1_product": {
                    "type": GetterType.ConstGetter,
                    "arguments": {"const": "612e0f9c-a3d7-4d02-be0b-a31a57242da4"},
                },
                "__service_fee_2": {"type": GetterType.ConstGetter, "arguments": {"const": 2}},
                "__service_fee_2_product": {
                    "type": GetterType.ConstGetter,
                    "arguments": {"const": "85a6597d-2bca-4da4-9b46-f6c23c4638cd"},
                },
                "__service_fee_4": {"type": GetterType.ConstGetter, "arguments": {"const": 4}},
                "__service_fee_4_product": {
                    "type": GetterType.ConstGetter,
                    "arguments": {"const": "1f102314-0817-4389-b6dd-1241326c4f79"},
                },
                "__service_fee_default_product": {
                    "type": GetterType.ConstGetter,
                    "arguments": {"const": "4bc007c3-11f1-4531-a1cb-09d0ec90468f"},
                },
                "__actual_service_fee": {
                    "type": GetterType.FieldGetter,
                    "arguments": {"jsonpath": "$.row.service_product.service_fee"},
                },
                "client_id": {"type": GetterType.ConstGetter, "arguments": {"const": CLIENT_ID}},
                "product_id": {
                    "type": GetterType.CaseGetter,
                    "arguments": {
                        "default": "__service_fee_default_product",
                        "cases": [
                            {
                                "left": "__actual_service_fee",
                                "operator": "eq",
                                "right": "__service_fee_1",
                                "value": "__service_fee_1_product",
                            },
                            {
                                "left": "__actual_service_fee",
                                "operator": "eq",
                                "right": "__service_fee_2",
                                "value": "__service_fee_2_product",
                            },
                            {
                                "left": "__actual_service_fee",
                                "operator": "eq",
                                "right": "__service_fee_4",
                                "value": "__service_fee_4_product",
                            },
                        ],
                    },
                },
            },
        },
    }
)


def gen_product(id_: int, service_fee: int, client_id: int = CLIENT_ID) -> dict[str, Any]:
    return gen_trust_service_product(partner_id=client_id, id=id_, external_id=str(id_), service_fee=service_fee)


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


def build_reward_analytic(product_eid: str) -> RewardAnalytic:
    return RewardAnalytic(CLIENT_ID, CONTRACT_ID, CURRENCY, product_eid)


EVENT_WITH_ROWS_WITH_DIFFERENT_SERVICE_FEE_METHOD = {
    "event": gen_payment(
        amount=Decimal(20),
        payment_method="composite",
        currency="RUB",
        composite_components=[
            gen_payment(
                amount=Decimal(10),
                postauth_amount=Decimal(10),
                payment_method_id=PaymentMethodID.CARD,
                currency="RUB",
                service_id=SERVICE_ID,
                products=[gen_product(id_=1, service_fee=1), gen_product(id_=2, service_fee=2)],
                rows=[
                    gen_trust_payment_row(id=1, amount=Decimal(10), order=gen_order(id_=1)),
                    gen_trust_payment_row(id=2, amount=Decimal(10), order=gen_order(id_=2)),
                ],
            ),
            gen_payment(
                amount=Decimal(10),
                postauth_amount=Decimal(10),
                payment_method_id=PaymentMethodID.MOBILE,
                currency="RUB",
                service_id=SERVICE_ID,
                products=[
                    gen_product(id_=3, service_fee=4, client_id=999),
                    gen_product(id_=4, service_fee=999, client_id=999),
                ],
                rows=[
                    gen_trust_payment_row(id=3, amount=Decimal(10), order=gen_order(id_=3)),
                    gen_trust_payment_row(id=4, amount=Decimal(10), order=gen_order(id_=4)),
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


class TestPaymentAction(BaseTestPaymentAction):
    SETTINGS = SETTINGS

    @pytest.mark.parametrize(
        "input_method,expected_transactions",
        [
            (
                deepcopy(EVENT_WITH_ROWS_WITH_DIFFERENT_SERVICE_FEE_METHOD),
                [
                    build_transaction(
                        "cashless",
                        build_reward_analytic("612e0f9c-a3d7-4d02-be0b-a31a57242da4"),
                        Decimal("10"),
                        "credit",
                    ),
                    build_transaction(
                        "cashless",
                        build_reward_analytic("85a6597d-2bca-4da4-9b46-f6c23c4638cd"),
                        Decimal("10"),
                        "credit",
                    ),
                    build_transaction(
                        "cashless",
                        build_reward_analytic("1f102314-0817-4389-b6dd-1241326c4f79"),
                        Decimal("10"),
                        "credit",
                    ),
                    build_transaction(
                        "cashless",
                        build_reward_analytic("4bc007c3-11f1-4531-a1cb-09d0ec90468f"),
                        Decimal("10"),
                        "credit",
                    ),
                ],
            ),
        ],
    )
    def test_action(self, input_method: dict, expected_transactions: list[dict[str, Any]]) -> None:
        _ = self.base_test_action(input_method, expected_transactions, len(expected_transactions))
