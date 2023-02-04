# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
from hamcrest import (
    assert_that,
    equal_to,
    contains_string,
    has_entries,
)

from balance import constants as cst

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role


@pytest.fixture(name='view_dev_data_role')
def create_view_dev_data_role():
    return create_role(cst.PermissionCode.VIEW_DEV_DATA)


class TestCaseDebugDump(TestCaseApiAppBase):
    BASE_API = '/v1/debug/dump'

    host = 'new_host'  # proxy заменяет заголовок на url, указанный в конфиге
    x_forwarded_host = 'x_forwarded_host'

    def test_get_dump_x_fields(self):
        response = self.test_client.get(
            self.BASE_API,
            headers={
                'X-Fields': '{resource{session{oper_id}}}',
            },
        )

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(
            response.data,
            contains_string(u'"oper_id": {0}'.format(self.test_session.oper_id)),
            'response must contains passport uid',
        )

    def test_get_dump_hosts(self):
        response = self.test_client.get(
            self.BASE_API,
            headers={
                'Host': self.host,
                'X-Forwarded-Host': self.x_forwarded_host,
            },
        )

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(
            response.get_json().get('data'),
            has_entries({
                'request': has_entries({
                    'header': has_entries({'Host': self.host, 'X-Forwarded-Host': self.x_forwarded_host}),
                    'request_host': self.x_forwarded_host,  # в request должен попасть правильный host
                }),
            }),
        )
