from datetime import datetime
from decimal import Decimal

from billing.library.python.calculator.models.product import ProductModel
from billing.library.python.calculator.services.product import ProductService
from billing.library.python.calculator.util import to_msk_dt


def test_price_by_date_not_found(product_model: ProductModel) -> None:
    assert ProductService(product_model).price_by_date(to_msk_dt(datetime(2015, 1, 1))) is None


def test_price_by_date_found(product_model: ProductModel) -> None:
    price = ProductService(product_model).price_by_date(to_msk_dt(datetime(2016, 1, 1)))
    assert price is not None and price.price == Decimal("0.01")


def test_tax_by_date_not_found(product_model: ProductModel) -> None:
    assert ProductService(product_model).tax_by_date(to_msk_dt(datetime(2015, 1, 1)), region_id=1) is None


def test_tax_by_date_found(product_model: ProductModel) -> None:
    tax = ProductService(product_model).tax_by_date(to_msk_dt(datetime(2016, 1, 1)), region_id=225)
    assert tax is not None and tax.tax_policy.id == 10
