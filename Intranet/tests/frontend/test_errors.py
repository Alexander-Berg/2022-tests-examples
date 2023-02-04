# coding: utf-8
import json

import pytest
from django import forms

from review.lib import views
from review.lib import errors
from tests import helpers


def get_permission_denied_view(params):
    class View(views.View):
        def process_get(self, *args, **kwargs):
            raise errors.PermissionDenied(
                **params
            )
    return View


@pytest.mark.parametrize(
    'params', [
        {},
        {'type': 'review'},
        {'type': 'review', 'action': 'edit'},
        {'type': 'review', 'action': 'edit', 'id': 10},
    ]
)
def test_permission_denied_response(params, rf):
    request = rf.get('/whatever/')
    request.auth = None
    view = get_permission_denied_view(params).as_view()

    response = view(request)

    assert response.status_code == 403

    data = json.loads(response.content)
    helpers.assert_is_substructure(
        {
            'errors': {
                '*': {
                    'code': 'PERMISSION_DENIED',
                    'params': params,
                }
            }
        },
        data
    )


class SomeForm(forms.Form):
    count = forms.IntegerField()


class ViewWithForm(views.View):
    form_cls_get = SomeForm

    def process_get(self, *args, **kwargs):
        return {}


def test_form_errors_response(rf):
    request = rf.get('/whatever/', data={'count': 'NOT_INTEGER'})
    request.auth = None
    view = ViewWithForm.as_view()

    response = view(request)

    assert response.status_code == 400

    data = json.loads(response.content)
    helpers.assert_is_substructure(
        {
            'errors': {
                'count': {
                    'code': 'VALIDATION_ERROR',
                    'params': {},
                }
            }
        },
        data
    )
