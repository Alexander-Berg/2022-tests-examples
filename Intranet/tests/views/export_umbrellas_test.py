from mock import patch, MagicMock

import json
from random import random, randint

from django.core.urlresolvers import reverse

from staff.lib.auth.auth_mechanisms import TVM

from staff.umbrellas.views import export_umbrellas_view


def test_export_umbrellas_view_not_valid(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = False
    form_class.return_value.errors_as_dict.return_value = {'test': random()}

    with patch('staff.umbrellas.views.export_umbrellas.ExportUmbrellasForm', form_class):
        response = export_umbrellas_view(request)
        form_class.assert_called_once_with(request.GET)

    assert response.status_code == 400, response.content
    assert json.loads(response.content) == form_class.return_value.errors_as_dict.return_value


def test_export_umbrella_assignments_view(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    cleaned_data = {
        'continuation_token': randint(1, 23123),
    }

    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = True
    form_class.return_value.cleaned_data = cleaned_data
    export = MagicMock(return_value={'test response': random()})

    with patch('staff.umbrellas.views.export_umbrellas.ExportUmbrellasForm', form_class):
        with patch('staff.umbrellas.views.export_umbrellas.export_umbrellas', export):
            response = export_umbrellas_view(request)
            form_class.assert_called_once_with(request.GET)
            export.asert_called_once_with(cleaned_data['continuation_token'])

    assert response.status_code == 200, response.content
    assert json.loads(response.content) == export.return_value


def _create_request(rf, test_data):
    request = rf.get(reverse('export-umbrellas'), data=test_data)
    request.auth_mechanism = TVM
    request.yauser = None
    request.user = MagicMock(is_superuser=True)
    return request
