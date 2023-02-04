# -*- coding: utf-8 -*-
import os

import pytest

from tests import TestAPI, get_testdata_config
from vertis.memcached.multiple import MemcachedConfig, MemcachedProperties
from vertis.memcached.multiple.utils.misc import parse_cfg


class TestConfigs(TestAPI):

    def test_regular_config(self):
        assert self.build_config('standart.cfg')

    def test_vs_all_config(self):
        assert self.build_config('csback.cfg')

    def test_missing_fields_config(self):
        config = get_testdata_config('standart.cfg')
        assert os.path.exists(config)

        mapping = parse_cfg(config)
        mapping.__delitem__('mem')

        with pytest.raises(AssertionError) as e:
            MemcachedConfig.create(mapping)

        assert 'Missing some option' in str(e.value)
        assert 'mem' in str(e.value)
