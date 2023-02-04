import pytest
from pay.lib.interactions.yandex_delivery.entities import CancelState

from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import (
    DeliveryCancelState,
    SatisfactoryDeliveryCancelState,
)


@pytest.mark.parametrize('yd_state', list(CancelState))
def test_delivery_cancel_state_from_yandex_delivery(yd_state):
    assert DeliveryCancelState.from_yandex_delivery(yd_state).value == yd_state.value.upper()


@pytest.mark.parametrize('state', list(SatisfactoryDeliveryCancelState))
def test_satisfactory_state_to_yandex_delivery(state):
    assert state.to_yandex_delivery().value == state.value.lower()
