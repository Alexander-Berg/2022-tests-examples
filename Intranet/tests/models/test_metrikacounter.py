import datetime
import random
from typing import Tuple, Optional, Dict, Any, NamedTuple, TypedDict, Union
from unittest import mock

import pytest
import pytz

from idm.core.models.metrikacounter import MetrikaCounter
from idm.tests.utils import random_slug

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def chunk_size_mock():
    with mock.patch('idm.core.models.metrikacounter.MetrikaCounter.INSERT_CHUNK_SIZE',
                    new=mock.PropertyMock(return_value=1)):
        yield


def parse_update_time(dt: str) -> datetime.datetime:
    return datetime.datetime.strptime(dt, MetrikaCounter.UPDATE_TIME_FORMAT).replace(tzinfo=pytz.UTC)


class CounterRecord(NamedTuple('CounterRecord', (('counter_id', int), ('name', str), ('update_time', str)))):
    @property
    def update_time_parsed(self) -> datetime.datetime:
        return parse_update_time(self.update_time)

    @property
    def as_dict(self) -> Dict[str, Any]:
        return {
            'counter_id': str(self.counter_id),
            'name': self.name,
            'update_time': self.update_time_parsed,
        }


def generate_counter_record(
        counter_id: Union[int, str] = None,
        counter_name: Optional[str] = '',
        update_time: Union[str, datetime.datetime] = None,
) -> CounterRecord:
    return CounterRecord(
        counter_id=int(counter_id or random.randint(1, 10 ** 8)),
        name=counter_name is '' and random_slug() or counter_name,
        update_time=isinstance(update_time, str) and update_time or (
                update_time or datetime.datetime.now(tz=pytz.UTC)
        ).strftime(MetrikaCounter.UPDATE_TIME_FORMAT),
    )


def test_sync_from_yt__full():
    yt_counter_data = [
        generate_counter_record(),
        generate_counter_record(),
        generate_counter_record(),
    ]

    # проверяем что перезаписываем записи
    old_record = generate_counter_record(counter_id=yt_counter_data[-1].counter_id)
    MetrikaCounter.objects.create(**old_record.as_dict)
    assert list(MetrikaCounter.objects.order_by('counter_id').values_list('counter_id', 'name', 'update_time')) == \
           [tuple(old_record.as_dict.values())]

    with mock.patch('yql.api.v1.client.YqlClient.query') as yql_query_mock:
        yql_query_mock.return_value = request_mock = mock.Mock()
        request_mock.get_results.return_value = [table_mock] = [mock.Mock()]
        table_mock.get_iterator.return_value = yt_counter_data

        MetrikaCounter.sync_from_yt(full=True)
    yql_query_mock.assert_called_once()
    assert yql_query_mock.call_args_list[0].args == (MetrikaCounter.FULL_TABLE_QUERY,)
    request_mock.run.assert_called_once()
    request_mock.get_results.assert_called_once()

    assert MetrikaCounter.objects.count() == len(yt_counter_data)
    assert list(MetrikaCounter.objects.order_by('counter_id').values_list('counter_id', 'name', 'update_time')) == \
           sorted(
               [tuple(counter_data.as_dict.values()) for counter_data in yt_counter_data],
               key=lambda record: record[0]
           )


@pytest.mark.parametrize('offset_kwargs', [{}, {'time_offset': 60 * 60}])
def test_sync_from_yt__recent_time(offset_kwargs):
    yt_counter_data = [
        generate_counter_record(),
        generate_counter_record(),
        generate_counter_record(),
    ]

    # проверяем что перезаписываем записи только в случае изменений
    changed_record = generate_counter_record(counter_id=yt_counter_data[-1].counter_id)
    changed_id = MetrikaCounter.objects.create(**changed_record.as_dict).id
    unchanged_record = yt_counter_data[0]
    unchanged_id = MetrikaCounter.objects.create(**unchanged_record.as_dict).id
    assert list(MetrikaCounter.objects.order_by('counter_id').values_list('counter_id', 'name', 'update_time')) == \
           sorted([
               tuple(changed_record.as_dict.values()), tuple(unchanged_record.as_dict.values())],
               key=lambda record: record[0],
           )

    with mock.patch('yql.api.v1.client.YqlClient.query') as yql_query_mock:
        yql_query_mock.return_value = request_mock = mock.Mock()
        request_mock.get_results.return_value = [table_mock] = [mock.Mock()]
        table_mock.get_iterator.return_value = yt_counter_data

        MetrikaCounter.sync_from_yt(**offset_kwargs)

    yql_query_mock.assert_called_once()
    assert yql_query_mock.call_args_list[0].args == (
        MetrikaCounter.RECENT_CHANGES_QUERY.format(
            update_time_format=MetrikaCounter.UPDATE_TIME_FORMAT,
            seconds=offset_kwargs.get('time_offset', 24 * 60 * 60)),
    )

    request_mock.run.assert_called_once()
    request_mock.get_results.assert_called_once()

    assert MetrikaCounter.objects.count() == len(yt_counter_data)
    assert list(MetrikaCounter.objects.order_by('counter_id').values_list('counter_id', 'name', 'update_time')) == \
           sorted(
               [tuple(counter_data.as_dict.values()) for counter_data in yt_counter_data],
               key=lambda record: record[0]
           )
    assert not MetrikaCounter.objects.filter(id=changed_id).exists()
    assert not MetrikaCounter.objects.filter(id=unchanged_id).values_list(*unchanged_record.as_dict.keys()) == \
        unchanged_record


def test_sync_from_yt__invalid_update_time():
    invalid_time = random_slug()
    yt_counter_data = [
        generate_counter_record(),
        generate_counter_record(update_time=invalid_time),
        generate_counter_record(),
    ]
    # проверяем что перезаписываем записи
    old_record = generate_counter_record(counter_id=yt_counter_data[-1].counter_id)
    MetrikaCounter.objects.create(**old_record.as_dict)
    assert list(MetrikaCounter.objects.order_by('counter_id').values_list('counter_id', 'name', 'update_time')) == \
           [tuple(old_record.as_dict.values())]

    with mock.patch('yql.api.v1.client.YqlClient.query') as yql_query_mock:
        yql_query_mock.return_value = request_mock = mock.Mock()
        request_mock.get_results.return_value = [table_mock] = [mock.Mock()]
        table_mock.get_iterator.return_value = yt_counter_data

        MetrikaCounter.sync_from_yt(full=True)
    yql_query_mock.assert_called_once()
    assert yql_query_mock.call_args_list[0].args == (MetrikaCounter.FULL_TABLE_QUERY,)
    request_mock.run.assert_called_once()
    request_mock.get_results.assert_called_once()

    assert MetrikaCounter.objects.count() == len(yt_counter_data) - 1
    assert list(MetrikaCounter.objects.order_by('counter_id').values_list('counter_id', 'name', 'update_time')) == \
           sorted(
               [
                   tuple(counter_data.as_dict.values())
                   for counter_data in yt_counter_data
                   if counter_data.update_time != invalid_time
               ],
               key=lambda record: record[0]
           )
