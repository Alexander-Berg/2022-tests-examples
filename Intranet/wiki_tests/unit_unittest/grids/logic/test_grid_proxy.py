import pytest

from unittest import TestCase

from wiki.api_frontend.logic import GridProxy
from wiki.grids.utils import dummy_request_for_grids, insert_rows

pytestmark = [pytest.mark.django_db]


class GridProxyTestCase(TestCase):
    def get_grid(self, sorting):
        from wiki.grids.models import Grid

        grid = Grid()
        grid.access_structure = {
            'fields': [{'type': 'string', 'name': 'first'}, {'type': 'string', 'name': 'second'}],
            'sorting': sorting,
        }
        return grid

    def insert_rows(self, grid, rows):
        insert_rows(grid, rows, request=dummy_request_for_grids())

    def grid_values(self, rows):
        return [[cell.get('raw') for cell in row] for row in rows]

    def test_it_sorts(self):
        grid = self.get_grid([{'name': 'first', 'type': 'asc'}])
        self.insert_rows(
            grid,
            [
                {'first': 'Олег', 'second': 'Олегов'},
                {'first': 'Ольга', 'second': 'Ольгина'},
                {'first': 'Олег', 'second': 'Вещий'},
            ],
        )
        proxy = GridProxy(grid)
        values = self.grid_values(proxy.rows)
        self.assertEqual(['Олег', 'Олегов'], values[0])
        self.assertEqual(['Олег', 'Вещий'], values[1])
        self.assertEqual(['Ольга', 'Ольгина'], values[2])

    def test_it_sorts_by_two_fields(self):
        grid = self.get_grid(
            [
                {'name': 'first', 'type': 'asc'},
                {'name': 'second', 'type': 'desc'},
            ]
        )
        self.insert_rows(
            grid,
            [
                {'first': 'Олег', 'second': 'Олегов'},
                {'first': 'Ольга', 'second': 'Ольгина'},
                {'first': 'Олег', 'second': 'Вещий'},
                {'first': 'Ольга', 'second': 'Игоревна'},
            ],
        )
        proxy = GridProxy(grid)
        values = self.grid_values(proxy.rows)
        self.assertEqual(['Олег', 'Олегов'], values[0])
        self.assertEqual(['Олег', 'Вещий'], values[1])
        self.assertEqual(['Ольга', 'Ольгина'], values[2])
        self.assertEqual(['Ольга', 'Игоревна'], values[3])

    def test_it_sorts_by_two_fields_backwards(self):
        grid = self.get_grid(
            [
                {'name': 'first', 'type': 'desc'},
                {'name': 'second', 'type': 'desc'},
            ]
        )
        self.insert_rows(
            grid,
            [
                {'first': 'Олег', 'second': 'Олегов'},
                {'first': 'Ольга', 'second': 'Ольгина'},
                {'first': 'Олег', 'second': 'Вещий'},
                {'first': 'Ольга', 'second': 'Игоревна'},
            ],
        )
        proxy = GridProxy(grid)
        values = self.grid_values(proxy.rows)
        self.assertEqual(['Ольга', 'Ольгина'], values[0])
        self.assertEqual(['Ольга', 'Игоревна'], values[1])
        self.assertEqual(['Олег', 'Олегов'], values[2])
        self.assertEqual(['Олег', 'Вещий'], values[3])

    def test_it_sorts_by_two_fields_third(self):
        grid = self.get_grid(
            [
                {'name': 'first', 'type': 'asc'},
                {'name': 'second', 'type': 'asc'},
            ]
        )
        self.insert_rows(
            grid,
            [
                {'first': 'Олег', 'second': 'Олегов'},
                {'first': 'Ольга', 'second': 'Ольгина'},
                {'first': 'Олег', 'second': 'Вещий'},
                {'first': 'Ольга', 'second': 'Игоревна'},
            ],
        )
        proxy = GridProxy(grid)
        values = self.grid_values(proxy.rows)
        self.assertEqual(['Олег', 'Вещий'], values[0])
        self.assertEqual(['Олег', 'Олегов'], values[1])
        self.assertEqual(['Ольга', 'Игоревна'], values[2])
        self.assertEqual(['Ольга', 'Ольгина'], values[3])
