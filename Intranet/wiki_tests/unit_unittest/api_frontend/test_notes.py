
from mock import patch

from wiki.pages.models import Page
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

NOTE_TAG = 'users/thasonic/notes/note-2015-01-01T10:10:10'


@patch(
    'wiki.api_frontend.views.notes.NotesView.generate_note_tag',
    lambda *a, **kw: NOTE_TAG,
)
class NotesViewTest(BaseApiTestCase):
    def setUp(self):
        super(NotesViewTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def get_url(self):
        return '/'.join(
            [
                self.api_url,
                '.notes',
            ]
        )

    def test_create_note(self):
        url = self.get_url()

        response = self.client.post(url, {'body': 'blarhg!'})
        self.assertEqual(response.status_code, 200)

        page_query = Page.objects.filter(tag=NOTE_TAG)
        self.assertTrue(page_query.exists())

        page = page_query.get()
        self.assertEqual(page.body, 'blarhg!')
