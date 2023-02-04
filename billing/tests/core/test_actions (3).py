from decimal import Decimal

import hamcrest as hm
from billing.library.python.calculator.models.transaction import TransactionModel
from billing.library.python.calculator.models.method import LockLocModel, StateModel

from billing.hot.calculators.taxi_light.calculator.core.actions import ProcessPayoutAction
from billing.hot.calculators.taxi_light.calculator.core.models import PayoutMethod


class TestProcessPayoutAction:
    def test_fill_invoices(self, method: PayoutMethod):
        action = ProcessPayoutAction({}, method)
        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        hm.assert_that(client_transactions, hm.has_length(1))

        transactions = client_transactions[0].transactions

        self._assert_all_taxi_light_transactions(transactions)

        transactions_count = sum(1 for t in transactions if t.loc.get('type') == 'incoming_payments_sent')
        hm.assert_that(transaction_batch.event.tariffer_payload, hm.has_entry(
            'contract_states', hm.has_entries({'invoices': hm.has_length(transactions_count)})
        ))

        hm.assert_that(transaction_batch.states, hm.has_length(1))
        hm.assert_that(transaction_batch.states, hm.has_item(
            StateModel(
                loc=LockLocModel(
                    namespace="taxi_light",
                    type="cutoff_dt_state",
                    client_id=method.event.client_id,
                ),
                state=method.event.transaction_dt,
            )
        ))

    def test_transaction_correctness(self, method: PayoutMethod):
        action = ProcessPayoutAction({}, method)
        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        hm.assert_that(client_transactions, hm.has_length(1))

        transactions = client_transactions[0].transactions

        self._assert_all_taxi_light_transactions(transactions)

        debit_transactions = [t for t in transactions if t.type == 'debit']
        credit_transactions = [t for t in transactions if t.type == 'credit']
        hm.assert_that(debit_transactions, hm.has_length(len(credit_transactions)))

        debited = self._amount(debit_transactions)
        credited = self._amount(credit_transactions)
        hm.assert_that(debited, hm.equal_to(credited))

    def test_skip_dry_run(self, method: PayoutMethod):
        action = ProcessPayoutAction({'skip_dry_run': True}, method)
        transaction_batch = action.run()

        client_transactions = transaction_batch.client_transactions
        hm.assert_that(client_transactions, hm.has_length(0))

    @staticmethod
    def _amount(transactions: list[TransactionModel]) -> Decimal:
        return sum(t.amount for t in transactions) if transactions else Decimal('0.00')

    @staticmethod
    def _assert_all_taxi_light_transactions(transactions: list[TransactionModel]):
        hm.assert_that(all(t.loc['namespace'] == 'taxi_light' for t in transactions), hm.is_(True))
