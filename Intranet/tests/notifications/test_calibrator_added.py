# coding: utf-8


import pytest
from post_office import models as post_office_models

from review.core import const as core_const
from review.notifications import creators
from review.notifications import const
from review.shortcuts import models
from tests import helpers


COLLECT_INFO_FOR_NOTIFYING_NUM_QUERIES = 3
SAVE_EMAIL_NUM_QUERIES = 1
MARK_NOTIFICED_NUM_QUERIES = 1
TRANSACTION_NUM_QUERIES = 2
EXPECTED_NUM_QUERIES = sum([
    COLLECT_INFO_FOR_NOTIFYING_NUM_QUERIES,
    SAVE_EMAIL_NUM_QUERIES,
    MARK_NOTIFICED_NUM_QUERIES,
    TRANSACTION_NUM_QUERIES,
])


def test_created(test_data):
    creators.create_notification_for_added_calibrators()

    assert post_office_models.Email.objects.count() == 1
    assert not models.CalibrationRole.objects.filter(
        notified=False,
        type=core_const.ROLE.CALIBRATION.CALIBRATOR,
    ).exists()


def test_disableable(test_data, settings):
    settings.DISABLE_NOTIFICATION_TYPES = [const.TYPES.CALIBRATORS_ADDED]
    creators.create_notifications_for_person_review_changes()

    assert post_office_models.Email.objects.count() == 0
    assert not models.PersonReviewChange.objects.filter(notified=False).exists()


def test_expected_queries_count(test_data):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        creators.create_notification_for_added_calibrators()


def test_notifications_disabled(test_data):
    calibration = test_data['calibration']
    calibration.notify_users = False
    calibration.save()
    creators.create_notification_for_added_calibrators()
    calibration.notify_users = True
    calibration.save()
    creators.create_notification_for_added_calibrators()
    assert not post_office_models.Email.objects.exists()


def test_context_is_ok(test_data):
    language = test_data['language']
    calibration = test_data['calibration']
    admins = test_data['admins']
    creators.create_notification_for_added_calibrators()

    email = post_office_models.Email.objects.first()

    def get_localized(obj, field):
        return getattr(obj, '{}_{}'.format(field, language))

    helpers.assert_is_substructure(
        dict(
            calibration=dict(
                id=calibration.id,
                name=calibration.name,
                start_date=calibration.start_date,
                finish_date=calibration.finish_date,
            ),
            admins=[
                dict(
                    login=admin.login,
                    first_name=get_localized(admin, 'first_name'),
                    last_name=get_localized(admin, 'last_name')
                )
                for admin in admins
            ],
            language=language,
        ),
        email.context,
    )


def test_content_is_ok(test_data):
    language = test_data['language']
    calibration = test_data['calibration']
    admins = test_data['admins']
    creators.create_notification_for_added_calibrators()

    email = post_office_models.Email.objects.first()
    expected_strings = [
        'Администраторы' if language == 'ru' else 'Administrators',
        calibration.name,
        '/calibrations/{}'.format(calibration.id),
    ]

    for field in ('first_name', 'last_name'):
        expected_strings += [
            getattr(it, '{}_{}'.format(field, language))
            for it in admins
        ]
    expected_strings += [it.login for it in admins]

    helpers.assert_all_strings_in_text(
        text=email.html_message,
        strings=expected_strings,
        ignore_html_tags=False,
    )

    subject_form = 'Калибровка «{}»' if language == 'ru' else 'Calibration "{}"'
    helpers.assert_all_strings_in_text(
        text=email.subject,
        strings=(subject_form.format(calibration.name), )
    )


@pytest.fixture(
    params=[
        {'language': 'ru'},
        {'language': 'en'},
    ]
)
def test_data(
    request,
    calibration_builder,
    calibration_role_builder,
):
    language = request.param['language']
    calibration = calibration_builder(status=core_const.CALIBRATION_STATUS.IN_PROGRESS)
    admins = [
        calibration_role_builder(
            type=core_const.ROLE.CALIBRATION.ADMIN,
            calibration=calibration,
        ).person
        for _ in range(2)
    ]
    calibrator = calibration_role_builder(
        type=core_const.ROLE.CALIBRATION.CALIBRATOR,
        calibration=calibration,
    ).person
    helpers.update_model(calibrator, language=language)
    return dict(
        language=language,
        calibration=calibration,
        admins=admins,
    )
