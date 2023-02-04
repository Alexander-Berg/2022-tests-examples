import pytest
from unittest.mock import patch

from django.core.urlresolvers import reverse
from django.test.utils import override_settings
from django.conf import settings

from plan.denormalization.tasks import update_denormalized_field
from plan.oebs import models as oebs_models
from plan.oebs import exceptions
from plan.oebs.constants import OEBS_FLAGS, OEBS_REVENUE_FLAG, STATES
from plan.oebs.models import ACTIONS
from plan.oebs.utils import finalize_service_request
from plan.services.models import (
    Service,
    ServiceTag,
)
from plan.services.state import SERVICE_STATE
from plan.services.tasks import (
    close_service,
    delete_service,
    move_service,
)
from plan.resources.models import ServiceResource
from common import factories


@pytest.fixture()
def agreement_filter_value(request, move_agreement):
    try:
        attr = getattr(move_agreement, request.param)
    except AttributeError:
        return request.param
    return attr


def find_service_in_result(result, service_pk):
    if result['id'] == service_pk:
        return result
    for child in result['child_nodes']:
        service = find_service_in_result(child, service_pk)
        if service:
            return service
    return None


def recursive_state_assert(service_node, state):
    assert service_node['state'] == state
    for service in service_node['child_nodes']:
        recursive_state_assert(service, state)


@pytest.mark.parametrize('root_side', ('from', 'to'))
def test_move_root(client, move_agreement, oebs_data, oebs_service, root_side):
    service = factories.ServiceFactory(
        parent=oebs_service,
        use_for_hr=True,
    )
    factories.ServiceResourceFactory(
        resource=oebs_data.service_resource.resource,
        service=service,
        state=ServiceResource.GRANTED,
        attributes={
            'parent_oebs_id': '1235',
            'leaf_oebs_id': '2345',
        }
    )
    move_agreement.service = service
    move_agreement.save()

    if root_side == 'from':
        move_agreement.service.parent_id = None
        move_agreement.move_request.destination_id = oebs_service.id
    else:
        move_agreement.service.parent = oebs_service
        move_agreement.move_request.destination_id = None

    move_agreement.service.save()
    move_agreement.move_request.save()

    update_denormalized_field('services.Service', move_agreement.service.id, 'ancestors')
    update_denormalized_field('services.Service', oebs_service.id, 'ancestors')

    print(move_agreement.service, move_agreement.service.id, move_agreement.service.ancestors, 888)
    print(oebs_service, oebs_service.id, oebs_service.ancestors, 8888)

    expected = factories.OEBSAgreementFactory(
        action=oebs_models.ACTIONS.MOVE,
        requester=move_agreement.requester,
        move_request=move_agreement.move_request,
        service=move_agreement.service,
        state=STATES.VALIDATING_IN_OEBS,
    )

    response = client.json.get(
        reverse('api-v4:oebs-agreement-list'),
        {'action': 'validate'}
    )
    assert response.status_code == 200

    data = response.json()['results']
    assert len(data) == 1
    agreement_data = data[0]
    assert agreement_data['agreement_id'] == expected.id
    if root_side == 'from':
        assert agreement_data['parent_id'] == oebs_service.id
        assert agreement_data['service']['parent_id'] == 0
    else:
        assert agreement_data['parent_id'] == 0
        assert agreement_data['service']['parent_id'] == oebs_service.id


def test_agreement_list(client, move_agreement):
    expected = factories.OEBSAgreementFactory(
        action=oebs_models.ACTIONS.MOVE,
        requester=move_agreement.requester,
        move_request=move_agreement.move_request,
        service=move_agreement.service,
    )
    factories.OEBSAgreementFactory(
        action=oebs_models.ACTIONS.MOVE,
        requester=move_agreement.requester,
        move_request=move_agreement.move_request,
        service=factories.ServiceFactory(),
    )
    url = reverse('api-frontend:oebs-agreement-list')
    response = client.json.get(url, {'service': move_agreement.service.id})
    assert response.status_code == 200

    data = response.json()['results']
    assert len(data) == 2
    agreement_data = data[0]
    assert agreement_data['id'] == expected.id


@pytest.mark.parametrize(
    'action', [oebs_models.ACTIONS.RENAME, oebs_models.ACTIONS.CHANGE_FLAGS]
)
def test_agreement_list_name(client, action):
    expected = factories.OEBSAgreementFactory(
        action=action,
        state=STATES.VALIDATING_IN_OEBS,
        attributes={
            'new_ru': 'some_new_name',
            'new_en': 'new_en_name',
        },
    )
    update_denormalized_field('services.Service', expected.service.id, 'ancestors')
    response = client.json.get(
        reverse('api-v4:oebs-agreement-list'),
        {'action': 'validate'}
    )
    assert response.status_code == 200

    data = response.json()['results']
    assert len(data) == 1
    agreement_data = data[0]
    assert agreement_data['agreement_id'] == expected.id
    if action == oebs_models.ACTIONS.RENAME:
        assert agreement_data['service']['name'] == 'some_new_name'
        assert agreement_data['service']['name_en'] == 'new_en_name'
    else:
        assert agreement_data['service']['name'] == expected.service.name
        assert agreement_data['service']['name_en'] == expected.service.name_en


@pytest.mark.parametrize(
    'filter_key, agreement_filter_value, results',
    [
        ('state', 'state', 1),
        ('service', 'service_id', 1),
        ('start_date__gte', 'start_date', 1),
        ('start_date__lte', 'start_date', 1),
        ('service__in', '%s,%s' % (1 << 20, 1 << 21), 0),
        ('state__in', oebs_models.STATES.FAILED, 0),
        ('end_date__range', '2020-12-01,2020-12-31', 0),
    ],
    indirect=['agreement_filter_value']
)
def test_agreement_filter(client, move_agreement, filter_key, agreement_filter_value, results):
    url = reverse('api-frontend:oebs-agreement-list')
    response = client.json.get(url, {filter_key: agreement_filter_value})
    assert response.status_code == 200

    data = response.json()['results']
    assert len(data) == results
    assert not results or data[0]['id'] == move_agreement.id


def test_agreement_detail(client, move_agreement):
    url = reverse('api-frontend:oebs-agreement-detail', args=[move_agreement.id])
    response = client.json.get(url)
    assert response.status_code == 200

    data = response.json()
    assert data['id'] == move_agreement.id


@patch('plan.services.models.get_unclosable_services')
@pytest.mark.parametrize(
    'target_state,task,req_attr,readonly_state',
    [
        (Service.states.CLOSED, close_service, 'close_request', Service.CLOSING),
        (Service.states.DELETED, delete_service, 'delete_request', Service.DELETING),
        (Service.states.IN_DEVELOP, move_service, 'move_request', Service.MOVING),
    ]
)
def test_change_state_oebs_related(get_unclosable_services, client, person, oebs_service, dormant_oebs_service,
                                   target_state, task, req_attr, readonly_state):
    get_unclosable_services.return_value = []
    service = oebs_service
    client.login(person.login)
    if req_attr != 'move_request':
        assert service.state != target_state
    assert service.readonly_state is None
    assert service.active_agreement is None
    if req_attr == 'move_request':
        response = client.json.post(
            reverse('services-api:moves-list'),
            {
                'service': service.id,
                'destination': dormant_oebs_service.id,
            }
        )
    else:
        response = client.json.patch(
            reverse('services-api:service-detail', args=[service.pk]),
            {
                'state': target_state,
            }
        )

    assert response.status_code == 200
    service.refresh_from_db()
    assert service.readonly_state == readonly_state
    assert service.state == Service.states.IN_DEVELOP
    agreement = service.active_agreement
    assert agreement is not None

    request = getattr(agreement, req_attr)
    assert agreement.state == oebs_models.STATES.VALIDATING_IN_OEBS
    with pytest.raises(exceptions.NotAppliedOEBSAgreementExists):
        task(request.id)

    service.refresh_from_db()
    assert service.readonly_state == readonly_state
    assert service.state == Service.states.IN_DEVELOP

    agreement.state = oebs_models.STATES.APPLIED
    agreement.save()
    with patch('plan.api.idm.actions.move_service'):
        with patch('plan.api.idm.actions.delete_service'):
            with patch('plan.api.idm.actions.assert_service_node_exists'):
                with patch('plan.services.tasks.rerequest_roles'):
                    finalize_service_request(agreement)

    service.refresh_from_db()
    assert service.readonly_state is None
    assert service.state == target_state

    if req_attr == 'move_request':
        assert service.parent == dormant_oebs_service


@pytest.mark.parametrize(
    'target_state',
    [
        Service.states.CLOSED,
        Service.states.DELETED,
        Service.MOVING,
    ]
)
@pytest.mark.parametrize(
    'agreement_state',
    [
        oebs_models.STATES.OPEN,
        oebs_models.STATES.FAILED,
        oebs_models.STATES.APPLIED,
        oebs_models.STATES.DECLINED,
        oebs_models.STATES.APPLYING_IN_OEBS,
    ]
)
def test_validate_change_state(client, oebs_data, oebs_service, target_state, agreement_state):
    service = oebs_data.service
    client.login(oebs_data.staff.login)
    oebs_models.OEBSAgreement.objects.create(
        service=service,
        state=agreement_state,
    )
    with patch('plan.services.tasks.drop_requests'):
        with patch('plan.services.tasks.delete_service'):
            with patch('plan.services.models.get_unclosable_services') as close_patcher:
                close_patcher.return_value = []
                if target_state == Service.MOVING:
                    response = client.json.post(
                        reverse('services-api:moves-list'),
                        {
                            'service': service.id,
                            'destination': oebs_service.id
                        }
                    )
                else:
                    response = client.json.patch(
                        reverse('services-api:service-detail', args=[service.pk]),
                        {
                            'state': target_state,
                        }
                    )
    if agreement_state not in oebs_models.STATES.FINAL_STATES:
        status_code = 400
    else:
        status_code = 200

    assert response.status_code == status_code


@pytest.mark.parametrize(
    'target_state',
    [
        Service.states.CLOSED,
        Service.states.DELETED,
        Service.states.IN_DEVELOP,
        Service.states.NEEDINFO,
    ]
)
@pytest.mark.parametrize(
    'initial_state',
    [
        Service.states.CLOSED,
        Service.states.IN_DEVELOP,
        Service.states.NEEDINFO,
    ]
)
def test_create_agreement_change_state(client, oebs_service, target_state, initial_state):
    oebs_service.state = initial_state
    oebs_service.save()
    oebs_models.OEBSAgreement.objects.create(
        service=oebs_service,
        state=oebs_models.STATES.OPEN,
    )

    response = client.json.patch(
        reverse('services-api:service-detail', args=[oebs_service.pk]),
        {
            'state': target_state,
        }
    )
    oebs_service.refresh_from_db()
    if (
        target_state in Service.states.ACTIVE_STATES and
        initial_state in Service.states.ACTIVE_STATES
    ):
        assert response.status_code == 200
        assert oebs_service.state == target_state
    elif target_state == Service.states.DELETED:
        assert response.status_code == 403
        assert oebs_service.state == initial_state
    else:
        assert response.status_code == 400
        assert oebs_service.state == initial_state


def validate_node(service_node, service_id, group_oebs_id, leaf_oebs_id, child_nodes_count):
    assert service_node['id'] == service_id
    assert service_node['group_oebs_id'] == group_oebs_id
    assert service_node['leaf_oebs_id'] == leaf_oebs_id
    assert len(service_node['child_nodes']) == child_nodes_count


def validate_result_structure(result, service_tree):
    assert result['id'] == service_tree.root_service.id
    assert result['group_oebs_id'] == service_tree.root_oebs_sr.attributes['parent_oebs_id']
    assert result['leaf_oebs_id'] is None
    child_nodes = sorted(result['child_nodes'], key=lambda c: c['id'])

    validate_node(
        service_node=child_nodes[0], service_id=service_tree.service1.id,
        group_oebs_id=service_tree.oebs1_sr.attributes['parent_oebs_id'], leaf_oebs_id=None,
        child_nodes_count=1
    )

    validate_node(
        service_node=child_nodes[0]['child_nodes'][0], service_id=service_tree.service3.id,
        group_oebs_id=None, leaf_oebs_id=service_tree.oebs3_sr.attributes['leaf_oebs_id'],
        child_nodes_count=0
    )

    validate_node(
        service_node=child_nodes[1], service_id=service_tree.service2.id,
        group_oebs_id=None, leaf_oebs_id=None,
        child_nodes_count=1
    )

    validate_node(
        service_node=child_nodes[1]['child_nodes'][0], service_id=service_tree.service5.id,
        group_oebs_id=None, leaf_oebs_id=service_tree.oebs5_sr.attributes['leaf_oebs_id'],
        child_nodes_count=0
    )


@pytest.mark.parametrize('oebs_flag', ['use_for_hardware', 'use_for_hr', 'use_for_procurement'])
def test_oebs_tree_change_flags(client, service_tree, responsible_role, django_assert_num_queries, oebs_flag):
    """
    Меняем флаги у сервиса.
    При построении дерева подменяем данные только у текущего.
    """

    service = service_tree.service2
    client.login(service_tree.owner.login)
    factories.ServiceMemberFactory(service=service, role=responsible_role, staff=service_tree.owner)

    response = client.json.patch(
        reverse('api-frontend:service-detail', args=[service.pk]),
        {
            oebs_flag: True,
            'use_for_revenue': True,
        }
    )
    assert response.status_code == 200

    service.refresh_from_db()
    agreement = service.oebs_agreements.first()

    with django_assert_num_queries(9):
        # 1 select intranet_staff join auth_user
        # 1 select auth_permission

        # 1 select oebs_oebsagreement
        # 2 select services_services (1 для root_service и 1 для tree_services)
        # 1 resources_serviceresource
        # 1 services_serviceclosure

        # 1 pg_is_in_recovery()
        # 1 select в waffle readonly switch

        response = client.get(reverse('api-frontend:oebs-tree-detail', args=[agreement.pk]))

    assert response.status_code == 200
    result = response.json()

    validate_result_structure(result, service_tree)
    service = find_service_in_result(result, agreement.service.pk)
    assert service['agreement']['action'] == ACTIONS.CHANGE_FLAGS
    assert service['agreement']['issue'] == agreement.issue
    assert service[oebs_flag]
    assert service['use_for_revenue']
    missing_oebs_flags = set(OEBS_FLAGS.keys()) - {oebs_flag, OEBS_REVENUE_FLAG}
    for missing_flag in missing_oebs_flags:
        assert not service[missing_flag]
    # проверим только один уровень вложенности
    for child in service['child_nodes']:
        assert not child['use_for_hardware']
        assert not child['use_for_hr']
        assert not child['use_for_procurement']
        assert not child['use_for_revenue']


@pytest.mark.parametrize('action', [ACTIONS.DELETE, ACTIONS.CLOSE])
def test_oebs_tree_close_or_delete(client, service_tree, action, django_assert_num_queries):
    agreement = factories.OEBSAgreementFactory(
        service=service_tree.service2,
        action=action,
    )

    with django_assert_num_queries(9):
        # 1 select intranet_staff join auth_user
        # 1 select auth_permission

        # 1 select oebs_oebsagreement
        # 2 select services_services (1 для root_service и 1 для tree_services)
        # 1 resources_serviceresource
        # 1 services_serviceclosure

        # 1 pg_is_in_recovery()
        # 1 select в waffle readonly switch

        response = client.get(reverse('api-frontend:oebs-tree-detail', args=[agreement.pk]))

    assert response.status_code == 200
    result = response.json()

    validate_result_structure(result, service_tree)
    service = find_service_in_result(result, agreement.service.pk)

    assert service['agreement']['action'] == action
    assert service['agreement']['issue'] == agreement.issue
    assert service['state'] == SERVICE_STATE.CLOSED if action == ACTIONS.CLOSE else SERVICE_STATE.DELETED
    recursive_state_assert(service, service['state'])


@pytest.mark.parametrize('other_tree', (False, True))
def test_oebs_tree_move(client, service_tree, other_tree, django_assert_num_queries):
    destination = service_tree.other_root_service if other_tree else service_tree.service1
    move_request = factories.ServiceMoveRequestFactory(service=service_tree.service5, destination=destination)
    agreement = factories.OEBSAgreementFactory(
        service=service_tree.service5,
        action=ACTIONS.MOVE,
        move_request=move_request,
    )

    with django_assert_num_queries(9):
        # 1 select intranet_staff join auth_user
        # 1 select auth_permission

        # 1 select oebs_oebsagreement
        # 2 select services_services (1 для root_service и 1 для tree_services)
        # 1 resources_serviceresource
        # 1 services_serviceclosure

        # 1 pg_is_in_recovery()
        # 1 select в waffle readonly switch

        response = client.get(reverse('api-frontend:oebs-tree-detail', args=[agreement.pk]))
    assert response.status_code == 200
    result = response.json()

    if other_tree:
        assert result['id'] == service_tree.other_root_service.id
        assert len(result['child_nodes']) == 1
        child = result['child_nodes'][0]
        assert child['id'] == service_tree.service5.id
        assert len(child['child_nodes']) == 0
    else:
        assert result['id'] == service_tree.root_service.id
        assert len(result['child_nodes']) == 1
        child = result['child_nodes'][0]
        assert child['id'] == service_tree.service1.id
        assert len(child['child_nodes']) == 2
        assert {c['id'] for c in child['child_nodes']} == {
            service_tree.service3.id, service_tree.service5.id
        }

    service = find_service_in_result(result, agreement.service.pk)
    assert service['agreement']['action'] == ACTIONS.MOVE
    assert service['agreement']['issue'] == agreement.issue


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
@pytest.mark.parametrize('current_tag', (
    'oebs_use_for_hardware', 'oebs_use_for_hr', 'oebs_use_for_procurement', 'oebs_use_for_revenue'))
@pytest.mark.parametrize('new_tag', (
    'oebs_use_for_hardware', 'oebs_use_for_hr', 'oebs_use_for_procurement', 'oebs_use_for_revenue'))
@override_settings(RESTRICT_OEBS_TAGS=True)
def test_change_oebs_tags_restricted(client, staff_factory, api, oebs_tags, current_tag, new_tag):
    endpoint_path = f'{api}:service-detail'
    owner = staff_factory()
    service = factories.ServiceFactory(owner=owner)
    service.tags.add(ServiceTag.objects.get(slug=current_tag))
    new_tag_obj = ServiceTag.objects.get(slug=new_tag)
    client.login(owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': [new_tag_obj.id]}
    )
    if new_tag != current_tag:
        assert response.status_code == 400
        assert response.json()['error']['message']['ru'] == 'Редактирование тегов связанных с OEBS запрещено'
    else:
        assert response.status_code == 200
        assert service.tags.count() == 1


@pytest.mark.parametrize(
    'target_state',
    [
        Service.states.CLOSED,
        Service.states.DELETED,
        Service.states.NEEDINFO,
    ]
)
def test_validate_change_state_with_gradient_tag(client, oebs_data, target_state):
    service = oebs_data.service
    client.login(oebs_data.staff.login)
    tag, _ = ServiceTag.objects.get_or_create(slug=settings.GRADIENT_VS)
    service.tags.add(tag)

    response = client.json.patch(
        reverse('services-api:service-detail', args=[service.pk]),
        {
            'state': target_state,
        }
    )
    if target_state not in Service.states.ACTIVE_STATES:
        status_code = 400
    else:
        status_code = 200

    assert response.status_code == status_code


@pytest.mark.parametrize('action', ('apply', 'validate', 'smth'))
def test_get_active_agreements_action(client, oebs_data, action):
    update_denormalized_field('services.Service', oebs_data.service.id, 'ancestors')
    factories.OEBSAgreementFactory(
        service=oebs_data.service,
        state=oebs_models.STATES.APPLYING_IN_OEBS,
    )
    response = client.json.get(
        reverse('api-v4:oebs-agreement-list'),
        {'action': action}
    )
    if action == 'smth':
        assert response.status_code == 400
    else:
        assert response.status_code == 200

    response_json = response.json()
    if action == 'apply':
        assert len(response_json['results']) == 1
    elif action == 'validate':
        assert len(response_json['results']) == 0


def test_get_active_agreements(client, oebs_data, oebs_service, django_assert_num_queries):
    """
    api-v4:oebs-agreement-list возвращает активное согласование для valuestream-сервиса.
    """
    oebs_data.service.tags.add(factories.ServiceTagFactory(slug=settings.GRADIENT_VS))
    oebs_data.service.use_for_procurement = True
    oebs_data.service.save()

    new_parent = factories.ServiceFactory(owner=oebs_data.staff, parent=oebs_service)
    move_request = factories.ServiceMoveRequestFactory(
        service=oebs_data.service,
        destination=new_parent,
        requester=oebs_data.staff,
    )
    update_denormalized_field('services.Service', oebs_data.service.id, 'ancestors')
    update_denormalized_field('services.Service', new_parent.id, 'ancestors')

    agreement = factories.OEBSAgreementFactory(
        service=oebs_data.service,
        action=ACTIONS.MOVE,
        state=oebs_models.STATES.VALIDATING_IN_OEBS,
        move_request=move_request,
        attributes={
            'use_for_hr': True,
        }
    )
    with django_assert_num_queries(7):
        # 1 middleware
        # 1 content type
        # 3 view сам
        # 1 pg_is_in_recovery
        # 1 waffle
        response = client.json.get(
            reverse('api-v4:oebs-agreement-list'),
            {'action': 'validate'}
        )

    assert response.status_code == 200
    response_json = response.json()
    agreement_data = response_json['results'][0]
    assert agreement_data['agreement_id'] == agreement.id
    assert agreement_data['action'] == agreement.action
    assert agreement_data['use_for_hr'] is True
    assert agreement_data['use_for_procurement'] is True
    assert agreement_data['use_for_revenue'] is False
    assert agreement_data['parent_id'] == oebs_service.id
    assert agreement_data['service']['parent_id'] == oebs_data.service.parent_id
    assert agreement_data['service']['group_oebs_id'] == '123'
    assert agreement_data['service']['use_for_hr'] is False
    assert agreement_data['service']['use_for_procurement'] is True
    assert agreement_data['service']['is_vs'] is True
    assert agreement_data['service']['is_bu'] is False
    assert agreement_data['service']['is_exp'] is False
    assert agreement_data['service']['name'] == oebs_data.service.name


def test_get_active_agreements_moving(client, oebs_data, oebs_service, oebs_resource_type):
    """
    Проверяем корректность подсчета родителя
    """
    new_parent = factories.ServiceFactory(owner=oebs_data.staff, parent=oebs_service)
    oebs_resource = factories.ResourceFactory(type=oebs_resource_type)
    ServiceResource.objects.create_granted(
        service=new_parent,
        resource=oebs_resource,
        type_id=oebs_resource_type.id,
        attributes={
            'parent_oebs_id': '1234',
            'leaf_oebs_id': '2344',
        },
    )

    move_request = factories.ServiceMoveRequestFactory(
        service=oebs_data.service,
        destination=new_parent,
        requester=oebs_data.staff,
    )
    update_denormalized_field('services.Service', oebs_data.service.id, 'ancestors')
    update_denormalized_field('services.Service', new_parent.id, 'ancestors')

    agreement = factories.OEBSAgreementFactory(
        service=oebs_data.service,
        action=ACTIONS.MOVE,
        state=oebs_models.STATES.VALIDATING_IN_OEBS,
        move_request=move_request,
        attributes={
            'use_for_hr': True,
        }
    )
    response = client.json.get(
        reverse('api-v4:oebs-agreement-list'),
        {'action': 'validate'}
    )

    assert response.status_code == 200
    response_json = response.json()
    agreement_data = response_json['results'][0]
    assert agreement_data['agreement_id'] == agreement.id
    assert agreement_data['action'] == agreement.action
    assert agreement_data['parent_id'] == new_parent.id
    assert agreement_data['service']['parent_id'] == oebs_data.service.parent_id


@pytest.mark.parametrize('mock_tvm_service_ticket', [000], indirect=True)
def test_oebs_respond_access_denied(client, mock_tvm_service_ticket):
    response = client.json.post(
        reverse('api-v4:oebs-respond-list'),
        {
            'agreement_id': 1,
            'result': {
                'applied': True,
                'leaf_oebs_id': 1,
                'group_oebs_id': "2",
            },
        },
        HTTP_X_YA_SERVICE_TICKET='ticket',
    )
    assert response.status_code == 403
    assert response.json()['error']['code'] == 'permission_denied'


@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.OEBS_TVM_CLIENT_ID_LIST[0]], indirect=True)
def test_oebs_respond_agreement_not_found(client, mock_tvm_service_ticket):
    response = client.json.post(
        reverse('api-v4:oebs-respond-list'),
        {'agreement_id': 0},
        HTTP_X_YA_SERVICE_TICKET='ticket',
    )
    assert response.status_code == 404


@pytest.mark.parametrize('result', [
    {'validated': True},
    {'validated': False},
    {'applied': True, 'leaf_oebs_id': 1, 'group_oebs_id': '2'},
])
@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.OEBS_TVM_CLIENT_ID_LIST[0]], indirect=True)
def test_oebs_respond_inconsistent_state(client, move_agreement, mock_tvm_service_ticket, result):
    response = client.json.post(
        reverse('api-v4:oebs-respond-list'),
        {
            'agreement_id': move_agreement.id,
            'result': result,
        },
        HTTP_X_YA_SERVICE_TICKET='ticket',
    )
    assert response.status_code == 400
    assert response.json()['error']['detail'] == f'Inconsistent agreement state {STATES.OPEN}'


@pytest.mark.parametrize('stage', ['validated', 'applied'])
@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.OEBS_TVM_CLIENT_ID_LIST[0]], indirect=True)
def test_oebs_respond_agreement_error(client, mock_tvm_service_ticket, move_agreement, stage):
    if stage == 'applied':
        move_agreement.state = STATES.APPLYING_IN_OEBS
    else:
        move_agreement.state = STATES.VALIDATING_IN_OEBS
    move_agreement.save()

    with patch('plan.oebs.api.views.handle_oebs_error') as handle_oebs_error:
        with patch('plan.oebs.api.views.fail_service_request') as fail_service_request:
            response = client.json.post(
                reverse('api-v4:oebs-respond-list'),
                {
                    'agreement_id': move_agreement.id,
                    'result': {
                        stage: False,
                    },
                    'error': {'message': 'error'}
                },
                HTTP_X_YA_SERVICE_TICKET='ticket',
            )
    assert response.status_code == 200
    assert handle_oebs_error.called and fail_service_request.called


@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.OEBS_TVM_CLIENT_ID_LIST[0]], indirect=True)
def test_oebs_respond_validated(client, move_agreement, mock_tvm_service_ticket):
    move_agreement.state = STATES.VALIDATING_IN_OEBS
    move_agreement.save(update_fields=['state'])
    with patch('plan.oebs.api.views.start_oebs_approve_process') as start_oebs_approve_process:
        response = client.json.post(
            reverse('api-v4:oebs-respond-list'),
            {
                'agreement_id': move_agreement.id,
                'result': {
                    'validated': True,
                },
            },
            HTTP_X_YA_SERVICE_TICKET='ticket',
        )
    assert response.status_code == 200
    move_agreement.refresh_from_db()
    assert move_agreement.state == STATES.VALIDATED_IN_OEBS
    assert start_oebs_approve_process.apply_async.called


@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.OEBS_TVM_CLIENT_ID_LIST[0]], indirect=True)
def test_oebs_respond_applied(client, move_agreement, mock_tvm_service_ticket):
    move_agreement.state = STATES.APPLYING_IN_OEBS
    move_agreement.save(update_fields=['state'])
    with patch('plan.oebs.api.views.finish_oebs_approve_process') as finish_oebs_approve_process:
        response = client.json.post(
            reverse('api-v4:oebs-respond-list'),
            {
                'agreement_id': move_agreement.id,
                'result': {
                    'applied': True,
                    'leaf_oebs_id': 1,
                    'group_oebs_id': '2',
                },
            },
            HTTP_X_YA_SERVICE_TICKET='ticket',
        )
    assert response.status_code == 200
    move_agreement.refresh_from_db()
    assert move_agreement.state == STATES.APPLIED_IN_OEBS
    assert finish_oebs_approve_process.apply_async.called


@pytest.mark.parametrize('root_parent', (True, False))
def test_oebs_deviation_view(client, oebs_data, django_assert_num_queries, root_parent):
    oebs_parent = factories.ServiceFactory()
    expected_oebs_data = {
        'smth': 'test',
        'deviation_reason': 'parent',
    }
    new_parent = factories.ServiceFactory(parent=oebs_data.service.parent)
    oebs_data.service.parent.tags.add(factories.ServiceTagFactory(slug=settings.GRADIENT_VS))
    oebs_data.service.oebs_parent_id = oebs_parent.id
    oebs_data.service.parent_id = new_parent.id
    oebs_data.service.oebs_data = expected_oebs_data
    if root_parent:
        oebs_data.service.parent_id = None
    oebs_data.service.save()
    update_denormalized_field('services.Service', oebs_data.service.id, 'ancestors')

    with django_assert_num_queries(7):
        # 1 middleware
        # 1 content type
        # 3 view сам
        # 1 pg_is_in_recovery
        # 1 waffle
        response = client.json.get(
            reverse('api-v4:oebs-deviation-list'),
        )

    assert response.status_code == 200
    response_json = response.json()
    assert len(response_json['results']) == 1
    deviation_data = response_json['results'][0]

    assert deviation_data['slug'] == oebs_data.service.slug
    assert deviation_data['oebs_parent_id'] == oebs_parent.id
    assert deviation_data['oebs_parent']['slug'] == oebs_parent.slug
    assert deviation_data['leaf_oebs_id'] == '234'
    assert deviation_data['oebs_data'] == expected_oebs_data
    assert deviation_data['group_oebs_id'] == '123'
    if root_parent:
        assert deviation_data['parent'] is None
        assert deviation_data['abc_oebs_parent_id'] == 0
    else:
        assert deviation_data['abc_oebs_parent_id'] == new_parent.parent_id
        assert deviation_data['parent']['slug'] == new_parent.slug
