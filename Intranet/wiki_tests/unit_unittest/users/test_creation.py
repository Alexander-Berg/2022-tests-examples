from unittest import skipIf

from django.conf import settings
from django.contrib.admin import AdminSite
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Group
from django.http import HttpRequest
from mock import Mock, patch

from wiki.personalisation.user_cluster import personal_cluster
from wiki.users import forms
from wiki.users.admin_forms import GroupAdminExternal, UserAdminExternal
from wiki.users.core import create_user_and_staff
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get

User = get_user_model()


class MockBlackbox(object):
    def __init__(self):
        pass

    def uid(self, username, headers):
        return {
            'valid.login': '12345',
            'Valid.Login@example.com': '12345',
            'qwerty': '111',
            'member1': '222',
            'member2': '333',
        }.get(username)


@skipIf(settings.IS_BUSINESS, 'only for intranet')
@patch('wiki.utils.tvm2.get_tvm2_client', Mock())
class UserCreationTest(BaseTestCase):
    has_personal_cluster = False

    def setUp(self):
        forms.blackbox = MockBlackbox()
        self.admin_site = AdminSite()

    def test_user_creation_through_admin(self):
        group = get(Group, name='test_group')

        user_admin = UserAdminExternal(User, self.admin_site)
        AddUserForm = user_admin.get_form(HttpRequest())

        form = AddUserForm(
            {
                'username': 'Valid.Login',
                'first_name': 'aaa',
                'last_name': 'bbb',
                'email': 'Valid.Login@example.com',
                'groups': [group.id],
            }
        )

        self.assertTrue(form.is_valid(), msg=form.errors.as_text())

        user = form.save()
        form.save_m2m()
        user = User.objects.get(id=user.id)

        staff = user.staff
        self.client.login(staff.login)

        request_url = '/_api/frontend/{page_supertag}'.format(page_supertag='users/%s' % staff.login)
        self.client.get(request_url)
        self.assertIsNotNone(personal_cluster(staff.user))

        self.assertEqual(staff.uid, '12345')
        self.assertEqual(staff.normal_login, 'valid-login')
        self.assertEqual(staff.wiki_name, 'AaaBbb')
        self.assertIsNotNone(staff.user)
        self.assertEqual(user.groups.get().name, 'test_group')

    def test_user_creation_through_admin_errors(self):
        user_admin = UserAdminExternal(User, self.admin_site)
        AddUserForm = user_admin.get_form(HttpRequest())
        form2 = AddUserForm(
            {'username': 'invalid', 'first_name': 'Ccc', 'last_name': 'Dddb', 'email': 'cd@example.com'}
        )

        self.assertFalse(form2.is_valid())

    def test_user_changing_through_admin(self):
        user = create_user_and_staff(
            username='qwerty', first_name='Killa', last_name='Gorilla', email='qwerty@example.com', uid='12345'
        )
        self.assertIsNotNone(user)

        user_admin = UserAdminExternal(User, self.admin_site)
        EditUserForm = user_admin.get_form(HttpRequest(), user)

        group1 = get(Group, name='group_1')
        group2 = get(Group, name='group_2')

        edit_form = EditUserForm(
            {'first_name': 'eee', 'last_name': 'FFF', 'email': 'ef@example.com', 'groups': [group1.id, group2.id]},
            instance=user,
        )

        self.assertTrue(edit_form.is_valid(), msg=edit_form.errors.as_text())

        user = edit_form.save()
        edit_form.save_m2m()

        user = User.objects.get(id=user.id)
        staff = user.staff

        self.assertEqual(user.first_name, 'eee')
        self.assertEqual(staff.last_name, 'FFF')
        self.assertEqual(staff.wiki_name, 'EeeFff')
        self.assertEqual(user.groups.count(), 2)

    def test_group_editing_through_admin(self):
        group_admin = GroupAdminExternal(Group, self.admin_site)
        GroupForm = group_admin.get_form(HttpRequest())

        user1 = create_user_and_staff(
            username='member1',
            first_name='User',
            last_name='One',
            email='member1@example.com',
            uid='111',
        )
        user2 = create_user_and_staff(
            username='member2',
            first_name='User',
            last_name='Two',
            email='member1@example.com',
            uid='222',
        )

        group_form = GroupForm({'name': 'new_group', 'users': [user1.id]})

        self.assertTrue(group_form.is_valid())

        group = group_form.save()
        group = Group.objects.get(id=group.id)

        self.assertEqual(group.user_set.count(), 1)

        GroupEditForm = group_admin.get_form(HttpRequest(), group)
        group_edit_form = GroupEditForm({'name': 'updated_name', 'users': [user1.id, user2.id]})

        self.assertTrue(group_edit_form.is_valid())

        group = group_edit_form.save(commit=False)
        group.save()
        group_edit_form.save_m2m()
        group = Group.objects.get(id=group.id)

        self.assertEqual(group.user_set.count(), 2)

    def test_create_user_on_wiki_school(self):
        if settings.WIKI_CODE != 'school':
            return
        new_user = get_user_model().objects.create(
            username='robot', first_name='Vasya', last_name='Pupkin', email='robot@yandex-team.ru'
        )
        user = get_user_model().objects.get(id=new_user.id)
        self.assertDictEqual(
            user.profile,
            {
                'theme_lang': 'en',
                'code_theme': 'github',
                'minimize_bookmarks': False,
                'use_nodejs_frontend': True,
                'double_click': True,
            },
        )

    def test_create_user_on_wiki_evaluation(self):
        if settings.WIKI_CODE != 'evaluation':
            return
        new_user = get_user_model().objects.create(
            username='robot', first_name='Vasya', last_name='Pupkin', email='robot@yandex-team.ru'
        )
        user = get_user_model().objects.get(id=new_user.id)
        self.assertDictEqual(
            user.profile,
            {
                'theme_lang': 'en',
                'code_theme': 'github',
                'minimize_bookmarks': False,
                'use_nodejs_frontend': True,
                'double_click': True,
            },
        )

    def test_create_user_on_main_instance(self):
        if settings.WIKI_CODE != 'wiki':
            return
        new_user = get_user_model().objects.create(
            username='robot', first_name='Vasya', last_name='Pupkin', email='robot@yandex-team.ru'
        )
        user = get_user_model().objects.get(id=new_user.id)
        self.assertDictEqual(
            user.profile,
            {
                'theme_lang': 'en',
                'code_theme': 'github',
                'minimize_bookmarks': False,
                'use_nodejs_frontend': True,
                'double_click': True,
                'new_favorites': True,
                'new_subscriptions': True,
            },
        )
