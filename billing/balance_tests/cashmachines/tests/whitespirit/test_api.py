# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import pytest
from hamcrest import equal_to, empty, not_

import cashmachines.whitespirit_steps as steps
from btestlib import utils
from btestlib.constants import Firms
from cashmachines.cashmachines_utils import need_clean_up
from cashmachines.data.constants import Group
from cashmachines.data.defaults import STABLE_SN


@pytest.mark.parametrize("inn, group, expected_status_code", [
    (None, None, 200),
    (Firms.YANDEX_1.inn, None, 200),
    (u'1', None, 429),
    (Firms.YANDEX_1.inn, Group.DEFAULT, 200),
    (Firms.YANDEX_1.inn, Group.DEFAULT.lower(), 200),
    (Firms.YANDEX_1.inn, 'not_existing_group', 429)
], ids=['EMPTY', 'YANDEX', 'INCORRECT', 'GROUP_OK', 'GROUP_OK_LOWER', 'GROUP_ERROR'])
def test_ping(inn, group, expected_status_code):
    raw_response, _ = steps.StatusSteps.ping(inn, group)
    status_code = raw_response.status_code
    utils.check_that(status_code, equal_to(expected_status_code), u'Проверяем, что получили ожидаемый ответ')


@pytest.mark.parametrize("inn, group", [
    (None, Group.DEFAULT),
    (Firms.YANDEX_1.inn, '!!!')
], ids=['GROUP_WO_INN', 'INCORRECT_GROUP'])
def test_incorrect_ping(inn, group):
    with utils.check_mode(utils.CheckMode.IGNORED):
        _, response = steps.StatusSteps.ping(inn, group)
    utils.check_that(response.get('error', None), equal_to(u'BadDataInput'))


def test_ping_backlog_ratio():
    raw_response, _ = steps.StatusSteps.ping(backlog_ratio=2)
    status_code = raw_response.status_code
    utils.check_that(status_code, equal_to(200), u'Проверяем, что получили ожидаемый ответ')


@pytest.mark.parametrize("on", [True, False], ids=['ON', 'OFF'])
def test_ident(on):
    raw_response, _ = steps.AdminSteps.ident(STABLE_SN, on)
    utils.check_that(raw_response.status_code, equal_to(200), u'Проверяем, что получили ожидаемый ответ')


def test_log():
    log = steps.AdminSteps.log(STABLE_SN)
    utils.check_that(log, not_(empty()), u'Проверяем, что лог не пуст')


@pytest.mark.parametrize("dt", [None, datetime.now()], ids=['NONE', 'NOW'])
@need_clean_up
def test_set_datetime(dt):
    steps.ShiftSteps.close_shift(STABLE_SN)

    raw_response, _ = steps.AdminSteps.set_datetime(STABLE_SN, dt)
    status_code = raw_response.status_code
    utils.check_that(status_code, equal_to(200), u'Проверяем успешность установки даты')


def test_set_datetime_during_shift():
    with utils.check_mode(utils.CheckMode.IGNORED):
        _, response = steps.AdminSteps.set_datetime(STABLE_SN)
    utils.check_that(response.get('error', None), equal_to(u'WrongKKTState'))
