import os

import pytest
from awtest import t


IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ

if IS_ARCADIA:
    @pytest.fixture(scope='module')
    def vcr_cassette_dir(request):
        return t('test_controllers/test_namespace_ctls/cassettes')
