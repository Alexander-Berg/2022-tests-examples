import json

import pytest

from functools import partial

from constance.test import override_config
from django.utils import timezone

from intranet.femida.src.candidates import choices
from intranet.femida.src.candidates.filters.base import FilterCtl
from intranet.femida.src.candidates.filters.fields.candidates import CANDIDATE_EMPLOYEE_TYPES
from intranet.femida.src.candidates.models import Candidate
from intranet.femida.src.interviews.choices import INTERVIEW_TYPES, INTERVIEW_STATES, AA_TYPES
from intranet.femida.src.offers.choices import SOURCES

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises


def test_filter_by_skill_any():
    candidate_skill1 = f.CandidateSkillFactory()
    candidate_skill2 = f.CandidateSkillFactory()
    f.CandidateSkillFactory()

    data = {'filters': [
        {
            'field': 'SkillFilter',
            'condition': 'Any',
            'values': [candidate_skill1.skill_id, candidate_skill2.skill_id],
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    candidates = Candidate.unsafe.filter(query)
    expected_candidate_ids = {candidate_skill1.candidate.id, candidate_skill2.candidate.id}
    assert {c.id for c in candidates} == expected_candidate_ids


def test_filter_by_skill_not_all():
    candidate_skill1 = f.CandidateSkillFactory()
    candidate_skill2 = f.CandidateSkillFactory()
    candidate_skill3 = f.CandidateSkillFactory()

    data = {'filters': [
        {
            'field': 'SkillFilter',
            'condition': 'NotAll',
            'values': [candidate_skill1.skill_id],
        },
        {
            'field': 'SkillFilter',
            'condition': 'NotAll',
            'values': [candidate_skill2.skill_id],
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    candidates = Candidate.unsafe.filter(query)
    assert list(candidates) == [candidate_skill3.candidate]


def test_filter_by_consideration_source_resolution():
    candidate_by_con_res_source = dict()

    refused_resolution = choices.CONSIDERATION_RESOLUTIONS.refused_after_vacancy_proposal
    resolutions = (
        refused_resolution,
        choices.CONSIDERATION_RESOLUTIONS.offer_rejected,
    )
    sources = (
        SOURCES.yandex_job_website,
        SOURCES.candidates_base,
    )
    for resolution in resolutions:
        for source in sources:
            consideration = f.ConsiderationFactory(
                resolution=resolution,
                source=source,
                state=choices.CONSIDERATION_STATUSES.archived,
                candidate__status=choices.CANDIDATE_STATUSES.closed,
            )
            candidate_by_con_res_source[(resolution, source)] = consideration.candidate

    data = {'filters': [
        {
            'field': 'ConsiderationSourceFilter',
            'condition': 'Any',
            'values': [SOURCES.candidates_base, SOURCES.internal_reference],
        },
        {
            'field': 'ConsiderationResolutionFilter',
            'condition': 'Any',
            'values': [refused_resolution],
        },
    ]}
    expected_candidate = candidate_by_con_res_source[(refused_resolution, SOURCES.candidates_base)]

    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    candidates = Candidate.unsafe.filter(query)
    assert list(candidates) == [expected_candidate]


@override_config(DISABLED_FILTERS='[]')
@pytest.mark.parametrize('filter_name', (
    'ConsiderationRecruiterFilter',
    'LastConsiderationRecruiterFilter',
))
def test_filter_by_responsibles(filter_name):
    in_progress = choices.CANDIDATE_STATUSES.in_progress
    archived = choices.CONSIDERATION_STATUSES.archived

    candidate1 = f.create_candidate_with_consideration(status=in_progress)
    candidate2 = f.create_candidate_with_consideration(status=in_progress)
    f.create_candidate_with_consideration(status=in_progress)

    candidate3 = f.CandidateFactory(status=choices.CANDIDATE_STATUSES.closed)
    consideration = f.create_consideration_with_responsibles(state=archived, candidate=candidate3)
    f.create_consideration_with_responsibles(state=archived)

    data = {'filters': [
        {
            'field': filter_name,
            'condition': 'Any',
            'values': [
                candidate1.main_recruiter.username,
                candidate2.recruiters[0].username,
                consideration.main_recruiter.username,
            ],
        },
    ]}

    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    candidates = Candidate.unsafe.filter(query)
    assert {c.id for c in candidates} == {candidate1.id, candidate2.id, candidate3.id}


@override_config(DISABLED_FILTERS='[]')
@pytest.mark.parametrize('sources, expected_candidates_count', (
    ((SOURCES.internal_event,), 1),
    ((SOURCES.internal_event, SOURCES.academic_project), 2),
    ((SOURCES.yandex_job_website,), 0),
))
def test_filter_by_last_consideration_source(sources, expected_candidates_count):
    candidate1 = f.CandidateFactory(source=SOURCES.internal_event)
    f.ConsiderationFactory(
        candidate=candidate1,
        state=choices.CONSIDERATION_STATUSES.in_progress,
    )
    f.ConsiderationFactory(
        candidate=candidate1,
        state=choices.CONSIDERATION_STATUSES.archived,
        source=SOURCES.yandex_job_website,
        is_last=False,
    )

    candidate2 = f.CandidateFactory()
    f.ConsiderationFactory(
        candidate=candidate2,
        state=choices.CONSIDERATION_STATUSES.archived,
        source=SOURCES.academic_project,
    )
    f.ConsiderationFactory(
        candidate=candidate2,
        state=choices.CONSIDERATION_STATUSES.archived,
        source=SOURCES.yandex_job_website,
        is_last=False,
    )
    f.ConsiderationFactory(source=SOURCES.expert_evaluation)

    data = {'filters': [
        {
            'field': 'LastConsiderationSourceFilter',
            'condition': 'Any',
            'values': sources,
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    count = Candidate.unsafe.filter(query).count()
    assert count == expected_candidates_count


def test_filter_by_created():
    today = timezone.now()
    f.CandidateFactory(created=today - timezone.timedelta(days=100500))
    yesterday = today - timezone.timedelta(days=1)
    candidate = f.CandidateFactory(created=yesterday)

    data = {'filters': [
        {
            'field': 'CandidateCreatedFilter',
            'condition': 'Range',
            'created__gte': yesterday.date().isoformat(),
            'created__lte': today.date().isoformat(),
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    assert list(Candidate.unsafe.filter(query)) == [candidate]


@pytest.mark.parametrize('params, code', (
    ({}, 'at_least_one_field_should_be_filled'),
    ({'created__gte': '2021-01-01', 'created__lte': '-'}, 'invalid'),
))
def test_filter_by_created_invalid_params(params, code):
    f.CandidateFactory()
    data = {'filters': [
        {
            'field': 'CandidateCreatedFilter',
            'condition': 'Range',
            **params,
        },
    ]}
    ctl = FilterCtl(data)
    assert not ctl.is_valid()
    assert code in str(ctl.errors)


@pytest.mark.parametrize('modified__gte, modified__lte', (
    ('', '2021-01-01'),
    ('2021-01-01', ''),
))
def test_filter_by_modified_one_bound(modified__gte, modified__lte):
    data = {'filters': [
        {
            'field': 'CandidateModifiedFilter',
            'condition': 'Range',
            'modified__gte': modified__gte,
            'modified__lte': modified__lte,
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid()

    query = ctl.get_query()
    with assert_not_raises():
        Candidate.unsafe.filter(query)


def test_filter_by_employee_type():
    current_employee_login = 'current'
    former_employee_login = 'former'

    f.UserFactory(username=current_employee_login)
    f.UserFactory(username=former_employee_login, is_dismissed=True)
    f.UserFactory.create_batch(2)

    candidate = f.CandidateFactory(login=current_employee_login)
    f.CandidateFactory(login=former_employee_login)

    data = {'filters': [
        {
            'field': 'CandidateEmployeeTypeFilter',
            'condition': 'Equal',
            'employee_type': CANDIDATE_EMPLOYEE_TYPES.current,
        },
    ]}

    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    assert list(Candidate.unsafe.filter(query)) == [candidate]


@override_config(DISABLED_FILTERS='[]')
def test_filter_by_last_consideration_extended_status_changed_at():
    today = timezone.now().date().isoformat()
    candidate = f.create_candidate_with_consideration()
    tomorrow = (timezone.now() + timezone.timedelta(days=1)).date().isoformat()
    data = {'filters': [
        {
            'field': 'LastConsiderationExtendedStatusChangedAtFilter',
            'condition': 'Range',
            'extended_status_changed_at__gte': today,
            'extended_status_changed_at__lte': tomorrow,
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    assert list(Candidate.unsafe.filter(query)) == [candidate]


def test_filter_by_candidate_job():
    job1 = f.CandidateJobFactory(position='Асессор')
    job2 = f.CandidateJobFactory(position='Разработчик')
    f.CandidateJobFactory(position='Мэнеджер')

    data = {'filters': [
        {
            'field': 'CandidateJobPositionFilter',
            'condition': 'Any',
            'values': ['Асессор', 'разработчик'],
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    assert {c.id for c in Candidate.unsafe.filter(query)} == {job1.candidate.id, job2.candidate.id}


def test_filter_by_candidate_education():
    candidate = f.CandidateFactory()
    f.CandidateEducationFactory(candidate=candidate, institution='МГУ им. Ломоносова')
    f.CandidateEducationFactory(candidate=candidate, institution='НИУ ВШЭ')
    e1 = f.CandidateEducationFactory(institution='МГУ им. Ломоносова')
    e2 = f.CandidateEducationFactory(institution='НИУ ВШЭ')

    data = {'filters': [
        {
            'field': 'CandidateEducationFilter',
            'condition': 'NotAll',
            'values': ['МГУ', 'ВШЭ'],
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    assert {c.id for c in Candidate.unsafe.filter(query)} == {e1.candidate.id, e2.candidate.id}


def test_filter_by_candidate_status():
    c1 = f.CandidateFactory(status=choices.CANDIDATE_STATUSES.in_progress)
    f.CandidateFactory(status=choices.CANDIDATE_STATUSES.closed)

    data = {'filters': [
        {
            'field': 'CandidateStatusFilter',
            'condition': 'Equal',
            'is_active': True,
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    assert [c.id for c in Candidate.unsafe.filter(query)] == [c1.id]


@pytest.mark.parametrize('msu_end_date, hse_end_date, value, is_found', (
    ('2020-01-01', '2021-01-01', 'МГУ', False),
    ('2022-01-01', '2021-01-01', 'МГУ', True),
    (None, '2021-01-01', 'МГУ', True),
))
def test_filter_by_last_candidate_education(msu_end_date, hse_end_date, value, is_found):
    candidate = f.CandidateFactory()
    education = f.CandidateEducationFactory(
        candidate=candidate,
        institution='МГУ им. Ломоносова',
        end_date=msu_end_date,
    )
    f.CandidateEducationFactory(
        candidate=candidate,
        institution='НИУ ВШЭ',
        end_date=hse_end_date,
    )

    data = {'filters': [
        {
            'field': 'LastCandidateEducationFilter',
            'condition': 'Any',
            'values': [value],
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    result = [c.id for c in Candidate.unsafe.filter(query)]
    expected = [education.candidate.id] if is_found else []
    assert result == expected


def test_filter_by_interview_avg_grade():
    now = timezone.now()
    yesterday = now - timezone.timedelta(days=1)
    one_week_ago = now - timezone.timedelta(days=7)

    FinishedInterview = partial(
        f.InterviewFactory,
        state=INTERVIEW_STATES.finished,
        finished=one_week_ago,
    )
    candidate1 = f.CandidateFactory()
    # avg_grade = (2 + 5 + 8) / 3  = 5
    FinishedInterview(candidate=candidate1, type=INTERVIEW_TYPES.screening, grade=2)
    FinishedInterview(candidate=candidate1, type=INTERVIEW_TYPES.regular, grade=5)
    FinishedInterview(candidate=candidate1, type=INTERVIEW_TYPES.aa, grade=8)
    f.InterviewFactory(candidate=candidate1, type=INTERVIEW_TYPES.aa, grade=0)

    candidate2 = f.CandidateFactory()
    # avg_grade = (1 + 4 + 4) / 3  = 3
    FinishedInterview(candidate=candidate2, type=INTERVIEW_TYPES.screening, grade=1)
    FinishedInterview(candidate=candidate2, type=INTERVIEW_TYPES.regular, grade=4)
    FinishedInterview(candidate=candidate2, type=INTERVIEW_TYPES.aa, grade=4)
    f.InterviewFactory(candidate=candidate2, type=INTERVIEW_TYPES.aa, grade=8)

    # кандидат без секций в выбранном интервале дат
    candidate3 = f.CandidateFactory()
    f.InterviewFactory(
        candidate=candidate3,
        type=INTERVIEW_TYPES.regular,
        state=INTERVIEW_STATES.finished,
        grade=5,
        finished=now,
    )

    data = {'filters': [
        {
            'field': 'InterviewGradeFilter',
            'condition': 'Equal',
            'avg_grade__gte': 4,
            'avg_grade__lte': 6,
            'finished__lte': yesterday.date().isoformat(),
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    assert {c.id for c in Candidate.unsafe.filter(query)} == {candidate1.id}


@pytest.mark.parametrize('params', (
    {},
    {'avg_grade__gte': '', 'avg_grade__lte': ''},
    {'avg_grade__gte': 2, 'avg_grade__lte': 1},
))
def test_filter_by_interview_avg_grade_invalid_params(params):
    data = {'filters': [
        {
            'field': 'InterviewGradeFilter',
            'condition': 'Equal',
            **params,
        },
    ]}
    ctl = FilterCtl(data)
    assert not ctl.is_valid()


@pytest.mark.parametrize('params, grade', (
    (
        {
            'condition': 'GradeRange',
            'avg_grade__gte': 4,
            'avg_grade__lte': 6,
        },
        5,
    ),
    ({'condition': 'WithoutNoHire', 'avg_grade__lte': 1}, 1),
    ({'condition': 'AllNoHire'}, 0),
))
def test_filter_by_aa_interview(params, grade):
    now = timezone.now()
    yesterday = now - timezone.timedelta(days=1)
    candidate = f.CandidateFactory()
    f.InterviewFactory(
        candidate=candidate,
        type=INTERVIEW_TYPES.aa,
        aa_type=AA_TYPES.canonical,
        grade=grade,
        state=INTERVIEW_STATES.finished,
        finished=yesterday,
    )

    data = {'filters': [
        {
            'field': 'AAInterviewFilter',
            'aa_type': AA_TYPES.canonical,
            'finished__lte': now.date().isoformat(),
            **params,
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    assert [c.id for c in Candidate.unsafe.filter(query)] == [candidate.id]


def test_filter_by_candidate_scoring():
    scoring1 = f.CandidateScoringFactory()
    f.CandidateScoringFactory()
    category_id = scoring1.scoring_category.id

    data = {'filters': [
        {
            'field': 'CandidateScoringFilter',
            'condition': 'CandidateScoringCondition',
            'scoring_category': category_id,
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    with override_config(ACTUAL_CANDIDATE_SCORING_VERSIONS=json.dumps({category_id: '1'})):
        annotations = ctl.get_annotations()
        query = ctl.get_query()

    candidate_ids = {c.id for c in Candidate.unsafe.annotate(**annotations).filter(query)}
    assert candidate_ids == {scoring1.candidate.id}


@pytest.mark.parametrize('name, cnt', [
    ("user1", 1),
    ("surname1", 1),
    ("surname2 user2", 1),
    ("user3 surname3", 1),
    ("user3surname3", 0),
    ("user5 surname6", 0),
])
def test_filter_name(name, cnt):
    """
    Проверяем фильтр по имени и фамилии
    """
    for idx in range(10):
        f.CandidateFactory(first_name=f'User{idx}', last_name=f'Surname{idx}')
    data = {'filters': [
        {
            'field': 'CandidateNameFilter',
            'condition': 'CandidateNameCondition',
            'name': name,
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    annotations = ctl.get_annotations()
    query = ctl.get_query()
    candidates = Candidate.unsafe.annotate(**annotations).filter(query)
    assert len(candidates) == cnt


@pytest.mark.parametrize('phone', [
    "+79251234567",
    "+7 903 456 78 90",
    "+375 29 111-22-34",
    "+1 202-588-6500",
    "+49 571 123456",
])
def test_filter_normalized_phone(phone):
    """
    Проверяем фильтр по телефону
    """
    f.CandidateContactFactory(
        account_id=phone, type=choices.CONTACT_TYPES.phone, is_main=True,
    )

    data = {'filters': [
        {
            'field': 'CandidatePhoneFilter',
            'condition': 'Contains',
            'value': phone,
        },
    ]}
    ctl = FilterCtl(data)
    assert ctl.is_valid(), ctl.errors

    query = ctl.get_query()
    candidates = Candidate.unsafe.filter(query)
    assert len(candidates) == 1
