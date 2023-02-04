from django.conf import settings

from ad_system.ad.exceptions import ADError, WrongADRoleError


class LdapMock:
    def __init__(self):
        self.known_groups = ['OU=group1', 'OU=group2', 'OU=group3']
        self.known_users = ['frodo', 'sam', 'login']
        self.members = set()
        self.responsibles = {('frodo', 'OU=group1')}
        self.system_group_relations = set()
        self.group_data = {
            'OU=group1': {
                'extensionAttribute1': 'true',
                'extensionAttribute2': [b'(frodo)']
            },
            'OU=group2': {
                'extensionAttribute1': 'kek',
            },
            'OU=group3': {
                'extensionAttribute1': '',
            }
        }

        self.members.add(('sam', 'OU=group1'))

    def connect(self):
        pass

    def disconnect(self):
        pass

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass

    def search(self, base, search_flt, search_fields):
        _ = base
        _ = search_fields

        if search_flt == '(objectclass=group)':
            # Поиск по группам
            result = [(name, data) for name, data in self.group_data.items()]
        else:
            # Поиск по пользователям
            result = [
                ('frodo', {
                    'sAMAccountName': [b'frodo'],
                    'memberOf': [b'OU=group2'],
                }),
                ('sam', {
                    'sAMAccountName': [b'sam'],
                    'memberOf': [b'OU=group1']
                }),
                ('login', {
                    'sAMAccountName': [b'login'],
                })
            ]

        for name, info in result:
            yield name, info

    def foo(self):
        return 'bar'

    def _change_member(self, user: str, group_dn: str, membership_type: str, action: str, **kwargs):
        """
        Добавить/удалить учасника/ответственного
        :param user:
        :param group_dn:
        :param membership_type: 'member' or 'responsible'
        :param action: 'add' or 'remove'
        :return:
        """
        if user not in self.known_users:
            raise ADError(f'Пользователь {user} не существует в AD')
        if group_dn not in self.known_groups:
            raise WrongADRoleError(f'Группа {group_dn} не находится под контролем IDM')

        if membership_type == 'member':
            existing_memberships = self.members
        elif membership_type == 'responsible':
            existing_memberships = self.responsibles
        elif membership_type == 'system_group_relation':
            existing_memberships = self.system_group_relations
        else:
            raise ValueError(membership_type)
        membership = (user, group_dn)
        if action == 'add':
            if membership in existing_memberships:
                raise ADError(f'Пользователь {user} уже является {membership_type} в {group_dn}')
            existing_memberships.add(membership)
        elif action == 'remove':
            if membership not in existing_memberships:
                raise ADError(f'Пользователь {user} не является {membership_type} в {group_dn}')
            existing_memberships.remove(membership)

    def fetch_ad_group_data(self, group_dn: str, fields):
        data = self.group_data.get(group_dn)
        if data is None:
            return data
        return {
            key: value
            for key, value in data.items()
            if key in fields
        }

    def update_extension_attribute_1(self, group_dn: str, value: str):
        self.group_data[group_dn]['extensionAttribute1'] = value

    def create_ad_group(self, group_dn: str, system_slug: str):
        if not group_dn.endswith(settings.AD_LDAP_IDM_OU):
            raise WrongADRoleError(f'Невозможно создать группу {group_dn}')

        self.group_data[group_dn] = {
            'extensionAttribute1': system_slug
        }

    @staticmethod
    def fetch_extension_attribute_1(group_info: dict):
        return group_info['extensionAttribute1']

    def clean_extension_attribute_1(self, group_dn: str):
        if 'extensionAttribute1' in self.group_data[group_dn]:
            self.group_data[group_dn].pop('extensionAttribute1')

    def add_user_to_ad_group(self, user: str, group_dn: str):
        self._change_member(user, group_dn, 'member', 'add')

    def add_responsible_user_to_ad_group(self, user: str, group_dn: str):
        self._change_member(user, group_dn, 'responsible', 'add')

    def remove_user_from_ad_group(self, user: str, group_dn: str, is_fired=False):
        _ = is_fired
        self._change_member(user, group_dn, 'member', 'remove')

    def remove_responsible_user_from_ad_group(self, user: str, group_dn: str, is_fired=False):
        _ = is_fired
        self._change_member(user, group_dn, 'responsible', 'remove')
