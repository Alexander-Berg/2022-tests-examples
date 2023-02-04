import pytest

from random import random

from staff.budget_position.models import BudgetPositionComment
from staff.lib.testing import StaffFactory, BudgetPositionFactory

from staff.budget_position.controllers import attach_comment_to_budget_position_controller


@pytest.mark.django_db
def test_attach_comment_to_budget_position_create_new():
    person = StaffFactory()
    budget_position = BudgetPositionFactory()
    comment = f'comment {random()}'

    attach_comment_to_budget_position_controller(person, budget_position, comment)

    created_comment_record = BudgetPositionComment.objects.get(person=person, budget_position=budget_position)
    assert created_comment_record.comment == comment


@pytest.mark.django_db
def test_attach_comment_to_budget_position_update_existing():
    person = StaffFactory()
    budget_position = BudgetPositionFactory()
    BudgetPositionComment.objects.create(
        person=person,
        budget_position=budget_position,
        comment=f'old comment {random()}'
    )
    comment = f'new comment {random()}'

    attach_comment_to_budget_position_controller(person, budget_position, comment)

    created_comment_record = BudgetPositionComment.objects.get(person=person, budget_position=budget_position)
    assert created_comment_record.comment == comment


@pytest.mark.django_db
def test_attach_comment_to_budget_position_remove_existing():
    person = StaffFactory()
    budget_position = BudgetPositionFactory()
    BudgetPositionComment.objects.create(
        person=person,
        budget_position=budget_position,
        comment=f'old comment {random()}',
    )

    attach_comment_to_budget_position_controller(person, budget_position, '')

    comment_query_set = BudgetPositionComment.objects.filter(person=person, budget_position=budget_position)
    assert comment_query_set.count() == 0


@pytest.mark.django_db
def test_attach_comment_to_budget_position_remove():
    person = StaffFactory()
    budget_position = BudgetPositionFactory()

    attach_comment_to_budget_position_controller(person, budget_position, '')

    comment_query_set = BudgetPositionComment.objects.filter(person=person, budget_position=budget_position)
    assert comment_query_set.count() == 0
