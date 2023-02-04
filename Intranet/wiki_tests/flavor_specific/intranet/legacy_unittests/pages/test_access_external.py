
from wiki.access import TYPES, set_access
from wiki.intranet.models import Department
from wiki.pages.access.external import (
    _get_external_groups,
    _get_external_staff,
    bulk_update_ext_access_status,
    is_available_to_ext_users,
    update_ext_access_status,
)
from wiki.pages.models import Page
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class ExternalAccessTest(BaseTestCase):
    def setUp(self):
        super(ExternalAccessTest, self).setUp()
        self.setUsers()
        self.setGroupMembers()

    def test_external_staff_getter(self):
        # ordinary chapson
        chapson = self.user_chapson.staff

        self.assertFalse(chapson in _get_external_staff())

        # upgraded chapson
        self.convert_user_to_external(chapson)

        self.assertTrue(chapson in _get_external_staff())

    def test_external_groups_getter(self):
        chapson = self.user_chapson.staff

        self.assertFalse(len(_get_external_groups()) > 0)

        self.convert_user_to_external(chapson)

        self.assertTrue(len(_get_external_groups()) > 0)

    def test_update_ext_access_status(self):
        """Создаем кластер, даем доступ внешнему консультанту.

        Функция update_access_status должна обновить статус доступа к подстранице.

        """
        page = self.create_page(tag='СтраницаВнешняя')
        subpage = self.create_page(tag='СтраницаВнешняя/Подстраница')
        subpage.opened_to_external_flag = None
        subpage.save()

        # Делаем внешнего консультанта
        chapson = self.user_chapson.staff
        self.convert_user_to_external(chapson)

        # Даем ему доступ
        set_access(page, TYPES.RESTRICTED, self.user_thasonic, staff_models=[self.user_chapson.staff])

        update_ext_access_status(page.supertag)

        renewed_page = Page.objects.get(supertag=subpage.supertag)
        self.assertTrue(renewed_page.opened_to_external_flag)

    def test_bulk_ext_access_status_update(self):
        """
        Тест хелпера для массового обновления статуса открытых страниц
        """
        pages = Page.active.all()

        make_open_limit = 15  # лимит куска в bulk_update_access_status 1000

        new_pages_ids = [self.create_page(tag='СтраницаВнешняя%d' % num).id for num in range(0, make_open_limit)]

        new_pages = Page.active.filter(id__in=new_pages_ids)

        # Делаем внешнего консультанта
        chapson = self.user_chapson.staff
        ext_department = Department.objects.get(id=2)

        opened = Page.active.filter(opened_to_external_flag=True).count()
        self.assertEqual(opened, 0)

        self.convert_user_to_external(chapson, ext_department)

        # Даем доступ внешнему консультанту на первые 5 страниц
        [set_access(page, TYPES.RESTRICTED, self.user_thasonic, staff_models=[chapson]) for page in new_pages]

        bulk_update_ext_access_status(pages, chunk_limit=10)

        opened = Page.active.filter(opened_to_external_flag=True).count()

        self.assertEqual(opened, make_open_limit)

    def test_available_to_ext_users_checker(self):
        chapson = self.user_chapson.staff
        ext_department = Department.objects.get(id=2)
        self.convert_user_to_external(chapson, ext_department)

        page = self.create_page(tag='АрктангенсИзАрканзаса/Подстраница')
        set_access(page, TYPES.COMMON, self.user_thasonic)

        self.assertFalse(is_available_to_ext_users(page))

        set_access(page, TYPES.RESTRICTED, self.user_thasonic, staff_models=[chapson])

        self.assertTrue(is_available_to_ext_users(page))
