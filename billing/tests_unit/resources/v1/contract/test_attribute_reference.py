# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.utils import iteritems

standard_library.install_aliases()

import random
import pytest
import hamcrest as hm
import http.client as http
import balance.constants as cst
from brest.core.tests import security

from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


@pytest.mark.smoke
class TestCaseAttributeReference(TestCaseApiAppBase):
    BASE_API = u'/v1/contract/print-form-rules/attributes'

    @pytest.mark.parametrize('ctype', ['GENERAL', 'SPENDABLE', 'DISTRIBUTION'])
    def test_attribute_reference(self, admin_role, ctype):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        res = self.test_client.get(
            self.BASE_API,
            params={'ctype': ctype},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(res.json['data'], hm.has_key('rule_types'))
        hm.assert_that(res.json['data'], hm.has_key('contexts'))
        hm.assert_that(res.json['data'], hm.has_key('attribute_types'))
        hm.assert_that(res.json['data'], hm.has_key('attributes'))

        ref = res.json['data']

        for i in ref['attributes']:
            if i['label'] == 'FINISH_DT':
                hm.assert_that(i['values'], not hm.empty())
                hm.assert_that(i['values'][0], hm.has_entries({'id': 1, 'label': u'Определена'}))
            elif i['label'] == 'PARTNER_CREDIT':
                hm.assert_that(i['values'], not hm.empty())
                hm.assert_that(i['values'][0], hm.has_entries({'id': 1, 'label': u'Доступен'}))

    def test_attribute_reference_check_perm(self, admin_role):
        security.set_roles([admin_role])

        res = self.test_client.get(
            self.BASE_API,
            params={'ctype': 'GENERAL'},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        ref = res.json['data']
        hm.assert_that(ref['rule_types'], hm.empty())
        hm.assert_that(ref['contexts'], hm.empty())
        hm.assert_that(ref['attribute_types'], hm.empty())
        hm.assert_that(ref['attributes'], hm.empty())
