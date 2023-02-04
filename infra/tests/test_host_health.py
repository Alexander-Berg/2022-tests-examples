import pytest

from infra.walle.server.tests.lib.api_util import juggler_stored_check
from infra.walle.server.tests.lib.util import TestCase
from sepelib.core.constants import DAY_SECONDS
from walle.host_health import _gc_outdated_host_health
from walle.models import timestamp


@pytest.fixture
def test(request):
    return TestCase.create(request, healthdb=True)


def test_outdated_health_gc(test):
    for i in range(3):
        check = juggler_stored_check('somehost-{}'.format(i), ts=timestamp() - i * DAY_SECONDS)
        add = True if not i else False  # first check with ts=timestamp() has to stay in collection after gc
        test.health_checks.mock(check, add=add, save=True)

    _gc_outdated_host_health()
    test.health_checks.assert_equal()
