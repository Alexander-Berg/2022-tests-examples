import json
import pytest
import uuid

from django.urls.base import reverse

from intranet.femida.src.publications.choices import PUBLICATION_STATUSES
from intranet.femida.src.vacancies.models import PublicationSubscription

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_internal_publication_list(su_client):
    f.create_publication()
    url = reverse('api:publications:list')
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('external_url', (
    'https://yandex.ru/jobs/vacancies/dev/bs_back_dev/',
    'https://yandex.ru/jobs/vacancies/разработчик-бэкенда-баннерной-системы-4207',
    'https://yandex.ru/jobs/vacancies/разработчик-бэкенда-баннерной-системы-4207/survey/',
    'https://yandex.ru/jobs/vacancies/разработчик-бэкенда-баннерной-911-системы-4207',
    'https://yandex.ru/jobs/vacancies/4207',
    'https://yandex.ru/jobs/vacancies/4207/',
    'https://yandex.ru/jobs/vacancies/4207#main',
    'https://yandex.ru/jobs/vacancies/4207?utm_campaign=xx',
))
def test_internal_publications_list_filter_by_external_url(su_client, external_url):
    internal_publication = f.create_publication()
    f.create_publication(publication_title='Irrelevant')
    f.ExternalPublicationFactory(id=4207, vacancy=internal_publication)
    sf = f.SubmissionFormFactory(url='https://yandex.ru/jobs/vacancies/dev/bs_back_dev/')
    sf.vacancies.add(internal_publication)

    url = reverse('api:publications:list')
    response = su_client.get(url, {'external_url': external_url})

    assert response.status_code == 200
    results = response.json()['results']
    assert len(results) == 1
    assert results[0]['id'] == internal_publication.id


def test_internal_publication_filter_form(su_client):
    url = reverse('api:publications:filter-form')
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('is_published, status_code', (
    (True, 200),
    (False, 404),
))
def test_internal_publication_detail(su_client, is_published, status_code):
    vacancy = f.create_heavy_vacancy(is_published=is_published)
    url = reverse('api:publications:internal-detail', kwargs={'pk': vacancy.id})
    response = su_client.get(url)
    assert response.status_code == status_code


def test_publication_subscription_list(su_client):
    f.PublicationSubscriptionFactory(created_by=f.create_user())
    f.PublicationSubscriptionFactory(created_by=f.get_superuser())
    url = reverse('api:publications:publication-subscription-list')
    response = su_client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content)['count'] == 1


@pytest.mark.parametrize('is_forbidden, status_code', (
    (False, 200),
    (True, 403),
))
def test_publication_subscription_detail(client, is_forbidden, status_code):
    subscriber = f.create_user()
    initiator = f.create_user() if is_forbidden else subscriber
    publication_subscription = f.PublicationSubscriptionFactory(created_by=subscriber)
    url = reverse('api:publications:publication-subscription-detail', kwargs={
        'pk': publication_subscription.id,
    })
    client.login(initiator.username)
    response = client.get(url)
    assert response.status_code == status_code


@pytest.fixture()
def subscription_data():
    return {
        'professions': [f.ProfessionFactory().id],
        'pro_level_min': 1,
        'only_active': True,
        'pro_level_max': 5,
        'department': f.DepartmentFactory().id,
        'external_url': 'https://yandex.ru/jobs/vacancies/dev/bs_back_dev/',
        'skills': [f.SkillFactory().id],
        'cities': [f.CityFactory().id],
        'abc_services': [f.ServiceFactory().id],
    }


@pytest.mark.parametrize('full_data', (True, False))
def test_publication_subscription_create(su_client, subscription_data, full_data):
    url = reverse('api:publications:publication-subscription-list')
    data = subscription_data if full_data else {}
    response = su_client.post(url, data)
    assert response.status_code == 201
    assert PublicationSubscription.objects.count() == 1


def test_publication_subscription_create_failure_bad_filter(su_client):
    url = reverse('api:publications:publication-subscription-list')
    data = {
        'pro_level_min': 5,
        'pro_level_max': 1,
    }
    response = su_client.post(url, data)
    assert response.status_code == 400
    assert PublicationSubscription.objects.count() == 0


def test_publication_subscription_create_failure_on_duplicate(su_client, subscription_data):
    url = reverse('api:publications:publication-subscription-list')
    su_client.post(url, subscription_data)
    response = su_client.post(url, subscription_data)
    assert response.status_code == 400
    assert PublicationSubscription.objects.count() == 1


@pytest.mark.parametrize('is_forbidden, status_code, subscriptions_left', (
    (False, 204, 0),
    (True, 403, 1),
))
def test_publication_subscription_delete(client, is_forbidden, status_code, subscriptions_left):
    subscriber = f.create_user()
    initiator = f.create_user() if is_forbidden else subscriber
    publication_subscription = f.PublicationSubscriptionFactory(created_by=subscriber)
    url = reverse('api:publications:publication-subscription-detail', kwargs={
        'pk': publication_subscription.id,
    })
    client.login(initiator.username)
    response = client.delete(url)
    assert response.status_code == status_code
    assert PublicationSubscription.objects.count() == subscriptions_left


@pytest.mark.parametrize('is_wrong_uuid, status_code', (
    (False, 200),
    (True, 404),
))
def test_publication_detail(client, is_wrong_uuid, status_code):
    publication = f.PublicationFactory(status=PUBLICATION_STATUSES.published)
    pub_uuid = uuid.uuid4() if is_wrong_uuid else publication.uuid
    url = reverse('api:publications:detail', kwargs={'uuid': pub_uuid})
    response = client.get(url)
    assert response.status_code == status_code
