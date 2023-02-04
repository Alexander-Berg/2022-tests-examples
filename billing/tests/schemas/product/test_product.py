import os
from typing import Any

import pytest

from billing.library.python.calculator.models.product import MDHProductRowModel
from billing.library.python.calculator.schemas.product import MDHProductRowSchema
from billing.library.python.calculator.test_utils.reader import from_file


_product_row_schema = MDHProductRowSchema()
TEST_PRODUCTS_DATA_FILENAME = os.path.join(os.path.dirname(__file__), 'products.jsonlines')


@pytest.mark.parametrize(
    'data',
    [pytest.param(data, id=f'line_{lineno}') for lineno, data in from_file(TEST_PRODUCTS_DATA_FILENAME)]
)
def test_load(data: dict[str, Any]) -> None:
    loaded, _ = _product_row_schema.load(data)
    MDHProductRowModel(**loaded)  # type: ignore
