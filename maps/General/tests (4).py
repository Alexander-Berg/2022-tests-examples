from maps.qa.geo_devices.common.lib.ut_helper import (
    read_json_file
)
from maps.qa.geo_devices.devices_stat_report.lib.eval_report import (
    parse_yt_data,
    create_tree_view_st_report
)

from yatest.common import source_path
from freezegun import freeze_time
import unittest


def data_path(filename):
    return source_path('maps/qa/geo_devices/devices_stat_report/ut/data/' + filename)


class Tests(unittest.TestCase):
    def test_parse_yt_data(self):
        input_data = read_json_file(data_path('geo_yt_table.json'))
        output = parse_yt_data(input_data)
        correct = read_json_file(data_path('parsed_yt_data_correct.json'))
        self.assertDictEqual(output, correct)

    @freeze_time('2020-09-21T10:55:03.406018')
    def test_create_tree_view_st_report(self):
        input_data = read_json_file(data_path('parsed_yt_data.json'))
        output = create_tree_view_st_report(input_data)
        correct = read_json_file(data_path('created_report_correct.json'))
        self.assertListEqual(output, correct)


if __name__ == '__main__':
    unittest.main()
