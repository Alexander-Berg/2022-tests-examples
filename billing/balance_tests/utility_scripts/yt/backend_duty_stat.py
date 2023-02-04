# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import argparse
import json

import yt.wrapper as yt

from btestlib import reporter
from common import write_table, check_100_pct, \
    str_to_dt_and_back_to_str, check_data_for_date


def fill_duty_backend_stat(data):
    path = '//home/balance-test/metrics/backend_duty_stat'

    # разбираем json полученный из TC, приводим дату к нужному формату
    data = [json.loads(data)]
    reporter.log(data)
    data[0]['dt'] = str_to_dt_and_back_to_str(data[0]['dt'])

    # проверяем, есть ли уже стата за этот день
    check_data_for_date(path, data[0]['dt'])

    # проверяем, что сумма по процетам = 100
    check_100_pct(data)

    # записываем данные в yt
    # обязательно передавать параметр append (иначе перетрет теккущие данные)
    dst = yt.TablePath(path, append=True)
    write_table(dst, data)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--value_json', help='duty stat data')
    args = parser.parse_args()
    value_json = args.value_json

    fill_duty_backend_stat(value_json)
