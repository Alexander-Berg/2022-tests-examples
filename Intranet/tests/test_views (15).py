import pytest
import json

from intranet.yasanta.backend.gifts.models import Event, SantaEntry
from intranet.yasanta.backend.gifts.controller import SantaEntryCtl


def get_json(result):
    return json.loads(result.content.decode('utf-8'))


@pytest.fixture
def fill_database(db):
    Event.objects.create(
        code='TestEvent',
        name='Event',
        comment='SomeComment',
        admins='test_admin',
        max_count=5,
        returnable=True,
    )
    event = Event.objects.get(code='TestEvent')
    SantaEntry.objects.create(
        event=event,
        lucky_login='test_login',
        himself=False,
        collector_login='test_collector_login'
    )


@pytest.mark.django_db
def test_example(fill_database):
    event = Event.objects.get(code='TestEvent')
    assert event.code == 'TestEvent'
    assert event.name == 'Event'
    assert event.comment == 'SomeComment'
    assert event.admins == 'test_admin'


@pytest.mark.django_db
def test_prepare(fill_database, client):
    login = 'test_login'
    result = client.get(
        '/api/EventThatDoesNotExist/prepare/',
        HTTP_DEBUG_LOGIN=login,
    )
    result_json = get_json(result)
    assert result_json['error'] == 'unknown_event'

    collector_login = 'test_collector_login'
    result = client.get(
        '/api/TestEvent/prepare/',
        HTTP_DEBUG_LOGIN=login,
    )
    result_json = get_json(result)
    assert result_json['lucky_login'] == login
    assert result_json['himself'] is False
    assert result_json['collector_login'] == collector_login


@pytest.mark.django_db
def test_collect(fill_database, client):
    admin_login = 'test_admin'
    login = 'test_login'
    result = client.get(
        '/api/EventThatDoesNotExist/collect/{}/'.format(login),
        HTTP_DEBUG_LOGIN=admin_login,
    )
    result_json = get_json(result)
    assert result_json['error'] == 'unknown_event'

    result = client.post(
        '/api/TestEvent/collect/{}/'.format(login),
        HTTP_DEBUG_LOGIN=admin_login,
    )
    result_json = get_json(result)
    assert result_json['collect_for_logins'] == [login]

    result = client.post(
        '/api/TestEvent/return/{}/'.format(login),
        HTTP_DEBUG_LOGIN=admin_login,
    )
    result_json = get_json(result)
    assert result_json['return_for_login'] == login


@pytest.mark.django_db
def test_report(fill_database, client):
    login = 'test_admin'
    result = client.get(
        '/api/EventThatDoesNotExist/report/',
        HTTP_DEBUG_LOGIN=login,
    )
    result_json = get_json(result)
    assert result_json['error'] == 'unknown_event'

    event = Event.objects.get(code='TestEvent')
    report = SantaEntryCtl(event=event).report()
    result = client.get(
        '/api/TestEvent/report/',
        HTTP_DEBUG_LOGIN=login,
    )
    result_json = get_json(result)
    assert result_json == report


@pytest.mark.django_db
def test_export_excel(fill_database, client):
    login = 'test_admin'
    result = client.get(
        '/api/EventThatDoesNotExist/report/',
        HTTP_DEBUG_LOGIN=login,
    )
    result_json = get_json(result)
    assert result_json['error'] == 'unknown_event'

    event = Event.objects.get(code='TestEvent')
    report = SantaEntryCtl(event=event).report()
    result = client.get(
        '/api/TestEvent/report/',
        HTTP_DEBUG_LOGIN=login,
    )
    print(result)
    result_json = get_json(result)
    assert result_json == report


@pytest.mark.django_db
def test_info(fill_database, client):
    login = 'test_login'
    result = client.get(
        '/api/EventThatDoesNotExist/info/',
        HTTP_DEBUG_LOGIN=login,
    )
    result_json = get_json(result)
    assert result_json['error'] == 'unknown_event'

    event = Event.objects.get(code='TestEvent')
    result = client.get(
        '/api/TestEvent/info/',
        HTTP_DEBUG_LOGIN=login,
    )
    result_json = get_json(result)
    assert result_json['name'] == event.name
    assert result_json['comment'] == event.comment
    assert result_json['admins'] == ['test_admin']
    assert result_json['max_count'] == event.max_count
