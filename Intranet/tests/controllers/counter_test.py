import pytest

from staff.gap.controllers.counter import CounterCtl
from staff.gap.controllers.gap import GapCtl
from staff.gap.tests.constants import COUNTERS_MONGO_COLLECTION

DEFAULT_VALUE = 100


@pytest.yield_fixture
def counter_ctl(gap_test):
    ctl = CounterCtl()
    assert ctl.MONGO_COLLECTION == COUNTERS_MONGO_COLLECTION
    ctl.set_counter(GapCtl.COUNTER_NAME, DEFAULT_VALUE, GapCtl.MONGO_COLLECTION)

    yield ctl

    ctl.set_counter(GapCtl.COUNTER_NAME, DEFAULT_VALUE, GapCtl.MONGO_COLLECTION)


@pytest.mark.django_db
def test_counter(counter_ctl, db):
    counter_ctl.set_counter(GapCtl.COUNTER_NAME, 10, GapCtl.MONGO_COLLECTION)
    assert 10 == counter_ctl.get_counter(GapCtl.COUNTER_NAME, increment=False)
    assert 11 == counter_ctl.get_counter(GapCtl.COUNTER_NAME)
    assert 12 == counter_ctl.get_counter(GapCtl.COUNTER_NAME)
