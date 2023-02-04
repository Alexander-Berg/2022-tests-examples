from unittest import mock

from ad_system.tests.conftest import LdapMock


def fetch_ad_group_info(group_name: str):
    return {
        'name': group_name,
        'unique_id': group_name,
        'roles': {
            'slug': 'group_roles',
            'name': {'ru': 'Роли группы', 'en': 'Group roles'},
            'values': {
                'member': {
                    'unique_id': f'{group_name}::member',
                    'name': {'ru': 'Участник', 'en': 'Member'}
                },
                'responsible': {
                    'unique_id': f'{group_name}::responsible',
                    'name': {'ru': 'Ответственный', 'en': 'Responsible'}
                }
            }
        }
    }


EXPECTED_INFO = {
    'code': 0,
    'roles': {
        'slug': 'type',
        'name': 'тип роли',
        'values': {
            'global': {
                'unique_id': 'global',
                'name': 'global',
                'roles': {
                    'slug': 'role',
                    'name': 'роль',
                    'values': {
                        'system_group_relation': {
                            'name': {'en': 'system-group relation', 'ru': 'связь системы с группой'},
                            'unique_id': 'system_group_relation',
                            'fields': [
                                {'is_required': True,
                                 'name': {'ru': 'слаг системы', 'en': 'system slug'},
                                 'slug': 'system'},
                                {'is_required': True, 'name': {'ru': 'DN группы', 'en': 'group DN'},
                                 'slug': 'group_dn'}]}}}},

            'roles_in_groups': {
                'unique_id': 'roles_in_groups',
                'name': 'roles_in_groups',
                'roles': {
                    'slug': 'ad_group',
                    'name': 'AD-группа',
                    'values': {
                        group_name: fetch_ad_group_info(group_name)
                        for group_name in ['OU=group1', 'OU=group2', 'OU=group3']
                    }
                }
            }
        }
    }
}


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_info(client):
    r = client.get('/idm/info/')
    assert r.status_code == 200
    assert r.json() == EXPECTED_INFO
