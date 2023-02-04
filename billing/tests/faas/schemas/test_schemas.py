from billing.library.python.calculator.exceptions import (
    LoadMethodError, DumpTransactionsError,
)

from billing.hot.calculators.bnpl.calculator.faas.schemas.method import (
    load_payout_method_schema, load_cashless_method_schema,
)
from billing.hot.calculators.bnpl.calculator.faas.schemas.transaction import (
    dump_cashless_transaction_batch_schema, dump_payout_transaction_batch_schema,
)


class TestLoadCashlessSchemas:
    def test_load_payment_method(self, payment_event: dict, cashless_references: dict):
        given_payment_method = {
            'event': payment_event,
            'references': cashless_references,
        }

        try:
            _ = load_cashless_method_schema(given_payment_method)
        except LoadMethodError as e:
            assert False, f'load_cashless_method_schema raises {e}'

    def test_load_refund_method(self, refund_event: dict, cashless_references: dict):
        given_refund_method = {
            'event': refund_event,
            'references': cashless_references,
        }

        try:
            _ = load_cashless_method_schema(given_refund_method)
        except LoadMethodError as e:
            assert False, f'load_cashless_method_schema raises {e}'

    def test_load_method_with_empty_event(self, cashless_references: dict):
        given_method = {
            'event': {},
            'references': cashless_references,
        }

        try:
            load_cashless_method_schema(given_method)
        except LoadMethodError as e:
            assert True, f"load_cashless_method_schema raises {e}"


class TestLoadPayoutSchema:
    def test_load_payout_method(self, payout_event: dict, payout_references: dict):
        given_payout_method = {
            'event': payout_event,
            'references': payout_references,
        }

        try:
            _ = load_payout_method_schema(given_payout_method)
        except LoadMethodError as e:
            assert False, f'load_payout_method_schema raises {e}'


class TestCashlessTransactionBatchSchema:
    def test_dump_cashless_transaction_batch_schema(self, cashless_transaction_batch: dict):
        try:
            _ = dump_cashless_transaction_batch_schema(cashless_transaction_batch)
        except DumpTransactionsError as e:
            assert False, f'dump_cashless_transaction_batch_schema raises {e}'


class TestPayoutTransactionBatchSchema:
    def test_dump_payout_transaction_batch_schema(self, payout_transaction_batch: dict):
        try:
            _ = dump_payout_transaction_batch_schema(payout_transaction_batch)
        except DumpTransactionsError as e:
            assert False, f'dump_payout_transaction_batch_schema raises {e}'
