from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class TestErrorsLogic(BaseApiTestCase):
    def test_strings_to_list(self):
        from wiki.utils.logic import strings_to_list

        self.assertEqual(strings_to_list(''), [''])
        self.assertEqual(strings_to_list(['']), [''])
        obj = object()
        self.assertEqual(strings_to_list(obj), obj)
