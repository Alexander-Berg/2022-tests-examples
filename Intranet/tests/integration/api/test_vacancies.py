import pytest

from unittest.mock import patch

from constance.test import override_config
from django.test import override_settings
from django.urls.base import reverse

from intranet.femida.src.staff.choices import GEOGRAPHY_KINDS
from intranet.femida.src.vacancies import choices
from intranet.femida.src.vacancies.models import Vacancy

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.startrek import EmptyIssue


pytestmark = pytest.mark.django_db

VS_ID = 1
BP_ID = 1234


patch_create_issue = patch(
    target='intranet.femida.src.vacancies.startrek.issues.create_issue',
    new=lambda *args, **kwargs: EmptyIssue(),
)


def _get_base_vacancy_create_data():
    profession = f.ProfessionFactory.create()
    location = f.LocationFactory.create()
    return {
        'name': 'Vacancy',
        'hiring_manager': f.UserFactory.create().username,
        'head': f.UserFactory.create().username,
        'department': f.DepartmentFactory.create().id,
        'pro_level_max': choices.VACANCY_PRO_LEVELS.expert,
        'value_stream': f.ValueStreamFactory.create(oebs_product_id=1).id,
        'locations': [{
            'geo_id': location.geo_id,
            'geocoder_uri': location.geocoder_uri,
        }],
        'professional_sphere': profession.professional_sphere.id,
        'profession': profession.id,
        'geography_international': False,
        'wage_system': choices.WAGE_SYSTEM_TYPES.fixed,
        'work_mode': [f.WorkModeFactory().id],
    }


def _get_geocoder_data(*args):
    return [
        {
            'geo_id': 100500,
            'country_code': 'RU',
            'uri': 'ymapsbm1://geo?ll=37.618,55.756&spn=0.642,0.466&text=Россия, Москва',
            'name_ru': 'Россия, Москва',
            'name_en': 'Russia, Moscow',
        },
        {
            'geo_id': 100501,
            'country_code': 'TR',
            'uri': 'ymapsbm1://geo?ll=28.978,41.011&spn=1.014,0.423&text=Türkiye, İstanbul',
            'name_ru': 'Турция, Стамбул',
            'name_en': 'Turkey, Istanbul',
        },
    ]


def test_vacancy_list(su_client):
    f.create_heavy_vacancy()
    url = reverse('api:vacancies:list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_vacancy_filter_form(su_client):
    url = reverse('api:vacancies:filter-form')
    response = su_client.get(url)
    assert response.status_code == 200


@patch_create_issue
def test_new_vacancy_create(su_client):
    url = reverse('api:vacancies:list')
    data = _get_base_vacancy_create_data()
    data['type'] = choices.VACANCY_TYPES.new
    data['reason'] = 'nothing'

    response = su_client.post(url, data)

    assert response.status_code == 201
    created_vacancy = Vacancy.unsafe.all().first()
    assert not created_vacancy.geography_international


@patch_create_issue
def test_new_vacancy_create_with_international_geography(su_client):
    url = reverse('api:vacancies:list')
    data = _get_base_vacancy_create_data()
    geography = f.GeographyFactory(
        is_deleted=False,
        ancestors=[0],
        oebs_code='a code',
        kind=GEOGRAPHY_KINDS.international,
    )
    data['type'] = choices.VACANCY_TYPES.new
    data['reason'] = 'nothing'
    data['geography_international'] = True
    data['geography'] = geography.id

    response = su_client.post(url, data)

    assert response.status_code == 201
    created_vacancy = Vacancy.unsafe.all().first()
    assert created_vacancy.geography_international
    assert created_vacancy.geography == geography


@patch_create_issue
@patch(
    target=(
        'intranet.femida.src.api.core.forms.'
        'GeosearchAPI.get_multilang_geo_objects'
    ),
    new=_get_geocoder_data,
)
@override_settings(CITY_HOMEWORKER_ID=100500)
def test_new_vacancy_create_with_locations(su_client):
    url = reverse('api:vacancies:list')

    locations = [
        f.LocationFactory(geo_id=item['geo_id'], geocoder_uri=item['uri'])
        for item in _get_geocoder_data()
    ]

    # Проверка сценария, когда фронт не знает geocoder_uri, но знает geo_id.
    # Например, как на форме редактирования вакансии
    locations.append(f.LocationFactory(geocoder_uri=''))

    location_ids = [i.id for i in locations]
    locations_data = [{'geo_id': i.geo_id, 'geocoder_uri': i.geocoder_uri} for i in locations]

    data = _get_base_vacancy_create_data()
    data['type'] = choices.VACANCY_TYPES.new
    data['reason'] = 'nothing'
    data['locations'] = locations_data

    response = su_client.post(url, data)

    assert response.status_code == 201
    response_data = response.json()
    assert set(location_ids) == {item['id'] for item in response_data['locations']}


@patch_create_issue
def test_new_vacancy_create_with_not_unique_locations(su_client):
    url = reverse('api:vacancies:list')
    location = f.LocationFactory()

    data = _get_base_vacancy_create_data()
    data['type'] = choices.VACANCY_TYPES.new
    data['reason'] = 'nothing'
    data['locations'] = [
        {'geo_id': location.geo_id, 'geocoder_uri': location.geocoder_uri} for _ in range(2)
    ]

    response = su_client.post(url, data)
    assert response.status_code == 400


@pytest.mark.parametrize(
    'location_count, status_code',
    (
        (1, 201),
        (2, 201),
        (10, 201),
        (11, 400),
        (15, 400),
        (30, 400),
    ),
)
@patch_create_issue
def test_new_vacancy_create_with_many_locations(su_client, location_count, status_code):
    url = reverse('api:vacancies:list')
    locations = f.LocationFactory.create_batch(location_count)

    data = _get_base_vacancy_create_data()
    data['type'] = choices.VACANCY_TYPES.new
    data['reason'] = 'nothing'
    data['locations'] = [
        {'geo_id': loc.geo_id, 'geocoder_uri': loc.geocoder_uri} for loc in locations
    ]

    response = su_client.post(url, data)
    assert response.status_code == status_code


@patch_create_issue
def test_internship_vacancy_create(su_client):
    url = reverse('api:vacancies:list')
    data = _get_base_vacancy_create_data()
    data['type'] = choices.VACANCY_TYPES.internship
    data['reason'] = 'nothing'
    response = su_client.post(url, data)
    assert response.status_code == 201


@patch_create_issue
def test_replacement_vacancy_create(su_client):
    url = reverse('api:vacancies:list')

    data = _get_base_vacancy_create_data()
    data.update({
        'type': choices.VACANCY_TYPES.replacement,
        'instead_of': f.UserFactory.create().username,
        'replacement_reason': choices.VACANCY_REPLACEMENT_REASONS.rotation,
        'replacement_department': f.DepartmentFactory.create().id,
        'quit_date': '2020-01-01',
    })

    response = su_client.post(url, data)
    assert response.status_code == 201, response.content
    created_vacancy = Vacancy.unsafe.all().first()
    assert not created_vacancy.geography_international


@patch_create_issue
def test_replacement_vacancy_create_with_international_geography(su_client):
    url = reverse('api:vacancies:list')
    geography = f.GeographyFactory(
        is_deleted=False,
        ancestors=[0],
        oebs_code='a code',
        kind=GEOGRAPHY_KINDS.international,
    )
    data = _get_base_vacancy_create_data()
    data.update({
        'type': choices.VACANCY_TYPES.replacement,
        'instead_of': f.UserFactory.create().username,
        'replacement_reason': choices.VACANCY_REPLACEMENT_REASONS.rotation,
        'replacement_department': f.DepartmentFactory.create().id,
        'quit_date': '2020-01-01',
        'geography_international': True,
        'geography': geography.id,
    })
    response = su_client.post(url, data)
    assert response.status_code == 201, response.content
    created_vacancy = Vacancy.unsafe.all().first()
    assert created_vacancy.geography_international
    assert created_vacancy.geography == geography


@pytest.mark.parametrize('vacancy_type', [
    choices.VACANCY_TYPES.new,
    choices.VACANCY_TYPES.replacement,
    choices.VACANCY_TYPES.internship,
])
def test_vacancy_create_form(su_client, vacancy_type):
    url = reverse('api:vacancies:create-form')
    response = su_client.get(url, {'type': vacancy_type})
    assert response.status_code == 200


@pytest.mark.parametrize('budget_field_key, budget_field_value', (
    ('value_stream', VS_ID),
))
def test_vacancy_create_form_getter(client, budget_field_key, budget_field_value):
    """
    Проверяет, что можно предзаполнять ID БП и value_stream на форме создания вакансии
    """
    user = f.create_user()
    f.ValueStreamFactory(id=VS_ID, staff_id=1, oebs_product_id=1)

    data = {
        'type': 'new',
        budget_field_key: budget_field_value,
    }
    url = reverse('api:vacancies:create-form')
    client.login(login=user.username)
    response = client.get(url, data)
    assert response.status_code == 200

    response_data = response.json()
    assert response_data['data'][budget_field_key]['value'] == budget_field_value


def test_vacancy_detail(su_client):
    vacancy = f.create_heavy_vacancy()
    url = reverse('api:vacancies:detail', kwargs={'pk': vacancy.id})
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('forbidden_actions, status_code', (
    ('[]', 200),
    ('["vacancy_update"]', 403),
))
def test_vacancy_update(su_client, forbidden_actions, status_code):
    vacancy = f.create_heavy_vacancy(type=choices.VACANCY_TYPES.autohire)
    url = reverse('api:vacancies:detail', kwargs={'pk': vacancy.id})
    profession = f.ProfessionFactory.create()

    data = {
        'name': 'Updated vacancy',
        'hiring_manager': f.UserFactory.create().username,
        'department': f.DepartmentFactory.create().id,
        'main_recruiter': f.create_recruiter().username,
        'pro_level_max': choices.VACANCY_PRO_LEVELS.expert,
        'publication_title': 'Title',
        'publication_content': 'Content __with__ %%WF%%',
        'is_published': True,
        'value_stream': f.ValueStreamFactory(oebs_product_id=1).id,
        'profession': profession.id,
        'professional_sphere': profession.professional_sphere.id,
        'work_mode': [f.WorkModeFactory().id],
    }
    with override_config(AUTOHIRE_FORBIDDEN_ACTIONS=forbidden_actions):
        response = su_client.put(url, data)

    assert response.status_code == status_code
    if status_code == 403:
        assert response.json()['error'][0]['code'] == 'forbidden_for_autohire'


@pytest.mark.parametrize('role,updated_fields', (
    (choices.VACANCY_ROLES.head, {'hiring_manager', 'responsibles'}),
    (choices.VACANCY_ROLES.hiring_manager, {'responsibles'}),
    (choices.VACANCY_ROLES.responsible, set()),
))
def test_vacancy_update_by_role(client, role, updated_fields):
    f.create_waffle_switch('enable_update_vacancy_abc_services')
    vacancy = f.VacancyFactory(
        status=choices.VACANCY_STATUSES.in_progress,
        name='Old name',
        pro_level_max=choices.VACANCY_PRO_LEVELS.middle,
    )
    department = vacancy.department
    head, hm, responsible = f.UserFactory.create_batch(3)
    vacancy.add_head(head)
    vacancy.add_hiring_manager(hm)
    vacancy.add_responsible(responsible)
    value_stream_old, value_stream_new = f.ValueStreamFactory.create_batch(2, oebs_product_id=1)
    vacancy.value_stream = value_stream_old
    vacancy.save()

    new_hm, new_responsible = f.UserFactory.create_batch(2)

    url = reverse('api:vacancies:detail', kwargs={'pk': vacancy.id})
    data = {
        'name': 'New name',
        'department': [f.DepartmentFactory().id],
        'head': [f.UserFactory().username],
        'hiring_manager': new_hm.username,
        'responsibles': [new_responsible.username],
        'value_stream': value_stream_new.id,
        'pro_level_max': choices.VACANCY_PRO_LEVELS.expert,
        'work_mode': [f.WorkModeFactory().id],
    }
    client.login(vacancy.members_by_role[role][0].username)
    response = client.put(url, data)
    assert response.status_code == 200, response.content

    vacancy = Vacancy.unsafe.get(id=vacancy.id)

    # Эти поля всегда остаются неизменными.
    # Подразделение менять может только рекрутер, а
    # руководитель берётся автоматически из подр-ия
    assert vacancy.department == department
    assert vacancy.head == head

    # Эти поля могут менять все
    assert vacancy.name == data['name']
    assert vacancy.pro_level_max == data['pro_level_max']
    assert vacancy.value_stream.id == value_stream_new.id

    # Эти поля должны меняться только если у пол-ля есть на это право
    expected_hm = new_hm if 'hiring_manager' in updated_fields else hm
    expected_responsible = new_responsible if 'responsibles' in updated_fields else responsible
    assert vacancy.hiring_manager == expected_hm
    assert vacancy.responsibles == [expected_responsible]


@pytest.mark.parametrize('type, status, is_readonly', (
    (choices.VACANCY_TYPES.new, choices.VACANCY_STATUSES.in_progress, False),
    (choices.VACANCY_TYPES.new, choices.VACANCY_STATUSES.on_approval, True),
    (choices.VACANCY_TYPES.pool, choices.VACANCY_STATUSES.in_progress, False),
))
def test_vacancy_update_value_stream(su_client, type, status, is_readonly):
    vacancy = f.create_heavy_vacancy(type=type, status=status)
    old_value_stream, new_value_stream = f.ValueStreamFactory.create_batch(2, oebs_product_id=1)
    vacancy.value_stream = old_value_stream
    vacancy.save()

    url = reverse('api:vacancies:detail', kwargs={'pk': vacancy.id})
    data = {
        'name': 'Updated vacancy',
        'hiring_manager': f.UserFactory.create().username,
        'department': f.DepartmentFactory.create().id,
        'main_recruiter': f.create_recruiter().username,
        'pro_level_max': choices.VACANCY_PRO_LEVELS.expert,
        'value_stream': new_value_stream.id,
        'professional_sphere': vacancy.professional_sphere.id,
        'profession': vacancy.profession.id,
        'work_mode': [wm.id for wm in vacancy.work_mode.all()],
    }

    response = su_client.put(url, data)
    vacancy = Vacancy.unsafe.get(id=vacancy.id)

    expected = old_value_stream if is_readonly else new_value_stream
    assert response.status_code == 200
    assert vacancy.value_stream == expected


actions_workflow_data = [
    (choices.VACANCY_STATUSES.on_approval, 'approve', lambda: {
        'main_recruiter': f.create_recruiter().username,
        'recruiters': [f.create_recruiter().username, f.create_recruiter().username],
        'budget_position_id': 1,
    }),
    (choices.VACANCY_STATUSES.in_progress, 'suspend', {'comment': 'comemnt'}),
    (choices.VACANCY_STATUSES.suspended, 'resume', {'comment': 'comment'}),
    (choices.VACANCY_STATUSES.on_approval, 'close', {'comment': 'comment'}),
]


@pytest.mark.parametrize('workflow_data', actions_workflow_data)
def test_vacancy_action(su_client, workflow_data):
    from_status, action_name, data = workflow_data
    vacancy = f.VacancyFactory(status=from_status)
    view_name = 'api:vacancies:{}'.format(action_name.replace('_', '-'))
    url = reverse(view_name, kwargs={'pk': vacancy.id})
    data = data() if callable(data) else data
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_vacancy_add_to_group(su_client):
    vacancy = f.VacancyFactory(status=choices.VACANCY_STATUSES.in_progress)
    superuser = f.get_superuser()
    vacancy_group = f.create_vacancy_group(recruiters=[superuser])
    url = reverse('api:vacancies:add-to-group', kwargs={'pk': vacancy.id})
    data = {
        'vacancy_group': vacancy_group.id,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


@patch(
    target=(
        'intranet.femida.src.api.vacancies.serializers.'
        'VacancyApproveFormSerializer.get_budget_position_id'
    ),
    new=lambda *x, **y: None,
)
@pytest.mark.parametrize('from_status, view_name', (
    (choices.VACANCY_STATUSES.on_approval, 'approve'),
    (choices.VACANCY_STATUSES.on_approval, 'update'),
    (choices.VACANCY_STATUSES.in_progress, 'add-to-group'),
    (choices.VACANCY_STATUSES.in_progress, 'suspend'),
    (choices.VACANCY_STATUSES.suspended, 'resume'),
    (choices.VACANCY_STATUSES.on_approval, 'close'),
))
def test_vacancy_view_forms(su_client, from_status, view_name):
    vacancy = f.VacancyFactory(status=from_status)
    url = reverse(
        viewname='api:vacancies:{}-form'.format(view_name),
        kwargs={'pk': vacancy.id},
    )
    response = su_client.get(url)
    assert response.status_code == 200


def test_vacancy_update_form_heavy(su_client):
    vacancy = f.create_heavy_vacancy()
    url = reverse('api:vacancies:update-form', kwargs={'pk': vacancy.id})
    response = su_client.get(url)
    assert response.status_code == 200
