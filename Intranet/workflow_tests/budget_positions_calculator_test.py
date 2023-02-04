import pytest

from staff.departments.tests.factories import VacancyFactory
from staff.lib.testing import StaffFactory

from staff.budget_position.workflow_service import entities, gateways
from staff.budget_position.tests.workflow_tests.utils import (
    FemidaServiceMock,
)


@pytest.mark.django_db
def test_calculator_provides_old_and_new_budget_positions_on_move_without_bp():
    # given
    staff = StaffFactory()
    job_ticket_url = 'TJOB-1'
    proposal_data = entities.ProposalData(
        proposal_id=0,
        person_id=staff.id,
        is_move_with_budget_position=False,
        job_ticket_url=job_ticket_url,
    )
    vacancy = VacancyFactory()
    repo = gateways.BudgetPositionsRepository()
    femida_service = FemidaServiceMock()
    femida_service.set_vacancies_by_job_ticket_urls({job_ticket_url: vacancy.id})

    calculator = entities.BudgetPositionsCalculator(repo, femida_service, [proposal_data])

    # when
    result = calculator.budget_positions_for_person_changes()

    # then
    assert result == {
        staff.id: entities.BudgetPositionMove(
            old_budget_position=entities.BudgetPosition(
                id=staff.budget_position.id,
                code=staff.budget_position.code,
            ),
            new_budget_position=entities.BudgetPosition(
                id=vacancy.budget_position.id,
                code=vacancy.budget_position.code,
            ),
        ),
    }
