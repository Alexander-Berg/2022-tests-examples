# coding: utf-8
import pytest
from pretend import stub

from review.shortcuts import const
from review.core.logic import calibration_actions


def test_calibration_subordination_stats_normal(
    case,
    calibration_person_review_builder,
    calibration_role_builder,
):
    calibration_person_review_builder(
        calibration=case.calibration,
        person=case.subordinates_one[0],
    )
    calibration_person_review_builder(
        calibration=case.calibration,
        person=case.subordinates_two[0],
    )
    calibration_person_review_builder(
        calibration=case.calibration,
        person=case.subordinates_one[-1],
    )
    calibration_role_builder(
        calibration=case.calibration,
        person=case.head_one,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    calibration_role_builder(
        calibration=case.calibration,
        person=case.head_two,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    )
    total_subordinates, in_calibration_subordinates = calibration_actions.get_subordination_stats(
        calibrators=[
            case.head_one,
            case.head_two,
        ],
        calibration=case.calibration,
    )
    assert total_subordinates == {
        case.head_one.id: 3,
        case.head_two.id: 1,
    }
    assert in_calibration_subordinates == {
        case.head_one.id: 2,
        case.head_two.id: 1,
    }


def test_calibration_subordination_stats_no_calibrators(
    case,
    calibration_person_review_builder,
    calibration_role_builder,
):
    calibration_person_review_builder(
        calibration=case.calibration,
        person=case.subordinates_one[0],
    )
    calibration_person_review_builder(
        calibration=case.calibration,
        person=case.subordinates_two[0],
    )
    total_subordinates, in_calibration_subordinates = calibration_actions.get_subordination_stats(
        calibrators=[],
        calibration=case.calibration,
    )
    assert total_subordinates == {}
    assert in_calibration_subordinates == {}


@pytest.fixture(name='case')
def calibration_persons_and_heads(
    calibration,
    person_builder_bulk,
    department_role_builder,
):
    head_one_role = department_role_builder(
        type=const.ROLE.DEPARTMENT.HEAD,
    )
    head_two_role = department_role_builder(
        type=const.ROLE.DEPARTMENT.HEAD,
    )
    subordinates_one = person_builder_bulk(
        _count=3,
        department=head_one_role.department,
    )
    subordinates_two = person_builder_bulk(
        _count=2,
        department=head_two_role.department,
    )
    for sub in (subordinates_one[-1], subordinates_two[-1]):
        sub.is_dismissed = True
        sub.save()

    return stub(
        head_one=head_one_role.person,
        head_two=head_two_role.person,
        subordinates_one=subordinates_one,
        subordinates_two=subordinates_two,
        calibration=calibration,

    )
