# coding: utf-8
__author__ = 'chihiro'

import datetime

import pytest
from hamcrest import contains_string

import btestlib.reporter as reporter
from btestlib import utils as butils
from check import utils
from check.utils import create_data_file_in_s3


def create_data_in_ado(data):
    orders = ''
    for key in data.values():
        one_order = str(key['service_order_id']) + '	' + str(key['volume_accepted']) + '	' + \
                    str(key['billing_realized']) + '	' + str(key['billing_realized']) + '	' + \
                    str(key['billing_realized']) + '	' + str(key['date_begin']) + '	' + \
                    str(key['date_end']) + '	1	0\n'
        orders += one_order

    create_data_file_in_s3(
        content=orders,
        file_name='ado_ado_2.csv',
        db_key='oba2_ad_office_importer_url',
    )
    reporter.log(orders)


def test_CHECK_2268_parsing_errors():
    date = datetime.datetime.now()
    data = {}
    one_order = {'service_order_id': 3,
                 'billing_realized': 'a0',
                 'volume_accepted': 0,
                 'date_begin': date.strftime('%d.%m.%Y'),
                 'date_end': date.strftime('%d.%m.%Y')}
    for i in range(11):
        one_order['service_order_id'] = i
        data[i] = one_order
    create_data_in_ado(data)
    _objects = []
    for key in data.keys():
        if key not in ['diffs', 'ado_data']:
            one = str(data[key]['service_order_id'])
            _objects.append(one)
    objects = ','.join(_objects)
    # Run checks
    with pytest.raises(Exception) as exceptions:
        utils.run_check_new('oba2', str(objects), {'import-pool-size': '1'})
    # TODO: Расскоментировать, когда сверки научатся отдавать ошибку
    # TODO: Раньше умели, но сейчас со сменой типа запуска разучились
    # butils.check_that(exceptions.value.faultString, contains_string(u'Too many parsing errors'),
    #                   u'Проверяем, что запуск произошел с ошибкой')


def test_CHECK_2268_bad_file():
    create_data_file_in_s3(
        content='Bad file\nfrom\nado\n!?',
        file_name='ado_ado_2.csv',
        db_key='oba2_ad_office_importer_url',
    )
    # Run checks
    with pytest.raises(Exception) as exceptions:
        utils.run_check_new('oba2', str('1'), {'import-pool-size': '1'})
    # TODO: Расскоментировать, когда сверки научатся отдавать ошибку
    # TODO: Раньше умели, но сейчас со сменой типа запуска разучились
    # butils.check_that(exceptions.value.faultString, contains_string(u'Bad file'),
    #                   u'Проверяем, что запуск произошел с ошибкой')


# TODO: Сейчас такое почти не реально проверить
# def test_CHECK_2268_file_not_found():
#     with pytest.raises(Exception) as exceptions:
#         utils.run_check_new('oba2', str('1'), {'import-pool-size': '1'})
#     butils.check_that(exceptions.value.faultString, contains_string(u'HTTPError'),
#                       u'Проверяем, что запуск произошел с ошибкой')
#     butils.check_that(exceptions.value.faultString, contains_string(u'Not Found'),
#                       u'Проверяем, что запуск произошел с ошибкой')
