
from wiki import access as wiki_access
from wiki.pages.models import Access
from wiki.users.models import User
from wiki.users_biz.signals import dismiss_dir_user, get_dir_organization, import_dir_user
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase

import_object = {
    'email': 'nologin@noemal.ru',
    'gender': 'male',
    'id': 5555555,
    'is_robot': False,
    'name': {'first': 'User', 'last': 'Testovyi'},
    'nickname': 'testuser',
    'service_slug': None,
    'org_id': 42,
}

dismiss_object = {
    'id': 5555555,
    'org_id': 42,
}


class TestUserBiz(BaseTestCase):
    def test_import_new_user(self):
        import_dir_user(sender=None, object=import_object, content=None)

        user = User.objects.get(dir_id=import_object['id'])
        self.assertTrue(user.is_active)
        self.assertFalse(user.staff.is_dismissed)

    def test_dismiss_user(self):
        import_dir_user(sender=None, object=import_object, content=None)
        dismiss_dir_user(sender=None, object=dismiss_object, content=None)

        user = User.objects.get(dir_id=import_object['id'])
        self.assertFalse(user.is_active)
        self.assertTrue(user.staff.is_dismissed)

    def test_dismiss_user_with_access(self):
        import_dir_user(sender=None, object=import_object, content=None)

        user = User.objects.get(dir_id=import_object['id'])
        page_with_org = self.create_page(
            tag='Page with org',
            body='Page body',
        )
        org = get_dir_organization(import_object['org_id'])
        page_with_org.org = org
        page_with_org.save()

        page_without_org = self.create_page(
            tag='Page without org',
            body='Page body',
        )

        wiki_access.set_access(
            page_with_org,
            wiki_access.TYPES.RESTRICTED,
            user,
            send_notification_signals=False,
            staff_models=[user.staff],
        )

        wiki_access.set_access(
            page_without_org,
            wiki_access.TYPES.RESTRICTED,
            user,
            send_notification_signals=False,
            staff_models=[user.staff],
        )

        access_count = Access.objects.count()
        self.assertEqual(access_count, 2)

        dismiss_dir_user(sender=None, object=dismiss_object, content=None)

        user.refresh_from_db()
        user.staff.refresh_from_db()
        self.assertFalse(user.is_active)
        self.assertTrue(user.staff.is_dismissed)

        # После удаления пользователя из организации должен остаться доступ
        # только к страницам не принадлежащим этой организации
        access_count = Access.objects.count()
        self.assertEqual(access_count, 1)
