
import pickle

from django.core.cache import caches

from wiki.grids.filter.parser import ParserError, make_cache_key, parse
from wiki.grids.filter.semantictree import Combinator, Condition, Value
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.utils import locmemcache


@locmemcache('grid_filter')
class ParserTest(BaseTestCase):
    def test_empty_input(self):
        input = ''
        semantic_tree = parse(input)
        self.assertIs(semantic_tree, None)

    def test_single_condition(self):
        input = "[13] = 'cool'"
        semantic_tree = parse(input)
        self.assertIsInstance(semantic_tree, Condition)
        self.assertEqual(semantic_tree.column, '13')
        self.assertEqual(semantic_tree.operator, 'EQUAL')
        self.assertEqual(len(semantic_tree.args), 1)
        self.assertIsInstance(semantic_tree.args[0], Value)
        self.assertEqual(semantic_tree.args[0].type, 'STRING')
        self.assertEqual(semantic_tree.args[0].value, 'cool')

    def test_two_conditions(self):
        input = '[13] = 2009-09-09, [14] = 99'
        semantic_tree = parse(input)
        self.assertIsInstance(semantic_tree, Combinator)
        self.assertEqual(semantic_tree.operator, 'AND')
        self.assertEqual(len(semantic_tree.args), 2)
        self.assertIsInstance(semantic_tree.args[0], Condition)
        self.assertIsInstance(semantic_tree.args[1], Condition)

    def test_three_conditions(self):
        input = '[13] = 2009-09-09, [14] = 99, [15] = alice@'
        semantic_tree = parse(input)
        self.assertIsInstance(semantic_tree, Combinator)
        self.assertEqual(semantic_tree.operator, 'AND')
        self.assertEqual(len(semantic_tree.args), 3)
        self.assertIsInstance(semantic_tree.args[0], Condition)
        self.assertIsInstance(semantic_tree.args[1], Condition)
        self.assertIsInstance(semantic_tree.args[2], Condition)

    def test_all_kind_of_values(self):
        input = """[13] in (1, -2.0, 'cool', '', alice@, WIKI-3000,
            Yes, ON, tRUe, done, checked, no, off, false, unchecked,
            emPTy, NULL)"""
        semantic_tree = parse(input)
        self.assertEqual(semantic_tree.args[0].type, 'NUMBER')
        self.assertEqual(semantic_tree.args[1].type, 'NUMBER')
        self.assertEqual(semantic_tree.args[2].type, 'STRING')
        self.assertEqual(semantic_tree.args[3].type, 'STRING')
        self.assertEqual(semantic_tree.args[4].type, 'USER')
        self.assertEqual(semantic_tree.args[5].type, 'TICKET')
        self.assertEqual(semantic_tree.args[6].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[7].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[8].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[9].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[10].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[11].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[12].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[13].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[14].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[15].type, 'NONE')
        self.assertEqual(semantic_tree.args[16].type, 'NONE')

    def test_parser_errors(self):
        input = "[13] 'hai'"
        with self.assertRaises(ParserError):
            parse(input)

    def test_equal(self):
        input = '[13] = YES'
        semantic_tree = parse(input)
        self.assertIsInstance(semantic_tree, Condition)
        self.assertEqual(semantic_tree.column, '13')
        self.assertEqual(semantic_tree.operator, 'EQUAL')
        self.assertEqual(len(semantic_tree.args), 1)
        self.assertIsInstance(semantic_tree.args[0], Value)
        self.assertFalse(semantic_tree.negated)

    def test_not_equal(self):
        input = '[13] != YES'
        semantic_tree = parse(input)
        self.assertIsInstance(semantic_tree, Condition)
        self.assertEqual(semantic_tree.column, '13')
        self.assertEqual(semantic_tree.operator, 'EQUAL')
        self.assertEqual(len(semantic_tree.args), 1)
        self.assertIsInstance(semantic_tree.args[0], Value)
        self.assertTrue(semantic_tree.negated)

    def test_no_smoke_with_operators_and_combinators(self):
        inputs = [
            '[13] = 100',
            '[14] != 100',
            '[15] ~ 100',
            '[16] !~ 100',
            '[17] > 100',
            '[18] < 100',
            '[19] >= 100',
            '[20] <= 100',
            '[21] is 100',
            '[22] is not 100',
            '[23] in (100)',
            '[24] not in (100)',
            '[25] between 100 and 200',
            '[26] in ()',
            '[27] in (100, 200, 300, 400, 500)',
            'not [28] = 100',
            'not [29] not in (100)',  # This is LACEDAEMON!!1
            '[30] = 100 and [31] = 100',
            '[32] = 100 or [33] = 100',
            '[34] = 100 and ([35] = 100 or [36] = 100)',
        ]
        for input in inputs:
            try:
                parse(input)
            except ParserError as error:
                self.fail('Problem with {0!r}: {1}'.format(input, error))

    def test_non_numeric_columns_are_fine_too(self):
        input = '[number] = 8'
        semantic_tree = parse(input)
        self.assertIsInstance(semantic_tree, Condition)
        self.assertEqual(semantic_tree.column, 'number')

    def test_single_words_may_go_without_quotation(self):
        input = '[0] = banana'
        semantic_tree = parse(input)
        self.assertEqual(semantic_tree.args[0].value, 'banana')
        self.assertEqual(semantic_tree.args[0].type, 'STRING')

    def test_multiple_words_without_quotation_is_error(self):
        input = '[0] = banana banana banana'
        with self.assertRaises(ParserError):
            parse(input)

    def test_tree_is_stored_in_cache(self):
        input = "[13] = 'cool'"
        semantic_tree = parse(input)  # Should trigger caching
        cache = caches['grid_filter']
        pickled_tree_x = cache.get(make_cache_key(input))
        semantic_tree_x = pickle.loads(pickled_tree_x)
        self.assertEqual(semantic_tree_x, semantic_tree)

    def test_tree_is_taken_from_cache(self):
        input = 'absolutely invalid input'
        semantic_tree = Condition('13', 'EQUAL', [Value('STRING', 'cool')], False)
        cache = caches['grid_filter']
        cache.set(make_cache_key(input), pickle.dumps(semantic_tree))
        semantic_tree_x = parse(input)  # Should take from cache
        self.assertEqual(semantic_tree_x, semantic_tree)

    def test_the_is_not(self):
        input = '[2] is not checked'
        semantic_tree = parse(input)
        self.assertEqual(semantic_tree.operator, 'EQUAL')
        self.assertEqual(semantic_tree.args[0].type, 'BOOLEAN')
        self.assertEqual(semantic_tree.args[0].value, True)
        self.assertTrue(semantic_tree.negated)
