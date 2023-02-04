# -*- coding: utf-8 -*-

import datetime

import yt.wrapper as yt

from common import dt_convert, write_table


def check_100_pct(data):
    total = 0
    for key, value in data[0].items():
        if key not in ('dt', 'login'):
            total += value
    assert total == 100, "Sum should be equal to 100"

def fill_duty_qa_stat():
    # обязательно передавать параметр append (иначе перетрет теккущие данные)
    dst = yt.TablePath('//home/balance-test/metrics/qa_team_stat', append=True)
    data = [
        {
            'dt': dt_convert(datetime.datetime(2021, 3, 22)),  # первый день дежурной недели
            'login': 'atkaya',
            'testing': 0,
            'assessors_new': 0,
            'assessors_support': 0,
            'autotests_ui': 0,
            'autotests_backend': 0,
            'autotests_support': 0,
            'manual_regression': 0,
            'duty': 0,
            'colleagues_support': 0,
            'infra': 0,
            'docs': 0,
            'goals': 0,
            'other': 0,
        }
    ]
    check_100_pct(data)
    write_table(dst, data)


# fill_duty_qa_stat()
