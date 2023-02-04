# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest

from tests import object_builder as ob

from brest.core.tests import utils as test_utils


@pytest.fixture(name='passport')
def create_passport(**kwargs):
    session = test_utils.get_test_session()
    return ob.create_passport(session, **kwargs)
