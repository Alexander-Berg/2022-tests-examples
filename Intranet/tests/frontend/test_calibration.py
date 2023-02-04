# coding=utf-8
import datetime
from collections import defaultdict
from itertools import chain

import pytest

from review.core import const
from review.core import models
from review.core.logic import calibration_rights
from tests.helpers import (
    delete_multipart,
    get_json,
    post_json,
)


def test_calibration_create(
    client,
    global_role_builder,
    test_person,
    person_builder,
    person_review_role_builder,
    person_review_builder,
):
    person_reviews = [person_review_builder().id for _ in range(3)]
    for pr in person_reviews:
        person_review_role_builder(
            person_review_id=pr,
            person=test_person,
            type=const.ROLE.PERSON_REVIEW.REVIEWER,
        )
    start_date = datetime.date(2017, 1, 1)
    finish_date = datetime.date(2017, 12, 12)
    name = 'test_calibration'
    admin_logins = [person_builder().login for _ in range(2)]
    post_params = dict(
        person_reviews=person_reviews,
        start_date=start_date.isoformat(),
        finish_date=finish_date.isoformat(),
        name=name,
        admins=admin_logins,
    )
    global_role_builder(
        person=test_person,
        type=const.ROLE.GLOBAL.CALIBRATION_CREATOR,
    )
    result = post_json(
        client,
        path='/frontend/calibrations/',
        request=post_params,
        login=test_person.login,
    )
    created_all = models.Calibration.objects.filter(id=result['id'])
    created_admins = models.CalibrationRole.objects.filter(
        type=const.ROLE.CALIBRATION.ADMIN,
        person__login__in=admin_logins,
    )
    assert created_all.exists()
    created_single = created_all[0]
    assert created_single.start_date == start_date
    assert created_single.finish_date == finish_date
    assert created_single.name == name
    assert created_single.status == const.CALIBRATION_STATUS.DRAFT
    assert created_admins.count() == len(admin_logins)


def test_create_calibration_access_to_person_reviews(
    client,
    global_role_builder,
    person,
    person_review_builder,
    person_review_role_builder,
):
    person_review_id = person_review_builder().id
    person_review_role_builder(
        person_review_id=person_review_id,
        person=person,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
    )
    global_role_builder(
        person=person,
        type=const.ROLE.GLOBAL.CALIBRATION_CREATOR,
    )
    response = post_json(
        client,
        path='/frontend/calibrations/',
        request=dict(
            person_reviews=[person_review_id],
            start_date='2018-01-01',
            finish_date='2018-02-01',
            name='test',
        ),
        login=person.login,
    )
    calibration_id = response['id']
    post_json(
        client,
        path='/frontend/calibrations/{}/workflow/'.format(calibration_id),
        request=dict(
            workflow=calibration_rights.ACTIONS.STATUS_PUBLISH,
        ),
        login=person.login,
    )
    result = get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration_id),
        login=person.login,
    )
    cprs = result['calibration_person_reviews']
    assert len(cprs) == 1
    assert cprs[0]['person_review']['id'] == person_review_id


def test_calibration_create_no_permission(
        client,
        person,
        person_review,
):
    params = dict(
        person_reviews=[person_review.id],
        start_date=datetime.date(2018, 1, 1).isoformat(),
        finish_date=datetime.date(2018, 2, 1).isoformat(),
        name='test_calibration',
    )
    response = post_json(
        client,
        path='/frontend/calibrations/',
        request=params,
        login=person.login,
        expect_status=403,
    )
    assert 'errors' in response


def test_calibration_delete(
    client,
    calibration_builder,
    calibration_role_builder,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.DRAFT)
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    delete_multipart(
        client,
        path='/frontend/calibrations/{}/'.format(calibration.id),
        login=admin.login,
    )
    assert not models.Calibration.objects.filter(id=calibration.id).exists()


@pytest.mark.parametrize(
    'status', [
        const.CALIBRATION_STATUS.IN_PROGRESS,
        const.CALIBRATION_STATUS.ARCHIVE,
    ]
)
def test_calibration_cant_delete_not_draft(
    client,
    calibration_builder,
    calibration_role_builder,
    status,
):
    calibration = calibration_builder(status=status)
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    delete_multipart(
        client,
        path='/frontend/calibrations/{}/'.format(calibration.id),
        login=admin.login,
        expect_status=403,
    )
    assert models.Calibration.objects.filter(id=calibration.id).exists()


def test_calibration_delete_no_permission(
    client,
    calibration
):
    delete_multipart(
        client,
        path='/frontend/calibrations/{}/'.format(calibration.id),
        expect_status=403,
    )
    assert models.Calibration.objects.filter(id=calibration.id).exists()


def test_calibration_list(
    client,
    calibration_builder,
    calibration_role_builder,
    person,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    calibration_role_builder(
        person=person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    result = get_json(client, '/frontend/calibrations/', login=person.login)
    assert (
        len(result.get('calibrations', [])) == 1 and
        result['calibrations'][0].get('id') == calibration.id
    )


def test_calibration_suggest(
    client,
    calibration_builder,
    calibration_role_builder,
    calibration_person_review_builder,
    person,
    person_review_builder,
    review_builder,
):
    search_string = 'supercalibration'
    calibration_to_find = calibration_builder(name=search_string)
    cpr = calibration_person_review_builder(calibration=calibration_to_find)
    person_review_to_find = cpr.person_review
    calibration_to_find_by_review = calibration_builder(name='random')
    review = review_builder(name=search_string)
    calibration_person_review_builder(
        calibration=calibration_to_find_by_review,
        person_review=person_review_builder(review=review),
    )

    # calibrations to not find
    calibration_another_review = calibration_builder(name=search_string)
    calibration_person_review_builder(calibration=calibration_another_review)
    calibration_wrong_name = calibration_builder(name='not_interested')
    calibration_person_review_builder(
        person_review=person_review_to_find,
        calibration=calibration_wrong_name,
    )
    calibration_no_role = calibration_builder(name=search_string)
    calibration_person_review_builder(
        person_review=person_review_to_find,
        calibration=calibration_no_role,
    )

    to_create_role = [
        calibration_to_find,
        calibration_to_find_by_review,
        calibration_another_review,
        calibration_wrong_name,
    ]
    for calibration in to_create_role:
        calibration_role_builder(
            person=person,
            calibration=calibration,
            type=const.ROLE.CALIBRATION.ADMIN,
        )

    result = get_json(
        client,
        '/frontend/calibrations/suggest/',
        login=person.login,
        request={
            'reviews': [person_review_to_find.review_id, review.id],
            'text': search_string,
        },
    )
    res_calibrations = result.get('calibrations', [])
    assert len(res_calibrations) == 2

    ids_to_find = {calibration_to_find_by_review.id, calibration_to_find.id}
    assert {it['id'] for it in res_calibrations} == ids_to_find

    review = next(
        c['reviews'][0]
        for c in res_calibrations
        if c['reviews'][0]['id'] == person_review_to_find.review_id
    )
    assert review['name'] == person_review_to_find.review.name


@pytest.mark.parametrize(
    'person_role,filter_role,should_be_found',
    [
        (const.ROLE.CALIBRATION.ADMIN, const.ROLE.CALIBRATION.VERBOSE[const.ROLE.CALIBRATION.ADMIN], True),
        (const.ROLE.CALIBRATION.ADMIN, const.ROLE.CALIBRATION.VERBOSE[const.ROLE.CALIBRATION.CALIBRATOR], False),
        (const.ROLE.CALIBRATION.CALIBRATOR, const.ROLE.CALIBRATION.VERBOSE[const.ROLE.CALIBRATION.ADMIN], False),
        (const.ROLE.CALIBRATION.CALIBRATOR, const.ROLE.CALIBRATION.VERBOSE[const.ROLE.CALIBRATION.CALIBRATOR], True),
        (None, const.ROLE.CALIBRATION.VERBOSE[const.ROLE.CALIBRATION.ADMIN], False),
        (None, const.ROLE.CALIBRATION.VERBOSE[const.ROLE.CALIBRATION.CALIBRATOR], False),
        (None, None, False),
        (const.ROLE.CALIBRATION.ADMIN, None, True),
        (const.ROLE.CALIBRATION.CALIBRATOR, None, True),
    ]
)
def test_calibration_suggest_filter_by_role_type(
    client,
    calibration_builder,
    calibration_role_builder,
    calibration_person_review_builder,
    person,
    person_role,
    filter_role,
    should_be_found,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    calibration_person_review_builder(calibration=calibration)
    if person_role:
        calibration_role_builder(calibration=calibration, person=person, type=person_role)

    filters = {'role': filter_role}

    result = get_json(client, '/frontend/calibrations/suggest/', login=person.login, request=filters)
    assert bool(result['calibrations']) is should_be_found
    if should_be_found:
        assert result['calibrations'][0]['id'] == calibration.id


def test_calibrator_doesnt_see_drafts(
    client,
    calibration,
    calibration_role_builder,
    person,
):
    calibration_role_builder(
        person=person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR
    )
    result = get_json(client, '/frontend/calibrations/', login=person.login)
    assert len(result.get('calibrations', [])) == 0


def test_calibration_list_not_seen_by_creator_role(
    client,
    calibration,
    global_role_builder,
):
    creator = global_role_builder(
        calibration=calibration,
        type=const.ROLE.GLOBAL.CALIBRATION_CREATOR,
    ).person
    result = get_json(
        client,
        path='/frontend/calibrations/',
        login=creator.login,
    )
    assert not result.get('calibrations')


def test_calibration_list_with_author(
    client,
    calibration_builder,
    calibration_role_builder,
    person,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    calibration_role_builder(
        person=person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    result = get_json(
        client,
        path='/frontend/calibrations/?fields=author',
        login=person.login,
    )
    calibration = result['calibrations'][0]
    assert 'author' in calibration


def test_calibration_list_order(
    client,
    global_role_builder,
    calibration_builder,
    calibration_role_builder,
):
    creator = global_role_builder(type=const.ROLE.GLOBAL.CALIBRATION_CREATOR).person
    year = 2018
    calibrations = []
    for month in (1, 2):
        for i, status in enumerate(const.CALIBRATION_STATUS.ALL):
            cal = calibration_builder(
                author=creator,
                status=status,
                start_date=datetime.date(year - i, month, 1),
                finish_date=datetime.date(year - i, month, 2),
            )
            calibration_role_builder(
                calibration=cal,
                person=creator,
                type=const.ROLE.CALIBRATION.ADMIN,
            )
            calibrations.append(cal)
    status_order = (
        const.CALIBRATION_STATUS.DRAFT,
        const.CALIBRATION_STATUS.IN_PROGRESS,
        const.CALIBRATION_STATUS.ARCHIVE,
    )
    expecting_order = sorted(
        calibrations,
        key=lambda it: (
            status_order.index(it.status),
            -it.start_date.toordinal(),
        )
    )
    expecting_order_ids = [it.id for it in expecting_order]
    response = get_json(
        client,
        path='/frontend/calibrations/',
        login=creator.login,
    )
    received_ids = [it['id'] for it in response['calibrations']]
    assert expecting_order_ids == received_ids


def test_calibration_edit(
    client,
    calibration,
    calibration_editor_role_builder,
    person,
):
    calibration_editor_role_builder(
        person=person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    new_params = dict(
        name='renamed_calibration',
        finish_date='2018-11-01',
        start_date='2018-10-01',
    )
    post_json(
        client,
        path='/frontend/calibrations/{}/'.format(calibration.id),
        request=new_params,
        login=person.login,
    )
    calibration.refresh_from_db()
    assert all(
        str(getattr(calibration, key)) == value
        for key, value in new_params.items()
    )


def test_calibration_edit_archive_non_available(
    client,
    calibration,
    calibration_role_builder,
):
    calibration.status = const.CALIBRATION_STATUS.ARCHIVE
    calibration.save()
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    response = post_json(
        client,
        path='/frontend/calibrations/{}/'.format(calibration.id),
        request=dict(
            name='renamed_calibration',
            finish_date='2018-11-01',
            start_date='2018-10-01',
        ),
        login=admin.login,
        expect_status=403,
    )
    assert response['errors']['*']['code'] == 'PERMISSION_DENIED'


def test_calibration_edit_admins(
    client,
    calibration_builder,
    calibration_role_builder,
    person_builder,
):
    another_roles_ids = [
        calibration_role_builder(
            type=t,
            calibration=calibration_builder(),
        ).id
        for t in (const.ROLE.CALIBRATION.ADMIN, const.ROLE.CALIBRATION.CALIBRATOR)
    ]

    calibration = calibration_builder()
    admin = calibration_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    ).person
    logins_to_add = [person_builder().login for _ in range(2)]
    post_json(
        client,
        path='/frontend/calibrations/{}/'.format(calibration.id),
        request=dict(
            name=calibration.name,
            finish_date=calibration.finish_date,
            start_date=calibration.start_date,
            admins=logins_to_add,
        ),
        login=admin.login,
    )
    cur_admins = models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    assert cur_admins.count() == len(logins_to_add)
    another_roles_left = models.CalibrationRole.objects.filter(
        id__in=another_roles_ids
    ).count()
    assert another_roles_left == len(another_roles_ids)


def test_calibration_edit_no_access(person, calibration_role_builder, calibration, client):
    calibration_role_builder(
        person=person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    response = post_json(
        client,
        path='/frontend/calibrations/{}/'.format(calibration.id),
        request=dict(name='renamed_calibration'),
        login=person.login,
        expect_status=403,
    )
    assert 'errors' in response


def test_calibration_get(test_person, calibration_role_builder, calibration, client):
    calibration_role_builder(
        person=test_person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN
    )
    status = const.CALIBRATION_STATUS.VERBOSE[calibration.status]
    result = get_json(client, '/frontend/calibrations/', {'statuses': [status]})
    calibrations = result['calibrations']
    assert len(calibrations) == 1
    actions = calibrations[0]['actions']
    expected_actions = defaultdict(lambda: const.OK)
    expected_actions[calibration_rights.ACTIONS.STATUS_ARCHIVE] = const.NO_ACCESS
    expected_actions[calibration_rights.ACTIONS.READ_FEEDBACK] = const.NO_ACCESS
    assert all(
        value == expected_actions[action]
        for action, value in actions.items()
    )


def test_calibration_get_one(
    client,
    calibration,
    calibration_person_review_builder,
    calibration_role_builder,
):
    admin_role = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN
    )
    review_id = calibration_person_review_builder(calibration=calibration).person_review.review_id
    admin_login = admin_role.person.login
    result = get_json(
        client,
        '/frontend/calibrations/{}/'.format(calibration.id),
        login=admin_login,
    )
    assert isinstance(result['author'], dict)
    assert calibration.author.login == result['author'].get('login')
    assert result['id'] == calibration.id
    assert result['admins'][0]['login'] == admin_login
    assert result['review_ids'] == [review_id]


@pytest.mark.parametrize(
    'with_reviews,without_reviews', [
        [0, 3],
        [3, 0],
        [3, 3],
    ]
)
def test_calibration_add_person(
    client,
    calibration,
    calibration_role_builder,
    review_builder,
    review_role_builder,
    person_builder,
    person_review_builder,
    with_reviews,
    without_reviews,
):
    calibration_admin = person_builder()
    calibration_role_builder(
        person=calibration_admin,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    active_review = review_builder(status=const.REVIEW_STATUS.IN_PROGRESS)
    review_role_builder(
        person=calibration_admin,
        review=active_review,
        type=const.ROLE.REVIEW.ADMIN,
    )
    persons_with_reviews = [
        person_review_builder(review=active_review).person.login
        for _ in range(with_reviews)
    ]
    persons_without_reviews = [
        person_builder().login
        for _ in range(without_reviews)
    ]
    response = post_json(
        client,
        '/frontend/calibrations/{}/add-persons/'.format(calibration.id),
        login=calibration_admin.login,
        request=dict(
            persons=persons_with_reviews + persons_without_reviews,
        )
    )
    success = response['success']
    failed = chain.from_iterable(
        err['logins'] for err in list(response.get('errors', {}).values())
    )
    assert (
        set(success) == set(persons_with_reviews) and
        set(failed) == set(persons_without_reviews)
    )


@pytest.mark.parametrize(
    'role_type,err_code', [
        (
            const.ROLE.CALIBRATION.ADMIN,
            const.ERROR_CODES.PERSONS_ARE_CALIBRATION_ADMINS,
        ),
        (
            const.ROLE.CALIBRATION.CALIBRATOR,
            const.ERROR_CODES.PERSONS_ARE_CALIBRATORS,
        ),
    ]
)
def test_calibration_add_person_with_role(
    client,
    calibration,
    calibration_role_builder,
    review_builder,
    review_role_builder,
    person_builder,
    person_review_builder,
    role_type,
    err_code
):
    calibration_admin = person_builder()
    calibration_role_builder(
        person=calibration_admin,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    active_review = review_builder(status=const.REVIEW_STATUS.IN_PROGRESS)
    review_role_builder(
        person=calibration_admin,
        review=active_review,
        type=const.ROLE.REVIEW.ADMIN,
    )
    person_review = person_review_builder(review=active_review)
    calibration_role_builder(
        person=person_review.person,
        calibration=calibration,
        type=role_type,
    )
    checking_login = person_review.person.login
    response = post_json(
        client,
        '/frontend/calibrations/{}/add-persons/'.format(calibration.id),
        login=calibration_admin.login,
        request=dict(
            persons=[checking_login],
        )
    )
    assert response['errors'][err_code]['logins'] == [checking_login]


def test_calibration_add_person_reviews_already_exists(
    client,
    calibration_builder,
    calibration_editor_role_builder,
    calibration_person_review_builder,
    person_review_builder,
    review_builder,
    review_role_builder,
):
    calibration = calibration_builder()
    admin = calibration_editor_role_builder(calibration=calibration).person

    active_review = review_builder(status=const.REVIEW_STATUS.IN_PROGRESS)
    person_review = person_review_builder(review=active_review)
    review_role_builder(person=admin, review=active_review, type=const.ROLE.REVIEW.ADMIN)
    calibration_person_review_builder(calibration=calibration, person_review=person_review)

    response = post_json(
        client,
        '/frontend/calibrations/{}/add-person-reviews/'.format(calibration.id),
        login=admin.login,
        request={'ids': [person_review.id]}
    )

    assert response['success'] == []
    assert response['warnings'][const.ERROR_CODES.ALREADY_EXISTS]['logins'] == [person_review.person.login]


def test_calibration_get_no_access(test_person, calibration_role_builder, calibration, client):
    calibration_role_builder(
        person=test_person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR
    )
    calibration.status = const.CALIBRATION_STATUS.IN_PROGRESS
    calibration.save()
    status = const.CALIBRATION_STATUS.VERBOSE[calibration.status]
    result = get_json(client, '/frontend/calibrations/', {'statuses': [status]})
    calibrations = result['calibrations']
    assert len(calibrations) == 1
    actions = calibrations[0]['actions']
    assert all((
        actions[action] == const.NO_ACCESS
        for action in calibration_rights.ACTIONS.STATUS | {calibration_rights.ACTIONS.EDIT_PARAMETERS}
    ))


def test_calibration_workflow_success(client, calibration, test_person, calibration_role_builder):
    calibration_role_builder(
        person=test_person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN
    )
    calibration.status = const.CALIBRATION_STATUS.DRAFT
    calibration.save()
    post_json(client, '/frontend/calibrations/{}/workflow/'.format(calibration.id),
              {'workflow': calibration_rights.ACTIONS.STATUS_PUBLISH}
              )
    assert models.Calibration.objects.get(id=calibration.id).status == const.CALIBRATION_STATUS.IN_PROGRESS


def test_calibration_workflow_denied(client, calibration, test_person, calibration_role_builder):
    calibration_role_builder(
        person=test_person,
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR
    )
    calibration.status = const.CALIBRATION_STATUS.DRAFT
    calibration.save()
    with pytest.raises(Exception):
        post_json(client, '/frontend/calibrations/{}/workflow/'.format(calibration.id),
                  {'workflow': calibration_rights.ACTIONS.STATUS_PUBLISH}
                  )
    assert models.Calibration.objects.get(id=calibration.id).status == const.CALIBRATION_STATUS.DRAFT


def test_calibration_copy(
    client,
    calibration_person_review,
    calibration_role_builder,
):
    calibration = calibration_person_review.calibration
    admins = [calibration_role_builder(calibration=calibration, type=const.ROLE.CALIBRATION.ADMIN) for _ in range(2)]
    for _ in range(2):
        calibration_role_builder(calibration=calibration, type=const.ROLE.CALIBRATION.CALIBRATOR)
    login = admins[0].person.login
    resp = post_json(
        client,
        '/frontend/calibrations/{}/copy/'.format(calibration.id),
        request={},
        login=login,
    )
    new_calibration_id = resp['id']
    new_calibration = (
        models.Calibration.objects
        .filter(id=new_calibration_id)
        .prefetch_related('calibration_person_reviews', 'roles')
        .first()
    )
    assert new_calibration
    assert new_calibration.id != calibration.id
    assert new_calibration.name == 'Копия калибровки: {}'.format(calibration.name)
    assert new_calibration.start_date != calibration.start_date
    assert new_calibration.finish_date != calibration.finish_date
    assert new_calibration.status == const.CALIBRATION_STATUS.DRAFT
    new_cprs = new_calibration.calibration_person_reviews
    assert new_cprs.count() == 1
    assert new_cprs.get().person_review_id and calibration_person_review.person_review_id
    roles = new_calibration.roles
    assert roles.count() == 1
    assert roles.get().type == const.ROLE.CALIBRATION.ADMIN
