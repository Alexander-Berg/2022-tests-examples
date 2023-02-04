import pytest
from freezegun import freeze_time
from mock import patch

from django.conf import settings
from django.utils import timezone

from plan.services.models import Service
from plan.suspicion.tasks import apply_executions
from common import factories


@pytest.mark.parametrize('execution_code,state', [
    ('change_state_to_need_info', Service.states.NEEDINFO),
    ('change_state_to_closed', Service.states.CLOSED)
])
@pytest.mark.parametrize('execution_time', ['today', 'yesterday'])
def test_change_state(robot, execution_code, state, execution_time):
    service_issue = factories.ServiceIssueFactory()
    execution = factories.ExecutionFactory(code=execution_code)
    service_execution = factories.ServiceExecutionActionFactory(execution=execution, service_issue=service_issue)

    if execution_time == 'yesterday':
        service_execution.should_be_applied_at = timezone.now() - timezone.timedelta(days=1)
    else:
        service_execution.should_be_applied_at = timezone.now()
    service_execution.save(update_fields=['should_be_applied_at'])
    apply_executions()
    service_issue.service.refresh_from_db()
    assert service_issue.service.state == (state if execution_time == 'yesterday' else Service.states.IN_DEVELOP)


@freeze_time('2019-01-01T10:00:00')
@pytest.mark.parametrize('execution_time', ['today', 'yesterday'])
def test_change_state_to_closed_with_important_resource(robot, execution_time):
    service_issue = factories.ServiceIssueFactory()
    execution = factories.ExecutionFactory(code='change_state_to_closed')
    service_execution = factories.ServiceExecutionActionFactory(execution=execution, service_issue=service_issue)

    if execution_time == 'yesterday':
        service_execution.should_be_applied_at = timezone.now() - timezone.timedelta(hours=11)
    else:
        service_execution.should_be_applied_at = timezone.now()
    service_execution.save(update_fields=['should_be_applied_at'])

    with patch('plan.services.models.Service.has_important_resources') as mock_request:
        mock_request.return_value = True
        apply_executions()
    service_issue.service.refresh_from_db()
    assert service_issue.service.state == (
        Service.states.CLOSED if execution_time == 'yesterday' else Service.states.IN_DEVELOP
    )


@pytest.mark.parametrize('execution_code,state', [
    ('change_state_to_need_info', Service.states.NEEDINFO),
    ('change_state_to_closed', Service.states.CLOSED)
])
def test_meta_other_execution(robot, execution_code, state):
    meta_other = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    service_issue = factories.ServiceIssueFactory(service=meta_other)
    execution = factories.ExecutionFactory(code=execution_code)
    factories.ServiceExecutionActionFactory(execution=execution, service_issue=service_issue)
    apply_executions()
    service_issue.service.refresh_from_db()
    assert service_issue.service.state == Service.states.IN_DEVELOP
