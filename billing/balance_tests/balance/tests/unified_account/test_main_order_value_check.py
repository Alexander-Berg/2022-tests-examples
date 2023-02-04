# coding=utf-8
__author__ = 'aikawa'

import datetime

import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
import btestlib.reporter as reporter
from balance.features import Features

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

dt = datetime.datetime.now()


def make_one_order(client_id):
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    return order_id


def make_branch(client_id):
    order_id = make_one_order(client_id)
    parent_order_id = make_one_order(client_id)
    steps.OrderSteps.merge(parent_order_id, [order_id])
    return order_id, parent_order_id


@reporter.feature(Features.TO_UNIT)
def test_main_order_value_one_order():
    client_id = steps.ClientSteps.create()
    order_id = make_one_order(client_id)

    order = db.get_order_by_id(order_id)
    utils.check_that(order[0]['main_order'], equal_to(0))


@reporter.feature(Features.TO_UNIT)
def test_main_order_value_branch():
    client_id = steps.ClientSteps.create()
    order_id, parent_order_id = make_branch(client_id)

    order = db.get_order_by_id(order_id)
    utils.check_that(order[0]['main_order'], equal_to(0))

    parent_order = db.get_order_by_id(parent_order_id)
    utils.check_that(parent_order[0]['main_order'], equal_to(1))


@reporter.feature(Features.TO_UNIT)
def test_main_order_value_tree():
    client_id = steps.ClientSteps.create()
    order_id, parent_order_id = make_branch(client_id)
    grand_parent_order_id = make_one_order(client_id)
    steps.OrderSteps.merge(grand_parent_order_id, [parent_order_id])

    order = db.get_order_by_id(order_id)
    utils.check_that(order[0]['main_order'], equal_to(0))

    parent_order = db.get_order_by_id(parent_order_id)
    utils.check_that(parent_order[0]['main_order'], equal_to(0))

    grand_parent_order = db.get_order_by_id(grand_parent_order_id)
    utils.check_that(grand_parent_order[0]['main_order'], equal_to(1))


@reporter.feature(Features.TO_UNIT)
def test_main_order_value_destroy_group():
    value_to_exclude_from_group = [None, 0, -1]
    client_id = steps.ClientSteps.create()
    order_id, parent_order_id = make_branch(client_id)
    for value in value_to_exclude_from_group:
        steps.OrderSteps.merge(value, [order_id])

        parent_order = db.get_order_by_id(parent_order_id)
        utils.check_that(parent_order[0]['main_order'], equal_to(0))


if __name__ == "__main__":
    pytest.main("test_main_order_check.py -v")
