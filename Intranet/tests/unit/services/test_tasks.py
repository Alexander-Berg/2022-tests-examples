import json
import random
from unittest.mock import Mock, patch, DEFAULT, MagicMock

import pretend
import pytest
from waffle.testutils import override_switch

from django.conf import settings
from django.core import mail
from django.core.management import call_command
from django.utils import timezone

from plan.oebs.constants import OEBS_DEVIATIONS_REASONS
from plan.api.exceptions import IntegrationError
from plan.common.person import Person
from plan.denormalization.tasks import check_model_denormalized_fields
from plan.idm.exceptions import IDMError
from plan.services import tasks, models
from plan.services.state import SERVICE_STATE
from plan.unistat.tasks import service_level_broken
from plan.duty.models import Schedule

from common import factories
from utils import MockIdmResponse, Response

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(metaservices):
    metaservice, meta_other = metaservices
    submeta_other = factories.ServiceFactory(parent=meta_other)
    service = factories.ServiceFactory(parent=meta_other)

    # пользователи с которыми изначально "все хорошо"
    person = factories.StaffFactory()
    factories.ServiceMemberFactory(
        staff=person,
        service=service,
    )

    superuser = factories.UserFactory(is_superuser=True)
    moderator = factories.StaffFactory(user=superuser)
    check_model_denormalized_fields('services.Service')
    for obj in (service, metaservice, meta_other, submeta_other):
        obj.refresh_from_db()

    fixture = pretend.stub(
        service=service,
        metaservice=metaservice,
        meta_other=meta_other,
        submeta_other=submeta_other,
        moderator=moderator,
    )
    return fixture


@pytest.fixture
def yql_query_oebs_mock(data):
    class FakeYQLRequest:
        def __init__(self):
            self.status = 'COMPLETED'
            self.run = MagicMock()
            table_mock = MagicMock()
            table_mock.rows = (
                (
                    data.service.id, 30, 'LEAF_CODE',
                    'GROUP_CODE', 'some', 'some_en',
                    'true', 'false', 'false', 'true'
                ),
            )
            self.get_results = MagicMock(return_value=[table_mock])

    with patch('yql.api.v1.client.YqlClient.query', return_value=FakeYQLRequest()) as _mock:
        yield _mock


@patch('plan.services.tasks.request_service_head')
@patch('plan.api.idm.actions.add_service')
def test_register_service(add_service, request_service_head, data):
    request = factories.ServiceCreateRequestFactory(
        service=data.service,
        state=models.ServiceCreateRequest.REQUESTED
    )

    tasks.register_service(request.id)

    assert add_service.called
    assert request_service_head.apply_async.called

    request.refresh_from_db()
    assert request.state == models.ServiceCreateRequest.PROCESSING_HEAD


@patch('plan.services.tasks.finalize_service_creation')
def test_register_service_no_service_owner(finalize_service_creation, data):
    """
    Если после создания сервиса owner исчез (например, запросы подвисли, а на следующий день owner уволился)
    из-за этого не должен останавливаться процесс создания сервиса.
    """

    request = factories.ServiceCreateRequestFactory(
        service=data.service,
        state=models.ServiceCreateRequest.PROCESSING_HEAD
    )

    data.service.owner = None
    data.service.save()

    tasks.request_service_head(request.id)

    request.refresh_from_db()
    assert request.state == models.ServiceCreateRequest.PROCESSING_ABC
    assert finalize_service_creation.apply_async.called


@patch('plan.api.idm.actions.add_service')
def test_register_service_saves_request_state_on_exception(add_service, data):
    request = factories.ServiceCreateRequestFactory(
        service=data.service,
        state=models.ServiceCreateRequest.REQUESTED
    )

    add_service.side_effect = Exception

    with pytest.raises(Exception):
        tasks.register_service(request.id)

    assert add_service.called

    request.refresh_from_db()
    assert request.state == models.ServiceCreateRequest.PROCESSING_IDM


@patch('plan.idm.nodes.get_role_node')
@patch('plan.idm.nodes.get_service_roles_node')
@patch('plan.idm.nodes.get_service_node')
@patch('plan.api.idm.actions.add_service')
def test_register_service_continues_on_integration_error(
        add_service, get_service_node, get_service_roles_node, get_role_node, owner_role, data):
    request = factories.ServiceCreateRequestFactory(
        service=data.service,
        state=models.ServiceCreateRequest.REQUESTED
    )

    add_service.side_effect = IntegrationError

    service_node = Mock()
    service_node.exists.return_value = True
    get_service_node.return_value = service_node

    roles_node = Mock()
    roles_node.exists.return_value = False
    get_service_roles_node.return_value = roles_node

    owner_role_node = Mock()
    owner_role_node.exists.return_value = False
    get_role_node.return_value = owner_role_node

    tasks.register_service(request.id)
    assert add_service.called
    assert roles_node.register.called
    assert owner_role_node.register.called

    request.refresh_from_db()
    assert request.state == models.ServiceCreateRequest.PROCESSING_HEAD


@patch('plan.api.idm.actions.add_service_head')
def test_request_service_head_on_service_creation(add_service_head, data):
    request = factories.ServiceCreateRequestFactory(
        service=data.service,
        state=models.ServiceCreateRequest.PROCESSING_HEAD
    )

    tasks.request_service_head(request.id)

    assert add_service_head.called


def test_request_move_on_service_creation(data):
    request = factories.ServiceCreateRequestFactory(
        service=data.service,
        move_to=data.submeta_other,
        state=models.ServiceCreateRequest.PROCESSING_ABC
    )

    tasks.finalize_service_creation(request.id)

    request.refresh_from_db()
    assert request.state == models.ServiceCreateRequest.COMPLETED

    move_request = data.service.move_requests.active().get()
    assert move_request.destination == data.submeta_other


@pytest.mark.parametrize('service_state', SERVICE_STATE.ALL_STATES)
@patch('plan.services.tasks.notify_staff')
def test_update_services_members(notify_staff, data, service_state):
    service = data.service
    service.state = service_state
    service.save(update_fields=('state',))
    assert data.service.members.filter(from_department__isnull=False).count() == 0

    tasks.update_service_department_members(data.service.id)

    assert not notify_staff.delay.called

    department = factories.DepartmentFactory()
    person = factories.StaffFactory(department=department)
    service_department = factories.ServiceMemberDepartmentFactory(
        service=data.service,
        department=department,
        service_id=data.service.id,
    )
    assert service_department.members_count == 0

    tasks.update_service_department_members(data.service.id)

    service_department.refresh_from_db()

    if service_state == SERVICE_STATE.DELETED:
        assert service_department.members_count == 0

        assert data.service.members.filter(from_department__isnull=False).count() == 0
        assert not data.service.members.filter(from_department__isnull=False).exists()

        assert not notify_staff.delay.called
    else:
        assert service_department.members_count == 1

        assert data.service.members.filter(from_department__isnull=False).count() == 1
        assert data.service.members.filter(from_department__isnull=False).get().staff == person

        assert notify_staff.delay.called


def test_update_services_members_call_count(data):
    service = data.service

    for _ in range(2):
        department = factories.DepartmentFactory()
        factories.StaffFactory(department=department)
        factories.ServiceMemberDepartmentFactory(service=service, department=department)

    with patch('plan.services.tasks.notify_staff') as notify_staff:
        tasks.update_service_department_members(service.id)
    assert notify_staff.delay.call_count == 1


def test_move_service(data, move_services_subtasks, mailoutbox, owner_role):
    name = 'Новые щенята & ^ # кусь 2 <script>console.log(1)</script>'
    typograf_name = 'Новые щенята &amp; ^ # кусь 2 &lt;script&gt;console.log(1)&lt;/script&gt;'
    head_membership = factories.ServiceMemberFactory(service=data.service, role=owner_role)
    move_request = factories.ServiceMoveRequestFactory(service=data.service, destination=data.metaservice)
    move_request._approve_transition(Person(data.moderator))
    move_request.save()

    data.service.name = name
    data.service.save()

    tasks.move_service(move_request.id)

    data.service.refresh_from_db()
    move_request.refresh_from_db()
    assert data.service.parent == data.metaservice
    assert move_request.state == 'completed'

    assert move_services_subtasks.idm_call.called
    assert move_services_subtasks.rerequest_roles.apply_async.called
    assert move_services_subtasks.notify_staff.apply_async.called

    assert len(mailoutbox) == 1

    expected_service = (
        'Ваш сервис <a href="{abc_url}/services/{service.slug}/">{name}</a>'
        .format(service=data.service, abc_url=settings.ABC_URL, name=typograf_name)
    )
    expected_parent = (
        'Новый родительский сервис: <a href="{abc_url}/services/{destination.slug}/">{destination.name}</a></p>'
        .format(abc_url=settings.ABC_URL, destination=data.metaservice)
    )

    assert expected_service in mailoutbox[0].body
    assert expected_parent in mailoutbox[0].body
    assert 'был успешно перенесен' in mailoutbox[0].body
    assert name in mailoutbox[0].subject
    assert mailoutbox[0].to == [head_membership.staff.email]


def test_moving_service_clears_readonly(data, move_services_subtasks):
    MOVING = 'moving'

    parent = factories.ServiceFactory(
        parent=data.metaservice,
        readonly_state=MOVING,
        readonly_start_time=timezone.now()
    )
    child = factories.ServiceFactory(
        parent=parent,
        readonly_state=MOVING,
        readonly_start_time=timezone.now()
    )
    grandchild = factories.ServiceFactory(
        parent=child,
        readonly_state=MOVING,
        readonly_start_time=timezone.now()
    )
    check_model_denormalized_fields('services.Service')

    move_request = factories.ServiceMoveRequestFactory(service=parent, state='approved')

    tasks.move_service(move_request.id)

    for service in (parent, child, grandchild):
        service.refresh_from_db()
        assert service.readonly_state is None


def test_moving_flips_is_exportable(data, move_services_subtasks):
    move_request = factories.ServiceMoveRequestFactory(
        service=data.service, destination=data.metaservice, state='approved')
    tasks.move_service(move_request.id)
    data.service.refresh_from_db()
    assert data.service.is_exportable

    move_request = factories.ServiceMoveRequestFactory(
        service=data.service, destination=data.meta_other, state='approved')
    tasks.move_service(move_request.id)
    data.service.refresh_from_db()
    assert not data.service.is_exportable

    move_request = factories.ServiceMoveRequestFactory(
        service=data.service, destination=data.submeta_other, state='approved')
    tasks.move_service(move_request.id)
    data.service.refresh_from_db()
    assert not data.service.is_exportable


def test_moving_flips_is_exportable_on_entire_family(data, move_services_subtasks):
    parent = factories.ServiceFactory(parent=data.meta_other)
    child = factories.ServiceFactory(parent=parent)
    grandchild = factories.ServiceFactory(parent=child)
    check_model_denormalized_fields('services.Service')

    family = (parent, child, grandchild)

    # there
    move_request = factories.ServiceMoveRequestFactory(
        service=parent, destination=data.metaservice, state='approved')
    tasks.move_service(move_request.id)

    for service in family:
        service.refresh_from_db()
        assert service.is_exportable is True

    # and back again
    move_request = factories.ServiceMoveRequestFactory(
        service=parent, destination=data.meta_other, state='approved')
    tasks.move_service(move_request.id)

    for service in family:
        service.refresh_from_db()
        assert service.is_exportable is False


@patch('plan.services.tasks.move_service_abc_side')
@patch('plan.api.idm.actions.move_service')
@patch('plan.api.idm.actions.assert_service_node_exists', Mock())
def test_move_service_retry(move_service, move_service_abc_side, data):
    move_request = factories.ServiceMoveRequestFactory(service=data.service, destination=data.metaservice)
    move_request._approve_transition(Person(data.moderator))
    move_request.save()

    move_service.side_effect = (IDMError, IDMError, IDMError, IDMError, IDMError, DEFAULT)

    tasks.move_service.delay(move_request.id)

    assert move_service.called
    assert move_service_abc_side.apply_async.called


@patch('plan.services.tasks.move_service_abc_side')
@patch('plan.api.idm.actions.move_service')
def test_move_service_retry_fail(move_service, move_service_abc_side, data):
    move_request = factories.ServiceMoveRequestFactory(service=data.service, destination=data.metaservice)
    move_request._approve_transition(Person(data.moderator))
    move_request.save()

    move_service.side_effect = (IDMError, IDMError, IDMError, IDMError, IDMError, IDMError, DEFAULT)

    tasks.move_service.delay(move_request.id)

    assert move_service.called
    assert not move_service_abc_side.delay.called


@patch('plan.services.tasks.move_service_abc_side')
@patch('plan.api.idm.actions.assert_service_node_exists')
@pytest.mark.parametrize("move_service_abc_side_called, idm_exceptions", [
    (True, (IDMError, IDMError, IDMError, IDMError, IDMError, DEFAULT)),
    (False, (IDMError, IDMError, IDMError, IDMError, IDMError, IDMError, DEFAULT)),
])
def test_verify_and_move_service_retry(assert_service_node_exists, move_service_abc_side, data,
                                       move_service_abc_side_called, idm_exceptions):
    move_request = factories.ServiceMoveRequestFactory(service=data.service, destination=data.metaservice)
    move_request._approve_transition(Person(data.moderator))
    move_request.process_oebs()
    move_request.process_d()
    move_request.process_idm()
    move_request.save()

    assert_service_node_exists.side_effect = idm_exceptions

    tasks.verify_and_move_service.delay(move_request.id)
    assert assert_service_node_exists.call_count == settings.ABC_IDM_MAX_RETRY + 1
    assert move_service_abc_side.apply_async.called is move_service_abc_side_called


@patch('plan.api.idm.actions.delete_service')
def test_delete_service(delete_service, service_with_owner):
    service = service_with_owner
    service2 = factories.ServiceFactory()

    move_request = factories.ServiceMoveRequestFactory(
        service=service,
        destination=service2,
        state=models.ServiceMoveRequest.REQUESTED,
    )

    models.ServiceDeleteRequest.request(service, Person(service.owner))

    assert delete_service.call_count == 0

    service.refresh_from_db()
    assert service.state == SERVICE_STATE.IN_DEVELOP

    move_request.refresh_from_db()
    assert move_request.state == models.ServiceMoveRequest.REQUESTED


@patch('plan.api.idm.actions.delete_service')
def test_delete_service_with_schedule(delete_service, service_with_owner):
    service = service_with_owner
    schedule = factories.ScheduleFactory(service=service)
    with patch('plan.services.tasks.drop_requests'):
        with patch('plan.services.models.get_unclosable_services') as get_unclosable_services:
            get_unclosable_services.return_value = []
            models.ServiceDeleteRequest.request(service, Person(service.owner))

    assert delete_service.call_count == 1

    service.refresh_from_db()
    assert service.state == SERVICE_STATE.DELETED

    schedule.refresh_from_db()

    assert schedule.status == Schedule.DELETED


def test_delete_service_with_moving_descendants(service_with_owner):
    service = service_with_owner
    with patch('plan.services.models.Service.has_moving_descendants', return_value=True) as mock:
        models.ServiceDeleteRequest.request(service, Person(service.owner))
    assert mock.call_count == 6  # 1 + 5 (max_retries)
    service.refresh_from_db()
    assert service.state == SERVICE_STATE.IN_DEVELOP


@patch('plan.common.utils.startrek.get_lsr_amount')
@patch('plan.common.utils.startrek.get_filter_amount')
def test_update_kpi(get_filter_amount, get_lsr_amount, service):
    get_filter_amount.return_value = 1
    get_lsr_amount.return_value = 2

    ct_bugs = factories.ContactTypeFactory(code=settings.STARTREK_BUGS_CONTACT)
    factories.ServiceContactFactory(
        service=service,
        type=ct_bugs,
    )

    tasks.update_services_kpi()

    service.refresh_from_db()
    assert service.kpi_bugs_count == 1
    assert service.kpi_release_count is None
    assert service.kpi_lsr_count == 2


@patch('plan.common.utils.startrek.get_lsr_amount')
@patch('plan.common.utils.startrek.get_filter_amount')
def test_update_kpi_double(get_filter_amount, get_lsr_amount, service):
    get_filter_amount.return_value = 3
    get_lsr_amount.return_value = 2

    ct_bugs = factories.ContactTypeFactory(code=settings.STARTREK_BUGS_CONTACT)
    factories.ServiceContactFactory(service=service, type=ct_bugs)
    factories.ServiceContactFactory(service=service, type=ct_bugs)

    tasks.update_services_kpi()

    service.refresh_from_db()
    assert service.kpi_bugs_count == 6


@patch('plan.common.utils.startrek.get_lsr_amount')
def test_skip_kpi(get_lsr_amount, service):
    get_lsr_amount.return_value = None

    def bad_save(self, *args, **kwargs):
        raise ValueError()

    original_save = models.Service.save
    models.Service.save = bad_save

    tasks.update_services_kpi()

    models.Service.save = original_save


def test_service_ancestors():
    metaservice = factories.ServiceFactory()
    parent = factories.ServiceFactory(parent=metaservice)
    service = factories.ServiceFactory(parent=parent)
    factories.ServiceFactory(parent=parent)

    tasks.update_service_fields(parent.id)

    metaservice.refresh_from_db()
    assert metaservice.children_count == 1
    assert metaservice.descendants_count == 3

    parent.refresh_from_db()
    assert parent.children_count == 2
    assert parent.descendants_count == 2
    assert [rec['id'] for rec in parent.ancestors] == [metaservice.id]

    service.refresh_from_db()
    assert [rec['id'] for rec in service.ancestors] == [metaservice.id, parent.id]


def test_service_ancestors_tag_serialization():
    metaservice = factories.ServiceFactory()
    parent = factories.ServiceFactory(parent=metaservice)
    service = factories.ServiceFactory(parent=parent)

    meta_tag = factories.ServiceTagFactory()
    metaservice.tags.add(meta_tag)

    parent_tag = factories.ServiceTagFactory()
    parent.tags.add(parent_tag)

    tasks.update_service_fields(parent.id)

    service.refresh_from_db()

    ancestors = {ancestor['id']: ancestor for ancestor in service.ancestors}

    for ancestor in (metaservice, parent):
        assert len(ancestors[ancestor.id]['tags']) == 1

        tag = ancestors[ancestor.id]['tags'][0]
        assert set(tag.keys()) == {'id', 'name', 'color', 'slug', 'description'}


def test_notification_move_request():
    person = factories.StaffFactory()
    person2 = factories.StaffFactory()
    service = factories.ServiceFactory()
    service2 = factories.ServiceFactory(owner=person2)

    request = factories.ServiceMoveRequestFactory(
        requester=person,
        service=service,
        destination=service2,
        approver_outgoing=person2,
    )

    tasks.notify_move_request(request.id)

    assert len(mail.outbox) == 1

    email = mail.outbox[0]
    assert email.subject == 'Запрос на перенос сервиса %s' % service.name
    assert 'руководителем которого вы являетесь' in email.body
    assert person.first_name in email.body


@patch('plan.api.idm.actions.move_service')
@patch('plan.api.idm.actions.assert_service_node_exists')
@patch('plan.api.idm.actions.rerequest_requested', Mock())
def test_move_to_root(assert_service_node_exists, move_service,
                      mailoutbox, data, owner_role):
    name = 'Новые щенята & ^ # кусь 2 <script>console.log(1)</script>'
    typograf_name = 'Новые щенята &amp; ^ # кусь 2 &lt;script&gt;console.log(1)&lt;/script&gt;'
    data.service.name = name
    data.service.save()
    tasks.move_to_root(data.service, Person(data.moderator))

    data.service.refresh_from_db()
    assert data.service.parent is None

    move_service.assert_called_once_with(data.service, None)
    assert_service_node_exists.assert_called_once_with(data.service, None)
    assert len(mailoutbox) == 1

    expected_service = (
        'Дочерний сервис <a href="{abc_url}/services/{service.slug}/">{name}</a> вашего сервиса '
        '<a href="{abc_url}/services/{parent_service.slug}/">{parent_service.name}</a>'

    ).format(service=data.service, abc_url=settings.ABC_URL, name=typograf_name, parent_service=data.meta_other)

    assert expected_service in mailoutbox[0].body
    assert name in mailoutbox[0].subject
    assert 'был успешно перенесен' in mailoutbox[0].body
    assert '<p>Сервис стал корневым</p>' in mailoutbox[0].body
    assert mailoutbox[0].to == [data.meta_other.owner.email]


@patch('plan.services.tasks.drop_requests')
@patch('plan.services.tasks.notify_staff')
def test_close_service(notify_staff, drop_requests, data):
    tasks.close_service_admin(data.service.id)
    notify_staff.apply_async.assert_called_once_with(
        args=[data.service.id],
        countdown=settings.ABC_DEFAULT_COUNTDOWN
    )
    drop_requests.apply_async.assert_called_once_with(
        args=[data.service.id],
        countdown=settings.ABC_DEFAULT_COUNTDOWN
    )


def test_update_idm_roles_count(patch_tvm):
    service = factories.ServiceFactory()
    mock_response = MockIdmResponse({
        'meta': {
            'total_count': 2,
            'next': None
        },
        'objects': ['some test objects'],
    })

    def mocked_session_send(*args, **kwargs):
        return Response(200, mock_response.response)

    with patch('requests.Session.send') as patched:
        patched.side_effect = mocked_session_send
        tasks.update_service_idm_roles_count(service.id)

    service.refresh_from_db()
    assert service.idm_roles_count == 2


def test_update_puncher_rules_count_from_api():
    service = factories.ServiceFactory()

    def mocked_session_send(*args, **kwargs):
        response = {
            'count': 5,
            'rules': []
        }

        return Response(200, json.dumps(response))

    with patch('requests.Session.send') as patched:
        patched.side_effect = mocked_session_send
        tasks.update_service_puncher_rules_count_from_api([service.id])

    service.refresh_from_db()
    assert service.puncher_rules_count == 5


def test_sync_owners(owner_role):
    user = factories.StaffFactory()
    another_user = factories.StaffFactory()

    service_with_no_owner = factories.ServiceFactory(slug='no_owner')
    factories.ServiceMemberFactory(
        service=service_with_no_owner,
        role=owner_role,
        staff=user,
    )
    service_with_no_owner.owner = None
    service_with_no_owner.save()

    service_with_two_heads = factories.ServiceFactory(slug='two_heads')
    to_delete = factories.ServiceMemberFactory(service=service_with_two_heads, role=owner_role, staff=user)
    factories.ServiceMemberFactory(service=service_with_two_heads, role=owner_role, staff=service_with_two_heads.owner)
    assert models.ServiceMember.objects.filter(service=service_with_two_heads, role=owner_role).count() == 2

    service_with_different_head_and_owner = factories.ServiceFactory(slug='different_head_and_owner')
    factories.ServiceMemberFactory(service=service_with_different_head_and_owner, role=owner_role, staff=user)
    service_with_different_head_and_owner.owner = another_user
    service_with_different_head_and_owner.save()

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        tasks.sync_owners()

        deprive_role.assert_called_once_with(
            to_delete, comment='Неактуальный руководитель'
        )

    service_with_no_owner.refresh_from_db()
    service_with_no_owner.fetch_owner()
    assert service_with_no_owner.owner is not None

    service_with_different_head_and_owner.refresh_from_db()
    service_with_different_head_and_owner.fetch_owner()
    head_sm = models.ServiceMember.objects.get(service=service_with_different_head_and_owner, role=owner_role)
    head = head_sm.staff
    assert service_with_different_head_and_owner.owner == head


def test_sync_owners_two_heads(owner_role):
    """
    Роль старого руководителя по какой-то причине создана позже, чем нового.
    Остаться должна роль, указанная в service.owner
    """

    user = factories.StaffFactory()
    service_with_two_heads = factories.ServiceFactory(slug='two_heads')
    factories.ServiceMemberFactory(
        service=service_with_two_heads,
        role=owner_role,
        staff=service_with_two_heads.owner
    )
    to_delete = factories.ServiceMemberFactory(service=service_with_two_heads, role=owner_role, staff=user)

    assert models.ServiceMember.objects.filter(service=service_with_two_heads, role=owner_role).count() == 2

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        tasks.sync_owners()

        deprive_role.assert_called_once_with(
            to_delete, comment='Неактуальный руководитель'
        )


def test_notify_services_owners(mailoutbox):
    top_owner = factories.StaffFactory()
    child_owner = factories.StaffFactory()
    top_service = factories.ServiceFactory(owner=top_owner)
    factories.ServiceFactory(parent=top_service, owner=child_owner)
    factories.ServiceFactory(owner=top_owner)

    tasks.notify_services_owners()
    assert len(mailoutbox) == 2


def test_cleanup_service_requests():
    source = factories.ServiceFactory()
    move_request_a = factories.ServiceMoveRequestFactory(
        service=source,
        updated_at=timezone.now(),
        state=models.ServiceMoveRequest.REQUESTED,
    )
    move_request_b = factories.ServiceMoveRequestFactory(
        service=source,
        updated_at=timezone.now() + timezone.timedelta(seconds=1),
        state=models.ServiceMoveRequest.REQUESTED,
    )
    other_request = factories.ServiceMoveRequestFactory(
        state=models.ServiceMoveRequest.REQUESTED,
    )
    tasks.cleanup_service_requests()

    for request in [move_request_a, move_request_b, other_request]:
        request.refresh_from_db()

    assert move_request_a.state == models.ServiceMoveRequest.REJECTED
    assert move_request_b.state == models.ServiceMoveRequest.REQUESTED
    assert other_request.state == models.ServiceMoveRequest.REQUESTED


def test_find_memberships_in_staff():
    factories.ServiceFactory(slug='meta_other')
    service = factories.ServiceFactory()
    member1 = factories.ServiceMemberFactory(service=service)
    member2 = factories.ServiceMemberFactory(service=service)
    member3 = factories.ServiceMemberFactory(service=service)

    with patch('plan.staff.tasks.MembershipStaffImporter.get_objects') as get_objects:
        get_objects.return_value = [
            {
                'id': member1.id,
                'person': {'login': member1.staff.login},
                'group': {'url': f'svc_{member1.service.slug}'},
            },
            {
                'id': member1.id,
                'person': {'login': member1.staff.login},
                'group': {'url': f'svc_{member1.service.slug}_{member1.role.scope.slug}'},
            },
            {
                'id': member3.id,
                'person': {'login': member3.staff.login},
                'group': {'url': f'svc_{member3.service.slug}'},
            },
            {
                'id': member3.id,
                'person': {'login': member3.staff.login},
                'group': {'url': f'role_svc_{member3.service.slug}_{member3.role.scope.slug}'},
            },
        ]
        tasks.find_memberships_in_staff()

    for member, found in [
        (member1, True),
        (member2, False),
        (member3, True),
    ]:
        member.refresh_from_db()
        assert (member.found_in_staff_at is not None) == found


@pytest.mark.parametrize('ok', [True, False])
def test_find_interactive_services(robot, ok, patch_tvm, responsible_role):
    service = factories.ServiceFactory()
    moves = []
    for i in range(10):
        moves.append(factories.ServiceMoveRequestFactory(service=service, completed_at=timezone.now()))
    with patch('plan.idm.manager.Manager._run_request') as run_request:
        response = Mock()
        response.ok = ok
        response.text = 'x'
        response.json.return_value = {}
        run_request.return_value = response
        tasks.find_interactive_services()

    for i in range(10):
        moves[i].refresh_from_db()
        if ok:
            assert moves[i].interactive_at is not None
        else:
            assert moves[i].interactive_at is None


def test_finish_all_service_move_requests():
    service_a = factories.ServiceFactory(parent=None, slug='A')
    service_b = factories.ServiceFactory(parent=None, slug='B')
    service_c = factories.ServiceFactory(parent=None, slug='C')
    requests = [
        factories.ServiceMoveRequestFactory(
            service=source,
            destination=destination,
            state=models.ServiceMoveRequest.PROCESSING_ABC,
            # чтобы письмо не собиралось
            from_creation=True,
        )
        for source, destination in [(service_b, service_a), (service_c, service_b)]
    ]
    tasks.finish_all_service_move_requests()
    for request in requests:
        request.refresh_from_db()
        assert request.state == models.ServiceMoveRequest.COMPLETED
    service_b.refresh_from_db()
    assert service_b.parent == service_a
    service_c.refresh_from_db()
    assert service_c.parent == service_b


@pytest.mark.parametrize('async_mode', [False])
def test_move_service_abc_side(async_mode):
    """Меняется родитель перемещаемого сервиса и обновляются уровни всех его потомков."""
    service_a = factories.ServiceFactory(parent=None, slug='A')
    service_b = factories.ServiceFactory(parent=None, slug='B')
    factories.ServiceFactory(parent=service_b, slug='C')
    request = factories.ServiceMoveRequestFactory(
        service=service_b,
        destination=service_a,
        state=models.ServiceMoveRequest.PROCESSING_ABC,
        # чтобы письмо не собиралось
        from_creation=True,
    )
    with override_switch('async_move_service_abc_side', active=async_mode):
        tasks.move_service_abc_side(request.id)
    request.refresh_from_db()
    assert request.state == models.ServiceMoveRequest.COMPLETED
    service_b.refresh_from_db()
    assert service_b.parent == service_a
    with patch('plan.unistat.tasks.fix_service_closuretree_levels') as fix_service_closuretree_levels_mock:
        assert service_level_broken() == 0
    fix_service_closuretree_levels_mock.apply_async.assert_not_called()


def test_move_service_abc_side_to_root(service):
    """Сервис перемещается на верхний уровень, уровни всех его потомков обновляются."""
    factories.ServiceFactory(parent=service, slug='C')
    request = factories.ServiceMoveRequestFactory(
        service=service,
        destination=None,
        state=models.ServiceMoveRequest.PROCESSING_ABC,
        # чтобы письмо не собиралось
        from_creation=True,
    )
    tasks.move_service_abc_side(request.id)
    request.refresh_from_db()
    assert request.state == models.ServiceMoveRequest.COMPLETED
    service.refresh_from_db()
    assert service.parent is None
    with patch('plan.unistat.tasks.fix_service_closuretree_levels') as fix_service_closuretree_levels_mock:
        assert service_level_broken() == 0
    fix_service_closuretree_levels_mock.apply_async.assert_not_called()


def test_deep_disable_membership_inheritance():
    root = factories.ServiceFactory()
    factories.ServiceFactory(parent=root, membership_inheritance=True)
    node = factories.ServiceFactory(parent=root, membership_inheritance=True)
    factories.ServiceFactory(parent=node, membership_inheritance=True)
    factories.ServiceFactory(parent=node, membership_inheritance=False)
    services = (root, *root.get_descendants())

    assert any(s.membership_inheritance is True for s in services)
    call_command('deep_disable_membership_inheritance', service_id=root.id)
    for s in services:
        s.refresh_from_db()
    assert all(s.membership_inheritance is False for s in services)


def test_sync_department_members_state():
    active_state = models.ServiceMember.states.ACTIVE
    deprved_state = models.ServiceMember.states.DEPRIVED
    service = factories.ServiceFactory()
    department_deprived = factories.ServiceMemberDepartmentFactory(
        state=deprved_state,
        service=service,
    )
    member_should_be_deprived = factories.ServiceMemberFactory(
        from_department=department_deprived,
        state=active_state,
        service=service,
    )
    member_out_of_department = factories.ServiceMemberFactory(
        state=active_state,
        service=service,
    )
    department_active = factories.ServiceMemberDepartmentFactory(
        state=active_state,
        service=service,
    )
    member_should_stay_active = factories.ServiceMemberFactory(
        from_department=department_active,
        state=active_state,
        service=service,
    )

    tasks.sync_department_members_state()

    member_out_of_department.refresh_from_db()
    member_should_stay_active.refresh_from_db()

    assert member_should_stay_active.state == active_state
    assert member_out_of_department.state == active_state

    deprived_member = models.ServiceMember.all_states.get(pk=member_should_be_deprived.pk)
    assert deprived_member.state == deprved_state


@pytest.mark.parametrize('tag_slug', settings.REVIEW_REQUIRED_TAG_SLUGS)
@pytest.mark.parametrize('review_required', [True, False, None])
@pytest.mark.parametrize('service_review_required', [True, False])
def test_update_service_review_policy(review_required_tags, review_required, service_review_required, tag_slug):
    service = factories.ServiceFactory()
    tag = models.ServiceTag.objects.get(slug=tag_slug)
    if service_review_required:
        service.tags.add(tag)

    with patch('plan.idm.nodes.Node.update') as node_update_mock:
        tasks.update_service_review_policy(service_slug=service.slug, review_required=review_required)

    node_update_mock.assert_has_calls([
        {'review_required': review_required is None and service_review_required or review_required}
        for _ in service.role_set.all()
    ])


@pytest.mark.parametrize('tag_slug', settings.REVIEW_REQUIRED_TAG_SLUGS)
def test_check_service_review_policy_enabled(review_required_tags, tag_slug):
    fake_manager = object()
    tag = models.ServiceTag.objects.get(slug=tag_slug)

    class FakeFetchDataMethod:
        def __init__(self, results: list = None):
            self._results = results or []
            self.call_count = 0

        def __call__(self, *args, **kwargs):
            result = self._results[self.call_count]
            self.call_count += 1
            return result

    service = factories.ServiceFactory()

    sox_service = factories.ServiceFactory()
    sox_service.tags.add(tag)
    custom_roles = [
        factories.RoleFactory(service=service),
        factories.RoleFactory(service=sox_service),
    ]
    default_roles_count = models.Role.objects.filter(service=None).count()

    fake_fetch_responses = [
        {'review_required': bool(random.randint(0, 1))}
        for _ in range(len([role for role in custom_roles if role.service.review_required]) + default_roles_count)
    ]

    with patch('plan.api.idm.actions.idm_manager', return_value=fake_manager), \
            patch(
                'plan.idm.nodes.Node.fetch_data',
                new=FakeFetchDataMethod(fake_fetch_responses),
            ) as node_fetch_data_mock, \
            patch('plan.idm.nodes.Node.update') as update_node_mock:
        tasks.check_service_review_policy_enabled()

    assert node_fetch_data_mock.call_count == \
           len(fake_fetch_responses)
    update_node_mock.assert_has_calls([
        ((), {'manager': fake_manager, 'review_required': True})
        for fake_response in fake_fetch_responses if not fake_response['review_required']
    ])


@pytest.mark.parametrize('diff_source', ['name', 'name_en', 'use_for_hr', 'leaf_code', 'parent'])
def test_sync_oebs_structure(data, yql_query_oebs_mock, diff_source):
    """
    Проверяем синхронизацию данных с oebs выгрузкой в YT
    У одного сервиса сменили родителя, другой из выгрузки пропал
    Соответственно такие же изменения должны произойти у нас
    """

    oebs_type = factories.ResourceTypeFactory(code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE)
    factories.ServiceResourceFactory(
        service=data.service,
        resource=factories.ResourceFactory(type=oebs_type),
        type_id=oebs_type.id,
        state='granted',
        attributes={
            'parent_oebs_id': 'GROUP_CODE',
            'leaf_oebs_id': 'FAKE_ONE' if diff_source == 'leaf_code' else 'LEAF_CODE',
        },
    )

    data.service.name = 'some'
    data.service.name_en = 'some_en'
    data.service.use_for_group_only = True
    data.service.use_for_hr = False
    data.service.use_for_procurement = False
    data.service.use_for_revenue = True
    data.service.oebs_parent_id = 42

    if diff_source == 'name_en':
        deviation_reason = OEBS_DEVIATIONS_REASONS.NAME
        data.service.name_en = 'hello'
    elif diff_source == 'name':
        deviation_reason = OEBS_DEVIATIONS_REASONS.NAME
        data.service.name = 'hello'
    elif diff_source == 'use_for_hr':
        deviation_reason = OEBS_DEVIATIONS_REASONS.FLAG
        data.service.use_for_hr = True
    elif diff_source == 'leaf_code':
        deviation_reason = OEBS_DEVIATIONS_REASONS.RESOURCE
    else:
        deviation_reason = OEBS_DEVIATIONS_REASONS.PARENT

    data.metaservice.oebs_parent_id = 50
    data.metaservice.oebs_data = {
        'smth': 'else',
        'deviation_reason': 'flag'
    }
    data.service.save()
    data.metaservice.save()

    tasks.sync_oebs_structure()
    yql_query_oebs_mock.assert_called_once()

    data.service.refresh_from_db()
    data.metaservice.refresh_from_db()

    assert data.service.oebs_parent_id == 30
    assert data.service.oebs_data == {
        'leaf_oebs_id': 'LEAF_CODE', 'name': 'some',
        'name_en': 'some_en', 'oebs_parent_id': 30,
        'parent_oebs_id': 'GROUP_CODE',
        'use_for_group_only': True, 'use_for_hr': False,
        'use_for_procurement': False,
        'use_for_revenue': True,
        'deviation_reason': deviation_reason,
    }

    assert data.metaservice.oebs_parent_id is None
    assert data.metaservice.oebs_data == {}
