from base64 import b64encode
from datetime import timedelta
from pickle import dumps

from wiki.pages.models import SearchExclude
from wiki.pages.models.page import YEARS_AGO, Page
from wiki.pages.models.consts import ACTUALITY_STATUS
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class PageTest(BaseApiTestCase):
    def test_dont_index(self):
        page = Page(tag='/1', modified_at=timezone.now())
        page.save()
        self.assertFalse(page.excluded_from_search_index)
        SearchExclude.objects.create(page=page)
        page.refresh_from_db()
        self.assertTrue(page.excluded_from_search_index)

    def test_active_property(self):
        """
        page.is_active property must depend on status
        """
        page = Page()
        self.assertTrue(page.is_active)

        page.status = 0
        self.assertFalse(page.is_active)

    def test_has_manual_actuality_mark(self):
        page = Page(actuality_status=ACTUALITY_STATUS.unspecified)
        self.assertFalse(page.has_manual_actuality_mark)

        page = Page(actuality_status=ACTUALITY_STATUS.possibly_obsolete)
        self.assertFalse(page.has_manual_actuality_mark)

        page = Page(actuality_status=ACTUALITY_STATUS.obsolete)
        self.assertTrue(page.has_manual_actuality_mark)

        page = Page(actuality_status=ACTUALITY_STATUS.actual)
        self.assertTrue(page.has_manual_actuality_mark)


class PageRealtimeActualityStatusTest(BaseApiTestCase):
    MORE_THAN_ONE_YEAR_AGO = timezone.now() - timedelta(days=365, seconds=1)
    MORE_THAN_HALFAYEAR_AGO = timezone.now() - timedelta(days=365 / 2 + 1)
    LESS_THAN_HALFAYEAR_AGO = timezone.now() - timedelta(days=365 / 2)

    def test_actual_page_no_mark(self):
        # отметок нет, страница свежая
        page = Page(modified_at=timezone.now(), actuality_status=ACTUALITY_STATUS.unspecified)
        self.assertEqual(page.realtime_actuality_status, ACTUALITY_STATUS.actual)

    def test_obsolete_page_no_mark(self):
        # отметок нет, страница несвежая
        page = Page(modified_at=self.MORE_THAN_ONE_YEAR_AGO, actuality_status=ACTUALITY_STATUS.unspecified)
        self.assertEqual(page.realtime_actuality_status, ACTUALITY_STATUS.possibly_obsolete)

    def test_obsolete_mark(self):
        # есть отметка про неактуальность, (страница свежая, но только для проверки)
        page = Page(modified_at=YEARS_AGO, actuality_status=ACTUALITY_STATUS.obsolete)
        self.assertEqual(page.realtime_actuality_status, ACTUALITY_STATUS.obsolete)

    def test_actual_mark_inforce(self):
        # есть свежая отметка про актуальность (страница несвежая, но только для проверки)
        page = Page(
            modified_at=YEARS_AGO,
            actuality_status=ACTUALITY_STATUS.actual,
            actuality_marked_at=self.LESS_THAN_HALFAYEAR_AGO,
        )
        self.assertEqual(page.realtime_actuality_status, ACTUALITY_STATUS.actual)

    def test_actual_mark_outdated(self):
        # есть несвежая отметка про актуальность, страница несвежая
        page = Page(
            modified_at=self.MORE_THAN_ONE_YEAR_AGO,
            actuality_status=ACTUALITY_STATUS.actual,
            actuality_marked_at=self.MORE_THAN_HALFAYEAR_AGO,
        )
        self.assertEqual(page.realtime_actuality_status, ACTUALITY_STATUS.possibly_obsolete)

    def test_actual_page_actual_mark_outdated(self):
        # есть несвежая отметка про актуальность, страница свежая
        page = Page(modified_at=timezone.now(), actuality_status=ACTUALITY_STATUS.actual, actuality_marked_at=YEARS_AGO)
        self.assertEqual(page.realtime_actuality_status, ACTUALITY_STATUS.actual)
