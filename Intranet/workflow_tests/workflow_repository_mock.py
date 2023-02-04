from uuid import UUID
from typing import List, Dict, Optional

from staff.person.models import Staff

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.workflow_service.entities import WorkflowRepositoryInterface, AbstractWorkflow


class WorkflowRepositoryMock(WorkflowRepositoryInterface):
    def __init__(self) -> None:
        self._workflows: Dict[UUID, AbstractWorkflow] = {}

    def save(self, workflow: AbstractWorkflow) -> UUID:
        self._workflows[workflow.id] = workflow
        return workflow.id

    def get_pending_workflows_by_budget_position_code(self, budget_position_code: int):
        pass

    def get_by_id(self, workflow_id: UUID) -> AbstractWorkflow:
        return self._workflows[workflow_id]

    def get_workflows_by_proposal_id(self, proposal_id: int, status: str) -> List[AbstractWorkflow] or None:
        pass

    def cancel_workflows_for_credit_repayment(self, credit_management_id: int) -> None:
        pass

    def get_workflows_by_credit_management_application(self, credit_repayment_id: int) -> List[AbstractWorkflow]:
        pass

    def is_all_workflows_for_credit_repayment_not_in_pending(self, credit_management_id: int) -> bool:
        return self._workflows and all(workflow.is_finished() for workflow in self._workflows.values())

    def is_all_workflows_for_credit_repayment_is_cancelled(self, credit_management_id: int) -> bool:
        return self._workflows and all(
            workflow.status == WORKFLOW_STATUS.CANCELLED
            for workflow in self._workflows.values()
        )

    def queue_workflows(self, workflow_ids: List[UUID], person: Optional[Staff]) -> None:
        pass

    def retry_workflows(self, workflow_ids: List[UUID], person: Staff) -> None:
        pass

    def get_workflow_list(self, workflow_ids: List[UUID]) -> List[AbstractWorkflow]:
        pass

    def get_workflows(self, status: str) -> List[UUID]:
        pass

    def can_workflow_be_finalized(self, workflow_id: UUID) -> bool:
        pass

    def get_related_department_ids(self, workflow_id: UUID) -> List[int]:
        pass

    def get_related_tickets(self, workflow_id: UUID) -> List[str]:
        pass

    def mark_changes_as_failed(self, workflow_id: UUID, exc: Exception) -> None:
        pass

    def cancel_workflows_for_proposal(self, proposal_id: str):
        pass
