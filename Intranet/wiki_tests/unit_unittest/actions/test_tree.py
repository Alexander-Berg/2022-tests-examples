
from mock import patch

from wiki.actions.classes.base_action import ParamsWrapper
from wiki.actions.classes.tree import PagesTree, Tree
from wiki.pages.pages_tree_new import Sort, SortBy
from intranet.wiki.tests.wiki_tests.unit_unittest.actions.base import OldHttpActionTestCase


class TreeTest(OldHttpActionTestCase):
    def setUp(self):
        super(TreeTest, self).setUp()
        self.request.supertag = 'tag3'

    def _encode(self, dict_params):
        params = ParamsWrapper(dict_params)
        return Tree(params, self.request).encode_params(params)

    def test_encode_page_param_by_page_param(self):
        # Приоритет: params['page'] > params['for'] > self.request.tag
        encoded_params = self._encode({'page': 'tag1', 'for': 'tag2'})
        self.assertTrue('page' in encoded_params, '\'page\' must be in encoded params')
        self.assertFalse('for' in encoded_params, '\'for\' must be removed from encoded params')
        self.assertEqual('tag1', encoded_params['page'])

    def test_encode_page_param_by_for_param(self):
        # Приоритет: params['for'] > self.request.tag
        encoded_params = self._encode({'for': 'tag2'})
        self.assertTrue('page' in encoded_params, '\'page\' must be in encoded params')
        self.assertFalse('for' in encoded_params, '\'for\' must be removed from encoded params')
        self.assertEqual('tag2', encoded_params['page'])

    def test_encode_page_param_by_request_tag(self):
        encoded_params = self._encode({})
        self.assertTrue('page' in encoded_params, '\'page\' must be in encoded params')
        self.assertEqual('tag3', encoded_params['page'])

    def test_encode_nomark_param_by_nomark_param(self):
        # Приоритет params['nomark'] > params['subtree']
        encoded_params = self._encode({'nomark': True, 'subtree': False})
        self.assertTrue('nomark' in encoded_params, '\'nomark\' must be in encoded params')
        self.assertFalse('subtree' in encoded_params, '\'subtree\' must be removed from encoded params')
        self.assertTrue(encoded_params['nomark'])

    def test_encode_nomark_param_by_subtree_param(self):
        encoded_params = self._encode({'subtree': False})
        self.assertTrue('nomark' in encoded_params, '\'nomark\' must be in encoded params')
        self.assertFalse('subtree' in encoded_params, '\'subtree\' must be removed from encoded params')
        self.assertFalse(encoded_params['nomark'])

    def test_pages_tree_call(self):
        params = {
            'page': 't',
            'depth': '2',
            'show_redirects': 'True',
            'show_grids': 'False',
            'show_files': 'True',
            'show_owners': 'True',
            'show_titles': 'False',
            'show_created_at': 'False',
            'show_modified_at': 'True',
            'sort_by': SortBy.MODIFIED_AT,
            'sort': Sort.DESC,
            'authors': [],
        }

        class PagesTreeMock(PagesTree):
            def __init__(self, **kwargs):
                kwargs.pop('expand_subtree_url_builder')
                kwargs.pop('user')
                kwargs.pop('from_yandex_server')
                self.data = kwargs

        with patch('wiki.actions.classes.tree.PagesTree', PagesTreeMock):
            args = Tree(ParamsWrapper(params), self.request).json_for_get(params)

            self.assertEqual(
                {
                    'root_supertag': 't',
                    'depth': 2,
                    'show_redirects': True,
                    'show_grids': False,
                    'show_files': True,
                    'show_owners': True,
                    'show_titles': False,
                    'show_created_at': False,
                    'show_modified_at': True,
                    'sort_by': SortBy.MODIFIED_AT,
                    'sort': Sort.DESC,
                    'authors': [],
                },
                args,
            )
