from ids.exceptions import BackendError
from ok.utils.staff import StaffUser
from ok.api.core.forms import STAFF_USER_EXTERNAL
import requests


STAFF_USER_INTERNAL = 'internal'


def _mock_get_staff_iter_500(resource_type, params):
    raise BackendError('Staff-api 500')


def _mock_group_members(groups):
    return {'{}_member'.format(group) for group in groups}


def _mock_staff_users(self, logins):
    return {login: STAFF_USER_INTERNAL for login in logins}


def _mock_external_users(self, logins):
    return {login: STAFF_USER_EXTERNAL for login in logins}


def _mock_get_validated_map_staff_users(logins):
    return {
        login: {'login': login, 'fullname': login, 'affiliation': 'yandex'}
        for login in logins
    }


def _mock_table_flow(url, **kwargs):
    r = requests.Response()
    r.status_code = 200
    r.json = lambda: {'login': 'tester'}
    return r
