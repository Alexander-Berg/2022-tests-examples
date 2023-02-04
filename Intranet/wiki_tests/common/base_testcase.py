import json
from datetime import datetime
from io import StringIO

from django.db import connection
from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Group as DjangoGroup
from django.core.cache import caches
from django.db import reset_queries
from django.utils import timezone
from django_replicated.utils import routers
from mock import patch
from pretend import stub

from wiki.sync.connect import OPERATING_MODE_NAMES
from wiki.sync.connect.injector import absorb, get_from_thread_store, is_in_thread_store
from wiki.sync.connect.models import OperatingMode
from wiki.sync.connect.org_ctx import _ORG_CTX_KEY
from wiki.intranet.models import Staff
from wiki.notifications.models import PageEvent, make_pageevent_storage_id
from wiki.pages.models import AccessRequest, Page
from wiki.pages.signals.access import invalidate_cache
from wiki.personalisation.user_cluster import create_personal_page
from wiki.users.logic import set_user_setting
from wiki.utils.storage import STORAGE
from wiki.utils.timezone import make_aware_current
from intranet.wiki.tests.wiki_tests.common.data_helper import open_json_fixture
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get, new
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.utils import only_model_fields
from intranet.wiki.tests.wiki_tests.common.rest_api_client import RestApiClient
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase

if settings.IS_BUSINESS:
    from wiki.users_biz.models import GROUP_TYPES, Group
else:
    from wiki.intranet.models import Department, DepartmentStaff, GroupMembership
    from wiki.users_ynx.models import Group

User = get_user_model()
patched_wiki_group = patch('wiki.pages.access.groups.get_wiki_service_group', lambda: stub(all_members=[]))


class BaseTestCase(FixtureMixin, WikiDjangoTestCase):
    """
    Sets up all the needed data
    """

    departments_set = False
    offices_set = False
    groups_set = False
    users_set = False
    groupmembers_set = False
    pages_set = False
    page_events_set = False
    access_requests_set = False

    client_class = RestApiClient

    supertag = 'homepage'

    @classmethod
    def setUpClass(cls):
        cls.marked = True
        patched_wiki_group.start()
        super(BaseTestCase, cls).setUpClass()

    @classmethod
    def tearDownClass(cls):
        cls.marked = False
        super(BaseTestCase, cls).tearDownClass()
        patched_wiki_group.stop()

    def login(self, user_login):
        user = self.get_or_create_user(user_login)
        self.client.login(user)
        return user

    def logout(self):
        self.client.logout()

    def setUp(self):
        # Грязный хак чтобы сбросить состояние роутера реплики
        # Скорее всего это надо перенести ReplicationMiddleware в process_response()
        routers._context.state_stack = []
        if settings.IS_INTRANET:
            self.setGroups()
        self.client.logout()
        if settings.IS_BUSINESS:
            self.setOrganization()
            # Выполняем тест в контексте организации
            org = self.get_or_create_org(dir_id='42')
            if not is_in_thread_store(_ORG_CTX_KEY):
                absorb(_ORG_CTX_KEY, [])
            ctx = get_from_thread_store(_ORG_CTX_KEY)
            ctx.append(org)

        super(BaseTestCase, self).setUp()

    def run(self, result=None):
        # Патчим походы в Склонятор. Не удалось замокать через cassetes, сделал через @patch.
        with patch('wiki.legacy.metainflect.get_inflect_form', lambda *args, **kwargs: ''):
            super(BaseTestCase, self).run(result)

    def tearDown(self):
        super(BaseTestCase, self).tearDown()
        from wiki.pages.access.groups import drop_admin_membership_cache

        drop_admin_membership_cache()
        caches['access_rights'].clear()
        caches['user_groups'].clear()
        from wiki.pages.access import groups

        groups._yandex_group = None
        groups._yamoney_group = None
        reset_queries()

        if settings.IS_BUSINESS and is_in_thread_store(_ORG_CTX_KEY):
            ctx = get_from_thread_store(_ORG_CTX_KEY)
            if len(ctx) > 0:
                ctx.pop()

    def setDepartments(self):
        if not settings.IS_INTRANET:
            return

        if self.departments_set:
            return
        self.departments_set = True
        department_data = open_json_fixture('department.json', dateaware=['modified_at'])
        for department in department_data:
            fields = department['fields']

            if fields['parent']:
                try:
                    fields['parent'] = Department.objects.get(id=fields['parent'])
                except Department.DoesNotExist:
                    print(('Department does not exist: ', fields))
            d = new(Department, id=department['pk'], **fields)
            d.save_base()
        Department.tree.rebuild()

    def setOffices(self):
        if self.offices_set:
            return
        self.offices_set = True

    def setOrganization(self):
        OperatingMode.objects.get_or_create(name=OPERATING_MODE_NAMES.free)
        self.org_42 = self.get_or_create_org(dir_id='42')

    def setUsers(self, use_legacy_subscr_favor=False):
        if self.users_set:
            return

        self.users_set = True
        self.setDepartments()
        staff_data = open_json_fixture('staff.json', dateaware=['modified_at'])
        kwargs = staff_data[0]['fields']
        kwargs['user'] = None
        if kwargs['department'] and settings.IS_INTRANET:
            kwargs['department'] = Department.objects.get(id=kwargs['department'])
        kwargs['lang_ui'] = kwargs['lang_content'] = 'en'
        staff = get(Staff, id=staff_data[0]['pk'], **only_model_fields(kwargs, Staff))
        self.user_thasonic = User.objects.create_user('thasonic', 'thasonic@yandex-team.ru', '1')
        self.user_thasonic.has_personal_cluster = self.has_personal_cluster
        self.user_thasonic.profile['use_nodejs_frontend'] = False
        self.user_thasonic.save()
        staff.user = self.user_thasonic
        staff.save()

        kwargs = staff_data[1]['fields']
        kwargs['user'] = None
        if kwargs['department'] and settings.IS_INTRANET:
            kwargs['department'] = Department.objects.get(id=kwargs['department'])
        kwargs['lang_ui'] = kwargs['lang_content'] = 'en'
        staff = get(Staff, id=staff_data[1]['pk'], **only_model_fields(kwargs, Staff))
        self.user_kolomeetz = User.objects.create_user('kolomeetz', 'kolomeetz@yandex-team.ru', '1')
        self.user_kolomeetz.has_personal_cluster = self.has_personal_cluster
        self.user_kolomeetz.save()
        self.user_kolomeetz.profile['use_nodejs_frontend'] = False
        self.user_kolomeetz.save()
        staff.user = self.user_kolomeetz
        staff.native_lang = 'ru'
        staff.save()

        kwargs = staff_data[2]['fields']
        kwargs['user'] = None
        if kwargs['department'] and settings.IS_INTRANET:
            kwargs['department'] = Department.objects.get(id=kwargs['department'])
        kwargs['lang_ui'] = kwargs['lang_content'] = 'en'
        staff = get(Staff, id=staff_data[2]['pk'], **only_model_fields(kwargs, Staff))
        self.user_chapson = User.objects.create_user('chapson', 'chapson@yandex-team.ru', '1')
        self.user_chapson.has_personal_cluster = self.has_personal_cluster
        self.user_chapson.save()
        self.user_chapson.profile['use_nodejs_frontend'] = False
        self.user_chapson.save()
        staff.user = self.user_chapson
        staff.native_lang = 'ru'
        staff.save()

        kwargs = staff_data[3]['fields']
        kwargs['user'] = None
        if kwargs['department'] and settings.IS_INTRANET:
            kwargs['department'] = Department.objects.get(id=kwargs['department'])
        kwargs['lang_ui'] = kwargs['lang_content'] = 'en'
        staff = get(Staff, id=staff_data[3]['pk'], **only_model_fields(kwargs, Staff))
        self.user_asm = User.objects.create_user('asm', 'asm@yandex-team.ru', '1')
        self.user_asm.has_personal_cluster = self.has_personal_cluster
        self.user_asm.save()
        self.user_asm.profile['use_nodejs_frontend'] = False
        self.user_asm.save()
        staff.user = self.user_asm
        staff.native_lang = 'ru'
        staff.save()

        kwargs = staff_data[4]['fields']
        kwargs['user'] = None
        if kwargs['department'] and settings.IS_INTRANET:
            kwargs['department'] = Department.objects.get(id=kwargs['department'])
        kwargs['lang_ui'] = kwargs['lang_content'] = 'en'
        staff = get(Staff, id=staff_data[4]['pk'], **only_model_fields(kwargs, Staff))
        self.user_volozh = User.objects.create_user('volozh', 'volozh@yandex-team.ru', '1')
        self.user_volozh.has_personal_cluster = self.has_personal_cluster
        self.user_volozh.save()
        self.user_volozh.profile['use_nodejs_frontend'] = False
        self.user_volozh.save()
        staff.user = self.user_volozh
        staff.native_lang = 'ru'
        staff.save()

        if use_legacy_subscr_favor:  # После миграции подписок и закладок на новую модель WIKI-16858
            for user in [self.user_thasonic, self.user_kolomeetz, self.user_chapson, self.user_asm, self.user_volozh]:
                set_user_setting(user, 'new_favorites', False)
                set_user_setting(user, 'new_subscriptions', False)

        if settings.IDM_ENABLED:
            group_name = settings.IDM_ROLE_EMPLOYEE_GROUP_NAME
            employee_group = DjangoGroup.objects.create(name=group_name)
            self.user_thasonic.groups.add(employee_group)
            self.user_kolomeetz.groups.add(employee_group)
            self.user_chapson.groups.add(employee_group)
            self.user_asm.groups.add(employee_group)
            self.user_volozh.groups.add(employee_group)

        if settings.IS_BUSINESS:
            self.user_thasonic.orgs.add(self.org_42)
            self.user_kolomeetz.orgs.add(self.org_42)
            self.user_chapson.orgs.add(self.org_42)
            self.user_asm.orgs.add(self.org_42)
            self.user_volozh.orgs.add(self.org_42)

        if self.create_user_clusters:
            create_personal_page(self.user_thasonic)
            create_personal_page(self.user_kolomeetz)
            create_personal_page(self.user_chapson)
            create_personal_page(self.user_volozh)
            create_personal_page(self.user_asm)

    def setExternalMember(self):
        self.setGroups()
        kwargs = {
            'user': None,
            'department': Department.objects.get(id=1122),  # ext_test,
            'id': 100500,
        }
        staff = get(Staff, login='snegovoy', **kwargs)
        staff.is_robot = True
        self.user_snegovoy = User.objects.create_user('snegovoy', 'snegovoy@yandex-team.com.ua')
        self.user_snegovoy.save()
        staff.user = self.user_snegovoy
        staff.save()
        get(GroupMembership, group=Group.objects.get(url='ext_test'), staff=staff)

    def setGroups(self):
        if self.groups_set:
            return
        self.groups_set = True
        if settings.IS_INTRANET:
            return self.set_groups_intranet()
        elif settings.IS_BUSINESS:
            return self.set_groups_business()
        else:
            return self.set_groups_extranet()

    def set_groups_intranet(self):
        self.setDepartments()
        group_data = open_json_fixture('group.json', dateaware=['modified_at'])
        parent_child_map = {}
        for group in group_data:
            fields = group['fields']
            if fields['parent']:
                fields['parent'] = parent_child_map[fields['parent']]
            if fields['department']:
                fields['department'] = Department.objects.get(id=fields['department'])
            if 'pk' in group:
                fields['id'] = group['pk']
            g = new(Group, **fields)
            parent_child_map[group['pk']] = g
            g.save_base()
            setattr(self, str('group_' + g.url), g)
        Group._tree_manager.rebuild()

        with connection.cursor() as cursor:
            cursor.execute('ALTER SEQUENCE intranet_group_id_seq RESTART 1000;')

    def set_groups_extranet(self):
        Group.objects.create(name='admins')
        Group.objects.create(name='school')
        Group.objects.create(name='employees')
        Group.objects.create(name='school РУС')

    def set_groups_business(self):
        Group.objects.create(
            name='admins',
            label='admins',
            title='admins',
            dir_id='1',
            group_type=GROUP_TYPES.group,
            group_dir_type='organization_admin',
        )
        Group.objects.create(name='school', label='school', title='school', dir_id='2', group_type=GROUP_TYPES.group)
        Group.objects.create(
            name='employees', label='employees', title='employees', dir_id='3', group_type=GROUP_TYPES.group
        )
        Group.objects.create(
            name='school РУС', label='school РУС', title='school РУС', dir_id='4', group_type=GROUP_TYPES.group
        )

    def setGroupMembers(self):
        if self.groupmembers_set:
            return
        self.groupmembers_set = True

        self.setGroups()
        self.setUsers()
        if settings.IS_INTRANET:
            return self.set_group_members_intranet()
        else:
            return self.set_group_members_extranet()

    def set_group_members_intranet(self):
        group = Group.objects.get(url='yandex_mnt')
        get(GroupMembership, group=group, staff=self.user_thasonic.staff)
        get(GroupMembership, group=group, staff=self.user_kolomeetz.staff)
        group = Group.objects.get(url='yandex_mnt_srv')
        get(GroupMembership, group=group, staff=self.user_thasonic.staff)

    def set_group_members_extranet(self):
        Group.objects.get(name='school').user_set.add(self.user_thasonic.id)

    def setPages(self):
        self.setUsers()
        if self.pages_set:
            return
        self.pages_set = True
        pages_data = open_json_fixture('pages.json', dateaware=['created_at'])

        for page in pages_data:
            if not page['fields'].get('page_type'):
                page['fields']['page_type'] = Page.TYPES.PAGE
            if not page['fields'].get('authors_to_add'):
                page['fields']['authors_to_add'] = []
            else:
                page['fields']['authors_to_add'] = [
                    getattr(self, 'user_' + author_name) for author_name in page['fields']['authors_to_add']
                ]
            if not page['fields'].get('last_author'):
                page['fields']['last_author'] = None
            else:
                page['fields']['last_author'] = getattr(self, 'user_' + page['fields']['last_author'])
            page['fields']['data'] = ''

            # Метод save ожидает атрибут modified_at обязательно в виде datetime, чтобы сравнить с modified_at_for_index
            page['fields']['modified_at'] = make_aware_current(
                datetime.strptime(page['fields']['modified_at'], '%Y-%m-%d %H:%M:%S')
            )

            org_tmp = page['fields'].get('org_id', None)
            if org_tmp:
                del page['fields']['org_id']

            p = get(Page, id=None, **only_model_fields(page['fields'], Page))

            if settings.IS_BUSINESS and org_tmp:
                p.org = getattr(self, 'org_' + org_tmp)

            # поля из key-value хранилища
            if 'body' in page['fields']:
                p.body = page['fields']['body']
                p.keywords = page['fields']['keywords'] if 'keywords' in page['fields'] else ''
                p.description = page['fields']['description'] if 'description' in page['fields'] else ''
                p.save()
            p.authors.add(*page['fields']['authors_to_add'])
        redirect = Page.objects.get(supertag='testinfo/redirectpage')
        page = Page.objects.get(supertag='destination/testinfo/testinfogem')
        redirect.redirects_to = page
        redirect.save()
        # сбросить кеш прав доступа
        invalidate_cache(None, Page.objects.all())

    def setAccessRequests(self):
        if not settings.IS_INTRANET:
            return
        if self.access_requests_set:
            return
        self.access_requests_set = True
        page = Page.objects.get(supertag='homepage')
        self.setUsers()
        self.setPages()
        get(
            AccessRequest,
            id=12,
            applicant=self.user_kolomeetz,
            page=page,
            reason='give me access, I am Santa!',
            verdict=False,
            verdict_by=self.user_thasonic,
            created_at=timezone.now(),
            verdict_reason='No, Santa Clauses are not allowed to view wiki pages',
        )
        get(
            AccessRequest,
            id=13,
            applicant=self.user_kolomeetz,
            page=page,
            reason='give me access, I am Alice',
            verdict=True,
            verdict_by=self.user_thasonic,
            verdict_reason='Yes, Alice may view the page',
            created_at=timezone.now(),
        )
        get(
            AccessRequest,
            id=13,
            applicant=self.user_kolomeetz,
            page=page,
            created_at=timezone.now(),
            reason='give me access, I am Konstantin Kolomeetz',
            verdict=None,
            verdict_by=None,
            verdict_reason=None,
        )

    def setPageEvents(self):
        if self.page_events_set:
            return
        self.page_events_set = True
        self.setUsers()
        self.setPages()
        self.setGroupMembers()
        self.setAccessRequests()

        events_data = open_json_fixture('page_events.json', dateaware=['timeout', 'sent_at'])
        groups_data = open_json_fixture('group.json', dateaware=['modified_at'])
        page = Page.objects.get(supertag=self.supertag)
        for data in events_data:
            if 'group_id' in data['fields']['meta']:
                for group in groups_data:
                    meta = json.loads(data['fields']['meta'])
                    if meta['group_id'] == group['pk']:
                        if settings.IS_INTRANET:
                            meta['group_id'] = Group.objects.get(url=group['fields']['url']).id
                        data['fields']['meta'] = json.dumps(meta)
                        break
            if data['fields']['meta'] == 'null':
                data['fields']['meta'] = '{}'
            storage_id = STORAGE.save('tests:' + make_pageevent_storage_id(), StringIO(data['fields']['meta']))
            data['fields']['meta'] = storage_id

            kwargs = data['fields']
            kwargs['author'] = self.user_kolomeetz
            kwargs['page'] = page
            get(PageEvent, id=data['pk'], **kwargs)

    def getNumEvents(self, event_type_str=None, author_login=None, supertag=None):
        """
        Count page events in database. The events can be
        filtered by the type, the author of event and the related page.

        > num_events = self.getNumEvents('watch', 'thasonic', 'xx/yy')

        """
        queryset = PageEvent.objects.all()
        if event_type_str is not None:
            event_type = getattr(PageEvent.EVENT_TYPES, event_type_str)
            queryset = queryset.filter(event_type=event_type)
        if author_login is not None:
            author = User.objects.get(username=author_login)
            queryset = queryset.filter(author=author)
        if supertag is not None:
            page = Page.objects.get(supertag=supertag)
            queryset = queryset.filter(page=page)
        return queryset.count()

    def convert_user_to_external(self, user, ext_department=None, ext_group=None):
        """
        Convert user to the user of external department

        @rtype user: Staff
        @param user: user profile

        @rtype ext_department: Department
        @param ext_department: external department
        """

        ext_department = ext_department or Department.objects.get(url='ext_test')
        ext_group = ext_group or Group.objects.get(url='ext_test')

        # mock external user
        (GroupMembership.objects.filter(staff=user).delete())

        (DepartmentStaff.objects.filter(staff=user).delete())

        get(GroupMembership, group=ext_group, staff=user)

        ext_group.externals_count += 1
        ext_group.save()

        get(DepartmentStaff, department=ext_department, staff=user)

        user.department = ext_department
        user.save()

        self.assertTrue(user.is_external_employee)
