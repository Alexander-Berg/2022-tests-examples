
from pretend import stub

from wiki.grids.logic.columns import column_attribute_by_names, column_by_name, columns_by_type, remove_column
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class ColumnByNameTestCase(BaseApiTestCase):
    def test_it_returns_column(self):
        hello_field = {
            'name': 'hello',
        }
        grid = stub(columns_meta=lambda: [hello_field])
        self.assertEqual(column_by_name(grid, 'hello'), hello_field)

    def test_column_does_not_exist(self):
        good_day_field = {
            'name': 'good_day',
        }
        grid = stub(columns_meta=lambda: [good_day_field])
        self.assertEqual(column_by_name(grid, 'hello'), None)


class ColumnsByTypeTestCase(BaseApiTestCase):
    def test_columns_by_type(self):
        def columns(*args):
            return [
                (0, 'string', {'name': '1', 'type': 'string'}),
                (1, 'int', {'name': '2', 'type': 'int'}),
            ]

        grid = stub(columns=columns)
        self.assertEqual([{'name': '1', 'type': 'string'}], list(columns_by_type(grid, 'string')))
        self.assertEqual([], list(columns_by_type(grid, 'number')))
        # порядок следования в гриде сохраняется
        self.assertEqual(
            [
                {'name': '1', 'type': 'string'},
                {'name': '2', 'type': 'int'},
            ],
            list(columns_by_type(grid, 'int', 'string')),
        )

    def test_empty_filter(self):
        columns = [
            {'name': '1', 'type': 'string'},
            {'name': '2', 'type': 'int'},
        ]
        grid = stub(columns_meta=lambda: columns)
        self.assertEqual(columns, list(columns_by_type(grid)))


class ColumnAttrsByNamesTestCase(BaseApiTestCase):
    def test_column_attrs_by_name(self):
        columns_meta = [
            {'name': '1', 'type': 'string'},
            {'name': '2', 'type': 'int'},
        ]
        grid = stub(columns_meta=lambda *args: columns_meta)
        self.assertEqual(
            ['string', 'int'],
            list(column_attribute_by_names(grid, 'type', '1', '2')),
        )


class RemoveColumnTestCase(BaseApiTestCase):
    def test_it_removes(self):
        column = {
            'type': 'string',
        }
        grid = stub(
            column_by_name=lambda *args: column,
            access_structure={
                'fields': [column],
            },
            access_data=[{'deleteme': None, 'iamleftalone': ''}, {'iamtheonlyone': ''}],
        )
        remove_column(grid, 'deleteme')
        self.assertEqual(grid.access_data[0], {'iamleftalone': ''})
        self.assertEqual(grid.access_data[1], {'iamtheonlyone': ''})
        self.assertEqual(grid.access_structure, {'fields': []})
