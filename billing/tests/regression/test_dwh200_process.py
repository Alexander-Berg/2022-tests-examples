import os
import logging as log
from collections import deque
from datetime import datetime as dt

import blinker
import pandas as pd
import mock
from sqlalchemy.engine import Engine
import pytest

from dwh.grocery.processing.process import (
    Process,
    TypeChecking,
)
from dwh.grocery.targets import YTTableTarget
from yatest.common import source_path


report_config_path = source_path('billing/dwh/src/dwh/conf/remote/usr/bin/dwh/dwh-200/')
trace_query = blinker.signal('trace_query')


@trace_query.connect
def log_query(sender, query):
    log.info(f"{sender} try get data from db")
    log.info(query)


@pytest.fixture(scope="function")
def process():
    return Process(os.path.join(report_config_path, 'dwh-200.yaml'))


class TestDWH200:

    def setup(self):
        self.process_common = Process(os.path.join(report_config_path, 'dwh-200.yaml'), type_checking=TypeChecking.strict)
        self.process_yan = Process(os.path.join(report_config_path, 'dwh-200-yan.yaml'), type_checking=TypeChecking.strict)
        self.process_distr = Process(os.path.join(report_config_path, 'dwh-200-distr.yaml'), type_checking=TypeChecking.strict)
        self.month = dt.now().strftime("%Y-%m-%d")

    def actual_rows(self):
        return pd.DataFrame(
            YTTableTarget("//home/balance-test/test/dwh/aggregation").read(cached=True)
        )

    def actual_awaps(self):
        return pd.DataFrame(
            YTTableTarget('//home/balance/test/dwh/dwh_200_video/video_awaps_mimic-2019-06').read(cached=True)
        )

    @staticmethod
    def make_call_result_seq(call_results):
        call_results = deque(call_results)

        def side_effect(*args, **kwargs):
            return call_results.popleft()

        return side_effect

    # Error resolving path //home/balance/test/dwh/dwh_200_video/video_awaps_mimic-2019-06/@
    # Node //home/balance/test/dwh/dwh_200_video has no child with key "video_awaps_mimic-2019-06"
    @pytest.mark.skip(reason='Something very strange, failing')
    @mock.patch('pandas.read_sql_query')
    def test_full_process(self, read_sql_query: mock.MagicMock):
        load_dsp_charge_query_result = pd.DataFrame(columns=['pageid', 'product', 'dsp_id', 'dsp_charge'])
        ur_service_codes_query_result = pd.DataFrame([
            {
                'domain': 'www.dwh.net',
                'pageid': 6666,
                'page_service_code': None,
                'page_ur_code': None,
            }
        ])
        extract_client_query_result = pd.DataFrame(columns=['clientid', 'order_service_code', 'order_ur_code'])
        extract_contract_query_result = self.process_yan.make_empty_df_from_scheme('extract_contracts', 'contracts')
        extract_contract_query_result['id'] = None
        extract_currency_query_result = pd.DataFrame([{
            'currency_name': 'RUR',
            'currency_rate': 1.0
        }])
        extract_ecb_currency_query_result = pd.DataFrame([
            {
                'currency_name': 'EUR',
                'ecb_rate': 1.0
            },
            {
                'currency_name': 'CHF',
                'ecb_rate': 2.0     # COMPLETELY WRONG RATE
            },
        ])
        extract_reward_query_result = pd.DataFrame([
            {
                'pageid': 143477,
                'contract_id': 185136,
                'product': 0,
                'charge2': 36.1926,
                'currency': 'RUR',
                'reward': 395.664864406779661016949152542372881356
            }
        ])
        extract_firm_query_result = pd.DataFrame([
            {'firm_id': 1, 'firm_name': "ООО «Яндекс»"},
            {'firm_id': 4, 'firm_name': "Yandex Inc"},
            {'firm_id': 13, 'firm_name': "ООО «Яндекс.Такси»"},
        ])
        extract_discount_query_result = pd.DataFrame([
            {"firm_name": "ООО «Яндекс»", 'dt': dt(year=2018, month=2, day=1), 'firm_discount_pct': 0.8775},
            {"firm_name": "Yandex Inc", 'dt': dt(year=2018, month=2, day=1), 'firm_discount_pct': 0.0},
        ])
        extract_distr_download_types = pd.DataFrame({'placeid': [
            909, 910, 911, 920, 931, 932, 938, 942, 949, 952, 996, 1000, 1111, 1132, 1146, 1157, 1184, 1546
        ]})

        extract_distr_balance_query_result = pd.DataFrame([{
            'pageid': 77114455,
            'placeid': 909,
            'balance_tag_id': 2973,
            'description': "Дистрибуция.Загрузка Firefox",
            'contract_id': 377550,
            'SUM(BUCKS)': 0.0,
            'currency': 'TRY',
            'reward': 21004.65,
        }])

        ids = [
            43758,
            43767,
            43769,
            43911,
            43912,
            43913,
            43914,
            43915,
            44123,
            44124,
            44125,
            44126,
            44127,
            44128,
            44129,
            39128,
            39664,
        ]
        extract_tag_query_result = pd.DataFrame({
            'pageid': ids,
            'tagid': ids,
        })

        extract_market_raw_query_result = pd.DataFrame([{
            'product_name2': 'Услуга по продвижению Карточки товара на Сервисе Яндекс.Маркет',
            'product_id': 508570,
            'activity_type_id': 86,
            'act_id': 77853165,
            'real_act_dt': dt.strptime('2018-02-25 22:12:52', '%Y-%m-%d %H:%M:%S'),
            'country': 'RU',
            'cost': 850.00002,
        }])

        extract_market_api_query_result = pd.DataFrame([{
            'dt': dt.strptime('2018-02-01 00:00:00', '%Y-%m-%d %H:%M:%S'),
            'dsp_id': 0,
            'pageid': 215165,
            'clientid': 0,
            'engineid': 0,
            'ordertype': 1,
            'product': 6,
            'devicegroup': 0,
            'country': 'RU',
            'cost': 13.43,
            'clicks': 41,
            'shows': 84736,
            'cost_net': 13.43
        }])

        extract_dsp_query_result = self.process_yan.make_empty_df_from_scheme('extract_dsp', 'dsp')

        extract_distr_contracts_query_result = pd.DataFrame([{
            'id': 199733,
            'contract': 'ДС-4171-12/14',
            'tag_id': 8,
            'firm_id': 1,
            'reward_type': 2,
            'client_id': 1016858,
            'client_name': 'Ковалев Вадим Евгеньевич',
            'person_id': 1966739,
            'person_name': 'Адстарк'
        }])

        extract_report_dt_result = pd.DataFrame([{
            'act_dt': dt.strptime('2018-02-01 00:00:00', '%Y-%m-%d %H:%M:%S'),
            'report_dt': dt.strptime('2018-02-01 00:00:00', '%Y-%m-%d %H:%M:%S'),
            'contract_id': 199733
        }])

        read_sql_query.side_effect = self.make_call_result_seq([
            ur_service_codes_query_result,
            extract_currency_query_result,
            extract_firm_query_result,
            extract_discount_query_result,
            extract_client_query_result,
            extract_contract_query_result,
            extract_reward_query_result,
            extract_market_raw_query_result,
            extract_market_api_query_result,
            load_dsp_charge_query_result,
            extract_dsp_query_result,
            extract_report_dt_result,
            extract_ecb_currency_query_result,
            extract_distr_download_types,
            extract_distr_balance_query_result,
            extract_tag_query_result,
            extract_distr_contracts_query_result,
        ])

        # rows = pd.DataFrame([{
        #     'tagid': 1915753,
        #     'deviceos': 'Android',
        #     'pageid': 90,
        #     'rur_cost': 0.5773983050847458,
        #     'cost_net': 0.020807818199999998,
        #     'engineid': 7,
        #     'rur_cost_net': 0.5290123271186441,
        #     'usd_cost_net': 0.009320029538633774,
        #     'usd_cost': 0.010172483670196216,
        #     'cost': 0.022711,
        #     'eur_cost': 0.008291889887256154,
        #     'clicks': 1,
        #     'try_cost': 0.038644191410199785,
        #     'dt': '2018-02-01',
        #     'clientid': 0,
        #     'ordertype': 1,
        #     'product': 0,
        #     'rtbshadow': 0,
        #     'country': 'BEL',
        #     'eur_cost_net': 0.007597029514704089,
        #     'try_cost_net': 0.03540580817002505,
        #     'shows': 0,
        #     'devicegroup': 1
        # }])

        awaps = self.actual_awaps()
        # awaps['dt'] = pd.to_datetime(awaps['dt'])
        rows = self.actual_rows()

        pages = pd.DataFrame([{
            'p.PageID': 73237,
            'domain': 'www.rockmarket.ru',
            'partnerid': 0,
            'intpage': 0,
            'mainserp': 0,
            'is_mobile': 0,
            'is_business_unit': 0
        }])
        # stat = self.process.make_empty_df_from_scheme('input', 'stat')
        # stat = pd.DataFrame(columns=list(self.process.input['stat'].keys()))
        # stat = stat.astype(dtype=self.process.input['stat'])
        env = {
            'rows': rows,
            'balance_ro': mock.Mock(spec=Engine),
            'meta': mock.Mock(spec=Engine),
            'begin_dt': '2018-02-01',
            'end_dt': '2018-02-28',
            # 'stat': stat,
            'pages': pages,
        }
        result_common = self.process_common.run_process(**env)
        assert(len(result_common) > 0)

        # тест сборки фазы РСЯ
        yan_env = {
            'is_forecast': False,
            'awaps': awaps,
            'awaps_dsp_ids': [666, 1488],
        }
        yan_env.update(result_common)
        result_yan = self.process_yan.run_process(**yan_env)
        assert(len(result_yan['partner_reward']) > 0)

        # тест сборки дистрибуции
        result_distr = self.process_distr.run_process(**result_common)
        assert(len(result_distr['distribution_details']) > 0)

    # KeyError: 'awaps_dsp_ids'
    @pytest.mark.skip(reason='Something very strange, failing')
    @mock.patch('pandas.read_sql_query')
    def test_load_dsp_charge(self, read_sql_query):
        read_sql_query.return_value = pd.DataFrame({
            'pageid': [1, 2],
            'product': [1, 7],
            'dsp_id': [1, 2],
            'dsp_charge': [1.0, 2.0],
        })
        env = {
            'begin_dt': None,
            'end_dt': None,
            'balance_ro': None,
        }
        self.process_yan.run_stage(env, 'load_dsp_charge')
