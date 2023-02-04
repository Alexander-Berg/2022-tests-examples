from constance import config
import json
from unittest.mock import patch
import pytest

from intranet.femida.src.interviews.choices import APPLICATION_STATUSES
from intranet.femida.src.staff.choices import DEPARTMENT_KINDS
from intranet.femida.src.vacancies.choices import VACANCY_STATUSES, VACANCY_PRO_LEVELS as VPL
from intranet.femida.src.vacancies.models import Vacancy
from intranet.femida.src.vacancies.ranking import (
    rank_proposals,
    InterviewByDepartmentFactor,
    VacancyDurationV2Factor,
    ProLevelsIntersectionV2Factor,
    InvertActiveApplicationQuantityV2Factor,
)
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises, freeze


@pytest.mark.parametrize('weights', [
    'invalid json',
    '["valid json", "but not dict"]',
    '{"valid json dict": "but not numeric value"}'
])
def test_rank_proposal_robust(weights):
    config.PROPOSALS_RANKING_FORMULA = weights
    original_applications = [f.ApplicationFactory(proposal_factors={})]

    with assert_not_raises():
        applications = rank_proposals(original_applications)
    assert applications == original_applications


def test_rank_proposal_by_formula():
    weight = 2
    config.PROPOSALS_RANKING_FORMULA = json.dumps({'factor': weight})
    more_relevant = f.ApplicationFactory(proposal_factors={'factor': 1})
    less_relevant = f.ApplicationFactory(proposal_factors={'factor': 0})

    applications = rank_proposals([less_relevant, more_relevant])

    assert applications == [more_relevant, less_relevant]
    for app in applications:
        assert app.proposal_factors['relevance'] == app.proposal_factors['factor'] * weight


def test_interview_by_department_factor_1():
    direction = f.DepartmentFactory(kind=DEPARTMENT_KINDS.direction)
    vacancy = f.VacancyFactory(department__ancestors=[direction.id])
    vacancies = Vacancy.unsafe.filter(id=vacancy.id)
    filter_params = {
        'interview_direction_ids': {direction.id},
    }

    factor = InterviewByDepartmentFactor()
    vacancies = factor.annotate_qs(vacancies)
    result = factor.compute_factor(vacancies.first(), filter_params)
    assert result == 1.0


def test_interview_by_department_factor_0():
    vacancy = f.VacancyFactory(department__ancestors=[])
    vacancies = Vacancy.unsafe.filter(id=vacancy.id)
    filter_params = {
        'interview_direction_ids': set(),
    }

    factor = InterviewByDepartmentFactor()
    vacancies = factor.annotate_qs(vacancies)
    result = factor.compute_factor(vacancies.first(), filter_params)
    assert result == 0.0


@patch('django.utils.timezone.now', freeze)
@pytest.mark.parametrize('days_in_progress, expected', (
    (0, 0.0),
    (90, 0.0),
    (91, 0.5),
    (180, 0.5),
    (181, 1.0),
    (1000, 1.0),
))
def test_vacancy_duration_v2(days_in_progress, expected):
    vacancy = f.VacancyFactory(status=VACANCY_STATUSES.in_progress)
    vacancy.vacancy_history.update(changed_at=shifted_now(days=-days_in_progress))
    vacancies = Vacancy.unsafe.filter(id=vacancy.id)

    factor = VacancyDurationV2Factor()
    vacancies = factor.annotate_qs(vacancies)
    result = factor.compute_factor(vacancies.first(), filter_params={})

    assert result == expected


@pytest.mark.parametrize('vacancy_levels, filter_levels, expected', (
    ((None, None), (None, None), 6 / 6.0),
    ((VPL.intern, VPL.expert), (VPL.intern, VPL.expert), 6 / 6.0),
    ((VPL.lead, VPL.lead), (VPL.intern, VPL.expert), 1 / 1.0),
    ((VPL.intern, VPL.expert), (VPL.lead, VPL.lead), 1 / 6.0),
    ((VPL.intern, VPL.middle), (VPL.middle, VPL.lead), 1 / 3.0),
    ((VPL.intern, VPL.middle), (VPL.senior, VPL.lead), 0.0),
    ((VPL.middle, VPL.lead), (VPL.senior, None), 2 / 3.0),
    # Специально кривые кейсы
    ((VPL.expert, VPL.intern), (VPL.intern, VPL.expert), 0.0),
    ((VPL.intern, VPL.expert), (VPL.expert, VPL.intern), 0.0),
))
def test_pro_levels_intersection_v2(vacancy_levels, filter_levels, expected):
    vacancy = f.VacancyFactory(
        pro_level_min=vacancy_levels[0],
        pro_level_max=vacancy_levels[1],
    )
    vacancies = Vacancy.unsafe.filter(id=vacancy.id)

    factor = ProLevelsIntersectionV2Factor()
    vacancies = factor.annotate_qs(vacancies)
    result = factor.compute_factor(
        vacancy=vacancies.first(),
        filter_params={
            'pro_level_min': filter_levels[0],
            'pro_level_max': filter_levels[1],
        },
    )

    assert result == expected


@pytest.mark.parametrize('active_applications_count, expected', (
    (0, 1.0),
    (5, 1.0),
    (6, 0.5),
    (9, 0.5),
    (10, 0.0),
    (12, 0.0),
))
def test_invert_active_application_quantity_v2(active_applications_count, expected):
    vacancy = f.VacancyFactory()
    f.ApplicationFactory.create_batch(
        active_applications_count,
        vacancy=vacancy,
        status=APPLICATION_STATUSES.in_progress,
    )
    # шум
    f.ApplicationFactory(vacancy=vacancy, status=APPLICATION_STATUSES.draft)
    f.ApplicationFactory(vacancy=vacancy, status=APPLICATION_STATUSES.closed)

    vacancies = Vacancy.unsafe.filter(id=vacancy.id)

    factor = InvertActiveApplicationQuantityV2Factor()
    vacancies = factor.annotate_qs(vacancies)
    result = factor.compute_factor(vacancies.first(), filter_params={})

    assert result == expected
