# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest

from tests import object_builder as ob

from brest.core.tests import utils as test_utils


@pytest.fixture(name='firm')
def create_firm(**kwargs):
    session = test_utils.get_test_session()
    return ob.FirmBuilder.construct(session, **kwargs)
