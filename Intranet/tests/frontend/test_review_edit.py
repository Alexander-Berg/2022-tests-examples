import csv
import datetime

import pytest

from review.lib import datetimes
from review.shortcuts import models
from review.shortcuts import const
from tests import helpers

pytestmark = pytest.mark.usefixtures("test_person_as_review_creator")


START_DATE = datetime.date(2000, 3, 7)
FINISH_DATE = datetime.date(2000, 4, 5)
DUMMY_DATE = datetime.date(2000, 4, 4)

MANUAL = const.REVIEW_MODE.MODE_MANUAL
MANUAL_VERBOSE = const.REVIEW_MODE.VERBOSE[MANUAL]
TYPE_MIDDLE = const.REVIEW_TYPE.MIDDLE
TYPE_MIDDLE_VERBOSE = const.REVIEW_TYPE.VERBOSE[TYPE_MIDDLE]
TYPE_BONUS = const.REVIEW_TYPE.BONUS
TYPE_BONUS_VERBOSE = const.REVIEW_TYPE.VERBOSE[TYPE_BONUS]
REVIEW_NAME = 'Some review'
OPTIONS_RSU_UNIT = const.REVIEW_OPTIONS_RSU_UNIT


FORM_PARAMS = {
    'name': REVIEW_NAME,
    'type': TYPE_MIDDLE_VERBOSE,
    'start_date': START_DATE.isoformat(),
    'finish_date': FINISH_DATE.isoformat(),
    'mark_mode': const.REVIEW_MODE.FORM_MARK_MODE_GOLDSTAR_RESTRICTED,
    'level_change_mode': MANUAL_VERBOSE,
    'salary_change_mode': MANUAL_VERBOSE,
    'bonus_mode': MANUAL_VERBOSE,
    'deferred_payment_mode': MANUAL_VERBOSE,
    'options_rsu_mode': MANUAL_VERBOSE,
    'notify_reminder_type': const.REVIEW_NOTIFICATION_SETTINGS.NO,
}


def test_valid_review_create_only_required(
    client,
    person_builder,
    test_person,
):
    response = helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/',
        request=FORM_PARAMS,
    )

    id = int(response['id'])
    review = helpers.fetch_model(id, cls=models.Review)

    assert review.name == REVIEW_NAME
    assert review.type == TYPE_MIDDLE

    assert review.start_date == START_DATE
    assert review.finish_date == FINISH_DATE

    assert review.feedback_to_date == START_DATE
    assert review.feedback_from_date == datetimes.shifted(START_DATE, months=-6)
    assert review.mark_mode == MANUAL
    assert review.goldstar_mode == const.REVIEW_MODE.MODE_MANUAL_BY_CHOSEN
    assert review.level_change_mode == MANUAL
    assert review.salary_change_mode == MANUAL
    assert review.bonus_mode == MANUAL
    assert review.options_rsu_mode == MANUAL
    assert review.deferred_payment_mode == MANUAL
    assert review.notify_reminder_days == ''

    assert models.ReviewRole.objects.filter(
        review=review,
        type=const.ROLE.REVIEW.ACCOMPANYING_HR,
        person__login=test_person.login,
    ).exists()


@pytest.mark.parametrize(
    'review_type, expected_bonus_mode',
    [
        (TYPE_BONUS_VERBOSE, const.REVIEW_MODE.MODE_MANUAL),
        (TYPE_MIDDLE_VERBOSE, const.REVIEW_MODE.MODE_AUTO),
    ]
)
def test_review_manual_bonus_mode_if_bonus_type(
    client,
    review_type,
    expected_bonus_mode,
):
    form_params = FORM_PARAMS.copy()
    form_params['bonus_mode'] = const.REVIEW_MODE.VERBOSE[const.REVIEW_MODE.MODE_AUTO]
    form_params['type'] = review_type

    response = helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/',
        request=form_params,
    )

    pk = int(response['id'])
    review = helpers.fetch_model(pk, cls=models.Review)

    assert review.bonus_mode == expected_bonus_mode


def test_review_create_with_scale(
    client,
    marks_scale,
    person_builder,
):
    params = {
        'admins': [person_builder().login],
        'scale': marks_scale.id,
    }
    params.update(FORM_PARAMS)

    response = helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/',
        request=params,
    )
    review = helpers.fetch_model(response['id'], cls=models.Review)
    assert review.scale_id == marks_scale.id


def test_review_create_with_goodies(
    client,
    marks_scale,
    person_builder,
    review_bonus_file,
):
    params = {
        'admins': [person_builder().login],
        'goodies': review_bonus_file,
    }
    params.update(FORM_PARAMS)

    response = helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/',
        request=params,
    )

    review = helpers.fetch_model(response['id'], cls=models.Review)
    goodies = list(models.Goodie.objects.filter(review=review).values())
    review_bonus_file.seek(0)
    bonus_data = [bonus for bonus in csv.DictReader(review_bonus_file)]

    for goodie in goodies:
        for bonus in bonus_data:
            if goodie['mark'] == bonus['mark'] and int(goodie['level']) == int(bonus['level']):
                assert float(goodie['bonus']) == float(bonus['bonus'])
                assert float(goodie['options_rsu']) == float(bonus['options_rsu'])
                break
        else:
            assert False, 'No such bonus {}'.format(goodie)


UNREQUIRED_FORM_PARAMS = [
    # field, value convertable to json, value in db
    ('finish_feedback_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('finish_submission_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('finish_calibration_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('finish_approval_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('evaluation_from_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('evaluation_to_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('feedback_from_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('feedback_to_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('salary_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('include_dismissed_after_date', DUMMY_DATE.isoformat(), DUMMY_DATE),
    ('options_rsu_unit', OPTIONS_RSU_UNIT.VERBOSE[OPTIONS_RSU_UNIT.USD], OPTIONS_RSU_UNIT.USD),
    ('notify_events_other', True, True),
    ('notify_events_superreviewer', True, True),
    (
        'bonus_type',
        const.REVIEW_BONUS_TYPE.VERBOSE[const.REVIEW_BONUS_TYPE.QUARTERLY],
        const.REVIEW_BONUS_TYPE.QUARTERLY,
    ),
    ('bonus_reason', 'Why not?', 'Why not?'),
]


@pytest.mark.parametrize(
    'field, value_json, value_db', UNREQUIRED_FORM_PARAMS
)
def test_review_creation_unrequired_params(
    client,
    person_builder,
    field,
    value_json,
    value_db,
):
    params = {
        'admins': [person_builder().login],
        field: value_json
    }
    params.update(FORM_PARAMS)

    response = helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/',
        request=params,
    )

    review = helpers.fetch_model(response['id'], cls=models.Review)
    assert getattr(review, field) == value_db


VALID_NOTIFICATIONS_PARAMS = [
    (
        {
            'notify_reminder_type': const.REVIEW_NOTIFICATION_SETTINGS.NO,
        },
        {
            'notify_reminder_days': '',
            'notify_reminder_date_from': None,
            'notify_reminder_date_to': None,
        }
    ),
    (
        {
            'notify_reminder_type': const.REVIEW_NOTIFICATION_SETTINGS.ONCE,
            'notify_reminder_date_from': DUMMY_DATE.isoformat(),
            'notify_reminder_date_to': '',
        },
        {
            'notify_reminder_days': '',
            'notify_reminder_date_from': DUMMY_DATE,
            'notify_reminder_date_to': None,
        }
    ),
    (
        {
            'notify_reminder_type': const.REVIEW_NOTIFICATION_SETTINGS.PERIOD,
            'notify_reminder_days': ['mo', 'tu'],
            'notify_reminder_date_from': DUMMY_DATE.isoformat(),
            'notify_reminder_date_to': DUMMY_DATE.isoformat(),
        },
        {
            'notify_reminder_days': 'mo,tu',
            'notify_reminder_date_from': DUMMY_DATE,
            'notify_reminder_date_to': DUMMY_DATE,
        }
    ),
]


@pytest.mark.parametrize('input,expected', VALID_NOTIFICATIONS_PARAMS)
def test_valid_notifications_params(input, expected, client, person_builder):
    params = {
        'admins': [person_builder().login],
    }
    params.update({
        key: value for key, value in FORM_PARAMS.items()
        if not key.startswith('notify_')
    })
    params.update(input)

    response = helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/',
        request=params,
    )

    pk = int(response['id'])
    review = helpers.fetch_model(pk, cls=models.Review)

    for key, value in expected.items():
        assert getattr(review, key) == value


def test_review_edit_scale(
    client,
    marks_scale_builder,
    review_builder,
    review_role_builder,
):
    review = review_builder()
    admin = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
    ).person
    new_scale = marks_scale_builder(scale=dict(A=1))
    params = dict(FORM_PARAMS)
    params.update(dict(
        scale=new_scale.id,
        admins=[admin.login],
    ))
    helpers.post_json(
        client=client,
        path='/frontend/reviews/{}/'.format(review.id),
        request=params,
        login=admin.login,
    )
    review_from_api = helpers.get_json(
        client=client,
        path='/frontend/reviews/{}/'.format(review.id),
        login=admin.login,
    )
    assert review_from_api['scale_id'] == new_scale.id


def test_review_edit_by_admin(
    client,
    test_person,
    review_builder,
    review_role_builder,
    person_builder,
    review_bonus_file,
):
    review = review_builder(
        start_date=FORM_PARAMS['start_date'],
        finish_date=FORM_PARAMS['finish_date'],
        author=test_person,
    )
    admin_role = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
    )

    start_date = datetimes.shifted(FORM_PARAMS['start_date'], years=-1)
    finish_date = datetimes.shifted(FORM_PARAMS['finish_date'], years=+1)
    include_dismissed_after_date = datetimes.shifted(FORM_PARAMS['start_date'], years=-2)
    new_superreviewer = person_builder()

    params = dict(FORM_PARAMS)
    params.update(
        start_date=start_date.isoformat(),
        finish_date=finish_date.isoformat(),
        include_dismissed_after_date=include_dismissed_after_date.isoformat(),
        goodies=review_bonus_file,
        admins=[admin_role.person.login],
        super_reviewers=[new_superreviewer.login],
    )

    helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/{}/'.format(review.id),
        request=params,
        login=admin_role.person.login,
    )
    review = helpers.fetch_model(review)

    assert review.start_date == start_date
    assert review.finish_date == finish_date
    assert review.include_dismissed_after_date == include_dismissed_after_date
    assert [new_superreviewer.id] == list(review.roles.filter(
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    ).values_list('person_id', flat=True))


def test_review_edit_superreviewers_by_accompanying_hr(
    client,
    test_person,
    review_builder,
    review_role_builder,
    person_builder,
):
    review = review_builder(
        start_date=FORM_PARAMS['start_date'],
        finish_date=FORM_PARAMS['finish_date'],
        author=test_person,
    )
    hr_role = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ACCOMPANYING_HR,
    )
    new_superreviewer = person_builder()

    params = dict(FORM_PARAMS)
    params.update(
        super_reviewers=[new_superreviewer.login],
    )

    helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/{}/'.format(review.id),
        request=params,
        login=hr_role.person.login,
    )
    review = helpers.fetch_model(review)

    assert (
        review.roles
        .filter(
            type=const.ROLE.REVIEW.SUPERREVIEWER,
            person_id=new_superreviewer.id
        )
        .exists()
    )


def test_review_edit_by_author(
    client,
    person_builder,
    review_builder,
    review_role_builder,
    test_person,
):
    resp = helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/',
        request=FORM_PARAMS,
        login=test_person.login,
    )
    review_id = resp['id']

    params = dict(FORM_PARAMS)
    new_start_date = datetimes.shifted(params['start_date'], years=-1)
    new_hr = person_builder()
    params.update(
        start_date=new_start_date.isoformat(),
        accompanying_hrs=[new_hr.login],
    )

    helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/{}/'.format(review_id),
        request=params,
        login=test_person.login,
    )

    db_start_date = models.Review.objects.get(id=review_id).start_date
    assert db_start_date == new_start_date
    assert (
        models.ReviewRole.objects
        .filter(type=const.ROLE.REVIEW.ACCOMPANYING_HR, person=new_hr)
        .exists()
    )


@pytest.mark.parametrize('to_remove', ['admin', 'superreviewer'])
def test_review_delete_admin_and_superreviewer_same_person(
    client,
    person,
    review_builder,
    review_role_builder,
    to_remove,
):
    review = review_builder(
        start_date=FORM_PARAMS['start_date'],
        finish_date=FORM_PARAMS['finish_date'],
        author=person,
    )
    admin_role = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=person,
    )
    superreviewer_role = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
        person=person,
    )
    # at least one admin have to stay alive
    immortal_admin_role = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
    )

    params = dict(FORM_PARAMS)
    if to_remove == 'admin':
        params.update(
            admins=[immortal_admin_role.person.login],
            super_reviewers=[person.login],
        )
    else:
        params.update(
            admins=[immortal_admin_role.person.login, person.login],
            super_reviewers=[],
        )

    helpers.post_multipart_data(
        client=client,
        path='/frontend/reviews/{}/'.format(review.id),
        request=params,
        login=person.login,
    )
    expecting_left_roles = {
        immortal_admin_role,
        superreviewer_role if to_remove == 'admin' else admin_role,
    }
    left_roles = set(models.ReviewRole.objects.filter(review=review))
    assert expecting_left_roles == left_roles


def test_review_get(person_builder, client):
    start_date = datetime.date(2000, 3, 7)
    finish_date = datetime.date(2000, 4, 5)
    dummy_date = datetime.date(2000, 4, 4)
    bonus_type = const.REVIEW_BONUS_TYPE.VERBOSE[const.REVIEW_BONUS_TYPE.QUARTERLY]

    args = {
        'name': 'test review',
        'type': TYPE_MIDDLE_VERBOSE,
        'bonus_type': bonus_type,
        'bonus_reason': 'Why not?',

        'start_date': start_date.isoformat(),
        'finish_date': finish_date.isoformat(),

        'finish_feedback_date': dummy_date.isoformat(),
        'finish_submission_date': dummy_date.isoformat(),
        'finish_calibration_date': dummy_date.isoformat(),
        'finish_approval_date': dummy_date.isoformat(),
        'include_dismissed_after_date': dummy_date.isoformat(),
        'feedback_from_date': dummy_date.isoformat(),
        'feedback_to_date': dummy_date.isoformat(),

        'mark_mode': const.REVIEW_MODE.FORM_MARK_MODE_GOLDSTAR_RESTRICTED,
        'level_change_mode': MANUAL_VERBOSE,
        'salary_change_mode': MANUAL_VERBOSE,
        'bonus_mode': MANUAL_VERBOSE,
        'options_rsu_mode': MANUAL_VERBOSE,
        'deferred_payment_mode': MANUAL_VERBOSE,
        'options_rsu_unit': OPTIONS_RSU_UNIT.VERBOSE[OPTIONS_RSU_UNIT.USD],

        'notify_reminder_type': 'no',
        'notify_reminder_days': [],
    }
    response = helpers.post_multipart_data(client, '/frontend/reviews/', args)
    created_review_id = response['id']
    response = helpers.get_json(
        client, '/frontend/reviews/{}/'.format(created_review_id),
    )
    for key, value in args.items():
        if key == 'login':
            continue
        if key in ('admins', 'super_reviewers'):
            assert [it['login'] for it in response[key]] == value
            continue
        assert response[key] == value
