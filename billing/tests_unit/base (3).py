# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from brest.core.tests.base import TestCaseAppBase


class TestCaseProxyAppBase(TestCaseAppBase):
    def _get_flask_app(self):
        import yb_snout_proxy.servant as servant

        return servant.flask_app

    def get_passport_id(self):
        return 1234567890
