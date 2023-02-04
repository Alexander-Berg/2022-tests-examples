import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils

SERVICE_LIST = [102]
# TASK_LIST = ['get_stat_from_bk']
TASK_LIST = ['adfox_alignment']
STATUS_ID_NAME_MAPPING = {0: 'new',
                          1: 'open',
                          2: 'resolved',
                          3: 'stalled'}

ACTION_LIST = ['open', 'resolve', 'stall', 'reopen']


@pytest.mark.parametrize('service', SERVICE_LIST)
@pytest.mark.parametrize('task', TASK_LIST)
def test_mnclose_status_getter(service, task):
    service_token = db.get_service_by_id(service)[0]['token']
    result = steps.CloseMonth.get_mnclose_status(service_token, task)
    state_id = steps.CloseMonth.get_last_state_change(task)['state_id']
    utils.check_that(result, equal_to(STATUS_ID_NAME_MAPPING[state_id]))

# @pytest.mark.parametrize('service', SERVICE_LIST)
# @pytest.mark.parametrize('task', TASK_LIST)
# @pytest.mark.parametrize('action', ACTION_LIST)
# def test_mnclose_status_setter(service, task, action):
#     service_token = db.get_service_by_id(service)[0]['token']
#     steps.CloseMonth.set_mnclose_status(service_token, task, action)
#     state_id = steps.CloseMonth.get_last_state_change(task)['state_id']
#
