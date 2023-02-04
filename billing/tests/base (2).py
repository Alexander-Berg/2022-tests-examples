# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future.backports.urllib.parse import urlencode
from future import standard_library

standard_library.install_aliases()

import logging
from abc import ABCMeta, abstractmethod

import collections
import allure
import six
import json
from flask.testing import FlaskClient

from brest.core.tests.yb_test_app import create_app
# noinspection PyUnresolvedReferences
from brest.core.tests.fixtures.security import generate_yandex_uid

from . import utils

LOGGER = logging.getLogger()

yb_test_app = create_app()


class TestClient(FlaskClient):

    @staticmethod
    def _set_header_is_admin(headers):
        headers['X-Is-Admin'] = True

    def get(self, url, params=None, is_admin=True, **kw):
        if params is None:
            params = {}
        assert \
            (
                isinstance(params, dict) or
                (isinstance(params, collections.Sequence) and all(isinstance(t, tuple) and len(t) == 2 for t in params))
            ), \
            u'Params must be dict or sequence of two element tuples'

        if params:
            url += u'?' + urlencode(params, encoding='utf-8')

        if is_admin:
            kw.setdefault('headers', {})
            self._set_header_is_admin(kw['headers'])

        return super(TestClient, self).get(url, **kw)

    def secure_post(self, url, data, is_admin=True, as_json=False, **kw):
        """
        Post request with secure data
        """
        session = yb_test_app.new_session()

        kw.setdefault('headers', {})
        if is_admin:
            self._set_header_is_admin(kw['headers'])

        cookies = kw.pop('cookies', {})
        for k,v in cookies.items():
            self.set_cookie('', k, v)

        data = utils.with_csrf(
            self,
            session.oper_id,
            cookies.get('yandexuid', generate_yandex_uid()),
            data,
        )

        if as_json:
            kw['headers']['Content-Type'] = 'application/json'
            data = json.dumps(data)

        return self.post(
            url,
            data=data,
            **kw  # noqa: C815
        )

    def secure_post_json(self, url, data, is_admin=True, **kw):
        """
        Post request with secure data
        """
        return self.secure_post(url, data, is_admin, as_json=True, **kw)


# noinspection PyAttributeOutsideInit
@six.add_metaclass(ABCMeta)
class TestCaseAppBase(object):
    # TODO: replace with pytest fixtures
    def setup_method(self):
        """
        Note that this code will be run before each test.

        :see: https://docs.pytest.org/en/latest/xunit_setup.html#method-and-function-level-setup-teardown
        """
        from brest.core.auth.direct import DirectAuthMethod
        from . import security

        flask_app = self._get_flask_app()
        flask_app.testing = True
        flask_app.test_client_class = TestClient

        self.test_client = flask_app.test_client()

        passport_id = self.get_passport_id()
        LOGGER.info('Patching auth methods, using "DirectAuthMethod" with uid: %s', passport_id)
        security.set_auth_methods(flask_app, [DirectAuthMethod(passport_id)])

    # noinspection PyMethodMayBeStatic
    def teardown_method(self):
        yb_test_app.cleanup_session()

    @classmethod
    @allure.step('get test session')
    def get_test_session(cls):
        return yb_test_app.new_session()

    @property
    def test_session(self):
        return yb_test_app.new_session()

    def get_passport_id(self):
        raise NotImplementedError

    @abstractmethod
    def _get_flask_app(self):
        """
        :return: Flask app instance specific to the current package (proxy/api)
        """
        pass
