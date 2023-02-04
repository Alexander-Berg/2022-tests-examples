import pretend
import pytest
import textwrap
from unittest import mock
from django.conf import settings
from django.utils import timezone
from freezegun import freeze_time
from ids.exceptions import IDSException
from mock import patch
from django.template import Template, Context
from ids.exceptions import BackendError
from django.test.utils import override_settings

from plan.oebs.constants import (
    STATES, ERRORS,
    OEBS_MATCHING_FLAGS_AND_ROLES,
    ACTIONS,
)
from plan.oebs.utils import (
    notify_agreement_staff_not_found,
    handle_oebs_error,
    create_oebs_approve_issue,
    get_oebs_approvers,
    is_oebs_related,
    get_move_approvers,
)
from plan.roles.models import Role
from common import factories

from .conftest import StartrekIssue

pytestmark = [pytest.mark.django_db(transaction=True)]


@pytest.fixture()
def move_agreement_issue_description(move_agreement):
    text = '''
    Произошла ошибка при записи изменений в OEBS

    **Сервис:** ((https://{{ host }}/services/{{ service.slug }}/ {{ service.name }}))
    **Автор запроса:** staff:{{ requester.login }}
    **Тикет согласования:** {{ agreement.issue }}
    **Тип изменения:** Перемещение


    <{**Ошибка**
    %%(JSON)
    {&quot;error&quot;: &quot;fail&quot;}
    %%
    }>
    '''
    template = Template(textwrap.dedent(text).lstrip())
    context = Context({
        'host': settings.ABC_HOST,
        'agreement': move_agreement,
        'service': move_agreement.service,
        'requester': move_agreement.requester,
    })
    return template.render(context)


class CommentsMock:
    def create(self, **_kwargs):
        return True


def create_issue_mock(queue, **kwargs):
    return pretend.stub(queue=queue, key=f'{queue}-1', comments=CommentsMock(), **kwargs)


def create_issue_failing_mock(*_args, **_kwargs):
    raise IDSException


def assert_approvers_list(move_agreement, approvers_list):
    oebs_approvers = get_oebs_approvers(move_agreement)

    assert len(oebs_approvers) == len(approvers_list)
    for approvers in oebs_approvers:
        assert approvers in approvers_list


@pytest.fixture
def agreement():
    service = factories.ServiceFactory()
    requester = factories.StaffFactory()
    return factories.OEBSAgreementFactory(service=service, requester=requester)


@freeze_time('2020-01-01')
def test_notify_agreement_staff_not_found(agreement):
    role = factories.RoleFactory()

    error_message = f'В вышестоящих сервисах отсутствует сотрудник с ролью {role.name}'
    error_description = f"""**Сервис:** https://abc.yandex-team.ru/services/{agreement.service.slug}
**Причина ошибки:** {error_message}
"""

    with patch('plan.oebs.utils.create_issue') as create_issue:
        create_issue.side_effect = create_issue_mock
        notify_agreement_staff_not_found(agreement, role)
        create_issue.assert_called_once_with(
            queue=settings.OEBS_AGREEMENT_ERROR_QUEUE,
            summary='Не найден сотрудник для подтверждения изменений в ГК OEBS',
            description=error_description,
            tags=settings.OEBS_AGREEMENT_ERROR_ISSUE_TAGS,
            components=settings.OEBS_AGREEMENT_ERROR_ISSUE_COMPONENTS,
            createdBy=agreement.requester.login,
            assignee=settings.OEBS_AGREEMENT_ERROR_ISSUE_ASSIGNEE
        )

    agreement.refresh_from_db()
    assert agreement.state == STATES.FAILED
    assert agreement.end_date == timezone.now().date()
    assert agreement.error_type == ERRORS.NO_APPROVERS
    assert agreement.error_message == {'message': error_message}
    assert agreement.repair_issue == f'{settings.OEBS_AGREEMENT_ERROR_QUEUE}-1'


def test_notify_agreement_staff_not_found_with_startrek_error(agreement):
    role = factories.RoleFactory()

    with patch('plan.oebs.utils.create_issue') as create_issue:
        create_issue.side_effect = create_issue_failing_mock
        notify_agreement_staff_not_found(agreement, role)

    agreement.refresh_from_db()
    assert agreement.state == STATES.FAILED
    assert agreement.repair_issue is None


def test_is_oebs_related(service, oebs_service, dormant_oebs_service):
    assert is_oebs_related(service, with_descendants=True) is True
    assert is_oebs_related(service, with_descendants=True, ignore_dormant=True) is True
    assert is_oebs_related(service, with_descendants=False) is False

    assert is_oebs_related(dormant_oebs_service, ignore_dormant=True) is False


@pytest.mark.parametrize('has_repair_issue', (True, False))
def test_handle_oebs_error(
    move_agreement,
    move_agreement_issue_description,
    has_repair_issue,
):
    issue_key = 'OEBSSUPPORT-1'
    if has_repair_issue:
        move_agreement.repair_issue = issue_key
        move_agreement.save()

    error_message = {'error': 'fail'}
    with mock.patch('plan.oebs.utils.create_issue') as create_issue:
        create_issue.return_value = StartrekIssue(issue_key)
        handle_oebs_error(move_agreement, error_message)

    move_agreement.refresh_from_db()
    if has_repair_issue:
        create_issue.assert_not_called()
    else:
        create_issue.assert_called_once()
        assert create_issue.call_args[1]['description'] == move_agreement_issue_description
    assert move_agreement.error_type == ERRORS.OEBS_ERROR
    assert move_agreement.state == STATES.FAILED
    assert move_agreement.error_message == error_message
    assert move_agreement.repair_issue == issue_key


@mock.patch('plan.oebs.utils.create_issue', mock.Mock(side_effect=BackendError))
def test_handle_oebs_error_with_startrek_error(move_agreement):
    error_message = {'error': 'fail'}
    handle_oebs_error(move_agreement, error_message)

    move_agreement.refresh_from_db()
    assert move_agreement.error_type == ERRORS.OEBS_ERROR
    assert move_agreement.state == STATES.FAILED
    assert move_agreement.error_message == error_message
    assert move_agreement.repair_issue is None


def test_create_approve_ticket(move_agreement):
    with patch('plan.oebs.utils.change_state') as mock_change_state:
        with patch('plan.oebs.utils.create_issue') as create_issue:
            create_issue.side_effect = create_issue_mock
            create_oebs_approve_issue(move_agreement)
    create_issue.assert_called_once()
    mock_change_state.assert_called_once()
    move_agreement.refresh_from_db()
    assert move_agreement.issue == f'{settings.OEBS_APPROVE_QUEUE}-1'


def test_create_approve_ticket_not_called(move_agreement):
    move_agreement.issue = 'TEST-1'
    with patch('plan.oebs.utils.create_issue') as create_issue:
        create_oebs_approve_issue(move_agreement)
    create_issue.assert_not_called()


@override_settings(
    OEBS_HARDWARE_APPROVERS=['abash'],
    OEBS_HR_APPROVERS=['volozh', 'alimpiev'],
    OEBS_PROCUREMENT_APPROVERS=['smosker']
)
@pytest.mark.parametrize('service_use_for_hardware', [True, False])
@pytest.mark.parametrize('service_use_for_hr', [True, False])
@pytest.mark.parametrize('service_use_for_procurement', [True, False])
@pytest.mark.parametrize('agreement_use_for_hardware', [True, False])
@pytest.mark.parametrize('agreement_use_for_hr', [True, False])
@pytest.mark.parametrize('agreement_use_for_procurement', [True, False])
def test_get_oebs_approvers_no_role(
        move_agreement, oebs_data,
        service_use_for_hardware, service_use_for_hr, service_use_for_procurement,
        agreement_use_for_hardware, agreement_use_for_hr, agreement_use_for_procurement,
):
    """
    Проверяем список апруверов при установке / снятие флагов. Специальных ролей нет.
    Поэтому флаг use_for_revenue тут не меняется, тк его изменение апрувят только обладатели роли.
    """

    approvers = {
        'use_for_hardware': ['abash'],
        'use_for_hr': ['volozh', 'alimpiev'],
        'use_for_procurement': ['smosker'],
        'use_for_revenue': [],
    }

    agreement_flags = {
        'use_for_hardware':  agreement_use_for_hardware,
        'use_for_hr': agreement_use_for_hr,
        'use_for_procurement': agreement_use_for_procurement,
    }
    move_agreement.service = oebs_data.service
    move_agreement.attributes.update(agreement_flags)
    move_agreement.save()

    service_flags = {
        'use_for_hardware': service_use_for_hardware,
        'use_for_hr': service_use_for_hr,
        'use_for_procurement': service_use_for_procurement,
    }
    for flag, value in service_flags.items():
        setattr(move_agreement.service, flag, value)
    move_agreement.service.save()

    approvers_list = []
    if service_use_for_hardware or agreement_use_for_hardware:
        approvers_list.append(approvers['use_for_hardware'])
    if service_use_for_hr or agreement_use_for_hr:
        approvers_list.append(approvers['use_for_hr'])
    if service_use_for_procurement or agreement_use_for_procurement:
        approvers_list.append(approvers['use_for_procurement'])

    move_agreement.service.get_descendants(include_self=False).update(
        use_for_hardware=False,
        use_for_hr=False,
        use_for_revenue=False,
        use_for_procurement=False,
    )  # чтобы не считались флаги детей

    assert_approvers_list(move_agreement, approvers_list)


@override_settings(
    OEBS_HR_APPROVERS=['volozh', 'alimpiev'],
    OEBS_PROCUREMENT_APPROVERS=['smosker'],
)
def test_oebs_approves_descendants(move_agreement, oebs_data):
    """
    Проверяем работу get_oebs_approvers для случая,
    когда у самого сервиса флагов нет, в этом случае флаги ищутся по потомкам
    """
    parent = factories.ServiceFactory()
    child_one = factories.ServiceFactory(parent=parent, use_for_hr=True)
    factories.ServiceFactory(parent=child_one, use_for_revenue=True)
    factories.ServiceFactory(parent=child_one, use_for_hr=True)

    assert_approvers_list(move_agreement, [['volozh', 'alimpiev']])


@override_settings(
    OEBS_HR_APPROVERS=['volozh', 'alimpiev'],
    OEBS_PROCUREMENT_APPROVERS=['smosker'],
)
def test_oebs_approves_flag_not_changed(oebs_data):
    """
    Не добавляем подтверждающего для флага если
    действие - изменение и флаг не меняется
    """
    oebs_data.service.use_for_hr = True
    oebs_data.service.save()
    agreement = factories.OEBSAgreementFactory(
        action=ACTIONS.CHANGE_FLAGS,
        requester=oebs_data.staff,
        service=oebs_data.service,
        attributes={'use_for_procurement': True}
    )

    assert_approvers_list(agreement, [['smosker']])


@override_settings(
    OEBS_HARDWARE_APPROVERS=['abash'],
    OEBS_HR_APPROVERS=['volozh', 'alimpiev'],
    OEBS_PROCUREMENT_APPROVERS=['smosker']
)
@pytest.mark.parametrize('agreement_use_for_hardware', [True, False])
@pytest.mark.parametrize('agreement_use_for_hr', [True, False])
@pytest.mark.parametrize('agreement_use_for_procurement', [True, False])
@pytest.mark.parametrize('spec_role', [
    settings.OEBS_REVENUE_ROLE_CODE, settings.OEBS_HR_ROLE_CODE, settings.OEBS_PROCUREMENT_ROLE_CODE
])
def test_get_oebs_approvers_with_role(
        move_agreement, oebs_data, spec_role,
        agreement_use_for_hardware, agreement_use_for_hr, agreement_use_for_procurement,
):
    """
    Проверяем список аппруверов. Изначально в сервисе все флаги False.
    В сервисе указана специальная роль.
    """

    approvers = {
        'use_for_hardware': ['abash'],
        'use_for_hr': ['volozh', 'alimpiev'],
        'use_for_procurement': ['smosker'],
        'use_for_revenue': [],
    }

    role = Role.objects.get(code=spec_role)
    factories.ServiceMemberFactory(
        role=role,
        service=oebs_data.service.parent,
        staff=oebs_data.staff
    )

    for flag, role_code in OEBS_MATCHING_FLAGS_AND_ROLES.items():
        if role_code == spec_role:
            approvers[flag] = [oebs_data.staff.login]

    agreement_flags = {
        'use_for_hardware': agreement_use_for_hardware,
        'use_for_hr': agreement_use_for_hr,
        'use_for_procurement': agreement_use_for_procurement,
    }
    if spec_role == settings.OEBS_REVENUE_ROLE_CODE:
        agreement_flags['use_for_revenue'] = True

    move_agreement.service = oebs_data.service
    move_agreement.attributes.update(agreement_flags)
    move_agreement.save()

    approvers_list = []
    if agreement_use_for_hardware:
        approvers_list.append(approvers['use_for_hardware'])
    if agreement_use_for_hr:
        approvers_list.append(approvers['use_for_hr'])
    if agreement_use_for_procurement:
        approvers_list.append(approvers['use_for_procurement'])
    if 'use_for_revenue' in agreement_flags:
        approvers_list.append(approvers['use_for_revenue'])

    move_agreement.service.get_descendants(include_self=False).update(
        use_for_hardware=False,
        use_for_hr=False,
        use_for_revenue=False,
        use_for_procurement=False,
    )  # чтобы не считались флаги детей

    assert_approvers_list(move_agreement, approvers_list)


@override_settings(
    OEBS_HR_APPROVERS=['smosker'],
    OEBS_PROCUREMENT_APPROVERS=['smosker']
)
def test_distinct_approvers_groups(oebs_data):
    agreement = factories.OEBSAgreementFactory(
        action=ACTIONS.CHANGE_FLAGS,
        attributes={
            'use_for_hr': True,
            'use_for_procurement': True,
        }
    )
    approvers = get_oebs_approvers(oebs_agreement=agreement)
    assert approvers == [['smosker']]


@pytest.mark.parametrize('tag_slug', (settings.GRADIENT_VS, settings.BUSINESS_UNIT_TAG))
def test_get_oebs_approvers_gradient(move_agreement, tag_slug):
    abc_service = factories.ServiceFactory(slug='abc')
    responsible_role = factories.RoleFactory(code='responsible')
    responsible = factories.ServiceMemberFactory(
        service=abc_service,
        role=responsible_role,
    )
    hr_role = factories.RoleFactory(
        code=settings.OEBS_HR_ROLE_CODE,
        name=settings.OEBS_HR_ROLE_CODE,
    )
    approver = factories.ServiceMemberFactory(
        service=move_agreement.service,
        role=hr_role,
    )
    move_agreement.action = ACTIONS.RENAME
    move_agreement.save()

    factories.ServiceMemberFactory(service=abc_service)
    vs_tag = factories.ServiceTagFactory(slug=tag_slug)
    move_agreement.service.tags.add(vs_tag)
    oebs_approvers = get_oebs_approvers(move_agreement)
    assert oebs_approvers == [[responsible.staff.login], [approver.staff.login]]


@pytest.mark.parametrize('is_base_current', (True, False))
@pytest.mark.parametrize('is_base_target', (True, False))
@pytest.mark.parametrize('is_vs_current', (True, False))
@pytest.mark.parametrize('is_vs_target', (True, False))
@pytest.mark.parametrize('has_vs_role_current_tree', (True, False))
@pytest.mark.parametrize('has_vs_role_target_tree', (True, False))
def test_additional_approver_for_moving_vs(
    move_agreement, is_base_target, is_base_current,
    is_vs_current, is_vs_target,
    has_vs_role_current_tree, has_vs_role_target_tree,
):
    abc_approvers = ['smosker']
    vs_tag = factories.ServiceTagFactory(slug=settings.GRADIENT_VS)
    base_service = factories.ServiceFactory(is_base=is_base_current)
    target_base_service = factories.ServiceFactory(is_base=is_base_target)
    vs_service = factories.ServiceFactory(parent=base_service)
    target_vs_service = factories.ServiceFactory(parent=target_base_service)
    if is_vs_current:
        vs_service.tags.add(vs_tag)
    if is_vs_target:
        target_vs_service.tags.add(vs_tag)

    move_agreement.service.parent = vs_service
    move_agreement.service.save()

    move_agreement.move_request.destination.parent = target_vs_service
    move_agreement.move_request.destination.save()

    vs_role = factories.RoleFactory(
        code=settings.OEBS_VS_ROLE_CODE,
        name=settings.OEBS_VS_ROLE_CODE,
    )
    if has_vs_role_current_tree:
        approver_current = factories.ServiceMemberFactory(
            service=vs_service,
            role=vs_role,
        )
    if has_vs_role_target_tree:
        approver_target = factories.ServiceMemberFactory(
            service=target_vs_service,
            role=vs_role,
        )

    target = []
    if is_vs_current and has_vs_role_current_tree:
        target.append([approver_current.staff.login])
    if is_vs_target and has_vs_role_target_tree:
        target.append([approver_target.staff.login])
    if any(
        value is True for value in (
            is_vs_target, is_vs_current,
            is_base_current, is_base_target,
        )
    ) and not target:
        target = [abc_approvers]

    oebs_approvers = get_move_approvers(move_agreement, abc_approvers)
    assert oebs_approvers == target
