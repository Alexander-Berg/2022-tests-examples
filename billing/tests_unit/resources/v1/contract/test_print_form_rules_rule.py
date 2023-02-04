# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.utils import iteritems

import uuid
import random

standard_library.install_aliases()

import uuid
import pytest
import hamcrest as hm
import http.client as http
import balance.constants as cst
from brest.core.tests import security
from balance.printform.rules import *

from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


@pytest.mark.smoke
class TestCasePrintFormRulesRule(TestCaseApiAppBase):
    BASE_API = u'/v1/contract/print-form-rules/rule'

    @pytest.mark.parametrize('ctype', ['GENERAL', 'SPENDABLE', 'DISTRIBUTION'])
    def test_print_form_rules_rule(self, admin_role, ctype):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': str(uuid.uuid4()),
            'caption': 'www',
            'ctype': ctype,
            'branch': 'test',
            'rule_type': 'contract',
            'content': Rule(item=Terminal('','','')).encode(),
            'print_forms': [{'caption': 'caption', 'link': 'link'}]
        }
        rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)
        res = self.test_client.get(
            self.BASE_API,
            params={'external_id': rule.external_id},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.json['data'],
            hm.has_entries(rule_params)
        )

        hm.assert_that(
            res.json['data'],
            hm.has_key('version')
        )

        hm.assert_that(
            res.json['data'],
            hm.has_key('is_published')
        )

    def test_print_form_rules_rule_check_perm(self, admin_role):
        security.set_roles([admin_role])

        rule_params = {
            'external_id': str(uuid.uuid4()),
            'caption': 'www',
            'ctype': 'GENERAL',
            'branch': 'test',
            'rule_type': 'contract',
            'content': Rule(item=Terminal('', '', '')).encode(),
            'print_forms': [{'caption': 'caption', 'link': 'link'}]
        }
        rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)
        res = self.test_client.get(
            self.BASE_API,
            params={'external_id': rule.external_id},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))

    def test_print_form_rules_rule_not_found(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        res = self.test_client.get(
            self.BASE_API,
            params={'external_id': 'XXXXX66666'},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
