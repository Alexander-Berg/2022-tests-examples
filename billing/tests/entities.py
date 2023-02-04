from datetime import timedelta
from enum import Enum, unique

import pytest

from sendr_utils import utcnow

from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant


@pytest.fixture
def card_entity():
    return Card(
        trust_card_id='trust-card-id',
        owner_uid=5555,
        tsp=TSPType.VISA,
        expire=utcnow() + timedelta(days=365),
        last4='1234',
    )


@pytest.fixture
def merchant_entity(rands):
    return Merchant(
        name=rands(),
        callback_url='http://127.0.0.1/',
    )


@pytest.fixture
def enrollment_entity(card, merchant):
    return Enrollment(
        card_id=card.card_id,
        merchant_id=merchant.merchant_id,
        tsp_card_id='tsp-card-id',
        tsp_token_id='tsp-token-id',
        tsp_token_status=TSPTokenStatus.ACTIVE,
        card_last4=card.last4,
    )


@unique
class APIKind(Enum):
    MOBILE = 'mobile'
    WEB = 'web'
