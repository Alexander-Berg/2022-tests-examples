# coding: utf-8

from review.core import const
from review.core import models
from review.core.logic import roles


def test_calibrator_denormalization_created(
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
):
    calibration_person_review = calibration_person_review_builder(
        calibration=calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    )
    calibration_role = calibration_role_builder(
        type=const.ROLE.CALIBRATION.CALIBRATOR,
        calibration=calibration_person_review.calibration,
        skip_denormalization=True,
    )

    roles.denormalize_person_review_roles()

    assert models.PersonReviewRole.objects.filter(
        type=const.ROLE.PERSON_REVIEW.CALIBRATOR_DENORMALIZED,
        from_calibration_role=calibration_role,
        person=calibration_role.person,
        person_review=calibration_person_review.person_review,
    ).exists()


def test_calibrator_denormalization_deleted(
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
):
    calibration_person_review = calibration_person_review_builder(
        calibration=calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    )
    calibration_role = calibration_role_builder(
        type=const.ROLE.CALIBRATION.CALIBRATOR,
        calibration=calibration_person_review.calibration,
        skip_denormalization=True,
    )

    roles.denormalize_person_review_roles()

    search_params = dict(
        type=const.ROLE.PERSON_REVIEW.CALIBRATOR_DENORMALIZED,
        from_calibration_role=calibration_role,
        from_review_role=None,
        inherited_from=None,
        person=calibration_role.person,
        person_review=calibration_person_review.person_review,
    )

    assert models.PersonReviewRole.objects.filter(**search_params).exists()
    calibration_role.delete()
    assert not models.PersonReviewRole.objects.filter(**search_params).exists()
