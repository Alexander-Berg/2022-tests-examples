import datetime
import mock

from dateutil import parser

from yt.wrapper import ypath_join

import maps.analyzer.pylibs.datasets.datasets as test_module


@mock.patch('test_datasets.test_module._get_now', return_value=datetime.datetime(2021, 9, 21, 13, 46))
def test_dataset_version(_m):
    ytc = mock.Mock()
    ytc.get = mock.Mock(side_effect=['20210917', '20210920'])
    exp_ans = '2021.09.21.13-20210917-20210920'
    assert test_module.dataset_version(ytc, '//table_path') == exp_ans


@mock.patch('test_datasets.test_module._get_now', return_value=datetime.datetime(2021, 9, 21, 13, 46))
def test_generate_new_version(_m):
    exp_ans = '2021.09.21.13-20210917-20210920'
    assert test_module.generate_new_version(parser.parse('20210917'), parser.parse('20210920')) == exp_ans


def test_set_dates_attributes():
    ytc = mock.Mock()
    ytc.set = mock.Mock()
    table_path = '//table_path'
    begin_day = parser.parse('2021-09-17')
    end_day = parser.parse('2021-09-20')
    test_module.set_dates_attributes(ytc, table_path, begin_day, end_day, test_module.DATE_FMT)
    ytc.set.assert_has_calls([
        mock.call(ypath_join(table_path, test_module.MIN_DATE_ATTRIBUTE), begin_day.strftime(test_module.DATE_FMT)),
        mock.call(ypath_join(table_path, test_module.MAX_DATE_ATTRIBUTE), end_day.strftime(test_module.DATE_FMT)),
    ])


def test_min_max_dates_attributes():
    ytc = mock.Mock()
    ytc.get = mock.Mock()
    table_path = '//table_path'
    test_module.min_max_dates_attributes(ytc, table_path)
    ytc.get.assert_has_calls([
        mock.call(ypath_join(table_path, test_module.MIN_DATE_ATTRIBUTE)),
        mock.call(ypath_join(table_path, test_module.MAX_DATE_ATTRIBUTE)),
    ])
