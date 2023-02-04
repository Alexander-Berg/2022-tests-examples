
from wiki.notifications.dao import revision_by_page_event
from wiki.pages.api import save_page
from wiki.users.models import User
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class RevisionByPageEventTestCase(WikiDjangoTestCase):
    def test_revision_by_page_event(self):
        from wiki.notifications.models import PageEvent

        chapson = User.objects.create_user('chapson', 'chapson@yandex-team.ru')
        page, revision, _ = save_page('tag', unicode_text='', user=chapson)
        page_event = PageEvent(event_type=PageEvent.EVENT_TYPES.create)
        page_event.meta = {
            'revision_id': revision.id,
        }
        self.assertEqual(revision, revision_by_page_event(page_event))

        page_event = PageEvent(
            event_type=PageEvent.EVENT_TYPES.delete,
            meta={},
        )
        self.assertEqual(None, revision_by_page_event(page_event))
