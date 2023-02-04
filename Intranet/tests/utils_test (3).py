from itertools import chain

from django.test import TestCase

from staff.lib.utils.tree import flat_to_hierarchical
from staff.lib.utils.tree import flat_to_hierarchical_using_level
from staff.anketa.models import OfficeSetting as SampleModel
from staff.lib.utils.admin import StaffModelAdmin


class TreeUtilsTest(TestCase):
    def test_flat_to_hierarchical_using_level(self):
        flat_tree = [
            {'name': '__dep__', 'level': 0},
            {'name': 'yandex', 'level': 1},
            {'name': 'yandex_infra', 'level': 2},
            {'name': 'yandex_comm', 'level': 2},
            {'name': 'yandex_comm_smth', 'level': 3},
            {'name': 'yandex_comm_smth_smth2', 'level': 4},
            {'name': 'yandex_mant', 'level': 2},
            {'name': 'ext', 'level': 1},
        ]

        expected_tree = {
            'name': '__dep__',
            'level': 0,
            'descendants': [
                {
                    'name': 'yandex',
                    'level': 1,
                    'descendants': [
                        {'name': 'yandex_infra', 'level': 2},
                        {
                            'name': 'yandex_comm',
                            'level': 2,
                            'descendants': [
                                {
                                    'name': 'yandex_comm_smth',
                                    'level': 3,
                                    'descendants': [
                                        {'name': 'yandex_comm_smth_smth2', 'level': 4},

                                    ]
                                },
                            ]
                        },
                        {'name': 'yandex_mant', 'level': 2},
                    ]
                },
                {'name': 'ext', 'level': 1},
            ]
        }

        self.assertEqual(
            flat_to_hierarchical_using_level(flat_tree),
            expected_tree
        )

    def test_flat_to_hierarchical(self):
        flat_tree = [
            {'name': '__dep__', 'id': 0, 'parent_id': None},
            {'name': 'yandex', 'id': 1, 'parent_id': 0},
            {'name': 'yandex_infra', 'id': 2, 'parent_id': 1},
            {'name': 'yandex_comm', 'id': 3, 'parent_id': 1},
            {'name': 'yandex_comm_smth', 'id': 4, 'parent_id': 3},
            {'name': 'yandex_comm_smth_smth2', 'id': 5, 'parent_id': 4},
            {'name': 'yandex_mant', 'id': 6, 'parent_id': 1},
            {'name': 'ext', 'id': 7, 'parent_id': 0},
        ]

        expected_tree = {
            'name': '__dep__',
            'id': 0,
            'parent_id': None,
            'descendants': [
                {
                    'name': 'yandex',
                    'id': 1,
                    'parent_id': 0,
                    'descendants': [
                        {'name': 'yandex_infra', 'id': 2, 'parent_id': 1},
                        {
                            'name': 'yandex_comm',
                            'id': 3,
                            'parent_id': 1,
                            'descendants': [
                                {
                                    'name': 'yandex_comm_smth',
                                    'id': 4,
                                    'parent_id': 3,
                                    'descendants': [
                                        {'name': 'yandex_comm_smth_smth2', 'id': 5, 'parent_id': 4},

                                    ]
                                },
                            ]
                        },
                        {'name': 'yandex_mant', 'id': 6, 'parent_id': 1},
                    ]
                },
                {'name': 'ext', 'id': 7, 'parent_id': 0},
            ]
        }

        self.assertEqual(
            flat_to_hierarchical(flat_tree),
            expected_tree
        )


class StaffModelAdminTest(TestCase):
    field = 'field'
    sub_fields = ['field1', 'field2', 'field3', 'field4']
    result_tuple = (
        'field__field1',
        'field__field2',
        'field__field3',
        'field__field4',
    )

    def setUp(self):
        self.sma = StaffModelAdmin(SampleModel, '')

    def test_add_search_fields_to_none(self):
        self.sma._add_search(self.field, self.sub_fields)
        self.assertEqual(self.sma.search_fields, self.result_tuple)

    def test_add_search_fields_preserving_order(self):
        self.sma.search_fields = tuple(self.result_tuple[2:])
        self.sma._add_search(self.field, self.sub_fields)
        expected_val = chain(self.result_tuple[2:], self.result_tuple[:2])
        self.assertEqual(self.sma.search_fields, tuple(expected_val))

    def test_add_filter_to_none(self):
        self.sma._add_filter(self.field)
        self.assertEqual(self.sma.list_filter, (self.field, ))

    def test_add_filter_preserving_order(self):
        self.sma.list_filter = (self.sub_fields[-1], )
        self.sma._add_filter(self.sub_fields[0])
        expected_val = (self.sub_fields[-1], self.sub_fields[0])
        self.assertEqual(self.sma.list_filter, expected_val)
