import pytest
import json

from mock import mock

from django.utils.encoding import force_text

from infra.cauth.server.common.alchemy import Session

from infra.cauth.server.common.constants import FLOW_TYPE, KEYS_INFO_ATTR
from infra.cauth.server.common.models import Source
from __tests__.utils.create import (
    create_server_trusted_source_relation,
    create_servergroup_trusted_source_relation,
    create_server_group,
)


def parse_response(response):
    user_keys = {}
    admin_keys = {}
    current_keys = None
    for line in force_text(response.content).splitlines():
        if line.startswith("######## KEYS FOR USERS FROM RULES"):
            current_keys = user_keys
        elif line.startswith("######## KEYS FOR SERVER ADMINS"):
            current_keys = admin_keys
        else:
            data = line.split(" : ")
            if len(data) == 2:
                current_keys[data[0]] = data[1]

    return user_keys, admin_keys


@pytest.mark.parametrize('flow', FLOW_TYPE.choices())
def test_server_flow(client, sources, server, public_keys, flow):
    if flow == FLOW_TYPE.BACKEND_SOURCES:
        walle = Source.get_one(name='walle')
        server_group = create_server_group('walle.server_group', walle)
        server.groups.append(server_group)
        create_servergroup_trusted_source_relation(server_group, sources['golem'])

    server.set_flow(flow)

    client.server = server
    create_server_trusted_source_relation(server, sources['golem'])

    with mock.patch('infra.cauth.server.public.api.views.keys.MasterApiClient.update_server') as update_mock:
        response = client.get('/userkeys/?sources=golem,conductor')

    assert response.status_code == 200
    user_keys, admin_keys = parse_response(response)

    if flow == FLOW_TYPE.CLASSIC:
        assert update_mock.call_count == 1
        assert set(admin_keys.keys()) == set(("user_conductor_1", "user_conductor_2", "user_golem", "user_super"))
    else:
        assert not update_mock.called
        assert set(admin_keys.keys()) == set(("user_golem",))


@pytest.mark.parametrize('key_sources', ('staff', 'staff,secure,insecure',))
@pytest.mark.parametrize('secure_ca_list_url', (None, 'mock-secure_ca_list_url',))
@pytest.mark.parametrize('insecure_ca_list_url', (None, 'mock-insecure_ca_list_url',))
@pytest.mark.parametrize('krl_url', (None, 'mock-krl_url',))
@pytest.mark.parametrize('sudo_ca_list_url', (None, 'mock-sudo_ca_list_url',))
@pytest.mark.parametrize('set_via_group', (True, False))
def test_keys_info(
        client, server, key_sources, secure_ca_list_url, insecure_ca_list_url, krl_url,
        sudo_ca_list_url, set_via_group,
):
    if set_via_group:
        walle = Source.get_one(name='walle')
        server_group = create_server_group('walle.server_group', walle)
        server_group.key_sources = key_sources
        server_group.secure_ca_list_url = secure_ca_list_url
        server_group.insecure_ca_list_url = insecure_ca_list_url
        server_group.krl_url = krl_url
        server_group.sudo_ca_list_url = sudo_ca_list_url
        server.groups.append(server_group)
    else:
        server.key_sources = key_sources
        server.secure_ca_list_url = secure_ca_list_url
        server.insecure_ca_list_url = insecure_ca_list_url
        server.krl_url = krl_url
        server.sudo_ca_list_url = sudo_ca_list_url

    client.server = server
    Session.flush()

    response = client.get('/keysinfo/')

    expected_data = {
        "key_sources": key_sources.split(","),
        "secure_ca_list_url": secure_ca_list_url or KEYS_INFO_ATTR.DEFAULTS["secure_ca_list_url"],
        "insecure_ca_list_url": insecure_ca_list_url or KEYS_INFO_ATTR.DEFAULTS["insecure_ca_list_url"],
        "krl_url": krl_url or KEYS_INFO_ATTR.DEFAULTS["krl_url"],
        "sudo_ca_list_url": sudo_ca_list_url or KEYS_INFO_ATTR.DEFAULTS["sudo_ca_list_url"],
    }

    assert response.status_code == 200
    assert json.loads(response.content) == expected_data
