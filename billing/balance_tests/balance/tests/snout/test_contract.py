# -*- coding: utf-8 -*-
import pytest

import btestlib.reporter as reporter
from balance.features import Features
from balance.balance_steps.other_steps import UserSteps
from balance.snout_steps import api_steps as steps
from btestlib.data.snout_constants import Handles

pytestmark = [reporter.feature(Features.UI, Features.CONTRACT)]


@pytest.mark.smoke
def test_contract_client_credit_limit_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/contract/client-credit/limit?client_id=XXX
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    client_id, _, _ = steps.create_contract()
    steps.pull_handle_and_check_result(Handles.CONTRACT_CLIENT_CREDIT_LIMIT, client_id, user=user)
