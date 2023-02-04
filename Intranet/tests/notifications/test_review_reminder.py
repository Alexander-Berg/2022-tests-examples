# coding: utf-8


from datetime import date
from pretend import stub
import pytest

from post_office import models as post_office_models

from review.lib import datetimes
from review.core import const as core_const
from review.notifications import creators
from review.notifications import const
from tests import helpers


PERSON_REVIEW_STATUS = core_const.PERSON_REVIEW_STATUS

FETCHER_NUM_QUERIES = 4
SAVE_EMAIL_NUM_QUERIES = 1
TRANSACTION_NUM_QUERIES = 2
EXPECTED_NUM_QUERIES = sum([
    FETCHER_NUM_QUERIES,
    SAVE_EMAIL_NUM_QUERIES,
    TRANSACTION_NUM_QUERIES,
])


def test_created(test_data):
    creators.create_notifications_for_review_reminders()

    first_reviewer_emails_count = 1
    second_reviwer_emails_count = 2
    assert post_office_models.Email.objects.count() == len([
        first_reviewer_emails_count,
        second_reviwer_emails_count,
    ])


def test_disableable(test_data, settings):
    settings.DISABLE_NOTIFICATION_TYPES = [const.TYPES.DATES_REMINDER]
    creators.create_notifications_for_person_review_changes()

    assert post_office_models.Email.objects.count() == 0


def test_expected_queries_count(test_data):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        creators.create_notifications_for_review_reminders()


def test_context_is_ok(test_data):
    review = test_data.review
    chain = test_data.person_reviews[0].roles.filter(
        type=core_const.ROLE.PERSON_REVIEW.REVIEWER
    ).order_by('position')
    first_reviewer_role = chain[0]
    second_reviewer_role = chain[1]

    creators.create_notifications_for_review_reminders()

    first_reviewer_email = post_office_models.Email.objects.get(
        context__contains=first_reviewer_role.person.login
    )
    second_reviewer_email = post_office_models.Email.objects.get(
        context__contains=second_reviewer_role.person.login
    )

    helpers.assert_is_substructure(
        {
            'review': {
                'id': review.id,
                'name': review.name,
                'finish_submission_date': review.finish_submission_date.isoformat(),
                'finish_approval_date': review.finish_approval_date,
            },
            'wait_evaluation_count': 1,
            'wait_approval_count': 1,
        },
        first_reviewer_email.context,
    )
    helpers.assert_is_substructure(
        {
            'review': {
                'id': review.id,
                'name': review.name,
                'finish_submission_date': review.finish_submission_date.isoformat(),
                'finish_approval_date': review.finish_approval_date,
            },
            'wait_evaluation_count': 0,
            'wait_approval_count': 1,
        },
        second_reviewer_email.context,
    )


def test_content_is_ok(test_data):
    language = test_data.language
    review = test_data.review
    chain = test_data.person_reviews[0].roles.filter(
        type=core_const.ROLE.PERSON_REVIEW.REVIEWER
    ).order_by('position')
    first_reviewer = chain[0].person

    creators.create_notifications_for_review_reminders()

    email = post_office_models.Email.objects.get(
        context__contains=first_reviewer.login
    )

    if language == 'ru':
        helpers.assert_all_strings_in_text(
            text=email.html_message,
            strings=(
                'ожидает',
                # 'января',
            )
        )
    else:
        helpers.assert_all_strings_in_text(
            text=email.html_message,
            strings=(
                'waiting',
                # 'january',
            )
        )
    helpers.assert_all_strings_in_text(
        text=email.subject,
        strings=(
            review.name,
        )
    )
    helpers.assert_all_strings_in_html(
        text=email.html_message,
        strings=(
            '/reviews/%s?action_at=%s' % (
                review.id,
                first_reviewer.login,
            ),
            'status=',
            first_reviewer.work_email,
        )
    )


def test_reminder_sent_to_current_reviewer(
    person_review,
    person_review_role_builder,
):
    review = person_review.review
    review.notify_reminder_date_from = datetimes.today()
    review.save()
    reviewers = [
        person_review_role_builder(
            type=type,
            position=level,
            person_review=person_review,
        )
        for type, level in (
            (core_const.ROLE.PERSON_REVIEW.TOP_REVIEWER, 1),
            (core_const.ROLE.PERSON_REVIEW.REVIEWER, 0),
            (core_const.ROLE.PERSON_REVIEW.TOP_REVIEWER, 1),
        )
    ]
    creators.create_notifications_for_review_reminders()
    email = post_office_models.Email.objects.first()
    assert reviewers[1].person.login == email.context['receiver_login']


@pytest.fixture(
    params=[
        {'language': 'ru'},
        {'language': 'en'},
    ]
)
def test_data(
    request,
    case_in_progress_review_with_everybody,
):
    review = case_in_progress_review_with_everybody.review
    helpers.update_model(
        review,
        finish_submission_date=date(2018, 1, 1),
        finish_approval_date=None,
    )
    person_reviews = case_in_progress_review_with_everybody.person_reviews

    language = request.param['language']
    for role in case_in_progress_review_with_everybody.roles:
        helpers.update_model(
            role.person,
            language=language,
        )

    today = datetimes.today()
    review = helpers.update_model(
        review,
        notify_reminder_date_from=today,
    )

    return stub(
        language=language,
        review=review,
        person_reviews=person_reviews,
    )
