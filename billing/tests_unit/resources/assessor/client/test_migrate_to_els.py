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
    has_entry,
    greater_than,
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client


@pytest.mark.smoke
class TestMigrateToEls(TestCaseApiAppBase):
    BASE_API = u'/assessor/client/migrate-to-els'

    def test_migrate_to_els(self, client):
        response = self.test_client.secure_post(self.BASE_API, data={'client_id': client.id}, is_admin=False)

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(client.has_single_account)
        assert_that(
            response.get_json()['data'],
            has_entry('single_account_number', greater_than(0)),
        )
