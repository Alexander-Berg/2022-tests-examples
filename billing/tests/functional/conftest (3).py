import re

import pytest

from billing.yandex_pay.yandex_pay.tests.common_conftest import *  # noqa
from billing.yandex_pay.yandex_pay.tests.db import *  # noqa


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine


@pytest.fixture
def mock_internal_tvm(aioresponses_mocker, yandex_pay_settings):
    aioresponses_mocker.get(
        re.compile(f'.*{yandex_pay_settings.TVM_URL}/tvm/checksrv.*'),
        payload={
            'src': yandex_pay_settings.TVM_ALLOWED_SRC[0],
            'dst': yandex_pay_settings.TVM_ALLOWED_SRC[0],
        },
    )
