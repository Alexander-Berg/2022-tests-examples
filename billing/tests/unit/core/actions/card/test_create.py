from datetime import timedelta

import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_item, has_properties

from billing.yandex_pay.yandex_pay.core.actions.card.create import CreateCardAction
from billing.yandex_pay.yandex_pay.core.entities.enums import TokenizationQueueState, TSPType
from billing.yandex_pay.yandex_pay.core.entities.user import User


@pytest.fixture(params=list(TSPType))
def params(request):
    tsp = request.param
    return {
        'trust_card_id': 12345,
        'user': User(12345),
        'last4': '9876',
        'expire': utcnow() + timedelta(days=365),
        'tsp': tsp,
        'payment_system': '',
    }


@pytest.fixture
def action():
    return CreateCardAction


@pytest.fixture
async def returned(params, action):
    return await action(**params).run()


@pytest.mark.asyncio
async def test_card_created(storage, returned):
    card = await storage.card.get(returned.card_id)
    assert_that(
        card,
        equal_to(returned)
    )


@pytest.mark.asyncio
async def test_should_enqueue_tokenization_task_with_card_creating(storage, returned):
    queues = await alist(storage.tokenization_queue.find())
    assert_that(
        queues,
        has_item(has_properties({
            'card_id': returned.card_id,
            'state': TokenizationQueueState.PENDING,
        }))
    )


def test_action_transact(storage, action, returned):
    assert action.transact
