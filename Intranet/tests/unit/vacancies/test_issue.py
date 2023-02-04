import pytest

from intranet.femida.src.vacancies.choices import VACANCY_TYPES, VACANCY_PRO_LEVELS
from intranet.femida.tests import factories as f
from intranet.femida.src.vacancies.startrek.issues import get_job_issue_summary


@pytest.mark.parametrize(
    'type, pro_level_min, pro_level_max, expected_string',
    (
        (
            VACANCY_TYPES.new,
            VACANCY_PRO_LEVELS.intern,
            VACANCY_PRO_LEVELS.expert,
            'Новая ({profession_name_ru}: Стажер — Эксперт) / New ({profession_name_en}: Intern — Expert)',
        ),
        (
            VACANCY_TYPES.replacement,
            VACANCY_PRO_LEVELS.intern,
            VACANCY_PRO_LEVELS.expert,
            'Замена ({profession_name_ru}: Стажер — Эксперт) / Replacement ({profession_name_en}: Intern — Expert)',
        ),
        (
            VACANCY_TYPES.replacement,
            VACANCY_PRO_LEVELS.expert,
            VACANCY_PRO_LEVELS.expert,
            'Замена ({profession_name_ru}: Эксперт) / Replacement ({profession_name_en}: Expert)',
        ),
        (
            VACANCY_TYPES.internship,
            VACANCY_PRO_LEVELS.intern,
            VACANCY_PRO_LEVELS.junior,
            'Стажировка ({profession_name_ru}: Стажер — Младший) / Internship ({profession_name_en}: Intern — Junior)',
        ),
    ),
)
def test_get_job_issue_summary(type, pro_level_min, pro_level_max, expected_string):
    profession = f.ProfessionFactory()
    expected_string = expected_string.format(
        profession_name_ru=profession.name_ru,
        profession_name_en=profession.name_en,
    )

    vacancy = f.VacancyFactory.create(
        type=type,
        profession=profession,
        pro_level_min=pro_level_min,
        pro_level_max=pro_level_max
    )

    summary = get_job_issue_summary(vacancy)

    assert summary == expected_string
