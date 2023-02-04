from typing import List, Dict, Tuple, Optional
from uuid import uuid1, UUID

import factory

from staff.budget_position.const import WORKFLOW_STATUS, FemidaProfessionalLevel
from staff.budget_position.models import Workflow, ChangeRegistry
from staff.budget_position.workflow_service import entities
from staff.budget_position.workflow_service.entities import (
    OccupationId,
    OccupationDetails,
    RewardSchemeId,
    RewardSchemeDetails,
    ReviewSchemeId,
    ReviewSchemeDetails,
    BonusSchemeId,
    BonusSchemeDetails, Person, PersonId, PersonSchemeException,
)


class WorkflowModelFactory(factory.DjangoModelFactory):
    class Meta:
        model = Workflow

    id = factory.LazyFunction(uuid1)
    code = '1.1'
    manually_processed = None
    status = WORKFLOW_STATUS.PENDING


class ChangeRegistryFactory(factory.DjangoModelFactory):

    class Meta:
        model = ChangeRegistry


class TableflowMock(entities.TableflowService):
    def __init__(self):
        self._review_scheme_id_responses: Dict[entities.ReviewSchemeIdRequest, entities.ReviewSchemeId or None] = {}
        self._bonus_scheme_id_responses: Dict[entities.BonusSchemeIdRequest, entities.BonusSchemeId or None] = {}
        self._reward_category_responses: Dict[entities.RewardCategoryRequest, entities.RewardCategory or None] = {}
        self._reward_scheme_id_responses: Dict[entities.RewardSchemeIdRequest, entities.RewardSchemeId or None] = {}

        self._review_scheme_id_for_group_responses: Dict[
            entities.ReviewSchemeIdByGroupRequest,
            entities.ReviewSchemeId or None,
        ] = {}
        self._bonus_scheme_id_for_group_responses: Dict[
            entities.BonusSchemeIdByGroupRequest,
            entities.ReviewSchemeId or None,
        ] = {}

    def add_response_for_reward_scheme_id(
        self,
        request: entities.RewardSchemeIdRequest,
        response: entities.RewardSchemeId,
    ):
        self._reward_scheme_id_responses[request] = response

    def reward_scheme_id(self, requests: List[entities.RewardSchemeIdRequest]) -> List[entities.RewardSchemeId or None]:
        result = []

        for request in requests:
            if request in self._reward_scheme_id_responses:
                result.append(self._reward_scheme_id_responses[request])
            else:
                result.append(None)

        return result

    def add_response_for_reward_category(
        self,
        request: entities.RewardCategoryRequest,
        response: entities.RewardCategory,
    ):
        self._reward_category_responses[request] = response

    def reward_category(self, requests: List[entities.RewardCategoryRequest]) -> List[entities.RewardCategory or None]:
        result = []

        for request in requests:
            if request in self._reward_category_responses:
                result.append(self._reward_category_responses[request])
            else:
                result.append(None)

        return result

    def add_response_for_bonus_scheme_id_by_group(
        self,
        request: entities.BonusSchemeIdByGroupRequest,
        response: entities.BonusSchemeId or None,
    ):
        self._bonus_scheme_id_for_group_responses[request] = response

    def bonus_scheme_id_by_group(
        self,
        requests: List[entities.BonusSchemeIdByGroupRequest],
    ) -> List[entities.BonusSchemeId or None]:
        result = []

        for request in requests:
            if request in self._bonus_scheme_id_for_group_responses:
                result.append(self._bonus_scheme_id_for_group_responses[request])
            else:
                result.append(None)

        return result

    def add_response_for_bonus_scheme_id(
        self,
        request: entities.BonusSchemeIdRequest,
        response: entities.BonusSchemeId or None,
    ):
        self._bonus_scheme_id_responses[request] = response

    def bonus_scheme_id(self, requests: List[entities.BonusSchemeIdRequest]) -> List[entities.BonusSchemeId or None]:
        result = []

        for request in requests:
            if request in self._bonus_scheme_id_responses:
                result.append(self._bonus_scheme_id_responses[request])
            else:
                result.append(None)

        return result

    def add_response_for_review_scheme_id(
        self,
        request: entities.ReviewSchemeIdRequest,
        response: entities.ReviewSchemeId or None,
    ):
        self._review_scheme_id_responses[request] = response

    def review_scheme_id(self, requests: List[entities.ReviewSchemeIdRequest]) -> List[entities.ReviewSchemeId or None]:
        result = []

        for request in requests:
            if request in self._review_scheme_id_responses:
                result.append(self._review_scheme_id_responses[request])
            else:
                result.append(None)

        return result

    def add_response_for_review_scheme_id_by_group(
        self,
        request: entities.ReviewSchemeIdByGroupRequest,
        response: entities.ReviewSchemeId or None,
    ):
        self._review_scheme_id_for_group_responses[request] = response

    def review_scheme_id_by_group(
        self,
        requests: List[entities.ReviewSchemeIdByGroupRequest],
    ) -> List[entities.ReviewSchemeId or None]:
        result = []

        for request in requests:
            if request in self._review_scheme_id_for_group_responses:
                result.append(self._review_scheme_id_for_group_responses[request])
            else:
                result.append(None)

        return result


class OEBSServiceMock(entities.OEBSService):
    def __init__(self):
        self._grade_data_response: Dict[str, entities.GradeData or None] = {}
        self._grade_ids: Dict[Tuple[entities.OccupationId, int], int] = {}
        self._mapping: Dict[Tuple[entities.OccupationId, FemidaProfessionalLevel], int] = {}
        self._grade_data: Dict[entities.GradeId, entities.GradeData or None] = {}
        self._positions_as_change: Dict[entities.BudgetPositionCode, entities.Change] = {}

    def add_mapping_for_femida_level(
        self,
        occupation: entities.OccupationId,
        level: FemidaProfessionalLevel,
        grade_level: int,
    ):
        self._mapping[(occupation, level)] = grade_level

    def get_grade_id_by_femida_level(
        self,
        occupation: entities.OccupationId,
        level: FemidaProfessionalLevel,
    ) -> int or None:
        return self._mapping.get((occupation, level))

    def get_salary_data(self, login: str) -> entities.SalaryData:
        raise NotImplementedError

    def get_budget_link(self, change: entities.Change) -> str:
        raise NotImplementedError

    def add_grade_id(self, occupation: entities.OccupationId, level: int or None, grade_id: int):
        self._grade_ids[(occupation, level)] = grade_id

    def get_grade_id(self, occupation: entities.OccupationId, level: int or None) -> int or None:
        return self._grade_ids.get((occupation, level))

    def set_grade_data(self, grade_id: entities.GradeId, data: entities.GradeData or None):
        self._grade_data[grade_id] = data

    def get_grade_data(self, grade_id: entities.GradeId) -> entities.GradeData or None:
        return self._grade_data[grade_id]

    def get_crossing_position_info_as_change(
        self,
        budget_position_code: entities.BudgetPositionCode,
        login: str,
    ) -> entities.Change:
        raise NotImplementedError

    def set_position_as_change(
        self,
        budget_position_code: entities.BudgetPositionCode,
        change: entities.Change,
    ) -> None:
        self._positions_as_change[budget_position_code] = change

    def get_position_as_change(self, budget_position_code: entities.BudgetPositionCode) -> entities.Change:
        return self._positions_as_change[budget_position_code]

    def update_changes_push_status(self, changes: List[entities.Change]) -> bool:
        pass

    def push_next_change_to_oebs(self, workflow: entities.AbstractWorkflow, catalyst_login: str) -> None:
        raise NotImplementedError

    def get_position_link(self, change: entities.Change, login: str, is_write=False) -> str:
        raise NotImplementedError

    def set_grades_data_response(self, login: str, grade_data: Optional[entities.GradeData]):
        self._grade_data_response[login] = grade_data

    def get_grades_data(self, logins: List[str]) -> Dict[str, Optional[entities.GradeData]]:
        result = {}
        for login in logins:
            result[login] = self._grade_data_response[login]

        return result


class FemidaServiceMock(entities.FemidaService):
    def __init__(self) -> None:
        self._vacancies_by_job_ticket_urls: Dict[str, int] or None = None

    def set_vacancies_by_job_ticket_urls(self, response: Dict[str, int]):
        self._vacancies_by_job_ticket_urls = response

    def vacancies_by_job_ticket_urls(self, job_ticket_urls: List[str]) -> Dict[str, int]:
        return self._vacancies_by_job_ticket_urls

    def push_scheduled(self):
        raise NotImplementedError

    def close_vacancy(self, workflow_id: UUID):
        raise NotImplementedError

    def close_vacancy_url(self, vacancy_id: int):
        raise NotImplementedError

    def change_vacancy_url(self, vacancy_id: int):
        raise NotImplementedError

    def schedule_department_push(self, workflow_id: UUID):
        raise NotImplementedError


class StaffServiceMock(entities.StaffService):
    def __init__(self) -> None:
        self.persons: Dict[PersonId, entities.Person] = {}
        self.placements: Dict[Tuple[int, int], entities.Placement] = {}
        self._bonus_scheme_details: Dict[BonusSchemeId, BonusSchemeDetails] = {}
        self._person_department: Dict[PersonId, int] = {}
        self._occupations: Dict[OccupationId, OccupationDetails] = {}
        self._person_scheme_exceptions: Dict[PersonId, PersonSchemeException] = {}

    def hr_analyst_responsible_for_department(self, department_id) -> Optional[entities.Person]:
        raise NotImplementedError

    def default_hr_analyst(self) -> Person:
        raise NotImplementedError

    def set_person(self, person: entities.Person) -> None:
        self.persons[person.id] = person

    def get_person(self, person_id: int) -> entities.Person:
        return self.persons[person_id]

    def set_placement(self, placement: entities.Placement) -> None:
        self.placements[(placement.office_id, placement.organization_id)] = placement

    def placement_for(self, office_id: int, organization_id: int) -> Optional[entities.Placement]:
        return self.placements.get((office_id, organization_id))

    def set_bonus_scheme_details(self, bonus_scheme_details: BonusSchemeDetails) -> None:
        self._bonus_scheme_details[bonus_scheme_details.scheme_id] = bonus_scheme_details

    def bonus_scheme_details(self, bonus_scheme_id: BonusSchemeId) -> Optional[BonusSchemeDetails]:
        return self._bonus_scheme_details.get(bonus_scheme_id)

    def review_scheme_details(self, review_scheme_id: ReviewSchemeId) -> Optional[ReviewSchemeDetails]:
        raise NotImplementedError

    def reward_scheme_details(self, reward_scheme_id: RewardSchemeId) -> Optional[RewardSchemeDetails]:
        raise NotImplementedError

    def set_occupation_details(self, occupation_id: OccupationId, details: OccupationDetails):
        self._occupations[occupation_id] = details

    def occupation_details(self, occupation_id: OccupationId) -> Optional[OccupationDetails]:
        return self._occupations.get(occupation_id)

    def multiple_occupation_groups(
        self,
        occupation_ids: List[OccupationId],
    ) -> Dict[OccupationId, Optional[OccupationDetails]]:
        return {
            occupation_id: self._occupations.get(occupation_id)
            for occupation_id in occupation_ids
        }

    def get_person_logins(self, person_ids: List[int]) -> Dict[int, str]:
        result = {
            person_id: self.persons[person_id].login
            for person_id in person_ids
            if person_id in self.persons
        }
        return result

    def set_person_department(self, person_id: PersonId, department_id: int) -> None:
        self._person_department[person_id] = department_id

    def person_departments(self, person_ids: List[int]) -> Dict[PersonId, int]:
        return {
            person_id: self._person_department[person_id]
            for person_id in person_ids
            if person_id in self._person_department
        }

    def set_person_scheme_exception(self, person_scheme_exception: PersonSchemeException) -> None:
        self._person_scheme_exceptions[person_scheme_exception.person_id] = person_scheme_exception

    def person_scheme_exceptions(self, person_ids: List[PersonId]) -> Dict[PersonId, PersonSchemeException]:
        return {
            person_id: self._person_scheme_exceptions[person_id]
            for person_id in person_ids
            if person_id in self._person_scheme_exceptions
        }
