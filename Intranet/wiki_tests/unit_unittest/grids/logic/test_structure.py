
from wiki.grids.logic import structure
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

GRID_BODY = {
    'idx': {
        '2': 0,
        '1': 1,
    },
    'data': [
        {
            '9': {'raw': 'ok'},
            '5': {'raw': ''},
            '33': {'raw': True},
            '__key__': '2',
        },
        {
            '33': {'raw': False},
            '5': {'raw': ''},
            '9': {'raw': 'wat'},
            '__key__': '1',
        },
    ],
    'structure': {
        'fields': [
            {'name': '9'},
            {'name': '33'},
            {'name': '5'},
        ],
    },
}


class TestGridWrapper(BaseApiTestCase):
    def setUp(self):
        self.wrapped = structure.GridWrapper(GRID_BODY)

    def test_rows_keys(self):
        self.assertEqual(self.wrapped.rows_keys, ['2', '1'])

    def test_columns_keys(self):
        self.assertEqual(self.wrapped.columns_keys, ['9', '33', '5'])

    def test_getitem(self):
        self.assertEqual(self.wrapped['2']['33'], {'raw': True})
        self.assertEqual(self.wrapped[('2', '33')], {'raw': True})

    def test_iter(self):
        row_keys = list(self.wrapped)
        self.assertEqual(row_keys, ['2', '1'])
