# -*- coding: utf-8 -*-
import pytest

from balance.snout_steps import api_steps as steps
from btestlib.data.snout_constants import Handles
from balance.balance_steps.other_steps import UserSteps
from balance.tests.conftest import get_free_user

@pytest.mark.smoke
def test_manager_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/manager/list
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    steps.pull_handle_and_check_result(Handles.MANAGER, user=user)
