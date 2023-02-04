import time

import maps.automotive.libs.large_tests.lib.db as db
import maps.automotive.libs.large_tests.lib.payment as payment
import lib.server as server


def get_order_from_db(purchase_id):
    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(f"""
            SELECT id, order_id, car_plate, order_status, code_status
            FROM orders
            WHERE purchase_id={purchase_id:d}
        """)
        order = cur.fetchone()
        return {
            "id": order[0],
            "order_id": order[1],
            "car_plate": order[2],
            "order_status": order[3],
            "code_status": order[4]
        }


def get_refund_from_db(refund_id):
    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(f"""
            SELECT id, refund_status
            FROM refunds
            WHERE target_order_id={refund_id:d}
        """)
        refund = cur.fetchone()
        return {
            "id": refund[0],
            "refund_status": refund[1]
        }


def wait_for(predicat, timeout=10):
    start_t = time.time()
    while True:
        if predicat():
            return True
        if time.time() - start_t > timeout:
            return False
        time.sleep(timeout / 10)
    return False


def perform_payment(user, order):
    payment_info = server.post_order(user=user, order=order) >> 200
    pay_id = payment_info["order_id"]

    assert (server.get_active_order(user=user) >> 200)["status"] == "Created"

    payment.set_order_pay_state(pay_id, "held")
    assert wait_for(lambda: payment.get_order(pay_id)["data"]["pay_status"] == "in_progress")

    assert (server.get_active_order(user=user) >> 200)["status"] == "Active"

    payment.set_order_pay_state(pay_id, "paid")
    assert wait_for(lambda: get_order_from_db(pay_id)["order_status"] == "Paid")

    assert (server.get_active_order(user=user) >> 200)["status"] == "Active"
    return pay_id
