# coding: utf-8
"""
Все поля в ручном режиме.

Если оценка в ручном — должна редактироваться для всех ролей
Если голдстар в ручном — должен редактироваться для всех ролей
Любое плюшковое поле в ручном — должно редактироваться для все ролей
Если голдстар для суперревьюеров — должен редактироваться только ими

Редактируем одно поле — редактируется только оно, остальные не редактируются.
В PersonReviewChange только ожидаемое изменение
Редактирование одинаковое через actions и import
"""

import pytest

from review.shortcuts import const

from tests import helpers
from tests.core_person_review_actions import edit_helpers


@pytest.fixture(name='case')
def case(case_actual_reviewer_and_others):
    helpers.update_model(
        case_actual_reviewer_and_others.person_review,
        **edit_helpers.FIELDS_BEFORE
    )
    helpers.update_model(
        case_actual_reviewer_and_others.review,
        **{
            const.REVIEW_MODE.FIELDS_TO_MODIFIERS[field]: const.REVIEW_MODE.MODE_MANUAL
            for field in edit_helpers.ALL_EDIT_FIELDS
        }
    )
    return case_actual_reviewer_and_others


@pytest.mark.parametrize('role', edit_helpers.TESTED_ROLES)
@pytest.mark.parametrize('edit_mode', edit_helpers.TESTED_EDIT_MODES)
@pytest.mark.parametrize('field', edit_helpers.ALL_EDIT_FIELDS)
def test_manual_fields_correctly_edited(
    case,
    edit_mode,
    role,
    field,
):
    edit_helpers.do_update(
        edit_mode=edit_mode,
        role=role,
        case=case,
        update_fields=[field],
    )

    edit_helpers.assert_only_affected(
        case=case,
        updated_fields=[field],
    )


@pytest.mark.parametrize('role, expected', [
    (const.ROLE.PERSON_REVIEW.REVIEWER, False),
    (const.ROLE.PERSON_REVIEW.TOP_REVIEWER, True),
    (const.ROLE.REVIEW.SUPERREVIEWER, True),
])
@pytest.mark.parametrize('edit_mode', edit_helpers.TESTED_EDIT_MODES)
def test_goldstar_manual_by_chosen_fields_correctly_edited(
    case,
    edit_mode,
    role,
    expected,
):
    helpers.update_model(
        case.review,
        goldstar_mode=const.REVIEW_MODE.MODE_MANUAL_BY_CHOSEN,
    )

    edit_helpers.do_update(
        edit_mode=edit_mode,
        role=role,
        case=case,
        update_fields=[const.FIELDS.GOLDSTAR],
    )

    if expected:
        updated_fields = [const.FIELDS.GOLDSTAR]
    else:
        updated_fields = []
    edit_helpers.assert_only_affected(
        case=case,
        updated_fields=updated_fields,
    )
