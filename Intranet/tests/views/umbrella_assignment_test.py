import pytest
from mock import patch, MagicMock

import json
from random import random, randint

from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory

from staff.umbrellas.views import umbrella_assignment_view


def test_umbrella_assignment_view_not_a_json(rf):
    test_data = {'test data': random()}
    request = rf.post(reverse('edit-umbrella-assignment'), data=test_data)
    json_loads = MagicMock(side_effect=ValueError)

    with patch('json.loads', json_loads):
        response = umbrella_assignment_view(request)

    assert response.status_code == 400, response.content


def test_umbrella_assignment_view_not_valid(rf):
    test_data = {'test data': random()}
    request = rf.post(reverse('edit-umbrella-assignment'), data=test_data)
    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = False
    form_class.return_value.errors_as_dict.return_value = {'test': random()}
    json_loads = MagicMock()

    with patch('staff.umbrellas.views.umbrella_assignment.UmbrellaAssignmentForm', form_class):
        with patch('json.loads', json_loads):
            response = umbrella_assignment_view(request)
            json_loads.assert_called_once_with(request.body)
            form_class.assert_called_once_with(json_loads.return_value)

    assert response.status_code == 400, response.content
    assert json.loads(response.content) == form_class.return_value.errors_as_dict.return_value


@patch('staff.umbrellas.controllers.update_umbrella_assignments')
def test_umbrella_assignment_view_no_permissions(update_umbrella_assignments_mock, rf):
    test_data = {'test data': random()}
    request = rf.post(reverse('edit-umbrella-assignment'), data=test_data)
    request.user = MagicMock()
    logins = [f'login {random()} {i}' for i in range(randint(3, 7))]
    cleaned_data = {'persons': [MagicMock(login=login) for login in logins]}

    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = True
    form_class.return_value.cleaned_data = cleaned_data
    properties_class = MagicMock()
    properties_class.return_value.get_is_chief.return_value = False
    json_loads = MagicMock()

    with patch('staff.umbrellas.views.umbrella_assignment.UmbrellaAssignmentForm', form_class):
        with patch('staff.person_profile.permissions.properties.Properties', properties_class):
            with patch('json.loads', json_loads):
                response = umbrella_assignment_view(request)
                json_loads.assert_called_once_with(request.body)
                form_class.assert_called_once_with(json_loads.return_value)
                properties_class.assert_called_once_with(
                    target_logins=logins,
                    observer=request.user.get_profile(),
                    readonly=True,
                )
                assert properties_class.return_value.get_is_chief.mock_calls[0][1] == (logins[0],)

    update_umbrella_assignments_mock.assert_not_called()
    assert response.status_code == 403, response.content


@pytest.mark.django_db
@patch('staff.umbrellas.controllers.update_umbrella_assignments')
def test_umbrella_assignment_view(update_umbrella_assignments_mock, rf):
    test_data = {'test data': random()}
    request = rf.post(reverse('edit-umbrella-assignment'), data=test_data)
    request.user = MagicMock()
    logins = [f'login {random()} {i}' for i in range(randint(3, 7))]
    cleaned_data = {
        'persons': [StaffFactory(login=login) for login in logins],
        'umbrellas': MagicMock(),
    }

    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = True
    form_class.return_value.cleaned_data = cleaned_data
    properties_class = MagicMock()
    properties_class.return_value.get_is_chief.return_value = True
    json_loads = MagicMock()

    with patch('staff.umbrellas.views.umbrella_assignment.UmbrellaAssignmentForm', form_class):
        with patch('staff.person_profile.permissions.properties.Properties', properties_class):
            with patch('json.loads', json_loads):
                response = umbrella_assignment_view(request)
                json_loads.assert_called_once_with(request.body)
                form_class.assert_called_once_with(json_loads.return_value)
                properties_class.assert_called_once_with(
                    target_logins=logins,
                    observer=request.user.get_profile(),
                    readonly=True,
                )

                for i in range(len(logins)):
                    assert properties_class.return_value.get_is_chief.mock_calls[i][1] == (logins[i],)

    update_umbrella_assignments_mock.asert_called_once_with(cleaned_data['persons'], cleaned_data['umbrellas'])
    assert response.status_code == 204, response.content
