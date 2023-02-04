# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import httpretty

@pytest.fixture
def not_existing_id():
    return -100500

@pytest.fixture()
def httpretty_enabled_fixture():
    """ reduce indentation when using httpretty. """
    with httpretty.enabled(allow_net_connect=True):
        yield
