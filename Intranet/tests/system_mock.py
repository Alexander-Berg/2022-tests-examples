import json
from copy import deepcopy
from functools import wraps
import traceback
from typing import Dict, Set

from attr import attributes, attrib

from idm.core.plugins.generic import Plugin as GenericPlugin

from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.users.models import Group


class PluginWithMockSystem(GenericPlugin):
    def __init__(self, system):
        super().__init__(system)
        self.system_mock: 'SystemMock' = MOCKS[system.group_policy]

    def _post_data(self, method, data, timeout=None, headers=None):
        try:
            method_f = getattr(self.system_mock, method.replace('-', '_'))
        except AttributeError:
            self.system_mock.errors.append(f"method `{method}` not found")
            return {'code': 500}
        return method_f(data)


def errors_to_code(f):
    @wraps(f)
    def wrapper(self: 'SystemMock', *args, **kwargs):
        try:
            return f(self, *args, **kwargs) or {'code': 200}
        except AssertionError as e:
            self.errors.append(traceback.format_exc())
            return {'code': 400}
        except Exception as e:
            self.errors.append(traceback.format_exc())
            return {'code': 500}

    return wrapper


class SystemMock:
    def __init__(self):
        self.errors = []
        self.roles = set()

    def reset(self):
        del self.errors[:]
        self.roles.clear()

    def dump_state(self) -> dict:
        raise NotImplementedError("abstract")

    @errors_to_code
    def add_batch_memberships(self, data: dict):
        raise NotImplementedError("abstract")

    @errors_to_code
    def remove_batch_memberships(self, data: dict):
        raise NotImplementedError("abstract")

    @errors_to_code
    def add_role(self, data: dict):
        raise NotImplementedError("abstract")

    @errors_to_code
    def remove_role(self, data: dict):
        raise NotImplementedError("abstract")


@attributes(slots=True, hash=True)
class MockSystemMembership:
    login = attrib(type=str)
    group_id = attrib(type=int)

    def dump(self, group_slug_per_id: Dict[int, str]):
        return self.login, group_slug_per_id[self.group_id]


@attributes(slots=True, hash=True)
class MockSystemMembershipWithLogin(MockSystemMembership):
    passport = attrib(type=str)

    def dump(self, group_slug_per_id: Dict[int, str]):
        return self.login, group_slug_per_id[self.group_id], self.passport


class SystemMockAwareOfMemberships(SystemMock):
    def __init__(self):
        super().__init__()
        self.memberships: Set[MockSystemMembership] = set()

    def reset(self):
        super().reset()
        self.memberships.clear()

    def dump_state(self, include_errors=True):
        """
        Возвращаем состояние системы, заменив ID групп на slug
        """
        slug_per_id = {g.external_id: g.slug for g in Group.objects.all()}
        slug_per_id[None] = None
        state = {
            'memberships': {mem.dump(slug_per_id) for mem in self.memberships},
            'roles': {(path, login, slug_per_id[group_id]) for path, login, group_id in self.roles}
        }
        if self.errors and include_errors:
            state['errors'] = deepcopy(self.errors)
        return state

    def make_membership(self, data) -> MockSystemMembership:
        login = data['login']
        group = data['group']
        assert group, 'membership with no group'
        return MockSystemMembership(login, group)

    def _change_group_membership_one(self, data: dict, action: str) -> str:
        item = self.make_membership(data)
        if action == 'add':
            self.memberships.add(item)
        elif action == 'remove':
            self.memberships.remove(item)
        else:
            raise ValueError(action)
        return ''

    def _change_batch_membership(self, data: dict, action: str) -> dict:
        items = json.loads(data.get('data'))
        if not items:
            return {'code': 204}
        response_data = []
        for item in items:
            try:
                self._change_group_membership_one(item, action)
            except AssertionError as e:
                item.pop('passport_login', None)
                error = str(e)
                item['error'] = error
                self.errors.append(error)
                response_data.append(item)
        if response_data:
            return {'code': 207, 'multi_status': response_data}
        else:
            return {'code': 200}

    def add_batch_memberships(self, data: dict) -> dict:
        return self._change_batch_membership(data, 'add')

    def remove_batch_memberships(self, data: dict) -> dict:
        return self._change_batch_membership(data, 'remove')

    @errors_to_code
    def add_role(self, data: dict):
        _ = data['fields']
        path = data['path']
        login = data.get('login')
        group_id = data.get('group')
        assert login or group_id
        self.roles.add((path, login, group_id))

    @errors_to_code
    def remove_role(self, data: dict):
        _ = data['fields']
        path = data['path']
        login = data.get('login')
        group_id = data.get('group')
        assert login or group_id
        self.roles.remove((path, login, group_id))


class SystemMockAwareOfMembershipsWithLogins(SystemMockAwareOfMemberships):
    def __init__(self):
        super().__init__()
        self.known_users = frozenset({(username, f'yndx-{username}') for username in (
            'frodo', 'bilbo', 'sam', 'meriadoc', 'peregrin',
            'aragorn', 'legolas', 'gandalf', 'gimli', 'boromir',
            'sauron', 'saruman', 'nazgul', 'witch-king-of-angmar',
            'galadriel', 'varda', 'manve')})

    def make_membership(self, data) -> MockSystemMembershipWithLogin:
        login = data['login']
        passport = data['passport_login']
        group = data['group']
        assert group, 'membership with no group'
        if (login, passport) not in self.known_users:
            raise AssertionError(f'pair login:{login} passport:{passport} not found')
        return MockSystemMembershipWithLogin(login, group, passport)

    @errors_to_code
    def add_role(self, data: dict):
        _ = data['fields']
        path = data['path']
        login = data.get('login')
        group_id = data.get('group')
        if login:
            assert login in set(u for u, p in self.known_users)
        self.roles.add((path, login, group_id))

    @errors_to_code
    def remove_role(self, data: dict):
        _ = data['fields']
        path = data['path']
        login = data.get('login')
        group_id = data.get('group')
        if login:
            assert login in set(u for u, p in self.known_users)
        self.roles.remove((path, login, group_id))


MOCKS = {
    SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS: SystemMockAwareOfMemberships(),
    SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS: SystemMockAwareOfMembershipsWithLogins()
}
