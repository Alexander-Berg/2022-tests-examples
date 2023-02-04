import pytest
from balance import balance_steps as steps
from btestlib import utils
from xmlrpclib import Fault
from hamcrest import equal_to


def test_check():
    client_id = steps.ClientSteps.create()
    steps.PersonSteps.create(client_id=client_id, type_='yt')
    with pytest.raises(Fault) as exc:
        steps.PersonSteps.create(client_id=client_id, type_='ph')
    # assert exc.value == steps.CommonSteps.get_exception_code(exc)
    utils.check_that('PERSON_TYPE_MISMATCH', equal_to(steps.CommonSteps.get_exception_code(exc.value)))
