import inspect

import pytest
from django.core.urlresolvers import reverse

from plan.api.router import v4_router
import plan.api.mixins
import plan.resources.api.constructor

pytestmark = pytest.mark.django_db


def test_url_not_found(client):
    response = client.json.get('/this_url_doesnot_exists')

    assert response.status_code == 404
    assert response.json() == {
        'content': {},
        'error': {
            'message': 'Url not found',
        },
    }


def test_common_user(client, person, department):

    person.department = department
    person.save()

    client.login(person.login)

    response = client.json.get('/common/user/')
    assert response.status_code == 200

    result = response.json()['content']
    assert result == {
        'person': {
            'id': person.id,
            'login': person.login,
            'firstName': person.i_first_name,
            'lastName': person.i_last_name,
            'fullName': person.get_full_name(),
            'isDismissed': person.is_dismissed,
            'is_robot': person.is_robot,
            'affiliation': person.affiliation,
            'is_frozen': person.is_frozen,
        }
    }


def test_current_user(client, person, department):

    person.department = department
    person.save()

    client.login(person.login)

    response = client.json.get(reverse('api-v3-common:user'))

    assert response.status_code == 200
    result = response.json()
    assert result['login'] == person.login
    assert result['id'] == person.staff_id
    assert result['abc_id'] == person.id
    assert result['department']['id'] == department.id


def test_all_views_in_v4_use_defaultfieldmixin():
    views_to_exclude = {
        plan.resources.api.constructor.V4ConstructorFormView,
        plan.services.api.moves.V4MovesView,
        plan.resources.api.financial.V4FinancialResourceRequestView,
        plan.oebs.api.views.V4OEBSAgreementView,
        plan.oebs.api.views.OEBSRespondView,
        plan.oebs.api.views.OEBSDeviationView,
    }
    registry_views = [item[1] for item in v4_router.registry]
    failed_views = []
    for view in registry_views:
        if view in views_to_exclude:
            continue
        if plan.api.mixins.DefaultFieldsMixin not in inspect.getmro(view):
            failed_views.append(view)
    assert not failed_views
