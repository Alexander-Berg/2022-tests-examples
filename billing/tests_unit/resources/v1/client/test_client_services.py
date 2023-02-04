# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order


@pytest.mark.smoke
class TestCaseClient(TestCaseApiAppBase):
    BASE_API = '/v1/client/services'

    def test_base(self, order):
        service = order.service

        security.set_roles([])
        security.set_passport_client(order.client)

        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains(
                    hm.has_entries({
                        'id': service.id,
                        'display_name': service.display_name,
                        'cc': service.cc,
                    }),
                ),
                'total_count': 1,
            }),
        )
