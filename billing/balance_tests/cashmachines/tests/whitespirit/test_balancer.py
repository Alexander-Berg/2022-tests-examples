# coding: utf-8

__author__ = 'a-vasin'

import pytest
from hamcrest import equal_to

import cashmachines.whitespirit_steps as steps
from btestlib import utils


def test_ping():
    raw_response, _ = steps.BalancerSteps.ping()
    status_code = raw_response.status_code
    utils.check_that(status_code, equal_to(200), u'Проверяем, что получили ожидаемый ответ')


def test_hudsucker():
    raw_response, _ = steps.BalancerSteps.hudsucker()
    status_code = raw_response.status_code
    utils.check_that(status_code, equal_to(200), u'Проверяем, что получили ожидаемый ответ')


def test_big_receipt():
    receipt = steps.CMSteps.make_big_single_receipt()
    sn, doc_number = receipt['kkt']['sn'], receipt['id']

    document = steps.ReceiptSteps.get_document(sn, doc_number, with_fullform=True)
    assert document
