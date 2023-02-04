import itertools
import copy
from datetime import datetime

import pytest

import yt.wrapper as yt

from billing.library.python.logfeller_utils import log_interval
from billing.log_tariffication.py.jobs.partner_tariff import bs_outlay
from billing.log_tariffication.py.tests import constants as tests_constants
from billing.log_tariffication.py.tests.integration import conftest_partner
from billing.log_tariffication.py.tests.integration.conftest_partner import to_ts
from billing.library.python.logmeta_utils.meta import (
    get_log_tariff_meta,
    set_log_tariff_meta,
)
from billing.library.python.yql_utils import query_metrics

from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)


RUN_ID = '2020-01-15T12:09:00'
PREV_RUN_ID = '2020-01-08T11:45:00'


def dunion(d1: dict, d2: dict) -> dict:
    return dict(**d1, **d2)


def gen_topics_meta(f: int, n: int) -> list:
    return [{'topic': 't1',
             'cluster': 'c1',
             'partitions': [{'partition': 0,
                             'first_offset': f,
                             'next_offset': n,
                             },
                            ],
             },
            ]


def gen_meta(
        log_interval_meta: list, ref_contracts_interval_meta: list, ref_pages_interval_meta: list,
        run_id: str = RUN_ID, prev_run_id: str = '') -> dict:
    res = {'run_id': run_id,
           'log_interval': {'topics': log_interval_meta},
           'ref_contracts_interval': {'topics': ref_contracts_interval_meta},
           'ref_pages_interval': {'topics': ref_pages_interval_meta},
           }

    if prev_run_id:
        res['prev_run_id'] = prev_run_id

    return res


class BSPartnerDataCase:
    _offset_seq = 0
    _guid_seq = 1

    def __init__(self, name):
        self.name = name
        self.bs_log = []
        self.prev_untariffed = []
        self.contracts = []
        self.pages = []
        self.aggregator_pages = []

    @classmethod
    def inc_guid_seq(cls):
        cls._guid_seq += 1

    @classmethod
    def _append_list(cls, items, dest):
        if type(items) == dict:
            items = [items]
        for i in items:
            dest.append(dunion(i, {
                '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': cls._offset_seq, '_chunk_record_index': 0
            }))
            cls._offset_seq += 1

    def _add_log_row(self, items, dest):
        if type(items) == dict:
            res = [copy.deepcopy(items)]
        else:
            res = copy.deepcopy(items)

        for i in res:
            i['__message__'] = f'{self.name}_{self._guid_seq}'
            self.inc_guid_seq()

        self._append_list(res, dest)

    def add_bs_log(self, bs_log):
        self._add_log_row(bs_log, self.bs_log)
        return self

    def add_prev_untariffed(self, prev_untariffed):
        self._add_log_row(prev_untariffed, self.prev_untariffed)
        return self

    def add_pages(self, pages):
        self._append_list(pages, self.pages)
        return self

    def add_aggregator_pages(self, aggregator_pages):
        self._append_list(aggregator_pages, self.aggregator_pages)
        return self

    def add_contracts(self, contracts):
        self._append_list(contracts, self.contracts)
        return self


cases_bsp_log = [
    BSPartnerDataCase('_contracts_dt_overlap')
    .add_bs_log({'TypeID': 0, 'PageID': 400, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                 'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                 'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
                 'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                 'UnixTime': to_ts(datetime(2020, 1, 1, 10, 35, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                 },
                )
    .add_pages(conftest_partner.gen_page_ref_row(page_id=400, client_id=4))
    .add_contracts([
        conftest_partner.gen_contract_ref_row(client_id=4, contract_id=40, dt=datetime(2019, 1, 1), end_dt=datetime(2020, 1, 2)),
        conftest_partner.gen_contract_ref_row(client_id=4, contract_id=41, dt=datetime(2020, 1, 1), end_dt=datetime(2020, 1, 2)),
    ]),

    BSPartnerDataCase('_completion_out_of_contract_bounds')
    .add_bs_log([{'TypeID': 0, 'PageID': 20, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                  'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                  'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
                  'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                  'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                  },
                 {'TypeID': 0, 'PageID': 20, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                  'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                  'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
                  'EventDate': to_ts(datetime(2020, 1, 4)), 'EventTime': to_ts(datetime(2020, 1, 4, 0, 0, 1)),
                  'UnixTime': to_ts(datetime(2020, 1, 4, 0, 0, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                  },
                 ]
                )
    .add_pages(conftest_partner.gen_page_ref_row(page_id=20, client_id=1))
    .add_contracts(
        conftest_partner.gen_contract_ref_row(client_id=1, contract_id=1, dt=datetime(2020, 1, 2), end_dt=datetime(2020, 1, 3))
    ),

    BSPartnerDataCase('_no_page_ref_data')
    .add_bs_log({'TypeID': 0, 'PageID': 666, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                 'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                 'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
                 'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                 'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                 }
                ),

    BSPartnerDataCase('_tariffed_internal')
    .add_bs_log({'TypeID': 0, 'PageID': 300, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                 'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                 'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
                 'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                 'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                 }
                )
    .add_pages(conftest_partner.gen_page_ref_row(page_id=300, internal=True, client_id=5)),

    BSPartnerDataCase('_dropped_undo')
    .add_bs_log({'TypeID': 0, 'PageID': 21, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                 'FakePrice': -1.2, 'YandexPrice': -1.2, 'PartnerCost': -1.2, 'PartnerPrice': -1.2,
                 'EventCostVATCorrected': -1.2, 'Price': -1.2, 'BillableEventType': 'undo-block-show',
                 'EventDate': to_ts(datetime(2019, 12, 31)), 'EventTime': to_ts(datetime(2019, 12, 31, 23, 59, 59)),
                 'UnixTime': to_ts(datetime(2020, 2, 3, 0, 0, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                 }
                )
    .add_pages(conftest_partner.gen_page_ref_row(page_id=21, client_id=1)),

    BSPartnerDataCase('_dropped_untariffed')
    .add_bs_log({'TypeID': 0, 'PageID': 21, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                 'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                 'EventCostVATCorrected': -1.2, 'Price': -1.2, 'BillableEventType': 'block-show',
                 'EventDate': to_ts(datetime(2019, 12, 31)), 'EventTime': to_ts(datetime(2019, 12, 31, 23, 59, 59)),
                 'UnixTime': to_ts(datetime(2020, 2, 3, 0, 0, 1)), 'PartnerStatID': 0, 'PlaceID': None,
                 }
                )
    .add_pages(conftest_partner.gen_page_ref_row(page_id=21, client_id=1)),

    BSPartnerDataCase('_collateral_pct_change')
    .add_bs_log([{'TypeID': 0, 'PageID': 200, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
                  'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                  'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
                  'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                  'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0,
                  },
                 {'TypeID': 1, 'PageID': 200, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
                  'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                  'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
                  'EventDate': to_ts(datetime(2020, 1, 2)), 'EventTime': to_ts(datetime(2020, 1, 2, 0, 0, 1)),
                  'UnixTime': to_ts(datetime(2020, 1, 2, 0, 0, 1)), 'PartnerStatID': 0,
                  },
                 ]
                )
    .add_pages(conftest_partner.gen_page_ref_row(page_id=200, client_id=2))
    .add_contracts(conftest_partner.gen_contract_ref_row(
        client_id=2, contract_id=2, dt=datetime(2020, 1, 1), end_dt=datetime(2020, 1, 3),
        collaterals=[{
            'dt': '2020-01-02T00:00:00', 'partner_pct': '90', 'num': '1',
            'is_signed': '2020-01-02T00:00:00', 'collateral_type_id': 2020
        }]
    )),

    BSPartnerDataCase('_tariff_in_test_mode')
    .add_bs_log([{'TypeID': 0, 'PageID': 500, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
                  'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                  'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
                  'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                  'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0,
                  },
                 ]
                )
    .add_pages(conftest_partner.gen_page_ref_row(page_id=500, client_id=5))
    .add_contracts(conftest_partner.gen_contract_ref_row(
        client_id=5, contract_id=510, dt=datetime(2020, 1, 1), end_dt=datetime(2020, 1, 3), signed=False, test_mode=True
    )),

    BSPartnerDataCase('_aggregator')
    .add_bs_log([
        {
            'TypeID': 0, 'PageID': 900, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
            'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
            'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
            'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
            'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0,
        },
        {
            'TypeID': 0, 'PageID': 900, 'DSPID': None, 'PlaceID': None, 'CostCur': 1.2,
            'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
            'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
            'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
            'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0,
        },
    ])
    .add_pages(conftest_partner.gen_page_ref_row(page_id=900, client_id=17))
    .add_aggregator_pages(conftest_partner.gen_aggregator_page_ref_row(page_id=900, client_id=19))
    .add_contracts(conftest_partner.gen_contract_ref_row(
        client_id=19, contract_id=910, dt=datetime(2020, 1, 1), signed=True, agregator_pct='47.3', dsp_agregation_pct='10'
    )),

    BSPartnerDataCase('_drop_outdated')
    .add_bs_log([
        {
            'TypeID': 0, 'PageID': 900, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
            'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
            'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
            'EventDate': to_ts(datetime(2019, 12, 31)), 'EventTime': to_ts(datetime(2019, 12, 31, 17, 21)),
            'UnixTime': to_ts(datetime(2020, 1, 2, 0, 0, 1)), 'PartnerStatID': 0,
        },
    ]),

   BSPartnerDataCase('_avg_discount_apply')
    .add_bs_log([{'TypeID': 0, 'PageID': 1000, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
                  'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                  'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
                  'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                  'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0,
                  },
                 {'TypeID': 1, 'PageID': 1000, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
                  'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                  'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
                  'EventDate': to_ts(datetime(2020, 1, 2)), 'EventTime': to_ts(datetime(2020, 1, 2, 0, 0, 1)),
                  'UnixTime': to_ts(datetime(2020, 1, 2, 0, 0, 1)), 'PartnerStatID': 0,
                  },
                 {'TypeID': 0, 'PageID': 1000, 'DSPID': 1, 'PlaceID': None, 'CostCur': 1.2,
                  'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                  'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'block-show',
                  'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                  'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0,
                  },
                 ]
                )
    .add_pages(conftest_partner.gen_page_ref_row(page_id=1000, client_id=1050))
    .add_contracts(conftest_partner.gen_contract_ref_row(
        client_id=1050, reward_type=conftest_partner.REWARD_NET, contract_id=1100, dt=datetime(2020, 1, 1), end_dt=datetime(2020, 1, 3)
    )),
]


cases_untariffed = [BSPartnerDataCase('_tariff_the_untariffed')
                    .add_bs_log({'TypeID': 0, 'PageID': 220, 'DSPID': None, 'PlaceID': 542, 'CostCur': 2.2,
                                 'FakePrice': 2.2, 'YandexPrice': 2.2, 'PartnerCost': 2.2, 'PartnerPrice': 2.2,
                                 'EventCostVATCorrected': 2.2, 'Price': 2.2, 'BillableEventType': 'click',
                                 'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 1, 0, 1)),
                                 'UnixTime': to_ts(datetime(2020, 1, 1, 1, 0, 1)), 'PartnerStatID': 0,
                                 }
                                )
                    .add_prev_untariffed({'TypeID': 0, 'PageID': 220, 'DSPID': None, 'PlaceID': 542, 'CostCur': 1.2,
                                          'FakePrice': 1.2, 'YandexPrice': 1.2, 'PartnerCost': 1.2, 'PartnerPrice': 1.2,
                                          'EventCostVATCorrected': 1.2, 'Price': 1.2, 'BillableEventType': 'click',
                                          'EventDate': to_ts(datetime(2020, 1, 1)), 'EventTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)),
                                          'UnixTime': to_ts(datetime(2020, 1, 1, 0, 0, 1)), 'PartnerStatID': 0, 'LBMessageUID': 'prev_untariffed_0',
                                          }
                                         )
                    .add_pages(conftest_partner.gen_page_ref_row(page_id=220, client_id=3))
                    .add_contracts(conftest_partner.gen_contract_ref_row(client_id=3, contract_id=33, dt=datetime(2019, 1, 1))),
                    ]


@pytest.mark.parametrize(['case_data'],
                         [pytest.param(cases_bsp_log, id='cases_bsp_log'),
                          pytest.param(cases_untariffed, id='cases_untariffed'),
                          ]
                         )
def test_tariffication_layout(yt_root,
                              yt_client,
                              yt_transaction,
                              yql_client,
                              udf_server_file_url,
                              case_data):

    all_page_rows = list(itertools.chain(*[case.pages for case in case_data]))
    all_aggregator_page_rows = list(itertools.chain(*[case.aggregator_pages for case in case_data]))
    all_contract_rows = list(itertools.chain(*[case.contracts for case in case_data]))
    all_bs_log_rows = list(itertools.chain(*[case.bs_log for case in case_data]))
    all_prev_untariffed_rows = list(itertools.chain(*[case.prev_untariffed for case in case_data]))

    all_average_discounts_rows = [{'start_dt': '2000-01-01', 'service_id': 7, 'pct': 50.0},
                                  {'start_dt': '2020-01-02', 'service_id': 7, 'pct': 75.0},
                                  ]

    prev_run_id=PREV_RUN_ID if all_prev_untariffed_rows else None
    current_meta = gen_meta(gen_topics_meta(0, BSPartnerDataCase._offset_seq),
                            gen_topics_meta(0, BSPartnerDataCase._offset_seq),
                            gen_topics_meta(0, BSPartnerDataCase._offset_seq),
                            prev_run_id=prev_run_id)

    bs_logs_dir = yt.ypath_join(yt_root, 'bs_log')
    bs_log_table_path = yt.ypath_join(bs_logs_dir, PREV_RUN_ID)
    contract_table_path = yt.ypath_join(yt_root, 'contracts')
    pages_table_path = yt.ypath_join(yt_root, 'pages')
    aggregator_pages_table_path = yt.ypath_join(yt_root, 'aggregator_pages')
    average_discounts_table_path = yt.ypath_join(yt_root, 'average_discounts')
    prev_untariffed_merged_dir = create_subdirectory(yt_client, yt_root, 'merged_untariffed')
    prev_untariffed_table_path = yt.ypath_join(prev_untariffed_merged_dir, prev_run_id) if prev_run_id else ''
    output_tariffed_dir = create_subdirectory(yt_client, yt_root, 'output_tariffed')
    output_untariffed_dir = create_subdirectory(yt_client, yt_root, 'output_untariffed')
    output_tariffed_internal_dir = create_subdirectory(yt_client, yt_root, 'output_tariffed_internal')

    get_attrs = lambda schema: {
        log_interval.LB_META_ATTR: {'topics': gen_topics_meta(0, BSPartnerDataCase._offset_seq)},
        'schema': schema
    }

    remove_tabs = []
    input_tables = {
        pages_table_path: {
            'schema': tests_constants.REFERENCE_LOG_TABLE_SCHEMA,
            'rows': all_page_rows
        },
        contract_table_path: {
            'schema': tests_constants.REFERENCE_LOG_TABLE_SCHEMA,
            'rows': all_contract_rows
        },
        average_discounts_table_path : {
            'schema': tests_constants.REFERENCE_AVERAGE_DISCOUNTS_SCHEMA,
            'rows': all_average_discounts_rows
        },
        aggregator_pages_table_path: {
            'schema': tests_constants.REFERENCE_LOG_TABLE_SCHEMA,
            'rows': all_aggregator_page_rows
        },
        bs_log_table_path: {
            'schema': tests_constants.BILLABLE_PARTNER_LOG_TABLE_SCHEMA + [
                {'name': '__message__', 'type': 'string'},
            ],
            'rows': all_bs_log_rows
        },
        prev_untariffed_table_path: {
            'schema': tests_constants.BILLABLE_PARTNER_LOG_TABLE_SCHEMA + [
                {'name': '__message__', 'type': 'string'},
                {'name': 'LBMessageUID', 'type': 'string'},
            ],
            'rows': all_prev_untariffed_rows
        },
    }

    for table_path, info in input_tables.items():
        if not table_path:
            continue
        yt_client.create('table', table_path, recursive=True,
                            attributes=get_attrs(info['schema']))
        if info['rows']:
            yt_client.write_table(table_path, info['rows'])
        set_log_tariff_meta(yt_client, table_path, current_meta)
        remove_tabs.append(table_path)

    output_tariffed, output_untariffed, output_tariffed_internal = bs_outlay.run_job(
        yt_client,
        yql_client,
        current_meta,
        bs_logs_dir=bs_logs_dir,
        ref_pages=pages_table_path,
        ref_contracts=contract_table_path,
        ref_aggregator_pages=aggregator_pages_table_path,
        ref_average_discounts=average_discounts_table_path,
        prev_untariffed_merged_dir=prev_untariffed_merged_dir,
        output_tariffed_dir=output_tariffed_dir,
        output_untariffed_dir=output_untariffed_dir,
        output_tariffed_internal_dir=output_tariffed_internal_dir,
        udf_file_url=udf_server_file_url,
        transaction=yt_transaction,
    )

    for t in remove_tabs:
        yt_client.remove(t)

    sort = lambda l, key: sorted(l, key=lambda x: x[key])

    return {
        'tariffed_rows': sort(list(yt_client.read_table(output_tariffed)), '__message__'),
        'untariffed_rows': sort(list(yt_client.read_table(output_untariffed)), '__message__'),
        'internal_rows': sort(list(yt_client.read_table(output_tariffed_internal)), '__message__'),
        'output_tariffed_meta': get_log_tariff_meta(yt_client, output_tariffed),
        'output_untariffed_meta': get_log_tariff_meta(yt_client, output_untariffed),
        'output_tariffed_internal_meta': get_log_tariff_meta(yt_client, output_tariffed_internal),
        'output_tariffed_metrics': yt.yson.convert.yson_to_json(
            query_metrics.get_table_metrics_data(yt_client, output_tariffed)
        ),
        'output_untariffed_metrics': yt.yson.convert.yson_to_json(
            query_metrics.get_table_metrics_data(yt_client, output_untariffed)
        ),
        'output_tariffed_internal_metrics': yt.yson.convert.yson_to_json(
            query_metrics.get_table_metrics_data(yt_client, output_tariffed_internal)
        ),
    }
