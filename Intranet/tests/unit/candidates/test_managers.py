import pytest

from intranet.femida.src.candidates.choices import SUBMISSION_SOURCES, REFERENCE_STATUSES
from intranet.femida.src.candidates.models import Reference, Candidate
from intranet.femida.src.core.switches import TemporarySwitch
from intranet.femida.src.permissions.context import context
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('consideration_finished, reference_expiration_date', (
    # базовая логика
    (shifted_now(months=-1), shifted_now(months=1)),
    # пролонгация до закрытия рассмотрения
    (None, shifted_now(months=-1)),
    # пролонгация после закрытия рассмотрения на время выполнения отложенных тасков
    (shifted_now(days=-1), shifted_now(months=-1))
))
def test_reference_alive_positive(consideration_finished, reference_expiration_date):
    consideration = f.ConsiderationFactory(
        created=shifted_now(months=-2),  # до reference_expiration_date
        finished=consideration_finished,
    )
    reference = f.ReferenceFactory(
        expiration_date=reference_expiration_date,
        status=REFERENCE_STATUSES.approved,
    )
    f.create_submission(
        source=SUBMISSION_SOURCES.reference,
        reference=reference,
        candidate=consideration.candidate,
    )

    assert Reference.objects.alive().count() == 1


@pytest.mark.parametrize('reference_status', (
    REFERENCE_STATUSES.new,
    REFERENCE_STATUSES.rejected,
))
def test_reference_alive_negative(reference_status):
    f.ReferenceFactory(status=reference_status)
    assert Reference.objects.alive().count() == 0


@pytest.mark.parametrize('consideration_created, consideration_finished', (
    # нет пролонгации до закрытия рассмотрения, если оно создано после завершения рекомендации
    (shifted_now(days=-1), None),
    # пролонгация после закрытия рассмотрения на время выполнения отложенных тасков
    (shifted_now(months=-2), shifted_now(days=-10)),
))
def test_reference_alive_expired(consideration_created, consideration_finished):
    consideration = f.ConsiderationFactory(
        created=consideration_created,
        finished=consideration_finished,
    )
    reference = f.ReferenceFactory(
        expiration_date=shifted_now(months=-1),
        status=REFERENCE_STATUSES.approved,
    )
    f.create_submission(
        source=SUBMISSION_SOURCES.reference,
        reference=reference,
        candidate=consideration.candidate,
    )

    assert Reference.objects.alive().count() == 0


@pytest.mark.parametrize('user_perm, switch_state, result', (
    ('recruiter_perm', False, True),
    ('recruiter_assessor_perm', False, False),
    (None, False, False),
    ('recruiter_perm', True, False),
    ('recruiter_assessor_perm', True, False),
    (None, True, False),
))
def test_employees_candidates_visibility(user_perm, switch_state, result):
    f.create_waffle_switch(TemporarySwitch.DISABLE_ACCESS_TO_EMPLOYEES, switch_state)
    user = f.create_user_with_perm(user_perm)
    context.init(user)
    login = 'login'
    f.create_user(
        username=login,
        is_dismissed=False,
        is_intern=False,
    )
    f.CandidateFactory(
        login=login,
    )
    assert Candidate.objects.filter(login=login).exists() is result


@pytest.mark.parametrize('user_perm, switch_state, result', (
    ('recruiter_perm', False, True),
    ('recruiter_assessor_perm', False, False),
    (None, False, False),
    ('recruiter_perm', True, True),
    ('recruiter_assessor_perm', True, False),
    (None, True, False),
))
def test_intern_candidates_visibility(user_perm, switch_state, result):
    f.create_waffle_switch(TemporarySwitch.DISABLE_ACCESS_TO_EMPLOYEES, switch_state)
    user = f.create_user_with_perm(user_perm)
    context.init(user)
    login = 'login'
    f.create_user(
        username=login,
        is_dismissed=False,
        is_intern=True,
    )
    f.CandidateFactory(
        login=login,
    )
    assert Candidate.objects.filter(login=login).exists() is result
