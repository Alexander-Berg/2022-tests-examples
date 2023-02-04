
from rest_framework import serializers

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class TestErrorsLogic(BaseApiTestCase):
    def test_non_field_error_from_rest_framework(self):
        from wiki.api_core.logic.errors import pop_non_field_rf_errors

        class SomeSerializer(serializers.Serializer):
            def validate(self, attrs):
                raise serializers.ValidationError('some error which is not bound to field')

        result = SomeSerializer(data={})
        result.is_valid()
        errors = result.errors
        self.assertEqual({'non_field_errors': ['some error which is not bound to field']}, errors)
        self.assertEqual(['some error which is not bound to field'], pop_non_field_rf_errors(errors))
        # теперь словарь модифицирован
        self.assertDictEqual({}, errors)

    def test_complex_structures(self):
        from wiki.api_core.logic.errors import pop_non_field_rf_errors

        errors = {'some_complex_field': {'non_field_errors': ['error!'], 'field_name': ['field error']}}
        self.assertEqual(['some_complex_field: error!'], pop_non_field_rf_errors(errors))
        self.assertEqual({'some_complex_field': {'field_name': ['field error']}}, errors)

    def test_context_is_passed(self):
        from wiki.api_core.logic.errors import pop_non_field_rf_errors

        errors = {'non_field_errors': ['message', 'message2']}
        self.assertEqual(pop_non_field_rf_errors(errors, context_key='key'), ['key: message', 'key: message2'])
        self.assertDictEqual({}, errors)
