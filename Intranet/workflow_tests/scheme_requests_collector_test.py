import datetime

import pytest
from waffle.models import Switch

from staff.budget_position.workflow_service import entities


def test_collector_ignores_person_without_budget_position():
    # given
    person_id = 0
    occupation_id = 'TEST_OCCUPATION'
    proposal_data = entities.ChangeSchemeRequest(
        person_id=person_id,
        budget_position=None,
        occupation_id=occupation_id,
        grade_level=16,
        department_id=1,
        is_internship=False,
        oebs_date=datetime.date.today(),
        force_recalculate_schemes=None,
    )

    # when
    requests_collector = entities.SchemeRequestsCollector([proposal_data])

    # then
    assert not requests_collector.has_any_change_that_leads_to_scheme_recalculation()


@pytest.mark.django_db
def test_collector_tracks_involved_occupations():
    # given
    Switch.objects.get_or_create(name='copy_oebs_info_when_move_without_budget', active=True)
    person_id = 0
    occupation_id = 'TEST_OCCUPATION'
    change_request = entities.ChangeSchemeRequest(
        person_id=person_id,
        occupation_id=occupation_id,
        budget_position=1,
        grade_level=16,
        department_id=1,
        is_internship=False,
        oebs_date=datetime.date.today(),
        force_recalculate_schemes=None,
    )

    requests_collector = entities.SchemeRequestsCollector([change_request])
    person_departments = {person_id: 100500}
    person_grades = {person_id: entities.GradeData(occupation=occupation_id, level=16)}

    # when
    requests_collector.prepare_data_for_request_by_occupation(person_departments, person_grades)

    # then
    assert requests_collector.involved_occupations == {occupation_id}


@pytest.mark.django_db
def test_collector_is_taking_into_account_changed_occupation():
    # given
    Switch.objects.get_or_create(name='copy_oebs_info_when_move_without_budget', active=True)
    person_id = 0
    occupation_to_change = 'OCCUPATION_TO_CHANGE'
    new_occupation = 'NEW_OCCUPATION'

    change_request = entities.ChangeSchemeRequest(
        person_id=person_id,
        occupation_id=new_occupation,
        budget_position=1,
        grade_level=16,
        department_id=1,
        is_internship=False,
        oebs_date=datetime.date.today(),
        force_recalculate_schemes=None,
    )

    requests_collector = entities.SchemeRequestsCollector([change_request])
    person_departments = {person_id: 100500}
    person_grades = {person_id: entities.GradeData(occupation=occupation_to_change, level=16)}

    # when
    requests_collector.prepare_data_for_request_by_occupation(person_departments, person_grades)

    # then
    result = requests_collector.bonus_scheme_id_requests_by_occupation()
    assert len(result) == 1
    request = result[0][1]
    assert request.occupation_id == new_occupation


@pytest.mark.parametrize(
    'change_scheme_patch, result',
    [
        ({}, True),
        ({'person_id': None}, False),
        ({'budget_position': None}, False),
        ({'force_recalculate_schemes': 0, 'occupation_id': None, 'department_id': None, 'grade_level': None}, False,),
        ({'force_recalculate_schemes': 0, 'occupation_id': 1, 'department_id': 1, 'grade_level': 1}, True,),
        ({'force_recalculate_schemes': 1, 'occupation_id': None, 'department_id': 1, 'grade_level': 1}, True,),
        ({'force_recalculate_schemes': 1, 'occupation_id': 1, 'department_id': None, 'grade_level': 1}, True,),
        ({'force_recalculate_schemes': 1, 'occupation_id': 1, 'department_id': 1, 'grade_level': None}, True,),
    ],
)
def test_has_any_change_that_leads_to_scheme_recalculation(change_scheme_patch, result):
    base_data = {
        'person_id': 1,
        'budget_position': 1,
        'force_recalculate_schemes': 1,
        'occupation_id': 1,
        'department_id': 1,
        'grade_level': 1,
        'is_internship': 1,
        'oebs_date': 1,
    }
    base_data.update(change_scheme_patch)
    target = entities.SchemeRequestsCollector([entities.ChangeSchemeRequest(**base_data)])

    assert target.has_any_change_that_leads_to_scheme_recalculation() == result


def test_collector_ignores_person_with_exceptions():
    # given
    person_id = 0
    occupation_id = 'TEST_OCCUPATION'
    change_scheme_request = entities.ChangeSchemeRequest(
        person_id=person_id,
        budget_position=None,
        occupation_id=occupation_id,
        grade_level=16,
        department_id=1,
        is_internship=False,
        oebs_date=datetime.date.today(),
        force_recalculate_schemes=None,
    )
    requests_collector = entities.SchemeRequestsCollector([change_scheme_request])

    # when
    requests_collector.exclude_persons_from_further_calculations([person_id])

    # then
    assert requests_collector.involved_staff_ids() == []
