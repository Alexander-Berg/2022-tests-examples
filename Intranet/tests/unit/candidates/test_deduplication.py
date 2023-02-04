import pytest

from unittest.mock import patch, Mock

from constance.test import override_config

from intranet.femida.src.candidates.choices import (
    CANDIDATE_RESPONSIBLE_ROLES,
    CONSIDERATION_EXTENDED_STATUSES,
    CONSIDERATION_STATUSES,
    CANDIDATE_STATUSES,
    SUBMISSION_SOURCES,
    REFERENCE_STATUSES,
    CHALLENGE_TYPES,
)
from intranet.femida.src.candidates.models import Candidate
from intranet.femida.src.candidates.deduplication import CandidateMerger
from intranet.femida.src.candidates.deduplication.exceptions import MergeConflictError
from intranet.femida.src.candidates.helpers import CandidatePolyglot
from intranet.femida.src.core.models import LanguageTag
from intranet.femida.src.hire_orders.choices import HIRE_ORDER_STATUSES
from intranet.femida.src.offers.choices import OFFER_STATUSES
from intranet.femida.src.startrek.utils import StatusEnum
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises, eager_task


pytestmark = pytest.mark.django_db


def test_candidate_merge_verifications():
    verification1 = f.VerificationFactory()
    verification2 = f.VerificationFactory()
    new_candidate = Candidate.unsafe.create()

    merger = CandidateMerger([verification1.candidate, verification2.candidate])
    merger.new_candidate = new_candidate
    merger._move_verifications()

    new_candidate.refresh_from_db()
    assert new_candidate.verifications.count() == 2


def test_candidate_merge_with_correct_creation_date():
    old_candidates = f.CandidateFactory.create_batch(3)
    old_candidates = sorted(old_candidates, key=lambda x: x.created, reverse=True)

    merger = CandidateMerger(candidates=old_candidates)
    new_candidate = merger.merge()

    assert new_candidate.created == min(c.created for c in old_candidates)


def test_candidate_merge_with_rotations():
    rotation = f.RotationFactory()
    cons_with_rotation = f.ConsiderationFactory(
        is_rotation=True,
        rotation=rotation,
        extended_status=CONSIDERATION_EXTENDED_STATUSES.challenge_finished,
    )
    cons_without_rotation = f.ConsiderationFactory(
        is_rotation=False,
        rotation=None,
        extended_status=CONSIDERATION_EXTENDED_STATUSES.final_assigned,
    )
    cand_with_rotation = cons_with_rotation.candidate
    cand_without_rotation = cons_without_rotation.candidate

    merger = CandidateMerger(candidates=[cand_with_rotation, cand_without_rotation])
    new_candidate = merger.merge()

    cons_with_rotation.refresh_from_db()
    cons_without_rotation.refresh_from_db()

    # Проверяем, что основным рассмотрением стало cons_without_rotation,
    # потому что оно находится на более поздней стадии
    assert cons_without_rotation.candidate_id == new_candidate.id
    assert cons_with_rotation.candidate_id == cand_with_rotation.id

    # Проверяем, что ротация перенеслась на новое основное рассмотрение
    assert cons_without_rotation.is_rotation
    assert cons_without_rotation.rotation == rotation
    assert cons_with_rotation.rotation is None


@pytest.mark.parametrize('cand_with_ref_data, cand_without_ref_data, called_actions', (
    (
        {'status': CANDIDATE_STATUSES.closed, 'issue_status': StatusEnum.closed},
        {'status': CANDIDATE_STATUSES.closed, 'with_offer': False},
        (False, False),
    ),
    (
        {'status': CANDIDATE_STATUSES.closed, 'issue_status': StatusEnum.closed},
        {'status': CANDIDATE_STATUSES.in_progress, 'with_offer': False},
        (True, False),
    ),
    (
        {'status': CANDIDATE_STATUSES.in_progress, 'issue_status': StatusEnum.in_progress},
        {'status': CANDIDATE_STATUSES.closed, 'with_offer': False},
        (False, False),
    ),
    (
        {'status': CANDIDATE_STATUSES.closed, 'issue_status': StatusEnum.closed},
        {'status': CANDIDATE_STATUSES.in_progress, 'with_offer': True},
        (True, True),
    ),
))
@patch('intranet.femida.src.candidates.tasks.resolve_reference_issue_task.si')
@patch('intranet.femida.src.candidates.tasks.send_reference_event_task.si')
@eager_task('intranet.femida.src.candidates.deduplication.merge.actualize_reference_issues')
def test_candidate_merge_with_reference(mocked_actualize, mocked_send_event, mocked_resolve,
                                        cand_with_ref_data, cand_without_ref_data, called_actions):
    cand_with_reference = f.create_candidate_with_consideration(
        status=cand_with_ref_data['status'],
    )
    f.SubmissionFactory(
        candidate=cand_with_reference,
        reference__expiration_date=shifted_now(months=1),
        reference__status=REFERENCE_STATUSES.approved,
        source=SUBMISSION_SOURCES.reference,
    )
    cand_without_reference = f.create_candidate_with_consideration(
        status=cand_without_ref_data['status'],
    )
    if cand_without_ref_data['with_offer']:
        f.OfferFactory(
            candidate=cand_without_reference,
            status=OFFER_STATUSES.accepted,
        )

    issue = Mock()
    issue.status.key = cand_with_ref_data['issue_status']

    with patch('intranet.femida.src.candidates.tasks.get_issue', return_value=issue):
        merger = CandidateMerger(candidates=[cand_with_reference, cand_without_reference])
        merger.merge()

    assert mocked_send_event.called == called_actions[0]
    assert mocked_resolve.called == called_actions[1]


def calculate_consideration_state(extended_status):
    state = CONSIDERATION_STATUSES.in_progress
    if extended_status == CONSIDERATION_EXTENDED_STATUSES.archived:
        state = CONSIDERATION_STATUSES.archived
    return state


@pytest.mark.parametrize('extended_status_with_odo, extended_status_without_odo, is_mock_called', (
    (
        CONSIDERATION_EXTENDED_STATUSES.challenge_assigned,
        CONSIDERATION_EXTENDED_STATUSES.offer_preparing,
        True,
    ),
    (
        CONSIDERATION_EXTENDED_STATUSES.offer_accepted,
        CONSIDERATION_EXTENDED_STATUSES.offer_preparing,
        False,
    ),
    (
        CONSIDERATION_EXTENDED_STATUSES.archived,
        CONSIDERATION_EXTENDED_STATUSES.offer_preparing,
        False,
    ),
    (
        CONSIDERATION_EXTENDED_STATUSES.challenge_assigned,
        CONSIDERATION_EXTENDED_STATUSES.archived,
        False,
    ),
))
@patch('intranet.femida.src.candidates.startrek.issues.send_onedayoffer_event')
@eager_task(
    'intranet.femida.src.candidates.deduplication.merge.send_challenges_onedayoffer_event_task',
)
def test_candidate_merge_with_onedayoffer(mocked_delay, mocked_send_event, extended_status_with_odo,
                                          extended_status_without_odo, is_mock_called):
    cons_with_odo = f.ConsiderationFactory(
        state=calculate_consideration_state(extended_status_with_odo),
        extended_status=extended_status_with_odo,
    )
    application = f.ApplicationFactory(
        candidate=cons_with_odo.candidate,
        consideration=cons_with_odo,
    )
    submission = f.SubmissionFactory(
        candidate=cons_with_odo.candidate,
        forms_data={'params': {'passcode': '123'}}
    )
    f.ChallengeFactory(
        type=CHALLENGE_TYPES.contest,
        consideration=cons_with_odo,
        candidate=cons_with_odo.candidate,
        application=application,
        submission=submission,
        startrek_onedayoffer_key='ONEDAYOFFER-123',
    )
    cons_without_odo = f.ConsiderationFactory(
        state=calculate_consideration_state(extended_status_without_odo),
        extended_status=extended_status_without_odo,
    )
    merger = CandidateMerger(candidates=[cons_with_odo.candidate, cons_without_odo.candidate])
    merger.merge()

    assert mocked_send_event.called == is_mock_called


@pytest.mark.parametrize('hire_order_statuses, forbidden_actions, success', (
    ((HIRE_ORDER_STATUSES.offer_sent, HIRE_ORDER_STATUSES.verification_sent), '[]', False),
    ((HIRE_ORDER_STATUSES.closed, HIRE_ORDER_STATUSES.closed), '[]', True),
    ((HIRE_ORDER_STATUSES.candidate_prepared, HIRE_ORDER_STATUSES.closed), '[]', True),
    ((HIRE_ORDER_STATUSES.offer_sent, HIRE_ORDER_STATUSES.closed), '["candidate_merge"]', False),
))
def test_candidate_merge_with_hire_orders(hire_order_statuses, forbidden_actions, success):
    hire_orders = [f.HireOrderFactory(status=s) for s in hire_order_statuses]
    new_candidate = Candidate.unsafe.create()

    merger = CandidateMerger([hire_orders[0].candidate, hire_orders[1].candidate])
    merger.new_candidate = new_candidate

    ctx_manager = assert_not_raises() if success else pytest.raises(MergeConflictError)
    with ctx_manager:
        with override_config(AUTOHIRE_FORBIDDEN_ACTIONS=forbidden_actions):
            merger._move_hire_orders()

    for hire_order in hire_orders:
        hire_order.refresh_from_db()
        assert (hire_order.candidate == new_candidate) is success


@pytest.mark.parametrize('role1,role2', (
    (None, None),
    (None, CANDIDATE_RESPONSIBLE_ROLES.main_recruiter),
    (CANDIDATE_RESPONSIBLE_ROLES.main_recruiter, CANDIDATE_RESPONSIBLE_ROLES.recruiter),
    (CANDIDATE_RESPONSIBLE_ROLES.recruiter, CANDIDATE_RESPONSIBLE_ROLES.main_recruiter),
    (CANDIDATE_RESPONSIBLE_ROLES.main_recruiter, CANDIDATE_RESPONSIBLE_ROLES.main_recruiter),
    (CANDIDATE_RESPONSIBLE_ROLES.recruiter, CANDIDATE_RESPONSIBLE_ROLES.recruiter),
))
def test_candidate_merge_responsibles(role1, role2):
    candidate1 = f.CandidateFactory()
    candidate2 = f.CandidateFactory()
    if role1:
        f.CandidateResponsibleFactory(candidate=candidate1, role=role1)
    if role2:
        f.CandidateResponsibleFactory(candidate=candidate2, role=role2)
    new_candidate = Candidate.unsafe.create()
    candidates_have_main_recruiter = (
        CANDIDATE_RESPONSIBLE_ROLES.main_recruiter in (role1, role2)
    )

    merger = CandidateMerger([candidate1, candidate2])
    merger.new_candidate = new_candidate

    merger._copy_responsibles()

    assert bool(new_candidate.main_recruiter) == candidates_have_main_recruiter
    if new_candidate.main_recruiter:
        assert new_candidate.main_recruiter in {
            candidate1.main_recruiter,
            candidate2.main_recruiter,
        }


@pytest.mark.parametrize('agreements,result', [
    ([None, None], None),
    ([None, True], True),
    ([None, False], False),
    ([False, None], False),
    ([True, None], True),
    ([False, False], False),
    ([False, True], True),
    ([True, False], False),
    ([True, True], True),
    ([None, True, None, False, None], False),
    ([None, False, None, True, None], True),
])
def test_candidate_merge_mailing_agreements(agreements, result):
    candidates = [f.CandidateFactory(vacancies_mailing_agreement=x) for x in agreements]
    merger = CandidateMerger(candidates)
    new_candidate = merger.merge()
    assert new_candidate.vacancies_mailing_agreement == result


@pytest.mark.parametrize('candidate1_languages, candidate2_languages, result', [
    ([None], [None], [None]),
    ([None], [None, 'ru'], [None, 'ru']),
    ([None], ['ru', 'en'], ['ru', 'en']),
    (['ru'], [None], ['ru']),
    (['ru'], ['ru'], ['ru']),
    (['ru'], ['en'], ['en', 'ru']),
    (['ru'], [None, 'ru'], ['ru']),
    (['ru'], ['en', 'fr'], ['en', 'ru', 'fr']),
    ([None, 'ru'], [None], [None, 'ru']),
    ([None, 'ru'], [None, 'en'], [None, 'ru', 'en']),
    (['ru', 'fr'], ['en', 'fr'], ['en', 'ru', 'fr']),
    (['ru', 'en', 'el'], ['bg', 'en', 'ru'], ['bg', 'en', 'ru', 'el']),
])
@pytest.mark.usefixtures('language_tags_fixture')
def test_merge_languages(candidate1_languages, candidate2_languages, result):
    candidates = []
    for languages in [candidate1_languages, candidate2_languages]:
        language_tags = [LanguageTag.objects.get(tag=x) if x else None for x in languages]
        candidates.append(f.CandidateFactory())
        polyglot = CandidatePolyglot(candidates[-1])
        polyglot.update_known_languages(language_tags[0], language_tags[1:])

    merger = CandidateMerger(candidates=candidates)
    new_candidate = merger.merge()

    if result[0]:
        assert new_candidate.main_language.tag == result[0]
    else:
        assert new_candidate.main_language is None
    spoken_languages = [x.tag for x in new_candidate.spoken_languages]
    assert sorted(spoken_languages) == sorted(result[1:])


def test_merge_candidate_costs():
    n = 10
    candidates = []
    costs_sets = []
    for i in range(n):
        candidates.append(f.CandidateFactory())
        costs_sets.append(f.CandidateCostsSetFactory(candidate=candidates[-1]))
        f.CandidateCostFactory(
            cost_group='expectation',
            type='comment',
            comment=str(i),
            candidate_costs_set=costs_sets[-1]
        )

    merger = CandidateMerger(candidates=candidates)
    new_candidate = merger.merge()

    candidate_costs_sets = new_candidate.candidate_costs_set.order_by('created').all()

    for cost_set1, cost_set2 in zip(candidate_costs_sets, costs_sets):
        assert cost_set1.id != cost_set2.id
        assert cost_set1.created == cost_set2.created
        cost1 = cost_set1.costs.first()
        cost2 = cost_set2.costs.first()
        assert cost1.comment == cost2.comment
        assert cost1.type == cost2.type
        assert cost1.cost_group == cost2.cost_group
        assert cost1.id != cost2.id
