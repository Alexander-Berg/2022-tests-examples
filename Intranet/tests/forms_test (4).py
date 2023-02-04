import mock
import pytest

from staff.trip_questionary.forms import TripEmployeeForm


params = [
    (False, '', False),
    (True, '+79199977659', False),
    (True, '', True),
]


@pytest.mark.parametrize('need, number, has_error', params)
def test_taxi_phone_need_and_not_phone(need, number, has_error):
    parent_form = mock.Mock(**{'initial.return_value': {'is_new': True}})

    data = {'need_taxi': need, 'mobile_number_for_taxi': number}
    trip_form = TripEmployeeForm(data=data, parent_form=parent_form)
    assert ('mobile_number_for_taxi' in trip_form.errors) == has_error
