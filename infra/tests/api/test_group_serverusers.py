import pytest
from django.urls import reverse
from django.utils.encoding import force_text

from __tests__.utils.create import (
    create_server,
    create_user,
    create_access_rule,
    create_user_group,
    get_or_create_source,
    add_user_to_responsibles,
)

url = reverse('group-serverusers')


@pytest.fixture
def server():
    return create_server()


@pytest.fixture
def access_group():
    return create_user_group()


@pytest.fixture
def user_with_server_access(server, access_group):
    user = create_user(group=access_group)
    create_access_rule('ssh', user, server)
    return user


def parse_response_content(content):
    return [s for s in force_text(content).splitlines() if s and not s.startswith('#')]


def format_etc_group(group, users):
    logins = ','.join(sorted(u.login for u in users))
    return ':'.join([group.name, 'x', str(group.gid), logins])


def test_return_only_groups_with_user_access(client, server, access_group, user_with_server_access):
    """ Возвращает только группы пользователей, у которых есть доступ к серверу """
    another_group = create_user_group()
    create_user(group=another_group)

    response = client.get(url, {'q': server.fqdn})

    assert response.status_code == 200
    groups = parse_response_content(response.content)
    assert len(groups) == 1
    assert groups[0] == format_etc_group(access_group, [user_with_server_access])


def test_return_only_users_with_access(client, server, access_group, user_with_server_access):
    """ Возвращает в списке пользователей в группе только тех, у кого есть доступ к серверу """
    create_user(group=access_group)

    response = client.get(url, {'q': server.fqdn})

    assert response.status_code == 200
    groups = parse_response_content(response.content)
    assert len(groups) == 1
    assert groups[0] == format_etc_group(access_group, [user_with_server_access])


def test_return_all_users(client, server, access_group, user_with_server_access):
    """ Возвращает в списке пользователей в группе всех пользователей по отдельному флагу """
    user_without_server_access = create_user(group=access_group)

    response = client.get(url, {'q': server.fqdn, 'all_users': 1})

    assert response.status_code == 200
    groups = parse_response_content(response.content)
    assert len(groups) == 1
    expected_users = [user_without_server_access, user_with_server_access]
    assert groups[0] == format_etc_group(access_group, expected_users)


def test_404_for_without_host(client):
    response = client.get(url)
    assert response.status_code == 404


@pytest.mark.parametrize('all_users', [True, False])
def test_empty_groups_list_if_no_ssh_users(client, server, access_group, all_users):
    # в группе есть пользователь, но без доступа к серверу
    create_user(group=access_group)

    params = {'q': server.fqdn}
    if all_users:
        params['all_users'] = 1

    response = client.get(url, params)

    assert response.status_code == 200
    assert len(parse_response_content(response.content)) == 0


def test_responsibles_in_list(client, server, access_group):
    """ Пользователь ответственный за сервер """
    responsible_user = create_user(group=access_group)

    add_user_to_responsibles(server, responsible_user, get_or_create_source('some_name'))

    response = client.get(url, {'q': server.fqdn})

    assert response.status_code == 200
    groups = parse_response_content(response.content)
    assert len(groups) == 1
    assert groups[0] == format_etc_group(access_group, [responsible_user])


def test_both_resps_and_ssh_users_in_list(client, server, access_group, user_with_server_access):
    """ Ответственный и пользователь с доступом в общем списке """
    responsible_user = create_user(group=access_group)

    add_user_to_responsibles(server, responsible_user, get_or_create_source('some_name'))

    response = client.get(url, {'q': server.fqdn})

    assert response.status_code == 200
    groups = parse_response_content(response.content)
    assert len(groups) == 1
    assert groups[0] == format_etc_group(access_group, [user_with_server_access, responsible_user])


def test_responsibles_in_list_if_source_trusted(client, server, access_group):
    """ Пользователь, ответственный за сервер, попадает в список в зависимости от источника """
    responsible_user = create_user(group=access_group)
    other_responsible_user = create_user(group=access_group)

    some_source = get_or_create_source('some_name')
    other_source = get_or_create_source('other_name')

    add_user_to_responsibles(server, responsible_user, some_source)
    add_user_to_responsibles(server, other_responsible_user, other_source)

    response = client.get(url, {'q': server.fqdn, 'sources': some_source.name})
    groups = parse_response_content(response.content)
    assert groups[0] == format_etc_group(access_group, [responsible_user])

    response = client.get(url, {'q': server.fqdn, 'sources': other_source.name})
    groups = parse_response_content(response.content)
    assert groups[0] == format_etc_group(access_group, [other_responsible_user])

    response = client.get(url, {
        'q': server.fqdn, 'sources': ','.join([some_source.name, other_source.name])
    })
    groups = parse_response_content(response.content)
    assert groups[0] == format_etc_group(access_group, [responsible_user, other_responsible_user])
