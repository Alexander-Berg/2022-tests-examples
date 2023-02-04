import pytest

from plan.services.models import Service
from plan.suspicion.constants import LEVELS
from plan.suspicion.tasks import check_issue_groups
from plan.suspicion.models import ServiceIssue

from common import factories


def test_update_level():
    service = factories.ServiceFactory()
    issue_group = factories.IssueGroupFactory()
    factories.IssueGroupThresholdFactory(issue_group=issue_group, threshold=0.3)
    factories.IssueGroupThresholdFactory(issue_group=issue_group, threshold=0.7)
    service_traffic_status = factories.ServiceTrafficStatusFactory(issue_group=issue_group, service=service)

    assert service_traffic_status.get_new_level(0.8) == LEVELS.CRIT
    assert service_traffic_status.get_new_level(0.4) == LEVELS.WARN
    assert service_traffic_status.get_new_level(0.2) == LEVELS.OK


@pytest.mark.parametrize('state', Service.states.ALL_STATES)
def test_calculate_level(state):
    service = factories.ServiceFactory(state=state)
    issue_group = factories.IssueGroupFactory()
    factories.ServiceIssueFactory(service=service, issue_group=issue_group)
    factories.IssueGroupThresholdFactory(issue_group=issue_group, threshold=0.3)
    factories.IssueGroupThresholdFactory(issue_group=issue_group, threshold=0.7)
    service_traffic_status = factories.ServiceTrafficStatusFactory(issue_group=issue_group, service=service)
    issue1 = factories.IssueFactory(issue_group=issue_group)
    factories.IssueFactory(issue_group=issue_group)

    check_issue_groups()
    service_traffic_status.refresh_from_db()
    assert service_traffic_status.level == LEVELS.OK

    service_issue = factories.ServiceIssueFactory(service=service, issue=issue1, state=ServiceIssue.STATES.ACTIVE)
    check_issue_groups()
    service_traffic_status.refresh_from_db()
    assert service_traffic_status.level == (LEVELS.WARN if state != Service.states.DELETED else LEVELS.OK)

    service_issue.state = ServiceIssue.STATES.REVIEW
    service_issue.save()
    check_issue_groups()
    service_traffic_status.refresh_from_db()
    assert service_traffic_status.level == (LEVELS.WARN if state != Service.states.DELETED else LEVELS.OK)

    service_issue.state = ServiceIssue.STATES.FIXED
    service_issue.save()
    check_issue_groups()
    service_traffic_status.refresh_from_db()
    assert service_traffic_status.level == LEVELS.OK
