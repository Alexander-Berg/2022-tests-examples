# -*- coding: utf-8 -*-

import pytest

from billing.library.python.yt_utils.test_utils.utils import (
    create_yt_client,
    fix_yt_logger,
)
from billing.library.python.yql_utils.test_utils.utils import (
    create_yql_client,
)


@pytest.fixture
def yql_client():
    return create_yql_client()


@pytest.fixture
def yt_client():
    return create_yt_client()


@pytest.fixture(name='yt_root')
def yt_root_fixture(yt_client):
    path = yt_client.find_free_subpath('//')
    yt_client.create('map_node', path)
    return path


@pytest.fixture(scope='session', autouse=True)
def fix_yt_logger_fixture():
    fix_yt_logger()
