# -*- coding: utf-8 -*-

import json
import hamcrest as hm
import pytest
import sqlalchemy as sa
from sqlalchemy import orm

from balance import mapper
from medium.crm_fallback_logic import CRMFallbackLogic

from tests import object_builder as ob


@pytest.fixture(name='crm_logic')
def create_crm_logic(session):
    sup_role = ob.RoleBuilder.construct(session, name=u"Тест саппорт")
    # Set any 10 permissions for this new role
    for perm in session.query(mapper.Permission).limit(10).all():
        role_perm = mapper.RolePermission(
            role=sup_role,
            permission=perm,
        )
        session.add(role_perm)

    res_role = ob.RoleBuilder.construct(session, name=u"Тест резервный саппорт")

    class TestLogic(CRMFallbackLogic):
        def __init__(self, sup_role_id, res_role_id, *args, **kwargs):
            self.main_role_id = sup_role_id
            self.reserved_role_id = res_role_id
            super(self.__class__, self).__init__(*args, **kwargs)

    return TestLogic(sup_role.id, res_role.id)


class TestCRMFallbackCase(object):
    @staticmethod
    def _check_simple_response(response, body):
        hm.assert_that(response, hm.has_properties(body=body, status_code=200))

    @staticmethod
    def _check_status(response, enabled):
        hm.assert_that(response, hm.has_properties(status_code=200))
        status = json.loads(response.body)
        hm.assert_that(status, hm.has_entries(enabled=enabled))

    @staticmethod
    def _check_config_value(session, crm_logic, expected_value):
        hm.assert_that(
            session.config.get(crm_logic.FALLBACK_INFO_KEY, column_name="value_json"),
            hm.equal_to(expected_value)
        )

    @staticmethod
    def _get_role_permissions(session, role_id):
        return (
            session.query(mapper.RolePermission.perm)
            .filter(mapper.RolePermission.role_id == role_id)
            .all()
        )

    def _check_equal_permissions(self, session, crm_logic):
        hm.assert_that(
            self._get_role_permissions(session, crm_logic.reserved_role_id),
            hm.contains_inanyorder(*self._get_role_permissions(session, crm_logic.main_role_id)),
        )

    def _check_empty_permissions(self, session, crm_logic):
        hm.assert_that(
            self._get_role_permissions(session, crm_logic.reserved_role_id),
            hm.empty(),
        )

    @staticmethod
    def _enable_manually(session, crm_logic):
        reason = {"woof": "no reason"}
        session.config.set(crm_logic.FALLBACK_INFO_KEY, reason, column_name="value_json")
        new_perm = ob.create_permission(session, ob.generate_character_string(10))
        session.add(mapper.RolePermission(perm=new_perm.perm, role_id=crm_logic.reserved_role_id))
        session.flush()

    @staticmethod
    def _disable_manually(session, crm_logic):
        session.config.set(crm_logic.FALLBACK_INFO_KEY, None, column_name="value_json")
        session.query(mapper.RolePermission).filter(
            mapper.RolePermission.role_id == crm_logic.reserved_role_id
        ).delete()

    def test_enable(self, session, crm_logic):
        self._disable_manually(session, crm_logic)

        reason = {
            "reason_type": "AUTO",
            "reason_text": "Idm delay 71s > 60s (threshold), <link to solomon>",
            "solomon_link": "<link to solomon>",
            "enabled_by": "<domain_login>",
        }

        # Enable two times for test
        self._check_simple_response(crm_logic.enable({}), "ok")
        self._check_simple_response(crm_logic.enable(reason), "ok")

        self._check_config_value(session, crm_logic, reason)
        self._check_status(crm_logic.status(None), "YES")
        self._check_equal_permissions(session, crm_logic)

    def test_disable(self, session, crm_logic):
        self._enable_manually(session, crm_logic)

        # Disable two times for test
        self._check_simple_response(crm_logic.disable(None), "ok")
        self._check_simple_response(crm_logic.disable(None), "ok")

        self._check_config_value(session, crm_logic, None)
        self._check_status(crm_logic.status(None), "NO")
        self._check_empty_permissions(session, crm_logic)

    @pytest.mark.parametrize('enabled', [False, True])
    def test_status(self, session, crm_logic, enabled):
        if enabled:
            self._enable_manually(session, crm_logic)
            self._check_status(crm_logic.status(None), "YES")

            # Remove config but keep permissions
            session.config.set(crm_logic.FALLBACK_INFO_KEY, None, column_name="value_json")
            self._check_status(crm_logic.status(None), "YES")
        else:
            self._disable_manually(session, crm_logic)
            self._check_status(crm_logic.status(None), "NO")

            # Add only permissions without config
            new_perm = ob.create_permission(session, ob.generate_character_string(10))
            session.add(mapper.RolePermission(perm=new_perm.perm, role_id=crm_logic.reserved_role_id))
            session.flush()
            self._check_status(crm_logic.status(None), "YES")
