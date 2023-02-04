import datetime
from decimal import Decimal

import pytest

from refs.currency.scrapers import *
from refs.currency.scrapers import RateInfo


@pytest.fixture
def rates_on_date():

    expected = {
        Armenia: [
            RateInfo(source='ARM', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('482.7300'), buy=None, sell=None, to_code='AMD', rate_dir=Decimal('482.730000'),
                     rate_inv=Decimal('0.002072')),
            RateInfo(source='ARM', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='IRR',
                     mid=Decimal('1.1200'), buy=None, sell=None, to_code='AMD', rate_dir=Decimal('0.011200'),
                     rate_inv=Decimal('89.285714')),
        ],

        Azerbaijan: [
            RateInfo(source='AZE', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('1.7000'), buy=None, sell=None, to_code='AZN', rate_dir=Decimal('1.700000'),
                     rate_inv=Decimal('0.588235')),
            RateInfo(source='AZE', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='CLP',
                     mid=Decimal('0.2584'), buy=None, sell=None, to_code='AZN', rate_dir=Decimal('0.002584'),
                     rate_inv=Decimal('386.996904')),
        ],

        WestAfrica: [
            RateInfo(source='BCEAO', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='JPY', mid=None,
                     buy=Decimal('5.080'), sell=Decimal('5.020'), to_code='XOF', rate_dir=Decimal('5.080000'),
                     rate_inv=Decimal('0.199203')),
        ],

        Bulgaria: [
            RateInfo(source='BGR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='AUD',
                     mid=Decimal('1.22823'), buy=None, sell=None, to_code='BGN', rate_dir=Decimal('1.228230'),
                     rate_inv=Decimal('0.814180')),
            RateInfo(source='BGR', date=datetime.date(2018, 8, 28), from_mid=Decimal('10'), from_code='CNY',
                     mid=Decimal('2.45581'), buy=None, sell=None, to_code='BGN', rate_dir=Decimal('0.245581'),
                     rate_inv=Decimal('4.071976')),
            RateInfo(source='BGR', date=datetime.date(2018, 8, 28), from_mid=Decimal('10000'), from_code='IDR',
                     mid=Decimal('1.14227'), buy=None, sell=None, to_code='BGN', rate_dir=Decimal('0.000114'),
                     rate_inv=Decimal('8754.497623')),
        ],

        Belarus: [
            RateInfo(source='BLR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('2.0551'), buy=None, sell=None, to_code='BYN', rate_dir=Decimal('2.055100'),
                     rate_inv=Decimal('0.486594')),
            RateInfo(source='BLR', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY',
                     mid=Decimal('1.8493'), buy=None, sell=None, to_code='BYN', rate_dir=Decimal('0.018493'),
                     rate_inv=Decimal('54.074515')),
        ],

        China: [
            RateInfo(source='CHN', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='RUB',
                     mid=Decimal('10.1'), buy=Decimal('10.01'), sell=Decimal('10.09'), to_code='CNY',
                     rate_dir=Decimal('0.101000'), rate_inv=Decimal('9.900990')),
            RateInfo(source='CHN', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='USD',
                     mid=Decimal('680.52'), buy=Decimal('679.02'), sell=Decimal('681.9'), to_code='CNY',
                     rate_dir=Decimal('6.805200'), rate_inv=Decimal('0.146946')),

        ],

        Czech: [
            RateInfo(source='CZE', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='EUR',
                     mid=Decimal('25.720'), buy=None, sell=None, to_code='CZK', rate_dir=Decimal('25.720000'),
                     rate_inv=Decimal('0.038880')),
            RateInfo(source='CZE', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='ISK',
                     mid=Decimal('20.626'), buy=None, sell=None, to_code='CZK', rate_dir=Decimal('0.206260'),
                     rate_inv=Decimal('4.848250')),

        ],

        Emirates: [
            RateInfo(source='UAE', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('3.6725'), buy=None, sell=None, to_code='AED', rate_dir=Decimal('3.6725'),
                     rate_inv=Decimal('0.272294')),
        ],

        Georgia: [
            RateInfo(source='GEO', date=datetime.date(2018, 8, 28), from_mid=Decimal('1000'), from_code='AMD',
                     mid=Decimal('5.3300'), buy=None, sell=None, to_code='GEL', rate_dir=Decimal('0.005330'),
                     rate_inv=Decimal('187.617261')),
            RateInfo(source='GEO', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='EUR',
                     mid=Decimal('2.9849'), buy=None, sell=None, to_code='GEL', rate_dir=Decimal('2.984900'),
                     rate_inv=Decimal('0.335020')),
        ],

        Ghana: [
            RateInfo(source='GHA', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('4.7196'), buy=Decimal('4.7173'), sell=Decimal('4.7219'), to_code='GHS',
                     rate_dir=Decimal('4.719600'), rate_inv=Decimal('0.211882')),
            RateInfo(source='GHA', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='CNY',
                     mid=Decimal('0.6955'), buy=Decimal('0.6951'), sell=Decimal('0.6958'), to_code='GHS',
                     rate_dir=Decimal('0.695500'), rate_inv=Decimal('1.437815')),
        ],

        Hungary: [
            RateInfo(source='HUN', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='GBP',
                     mid=Decimal('357.30'), buy=None, sell=None, to_code='HUF', rate_dir=Decimal('357.300000'),
                     rate_inv=Decimal('0.002799')),
            RateInfo(source='HUN', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY',
                     mid=Decimal('249.27'), buy=None, sell=None, to_code='HUF', rate_dir=Decimal('2.492700'),
                     rate_inv=Decimal('0.401171')),
        ],

        Israel: [
            RateInfo(source='ISR', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY',
                     mid=Decimal('3.2631'), buy=None, sell=None, to_code='ILS', rate_dir=Decimal('0.032631'),
                     rate_inv=Decimal('30.645705')),
            RateInfo(source='ISR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('3.624'), buy=None, sell=None, to_code='ILS', rate_dir=Decimal('3.624000'),
                     rate_inv=Decimal('0.275938')),
        ],

        Kazakhstan: [
            RateInfo(source='KAZ', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('361.19'), buy=None, sell=None, to_code='KZT', rate_dir=Decimal('361.190000'),
                     rate_inv=Decimal('0.002769')),
            RateInfo(source='KAZ', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='UZS',
                     mid=Decimal('4.63'), buy=None, sell=None, to_code='KZT', rate_dir=Decimal('0.046300'),
                     rate_inv=Decimal('21.598272')),
        ],

        Kenya: [
            RateInfo(source='KEN', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY',
                     mid=Decimal('90.7650'), buy=Decimal('90.6652'), sell=Decimal('90.8648'), to_code='KES',
                     rate_dir=Decimal('0.907650'), rate_inv=Decimal('1.101746')),
            RateInfo(source='KEN', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('100.7900'), buy=Decimal('100.6928'), sell=Decimal('100.8872'), to_code='KES',
                     rate_dir=Decimal('100.790000'), rate_inv=Decimal('0.009922')),
        ],

        Korea: [
            RateInfo(source='KOR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('1114.3'), buy=None, sell=None, to_code='KRW', rate_dir=Decimal('1114.3'),
                     rate_inv=Decimal('0.000897')),
            RateInfo(source='KOR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='CNY',
                     mid=Decimal('163.76'), buy=None, sell=None, to_code='KRW', rate_dir=Decimal('163.76'),
                     rate_inv=Decimal('0.006106')),
        ],

        Moldova: [
            RateInfo(source='MDA', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='RUB',
                     mid=Decimal('0.2470'), buy=None, sell=None, to_code='MDL', rate_dir=Decimal('0.247000'),
                     rate_inv=Decimal('4.048583')),
            RateInfo(source='MDA', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY',
                     mid=Decimal('15.0818'), buy=None, sell=None, to_code='MDL', rate_dir=Decimal('0.150818'),
                     rate_inv=Decimal('6.630508')),
        ],

        Mongolia: [
            RateInfo(source='MNG', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('2472.73'), buy=None, sell=None, to_code='MNT', rate_dir=Decimal('2472.730000'),
                     rate_inv=Decimal('0.000404')),
            RateInfo(source='MNG', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='RUB',
                     mid=Decimal('36.63'), buy=None, sell=None, to_code='MNT', rate_dir=Decimal('36.630000'),
                     rate_inv=Decimal('0.027300')),
        ],

        Nigeria: [
            RateInfo(source='NGA', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('305.65'), buy=Decimal('305.15'), sell=Decimal('306.15'), to_code='NGN',
                     rate_dir=Decimal('305.650000'), rate_inv=Decimal('0.003272')),
            RateInfo(source='NGA', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='EUR',
                     mid=Decimal('358.2218'), buy=Decimal('357.6358'), sell=Decimal('358.8078'), to_code='NGN',
                     rate_dir=Decimal('358.221800'), rate_inv=Decimal('0.002792')),
        ],

        Norway: [
            RateInfo(source='NOR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='AUD',
                     mid=Decimal('6.1067'), buy=None, sell=None, to_code='NOK',
                     rate_dir=Decimal('6.1067'), rate_inv=Decimal('0.163755')),
            RateInfo(source='NOR', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='BGN',
                     mid=Decimal('497.2'), buy=None, sell=None, to_code='NOK',
                     rate_dir=Decimal('4.972'), rate_inv=Decimal('0.201126')),
        ],

        Oanda: [
            RateInfo(source='OANDA', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD', mid=None,
                     buy=Decimal('7761.112541633680556'), sell=Decimal('7821.189402159722222'), to_code='UZS',
                     rate_dir=Decimal('7761.112542'), rate_inv=Decimal('0.000128')),
            RateInfo(source='OANDA', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='GHS', mid=None,
                     buy=Decimal('0.209545433779891'), sell=Decimal('0.210681610819572'), to_code='USD',
                     rate_dir=Decimal('0.209545'), rate_inv=Decimal('4.746499')),
        ],

        Poland: [
            RateInfo(source='POL', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('3.6548'), buy=Decimal('3.6306'), sell=Decimal('3.7040'), to_code='PLN',
                     rate_dir=Decimal('3.654800'), rate_inv=Decimal('0.273613')),
            RateInfo(source='POL', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY',
                     mid=Decimal('3.2901'), buy=Decimal('3.2694'), sell=Decimal('3.3354'), to_code='PLN',
                     rate_dir=Decimal('0.032901'), rate_inv=Decimal('30.394213')),
        ],

        Romania: [
            RateInfo(source='ROU', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='EUR',
                     mid=Decimal('4.6474'), buy=None, sell=None, to_code='RON', rate_dir=Decimal('4.647400'),
                     rate_inv=Decimal('0.215174')),
            RateInfo(source='ROU', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY',
                     mid=Decimal('3.5762'), buy=None, sell=None, to_code='RON', rate_dir=Decimal('0.035762'),
                     rate_inv=Decimal('27.962642')),
        ],

        Russia: [
            RateInfo(source='RUS', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('67.3963'), buy=None, sell=None, to_code='RUB', rate_dir=Decimal('67.396300'),
                     rate_inv=Decimal('0.014838')),
            RateInfo(source='RUS', date=datetime.date(2018, 8, 28), from_mid=Decimal('10000'), from_code='UZS',
                     mid=Decimal('86.3638'), buy=None, sell=None, to_code='RUB', rate_dir=Decimal('0.008636'),
                     rate_inv=Decimal('115.789254')),
        ],

        Serbia: [
            RateInfo(source='SRB', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('101.3041'), buy=Decimal('101.0002'), sell=Decimal('101.6080'), to_code='RSD',
                     rate_dir=Decimal('101.304100'), rate_inv=Decimal('0.009871')),
            RateInfo(source='SRB', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY',
                     mid=Decimal('91.0902'), buy=Decimal('90.8169'), sell=Decimal('91.3635'), to_code='RSD',
                     rate_dir=Decimal('0.910902'), rate_inv=Decimal('1.097813')),
        ],

        Tajikistan: [
            RateInfo(source='TJK', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('9.4294'), buy=None, sell=None, to_code='TJS', rate_dir=Decimal('9.429400'),
                     rate_inv=Decimal('0.106051')),
            RateInfo(source='TJK', date=datetime.date(2018, 8, 28), from_mid=Decimal('10'), from_code='JPY',
                     mid=Decimal('0.8493'), buy=None, sell=None, to_code='TJS', rate_dir=Decimal('0.084930'),
                     rate_inv=Decimal('11.774402')),
        ],

        Turkey: [
            RateInfo(source='TUR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD', mid=None,
                     buy=Decimal('6.2186'), sell=Decimal('6.2298'), to_code='TRY', rate_dir=Decimal('6.218600'),
                     rate_inv=Decimal('0.160519')),
            RateInfo(source='TUR', date=datetime.date(2018, 8, 28), from_mid=Decimal('100'), from_code='JPY', mid=None,
                     buy=Decimal('5.5842'), sell=Decimal('5.6212'), to_code='TRY', rate_dir=Decimal('0.055842'),
                     rate_inv=Decimal('17.789796')),
        ],

        Ukraine: [
            RateInfo(source='UKR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='RUB',
                     mid=Decimal('0.41375'), buy=None, sell=None, to_code='UAH', rate_dir=Decimal('0.413750'),
                     rate_inv=Decimal('2.416918')),
            RateInfo(source='UKR', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='USD',
                     mid=Decimal('27.88524'), buy=None, sell=None, to_code='UAH', rate_dir=Decimal('27.885240'),
                     rate_inv=Decimal('0.035861')),
        ],

        Uzbekistan: [
            RateInfo(source='UZB', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='JPY',
                     mid=Decimal('70.38'), buy=None, sell=None, to_code='UZS', rate_dir=Decimal('70.380000'),
                     rate_inv=Decimal('0.014209')),
            RateInfo(source='UZB', date=datetime.date(2018, 8, 28), from_mid=Decimal('1'), from_code='RUB',
                     mid=Decimal('116.46'), buy=None, sell=None, to_code='UZS', rate_dir=Decimal('116.460000'),
                     rate_inv=Decimal('0.008587')),

        ],
    }
    return expected
