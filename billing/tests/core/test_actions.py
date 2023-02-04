import typing as tp
from datetime import datetime
from decimal import Decimal

import arrow
import hamcrest as hm
import pytest
from billing.library.python.calculator.models import transaction as tx
from billing.library.python.calculator.models.personal_account import ServiceCode
from billing.library.python.calculator.test_utils import builder

from billing.hot.calculators.actotron_act_rows.calculator.core import actions
from billing.hot.calculators.actotron_act_rows.calculator.core import models


@pytest.fixture
def settings(services: tp.List[int]) -> tp.Dict[str, tp.Any]:
    return {
        'namespace': 'bnpl',
        'service_id': services[0],
        'invoice_code': ServiceCode.EMPTY,
    }


@pytest.fixture
def binary_event(contracts: tp.List[tp.Dict[str, tp.Any]]) -> models.ActOTronActRowsEvent:
    return models.ActOTronActRowsEvent(
        act_row_id='act-row-id',
        export_id=1,
        mdh_product_id='mdh',
        act_sum=Decimal('113.12'),
        act_sum_components=models.ActOTronActSumComponents(
            act_sum_negative=Decimal('-11.23'),
            act_sum_positive=Decimal('124.35'),
            act_sum_wo_vat_negative=Decimal('-11.104224'),
            act_sum_wo_vat_positive=Decimal('122.95728'),
        ),
        act_effective_nds_pct=Decimal('1.12'),
        tariffer_service_id=2,
        act_start_dt=datetime(2022, 4, 10),
        act_finish_dt=datetime(2022, 4, 11),
        client_id=contracts[0]['client_id'],
        contract_id=contracts[0]['id'],
        currency='RUB',
        version_id=1,
    )


@pytest.fixture
def references(
    contracts: tp.List[tp.Dict[str, tp.Any]],
    migration_info: tp.List[tp.Dict[str, tp.Any]],
    personal_accounts: tp.List[tp.Dict[str, tp.Any]],
) -> models.ActOTronRowsReferences:
    return models.ActOTronRowsReferences(
        contracts=contracts,
        migration_info=migration_info,
        personal_accounts=personal_accounts,
    )


@pytest.fixture
def references_wo_migration_info(
    contracts: tp.List[tp.Dict[str, tp.Any]],
    personal_accounts: tp.List[tp.Dict[str, tp.Any]],
):
    return models.ActOTronRowsReferences(
        contracts=contracts,
        migration_info=[],
        personal_accounts=personal_accounts,
    )


@pytest.fixture
def references_wo_holding_commissions(
    contracts_wo_holding_commissions: tp.List[tp.Dict[str, tp.Any]],
    migration_info: tp.List[tp.Dict[str, tp.Any]],
    personal_accounts: tp.List[tp.Dict[str, tp.Any]],
):
    return models.ActOTronRowsReferences(
        contracts=contracts_wo_holding_commissions,
        migration_info=migration_info,
        personal_accounts=personal_accounts
    )


@pytest.fixture
def contracts(services: tp.List[int]) -> tp.List[tp.Dict[str, tp.Any]]:
    return [
        builder.gen_general_contract(
            contract_id=1,
            client_id=2,
            person_id=3,
            firm=4,
            services=services,
            withholding_commissions_from_payments=True,
        )
    ]


@pytest.fixture
def contracts_wo_holding_commissions(services: tp.List[int]) -> tp.List[tp.Dict[str, tp.Any]]:
    return [
        builder.gen_general_contract(
            contract_id=1,
            client_id=2,
            person_id=3,
            firm=4,
            services=services,
            withholding_commissions_from_payments=False,
        )
    ]


@pytest.fixture
def migration_info(
    contracts: tp.List[tp.Dict[str, tp.Any]],
    settings: tp.Dict[str, tp.Any],
) -> tp.List[tp.Dict[str, tp.Any]]:
    return [
        builder.gen_migration_info(
            namespace=settings['namespace'],
            filter='Client',
            object_id=contracts[0]['client_id'],
            from_dt=datetime(2019, 5, 5),
            dry_run=1,
        )
    ]


@pytest.fixture
def personal_accounts(
    contracts: tp.List[tp.Dict[str, tp.Any]],
) -> tp.List[tp.Dict[str, tp.Any]]:
    return [
        builder.gen_generic_personal_account(
            contract_id=contract['id'],
            client_id=contract['client_id'],
        ) for contract in contracts
    ]


@pytest.fixture
def services() -> tp.List[int]:
    return [228]


@pytest.fixture
def client_transactions(
    binary_event: models.ActOTronActRowsEvent,
    references: models.ActOTronRowsReferences,
    settings: tp.Dict[str, tp.Any],
) -> tp.List[tx.ClientTransactionBatchModel]:
    positive_amount = binary_event.act_sum_components.act_sum_positive
    negative_amount = abs(binary_event.act_sum_components.act_sum_negative)
    on_dt = arrow.get(binary_event.act_finish_dt).int_timestamp

    return [
        tx.ClientTransactionBatchModel(
            client_id=references.contracts[0]['client_id'],
            transactions=[
                tx.TransactionModel(
                    loc={
                        'namespace': settings['namespace'],
                        'type': 'commissions',
                        'contract_id': references.contracts[0]['id'],
                        'client_id': references.contracts[0]['client_id'],
                        'currency': 'RUB',
                        'product': '',
                    },
                    amount=positive_amount,
                    type='credit',
                    dt=on_dt,
                ),
                tx.TransactionModel(
                    loc={
                        'namespace': settings['namespace'],
                        'type': 'commissions_acted',
                        'contract_id': references.contracts[0]['id'],
                        'client_id': references.contracts[0]['client_id'],
                        'invoice_id': references.personal_accounts[0].id,
                        'currency': 'RUB',
                        'operation_type': '',
                    },
                    amount=positive_amount,
                    type='debit',
                    dt=on_dt,
                ),
                tx.TransactionModel(
                    loc={
                        'namespace': settings['namespace'],
                        'type': 'commission_refunds',
                        'contract_id': references.contracts[0]['id'],
                        'client_id': references.contracts[0]['client_id'],
                        'currency': 'RUB',
                        'product': '',
                    },
                    amount=negative_amount,
                    type='debit',
                    dt=on_dt,
                ),
                tx.TransactionModel(
                    loc={
                        'namespace': settings['namespace'],
                        'type': 'commission_refunds_acted',
                        'contract_id': references.contracts[0]['id'],
                        'client_id': references.contracts[0]['client_id'],
                        'invoice_id': references.personal_accounts[0].id,
                        'currency': 'RUB',
                        'operation_type': '',
                    },
                    amount=negative_amount,
                    type='credit',
                    dt=on_dt,
                )
            ],
        ),
    ]


class TestProcessActedCommissionActionRun:
    testcases = [
        'not_migrated',
        'wo_holding_commissions',
        'act_row',
    ]

    @pytest.fixture(params=testcases)
    def bundle(
        self,
        request,
        binary_event: models.ActOTronActRowsEvent,
        references: models.ActOTronRowsReferences,
        references_wo_migration_info: models.ActOTronRowsReferences,
        references_wo_holding_commissions: models.ActOTronRowsReferences,
        client_transactions: tp.List[tx.ClientTransactionBatchModel],
        settings: tp.Dict[str, tp.Any],
    ) -> tp.Dict[str, tp.Any]:
        return {
            'not_migrated': {
                'given_method': models.ActOTronActRowsMethod(
                    event=binary_event,
                    references=references_wo_migration_info,
                ),
                'expected_client_transactions': [],
            },
            'wo_holding_commissions': {
                'given_method': models.ActOTronActRowsMethod(
                    event=binary_event,
                    references=references_wo_holding_commissions,
                ),
                'expected_client_transactions': [],
            },
            'act_row': {
                'given_method': models.ActOTronActRowsMethod(
                    event=binary_event,
                    references=references,
                ),
                'expected_client_transactions': client_transactions,
            },
        }[request.param]

    @pytest.fixture
    def given_method(self, bundle: tp.Dict[str, tp.Any]) -> models.ActOTronActRowsMethod:
        return bundle.get('given_method')

    @pytest.fixture
    def expected_client_transactions(
        self,
        bundle: tp.Dict[str, tp.Any],
    ) -> tp.List[tx.ClientTransactionBatchModel]:
        return bundle.get('expected_client_transactions')

    def test_run(
        self,
        settings: tp.Dict[str, tp.Any],
        given_method: models.ActOTronActRowsMethod,
        expected_client_transactions: tp.List[tx.ClientTransactionBatchModel],
    ):
        action = actions.ProcessActedCommissionAction(settings, given_method)

        transaction_batch = action.run()
        actual_client_transactions = transaction_batch.client_transactions

        hm.assert_that(
            actual_client_transactions,
            hm.equal_to(expected_client_transactions),
        )
