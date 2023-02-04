from unittest import mock

import pytest

from staff.budget_position import workflow_service

from staff.headcounts.headcounts_credit_management import Workflows, CreditRepayment
from staff.headcounts.tests.factories import CreditRepaymentRowFactory


@pytest.mark.django_db
def test_workflows_send_request_to_workflow_service():
    # given
    workflow_registry = mock.create_autospec(spec=workflow_service.WorkflowRegistryService)
    workflow = Workflows(workflow_registry)

    row = CreditRepaymentRowFactory()
    credit_repayment = CreditRepayment(
        id=100500,
        author_login='some',
        comment='test_comment',
        ticket=None,
        is_active=True,
        rows=[row],
        closed_at=None,
    )

    # when
    workflow.create_workflow(credit_repayment)

    # then
    workflow_registry.try_create_workflow_for_credit_repayment.assert_called_once_with(
        data=workflow_service.CreditRepaymentData(
            credit_management_id=100500,
            credit_budget_position=row.credit_budget_position.code,
            repayment_budget_position=row.repayment_budget_position.code,
        ),
    )
