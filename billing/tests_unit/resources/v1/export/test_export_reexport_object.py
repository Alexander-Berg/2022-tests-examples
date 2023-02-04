# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import http.client as http
from balance import constants as cst, mapper
from hamcrest import assert_that, equal_to, has_entries

from brest.core.tests import security
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.resources.enums import ExportState
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_role_client, create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person


@pytest.fixture(name='reexport_inv')
def create_reexport_inv():
    return create_role(
        (
            cst.PermissionCode.OEBS_REEXPORT_INVOICE,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='reexport_act')
def create_reexport_act():
    return create_role(
        (
            cst.PermissionCode.OEBS_REEXPORT_ACT,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='reexport_person')
def create_reexport_person():
    return create_role((cst.PermissionCode.OEBS_REEXPORT_PERSON, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='reexport_contract')
def create_reexport_contract():
    return create_role(
        (
            cst.PermissionCode.OEBS_REEXPORT_CONTRACT,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


class TestCaseExportReexportObject(TestCaseApiAppBase):
    BASE_API = u'/v1/export/reexport-object'
    EXPORT_TYPE = 'OEBS'

    def test_reexport_person(self, person):
        from yb_snout_api.resources.v1.export import enums

        session = self.test_session

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'object_name': enums.ReexportInOEBSType.PERSON.name,
                'object_id': person.id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        export = session.query(mapper.Export).getone(
            classname='Person',
            type=self.EXPORT_TYPE,
            object_id=person.id,
        )
        assert_that(export.state, equal_to(ExportState.WAITING.value))

    def test_reexport_invoice(self, invoice):
        from yb_snout_api.resources.v1.export import enums

        session = self.test_session

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'object_name': enums.ReexportInOEBSType.INVOICE.name,
                'object_id': invoice.id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        export = session.query(mapper.Export).getone(
            classname='Invoice',
            type=self.EXPORT_TYPE,
            object_id=invoice.id,
        )
        assert_that(export.state, equal_to(ExportState.WAITING.value))

    def test_object_not_exists(self):
        from yb_snout_api.resources.v1.export import enums

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'object_name': enums.ReexportInOEBSType.PERSON.name,
                'object_id': -1234,
            },
        )
        assert_that(response.status_code, equal_to(http.NOT_FOUND))
        assert_that(
            response.get_json(),
            has_entries({
                'error': 'PERSON_NOT_FOUND',
                'description': 'Person with ID -1234 not found in DB',
            }),
        )

    @pytest.mark.parametrize(
        'object_name, obj_func, parameter_func',
        [
            pytest.param('INVOICE', create_invoice, lambda o: setattr(o, 'hidden', 2),
                         id='hidden invoice'),
            pytest.param('INVOICE', create_invoice, lambda o: setattr(o, 'status_id', 5),
                         id='wrong status_id of invoice'),
            pytest.param('PERSON', create_person, lambda o: setattr(o, 'hidden', 1),
                         id='hidden person'),
            pytest.param('ACT', create_act, lambda o: setattr(o, 'hidden', 4),
                         id='hidden act'),
        ],
    )
    def test_check_filtration(self, object_name, obj_func, parameter_func):
        obj = obj_func()
        parameter_func(obj)
        self.test_session.flush()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'object_name': object_name,
                'object_id': obj.id,
            },
        )
        assert_that(response.status_code, equal_to(http.BAD_REQUEST))
        assert_that(
            response.get_json(),
            has_entries({
                'error': 'BAD_REQUEST',
                'description': 'Invalid parameter for function: Wrong reexport object',
            }),
        )


@pytest.mark.permissions
@mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
class TestExportReexportObjectPermissions(TestCaseApiAppBase):
    BASE_API = u'/v1/export/reexport-object'
    EXPORT_TYPE = 'OEBS'

    @pytest.mark.parametrize(
        'object_name, create_obj_func, create_role_func',
        [
            pytest.param('PERSON', create_person, create_reexport_person, id='person'),
            pytest.param('INVOICE', create_invoice, create_reexport_inv, id='invoice'),
            pytest.param('ACT', create_act, create_reexport_act, id='act'),
            pytest.param('CONTRACT', create_general_contract, create_reexport_contract, id='contract'),
        ],
    )
    @pytest.mark.parametrize(
        'w_role, res',
        [
            pytest.param(False, http.FORBIDDEN, id='wo role'),
            pytest.param(True, http.OK, id='w role'),
        ],
    )
    def test_wo_constraints(
            self,
            object_name,
            create_obj_func,
            admin_role,
            create_role_func,
            w_role,
            res,
    ):
        roles = [admin_role]
        if w_role:
            roles.append(create_role_func())

        security.set_roles(roles)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'object_name': object_name,
                'object_id': create_obj_func().id,
            },
        )
        assert_that(response.status_code, equal_to(res))

    @pytest.mark.parametrize(
        'object_name, create_obj_func, create_role_func',
        [
            pytest.param('INVOICE', create_invoice, create_reexport_inv, id='invoice'),
            pytest.param('ACT', create_act, create_reexport_act, id='act'),
            pytest.param('CONTRACT', create_general_contract, create_reexport_contract, id='contract'),
        ],
    )
    @pytest.mark.parametrize(
        'role_firm_id, res',
        [
            pytest.param(None, http.OK, id='role wo constraints'),
            pytest.param(cst.FirmId.YANDEX_OOO, http.OK, id='role constraints matches firm_id'),
            pytest.param(cst.FirmId.CLOUD, http.FORBIDDEN, id='role constraints don\'t matches firm_id'),
        ],
    )
    def test_w_constraint_firm_id(
            self,
            admin_role,
            object_name,
            create_obj_func,
            create_role_func,
            role_firm_id,
            res,
    ):
        base_firm_id = cst.FirmId.YANDEX_OOO

        roles = [admin_role]
        if role_firm_id is not cst.SENTINEL:
            roles.append((create_role_func(), {cst.ConstraintTypes.firm_id: role_firm_id}))

        security.set_roles(roles)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'object_name': object_name,
                'object_id': create_obj_func(firm_id=base_firm_id).id,
            },
        )
        assert_that(response.status_code, equal_to(res))

    @pytest.mark.parametrize(
        'object_name, create_obj_func, create_role_func',
        [
            pytest.param('PERSON', create_person, create_reexport_person, id='person'),
            pytest.param('INVOICE', create_invoice, create_reexport_inv, id='invoice'),
            pytest.param('ACT', create_act, create_reexport_act, id='act'),
            pytest.param('CONTRACT', create_general_contract, create_reexport_contract, id='contract'),
        ],
    )
    @pytest.mark.parametrize(
        'match_client, res',
        [
            pytest.param(True, http.OK, id='match client'),
            pytest.param(False, http.FORBIDDEN, id='not match client'),
        ],
    )
    def test_w_constraint_client(
            self,
            client,
            admin_role,
            object_name,
            create_obj_func,
            create_role_func,
            match_client,
            res,
    ):
        client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
        roles = [
            admin_role,
            (create_role_func(), {cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        obj = create_obj_func(client=client)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'object_name': object_name,
                'object_id': obj.id,
            },
        )
        assert_that(response.status_code, equal_to(res))
