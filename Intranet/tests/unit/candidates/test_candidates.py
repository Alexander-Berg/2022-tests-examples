from unittest.mock import patch

import pytest

from django.contrib.auth import get_user_model

from intranet.femida.src.candidates.models import Candidate
from intranet.femida.tests import factories as f
from intranet.femida.src.interviews.models import Interview, Application
from intranet.femida.src.candidates.helpers import (
    CandidatePolyglot,
    CandidateLanguageTagEqContext,
)
from intranet.femida.src.candidates.controllers import (
    update_candidate_responsibles,
    update_or_create_candidate,
)
from intranet.femida.src.candidates.workflow import CandidateWorkflow
from intranet.femida.src.core.models import LanguageTag
from intranet.femida.src.permissions.context import context
from intranet.femida.src.staff.models import Department
from intranet.femida.src.vacancies.models import Vacancy
from intranet.femida.src.vacancies.helpers import get_suitable_vacancies

User = get_user_model()
pytestmark = pytest.mark.django_db
prefix = 'intranet.femida.src.'


@pytest.mark.parametrize(
    'params,result_names',
    (
        ({}, ['simple', 'first_dept', 'first_child_dept']),
        (
            {'departments': ['first_child', 'second']},
            ['first_child_dept'],
        ),
        (
            {'departments': ['first', 'second']},
            ['first_dept', 'first_child_dept'],
        ),
        (
            {'department': 'first'},
            ['first_dept', 'first_child_dept'],
        ),
        (
            {'department': 'second'},
            [],
        ),
        (
            {'department': 'first_child'},
            ['first_child_dept'],
        ),
    ),
)
def test_get_suitable_vacancies(populate_db_vacancies_variants, params, result_names):
    recruiter = f.create_recruiter()
    context.init(recruiter)
    departments = populate_db_vacancies_variants['departments']
    if 'departments' in params:
        params['departments'] = Department.objects.filter(id__in=[departments[name].id for name in params['departments']])
    if 'department' in params:
        params['department'] = departments[params['department']].id

    vacancies = populate_db_vacancies_variants['vacancies']

    vacs_qs = Vacancy.objects.all()
    result = get_suitable_vacancies(vacs_qs, **params).all()

    fit_result = {vacancies[name] for name in result_names}
    assert fit_result == set(result)


# TODO: привести к виду unit-теста
def test_proposals_create(populate_db_proposals):
    recruiter = f.create_recruiter()
    context.init(recruiter)

    workflow = CandidateWorkflow(
        instance=populate_db_proposals['candidate'],
        user=recruiter,
    )
    create_proposals_action = workflow.get_action('create_proposals')
    create_proposals_action.perform(
        responsibles=User.objects.filter(id=recruiter.id),
        interviews=Interview.unsafe.none(),
        comment='',
        **populate_db_proposals['proposal_data']
    )

    assert (
        Application.unsafe.filter(
            vacancy=populate_db_proposals['vacancy_fit'].id,
        )
        .exists()
    )
    assert not (
        Application.unsafe.filter(
            vacancy=populate_db_proposals['vacancy_not_fit'].id,
        )
        .exists()
    )


@pytest.mark.parametrize('recruiters', (
    [],
    None,
    [f.create_recruiter, f.create_recruiter],
))
def test_update_candidate_recruiters(recruiters):
    instance = f.create_candidate_with_responsibles()
    candidate_recruiters = instance.recruiters
    responsibles_by_role = {
        'main_recruiter': None,
        'recruiters': [factory() for factory in recruiters] if recruiters else recruiters,
        'responsibles': None,
    }
    if recruiters is None:
        expected_recruiters = candidate_recruiters
    else:
        expected_recruiters = responsibles_by_role['recruiters']

    update_candidate_responsibles(instance, responsibles_by_role)

    assert set(instance.recruiters) == set(expected_recruiters)


@patch(prefix + 'candidates.controllers.update_reference_issues_with_candidate_recruiters.delay')
@patch(prefix + 'candidates.controllers.update_rotation_issues_with_candidate_recruiters.delay')
def test_update_candidate_recruiters_in_issues(first_task, second_task):
    candidate = f.create_candidate_with_responsibles()
    new_main_recruiter = f.create_recruiter()
    data = {'main_recruiter': new_main_recruiter}

    update_or_create_candidate(data, instance=candidate)

    assert first_task.called_once_with((candidate.id, True, False))
    assert second_task.called_once_with((candidate.id, True, False))


@pytest.mark.usefixtures('language_tags_fixture')
def test_cant_create_existing_language_tag():
    with pytest.raises(Exception):
        f.LanguageTagFactory(tag='ru', native='русский', name_en='Russian', name_ru='русский')


def language_tag(tag_str):
    if tag_str is not None:
        return LanguageTag.objects.get(tag=tag_str)
    return None


@pytest.mark.parametrize('is_main1, is_main2', [
    (is_main1, is_main2)
    for is_main1 in [True, False]
    for is_main2 in [True, False]
])
@pytest.mark.usefixtures('language_tags_fixture')
def test_cannot_add_two_same_languages_to_candidate(is_main1, is_main2):
    candidate = f.CandidateFactory()
    f.CandidateLanguageTagFactory(candidate=candidate, tag=language_tag('ru'), is_main=is_main1)
    with pytest.raises(Exception):
        f.CandidateLanguageTagFactory(candidate=candidate, tag=language_tag('ru'), is_main=is_main2)


@pytest.mark.usefixtures('language_tags_fixture')
def test_cannot_add_two_main_languages_to_candidate():
    candidate = f.CandidateFactory()
    f.CandidateLanguageTagFactory(candidate=candidate, tag=language_tag('en'), is_main=True)
    with pytest.raises(Exception):
        f.CandidateLanguageTagFactory(candidate=candidate, tag=language_tag('ru'), is_main=True)


@pytest.mark.parametrize('init_langs, new_langs, main_result, spoken_result', [
    ([None], [None], None, []),
    ([None], ['ru'], 'ru', []),
    (['ru'], [None], None, []),
    (['ru'], ['ru'], 'ru', []),
    ([None], [None, 'ru'], None, ['ru']),
    ([None], ['en', 'en', 'en'], 'en', []),
    ([None], ['en', 'en', 'ru'], 'en', ['ru']),
    ([None], ['ru', 'en'], 'ru', ['en']),
    (['ru'], [None, 'ru'], None, ['ru']),
    (['ru'], [None, 'en'], None, ['en']),
    (['ru'], ['ru', 'en'], 'ru', ['en']),
    (['ru', 'en'], [None], None, []),
    (['ru', 'en'], ['ru'], 'ru', []),
    (['ru', 'en'], [None, 'en'], None, ['en']),
    (['ru', 'en'], ['ru', 'en'], 'ru', ['en']),
    (['ru', 'en'], ['en', 'ru'], 'en', ['ru']),
    (['ru', 'en'], ['en', 'en', 'en'], 'en', []),
    (['ru', 'en'], ['en', 'en', 'ru'], 'en', ['ru']),
    (['ru', 'fr', 'de'], ['en', 'bg', 'el'], 'en', ['bg', 'el']),
    (['ru', 'fr', 'de'], [None, 'en', 'bg', 'el'], None, ['en', 'bg', 'el']),
])
@pytest.mark.usefixtures('language_tags_fixture')
def test_candidate_polyglot(init_langs, new_langs, main_result, spoken_result):
    candidate = f.CandidateFactory()
    if init_langs[0]:
        f.CandidateLanguageTagFactory(candidate=candidate,
                                      tag=language_tag(init_langs[0]),
                                      is_main=True)
    for lang in init_langs[1:]:
        f.CandidateLanguageTagFactory(candidate=candidate,
                                      tag=language_tag(lang),
                                      is_main=False)
    polyglot = CandidatePolyglot(candidate)
    polyglot.update_known_languages(
        main_language=language_tag(new_langs[0]),
        spoken_languages=list(map(language_tag, new_langs[1:])),
    )
    if main_result:
        assert candidate.main_language.tag == main_result
    else:
        assert not candidate.main_language
    spoken_languages = candidate.spoken_languages
    assert sorted(x.tag for x in spoken_languages) == sorted(spoken_result)


@pytest.mark.usefixtures('language_tags_fixture')
def test_candidate_tag_eq_context():
    candidate: Candidate = f.CandidateFactory()
    for tag_str in ['ru', 'en', 'el', 'fr']:
        f.CandidateLanguageTagFactory(
            candidate=candidate,
            tag=LanguageTag.objects.get(tag=tag_str),
        )
    candidate_tags = list(candidate.candidate_language_tags.all())
    eq_context = CandidateLanguageTagEqContext(
        candidate_id=candidate.id,
        tag_id=candidate_tags[0].tag.id,
        is_main=False
    )

    assert eq_context in [CandidateLanguageTagEqContext(tag=x) for x in candidate_tags]
