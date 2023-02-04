import pytest

from django.urls.base import reverse

from intranet.femida.src.vacancies.choices import VACANCY_STATUSES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


STRING = 'abcde'


@pytest.mark.parametrize('data', [
    ('candidate', lambda: f.CandidateFactory.create(first_name=STRING)),
    ('vacancy', lambda: f.create_active_vacancy(name=STRING)),
    ('publication', lambda: f.VacancyFactory.create(publication_title=STRING, is_published=True)),
    ('application', lambda: f.ApplicationFactory.create(
        vacancy=f.create_active_vacancy(name=STRING)
    )),
    ('skill', lambda: f.SkillFactory.create(name=STRING)),
    ('profession', lambda: f.ProfessionFactory.create(name=STRING)),
    ('position', lambda: f.PositionFactory.create(name_en=STRING)),
    ('city', lambda: f.CityFactory.create(name_en=STRING)),
    ('category', lambda: f.CategoryFactory.create(name=STRING)),
    ('problem', lambda: f.ProblemFactory.create(summary=STRING)),
    ('tag', lambda: f.TagFactory.create(name=STRING))
])
def test_suggest(su_client, data):
    url = reverse('api:suggest:suggest')
    _type, initialize = data
    initialize()
    response = su_client.get(url, {
        'types': _type,
        'q': 'bcd',
    })
    assert response.status_code == 200
    assert len(response.json()) == 1


@pytest.mark.parametrize('statuses, expected_statuses', (
    ('', {VACANCY_STATUSES.in_progress, VACANCY_STATUSES.offer_processing}),
    ('in_progress,closed', {VACANCY_STATUSES.in_progress, VACANCY_STATUSES.closed}),
    ('*', {
        VACANCY_STATUSES.in_progress,
        VACANCY_STATUSES.offer_processing,
        VACANCY_STATUSES.closed,
    }),
))
def test_vacancy_suggest(su_client, statuses, expected_statuses):
    f.VacancyFactory(status=VACANCY_STATUSES.in_progress, name=STRING)
    f.VacancyFactory(status=VACANCY_STATUSES.offer_processing, name=STRING)
    f.VacancyFactory(status=VACANCY_STATUSES.closed, name=STRING)
    url = reverse('api:suggest:suggest')
    response = su_client.get(url, {
        'types': 'vacancy',
        'q': 'bcd',
        'statuses': statuses,
    })
    assert response.status_code == 200
    result_statuses = {i['status'] for i in response.json()}
    assert result_statuses == expected_statuses


@pytest.mark.parametrize('is_deleted, found', (
    (True, 0),
    (False, 1),
))
def test_position_suggest(su_client, is_deleted, found):
    f.PositionFactory(name_en=STRING, is_deleted=is_deleted)
    url = reverse('api:suggest:suggest')
    response = su_client.get(url, {
        'types': 'position',
        'q': 'bcd',
    })
    assert response.status_code == 200, response.content
    assert len(response.json()) == found
