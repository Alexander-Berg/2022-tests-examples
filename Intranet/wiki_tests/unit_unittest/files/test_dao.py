from wiki.files import dao
from wiki.notifications.models import PageEvent
from intranet.wiki.tests.wiki_tests.unit_unittest.files.core import BaseFilesTestCase


class DaoTest(BaseFilesTestCase):
    def test_soft_delete(self):
        self.assertEqual(self.file.status, 1)

        dao.delete(self.file)

        file = self.refresh_objects(self.file)
        self.assertEqual(file.status, 0)
        self.assertIn('deleted', file.url)

    def test_delete_with_page_event(self):
        dao.create_delete_file_event(self.file, user=self.user)

        event = PageEvent.objects.get()

        self.assertEqual(event.page, self.page)
        self.assertEqual(event.author, self.user)
        self.assertEqual(event.event_type, PageEvent.EVENT_TYPES.delete_file)
        self.assertFalse(event.notify)
