# -*- coding: utf-8 -*-
import pytest

import btestlib.reporter as reporter
from balance.features import Features
from balance.snout_steps import api_steps as steps
from balance.balance_steps.other_steps import UserSteps
from btestlib.data.snout_constants import Handles

pytestmark = [reporter.feature(Features.UI, Features.PERMISSION)]


@pytest.mark.smoke
def test_user_permissions_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/user/permissions
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    steps.pull_handle_and_check_result(Handles.USER, custom_headers={'X-Is-Admin': 'false'}, user=user)
