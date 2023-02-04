
import urllib.parse
from datetime import datetime, timedelta

from mock import patch
from ujson import loads

from wiki.api_v1.dao.page_catalog import get_pages, insert_pages, remove_all_pages, remove_store_last_update_started_at
from wiki.api_v1.logic.page_catalog import PageCatalogQueryParams, load_page_catalog, update_page_catalog
from wiki.api_v1.tasks.page_catalog import run_update_page_catalog
from wiki.api_v1.views import PageCatalogView
from wiki.intranet.models import Staff
from wiki.pages.models import Access
from wiki.users.models import Group
from wiki.utils.lock import LockTaken
from wiki.utils.timezone import make_aware_current, make_aware_utc
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


def _mock_check_lock_acquired(lock_name):
    return False


def _mock_execute_with_lock_raise_taken(lock_name, callable, timeout=None):
    raise LockTaken()


def _mock_check_lock_acquired_raise_taken(lock_name):
    raise LockTaken()


patch_lock = patch('wiki.api_v1.logic.page_catalog.check_lock_acquired', _mock_check_lock_acquired)

patch_lock_taken_for_logic = patch(
    'wiki.api_v1.logic.page_catalog.check_lock_acquired', _mock_check_lock_acquired_raise_taken
)

patch_lock_taken_for_tasks = patch('wiki.utils.lock.execute_with_lock', _mock_execute_with_lock_raise_taken)


class PageCatalogUpdateTest(BaseTestCase):
    # Проверяем, что процедура обновления каталога страниц, запускаемая задачей celery,
    # вставляет в MongoDB ожидаемые данные для указанных страниц.

    def setUp(self):
        super(PageCatalogUpdateTest, self).setUp()
        remove_all_pages()
        remove_store_last_update_started_at()

        self.setGroups()
        self.setUsers()
        self.setExternalMember()

    def test_common_fields(self):
        # Проверяем, что правильно вставляются общие параметры -
        # uri, title, opened_to_external, deleted, modified_at, pk

        def create_page():
            page = self.create_page(
                supertag='just_page',
                title='Just page',
                opened_to_external_flag=True,
                modified_at=datetime.fromtimestamp(42),
                modified_at_for_index=datetime.fromtimestamp(42),
                status=0,
            )
            return page

        page_pk = create_page().pk
        update_page_catalog()

        pages = get_pages(all_fields=True)

        self.assertEqual(1, len(pages))

        page = pages[0]

        self.assertEqual('just_page', page['uri'])
        self.assertEqual('Just page', page['title'])
        self.assertEqual(True, page['deleted'])
        self.assertEqual(True, page['opened_to_external'])
        self.assertEqual(42, (page['modified_at'] - make_aware_utc(datetime(1970, 1, 1))).total_seconds())
        self.assertEqual(page_pk, page['pk'])

    def test_access_fields(self):
        # Проверяем, что правильно вставляются параметры доступа -
        # users, groups, users_ext, groups_ext

        def create_page_with_common_access():
            page = self.create_page(
                supertag='internal/page_c', authors_to_add=[self.user_thasonic], opened_to_external_flag=False
            )
            return page

        def create_page_with_owner_access():
            page = self.create_page(
                supertag='internal/page_o', authors_to_add=[self.user_thasonic], opened_to_external_flag=False
            )
            Access.objects.create(page=page, is_owner=True)
            return page

        def create_page_with_some_users_and_grous_access():
            page = self.create_page(
                supertag='internal/page_u', authors_to_add=[self.user_thasonic], opened_to_external_flag=True
            )
            staff_thasonic = Staff.objects.get_by_user(self.user_thasonic)
            staff_chapson = Staff.objects.get_by_user(self.user_chapson)
            staff_snegovoy = Staff.objects.get_by_user(self.user_snegovoy)
            group_yandex_mnt = Group.objects.get(url='yandex_mnt')
            group_yandex_mnt_srv = Group.objects.get(url='yandex_mnt_srv')

            Access.objects.create(page=page, staff=staff_thasonic)
            Access.objects.create(page=page, staff=staff_chapson)
            Access.objects.create(page=page, staff=staff_snegovoy)
            Access.objects.create(page=page, group=group_yandex_mnt)
            Access.objects.create(page=page, group=group_yandex_mnt_srv)

            return page

        create_page_with_common_access()
        create_page_with_owner_access()
        create_page_with_some_users_and_grous_access()

        update_page_catalog()

        pages = get_pages()

        self.assertEqual(3, len(pages))

        # common access

        page = pages[0]

        self.assertIsNone(page['users'])
        self.assertIsNone(page['groups'])
        self.assertIsNone(page['users_ext'])
        self.assertIsNone(page['groups_ext'])

        # owner access

        page = pages[1]

        staff_thasonic = Staff.objects.get_by_user(self.user_thasonic)

        self.assertEqual([staff_thasonic.uid], page['users'])
        self.assertIsNone(page['groups'])
        self.assertIsNone(page['users_ext'])
        self.assertIsNone(page['groups_ext'])

        # some users and groups access

        page = pages[2]

        staff_thasonic = Staff.objects.get_by_user(self.user_thasonic)
        staff_chapson = Staff.objects.get_by_user(self.user_chapson)
        staff_snegovoy = Staff.objects.get_by_user(self.user_snegovoy)
        group_yandex_mnt = Group.objects.get(url='yandex_mnt')
        group_yandex_mnt_srv = Group.objects.get(url='yandex_mnt_srv')

        self.assertEqual(set([staff_thasonic.uid, staff_chapson.uid, staff_snegovoy.uid]), set(page['users']))
        self.assertEqual(set([group_yandex_mnt.url, group_yandex_mnt_srv.url]), set(page['groups']))
        self.assertEqual([staff_snegovoy.uid], page['users_ext'])
        self.assertIsNone(page['groups_ext'])

    def test_multiple_updates(self):
        # Первое обновление просто записывает данные страниц.
        # Последующие обновления перезаписывают данные о страницах,
        # которые изменились с даты начала предыдущего обновления,
        # и записывают данные о новых страницах.

        page1 = self.create_page(supertag='page1', title='old title')
        self.create_page(supertag='page2')

        update_page_catalog()
        pages = get_pages(all_fields=True)

        self.assertEqual(2, len(pages))

        page2_oid = pages[1]['_id']

        # Чтобы избежать расхождения во времени в хранилищах,
        # явно сдвинем modified_at_for_index в будущее
        td = timedelta(hours=1)

        page1.title = 'new title'
        page1.modified_at_for_index = page1.modified_at_for_index + td
        page1.save()

        page3 = self.create_page(supertag='page3')
        page3.modified_at_for_index = page3.modified_at_for_index + td
        page3.save()

        update_page_catalog()
        pages = get_pages(all_fields=True)

        self.assertEqual(3, len(pages))

        self.assertEqual('page1', pages[0]['uri'])
        self.assertEqual('page2', pages[1]['uri'])
        self.assertEqual('page3', pages[2]['uri'])

        # Проверим, что данные page1 обновились
        self.assertEqual('new title', pages[0]['title'])

        # Проверим, что данные о page2 не перезаписывались,
        # убедившись в неизменности ObjectId
        self.assertEqual(page2_oid, pages[1]['_id'])

    @patch_lock_taken_for_tasks
    def test_failed_to_get_lock(self):
        # Проверяем случай, когда не удалось захватить
        # Zookeeper лок за заданный таумаут. В этом случае
        # каталог страниц не должен обновиться.

        self.create_page()

        run_update_page_catalog()
        pages = get_pages()

        self.assertEqual(0, len(pages))


class PageCatalogDataMixin(BaseTestCase):
    # Страницы для тестов view и загрузки каталога страниц.

    page1 = {
        'uri': 'page1',
        'title': 'Page 1',
        'users': None,
        'users_ext': None,
        'groups': None,
        'groups_ext': None,
        'deleted': False,
        'opened_to_external': False,
        'modified_at': make_aware_current(datetime.fromtimestamp(1)),
        'pk': 100,
    }

    page2 = {
        'uri': 'page2',
        'title': 'Page 2',
        'users': None,
        'users_ext': None,
        'groups': None,
        'groups_ext': None,
        'deleted': True,
        'opened_to_external': False,
        'modified_at': make_aware_current(datetime.fromtimestamp(4)),
        'pk': 101,
    }

    page3 = {
        'uri': 'page3',
        'title': 'Page 3',
        'users': None,
        'users_ext': None,
        'groups': None,
        'groups_ext': None,
        'deleted': True,
        'opened_to_external': False,
        'modified_at': make_aware_current(datetime.fromtimestamp(2)),
        'pk': 102,
    }

    page4 = {
        'uri': 'page4',
        'title': 'Page 4',
        'users': None,
        'users_ext': None,
        'groups': None,
        'groups_ext': None,
        'deleted': False,
        'opened_to_external': False,
        'modified_at': make_aware_current(datetime.fromtimestamp(3)),
        'pk': 103,
    }

    page_from_catalog_fields = (
        'uri',
        'title',
        'users',
        'users_ext',
        'groups',
        'groups_ext',
        'opened_to_external',
        'pk',
    )

    handler_url = 'https://wiki.ru/pages/.catalog'

    def assertPagesEqual(self, page1, page2):
        """
        Мы не можем сравнивать страницу, сохраняемую в MongoDB, и страницу,
        загружаемую из каталога, как словари, т.к. в загружаемой странице
        нет некоторых ненужных полей - _id и modified_at.

        @type page1: dict
        @type page2: dict
        """
        for field in self.page_from_catalog_fields:
            self.assertEqual(page1[field], page2[field])

    def assertNextParams(self, expected_params, next):
        """
        Проверяем параметры из next.

        @type next: basestring
        @type expected_params: dict
        """
        parsed_next = urllib.parse.urlparse(next)
        parsed_qs = urllib.parse.parse_qs(parsed_next.query)
        parsed_qs = {key: value_array[0] for key, value_array in parsed_qs.items()}
        self.assertEqual(expected_params, parsed_qs)


class PageCatalogLoadTest(PageCatalogDataMixin):
    # Проверяем загрузку каталога страниц из MongoDB.

    def setUp(self):
        super(PageCatalogLoadTest, self).setUp()
        remove_all_pages()
        remove_store_last_update_started_at()

        # Сохраняем специально не по порядку
        insert_pages([self.page3, self.page1, self.page4, self.page2])

    def _load_page_catalog(self, params):
        return load_page_catalog(params, self.handler_url)

    @patch_lock
    def test_simple_load(self):
        # Проверяем параметры по умолчанию, общие поля ответа и
        # упорядоченность по выборки по pk.

        page_catalog = self._load_page_catalog(PageCatalogQueryParams())

        self.assertTrue(isinstance(page_catalog['timestamp'], int))
        self.assertEqual('ok', page_catalog['status'])
        self.assertIsNone(page_catalog['next'])

        self.assertEqual(4, len(page_catalog['pages']))
        self.assertPagesEqual(self.page1, page_catalog['pages'][0])
        self.assertPagesEqual(self.page2, page_catalog['pages'][1])
        self.assertPagesEqual(self.page3, page_catalog['pages'][2])
        self.assertPagesEqual(self.page4, page_catalog['pages'][3])

    @patch_lock
    def test_page_from_catalog_fields(self):
        # Проверяем состав полей загружаемой страницы.
        # Должны быть только те поля, которые описаны в API.

        page_catalog = self._load_page_catalog(PageCatalogQueryParams())

        self.assertEqual(4, len(page_catalog['pages']))
        self.assertEqual(set(self.page_from_catalog_fields), set(page_catalog['pages'][0].keys()))

    @patch_lock
    def test_pagination_divisible_case(self):
        # Проверяем пагинацию в случае N % page_size == 0

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(limit=2))

        self.assertEqual(2, len(page_catalog['pages']))
        self.assertPagesEqual(self.page1, page_catalog['pages'][0])
        self.assertPagesEqual(self.page2, page_catalog['pages'][1])
        self.assertNextParams({'limit': '2', 'gt_pk': str(self.page2['pk'])}, page_catalog['next'])

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(limit=2, gt_pk=self.page2['pk']))

        self.assertEqual(2, len(page_catalog['pages']))
        self.assertPagesEqual(self.page3, page_catalog['pages'][0])
        self.assertPagesEqual(self.page4, page_catalog['pages'][1])
        self.assertNextParams({'limit': '2', 'gt_pk': str(self.page4['pk'])}, page_catalog['next'])

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(limit=2, gt_pk=self.page4['pk']))

        self.assertEqual(0, len(page_catalog['pages']))
        self.assertIsNone(None, page_catalog['next'])

    @patch_lock
    def test_pagination_non_divisible_case(self):
        # Проверяем пагинацию в случае N % page_size != 0

        page5 = {
            'uri': 'page5',
            'title': 'Page 5',
            'users': None,
            'users_ext': None,
            'groups': None,
            'groups_ext': None,
            'deleted': False,
            'opened_to_external': False,
            'modified_at': make_aware_current(datetime.fromtimestamp(5)),
            'pk': 104,
        }
        insert_pages([page5])

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(limit=2))

        self.assertEqual(2, len(page_catalog['pages']))
        self.assertPagesEqual(self.page1, page_catalog['pages'][0])
        self.assertPagesEqual(self.page2, page_catalog['pages'][1])
        self.assertNextParams({'limit': '2', 'gt_pk': str(self.page2['pk'])}, page_catalog['next'])

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(limit=2, gt_pk=self.page2['pk']))

        self.assertEqual(2, len(page_catalog['pages']))
        self.assertPagesEqual(self.page3, page_catalog['pages'][0])
        self.assertPagesEqual(self.page4, page_catalog['pages'][1])
        self.assertNextParams({'limit': '2', 'gt_pk': str(self.page4['pk'])}, page_catalog['next'])

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(limit=2, gt_pk=self.page4['pk']))

        self.assertEqual(1, len(page_catalog['pages']))
        self.assertPagesEqual(page5, page_catalog['pages'][0])
        self.assertIsNone(page_catalog['next'])

    @patch_lock
    def test_not_deleted(self):
        # Проверяем соответствие параметру not_deleted.

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(not_deleted=True))

        self.assertEqual(2, len(page_catalog['pages']))
        self.assertPagesEqual(self.page1, page_catalog['pages'][0])
        self.assertPagesEqual(self.page4, page_catalog['pages'][1])

    @patch_lock
    def test_since(self):
        # Проверяем соответствие параметру since.

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(since=3))

        self.assertEqual(2, len(page_catalog['pages']))
        self.assertPagesEqual(self.page2, page_catalog['pages'][0])
        self.assertPagesEqual(self.page4, page_catalog['pages'][1])

    @patch_lock
    def test_not_deleted_shifts_gt_pk(self):
        # Проверяем, что not_deleted сдвигает gt_pk.

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(limit=2, not_deleted=True))

        self.assertNextParams(
            {'limit': '2', 'not_deleted': 'true', 'gt_pk': str(self.page4['pk'])}, page_catalog['next']
        )

    @patch_lock
    def test_since_shifts_gt_pk(self):
        # Проверяем, что since сдвигает gt_pk.

        page_catalog = self._load_page_catalog(PageCatalogQueryParams(limit=2, since=2))

        self.assertNextParams({'limit': '2', 'since': '2', 'gt_pk': str(self.page3['pk'])}, page_catalog['next'])

    @patch_lock_taken_for_logic
    def test_failed_to_get_lock(self):
        # Проверяем случай, когда не удалось захватить
        # Zookeeper лок за заданный таумаут. В этом случае
        # статус должен быть 'retry', а параметр 'next'
        # должен содержать URL оригинального запроса.

        page_catalog = self._load_page_catalog(
            PageCatalogQueryParams(
                since=1,
                limit=2,
                not_deleted=True,
                gt_pk=self.page2['pk'],
            )
        )

        self.assertEqual('retry', page_catalog['status'])
        self.assertEqual([], page_catalog['pages'])
        self.assertNextParams(
            {'since': '1', 'limit': '2', 'not_deleted': 'true', 'gt_pk': str(self.page2['pk'])}, page_catalog['next']
        )


class PageCatalogViewTest(BaseApiTestCase, PageCatalogDataMixin):
    # Проверяем view каталога страниц.

    def setUp(self):
        super(PageCatalogViewTest, self).setUp()
        remove_all_pages()
        remove_store_last_update_started_at()

        self.setUsers()
        self.client.login('chapson')

        # Сохраняем специально не по порядку
        insert_pages([self.page3, self.page1, self.page4, self.page2])
        PageCatalogView.permission_classes = tuple()

    @patch_lock
    def test_ok(self):
        # Проверяем два последовательных запроса с пагинацией.

        response = self.client.get('/_api/v1/pages/.catalog?since=2&limit=2')

        self.assertEqual(200, response.status_code)
        page_catalog = loads(response.content)['data']

        self.assertEqual(2, len(page_catalog['pages']))
        self.assertNextParams({'since': '2', 'limit': '2', 'gt_pk': str(self.page3['pk'])}, page_catalog['next'])

        self.assertPagesEqual(self.page2, page_catalog['pages'][0])
        self.assertPagesEqual(self.page3, page_catalog['pages'][1])

        response = self.client.get('/_api/v1/pages/.catalog?since=2&limit=2&gt_pk=%s' % self.page3['pk'])

        self.assertEqual(200, response.status_code)
        page_catalog = loads(response.content)['data']

        self.assertEqual(1, len(page_catalog['pages']))
        self.assertIsNone(page_catalog['next'])

        self.assertPagesEqual(self.page4, page_catalog['pages'][0])


class PageCatalogParamsTest(BaseTestCase):
    # Проверяем заполнение параметров ручки каталога страниц.

    def test_ok_params(self):
        params = PageCatalogQueryParams.from_request_params(
            {'since': 1455711921, 'limit': 1000, 'not_deleted': 'true', 'gt_pk': 124032}
        )
        self.assertEqual(1455711921, params.since)
        self.assertEqual(1000, params.limit)
        self.assertTrue(params.not_deleted)
        self.assertEqual(124032, params.gt_pk)
