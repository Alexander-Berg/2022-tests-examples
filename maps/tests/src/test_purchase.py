import pytest
import time

import lib.alfred as alfred
import maps.automotive.libs.large_tests.lib.datasync as datasync
import maps.automotive.libs.large_tests.lib.payment as payment
import lib.server as server

from lib.helper import wait_for, get_order_from_db
from data_types.order import Order, Car
from data_types.user import User, Phone


def test_post_order(user):
    server.get_active_order(user=user) >> 204

    order = Order(phone=user.phones[0], car=Car())
    payment_info = server.post_order(user=user, order=order) >> 200
    pay_id = payment_info["order_id"]

    assert (server.get_active_order(user=user) >> 200)["status"] == "Created"

    assert payment.get_order(pay_id)["data"]["pay_status"] == "new"

    assert len(alfred.get_alfred_orders().items()) == 0

    payment.set_order_pay_state(pay_id, "held")
    assert (server.get_active_order(user=user) >> 200)["status"] == "Created"

    assert wait_for(lambda: payment.get_order(pay_id)["data"]["pay_status"] == "in_progress")

    datasync_coupons = datasync.get_datasync_data(user)
    assert len(datasync_coupons.items()) == 1

    datasync_coupon = list(datasync_coupons.items())[0][1]

    assert order.car.plate == datasync_coupon["plate"]
    assert order.car.title == datasync_coupon["car_title"]
    assert order.carwash_id == datasync_coupon["carwashId"]
    assert "Active" == datasync_coupon["status"]

    alfred_orders = alfred.get_alfred_orders()
    assert len(alfred_orders.items()) == 1

    _, alfred_order = list(alfred_orders.items())[0]
    assert datasync_coupon["code"] == alfred_order["code"]
    assert order.car.plate == alfred_order["user_car_number"]
    assert order.car.title == alfred_order["user_car_title"]
    assert user.phones[0].number[1:] == alfred_order["user_phone"]

    assert (server.get_active_order(user=user) >> 200)["status"] == "Active"

    payment.set_order_pay_state(pay_id, "paid")
    assert wait_for(lambda: get_order_from_db(pay_id)["order_status"] == "Paid")

    assert (server.get_active_order(user=user) >> 200)["status"] == "Active"

    order_from_db = get_order_from_db(pay_id)
    assert order_from_db["order_status"] == "Paid"
    assert order_from_db["car_plate"] is None


def test_rejected_order(user):
    order = Order(phone=user.phones[0], car=Car())

    payment_info = server.post_order(user=user, order=order) >> 200
    pay_id = payment_info["order_id"]

    assert (server.get_active_order(user=user) >> 200)["status"] == "Created"

    assert payment.get_order(pay_id)["data"]["pay_status"] == "new"

    payment.set_order_pay_state(pay_id, "rejected")
    assert wait_for(lambda: get_order_from_db(pay_id)["order_status"] == "Failed")

    server.get_active_order(user=user) >> 204

    assert not datasync.get_datasync_data(user)


def cancel_order_after_hold(pay_id):
    payment.set_order_pay_state(pay_id, "held")
    assert wait_for(lambda: payment.get_order(pay_id)["data"]["pay_status"] == "in_cancel")
    payment.set_order_pay_state(pay_id, "cancelled")


def cancel_order_by_reject(pay_id):
    payment.set_order_pay_state(pay_id, "rejected")


def cancel_order_by_abandon(pay_id):
    payment.set_order_pay_state(pay_id, "abandoned")


@pytest.mark.parametrize("cancelation_scenario", [cancel_order_after_hold, cancel_order_by_reject])
def test_cancel_order_on_purchase(user, cancelation_scenario):
    server.get_active_order(user=user) >> 204

    order1 = Order(phone=user.phones[0], car=Car())
    order2 = Order(phone=user.phones[0], car=Car())

    payment_info = server.post_order(user=user, order=order1) >> 200
    pay_id1 = payment_info["order_id"]

    payment_info = server.post_order(user=user, order=order2) >> 200
    pay_id2 = payment_info["order_id"]

    assert pay_id1 != pay_id2

    assert get_order_from_db(pay_id1)["order_status"] == "Canceling"
    assert get_order_from_db(pay_id2)["order_status"] == "NotPaid"

    cancelation_scenario(pay_id1)
    assert wait_for(lambda: get_order_from_db(pay_id1)["order_status"] == "Cancelled")

    payment.set_order_pay_state(pay_id2, "held")
    assert (server.get_active_order(user=user) >> 200)["status"] == "Created"
    assert wait_for(lambda: payment.get_order(pay_id2)["data"]["pay_status"] == "in_progress")
    payment.set_order_pay_state(pay_id2, "paid")
    assert wait_for(lambda: get_order_from_db(pay_id2)["order_status"] == "Paid")

    datasync_coupons = datasync.get_datasync_data(user)
    assert len(datasync_coupons.items()) == 1

    datasync_coupon = list(datasync_coupons.items())[0][1]
    server_coupon = server.get_active_order(user=user) >> 200

    assert datasync_coupon["code"] and datasync_coupon["code"] == server_coupon["code"]
    assert order2.car.plate == datasync_coupon["plate"]
    assert server_coupon["plate"] is None
    assert order2.car.title == datasync_coupon["car_title"] == server_coupon["car_title"]
    assert order2.carwash_id == datasync_coupon["carwashId"] == server_coupon["carwashId"]
    assert "Active" == datasync_coupon["status"] == server_coupon["status"]


def test_wrong_phone_id():
    user = User(phones=[Phone(), Phone(is_confirmed=False)])
    user.register()

    order = Order(phone=user.phones[1], car=Car())

    error = server.post_order(user=user, order=order) >> 422
    assert error["code"] == "INVALID_PHONE_ID"


@pytest.mark.parametrize("car_plate,is_valid", [
    ("А777АА77", True),
    ("В123ХУ456", True),
    ("А777АА", False),
    ("A777BC77", True),  # in english
    ("777АС77", False),
    ("А777АА7777", False),
    ("АА777АА77", False),
    ("T777АX77", True),  # english and russian
])
def test_car_plate(user, car_plate, is_valid):
    order = Order(phone=user.phones[0], car=Car(plate=car_plate))
    if is_valid:
        payment_info = server.post_order(user=user, order=order) >> 200
        pay_id = payment_info["order_id"]
        payment.set_order_pay_state(pay_id, "held")

        assert wait_for(lambda: payment.get_order(pay_id)["data"]["pay_status"] == "in_progress")

        order = list(alfred.get_alfred_orders().values())[0]

        symbols = (
            u"ABEKMHOPCTXY",  # eng
            u"АВЕКМНОРСТХУ"  # ru
        )
        tr = {ord(a): ord(b) for a, b in zip(*symbols)}
        assert order["user_car_number"] == car_plate.translate(tr)
    else:
        response = server.post_order(user=user, order=order) >> 422
        assert response["code"] == "INVALID_CAR_PLATE"


@pytest.mark.parametrize("number", [
    "+91112223344",
    "+731112223344",
    "+7321112223344",
    "+74321112223344",
])
def test_with_not_russian_phone(number):
    user = User(phones=[Phone(number=number)])
    user.register()

    order = Order(phone=user.phones[0], car=Car())

    error = server.post_order(user=user, order=order) >> 422
    assert error["code"] == "INVALID_PHONE_ID"


def test_hanged_not_paid_order(user):
    order = Order(phone=user.phones[0], car=Car())
    payment_info = server.post_order(user=user, order=order) >> 200
    pay_id = payment_info["order_id"]

    assert wait_for(lambda: payment.get_order(pay_id)["data"]["pay_status"] == "cancelled", timeout=10)
    assert wait_for(lambda: get_order_from_db(pay_id)["order_status"] in ["Canceling", "Cancelled"])
