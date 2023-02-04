# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import allure
import tests.object_builder as ob

from brest.core.tests import utils as test_utils


@pytest.fixture(name='legacy_promocode')
@allure.step('create legacy promocode')
def create_legacy_promocode(**kwargs):
    session = test_utils.get_test_session()
    group = ob.PromoCodeGroupBuilder.construct(session, **kwargs)
    return group.promocodes[0]
