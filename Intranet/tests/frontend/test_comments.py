# coding: utf-8
from review.core import const

from tests import helpers
from tests.core_assemble_collect_person_reviews import test_get_allowed

AUTH_NUM_QUERIES = 1
REVIEW_MODIFIERS_NUM_QUERIES = 1
FETCH_COMMENTS_NUM_QUERIES = 1
EXPECTED_NUM_QUERIES = sum([
    AUTH_NUM_QUERIES,
    test_get_allowed.EXPECTED_NUM_QUERIES - test_get_allowed.FETCH_PR_FOR_GLOBAL_ROLES_QUERIES,
    REVIEW_MODIFIERS_NUM_QUERIES,
    FETCH_COMMENTS_NUM_QUERIES,
])


def test_get_comments_list_edit_false(
    client,
    person_review,
    review_role_builder,
    person_review_comment_builder,
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
    person_review = comment.person_review

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        result = helpers.get_json(
            client=client,
            login=superreviewer.person.login,
            path='/frontend/person-reviews/{}/comments/'.format(person_review.id),
        )

    comments = result['comments']
    helpers.assert_is_substructure(
        [
            {
                'subject': {
                    'login': comment.subject.login,
                },
                'text': 'WOW',
                'actions': {
                    'edit': False,
                }
            }
        ],
        comments,
    )


def test_get_comments_list_edit_true_author(
    client,
    person_review,
    review_role_builder,
    person_review_comment_builder,
):
    review = person_review.review
    superreviewer = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.SUPERREVIEWER
    )
    comment = person_review_comment_builder(
        subject=superreviewer.person,
        person_review=person_review,
        text_wiki='WOW',
    )
    person_review = comment.person_review

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        result = helpers.get_json(
            client=client,
            login=superreviewer.person.login,
            path='/frontend/person-reviews/{}/comments/'.format(person_review.id),
        )

    comments = result['comments']
    helpers.assert_is_substructure(
        [
            {
                'subject': {
                    'login': comment.subject.login,
                },
                'text': 'WOW',
                'actions': {
                    'edit': True,
                }
            }
        ],
        comments,
    )


def test_edit_comment_forbidden_if_not_author(
    client,
    review_role_builder,
    person_review_comment,
):
    person_review = person_review_comment.person_review
    superreviewer = review_role_builder(
        review=person_review.review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    helpers.post_json(
        client=client,
        login=superreviewer.person.login,
        request={
            'text': 'WAT',
        },
        path='/frontend/person-reviews/{}/comments/{}/'.format(
            person_review.id,
            person_review_comment.id,
        ),
        expect_status=403,
        json_response=False,
    )


def test_edit_comment_forbidden_if_review_not_active(
    client,
    person_review_role_reviewer,
    person_review_comment_builder,
):
    person_review = person_review_role_reviewer.person_review
    person_review_comment = person_review_comment_builder(
        person_review=person_review,
        subject=person_review_role_reviewer.person,
        text_wiki='text',
    )
    helpers.update_model(
        person_review.review,
        status=const.REVIEW_STATUS.ARCHIVE,
    )

    helpers.post_json(
        client=client,
        login=person_review_role_reviewer.person.login,
        request={
            'text': 'WAT',
        },
        path='/frontend/person-reviews/{}/comments/{}/'.format(
            person_review.id,
            person_review_comment.id,
        ),
        expect_status=403,
        json_response=False,
    )


def test_edit_comment_success(
    client,
    person_review,
    review_role_builder,
    person_review_comment_builder,
):
    superreviewer = review_role_builder(
        review=person_review.review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    person_review_comment = person_review_comment_builder(
        person_review=person_review,
        text_wiki='W',
        subject=superreviewer.person,
    )
    response = helpers.post_json(
        client=client,
        login=superreviewer.person.login,
        request={
            'text': 'WAT',
        },
        path='/frontend/person-reviews/{}/comments/{}/'.format(
            person_review.id,
            person_review_comment.id,
        ),
    )

    comment_from_response = response['comment']
    assert comment_from_response['text'] == 'WAT'
    assert comment_from_response['updated_at'] > comment_from_response['created_at']

    person_review_comment = helpers.fetch_model(person_review_comment)
    assert person_review_comment.text_wiki == 'WAT'
    assert person_review_comment.updated_at > person_review_comment.created_at
