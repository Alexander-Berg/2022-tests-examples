# -*- coding: utf-8 -*-
import pytest

from balance.balance_steps.other_steps import UserSteps
from balance.snout_steps import api_steps as steps
from balance.tests.conftest import get_free_user

from btestlib.data.snout_constants import Handles
from btestlib.data.snout_defaults import CommonDefaults


@pytest.mark.smoke
def test_service_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/service?service_id=XXX
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    steps.pull_handle_and_check_result(Handles.SERVICE, CommonDefaults.SERVICE, user=user)


def test_service_list_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/service/list
    """
    steps.pull_handle_and_check_result(Handles.SERVICE_LIST)
