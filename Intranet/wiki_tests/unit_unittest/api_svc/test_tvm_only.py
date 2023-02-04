from django.test import override_settings

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class TvmPermissionTestCase(BaseApiTestCase):
    api_url = '/_api/svc'

    @override_settings(STRICT_TVM2=True)
    def test_must_ping(self):
        self.setUsers()
        self.client.login('thasonic').use_tvm2()
        request_url = '%s/.ping' % (self.api_url)
        response = self.client.post(request_url, {})
        self.assertEqual(response.status_code, 200)

    @override_settings(STRICT_TVM2=True)
    def test_must_403(self):
        self.setUsers()
        self.client.login('thasonic').use_cookie_auth()  # "use_cookie_auth" because tvm is default
        request_url = '%s/.ping' % (self.api_url)
        response = self.client.post(request_url, {})
        self.assertEqual(response.status_code, 403)
