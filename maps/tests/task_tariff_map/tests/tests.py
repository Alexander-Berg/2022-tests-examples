from maps.wikimap.stat.tasks_payment.dictionaries.task_tariff_map.lib import task_tariff_map

from datetime import date
from nile.api.v1 import Record
import pytest


def test_should_get_task_prefixes():
    assert task_tariff_map._get_task_prefixes({'task_prefix': 'prefix'}) == ['prefix']
    assert task_tariff_map._get_task_prefixes({'task_prefixes': ['prefix 1', 'prefix 2']}) == ['prefix 1', 'prefix 2']


def test_should_fail_getting_prefixes_if_two_fields_used_simultaneously():
    with pytest.raises(AssertionError, match='One key of .* must be present in'):
        task_tariff_map._get_task_prefixes({'task_prefix': 'prefix 1', 'task_prefixes': ['prefix 2']})


def test_should_fail_if_prefix_is_empty():
    with pytest.raises(AssertionError, match='Prefixes list cannot be empty'):
        task_tariff_map._get_task_prefixes({'task_prefixes': []})


def test_should_make_record():
    assert (
        task_tariff_map._make_record('task_id 1', ['lvl 1'], 1) ==
        Record(task_id='task_id 1', task_name_tree='\tlvl 1\t', seconds_per_task=1)
    )
    assert (
        task_tariff_map._make_record('task_id 2', ['lvl 1', 'lvl 2'], 2) ==
        Record(task_id='task_id 2', task_name_tree='\tlvl 1\tlvl 2\t', seconds_per_task=2)
    )
    assert (
        task_tariff_map._make_record('task_id 3', ['lvl 1', 'lvl 2', 'lvl 3'], 3) ==
        Record(task_id='task_id 3', task_name_tree='\tlvl 1\tlvl 2\tlvl 3\t', seconds_per_task=3)
    )


def test_should_fail_making_record_with_empty_task_name_tree():
    with pytest.raises(AssertionError, match="task_name_tree.*empty"):
        task_tariff_map._make_record('task_id', [], 0)


def test_should_make_records_for_task():
    assert (
        task_tariff_map._make_records_for_task('task 1', 'prefix/', ['lvl 1'], 1) ==
        [
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttask 1\t', seconds_per_task=1)
        ]
    )


def test_should_make_records_for_tariff_without_dates():
    tariff = {
        'tariff_name': 'tariff name',
        'seconds': 1,
        'tasks': ['task 1', 'task 2'],
    }
    assert (
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1']) ==
        [
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\ttask 1\t', seconds_per_task=1),

            Record(task_id='prefix/task 2', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 2', task_name_tree='\tlvl 1\ttariff name\t', seconds_per_task=1),
            Record(task_id='prefix/task 2', task_name_tree='\tlvl 1\ttariff name\ttask 2\t', seconds_per_task=1),
        ]
    )


def test_should_make_records_for_tariff_with_start_and_end_dates():
    tariff = {
        'tariff_name': 'tariff name',
        'start_date': date(2020, 4, 27),
        'end_date': date(2020, 4, 29),
        'seconds': 1,
        'tasks': ['task 1'],
    }
    assert (
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1']) ==
        [
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\ttask 1\t', seconds_per_task=1),
        ]
    )
    assert task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 26), 'prefix/', ['lvl 1']) == []
    assert task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 30), 'prefix/', ['lvl 1']) == []


def test_should_make_records_for_one_day_tariff():
    tariff = {
        'tariff_name': 'tariff name',
        'start_date': date(2020, 4, 28),
        'end_date': date(2020, 4, 28),
        'seconds': 1,
        'tasks': ['task 1'],
    }
    assert (
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1']) ==
        [
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\ttask 1\t', seconds_per_task=1),
        ]
    )
    assert task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 27), 'prefix/', ['lvl 1']) == []
    assert task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 29), 'prefix/', ['lvl 1']) == []


def test_should_make_records_for_tariff_with_start_date():
    tariff = {
        'tariff_name': 'tariff name',
        'start_date': date(2020, 4, 28),
        'seconds': 1,
        'tasks': ['task 1'],
    }
    assert task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 27), 'prefix/', ['lvl 1']) == []
    assert (
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1']) ==
        [
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\ttask 1\t', seconds_per_task=1),
        ]
    )


def test_should_make_records_for_tariff_with_end_date():
    tariff = {
        'tariff_name': 'tariff name',
        'end_date': date(2020, 4, 28),
        'seconds': 1,
        'tasks': ['task 1'],
    }
    assert (
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1']) ==
        [
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tlvl 1\ttariff name\ttask 1\t', seconds_per_task=1),
        ]
    )
    assert task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 29), 'prefix/', ['lvl 1']) == []


def test_should_fail_making_records_for_tariff_without_seconds():
    tariff = {
        'tariff_name': 'tariff name',
        # 'seconds': 0
        'tasks': ['task 1', 'task 2']
    }
    with pytest.raises(AssertionError, match='`seconds` key is absent: {.*tariff name.*}'):
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1'])


def test_should_fail_making_records_for_tariff_without_name():
    tariff = {
        # 'tariff_name': 'tariff name',
        'seconds': 0,
        'tasks': ['task 1', 'task 2']
    }
    with pytest.raises(AssertionError, match='`tariff_name` key is absent: {.*task 1.*task 2.*}'):
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1'])


def test_should_fail_making_records_for_tariff_without_tasks():
    tariff = {
        'tariff_name': 'tariff name',
        'seconds': 0,
        # 'tasks': ['task 1', 'task 2']
    }
    with pytest.raises(AssertionError, match='`tasks` key is absent: {.*tariff name.*}'):
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1'])


def test_should_fail_making_records_for_tariff_with_empty_tasks():
    tariff = {
        'tariff_name': 'tariff name',
        'seconds': 0,
        'tasks': []
    }
    with pytest.raises(AssertionError, match='Tasks list is empty in the tariff: {.*tariff name.*}'):
        task_tariff_map._make_records_for_tariff(tariff, date(2020, 4, 28), 'prefix/', ['lvl 1'])


def test_should_make_records_for_task_prefix():
    tariffs = [
        {
            'tariff_name': 'tariff 1',
            'seconds': 1,
            'tasks': ['task 11', 'task 12']
        },
        {
            'tariff_name': 'tariff 2',
            'seconds': 2,
            'tasks': ['task 2']
        },
    ]
    assert (
        task_tariff_map._make_records_for_task_prefix(tariffs, date(2020, 4, 28), 'prefix/', ['lvl 1']) == [
            Record(task_id='prefix/task 11', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 11', task_name_tree='\tlvl 1\ttariff 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 11', task_name_tree='\tlvl 1\ttariff 1\ttask 11\t', seconds_per_task=1),
            Record(task_id='prefix/task 12', task_name_tree='\tlvl 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 12', task_name_tree='\tlvl 1\ttariff 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 12', task_name_tree='\tlvl 1\ttariff 1\ttask 12\t', seconds_per_task=1),
            Record(task_id='prefix/task 2', task_name_tree='\tlvl 1\t', seconds_per_task=2),
            Record(task_id='prefix/task 2', task_name_tree='\tlvl 1\ttariff 2\t', seconds_per_task=2),
            Record(task_id='prefix/task 2', task_name_tree='\tlvl 1\ttariff 2\ttask 2\t', seconds_per_task=2),
        ]
    )


def test_should_make_records_for_tariffs():
    tariffs_description = {
        'task_group': 'group',
        'task_subgroup': 'subgroup',
        'task_prefix': 'prefix/',
        'tariffs': [
            {
                'tariff_name': 'tariff 1',
                'seconds': 1,
                'tasks': ['task 11', 'task 12']
            },
            {
                'tariff_name': 'tariff 2',
                'seconds': 2,
                'tasks': ['task 2']
            },
            {
                'tariff_name': 'tariff 3',
                'end_date': date(2020, 4, 27),
                'seconds': 3,
                'tasks': ['task 3']
            },
            {
                'tariff_name': 'tariff 4',
                'start_date': date(2020, 4, 29),
                'seconds': 4,
                'tasks': ['task 4']
            },
        ]
    }
    assert (
        task_tariff_map.make_records(tariffs_description, date(2020, 4, 28)) ==
        [
            Record(task_id='prefix/task 11', task_name_tree='\tall\t', seconds_per_task=1),
            Record(task_id='prefix/task 11', task_name_tree='\tall\tgroup\t', seconds_per_task=1),
            Record(task_id='prefix/task 11', task_name_tree='\tall\tgroup\tsubgroup\t', seconds_per_task=1),
            Record(task_id='prefix/task 11', task_name_tree='\tall\tgroup\tsubgroup\ttariff 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 11', task_name_tree='\tall\tgroup\tsubgroup\ttariff 1\ttask 11\t', seconds_per_task=1),

            Record(task_id='prefix/task 12', task_name_tree='\tall\t', seconds_per_task=1),
            Record(task_id='prefix/task 12', task_name_tree='\tall\tgroup\t', seconds_per_task=1),
            Record(task_id='prefix/task 12', task_name_tree='\tall\tgroup\tsubgroup\t', seconds_per_task=1),
            Record(task_id='prefix/task 12', task_name_tree='\tall\tgroup\tsubgroup\ttariff 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 12', task_name_tree='\tall\tgroup\tsubgroup\ttariff 1\ttask 12\t', seconds_per_task=1),

            Record(task_id='prefix/task 2', task_name_tree='\tall\t', seconds_per_task=2),
            Record(task_id='prefix/task 2', task_name_tree='\tall\tgroup\t', seconds_per_task=2),
            Record(task_id='prefix/task 2', task_name_tree='\tall\tgroup\tsubgroup\t', seconds_per_task=2),
            Record(task_id='prefix/task 2', task_name_tree='\tall\tgroup\tsubgroup\ttariff 2\t', seconds_per_task=2),
            Record(task_id='prefix/task 2', task_name_tree='\tall\tgroup\tsubgroup\ttariff 2\ttask 2\t', seconds_per_task=2)
        ]
    )


def test_should_make_records_for_tariffs_without_subgroup():
    tariffs_description = {
        'task_group': 'group',
        'task_prefix': 'prefix/',
        'tariffs': [
            {
                'tariff_name': 'tariff 1',
                'seconds': 1,
                'tasks': ['task 1']
            }
        ]
    }
    assert (
        task_tariff_map.make_records(tariffs_description, date(2020, 4, 28)) ==
        [
            Record(task_id='prefix/task 1', task_name_tree='\tall\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tall\tgroup\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tall\tgroup\ttariff 1\t', seconds_per_task=1),
            Record(task_id='prefix/task 1', task_name_tree='\tall\tgroup\ttariff 1\ttask 1\t', seconds_per_task=1),
        ]
    )


def test_should_make_records_with_task_prefixes():
    tariffs_description = {
        'task_group': 'group',
        'task_prefixes': ['prefix 1/', 'prefix 2/'],
        'tariffs': [
            {
                'tariff_name': 'tariff',
                'seconds': 1,
                'tasks': ['task']
            },
        ]
    }
    assert (
        task_tariff_map.make_records(tariffs_description, date(2020, 4, 28)) ==
        [
            Record(task_id='prefix 1/task', task_name_tree='\tall\t', seconds_per_task=1),
            Record(task_id='prefix 1/task', task_name_tree='\tall\tgroup\t', seconds_per_task=1),
            Record(task_id='prefix 1/task', task_name_tree='\tall\tgroup\ttariff\t', seconds_per_task=1),
            Record(task_id='prefix 1/task', task_name_tree='\tall\tgroup\ttariff\ttask\t', seconds_per_task=1),

            Record(task_id='prefix 2/task', task_name_tree='\tall\t', seconds_per_task=1),
            Record(task_id='prefix 2/task', task_name_tree='\tall\tgroup\t', seconds_per_task=1),
            Record(task_id='prefix 2/task', task_name_tree='\tall\tgroup\ttariff\t', seconds_per_task=1),
            Record(task_id='prefix 2/task', task_name_tree='\tall\tgroup\ttariff\ttask\t', seconds_per_task=1),
        ]
    )


def test_should_fail_making_records_without_task_group():
    tariffs_description = {
        # 'task_group': 'group 1',
        'task_prefix': 'prefix 1/',
        'tariffs': [
            {
                'tariff_name': 'tariff 1',
                'seconds': 1,
                'tasks': ['task 1']
            }
        ]
    }
    with pytest.raises(AssertionError, match='`task_group` key is absent: {.*prefix 1.*}'):
        task_tariff_map.make_records(tariffs_description, date(2020, 4, 28))


def test_should_fail_making_records_without_task_prefix():
    tariffs_description = {
        'task_group': 'group 1',
        # 'task_prefix': 'prefix 1/',
        'tariffs': [
            {
                'tariff_name': 'tariff 1',
                'seconds': 1,
                'tasks': ['task 1']
            }
        ]
    }
    with pytest.raises(AssertionError, match='One key of .* must be present in'):
        task_tariff_map.make_records(tariffs_description, date(2020, 4, 28))


def test_should_fail_making_records_without_tariffs():
    tariffs_description = {
        'task_group': 'group 1',
        'task_prefix': 'prefix 1/',
        # 'tariffs': [...]
    }
    with pytest.raises(AssertionError, match='`tariffs` key is absent: {.*group 1.*}'):
        task_tariff_map.make_records(tariffs_description, date(2020, 4, 28))


def test_should_fail_making_records_with_empty_tariffs():
    tariffs_description = {
        'task_group': 'group 1',
        'task_prefix': 'prefix 1/',
        'tariffs': []
    }
    with pytest.raises(AssertionError, match='Tariffs list is empty in the tariffs: {.*group 1.*}'):
        task_tariff_map.make_records(tariffs_description, date(2020, 4, 28))
