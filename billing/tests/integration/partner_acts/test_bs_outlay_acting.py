import pytest

import yt.wrapper as yt

from billing.log_tariffication.py.lib import constants
from billing.log_tariffication.py.jobs.partner_acts import bs_outlay_acting
from billing.log_tariffication.py.tests import constants as tests_constants
from billing.log_tariffication.py.tests.integration import conftest_partner


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
    'prepared_rows/2020-10-31': {
        'common_data_part': {
            "BillableAmount": 6.0,
            "DT": "2020-09-01T00:00:00+0300",
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 6.0,
            "RUBTurnoverWithoutNDS": 7.0,
        },
        'data': [
            {
                "ActRowID": "1",
                "BlockID": 1,
                "ClientID": 1,
                "ContractID": 1,
                "DSPID": 1,
                "PageID": 1,
                "ProductID": 1,
                "Clicks": 0,
                "Shows": 1
            },
            {
                "ActRowID": "2",
                "BlockID": 2,
                "ClientID": 2,
                "ContractID": 2,
                "DSPID": 2,
                "PageID": 2,
                "ProductID": 2,
                "Clicks": 1,
                "Shows": 0
            },
            {
                "ActRowID": "5",
                "BlockID": 1,
                "ClientID": 1,
                "ContractID": 3,
                "DSPID": 1,
                "PageID": 1,
                "ProductID": 1,
                "Clicks": 0,
                "Shows": 1
            },
            {
                "ActRowID": "6",
                "BlockID": 3,
                "ClientID": 3,
                "ContractID": 4,
                "DSPID": 3,
                "PageID": 3,
                "ProductID": 3,
                "Clicks": 0,
                "Shows": 1
            },
        ],
        'attributes': {
            'schema': tests_constants.PARTNER_ACT_ROWS_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: CURR_ACT_META,
        },
    },
    'ref_contracts/2020-10-31': {
        'common_data_part': {
        },
        'data': [
            {
                'ID': 1,
                'Object': {
                    'client_id': 1,
                    'collaterals': {
                        '0': {
                            'collateral_type_id': None,
                            'currency': 643,
                            'dt': '2020-01-01T00:00:00',
                            'end_dt': None,
                            'firm': 1,
                            'is_signed': None,
                            'is_suspended': None,
                            'manager_code': 27649,
                            'nds': 18,
                            'partner_pct': 45,
                            'num': None,
                            'test_mode': 1,
                            'services': {
                                '11': 1
                            },
                        },
                    },
                    'external_id': 'e-1',
                    'id': 1,
                    'type': 'PARTNERS',
                    'version_id': 1,
                    'passport_id': 666,
                    'person_id': 666,
                    'update_dt': '2020-01-01T15:35:00',
                },
            },
            {
                'ID': 2,
                'Object': {
                    'client_id': 2,
                    'collaterals': {
                        '0': {
                            'collateral_type_id': None,
                            'currency': 643,
                            'dt': '2020-01-01T00:00:00',
                            'end_dt': None,
                            'firm': 1,
                            'is_signed': '2020-01-01T15:35:00',
                            'is_suspended': None,
                            'manager_code': 27649,
                            'nds': 18,
                            'partner_pct': 45,
                            'num': None,
                            'test_mode': 0,
                            'services': {
                                '11': 1
                            },
                        },
                    },
                    'external_id': 'e-2',
                    'id': 2,
                    'type': 'PARTNERS',
                    'version_id': 1,
                    'passport_id': 666,
                    'person_id': 666,
                    'update_dt': '2020-01-01T15:35:00',
                },
            },
            {
                'ID': 3,
                'Object': {
                    'client_id': 1,
                    'collaterals': {
                        '0': {
                            'collateral_type_id': None,
                            'currency': 643,
                            'dt': '2020-01-27T00:00:00',
                            'end_dt': None,
                            'firm': 1,
                            'is_signed': '2020-02-01T00:00:00',
                            'is_suspended': None,
                            'manager_code': 27649,
                            'nds': 18,
                            'partner_pct': 45,
                            'num': None,
                            'test_mode': 1,
                            'services': {
                                '11': 1
                            },
                            'service_start_dt': '2020-01-01T00:00:00'
                        },
                    },
                    'external_id': 'e-1',
                    'id': 1,
                    'type': 'PARTNERS',
                    'version_id': 1,
                    'passport_id': 666,
                    'person_id': 666,
                    'update_dt': '2020-01-01T15:35:00',
                },
            },
            {
                'ID': 4,
                'Object': {
                    'client_id': 3,
                    'collaterals': {
                        '0': {
                            'collateral_type_id': None,
                            'currency': 643,
                            'dt': '2020-01-27T00:00:00',
                            'end_dt': None,
                            'firm': 1,
                            'is_cancelled': '2020-09-17T00:00:00',
                            'is_suspended': None,
                            'manager_code': 27649,
                            'nds': 18,
                            'partner_pct': 45,
                            'num': None,
                            'test_mode': None,
                            'services': {
                                '11': 1
                            },
                            'service_start_dt': '2020-01-01T00:00:00'
                        },
                    },
                    'external_id': 'e-4',
                    'id': 4,
                    'type': 'PARTNERS',
                    'version_id': 1,
                    'passport_id': 666,
                    'person_id': 666,
                    'update_dt': '2020-09-17T00:35:00',
                },
            },
        ],
        'attributes': {
            'schema': tests_constants.REFERENCE_SNAPSHOT_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: CURR_ACT_META,
        },
    },
    'unacted_rows/2020-09-30': {
        'common_data_part': {
            "BillableAmount": 3.0,
            "DT": "2020-08-01T00:00:00+0300",
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 3.0,
            "RUBTurnoverWithoutNDS": 5.0,
        },
        'data': [
            {
                "ActRowID": "3",
                "BlockID": 1,
                "ClientID": 1,
                "ContractID": 1,
                "DSPID": 1,
                "PageID": 1,
                "ProductID": 1,
                "Clicks": 0,
                "Shows": 7
            },
            {
                "ActRowID": "4",
                "BlockID": 2,
                "ClientID": 2,
                "ContractID": 2,
                "DSPID": 2,
                "PageID": 2,
                "ProductID": 2,
                "Clicks": 7,
                "Shows": 0
            },
        ],
        'attributes': {
            'schema': tests_constants.PARTNER_ACT_ROWS_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: PREV_ACT_META,
        },
    },
}


@pytest.fixture()
def work_dir_tree(yt_client, yt_root):
    work_dir_path = yt.ypath_join(yt_root, 'work_dir')
    yt_client.create('map_node', work_dir_path)
    work_tree = {
        'acted_rows_dir': yt.ypath_join(work_dir_path, 'acted_rows'),
        'unacted_rows_dir': yt.ypath_join(work_dir_path, 'unacted_rows'),
        'dropped_rows_dir': yt.ypath_join(work_dir_path, 'dropped_rows'),
    }
    for work_leaf in work_tree.values():
        yt_client.create('map_node', work_leaf)
    for table_name, info in DATA.items():
        table_path = yt.ypath_join(work_dir_path, table_name)
        conftest_partner.create_table(yt_client, table_path, info)
        work_tree[table_name] = table_path
    yield work_tree
    yt_client.remove(work_dir_path, force=True, recursive=True)


def test_bs_outlay_acting(yt_client, yql_client, udf_server_file_url, work_dir_tree):
    with yt_client.Transaction() as transaction:
        output_acted_rows_path, next_unacted_rows_path, output_dropped_rows_path = bs_outlay_acting.run_job(
            yt_client, yql_client,
            current_meta=CURR_ACT_META,
            acted_rows_dir=work_dir_tree['acted_rows_dir'],
            dropped_rows_dir=work_dir_tree['dropped_rows_dir'],
            prepared_rows_path=work_dir_tree['prepared_rows/2020-10-31'],
            unacted_rows_dir=work_dir_tree['unacted_rows_dir'],
            ref_contracts_path=work_dir_tree['ref_contracts/2020-10-31'],
            udf_file_url=udf_server_file_url,
            transaction=transaction,
        )
    def by_field(name):
        return lambda item: item[name]
    return {
        'output_acted_rows_path': list(sorted(
            yt_client.read_table(output_acted_rows_path, format="json"), key=by_field('ActRowID')
        )),
        'next_unacted_rows_path': list(sorted(
            yt_client.read_table(next_unacted_rows_path, format="json"), key=by_field('ActRowID')
        )),
        'output_dropped_rows_path': list(sorted(
            yt_client.read_table(output_dropped_rows_path, format="json"), key=by_field('ActRowID')
        )),
    }
