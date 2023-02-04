from datetime import datetime
import json

import pytest

import yt.wrapper as yt

from billing.log_tariffication.py.lib import constants
from billing.log_tariffication.py.jobs.partner_acts import bs_outlay_retariffication
from billing.log_tariffication.py.tests import constants as tests_constants
from billing.log_tariffication.py.tests.integration import conftest_partner
from billing.log_tariffication.py.tests.integration.conftest_partner import to_ts


TARIFFED_META = {
    constants.RUN_ID_KEY: '2020-01-11T11:00:00',
    constants.LOG_INTERVAL_KEY: tests_constants.CURR_LOG_INTERVAL.to_meta(),
}
PREV_ACT_META = {
    constants.RUN_ID_KEY: '2019-12-31',
    constants.ACT_DT_KEY: '2019-12-31',
    constants.LOG_INTERVAL_KEY: tests_constants.PREV_LOG_INTERVAL.to_meta(),
}
CURR_ACT_META = {
    constants.RUN_ID_KEY: '2020-01-31',
    constants.ACT_DT_KEY: '2020-01-31',
    constants.PREVIOUS_RUN_ID_KEY: '2019-12-31',
    constants.LOG_INTERVAL_KEY: tests_constants.CURR_LOG_INTERVAL.to_meta(),
}


"""
Tariffed rows is copied from billing/log_tariffication/py/tests/integration/partner_tariff/canondata/result.json
and mixed up with source data from billing/log_tariffication/py/tests/integration/partner_tariff/test_bs_outlay.py
"""


def generate_yql_row_spec(table_schema):
    _yql_row_spec_fields = []
    for c in table_schema:
        if c['name'] == 'ContractObject':
            _yql_row_spec_fields.append([c['name'], ['OptionalType', ['DataType', 'Json']]])
        else:
            _yql_row_spec_fields.append([c['name'], ['OptionalType', ['DataType', c['type'].capitalize()]]])

    _yql_row_spec = {
        'StrictSchema': True,
        'Type': [
            'StructType',
            _yql_row_spec_fields,
        ],
    }
    return _yql_row_spec


DATA = {
    'tariffed/2020-10-11T11:00:00': {
        'common_data_part': {
        },
        'data': [
            {
                'TypeID': 0, 'PageID': 200, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
                'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
                'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0,
                "BillableAmount": 1.2,
                "ClientID": 2,
                "ContractID": 2,
                "ContractObject": json.dumps({
                    "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.4"
                }),
                "LBMessageUID": "_collateral_pct_change_7",
                "OutputProductID": 542,
                "OutputStatus": "tariffed",
                "OwnerID": 2,
                "RUBAggregatorRewardWithoutNDS": None,
                "RUBRewardWithoutNDS": 13.5,
                "RUBTurnoverWithoutNDS": 30.0,
                "_BillingTS": 10000000000
            },
            {
                'TypeID': 1, 'PageID': 200, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
                'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
                'EventDate': to_ts(datetime(2020, 1, 2)), 'EventTime': to_ts(datetime(2020, 1, 2, 0, 0, 1)),
                'UnixTime': to_ts(datetime(2020, 1, 2, 0, 0, 1)), 'PartnerStatID': 0,
                "BillableAmount": 1.2,
                "ClientID": 2,
                "ContractID": 2,
                "ContractObject": json.dumps({
                    "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.5"
                }),
                "LBMessageUID": "_collateral_pct_change_8",
                "OutputProductID": 2010,
                "OutputStatus": "tariffed",
                "OwnerID": 2,
                "RUBAggregatorRewardWithoutNDS": None,
                "RUBRewardWithoutNDS": 27.0,
                "RUBTurnoverWithoutNDS": 30.0,
                "_BillingTS": 10000000000
            },
            {
                'TypeID': 0, 'PageID': 400, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
                'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                'UnixTime': to_ts(datetime(2020, 1, 1, 10, 35, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                "BillableAmount": 1.2,
                "ClientID": 4,
                "ContractID": 41,
                "ContractObject": json.dumps({
                    "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.6"
                }),
                "LBMessageUID": "_contracts_dt_overlap_1",
                "OutputProductID": 100002,
                "OutputStatus": "tariffed",
                "OwnerID": 4,
                "RUBAggregatorRewardWithoutNDS": None,
                "RUBRewardWithoutNDS": 1.2,
                "RUBTurnoverWithoutNDS": 0.0,
                "_BillingTS": 10000000000
            },
            {
                'TypeID': 0, 'PageID': 21, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                'FakePrice': -1.2, 'YandexPrice': -1.2, 'PartnerCost': -1.2, 'PartnerPrice': -1.2,
                'EventCostVATCorrected': -1.2, 'Price': -1.2, 'BillableEventType': 'undo-block-show',
                'EventDate': to_ts(datetime(2020, 1, 31)), 'EventTime': to_ts(datetime(2020, 1, 31, 23, 59, 59)),
                'UnixTime': to_ts(datetime(2020, 2, 3, 0, 0, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                "BillableAmount": None,
                "ClientID": None,
                "ContractID": None,
                "ContractObject": None,
                "LBMessageUID": "_dropped_undo_6",
                "OutputProductID": 100002,
                "OutputStatus": "dropped_undo",
                "OwnerID": 1,
                "RUBAggregatorRewardWithoutNDS": None,
                "RUBRewardWithoutNDS": None,
                "RUBTurnoverWithoutNDS": None,
                "_BillingTS": 10000000000
            },
        ],
        'attributes': {
            'schema': tests_constants.BILLED_PARTNER_LOG_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: TARIFFED_META,
            '_yql_row_spec': generate_yql_row_spec(tests_constants.BILLED_PARTNER_LOG_TABLE_SCHEMA),
        },
    },
    'unacted_events/2019-12-31': {
        'common_data_part': {
        },
        'data': [
            {
                'TypeID': 0, 'PageID': 20, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
                'EventDate': to_ts(datetime(2020, 1, 4)), 'EventTime': to_ts(datetime(2020, 1, 4, 0, 0, 1)),
                'UnixTime': to_ts(datetime(2020, 1, 4, 0, 0, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                "LBMessageUID": "_completion_out_of_contract_bounds_3",
                "OutputStatus": "untariffed"
            },
        ],
        'attributes': {
            'schema': tests_constants.BILLED_PARTNER_LOG_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: PREV_ACT_META,
            '_yql_row_spec': generate_yql_row_spec(tests_constants.BILLED_PARTNER_LOG_TABLE_SCHEMA),
        },
    },
    'ref_contracts/2020-10-31': {
        'common_data_part': {
        },
        'data': [
            # _contracts_dt_overlap
            conftest_partner.gen_contract_ref_row(
                client_id=4, contract_id=40, dt=datetime(2019, 1, 1), end_dt=datetime(2020, 1, 2)
            ),
            conftest_partner.gen_contract_ref_row(
                client_id=4, contract_id=41, dt=datetime(2020, 1, 1), end_dt=datetime(2020, 1, 2), cancelled=True
                # was cancelled=False
            ),
            # _completion_out_of_contract_bounds
            conftest_partner.gen_contract_ref_row(
                client_id=1, contract_id=1, dt=datetime(2020, 1, 2), end_dt=datetime(2020, 1, 5)
                # was end_dt=datetime(2020, 1, 3)
            ),
            # _collateral_pct_change
            conftest_partner.gen_contract_ref_row(
                client_id=2, contract_id=2, dt=datetime(2020, 1, 1), end_dt=datetime(2020, 1, 3),
                collaterals=[{
                    'dt': '2020-01-01T00:00:00', 'partner_pct': '90', 'num': '1',
                    # was 'dt': '2020-01-02T00:00:00'
                    'is_signed': '2020-01-02T00:00:00', 'collateral_type_id': 2020
                }]
            ),
            # # _drop_outdated
            # conftest_partner.gen_contract_ref_row(
            #     client_id=19, contract_id=910, dt=datetime(2020, 1, 1), signed=True, agregator_pct='47.3', dsp_agregation_pct='10'
            # ),
        ],
        'attributes': {
            'schema': tests_constants.REFERENCE_SNAPSHOT_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: CURR_ACT_META,
        },
    },
    'ref_pages/2020-10-31': {
        'common_data_part': {
        },
        'data': [
            # _contracts_dt_overlap
            conftest_partner.gen_page_ref_row(page_id=400, client_id=4),
            # _completion_out_of_contract_bounds
            conftest_partner.gen_page_ref_row(page_id=20, client_id=1),
            # _collateral_pct_change
            conftest_partner.gen_page_ref_row(page_id=200, client_id=2),
        ],
        'attributes': {
            'schema': tests_constants.REFERENCE_SNAPSHOT_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: CURR_ACT_META,
        },
    },
    'ref_aggregator_pages/2020-10-31': {
        'common_data_part': {
        },
        'data': [
        ],
        'attributes': {
            'schema': tests_constants.REFERENCE_SNAPSHOT_TABLE_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: CURR_ACT_META,
        },
    },
    'ref_average_discounts/2020-10-31': {
        'common_data_part': {
        },
        'data': [
        ],
        'attributes': {
            'schema': tests_constants.REFERENCE_AVERAGE_DISCOUNTS_SCHEMA,
            constants.LOG_TARIFF_META_ATTR: CURR_ACT_META,
        },
    },
}

FIRST_REGENERATION_DATA = {
    'data': [
        {
            "BillableAmount": -1.2,
            "BillableEventType": "click",
            "ClientID": 2,
            "ContractID": 2,
            "ContractObject": json.dumps({
                "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.4"
            }),
            "CostCur": 1.2,
            "DSPID": None,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_collateral_pct_change_7",
            "OutputProductID": 542,
            "OutputStatus": "tariffed-rollback",
            "OwnerID": 2,
            "PageID": 200,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": 542,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": -13.5,
            "RUBTurnoverWithoutNDS": -30.0,
            "TypeID": 0,
            "UnixTime": 1577826001,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": 1.2,
            "BillableEventType": "click",
            "ClientID": 2,
            "ContractID": 2,
            # "ContractObject": {
            #     "uri": "file://test_bs_outlay_retariffication.test_bs_outlay_retariffication/extracted"
            # },
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None,
                 "bm_domains": None, "bm_market_pct": None, "bm_places": None, "client_id": 2,
                 "collateral_type": None, "contract_type": None, "currency": 643,
                 "doc_set": None, "domains": None, "dsp_agregation_pct": None,
                 "dt": "2020-01-01T00:00:00", "end_dt": "2020-01-03T00:00:00", "end_reason": None,
                 "external_id": "e-2", "firm": 1, "id": 2, "individual_docs": None, "is_archived": None,
                 "is_archived_dt": None, "is_booked": None, "is_booked_dt": None,
                 "is_cancelled": None, "is_faxed": None, "is_signed": "2020-01-01T00:00:00",
                 "manager_code": 27649, "market_api_pct": None, "market_banner": None, "memo": None,
                 "mkb_price": {}, "nds": 18, "num": None, "open_date": None, "partner_pct": "90", "passport_id": 666,
                 "pay_to": None, "payment_type": None, "person_id": 666, "print_tpl_barcode": None, "reward_type": 1,
                 "search_forms": None, "selfemployed": None, "sent_dt": None, "service_start_dt": None, "services": [],
                 "test_mode": None, "type": "PARTNERS", "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": None,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_collateral_pct_change_7",
            "OutputProductID": 542,
            "OutputStatus": "tariffed-uptariffed",
            "OwnerID": 2,
            "PageID": 200,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": 542,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 27.0,
            "RUBTurnoverWithoutNDS": 30.0,
            "TypeID": 0,
            "UnixTime": 1577826001,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": 1.2,
            "BillableEventType": "block-show",
            "ClientID": 1,
            "ContractID": 1,
            # "ContractObject": json.dumps({
            #     "uri": "file://test_bs_outlay_retariffication.test_bs_outlay_retariffication/extracted.0"
            # }),
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None, "bm_domains": None,
                 "bm_market_pct": None, "bm_places": None, "client_id": 1, "collateral_type": None,
                 "contract_type": None, "currency": 643, "doc_set": None, "domains": None,
                 "dsp_agregation_pct": None, "dt": "2020-01-02T00:00:00", "end_dt": "2020-01-05T00:00:00",
                 "end_reason": None, "external_id": "e-1", "firm": 1, "id": 1, "individual_docs": None,
                 "is_archived": None, "is_archived_dt": None, "is_booked": None, "is_booked_dt": None,
                 "is_cancelled": None, "is_faxed": None, "is_signed": "2020-01-02T00:00:00", "manager_code": 27649,
                 "market_api_pct": None, "market_banner": None, "memo": None, "mkb_price": {}, "nds": 18, "num": None,
                 "open_date": None, "partner_pct": "45", "passport_id": 666, "pay_to": None, "payment_type": None,
                 "person_id": 666, "print_tpl_barcode": None, "reward_type": 1, "search_forms": None,
                 "selfemployed": None, "sent_dt": None, "service_start_dt": None, "services": [],
                 "test_mode": None, "type": "PARTNERS", "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1578085200,
            "EventTime": 1578085201,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_completion_out_of_contract_bounds_3",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-uptariffed",
            "OwnerID": 1,
            "PageID": 20,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1578085201,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": -1.2,
            "BillableEventType": "block-show",
            "ClientID": 4,
            "ContractID": 41,
            "ContractObject": json.dumps({
                "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.6"
            }),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_contracts_dt_overlap_1",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-rollback",
            "OwnerID": 4,
            "PageID": 400,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": -1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1577864101,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": 1.2,
            "BillableEventType": "block-show",
            "ClientID": 4,
            "ContractID": 40,
            # "ContractObject": json.dumps({
            #     "uri": "file://test_bs_outlay_retariffication.test_bs_outlay_retariffication/extracted.1"
            # }),
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None, "bm_domains": None,
                 "bm_market_pct": None, "bm_places": None, "client_id": 4, "collateral_type": None,
                 "contract_type": None, "currency": 643, "doc_set": None, "domains": None, "dsp_agregation_pct": None,
                 "dt": "2019-01-01T00:00:00", "end_dt": "2020-01-02T00:00:00", "end_reason": None,
                 "external_id": "e-40", "firm": 1, "id": 40, "individual_docs": None, "is_archived": None,
                 "is_archived_dt": None, "is_booked": None, "is_booked_dt": None, "is_cancelled": None,
                 "is_faxed": None, "is_signed": "2019-01-01T00:00:00", "manager_code": 27649, "market_api_pct": None,
                 "market_banner": None, "memo": None, "mkb_price": {}, "nds": 18, "num": None, "open_date": None,
                 "partner_pct": "45", "passport_id": 666, "pay_to": None, "payment_type": None, "person_id": 666,
                 "print_tpl_barcode": None, "reward_type": 1, "search_forms": None, "selfemployed": None,
                 "sent_dt": None, "service_start_dt": None, "services": [], "test_mode": None, "type": "PARTNERS",
                 "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_contracts_dt_overlap_1",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-uptariffed",
            "OwnerID": 4,
            "PageID": 400,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1577864101,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": None,
            "BillableEventType": "undo-block-show",
            "ClientID": None,
            "ContractID": None,
            "ContractObject": None,
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": -1.2,
            "EventDate": 1580418000,
            "EventTime": 1580504399,
            "FakePrice": -1.2,
            "ImpID": None,
            "LBMessageUID": "_dropped_undo_6",
            "OutputProductID": 100002,
            "OutputStatus": "untariffed-uptariffed",
            "OwnerID": None,
            "PageID": 21,
            "PartnerCost": -1.2,
            "PartnerPrice": -1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": -1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": None,
            "RUBTurnoverWithoutNDS": None,
            "TypeID": 0,
            "UnixTime": 1580677201,
            "YandexPrice": -1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        }
    ],
    'attributes': {
        'schema': tests_constants.RETARRIFED_TABLE_SCHEMA,
        constants.LOG_TARIFF_META_ATTR: TARIFFED_META,
        '_yql_row_spec': generate_yql_row_spec(tests_constants.RETARRIFED_TABLE_SCHEMA),
    },
}

SECOND_REGENERATION_DATA = {
    'data': [
        {
            "BillableAmount": -1.2,
            "BillableEventType": "click",
            "ClientID": 2,
            "ContractID": 2,
            "ContractObject": json.dumps({
                "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.4"
            }),
            "CostCur": 1.2,
            "DSPID": None,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_collateral_pct_change_7",
            "OutputProductID": 542,
            "OutputStatus": "tariffed-rollback",
            "OwnerID": 2,
            "PageID": 200,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": 542,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": -13.5,
            "RUBTurnoverWithoutNDS": -30.0,
            "TypeID": 0,
            "UnixTime": 1577826001,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": 1.2,
            "BillableEventType": "click",
            "ClientID": 2,
            "ContractID": 2,
            "ContractObject": json.dumps({
                "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.4"
            }),
            "CostCur": 1.2,
            "DSPID": None,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_collateral_pct_change_7",
            "OutputProductID": 542,
            "OutputStatus": "tariffed-rollback-published_rollback",
            "OwnerID": 2,
            "PageID": 200,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": 542,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 13.5,
            "RUBTurnoverWithoutNDS": 30.0,
            "TypeID": 0,
            "UnixTime": 1577826001,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 1
        },
        {
            "BillableAmount": 1.2,
            "BillableEventType": "click",
            "ClientID": 2,
            "ContractID": 2,
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None,
                 "bm_domains": None, "bm_market_pct": None, "bm_places": None, "client_id": 2,
                 "collateral_type": None, "contract_type": None, "currency": 643,
                 "doc_set": None, "domains": None, "dsp_agregation_pct": None,
                 "dt": "2020-01-01T00:00:00", "end_dt": "2020-01-03T00:00:00", "end_reason": None,
                 "external_id": "e-2", "firm": 1, "id": 2, "individual_docs": None, "is_archived": None,
                 "is_archived_dt": None, "is_booked": None, "is_booked_dt": None,
                 "is_cancelled": None, "is_faxed": None, "is_signed": "2020-01-01T00:00:00",
                 "manager_code": 27649, "market_api_pct": None, "market_banner": None, "memo": None,
                 "mkb_price": {}, "nds": 18, "num": None, "open_date": None, "partner_pct": "90", "passport_id": 666,
                 "pay_to": None, "payment_type": None, "person_id": 666, "print_tpl_barcode": None, "reward_type": 1,
                 "search_forms": None, "selfemployed": None, "sent_dt": None, "service_start_dt": None, "services": [],
                 "test_mode": None, "type": "PARTNERS", "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": None,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_collateral_pct_change_7",
            "OutputProductID": 542,
            "OutputStatus": "tariffed-uptariffed",
            "OwnerID": 2,
            "PageID": 200,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": 542,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 27.0,
            "RUBTurnoverWithoutNDS": 30.0,
            "TypeID": 0,
            "UnixTime": 1577826001,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": -1.2,
            "BillableEventType": "click",
            "ClientID": 2,
            "ContractID": 2,
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None,
                 "bm_domains": None, "bm_market_pct": None, "bm_places": None, "client_id": 2,
                 "collateral_type": None, "contract_type": None, "currency": 643,
                 "doc_set": None, "domains": None, "dsp_agregation_pct": None,
                 "dt": "2020-01-01T00:00:00", "end_dt": "2020-01-03T00:00:00", "end_reason": None,
                 "external_id": "e-2", "firm": 1, "id": 2, "individual_docs": None, "is_archived": None,
                 "is_archived_dt": None, "is_booked": None, "is_booked_dt": None,
                 "is_cancelled": None, "is_faxed": None, "is_signed": "2020-01-01T00:00:00",
                 "manager_code": 27649, "market_api_pct": None, "market_banner": None, "memo": None,
                 "mkb_price": {}, "nds": 18, "num": None, "open_date": None, "partner_pct": "90", "passport_id": 666,
                 "pay_to": None, "payment_type": None, "person_id": 666, "print_tpl_barcode": None, "reward_type": 1,
                 "search_forms": None, "selfemployed": None, "sent_dt": None, "service_start_dt": None, "services": [],
                 "test_mode": None, "type": "PARTNERS", "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": None,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_collateral_pct_change_7",
            "OutputProductID": 542,
            "OutputStatus": "tariffed-uptariffed-published_rollback",
            "OwnerID": 2,
            "PageID": 200,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": 542,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": -27.0,
            "RUBTurnoverWithoutNDS": -30.0,
            "TypeID": 0,
            "UnixTime": 1577826001,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 1
        },
        {
            "BillableAmount": 1.2,
            "BillableEventType": "block-show",
            "ClientID": 1,
            "ContractID": 1,
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None, "bm_domains": None,
                 "bm_market_pct": None, "bm_places": None, "client_id": 1, "collateral_type": None,
                 "contract_type": None, "currency": 643, "doc_set": None, "domains": None,
                 "dsp_agregation_pct": None, "dt": "2020-01-02T00:00:00", "end_dt": "2020-01-05T00:00:00",
                 "end_reason": None, "external_id": "e-1", "firm": 1, "id": 1, "individual_docs": None,
                 "is_archived": None, "is_archived_dt": None, "is_booked": None, "is_booked_dt": None,
                 "is_cancelled": None, "is_faxed": None, "is_signed": "2020-01-02T00:00:00", "manager_code": 27649,
                 "market_api_pct": None, "market_banner": None, "memo": None, "mkb_price": {}, "nds": 18, "num": None,
                 "open_date": None, "partner_pct": "45", "passport_id": 666, "pay_to": None, "payment_type": None,
                 "person_id": 666, "print_tpl_barcode": None, "reward_type": 1, "search_forms": None,
                 "selfemployed": None, "sent_dt": None, "service_start_dt": None, "services": [],
                 "test_mode": None, "type": "PARTNERS", "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1578085200,
            "EventTime": 1578085201,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_completion_out_of_contract_bounds_3",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-uptariffed",
            "OwnerID": 1,
            "PageID": 20,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1578085201,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": -1.2,
            "BillableEventType": "block-show",
            "ClientID": 1,
            "ContractID": 1,
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None, "bm_domains": None,
                 "bm_market_pct": None, "bm_places": None, "client_id": 1, "collateral_type": None,
                 "contract_type": None, "currency": 643, "doc_set": None, "domains": None,
                 "dsp_agregation_pct": None, "dt": "2020-01-02T00:00:00", "end_dt": "2020-01-05T00:00:00",
                 "end_reason": None, "external_id": "e-1", "firm": 1, "id": 1, "individual_docs": None,
                 "is_archived": None, "is_archived_dt": None, "is_booked": None, "is_booked_dt": None,
                 "is_cancelled": None, "is_faxed": None, "is_signed": "2020-01-02T00:00:00", "manager_code": 27649,
                 "market_api_pct": None, "market_banner": None, "memo": None, "mkb_price": {}, "nds": 18, "num": None,
                 "open_date": None, "partner_pct": "45", "passport_id": 666, "pay_to": None, "payment_type": None,
                 "person_id": 666, "print_tpl_barcode": None, "reward_type": 1, "search_forms": None,
                 "selfemployed": None, "sent_dt": None, "service_start_dt": None, "services": [],
                 "test_mode": None, "type": "PARTNERS", "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1578085200,
            "EventTime": 1578085201,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_completion_out_of_contract_bounds_3",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-uptariffed-published_rollback",
            "OwnerID": 1,
            "PageID": 20,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": -1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1578085201,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 1
        },
        {
            "BillableAmount": -1.2,
            "BillableEventType": "block-show",
            "ClientID": 4,
            "ContractID": 41,
            "ContractObject": json.dumps({
                "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.6"
            }),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_contracts_dt_overlap_1",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-rollback",
            "OwnerID": 4,
            "PageID": 400,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": -1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1577864101,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": 1.2,
            "BillableEventType": "block-show",
            "ClientID": 4,
            "ContractID": 41,
            "ContractObject": json.dumps({
                "uri": "file://test_bs_outlay.test_tariffication_layout_cases_bsp_log_/extracted.6"
            }),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_contracts_dt_overlap_1",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-rollback-published_rollback",
            "OwnerID": 4,
            "PageID": 400,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1577864101,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 1
        },
        {
            "BillableAmount": 1.2,
            "BillableEventType": "block-show",
            "ClientID": 4,
            "ContractID": 40,
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None, "bm_domains": None,
                 "bm_market_pct": None, "bm_places": None, "client_id": 4, "collateral_type": None,
                 "contract_type": None, "currency": 643, "doc_set": None, "domains": None, "dsp_agregation_pct": None,
                 "dt": "2019-01-01T00:00:00", "end_dt": "2020-01-02T00:00:00", "end_reason": None,
                 "external_id": "e-40", "firm": 1, "id": 40, "individual_docs": None, "is_archived": None,
                 "is_archived_dt": None, "is_booked": None, "is_booked_dt": None, "is_cancelled": None,
                 "is_faxed": None, "is_signed": "2019-01-01T00:00:00", "manager_code": 27649, "market_api_pct": None,
                 "market_banner": None, "memo": None, "mkb_price": {}, "nds": 18, "num": None, "open_date": None,
                 "partner_pct": "45", "passport_id": 666, "pay_to": None, "payment_type": None, "person_id": 666,
                 "print_tpl_barcode": None, "reward_type": 1, "search_forms": None, "selfemployed": None,
                 "sent_dt": None, "service_start_dt": None, "services": [], "test_mode": None, "type": "PARTNERS",
                 "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_contracts_dt_overlap_1",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-uptariffed",
            "OwnerID": 4,
            "PageID": 400,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": 1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1577864101,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": -1.2,
            "BillableEventType": "block-show",
            "ClientID": 4,
            "ContractID": 40,
            "ContractObject": json.dumps(
                {"agregator_pct": None, "atypical_conditions": None, "bm_direct_pct": None, "bm_domains": None,
                 "bm_market_pct": None, "bm_places": None, "client_id": 4, "collateral_type": None,
                 "contract_type": None, "currency": 643, "doc_set": None, "domains": None, "dsp_agregation_pct": None,
                 "dt": "2019-01-01T00:00:00", "end_dt": "2020-01-02T00:00:00", "end_reason": None,
                 "external_id": "e-40", "firm": 1, "id": 40, "individual_docs": None, "is_archived": None,
                 "is_archived_dt": None, "is_booked": None, "is_booked_dt": None, "is_cancelled": None,
                 "is_faxed": None, "is_signed": "2019-01-01T00:00:00", "manager_code": 27649, "market_api_pct": None,
                 "market_banner": None, "memo": None, "mkb_price": {}, "nds": 18, "num": None, "open_date": None,
                 "partner_pct": "45", "passport_id": 666, "pay_to": None, "payment_type": None, "person_id": 666,
                 "print_tpl_barcode": None, "reward_type": 1, "search_forms": None, "selfemployed": None,
                 "sent_dt": None, "service_start_dt": None, "services": [], "test_mode": None, "type": "PARTNERS",
                 "unilateral_acts": None}
            ),
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": 1.2,
            "EventDate": 1577826000,
            "EventTime": 1577826001,
            "FakePrice": 1.2,
            "ImpID": None,
            "LBMessageUID": "_contracts_dt_overlap_1",
            "OutputProductID": 100002,
            "OutputStatus": "tariffed-uptariffed-published_rollback",
            "OwnerID": 4,
            "PageID": 400,
            "PartnerCost": 1.2,
            "PartnerPrice": 1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": 1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": -1.2,
            "RUBTurnoverWithoutNDS": 0.0,
            "TypeID": 0,
            "UnixTime": 1577864101,
            "YandexPrice": 1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 1
        },
        {
            "BillableAmount": None,
            "BillableEventType": "undo-block-show",
            "ClientID": None,
            "ContractID": None,
            "ContractObject": None,
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": -1.2,
            "EventDate": 1580418000,
            "EventTime": 1580504399,
            "FakePrice": -1.2,
            "ImpID": None,
            "LBMessageUID": "_dropped_undo_6",
            "OutputProductID": 100002,
            "OutputStatus": "untariffed-uptariffed",
            "OwnerID": None,
            "PageID": 21,
            "PartnerCost": -1.2,
            "PartnerPrice": -1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": -1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": None,
            "RUBTurnoverWithoutNDS": None,
            "TypeID": 0,
            "UnixTime": 1580677201,
            "YandexPrice": -1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 0
        },
        {
            "BillableAmount": None,
            "BillableEventType": "undo-block-show",
            "ClientID": None,
            "ContractID": None,
            "ContractObject": None,
            "CostCur": 1.2,
            "DSPID": 1,
            "EventCostVATCorrected": -1.2,
            "EventDate": 1580418000,
            "EventTime": 1580504399,
            "FakePrice": -1.2,
            "ImpID": None,
            "LBMessageUID": "_dropped_undo_6",
            "OutputProductID": 100002,
            "OutputStatus": "untariffed-uptariffed-published_rollback",
            "OwnerID": None,
            "PageID": 21,
            "PartnerCost": -1.2,
            "PartnerPrice": -1.2,
            "PartnerStatID": 0,
            "PlaceID": None,
            "Price": -1.2,
            "RUBAggregatorRewardWithoutNDS": None,
            "RUBRewardWithoutNDS": None,
            "RUBTurnoverWithoutNDS": None,
            "TypeID": 0,
            "UnixTime": 1580677201,
            "YandexPrice": -1.2,
            "_chunk_record_index": None,
            "_offset": None,
            "_partition": None,
            "_topic": None,
            "_topic_cluster": None,
            "_RollbackPublished": 1
        }
    ],
    'attributes': {
        'schema': tests_constants.RETARRIFED_TABLE_SCHEMA,
        constants.LOG_TARIFF_META_ATTR: TARIFFED_META,
        '_yql_row_spec': generate_yql_row_spec(tests_constants.RETARRIFED_TABLE_SCHEMA),
    },
}


@pytest.fixture()
def work_dir_tree(yt_client, yt_root):
    work_dir_path = yt.ypath_join(yt_root, 'work_dir')
    yt_client.create('map_node', work_dir_path)
    work_tree = {
        'tariffed_dir': yt.ypath_join(work_dir_path, 'tariffed'),
        'unacted_events_dir': yt.ypath_join(work_dir_path, 'unacted_events'),
        'retariffed_events_dir': yt.ypath_join(work_dir_path, 'retariffed_events'),
        'published_retariffed_events_dir': yt.ypath_join(work_dir_path, 'published_retariffed_events_dir'),
    }
    for work_leaf in work_tree.values():
        yt_client.create('map_node', work_leaf)
    for table_name, info in DATA.items():
        table_path = yt.ypath_join(work_dir_path, table_name)
        conftest_partner.create_table(yt_client, table_path, info)
        work_tree[table_name] = table_path
    yield work_tree
    yt_client.remove(work_dir_path, force=True, recursive=True)


@pytest.mark.parametrize('published_data', [
    pytest.param([], id='no_published_retariffication_data'),
    pytest.param([{'data': FIRST_REGENERATION_DATA, 'table_postfix': '0001'}],
                 id='published_retariffication_data_without_rollbacks'),
    pytest.param([{'data': FIRST_REGENERATION_DATA, 'table_postfix': '0001'},
                  {'data': SECOND_REGENERATION_DATA, 'table_postfix': '0002'}],
                 id='published_retariffication_data_with_rollbacks'),
])
def test_bs_outlay_retariffication(yt_client, yql_client, udf_server_file_url, work_dir_tree, published_data):
    for data in published_data:
        table_path = yt.ypath_join(work_dir_tree['published_retariffed_events_dir'],
                                   '{}_{}'.format(CURR_ACT_META[constants.RUN_ID_KEY], data['table_postfix']))
        conftest_partner.create_table(yt_client, table_path, data['data'])
    with yt_client.Transaction() as transaction:
        output_retariffed_events_path = bs_outlay_retariffication.run_job(
            yt_client, yql_client,
            current_meta=CURR_ACT_META,
            tariffed_dir=work_dir_tree['tariffed_dir'],
            unacted_events_dir=work_dir_tree['unacted_events_dir'],
            ref_pages_path=work_dir_tree['ref_pages/2020-10-31'],
            ref_contracts_path=work_dir_tree['ref_contracts/2020-10-31'],
            ref_aggregator_pages_path=work_dir_tree['ref_aggregator_pages/2020-10-31'],
            ref_average_discounts_path=work_dir_tree['ref_average_discounts/2020-10-31'],
            output_retariffed_events_dir=work_dir_tree['retariffed_events_dir'],
            published_retariffed_events_dir=work_dir_tree['published_retariffed_events_dir'],
            udf_file_url=udf_server_file_url,
            transaction=transaction,
            job_split_factor=1,
            drop_events_after_day=3,
        )

    return {
        'output_retariffed_events_path': list(sorted(
            yt_client.read_table(output_retariffed_events_path, format="json"),
            key=lambda r: (r['LBMessageUID'], r['OutputStatus'], )
        )),
    }
