import pytest

from review.core.logic import calibration_actions
from review.core import models, const
from tests.helpers import get_json, post_json


def test_calibration_person_reviews_for_mode_calibration(
    person_review_builder,
    test_person,
    client,
):
    person_reviews = [person_review_builder() for _ in range(4)][:2]
    calibration = calibration_actions.create(
        test_person,
        person_reviews,
    )
    calibration.status = const.CALIBRATION_STATUS.IN_PROGRESS
    calibration.save()
    calibration_actions.add_calibrators(
        calibration,
        [test_person]
    )
    result = get_json(client, '/frontend/calibrations/{}/mode-calibration/'.format(calibration.id))
    result = result['calibration_person_reviews']
    assert len(result) == 2
    expected_cpr_keys = ('discuss', 'person_review')
    assert all(
        all(k in list(cpr.keys()) for k in expected_cpr_keys)
        for cpr in result
    )
    person_reviews = [cpr['person_review'] for cpr in result]
    expected_pr_keys = ('person', 'status', 'action_at')
    assert all(
        all(k in list(pr.keys()) for k in expected_pr_keys)
        for pr in person_reviews
    )
    expected_person_keys = ('chief', 'position')
    assert all(
        all(k in pr.get('person', {}) for k in expected_person_keys)
        for pr in person_reviews
    )


def test_calibration_person_reviews_for_mode_edit(
    person_review_builder,
    test_person,
    client,
):
    person_reviews = [person_review_builder() for _ in range(2)]
    calibration = calibration_actions.create(
        test_person,
        person_reviews,
    )
    calibration_actions.add_calibrators(
        calibration,
        [test_person]
    )
    result = get_json(client, '/frontend/calibrations/{}/mode-edit/'.format(calibration.id))
    result = result['calibration_person_reviews']
    assert len(result) == 2


def test_calibration_person_review_add_person_reviews(
    calibration,
    calibration_role_builder,
    person_review_builder,
    person_review_role_builder,
    test_person,
    client,
):
    person_reviews = [person_review_builder() for _ in range(3)]
    calibration_role_builder(
        calibration=calibration,
        person=test_person,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    for pr in person_reviews[:2]:
        person_review_role_builder(
            person_review=pr,
            person=test_person,
            type=const.ROLE.PERSON_REVIEW.REVIEWER,
        )
    calibration_person_reviews_query = models.CalibrationPersonReview.objects.filter(
        calibration_id=calibration.id,
    )
    assert not calibration_person_reviews_query.exists()
    pr_ids = [pr.id for pr in person_reviews]
    post_json(client, '/frontend/calibrations/{}/add-person-reviews/'.format(calibration.id), {
        'ids': pr_ids
    })
    assert calibration_person_reviews_query.count() == 2


def test_calibration_person_review_add_persons(
    calibration,
    calibration_role_builder,
    person_review_builder,
    person_review_role_builder,
    person_builder,
    test_person,
    client,
):
    person_reviews = [person_review_builder() for _ in range(3)]
    admin = person_builder()
    calibration_role_builder(
        calibration=calibration,
        person=admin,
        type=const.ROLE.CALIBRATION.ADMIN,
    )
    for pr in person_reviews[:2]:
        person_review_role_builder(
            person_review=pr,
            person=admin,
            type=const.ROLE.PERSON_REVIEW.REVIEWER,
        )
    calibration_person_reviews_query = models.CalibrationPersonReview.objects.filter(
        calibration_id=calibration.id,
    )
    assert not calibration_person_reviews_query.exists()
    persons = [pr.person.login for pr in person_reviews]
    post_json(
        client,
        path='/frontend/calibrations/{}/add-persons/'.format(calibration.id),
        request=dict(persons=persons),
        login=admin.login,
    )
    assert calibration_person_reviews_query.count() == 2


def test_calibration_person_review_delete(
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
    person_builder,
    client,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    for _ in range(2):
        calibration_person_review_builder(
            calibration=calibration,
        )
    admin = person_builder()
    calibration_role_builder(
        calibration=calibration,
        person=admin,
        type=const.ROLE.CALIBRATION.ADMIN,
    )

    calibration_uri = '/frontend/calibrations/{}'.format(calibration.id)

    def get_calibration_person_reviews():
        calibrations_json = get_json(
            client,
            path='{}/mode-calibration/'.format(calibration_uri),
            login=admin.login,
        )
        return [
            obj['person_review']
            for obj in calibrations_json['calibration_person_reviews']
        ]

    person_reviews = get_calibration_person_reviews()
    assert len(person_reviews) == 2
    post_json(
        client,
        '{}/delete-calibration-person-reviews/'.format(calibration_uri),
        dict(ids=[pr['id'] for pr in person_reviews]),
        login=admin.login,
    )
    assert not get_calibration_person_reviews()


def test_calibration_person_review_delete_not_all(
        client,
        calibration_builder,
        calibration_role_builder,
        calibration_person_review_builder,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    cprs = [
        calibration_person_review_builder(calibration=calibration)
        for _ in range(2)
    ]
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person

    post_json(
        client,
        path='/frontend/calibrations/{}/delete-calibration-person-reviews/'.format(
            calibration.id
        ),
        request=dict(ids=[cprs[0].person_review.id]),
        login=admin.login,
    )
    response = get_json(
        client,
        path='/frontend/calibrations/{}/mode-calibration/'.format(calibration.id),
        login=admin.login,
    )
    received_cprs = response['calibration_person_reviews']
    assert len(received_cprs) == 1
    assert received_cprs[0]['id'] == cprs[1].id
