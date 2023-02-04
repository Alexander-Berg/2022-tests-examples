import json

import pytest
import requests

from django.conf import settings

from infra.cauth.server.common.alchemy import Session
from infra.cauth.server.common.constants import SERVER_TYPE
from infra.cauth.server.common.models import ServerGroup, ServerResponsible
from infra.cauth.server.master.cache.puncher import update_puncher_rules_cache
from infra.cauth.server.master.constants import FILE_TYPE
from infra.cauth.server.master.files.models import S3File
from __tests__.utils import (
    create_access_rule,
    create_server,
    get_or_create_source,
    get_or_create_server_group,
)

pytestmark = pytest.mark.django_db

ALLOWED_SOURCE_FOR_PUNCHER = next(iter(settings.PUNCHER_SOURCES_WHITE_LIST))
PROHIBITED_SOURCE_FOR_PUNCHER = 'gywqeguqge'
assert PROHIBITED_SOURCE_FOR_PUNCHER not in settings.PUNCHER_SOURCES_WHITE_LIST

PUNCHER_DN = '/C=RU/ST=Russia/L=Moscow/O=Yandex/OU=ITO/CN=Puncher Dev Client/emailAddress=puncher@yandex-team.ru'


def get_puncher_rules():
    cache_file = S3File.objects.get_last(FILE_TYPE.PUNCHER_CACHE)
    output = cache_file.file.read()
    return json.loads(output)


@pytest.mark.parametrize('server_type', SERVER_TYPE.choices())
def test_servers_for_responsibles(server_type, sources, users, settings):
    source = get_or_create_source('golem')
    server = create_server('host1.yandex.net', type=server_type, source_name=source.name)
    resp = ServerResponsible(
        server=server,
        user=users.frodo,
        source=source,
    )
    Session.add(resp)

    update_puncher_rules_cache.delay()
    data = get_puncher_rules()

    expected = {'rules': [
        {
            'src': {
                'object': {
                    'login': 'frodo',
                    'uid': users.frodo.uid,
                },
                'type': 'user'
            },
            'dst': {
                'object': {
                    'id': server.id,
                    'fqdn': server.fqdn
                },
                'type': server_type
            },
            'is_responsible': True
        },
    ]}
    assert data == expected


@pytest.mark.parametrize('server_type', SERVER_TYPE.choices())
def test_servers_from_idm(server_type, users, settings, sources):
    server = create_server('host1.yandex.net', type=server_type, source_name='golem')
    create_access_rule('ssh', users.frodo, server)

    update_puncher_rules_cache.delay()
    data = get_puncher_rules()

    expected = {'rules': [
        {
            'src': {
                'object': {
                    'login': 'frodo',
                    'uid': users.frodo.uid,
                },
                'type': 'user'
            },
            'dst': {
                'object': {
                    'id': server.id,
                    'fqdn': server.fqdn
                },
                'type': server_type
            },
        },
    ]}
    assert data == expected


@pytest.mark.parametrize('server_type', SERVER_TYPE.choices())
@pytest.mark.parametrize('source_name', [ALLOWED_SOURCE_FOR_PUNCHER, PROHIBITED_SOURCE_FOR_PUNCHER])
def test_groups_for_responsibles_are_expanded(server_type, source_name, users, settings, sources):
    server_group = get_or_create_server_group(source_name)
    server_group.responsible_users.append(users.frodo)

    server = create_server('host1.yandex.ru', type=server_type, source_name=source_name)
    server.groups.append(server_group)

    update_puncher_rules_cache.delay()
    received = get_puncher_rules()

    if source_name == ALLOWED_SOURCE_FOR_PUNCHER:
        expected_dst = {
            'object': {
                'source': ALLOWED_SOURCE_FOR_PUNCHER,
                'id': server_group.id,
                'name': 'some_group'
            },
            'type': 'group'
        }
    else:
        expected_dst = {
            'object': {
                'id': server.id,
                'fqdn': 'host1.yandex.ru'
            },
            'type': server_type,
        }
    assert received == {'rules': [
        {
            'src': {
                'object': {
                    'login': 'frodo',
                    'uid': users.frodo.uid,
                },
                'type': 'user'
            },
            'dst': expected_dst,
            'is_responsible': True
        },
    ]}


@pytest.mark.parametrize('server_type', SERVER_TYPE.choices())
@pytest.mark.parametrize('source_name', [ALLOWED_SOURCE_FOR_PUNCHER, PROHIBITED_SOURCE_FOR_PUNCHER])
def test_prohibited_groups_from_idm_are_expanded(server_type, source_name, users, settings, sources):
    server_group = get_or_create_server_group(source_name)
    server = create_server('host1.yandex.ru', type=server_type, source_name=source_name)
    server.groups.append(server_group)

    create_access_rule('ssh', users.frodo, server_group)

    update_puncher_rules_cache.delay()
    received = get_puncher_rules()

    if source_name == ALLOWED_SOURCE_FOR_PUNCHER:
        expected_dst = {
            'object': {
                'source': ALLOWED_SOURCE_FOR_PUNCHER,
                'id': server_group.id,
                'name': 'some_group'
            },
            'type': 'group'
        }
    else:
        expected_dst = {
            'object': {
                'id': server.id,
                'fqdn': 'host1.yandex.ru'
            },
            'type': server_type,
        }
    assert received == {'rules': [
        {
            'src': {
                'object': {
                    'login': 'frodo',
                    'uid': users.frodo.uid,
                },
                'type': 'user'
            },
            'dst': expected_dst,
        },
    ]}


@pytest.mark.parametrize('server_type', SERVER_TYPE.choices())
def test_prohibited_group_responsibles_are_not_duplicated(server_type, users, settings, sources):
    source = get_or_create_source(PROHIBITED_SOURCE_FOR_PUNCHER)
    server = create_server('host1.yandex.ru', type=server_type, source_name=PROHIBITED_SOURCE_FOR_PUNCHER)

    for group_name in ['yp.something', 'yp.YP-SAS.something']:
        server_group = ServerGroup(
            name=group_name,
            source=source,
        )
        Session.add(server_group)
        server_group.responsible_users.append(users.frodo)
        server.groups.append(server_group)

    update_puncher_rules_cache.delay()
    data = get_puncher_rules()

    expected = {'rules': [
        {
            'src': {
                'object': {
                    'login': 'frodo',
                    'uid': users.frodo.uid,
                },
                'type': 'user'
            },
            'dst': {
                'object': {
                    'id': server.id,
                    'fqdn': 'host1.yandex.ru'
                },
                'type': server_type,
            },
            'is_responsible': True
        },
    ]}
    assert data == expected


@pytest.mark.parametrize('server_type', SERVER_TYPE.choices())
def test_eine_access_is_not_uploaded(server_type, users, settings, sources):
    server = create_server('server.net')
    create_access_rule('eine', users.frodo, server)

    update_puncher_rules_cache()
    data = get_puncher_rules()

    assert data == {'rules': []}


def test_puncher_cache_redirect(client):
    update_puncher_rules_cache()
    response = client.json.get(
        '/puncher/rules/',
        HTTP_X_SSL_CLIENT_VERIFY='0',
        HTTP_X_SSL_CLIENT_SUBJECT=PUNCHER_DN,
    )
    assert response.has_header('X-Accel-Redirect')

    file_url = response._headers['x-accel-redirect'][1].split('/s3/', 1)[1]
    response = requests.get(file_url)
    assert response.json() == {'rules': []}
