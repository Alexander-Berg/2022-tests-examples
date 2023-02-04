
from wiki.grids.logic import diff
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class TestGridDiff(BaseApiTestCase):
    def test_get_first_common_element_found(self):
        test_data = (
            ([1, 2], [1], 1),
            ([1, 2], [2], 2),
            ([1, 2], [2, 1], 1),
            ([1, 2], [3, 1], 1),
            ([1, 2, 3], [3, 2], 2),
            ([1, 2], [3, 4], None),
        )
        for one, two, expected in test_data:
            self.assertEqual(
                diff._get_first_common_element(one, two),
                expected,
                msg=repr((one, two, expected)),
            )

    def test_smart_merge_key_lists(self):
        test_data = (
            ([], [], []),
            ([1, 2], [1, 2], [1, 2]),
            ([1, 2], [3, 4], [3, 4, 1, 2]),
            ([1], [1, 2], [1, 2]),
            ([1, 2], [1], [1, 2]),
            ([1, 2], [3, 2], [3, 1, 2]),
            ([1, 3, 4], [1, 2, 3], [1, 2, 3, 4]),
            ([1, 2, 3], [], [1, 2, 3]),
        )
        for one, two, expected in test_data:
            merged = diff._smart_merge_key_lists(one, two)
            self.assertEqual(
                merged,
                expected,
                msg=repr((one, two, expected, merged)),
            )


GRID_OLD = {
    'data': [
        {
            '5': {'raw': True},
            '9': {'raw': 'wow'},
            '__key__': '10',
        },
        {
            '5': {'raw': False},
            '9': {'raw': 'hello'},
            '__key__': '20',
        },
    ],
    'idx': {'20': 1, '10': 0},
    'structure': {
        'fields': [
            {'name': '5', 'title': 'one'},
            {'name': '9', 'title': 'two'},
        ],
    },
}

GRID_NEW = {
    'data': [
        {
            '5': {'raw': False},
            '9': {'raw': 'wob'},
            '__key__': '10',
        },
        {
            '5': {'raw': False},
            '9': {'raw': 'wtf'},
            '__key__': '30',
        },
    ],
    'idx': {'10': 0, '30': 1},
    'structure': {
        'fields': [
            {'name': '5', 'title': 'one edited'},
            {'name': '9', 'title': 'two'},
        ],
    },
}


class TestBuildGridDiff(BaseApiTestCase):
    def setUp(self):
        self.diff, self.titles = diff.build_diff(GRID_OLD, GRID_NEW)

    def test_rows_count(self):
        self.assertEqual(len(self.diff), 3)

    def test_rows_keys(self):
        self.assertEqual(list(self.diff.keys()), ['10', '30', '20'])

    def test_columns_keys(self):
        self.assertEqual(list(list(self.diff.values())[0].keys()), ['5', '9'])

    def test_diff_changed_cell(self):
        self.assertEqual(
            self.diff['10']['5'],
            {'old': True, 'new': False},
        )

    def test_diff_add_row(self):
        self.assertEqual(
            self.diff['30']['5'],
            {'new': False},
        )

    def test_diff_del_row(self):
        self.assertEqual(
            self.diff['20']['9'],
            {'old': 'hello'},
        )
