import json
from uuid import UUID

import pytest
from dataclasses import dataclass, fields
from django.conf import settings
from django.contrib.auth.models import Group as DjangoGroup
from django.db import connection

from wiki.acl.consts import ParentAclType, AclDiff
from wiki.acl.models import Acl
from wiki.inline_grids import logic as grids_logic
from wiki.intranet.models import Staff
from wiki.pages.models import Page
from wiki.sync.connect import OPERATING_MODE_NAMES
from wiki.sync.connect.base_organization import as_base_organization
from wiki.sync.connect.models import OperatingMode, Organization
from wiki.sync.connect.org_ctx import org_ctx
from wiki.uploads.consts import UploadSessionTargetType
from wiki.users.logic import set_user_setting
from wiki.users.models import Group
from wiki.users.models import User
from intranet.wiki.tests.wiki_tests.common import grid_helper
from intranet.wiki.tests.wiki_tests.common.data_helper import open_json_fixture
from intranet.wiki.tests.wiki_tests.common.factories.cloud_page import CloudPageFactory
from intranet.wiki.tests.wiki_tests.common.factories.file import FileFactory
from intranet.wiki.tests.wiki_tests.common.factories.grid import GridFactory
from intranet.wiki.tests.wiki_tests.common.factories.group import GroupFactory
from intranet.wiki.tests.wiki_tests.common.factories.inline_grid import InlineGridFactory
from intranet.wiki.tests.wiki_tests.common.factories.organization import OrganizationFactory
from intranet.wiki.tests.wiki_tests.common.factories.page import PageFactory
from intranet.wiki.tests.wiki_tests.common.factories.staff import StaffFactory
from intranet.wiki.tests.wiki_tests.common.factories.upload_session import UploadSessionFactory
from intranet.wiki.tests.wiki_tests.common.factories.user import UserFactory
from intranet.wiki.tests.wiki_tests.common.rest_api_client import RestApiClient
from intranet.wiki.tests.wiki_tests.common.utils import only_model_fields

if settings.IS_INTRANET:
    from wiki.intranet.models import Department, GroupMembership
    from intranet.wiki.tests.wiki_tests.common.factories.department import DepartmentFactory

if settings.IS_BUSINESS:
    from wiki.users.models import GROUP_TYPES


def patch_django_pytest():
    # Отключаем вывод "примененных миграций" при запуске джанговских тестов
    from django.test import utils

    old = utils.setup_databases

    def new(*args, **kwargs):
        kwargs['verbosity'] = 0
        return old(*args, **kwargs)

    utils.setup_databases = new


@pytest.fixture(scope='session', autouse=True)
def drop_constraint_of_uniq_user_login(django_db_setup, django_db_blocker):
    if settings.IS_BUSINESS:
        with django_db_blocker.unblock(), connection.cursor() as cursor:
            cursor.execute(
                """
                ALTER TABLE IF EXISTS auth_user DROP CONSTRAINT IF EXISTS auth_user_username_key;
                ALTER TABLE IF EXISTS intranet_staff DROP CONSTRAINT IF EXISTS intranet_staff_login_ld_key
                """
            )


@pytest.fixture
def client():
    return RestApiClient()


@pytest.fixture
def api_url():
    return '/_api/frontend'


@pytest.fixture
def add_user_to_group():
    def _add_user_to_group(group: Group, user: User):
        if settings.IS_INTRANET:
            GroupMembership.objects.create(group=group, staff=user.staff)
        if settings.IS_BUSINESS:
            group.user_set.add(user)

    return _add_user_to_group


@pytest.fixture
def groups(organizations):
    if settings.IS_INTRANET:
        return _intranet_groups()
    elif settings.IS_BUSINESS:
        return _business_groups(organizations)


@pytest.fixture
def test_group(organizations):
    if settings.IS_INTRANET:
        return _intranet_groups().child_group
    elif settings.IS_BUSINESS:
        return _business_groups(organizations).group_org_42


@pytest.fixture
def intranet_groups():
    return _intranet_groups()


def _intranet_groups():
    @dataclass
    class Groups:
        root_group: Group
        child_group: Group
        side_group: Group

    root_group = GroupFactory(name='main_group', url='main_group')
    child_group = GroupFactory(name='child_group', url='child_group')
    side_group = GroupFactory(name='side_group', url='side_group')

    # иначе MPTT не отработает
    child_group.parent = root_group
    child_group.save()

    return Groups(root_group=root_group, child_group=child_group, side_group=side_group)


@pytest.fixture
def business_groups(organizations):
    return _business_groups(organizations)


def _business_groups(organizations):
    @dataclass
    class Groups:
        group_org_21: Group
        department_org_21: Group
        group_org_42: Group
        department_org_42: Group

    group_org_21 = GroupFactory(
        name='accounting_group_21',
        title='Accounting',
        group_dir_type='generic',
        org_id=organizations.org_21.id,
        group_type=GROUP_TYPES.group,
        dir_id='5622',
    )

    department_org_21 = GroupFactory(
        name='devops_group_21',
        dir_id='5623',
        title='DevOps',
        org_id=organizations.org_21.id,
        group_type=GROUP_TYPES.department,
    )

    group_org_42 = GroupFactory(
        name='accounting_group_42',
        title='Accounting',
        dir_id='5624',
        group_dir_type='generic',
        org_id=organizations.org_42.id,
        group_type=GROUP_TYPES.group,
    )

    department_org_42 = GroupFactory(
        name='devops_group_42',
        dir_id='5625',
        title='DevOps',
        org_id=organizations.org_42.id,
        group_type=GROUP_TYPES.department,
    )

    return Groups(
        group_org_21=group_org_21,
        group_org_42=group_org_42,
        department_org_21=department_org_21,
        department_org_42=department_org_42,
    )


@pytest.fixture
def departments():
    if not settings.IS_INTRANET:
        return

    department_data = open_json_fixture('department.json', dateaware=['modified_at'])
    for department in department_data:
        fields = department['fields']

        if fields['parent']:
            try:
                fields['parent'] = Department.objects.get(id=fields['parent'])
            except Department.DoesNotExist:
                print(('Department does not exist: ', fields))
        DepartmentFactory(id=department['pk'], **fields)
    Department.tree.rebuild()


@pytest.fixture
def organizations():
    if not settings.IS_BUSINESS:
        return

    @dataclass
    class Organizations:
        org_21: Organization
        org_42: Organization

    OperatingMode.objects.get_or_create(name=OPERATING_MODE_NAMES.free)

    dir_id = '21'
    org_21 = OrganizationFactory(dir_id=dir_id, label=dir_id, status=Organization.ORG_STATUSES.enabled)

    dir_id = '42'
    org_42 = OrganizationFactory(dir_id=dir_id, label=dir_id, status=Organization.ORG_STATUSES.enabled)

    return Organizations(org_21=org_21, org_42=org_42)


@pytest.fixture
def intranet_outstaff_manager():
    mdl, _ = DjangoGroup.objects.get_or_create(name=settings.IDM_ROLE_OUTSTAFF_MANAGER_GROUP_NAME)
    return mdl


@pytest.fixture
def wiki_users(departments, organizations):
    @dataclass
    class Users:
        thasonic: User
        kolomeetz: User
        chapson: User
        asm: User
        volozh: User
        robot_wiki: User

    staff_fixture = open_json_fixture('staff.json', dateaware=['modified_at'])
    users = {}

    for staff_data in staff_fixture:
        kwargs = staff_data['fields']
        login = kwargs['login']

        user = UserFactory(
            username=login,
            email=f'{login}@yandex-team.ru',
        )
        kwargs['user'] = user

        if kwargs['department'] and settings.IS_INTRANET:
            kwargs['department'] = Department.objects.get(id=kwargs['department'])

        StaffFactory(**only_model_fields(kwargs, Staff))

        if settings.IDM_ENABLED:
            group_name = settings.IDM_ROLE_EMPLOYEE_GROUP_NAME
            employee_group = DjangoGroup.objects.get_or_create(name=group_name)
            user.groups.add(employee_group[0])

        if organizations and hasattr(user, 'orgs'):
            user.orgs.add(organizations.org_42)

        users[login] = user

    robot_login = 'robot-wiki'
    if organizations:
        robot_wiki = UserFactory(
            username=robot_login,
            is_dir_robot=True,
            service_slug='wiki',
            dir_id=settings.ROBOT_DIR_ID,
        )
        StaffFactory(login=robot_login, user=robot_wiki, is_robot=True)
        # Робот один на все организации
        robot_wiki.orgs.add(organizations.org_21)
        robot_wiki.orgs.add(organizations.org_42)
    else:
        robot_wiki = UserFactory(
            username=robot_login,
        )
        StaffFactory(login=robot_login, user=robot_wiki, is_robot=True, uid=settings.ROBOT_VIKA_UID)

    users['robot_wiki'] = robot_wiki

    return Users(**users)


@pytest.fixture
def test_page(wiki_users, organizations, request):
    params = getattr(request, 'param', {})
    page_body = params.get('page_body', 'test_page')
    org_id = None
    if organizations:
        org_id = organizations.org_42.id

    page = PageFactory(
        tag='TestPage',
        supertag='testpage',
        owner=wiki_users.thasonic,
        last_author=wiki_users.thasonic,
        org_id=org_id,
    )
    page.authors.add(wiki_users.thasonic)
    page.body = page_body

    page.save()
    return page


@pytest.fixture
def test_wysiwyg(wiki_users, organizations, request):
    params = getattr(request, 'param', {})
    page_body = params.get('page_body', 'wysiwyg')

    org_id = None
    if organizations:
        org_id = organizations.org_42.id

    page = PageFactory(
        tag='TestWysiwyg',
        supertag='wysiwyg',
        owner=wiki_users.thasonic,
        last_author=wiki_users.thasonic,
        org_id=org_id,
        page_type=Page.TYPES.WYSIWYG,
    )
    page.authors.add(wiki_users.thasonic)
    page.body = page_body

    page.save()
    return page


@pytest.fixture
def test_files(test_page, wiki_users):
    files = {
        fname: FileFactory(
            name=fname,
            page=test_page,
            user=wiki_users.thasonic,
        )
        for fname in ('file1', 'file2')
    }
    test_page.files += 2
    test_page.save()
    return files


@pytest.fixture
def test_inline_grid(test_page):
    structure = {
        'fields': [
            {'slug': 'col1', 'title': 'text column', 'type': 'string', 'required': True},
            {'slug': 'col2', 'title': 'optional text column', 'type': 'string', 'required': False},
            {'slug': 'col3', 'title': 'number column', 'type': 'number', 'required': True},
            {'slug': 'col4', 'title': 'date column', 'type': 'date', 'required': True},
        ]
    }

    grid = InlineGridFactory(
        id='cc9d1f3a-a1d1-4c1e-8604-4de06daa76cc',
        title='Test Grid',
        page_id=test_page.id,
        structure=structure,
    )

    rows = [
        ['1.1', '1.2', 42, '1655842678'],
        ['2.1', '2.2', 77, '2007-10-07'],
    ]

    grids_logic.add_rows(grid, rows)
    return grid


@pytest.fixture
def test_grid(wiki_users, organizations):
    GRID_STRUCTURE = """
{
  "title" : "Test Grid",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "src",
      "title" : "Source",
      "type": "string",
      "required": true
    },
    {
      "name" : "dst",
      "title" : "Destination",
      "type": "string"
    },
    {
      "name" : "staff",
      "title" : "Staff",
      "type": "staff"
    }
  ]
}
"""
    org_id = None
    if organizations:
        org_id = organizations.org_42.id

    grid = GridFactory(
        tag='TestGrid',
        supertag='testgrid',
        owner=wiki_users.thasonic,
        last_author=wiki_users.thasonic,
        org_id=org_id,
    )
    grid.change_structure(GRID_STRUCTURE)
    grid.save()
    grid.authors.add(wiki_users.thasonic)
    return grid


@pytest.fixture
def test_org_id(wiki_users, test_org):
    if test_org is not None:
        return test_org.id
    else:
        return None


@pytest.fixture
def test_org(wiki_users, organizations):
    if organizations:
        return organizations.org_42
    else:
        return None


@pytest.fixture
def test_baseorg(organizations):
    org = None
    if organizations:
        org = organizations.org_42

    return as_base_organization(org)


@pytest.fixture
def test_org_ctx(wiki_users, test_org):
    # прогнать тест в контексте организации
    with org_ctx(test_org):
        yield test_org


@pytest.fixture
def page_cluster(wiki_users, organizations):
    org_id = None
    if organizations:
        org_id = organizations.org_42.id

    pages = {
        st: PageFactory(
            tag=st,
            supertag=st,
            last_author=wiki_users.thasonic,
            owner=wiki_users.thasonic,
            org_id=org_id,
        )
        for st in [
            'root',
            'root/a',
            'root/a/aa',
            'root/a/ad',
            'root/a/ad/bc',
            'root/a/ac/bd',
            'root/b',
            'root/b/bd',
            'root/b/bd/bc',
            'root/c',
            'users/someprotectedpage',
        ]
    }

    for _, page in pages.items():
        page.authors.add(wiki_users.thasonic)

    return pages


@pytest.fixture
def big_page_cluster(wiki_users, organizations):
    org_id = None
    if organizations:
        org_id = organizations.org_42.id

    page_urls = []
    for root in ['root', 'broot', 'kroot']:
        page_urls += [root]
        page_urls += [f'{root}/page{q}' for q in range(25)]
        page_urls += [f'{root}/page15/subpage{q}' for q in range(2)]
        page_urls += [f'{root}/page13/subpage{q}' for q in range(2)]
        page_urls += [f'{root}/page22/subpage{q}' for q in range(2)]
        page_urls += [f'{root}/page8/subpage{q}' for q in range(2)]

    page_urls += [
        'root/page16/gap/subpage',
        'root/page15/subpage1/gap/child',
        'root/page15/subpage1/gap/child/a',
        'root/page15/subpage1/gap/child/b',
    ]
    page_urls += [f'root{q}' for q in range(100)]

    pages = {
        st: PageFactory(
            tag=st,
            supertag=st,
            last_author=wiki_users.thasonic,
            owner=wiki_users.thasonic,
            org_id=org_id,
        )
        for st in page_urls
    }

    for _, page in pages.items():
        page.authors.add(wiki_users.thasonic)

    return pages


@pytest.fixture()
def grid_with_content(client, api_url, wiki_users):
    # TBD -- переписать эту жесть на явный вызов создания типизированых гридов
    # ... когда код настолько запутан, что для того чтобы подготовить грид в тестах дергается ручка апи :)

    GRID_STRUCTURE = {
        'title': 'Grid Regress',
        'fields': [
            {'name': '100', 'title': 'Text', 'required': False, 'type': 'string'},
            {'name': '101', 'title': 'Ticket', 'required': False, 'type': 'ticket'},
            {'name': '102', 'title': 'Ticket-Subject', 'required': False, 'type': 'ticket-subject'},
            {
                'name': '103',
                'title': 'Number',
                'required': False,
                'width': '5%',
                'type': 'number',
                'format': '%.2f',
            },
            {'name': '104', 'title': 'Checkbox', 'required': False, 'markdone': True, 'type': 'checkbox'},
            {'name': '105', 'title': 'Date', 'required': False, 'type': 'date', 'format': 'd.m.Y'},
            {
                'name': '106',
                'title': 'Select',
                'required': False,
                'options': ['1', '2', '3'],
                'type': 'select',
                'multiple': False,
            },
            {
                'name': '107',
                'title': 'Multiple Select',
                'required': False,
                'multiple': True,
                'options': ['a', 'b', 'c', 'd'],
                'type': 'select',
            },
            {
                'name': '108',
                'title': 'Staff',
                'required': False,
                'format': 'i_first_name i_last_name',
                'type': 'staff',
                'multiple': False,
            },
        ],
    }

    client.login(wiki_users.thasonic)

    slug = 'grid'
    grid = grid_helper.create_grid(client, api_url, slug, json.dumps(GRID_STRUCTURE), wiki_users.thasonic)
    grid.refresh_from_db()

    rows = [
        {
            '103': 1,
            '108': 'chapson',
            '107': ['a', 'b'],
            '105': '2021-12-02',
            '104': True,
            '100': 'OwO',
            '101': 'WIKI-1000',
        },
        {
            '103': 2,
            '108': 'thasonic',
            '106': '1',
        },
    ]

    for row in rows:
        grid_helper.add_row(client, api_url, slug, row, 'last')

    return grid


@pytest.fixture
def cloud_page_cluster(wiki_users, organizations):
    org_id = None
    if organizations:
        org_id = organizations.org_42.id

    cloud_pages = ['root/a', 'root/b', 'root/a/aa', 'root/a/ab']
    other_pages = ['root']

    pages = {
        st: PageFactory(
            tag=st,
            supertag=st,
            owner=wiki_users.thasonic,
            last_author=wiki_users.thasonic,
            page_type=Page.TYPES.CLOUD if st in cloud_pages else Page.TYPES.PAGE,
            org_id=org_id,
        )
        for st in other_pages + cloud_pages
    }

    for _, page in pages.items():
        if page.page_type == Page.TYPES.CLOUD:
            CloudPageFactory.make_cloud_page(page)

        page.authors.add(wiki_users.thasonic)

    return pages


@pytest.fixture
def page_acl(page_cluster):
    acl_dict = {}
    for page in page_cluster.values():
        parent_page_supertag = page.supertag.rsplit('/', 1)[0]
        if parent_page_supertag == page.supertag:
            parent_page_supertag = ''
        acl = Acl(
            page=page,
            break_inheritance=False,
            parent_acl_type=ParentAclType.PAGE_ACL if parent_page_supertag else ParentAclType.ROOT_ACL,
            acl=AclDiff().json(),
            parent_acl=acl_dict.get(parent_page_supertag),
        )
        acl.save()
        acl_dict[page.supertag] = acl

    return acl_dict


@pytest.fixture
def org_dir_id(organizations):
    """dir_id организации по умолчанию (42) или None, для интранета"""
    return None if settings.IS_INTRANET else organizations.org_42.dir_id


@pytest.fixture
def support_client(client, wiki_users, add_user_to_group):
    user = wiki_users.asm

    group_name = settings.IDM_ROLE_SUPPORT_GROUP_NAME
    employee_group = DjangoGroup.objects.get_or_create(name=group_name)
    user.groups.add(employee_group[0])

    client.login(user)
    return client


@pytest.fixture
def legacy_subscr_favor(wiki_users):
    """После миграции подписок и закладок на новую модель WIKI-16858"""
    for login_field in fields(wiki_users):
        user = getattr(wiki_users, login_field.name)
        set_user_setting(user, 'new_favorites', False)
        set_user_setting(user, 'new_subscriptions', False)


@pytest.fixture
def upload_sessions(wiki_users, organizations):
    mb = 1024 * 1024

    org_id = None
    if organizations:
        org_id = organizations.org_42.id

    upload_sessions = {
        uuid4_id: UploadSessionFactory(
            session_id=UUID(uuid4_id),
            target=target,
            file_name=name,
            file_size=size,
            user=wiki_users.thasonic,
            org_id=org_id,
        )
        for uuid4_id, target, name, size in [
            ('9b18deaa-b969-4caa-a4f0-b13e455b610b', UploadSessionTargetType.ATTACHMENT, 'sugoma.txt', 2 * mb),
            ('bb143df4-9309-4ae1-97d1-93f8d86d9805', UploadSessionTargetType.ATTACHMENT, 'funnycat.png', 1 * mb),
            ('3ee4bb73-57f4-4048-ac66-ce1777341cb0', UploadSessionTargetType.IMPORT_PAGE, 'somepage', 3 * mb),
            ('15ba83b6-c4a4-4d82-ae40-81751e0fe166', UploadSessionTargetType.IMPORT_PAGE, 'amotherpage', 10 * mb),
            ('a7901b58-d316-4b86-ac85-002a3bf68d88', UploadSessionTargetType.IMPORT_GRID, 'grid123', 3 * mb),
            ('dbc04216-122f-4aa2-93d7-3ddd339297ae', UploadSessionTargetType.IMPORT_GRID, 'crew_mates', 4 * mb),
            ('a2986e79-c999-4d9f-ab23-c60bb677e5ab', UploadSessionTargetType.IMPORT_PAGE, 'page_data.txt', 4 * mb),
            ('37a000ae-0478-4f2c-b93f-60a6e596ac0e', UploadSessionTargetType.IMPORT_PAGE, 'page_data.pdf', 4 * mb),
            ('279af70e-9952-4989-9fde-180d54c5fdfc', UploadSessionTargetType.IMPORT_PAGE, 'page_data.docx', 4 * mb),
        ]
    }

    return upload_sessions


patch_django_pytest()
