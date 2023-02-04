import datetime
import unittest

import xml.etree.ElementTree as et
from io import StringIO
from unittest.mock import MagicMock
import random

from agency_rewards.rewards.utils.dates import get_last_dt_prev_month
from agency_rewards.rewards.utils.yt import (
    get_yt_upload_path,
    get_yt_upload_cmd,
    YtTablesDiffer,
)

from . import config_sample


class TestYTConfig(unittest.TestCase):
    def setUp(self):
        self.cfg = et.parse(StringIO(config_sample))

    def test_yt_upload_path_dt(self):
        self.assertEqual(
            get_yt_upload_path(self.cfg, dt=datetime.datetime(2019, 2, 1), node_name='LastMonthActsPath'),
            '''//home/balance/test/yb-ar/acts/201901''',
        )

    def test_yt_upload_path_default(self):
        prev_month = get_last_dt_prev_month(datetime.datetime.now())
        self.assertEqual(
            get_yt_upload_path(self.cfg, node_name='LastMonthActsPath'),
            '''//home/balance/test/yb-ar/acts/{}{:02d}'''.format(prev_month.year, prev_month.month),
        )

    def test_celery_queue(self):
        self.assertEqual(get_yt_upload_cmd(self.cfg), '''/usr/bin/dwh/run_with_env.sh''')

    def test_long_formatted_diff(self):
        """
        Проверяет, что в таблицах с большим кол-вом расхождений, выводятся только первые 50
        """
        yt_client = MagicMock()
        yql_client = MagicMock()

        d1 = MagicMock()
        d2 = MagicMock()
        differ = YtTablesDiffer(
            correct_table_path=None,
            result_table_path=None,
            yt_client=yt_client,
            yql_client=yql_client,
            is_need_tci=False,
        )
        column_names = [
            'correct_contract_id',
            'correct_discount_type',
            'correct_amt',
            'correct_reward',
            'calc_contract_id',
            'calc_discount_type',
            'calc_amt',
            'calc_reward',
        ]

        d1.rows = [[random.randint(1, 100) for _ in range(8)] for _ in range(10000)]
        d2.rows = [[random.randint(1, 100) for _ in range(8)] for _ in range(10000)]

        d1.column_names = column_names
        d2.column_names = column_names
        diffs = [d1, d2]
        formatted_diff = differ.get_formatted_diff(diffs)
        num_rows = differ._max_diff_count // (len(column_names) // 2)
        expected_lines_count = 3 + num_rows * (len(column_names) // 2 + 1)
        self.assertEqual(formatted_diff.count('\n'), expected_lines_count, formatted_diff)

    def test_long_f_diff_extra_columns(self):
        """
        Если количество required_columns больше, чем количество сравниваемых колонок в таблицах в diffs,
        должно выводить только имеющиеся колонки
        """
        yt_client = MagicMock()
        yql_client = MagicMock()

        d1 = MagicMock()
        d2 = MagicMock()
        # is_need_tc=True => в differ.required_columns 6 колонок, а в column_names -- 8//2 = 4 колонки
        differ = YtTablesDiffer(
            correct_table_path=None, result_table_path=None, yt_client=yt_client, yql_client=yql_client, is_need_tc=True
        )
        column_names = [
            'correct_contract_id',
            'correct_discount_type',
            'correct_amt',
            'correct_reward',
            'calc_contract_id',
            'calc_discount_type',
            'calc_amt',
            'calc_reward',
        ]

        d1.rows = [[random.randint(1, 100) for _ in range(8)] for _ in range(10000)]
        d2.rows = [[random.randint(1, 100) for _ in range(8)] for _ in range(10000)]

        d1.column_names = column_names
        d2.column_names = column_names
        diffs = [d1, d2]
        formatted_diff = differ.get_formatted_diff(diffs)
        num_rows = differ._max_diff_count // (len(column_names) // 2)
        expected_lines_count = 3 + num_rows * (len(column_names) // 2 + 1)
        self.assertEqual(formatted_diff.count('\n'), expected_lines_count, formatted_diff)
