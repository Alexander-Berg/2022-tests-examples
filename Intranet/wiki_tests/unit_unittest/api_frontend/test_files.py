from ujson import dumps, loads

import requests
from django.conf import settings
from mock import patch

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import encode_multipart_formdata, override_wiki_features
from wiki import access as wiki_access
from wiki.api_core.errors.permissions import UserHasNoAccess
from wiki.api_core.logic.files import EmptyFileError
from wiki.files.models import MDS_STORAGE, File


class APIFilesTest(BaseApiTestCase):
    def setUp(self):
        super(APIFilesTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def _upload_test_file(self, file_name, data):

        content = dumps(data)

        content_type, body = encode_multipart_formdata([('someField', 'somedata')], [('filefield', file_name, content)])

        response = self.client.post('/_api/frontend/.upload', data=body, content_type=content_type)

        self.assertEqual(200, response.status_code)
        resp_data = loads(response.content)['data']
        self.assertTrue('storage_id' in resp_data)

        return resp_data['storage_id']

    def test_files(self):
        page = self.create_page(tag='Файлы')

        request_url = '{api_url}/{page_supertag}/.files'.format(api_url=self.api_url, page_supertag=page.supertag)
        assert_queries = 50 if not settings.WIKI_CODE == 'wiki' else 8
        with self.assertNumQueries(assert_queries):
            r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']
        self.assertEqual(data['total'], 0)

        File(
            page=page, user=self.user_thasonic, name='ф.pdf', url='f.pdf', size=491425, description='A nice new file'
        ).save()

        File(
            page=page, user=self.user_thasonic, name='п.jpg', url='p.jpg', size=491425, description='A nice new file'
        ).save()

        File(
            page=page, user=self.user_thasonic, name='ват', url='wot', size=491425, description='A nice new file'
        ).save()

        r = self.client.get(request_url)
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertEqual(data['total'], 3)
        self.assertEqual(float(data['data'][0]['size']), 0.47)
        self.assertEqual(data['data'][0]['url'], '/fajjly/.files/wot')
        self.assertEqual(data['data'][0]['docviewer_url'], None)
        self.assertTrue(data['data'][0]['can_delete'])

        self.assertEqual(data['data'][1]['url'], '/fajjly/.files/p.jpg')
        # TODO убрать условие после https://st.yandex-team.ru/WIKI-9566
        if settings.IS_INTRANET:
            self.assertEqual(data['data'][1]['docviewer_url'], data['data'][1]['url'])

        self.assertEqual(data['data'][2]['url'], '/fajjly/.files/f.pdf')
        # TODO убрать условие после https://st.yandex-team.ru/WIKI-9566
        if settings.IS_INTRANET:
            self.assertEqual(
                data['data'][2]['docviewer_url'],
                'https://{0}/?url=ya-wiki%3A//{1}/fajjly/f.pdf'.format(settings.DOCVIEWER_HOST, settings.API_WIKI_HOST),
            )

    @override_wiki_features(mock_attach_download=False)
    def test_file(self):
        page = self.create_page(tag='Файлы')
        request_url = '{api_url}/{page_supertag}/.files/wot'.format(api_url=self.api_url, page_supertag=page.supertag)

        file = File(page=page, user=self.user_thasonic, name='wat', url='wot', description='A nice new file')
        file.save()

        with patch.object(requests, 'get', return_value=requests.Response()):
            response = self.client.get(request_url)
            self.assertEqual(200, response.status_code)
            self.assertEqual(response['Content-Type'], 'application/octet-stream;')
            self.assertEqual(response['Content-Disposition'], 'attachment; filename="wat"; filename*="UTF-8\'\'wat"')
            self.assertEqual(response['Expires'], file.modified_at.strftime('%a, %d %b %Y %H:%M:%S GMT'))

    def test_file_404_simple_file(self):
        self.create_page(tag='faily')

        request_url = '{api_url}/faily/.files/wot'.format(
            api_url=self.api_url,
        )

        response = self.client.get(request_url)
        self.assertEqual(404, response.status_code)
        self.assertEqual('text/html; charset=utf-8', response['Content-Type'])

    def test_file_404_page_does_not_exist(self):
        request_url = '{api_url}/faily/.files/wot'.format(
            api_url=self.api_url,
        )

        response = self.client.get(request_url)
        self.assertEqual(404, response.status_code)
        self.assertEqual('text/html; charset=utf-8', response['Content-Type'])

    def test_file_404_image(self):
        self.create_page(tag='faily')

        request_url = '{api_url}/faily/.files/image.png'.format(
            api_url=self.api_url,
        )

        response = self.client.get(request_url)
        self.assertEqual(404, response.status_code)
        self.assertEqual('image/png', response['Content-Type'])

    def test_file_404_image_page_does_not_exist(self):
        request_url = '{api_url}/faily/.files/image.png'.format(
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
            response = self.client.get('/_api/frontend/faily/.files/aaa.doc')
            self.assertEqual(403, response.status_code)
            self.assertEqual('image/png', response['Content-Type'])
            # какая-то картинка
            self.assertTrue(response.content)

    def test_file_upload(self):
        storage_id = self._upload_test_file('test.json', {'testKey': 'testValue'})

        MDS_STORAGE.delete(storage_id)

    def test_files_upload__empty(self):
        request_url = '{api_url}/.upload'.format(api_url=self.api_url)

        content_type, body = encode_multipart_formdata((), [('filefield', 'file_name', '')])
        response = self.client.post(request_url, data=body, content_type=content_type)

        self.assertEqual(409, response.status_code)
        self.assertEqual(response.json()['error']['message'], [str(EmptyFileError())])

    def test_no_files_upload(self):
        request_url = '{api_url}/.upload'.format(api_url=self.api_url)

        content_type, body = encode_multipart_formdata((('key', 'value'),), ())
        response = self.client.post(request_url, data=body, content_type=content_type)

        self.assertEqual(409, response.status_code)

    def test_attach_files(self):
        page = self.create_page(tag='Страница с файлами')

        storage_id1 = self._upload_test_file('почитать.txt', 'adsgdfg dfgsdfgsdfg sdfgsdfgsdfgsdfg asdfgsdfgsd')
        storage_id2 = self._upload_test_file('картинка.jpeg', '3452345 23452345 23452345234 5234523452354')

        data = {'files': [storage_id1, storage_id2]}

        request_url = '{api_url}/{page_supertag}/.attach'.format(api_url=self.api_url, page_supertag=page.supertag)
        assert_queries = 27 if not settings.WIKI_CODE == 'wiki' else 25
        with self.assertNumQueries(assert_queries):
            response = self.client.post(request_url, data=data)

        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['total'], 2)
        self.assertEqual(len(data['data']), 2)

        storage_id3 = self._upload_test_file('file2.txt', 'adsgdfg dfgsdfgsdfg sdfgsdfgsdfgsdfg asdfgsdfgsd')
        storage_id4 = self._upload_test_file('image2.jpeg', '3452345 23452345 23452345234 5234523452354')

        data = {'files': [storage_id3, storage_id4]}

        response = self.client.post(request_url, data=data)

        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['total'], 4)
        self.assertEqual(len(data['data']), 2)

        MDS_STORAGE.delete(storage_id1)
        MDS_STORAGE.delete(storage_id2)
        MDS_STORAGE.delete(storage_id3)
        MDS_STORAGE.delete(storage_id4)

    def test_attach_file_with_unknown_id(self):
        page = self.create_page(tag='Страница с файлами')

        data = {'files': ['wiki:file:readme.txt:126845:2014-04-09 20:51:46:59347321']}

        request_url = '{api_url}/{page_supertag}/.attach'.format(api_url=self.api_url, page_supertag=page.supertag)
        response = self.client.post(request_url, data=data)

        self.assertEqual(404, response.status_code)

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

        request_url = '{api_url}/{page_supertag}/.files/{url}'.format(
            api_url=self.api_url, page_supertag=page.supertag, url=file.url
        )

        assert_queries = 54 if not settings.WIKI_CODE == 'wiki' else 12
        with self.assertNumQueries(assert_queries):
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

        request_url = '{api_url}/{page_supertag}/.files/{url}'.format(
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

        request_url = '{api_url}/{page_supertag}/.files/{url}'.format(
            api_url=self.api_url, page_supertag=page.supertag, url=file.url
        )
        response = self.client.delete(request_url)
        self.assertEqual(403, response.status_code)
        self.assertEqual(File.active.filter(id=file_id).count(), 1)

    @patch('wiki.api_frontend.serializers.files.is_admin', lambda x: True)
    @patch('wiki.api_frontend.views.files.is_admin', lambda x: True)
    def test_admin_can_delete_foreign_file(self):
        page = self.create_page(tag='Страница с файлами')

        self.setGroupMembers()
        wiki_access.set_access(page, wiki_access.TYPES.COMMON, self.user_thasonic)

        file = File(
            page=page, user=self.user_kolomeetz, name='wat', url='wot', size=491425, description='A nice new file'
        )
        file.save()
        file_id = file.id

        self.client.login('volozh')

        request_url = '{api_url}/{page_supertag}/.files'.format(api_url=self.api_url, page_supertag=page.supertag)
        r = self.client.get(request_url)
        data = loads(r.content)['data']
        self.assertTrue(data['data'][0]['can_delete'])

        request_url = '{api_url}/{page_supertag}/.files/{url}'.format(
            api_url=self.api_url, page_supertag=page.supertag, url=file.url
        )
        response = self.client.delete(request_url)
        self.assertEqual(200, response.status_code)
        self.assertEqual(File.active.filter(id=file_id).count(), 0)

    def test_files_can_delete(self):
        page = self.create_page(tag='Файлы')

        request_url = '{api_url}/{page_supertag}/.files'.format(api_url=self.api_url, page_supertag=page.supertag)

        File(
            page=page, user=self.user_kolomeetz, name='wat', url='wot', size=491425, description='A nice new file'
        ).save()

        r = self.client.get(request_url)
        data = loads(r.content)['data']
        # права на удаление как у владельца страницы
        self.assertTrue(data['data'][0]['can_delete'])

        self.client.login('kolomeetz')
        r = self.client.get(request_url)
        data = loads(r.content)['data']
        # права на удаление как у того, кто загрузил файл
        self.assertTrue(data['data'][0]['can_delete'])

        self.client.login('volozh')
        r = self.client.get(request_url)
        data = loads(r.content)['data']
        # нет прав на удаление
        self.assertFalse(data['data'][0]['can_delete'])
