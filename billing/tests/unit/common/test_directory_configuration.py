# -*- coding: utf-8 -*-

import pytest
import hamcrest as hm
from datetime import datetime, timedelta
import logging

from yt.yson import YsonUnicode, YsonMap, YsonList

from billing.log_tariffication.py.lib.archive import (
    DirectoryConfiguration,
    DeletionMode,
    MergeMode,
)

TABLE_NAME_FORMAT = '%Y-%m-%dT%H:%M:%S'


@pytest.mark.parametrize('last_files', [0, 1, 5])
@pytest.mark.parametrize('existing_files', [0, 1, 5])
def test_deletion_by_files(last_files, existing_files):
    now = datetime.now()
    tables = [
        YsonUnicode((now - timedelta(days=i)).strftime(TABLE_NAME_FORMAT))
        for i in range(existing_files)
    ]
    config = DirectoryConfiguration(
        deletion_mode=DeletionMode.AFTER_N_FILES,
        deletion_params={'last_files': last_files},
    )
    tables_for_deletion = config.select_tables_for_deletion(tables)
    logging.info('%s %s', tables, tables_for_deletion)
    hm.assert_that(
        tables_for_deletion,
        hm.contains_inanyorder(*tables[last_files:]),
    )


@pytest.mark.parametrize(
    'last_days, expected_tables_for_deletion',
    [
        [0, ['2020-01-01T01:00:00', '2020-01-01T12:00:00', '2020-01-05T12:00:00', '2020-01-06T11:00:00',
             '2020-01-06T12:00:00', ]],
        [1, ['2020-01-01T01:00:00', '2020-01-01T12:00:00', '2020-01-05T12:00:00', ]],
        [5, ['2020-01-01T01:00:00', '2020-01-01T12:00:00', ]],
    ]
)
def test_deletion_by_days(last_days, expected_tables_for_deletion):
    now = datetime(year=2020, month=1, day=6, hour=12)
    tables = [
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=12).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=11).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=5, hour=12).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=1, hour=12).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=1, hour=1).strftime(TABLE_NAME_FORMAT)),
    ]
    config = DirectoryConfiguration(
        deletion_mode=DeletionMode.AFTER_N_DAYS,
        deletion_params={'last_days': last_days},
    )
    tables_for_deletion = config.select_tables_for_deletion(tables, now)
    hm.assert_that(
        tables_for_deletion,
        hm.equal_to(expected_tables_for_deletion),
    )


@pytest.mark.parametrize(
    'merge_period, expected_groups',
    [
        [None, {
            '2020-01-06T12:00:00': ['2020-01-06T11:00:00', '2020-01-06T12:00:00', ],
            '2020-01-06T10:00:00': ['2020-01-06T10:00:00', ],
            '2020-01-06T09:00:00': ['2020-01-06T09:00:00', ],
            '2020-01-05T12:00:00': ['2020-01-05T12:00:00', ],
            '2020-01-01T12:00:00': ['2020-01-01T01:00:00', '2020-01-01T12:00:00', ],
        }],
        [0, {
            '2020-01-06T12:00:00': ['2020-01-06T11:00:00', '2020-01-06T12:00:00', ],
            '2020-01-06T10:00:00': ['2020-01-06T10:00:00', ],
            '2020-01-06T09:00:00': ['2020-01-06T09:00:00', ],
            '2020-01-05T12:00:00': ['2020-01-05T12:00:00', ],
            '2020-01-01T12:00:00': ['2020-01-01T01:00:00', '2020-01-01T12:00:00', ],
        }],
        [1, {
            '2020-01-05T12:00:00': ['2020-01-05T12:00:00', ],
            '2020-01-01T12:00:00': ['2020-01-01T01:00:00', '2020-01-01T12:00:00', ],
        }],
        [5, {
            '2020-01-01T12:00:00': ['2020-01-01T01:00:00', '2020-01-01T12:00:00', ],
        }]]
)
def test_group_tables_for_merge(merge_period, expected_groups):
    tables = [
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=12).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=11).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=10).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=9).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=5, hour=12).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=1, hour=12).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=1, hour=1).strftime(TABLE_NAME_FORMAT)),
    ]
    for table in tables:
        table.attributes['schema'] = YsonList([YsonMap({'name': 'table'})])
    tables[2].attributes['schema'] = YsonList([YsonMap({'name': 'a', 'type': 'int64'})])
    tables[3].attributes['schema'] = YsonList([YsonMap({'name': 'a', 'type': 'int64'})])
    tables[3].attributes['schema'].attributes = YsonList([YsonMap({'name': 'b', 'type': 'int64'})])
    now = datetime(year=2020, month=1, day=6, hour=12)
    config = DirectoryConfiguration(
        merge_mode=MergeMode.UNORDERED,
        merge_period=merge_period,
    )
    merge_groups = {group_name: [str(table) for table in group]
                    for group_name, group, sorted_by in config.group_tables_for_merge(tables, now)}
    hm.assert_that(
        merge_groups,
        hm.has_entries(expected_groups),
    )


def test_group_sorted_tables_for_merge():
    tables = [
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=12).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=11).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=10).strftime(TABLE_NAME_FORMAT)),
        YsonUnicode(datetime(year=2020, month=1, day=6, hour=9).strftime(TABLE_NAME_FORMAT)),
    ]
    tables[0].attributes['schema'] = YsonList([YsonMap({'name': 'a', 'type': 'int64'})])
    tables[1].attributes['schema'] = YsonList([YsonMap({'name': 'b', 'type': 'int64', 'sort_order': 'ascending'})])
    tables[1].attributes['sorted_by'] = ['b', ]
    tables[2].attributes['schema'] = YsonList([YsonMap({'name': 'a', 'type': 'int64', 'sort_order': 'ascending'})])
    tables[2].attributes['sorted_by'] = ['a', ]
    tables[3].attributes['schema'] = YsonList([YsonMap({'name': 'a', 'type': 'int64', 'sort_order': 'ascending'})])
    tables[3].attributes['sorted_by'] = ['a', ]
    now = datetime(year=2020, month=1, day=6, hour=12)
    config = DirectoryConfiguration(
        merge_mode=MergeMode.ORDERED,
        merge_period=None,
    )
    merge_groups = {group_name: ([str(table) for table in group], sorted_by)
                    for group_name, group, sorted_by in config.group_tables_for_merge(tables, now)}

    expected_groups = {
        '2020-01-06T12:00:00': (['2020-01-06T12:00:00', ], None),
        '2020-01-06T11:00:00': (['2020-01-06T11:00:00', ], ['b', ]),
        '2020-01-06T10:00:00': (['2020-01-06T09:00:00', '2020-01-06T10:00:00'], ['a', ]),
    }
    hm.assert_that(
        merge_groups,
        hm.has_entries(expected_groups),
    )


def test_empty_config():
    assert str(DirectoryConfiguration.from_dict({})) == str(DirectoryConfiguration(recursive=False))
