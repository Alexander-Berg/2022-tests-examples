# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from hamcrest import assert_that, equal_to, contains, has_length, has_item, has_entry
import http.client as http
import pytest
import mock

from balance import constants as cst
from balance.correction_template import TemplateWrapper

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.correction_template_group import create_correction_template_group
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role


TEST_CORRECTION_TEMPLATE = {
    'id': 'TEST/TEST',
    'title': 'Стандартный шаблон',
    'fields': [
        {
            'name': 'client_id',
            'type': 'integer',
            'title': 'client_id',
            'required': True,
            'values': [],
        },
        {
            'name': 'service_id',
            'type': 'integer',
            'title': 'service_id',
            'required': True,
            'values': [],
        },
        {
            'name': 'TRANSACTION_TYPE',
            'type': 'string',
            'title': 'TRANSACTION_TYPE',
            'required': False,
            'values': [
                'payment',
                'refund',
            ],
        },
    ],
}


def prepare_template(template_id, allowed_groups):
    template = TEST_CORRECTION_TEMPLATE.copy()
    template['groups'] = allowed_groups
    return TemplateWrapper(template_id, template, session=mock.MagicMock())


@pytest.fixture(name='use_correction_template_role')
def create_use_correction_template_role():
    return create_role((cst.PermissionCode.USE_CORRECTION_TEMPLATE, {cst.ConstraintTypes.template_group_id: None}))


@pytest.mark.smoke
class TestCaseCorrectionTemplateList(TestCaseApiAppBase):
    BASE_API = '/v1/correction/template/list'

    def test_correction_template_list(self, mocker, admin_role, use_correction_template_role):
        group = create_correction_template_group()
        template = prepare_template('test', [group.id])
        mocker.patch('balance.correction_template.list_templates', return_value=[template])

        roles = [
            admin_role,
            (use_correction_template_role, {cst.ConstraintTypes.template_group_id: group.id}),
        ]
        security.set_roles(roles)

        response = self.test_client.get(self.BASE_API)
        assert_that(response.status_code, equal_to(http.OK), u'Response code must be OK.')
        response_data = response.get_json()['data']
        assert_that(response_data, has_length(1))
        expected_template = template.get_prepared_template()
        expected_template.pop('groups')
        assert_that(response_data, contains(expected_template))


@pytest.mark.smoke
@pytest.mark.permissions
class TestCorrectionTemplateListAccess(TestCaseApiAppBase):
    BASE_API = '/v1/correction/template/list'

    @staticmethod
    def prepare_templates(group, allowed_template_ids):
        """
        Создадим для тестов три шаблона - c id test-0, test-1, test-2
        """
        templates = []
        for i in range(0, 3):
            template_id = 'test-' + str(i)
            templates.append(
                prepare_template(template_id, [group.id] if group and template_id in allowed_template_ids else []),
            )
        return templates

    @pytest.mark.parametrize('allowed_template_ids, expected_template_ids', [
        pytest.param([], [], id='Empty list for user without permission'),
        pytest.param(['test-0', 'test-2', 'test-100'], ['test-0', 'test-2'],
                     id='Filtered list for user with permission'),
    ])
    def test_correction_template_list_filtering(self, mocker, admin_role, use_correction_template_role,
                                                allowed_template_ids, expected_template_ids):
        roles = [admin_role]
        group = create_correction_template_group()
        if allowed_template_ids:
            roles.append((use_correction_template_role, {cst.ConstraintTypes.template_group_id: group.id}))
        security.set_roles(roles)

        templates = self.prepare_templates(group, allowed_template_ids)
        mocker.patch('balance.correction_template.list_templates', return_value=templates)

        response = self.test_client.get(self.BASE_API)
        assert_that(response.status_code, equal_to(http.OK), u'Response code must be OK.')
        response_data = response.get_json()['data']
        assert_that(response_data, has_length(len(expected_template_ids)))
        for id_ in expected_template_ids:
            assert_that(response_data, has_item(has_entry('id', id_)))
