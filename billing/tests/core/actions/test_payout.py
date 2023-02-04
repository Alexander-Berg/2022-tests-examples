from datetime import timedelta
from decimal import Decimal
from functools import reduce

import hamcrest as hm
from billing.hot.calculators.bnpl.calculator.core.actions.payout import ProcessPayoutAction
from billing.hot.calculators.bnpl.calculator.core.models.event import PayoutEvent
from billing.hot.calculators.bnpl.calculator.core.models.method import PayoutReferences
from billing.library.python.calculator.exceptions import NettingInThePastError
from billing.library.python.calculator.models.transaction import TransactionModel
from billing.library.python.calculator.services.account import (
    DistributionOperationType, OperationExpression
)


# подробно почему распределение по счетам такое - смотри в генерируемых балансах в conftest
class TestProcessPayoutAction:
    def test_happy_path(
        self,
        payout_event: PayoutEvent,
        payout_references_with_migration_info: PayoutReferences,
        operation_expressions: dict[int, list[OperationExpression]],
    ):
        action = ProcessPayoutAction(
            payout_event,
            payout_references_with_migration_info,
        )
        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        hm.assert_that(client_transactions, hm.has_length(1))

        transactions = client_transactions[0].transactions

        self._assert_all_bnpl_transactions(transactions)
        expected_transaction_count = sum(len(o.expressions) for o in operation_expressions)
        hm.assert_that(transactions, hm.has_length(expected_transaction_count))

        contract_ids = {t.loc['contract_id'] for t in transactions
                        if t.loc.get('type') == 'incoming_payments'}

        hm.assert_that(transaction_batch.event.tariffer_payload, hm.has_entry(
            'contract_states', hm.has_entries({
                'invoices': hm.has_entries({
                    contract_id: hm.has_item(
                        hm.has_entries({
                            'operation_type': DistributionOperationType.INSERT.value,
                            'amount': hm.is_not(hm.empty()),
                        })
                    )
                    for contract_id in contract_ids
                })
            })
        ))

        # проверим что вернули стейт
        hm.assert_that(transaction_batch.states, hm.has_length(1))
        # в котором обновили дату = дату события
        hm.assert_that(
            transaction_batch.states, hm.has_item(
                hm.has_entries({
                    'loc': hm.has_entries({
                        'namespace': 'bnpl',
                        'client_id': payout_event.client_id,
                    }),
                    'state': payout_event.event_time,
                })
            )
        )

    def test_netting_in_the_past_error(
        self,
        payout_event: PayoutEvent,
        payout_references_with_migration_info: PayoutReferences,
    ):
        payout_event.event_time = payout_references_with_migration_info.lock.states[0].state - timedelta(days=1)
        hm.assert_that(
            hm.calling(ProcessPayoutAction).with_args(payout_event, payout_references_with_migration_info),
            hm.raises(NettingInThePastError)
        )

    def test_operation_expressions(
        self,
        payout_event: PayoutEvent,
        payout_references_with_migration_info: PayoutReferences,
        operation_expressions: dict[int, list[OperationExpression]],
    ):
        action = ProcessPayoutAction(payout_event, payout_references_with_migration_info)

        transaction_batch = action.run()
        actual_operation_expressions = transaction_batch.event.tariffer_payload['operation_expressions'][1]

        hm.assert_that(
            actual_operation_expressions,
            hm.has_items(*operation_expressions),
        )

    @staticmethod
    def _amount(transactions: list[TransactionModel]) -> Decimal:
        return reduce(lambda a, b: a + b, [t.amount for t in transactions], Decimal('0.00'))

    @staticmethod
    def _assert_all_bnpl_transactions(transactions: list[TransactionModel]):
        assert all(t.loc['namespace'] == 'bnpl' for t in transactions)
