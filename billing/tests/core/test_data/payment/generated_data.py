from copy import deepcopy
from decimal import Decimal
from typing import Any

from billing.library.python.calculator.test_utils.builder import (
    gen_diod_key,
    gen_firm,
    gen_general_contract,
    gen_migration_info,
)

from .builder import ORDER, gen_payment, gen_references, gen_refund, gen_trust_payment_row
from .const import (
    CLIENT_ID,
    CONTRACT_ID,
    CONTRACT_SERVICES_IDS,
    CURRENCY,
    FIRM_ID,
    PERSON_ID,
    YESTERDAY,
    YESTERDAY_WITH_TZ,
)


GENERAL_CONTRACT = gen_general_contract(
    CONTRACT_ID, CLIENT_ID, PERSON_ID, CONTRACT_SERVICES_IDS, firm=FIRM_ID, dt=YESTERDAY, currency=CURRENCY
)
GENERAL_CONTRACT_FOR_USD = gen_general_contract(
    CONTRACT_ID, CLIENT_ID, PERSON_ID, CONTRACT_SERVICES_IDS, firm=FIRM_ID, dt=YESTERDAY, currency="USD"
)
GENERAL_CONTRACT_WITH_REWARD_PERCENT = deepcopy(GENERAL_CONTRACT)
GENERAL_CONTRACT_WITH_REWARD_PERCENT["collaterals"]["0"]["partner_commission_pct2"] = "10"

MIGRATION_INFO = gen_migration_info(object_id=FIRM_ID, filter="Firm", from_dt=YESTERDAY_WITH_TZ, dry_run=False)
FIRM = gen_firm(FIRM_ID, "firm_mdh_id")

REFERENCES = gen_references(contracts=[GENERAL_CONTRACT], migration_info=[MIGRATION_INFO], firms=[FIRM])
REFERENCES_WITH_REWARD_PERCENT = gen_references(
    contracts=[GENERAL_CONTRACT_WITH_REWARD_PERCENT], migration_info=[MIGRATION_INFO], firms=[FIRM]
)
REFERENCES_WITH_DIOD_KEYS = gen_references(
    contracts=[GENERAL_CONTRACT],
    migration_info=[MIGRATION_INFO],
    firms=[FIRM],
    diod_keys=[gen_diod_key("1", "trust_calculator"), gen_diod_key("2", "trust_calculator")],
)
REFERENCES_WITH_CURRENCY_CONVERSION = gen_references(
    contracts=[GENERAL_CONTRACT],
    migration_info=[MIGRATION_INFO],
    firms=[FIRM],
    currency_conversion={"to": "RUB", "rate": 50},
)

EVENT_WITH_ONE_ROW_METHOD = {
    "event": gen_payment(
        amount=Decimal("100.6"),
        postauth_amount=Decimal("100.6"),
        rows=[
            gen_trust_payment_row(id=1, amount=Decimal("100.6")),
        ],
    ),
    "references": REFERENCES,
}


EVENT_WITH_REFUND_WITH_ONE_ROW_METHOD = {
    "event": gen_payment(
        amount=0, rows=[], refunds=[gen_refund(amount=0, rows=[gen_trust_payment_row(id=1, amount=Decimal("30.1"))])]
    ),
    "references": REFERENCES,
}


EVENT_WITH_MULTIPLE_ROWS_METHOD = {
    "event": gen_payment(
        amount=Decimal("100.6"),
        postauth_amount=Decimal("100.6"),
        rows=[
            gen_trust_payment_row(id=1, amount=Decimal("20.1")),
            gen_trust_payment_row(id=2, amount=Decimal(50.0)),
            gen_trust_payment_row(id=3, amount=Decimal("30.5")),
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_ROWS_AND_REFUNDS_METHOD = {
    "event": gen_payment(
        amount=Decimal("30.1"),
        postauth_amount=Decimal("30.1"),
        rows=[
            gen_trust_payment_row(id=1, amount=Decimal("30.1")),
        ],
        refunds=[
            gen_refund(
                amount=Decimal("25.2"),
                rows=[
                    gen_trust_payment_row(id=2, amount=Decimal("25.2")),
                ],
            ),
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_REFUND_WITH_MULTIPLE_ROWS_METHOD = {
    "event": gen_payment(
        amount=Decimal(50.6),
        postauth_amount=Decimal(50.6),
        refunds=[
            gen_refund(
                amount=Decimal("50.6"),
                rows=[
                    gen_trust_payment_row(id=1, amount=Decimal("25.2")),
                    gen_trust_payment_row(id=2, amount=Decimal("25.4")),
                ],
            ),
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_MULTIPLE_REFUNDS_METHOD = {
    "event": gen_payment(
        amount=Decimal("50.6"),
        postauth_amount=Decimal("50.6"),
        refunds=[
            gen_refund(
                amount=Decimal("50.6"),
                rows=[
                    gen_trust_payment_row(id=1, amount=Decimal("25.2")),
                    gen_trust_payment_row(id=2, amount=Decimal("25.4")),
                ],
            ),
            gen_refund(
                amount=Decimal(20.0),
                rows=[
                    gen_trust_payment_row(id=3, amount=Decimal(20.0)),
                ],
            ),
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_PARTIAL_REVERSAL_REFUND_METHOD = {
    "event": gen_payment(
        amount=Decimal(30.0),
        postauth_amount=Decimal(30.0),
        rows=[
            gen_trust_payment_row(id=1, amount=Decimal(30.0), order=ORDER),
        ],
        refunds=[
            gen_refund(
                amount=Decimal("25.2"),
                is_reversal=1,
                rows=[
                    gen_trust_payment_row(id=2, amount=Decimal("25.2"), order=ORDER),
                ],
            ),
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_FULL_REVERSAL_REFUND_METHOD = {
    "event": gen_payment(
        amount=Decimal("100.1"),
        rows=[gen_trust_payment_row(id=1, amount=Decimal("100.1"), order=ORDER)],
        refunds=[
            gen_refund(
                amount=Decimal("100.1"),
                is_reversal=1,
                rows=[
                    gen_trust_payment_row(id=2, amount=Decimal("100.1"), order=ORDER),
                ],
            ),
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_COMPOSITE_METHOD = {
    "event": gen_payment(
        payment_method="composite",
        amount=Decimal("100.6"),
        postauth_amount=Decimal("100.6"),
        composite_components=[
            gen_payment(
                amount=Decimal("100.6"),
                postauth_amount=Decimal("100.6"),
                rows=[
                    gen_trust_payment_row(id=1, amount=Decimal("20.1")),
                    gen_trust_payment_row(id=2, amount=Decimal("50.0")),
                    gen_trust_payment_row(id=3, amount=Decimal("30.5")),
                ],
            )
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_COMPOSITE_WITH_REFUNDS_METHOD = {
    "event": gen_payment(
        payment_method="composite",
        amount=Decimal("20.1"),
        postauth_amount=Decimal("20.1"),
        composite_components=[
            gen_payment(
                amount=Decimal("20.1"),
                postauth_amount=Decimal("20.1"),
                rows=[
                    gen_trust_payment_row(id=1, amount=Decimal("20.1")),
                ],
            ),
            gen_payment(
                amount=Decimal(39.1),
                postauth_amount=Decimal(39.1),
                refunds=[
                    gen_refund(
                        amount=Decimal("39.1"),
                        rows=[
                            gen_trust_payment_row(id=2, amount=Decimal(20.0)),
                            gen_trust_payment_row(id=3, amount=Decimal("19.1")),
                        ],
                    ),
                ],
            ),
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_COMPOSITE_WITH_REVERSAL_REFUND_METHOD = {
    "event": gen_payment(
        payment_method="composite",
        amount=Decimal("100.6"),
        postauth_amount=Decimal("100.6"),
        composite_components=[
            gen_payment(
                amount=Decimal("100.6"),
                postauth_amount=Decimal("100.6"),
                rows=[
                    gen_trust_payment_row(id=1, amount=Decimal("100.6"), order=ORDER),
                ],
                refunds=[
                    gen_refund(
                        amount=Decimal(50.0),
                        is_reversal=1,
                        rows=[
                            gen_trust_payment_row(id=2, amount=Decimal(50.0), order=ORDER),
                        ],
                    )
                ],
            )
        ],
    ),
    "references": REFERENCES,
}

EVENT_WITH_CONTRACT_WITH_REWARD_PERCENT = {
    "event": gen_payment(
        amount=Decimal("12.5"),
        postauth_amount=Decimal("12.5"),
        rows=[
            gen_trust_payment_row(id=1, amount=Decimal("7.5")),
            gen_trust_payment_row(id=2, amount=Decimal("50")),
        ],
    ),
    "references": REFERENCES_WITH_REWARD_PERCENT,
}

EVENT_WITH_DIOD_KEYS = {
    "event": gen_payment(
        amount=Decimal("100"),
        postauth_amount=Decimal("100"),
        rows=[
            gen_trust_payment_row(id=1, amount=Decimal("20")),
            gen_trust_payment_row(id=2, amount=Decimal("30")),
            gen_trust_payment_row(id=3, amount=Decimal("50")),
        ],
    ),
    "references": REFERENCES_WITH_DIOD_KEYS,
}

EVENT_WITH_CURRENCY_CONVERSION = {
    "event": gen_payment(
        amount=Decimal("2.5"),
        postauth_amount=Decimal("2.5"),
        currency="USD",
        rows=[
            gen_trust_payment_row(id=1, amount=Decimal("1")),
            gen_trust_payment_row(id=2, amount=Decimal("1.5")),
        ],
    ),
    "references": REFERENCES_WITH_CURRENCY_CONVERSION,
}


def gen_event_with_one_row_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_ONE_ROW_METHOD)


def gen_event_with_refund_with_one_row_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_REFUND_WITH_ONE_ROW_METHOD)


def gen_event_with_multiple_rows_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_MULTIPLE_ROWS_METHOD)


def gen_event_with_rows_and_refunds_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_ROWS_AND_REFUNDS_METHOD)


def gen_event_with_refund_with_multiple_rows_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_REFUND_WITH_MULTIPLE_ROWS_METHOD)


def gen_event_with_multiple_refunds_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_MULTIPLE_REFUNDS_METHOD)


def gen_event_with_partial_reversal_refund_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_PARTIAL_REVERSAL_REFUND_METHOD)


def gen_event_with_full_reversal_refund_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_FULL_REVERSAL_REFUND_METHOD)


def gen_event_with_composite_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_COMPOSITE_METHOD)


def gen_event_with_composite_with_refunds_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_COMPOSITE_WITH_REFUNDS_METHOD)


def gen_event_with_composite_with_reversal_refund_method() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_COMPOSITE_WITH_REVERSAL_REFUND_METHOD)


def gen_event_with_contract_with_reward_percent() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_CONTRACT_WITH_REWARD_PERCENT)


def gen_event_with_diod_keys() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_DIOD_KEYS)


def gen_event_with_currency_conversion() -> dict[str, Any]:
    return deepcopy(EVENT_WITH_CURRENCY_CONVERSION)
