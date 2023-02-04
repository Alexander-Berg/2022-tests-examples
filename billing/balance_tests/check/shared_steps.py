# coding: utf-8
__author__ = 'chihiro'
import time
import json
import re
import copy
import uuid
import types
import decimal
import itertools
from datetime import datetime, timedelta
from decorator import decorator

from dateutil.relativedelta import relativedelta

from btestlib import shared, constants, utils, reporter
from check import utils as check_utils
from check import db
from check import yt_tables
from check.utils import create_data_file_in_s3
from check.steps import COMPLETION_DT, create_all_dc_files, create_ado_ado_awaps_data, \
    create_awaps_data, create_prcbb_data, create_data_in_market, create_data_in_ado, \
    create_data_in_navi, CONTRACT_START_DT, ACT_DT, TRANSACTION_LOG_DT
from check.defaults import Services


@decorator
def wait_for_run_check(check_runner, *args, **kwargs):
    # использовать этот декоратор, если планируется несколько запусков одной сверки в одном файле тестов
    def wrapper():
        for i in range(5):
            try:
                return check_runner(*args, **kwargs)
            except Exception as e:
                if i == 2 or not ('CheckAlreadyRunning' in str(e)):
                    raise e
                else:
                    time.sleep(10)

    return wrapper()


class SharedSteps(object):
    @staticmethod
    def sbt_data_for_yt(raw_data):
        yt_data = [
            {
                "client_id": str(data['client_id']),
                "currency": data['currency'],
                "payment_id": data['payment_id'],
                "type": data['type'],
                "updated": time.mktime(data['payment_data']['payment_dt'].timetuple()),
                "value": data['payment_data']['amount'],
                "payment_method": data['paymethod']
            } for data in raw_data.values()]
        yt_tables.sbt_create_data_in_yt(yt_data)

    @staticmethod
    def cbt_data_for_yt(raw_data):
        yt_data = dict()
        for data in raw_data:
            file_date = data['dt'].strftime('%Y-%m-%d')
            yt_data.setdefault(file_date, []).append(
                {
                    "client_id": str(data['client_id']),
                    "commission_currency": data.get('commission_currency'),
                    "commission_value": data.get('commission_value'),
                    "type": data.get('type'),
                    "coupon_value": data.get('coupon_value'),
                    "payment_method": data.get('payment_method'),
                    "order_cost": data.get('order_cost'),
                }
            )
        yt_tables.cbt_create_data_in_yt(yt_data)

    @staticmethod
    def cbd_data_for_yt(completions):
        def iso_dt_with_shift(date, shift):
            date += relativedelta(seconds=shift)
            return date.strftime('%Y-%m-%dT%H:%M:%S.%fZ')

        def group_key(d):
            return check_utils.str_date(d['dt'].date())

        def group_by_day(iterable):
            return itertools.groupby(sorted(iterable, key=group_key), group_key)

        def enumerate_id_uuid(iterable, start=0):
            for i, value in enumerate(iterable, start=start):
                yield i, str(uuid.uuid4()), value

        completions = group_by_day(completions)

        yt_data = dict()
        for day, day_completions in completions:
            day_completions = enumerate_id_uuid(day_completions, start=1)
            yt_data[day] = [{
                'client_id': str(row_id),
                'commission_sum': 0,
                'dt': iso_dt_with_shift(completion['dt'], row_id),
                'orig_transaction_id': row_uuid,
                'payment_type': 'card',
                'promocode_sum': str(completion['promocode_sum']),
                'service_order_id': row_uuid,
                'total_sum': str(completion['total_sum']),
                'transaction_currency': completion['transaction_currency'],
                'transaction_id': row_uuid,
                'type': completion['type'],
                'use_discount': 0,
            } for row_id, row_uuid, completion in day_completions]
        yt_tables.cbd_create_data_in_yt(yt_data)

    @staticmethod
    def obg2_data_for_yt(completions):
        #TODO запустить и посмотреть что получается
        def group_key(d):
            return check_utils.str_date(d['dt'].date())

        def group_by_day(iterable):
            return itertools.groupby(sorted(iterable, key=group_key), group_key)

        def enumerate_id_uuid(iterable, start=0):
            for i, value in enumerate(iterable, start=start):
                yield i, str(uuid.uuid4()), value

        completions = group_by_day(completions)

        yt_data = dict()
        for day, day_completions in completions:
            day_completions = enumerate_id_uuid(day_completions, start=1)
            yt_data[day] = [{
                'order_id': int(completion['order_id']),
                # 'completion_delta': str(completion['completion_delta'])
                'spent_delta': str(completion['completion_delta'])
            } for row_id, row_uuid, completion in day_completions]
        yt_tables.obg2_create_data_in_yt(yt_data)

    @staticmethod
    def obb2_data_for_yt(raw_data):
        yt_tables.obb2_create_data_in_yt(raw_data)

    @staticmethod
    def cpbt_data_for_yt(raw_data):
        yt_tables.cpbt_create_data_in_yt(raw_data)

    @staticmethod
    def tbs_data_for_yt(raw_data):
        yt_tables.tbs_create_data_in_yt(raw_data)

    @staticmethod
    def prcbb_revers_data_for_yt(raw_data):
        yt_tables.prcbb_create_data_in_yt(raw_data)

    @staticmethod
    def cbf_data_for_yt(raw_data):
        file_date = None
        formatted_data = []

        for data in raw_data:
            transaction_dt = data['dt']
            transaction_dt_str = transaction_dt.strftime('%Y-%m-%d %H:%M:%S')

            day = transaction_dt.replace(hour=0, minute=0, second=0, microsecond=0)
            day_str = day.strftime('%Y-%m-%d')
            if file_date is None:
                file_date = day_str

            transaction_id = int(time.time())
            commission_sum = '{:.2f}'.format(decimal.Decimal(data['commission_sum']))

            formatted_data.append({
                'client_id': int(data['client_id']),
                'service_id': int(data['service_id']),

                'transaction_id': transaction_id,
                'orig_transaction_id': transaction_id,
                'service_order_id': '{0}-{0}'.format(transaction_id),

                'commission_sum': commission_sum,
                'promocode_sum': '0.00',
                'total_sum': commission_sum,

                'transaction_currency': data['currency'],
                'type': data['type'],

                'dt': transaction_dt_str,
                'utc_start_load_dttm': transaction_dt_str,
            })

        yt_tables.cbf_create_data_in_yt(file_date, formatted_data)

    @staticmethod
    def pbf_data_for_yt(file_date, raw_data):
        formatted_data = []

        for data in raw_data:
            transaction_dt = data['dt']
            transaction_dt_str = transaction_dt.strftime('%Y-%m-%d %H:%M:%S')

            day = transaction_dt.replace(hour=0, minute=0, second=0, microsecond=0)
            day_str = day.strftime('%Y-%m-%d')

            amount = '{:.2f}'.format(decimal.Decimal(data['amount']))

            formatted_data.append({
                'transaction_id': int(data['transaction_id']),
                'service_id': int(data['service_id']),
                'client_id': int(data['client_id']),
                'payment_id': int(data['payment_id']),

                'payment_type': data['payment_type'],
                'transaction_type': data['transaction_type'],
                'paysys_type_cc': data['paysys_type_cc'],
                'product': data['product'],
                'service_order_id': str(data['service_order_id']),

                'value_amount': amount,
                'value_currency': data['currency'],

                'dt': transaction_dt_str,
                'utc_start_load_dttm': transaction_dt_str,

                'payload': '{}',
            })

        yt_tables.pbf_create_data_in_yt(file_date, formatted_data)

    @staticmethod
    def dc_reverse_data_for_yt(raw_data):
        yt_tables.dc_reverse_create_data_in_yt(raw_data)

    @staticmethod
    def zbb_boid_data_in_yt(raw_data):
        data = [
            {'cid': service_order_id, 'wallet_cid': index}
            for index, service_order_id in enumerate(raw_data, 1)
        ]
        yt_tables.zbb_boid_create_data_in_yt(data)

    @staticmethod
    def taxi_revenues_data_for_yt(tables_path, raw_data):
        data = dict()
        for d in raw_data:
            file_date = d['dt'].strftime('%Y-%m-%d')
            data.setdefault(file_date, []).append(
                {
                    "client_id": str(d['client_id']),
                    "currency": d.get('currency'),
                    "service_id": d.get('service_id'),
                    "transaction_type": d.get('transaction_type'),
                    "amount": d.get('amount'),
                    "event_time": d.get('event_time'),
                    "product": d.get('product'),
                    "aggregation_sign": d.get('aggregation_sign'),
                    "ignore_in_balance": d.get('ignore_in_balance', False),
                }
            )
        yt_tables.taxi_revenues_create_data_in_yt(tables_path, data)

    @staticmethod
    def taxi_expenses_data_for_yt(tables_path, raw_data):
        def format_taxi_dt(dt_):
            return dt_.strftime('%Y-%m-%dT%H:%M:%S+03:00')

        data = dict()
        for d in raw_data:
            file_date = d['dt'].strftime('%Y-%m-%d')
            data.setdefault(file_date, []).append(
                {
                    'service_id': int(d['service_id']),
                    'transaction_id': int(d['transaction_id']),
                    'client_id': str(d['client_id']),
                    'product': d['payment_type'],
                    'transaction_type': d['transaction_type'],
                    'amount': '{:.4f}'.format(d['amount']),
                    'currency': d['currency'],
                    'event_time': format_taxi_dt(d['dt']),
                    'transaction_time': format_taxi_dt(d['transaction_dt']),
                    'ignore_in_balance': d.get('ignore_in_balance', False),
                }
            )
        yt_tables.taxi_expenses_create_data_in_yt(tables_path, data)


class SharedBlocks(object):
    RUN_CBD = 'run_cbd'
    RUN_SPA = 'run_spa'
    RUN_SBT = 'run_sbt'
    RUN_SBT2 = 'run_sbt2'
    RUN_BUA = 'run_bua'
    RUN_BUA_ADFOX = 'run_bua_adfox'
    RUN_BUA_PARTNER = 'run_bua_partner'
    RUN_AOB = 'run_aob'
    RUN_AOB_MARKET = 'run_aob_market'
    RUN_AOB_TR = 'run_aob_tr'
    RUN_AOB_SW = 'run_aob_sw'
    RUN_AOB_TAXI = 'run_aob_taxi'
    RUN_AOB_US = 'run_aob_us'
    RUN_AOB_VERTICAL = 'run_aob_vertical'
    RUN_AOB_SERVICES = 'run_aob_services'
    RUN_AOB_HEALTH = 'run_aob_health'
    RUN_AOB_UBER_ML_BV = 'run_aob_uber_ml_bv'
    RUN_AOB_KINOPOISK = 'run_aob_kinopoisk'
    RUN_AOB_UBER_KZ = 'run_aob_uber_kz'
    RUN_AOB_ISRAEL_GO = 'run_aob_israel_go'
    RUN_AOB_TAXI_AM = 'run_aob_taxi_am'
    RUN_AOB_TAXI_BV = 'run_aob_taxi_bv'
    RUN_AOB_TAXI_KZ = 'run_aob_taxi_kz'
    RUN_AOB_DRIVE = 'run_aob_drive'
    RUN_AOB_HK_ECOMMERCE = 'run_aob_hk_ecommerce'
    RUN_AOB_YA_CLOUD = 'run_aob_ya_cloud'
    RUN_AOB_GAS = 'run_aob_gas'
    RUN_AOB_UBER_AZ = 'run_aob_uber_az'
    RUN_AOB_MLU_EUROPE_BV = 'run_aob_mlu_europe_bv'
    RUN_AOB_MLU_AFRICA_BV = 'run_aob_mlu_africa_bv'
    RUN_ZBB = 'run_zbb'
    RUN_ARH = 'run_arh'
    # NO_ACTIONS = 'no_actions'
    RUN_OVI = 'run_ovi'
    RUN_DC = 'run_dc'
    RUN_DC_MARKET_CPA = 'run_dc_market_cpa'
    RUN_CBT = 'run_cbt'
    RUN_CBT2 = 'run_cbt2'
    RUN_OAAW2 = 'run_oaaw2'
    RUN_OMB = 'run_omb'
    RUN_PRCBB = 'run_prcbb'
    RUN_PRCBB_REVERS = 'run_prcbb_revers'
    RUN_TCTC = 'run_tctc'
    RUN_OBB2 = 'run_obb2'
    RUN_OBG = 'run_obg'
    RUN_OBG2 = 'run_obg2'
    RUN_IOB = 'run_iob'
    RUN_IOB_MARKET = 'run_iob_market'
    RUN_IOB_TR = 'run_iob_tr'
    RUN_IOB_SW = 'run_iob_sw'
    RUN_IOB_TAXI = 'run_iob_taxi'
    RUN_IOB_US = 'run_iob_us'
    RUN_IOB_VERTICAL = 'run_iob_vertical'
    RUN_IOB_SERVICES = 'run_iob_services'
    RUN_IOB_HEALTH = 'run_iob_health'
    RUN_IOB_UBER_ML_BV = 'run_iob_uber_ml_bv'
    RUN_IOB_KINOPOISK = 'run_iob_kinopoisk'
    RUN_IOB_UBER_KZ = 'run_iob_uber_kz'
    RUN_IOB_ISRAEL_GO = 'run_iob_israel_go'
    RUN_IOB_TAXI_AM = 'run_iob_taxi_am'
    RUN_IOB_TAXI_BV = 'run_iob_taxi_bv'
    RUN_IOB_TAXI_KZ = 'run_iob_taxi_kz'
    RUN_IOB_DRIVE = 'run_iob_drive'
    RUN_IOB_HK_ECOMMERCE = 'run_iob_hk_ecommerce'
    RUN_IOB_YA_CLOUD = 'run_iob_ya_cloud'
    RUN_IOB_GAS = 'run_iob_gas'
    RUN_IOB_UBER_AZ = 'run_iob_uber_az'
    RUN_IOB_MLU_EUROPE_BV = 'run_iob_mlu_europe_bv'
    RUN_IOB_MLU_AFRICA_BV = 'run_iob_mlu_africa_bv'
    RUN_ZBG = 'run_zbg'
    RUN_ZMB = 'run_zmb'
    RUN_OBA2 = 'run_oba2'
    RUN_OBN = 'run_obn'
    RUN_CPBT = 'run_cpbt'
    RUN_CPBT2 = 'run_cpbt2'
    RUN_CSBT = 'run_csbt'
    RUN_OBAR = 'run_obar'
    RUN_TBS = 'run_tbs'
    RUN_UAO = 'run_uao'
    RUN_CCAOB = 'run_ccaob'
    RUN_CAOB = 'run_caob'
    RUN_AROB = 'run_arob'
    RUN_CBF = 'run_cbf'
    RUN_PBF = 'run_pbf'

    TEST1 = 'tst1'

    CHECK_BLOCK = {
        'aob':                RUN_AOB,
        'aob_market':         RUN_AOB_MARKET,
        'aob_tr':             RUN_AOB_TR,
        'aob_sw':             RUN_AOB_SW,
        'aob_taxi':           RUN_AOB_TAXI,
        'aob_us':             RUN_AOB_US,
        'aob_vertical':       RUN_AOB_VERTICAL,
        'aob_services':       RUN_AOB_SERVICES,
        'aob_health':         RUN_AOB_HEALTH,
        'aob_uber_ml_bv':     RUN_AOB_UBER_ML_BV,
        'aob_kinopoisk':      RUN_AOB_KINOPOISK,
        'aob_uber_kz':        RUN_AOB_UBER_KZ,
        'aob_israel_go':      RUN_AOB_ISRAEL_GO,
        'aob_taxi_am':        RUN_AOB_TAXI_AM,
        'aob_taxi_bv' :       RUN_AOB_TAXI_BV,
        'aob_taxi_kz':        RUN_AOB_TAXI_KZ,
        'aob_drive':          RUN_AOB_DRIVE,
        'aob_hk_ecommerce':   RUN_AOB_HK_ECOMMERCE,
        'aob_ya_cloud':       RUN_AOB_YA_CLOUD,
        'aob_gas':            RUN_AOB_GAS,
        'aob_uber_az':        RUN_AOB_UBER_AZ,
        'aob_mlu_europe_bv':  RUN_AOB_MLU_EUROPE_BV,
        'aob_mlu_africa_bv':  RUN_AOB_MLU_AFRICA_BV,



        'iob':               RUN_IOB,
        'iob_market':        RUN_IOB_MARKET,
        'iob_tr':            RUN_IOB_TR,
        'iob_sw':            RUN_IOB_SW,
        'iob_taxi':          RUN_IOB_TAXI,
        'iob_us':            RUN_IOB_US,
        'iob_vertical':      RUN_IOB_VERTICAL,
        'iob_services':      RUN_IOB_SERVICES,
        'iob_health':        RUN_IOB_HEALTH,
        'iob_uber_ml_bv':    RUN_IOB_UBER_ML_BV,
        'iob_kinopoisk':     RUN_IOB_KINOPOISK,
        'iob_uber_kz':       RUN_IOB_UBER_KZ,
        'iob_israel_go':     RUN_IOB_ISRAEL_GO,
        'iob_taxi_am':       RUN_IOB_TAXI_AM,
        'iob_taxi_bv' :      RUN_IOB_TAXI_BV,
        'iob_taxi_kz':       RUN_IOB_TAXI_KZ,
        'iob_drive':         RUN_IOB_DRIVE,
        'iob_hk_ecommerce':  RUN_IOB_HK_ECOMMERCE,
        'iob_ya_cloud':      RUN_IOB_YA_CLOUD,
        'iob_gas':           RUN_IOB_GAS,
        'iob_uber_az':       RUN_IOB_UBER_AZ,
        'iob_mlu_europe_bv': RUN_IOB_MLU_EUROPE_BV,
        'iob_mlu_africa_bv': RUN_IOB_MLU_AFRICA_BV,
    }

    @staticmethod
    def block_by_check_name(check_name):
        return SharedBlocks.CHECK_BLOCK[check_name]

    @staticmethod
    def check_test_name_aob(check_name, test_name):
        keys_aob = [
            'AobHealth', 'AobKinopoisk',   'AobMarket',   'AobMluEuropeBv', 'AobMluAfricaBv','AobServices',
            'AobSW',     'AobTaxiAm',      'AobTaxiBv',   'AobTaxiKz',     'AobTaxi',     'AobTR',
            'AobUberAZ', 'AobUberKZ',      'AobUberMlBv', 'AobUS',         'AobVertical', 'AobIsraelGo',
            'AobDrive',  'AobHkEcommerce', 'AobYaCloud',  'AobGas'
        ]
        result = re.findall(r'.Test({})\S*$'.format('|'.join(keys_aob) + '|Aob'), test_name)
        result = [res.upper() for res in result]
        return check_name.replace('_', '').upper() in result if result and len(result) == 1 else False

    @staticmethod
    def check_test_name_iob(check_name, test_name):
        keys_iob = [
            'IobHealth', 'IobKinopoisk',   'IobMarket',   'IobMluEuropeBv', "IobMluAfricaBv", 'IobServices',
            'IobSW',     'IobTaxiAm',      'IobTaxiBv',   'IobTaxiKz',      'IobTaxi',     'IobTR',
            'IobUberAZ', 'IobUberKZ',      'IobUberMlBv', 'IobUS',          'IobVertical', 'IobIsraelGo',
            'IobDrive',  'IobHkEcommerce', 'IobYaCloud',  'IobGas'
        ]
        result = re.findall(r'.Test({})\S*$'.format('|'.join(keys_iob) + '|Iob'), test_name)
        result = [res.upper() for res in result]
        return check_name.replace('_', '').upper() in result if result and len(result) == 1 else False

    @staticmethod
    def run_cbd(shared_data, before, tests_list):
        from balance.tests.partner_schema_acts.test_drive_acts import \
            create_drive_completion, delete_drive_completions

        check_code = 'cbd'
        default_product_id = constants.Products.CARSHARING_WITH_NDS_1.id
        # TODO: move to constants
        default_order_type = 'carsharing'
        default_currency = constants.Currencies.RUB.iso_code

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_CBD) as block:
            block.validate()
            s3_data = {}

            start_dt = None
            end_dt = None
            completions = {
                'billing': [],
                'yt': [],
            }

            for test_name in tests_list:
                if 'test_cbd' in test_name:
                    s3_data[test_name] = test_data = \
                        shared.get_data_from_s3(test_name)

                    if not start_dt and 'start_dt' in test_data:
                        start_dt = test_data['start_dt']
                    if not end_dt and 'end_dt' in test_data:
                        end_dt = test_data['end_dt']
                    if 'billing_completions' in test_data:
                        for completion in test_data['billing_completions']:
                            completions['billing'].append({
                                'product_id': completion.get(
                                    'product_id', default_product_id),
                                'dt': completion.get('dt', start_dt),
                                'amount': completion.get('amount', 1),
                            })
                    if 'yt_completions' in test_data:
                        for completion in test_data['yt_completions']:
                            completions['yt'].append({
                                'type': completion.get(
                                    'type', default_order_type),
                                'dt': completion.get('dt', start_dt),
                                'total_sum': completion.get('amount', 1),
                                'promocode_sum': completion.get(
                                    'promocode_sum', 0),
                                'transaction_currency': completion.get(
                                    'currency', default_currency),
                            })

            delete_drive_completions(start_dt)
            for completion in completions['billing']:
                create_drive_completion(**completion)

            SharedSteps.cbd_data_for_yt(completions['yt'])

            check_args = dict()
            if start_dt:
                check_args['check-period-begin-dt'] = \
                    check_utils.str_date(start_dt)
            if end_dt:
                check_args['check-period-end-dt'] = \
                    check_utils.str_date(end_dt)

            cmp_id = check_utils.run_check_new(
                check_code, [], check_args)
            diffs = db.get_cmp_diff(cmp_id, check_code)

            for test_name in tests_list:
                if 'test_cbd' in test_name:
                    s3_data[test_name].update({'cmp_data': diffs})
                    shared.push_data_to_s3(s3_data[test_name], test_name)

            return diffs

    @staticmethod
    def run_spa(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_SPA) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_spa' in test_name:
                    if shared.get_data_from_s3(test_name):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                        if s3_data_list[test_name] and s3_data_list[test_name].get('contract_id'):
                            object_ids.append(str(s3_data_list[test_name]['contract_id']))

            diffs = db.get_cmp_diff(check_utils.run_check_new('spa', ','.join(object_ids)), 'spa')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_sbt(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_SBT) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            all_data = {}

            for test_name in tests_list:
                if 'test_sbt.py' in test_name:
                    all_data[test_name] = shared.get_data_from_s3(test_name)
                    if 'not_found_in_yt' not in test_name and all_data[test_name].get('client_id'):
                        s3_data_list[test_name] = all_data[test_name]
                        s3_data_list[test_name].update({"payment_id": s3_data_list[test_name]['trust_payment_id']})
                        s3_data_list[test_name].update({"type": "payment"})
                        s3_data_list[test_name + '_refund'] = copy.deepcopy(s3_data_list[test_name])
                        s3_data_list[test_name + '_refund'].update(
                            {"payment_id": s3_data_list[test_name]['trust_refund_id']})
                        s3_data_list[test_name + '_refund'].update(
                            {"payment_data": s3_data_list[test_name]['refund_data']})
                        s3_data_list[test_name + '_refund'].update({"type": "refund"})
                    if all_data[test_name].get('client_id'):
                        object_ids.append(str(all_data[test_name]['client_id']))
                    if all_data[test_name].get('client_id_changed'):
                        s3_data_list[test_name].update({"client_id": all_data[test_name]['client_id_changed']})
                        s3_data_list[test_name + '_refund'].update(
                            {"client_id": all_data[test_name]['client_id_changed']})
                        object_ids.append(str(all_data[test_name]['client_id_changed']))
            SharedSteps.sbt_data_for_yt(s3_data_list)

            run_dt = datetime.now().strftime('%Y-%m-%d')
            params = {
                'check-period-begin-dt': run_dt,
                'check-period-end-dt': run_dt,
            }

            diffs = db.get_cmp_diff(check_utils.run_check_new('sbt', ','.join(object_ids), params), 'sbt')
            for test_name in tests_list:
                if 'test_sbt.py' in test_name:
                    all_data[test_name].update({'cmp_data': diffs})
                    shared.push_data_to_s3(all_data[test_name], test_name)
            return diffs

    @staticmethod
    def run_cbt(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_CBT) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            yt_data = []

            for test_name in tests_list:
                if 'test_cbt.py' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)

                    if s3_data_list[test_name].get('client_id'):
                        object_ids.append(str(s3_data_list[test_name]['client_id']))
                    if s3_data_list[test_name].get('yt_data'):
                        yt_data.append(s3_data_list[test_name].get('yt_data'))

            SharedSteps.cbt_data_for_yt(yt_data)

            the_day_before_tlog = TRANSACTION_LOG_DT - timedelta(days=1)
            params = {
                'check-period-begin-dt': CONTRACT_START_DT.strftime('%Y-%m-%d'),
                'check-period-end-dt': the_day_before_tlog.strftime('%Y-%m-%d'),
            }

            diffs = db.get_cmp_diff(check_utils.run_check_new('cbt', ','.join(object_ids), params), 'cbt')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_taxi_transaction_log_check(test_namespace, check_code_name,
                                       yt_data_tables_path, yt_data_generator,
                                       shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=shared_data.block_name) as block:
            block.validate()

            balance_objects = []
            taxi_data_list = []

            s3_cache = {}

            for test_name in tests_list:
                if test_namespace not in test_name:
                    continue

                test_cache = s3_cache[test_name] = shared.get_data_from_s3(test_name)
                if test_cache is None:
                    # Некоторые тесты не подготавливают данные
                    continue

                client_id = test_cache.get('client_id')
                taxi_client_id = test_cache.get('taxi_client_id')
                taxi_data = test_cache.get('yt_data')

                if client_id:
                    balance_objects.append(str(client_id))
                if taxi_client_id:
                    balance_objects.append(str(taxi_client_id))
                if taxi_data:
                    taxi_data_list.append(taxi_data)

            yt_data_generator(yt_data_tables_path, taxi_data_list)

            params = {
                'check-period-begin-dt': TRANSACTION_LOG_DT.strftime('%Y-%m-%d'),
                'check-period-end-dt': ACT_DT.strftime('%Y-%m-%d'),
            }

            cmp_id = check_utils.run_check_new(check_code_name, ','.join(balance_objects), params)
            diffs = db.get_cmp_diff(cmp_id, check_code_name)

            for test_name in s3_cache:
                s3_cache[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_cache[test_name], test_name)
            return diffs

    @classmethod
    def run_taxi_revenues_check(cls, test_namespace, check_code_name, yt_data_tables_path,
                                shared_data, before, tests_list):
        yt_data_generator = SharedSteps.taxi_revenues_data_for_yt
        return cls.run_taxi_transaction_log_check(test_namespace, check_code_name,
                                                  yt_data_tables_path, yt_data_generator,
                                                  shared_data, before, tests_list)

    @classmethod
    def run_taxi_expenses_check(cls, test_namespace, check_code_name, yt_data_tables_path,
                                shared_data, before, tests_list):
        yt_data_generator = SharedSteps.taxi_expenses_data_for_yt
        return cls.run_taxi_transaction_log_check(test_namespace, check_code_name,
                                                  yt_data_tables_path, yt_data_generator,
                                                  shared_data, before, tests_list)

    @staticmethod
    def run_oaaw2(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OAAW2) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            ado_data = []
            awaps_data = []

            for test_name in tests_list:
                if 'test_oaaw2' in test_name:
                    if shared.get_data_from_s3(test_name):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                        if s3_data_list[test_name].get('order_number'):
                            object_ids.append(str(s3_data_list[test_name]['order_number']))
                        if s3_data_list[test_name].get('ado_data'):
                            ado_data.append(s3_data_list[test_name].get('ado_data'))
                        if s3_data_list[test_name].get('awaps_data'):
                            awaps_data.append(s3_data_list[test_name].get('awaps_data'))

            create_ado_ado_awaps_data(ado_data, db_key='oaaw2_ad_office_importer_url')
            create_awaps_data(awaps_data, db_key='oaaw2_awaps_importer_url')

            diffs = db.get_cmp_diff(check_utils.run_check_new('oaaw2', ','.join(object_ids)), 'oaaw2')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_omb(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OMB) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_omb' in test_name:
                    if shared.get_data_from_s3(test_name):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                        if s3_data_list[test_name].get('order_data'):
                            object_ids.append(str(s3_data_list[test_name]['order_data']['service_order_id']))

            diffs = db.get_cmp_diff(check_utils.run_check_new('omb', ','.join(object_ids)), 'omb')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_bua(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_BUA) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_bua' in test_name and not ('adfox' in test_name.lower()) and not ('partners' in test_name.lower()):

                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name] and s3_data_list[test_name].get('orders_map'):
                        for order_key in s3_data_list[test_name]['orders_map']:
                            if s3_data_list[test_name]['orders_map'][order_key].get('id'):
                                object_ids.append(str(s3_data_list[test_name]['orders_map'][order_key].get('id')))

            print ('>>>>>>> [S3_DATA_LIST]: {}'.format(s3_data_list))

            date = utils.Date.get_last_day_of_previous_month().strftime('%Y-%m-%d')

            # запускаем сверку на сегодняшний день
            params = {
                'completions-dt': date,
                'acts-dt': date,
                'exclude-service-ids': str(Services.ticket),
                 # 'need-auto-analysis': '0',   # TODO remove this string
            }
            cmp_id = check_utils.run_check_new('bua', ','.join(object_ids), params)
            diffs = db.get_cmp_diff(cmp_id, 'bua')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_bua_adfox(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_BUA_ADFOX) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_bua' in test_name and 'adfox' in test_name.lower():
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('orders_map') and s3_data_list[test_name]['orders_map'].get(1):
                        object_ids.append(str(s3_data_list[test_name]['orders_map'][1]['id']))

            date = datetime.now().strftime('%Y-%m-%d')
            params = {
                'acts-dt': date,
                'completions-dt': date,
                'need-auto-analysis': '1',
            }
            cmp_id = check_utils.run_check_new('bua_partners', ','.join(object_ids), params)

            diffs = db.get_cmp_diff(cmp_id, 'bua')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_bua_partner(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_BUA_PARTNER) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_bua' in test_name and 'partners' in test_name.lower():
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('orders_map') and s3_data_list[test_name]['orders_map'].get(1):
                        object_ids.append(str(s3_data_list[test_name]['orders_map'][1]['id']))

            date = datetime.now().strftime('%Y-%m-%d')
            params = {
                'acts-dt': date,
                'completions-dt': date,
                'need-auto-analysis': '0',
            }
            cmp_id = check_utils.run_check_new('bua_partners', ','.join(object_ids), params)
            diffs = db.get_cmp_diff(cmp_id, 'bua')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_aob(shared_data, before, tests_list, check_name='aob'):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.block_by_check_name(check_name)) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if SharedBlocks.check_test_name_aob(check_name, test_name):
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name] and s3_data_list[test_name].get('act_map'):
                        if s3_data_list[test_name]['act_map'].get('eid'):
                            object_ids.append(str(s3_data_list[test_name]['act_map'].get('eid')))

            # запускаем сверку за предыдущий месяц
            diffs = db.get_cmp_diff(check_utils.run_check_new(check_name, ','.join(object_ids)),
                                    cmp_name=check_name if check_name in ['aob_auto', 'aob_sw', 'aob_tr', 'aob_ua',
                                                                          'aob_us'] else 'aob')

            # запускаем сверку в yt
            try:
                yt_cmp_id = check_utils.run_check_new(check_name, str(','.join(object_ids)), {'use-yt': '1'})
            except:
                yt_cmp_id = None

            for test_name in s3_data_list:
                if SharedBlocks.check_test_name_aob(check_name, test_name) and s3_data_list[test_name]:
                    s3_data_list[test_name].update({'cmp_data': diffs, 'cmp_id_yt': yt_cmp_id})
                    shared.push_data_to_s3(s3_data_list[test_name], test_name)

            return diffs

    @staticmethod
    def run_zbb(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_ZBB) as block:
            block.validate()
            objects = []
            bk_data_list = []
            boid_data_list = []
            s3_data_list = {}

            def format_order(order_):
                return '{}-{}'.format(order_['service_id'], order_['service_order_id'])

            def format_bk_data(data):
                return '\t'.join(data)

            for test_name in tests_list:
                if 'test_zbb' in test_name:
                    if shared.get_data_from_s3(test_name):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                        if s3_data_list[test_name].get('order_data'):
                            objects.append(format_order(s3_data_list[test_name]['order_data']))

                            if s3_data_list[test_name]['bk_data']:
                                bk_data_list.append(format_bk_data(s3_data_list[test_name]['bk_data']))

                        boid_data = s3_data_list[test_name].get('boid_data')
                        if boid_data:
                            boid_service_order_id = boid_data['boid']['service_order_id']
                            boid_data_list.append(boid_service_order_id)

                            for order in boid_data.values():
                                objects.append(format_order(order))

                                bk_data = order.get('bk_data')
                                if bk_data:
                                    bk_data_list.append(format_bk_data(bk_data))

            create_data_file_in_s3(
                content='0\tok\n{}\n#End'.format('\n'.join(bk_data_list)),
                file_name='bk_consumptions.csv',
                db_key='zbb_bk_importer_url',
            )
            SharedSteps.zbb_boid_data_in_yt(boid_data_list)

            diffs = db.get_cmp_diff(check_utils.run_check_new('zbb', ','.join(objects)), 'zbb')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_arh(shared_data, before, tests_list):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_ARH) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_arh' in test_name:
                    if shared.get_data_from_s3(test_name):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                        if s3_data_list[test_name] and s3_data_list[test_name].get('new_head'):
                            object_ids.append(str(s3_data_list[test_name]['new_head']))

            begin_dt = (datetime.now() - timedelta(days=5)).strftime('%Y-%m-%d')
            end_dt = datetime.now().strftime('%Y-%m-%d')

            params = {
                'begin-dt': begin_dt,
                'end-dt': end_dt,
                'username': 'chihiro',
                'need-auto-analysis': '1',
            }
            diffs = db.get_cmp_diff(check_utils.run_check_new('arh', ','.join(object_ids), params), 'arh')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_prcbb(shared_data, before, tests_list, begin_dt=None, end_dt=None):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_PRCBB) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            prcbb_data = []
            yt_data_list = []
            reverse_obj_id = []

            for test_name in tests_list:
                if 'test_prcbb' in test_name:
                    if shared.get_data_from_s3(test_name):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                        if s3_data_list[test_name] and s3_data_list[test_name].get('prcbb_data'):
                            prcbb_data.append(s3_data_list[test_name]['prcbb_data'])

                        if s3_data_list[test_name].get('yt_reverse'):
                            yt_data_list.append(s3_data_list[test_name]['yt_reverse'])
                            reverse_obj_id.append(str(s3_data_list[test_name]['place_id']))
                            begin_dt_rev = s3_data_list[test_name]['prcbb_data']['date']
                            end_dt_rev = s3_data_list[test_name]['prcbb_data']['date']
                            continue # не заносим 'place_id' для revers в общий список 'place_id'

                        if s3_data_list[test_name] and s3_data_list[test_name].get('place_id'):
                            object_ids.append(str(s3_data_list[test_name]['place_id']))

            create_prcbb_data(prcbb_data)
            SharedSteps.prcbb_revers_data_for_yt(yt_data_list)

            diffs = []
            diffs_rev = []

            if reverse_obj_id:
                begin_dt = begin_dt_rev
                end_dt = end_dt_rev
                params = {'begin-dt': begin_dt, 'end-dt': end_dt}
                diffs_rev = db.get_cmp_diff(
                    check_utils.run_check_new('prcbb', ','.join(reverse_obj_id), params), 'prcbb')

            if object_ids:
                begin_dt = datetime.now().replace(day=1).strftime('%Y-%m-%d')
                end_dt = datetime.now().strftime('%Y-%m-%d')

                params = {'begin-dt': begin_dt, 'end-dt': end_dt}
                diffs = db.get_cmp_diff(
                    check_utils.run_check_new('prcbb', ','.join(object_ids), params), 'prcbb')

            # добавляем расхождения из revers
            diffs.extend(diffs_rev)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs



    @staticmethod
    def run_dc(shared_data, before, tests_list, additional_conditions=None, block_name=None, begin_dt=None):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_DC if not block_name else block_name) as block:
            block.validate()
            source_ids = set()
            object_ids = set()
            s3_data_list = {}
            data_by_source = {4: [], 1: [], 17: [], 11: [], 8: [], 14: [], 31: []}

            def additional_func(test_name):
                return 'MarketCPA' not in test_name

            additional_conditions = additional_conditions if additional_conditions else additional_func

            for test_name in tests_list:
                if 'test_dc' in test_name and additional_conditions(test_name):
                    if shared.get_data_from_s3(test_name):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                        if s3_data_list[test_name]:

                            object_id = (s3_data_list[test_name].get('place_id') or
                                         s3_data_list[test_name].get('tag_id'))
                            if object_id:
                                object_ids.add(str(object_id))

                            source_id = s3_data_list[test_name].get('source_id')
                            if source_id:
                                source_ids.add(str(source_id))

                                dc_data = s3_data_list[test_name].get('dc_data')
                                if dc_data:
                                    # Загрузки объединяются с БК при формировании файла
                                    if source_id == 101:
                                        source_id = 1
                                    data_by_source[source_id].append(dc_data)


            begin_dt = begin_dt or COMPLETION_DT
            create_all_dc_files(data_by_source, file_date=begin_dt)

            date = begin_dt.strftime('%Y-%m-%d')
            params = {'begin-dt': date, 'end-dt': date, 'need-auto-analysis': '0'}

            if source_ids:
                params['source-ids'] = ','.join(source_ids)

            objects = object_ids and ','.join(object_ids) or None
            diffs = db.get_cmp_diff(check_utils.run_check_new('dc', objects, params), 'dc')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_dc_with_auto_analyze(shared_data, before, tests_list, additional_conditions=None, block_name=None, begin_dt=None):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_DC if not block_name else block_name) as block:
            block.validate()
            source_ids = set()
            object_ids = set()
            s3_data_list = {}
            data_by_source = {4: [], 1: [], 17: [], 11: [], 8: [], 26: [], 14: [], 31: []}
            yt_data_list = []

            for test_name in tests_list:
                if 'test_dc_auto_analyze' in test_name:
                    if shared.get_data_from_s3(test_name):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                        if s3_data_list[test_name]:

                            if s3_data_list[test_name].get('place_id'):
                                object_ids.add(str(s3_data_list[test_name]['place_id']))

                            if s3_data_list[test_name].get('source_id'):
                                source_id = s3_data_list[test_name]['source_id']
                                source_ids.add(str(source_id))

                            if s3_data_list[test_name].get('dc_data'):
                                data_by_source[source_id].append(s3_data_list[test_name]['dc_data'])

                            if s3_data_list[test_name].get('yt_reverse'):
                                yt_data_list.append(s3_data_list[test_name]['yt_reverse'])

            if yt_data_list:
                SharedSteps.dc_reverse_data_for_yt(yt_data_list)

            begin_dt = begin_dt or COMPLETION_DT
            create_all_dc_files(data_by_source, file_date=begin_dt)

            date = begin_dt.strftime('%Y-%m-%d')
            params = {'begin-dt': date, 'end-dt': date}


            # TODO пока оставлю для теста "test_dc_shows_not_converge_revers_bk"
            # if 'test_dc_shows_not_converge_revers_bk' in test_name:
            #     two_month_ago = datetime.now() - relativedelta(months=2)
            #     create_all_dc_files(data_by_source, file_date=two_month_ago)
            #
            #     date = begin_dt
            #     params = {'begin-dt': date, 'end-dt': date}



            if source_ids:
                params['source-ids'] = ','.join(source_ids)

            objects = object_ids and ','.join(object_ids) or None
            diffs = db.get_cmp_diff(check_utils.run_check_new('dc', objects, params), 'dc')
            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs


    @staticmethod
    def run_ovi(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'ovi'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OVI) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_ovi' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('orders_map'):
                        object_ids.append(str(s3_data_list[test_name]['orders_map']['invoice_ids'][0]))

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_tctc(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'tctc'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_TCTC) as block:

            block.validate()
            object_ids = []
            s3_data_list = {}

            # Дальше ещё ничего не менял

            for test_name in tests_list:
                if 'test_tctc' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('contract_id'):
                        object_ids.append(str(s3_data_list[test_name]['contract_id']))

            objects = ','.join(object_ids)

            begin_dt = utils.Date.get_last_day_of_previous_month().replace(day=1).strftime('%Y-%m-%d')
            end_dt = utils.Date.get_last_day_of_previous_month().strftime('%Y-%m-%d')

            params = {
                'check-period-begin-dt': begin_dt,
                'check-period-end-dt': end_dt,
            }
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects, params)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_obb2(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'obb2'
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OBB2) as block:

            block.validate()
            rows_order = ['ExportID', 'EngineID', 'Shows', 'Clicks', 'Cost', 'CostCur', 'CurrencyCode',
                          'CostFinal', 'DShows', 'DCost', 'DCostCur']
            object_ids = []
            s3_data_list = {}
            bk_data_list = []
            yt_data_list = []

            for test_name in tests_list:
                if 'test_obb2' in test_name and shared.get_data_from_s3(test_name):
                    if shared.get_data_from_s3(test_name).get('order_info'):
                        data = shared.get_data_from_s3(test_name)
                        s3_data_list[test_name] = data

                        #  Решил использовать get_subitems - Возвращает значение, проходя указанным путём
                        #  по вложенным в указанный словарь словарям.
                        child = utils.get_subitem(data, 'order_info.child')
                        parent = utils.get_subitem(data, 'order_info.parent')
                        orders = utils.get_subitem(data, 'order_info.orders')

                        if child and parent:
                            object_ids.extend([
                                str(child['service_id']) + '-' + str(child['service_order_id']),
                                str(parent['service_id']) + '-' + str(parent['service_order_id'])
                            ])

                        elif orders:
                            object_ids.append(str(orders[1]['service_id']) + '-' + str(orders[1]['service_order_id']))

                        else:
                            order_info = utils.get_subitem(data, 'order_info')
                            object_ids.append(str(order_info['service_id']) + '-' + str(order_info['service_order_id']))

                            if data.get('yt_data'):
                                yt_data_list.extend(data['yt_data'])

                        if data.get('bk_data'):
                            bk_data_list.append('\t'.join(data['bk_data']))

                    elif shared.get_data_from_s3(test_name).get('cache_var'):
                        s3_data_list[test_name] = shared.get_data_from_s3(test_name)

            SharedSteps.obb2_data_for_yt(yt_data_list)

            orders = '0\n#' + '\t'.join(rows_order) + '\n'
            one_order = '\n'.join(bk_data_list)
            orders += one_order + '\n#End'

            create_data_file_in_s3(
                content=orders,
                file_name='bk_completions.csv',
                db_key='obb2_bk_importer_url',
            )

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_obg(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'obg'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OBG) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            orders = []

            for test_name in tests_list:
                if 'test_obg' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('service_order_id'):
                        object_ids.append(str(s3_data_list[test_name]['service_order_id']))


                    if s3_data_list[test_name].get('geo_data'):
                        one_order = {"paid_qty": str(0),
                                     "spent_qty": str(s3_data_list[test_name]['geo_data'][1]),
                                     "order_id": str(s3_data_list[test_name]['geo_data'][0]),
                                     "service_id": Services.geo}
                        orders.append(one_order)

            create_data_file_in_s3(
                content=json.dumps({'report': orders}),
                file_name='geocontext.json',
                db_key='obg_geo_importer_url',
            )

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_obg2(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'obg2'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OBG2) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            orders = []
            start_dt = None
            end_dt = None
            completions = {'yt_data':[]}

            for test_name in tests_list:
                if 'test_obg2' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)

                    if not start_dt and 'start_dt' in s3_data_list[test_name]:
                        start_dt = s3_data_list[test_name]['start_dt']
                    if not end_dt and 'end_dt' in s3_data_list[test_name]:
                        end_dt = s3_data_list[test_name]['end_dt']

                    if s3_data_list[test_name].get('service_order_id'):
                        object_ids.append(str(s3_data_list[test_name]['service_order_id']))

                    if s3_data_list[test_name].get('geo_completions'):
                        for completion in s3_data_list[test_name].get('geo_completions'):
                            completions['yt_data'].append({
                                'order_id': s3_data_list[test_name]['service_order_id'],
                                'completion_delta': completion.get('amount'),
                                'dt': completion.get('dt'),
                            })

            SharedSteps.obg2_data_for_yt(completions['yt_data'])

            objects = ','.join(object_ids)

            check_args = dict()
            if start_dt:
                check_args['check-period-begin-dt'] = check_utils.str_date(start_dt)
            if end_dt:
                check_args['check-period-end-dt'] = check_utils.str_date(end_dt)

            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects, check_args)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs


    @staticmethod
    def run_zbg(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'zbg'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_ZBG) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            orders = []

            for test_name in tests_list:
                if 'test_zbg' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('order_info'):
                        object_ids.append(str(s3_data_list[test_name]['order_info']['service_order_id']))

                    if s3_data_list[test_name].get('geo_data'):
                        one_order = {
                            "paid_qty":   str(s3_data_list[test_name]['geo_data']['amount_a']),
                            "spent_qty":  str(0),
                            "order_id":   str(s3_data_list[test_name]['geo_data']['service_order_id']),
                            "service_id": s3_data_list[test_name]['geo_data']['service_id']}
                        orders.append(one_order)

            create_data_file_in_s3(
                content=json.dumps({'report': orders}),
                file_name='geocontext.json',
                db_key='zbg_geo_importer_url',
            )

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_iob(shared_data, before, tests_list, check_name='iob'):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.block_by_check_name(check_name)) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if SharedBlocks.check_test_name_iob(check_name, test_name):
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name] and s3_data_list[test_name].get('invoices'):
                        if s3_data_list[test_name]['invoices'].get('eid'):
                            object_ids.append(str(s3_data_list[test_name]['invoices'].get('eid').encode('utf8')))

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(check_name, objects)

            diffs = db.get_cmp_diff(cmp_id,
                                    cmp_name=check_name if check_name in ['iob_auto', 'iob_sw', 'iob_tr', 'iob_ua',
                                                                          'iob_us'] else 'iob')

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)

            return diffs



    @staticmethod
    def run_zmb(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'zmb'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_ZMB) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            market_data_list = []

            for test_name in tests_list:
                if 'test_zmb' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('market_data'):
                        object_ids.append(str(s3_data_list[test_name]['market_data']['service_order_id']))
                        market_data_list.append(s3_data_list[test_name])

                    elif s3_data_list[test_name].get('service_order_id'):
                        object_ids.append(str(s3_data_list[test_name]['service_order_id']))

            create_data_in_market(market_data_list)

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_oba2(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'oba2'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OBA2) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            ado_data_list = []

            for test_name in tests_list:
                if 'test_oba2' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    print ('>>>>>>> [S3_DATA_LIST]: {}'.format(s3_data_list))
                    print ('>>>>>>> [TEST_NAME]: {}'.format(test_name))
                    if s3_data_list[test_name].get('ado_data'):
                        object_ids.append(str(s3_data_list[test_name]['ado_data']['service_order_id']))
                        ado_data_list.append(s3_data_list[test_name].get('ado_data'))

                    elif s3_data_list[test_name].get('service_order_id'):
                        object_ids.append(str(s3_data_list[test_name]['service_order_id']))

            create_data_in_ado(ado_data_list,
                               db_key='oba2_ad_office_importer_url')

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_obn(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'obn'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OBN) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            navi_data_list = []

            for test_name in tests_list:
                if 'test_obn' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('navi_data'):
                        object_ids.append(str(s3_data_list[test_name]['navi_data']['order_id']))
                        navi_data_list.append(s3_data_list[test_name]['navi_data'])

                    elif s3_data_list[test_name].get('service_order_id'):
                        object_ids.append(str(s3_data_list[test_name]['service_order_id']))

            create_data_in_navi(navi_data_list)

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects,
                                               {'cut-dt': datetime.now().strftime('%Y-%m-%d')})

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_obar(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'obar'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_OBAR) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            orders = []
            orders_auto = []

            for test_name in tests_list:
                if 'test_obar' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)

                    if s3_data_list[test_name].get('service_order_id'):
                        object_ids.append(str(s3_data_list[test_name]['service_order_id']))

                    if s3_data_list[test_name].get('auto_data'):

                        s3_test_data = s3_data_list[test_name]['auto_data']

                        if s3_data_list[test_name].get('auto_analysis'):

                            order = {
                                'orderId':       int(s3_test_data['service_order_id']),
                                'date':          str(s3_test_data['dt']),
                                'completionQty': float(s3_test_data['completion_qty']),
                                'sentTime':      str(s3_test_data['sent_dt']),
                            }
                            orders_auto.append(order)

                        if 'test_sent_completions_auto_analysis_with_diff' in test_name:
                            s3_test_data['completion_qty'] = int(s3_data_list[test_name]['auto_ru_completion_qty'])

                        one_order = {
                                    "orderId":       int(s3_test_data['service_order_id']),
                                    "consumeQty":    float(s3_test_data['consume_qty']),
                                    "completionQty": float(s3_test_data['completion_qty']),
                                    "unit":            str(s3_test_data['unit']),
                                    "dt":              str(s3_test_data['dt'])
                                     }
                        orders.append(one_order)

            create_data_file_in_s3(
                content=json.dumps(orders),
                file_name='obar.json',
                db_key='obar_auto_ru_importer_url',
            )
            reporter.log(orders)

            if orders_auto:
                create_data_file_in_s3(
                    content=json.dumps(orders_auto),
                    file_name='obar_sent_spendings.json',
                    db_key='obar_sent_completions_url',
                )
                reporter.log(orders_auto)

            date = (datetime.now() + timedelta(days=3)).strftime('%Y-%m-%d')
            params = {
                'max-order-creation-dt': date,
                'operations-dt': date,
            }

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects, params)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)
            reporter.log("DIFFS = %s " % diffs)

            for test_name in tests_list:
                if 'test_sent_completions_auto_analysis_with_diff' in test_name:
                    order_id_with_diff = int(s3_data_list[test_name]['service_order_id'])
                    diffs.append({'order_id': order_id_with_diff, 'state': 'with_diff'})
                if 'test_sent_completions_auto_analysis_qty_diff' in test_name:
                    order_id_qty_diff = int(s3_data_list[test_name]['service_order_id'])
                    diffs.append({'order_id': order_id_qty_diff, 'state': 'qty_diff'})

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_cpbt(shared_data, before, tests_list):
        from btestlib import environments as env
        env.SimpleapiEnvironment.switch_param() # Disable service's params

        CHECK_CODE_NAME = 'cpbt'
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_CPBT) as block:

            block.validate()

            object_ids = []
            s3_data_list = {}
            yt_data_list = []

            for test_name in tests_list:
                if 'test_cpbt.py' in test_name and shared.get_data_from_s3(test_name):
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)

                    if s3_data_list[test_name].get('yt_data'):
                        yt_data_list.extend(s3_data_list[test_name]['yt_data'])

                    if s3_data_list[test_name].get('partner_id'):
                        object_ids.append(str(s3_data_list[test_name]['partner_id']))

                    if s3_data_list[test_name].get('taxi_client_id_'):
                        object_ids.append(str(s3_data_list[test_name]['taxi_client_id_']))


            SharedSteps.cpbt_data_for_yt(yt_data_list)

            begin_dt = (datetime.now()).strftime('%Y-%m-%d')
            end_dt = (datetime.now()).strftime('%Y-%m-%d')
            params = {
                'check-period-begin-dt': begin_dt,
                'check-period-end-dt': end_dt,
            }

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects, params)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_tbs(shared_data, before, tests_list):
        from btestlib import environments as env
        env.SimpleapiEnvironment.switch_param()  # Disable service's params

        CHECK_CODE_NAME = 'tbs'
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_TBS) as block:

            block.validate()

            object_ids = []
            s3_data_list = {}
            yt_data_list = []

            for test_name in tests_list:
                if 'test_tbs' in test_name and shared.get_data_from_s3(test_name):
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)

                    if s3_data_list[test_name].get('scouts_data'):
                        yt_data_list.extend(s3_data_list[test_name]['scouts_data'])

                    if s3_data_list[test_name].get('client_id'):
                        object_ids.append(str(s3_data_list[test_name]['client_id']))

                    if s3_data_list[test_name].get('client_id_missmatch'):
                            object_ids.append(str(s3_data_list[test_name]['client_id_missmatch']))


            SharedSteps.tbs_data_for_yt(yt_data_list)

            begin_dt = (datetime.now()).strftime('%Y-%m-%d')
            end_dt = (datetime.now()).strftime('%Y-%m-%d')
            params = {
                'check-period-begin-dt': begin_dt,
                'check-period-end-dt': end_dt,
            }

            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects, params)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_uao(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'uao'
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_UAO) as block:

            block.validate()

            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_uao' in test_name and shared.get_data_from_s3(test_name):
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('orders_map'):

                        for key in s3_data_list[test_name]['orders_map']:
                            id = s3_data_list[test_name]['orders_map'][key]['id']
                            object_ids.append(str(id))

            objects = ','.join(object_ids)
            date = datetime.now().strftime('%Y-%m-%d')
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects, {'completions-dt': date})

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_caob(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'caob'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_CAOB) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_caob' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    print ('>>>>>>> [S3_DATA_LIST]: {}'.format(s3_data_list))
                    print ('>>>>>>> [TEST_NAME]: {}'.format(test_name))
                    if s3_data_list[test_name].get('contract_id'):
                        object_ids.append(str(s3_data_list[test_name]['contract_id']))
                    if s3_data_list[test_name].get('contract_id_child'):
                        object_ids.append(str(s3_data_list[test_name]['contract_id_child']))


            objects = ','.join(object_ids)
            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_ccaob(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'ccaob'
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_CCAOB) as block:

            block.validate()

            object_ids = []
            s3_data_list = {}

            for test_name in tests_list:
                if 'test_ccaob' in test_name and shared.get_data_from_s3(test_name):
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('contract_id'):
                        id = s3_data_list[test_name]['contract_id']
                        object_ids.append(str(id))

            objects = ','.join(object_ids)

            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects)
            # cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects, '')

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs


    @staticmethod
    def run_arob(shared_data, before, tests_list):
        CHECK_CODE_NAME = 'arob'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_AROB) as block:
            block.validate()
            object_ids = []
            s3_data_list = {}
            data_list_for_insert = []

            for test_name in tests_list:
                if 'test_arob' in test_name:
                    s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                    if s3_data_list[test_name].get('contract_id'):
                        object_ids.append(str(s3_data_list[test_name]['contract_id']))


            objects = ','.join(object_ids)
            begin_dt = (datetime.now().replace(day=1) - timedelta(weeks=5)).strftime('%Y-%m-%d')
            end_dt = datetime.now().strftime('%Y-%m-%d')
            raw_cmd_args = {'begin-dt': begin_dt,
                            'end-dt': end_dt
                            }

            cmp_id = check_utils.run_check_new(CHECK_CODE_NAME, objects, raw_cmd_args)

            diffs = db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)

            for test_name in s3_data_list:
                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def run_cbf(shared_data, before, tests_list):
        check_code_name = 'cbf'
        test_class_name = 'TestCbf'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_CBF) as block:
            block.validate()

            s3_data_list = {}

            clients = []
            yt_data_list = []

            for test_name in tests_list:
                if test_class_name not in test_name:
                    continue

                s3_data = s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                if s3_data is None:
                    continue

                client_id = s3_data.get('client_id')
                yt_data = s3_data.get('yt_data')

                if client_id:
                    clients.append(str(client_id))
                if yt_data:
                    yt_data_list.append(yt_data)

            objects = ','.join(clients)
            if yt_data_list:
                SharedSteps.cbf_data_for_yt(yt_data_list)

            cmp_id = check_utils.run_check_new(check_code_name, objects)
            diffs = db.get_cmp_diff(cmp_id, check_code_name)

            for test_name in s3_data_list:
                if s3_data_list[test_name] is None:
                    continue

                s3_data_list[test_name].update({'cmp_data': diffs})
                shared.push_data_to_s3(s3_data_list[test_name], test_name)

            return diffs

    @staticmethod
    def run_pbf(shared_data, before, tests_list):
        check_code_name = 'pbf'
        test_class_name = 'TestPbf'

        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.RUN_PBF) as block:
            block.validate()

            s3_data_list = {}

            objects = []
            transactions = []
            yt_data_list = []

            begin_dt = TRANSACTION_LOG_DT
            end_dt = ACT_DT

            begin_dt_str = begin_dt.strftime('%Y-%m-%d')
            end_dt_str = end_dt.strftime('%Y-%m-%d')

            for test_name in tests_list:
                if test_class_name not in test_name:
                    continue

                s3_data = s3_data_list[test_name] = shared.get_data_from_s3(test_name)
                if s3_data is None:
                    continue

                client_id = s3_data.get('client_id')
                yt_data = s3_data.get('yt_data')
                transaction_id = s3_data.get('transaction_id')

                if client_id:
                    objects.append(str(client_id))
                if transaction_id:
                    transactions.append(str(transaction_id))
                if yt_data:
                    if isinstance(yt_data, types.DictionaryType):
                        yt_data_list.append(yt_data)
                    else:
                        yt_data_list.extend(yt_data)

            if yt_data_list:
                SharedSteps.pbf_data_for_yt(begin_dt_str, yt_data_list)

            params = {
                'check-period-begin-dt': begin_dt_str,
                'check-period-end-dt': end_dt_str,
                'transactions': ','.join(transactions),
            }
            cmp_id = check_utils.run_check_new(check_code_name, ','.join(objects), params)
            diffs = db.get_cmp_diff(cmp_id, check_code_name)

            for test_name in tests_list:
                if test_class_name not in test_name:
                    continue

                if s3_data_list[test_name] is not None:
                    s3_data_list[test_name].update({'cmp_data': diffs})
                    shared.push_data_to_s3(s3_data_list[test_name], test_name)
            return diffs

    @staticmethod
    def get_cmp_data(test_namespace, tests_list):
        for test_name in tests_list:
            if test_namespace not in test_name:
                continue

            s3_data = shared.get_data_from_s3(test_name)
            if s3_data is None:
                continue

            cmp_data = s3_data.get('cmp_data')
            if cmp_data is not None:
                return cmp_data

