from django.test import override_settings

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class BaseSvcAPITestCase(BaseApiTestCase):
    api_url = '/_api/svc'

    def setUp(self):
        super(BaseSvcAPITestCase, self).setUp()
        self._settings_context = override_settings(AUTH_TEST_MECHANISM='tvm')
        self._settings_context.enable()

    def tearDown(self):
        super(BaseSvcAPITestCase, self).tearDown()
        self._settings_context.disable()
