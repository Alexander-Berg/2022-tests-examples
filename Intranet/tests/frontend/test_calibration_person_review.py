import datetime
from functools import partial

import pytest

from review.core import const
from review.lib import datetimes
from tests import helpers


@pytest.mark.parametrize(
    'role_type', [const.ROLE.CALIBRATION.ADMIN,
                  const.ROLE.CALIBRATION.CALIBRATOR]
)
def test_get_list(
    client,
    calibration_situation,
    calibration_role_builder,
    role_type,
):
    calibration = calibration_situation['calibration']
    cur_person = calibration_role_builder(
        calibration=calibration,
        type=role_type,
    ).person
    response = helpers.get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=cur_person.login,
    )
    received = {cpr['id'] for cpr in response['calibration_person_reviews']}
    expected = {cpr.id for cpr in calibration_situation['calibration_person_reviews']}
    assert received == expected


def test_get_draft_list_admin(
    client,
    calibration_situation,
    calibration_role_builder,
):
    calibration = calibration_situation['calibration']
    calibration.status = const.CALIBRATION_STATUS.DRAFT
    calibration.save()

    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    response = helpers.get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=admin.login,
    )
    received = {cpr['id'] for cpr in response['calibration_person_reviews']}
    expected = {cpr.id for cpr in calibration_situation['calibration_person_reviews']}
    assert received == expected


def test_get_list_draft_non_available(
    client,
    calibration_situation,
    calibration_role_builder,
):
    calibration = calibration_situation['calibration']
    calibration.status = const.CALIBRATION_STATUS.DRAFT
    calibration.save()

    cur_person = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    ).person

    response = helpers.get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=cur_person.login,
        expect_status=403,
    )
    assert response['errors']['*']['code'] == 'PERMISSION_DENIED'


@pytest.mark.parametrize(
    'role_type', [const.ROLE.CALIBRATION.ADMIN,
                  const.ROLE.CALIBRATION.CALIBRATOR]
)
def test_get_previous_person_reviews(
    client,
    calibration_role_builder,
    calibration_situation,
    role_type,
):
    calibration = calibration_situation['calibration']
    available_ids = {pr.id for pr in calibration_situation['visible_person_reviews']}
    available_ids |= {
        cpr.person_review.id
        for cpr in calibration_situation['calibration_person_reviews']
    }
    unavailable_ids = {pr.id for pr in calibration_situation['invisible_person_reviews']}
    cur_person = calibration_role_builder(
        calibration=calibration,
        type=role_type,
    ).person
    for person in calibration_situation['persons']:
        response = helpers.get_json(
            client,
            path='/frontend/calibration-person-reviews/history/?person={}'.format(person.login),
            login=cur_person.login,
        )
        person_reviews = response['person_reviews']
        received_ids = {it['id'] for it in person_reviews}

        # + current and last mid-review
        expecting_len = const.CALIBRATION_INHERITANCE_LENGTH + 2

        assert len(person_reviews) == expecting_len
        assert all(id_ in available_ids for id_ in received_ids)
        assert not any(id_ in received_ids for id_ in unavailable_ids)


FIELDS_TO_READ = (
    'mark',
    'goldstar',
    'level',
    'salary',
    'status',
    'flagged',
    'flagged_positive',
    'level_change',
    'goals_url',
    'st_goals_url',
    'action_at',
)


@pytest.mark.parametrize(
    'role_type', [const.ROLE.CALIBRATION.ADMIN,
                  const.ROLE.CALIBRATION.CALIBRATOR]
)
def test_access_to_fields_history(
    client,
    calibration_situation,
    calibration_role_builder,
    role_type,
):
    calibration = calibration_situation['calibration']
    cur_person = calibration_role_builder(
        calibration=calibration,
        type=role_type,
    ).person
    person = calibration_situation['persons'][0]
    response = helpers.get_json(
        client,
        path='/frontend/calibration-person-reviews/history/?person={}'.format(person.login),
        login=cur_person.login,
    )
    for person_review in response['person_reviews']:
        assert all(
            person_review[field] != const.NO_ACCESS
            for field in FIELDS_TO_READ
        )


def test_access_to_gradient_action(
    client,
    calibration_situation,
    review_role_builder,
    main_product_review_builder,
):
    cur_person = review_role_builder(
        review=calibration_situation['cur_review'],
        type=const.ROLE.REVIEW.ACCOMPANYING_HR,
    ).person

    main_product_review_builder(review=calibration_situation['cur_review'])
    person = calibration_situation['persons'][0]
    response = helpers.get_json(
        client,
        path='/frontend/calibration-person-reviews/history/?person={}'.format(person.login),
        login=cur_person.login,
    )

    for person_review in response['person_reviews']:
        assert person_review['actions']['umbrella'] == const.OK
        assert person_review['actions']['main_product'] == const.OK


@pytest.mark.parametrize(
    'role_type', [const.ROLE.CALIBRATION.ADMIN,
                  const.ROLE.CALIBRATION.CALIBRATOR]
)
def test_access_to_mark_level_history(
    client,
    calibration_situation,
    calibration_role_builder,
    role_type,
):
    # history shows only for the last year
    review = calibration_situation['cur_review']
    review.start_date = datetimes.shifted(review.start_date, years=-1)
    review.finish_date = datetimes.shifted(review.finish_date, years=-1)
    review.save()

    calibration = calibration_situation['calibration']
    cur_person = calibration_role_builder(
        calibration=calibration,
        type=role_type,
    ).person
    response = helpers.get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=cur_person.login,
    )
    assert all(
        cpr['person_review']['mark_level_history']
        for cpr in response['calibration_person_reviews']
    )


@pytest.mark.parametrize(
    'role_type', [const.ROLE.CALIBRATION.ADMIN,
                  const.ROLE.CALIBRATION.CALIBRATOR]
)
def test_access_to_fields_mode_calibration(
    client,
    calibration_situation,
    calibration_role_builder,
    role_type,
):
    calibration = calibration_situation['calibration']
    cur_person = calibration_role_builder(
        calibration=calibration,
        type=role_type,
    ).person
    response = helpers.get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=cur_person.login,
    )
    for cpr in response['calibration_person_reviews']:
        assert all(
            cpr['person_review'][field] != const.NO_ACCESS
            for field in FIELDS_TO_READ
        )


@pytest.mark.parametrize(
    'role_type', [const.ROLE.CALIBRATION.ADMIN,
                  const.ROLE.CALIBRATION.CALIBRATOR]
)
def test_access_to_person_fields_mode_calibration(
    client,
    calibration_situation,
    calibration_role_builder,
    role_type,
):
    calibration = calibration_situation['calibration']
    cur_person = calibration_role_builder(
        calibration=calibration,
        type=role_type,
    ).person
    response = helpers.get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=cur_person.login,
    )
    for cpr in response['calibration_person_reviews']:
        person = cpr['person_review']['person']
        assert all(
            person[field] != const.NO_ACCESS
            for field in [
                'last_name',
                'first_name',
                'department_chain_slugs',
                'gender',
                'department_chain_names',
                'chief',
                'department_slug',
                'position',
                'is_dismissed',
                'id',
                'login',
            ]
        )


def test_write_rights_admin(
    client,
    calibration_situation,
    calibration_role_builder,
):
    calibration = calibration_situation['calibration']
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    response = helpers.get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=admin.login,
    )
    expected_actions = {
        'comment',
        'mark',
        'level_change',
        'goldstar',
        'flagged',
    }
    cpr = response['calibration_person_reviews'][0]
    actions = cpr['person_review']['actions']
    assert all(actions[act] == const.OK for act in expected_actions)


def test_admin_can_edit_comment(
    client,
    calibration_situation,
    calibration_role_builder,
    person_review_comment_builder,
):
    calibration_person_review = calibration_situation['calibration_person_reviews'][0]
    admin = calibration_role_builder(
        calibration=calibration_person_review.calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    person_review = calibration_person_review.person_review
    comment = person_review_comment_builder(
        person_review=person_review,
        subject=admin,
    )
    new_text = 'this is CALIBRATION'
    modify_resp = helpers.post_json(
        client=client,
        login=admin.login,
        request={
            'text': new_text,
        },
        path='/frontend/person-reviews/{}/comments/{}/'.format(
            calibration_person_review.person_review.id,
            comment.id,
        ),
    )
    assert modify_resp['comment']['text'] == new_text
    read_resp = helpers.get_json(
        client=client,
        login=admin.login,
        path='/frontend/person-reviews/{}/comments/'.format(
            calibration_person_review.person_review.id,
        ),
    )
    resp_comment = next(cmt for cmt in read_resp['comments'] if cmt['id'] == comment.id)
    assert resp_comment['text'] == new_text, resp_comment


def test_actions_calibrator(
    client,
    calibration_situation,
    calibration_role_builder,
):
    calibration = calibration_situation['calibration']
    calibrator = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    ).person
    response = helpers.get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=calibrator.login,
    )
    cpr = response['calibration_person_reviews'][0]
    actions = cpr['person_review']['actions']
    assert actions.pop('flagged') and actions.pop('flagged_positive')
    assert all(act == const.NO_ACCESS for act in list(actions.values())), actions


def test_archived_as_calibrator(
    client,
    calibration_situation,
    calibration_role_builder,
):
    calibration = calibration_situation['calibration']
    calibration.status = const.CALIBRATION_STATUS.ARCHIVE
    calibration.save()
    calibrator = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    ).person
    person = calibration_situation['persons'][0]
    response = helpers.get_json(
        client,
        path='/frontend/calibration-person-reviews/history/?person={}'.format(person.login),
        login=calibrator.login,
    )
    person_review = response['person_reviews'][0]
    visible_fields = (
        'id',
        'profession',
        'goals_url',
        'st_goals_url',
        'feedback_url',
        'review',
        'umbrella',
        'main_product',
        'product_schema_loaded',
    )

    def expecting_empty():
        for key, val in person_review.items():
            if key in visible_fields:
                continue
            if isinstance(val, dict):
                for inner_key, inner_val in val.items():
                    yield inner_val
            else:
                yield val
    assert all(received in (const.NO_ACCESS, []) for received in expecting_empty())


@pytest.fixture
def calibration_situation(
    calibration_builder,
    calibration_person_review_builder,
    review_builder,
    person_builder,
    person_review_builder,
):
    start_date = datetime.date(2017, 1, 1)
    finish_date = datetimes.shifted(start_date, months=1)
    manual = const.REVIEW_MODE.MODE_MANUAL
    create_review_all_included = partial(
        review_builder,
        goldstar_mode=manual,
        level_change_mode=manual,
        salary_change_mode=manual,
        options_rsu_mode=manual,
        bonus_mode=manual,
        deferred_payment_mode=manual,
    )
    prev_reviews = [
        create_review_all_included(
            start_date=datetimes.shifted(start_date, months=2 * pos),
            finish_date=datetimes.shifted(finish_date, months=2 * pos),
            status=const.REVIEW_STATUS.ARCHIVE,
        )
        for pos in range(7)
    ]
    persons = [person_builder() for _ in range(5)]
    for middle in (prev_reviews[-2:]):
        middle.type = const.REVIEW_TYPE.MIDDLE
        middle.save()
    invisible_reviews = prev_reviews[0], prev_reviews[-2]
    visible_offset = const.CALIBRATION_INHERITANCE_LENGTH + 2
    visible_reviews = prev_reviews[-visible_offset:-2] + [prev_reviews[-1]]
    visible_person_reviews = []
    for review in visible_reviews:
        visible_person_reviews += [
            person_review_builder(review=review, person=person)
            for person in persons
        ]
    invisible_person_reviews = []
    for review in invisible_reviews:
        invisible_person_reviews += [
            person_review_builder(review=review, person=person)
            for person in persons
        ]
    cur_review = create_review_all_included(
        start_date=datetimes.shifted(start_date, years=2),
        finish_date=datetimes.shifted(finish_date, years=2),
        status=const.REVIEW_STATUS.IN_PROGRESS,
    )
    cur_person_reviews = [
        person_review_builder(review=cur_review, person=person)
        for person in persons
    ]
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    calibration_person_reviews = [
        calibration_person_review_builder(
            person_review=pr,
            calibration=calibration,
        )
        for pr in cur_person_reviews
    ]
    return dict(
        invisible_reviews=invisible_reviews,
        visible_reviews=visible_reviews,
        visible_person_reviews=visible_person_reviews,
        invisible_person_reviews=invisible_person_reviews,
        cur_review=cur_review,
        cur_person_reviews=cur_person_reviews,
        calibration=calibration,
        calibration_person_reviews=calibration_person_reviews,
        persons=persons,
    )
