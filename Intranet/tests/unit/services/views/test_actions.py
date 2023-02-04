from functools import partial

import pretend
import pytest
from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.urlresolvers import reverse
from mock import patch

from plan.services.models import Service
from common import factories
from utils import Response

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, staff_factory):
    staff1 = staff_factory()
    metaservice = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    service1 = factories.ServiceFactory(parent=metaservice, owner=staff_factory())
    fixture = pretend.stub(
        staff1=staff1,
        metaservice=metaservice,
        service1=service1,
    )
    return fixture


def test_cannot_restore_service_brutally():
    service = factories.ServiceFactory(state=Service.states.DELETED)

    service.state = Service.states.IN_DEVELOP
    with pytest.raises(ValidationError):
        service.save()


@pytest.fixture
def _request_role(client, service, role, person):
    client.login(person.login)
    return partial(
        client.json.post,
        reverse('api-v4:service-member-list'),
        {
            'service': service.id,
            'person': person.login,
            'role': role.id,
        }
    )


def test_idm_error_1(_request_role, patch_tvm):
    idm_error = '{"error": "qqq"}'

    with patch('plan.idm.manager.Manager._run_request') as _run_request:
        _run_request.return_value = Response(400, idm_error)

        response = _request_role()

    assert response.status_code == 400

    result = response.json()
    assert result['error']['code'] == 'idm_bad_request'
    assert result['error']['detail'] == 'IDM did not fulfil the request.'
    assert result['error']['extra']['message'] == 'qqq'


def test_idm_error_2(_request_role, patch_tvm):
    idm_error = '''
        {
            "error": {
                "error_code": "NOT_FOUND",
                "message": "qqq"
            }
        }
    '''

    with patch('plan.idm.manager.Manager._run_request') as _run_request:
        _run_request.return_value = Response(404, idm_error)

        response = _request_role()

    assert response.status_code == 404

    result = response.json()
    assert result['error']['code'] == 'idm_not_found'
    assert result['error']['detail'] == 'IDM returned not found.'
    assert result['error']['extra']['message'] == 'qqq'


def test_idm_error_3(_request_role, patch_tvm):
    idm_error = '''
        {
            "error_code": "BAD_REQUEST",
            "errors": {
                "status": ["Field required"]
            },
            "message": "Invalid data sent"
        }
    '''

    with patch('plan.idm.manager.Manager._run_request') as _run_request:
        _run_request.return_value = Response(400, idm_error)

        response = _request_role()

    assert response.status_code == 400

    result = response.json()
    assert result['error']['code'] == 'idm_bad_request'
    assert result['error']['detail'] == 'IDM did not fulfil the request.'
    assert result['error']['extra']['message'] == 'Invalid data sent'
    assert result['error']['extra']['errors'] == {'status': ['Field required']}
