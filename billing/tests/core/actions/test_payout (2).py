from decimal import Decimal
from functools import reduce

import hamcrest as hm
from billing.library.python.calculator.models.transaction import TransactionModel
from billing.library.python.calculator.services.account import OperationExpression

from billing.hot.calculators.oplata.calculator.core.actions.payout import ProcessPayoutAction
from billing.hot.calculators.oplata.calculator.core.models.event import PayoutEvent
from billing.hot.calculators.oplata.calculator.core.models.method import PayoutReferences


class TestProcessPayoutAction:
    def test_fill_invoices(
        self,
        payout_event: PayoutEvent,
        payout_references_with_migration_info: PayoutReferences
    ):
        action = ProcessPayoutAction(
            payout_event,
            payout_references_with_migration_info,
        )
        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        hm.assert_that(client_transactions, hm.has_length(1))

        transactions = client_transactions[0].transactions

        self._assert_all_oplata_transactions(transactions)

        agent_rewards_transactions = [t for t in transactions
                                      if t.loc.get('type') == 'agent_rewards' and t.amount > 0]
        agent_rewards_transactions_count = len(agent_rewards_transactions)

        hm.assert_that(transaction_batch.event.tariffer_payload, hm.has_entry(
            'contract_states', hm.has_entries({
                'invoices': hm.has_length(agent_rewards_transactions_count)
            })
        ))

    def test_transaction_correctness(
        self,
        payout_event: PayoutEvent,
        payout_references_with_migration_info: PayoutReferences,
    ):
        action = ProcessPayoutAction(
            payout_event,
            payout_references_with_migration_info,
        )
        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        hm.assert_that(client_transactions, hm.has_length(1))

        transactions = client_transactions[0].transactions

        self._assert_all_oplata_transactions(transactions)

        debit_transactions = [t for t in transactions if t.type == 'debit']
        credit_transactions = [t for t in transactions if t.type == 'credit']
        hm.assert_that(debit_transactions, hm.has_length(len(credit_transactions)))

        debited = self._amount(debit_transactions)
        credited = self._amount(credit_transactions)
        hm.assert_that(debited, hm.equal_to(credited))

    def test_operation_expressions(
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
        assert transaction_batch.event.tariffer_payload['operation_expressions'] == operation_expressions

    @staticmethod
    def _amount(transactions: list[TransactionModel]) -> Decimal:
        return reduce(lambda a, b: a + b, [t.amount for t in transactions], Decimal('0.00'))

    @staticmethod
    def _assert_all_oplata_transactions(transactions: list[TransactionModel]):
        hm.assert_that(all(t.loc['namespace'] == 'oplata' for t in transactions), hm.is_(True))
