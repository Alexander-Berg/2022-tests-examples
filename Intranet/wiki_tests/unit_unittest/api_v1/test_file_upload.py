
from ujson import loads

from wiki.files.models import MDS_STORAGE
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import encode_multipart_formdata


class FileUploadTest(BaseApiTestCase):
    def test_file_upload(self):
        self.setUsers()
        self.client.login('chapson')

        content_type, body = encode_multipart_formdata([], [('filefield', 'file_file.txt', 'file-contents')])
        response = self.client.post('/_api/v1/files/', data=body, content_type=content_type)

        self.assertEqual(200, response.status_code)
        storage_id = loads(response.content)['data']['storage_id']
        self.assertEqual(MDS_STORAGE.open(storage_id).read(), b'file-contents')
