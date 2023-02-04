from pretend import stub
import pytest

from django.core.urlresolvers import reverse

from plan.suspicion.models import (
    ServiceIssue,
    ServiceExecutionAction,
    ExecutionStep,
)
from plan.suspicion.constants import LEVELS
from plan.suspicion.tasks import check_issue_groups
from common import factories
from plan.common.utils.dates import datetime_isoformat_with_microseconds


@pytest.fixture
def data(owner_role):
    service = factories.ServiceFactory()
    factories.ServiceMemberFactory(staff=service.owner, service=service, role=owner_role)
    issue_group = factories.IssueGroupFactory()
    threshold_crit = factories.IssueGroupThresholdFactory(
        issue_group=issue_group,
        level=LEVELS.CRIT,
        threshold=0.7,
    )
    step_crit = factories.ExecutionStepFactory(
        execution_chain=threshold_crit.chain,
        is_active=True,
    )
    threshold_warn = factories.IssueGroupThresholdFactory(
        issue_group=issue_group,
        level=LEVELS.WARN,
        threshold=0.3,
    )
    step_warn = factories.ExecutionStepFactory(
        execution_chain=threshold_warn.chain,
        is_active=True,
    )
    issues = [
        factories.IssueFactory(issue_group=issue_group)
        for _ in range(2)
    ]
    service_issue_group = factories.ServiceIssueFactory(
        service=service,
        issue_group=issue_group,
    )
    traffic_light = factories.ServiceTrafficStatusFactory(
        service=service,
        issue_group=issue_group,
    )

    return stub(
        service=service,
        staff=service.owner,
        issue_group=issue_group,
        threshold_warn=threshold_warn,
        threshold_crit=threshold_crit,
        step_warn=step_warn,
        step_crit=step_crit,
        issues=issues,
        service_issue_group=service_issue_group,
        traffic_light=traffic_light,
    )


def test_same_crit_and_warn_threshold(client, data):
    data.threshold_crit.threshold = data.threshold_warn.threshold
    data.threshold_crit.save()

    data.traffic_light.current_weight = 0.5
    data.traffic_light.level = LEVELS.WARN
    data.traffic_light.save()

    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )

    assert response.status_code == 200

    result = response.json()
    thresholds = {
        threshold['level']: threshold
        for threshold in result[0]['summary']['thresholds']
    }

    assert not thresholds[LEVELS.WARN]['is_current']
    assert thresholds[LEVELS.CRIT]['is_current']


def test_issuegroups_summary_thresholds_no_executions(client, data):
    ExecutionStep.objects.all().delete()
    check_issue_groups()
    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.status_code == 200
    result = response.json()
    thresholds = result[0]['summary']['thresholds']
    assert all(threshold['execution'] is None for threshold in thresholds)


def test_issuegroups_summary_thresholds_executions(client, data):
    check_issue_groups()
    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.status_code == 200
    result = response.json()
    thresholds = {
        threshold['level']: threshold
        for threshold in result[0]['summary']['thresholds']
    }
    assert thresholds[LEVELS.WARN]['is_next']
    assert not thresholds[LEVELS.WARN]['is_current']
    warn_execution = data.threshold_warn.chain.steps.get().execution
    assert thresholds[LEVELS.WARN]['execution'] == {
        'name': {'ru': warn_execution.name, 'en': warn_execution.name_en}
    }
    assert not thresholds[LEVELS.CRIT]['is_next']
    assert not thresholds[LEVELS.CRIT]['is_current']
    assert thresholds[LEVELS.CRIT]['execution'] is None

    ServiceExecutionAction.objects.create_from_service_issue_and_chain(
        ServiceIssue.objects.get(service=data.service.id),
        data.threshold_warn.chain,
    )
    data.traffic_light.current_weight = 0.5
    data.traffic_light.level = LEVELS.WARN
    data.traffic_light.save()

    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.status_code == 200
    result = response.json()
    thresholds = {
        threshold['level']: threshold
        for threshold in result[0]['summary']['thresholds']
    }
    assert not thresholds[LEVELS.WARN]['is_next']
    assert thresholds[LEVELS.WARN]['is_current']
    assert thresholds[LEVELS.WARN]['execution'] == {
        'is_critical': False,
        'name': {'ru': warn_execution.name, 'en': warn_execution.name_en},
        'should_be_applied_at': datetime_isoformat_with_microseconds(ServiceExecutionAction.objects.get().should_be_applied_at),
        'applied_at': None,
    }
    assert thresholds[LEVELS.CRIT]['is_next']
    assert not thresholds[LEVELS.CRIT]['is_current']
    crit_execution = data.threshold_crit.chain.steps.get().execution
    assert thresholds[LEVELS.CRIT]['execution'] == {
        'name': {'ru': crit_execution.name, 'en': crit_execution.name_en}
    }
