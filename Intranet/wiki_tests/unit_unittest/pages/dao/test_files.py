
from wiki.pages import dao
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class FilesDaoTest(FixtureMixin, WikiDjangoTestCase):
    def setUp(self):
        super(FilesDaoTest, self).setUp()
        self.page = self.create_page()

    def test_decrement_files_count(self):
        self.page.files = 10
        self.page.save()
        page = self.refresh_objects(self.page)

        dao.decrement_files_count(page)

        page = self.refresh_objects(page)
        self.assertEqual(page.files, 9)

    def test_get_files(self):
        file = self.create_file(page=self.page)
        self.create_file(page=self.page, status=0, url='deleted')

        files = dao.get_files(page=self.page)

        self.assertEqual(list(files), [file])
