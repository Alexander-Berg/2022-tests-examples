from review.core import models
from review.core import const
from review.core.logic import (
    calibration_actions,
    calibration_rights,
    roles as roles_logic,
)


def test_calibration_roles_create(
    person_review_builder,
    person_builder,
    test_person,
):
    person_reviews = [person_review_builder() for _ in range(2)]
    assert not models.PersonReviewRole.objects.filter(
        type=const.ROLE.PERSON_REVIEW.CALIBRATOR_DENORMALIZED,
        person_review__in=person_reviews,
    ).exists()

    calibration = calibration_actions.create(
        test_person,
        person_reviews,
    )
    random_person = person_builder()
    calibration_actions.add_calibrators(
        calibration,
        [random_person]
    )
    calibration_actions.follow_workflow(
        calibration,
        calibration_rights.ACTIONS.STATUS_PUBLISH,
    )
    calibration_roles_query = models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    assert calibration_roles_query.count() == 1
    assert calibration_roles_query.first().person == random_person

    person_review_roles_query = models.PersonReviewRole.objects.filter(
        person_review__in=person_reviews,
        type=const.ROLE.PERSON_REVIEW.CALIBRATOR_DENORMALIZED
    )

    assert person_review_roles_query.count() == len(person_reviews)
    assert all(it.person == random_person for it in person_review_roles_query.all())


def test_calibration_roles_delete(
    person_review_builder,
    person_builder,
    test_person,
):
    person_reviews = [person_review_builder() for _ in range(2)]
    assert not models.PersonReviewRole.objects.filter(
        type=const.ROLE.PERSON_REVIEW.CALIBRATOR_DENORMALIZED,
        person_review__in=person_reviews,
    ).exists()

    calibration = calibration_actions.create(
        test_person,
        person_reviews,
    )
    calibration_actions.follow_workflow(
        calibration,
        calibration_rights.ACTIONS.STATUS_PUBLISH,
    )
    random_person = person_builder()
    calibration_actions.add_calibrators(
        calibration,
        [random_person]
    )
    person_review_roles_query = models.PersonReviewRole.objects.filter(
        person_review__in=person_reviews,
        type=const.ROLE.PERSON_REVIEW.CALIBRATOR_DENORMALIZED
    )

    assert person_review_roles_query.count() == len(person_reviews)
    calibration_actions.delete_roles(calibration, [random_person], const.ROLE.CALIBRATION.CALIBRATOR)
    assert person_review_roles_query.count() == 0


def test_calibration_roles_actualize(
    person_review_builder,
    person_builder,
    test_person,
):
    person_reviews = [person_review_builder() for _ in range(2)]
    calibration = calibration_actions.create(
        test_person,
        person_reviews,
    )
    calibration_actions.follow_workflow(
        calibration,
        calibration_rights.ACTIONS.STATUS_PUBLISH,
    )
    random_person = person_builder()
    calibration_actions.add_calibrators(
        calibration,
        [random_person]
    )
    calibrator_denormalized_roles = models.PersonReviewRole.objects.filter(
        person_review__in=person_reviews,
        type=const.ROLE.PERSON_REVIEW.CALIBRATOR_DENORMALIZED
    )
    assert calibrator_denormalized_roles.count() == len(person_reviews)
    models.Calibration.objects.filter(
        id=calibration.id,
    ).update(status=const.CALIBRATION_STATUS.ARCHIVE)
    roles_logic._denormalize_calibration_roles()
    assert calibrator_denormalized_roles.count() == 0
    calibrator_archived_roles = models.PersonReviewRole.objects.filter(
        person_review__in=person_reviews,
        type=const.ROLE.PERSON_REVIEW.CALIBRATOR_ARCHIVED,
    )
    assert calibrator_archived_roles.count() == len(person_reviews)
    calibration_roles_query = models.CalibrationRole.objects.filter(
        calibration_id=calibration.id,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    assert calibration_roles_query.count() == 1
    assert calibration_roles_query.first().person == random_person
