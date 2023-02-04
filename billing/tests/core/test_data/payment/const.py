from datetime import datetime, timedelta, timezone

import arrow


def _to_timezone_aware(dt: datetime) -> datetime:
    return dt.replace(tzinfo=timezone.utc)


def to_timestamp(dt: datetime) -> int:
    return arrow.get(dt).int_timestamp


NOW = datetime.now()
NOW_WITH_TZ = _to_timezone_aware(datetime.now())
YESTERDAY = NOW - timedelta(days=1)
YESTERDAY_WITH_TZ = NOW_WITH_TZ - timedelta(days=1)

CLIENT_ID = 1
PERSON_ID = 1
CONTRACT_ID = 1
SERVICE_ID = 1
FIRM_ID = 1
SERVICE_ORDER_ID = "1"
SERVICE_PRODUCT_ID = 1
SERVICE_PRODUCT_EXTERNAL_ID = "1"
CONTRACT_SERVICES_IDS = [SERVICE_ID]
PRODUCT_ID = 1
PRODUCT_EXTERNAL_ID = "1"
TERMINAL_ID = 1
PAYMENT_METHOD = "card-x04f1ec28585ff787eb9cd9ea"
CURRENCY = "RUB"
