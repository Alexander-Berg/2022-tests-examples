# coding: utf-8


import pytz
from pretend import stub
import pytest
from django.conf import settings
from post_office import models as post_office_models

from review.shortcuts import models
from review.lib import wf
from review.core import const as core_const
from review.notifications import (
    creators,
    const,
)
from tests import helpers


PERSON_REVIEW_STATUS = core_const.PERSON_REVIEW_STATUS

COLLECT_COMMENTS_WITH_PREFETCH_NUM_QUERIES = 6
SAVE_EMAIL_NUM_QUERIES = 1
MARK_NOTIFICED_NUM_QUERIES = 1
TRANSACTION_NUM_QUERIES = 2
EXPECTED_NUM_QUERIES = sum([
    COLLECT_COMMENTS_WITH_PREFETCH_NUM_QUERIES,
    SAVE_EMAIL_NUM_QUERIES,
    MARK_NOTIFICED_NUM_QUERIES,
    TRANSACTION_NUM_QUERIES,
])


TEST_COMMENT = '**<div>DUMMY</div>**'


def test_created(test_data, mock_formatter):
    creators.create_notifications_for_person_review_comments()

    assert post_office_models.Email.objects.count() == 1
    assert not models.PersonReviewComment.objects.filter(notified=False).exists()


def test_disableable(test_data, settings):
    settings.DISABLE_NOTIFICATION_TYPES = [
        const.TYPES.COMMENTED,
    ]
    creators.create_notifications_for_person_review_changes()

    assert post_office_models.Email.objects.count() == 0
    assert not models.PersonReviewChange.objects.filter(notified=False).exists()


def test_expected_receivers_reviewers_simple(test_data, mock_formatter):
    creators.create_notifications_for_person_review_comments()

    receivers = [
        email.context['receiver_login']
        for email in post_office_models.Email.objects.all()
    ]
    helpers.assert_ids_equal(
        receivers,
        [test_data.first_reviewer_role.person.login],
    )


def test_expected_receivers_with_superreviewers(test_data, mock_formatter):
    helpers.update_model(
        test_data.person_review.review,
        notify_events_other=True,
        notify_events_superreviewer=True,
    )

    creators.create_notifications_for_person_review_comments()

    receivers = [
        email.context['receiver_login']
        for email in post_office_models.Email.objects.all()
    ]
    helpers.assert_ids_equal(
        receivers,
        [
            test_data.first_reviewer_role.person.login,
            test_data.superreviewer_role.person.login,
        ],
    )


def test_expected_receivers_disable_settings(test_data, mock_formatter):
    helpers.update_model(
        test_data.person_review.review,
        notify_events_other=False,
        notify_events_superreviewer=False,
    )

    creators.create_notifications_for_person_review_comments()

    receivers = [
        email.context['receiver_login']
        for email in post_office_models.Email.objects.all()
    ]
    helpers.assert_ids_equal(
        receivers,
        [],
    )


def test_expected_queries_count(test_data, mock_formatter):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        creators.create_notifications_for_person_review_comments()


def test_context_is_ok(test_data, mock_formatter):
    person_review = test_data.person_review
    review = person_review.review
    comment = test_data.comment
    language = test_data.language
    creators.create_notifications_for_person_review_comments()

    email = post_office_models.Email.objects.first()
    helpers.assert_is_substructure(
        {
            'review': {
                'id': review.id,
                'name': review.name,
            },
            'comment_data': [
                {
                    'person_review': {
                        'id': person_review.id,
                        'review': {
                            'id': review.id,
                        },
                        'person': {
                            'login': person_review.person.login,
                        },
                    },
                    'comments': [
                        {
                            'subject': {
                                'login': comment.subject.login,
                                'first_name': getattr(
                                    comment.subject, 'first_name_' + language),
                            },
                            'text_wiki': TEST_COMMENT,
                        },
                    ],
                },
            ],
        },
        email.context,
    )


def test_content_is_ok(test_data, mock_formatter):
    language = test_data.language
    person_review = test_data.person_review
    creators.create_notifications_for_person_review_comments()
    receiver = test_data.first_reviewer_role.person
    commenter = test_data.comment.subject

    email = post_office_models.Email.objects.first()
    if language == 'ru':
        helpers.assert_all_strings_in_text(
            text=email.html_message,
            strings=(
                'комментарий',
            )
        )
    else:
        helpers.assert_all_strings_in_text(
            text=email.html_message,
            strings=(
                'comment',
            )
        )
    helpers.assert_all_strings_in_text(
        text=email.subject,
        strings=(
            person_review.review.name,
        )
    )
    helpers.assert_all_strings_in_html(
        text=email.html_message,
        strings=(
            '/reviews/{review_id}/{login}/{person_review_id}'.format(
                review_id=person_review.review_id,
                login=person_review.person.login,
                person_review_id=person_review.id,
            ),
            commenter.login,
            receiver.work_email,
        )
    )


def test_datetimes_in_context_are_ok(test_data, mock_formatter):
    updated_at = test_data.comment.updated_at
    receiver = test_data.first_reviewer_role.person
    ekb_timezone = "Asia/Yekaterinburg"
    helpers.update_model(receiver, timezone=ekb_timezone)

    creators.create_notifications_for_person_review_comments()

    email = post_office_models.Email.objects.first()
    localized_date = updated_at.astimezone(pytz.timezone(ekb_timezone))

    only_comment = email.context['comment_data'][0]['comments'][0]
    context_date = only_comment['updated_at']
    assert context_date == localized_date.strftime(settings.MAIN_DATETIME_FORMAT)


@pytest.fixture(
    params=[
        {'language': 'ru'},
        {'language': 'en'},
    ]
)
def test_data(
    request,
    case_person_review_in_progress,
    person_review_comment_builder,
):
    language = request.param['language']
    person_review = case_person_review_in_progress.person_review
    for role in person_review.roles.all():
        helpers.update_model(
            role.person,
            language=language,
        )
    first_reviewer_role = person_review.roles.filter(
        type=core_const.ROLE.PERSON_REVIEW.REVIEWER
    ).order_by('position').first()
    comment = person_review_comment_builder(
        person_review=person_review,
        text_wiki=TEST_COMMENT,
        created_at_ago={
            'minutes': -(settings.EVENT_NOTIFICATION_DELAY_IN_MINUTES - 1),
        },
    )
    helpers.update_model(
        person_review.review,
        notify_events_other=True,
        notify_events_superreviewer=False,
    )
    return stub(
        language=language,
        person_review=person_review,
        comment=comment,
        superreviewer_role=case_person_review_in_progress.superreviewer,
        first_reviewer_role=first_reviewer_role,
    )


@pytest.fixture
def mock_formatter(monkeypatch):
    def mock(*args, **kwargs):
        return stub(text=kwargs['data']['text'])
    monkeypatch.setattr(wf.WikiConnector, 'post', mock)
