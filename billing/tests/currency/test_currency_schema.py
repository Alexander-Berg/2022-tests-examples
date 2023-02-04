from datetime import datetime

import pytest

from refs.currency.exporters import YTSynchronizer, get_yt_datetime, \
    get_date_from_yt, get_yt_date, get_datetime_from_yt
from refs.currency.models import Rate, SourceInfo


def test_models():

    date = datetime(2018, 8, 28).date()

    rate = Rate(
        source='RUS', from_code='RUB', to_code='USD',
        from_amount=1, rate_dir=1, rate_inv=1, date=date)

    assert str(rate) == '1RUB -> 1USD [1]'

    info = SourceInfo(source='RUS', date_latest=date)
    assert str(info) == 'RUS: 2018-08-28'


@pytest.mark.django_db
def test_basic(client, django_assert_num_queries):

    date = datetime(2018, 8, 28).date()

    rates = [
        Rate(
            source='RUS', from_code='RUB', to_code='USD',
            from_amount=1, rate_dir=1, rate_inv=1, date=date),
    ]
    Rate.objects.bulk_create(rates)

    with django_assert_num_queries(1) as _:

        result = client.get('/api/currency/?query=query{sources {alias}}').json()
        assert {'alias': 'RUS'} in result['data']['sources']

    with django_assert_num_queries(0) as _:

        result = client.get('/api/currency/?query=query{listing {alpha name}}', HTTP_ACCEPT_LANGUAGE='ru').json()
        assert {'alpha': 'JPY', 'name': 'Иена'} in result['data']['listing']

    with django_assert_num_queries(1) as _:

        result = client.get(
            '/api/currency/?query=query{rates('
            'code:"RUB", date:"2018-08-28", source:"RUS", fromCode:"RUB", toCode:"USD")'
            ' {source rateDir}}').json()

        assert result == {'data': {'rates': [{'source': 'RUS', 'rateDir': '1.000000'}]}}

    with django_assert_num_queries(2) as _:

        result = client.get('/api/currency/?query=query{rates(code:"RUB") {source rateDir}}').json()
        assert result == {'data': {'rates': [{'source': 'RUS', 'rateDir': '1.000000'}]}}



class YTMock:
    """Мок для операций с Yt."""

    def create(self, table, table_path, attributes=None):
        pass

    def mount_table(self, table_path, **kwargs):
        pass

    def set_attribute(self, table_path, attr_name, value):
        pass

    def get_attribute(self, table_path, attr_name):
        return '2020-06-29 07:36:44'

    def insert_rows(self, table_path, rows):
        pass


@pytest.mark.django_db
def test_yt_export(monkeypatch):

    date_tm = datetime(2018, 8, 28)

    date = date_tm.date()

    converted_date_tm = get_datetime_from_yt(get_yt_datetime(date_tm))

    assert date_tm == converted_date_tm

    converted_date = get_date_from_yt(get_yt_date(date))

    assert date == converted_date

    rates = [
        Rate(
            source='RUS', from_code='RUB', to_code='USD',from_amount=1,
            rate_dir=x, rate_inv=1, date=date
        ) for x in range(50)
    ]
    Rate.objects.bulk_create(rates)

    monkeypatch.setattr(
        'yt.wrapper.table_commands.exists',
        lambda *args, **kwargs: False
    )

    syncer = YTSynchronizer()

    syncer.client = YTMock()

    syncer.run()
