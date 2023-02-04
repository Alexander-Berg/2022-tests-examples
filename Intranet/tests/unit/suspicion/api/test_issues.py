import pytest
from django.core.urlresolvers import reverse

from plan.suspicion.models import ServiceIssue
from common import factories

pytestmark = pytest.mark.django_db


def test_get_issue(client):
    issue = factories.ServiceIssueFactory()
    response = client.get(
        reverse('api-v4:issue-detail', args=[issue.pk]),
        {'fields': 'id,context,service'}
    )
    assert response.status_code == 200, response.content
    assert response.json()['service']['slug'] == issue.service.slug


def test_list_issue(client):
    issue = factories.ServiceIssueFactory(context={'smth': 'test'})
    response = client.get(
        reverse('api-v4:issue-list'),
        {
            'fields': 'id,context,service',
            'service': issue.service.id
        }
    )
    assert response.status_code == 200, response.content
    data = response.json()
    assert len(data['results']) == 1
    assert data['results'][0]['service']['slug'] == issue.service.slug
    assert data['results'][0]['context'] == {'smth': 'test'}


@pytest.mark.parametrize('mock_tvm_service_ticket', [100500, 100], indirect=True)
def test_delete_issue(client, mock_tvm_service_ticket):
    group = factories.IssueGroupFactory(allowed_tvm_ids=[100])
    issue = factories.ServiceIssueFactory(issue=factories.IssueFactory(issue_group=group))

    response = client.delete(
        reverse('api-v4:issue-detail', args=[issue.pk]),
    )
    issue = ServiceIssue.objects.filter(pk=issue.id).first()
    if mock_tvm_service_ticket == 100:
        assert response.status_code == 204, response.content
        assert issue is None
    else:
        assert response.status_code == 403, response.content
        assert issue is not None


@pytest.mark.parametrize('mock_tvm_service_ticket', [100], indirect=True)
def test_patch_issue(client, mock_tvm_service_ticket):
    group = factories.IssueGroupFactory(allowed_tvm_ids=[100])
    issue = factories.ServiceIssueFactory(issue=factories.IssueFactory(issue_group=group))

    data = {'ru': 'smth', 'en': 'smth_en'}
    response = client.json.patch(
        reverse('api-v4:issue-detail', args=[issue.pk]),
        data={'context': data}
    )
    assert response.status_code == 200, response.content
    issue.refresh_from_db()
    assert issue.context == data


@pytest.mark.parametrize('mock_tvm_service_ticket', [100, 100500], indirect=True)
def test_create_issue(client, mock_tvm_service_ticket):
    group = factories.IssueGroupFactory(allowed_tvm_ids=[100])
    service = factories.ServiceFactory()
    issue = factories.IssueFactory(issue_group=group)

    data = {'ru': 'smth', 'en': 'smth_en'}
    response = client.json.post(
        reverse('api-v4:issue-list'),
        data={
            'context': data, 'service': service.id,
            'issue': issue.id, 'state': 'active',
        }
    )
    if mock_tvm_service_ticket == 100:
        assert response.status_code == 201, response.content
        service_issue = ServiceIssue.objects.get(service=service)
        assert service_issue.context == data
    else:
        assert response.status_code == 403, response.content
        assert ServiceIssue.objects.filter(service=service).exists() is False


@pytest.mark.parametrize('mock_tvm_service_ticket', [100, 100500], indirect=True)
def test_put_issues(client, mock_tvm_service_ticket):
    service = factories.ServiceFactory()
    group = factories.IssueGroupFactory(allowed_tvm_ids=[100])
    issue = factories.IssueFactory(issue_group=group)
    another_issue = factories.IssueFactory(issue_group=group)
    new_issue = factories.IssueFactory(issue_group=group)

    # изменяем данные этого issue
    service_issue = factories.ServiceIssueFactory(
        service=service, issue=issue,
        context={'ru': 'some', 'en': 'some_en'},
        percentage_of_completion=5,
    )

    # не передаем в запросе - должна удалиться
    service_another_issue = factories.ServiceIssueFactory(
        service=service, issue=another_issue,
    )

    # issue другого сервиса - не должна удалиться
    other_service_issue = factories.ServiceIssueFactory(issue=issue)

    new_group = factories.IssueGroupFactory(allowed_tvm_ids=[100])
    to_del_issue = factories.IssueFactory(issue_group=new_group)
    # передаем пустой список issue для этого сервиса, указанная должна удалиться
    to_del_service_issue = factories.ServiceIssueFactory(issue=to_del_issue)

    response = client.json.put(
        reverse('api-v4:issue-bulk'),
        data=[
            {
                'service': to_del_service_issue.service.slug,
                'group_code': new_group.code,
                'issues': []
            },
            {
                'service': service_issue.service_id,
                'group_code': group.code,
                'issues': [
                    {  # обновление текущей проблемы
                        'code': issue.code,
                        'percentage_of_completion': 2,
                        'context': {'ru': 'value'},
                    },
                    {  # новая проблема
                        'code': new_issue.code,
                        'context': {'ru': 'new'},
                    },
                ]
            }
        ]
    )
    if mock_tvm_service_ticket == 100:
        assert response.status_code == 204, response.content

        to_del_service_issue = ServiceIssue.objects.filter(pk=to_del_service_issue.id).first()
        assert to_del_service_issue is None

        service_another_issue = ServiceIssue.objects.filter(pk=service_another_issue.id).first()
        assert service_another_issue is None

        other_service_issue.refresh_from_db()
        assert other_service_issue is not None

        service_issue.refresh_from_db()
        assert service_issue.percentage_of_completion == 2
        assert service_issue.context == {'ru': 'value'}

        new_service_issue = ServiceIssue.objects.get(
            issue=new_issue, service=service
        )
        assert new_service_issue.percentage_of_completion == 1
        assert new_service_issue.context == {'ru': 'new'}
    else:
        assert response.status_code == 403, response.content
        to_del_service_issue.refresh_from_db()
        assert to_del_service_issue is not None


@pytest.mark.parametrize('mock_tvm_service_ticket', [100], indirect=True)
def test_put_duplicate_issues(client, mock_tvm_service_ticket):
    service = factories.ServiceFactory()
    group = factories.IssueGroupFactory(allowed_tvm_ids=[100])
    issue = factories.IssueFactory(issue_group=group)

    response = client.json.put(
        reverse('api-v4:issue-bulk'),
        data=[
            {
                'service': service.id,
                'group_code': group.code,
                'issues': [
                    {
                        'code': issue.code,
                        'percentage_of_completion': 2,
                        'context': {'ru': 'value'},
                    },
                    {
                        'code': issue.code,
                        'context': {'ru': 'new'},
                    },
                ]
            }
        ]
    )
    assert response.status_code == 400, response.content
    assert response.json()['error']['detail'] == f'Got duplicate issue {issue.code} for {service.id}'
