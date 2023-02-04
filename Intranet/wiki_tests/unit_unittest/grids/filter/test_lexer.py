
from wiki.grids.filter.lexer import LexerError, lexer
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class LexerTest(BaseTestCase):
    def test_numbers(self):
        input = '1 0 13 -9 +400 99.999 -101.5'
        lexer.input(input)
        tokens = list(lexer)
        self.assertTrue(all(t.type == 'NUMBER' for t in tokens))

    def test_strings(self):
        input = " 'hai!' 'bye' yandex медвет "
        lexer.input(input)
        tokens = list(lexer)
        self.assertTrue(all(t.type == 'STRING' for t in tokens))

    def test_users(self):
        input = ' fry@ leela@ zapp-brannigan@ kif_kroker@ '
        lexer.input(input)
        tokens = list(lexer)
        self.assertTrue(all(t.type == 'USER' for t in tokens))

    def test_dates(self):
        input = ' 2010-10-10 1900-01-01 '
        lexer.input(input)
        tokens = list(lexer)
        self.assertTrue(all(t.type == 'DATE' for t in tokens))

    def test_tickets(self):
        input = ' WIKI-1 SSB-100 '
        lexer.input(input)
        tokens = list(lexer)
        self.assertTrue(all(t.type == 'TICKET' for t in tokens))

    def test_column_identifiers(self):
        input = ' [0] [55] [name] [i_love_mice] [1o1] [чихуахуа] '
        lexer.input(input)
        tokens = list(lexer)
        self.assertTrue(all(t.type == 'COLUMN' for t in tokens))

    def test_other_stuff(self):
        input = """ = == != ~ !~ > < >= <= not empty null yes no and or ,
            between ( ) on true done checked off false """
        lexer.input(input)
        list(lexer)  # No error

    def test_lexer_errors(self):
        input = ' ?? '
        lexer.input(input)
        with self.assertRaises(LexerError):
            list(lexer)
