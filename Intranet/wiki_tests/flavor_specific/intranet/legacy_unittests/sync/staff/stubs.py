from wiki.sync.staff.mapping.models import GroupMapper, GroupMembershipMapper
from wiki.sync.staff.mapping.models import DepartmentMapper, StaffMapper
from wiki.sync.staff.mapping.registry import MapperRegistry

NEOFELIS_ID = 80382
NEOFELIS_DEPT_ID = 3578
NEOFELIS_PARENT_DEPT_ID = 3583
GROUP_ID = 759
GROUPB_ID = 760

grp_admins = {
    'is_deleted': False,
    '_meta': {'modified_at': '2020-10-14T01:00:04.250203+00:00'},
    'service': {'id': None},
    'url': 'admins',
    'description': 'Администраторы Центра и Вики',
    'affiliation_counters': {'yandex': 7, 'yamoney': 0, 'external': 1},
    'department': {'id': None},
    'type': 'wiki',
    'id': 759,
    'name': 'Admins',
}

grp_neadmins = {
    'is_deleted': False,
    '_meta': {'modified_at': '2020-10-14T01:00:04.250203+00:00'},
    'service': {'id': None},
    'url': 'neadmins',
    'description': 'Администраторы Центра и Вики',
    'affiliation_counters': {'yandex': 7, 'yamoney': 0, 'external': 1},
    'department': {'id': None},
    'type': 'wiki',
    'id': 760,
    'name': 'neAdmins',
}

staff_neofelis = {
    'language': {'content': '', 'ui': 'ru', 'native': 'ru'},
    'is_deleted': False,
    '_meta': {'modified_at': '2020-05-19T01:01:17.464340+00:00'},
    'department_group': {'department': {'id': NEOFELIS_DEPT_ID}},
    'work_phone': 31558,
    'cars': [],
    'created_at': '2020-03-03T11:35:33.662000+00:00',
    'official': {
        'quit_at': None,
        'join_at': '2020-03-03',
        'is_homeworker': False,
        'affiliation': 'yandex',
        'position': {'ru': 'Аааа', 'en': 'Techlead'},
        'is_dismissed': False,
        'employment': 'full',
        'is_robot': False,
    },
    'name': {'middle': '', 'last': {'ru': 'Аааа', 'en': 'ZAKHARCHENKO'}, 'first': {'ru': 'Аааа', 'en': 'SERGEI'}},
    'environment': {'timezone': 'Europe/Moscow'},
    'personal': {
        'family_status': '',
        'gender': 'male',
        'birthday': '1988-07-27',
        'children': 0,
        'address': {'ru': '', 'en': ''},
    },
    'login': 'neofelis',
    'guid': 'ab32c64e347b8144871659ca1fce6e1a',
    'work_email': 'neofelis@yandex-team.ru',
    'id': NEOFELIS_ID,
    'uid': '1120000000217694',
}

neofelis_department = {
    'department': {
        'is_deleted': False,
        'description': {'ru': '', 'en': ''},
        'contacts': {'wiki': '/content/', 'maillists': []},
        'url': 'yandex_infra_tech_tools_content_dev_wiki',
        'id': NEOFELIS_DEPT_ID,
        'name': {'full': {'ru': 'аааа', 'en': 'Group programming for computer'}, 'short': {'ru': '', 'en': ''}},
    },
    'id': 49793,
    'parent': {'department': {'id': 3583}},
    '_meta': {'modified_at': '2020-05-19T01:01:17.464340+00:00'},
}

parent_dept = {
    'department': {
        'is_deleted': False,
        'description': {'ru': '', 'en': ''},
        'contacts': {'wiki': '', 'maillists': []},
        'url': 'yandex_infra_tech_tools_access_dev',
        'id': NEOFELIS_PARENT_DEPT_ID,
        'name': {
            'full': {'ru': 'иии', 'en': 'Collaboration Tools Python Development Unit'},
            'short': {'ru': '', 'en': ''},
        },
    },
    'id': 49798,
    'parent': None,
    '_meta': {'modified_at': '2020-05-19T01:01:17.464340+00:00'},
}


class MockDatasource:
    def __init__(self, remote):
        self.remote = remote

    def get_object(self, lookup_dict):
        """
        Обычно lookup_dict это  {'путь-к-полю': pk},
        поэтому наш мок-словарь pk: mock-value и
        ищем мы по lookup_dict.items()[0][1]
        """
        try:
            return self.remote[list(lookup_dict.items())[0][1]]
        except KeyError:
            raise


class OverrideDefaultDatasource(object):
    datasource = None

    @classmethod
    def get_default_datasource(cls):
        return cls.datasource


mock_depts = MockDatasource({NEOFELIS_DEPT_ID: neofelis_department, NEOFELIS_PARENT_DEPT_ID: parent_dept})
mock_staff = MockDatasource({NEOFELIS_ID: staff_neofelis})


def gm_factory(group_id, person_id, idx):
    return {
        'person': {'id': person_id},
        'group': {'id': group_id},
        'id': idx,
        '_meta': {'modified_at': '2020-06-16T01:11:25.919945+00:00'},
    }


mock_groups = MockDatasource({GROUP_ID: grp_admins, GROUPB_ID: grp_neadmins})
mock_gm = MockDatasource({1: gm_factory(GROUP_ID, NEOFELIS_ID, 1)})


class StaffMockMapper(OverrideDefaultDatasource, StaffMapper):
    datasource = mock_staff


class DepartmentMockMapper(OverrideDefaultDatasource, DepartmentMapper):
    datasource = mock_depts


class GroupMockMapper(OverrideDefaultDatasource, GroupMapper):
    datasource = mock_groups


class GroupMembershipMockMapper(OverrideDefaultDatasource, GroupMembershipMapper):
    datasource = mock_gm


mock_registry = MapperRegistry()
mock_registry.register(DepartmentMockMapper)
mock_registry.register(GroupMockMapper)
mock_registry.register(GroupMembershipMockMapper)
mock_registry.register(StaffMockMapper)
