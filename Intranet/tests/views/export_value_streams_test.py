from mock import patch, MagicMock

import json
from random import random, randint

from django.core.urlresolvers import reverse

from staff.lib.auth.auth_mechanisms import TVM

from staff.budget_position.views import export_person_value_streams_view


def test_export_person_value_streams_view_not_a_json(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    json_loads = MagicMock(side_effect=ValueError)

    with patch('json.loads', json_loads):
        response = export_person_value_streams_view(request)

    assert response.status_code == 400, response.content


def test_export_person_value_streams_view_not_valid(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = False
    form_class.return_value.errors_as_dict.return_value = {'test': random()}
    json_loads = MagicMock()

    with patch('staff.budget_position.views.export_value_streams.ExportPersonInfoForm', form_class):
        with patch('json.loads', json_loads):
            response = export_person_value_streams_view(request)
            json_loads.assert_called_once_with(request.body)
            form_class.assert_called_once_with(json_loads.return_value)

    assert response.status_code == 400, response.content
    assert json.loads(response.content) == form_class.return_value.errors_as_dict.return_value


def test_export_person_value_streams_view(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    logins = [f'login {random()} {i}' for i in range(randint(3, 7))]
    cleaned_data = {
        'persons': [f'other login {login}' for login in logins],
    }

    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = True
    form_class.return_value.cleaned_data = cleaned_data
    json_loads = MagicMock()
    export = MagicMock(return_value={'test response': random()})

    with patch('staff.budget_position.views.export_value_streams.ExportPersonInfoForm', form_class):
        with patch('staff.budget_position.views.export_value_streams.export_person_value_streams_controller', export):
            with patch('json.loads', json_loads):
                response = export_person_value_streams_view(request)
                json_loads.assert_called_once_with(request.body)
                form_class.assert_called_once_with(json_loads.return_value)
                export.asert_called_once_with(cleaned_data['persons'])

    assert response.status_code == 200, response.content
    assert json.loads(response.content) == export.return_value


def _create_request(rf, test_data):
    request = rf.post(reverse('budget-position-api:export-person-value-streams'), data=test_data)
    request.auth_mechanism = TVM
    request.yauser = None
    request.user = MagicMock(is_superuser=True)
    return request
