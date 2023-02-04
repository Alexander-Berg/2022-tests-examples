# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from hamcrest import assert_that, equal_to

from brest.core.tests import utils as test_utils
from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class TestCaseWsgi(TestCaseApiAppBase):
    def test_ping(self, mocker):
        from butils.dbhelper.session import SessionBase
        from brest.core.application import create_wsgi_app

        start_response_mock = mocker.Mock()

        environ = {
            'SCRIPT_NAME': '/ping',
        }
        session = test_utils.get_test_session()

        dual_table_mock = mocker.patch.object(
            SessionBase,
            'dual_table',
            new_callable=mocker.PropertyMock,
            return_value=session.dual_table,
        )

        # noinspection PyTypeChecker
        dispatcher = create_wsgi_app(None)
        response = dispatcher(environ, start_response_mock.method)

        dual_table_mock.assert_called_once()
        start_response_mock.method.assert_called_once_with(
            '200 OK',
            [('Content-Type', 'text/plain')],
        )

        assert_that(dual_table_mock(), equal_to(session.dual_table))
        assert_that(response, equal_to(['SUCCESS']))
