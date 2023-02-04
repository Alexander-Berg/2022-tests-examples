import json
from textwrap import dedent

import pretend
import pytest
from django.core.urlresolvers import reverse
from django.conf import settings
from mock import patch

from plan.common.utils.oauth import get_abc_zombik
from plan.resources.api.base import make_signature
from plan.resources.models import ServiceResource, ResourceType
from common import factories
from utils import Response
from unit.resources.test_request import fake_form_metadata

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, owner_role, staff_factory):
    staff = staff_factory()
    stranger = staff_factory()
    service1 = factories.ServiceFactory(owner=staff)
    service2 = factories.ServiceFactory()
    factories.ServiceMemberFactory(service=service1, role=owner_role, staff=staff)
    resource_type = factories.ResourceTypeFactory(
        import_plugin='generic',
        code=settings.TVM_RESOURCE_TYPE_CODE,
        import_link='http://yandex.ru/?consumer=abc',
        supplier_plugin='tvm',
        has_automated_grant=True,
        form_id=1,
        form_handler=dedent('''
        from plan.resources.handlers.tvm.forward import process_form_forward
        result = process_form_forward(data, form_metadata, cleaned_data)
        '''),
        form_back_handler=dedent('''
        from plan.resources.handlers.tvm.backward import process_form_backward
        result = process_form_backward(attributes, form_metadata)
        '''),
    )
    resource_type.consumer_roles.add(owner_role)
    fixture = pretend.stub(
        resource_type=resource_type,
        service1=service1,
        service2=service2,
        staff=staff,
        stranger=stranger,
    )
    return fixture


@pytest.fixture
def move_data(db, data, client, owner_role, staff_factory):
    superuser = factories.StaffFactory(user=factories.UserFactory(is_superuser=True))
    client.login(superuser.login)

    res = factories.ResourceFactory(
        external_id='100500',
        name='name',
        type=data.resource_type,
    )

    sr = factories.ServiceResourceFactory(
        service=data.service1,
        resource=res,
        state='granted',
        type=data.resource_type,
    )

    signature = make_signature(
        service=data.service2,
        resource_type=data.resource_type,
        user=superuser
    )

    fixture = pretend.stub(
        data=data,
        superuser=superuser,
        signature=signature,
        service_resource=sr,
        resource=res,
        client=client,
    )
    return fixture


def _make_move_request(move_data, **kwargs):
    response = move_data.client.post(
        reverse('resources-api:request-list'),
        {
            'field_1': '{"question": {"id": 2, "slug": "tvm_client_id"}, "value": "100500"}',
        },
        **{
            'HTTP_X_FORM_ANSWER_ID': 1,
            'HTTP_X_ABC_USER': move_data.superuser.login,
            'HTTP_X_ABC_SERVICE': move_data.data.service2.pk,
            'HTTP_X_ABC_RESOURCE_TYPE': move_data.data.resource_type.pk,
            'HTTP_X_ABC_SIGNATURE': move_data.signature,
            'HTTP_X_YA_USER_TICKET': 'some_ticket',
            **kwargs
        }
    )
    return response


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@pytest.mark.usefixtures('robot')
def test_tvm(request, client, data):
    client.login(data.staff.user.username)
    request.return_value = Response(200, '{}')

    # дернуть ручку реквест и убедиться что будет дернута ручка tvm
    signature = make_signature(
        service=data.service1,
        resource_type=data.resource_type,
        user=data.staff.user
    )

    with patch('plan.resources.tasks._send_resource_to_supplier') as send:
        request.return_value = Response(200, '{"status": "ok", "client_id": 1}')

        response = client.post(
            reverse('resources-api:request-list'),
            {
                'field_2': '{"question": {"id": 2, "slug": "resource_name"}, "value": "resource_name"}',
            },
            **{
                'HTTP_X_FORM_ANSWER_ID': 1,
                'HTTP_X_ABC_USER': data.staff.login,
                'HTTP_X_ABC_SERVICE': data.service1.pk,
                'HTTP_X_ABC_RESOURCE_TYPE': data.resource_type.pk,
                'HTTP_X_ABC_SIGNATURE': signature,
            }
        )

    assert response.status_code == 200

    sr = ServiceResource.objects.get()
    assert send.called
    send.assert_called_with(sr)


def test_metainfo(client, data, owner_role):
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
        attributes={'client_id': 1},
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.service2,
        state='granted',
    )
    factories.ServiceMemberFactory(
        service=data.service2,
        role=owner_role,
        staff=data.staff,
    )

    client.login(data.staff.login)

    with patch('requests.sessions.Session.request') as request:
        request.return_value = Response(200, '{"status":"ok","content":[{"attributes": {"a":"b"}}]}')
        with patch('plan.resources.suppliers.tvm.get_tvm_ticket') as get_tvm_ticket:
            get_tvm_ticket.return_value = 'some_ticket'
            response = client.json.post(
                reverse('resources-api:serviceresources-secrets', args=[service_resource.pk]),
                {'action': 'meta_info'},
            )

    assert response.status_code == 200
    assert response.json() == {'result': {'a': 'b'}}


def test_metainfo_by_stranger(client, data, owner_role, staff_factory):
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
        attributes={'client_id': 1},
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.service2,
        state='granted',
    )
    factories.ServiceMemberFactory(
        service=data.service2,
        role=owner_role,
        staff=data.staff,
    )

    stranger = staff_factory()
    client.login(stranger.login)

    with patch('requests.sessions.Session.request') as request:
        request.return_value = Response(200, '{"status": "error", "errors": ["abc_team.member_required"]}')

        response = client.json.post(
            reverse('resources-api:serviceresources-secrets', args=[service_resource.pk]),
            {'action': 'meta_info'},
        )

    assert response.status_code == 403
    assert response.json()['error']['message']['ru'] == 'У вас недостаточно прав для просмотра мета-информации'


def test_metainfo_not_granted(client, data, owner_role):
    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
        attributes={'client_id': 1},
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.service2,
        state='requested',
    )
    factories.ServiceMemberFactory(
        service=data.service2,
        role=owner_role,
        staff=data.staff,
    )

    client.login(data.staff.login)

    response = client.json.post(
        reverse('resources-api:serviceresources-secrets', args=[service_resource.pk]),
        {'action': 'meta_info'},
    )

    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'Работа с секретами возможна только у выданного ресурса'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_edit_resource(request, client, data):
    request.return_value = Response(200, '{}')

    client.login(data.staff.login)

    res = factories.ResourceFactory(
        external_id='external_id',
        name='name',
        type=data.resource_type,
    )
    sr = factories.ServiceResourceFactory(
        service=data.service1,
        state=ServiceResource.GRANTED,
        resource=res,
    )

    signature = make_signature(
        service=data.service1,
        resource_type=data.resource_type,
        user=data.stranger.user
    )

    with patch('requests.request') as request:
        request.return_value = Response(200, '{"status": "ok", "client_id": 1}')
        response = client.post(
            reverse('resources-api:request-list'),
            {
                'field_1': '{"question": {"id": 2, "slug": "resource_name"}, "value": "new_name"}',
            },
            **{
                'HTTP_X_FORM_ANSWER_ID': 1,
                'HTTP_X_ABC_USER': data.stranger.login,
                'HTTP_X_ABC_SERVICE': data.service1.pk,
                'HTTP_X_ABC_RESOURCE_TYPE': data.resource_type.pk,
                'HTTP_X_ABC_SIGNATURE': signature,
                'HTTP_X_ABC_SERVICE_RESOURCE': sr.pk,
            }
        )

    assert response.status_code == 200

    assert request.call_count == 1
    assert request.call_args[0][0] == 'post'
    assert 'tvm' in request.call_args[0][1]
    assert request.call_args[1]['data'] == {
        'client_id': sr.resource.external_id,
        'uid': data.stranger.uid,
        'name': 'new_name'
    }

    res.refresh_from_db()
    assert res.name == 'new_name'
    assert res.external_id == 'external_id'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_edit_by_stranger(request, client, data):
    request.return_value = Response(200, '{}')
    client.login(data.staff.login)

    res = factories.ResourceFactory(
        external_id='external_id',
        name='name',
        type=data.resource_type,
    )
    sr = factories.ServiceResourceFactory(
        service=data.service1,
        resource=res,
        state=ServiceResource.GRANTED,
    )

    signature = make_signature(
        service=data.service1,
        resource_type=data.resource_type,
        user=data.stranger.user
    )

    with patch('requests.request') as request:
        request.return_value = Response(200, '{"status": "error", "errors": ["abc_team.member_required"]}')
        response = client.post(
            reverse('resources-api:request-list'),
            {
                'field_2': '{"question": {"id": 2, "slug": "resource_name"}, "value": "new_name"}',
            },
            **{
                'HTTP_X_FORM_ANSWER_ID': 1,
                'HTTP_X_ABC_USER': data.stranger.login,
                'HTTP_X_ABC_SERVICE': data.service1.pk,
                'HTTP_X_ABC_RESOURCE_TYPE': data.resource_type.pk,
                'HTTP_X_ABC_SIGNATURE': signature,
                'HTTP_X_ABC_SERVICE_RESOURCE': sr.pk,
            }
        )

    assert response.status_code == 409
    assert json.loads(response.content)['error']['message']['ru'] == \
        'Ваш запрос не может быть осуществлен из-за его конфликта с состоянием сервиса'
    assert json.loads(response.content)['error']['extra']['ru'] == \
        'Ошибка взаимодействия с TVM: у вас недостаточно прав для этого действия'

    res.refresh_from_db()
    assert res.name == 'name'
    assert res.external_id == 'external_id'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@patch('requests.get')
def test_two_secrets(request, client, data):
    request.return_value = Response(200, '{}')
    client.login(data.staff.login)

    res = factories.ResourceFactory(
        external_id='external_id',
        name='name',
        type=data.resource_type,
    )
    sr = factories.ServiceResourceFactory(
        service=data.service1,
        resource=res,
        state='granted'
    )

    with patch('requests.request') as request:
        request.return_value = Response(200, '{"status": "error", "errors": ["old_secret.exists"]}')
        with patch('plan.resources.suppliers.tvm.get_tvm_ticket') as get_tvm_ticket:
            get_tvm_ticket.return_value = 'some_ticket'
            response = client.post(
                reverse('resources-api:serviceresources-secrets', args=[sr.pk]),
                {'action': 'recreate_secret'},
            )

    assert response.status_code == 409
    assert json.loads(response.content)['error']['message']['ru'] == \
        'У приложения уже есть два секрета: для создания нового нужно предварительно удалить старый'

    res.refresh_from_db()
    assert res.name == 'name'
    assert res.external_id == 'external_id'


@pytest.mark.parametrize('with_obsolete', [True, False])
@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_deprive_and_move(move_data, with_obsolete):
    data = move_data.data
    if with_obsolete:
        obsolete_resource = factories.ServiceResourceFactory(
            service=data.service1,
            resource=move_data.resource,
            state='obsolete',
        )
        move_data.service_resource.obolete_id = obsolete_resource
        move_data.service_resource.save()

    with patch('plan.resources.suppliers.tvm.get_tvm_ticket'):
        with patch.object(ResourceType, 'can_be_granted_automated'):
            with patch('requests.request') as request:
                request.return_value = Response(200, '{"status": "ok"}')
                response = _make_move_request(move_data)
    print(response.content)
    assert response.status_code == 200

    assert request.call_count == 1
    assert request.call_args[0][0] == 'post'
    assert 'tvm' in request.call_args[0][1]
    assert request.call_args[1]['data'] == {
        'abc_service_id': data.service2.pk,
        'client_id': '100500',
        'initiator_uid': get_abc_zombik().uid
    }
    service_resource = move_data.service_resource
    service_resource.refresh_from_db()
    assert service_resource.state == ServiceResource.OBSOLETE

    new_sr = ServiceResource.objects.get(
        service=data.service2,
        type=data.resource_type,
    )
    assert new_sr.state == ServiceResource.GRANTED
    assert new_sr.obsolete == service_resource
    assert new_sr.resource == move_data.resource

    resource = move_data.resource
    resource.refresh_from_db()
    assert resource.name == 'name'
    assert resource.external_id == '100500'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_move_no_resource(move_data):
    move_data.service_resource.delete()
    response = _make_move_request(move_data)
    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'Ресурса с таким tvmid не существует'


@pytest.mark.parametrize('state', [ServiceResource.GRANTING, ServiceResource.GRANTING])
@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_move_many_resources(move_data, state):
    factories.ServiceResourceFactory(
        service=move_data.data.service1,
        resource=move_data.resource,
        state=state,
    )
    response = _make_move_request(move_data)
    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'Уже есть ресурс с таким tvmid, находящийся в стадии утверждения'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_move_same_service(move_data):
    move_data.service_resource.delete()
    factories.ServiceResourceFactory(
        service=move_data.data.service2,
        resource=move_data.resource,
        state='granted'
    )
    response = _make_move_request(move_data)
    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'Ресурс с таким tvmid уже связан с данным сервисом'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_change_existing_resource(move_data):
    move_data.service_resource.delete()
    sr = factories.ServiceResourceFactory(
        service=move_data.data.service2,
        resource=move_data.resource,
        state='granted'
    )
    response = _make_move_request(move_data, HTTP_X_ABC_SERVICE_RESOURCE=sr.pk)
    assert response.status_code == 400
    assert response.json()['error']['extra']['ru'] == 'Нельзя изменить tvmid у существующего ресурса'
