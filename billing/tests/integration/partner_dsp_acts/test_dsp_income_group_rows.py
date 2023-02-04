import arrow
import pytest

import yt.wrapper as yt

from billing.log_tariffication.py.lib import constants
from billing.log_tariffication.py.jobs.partner_acts import dsp_income_group_rows
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
            "EventDate": ts("2020-10-11"), "ContractID": 1, "ClientID": 1, "OutputStatus": "tariffed",
            "ProductID": 2, "ProductMdhID": "2",
            "BillableAmountCurrency": "RUB", "AmountCurrency": "RUB",
            "CurrencyRate": 1.0, "CurrencyRateDate": ts("2020-09-11"),
            "TaxPolicyPercentID": 281,
            "TaxPolicyPercentMdhID": "91f77dca-81d8-4f6e-b3d1-04fa78ce6b50",
            "TaxPolicyMdhID": "93fb537f-e424-49b6-8b3a-b735f9164708",
            "TaxPolicyID": 1,
        },
        'data': [
            {"LBMessageUID": "1", "BillableAmount": 0.4, "Amount": 0.5, "AmountWithoutNDS": 0.5},
            {"LBMessageUID": "2", "BillableAmount": -0.4, "Amount": -0.5, "AmountWithoutNDS": -0.5},
            {"LBMessageUID": "3", "BillableAmount": 0.3, "Amount": 0.4, "AmountWithoutNDS": 0.4},
            {"LBMessageUID": "4", "EventDate": ts("2020-09-11"), "BillableAmount": 0.01,
             "Amount": 0.1, "AmountWithoutNDS": 0.1},
            {"LBMessageUID": "5", "EventDate": ts("2020-11-11"), "BillableAmount": 0.01,
             "Amount": 0.1, "AmountWithoutNDS": 0.1},
        ],
        'attributes': {
            'schema': tests_constants.BILLED_DSP_LOG_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: TARIFFED_META,
        },
    },
    'unacted_events/2020-09-30': {
        'common_data_part': {
            "EventDate": ts("2020-10-11"),
            "ContractID": 2,
            "ClientID": 2,
            "ProductID": 2,
            "ProductMdhID": "2",
            "BillableAmountCurrency": "RUB",
            "AmountCurrency": "RUB",
            "CurrencyRate": 1.0,
            "CurrencyRateDate": ts("2020-10-11"),
            "TaxPolicyPercentID": 281,
            "TaxPolicyPercentMdhID": "91f77dca-81d8-4f6e-b3d1-04fa78ce6b50",
            "TaxPolicyMdhID": "93fb537f-e424-49b6-8b3a-b735f9164708",
            "TaxPolicyID": 1,
            "OutputStatus": "tariffed"
        },
        'data': [
            {"LBMessageUID": "6", "BillableAmount": 0.4, "Amount": 0.5, "AmountWithoutNDS": 0.5},
            {"LBMessageUID": "7", "BillableAmount": -0.4, "Amount": -0.5, "AmountWithoutNDS": -0.5},
            {"LBMessageUID": "8", "BillableAmount": 0.3, "Amount": 0.4, "AmountWithoutNDS": 0.4},
        ],
        'attributes': {
            'schema': tests_constants.BILLED_DSP_LOG_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: PREV_ACT_META,
        },
    },
    'ref_firm_info/2020-10-31': {
        'common_data_part': {
        },
        'data': [
            {
                'id': 1,
                'person_categories': [
                    {
                        "category": "ur",
                        "is_legal": 1,
                        "is_resident": 1
                    }
                ],
                "title": "OOO Yandex",
                "mdh_id": "666-666",
                'tax_policies': [
                    {
                        "default_tax": 1,
                        "hidden": 0,
                        "id": 1,
                        "mdh_id": "93fb537f-e424-49b6-8b3a-b735f9164708",
                        "name": "Россия, резидент, НДС облагается",
                        "percents": [
                            {
                                "dt": "2019-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 281,
                                "mdh_id": "91f77dca-81d8-4f6e-b3d1-04fa78ce6b50",
                                "nds_pct": 20,
                                "nsp_pct": 0
                            },
                            {
                                "dt": "2003-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 2,
                                "mdh_id": "9feab74e-4252-4507-a572-124304d9b9eb",
                                "nds_pct": 20,
                                "nsp_pct": 5
                            },
                            {
                                "dt": "2004-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 1,
                                "mdh_id": "cd174c94-b588-451a-81df-78d765bd258f",
                                "nds_pct": 18,
                                "nsp_pct": 0
                            }
                        ],
                        "region_id": 225,
                        "resident": 1,
                        "spendable_nds_id": 18
                    },
                    {
                        "default_tax": 0,
                        "hidden": 1,
                        "id": 203,
                        "mdh_id": "418f42d1-552f-411d-a110-c851d961c19b",
                        "name": "Без НДС (осв.) - медицина",
                        "percents": [
                            {
                                "dt": "2019-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 343,
                                "mdh_id": "855c453c-7bf6-48b7-9b80-94b043198cf3",
                                "nds_pct": 0,
                                "nsp_pct": 0
                            }
                        ],
                        "region_id": 225,
                        "resident": 1
                    },
                    {
                        "default_tax": 0,
                        "hidden": 1,
                        "id": 12,
                        "mdh_id": "10cad9b2-4e02-4502-a440-3c002b2caf87",
                        "name": "Резидент РФ для Авто.ру",
                        "percents": [
                            {
                                "dt": "2015-01-01T00:00:00+03:00",
                                "hidden": 1,
                                "id": 101,
                                "mdh_id": "18311966-8888-47af-93cb-aef87f0234e7",
                                "nds_pct": 18,
                                "nsp_pct": 0
                            }
                        ],
                        "region_id": 225,
                        "resident": 1
                    },
                    {
                        "default_tax": 0,
                        "hidden": 0,
                        "id": 11,
                        "mdh_id": "67d9783c-9d14-4ae9-b526-8e8f5186d2b4",
                        "name": "Россия, резидент, НДС не облагается",
                        "percents": [
                            {
                                "dt": "2004-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 81,
                                "mdh_id": "6696ede3-7505-42a2-b4aa-559c4d57886b",
                                "nds_pct": 0,
                                "nsp_pct": 0
                            }
                        ],
                        "region_id": 225,
                        "resident": 1
                    },
                    {
                        "default_tax": 0,
                        "hidden": 0,
                        "id": 10,
                        "mdh_id": "201d0a70-430a-4606-b826-6c85ccc98740",
                        "name": "Россия, нерезидент, НДС облагается",
                        "percents": [
                            {
                                "dt": "2019-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 301,
                                "mdh_id": "02af481c-7bec-43ac-b90b-1f6a1f544e1c",
                                "nds_pct": 20,
                                "nsp_pct": 0
                            },
                            {
                                "dt": "2004-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 61,
                                "mdh_id": "a281beab-0d17-49a1-88e7-90256c769557",
                                "nds_pct": 18,
                                "nsp_pct": 0
                            }
                        ],
                        "region_id": 225,
                        "resident": 0
                    },
                    {
                        "default_tax": 1,
                        "hidden": 0,
                        "id": 2,
                        "mdh_id": "799ed901-ab97-47e9-8120-d07d78c146fc",
                        "name": "Россия, нерезидент, НДС не облагается",
                        "percents": [
                            {
                                "dt": "2004-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 3,
                                "mdh_id": "cfd45ffb-c654-4d98-8fc1-355ca295d4b8",
                                "nds_pct": 0,
                                "nsp_pct": 0
                            },
                            {
                                "dt": "2003-01-01T00:00:00+03:00",
                                "hidden": 0,
                                "id": 4,
                                "mdh_id": "7afb2d22-bbc2-4592-b033-8861b90e3cf3",
                                "nds_pct": 0,
                                "nsp_pct": 5
                            }
                        ],
                        "region_id": 225,
                        "resident": 0,
                        "spendable_nds_id": 0
                    }
                ]
            }
        ],
        'attributes': {
            'schema': tests_constants.REFERENCE_FIRM_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: CURR_ACT_META,
        }
    },
}


@pytest.fixture()
def work_dir_tree(yt_client, yt_root):
    work_dir_path = yt.ypath_join(yt_root, 'work_dir')
    yt_client.create('map_node', work_dir_path)
    work_tree = {
        'tariffed_dir': yt.ypath_join(work_dir_path, 'tariffed'),
        'unacted_events_dir': yt.ypath_join(work_dir_path, 'unacted_events'),
        'prepared_rows_dir': yt.ypath_join(work_dir_path, 'prepared_rows'),
        'reverse_events_index_dir': yt.ypath_join(work_dir_path, 'reverse_events_index'),
    }
    for work_leaf in work_tree.values():
        yt_client.create('map_node', work_leaf)
    for table_name, info in DATA.items():
        table_path = yt.ypath_join(work_dir_path, table_name)
        conftest_partner.create_table(yt_client, table_path, info)
        work_tree[table_name] = table_path
    yield work_tree
    yt_client.remove(work_dir_path, force=True, recursive=True)


def test_dsp_income_group_rows(yt_client, yql_client, work_dir_tree):
    with yt_client.Transaction() as transaction:
        (
            output_prepared_rows_path,
            next_unacted_events_path,
            output_reverse_events_index_path,
        ) = dsp_income_group_rows.run_job(
            yt_client, yql_client,
            current_meta=CURR_ACT_META,
            tariffed_dir=work_dir_tree['tariffed_dir'],
            unacted_events_dir=work_dir_tree['unacted_events_dir'],
            ref_firm_info_path=work_dir_tree['ref_firm_info/2020-10-31'],
            prepared_rows_dir=work_dir_tree['prepared_rows_dir'],
            reverse_events_index_dir=work_dir_tree['reverse_events_index_dir'],
            transaction=transaction,
        )

    def by_field(name):
        return lambda item: item[name]

    return {
        'output_prepared_rows': list(sorted(
            yt_client.read_table(output_prepared_rows_path, format="json"), key=by_field('act_row_id')
        )),
        'next_unacted_events': list(sorted(
            yt_client.read_table(next_unacted_events_path, format="json"), key=by_field('LBMessageUID')
        )),
        'output_reverse_events_index': list(sorted(
            yt_client.read_table(output_reverse_events_index_path, format="json"), key=by_field('LBMessageUID')
        )),
    }
