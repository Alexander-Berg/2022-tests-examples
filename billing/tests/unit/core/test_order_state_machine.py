from dataclasses import dataclass
from decimal import Decimal
from typing import Optional

import pytest
from transitions import MachineError

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.order_state_machine import MachineState, OrderStateMachine
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import ClassicOrderStatus


@pytest.fixture(params=[ClassicOrderStatus.NEW])
def initial_order_status(request):
    return request.param


@pytest.fixture(params=[Decimal(0)])
def initial_amount(request):
    return request.param


@pytest.fixture
def machine(initial_order_status, initial_amount):
    return OrderStateMachine.from_order(initial_order_status, initial_amount)


@dataclass
class Transition:
    trigger: str
    amount: Decimal
    state: MachineState
    _expected_amount: Optional[Decimal] = None

    @property
    def expected_amount(self):
        return self._expected_amount if self._expected_amount is not None else self.amount


HOLD_1 = Transition('hold', Decimal(1), MachineState.CAPTURED)
HOLD_10 = Transition('hold', Decimal(10), MachineState.CAPTURED)
HOLD_NOOP = Transition('hold', Decimal(3), MachineState.CHARGED, Decimal(5))
SUCCESS_5 = Transition('success', Decimal(5), MachineState.CHARGED)
SUCCESS_15 = Transition('success', Decimal(15), MachineState.CHARGED)
FAIL = Transition('fail', Decimal(10), MachineState.FAILED, Decimal(0))
REVERSE = Transition('reverse', Decimal(10), MachineState.CANCELLED)
REVERSE_6 = Transition('reverse', Decimal(6), MachineState.CANCELLED)
REVERSE_5 = Transition('reverse', Decimal(5), MachineState.CANCELLED)
PARTIAL_REVERSE_4 = Transition('reverse', Decimal(4), MachineState.CHARGED)
PARTIAL_REFUND_1 = Transition('refund', Decimal(1), MachineState.CAPTURED)
FULL_REFUND_5 = Transition('refund', Decimal(5), MachineState.CAPTURED)
CHARGEBACK = Transition('chargeback', Decimal(99), MachineState.CANCELLED)


@pytest.mark.parametrize('initial_order_status', list(ClassicOrderStatus), indirect=True)
def test_can_init_with_every_order_status(initial_order_status, machine):
    expected = MachineState.from_order(initial_order_status, machine.amount)
    assert_that(machine.state, equal_to(expected))


class TestValidTransitionScenarios:
    @pytest.mark.parametrize(
        'scenario',
        [
            (HOLD_10, HOLD_1),
            (HOLD_1, SUCCESS_5),
            (HOLD_1, SUCCESS_5, SUCCESS_5),
            (HOLD_1, SUCCESS_5, HOLD_NOOP),
            (SUCCESS_5,),
            (HOLD_1, HOLD_1, SUCCESS_5),
            (HOLD_1, HOLD_10, SUCCESS_5),
            (HOLD_1, HOLD_10, SUCCESS_15),
        ]
    )
    def test_hold_and_success_scenarios(self, machine, scenario):
        assert_that(machine.amount, equal_to(0))

        for transition in scenario:
            machine.trigger(transition.trigger, transition.amount)
            assert_that(machine.state, transition.state)
            assert_that(machine.amount, equal_to(transition.expected_amount))

    @pytest.mark.parametrize(
        'scenario',
        [
            (FAIL,),
            (FAIL, FAIL),
            (FAIL, HOLD_1),
            (FAIL, SUCCESS_5),
        ]
    )
    def test_fail_scenarios(self, machine, scenario):
        assert_that(machine.amount, equal_to(0))

        for transition in scenario:
            machine.trigger(transition.trigger, transition.amount)
            assert_that(machine.state, transition.state)
            assert_that(machine.amount, equal_to(transition.expected_amount))

    @pytest.mark.parametrize(
        'scenario,expected_amount',
        [
            # reverse
            ((HOLD_1, REVERSE), HOLD_1.amount),
            ((HOLD_1, HOLD_10, REVERSE, REVERSE), HOLD_10.amount),
            # reverse after success
            ((SUCCESS_5, REVERSE_5), Decimal(0)),
            ((SUCCESS_5, PARTIAL_REVERSE_4), Decimal(1)),
            ((SUCCESS_5, REVERSE_6), Decimal(5)),  # NOTE: случай amount > self.amount как будто забыли
            # refund
            ((HOLD_1, SUCCESS_5, PARTIAL_REFUND_1), Decimal(4)),
            ((SUCCESS_15, PARTIAL_REFUND_1, PARTIAL_REFUND_1), Decimal(13)),
            ((SUCCESS_5, FULL_REFUND_5), Decimal(0)),
            # chargeback
            ((HOLD_1, SUCCESS_5, CHARGEBACK), SUCCESS_5.amount),
            ((HOLD_10, SUCCESS_5, PARTIAL_REFUND_1, CHARGEBACK), Decimal(4)),
            ((SUCCESS_15, CHARGEBACK, CHARGEBACK), SUCCESS_15.amount),
            ((SUCCESS_5, PARTIAL_REFUND_1, PARTIAL_REFUND_1, CHARGEBACK), Decimal(3)),
        ]
    )
    def test_scenarios_with_refunds(self, machine, scenario, expected_amount):
        assert_that(machine.amount, equal_to(0))

        for transition in scenario:
            machine.trigger(transition.trigger, transition.amount)
            assert_that(machine.state, transition.state)

        assert_that(machine.amount, equal_to(expected_amount))


class TestBadTransitionScenarios:
    @pytest.mark.parametrize(
        'scenario',
        [
            (SUCCESS_5, FAIL),
            (SUCCESS_5, FULL_REFUND_5, SUCCESS_5),
            (SUCCESS_5, FULL_REFUND_5, FULL_REFUND_5),
            (SUCCESS_5, CHARGEBACK, SUCCESS_5),
            (SUCCESS_5, CHARGEBACK, FULL_REFUND_5),
            (PARTIAL_REFUND_1,),
            (FULL_REFUND_5,),
            (CHARGEBACK,),
        ]
    )
    def test_bad_transitions(self, machine, scenario):
        assert_that(machine.amount, equal_to(0))

        for transition in scenario[:-1]:
            machine.trigger(transition.trigger, transition.amount)
            assert_that(machine.state, transition.state)

        last_transition = scenario[-1]
        pattern = "Can't trigger event .+ from state .+"
        with pytest.raises(MachineError, match=pattern):
            machine.trigger(last_transition.trigger, last_transition.amount)

    @pytest.mark.parametrize('initial_order_status', set(ClassicOrderStatus) - {ClassicOrderStatus.NEW}, indirect=True)
    @pytest.mark.parametrize('initial_amount', [Decimal(1)], indirect=True)
    def test_negative_amount_not_allowed(self, machine, initial_order_status):
        if initial_order_status == ClassicOrderStatus.SUCCESS:
            new_status = ClassicOrderStatus.CHARGEBACK
        else:
            new_status = initial_order_status

        pattern = 'Negative amount not allowed'
        with pytest.raises(MachineError, match=pattern):
            machine.trigger(new_status.value, Decimal(-1))
