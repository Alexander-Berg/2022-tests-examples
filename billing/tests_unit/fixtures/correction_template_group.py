# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import allure

from brest.core.tests import utils as test_utils
from tests.object_builder import CorrectionTemplateGroupBuilder


@pytest.fixture(name='correction_template_group')
@allure.step('create correction template group')
def create_correction_template_group(title='test group'):
    session = test_utils.get_test_session()
    group = CorrectionTemplateGroupBuilder(title=title).build(session).obj
    session.add(group)
    session.flush()
    return group
