# coding: utf-8
try:  # setup plugin only when running tests with ya make
    import yatest.common
    import os
    import shutil
    import pytest


    """
    @pytest.fixture(scope='function')
    def balancer_executable_path():
        return yatest.common.binary_path('balancer/daemons/balancer/balancer')

    @pytest.fixture(scope='session', autouse=True)
    def __copy_fixtures():
        if os.path.exists('tests'):
            os.rmdir('tests')
        src_dir = yatest.common.source_path('infra/awacs/vendor/awacs/tests/fixtures')
        shutil.copytree(src_dir, 'tests/fixtures')
    """
except ImportError:
    pass
