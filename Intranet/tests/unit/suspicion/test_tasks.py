import pytest
import mock

from django.utils import timezone

from django.core import mail
from freezegun import freeze_time
from mock import patch

from plan.services.models import Service
from plan.suspicion.constants import ServiceIssueStates, LEVELS
from plan.suspicion.models import ServiceIssue, ServiceTrafficStatus
from plan.suspicion.tasks import (
    find_issues,
    add_green_traffic,
    send_suspicion_digest,
    check_issue_groups,
)
from plan.suspicion.tasks_helpers import create_new_service_issue_groups, get_max_issue_groups_weights
from common import factories
from utils import list_workdays_some_months

pytestmark = [pytest.mark.django_db]


def get_fake_issue_finder(return_value):
    class IssueFinder(object):
        def __init__(self, *args, **kwargs):
            pass

        def __call__(*args, **kwargs):
            for x in return_value:
                yield x

    return IssueFinder


def test_find_issues(monkeypatch):
    """
    У service_1 нет issue, но будет
    У service_2 есть issue, но с другим context
    У service_3 есть issue, но не будет
    """
    issue = factories.IssueFactory()

    service_1 = factories.ServiceFactory()

    service_2 = factories.ServiceFactory()
    service_2_issue = factories.ServiceIssueFactory(service=service_2, issue=issue, context={'a': 1})

    service_3 = factories.ServiceFactory()
    service_3_issue = factories.ServiceIssueFactory(service=service_3, issue=issue, context={'a': 1})

    new_issues = [
        (service_1.id, {'c': 1}, 'x', 1),
        (service_2.id, {'b': 1}, 'y', 1)
    ]
    monkeypatch.setattr('plan.suspicion.tasks.Issue.get_issue_finder', lambda x: get_fake_issue_finder(new_issues))
    find_issues()

    service_1_issue = ServiceIssue.objects.get(service=service_1)
    assert service_1_issue.context == {'c': 1}
    assert service_1_issue.state == 'active'

    service_2_issue.refresh_from_db()
    assert service_2_issue.context == {'b': 1}
    assert service_2_issue.state == 'active'

    service_3_issue.refresh_from_db()
    assert service_3_issue.state == 'fixed'

    assert ServiceIssue.objects.count() == 3

    appeal = factories.ServiceAppealIssueFactory(service_issue=service_2_issue)
    find_issues()
    service_2_issue.refresh_from_db()
    assert service_2_issue.state == 'review'

    appeal.reject(factories.StaffFactory())
    find_issues()
    service_2_issue.refresh_from_db()
    assert service_2_issue.state == 'active'

    appeal = factories.ServiceAppealIssueFactory(service_issue=service_2_issue)
    appeal.approve(factories.StaffFactory())
    find_issues()
    service_2_issue.refresh_from_db()
    assert service_2_issue.state == 'appealed'


def test_find_issues_after_appeal(monkeypatch):
    """
    У service_1 нет issue, но будет
    У service_2 есть issue, но с другим context
    У service_3 есть issue, но не будет
    """
    factories.IssueFactory()
    service = factories.ServiceFactory()

    new_issues = [
        (service.id, {'c': 1}, 'x', 1),
    ]
    monkeypatch.setattr('plan.suspicion.tasks.Issue.get_issue_finder', lambda x: get_fake_issue_finder(new_issues))
    find_issues()

    assert ServiceIssue.objects.filter(service=service).count() == 1
    assert ServiceIssue.objects.get(service=service).state == ServiceIssue.STATES.ACTIVE
    assert ServiceIssue.objects.get(service=service).issue_action_key == 'x'

    ServiceIssue.objects.filter(service=service).update(state=ServiceIssue.STATES.APPEALED)

    find_issues()

    assert ServiceIssue.objects.filter(service=service).count() == 1
    assert ServiceIssue.objects.get(service=service).state == ServiceIssue.STATES.APPEALED

    new_issues[0] = (service.id, {'c': 2}, 'y', 1)
    find_issues()
    assert ServiceIssue.objects.filter(service=service).count() == 2
    assert ServiceIssue.objects.get(service=service, state=ServiceIssue.STATES.ACTIVE).issue_action_key == 'y'


def test_create_new_service_issue_groups():
    issue = factories.IssueFactory()
    service_issue = factories.ServiceIssueFactory(issue=issue)
    create_new_service_issue_groups()
    service_issue_group = ServiceIssue.objects.get(issue_group__isnull=False)
    assert service_issue_group.service == service_issue.service
    assert ServiceIssue.objects.count() == 2
    create_new_service_issue_groups()
    assert ServiceIssue.objects.count() == 2


def test_get_max_issue_groups_weights():
    issue_group = factories.IssueGroupFactory()
    factories.IssueFactory(weight=2, issue_group=issue_group)
    factories.IssueFactory(weight=3, issue_group=issue_group)

    assert get_max_issue_groups_weights() == {
        issue_group.id: 2 + 3,
    }


def test_add_green_traffic():
    group1 = factories.IssueGroupFactory()
    service1 = factories.ServiceFactory()
    group2 = factories.IssueGroupFactory()
    service2 = factories.ServiceFactory()
    factories.ServiceTrafficStatusFactory(issue_group=group1, service=service1)

    add_green_traffic()

    assert (
        set(ServiceTrafficStatus.objects.values_list('issue_group', 'service')) ==
        {
            (group1.id, service1.id),
            (group1.id, service2.id),
            (group2.id, service1.id),
            (group2.id, service2.id),
        }
    )


def test_find_issues_make_one_update_for_issue(monkeypatch, django_assert_num_queries):
    """
    Проверим, что для одного issue_code и одного значения percentage сделаем один UPDATE, если не изменился context
    """
    issue = factories.IssueFactory()
    services = []

    for _ in range(100):
        service = factories.ServiceFactory()
        services.append(service)
        factories.ServiceIssueFactory(service=service, issue=issue, context={'a': 1})

    new_issues = [(service.id, {'a': 1}, 'x', 1) for service in services]
    monkeypatch.setattr('plan.suspicion.tasks.Issue.get_issue_finder', lambda x: get_fake_issue_finder(new_issues))

    # Делаем 7 запросов: SELECT - 3, UPDATE - 4, SAVEPOINT - 2
    with django_assert_num_queries(13):
        find_issues()


def test_suspicious():
    already_suspicious = factories.ServiceFactory(suspicious_date=timezone.now())
    must_be_ok = factories.ServiceFactory(suspicious_date=timezone.now())
    must_be_suspicious = factories.ServiceFactory()
    already_ok = factories.ServiceFactory()

    for service in [already_suspicious, must_be_ok, must_be_suspicious, already_ok]:
        factories.ServiceTrafficStatusFactory(level=LEVELS.OK, service=service)

    for service in [already_suspicious, must_be_suspicious]:
        factories.ServiceTrafficStatusFactory(level=LEVELS.CRIT, service=service)

    with mock.patch('plan.suspicion.tasks_helpers.IssueGroupsChecker.run'):
        check_issue_groups()

    already_suspicious.refresh_from_db()
    must_be_suspicious.refresh_from_db()
    must_be_ok.refresh_from_db()
    already_ok.refresh_from_db()
    assert already_suspicious.suspicious_date is not None
    assert must_be_suspicious.suspicious_date is not None
    assert must_be_ok.suspicious_date is None
    assert already_ok.suspicious_date is None


@pytest.fixture
def data_for_suspicion_digest(owner_role):
    staff = factories.StaffFactory(is_robot=True)
    service_1 = factories.ServiceFactory(parent=None, owner=staff, description='абв')
    service_2 = factories.ServiceFactory(parent=None, owner=staff, description='гдеёж')
    factories.ServiceMemberFactory(role=owner_role, service=service_1, staff=staff)
    factories.ServiceMemberFactory(role=owner_role, service=service_2, staff=staff)
    group_team = factories.IssueGroupFactory(code='team', name='Команда')
    group_clarity = factories.IssueGroupFactory(code='clarity', name='Понятность')
    factories.IssueFactory(code='owner_is_robot', issue_group=group_team)
    factories.IssueFactory(code='empty_description_ru', issue_group=group_clarity)
    factories.IssueFactory(code='empty_description_en', issue_group=group_clarity)
    factories.IssueGroupThresholdFactory(issue_group=group_team, level=LEVELS.CRIT, threshold=0.5)
    factories.IssueGroupThresholdFactory(issue_group=group_team, level=LEVELS.WARN, threshold=0.3)
    factories.IssueGroupThresholdFactory(issue_group=group_clarity, level=LEVELS.CRIT, threshold=0.7)
    factories.IssueGroupThresholdFactory(issue_group=group_clarity, level=LEVELS.WARN, threshold=0.4)

    return staff, service_1, service_2, group_team, group_clarity


@freeze_time('2019-12-09')
@pytest.mark.parametrize('send_suggest', [True, False])
def test_notifications_suspicion_digest_check_send(data_for_suspicion_digest, send_suggest):
    staff, service, service_2, group_team, group_clarity = data_for_suspicion_digest
    group_clarity.delete()
    group_team.send_suggest = send_suggest
    group_team.save(update_fields=('send_suggest',))

    find_issues()
    check_issue_groups()

    with patch('plan.holidays.calendar.get_list_workdays') as get_list_workdays:
        get_list_workdays.side_effect = list_workdays_some_months
        send_suspicion_digest()

    if send_suggest:
        assert len(mail.outbox) == 1
    else:
        assert len(mail.outbox) == 0


@freeze_time('2019-12-09')
@pytest.mark.parametrize('service_state', Service.states.ALL_STATES)
def test_notifications_suspicion_digest(data_for_suspicion_digest, service_state):
    """
    Проверим отправку еженедельного дайджеста.
    Если сервис закрыт или удалён - не отправляем.
    В письме пишем про оба сервиса:
        * У сервиса 1 попадают обе группы (понятность и команда).
        * У сервиса 2 только команда.
    Ожидаем, что понятность в письме должна быть жёлтой, команда - красной.
    """

    staff, service, service_2, group_team, group_clarity = data_for_suspicion_digest

    service.state = service_state
    service.save(update_fields=['state'])

    service_2.state = service_state
    service_2.save(update_fields=['state'])

    find_issues()
    service_2.service_issues.filter(issue__code='empty_description_en').update(state=ServiceIssueStates.APPEALED)
    check_issue_groups()

    with patch('plan.holidays.calendar.get_list_workdays') as get_list_workdays:
        get_list_workdays.side_effect = list_workdays_some_months
        send_suspicion_digest()

    if service_state in [Service.states.CLOSED, Service.states.DELETED]:
        assert len(mail.outbox) == 0

    else:
        assert len(mail.outbox) == 1
        assert staff.email in mail.outbox[0].to
        assert mail.outbox[0].subject == '[ABC] Наведите порядок в ABC-сервисах'
        assert service.name in mail.outbox[0].body
        assert service_2.name in mail.outbox[0].body
        assert group_team.name in mail.outbox[0].body
        assert group_clarity.name in mail.outbox[0].body
        assert mail.outbox[0].body.count(group_team.name) == 2
        assert mail.outbox[0].body.count(group_clarity.name) == 1
        # светофоры по группе "Команда" будут с уровнем critical - красного цвета
        assert mail.outbox[0].body.count('color: #FF0000') == 2
        # светофоры по группе "Понятность" будут с уровнем warning - жёлтого цвета
        assert mail.outbox[0].body.count('color: #FFCC00') == 1


@freeze_time('2019-12-09')
def test_notifications_suspicion_digest_no_problem(data_for_suspicion_digest):
    """
    Если активных проблем нет, есть только групповая, которая активна всегда.
    Письма быть не должно.
    """

    find_issues()
    check_issue_groups()
    issue_code = ['empty_description_ru', 'empty_description_en', 'owner_is_robot']
    ServiceIssue.objects.filter(issue__code__in=issue_code).update(state=ServiceIssueStates.APPEALED)
    find_issues()
    check_issue_groups()

    with patch('plan.holidays.calendar.get_list_workdays') as get_list_workdays:
        get_list_workdays.side_effect = list_workdays_some_months
        send_suspicion_digest()

    assert len(mail.outbox) == 0


@freeze_time('2019-12-23')
def test_notifications_suspicion_digest_today_is_holiday_monday(data_for_suspicion_digest):
    """
    Проверим отправку еженедельного дайджеста.
    Если сегодня понедельник и это выходной, то ничего не отправляем.
    Замокали, что рабочие дни:
        days = [2, 3, 4, 5, 8, 9, 10, 11, 16, 17, 18, 19, 20, 22, 25, 26, 27]
    Поэтому в этом тесте 23 декабря - не рабочий пн
    """

    find_issues()
    check_issue_groups()

    with patch('plan.holidays.calendar.get_list_workdays') as get_list_workdays:
        get_list_workdays.side_effect = list_workdays_some_months
        send_suspicion_digest()

    assert len(mail.outbox) == 0


@freeze_time('2019-12-27')
def test_notifications_suspicion_digest_next_monday_is_holiday(data_for_suspicion_digest):
    """
    Проверим отправку еженедельного дайджеста.
    Если ближайший понедельник - выходной, и вперели только выходные, то отправляем письмо сегодня.
    Замокали, что рабочие дни:
        days = [2, 3, 4, 5, 8, 9, 10, 11, 16, 17, 18, 19, 20, 22, 25, 26, 27]
    Поэтому в этом тесте 30 декабря - не рабочий пн
    """

    find_issues()
    check_issue_groups()

    with patch('plan.holidays.calendar.get_list_workdays') as get_list_workdays:
        get_list_workdays.side_effect = list_workdays_some_months
        send_suspicion_digest()

    assert len(mail.outbox) == 1


@freeze_time('2019-12-26')
def test_notifications_suspicion_digest_next_monday_is_holiday_not_send(data_for_suspicion_digest):
    """
    Проверим отправку еженедельного дайджеста.
    Если ближайший понедельник - выходной, и вперели есть рабочие дни, то ничего не отправляем.
    Замокали, что рабочие дни:
        days = [2, 3, 4, 5, 8, 9, 10, 11, 16, 17, 18, 19, 20, 22, 25, 26, 27]
    Поэтому в этом тесте 30 декабря - не рабочий пн
    """

    find_issues()
    check_issue_groups()

    with patch('plan.holidays.calendar.get_list_workdays') as get_list_workdays:
        get_list_workdays.side_effect = list_workdays_some_months
        send_suspicion_digest()

    assert len(mail.outbox) == 0


@freeze_time('2019-12-01')
@pytest.mark.parametrize('has_new_complaints', [True, False])
def test_notifications_suspicion_digest_complaints(data_for_suspicion_digest, has_new_complaints):
    """
    Проверим отправку еженедельного дайджеста.
    В сервисе есть жалобы, но нет проблем.
    Если новых проблем нет, то письмо не отправляем.
    """

    staff, service, service_2, group_team, group_clarity = data_for_suspicion_digest
    factories.ComplaintFactory(service=service)

    with freeze_time('2019-12-09'):
        with patch('plan.holidays.calendar.get_list_workdays') as get_list_workdays:
            if has_new_complaints:
                factories.ComplaintFactory(service=service)
            get_list_workdays.side_effect = list_workdays_some_months
            send_suspicion_digest()

    if has_new_complaints:
        assert len(mail.outbox) == 1
        assert staff.email in mail.outbox[0].to
        assert mail.outbox[0].subject == '[ABC] Наведите порядок в ABC-сервисах'
        assert service.name in mail.outbox[0].body
        assert service_2.name not in mail.outbox[0].body
        assert group_team.name not in mail.outbox[0].body
        assert group_clarity.name not in mail.outbox[0].body
        assert 'всего: 2' in mail.outbox[0].body
        assert 'новых: 1' in mail.outbox[0].body
        # в письме нет ничего про состояние светофоров:
        assert 'Чтобы исправить проблемы' not in mail.outbox[0].body

    else:
        assert len(mail.outbox) == 0
