# coding: utf-8


import mock
import pytest

from review.core import const
from review.core.logic import bulk

from tests import helpers

DUMMY_COMMENT = """
They're Good
Dogs Brent
"""


@pytest.mark.parametrize(
    'old, new', [
        ['bad', 'good'],
        ['?', '-'],
    ]
)
def test_edit_mark(person_review_role_reviewer, old, new):
    subject = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review
    review = person_review.review
    helpers.update_model(person_review, mark=old)
    helpers.update_model(review, mark_mode=const.REVIEW_MODE.MODE_MANUAL)

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk.bulk_same_action_set(
            subject=subject,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.MARK: new,
            }
        )

    changes = person_review.changes.all()
    comments = person_review.comments.all()
    assert len(changes) == 1
    assert len(comments) == 0
    only_change = changes[0]
    helpers.assert_is_substructure(
        {
            const.FIELDS.MARK: {
                'old': old,
                'new': new,
            }
        },
        only_change.diff
    )


def test_edit_mark_with_comment(person_review_role_reviewer):
    subject = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review
    review = person_review.review
    helpers.update_model(person_review, mark='bad')
    helpers.update_model(review, mark_mode=const.REVIEW_MODE.MODE_MANUAL)

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        result = bulk.bulk_same_action_set(
            subject=subject,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.MARK: 'good',
                const.PERSON_REVIEW_ACTIONS.COMMENT: DUMMY_COMMENT,
            }
        )
    only_result = list(result.values())[0]
    assert not only_result['failed']

    changes = person_review.changes.all()
    comments = person_review.comments.all()
    assert len(changes) == 1
    assert len(comments) == 1
    only_change = changes[0]
    only_comment = comments[0]
    helpers.assert_is_substructure(
        {
            const.FIELDS.MARK: {
                'old': 'bad',
                'new': 'good',
            }
        },
        only_change.diff
    )
    assert only_comment.text_wiki == DUMMY_COMMENT
