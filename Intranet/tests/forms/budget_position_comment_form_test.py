import pytest

from random import random

from staff.lib.testing import verify_forms_error_code, BudgetPositionFactory

from staff.budget_position.forms import BudgetPositionCommentForm


@pytest.mark.django_db
def test_budget_position_comment_form():
    target = BudgetPositionCommentForm(
        data={
            'budget_position': BudgetPositionFactory().code,
            'comment': ''.join(('1' for _ in range(255))),
        },
    )

    result = target.is_valid()

    assert result is True, target.errors


@pytest.mark.django_db
def test_budget_position_comment_form_no_budget_position():
    target = BudgetPositionCommentForm(
        data={
            'comment': ''.join(('1' for _ in range(255))),
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'budget_position', 'required')


@pytest.mark.django_db
def test_budget_position_comment_form_invalid_budget_position():
    target = BudgetPositionCommentForm(
        data={
            'budget_position': 123,
            'comment': ''.join(('1' for _ in range(255))),
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'budget_position', 'invalid_choice')


@pytest.mark.django_db
def test_budget_position_comment_form_no_comment():
    target = BudgetPositionCommentForm(
        data={
            'budget_position': BudgetPositionFactory().code,
        },
    )

    result = target.is_valid()

    assert result is True


@pytest.mark.django_db
def test_budget_position_comment_form_empty_comment():
    target = BudgetPositionCommentForm(
        data={
            'budget_position': BudgetPositionFactory().code,
            'comment': '',
        },
    )

    result = target.is_valid()

    assert result is True


@pytest.mark.django_db
def test_budget_position_comment_form_whitespace_comment():
    target = BudgetPositionCommentForm(
        data={
            'budget_position': BudgetPositionFactory().code,
            'comment': '     ',
        },
    )

    result = target.is_valid()

    assert result is True
    assert not target.cleaned_data['comment']


@pytest.mark.django_db
def test_budget_position_comment_form_trim_comment():
    comment_text = f'comment {random()}'
    target = BudgetPositionCommentForm(
        data={
            'budget_position': BudgetPositionFactory().code,
            'comment': '   ' + comment_text + '\t',
        },
    )

    result = target.is_valid()

    assert result is True
    assert target.cleaned_data['comment'] == comment_text


@pytest.mark.django_db
def test_budget_position_comment_form_long_comment():
    target = BudgetPositionCommentForm(
        data={
            'budget_position': BudgetPositionFactory().code,
            'comment': ''.join(('1' for _ in range(256))),
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'comment', 'max_length')
