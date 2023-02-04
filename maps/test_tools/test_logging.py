import pytest

import maps.analyzer.pylibs.envkit.loggy as loggy


@pytest.fixture(scope='session', autouse=True)
def init_test_logging(request):
    loggy.init_logging()
