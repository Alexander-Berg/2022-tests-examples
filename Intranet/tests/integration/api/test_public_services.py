import pytest

from django.urls.base import reverse

from intranet.femida.src.publications.choices import PUBLICATION_STATUSES
from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_public_service_list(tvm_jobs_client):
    service = f.PublicServiceFactory()

    city1 = f.CityFactory()
    city2 = f.CityFactory()

    vacancy1 = f.VacancyFactory()
    vacancy2 = f.VacancyFactory()
    vacancy3 = f.VacancyFactory()

    f.VacancyCityFactory(vacancy=vacancy1, city=city1)
    f.VacancyCityFactory(vacancy=vacancy2, city=city2)
    f.VacancyCityFactory(vacancy=vacancy3, city=city2)

    f.ExternalPublicationFactory(
        vacancy=vacancy1,
        public_service=service,
        status=PUBLICATION_STATUSES.published,
    )
    f.ExternalPublicationFactory(
        vacancy=vacancy2,
        public_service=service,
        status=PUBLICATION_STATUSES.published,
    )
    f.ExternalPublicationFactory(
        vacancy=vacancy3,
        public_service=service,
        status=PUBLICATION_STATUSES.published,
    )

    url = reverse('private-api:services:list')
    response = tvm_jobs_client.get(url)
    assert response.status_code == 200, str(response.content)
    results = response.json()['results']
    assert len(results) == 1
    result_service = results[0]
    assert result_service['id'] == service.id
    assert result_service['publications_count'] == 3
    result_cities = result_service['cities']
    assert len(result_cities) == 2

    correct_priorities = {city1.id: 1, city2.id: 2}
    for city in result_cities:
        assert city['priority'] == correct_priorities[city['id']]


@pytest.mark.parametrize('slug, status_code', (
    ('slug', 200),
    ('wrong', 404),
))
def test_public_service_detail(tvm_jobs_client, slug, status_code):
    f.PublicServiceFactory(slug='slug')
    url = reverse('private-api:services:detail', kwargs={'slug': slug})
    response = tvm_jobs_client.get(url)
    assert response.status_code == status_code


@pytest.mark.parametrize('publications_num', (0, 1))
def test_publication_count_field(tvm_jobs_client, publications_num):
    service = f.PublicServiceFactory(slug='slug')
    for i in range(publications_num):
        f.ExternalPublicationFactory(public_service=service, status=PUBLICATION_STATUSES.published)
    url = reverse('private-api:services:detail', kwargs={'slug': service.slug})
    response = tvm_jobs_client.get(url)
    assert response.json()['publications_count'] == publications_num
