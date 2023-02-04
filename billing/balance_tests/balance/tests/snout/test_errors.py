# -*- coding: utf-8 -*-
import pytest
import copy

import balance.balance_db as db
from balance.snout_steps import api_steps as steps
from balance.balance_steps.other_steps import UserSteps
from btestlib.data.snout_constants import Handles, ErrorData


@pytest.mark.parametrize('handle, error_value', [
    pytest.mark.smoke((Handles.CLIENT_PERSON, ErrorData.ERROR_ID)),
    (Handles.CONTRACT_CLIENT_CREDIT_LIMIT, ErrorData.ERROR_ID),
    (Handles.EDO_PERSON_ACTUAL_OFFERS, ErrorData.ERROR_ID),
    (Handles.EDO_PERSON_CONTRACTS, ErrorData.ERROR_ID),
], ids=lambda handle, error_value: handle)
def test_client_no_found_error(handle, error_value, get_free_user):
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    query = "select bo.S_CLIENT_ID.nextval id from dual"
    client_id = db.balance().execute(query)[0]['id']
    expected_error = copy.copy(ErrorData.CLIENT_NOT_FOUND)
    steps.pull_handle_and_check_result(handle, client_id, additional_params=None,
                                       status_code=404, expected_error=expected_error, user=user)
