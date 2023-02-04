import pytest

from django.utils.encoding import force_text

from infra.cauth.server.public.constants import SOURCE_NAME
from __tests__.utils.create import create_access_rule


@pytest.mark.parametrize('nopasswd', [True, False])
def test_soduers_list(server, users, client, default_user_group, nopasswd):

    create_access_rule('sudo', users['user_ssh'], server, nopasswd=nopasswd)
    create_access_rule('sudo', users['user_ssh_2'], server, nopasswd=(not nopasswd))
    create_access_rule('sudo', default_user_group, server, nopasswd=(not nopasswd))

    response = client.get('/sudoers/', {'q': server.fqdn})

    rulelist = force_text(response.content).splitlines()
    rulelist = [rule for rule in rulelist if '########' not in rule]

    nopasswd_map = [1 if 'NOPASSWD: ' in rule else 0 for rule in rulelist]

    assert set(nopasswd_map) == {0, 1}
    assert nopasswd_map == sorted(nopasswd_map)


@pytest.mark.parametrize('endpoint', ['/sudoers/', '/access/'])
def test_404_when_host_not_found(client, endpoint):

    response = client.get(endpoint, {'q': 'unknown.yandex.ru'})
    assert response.status_code == 404
    assert response.content == b'Host not found'


@pytest.mark.parametrize('endpoint', ['/sudoers/', '/access/'])
@pytest.mark.parametrize('source', SOURCE_NAME.choices())
def test_200_when_access_not_found(server, client, source, endpoint):

    response = client.get(endpoint, {'q': server.fqdn, 'sources': source})
    assert response.status_code == 200
