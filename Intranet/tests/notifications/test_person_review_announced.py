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
    settings.DISABLE_NOTIFICATION_TYPES = [const.TYPES.ANNOUNCED]
    creators.create_notifications_for_person_review_changes()

    assert post_office_models.Email.objects.count() == 0
    assert not models.PersonReviewChange.objects.filter(notified=False).exists()


def test_off_does_not_sent(test_data):
    review = test_data.person_review.review
    review.notify_events_other = False
    review.save()

    creators.create_notifications_for_person_review_changes()
    assert not post_office_models.Email.objects.exists()
    assert not models.PersonReviewChange.objects.filter(notified=False).exists()


def test_expected_queries_count(test_data):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        creators.create_notifications_for_person_review_changes()


def test_context_is_ok(test_data):
    person_review = test_data.person_review
    creators.create_notifications_for_person_review_changes()

    email = post_office_models.Email.objects.first()
    helpers.assert_is_substructure(
        {
            'person_review': {
                'id': person_review.id,
                'review': {
                    'id': person_review.review.id,
                    'name': person_review.review.name,
                },
                'person': {
                    'login': person_review.person.login,
                }
            },
        },
        email.context,
    )


def test_content_is_ok(test_data):
    language = test_data.language
    person_review = test_data.person_review
    creators.create_notifications_for_person_review_changes()

    email = post_office_models.Email.objects.first()
    if language == 'ru':
        helpers.assert_all_strings_in_text(
            text=email.html_message,
            strings=(
                'опубликованы',
            )
        )
    else:
        helpers.assert_all_strings_in_text(
            text=email.html_message,
            strings=(
                'published',
                'results',
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
            '/reviews/%s/%s/%s' % (
                person_review.review_id,
                person_review.person.login,
                person_review.id,
            ),
            person_review.person.work_email,
        ),
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
    person_review_change_builder(
        person_review=person_review,
        diff={
            core_const.FIELDS.STATUS: {
                'new': PERSON_REVIEW_STATUS.ANNOUNCED,
            },
        },
        created_at_ago={
            'minutes': -(settings.EVENT_NOTIFICATION_DELAY_IN_MINUTES - 1),
        },
    )
    helpers.update_model(person_review.person, language=language)
    helpers.update_model(person_review, status=PERSON_REVIEW_STATUS.ANNOUNCED)
    return stub(
        language=language,
        person_review=person_review,
    )
