import unittest
from unittest.mock import patch, Mock

from dwh.grocery.yt_export import MonoClusterYTExport
from dwh.grocery.targets import YTTableTarget, YTMapNodeTarget


class TestYtExport(unittest.TestCase):

    @patch('dwh.grocery.yt_export.MonoClusterYTExport.get_outputs_roots')
    @patch('dwh.grocery.yt_export.MonoClusterYTExport.get_meta')
    def test_fill_row_count_empty(self, get_meta_mock, get_outputs_roots_mock):
        """
            Проверяем заполнение атрибута map_node_row_count, если он пустой
        """

        get_meta_mock.return_value = {"table_v2": {"type": "monthly"}}
        root_mock = Mock(YTMapNodeTarget)
        root_mock.path = "path/to/node/"

        # мокаем отсутствие атрибута map_node_row_count
        root_mock.get_attr = Mock(return_value=None)
        get_outputs_roots_mock.return_value = {"table_v2": root_mock}
        task = MonoClusterYTExport()

        mock_targets = [
            Mock(YTTableTarget, get_attr=Mock(return_value=123_456), leaf="2021-11"),
            Mock(YTTableTarget, get_attr=Mock(return_value=500_000), leaf="2021-12"),
            Mock(YTTableTarget, get_attr=Mock(return_value=1), leaf="2022-01")
        ]
        output = {
            "table_v2": mock_targets
        }
        task.fill_row_count(output)
        root_mock.assert_has_calls([
            unittest.mock.call.set_attr("map_node_row_count", {
                '2021-11': 123456,
                '2021-12': 500000,
                '2022-01': 1
            })])

    @patch('dwh.grocery.yt_export.MonoClusterYTExport.get_outputs_roots')
    @patch('dwh.grocery.yt_export.MonoClusterYTExport.get_meta')
    def test_fill_row_count_non_empty(self, get_meta_mock, get_outputs_roots_mock):
        """
            Проверяем заполнение атрибута map_node_row_count, если он не пустой
        """

        get_meta_mock.return_value = {"table_v2": {"type": "monthly"}}
        root_mock = Mock(YTMapNodeTarget)
        root_mock.path = "path/to/node/"

        # мокаем существующий атрибут map_node_row_count
        map_node_row_count = {
            '2021-09': 11111,
            '2021-10': 22222,
            '2021-11': 8800
        }
        root_mock.get_attr = Mock(return_value=map_node_row_count)
        get_outputs_roots_mock.return_value = {"table_v2": root_mock}
        task = MonoClusterYTExport()

        mock_targets = [
            Mock(YTTableTarget, get_attr=Mock(return_value=123_456), leaf="2021-11"),
            Mock(YTTableTarget, get_attr=Mock(return_value=500_000), leaf="2021-12"),
            Mock(YTTableTarget, get_attr=Mock(return_value=1), leaf="2022-01")
        ]
        output = {
            "table_v2": mock_targets
        }
        task.fill_row_count(output)

        # Один месяц перезаписывается, два добавляются
        root_mock.assert_has_calls([
            unittest.mock.call.set_attr("map_node_row_count", {
                '2021-09': 11111,
                '2021-10': 22222,
                '2021-11': 123456,
                '2021-12': 500000,
                '2022-01': 1
            })])
