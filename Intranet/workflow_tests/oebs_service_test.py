from datetime import date, datetime
from dateutil import parser

from mock import MagicMock
import pytest

from staff.lib.testing import StaffFactory

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.tests.utils import ChangeFactory
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory
from staff.budget_position.workflow_service import OebsErrorResponseOEBSError, WorkflowInvalidStateError
from staff.budget_position.workflow_service.gateways import OEBSService, WorkflowRepository, BudgetPositionsRepository


@pytest.mark.django_db
def test_push_workflow_with_one_change_to_oebs():
    # given
    now = datetime.now()
    workflow_id = WorkflowModelFactory(status=WORKFLOW_STATUS.CONFIRMED).id
    ChangeFactory(
        ticket='SALARY-123',
        effective_date=parser.parse('2019-08-13').date(),
        position_name='some',
        workflow_id=workflow_id,
    )
    staff = StaffFactory(login='testlogin')

    workflow = WorkflowRepository().get_by_id(workflow_id)
    workflow.catalyst_id = staff.id
    workflow.permission_date = date.today()
    workflow.mark_pushing_to_oebs()

    oebs_service = OEBSService(BudgetPositionsRepository())

    # when
    oebs_service.push_next_change_to_oebs(workflow, staff.login)

    # then
    change = workflow.changes[0]
    assert change.oebs_transaction_id == 123
    assert change.sent_to_oebs >= now


@pytest.mark.django_db
def test_oebs_service_raises_exception_on_errors_while_sending_to_oebs():
    # given
    catalyst = StaffFactory()
    workflow_id = WorkflowModelFactory(status=WORKFLOW_STATUS.CONFIRMED).id
    ChangeFactory(
        ticket='SALARY-123',
        effective_date=parser.parse('2019-08-13').date(),
        position_name='some',
        workflow_id=workflow_id,
    )

    workflow = WorkflowRepository().get_by_id(workflow_id)
    workflow.catalyst_id = catalyst.id
    workflow.permission_date = date.today()
    workflow.mark_pushing_to_oebs()
    error_response = {'error': 'some error'}

    oebs_service = OEBSService(BudgetPositionsRepository())
    oebs_service.send_request = MagicMock(return_value=error_response)

    # when
    with pytest.raises(OebsErrorResponseOEBSError):
        oebs_service.push_next_change_to_oebs(workflow, catalyst.login)


@pytest.mark.django_db
def test_oebs_service_raises_exception_while_trying_to_push_changes_of_not_pushed_workflow():
    # given
    catalyst = StaffFactory()
    workflow_id = WorkflowModelFactory(status=WORKFLOW_STATUS.PENDING).id
    ChangeFactory(
        ticket='SALARY-123',
        effective_date=parser.parse('2019-08-13').date(),
        position_name='some',
        workflow_id=workflow_id,
    )

    workflow = WorkflowRepository().get_by_id(workflow_id)
    workflow.catalyst_id = catalyst.id
    workflow.permission_date = date.today()

    oebs_service = OEBSService(BudgetPositionsRepository())

    # when
    with pytest.raises(WorkflowInvalidStateError):
        oebs_service.push_next_change_to_oebs(workflow, catalyst.login)

    # then
    assert workflow.status == WORKFLOW_STATUS.PENDING
    assert workflow.manually_processed is None


def test_parse_normal_grade_name():
    # given
    oebs_service = OEBSService(None)
    grade_name = 'CallCentreSpec.9.2'

    # when
    grade_data = oebs_service._parse_grade(grade_name)

    # then
    assert grade_data.level == 9
    assert grade_data.occupation == 'CallCentreSpec'


def test_parse_grade_name_without_level():
    # given
    oebs_service = OEBSService(None)
    grade_name = 'CallCentreSpec.Без грейда'

    # when
    grade_data = oebs_service._parse_grade(grade_name)

    # then
    assert grade_data.level is None
    assert grade_data.occupation == 'CallCentreSpec'


def test_parse_unsupported_grade_level():
    # given
    oebs_service = OEBSService(None)
    grade_name = 'CallCentreSpec.Some'

    # when
    grade_data = oebs_service._parse_grade(grade_name)

    # then
    assert grade_data.level is None
    assert grade_data.occupation == 'CallCentreSpec'


def test_parse_unsupported_grade_name():
    # given
    oebs_service = OEBSService(None)
    grade_name = 'CallCentreSpec'

    # when
    grade_data = oebs_service._parse_grade(grade_name)

    # then
    assert grade_data is None


def test_parse_unsupported_grade_format():
    # given
    oebs_service = OEBSService(None)
    grade_name = 42

    # when
    grade_data = oebs_service._parse_grade(grade_name)

    # then
    assert grade_data is None
