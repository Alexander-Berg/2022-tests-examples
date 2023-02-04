# coding: utf-8
import pytest

from review.shortcuts import const
from review.core.logic import assemble

from tests import helpers


@pytest.mark.parametrize(
    'status',
    const.PERSON_REVIEW_STATUS.ALL - {
        const.PERSON_REVIEW_STATUS.ANNOUNCED
    }
)
def test_not_announced_not_self_visible(status, person_review):
    helpers.update_model(
        person_review,
        status=status,
    )

    pre = assemble.get_person_review(
        subject=person_review.person,
        fields_requested=const.FIELDS.ALL,
        id=person_review.id,
    )
    assert pre is None


def test_announced_self_visible_some_fields(person_review):
    helpers.update_model(
        person_review,
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
        mark='E',
        goldstar=const.GOLDSTAR.OPTION_AND_BONUS,
    )

    pre = assemble.get_person_review(
        subject=person_review.person,
        fields_requested=const.FIELDS.ALL,
        id=person_review.id,
    )
    assert pre.mark == 'E'
    assert pre.salary_value != const.NO_ACCESS
    assert pre.level != const.NO_ACCESS
