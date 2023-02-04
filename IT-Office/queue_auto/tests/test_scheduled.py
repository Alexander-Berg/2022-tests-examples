from source.tests.fixtures import (FakeFabric,
                                   FakeFabricIterator)

from source.queue_auto.tests.testdata import *
from source.queue_auto.scheduled_status import _get_transition
import pytest

@pytest.mark.parametrize("issue, transition", SCHEDULED_TESTDATA)
def test_get_transition(issue, transition):
    assert _get_transition(issue) == transition