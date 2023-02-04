import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from billing.hot.calculators.taxi.calculator.core.constants import (
    CARD_COMM_SID, CASH_COMM_SID, CASHLESS_SID, LOGISTICS_SID, REVENUE_PROMO_PRODUCTS, SUBVENTION_SID
)
from billing.hot.calculators.taxi.calculator.tests.builder import (
    gen_cashless_event, gen_firm, gen_general_contract, gen_loc, gen_partner_product, gen_revenue_event,
    gen_spendable_contract, gen_subvention_event, gen_thirdparty_event
)
from billing.hot.calculators.taxi.calculator.tests.const import (
    CLIENT_ID, COMMON_DT, COMMON_DT_TS, GENERAL_CONTRACT_ID, PERSON_ID, SPENDABLE_CONTRACT_ID, TAXI_RU_FIRM
)

from . import expected_client_transactions


class TestStreamEvents:
    @pytest.mark.asyncio
    async def test_lock_timestamp(self, make_request):
        event = gen_subvention_event(
            client_id=CLIENT_ID,
            dt=COMMON_DT,
            contract_id=SPENDABLE_CONTRACT_ID,
            transaction_type='payment',
            amount=100,
            ignore_in_balance=False
        )

        dry_run = False
        request = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': COMMON_DT.isoformat(), 'dry_run': dry_run}],
                'contracts': [
                    gen_spendable_contract(SPENDABLE_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[SUBVENTION_SID])
                ],
                'lock': {
                    'states': [{'state': COMMON_DT_TS + 1, 'loc': {'client_id': CLIENT_ID, 'type': 'cutoff_dt_state'}}]
                },
            }
        }
        response = (await make_request('subvention', request))['data']

        assert_that(response['event'], has_entries(event))
        assert_that(response['event']['tariffer_payload'], has_entries({'common_ts': COMMON_DT_TS + 1,
                                                                        'contract_external_id': 'test_spendable',
                                                                        'dry_run': dry_run,
                                                                        }))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'service_id,product,amount,ignore_in_balance,transaction_type,client_transactions',
        [
            [SUBVENTION_SID, 'subsidy', 100.0, True, 'payment', []],
            [
                SUBVENTION_SID, 'subsidy', 100.0, False, 'payment',
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc(
                                    'subventions',
                                    **{
                                        'client_id': CLIENT_ID,
                                        'contract_id': 667,
                                        'currency': 'RUB',
                                        'product': 'subsidy',
                                        'detailed_product': 'subsidy_commission',
                                        'region': 'MSKc'
                                    }
                                ),
                                'amount': 100.0,
                                'type': 'credit',
                            },
                        ]
                    }
                ]
            ],
            [
                SUBVENTION_SID, 'subsidy', 100.0, False, 'refund',
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc(
                                    'subventions_refunds',
                                    **{
                                        'client_id': CLIENT_ID,
                                        'contract_id': 667,
                                        'currency': 'RUB',
                                        'product': 'subsidy',
                                        'detailed_product': 'subsidy_commission',
                                        'region': 'MSKc'
                                    }
                                ),
                                'amount': 100.0,
                                'type': 'debit',
                            }
                        ]
                    }
                ],
            ],
            [
                LOGISTICS_SID, 'delivery_park_b2b_logistics_payment', 100.0, False, 'payment',
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc(
                                    'logistics',
                                    **{
                                        'client_id': CLIENT_ID,
                                        'contract_id': 667,
                                        'currency': 'RUB',
                                        'product': 'delivery_park_b2b_logistics_payment',
                                        'detailed_product': 'subsidy_commission',
                                        'region': 'MSKc'
                                    }
                                ),
                                'amount': 100.0,
                                'type': 'credit',
                            }
                        ],
                    }
                ]
            ]
        ]
    )
    async def test_spendable(self, service_id, product, amount, ignore_in_balance, transaction_type,
                             client_transactions, make_request):
        event = gen_subvention_event(
            client_id=CLIENT_ID,
            dt=COMMON_DT,
            service_id=service_id,
            product=product,
            contract_id=SPENDABLE_CONTRACT_ID,
            transaction_type=transaction_type,
            amount=amount,
            ignore_in_balance=ignore_in_balance
        )

        dry_run = True
        request = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': COMMON_DT.isoformat(), 'dry_run': dry_run}],
                'contracts': [
                    gen_spendable_contract(SPENDABLE_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[service_id])
                ],
                'firm': gen_firm(TAXI_RU_FIRM, 'some_mdh_id')
            }
        }

        response = (await make_request('subvention', request))['data']
        assert_that(response['event'], has_entries(event))
        assert_that(response['event']['tariffer_payload'], has_entries({
            'common_ts': COMMON_DT_TS,
            'contract_external_id': 'test_spendable',
            'dry_run': dry_run,
        }))

        if not ignore_in_balance:
            assert_that(
                response['event']['tariffer_payload'],
                has_entries({
                    'firm_id': TAXI_RU_FIRM,
                    'tax_policy_id': 1,
                    'tax_policy_pct': {
                        'hidden': 0,
                        'dt': 1577836800,
                        'id': 1,
                        'mdh_id': 'a1',
                        'nds_pct': 18.0,
                        'nsp_pct': 0.0,
                        'sum_taxes': 18.0
                    }
                })
            )

        assert_that(
            response['client_transactions'],
            contains_inanyorder(*expected_client_transactions(client_transactions, {'dt': COMMON_DT_TS}))
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'product,amount,ignore_in_balance,aggregation_sign,client_transactions',
        [
            ['order', 100.01, True, 1, []],
            [
                'coupon', 100.01, False, 1,
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('promocodes', **{
                                    'client_id': CLIENT_ID,
                                    'contract_id': 666,
                                    'currency': 'RUB',
                                    'region': 'KOS'
                                }),
                                'amount': 100.01, 'type': 'credit',
                            }
                        ]
                    }
                ]
            ],
            [
                'subvention', 100.01, False, -1,
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('promocodes_refunds', **{
                                    'client_id': CLIENT_ID,
                                    'contract_id': 666,
                                    'currency': 'RUB',
                                    'region': 'KOS'
                                }),
                                'amount': 100.01, 'type': 'debit',
                            }
                        ]
                    }
                ]
            ],
            [
                'order', 100.01, False, 1,
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('commissions', **{
                                    'client_id': CLIENT_ID,
                                    'contract_id': 666,
                                    'currency': 'RUB',
                                    'product': 'order',
                                    'detailed_product': 'gross_taximeter_payment',
                                    'region': 'KOS'
                                }),
                                'amount': 100.01,
                                'type': 'debit',
                            },
                            {
                                'loc': gen_loc('commissions_with_vat', **{
                                    'client_id': CLIENT_ID,
                                    'contract_id': 666,
                                    'currency': 'RUB',
                                    'product': 'order',
                                    'detailed_product': 'gross_taximeter_payment',
                                    'region': 'KOS'
                                }),
                                'amount': 118.0118,
                                'type': 'debit',
                            },
                        ]
                    }
                ]
            ],
            [
                'order', 100.01, False, -1,
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('commissions_refunds', **{
                                    'client_id': CLIENT_ID,
                                    'contract_id': 666,
                                    'currency': 'RUB',
                                    'product': 'order',
                                    'detailed_product': 'gross_taximeter_payment',
                                    'region': 'KOS'
                                }),
                                'amount': 100.01,
                                'type': 'credit',
                            },
                            {
                                'loc': gen_loc('commissions_refunds_with_vat', **{
                                    'client_id': CLIENT_ID,
                                    'contract_id': 666,
                                    'currency': 'RUB',
                                    'product': 'order',
                                    'detailed_product': 'gross_taximeter_payment',
                                    'region': 'KOS'
                                }),
                                'amount': 118.0118,
                                'type': 'credit',
                            },
                        ]
                    }
                ]
            ]
        ]
    )
    async def test_revenue(self, product, amount, ignore_in_balance, aggregation_sign, client_transactions,
                           make_request):
        event = gen_revenue_event(client_id=CLIENT_ID, contract_id=GENERAL_CONTRACT_ID, product=product,
                                  amount=amount, dt=COMMON_DT,
                                  aggregation_sign=aggregation_sign,
                                  ignore_in_balance=ignore_in_balance)
        dry_run = False
        product_mdh_id = 'product_mdh_id_66'
        request = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': None, 'object_id': None,
                                    'from_dt': COMMON_DT.isoformat(), 'dry_run': dry_run},
                                   {'namespace': 'weird', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': COMMON_DT.isoformat(), 'dry_run': dry_run}
                                   ],
                'contracts': [
                    gen_general_contract(
                        GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
                    ),
                ],
                'firm': gen_firm(TAXI_RU_FIRM, 'some_mdh_id'),
                'partner_products': [gen_partner_product(product, CASH_COMM_SID, 'RUB', product_mdh_id)]
            }
        }

        response = (await make_request('revenue', request))['data']
        assert_that(response['event'], has_entries(event))
        assert_that(response['event']['tariffer_payload'], has_entries({'common_ts': COMMON_DT_TS,
                                                                        'contract_external_id': 'test_general',
                                                                        'dry_run': dry_run,
                                                                        }))
        if not ignore_in_balance and product not in REVENUE_PROMO_PRODUCTS:
            assert_that(response['event']['tariffer_payload'], has_entries({'firm_id': TAXI_RU_FIRM,
                                                                            'product_mdh_id': product_mdh_id,
                                                                            'tax_policy_id': 1,
                                                                            'tax_policy_pct': {'hidden': 0,
                                                                                               'dt': 1577836800,
                                                                                               'id': 1,
                                                                                               'mdh_id': 'a1',
                                                                                               'nds_pct': 18.0,
                                                                                               'nsp_pct': 0.0,
                                                                                               'sum_taxes': 18.0
                                                                                               }
                                                                            }))
        assert_that(response['client_transactions'],
                    contains_inanyorder(*expected_client_transactions(client_transactions, {'dt': COMMON_DT_TS})))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'product,transaction_type,amount,client_transactions',
        [
            [
                'trip_payment', 'payment', 100.01,
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('cashless', **{
                                    'client_id': CLIENT_ID,
                                    'contract_id': 666,
                                    'currency': 'RUB',
                                    'terminal_id': 123
                                }),
                                'amount': 100.01,
                                'type': 'credit'
                            }
                        ]
                    }
                ]
            ],
            [
                'trip_payment', 'refund', -100.01,
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('cashless_refunds', **{
                                    'client_id': CLIENT_ID,
                                    'contract_id': 666,
                                    'currency': 'RUB',
                                    'terminal_id': 123
                                }),
                                'amount': 100.01,
                                'type': 'debit'
                            }
                        ]
                    }
                ]
            ],
            [
                'trip_compensation', 'payment', 100.01,
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('compensations',
                                               **{'client_id': CLIENT_ID, 'contract_id': 666, 'currency': 'RUB'}),
                                'amount': 100.01,
                                'type': 'credit'
                            }
                        ]
                    }
                ]
            ],
        ]
    )
    async def test_cashless(self, product, transaction_type, amount, client_transactions, make_request):
        event = gen_cashless_event(
            client_id=CLIENT_ID, contract_id=GENERAL_CONTRACT_ID, product=product,
            transaction_type=transaction_type, amount=amount,
            terminal_id=123, dt=COMMON_DT
        )

        dry_run = False
        request = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': COMMON_DT.isoformat(), 'dry_run': dry_run}],
                'contracts': [
                    gen_general_contract(
                        GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
                    ),
                ]
            }
        }

        response = (await make_request('cashless', request))['data']

        assert_that(response['event'], has_entries(event))
        assert_that(response['event']['tariffer_payload'], has_entries({'common_ts': COMMON_DT_TS,
                                                                        'contract_external_id': 'test_general',
                                                                        'dry_run': dry_run,
                                                                        }))
        assert_that(response['client_transactions'],
                    contains_inanyorder(*expected_client_transactions(client_transactions, {'dt': COMMON_DT_TS})))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'payment_type,paysys_type,client_transactions',
        [
            [
                'deposit',
                'fuel_hold',
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('fuel_hold',
                                               **{'client_id': CLIENT_ID, 'contract_id': 666, 'currency': 'RUB'}),
                                'amount': 100.0,
                                'type': 'credit'
                            }
                        ]
                    }
                ]
            ],
            [
                'refuel',
                'fuel_fact',
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('fuel_fact',
                                               **{'client_id': CLIENT_ID, 'contract_id': 666, 'currency': 'RUB'}),
                                'amount': 100.0,
                                'type': 'credit'
                            }
                        ]
                    }
                ]
            ],
            [
                'deposit_payout',
                'fuel_hold_payment',
                [
                    {
                        'client_id': CLIENT_ID,
                        'transactions': [
                            {
                                'loc': gen_loc('fuel_hold',
                                               **{'client_id': CLIENT_ID, 'contract_id': 666, 'currency': 'RUB'}),
                                'amount': 100.0,
                                'type': 'debit'
                            },
                            {
                                'loc': gen_loc('cashless',
                                               **{'client_id': CLIENT_ID, 'contract_id': 666, 'currency': 'RUB',
                                                  'terminal_id': 0}),
                                'amount': 100.0,
                                'type': 'credit'
                            }
                        ]
                    }
                ]
            ],
        ]
    )
    async def test_fuel(self, payment_type, paysys_type, client_transactions, make_request):
        event = gen_thirdparty_event(
            client_id=CLIENT_ID,
            contract_id=GENERAL_CONTRACT_ID,
            payment_type=payment_type,
            paysys_type=paysys_type,
            amount=100.0,
            dt=COMMON_DT
        )

        dry_run = True
        request = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': COMMON_DT.isoformat(), 'dry_run': dry_run}],
                'contracts': [
                    gen_general_contract(
                        GENERAL_CONTRACT_ID, CLIENT_ID, PERSON_ID, services=[CASH_COMM_SID, CASHLESS_SID, CARD_COMM_SID]
                    ),
                ]
            }
        }

        response = (await make_request('fuel', request))['data']

        del event['client_id']
        assert_that(response['event'], has_entries(event))
        assert_that(response['event']['tariffer_payload'], has_entries({'common_ts': COMMON_DT_TS,
                                                                        'contract_external_id': 'test_general',
                                                                        'dry_run': dry_run,
                                                                        }))
        assert_that(response['client_transactions'],
                    contains_inanyorder(*expected_client_transactions(client_transactions, {'dt': COMMON_DT_TS})))
