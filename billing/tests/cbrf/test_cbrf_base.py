from datetime import datetime

import pytest
from freezegun import freeze_time

from refs.cbrf.models import Bank
from refs.cbrf.tasks import sync_cbrf
from refs.core.models import Log


@pytest.mark.django_db
def test_cbrf_basic(read_fixture, mock_sleepy_sync, response_mock, monkeypatch):

    date = datetime(2019, 12, 25)

    Bank(bic='044525229', checksum='stale', date_added=date).save()

    with freeze_time(date):

        with response_mock(
            b'GET http://www.cbr.ru/VFS/mcirabis/BIKNew/20191225ED01OSBR.zip -> 200:' +
            read_fixture('20200217_ED807_full.zip')
        ):
            sync_cbrf(None)

    assert Bank.objects.count() == 19
    assert Bank.objects.get(bic='017888102').corr == '40102810245370000065'
    assert Bank.objects.get(bic='024501901').corr == '40102810745370000001'
    assert Bank.objects.get(bic='044525755').corr == '30101810445250000755'

    # Проверяем данные о счетах
    bank = Bank.objects.get(bic='044525229')
    assert bank.accounts[0]['number'] == '30101810845250000229'
    assert bank.accounts[0]['type'] == 'CRSA'

    # Проверяем ограничения.
    bank = Bank.objects.get(bic='044525692')
    assert bank.restrictions == [{'code': 'URRS', 'date': '2017-02-28'}]
    assert bank.accounts == [{
        'number': '30101810045250000692', 'type': 'CRSA', 'restrictions': [{'code': 'FPRS', 'date': '2020-11-16'}]}]

    # провекра обновления
    with freeze_time(date):

        with response_mock(
            b'GET http://www.cbr.ru/VFS/mcirabis/BIKNew/20191225ED01OSBR.zip -> 200:' +
            read_fixture('20200217_ED807_full.zip')
        ):
            sync_cbrf(None)

    assert Bank.objects.get(bic='017888102').corr == '40102810245370000065'
    assert Bank.objects.get(bic='024501901').corr == '40102810745370000001'

    # Проверка пустой выдачи в будни.
    class MockBanks:

        def __init__(self, *args, **kwargs):
            pass

        banks = []

    with freeze_time(datetime(2020, 12, 1)):
        monkeypatch.setattr('refs.cbrf.fetchers.Banks', MockBanks)
        sync_cbrf(None)
        error = Log.objects.latest('id')
        assert error.status == Log.STATUS_FAIL
        assert 'Unexpected empty result for workday.' in error.task_info
