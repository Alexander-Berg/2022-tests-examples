# encoding: utf-8
from __future__ import absolute_import

import pytest


@pytest.fixture
def yasm_conf():
    from cppzoom import ZYasmConf
    return ZYasmConf('{"conflist": {"common": {"signals": {}, "patterns": {}, "periods": {}}}}')
