# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from brest.core.tests import security
from brest.core.tests.base import TestCaseAppBase

BALANCE_SUPPORT_ROLE_ID = 0


class TestCaseApiAppBase(TestCaseAppBase):

    def setup_method(self):
        from balance import mapper

        super(TestCaseApiAppBase, self).setup_method()

        role = self.test_session.query(mapper.Role).get(BALANCE_SUPPORT_ROLE_ID)
        security.set_roles([role])

    @staticmethod
    def _get_flask_app():
        import yb_snout_api.servant as servant

        return servant.flask_app

    def get_passport_id(self):
        return self.test_session.oper_id
