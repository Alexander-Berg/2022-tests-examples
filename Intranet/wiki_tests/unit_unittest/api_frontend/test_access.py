import json
from unittest import skipIf
from mock import patch

from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Group as DjangoGroup
from ujson import loads

from wiki import access as wiki_access
from wiki.api_core.errors.bad_request import InvalidDataSentError
from wiki.api_core.errors.permissions import UserHasNoAccess
from wiki.api_frontend.serializers.access import AccessData
from wiki.intranet.models import Staff
from wiki.intranet.models.consts import GROUP_TYPE_CHOICES
from wiki.pages.access import yandex_group
from wiki.pages.models import Access
from wiki.users.models import Group
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from wiki.utils.idm import GroupNotFoundInIDM

if settings.IS_INTRANET:
    from wiki.intranet.models import GroupMembership

if settings.IS_BUSINESS:
    from wiki.users.models import GROUP_TYPES

User = get_user_model()


class APIAccessHandlerTest(BaseApiTestCase):
    """
    Test for access api handlers
    """

    def setUp(self):
        super(APIAccessHandlerTest, self).setUp()
        self.setUsers()
        self.setGroupMembers()
        self.client.login('thasonic')
        self.page = self.create_page(
            tag='TestAccess',
            supertag='testaccess',
        )

    def get_request_url(self, page=None):
        page = page or self.page
        return '{api_url}/{supertag}/.access'.format(
            api_url=self.api_url,
            supertag=page.supertag,
        )

    def test_access_handle_auth_get(self):
        """
        test if handle /.access answers correctly to page owner and guest
        """
        # owner GET
        assert_queries = 52 if not settings.WIKI_CODE == 'wiki' else 9
        with self.assertNumQueries(assert_queries):
            response = self.client.get(self.get_request_url())
        self.assertEqual(response.status_code, 200)

        page_data = loads(response.content)
        self.assertTrue('error' not in page_data)

    def test_access_handle_auth_post(self):
        assert_queries = 59 if not settings.WIKI_CODE == 'wiki' else 19
        with self.assertNumQueries(assert_queries):
            response = self.client.post(self.get_request_url(), {'type': 'owner'})
        self.assertEqual(response.status_code, 200)
        page_data = loads(response.content)
        self.assertTrue('error' not in page_data)

    def test_access_guest_get(self):
        self.client.login('kolomeetz')

        response = self.client.get(self.get_request_url())
        self.assertEqual(response.status_code, 200)

        page_data = loads(response.content)
        self.assertTrue('error' not in page_data)

    def test_access_guest_post(self):
        self.client.login('kolomeetz')

        response = self.client.post(self.get_request_url(), {'type': 'owner'})
        self.assertEqual(response.status_code, 403)

        page_data = loads(response.content)
        self.assertTrue('error' in page_data)
        self.assertEqual(page_data['error']['error_code'], UserHasNoAccess.error_code)

    def test_get_common_access_data(self):
        child = self.create_page(
            tag='TestAccess/child',
            supertag='testaccess/child',
        )

        assert_queries = 53 if not settings.WIKI_CODE == 'wiki' else 10
        with self.assertNumQueries(assert_queries):
            response = self.client.get(self.get_request_url(child))
        page_data = loads(response.content)['data']

        self.assertEqual(page_data['type'], 'inherited')
        self.assertEqual(page_data['parent_access'], 'common')

    def test_get_owner_access_data(self):
        wiki_access.set_access(self.page, wiki_access.TYPES.OWNER, self.user_thasonic)

        assert_queries = 52 if not settings.WIKI_CODE == 'wiki' else 9
        with self.assertNumQueries(assert_queries):
            response = self.client.get(self.get_request_url())
        page_data = loads(response.content)['data']

        self.assertEqual(page_data['type'], 'owner')

    def test_dupes(self):
        if settings.IS_INTRANET:
            y_g = yandex_group()
            for i in range(5):
                Access.objects.create(staff=self.user_kolomeetz.staff, page=self.page)
                Access.objects.create(staff=self.user_volozh.staff, page=self.page)
                Access.objects.create(group=y_g, page=self.page)
            # n/a
            p = AccessData(self.page)
            self.assertEqual(set(p.restrictions['users']), {self.user_kolomeetz, self.user_volozh})
            self.assertEqual(p.restrictions['groups'], [y_g])

    def test_get_inherited_access_data(self):
        wiki_access.set_access(self.page, wiki_access.TYPES.OWNER, self.user_thasonic)

        child = self.create_page(
            tag='TestAccess/child',
            supertag='testaccess/child',
        )

        assert_queries = 55 if not settings.WIKI_CODE == 'wiki' else 12
        with self.assertNumQueries(assert_queries):
            r = self.client.get(self.get_request_url(child))
        page_data = loads(r.content)['data']

        self.assertEqual(page_data['type'], 'inherited')
        self.assertEqual(page_data['parent_access'], 'owner')

    def test_get_restricted_access_data(self):

        # let's build some robots
        robo1 = self.get_or_create_user('_rpc_zapp')
        robo1.staff.is_robot = True
        robo2 = self.get_or_create_user('_rpc_kif')
        robo2.staff.is_robot = True
        robo3 = self.get_or_create_user('_rpc_scruffy')
        robo3.staff.is_robot = True

        group = yandex_group() if settings.IS_INTRANET else Group.objects.get(name='admins')
        params = {
            'staff_models': [
                self.user_volozh.staff,
                self.user_asm.staff,
                self.user_kolomeetz.staff,
                robo1.staff,
                robo2.staff,
                robo3.staff,
            ],
            'groups': [group],
        }

        wiki_access.set_access(self.page, wiki_access.TYPES.RESTRICTED, self.user_thasonic, **params)

        r = self.client.get(self.get_request_url())
        page_data = loads(r.content)['data']

        self.assertEqual(page_data['type'], 'restricted')
        if settings.IS_INTRANET:
            self.assertIn('opened_to_external_flag', page_data)
        else:
            self.assertNotIn('opened_to_external_flag', page_data)

        restrictions = page_data['restrictions']
        response_uids = sorted([int(user['uid']) for user in restrictions['users']])
        response_gids = sorted([int(grp['id']) for grp in restrictions['groups']])

        sent_uids = sorted([int(user.uid) for user in params['staff_models']])
        sent_gids = sorted([int(grp.id) for grp in params['groups']])

        self.assertEqual(response_uids, sent_uids, 'Flaky test ' + json.dumps(restrictions))
        self.assertEqual(response_gids, sent_gids)

    def test_common_access_for_page(self):
        p = self.create_page(
            tag='TestAccess',
            supertag='testaccess',
        )

        request_url = '{api_url}/{supertag}/.access'.format(api_url=self.api_url, supertag=p.supertag)
        response = self.client.get(request_url)
        page_data = loads(response.content)['data']

        self.assertEqual(page_data['type'], 'common')
        self.assertEqual(page_data['parent_access'], None)

    def test_set_invalid_restricted_access(self):
        self.create_page(
            tag='TestAccess',
            supertag='testaccess',
        )

        response = self.client.post(
            '/_api/frontend/testaccess/.access',
            dict(
                type='restricted',
            ),
        )

        self.assertEqual(409, response.status_code)
        content = loads(response.content)['error']

        self.assertEqual(InvalidDataSentError.error_code, content['error_code'])

    def test_set_restricted_access_for_registered_user(self):
        staff = Staff.objects.all()[0]
        staff.uid = 100500
        staff.save()

        if settings.IS_INTRANET:
            employee_group = DjangoGroup.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
            employee_group.user_set.add(staff.user)

        response = self.client.post(
            '/_api/frontend/testaccess/.access',
            dict(
                type='restricted',
                users=[staff.uid],  # просто uid некоторого пользователя
            ),
        )

        self.assertEqual(200, response.status_code)

        content = loads(response.content)['data']
        self.assertEqual(1, len(content['restrictions']['users']))
        self.assertEqual(100500, content['restrictions']['users'][0]['uid'])
        self.assertEqual(0, len(content['restrictions']['groups']))
        self.assertEqual('restricted', content['type'])

    if settings.IS_INTRANET:

        def test_set_restricted_access_for_unregistered_user(self):
            staff = self.user_kolomeetz.staff
            staff.uid = 100500
            staff.save()

            employee_group = DjangoGroup.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
            employee_group.user_set.remove(self.user_thasonic)
            employee_group.user_set.remove(self.user_kolomeetz)

            outstaff_manager_group = DjangoGroup.objects.create(name=settings.IDM_ROLE_OUTSTAFF_MANAGER_GROUP_NAME)

            with self.assertNumQueries(20):
                response = self.client.post(
                    '/_api/frontend/testaccess/.access',
                    dict(
                        type='restricted',
                        users=[staff.uid],  # просто uid некоторого пользователя
                    ),
                )

            self.assertEqual(200, response.status_code)

            content = loads(response.content)['data']
            self.assertIsNone(content['restrictions'])
            self.assertEqual(1, len(content['rejects']['users']))
            self.assertEqual(100500, content['rejects']['users'][0]['uid'])
            self.assertEqual(0, len(content['rejects']['groups']))
            self.assertEqual('common', content['type'])
            self.assertEqual(
                'https://idm.yandex-team.ru/system/wiki-test/roles#'
                'rf=1,rf-role=x#thasonic@wiki-test/group-%d;;;,rf-expanded=x,sort-by=name' % outstaff_manager_group.id,
                content['request_role_url'],
            )

        def test_set_restricted_access_for_unregistered_user_by_privileged_user(self):
            staff = Staff.objects.all()[0]
            staff.uid = 100500
            staff.save()

            outstaff_manager_group = DjangoGroup.objects.create(name=settings.IDM_ROLE_OUTSTAFF_MANAGER_GROUP_NAME)
            outstaff_manager_group.user_set.add(self.user_thasonic)

            with self.assertNumQueries(24):
                response = self.client.post(
                    '/_api/frontend/testaccess/.access',
                    dict(
                        type='restricted',
                        users=[staff.uid],  # просто uid некоторого пользователя
                    ),
                )

            self.assertEqual(200, response.status_code)

            content = loads(response.content)['data']
            self.assertEqual(1, len(content['restrictions']['users']))
            self.assertEqual(100500, content['restrictions']['users'][0]['uid'])
            self.assertEqual(0, len(content['restrictions']['groups']))
            self.assertEqual('restricted', content['type'])

        @patch('wiki.access.get_group_roles', lambda group: [])
        def test_set_restricted_access_with_the_same_rejected_users_and_groups(self):
            staff = Staff.objects.all()[0]
            group = Group.objects.filter(type=GROUP_TYPE_CHOICES.DEPARTMENT)[0]

            outstaff_manager_group = DjangoGroup.objects.create(name=settings.IDM_ROLE_OUTSTAFF_MANAGER_GROUP_NAME)
            outstaff_manager_group.user_set.add(self.user_thasonic)

            response = self.client.post(
                '/_api/frontend/testaccess/.access',
                dict(
                    type='restricted',
                    users=[staff.uid],
                    groups=[group.id],
                ),
            )
            self.assertEqual(200, response.status_code)
            content = loads(response.content)['data']

            employee_group = DjangoGroup.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
            employee_group.user_set.remove(staff.user)
            outstaff_manager_group.user_set.remove(self.user_thasonic)

            staff_new = Staff.objects.all()[1]
            group_new = Group.objects.filter(type=GROUP_TYPE_CHOICES.SERVICE)[0]

            response = self.client.post(
                '/_api/frontend/testaccess/.access',
                dict(type='restricted', users=[staff.uid, staff_new.uid], groups=[group.id, group_new.id]),
            )

            self.assertEqual(200, response.status_code)

            content = loads(response.content)['data']
            self.assertEqual(2, len(content['restrictions']['users']))
            self.assertEqual(0, len(content['rejects']['users']))
            self.assertEqual(2, len(content['restrictions']['groups']))
            self.assertEqual(0, len(content['rejects']['groups']))

        @patch('wiki.access.get_group_roles', lambda group: [])
        def test_set_restricted_access_with_new_rejected_users_and_groups(self):
            staff = Staff.objects.all()[0]
            group = Group.objects.filter(type=GROUP_TYPE_CHOICES.DEPARTMENT)[0]

            DjangoGroup.objects.create(name=settings.IDM_ROLE_OUTSTAFF_MANAGER_GROUP_NAME)

            employee_group = DjangoGroup.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
            employee_group.user_set.remove(staff.user)

            response = self.client.post(
                '/_api/frontend/testaccess/.access',
                dict(type='restricted', users=[staff.uid], groups=[group.id]),
            )

            self.assertEqual(200, response.status_code)

            content = loads(response.content)['data']
            self.assertIsNone(content['restrictions'])
            self.assertEqual(1, len(content['rejects']['users']))
            self.assertEqual(staff.uid, str(content['rejects']['users'][0]['uid']))
            self.assertEqual(1, len(content['rejects']['groups']))
            self.assertEqual(group.id, content['rejects']['groups'][0]['id'])

        @patch('wiki.access.get_group_roles', lambda group: [])
        def test_anyone_can_remove_outstaff(self):
            staff_a, staff_b = Staff.objects.all()[:2]

            outstaff_manager_group = DjangoGroup.objects.create(name=settings.IDM_ROLE_OUTSTAFF_MANAGER_GROUP_NAME)
            outstaff_manager_group.user_set.add(self.user_thasonic)

            employee_group = DjangoGroup.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
            employee_group.user_set.remove(staff_a.user)
            employee_group.user_set.remove(staff_b.user)

            response = self.client.post(
                '/_api/frontend/testaccess/.access',
                dict(
                    type='restricted',
                    users=[staff_a.uid, staff_b.uid],
                    groups=[],
                ),
            )
            self.assertEqual(200, response.status_code)

            outstaff_manager_group.user_set.remove(self.user_thasonic)

            response = self.client.post(
                '/_api/frontend/testaccess/.access',
                dict(
                    type='restricted',
                    users=[staff_a.uid],
                    groups=[],
                ),
            )

            self.assertEqual(200, response.status_code)

            content = loads(response.content)['data']
            self.assertEqual(1, len(content['restrictions']['users']))
            self.assertEqual(0, len(content['rejects']['users']))

        def test_update_permissions_with_deleted_person(self):
            staff_a, staff_b, staff_c = Staff.objects.all()[:3]

            outstaff_manager_group = DjangoGroup.objects.create(name=settings.IDM_ROLE_OUTSTAFF_MANAGER_GROUP_NAME)
            outstaff_manager_group.user_set.add(self.user_thasonic)

            # Удаленные пользователи не должны мешать редактированию прав (WIKI-15048)

            with patch('wiki.access.get_group_roles', side_effect=lambda x: []):
                response = self.client.post(
                    '/_api/frontend/testaccess/.access',
                    dict(
                        type='restricted',
                        users=[staff_a.uid, staff_b.uid],
                        groups=[],
                    ),
                )
                self.assertEqual(200, response.status_code)
                staff_a.is_dismissed = True
                staff_b.is_dismissed = True

                staff_a.save()
                staff_b.save()

                response = self.client.post(
                    '/_api/frontend/testaccess/.access',
                    dict(
                        type='restricted',
                        users=[staff_c.uid],
                        groups=[],
                    ),
                )
                self.assertEqual(200, response.status_code)

                response = self.client.get(
                    '/_api/frontend/testaccess/.access',
                )
                content = loads(response.content)['data']
                self.assertEqual(1, len(content['restrictions']['users']))
                self.assertEqual(staff_c.uid, str(content['restrictions']['users'][0]['uid']))

        def test_update_permissions_with_deleted_group(self):
            group_1, group_2 = Group.objects.filter(type=GROUP_TYPE_CHOICES.DEPARTMENT)[:2]
            staff = Staff.objects.all()[0]

            outstaff_manager_group = DjangoGroup.objects.create(name=settings.IDM_ROLE_OUTSTAFF_MANAGER_GROUP_NAME)
            outstaff_manager_group.user_set.add(self.user_thasonic)

            def mock_get_group_roles(group):
                if group_2 == group:
                    raise GroupNotFoundInIDM(group_id=group.id, group_name=group.name, message=':)', id=group.id)

                print(group.id)
                return []

            # 1. нельзя добавлять удаленные группы
            with patch('wiki.access.get_group_roles', side_effect=mock_get_group_roles):
                response = self.client.post(
                    '/_api/frontend/testaccess/.access',
                    dict(
                        type='restricted',
                        users=[staff.uid],
                        groups=[group_1.id, group_2.id],
                    ),
                )
                self.assertEqual(409, response.status_code)

            # 2. при этом, если группа удалена можно редактировать
            with patch('wiki.access.get_group_roles', side_effect=lambda x: []):
                response = self.client.post(
                    '/_api/frontend/testaccess/.access',
                    dict(
                        type='restricted',
                        users=[],
                        groups=[group_2.id],
                    ),
                )
                self.assertEqual(200, response.status_code)

            # 2.1 можно добавлять новые группы с сохранением уже существующих удаленных
            with patch('wiki.access.get_group_roles', side_effect=mock_get_group_roles):
                response = self.client.post(
                    '/_api/frontend/testaccess/.access',
                    dict(
                        type='restricted',
                        users=[staff.uid],
                        groups=[group_1.id, group_2.id],
                    ),
                )
                self.assertEqual(200, response.status_code)
                response = self.client.get(
                    '/_api/frontend/testaccess/.access',
                )
                content = loads(response.content)['data']
                self.assertEqual(2, len(content['restrictions']['groups']))
                group_ids = [group['id'] for group in content['restrictions']['groups']]
                self.assertTrue(group_1.id in group_ids)
                self.assertTrue(group_2.id in group_ids)

            # 2.2 можно добавлять новые без удаленных
            with patch('wiki.access.get_group_roles', side_effect=mock_get_group_roles):
                response = self.client.post(
                    '/_api/frontend/testaccess/.access',
                    dict(
                        type='restricted',
                        users=[staff.uid],
                        groups=[group_1.id],
                    ),
                )
                self.assertEqual(200, response.status_code)
                response = self.client.get(
                    '/_api/frontend/testaccess/.access',
                )
                content = loads(response.content)['data']
                self.assertEqual(1, len(content['restrictions']['groups']))
                self.assertEqual(group_1.id, content['restrictions']['groups'][0]['id'])

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_check_write_access_for_admins(self):
        page = self.create_page(
            tag='TestAccess',
            supertag='testaccess',
        )
        request_url = '{api_url}/{supertag}/.access'.format(api_url=self.api_url, supertag=page.supertag)

        r = self.client.post(request_url, {'type': 'owner'})
        self.assertEqual(r.status_code, 200)

        self.client.login('kolomeetz')

        if settings.IS_BUSINESS:
            group = Group(
                dir_id='1',
                name='Admins',
                group_dir_type='organization_admin',
                group_type=GROUP_TYPES.group,
                label='admins',
                title='admins',
            )
        elif settings.IS_INTRANET:
            wiki = Group(
                name='__wiki__', url='__wiki__', created_at='2012-02-24 16:54:00', modified_at='2012-02-24 16:54:00'
            )
            wiki.save()

            group = Group(
                name='Admins',
                parent=wiki,
                url='admins',
                created_at='2012-02-24 16:54:00',
                id=settings.WIKI_ADMIN_GROUP_ID,
                modified_at='2012-02-24 16:54:00',
            )
        else:
            wiki = Group(name='__wiki__')
            wiki.save()

            group = Group(name='Admins')

        group.save()
        if settings.IS_INTRANET:
            GroupMembership(staff=self.user_kolomeetz.staff, group=group).save()
        else:
            group.user_set.add(self.user_kolomeetz)

        page_data = {'title': 'Title', 'body': ''}

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=self.page.supertag)
        response = self.client.post(request_url, data=page_data)
        # POST
        self.assertEqual(200, response.status_code)

    def test_check_access_for_authors_only(self):
        request_url = '{api_url}/{supertag}/.access'.format(api_url=self.api_url, supertag=self.page.supertag)

        r = self.client.post(request_url, {'type': 'owner'})
        self.assertEqual(r.status_code, 200)

        self.client.login('kolomeetz')

        page_data = {'title': 'Title', 'body': 'text'}

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=self.page.supertag)

        response = self.client.get(request_url)
        self.assertEqual(403, response.status_code)

        response = self.client.post(request_url, data=page_data)
        self.assertEqual(403, response.status_code)

        self.page.authors.add(self.user_kolomeetz)

        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)

        response = self.client.post(request_url, data=page_data)
        self.assertEqual(200, response.status_code)
