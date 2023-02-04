import pytest

from unittest.mock import patch, MagicMock, Mock

from django.urls import reverse

from ok.api.scenarios.serializers import ScenarioUpdateFormSerializer
from ok.scenarios.choices import SCENARIO_STATUSES
from ok.scenarios.models import Scenario

from tests import factories as f


@patch('ok.tracker.macros.macros.create', return_value=Mock(id=123))
def test_scenario_create(macro_create_mock, client):
    approver = f.UserFactory()
    data = {
        'slug': 'vacation',
        'name': 'Vacation approvement',
        'responsible_groups': [f.GroupFactory().url, f.GroupFactory().url],
        'tracker_queue': 'OK',
        'approvement_data': {
            'stages': [{'approver': approver.username}],
        },
    }
    url = reverse('api:scenarios:list-create')
    client.force_authenticate('tigran')
    response = client.post(url, data=data)

    assert response.status_code == 201, response.content

    scenario = Scenario.objects.get(slug=data['slug'])
    group_urls = scenario.responsible_groups.values_list('url', flat=True)
    macro, = scenario.tracker_macros.all()

    expected_macro_body = (
        '{{=<% %>=}}{{iframe src="'
        'https://ok.test.yandex-team.ru/tracker'
        '?_embedded=1'
        '&author=<%currentUser.login%>'
        '&object_id=<%issue.key%>'
        '&uid=<%currentDateTime.iso8601%>'
        '&scenario=vacation'
        '" frameborder=0 width=100% height=400px}}'
    )

    assert scenario.author == 'tigran'
    assert scenario.name == data['name']
    assert scenario.approvement_data == data['approvement_data']
    assert sorted(group_urls) == sorted(data['responsible_groups'])
    assert macro.name == scenario.name
    assert macro.body == expected_macro_body
    assert macro.tracker_id == 123
    assert macro.tracker_queue.name == data['tracker_queue']
    macro_create_mock.assert_called_once_with(
        {'queue': data['tracker_queue']},
        name=macro.name,
        body=expected_macro_body,
    )


@pytest.mark.parametrize('data, status_code', (
    ({}, 200),
    ({'tracker_queue': 'VALID'}, 200),
    ({'tracker_queue': 'IN-VALID'}, 400),
    ({'slug': 'valid'}, 200),
    ({'slug': 'in.valid'}, 400),
))
def test_scenario_create_validate(client, data, status_code):
    url = reverse('api:scenarios:create-validate')
    response = client.post(url, data=data)
    assert response.status_code == status_code, response.content


def test_scenario_create_form(client):
    url = reverse('api:scenarios:create-form')
    response = client.get(url)
    assert response.status_code == 200, response.content


@pytest.mark.parametrize('user, status, status_code', (
    ('author', SCENARIO_STATUSES.active, 200),
    ('author', SCENARIO_STATUSES.archived, 403),
    ('responsible', SCENARIO_STATUSES.active, 200),
    ('viewer', SCENARIO_STATUSES.active, 403),
))
def test_scenario_update_permissions(client, user, status, status_code):
    membership = f.GroupMembershipFactory(login='responsible')
    scenario = f.ScenarioFactory(author='author', status=status)
    scenario.scenario_groups.create(group=membership.group)

    url = reverse('api:scenarios:detail', kwargs={'slug': scenario.slug})
    scenario_data = ScenarioUpdateFormSerializer(scenario).data
    client.force_authenticate(user)
    response = client.put(url, data=scenario_data)

    assert response.status_code == status_code, response.content


@patch('ok.scenarios.controllers.update_macro', return_value=Mock(id=123))
def test_scenario_update(macro_update_mock, client):
    approver = f.UserFactory()
    update_data = {
        'name': 'Vacation approvement',
        'responsible_groups': ['svc_ok', 'epic'],
        'approvement_data': {
            'stages': [{'approver': approver.username}],
        },
    }
    scenario = f.ScenarioFactory()
    author = f.UserFactory(username=scenario.author)
    scenario.scenario_groups.create(group=f.GroupFactory(url='epic'))
    scenario.scenario_groups.create(group=f.GroupFactory(url='cvc_ok'))
    f.GroupFactory(url='svc_ok')
    macro = f.ScenarioTrackerMacroFactory(scenario=scenario, tracker_id=123)

    url = reverse('api:scenarios:detail', kwargs={'slug': scenario.slug})
    data = dict(ScenarioUpdateFormSerializer(scenario).data, **update_data)
    client.force_authenticate(author)
    response = client.put(url, data=data)

    assert response.status_code == 200, response.content

    scenario.refresh_from_db()
    macro.refresh_from_db()
    group_urls = scenario.responsible_groups.values_list('url', flat=True)
    history_responsibles, history_change, _ = scenario.history.all()

    assert scenario.name == update_data['name']
    assert scenario.approvement_data == update_data['approvement_data']
    assert sorted(group_urls) == sorted(update_data['responsible_groups'])
    assert macro.name == update_data['name']
    macro_update_mock.assert_called_once_with(
        key=macro.tracker_id,
        queue=macro.tracker_queue.name,
        name=macro.name,
    )
    assert history_responsibles.history_type == '~'
    assert history_responsibles.history_change_reason == 'responsible_groups_changed'
    assert history_change.history_type == '~'
    assert history_change.history_user == author
    assert history_change.name == update_data['name']


def test_scenario_update_form(client):
    scenario = f.ScenarioFactory()
    url = reverse('api:scenarios:update-form', kwargs={'slug': scenario.slug})
    response = client.get(url)
    assert response.status_code == 200, response.content


def test_scenario_update_form_enriched_stages(client):
    approver = f.UserFactory(
        username='bukovsky',
        first_name_en='Vladimir',
        last_name_en='Bukovsky',
    )
    scenario = f.ScenarioFactory(approvement_data={'stages': [{'approver': approver.username}]})

    url = reverse('api:scenarios:update-form', kwargs={'slug': scenario.slug})
    response = client.get(url)

    assert response.status_code == 200, response.content
    response_data = response.json()
    stages = response_data['data']['approvement_data']['value']['stages']['value']
    assert stages[0]['value']['approver']['value'] == dict(
        username='bukovsky',
        first_name='Vladimir',
        last_name='Bukovsky',
        affiliation='yandex',
        login='bukovsky',
        fullname='Vladimir Bukovsky',
    )


@pytest.mark.parametrize('user, status, actions', (
    ('author', SCENARIO_STATUSES.active, {'update': True, 'archive': True, 'restore': False}),
    ('author', SCENARIO_STATUSES.archived, {'update': False, 'archive': False, 'restore': True}),
    ('responsible', SCENARIO_STATUSES.active, {'update': True, 'archive': True, 'restore': False}),
    ('responsible', SCENARIO_STATUSES.archived, {'update': False, 'archive': False, 'restore': True}),
    ('viewer', SCENARIO_STATUSES.active, {'update': False, 'archive': False, 'restore': False}),
    ('viewer', SCENARIO_STATUSES.archived, {'update': False, 'archive': False, 'restore': False}),
))
def test_scenario_detail(client, user, status, actions):
    membership = f.GroupMembershipFactory(login='responsible')
    scenario = f.ScenarioFactory(author='author', status=status)
    scenario.scenario_groups.create(group=membership.group)
    macro = f.ScenarioTrackerMacroFactory(scenario=scenario)

    url = reverse('api:scenarios:detail', kwargs={'slug': scenario.slug})
    client.force_authenticate(user)
    response = client.get(url)
    assert response.status_code == 200, response.content

    response_data = response.json()
    actions['clone'] = True
    scenario_data = {
        'slug': scenario.slug,
        'name': scenario.name,
        'status': status,
        'tracker_queue': macro.tracker_queue.name,
        'actions': actions,
    }
    assert response_data == scenario_data


def test_scenario_list(client, django_assert_num_queries):
    login = 'user'
    membership1 = f.GroupMembershipFactory(login=login)
    membership2 = f.GroupMembershipFactory(login=login)
    visible_scenarios = [
        f.ScenarioFactory(author=login),
        f.ScenarioResponsibleGroupFactory(group=membership1.group).scenario,
        f.ScenarioResponsibleGroupFactory(group=membership2.group).scenario,
        f.ScenarioResponsibleGroupFactory(group=membership2.group).scenario,
    ]
    f.ScenarioTrackerMacroFactory(scenario=visible_scenarios[0])
    f.ScenarioTrackerMacroFactory(scenario=visible_scenarios[1])
    f.ScenarioResponsibleGroupFactory()
    f.ScenarioFactory()
    f.ScenarioFactory(author=login, status=SCENARIO_STATUSES.archived)
    expected_scenario_slugs = [scenario.slug for scenario in reversed(visible_scenarios)]

    url = reverse('api:scenarios:list-create')
    client.force_authenticate(login)
    # 2 - savepoint
    # 4 - prefetch responsible_groups, memberships и tracker_macros, tracker_queue
    # 2 - count и select списка
    with django_assert_num_queries(8):
        response = client.get(url)
    assert response.status_code == 200, response.content

    response_data = response.json()
    assert response_data['count'] == len(expected_scenario_slugs)
    assert [scenario['slug'] for scenario in response_data['results']] == expected_scenario_slugs


@pytest.mark.parametrize('statuses, returned', (
    ([SCENARIO_STATUSES.active], 2),
    ([SCENARIO_STATUSES.archived], 1),
    ([SCENARIO_STATUSES.active, SCENARIO_STATUSES.archived], 3),
    ([], 2),
    (None, 2),
))
def test_scenario_list_filter_by_statuses(statuses, returned, client):
    login = 'user'
    f.ScenarioFactory(author=login)
    f.ScenarioFactory(author=login)
    f.ScenarioFactory(author=login, status=SCENARIO_STATUSES.archived)

    params = {}
    if statuses is not None:
        params['statuses'] = statuses
    url = reverse('api:scenarios:list-create')
    client.force_authenticate(login)
    response = client.get(url, params)
    assert response.status_code == 200, response.content

    response_data = response.json()
    assert response_data['count'] == returned
    expected_statuses = statuses if statuses else ['active']
    for scenario in response_data['results']:
        assert scenario['status'] in expected_statuses


def test_scenario_filter_form(client):
    url = reverse('api:scenarios:filter-form')
    response = client.get(url)
    assert response.status_code == 200, response.content


def test_scenario_clone_form(client):
    scenario = f.ScenarioFactory(slug='slug')
    f.ScenarioFactory(slug='slug-copy')
    f.ScenarioFactory(slug='slug-copy-2')
    f.ScenarioFactory(slug='slug-copy-4')
    f.create_waffle_switch('enable_scenario_clone_slug_generation')

    url = reverse('api:scenarios:clone-form', kwargs={'slug': 'slug'})
    response = client.get(url)

    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['data']['slug']['value'] == 'slug-copy-3'
    assert response_data['data']['name']['value'] == scenario.name + ' - копия 3'


@patch('ok.scenarios.controllers.delete_macro')
def test_scenario_archive(macro_delete_mock, client):
    scenario = f.ScenarioFactory()
    author = f.UserFactory(username=scenario.author)
    macro = f.ScenarioTrackerMacroFactory(scenario=scenario)

    url = reverse('api:scenarios:archive', kwargs={'slug': scenario.slug})
    client.force_authenticate(author)
    response = client.post(url)

    assert response.status_code == 200, response.content
    scenario.refresh_from_db()
    macro.refresh_from_db()
    history_change, _ = scenario.history.all()
    assert scenario.status == SCENARIO_STATUSES.archived
    assert macro.is_active is False
    assert history_change.history_type == '~'
    assert history_change.history_user == author
    assert history_change.status == SCENARIO_STATUSES.archived
    macro_delete_mock.assert_called_once_with(
        key=macro.tracker_id,
        queue=macro.tracker_queue.name,
    )


@patch('ok.tracker.macros.macros.create', return_value=Mock(id=321))
def test_scenario_restore(macro_create_mock, client):
    scenario = f.ScenarioFactory(status=SCENARIO_STATUSES.archived)
    author = f.UserFactory(username=scenario.author)
    macro = f.ScenarioTrackerMacroFactory(
        scenario=scenario,
        tracker_id=123,
        is_active=False,
    )

    url = reverse('api:scenarios:restore', kwargs={'slug': scenario.slug})
    client.force_authenticate(author)
    response = client.post(url)

    assert response.status_code == 200, response.content
    scenario.refresh_from_db()
    macro.refresh_from_db()
    history_change, _ = scenario.history.all()
    assert scenario.status == SCENARIO_STATUSES.active
    assert macro.is_active is True
    assert macro.tracker_id == 321
    assert history_change.history_type == '~'
    assert history_change.history_user == author
    assert history_change.status == SCENARIO_STATUSES.active

    expected_macro_body = (
        '{{=<% %>=}}{{iframe src="'
        'https://ok.test.yandex-team.ru/tracker'
        '?_embedded=1'
        '&author=<%currentUser.login%>'
        '&object_id=<%issue.key%>'
        '&uid=<%currentDateTime.iso8601%>'
        f'&scenario={scenario.slug}'
        '" frameborder=0 width=100% height=400px}}'
    )
    macro_create_mock.assert_called_once_with(
        {'queue': macro.tracker_queue.name},
        name=macro.name,
        body=expected_macro_body,
    )
