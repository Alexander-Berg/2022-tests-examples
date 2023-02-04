import datetime

from wiki import access as wiki_access
from wiki.pages.models import Page
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager


class UpdateAccessForIndex(BaseTestCase):
    def setUp(self):
        super(UpdateAccessForIndex, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    @celery_eager
    def test_update_access_for_index(self):
        before = timezone.now() - datetime.timedelta(days=1)

        page1 = self.create_page(tag='One')
        page2 = self.create_page(tag='One/Two')
        page3 = self.create_page(tag='One/Two/Three')
        page4 = self.create_page(tag='One/Two/Three/Four')

        wiki_access.set_access(
            page3, wiki_access.TYPES.OWNER, self.user_thasonic, staff_models=[self.user_chapson.staff]
        )

        # Делаем страницы старыми
        for page in (page1, page2, page3, page4):
            page.modified_at = before
            page.modified_at_for_index = before
            page.save()

        now = timezone.now()
        # В базе не хранятся миллисекунды, поэтому время обновления страницы может получиться раньше, чем now
        now -= datetime.timedelta(seconds=1)

        wiki_access.set_access(
            page1, wiki_access.TYPES.OWNER, self.user_thasonic, staff_models=[self.user_kolomeetz.staff]
        )

        # Теперь page1 и page2 должны обновиться, а page3 и page4 - нет
        page1 = Page.objects.get(id=page1.id)
        page2 = Page.objects.get(id=page2.id)
        page3 = Page.objects.get(id=page3.id)
        page4 = Page.objects.get(id=page4.id)

        msg = '%s compared to %s'
        self.assertTrue(page1.modified_at_for_index >= now, msg % (page1.modified_at_for_index, now))
        self.assertTrue(page2.modified_at_for_index >= now, msg % (page2.modified_at_for_index, now))
        self.assertTrue(page3.modified_at_for_index <= before, msg % (page3.modified_at_for_index, before))
        self.assertTrue(page4.modified_at_for_index <= before, msg % (page4.modified_at_for_index, before))
