# -*- coding: utf-8 -*-
import pytest

from balance import (
    constants as cst,
    exc,
)
from muzzle.api import user as user_api
from tests import object_builder as ob


@pytest.fixture(name='view_invoices_role')
def create_view_invoices_role(session, firm_id=None):
    return ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: firm_id}))


@pytest.mark.permissions
class TestCheckPermConstraints(object):
    @pytest.mark.parametrize(
        'classname, res, description',
        [
            ('FailClassName', exc.INVALID_PARAM,
             'Invalid parameter for function: Wrong classname=FailClassName for checking permission'),
            ('Invoice', exc.NOT_FOUND, 'Invoice with ID -1 not found in DB'),
        ],
        ids=['wrong_classname', 'wrong_object_id'],
    )
    def test_wrong_object_params(self, session, classname, res, description):
        with pytest.raises(res) as excinfo:
            user_api.check_perm_constraints(
                session=session,
                classname=classname,
                obj_id=-1,
                perm=cst.PermissionCode.ADMIN_ACCESS,
                strictness=False
            )
        assert excinfo.value.msg == description

    def test_wo_perm(self, session):
        """У пользователя нет нужного права"""
        ob.set_roles(session, session.passport, [])
        res = user_api.check_perm_constraints(
            session=session,
            perm=cst.PermissionCode.VIEW_INVOICES,
            strictness=False,
        )
        assert res is False

        with pytest.raises(exc.PERMISSION_DENIED):
            user_api.check_perm_constraints(
                session=session,
                perm=cst.PermissionCode.VIEW_INVOICES,
                strictness=True,
            )

    @pytest.mark.parametrize(
        'firm_id, req_res',
        [
            (cst.FirmId.YANDEX_OOO, True),
            (cst.FirmId.CLOUD, False),
        ],
    )
    def test_constraints(self, session, firm_id, req_res):
        """Проверяем ограничения права"""
        role = create_view_invoices_role(session)
        ob.set_roles(session, session.passport, [(role, {cst.ConstraintTypes.firm_id: firm_id})])
        invoice = ob.InvoiceBuilder.construct(session, firm_id=cst.FirmId.YANDEX_OOO)
        res = user_api.check_perm_constraints(
            session=session,
            perm=cst.PermissionCode.VIEW_INVOICES,
            strictness=False,
            classname='Invoice',
            obj_id=invoice.id,
        )
        assert res == req_res
