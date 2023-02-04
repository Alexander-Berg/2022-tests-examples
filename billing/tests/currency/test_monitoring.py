from datetime import date, datetime
from decimal import Decimal

import pytest
from freezegun import freeze_time
from sitemessage.models import Message

from refs.core.notifiers import SolomonNotifier
from refs.currency.models import Rate
from refs.currency.monitors import monitor_rates, get_empty_scrapers, send_currency_rates_metrics
from refs.currency.scrapers import SCRAPERS, RateInfo, Ethiopia
from refs.swift.models import Holiday


@pytest.mark.django_db
def test_rates_monitoring(django_assert_num_queries):

    week_date = date(2019, 12, 18)
    target_date = date(2019, 12, 25)
    old_date = date(2019, 12, 24)

    rates = {}
    holidays = []

    for target_date in (week_date, target_date, old_date):
        for alias, scraper in SCRAPERS.items():
            rates[alias] = (
                Rate(
                    source=alias, date=target_date, from_amount=Decimal(1),
                    rate_dir=Decimal(1), rate_inv=Decimal(1)
                )
            )
        holidays.append(
            Holiday(
                date=target_date, checksum=str(target_date), type=1, country_code='DE',
                country_name='Germany', hint='',
            )
        )

    Holiday.objects.bulk_create(holidays)
    Rate.objects.bulk_create(rates.values())
    Rate.objects.filter(source='UZB').delete()

    assert 'POL' in rates  # проверка отсутствия неактивных поставщиков
    Rate.objects.filter(source='POL').delete()

    Rate.objects.filter(source__in=['RUS', 'EUR'], date__in=(target_date, old_date)).delete()

    with freeze_time(target_date):
        with django_assert_num_queries(4) as _:
            failed = monitor_rates(target_date)

    assert 'POL' not in failed
    assert 'UZB' in failed
    assert 'RUS' in failed

    assert Message.objects.count() == 1
    message = Message.objects.last()
    assert 'UZB'in message.context['stext_']
    assert 'RUS'in message.context['stext_']

    with django_assert_num_queries(2) as _:
        failed = get_empty_scrapers(target_date)

    assert ['RUS', 'UZB'] == failed


def test_solomon_push_rates(mock_solomon):

    week_date = date(2019, 12, 18)
    rates = []

    for alias in ('RUS', 'UZB'):
        rates.append(
            RateInfo(
                source=alias,
                date=week_date,

                from_mid=None,
                from_code='',

                mid=None,
                buy=None,
                sell=None,
                to_code='',

                rate_dir=Decimal(1),
                rate_inv=None,
            ))

    metrics = send_currency_rates_metrics(rates)

    assert metrics == [
        SolomonNotifier.metric(
            name='rates',
            labels={'source': 'RUS', 'currency_from': '', 'currency_to': '', 'sensor': 'rates'}, value='1'),

        SolomonNotifier.metric(
            name='rates',
            labels={'source': 'UZB', 'currency_from': '', 'currency_to': '', 'sensor': 'rates'}, value='1')
    ]


@pytest.mark.django_db
def test_suspicious_currency(response_mock, read_fixture):

    with response_mock(
        f"GET https://market.nbebank.com/admin/searchsystem/dollarbirr/dollar_against_birrlist.php?x_time=2019-01-09&export=html -> "
        f"200 :{read_fixture('smoke_eth.html').decode('utf-8').replace('Kuwaiti Dinar', 'Kuwaiti D')}"
    ):
        scraper = Ethiopia(date=datetime(2019, 1, 9))
        scraper.scrape()

    messages = Message.objects.all()
    assert len(messages) == 1
    assert 'National Bank of Ethiopi' in messages[0].context['subject']
    assert 'Подозрительный код валюты' in messages[0].context['subject']
    assert 'KUWAITID' in messages[0].context['stext_']
