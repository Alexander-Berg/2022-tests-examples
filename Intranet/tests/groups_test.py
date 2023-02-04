from datetime import datetime

from django.core.urlresolvers import reverse

from staff.groups.models import Group, GROUP_TYPE_CHOICES

from staff.lib.testing import (
    GroupFactory,
    GroupMembershipFactory,
    DepartmentFactory,
    StaffFactory,
)
from staff.apicenter.tests.base import BaseApiTestCase


class ApiV1GroupBaseTest(BaseApiTestCase):
    def setUp(self):
        super(ApiV1GroupBaseTest, self).setUp()

        now = datetime.now()

        self.wiki_root = GroupFactory(
            name='__wiki__', url='__wiki__',
            service_id=None, department=None,
            parent=None,
            created_at=now, modified_at=now,
            type=GROUP_TYPE_CHOICES.WIKI,
        )
        self.wiki_first_lvl = GroupFactory(
            name='first', url='first',
            service_id=None, department=None,
            parent=self.wiki_root,
            created_at=now, modified_at=now,
            type=GROUP_TYPE_CHOICES.WIKI,
        )
        self.wiki_second_lvl = GroupFactory(
            name='second', url='second',
            service_id=None, department=None,
            parent=self.wiki_first_lvl,
            created_at=now, modified_at=now,
            type=GROUP_TYPE_CHOICES.WIKI,
        )

        self.department_root = GroupFactory(
            name='__departments__', url='__departments__',
            service_id=None, department=None,
            parent=None,
            created_at=now, modified_at=now,
            type=GROUP_TYPE_CHOICES.DEPARTMENT,
        )
        self.yandex = DepartmentFactory(name='yandex', url='yandex', parent=None)
        self.group_yandex = GroupFactory(
            name='yandex', url='yandex',
            service_id=None, department=self.yandex,
            parent=self.department_root,
            created_at=now, modified_at=now,
            type=GROUP_TYPE_CHOICES.DEPARTMENT,
        )

        self.service_root = GroupFactory(
            name='__services__', url='__services__',
            service_id=None, department=None,
            parent=None,
            created_at=now, modified_at=now,
            type=GROUP_TYPE_CHOICES.SERVICE,
        )
        self.service_id = 123
        self.group_staff = GroupFactory(
            name='staff', url='staff',
            service_id=self.service_id, department=None,
            parent=self.service_root,
            created_at=now, modified_at=now,
            type=GROUP_TYPE_CHOICES.SERVICE,
        )


class ApiV1UserGroupsTest(ApiV1GroupBaseTest):
    """Группы, в которых состоит сотрудник.
    http://center.yandex-team.ru/api/v1/user/sibirev/groups.xml

    """

    def setUp(self):
        super(ApiV1UserGroupsTest, self).setUp()
        self.mouse = StaffFactory(login='mouse')

    def test_only_user_groups(self):
        self.mice = GroupFactory(
            name='mice',
            service_id=None,
            department=None,
            parent=self.wiki_root,
            type=GROUP_TYPE_CHOICES.WIKI,
        )
        self.squirrels = GroupFactory(
            name='squirrels',
            service_id=None,
            department=None,
            parent=self.mice,
            type=GROUP_TYPE_CHOICES.WIKI,
        )
        GroupMembershipFactory(staff=self.mouse, group=self.mice)

        self.url = reverse('user_groups', args=(self.mouse.login, 'json'))
        groups = self.get_json(url=self.url)

        self.assertEqual(len(groups), 1)
        self.assertEqual(groups[0]['id'], self.mice.id)

    def test_plain_groups_in_hierarchy_without_root(self):
        group_top = GroupFactory(name='top', service_id=None, department=None, parent=self.wiki_root)
        group_mice = GroupFactory(name='mice', service_id=None, department=None, parent=group_top)
        Group.tree.rebuild()

        GroupMembershipFactory(staff=self.mouse, group=group_mice)

        self.url = reverse('user_groups', args=(self.mouse.login, 'json'))
        groups = self.get_json(url=self.url)

        self.assertEqual(len(groups), 2)
        self.assertEqual(set(g['id'] for g in groups), {group_top.id, group_mice.id})

    def test_default_fields(self):
        self.mice = GroupFactory(
            name='mice',
            service_id=None,
            department=None,
            parent=self.wiki_root,
            code='mice',
            url='mice',
            type=0,
        )
        GroupMembershipFactory(staff=self.mouse, group=self.mice)

        self.url = reverse('user_groups', args=(self.mouse.login, 'json'))
        groups = self.get_json(url=self.url)

        self.assertEqual(set(groups[0].keys()), {'id', 'url', 'code', 'name', 'parent'})
        self.assertEqual(groups[0]['id'], self.mice.id)
        self.assertEqual(groups[0]['url'], self.mice.url)
        self.assertEqual(groups[0]['code'], self.mice.code)
        self.assertEqual(groups[0]['name'], self.mice.name)
        self.assertEqual(groups[0]['parent'], self.mice.parent.id)

    def test_all_fields(self):
        mice = GroupFactory(
            name='mice',
            parent=self.wiki_root,
            code='mice',
            url='mice',
            type=GROUP_TYPE_CHOICES.WIKI,
        )
        GroupMembershipFactory(staff=self.mouse, group=mice)

        url = reverse('user_groups', args=(self.mouse.login, 'json'))
        groups = self.get_json(url=url, fields=['__all__'])

        self.assertEqual(
            set(groups[0].keys()),
            {
                'id', 'url', 'code', 'name', 'parent', 'intranet_status',
                'rght', 'lft', 'description', 'service_id', 'level', 'created_at',
                'modified_at', 'tree_id', 'department', 'position',
                'native_lang', 'externals_count', 'yamoney_count',
                'type', 'role_scope_id', 'yandex_count', 'parent_service_id', 'service_tags',
            }
        )

    def test_type_field(self):
        group_top = GroupFactory(name='top', service_id=None, department=None, parent=self.wiki_root)
        group_mice = GroupFactory(name='mice', service_id=None, department=None, parent=group_top)
        Group.tree.rebuild()

        GroupMembershipFactory(staff=self.mouse, group=group_mice)

        self.url = reverse('user_groups', args=(self.mouse.login, 'json'))
        groups = self.get_json(url=self.url, fields=['id', 'type'])

        self.assertEqual(groups[0]['type'], 'wiki')
        self.assertEqual(groups[1]['type'], 'wiki')


class ApiV1GroupsListTest(ApiV1GroupBaseTest):
    """Список всех групп.
    http://center.yandex-team.ru/api/v1/groups.xml
    """

    def test_all_groups_in_json(self):
        self.url = reverse('groups', args=('json',))
        groups = self.get_json(url=self.url)

        # Одна группа создается при создании департамента для тестового юзера
        self.assertEqual(len(groups), 7 + 1)
        self.assertEqual(
            set(g['id'] for g in groups),
            set(Group.objects.values_list('id', flat=True))
        )


class ApiV1GroupByIdOrUrlTest(ApiV1GroupBaseTest):
    """Информация о группе.
    http://center.yandex-team.ru/api/v1/groups/admins.xml?fields=__all__

    """

    def test_group_404(self):
        url = reverse('group', args=('nonexistent', 'json'))
        response = self.get_page(url=url, fields=['id', 'type'])

        self.assertEqual(response.status_code, 404)

    def test_wiki_group(self):
        url = reverse('group', args=(self.wiki_second_lvl.url, 'json'))
        group = self.get_json(url=url, fields=['id', 'type', 'parent'])

        self.assertEqual(group['type'], 'wiki')
        self.assertEqual(group['id'], self.wiki_second_lvl.id)
        self.assertEqual(group['parent'], self.wiki_first_lvl.id)

    def test_wiki_group_by_id(self):
        url = reverse('group', args=(self.wiki_second_lvl.id, 'json'))
        group = self.get_json(url=url, fields=['id', 'type', 'parent'])

        self.assertEqual(group['type'], 'wiki')
        self.assertEqual(group['id'], self.wiki_second_lvl.id)
        self.assertEqual(group['parent'], self.wiki_first_lvl.id)

    def test_department_group(self):
        url = reverse('group', args=(self.group_yandex.url, 'json'))
        group = self.get_json(url=url, fields=['id', 'type', 'parent'])

        self.assertEqual(group['type'], 'department')
        self.assertEqual(group['id'], self.group_yandex.id)
        self.assertEqual(group['parent'], self.department_root.id)

    def test_service_group(self):
        url = reverse('group', args=(self.group_staff.url, 'json'))
        group = self.get_json(url=url, fields=['id', 'type', 'parent'])

        self.assertEqual(group['type'], 'service')
        self.assertEqual(group['id'], self.group_staff.id)
        self.assertEqual(group['parent'], self.service_root.id)


class ApiV1GroupChildrenTest(ApiV1GroupBaseTest):
    """Группа и ее подгруппы.
    http://center.yandex-team.ru/api/v1/groups/yandex_infra/children.xml

    """

    def setUp(self):
        super(ApiV1GroupChildrenTest, self).setUp()
        self.wiki_second_lvl_second_child = Group.objects.create(
            name='second_second', url='second_second',
            service_id=None, department=None,
            parent=self.wiki_first_lvl,
            created_at=self.wiki_root.created_at,
            modified_at=self.wiki_root.modified_at,
        )


class ApiV1GroupAllMembersTest(ApiV1GroupBaseTest):
    """Состав группы с учетом подгрупп.
    http://center.yandex-team.ru/api/v1/groups/yandex_infra/all_members.xml

    """

    def setUp(self):
        super(ApiV1GroupAllMembersTest, self).setUp()
        self.mouse = StaffFactory(login='mouse')
        self.squirrel = StaffFactory(login='squirrel')
        GroupMembershipFactory(staff=self.mouse, group=self.wiki_first_lvl)
        GroupMembershipFactory(staff=self.squirrel, group=self.wiki_second_lvl)

    def test_group_404(self):
        """Это баг, можно его поправить, если попросят пользователи.
        """
        pass
#        url = reverse('group_all_members', args=('nonexistent', 'json'))
#        response = self.get_page(url=url, fields=['id', 'type'])
#
#        self.assertEqual(response.status_code, 404)

    def test_wiki_group_with_children(self):
        url = reverse('group_all_members', args=(self.wiki_first_lvl.url, 'json'))
        users = self.get_json(url=url, fields=['id', 'login'])

        self.assertEqual(len(users), 2)
        self.assertEqual(users[0]['login'], self.mouse.login)
        self.assertEqual(users[1]['login'], self.squirrel.login)

    def test_wiki_group_without_children(self):
        url = reverse('group_all_members', args=(self.wiki_second_lvl.url, 'json'))
        users = self.get_json(url=url, fields=['id', 'login'])

        self.assertEqual(len(users), 1)
        self.assertEqual(users[0]['login'], self.squirrel.login)
