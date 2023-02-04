
from json import dumps, loads
from unittest import skip

from mock import patch

from wiki import access as wiki_access
from wiki.api_core.errors.permissions import UserHasNoAccess
from wiki.files.models import MDS_STORAGE, File
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import encode_multipart_formdata


class APIv1FilesTest(BaseApiTestCase):
    def setUp(self):
        super(APIv1FilesTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.api_url = '/_api/v1'

    def _upload_test_file(self, file_name, data):
        content = dumps(data)

        content_type, body = encode_multipart_formdata([('someField', 'somedata')], [('filefield', file_name, content)])

        response = self.client.post('/_api/v1/files', data=body, content_type=content_type)

        self.assertEqual(200, response.status_code)
        resp_data = loads(response.content)['data']
        self.assertTrue('storage_id' in resp_data)

        return resp_data['storage_id']

    @skip('WIKI-9427')
    def test_file(self):
        page = self.create_page(tag='Файлы')

        request_url = '{api_url}/pages/{page_supertag}/.files/wot'.format(
            api_url=self.api_url, page_supertag=page.supertag
        )

        file = File(page=page, user=self.user_thasonic, name='wat', url='wot', description='A nice new file')
        file.save()

        # no storage_url
        resp = self.client.get(request_url)
        self.assertEqual(404, resp.status_code)

        # fake storage_url
        with patch.object(MDS_STORAGE, 'url', new=lambda _: 'http://127.0.0.1:88/faaake'):
            file.mds_storage_id = 'faaake'
            file.save()
            response = self.client.get(request_url)
            self.assertEqual(200, response.status_code)
            self.assertTrue(response.has_header('X-Accel-Redirect'))
            self.assertEqual(response['X-Accel-Redirect'], '/.storage/?fileurl=http://127.0.0.1:88/faaake')

    def test_file_404_simple_file(self):
        self.create_page(tag='faily')

        request_url = '{api_url}/pages/faily/.files/wot'.format(api_url=self.api_url)

        response = self.client.get(request_url)
        self.assertEqual(404, response.status_code)
        self.assertEqual('text/html; charset=utf-8', response['Content-Type'])

    def test_file_404_page_does_not_exist(self):
        request_url = '{api_url}/pages/faily/.files/wot'.format(
            api_url=self.api_url,
        )

        response = self.client.get(request_url)
        self.assertEqual(404, response.status_code)
        self.assertEqual('text/html; charset=utf-8', response['Content-Type'])

    def test_file_404_image(self):
        self.create_page(tag='faily')

        request_url = '{api_url}/pages/faily/.files/image.png'.format(
            api_url=self.api_url,
        )

        response = self.client.get(request_url)
        self.assertEqual(404, response.status_code)
        self.assertEqual('image/png', response['Content-Type'])

    def test_file_404_image_page_does_not_exist(self):
        request_url = '{api_url}/pages/faily/.files/image.png'.format(
            api_url=self.api_url,
        )

        response = self.client.get(request_url)
        self.assertEqual(404, response.status_code)
        # тут ошибка не важна на самом деле, страницы нет и файла нет.
        # какая разница будет ли в ответе картинка? Пусть не будет, чтобы
        # писать меньше кода.
        self.assertEqual('text/html; charset=utf-8', response['Content-Type'])

    def test_403(self):
        self.create_page(tag='faily')

        def raise403(*args, **kwargs):
            raise UserHasNoAccess

        with patch(target='wiki.api_frontend.views.FileView.check_page_access', new=raise403):
            response = self.client.get('/_api/frontend/faily/.files/aaa.jpg')
            self.assertEqual(403, response.status_code)
            self.assertEqual('image/png', response['Content-Type'])
            # какая-то картинка
            self.assertTrue(response.content)

            # даже если не похоже на картинку. Все равно картинка!
            response = self.client.get('{0}/pages/faily/.files/aaa.doc'.format(self.api_url))
            self.assertEqual(403, response.status_code)
            self.assertEqual('image/png', response['Content-Type'])
            # какая-то картинка
            self.assertTrue(response.content)

    def test_delete_my_file(self):
        page = self.create_page(tag='Страница с файлами')

        self.setGroupMembers()
        wiki_access.set_access(page, wiki_access.TYPES.COMMON, self.user_thasonic)

        file = File(
            page=page, user=self.user_kolomeetz, name='wat', url='wot', size=491425, description='A nice new file'
        )
        file.save()
        file_id = file.id

        self.client.login('kolomeetz')

        request_url = '{api_url}/pages/{page_supertag}/.files/{url}'.format(
            api_url=self.api_url, page_supertag=page.supertag, url=file.url
        )
        response = self.client.delete(request_url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(File.active.filter(id=file_id).count(), 0)

    def test_delete_file_on_my_page(self):
        page = self.create_page(tag='Страница с файлами')

        file = File(
            page=page, user=self.user_kolomeetz, name='wat', url='wot', size=491425, description='A nice new file'
        )
        file.save()
        file_id = file.id

        request_url = '{api_url}/pages/{page_supertag}/.files/{url}'.format(
            api_url=self.api_url, page_supertag=page.supertag, url=file.url
        )
        response = self.client.delete(request_url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(File.active.filter(id=file_id).count(), 0)

    def test_cant_delete_foreign_file(self):
        page = self.create_page(tag='Страница с файлами')

        # Даем права на страницу, файла все равно не должно дать удалить
        self.setGroupMembers()
        wiki_access.set_access(page, wiki_access.TYPES.COMMON, self.user_thasonic)

        file = File(
            page=page, user=self.user_kolomeetz, name='wat', url='wot', size=491425, description='A nice new file'
        )
        file.save()
        file_id = file.id

        self.client.login('volozh')

        request_url = '{api_url}/pages/{page_supertag}/.files/{url}'.format(
            api_url=self.api_url, page_supertag=page.supertag, url=file.url
        )
        response = self.client.delete(request_url)
        self.assertEqual(403, response.status_code)
        self.assertEqual(File.active.filter(id=file_id).count(), 1)

    def test_change_file(self):
        page = self.create_page(tag='testpage')
        file = File(
            page=page, user=self.user_kolomeetz, name='wat', url='wot', size=491425, description='A nice new file'
        )
        file.save()

        request_url = self.api_url + '/pages/testpage/.files/wot'

        content_type, body = encode_multipart_formdata(
            [('description', 'brand new file')], [('filefield', 'filename', 'mycontent')]
        )

        response = self.client.post(request_url, body, content_type=content_type)

        self.assertEqual(200, response.status_code)
        file = File.active.get(id=file.id)
        self.assertEqual('brand new file', file.description)
        self.assertEqual(b'mycontent', file.mds_storage_id.read())
