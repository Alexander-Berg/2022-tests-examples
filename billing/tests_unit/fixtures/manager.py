# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import allure

from tests import object_builder as ob

from brest.core.tests import utils as test_utils


MANAGER_NAME = 'test_snout_manager'


@pytest.fixture(name='manager')
@allure.step('create manager')
def create_manager():
    session = test_utils.get_test_session()
    return ob.SingleManagerBuilder(name=MANAGER_NAME).build(session).obj
