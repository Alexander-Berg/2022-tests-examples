import json
import uuid

import pytest
from mock import patch, MagicMock

from staff.departments.models import DepartmentRoles
from staff.lib.testing import DepartmentStaffFactory

from staff.headcounts.views import move_to_maternity


@pytest.mark.django_db
def test_move_to_maternity(post_rf, company):
    # given
    chief = company.persons['yandex-chief']
    DepartmentStaffFactory(
        staff=chief,
        role_id=DepartmentRoles.HR_ANALYST.value,
        department=company.yandex,
    )
    person = company.persons['dep1-person']
    form = {'budget_position': person.budget_position.code, 'person': person.login}
    request = post_rf('headcounts-api:move_to_maternity', form, chief.user)
    id = uuid.uuid1()

    # when
    with patch('staff.headcounts.views.maternity.create_issue') as m:
        m.return_value = MagicMock(key='TICKET-12345')
        path = 'staff.budget_position.workflow_service.WorkflowRegistryService.try_create_workflow_for_maternity'
        with patch(path) as create_mock:
            create_mock.return_value = id
            response = move_to_maternity(request)

    # then
    assert response.status_code == 200
    result = json.loads(response.content)
    assert result == {'ticket_key': 'TICKET-12345', 'workflow_id': str(id)}
