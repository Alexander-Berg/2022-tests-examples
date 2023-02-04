import pytest
from mock import MagicMock

from infra.cauth.server.master.api.idm.dsts import ServerDst, GroupDst
from infra.cauth.server.master.api.idm.update import ApiNode, create_dst_requests
from infra.cauth.server.master.api.idm import client
from __tests__.utils import create_server, get_or_create_server_group


def test_dashboard(client):
    client.get('/dashboard/celery/new/')

    pass


@pytest.mark.parametrize('source_name', ['cms', 'yp', 'walle'])
def test_get_nodes_for_regular_server(source_name, sources):
    dst = ServerDst(create_server('server.net', source_name=source_name))

    nodes = ApiNode.get_nodes(dst)
    result = [(node.parent, node.slug) for node in nodes]

    expected = [
        ('/dst/', dst.key),
        ('/dst/%s/' % dst.key, 'role'),
        ('/dst/%s/role/' % dst.key, 'ssh'),
        ('/dst/%s/role/' % dst.key, 'sudo'),
    ]
    assert result == expected


def test_get_nodes_for_bot_server(sources):
    dst = ServerDst(create_server('server.net', source_name='bot', is_baremetal=True))

    nodes = ApiNode.get_nodes(dst)
    result = [(node.parent, node.slug) for node in nodes]

    expected = [
        ('/dst/', dst.key),
        ('/dst/%s/' % dst.key, 'role'),
        ('/dst/%s/role/' % dst.key, 'ssh'),
        ('/dst/%s/role/' % dst.key, 'sudo'),
        ('/dst/%s/role/' % dst.key, 'eine'),
    ]
    assert result == expected


@pytest.mark.parametrize('source_name', ['cms', 'yp', 'walle'])
def test_get_nodes_for_regular_group(source_name, sources):
    dst = GroupDst(get_or_create_server_group(source_name))

    nodes = ApiNode.get_nodes(dst)
    result = [(node.parent, node.slug) for node in nodes]

    expected = [
        ('/dst/', dst.key),
        ('/dst/%s/' % dst.key, 'role'),
        ('/dst/%s/role/' % dst.key, 'ssh'),
        ('/dst/%s/role/' % dst.key, 'sudo'),
    ]
    assert result == expected


def test_get_nodes_for_bot_group(sources):
    dst = GroupDst(get_or_create_server_group('bot'))

    nodes = ApiNode.get_nodes(dst)
    result = [(node.parent, node.slug) for node in nodes]

    expected = [
        ('/dst/', dst.key),
        ('/dst/%s/' % dst.key, 'role'),
        ('/dst/%s/role/' % dst.key, 'ssh'),
        ('/dst/%s/role/' % dst.key, 'sudo'),
        ('/dst/%s/role/' % dst.key, 'eine'),
    ]
    assert result == expected


@pytest.mark.parametrize('status_code', [400, 500])
def test_log_while_push_to_idm(sources, monkeypatch, status_code):
    log_msg = []

    def log_mock(msg, *args, **kwargs):
        log_msg.append(msg)

    def post_mock(*args, **kwargs):
        return MagicMock(status_code=status_code)

    monkeypatch.setattr(client.logger, 'exception', log_mock)
    monkeypatch.setattr(client.requests.Session, 'post', post_mock)
    monkeypatch.setattr(client.IdmClient, 'fetch_rolenode_objects', lambda *a, **kw: [])

    group_name = 'bot'
    dst_obj = get_or_create_server_group(group_name)
    idm_client = client.IdmClient()
    requests = create_dst_requests(idm_client, dst_obj)
    with pytest.raises(client.IdmResponseError):
        idm_client.perform_batch(requests, dst_obj, '')
    assert "Failed to reach idm with status code {}".format(status_code) in log_msg[0]
    assert "dst {}".format(group_name) in log_msg[0]
