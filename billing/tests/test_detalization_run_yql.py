import json

import pytest

import yt.wrapper as yt
import yatest

from billing.hot.nirvana.detalization import detalization
from billing.hot.nirvana.tests.test_lib import utils


@pytest.fixture()
def mark_o_tron_marked_data():
    with open(yatest.common.source_path('billing/hot/nirvana/mark_o_tron/tests/canondata/result.json')) as f:
        yield json.load(f)['test_mark_o_tron_run_yql.test_mark_o_tron_run_yql']['marked']


@pytest.fixture()
def mark_o_tron_marked_table(yt_client, yt_root, mark_o_tron_marked_data):
    schema = [
        {'name': 'mark_data', 'type_v3': {'type_name': 'optional', 'item': 'yson'}},
        {'name': 'original_amount', 'type': 'string'},
        {'name': 'original_data', 'type_v3': {'type_name': 'optional', 'item': 'yson'}},
        {'name': 'partition_amount', 'type': 'string'},
    ]
    table_path = yt.ypath_join(yt_root, 'marked')
    yt_client.create('table', table_path, recursive=True, attributes={'schema': schema})
    yt_client.write_table(
        table_path,
        mark_o_tron_marked_data,
    )
    yield table_path
    yt_client.remove(table_path, force=True)


def test_detalization_run_yql(yql_client, yt_client, yt_root, mark_o_tron_marked_table, ns_config):
    """
    Если тест падает с ошибкой, похожей на

    ERROR ya.test: Cannot canonize broken test test_detalization_run_yql.py::test_detalization_run_yql:
    Error while saving results test_detalization_run_yql.py::test_detalization_run_yql from billing/hot/nirvana/detalization/tests:
    [Errno 2] No such file or directory: 'test_mark_o_tron_run_yql.test_mark_o_tron_run_yql/extracted'

    значит при канонизации соседнего теста test_mark_o_tron_run_yql, его canondata/result.json распался на несколько файлов.
    Нужно либо учить фикстуру mark_o_tron_marked_data собирать результат обратно из extracted,
    либо вернуть компактность результату теста test_mark_o_tron_run_yql.
    """
    detalization_sign_config = ns_config['detalization_sign_config']
    location_attrs_config = ns_config['location_attrs_config']
    extract_fields_config = ns_config['extract_fields_config']
    detalization_path = yt.ypath_join(yt_root, 'detalization')
    undetalized_path = yt.ypath_join(yt_root, 'undetalized')

    with yt_client.Transaction() as transaction:
        detalization.run_yql(
            yql_client=yql_client,
            detalization_sign_config=detalization.format_sign_config(detalization_sign_config),
            location_attrs_config=location_attrs_config,
            extract_fields_config=detalization.format_fields_config(extract_fields_config),
            input_marked_events_path=mark_o_tron_marked_table,
            input_prev_undetalized_payments_path=None,
            input_published_events_dir=yt_root,
            input_published_events_range_from='there_is_no_table2',
            input_published_events_range_to='there_is_no_table1',
            output_detalization=detalization_path,
            output_undetalized_payments=undetalized_path,
            transaction=transaction,
        )

    return {
        'detalization': sorted(
            list(yt_client.read_table(detalization_path, format="yson")),
            key=lambda o: (o['payment_id'], o['payout_id'], o['original_accounter_internal_id'], o['mark_accounter_internal_id'])
        ),
        'undetalized': utils.sort_yson(yt_client.read_table(undetalized_path, format="yson")),
    }
