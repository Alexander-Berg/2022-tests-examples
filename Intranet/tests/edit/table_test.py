from mock import patch, MagicMock

import json
from random import random, randint

from django.core.urlresolvers import reverse
from staff.map.errors import MultipleTableError

from staff.lib.auth.auth_mechanisms import TVM
from staff.lib.testing import get_random_date

from staff.map.edit.table import book_table_for_person


def test_book_table_for_person_not_a_json(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    json_loads = MagicMock(side_effect=ValueError)

    with patch('json.loads', json_loads):
        response = book_table_for_person(request)

    assert response.status_code == 400, response.content


def test_book_table_for_person_form_not_valid(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = False
    form_class.return_value.errors_as_dict.return_value = {'test': random()}
    json_loads = MagicMock()

    with patch('staff.map.edit.table.BookTableForPersonForm', form_class):
        with patch('json.loads', json_loads):
            response = book_table_for_person(request)
            json_loads.assert_called_once_with(request.body)
            form_class.assert_called_once_with(json_loads.return_value)

    assert response.status_code == 400, response.content
    assert json.loads(response.content) == form_class.return_value.errors_as_dict.return_value


def test_book_table_for_person_controller_errors(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    cleaned_data = {
        'person': f'login {random()}',
        'table': randint(1, 1999921),
        'date_from': get_random_date(),
        'date_to': get_random_date(),
    }

    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = True
    form_class.return_value.cleaned_data = cleaned_data
    json_loads = MagicMock()
    e = MultipleTableError([{'code': f'err {i}'} for i in range(randint(2, 5))])

    def controller_side_effect(*_, **_2):
        raise e

    controller = MagicMock(side_effect=controller_side_effect)

    with patch('staff.map.edit.table.BookTableForPersonForm', form_class):
        with patch('json.loads', json_loads):
            with patch('staff.map.edit.table.book_table_for_person_controller', controller):
                response = book_table_for_person(request)
                json_loads.assert_called_once_with(request.body)
                form_class.assert_called_once_with(json_loads.return_value)
                controller.asert_called_once_with(
                    cleaned_data['person'],
                    cleaned_data['table'],
                    cleaned_data['date_from'],
                    cleaned_data['date_to'],
                )

    assert response.status_code == 400, response.content
    assert json.loads(response.content) == {'errors': [error for error in e.errors]}


def test_book_table_for_person(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    cleaned_data = {
        'person': f'login {random()}',
        'table': randint(1, 1999921),
        'date_from': get_random_date(),
        'date_to': get_random_date(),
    }

    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = True
    form_class.return_value.cleaned_data = cleaned_data
    json_loads = MagicMock()
    controller = MagicMock(return_value={'test response': random()})

    with patch('staff.map.edit.table.BookTableForPersonForm', form_class):
        with patch('json.loads', json_loads):
            with patch('staff.map.edit.table.book_table_for_person_controller', controller):
                response = book_table_for_person(request)
                json_loads.assert_called_once_with(request.body)
                form_class.assert_called_once_with(json_loads.return_value)
                controller.asert_called_once_with(
                    cleaned_data['person'],
                    cleaned_data['table'],
                    cleaned_data['date_from'],
                    cleaned_data['date_to'],
                )

    assert response.status_code == 200, response.content
    assert json.loads(response.content) == controller.return_value


def _create_request(rf, test_data):
    request = rf.post(reverse('book-table-for-person'), data=test_data)
    request.auth_mechanism = TVM
    request.yauser = None
    request.user = MagicMock(is_superuser=True)
    return request
