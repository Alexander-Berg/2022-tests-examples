# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.utils import iteritems

import uuid
import random

standard_library.install_aliases()

import json
import uuid
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
class TestCasePrintFormRulesSave(TestCaseApiAppBase):
    BASE_API = '/v1/contract/print-form-rules/create'

    def test_print_form_rules_save(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': str(uuid.uuid4()),
            'caption': 'www',
            'ctype': 'DISTRIBUTION',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [],
            'create': True
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params}
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.json['data'],
            hm.has_key('intersections'),
        )
        hm.assert_that(
            res.json['data']['intersections'],
            hm.empty(),
        )

        rule = (
            self.test_session.query(mapper.ContractPrintFormRules)
                .filter(
                mapper.ContractPrintFormRules.external_id == rule_params['external_id'],
            )
        ).one_or_none()

        hm.assert_that(
            rule,
            not hm.none()
        )

        if rule_params['rule_type'] == 'contract':
            rule_params['is_contract'] = 1
        else:
            rule_params['is_contract'] = 0
        rule_params.pop('rule_type')
        rule_params['json'] = rule_params['content']
        rule_params.pop('content')
        rule_params.pop('create')

        for i in rule_params:
            hm.assert_that(
                getattr(rule, i),
                hm.equal_to(rule_params[i])
            )

    def test_print_form_rules_save_check_perm(self, admin_role):
        security.set_roles([admin_role])

        rule_params = {
            'external_id': str(uuid.uuid4()),
            'caption': 'www',
            'ctype': 'GENERAL',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [],
            'create': True
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params}
        )

        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))

    def test_print_form_rules_save_invalid_json(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': str(uuid.uuid4()),
            'caption': 'www',
            'ctype': 'GENERAL',
            'rule_type': 'contract',
            'content': [666],
            'print_forms': [],
            'create': True
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params}
        )

        hm.assert_that(res.status_code, hm.equal_to(http.UNPROCESSABLE_ENTITY))

    def test_print_form_rules_save_invalid_external_id(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': str(uuid.uuid4()) + '?',
            'caption': 'www',
            'ctype': 'DISTRIBUTION',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [],
            'create': True
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params}
        )

        hm.assert_that(res.status_code, hm.equal_to(http.UNPROCESSABLE_ENTITY))

    def test_print_form_rules_save_invalid_rule(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': 'XXX666',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'hidden': 0,
        }

        prev_rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)
        prev_rule_params = dict(caption=rule_params['caption'], external_id=rule_params['external_id'])

        rule_params = {
            'external_id': 'XXX777',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [],
            'create': True
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params}
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.json['data'],
            hm.has_entries(
                intersections=hm.contains(hm.has_entries(prev_rule_params))
            )
        )

    def test_print_form_rules_save_invalid_rule_force(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': 'XXX666',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'hidden': 0
        }

        prev_rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)
        prev_rule_params = dict(caption=rule_params['caption'], external_id=rule_params['external_id'])

        rule_params = {
            'external_id': 'XXX777',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [],
            'force': True,
            'create': True
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.json['data'],
            hm.has_key('intersections'),
        )
        hm.assert_that(
            res.json['data']['intersections'],
            hm.empty(),
        )

    def test_print_form_rules_edit(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': 'XXX666',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'hidden': 0,
        }

        prev_rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)

        rule_params = {
            'external_id': 'XXX666',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [],
            'force': False,
            'create': False
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.json['data'],
            hm.has_key('intersections'),
        )
        hm.assert_that(
            res.json['data']['intersections'],
            hm.empty(),
        )

        rule = (
            self.test_session.query(mapper.ContractPrintFormRules)
                .filter(
                mapper.ContractPrintFormRules.external_id == rule_params['external_id'],
            )
        ).one_or_none()

        hm.assert_that(
            rule,
            not hm.none()
        )

        hm.assert_that(
            rule.print_forms,
            hm.equal_to(rule_params['print_forms'])
        )

    def test_print_form_rules_edit_inter(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': 'XXX555',
            'caption': 'yyy',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abcd@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'hidden': 0,
        }

        prev_rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)
        prev_rule_params = dict(caption=rule_params['caption'], external_id=rule_params['external_id'])

        rule_params = {
            'external_id': 'XXX666',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'hidden': 0,
        }

        middle_rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)

        rule_params = {
            'external_id': 'XXX666',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'rule_type': 'contract',
            'content': {'context': 'abcd@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'force': False,
            'create': False
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.json['data'],
            hm.has_key('intersections'),
        )
        hm.assert_that(
            res.json['data']['intersections'],
            hm.empty()
        )

    def test_print_form_rules_edit_inter_force(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': 'XXX555',
            'caption': 'yyy',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abcd@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'hidden': 0,
        }

        prev_rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)
        prev_rule_params = dict(caption=rule_params['caption'], external_id=rule_params['external_id'])

        rule_params = {
            'external_id': 'XXX666',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'hidden': 0,
        }

        middle_rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)

        rule_params = {
            'external_id': 'XXX666',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'rule_type': 'contract',
            'content': {'context': 'abcd@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'force': True,
            'create': False
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            res.json['data'],
            hm.has_key('intersections'),
        )
        hm.assert_that(
            res.json['data']['intersections'],
            hm.empty(),
        )

        rule = (
            self.test_session.query(mapper.ContractPrintFormRules)
                .filter(
                mapper.ContractPrintFormRules.external_id == rule_params['external_id'],
            )
        ).one_or_none()

        hm.assert_that(
            rule,
            not hm.none()
        )

        hm.assert_that(
            rule.json,
            hm.equal_to(rule_params['content'])
        )

    def test_print_form_rules_create_existing_id(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': 'XXX555',
            'caption': 'yyy',
            'ctype': 'SPENDABLE',
            'branch': 'test',
            'rule_type': 'contract',
            'content': {'context': 'abcd@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [{'caption': 'caption', 'link': 'link'}],
            'hidden': 0,
        }

        prev_rule = ob.ContractPrintFormRuleBuilder.construct(self.test_session, **rule_params)

        rule_params = {
            'external_id': 'XXX555',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [],
            'force': False,
            'create': True
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.UNPROCESSABLE_ENTITY))

    def test_print_form_rules_edit_non_existing_rule(self, admin_role):
        role = ob.create_role(
            self.test_session,
            cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
        )

        security.set_roles([admin_role, role])

        rule_params = {
            'external_id': 'XXX555',
            'caption': 'www',
            'ctype': 'SPENDABLE',
            'rule_type': 'contract',
            'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
            'print_forms': [],
            'force': False,
            'create': False
        }

        res = self.test_client.secure_post_json(
            self.BASE_API,
            data={"rule_params": rule_params},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.UNPROCESSABLE_ENTITY))

        def test_print_form_rules_edit_without_mode(self, admin_role):
            role = ob.create_role(
                self.test_session,
                cst.PermissionCode.PRINT_TEMPLATE_RULES_EDIT,
            )

            security.set_roles([admin_role, role])

            rule_params = {
                'external_id': 'XXX555',
                'caption': 'www',
                'ctype': 'SPENDABLE',
                'rule_type': 'contract',
                'content': {'context': 'abc@', 'attr': 'cab@', 'value': 'cba@'},
                'print_forms': [],
                'force': False,
            }

            res = self.test_client.secure_post_json(
                self.BASE_API,
                data={"rule_params": rule_params},
            )

            hm.assert_that(res.status_code, hm.equal_to(http.UNPROCESSABLE_ENTITY))
