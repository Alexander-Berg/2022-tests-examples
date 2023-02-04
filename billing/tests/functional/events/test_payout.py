from datetime import datetime, timedelta

import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries, has_key

from billing.hot.calculators.taxi.calculator.core.constants import (
    CARD_COMM_SID, CASH_COMM_SID, CASHLESS_SID, CORP_SPENDABLE_SID, LOGISTICS_SID, REQUEST_SID, SUBVENTION_SID
)
from billing.hot.calculators.taxi.calculator.core.entities.personal_account import ServiceCode
from billing.hot.calculators.taxi.calculator.tests.builder import (
    gen_account, gen_general_contract, gen_loc, gen_payout_event, gen_personal_account, gen_spendable_contract,
    gen_transfer_event
)
from billing.hot.calculators.taxi.calculator.tests.const import (
    CLIENT_ID, CLIENT_SD_ID, COMMON_DT, COMMON_DT_TS, GENERAL_CONTRACT_ID, GENERAL_SD_CONTRACT_ID, PERSON_ID,
    SPENDABLE_CONTRACT_ID
)

from . import expected_client_transactions

params = [
    # multiaccount refund-payments netting begin
    [
        [
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='2500', debit='1000', ts=COMMON_DT_TS),
            gen_account('compensations', CLIENT_ID, GENERAL_CONTRACT_ID, credit='2000', debit='1000', ts=COMMON_DT_TS),
            gen_account('cashless_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000', debit='2700',
                        ts=COMMON_DT_TS),
            gen_account('compensations_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000', debit='1700',
                        ts=COMMON_DT_TS),
        ],
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID,
                services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1400.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 100.0,
                     'type': 'debit'},
                    {
                        'loc': gen_loc('cashless_refunds', **{
                            'client_id': 1,
                            'contract_id': 666,
                            'currency': 'RUB',
                            'terminal_id': 0
                        }),
                        'amount': 300.0,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc('cashless_refunds', **{
                            'client_id': 1,
                            'contract_id': 666,
                            'currency': 'RUB',
                            'terminal_id': 0
                        }),
                        'amount': 1400.0,
                        'type': 'credit'
                    },
                    {'loc': gen_loc('compensations', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 700.0,
                     'type': 'debit'},
                    {'loc': gen_loc('compensations', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 300.0,
                     'type': 'debit'},
                    {'loc': gen_loc('compensations_refunds', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 700.0,
                     'type': 'credit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'service_id': 124}),
                     'amount': 100.0,
                     'type': 'credit'},
                ]
            }
        ],
        {
            'contract_states': {
                '666': {'netting_done': True,
                        'contract_external_id': 'test_general',
                        'payment_amount': 100.0,
                        'payment_currency': 'RUB'}
            },
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_compensations_and_cashless'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_camera'
                    },
                    {
                        'expressions': [],
                        'name': 'fuel'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_car_and_lightbox'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 700.0,
                                'account': 'compensations',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 700.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 300.0,
                                'account': 'compensations',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 300.0,
                                'account': 'cashless_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1400.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1400.0,
                                'account': 'cashless_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 100.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 100.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'payout_acc'
                    }
                ]
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # multiaccount refund-payments netting end

    # compensations/cashless payout
    [
        [
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='2500', debit='1000', ts=COMMON_DT_TS),
            gen_account('compensations', CLIENT_ID, GENERAL_CONTRACT_ID, credit='2000', debit='1000', ts=COMMON_DT_TS),
        ],
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID,
                services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1500.0,
                     'type': 'debit'},
                    {'loc': gen_loc('compensations',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'service_id': 124}),
                     'amount': 1500.0,
                     'type': 'credit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'service_id': 124}),
                     'amount': 1000.0,
                     'type': 'credit'},
                ]
            }
        ],
        {
            'contract_states': {
                '666': {'netting_done': True,
                        'contract_external_id': 'test_general',
                        'payment_amount': 2500.0,
                        'payment_currency': 'RUB'}
            },
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_compensations_and_cashless'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_camera'
                    },
                    {
                        'expressions': [],
                        'name': 'fuel'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_car_and_lightbox'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 1500.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1500.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'payout_acc'
                    }
                ]
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # compensations/cashless payout end

    # empty accounts balance begin
    [
        None,
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [],
        True,
        [],
        {
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_compensations_and_cashless'
                    }
                ]
            }
        },
        [],
    ],
    # empty accounts balance end

    # not migrated client begin
    [
        [
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1500'),
            gen_account('compensations', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000'),
            gen_account('cashless_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='1700'),
            gen_account('compensations_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='700')
        ],
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [],
        False,
        [],
        {},
        [],
    ],
    # not migrated client end

    # subvention contract payout case begin
    [
        [
            gen_account('subventions', CLIENT_ID, SPENDABLE_CONTRACT_ID, credit='4000', debit='1000'),
            gen_account('subventions_refunds', CLIENT_ID, SPENDABLE_CONTRACT_ID, debit='1500')
        ],
        [
            gen_spendable_contract(SPENDABLE_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[SUBVENTION_SID])
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 667, 'currency': 'RUB', 'service_id': 137}),
                     'amount': 1500.0,
                     'type': 'credit'},
                    {
                        'loc': gen_loc(
                            'subventions',
                            **{
                                'client_id': 1,
                                'contract_id': 667,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 3000.0,
                        'type': 'debit'
                    },
                    {
                        'loc': gen_loc(
                            'subventions_refunds',
                            **{
                                'client_id': 1,
                                'contract_id': 667,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 1500.0,
                        'type': 'credit'
                    },
                ]
            }
        ],
        {
            'contract_states': {
                '667': {'netting_done': True,
                        'contract_external_id': 'test_spendable',
                        'payment_amount': 1500.0,
                        'payment_currency': 'RUB'}
            },
            'operation_expressions': {
                '667': []
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # subvention contract payout case end

    # subvention refunds more than payments, no payout begin
    [
        [
            gen_account('subventions', CLIENT_ID, SPENDABLE_CONTRACT_ID, credit='3000'),
            gen_account('subventions_refunds', CLIENT_ID, SPENDABLE_CONTRACT_ID, debit='3000.01')
        ],
        [
            gen_spendable_contract(SPENDABLE_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[SUBVENTION_SID])
        ],
        [],
        True,
        [],
        {
            'contract_states': {
                '667': {'netting_done': True,
                        'contract_external_id': 'test_spendable',
                        'payment_amount': 0.0,
                        'payment_currency': 'RUB'}
            },
            'operation_expressions': {
                '667': []
            }
        },
        []
    ],
    # subvention refunds more than payments, no payout end

    # corporate contract payout case begin
    [
        [
            gen_account('corporate', CLIENT_ID, SPENDABLE_CONTRACT_ID, credit='3000'),
            gen_account('corporate_refunds', CLIENT_ID, SPENDABLE_CONTRACT_ID, debit='1500')
        ],
        [
            gen_spendable_contract(SPENDABLE_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CORP_SPENDABLE_SID])
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions':
                    [
                        {'loc': gen_loc('payout',
                                        **{'client_id': 1, 'contract_id': 667, 'currency': 'RUB', 'service_id': 651}),
                         'amount': 1500.0,
                         'type': 'credit'},
                        {
                            'loc': gen_loc(
                                'corporate',
                                **{
                                    'client_id': 1,
                                    'contract_id': 667,
                                    'currency': 'RUB',
                                    'detailed_product': '',
                                    'product': '',
                                    'region': ''
                                }
                            ),
                            'amount': 3000.0,
                            'type': 'debit'
                        },
                        {
                            'loc': gen_loc(
                                'corporate_refunds',
                                **{
                                    'client_id': 1,
                                    'contract_id': 667,
                                    'currency': 'RUB',
                                    'detailed_product': '',
                                    'product': '',
                                    'region': ''
                                }
                            ),
                            'amount': 1500.0,
                            'type': 'credit'
                        },
                    ]
            }
        ],
        {
            'contract_states': {
                '667': {'netting_done': True,
                        'contract_external_id': 'test_spendable',
                        'payment_amount': 1500.0,
                        'payment_currency': 'RUB'},
            },
            'operation_expressions': {
                '667': []
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # corporate contract payout case end

    # logistics contract payout case begin
    [
        [
            gen_account('logistics', CLIENT_ID, SPENDABLE_CONTRACT_ID, credit='3000'),
            gen_account('logistics_refunds', CLIENT_ID, SPENDABLE_CONTRACT_ID, debit='1500')
        ],
        [
            gen_spendable_contract(SPENDABLE_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[LOGISTICS_SID])
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 667, 'currency': 'RUB',
                                       'service_id': LOGISTICS_SID}),
                     'amount': 1500.0,
                     'type': 'credit'},
                    {
                        'loc': gen_loc(
                            'logistics',
                            **{
                                'client_id': 1,
                                'contract_id': 667,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 3000.0,
                        'type': 'debit'
                    },
                    {
                        'loc': gen_loc(
                            'logistics_refunds',
                            **{
                                'client_id': 1,
                                'contract_id': 667,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 1500.0,
                        'type': 'credit'
                    },
                ]
            }
        ],
        {
            'contract_states': {
                '667': {'netting_done': True,
                        'contract_external_id': 'test_spendable',
                        'payment_amount': 1500.0,
                        'payment_currency': 'RUB'}
            },
            'operation_expressions': {
                '667': []
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # logistics contract payout case end

    # corporate refunds more than payments, no payout begin
    [
        [
            gen_account('corporate', CLIENT_ID, SPENDABLE_CONTRACT_ID, credit='3000'),
            gen_account('corporate_refunds', CLIENT_ID, SPENDABLE_CONTRACT_ID, debit='3000.01')
        ],
        [
            gen_spendable_contract(
                SPENDABLE_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CORP_SPENDABLE_SID]
            )
        ],
        [],
        True,
        [],
        {
            'contract_states': {
                '667': {'netting_done': True,
                        'contract_external_id': 'test_spendable',
                        'payment_amount': 0.0,
                        'payment_currency': 'RUB'}
            },
            'operation_expressions': {
                '667': []
            }
        },
        [],
    ],
    # corporate refunds more than payments, no payout end

    # complex netting of all the general contract products with payout begin
    [
        [
            gen_account('promocodes', CLIENT_ID, GENERAL_CONTRACT_ID, credit='500'),
            gen_account('commissions', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2700'),
            gen_account('commissions_with_vat', CLIENT_ID, GENERAL_CONTRACT_ID, debit='3240'),
            gen_account('fuel_hold', CLIENT_ID, GENERAL_CONTRACT_ID, credit='100'),
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='4900'),
            gen_account('compensations', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000'),
            gen_account('cashless_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='1000'),
            gen_account('compensations_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2000'),
            gen_account('transfer_source_selfemployed_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_hold_selfemployed_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('foreign_income_selfemployed_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_source_lightbox_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_hold_lightbox_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('foreign_income_lightbox_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_source_signalq_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_hold_signalq_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
        ],
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 2740.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 100.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 60.0,
                     'type': 'debit'},
                    {
                        'loc': gen_loc('cashless_refunds', **{
                            'client_id': 1,
                            'contract_id': 666,
                            'currency': 'RUB',
                            'terminal_id': 0
                        }),
                        'amount': 1000.0,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 416.666667,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 2283.333333,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions_with_vat',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 500.0,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions_with_vat',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 2740.0,
                        'type': 'credit'
                    },
                    {'loc': gen_loc('compensations', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('compensations_refunds', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'credit'},
                    {'loc': gen_loc('compensations_refunds', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'credit'},
                    {'loc': gen_loc('fuel_hold', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 100.0,
                     'type': 'debit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'service_id': 124}),
                     'amount': 60.0,
                     'type': 'credit'},
                    {'loc': gen_loc('promocodes',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'region': ''}),
                     'amount': 500.0,
                     'type': 'debit'},
                ]
            }
        ],
        {
            'contract_states': {
                '666': {
                    'netting_done': True,
                    'contract_external_id': 'test_general',
                    'payment_amount': 60.0,
                    'payment_currency': 'RUB',
                    'invoices': [
                        {'id': 1, 'external_id': 'LST-1', 'operation_type': 'INSERT_NETTING', 'amount': 2740.0},
                        {'id': 2, 'external_id': 'LST-2', 'operation_type': 'FUEL_HOLD', 'amount': 100.0}
                    ],
                },
            },
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 500.0,
                                'account': 'promocodes',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 416.666667,
                                'account': 'commissions',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 500.0,
                                'account': 'commissions_with_vat',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 2283.333333,
                                'account': 'commissions',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 2740.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 2740.0,
                                'account': 'commissions_with_vat',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'commission_to_compensations_and_cashless'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_camera'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 100.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 100.0,
                                'account': 'fuel_hold',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'fuel'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_car_and_lightbox'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 60.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 60.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'payout_acc'
                    }
                ]
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # complex netting of all the general contract products with payout end

    # all the general contract products with payout and no netting begin
    [
        (
            gen_account('promocodes', CLIENT_ID, GENERAL_CONTRACT_ID, credit='500'),
            gen_account('commissions', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2700'),
            gen_account('commissions_with_vat', CLIENT_ID, GENERAL_CONTRACT_ID, debit='3240'),
            gen_account('fuel_hold', CLIENT_ID, GENERAL_CONTRACT_ID, credit='100'),
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='4900'),
            gen_account('compensations', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000'),
            gen_account('cashless_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='1000'),
            gen_account('compensations_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2000'),
        ),
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID,
                services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID],
                netting=False
            )
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('compensations', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('compensations_refunds', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'credit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('compensations_refunds', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'credit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {
                        'loc': gen_loc(
                            'cashless_refunds',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'terminal_id': 0
                            }
                        ),
                        'amount': 1000.0,
                        'type': 'credit'
                    },
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 100.0,
                     'type': 'debit'},
                    {'loc': gen_loc('fuel_hold', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 100.0,
                     'type': 'debit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'service_id': 124}),
                     'amount': 2800.0,
                     'type': 'credit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 2800.0,
                     'type': 'debit'},
                ]
            }
        ],
        {
            'contract_states': {
                '666': {
                    'netting_done': True,
                    'contract_external_id': 'test_general',
                    'payment_amount': 2800.0,
                    'payment_currency': 'RUB',
                    'invoices': [{'id': 2, 'external_id': 'LST-2', 'operation_type': 'FUEL_HOLD', 'amount': 100.0}],
                },
            },
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_compensations_and_cashless'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_camera'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 1000.0,
                                'account': 'compensations',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 100.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 100.0,
                                'account': 'fuel_hold',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'fuel'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_car_and_lightbox'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 2800.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 2800.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'payout_acc'
                    }
                ]
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # all the general contract products with payout and no netting end

    # complex netting of all the general contract products with zero payment sum and no payout begin
    [
        [
            gen_account('promocodes', CLIENT_ID, GENERAL_CONTRACT_ID, credit='500'),
            gen_account('commissions', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2700'),
            gen_account('commissions_with_vat', CLIENT_ID, GENERAL_CONTRACT_ID, debit='3240'),
            gen_account('fuel_hold', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000'),
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='4900'),
            gen_account('compensations', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000'),
            gen_account('cashless_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='1000'),
            gen_account('compensations_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2000')
        ],
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 2740.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 160.0,
                     'type': 'debit'},
                    {
                        'loc': gen_loc(
                            'cashless_refunds',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'terminal_id': 0
                            }
                        ),
                        'amount': 1000.0,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 416.666667,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 2283.333333,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions_with_vat',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 500.0,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions_with_vat',
                            **{
                                'client_id': 1,
                                'contract_id': 666,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 2740.0,
                        'type': 'credit'
                    },
                    {'loc': gen_loc('compensations', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('compensations_refunds', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'credit'},
                    {'loc': gen_loc('compensations_refunds', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'credit'},
                    {'loc': gen_loc('fuel_hold', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': -840.0,
                     'type': 'credit'},
                    {'loc': gen_loc('fuel_hold', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB'}),
                     'amount': 160.0,
                     'type': 'debit'},
                    {'loc': gen_loc('promocodes',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'region': ''}),
                     'amount': 500.0,
                     'type': 'debit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'service_id': 124}),
                     'amount': 0.0,
                     'type': 'credit'},
                ]
            }
        ],
        {
            'contract_states': {
                '666': {
                    'netting_done': True,
                    'contract_external_id': 'test_general',
                    'payment_amount': 0.0,
                    'payment_currency': 'RUB',
                    'invoices': [
                        {'id': 1, 'external_id': 'LST-1', 'operation_type': 'INSERT_NETTING', 'amount': 2740.0},
                        {'id': 2, 'external_id': 'LST-2', 'operation_type': 'FUEL_HOLD', 'amount': 160.0}],
                },
            },
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 500.0,
                                'account': 'promocodes',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 416.666667,
                                'account': 'commissions',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 500.0,
                                'account': 'commissions_with_vat',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 2283.333333,
                                'account': 'commissions',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 2740.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 2740.0,
                                'account': 'commissions_with_vat',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'commission_to_compensations_and_cashless'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_camera'
                    },
                    {
                        'expressions': [
                            {
                                'amount': -840.0,
                                'account': 'fuel_hold',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 160.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 160.0,
                                'account': 'fuel_hold',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'fuel'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_car_and_lightbox'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'payout_acc'
                    }
                ]
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # complex netting of all the general contract products with zero payment sum and no payout end

    # promocodes with more refunds
    [
        [
            gen_account('promocodes', CLIENT_ID, GENERAL_CONTRACT_ID, credit='500'),
            gen_account('promocodes_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='700'),
            gen_account('commissions', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2700'),
            gen_account('commissions_with_vat', CLIENT_ID, GENERAL_CONTRACT_ID, debit='3240'),
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='4900'),
        ],
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('promocodes',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'region': ''}),
                     'amount': 500.0,
                     'type': 'debit'},
                    {'loc': gen_loc('promocodes_refunds',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'region': ''}),
                     'amount': 500.0,
                     'type': 'credit'},
                    {'loc': gen_loc('commissions', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB',
                                                      'detailed_product': '', 'product': '', 'region': ''
                                                      }
                                    ),
                     'amount': 2700.0,
                     'type': 'credit'},
                    {'loc': gen_loc('commissions_with_vat', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB',
                                                               'detailed_product': '', 'product': '', 'region': ''
                                                               }
                                    ),
                     'amount': 3240.0,
                     'type': 'credit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 3240.0,
                     'type': 'debit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'service_id': 124}),
                     'amount': 1660.0,
                     'type': 'credit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 1660.0,
                     'type': 'debit'},
                ]
            }
        ],
        {
            'contract_states': {
                '666': {
                    'netting_done': True,
                    'contract_external_id': 'test_general',
                    'payment_amount': 1660.0,
                    'payment_currency': 'RUB',
                    'invoices': [
                        {'id': 1, 'external_id': 'LST-1', 'operation_type': 'INSERT_NETTING', 'amount': 3240.0},
                    ],
                },
            },
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 500.0,
                                'account': 'promocodes',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 500.0,
                                'account': 'promocodes_refunds',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 2700.0,
                                'account': 'commissions',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 3240.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 3240.0,
                                'account': 'commissions_with_vat',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'commission_to_compensations_and_cashless'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_camera'
                    },
                    {
                        'expressions': [],
                        'name': 'fuel'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_car_and_lightbox'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 1660.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1660.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'payout_acc'
                    }
                ]
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # promocodes with more refunds end

    # commissions with more refunds
    [
        [
            gen_account('commissions', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2700'),
            gen_account('commissions_with_vat', CLIENT_ID, GENERAL_CONTRACT_ID, debit='3240'),
            gen_account('commissions_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, credit='3700'),
            gen_account('commissions_refunds_with_vat', CLIENT_ID, GENERAL_CONTRACT_ID, credit='4440'),
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='4900'),
        ],
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('commissions', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB',
                                                      'detailed_product': '', 'product': '', 'region': ''
                                                      }
                                    ),
                     'amount': 2700.0,
                     'type': 'credit'},
                    {'loc': gen_loc('commissions_with_vat', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB',
                                                               'detailed_product': '', 'product': '', 'region': ''
                                                               }
                                    ),
                     'amount': 3240.0,
                     'type': 'credit'},
                    {'loc': gen_loc('commissions_refunds', **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB',
                                                              'detailed_product': '', 'product': '', 'region': ''
                                                              }
                                    ),
                     'amount': 2700.0,
                     'type': 'debit'},
                    {'loc': gen_loc('commissions_refunds_with_vat',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB',
                                       'detailed_product': '', 'product': '', 'region': ''
                                       }
                                    ),
                     'amount': 3240.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'terminal_id': 0}),
                     'amount': 4900.0,
                     'type': 'debit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': 1, 'contract_id': 666, 'currency': 'RUB', 'service_id': 124}),
                     'amount': 4900.0,
                     'type': 'credit'},
                ]
            }
        ],
        {
            'contract_states': {
                '666': {
                    'netting_done': True,
                    'contract_external_id': 'test_general',
                    'payment_amount': 4900.0,
                    'payment_currency': 'RUB',
                },
            },
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [
                            {
                                'amount': 2700.0,
                                'account': 'commissions',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 3240.0,
                                'account': 'commissions_with_vat',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 2700.0,
                                'account': 'commissions_refunds',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 3240.0,
                                'account': 'commissions_refunds_with_vat',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [],
                        'name': 'commission_to_compensations_and_cashless'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_camera'
                    },
                    {
                        'expressions': [],
                        'name': 'fuel'
                    },
                    {
                        'expressions': [],
                        'name': 'transfer_for_car_and_lightbox'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 4900.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 4900.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'payout_acc'
                    }
                ]
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': 1})},
         {'state': {}, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # commissions with more refunds end

    # complex netting of all the general contract products with payout begin and transfer events
    [
        [
            gen_account('promocodes', CLIENT_ID, GENERAL_CONTRACT_ID, credit='500'),
            gen_account('commissions', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2700'),
            gen_account('commissions_with_vat', CLIENT_ID, GENERAL_CONTRACT_ID, debit='3240'),
            gen_account('fuel_hold', CLIENT_ID, GENERAL_CONTRACT_ID, credit='100'),
            gen_account('cashless', CLIENT_ID, GENERAL_CONTRACT_ID, credit='4900'),
            gen_account('compensations', CLIENT_ID, GENERAL_CONTRACT_ID, credit='1000'),
            gen_account('cashless_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='1000'),
            gen_account('compensations_refunds', CLIENT_ID, GENERAL_CONTRACT_ID, debit='2000'),
            gen_account('transfer_source_selfemployed_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_hold_selfemployed_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('foreign_income_selfemployed_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_source_lightbox_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_hold_lightbox_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('foreign_income_lightbox_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_source_signalq_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
            gen_account('transfer_hold_signalq_rent', CLIENT_ID, GENERAL_CONTRACT_ID, debit='0'),
        ],
        [
            gen_general_contract(
                GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
            )
        ],
        [
            {
                'loc': gen_loc('transfer_queue_state', **{'client_id': 1}),
                'state': {
                    '0': {
                        str(GENERAL_CONTRACT_ID): [
                            gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID,
                                               CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                                               transaction_id='1001', amount='30.0',
                                               transfer_type='selfemployed_rent',
                                               dt=datetime(2021, 6, 21, 3),
                                               remains='30.0',
                                               ),
                            gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID,
                                               CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                                               transaction_id='1002', amount='70.0',
                                               transfer_type='selfemployed_rent',
                                               dt=datetime(2021, 6, 22, 3),
                                               remains='70.0',
                                               ),
                        ],
                    },
                    '1': {
                        str(GENERAL_CONTRACT_ID): [
                            gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID,
                                               CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                                               transaction_id='1000', amount='10.0',
                                               transfer_type='signalq_rent',
                                               dt=datetime(2021, 6, 20, 3),
                                               remains='10.0',
                                               ),
                        ]
                    }
                }
            }
        ],
        True,
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('cashless',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'terminal_id': 0}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'terminal_id': 0}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'terminal_id': 0}),
                     'amount': 2740.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'terminal_id': 0}),
                     'amount': 10.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'terminal_id': 0}),
                     'amount': 100.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'terminal_id': 0}),
                     'amount': 30.0,
                     'type': 'debit'},
                    {'loc': gen_loc('cashless',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'terminal_id': 0}),
                     'amount': 20.0,
                     'type': 'debit'},
                    {
                        'loc': gen_loc('cashless_refunds', **{
                            'client_id': CLIENT_ID,
                            'contract_id': GENERAL_CONTRACT_ID,
                            'currency': 'RUB',
                            'terminal_id': 0
                        }),
                        'amount': 1000.0,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions',
                            **{
                                'client_id': CLIENT_ID,
                                'contract_id': GENERAL_CONTRACT_ID,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 416.666667,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions',
                            **{
                                'client_id': CLIENT_ID,
                                'contract_id': GENERAL_CONTRACT_ID,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 2283.333333,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions_with_vat',
                            **{
                                'client_id': CLIENT_ID,
                                'contract_id': GENERAL_CONTRACT_ID,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 500.0,
                        'type': 'credit'
                    },
                    {
                        'loc': gen_loc(
                            'commissions_with_vat',
                            **{
                                'client_id': CLIENT_ID,
                                'contract_id': GENERAL_CONTRACT_ID,
                                'currency': 'RUB',
                                'detailed_product': '',
                                'product': '',
                                'region': ''
                            }
                        ),
                        'amount': 2740.0,
                        'type': 'credit'
                    },
                    {'loc': gen_loc('compensations',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'debit'},
                    {'loc': gen_loc('compensations_refunds',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'credit'},
                    {'loc': gen_loc('compensations_refunds',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': 1000.0,
                     'type': 'credit'},
                    {'loc': gen_loc('fuel_hold',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': 100.0,
                     'type': 'debit'},
                    {'loc': gen_loc('payout',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'service_id': CASHLESS_SID}),
                     'amount': 0.0,
                     'type': 'credit'},
                    {'loc': gen_loc('promocodes',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB',
                                       'region': ''}),
                     'amount': 500.0,
                     'type': 'debit'},
                    {'loc': gen_loc('transfer_hold_signalq_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 10.0,
                     'type': 'credit'},
                    {'loc': gen_loc('transfer_hold_selfemployed_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 30.0,
                     'type': 'credit'},
                    {'loc': gen_loc('transfer_hold_selfemployed_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 20.0,
                     'type': 'credit'},
                    {'loc': gen_loc('transfer_hold_signalq_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 10.0,
                     'type': 'debit'},
                    {'loc': gen_loc('transfer_hold_selfemployed_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 30.0,
                     'type': 'debit'},
                    {'loc': gen_loc('transfer_hold_selfemployed_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 20.0,
                     'type': 'debit'},
                    {'loc': gen_loc('transfer_source_signalq_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 10.0,
                     'type': 'credit'},
                    {'loc': gen_loc('transfer_source_selfemployed_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 30.0,
                     'type': 'credit'},
                    {'loc': gen_loc('transfer_source_selfemployed_rent',
                                    **{'client_id': CLIENT_ID,
                                       'contract_id': GENERAL_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 20.0,
                     'type': 'credit'},
                ]
            },
            {
                'client_id': CLIENT_SD_ID,
                'transactions': [
                    {'loc': gen_loc('payout', **{'client_id': CLIENT_SD_ID,
                                                 'contract_id': GENERAL_SD_CONTRACT_ID,
                                                 'currency': 'RUB',
                                                 'service_id': REQUEST_SID}),
                     'amount': 30.0,
                     'type': 'credit'},
                    {'loc': gen_loc('payout', **{'client_id': CLIENT_SD_ID,
                                                 'contract_id': GENERAL_SD_CONTRACT_ID,
                                                 'currency': 'RUB',
                                                 'service_id': REQUEST_SID}),
                     'amount': 20.0,
                     'type': 'credit'},
                    {'loc': gen_loc('foreign_income_selfemployed_rent',
                                    **{'client_id': CLIENT_SD_ID,
                                       'contract_id': GENERAL_SD_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 30.0,
                     'type': 'debit'},
                    {'loc': gen_loc('foreign_income_selfemployed_rent',
                                    **{'client_id': CLIENT_SD_ID,
                                       'contract_id': GENERAL_SD_CONTRACT_ID,
                                       'currency': 'RUB'}),
                     'amount': 20.0,
                     'type': 'debit'},
                ]
            },
        ],
        {
            'contract_states': {
                '666': {
                    'netting_done': True,
                    'contract_external_id': 'test_general',
                    'payment_amount': 0.0,
                    'payment_currency': 'RUB',
                    'invoices': [
                        {'id': 1, 'external_id': 'LST-1', 'operation_type': 'INSERT_NETTING', 'amount': 2740.0},
                        {'id': 2, 'external_id': 'LST-2', 'operation_type': 'FUEL_HOLD', 'amount': 100.0}
                    ],
                },
            },
            'operation_expressions': {
                '666': [
                    {
                        'expressions': [],
                        'name': 'commissions'
                    },
                    {
                        'expressions': [],
                        'name': 'promocodes'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 500.0,
                                'account': 'promocodes',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 416.666667,
                                'account': 'commissions',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 500.0,
                                'account': 'commissions_with_vat',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'commission_to_promocode_actless'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 2283.333333,
                                'account': 'commissions',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'compensations_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 1000.0,
                                'account': 'cashless_refunds',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 2740.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 2740.0,
                                'account': 'commissions_with_vat',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'commission_to_compensations_and_cashless'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 10.0,
                                'account': 'transfer_source_signalq_rent',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 10.0,
                                'account': 'transfer_hold_signalq_rent',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 10.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 10.0,
                                'account': 'transfer_hold_signalq_rent',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'transfer_for_camera'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 100.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 100.0,
                                'account': 'fuel_hold',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'fuel'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 30.0,
                                'account': 'transfer_source_selfemployed_rent',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 30.0,
                                'account': 'transfer_hold_selfemployed_rent',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 30.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 30.0,
                                'account': 'transfer_hold_selfemployed_rent',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 30.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 30.0,
                                'account': 'foreign_income_selfemployed_rent',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 20.0,
                                'account': 'transfer_source_selfemployed_rent',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 20.0,
                                'account': 'transfer_hold_selfemployed_rent',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 20.0,
                                'account': 'cashless',
                                'operation_type': 'DEBIT'
                            },
                            {
                                'amount': 20.0,
                                'account': 'transfer_hold_selfemployed_rent',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 20.0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            },
                            {
                                'amount': 20.0,
                                'account': 'foreign_income_selfemployed_rent',
                                'operation_type': 'DEBIT'
                            }
                        ],
                        'name': 'transfer_for_car_and_lightbox'
                    },
                    {
                        'expressions': [
                            {
                                'amount': 0,
                                'account': 'payout',
                                'operation_type': 'CREDIT'
                            }
                        ],
                        'name': 'payout_acc'
                    }
                ]
            }
        },
        [{'state': COMMON_DT_TS, 'loc': gen_loc('cutoff_dt_state', **{'client_id': CLIENT_ID})},
         {'state': {
             '0': {
                 str(GENERAL_CONTRACT_ID): [
                     gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID,
                                        CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                                        transaction_id='1002', amount='70.0',
                                        transfer_type='selfemployed_rent',
                                        dt=datetime(2021, 6, 22, 3),
                                        remains=50.0,
                                        ),
                 ]
             },
             '1': {}
         }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
    ],
    # complex netting of all the general contract products with payout end
]


class TestPayoutEvents:
    @pytest.mark.asyncio
    @pytest.mark.parametrize('accounts,contracts,transfer_queue_states,migrated,client_transactions,payload,states',
                             params)
    async def test_payout(self, make_request, accounts, contracts, transfer_queue_states, migrated,
                          client_transactions, payload, states):
        event = gen_payout_event(CLIENT_ID, dt=COMMON_DT)
        dt = COMMON_DT if migrated else COMMON_DT + timedelta(seconds=1)

        dry_run = False
        jval = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': dt.isoformat(), 'dry_run': dry_run}],
                'contracts': contracts,
                'personal_accounts': [
                    gen_personal_account(
                        1, CLIENT_ID, GENERAL_CONTRACT_ID, 'LST-1', service_code=ServiceCode.YANDEX_SERVICE
                    ),
                    gen_personal_account(
                        2, CLIENT_ID, GENERAL_CONTRACT_ID, 'LST-2', service_code=ServiceCode.DEPOSITION
                    ),
                ],
                'accounts': {'balances': accounts},
                'lock': {'states': transfer_queue_states},
            },
        }
        res = (await make_request('payout', jval))['data']

        assert_that(res, has_key('event'))
        assert_that(res['event'], has_entries(event))
        assert_that(res['event']['tariffer_payload'], has_entries({**payload,
                                                                   **{'common_ts': COMMON_DT_TS, 'dry_run': dry_run}}))
        assert res['states'] == states
        assert_that(res['client_transactions'],
                    contains_inanyorder(*expected_client_transactions(client_transactions, {'dt': COMMON_DT_TS})))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('service_code', [None, ''])
    async def test_payout_netting_in_the_past(self, service_code, make_request):
        event = gen_payout_event(CLIENT_ID, dt=COMMON_DT)

        dry_run = False
        jval = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': COMMON_DT.isoformat(), 'dry_run': dry_run}],
                'contracts': [
                    gen_general_contract(
                        GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
                    )
                ],
                'accounts': {'balances': None},
                'personal_accounts': [
                    gen_personal_account(1, CLIENT_ID, GENERAL_CONTRACT_ID, 'LST-1', service_code=service_code)
                ],
                'lock': {
                    'states': [
                        {'state': COMMON_DT_TS + 1, 'loc': {'client_id': CLIENT_ID, 'type': 'cutoff_dt_state'}}
                    ]
                },
            }
        }

        response = await make_request('payout', jval)
        assert response == {
            'code': 400,
            'status': 'fail',
            'data': {
                'message': 'NETTING_IN_THE_PAST',
                'params': {'desired_netting_dt': '2020-01-01T10:54:00+00:00',
                           'cutoff_dt': '2020-01-01T10:54:01+00:00'}
            }
        }
