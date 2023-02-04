
from wiki.grids.filter.base import FilterError
from wiki.grids.filter.evaluator import evaluate
from wiki.grids.filter.parser import parse
from wiki.grids.utils import insert_rows
from wiki.utils.callsuper import callsuper
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.utils import locmemcache
from intranet.wiki.tests.wiki_tests.unit_unittest.grids.base import (
    GRID_WITH_ALL_KIND_OF_FIELDS_STRUCTURE,
    create_grid,
    default_row,
)


@locmemcache('grid_filter')
class EvaluatorTest(BaseTestCase):
    @callsuper
    def setUp(self):
        self.grid = create_grid(
            GRID_WITH_ALL_KIND_OF_FIELDS_STRUCTURE,
            tag='testgrid',
            owner=self.get_or_create_user('alice'),
        )

    def test_in(self):
        h1, h2 = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': 'hot'}),
            ],
            None,
        )
        input = "[0] = 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(h1, in_)
        self.assertNotIn(h2, in_)

    def test_out(self):
        h1, h2 = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': 'hot'}),
            ],
            None,
        )
        input = "[0] = 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(h2, out)
        self.assertNotIn(h1, out)

    def test_conditions_are_fine(self):
        h1, h2 = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': 'hot'}),
            ],
            None,
        )
        input = "[0] = 'cool'"
        semantic_tree = parse(input)
        with self.assertRaises(AssertionError):
            with self.assertRaises(FilterError):
                evaluate(semantic_tree, self.grid)

    def test_combinators_are_fine(self):
        insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),  # , '1': 50}),
                default_row(self.grid, {'0': 'hot', '1': 100}),
            ],
            None,
        )
        input = "[0] = 'cool', [1] = 50"
        semantic_tree = parse(input)
        with self.assertRaises(AssertionError):
            with self.assertRaises(FilterError):
                evaluate(semantic_tree, self.grid)

    def test_no_rows_is_fine(self):
        input = "[0] = 'cool'"
        semantic_tree = parse(input)
        with self.assertRaises(AssertionError):
            with self.assertRaises(FilterError):
                evaluate(semantic_tree, self.grid)

    def test_equal_strings(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': 'hot'}),
                default_row(self.grid, {'0': 'not cool'}),
                default_row(self.grid),
            ],
            None,
        )
        input = "[0] = 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], out)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)

    def test_equal_numbers(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'1': 50}),
                default_row(self.grid, {'1': 50.0}),
                default_row(self.grid, {'1': 100}),
                default_row(self.grid),
            ],
            None,
        )
        input = '[1] = 50'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)

    def test_in_dates(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'3': '2012-10-01'}),
                default_row(self.grid, {'3': '2012-10-02'}),
                default_row(self.grid, {'3': '2012-10-03'}),
                default_row(self.grid),
            ],
            None,
        )
        input = '[3] in (2012-10-01, 2012-10-02)'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)

    def test_equal_multiple_selects(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'5': ['cool']}),
                default_row(self.grid, {'5': ['not cool', 'cool', 'hot']}),
                default_row(self.grid, {'5': ['hot']}),
                default_row(self.grid, {'5': []}),
                default_row(self.grid),
            ],
            None,
        )
        input = "[5] = 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_equal_users(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'6': 'alice'}),
                default_row(self.grid, {'6': 'bob'}),
                default_row(self.grid),
            ],
            None,
        )
        input = '[6] = alice@'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], out)
        self.assertIn(hashes[2], out)

    def test_equal_string_and_number(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': '50'}),
                default_row(self.grid, {'0': '+50.0'}),
                default_row(self.grid, {'0': '100'}),
                default_row(self.grid, {'0': 'banana'}),
                default_row(self.grid),
            ],
            None,
        )
        input = '[0] = 50.0'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_in_multiple_selects(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'5': ['cool']}),
                default_row(self.grid, {'5': ['hot', 'banana']}),
                default_row(self.grid, {'5': ['apple', 'banana']}),
                default_row(self.grid, {'5': []}),
                default_row(self.grid),
            ],
            None,
        )
        input = "[5] in ('cool', 'hot')"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_not_equal_strings(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'hot'}),
                default_row(self.grid, {'0': 'not hot'}),
                default_row(self.grid, {'0': 'not cool'}),
                default_row(self.grid, {'0': ''}),
                default_row(self.grid),
                default_row(self.grid, {'0': 'cool'}),
            ],
            None,
        )
        input = "[0] != 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], in_)
        self.assertIn(hashes[4], in_)
        self.assertIn(hashes[5], out)

    def test_normalization(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': '  cool      '}),
                default_row(self.grid, {'0': 'COOL'}),
                default_row(self.grid, {'0': '      CoOl'}),
                default_row(self.grid),
            ],
            None,
        )
        input = "[0] == 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], in_)
        self.assertIn(hashes[4], out)

    def test_contains(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': 'not cool'}),
                default_row(self.grid, {'0': 'hot'}),
                default_row(self.grid, {'0': ''}),
                default_row(self.grid),
            ],
            None,
        )
        input = "[0] ~ 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_not_contains(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'hot'}),
                default_row(self.grid, {'0': ''}),
                default_row(self.grid),
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': 'not cool'}),
            ],
            None,
        )
        input = "[0] !~ 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_greater(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'1': 51}),
                default_row(self.grid, {'1': 50}),
                default_row(self.grid, {'1': 0}),
                default_row(self.grid),
            ],
            None,
        )
        input = '[1] > 50'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], out)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)

    def test_between(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'3': '2010-10-11'}),
                default_row(self.grid, {'3': '2010-10-10'}),
                default_row(self.grid, {'3': '2010-10-20'}),
                default_row(self.grid, {'3': '2010-10-09'}),
                default_row(self.grid, {'3': '2010-10-21'}),
                default_row(self.grid),
            ],
            None,
        )
        input = '[3] between 2010-10-10 and 2010-10-20'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)
        self.assertIn(hashes[5], out)

    def test_empty(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': ''}),
                default_row(self.grid, {'0': '           '}),
                default_row(self.grid),
                default_row(self.grid, {'0': 'cool'}),
            ],
            None,
        )
        input = '[0] is empty'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], out)

    def test_null_users(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'6': ''}),
                default_row(self.grid),
                default_row(self.grid, {'6': 'alice'}),
            ],
            None,
        )
        input = '[6] in ()'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], out)

    def test_and(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'haxor', '1': 5}),
                default_row(self.grid, {'0': 'haxor', '1': 50}),
                default_row(self.grid, {'0': 'user', '1': 5}),
                default_row(self.grid, {'0': 'user', '1': 50}),
                default_row(self.grid),
            ],
            None,
        )
        input = "[0] ~ 'x' and [1] <= 13"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], out)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_or(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'haxor', '1': 5}),
                default_row(self.grid, {'0': 'haxor', '1': 50}),
                default_row(self.grid, {'0': 'user', '1': 5}),
                default_row(self.grid, {'0': 'user', '1': 50}),
                default_row(self.grid),
            ],
            None,
        )
        input = "[0] ~ 'x' or [1] <= 13"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_empty_filter(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': 'hot'}),
                default_row(self.grid, {'0': ''}),
                default_row(self.grid),
            ],
            None,
        )
        input = ''
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], in_)

    def test_single_words_may_go_without_quotation(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'cool'}),
                default_row(self.grid, {'0': 'hot'}),
                default_row(self.grid, {'0': ''}),
                default_row(self.grid),
            ],
            None,
        )
        input = '[0] = cool'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], out)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)

    def test_cyrillic_is_allowed_in_strings(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'0': 'холодно'}),
                default_row(self.grid, {'0': 'горячо'}),
                default_row(self.grid, {'0': ''}),
                default_row(self.grid),
            ],
            None,
        )
        input = "[0] in ('холодно', горячо)"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], out)
        self.assertIn(hashes[3], out)

    def test_not_equal_multiple_selects(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'5': ['hot']}),
                default_row(self.grid, {'5': []}),
                default_row(self.grid),
                default_row(self.grid, {'5': ['cool']}),
                default_row(self.grid, {'5': ['not cool', 'cool', 'hot']}),
            ],
            None,
        )
        input = "[5] != 'cool'"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_not_in_multiple_selects(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'5': ['apple', 'banana']}),
                default_row(self.grid, {'5': []}),
                default_row(self.grid),
                default_row(self.grid, {'5': ['cool']}),
                default_row(self.grid, {'5': ['hot', 'cool']}),
                default_row(self.grid, {'5': ['hot', 'banana']}),
            ],
            None,
        )
        input = "[5] not in ('cool', 'hot')"
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)
        self.assertIn(hashes[5], out)

    def test_not_greater(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'1': 50}),
                default_row(self.grid, {'1': 49}),
                default_row(self.grid, {'1': -1000}),
                # default_row(self.grid),  # I don't know...
                default_row(self.grid, {'1': 51}),
                default_row(self.grid, {'1': 50.1}),
            ],
            None,
        )
        input = 'not [1] > 50'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)

    def test_not_between(self):
        hashes = insert_rows(
            self.grid,
            [
                default_row(self.grid, {'1': 9.5}),
                default_row(self.grid, {'1': 20.1}),
                default_row(self.grid),
                default_row(self.grid, {'1': 10}),
                default_row(self.grid, {'1': 20}),
                default_row(self.grid, {'1': 15}),
            ],
            None,
        )
        input = 'not [1] between 10 and 20'
        semantic_tree = parse(input)
        in_, out = evaluate(semantic_tree, self.grid)
        self.assertIn(hashes[0], in_)
        self.assertIn(hashes[1], in_)
        self.assertIn(hashes[2], in_)
        self.assertIn(hashes[3], out)
        self.assertIn(hashes[4], out)
        self.assertIn(hashes[5], out)

    def test_error_if_no_such_column(self):
        input = '[99] = boo'
        semantic_tree = parse(input)
        with self.assertRaises(FilterError):
            evaluate(semantic_tree, self.grid)

    def test_error_if_no_such_column_2(self):
        input = '[0] = boo, ([0] = far OR [99] between -10 and 10)'
        semantic_tree = parse(input)
        with self.assertRaises(FilterError):
            evaluate(semantic_tree, self.grid)
