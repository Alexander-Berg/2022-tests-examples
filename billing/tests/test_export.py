from unittest import mock
import pytest
from random import randint
from collections import defaultdict
import pytz
from billing.apikeys.apikeys import mapper
from datetime import datetime, timedelta
from billing.apikeys.apikeys.mapper.export import StatExport


TEST_DATA_SIZE = 10
ALREADY_EXPORTED_DATA_SIZE = TEST_DATA_SIZE // 2


class FakeYtExport:
    tables = defaultdict(list)

    class FakeYt:
        def insert_rows(self, table, serialized_data):
            FakeYtExport.tables[table] += serialized_data

    def __init__(self):
        self.yt = self.FakeYt()

    def upload_stat(self, data, with_indexes=False):
        self.yt.insert_rows('stats', data)

    @staticmethod
    def clear():
        FakeYtExport.tables = defaultdict(list)


@pytest.fixture
def hourly_stats(mongomock, simple_key_delayed):
    online_ksc, delayed_ksc = simple_key_delayed.get_counters()
    assert mapper.Unit.getone(id=online_ksc.unit_id).cc == 'hits'
    assert mapper.Unit.getone(id=delayed_ksc.unit_id).cc == 'hits_delayed'
    stats = [
        mapper.HourlyStatArchived(
            counter_id=online_ksc.counter.id,
            dt=datetime(2015, 1, 1, 12, 0, tzinfo=pytz.utc) + timedelta(days=i) + timedelta(hours=6 if i % 2 else 18),
            value=i+1
        ).save()
        for i in range(TEST_DATA_SIZE-1)
    ] + [
        # TODO Последний элемент - отложенный счетчик, как-то более внятно это изложить?
        #  В самих тестах в проверках assert как-то коряво обрабатывается это
        mapper.HourlyStatArchived(
            counter_id=delayed_ksc.id,
            dt=datetime(2015, 1, 1, 12, 0, tzinfo=pytz.utc),
            value=1
        ).save()
    ]
    yield stats
    for stat in stats:
        stat.delete()


@pytest.fixture
def stat_exports(mongomock, simple_key_delayed, hourly_stats):
    estats = [
        StatExport(stat_id=hourly_stat.id,
                   key_id=simple_key_delayed.id,
                   tariff=None,
                   unit='hits',
                   dt=hourly_stat.dt,
                   value=hourly_stat.value,
                   rand=randint(0, StatExport.RANDS)
                   ).save()
        for hourly_stat in hourly_stats
    ]
    yield estats
    for stat in estats:
        stat.delete()


@pytest.fixture
def already_exported_stat(mongomock, simple_key_delayed, hourly_stats):
    estats = [
        StatExport(stat_id=hourly_stat.id,
                   key_id=simple_key_delayed.id,
                   tariff=None,
                   unit='hits',
                   dt=hourly_stat.dt,
                   value=hourly_stat.value,
                   rand=randint(0, StatExport.RANDS)
                   ).save()
        for hourly_stat in hourly_stats[:ALREADY_EXPORTED_DATA_SIZE]
    ]
    yield estats
    for stat in estats:
        stat.delete()


def test_export_prepared_data_directly_to_yt(mongomock, simple_key_delayed, hourly_stats, stat_exports):
    with mock.patch('billing.apikeys.apikeys.mapper.task.YtExport', new=FakeYtExport):
        task = mapper.task.StatsYtExportTask(
            export_part={
                'range_from': 0,
                'range_to': StatExport.RANDS
            }
        )
        task._do_task()

        assert StatExport.objects.count() == 0
        assert len(FakeYtExport.tables['stats']) == len(stat_exports)
        assert FakeYtExport.tables['stats'] == [
            {
                'stat_id': str(stat.id),
                'key_id': str(simple_key_delayed.id),
                'tariff': None,
                'unit': 'hits',
                'dt': int(stat.dt.timestamp()),
                'value': stat.value
            } for stat in hourly_stats
        ]
        FakeYtExport.clear()


def test_prepare_data_to_be_exported(mongomock, simple_key_delayed, hourly_stats):
    task1 = mapper.task.PrepareStatsYtExportTask(hour_range={'from': 0, 'to': 11})
    task2 = mapper.task.PrepareStatsYtExportTask(hour_range={'from': 12, 'to': 23})
    task1._do_task()
    task2._do_task()

    result = [
        (item.stat_id, item.key_id, item.tariff, item.unit, item.dt, item.value)
        for item in StatExport.objects.all()
    ]

    assert len(result) == len(hourly_stats)
    assert result == [
        (stat.id, simple_key_delayed.id, None, 'hits', stat.dt, stat.value)
        for stat in hourly_stats[::2]
    ] + [
        (stat.id, simple_key_delayed.id, None, 'hits', stat.dt, stat.value)
        for stat in hourly_stats[1:-1:2]
    ] + [
        (hourly_stats[-1].id, simple_key_delayed.id, None, 'hits_delayed', hourly_stats[-1].dt, hourly_stats[-1].value)
    ]
    new_last_stat_id = result[4][0]
    assert task1.last_stat_id == new_last_stat_id
    new_last_stat_id = result[-1][0]
    assert task2.last_stat_id == new_last_stat_id


def test_prepare_data_to_be_exported_when_some_already_exported(mongomock, simple_key_delayed, hourly_stats):
    task = mapper.task.PrepareStatsYtExportTask(
        hour_range={'from': 1, 'to': 24},
        last_stat_id=hourly_stats[ALREADY_EXPORTED_DATA_SIZE-1].pk
    )
    task._do_task()

    result = [
        (item.stat_id, item.key_id, item.tariff, item.unit, item.dt, item.value)
        for item in StatExport.objects.all()
    ]

    assert len(result) == TEST_DATA_SIZE - ALREADY_EXPORTED_DATA_SIZE
    assert result == [
        (stat.id, simple_key_delayed.id, None, 'hits', stat.dt, stat.value)
        for stat in hourly_stats[ALREADY_EXPORTED_DATA_SIZE:-1]
    ] + [
        (hourly_stats[-1].id, simple_key_delayed.id, None, 'hits_delayed', hourly_stats[-1].dt, hourly_stats[-1].value)
    ]
    new_last_stat_id = result[-1][0]
    assert task.last_stat_id == new_last_stat_id
