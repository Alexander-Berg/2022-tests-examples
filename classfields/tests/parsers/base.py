from unittest import IsolatedAsyncioTestCase
from typing import List

from app.parsers.parsed_value import Expected, ParsedValue


class TestParser(IsolatedAsyncioTestCase):
    def assert_parsed_value(self, expected: ParsedValue, actual: ParsedValue):
        self.assertIsInstance(actual, expected.__class__)
        self.assertEqual(expected.value, actual.value)

    def assert_parsed_phones(self, phones: List[str], actual: ParsedValue):
        self.assertIsInstance(actual, Expected)
        expected_phones = [Expected(phone) for phone in phones]
        for i, parsed_phone in enumerate(actual.value):
            self.assert_parsed_value(expected_phones[i], parsed_phone)
