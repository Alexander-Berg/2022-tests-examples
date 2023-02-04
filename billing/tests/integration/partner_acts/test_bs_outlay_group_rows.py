import arrow
import pytest

import yt.wrapper as yt

from billing.log_tariffication.py.lib import constants
from billing.log_tariffication.py.jobs.partner_acts import bs_outlay_group_rows
from billing.log_tariffication.py.tests import constants as tests_constants
from billing.log_tariffication.py.tests.integration import conftest_partner


def ts(dt: str) -> int:
    return arrow.get(dt).replace(tzinfo=constants.MSK_TZ).int_timestamp


TARIFFED_META = {
    constants.RUN_ID_KEY: '2020-10-11T11:00:00',
    constants.LOG_INTERVAL_KEY: tests_constants.CURR_LOG_INTERVAL.to_meta(),
}
PREV_ACT_META = {
    constants.RUN_ID_KEY: '2020-09-30',
    constants.ACT_DT_KEY: '2020-09-30',
    constants.LOG_INTERVAL_KEY: tests_constants.PREV_LOG_INTERVAL.to_meta(),
}
CURR_ACT_META = {
    constants.RUN_ID_KEY: '2020-10-31',
    constants.ACT_DT_KEY: '2020-10-31',
    constants.PREVIOUS_RUN_ID_KEY: '2020-09-30',
    constants.LOG_INTERVAL_KEY: tests_constants.CURR_LOG_INTERVAL.to_meta(),
}


DATA = {
    'tariffed/2020-10-11T11:00:00': {
        'common_data_part': {
            "EventDate": ts("2020-10-11"), "DSPID": 1, "PageID": 1, "ImpID": 1, "OutputProductID": 1, "ContractID": 1, "ClientID": 1, "OutputStatus": "tariffed",
        },
        'data': [
            {"LBMessageUID": "0", "OutputStatus": "dropped_untarffed", "BillableAmount": -0.5, "RUBTurnoverWithoutNDS": -0.6, "RUBRewardWithoutNDS": -0.5, "BillableEventType": "undo-block-show"},
            {"LBMessageUID": "1", "BillableAmount": 0.5, "RUBTurnoverWithoutNDS": 0.6, "RUBRewardWithoutNDS": 0.5, "BillableEventType": "block-show"},
            {"LBMessageUID": "2", "BillableAmount": -0.5, "RUBTurnoverWithoutNDS": -0.6, "RUBRewardWithoutNDS": -0.5, "BillableEventType": "undo-block-show"},
            {"LBMessageUID": "3", "BillableAmount": 0.4, "RUBTurnoverWithoutNDS": 0.5, "RUBRewardWithoutNDS": 0.4, "BillableEventType": "block-show"},
            {"LBMessageUID": "4", "EventDate": ts("2020-09-11"), "BillableAmount": 0.1, "RUBTurnoverWithoutNDS": 0.2, "RUBRewardWithoutNDS": 0.1, "BillableEventType": "block-show"},
            {"LBMessageUID": "5", "EventDate": ts("2020-11-11"), "BillableAmount": 0.1, "RUBTurnoverWithoutNDS": 0.2, "RUBRewardWithoutNDS": 0.1, "BillableEventType": "block-show"},
        ],
        'attributes': {
            'schema': tests_constants.BILLED_PARTNER_LOG_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: TARIFFED_META,
        },
    },
    'unacted_events/2020-09-30': {
        'common_data_part': {
            "EventDate": ts("2020-10-11"), "DSPID": 2, "PageID": 2, "ImpID": 2, "OutputProductID": 2, "ContractID": 2, "ClientID": 2, "OutputStatus": "tariffed",
        },
        'data': [
            {"LBMessageUID": "6", "BillableAmount": 0.5, "RUBTurnoverWithoutNDS": 0.6, "RUBRewardWithoutNDS": 0.5, "BillableEventType": "click"},
            {"LBMessageUID": "7", "BillableAmount": -0.5, "RUBTurnoverWithoutNDS": -0.6, "RUBRewardWithoutNDS": -0.5, "BillableEventType": "undo-click"},
            {"LBMessageUID": "8", "BillableAmount": 0.4, "RUBTurnoverWithoutNDS": 0.5, "RUBRewardWithoutNDS": 0.4, "BillableEventType": "click"},
        ],
        'attributes': {
            'schema': tests_constants.BILLED_PARTNER_LOG_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: PREV_ACT_META,
        },
    },
    'retariffed_events/2020-10-31': {
        'common_data_part': {
            "EventDate": ts("2020-10-11"), "DSPID": 2, "PageID": 2, "ImpID": 2, "OutputProductID": 2, "ContractID": 2, "ClientID": 2, "_RollbackPublished": 0
        },
        'data': [
            {"OutputStatus": "tariffed-rollback", "LBMessageUID": "8", "BillableAmount": -0.4, "RUBTurnoverWithoutNDS": -0.5, "RUBRewardWithoutNDS": -0.4, "BillableEventType": "click"},
            {"OutputStatus": "tariffed-uptariffed", "LBMessageUID": "8", "BillableAmount": 0.3, "RUBTurnoverWithoutNDS": 0.4, "RUBRewardWithoutNDS": 0.3, "BillableEventType": "click"},
        ],
        'attributes': {
            'schema': tests_constants.RETARRIFED_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: CURR_ACT_META,
        },
    },
}


@pytest.fixture()
def work_dir_tree(yt_client, yt_root):
    work_dir_path = yt.ypath_join(yt_root, 'work_dir')
    yt_client.create('map_node', work_dir_path)
    work_tree = {
        'tariffed_dir': yt.ypath_join(work_dir_path, 'tariffed'),
        'unacted_events_dir': yt.ypath_join(work_dir_path, 'unacted_events'),
        'dropped_events_dir': yt.ypath_join(work_dir_path, 'dropped_events'),
        'prepared_rows_dir': yt.ypath_join(work_dir_path, 'prepared_rows'),
        'reverse_events_index_dir': yt.ypath_join(work_dir_path, 'reverse_events_index'),
        'retariffed_events_dir': yt.ypath_join(work_dir_path, 'retariffed_events'),
    }
    for work_leaf in work_tree.values():
        yt_client.create('map_node', work_leaf)
    for table_name, info in DATA.items():
        table_path = yt.ypath_join(work_dir_path, table_name)
        conftest_partner.create_table(yt_client, table_path, info)
        work_tree[table_name] = table_path
    yield work_tree
    yt_client.remove(work_dir_path, force=True, recursive=True)


def test_bs_outlay_group_rows(yt_client, yql_client, work_dir_tree):
    with yt_client.Transaction() as transaction:
        (
            output_prepared_rows_path,
            output_dropped_events_path,
            next_unacted_events_path,
            output_reverse_events_index_path,
        ) = bs_outlay_group_rows.run_job(
            yt_client, yql_client,
            current_meta=CURR_ACT_META,
            tariffed_dir=work_dir_tree['tariffed_dir'],
            retariffed_events_path=work_dir_tree['retariffed_events/2020-10-31'],
            unacted_events_dir=work_dir_tree['unacted_events_dir'],
            dropped_events_dir=work_dir_tree['dropped_events_dir'],
            prepared_rows_dir=work_dir_tree['prepared_rows_dir'],
            reverse_events_index_dir=work_dir_tree['reverse_events_index_dir'],
            transaction=transaction,
        )
    def by_field(name):
        return lambda item: item[name]
    return {
        'output_prepared_rows': list(sorted(
            yt_client.read_table(output_prepared_rows_path, format="json"), key=by_field('ActRowID')
        )),
        'output_dropped_events': list(sorted(
            yt_client.read_table(output_dropped_events_path, format="json"), key=by_field('LBMessageUID')
        )),
        'next_unacted_events': list(sorted(
            yt_client.read_table(next_unacted_events_path, format="json"), key=by_field('LBMessageUID')
        )),
        'output_reverse_events_index': list(sorted(
            yt_client.read_table(output_reverse_events_index_path, format="json"), key=by_field('LBMessageUID')
        )),
    }
