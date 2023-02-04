from decimal import Decimal

import pytest
import yenv

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.entities.processing import (
    HoldResultAuthorized,
    ResultAuthorized,
    ResultCancelled,
    ResultCleared,
    ResultFailed,
    ResultRefunded,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.processings.mock import MockProcessing

PARAMS = {
    'hold': dict(payment_token='ToKeN', amount=Decimal('12')),
    'clear': dict(amount=Decimal('10')),
    'refund': dict(amount=Decimal('10')),
}


@pytest.mark.parametrize(
    'action_name, expected',
    (
        ('hold', HoldResultAuthorized()),
        ('submit_3ds', ResultAuthorized()),
        ('submit_fingerprinting', ResultAuthorized()),
        ('cancel', ResultCancelled()),
        ('clear', ResultCleared()),
        ('refund', ResultRefunded()),
    ),
)
@pytest.mark.asyncio
async def test_success(processing, action_name, expected):
    action = getattr(processing, action_name)
    assert_that(
        await action(**PARAMS.get(action_name, {})),
        equal_to(expected),
    )


@pytest.mark.asyncio
async def test_chooses_scenario(processing):
    assert_that(
        await processing.hold(payment_token='ToKeN', amount=Decimal('10002')),
        equal_to(ResultFailed(reason='NOT_ENOUGH_FUNDS')),
    )


@pytest.mark.parametrize(
    'action_name',
    (
        'hold',
        'submit_3ds',
        'submit_fingerprinting',
        'cancel',
        'clear',
        'refund',
    ),
)
@pytest.mark.asyncio
async def test_always_fails_on_prod(mocker, processing, action_name):
    mocker.patch.object(yenv, 'type', 'production')

    action = getattr(processing, action_name)
    assert_that(
        await action(**PARAMS.get(action_name, {})),
        equal_to(ResultFailed(reason='WRONG_ENVIRONMENT')),
    )


@pytest.fixture
def processing(core_context, stored_integration, stored_checkout_order, stored_transaction, stored_psp):
    return MockProcessing(
        context=core_context,
        integration=stored_integration,
        order=stored_checkout_order,
        transaction=stored_transaction,
        psp=stored_psp,
    )
