# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from builtins import int
from future import standard_library

standard_library.install_aliases()

import pytest
from hamcrest import assert_that, equal_to, none

from brest.core.tests.utils import real_session_ctx
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from brest.core.tests.fixtures.security import generate_oper_id
from yb_snout_api.utils import deco


@pytest.mark.skip('Understand what is had to be mocked')
class TestCaseDeco(TestCaseApiAppBase):
    def test_deco_create_new_read_only_session(self, mocker, oper_id):
        from yb_snout_api.utils.db import DatabaseId

        # noinspection PyShadowingNames
        class FakeResource(object):
            session_ro_database_id = property(lambda self: DatabaseId.BALANCE_RO)

            def get(self):
                pass

            def dispatch_request(self):
                method = deco.check_auth(deco.create_new_read_only_session(self.get))

                return method()

        flask_app = self._get_flask_app()

        mocker.patch('yb_snout_api.utils.deco.auth.cookie.get_balance_cookie', return_value={
            'SUBST_ID': oper_id,
        })

        with flask_app.test_request_context('/fake'):
            from flask import g

            resource = FakeResource()

            with real_session_ctx():
                resource.dispatch_request()

            assert_that(g.subst_oper_id, equal_to(int(oper_id)))
            assert_that(int(oper_id), equal_to(resource.session_ro.oper_id))
            assert_that(resource.session_ro.transaction, none())
