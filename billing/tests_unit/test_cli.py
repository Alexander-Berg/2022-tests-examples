# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import json

from hamcrest import assert_that, has_key

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


@pytest.mark.skip('Nobody uses cli')
class TestCaseCli(TestCaseApiAppBase):
    def test_debug_dump(self):
        from yb_snout_api import cli

        flask_app = self._get_flask_app()

        runner = flask_app.test_cli_runner()

        result_raw = runner.invoke(cli.debug_dump_command)
        result_json = json.loads(result_raw.output)

        assert_that(result_json, has_key('version'))
        assert_that(result_json, has_key('data'))
