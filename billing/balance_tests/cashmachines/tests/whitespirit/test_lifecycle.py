# coding: utf-8
__author__ = 'a-vasin'

import pytest
from hamcrest import equal_to, contains_string

import cashmachines.whitespirit_steps as steps
from btestlib import utils
from cashmachines.cashmachines_utils import need_clean_up
from cashmachines.data import defaults
from cashmachines.data.constants import State, Group
from cashmachines.data.defaults import STABLE_SN


@pytest.mark.skip(reason="Fix this one after NDS20's hell")
@need_clean_up
def test_full_lifecycle():
    steps.CMSteps.reset_cashmachine(STABLE_SN)
    state = steps.StatusSteps.get_status(STABLE_SN)['state']
    utils.check_that(state, equal_to(State.NONCONFIGURED))

    steps.CMSteps.setup_cashmachine(STABLE_SN)
    state = steps.StatusSteps.get_status(STABLE_SN)['state']
    utils.check_that(state, equal_to(State.CLOSE_SHIFT))

    steps.ShiftSteps.open_shift(STABLE_SN)
    state = steps.StatusSteps.get_status(STABLE_SN)['state']
    utils.check_that(state, equal_to(State.OPEN_SHIFT))

    steps.ShiftSteps.close_shift(STABLE_SN)
    state = steps.StatusSteps.get_status(STABLE_SN)['state']
    utils.check_that(state, equal_to(State.CLOSE_SHIFT))


def test_open_opened_shift():
    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ShiftSteps.open_shift(STABLE_SN)
    utils.check_that(response['value'], contains_string(u'Смена открыта'), u'Проверяем текст ошибки')


@pytest.mark.skip(reason="Fix this one after NDS20's hell")
@need_clean_up
def test_close_closed_shift():
    steps.ShiftSteps.close_shift(STABLE_SN)
    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ShiftSteps.close_shift(STABLE_SN)
    utils.check_that(response['value'], contains_string(u'Смена не открыта'), u'Проверяем текст ошибки')


# a-vasin: TODO понять, что является важным параметром и почему =)
@pytest.mark.skip
@need_clean_up
def test_re_registration():
    steps.ShiftSteps.close_shift(STABLE_SN)

    config_params = defaults.config(STABLE_SN)
    config_params['reg_info']['ofd_inn'] = u'abc'

    steps.AdminSteps.configure(STABLE_SN, config_params)
    steps.CMSteps.wait_for_cashmachine(STABLE_SN, State.CLOSE_SHIFT)

    steps.AdminSteps.reboot(STABLE_SN)
    steps.CMSteps.wait_for_cashmachine(STABLE_SN, State.CLOSE_SHIFT)

    steps.AdminSteps.register(STABLE_SN, reregister=True)
    steps.CMSteps.wait_for_cashmachine(STABLE_SN, State.CLOSE_SHIFT)


@need_clean_up
def test_empty_group_configure():
    params = defaults.groups_config(STABLE_SN, groups=[])
    steps.AdminSteps.configure(STABLE_SN, params)

    status = steps.StatusSteps.get_status(STABLE_SN)
    utils.check_that(status['groups'], equal_to([Group.DEFAULT]))


def test_wrong_group_configure():
    params = defaults.groups_config(STABLE_SN, groups=['!!!'])
    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.AdminSteps.configure(STABLE_SN, params)

    utils.check_that(response.get('error', None), equal_to(u'BadDataInput'))
