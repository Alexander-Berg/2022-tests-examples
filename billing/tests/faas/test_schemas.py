import typing as tp
from datetime import datetime
from decimal import Decimal

import hamcrest as hm
import pytest
from billing.library.python.calculator import exceptions as exc
from billing.library.python.calculator.models import transaction as tx
from dateutil.tz import tzutc

from billing.hot.calculators.actotron_act_rows.calculator.core import models
from billing.hot.calculators.actotron_act_rows.calculator.faas.schemas import (
    ActOTronActRowsEventSchema,
    ActOTronActRowsReferencesSchema,
    ActOTronActRowsMethodSchema,
    ActedCommissionTransactionBatchSchema,
)

_DATETIME_FMT = '%Y-%m-%dT%H:%M:%SZ'


class TestLoadActOTronActRowsMethodSchema:
    testcases = ['valid', 'no_event', 'no_references']

    dt = datetime(2022, 3, 21, 13, 32, 20, tzinfo=tzutc())

    @pytest.fixture(params=testcases)
    def bundle(self, request) -> tp.Dict[str, tp.Any]:
        return {
            'valid': {
                'method': {
                    'event': {
                        'obj': {
                            'act_row_id': 'act-row-id',
                            'export_id': 1,
                            'mdh_product_id': 'mdh',
                            'act_sum': '1014.19',
                            'act_sum_components': {
                                'act_sum_negative': '-15.23',
                                'act_sum_positive': '1029.42',
                                'act_sum_wo_vat_negative': '-13.681109',
                                'act_sum_wo_vat_positive': '924.727986',
                            },
                            'act_effective_nds_pct': '10.17',
                            'tariffer_service_id': 2,
                            'act_start_dt': self.dt.strftime(_DATETIME_FMT),
                            'act_finish_dt': self.dt.strftime(_DATETIME_FMT),
                            'client_id': 10,
                            'contract_id': 11,
                            'currency': 'RUB',
                            'version_id': 3,
                        },
                        'classname': 'ActOTronActRows',
                        'version_id': 3,
                    },
                    'references': {
                        'contracts': [],
                        'migration_info': [
                            {
                                'namespace': 'bnpl',
                                'filter': 'Client',
                                'object_id': 1,
                                'from_dt': self.dt.strftime(_DATETIME_FMT),
                                'dry_run': True,
                            }
                        ],
                        'personal_accounts': [
                            {
                                'id': 1,
                                'contract_id': 11,
                                'client_id': 10,
                                'version': 3,
                                'obj': {
                                    'id': 1,
                                    'contract_id': 1,
                                    'external_id': '2134/7482',
                                    'iso_currency': 'RUB',
                                    'type': 'personal_account',
                                    'service_code': None,
                                    'hidden': 1,
                                    'postpay': 1,
                                },
                            },
                        ],
                    },
                },
                'should_valid': True,
            },
            'no_event': {
                'method': {
                    'references': {
                        'contracts': [],
                        'migration_info': [
                            {
                                'namespace': 'bnpl',
                                'filter': 'Client',
                                'object_id': 1,
                                'from_dt': self.dt.strftime(_DATETIME_FMT),
                                'dry_run': False,
                            }
                        ],
                        'personal_accounts': [
                            {
                                'id': 1,
                                'contract_id': 4,
                                'client_id': 5,
                                'version': 10,
                                'obj': {
                                    'id': 1,
                                    'contract_id': 4,
                                    'external_id': '2134/7482',
                                    'iso_currency': 'RUB',
                                    'type': 'personal_account',
                                    'service_code': None,
                                    'hidden': 1,
                                    'postpay': 1,
                                },
                            },
                        ],
                    },
                },
                'should_valid': False,
            },
            'no_references': {
                'method': {
                    'event': {
                        'obj': {
                            'act_row_id': 'act-row-id',
                            'export_id': 2,
                            'mdh_product_id': 'mdh',
                            'act_sum': '1014.19',
                            'act_sum_components': {
                                'act_sum_negative': '-15.23',
                                'act_sum_positive': '1029.42',
                                'act_sum_wo_vat_negative': '-13.681109',
                                'act_sum_wo_vat_positive': '924.727986',
                            },
                            'act_effective_nds_pct': '10.17',
                            'tariffer_service_id': 3,
                            'act_start_dt': self.dt.strftime(_DATETIME_FMT),
                            'act_finish_dt': self.dt.strftime(_DATETIME_FMT),
                            'client_id': 20,
                            'contract_id': 21,
                            'currency': 'RUB',
                            'version_id': 4,
                        },
                        'classname': 'ActOTronActRows',
                        'version_id': 4,
                    },
                },
                'should_valid': False,
            }
        }[request.param]

    @pytest.fixture
    def given_method(self, bundle: tp.Dict[str, tp.Any]) -> tp.Dict[str, tp.Any]:
        return bundle.get('method')

    @pytest.fixture
    def should_valid(self, bundle: tp.Dict[str, tp.Any]) -> bool:
        return bundle.get('should_valid')

    def test_load_method(self, given_method: tp.Dict[str, tp.Any], should_valid: bool):
        if should_valid:
            hm.assert_that(
                hm.calling(ActOTronActRowsMethodSchema().adap_load).with_args(given_method),
                hm.not_(hm.raises(exc.LoadSchemaError)),
            )

            return

        hm.assert_that(
            hm.calling(ActOTronActRowsMethodSchema().adap_load).with_args(given_method),
            hm.raises(exc.LoadSchemaError),
        )


class TestLoadActOTronActRowsEventSchema:
    testcases = [
        'valid_all_fields',
        'valid_required_fields',
        'mistype',
        'no_required_fields',
    ]

    dt = datetime(2022, 4, 21, 13, 13, 13, tzinfo=tzutc())

    @pytest.fixture(params=testcases)
    def bundle(self, request) -> tp.Dict[str, tp.Any]:
        return {
            'valid_all_fields': {
                'given_event': {
                    'act_row_id': 'act-row-id',
                    'export_id': 3,
                    'mdh_product_id': 'mdh',
                    'act_sum': '23134.19',
                    'act_sum_components': {
                        'act_sum_negative': '-1501.23',
                        'act_sum_positive': '24635.42',
                        'act_sum_wo_vat_negative': '-1303.518009',
                        'act_sum_wo_vat_positive': '21390.935186',
                    },
                    'act_effective_nds_pct': '13.17',
                    'tariffer_service_id': 3,
                    'act_start_dt': self.dt.strftime(_DATETIME_FMT),
                    'act_finish_dt': self.dt.strftime(_DATETIME_FMT),
                    'client_id': 20,
                    'contract_id': 21,
                    'currency': 'RUB',
                    'version_id': 5,
                },
                'expected_event': {
                    'act_row_id': 'act-row-id',
                    'export_id': 3,
                    'mdh_product_id': 'mdh',
                    'act_sum': Decimal('23134.19'),
                    'act_sum_components': {
                        'act_sum_negative': Decimal('-1501.23'),
                        'act_sum_positive': Decimal('24635.42'),
                        'act_sum_wo_vat_negative': Decimal('-1303.518009'),
                        'act_sum_wo_vat_positive': Decimal('21390.935186'),
                    },
                    'act_effective_nds_pct': Decimal('13.17'),
                    'tariffer_service_id': 3,
                    'act_start_dt': self.dt,
                    'act_finish_dt': self.dt,
                    'client_id': 20,
                    'contract_id': 21,
                    'currency': 'RUB',
                    'version_id': 5,
                    'tariffer_payload': {},
                    'payload': {},
                },
                'should_valid': True,
            },
            'valid_required_fields': {
                'given_event': {
                    'act_row_id': 'act-row-id',
                    'mdh_product_id': 'mdh',
                    'act_sum': '23134.19',
                    'act_sum_components': {
                        'act_sum_negative': None,
                        'act_sum_positive': '23134.19',
                        'act_sum_wo_vat_negative': None,
                        'act_sum_wo_vat_positive': '20087.417177',
                    },
                    'act_effective_nds_pct': '13.17',
                    'act_finish_dt': self.dt.strftime(_DATETIME_FMT),
                    'client_id': 20,
                    'contract_id': 21,
                    'currency': 'RUB',
                },
                'expected_event': {
                    'act_row_id': 'act-row-id',
                    'mdh_product_id': 'mdh',
                    'act_sum': Decimal('23134.19'),
                    'act_sum_components': {
                        'act_sum_negative': None,
                        'act_sum_positive': Decimal('23134.19'),
                        'act_sum_wo_vat_negative': None,
                        'act_sum_wo_vat_positive': Decimal('20087.417177'),
                    },
                    'act_effective_nds_pct': Decimal('13.17'),
                    'act_finish_dt': self.dt,
                    'client_id': 20,
                    'contract_id': 21,
                    'currency': 'RUB',
                    'tariffer_payload': {},
                    'payload': {},
                },
                'should_valid': True,
            },
            'mistype': {
                'given_event': {
                    'act_row_id': 228,
                    'export_id': 'wrong',
                    'mdh_product_id': 'mdh',
                    'act_sum': '23134.19',
                    'act_sum_components': {
                        'act_sum_negative': None,
                        'act_sum_positive': '23134.19',
                        'act_sum_wo_vat_negative': None,
                        'act_sum_wo_vat_positive': '20087.417177',
                    },
                    'act_effective_nds_pct': '13.17',
                    'tariffer_service_id': 3,
                    'act_start_dt': self.dt.strftime(_DATETIME_FMT),
                    'act_finish_dt': self.dt.strftime(_DATETIME_FMT),
                    'client_id': 'wrong',
                    'contract_id': 21,
                    'currency': 'RUB',
                    'version_id': 5,
                },
                'should_valid': False,
            },
            'no_required_fields': {
                'given_event': {
                    'export_id': 3,
                    'tariffer_service_id': 3,
                    'act_start_dt': self.dt.strftime(_DATETIME_FMT),
                    'version_id': 5,
                },
                'should_valid': False,
            },
        }[request.param]

    @pytest.fixture
    def given_event(self, bundle: tp.Dict[str, tp.Any]) -> tp.Dict[str, tp.Any]:
        return bundle.get('given_event')

    @pytest.fixture
    def expected_event(self, bundle: tp.Dict[str, tp.Any]) -> tp.Dict[str, tp.Any]:
        return bundle.get('expected_event')

    @pytest.fixture
    def should_valid(self, bundle: tp.Dict[str, tp.Any]) -> bool:
        return bundle.get('should_valid')

    def test_load_event(
        self,
        given_event: tp.Dict[str, tp.Any],
        expected_event: tp.Dict[str, tp.Any],
        should_valid: bool,
    ):
        if not should_valid:
            hm.assert_that(
                hm.calling(ActOTronActRowsEventSchema().adap_load).with_args(given_event),
                hm.raises(exc.LoadSchemaError),
            )

            return

        actual = ActOTronActRowsEventSchema().adap_load(given_event)

        hm.assert_that(actual, hm.equal_to(expected_event))


class TestLoadActOTronActRowsReferencesSchema:
    testcases = ['valid', 'not_valid']

    dt = datetime(2022, 10, 21, 13, 43, 12, tzinfo=tzutc())

    @pytest.fixture(params=testcases)
    def bundle(self, request) -> tp.Dict[str, tp.Any]:
        return {
            'valid': {
                'given_references': {
                    'contracts': [],
                    'migration_info': [
                        {
                            'namespace': 'bnpl',
                            'filter': 'Client',
                            'object_id': 3,
                            'from_dt': self.dt.strftime(_DATETIME_FMT),
                            'dry_run': True,
                        }
                    ],
                    'personal_accounts': [
                        {
                            'id': 2,
                            'contract_id': 11,
                            'client_id': 10,
                            'version': 3,
                            'obj': {
                                'id': 2,
                                'contract_id': 11,
                                'external_id': '2134/7482',
                                'iso_currency': 'RUB',
                                'type': 'personal_account',
                                'service_code': None,
                                'hidden': 1,
                                'postpay': 1,
                            },
                        },
                    ],
                },
                'expected_references': {
                    'contracts': [],
                    'migration_info': [
                        {
                            'namespace': 'bnpl',
                            'filter': 'Client',
                            'object_id': 3,
                            'from_dt': self.dt,
                            'dry_run': True,
                        }
                    ],
                    'personal_accounts': [
                        {
                            'id': 2,
                            'contract_id': 11,
                            'client_id': 10,
                            'version': 3,
                            'obj': {
                                'id': 2,
                                'contract_id': 11,
                                'external_id': '2134/7482',
                                'iso_currency': 'RUB',
                                'type': 'personal_account',
                                'service_code': None,
                                'hidden': 1,
                                'postpay': 1,
                            },
                        },
                    ],
                },
                'should_valid': True,
            },
            'not_valid': {
                'given_references': {
                    'contracts': [],
                },
                'should_valid': False,
            },
        }[request.param]

    @pytest.fixture
    def given_references(self, bundle: tp.Dict[str, tp.Any]) -> tp.Dict[str, tp.Any]:
        return bundle.get('given_references')

    @pytest.fixture
    def expected_references(self, bundle: tp.Dict[str, tp.Any]) -> tp.Dict[str, tp.Any]:
        return bundle.get('expected_references')

    @pytest.fixture
    def should_valid(self, bundle: tp.Dict[str, tp.Any]) -> bool:
        return bundle.get('should_valid')

    def test_load_references(
        self,
        given_references: tp.Dict[str, tp.Any],
        expected_references: tp.Dict[str, tp.Any],
        should_valid: bool,
    ):
        if not should_valid:
            hm.assert_that(
                hm.calling(ActOTronActRowsReferencesSchema().adap_load).with_args(given_references),
                hm.raises(exc.LoadSchemaError),
            )

            return

        actual = ActOTronActRowsReferencesSchema().adap_load(given_references)

        hm.assert_that(actual, hm.equal_to(expected_references))


class TestDumpActedCommissionTransactionBatchSchema:
    testcases = ['valid', 'not_valid']

    @pytest.fixture(params=testcases)
    def bundle(self, request) -> tp.Dict[str, tp.Any]:
        return {
            'valid': {
                'given_transaction_batch': models.ActedCommissionTransactionBatch(
                    event=models.ActOTronActRowsEvent(
                        act_row_id='act-row-id',
                        export_id=1,
                        mdh_product_id='mdh',
                        act_sum=Decimal('123.99'),
                        act_sum_components=models.ActOTronActSumComponents(
                            act_sum_negative=Decimal('-11.23'),
                            act_sum_positive=Decimal('135.22'),
                            act_sum_wo_vat_negative=Decimal('-11.118823'),
                            act_sum_wo_vat_positive=Decimal('133.881322'),
                        ),
                        act_effective_nds_pct=Decimal('0.99'),
                        tariffer_service_id=1,
                        act_start_dt=datetime(2022, 9, 9),
                        act_finish_dt=datetime(2022, 9, 9),
                        client_id=9,
                        contract_id=10,
                        currency='RUB',
                        version_id=2,
                        payload={},
                        tariffer_payload={
                            'common_ts': 1633850400,
                            'external_id': 'e:id',
                            'dry_run': True,
                        },
                    ),
                    client_transactions=[
                        tx.ClientTransactionBatchModel(
                            client_id=9,
                            transactions=[
                                tx.TransactionModel(
                                    loc={
                                        'namespace': 'bnpl',
                                        'type': 'commissions',
                                        'client_id': 9,
                                        'contract_id': 10,
                                        'currency': 'RUB',
                                        'product': 'foo',
                                    },
                                    amount=Decimal('2.28'),
                                    type='credit',
                                    dt=1662670800,
                                ),
                            ],
                        ),
                    ],
                ),
                'expected_transaction_batch': {
                    'event': {
                        'act_row_id': 'act-row-id',
                        'export_id': 1,
                        'mdh_product_id': 'mdh',
                        'act_sum': '123.99',
                        'act_sum_components': {
                            'act_sum_negative': '-11.23',
                            'act_sum_positive': '135.22',
                            'act_sum_wo_vat_negative': '-11.118823',
                            'act_sum_wo_vat_positive': '133.881322',
                        },
                        'act_effective_nds_pct': '0.99',
                        'tariffer_service_id': 1,
                        'act_start_dt': '2022-09-09T00:00:00+00:00',
                        'act_finish_dt': '2022-09-09T00:00:00+00:00',
                        'client_id': 9,
                        'contract_id': 10,
                        'currency': 'RUB',
                        'version_id': 2,
                        'payload': {},
                        'tariffer_payload': {
                            'common_ts': 1633850400,
                            'external_id': 'e:id',
                            'dry_run': True,
                        },
                    },
                    'client_transactions': [
                        {
                            'client_id': 9,
                            'transactions': [
                                {
                                    'type': 'credit',
                                    'dt': 1662670800,
                                    'amount': '2.28',
                                    'loc': {
                                        'namespace': 'bnpl',
                                        'type': 'commissions',
                                        'client_id': 9,
                                        'contract_id': 10,
                                        'currency': 'RUB',
                                        'product': 'foo',
                                    },
                                },
                            ],
                        },
                    ]
                },
                'should_valid': True
            },
            'not_valid': {
                'given_transaction_batch': models.ActedCommissionTransactionBatch(
                    event=models.ActOTronActRowsEvent(
                        act_row_id=2,
                        export_id=1,
                        mdh_product_id='mdh',
                        act_sum=Decimal('123.99'),
                        act_sum_components=models.ActOTronActSumComponents(
                            act_sum_negative=Decimal('-11.23'),
                            act_sum_positive=Decimal('135.22'),
                            act_sum_wo_vat_negative=Decimal('-11.118823'),
                            act_sum_wo_vat_positive=Decimal('133.881322'),
                        ),
                        act_effective_nds_pct=Decimal('0.99'),
                        tariffer_service_id=1,
                        act_start_dt=datetime(2022, 9, 9),
                        act_finish_dt=datetime(2022, 9, 9),
                        client_id='wrong_client_id',
                        contract_id='wrong_contract_id',
                        currency='RUB',
                        version_id=3,
                        payload={},
                        tariffer_payload={
                            'common_ts': 1633850400,
                            'external_id': 'e:id',
                            'dry_run': True,
                        },
                    ),
                    client_transactions=[],
                ),
                'should_valid': False,
            }
        }[request.param]

    @pytest.fixture
    def given_transaction_batch(
        self,
        bundle: tp.Dict[str, tp.Any],
    ) -> models.ActedCommissionTransactionBatch:
        return bundle.get('given_transaction_batch')

    @pytest.fixture
    def expected_transaction_batch(self, bundle: tp.Dict[str, tp.Any]) -> tp.Dict[str, tp.Any]:
        return bundle.get('expected_transaction_batch')

    @pytest.fixture
    def should_valid(self, bundle: tp.Dict[str, tp.Any]) -> bool:
        return bundle.get('should_valid')

    def test_dump_transaction_batch(
        self,
        given_transaction_batch: models.ActedCommissionTransactionBatch,
        expected_transaction_batch: tp.Dict[str, tp.Any],
        should_valid: bool,
    ):
        if not should_valid:
            hm.assert_that(
                hm.calling(ActedCommissionTransactionBatchSchema().adap_dump).with_args(given_transaction_batch),
                hm.raises(exc.DumpSchemaError),
            )

            return

        actual = ActedCommissionTransactionBatchSchema().adap_dump(given_transaction_batch)

        hm.assert_that(actual, hm.equal_to(expected_transaction_batch))
