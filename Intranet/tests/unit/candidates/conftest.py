import pytest

from intranet.femida.src.professions.models import Profession
from intranet.femida.src.vacancies.choices import VACANCY_PRO_LEVELS

from intranet.femida.tests import factories as f


@pytest.fixture
def populate_db_proposals():
    candidate = f.CandidateFactory.create()
    candidate_city = f.CandidateCityFactory.create(candidate=candidate)
    candidate_profession = f.CandidateProfessionFactory.create(candidate=candidate)
    candidate_skill = f.CandidateSkillFactory.create(candidate=candidate)

    proposal_data = {
        'pro_level_min': VACANCY_PRO_LEVELS.junior,
        'pro_level_max': VACANCY_PRO_LEVELS.expert,
        'cities': candidate.target_cities.all(),
        'skills': candidate.skills.all(),
        'professions': Profession.objects.filter(id=candidate_profession.profession.id),
    }

    vacancy_fit = f.VacancyFactory.create(
        profession=candidate_profession.profession,
        pro_level_min=proposal_data['pro_level_min'],
        pro_level_max=proposal_data['pro_level_max'],
    )
    f.VacancySkillFactory.create(
        vacancy=vacancy_fit,
        skill=candidate_skill.skill,
    )
    f.VacancyCityFactory.create(
        vacancy=vacancy_fit,
        city=candidate_city.city,
    )

    return {
        'candidate': candidate,
        'vacancy_fit': vacancy_fit,
        'vacancy_not_fit': f.VacancyFactory.create(),
        'proposal_data': proposal_data,
    }


@pytest.fixture
def populate_db_vacancies_variants():
    departments = {
        'first': f.DepartmentFactory(tags=[]),
        'second': f.DepartmentFactory(tags=[]),
    }
    departments['first_child'] = f.DepartmentFactory(tags=[], ancestors=[departments['first'].id])

    vacancies = {
        'simple': f.VacancyFactory.create(),
        'first_dept': f.VacancyFactory.create(department=departments['first']),
        'first_child_dept': f.VacancyFactory.create(department=departments['first_child']),
    }

    return {
        'vacancies': vacancies,
        'departments': departments,
    }
