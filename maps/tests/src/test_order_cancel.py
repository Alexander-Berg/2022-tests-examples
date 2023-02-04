import pytest
import time
import lib.server as server
import maps.automotive.libs.large_tests.lib.datasync as datasync
import logging

from lib.helper import wait_for, perform_payment, get_order_from_db
from data_types.order import Order, Car


def test_mark_order_used(user):
    order = Order(phone=user.phones[0], car=Car())

    pay_id = perform_payment(user=user, order=order)

    order_from_db = get_order_from_db(pay_id)
    server.mark_order_used(order_from_db["order_id"]) >> 200

    order_from_db = get_order_from_db(pay_id)
    assert order_from_db["code_status"] == "Used"

    server.get_active_order(user=user) >> 204

    assert wait_for(lambda: list(datasync.get_datasync_data(user).items())[0][1]["status"] == "Used")


def test_absent_order(user):
    server.mark_order_used("qweasdqwe") >> 404


def test_invalid_secret(user):
    server.mark_order_used("qweasdqwe", secret="qweasd") >> 401
