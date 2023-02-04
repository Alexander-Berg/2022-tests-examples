import pytest

from django.urls.base import reverse

from intranet.femida.src.publications.choices import PUBLICATION_STATUSES
from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_city_list(tvm_jobs_client):
    url = reverse('private-api:cities:list')
    response = tvm_jobs_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('slug, status_code', (
    ('slug', 200),
    ('wrong', 404),
))
def test_city_detail(tvm_jobs_client, slug, status_code):
    f.CityFactory(slug='slug')
    url = reverse('private-api:cities:detail', kwargs={'slug': slug})
    response = tvm_jobs_client.get(url)
    assert response.status_code == status_code


def test_services_field(tvm_jobs_client):
    service = f.PublicServiceFactory()
    vacancy = f.VacancyFactory()
    city = f.CityFactory(slug='slug')
    f.VacancyCityFactory(vacancy=vacancy, city=city)
    f.ExternalPublicationFactory(
        vacancy=vacancy,
        public_service=service,
        status=PUBLICATION_STATUSES.published,
    )
    url = reverse('private-api:cities:list')
    response = tvm_jobs_client.get(url)
    assert response.json()['results'][0]['services'][0]['slug'] == service.slug


@pytest.mark.parametrize('publications_num', (0, 1))
def test_publication_count_field(tvm_jobs_client, publications_num):
    vacancy = f.VacancyFactory()
    city = f.CityFactory(slug='slug')
    f.VacancyCityFactory(vacancy=vacancy, city=city)
    for i in range(publications_num):
        f.ExternalPublicationFactory(
            vacancy=vacancy,
            status=PUBLICATION_STATUSES.published,
        )
    url = reverse('private-api:cities:list')
    response = tvm_jobs_client.get(url)
    assert response.json()['results'][0]['publications_count'] == publications_num
