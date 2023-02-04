import typing as tp
from datetime import datetime
from decimal import Decimal

import arrow
import hamcrest as hm
import pytest
from billing.library.python.calculator import exceptions as exc
from billing.library.python.calculator.models import transaction as tx
from billing.library.python.calculator.models import tax
from dateutil.tz import tzutc

from billing.hot.calculators.bnpl_income.calculator.core import models
from billing.hot.calculators.bnpl_income.calculator.faas import schemas

_DATETIME_FMT = '%Y-%m-%dT%H:%M:%SZ'


class TestLoadCommissionMethodSchema:
    dt = datetime(2022, 3, 21, 13, 32, 20, tzinfo=tzutc())

    testcases = [
        pytest.param(
            {
                'event': {
                    'transaction_amount': '123.13',
                    'transaction_dt': dt.strftime(_DATETIME_FMT),
                    'transaction_id': 'o28391jk209213',
                    'transaction_type': 'payment',
                    'billing_client_id': 12,
                    'billing_contract_id': 132,
                    'currency': 'RUB',
                    'product_id': 'foo-bar-baz',
                },
                'references': {
                    'contracts': [],
                    'migration_info': [
                        {
                            'namespace': 'bnpl_income',
                            'filter': 'Client',
                            'object_id': 1,
                            'from_dt': dt.strftime(_DATETIME_FMT),
                            'dry_run': True,
                        }
                    ],
                    'firm': {
                        'id': 12,
                        'mdh_id': 'mdh_id',
                        'title': 'Some firm',
                        'legal_address': 'Some legal address',
                        'inn': '1234567891',
                        'kpp': '123456789',
                        'email': 'some@email.ru',
                        'mnclose_email': 'mnclose@email.ru',
                        'payment_invoice_email': 'payment_invoice_email@email.ru',
                        'phone': '+79087813132',
                        'region_id': 31,
                        'currency_rate_src': 'currency_rate_src',
                        'default_iso_currency': 'default_iso_currency',
                        'pa_prefix': 'pa_prefix',
                        'contract_id': 4,
                        'unilateral': 112,
                        'postpay': 1345,
                        'alter_permition_code': 'alter_permition_code',
                        'test_env': 1,
                        'tax_policies': [
                            {
                                'id': 2,
                                'name': 'tax policy',
                                'hidden': 1,
                                'resident': 1,
                                'region_id': 31,
                                'default_tax': 1,
                                'mdh_id': 'mdh_id',
                                'spendable_nds_id': 40,
                                'percents': [
                                    {
                                        'id': 1,
                                        'dt': dt.strftime(_DATETIME_FMT),
                                        'nds_pct': Decimal(13.0),
                                        'nsp_pct': Decimal(10.0),
                                        'hidden': 1,
                                        'mdh_id': 'mdh_id',
                                    },
                                ]
                            }
                        ],
                        'person_categories': [
                            {
                                'category': 'category',
                                'is_resident': 1,
                                'is_legal': 1,
                            },
                        ],
                    },
                    'products': {
                        'id': 'mdh',
                    },
                },
            },
            True,
        ),
        pytest.param(
            {
                'event': {
                    'transaction_amount': '123.13',
                    'transaction_dt': dt.strftime(_DATETIME_FMT),
                    'transaction_id': 'o28391jk209213',
                    'transaction_type': 'payment',
                    'billing_client_id': 12,
                    'billing_contract_id': 132,
                    'currency': 'RUB',
                    'product_id': 'foo-bar-baz',
                },
            },
            False,
        ),
        pytest.param(
            {
                'references': {
                    'contracts': [],
                    'migration_info': [
                        {
                            'namespace': 'bnpl_income',
                            'filter': 'Firm',
                            'object_id': 20,
                            'from_dt': dt.strftime(_DATETIME_FMT),
                            'dry_run': True,
                        }
                    ],
                    'firm': {
                        'id': 100,
                        'mdh_id': 'mdh_id',
                        'title': 'Some firm',
                        'legal_address': 'Some legal address',
                        'inn': '1234567891',
                        'kpp': '123456789',
                        'email': 'some@email.ru',
                        'mnclose_email': 'mnclose@email.ru',
                        'payment_invoice_email': 'payment_invoice_email@email.ru',
                        'phone': '+79087813132',
                        'region_id': 31,
                        'currency_rate_src': 'currency_rate_src',
                        'default_iso_currency': 'default_iso_currency',
                        'pa_prefix': 'pa_prefix',
                        'contract_id': 4,
                        'unilateral': 112,
                        'postpay': 1345,
                        'alter_permition_code': 'alter_permition_code',
                        'test_env': 1,
                        'tax_policies': [
                            {
                                'id': 2,
                                'name': 'tax policy',
                                'hidden': 1,
                                'resident': 1,
                                'region_id': 31,
                                'default_tax': 1,
                                'mdh_id': 'mdh_id',
                                'spendable_nds_id': 40,
                                'percents': [
                                    {
                                        'id': 1,
                                        'dt': dt.strftime(_DATETIME_FMT),
                                        'nds_pct': Decimal(13.0),
                                        'nsp_pct': Decimal(10.0),
                                        'hidden': 1,
                                        'mdh_id': 'mdh_id',
                                    },
                                ]
                            }
                        ],
                        'person_categories': [
                            {
                                'category': 'category',
                                'is_resident': 1,
                                'is_legal': 1,
                            },
                        ],
                    },
                    'products': {
                        'id': 'mdh',
                    },
                },
            },
            False,
        )
    ]

    @pytest.mark.parametrize('given, valid', testcases)
    def test_load_commission_method_schema(self, given: tp.Dict, valid: bool):
        if not valid:
            hm.assert_that(
                hm.calling(schemas.load_commission_method_schema).with_args(given),
                hm.raises(exc.LoadMethodError),
            )
        else:
            hm.assert_that(
                hm.calling(schemas.load_commission_method_schema).with_args(given),
                hm.not_(hm.raises(exc.LoadMethodError)),
            )


class TestLoadCommissionEventSchema:
    dt = datetime(2022, 3, 21, 12, 42, 10, tzinfo=tzutc())

    testcases = [
        pytest.param(
            {
                'transaction_amount': '123.13',
                'transaction_dt': dt.strftime(_DATETIME_FMT),
                'transaction_id': 'o28391jk209213',
                'transaction_type': 'payment',
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'currency': 'RUB',
                'product_id': 'foo-bar-baz',
            },
            {
                'transaction_amount': Decimal('123.13'),
                'transaction_dt': dt,
                'transaction_id': 'o28391jk209213',
                'transaction_type': models.TransactionType.PAYMENT,
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'currency': 'RUB',
                'product_id': 'foo-bar-baz',
                'tariffer_payload': {},
                'payload': {},
            },
            True,
        ),
        pytest.param(
            {
                'transaction_amount': '123.13',
                'transaction_dt': dt.strftime(_DATETIME_FMT),
                'transaction_id': 'o28391jk209213',
                'transaction_type': 'payment',
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'product_id': 'foo-bar-baz',
            },
            {
                'transaction_amount': Decimal('123.13'),
                'transaction_dt': dt,
                'transaction_id': 'o28391jk209213',
                'transaction_type': models.TransactionType.PAYMENT,
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'product_id': 'foo-bar-baz',
                'tariffer_payload': {},
                'payload': {},
            },
            True,
        ),
        pytest.param(
            {
                'transaction_amount': '123.13',
                'transaction_dt': dt.strftime(_DATETIME_FMT),
                'transaction_id': 'o28391jk209213',
                'transaction_type': 'payment',
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'currency': 'RUB',
            },
            {
                'transaction_amount': Decimal('123.13'),
                'transaction_dt': dt,
                'transaction_id': 'o28391jk209213',
                'transaction_type': models.TransactionType.PAYMENT,
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'currency': 'RUB',
                'tariffer_payload': {},
                'payload': {},
            },
            True,
        ),
        pytest.param(
            {
                'transaction_amount': '123.13',
                'transaction_dt': dt.strftime(_DATETIME_FMT),
                'transaction_id': 'o28391jk209213',
                'transaction_type': 'pay',
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'currency': 'RUB',
                'product_id': 'foo-bar-baz',
            },
            False,
            None,
        ),
        pytest.param(
            {
                'transaction_dt': dt.strftime(_DATETIME_FMT),
                'transaction_id': 'o28391jk209213',
                'transaction_type': 'payment',
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'currency': 'RUB',
                'product_id': 'foo-bar-baz',
            },
            False,
            None,
        ),
        pytest.param(
            {
                'transaction_amount': '123.13',
                'transaction_id': 'o28391jk209213',
                'transaction_type': 'payment',
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'currency': 'RUB',
                'product_id': 'foo-bar-baz',
            },
            False,
            None,
        ),
        pytest.param(
            {
                'transaction_amount': '123.13',
                'transaction_dt': dt.strftime(_DATETIME_FMT),
                'transaction_id': 'o28391jk209213',
                'billing_client_id': 12,
                'billing_contract_id': 132,
                'currency': 'RUB',
                'product_id': 'foo-bar-baz',
            },
            False,
            None,
        )
    ]

    @pytest.mark.parametrize('given, expected, valid', testcases)
    def test_load_commission_event_schema(
        self,
        given: tp.Dict,
        expected: tp.Dict,
        valid: bool,
    ):
        if not valid:
            with pytest.raises(exc.LoadMethodError):
                schemas.load_commission_event_schema(given)

            return

        actual = schemas.load_commission_event_schema(given)

        hm.assert_that(actual, hm.equal_to(expected))


class TestLoadCommissionReferencesSchema:
    dt = datetime(2022, 3, 21, 13, 17, 34, tzinfo=tzutc())

    testcases = [
        pytest.param(
            {
                'contracts': [],
                'migration_info': [
                    {
                        'namespace': 'bnpl_income',
                        'filter': 'Firm',
                        'object_id': 12,
                        'from_dt': dt.strftime(_DATETIME_FMT),
                        'dry_run': True,
                    }
                ],
                'firm': {
                    'id': 12,
                    'mdh_id': 'mdh_id',
                    'title': 'Some firm',
                    'legal_address': 'Some legal address',
                    'inn': '1234567891',
                    'kpp': '123456789',
                    'email': 'some@email.ru',
                    'mnclose_email': 'mnclose@email.ru',
                    'payment_invoice_email': 'payment_invoice_email@email.ru',
                    'phone': '+79087813132',
                    'region_id': 31,
                    'currency_rate_src': 'currency_rate_src',
                    'default_iso_currency': 'default_iso_currency',
                    'pa_prefix': 'pa_prefix',
                    'contract_id': 3,
                    'unilateral': 112,
                    'postpay': 1345,
                    'alter_permition_code': 'alter_permition_code',
                    'test_env': 1,
                    'tax_policies': [
                        {
                            'id': 1,
                            'name': 'tax policy',
                            'hidden': 1,
                            'resident': 1,
                            'region_id': 31,
                            'default_tax': 1,
                            'mdh_id': 'mdh_id',
                            'spendable_nds_id': 40,
                            'percents': [
                                {
                                    'id': 1,
                                    'dt': dt.strftime(_DATETIME_FMT),
                                    'nds_pct': Decimal(13.0),
                                    'nsp_pct': Decimal(10.0),
                                    'hidden': 1,
                                    'mdh_id': 'mdh_id',
                                },
                            ]
                        }
                    ],
                    'person_categories': [
                        {
                            'category': 'category',
                            'is_resident': 1,
                            'is_legal': 1,
                        },
                    ],
                },
                'products': {
                    'id': 'mdh',
                }
            },
            {
                'contracts': [],
                'migration_info': [
                    {
                        'namespace': 'bnpl_income',
                        'filter': 'Firm',
                        'object_id': 12,
                        'from_dt': dt,
                        'dry_run': True,
                    }
                ],
                'firm': {
                    'id': 12,
                    'mdh_id': 'mdh_id',
                    'title': 'Some firm',
                    'legal_address': 'Some legal address',
                    'inn': '1234567891',
                    'kpp': '123456789',
                    'email': 'some@email.ru',
                    'mnclose_email': 'mnclose@email.ru',
                    'payment_invoice_email': 'payment_invoice_email@email.ru',
                    'phone': '+79087813132',
                    'region_id': 31,
                    'currency_rate_src': 'currency_rate_src',
                    'default_iso_currency': 'default_iso_currency',
                    'pa_prefix': 'pa_prefix',
                    'contract_id': 3,
                    'unilateral': 112,
                    'postpay': 1345,
                    'alter_permition_code': 'alter_permition_code',
                    'test_env': 1,
                    'tax_policies': [
                        {
                            'id': 1,
                            'name': 'tax policy',
                            'hidden': 1,
                            'resident': 1,
                            'region_id': 31,
                            'default_tax': 1,
                            'mdh_id': 'mdh_id',
                            'spendable_nds_id': 40,
                            'percents': [
                                {
                                    'id': 1,
                                    'dt': dt,
                                    'nds_pct': Decimal(13.0),
                                    'nsp_pct': Decimal(10.0),
                                    'hidden': 1,
                                    'mdh_id': 'mdh_id',
                                },
                            ]
                        }
                    ],
                    'person_categories': [
                        {
                            'category': 'category',
                            'is_resident': 1,
                            'is_legal': 1,
                        },
                    ],
                },
                'products': {
                    'id': 'mdh',
                },
                'lock': {
                    'states': [],
                }
            },
            True,
        ),
        pytest.param(
            {
                'contracts': [],
                'migration_info': [
                    {
                        'namespace': 'bnpl_income',
                        'filter': 'Firm',
                        'object_id': 12,
                        'from_dt': dt.strftime(_DATETIME_FMT),
                        'dry_run': True,
                    }
                ],
                'products': {
                    'id': 'mdh',
                }
            },
            None,
            False,
        )
    ]

    @pytest.mark.parametrize('given, expected, valid', testcases)
    def test_load_commission_references_schema(
        self,
        given: tp.Dict,
        expected: tp.Dict,
        valid: bool,
    ):
        if not valid:
            with pytest.raises(exc.LoadMethodError):
                schemas.load_commission_references_schema(given)

            return

        actual = schemas.load_commission_references_schema(given)

        hm.assert_that(actual, hm.equal_to(expected))


class TestDumpCommissionTransactionBatchSchema:
    testcases = [
        pytest.param(
            models.CommissionTransactionBatch(
                event=models.CommissionEvent(
                    transaction_id='lqkllqwkqwlw120312',
                    transaction_amount=Decimal('10.00'),
                    transaction_dt=datetime(2022, 3, 22),
                    transaction_type=models.TransactionType.REFUND,
                    billing_client_id=1,
                    billing_contract_id=2,
                    currency='RUB',
                    product_id='foo',
                    tariffer_payload={
                        'product_mdh_id': 'bar',
                        'common_ts': 1633850400,
                        'tax_policy_id': 1,
                        'external_id': 'o28391jk209213',
                        'tax_policy_pct': tax.TaxPolicyPctModel(
                            dt=datetime(2010, 9, 9),
                            hidden=0,
                            id=3,
                            mdh_id='mdh_id',
                            nds_pct=Decimal('0.4'),
                            nsp_pct=Decimal('0.2'),
                        ),
                        'dry_run': True,
                        'amount_wo_vat': Decimal('1.12'),
                        'firm_id': 1,
                    },
                ),
                client_transactions=[
                    tx.ClientTransactionBatchModel(
                        client_id=1,
                        transactions=[
                            tx.TransactionModel(
                                loc={
                                    'namespace': 'bnpl_income',
                                    'type': 'commission_refunds',
                                    'client_id': 1,
                                    'contract_id': 2,
                                    'currency': 'RUB',
                                    'product': 'bar',
                                },
                                amount=Decimal('10.00'),
                                type='credit',
                                dt=arrow.get(datetime(2022, 3, 22)).int_timestamp
                            ),
                        ],
                    ),
                ],
            ),
            {
                'event': {
                    'transaction_id': 'lqkllqwkqwlw120312',
                    'transaction_amount': '10.00',
                    'transaction_dt': '2022-03-22T00:00:00+00:00',
                    'transaction_type': 'refund',
                    'billing_client_id': 1,
                    'billing_contract_id': 2,
                    'currency': 'RUB',
                    'product_id': 'foo',
                    'payload': None,
                    'tariffer_payload': {
                        'product_mdh_id': 'bar',
                        'common_ts': 1633850400,
                        'tax_policy_id': 1,
                        'external_id': 'o28391jk209213',
                        'tax_policy_pct': {
                            'dt': '2010-09-09T00:00:00+00:00',
                            'hidden': 0,
                            'id': 3,
                            'mdh_id': 'mdh_id',
                            'nds_pct': '0.4',
                            'nsp_pct': '0.2',
                            'sum_taxes': '0.6',
                            'tax_date': None,
                            'tax_date_2': None,
                            'tax_dict': None,
                            'tax_var': None,
                        },
                        'dry_run': True,
                        'amount_wo_vat': '1.12',
                        'firm_id': 1,
                    },
                },
                'client_transactions': [
                    {
                        'client_id': 1,
                        'transactions': [
                            {
                                'type': 'credit',
                                'dt': 1647907200,
                                'amount': '10.00',
                                'loc': {
                                    'namespace': 'bnpl_income',
                                    'type': 'commission_refunds',
                                    'client_id': 1,
                                    'contract_id': 2,
                                    'currency': 'RUB',
                                    'product': 'bar',
                                },
                            },
                        ],
                    },
                ],
            },
        ),
    ]

    @pytest.mark.parametrize('given, expected', testcases)
    def test_dump_commission_transaction_batch_schema(
        self,
        given: tp.Any,
        expected: tp.Dict,
    ):
        actual = schemas.dump_commission_transaction_batch_schema(given)

        hm.assert_that(actual, hm.equal_to(expected))
