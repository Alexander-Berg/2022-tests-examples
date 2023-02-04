from typing import Any

import pytest

from billing.library.python.calculator.exceptions import DumpTransactionsError, LoadMethodError

from billing.hot.calculators.trust.calculator.core.models.transaction import PaymentTransactionBatchModel
from billing.hot.calculators.trust.calculator.core.schemas.method import load_payment_method_schema
from billing.hot.calculators.trust.calculator.core.schemas.transaction import (
    _payment_transaction_batch_schema,
    dump_payment_transaction_batch_schema,
)

from ..test_data.payment.raw_data import (
    PAYMENT_COMPOSITE_METHOD,
    PAYMENT_METHOD,
    PAYMENT_TRANSACTION_BATCH,
    PAYMENT_WITH_REFUNDS_METHOD,
)


class TestLoadPaymentMethodSchema:
    @pytest.mark.parametrize("method", [PAYMENT_METHOD, PAYMENT_WITH_REFUNDS_METHOD, PAYMENT_COMPOSITE_METHOD])
    def test_load(self, method: dict) -> None:
        try:
            _ = load_payment_method_schema(method)
        except LoadMethodError as e:
            assert False, f"load_payment_method_schema raises {e}"

    def test_load_empty_method(self) -> None:
        method_data: dict[str, Any] = {
            "event": {},
            "references": {},
        }

        with pytest.raises(LoadMethodError):
            _ = load_payment_method_schema(method_data)


class TestDumpPaymentTransactionBatchSchema:
    @pytest.mark.parametrize("payment_transaction_batch", [PAYMENT_TRANSACTION_BATCH])
    def test_dump_payment_transaction_batch_schema(self, payment_transaction_batch: dict) -> None:
        # load raw data to convert types such as datetimes
        loaded, _ = _payment_transaction_batch_schema.load(payment_transaction_batch)  # type: ignore

        transactions_batch = PaymentTransactionBatchModel(**loaded)

        try:
            _ = dump_payment_transaction_batch_schema(transactions_batch)
        except DumpTransactionsError as e:
            assert False, f"dump_payment_transaction_batch_schema raises {e}"
