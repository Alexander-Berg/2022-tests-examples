import io
import json

import pretend
import pytest
from mock import patch

from plan import exceptions
from plan.roles import views
from common import factories


@pytest.fixture
def data(db):
    service1 = factories.ServiceFactory(id=1, name='service1')
    service2 = factories.ServiceFactory(id=2, name='service2')
    scope = factories.RoleScopeFactory()

    fixture = pretend.stub(
        service1=service1,
        service2=service2,
        scope=scope,
    )
    return fixture


def render_create_role(request_factory, json_data):
    path = '/roles/create/'
    request = request_factory.post(
        path, data=json_data, content_type='application/json',
    )
    view = views.CreateView()
    # dirty test hack
    view.request = io.StringIO(json.dumps(json_data))
    response = view.post(request)
    return response


def test_unique_name_in_one_service(rf, data):
    name_dict = {
        'ru': 'new role name',
        'en': 'new role name',
    }
    new_role_data = {
        'name': name_dict,
        'scope': data.scope.id,
        'service': data.service1.id,
    }

    with patch('plan.api.idm.actions.add_role'):
        result = render_create_role(rf, new_role_data)

    assert type(result) == dict
    assert 'id' in result

    # try to create one more role with same name
    # VALIDATION_ERROR returns
    with pytest.raises(exceptions.ConflictError) as exc:
        render_create_role(rf, new_role_data)
    assert exc.value.error_code == 'VALIDATION_ERROR'


def test_unique_name_in_different_services(rf, data):
    name_dict = {
        'ru': 'name1',
        'en': 'name1',
    }
    new_role_data = {
        'name': name_dict,
        'scope': data.scope.id,
        'service': data.service1.id,
    }

    with patch('plan.api.idm.actions.add_role'):
        result = render_create_role(rf, new_role_data)

    assert type(result) == dict
    assert 'id' in result

    # try to create one more role with same name
    # in another service
    # everything should be ok
    new_role_data['service'] = data.service2.id

    with patch('plan.api.idm.actions.add_role'):
        result = render_create_role(rf, new_role_data)

    assert type(result) == dict
    assert 'id' in result
