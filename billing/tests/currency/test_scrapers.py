from datetime import datetime, timedelta
from decimal import Decimal
from functools import partial
from traceback import format_exc
from typing import Union

import pytest
from django.conf import settings
from freezegun import freeze_time

from refs.currency.models import Rate
from refs.currency.scrapers import *
from refs.currency.scrapers._base import Scraper
from refs.currency.tasks import sync_currency
from refs.currency.utils import MigrationUtils


@pytest.mark.django_db
def test_scrapers_mocked_smoke(monkeypatch, mock_sleepy_sync, response_mock, read_fixture):
    """Тест с имитаторами."""

    date = datetime(2019, 12, 25, 13)

    responses = {
        Azerbaijan: {'https://cbar.az/currencies/25.12.2019.xml': 'smoke_aze.xml'},
        WestAfrica: {'https://www.bceao.int/en/cours/get_all_devise_by_date?dateJour=2019-12-25': 'smoke_bceao.html'},
        Bulgaria: {
            'http://www.bnb.bg/Statistics/StExternalSector/StExchangeRates/StERForeignCurrencies/index.htm?'
            'downloadOper=true&group1=first&firstDays=25&firstMonths=12&firstYear=2019&search=true&showChart=false&'
            'showChartButton=false&type=XML': 'smoke_bgr.xml'},
        Belarus: {'https://www.nbrb.by/API/ExRates/Rates?onDate=2019-12-25&Periodicity=0': 'smoke_blr.json'},
        Czech: {
            'https://www.cnb.cz/en/financial-markets/foreign-exchange-market/central-bank-exchange-rate-fixing/'
            'central-bank-exchange-rate-fixing/daily.txt?date=25.12.2019': 'smoke_cze.csv'
        },
        Ethiopia: {'https://market.nbebank.com/admin/searchsystem/dollarbirr/dollar_against_birrlist.php?x_time=2019-12-25&export=html': 'smoke_eth.html'},
        Europa: {'https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml': 'smoke_eur.xml'},
        Fixer: {
            'https://data.fixer.io/api/2019-12-25?access_key=access_key&base=EUR': 'smoke_fixer.json',
            'https://data.fixer.io/api/2019-12-25?access_key=access_key&base=USD': 'smoke_fixer.json',
            'https://data.fixer.io/api/2019-12-25?access_key=access_key&base=ILS&symbols=RUB ': 'smoke_fixer.json',
            'https://data.fixer.io/api/2019-12-25?access_key=access_key&base=GBP': 'smoke_fixer.json',
        },
        Georgia: {'https://nbg.gov.ge/gw/api/ct/monetarypolicy/currencies/en/json?date=2019-12-26': 'smoke_geo.json'},
        Ghana: {
            'https://www.bog.gov.gh/treasury-and-the-markets/historical-interbank-fx-rates/':
                '<input id="wdtNonceFrontendEdit" value="fake">',
            'P https://www.bog.gov.gh/wp-admin/admin-ajax.php?action=get_wdtable&table_id=40': 'smoke_gha.json',
        },
        Hungary: {'http://www.mnb.hu/arfolyam-tablazat?query=daily,25/12/2019': 'smoke_hun.html'},
        Israel: {
            'https://www.boi.org.il/currency.xml?rdate=20191225': "document.cookie='xx=yy; path=/';",
            ' https://www.boi.org.il/currency.xml?rdate=20191225': 'smoke_isr.xml',
        },
        Kazakhstan: {'http://www.nationalbank.kz/rss/get_rates.cfm?fdate=25.12.2019': 'smoke_kaz.xml'},
        Kyrgyzstan: {'http://www.nbkr.kg/XML/daily.xml': 'smoke_kgz.xml'},
        Moldova: {
            'http://www.bnm.md/ru/official_exchange_rates?get_xml=1&date=26.12.2019':
                read_fixture('smoke_rus.xml').decode('cp1251')},
        Mongolia: {
            'https://www.mongolbank.mn/eng/dblistofficialdailyrate.aspx?vYear=2019&vMonth=12&vDay=25': 'smoke_mng.html'
        },
        Oanda: {
            'https://www.oanda.com/currency/converter/update?end_date=2019-12-25&view=details&id=1&action=C&'
            f'base_currency_0={src}&quote_currency={dst}': 'smoke_oanda.json'
            for src, dst in [
                ('AED', 'RUB'),
                ('AOA', 'EUR'),
                ('AOA', 'RUB'),
                ('AOA', 'USD'),
                ('BOB', 'USD'),
                ('CDF', 'EUR'),
                ('CLP', 'EUR'),
                ('CLP', 'RUB'),
                ('CLP', 'USD'),
                ('DZD', 'EUR'),
                ('EUR', 'AED'),
                ('EUR', 'AMD'),
                ('EUR', 'AOA'),
                ('EUR', 'AZN'),
                ('EUR', 'BYN'),
                ('EUR', 'CDF'),
                ('EUR', 'CLP'),
                ('EUR', 'DZD'),
                ('EUR', 'GHS'),
                ('EUR', 'ILS'),
                ('EUR', 'KGS'),
                ('EUR', 'KZT'),
                ('EUR', 'MAD'),
                ('EUR', 'MDL'),
                ('EUR', 'MXN'),
                ('EUR', 'PEN'),
                ('EUR', 'RSD'),
                ('EUR', 'SDG'),
                ('EUR', 'TND'),
                ('EUR', 'TRY'),
                ('EUR', 'UZS'),
                ('EUR', 'XAF'),
                ('EUR', 'XOF'),
                ('EUR', 'ZAR'),
                ('EUR', 'ZMW'),
                ('GEL', 'RUB'),
                ('GHS', 'EUR'),
                ('GHS', 'RUB'),
                ('GHS', 'USD'),
                ('ILS', 'RUB'),
                ('MAD', 'EUR'),
                ('MXN', 'RUB'),
                ('MXN', 'USD'),
                ('PEN', 'EUR'),
                ('PEN', 'RUB'),
                ('PEN', 'USD'),
                ('RUB', 'AED'),
                ('RUB', 'AOA'),
                ('RUB', 'CLP'),
                ('RUB', 'GHS'),
                ('RUB', 'GEL'),
                ('RUB', 'ILS'),
                ('RUB', 'MXN'),
                ('RUB', 'PEN'),
                ('RUB', 'RSD'),
                ('RUB', 'XAF'),
                ('RUB', 'XOF'),
                ('RUB', 'ZMW'),
                ('RWF', 'USD'),
                ('SDG', 'EUR'),
                ('TND', 'EUR'),
                ('USD', 'AED'),
                ('USD', 'AMD'),
                ('USD', 'AOA'),
                ('USD', 'AZN'),
                ('USD', 'BOB'),
                ('USD', 'BYN'),
                ('USD', 'CLP'),
                ('USD', 'GBP'),
                ('USD', 'GHS'),
                ('USD', 'ILS'),
                ('USD', 'KGS'),
                ('USD', 'KZT'),
                ('USD', 'MDL'),
                ('USD', 'MXN'),
                ('USD', 'PEN'),
                ('USD', 'RSD'),
                ('USD', 'RWF'),
                ('USD', 'TRY'),
                ('USD', 'UZS'),
                ('USD', 'XAF'),
                ('USD', 'XOF'),
                ('USD', 'ZAR'),
                ('USD', 'ZMW'),
                ('UZS', 'USD'),
                ('XAF', 'EUR'),
                ('XAF', 'RUB'),
                ('XAF', 'USD'),
                ('XOF', 'RUB'),
                ('ZAR', 'RUB'),
                ('ZMW', 'EUR'),
                ('ZMW', 'RUB'),
                ('ZMW', 'USD'),
            ]
        },
        Poland: {
            'http://www.nbp.pl/kursy/xml/dir.txt': 'c001z191225\r\nh001z191225\r\n',
            'http://www.nbp.pl/kursy/xml/c001z191225.xml': 'smoke_pol.xml',
        },
        Romania: {'http://www.bnr.ro/files/xml/years/nbrfxrates2019.xml': 'smoke_rou.xml'},
        Russia: {
            'http://www.cbr.ru/scripts/XML_daily.asp?date_req=26.12.2019':
                read_fixture('smoke_rus.xml').decode('cp1251')
        },
        Serbia: {
            'https://www.nbs.rs/kursnaListaModul/naZeljeniDan.faces':
                '<form action="/dummy"><input name="javax.faces.ViewState" value="xxx"></form>',
            'P https://www.nbs.rs/dummy': 'smoke_srb.xml'
        },
        Tajikistan: {'http://www.nbt.tj/en/kurs/export_xml.php?date=2019-12-26&export=xmlout': 'smoke_tjk.xml'},
        Turkey: {'http://tcmb.gov.tr/kurlar/201912/25122019.xml': 'smoke_tur.xml'},
        Ukraine: {'http://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?date=20191225': 'smoke_ukr.xml'},
        Uzbekistan: {'http://cbu.uz/ru/arkhiv-kursov-valyut/xml/all/2019-12-25/': 'smoke_uzb.xml'},
        SouthAfrica: {
            'https://custom.resbank.co.za/SarbWebApi/WebIndicators/Shared/GetTimeseriesObservations//'
            'EXCX135D/2019-12-25/2019-12-25': 'smoke_zaf.json',
            'https://custom.resbank.co.za/SarbWebApi/WebIndicators/Shared/GetTimeseriesObservations//'
            'EXCZ001D/2019-12-25/2019-12-25': 'smoke_zaf.json',
            'https://custom.resbank.co.za/SarbWebApi/WebIndicators/Shared/GetTimeseriesObservations//'
            'EXCZ002D/2019-12-25/2019-12-25': 'smoke_zaf.json',
            'https://custom.resbank.co.za/SarbWebApi/WebIndicators/Shared/GetTimeseriesObservations//'
            'EXCZ120D/2019-12-25/2019-12-25': 'smoke_zaf.json',
        },
        Armenia: {'P http://api.cba.am/exchangerates.asmx': 'smoke_arm.xml'},
        China: {
            'P http://srh.bankofchina.com/search/whpj/searchen.jsp':
                '<select name="pjname" id="pjname"><option value="x" >x</option>'
                '<option value="EUR" >EUR</option></select>',
            ' P http://srh.bankofchina.com/search/whpj/searchen.jsp': 'smoke_chn.html',
        },
        Kenya: {
            'P https://www.centralbank.go.ke/wp-admin/admin-ajax.php?action=get_wdtable&table_id=32': 'smoke_ken.json'
        },
        Nigeria: {'P https://www.cbn.gov.ng/rates/ExchangeArchives.asp': 'smoke_nga.html'},
        England: {
            'https://www.bankofengland.co.uk/boeapps/database/Rates.asp?TD=24&TM=Dec&TY=2019&into=GBP&rateview=D':
                'smoke_gbp.html',
        },
        Korea: {
            'https://ecos.bok.or.kr/api/StatisticSearch/access_key/xml/en/1/100/731Y001/D/20191225/20191225':
                'smoke_kor.xml',
        },
        Norway: {
            'https://data.norges-bank.no/api/data/EXR/B..NOK.SP?startPeriod=2019-12-25&EndPeriod=2019-12-25':
                'smoke_nor.xml',
        },
        Emirates: {'https://www.centralbank.ae/en/fx-rates-ajax?v&date=25-12-2019': 'smoke_uae.json'},

    }
    """Соответствие адресов выдач ответам сервера.
    P - перед адресом, указывает на необходимость имитации POST-запроса.

    """
    exceptions = []

    def capture():
        trace = format_exc()
        if 'Responses Mock' in trace:
            # Вероятно добавился источник, который не учтён в тесте.
            exceptions.append(trace)
        raise ValueError(trace)

    monkeypatch.setattr('refs.core.utils.ExceptionCapturer.capture', capture)

    with freeze_time(date):

        rules = []

        for scraper, responses_ in responses.items():

            if not scraper.active:
                continue

            for resp_url, resp_body in responses_.items():

                resp_url = resp_url.strip()

                method = 'GET'

                if resp_url.startswith('P '):
                    resp_url = resp_url.lstrip('P ')
                    method = 'POST'

                if resp_body.startswith('xml'):
                    if resp_body == 'xml':
                        resp_body = f'{resp_body}<root></root>'  # Простейший имитатор.
                    resp_body = resp_body.replace('xml', '<?xml version="1.0"?>', 1)

                elif resp_body.startswith('smoke_'):
                    resp_body = read_fixture(resp_body).decode()

                rules.append(f'{method} {resp_url} -> 200 :{resp_body}')

        with response_mock(rules):
            sync_currency(None)

    # Если были исключения в связи с неучтёнными в тесте сборщиками,
    # выведем все исключения.
    assert not exceptions


def scrape(title, *, scrapers, target_date=None, rates_on_date=None):
    success = []
    errors = []

    for scraper in scrapers:
        scraper = scraper(date=target_date)

        try:
            items = scraper.scrape()

        except Exception as e:
            errors.append(f'{scraper}: exception {e.__class__.__name__} {e}')
            continue

        failed = False

        if len(items):

            if target_date:
                expected = rates_on_date.get(scraper.__class__)

                if not expected:
                    failed = True
                    errors.append(f'{scraper}: not in fixture')

                elif len(set(expected).intersection(items)) != len(expected):
                    failed = True
                    errors.append(f'{scraper}: fixture mismatch')

        else:
            failed = True
            errors.append(f'{scraper}: no items')

        if not failed:
            success.extend(items)

    print(f'\n{title}: Scrapers count: {len(scrapers)}. Rates gathered: {len(success)}')

    assert not errors


@pytest.mark.regression
@pytest.mark.skipif(condition=settings.ARCADIA_RUN, reason='Не пытаемся ходить вовне из Sandbox')
def test_scrapers_on_date_true(rates_on_date):

    target_date = datetime(2018, 8, 28)  # дата специально подобрана

    scrape(
        'With on date support',
        scrapers=[scraper for scraper in SCRAPERS.values() if scraper.supports_on_date],
        target_date=target_date, rates_on_date=rates_on_date)


@pytest.mark.regression
@pytest.mark.skipif(condition=settings.ARCADIA_RUN, reason='Не пытаемся ходить вовне из Sandbox')
def test_scrapers_on_date_false():

    scrape(
        'Without on date support',
        scrapers=[scraper for scraper in SCRAPERS.values() if not scraper.supports_on_date])


@pytest.mark.regression
@pytest.mark.skipif(condition=settings.ARCADIA_RUN, reason='Не пытаемся ходить вовне из Sandbox')
class TestCornerCases:

    def test_west_africa(self, read_fixture, response_mock):

        with response_mock(
            f"GET https://www.bceao.int/en/cours/get_all_devise_by_date?dateJour=2019-01-09 -> "
            f"200 :{read_fixture('west_africa_money.html')}"
        ):
            scraper = WestAfrica(date=datetime(2019, 1, 9))
            result = scraper.scrape()
        assert result[0].buy == Decimal('655.957')

    def test_ghana(self, response_mock):
        # Нет курсов.
        rules = [
            'GET https://www.bog.gov.gh/treasury-and-the-markets/historical-interbank-fx-rates/ -> 200 :'
            '<input type="hidden" id="wdtNonceFrontendEdit" name="wdtNonceFrontendEdit" value="216ce9ae92" />',

            'POST https://www.bog.gov.gh/wp-admin/admin-ajax.php?action=get_wdtable&table_id=40 -> 200:'
        ]

        with response_mock(rules) as http_mock:
            scraper = Ghana(date=datetime(1990, 8, 28))
            result = scraper.scrape()

        assert not result

    def test_ethiopia(self, read_fixture, response_mock):

        with response_mock(
            f"GET https://market.nbebank.com/admin/searchsystem/dollarbirr/dollar_against_birrlist.php?x_time=2019-12-25&export=html -> "
            f"200 :{read_fixture('smoke_eth.html')}"
        ):
            scraper = Ethiopia(date=datetime(2019, 12, 25))
            result = scraper.scrape()
        assert result[-1].buy == Decimal('141.8139')

    def test_poland(self):
        date = datetime(2019, 12, 25, 10)  # проверяем время до 12:20
        with freeze_time(date):
            scraper = Poland(date=datetime(2019, 12, 25))
            result = scraper.scrape()
        assert len(result) == 0


@pytest.mark.parametrize(
    'scraper, url, fixture_name',
    [
        (
            Ethiopia,
            'https://market.nbebank.com/admin/searchsystem/dollarbirr/dollar_against_birrlist.php?'
            'x_time=2019-01-09&export=html', 'smoke_eth_empty.html'
        ),
        (
            England,
            'https://www.bankofengland.co.uk/boeapps/database/Rates.asp?TD=09&TM=Jan&TY=2019&into=GBP&rateview=D',
            'smoke_gbp_empty.html'
        )
    ])
def test_empty(scraper, url, fixture_name, read_fixture, response_mock):

    with response_mock(
        f"GET {url}-> 200 :{read_fixture(fixture_name)}"
    ):
        scraper = scraper(date=datetime(2019, 1, 9))
        result = scraper.scrape()
    assert len(result) == 0


@pytest.mark.django_db
def test_migration_utils():

    class DummyScraper(Scraper):

        alias = 'dum'
        title = 'Dummy'
        currency = 'DU'
        url_template = 'http://localhost/?date={date}'
        timezone = 'Asia/Novosibirsk'
        timeout = 1

        def fetch(self, *args, **kwargs) -> bytes:
            return b''

        def prepare(self, raw_data: Union[bytes, dict]) -> None:

            contribute = partial(
                self.contribute_info,
                date_actual=self.date_parse(self.target_date.strftime('%Y-%m-%d')))

            contribute(code='DUM', mid=Decimal('22.1'))

    dumped = MigrationUtils.dump_rates(DummyScraper, since=datetime.now() - timedelta(days=2))

    class DummyApps:

        def get_model(self, *args, **kwargs):
            return Rate

    apps = DummyApps()

    MigrationUtils.load_rates(dumped)(apps, None)

    assert Rate.objects.count() == 3
