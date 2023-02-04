
from django.conf import settings

from wiki.notifications.generators import request_ownership as request_ownership_gen
from wiki.notifications.models import PageEvent
from wiki.pages.models import Page
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class RequestOwnershipTest(BaseTestCase):
    def setUp(self):
        super(RequestOwnershipTest, self).setUp()
        self.alice = self.get_or_create_user('alice')
        self.bob = self.get_or_create_user('bob')
        self.alices_cluster = Page.objects.create(
            tag='wonderland/garden',
            supertag='wonderland/garden',
            owner=self.alice,
            modified_at=timezone.now(),
        )
        self.alices_cluster.authors.add(self.alice)

    def _generate_event(self, event_meta):
        event = PageEvent(
            event_type=PageEvent.EVENT_TYPES.request_ownership,
            author=self.bob,
            timeout=timezone.now(),
            page=self.alices_cluster,
        )
        event.meta = event_meta
        emails = request_ownership_gen.generate([event])
        return emails.popitem()

    def test_receiver_is_owner(self):
        email_details, message = self._generate_event({'reason': 'I liek this page'})

        self.assertIn('<alice@yandex-team.ru>', email_details.receiver_email)

    if settings.IS_INTRANET:

        def test_receiver_is_tools_if_owner_is_dismissed(self):
            self.alice.staff.is_dismissed = True
            self.alice.staff.save()

            email_details, message = self._generate_event({'reason': 'I liek this page'})

            self.assertEqual(settings.SUPPORT_EMAIL, email_details.receiver_email)

    def test_text_in_message_for_whole_cluster_request(self):
        email_details, message = self._generate_event({'reason': 'I liek this page'})

        self.assertIn('notifications.request_ownership:UserWantsToOwnClusterByOther', message[0])

    def test_text_in_message_for_root_page_only_request(self):
        email_details, message = self._generate_event({'reason': 'I liek this page', 'root_page_only': True})

        self.assertIn('notifications.request_ownership:UserWantsToOwnPageByOther', message[0])

    def test_subject_and_message_contain_event_data(self):
        email_details, message = self._generate_event({'reason': 'I liek this page'})

        self.assertIn('alice@', email_details.subject, 'Owner should be mentioned in subject')
        self.assertIn('wonderland/garden', message[0], 'Page tag should be mentioned in message')
        self.assertIn(settings.NGINX_HOST, message[0], 'Host should be mentioned in message')
        self.assertIn('I liek this page', message[0], 'Reason should be mentioned in message')
