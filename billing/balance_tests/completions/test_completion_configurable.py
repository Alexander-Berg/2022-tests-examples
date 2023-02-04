# -*- coding: utf-8 -*-
from StringIO import StringIO
from collections import defaultdict
from datetime import datetime
from decimal import Decimal
from yt.wrapper.mappings import FrozenDict

import copy
import hamcrest as hm
import json
import mock
import sqlalchemy as sa

from balance import mapper, scheme, muzzle_util as ut
from balance.completions_fetcher.configurable_partner_completion import BaseFetcher, CompletionConfig, ProcessorFactory
from balance.mapper import CompletionSource
from balance.payments import scheme_payments
from tests.base import BalanceTest


DT = datetime(2019, 1, 20)

map_dt = {
    'zen': datetime(1999, 1, 1),
    'toloka': datetime(1999, 1, 1),
    'boyscouts': datetime(1999, 1, 1, 19, 47, 51),
    'taxi_stand_svo': datetime(2018, 7, 25, 6, 54, 23),
    'mediaservice_subagency_payment': datetime(2020, 4, 9, 0, 0, 0),
    'drive_fueling': datetime(2019, 3, 20, 17, 0, 0),
    'drive_penalty': datetime(2019, 3, 21, 12, 1, 15),
    'travel': datetime(2018, 10, 12, 1, 13, 17),
    'practicum_spendable': datetime(2021, 3, 17, 10, 26, 51, 5463),
    'zaxi_selfemployed_spendable': datetime(2021, 3, 17, 10, 26, 51, 5463),
    'food_payment': datetime(1990, 1, 31, 10, 0),  # Просто дата открытия первого ресторана McDonalds в СССР
    'drive_corp': datetime(2020, 7, 31, 0, 0),
    'cloud_referal_payment': datetime(2019, 3, 20, 17, 0, 0),
    'news_payment': datetime(2021, 7, 19, 12, 0, 0),
    'disk_b2b': datetime(2021, 7, 31, 0, 0),
}

SKIP = {
    # Пока не покрыто тестом:
    'avia_chain',
    # Покрыто другим тестом (файл test_partner_completions_configurable)
    'addappter2', 'taxi_aggr_tlog', 'taxi_subvention',
    'test_missing_deletion_filter_completion',
    'taxi_aggr', 'taxi_medium', 'blue_market_aggr_tlog', 'dzen_ip_payment', 'k50',
    'market_subvention', 'blue_marketing_services_aggr_tlog',
    # Сервис удален, но заборщик ещё нет: todo: удалить
    'connect'
}

source_conditions = {
    'distr_pages': {
        'params': {
            'places': range(2000, 2007)
        },
        'sql': '(place_id >= 2000 and place_id < 2007)',
    },
    'food_srv': {
        'sql': 'transaction_dt = :dt',
    },
    'oebs_compls': {
        'sql': 'transaction_dt = :dt',
    },
}


def create_contract_context(session, service_code, contract_idx, **kw):
    # заводим строчки договоров, т.к. констрейт на contract_id.
    contract_ids = set(r[contract_idx] for r in expected[service_code])
    contracts_exists = session.execute(
        sa.select([scheme.contracts2.c.id], scheme.contracts2.c.id.in_(contract_ids))
    ).fetchall()
    contract_ids -= {c.id for c in contracts_exists}
    for contract_id in contract_ids:
        values = {scheme.contracts2.c.id: contract_id,
                  scheme.contracts2.c.type: 'GENERAL'}
        session.execute(scheme.contracts2.insert(values))


prepare_context = {
    'adfox': {'action': create_contract_context,
              'params': lambda session, service_code: dict(session=session, service_code=service_code, contract_idx=1)},
    'multiship': {'action': create_contract_context,
                  'params': lambda session, service_code: dict(session=session,
                                                               service_code=service_code, contract_idx=2)},
}


class Data:
    def __init__(self, data, ignore_fields=tuple(), convert_to_strio=True):
        from copy import deepcopy
        self.data = deepcopy(data)
        self.ignore_fields = ignore_fields
        self.convert_to_strio = convert_to_strio

    def to_dict(self, keys):
        return [tuple_to_dict(row, keys, self.ignore_fields) for row in self.data]

    def __repr__(self):
        return str(self.data)


class SourceData(object):
    def __init__(self, data):
        self.data = data
        self.convert_to_strio = False  # пока так для совместимости с Data, допилить, если понадобится


class PreparedData(object):
    def __init__(self, data, ignore_fields=tuple()):
        from copy import deepcopy
        self.data = deepcopy(data)
        self.ignore_fields = ignore_fields
        self.prepared_data = self.prepare_data_for_comparison()
        self.convert_to_strio = False

    def prepare_data_for_comparison(self):
        result = defaultdict(int)
        for row in self.data:
            prepared = FrozenDict(
                {key: value for (key, value) in dict(row).iteritems() if key not in self.ignore_fields}
            )
            result[prepared] += 1
        return result

    def __eq__(self, other):
        assert isinstance(other, PreparedData)
        return self.prepared_data == other.prepared_data

    def __repr__(self):
        return str(self.prepared_data)


fake_source = {}

fake_source['drive'] = Data([
    {"product_id": 666, "amount": "666.66", "type": "carsharing"},
    {"product_id": 666, "amount": "777.77", "type": "toll_road", "payment_type": None},
    {"product_id": 666, "amount": "333.33", "type": "carsharing", "payment_type": "yandex_account_topup"},
    {"product_id": 666, "amount": "444.44", "type": "carsharing", "payment_type": "yandex_account_withdraw"},
], convert_to_strio=False)

fake_source['drive_plus'] = fake_source['drive']

fake_source["distr_pages"] = '\n'.join(str(i) for i in source_conditions['distr_pages']['params']['places'])

fake_source['activations'] = (
    """{"data":[
        {"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "10", "activations": "472"},
        {"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "11", "activations": "55"},
        {"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "15", "activations": "20"},
        {"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "3", "activations": "8"},
        {"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "5", "activations": "1"},
        {"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "8", "activations": "3"},
        {"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "9", "activations": "29"},
        {"dt": "2019-01-20", "page_id": 158340, "clid": "2257166", "currency_id": 2, "vid": "-1", "activations": "19"},
        {"dt": "2019-01-20", "page_id": 158484, "clid": "2257310", "currency_id": 2, "vid": "-1", "activations": "3"},
        {"dt": "2019-01-20", "page_id": 158516, "clid": "2257342", "currency_id": 2, "vid": "-1", "activations": "451"}
    ],"result":"ok"}"""
)

fake_source['addapter_dev_com'] = (
    """2019-01-20;2106502;2106550;534.00;654.00;6
2019-01-20;777888999;127774;6.00;4.00;2
2019-01-20;87452;123;54.21;56.50;1
2019-01-20;2276700;123;32.04;43.55;1
2019-01-20;2075745;123;45.00;65.90;1
2019-01-20;25421;123;21.21;33.33;1
2019-01-20;8754;123;24.51;25.41;1
2019-01-20;87565442;123;22.22;33.33;1
2019-01-20;521;123;22.22;33.33;1
2019-01-20;4874;123;54.21;66.66;1"""
)

fake_source['addapter_ret_ds'] = fake_source['addapter_dev_com']

fake_source['addapter_ret_com'] = fake_source['addapter_dev_com']

fake_source['addapter_dev_ds'] = fake_source['addapter_dev_com']

fake_source['adfox'] = (
    '''"2019-01-20",216135,505170,59665733,46601691,0,0,""
"2019-01-20",216135,505173,1084378,4084,0,1,""
"2019-01-20",216135,507217,100557,100557,0,1,""
"2019-01-20",239925,505170,450,0,0,1,""
"2019-01-20",239959,505170,9522,735,0,1,""
"2019-01-20",239970,505170,44438,21502,0,1,""
"2019-01-20",240126,505170,80885,80885,0,1,""
"2019-01-20",267148,505170,270949,228848,0,1,""
"2019-01-20",267747,505170,120604,10672,0,1,""
"2019-01-20",267747,505178,1,1,0,1,""'''
)

fake_source['api_market'] = (
    """139092;;2019-01-20;577461;0;0;0;0
151198;;2019-01-20;67293;66;5.45;545;205
2294597;;2019-01-20;15;0;0;0;2
141487;;2019-01-20;873;1;1.52;152;5
2289306;;2019-01-20;5779;0;0;0;195
152593;;2019-01-20;182;0;0;0;2
152543;;2019-01-20;2135554;36;12.21;1221;185
219512;;2019-01-20;5233;5;1.3;130;280
170108;;2019-01-20;4520291;180;52.09;5209;840
170108;;2019-01-20;;;52.09;5209;840
170108;;2019-01-20;;0;52.09;5209;840
170108;;2019-01-20;0;;52.09;5209;840
170108;;2019-01-20;10;100;52.09;5209;840
2291287;;2019-01-20;120915;0;0;0;0"""
)

fake_source['avia_rs'] = Data([
    {"clicks": 17,  "shows": 17, "clid": None,           "national_version": "com",
        "bucks": "63.864835", "client_id": 34879676},
    {"clicks": 7,   "shows": 7,  "clid": "j:null",       "national_version": "kz",
        "bucks": "10.005718", "client_id": 13361436},
    {"clicks": 120, "shows": 11, "clid": "2328170",      "national_version": "kz",
        "bucks": "2.429388",  "client_id": 1612908},
    {"clicks": 1,   "shows": 1,  "clid": "2328169-637",	 "national_version": "kz",
        "bucks": "1.429388",  "client_id": 1612907},
    {"clicks": 1,   "shows": 1,  "clid": "2323977-673",	 "national_version": "kz",
        "bucks": "1.429388",  "client_id": 13361436},
    {"clicks": 1,   "shows": 1,  "clid": "2323977-671",	 "national_version": "kz",
        "bucks": "1.429388",  "client_id": 13361436},
    {"clicks": 4,   "shows": 4,  "clid": "2279278-100",	 "national_version": "kz",
        "bucks": "5.717553",  "client_id": 8215909},
    {"clicks": 1,   "shows": 1,  "clid": "2262097-252",	 "national_version": "kz",
        "bucks": "1.429388",  "client_id": 6056368},
    {"clicks": 1,   "shows": 1,  "clid": "2256434-306",	 "national_version": "kz",
        "bucks": "1.429388",  "client_id": 13361436},
    {"clicks": 3,   "shows": 3,  "clid": "2256434-306",	 "national_version": "kz",
        "bucks": "4.288165",  "client_id": 8215909}
], convert_to_strio=False)

fake_source['avia_product_completions'] = Data([
    {'client_id': 45043603, 'clicks': 35, 'shows': 0, 'product_id': 508972, 'national_version': 'com',
        'price': '10.5', 'contract_id': 422163, 'currency': 'EUR'},
    {'client_id': 34879676, 'clicks': 15, 'shows': 0, 'product_id': 508972, 'national_version': 'com',
        'price': '4.5', 'contract_id': 295936, 'currency': 'EUR'},
    {'client_id': 9096976,  'clicks': 6,  'shows': 0, 'product_id': 508967, 'national_version': 'kz',
        'price': '0.708015', 'contract_id': 349525, 'currency': 'EUR'}
], convert_to_strio=False)

fake_source['bk'] = (
    """20190120000000	99758	542	1	0	2164	7	2.9028	2902800	1651
20190120000000	99758	542	6	0	56	0	0.0000	0	1651
20190120000000	99758	542	9	0	1	0	0.0000	0	1651
20190120000000	90114	542	1	0	654	0	0.0000	0	395
20190120000000	90114	542	6	0	17	0	0.0000	0	395
20190120000000	90122	542	1	0	129	4	1.8959	1895866	38
20190120000000	90122	542	6	0	1	0	0.0000	0	38
20190120000000	186038	542	6	0	61	1	0.3107	310733	1182
20190120000000	186038	542	9	0	2	0	0.0000	0	1182
20190120000000	186095	542	1	0	6925	44	17.1317	17131733	2471
#End"""
)

fake_source['blue_market'] = (
    """dt, client_id, product_id, amount
    2019-01-20, 11, 1, 134.55
    2019-01-20, 11, 2, 1.55
    2019-01-20, 12, 1, 0.55
    2019-01-20, 13, 3, 1000"""
)

fake_source['red_market'] = (
    """dt,client_id,product_id,currency,amount,service_id
    2019-01-20,11,1,USD,134.55,618
    2019-01-20,11,2,USD,1.55,618
    2019-01-20,12,1,USD,0.55,618
    2019-01-20,13,3,,1000.001,618"""
)

fake_source['purple_market'] = (
    """dt,client_id,product_id,currency,amount,service_id
    2019-01-20,11,1,USD,134.55,665
    2019-01-20,11,2,USD,1.55,665
    2019-01-20,12,1,USD,0.55,665
    2019-01-20,13,3,,1000.001,666"""
)

fake_source['boyscouts'] = Data([{
    "client_id": "42706149",
    "amount": 1500,
    "transaction_type": "payment",
    "service_id": "619",
    "payment_type": "scout",
    "payload": "{\"db\"=\"d1266dc305844c4291807cf1d2c18085\";\"scout_id\"=\"yantonenkornduber\";\"scout_name\"=\"\\u0430\\u043d\\u0442\\u043e\\u043d\\u0435\\u043d\\u043a\\u043e \\u044f\\u0440\\u043e\\u0441\\u043b\\u0430\\u0432uber\";\"uuid\"=\"fdb7d93651abde7102341305bfee45d7\"}",
    "currency": "rub",
    "transaction_id": "008893cac56c18e20d7fe4565b014aaa",
    "dt": "1999-01-01T19:47:51Z"
}], convert_to_strio=False)

fake_source['cloud'] = Data([
    {"product_id": 508563, "amount": 8548.156, "contract_id": 669224, "project_id": "00000000-0000-0000-0000-000000000000"},
    {"product_id": 508563, "amount": 4136, "contract_id": None, "project_id": "00000000-0000-0000-0000-000000000001"},
    {"product_id": 508563, "amount": 4236, "contract_id": None, "project_id": "00000000-0000-0000-0000-000000000004"},
    {"product_id": 508563, "amount": 46289, "contract_id": 669224, "project_id": "19a10406-3ba0-4427-8280-35616eccde89"},
    {"product_id": 508563, "amount": 338, "contract_id": None, "project_id": "232a724c-ba16-4474-93eb-3ab387d1d2fe"},
    {"product_id": 508563, "amount": 1018, "contract_id": None, "project_id": "2ee4b758-e84c-4f8c-b63e-909cb0885ae4"}],
    convert_to_strio=False)

fake_source['hosting_service'] = Data([
    {"product_id": 508563, "amount": 8548.156, "contract_id": 669224, "project_id": "00000000-0000-0000-0000-000000000000"},
    {"product_id": 508563, "amount": 4136, "contract_id": None, "project_id": "00000000-0000-0000-0000-000000000001"},
    {"product_id": 508563, "amount": 4236, "contract_id": None, "project_id": "00000000-0000-0000-0000-000000000004"},
    {"product_id": 508563, "amount": 46289, "contract_id": 669224, "project_id": "19a10406-3ba0-4427-8280-35616eccde89"},
    {"product_id": 508563, "amount": 338, "contract_id": None, "project_id": "232a724c-ba16-4474-93eb-3ab387d1d2fe"},
    {"product_id": 508563, "amount": 1018, "contract_id": None, "project_id": "2ee4b758-e84c-4f8c-b63e-909cb0885ae4"}],
    convert_to_strio=False)

fake_source['d_installs'] = (
    r"""{"values": [
{"path_override_by_dictionary": "1996812","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
"install_new": 12.0,"path__lvl": 4,"path": "\tR\tYandex software on third party websites\tpunto.browser.yandex.ru\tUnique installation\t1996812\t"},
{"path_override_by_dictionary": "Unique installation","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
 "install_new": 12.0,"path__lvl": 3,"path": "\tR\tYandex software on third party websites\tpunto.browser.yandex.ru\tUnique installation\t"},
{"path_override_by_dictionary": "punto.browser.yandex.ru","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
 "install_new": 12.0,"path__lvl": 2,"path": "\tR\tYandex software on third party websites\tpunto.browser.yandex.ru\t"},
{"path_override_by_dictionary": "393","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
 "install_new": 7.0,"path__lvl": 5,"path": "\tR\tCommercial bundling\tsavefrom.net\tUnique installation\t2157766\t393\t"},
{"path_override_by_dictionary": "392","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 736.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tsavefrom.net\tUnique installation\t2157766\t392\t"},
{"path_override_by_dictionary": "390","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 1.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tsavefrom.net\tUnique installation\t2157766\t390\t"},
{"path_override_by_dictionary": "388","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 5.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tsavefrom.net\tUnique installation\t2157766\t388\t"},
{"path_override_by_dictionary": "2271202","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
 "install_new": 1.0,"path__lvl": 4,"path": "\tR\tCommercial bundling\tbm.zona.ru\tUnique installation\t2271202\t"},
{"path_override_by_dictionary": "3","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 1.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tbm.zona.ru\tUnique installation\t2224484\t3\t"},
{"path_override_by_dictionary": "2","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 1.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tbm.zona.ru\tUnique installation\t2224484\t2\t"}
]}"""
)

fake_source['dsp'] = (
    """2019-01-20	128934	2	0	2	2563049	1	0	0	0	12	0	0	0	0	0
2019-01-20	112158	2	0	0	2563113	0	0	0	0	24	0	0	0	0	0
2019-01-20	185291	4	0	0	2563081	0	0	0	0	7	0	0	0	0	0
2019-01-20	188382	227	0	100003014	1	1488	34257590	16272435	610	1488	40427646	0	0	621	7
2019-01-20	142545	104	0	499000	2563049	71	26946000	13473000	27	73	39563000	0	0	0	0
2019-01-20	189903	87	0	100001439	1	155	8508100	4041357	70	2850	10039976	0	0	202	2
2019-01-20	188382	191	0	100003434	10	4544	0	0	0	4544	0	0	0	0	0
2019-01-20	208444	1	0	13565962	2563049	0	0	0	0	1	0	0	0	0	0
2019-01-20	192531	2	0	0	2563081	0	0	0	0	54	0	0	0	0	0
2019-01-20	224669	5	0	0	2563081	0	0	0	0	19	0	0	0	0	0
#end"""
)

fake_source['health'] = (
    """{
      "data": [{
        "attributes": {
          "ClientID": 15919809,
          "appointments": [
            {
              "data": {
                "attributes": {
                  "appointment_id": 2559,
                  "price": 500,
                  "service_date": "2019-01-20T15:57:45.165483+03:00"
                },
                "id": 55,
                "type": "appointments"
              }
            }
          ],
          "date_from": "2019-01-20",
          "date_to": "2019-01-20",
          "total": 500
        },
        "id": 83,
        "type": "monthly_total"
      }]
    }"""
)

fake_source['multiship'] = (
    """{
        "status": "ok",
        "data": {
            "transactions":
            [
                {
                    "amount": "-295.00",
                    "contract_id": "666",
                    "entity_id": "100002237",
                    "id": "94",
                    "is_correction": "0",
                    "product_id": "504716",
                    "time": "2019-01-20 00:00:00"
                },
                {
                    "amount": "-25.00",
                    "contract_id": "666",
                    "entity_id": "100002238",
                    "id": "95",
                    "is_correction": "1",
                    "product_id": "504716",
                    "time": "2019-01-20 00:00:00"
                }
            ]
        }
    }"""
)

fake_source['rs_market'] = (
    """2297463;225;2019-01-20;0;0;0;0;1
1985209;105;2019-01-20;0;0;0;0;1
1787308;;2019-01-20;58886;119;35.25;3525;788
2051477;;2019-01-20;0;0;0;0;2
2175745;;2019-01-20;0;0;0;0;8
2282513;;2019-01-20;0;0;0;0;4
459909;;2019-01-20;38;0;0;0;0
2210393;;2019-01-20;260102;495;187.37;18737;196
2291166;;2019-01-20;444;0;0;0;4
2291166;;2019-01-20;;;0;0;1
2291166;;2019-01-20;;1;0;0;2
2291166;;2019-01-20;1;;0;0;3
2291166;;2019-01-20;1;100;0;0;3
2136398;;2019-01-20;14079;34;11.82;1182;46"""
)

fake_source['rs_market_cpa'] = (
    """2256021;;2019-01-20;36;0;0;
2270534;;2019-01-20;20;0;0;
2285121;;2019-01-20;1;0;0;
2295143;;2019-01-20;1;0;0;
2271525;;2019-01-20;2297;6;73.2582;
2277535;;2019-01-20;0;2;13.120125;
2290309;225;2019-01-20;1;0;0;
1955451;;2019-01-20;1666;2;10.99039167;
2066562;;2019-01-20;1900;2;16.014785;
2039513;;2019-01-20;3546;2;8.76033333;"""
)

fake_source['rtb_distr'] = (
    """2019-01-20	153638	6	2267211	-1	169	0	0	1	3	19360	19360	1	3	22852	0	0	1	0
2019-01-20	153638	2	2263185	-1	995	0	0	1	117	4290600	4290600	80	117	5063375	0	0	75	1
2019-01-20	185103	14	2289243	-1	208	0	0	1	1	0	0	0	1	0	0	0	0	0
2019-01-20	185103	22	2263191	-1	225	0	0	10	69064	0	0	0	69064	0	0	0	0	0
2019-01-20	153638	12	2263191	-1	168	0	0	1	176	4131380	4131380	88	176	4875617	0	0	76	1
2019-01-20	153638	2	2266536	-1	209	0	0	1	9	203250	203250	5	9	239864	0	0	5	0
2019-01-20	153638	3	2263191	-1	977	0	0	1	10692	83014920	83014920	2136	10692	97970373	0	0	2114	14
2019-01-20	153638	6	2266536	-1	225	0	0	1	78852	6191256060	6191256060	51713	78852	7305991760	0	0	45568	717
2019-01-20	153638	12	2266536	-1	134	0	0	1	17	273030	273030	12	17	322245	0	0	11	0
2019-01-20	153638	1	2278292	-1	137	0	0	1	3	0	0	0	3	0	0	0	0	0
2019-01-20	49688	21	2064708	-1	225	0	0	2	0	0	0	0	1	0	0	0	0	0
2019-01-20	49688	32	2100783	3	225	0	0	2	0	0	0	0	1	0	0	0	0	0
2019-01-20	49688	32	2052594	1	225	0	0	1	1	58660	58660	1	1	69227	0	0	2	0
2019-01-20	153638	12	2263191	-1	94	0	0	1	9	825490	825490	8	9	974142	0	0	8	0
2019-01-20	153638	1	2266102	-1	977	0	0	1	153	253700	253700	3	153	299385	0	0	3	0
2019-01-20	153638	6	2279175	-1	20574	0	0	1	13	677510	677510	7	13	799499	0	0	6	0
2019-01-20	153638	3	2281101	-1	159	0	0	1	72	59860	59860	20	72	70778	0	0	20	0
2019-01-20	153638	6	2267211	-1	206	0	0	1	15	1613380	1613380	11	15	1903853	0	0	10	0
2019-01-20	153638	6	2278292	-1	117	0	0	1	5	79370	79370	3	5	93668	0	0	3	0
2019-01-20	153638	6	2278336	-1	96	0	0	1	0	55390	55390	1	0	65370	0	0	1	0
2019-01-20	153638	1	2278336	-1	118	0	0	1	124	969230	969230	25	124	1143815	0	0	25	0
2019-01-20	153638	6	2267211	-1	187	0	0	1	16	713890	713890	10	16	842440	0	0	10	0"""
)

fake_source['serphits'] = (
    """20190120000000	63	2286651	84
20190120000000	227826	2041437	135
20190120000000	90	2149692	1
20190120000000	172351	2230936	6
20190120000000	227826	124995	13
20190120000000	172351	2226561	1783
20190120000000	172353	2084462	26
20190120000000	129	2261875	4
20190120000000	63	1969470	7
20190120000000	243663	2257594	36
#End"""
)

fake_source['tags3'] = (
    """20190120	93572	1073890801	542	1	2	0	0		2
20190120	93572	1073890803	542	1	2	1	141600		2
20190120	93572	1073890812	542	1	8	0	0		2
20190120	5597	1086570737	542	1	36	1	114067		2
20190120	5597	1086570737	542	6	2	0	0		2
20190120	5597	1086570739	542	1	95	2	1425734		2
20190120	5597	1086570739	542	6	3	0	0		2
20190120	141078	1088897027	542	1	4	0	0		2
20190120	141078	1088897028	542	1	1	0	0		2
20190120	141078	1088897036	542	1	3	1	125867		2
#End"""
)

fake_source['taxi_aggr'] = Data([
    {"client_id": 34901168,	"commission_currency": "RUB",	"commission_value": 4338.84, 	 "type": "order",
        "coupon_value": 0.0,   "payment_method": "cash", "subvention_value": 0.0},
    {"client_id": 34901168,	"commission_currency": "RUB",	"commission_value": 39.5, 	     "type": "order",
        "coupon_value": 0.0,   "payment_method": "corporate", "subvention_value": 0.0},
    {"client_id": 34905406,	"commission_currency": "RUB",	"commission_value": 64.3, 	     "type": "order",
        "coupon_value": 0.0,   "payment_method": "card", "subvention_value": 0.0},
    {"client_id": 37193959,	"commission_currency": "RUB",	"commission_value": 1022.2, 	 "type": "order",
        "coupon_value": 0.0,   "payment_method": "card", "subvention_value": 0.0},
    {"client_id": 37193959,	"commission_currency": "RUB",	"commission_value": 3426.8, 	 "type": "order",
        "coupon_value": 0.0,   "payment_method": "cash", "subvention_value": 0.0},
    {"client_id": 37193959,	"commission_currency": "RUB",	"commission_value": 37.5, 	     "type": "order",
        "coupon_value": 0.0,   "payment_method": "corporate", "subvention_value": 0.0},
    {"client_id": 37194208,	"commission_currency": "RUB",	"commission_value": 344.747627,  "type": "order",
        "coupon_value": 0.0,   "payment_method": "card", "subvention_value": 0.0},
    {"client_id": 38172961,	"commission_currency": "RUB",	"commission_value": 6429.281249, "type": "order",
        "coupon_value": 0.0,   "payment_method": "card", "subvention_value": 0.0},
    {"client_id": 38172961,	"commission_currency": "RUB",	"commission_value": 6068.910057, "type": "order",
        "coupon_value": 0.0,   "payment_method": "cash", "subvention_value": 0.0},
    {"client_id": 38172961,	"commission_currency": "RUB",	"commission_value": 309.006561,  "type": "order",
        "coupon_value": 0.0,   "payment_method": "corporate", "subvention_value": 0.0},
    {"client_id": 39123321,	"commission_currency": "USD",	"commission_value": 509.006561,  "type": "subvention",
        "coupon_value": 70.01, "payment_method": "cash", "subvention_value": 12.321},
    {"client_id": 39123321,	"commission_currency": "EUR",	"commission_value": 0, 	         "type": "subvention",
        "coupon_value": 0,      "payment_method": "cash",  "subvention_value": 13.321}
], convert_to_strio=False)

fake_source['taxi_distr'] = Data([
    {"utc_dt": "2019-01-20", "clid": 2319588, "product_id": 13002, "commission": 1901.90,
     "cost": 10010.00, "quantity": 11}
], convert_to_strio=False)

fake_source['taxi_medium'] = Data([
    {"count": 3522, 	"clid": 2040463, 	"commission_value": "2300.402857"},
    {"count": 1319, 	"clid": 2046579, 	"commission_value": "711.366497"},
    {"count": 23, 	    "clid": 2046818, 	"commission_value": "4.683405"},
    {"count": 1, 	    "clid": 2058507, 	"commission_value": "0.842541"},
    {"count": 236, 	    "clid": 2059749, 	"commission_value": "100.111212"},
    {"count": 4, 	    "clid": 2061400, 	"commission_value": "4.807665"},
    {"count": 3, 	    "clid": 2104423, 	"commission_value": "0.277805"},
    {"count": 2, 	    "clid": 2120168, 	"commission_value": "0.749922"},
    {"count": 1150,     "clid": 2190366, 	"commission_value": "526.806281"},
    {"count": 2, 	    "clid": 2221170, 	"commission_value": "0.658667"},
    {"count": 3, 	    "clid": 2222875, 	"commission_value": "7.742447"},
    {"count": 199, 	    "clid": 2228586, 	"commission_value": "76.750744"}
], convert_to_strio=False)

fake_source['taxi_stand_svo'] = SourceData([{
    'billing_client_id': '80781421',
    'payment_id': '100119754314295768443810525125500',
    'type': 'payment',
    'voucher_updated': '2018-07-25T06:54:23.780000Z',
    'price': 1234,
    'voucher_created': '2018-07-24T23:55:25.162000Z',
    'original_payment_id': None,
    'payment_fd': '6970618519452892',
    'payment_fn': '44997',
    'order_id': 'abc3707111d24d1fdfc8fedbfae36f71aa3def',
    'voucher_id': 'def4d7029329d9fa85d9cc93f58e2f715e7abc',
}])

fake_source['travel'] = Data([
    # Service: Яндекс.Путешествия
    {'client_id': 7654321, 'update_dt': '2018-10-12T01:13:17', 'transaction_type': 'payment', 'partner_id': 1234567,
        'payment_type': 'cost', '_expedia_key': '452308__12/23/2018__12/27/2018__4', 'orig_transaction_id': None,
        'trust_payment_id': 'trust_pmt_3', '_table': '2018-10-12', 'price': '355.29612178',
        'service_order_id': 'YA-0000-0000-0003', 'currency': 'USD', '_expedia_type': 'FakePurchase',
        'dt': '2018-10-12T01:13:17', 'transaction_id': 92},
    {'client_id': 7654321, 'update_dt': '2018-10-12T01:13:17', 'transaction_type': 'payment', 'partner_id': 1234567,
        'payment_type': 'reward', '_expedia_key': '452308__12/23/2018__12/27/2018__4', 'orig_transaction_id': None,
        'trust_payment_id': 'trust_pmt_3', '_table': '2018-10-12', 'price': '28.70387822',
        'service_order_id': 'YA-0000-0000-0003', 'currency': 'USD', '_expedia_type': 'FakePurchase',
        'dt': '2018-10-12T01:13:17', 'transaction_id': 93, 'service_id': 641},

    # Service: Я.Путешествия электрички ЦППК
    # payment
    {'client_id': 7654321, 'update_dt': '2018-10-12T01:13:17', 'transaction_type': 'payment', 'partner_id': 1234567,
     'payment_type': 'cost', 'orig_transaction_id': None, 'trust_payment_id': 'trust_pmt_3', 'price': '355.29612178',
     'service_order_id': 'YA-0000-0000-0003', 'currency': 'USD', 'dt': '2018-10-12T01:13:17', 'transaction_id': 94,
     'service_id': 716},
    {'client_id': 7654321, 'update_dt': '2018-10-12T01:13:17', 'transaction_type': 'payment', 'partner_id': 1234567,
     'payment_type': 'reward', 'orig_transaction_id': None, 'trust_payment_id': 'trust_pmt_3', 'price': '28.70387822',
     'service_order_id': 'YA-0000-0000-0003', 'currency': 'USD', 'dt': '2018-10-12T01:13:17', 'transaction_id': 95,
     'service_id': 716},
    # refund
    {'client_id': 7654321, 'update_dt': '2018-10-12T01:13:17', 'transaction_type': 'refund', 'partner_id': 1234567,
     'payment_type': 'reward', 'orig_transaction_id': None, 'trust_payment_id': 'trust_pmt_3', 'price': '355.29612178',
     'service_order_id': 'YA-0000-0000-0003', 'currency': 'USD', 'dt': '2018-10-12T01:13:17', 'transaction_id': 96,
     'service_id': 716},
    {'client_id': 7654321, 'update_dt': '2018-10-12T01:13:17', 'transaction_type': 'refund', 'partner_id': 1234567,
     'payment_type': 'reward', 'orig_transaction_id': None, 'trust_payment_id': 'trust_pmt_3', 'price': '28.70387822',
     'service_order_id': 'YA-0000-0000-0003', 'currency': 'USD', 'dt': '2018-10-12T01:13:17', 'transaction_id': 97,
     'service_id': 716},
], convert_to_strio=False)

fake_source['practicum_spendable'] = Data([
    # Service: Яндекс.Практикум
    {"transaction_id": 1, "service_id": 1041, "service_order_id": "1", "dt": "2021-03-17T07:26:51.005463+00:00",
     "client_id": 100, "transaction_type": "payment", "orig_transaction_id": None, "payload": "{}",
     "payment_type": "practicum_flow_1", "amount": "20000.00", "currency": "RUB"},
    {"transaction_id": 2, "service_id": 1041, "service_order_id": "1", "dt": "2021-03-17T07:26:51.005463+00:00",
     "client_id": 100, "transaction_type": "payment", "orig_transaction_id": None, "payload": "{}",
     "payment_type": "practicum_flow_1", "amount": "20000.00", "currency": "RUB"},

    # refund
    {"transaction_id": 3, "service_id": 1041, "service_order_id": "1", "dt": "2021-03-17T07:26:51.005463+00:00",
     "client_id": 100, "transaction_type": "refund", "orig_transaction_id": None, "payload": "{}",
     "payment_type": "practicum_flow_1", "amount": "20000.00", "currency": "RUB"},
    {"transaction_id": 4, "service_id": 1041, "service_order_id": "1", "dt": "2021-03-17T07:26:51.005463+00:00",
     "client_id": 100, "transaction_type": "refund", "orig_transaction_id": None, "payload": "{}",
     "payment_type": "practicum_flow_1", "amount": "20000.00", "currency": "RUB"},
], convert_to_strio=False)

fake_source['zaxi_selfemployed_spendable'] = Data([
    # Service: Яндекс.Заправки: Выплаты самозанятым
    {"transaction_id": 1, "service_id": 1120, "service_order_id": "1", "dt": "2021-03-17T07:26:51.005463+00:00",
     "client_id": 100, "transaction_type": "payment", "orig_transaction_id": None, "payload": "{}",
     "payment_type": "zaxi", "amount": "20000.00", "currency": "RUB"},
    {"transaction_id": 2, "service_id": 1120, "service_order_id": "1", "dt": "2021-03-17T07:26:51.005463+00:00",
     "client_id": 100, "transaction_type": "payment", "orig_transaction_id": None, "payload": "{}",
     "payment_type": "zaxi", "amount": "20000.00", "currency": "RUB"},

    # refund
    {"transaction_id": 3, "service_id": 1120, "service_order_id": "1", "dt": "2021-03-17T07:26:51.005463+00:00",
     "client_id": 100, "transaction_type": "refund", "orig_transaction_id": None, "payload": "{}",
     "payment_type": "zaxi", "amount": "20000.00", "currency": "RUB"},
    {"transaction_id": 4, "service_id": 1120, "service_order_id": "1", "dt": "2021-03-17T07:26:51.005463+00:00",
     "client_id": 100, "transaction_type": "refund", "orig_transaction_id": None, "payload": "{}",
     "payment_type": "zaxi", "amount": "20000.00", "currency": "RUB"},
], convert_to_strio=False)

fake_source['toloka'] = json.dumps([
    {
        "currency": "RUB",
        "doc_number": 535232,
        "ground": "Yandex.Zen Payment",
        "payment_system_answer": "{\"status\": \"0\", \"clientOrderId\": \"535232\", \"balance\": \"299999.00\", \"identification\": \"identified\", \"processedDT\": \"1999-01-01T10:21:38.792+03:00\"}",
        "service": 5,
        "metadata": "{\"client_id\": 123}",
        "status": "exported_to_h2h",
        "summ": "1.00",
        "t_acc": "41001926962414",
        "t_acc_type": "wallet",
        "transaction_id": "7"
    }
])

fake_source['video_distr'] = (
    """2019-01-20	49688	16	2242347	-1	225	0	0	2563180	0	3000	0	0	4	0	0	0	0	0	5
2019-01-20	49688	13	2242347	-1	225	33	2	2	1	0	0	0	1	0	0	0	0	0	3
2019-01-20	49688	113	2242347	-1	225	0	0	1	0	0	0	0	33	0	0	0	0	0	2
2019-01-20	260290	1	2321787	-1	225	16	2	2563117	2	237660	166362	1	2	2376600	0	0	0	0	5
2019-01-20	260290	1	2321787	-1	225	0	0	1	0	0	0	0	8	0	0	0	0	0	6
2019-01-20	231296	7	2321787	-1	225	16	2	2563117	2	167370	117159	1	2	1673700	0	0	0	0	5
2019-01-20	260290	1	2321787	-1	225	16	2	2563117	2	237660	166362	1	2	2376600	0	0	0	0	5"""
)

fake_source['zen'] = json.dumps([
    {
        "currency": "RUB",
        "doc_number": 535232,
        "ground": "Yandex.Zen Payment",
        "payment_system_answer": "{\"status\": \"0\", \"clientOrderId\": \"535232\", \"balance\": \"299999.00\", \"identification\": \"identified\", \"processedDT\": \"1999-01-01T10:21:38.792+03:00\"}",
        "service": 5,
        "metadata": "{\"client_id\": 123}",
        "status": "exported_to_h2h",
        "summ": "1.00",
        "t_acc": "41001926962414",
        "t_acc_type": "wallet",
        "transaction_id": "7"
    }
])

fake_source['mediaservice_subagency_payment'] = SourceData([
    {"client_id": 44, "amount": 100.0, "transaction_type": "payment", "service_id": 325, "payment_type": "card",
     "currency": "RUB", "transaction_id": 10315974, "payment_id": 10315974, "dt": "2020-04-09T00:00:00+03:00",
     "service_order_id": 29058, "commission_category": "3", "payload": "{\"systemName\":\"testing1\"}"},

    {"client_id": 44, "amount": 100.0, "transaction_type": "refund", "service_id": 325, "payment_type": "card",
     "currency": "RUB", "transaction_id": 10316074, "payment_id": 10315974, "dt": "2020-04-09T00:00:00+03:00",
     "service_order_id": 29058, "commission_category": "3", "payload": "{\"systemName\":\"testing2\"}"},

    {"client_id": 3, "amount": 1500.0, "transaction_type": "payment", "service_id": 326, "payment_type": "card",
     "currency": "RUB", "transaction_id": 10318424, "payment_id": 10318424, "dt": "2020-04-09T00:00:00+03:00",
     "service_order_id": 29086, "commission_category": None, "payload": "{\"systemName\":\"testing3\"}"},

    {"client_id": 3, "amount": 1500.0, "transaction_type": "refund", "service_id": 326, "payment_type": "card",
     "currency": "RUB", "transaction_id": 10318434, "payment_id": 10318424, "dt": "2020-04-09T00:00:00+03:00",
     "service_order_id": 29086, "commission_category": None, "payload": "{\"systemName\":\"testing4\"}"},
])

fake_source['drive_fueling'] = SourceData([
    {
        "value_amount": "81.40",
        "client_id": "666666",
        "transaction_type": "payment",
        "service_id": "644",
        "payment_type": "drive_fueler",
        "payload": "fake",
        "product": "drive_fueler",
        "service_order_id": "268545f6-694d-4526-af55-dca9a99b2553",
        "paysys_partner_id": "yadrive",
        "value_currency": "RUB",
        "dt": "2019-03-20T17:00:00.000000Z",
        "transaction_id": "268545f6-694d-4526-af55-dca9a99b2553",
        "payment_id": "268545f6-694d-4526-af55-dca9a99b2553"
    }
])

fake_source['drive_penalty'] = SourceData([{
    "client_id": "12345",
    "promocode_sum": 0,
    "type": "1",
    "payment_type": "card",
    "orig_transaction_id": "00000ecc-0b3a-3684-c405-bc0302ae9b7c",
    "transaction_currency": "RUB",
    "service_order_id": "000018a8-6614-b93b-cf5f-9b0502ae9b7c",
    "commission_sum": 0,
    "use_discount": 0,
    "total_sum": "-200.0",
    "dt": "2019-03-21T12:01:15.000000Z",
    "transaction_id": "00059cca-5db6-0405-a9bd-0cc3a5f42677"
}])

fake_source['food_payment'] = SourceData([
    {
        "transaction_id": 1337,
        "client_id": 13370,
        "dt": map_dt["food_payment"].strftime("%Y-%m-%d %H:%M:%S"),
        "payload": "[]",
        "payment_id": 1337,
        "payment_type": "card",
        "paysys_type_cc": "payture",
        "product": "goods",
        "service_id": 629,
        "service_order_id": "Ordnung muss sein",
        "transaction_type": "payment",
        "utc_start_load_dttm": map_dt["food_payment"].strftime("%Y-%m-%d %H:%M:%S"),
        "value_amount": Decimal("13.37"),
        "value_currency": "RUB",
    },
    {
        "transaction_id": 1337,
        "client_id": 13370,
        "dt": map_dt["food_payment"].strftime("%Y-%m-%d %H:%M:%S"),
        "payload": "[]",
        "payment_id": 1337,
        "payment_type": "card",
        "paysys_type_cc": "payture",
        "product": "goods",
        "service_id": 662,
        "service_order_id": "Ordnung muss sein",
        "transaction_type": "payment",
        "utc_start_load_dttm": map_dt["food_payment"].strftime("%Y-%m-%d %H:%M:%S"),
        "value_amount": Decimal("13.37"),
        "value_currency": "RUB",
    },
    {
        "transaction_id": 1337,
        "client_id": 13370,
        "dt": map_dt["food_payment"].strftime("%Y-%m-%d %H:%M:%S"),
        "payload": "[]",
        "payment_id": 1337,
        "payment_type": "card",
        "paysys_type_cc": "payture",
        "product": "goods",
        "service_id": 676,
        "service_order_id": "Ordnung muss sein",
        "transaction_type": "payment",
        "utc_start_load_dttm": map_dt["food_payment"].strftime("%Y-%m-%d %H:%M:%S"),
        "value_amount": Decimal("13.37"),
        "value_currency": "RUB",
    }
])

fake_source['food_srv'] = SourceData([
    {
        "client_id": 13370,
        "commission_sum": Decimal("1234.56"),
        "service_id": 628,
        "transaction_currency": "RUB",
        "type": "goods",
        "dt": "2019-12-21",  # stager отдал dt, запишем в поле dt
    },
    {
        "client_id": 13370,
        "commission_sum": Decimal("1237.56"),
        "service_id": 661,
        "transaction_currency": "RUB",
        "type": "goods",
        "dt": "2019-12-21",  # stager отдал dt, запишем в поле dt
    },
    {
        "client_id": 13371,
        "commission_sum": Decimal("1234.56"),
        "service_id": 628,
        "transaction_currency": "RUB",
        "type": "goods",
        #  stager не отдал dt, в dt запишется дата забора
    },
    {
        "client_id": 13370,
        "commission_sum": Decimal("1237.56"),
        "service_id": 661,
        "transaction_currency": "RUB",
        "type": "goods",
        #  stager не отдал dt, в dt запишется дата забора
    },
    {
        "client_id": 13372,
        "commission_sum": Decimal("1238.56"),
        "service_id": 675,
        "transaction_currency": "RUB",
        "type": "goods",
        #  stager не отдал dt, в dt запишется дата забора
    },
])

fake_source['cloud_referal_payment'] = SourceData([
    {
        "transaction_id": 1337,
        "orig_transaction_id": None,
        "client_id": 13370,
        "dt": map_dt["cloud_referal_payment"].strftime("%Y-%m-%d %H:%M:%S"),
        "payment_id": 1337,
        "payment_type": "reward",
        "paysys_type_cc": "yandex",
        "product": '666',
        "service_id": 693,
        "service_order_id": "Ordnung muss sein",
        "transaction_type": "payment",
        "value_amount": Decimal("13.37"),
        "value_currency": "RUB",
    }
]
)

fake_source['news_payment'] = SourceData([
    {
        "transaction_id": 1434,
        "service_id": 1127,
        "service_order_id": "42342",
        "dt": map_dt["news_payment"].strftime("%Y-%m-%d %H:%M:%S"),
        "client_id": 14340,
        "transaction_type": "payment",
        "orig_transaction_id": 1434,
        "payload": "trt",
        "payment_type": "news_payment",
        "amount": "14.34",
        "currency": "RUB",
    }
])

fake_source['drive_corp'] = SourceData([
  {
    "client_id": "1",
    "type": "carsharing",
    "transaction_type":  "payment",
    "payment_type": "wallet",
    "orig_transaction_id": "ec37bb4d-5b862644-171d24b6-6208693",
    "transaction_currency": "RUB",
    "service_order_id": "ec37bb4d-5b862644-171d24b6-6208693",
    "use_discount": 0,
    "total_sum": "85.54",
    "dt": "2020-07-31T00:00:00.000000Z",
    "transaction_id": "3df7b6d3-5df8-ec9b-fcf3-8af32f57d2b6",
    "commission_sum": "0.0",
    "promocode_sum": "0.00",
  }
])

fake_source['oebs_compls'] = SourceData([
    {   # будет зафильтрована по source_tabname
        "client_id": "123",
        "service_id": 111,
        "accounting_period": "2020-06",
        "balance_tabname": "2020-06-30",
        "transaction_id_max": 888999,
        "product_id": "54321",
        "amount_vat": "-8.33",
        "partner_contract_id": "54345",
        "amount_wo_vat": "-41.67",
        "event_time": "2020-06-28T21:00:00Z",
        "currency": "RUB",
        "source_tabname": "2020-06-30",
        "amount_w_vat": "-50.01"
    },
    {   # будет зафильтрована по source_tabname
        "client_id": "1234",
        "service_id": 128,
        "accounting_period": None,
        "balance_tabname": "2020-06-30",
        "transaction_id_max": None,
        "product_id": "54321",
        "amount_vat": "8.33",
        "partner_contract_id": "54345",
        "amount_wo_vat": "41.67",
        "event_time": "2020-06-29T21:00:00Z",
        "currency": "RUB",
        "source_tabname": "2020-06-30",
        "amount_w_vat": "50.01"
    },
    {
        "client_id": "123",
        "service_id": 111,
        "accounting_period": "2020-06",
        "balance_tabname": "2020-06-30",
        "transaction_id_max": 888999,
        "product_id": "54321",
        "amount_vat": "-8.33",
        "partner_contract_id": "54345",
        "amount_wo_vat": "-41.67",
        "event_time": "2020-06-28T21:00:00Z",
        "currency": "RUB",
        "source_tabname": "2020-11-01",
        "amount_w_vat": "-50.01"
    },
    {
        "client_id": "1234",
        "service_id": 128,
        "accounting_period": None,
        "balance_tabname": "2020-06-30",
        "transaction_id_max": None,
        "product_id": "54321",
        "amount_vat": "8.33",
        "partner_contract_id": "54345",
        "amount_wo_vat": "41.67",
        "event_time": "2020-06-29T21:00:00Z",
        "currency": "RUB",
        "source_tabname": "2020-11-01",
        "amount_w_vat": "50.01"
    },
])

fake_source['disk_b2b'] = SourceData([
    {
        "client_id": "1",
        "service_id": 671,
        "type": "disk_b2b",
        "transaction_currency": "RUB",
        "service_order_id": "090c3157-f8bc-41a4-a7a6-cd597a2db974",
        "total_sum": "85.54",
        "dt": "2021-07-31T00:00:00.000000Z"
    },
    {
        "client_id": "1",
        "service_id": 671,
        "type": "disk_b2b",
        "transaction_type":  "payment",
        "transaction_currency": "RUB",
        "service_order_id": "090c3157-f8bc-41a4-a7a6-cd597a2db975",
        "total_sum": "85.55",
        "dt": "2021-07-31T00:00:00.000000Z"
    },
    {
        "client_id": "1",
        "service_id": 671,
        "type": "disk_b2b",
        "transaction_type":  "refund",
        "transaction_currency": "RUB",
        "service_order_id": "090c3157-f8bc-41a4-a7a6-cd597a2db976",
        "total_sum": "85.56",
        "dt": "2021-07-31T00:00:00.000000Z"
    }
])


def tuple_to_dict(t, keys, ignore_fields=tuple()):
    assert isinstance(t, tuple)
    return {k: v for k, v in zip(keys, t) if k not in ignore_fields}


expected = {}

expected['drive'] = Data([
    (datetime(2019, 1, 20, 0, 0), 666, None, None, 0, 604, None, None, Decimal('666.66'), 13372, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20, 0, 0), 666, None, None, 0, 604, None, None, Decimal('777.77'), 13372, None, None, None, None, None, None, None, None, None),
], ignore_fields=('id', ))

expected['drive_plus'] = Data([
    (datetime(2019, 1, 20, 0, 0), 666, None, None, 0, 705, None, None, Decimal('333.33'), 13372, None, None, None, None, None, None, "yandex_account_topup", None, None),
    (datetime(2019, 1, 20, 0, 0), 666, None, None, 0, 705, None, None, Decimal('444.44'), 13372, None, None, None, None, None, None, "yandex_account_withdraw", None, None)
], ignore_fields=('id', ))

expected['distr_pages'] = [(place_id, 542, 1, 10000)
                           for place_id in source_conditions['distr_pages']['params']['places']]

expected['activations'] = [
    (2257150, 3010, datetime(2019, 1, 20), 0, 472, 0, 0, 0, 0, 0, 8, None, None, None, None, 10, None),
    (2257150, 3010, datetime(2019, 1, 20), 0, 55, 0, 0, 0, 0, 0, 8, None, None, None, None, 11, None),
    (2257150, 3010, datetime(2019, 1, 20), 0, 20, 0, 0, 0, 0, 0, 8, None, None, None, None, 15, None),
    (2257150, 3010, datetime(2019, 1, 20), 0, 8, 0, 0, 0, 0, 0, 8, None, None, None, None, 3, None),
    (2257150, 3010, datetime(2019, 1, 20), 0, 1, 0, 0, 0, 0, 0, 8, None, None, None, None, 5, None),
    (2257150, 3010, datetime(2019, 1, 20), 0, 3, 0, 0, 0, 0, 0, 8, None, None, None, None, 8, None),
    (2257150, 3010, datetime(2019, 1, 20), 0, 29, 0, 0, 0, 0, 0, 8, None, None, None, None, 9, None),
    (2257166, 3010, datetime(2019, 1, 20), 0, 19, 0, 0, 0, 0, 0, 8, None, None, None, None, None, None),
    (2257310, 3010, datetime(2019, 1, 20), 0, 3, 0, 0, 0, 0, 0, 8, None, None, None, None, None, None),
    (2257342, 3010, datetime(2019, 1, 20), 0, 451, 0, 0, 0, 0, 0, 8, None, None, None, None, None, None)
]

expected['addapter_dev_ds'] = [
    (datetime(2019, 1, 20), 2106502, 2106550, 4011, 534, 6, 25),
    (datetime(2019, 1, 20), 777888999, 127774, 4011, 6, 2, 25),
    (datetime(2019, 1, 20), 87452, 123, 4011, Decimal('54.21'), 1, 25),
    (datetime(2019, 1, 20), 2276700, 123, 4011, Decimal('32.04'), 1, 25),
    (datetime(2019, 1, 20), 2075745, 123, 4011, 45, 1, 25),
    (datetime(2019, 1, 20), 25421, 123, 4011, Decimal('21.21'), 1, 25),
    (datetime(2019, 1, 20), 8754, 123, 4011, Decimal('24.51'), 1, 25),
    (datetime(2019, 1, 20), 87565442, 123, 4011, Decimal('22.22'), 1, 25),
    (datetime(2019, 1, 20), 521, 123, 4011, Decimal('22.22'), 1, 25),
    (datetime(2019, 1, 20), 4874, 123, 4011, Decimal('54.21'), 1, 25)
]

expected['addapter_dev_com'] = [
    (datetime(2019, 1, 20), 2106502, 2106550, 4012, 534, 6, 26),
    (datetime(2019, 1, 20), 777888999, 127774, 4012, 6, 2, 26),
    (datetime(2019, 1, 20), 87452, 123, 4012, Decimal('54.21'), 1, 26),
    (datetime(2019, 1, 20), 2276700, 123, 4012, Decimal('32.04'), 1, 26),
    (datetime(2019, 1, 20), 2075745, 123, 4012, 45, 1, 26),
    (datetime(2019, 1, 20), 25421, 123, 4012, Decimal('21.21'), 1, 26),
    (datetime(2019, 1, 20), 8754, 123, 4012, Decimal('24.51'), 1, 26),
    (datetime(2019, 1, 20), 87565442, 123, 4012, Decimal('22.22'), 1, 26),
    (datetime(2019, 1, 20), 521, 123, 4012, Decimal('22.22'), 1, 26),
    (datetime(2019, 1, 20), 4874, 123, 4012, Decimal('54.21'), 1, 26)]

expected['addapter_ret_com'] = [
    (datetime(2019, 1, 20), 2106550, 2106502, 4010, 654, 6, 24),
    (datetime(2019, 1, 20), 127774, 777888999, 4010, 4, 2, 24),
    (datetime(2019, 1, 20), 123, 87452, 4010, Decimal('56.5'), 1, 24),
    (datetime(2019, 1, 20), 123, 2276700, 4010, Decimal('43.55'), 1, 24),
    (datetime(2019, 1, 20), 123, 2075745, 4010, Decimal('65.9'), 1, 24),
    (datetime(2019, 1, 20), 123, 25421, 4010, Decimal('33.33'), 1, 24),
    (datetime(2019, 1, 20), 123, 8754, 4010, Decimal('25.41'), 1, 24),
    (datetime(2019, 1, 20), 123, 87565442, 4010, Decimal('33.33'), 1, 24),
    (datetime(2019, 1, 20), 123, 521, 4010, Decimal('33.33'), 1, 24),
    (datetime(2019, 1, 20), 123, 4874, 4010, Decimal('66.66'), 1, 24)
]

expected['addapter_ret_ds'] = [
    (datetime(2019, 1, 20), 2106550, 2106502, 4009, 654, 6, 23),
    (datetime(2019, 1, 20), 127774, 777888999, 4009, 4, 2, 23),
    (datetime(2019, 1, 20), 123, 87452, 4009, Decimal('56.5'), 1, 23),
    (datetime(2019, 1, 20), 123, 2276700, 4009, Decimal('43.55'), 1, 23),
    (datetime(2019, 1, 20), 123, 2075745, 4009, Decimal('65.9'), 1, 23),
    (datetime(2019, 1, 20), 123, 25421, 4009, Decimal('33.33'), 1, 23),
    (datetime(2019, 1, 20), 123, 8754, 4009, Decimal('25.41'), 1, 23),
    (datetime(2019, 1, 20), 123, 87565442, 4009, Decimal('33.33'), 1, 23),
    (datetime(2019, 1, 20), 123, 521, 4009, Decimal('33.33'), 1, 23),
    (datetime(2019, 1, 20), 123, 4874, 4009, Decimal('66.66'), 1, 23)
]

expected['adfox'] = [
    (datetime(2019, 1, 20), 216135, 505170, 59665733, 46601691, 0, 0, None),
    (datetime(2019, 1, 20), 216135, 505173, 1084378, 4084, 0, 1, None),
    (datetime(2019, 1, 20), 216135, 507217, 100557, 100557, 0, 1, None),
    (datetime(2019, 1, 20), 239925, 505170, 450, 0, 0, 1, None),
    (datetime(2019, 1, 20), 239959, 505170, 9522, 735, 0, 1, None),
    (datetime(2019, 1, 20), 239970, 505170, 44438, 21502, 0, 1, None),
    (datetime(2019, 1, 20), 240126, 505170, 80885, 80885, 0, 1, None),
    (datetime(2019, 1, 20), 267148, 505170, 270949, 228848, 0, 1, None),
    (datetime(2019, 1, 20), 267747, 505170, 120604, 10672, 0, 1, None),
    (datetime(2019, 1, 20), 267747, 505178, 1, 1, 0, 1, None)
]

expected['avia_rs'] = [
    (datetime(2019, 1, 20), 10007, 2256434, 10007, 1,  1,   1429388, 0, 306,  0,  15, None, None, None),
    (datetime(2019, 1, 20), 10007, 2328170, 10007, 11, 120, 2429388, 0, None, 0,  15, None, None, None),
    (datetime(2019, 1, 20), 10007, 2256434, 10007, 3,  3,   4288165, 0, 306,  0,  15, None, None, None),
    (datetime(2019, 1, 20), 10007, 2279278, 10007, 4,  4,   5717553, 0, 100,  0,  15, None, None, None),
    (datetime(2019, 1, 20), 10007, 2262097, 10007, 1,  1,   1429388, 0, 252,  0,  15, None, None, None),
    (datetime(2019, 1, 20), 10007, 2328169, 10007, 1,  1,   1429388, 0, 637,  0,  15, None, None, None),
    (datetime(2019, 1, 20), 10007, 2323977, 10007, 1,  1,   1429388, 0, 671,  0,  15, None, None, None),
    (datetime(2019, 1, 20), 10007, 2323977, 10007, 1,  1,   1429388, 0, 673,  0,  15, None, None, None),
]

expected['avia_product_completions'] = Data(
    [                                           # BO.T_PARTNER_PRODUCT_COMPLETION
        (
            DT,                                 # dt
            508972,                             # product_id
            422163,                             # contract_id
            45043603,                           # client_id
            0,                                  # service_order_id
            114,                                # service_id
            2,                                  # source_id
            None,                               # qty
            Decimal('10.5'),                    # amount
            None,                               # id
            None,                               # currency_chr
            None,                               # commission_sum
            None,                               # promocode_sum
            None,                               # transaction_id
            None,                               # orig_transaction_id
            None,                               # service_order_id_str
            None,                               # payment_type
            None,                               # type
            None,                               # use_discount
        ),
        (DT, 508972, 295936, 34879676, 0, 114, 2, None, Decimal('4.5'), None, None,
         None, None, None, None, None, None, None, None),
        (DT, 508967, 349525, 9096976, 0, 114, 2, None, Decimal('0.708015'), None, None,
         None, None, None, None, None, None, None, None)
    ],
    ignore_fields=('id', ))

expected['travel'] = Data([
    (None, 641, 92, None, datetime(2018, 10, 12, 1, 13, 17), datetime(2018, 10, 12, 1, 13, 17),
     'cost', 'payment', 1234567, 'USD', Decimal('355.29612178'),
     None, None, None, None, datetime(2018, 10, 12, 1, 13, 17), None, 7654321, None, 'YA-0000-0000-0003',
     'trust_pmt_3', None),
    (None, 641, 93, None, datetime(2018, 10, 12, 1, 13, 17), datetime(2018, 10, 12, 1, 13, 17),
     'reward', 'payment', 1234567, 'USD', Decimal('28.70387822'),
     None, None, None, None, datetime(2018, 10, 12, 1, 13, 17), None, 7654321, None, 'YA-0000-0000-0003',
     'trust_pmt_3', None),
    (None, 716, 94, None, datetime(2018, 10, 12, 1, 13, 17), datetime(2018, 10, 12, 1, 13, 17),
     'cost', 'payment', 1234567, 'USD', Decimal('355.29612178'),
     None, None, None, None, datetime(2018, 10, 12, 1, 13, 17), None, 7654321, None, 'YA-0000-0000-0003',
     'trust_pmt_3', None),
    (None, 716, 95, None, datetime(2018, 10, 12, 1, 13, 17), datetime(2018, 10, 12, 1, 13, 17),
     'reward', 'payment', 1234567, 'USD', Decimal('28.70387822'),
     None, None, None, None, datetime(2018, 10, 12, 1, 13, 17), None, 7654321, None, 'YA-0000-0000-0003',
     'trust_pmt_3', None),
    (None, 716, 96, None, datetime(2018, 10, 12, 1, 13, 17), datetime(2018, 10, 12, 1, 13, 17),
     'reward', 'refund', 1234567, 'USD', Decimal('355.29612178'),
     None, None, None, None, datetime(2018, 10, 12, 1, 13, 17), None, 7654321, None, 'YA-0000-0000-0003',
     'trust_pmt_3', None),
    (None, 716, 97, None, datetime(2018, 10, 12, 1, 13, 17), datetime(2018, 10, 12, 1, 13, 17),
     'reward', 'refund', 1234567, 'USD', Decimal('28.70387822'),
     None, None, None, None, datetime(2018, 10, 12, 1, 13, 17), None, 7654321, None, 'YA-0000-0000-0003',
     'trust_pmt_3', None),
], ignore_fields=('id', ))


expected['practicum_spendable'] = Data([
    (None, 1041, 1, None, datetime(2021, 3, 17, 10, 26, 51), datetime(2021, 3, 17, 10, 26, 51), 'practicum_flow_1',
     'payment', 100, 'RUB', 20000, None, None, None, '{}', None, None, None, None, '1', None, None),
    (None, 1041, 2, None, datetime(2021, 3, 17, 10, 26, 51), datetime(2021, 3, 17, 10, 26, 51), 'practicum_flow_1',
     'payment', 100, 'RUB', 20000, None, None, None, '{}', None, None, None, None, '1', None, None),
    (None, 1041, 3, None, datetime(2021, 3, 17, 10, 26, 51), datetime(2021, 3, 17, 10, 26, 51), 'practicum_flow_1',
     'refund', 100, 'RUB', 20000, None, None, None, '{}', None, None, None, None, '1', None, None),
    (None, 1041, 4, None, datetime(2021, 3, 17, 10, 26, 51), datetime(2021, 3, 17, 10, 26, 51), 'practicum_flow_1',
     'refund', 100, 'RUB', 20000, None, None, None, '{}', None, None, None, None, '1', None, None),
], ignore_fields=('id', ))

expected['zaxi_selfemployed_spendable'] = Data([
    (None, 1120, 1, None, datetime(2021, 3, 17, 10, 26, 51), datetime(2021, 3, 17, 10, 26, 51), 'zaxi',
     'payment', 100, 'RUB', 20000, None, None, None, '{}', None, None, None, None, '1', None, None),
    (None, 1120, 2, None, datetime(2021, 3, 17, 10, 26, 51), datetime(2021, 3, 17, 10, 26, 51), 'zaxi',
     'payment', 100, 'RUB', 20000, None, None, None, '{}', None, None, None, None, '1', None, None),
    (None, 1120, 3, None, datetime(2021, 3, 17, 10, 26, 51), datetime(2021, 3, 17, 10, 26, 51), 'zaxi',
     'refund', 100, 'RUB', 20000, None, None, None, '{}', None, None, None, None, '1', None, None),
    (None, 1120, 4, None, datetime(2021, 3, 17, 10, 26, 51), datetime(2021, 3, 17, 10, 26, 51), 'zaxi',
     'refund', 100, 'RUB', 20000, None, None, None, '{}', None, None, None, None, '1', None, None),
], ignore_fields=('id', ))

expected['api_market'] = [
    (139092, 2070, datetime(2019, 1, 20), 0, 577461, 0, 0, 0, 0, 0, 5, None, None, None, None, None, None),
    (151198, 2070, datetime(2019, 1, 20), 0, 67293, 66, Decimal('5.45'), 545, 0, 0, 5, None, None, None, None, None, None),
    (2294597, 2070, datetime(2019, 1, 20), 0, 15, 0, 0, 0, 0, 0, 5, None, None, None, None, None, None),
    (141487, 2070, datetime(2019, 1, 20), 0, 873, 1, Decimal('1.52'), 152, 0, 0, 5, None, None, None, None, None, None),
    (2289306, 2070, datetime(2019, 1, 20), 0, 5779, 0, 0, 0, 0, 0, 5, None, None, None, None, None, None),
    (152593, 2070, datetime(2019, 1, 20), 0, 182, 0, 0, 0, 0, 0, 5, None, None, None, None, None, None),
    (152543, 2070, datetime(2019, 1, 20), 0, 2135554, 36, Decimal('12.21'), 1221, 0, 0, 5, None, None, None, None, None, None),
    (219512, 2070, datetime(2019, 1, 20), 0, 5233, 5, Decimal('1.3'), 130, 0, 0, 5, None, None, None, None, None, None),
    (170108, 2070, datetime(2019, 1, 20), 0, 4520291, 180, Decimal('52.09'), 5209, 0, 0, 5, None, None, None, None, None, None),
    (2291287, 2070, datetime(2019, 1, 20), 0, 120915, 0, 0, 0, 0, 0, 5, None, None, None, None, None, None)
]

expected['bk'] = [
    (99758, 542, datetime(2019, 1, 20), 0, 2164, 7, Decimal('2.9028'), 2902800, 1, 1651, 1, None, None, None, None, None, None),
    (99758, 542, datetime(2019, 1, 20), 0, 56, 0, 0, 0, 6, 1651, 1, None, None, None, None, None, None),
    (99758, 542, datetime(2019, 1, 20), 0, 1, 0, 0, 0, 9, 1651, 1, None, None, None, None, None, None),
    (90114, 542, datetime(2019, 1, 20), 0, 654, 0, 0, 0, 1, 395, 1, None, None, None, None, None, None),
    (90114, 542, datetime(2019, 1, 20), 0, 17, 0, 0, 0, 6, 395, 1, None, None, None, None, None, None),
    (90122, 542, datetime(2019, 1, 20), 0, 129, 4, Decimal('1.8959'), 1895866, 1, 38, 1, None, None, None, None, None, None),
    (90122, 542, datetime(2019, 1, 20), 0, 1, 0, 0, 0, 6, 38, 1, None, None, None, None, None, None),
    (186038, 542, datetime(2019, 1, 20), 0, 61, 1, Decimal('0.3107'), 310733, 6, 1182, 1, None, None, None, None, None, None),
    (186038, 542, datetime(2019, 1, 20), 0, 2, 0, 0, 0, 9, 1182, 1, None, None, None, None, None, None),
    (186095, 542, datetime(2019, 1, 20), 0, 6925, 44, Decimal('17.1317'), 17131733, 1, 2471, 1, None, None, None, None, None, None)
]

expected['blue_market'] = Data([
    (datetime(2019, 1, 20), 1, None, 11, 0, 612, None, None, Decimal('134.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 2, None, 11, 0, 612, None, None, Decimal('1.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 1, None, 12, 0, 612, None, None, Decimal('0.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 3, None, 13, 0, 612, None, None, 1000, None, None, None, None, None, None, None, None, None, None),
], ignore_fields=('id', ))

expected['red_market'] = Data([
    (datetime(2019, 1, 20), 1, None, 11, 0, 618, None, None, Decimal('134.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 2, None, 11, 0, 618, None, None, Decimal('1.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 1, None, 12, 0, 618, None, None, Decimal('0.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 3, None, 13, 0, 618, None, None, Decimal('1000.001'), None, None, None, None, None, None, None, None, None, None),
], ignore_fields=('id', ))

expected['purple_market'] = Data([
    (datetime(2019, 1, 20), 1, None, 11, 0, 665, None, None, Decimal('134.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 2, None, 11, 0, 665, None, None, Decimal('1.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 1, None, 12, 0, 665, None, None, Decimal('0.55'), None, None, None, None, None, None, None, None, None, None),
    (datetime(2019, 1, 20), 3, None, 13, 0, 665, None, None, Decimal('1000.001'), None, None, None, None, None, None, None, None, None, None),
], ignore_fields=('id', ))

expected['boyscouts'] = PreparedData([
     {'client_id': 42706149,
      'currency': 'RUB',
      'dt': map_dt['boyscouts'],
      'transaction_dt': map_dt['boyscouts'],
      'extra_dt_0': None,
      'extra_num_0': None,
      'extra_num_1': None,
      'extra_num_2': None,
      'extra_str_0': None,
      'extra_str_1': None,
      'extra_str_2': None,
      'id': None,
      'orig_transaction_id': None,
      'payload': '{"db"="d1266dc305844c4291807cf1d2c18085";"scout_id"="yantonenkornduber";"scout_name"="\\u0430\\u043d\\u0442\\u043e\\u043d\\u0435\\u043d\\u043a\\u043e \\u044f\\u0440\\u043e\\u0441\\u043b\\u0430\\u0432uber";"uuid"="fdb7d93651abde7102341305bfee45d7"}',
      'payment_type': 'scout',
      'payout_ready_dt': None,
      'paysys_partner_id': None,
      'paysys_type_cc': None,
      'price': 1500,
      'service_id': 619,
      'transaction_id': 709149952160529664484516766165322410L,
      'transaction_type': 'payment'}],
    ignore_fields=('id', ))

expected['cloud'] = [
    (datetime(2019, 1, 20), 669224, 508563, Decimal('8548.156'), None),
    (datetime(2019, 1, 20), 669224, 508563, 46289, None)
]

expected['hosting_service'] = [
    (datetime(2019, 1, 20), 669224, 508563, Decimal('8548.156'), None),
    (datetime(2019, 1, 20), 669224, 508563, 46289, None)
]

expected['d_installs'] = [
    (1996812, 10001, datetime(2019, 1, 20), 0, 12, 0, 0, 0, 0, 0, 4, None, None, None, None, None, None),
    (2157766, 10001, datetime(2019, 1, 20), 0, 7, 0, 0, 0, 0, 0, 4, None, None, None, None, 393, None),
    (2157766, 10001, datetime(2019, 1, 20), 0, 736, 0, 0, 0, 0, 0, 4, None, None, None, None, 392, None),
    (2157766, 10001, datetime(2019, 1, 20), 0, 1, 0, 0, 0, 0, 0, 4, None, None, None, None, 390, None),
    (2157766, 10001, datetime(2019, 1, 20), 0, 5, 0, 0, 0, 0, 0, 4, None, None, None, None, 388, None),
    (2271202, 10001, datetime(2019, 1, 20), 0, 1, 0, 0, 0, 0, 0, 4, None, None, None, None, None, None),
    (2224484, 10001, datetime(2019, 1, 20), 0, 1, 0, 0, 0, 0, 0, 4, None, None, None, None, 3, None),
    (2224484, 10001, datetime(2019, 1, 20), 0, 1, 0, 0, 0, 0, 0, 4, None, None, None, None, 2, None)
]

expected['dsp'] = [
    (datetime(2019, 1, 20), 128934, 2, 2563049, 1, 0, 0, 0, 12, 0, 0, 0, 0, 2),
    (datetime(2019, 1, 20), 112158, 2, 2563113, 0, 0, 0, 0, 24, 0, 0, 0, 0, 0),
    (datetime(2019, 1, 20), 185291, 4, 2563081, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0),
    (datetime(2019, 1, 20), 188382, 227, 1, 1488, Decimal('34.25759'), Decimal('16.272435'), 610, 1488, Decimal('40.427646'), 0, 0, 0, 100003014),
    (datetime(2019, 1, 20), 142545, 104, 2563049, 71, Decimal('26.946'), Decimal('13.473'), 27, 73, Decimal('39.563'), 0, 0, 0, 499000),
    (datetime(2019, 1, 20), 189903, 87, 1, 155, Decimal('8.5081'), Decimal('4.041357'), 70, 2850, Decimal('10.039976'), 0, 0, 0, 100001439),
    (datetime(2019, 1, 20), 188382, 191, 10, 4544, 0, 0, 0, 4544, 0, 0, 0, 0, 100003434),
    (datetime(2019, 1, 20), 208444, 1, 2563049, 0, 0, 0, 0, 1, 0, 0, 0, 0, 13565962),
    (datetime(2019, 1, 20), 192531, 2, 2563081, 0, 0, 0, 0, 54, 0, 0, 0, 0, 0),
    (datetime(2019, 1, 20), 224669, 5, 2563081, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0)
]

expected['health'] = [
    (datetime(2019, 1, 20), 2559, 15919809, 500, None)
]

expected['multiship'] = [
    (94, 100002237, 666, datetime(2019, 1, 20), 504716, 295, 0),
    (95, 100002238, 666, datetime(2019, 1, 20), 504716, 25, 1)
]

expected['rs_market'] = [
    (datetime(2019, 1, 20), 10003, 2297463, 10003, 0, 0, 0, 0, 225, 0, 13, None, None, 1),
    (datetime(2019, 1, 20), 10003, 1985209, 10003, 0, 0, 0, 0, 105, 0, 13, None, None, 1),
    (datetime(2019, 1, 20), 10003, 1787308, 10003, 58886, 119, 35250000, 0, None, 0, 13, None, None, 788),
    (datetime(2019, 1, 20), 10003, 2051477, 10003, 0, 0, 0, 0, None, 0, 13, None, None, 2),
    (datetime(2019, 1, 20), 10003, 2175745, 10003, 0, 0, 0, 0, None, 0, 13, None, None, 8),
    (datetime(2019, 1, 20), 10003, 2282513, 10003, 0, 0, 0, 0, None, 0, 13, None, None, 4),
    (datetime(2019, 1, 20), 10003, 459909, 10003, 38, 0, 0, 0, None, 0, 13, None, None, 0),
    (datetime(2019, 1, 20), 10003, 2210393, 10003, 260102, 495, 187370000, 0, None, 0, 13, None, None, 196),
    (datetime(2019, 1, 20), 10003, 2291166, 10003, 444, 0, 0, 0, None, 0, 13, None, None, 4),
    (datetime(2019, 1, 20), 10003, 2136398, 10003, 14079, 34, 11820000, 0, None, 0, 13, None, None, 46)
]

expected['rs_market_cpa'] = [
    (datetime(2019, 1, 20), 10004, 2256021, 10004, 0, 36, 0, 0, None, 0, 14, 0, None, None),
    (datetime(2019, 1, 20), 10004, 2270534, 10004, 0, 20, 0, 0, None, 0, 14, 0, None, None),
    (datetime(2019, 1, 20), 10004, 2285121, 10004, 0, 1, 0, 0, None, 0, 14, 0, None, None),
    (datetime(2019, 1, 20), 10004, 2295143, 10004, 0, 1, 0, 0, None, 0, 14, 0, None, None),
    (datetime(2019, 1, 20), 10004, 2271525, 10004, 0, 2297, 73258200, 0, None, 0, 14, 6, None, None),
    (datetime(2019, 1, 20), 10004, 2277535, 10004, 0, 0, 13120125, 0, None, 0, 14, 2, None, None),
    (datetime(2019, 1, 20), 10004, 2290309, 10004, 0, 1, 0, 0, 225, 0, 14, 0, None, None),
    (datetime(2019, 1, 20), 10004, 1955451, 10004, 0, 1666, Decimal('10990391.67'), 0, None, 0, 14, 2, None, None),
    (datetime(2019, 1, 20), 10004, 2066562, 10004, 0, 1900, 16014785, 0, None, 0, 14, 2, None, None),
    (datetime(2019, 1, 20), 10004, 2039513, 10004, 0, 3546, Decimal('8760333.33'), 0, None, 0, 14, 2, None, None)
]

expected['rtb_distr'] = [
    (datetime(2019, 1, 20), 10000, 2278292, 10000, 3, 0, Decimal('3174.8'), 0, None, 0, 16, None, None, None),
    (datetime(2019, 1, 20), 10000, 2279175, 10000, 6, 0, Decimal('27100.4'), 0, None, 0, 16, None, None, None),
    (datetime(2019, 1, 20), 10000, 2266102, 10000, 3, 0, 10148, 0, None, 0, 16, None, None, None),
    (datetime(2019, 1, 20), 10000, 2281101, 10000, 20, 0, Decimal('2394.4'), 0, None, 0, 16, None, None, None),
    (datetime(2019, 1, 20), 10000, 2263185, 10000, 75, 1, 171624, 0, None, 0, 16, None, None, None),
    (datetime(2019, 1, 20), 10000, 2266536, 10000, 45584, 717, Decimal('247669293.6'), 0, None, 0, 16, None, None, None),
    (datetime(2019, 1, 20), 10000, 2278336, 10000, 26, 0, Decimal('40984.8'), 0, None, 0, 16, None, None, None),
    (datetime(2019, 1, 20), 10000, 2263191, 10000, 2198, 15, Decimal('3518871.6'), 0, None, 0, 16, None, None, None),
    (datetime(2019, 1, 20), 10000, 2267211, 10000, 21, 0, Decimal('93865.2'), 0, None, 0, 16, None, None, None)
]

expected['serphits'] = [
    (2286651, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 63, 84, 6, None, None, None, None, None, None),
    (2041437, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 227826, 135, 6, None, None, None, None, None, None),
    (2149692, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 90, 1, 6, None, None, None, None, None, None),
    (2230936, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 172351, 6, 6, None, None, None, None, None, None),
    (124995, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 227826, 13, 6, None, None, None, None, None, None),
    (2226561, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 172351, 1783, 6, None, None, None, None, None, None),
    (2084462, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 172353, 26, 6, None, None, None, None, None, None),
    (2261875, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 129, 4, 6, None, None, None, None, None, None),
    (1969470, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 63, 7, 6, None, None, None, None, None, None),
    (2257594, 100005, datetime(2019, 1, 20), 0, 0, 0, 0, 0, 243663, 36, 6, None, None, None, None, None, None)
]

expected['tags3'] = [
    (datetime(2019, 1, 20), 93572, 1073890801, 542, 2, 0, 0, 1, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 93572, 1073890803, 542, 2, 1, 141600, 1, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 93572, 1073890812, 542, 8, 0, 0, 1, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 5597, 1086570737, 542, 36, 1, 114067, 1, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 5597, 1086570737, 542, 2, 0, 0, 6, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 5597, 1086570739, 542, 95, 2, 1425734, 1, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 5597, 1086570739, 542, 3, 0, 0, 6, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 141078, 1088897027, 542, 4, 0, 0, 1, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 141078, 1088897028, 542, 1, 0, 0, 1, None, 2, 11, None, None, None),
    (datetime(2019, 1, 20), 141078, 1088897036, 542, 3, 1, 125867, 1, None, 2, 11, None, None, None)
]

expected['taxi_aggr'] = [
    (datetime(2019, 1, 20), 34901168, 'RUB', 'cash', Decimal('4338.84'), 0, 'order', 0),
    (datetime(2019, 1, 20), 34901168, 'RUB', 'corporate', Decimal('39.5'), 0, 'order', 0),
    (datetime(2019, 1, 20), 34905406, 'RUB', 'card', Decimal('64.3'), 0, 'order', 0),
    (datetime(2019, 1, 20), 37193959, 'RUB', 'card', Decimal('1022.2'), 0, 'order', 0),
    (datetime(2019, 1, 20), 37193959, 'RUB', 'cash', Decimal('3426.8'), 0, 'order', 0),
    (datetime(2019, 1, 20), 37193959, 'RUB', 'corporate', Decimal('37.5'), 0, 'order', 0),
    (datetime(2019, 1, 20), 37194208, 'RUB', 'card', Decimal('344.747627'), 0, 'order', 0),
    (datetime(2019, 1, 20), 38172961, 'RUB', 'card', Decimal('6429.281249'), 0, 'order', 0),
    (datetime(2019, 1, 20), 38172961, 'RUB', 'cash', Decimal('6068.910057'), 0, 'order', 0),
    (datetime(2019, 1, 20), 38172961, 'RUB', 'corporate', Decimal('309.006561'), 0, 'order', 0),
    (datetime(2019, 1, 20), 39123321, 'USD', 'cash', Decimal('509.006561'), Decimal('70.01'), 'subvention', Decimal('12.321')),
    (datetime(2019, 1, 20), 39123321, 'EUR', 'cash', 0, 0, 'subvention', Decimal('13.321')),
]

expected['taxi_distr'] = [
    (datetime(2019, 1, 20), 13002, 2319588, 13002, 11, None, 76076000, 0, None, None, 17, None, None, None)
]
expected['taxi_stand_svo'] = PreparedData(
    [{'client_id': 80781421,
      'currency': 'RUB',
      'dt': map_dt['taxi_stand_svo'],
      'transaction_dt': map_dt['taxi_stand_svo'],
      'extra_dt_0': datetime(2018, 7, 24, 23, 55, 25),
      'extra_num_0': None,
      'extra_num_1': 6970618519452892,
      'extra_num_2': 44997,
      'extra_str_0': 'abc3707111d24d1fdfc8fedbfae36f71aa3def',
      'extra_str_1': 'def4d7029329d9fa85d9cc93f58e2f715e7abc',
      'extra_str_2': None,
      'id': None,
      'orig_transaction_id': None,
      'payload': None,
      'payment_type': 'taxi_stand_svo',
      'payout_ready_dt': None,
      'paysys_partner_id': None,
      'paysys_type_cc': None,
      'price': 1234,
      'service_id': 626,
      'transaction_id': 100119754314295768443810525125500L,
      'transaction_type': 'payment'}],
    ignore_fields=('id', 'partner_payment_registry_id', 'payout_ready_dt'))
expected['taxi_medium'] = [
    (datetime(2019, 1, 20), 10002, 2040463, 10002, 3522, None, 2300402857, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2046579, 10002, 1319, None, 711366497, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2046818, 10002, 23, None, 4683405, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2058507, 10002, 1, None, 842541, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2059749, 10002, 236, None, 100111212, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2061400, 10002, 4, None, 4807665, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2104423, 10002, 3, None, 277805, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2120168, 10002, 2, None, 749922, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2190366, 10002, 1150, None, 526806281, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2221170, 10002, 2, None, 658667, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2222875, 10002, 3, None, 7742447, 0, None, None, 12, None, None, None),
    (datetime(2019, 1, 20), 10002, 2228586, 10002, 199, None, 76750744, 0, None, None, 12, None, None, None)
]

expected['video_distr'] = [
    (datetime(2019, 1, 20), 13003, 2242347, 13003, 0, 0, 120, 0, None, 0, 18, None, None, None),
    (datetime(2019, 1, 20), 13003, 2321787, 13003, 0, 0, Decimal('25707.6'), 0, None, 0, 18, None, None, None)
]
expected['toloka'] = PreparedData([
     {'client_id': 123,
      'currency': 'RUB',
      'dt': map_dt['toloka'],
      'transaction_dt': map_dt['toloka'],
      'extra_dt_0': None,
      'extra_num_0': None,
      'extra_num_1': None,
      'extra_num_2': None,
      'extra_str_0': None,
      'extra_str_1': None,
      'extra_str_2': None,
      'id': None,
      'orig_transaction_id': None,
      'payload': None,
      'payment_type': 'wallet',
      'payout_ready_dt': None,
      'paysys_partner_id': None,
      'paysys_type_cc': None,
      'price': 1.0,
      'service_id': 42,
      'transaction_id': 535232,
      'transaction_type': 'payment'}],
    ignore_fields=('id', )
)
expected['zen'] = PreparedData([
     {'client_id': 123,
      'currency': 'RUB',
      'dt': map_dt['zen'],
      'transaction_dt': map_dt['zen'],
      'extra_dt_0': None,
      'extra_num_0': None,
      'extra_num_1': None,
      'extra_num_2': None,
      'extra_str_0': None,
      'extra_str_1': None,
      'extra_str_2': None,
      'id': None,
      'orig_transaction_id': None,
      'payload': None,
      'payment_type': 'wallet',
      'payout_ready_dt': None,
      'paysys_partner_id': None,
      'paysys_type_cc': None,
      'price': 1.0,
      'service_id': 134,
      'transaction_id': 535232,
      'transaction_type': 'payment'}],
    ignore_fields=('id', ))


expected['mediaservice_subagency_payment'] = PreparedData([
    {
        'client_id': 44,
        'currency': 'RUB',
        'dt': map_dt['mediaservice_subagency_payment'],
        'transaction_dt': map_dt['mediaservice_subagency_payment'],
        'extra_dt_0': None,
        'extra_num_0': None,
        'extra_num_1': None,
        'extra_num_2': None,
        'extra_str_0': '3',
        'extra_str_1': '29058',
        'extra_str_2': None,
        'id': 1002156931,
        'orig_transaction_id': 10315974,
        'payload': '{\"systemName\":\"testing1\"}',
        'payment_type': 'card',
        'payout_ready_dt': None,
        'paysys_partner_id': None,
        'paysys_type_cc': None,
        'price': Decimal('100'),
        'service_id': 325,
        'transaction_id': 10315974,
        'transaction_type': 'payment',
    },
    {
        'client_id': 44,
        'currency': 'RUB',
        'dt': map_dt['mediaservice_subagency_payment'],
        'transaction_dt': map_dt['mediaservice_subagency_payment'],
        'extra_dt_0': None,
        'extra_num_0': None,
        'extra_num_1': None,
        'extra_num_2': None,
        'extra_str_0': '3',
        'extra_str_1': '29058',
        'extra_str_2': None,
        'id': 1002156932,
        'orig_transaction_id': 10315974,
        'payload': '{\"systemName\":\"testing2\"}',
        'payment_type': 'card',
        'payout_ready_dt': None,
        'paysys_partner_id': None,
        'paysys_type_cc': None,
        'price': Decimal('100'),
        'service_id': 325,
        'transaction_id': 10316074,
        'transaction_type': 'refund',
    },
    {
        'client_id': 3,
        'currency': 'RUB',
        'dt': map_dt['mediaservice_subagency_payment'],
        'transaction_dt': map_dt['mediaservice_subagency_payment'],
        'extra_dt_0': None,
        'extra_num_0': None,
        'extra_num_1': None,
        'extra_num_2': None,
        'extra_str_0': None,
        'extra_str_1': '29086',
        'extra_str_2': None,
        'id': 1002156933,
        'orig_transaction_id': 10318424,
        'payload': '{\"systemName\":\"testing3\"}',
        'payment_type': 'card',
        'payout_ready_dt': None,
        'paysys_partner_id': None,
        'paysys_type_cc': None,
        'price': Decimal('1500'),
        'service_id': 326,
        'transaction_id': 10318424,
        'transaction_type': 'payment',
    },
    {
        'client_id': 3,
        'currency': 'RUB',
        'dt': map_dt['mediaservice_subagency_payment'],
        'transaction_dt': map_dt['mediaservice_subagency_payment'],
        'extra_dt_0': None,
        'extra_num_0': None,
        'extra_num_1': None,
        'extra_num_2': None,
        'extra_str_0': None,
        'extra_str_1': '29086',
        'extra_str_2': None,
        'id': 1002156934,
        'orig_transaction_id': 10318424,
        'payload': '{\"systemName\":\"testing4\"}',
        'payment_type': 'card',
        'payout_ready_dt': None,
        'paysys_partner_id': None,
        'paysys_type_cc': None,
        'price': Decimal('1500'),
        'service_id': 326,
        'transaction_id': 10318434,
        'transaction_type': 'refund',
    },
], ignore_fields=('id', ))


expected['drive_fueling'] = PreparedData(
    [{'client_id': 666666,
      'currency': 'RUB',
      'dt': map_dt['drive_fueling'],
      'extra_dt_0': None,
      'extra_num_0': None,
      'extra_num_1': None,
      'extra_num_2': None,
      'extra_str_0': None,
      'extra_str_1': None,
      'extra_str_2': None,
      'id': 1002156931,
      'orig_transaction_id': None,
      'payload': 'fake',
      'payment_type': 'drive_fueler',
      'payout_ready_dt': None,
      'paysys_partner_id': None,
      'paysys_type_cc': None,
      'price': Decimal('81.4'),
      'service_id': 644,
      'transaction_dt': map_dt['drive_fueling'],
      'transaction_id': 51202658330991905595474923094537872723L,
      'transaction_type': 'payment'}],
    ignore_fields=('id', ))

expected['drive_penalty'] = PreparedData(
    [{'client_id': 12345,
      'currency': 'RUB',
      'dt': map_dt['drive_penalty'],
      'transaction_dt': map_dt['drive_penalty'],
      'extra_dt_0': None,
      'extra_num_0': None,
      'extra_num_1': None,
      'extra_num_2': None,
      'extra_str_0': None,
      'extra_str_1': None,
      'extra_str_2': None,
      'id': 1002156935,
      'orig_transaction_id': 300119754314295768443810525125500L,
      'payload': None,
      'payment_type': 'card',
      'payout_ready_dt': None,
      'paysys_partner_id': None,
      'paysys_type_cc': None,
      'price': Decimal('200'),
      'service_id': 643,
      'transaction_id': 29141573281821086610534094504994423L,
      'transaction_type': None}],
    ignore_fields=('id', ))

expected['food_payment'] = PreparedData(
    [   # BO.T_PARTNER_PAYMENT_STAT
        {
            'price': Decimal("13.37"),
            'dt': map_dt["food_payment"],
            'transaction_dt': map_dt["food_payment"],
            'client_id': 13370,
            'transaction_id': 1337,
            'currency': "RUB",
            'payment_type': "card",
            'service_id': 629,
            'transaction_type': "payment",
            'payload': "[]",
            'id': None,
            'extra_num_0': None,
            'extra_num_1': None,
            'extra_num_2': None,
            'extra_str_0': "goods",
            'extra_str_1': "Ordnung muss sein",
            'extra_dt_0': None,
            'payout_ready_dt': None,
            'orig_transaction_id': None,
            'extra_str_2': "1337",
            'paysys_type_cc': "payture",
            'paysys_partner_id': None,
        },
        {
            'price': Decimal("13.37"),
            'dt': map_dt["food_payment"],
            'transaction_dt': map_dt["food_payment"],
            'client_id': 13370,
            'transaction_id': 1337,
            'currency': "RUB",
            'payment_type': "card",
            'service_id': 662,
            'transaction_type': "payment",
            'payload': "[]",
            'id': None,
            'extra_num_0': None,
            'extra_num_1': None,
            'extra_num_2': None,
            'extra_str_0': "goods",
            'extra_str_1': "Ordnung muss sein",
            'extra_dt_0': None,
            'payout_ready_dt': None,
            'orig_transaction_id': None,
            'extra_str_2': "1337",
            'paysys_type_cc': "payture",
            'paysys_partner_id': None,
        },
{
            'price': Decimal("13.37"),
            'dt': map_dt["food_payment"],
            'transaction_dt': map_dt["food_payment"],
            'client_id': 13370,
            'transaction_id': 1337,
            'currency': "RUB",
            'payment_type': "card",
            'service_id': 676,
            'transaction_type': "payment",
            'payload': "[]",
            'id': None,
            'extra_num_0': None,
            'extra_num_1': None,
            'extra_num_2': None,
            'extra_str_0': "goods",
            'extra_str_1': "Ordnung muss sein",
            'extra_dt_0': None,
            'payout_ready_dt': None,
            'orig_transaction_id': None,
            'extra_str_2': "1337",
            'paysys_type_cc': "payture",
            'paysys_partner_id': None,
        }
    ],
    ignore_fields=('id', ))


expected['food_srv'] = PreparedData(
    [  # BO.T_PARTNER_PRODUCT_COMPLETION
        {
            'dt': datetime(2019, 12, 21),
            'product_id': None,
            'contract_id': None,
            'client_id': 13370,
            'service_order_id': 0,
            'service_id': 628,
            'source_id': None,
            'qty': None,
            'amount': None,
            'id': None,
            'currency_chr': "RUB",
            'commission_sum': Decimal('1234.56'),
            'promocode_sum': None,
            'transaction_id': None,
            'orig_transaction_id': None,
            'service_order_id_str': None,
            'payment_type': None,
            'type': "goods",
            'use_discount': None,
            'transaction_dt': DT,
            'transaction_type': None,
        },
        {
            'dt': DT,
            'product_id': None,
            'contract_id': None,
            'client_id': 13371,
            'service_order_id': 0,
            'service_id': 628,
            'source_id': None,
            'qty': None,
            'amount': None,
            'id': None,
            'currency_chr': "RUB",
            'commission_sum': Decimal('1234.56'),
            'promocode_sum': None,
            'transaction_id': None,
            'orig_transaction_id': None,
            'service_order_id_str': None,
            'payment_type': None,
            'type': "goods",
            'use_discount': None,
            'transaction_dt': DT,
            'transaction_type': None,
        },
        {
            'dt': datetime(2019, 12, 21),
            'product_id': None,
            'contract_id': None,
            'client_id': 13370,
            'service_order_id': 0,
            'service_id': 661,
            'source_id': None,
            'qty': None,
            'amount': None,
            'id': None,
            'currency_chr': "RUB",
            'commission_sum': Decimal('1237.56'),
            'promocode_sum': None,
            'transaction_id': None,
            'orig_transaction_id': None,
            'service_order_id_str': None,
            'payment_type': None,
            'type': "goods",
            'use_discount': None,
            'transaction_dt': DT,
            'transaction_type': None,
        },
        {
            'dt': DT,
            'product_id': None,
            'contract_id': None,
            'client_id': 13370,
            'service_order_id': 0,
            'service_id': 661,
            'source_id': None,
            'qty': None,
            'amount': None,
            'id': None,
            'currency_chr': "RUB",
            'commission_sum': Decimal('1237.56'),
            'promocode_sum': None,
            'transaction_id': None,
            'orig_transaction_id': None,
            'service_order_id_str': None,
            'payment_type': None,
            'type': "goods",
            'use_discount': None,
            'transaction_dt': DT,
            'transaction_type': None,
        },
        {
            'dt': DT,
            'product_id': None,
            'contract_id': None,
            'client_id': 13372,
            'service_order_id': 0,
            'service_id': 675,
            'source_id': None,
            'qty': None,
            'amount': None,
            'id': None,
            'currency_chr': "RUB",
            'commission_sum': Decimal('1238.56'),
            'promocode_sum': None,
            'transaction_id': None,
            'orig_transaction_id': None,
            'service_order_id_str': None,
            'payment_type': None,
            'type': "goods",
            'use_discount': None,
            'transaction_dt': DT,
            'transaction_type': None,
        },
    ],
    ignore_fields=('id', ))

expected['cloud_referal_payment'] = PreparedData(
    [  # BO.T_PARTNER_PAYMENT_STAT
        {
            'price': Decimal("13.37"),
            'dt': map_dt["cloud_referal_payment"],
            'transaction_dt': map_dt["cloud_referal_payment"],
            'client_id': 13370,
            'transaction_id': 1337,
            'currency': "RUB",
            'payment_type': "reward",
            'service_id': 693,
            'transaction_type': "payment",
            'payload': None,
            'extra_num_0': None,
            'extra_num_1': None,
            'extra_num_2': None,
            'extra_str_0': '666',
            'extra_str_1': None,
            'extra_dt_0': None,
            'payout_ready_dt': None,
            'orig_transaction_id': None,
            'extra_str_2': '1337',
            'paysys_type_cc': "yandex",
            'paysys_partner_id': None,
        },
    ],
    ignore_fields=('id', )
)

expected['news_payment'] = PreparedData(
    [  # BO.T_PARTNER_PAYMENT_STAT
        {
            'price': Decimal("14.34"),
            'dt': map_dt["news_payment"],
            'transaction_dt': map_dt["news_payment"],
            'client_id': 14340,
            'transaction_id': 1434,
            'currency': "RUB",
            'payment_type': "news_payment",
            'service_id': 1127,
            'transaction_type': "payment",
            'payload': "trt",
            'extra_num_0': None,
            'extra_num_1': None,
            'extra_num_2': None,
            'extra_str_0': "42342",
            'extra_str_1': None,
            'extra_dt_0': None,
            'payout_ready_dt': None,
            'orig_transaction_id': 1434,
            'extra_str_2': None,
            'paysys_type_cc': None,
            'paysys_partner_id': None,
        },
    ],
    ignore_fields=('id', )
)

expected['drive_corp'] = PreparedData(
    [  # BO.T_PARTNER_PRODUCT_COMPLETION
        {
            'amount': Decimal('85.54'),
            'dt': map_dt["drive_corp"],
            'transaction_dt': map_dt["drive_corp"],
            'client_id': 1,
            'transaction_id': 82369113211711162657783526889883161270,
            'currency_chr': "RUB",
            'payment_type': "wallet",
            'service_id': 702,
            'transaction_type': "payment",
            'service_order_id_str': "ec37bb4d-5b862644-171d24b6-6208693",
            'type': 'carsharing',
            'orig_transaction_id': None,
            'use_discount': None,
            'qty': None,
            'service_order_id': 0,
            'contract_id': None,
            'product_id': None,
            'promocode_sum': None,
            'source_id': None,
            'commission_sum': None,
        },
    ],
    ignore_fields=('id', )
)

expected['oebs_compls'] = PreparedData(
    [ # BO.T_PARTNER_OEBS_COMPLETIONS
        {
             u"client_id": 123,
             u"service_id": 111,
             u"accounting_period": datetime(2020, 6, 1),
             u"last_transaction_id": 888999,
             u"product_id": 54321,
             u"amount_nds": Decimal('-8.33'),
             u"contract_id": 54345,
             u"amount_wo_nds": Decimal('-41.67'),
             u"dt": datetime(2020, 6, 29),
             u"currency": "RUB",
             u"source_tabname": "2020-11-01",
             u"amount": Decimal('-50.01'),
             u"transaction_dt": DT,
             u"source_id": "taxi_default"
        },
        {
            u"client_id": 1234,
            u"service_id": 128,
            u"accounting_period": None,
            u"last_transaction_id": None,
            u"product_id": 54321,
            u"amount_nds": Decimal("8.33"),
            u"contract_id": 54345,
            u"amount_wo_nds": Decimal("41.67"),
            u"dt": datetime(2020, 6, 30),
            u"currency": "RUB",
            u"source_tabname": "2020-11-01",
            u"amount": Decimal("50.01"),
            u"transaction_dt": DT,
            u"source_id": "taxi_default"
        },
    ]
)

expected['disk_b2b'] = PreparedData(
    [  # BO.T_PARTNER_PRODUCT_COMPLETION
        {
            'amount': Decimal('85.54'),
            'dt': map_dt["disk_b2b"],
            'transaction_dt': map_dt["disk_b2b"],
            'client_id': 1,
            'currency_chr': "RUB",
            'payment_type': None,
            'service_id': 671,
            'transaction_type': "payment",
            'service_order_id_str': "090c3157-f8bc-41a4-a7a6-cd597a2db974",
            'type': 'disk_b2b',
            'transaction_id': None,
            'orig_transaction_id': None,
            'use_discount': None,
            'qty': None,
            'service_order_id': 0,
            'contract_id': None,
            'product_id': None,
            'promocode_sum': None,
            'source_id': None,
            'commission_sum': None,
        },
        {
            'amount': Decimal('85.55'),
            'dt': map_dt["disk_b2b"],
            'transaction_dt': map_dt["disk_b2b"],
            'client_id': 1,
            'currency_chr': "RUB",
            'payment_type': None,
            'service_id': 671,
            'transaction_type': "payment",
            'service_order_id_str': "090c3157-f8bc-41a4-a7a6-cd597a2db975",
            'type': 'disk_b2b',
            'transaction_id': None,
            'orig_transaction_id': None,
            'use_discount': None,
            'qty': None,
            'service_order_id': 0,
            'contract_id': None,
            'product_id': None,
            'promocode_sum': None,
            'source_id': None,
            'commission_sum': None,
        },
        {
            'amount': Decimal('85.56'),
            'dt': map_dt["disk_b2b"],
            'transaction_dt': map_dt["disk_b2b"],
            'client_id': 1,
            'currency_chr': "RUB",
            'payment_type': None,
            'service_id': 671,
            'transaction_type': "refund",
            'service_order_id_str': "090c3157-f8bc-41a4-a7a6-cd597a2db976",
            'type': 'disk_b2b',
            'transaction_id': None,
            'orig_transaction_id': None,
            'use_discount': None,
            'qty': None,
            'service_order_id': 0,
            'contract_id': None,
            'product_id': None,
            'promocode_sum': None,
            'source_id': None,
            'commission_sum': None,
        },
    ],
    ignore_fields=('id',)
)


class HasNoTestData(Exception):
    pass


class Rollback(Exception):
    pass


def prepare_fake_fetcher(config):
    if config.code not in fake_source:
        raise HasNoTestData('Has no test data for %s' % config.code)

    class FakeFetcher(object):
        def __init__(self, *args, **kwargs):
            pass

        @staticmethod
        def get_dynamics_params():
            return None

        @staticmethod
        def process(*args, **kwargs):
            fs = fake_source[config.code]
            convert_to_strio = getattr(fs, 'convert_to_strio', True)
            return StringIO(fs) if convert_to_strio else copy.deepcopy(fs.data)

    BaseFetcher.children[config.fetcher['type']] = FakeFetcher


def prepare_env(config, session):
    if config.code in prepare_context:
        prepare_config = prepare_context[config.code]
        prepare_config['action'](**prepare_config['params'](session, config.code))
    prepare_fake_fetcher(config)


def get_inserted_rows(config, session, dt):
    suffix = (' and source_id=%s' % config.src_id) if config.src_id else ''
    service_id = config.cleaner and config.cleaner.get('deletion_selector') \
        and config.cleaner['deletion_selector'].get('service_id')
    if service_id:
        service_ids = ut.ensure_iterable(service_id)
        suffix += ' and service_id in ({})'.format(', '.join(map(str, service_ids)))
    table = config.tables['table']
    condition = source_conditions.get(config.code)
    condition_sql = condition['sql'] if condition and condition.get('sql') else 'dt = :dt'
    params = condition['params'] if condition and condition.get('params') else {'dt': dt}
    sql = (' select * from {table} where {condition} '.format(table=table,
                                                              condition=condition_sql)) + suffix

    e = session.execute(sql, params)
    return e.keys(), e.fetchall()


def check_completions(source, actual, expected, keys, tuple_res):
    hm.assert_that(actual, hm.has_length(len(set(actual))),
                   "Duplicate rows inserts in db for %s" % source)

    if isinstance(expected, Data):
        ignore_fields = expected.ignore_fields
        expected_dicts = list(sorted(expected.to_dict(keys)))
        actual_dicts = list(sorted(Data(actual, ignore_fields).to_dict(keys)))
        hm.assert_that(actual_dicts, hm.has_length(len(expected_dicts)))
        for actual_dict, expected_dict in zip(actual_dicts, expected_dicts):
            hm.assert_that(actual_dict, hm.has_entries(expected_dict))
    elif isinstance(expected, PreparedData):
        actual_values = PreparedData(tuple_res, ignore_fields=expected.ignore_fields)
        hm.assert_that(expected, hm.equal_to(actual_values),
                       'Test %s failed.\nExpected:\n %s\n, Actual:\n%s\n' % (source, expected, actual_values))
    else:
        hm.assert_that(set(expected), hm.equal_to(set(actual)),
                       "Rows inserted in db differs from expected for %s: expected %s but actual is %s"
                       % (source, set(expected), set(actual)))


def get_source_codes(session, code=None):
    query = session.query(CompletionSource.code).filter(
        CompletionSource.queue == 'PARTNER_COMPL',
        ~CompletionSource.code.in_(SKIP),
        CompletionSource.enabled != CompletionSource.DISABLED
    )
    if code:
        query = query.filter(CompletionSource.code == code)
    sources = query.all()
    in_development = session.config.get('PARTNER_COMPL_IN_DEVELOPMENT', [])
    return [code for code, in sources if code not in in_development]


@mock.patch(
    'balance.completions_fetcher.configurable_partner_completion.CredentialParameter.get_credential',
    return_value='***'
)
class TestCompletions(BalanceTest):
    def test_general(self, _):
        app = self.app
        print(app.cfg_path)
        sources = get_source_codes(self.session)
        for source in sources:
            try:
                print('\033[4;33m{}\033[0m'.format(source))
                session = app.real_new_session()
                with session.begin():
                    config = CompletionConfig(code=source, queue='PARTNER_COMPL', session=session)
                    prepare_env(config, session)

                    dt = map_dt.get(config.code, DT)
                    processor = ProcessorFactory.get_instance(session, config, dt, dt)
                    processor.process()

                    # Тестовая запись в БД произведена
                    # Теперь прочитаем её результат
                    keys, tuple_res = get_inserted_rows(config, session, dt)
                    # и откатим её
                    raise Rollback
                    # А теперь сравним то, что фактически было записано, с тем, что должно быть
            except Rollback:
                expected_ = expected.get(source, [])
                expected_ = expected_() if callable(expected_) else expected_
                actual = [tuple(el) for el in tuple_res]

                print(expected_)
                print(actual)
                check_completions(source, actual, expected_, keys, tuple_res)

            except HasNoTestData as e:
                print('\033[31m{}\033[0m'.format(str(e)))
                raise

    # проверяем, что открутки типа SidePayment экспортируются в THIRDPARTY_TRANS,
    # т.к. это по сути платежи
    def test_side_payment_export(self, _):
        sources = get_source_codes(self.session)
        for source in sources:
            config = CompletionConfig(code=source, queue='PARTNER_COMPL', session=self.session)
            if config.tables['table'] != scheme_payments.side_payments.name:
                continue

            print('\033[4;33m{}\033[0m'.format(source))
            session = self.app.real_new_session()
            try:
                with session.begin():
                    prepare_env(config, session)
                    # принимаем открутки за указанную дату
                    dt = map_dt.get(source, DT)
                    processor = ProcessorFactory.get_instance(session, config, dt, dt)
                    processor.process()

                    # запрашиваем id сохранённых откруток
                    stored_ids = session.query(mapper.SidePayment.id).filter(mapper.SidePayment.dt == dt).all()
                    stored_ids = {id_ for id_, in stored_ids}
                    assert stored_ids, 'Fake source should return at least one row of data for %s' % source

                    # проверяем, что все платежи были экспортированы
                    exported = session \
                        .query(mapper.Export.object_id) \
                        .filter(mapper.Export.classname.in_(['BalalaykaPayment', 'SidePayment', 'ZenPayment']),
                                mapper.Export.object_id.in_(stored_ids),
                                mapper.Export.type == 'THIRDPARTY_TRANS').all()

                    exported = {id_ for id_, in exported}
                    print(stored_ids)
                    print(exported)
                    doesnt_exported = stored_ids - exported
                    for side_payment_id in doesnt_exported:
                        raise AssertionError('SidePayment id=%s has no export, but must to' % side_payment_id)

                    # откатываем изменения
                    raise Rollback
            except Rollback:
                pass


if __name__ == '__main__':
    import unittest
    unittest.main()
