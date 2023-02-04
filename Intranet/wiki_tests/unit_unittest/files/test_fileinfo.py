from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class TestFileInfoByStorageId(BaseApiTestCase):
    def test_fileinfo_by_storage_id(self):
        from wiki.files.models import fileinfo_by_storage_id

        etalon = 'wiki:file:Zm9vLmJhcg==:100:2014-09-29 17:20:31:624978'
        filename, size = fileinfo_by_storage_id(etalon)
        self.assertEqual(filename, 'foo.bar')
        self.assertEqual(100, size)
