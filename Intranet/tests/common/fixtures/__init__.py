import pytest

from .models import *  # noqa: F401, F403
from .mocks import *  # noqa: F401, F403

from contextlib import contextmanager


@pytest.fixture(scope='function')
def django_assert_num_queries_lte(pytestconfig):
    from django.db import connection
    from django.test.utils import CaptureQueriesContext

    @contextmanager
    def _assert_num_queries_lte(limit):
        with CaptureQueriesContext(connection) as context:
            yield
            if len(context) > limit:
                msg = "Expected to perform less than or equal %s queries but %s were done" % (limit, len(context))
                if pytestconfig.getoption('verbose') > 0:
                    sqls = (q['sql'] for q in context.captured_queries)
                    msg += '\n\nQueries:\n========\n\n%s' % '\n\n'.join(sqls)
                else:
                    msg += " (add -v option to show queries)"
                pytest.fail(msg)

    return _assert_num_queries_lte
