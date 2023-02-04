import pytest

from django.urls.base import reverse

from intranet.femida.src.vacancies.choices import VACANCY_ROLES
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_user_list(su_client):
    url = reverse('api:users:list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_problem_fan_add(su_client):
    problem = f.create_problem()
    url = reverse('api:users:favorite-problem', kwargs={'pk': problem.id})
    response = su_client.put(url)
    assert response.status_code == 204


def test_problem_fan_remove(su_client):
    problem = f.create_problem()
    user = f.get_superuser()
    problem.fans.add(user)
    url = reverse('api:users:favorite-problem', kwargs={'pk': problem.id})
    response = su_client.delete(url)
    assert response.status_code == 204


def test_interviewer_list_by_vacancy(su_client):
    vacancy = f.VacancyFactory()
    interviewers = [
        f.VacancyMembershipFactory(vacancy=vacancy, role=VACANCY_ROLES.interviewer).member
        for _ in range(3)
    ]

    f.InterviewFactory(
        interviewer=interviewers[1],
        event_start_time=shifted_now(days=1),
    )
    f.InterviewFactory(
        interviewer=interviewers[2],
        finished=shifted_now(days=-1),
    )
    f.InterviewFactory(
        interviewer=interviewers[2],
        finished=shifted_now(days=-1),
    )

    url = reverse('api:users:interviewers-list')
    response = su_client.get(url, {
        'vacancy': vacancy.id,
    })
    assert response.status_code == 200, response.content
    response_data = response.json()
    results = response_data['results']

    assert response_data['count'] == 3
    assert results[0]['interviews_finished_2weeks_count'] == 0
    assert results[0]['interviews_planned_count'] == 0
    assert results[1]['interviews_planned_count'] == 1
    assert results[2]['interviews_finished_2weeks_count'] == 2

    for i, result in enumerate(results):
        assert result['username'] == interviewers[i].username


@pytest.mark.parametrize('query_param', (
    'aa_type',
    'user',
))
def test_interviewer_list(su_client, query_param):
    user = f.create_aa_interviewer('ml')
    url = reverse('api:users:interviewers-list')
    response = su_client.get(url, {
        query_param: user.username if query_param == 'user' else user.aa_type,
    })
    assert response.status_code == 200, response.content
    assert response.json()['count'] == 1
