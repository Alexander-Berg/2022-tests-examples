import pretend
import pytest
from mock import patch, Mock

from django.core.urlresolvers import reverse
from django.conf import settings

from plan.idm.exceptions import IDMError
from plan.services.models import Service, ServiceCreateRequest

from common import factories


@pytest.fixture
def creates(data, owner_role):
    requested = factories.ServiceCreateRequestFactory(
        service=data.service,
        move_to=data.other_service,
        state=ServiceCreateRequest.REQUESTED,
        requester=data.staff
    )
    completed = factories.ServiceCreateRequestFactory(
        service=data.service,
        move_to=data.other_service,
        state=ServiceCreateRequest.COMPLETED,
        requester=data.staff,
        approver_incoming=data.big_boss,
    )
    rejected = factories.ServiceCreateRequestFactory(
        service=data.service,
        move_to=data.other_service,
        state=ServiceCreateRequest.REJECTED,
        requester=data.stranger,
        approver_incoming=data.big_boss,
    )
    approved = factories.ServiceCreateRequestFactory(
        service=data.service,
        move_to=data.other_service,
        state=ServiceCreateRequest.APPROVED,
        requester=data.stranger,
        approver_incoming=data.big_boss,
    )
    return pretend.stub(
        requested=requested,
        completed=completed,
        rejected=rejected,
        approved=approved,
    )


@pytest.mark.parametrize('directly', [True, False, None])
def test_create_service(creates, directly, client, owner_role, staff_factory):
    """
    Создаём сервис, проверим, что текущее поведение не сломалось:
        * непосредственно в песочнице, если directly == True или None (отсуствие равносильно True),
            тогда requester сам себе подтверждающий, запрос переходит в следующий статус PROCESSING_IDM;
        * или под другим сервисом, тогда подтверждающего нет, но запрос на создание переходит дальше по статусам
    """

    sandbox = Service.objects.get(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    owner = staff_factory()
    if directly is None:
        data = {
            'name': {'ru': 'Сервис', 'en': 'Service'},
            'slug': 'cat_1',
            'owner': owner.login,
        }

    else:
        parent = sandbox
        if not directly:
            parent = factories.ServiceFactory(parent=sandbox)

        data = {
            'name': {'ru': 'Сервис', 'en': 'Service'},
            'slug': 'cat_1',
            'parent': parent.pk,
            'owner': owner.login,
        }

    staff = staff_factory()
    client.login(staff.login)
    with patch('plan.services.tasks.actions') as mock_actions:
        mock_actions.add_service = Mock(side_effect=IDMError)
        response = client.json.post(
            reverse('api-v4:service-list'),
            data
        )

    assert response.status_code == 201
    service = Service.objects.get(slug='cat_1')

    create_requests = service.create_requests.first()
    assert create_requests.state == ServiceCreateRequest.PROCESSING_IDM
    if directly or directly is None:
        assert create_requests.approver_incoming == staff
    else:
        assert create_requests.approver_incoming is None


@pytest.mark.parametrize('state', ['requested', 'completed', 'rejected', 'approved'])
def test_approvers(creates, state, client):
    """
    Поверяем список апруверов.
    """

    create_request = ServiceCreateRequest.objects.get(state=state)
    create_request.move_to.fetch_owner()
    create_request.move_to.parent.fetch_owner()
    response = client.json.get(
        reverse('api-frontend:creates-approvers', args=[create_request.id]),
    )
    assert response.status_code == 200

    result = response.json()

    assert len(result['parent_approvers']['recommended']) == 1
    assert result['parent_approvers']['recommended'][0]['login'] == create_request.move_to.owner.login

    assert len(result['parent_approvers']['other']) == 1
    assert result['parent_approvers']['other'][0]['login'] == create_request.move_to.parent.owner.login


@pytest.mark.parametrize(
    'state',
    ['requested', 'completed', 'rejected', 'approved', 'processing_idm', 'processing_head', 'processing_abc'],
)
def test_get_request(creates, state, client, staff_factory):
    """
    Проверим данные запроса.
    Если запрос уже подтверждён или завершён, то экшенов быть не должно.
    """

    factories.ServiceCreateRequestFactory(
        state=ServiceCreateRequest.PROCESSING_IDM,
        move_to=factories.ServiceFactory(owner=staff_factory())
    )
    factories.ServiceCreateRequestFactory(
        state=ServiceCreateRequest.PROCESSING_HEAD,
        move_to=factories.ServiceFactory(owner=staff_factory())
    )
    factories.ServiceCreateRequestFactory(
        state=ServiceCreateRequest.PROCESSING_ABC,
        move_to=factories.ServiceFactory(owner=staff_factory())
    )

    create_request = ServiceCreateRequest.objects.get(state=state)
    create_request.move_to.fetch_owner()
    ServiceCreateRequest.objects.exclude(pk=create_request.id).delete()

    client.login(create_request.move_to.owner.login)
    response = client.json.get(
        reverse('api-frontend:creates-detail', args=[create_request.id]),
    )
    assert response.status_code == 200

    result = response.json()

    assert result['service']['id'] == create_request.service.id
    assert result['move_to']['id'] == create_request.move_to.id
    assert result['state'] == state
    assert result['state_display']['en'] == create_request.state.title()
    assert result['state_display']['ru'] == create_request.get_state_display()
    assert result['requester']['login'] == create_request.requester.login
    if state == 'requested':
        assert result['approver_incoming'] is None
        assert set(response.json()['actions']) == {'approve', 'reject'}

    else:
        if create_request.approver_incoming is not None:
            assert result['approver_incoming']['login'] == create_request.approver_incoming.login
        assert result['actions'] == []


def test_get_request_not_action(creates, client):
    """
    Проверим экшены для стаффа, который не может подтверждать/отклонять.
    """

    create_request = creates.requested
    ServiceCreateRequest.objects.exclude(pk=create_request.id).delete()
    response = client.json.get(
        reverse('api-frontend:creates-detail', args=[create_request.id]),
    )
    assert response.status_code == 200
    assert response.json()['actions'] == []


@pytest.mark.parametrize('state', ['requested', 'completed', 'rejected', 'approved'])
@pytest.mark.parametrize('permission', [True, False])
def test_approve_create_request(creates, state, permission, client, staff_factory):
    """
    Попробуем подтвердить запрос. Запрос может подтвердить только рукодитель сервиса, указанного в запросе как move_to.
    Нельзя подвтердить CreateRequest, если он уже подтверждён или завершён.
    """

    create_request = ServiceCreateRequest.objects.get(state=state)
    create_request.move_to.fetch_owner()
    ServiceCreateRequest.objects.exclude(pk=create_request.id).delete()

    login = staff_factory().login
    if permission:
        login = creates.requested.move_to.owner.login

    client.login(login)
    response = client.json.post(
        reverse('api-frontend:creates-approve', args=[create_request.id])
    )

    create_request.refresh_from_db()
    if state == 'requested' and permission:
        assert response.status_code == 204
        assert create_request.state == ServiceCreateRequest.APPROVED
        assert create_request.approver_incoming.login == login

    elif state in ['completed', 'rejected', 'approved']:
        assert response.status_code == 400
        assert response.json()['error']['message'] == {
            'ru': 'Решение по этому запросу уже было принято',
            'en': 'This request was already processed',
        }

    else:
        assert response.status_code == 403
        assert response.json()['error']['message'] == {
            'ru': 'Извините, вам сюда нельзя',
            'en': "Sorry, we can't let you in here",
        }


@pytest.mark.parametrize('state', ['requested', 'completed', 'rejected', 'approved'])
@pytest.mark.parametrize('permission', [True, False])
def test_rejecte_create_request(creates, state, permission, client, staff_factory):
    """
    Попробуем подтвердить запрос. Запрос может подтвердить только рукодитель сервиса, указанного в запросе как move_to.
    Нельзя подвтердить CreateRequest, если он уже подтверждён или завершён.
    """
    create_request = ServiceCreateRequest.objects.get(state=state)
    create_request.move_to.fetch_owner()
    ServiceCreateRequest.objects.exclude(pk=create_request.id).delete()

    login = staff_factory().login
    if permission:
        login = creates.requested.move_to.owner.login

    client.login(login)
    response = client.json.post(
        reverse('api-frontend:creates-reject', args=[create_request.id])
    )

    create_request.refresh_from_db()
    if state == 'requested' and permission:
        assert response.status_code == 204
        assert create_request.state == ServiceCreateRequest.REJECTED
        assert create_request.approver_incoming.login == login

    elif state in ['completed', 'rejected', 'approved']:
        assert response.status_code == 400
        assert response.json()['error']['message'] == {
            'ru': 'Решение по этому запросу уже было принято',
            'en': 'This request was already processed',
        }

    else:
        assert response.status_code == 403
        assert response.json()['error']['message'] == {
            'ru': 'Извините, вам сюда нельзя',
            'en': "Sorry, we can't let you in here",
        }


def test_options_permission(creates, client):
    """
    Проверим пермишены.
    """
    create_request = creates.requested
    ServiceCreateRequest.objects.exclude(pk=create_request.id).delete()
    client.login(create_request.move_to.owner.login)
    response = client.json.options(
        reverse('api-frontend:creates-reject', args=[create_request.id])
    )

    assert response.status_code == 200
    assert response.json()['permissions'] == ['can_approve', 'can_cancel']

    client.login(create_request.service.owner.login)
    response = client.json.options(
        reverse('api-frontend:creates-reject', args=[create_request.id])
    )

    assert response.status_code == 200
    assert response.json()['permissions'] == []


@pytest.mark.parametrize('state', ['requested', 'completed', 'rejected', 'approved'])
def test_filter_service(creates, state, client, data):
    """
    Фильтруем по сервису.
        * у metaservice нет ни одного запроса,
        * у child есть 1 запрос (активный или завершенный),
        * у service их 3 (хотя в норме так быть не должно, но тут тестируем фильтры).

    Запрашиваем реквесты для metaservice:
        => реквесты child'а и  service'а не должны туда попадать ни в каких состояних.
    Запрашиваем реквесты для service:
        => реквест child'а не должен попадать в результаты, ни в актвином, ни в завершенном состоянии.
    """

    factories.ServiceCreateRequestFactory(
        service=data.child,
        move_to=data.other_service,
        state=state,
        requester=data.staff
    )
    creates.approved.delete()

    response = client.json.get(
        reverse('api-frontend:creates-list'),
        {
            'service': data.metaservice.id,
        }
    )
    assert response.status_code == 200
    assert len(response.json()['results']) == 0

    response = client.json.get(
        reverse('api-frontend:creates-list'),
        {
            'service': data.service.id,
        }
    )
    assert response.status_code == 200

    result = response.json()['results']
    create_requests = set([creates.requested.id, creates.completed.id, creates.rejected.id])
    assert len(result) == 3
    assert set([create_request['id'] for create_request in result]) == create_requests


@pytest.mark.parametrize('state', ['requested', 'completed', 'rejected', 'approved'])
def test_filter_move_to(creates, state, client, data):
    """
    Фильтруем по родителю, куда добавляется сервис.
        * в other_service добавляется 3 запроса,
        * в child добавляется 1 запрос,
        * в service никто не добавляется.

    Фильтруем запросы для service:
        => реквесты child'а и  other_service'а не должны туда попадать ни в каких состояних.
    Фильтруем запросы для other_service:
        => реквест child'а не должен попадать в результаты, ни в актвином, ни в завершенном состоянии.
    """

    factories.ServiceCreateRequestFactory(
        service=data.metaservice,
        move_to=data.child,
        state=state,
        requester=data.staff
    )
    creates.approved.delete()

    response = client.json.get(
        reverse('api-frontend:creates-list'),
        {
            'move_to': data.service.id,
        }
    )
    assert response.status_code == 200
    assert len(response.json()['results']) == 0

    response = client.json.get(
        reverse('api-frontend:creates-list'),
        {
            'move_to': data.other_service.id,
        }
    )
    assert response.status_code == 200

    result = response.json()['results']
    create_requests = set([creates.requested.id, creates.completed.id, creates.rejected.id])
    assert len(result) == 3
    assert set([create_request['id'] for create_request in result]) == create_requests


@pytest.mark.parametrize('kind', ['active', 'inactive'])
def test_filter_kind(creates, kind, client, data):
    """
    Фильтруем по активности.
    """

    creates.approved.service = data.child
    creates.approved.save()
    response = client.json.get(
        reverse('api-frontend:creates-list'),
        {
            'kind': kind,
        }
    )
    assert response.status_code == 200

    result = response.json()['results']
    if kind == 'active':
        create_requests = set([creates.requested.id, creates.approved.id])
    else:
        create_requests = set([creates.completed.id, creates.rejected.id])
    assert len(result) == 2
    assert set([create_request['id'] for create_request in result]) == create_requests


def test_filter_by_requester(creates, client, data):
    """
    Фильтруем по реквестеру.
    Всего 4 запроса, два из них созданы data.staff
    """

    creates.approved.service = data.child
    creates.approved.save()
    assert ServiceCreateRequest.objects.count() == 4

    response = client.json.get(
        reverse('api-frontend:creates-list'),
        {
            'requester': data.staff.login,
        }
    )
    assert response.status_code == 200

    result = response.json()['results']
    create_requests = {creates.requested.id, creates.completed.id}
    assert len(result) == 2
    assert set([create_request['id'] for create_request in result]) == create_requests

    response = client.json.get(
        reverse('api-frontend:creates-list'),
        {
            'requester': data.big_boss.login,
        }
    )
    assert response.status_code == 200
    assert len(response.json()['results']) == 0


@pytest.mark.parametrize('state', ['requested', 'completed', 'rejected', 'approved'])
def test_filter_state(creates, state, client, data):
    """
    Фильтруем по статусу.
    В каждом из статусов существует по 1 запросу.
    """

    creates.approved.service = data.child
    creates.approved.save()
    create_requests = ServiceCreateRequest.objects.filter(state=state)
    assert create_requests.count() == 1
    create_request = create_requests.first()

    response = client.json.get(
        reverse('api-frontend:creates-list'),
        {
            'state': state,
        }
    )
    assert response.status_code == 200

    result = response.json()['results']
    assert len(result) == 1
    assert result[0]['id'] == create_request.id


@pytest.mark.parametrize('only_mine', [True, False])
@pytest.mark.parametrize('with_descendants', [True, False, None])
def test_filter_only_mine(creates, only_mine, with_descendants, client, data):
    """
    Отфильтровываем запросы, которые пользователь может подтвердить.
    Если True, то отфильтруем прямые запросы, иначе с учетом иерархии.
    """

    creates.approved.service = data.child
    creates.approved.save()

    direct_request = factories.ServiceCreateRequestFactory(
        move_to=data.metaservice,
    )

    other_request = factories.ServiceCreateRequestFactory(
        move_to=factories.ServiceFactory(),
    )

    param = {
        'only_mine': only_mine,
    }

    if with_descendants is not None:
        param['with_descendants'] = with_descendants

    client.login(data.big_boss.login)
    response = client.json.get(
        reverse('api-frontend:creates-list'),
        param
    )
    assert response.status_code == 200

    result = response.json()['results']
    create_requests = set([create_request['id'] for create_request in result])

    if only_mine and with_descendants:
        assert len(result) == 2
        assert create_requests == {direct_request.id, creates.requested.id}

    elif only_mine and not with_descendants:
        assert len(result) == 1
        assert create_requests == {direct_request.id}

    else:
        assert len(result) == 6
        assert create_requests == {
            direct_request.id,
            other_request.id,
            creates.requested.id,
            creates.approved.id,
            creates.completed.id,
            creates.rejected.id,
        }


@pytest.mark.parametrize('with_descendants', [True, False, None])
def test_filter_only_mine_not_create_request(creates, with_descendants, client, data, staff_factory):
    """
    Отфильтровываем запросы, которые пользователь может подтвердить.
    Если True, то отфильтруем прямые запросы, иначе с учетом иерархии.
    Случай, когда у пользователя вообще нет запросов для подтверждения.
    """

    creates.approved.service = data.child
    creates.approved.save()

    param = {
        'only_mine': True,
    }

    if with_descendants is not None:
        param['with_descendants'] = with_descendants

    client.login(staff_factory().login)

    response = client.json.get(
        reverse('api-frontend:creates-list'),
        param
    )
    assert response.status_code == 200
    assert len(response.json()['results']) == 0


@pytest.mark.parametrize('only_mine', ['True', 'true', '1'])
def test_filter_only_mine_true(creates, only_mine, client, data):
    """
    Фильтруем only_mine, разные варианты написания
    """

    creates.approved.service = data.child
    creates.approved.save()

    direct_request = factories.ServiceCreateRequestFactory(
        move_to=data.metaservice,
    )

    factories.ServiceCreateRequestFactory(
        move_to=factories.ServiceFactory(),
    )

    param = {
        'only_mine': only_mine,
    }

    client.login(data.big_boss.login)
    response = client.json.get(
        reverse('api-frontend:creates-list'),
        param
    )
    assert response.status_code == 200

    result = response.json()['results']
    create_requests = set([create_request['id'] for create_request in result])

    assert len(result) == 1
    assert create_requests == {direct_request.id}
