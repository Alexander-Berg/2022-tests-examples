from django.contrib.auth import get_user_model
from mock import patch
from pretend import stub

from wiki.notifications.models import PageEvent
from wiki.pages.logic.actuality import mark_page_actuality
from wiki.pages.models import Comment, Page
from wiki.pages.models.consts import ACTUALITY_STATUS
from wiki.utils.errors import ValidationError
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import CallRecorder


class ActualityLogicTestCase(BaseApiTestCase):

    # get_page_actuality_details тестируется в api_frontend.tests.actuality вместе с вью

    def _test_mark_page_actuality(
        self,
        user,
        page,
        is_actual,
        comment,
        links,
        add_comment_patch,
        get_actuality_mark_patch,
        get_actuality_mark_links_patch,
        get_pages_by_supertags_patch,
        page_save_patch,
        actuality_mark_save_patch,
        actuality_mark_link_save_patch,
        page_event_save_patch,
        moment,
    ):
        @patch('wiki.pages.logic.actuality.add_comment', add_comment_patch.get_func())
        @patch('wiki.pages.logic.actuality.get_actuality_mark', get_actuality_mark_patch.get_func())
        @patch('wiki.pages.logic.actuality.get_actuality_mark_links', get_actuality_mark_links_patch.get_func())
        @patch('wiki.pages.logic.actuality.get_pages_by_supertags', get_pages_by_supertags_patch.get_func())
        @patch('wiki.pages.models.Page.save', page_save_patch.get_func())
        @patch('wiki.pages.models.ActualityMark.save', actuality_mark_save_patch.get_func())
        @patch('wiki.pages.models.ActualityMarkLink.save', actuality_mark_link_save_patch.get_func())
        @patch('wiki.notifications.models.PageEvent.save', page_event_save_patch.get_func())
        @patch('wiki.pages.logic.actuality.now', lambda: moment)
        def f():
            return mark_page_actuality(user, page, is_actual, comment, links)

        f()

    def _test_mark_page_actuality_valid(self, is_actual, current_status, with_maxdata):
        user = get_user_model()()
        page = Page(id=111)
        page.actuality_status = current_status
        moment = stub()

        comment = Comment()
        actual_page = Page()

        actuality_mark_delete_patch = CallRecorder()
        old_actuality_mark = stub(delete=actuality_mark_delete_patch.get_func())
        get_actuality_mark_patch = CallRecorder(lambda *args: old_actuality_mark)

        actuality_mark_link_delete_patch = CallRecorder()
        old_actuality_mark_link = stub(delete=actuality_mark_link_delete_patch.get_func())
        get_actuality_mark_links_patch = CallRecorder(lambda *args: [old_actuality_mark_link, old_actuality_mark_link])

        add_comment_patch = CallRecorder(lambda *args: comment)
        get_pages_by_supertags_patch = CallRecorder(lambda _supertags: [actual_page, actual_page] if _supertags else [])
        page_save_patch = CallRecorder()
        actuality_mark_save_patch = CallRecorder()
        actuality_mark_link_save_patch = CallRecorder()
        page_event_save_patch = CallRecorder()

        comment_text = 'йцук' if with_maxdata else None
        links = ['aaa', 'bbb', 'bbb'] if with_maxdata else []
        self._test_mark_page_actuality(
            user,
            page,
            is_actual,
            comment_text,
            links,
            add_comment_patch,
            get_actuality_mark_patch,
            get_actuality_mark_links_patch,
            get_pages_by_supertags_patch,
            page_save_patch,
            actuality_mark_save_patch,
            actuality_mark_link_save_patch,
            page_event_save_patch,
            moment,
        )

        if with_maxdata:
            self.assertEqual(add_comment_patch.times, 1)
            comment_args = add_comment_patch.calls[0].args
            self.assertIs(comment_args[0], user)
            self.assertIs(comment_args[1], page)
            self.assertEqual(comment_args[2], 'йцук')
            self.assertTrue(comment_args[3] is None)
        else:
            self.assertEqual(add_comment_patch.times, 0)

        if with_maxdata:
            self.assertEqual(get_pages_by_supertags_patch.times, 1)
            self.assertEqual(get_pages_by_supertags_patch.calls[0].args[0], set(links))
        else:
            self.assertEqual(get_pages_by_supertags_patch.times, 0)

        self.assertEqual(page_save_patch.times, 1)
        self.assertTrue(page_save_patch.calls[0].args[0] is page)
        self.assertEqual(page.actuality_status, ACTUALITY_STATUS.actual if is_actual else ACTUALITY_STATUS.obsolete)
        self.assertTrue(page.actuality_marked_at is moment)

        self.assertEqual(actuality_mark_save_patch.times, 1)
        actuality_mark = actuality_mark_save_patch.calls[0].args[0]
        self.assertIs(actuality_mark.user, user)
        self.assertIs(actuality_mark.page, page)
        self.assertIs(actuality_mark.comment, (comment if with_maxdata else None))

        if with_maxdata:
            self.assertEqual(actuality_mark_link_save_patch.times, 2)
            actuality_mark_link = actuality_mark_link_save_patch.calls[0].args[0]
            self.assertIs(actuality_mark_link.page, page)
            self.assertIs(actuality_mark_link.actual_page, actual_page)
            actuality_mark_link = actuality_mark_link_save_patch.calls[1].args[0]
            self.assertIs(actuality_mark_link.page, page)
            self.assertIs(actuality_mark_link.actual_page, actual_page)
        else:
            self.assertEqual(actuality_mark_link_save_patch.times, 0)

        if current_status == ACTUALITY_STATUS.unspecified:
            self.assertFalse(get_actuality_mark_patch.is_called)
            self.assertFalse(get_actuality_mark_links_patch.is_called)

            self.assertFalse(actuality_mark_delete_patch.is_called)
            self.assertFalse(actuality_mark_link_delete_patch.is_called)
        else:
            self.assertEqual(get_actuality_mark_patch.times, 1)
            self.assertEqual(get_actuality_mark_patch.calls[0].args[0], page.id)

            self.assertEqual(get_actuality_mark_links_patch.times, 1)
            self.assertEqual(get_actuality_mark_links_patch.calls[0].args[0], page.id)

            self.assertEqual(actuality_mark_delete_patch.times, 1)
            self.assertEqual(actuality_mark_link_delete_patch.times, 2)

        self.assertEqual(page_event_save_patch.times, 1)
        event = page_event_save_patch.calls[0].args[0]
        self.assertTrue(event.author is user)
        self.assertTrue(event.page is page)
        self.assertTrue(event.timeout is moment)
        self.assertEqual(
            event.event_type, PageEvent.EVENT_TYPES.mark_actual if is_actual else PageEvent.EVENT_TYPES.mark_obsolete
        )
        if with_maxdata:
            self.assertEqual(event.meta['comment_id'], comment.id)

    def test_mark_page_actuality_valid(self):
        # без удаления старого, максимальные данные
        self._test_mark_page_actuality_valid(
            is_actual=False, current_status=ACTUALITY_STATUS.unspecified, with_maxdata=True
        )

        # с удалением старого, минимальные данные
        self._test_mark_page_actuality_valid(
            is_actual=False, current_status=ACTUALITY_STATUS.obsolete, with_maxdata=False
        )
        self._test_mark_page_actuality_valid(
            is_actual=True, current_status=ACTUALITY_STATUS.obsolete, with_maxdata=False
        )

    def _test_mark_page_actuality_invalid(self, links, found_pages):
        page = stub(has_manual_actuality_mark=False)
        with patch('wiki.pages.logic.actuality.get_pages_by_supertags', lambda x: found_pages):
            with self.assertRaises(ValidationError) as cm:
                mark_page_actuality(
                    user=None, page=page, is_actual=False, comment='', mixed_links=links  # неважно  # неважно
                )
            return cm.exception.invalid_value

    def test_mark_page_actuality_invalid(self):
        # хотя бы для одного супертега не найдена страница

        # ни одной из одной не найдено
        invalid_value = self._test_mark_page_actuality_invalid(links=['missing'], found_pages=[])
        self.assertEqual(invalid_value, 'missing')

        # ни одной из двух не найдено
        links = ['missing1', 'missing2']
        invalid_value = self._test_mark_page_actuality_invalid(links=links, found_pages=[])
        self.assertTrue(invalid_value in links)

        # одна из двух найдена, вторая – нет
        page = stub(supertag='notmissing')
        invalid_value = self._test_mark_page_actuality_invalid(links=[page.supertag, 'missing'], found_pages=[page])
        self.assertEqual(invalid_value, 'missing')
