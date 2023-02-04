import pytest

import random
from decimal import Decimal

from staff.lib.testing import StaffFactory

from staff.umbrellas.controllers import update_umbrella_assignments
from staff.umbrellas.models import UmbrellaAssignment
from staff.umbrellas.tests.factories import UmbrellaFactory, UmbrellaAssignmentFactory


@pytest.mark.django_db
def test_update_umbrella_assignments_new_person():
    existing_person = StaffFactory()
    existing_person_umbrellas = [UmbrellaAssignmentFactory(person=existing_person) for _ in range(random.randint(3, 5))]
    new_person = StaffFactory()
    new_engagements = {UmbrellaFactory(): round(Decimal(random.random()), 3) for _ in range(random.randint(3, 5))}

    update_umbrella_assignments(
        [new_person],
        [
            {
                'umbrella': umbrella,
                'engagement': engagement,
            }
            for umbrella, engagement in new_engagements.items()
        ],
    )

    db_existing_umbrellas = dict(
        UmbrellaAssignment.all_assignments
        .filter(person=existing_person, engaged_to__isnull=True)
        .values_list('umbrella_id', 'engagement')
    )
    for existing_umbrella in existing_person_umbrellas:
        assert existing_umbrella.umbrella_id in db_existing_umbrellas
        assert existing_umbrella.engagement == db_existing_umbrellas[existing_umbrella.umbrella_id]

    db_new_umbrellas = dict(
        UmbrellaAssignment.all_assignments
        .filter(person=new_person, engaged_to__isnull=True)
        .values_list('umbrella__issue_key', 'engagement')
    )
    for new_umbrella, new_engagement in new_engagements.items():
        assert new_umbrella.issue_key in db_new_umbrellas
        assert db_new_umbrellas[new_umbrella.issue_key] == new_engagement


@pytest.mark.django_db
def test_update_umbrella_assignments_existing_person():
    existing_person = StaffFactory()
    existing_person_umbrellas = [UmbrellaAssignmentFactory(person=existing_person) for _ in range(random.randint(3, 5))]
    new_engagements = {
        existing_person_umbrellas[0].umbrella: round(Decimal(random.random()), 3),
        existing_person_umbrellas[1].umbrella: existing_person_umbrellas[1].engagement,
    }

    update_umbrella_assignments(
        [existing_person],
        [
            {
                'umbrella': umbrella,
                'engagement': engagement,
            }
            for umbrella, engagement in new_engagements.items()
        ],
    )

    db_new_umbrellas = dict(
        UmbrellaAssignment.all_assignments
        .filter(person=existing_person, engaged_to__isnull=True)
        .values_list('umbrella__issue_key', 'engagement')
    )
    for new_umbrella, new_engagement in new_engagements.items():
        assert new_umbrella.issue_key in db_new_umbrellas
        assert db_new_umbrellas[new_umbrella.issue_key] == new_engagement

    db_new_umbrellas_id = dict(
        UmbrellaAssignment.all_assignments
        .filter(person=existing_person, engaged_to__isnull=True)
        .values_list('umbrella__issue_key', 'id')
    )

    def _get_assignment_id(index):
        return db_new_umbrellas_id[existing_person_umbrellas[index].umbrella.issue_key]

    assert existing_person_umbrellas[0].id != _get_assignment_id(0), 'should create new assignment'
    assert existing_person_umbrellas[1].id == _get_assignment_id(1), 'should use existing assignment'


@pytest.mark.django_db
def test_update_umbrella_assignments_wildcard_assignment():
    existing_person = StaffFactory()
    new_engagement = round(Decimal(random.random()), 3)

    update_umbrella_assignments(
        [existing_person],
        [
            {
                'umbrella': None,
                'engagement': new_engagement,
            },
        ],
    )

    db_new_umbrellas = dict(
        UmbrellaAssignment.all_assignments
        .filter(person=existing_person, engaged_to__isnull=True)
        .values_list('umbrella__issue_key', 'engagement')
    )
    assert set(db_new_umbrellas) == {None}
    assert db_new_umbrellas[None] == new_engagement
