# coding: utf-8
__author__ = 'chihiro'

import datetime
import os

import pytest
from hamcrest import contains_string

import btestlib.reporter as reporter
from btestlib import utils as butils
from check import utils
from check.defaults import DATA_DIR
from check.utils import create_data_file_in_s3

check_list = ['oaaw2']


def create_data_in_ado(data):
    lines = []
    columns = ['order_nmb', 'shows_accepted', 'shows_realized', 'budget_plan', 'budget_realized',
               'date_begin', 'date_end', 'product_type_nmb']
    for order in data.values():
        lines.append('\t'.join([str(order[column]) for column in columns]))

    lines = '\n'.join(lines)
    create_data_file_in_s3(
        content=lines,
        file_name='ado_ado_awaps_2.csv',
        db_key='oaaw2_awaps_importer_url',
    )
    reporter.log(lines)


def test_CHECK_2268_parsing_errors():
    date = datetime.datetime.now()
    data = {}
    one_order = {
        'order_nmb': 1,
        'shows_accepted': 50,
        'shows_realized': 'a50',
        'budget_plan': 10,
        'budget_realized': 10,
        'date_begin': date.strftime('%d.%m.%Y'),
        'date_end': date.strftime('%d.%m.%Y'),
        'product_type_nmb': 1}
    _objects = []
    for i in range(1, 12):
        one_order['order_nmb'] = i
        data[i] = one_order
        _objects.append(str(i))
    create_data_in_ado(data)
    objects = ','.join(_objects)
    # Run checks
    with pytest.raises(Exception) as exceptions:
        utils.run_check_new('oaaw2', str(objects), {'import-pool-size': '1'})
    # TODO: Расскоментировать, когда сверки научатся отдавать ошибку
    # TODO: Раньше умели, но сейчас со сменой типа запуска разучились
    # butils.check_that(exceptions.value.faultString, contains_string(u'Too many parsing errors'),
    #                   u'Проверяем, что запуск произошел с ошибкой')


def test_CHECK_2268_bad_file():
    create_data_file_in_s3(
        content='Bad file\nfrom\nado\n!?',
        file_name='ado_ado_awaps_2.csv',
        db_key='oaaw2_awaps_importer_url',
    )
    # Run checks
    with pytest.raises(Exception) as exceptions:
        utils.run_check_new('oaaw2', str('1'), {'import-pool-size': '1'})
    # TODO: Расскоментировать, когда сверки научатся отдавать ошибку
    # TODO: Раньше умели, но сейчас со сменой типа запуска разучились
    # butils.check_that(exceptions.value.faultString, contains_string(u'Bad file from oaaw2 ado'),
    #                   u'Проверяем, что запуск произошел с ошибкой')


# TODO: Сейчас такое почти не реально проверить
# def test_CHECK_2268_file_not_found():
#     os.remove(os.path.join(DATA_DIR, 'ado_ado_awaps_2.csv'))
#     # Run checks
#     with pytest.raises(Exception) as exceptions:
#         utils.run_check_new('oaaw2', str('1'), {'import-pool-size': '1'})
#     butils.check_that(exceptions.value.faultString, contains_string(u'HTTP Error 404: Not Found'),
#                       u'Проверяем, что запуск произошел с ошибкой')
