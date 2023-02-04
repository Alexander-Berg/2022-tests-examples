
from django.conf import settings
from ujson import dumps, loads

from wiki.sync.connect.models import Organization
from wiki.org import org_ctx
from wiki.pages.logic import comment as comment_logic
from wiki.pages.logic.clone import clone_page
from wiki.pages.logic.edit import create
from wiki.pages.models import Page
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import encode_multipart_formdata


class ClonePageTest(BaseApiTestCase):
    def setUp(self):
        super(ClonePageTest, self).setUp()
        self.setUsers()
        self.client.login(self.user_chapson.username)

    def _create_page(self):
        self.page1 = create(
            tag='page1',
            user=self.user_chapson,
            body='body for page1',
            title='page1',
        )
        comment_logic.add_comment(user=self.user_chapson, page=self.page1, body='new comment')

    def _upload_test_file(self, file_name, data):
        content = dumps(data)
        content_type, body = encode_multipart_formdata([('someField', 'somedata')], [('filefield', file_name, content)])
        response = self.client.post('/_api/v1/files/', data=body, content_type=content_type)
        resp_data = loads(response.content)['data']
        return resp_data['storage_id']

    def test_clone_simple_page(self):
        org = None
        if settings.IS_BUSINESS:
            org = Organization.objects.create(dir_id='123', name='test org', label='test_org')

        with org_ctx(org):
            self._create_page()
            new_page = clone_page(self.page1, 'new_page', [self.user_chapson], self.user_chapson)

        self.assertNotEqual(self.page1.id, new_page.id)
        self.assertEqual(self.page1.mds_storage_id, new_page.mds_storage_id)
        self.assertEqual(self.page1.title, new_page.title)
        self.assertEqual(self.page1.body, new_page.body)
        self.assertEqual(new_page.supertag, 'newpage')
        self.assertEqual(new_page.tag, 'new_page')
        self.assertEqual(new_page.get_authors().count(), 1)
        self.assertTrue(self.user_chapson in new_page.get_authors())
        self.assertEqual(new_page.last_author, self.user_chapson)
        self.assertEqual(self.page1.org_id, new_page.org_id)
        self.assertEqual(new_page.revision_set.count(), 1)
        self.assertEqual(new_page.comments, 0)
        self.assertEqual(new_page.files, 0)
        self.assertEqual(self.page1.formatter_version, new_page.formatter_version)
        if settings.IS_BUSINESS:
            self.assertEqual(new_page.org.id, org.id)

    def test_clone_page_with_custom_params(self):
        self._create_page()
        new_page = clone_page(
            self.page1,
            'new_page',
            [self.user_chapson],
            self.user_chapson,
            title='new_title',
        )

        self.assertEqual(new_page.title, 'new_title')

    def test_clone_page_with_files(self):
        self._create_page()
        storage_id1 = self._upload_test_file('some_text.txt', 'sdfsdfsd')
        storage_id2 = self._upload_test_file('some_image.jpeg', '3452345')

        data = {'files': [storage_id1, storage_id2]}

        request_url = '{api_url}/{page_supertag}/.attach'.format(
            api_url='/_api/frontend', page_supertag=self.page1.supertag
        )
        self.client.post(request_url, data=data)

        self.page1 = Page.objects.get(id=self.page1.id)
        new_page = clone_page(self.page1, 'new_page', [self.user_chapson], self.user_chapson)
        self.assertEqual(new_page.files, 2)
