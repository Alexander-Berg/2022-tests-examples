from decimal import Decimal
from functools import partial
from typing import Any, Optional

from billing.library.python.calculator.services.account import AnalyticConfig
from billing.library.python.calculator.test_utils.builder import gen_firm, gen_trust_order, gen_trust_payment
from billing.library.python.calculator.test_utils.builder import gen_trust_payment_row as base_gen_trust_payment_row
from billing.library.python.calculator.test_utils.builder import gen_trust_refund, gen_trust_service_product
from billing.library.python.calculator.values import PaymentMethodID

from .const import (
    CLIENT_ID,
    CURRENCY,
    FIRM_ID,
    NOW_WITH_TZ,
    PAYMENT_METHOD,
    PRODUCT_EXTERNAL_ID,
    PRODUCT_ID,
    SERVICE_ID,
    SERVICE_ORDER_ID,
    SERVICE_PRODUCT_EXTERNAL_ID,
    TERMINAL_ID,
    to_timestamp,
)


ORDER = gen_trust_order(
    service_order_id=SERVICE_ORDER_ID, service_id=SERVICE_ID, service_product_external_id=SERVICE_PRODUCT_EXTERNAL_ID
)
PRODUCT = gen_trust_service_product(partner_id=CLIENT_ID, id=PRODUCT_ID, external_id=PRODUCT_EXTERNAL_ID)


gen_trust_payment_row = partial(base_gen_trust_payment_row, order=ORDER)


def gen_payment(*args: Any, **kwargs: Any) -> dict[str, Any]:
    """Set some fields for all methods to same values to have const analytics and make validation checks always pass"""

    kwargs.setdefault("dt", NOW_WITH_TZ)
    kwargs.setdefault("payment_dt", NOW_WITH_TZ)
    kwargs.setdefault("postauth_dt", NOW_WITH_TZ)
    kwargs.setdefault("service_id", SERVICE_ID)
    kwargs.setdefault("products", [PRODUCT])
    kwargs.setdefault("payment_method", PAYMENT_METHOD)
    kwargs.setdefault("payment_method_id", PaymentMethodID.CARD)
    kwargs.setdefault("terminal_id", TERMINAL_ID)
    kwargs.setdefault("terminal_id", CURRENCY)

    return gen_trust_payment(*args, **kwargs)


def gen_refund(*args: Any, **kwargs: Any) -> dict[str, Any]:
    kwargs.setdefault("dt", NOW_WITH_TZ)
    kwargs.setdefault("payment_dt", NOW_WITH_TZ)

    return gen_trust_refund(*args, **kwargs)


def gen_references(
    contracts: Optional[list[dict]] = None,
    migration_info: Optional[list[dict]] = None,
    lock: Optional[dict] = None,
    firms: Optional[list[dict]] = None,
    diod_keys: Optional[list[dict]] = None,
    currency_conversion: Optional[dict] = None,
) -> dict[str, Any]:
    return {
        "contracts": contracts or [],
        "migration_info": migration_info,
        "lock": lock,
        "firms": firms or [gen_firm(FIRM_ID, "mdh_id")],
        "diod_keys": diod_keys or [],
        "currency_conversion": currency_conversion,
    }


def build_transaction(
    transaction_type: str,
    analytic: AnalyticConfig,
    amount: Decimal,
    operation_type: str,
    dt: int = to_timestamp(NOW_WITH_TZ),
    info: Any = None,
) -> dict[str, Any]:
    return {
        "loc": {
            "namespace": "trust",
            "type": transaction_type,
            **analytic.serialize(),
        },
        "amount": amount,
        "type": operation_type,
        "dt": dt,
        "info": info or {"tariffer_payload": {}},
    }
