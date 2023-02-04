from mock import patch, MagicMock

import json
from random import random, randint

from django.core.urlresolvers import reverse

from staff.budget_position.views import attach_comment_view


def test_attach_comment_view_not_a_json(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    json_loads = MagicMock(side_effect=ValueError)

    with patch('json.loads', json_loads):
        response = attach_comment_view(request)

    assert response.status_code == 400, response.content


def test_attach_comment_view_not_valid(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    json_loads = MagicMock()
    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = False
    form_class.return_value.errors_as_dict.return_value = {'test': random()}

    with patch('json.loads', json_loads):
        with patch('staff.budget_position.views.front_views.BudgetPositionCommentForm', form_class):
            response = attach_comment_view(request)
            form_class.assert_called_once_with(json_loads.return_value)
            json_loads.assert_called_once_with(request.body)

    assert response.status_code == 400, response.content
    assert json.loads(response.content) == form_class.return_value.errors_as_dict.return_value


def test_attach_comment_view_view(rf):
    test_data = {'test data': random()}
    request = _create_request(rf, test_data)
    cleaned_data = {
        'budget_position': randint(1, 23123),
        'comment': f'comment {random()}',
    }

    json_loads = MagicMock()
    form_class = MagicMock()
    form_class.return_value.is_valid.return_value = True
    form_class.return_value.cleaned_data = cleaned_data
    controller = MagicMock()

    with patch('staff.budget_position.views.front_views.BudgetPositionCommentForm', form_class):
        with patch('staff.budget_position.views.front_views.attach_comment_to_budget_position_controller', controller):
            with patch('json.loads', json_loads):
                response = attach_comment_view(request)
                form_class.assert_called_once_with(json_loads.return_value)
                json_loads.assert_called_once_with(request.body)
                controller.asert_called_once_with(
                    request.user.get_profile.return_value,
                    cleaned_data['budget_position'],
                    cleaned_data['comment'],
                )

    assert response.status_code == 204, response.content


def _create_request(rf, test_data):
    request = rf.post(reverse('budget-position-api:attach-comment-to-vacancy'), data=test_data)
    request.user = MagicMock()
    return request
