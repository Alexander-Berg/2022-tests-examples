# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.utils import iteritems

import uuid
import random

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http
import balance.constants as cst
from balance import mapper
from brest.core.tests import security

from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role
from balance.printform.rules import *


@pytest.mark.smoke
class TestCasePrintFormRulesValidate(TestCaseApiAppBase):
    BASE_API = u'/v1/contract/print-form-rules/validate'

    @pytest.mark.parametrize('ctype', ['GENERAL', 'SPENDABLE', 'DISTRIBUTION'])
    def test_print_form_rules_validate(self, admin_role, ctype):
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
            'content': Rule(item=Terminal('test_print_form_rules_validate', '', '')).encode(),
            'print_forms': [{'caption': 'caption', 'link': 'link'}]
        }

        rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)
        hm.assert_that(rule.hidden, hm.equal_to(1))
        res = self.test_client.get(
            self.BASE_API,
            params={'external_id': rule.external_id},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.json['data'],
            hm.has_key('intersections')
        )
        hm.assert_that(
            res.json['data']['intersections'],
            hm.empty()
        )

    def test_print_form_rules_validate_invalid_rule(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': str(uuid.uuid4()),
            'caption': 'www',
            'ctype': 'GENERAL',
            'branch': 'test',
            'rule_type': 'contract',
            'content': Rule(Terminal('a', 'a', 'a')).encode(),
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
        }

        rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)
        rule_params['hidden'] = 0
        rule_params['external_id'] = str(uuid.uuid4())
        rule2 = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)

        hm.assert_that(rule.hidden, hm.equal_to(1))
        res = self.test_client.get(
            self.BASE_API,
            params={'external_id': rule.external_id},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(rule.hidden, hm.equal_to(1))
        hm.assert_that(
            res.json['data'],
            hm.has_entries(
                intersections=hm.contains(
                    hm.has_entries(dict(caption=rule_params['caption'], external_id=rule_params['external_id']))
                )
            )
        )

    def test_print_form_rules_validate_check_perm(self, admin_role):
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
