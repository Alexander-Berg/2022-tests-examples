# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import copy
import http.client as http
import pytest
import mock
from hamcrest import assert_that, equal_to

from balance import constants as cst
from balance.utils.ya_bunker import BunkerRepository

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.correction_template_group import create_correction_template_group
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role


TEMPLATE_ID = 'test/template'

TEST_CORRECTION_TEMPLATE = {
    'title': 'Стандартный шаблон',
    'fields': [
        {
            'name': 'PARTNER_ID',
            'type': 'string',
            'title': 'PARTNER_ID',
            'required': True,
        },
        {
            'name': 'STARTRACK_ID',
            'type': 'string',
            'title': 'STARTRACK_ID',
            'required': True,
        },
        {
            'name': 'TRANSACTION_TYPE',
            'type': 'string',
            'title': 'TRANSACTION_TYPE',
            'required': True,
        },
        {
            'name': 'SERVICE_ID',
            'type': 'number',
            'title': 'SERVICE_ID',
            'required': True,
        },
        {
            'name': 'DT',
            'type': 'datetime',
            'title': 'DT',
            'required': True,
        },
        {
            'name': 'paysys_type_cc',
            'type': 'string',
            'title': 'paysys_type_cc',
            'required': True,
        },
        {
            'name': 'payment_type',
            'type': 'string',
            'title': 'payment_type',
            'required': True,
        },
        {
            'name': 'amount',
            'type': 'number',
            'title': 'amount',
            'required': True,
        },
        {
            'name': 'amount_fee',
            'type': 'number',
            'title': 'amount_fee',
            'required': True,
        },
        {
            'name': 'yandex_reward',
            'type': 'number',
            'title': 'yandex_reward',
            'required': True,
        },
        {
            'name': 'auto',
            'type': 'boolean',
            'title': 'auto',
            'required': True,
        },
        {
            'name': 'internal',
            'type': 'boolean',
            'title': 'internal',
            'required': True,
        },
    ],
}


@pytest.fixture(name='use_correction_template_role')
def create_use_correction_template_role():
    return create_role((cst.PermissionCode.USE_CORRECTION_TEMPLATE, {cst.ConstraintTypes.template_group_id: None}))


def get_template_with_groups(groups):
    template = copy.deepcopy(TEST_CORRECTION_TEMPLATE)
    template['groups'] = groups
    return template


@pytest.mark.smoke
@pytest.mark.slow
@pytest.mark.permissions
class TestCorrectionCreateAccess(TestCaseApiAppBase):
    BASE_API = '/v1/correction/create'

    @staticmethod
    def prepare_post_data(contract):
        return {
            'partner_id': contract.client_id,
            'service_id': list(contract.current_state().services)[0],
            'template_id': TEMPLATE_ID,
            'contract_id': contract.id,
            'startrack_id': 'BALANCE_TEST-12',
            'transaction_type': 'payment',
            'dt': '2020-01-01',
            'paysys_type_cc': 'yandex',
            'payment_type': 'correction',
            'amount': 100,
            'amount_fee': 10,
            'yandex_reward': 1,
            'auto': "false",
            'internal': 1,
        }

    @pytest.mark.parametrize('template_in_allowed_group', [
        pytest.param(False, id='Template is not in the allowed group / FORBIDDEN'),
        pytest.param(True, id='Template in the allowed group / OK'),
    ])
    def test_correction_creating(self, mocker, admin_role, use_correction_template_role, template_in_allowed_group):
        roles = [admin_role]
        allowed_groups = [-1, -2]  # случайные несуществующие группы
        group = create_correction_template_group()

        if template_in_allowed_group:
            allowed_groups.append(group.id)
            roles.append((use_correction_template_role, {cst.ConstraintTypes.template_group_id: group.id}))
        security.set_roles(roles)

        contract = create_general_contract()
        mocker.patch.object(BunkerRepository, '_get_bunker_client')
        mocker.patch.object(BunkerRepository, 'get', return_value=get_template_with_groups(allowed_groups)).start()
        response = self.test_client.secure_post(self.BASE_API, data=self.prepare_post_data(contract))
        assert_that(response.status_code, equal_to(http.OK if template_in_allowed_group else http.FORBIDDEN))
