import pytest
from django.utils import timezone
import pretend

from plan.suspicion import models
from plan.suspicion.constants import LEVELS
from plan.suspicion.tasks import process_service_execution_actions
from common import factories
from plan import settings
from plan.services.models import Service

pytestmark = [pytest.mark.django_db]


@pytest.fixture
def single_execution_data():
    # Добавляем одно ServiceIssue без чейна для проверки, что ничего не падает
    factories.ServiceIssueFactory(
        issue=factories.IssueFactory(execution_chain=None),
        issue_action_key='issue_key_none',
    )
    service_issue = factories.ServiceIssueFactory(
        issue=factories.IssueFactory(),
        issue_action_key='issue_key',
    )
    chain = service_issue.issue.execution_chain
    execution = factories.ExecutionFactory()
    execution_step = factories.ExecutionStepFactory(
        execution_chain=chain,
        execution=execution,
        apply_after=timezone.timedelta(days=1),
        is_active=True,
    )
    return pretend.stub(
        service_issue=service_issue,
        issue=service_issue.issue,
        service=service_issue.service,
        execution=execution,
        execution_step=execution_step,
    )


@pytest.fixture
def group_execution_data():
    # Добавляем одно IssueGroup без чейнов для проверки, что ничего не падает
    fake_issue_group = factories.IssueGroupFactory()
    issue_group = factories.IssueGroupFactory()
    fake_issues = [
        factories.IssueFactory(issue_group=fake_issue_group, is_active=True, weight=1.0)
        for _ in range(2)
    ]
    issues = [
        factories.IssueFactory(issue_group=issue_group, is_active=True, weight=1.0)
        for _ in range(2)
    ]

    warn_threshold = factories.IssueGroupThresholdFactory(
        issue_group=issue_group,
        level=LEVELS.WARN,
        threshold=0.49,
    )
    crit_threshold = factories.IssueGroupThresholdFactory(
        issue_group=issue_group,
        level=LEVELS.CRIT,
        threshold=0.99,
    )
    factories.IssueGroupThresholdFactory(
        issue_group=fake_issue_group,
        level=LEVELS.WARN,
        threshold=0.49,
        chain=None,
    )
    factories.IssueGroupThresholdFactory(
        issue_group=fake_issue_group,
        level=LEVELS.CRIT,
        threshold=0.99,
        chain=None,
    )
    execution_warn = factories.ExecutionFactory()
    execution_crit = factories.ExecutionFactory()
    factories.ExecutionStepFactory(
        execution_chain=warn_threshold.chain,
        execution=execution_warn,
        apply_after=timezone.timedelta(days=1),
        is_active=True,
    )
    factories.ExecutionStepFactory(
        execution_chain=crit_threshold.chain,
        execution=execution_crit,
        apply_after=timezone.timedelta(days=1),
        is_active=True,
    )
    service_issue = factories.ServiceIssueFactory(
        issue_group=issue_group,
    )
    return pretend.stub(
        service_issue=service_issue,
        service=service_issue.service,
        issue_group=issue_group,
        issues=issues,
        fake_issues=fake_issues,
        warn_threshold=warn_threshold,
        crit_threshold=crit_threshold,
        execution_warn=execution_warn,
        execution_crit=execution_crit,
    )


def test_create_service_execution_action_with_deleted_service(single_execution_data):
    process_service_execution_actions()
    assert models.ServiceExecutionAction.objects.count() == 1
    single_execution_data.service.state = Service.states.DELETED
    single_execution_data.service.save(update_fields=['state'])
    process_service_execution_actions()
    assert models.ServiceExecutionAction.objects.count() == 0


def test_create_service_execution_action_with_closed_service(single_execution_data):
    process_service_execution_actions()
    assert models.ServiceExecutionAction.objects.count() == 1
    assert models.ServiceExecutionAction.objects.get().held_at is None
    single_execution_data.service.state = Service.states.CLOSED
    single_execution_data.service.save(update_fields=['state'])
    process_service_execution_actions()
    assert models.ServiceExecutionAction.objects.count() == 1
    assert models.ServiceExecutionAction.objects.get().held_at is not None


@pytest.mark.parametrize('state', [Service.states.CLOSED, Service.states.DELETED])
def test_issues_for_closed_services_dont_create_execussions(single_execution_data, state):
    single_execution_data.service.state = state
    single_execution_data.service.save(update_fields=['state'])
    process_service_execution_actions()
    assert models.ServiceExecutionAction.objects.count() == 0


def test_create_service_execution_action_by_single_issue(single_execution_data):
    process_service_execution_actions()
    action = models.ServiceExecutionAction.objects.get()
    assert action.execution == single_execution_data.execution
    action_apply_time = action.should_be_applied_at - timezone.now()
    delta = timezone.timedelta(minutes=1)
    step_apply_time = single_execution_data.execution_step.apply_after
    assert step_apply_time + delta >= action_apply_time >= step_apply_time - delta
    assert action.held_at is None
    assert action.issue_action_key == single_execution_data.service_issue.issue_action_key


@pytest.mark.parametrize('temp_state', [models.ServiceIssue.STATES.APPEALED, models.ServiceIssue.STATES.FIXED])
@pytest.mark.parametrize('final_state', [models.ServiceIssue.STATES.ACTIVE, models.ServiceIssue.STATES.FIXED])
def test_hold_execution_action_by_single_issue(single_execution_data, temp_state, final_state):
    process_service_execution_actions()
    action = models.ServiceExecutionAction.objects.get()
    assert action.held_at is None
    single_execution_data.service_issue.change_state(temp_state)

    process_service_execution_actions()
    action.refresh_from_db()
    assert action.held_at is not None
    action.held_at = timezone.now() - settings.MAX_SERVICE_EXECUTION_ACTION_HELD_TIME - timezone.timedelta(days=1)
    action.save()
    single_execution_data.service_issue.change_state(final_state)

    process_service_execution_actions()
    if final_state == models.ServiceIssue.STATES.ACTIVE:
        action.refresh_from_db()
        assert action.held_at is None
    else:
        assert models.ServiceExecutionAction.objects.count() == 0


@pytest.mark.parametrize('issue_count', [1, 2])
def test_one_change_group_issue(group_execution_data, issue_count):
    process_service_execution_actions()
    related_traffic_status = models.ServiceTrafficStatus.objects.get(
        service=group_execution_data.service,
        issue_group=group_execution_data.issue_group,
    )
    assert models.ServiceExecutionAction.objects.count() == 0
    assert related_traffic_status.level == LEVELS.OK

    for i in range(issue_count):
        issue = group_execution_data.issues[i]
        factories.ServiceIssueFactory(service=group_execution_data.service, issue=issue)
        # Проверяем, что ничего не падает от Threshold без чейнов
        fake_issue = group_execution_data.fake_issues[i]
        factories.ServiceIssueFactory(service=group_execution_data.service, issue=fake_issue)

    process_service_execution_actions()

    related_traffic_status.refresh_from_db()
    if issue_count == 2:
        assert models.ServiceExecutionAction.objects.count() == 1
        assert related_traffic_status.level == LEVELS.CRIT
    else:
        assert models.ServiceExecutionAction.objects.count() == 1
        assert related_traffic_status.level == LEVELS.WARN


def test_step_change_group_issue(group_execution_data):
    process_service_execution_actions()
    related_traffic_status = models.ServiceTrafficStatus.objects.get(
        service=group_execution_data.service,
        issue_group=group_execution_data.issue_group,
    )

    factories.ServiceIssueFactory(
        service=group_execution_data.service,
        issue=group_execution_data.issues[0],
        issue_action_key='a',
    )
    process_service_execution_actions()

    related_traffic_status.refresh_from_db()
    assert related_traffic_status.level == LEVELS.WARN
    assert models.ServiceExecutionAction.objects.get().held_at is None

    factories.ServiceIssueFactory(
        service=group_execution_data.service,
        issue=group_execution_data.issues[1],
        issue_action_key='b',
    )
    process_service_execution_actions()

    related_traffic_status.refresh_from_db()
    assert related_traffic_status.level == LEVELS.CRIT
    assert models.ServiceExecutionAction.objects.count() == 2
    assert models.ServiceExecutionAction.objects.get(execution=group_execution_data.execution_warn).held_at is not None
    assert models.ServiceExecutionAction.objects.get(execution=group_execution_data.execution_crit).held_at is None


def test_hold_unhold_create(single_execution_data):
    # Создается первый action
    process_service_execution_actions()

    action = models.ServiceExecutionAction.objects.get()
    assert action.held_at is None

    service = single_execution_data.service
    service_issue = single_execution_data.service_issue
    issue = service_issue.issue
    service_issue.change_state(models.ServiceIssue.STATES.FIXED)
    new_service_issue = factories.ServiceIssueFactory(
        service=service,
        issue=issue,
        issue_action_key=service_issue.issue_action_key + 'a',
    )

    # Первый action заморожен, создается второй
    process_service_execution_actions()

    action.refresh_from_db()
    assert action.held_at is not None
    new_action = models.ServiceExecutionAction.objects.order_by('id').last()
    assert new_action.held_at is None

    new_service_issue.change_state(models.ServiceIssue.STATES.FIXED)
    factories.ServiceIssueFactory(
        service=service,
        issue=issue,
        issue_action_key=service_issue.issue_action_key,
    )

    # Первый action разморожен, второй заморожен
    process_service_execution_actions()

    action.refresh_from_db()
    new_action.refresh_from_db()
    assert action.held_at is None
    assert new_action.held_at is not None
