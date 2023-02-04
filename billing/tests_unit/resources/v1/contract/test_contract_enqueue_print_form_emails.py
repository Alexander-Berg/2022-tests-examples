# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.utils import iteritems

standard_library.install_aliases()

import pytest
import pickle
import hamcrest as hm
import http.client as http
import datetime

from balance import constants as cst, mapper
from billing.contract_iface.contract_meta import collateral_types
from tests import object_builder as ob

from brest.core.tests import security
from brest.core.tests import utils as test_utils
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_role,
    create_view_client_role,
)
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_manager
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.firm import create_firm
from yb_snout_api.tests_unit.fixtures.common import not_existing_id


@pytest.fixture(name='alter_print_template_role')
def create_alter_print_template_role():
    return create_role(
        (
            cst.PermissionCode.ALTER_PRINT_TEMPLATE,
            {cst.ConstraintTypes.firm_id: None},
        ),
    )


@pytest.mark.smoke
class TestCaseContractEnqueuePrintFormEmails(TestCaseApiAppBase):
    BASE_API = u'/v1/contract/enqueue-print-form-emails'

    @staticmethod
    def _get_simple_extra_data():
        return {
            'email_from': 'from@yandex.ru',
            'email_to': 'to@yandex.ru',
            'email_to_client': False,
            'email_to_manager': False,
            'email_subject': 'Test',
            'email_body': 'nothing',
        }

    def test_contract_not_found(self):
        params = {
            'objects': [dict(object_type='contract', object_id=not_existing_id(ob.ContractBuilder))],
        }
        params.update(self._get_simple_extra_data())
        res = self.test_client.secure_post_json(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        hm.assert_that(res.get_json(), hm.has_entry('error', 'CONTRACT_NOT_FOUND'))

    def test_collateral_not_found(self):
        params = {
            'objects': [dict(object_type='collateral', object_id=not_existing_id(ob.CollateralBuilder))],
        }
        params.update(self._get_simple_extra_data())
        res = self.test_client.secure_post_json(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))

    @pytest.mark.parametrize('email_to_manager', [True, False])
    @pytest.mark.parametrize('email_to_client', [True, False])
    def test_enqueueing(self, email_to_manager, email_to_client, person, manager, firm):
        contract = create_general_contract(firm_id=firm.id, person=person)
        contract.col0.manager_code = manager.manager_code
        appended_cols = []
        for _ in range(3):
            appended_cols.append(
                contract.append_collateral(
                    dt=datetime.datetime.now(),
                    collateral_type=collateral_types['GENERAL'][1014],  # изменение бонусной программы
                    NDS=20,
                ),
            )

        self.test_session.flush()
        datetime_before_post = self.test_session.now()
        email_to_list = [ob.generate_character_string(20) for _ in range(3)]
        extra_data = {
            'email_from': ob.generate_character_string(20),
            'email_to': '; '.join(email_to_list),
            'email_to_client': email_to_client,
            'email_to_manager': email_to_manager,
            'email_subject': ob.generate_character_string(20),
            'email_body': ob.generate_character_string(20),
        }
        params = {
            'objects': [
                dict(object_type='contract', object_id=contract.id),
            ] + [
                dict(object_type='collateral', object_id=col.id)
                for col in appended_cols
            ],
        }
        params.update(extra_data)
        res = self.test_client.secure_post_json(
            self.BASE_API,
            params,
        )
        self.test_session.flush()
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        for col in [contract.col0] + appended_cols:
            hm.assert_that(
                col,
                hm.has_properties(
                    print_tpl_email=hm.equal_to(True),
                    print_tpl_email_to=hm.equal_to(extra_data['email_to']),
                    print_tpl_email_from=hm.equal_to(extra_data['email_from']),
                    print_tpl_email_client=hm.not_(0) if extra_data['email_to_client'] else 0,
                    print_tpl_email_manager=hm.not_(0) if extra_data['email_to_manager'] else 0,
                    print_tpl_email_subject=hm.equal_to(extra_data['email_subject']),
                    print_tpl_email_body=hm.equal_to(extra_data['email_body']),
                ),
            )

            email_message = self.test_session.query(mapper.EmailMessage).filter(
                mapper.EmailMessage.object_id == col.id,
            ).one()

            object_id, object_type = pickle.loads(email_message.data)
            opcode = email_message.opcode
            recepient_address = email_message.recepient_address.split(';')

            hm.assert_that(object_type, hm.is_in(['collateral', 'contract']))
            if object_type == 'collateral':
                hm.assert_that(object_id, hm.equal_to(col.id))
            if object_type == 'contract':
                hm.assert_that(object_id, hm.equal_to(col.contract.id))

            hm.assert_that(opcode, hm.equal_to(cst.PRINT_TEMPLATE_MESSAGE_CREATOR_OPCODE))

            additional_email_list = []
            if email_to_client:
                additional_email_list.append(person.email)
            if email_to_manager:
                additional_email_list.append(manager.email)
            hm.assert_that(recepient_address, hm.contains_inanyorder(*(additional_email_list + email_to_list)))

            hm.assert_that(len(col.print_tpl_email_log), hm.greater_than(0))
            datetime_iso_log, email_message_id_log = col.print_tpl_email_log[-1]
            hm.assert_that(email_message_id_log, hm.equal_to(email_message.id))
            hm.assert_that(datetime_before_post <= datetime.datetime.strptime(datetime_iso_log, "%Y-%m-%dT%H:%M:%S.%f"))

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_constraint, status_code',
        [
            (None, http.FORBIDDEN),
            (False, http.FORBIDDEN),
            (True, http.OK),
        ],
    )
    def test_permission(self, match_constraint, status_code, admin_role, alter_print_template_role, firm):
        roles = [admin_role]
        if match_constraint is not None:
            firm_id = firm.id if match_constraint else create_firm().id
            roles.append(
                (alter_print_template_role, {cst.ConstraintTypes.firm_id: firm_id}),
            )
        security.set_roles(roles)
        contract = create_general_contract(firm_id=firm.id)
        params = {
            'objects': [
                dict(object_type='contract', object_id=contract.id),
            ],
        }
        params.update(self._get_simple_extra_data())
        response = self.test_client.secure_post_json(
            self.BASE_API,
            params,
        )
        hm.assert_that(response.status_code, hm.equal_to(status_code))
        if match_constraint:
            hm.assert_that(contract.col0.print_tpl_email, hm.equal_to(True))
