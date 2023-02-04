
from datetime import datetime

from django.conf import settings
from django.test.utils import override_settings
from mock import Mock, patch
from pretend import stub
from ujson import loads

from wiki.org import get_user_orgs
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import encode_multipart_formdata


@override_settings(WIKI_PROTOCOL='http', NGINX_HOST='wiki', DOCVIEWER_HOST='docviewer')
class AttachFilesTest(BaseApiTestCase):
    def setUp(self):
        super(AttachFilesTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    @patch(target='wiki.api_v1.views.serializers.files.MDS_STORAGE.exists', new=lambda storage_id: True)
    @patch(
        target='wiki.api_v1.views.serializers.files.fileinfo_by_storage_id',
        new=lambda storage_id: ('myfilename.doc', 10000),
    )
    @patch(target='wiki.api_v1.views.serializers.files.now', new=lambda: datetime(2014, 8, 12, 17, 14, 15))
    def test_attach_files(self):
        self.create_page(tag='filespages')

        response = self.client.post(
            '/_api/v1/pages/filespages/.files',
            {
                'files': [
                    {'storage_id': 'foo', 'description': 'My text file'},
                    {'storage_id': 'bar', 'description': 'Another text file'},
                ]
            },
        )

        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        org = get_user_orgs(self.user_thasonic)[0]
        expected = {
            'files': [
                {
                    'description': 'My text file',
                    'docviewer_url': 'https://docviewer/?url=ya-wiki%3A//{0}/filespages/myfilename.doc{1}'.format(
                        settings.API_WIKI_HOST, '%3F' + org.dir_id if org else ''
                    ),
                    'name': 'myfilename.doc',
                    'size': 0.01,
                    'upload_date': '2014-08-12T17:14:15',
                    'url': 'http://wiki/filespages/.files/myfilename.doc',
                    'user_name': 'thasonic',
                },
                {
                    'description': 'Another text file',
                    'docviewer_url': 'https://docviewer/?url=ya-wiki%3A//{0}/filespages/myfilename-1.doc{1}'.format(
                        settings.API_WIKI_HOST, '%3F' + org.dir_id if org else ''
                    ),
                    'name': 'myfilename.doc',
                    'size': 0.01,
                    'upload_date': '2014-08-12T17:14:15',
                    'url': 'http://wiki/filespages/.files/myfilename-1.doc',
                    'user_name': 'thasonic',
                },
            ],
            'links': None,
        }
        self.maxDiff = None
        self.assertDictEqual(data, expected)

    @patch(target='wiki.api_v1.views.serializers.files.MDS_STORAGE.exists', new=lambda storage_id: False)
    def test_attach_file_with_unknown_id(self):
        self.create_page(tag='filespages')
        response = self.client.post(
            '/_api/v1/pages/filespages/.files',
            data={
                'files': [
                    {
                        'storage_id': 'wiki:file:readme.txt',
                    }
                ]
            },
        )
        self.assertEqual(409, response.status_code)
        data = loads(response.content)['error']
        self.assertEqual(data['message'], ['No storage_id "wiki:file:readme.txt" in storage'])

    @patch(target='wiki.api_v1.views.serializers.files.now', new=lambda: datetime(2014, 8, 12, 17, 14, 15))
    def test_put_single_file(self):
        self.create_page(tag='filespages')
        content_type, body = encode_multipart_formdata(
            [
                ('description', 'Another text file'),
                ('name', 'myfilename.doc'),
            ],
            [('filefield', 'file_file.txt', 'file-contents')],
        )
        response = self.client.put('/_api/v1/pages/filespages/.files', data=body, content_type=content_type)
        self.assertEqual(response.status_code, 200)
        content = response.data['data']
        self.assertDictEqual(
            content,
            {
                'name': 'file_file.txt',
                'url': 'http://wiki/filespages/.files/filefile.txt',
                'size': 0.0,
                'description': 'Another text file',
                'user_name': 'thasonic',
                'docviewer_url': None,
                'upload_date': '2014-08-12T17:14:15',
            },
        )


class DeleteFilesTest(BaseApiTestCase):
    def test_delete_my_file(self):
        from wiki import access as wiki_access
        from wiki.files.models import File

        self.setGroupMembers()
        page = self.create_page(tag='filespage')

        wiki_access.set_access(page, wiki_access.TYPES.COMMON, self.user_thasonic)

        file = File(
            page=page, user=self.user_kolomeetz, name='wat', url='wot', size=491425, description='A nice new file'
        )
        file.save()
        file_id = file.id

        self.client.login('kolomeetz')

        response = self.client.delete('/_api/v1/pages/filespage/.files/wot')
        self.assertEqual(200, response.status_code)
        self.assertEqual(File.active.filter(id=file_id).count(), 0)

    def test_delete_file_on_my_page(self):
        from wiki.files.models import File

        self.setUsers()
        page = self.create_page(tag='filespage')

        file = File(
            page=page, user=self.user_kolomeetz, name='wat', url='wot', size=491425, description='A nice new file'
        )
        file.save()

        self.client.login('thasonic')
        response = self.client.delete('/_api/v1/pages/filespage/.files/wot')
        self.assertEqual(200, response.status_code)
        self.assertEqual(File.active.filter(id=file.id).count(), 0)


@override_settings(
    WIKI_PROTOCOL='http',
    NGINX_HOST='wiki',
)
class ListFilesTest(BaseApiTestCase):
    def test_get_list_of_files(self):
        from datetime import datetime

        self.setUsers()
        page = self.create_page(tag='filespages')
        file_stub = stub(
            name='moskva.mp3',
            description='By Leningrad',
            url='moskva_1.mp3',
            size=1024 * 1024,
            upload_date=datetime(2014, 8, 14, 19, 22, 00),
            user=stub(username='chapson'),
        )
        stream_stub = stub(__iter__=lambda *args: iter([file_stub]), next_start_id=1)
        with patch('wiki.api_v1.views.files.get_files', Mock(return_value=stream_stub)) as get_files_mock:
            self.client.login('thasonic')
            response = self.client.get('/_api/v1/pages/filespages/.files')

            self.assertEqual(200, response.status_code)
            data = loads(response.content)['data']
            self.assertEqual(
                data,
                {
                    'files': [
                        {
                            'upload_date': '2014-08-14T19:22:00',
                            'description': 'By Leningrad',
                            'url': 'moskva_1.mp3',
                            'docviewer_url': None,
                            'size': 1.0,
                            'user_name': 'chapson',
                            'name': 'moskva.mp3',
                        }
                    ],
                    'links': {'next': 'http://wiki/_api/v1/pages/filespages/.files?per_page=25&start_id=1'},
                },
            )  # noqa
            get_files_mock.assert_called_once_with(page.id, 25, None)
