# coding: utf-8
"""
Плюшки в режиме авто
Если плюшковый файл не загружен
    для всех ролей входные поля редактируются без сайд-эффектов
    для ревьюера плюшки не редактируются
Если плюшковый файл загружен
    через import для всех без автоапдейта
    через actions — автоапдейт только для ревьюера
    для суперревьюера и топа плюшки редактируется вручную
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
            field + '_mode': const.REVIEW_MODE.MODE_AUTO
            for field in const.FIELDS.GOODIE_FIELDS
        }
    )
    helpers.update_model(
        case_actual_reviewer_and_others.review,
        **{
            const.REVIEW_MODE.MARK_MODE: const.REVIEW_MODE.MODE_MANUAL,
            const.REVIEW_MODE.GOLDSTAR_MODE: const.REVIEW_MODE.MODE_MANUAL,
            const.REVIEW_MODE.LEVEL_CHANGE_MODE: const.REVIEW_MODE.MODE_MANUAL,
        }
    )
    return case_actual_reviewer_and_others


@pytest.mark.parametrize('role', edit_helpers.TESTED_ROLES)
@pytest.mark.parametrize('edit_mode', edit_helpers.TESTED_EDIT_MODES)
def test_auto_fields_not_changed_without_file(
    case,
    edit_mode,
    role,
):
    input_fields = [
        const.FIELDS.MARK,
        const.FIELDS.GOLDSTAR,
        const.FIELDS.LEVEL_CHANGE,
    ]

    edit_helpers.do_update(
        edit_mode=edit_mode,
        role=role,
        case=case,
        update_fields=input_fields,
    )

    edit_helpers.assert_only_affected(
        case=case,
        updated_fields=input_fields,
    )


@pytest.mark.parametrize('edit_mode, role, should_be_updated', [
    ('actions', const.ROLE.PERSON_REVIEW.REVIEWER, edit_helpers.ALL_EDIT_FIELDS - {const.FIELDS.BONUS_RSU}),
    ('actions', const.ROLE.PERSON_REVIEW.TOP_REVIEWER, edit_helpers.INPUT_FIELDS),
    ('actions', const.ROLE.REVIEW.SUPERREVIEWER, edit_helpers.INPUT_FIELDS),
    ('import', const.ROLE.PERSON_REVIEW.REVIEWER, edit_helpers.INPUT_FIELDS),
    ('import', const.ROLE.PERSON_REVIEW.TOP_REVIEWER, edit_helpers.INPUT_FIELDS),
    ('import', const.ROLE.REVIEW.SUPERREVIEWER, edit_helpers.INPUT_FIELDS),
])
def test_auto_fields_changed_with_uploaded_goodie_file(
    case,
    edit_mode,
    role,
    should_be_updated,
    review_goodie_builder,
    finance_builder,
):
    edit_helpers.build_person_matched_goodie(
        case=case,
        review_goodie_builder=review_goodie_builder,
        finance_builder=finance_builder,
    )

    input_fields = [
        const.FIELDS.MARK,
        const.FIELDS.GOLDSTAR,
        const.FIELDS.LEVEL_CHANGE,
    ]

    edit_helpers.do_update(
        edit_mode=edit_mode,
        role=role,
        case=case,
        update_fields=input_fields,
    )

    edit_helpers.assert_only_affected(
        case=case,
        updated_fields=should_be_updated,
    )


@pytest.mark.parametrize('edit_mode', edit_helpers.TESTED_EDIT_MODES)
@pytest.mark.parametrize('upload_goodie_file', edit_helpers.YES_NO)
@pytest.mark.parametrize('role, should_be_updated', [
    (const.ROLE.PERSON_REVIEW.REVIEWER, []),
    (const.ROLE.PERSON_REVIEW.TOP_REVIEWER, edit_helpers.GOODIE_FIELDS),
    (const.ROLE.REVIEW.SUPERREVIEWER, edit_helpers.GOODIE_FIELDS),
    (const.ROLE.PERSON_REVIEW.REVIEWER, []),
    (const.ROLE.PERSON_REVIEW.TOP_REVIEWER, edit_helpers.GOODIE_FIELDS),
    (const.ROLE.REVIEW.SUPERREVIEWER, edit_helpers.GOODIE_FIELDS),
])
def test_auto_fields_editable_for_some_roles_goodie_file_not_matter(
    case,
    edit_mode,
    upload_goodie_file,
    role,
    should_be_updated,
    review_goodie_builder,
    finance_builder,
):
    helpers.update_model(
        case.review,
        **{
            const.REVIEW_MODE.MARK_MODE: const.REVIEW_MODE.MODE_MANUAL,
            const.REVIEW_MODE.GOLDSTAR_MODE: const.REVIEW_MODE.MODE_MANUAL,
            const.REVIEW_MODE.LEVEL_CHANGE_MODE: const.REVIEW_MODE.MODE_MANUAL,
        }
    )
    if upload_goodie_file:
        edit_helpers.build_person_matched_goodie(
            case=case,
            review_goodie_builder=review_goodie_builder,
            finance_builder=finance_builder,
        )

    def remove_connected(fields):
        # only one of bonuses and salary changes field needs to be updated
        return {it for it in fields if it not in {const.FIELDS.BONUS, const.FIELDS.SALARY_CHANGE}}
    edit_helpers.do_update(
        edit_mode=edit_mode,
        role=role,
        case=case,
        update_fields=remove_connected(edit_helpers.GOODIE_FIELDS),
    )

    edit_helpers.assert_only_affected(
        case=case,
        updated_fields=remove_connected(should_be_updated),
    )
