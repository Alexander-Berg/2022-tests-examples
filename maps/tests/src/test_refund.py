import pytest

import lib.alfred as alfred
import maps.automotive.libs.large_tests.lib.datasync as datasync
import maps.automotive.libs.large_tests.lib.payment as payment
import lib.server as server
import lib.startrek as startrek

from lib.helper import wait_for, get_order_from_db, get_refund_from_db, perform_payment
from data_types.order import Order, Car
from data_types.user import Phone, User


def test_refund_order(user):
    order = Order(phone=user.phones[0], car=Car())

    pay_id = perform_payment(user=user, order=order)
    order_id = get_order_from_db(pay_id)["id"]

    alfred_orders = alfred.get_alfred_orders()
    assert len(alfred_orders.items()) == 1
    alfred_order_id, alfred_order = list(alfred_orders.items())[0]
    assert alfred_order["status"] == "new"

    code = list(datasync.get_datasync_data(user).items())[0][1]["code"]

    admin = "admin"
    server.post_refund(login=admin, code=code, phone=user.phones[0]) >> 403
    server.add_idm_role(admin)
    server.post_refund(login=admin, code=code, phone=user.phones[0]) >> 200

    assert alfred.get_alfred_orders()[alfred_order_id]["status"] == "cancelled"

    server.get_active_order(user=user) >> 204

    assert wait_for(lambda: list(datasync.get_datasync_data(user).items())[0][1]["status"] == "Refunded")

    assert wait_for(lambda: get_refund_from_db(order_id)["refund_status"] == "RefundStarted")
    assert len(startrek.get_startrek_issues()) == 1

    refund_id = get_refund_from_db(order_id)["id"]
    payment.set_order_refund_state(refund_id, "completed")

    assert wait_for(lambda: get_refund_from_db(order_id)["refund_status"] == "Refunded")

    issue = list(startrek.get_startrek_issues().items())[0][1]
    assert len(issue["comments"]) == 2
    assert issue["status"] == "closed"


def test_refund_with_wrong_phone(user):
    order = Order(phone=user.phones[0], car=Car())

    pay_id = perform_payment(user=user, order=order)
    order_id = get_order_from_db(pay_id)["id"]

    code = list(datasync.get_datasync_data(user).items())[0][1]["code"]

    admin = "admin"
    server.add_idm_role(admin)
    server.post_refund(login=admin, code=code, phone=Phone()) >> 412

    assert len(startrek.get_startrek_issues()) == 0
