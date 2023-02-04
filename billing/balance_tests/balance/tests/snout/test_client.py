# -*- coding: utf-8 -*-
import pytest

import btestlib.reporter as reporter

from balance import balance_db as db
from balance import balance_steps as b_steps
from balance.balance_steps.other_steps import UserSteps
from balance.features import Features
from balance.snout_steps import api_steps as steps

from btestlib.data.defaults import Users
from btestlib.data.snout_constants import Handles

pytestmark = [reporter.feature(Features.UI, Features.CLIENT)]


@pytest.mark.smoke
def test_client_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/client&client_id=XXpython -m dev.command_line flask -- --versionX
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    client_id = b_steps.ClientSteps.get_client_id_by_passport_id(Users.CLIENT_TESTBALANCE_MAIN.uid)
    steps.pull_handle_and_check_result(Handles.CLIENT, client_id, user=user)


def test_client_person_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/client/person?mode=ALL&client_id=XXX
    """
    client_id = b_steps.ClientSteps.get_client_id_by_passport_id(Users.CLIENT_TESTBALANCE_MAIN.uid)
    steps.pull_handle_and_check_result(Handles.CLIENT_PERSON, client_id, {'mode': 'ALL'},
                                       custom_headers={'X-Is-Admin': 'false'})


@pytest.mark.skip
def test_client_representatives_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/client/representatives
    """
    db.balance().execute('''update bo.t_passport set is_main=1 where passport_id=uid''',
                         {'uid': str(Users.CLIENT_TESTBALANCE_MAIN.uid)})
    steps.pull_handle_and_check_result(Handles.CLIENT_REPRESENTATIVES)
