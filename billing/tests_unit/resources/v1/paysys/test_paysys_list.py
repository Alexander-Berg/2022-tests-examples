# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import sqlalchemy as sa
from balance import mapper
from hamcrest import assert_that, equal_to

from brest.core.tests import utils as test_utils, security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


class TestCasePaysysList(TestCaseApiAppBase):
    BASE_API = u'/v1/paysys/list'

    def test_get_paysys_list(self, admin_role):
        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

    def test_get_filtered_paysys_list(self, admin_role):
        char_codes = ['ph', 'ur']

        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API, {'char_codes': ','.join(char_codes)})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        session = test_utils.get_test_session()
        paysys_select = sa.sql.select(
            columns=(mapper.Paysys.id,),
            from_obj=mapper.Paysys,
            whereclause=mapper.Paysys.cc.in_(char_codes),
        )

        filtered_paysys_ids = session.execute(paysys_select).fetchall()
        filtered_paysys_ids = sorted([i[0] for i in filtered_paysys_ids])
        response_paysys_ids = sorted([i['id'] for i in data])

        assert_that(
            response_paysys_ids,
            equal_to(filtered_paysys_ids),
            'paysys_list must contain only filtered paysyses',
        )
