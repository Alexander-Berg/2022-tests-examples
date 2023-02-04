# coding: utf-8

import itertools
import json
from copy import deepcopy

import pytest

from review.lib import encryption, datetimes
from review.bi.models import (
    BICurrencyConversionRate,
    BIPersonAssignment,
    BIPersonIncome,
    BIPersonDetailedIncome,
    BIPersonVesting,
)

INCOME_SAMPLE_YT_RECORDS = [
    {
        'ANNOUNCED_FLAG': 'N',
        'AVG_SAL': 100500.0,
        'CAL01_BONUS': 180519.0,
        'CAL01_DEFPAY': 18519.0,
        'CAL01_INCOME': 2160366.0,
        'CAL01_OTHER': 185129.0,
        'CAL01_PERIOD': '2022-05-01',
        'CAL01_PIECERATE': 60366.0,
        'CAL01_RETENTION': 603636.0,
        'CAL01_SALARY': 4000.0,
        'CAL01_SIGNUP': 13519.0,
        'CAL01_SIGNUP2': 0.0,
        'CAL01_VESTCASHPAY': 16000.0,
        'CAL01_VESTING': 46257,
        'CAL01_WELCOME': 80519.0,
        'CAL02_BONUS': 130650.0,
        'CAL02_DEFPAY': 0.0,
        'CAL02_INCOME': 1336650.0,
        'CAL02_PERIOD': '2023-01-01',
        'CAL02_SALARY': 1206000.0,
        'CAL02_VESTCASHPAY': 160300.0,
        'CAL02_VESTING': 0,
        'CAL02_WELCOME': 0.0,
        'CAL03_BONUS': 13519.0,
        'CAL03_DEFPAY': 1809.0,
        'CAL03_INCOME': 6000.0,
        'CAL03_PERIOD': '2024-01-01',
        'CAL03_SALARY': 1206000.0,
        'CAL03_VESTCASHPAY': 160030.0,
        'CAL03_VESTING': 0,
        'CAL03_WELCOME': 180549.0,
        'CAL04_BONUS': 130650.0,
        'CAL04_DEFPAY': 123519.0,
        'CAL04_INCOME': 1336650.0,
        'CAL04_PERIOD': '2025-01-01',
        'CAL04_SALARY': 1206000.0,
        'CAL04_VESTCASHPAY': 160003.0,
        'CAL04_VESTING': 0,
        'CAL04_WELCOME': 0.0,
        'CAL1_BONUS': 16032.0,
        'CAL1_DEFPAY': 180219.0,
        'CAL1_INCOME': 2000.0,
        'CAL1_OTHER': 8354.92,
        'CAL1_PERIOD': '2022-01-01',
        'CAL1_PIECERATE': 18419.0,
        'CAL1_RETENTION': 60546.0,
        'CAL1_SALARY': 2000.0,
        'CAL1_SIGNUP': 16030.0,
        'CAL1_SIGNUP2': 8354.9,
        'CAL1_VESTCASHPAY': 13000.0,
        'CAL1_VESTING': 18039.0,
        'CAL1_WELCOME': 0.0,
        'CAL2_BONUS': 160200.0,
        'CAL2_DEFPAY': 143519.0,
        'CAL2_INCOME': 508334.93,
        'CAL2_OTHER': 0.0,
        'CAL2_PERIOD': '2021-01-01',
        'CAL2_PIECERATE': 0.0,
        'CAL2_RETENTION': 604566.0,
        'CAL2_SALARY': 508334.93,
        'CAL2_SIGNUP': 18529.0,
        'CAL2_SIGNUP2': 6543.3,
        'CAL2_VESTCASHPAY': 83354.9,
        'CAL2_VESTING': 162300.0,
        'CAL2_WELCOME': 160340.0,
        'CAL3_BONUS': 2345.65,
        'CAL3_DEFPAY': 160023.0,
        'CAL3_INCOME': '2020-01-01',
        'CAL3_OTHER': 0.0,
        'CAL3_PERIOD': 83654.9,
        'CAL3_PIECERATE': 160020.0,
        'CAL3_RETENTION': 503334.93,
        'CAL3_SALARY': 0.0,
        'CAL3_SIGNUP': 508354.93,
        'CAL3_SIGNUP2': 0.0,
        'CAL3_VESTCASHPAY': 234125.6,
        'CAL3_VESTING': 56000.0,
        'CAL3_WELCOME': 0.0,
        'CAL4_BONUS': 16002.0,
        'CAL4_DEFPAY': 502334.93,
        'CAL4_INCOME': 81354.9,
        'CAL4_OTHER': 0.0,
        'CAL4_PERIOD': '2019-01-01',
        'CAL4_PIECERATE': 18559.0,
        'CAL4_RETENTION': 62366.0,
        'CAL4_SALARY': 16020.0,
        'CAL4_SIGNUP': 0.0,
        'CAL4_SIGNUP2': 18512.0,
        'CAL4_VESTCASHPAY': 0.0,
        'CAL4_VESTING': 16025.0,
        'CAL4_WELCOME': 23415.6,
        'EFF01_BEG': '2022-05-01',
        'EFF01_BONUS': 12519.0,
        'EFF01_DEFPAY': 16000.56,
        'EFF01_END': '2023-04-30',
        'EFF01_INCOME': 6000.0,
        'EFF01_OTHER': 23425.6,
        'EFF01_PIECERATE': 13000.0,
        'EFF01_RETENTION': 60326.0,
        'EFF01_SALARY': 6000.0,
        'EFF01_SIGNUP': 8743.2,
        'EFF01_SIGNUP2': 0.0,
        'EFF01_VESTCASHPAY': 2345.63,
        'EFF01_VESTING': 0,
        'EFF01_WELCOME': 12519.0,
        'EFF02_BEG': '2023-05-01',
        'EFF02_BONUS': 130650.0,
        'EFF02_DEFPAY': 84354.9,
        'EFF02_END': '2024-04-30',
        'EFF02_INCOME': 1336650.0,
        'EFF02_SALARY': 1206000.0,
        'EFF02_VESTCASHPAY': 0.0,
        'EFF02_VESTING': 0,
        'EFF02_WELCOME': 56344.2,
        'EFF03_BEG': '2024-05-01',
        'EFF03_BONUS': 18539.0,
        'EFF03_DEFPAY': 23354.12,
        'EFF03_END': '2025-04-30',
        'EFF03_INCOME': 6000.0,
        'EFF03_SALARY': 1206000.0,
        'EFF03_VESTCASHPAY': 0.0,
        'EFF03_VESTING': 0,
        'EFF03_WELCOME': 48519.0,
        'EFF04_BEG': '2025-05-01',
        'EFF04_BONUS': 130650.0,
        'EFF04_DEFPAY': 83354.9,
        'EFF04_END': '2026-04-30',
        'EFF04_INCOME': 1336650.0,
        'EFF04_SALARY': 1206000.0,
        'EFF04_VESTCASHPAY': 98334.2,
        'EFF04_VESTING': 2345.6,
        'EFF04_WELCOME': 58619.0,
        'EFF1_BEG': '2021-05-01',
        'EFF1_BONUS': 2354.45,
        'EFF1_DEFPAY': 38519.0,
        'EFF1_END': '2022-04-30',
        'EFF1_INCOME': 29863.64,
        'EFF1_OTHER': 86354.9,
        'EFF1_PIECERATE': 25000.0,
        'EFF1_RETENTION': 20366.0,
        'EFF1_SALARY': 4863.64,
        'EFF1_SIGNUP': 164500.0,
        'EFF1_SIGNUP2': 2354.21,
        'EFF1_VESTCASHPAY': 0.0,
        'EFF1_VESTING': 83454.9,
        'EFF1_WELCOME': 0.0,
        'EFF2_BEG': '2020-05-01',
        'EFF2_BONUS': 0.0,
        'EFF2_DEFPAY': 2354.53,
        'EFF2_END': '2021-04-30',
        'EFF2_INCOME': 83545.9,
        'EFF2_OTHER': 160230.0,
        'EFF2_PIECERATE': 0.0,
        'EFF2_RETENTION': 30366.0,
        'EFF2_SALARY': 0.0,
        'EFF2_SIGNUP': 18315.0,
        'EFF2_SIGNUP2': 0.0,
        'EFF2_VESTCASHPAY': 0.0,
        'EFF2_VESTING': 0.0,
        'EFF2_WELCOME': 8354.39,
        'EFF3_BEG': '2019-05-01',
        'EFF3_BONUS': 45332.3,
        'EFF3_DEFPAY': 0.0,
        'EFF3_END': '2020-04-30',
        'EFF3_INCOME': 83254.9,
        'EFF3_OTHER': 4532.3,
        'EFF3_PIECERATE': 0.0,
        'EFF3_RETENTION': 78366.0,
        'EFF3_SALARY': 0.0,
        'EFF3_SIGNUP': 83546.9,
        'EFF3_SIGNUP2': 45362.3,
        'EFF3_VESTCASHPAY': 0.0,
        'EFF3_VESTING': 0.0,
        'EFF3_WELCOME': 14532.3,
        'EFF4_BEG': '2018-05-01',
        'EFF4_BONUS': 0.0,
        'EFF4_DEFPAY': 832354.9,
        'EFF4_END': '2019-04-30',
        'EFF4_INCOME': 2354.3,
        'EFF4_OTHER': 2354.2,
        'EFF4_PIECERATE': 45323.3,
        'EFF4_RETENTION': 60266.0,
        'EFF4_SALARY': 14539.0,
        'EFF4_SIGNUP': 0.0,
        'EFF4_SIGNUP2': 15529.0,
        'EFF4_VESTCASHPAY': 18513.0,
        'EFF4_VESTING': 83564.9,
        'EFF4_WELCOME': 2354.63,
        'GOLD_OPT_CURR': True,
        'GOLD_OPT_LAST': None,
        'GOLD_PAY_CURR': None,
        'GOLD_PAY_LAST': None,
        'GRADE_CURRENT': 15,
        'GRADE_LAST': 14,
        'GRADE_MAIN': 15,
        'HR_DEPARTMENT': 'Контактный центр Яндекс GO в Казахстане (Поддержка бизнеса)',
        'LOGIN': 'test_user',
        'MAIN_ASG_CURRENCY': 'UAH',
        'MARK_CURRENT': 'GOOD',
        'MARK_LAST': 'E',
        'PROFESSION': 'SupportOperator',
        'SALARY': 232340.0,
        'UP_CURRENT': -1,
        'UP_LAST': 1,
    },
    {
        'LOGIN': 'another_user',
    },
]

DETAILED_INCOME_SAMPLE_YT_RECORDS = [
    {
        'LOGIN': 'test_user',
        'MTH_ID': 201901,
        'YEAR_ID': 2019,
        'SALARY': 232340.0,
        'MAIN_ASG_CURRENCY': 'UAH',
        'MARK_LAST': 'E',
        'GRADE_LAST': 14,
        'UP_LAST': 1,
        'GRADE_CURRENT': 15,
        'MARK_CURRENT': 'GOOD',
        'UP_CURRENT': -1,
        'GRADE_MAIN': 15,
        'GOLD_OPT_CURR': True,
        'GOLD_PAY_CURR': None,
        'GOLD_OPT_LAST': None,
        'GOLD_PAY_LAST': None,
        'HR_DEPARTMENT': 'Контактный центр Яндекс GO в Казахстане (Поддержка бизнеса)',
        'PROFESSION': 'SupportOperator',
        'AVG_SAL': 100500.0,
        'ANNOUNCED_FLAG': 'N',
        'SALARY_SUM': 4000.0,
        'BONUS_SUM': 180519.0,
        'OTHER_SUM': 185129.0,
        'SIGNUP_SUM': 13519.0,
        'SIGNUP2_SUM': 0.0,
        'RETENTION_SUM': 603636.0,
        'VEST_SUM': 46257,
        'PIECERATE_SUM': 60366.0,
        'WELCOME_SUM': 80519.0,
        'VESTCASHPAY_SUM': 16000.0,
        'DEFPAY_SUM': 18519.0,
        'INCOME_SUM': 2160366.0,
    },
    {
        'LOGIN': 'test_user',
        'MTH_ID': 201902,
        'YEAR_ID': 2019,
        'SALARY': 232340.0,
        'MAIN_ASG_CURRENCY': 'UAH',
        'MARK_LAST': 'E',
        'GRADE_LAST': 14,
        'UP_LAST': 1,
        'GRADE_CURRENT': 15,
        'MARK_CURRENT': 'GOOD',
        'UP_CURRENT': -1,
        'GRADE_MAIN': 15,
        'GOLD_OPT_CURR': True,
        'GOLD_PAY_CURR': None,
        'GOLD_OPT_LAST': None,
        'GOLD_PAY_LAST': None,
        'HR_DEPARTMENT': 'Контактный центр Яндекс GO в Казахстане (Поддержка бизнеса)',
        'PROFESSION': 'SupportOperator',
        'AVG_SAL': 10500.0,
        'ANNOUNCED_FLAG': 'N',
        'SALARY_SUM': 4000.0,
        'BONUS_SUM': 18059.0,
        'OTHER_SUM': 18519.0,
        'SIGNUP_SUM': 13519.0,
        'SIGNUP2_SUM': 0.0,
        'RETENTION_SUM': 603636.0,
        'VEST_SUM': 4657,
        'PIECERATE_SUM': 6036.0,
        'WELCOME_SUM': 80519.0,
        'VESTCASHPAY_SUM': 1000.0,
        'DEFPAY_SUM': 1819.0,
        'INCOME_SUM': 216036.0,
    },
    {
        'LOGIN': 'test_user',
        'MTH_ID': 203003,  # far in the future
        'YEAR_ID': 2030,  # far in the future
        'SALARY': 2325340.0,
        'MAIN_ASG_CURRENCY': 'UAH',
        'MARK_LAST': 'E',
        'GRADE_LAST': 14,
        'UP_LAST': 1,
        'GRADE_CURRENT': 15,
        'MARK_CURRENT': 'GOOD',
        'UP_CURRENT': -1,
        'GRADE_MAIN': 15,
        'GOLD_OPT_CURR': True,
        'GOLD_PAY_CURR': None,
        'GOLD_OPT_LAST': None,
        'GOLD_PAY_LAST': None,
        'HR_DEPARTMENT': 'Контактный центр Яндекс GO в Казахстане (Поддержка бизнеса)',
        'PROFESSION': 'SupportOperator',
        'AVG_SAL': 1005400.0,
        'ANNOUNCED_FLAG': 'N',
        'SALARY_SUM': 40600.0,
        'BONUS_SUM': 1820519.0,
        'OTHER_SUM': 1851529.0,
        'SIGNUP_SUM': 163519.0,
        'SIGNUP2_SUM': 6456.0,
        'RETENTION_SUM': 6043636.0,
        'VEST_SUM': 466257,
        'PIECERATE_SUM': 60366.0,
        'WELCOME_SUM': 805619.0,
        'VESTCASHPAY_SUM': 146000.0,
        'DEFPAY_SUM': 185159.0,
        'INCOME_SUM': 2106366.0,
    },
]

ORG_ID = 123

ASSIGNMENT_SAMPLE_YT_RECORDS = [
    {
        'LOGIN': 'test_user',
        'ASSIGNMENT_ID': 8897,
        'ASSIGNMENT_NUMBER': '2590-23664',
        'ASG_SALARY': 135422.0,
        'ASG_CURRENCY': 'UAH',
        'REPORT_DATE': '2020-01-31',
        'ASG_RATE': 0.7,
        'ASG_LEGAL_ENTITY_ID': 123,
        'ASG_LEGAL_ENTITY_NAME': 'ООО Яндекс.Технологии',
        'ASG_JOB_NAME': 'Журналист',
        'ASG_CONTRACT_NUMBER': '№ 011/12/0010 от 11.01.2012',
        'MAIN_ASG_FLAG': 'Y',
        'ASG_RATE_FACT': 1.0,
        'ASG_CONTR_END_DATE': '2020-01-31',
    },
    {
        'LOGIN': 'test_user',
        'ASSIGNMENT_ID': 8898,
        'ASSIGNMENT_NUMBER': '2590-23665',
        'ASG_SALARY': 135423.0,
        'ASG_CURRENCY': 'UAH',
        'REPORT_DATE': '2020-01-31',
        'ASG_RATE': 0.7,
        'ASG_LEGAL_ENTITY_ID': 124,
        'ASG_LEGAL_ENTITY_NAME': 'Yandex.Taxi AM',
        'ASG_JOB_NAME': 'Фотокорреспондент',
        'ASG_CONTRACT_NUMBER': '№ 011/12/0010 от 11.01.2012',
        'MAIN_ASG_FLAG': 'N',
        'ASG_RATE_FACT': 1.0,
        'ASG_CONTR_END_DATE': '2020-01-31',
    },
    {
        'LOGIN': 'another_user',
        'ASSIGNMENT_ID': 8899,
        'ASSIGNMENT_NUMBER': '2590-23666',
        'ASG_SALARY': 135424.0,
        'ASG_CURRENCY': 'UAH',
        'REPORT_DATE': '2020-01-31',
        'ASG_RATE': 1.0,
        'ASG_LEGAL_ENTITY_ID': 124,
        'ASG_LEGAL_ENTITY_NAME': 'Yandex.Taxi AM',
        'ASG_JOB_NAME': 'Фотокорреспондент',
        'ASG_CONTRACT_NUMBER': '№ 011/12/0010 от 11.01.2012',
        'MAIN_ASG_FLAG': 'Y',
        'ASG_RATE_FACT': 1.0,
        'ASG_CONTR_END_DATE': '2020-01-31',
    },
]

VESTING_SAMPLE_YT_RECORDS = [
    {
        'CAL01_LE_PERIOD': '2022-05-01',
        'CAL01_LE_VESTING': 0,
        'CAL02_LE_PERIOD': '2023-01-01',
        'CAL02_LE_VESTING': 0,
        'CAL03_LE_PERIOD': '2024-01-01',
        'CAL03_LE_VESTING': 0,
        'CAL04_LE_PERIOD': '2025-01-01',
        'CAL04_LE_VESTING': 0,
        'CAL1_LE_PERIOD': '2022-01-01',
        'CAL1_LE_VESTING': 0.0,
        'CAL2_LE_PERIOD': '2021-01-01',
        'CAL2_LE_VESTING': 0.0,
        'CAL3_LE_PERIOD': '2020-01-01',
        'CAL3_LE_VESTING': 0.0,
        'CAL4_LE_PERIOD': '2019-01-01',
        'CAL4_LE_VESTING': 0.0,
        'EFF01_LE_BEG': '2022-05-01',
        'EFF01_LE_END': '2023-04-30',
        'EFF01_LE_VESTING': 0,
        'EFF02_LE_BEG': '2023-05-01',
        'EFF02_LE_END': '2024-04-30',
        'EFF02_LE_VESTING': 0,
        'EFF03_LE_BEG': '2024-05-01',
        'EFF03_LE_END': '2025-04-30',
        'EFF03_LE_VESTING': 0,
        'EFF04_LE_BEG': '2025-05-01',
        'EFF04_LE_END': '2026-04-30',
        'EFF04_LE_VESTING': 0,
        'EFF1_LE_BEG': '2021-05-01',
        'EFF1_LE_END': '2022-04-30',
        'EFF1_LE_VESTING': 0.0,
        'EFF2_LE_BEG': '2020-05-01',
        'EFF2_LE_END': '2021-04-30',
        'EFF2_LE_VESTING': 0.0,
        'EFF3_LE_BEG': '2019-05-01',
        'EFF3_LE_END': '2020-04-30',
        'EFF3_LE_VESTING': 0.0,
        'EFF4_LE_BEG': '2018-05-01',
        'EFF4_LE_END': '2019-04-30',
        'EFF4_LE_VESTING': 0.0,
        'LOGIN': 'test_user',
        'VEST_LE_NAME': 'Yandex.Classifieds Holding B.V.',
    },
    {
        'CAL01_LE_PERIOD': '2022-05-01',
        'CAL01_LE_VESTING': 0,
        'CAL02_LE_PERIOD': '2023-01-01',
        'CAL02_LE_VESTING': 0,
        'CAL03_LE_PERIOD': '2024-01-01',
        'CAL03_LE_VESTING': 0,
        'CAL04_LE_PERIOD': '2025-01-01',
        'CAL04_LE_VESTING': 0,
        'CAL1_LE_PERIOD': '2022-01-01',
        'CAL1_LE_VESTING': 0.0,
        'CAL2_LE_PERIOD': '2021-01-01',
        'CAL2_LE_VESTING': 0.0,
        'CAL3_LE_PERIOD': '2020-01-01',
        'CAL3_LE_VESTING': 0.0,
        'CAL4_LE_PERIOD': '2019-01-01',
        'CAL4_LE_VESTING': 0.0,
        'EFF01_LE_BEG': '2022-05-01',
        'EFF01_LE_END': '2023-04-30',
        'EFF01_LE_VESTING': 0,
        'EFF02_LE_BEG': '2023-05-01',
        'EFF02_LE_END': '2024-04-30',
        'EFF02_LE_VESTING': 0,
        'EFF03_LE_BEG': '2024-05-01',
        'EFF03_LE_END': '2025-04-30',
        'EFF03_LE_VESTING': 0,
        'EFF04_LE_BEG': '2025-05-01',
        'EFF04_LE_END': '2026-04-30',
        'EFF04_LE_VESTING': 0,
        'EFF1_LE_BEG': '2021-05-01',
        'EFF1_LE_END': '2022-04-30',
        'EFF1_LE_VESTING': 0.0,
        'EFF2_LE_BEG': '2020-05-01',
        'EFF2_LE_END': '2021-04-30',
        'EFF2_LE_VESTING': 0.0,
        'EFF3_LE_BEG': '2019-05-01',
        'EFF3_LE_END': '2020-04-30',
        'EFF3_LE_VESTING': 0.0,
        'EFF4_LE_BEG': '2018-05-01',
        'EFF4_LE_END': '2019-04-30',
        'EFF4_LE_VESTING': 0.0,
        'LOGIN': 'test_user',
        'VEST_LE_NAME': 'ООО Знание',
    }
]

INCOME_PARSED_DATA = {
    'per_calendar_year': [
        {
            'to': None,
            'forecast': False,
            'salary': 16020.0,
            'bonus': 16002.0,
            'other': 0.0,
            'signup': 0.0,
            'signup2': 18512.0,
            'retention': 62366.0,
            'vesting': 16025.0,
            'income': 81354.9,
            'piecerate': 18559.0,
            'welcome': 23415.6,
            'vesting_cash_payment': 0.0,
            'deferred_payment': 502334.93,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2019-01-01'
        },
        {
            'to': None,
            'forecast': False,
            'salary': 0.0,
            'bonus': 2345.65,
            'other': 0.0,
            'signup': 508354.93,
            'signup2': 0.0,
            'retention': 503334.93,
            'vesting': 56000.0,
            'income': 0.0,
            'piecerate': 160020.0,
            'welcome': 0.0,
            'vesting_cash_payment': 234125.6,
            'deferred_payment': 160023.0,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': 83654.9
        },
        {
            'to': None,
            'forecast': False,
            'salary': 508334.93,
            'bonus': 160200.0,
            'other': 0.0,
            'signup': 18529.0,
            'signup2': 6543.3,
            'retention': 604566.0,
            'vesting': 162300.0,
            'income': 508334.93,
            'piecerate': 0.0,
            'welcome': 160340.0,
            'vesting_cash_payment': 83354.9,
            'deferred_payment': 143519.0,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2021-01-01'
        },
        {
            'to': None,
            'forecast': False,
            'salary': 2000.0,
            'bonus': 16032.0,
            'other': 8354.92,
            'signup': 16030.0,
            'signup2': 8354.9,
            'retention': 60546.0,
            'vesting': 18039.0,
            'income': 2000.0,
            'piecerate': 18419.0,
            'welcome': 0.0,
            'vesting_cash_payment': 13000.0,
            'deferred_payment': 180219.0,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2022-01-01'
        },
        {
            'to': None,
            'forecast': True,
            'salary': 4000.0,
            'bonus': 180519.0,
            'other': 185129.0,
            'signup': 13519.0,
            'signup2': 0.0,
            'retention': 603636.0,
            'vesting': 46257.0,
            'income': 2160366.0,
            'piecerate': 60366.0,
            'welcome': 80519.0,
            'vesting_cash_payment': 16000.0,
            'deferred_payment': 18519.0,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2022-05-01'
        }
    ],
    'from_current_date': [
        {
            'to': '2019-04-30',
            'forecast': False,
            'salary': 14539.0,
            'bonus': 0.0,
            'other': 2354.2,
            'signup': 0.0,
            'signup2': 15529.0,
            'retention': 60266.0,
            'vesting': 83564.9,
            'income': 2354.3,
            'piecerate': 45323.3,
            'welcome': 2354.63,
            'vesting_cash_payment': 18513.0,
            'deferred_payment': 832354.9,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2018-05-01'
        },
        {
            'to': '2020-04-30',
            'forecast': False,
            'salary': 0.0,
            'bonus': 45332.3,
            'other': 4532.3,
            'signup': 83546.9,
            'signup2': 45362.3,
            'retention': 78366.0,
            'vesting': 0.0,
            'income': 83254.9,
            'piecerate': 0.0,
            'welcome': 14532.3,
            'vesting_cash_payment': 0.0,
            'deferred_payment': 0.0,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2019-05-01'
        },
        {
            'to': '2021-04-30',
            'forecast': False,
            'salary': 0.0,
            'bonus': 0.0,
            'other': 160230.0,
            'signup': 18315.0,
            'signup2': 0.0,
            'retention': 30366.0,
            'vesting': 0.0,
            'income': 83545.9,
            'piecerate': 0.0,
            'welcome': 8354.39,
            'vesting_cash_payment': 0.0,
            'deferred_payment': 2354.53,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2020-05-01'
        },
        {
            'to': '2022-04-30',
            'forecast': False,
            'salary': 4863.64,
            'bonus': 2354.45,
            'other': 86354.9,
            'signup': 164500.0,
            'signup2': 2354.21,
            'retention': 20366.0,
            'vesting': 83454.9,
            'income': 29863.64,
            'piecerate': 25000.0,
            'welcome': 0.0,
            'vesting_cash_payment': 0.0,
            'deferred_payment': 38519.0,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2021-05-01'
        },
        {
            'to': '2023-04-30',
            'forecast': True,
            'salary': 6000.0,
            'bonus': 12519.0,
            'other': 23425.6,
            'signup': 8743.2,
            'signup2': 0.0,
            'retention': 60326.0,
            'vesting': 0.0,
            'income': 6000.0,
            'piecerate': 13000.0,
            'welcome': 12519.0,
            'vesting_cash_payment': 2345.63,
            'deferred_payment': 16000.56,
            'vesting_detail': {
                'Yandex.Classifieds Holding B.V.': 0.0,
                'ООО Знание': 0.0
            },
            'from': '2022-05-01'
        }
    ],
    'detailed': [
        {
            'to': '2019-01-31',
            'forecast': False,
            'salary': 232340.0,
            'bonus': 180519.0,
            'other': 185129.0,
            'signup': 13519.0,
            'signup2': 0.0,
            'retention': 603636.0,
            'vesting': 46257.0,
            'income': 2160366.0,
            'piecerate': 60366.0,
            'welcome': 80519.0,
            'vesting_cash_payment': 16000.0,
            'deferred_payment': 18519.0,
            'vesting_detail': {},
            'from': '2019-01-01',
        },
        {
            'to': '2019-02-28',
            'forecast': False,
            'salary': 232340.0,
            'bonus': 18059.0,
            'other': 18519.0,
            'signup': 13519.0,
            'signup2': 0.0,
            'retention': 603636.0,
            'vesting': 4657.0,
            'income': 216036.0,
            'piecerate': 6036.0,
            'welcome': 80519.0,
            'vesting_cash_payment': 1000.0,
            'deferred_payment': 1819.0,
            'vesting_detail': {},
            'from': '2019-02-01',
        },
        {
            'to': '2030-03-31',
            'forecast': True,
            'salary': 2325340.0,
            'bonus': 1820519.0,
            'other': 1851529.0,
            'signup': 163519.0,
            'signup2': 6456.0,
            'retention': 6043636.0,
            'vesting': 466257.0,
            'income': 2106366.0,
            'piecerate': 60366.0,
            'welcome': 805619.0,
            'vesting_cash_payment': 146000.0,
            'deferred_payment': 185159.0,
            'vesting_detail': {},
            'from': '2030-03-01',
        }
    ],
}

INCOME_PARSED_DATA_LONG_FORECAST = deepcopy(INCOME_PARSED_DATA)
INCOME_PARSED_DATA_LONG_FORECAST['from_current_date'].extend([
    {
        'from': '2023-05-01',
        'to': '2024-04-30',
        'forecast': True,
        'salary': 1206000.0,
        'bonus': 130650.0,
        'other': 0.0,
        'signup': 0.0,
        'signup2': 0.0,
        'retention': 0.0,
        'vesting': 0.0,
        'income': 1336650.0,
        'piecerate': 0.0,
        'welcome': 56344.2,
        'vesting_cash_payment': 0.0,
        'deferred_payment': 84354.9,
        'vesting_detail': {
            'Yandex.Classifieds Holding B.V.': 0.0,
            'ООО Знание': 0.0,
        },
    },
    {
        'from': '2024-05-01',
        'to': '2025-04-30',
        'forecast': True,
        'salary': 1206000.0,
        'bonus': 18539.0,
        'other': 0.0,
        'signup': 0.0,
        'signup2': 0.0,
        'retention': 0.0,
        'vesting': 0.0,
        'income': 6000.0,
        'piecerate': 0.0,
        'welcome': 48519.0,
        'vesting_cash_payment': 0.0,
        'deferred_payment': 23354.12,
        'vesting_detail': {
            'Yandex.Classifieds Holding B.V.': 0.0,
            'ООО Знание': 0.0,
        },
    },
    {
        'from': '2025-05-01',
        'to': '2026-04-30',
        'forecast': True,
        'salary': 1206000.0,
        'bonus': 130650.0,
        'other': 0.0,
        'signup': 0.0,
        'signup2': 0.0,
        'retention': 0.0,
        'vesting': 2345.6,
        'income': 1336650.0,
        'piecerate': 0.0,
        'welcome': 58619.0,
        'vesting_cash_payment': 98334.2,
        'deferred_payment': 83354.9,
        'vesting_detail': {
            'Yandex.Classifieds Holding B.V.': 0.0,
            'ООО Знание': 0.0,
        },
    },
])

INCOME_PARSED_DATA_LONG_FORECAST['per_calendar_year'].extend([
    {
        'from': '2023-01-01',
        'to': None,
        'forecast': True,
        'salary': 1206000.0,
        'bonus': 130650.0,
        'other': 0.0,
        'signup': 0.0,
        'signup2': 0.0,
        'retention': 0.0,
        'vesting': 0.0,
        'income': 1336650.0,
        'piecerate': 0.0,
        'welcome': 0.0,
        'vesting_cash_payment': 160300.0,
        'deferred_payment': 0.0,
        'vesting_detail': {
            'Yandex.Classifieds Holding B.V.': 0.0,
            'ООО Знание': 0.0,
        },
    },
    {
        'from': '2024-01-01',
        'to': None,
        'forecast': True,
        'salary': 1206000.0,
        'bonus': 13519.0,
        'other': 0.0,
        'signup': 0.0,
        'signup2': 0.0,
        'retention': 0.0,
        'vesting': 0.0,
        'income': 6000.0,
        'piecerate': 0.0,
        'welcome': 180549.0,
        'vesting_cash_payment': 160030.0,
        'deferred_payment': 1809.0,
        'vesting_detail': {
            'Yandex.Classifieds Holding B.V.': 0.0,
            'ООО Знание': 0.0,
        },
    },
    {
        'from': '2025-01-01',
        'to': None,
        'forecast': True,
        'salary': 1206000.0,
        'bonus': 130650.0,
        'other': 0.0,
        'signup': 0.0,
        'signup2': 0.0,
        'retention': 0.0,
        'vesting': 0.0,
        'income': 1336650.0,
        'piecerate': 0.0,
        'welcome': 0.0,
        'vesting_cash_payment': 160003.0,
        'deferred_payment': 123519.0,
        'vesting_detail': {
            'Yandex.Classifieds Holding B.V.': 0.0,
            'ООО Знание': 0.0,
        },
    },
])


@pytest.fixture
def bi_assignment_builder(db, person_builder):
    def builder(ext_data={}, **kwargs):
        params = {}
        params.update(kwargs)
        if 'person' not in params:
            params['person'] = person_builder()

        parsed_data = ASSIGNMENT_SAMPLE_YT_RECORDS[0]
        parsed_data.update(**ext_data)
        if 'organization_id' in params:
            parsed_data['ASG_LEGAL_ENTITY_ID'] = params.pop('organization_id')
        params['data'] = parsed_data
        params['hash'] = hash(json.dumps(parsed_data, sort_keys=True))

        return BIPersonAssignment.objects.create(**params)
    return builder


@pytest.fixture
def bi_currency_coversion_builder(db, bi_assignment_builder):
    id_gen = itertools.count()

    def builder(**kwargs):
        params = dict(kwargs)
        params['id'] = next(id_gen)
        params.setdefault('organization_id', ORG_ID)
        params.setdefault('legal_entity_name', 'HOHOHO')
        params.setdefault('from_currency', 'RUB')
        params.setdefault('to_currency', 'USD')
        params.setdefault('conversion_date', datetimes.today())
        params.setdefault('rate', '32')
        params.setdefault('legal_entity_convert_type', 'have-no-idea-what-is-it')
        return BICurrencyConversionRate.objects.create(**params)

    return builder


@pytest.fixture
def bi_income_builder(db, person_builder):
    def builder(ext_data={}, **kwargs):
        params = {}
        params.update(kwargs)
        if 'person' not in params:
            params['person'] = person_builder()

        parsed_data = INCOME_SAMPLE_YT_RECORDS[0]
        parsed_data.update(ext_data)
        parsed_data = json.dumps(parsed_data, sort_keys=True)
        params['data'] = encryption.encrypt(parsed_data)
        params['hash'] = hash(parsed_data)

        return BIPersonIncome.objects.create(**params)
    return builder


@pytest.fixture
def bi_detailed_incomes_builder(db, person_builder):
    def builder(ext_data={}, **kwargs):
        result = []
        params = {}
        params.update(kwargs)
        params['person'] = params.get('person') or person_builder()
        parsed_data = [inc.copy() for inc in DETAILED_INCOME_SAMPLE_YT_RECORDS]
        for data in parsed_data:
            data['LOGIN'] = params['person'].login
            login = params['person'].login
            month_id = data['MTH_ID']

            data.update(ext_data)
            data = json.dumps(data, sort_keys=True)

            params['data'] = encryption.encrypt(data)
            params['hash'] = hash(data)
            params['unique'] = f'{login}_{month_id}'

            result.append(BIPersonDetailedIncome.objects.create(**params))
        return result

    return builder


@pytest.fixture
def bi_vesting_builder(db, person_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'person' not in params:
            params['person'] = person_builder()
        test_user_vesting_records = [
            VESTING_SAMPLE_YT_RECORDS[0].copy(),
            VESTING_SAMPLE_YT_RECORDS[1].copy(),
        ]
        test_user_vesting_sapmle = {
            record.pop('VEST_LE_NAME'): record
            for record in test_user_vesting_records
        }
        parsed_data = json.dumps(
            test_user_vesting_sapmle,
            sort_keys=True,
        )
        params['data'] = encryption.encrypt(parsed_data)
        params['hash'] = hash(parsed_data)

        return BIPersonVesting.objects.create(**params)
    return builder
