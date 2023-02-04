import copy
import json
from tvm2 import TVM2
from textwrap import dedent
from urllib.parse import urlparse, parse_qsl

from mock import patch, Mock
import pretend
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse
from django.utils.datastructures import MultiValueDict

from plan.resources.api.base import make_signature
from plan.resources.models import ServiceResource

from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def patch_tvm(monkeypatch):
    monkeypatch.setattr(TVM2, '_init_context', lambda *args, **kwargs: None)
    monkeypatch.setattr('plan.resources.api.constructor.get_tvm_ticket', lambda *args, **kwargs: '1')


@pytest.fixture
def data(db, owner_role, staff_factory):
    staff = staff_factory()

    service = factories.ServiceFactory()
    other_service = factories.ServiceFactory()

    resource_type = factories.ResourceTypeFactory(
        import_plugin='generic',
        import_link='http://yandex.ru/?consumer=abc',
        form_id=100500,
        form_handler=dedent('''result = {
            'external_id': data['field_1']['value'],
            'name': data['field_1']['value'],
        }'''),
        form_back_handler=dedent('''
            result = {}
            # в случае создания нового ресурса resource_name не будет
            if attributes.get('resource_name'):
                result = {
                    'answer_text_1': attributes['resource_name'],
                }
        '''),
    )
    resource_type.supplier_roles.add(owner_role)
    resource_type.consumer_roles.add(owner_role)

    tag = factories.ResourceTagFactory()
    other_tag = factories.ResourceTagFactory()
    return pretend.stub(
        resource_type=resource_type,
        service=service,
        other_service=other_service,
        staff=staff,
        tag=tag,
        other_tag=other_tag
    )


@pytest.fixture(autouse=True)
def mocked_form_metadata():
    with patch('plan.resources.models.ResourceType.get_form_metadata') as mocked:
        yield mocked


@pytest.fixture()
def mocked_yp_form(mocked_form_metadata):
    from test_data.resources.yp.form_metadata import FORM_METADATA
    mocked_form_metadata.return_value = FORM_METADATA


def test_edit_link(client, data, owner_role):
    client.login(data.staff.login)

    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.other_service,
    )
    factories.ServiceMemberFactory(
        staff=data.staff,
        service=data.other_service,
        role=owner_role,
    )

    response = client.json.get(
        reverse('resources-api:resource-form-list'),
        {
            'service_resource': service_resource.pk
        }
    )

    assert response.status_code == 200

    uri = response.json()['form_url']
    url, qs = uri.split('?', 1)

    query = dict(parse_qsl(qs))

    assert query['service'] == str(service_resource.service.pk)
    assert query['service_slug'] == service_resource.service.slug
    assert query['service_name'] == service_resource.service.name
    assert query['resource_type'] == str(data.resource_type.pk)
    assert query['answer_text_1'] == resource.name


@pytest.mark.usefixtures('mocked_yp_form')
@pytest.mark.parametrize('resource_state, expected_scenario_id', [
    (ServiceResource.REQUESTED, 495399),  # Перераспределение квоты
    (ServiceResource.APPROVED, 495403),   # Другое
    (ServiceResource.GRANTED, 995403),   # Другое
])
def test_yp_edit_link(client, person, yp_quota_service_resource_data,
                      resource_state, expected_scenario_id):
    """resource-form-list returns url to populate Constructor form."""
    data = yp_quota_service_resource_data
    client.login(person.login)
    ServiceResource.objects.filter(pk=data.service_resource.pk).update(state=resource_state)

    expected_form_query = copy.deepcopy(data.expected_form_query)
    expected_form_query.update({
        'location': '68415',  # SAS
        'segment': '70109',   # default
    })
    if resource_state == ServiceResource.GRANTED:
        expected_form_query['is_scenario_restricted'] = 'True'
        expected_form_query['scenario_restricted'] = str(expected_scenario_id)
    else:
        expected_form_query['scenario'] = str(expected_scenario_id)

    response = client.json.get(
        reverse('resources-api:resource-form-list'),
        {'service_resource': data.service_resource.pk},
    )

    assert response.status_code == 200

    uri = response.json()['form_url']
    qs = urlparse(uri).query
    query = {k: v for k, v in parse_qsl(qs) if v != 'None' and k != 'signature'}

    assert query == expected_form_query


def test_edit_gdpr_link(client, data, owner_role, gdpr_resource_type, staff_factory):

    staff = staff_factory()
    target_service = factories.ServiceFactory()
    approver_role = factories.RoleFactory()
    member = factories.ServiceMemberFactory(
        service=target_service,
        role=approver_role,
        staff=staff,
    )
    client.login(member.staff.login)
    gdpr_resource_type.supplier_roles.add(approver_role)
    resource = factories.ResourceFactory(
        type=gdpr_resource_type,
        name='name',
        attributes={
            'data': 'Имя',
            'subject': 'пользователь',
            'service_from_slug': target_service.slug,
            'purpose': 'важная цель',
            'store_for': '5 лет',
            'store_in': 'YT',
            'is_storing': True,
            'is_using': True,
            'path_to_data': 'smth/smth',
            'access_from_web_cloud': True,
            'access_from_api': 'Нет',
        }
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.other_service,
    )

    with patch('plan.resources.processing.get_data_source_items') as get_data_source_items:
        with patch('plan.resources.models.ResourceType.get_form_metadata') as fake_form_metadata:
            fake_form_metadata.return_value = {
                'fields': {
                    'store_for': {
                        'data_source':
                              {'items': [
                                  {'text': '5 лет', 'id': 1},
                                  {'text': '12 лет', 'id': 4},
                              ]}
                    },
                    'subject': {
                        'data_source': {
                            'filters': [
                                {'value': 'wiki-data-source-some.url'},
                            ]}
                    },
                    'store_in': {
                        'data_source': {
                            'filters': [
                                {'value': 'wiki-data-source-stor.url'},
                            ]}
                    },
                    'data': {
                        'data_source': {
                            'filters': [
                                {'value': 'wiki-data-source.url'},
                            ]}
                    },
                }
            }
            get_data_source_items.return_value = {
                'Имя': 1,
                'YT': 350,
                'пользователь': 5,
            }
            response = client.json.get(
                reverse('resources-api:resource-form-list'),
                {
                    'service_resource': service_resource.pk
                }
            )
        assert response.status_code == 200

        uri = response.json()['form_url']
        url, qs = uri.split('?', 1)

        query = dict(parse_qsl(qs))
        assert query['service'] == str(service_resource.service.pk)
        assert query['service_slug'] == service_resource.service.slug
        assert query['service_name'] == service_resource.service.name
        assert query['resource_type'] == str(gdpr_resource_type.pk)
        assert query['service_from_slug'] == target_service.slug
        assert query['store_for'] == '1'
        assert query['data'] == '1'
        assert query['purpose'] == 'важная цель'
        assert query['subject'] == '5'
        assert query['store_in'] == '350'
        assert query['path_to_data'] == 'smth/smth'
        assert query['access_from_web_cloud'] == 'True'
        assert 'access_from_api' not in query


def test_link_without_params(client, data, owner_role):
    client.login(data.staff.login)

    factories.ServiceMemberFactory(
        staff=data.staff,
        service=data.other_service,
        role=owner_role,
    )

    response = client.json.get(
        reverse('resources-api:resource-form-list'),
        {
            'service': data.other_service.id,
            'resource_type': data.resource_type.pk
        }
    )

    assert response.status_code == 200

    uri = response.json()['form_url']
    url, qs = uri.split('?', 1)

    query = dict(parse_qsl(qs))

    assert query['service'] == str(data.other_service.pk)
    assert query['service_slug'] == data.other_service.slug
    assert query['service_name'] == data.other_service.name
    assert query['resource_type'] == str(data.resource_type.pk)


def test_cannot_edit(client, data):
    client.login(data.staff.login)

    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.other_service,
    )

    response = client.json.get(
        reverse('resources-api:resource-form-list'),
        {
            'service_resource': service_resource.pk
        }
    )

    assert response.status_code == 403


def test_cannot_edit_junk(client, data, owner_role):
    client.login(data.staff.login)

    data.other_service.is_exportable = False
    data.other_service.save()

    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.other_service,
    )
    factories.ServiceMemberFactory(
        staff=data.staff,
        service=data.other_service,
        role=owner_role,
    )

    response = client.json.get(
        reverse('resources-api:resource-form-list'),
        {
            'service_resource': service_resource.pk
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'service_not_exportable'


def test_edit_link_unicode(client, data, owner_role):
    client.login(data.staff.login)

    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='Имя ресурса',
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.other_service,
    )
    factories.ServiceMemberFactory(
        staff=data.staff,
        service=data.other_service,
        role=owner_role,
    )

    response = client.json.get(
        reverse('resources-api:resource-form-list'),
        {
            'service_resource': service_resource.pk
        }
    )

    assert response.status_code == 200

    uri = response.json()['form_url']
    url, qs = uri.split('?', 1)
    params = dict(parse_qsl(qs))

    assert params['answer_text_1'] == resource.name


def test_edit_link_multi_value(client, data, owner_role):
    """Проверяем как работает формирование урла при нескольких значениях для оного параметра"""
    client.login(data.staff.login)

    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.other_service,
    )
    factories.ServiceMemberFactory(
        staff=data.staff,
        service=data.other_service,
        role=owner_role,
    )

    data.resource_type.form_back_handler = '''result = MultiValueDict();
result.setlist('answer_text_1', ['foo', 'bar'])'''
    data.resource_type.save()

    response = client.json.get(
        reverse('resources-api:resource-form-list'),
        {
            'service_resource': service_resource.pk
        }
    )

    assert response.status_code == 200

    uri = response.json()['form_url']
    url, qs = uri.split('?', 1)
    params = MultiValueDict()
    for k, v in parse_qsl(qs):
        params.appendlist(k, v)
    assert params.getlist('answer_text_1') == ['foo', 'bar']


@pytest.mark.parametrize('state', [ServiceResource.OBSOLETE, ServiceResource.DEPRIVED])
def test_cannot_edit_dead_resource(client, data, state, owner_role):
    client.login(data.staff.login)

    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='xxx',
        name='name',
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.other_service,
        state=state,
    )

    factories.ServiceMemberFactory(
        staff=data.staff,
        service=data.other_service,
        role=owner_role,
    )

    response = client.json.get(
        reverse('resources-api:resource-form-list'),
        {
            'service_resource': service_resource.pk
        }
    )

    assert response.status_code == 403


def test_request_invalid_form_answers(client, data, owner_role, patch_tvm):
    client.login(data.staff.login)
    factories.ServiceMemberFactory(role=owner_role, service=data.service, staff=data.staff)

    forms_response = Mock()
    forms_response.status_code = 400
    forms_response.json = Mock(return_value={
        'fields': {
            'field_1': {
                'widget': 'Something',
                'errors': ['invalid literal for int() with base 10: hello'],
            },
            'field_2': {
                'widget': 'Something',
                'errors': ['this field is required'],
            },
        },
        'non_field_errors': [
            'something went really wrong'
        ]
    })

    with patch('requests.post', Mock(return_value=forms_response)):
        response = client.json.post(
            reverse('resources-api:resource-form-list'),
            {
                'service': data.service.id,
                'resource_type': data.resource_type.id,
                'data': {
                    'field_3': 'hello'
                },
            },
            HTTP_X_YA_USER_TICKET='test',
        )

    assert response.status_code == 400
    data = response.json()
    expected = {
        'errors': {
            'field_1': ['invalid literal for int() with base 10: hello'],
            'field_2': ['this field is required'],
            'non_field_errors': ['something went really wrong']},
        'created': False
    }
    assert data == expected


def test_request_resource_via_abc_api(client, data, owner_role, patch_tvm):
    client.login(data.staff.login)
    factories.ServiceMemberFactory(role=owner_role, service=data.service, staff=data.staff)

    forms_response = Mock()
    forms_response.status_code = 200
    forms_response.json = Mock(return_value={})

    with patch('requests.post', Mock(return_value=forms_response)) as post:
        response = client.json.post(
            reverse('resources-api:resource-form-list'),
            {
                'service': data.service.id,
                'resource_type': data.resource_type.id,
                'data': {
                    'resource_name': 'Autogranted resource'
                },
                'tags': [data.tag.id, data.other_tag.id],
            },
            HTTP_X_YA_USER_TICKET='test',
        )

    assert response.status_code == 200
    assert json.loads(post.call_args[1]['data']['source_request'])['query_params'] == {
        'service': data.service.id,
        'service_name': data.service.name,
        'service_slug': data.service.slug,
        'resource_type': data.resource_type.id,
        'tags': '{},{}'.format(data.tag.id, data.other_tag.id),
        'supplier_tags': '',
        'signature': make_signature(data.service, data.resource_type, data.staff.user)
    }
    assert (
        post.call_args[0][0] ==
        data.resource_type.form_api_link ==
        settings.CONSTRUCTOR_FORM_API_URL + 'v1/surveys/{}/form/'.format(data.resource_type.form_id)
    )
    assert post.call_args[1]['files'] == {
        'answer_text_1': ('', 'Autogranted resource')
    }
    headers = post.call_args[1]['headers']
    assert headers['X-Ya-Service-Ticket'] == '1'
    assert headers['X-Ya-User-Ticket'] == 'test'


def test_request_replacement_via_api(client, data, owner_role, patch_tvm):
    resource = factories.ResourceFactory(type=data.resource_type)
    service_resource = factories.ServiceResourceFactory(resource=resource)

    client.login(data.staff.login)
    factories.ServiceMemberFactory(role=owner_role, service=service_resource.service, staff=data.staff)

    forms_response = Mock()
    forms_response.status_code = 200
    forms_response.json = Mock(return_value={})

    with patch('requests.post', Mock(return_value=forms_response)) as post:
        response = client.json.post(
            reverse('resources-api:resource-form-list'),
            {
                'service_resource': service_resource.id,
                'data': {
                    'resource_name': 'Autogranted resource'
                },
                'tags': [data.tag.id, data.other_tag.id],
                'supplier_tags': []
            }
        )

    assert response.status_code == 200

    call_args = json.loads(post.call_args[1]['data']['source_request'])['query_params']

    assert call_args['service'] == service_resource.service.id
    assert call_args['service'] != data.service.id

    assert call_args['resource_type'] == service_resource.resource.type.id
    assert call_args['resource_type'] == data.resource_type.id

    assert post.called


def test_request_via_api_validation_args_required(client, data):
    client.login(data.staff.login)

    forms_response = Mock()
    forms_response.status_code = 200
    forms_response.json = Mock(return_value={})

    with patch('requests.post', Mock(return_value=forms_response)) as post:
        response = client.json.post(
            reverse('resources-api:resource-form-list'),
            {
                'data': {
                    'resource_name': 'Autogranted resource'
                },
                'tags': [data.tag.id, data.other_tag.id],
                'supplier_tags': []
            }
        )

    assert response.status_code == 400
    assert not post.called

    error = response.json()['error']
    assert error['code'] == 'invalid'


def test_request_via_api_validation_either_not_both(client, data):
    resource = factories.ResourceFactory(type=data.resource_type)
    service_resource = factories.ServiceResourceFactory(resource=resource)

    client.login(data.staff.login)

    forms_response = Mock()
    forms_response.status_code = 200
    forms_response.json = Mock(return_value={})

    with patch('requests.post', Mock(return_value=forms_response)) as post:
        response = client.json.post(
            reverse('resources-api:resource-form-list'),
            {
                'service_resource': service_resource.id,
                'service': data.service.id,
                'resource_type': data.resource_type.id,
                'data': {
                    'resource_name': 'Autogranted resource'
                },
                'tags': [data.tag.id, data.other_tag.id],
                'supplier_tags': []
            }
        )

    assert response.status_code == 400
    assert not post.called

    error = response.json()['error']
    assert error['code'] == 'invalid'


def test_yp_request(client, owner_role, mocked_yp_form, staff_factory, yp_quota_resource_type, patch_tvm):
    staff = staff_factory()
    service = factories.ServiceFactory()
    factories.ServiceMemberFactory(role=owner_role, service=service, staff=staff)

    client.login(staff.login)
    forms_response = Mock()
    forms_response.status_code = 200
    forms_response.json = Mock(return_value={'answer_id': 123})

    with patch('requests.post', Mock(return_value=forms_response)):
        response = client.json.post(
            reverse('resources-api:resource-form-list'),
            {
                'service': service.id,
                'resource_type': yp_quota_resource_type.id,
                'data': {
                    'location': 'SAS',
                },
            },
            HTTP_X_YA_USER_TICKET='test',
        )

    assert response.status_code == 200
    assert response.json() == {'answer_id': 123}
