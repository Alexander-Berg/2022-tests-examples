import pytest

from django.urls.base import reverse

from intranet.femida.src.publications.choices import PUBLICATION_STATUSES
from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_public_profession_list(tvm_jobs_client):
    url = reverse('private-api:public-professions:list')
    public_profession = f.PublicProfessionFactory()
    response = tvm_jobs_client.get(url)
    assert response.status_code == 200
    assert response.json()['results'][0]['slug'] == public_profession.slug


@pytest.mark.parametrize('slug, status_code', (
    ('slug', 200),
    ('wrong', 404),
))
def test_public_profession_detail(tvm_jobs_client, slug, status_code):
    f.PublicProfessionFactory(slug='slug')
    url = reverse('private-api:public-professions:detail', kwargs={'slug': slug})
    response = tvm_jobs_client.get(url)
    assert response.status_code == status_code


@pytest.mark.parametrize('publications_count', (0, 1))
def test_publication_count_field(tvm_jobs_client, publications_count):
    profession = f.ProfessionFactory()
    f.PublicProfessionFactory(professions=[profession])
    vacancy = f.VacancyFactory(profession=profession)
    for i in range(publications_count):
        f.ExternalPublicationFactory(vacancy=vacancy, status=PUBLICATION_STATUSES.published)
    url = reverse('private-api:public-professions:list')
    response = tvm_jobs_client.get(url)
    assert response.json()['results'][0]['publications_count'] == publications_count
