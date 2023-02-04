# -*- coding: utf-8 -*-
from tests.test_fixtures.dbpool import DummyDbPool

import unittest
import pytest

from at.common.Rubrics import *


database = {
    'Table': [
        (1, 'a', 'first'),
        (2, 'b', 'second'),
        (3, 'a', 'third'),
    ],
    'CommunitiesRubrics': [
        (1, 'о работе', 'about work', 0, 'work'),
        (3, 'о жизни', 'about life', 0, 'life'),
        (0, '', '', 1, 'adult'),
        ]
}
POOL = DummyDbPool(database)


@pytest.mark.skip
@pytest.mark.django_db
class TestMagicTables(unittest.TestCase):

    def test_works(self):
        ''' Loads all data and iterates over it '''
        POOL._expect('Table')
        table = MagicTable('Table', 'id, char, order')()
        assert hasattr(table, 'all_ids')
        assert len(list(table)) == 3
        self.assertEqual(list(table)[0], {'id': 1, 'char': 'a', 'order': 'first'})

    def test_gets_by(self):
        ''' Gets items by value (returns single item even if many match) '''
        POOL._expect('Table')
        table = MagicTable('Table', 'id, char, order')()
        assert hasattr(table, 'by_char')
        assert table.by_char('b') == {'id': 2, 'char': 'b', 'order': 'second'}
        assert table.by_char('a') == {'id': 1, 'char': 'a', 'order': 'first'}

    def test_filters(self):
        ''' Filters by value (returns multiple values) '''
        POOL._expect('Table')
        table = MagicTable('Table', 'id, char, order')()
        assert hasattr(table, 'filter_order')
        assert table.filter_char('a') == [
                {'id': 1, 'char': 'a', 'order': 'first'},
                {'id': 3, 'char': 'a', 'order': 'third'},
                ]

    def test_gets_all(self):
        ''' Returns all values of a given field '''
        POOL._expect('Table')
        table = MagicTable('Table', 'id, char, order')()
        assert hasattr(table, 'all_orders')
        self.assertEqual(table.all_orders(), ['first', 'second', 'third'])

    def test_takes_constructor(self):
        POOL._expect('Table')
        table = MagicTable('Table', 'id, char, order', constructor = lambda x: set(x.values()))()
        assert table.by_id(1) == set([1, 'a', 'first'])

    def test_rubrics(self):
        POOL._expect('CommunitiesRubrics')
        table = Rubrics()
        for rub in table:
            assert hasattr(rub, 'rubric_id'), rub.__dict__
