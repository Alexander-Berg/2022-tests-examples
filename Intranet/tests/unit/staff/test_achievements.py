import pytest

from unittest.mock import patch, ANY

from intranet.femida.src.candidates.choices import REFERENCE_STATUSES
from intranet.femida.src.offers.models import Offer
from intranet.femida.src.staff.achievements import ApprovedReferenceAchievement, AcceptedOfferReferenceAchievement
from intranet.femida.tests import factories as f


@pytest.mark.parametrize('created_delta, closed_delta, calls', [
    (1, 1, 1),
    (1, 150, 1),
    (1, 183, 1),
    (1, 184, 0),
    (-2, -1, 0),
    (-2, 2, 0),
])
@patch('intranet.femida.src.staff.achievements.AchievementsAPI.give', return_value=None)
def test_achievement_reference_offer_closed_in_time(mock_give, created_delta, closed_delta, calls):
    f.create_n_references_with_m_closed_offers(f.UserFactory(), (1, 1), REFERENCE_STATUSES.approved,
                                               [(created_delta, closed_delta)])
    AcceptedOfferReferenceAchievement().give_all()
    assert mock_give.call_count == calls


@patch('intranet.femida.src.staff.achievements.AchievementsAPI.give', return_value=None)
def test_achievement_reference_by_offer_id(mock_give):
    f.create_n_references_with_m_closed_offers(f.UserFactory(), (1, 1), REFERENCE_STATUSES.approved, [(1, 2)])
    offer = Offer.unsafe.first()
    AcceptedOfferReferenceAchievement(offer.id).give_all()
    assert mock_give.call_count == 1


def test_expected_achievements_has_no_achievements_case(expected_achievements):
    assert any(green == 0 for _, green, _ in expected_achievements)


def test_expected_achievements_purple_le_green(expected_achievements):
    assert all(purple <= green for _, green, purple in expected_achievements)


def test_expected_achievements_has_green_achievements_case(expected_achievements):
    assert any(green > 0 for _, green, _ in expected_achievements)


def test_expected_achievements_has_green_without_purple_achievements_case(expected_achievements):
    assert any(purple == 0 and green > 0 for _, green, purple in expected_achievements)


def test_expected_achievements_has_purple_achievements_case(expected_achievements):
    assert any(purple > 0 for _, _, purple in expected_achievements)


@patch('intranet.femida.src.staff.achievements.AchievementsAPI.give', return_value=None)
def test_reference_many_users_achievements_check_not_given(mock_give, expected_achievements):
    ApprovedReferenceAchievement().give_all()
    AcceptedOfferReferenceAchievement().give_all()

    for user, green_count, purple_count in expected_achievements:
        if green_count + purple_count == 0:
            for call in mock_give.call_list:
                assert call.args.kwargs['login'] != user.username


@patch('intranet.femida.src.staff.achievements.AchievementsAPI.give', return_value=None)
def test_reference_many_users_achievements_check_green_given(mock_give, expected_achievements):
    ApprovedReferenceAchievement().give_all()
    AcceptedOfferReferenceAchievement().give_all()

    for user, green_count, _ in expected_achievements:
        if green_count > 0:
            mock_give.assert_any_call(
                login=user.username,
                level=green_count,
                achievement_id=5300,
                comment=ANY,
            )


@patch('intranet.femida.src.staff.achievements.AchievementsAPI.give', return_value=None)
def test_reference_many_users_achievements_check_green_not_given(mock_give, expected_achievements):
    ApprovedReferenceAchievement().give_all()
    AcceptedOfferReferenceAchievement().give_all()

    for user, green_count, _ in expected_achievements:
        if green_count == 0:
            for call in mock_give.call_list:
                kwargs = call.args.kwargs
                assert kwargs['login'] != user.username and kwargs['achievement_id'] != 5300


@patch('intranet.femida.src.staff.achievements.AchievementsAPI.give', return_value=None)
def test_reference_many_users_achievements_purple_given(mock_give, expected_achievements):
    ApprovedReferenceAchievement().give_all()
    AcceptedOfferReferenceAchievement().give_all()

    for user, _, purple_count in expected_achievements:
        if purple_count > 0:
            mock_give.assert_any_call(
                login=user.username,
                level=purple_count,
                achievement_id=171,
                comment=ANY,
            )


@patch('intranet.femida.src.staff.achievements.AchievementsAPI.give', return_value=None)
def test_reference_many_users_achievements_purple_not_given(mock_give, expected_achievements):
    ApprovedReferenceAchievement().give_all()
    AcceptedOfferReferenceAchievement().give_all()

    for user, _, purple_count in expected_achievements:
        if purple_count == 0:
            for call in mock_give.call_list:
                kwargs = call.args.kwargs
                assert kwargs['login'] != user.username and kwargs['achievement_id'] != 171
