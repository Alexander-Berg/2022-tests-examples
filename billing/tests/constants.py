# coding=utf-8
from typing import Dict

import arrow

from billing.library.python.logmeta_utils.meta import generate_run_id
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)

CURR_RUN_DT = arrow.Arrow(2020, 6, 6, 6)
PREV_RUN_DT = CURR_RUN_DT.shift(minutes=-5)
PREV_DAY_RUN_DT = CURR_RUN_DT.shift(days=-1)

CURR_RUN_ID = generate_run_id(CURR_RUN_DT)
OLD_RUN_ID = generate_run_id(CURR_RUN_DT.shift(minutes=-10))
PREV_RUN_ID = generate_run_id(PREV_RUN_DT)
PREV_DAY_RUN_ID = generate_run_id(PREV_DAY_RUN_DT)
NEXT_RUN_ID = generate_run_id(CURR_RUN_DT.shift(minutes=5))

AUTO_OVERDRAFT_DT = '0000-000'

TARIFF_DATE_START_HOUR = 3

# doesn't precede current interval
OLD_LOG_INTERVAL = LogInterval([
    Subinterval('c1', 't1', 1, 0, 5),
])

PREV_LOG_INTERVAL = LogInterval([
    Subinterval('c1', 't1', 0, 0, 10),
    Subinterval('c1', 't1', 1, 10, 15),
])

CURR_LOG_INTERVAL = LogInterval([
    Subinterval('c1', 't1', 0, 10, 20),
    Subinterval('c1', 't1', 1, 15, 25),
])

NEXT_LOG_INTERVAL = LogInterval([
    Subinterval('c1', 't1', 0, 20, 30),
    Subinterval('c1', 't1', 1, 25, 35),
])

ZERO_CORRECTIONS_INTERVAL = LogInterval([
    Subinterval('cor_c1', 'cor_t1', 0, 0, 0),
    Subinterval('cor_c1', 'cor_t1', 1, 0, 0),
])

PREV_CORRECTIONS_INTERVAL = LogInterval([
    Subinterval('cor_c1', 'cor_t1', 0, 0, 1),
    Subinterval('cor_c1', 'cor_t1', 1, 10, 15),
])

CURR_CORRECTIONS_INTERVAL = LogInterval([
    Subinterval('cor_c1', 'cor_t1', 0, 1, 2),
    Subinterval('cor_c1', 'cor_t1', 1, 15, 25),
])

NEXT_CORRECTIONS_INTERVAL = LogInterval([
    Subinterval('cor_c1', 'cor_t1', 0, 2, 3),
    Subinterval('cor_c1', 'cor_t1', 1, 25, 35),
])

OLD_LOG_TARIFF_META = dict(
    log_interval=OLD_LOG_INTERVAL.to_meta(),
    run_id=OLD_RUN_ID,
    prev_run_id=None,
    auto_overdraft_dt=AUTO_OVERDRAFT_DT
)

PREV_LOG_TARIFF_META = dict(
    log_interval=PREV_LOG_INTERVAL.to_meta(),
    run_id=PREV_RUN_ID,
    prev_run_id=OLD_RUN_ID,
    auto_overdraft_dt=AUTO_OVERDRAFT_DT
)

CURR_LOG_TARIFF_META = dict(
    log_interval=CURR_LOG_INTERVAL.to_meta(),
    run_id=CURR_RUN_ID,
    prev_run_id=PREV_RUN_ID,
    tariff_date=172800,
    auto_overdraft_dt=AUTO_OVERDRAFT_DT
)

NEXT_LOG_TARIFF_META = dict(
    log_interval=NEXT_LOG_INTERVAL.to_meta(),
    run_id=NEXT_RUN_ID,
    prev_run_id=CURR_RUN_ID,
    auto_overdraft_dt=AUTO_OVERDRAFT_DT
)

LOGFELLER_TABLE_SCHEMA = [
    {'name': '_topic_cluster', 'type': 'string'},
    {'name': '_topic', 'type': 'string'},
    {'name': '_partition', 'type': 'uint64'},
    {'name': '_offset', 'type': 'uint64'},
    {'name': '_chunk_record_index', 'type': 'uint64'},
]


STREAM_LOG_TABLE_SCHEMA = [
    {'name': 'OrderID', 'type': 'uint64'},
    {'name': 'EventTime', 'type': 'int64'},
    {'name': 'Bucks', 'type': 'uint64'},
] + LOGFELLER_TABLE_SCHEMA


BILLABLE_LOG_TABLE_SCHEMA = [
    {'name': 'ServiceID', 'type': 'int64'},
    {'name': 'ServiceOrderID', 'type': 'int64'},
    {'name': 'CurrencyID', 'type': 'uint64'},
    {'name': 'BillableEventCostCur', 'type': 'double'},
    {'name': 'EventCost', 'type': 'int64'},
    {'name': 'EventTime', 'type': 'int64'},
] + LOGFELLER_TABLE_SCHEMA


def yson_field(name) -> Dict:
    return {
        'name': name,
        'type': 'any',
        'type_v3': {
            'item': 'yson',
            'type_name': 'optional',
        },
    }


REFERENCE_SNAPSHOT_TABLE_SCHEMA = [
    {'name': 'ID', 'type': 'uint64'},
    {'name': 'Version', 'type': 'uint64'},
    yson_field('Object'),
]


REFERENCE_LOG_TABLE_SCHEMA = REFERENCE_SNAPSHOT_TABLE_SCHEMA + LOGFELLER_TABLE_SCHEMA


BILLABLE_PARTNER_LOG_TABLE_SCHEMA = [
    {'name': 'PageID', 'type': 'int64'},
    {'name': 'PlaceID', 'type': 'int64'},
    {'name': 'DSPID', 'type': 'int64'},
    {'name': 'ImpID', 'type': 'int64'},
    {'name': 'CostCur', 'type': 'double'},
    {'name': 'FakePrice', 'type': 'double'},
    {'name': 'YandexPrice', 'type': 'double'},
    {'name': 'PartnerCost', 'type': 'double'},
    {'name': 'PartnerPrice', 'type': 'double'},
    {'name': 'EventCostVATCorrected', 'type': 'double'},
    {'name': 'Price', 'type': 'double'},
    {'name': 'BillableEventType', 'type': 'string'},
    {'name': 'EventDate', 'type': 'int64'},
    {'name': 'EventTime', 'type': 'int64'},
    {'name': 'UnixTime', 'type': 'int64'},
    {'name': 'PartnerStatID', 'type': 'int64'},
    {'name': 'TypeID', 'type': 'int64'},
] + LOGFELLER_TABLE_SCHEMA

BILLED_PARTNER_LOG_TABLE_SCHEMA = [
    {'name': 'LBMessageUID', 'type': 'string'},
    {'name': '_BillingTS', 'type': 'int64'},
    {'name': 'ClientID', 'type': 'int64'},
    {'name': 'OwnerID', 'type': 'int64'},
    {'name': 'ContractID', 'type': 'int64'},
    {'name': 'BillableAmount', 'type': 'double'},
    {'name': 'RUBRewardWithoutNDS', 'type': 'double'},
    {'name': 'RUBTurnoverWithoutNDS', 'type': 'double'},
    {'name': 'RUBAggregatorRewardWithoutNDS', 'type': 'double'},
    {'name': 'OutputProductID', 'type': 'int64'},
    {'name': 'OutputStatus', 'type': 'string'},
    {'name': 'ContractObject', 'type': 'string'},
] + BILLABLE_PARTNER_LOG_TABLE_SCHEMA

REFERENCE_FIRM_SCHEMA = [
    {'name': 'id', 'type': 'int64'},
    {'name': 'title', 'type': 'string'},
    {'name': 'mdh_id', 'type': 'string'},
    yson_field('tax_policies'),
    yson_field('person_categories'),
]

BILLABLE_DSP_LOG_TABLE_SCHEMA = BILLABLE_PARTNER_LOG_TABLE_SCHEMA

BILLED_DSP_LOG_TABLE_SCHEMA = [
    {'name': 'LBMessageUID', 'type': 'string'},
    {'name': 'BillableAmount', 'type': 'double'},
    {'name': 'BillableAmountCurrency', 'type': 'string'},
    {'name': 'ClientID', 'type': 'int64'},
    {'name': 'ContractID', 'type': 'int64'},
    {'name': 'ContractObject', 'type': 'string'},
    {'name': 'Amount', 'type': 'double'},
    {'name': 'AmountWithoutNDS', 'type': 'double'},
    {'name': 'AmountCurrency', 'type': 'string'},
    {'name': 'CurrencyRate', 'type': 'double'},
    {'name': 'CurrencyRateDate', 'type': 'int64'},
    {'name': 'ProductID', 'type': 'int64'},
    {'name': 'ProductMdhID', 'type': 'string'},
    {'name': 'TaxPolicyPercentID', 'type': 'int64'},
    {'name': 'TaxPolicyPercentMdhID', 'type': 'string'},
    {'name': 'TaxPolicyID', 'type': 'int64'},
    {'name': 'TaxPolicyMdhID', 'type': 'string'},
    {'name': 'OutputStatus', 'type': 'string'},
] + BILLABLE_DSP_LOG_TABLE_SCHEMA

UNTARIFFED_DSP_LOG_TABLE_SCHEMA = [
    yson_field('_TarifficationInfo')
] + BILLABLE_DSP_LOG_TABLE_SCHEMA

REFERENCE_AVERAGE_DISCOUNTS_SCHEMA = [
    {'name': 'service_id', 'type': 'int64'},
    {'name': 'start_dt', 'type': 'string'},
    {'name': 'pct', 'type': 'double'},
]

ACT_O_TRON_COMMON_ACT_ROWS_TABLE_SCHEMA = [
    {'name': 'mdh_product_id', 'type': 'string'},
    {'name': 'act_row_id', 'type': 'string'},
    {'name': 'act_sum', 'type': 'double'},
    {'name': 'act_effective_nds_pct', 'type': 'double'},
    {'name': 'tariffer_service_id', 'type': 'int64'},
    {'name': 'act_start_dt', 'type': 'string'},
    {'name': 'act_finish_dt', 'type': 'string'},
]

PARTNER_ACT_ROWS_TABLE_SCHEMA = [
    {'name': 'BillableAmount', 'type': 'double'},
    {'name': 'BlockID', 'type': 'int64'},
    {'name': 'Clicks', 'type': 'int64'},
    {'name': 'ClientID', 'type': 'int64'},
    {'name': 'ContractID', 'type': 'int64'},
    {'name': 'DSPID', 'type': 'int64'},
    {'name': 'DT', 'type': 'string'},
    {'name': 'PageID', 'type': 'int64'},
    {'name': 'ProductID', 'type': 'int64'},
    {'name': 'RUBAggregatorRewardWithoutNDS', 'type': 'double'},
    {'name': 'RUBRewardWithoutNDS', 'type': 'double'},
    {'name': 'RUBTurnoverWithoutNDS', 'type': 'double'},
    {'name': 'Shows', 'type': 'int64'},
    {'name': 'ActRowID', 'type': 'string'},
]

DSP_ACT_ROWS_TABLE_SCHEMA = [
    {'name': 'client_id', 'type': 'int64'},
    {'name': 'contract_id', 'type': 'int64'},
    {'name': 'currency', 'type': 'string'},
    {'name': 'tax_policy_mdh_id', 'type': 'string'},
    {'name': 'act_effective_tax_policy_pct_mdh_id', 'type': 'string'},
    {'name': 'act_sum_wo_nds', 'type': 'double'},
    {'name': 'events_sum', 'type': 'double'},
    {'name': 'events_currency', 'type': 'string'},
    {'name': 'events_month_dt', 'type': 'string'},

] + ACT_O_TRON_COMMON_ACT_ROWS_TABLE_SCHEMA

RETARRIFED_TABLE_SCHEMA = [
    {'name': 'LBMessageUID', 'type': 'string'},
    {'name': 'ClientID', 'type': 'int64'},
    {'name': 'OwnerID', 'type': 'int64'},
    {'name': 'ContractID', 'type': 'int64'},
    {'name': 'BillableAmount', 'type': 'double'},
    {'name': 'RUBRewardWithoutNDS', 'type': 'double'},
    {'name': 'RUBTurnoverWithoutNDS', 'type': 'double'},
    {'name': 'RUBAggregatorRewardWithoutNDS', 'type': 'double'},
    {'name': 'OutputProductID', 'type': 'int64'},
    {'name': 'OutputStatus', 'type': 'string'},
    {'name': 'ContractObject', 'type': 'string'},
    {'name': '_RollbackPublished', 'type': 'int64'}
] + BILLABLE_PARTNER_LOG_TABLE_SCHEMA
