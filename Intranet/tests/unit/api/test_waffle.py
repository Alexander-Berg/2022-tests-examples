import pytest
import json

from django.core.urlresolvers import reverse

from common import factories

NOTIFIER_NAME = 'notice_'


def get_note(type_notice, permissions=('can_view',), subtype='common'):
    text = 'Тестовые котики хотят нажать на кнопощку'
    cta = 'Кнопощка'
    link = '/api/v3/notifier/'

    note = json.dumps(
        {
            'message': text,
            'cta': cta,
            'link': link,
            'type': type_notice,
            'expiration_time': 2,
            'permissions': permissions,
            'subtype': subtype,
        }
    )
    return note


def test_waffle_notifier(only_view_client):
    flag = factories.SwitchFactory(
        name='notice_1',
        note=get_note('info'),
        active=True,
    )

    response = only_view_client.json.get(reverse('api-v3:notifier-list'))

    assert len(response.json()['results']) == 1
    assert response.json()['results'][0]['name'] == flag.name


def test_waffle_notifier_two_message_with_no_active(only_view_client):
    factories.SwitchFactory(name='notice_1', note=get_note('error'), active=False)
    flag_2 = factories.SwitchFactory(
        name='notice_2',
        note=get_note('error'),
        active=True,
    )

    response = only_view_client.json.get(reverse('api-v3:notifier-list'))

    assert len(response.json()['results']) == 1
    assert response.json()['results'][0]['name'] == flag_2.name


def test_waffle_notifier_three_active_message_error(only_view_client):
    factories.SwitchFactory(name='notice_2', note=get_note('error'), active=True)
    flag_2 = factories.SwitchFactory(
        name='notice_1',
        note=get_note('error'),
        active=True,
    )
    factories.SwitchFactory(name='notice_3', note=get_note('error'), active=True)

    response = only_view_client.json.get(reverse('api-v3:notifier-list'),)

    assert len(response.json()['results']) == 1
    assert response.json()['results'][0]['name'] == flag_2.name


def test_waffle_notifier_three_active_message_different_type(only_view_client):
    factories.SwitchFactory(name='notice_1', note=get_note('info'), active=True)
    flag_2 = factories.SwitchFactory(
        name='notice_2',
        note=get_note('error'),
        active=True,
    )
    factories.SwitchFactory(name='notice_3', note=get_note('warning'), active=True)

    response = only_view_client.json.get(reverse('api-v3:notifier-list'))

    assert len(response.json()['results']) == 1
    assert response.json()['results'][0]['name'] == flag_2.name


def test_waffle_notifier_no_active_message(only_view_client):
    factories.SwitchFactory(name='notice_1', note=get_note('info'), active=False)
    factories.SwitchFactory(name='notice_2', note=get_note('error'), active=False)

    response = only_view_client.json.get(reverse('api-v3:notifier-list'))

    assert len(response.json()['results']) == 0


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer', 'services_viewer', 'full_access')
)
def test_waffle_notifier_permissions(client, staff_factory, staff_role):
    client.login(staff_factory(staff_role).login)

    flag_1 = factories.SwitchFactory(
        name='notice_1',
        note=get_note('info', permissions=('can_view',)),
        active=True,
    )

    flag_2 = factories.SwitchFactory(
        name='notice_2',
        note=get_note('warning', permissions=('view_all_services',)),
        active=True,
    )

    flag_3 = factories.SwitchFactory(
        name='notice_3',
        note=get_note('error', permissions=('can_edit',)),
        active=True,
    )

    response = client.json.get(reverse('api-v3:notifier-list'))

    results = {
        'own_only_viewer': flag_1.name,
        'services_viewer': flag_2.name,
        'full_access': flag_3.name,
    }

    assert len(response.json()['results']) == 1
    assert response.json()['results'][0]['name'] == results[staff_role]


def test_waffle_invalid_notice(only_view_client):
    factories.SwitchFactory(
        name='notice_1',
        note=get_note('info', permissions=('can_view',))[1:],
        active=True,
    )
    response = only_view_client.json.get(reverse('api-v3:notifier-list'))

    assert len(response.json()['results']) == 0


def test_waffle_notifier_subtype(only_view_client):
    factories.SwitchFactory(
        name='notice_1',
        note=get_note('info'),
        active=True,
    )
    flag_2 = factories.SwitchFactory(
        name='notice_2',
        note=get_note('error'),
        active=True,
    )
    factories.SwitchFactory(
        name='notice_3',
        note=get_note('warning', subtype='hardware'),
        active=True,
    )
    flag_4 = factories.SwitchFactory(
        name='notice_4',
        note=get_note('error', subtype='hardware'),
        active=True,
    )
    flag_5 = factories.SwitchFactory(
        name='notice_5',
        note=get_note('info', subtype='smth'),
        active=True,
    )

    response = only_view_client.json.get(
        reverse('api-v3:notifier-list'),
        {'subtype': 'common,smth,hardware'},
    )

    assert len(response.json()['results']) == 3
    assert (
        {item['name'] for item in response.json()['results']} ==
        {flag_2.name, flag_4.name, flag_5.name}
    )
