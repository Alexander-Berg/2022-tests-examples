import unittest
from unittest import mock

from dwh.grocery.tools.bunker import get_from_bunker


class TestBunkerCache(unittest.TestCase):
    @mock.patch('dwh.grocery.tools.bunker.request_to_bunker', return_value=("true_data", "true_schema", 200))
    def test_true_request_first(self, request_mock):
        """
            Проверяет, что сначала пытаемся достать информацию из настоящего Бункера
        """
        data, schema = get_from_bunker("node/node", "stable")
        self.assertEqual("true_data", data)
        self.assertEqual("true_schema", schema)

    @mock.patch('dwh.grocery.tools.bunker.get_from_cache', return_value=("cached_data", "cached_schema"))
    @mock.patch('dwh.grocery.tools.bunker.request_to_bunker', return_value=(None, None, 500))
    def test_cached_request_if_bunker_none(self, request_mock, cache_mock):
        """
            Проверяет, что если Бункер недоступен, пытаемся достать информацию из кэша
        """
        data, schema = get_from_bunker("node/node", "stable")
        self.assertEqual("cached_data", data)
        self.assertEqual("cached_schema", schema)

    @mock.patch('dwh.grocery.tools.bunker.get_from_cache', return_value=None)
    @mock.patch('dwh.grocery.tools.bunker.request_to_bunker', return_value=(None, None, 500))
    def test_error_if_both_none(self, request_mock, cache_mock):
        """
            Проверяет, что если и Бункер, и кэш вернули None, поднимаем ошибку
        """
        with self.assertRaises(Exception) as context:
            get_from_bunker("node/node", "stable")
            self.assertIn("Something wrong with bunker", str(context.exception))

    @mock.patch('dwh.grocery.tools.bunker.request_to_bunker', return_value=(None, None, 404))
    def test_404(self, request_mock):
        """
            Если Бункер вернул 404, получаем None
        """
        data = get_from_bunker("node/node", "stable")
        self.assertIsNone(data)
