import pytest

from django.urls import reverse

from ok.approvements.choices import APPROVEMENT_STAGE_STATUSES

from tests import factories as f


pytestmark = pytest.mark.django_db


def test_approvement_count(client, django_assert_num_queries):
    user = 'approver'

    # Согласования, где user – текущий согласующий
    f.create_approvement(approvers=[user, 'another'], is_parallel=False)
    f.create_approvement(approvers=[user, 'another', user], is_parallel=False)
    f.create_approvement(approvers=['another', user], is_parallel=True)

    # Другие его разные согласования
    f.create_approvement(approvers=['another', user], is_parallel=False)
    f.ApprovementStageFactory(approver=user, is_approved=True)
    f.ApprovementStageFactory(approver=user, status=APPROVEMENT_STAGE_STATUSES.suspended)

    url = reverse('private_api:staff:approvements-counts')
    client.force_authenticate(user)
    # 2 запроса на savepoint + 1 запрос с count
    with django_assert_num_queries(3):
        response = client.get(url)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['current_count'] == 3
