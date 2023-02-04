# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import collections
import datetime
import getpass
import pickle

import numpy
import xlwt

from btestlib import utils
from maintenance.test_aggregate_stats import aggregate_stats


def get_all_stat_data():
    for key in utils.s3storage_stats().get_keys():
        if key.name == u'aggregation_Full':
            result = process_stat(pickle.loads(utils.s3storage_stats().get_string_value(key.name)))
    stat_to_xls('all_stat', result)


def get_stat_data_for_period(start_biuld, finish_build):
    result = process_stat(aggregate_stats('Full', start_biuld, finish_build, prefix='stats'))
    stat_to_xls('stat_from_{}_to_{}'.format(start_biuld, finish_build), result)


def process_stat(stat_data):
    test_info = collections.defaultdict(dict)
    for test, test_stat in stat_data.iteritems():
        test_info[test]['raw_data'] = str(test_stat)
        test_info[test]['avg_duration'] = round(test_stat['duration']/test_stat['duration_count'], 2)
        test_info[test]['% of fails'] = round((numpy.float64(test_stat['failed'])/(test_stat['failed'] + test_stat['passed']))*100, 2) if (test_stat['failed'] + test_stat['passed'] <>0) else 0
        test_info[test]['common_avg_delay_time'] =  round(sum(numpy.float64(test_stat['runtime'].values()))/test_stat['runtime_count'], 2)  if 'runtime_count' in test_stat.keys() else 0
        test_info[test]['lock'] = round(numpy.float64(test_stat['runtime']['lock']) / test_stat['runtime_count'], 2) if 'runtime_count' in test_stat.keys() else 0
        test_info[test]['wait_for_export_from_bs'] = round(numpy.float64(test_stat['runtime']['wait_for_export_from_bs']) / test_stat['runtime_count'], 2) if 'runtime_count' in test_stat.keys() else 0
        test_info[test]['wait_until'] = round(numpy.float64(test_stat['runtime']['wait_until']) / test_stat['runtime_count'], 2) if 'runtime_count' in test_stat.keys() else 0
    return sorted(test_info.items(), key=lambda x: x[1]['avg_duration'], reverse=True)


def stat_to_xls(name, result):
    wb = xlwt.Workbook()
    ws = wb.add_sheet("Stat_{}".format(str(datetime.datetime.now().strftime("%Y-%m-%d"))))
    for key, value in enumerate(result[0][1]):
        ws.write(0, key + 2, value)
    row = 0
    for index, test in enumerate(result):
        test_path = test[0].replace('::', '@').split('@')
        for tp in test_path:
            ws.write(row + 1, test_path.index(tp), str(tp))
        for ind, header in enumerate(test[1]):
            ws.write(row + 1, ind + 2, result[row][1][header])
        row += 1
    wb.save("C:/Users/{login}/{name}.xls".format(login=getpass.getuser(), name=name))

# get_all_stat_data()   ## вся статистика
# get_stat_data_for_period(1815, 1830)   ## статистика за период(указываются нужные сборки)
