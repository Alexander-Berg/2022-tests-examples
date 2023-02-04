
import requests
from django.urls import reverse
from mock import patch

from wiki.files.models import File
from wiki.intranet.models import Staff
from wiki.pages.models import Page
from wiki.utils.docviewer import file_id
from wiki.utils.supertag import translit
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get

__author__ = 'chapson'

from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class DeprecatedDocviewerTest(BaseTestCase):
    def setUp(self):
        super(DeprecatedDocviewerTest, self).setUp()
        self.setPages()
        self.setUsers()
        self.chapson_uid = '112000000000538'
        Staff.objects.filter(login='chapson').update(uid=self.chapson_uid)

    def _create_file(self, page, filename):
        filename = translit(filename)
        return get(File, url=filename, user=self.user_thasonic, page=page, name='File to be downloaded by docviewer')

    def test_download_file(self):
        for filename in ['file', 'файл']:
            page = Page.objects.get(supertag='testinfo')
            self._create_file(page, filename)

            uri = reverse('docviewer_download_deprecated') + '?fileid=%s' % file_id(page.supertag, filename)

            response = self.client.get(uri + '&uid=0')
            self.assertEqual(response.status_code, 403)

            with patch.object(requests, 'get', return_value=requests.Response()):
                response = self.client.get(uri + '&uid=' + self.chapson_uid, **{'REQUEST_URI': '/meaningless_uri/'})
                self.assertEqual(response.status_code, 200)
                self.assertEqual(
                    response['Content-Disposition'],
                    'attachment; filename="File to be downloaded by docviewer";'
                    ' filename*="UTF-8\'\'File%20to%20be%20downloaded%20by%20docviewer"',
                )

    def test_download_file_through_redirect_page(self):
        page = Page.objects.get(supertag='destination/testinfo/testinfogem')
        redirect = Page.objects.get(supertag='testinfo/redirectpage')
        filename = 'file'
        self._create_file(page, filename)
        uri = reverse('docviewer_download_deprecated') + '?fileid=%s' % file_id(redirect.supertag, filename)

        with patch.object(requests, 'get', return_value=requests.Response()):
            response = self.client.get(uri + '&uid=' + self.chapson_uid, **{'REQUEST_URI': '/meaningless_uri/'})
            self.assertEqual(response.status_code, 200)
            self.assertEqual(
                response['Content-Disposition'],
                'attachment; filename="File to be downloaded by docviewer";'
                ' filename*="UTF-8\'\'File%20to%20be%20downloaded%20by%20docviewer"',
            )
