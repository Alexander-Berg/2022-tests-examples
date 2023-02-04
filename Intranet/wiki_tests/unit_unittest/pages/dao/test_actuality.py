from wiki.pages.dao.actuality import (
    get_actuality_mark,
    get_actuality_mark_links,
    get_linked_actual_pages_supertags,
    get_linked_actual_pages_tags,
    remove_actuality_mark_links_to,
)
from wiki.pages.models import ActualityMark, ActualityMarkLink, Comment
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class ActualityDaoTestCase(FixtureMixin, WikiDjangoTestCase):
    def setUp(self):
        super(ActualityDaoTestCase, self).setUp()
        self.page = self.create_page()

    def _test_get_actuality_mark(self, comment):
        mark = ActualityMark.objects.create(page=self.page, comment=comment, user=self._default_user)

        got_mark = get_actuality_mark(self.page.pk)
        self.assertTrue(isinstance(got_mark, ActualityMark))
        self.assertEqual((mark.page, mark.comment, mark.user), (got_mark.page, got_mark.comment, got_mark.user))

    def test_get_actuality_mark_with_comment(self):
        comment = Comment.objects.create(
            page=self.page, body='неправда', user=self._default_user, page_at=self.page.modified_at
        )
        self._test_get_actuality_mark(comment)

    def test_get_actuality_mark_without_comment(self):
        self._test_get_actuality_mark(None)

    def test_get_linked_actual_pages_supertags(self):
        linked_pages_tags = get_linked_actual_pages_supertags(self.page.pk)
        self.assertEqual(len(linked_pages_tags), 0)

        actual_page1_supertag = 'some/page1'
        actual_page1 = self.create_page(supertag=actual_page1_supertag)
        am_link = ActualityMarkLink()
        am_link.page = self.page
        am_link.actual_page = actual_page1
        am_link.save()

        linked_pages_tags = get_linked_actual_pages_supertags(self.page.pk)
        self.assertEqual(list(linked_pages_tags), [actual_page1_supertag])

        actual_page2_supertag = 'page2'
        actual_page2 = self.create_page(supertag=actual_page2_supertag)
        am_link = ActualityMarkLink()
        am_link.page = self.page
        am_link.actual_page = actual_page2
        am_link.save()

        linked_pages_tags = get_linked_actual_pages_supertags(self.page.pk)
        self.assertEqual(set(linked_pages_tags), set([actual_page1_supertag, actual_page2_supertag]))

    def test_get_linked_actual_pages_tags(self):
        linked_pages_tags = get_linked_actual_pages_tags(self.page.pk)
        self.assertEqual(len(linked_pages_tags), 0)

        actual_page1_tag = '/some/page1'
        actual_page1 = self.create_page(tag=actual_page1_tag, supertag=actual_page1_tag)
        am_link = ActualityMarkLink()
        am_link.page = self.page
        am_link.actual_page = actual_page1
        am_link.save()

        linked_pages_tags = get_linked_actual_pages_tags(self.page.pk)
        self.assertEqual(list(linked_pages_tags), [actual_page1_tag])

        actual_page2_tag = '/page2'
        actual_page2 = self.create_page(tag=actual_page2_tag, supertag=actual_page2_tag)
        am_link = ActualityMarkLink()
        am_link.page = self.page
        am_link.actual_page = actual_page2
        am_link.save()

        linked_pages_tags = get_linked_actual_pages_tags(self.page.pk)
        self.assertEqual(sorted(list(linked_pages_tags)), [actual_page2_tag, actual_page1_tag])

    def test_get_actuality_mark_links(self):
        am_links = get_actuality_mark_links(self.page.pk)
        self.assertEqual(len(am_links), 0)

        actual_page1_tag = '/some/page1'
        actual_page1 = self.create_page(tag=actual_page1_tag, supertag=actual_page1_tag)
        am_link1 = ActualityMarkLink()
        am_link1.page = self.page
        am_link1.actual_page = actual_page1
        am_link1.save()

        am_links = get_actuality_mark_links(self.page.pk)
        self.assertEqual(list(am_links), [am_link1])

        actual_page2_tag = '/page2'
        actual_page2 = self.create_page(tag=actual_page2_tag, supertag=actual_page2_tag)
        am_link2 = ActualityMarkLink()
        am_link2.page = self.page
        am_link2.actual_page = actual_page2
        am_link2.save()

        am_links = get_actuality_mark_links(self.page.pk)
        self.assertEqual(list(am_links), [am_link1, am_link2])

    def test_remove_actuality_mark_links_to(self):
        actual_page1_tag = '/some/page1'
        actual_page1 = self.create_page(tag=actual_page1_tag, supertag=actual_page1_tag)
        am_link1 = ActualityMarkLink()
        am_link1.page = self.page
        am_link1.actual_page = actual_page1
        am_link1.save()

        actual_page2_tag = '/page2'
        actual_page2 = self.create_page(tag=actual_page2_tag, supertag=actual_page2_tag)
        am_link2 = ActualityMarkLink()
        am_link2.page = self.page
        am_link2.actual_page = actual_page2
        am_link2.save()

        am_link21 = ActualityMarkLink()
        am_link21.page = actual_page2
        am_link21.actual_page = actual_page1
        am_link21.save()

        remove_actuality_mark_links_to(actual_page1)
        am_links = get_actuality_mark_links(self.page.pk)
        self.assertEqual(list(am_links), [am_link2])
