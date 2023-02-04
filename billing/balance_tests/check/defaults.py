# coding: utf-8
# todo-bad_practice не было поддержки utf-8 в файле
# todo-bad_practice PEP8 =(
import datetime
import decimal
import os

from btestlib import secrets

DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'test_data')
HTTP_SERVER_PORT = 8666

CLIENT_LOGIN = 'chihiro-test-0'
LAST_DAY_OF_PREVIOUS_MONTH = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)

STARTREK_PARAMS = {
    'useragent': 'yandex-balance-dcs',
    'base_url': 'https://st-api.test.yandex-team.ru',
    # тестовый робот testuser-balance1
    'token': secrets.get_secret(*secrets.Tokens.STARTREK_CHECK_TOKEN)
}


# todo-architect данные о сервисах и продуктах не специфичны для сверок. Перенести в баланс
class Services(object):
    # todo-bad_practice константы называем большими буквами
    direct = 7
    client = 8
    vertical = 81
    market = 11
    geo = 37
    ado = 70
    bk = 67
    service_ag = 42
    autoru = 99
    ticket = 114
    health = 153
    adfox = 102


class Products(object):
    # todo-bad_practice константы называем большими буквами
    direct_pcs = 1475
    direct_money = 503162
    vertical = 503937
    market = 2136
    geo = 502918
    ado = 504033
    bk = 2584
    service_ag = 507130
    ticket = 502981
    autoru = 505206


# todo-architect значения paysys_id, person_category, firm_id лучше бы вынести в константы
data = {
    'aob_market': {'service_id': Services.market, 'product_id': Products.market, 'paysys_id': 1003,
                   'person_category': 'ur', 'person_additional_params': None, 'firm_id': 111},
    'aob_tr': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1050,
               'person_category': 'tru', 'person_additional_params': None},
    'aob_ua': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1017,
               'person_category': 'ua',
               'person_additional_params': None},
    'aob_sw': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1045,
               'person_category': 'sw_ur', 'person_additional_params': None},
    'aob': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1003,
            'person_category': 'ur',
            'firm_id': 1, 'person_additional_params': None},
    'aob_taxi': {'person_additional_params': {'kpp': '234567890'}, 'person_category': 'ur', 'firm_id': 13},
    'aob_us': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1028,
               'person_category': 'usu', 'person_additional_params': None},
    'aob_vertical': {'service_id': Services.vertical, 'product_id': Products.vertical, 'paysys_id': 1201003,
                     'person_category': 'ur', 'person_additional_params': None, 'firm_id': 12},
    'aob_services': {'service_id': Services.service_ag, 'product_id': Products.service_ag, 'paysys_id': 1601047,
                     'person_category': 'sw_yt', 'person_additional_params': None, 'firm_id': 16},
    'iob_services': {'service_id': Services.service_ag, 'product_id': Products.service_ag, 'paysys_id': 1601047,
                     'person_category': 'sw_yt', 'person_additional_params': None, 'firm_id': 16},
    'iob_market': {'service_id': Services.market, 'product_id': Products.market, 'paysys_id': 1003,
                   'person_category': 'ur', 'person_additional_params': None, 'firm_id': 111},
    'iob_tr': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1050,
               'person_category': 'tru', 'person_additional_params': None},
    'iob_ua': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1017,
               'person_category': 'ua',
               'person_additional_params': None},
    'iob_sw': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1045,
               'person_category': 'sw_ur', 'person_additional_params': None},
    'iob': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1033,
            'person_category': 'ur',
            'person_additional_params': None, 'firm_id': 1},
    'iob_taxi': {'person_additional_params': {'kpp': '234567890'}, 'person_category': 'ur', 'firm_id': 13},
    'iob_us': {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1028,
               'person_category': 'usu', 'person_additional_params': None},
    'iob_vertical': {'service_id': Services.vertical, 'product_id': Products.vertical, 'paysys_id': 1201003,
                     'person_category': 'ur', 'person_additional_params': None, 'firm_id': 12},
    'omb': {'service_id': Services.market, 'product_id': Products.market, 'paysys_id': 1003, 'person_category': 'ur',
            'person_additional_params': None},
    'zmb': {'service_id': Services.market, 'product_id': Products.market, 'paysys_id': 1003, 'person_category': 'ur',
            'person_additional_params': None},
    'okmb': {'type': 'okmb', 'service_id': 114, 'product_id': 502981, 'paysys_id': 1003, 'person_category': 'ur',
             'person_additional_params': None},
    'obg': {'type': 'obg', 'service_id': Services.geo, 'product_id': Products.geo, 'paysys_id': 1003,
            'person_category': 'ur', 'person_additional_params': None},
    'zbb': {'type': 'zbb', 'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1003,
            'person_category': 'ur', 'person_additional_params': None},
    'oba': {'type': 'oba', 'service_id': Services.ado, 'product_id': Products.ado, 'paysys_id': 1017,
            'person_category': 'ua', 'person_additional_params': None},
    'obb2': {'type': 'oba', 'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1003,
             'person_category': 'ur', 'person_additional_params': None},
}

MEANINGLESS_DEFAULTS = {
    'service_id': Services.direct, 'product_id': Products.direct_pcs,
    'qty': decimal.Decimal('55.7')
}