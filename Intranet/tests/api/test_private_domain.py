from datetime import datetime
from typing import List

import pytest
import collections

from intranet.domenator.src.db import Domain
from intranet.domenator.src.db.event.models import Event

pytestmark = pytest.mark.asyncio


@pytest.fixture()
async def domains(db_bind):
    async with db_bind as engine:
        return [
            await Domain.create(
                name='test1-master.ru',
                admin_id='1',
                org_id='1',
                owned=True,
                master=True,
                display=True,
                bind=engine,
            ),
            await Domain.create(
                name='test1-alias.ru',
                admin_id='1',
                org_id='1',
                bind=engine,
                owned=True,
                master=False,
            ),
            await Domain.create(
                name='test1-unowned.ru',
                admin_id='1',
                org_id='1',
                bind=engine,
                owned=False,
                master=False,
            ),
            await Domain.create(
                name='test2.ru',
                admin_id='2',
                org_id='2',
                bind=engine,
            ),
            await Domain.create(
                name='test3.ru',
                admin_id='3',
                org_id='3',
                bind=engine,
            ),
            await Domain.create(
                name='test2-master.ru',
                admin_id='2',
                org_id='2',
                bind=engine,
                master=True,
                owned=True
            ),
            await Domain.create(
                name='test4.ru',
                admin_id='4',
                org_id='4',
                bind=engine,
                master=False,
                owned=True,
                blocked_at=datetime.utcnow(),
            )
        ]


async def test_get_domains(client, domains: List[Domain]):
    response = await client.get('api/private/domains/?org_id=1&master=1&owned=1')
    assert response.status_code == 200, response.text

    response_data = response.json()
    assert len(response_data) == 1

    assert response_data[0].items() >= {
        'org_id': 1,
        'name': 'test1-master.ru',
        'owned': True,
        'master': True,
        'display': True,
        'blocked_at': None,
        'delegated': False,
        'mx': False,
        'validated': False,
        'validated_at': None,
        'via_webmaster': False,
    }.items()

    response = await client.get('api/private/domains/?org_id=1&master=0&owned=1')
    assert response.status_code == 200, response.text
    response_data = response.json()
    assert len(response_data) == 1
    assert response_data[0]['name'] == 'test1-alias.ru'

    response = await client.get('api/private/domains/?org_id=1,2&owned=1')
    assert response.status_code == 200, response.text

    response_data = response.json()
    assert len(response_data) == 3
    assert response_data[0]['name'] == 'test1-master.ru'
    assert response_data[1]['name'] == 'test1-alias.ru'
    assert response_data[2]['name'] == 'test2-master.ru'


async def test_cannot_update_all_domains_to_master(client, domains: List[Domain]):
    patch_response = await client.patch('api/private/domains/?org_id=1', json={
        'master': True,
    })
    assert patch_response.status_code == 400
    expected = {
        'detail': 'You should provide domain name when making it master for the organization'
    }
    assert patch_response.json() == expected


async def test_cannot_set_domain_to_be_not_master(client, domains: List[Domain]):
    patch_response = await client.patch('api/private/domains/?org_id=1&name=test1-master.ru', json={
        'master': False,
    })
    assert patch_response.status_code == 400
    expected = {
        'detail': 'You can make domain "test1-master.ru" master, but you cannot unmake it master'
    }
    assert patch_response.json() == expected


async def test_cannot_set_domain_to_be_master_if_not_owned(client, domains: List[Domain]):
    patch_response = await client.patch('api/private/domains/?org_id=2&name=test2.ru', json={
        'master': True,
    })
    assert patch_response.status_code == 422
    expected = {
        'detail': 'Cannot make master not owned domain "test2.ru"'
    }
    assert patch_response.json() == expected


async def test_cannot_set_domain_to_be_master_if_blocked(client, domains: List[Domain]):
    patch_response = await client.patch('api/private/domains/?org_id=4&name=test4.ru', json={
        'master': True,
    })
    assert patch_response.status_code == 422
    expected = {
        'detail': 'Cannot make master blocked domain "test4.ru"'
    }
    assert patch_response.json() == expected


async def test_set_owned_for_all_domains(client, domains: List[Domain]):
    response = await client.get('api/private/domains/?org_id=1')
    owned_states = [item['owned'] for item in response.json()]
    assert len(owned_states) == 3
    assert len([state for state in owned_states if state]) == 2

    # проставим owned в true и убедимся, что все 3 домена стали owned
    patch_response = await client.patch('api/private/domains/?org_id=1', json={
        'owned': True,
    })
    assert patch_response.status_code == 200
    response = await client.get('api/private/domains/?org_id=1')
    owned_states = [item['owned'] for item in response.json()]
    assert len(owned_states) == 3
    assert len([state for state in owned_states if state]) == 3


async def test_set_master(client, domains: List[Domain], test_vcr):
    with test_vcr.use_cassette('test_set_master.yaml'):
        patch_response = await client.patch('api/private/domains/?org_id=1&name=test1-alias.ru', json={
            'master': True,
            'display': True,
        })
        assert patch_response.status_code == 200, patch_response.text

    response = await client.get('api/private/domains/?org_id=1&name=test1-master.ru')
    assert response.status_code == 200, response.text
    response_data = response.json()
    assert len(response_data) == 1
    expected = {
        'org_id': 1,
        'name': 'test1-master.ru',
        'owned': True,
        'master': False,
        'display': True,
    }
    got_response = {k: v for k, v in response.json()[0].items() if k in expected}
    assert got_response == expected

    response = await client.get('api/private/domains/?org_id=1&name=test1-alias.ru')
    expected = {
        'org_id': 1,
        'name': 'test1-alias.ru',
        'owned': True,
        'master': True,
        'display': True,
    }
    got_response = {k: v for k, v in response.json()[0].items() if k in expected}
    assert got_response == expected

    events = await Event.query.gino.all()
    assert len(events) == 3
    counter = collections.Counter([event.name for event in events])
    assert counter == {'domain_changed': 2, 'domain_master_changed': 1}

    set_master_event: Event = [
        event for event in events if event.data['domain'] == 'test1-alias.ru' and event.name == 'domain_changed'
    ][0]
    assert set_master_event.data['old_data']['master'] is False
    assert set_master_event.data['new_data']['master'] is True
    unset_master_event: Event = [
        event for event in events if event.data['domain'] == 'test1-master.ru' and event.name == 'domain_changed'
    ][0]
    assert unset_master_event.data['domain'] == 'test1-master.ru'
    assert unset_master_event.data['old_data']['master'] is True
    assert unset_master_event.data['new_data']['master'] is False

    change_master_event: Event = [event for event in events if event.name == 'domain_master_changed'][0]
    expected = {
        'domain': 'test1-alias.ru',
        'org_id': '1',
        'admin_uid': '1',
        'old_master_domain': 'test1-master.ru',
        'old_master_admin_uid': '1',
    }
    assert change_master_event.data == expected
