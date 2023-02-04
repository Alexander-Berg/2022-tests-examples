from unittest import TestCase

from yaphone.advisor.common.tools import rewrite_params_in_url, remove_bom


class AddParamsToUrlTest(TestCase):
    def setUp(self):
        self.url = 'http://example.com/path/?id=1&page=1'

    def test_update_existing_param(self):
        self.assertEqual(rewrite_params_in_url(self.url, {'page': 2}),
                         'http://example.com/path/?id=1&page=2')

    def test_add_new_param(self):
        self.assertEqual(rewrite_params_in_url(self.url, {'foo': 'bar'}),
                         'http://example.com/path/?id=1&page=1&foo=bar')


class ToolsRemoveBOMTest(TestCase):
    def setUp(self):
        self.expected_result = '<!DOCTYPE...'

    def test_on_normal_data(self):
        self.assertTrue(self.expected_result == remove_bom('<!DOCTYPE...'))

    def test_on_malformed_data(self):
        self.assertTrue(self.expected_result == remove_bom('\x0d\x0a\xef\xbb\xbf<!DOCTYPE...'))

    def test_on_malformed_data_2(self):
        self.assertTrue(self.expected_result == remove_bom('\xef\xbb\xbf<!DOCTYPE...'))
