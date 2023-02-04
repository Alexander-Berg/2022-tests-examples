# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.builtins import str as text

standard_library.install_aliases()

import pytest
import mock
import http.client as http
import hamcrest as hm

from balance import constants as cst, mapper

from brest.core.tests import security
from brest.core.tests.base import yb_test_app
from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
from yb_snout_api.tests_unit.fixtures.firm import create_firm
from tests import object_builder as ob


@pytest.fixture(name='some_role')
def create_some_role():
    return create_role((enums.PermissionCode.BILLING_SUPPORT.value, {cst.ConstraintTypes.firm_id: None}))


class TestCaseFirmList(TestCaseApiAppBase):
    BASE_API = '/v1/firm/list'

    def test_get_firm_list(self, admin_role):
        firms_count = self.test_session.query(mapper.Firm).count()

        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API)
        data = response.get_json().get('data')

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(len(data), hm.equal_to(firms_count), 'equal count of all firms')

    def test_filter_by_perm(self, admin_role, some_role):
        firm_ids = [
            cst.FirmId.YANDEX_OOO,
            cst.FirmId.CLOUD,
        ]
        roles = [
            admin_role,
            (some_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
            (some_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD}),
        ]

        security.set_roles(roles)
        response = self.test_client.get(
            self.BASE_API,
            data={'permission': enums.PermissionCode.BILLING_SUPPORT.name},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        hm.assert_that(
            response.get_json().get('data'),
            hm.contains_inanyorder(*[
                hm.has_entry('id', id)
                for id in firm_ids
            ]),
        )

    @pytest.mark.parametrize(
        'test_env, env_type, is_ok',
        [
            (0, 'prod', True),
            (0, 'test', True),
            (1, 'prod', False),
            (1, 'test', True),
        ],
    )
    def test_with_test_env(self, test_env, env_type, is_ok):
        firm = create_firm(test_env=test_env)

        with mock.patch.object(yb_test_app, 'get_current_env_type', return_value=env_type):
            response = self.test_client.get(self.BASE_API)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        data = response.get_json().get('data')

        firm_ids = {r['id'] for r in data}
        if is_ok:
            assert firm.id in firm_ids
        else:
            assert firm.id not in firm_ids

    def test_filter_by_ids(self):
        firm_ids = [cst.FirmId.YANDEX_OOO, cst.FirmId.AUTORU, cst.FirmId.BUS]

        res = self.test_client.get(
            self.BASE_API,
            {'ids': ', '.join(map(text, firm_ids))},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', [])
        hm.assert_that(
            data,
            hm.contains_inanyorder(*[
                hm.has_entries({'id': id_})
                for id_ in firm_ids
            ]),
        )


class TestCaseIntercompanyList(TestCaseApiAppBase):
    BASE_API = '/v1/firm/intercompany_list'

    def test_get_intercompany_list(self, admin_role):
        intercompany_count = self.test_session.query(mapper.Intercompany).count()

        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API)
        data = response.get_json().get('data')

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(len(data), hm.equal_to(intercompany_count), 'equal count of all intercompany')
