from django.conf import settings
from mock import patch
from pretend import stub

from wiki.intranet.models import Staff
from wiki.notifications.generators import mark_actuality_gen
from wiki.notifications.generators.base import EventTypes
from intranet.wiki.tests.wiki_tests.common.utils import CallRecorder, SimpleStub, unexpected_call
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class MarkActualityTest(WikiDjangoTestCase):

    # часть случаев покрыта в api_frontend.tests.actuality.ActualityAPIIntegrationTest

    def test_author_is_owner(self):
        author = stub(staff=Staff())
        page = stub(supertag='str', tag='стр', get_authors=lambda: [author], is_official=False)

        events = list()
        event = stub(page=page, author=author, event_type=EventTypes.mark_actual, meta={})
        events.append(event)

        if settings.IS_INTRANET:

            @patch('wiki.notifications.generators.mark_actuality.get_officiality', unexpected_call)
            @patch.object(mark_actuality_gen, 'get_groups_all_members', unexpected_call)
            @patch.object(mark_actuality_gen, 'email_language', unexpected_call)
            def f():
                return mark_actuality_gen.generate(events)

        else:

            @patch.object(mark_actuality_gen, 'email_language', unexpected_call)
            def f():
                return mark_actuality_gen.generate(events)

        messages = f()

        self.assertEqual(len(messages), 0)

    if settings.IS_INTRANET:

        def test_owner_is_dismissed(self):
            author = stub(staff=Staff(is_dismissed=True))
            page = stub(supertag='str', tag='стр', get_authors=lambda: [author], is_official=False)

            events = list()
            event = stub(page=page, author=author, event_type=EventTypes.mark_actual, meta={})
            events.append(event)

            @patch('wiki.notifications.generators.mark_actuality.get_officiality', unexpected_call)
            @patch.object(mark_actuality_gen, 'get_groups_all_members', unexpected_call)
            @patch.object(mark_actuality_gen, 'email_language', unexpected_call)
            def f():
                return mark_actuality_gen.generate(events)

            messages = f()

            self.assertEqual(len(messages), 0)

        def test_author_and_owner_among_responsible(self):
            # официальная страница, автор и владельцы находятся среди responsible_persons и входят в responsible_groups
            # и среди ответственных есть dismissed.

            author_of_page = stub(staff=SimpleStub(login='theowner', is_dismissed=False))
            author_of_page.staff.user = author_of_page
            page = stub(
                pk=432,
                status=1,
                supertag='str',
                absolute_url='absolute url',
                tag='стр',
                get_authors=lambda: [author_of_page],
                is_official=True,
                title='тайтл',
            )

            author = stub(
                staff=SimpleStub(
                    login='theauthor',
                    is_dismissed=False,
                    inflections=stub(subjective='theauthor'),
                    get_email=lambda: 'theauthor@yandex-team.ru',
                )
            )
            author.staff.user = author
            events = list()
            event = stub(page=page, author=author, event_type=EventTypes.mark_obsolete, meta={})
            events.append(event)

            group1 = stub(pk=123)
            group2 = stub(pk=124)
            officiality = stub(
                responsible_persons=stub(all=lambda: {author_of_page, author}),
                responsible_groups=stub(all=lambda: [group1, group2]),
            )
            get_officiality_patch = CallRecorder(lambda *args, **kwargs: officiality)

            another_user_active = stub(staff=SimpleStub(login='user1', is_dismissed=False))
            another_user_active.staff.user = another_user_active
            another_user_dismissed = stub(staff=SimpleStub(login='user2', is_dismissed=True))
            another_user_dismissed.staff.user = another_user_dismissed
            get_groups_all_members_patch = CallRecorder(
                lambda *args: {author_of_page, author, another_user_active, another_user_dismissed}
            )

            email_language_patch = CallRecorder(
                lambda r, strict_mode: (r.staff.login + '@yandex-team.ru', r.staff.login + 'NAME', 'en')
            )

            @patch('wiki.notifications.generators.mark_actuality.get_officiality', get_officiality_patch.get_func())
            @patch.object(mark_actuality_gen, 'get_groups_all_members', get_groups_all_members_patch.get_func())
            @patch.object(mark_actuality_gen, 'email_language', email_language_patch.get_func())
            def f():
                return mark_actuality_gen.generate(events)

            messages = f()

            self.assertEqual(len(messages), 2)

            for email_details, receiver_messages in messages.items():
                self.assertTrue('obsolete' in email_details.subject)
                self.assertTrue(page.tag in email_details.subject)
                self.assertTrue(email_details.receiver_email in {'user1@yandex-team.ru', 'theowner@yandex-team.ru'})
                self.assertEqual(len(receiver_messages), 1)
                message = receiver_messages[0]
                self.assertTrue(str(author) in message)
                self.assertTrue('obsolete' in message)
                self.assertTrue(page.tag in message)
                self.assertTrue('absolute url' in message)

            self.assertEqual(get_officiality_patch.times, 1)
            self.assertEqual(get_officiality_patch.calls[0].args[0], page.pk)

            self.assertEqual(get_groups_all_members_patch.times, 1)
            self.assertEqual(get_groups_all_members_patch.calls[0].args[0], [group1.pk, group2.pk])

            self.assertEqual(email_language_patch.times, 2)
            self.assertEqual(email_language_patch.calls[0].args[0], author_of_page)
            self.assertEqual(email_language_patch.calls[1].args[0], another_user_active)
