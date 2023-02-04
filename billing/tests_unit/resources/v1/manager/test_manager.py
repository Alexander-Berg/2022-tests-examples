# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.manager import create_manager


@pytest.mark.smoke
class TestCaseManager(TestCaseApiAppBase):
    BASE_API = u'/v1/manager'

    def test_get_manager(self, manager):
        response = self.test_client.get(self.BASE_API, {'manager_code': manager.manager_code})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        manager_data = response.get_json().get('data')
        assert_that(manager_data.get('id'), equal_to(manager.manager_code))
        assert_that(
            manager_data,
            has_entries({
                'id': manager.manager_code,
                'name': manager.name,
                'parents_names': manager.parents_names,
                'parent_code': manager.parent_code,
            }),
        )

    def test_manager_not_found(self):
        # такого id не должно быть в базе
        response = self.test_client.get(self.BASE_API, {'manager_code': -1})
        assert_that(response.status_code, equal_to(http.NOT_FOUND), 'response code must be NOT_FOUND')
