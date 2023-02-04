from datetime import datetime
from decimal import Decimal

from billing.library.python.calculator.util import to_msk_dt


def gen_service_merchant() -> dict:
    return {
        'service_merchant_id': 3823,
        'service_id': 2389,
    }


def gen_event(
    price: Decimal,
    aquiring_commission: Decimal,
    service_commission: Decimal
) -> dict:
    return {
        "aquiring_commission": aquiring_commission,
        "cancel_reason": None,
        "service_commission": service_commission,
        "transaction_amount": price,
        "transaction_dt": to_msk_dt(datetime(2020, 11, 4)),
        "transaction_id": "03b7452a-5969-4249-478c-dbcae3606459-approved",
        "transaction_type": "payment",
        "billing_client_id": 441424,
        "billing_contract_id": 3245907,
        "product_id": "loan_commission",
        "currency": "RUB"
    }
