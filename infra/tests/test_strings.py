import unittest

from infra.cores.app import strings


class TestStrings(unittest.TestCase):
    def test_split_string_into_tokens(self):
        self.assertEqual(
            strings.parse_tokens(''),
            [],
        )

        self.assertEqual(
            strings.parse_tokens(None),
            [],
        )

        raw_input_first = '''
            ride:whisky-hil/2021-05-28/16:10:46/16:10:46,
            ride:whisky-hil/2021-05-28/16:20:49/16:20:49
        '''
        self.assertEqual(
            strings.parse_tokens(raw_input_first),
            sorted([
                'ride:whisky-hil/2021-05-28/16:10:46/16:10:46',
                'ride:whisky-hil/2021-05-28/16:20:49/16:20:49',
            ]),
        )

        self.assertEqual(
            strings.parse_tokens('a'),
            ['a'],
        )

        self.assertEqual(
            strings.parse_tokens('a,b,c'),
            ['a', 'b', 'c']
        )

    def test_merge_tokens(self):
        self.assertEqual(
            strings.merge_tokens('abc,\ndef', '002,001'),
            '001,002,abc,def'
        )
        self.assertEqual(
            strings.merge_tokens('\n', '002,001'),
            '001,002'
        )
