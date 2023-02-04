import mock
import pytest

from review.core import const as core_const, models, const
from review.core import models as core_models

from tests.helpers import get_json, post_json, dump_model
from tests.fixtures_for_staff_models import sync_subordination_with_structure


CALIBRATORS_URL = '/frontend/calibrations/{id}/calibrators/'
RECOMMENDED_CALIBRATORS_URL = '/frontend/calibrations/{id}/recommended-calibrators/'


@pytest.mark.parametrize(
    'status', const.CALIBRATION_STATUS.ALL,
)
def test_get_calibrators(
    client,
    person_builder_bulk,
    calibration_role_builder,
    calibration_person_review_builder,
    calibration,
    department_role_builder,
    test_person,
    status,
):
    calibration_role_builder(
        calibration=calibration,
        person=test_person,
        type=core_const.ROLE.CALIBRATION.ADMIN,
    )
    heads = [department_role_builder(type=core_const.ROLE.DEPARTMENT.HEAD).person for _ in range(4)]
    for head in heads[:3]:
        calibration_role_builder(
            calibration=calibration,
            person=head,
            type=core_const.ROLE.CALIBRATION.CALIBRATOR,
        )

    persons = [
        person_builder_bulk(department=heads[0].department, _count=2),
        person_builder_bulk(department=heads[1].department, _count=3),
    ]
    sync_subordination_with_structure()
    for p in persons[0]:
        calibration_person_review_builder(calibration=calibration, person=p)
    for p in persons[1][:2]:
        calibration_person_review_builder(calibration=calibration, person=p)

    calibration.status = status
    calibration.save()

    result = get_json(client, '/frontend/calibrations/{}/calibrators/'.format(calibration.id))
    result = result['roles']
    assert len(result) == 3
    for res in result:
        if res['person']['login'] == heads[0].login:
            assert res['subordinates_total'] == 2 and res['subordinates_in_calibration'] == 2
        if res['person']['login'] == heads[1].login:
            assert res['subordinates_total'] == 3 and res['subordinates_in_calibration'] == 2
        if res['person']['login'] == heads[2].login:
            assert res['subordinates_total'] == 0 and res['subordinates_in_calibration'] == 0


def test_get_calibrators_chief(
    client,
    person_builder,
    calibration_editor_role_builder,
    calibration_role_builder,
    department_role_builder,
):
    admin = calibration_editor_role_builder()
    calibration = admin.calibration

    chief = department_role_builder(type=core_const.ROLE.DEPARTMENT.HEAD).person
    calibrator = person_builder(department=chief.department)
    calibration_role_builder(
        calibration=calibration,
        person=calibrator,
        type=core_const.ROLE.CALIBRATION.CALIBRATOR,
    )

    result = get_json(client, CALIBRATORS_URL.format(id=calibration.id), login=admin.person.login)

    assert len(result['roles']) == 1
    person = result['roles'][0]['person']
    expected_chief = dump_model(chief, ['login', 'is_dismissed', 'first_name', 'last_name', 'gender'])
    assert person['chief'] == expected_chief


def test_get_recommended_calibrators_chief(
    client,
    person_builder,
    calibration_editor_role_builder,
    calibration_role_builder,
    department_role_builder,
):
    admin = calibration_editor_role_builder()
    calibration = admin.calibration

    chief = department_role_builder(type=core_const.ROLE.DEPARTMENT.HEAD).person
    recommended_calibrator = person_builder(department=chief.department)

    with mock.patch('review.core.logic.calibration_actions.get_recommended_calibrators') as m:
        m.return_value = [{
            'person': recommended_calibrator,
            'subordinates_total': 0,
            'subordinates_in_calibration': 0,
        }]
        result = get_json(client, RECOMMENDED_CALIBRATORS_URL.format(id=calibration.id), login=admin.person.login)

    assert len(result['persons']) == 1
    person = result['persons'][0]['person']
    expected_chief = dump_model(chief, ['login', 'is_dismissed', 'first_name', 'last_name', 'gender'])
    assert person['chief'] == expected_chief


def test_get_calibrators_order(
    client,
    calibration,
    calibration_role_builder,
    person_builder,
):
    admin = calibration_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    ).person
    persons = [person_builder() for _ in range(3)]
    role_order = [
        calibration_role_builder(
            calibration=calibration,
            type=const.ROLE.CALIBRATION.CALIBRATOR,
            person=person,
        )
        # mixing persons order to be sure that order of creating persons do not affects
        # order of calibrators
        for person in (persons[1], persons[0], persons[2])
    ]
    expected_order_ids = [it.person.id for it in role_order[::-1]]
    response = get_json(
        client,
        path='/frontend/calibrations/{}/calibrators/'.format(calibration.id),
        login=admin.login
    )
    received_ids = [it['person']['id'] for it in response['roles']]
    assert expected_order_ids == received_ids


def test_add_calibrators(
    client,
    person_builder_bulk,
    calibration_role_builder,
    calibration,
    test_person,
):
    calibration_role_builder(
        calibration=calibration,
        person=test_person,
        type=core_const.ROLE.CALIBRATION.ADMIN,
    )
    persons = person_builder_bulk(_count=7)
    for person in persons[:5]:
        calibration_role_builder(
            calibration=calibration,
            person=person,
            type=core_const.ROLE.CALIBRATION.CALIBRATOR,
        )
    assert core_models.CalibrationRole.objects.filter(
        type=core_const.ROLE.CALIBRATION.CALIBRATOR
    ).count() == 5
    request = {
        'persons': [p.login for p in persons[3:]],
    }
    post_json(client, '/frontend/calibrations/{}/add-calibrators/'.format(calibration.id), request)
    assert core_models.CalibrationRole.objects.filter(
        type=core_const.ROLE.CALIBRATION.CALIBRATOR
    ).count() == 7


def test_add_calibrators_no_access(
    client,
    person_builder,
    calibration,
):
    person_to_add = person_builder()
    post_json(
        client,
        path='/frontend/calibrations/{}/add-calibrators/'.format(calibration.id),
        request=dict(persons=[person_to_add.login]),
        login=person_builder().login,
        expect_status=403,
    )
    assert not models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.ADMIN,
        person_id=person_to_add.id,
    ).exists()


def test_add_calibrators_that_are_calibrating(
    client,
    calibration_person_review,
    calibration_role_builder,
):
    calibration = calibration_person_review.calibration
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    to_add = [calibration_person_review.person_review.person.login]
    response = post_json(
        client,
        path='/frontend/calibrations/{}/add-calibrators/'.format(calibration.id),
        request=dict(persons=to_add),
        login=admin.login,
    )
    err_logins = response['errors']['PERSONS_ARE_CALIBRATING']['params']['logins']
    assert err_logins == to_add


def test_add_existing_calibrators(
    client,
    calibration,
    calibration_role_builder,
):
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    calibrator = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    ).person
    response = post_json(
        client,
        path='/frontend/calibrations/{}/add-calibrators/'.format(calibration.id),
        request=dict(persons=[calibrator.login]),
        login=admin.login,
    )
    err_logins = response['errors']['ALREADY_EXISTS']['params']['logins']
    assert err_logins == [calibrator.login]


def test_delete_calibrators(
    client,
    calibration,
    calibration_role_builder,
    person,
):
    calibration_role_builder(
        calibration=calibration,
        person=person,
        type=core_const.ROLE.CALIBRATION.ADMIN,
    )
    calibrators_logins = [
        calibration_role_builder(
            calibration=calibration,
            type=core_const.ROLE.CALIBRATION.CALIBRATOR,
        ).person.login
        for _ in range(5)
    ]
    to_del, alive_expected = calibrators_logins[:3], calibrators_logins[3:]
    post_json(
        client,
        path='/frontend/calibrations/{}/delete-calibrators/'.format(calibration.id),
        request=dict(persons=to_del),
        login=person.login,
    )
    alive = core_models.CalibrationRole.objects.filter(
        type=core_const.ROLE.CALIBRATION.CALIBRATOR
    ).values_list('person__login', flat=True)
    assert set(alive) == set(alive_expected)


def test_delete_calibrators_no_access(
    client,
    calibration,
    calibration_role_builder,
    person,
):
    calibrator = calibration_role_builder(
        type=const.ROLE.CALIBRATION.CALIBRATOR,
        calibration=calibration,
    ).person
    post_json(
        client,
        path='/frontend/calibrations/{}/delete-calibrators/'.format(calibration.id),
        request=dict(persons=[calibrator.login]),
        login=person.login,
        expect_status=403,
    )
    assert not models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.ADMIN,
        person_id=calibrator.id,
    ).exists()


def test_get_admins(
    client,
    calibration,
    calibration_editor_role_builder,
):
    cur_person = calibration_editor_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    ).person
    expecting_admins_logins = {
        calibration_editor_role_builder(
            calibration=calibration,
            type=const.ROLE.CALIBRATION.ADMIN,
        ).person.login
        for _ in range(3)
    }
    response = get_json(
        client,
        path='/frontend/calibrations/{}/admins/'.format(calibration.id),
        login=cur_person.login,
    )
    received_logins = {admin['login'] for admin in response['admins']}
    assert all(
        expected in received_logins
        for expected in expecting_admins_logins
    )


def test_get_admins_no_access(
    client,
    calibration,
    person,
):
    response = get_json(
        client,
        path='/frontend/calibrations/{}/admins/'.format(calibration.id),
        login=person.login,
        expect_status=403,
    )
    assert 'errors' in response


def test_calibration_add_admins(
    client,
    calibration,
    calibration_editor_role_builder,
    person_builder,
):
    cur_person = calibration_editor_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    person_to_add = person_builder()
    calibration_editor_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    post_json(
        client,
        path='/frontend/calibrations/{}/add-admins/'.format(calibration.id),
        request=dict(persons=[person_to_add.login]),
        login=cur_person.login,
    )
    assert models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.ADMIN,
        person_id=person_to_add.id,
    ).exists()


def test_admin_has_rights_after_creation(
    client,
    calibration,
    calibration_editor_role_builder,
    calibration_person_review_builder,
    person_builder,
):
    cur_person = calibration_editor_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    calibration.status = const.CALIBRATION_STATUS.IN_PROGRESS
    calibration.save()
    expecting_id = calibration_person_review_builder(calibration=calibration).id
    admin_to_add = person_builder()
    post_json(
        client,
        path='/frontend/calibrations/{}/add-admins/'.format(calibration.id),
        request=dict(persons=[admin_to_add.login]),
        login=cur_person.login,
    )
    result = get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=admin_to_add.login,
    )
    received_ids = [it['id'] for it in result['calibration_person_reviews']]
    assert [expecting_id] == received_ids


def test_calibration_add_admins_no_access(
    calibration,
    person_builder,
    client,
):
    person_to_add = person_builder()
    post_json(
        client,
        path='/frontend/calibrations/{}/add-admins/'.format(calibration.id),
        request=dict(persons=[person_to_add.login]),
        login=person_builder().login,
        expect_status=403,
    )
    assert not models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.ADMIN,
        person_id=person_to_add.id,
    ).exists()


def test_calibration_delete_admins(
    client,
    calibration,
    calibration_editor_role_builder,
    person,
):
    calibration_editor_role_builder(
        calibration=calibration,
        person=person,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    post_json(
        client,
        path='/frontend/calibrations/{}/delete-admins/'.format(calibration.id),
        request=dict(persons=[person.login]),
        login=person.login,
    )
    assert not models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.ADMIN,
        person_id=person.id,
    ).exists()


def test_calibration_delete_admins_no_access(
    client,
    calibration,
    calibration_role_builder,
    person_builder,
):
    admin = person_builder()
    calibration_role_builder(
        calibration=calibration,
        person=admin,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    post_json(
        client,
        path='/frontend/calibrations/{}/delete-admins/'.format(calibration.id),
        request=dict(persons=[admin.login]),
        login=person_builder().login,
        expect_status=403,
    )
    assert models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.ADMIN,
        person_id=admin.id,
    ).exists()


def test_log_access(
    client,
    comment_situation,
):
    calibration_person = comment_situation['calibration_person']
    person_review = comment_situation['person_review']
    comment = comment_situation['comment']
    response = get_json(
        client,
        path='/frontend/persons/{}/log/'.format(person_review.person.login),
        login=calibration_person.login,
    )
    assert response['log'][0]['text'] == comment


def test_comments_access(
    client,
    comment_situation,
):
    calibration_person = comment_situation['calibration_person']
    person_review = comment_situation['person_review']
    comment = comment_situation['comment']
    response = get_json(
        client,
        path='/frontend/person-reviews/{}/comments/'.format(person_review.id),
        login=calibration_person.login,
    )
    assert response['comments'][0]['text'] == comment


@pytest.fixture(
    params=[
        dict(role=const.ROLE.CALIBRATION.ADMIN),
        dict(role=const.ROLE.CALIBRATION.CALIBRATOR),
    ]
)
def comment_situation(
    request,
    client,
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
    person_review,
    person_review_role_builder,
):
    reviewer = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    ).person
    comment = 'asd'

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        post_json(
            client,
            path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
            request=dict(comment=comment),
            login=reviewer.login,
        )
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    calibration_person_review_builder(
        person_review=person_review,
        calibration=calibration,
    )

    checking_role = calibration_role_builder(
        calibration=calibration,
        type=request.param['role'],
    )
    return dict(
        calibration_person=checking_role.person,
        person_review=person_review,
        comment=comment,
    )


def test_action_returns_new_person_review_obj(
    client,
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
    person_review,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    calibration_person_review_builder(
        person_review=person_review,
        calibration=calibration,
    )
    mark = 'extraordinary'

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        response = post_json(
            client,
            path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
            request=dict(comment='asd', mark=mark),
            login=admin.login,
        )
    assert response['person_review'].get('mark') == mark


def test_calibration_role_recommendation(
    client,
    calibration,
    test_person,
    person_builder,
    calibration_role_builder,
    department_role_builder,
    calibration_person_review_builder,
):
    calibration_role_builder(
        calibration=calibration,
        person=test_person,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    calibration_person_reviews = [calibration_person_review_builder(calibration=calibration) for _ in range(4)]
    persons = [cpr.person_review.person for cpr in calibration_person_reviews]
    heads = [persons[3], person_builder(), person_builder()]
    for person, head in zip(persons[:3], heads):
        department_role_builder(
            department=person.department,
            person=head,
            type=core_const.ROLE.DEPARTMENT.HEAD,
        )
    calibration_role_builder(
        calibration=calibration,
        person=heads[1],
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    result = get_json(client, '/frontend/calibrations/{}/recommended-calibrators/'.format(calibration.id))
    result = result['persons']
    assert len(result) == 1
    assert result[0]['person']['login'] == heads[2].login
