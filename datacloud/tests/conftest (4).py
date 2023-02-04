# -*- coding: utf-8 -*-
import pytest

import yatest.common as yc
from mapreduce.yt.python.yt_stuff import YtConfig

CYPRESS_DIR = 'datacloud/features/geo/tests/cypress_data'


@pytest.fixture(scope='module')
def yt_config():
    return YtConfig(local_cypress_dir=yc.source_path(CYPRESS_DIR))


@pytest.fixture(scope='module')
def yt_client(yt):
    return yt.get_yt_client()
