# flake8: noqa: E126

from django.conf import settings

from wiki.pages.pages_tree_new import ExpandSubtreeUrlBuilder, PagesTree, Sort, SortBy
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.tree_builder_mixin import PagesTreeBuilderTestMixin


class PagesTreeTest(BaseTestCase, PagesTreeBuilderTestMixin):
    def setUp(self):
        super(PagesTreeTest, self).setUp()
        self.setUsers()

        self.current_user = self.user_thasonic.staff
        self.other_user = self.user_chapson.staff

    def _compare_nodes(self, expected_node, actual_node, compare_urls=False, compare_subpages_count=False):
        # Сравнение деревьев, сравнивается часть полей по специальным правилам.

        expected_page = expected_node['page']
        actual_page = actual_node['page']

        self.assertEqual(expected_page['cluster'], actual_page['cluster'])
        self.assertEqual(expected_page['type'], actual_page['type'])
        self.assertEqual(expected_page.get('title'), actual_page.get('title'))
        self.assertEqual(expected_page.get('files'), actual_page.get('files'))
        self.assertEqual(expected_page.get('created_at'), actual_page.get('created_at'))
        self.assertEqual(expected_page.get('modified_at'), actual_page.get('modified_at'))

        # URL и число подстраниц проверяем не во всех тестах, чтобы не загромождать другие json'ы.
        if compare_urls:
            self.assertEqual(expected_page['url'], actual_page['url'])
        if compare_subpages_count:
            self.assertEqual(expected_node['subpages_count'], actual_node['subpages_count'])

        self.assertEqual('expand_url' in expected_node, 'expand_url' in actual_node)
        if 'expand_url' in expected_node:
            # Не проверяем GET параметры
            self.assertEqual(
                '//wiki.su/_api/frontend/' + expected_node['expand_url'] + '/.actions_view',
                actual_node['expand_url'].split('?')[0],
            )
            self.assertTrue(expected_node['expand_url'] in actual_node['expand_url'])

        expected_subpages = expected_node['subpages']
        actual_subpages = actual_node['subpages']

        self.assertEqual(len(expected_subpages), len(actual_subpages))

        for i in range(len(expected_subpages)):
            self._compare_nodes(
                expected_subpages[i],
                actual_subpages[i],
                compare_urls=compare_urls,
                compare_subpages_count=compare_subpages_count,
            )

    def _build_pages_tree(self, tree):
        # Shortcut для build_pages_tree(...)
        self.build_pages_tree(tree, self.current_user, self.other_user)

    def _get_pages_tree_data(self, supertag, **kwargs):
        # Shortcut для PagesTree(...).data

        # В конструктор URL необязательно передавать настоящие значения GET параметров
        # и абсолютного URL запроса. Впоследствии будет проверяться факт вхождения
        # супертэга корня дерева в URL.
        expand_subtree_url_builder = ExpandSubtreeUrlBuilder(
            get_params={
                'show_redirects': '',
                'show_grids': '',
                'show_files': '',
                'show_owners': '',
                'show_titles': '',
                'show_created_at': '',
                'show_modified_at': '',
                'sort_by': '',
                'sort': '',
                'authors': '',
            },
            nginx_host='wiki.su',
        )
        return PagesTree(supertag, self.current_user.user, expand_subtree_url_builder, **kwargs).data

    def test_empty_tree(self):
        # Страница без подстраниц.

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [],
            }
        )

        self._compare_nodes(
            {
                'page': {'cluster': 't', 'type': 'P', 'title': 'Tree'},
                'subpages': [],
            },
            self._get_pages_tree_data('t', depth=3),
        )

    def test_subpages_sorted_by_title(self):
        # Проверим сортировку подстраниц по заголовкам.
        # Несуществующие и закрытые страницы сортируется по кластеру.

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/4',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '5',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/2/3',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '3',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'N',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/2/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'C',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '5',
                            'type': 'N',
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
                                            'type': 'P',
                                            'title': 'Tree/2/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '3',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/2/3',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/4',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=None),
        )

        pass

    def test_subpages_sorted_by_cluster(self):
        # Проверим сортировку подстраниц по кластерам.
        # Все страницы (в том числе несуществующие и закрытые) сортируются по кластеру.

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/1/3',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '3',
                                    'type': 'P',
                                    'title': 'Tree/1/2',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                ],
            }
        )

        self._compare_nodes(
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
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/1/3',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '3',
                                    'type': 'P',
                                    'title': 'Tree/1/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=None, sort_by=SortBy.CLUSTER),
        )

    def test_subpages_sorted_by_created_at(self):
        # Проверим сортировку подстраниц по дате создания.
        # Страницы сортируются сначала по дате, несуществующие страницы
        # попадают в конец и сортируются по кластеру.

        self._build_pages_tree(
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
                            'type': 'P',
                            'title': 'Tree/1',
                            'created_at': 3,
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/4/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                            'created_at': 1,
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '5',
                            'type': 'P',
                            'title': 'Tree/5',
                            'created_at': 2,
                        },
                        'subpages': [],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '5',
                            'type': 'P',
                            'title': 'Tree/5',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/4/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=None, sort_by=SortBy.CREATED_AT),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/4/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '5',
                            'type': 'P',
                            'title': 'Tree/5',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=None, sort_by=SortBy.CREATED_AT, sort=Sort.DESC),
        )

    def test_no_titles(self):
        # Проверим, что при show_titles=False заголовки страниц
        # не попадают в итоговые данные.
        self._build_pages_tree(
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
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    }
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                        },
                        'subpages': [],
                    }
                ],
            },
            self._get_pages_tree_data('t', depth=None, show_titles=False),
        )

    def test_show_created_and_modified_at(self):
        # Проверим, что при show_created_at=True или show_modified_at=True
        # даты создания и модификации страниц попадают в итоговые данные.
        # Для закрытых страниц даты не добавляются.

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                    'created_at': 1,
                    'modified_at': 3,
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                            'created_at': 8,
                            'modified_at': 14,
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'C',
                            'title': 'Tree/2',
                            'created_at': 4,
                            'modified_at': 9,
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                    'created_at': 15,
                                    'modified_at': 15,
                                },
                                'subpages': [],
                            }
                        ],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                    'created_at': 1,
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                    'created_at': 15,
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                            'created_at': 8,
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=None, show_created_at=True),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                    'modified_at': 3,
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                    'modified_at': 15,
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                            'modified_at': 14,
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=None, show_modified_at=True),
        )

    def test_level_folding(self):
        # Проверим ограничение дерева по глубине.
        # Если какие-то подстраницы не влезли в дерево из-за ограничения по глубине,
        # то в их родительский узел дерева должен быть добавлен
        # атрибут expand_url, позволяющий развернуть поддерево.

        self._build_pages_tree(
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
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
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
                                                'page': {
                                                    'cluster': '1',
                                                    'type': 'P',
                                                    'title': 'Tree/1/1/1/1',
                                                },
                                                'subpages': [],
                                            }
                                        ],
                                    },
                                    {
                                        'page': {
                                            'cluster': '2',
                                            'type': 'P',
                                            'title': 'Tree/1/1/2',
                                        },
                                        'subpages': [
                                            {
                                                'page': {
                                                    'cluster': '1',
                                                    'type': 'P',
                                                    'title': 'Tree/1/1/2/1',
                                                },
                                                'subpages': [
                                                    {
                                                        'page': {
                                                            'cluster': '1',
                                                            'type': 'P',
                                                            'title': 'Tree/1/1/2/1/1',
                                                        },
                                                        'subpages': [],
                                                    }
                                                ],
                                            }
                                        ],
                                    },
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/1/2',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/2/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                ],
            }
        )

        self._compare_nodes(
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
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                        'expand_url': 't/1',
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                        'expand_url': 't/3',
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=1),
        )

        self._compare_nodes(
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
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                                'expand_url': 't/1/1',
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/1/2',
                                },
                                'subpages': [],
                                'expand_url': 't/1/2',
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=2),
        )

        self._compare_nodes(
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
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/1/1',
                                        },
                                        'subpages': [],
                                        'expand_url': 't/1/1/1',
                                    },
                                    {
                                        'page': {
                                            'cluster': '2',
                                            'type': 'P',
                                            'title': 'Tree/1/1/2',
                                        },
                                        'subpages': [],
                                        'expand_url': 't/1/1/2',
                                    },
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/1/2',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/2/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=3),
        )

    def test_closed_pages(self):
        # Проверим дерево с закрытыми страницами.
        # Заодно убеждаемся, что у закрытых страниц не отображаются названия title.

        self._build_pages_tree(
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
                            'type': 'C',
                            'title': 'Tree/1',
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
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {'cluster': '2', 'type': 'C', 'title': 'Tree/2'},
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'C',
                                    'title': 'Tree/2/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'C',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'C',
                                            'title': 'Tree/3/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'P',
                            'title': 'Tree/4',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                    'title': 'Tree/4/1',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/4/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'C',
                        },
                        'subpages': [],
                        'expand_url': 't/2',
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'P',
                            'title': 'Tree/4',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=1),
        )

        self._compare_nodes(
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
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [],
                                'expand_url': 't/1/1',
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'P',
                            'title': 'Tree/4',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [],
                                'expand_url': 't/4/1',
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=2),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': '3',
                    'type': 'C',
                },
                'subpages': [],
            },
            self._get_pages_tree_data('t/3', depth=None),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': '1',
                    'type': 'C',
                },
                'subpages': [],
            },
            self._get_pages_tree_data('t/3/1', depth=None),
        )

    def test_nonexistent_pages(self):
        # Проверим несуществующие страницы.

        self._build_pages_tree(
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
                                    'type': 'N',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/1/1',
                                        },
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
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
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
                                            'type': 'P',
                                            'title': 'Tree/3/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'N',
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
                                            'type': 'N',
                                        },
                                        'subpages': [
                                            {
                                                'page': {
                                                    'cluster': '1',
                                                    'type': 'P',
                                                    'title': 'Tree/4/1/1/1',
                                                },
                                                'subpages': [],
                                            }
                                        ],
                                    }
                                ],
                            }
                        ],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'N',
                        },
                        'subpages': [],
                        'expand_url': 't/2',
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=1),
        )

        self._compare_nodes(
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
                                    'type': 'N',
                                },
                                'subpages': [],
                                'expand_url': 't/1/1',
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'N',
                                },
                                'subpages': [],
                                'expand_url': 't/3/1',
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=2),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': '4',
                    'type': 'N',
                },
                'subpages': [],
            },
            self._get_pages_tree_data('t/4', depth=1),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': '1',
                    'type': 'N',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'N',
                        },
                        'subpages': [],
                        'expand_url': 't/4/1/1',
                    }
                ],
            },
            self._get_pages_tree_data('t/4/1', depth=1),
        )

    def test_hide_grids_and_redirects(self):
        # Проверим, что при соответствующей настройке скрываются гриды и редиректы.

        self._build_pages_tree(
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
                            'type': 'G',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'G',
                                    'title': 'Tree/1/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'R',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'G',
                                    'title': 'Tree/2/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'G',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'R',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'G',
                            'title': 'Tree/4',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'R',
                                    'title': 'Tree/4/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                ],
            }
        )

        self._compare_nodes(
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
                            'type': 'G',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'R',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'R',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'G',
                            'title': 'Tree/4',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'R',
                                    'title': 'Tree/4/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=2, show_grids=False),
        )

        self._compare_nodes(
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
                            'type': 'G',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'G',
                                    'title': 'Tree/1/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'R',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'G',
                                    'title': 'Tree/2/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'G',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'G',
                            'title': 'Tree/4',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=2, show_redirects=False),
        )

        self._compare_nodes(
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
                            'type': 'G',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'R',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=2, show_grids=False, show_redirects=False),
        )

    def test_subtree(self):
        # Проверим построение от супертэга поддерева.

        self._build_pages_tree(
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
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/1/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': '1',
                    'type': 'P',
                    'title': 'Tree/1',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/1/2',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t/1', depth=3),
        )

    def test_root_closed(self):
        # Проверим ситуацию, когда корневая страница - закрытая.

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'C',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    }
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'C',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    }
                ],
            },
            self._get_pages_tree_data('t', depth=3),
        )

    def test_root_nonexistent(self):
        # Проверим ситуацию, когда корневой страницы не существует.

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'N',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    }
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'N',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    }
                ],
            },
            self._get_pages_tree_data('t', depth=3),
        )

    def test_root_and_subtree_nonexistent(self):
        # Проверим ситуацию, когда по указанному префиксу вообще нет страниц.

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'N',
                },
                'subpages': [],
            },
            self._get_pages_tree_data('t', depth=3),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': '1',
                    'type': 'N',
                },
                'subpages': [],
            },
            self._get_pages_tree_data('t/1', depth=3),
        )

    def test_empty_root_supertag(self):
        # Пустой супертэг '' корневой страницы – синоним главной страницы.

        self._compare_nodes(
            {
                'page': {
                    'cluster': 'homepage',
                    'type': 'N',
                },
                'subpages': [],
            },
            self._get_pages_tree_data('', depth=3),
        )

    def test_attached_files(self):
        # Проверим отображение прикрепленных файлов. Прикрепленные файлы должны
        # отображаться для всех типов страниц, кроме закрытых. Заодно проверяем, что
        # прикрепленные файлы перечисляются в алфавитном порядке их имен.

        self._build_pages_tree(
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
                            'type': 'G',
                            'title': 'Tree/1',
                            'files': [
                                {
                                    'name': '1.txt',
                                    'url': '1',
                                }
                            ],
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'C',
                            'title': 'Tree/2',
                            'files': [
                                {
                                    'name': '2.txt',
                                    'url': '2',
                                }
                            ],
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                    'files': [
                                        {
                                            'name': '2-1_2.txt',
                                            'url': '2-1_2',
                                        },
                                        {
                                            'name': '2-1_1.txt',
                                            'url': '2-1_1',
                                        },
                                    ],
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'C',
                                    'title': 'Tree/2/2',
                                    'files': [
                                        {
                                            'name': '2-2_1.txt',
                                            'url': '2-2_1',
                                        },
                                        {
                                            'name': '2-2_2.txt',
                                            'url': '2-2_2',
                                        },
                                    ],
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/2/2/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'L',
                            'title': 'Tree/3',
                            'files': [
                                {
                                    'name': '3_3.txt',
                                    'url': '3_3',
                                },
                                {
                                    'name': '3_1.txt',
                                    'url': '3_1',
                                },
                                {
                                    'name': '3_2.txt',
                                    'url': '3_2',
                                },
                            ],
                        },
                        'subpages': [],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'C',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/2/2/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                    'files': [
                                        {
                                            'name': '2-1_1.txt',
                                            'url': '/t/2/1/.files/2-1_1?download=1',
                                        },
                                        {
                                            'name': '2-1_2.txt',
                                            'url': '/t/2/1/.files/2-1_2?download=1',
                                        },
                                    ],
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'G',
                            'title': 'Tree/1',
                            'files': [
                                {
                                    'name': '1.txt',
                                    'url': '/t/1/.files/1?download=1',
                                }
                            ],
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                            'files': [
                                {
                                    'name': '3_1.txt',
                                    'url': '/t/3/.files/3_1?download=1',
                                },
                                {
                                    'name': '3_2.txt',
                                    'url': '/t/3/.files/3_2?download=1',
                                },
                                {
                                    'name': '3_3.txt',
                                    'url': '/t/3/.files/3_3?download=1',
                                },
                            ],
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=3, show_files=True, show_grids=False),
        )

    def test_from_yandex_server(self):
        # Проверяем данные для запросов с from_yandex_server=True.
        # В этом случае правила доступа игнорируются, все страницы считаются открытыми.

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'С',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'С',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'С',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'L',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                ],
            }
        )

        self._compare_nodes(
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
                            'type': 'P',
                            'title': 'Tree/1',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=3, from_yandex_server=True),
        )

        pass

    def test_urls(self):
        # Проверим URL в данных страницы.

        self._build_pages_tree(
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
                            'type': 'C',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'url': '/t',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'url': '/t/1',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'url': '/t/1/1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'url': '/t/3',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'url': '/t/3/1',
                                    'type': 'P',
                                    'title': 'Tree/3/1',
                                },
                                'subpages': [],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'url': '/t/3/2',
                                    'type': 'P',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'url': '/t/2',
                            'type': 'P',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=3),
            compare_urls=True,
        )

    def test_subpages_count(self):
        # Проверим число подстраниц для узлов дерева.
        # Учитываются все подстраницы на следующем уровне вложенности, кроме несуществующих.
        # Страницы, которые пользователю не видны, все равно учитываются.

        self._build_pages_tree(
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
                            'type': 'C',
                            'title': 'Tree/1',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/1/1',
                                        },
                                        'subpages': [],
                                    },
                                    {
                                        'page': {
                                            'cluster': '2',
                                            'type': 'C',
                                            'title': 'Tree/1/1/2',
                                        },
                                        'subpages': [],
                                    },
                                    {
                                        'page': {
                                            'cluster': '3',
                                            'type': 'N',
                                        },
                                        'subpages': [
                                            {
                                                'page': {
                                                    'cluster': '1',
                                                    'type': 'P',
                                                    'title': 'Tree/1/1/3/1',
                                                },
                                                'subpages': [],
                                            }
                                        ],
                                    },
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'N',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/2/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '3',
                                    'type': 'P',
                                    'title': 'Tree/1/3',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages': [],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'C',
                            'title': 'Tree/4',
                        },
                        'subpages': [],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages_count': 3,
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'C',
                        },
                        'subpages_count': 2,
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'N',
                                },
                                'subpages_count': 1,
                                'subpages': [],
                                'expand_url': 't/1/2',
                            },
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/1/1',
                                },
                                'subpages_count': 2,
                                'subpages': [],
                                'expand_url': 't/1/1',
                            },
                            {
                                'page': {
                                    'cluster': '3',
                                    'type': 'P',
                                    'title': 'Tree/1/3',
                                },
                                'subpages_count': 0,
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'N',
                        },
                        'subpages_count': 1,
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'P',
                                    'title': 'Tree/2/1',
                                },
                                'subpages_count': 0,
                                'subpages': [],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages_count': 0,
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=2),
            compare_subpages_count=True,
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages_count': 3,
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'C',
                        },
                        'subpages_count': 2,
                        'subpages': [],
                        'expand_url': 't/1',
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'N',
                        },
                        'subpages_count': 1,
                        'subpages': [],
                        'expand_url': 't/2',
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'P',
                            'title': 'Tree/3',
                        },
                        'subpages_count': 0,
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=1),
            compare_subpages_count=True,
        )

    def test_show_owners(self):
        # Проверим, что в случае show_owners=True не делается
        # лишних запросов за полем page.authors за счет .prefetch_related('authors')

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'P',
                    'title': 'Tree',
                },
                'subpages': [],
            }
        )

        # Некоторое количество запросов добавляется из-за вызова
        # is_admin() внутри UserSerializer
        with self.assertNumQueries(7 if settings.IS_INTRANET else 7):
            self._get_pages_tree_data('t', show_owners=True)

    def test_marvelous_tree(self):
        # И в заключение, потестируем большое изумительное дерево с разными типами страниц.

        # Схематичная структура дерева:
        #
        # C
        #   C
        #     C
        #       P
        #   R
        #     C
        #       C
        #     N
        #       G
        #   N
        #     N
        #       P
        #       C
        #     P
        #       C
        #         N
        #           G
        #   C
        #     C
        #       N
        #         P
        #       C
        #         C
        #         R
        #           G
        #   N
        #     C
        #       R
        #         N
        #           G
        #

        self._build_pages_tree(
            {
                'page': {
                    'cluster': 't',
                    'type': 'C',
                    'title': 'Tree',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'C',
                            'title': 'Tree/1',
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
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'R',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                    'title': 'Tree/2/1',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'C',
                                            'title': 'Tree/2/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'N',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'G',
                                            'title': 'Tree/2/2/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'N',
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
                                            'type': 'P',
                                            'title': 'Tree/3/1/1',
                                        },
                                        'subpages': [],
                                    },
                                    {
                                        'page': {
                                            'cluster': '2',
                                            'type': 'C',
                                            'title': 'Tree/3/1/2',
                                        },
                                        'subpages': [],
                                    },
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'C',
                                            'title': 'Tree/3/2/1',
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
                                                            'type': 'G',
                                                            'title': 'Tree/3/2/1/1/1',
                                                        },
                                                        'subpages': [],
                                                    }
                                                ],
                                            }
                                        ],
                                    }
                                ],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'C',
                            'title': 'Tree/4',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                    'title': 'Tree/4/1',
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
                                                    'type': 'P',
                                                    'title': 'Tree/4/1/1/1',
                                                },
                                                'subpages': [],
                                            }
                                        ],
                                    },
                                    {
                                        'page': {
                                            'cluster': '2',
                                            'type': 'C',
                                            'title': 'Tree/4/1/2',
                                        },
                                        'subpages': [
                                            {
                                                'page': {
                                                    'cluster': '1',
                                                    'type': 'C',
                                                    'title': 'Tree/4/1/2/1',
                                                },
                                                'subpages': [],
                                            },
                                            {
                                                'page': {
                                                    'cluster': '2',
                                                    'type': 'R',
                                                    'title': 'Tree/4/1/2/2',
                                                },
                                                'subpages': [
                                                    {
                                                        'page': {
                                                            'cluster': '1',
                                                            'type': 'G',
                                                            'title': 'Tree/4/1/2/2/1',
                                                        },
                                                        'subpages': [],
                                                    }
                                                ],
                                            },
                                        ],
                                    },
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '5',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                    'title': 'Tree/5/1',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'R',
                                            'title': 'Tree/5/1/1',
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
                                                            'type': 'G',
                                                            'title': 'Tree/5/1/1/1/1',
                                                        },
                                                        'subpages': [],
                                                    }
                                                ],
                                            }
                                        ],
                                    }
                                ],
                            }
                        ],
                    },
                ],
            }
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'C',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'N',
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
                                            'type': 'P',
                                            'title': 'Tree/3/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
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
                                                    'type': 'P',
                                                    'title': 'Tree/4/1/1/1',
                                                },
                                                'subpages': [],
                                            }
                                        ],
                                    },
                                    {
                                        'page': {
                                            'cluster': '2',
                                            'type': 'C',
                                        },
                                        'subpages': [
                                            {
                                                'page': {
                                                    'cluster': '2',
                                                    'type': 'R',
                                                    'title': 'Tree/4/1/2/2',
                                                },
                                                'subpages': [],
                                            }
                                        ],
                                    },
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '5',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'R',
                                            'title': 'Tree/5/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'R',
                            'title': 'Tree/2',
                        },
                        'subpages': [],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=4, show_grids=False),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'C',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [],
                                'expand_url': 't/1/1',
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'N',
                                },
                                'subpages': [],
                                'expand_url': 't/3/1',
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'R',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'N',
                                },
                                'subpages': [],
                                'expand_url': 't/2/2',
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=2, show_redirects=False),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'C',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'N',
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
                                            'type': 'P',
                                            'title': 'Tree/3/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'N',
                                        },
                                        'subpages': [],
                                        'expand_url': 't/4/1/1',
                                    },
                                    {
                                        'page': {
                                            'cluster': '2',
                                            'type': 'C',
                                        },
                                        'subpages': [],
                                        'expand_url': 't/4/1/2',
                                    },
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '5',
                            'type': 'N',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'R',
                                            'title': 'Tree/5/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '2',
                            'type': 'R',
                            'title': 'Tree/2',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'N',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'G',
                                            'title': 'Tree/2/2/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=3),
        )

        self._compare_nodes(
            {
                'page': {
                    'cluster': 't',
                    'type': 'C',
                },
                'subpages': [
                    {
                        'page': {
                            'cluster': '1',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'P',
                                            'title': 'Tree/1/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            }
                        ],
                    },
                    {
                        'page': {
                            'cluster': '3',
                            'type': 'N',
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
                                            'type': 'P',
                                            'title': 'Tree/3/1/1',
                                        },
                                        'subpages': [],
                                    }
                                ],
                            },
                            {
                                'page': {
                                    'cluster': '2',
                                    'type': 'P',
                                    'title': 'Tree/3/2',
                                },
                                'subpages': [],
                            },
                        ],
                    },
                    {
                        'page': {
                            'cluster': '4',
                            'type': 'C',
                        },
                        'subpages': [
                            {
                                'page': {
                                    'cluster': '1',
                                    'type': 'C',
                                },
                                'subpages': [
                                    {
                                        'page': {
                                            'cluster': '1',
                                            'type': 'N',
                                        },
                                        'subpages': [],
                                        'expand_url': 't/4/1/1',
                                    }
                                ],
                            }
                        ],
                    },
                ],
            },
            self._get_pages_tree_data('t', depth=3, show_grids=False, show_redirects=False),
        )
