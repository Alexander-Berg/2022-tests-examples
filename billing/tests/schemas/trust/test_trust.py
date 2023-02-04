import os
from typing import Any

import pytest

from billing.library.python.calculator.models.trust import PaymentModel
from billing.library.python.calculator.schemas.trust import PaymentSchema
from billing.library.python.calculator.test_utils.reader import from_file


_payment_schema = PaymentSchema()
TEST_PAYMENTS_DATA_FILENAME = os.path.join(os.path.dirname(__file__), 'payments.jsonlines')


@pytest.mark.parametrize(
    'data',
    [pytest.param(data, id=f'line_{lineno}') for lineno, data in from_file(TEST_PAYMENTS_DATA_FILENAME)]
)
def test_load(data: dict[str, Any]) -> None:
    loaded, _ = _payment_schema.load(data)
    PaymentModel(**loaded)  # type: ignore
