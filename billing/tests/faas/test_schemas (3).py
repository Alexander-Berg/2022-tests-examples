from billing.library.python.calculator import exceptions

from billing.hot.calculators.taxi_light.calculator.core.models import PayoutTransactionBatch
from billing.hot.calculators.taxi_light.calculator.faas.schemas import PayoutMethodSchema, PayoutTransactionBatchSchema


class TestLoadPayoutSchema:
    def test_load_payout_method(self, payout_event: dict, payout_references: dict):
        given_payout_method = {
            'event': payout_event,
            'references': payout_references,
        }

        try:
            _ = PayoutMethodSchema().adap_load(given_payout_method)
        except exceptions.LoadMethodError as e:
            assert False, f'PayoutMethodSchema.adap_load raises {e}'


class TestPayoutTransactionBatchSchema:
    def test_dump_payout_transaction_batch_schema(self, payout_transaction_batch: PayoutTransactionBatch):
        try:
            _ = PayoutTransactionBatchSchema().adap_dump(payout_transaction_batch)
        except exceptions.DumpTransactionsError as e:
            assert False, f'PayoutTransactionBatchSchema.adap_dump raises {e}'
