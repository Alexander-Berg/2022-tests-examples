# coding=utf-8
import pytest
from hamcrest import *

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import utils
from btestlib.data import defaults

PASSPORT_UID = defaults.PASSPORT_UID


@pytest.mark.parametrize('request_params, expected_error_message', [
    ({'person_type': 'ur'}, 'Invalid parameter for function: Missing client_id from client_hash'),
    ({'client_id': 1}, 'Invalid parameter for function: Missing type from client_hash')
])
def test_missing_param(request_params, expected_error_message):
    try:
        api.medium().CreatePerson(PASSPORT_UID, request_params)
    except Exception as Exc:
        utils.check_that(steps.CommonSteps.get_exception_code(Exc, 'msg'), equal_to(expected_error_message))
    else:
        utils.TestsError(u'CreatePerson works without required field')
