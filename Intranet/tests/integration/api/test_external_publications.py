import json

import pytest

from unittest.mock import patch

from constance.test import override_config
from django.test import override_settings
from django.urls.base import reverse
from waffle.models import Switch

from intranet.femida.src.publications.choices import PUBLICATION_STATUSES, PUBLICATION_TYPES
from intranet.femida.src.vacancies.choices import VACANCY_PRO_LEVELS

from intranet.femida.tests import factories as f
from intranet.femida.tests.clients import APIClient
from intranet.femida.tests.utils import patch_service_permissions


pytestmark = pytest.mark.django_db


def test_external_publication_detail_without_service_ticket():
    publication = f.ExternalPublicationFactory(status=PUBLICATION_STATUSES.archived)
    client = APIClient()
    user = f.create_user()
    client.login(user.username)
    url = reverse('private-api:publications:detail', kwargs={'pk': publication.id})
    response = client.get(url)
    assert response.status_code == 403


@patch_service_permissions({})
def test_external_publication_detail_without_permission(tvm_jobs_client):
    publication = f.ExternalPublicationFactory(status=PUBLICATION_STATUSES.archived)
    url = reverse('private-api:publications:detail', kwargs={'pk': publication.id})
    response = tvm_jobs_client.get(url)
    assert response.status_code == 403


@pytest.mark.parametrize('permissions, status_code', (
    ([], 403),
    (["permissions.can_view_external_publications"], 200),
))
def test_external_publication_detail_with_constance(tvm_jobs_client_without_permissions,
                                                    permissions, status_code):
    """
    Проверяет доступы на обратную совместимость (права в constance)
    """
    Switch.objects.update_or_create(name='enable_tirole', defaults={'active': False})
    tvm_id = tvm_jobs_client_without_permissions._tvm_client_id
    publication = f.ExternalPublicationFactory(status=PUBLICATION_STATUSES.archived)
    url = reverse('private-api:publications:detail', kwargs={'pk': publication.id})
    with override_config(SERVICE_PERMISSIONS=json.dumps({tvm_id: permissions})):
        response = tvm_jobs_client_without_permissions.get(url)
    assert response.status_code == status_code


def test_external_publication_detail(tvm_jobs_client):
    vacancy = f.create_heavy_vacancy()
    f.VacancySkillFactory(vacancy=vacancy, skill__is_public=True)
    publication = f.ExternalPublicationFactory(
        status=PUBLICATION_STATUSES.archived,
        vacancy=vacancy,
    )
    url = reverse('private-api:publications:detail', kwargs={'pk': publication.id})
    response = tvm_jobs_client.get(url)
    assert response.status_code == 200, response.content
    response_data = response.json()

    assert response_data['id'] == publication.id
    vacancy_data = response_data['vacancy']

    expected_skills = [skill.id for skill in vacancy.skills.filter(is_public=True)]
    expected_cities = [city.id for city in vacancy.cities.all()]
    assert [skill['id'] for skill in vacancy_data['skills']] == expected_skills
    assert [city['id'] for city in vacancy_data['cities']] == expected_cities


def test_priorities_in_external_publication_filter_form(tvm_jobs_client):
    url = reverse('private-api:publications:filter-form')

    prof_sphere1 = f.ProfessionalSphereFactory()
    prof_sphere2 = f.ProfessionalSphereFactory()
    pub_prof_sphere1 = f.PublicProfessionalSphereFactory()
    pub_prof_sphere2 = f.PublicProfessionalSphereFactory()

    profession1 = f.ProfessionFactory(professional_sphere=prof_sphere1)
    profession2 = f.ProfessionFactory(professional_sphere=prof_sphere1)
    profession3 = f.ProfessionFactory(professional_sphere=prof_sphere2)

    public_profession1 = f.PublicProfessionFactory(
        public_professional_sphere=pub_prof_sphere1,
        professions=[profession1, profession2],
    )
    public_profession2 = f.PublicProfessionFactory(
        public_professional_sphere=pub_prof_sphere2,
        professions=[profession3],
    )

    city1 = f.CityFactory()
    city2 = f.CityFactory()

    vacancy1 = f.VacancyFactory(profession=profession1, professional_sphere=prof_sphere1)
    vacancy2 = f.VacancyFactory(profession=profession2, professional_sphere=prof_sphere1)
    f.VacancyFactory(profession=profession3, professional_sphere=prof_sphere2)

    f.VacancyCityFactory(vacancy=vacancy1, city=city1)
    f.VacancyCityFactory(vacancy=vacancy2, city=city2)

    pub_service1 = f.PublicServiceFactory()
    pub_service2 = f.PublicServiceFactory()

    f.PublicationFactory(
        vacancy=vacancy1,
        public_service=pub_service1,
        type=PUBLICATION_TYPES.external,
        status=PUBLICATION_STATUSES.published,
    )
    f.PublicationFactory(
        vacancy=vacancy2,
        public_service=pub_service2,
        type=PUBLICATION_TYPES.external,
        status=PUBLICATION_STATUSES.published,
    )
    response = tvm_jobs_client.get(url)
    assert response.status_code == 200, str(response.content)
    response_data = response.json()['structure']

    # professional_spheres всё ещё возвращают id вместо slug
    correct_priorities = {
        'cities': {city1.slug: 1, city2.slug: 1},
        'professions': {profession1.slug: 1, profession2.slug: 1, profession3.slug: 0},
        'public_professions': {public_profession1.slug: 2, public_profession2.slug: 0},
        'professional_spheres': {prof_sphere1.id: 2, prof_sphere2.id: 0},
        'public_professional_spheres': {pub_prof_sphere1.id: 2, pub_prof_sphere2.id: 0},
        'services': {pub_service1.slug: 1, pub_service2.slug: 1},
    }

    for city in response_data['cities']['choices']:
        assert city['data']['priority'] == correct_priorities['cities'][city['value']]

    for prof in response_data['professions']['choices']:
        assert prof['data']['priority'] == correct_priorities['professions'][prof['value']]
        sphere = prof['data']['professional_sphere']
        assert sphere['priority'] == correct_priorities['professional_spheres'][sphere['id']]

    for pub_prof in response_data['public_professions']['choices']:
        correct_value = correct_priorities['public_professions'][pub_prof['value']]
        assert pub_prof['data']['priority'] == correct_value
        pub_sphere = pub_prof['data']['professional_sphere']
        correct_priority = correct_priorities['public_professional_spheres'][pub_sphere['id']]
        assert pub_sphere['priority'] == correct_priority

    for service in response_data['services']['choices']:
        assert service['data']['priority'] == correct_priorities['services'][service['value']]


def test_external_publication_filter_form_with_params(tvm_jobs_client):
    url = reverse('private-api:publications:filter-form')
    params = {
        'professions': [f.ProfessionFactory().slug],
        'public_professions': [f.PublicProfessionFactory(professions=[f.ProfessionFactory()]).slug],
        'cities': [f.CityFactory().slug],
        'services': [f.PublicServiceFactory().slug],
        'skills': [f.SkillFactory(is_public=True).id],
        'pro_levels': ['chief', 'senior'],
        'employment_types': ['intern', 'remote'],
    }
    response = tvm_jobs_client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()['data']

    for param in params:
        assert set(response_data[param]['value']) == set(params[param]), response_data


@pytest.mark.parametrize(
    'search_text, result_count',
    [
        ('в москве', 0),
        ('sepulki', 0),
        ('moscow', 2),
        ('market', 1),
        ('developer', 2),
    ]
)
@patch('intranet.femida.src.utils.begemot.MisspellAPI._request', return_value={'code': 200})
def test_external_publication_filter_by_text_search(mocked__request, tvm_jobs_client, search_text, result_count):
    f.create_waffle_switch('enable_publication_text_search_spellcheck')
    url = reverse('private-api:publications:list')

    f.PublicationFactory(
        type=PUBLICATION_TYPES.external,
        status=PUBLICATION_STATUSES.published,
        title='vacancy1',
        search_vector_ru="'backend':18B 'develop':19B 'django':13B,15B 'market':3B 'minsk':8B,9B",
        search_vector_en=(
            "'backend':11B 'develop':12B,14B 'django':8B,10B 'market':2B 'minsk':3B,4B "
            "'moscow':5B,6B 'python':7B,9B 'softwar':13B 'ааа':15C"
        )
    )
    f.PublicationFactory(
        type=PUBLICATION_TYPES.external,
        status=PUBLICATION_STATUSES.published,
        title='vacancy2',
        search_vector_ru="'cloud':4B 'moscow':6B 'вв':8C 'москв':5B 'облак':2B 'разработк':7B 'яндекс':1B,3B",
        search_vector_en="'cloud':3B 'develop':6B 'moscow':4B 'softwar':5B 'в':1A,8 'вв':7C 'яндекс':2B",
    )

    params = {'text': search_text}
    response = tvm_jobs_client.get(url, params)

    assert response.status_code == 200, response.content
    mocked__request.assert_called_once_with(params, None)

    response_results = response.json()['results']
    assert len(response_results) == result_count


@patch('intranet.femida.src.utils.begemot.MisspellAPI.get_spellcheck')
@override_settings(CITY_HOMEWORKER_ID=100500)
def test_external_publication_list(mocked_get_spellcheck, tvm_jobs_client, settings):
    f.create_waffle_switch('enable_publication_text_search_spellcheck')
    skill = f.SkillFactory(is_public=True)
    city = f.CityFactory(id=settings.CITY_HOMEWORKER_ID)
    other_city = f.CityFactory()
    service = f.PublicServiceFactory()
    profession = f.ProfessionFactory()
    public_profession = f.PublicProfessionFactory(professions=[profession])
    pro_level_min = VACANCY_PRO_LEVELS.lead
    pro_level_max = VACANCY_PRO_LEVELS.expert
    suitable_publications_count = 3
    is_chief = True
    not_chief = not is_chief
    data = [(skill, [city, other_city], profession, pro_level_min, pro_level_max, is_chief)]
    data *= suitable_publications_count
    data += (
        (f.SkillFactory(is_public=True), [city, other_city], profession, pro_level_min, pro_level_max, is_chief),
        (skill, [f.CityFactory()], profession, pro_level_min, pro_level_max, is_chief),
        (skill, [city, other_city], f.ProfessionFactory(), pro_level_min, pro_level_max, is_chief),
        (skill, [city, other_city], profession, VACANCY_PRO_LEVELS.junior, VACANCY_PRO_LEVELS.middle, not_chief),
    )
    publications = []
    for i, (s, c, p, level_min, level_max, chief) in enumerate(data):
        vacancy = f.VacancyFactory(profession=p, pro_level_min=level_min, pro_level_max=level_max)
        f.VacancySkillFactory(vacancy=vacancy, skill=s)
        for city in c:
            f.VacancyCityFactory(vacancy=vacancy, city=city)

        publications.append(
            f.ExternalPublicationFactory(
                status=PUBLICATION_STATUSES.published,
                public_service=service,
                vacancy=vacancy,
                published_at='2021-01-01',
                priority=-i,
                is_chief=chief,
            ),
        )

    params = {
        'professions': [profession.slug],
        'public_professions': [public_profession.slug],
        'cities': [other_city.slug],
        'services': [service.slug],
        'skills': [skill.id],
        'pro_levels': ['chief', 'senior'],
        'employment_types': ['remote'],
    }
    url = reverse('private-api:publications:list')
    response = tvm_jobs_client.get(url, params)
    assert response.status_code == 200, response.content

    response_data = response.json()
    expected_publications = sorted(
        publications[:suitable_publications_count],
        key=lambda x: (x.priority, x.published_at, x.id),
        reverse=True,
    )
    expected_publication_ids = [p.id for p in expected_publications]
    assert expected_publication_ids == [item['id'] for item in response_data['results']]
    mocked_get_spellcheck.assert_not_called()


@pytest.mark.parametrize('status, type, is_found', (
    (PUBLICATION_STATUSES.published, PUBLICATION_TYPES.external, True),
    (PUBLICATION_STATUSES.published, PUBLICATION_TYPES.internal, False),
    (PUBLICATION_STATUSES.archived, PUBLICATION_TYPES.external, False),
))
def test_external_publication_list_unfiltered(tvm_jobs_client, status, type, is_found):
    f.PublicationFactory(status=status, type=type)
    url = reverse('private-api:publications:list')
    response = tvm_jobs_client.get(url)
    assert response.status_code == 200, response.content
    assert bool(response.json()['results']) == is_found


@pytest.mark.parametrize('field_name', ('text', 'cities'))
def test_external_publication_list_filter_by_null(tvm_jobs_client, field_name):
    url = reverse('private-api:publications:list')
    response = tvm_jobs_client.get(url, {field_name: '\x00'})
    assert response.status_code == 400, response.content
    assert response.json()['errors'][field_name][0]['code'] == 'null_characters_not_allowed'


def test_priorities_in_external_cities_list(tvm_jobs_client):
    url = reverse('private-api:cities:list')

    city1 = f.CityFactory(name_en='city1')
    city2 = f.CityFactory(name_en='city2')

    vacancy1 = f.VacancyFactory()
    vacancy2 = f.VacancyFactory()
    vacancy3 = f.VacancyFactory()

    f.VacancyCityFactory(vacancy=vacancy1, city=city1)
    f.VacancyCityFactory(vacancy=vacancy1, city=city2)
    f.VacancyCityFactory(vacancy=vacancy2, city=city1)
    f.VacancyCityFactory(vacancy=vacancy2, city=city2)
    f.VacancyCityFactory(vacancy=vacancy3, city=city1)

    service1 = f.PublicServiceFactory()
    service2 = f.PublicServiceFactory()

    f.PublicationFactory(
        vacancy=vacancy1,
        public_service=service1,
        status=PUBLICATION_STATUSES.published,
        type=PUBLICATION_TYPES.external,
    )
    f.PublicationFactory(
        vacancy=vacancy2,
        public_service=service2,
        status=PUBLICATION_STATUSES.published,
        type=PUBLICATION_TYPES.external,
    )
    f.PublicationFactory(
        vacancy=vacancy3,
        public_service=service1,
        status=PUBLICATION_STATUSES.published,
        type=PUBLICATION_TYPES.external,
    )

    correct_priorities = {
        (city1.id, service1.id): 2,  # публикации 1-й и 3-й вакансий в одном городе
        (city1.id, service2.id): 1,
        (city2.id, service1.id): 1,
        (city2.id, service2.id): 1,
    }

    response = tvm_jobs_client.get(url)
    assert response.status_code == 200, response.content
    results = response.json()['results']
    for city in results:
        assert 'services' in city, city
        for service in city['services']:
            assert service['priority'] == correct_priorities[(city['id'], service['id'])]


def test_public_professions_is_active_flag(tvm_jobs_client):
    prof_sphere = f.ProfessionalSphereFactory()
    profession = f.ProfessionFactory(professional_sphere=prof_sphere)

    public_profession = f.PublicProfessionFactory(
        professions=[profession],
    )
    f.PublicProfessionFactory(
        professions=[profession],
        is_active=False,
    )
    vacancy = f.VacancyFactory(profession=profession, professional_sphere=prof_sphere)
    f.VacancyCityFactory(vacancy=vacancy, city=f.CityFactory())
    pub_service = f.PublicServiceFactory()
    f.PublicationFactory(
        vacancy=vacancy,
        public_service=pub_service,
        type=PUBLICATION_TYPES.external,
        status=PUBLICATION_STATUSES.published,
    )
    url = reverse('private-api:publications:list')
    response = tvm_jobs_client.get(url, data={'professions': profession.slug})
    assert response.status_code == 200, str(response.content)
    response_data = response.json()['results']
    assert len(response_data) == 1
    pub_prof_list = response_data[0]['vacancy']['public_professions']
    assert len(pub_prof_list) == 1
    assert pub_prof_list[0]['slug'] == public_profession.slug


def test_public_professions_is_active_flag_in_filter_form(tvm_jobs_client):
    profession = f.ProfessionFactory()
    public_profession = f.PublicProfessionFactory(professions=[profession])
    f.PublicProfessionFactory(
        professions=[profession],
        is_active=False,
    )
    f.PublishedExternalPublicationFactory(vacancy=f.VacancyFactory(profession=profession))
    url = reverse('private-api:publications:filter-form')
    response = tvm_jobs_client.get(url)
    assert response.status_code == 200, str(response.content)
    response_data = response.json()['structure']
    assert len(response_data['public_professions']['choices']) == 1
    assert response_data['public_professions']['choices'][0]['value'] == public_profession.slug


def test_inactive_public_professions_resolution(tvm_jobs_client):
    prof1 = f.ProfessionFactory()
    prof2 = f.ProfessionFactory()
    prof3 = f.ProfessionFactory()
    pub1 = f.PublicationFactory(
        vacancy=f.VacancyFactory(profession=prof1),
        type=PUBLICATION_TYPES.external,
        status=PUBLICATION_STATUSES.published,
    )
    pub2 = f.PublicationFactory(
        vacancy=f.VacancyFactory(profession=prof2),
        type=PUBLICATION_TYPES.external,
        status=PUBLICATION_STATUSES.published,
    )
    pub3 = f.PublicationFactory(
        vacancy=f.VacancyFactory(profession=prof3),
        type=PUBLICATION_TYPES.external,
        status=PUBLICATION_STATUSES.published,
    )
    pub_prof1 = f.PublicProfessionFactory(professions=[prof1, prof2])
    pub_prof2 = f.PublicProfessionFactory(professions=[prof2, prof3])
    expected_public_profs = {
        pub1.id: {pub_prof1.slug},
        pub2.id: {pub_prof1.slug, pub_prof2.slug},
    }
    ina_pub_prof1 = f.PublicProfessionFactory(is_active=False, professions=[prof1])
    ina_pub_prof2 = f.PublicProfessionFactory(is_active=False, professions=[prof2])
    expected_results = {
        # inactive(prof1) -> prof1(active1) prof2(active1, active2)
        ina_pub_prof1.slug: expected_public_profs,
        # inactive(prof2) -> prof1(active1), prof2(active1, active2), prof3(active2)
        ina_pub_prof2.slug: expected_public_profs | {pub3.id: {pub_prof2.slug}},
    }
    url = reverse('private-api:publications:list')
    for ina_pub_prof_slug, expected_pubs in expected_results.items():
        response = tvm_jobs_client.get(url, data={'public_professions': ina_pub_prof_slug})
        assert response.status_code == 200, str(response.content)
        response_data = response.json()['results']
        pub_profs_by_pub_id = {p['id']: p['vacancy']['public_professions'] for p in response_data}
        for expected_pub_id, expected_profs in expected_pubs.items():
            assert expected_pub_id in pub_profs_by_pub_id
            assert set(p['slug'] for p in pub_profs_by_pub_id[expected_pub_id]) == expected_profs
