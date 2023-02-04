# coding: utf-8


from pretend import stub
import pytest
from django.conf import settings
from post_office import models as post_office_models

from review.shortcuts import models
from review.core import const as core_const
from review.notifications import creators
from review.notifications import const
from tests import helpers


PERSON_REVIEW_STATUS = core_const.PERSON_REVIEW_STATUS

COLLECT_CHANGES_WITH_PREFETCH_NUM_QUERIES = 6
SAVE_EMAIL_NUM_QUERIES = 1
MARK_NOTIFICED_NUM_QUERIES = 1
TRANSACTION_NUM_QUERIES = 2
EXPECTED_NUM_QUERIES = sum([
    COLLECT_CHANGES_WITH_PREFETCH_NUM_QUERIES,
    SAVE_EMAIL_NUM_QUERIES,
    MARK_NOTIFICED_NUM_QUERIES,
    TRANSACTION_NUM_QUERIES,
])


def test_created(test_data):
    creators.create_notifications_for_person_review_changes()

    assert post_office_models.Email.objects.count() == 1
    assert not models.PersonReviewChange.objects.filter(notified=False).exists()


def test_disableable(test_data, settings):
    settings.DISABLE_NOTIFICATION_TYPES = [const.TYPES.FLAGGED]
    creators.create_notifications_for_person_review_changes()

    assert post_office_models.Email.objects.count() == 0
    assert not models.PersonReviewChange.objects.filter(notified=False).exists()


def test_expected_receivers_reviewers_simple(test_data):
    creators.create_notifications_for_person_review_changes()

    receivers = [
        email.context['receiver_login']
        for email in post_office_models.Email.objects.all()
    ]
    helpers.assert_ids_equal(
        receivers,
        [test_data.first_reviewer_role.person.login],
    )


def test_expected_receivers_with_superreviewers(test_data):
    helpers.update_model(
        test_data.person_review.review,
        notify_events_other=True,
        notify_events_superreviewer=True,
    )

    creators.create_notifications_for_person_review_changes()

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


def test_expected_receivers_disable_settings(test_data):
    helpers.update_model(
        test_data.person_review.review,
        notify_events_other=False,
        notify_events_superreviewer=False,
    )

    creators.create_notifications_for_person_review_changes()

    receivers = [
        email.context['receiver_login']
        for email in post_office_models.Email.objects.all()
    ]
    helpers.assert_ids_equal(
        receivers,
        [],
    )


def test_expected_queries_count(test_data):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        creators.create_notifications_for_person_review_changes()


def test_context_is_ok(test_data):
    person_review = test_data.person_review
    review = person_review.review
    change = test_data.change
    language = test_data.language
    creators.create_notifications_for_person_review_changes()

    email = post_office_models.Email.objects.first()
    helpers.assert_is_substructure(
        {
            'review': {
                'id': review.id,
                'name': review.name,
            },
            'change_data': [
                {
                    'change_author': {
                        'login': change.subject.login,
                        'first_name': getattr(
                            change.subject, 'first_name_' + language)
                    },
                    'changes': {
                        'flagged': [
                            {
                                'person_review': {
                                    'id': person_review.id,
                                    'review': {
                                        'id': review.id,
                                    },
                                    'person': {
                                        'login': person_review.person.login,
                                    }
                                }
                            },
                        ]
                    }
                }
            ]
        },
        email.context,
    )


def test_content_is_ok(test_data):
    language = test_data.language
    person_review = test_data.person_review
    creators.create_notifications_for_person_review_changes()
    receiver = test_data.first_reviewer_role.person
    changer = test_data.change.subject

    email = post_office_models.Email.objects.first()
    if language == 'ru':
        helpers.assert_all_strings_in_text(
            text=email.html_message,
            strings=(
                'обсуждени',
            )
        )
    else:
        helpers.assert_all_strings_in_text(
            text=email.html_message,
            strings=(
                'flag',
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
            receiver.work_email,
        )
    )


@pytest.fixture(
    params=[
        {'language': 'ru'},
        {'language': 'en'},
    ]
)
def test_data(
    request,
    case_person_review_in_progress,
    person_review_change_builder,
):
    language = request.param['language']
    person_review = case_person_review_in_progress.person_review
    change = person_review_change_builder(
        person_review=person_review,
        diff={
            core_const.FIELDS.FLAGGED: {
                'new': True,
            },
        },
        created_at_ago={
            'minutes': -(settings.EVENT_NOTIFICATION_DELAY_IN_MINUTES - 1),
        },
    )
    for role in person_review.roles.all():
        helpers.update_model(
            role.person,
            language=language
        )
    first_reviewer_role = person_review.roles.filter(
        type=core_const.ROLE.PERSON_REVIEW.REVIEWER
    ).order_by('position').first()
    helpers.update_model(
        person_review,
        mark='E',
        status=PERSON_REVIEW_STATUS.APPROVAL,
        approve_level=0,
    )
    helpers.update_model(
        person_review.review,
        notify_events_other=True,
        notify_events_superreviewer=False,
    )
    return stub(
        language=language,
        person_review=person_review,
        change=change,
        first_reviewer_role=first_reviewer_role,
        superreviewer_role=case_person_review_in_progress.superreviewer,
    )
