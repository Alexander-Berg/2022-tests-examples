# coding: utf-8
import datetime

from review.core import const

from tests import helpers
from tests.core_assemble_collect_person_reviews import test_get_allowed

AUTH_NUM_QUERIES = 1
REVIEW_MODIFIERS_NUM_QUERIES = 1
FETCH_COMMENTS_NUM_QUERIES = 1
FETCH_CHANGES_NUM_QUERIES = 1
EXPECTED_NUM_QUERIES = sum([
    AUTH_NUM_QUERIES,
    (
        test_get_allowed.EXPECTED_NUM_QUERIES -
        test_get_allowed.FETCH_PR_FOR_GLOBAL_ROLES_QUERIES -
        test_get_allowed.test_get_as_self.EXPECTED_NUM_QUERIES
    ),
    REVIEW_MODIFIERS_NUM_QUERIES,
    FETCH_COMMENTS_NUM_QUERIES,
    FETCH_CHANGES_NUM_QUERIES,
])


STATUSES = const.PERSON_REVIEW_STATUS
CHANGE_TYPE = const.PERSON_REVIEW_CHANGE_TYPE


def test_get_log_response(
    client,
    person_review,
    review_role_builder,
    person_review_comment_builder,
    person_review_change_builder,
):
    review = person_review.review
    superreviewer = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.SUPERREVIEWER
    )
    comment = person_review_comment_builder(
        person_review=person_review,
        text_wiki='WOW',
    )
    change = person_review_change_builder(
        person_review=person_review,
        diff={
            'status': {
                'old': STATUSES.EVALUATION,
                'new': STATUSES.APPROVAL,
            }
        }
    )
    person_review = comment.person_review

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        result = helpers.get_json(
            client=client,
            login=superreviewer.person.login,
            path='/frontend/persons/{}/log/'.format(person_review.person.login),
        )

    log = result['log']
    helpers.assert_is_substructure(
        [
            {
                'type': 'change',
                'subject': {
                    'login': change.subject.login,
                },
                'diff': {
                    'status': {
                        'old': STATUSES.VERBOSE[STATUSES.EVALUATION],
                        'new': STATUSES.VERBOSE[STATUSES.APPROVAL],
                    },
                },
                'person_review': {
                    'id': person_review.id,
                    'review': {
                        'id': person_review.review.id,
                        'name': person_review.review.name,
                        'start_date': person_review.review.start_date.isoformat(),
                    },
                },
                'subject_type': CHANGE_TYPE.VERBOSE[CHANGE_TYPE.PERSON],
            },
            {
                'type': 'comment',
                'subject': {
                    'login': comment.subject.login,
                },
                'text': 'WOW',
                'actions': {
                    'edit': False,
                },
                'person_review': {
                    'id': person_review.id,
                    'review': {
                        'id': person_review.review.id,
                        'name': person_review.review.name,
                        'start_date': person_review.review.start_date.isoformat(),
                    }
                },
                'subject_type': CHANGE_TYPE.VERBOSE[CHANGE_TYPE.PERSON],
            }
        ],
        log,
    )
