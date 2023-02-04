import json
from textwrap import dedent
from urllib.parse import quote

from mock import patch
import pretend
import pytest

from django.db.utils import IntegrityError
from django.conf import settings
from django.core.urlresolvers import reverse

from plan.resources.api.base import make_signature
from plan.resources.models import ServiceResource, Resource
from plan.resources.policies import APPROVE_POLICY
from common import factories
from utils import Response

pytestmark = [pytest.mark.django_db, pytest.mark.usefixtures('robot')]


@pytest.fixture
def data(db, owner_role, staff_factory):
    staff = staff_factory()
    service = factories.ServiceFactory(owner=staff)
    factories.ServiceMemberFactory(service=service, role=owner_role, staff=staff)

    supplier = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(
        supplier=supplier,
        import_plugin='generic',
        import_link='http://yandex.ru/?consumer=abc',
        form_id=1,
        form_handler=dedent('''result = {
            'external_id': data['field_1']['value'],
            'name': data['field_2']['value'],
        }'''),
        form_back_handler=dedent('''result = {
            'answer_short_text_18809': service_resource.resource.name,
        }'''),
    )

    tag1 = factories.ResourceTagFactory(service=service)
    tag2 = factories.ResourceTagFactory(service=service)
    tag3 = factories.ResourceTagFactory(service=service)

    supplier_tag = factories.ResourceTagFactory(service=supplier)

    global_tag = factories.ResourceTagFactory(service=None)

    signature = make_signature(
        service=service,
        resource_type=resource_type,
        user=staff.user
    )

    return pretend.stub(
        resource_type=resource_type,
        service=service,
        supplier=supplier,
        staff=staff,
        tag1=tag1,
        tag2=tag2,
        tag3=tag3,
        supplier_tag=supplier_tag,
        global_tag=global_tag,
        signature=signature,
    )


@pytest.fixture
def yp_quota_data(db, data):
    data.resource_type.approve_policy = APPROVE_POLICY.SUPPLIER_OR_OWNER_ROLE
    data.resource_type.code = settings.YP_RESOURCE_TYPE_CODE
    data.resource_type.save()
    return data


def fake_form_metadata(self, *args, **kwargs):
    return {}


def request(client, data, form=None, answer_id=1, user=None, service=None, resource_type=None, tags=None,
            supplier_tags=None, signature=None, service_resource=None):
    with patch('requests.get', return_value=Response(200, '{}')):
        response = client.post(
            reverse('resources-api:request-list'),
            form or {'field_1': '{"question": {"id": 1}, "value": "external_id"}', 'field_2': '{"question": {"id": 2}, "value": "resource_name"}'},
            **{
                'HTTP_X_FORM_ANSWER_ID': answer_id,
                'HTTP_X_ABC_USER': user or data.staff.login,
                'HTTP_X_ABC_SERVICE': service or data.service.pk,
                'HTTP_X_ABC_RESOURCE_TYPE': resource_type or data.resource_type.pk,
                'HTTP_X_ABC_RESOURCE_TAGS': quote(','.join(str(tag) for tag in (tags or []))),
                'HTTP_X_ABC_RESOURCE_SUPPLIER_TAGS': quote(','.join(str(tag) for tag in (supplier_tags or []))),
                'HTTP_X_ABC_SIGNATURE': signature or data.signature,
                'HTTP_X_ABC_SERVICE_RESOURCE': service_resource
            }
        )
        return response


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_parse_resource(client, data):
    client.login(data.staff.user.username)
    tags = (data.tag1.id, data.tag2.id)
    supplier_tags = (data.supplier_tag.id,)

    response = request(client, data, tags=tags, supplier_tags=supplier_tags, answer_id=123)
    assert response.status_code == 200

    sr = ServiceResource.objects.get()
    assert sr.state == ServiceResource.APPROVED
    assert sr.resource.external_id == 'external_id'
    assert sr.resource.name == 'resource_name'
    assert sr.tags.count() == 2
    assert sr.supplier_tags.count() == 1
    assert tuple(sr.tags.values_list('id', flat=True)) == tags
    assert sr.resource.answer_id == 123


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_requesting_with_tags(client, data):
    client.login(data.staff.user.username)
    tags = (data.tag1.id, data.tag2.id)

    response = request(client, data, tags=tags)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()
    assert set(sr.tags.all()) == set((data.tag1, data.tag2))


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_requesting_with_supplier_tag(client, data):
    client.login(data.staff.user.username)
    tags = (data.supplier_tag.id,)

    response = request(client, data, supplier_tags=tags)
    assert response.status_code == 200

    sr = ServiceResource.objects.get()
    assert sr.supplier_tags.get() == data.supplier_tag


def test_requesting_with_foreign_tag(client, data):
    client.login(data.staff.user.username)
    tags = (data.supplier_tag.id,)

    response = request(client, data, tags=tags)
    assert response.status_code == 400


def test_requesting_with_foreign_supplier_tag(client, data):
    client.login(data.staff.user.username)
    tags = (data.tag1.id,)

    response = request(client, data, supplier_tags=tags)
    assert response.status_code == 400


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_requesting_with_global_tag(client, data):
    client.login(data.staff.user.username)
    tags = (data.global_tag.id,)

    response = request(client, data, supplier_tags=tags)
    assert response.status_code == 200

    sr = ServiceResource.objects.get()
    assert sr.supplier_tags.get() == data.global_tag


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_requesting_with_invalid_supplier_tags(client, data):
    client.login(data.staff.user.username)

    data.resource_type.tags.add(data.global_tag)
    tags = (data.global_tag.id,)
    response = request(client, data, supplier_tags=tags)
    assert response.status_code == 200

    other_global_tag = factories.ResourceTagFactory(service=None)
    tags = (other_global_tag.id,)
    response = request(client, data, supplier_tags=tags)
    assert response.status_code == 400


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_request_same_resource(client, data):
    data.resource_type.has_multiple_consumers = True
    data.resource_type.save()

    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id='external_id',
    )

    client.login(data.staff.user.username)

    response = request(client, data, answer_id=321)
    assert response.status_code == 200

    assert Resource.objects.all().count() == 1

    sr = ServiceResource.objects.last()
    assert sr.state == ServiceResource.APPROVED
    assert sr.service == data.service
    assert sr.resource == resource
    assert sr.resource.answer_id is None


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_request_same_resource_externalid_none(client, data):
    data.resource_type.has_multiple_consumers = True
    data.resource_type.form_handler = "result = {'external_id': None, 'name': ''}"
    data.resource_type.save()

    resource = factories.ResourceFactory(
        type=data.resource_type,
        external_id=None,
    )

    client.login(data.staff.user.username)

    response = request(client, data, form={})
    assert response.status_code == 200

    assert Resource.objects.all().count() == 2

    sr = ServiceResource.objects.last()
    assert sr.state == ServiceResource.APPROVED
    assert sr.service == data.service
    assert sr.resource != resource


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_request_same_resource_wo_multiple_resources(client, data):
    factories.ResourceFactory(
        type=data.resource_type,
        external_id='external_id',
    )

    client.login(data.staff.user.username)

    response = request(client, data)
    assert response.status_code == 409

    result = json.loads(response.content)
    assert result['error']['code'] == 'conflict'
    assert (
        result['error']['message']['ru'] ==
        'Многократное использование ресурсов запрещено'
    )


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_request_same_request_id(client, data):
    data.resource_type.form_handler = dedent('''result = {
        'name': data['field_1']['value'],
    }''')
    data.resource_type.save()
    client.login(data.staff.user.username)
    form = {'field_1': '{"question": {"id": 1}, "value": "name"}'}

    response = request(client, data, form=form)
    assert response.status_code == 200
    response = request(client, data, form=form)
    assert response.status_code == 409
    assert ServiceResource.objects.count() == 1


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_request_another_answer_id(client, data):
    data.resource_type.form_handler = dedent('''result = {
        'name': data['field_1']['value'],
    }''')
    data.resource_type.save()
    client.login(data.staff.user.username)
    form = {'field_1': '{"question": {"id": 1}, "value": "name"}'}

    response = request(client, data, form=form)
    assert response.status_code == 200
    response = request(client, data, form=form, answer_id=2)
    assert response.status_code == 200
    assert ServiceResource.objects.count() == 2


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_request_invalid_form_answers(client, data):
    data.resource_type.form_handler = 'assert False'
    data.resource_type.save()
    response = request(client, data)
    assert response.status_code == 400
    result = response.json()
    assert result['error']['message'] == 'Error during form processing'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_parse_from_one_field(client, data):
    client.login(data.staff.user.username)

    resource_type = factories.ResourceTypeFactory(
        import_plugin='generic',
        import_link='http://yandex.ru/?consumer=abc',
        form_id=1,
        form_handler=dedent('''result = {
            'external_id': data['field_1']['value'],
            'name': data['field_1']['value'],
        }'''),
    )

    signature = make_signature(
        service=data.service,
        resource_type=resource_type,
        user=data.staff.user
    )

    form = {'field_1': '{"question": {"id": 1}, "value": "qqq"}'}
    response = request(client, data, form=form, resource_type=resource_type.pk, signature=signature)
    assert response.status_code == 200

    sr = ServiceResource.objects.get()
    assert sr.state == ServiceResource.APPROVED
    assert sr.resource.external_id == 'qqq'
    assert sr.resource.name == 'qqq'


def test_form_metadata(client, data):
    client.login(data.staff.user.username)

    resource_type = factories.ResourceTypeFactory(
        import_plugin='generic',
        import_link='http://yandex.ru/?consumer=abc',
        form_id=1,
        form_handler=dedent('''result = {
            'external_id': form_metadata['ext'],
        }'''),
    )

    signature = make_signature(
        service=data.service,
        resource_type=resource_type,
        user=data.staff.user
    )

    with patch('plan.common.utils.http.Session.get') as request:
        request.return_value = Response(200, '{"ext": "xxx"}')

        response = client.post(
            reverse('resources-api:request-list'),
            {},
            **{
                'HTTP_X_FORM_ANSWER_ID': 1,
                'HTTP_X_ABC_USER': data.staff.login,
                'HTTP_X_ABC_SERVICE': data.service.pk,
                'HTTP_X_ABC_RESOURCE_TYPE': resource_type.pk,
                'HTTP_X_ABC_SIGNATURE': signature,
            }
        )

        request.assert_called_once_with(resource_type.form_api_link)

    assert response.status_code == 200

    sr = ServiceResource.objects.get()
    assert sr.resource.external_id == 'xxx'


@pytest.fixture
def resource_edit_data(data, owner_role):
    resource = factories.ResourceFactory(type=data.resource_type, external_id='xxx')
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=data.service,
        state=ServiceResource.GRANTED,
    )
    owner = factories.ServiceMemberFactory(
        staff=data.staff,
        service=data.service,
        role=owner_role,
    )
    return pretend.stub(
        owner_role=owner_role,
        resource=resource,
        service_resource=service_resource,
        owner=owner,
    )


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_edit_resource(client, data, resource_edit_data):
    client.login(data.staff.login)
    form = {
        'field_1': '{"question": {"id": 1}, "value": "xxx"}',
        'field_2': '{"question": {"id": 2}, "value": "name2"}',
    }
    response = request(client, data, form=form, service_resource=resource_edit_data.service_resource.pk)
    assert response.status_code == 200
    assert Resource.objects.all().count() == 1

    resource_edit_data.resource.refresh_from_db()
    assert resource_edit_data.resource.name == 'name2'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_edit_resource_unicode_name(client, data, resource_edit_data):
    client.login(data.staff.login)

    form = {
        'field_1': '{"question": {"id": 1}, "value": "xxx"}',
        'field_2': '{"question": {"id": 2}, "value": "новое имя"}',
    }
    response = request(client, data, form=form, service_resource=resource_edit_data.service_resource.pk)
    assert response.status_code == 200
    assert Resource.objects.all().count() == 1

    resource_edit_data.resource.refresh_from_db()
    assert resource_edit_data.resource.name == 'новое имя'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_edit_resource_complex(client, data, resource_edit_data):
    service_resource = resource_edit_data.service_resource
    client.login(data.staff.login)

    data.resource_type.is_immutable = True
    data.resource_type.save()

    resource_edit_data.resource.name = 'foo'
    resource_edit_data.resource.save()

    form = {
        'field_1': '{"question": {"id": 1}, "value": "xxx2"}',
        'field_2': '{"question": {"id": 2}, "value": "новое имя"}',
    }

    # Создадим такой же отозванный ServiceResource, чтобы проверить, что он не копируется
    ServiceResource.objects.create(
        service=service_resource.service,
        resource=service_resource.resource,
        state='deprived',
        type_id=service_resource.type_id,
    )
    response = request(client, data, form=form, service_resource=service_resource.pk, answer_id=123)
    assert response.status_code == 200
    assert Resource.objects.all().count() == 2

    resource_edit_data.resource.refresh_from_db()
    assert resource_edit_data.resource.name == 'foo'

    resource_edit_data.service_resource.refresh_from_db()
    assert resource_edit_data.service_resource.state == ServiceResource.GRANTED

    new_sr = ServiceResource.objects.get(state=ServiceResource.APPROVED)
    assert new_sr.obsolete == resource_edit_data.service_resource
    assert new_sr.resource.obsolete == resource_edit_data.resource
    assert new_sr.resource.external_id == 'xxx2'
    assert new_sr.resource.name == 'новое имя'
    assert new_sr.resource.answer_id == 123


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_edit_uneditable_type(client, data, resource_edit_data):
    client.login(data.staff.login)

    data.resource_type.form_back_handler = None
    data.resource_type.save()

    form = {
        'field_1': '{"question": {"id": 1}, "value": "external_id"}',
        'field_2': '{"question": {"id": 2}, "value": "resource_name"}',
    }
    response = request(client, data, form=form, service_resource=resource_edit_data.service_resource.pk)
    assert response.status_code == 400
    response_json = json.loads(response.content)
    assert response_json['error']['code'] == 'resource_edit_disabled'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_edit_same_request_id(client, data, resource_edit_data):
    client.login(data.staff.login)
    data.resource_type.is_immutable = True
    data.resource_type.form_handler = dedent('''result = {
        'name': data['field_1']['value'],
    }''')
    data.resource_type.save()

    form = {'field_1': '{"question": {"id": 1}, "value": "name"}'}
    response = request(client, data, form=form, service_resource=resource_edit_data.service_resource.pk)
    assert response.status_code == 200
    response = request(client, data, form=form, service_resource=resource_edit_data.service_resource.pk)
    assert response.status_code == 200
    assert ServiceResource.objects.filter(obsolete=resource_edit_data.service_resource).count() == 1


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_supplier_approve(client, data, staff_factory):
    data.resource_type.approve_policy = APPROVE_POLICY.SUPPLIER
    data.resource_type.save()

    # Запросили ресурс, который должен подтверждать поставщик, но подтверждающих нет
    response = request(client, data)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()
    assert sr.state == ServiceResource.REQUESTED
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers) == []
    assert consumer_approvers is None

    # Добавим в сервис поставщика человека, который может подтвердить
    staff = staff_factory()
    another_staff = staff_factory()
    role = factories.RoleFactory()
    another_role = factories.RoleFactory()
    data.resource_type.supplier_roles.add(role)
    factories.ServiceMemberFactory(staff=staff, service=data.resource_type.supplier, role=role)
    factories.ServiceMemberFactory(staff=another_staff, service=data.resource_type.supplier, role=another_role)
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers.values_list('id', flat=True)) == [staff.id]
    assert consumer_approvers is None

    # залогинимся за того, который не может, и проверим, что вернется 403
    client.login(another_staff.user.username)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 403

    # теперь залогинимся правильным пользователем и подтвердим ресурс
    client.login(staff.user.username)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 200
    sr.refresh_from_db()
    assert sr.state == ServiceResource.APPROVED


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_supplier_and_consumer_approve(client, data, staff_factory):
    s = factories.StaffFactory()
    signature = make_signature(
        service=data.service,
        resource_type=data.resource_type,
        user=s.user
    )
    data.resource_type.approve_policy = APPROVE_POLICY.SUPPLIER_AND_CONSUMER
    data.resource_type.save()

    response = request(client, data, user=s.login, signature=signature)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()

    # Добавим в сервисы поставщика и потребителя по человеку, которые могут подтвердить
    staff = staff_factory()
    another_staff = staff_factory()
    role = factories.RoleFactory()
    another_role = factories.RoleFactory()
    data.resource_type.supplier_roles.add(role)
    data.resource_type.consumer_roles.add(another_role)
    factories.ServiceMemberFactory(staff=staff, service=data.resource_type.supplier, role=role)
    factories.ServiceMemberFactory(staff=another_staff, service=data.resource_type.supplier, role=another_role)
    factories.ServiceMemberFactory(staff=staff, service=data.service, role=role)
    factories.ServiceMemberFactory(staff=another_staff, service=data.service, role=another_role)

    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers.values_list('id', flat=True)) == [staff.id]
    assert list(consumer_approvers.values_list('id', flat=True)) == [data.staff.id]  # руководитель сервиса

    # залогинимся за того, который не может, и проверим, что вернется 403
    client.login(another_staff.user.username)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 403

    # теперь залогинимся правильным пользователем и подтвердим ресурс
    client.login(staff.user.username)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 200
    sr.refresh_from_db()
    assert sr.state == ServiceResource.REQUESTED  # ресурс все еще в статусе запрошен, потому что 2 подтверждающих

    client.login(data.staff.user.username)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 200
    sr.refresh_from_db()
    assert sr.state == ServiceResource.APPROVED


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@pytest.mark.parametrize('is_robot', (True, False))
def test_owner_approve(client, data, staff_factory, is_robot):
    data.resource_type.approve_policy = APPROVE_POLICY.OWNER
    if is_robot:
        data.resource_type.code = settings.ROBOT_RESOURCE_TYPE_CODE
    data.resource_type.save()

    response = request(client, data)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()

    # Добавим в сервис поставщика человека, который может подтвердить
    staff = factories.StaffFactory()
    another_staff = factories.StaffFactory()
    supplier_role = factories.RoleFactory()
    owner_role = factories.RoleFactory()
    another_role = factories.RoleFactory()
    data.resource_type.supplier_roles.add(supplier_role)
    data.resource_type.owner_roles.add(owner_role)
    factories.ServiceMemberFactory(staff=staff, service=data.resource_type.supplier, role=supplier_role)
    factories.ServiceMemberFactory(staff=another_staff, service=data.resource_type.supplier, role=another_role)
    supplier_approvers, consumer_approvers = sr.get_approvers()
    # у ресурса нет других владельцев, поэтому подтверждают владельцы нужной роли в сервисе-поставщике
    assert list(supplier_approvers.values_list('id', flat=True)) == [staff.pk]
    assert consumer_approvers is None

    # Добавим еще одного владельца
    another_service_staff = staff_factory()
    another_service = factories.ServiceFactory()
    factories.ServiceMemberFactory(staff=another_service_staff, role=owner_role, service=another_service)
    another_resource = factories.ServiceResourceFactory(
        resource=sr.resource,
        service=another_service,
        state=ServiceResource.GRANTING,
    )

    supplier_approvers, consumer_approvers = sr.get_approvers()
    # теперь может подтвердить еще и владелец, но только если ресурс типа робот
    if is_robot:
        expected = {another_service_staff.pk, staff.pk}
    else:
        expected = {staff.pk}
    assert set(supplier_approvers.values_list('id', flat=True)) == expected
    assert consumer_approvers is None

    another_resource.state = ServiceResource.GRANTED
    another_resource.save()

    supplier_approvers, consumer_approvers = sr.get_approvers()
    # теперь может подтвердить как владелец, так и поставщик
    expected = {another_service_staff.pk, staff.pk}
    assert set(supplier_approvers.values_list('id', flat=True)) == expected
    assert consumer_approvers is None

    # теперь залогинимся за владельца ресурса
    client.login(another_service_staff.user.username)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 200
    sr.refresh_from_db()
    assert sr.state == ServiceResource.APPROVED


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@pytest.mark.parametrize('consumer_exists', [True, False])
@pytest.mark.parametrize('supplier_exists', [True, False])
def test_consumer_or_maybe_supplier_approve(client, data, consumer_exists, supplier_exists, staff_factory):
    s = factories.StaffFactory()
    signature = make_signature(
        service=data.service,
        resource_type=data.resource_type,
        user=s.user
    )
    data.resource_type.approve_policy = APPROVE_POLICY.CONSUMER_OR_MAYBE_SUPPLIER
    data.resource_type.save()

    staff = staff_factory()
    role = factories.RoleFactory()
    data.resource_type.supplier_roles.add(role)
    if supplier_exists:
        factories.ServiceMemberFactory(staff=staff, service=data.resource_type.supplier, role=role)
        factories.ServiceMemberFactory(staff=staff, service=data.service, role=role)

    another_staff = staff_factory()
    another_role = factories.RoleFactory()
    data.resource_type.consumer_roles.add(another_role)
    if consumer_exists:
        factories.ServiceMemberFactory(staff=another_staff, service=data.resource_type.supplier, role=another_role)
        factories.ServiceMemberFactory(staff=another_staff, service=data.service, role=another_role)

    response = request(client, data, user=s.login, signature=signature)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()

    supplier_approvers, consumer_approvers = sr.get_approvers()
    if consumer_exists:
        assert supplier_approvers is None
        assert list(consumer_approvers.values_list('id', flat=True)) == [another_staff.id]
        correct_user, wrong_user = another_staff.user.username, staff.user.username
    elif supplier_exists:
        assert list(supplier_approvers.values_list('id', flat=True)) == [staff.id]
        assert consumer_approvers is None
        correct_user, wrong_user = staff.user.username, another_staff.user.username
    else:
        assert supplier_approvers is None
        assert list(consumer_approvers.values_list('id', flat=True)) == []
        factories.ServiceMemberFactory(staff=another_staff, service=data.resource_type.supplier, role=another_role)
        factories.ServiceMemberFactory(staff=another_staff, service=data.service, role=another_role)
        correct_user, wrong_user = another_staff.user.username, staff.user.username

    # залогинимся за того, который не может, и проверим, что вернется 403
    client.login(wrong_user)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 403

    # теперь залогинимся правильным пользователем и подтвердим ресурс
    client.login(correct_user)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 200
    sr.refresh_from_db()
    assert sr.state == ServiceResource.APPROVED


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_autoapprove(client, data):
    role = factories.RoleFactory()
    data.resource_type.approve_policy = APPROVE_POLICY.OWNER
    data.resource_type.has_automated_grant = True
    data.resource_type.supplier_roles.set([role])
    data.resource_type.save()
    factories.ServiceMemberFactory(staff=data.staff, service=data.supplier, role=role)

    response = request(client, data)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()
    assert sr.state == 'granted'


@pytest.mark.parametrize('model', ('plan.resources.api.request.Resource', 'plan.resources.api.request.ServiceResource'))
@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_return_conflict_if_resource_exist(client, data, model):
    # Проверим, что овечаем 409, если при создании ресурса или его привязке к сервису,
    # нарушаются ограничения на уникальность значений в базе
    role = factories.RoleFactory()
    data.resource_type.approve_policy = APPROVE_POLICY.OWNER
    data.resource_type.has_automated_grant = True
    data.resource_type.supplier_roles.set([role])
    data.resource_type.save()
    factories.ServiceMemberFactory(staff=data.staff, service=data.supplier, role=role)

    def bad_create(*args, **kwargs):
        raise IntegrityError()

    with patch('%s.objects.create' % model) as save:
        save.side_effect = bad_create
        response = request(client, data)
    assert response.status_code == 409


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_create_yp_service_resource(client, data, yp_quota_resource_type):
    signature = make_signature(
        service=data.service,
        resource_type=yp_quota_resource_type,
        user=data.staff.user
    )

    form = {
        'field1': '{"question": {"slug": "cpu-float"}, "value": "1"}',
        'field2': '{"question": {"slug": "gpu_model"}, "value": ""}',
        'field3': '{"question": {"slug": "gpu_qty"}, "value": ""}',
    }

    response = request(client, data, form=form, resource_type=yp_quota_resource_type.pk, signature=signature)
    assert response.status_code == 200

    resource = ServiceResource.objects.get(service=data.service).resource
    assert resource.attributes == {'cpu': '1'}


def test_create_gdpr_service_resource(client, data, gdpr_resource_type):
    signature = make_signature(
        service=data.service,
        resource_type=gdpr_resource_type,
        user=data.staff.user
    )
    target_service = factories.ServiceFactory(name='GDPR name')

    form = {
        'field1': '{"question": {"slug": "is_using"}, "value": "True"}',
        'field2': '{"question": {"slug": "service_from_slug"}, "value": "GDPR name"}',
        'field3': '{"question": {"slug": "subject"}, "value": "пользователь"}',
        'field4': '{"question": {"slug": "purpose"}, "value": "важная цель"}',
        'field5': '{"question": {"slug": "data"}, "value": "Имя"}',
        'field6': '{"question": {"slug": "store_for"}, "value": "5 лет"}',
        'field7': '{"question": {"slug": "path_to_data"}, "value": "smth/smth"}',
    }
    with patch('plan.resources.models.ResourceType.get_form_metadata') as fake_form_metadata:
        fake_form_metadata.return_value = {
            'fields': {
                'store_for': {
                    'data_source': {
                        'items': [
                            {'text': '5 лет', 'id': 1},
                            {'text': '12 лет', 'id': 4},
                        ],
                    },
                },
            }
        }
        response = request(
            client, data, form=form,
            resource_type=gdpr_resource_type.pk,
            signature=signature,
        )
    assert response.status_code == 200

    resource = ServiceResource.objects.get(service=data.service).resource
    assert resource.name == f'Пользовательские данные "Имя" от {target_service.slug}'
    assert resource.attributes == {
        'is_using': 'True',
        'store_for': '5 лет',
        'data': 'Имя',
        'purpose': 'важная цель',
        'service_from_slug': target_service.slug,
        'subject': 'пользователь',
        'path_to_data': 'smth/smth',
    }


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_yp_dont_create_empty_quota(client, data):
    yp_quota = factories.ResourceTypeFactory(
        form_id=100500,
        form_handler=dedent('''
            from plan.resources.handlers.yp.forward import process_form_forward
            result = process_form_forward(data, form_metadata)
            '''),
        form_back_handler=dedent('''
            from plan.resources.handlers.yp.backward import process_form_backward
            result = process_form_backward(attributes, form_metadata)
            '''),
    )

    signature = make_signature(
        service=data.service,
        resource_type=yp_quota,
        user=data.staff.user
    )

    form = {
        'field1': '{"question": {"slug": "cpu-float"}, "value": "0"}'
    }

    response = request(client, data, form=form, resource_type=yp_quota.pk, signature=signature)
    assert response.status_code == 400

    result = response.json()
    assert result['error']['code'] == 'bad_request'
    assert result['error']['extra']['ru'] == 'Запрещен запрос пустых квот'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@pytest.mark.parametrize('robot_exists', [True, False])
def test_request_robot(client, data, owner_role, robot_exists):
    robot_resource_type = factories.ResourceTypeFactory(
        code='staff-robot',
        supplier_plugin='robots',
        has_multiple_consumers=True,
        has_automated_grant=True,
        form_id=100500,
        form_handler=dedent('''
                from plan.resources.handlers.staff.robot.forward import process_form_forward
                result = process_form_forward(data, form_metadata)
                '''),
        form_back_handler=dedent('''
                from plan.resources.handlers.staff.robot.backward import process_form_backward
                result = process_form_backward(attributes, form_metadata)
                '''),
    )
    robot_resource_type.consumer_roles.add(owner_role)

    if robot_exists:
        resource = factories.ResourceFactory(
            type=robot_resource_type,
            external_id='robot-login',
            attributes={'secret_id': {'value': 'value'}},
        )
        factories.StaffFactory(login='robot-login', is_robot=True)

    signature = make_signature(
        service=data.service,
        resource_type=robot_resource_type,
        user=data.staff.user
    )

    form = {
        'field_1': '{"question": {"id": 1}, "value": "robot-login"}'
    }

    response = request(client, data, form=form, resource_type=robot_resource_type.pk, signature=signature)

    if robot_exists:
        assert response.status_code == 200
        assert ServiceResource.objects.filter(service=data.service, resource=resource).exists()
        resource.refresh_from_db()
        assert resource.attributes['secret_id']['value'] == 'value'
    else:
        assert response.status_code == 400


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_supplier_or_owner_role_approve_from_service(client, yp_quota_data):
    # Запросили ресурс, который должен подтверждать поставщик или пользователь с owner_role, но подтверждающих нет

    response = request(client, yp_quota_data)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()
    sr.resource.attributes['scenario'] = 'Перераспределение квоты'
    sr.resource.save()
    assert sr.state == ServiceResource.REQUESTED
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers) == []
    assert consumer_approvers is None

    # Добавим в сервис поставщика человека, который может подтвердить
    staff = factories.StaffFactory()
    another_staff = factories.StaffFactory()
    role = factories.RoleFactory()
    another_role = factories.RoleFactory()
    yp_quota_data.resource_type.supplier_roles.add(role)
    factories.ServiceMemberFactory(staff=staff, service=yp_quota_data.resource_type.supplier, role=role)
    factories.ServiceMemberFactory(staff=another_staff, service=yp_quota_data.resource_type.supplier, role=another_role)
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers.values_list('id', flat=True)) == [staff.id]
    assert consumer_approvers is None


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_supplier_or_owner_role_approve_global(client, yp_quota_data):
    response = request(client, yp_quota_data)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()
    sr.resource.attributes['scenario'] = 'Перераспределение квоты'
    sr.resource.save()

    # Добавим в сервис пользователя с ролью из owner_roles, но без привязки этой роли к сервису
    staff_service = factories.StaffFactory()
    role_global = factories.RoleFactory()
    yp_quota_data.resource_type.owner_roles.add(role_global)
    factories.ServiceMemberFactory(staff=staff_service, service=yp_quota_data.service, role=role_global)
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers.values_list('id', flat=True)) == [staff_service.id]


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@pytest.mark.parametrize(
    ('scenario', 'should_approve'),
    [
        ('smth', False),
        ('Перераспределение квоты', True)
    ]
)
def test_supplier_or_owner_role_approve_custom(client, yp_quota_data, scenario, should_approve):
    response = request(client, yp_quota_data)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()
    assert sr.state == ServiceResource.REQUESTED
    sr.resource.attributes['scenario'] = scenario
    sr.resource.save()

    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers) == []
    assert consumer_approvers is None

    # Добавим в сервис пользователя с ролью из owner_roles, с привязкой этой роли к сервису
    staff_service = factories.StaffFactory()
    role_service = factories.RoleFactory(service=yp_quota_data.service)
    yp_quota_data.resource_type.owner_roles.add(role_service)
    factories.ServiceMemberFactory(staff=staff_service, service=yp_quota_data.service, role=role_service)
    supplier_approvers, consumer_approvers = sr.get_approvers()
    if should_approve:
        assert list(supplier_approvers.values_list('id', flat=True)) == [staff_service.id]
    else:
        assert consumer_approvers is None


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_supplier_or_owner_role_approve_custom_ancestor(client, yp_quota_data, staff_factory):
    response = request(client, yp_quota_data)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()
    sr.resource.attributes['scenario'] = 'Перераспределение квоты'
    sr.resource.save()
    assert sr.state == ServiceResource.REQUESTED

    # Добавим сервису родительский, проверим, что при наличии в ней пользователя с owner_roles он
    # так же может подтверждать
    service_ancestor = factories.ServiceFactory(owner=yp_quota_data.staff)
    sr.service.parent = service_ancestor
    sr.service.save()
    staff_service_ancestor = staff_factory()
    role_staff_service_ancestor = factories.RoleFactory(service=service_ancestor)
    yp_quota_data.resource_type.owner_roles.add(role_staff_service_ancestor)
    factories.ServiceMemberFactory(
        staff=staff_service_ancestor,
        service=service_ancestor,
        role=role_staff_service_ancestor,
    )
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers.values_list('id', flat=True)) == [
        staff_service_ancestor.id,
    ]
    # теперь залогинимся правильным пользователем и подтвердим ресурс
    client.login(staff_service_ancestor.user.username)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 200
    sr.refresh_from_db()
    assert sr.state == ServiceResource.APPROVED


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_supplier_or_owner_role_approve_custom_not_ancestor(client, yp_quota_data):
    response = request(client, yp_quota_data)
    assert response.status_code == 200
    sr = ServiceResource.objects.get()
    sr.resource.attributes['scenario'] = 'Перераспределение квоты'
    sr.resource.save()

    # Однако пользователь с ролью owner_roles не может подтверждать, если сервис не находится в дереве
    service_not_ancestor = factories.ServiceFactory(owner=yp_quota_data.staff)
    staff_service_not_ancestor = factories.StaffFactory()
    role_staff_service_not_ancestor = factories.RoleFactory(service=service_not_ancestor)
    yp_quota_data.resource_type.owner_roles.add(role_staff_service_not_ancestor)
    factories.ServiceMemberFactory(
        staff=staff_service_not_ancestor,
        service=service_not_ancestor,
        role=role_staff_service_not_ancestor,
    )
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert supplier_approvers.count() == 0

    # залогинимся за того, который не может, и проверим, что вернется 403
    client.login(staff_service_not_ancestor.user.username)
    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', args=[sr.id]),
        {
            'state': 'approved',
        }
    )
    assert response.status_code == 403


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_request_balance_attrs_client_id(client, data, owner_role):
    balance_resource_type = factories.ResourceTypeFactory(
        code=settings.BALANCE_RESOURCE_TYPE_CODE,
        has_multiple_consumers=True,
        has_automated_grant=True,
        form_id=100500,
        form_handler=dedent('''
                from plan.resources.handlers.financial_resources.balance.forward import process_form_forward
                result = process_form_forward(data, form_metadata)
                '''),
    )

    signature = make_signature(
        service=data.service,
        resource_type=balance_resource_type,
        user=data.staff.user
    )

    form = {
        'field_1': '{"question": {"id": 1}, "value": 12345}'
    }

    response = request(client, data, form=form, resource_type=balance_resource_type.pk, signature=signature)

    assert response.status_code == 200

    resource = Resource.objects.get(type=balance_resource_type.pk)
    # client_id должен быть строкой
    assert resource.attributes['client_id'] == '12345'
    assert resource.external_id == '12345'


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
def test_related_service_owner_role_approve_from_service(client, data, gdpr_resource_type):
    """
    Проверяем, что пользователь с определенной ролью из сервиса указанного в атрибутах
    может подтвердить ресурс
    """
    related_service = factories.ServiceFactory()
    resource = factories.ResourceFactory(
        type=gdpr_resource_type,
        attributes={'service_from_slug': related_service.slug}
    )

    sr = factories.ServiceResourceFactory(
        resource=resource,
        service=data.service,
    )

    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers) == []
    assert consumer_approvers is None

    # Добавим в связанный сервис человека, который может подтвердить
    staff = factories.StaffFactory()
    role = factories.RoleFactory()
    gdpr_resource_type.supplier_roles.add(role)
    factories.ServiceMemberFactory(staff=staff, service=related_service, role=role)
    factories.ServiceMemberFactory(service=related_service)
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers.values_list('id', flat=True)) == [staff.id]
    assert consumer_approvers is None


def test_tvm_approve_policy_service(client, data):
    """
    Проверяем, что пользователь с определенной ролью из сервиса у ресурса
    может подтвердить перенос ресурса
    """
    resource_type = factories.ResourceTypeFactory(approve_policy=APPROVE_POLICY.TVM_RESOURCES)
    related_service = factories.ServiceFactory()
    resource = factories.ResourceFactory(type=resource_type)

    sr = factories.ServiceResourceFactory(
        resource=resource,
        service=data.service,
    )

    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert supplier_approvers is None
    assert consumer_approvers is None

    # При этом если происходит перенос - нужно подтверждение
    obsolete = factories.ServiceResourceFactory(
        resource=resource,
        service=related_service,
        state='obsolete',
    )
    sr.obsolete = obsolete
    sr.save()
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers) == []
    assert list(consumer_approvers) == []

    # Добавим в сервис человека, который может подтвердить
    staff = factories.StaffFactory()
    role = factories.RoleFactory()
    resource_type.consumer_roles.add(role)
    resource_type.supplier_roles.add(role)
    staff_2 = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=staff_2, service=related_service, role=role)
    factories.ServiceMemberFactory(staff=staff, service=data.service, role=role)
    supplier_approvers, consumer_approvers = sr.get_approvers()
    assert list(supplier_approvers.values_list('id', flat=True)) == [staff_2.id]
    assert list(consumer_approvers.values_list('id', flat=True)) == [staff.id]
