import unittest
from unittest import mock
from importlib import import_module

from luigi import date_interval as di

dwh724 = import_module("dwh.grocery.dwh-724")


class TestDWH724(unittest.TestCase):

    @mock.patch('dwh.grocery.targets.YTMapNodeTarget.set_attr')
    @mock.patch('dwh.grocery.targets.YTTableTarget.get_attr')
    @mock.patch('dwh.grocery.targets.YTMapNodeTarget.get_attr')
    def test_fill_row_count(self, mapnode_get_attr_mock, table_get_attr_mock, mapnode_set_attr_mock):
        """
            Проверяем заполнение атрибута map_node_row_count для мапнод tariffied_acts*
        """
        mapnode_get_attr_mock.side_effect = [
            {"2021-08": 10, "2021-09": 15},
            {},
            {"2021-12": 50},
            {"2021-12": 40, "2022-01": 80}
        ]
        table_get_attr_mock.side_effect = [1, 10, 100, 1000]
        task = dwh724.DWH724(start_month=di.Month.parse("2021-12"),
                             end_month=di.Month.parse("2021-12"))
        task.run()
        mapnode_set_attr_mock.assert_has_calls(
            [mock.call('map_node_row_count', {'2021-08': 10, '2021-09': 15, '2021-12': 1}),
             mock.call('map_node_row_count', {'2021-12': 10}),
             mock.call('map_node_row_count', {'2021-12': 100}),
             mock.call('map_node_row_count', {'2021-12': 1000, "2022-01": 80})])
