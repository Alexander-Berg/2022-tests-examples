
from wiki.api_frontend.logic.errors import yield_key_value_string_errors
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class YieldKeyValueErrorsTestCase(BaseApiTestCase):
    def test_it_works(self):
        result = list(
            yield_key_value_string_errors(
                {
                    'changes': {
                        'added_column': {'type': ['This field is required.']},
                        'option': ['Options required', 'Options are very required'],
                    },
                }
            )
        )
        self.assertEqual(
            set(result),
            {
                ('type', 'This field is required.'),
                ('option', 'Options required'),
                ('option', 'Options are very required'),
            },
        )

    def test_it_works_on_lists_of_dicts(self):
        result = list(yield_key_value_string_errors({'changes': [{'type': ['This field is required.']}]}))
        self.assertEqual(
            result,
            [
                ('type', 'This field is required.'),
            ],
        )

    def test_it_works_on_lists(self):
        result = list(
            yield_key_value_string_errors(
                {
                    'option': ['Options required', 'Options are very required'],
                }
            )
        )
        self.assertEqual(set(result), {('option', 'Options required'), ('option', 'Options are very required')})
