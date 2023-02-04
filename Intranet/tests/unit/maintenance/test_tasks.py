from datetime import datetime, timedelta
from urllib.parse import urljoin

from freezegun import freeze_time
from mock import call, patch, Mock
import pytest
from waffle.testutils import override_switch

from django.conf import settings
from django.core import mail
from django.utils import timezone

from plan.common.person import Person
from plan.idm.exceptions import IDMError
from plan.maintenance import tasks
from plan.services.models import Service, ServiceMember, ServiceCreateRequest, ServiceMoveRequest

from common import factories
from unit.idm.conftest import patch_tvm  # noqa: F401
from plan.oebs.constants import STATES, ACTIONS
from utils import MockIdmResponse, Response

pytestmark = pytest.mark.django_db


@pytest.fixture
def patched_session(owner_role):
    node = {
        'slug': str(owner_role.id),
        'value_path': '/services/diamond_dogs/*/{}/'.format(owner_role.id)
    }

    mock_response = MockIdmResponse({
        'meta': {
            'total_count': 2,
            'next': None
        },
        'objects': [
            {
                'user': {
                    'username': 'venom_snake'
                },
                'state': 'granted',
                'id': 1984,
                'node': node,
                'granted_at': '1984-02-26T06:21:01.814505+00:00'
            },
            {
                'user': {
                    'username': 'big_boss'
                },
                'state': 'granted',
                'id': 1964,
                'node': node,
                'granted_at': '1964-09-02T06:21:01.814505+00:00'
            }
        ]
    })

    def mocked_session_send(*args, **kwargs):
        return Response(200, mock_response.response)

    with patch('requests.Session.send') as patched:
        patched.side_effect = mocked_session_send
        yield patched


def test_fix_duplicate_heads(owner_role, patched_session, patch_tvm):  # noqa: F811
    diamond_dogs = factories.ServiceFactory(slug='diamond_dogs')

    venom = factories.StaffFactory(login='venom_snake')
    big_boss = factories.StaffFactory(login='big_boss')
    master_miller = factories.StaffFactory(login='master_miller')

    with freeze_time(datetime(1975, 3, 16)):
        old = factories.ServiceMemberFactory(service=diamond_dogs, role=owner_role, staff=venom)
    true = factories.ServiceMemberFactory(service=diamond_dogs, role=owner_role, staff=venom)

    other = factories.ServiceMemberFactory(service=diamond_dogs, role=owner_role, staff=big_boss)
    absent = factories.ServiceMemberFactory(service=diamond_dogs, role=owner_role, staff=master_miller)

    with patch('plan.maintenance.tasks.deprive_role') as deprive_role:
        tasks.fix_duplicate_heads()

    deprive_role.delay.assert_called_with(
        other.id,
        'Роль отозвана, так как у сервиса было найдено несколько руководителей.'
    )

    assert not ServiceMember.objects.filter(id=absent.id).exists()
    assert not ServiceMember.objects.filter(id=old.id).exists()
    assert ServiceMember.objects.filter(id=true.id).exists()
    assert ServiceMember.objects.filter(staff=venom, role=owner_role, service=diamond_dogs).count() == 1


@patch('plan.idm.nodes.get_service_node')
@patch('plan.idm.nodes.get_service_roles_node')
@patch('plan.maintenance.tasks.send_to_team')
def test_check_nodes(send_to_team, get_service_node, get_service_roles_node):
    service = factories.ServiceFactory()

    mocked_node = Mock()
    mocked_node.exists.return_value = True
    mocked_node.register.return_value = IDMError
    get_service_node.return_value = get_service_roles_node.return_value = mocked_node

    def mocked_role_nodes(*args, **kwargs):
        for i in range(10):
            mock = Mock()
            mock.slug = str(i)
            mock.exists.return_value = bool(i % 2)
            mock.register.side_effect = IDMError
            yield mock

    with patch('plan.idm.nodes.get_service_role_nodes', new=mocked_role_nodes):
        tasks.check_nodes()

    assert send_to_team.called

    check_result = send_to_team.call_args[1]['context']['stats'][service.id]

    assert check_result['service'] == service
    assert not check_result['missing_nodes']
    assert len(check_result['missing_role_nodes']) == 5
    assert [node.slug for node in check_result['missing_role_nodes']] == [str(i) for i in range(0, 10, 2)]


@patch('plan.idm.nodes.get_service_node')
@patch('plan.idm.nodes.get_service_roles_node')
def test_check_nodes_sends_mail(get_service_node, get_service_roles_node):
    factories.ServiceFactory()

    mocked_node = Mock()
    mocked_node.exists.return_value = True
    mocked_node.register.return_value = IDMError
    get_service_node.return_value = get_service_roles_node.return_value = mocked_node

    def mocked_role_nodes(*args, **kwargs):
        for i in range(10):
            mock = Mock()
            mock.slug_path = '/herpa/derpa/'
            mock.exists.return_value = bool(i % 2)
            mock.register.side_effect = IDMError
            yield mock

    with patch('plan.idm.nodes.get_service_role_nodes', new=mocked_role_nodes):
        tasks.check_nodes()

    assert len(mail.outbox) == 1

    email = mail.outbox[0]
    assert email.subject == 'Отчет о сверке узлов с IDM'
    assert 'Основные узлы не найдены в idm:' not in email.body
    assert email.body.count('/herpa/derpa/') == 5


@patch('plan.maintenance.tasks.finish_deleting')
@patch('plan.maintenance.tasks.finish_moving')
@patch('plan.maintenance.tasks.finish_creating')
@patch('plan.maintenance.tasks.finish_closing')
@pytest.mark.parametrize('has_active_agreement', [True, False])
def test_clear_readonly(finish_closing, finish_creating, finish_moving, finish_deleting, has_active_agreement):
    renaming = factories.ServiceFactory(
        readonly_state=Service.RENAMING,
        readonly_start_time=timezone.now()
    )
    factories.ServiceFactory(
        readonly_state=Service.CREATING,
        readonly_start_time=timezone.now()
    )
    moving = factories.ServiceFactory(
        readonly_state=Service.MOVING,
        readonly_start_time=timezone.now()
    )
    deleting = factories.ServiceFactory(
        readonly_state=Service.DELETING,
        readonly_start_time=timezone.now()
    )
    closing = factories.ServiceFactory(
        readonly_state=Service.CLOSING,
        readonly_start_time=timezone.now()
    )
    if has_active_agreement:
        for service in [moving, deleting, closing, renaming]:
            factories.OEBSAgreementFactory(
                service=service
            )

    yesterday = timezone.now() - timedelta(days=1)
    Service.objects.filter(readonly_state__isnull=False).update(modified_at=yesterday)

    tasks.clear_readonly()

    renaming.refresh_from_db()
    assert bool(renaming.readonly_state) is has_active_agreement
    assert finish_creating.delay.called
    assert finish_moving.delay.called is not has_active_agreement
    assert finish_deleting.delay.called is not has_active_agreement
    assert finish_closing.delay.called is not has_active_agreement


@pytest.mark.parametrize(
    ('action', 'readonly_state', 'request_type', 'factory', 'task', 'task_to_patch', 'celery_attr'),
    (
        (
            ACTIONS.MOVE, Service.MOVING, 'move_request', factories.ServiceMoveRequestFactory,
            tasks.finish_moving, 'move_service', 'apply_async',
        ),
        (
            ACTIONS.CLOSE, Service.CLOSING, 'close_request', factories.ServiceCloseRequestFactory,
            tasks.finish_closing, 'close_service', 'delay',
        ),
        (
            ACTIONS.DELETE, Service.DELETING, 'delete_request', factories.ServiceDeleteRequestFactory,
            tasks.finish_deleting, 'delete_service', 'delay',
        ),
    )
)
@pytest.mark.parametrize(
    'agreement_state',
    [
        STATES.APPLIED,
        STATES.FAILED,
        STATES.APPROVED,
        STATES.APPROVING
    ],
)
def test_finish_requests_with_active_agreement(
        agreement_state, action, readonly_state, request_type,
        factory, task, task_to_patch, celery_attr,
):
    service = factories.ServiceFactory(
        readonly_state=readonly_state,
        readonly_start_time=timezone.now()
    )
    request = factory(
        service=service,
        state=ServiceMoveRequest.PROCESSING_OEBS,
    )
    if request_type == 'move_request':
        request.approver_incoming=service.owner
        request.approver_outgoing=service.owner
        request.save()

    agreement = factories.OEBSAgreementFactory(
        action=action,
        service=service,
        state=agreement_state,
    )
    setattr(agreement, request_type, request)
    agreement.save()

    with patch('plan.idm.nodes.get_service_node'):
        with patch('plan.services.tasks.{}'.format(task_to_patch)) as patched_task:
            task(service.id)

    service.refresh_from_db()
    request.refresh_from_db()
    assert bool(service.readonly_state) is (agreement_state != STATES.FAILED)
    if agreement_state == STATES.APPLIED:
        assert getattr(patched_task, celery_attr).assert_called_once
    else:
        assert getattr(patched_task, celery_attr).assert_not_called

    if agreement_state == STATES.FAILED:
        assert request.state == ServiceMoveRequest.FAILED
    else:
        assert request.state == ServiceMoveRequest.PROCESSING_OEBS


@patch('plan.maintenance.tasks.finish_moving')
def test_clear_readonly_moving_not_readonly(finish_moving):
    """
    Есть сервис без ред-онли, но мув-реквест не завершен.
    Проверим, что вызываем допинывалку.
    """

    service = factories.ServiceFactory(
        readonly_state=Service.MOVING,
        readonly_start_time=timezone.now()
    )
    factories.ServiceCreateRequestFactory(service=service, state=ServiceCreateRequest.REQUESTED)

    yesterday = timezone.now() - timedelta(days=1)
    Service.objects.update(modified_at=yesterday)

    tasks.clear_readonly()
    assert finish_moving.delay.called


def test_clear_readonly_moving_not_readonly_after_idm_push():
    """
    Проверим что не запускаем процесс перемещения снова,
    если уже отправили запрос на перемещение в IDM
    """

    service = factories.ServiceFactory(
        readonly_state=Service.MOVING,
        readonly_start_time=timezone.now()
    )
    factories.ServiceMoveRequestFactory(
        service=service,
        state=ServiceMoveRequest.PROCESSING_IDM,
        start_moving_idm=True,
        approver_incoming=service.owner,
        approver_outgoing=service.owner,
    )

    yesterday = timezone.now() - timedelta(days=1)
    Service.objects.update(modified_at=yesterday)
    with patch('plan.maintenance.tasks.services_tasks.verify_and_move_service') as verify_and_move_service:
        tasks.clear_readonly()
    assert verify_and_move_service.apply_async.called


@patch('plan.maintenance.tasks.finish_deleting')
@patch('plan.maintenance.tasks.finish_moving')
@patch('plan.maintenance.tasks.finish_creating')
def test_clear_readonly_by_service_id(finish_creating, finish_moving, finish_deleting):
    renaming = factories.ServiceFactory(readonly_state=Service.RENAMING, readonly_start_time=timezone.now())
    factories.ServiceFactory(readonly_state=Service.CREATING, readonly_start_time=timezone.now())
    factories.ServiceFactory(readonly_state=Service.MOVING, readonly_start_time=timezone.now())
    factories.ServiceFactory(readonly_state=Service.DELETING, readonly_start_time=timezone.now())

    yesterday = timezone.now() - timedelta(days=1)
    Service.objects.filter(readonly_state__isnull=False).update(modified_at=yesterday)

    tasks.clear_readonly([renaming.id])

    renaming.refresh_from_db()
    assert renaming.readonly_state is None
    assert not finish_creating.delay.called
    assert not finish_moving.delay.called
    assert not finish_deleting.delay.called


@patch('plan.services.tasks.register_service')
def test_finish_creating_on_requested(register_service):
    service = factories.ServiceFactory(readonly_state=Service.CREATING, readonly_start_time=timezone.now())
    factories.ServiceCreateRequestFactory(service=service, state=ServiceCreateRequest.REQUESTED)

    tasks.finish_creating(service.id)

    assert register_service.delay.called


@patch('plan.services.tasks.register_service')
def test_finish_creating_on_processing(register_service):
    service = factories.ServiceFactory(readonly_state=Service.CREATING, readonly_start_time=timezone.now())
    factories.ServiceCreateRequestFactory(service=service, state=ServiceCreateRequest.PROCESSING_IDM)

    tasks.finish_creating(service.id)

    assert register_service.delay.called


@patch('plan.services.tasks.request_service_head')
def test_finish_creating_on_processing_head(request_service_head):
    service = factories.ServiceFactory(readonly_state=Service.CREATING, readonly_start_time=timezone.now())
    factories.ServiceCreateRequestFactory(service=service, state=ServiceCreateRequest.PROCESSING_HEAD)

    tasks.finish_creating(service.id)

    assert request_service_head.delay.called


@patch('plan.services.tasks.register_service')
@pytest.mark.parametrize('readonly_state', [True, False])
def test_finish_creating_no_request(register_service, robot, readonly_state):
    if readonly_state:
        service = factories.ServiceFactory(readonly_state=Service.CREATING, readonly_start_time=timezone.now())
    else:
        service = factories.ServiceFactory()

    tasks.finish_creating(service.id)
    service.refresh_from_db()

    assert register_service.apply_async.called
    assert ServiceCreateRequest.objects.filter(service=service).exists()
    assert service.readonly_state == Service.CREATING
    assert service.readonly_start_time is not None


@patch('plan.services.tasks.move_service')
def test_finish_moving_approved(move_service, superuser):
    service = factories.ServiceFactory(readonly_state=Service.MOVING, readonly_start_time=timezone.now())
    move_request = factories.ServiceMoveRequestFactory(service=service)
    move_request._approve_transition(Person(superuser))
    move_request.save()

    tasks.finish_moving(service.id)

    assert move_service.apply_async.call_args_list == [call(args=[move_request.id])]


@patch('plan.services.tasks.move_service')
def test_finish_moving_idm(move_service, superuser):
    service = factories.ServiceFactory(readonly_state=Service.MOVING, readonly_start_time=timezone.now())
    move_request = factories.ServiceMoveRequestFactory(service=service)
    move_request._approve_transition(Person(superuser))
    move_request.process_oebs()
    move_request.process_d()
    move_request.process_idm()
    move_request.save()

    tasks.finish_moving(service.id)

    assert move_service.apply_async.call_args_list == [call(args=[move_request.id])]


@patch('plan.services.tasks.move_service_abc_side')
@pytest.mark.parametrize('async_mode', [True, False])
def test_finish_moving_abc(move_service_abc_side, superuser, async_mode):
    service = factories.ServiceFactory(readonly_state=Service.MOVING, readonly_start_time=timezone.now())
    move_request = factories.ServiceMoveRequestFactory(service=service)
    move_request._approve_transition(Person(superuser))
    move_request.process_oebs()
    move_request.process_d()
    move_request.process_idm()
    move_request.process_abc()
    move_request.save()

    with override_switch('async_move_service_abc_side', active=async_mode):
        tasks.finish_moving(service.id)

    if async_mode:
        assert move_service_abc_side.apply_async.call_args_list == [call(args=[move_request.id])]
    else:
        assert move_service_abc_side.apply_async.call_count == 0


def test_finish_moving_stuck():
    service = factories.ServiceFactory(readonly_state=Service.MOVING, readonly_start_time=timezone.now())
    factories.ServiceMoveRequestFactory(service=service, state=ServiceMoveRequest.COMPLETED)

    tasks.finish_moving(service.id)

    service.refresh_from_db()
    assert service.readonly_state is None


@patch('plan.services.tasks.delete_service_abc_side')
@patch('plan.services.tasks.delete_service')
@patch('plan.idm.nodes.get_service_node')
def test_finish_deleting_idm(get_service_node, delete_service, delete_service_abc_side):
    service = factories.ServiceFactory(readonly_state='deleting', readonly_start_time=timezone.now())
    factories.ServiceDeleteRequestFactory(service=service)
    get_service_node.return_value = node_mock = Mock()
    node_mock.exists.return_value = True

    tasks.finish_deleting(service.id)

    assert node_mock.exists.called
    assert delete_service.delay.called
    assert not delete_service_abc_side.delay.called


@patch('plan.services.tasks.delete_service_abc_side')
@patch('plan.services.tasks.delete_service')
@patch('plan.idm.nodes.get_service_node')
def test_finish_deleting_abc(get_service_node, delete_service, delete_service_abc_side):
    service = factories.ServiceFactory(readonly_state='deleting', readonly_start_time=timezone.now())
    factories.ServiceDeleteRequestFactory(service=service)
    get_service_node.return_value = node_mock = Mock()
    node_mock.exists.return_value = False

    tasks.finish_deleting(service.id)

    assert node_mock.exists.called
    assert not delete_service.delay.called
    assert delete_service_abc_side.delay.called


@pytest.mark.postgresql
def test_check_double_roles():
    member = factories.ServiceMemberFactory()
    member2 = factories.ServiceMemberFactory(
        service=member.service,
        staff=member.staff,
        role=member.role,
    )
    member3 = factories.ServiceMemberFactory()

    tasks.check_double_roles()

    email = mail.outbox[0]
    assert f'{member.staff.login}@{member.service.slug}' in email.body
    assert f'Member {member.id}' in email.body
    assert f'Member {member2.id}' in email.body
    assert f'Member {member3.id}' not in email.body


def test_remind_meta_other_heads(owner_role):

    meta_other = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    # этому ничего не должно прийти
    factories.ServiceFactory(parent=meta_other, is_exportable=False)
    # этому придет напоминалка
    service = factories.ServiceFactory(parent=meta_other, is_exportable=True, name='Test Service')
    factories.ServiceMemberFactory(service=service, role=owner_role)

    tasks.remind_meta_other_heads()

    assert len(mail.outbox) == 1

    email = mail.outbox[0]
    assert 'ABC скоро уберет твои сервисы из API!' in email.subject

    assert service.name in email.body
    assert urljoin(settings.ABC_URL, 'services/{}'.format(service.slug)) in email.body
