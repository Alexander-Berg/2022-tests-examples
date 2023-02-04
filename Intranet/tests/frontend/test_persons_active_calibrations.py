# coding: utf-8
from review.core import const

from tests.helpers import get_json


def test_persons_existing_active_calibration(
        client,
        test_person,
        person,
        person_builder,
        calibration_builder,
        calibration_person_review_builder,
        calibration_role_builder,
):
    calibrations_active = [
        calibration_builder(status=status)
        for status in [const.CALIBRATION_STATUS.IN_PROGRESS, const.CALIBRATION_STATUS.DRAFT]
    ]
    calibrations_active_another_person = [
        calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
        for _ in range(2)
    ]
    calibration_archive = calibration_builder(status=const.CALIBRATION_STATUS.ARCHIVE)
    admin = person_builder()
    for calibration in calibrations_active + [calibration_archive] + calibrations_active_another_person:
        calibration_role_builder(
            calibration=calibration,
            person=admin,
            type=const.ROLE.CALIBRATION.ADMIN,
        )
    for calibration in calibrations_active + [calibration_archive]:
        calibration_person_review_builder(person=person,
                                          calibration=calibration)
    for calibration in calibrations_active_another_person:
        calibration_person_review_builder(calibration=calibration)

    response = get_json(
        client,
        path='/frontend/persons/{}/active-calibrations/'.format(person.login),
        login=admin.login,
    )
    received_calibrations = response['calibrations']
    assert {it['id'] for it in received_calibrations} == {it.id for it in calibrations_active}
    assert 'name' in received_calibrations[0]
    assert 'status' in received_calibrations[0]


def test_persons_active_calibration_for_one_person_review_in_several_calibrations(
    client,
    calibration_person_review_builder,
    calibration_role_builder,
    person_review,
    person,
):
    cprs = [calibration_person_review_builder() for _ in range(2)]
    for cpr in cprs:
        calibration = cpr.calibration
        calibration.status = const.CALIBRATION_STATUS.IN_PROGRESS
        calibration.save()
        calibration_role_builder(person=person, calibration=calibration, type=const.ROLE.CALIBRATION.CALIBRATOR)
    response = get_json(
        client,
        path='/frontend/persons/{}/active-calibrations/'.format(person_review.person.login),
        login=person.login,
    )
    assert not response['calibrations']


def test_persons_non_existing_active_calibration_for_person(
    client,
    calibration_person_review,
    calibration_role_builder,
    person_review_builder,
):
    calibration = calibration_person_review.calibration
    calibration.status = const.CALIBRATION_STATUS.IN_PROGRESS
    calibration.save()
    admin = calibration_role_builder(
        calibration=calibration,
        type=const.ROLE.CALIBRATION.ADMIN,
    ).person
    response = get_json(
        client,
        path='/frontend/persons/{}/active-calibrations/'.format(person_review_builder().person.login),
        login=admin.login,
    )
    assert not response['calibrations']
