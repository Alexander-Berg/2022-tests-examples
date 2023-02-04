import pytest

from intranet.femida.src.offers.models import Offer
from intranet.femida.src.permissions.context import context
from intranet.femida.src.vacancies.choices import VACANCY_ROLES

from intranet.femida.tests import factories as f


@pytest.mark.parametrize('user_perm, user_role', (
    ('hrbp_perm', VACANCY_ROLES.observer),
    ('hrbp_perm', VACANCY_ROLES.auto_observer),
    ('recruiter_perm', VACANCY_ROLES.recruiter),
    ('recruiter_perm', VACANCY_ROLES.main_recruiter),
))
def test_users_with_perms_access_to_offer(user_perm, user_role):
    user = f.create_user_with_perm(user_perm)
    members = [] if user_role is None else [(user_role, user)]
    vacancy, consideration = f.create_simple_vacancy_with_candidate(vacancy_members=members)
    f.OfferFactory(candidate=consideration.candidate, vacancy=vacancy)
    context.init(user)
    offer_qs = Offer.objects.filter(candidate=consideration.candidate)
    assert offer_qs.exists()


@pytest.mark.parametrize('user_role', VACANCY_ROLES._db_values)
def test_users_without_perms_access_to_offer(user_role):
    result = user_role == VACANCY_ROLES.hiring_manager
    user = f.create_user()
    members = [] if user_role is None else [(user_role, user)]
    vacancy, consideration = f.create_simple_vacancy_with_candidate(vacancy_members=members)
    f.OfferFactory(candidate=consideration.candidate, vacancy=vacancy)
    context.init(user)
    offer_qs = Offer.objects.filter(candidate=consideration.candidate)
    assert offer_qs.exists() is result
