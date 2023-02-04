# -*- coding: utf-8 -*-

import pytest
from enum import Enum
import datetime
from decimal import Decimal as D

import hamcrest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import Firms, Currencies as c, CurrencyRateSource as crs
from export_commons import Locators, read_attr_values

pytestmark = [reporter.feature(Features.OEBS, Features.CLIENT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

'''
Посмотреть в коде какие атрибуты, как и куда выгружаются можно тут balance.processors.oebs.process_client (__init__.py)
'''

TODAY = datetime.datetime.now()
TOMORROW = TODAY + datetime.timedelta(days=1)
YESTERDAY = TODAY - datetime.timedelta(days=1)
DAY_BEFORE_YESTERDAY = YESTERDAY - datetime.timedelta(days=1)

PRECISION = D('0.00000001')


class currencyAttrs(Enum):
    rate = \
        Locators(balance=lambda b: b['t_currency_rate_v2.rate'],
                 oebs=lambda o: o['gl_daily_rates.conversion_rate'])


class currencyCommonAttrs(object):
    COMMON = {
        currencyAttrs.rate
    }


# 1000
tomorrow_rates = {crs.CBR: [
    (c.RUB, [
        # c.AMD, c.AUD, c.AZN, c.BGN, c.BRL, c.BYR,
        # c.BYN, c.CAD, c.CHF, c.CNY, c.CZK,
        c.EUR,
        # c.DKK, c.HUF, c.INR, c.JPY, c.KGS, c.GBP,
        # c.HKD, c.KRW, c.kzt, c.MDL, c.NOK, c.PLN,
        # c.RON, c.SEK, c.SGD, c.TJS, c.TMT,
        c.try_,
        # c.uah,
        c.USD,
        # c.UZS, c.XDR, c.ZAR
    ])],
}

yesterday_rates = {

    # 1002
    crs.TCMB: [
        (c.try_, [
            c.EUR,
            # c.USD,
            # c.GBP,
            c.RUB
        ])
    ],

    # 1003
    crs.NBU: [
        (c.uah, [
            c.EUR, c.USD, c.RUB
        ])
    ],

    # 1004
    # Курсы по 1004 забираются с НЕнулевым временем
    # иногда курс не забираются в тестовой среде https://st.yandex-team.ru/BALANCEDUTY-15
    # crs.BOC: [
    #     (c.CNY, [
    #         # c.AED, c.AUD, c.CAD, c.CHF, c.DKK, c.EUR,
    #         # c.GBP, c.HKD, c.JPY, c.KRW, c.MYR, c.NOK,
    #         # c.NZD,
    #         # Банк Китая банит нас из-за большого количества запросов, оставляем 1 тест: проверяем гипотезу, разбанят ли
    #         c.RUB,
    #         # c.SAR, c.SEK, c.SGD, c.THB,
    #         # c.try_, c.USD, c.ZAR,
    #     ])
    # ],

    # 1005
    crs.CBA: [
        (c.AMD, [
            c.EUR, c.USD, c.RUB
        ])
    ],

    # 1006
    crs.NBRB: [
        (c.BYR, [
            # c.AUD, c.BGN, c.CAD, c.CHF, c.CNY, c.CZK,
            # c.DKK,
            c.EUR,
            # c.GBP, c.JPY, c.KGS, c.kzt,
            # c.MDL, c.NOK, c.NZD, c.PLN,
            c.RUB,
            # c.SEK,
            # c.SGD,
            c.try_, c.uah, c.USD,
            # c.XDR
        ]),
        (c.BYN, [
            # c.AUD, c.BGN, c.CAD, c.CHF, c.CNY, c.CZK,
            # c.DKK,
            c.EUR,
            # c.GBP, c.JPY, c.KGS, c.kzt,
            # c.MDL, c.NOK, c.NZD, c.PLN,
            c.RUB,
            # c.SEK,
            # c.SGD,
            c.try_, c.uah, c.USD,
            # c.XDR
        ])
    ],

    # 1007
    # KZT: не забираются по выходным
    crs.NBKZ: [
        (c.kzt, [
            c.EUR, c.USD, c.RUB, c.try_
        ])
    ],

    # 1008
    crs.NBGE: [
        (c.GEL, [
            # c.AMD, c.AUD, c.AZN, c.BGN, c.BRL, c.BYN,
            # c.CAD, c.CHF, c.CNY, c.CZK, c.DKK,
            c.EUR,
            c.ILS,
            # c.GBP, c.HKD, c.HUF, c.INR, c.JPY, c.KGS,
            # c.KRW, c.MDL, c.NOK, c.NZD, c.PLN, c.RON,
            # c.RSD,
            c.RUB,
            # c.SEK, c.SGD, c.TJS, c.TMT,
            c.USD,
            # c.UZS, c.ZAR,
            c.kzt, c.try_, c.uah
        ])
    ],

    # 1009
    crs.BNM: [
        (c.MDL, [
            # c.RON, c.RSD, c.try_, c.UZS, c.AMD, c.AUD,
            # c.KGS, c.KRW, c.TJS, c.TMT, c.XDR,
            c.RUB,
            # c.DKK, c.HUF, c.NOK, c.SEK, c.CAD, c.GBP,
            # c.HKD,
            c.kzt,
            # c.BGN, c.BYN, c.BYR, c.CNY,
            # c.CZK, c.INR, c.MYR,
            c.EUR,
            # c.HRK,
            c.USD,
            # c.uah, c.AZN, c.CHF, c.GEL, c.JPY, c.NZD,
            # c.PLN
        ])
    ],

    # 1010
    crs.NBKR: [
        (c.KGS, [
            c.EUR, c.RUB, c.USD, c.kzt
        ])
    ],

    # 1011
    crs.CBU: [
        (c.UZS, [
            # c.AED, c.AMD, c.AUD, c.AZN, c.BGN, c.BRL,
            # c.BYN, c.CAD, c.CHF, c.CNY, c.CZK, c.DKK,
            c.EUR,
            # c.GBP, c.GEL, c.HUF, c.IDR, c.INR,
            # c.JPY, c.KGS, c.KRW, c.kzt, c.MDL,
            # c.MNT, - https://st.yandex-team.ru/BALANCE-32083
            # c.MXN, c.MYR, c.NOK, c.NZD, c.PHP, c.PLN,
            # c.RON, c.RSD,
            c.RUB,
            # c.SAR, c.SEK, c.SGD,
            # c.THB, c.TJS, c.TMT,
            c.try_, c.uah, c.USD,
            # c.XDR, c.ZAR,
        ])
    ],

    # 1012
    crs.NBS: [
        (c.RSD, [
            # c.AUD, c.BGN, c.CAD, c.CHF, c.CNY, c.CZK,
            # c.DKK,
            c.EUR,
            # c.GBP, c.HRK, c.HUF, c.JPY,
            # c.NOK, c.PLN, c.RON,
            c.RUB,
            # c.SEK,
            c.USD,
            c.try_
        ])
    ],

    # 1013
    crs.NBT: [
        (c.TJS, [
            # c.AED, c.AMD, c.AUD, c.AZN, c.BYN, c.BYR,
            # c.CAD, c.CHF, c.CNY, c.DKK,
            c.EUR,
            # c.GBP,
            # c.GEL, c.INR, c.JPY, c.KGS, c.kzt, c.MDL,
            # c.MYR, c.NOK, c.PLN,
            c.RUB,
            # c.SAR, c.SEK,
            # c.SGD, c.THB, c.TMT,
            c.try_, c.uah, c.USD,
            # c.UZS
        ])

    ],

    # 1014
    crs.BOM: [
        (c.MNT, [
            # c.AED, c.AUD, c.BGN, c.CAD, c.CHF, c.CNY,
            # c.CZK, c.DKK,
            c.EUR,
            # c.GBP, c.HKD, c.HUF,
            # c.IDR, c.INR, c.JPY, c.KRW, c.kzt, c.MYR,
            # c.NOK, c.NZD, c.PLN,
            c.RUB,
            # c.SEK, c.SGD,
            # c.THB, c.try_, c.TWD,
            c.uah, c.USD,
            # c.ZAR
        ])
    ],

    # 1015
    crs.CBAR: [
        (c.AZN, [
            # c.AED, c.AUD, c.BRL, c.BYN, c.CAD, c.CHF,
            # c.CNY, c.CZK, c.DKK,
            c.EUR,
            # c.GBP, c.GEL,
            # c.HKD, c.IDR, c.INR, c.JPY, c.KGS, c.KRW,
            # c.kzt, c.MDL, c.MXN, c.MYR, c.NOK, c.NZD,
            # c.PLN,
            c.RUB,
            # c.SAR, c.SEK, c.SGD, c.TJS,
            # c.TMT,
            c.try_,
            # c.TWD,
            c.uah, c.USD,
            # c.UZS,
            # c.ZAR
        ])
    ],

    # 1016
    crs.CNB: [
        (c.CZK, [
            # c.AUD, c.BGN, c.BRL, c.CAD, c.CHF, c.CNY,
            # c.DKK,
            c.EUR,
            # c.GBP, c.HKD, c.HRK, c.HUF,
            # c.IDR, c.INR, c.JPY, c.KRW, c.MXN, c.MYR,
            # c.NOK, c.NZD, c.PHP, c.PLN, c.RON,
            c.RUB,
            # c.SEK, c.SGD, c.THB,
            c.try_, c.USD,
            # c.XDR,
            # c.ZAR
        ])
    ],

    # 1017
    crs.NBP: [
        (c.PLN, [
            # c.AED, c.AMD, c.AUD, c.AZN, c.BGN, c.BRL,
            # c.BYN, c.CAD, c.CHF, c.CNY, c.CZK, c.DKK,
            # c.ETB, - https://st.yandex-team.ru/BALANCE-32083
            c.EUR,
            # c.GBP, c.GEL, c.GHS, c.HKD,
            # c.HRK, c.HUF, c.IDR, c.INR, c.JPY, c.KES,
            # c.KGS, c.KRW, c.kzt, c.MDL,
            # c.MNT, - https://st.yandex-team.ru/BALANCE-32083
            # c.MXN,
            # c.MYR,
            # c.NGN, - https://st.yandex-team.ru/BALANCE-32083
            # c.NOK, c.NZD, c.PHP, c.RON,
            # c.RSD,
            c.RUB,
            # c.SAR, c.SEK, c.SGD, c.THB,
            # c.TJS, c.TMT, c.try_, c.TWD,
            c.uah, c.USD,
            # c.UZS,
            # c.XAF, - https://st.yandex-team.ru/BALANCE-32083
            # c.XDR, c.XOF, c.ZAR,
        ])
    ],

    # 1018 - ILS
    crs.BOI: [
        (c.ILS, [
            # c.AUD, c.CAD, c.CHF, c.DKK,
            c.EUR,
            # c.GBP,
            # c.JPY, c.NOK, c.SEK,
            c.USD,
            # c.ZAR
        ])
    ],

    # 1019
    crs.NBE: [
        (c.ETB, [
            # c.AED, c.AUD, c.CAD, c.CHF, c.CNY, c.DKK,
            c.EUR,
            # c.GBP, c.INR, c.JPY, c.KES, c.NOK,
            # c.SAR, c.SEK,
            c.USD,
            # c.XDR, c.ZAR
        ])
    ],

    # 1020
    crs.CBN: [
        (c.NGN, [
            # c.CHF, c.CNY, c.DKK,
            c.EUR,
            # c.GBP, c.JPY,
            # c.SAR,
            c.USD,
            # c.XAF, - https://st.yandex-team.ru/BALANCE-32083
            # c.XDR, c.XOF, c.ZAR
        ])
    ],

    # 1021 - XOF
    # TODO: доделать.
    crs.BCEAO: [],

    # 1022 - ZAR
    # TODO: доделать.
    crs.SARB: [],

    # 1023 - KES
    # TODO: доделать.
    crs.CBK: [],

    # 1024 - RON
    # TODO: доделать.
    crs.BNR: [],

    # 1025 - HUF
    # TODO: доделать.
    crs.MNB: [],

    # 1026 - BGN
    # TODO: доделать.
    crs.BNB: [],

    # 1100
    crs.OANDA: [
        (c.UZS, [
            c.USD
        ])
    ]

    # 1111 BALANCE - нет курсов
}


rev_yesterday_rates = {
    # 1001
    crs.ECB: [
        (c.EUR, [
            # c.AUD, c.BGN, c.BRL, c.CAD, c.CHF, c.CNY,
            # c.CZK, c.DKK, c.GBP, c.HKD, c.HRK, c.HUF,
            # c.IDR, c.ILS, c.INR, c.JPY, c.KRW, c.MXN,
            # c.MYR, c.NOK, c.NZD, c.PHP, c.PLN, c.RON,
            c.RUB,
            # c.SEK, c.SGD, c.THB,
            c.try_, c.USD,
            # c.ZAR
        ])
    ],

    # 1018
    crs.BOI: [
        (c.ILS, [
            # c.AZN, c.BGN, c.CZK,
            # c.GEL,
            # c.HUF, c.MDL,
            # c.PLN, c.UZS
        ])
    ],
}

day_before_yesterday_rates = {
    # 1027
    crs.BOE: [
        (c.GBP, [
            # c.AUD, c.CAD, c.CHF, c.CNY, c.CZK, c.DKK,
            c.EUR,
            # c.HKD, c.HUF, c.ILS, c.INR, c.JPY, c.KRW,
            # c.LTL,
            # c.MYR, c.NOK, c.NZD, c.PLN,
            c.RUB,
            # c.SAR, c.SEK, c.SGD,
            # c.THB,
            c.try_,
            # c.TWD,
            c.USD,
            # c.ZAR
        ])
    ],
}

prepare_data = lambda rates, date, rev: [(cc, base_cc, date, source, rev)
                                         for source, pairs in rates.items()
                                         for base_cc, cc_list in pairs
                                         for cc in cc_list]

tomorrow = prepare_data(tomorrow_rates, TOMORROW, False)
yesterday = prepare_data(yesterday_rates, YESTERDAY, False)
rev_yesterday = prepare_data(rev_yesterday_rates, YESTERDAY, True)
rev_day_before_yesterday = prepare_data(rev_yesterday_rates, DAY_BEFORE_YESTERDAY, True)


@pytest.mark.parametrize(
    'params', tomorrow + yesterday + rev_yesterday + rev_day_before_yesterday,
    ids=lambda params: '{} {} {} {}'.format(params[0].char_code, params[1].char_code, params[3].id, params[4])
)
def test_export_currency_rate(params):
    cc, base_cc, rate_dt, source, rev = params
    check_attrs(cc, base_cc, rate_dt, source, rev, currencyCommonAttrs.COMMON)


# ---------------------------------------------- Utils ----------------------------------------------

def get_balance_currency_data(cc, base_cc, rate_dt, source):
    balance_currency_data = {}

    # t_client
    query = "SELECT * FROM t_currency_rate_v2 WHERE cc = :cc and base_cc = :base_cc and trunc(rate_dt) = trunc(:rate_dt) and rate_src_id = :source"
    result = db.balance().execute(query, {'cc': cc.char_code, 'base_cc': base_cc.char_code, 'rate_dt': rate_dt,
                                          'source': source.id},
                                  single_row=True)

    # Если нет курса из списка ожидаемых - падаем
    if not result:
        msg = 'No currency rate in Balance: cc.char_code: ' \
              '{cc}, base_cc.char_code: {base_cc}, rate_dt: {rate_dt}, source: {source}'
        msg = msg.format(**{'cc': cc.char_code, 'base_cc': base_cc.char_code, 'rate_dt': rate_dt, 'source': source.id})
        raise Exception(msg)

    # Всегда явно перевыгружаем курсы
    object_id = result['id']
    steps.CommonSteps.export('OEBS', 'CurrencyRate', object_id)

    balance_currency_data.update(utils.add_key_prefix(result, 't_currency_rate_v2.'))

    return balance_currency_data


def get_oebs_currency_data(cc, base_cc, rate_dt, source):
    oebs_currency_data = {}

    # Превращаем RUR в RUB
    convert = {'RUR': 'RUB'}
    cc = convert.get(cc, cc)
    base_cc = convert.get(base_cc, base_cc)

    # gl_daily_rates
    query = 'select from_currency, conversion_rate, conversion_date, to_currency, conversion_type ' \
            'from apps.gl_daily_rates ' \
            'where from_currency = :cc and to_currency = :base_cc ' \
            'and trunc(conversion_date) = trunc(:rate_dt)  and conversion_type = :source'
    result = db.oebs().execute_oebs(Firms.YANDEX_1.id, query,
                                    {'cc': cc.iso_code, 'base_cc': base_cc.iso_code, 'rate_dt': rate_dt,
                                     'source': str(source.oebs_rate_src)},
                                    single_row=True)
    oebs_currency_data.update(utils.add_key_prefix(result, 'gl_daily_rates.'))

    return oebs_currency_data


def check_attrs(cc, base_cc, rate_dt, source, rev, attrs):

    # Исключение: если курсы "перевёрнуты" (rev) - меняем местам cc <> base_cc
    if rev:
        cc, base_cc = base_cc, cc

    # Исключение: не забираем курсы валют по выходным из NBKZ, BOE
    if rate_dt.strftime("%a") in ('Sat', 'Sun') and source in (crs.NBKZ, crs.BOE):
        pytest.skip('We dont get rates from NBKZ (1007), BOE (1027) on weekends')

    with reporter.step(u'Считываем данные из баланса'):
        balance_client_data = get_balance_currency_data(cc, base_cc, rate_dt, source)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_client_data = get_oebs_currency_data(cc, base_cc, rate_dt, source)

    balance_values, oebs_values = read_attr_values(attrs, balance_client_data, oebs_client_data)

    utils.check_that(D(oebs_values['currencyattrs.rate']),
                     hamcrest.close_to(D(balance_values['currencyattrs.rate']), PRECISION))

    # utils.check_that(oebs_values, equal_to_casted_dict(balance_values),
    #                  step=u'Проверяем корректность данных клиента в ОЕБС')
