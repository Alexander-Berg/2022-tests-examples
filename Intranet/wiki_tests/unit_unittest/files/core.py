from wiki.files.models import File
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class TestFileInfoByStorageId(BaseApiTestCase):
    def test_fileinfo_by_storage_id(self):
        from wiki.files.models import fileinfo_by_storage_id

        etalon = 'wiki:file:Zm9vLmJhcg==:100:2014-09-29 17:20:31:624978'
        filename, size = fileinfo_by_storage_id(etalon)
        self.assertEqual(filename, 'foo.bar')
        self.assertEqual(100, size)


class BaseFilesTestCase(BaseApiTestCase):
    def setUp(self):
        self.page = self.create_page(tag='tracks', files=1)
        self.user = self.page.get_authors().first()
        self.file = File.objects.create(page=self.page, user=self.user)
