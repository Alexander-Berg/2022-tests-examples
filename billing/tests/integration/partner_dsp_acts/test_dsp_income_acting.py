import pytest

import yt.wrapper as yt

from billing.log_tariffication.py.lib import constants
from billing.log_tariffication.py.jobs.partner_acts import dsp_income_acting
from billing.log_tariffication.py.tests import constants as tests_constants
from billing.log_tariffication.py.tests.integration import conftest_partner

YT_ACTING_EVENTS_DT = '2002-04-01T00:00:00'  # TZ=Europe/Moscow

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
            "act_sum": 6.0,
            "act_sum_wo_nds": 6.0,
            "currency": "RUB",
            "events_sum": 5.0,
            "events_currency": "RUB",
            "tariffer_service_id": 80,
            "events_month_dt": "2020-09-01T00:00:00+0300",
            "act_effective_tax_policy_pct_mdh_id": "91f77dca-81d8-4f6e-b3d1-04fa78ce6b50",
            "tax_policy_mdh_id": "93fb537f-e424-49b6-8b3a-b735f9164708",
            "act_effective_nds_pct": 20.0,
        },
        'data': [
            {
                "act_row_id": "1",
                "client_id": 1,
                "contract_id": 1,
                "mdh_product_id": "1"
            },
            {
                "act_row_id": "2",
                "client_id": 2,
                "contract_id": 2,
                "mdh_product_id": "2"
            },
            {
                "act_row_id": "5",
                "client_id": 1,
                "contract_id": 3,
                "mdh_product_id": "1"
            },
            {
                "act_row_id": "6",
                "client_id": 3,
                "contract_id": 4,
                "mdh_product_id": "3"
            },
            {  # отфильтруется, так как и месяц событий, и дата начала действия договора раньше даты YT_ACTING_EVENTS_DT
                "act_row_id": "666",
                "client_id": 3,
                "contract_id": 666,  # contract.dt < YT_ACTING_EVENTS_DT
                "mdh_product_id": "3",
                "events_month_dt": "2000-09-01T00:00:00+0300",  # < YT_ACTING_EVENTS_DT
            },
        ],
        'attributes': {
            'schema': tests_constants.DSP_ACT_ROWS_TABLE_SCHEMA,
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
                                '80': 1
                            },
                        },
                    },
                    'external_id': 'e-1',
                    'id': 1,
                    'type': 'GENERAL',
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
                                '80': 1
                            },
                        },
                    },
                    'external_id': 'e-2',
                    'id': 2,
                    'type': 'GENERAL',
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
                                '80': 1
                            },
                            'service_start_dt': '2020-01-01T00:00:00'
                        },
                    },
                    'external_id': 'e-1',
                    'id': 1,
                    'type': 'GENERAL',
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
                                '80': 1
                            },
                            'service_start_dt': '2020-01-01T00:00:00'
                        },
                    },
                    'external_id': 'e-4',
                    'id': 4,
                    'type': 'GENERAL',
                    'version_id': 1,
                    'passport_id': 666,
                    'person_id': 666,
                    'update_dt': '2020-09-17T00:35:00',
                },
            },
            {
                'ID': 666,
                'Object': {
                    'client_id': 3,
                    'collaterals': {
                        '0': {
                            'collateral_type_id': None,
                            'currency': 643,
                            'dt': '2000-01-27T00:00:00',
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
                                '80': 1
                            },
                            'service_start_dt': '2000-01-01T00:00:00'
                        },
                    },
                    'external_id': 'e-4',
                    'id': 4,
                    'type': 'GENERAL',
                    'version_id': 1,
                    'passport_id': 666,
                    'person_id': 666,
                    'update_dt': '2000-09-17T00:35:00',
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
            "act_sum": 3.0,
            "act_sum_wo_nds": 3.0,
            "currency": "RUB",
            "events_sum": 2.0,
            "events_currency": "RUB",
            "events_month_dt": "2020-08-01T00:00:00+0300",
            "tariffer_service_id": 80,
            "tax_policy_mdh_id": "93fb537f-e424-49b6-8b3a-b735f9164708",
            "act_effective_tax_policy_pct_mdh_id": "91f77dca-81d8-4f6e-b3d1-04fa78ce6b50",
            "act_effective_nds_pct": 20.0,
        },
        'data': [
            {
                "act_row_id": "3",
                "client_id": 1,
                "contract_id": 1,
                "mdh_product_id": "1"
            },
            {
                "act_row_id": "4",
                "client_id": 2,
                "contract_id": 2,
                "mdh_product_id": "2"
            },
        ],
        'attributes': {
            'schema': tests_constants.DSP_ACT_ROWS_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: PREV_ACT_META,
        },
    },
}

# USA/USD
DATA_2 = {
    'prepared_rows/2020-10-31': {
        'common_data_part': {
            "act_sum": 5.0,
            "act_sum_wo_nds": 5.0,
            "currency": "USD",
            "events_sum": 5.0,
            "events_currency": "USD",
            "tariffer_service_id": 80,
            "events_month_dt": "2020-09-01T00:00:00+0300",
            "act_effective_tax_policy_pct_mdh_id": "0afe9d15-202e-4498-af61-423d92ad5782",
            "tax_policy_mdh_id": "87561f18-5f5b-46d8-8c3d-a82d4a06ebab",
            "act_effective_nds_pct": 0.0,
        },
        'data': [
            {
                "act_row_id": "1",
                "client_id": 1,
                "contract_id": 1,
                "mdh_product_id": "1"
            },
            {
                "act_row_id": "2",
                "client_id": 2,
                "contract_id": 2,
                "mdh_product_id": "2"
            },
            {
                "act_row_id": "5",
                "client_id": 1,
                "contract_id": 3,
                "mdh_product_id": "1"
            },
            {
                "act_row_id": "6",
                "client_id": 3,
                "contract_id": 4,
                "mdh_product_id": "3"
            },
        ],
        'attributes': {
            'schema': tests_constants.DSP_ACT_ROWS_TABLE_SCHEMA,
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
                            'currency': 840,
                            'dt': '2020-01-01T00:00:00',
                            'end_dt': None,
                            'firm': 4,
                            'is_signed': None,
                            'is_suspended': None,
                            'manager_code': 27649,
                            'nds': 0,
                            'partner_pct': 45,
                            'num': None,
                            'test_mode': 1,
                            'services': {
                                '80': 1
                            },
                        },
                    },
                    'external_id': 'e-1',
                    'id': 1,
                    'type': 'GENERAL',
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
                            'currency': 840,
                            'dt': '2020-01-01T00:00:00',
                            'end_dt': None,
                            'firm': 4,
                            'is_signed': '2020-01-01T15:35:00',
                            'is_suspended': None,
                            'manager_code': 27649,
                            'nds': 0,
                            'partner_pct': 45,
                            'num': None,
                            'test_mode': 0,
                            'services': {
                                '80': 1
                            },
                        },
                    },
                    'external_id': 'e-2',
                    'id': 2,
                    'type': 'GENERAL',
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
                            'currency': 840,
                            'dt': '2020-01-27T00:00:00',
                            'end_dt': None,
                            'firm': 4,
                            'is_signed': '2020-02-01T00:00:00',
                            'is_suspended': None,
                            'manager_code': 27649,
                            'nds': 0,
                            'partner_pct': 45,
                            'num': None,
                            'test_mode': 1,
                            'services': {
                                '80': 1
                            },
                            'service_start_dt': '2020-01-01T00:00:00'
                        },
                    },
                    'external_id': 'e-1',
                    'id': 1,
                    'type': 'GENERAL',
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
                            'currency': 840,
                            'dt': '2020-01-27T00:00:00',
                            'end_dt': None,
                            'firm': 4,
                            'is_cancelled': '2020-09-17T00:00:00',
                            'is_suspended': None,
                            'manager_code': 27649,
                            'nds': 0,
                            'partner_pct': 45,
                            'num': None,
                            'test_mode': None,
                            'services': {
                                '80': 1
                            },
                            'service_start_dt': '2020-01-01T00:00:00'
                        },
                    },
                    'external_id': 'e-4',
                    'id': 4,
                    'type': 'GENERAL',
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
            "act_sum": 2.0,
            "act_sum_wo_nds": 2.0,
            "currency": "USD",
            "events_sum": 2.0,
            "events_currency": "USD",
            "events_month_dt": "2020-08-01T00:00:00+0300",
            "tariffer_service_id": 80,
            "tax_policy_mdh_id": "87561f18-5f5b-46d8-8c3d-a82d4a06ebab",
            "act_effective_tax_policy_pct_mdh_id": "0afe9d15-202e-4498-af61-423d92ad5782",
            "act_effective_nds_pct": 0.0,
        },
        'data': [
            {
                "act_row_id": "3",
                "client_id": 1,
                "contract_id": 1,
                "mdh_product_id": "1"
            },
            {
                "act_row_id": "4",
                "client_id": 2,
                "contract_id": 2,
                "mdh_product_id": "2"
            },
        ],
        'attributes': {
            'schema': tests_constants.DSP_ACT_ROWS_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: PREV_ACT_META,
        },
    },
}


@pytest.mark.parametrize('data', [DATA, DATA_2], ids=['firm_1/RUB', 'firm_4/USD'])
def test_dsp_income_acting(yt_client, yt_root, yql_client, udf_server_file_url, data):
    work_dir_path = yt.ypath_join(yt_root, 'work_dir')
    yt_client.create('map_node', work_dir_path)

    work_dir_tree = {
        'acted_rows_dir': yt.ypath_join(work_dir_path, 'acted_rows'),
        'unacted_rows_dir': yt.ypath_join(work_dir_path, 'unacted_rows'),
    }

    for work_leaf in work_dir_tree.values():
        yt_client.create('map_node', work_leaf)

    for table_name, info in data.items():
        table_path = yt.ypath_join(work_dir_path, table_name)
        conftest_partner.create_table(yt_client, table_path, info)
        work_dir_tree[table_name] = table_path

    with yt_client.Transaction() as transaction:
        output_acted_rows_path, next_unacted_rows_path = dsp_income_acting.run_job(
            yt_client, yql_client,
            current_meta=CURR_ACT_META,
            acted_rows_dir=work_dir_tree['acted_rows_dir'],
            prepared_rows_path=work_dir_tree['prepared_rows/2020-10-31'],
            unacted_rows_dir=work_dir_tree['unacted_rows_dir'],
            ref_contracts_path=work_dir_tree['ref_contracts/2020-10-31'],
            udf_file_url=udf_server_file_url,
            transaction=transaction,
            yt_acting_events_dt=YT_ACTING_EVENTS_DT
        )

    def by_field(name):
        return lambda item: item[name]

    result = {
        'output_acted_rows_path': list(sorted(
            yt_client.read_table(output_acted_rows_path, format="json"), key=by_field('act_row_id')
        )),
        'next_unacted_rows_path': list(sorted(
            yt_client.read_table(next_unacted_rows_path, format="json"), key=by_field('act_row_id')
        ))
    }

    yt_client.remove(work_dir_path, force=True, recursive=True)
    return result
