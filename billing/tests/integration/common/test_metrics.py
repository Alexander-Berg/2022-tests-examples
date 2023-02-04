# -*- coding: utf-8 -*-

import datetime
import time

import pytest
from unittest import mock
import hamcrest as hm

import yt.wrapper as yt

from billing.log_tariffication.py.jobs.common import metrics
from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_METRICS_ATTR,
)
from billing.library.python.yql_utils.query_metrics import QUERY_METRICS_YT_ATTR
from billing.library.python import yt_utils


@pytest.fixture(name='task_dir')
def create_task_dir(yt_root, yt_client):
    path = yt.ypath_join(yt_root, 'task_dir')
    yt_client.create('map_node', path)
    return path


def test_row_count(yt_client, task_dir):
    yt_client.set_attribute(task_dir, LOG_TARIFF_METRICS_ATTR, ['last_table_row_count', ])
    table1 = yt.ypath_join(task_dir, '2020-01-01T01:01:01')
    yt_client.create('table', table1)
    yt_client.write_table(table1, [{'a': 1, 'b': 2, 'c': 3}, ] * 2)

    table2 = yt.ypath_join(task_dir, '2020-02-02T02:02:02')
    yt_client.create('table', table2)
    yt_client.write_table(table2, [{'a': -1, 'b': -2, 'c': -3}, ])

    current_time = datetime.datetime.strptime('2020-02-02T02:02:03', '%Y-%m-%dT%H:%M:%S')
    metrics_dict = metrics.run_job(yt_client, 'cluster_X', task_dir, common_labels={'a': 'b'}, current_time=current_time)

    excpected_metrics_dict = {
        'sensors': [
            {'labels': {'sensor': 'last_table_row_count', 'folder': 'task_dir'}, 'type': 'IGAUGE', 'value': 1},
        ],
        'commonLabels': {'a': 'b', 'host': 'cluster_X'}
    }

    hm.assert_that(
        metrics_dict,
        hm.equal_to(excpected_metrics_dict)
    )


def test_last_table_creation_time(yt_client, task_dir):
    yt_client.set_attribute(task_dir, LOG_TARIFF_METRICS_ATTR, ['last_table_creation_time', ])
    table1 = yt.ypath_join(task_dir, 'table_1_1')
    yt_client.create('table', table1)
    yt_client.write_table(table1, [{'a': 1, 'b': 2, 'c': 3}, ] * 2)

    time.sleep(1)

    table2 = yt.ypath_join(task_dir, 'table_1')
    yt_client.create('table', table2)
    yt_client.write_table(table2, [{'a': -1, 'b': -2, 'c': -3}, ])

    creation_time = yt_utils.yt.get_node_attr(yt_client, table2, 'creation_time')
    current_time = datetime.datetime.strptime(
        creation_time, '%Y-%m-%dT%H:%M:%S.%fZ'
    ).replace(tzinfo=datetime.timezone.utc) + datetime.timedelta(seconds=1)
    metrics_dict = metrics.run_job(yt_client, 'cluster_X', task_dir, common_labels={'host': 'Y'}, current_time=current_time)

    excpected_metrics_dict = {
        'sensors': [
            {'labels': {'sensor': 'last_table_creation_time', 'folder': 'task_dir'}, 'type': 'IGAUGE', 'value': 1},
        ],
        'commonLabels': {'host': 'cluster_X'}
    }

    hm.assert_that(
        metrics_dict,
        hm.equal_to(excpected_metrics_dict)
    )


def test_row_count_and_last_table_creation_time(yt_client, task_dir):
    yt_client.set_attribute(task_dir, LOG_TARIFF_METRICS_ATTR, ['last_table_creation_time', 'last_table_row_count'])

    table1 = yt.ypath_join(task_dir, '2020-01-01T01:01:01')
    yt_client.create('table', table1)
    yt_client.write_table(table1, [{'a': 1, 'b': 2, 'c': 3}, ] * 12)

    table2 = yt.ypath_join(task_dir, '2020-02-02T02:02:02')
    yt_client.create('table', table2)

    creation_time = yt_utils.yt.get_node_attr(yt_client, table2, 'creation_time')
    current_time = datetime.datetime.strptime(
        creation_time, '%Y-%m-%dT%H:%M:%S.%fZ').replace(tzinfo=datetime.timezone.utc
                                                        ) + datetime.timedelta(seconds=3600, milliseconds=459)
    metrics_dict = metrics.run_job(yt_client, 'cluster_X', task_dir, current_time=current_time)

    excpected_metrics_dict = {
        'sensors': [
            {'labels': {'sensor': 'last_table_creation_time', 'folder': 'task_dir'}, 'type': 'IGAUGE', 'value': 3600},
            {'labels': {'sensor': 'last_table_row_count', 'folder': 'task_dir'}, 'type': 'IGAUGE', 'value': 0},
        ],
        'commonLabels': {'host': 'cluster_X'}
    }

    hm.assert_that(
        metrics_dict,
        hm.equal_to(excpected_metrics_dict)
    )


def test_empty_folder(yt_client, task_dir):
    yt_client.set_attribute(task_dir, LOG_TARIFF_METRICS_ATTR, ['last_table_creation_time', 'last_table_row_count'])

    current_time = datetime.datetime.strptime('2020-02-02T02:02:03', '%Y-%m-%dT%H:%M:%S')
    metrics_dict = metrics.run_job(yt_client, 'cluster_X', task_dir, current_time=current_time)

    excpected_metrics_dict = {
        'sensors': [],
        'commonLabels': {'host': 'cluster_X'}
    }

    hm.assert_that(
        metrics_dict,
        hm.equal_to(excpected_metrics_dict)
    )


def test_folder_without_tables(yt_client, task_dir):
    yt_client.set_attribute(task_dir, LOG_TARIFF_METRICS_ATTR, ['last_table_creation_time', 'last_table_row_count'])

    dir_1 = yt.ypath_join(task_dir, 'dir_1')
    yt_client.create('map_node', dir_1)
    yt_client.set_attribute(dir_1, LOG_TARIFF_METRICS_ATTR, ['last_table_row_count', 'last_table_creation_time'])

    current_time = datetime.datetime.strptime('2020-02-02T02:02:03', '%Y-%m-%dT%H:%M:%S')
    metrics_dict = metrics.run_job(yt_client, 'cluster_X', task_dir, current_time=current_time)

    excpected_metrics_dict = {
        'sensors': [],
        'commonLabels': {'host': 'cluster_X'}
    }

    hm.assert_that(
        metrics_dict,
        hm.equal_to(excpected_metrics_dict)
    )


def test_metrics_tree(yt_client, task_dir):
    dir_1 = yt.ypath_join(task_dir, 'dir_1')
    yt_client.create('map_node', dir_1)
    yt_client.set_attribute(dir_1, LOG_TARIFF_METRICS_ATTR, ['last_table_row_count', ])
    table1 = yt.ypath_join(dir_1, '2020-01-01T01:01:01')
    yt_client.create('table', table1)
    yt_client.write_table(table1, [{'a': 1, 'b': 2, 'c': 3}, ] * 3)

    dir_2 = yt.ypath_join(task_dir, 'dir_2')
    yt_client.create('map_node', dir_2)
    yt_client.set_attribute(dir_2, LOG_TARIFF_METRICS_ATTR, ['last_table_creation_time', ])
    table2 = yt.ypath_join(dir_2, '2020-02-02T02:02:02')
    yt_client.create('table', table2)
    yt_client.write_table(table2, [{'a': 1, 'b': 2, 'c': 3}, ])

    dir_3 = yt.ypath_join(dir_1, 'dir_3')
    yt_client.create('map_node', dir_3)
    yt_client.set_attribute(dir_3, LOG_TARIFF_METRICS_ATTR, ['last_table_row_count', ])
    table3 = yt.ypath_join(dir_3, '2020-03-03T03:03:03')
    yt_client.create('table', table3)
    yt_client.write_table(table3, [{'a': 1, 'b': 2, 'c': 3}, ] * 2)

    dir_4 = yt.ypath_join(dir_1, 'dir_4')
    yt_client.create('map_node', dir_4)
    yt_client.set_attribute(dir_4, LOG_TARIFF_METRICS_ATTR, ['last_table_row_count', 'last_table_creation_time'])

    creation_time = yt_utils.yt.get_node_attr(yt_client, table2, 'creation_time')
    current_time = datetime.datetime.strptime(
        creation_time, '%Y-%m-%dT%H:%M:%S.%fZ'
    ).replace(tzinfo=datetime.timezone.utc) + datetime.timedelta(seconds=3600, milliseconds=459)
    metrics_dict = metrics.run_job(yt_client, 'cluster_X', task_dir, current_time=current_time)

    excpected_metrics_dict = {
        'sensors': [
            {
                'labels': {'sensor': 'last_table_row_count', 'folder': 'task_dir/dir_1'},
                'type': 'IGAUGE',
                'value': 3,
            },
            {
                'labels': {'sensor': 'last_table_row_count', 'folder': 'task_dir/dir_1/dir_3'},
                'type': 'IGAUGE',
                'value': 2,
            },
            {
                'labels': {'sensor': 'last_table_creation_time', 'folder': 'task_dir/dir_2'},
                'type': 'IGAUGE',
                'value': 3600,
            },
        ],
        'commonLabels': {'host': 'cluster_X'}
    }

    hm.assert_that(
        metrics_dict,
        hm.equal_to(excpected_metrics_dict)
    )


def test_query_metrics(yt_client, task_dir):
    dir_1 = yt.ypath_join(task_dir, 'dir_1')
    yt_client.create('map_node', dir_1)
    yt_client.set_attribute(dir_1, LOG_TARIFF_METRICS_ATTR, ['query_metrics', ])

    table1 = yt.ypath_join(dir_1, '2020-01-01T01:01:01')
    yt_client.create('table', table1)
    yt_client.set_attribute(table1, QUERY_METRICS_YT_ATTR, [
        {
            'name': 'skip_table',
            'type': '_',
            'value': '',
        },
    ])

    table2 = yt.ypath_join(dir_1, '2020-01-01T01:01:02')
    yt_client.create('table', table2)
    yt_client.set_attribute(table2, QUERY_METRICS_YT_ATTR, [
        {
            'name': 'simple_metric',
            'type': 'float',
            'value': '15.5',
        },
        {
            'name': 'timestamp_metric',
            'type': 'timestamp',
            'value': '1614830852',
            'labels': {
                'cluster': 'hahn'
            }
        },
    ])

    with mock.patch('billing.library.python.yql_utils.query_metrics.time') as patched:
        patched.time.return_value = 1614830856.0
        metrics_dict = metrics.run_job(yt_client, 'cluster_X', task_dir)

    excpected_metrics_dict = {
        'sensors': [
            {
                'labels': {'sensor': 'simple_metric', 'folder': 'task_dir/dir_1'},
                'type': 'DGAUGE',
                'value': 15.5,
            },
            {
                'labels': {'sensor': 'timestamp_metric', 'folder': 'task_dir/dir_1', 'cluster': 'hahn'},
                'type': 'IGAUGE',
                'value': 4,
            },
        ],
        'commonLabels': {'host': 'cluster_X'}
    }

    hm.assert_that(
        metrics_dict,
        hm.equal_to(excpected_metrics_dict)
    )
