import mock
import pytest

from django.utils.encoding import force_text

from infra.cauth.server.common.constants import FLOW_TYPE
from infra.cauth.server.common.models import (
    PublicKey, Server, ServerGroup, Session, Source, User
)

from __tests__.utils.create import (
    add_user_to_responsibles,
    create_access_rule,
    create_public_key,
    create_server,
    create_server_group,
    create_servergroup_trusted_source_relation,
    create_user,
    create_user_group,
)


urls = [
    '/access/',
    '/sudoers/',
    '/userkeys/',
    '/adminkeys/',
    '/group/serveradmins/',
    '/group/serverusers/',
]


@pytest.fixture
def structure(sources):
    Session.query(User).delete()
    Session.query(Server).delete()
    Session.query(ServerGroup).delete()
    Session.commit()

    walle = Source.get_one(name='walle')
    conductor = Source.get_one(name='conductor')

    server = create_server(source_names=['walle', 'conductor'])
    server_group = create_server_group('walle.server_group', walle)
    server.groups.append(server_group)

    walle_resp = create_user(create_user_group(), login='walle_resp')
    conductor_resp = create_user(create_user_group(), login='conductor_resp')
    idm_root = create_user(create_user_group(), login='idm_root')
    idm_sudoer = create_user(create_user_group(), login='idm_sudoer')

    add_user_to_responsibles(server, walle_resp, walle)
    add_user_to_responsibles(server, conductor_resp, conductor)
    create_access_rule('ssh', idm_root, server, is_root=True)
    create_access_rule('ssh', idm_sudoer, server, is_root=False)
    create_access_rule('sudo', idm_sudoer, server, nopasswd=False)

    create_public_key(walle_resp)
    create_public_key(conductor_resp)
    create_public_key(idm_root)
    create_public_key(idm_sudoer)

    return {
        'walle': walle,
        'conductor': conductor,
        'server': server,
        'server_group': server_group,
        'walle_resp': walle_resp,
        'conductor_resp': conductor_resp,
        'idm_root': idm_root,
        'idm_sudoer': idm_sudoer,
    }


def assert_response_is_correct_for_source(structure, url, source, content):
    walle_resp = structure['walle_resp']
    conductor_resp = structure['conductor_resp']
    idm_root = structure['idm_root']
    idm_sudoer = structure['idm_sudoer']

    expectations_map = {
        'walle': {
            '/access/': {
                'should_be': [],
                'should_not_be': [walle_resp, conductor_resp, idm_root, idm_sudoer],
            },
            '/sudoers/': {
                'should_be': [],
                'should_not_be': [walle_resp, conductor_resp, idm_root, idm_sudoer],
            },
            '/userkeys/': {
                'should_be': [walle_resp],
                'should_not_be': [conductor_resp, idm_root, idm_sudoer],
            },
            '/adminkeys/': {
                'should_be': [walle_resp],
                'should_not_be': [conductor_resp, idm_root, idm_sudoer],
            },
            '/group/serveradmins/': {
                'should_be': [walle_resp],
                'should_not_be': [conductor_resp, idm_root, idm_sudoer],
            },
            '/group/serverusers/': {
                'should_be': [walle_resp],
                'should_not_be': [conductor_resp, idm_root, idm_sudoer],
            },
        },
        'conductor': {
            '/access/': {
                'should_be': [],
                'should_not_be': [walle_resp, conductor_resp, idm_root, idm_sudoer],
            },
            '/sudoers/': {
                'should_be': [],
                'should_not_be': [walle_resp, conductor_resp, idm_root, idm_sudoer],
            },
            '/userkeys/': {
                'should_be': [conductor_resp],
                'should_not_be': [walle_resp, idm_root, idm_sudoer],
            },
            '/adminkeys/': {
                'should_be': [conductor_resp],
                'should_not_be': [walle_resp, idm_root, idm_sudoer],
            },
            '/group/serveradmins/': {
                'should_be': [conductor_resp],
                'should_not_be': [walle_resp, idm_root, idm_sudoer],
            },
            '/group/serverusers/': {
                'should_be': [conductor_resp],
                'should_not_be': [walle_resp, idm_root, idm_sudoer],
            },
        },
        'idm': {
            '/access/': {
                'should_be': [idm_root, idm_sudoer],
                'should_not_be': [walle_resp, conductor_resp],
            },
            '/sudoers/': {
                'should_be': [idm_sudoer],
                'should_not_be': [walle_resp, conductor_resp, idm_root],
            },
            '/userkeys/': {
                'should_be': [idm_root, idm_sudoer],
                'should_not_be': [walle_resp, conductor_resp],
            },
            '/adminkeys/': {
                'should_be': [idm_root],
                'should_not_be': [walle_resp, conductor_resp, idm_sudoer],
            },
            '/group/serveradmins/': {
                'should_be': [idm_root],
                'should_not_be': [walle_resp, conductor_resp, idm_sudoer],
            },
            '/group/serverusers/': {
                'should_be': [idm_root, idm_sudoer],
                'should_not_be': [walle_resp, conductor_resp],
            },
        },
        'walle,idm': {
            '/access/': {
                'should_be': [idm_root, idm_sudoer],
                'should_not_be': [walle_resp, conductor_resp],
            },
            '/sudoers/': {
                'should_be': [idm_sudoer],
                'should_not_be': [walle_resp, conductor_resp, idm_root],
            },
            '/userkeys/': {
                'should_be': [walle_resp, idm_root, idm_sudoer],
                'should_not_be': [conductor_resp],
            },
            '/adminkeys/': {
                'should_be': [walle_resp, idm_root],
                'should_not_be': [conductor_resp, idm_sudoer],
            },
            '/group/serveradmins/': {
                'should_be': [walle_resp, idm_root],
                'should_not_be': [conductor_resp, idm_sudoer],
            },
            '/group/serverusers/': {
                'should_be': [walle_resp, idm_root, idm_sudoer],
                'should_not_be': [conductor_resp],
            },
        },
    }
    content = '\n'.join(line for line in force_text(content).splitlines() if '########' not in line)
    if 'keys' in url:
        for user in expectations_map[source][url]['should_be']:
            for public_key in PublicKey.query.filter(PublicKey.uid == user.uid).all():
                assert public_key.key in content
        for user in expectations_map[source][url]['should_not_be']:
            for public_key in PublicKey.query.filter(PublicKey.uid == user.uid).all():
                assert public_key.key not in content
    else:
        for user in expectations_map[source][url]['should_be']:
            assert user.login in content
        for user in expectations_map[source][url]['should_not_be']:
            assert user.login not in content


@pytest.mark.parametrize('q_is_present', (True, False))
@pytest.mark.parametrize('client_sources', ('walle', 'conductor', 'idm', 'walle,idm'))
@pytest.mark.parametrize('url', urls)
@mock.patch('infra.cauth.server.public.api.views.keys.MasterApiClient.update_server')
def test_classic_filter_by_source(update_server, url, client_sources, q_is_present, structure, client):
    server = structure['server']
    client_params = {'sources': client_sources}

    if q_is_present:
        client_params.update({'q': server.fqdn})
    else:
        client.server = server

    response = client.get(url, client_params)

    assert_response_is_correct_for_source(structure, url, client_sources, response.content)
    assert update_server.called == (url == '/userkeys/' and not q_is_present)


@pytest.mark.parametrize('flow', FLOW_TYPE.choices())
@pytest.mark.parametrize('url', urls)
@mock.patch('infra.cauth.server.public.api.views.keys.MasterApiClient.update_server')
def test_client_params_is_ignored_with_backend_sources(update_server, url, flow, structure, client):
    server = structure['server']
    server_group = structure['server_group']
    server.set_flow(flow)
    client.server = server

    backend_sources_setting = 'walle'
    query_sources_setting = 'idm'

    create_servergroup_trusted_source_relation(server_group, structure[backend_sources_setting])

    effective_sources_setting = backend_sources_setting
    if flow == FLOW_TYPE.CLASSIC:
        effective_sources_setting = query_sources_setting

    response = client.get(url, {'sources': query_sources_setting})

    assert_response_is_correct_for_source(structure, url, effective_sources_setting, response.content)
    assert update_server.called == (flow == FLOW_TYPE.CLASSIC and url == '/userkeys/')
