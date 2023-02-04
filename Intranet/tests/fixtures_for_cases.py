# coding: utf-8
import pytest
from pretend import stub

from review.lib import datetimes
from review.core import const as core_const
from review.staff import const as staff_const

from tests import helpers


@pytest.fixture
def case_person_review_with_roles_builder(
    review_builder,
    person_review_builder,
    person_review_role_builder,
    review_role_builder
):
    def builder(**params):
        if 'review_status' in params:
            review_status = params['review_status']
        else:
            review_status = core_const.REVIEW_STATUS.IN_PROGRESS

        if 'review' not in params:
            review = review_builder(status=review_status)
            superreviewer = review_role_builder(
                review=review,
                type=core_const.ROLE.REVIEW.SUPERREVIEWER,
            )
        else:
            review = params['review']
            superreviewer = None

        person_review_params = {
            'status': core_const.PERSON_REVIEW_STATUS.EVALUATION
        }
        if 'person_review_params' in params:
            person_review_params.update(params['person_review_params'])

        person_review = person_review_builder(
            review=review,
            **person_review_params
        )

        if 'reviewers_roles' in params:
            reviewers_roles = []
            top_reviewer_roles = []

            for role_data in params['reviewers_roles']:
                role_type = role_data['type']
                if role_type == core_const.ROLE.PERSON_REVIEW.TOP_REVIEWER:
                    roles_list = top_reviewer_roles
                else:
                    roles_list = reviewers_roles

                roles_list.append(
                    person_review_role_builder(
                        person_review=person_review,
                        type=role_type,
                        person=role_data['person'],
                        position=role_data['position'],
                    )
                )
        else:
            reviewers_roles = [
                person_review_role_builder(
                    person_review=person_review,
                    type=core_const.ROLE.PERSON_REVIEW.REVIEWER,
                    position=index,
                )
                for index in range(3)
            ]
            top_reviewer_roles = [
                person_review_role_builder(
                    person_review=person_review,
                    type=core_const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
                    position=3,
                )
            ]

        return stub(
            person_review=person_review,
            review=review,
            superreviewer=superreviewer,
            roles=reviewers_roles + top_reviewer_roles,
        )
    return builder


@pytest.fixture
def case_person_review_in_progress(case_person_review_with_roles_builder):
    return case_person_review_with_roles_builder()


@pytest.fixture
def case_in_progress_review_with_everybody(
    review_builder,
    review_role_builder,
    case_person_review_with_roles_builder,
):
    review = review_builder(
        status=core_const.REVIEW_STATUS.IN_PROGRESS
    )
    superreviewer = review_role_builder(
        review=review,
        type=core_const.ROLE.REVIEW.SUPERREVIEWER,
    )

    STATUSES = core_const.PERSON_REVIEW_STATUS
    wait_eval_person_review_data = case_person_review_with_roles_builder(
        review=review,
        person_review_params={
            'status': STATUSES.WAIT_EVALUATION,
        }
    )
    roles = wait_eval_person_review_data.roles
    reviewers_roles = [
        {
            'person': role.person,
            'type': role.type,
            'position': role.position,
        }
        for role in wait_eval_person_review_data.roles

    ]

    STATUS_TO_APPROVE_LEVEL = {
        STATUSES.APPROVAL: 1,
        STATUSES.APPROVED: len(reviewers_roles),
        STATUSES.ANNOUNCED: len(reviewers_roles),
    }
    person_reviews = [
        wait_eval_person_review_data.person_review
    ] + [
        case_person_review_with_roles_builder(
            review=review,
            person_review_params={
                'status': status,
                'approve_level': STATUS_TO_APPROVE_LEVEL.get(status, 0),
            },
            reviewers_roles=reviewers_roles,
        ).person_review
        for status in STATUSES.ALL - {STATUSES.WAIT_EVALUATION}
    ]
    return stub(
        review=review,
        superreviewer=superreviewer,
        person_reviews=person_reviews,
        roles=roles,
    )


@pytest.fixture
def case_two_reviews_for_person(
    person_review_builder,
):
    person_review_latest = person_review_builder()
    person_review_earliest = person_review_builder(
        person=person_review_latest.person,
        review__start_date=datetimes.shifted(
            person_review_latest.review.start_date,
            months=-6,
        ),
        review__finish_date=datetimes.shifted(
            person_review_latest.review.finish_date,
            months=-6,
        ),

    )
    return stub(
        person_review_latest=person_review_latest,
        person_review_earliest=person_review_earliest,
        review_latest=person_review_latest.review,
        review_earliest=person_review_earliest.review,
        person=person_review_earliest.person
    )


@pytest.fixture
def case_actual_reviewer_and_others(
    review_role_builder,
    person_review_role_builder,
):
    reviewer_role = person_review_role_builder(
        type=core_const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    person_review = reviewer_role.person_review
    review = person_review.review
    top_role = person_review_role_builder(
        type=core_const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
        person_review=person_review,
        position=1,
    )
    superreviewer_role = review_role_builder(
        type=core_const.ROLE.REVIEW.SUPERREVIEWER,
        review=review,
    )
    helpers.update_model(
        reviewer_role.person_review,
        status=core_const.PERSON_REVIEW_STATUS.APPROVAL,
        mark='A',
        approve_level=0,
    )
    return stub(
        reviewer=reviewer_role.person,
        top_reviewer=top_role.person,
        superreviewer=superreviewer_role.person,
        review=review,
        person_review=person_review,
    )


@pytest.fixture
def case_reviewers_dep_roles(
    case_actual_reviewer_and_others,
    department_role_builder,
    global_role_builder,
):
    case = case_actual_reviewer_and_others
    person = case.person_review.person
    head_role = department_role_builder(
        department=person.department,
        type=staff_const.STAFF_ROLE.DEPARTMENT.HEAD,
    )
    hr_partner_role = department_role_builder(
        department=person.department,
        type=staff_const.STAFF_ROLE.HR.HR_PARTNER,
    )
    hr_analyst_role = department_role_builder(
        department=person.department,
        type=staff_const.STAFF_ROLE.HR.HR_ANALYST,
    )
    exporter_role = global_role_builder(
        type=core_const.ROLE.GLOBAL.EXPORTER,
    )
    return stub(
        head=head_role.person,
        hr_partner=hr_partner_role.person,
        exporter=exporter_role.person,
        hr_analyst=hr_analyst_role.person,
        **case.__dict__
    )
