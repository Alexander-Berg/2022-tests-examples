import pytest

from common import factories

pytestmark = [pytest.mark.django_db]


def test_get_weight_no_group():
    service_issue_group = factories.ServiceIssueFactory()
    service_issue_group.issue_group = None

    with pytest.raises(AssertionError):
        service_issue_group.get_weight_and_count()


def test_get_weight_no_issues_in_group():
    issue_group = factories.IssueGroupFactory()
    service_issue_group = factories.ServiceIssueFactory(issue_group=issue_group)

    assert service_issue_group.get_weight_and_count() == (0, 0)


@pytest.mark.parametrize('max_weight', [None, 8])
def test_get_weight_with_issues(max_weight):
    service = factories.ServiceFactory()
    issue_group = factories.IssueGroupFactory()
    service_issue_group = factories.ServiceIssueFactory(issue_group=issue_group, service=service)
    issue_1 = factories.IssueFactory(issue_group=issue_group, weight=1)
    factories.IssueFactory(issue_group=issue_group, weight=3)
    factories.ServiceIssueFactory(service=service, issue=issue_1)
    factories.ServiceIssueFactory(issue=issue_1)

    weight = 1.0 / ((1 + 3) if max_weight is None else max_weight)
    assert service_issue_group.get_weight_and_count(max_weight=max_weight) == (weight, 1)
