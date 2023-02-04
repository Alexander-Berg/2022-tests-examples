
from django.conf import settings

from wiki.intranet.models import Staff
from wiki.pages.access import ACCESS_COMMON, ACCESS_DENIED, ACCESS_RESTRICTED
from wiki.pages.logic.subpages import get_subpages, get_subpages_tags
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.tree_builder_mixin import PagesTreeBuilderTestMixin


class SubpagesTest(BaseTestCase, PagesTreeBuilderTestMixin):
    def setUp(self):
        super(SubpagesTest, self).setUp()
        self.setUsers()

        self.current_user = Staff.objects.get(user=self.user_thasonic)
        self.other_user = Staff.objects.get(user=self.user_chapson)

        self.build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/1/1',
                                        },
                                        'subpages': [
                                            {
                                                'page': {'cluster': '1', 'type': 'P', 'title': 'Tree/1/1/1/1'},
                                                'subpages': [],
                                            }
                                        ],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'N',
                                },
                                'subpages': [
                                    {'page': {'cluster': '1', 'type': 'L', 'title': 'Tree/1/2/1'}, 'subpages': []}
                                ],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'L',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                ],
            },
            self.current_user,
            self.other_user,
        )

    def test_depth_1(self):
        subpages, page_pk_to_access, limit_exceeded = get_subpages('t', self.current_user.user, max_depth=1)

        self.assertEqual(2, len(subpages))

        self.assertEqual('t', subpages[0].supertag)
        self.assertTrue(subpages[0].pk in page_pk_to_access)
        self.assertEqual(ACCESS_COMMON, page_pk_to_access[subpages[0].pk])

        self.assertEqual('t/2', subpages[1].supertag)
        self.assertTrue(subpages[1].pk in page_pk_to_access)
        self.assertEqual(ACCESS_RESTRICTED, page_pk_to_access[subpages[1].pk])

        self.assertFalse(limit_exceeded)

    def test_depth_2(self):
        subpages, page_pk_to_access, limit_exceeded = get_subpages('t', self.current_user.user, max_depth=2)

        self.assertEqual(3, len(subpages))

        self.assertEqual('t', subpages[0].supertag)
        self.assertTrue(subpages[0].pk in page_pk_to_access)
        self.assertEqual(ACCESS_COMMON, page_pk_to_access[subpages[0].pk])

        self.assertEqual('t/1/1', subpages[1].supertag)
        self.assertTrue(subpages[1].pk in page_pk_to_access)
        self.assertEqual(ACCESS_DENIED, page_pk_to_access[subpages[1].pk])

        self.assertEqual('t/2', subpages[2].supertag)
        self.assertTrue(subpages[2].pk in page_pk_to_access)
        self.assertEqual(ACCESS_RESTRICTED, page_pk_to_access[subpages[2].pk])

        self.assertFalse(limit_exceeded)

    def test_depth_3(self):
        subpages, page_pk_to_access, limit_exceeded = get_subpages('t', self.current_user.user, max_depth=3)

        self.assertEqual(5, len(subpages))

        self.assertEqual('t', subpages[0].supertag)
        self.assertTrue(subpages[0].pk in page_pk_to_access)
        self.assertEqual(ACCESS_COMMON, page_pk_to_access[subpages[0].pk])

        self.assertEqual('t/1/1', subpages[1].supertag)
        self.assertTrue(subpages[1].pk in page_pk_to_access)
        self.assertEqual(ACCESS_DENIED, page_pk_to_access[subpages[1].pk])

        self.assertEqual('t/1/1/1', subpages[2].supertag)
        self.assertTrue(subpages[2].pk in page_pk_to_access)
        self.assertEqual(ACCESS_COMMON, page_pk_to_access[subpages[2].pk])

        self.assertEqual('t/1/2/1', subpages[3].supertag)
        self.assertTrue(subpages[3].pk in page_pk_to_access)
        self.assertEqual(ACCESS_RESTRICTED, page_pk_to_access[subpages[3].pk])

        self.assertEqual('t/2', subpages[4].supertag)
        self.assertTrue(subpages[4].pk in page_pk_to_access)
        self.assertEqual(ACCESS_RESTRICTED, page_pk_to_access[subpages[4].pk])

        self.assertFalse(limit_exceeded)

    def test_subtree(self):
        subpages, page_pk_to_access, limit_exceeded = get_subpages('t/1', self.current_user.user, max_depth=3)

        self.assertEqual(4, len(subpages))

        self.assertEqual('t/1/1', subpages[0].supertag)
        self.assertTrue(subpages[0].pk in page_pk_to_access)
        self.assertEqual(ACCESS_DENIED, page_pk_to_access[subpages[0].pk])

        self.assertEqual('t/1/1/1', subpages[1].supertag)
        self.assertTrue(subpages[1].pk in page_pk_to_access)
        self.assertEqual(ACCESS_COMMON, page_pk_to_access[subpages[1].pk])

        self.assertEqual('t/1/1/1/1', subpages[2].supertag)
        self.assertTrue(subpages[2].pk in page_pk_to_access)
        self.assertEqual(ACCESS_COMMON, page_pk_to_access[subpages[2].pk])

        self.assertEqual('t/1/2/1', subpages[3].supertag)
        self.assertTrue(subpages[3].pk in page_pk_to_access)
        self.assertEqual(ACCESS_RESTRICTED, page_pk_to_access[subpages[3].pk])

        self.assertFalse(limit_exceeded)

    def test_unlimited_depth(self):
        subpages, _, _ = get_subpages('t', self.current_user.user, max_depth=None)
        self.assertEqual(6, len(subpages))

    def test_from_yandex_server(self):
        _, page_pk_to_access, _ = get_subpages('t', self.current_user.user, max_depth=None, from_yandex_server=True)
        self.assertEqual({}, page_pk_to_access)

    def test_limit_exceeded(self):
        _, _, limit_exceeded = get_subpages('t', self.current_user.user, max_depth=3, limit=5)
        self.assertFalse(limit_exceeded)

        _, _, limit_exceeded = get_subpages('t', self.current_user.user, max_depth=3, limit=4)
        self.assertTrue(limit_exceeded)

    def test_nonexistent_tree(self):
        subpages, page_pk_to_access, limit_exceeded = get_subpages('g', self.current_user.user, max_depth=None)

        self.assertEqual([], subpages)
        self.assertEqual({}, page_pk_to_access)
        self.assertFalse(limit_exceeded)

    def test_show_owners(self):
        def get_subpages_authors(show_owners):
            subpages, _, _ = get_subpages('t', self.current_user.user, max_depth=3, show_owners=show_owners)
            for page in subpages:
                for author in page.get_authors():
                    username = author.username  # noqa

        assert_queries = 10 if not settings.WIKI_CODE == 'wiki' else 10
        with self.assertNumQueries(assert_queries):
            get_subpages_authors(show_owners=False)

        assert_queries = 6 if not settings.WIKI_CODE == 'wiki' else 6
        with self.assertNumQueries(assert_queries):
            get_subpages_authors(show_owners=True)


class SubpagesTagsTest(BaseTestCase):
    def setUp(self):
        super(SubpagesTagsTest, self).setUp()
        self.setUsers()
        self.create_page(tag='Рики', authors_to_add=[self.user_thasonic])
        self.create_page(tag='Рики/Рут', authors_to_add=[self.user_thasonic])
        self.create_page(tag='Рики/Рут/Мидл1', authors_to_add=[self.user_chapson])
        self.create_page(tag='Рики/Рут/Мидл2', authors_to_add=[self.user_thasonic])
        self.create_page(tag='Рики/Рут/Мидл1/Чайлд1', authors_to_add=[self.user_chapson])
        self.create_page(tag='Рики/Рут/Мидл1/Чайлд2', authors_to_add=[self.user_thasonic])

    def test(self):
        self.assertEqual(
            (
                [
                    'Рики/Рут',
                    'Рики/Рут/Мидл1',
                    'Рики/Рут/Мидл1/Чайлд1',
                    'Рики/Рут/Мидл1/Чайлд2',
                    'Рики/Рут/Мидл2',
                ],
                False,
            ),
            get_subpages_tags('riki'),
        )
        self.assertEqual(
            (
                [
                    'Рики/Рут/Мидл1/Чайлд1',
                    'Рики/Рут/Мидл1/Чайлд2',
                ],
                False,
            ),
            get_subpages_tags('riki/rut/midl1'),
        )

    def test_owner(self):
        self.assertEqual(
            (
                [
                    'Рики/Рут',
                    'Рики/Рут/Мидл1/Чайлд2',
                    'Рики/Рут/Мидл2',
                ],
                False,
            ),
            get_subpages_tags('riki', authors=[self.user_thasonic]),
        )
        self.assertEqual(
            (
                [
                    'Рики/Рут/Мидл1',
                    'Рики/Рут/Мидл1/Чайлд1',
                ],
                False,
            ),
            get_subpages_tags('riki', authors=[self.user_chapson]),
        )

    def test_limit_exceeded(self):
        self.assertEqual(
            (
                [
                    'Рики/Рут',
                    'Рики/Рут/Мидл1',
                ],
                True,
            ),
            get_subpages_tags('riki', limit=2),
        )
