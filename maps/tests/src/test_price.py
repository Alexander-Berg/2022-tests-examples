import pytest
import maps.automotive.libs.large_tests.lib.payment as payment
import lib.server as server
import lib.tariff as tariff

from lib.helper import wait_for, get_order_from_db
from data_types.order import Order, Car
from data_types.user import User, Phone


@pytest.mark.parametrize("price_to_set,price_to_check", [
    (None, "100"),  # default fake-env value
    ("299", "299"),
    (399, "299"),  # previous valid
    ("399.23", "299"),  # previous valid
    ("99999", "299"),  # too big, previous valid
    ("1", "299"),  # too small, previous valid
    ("-100", "299"),
    ("423", "423"),
])
def test_tariff_price(user, price_to_set, price_to_check):
    order = Order(phone=user.phones[0], car=Car())

    tariff.set_tariff_price(price_to_set)

    payment_info = server.post_order(user=user, order=order) >> 200
    pay_id = payment_info["order_id"]
    assert payment.get_order(pay_id)["data"]["items"][0]["price"] == price_to_check
