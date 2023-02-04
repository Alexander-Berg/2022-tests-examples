import mock

from review.core import const, models
from tests.helpers import post_json


def test_calibration_person_review_single_action_no_access(calibration_person_review_builder, client):
    calibration_person_review = calibration_person_review_builder(discuss=False)
    result = post_json(client, '/frontend/calibration-person-reviews/{}/actions/'.format(calibration_person_review.id),
                       {'discuss': True})
    assert const.ERROR_CODES.ACTION_UNAVAILABLE in result['errors']
    calibration_person_review.refresh_from_db()
    assert not calibration_person_review.discuss


def test_calibration_person_review_single_action_allowed(
    client,
    calibration_person_review_builder,
    calibration_role_builder,
    test_person,
):
    cpr = calibration_person_review_builder(discuss=False)
    calibration_role_builder(person=test_person, calibration=cpr.calibration, type=const.ROLE.CALIBRATION.ADMIN)

    result = post_json(client, '/frontend/calibration-person-reviews/{}/actions/'.format(cpr.id),
                       {'discuss': True})
    assert 'errors' not in result
    cpr.refresh_from_db()
    assert cpr.discuss


def test_calibration_person_review_bulk_action(
    client,
    calibration_person_review_builder,
    calibration_role_builder,
    person_review_role_builder,
    test_person,
):
    calibration_person_reviews = [calibration_person_review_builder(discuss=False) for _ in range(3)]
    for cpr in calibration_person_reviews:
        calibration_role_builder(person=test_person, calibration=cpr.calibration, type=const.ROLE.CALIBRATION.ADMIN)
    ids = [pc.id for pc in calibration_person_reviews]
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        result = post_json(client, '/frontend/calibration-person-reviews/bulk/actions/',
                           {'ids': ids, 'discuss': True})
    assert 'errors' not in result
    updated_calibrations = models.CalibrationPersonReview.objects.filter(
        id__in=ids
    ).values_list('discuss', flat=True)
    assert all(updated_calibrations)
