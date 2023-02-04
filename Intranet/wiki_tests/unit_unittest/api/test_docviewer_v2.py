from unittest import skipIf

import requests
from django.conf import settings
from django.urls import reverse
from django.test import override_settings
from mock import patch

from wiki.files.models import File
from wiki.intranet.models import Staff
from wiki.pages.models import Page
from wiki.utils.docviewer import file_id
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get

auth_settings = {
    'AUTH_TEST_MECHANISM': 'tvm',
    'AUTH_TEST_TVM_CLIENT_ID': settings.DOCVIEWER_TVM2_CLIENT_ID,
}


@override_settings(**auth_settings)
class DocviewerV2Test(BaseTestCase):
    def setUp(self):
        super(DocviewerV2Test, self).setUp()
        self.setPages()
        self.setUsers()
        self.chapson_uid = '112000000000538'
        Staff.objects.filter(login='chapson').update(uid=self.chapson_uid)
        self.client.login(self.user_thasonic)
        self.page = Page.objects.get(supertag='testinfo')

    def _get_file(self, page):
        return get(File, url='file', user=self.user_thasonic, page=page, name='File to be downloaded by docviewer')

    def test_not_tvm_auth_mechanism(self):
        self.client.use_cookie_auth()
        file = self._get_file(self.page)

        uri = reverse('docviewer_download_with_tvm') + '?fileid=%s' % file_id(self.page.supertag, file.url)

        response = self.client.get(uri, **{'REQUEST_URI': '/meaningless_uri/'})
        self.assertEqual(response.status_code, 403)

    def test_not_docviewer_tvm2_client_id(self):
        self.client.use_tvm2(client_id='12345')
        file = self._get_file(self.page)

        uri = reverse('docviewer_download_with_tvm') + '?fileid=%s' % file_id(self.page.supertag, file.url)

        response = self.client.get(uri, **{'REQUEST_URI': '/meaningless_uri/'})
        self.assertEqual(response.status_code, 403)

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_download_v2_file(self):
        self.client.use_tvm2(client_id=auth_settings['AUTH_TEST_TVM_CLIENT_ID'])
        file = self._get_file(self.page)

        Staff.objects.filter(login='chapson').update(uid=self.chapson_uid)
        uri = reverse('docviewer_download_with_tvm') + '?fileid=%s' % file_id(self.page.supertag, file.url)

        with patch.object(requests, 'get', return_value=requests.Response()):
            response = self.client.get(uri, **{'REQUEST_URI': '/meaningless_uri/'})
            self.assertEqual(response.status_code, 200)
            self.assertEqual(
                response['Content-Disposition'],
                'attachment; filename="File to be downloaded by docviewer";'
                ' filename*="UTF-8\'\'File%20to%20be%20downloaded%20by%20docviewer"',
            )
