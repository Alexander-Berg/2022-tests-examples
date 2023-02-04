from mock import mock
import pytest
from waffle.testutils import override_switch

from django.conf import settings

from infra.cauth.server.common.alchemy import Session
from infra.cauth.server.common.constants import FLOW_TYPE, SERVER_TYPE
from infra.cauth.server.common.models import Server
from infra.cauth.server.master.api.models import DNS_STATUS

from __tests__.utils import create_server


pytestmark = pytest.mark.django_db
TEST_CLASSIC_DN = ('/C=RU/ST=Russia/L=Moscow/O=Yandex/OU=ITO'
                   '/CN=client.c.yandex-team.ru/emailAddress=rccs-admin@yandex-team.ru')
TEST_MODERN_DN = ('/C=RU/ST=Moscow/L=Moscow/O=Yandex LLC/OU=ITO'
                  '/CN=robot-walle@yandex-team.ru/emailAddress=wall-e@yandex-team.ru')
TEST_UPDATE_DN = ('/C=RU/ST=Moscow/L=Moscow/O=Yandex LLC/OU=ITO'
                  '/CN=robot-cauth-test@yandex-team.ru/emailAddress=cauth-dev@yandex-team.ru')


@pytest.mark.parametrize('fqdn,should_add,should_push', [
    ['hello.yandex.net', True, True],
    ['vm-e2e-head1-network-1521137421-trk-1.pre-df.cloud.yandex.net', True, False],
    ['man-pfp80g7sqqa4ziv0.db.yandex.net', True, False],
    ['sas1-79818febe86d.qloud-c.yandex.net', False, False],
])
@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_ignore_idm_push(mocked, fqdn, client, users, sources, should_add, should_push):
    request_data = {
        'srv': fqdn,
        'resp': 'frodo',
        'grp': 'walle.some_group',
    }
    with mock.patch('infra.cauth.server.master.api.idm.update.IdmClient.perform_batch') as perform_batch,\
            mock.patch('infra.cauth.server.master.api.idm.update.create_dst_requests') as create_requests:
        create_requests.return_value = ['request1', 'request2']
        response = client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        if should_push:
            assert len(create_requests.call_args_list) == 2
            assert len(perform_batch.call_args_list) == 2
        else:
            assert len(create_requests.call_args_list) == 0
            assert len(perform_batch.call_args_list) == 0

    if should_add:
        expected_code = 200
        expected = {
            'status': 'added',
            'srv': fqdn,
        }
        assert response.json() == expected
        server = Server.query.filter_by(fqdn=fqdn).first()
        assert len(server.sources) == 1
        assert server.sources[0].name == 'walle'
        if should_push:
            assert server.idm_status == 'dirty'
        else:
            assert server.idm_status == 'actual'
    else:
        expected_code = 200
        expected = 'srv: {}: server fqdn is ignored in CAuth'.format(fqdn)

        assert response.content == expected.encode('utf-8')
    assert response.status_code == expected_code

    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object') as push_idm_object:
        response = client.json.post(
            '/remove_server/',
            {'srv': fqdn},
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        if should_add:
            expected_code = 200
            expected = {
                'status': 'removed',
                'srv': fqdn,
            }
            assert response.json() == expected
        else:
            expected = 'srv: Server not found.'
            expected_code = 200
            assert response.content == expected.encode('utf-8')
        if should_push:
            assert len(push_idm_object.delay.call_args_list) == 1
        else:
            assert len(push_idm_object.delay.call_args_list) == 0
        assert response.status_code == expected_code


@pytest.mark.parametrize('disable_idm_pushes', [True, False])
@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_disable_idm_pushes_switch(mocked, client, users, sources, disable_idm_pushes):
    request_data = {
        'srv': 'hello.yandex.net',
        'resp': 'frodo',
        'grp': 'walle.some_group',
    }
    with mock.patch('infra.cauth.server.master.api.idm.update.IdmClient.perform_batch') as perform_batch,\
            mock.patch('infra.cauth.server.master.api.idm.update.create_dst_requests') as create_requests,\
            override_switch('cauth.disable_idm_pushes', active=disable_idm_pushes):
        create_requests.return_value = ['request1', 'request2']
        client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        if disable_idm_pushes:
            assert len(create_requests.call_args_list) == 0
            assert len(perform_batch.call_args_list) == 0
        else:
            assert len(create_requests.call_args_list) == 2
            assert len(perform_batch.call_args_list) == 2


@pytest.mark.parametrize('type_', [None, 'somerandomtype'] + list(SERVER_TYPE.choices()))
@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_add_server_with_specified_type(mocked, client, users, default_source, sources, type_):
    request_data = {
        'srv': 'server1.yandex.net',
        'resp': 'frodo',
        'grp': 'walle.some_group',
    }
    if type_ is not None:
        request_data['type'] = type_

    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object') as push_idm_object:
        response = client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        if type_ == 'somerandomtype':
            expected = 0
        else:
            expected = 2
        assert len(push_idm_object.delay.call_args_list) == expected

    assert response.status_code == 200

    if type_ == 'somerandomtype':
        assert response.content == b'type: Select a valid choice. somerandomtype is not one of the available choices.'
        assert Server.query.count() == 0
    else:
        expected = {
            'status': 'added',
            'srv': 'server1.yandex.net',
        }
        assert response.status_code == 200
        assert response.json() == expected
        server = Server.query.filter_by(fqdn='server1.yandex.net').first()
        assert server.first_pending_push_started_at is not None
        if type_ in SERVER_TYPE.choices():
            assert server.type == type_
        elif type_ is None:
            assert server.type == SERVER_TYPE.DEFAULT
        else:
            assert False, 'Should not be here'


@pytest.mark.parametrize('initial_type', SERVER_TYPE.choices())
@pytest.mark.parametrize('new_type', [None, 'somerandomtype'] + list(SERVER_TYPE.choices()))
@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_update_server_with_specified_type(mocked, client, users, default_source, sources, initial_type, new_type):
    new_server = Server(fqdn='server1.yandex.net', type=initial_type, client_version='cauth/100')
    Session.add(new_server)
    Session.commit()

    request_data = {
        'fqdn': 'server1.yandex.net',
    }
    if new_type is not None:
        request_data['type'] = new_type

    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object') as push_idm_object:
        response = client.json.post(
            '/update_server/',
            request_data,
            content_type='application/json',
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_UPDATE_DN,
        )
    assert response.status_code == 200
    assert len(push_idm_object.delay.call_args_list) == 0  # ничего IDM'ного не обновилось
    if new_type is not None:
        assert response.json() == {'status': 'ok'}
    else:
        assert response.json() == {'status': 'fail'}

    server = Server.query.first()
    assert server.idm_status == 'actual'

    if new_type in ['somerandomtype', None]:
        assert server.type == initial_type
    else:
        assert server.type == new_type


@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_update_responsibles_on_add_existing_server(mocked, client, users, sources):
    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object'):

        request_data = {
            'srv': 'server1.yandex.net',
            'resp': 'frodo',
            'grp': 'walle.some_group',
        }
        response = client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )

        expected_response = {'status': 'added', 'srv': 'server1.yandex.net'}
        assert response.json() == expected_response

        server = Session.query(Server).filter(Server.fqdn == 'server1.yandex.net').first()
        assert {'frodo'} == {u.login for u in server.responsible_users}
        assert server.idm_status == 'dirty'
        server.idm_status = 'actual'
        Session.commit()

        request_data = {
            'srv': 'server1.yandex.net',
            'resp': 'frodo,gandalf,legolas',
            'grp': 'walle.some_group',
        }
        client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        server = Session.query(Server).filter(Server.fqdn == 'server1.yandex.net').first()
        assert {'frodo', 'gandalf', 'legolas'} == {u.login for u in server.responsible_users}
        assert server.idm_status == 'dirty'
        server.idm_status = 'actual'
        Session.commit()

        request_data = {
            'srv': 'server1.yandex.net',
            'resp': 'gandalf',
            'grp': 'walle.some_group',
        }
        client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        server = Session.query(Server).filter(Server.fqdn == 'server1.yandex.net').first()
        assert {'gandalf'} == {u.login for u in server.responsible_users}
        assert server.idm_status == 'dirty'
        server.idm_status = 'actual'
        Session.commit()

        request_data = {
            'srv': 'server1.yandex.net',
            'resp': 'legolas',
            'grp': 'walle.some_group',
        }
        client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        server = Session.query(Server).filter(Server.fqdn == 'server1.yandex.net').first()
        assert {'legolas'} == {u.login for u in server.responsible_users}
        assert server.idm_status == 'dirty'
        server.idm_status = 'actual'
        Session.commit()

        request_data = {
            'srv': 'server1.yandex.net',
            'resp': 'frodo,gandalf',
            'grp': 'walle.some_group',
        }
        client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        server = Session.query(Server).filter(Server.fqdn == 'server1.yandex.net').first()
        assert {'frodo', 'gandalf'} == {u.login for u in server.responsible_users}
        assert server.idm_status == 'dirty'
        server.idm_status = 'actual'
        Session.commit()

        # if 'resp' is not given, responsibles should be preserved
        client.json.post(
            '/add_server/',
            {
                'srv': 'server1.yandex.net',
                'grp': 'walle.some_group',
            },
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        server = Session.query(Server).filter(Server.fqdn == 'server1.yandex.net').first()
        assert {'frodo', 'gandalf'} == {u.login for u in server.responsible_users}
        assert server.idm_status == 'dirty'
        server.idm_status = 'actual'
        Session.commit()

        # 'resp' is empty, resetting responsibles
        client.json.post(
            '/add_server/',
            {
                'srv': 'server1.yandex.net',
                'grp': 'walle.some_group',
                'resp': '',
            },
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        server = Session.query(Server).filter(Server.fqdn == 'server1.yandex.net').first()
        assert {u.login for u in server.responsible_users} == set()
        assert server.idm_status == 'dirty'
        server.idm_status = 'actual'
        Session.commit()


@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_if_resp_is_required(mocked, client, users, default_source, sources):
    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object'):
        response = client.json.post(
            '/add_server/',
            {
                'srv': 'server1.yandex.net',
                'grp': 'walle.some_group',
            },
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        assert response.status_code == 200
        assert response.json() == {
            'status': 'added',
            'srv': 'server1.yandex.net'
        }
        server = Server.query.first()
        assert server.fqdn == 'server1.yandex.net'


def test_add_to_group(client, users, default_source, sources):
    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object'):
        request_data = {
            'srv': 'server1.yandex.net',
            'grp': 'walle.hello'
        }

        response = client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        assert response.status_code == 200
        assert response.json() == {'status': 'added', 'srv': 'server1.yandex.net'}
        server = Server.query.first()
        assert server.fqdn == 'server1.yandex.net'
        assert server.idm_status == 'dirty'
        assert len(server.groups) == 1
        group = server.groups[0]
        assert group.name == 'walle.hello'
        assert group.idm_status == 'dirty'


@pytest.mark.parametrize("key_sources", (None, 'staff,insecure'))
@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_add_with_key_sources(mocked, client, users, default_source, sources, key_sources):
    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object'):
        request_data = {
            'srv': 'server1.yandex.net',
            'grp': 'walle.some_group',
        }
        if key_sources is not None:
            request_data["key_sources"] = key_sources

        response = client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=TEST_MODERN_DN,
        )
        assert response.status_code == 200
        assert response.json() == {'status': 'added', 'srv': 'server1.yandex.net'}
        server = Server.query.first()
        assert server.fqdn == 'server1.yandex.net'
        assert server.idm_status == 'dirty'

        set_sources = set(key_sources.split(",")) if key_sources else {"staff"}
        assert set(server.key_sources.split(",")) == set_sources


@pytest.mark.parametrize('grp_is_present', (True, False))
@pytest.mark.parametrize('new_flow', list(FLOW_TYPE.choices()) + [None])
@pytest.mark.parametrize('existing_flow', list(FLOW_TYPE.choices()) + [None])
@pytest.mark.parametrize('trusted_sources', ([], ['conductor']))
@pytest.mark.parametrize('verify_subject', (('conductor', TEST_CLASSIC_DN), ('walle', TEST_MODERN_DN)))
@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_add_server_with_specified_flow(
        mocked, client, users, sources, new_flow, existing_flow, trusted_sources, verify_subject, grp_is_present
):
    client_cert_owner = verify_subject[0]
    if existing_flow is not None:
        srv_params = dict(
            fqdn="server1.yandex.net",
            flow=existing_flow
        )
        if existing_flow == FLOW_TYPE.BACKEND_SOURCES:
            srv_params['groups'] = 'walle.some_group'
        create_server(**srv_params)

    request_data = {
        'srv': 'server1.yandex.net',
        'resp': 'frodo',
    }
    if new_flow is not None:
        request_data['flow'] = new_flow
    if len(trusted_sources) > 0:
        request_data['trusted_sources'] = ",".join(trusted_sources)
    if grp_is_present:
        request_data['grp'] = '{}.some_group'.format(client_cert_owner)

    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object'),\
            mock.patch('infra.cauth.server.master.api.tasks.push_idm_object') as mock_push_idm_object:
        response = client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=verify_subject[1],
        )

    expected = {
        'status': 'added',
        'srv': 'server1.yandex.net',
    }
    if new_flow and client_cert_owner == 'conductor':
        assert response.status_code == 400
        assert response.content == b"Source 'conductor' is not allowed to set flow"
        return

    if not grp_is_present and client_cert_owner in settings.CAUTH_FLOW_SOURCES_BY_PRIO:
        assert response.status_code == 400
        assert response.content == (
            "'grp' parameter for source '{}' must contain exact one name.".format(client_cert_owner).encode('utf-8')
        )
        return

    assert response.status_code == 200
    assert response.json() == expected

    server = Server.query.filter_by(fqdn='server1.yandex.net').first()

    assert server.flow == new_flow or existing_flow or FLOW_TYPE.CLASSIC

    if new_flow == FLOW_TYPE.BACKEND_SOURCES:
        expected_server_sources = set()
        expected_server_sources.add(client_cert_owner)
        expected_server_sources.update(trusted_sources)
        assert {source.name for source in server.trusted_sources} == expected_server_sources
        assert mock_push_idm_object.delay.called
    else:
        assert not mock_push_idm_object.delay.called


@pytest.mark.parametrize(
    'existing_groups', (
        [],
        ['walle.old_group'],
        ['conductor.old_group'],
        ['conductor.old_group', 'walle.old_group'],
    )
)
@pytest.mark.parametrize(
    'new_groups', (
        [],
        ['walle.old_group'],
        ['walle.new_group'],
        ['walle.new_group', 'walle.new_group2'],
        ['conductor.old_group'],
    )
)
@pytest.mark.parametrize('new_flow', list(FLOW_TYPE.choices()) + [None])
@pytest.mark.parametrize('existing_flow', list(FLOW_TYPE.choices()) + [None])
@pytest.mark.parametrize('existing_source', [None, 'conductor', 'walle'])
@pytest.mark.parametrize('trusted_sources', ([], ['conductor']))
@pytest.mark.parametrize('verify_subject', (('conductor', TEST_CLASSIC_DN), ('walle', TEST_MODERN_DN)))
@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_add_server_groups_and_sources(
    mocked, client, users, sources, new_flow, existing_flow, trusted_sources,
        verify_subject, new_groups, existing_groups, existing_source
):
    client_cert_owner = verify_subject[0]
    srv_params = dict(
        fqdn='server1.yandex.net',
        flow=existing_flow,
    )
    if (
            existing_flow == FLOW_TYPE.BACKEND_SOURCES
            and
            not any([name.split('.')[0] in settings.CAUTH_FLOW_SOURCES_BY_PRIO for name in existing_groups])
    ):
        return
    if existing_groups:
        srv_params['groups'] = ','.join(existing_groups)
    if existing_source:
        srv_params['source_name'] = existing_source
    server = create_server(**srv_params)

    request_data = {
        'srv': 'server1.yandex.net',
        'resp': 'frodo',
    }
    if new_flow:
        request_data['flow'] = new_flow
    if trusted_sources:
        request_data['trusted_sources'] = ','.join(trusted_sources)
    if new_groups:
        request_data['grp'] = ','.join(new_groups)

    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object'), \
            mock.patch('infra.cauth.server.master.api.tasks.push_idm_object'):
        response = client.json.post(
            '/add_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=verify_subject[1],
        )

    if client_cert_owner == 'conductor' and new_flow:
        assert response.status_code == 400
        assert response.content == b"Source 'conductor' is not allowed to set flow"
        return

    if new_groups and not all([name.split('.')[0] == client_cert_owner for name in new_groups]):
        assert response.status_code == 400
        assert response.content == "Group name prefix must equal '{}'".format(client_cert_owner).encode('utf-8')
        return

    if client_cert_owner == 'walle':
        if len(new_groups) != 1:
            assert response.status_code == 400
            assert response.content == b"'grp' parameter for source 'walle' must contain exact one name."
            return

    expected = {
        'status': 'added',
        'srv': 'server1.yandex.net',
    }
    assert response.json() == expected

    expected_groups = set(existing_groups) | set(new_groups)
    new_walle_group_set = {group for group in new_groups if group.split('.')[0] == 'walle'}
    if new_walle_group_set:
        old_walle_group_set = {group for group in existing_groups if group.split('.')[0] == 'walle'}
        expected_groups -= old_walle_group_set
        expected_groups |= new_walle_group_set

    assert {g.name for g in server.groups} == expected_groups

    expected_sources = {client_cert_owner}
    if existing_source:
        expected_sources.add(existing_source)

    assert {s.name for s in server.sources} == expected_sources


@pytest.mark.parametrize('flow', FLOW_TYPE.choices())
@pytest.mark.parametrize('verify_subject', (('conductor', TEST_CLASSIC_DN), ('walle', TEST_MODERN_DN)))
@mock.patch('infra.cauth.server.master.utils.dns_status.get_dns_status', return_value=DNS_STATUS.OK)
def test_remove_server(mocked, client, sources, flow, verify_subject):
    srv_params = dict(
        fqdn="server1.yandex.net",
        flow=flow
    )
    if flow == FLOW_TYPE.BACKEND_SOURCES:
        srv_params['groups'] = 'walle.some_group'
    create_server(**srv_params)

    request_data = {
        'srv': 'server1.yandex.net',
    }

    with mock.patch('infra.cauth.server.master.api.views.servers.push_idm_object'):
        response = client.json.post(
            '/remove_server/',
            request_data,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT=verify_subject[1],
        )

    if flow == FLOW_TYPE.BACKEND_SOURCES and verify_subject[0] != 'walle':
        assert response.status_code == 403
        assert Server.query.count() == 1
        return

    assert response.status_code == 200
    assert Server.query.count() == 0
    assert response.json() == {'status': 'removed', 'srv': 'server1.yandex.net'}
