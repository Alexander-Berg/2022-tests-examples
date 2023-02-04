import pytest

from infra.cauth.server.common.alchemy import Session

from django.utils.encoding import force_text

from infra.cauth.server.public.utils.sudo import is_correct_sudoers
from __tests__.utils.create import create_access_rule


def test_correct_sudoers(client, server):
    client.server = server
    response = client.get('/sudoers/')

    assert response.status_code == 200
    assert is_correct_sudoers(response.content)


def test_is_correct_sudoers():
    sudoers_text = '''
        login ALL=(ALL) ALL
        %wheel ALL=(ALL) NOPASSWD: ALL
    '''
    assert is_correct_sudoers(sudoers_text)


def test_is_correct_sudoers_false():
    sudoers_text = '''
        login: ALL=(ALL) ALL
        %wheel ALL=(ALL NOPASSWD: ALL
    '''
    assert not is_correct_sudoers(sudoers_text)


@pytest.mark.parametrize('rule_type', ['ssh', 'sudo'])
@pytest.mark.parametrize('deleted_src_type', ['user', 'group'])
def test_deleted_src_not_in_rule_list(server, users, client, default_user_group, deleted_src_type, rule_type):
    user_ssh = users['user_ssh']

    urls = {
        'ssh': '/access/',
        'sudo': '/sudoers/',
    }
    url = urls[rule_type]

    obj_map = {
        'user': {
            'obj': user_ssh,
            'repr': '{} '.format(user_ssh.login),
        },
        'group': {
            'obj': default_user_group,
            'repr': '{} '.format(default_user_group.name),
        },
    }
    deleted_src = obj_map[deleted_src_type]['obj']
    deleted_src_repr = obj_map[deleted_src_type]['repr']

    create_access_rule(rule_type, user_ssh, server)
    create_access_rule(rule_type, default_user_group, server)

    response = client.get(url, {'q': server.fqdn})
    assert deleted_src_repr in force_text(response.content)

    Session.delete(deleted_src)
    Session.commit()

    response = client.get(url, {'q': server.fqdn})
    assert deleted_src_repr not in force_text(response.content)
