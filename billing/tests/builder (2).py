from datetime import datetime
from decimal import Decimal
from typing import Optional

from billing.library.python.calculator.util import to_msk_dt


def gen_service_merchant(service_fee: int) -> dict:
    return {
        'service_merchant_id': 3823,
        'service_id': 2389,
        'service': {'service_fee': service_fee},
    }


def gen_order(
    price: Decimal,
    commission: int,
    kind: str = 'pay',
    service_merchant_service_fee: int = 1,
    item_markup: Optional[dict] = None,
) -> dict:
    return {
        'uid': 3123,
        'shop_id': 2314,
        'order_id': 7328,
        'parent_order_id': 3214,
        'original_order_id': 823,
        'revision': 73429,
        'acquirer': 'tinkoff',
        'commission': commission,
        'kind': kind,
        'autoclear': True,
        'closed': to_msk_dt(datetime(2020, 11, 5)),
        'created': to_msk_dt(datetime(2020, 11, 3)),
        'updated': to_msk_dt(datetime(2020, 11, 4)),
        'held_at': None,
        'pay_status_updated_at': to_msk_dt(datetime(2020, 11, 4)),
        'caption': 'caption',
        'description': 'description',
        'customer_uid': 142543,
        'price': price,
        'currency': 'RUB',
        'items': [
            {
                'name': 'payments item',
                'trust_order_id': '4021932604.18.1._',
                'product_id': 1,
                'nds': 'nds_20_120',
                'currency': 'RUB',
                'total_price': price,
                'prices': None,
                'amount': 1,
                'payment_method': 'card',
                'markup': item_markup,
            },
        ],
        'service_client_id': 2314,
        'service_merchant': gen_service_merchant(service_merchant_service_fee)
    }
