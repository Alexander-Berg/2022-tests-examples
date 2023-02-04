# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import allure

from brest.core.tests import utils as test_utils
from balance.mapper import Terminal


@pytest.fixture(name='any_existing_terminal_id')
@allure.step('get any valid terminal id from the db')
def get_any_existing_terminal_id():
    session = test_utils.get_test_session()
    return session.query(Terminal.id).first().id
