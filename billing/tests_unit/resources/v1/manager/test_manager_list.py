# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm
import sqlalchemy as sa

from balance import mapper

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.manager import create_manager, MANAGER_NAME
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


@pytest.mark.smoke
class TestCaseManagerList(TestCaseApiAppBase):
    BASE_API = u'/v1/manager/list'

    def test_get_manager_list(self, admin_role):
        managers = (
            self.test_session
            .query(mapper.Manager)
            .order_by(mapper.Manager.manager_code)
            .limit(10)
        )

        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API)
        data = response.get_json().get('data')

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(
            data,
            hm.contains(*[
                hm.has_entries({
                    'id': m.manager_code,
                    'name': m.name,
                    'parent_code': m.parent_code,
                })
                for m in managers
            ]),
        )

    def test_get_manager_list_filtered_by_type_bo(self, admin_role):
        from yb_snout_api.resources.v1.manager import enums

        manager_type = enums.ManagerType.BO
        managers = (
            self.test_session
            .query(mapper.Manager)
            .filter_by(manager_type=manager_type.value)
            .order_by(mapper.Manager.manager_code)
            .limit(10)
        )

        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API, {'manager_type': manager_type.name})
        data = response.get_json().get('data')

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(
            data,
            hm.contains(*[
                hm.has_entries({
                    'id': m.manager_code,
                    'name': m.name,
                })
                for m in managers
            ]),
        )

    @pytest.mark.parametrize(
        'field_name',
        ['name', 'manager_code'],
    )
    def test_get_manager_list_filtered_by_name(self, manager, admin_role, field_name):
        from yb_snout_api.resources.v1.manager import enums

        security.set_roles([admin_role])
        params = {'manager_type': enums.ManagerType(manager.manager_type).name}
        if field_name == 'name':
            params['name'] = MANAGER_NAME[1:-1].upper()  # часть имени
        elif field_name == 'manager_code':
            params['manager_code'] = manager.manager_code

        response = self.test_client.get(self.BASE_API, params)
        data = response.get_json().get('data')

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(data, hm.has_length(1))
        hm.assert_that(
            data[0],
            hm.has_entries({
                'id': manager.manager_code,
                'name': manager.name,
                'parents_names': manager.parents_names,
                'parent_code': manager.parent_code,
            }),
        )

    @pytest.mark.parametrize(
        'pn, ps',
        [
            (1, 5),
            (5, 3),
        ],
    )
    @pytest.mark.parametrize(
        'sort_key, sort_order',
        [
            ('name', 'asc'),
            ('name', 'desc'),
            ('manager_code', 'desc'),
        ],
    )
    def test_pagination(self, pn, ps, sort_key, sort_order):
        offset, limit = ps * (pn - 1), ps * pn
        sort_func = {'asc': sa.asc, 'desc': sa.desc}[sort_order]
        sort_mkey = {'name': mapper.Manager.name, 'manager_code': mapper.Manager.manager_code}[sort_key]
        managers = (
            self.test_session
            .query(mapper.Manager)
            .order_by(sort_func(sort_mkey))
            .slice(offset, limit)
            .all()
        )

        res = self.test_client.get(
            self.BASE_API,
            {
                'pagination_pn': pn,
                'pagination_ps': ps,
                'sort_key': sort_key.upper(),
                'sort_order': sort_order.upper()
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(*[
                hm.has_entries({
                    'id': m.manager_code,
                    'name': m.name,
                })
                for m in managers
            ]),
        )
