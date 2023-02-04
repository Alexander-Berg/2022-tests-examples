import pytest

from django.conf import settings
from django.core.urlresolvers import reverse
from mock import patch

from plan.denormalization.tasks import update_denormalized_field
from plan.services.state import SERVICEMEMBER_STATE
from plan.oebs.models import STATES
from plan.services.api.services import ServicesView
from plan.services.models import Service, ServiceCreateRequest
from plan.suspicion.models import IssueGroup
from plan.puncher.rules import PuncherClient
from plan.services.constants.api_frontend import (
    DEEP_FILTER_MODE_LABEL,
    SHALLOW_FILTER_MODE_LABEL,
)
from plan.staff.constants import LANG
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def idm_response():
    return {
        'meta': {'limit': 20,
                 'next': '/api/v1/roles/?order_by=-updated&path=%2F&group=42868%2C114362&type=active&limit=20&offset=40',
                 'offset': 20,
                 'previous': '/api/v1/roles/?order_by=-updated&path=%2F&group=42868%2C114362&type=active&limit=20&offset=0',
                 'total_count': 61},
        'objects': [{'added': '2022-03-15T18:26:20.050597+00:00',
                     'data': {'role': 'PROJECT_ADMIN', 'type': 'project'},
                     'expire_at': None,
                     'fields_data': {'project': 'abc-production'},
                     'granted_at': '2022-03-15T18:26:24.554950+00:00',
                     'group': {'created_at': '2019-04-29T16:21:03.446933+00:00',
                               'id': 114362,
                               'name': 'DevOps (ABC (Каталог сервисов Яндекса))',
                               'slug': 'svc_abc_devops',
                               'state': 'active',
                               'type': 'service',
                               'updated_at': '2022-07-05T12:10:18.736235+00:00',
                               'url': 'https://abc.yandex-team.ru/services/abc/'},
                     'human': 'Тип роли: Роль в проекте, : Администратор проекта',
                     'human_short': 'Роль в проекте / Администратор проекта',
                     'human_state': 'Выдана',
                     'id': 64288839,
                     'is_active': True,
                     'is_public': None,
                     'node': {'data': {'role': 'PROJECT_ADMIN', 'type': 'project'},
                              'description': 'Может делать в проекте всё, включая '
                                             'добавление новых администраторов. Набор '
                                             'разрешений: monitoring.configs.list, '
                                             'monitoring.configs.get, '
                                             'monitoring.configs.create, '
                                             'monitoring.configs.update, '
                                             'monitoring.configs.delete, '
                                             'monitoring.roles.update, '
                                             'monitoring.sensors.get, '
                                             'monitoring.sensorLabels.get, '
                                             'monitoring.sensorNames.get, '
                                             'monitoring.data.read, '
                                             'monitoring.data.write, '
                                             'monitoring.mutes.get, '
                                             'monitoring.mutes.create, '
                                             'monitoring.mutes.delete, '
                                             'monitoring.mutes.update, '
                                             'jns.notifications.send, '
                                             'jns.notifications.get',
                              'human': 'Тип роли: Роль в проекте, : Администратор '
                                       'проекта',
                              'human_short': 'Роль в проекте / Администратор проекта',
                              'id': 56799038,
                              'is_auto_updated': False,
                              'is_key': False,
                              'is_public': True,
                              'name': 'Администратор проекта',
                              'set': None,
                              'slug': 'PROJECT_ADMIN',
                              'slug_path': '/type/project/role/PROJECT_ADMIN/',
                              'state': 'active',
                              'system': {'description': '',
                                         'endpoint_long_timeout': 60,
                                         'endpoint_timeout': 60,
                                         'group_policy': 'aware',
                                         'has_review': False,
                                         'id': 1728,
                                         'is_active': True,
                                         'is_broken': False,
                                         'is_sox': False,
                                         'name': 'Solomon(prestable)',
                                         'roles_review_days': 360,
                                         'slug': 'solomon_prestable',
                                         'state': 'Активна',
                                         'use_mini_form': False},
                              'unique_id': '',
                              'value_path': '/project/PROJECT_ADMIN/'},
                     'parent': None,
                     'review_at': None,
                     'review_date': None,
                     'review_days': None,
                     'state': 'granted',
                     'system': {'description': '',
                                'endpoint_long_timeout': 60,
                                'endpoint_timeout': 60,
                                'group_policy': 'aware',
                                'has_review': False,
                                'id': 1728,
                                'is_active': True,
                                'is_broken': False,
                                'is_sox': False,
                                'name': 'Solomon(prestable)',
                                'roles_review_days': 360,
                                'slug': 'solomon_prestable',
                                'state': 'Активна',
                                'use_mini_form': False},
                     'system_specific': {'project': 'abc-production'},
                     'ttl_date': None,
                     'ttl_days': None,
                     'updated': '2022-03-15T18:26:24.554950+00:00',
                     'user': None,
                     'with_external': True,
                     'with_inheritance': True,
                     'with_robots': True,
                     'without_hold': False},
                    {'added': '2019-02-22T12:50:16.443588+00:00',
                     'data': {'obj_key': '35149',
                              'perm_kind': 'acl_edit',
                              'type': 'statreport'},
                     'expire_at': None,
                     'fields_data': None,
                     'granted_at': '2021-05-07T18:09:30.238041+00:00',
                     'group': {'created_at': '2015-08-19T13:00:07+00:00',
                               'id': 42868,
                               'name': 'ABC (Каталог сервисов Яндекса)',
                               'slug': 'svc_abc',
                               'state': 'active',
                               'type': 'service',
                               'updated_at': '2022-05-31T18:36:09.274312+00:00',
                               'url': 'https://abc.yandex-team.ru/services/abc/'},
                     'human': 'Тип доступа: Отчёты, Отчёт: '
                              'ExtData/ABC/qloud_applications: Внешние данные / ABC / '
                              'Qloud-приложения, Тип доступа: На редактирование',
                     'human_short': 'Отчёты / ExtData/ABC/qloud_applications: Внешние '
                                    'данные / ABC / Qloud-приложения / На '
                                    'редактирование',
                     'human_state': 'Выдана',
                     'id': 7395835,
                     'is_active': True,
                     'is_public': None,
                     'node': {'data': {'obj_key': '35149',
                                       'perm_kind': 'acl_edit',
                                       'type': 'statreport'},
                              'description': '',
                              'human': 'Тип доступа: Отчёты, Отчёт: '
                                       'ExtData/ABC/qloud_applications: Внешние '
                                       'данные / ABC / Qloud-приложения, Тип доступа: '
                                       'На редактирование',
                              'human_short': 'Отчёты / '
                                             'ExtData/ABC/qloud_applications: Внешние '
                                             'данные / ABC / Qloud-приложения / На '
                                             'редактирование',
                              'id': 17728061,
                              'is_auto_updated': False,
                              'is_key': False,
                              'is_public': True,
                              'name': 'На редактирование',
                              'set': 'acl_edit',
                              'slug': 'acl_edit',
                              'slug_path': '/type/statreport/obj_key/35149/perm_kind/acl_edit/',
                              'state': 'active',
                              'system': {'description': '',
                                         'endpoint_long_timeout': 600,
                                         'endpoint_timeout': 600,
                                         'group_policy': 'unaware',
                                         'has_review': True,
                                         'id': 5,
                                         'is_active': True,
                                         'is_broken': False,
                                         'is_sox': True,
                                         'name': 'Статистика',
                                         'roles_review_days': 360,
                                         'slug': 'stat',
                                         'state': 'Активна',
                                         'use_mini_form': False},
                              'unique_id': '',
                              'value_path': '/statreport/35149/acl_edit/'},
                     'parent': None,
                     'review_at': '2022-05-02T18:09:30.238041+00:00',
                     'review_date': None,
                     'review_days': None,
                     'state': 'granted',
                     'system': {'description': '',
                                'endpoint_long_timeout': 600,
                                'endpoint_timeout': 600,
                                'group_policy': 'unaware',
                                'has_review': True,
                                'id': 5,
                                'is_active': True,
                                'is_broken': False,
                                'is_sox': True,
                                'name': 'Статистика',
                                'roles_review_days': 360,
                                'slug': 'stat',
                                'state': 'Активна',
                                'use_mini_form': False},
                     'system_specific': None,
                     'ttl_date': None,
                     'ttl_days': None,
                     'updated': '2021-05-07T18:09:30.238041+00:00',
                     'user': None,
                     'with_external': True,
                     'with_inheritance': True,
                     'with_robots': True,
                     'without_hold': False},
                    {'added': '2019-02-22T12:50:06.288319+00:00',
                     'data': {'obj_key': '35148',
                              'perm_kind': 'acl_edit',
                              'type': 'statreport'},
                     'expire_at': None,
                     'fields_data': None,
                     'granted_at': '2021-05-07T18:09:29.172223+00:00',
                     'group': {'created_at': '2015-08-19T13:00:07+00:00',
                               'id': 42868,
                               'name': 'ABC (Каталог сервисов Яндекса)',
                               'slug': 'svc_abc',
                               'state': 'active',
                               'type': 'service',
                               'updated_at': '2022-05-31T18:36:09.274312+00:00',
                               'url': 'https://abc.yandex-team.ru/services/abc/'},
                     'human': 'Тип доступа: Отчёты, Отчёт: ExtData/ABC/nanny: Внешние '
                              'данные / ABC / Nanny-сервисы, Тип доступа: На '
                              'редактирование',
                     'human_short': 'Отчёты / ExtData/ABC/nanny: Внешние данные / ABC '
                                    '/ Nanny-сервисы / На редактирование',
                     'human_state': 'Выдана',
                     'id': 7395806,
                     'is_active': True,
                     'is_public': None,
                     'node': {'data': {'obj_key': '35148',
                                       'perm_kind': 'acl_edit',
                                       'type': 'statreport'},
                              'description': '',
                              'human': 'Тип доступа: Отчёты, Отчёт: '
                                       'ExtData/ABC/nanny: Внешние данные / ABC / '
                                       'Nanny-сервисы, Тип доступа: На редактирование',
                              'human_short': 'Отчёты / ExtData/ABC/nanny: Внешние '
                                             'данные / ABC / Nanny-сервисы / На '
                                             'редактирование',
                              'id': 17728050,
                              'is_auto_updated': False,
                              'is_key': False,
                              'is_public': True,
                              'name': 'На редактирование',
                              'set': 'acl_edit',
                              'slug': 'acl_edit',
                              'slug_path': '/type/statreport/obj_key/35148/perm_kind/acl_edit/',
                              'state': 'active',
                              'system': {'description': '',
                                         'endpoint_long_timeout': 600,
                                         'endpoint_timeout': 600,
                                         'group_policy': 'unaware',
                                         'has_review': True,
                                         'id': 5,
                                         'is_active': True,
                                         'is_broken': False,
                                         'is_sox': True,
                                         'name': 'Статистика',
                                         'roles_review_days': 360,
                                         'slug': 'stat',
                                         'state': 'Активна',
                                         'use_mini_form': False},
                              'unique_id': '',
                              'value_path': '/statreport/35148/acl_edit/'},
                     'parent': None,
                     'review_at': '2022-05-02T18:09:29.172223+00:00',
                     'review_date': None,
                     'review_days': None,
                     'state': 'granted',
                     'system': {'description': '',
                                'endpoint_long_timeout': 600,
                                'endpoint_timeout': 600,
                                'group_policy': 'unaware',
                                'has_review': True,
                                'id': 5,
                                'is_active': True,
                                'is_broken': False,
                                'is_sox': True,
                                'name': 'Статистика',
                                'roles_review_days': 360,
                                'slug': 'stat',
                                'state': 'Активна',
                                'use_mini_form': False},
                     'system_specific': None,
                     'ttl_date': None,
                     'ttl_days': None,
                     'updated': '2021-05-07T18:09:29.172223+00:00',
                     'user': None,
                     'with_external': True,
                     'with_inheritance': True,
                     'with_robots': True,
                     'without_hold': False}]
    }


@pytest.fixture
def puncher_response():
    return {
        "count": 24,
        "links": {
            "first": "https://api.puncher.yandex-team.ru/api/dynfw/rules?service_id=989&status=active",
            "next": "https://api.puncher.yandex-team.ru/api/dynfw/rules?_cursor_last_id=smth&service_id=989&status=active"
        },
        "rules": [
            {
                "id": "57ce8b70d5626dfa1415b877",
                "request": None,
                "sources": [
                    {
                        "machine_name": "@srv_svc_abc_development@",
                        "type": "servicerole",
                        "title": {
                            "en": "Разработка (ABC (Каталог сервисов Яндекса))",
                            "ru": "Разработка (ABC (Каталог сервисов Яндекса))"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://abc.yandex-team.ru/services/989",
                        "is_user_type": True,
                        "highlight": True
                    }
                ],
                "destinations": [
                    {
                        "machine_name": "_C_CS_TOOLSBACK_",
                        "type": "macro",
                        "title": {
                            "en": "_C_CS_TOOLSBACK_",
                            "ru": "_C_CS_TOOLSBACK_"
                        },
                        "is_deleted": True,
                        "external": False,
                        "url": "https://racktables.yandex.net/index.php?page=macros&macro_name=_C_CS_TOOLSBACK_",
                        "is_user_type": False,
                        "highlight": False
                    }
                ],
                "protocol": "tcp",
                "ports": [
                    "443"
                ],
                "locations": [
                    "office",
                    "vpn"
                ],
                "until": "2022-09-06T09:25:04.35Z",
                "status": "active",
                "tasks": [
                    "DOSTUPREQ-10451",
                    "DOSTUPREQ-137841"
                ],
                "comment": "Проверка корректности работы бекенда ABC",
                "system": "puncher",
                "added": "2016-09-06T09:25:04.35Z",
                "updated": "2019-12-29T17:06:20.726Z",
                "readonly": False
            },
            {
                "id": "57e11ae30d0795cb1f48d96b",
                "request": None,
                "sources": [
                    {
                        "machine_name": "@srv_svc_abc_development@",
                        "type": "servicerole",
                        "title": {
                            "en": "Разработка (Каталог сервисов Яндекса (ABC))",
                            "ru": "Разработка (Каталог сервисов Яндекса (ABC))"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://abc.yandex-team.ru/services/989",
                        "is_user_type": True,
                        "highlight": True
                    }
                ],
                "destinations": [
                    {
                        "machine_name": "vs-planner-back.http.yandex.net",
                        "type": "virtualservice",
                        "title": {
                            "en": "vs-planner-back.http.yandex.net",
                            "ru": "vs-planner-back.http.yandex.net"
                        },
                        "is_deleted": True,
                        "external": False,
                        "is_user_type": False,
                        "highlight": False
                    }
                ],
                "protocol": "tcp",
                "ports": [
                    "443"
                ],
                "locations": [
                    "office",
                    "vpn"
                ],
                "until": None,
                "status": "obsolete",
                "tasks": [
                    "DOSTUPREQ-10660",
                    "DOSTUPREQ-18820"
                ],
                "comment": "Разработчики abc хотят уметь дергать ручки своего прода",
                "system": "puncher",
                "added": "2016-09-20T11:17:55.328Z",
                "updated": "2017-08-30T10:00:44.907Z",
                "readonly": False
            },
            {
                "id": "59230b840d0795a886dcec31",
                "request": None,
                "sources": [
                    {
                        "machine_name": "@srv_svc_abc@",
                        "type": "service",
                        "title": {
                            "en": "Service ABC (Каталог сервисов Яндекса)",
                            "ru": "Сервис ABC (Каталог сервисов Яндекса)"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://abc.yandex-team.ru/services/989",
                        "is_user_type": True,
                        "highlight": True
                    }
                ],
                "destinations": [
                    {
                        "machine_name": "_ABC_TEST_NETS_",
                        "type": "macro",
                        "title": {
                            "en": "_ABC_TEST_NETS_",
                            "ru": "_ABC_TEST_NETS_"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://racktables.yandex.net/index.php?page=macros&macro_name=_ABC_TEST_NETS_",
                        "is_user_type": False,
                        "highlight": False
                    },
                    {
                        "machine_name": "_ABC_PROD_NETS_",
                        "type": "macro",
                        "title": {
                            "en": "_ABC_PROD_NETS_",
                            "ru": "_ABC_PROD_NETS_"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://racktables.yandex.net/index.php?page=macros&macro_name=_ABC_PROD_NETS_",
                        "is_user_type": False,
                        "highlight": False
                    }
                ],
                "protocol": "tcp",
                "ports": [
                    "22"
                ],
                "locations": [
                    "office",
                    "vpn"
                ],
                "until": None,
                "status": "active",
                "tasks": [
                    "DOSTUPREQ-16276"
                ],
                "comment": "Доступ по ssh для команды ABC",
                "system": "puncher",
                "added": "2017-05-22T16:02:12.892Z",
                "updated": "2017-05-22T16:02:12.892Z",
                "readonly": False
            },
            {
                "id": "5bc6c4f2d5626d9b782d9d3f",
                "request": None,
                "sources": [
                    {
                        "machine_name": "@srv_svc_abc_administration@",
                        "type": "servicerole",
                        "title": {
                            "en": "Администрирование (ABC (Каталог сервисов Яндекса))",
                            "ru": "Администрирование (ABC (Каталог сервисов Яндекса))"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://abc.yandex-team.ru/services/abc?scope=administration",
                        "is_user_type": True,
                        "highlight": True
                    },
                    {
                        "machine_name": "@srv_svc_abc_projects_management@",
                        "type": "servicerole",
                        "title": {
                            "en": "Управление проектами (ABC (Каталог сервисов Яндекса))",
                            "ru": "Управление проектами (ABC (Каталог сервисов Яндекса))"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://abc.yandex-team.ru/services/abc?scope=projects_management",
                        "is_user_type": True,
                        "highlight": True
                    },
                    {
                        "machine_name": "@srv_svc_abc_services_management@",
                        "type": "servicerole",
                        "title": {
                            "en": "Управление продуктом (ABC (Каталог сервисов Яндекса))",
                            "ru": "Управление продуктом (ABC (Каталог сервисов Яндекса))"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://abc.yandex-team.ru/services/abc?scope=services_management",
                        "is_user_type": True,
                        "highlight": True
                    },
                    {
                        "machine_name": "@srv_svc_abc_development@",
                        "type": "servicerole",
                        "title": {
                            "en": "Разработка (ABC (Каталог сервисов Яндекса))",
                            "ru": "Разработка (ABC (Каталог сервисов Яндекса))"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://abc.yandex-team.ru/services/abc?scope=development",
                        "is_user_type": True,
                        "highlight": True
                    },
                    {
                        "machine_name": "@srv_svc_d_development@",
                        "type": "servicerole",
                        "title": {
                            "en": "Разработка (Сервис D)",
                            "ru": "Разработка (Сервис D)"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://abc.yandex-team.ru/services/abc?scope=development",
                        "is_user_type": True,
                        "highlight": True
                    }
                ],
                "destinations": [
                    {
                        "machine_name": "_TOOLS_KIBANA_PROD_NETS_",
                        "type": "macro",
                        "title": {
                            "en": "_TOOLS_KIBANA_PROD_NETS_",
                            "ru": "_TOOLS_KIBANA_PROD_NETS_"
                        },
                        "is_deleted": False,
                        "external": False,
                        "url": "https://racktables.yandex.net/index.php?page=macros&macro_name=_TOOLS_KIBANA_PROD_NETS_",
                        "is_user_type": False,
                        "highlight": False
                    }
                ],
                "protocol": "tcp",
                "ports": [
                    "22"
                ],
                "locations": [
                    "office",
                    "vpn"
                ],
                "until": None,
                "status": "active",
                "tasks": [
                    "DOSTUPREQ-55926"
                ],
                "comment": "ssh",
                "system": "puncher",
                "added": "2018-10-17T05:13:22.649Z",
                "updated": "2018-10-17T05:13:22.649Z",
                "readonly": False
            },
        ],
        "search": {
            "service_id": 989
        },
        "status": "success"
    }


@pytest.mark.parametrize('staff_role', ['own_only_viewer', 'full_access', 'services_viewer'])
def test_get_services_new_roles(client, service_member, staff_role, staff_factory):
    def permission_keys(permissions):
        return (k for p in permissions for k in ServicesView.PERMISSION_CODENAME_MAPPER.get(p) or ())

    all_keys = set(permission_keys(settings.ABC_INTERNAL_ROLES_PERMISSIONS[staff_role]))
    excluded_keys = {'empty'}
    optional_keys = {
        'own_only_viewer':  {
            'level', 'matched', 'human_readonly_state',
            'use_for_hardware', 'use_for_hr', 'use_for_procurement', 'use_for_revenue',
            'use_for_group_only', 'flags', 'functions',
        },
        'services_viewer':  {
            'level', 'matched', 'human_readonly_state',
            'use_for_hardware', 'use_for_hr', 'use_for_procurement', 'use_for_revenue',
            'use_for_group_only', 'flags', 'functions',
        },
        'full_access':      {
            'level', 'matched', 'issue', 'human_readonly_state', 'oebs_agreement',
            'use_for_hardware', 'use_for_hr', 'use_for_procurement', 'use_for_revenue',
            'actions', 'available_states', 'is_important', 'use_for_group_only', 'flags',
            'functions',
        },
    }[staff_role]
    keys = all_keys - excluded_keys

    staff = staff_factory(staff_role)
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    factories.ServiceMemberFactory(staff=staff, service=service, role=role)
    client.login(staff.login)

    response = client.json.get(reverse('services-api:service-list'))
    result_keys = set(response.data['results'][0].keys())
    assert keys == result_keys | optional_keys

    response = client.json.get(reverse('services-api:service-detail', args=(service.id,)))
    result_keys = set(response.data)
    assert keys == result_keys | optional_keys


def test_parent_filter(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    A, B, C, F = Service.objects.filter(slug__in=['A', 'B', 'C', 'F']).order_by('slug').values_list('id', flat=True)
    response = client.json.get(reverse('api-frontend:service-list'), {'parents': A})
    assert {service['slug'] for service in response.json()['results']} == {'C'}
    response = client.json.get(
        reverse('api-frontend:service-list'),
        {'parents': '%s,%s' % (A, B)},
    )
    assert {service['slug'] for service in response.json()['results']} == {'C', 'F'}
    response = client.json.get(
        reverse('api-frontend:service-list'),
        {'parents': '%s,%s,0' % (A, B)},
    )
    assert {service['slug'] for service in response.json()['results']} == {'A', 'B', 'C', 'F', 'H'}
    response = client.json.get(
        reverse('api-frontend:service-list'),
        {'parents': '0'},
    )
    assert {service['slug'] for service in response.json()['results']} == {'A', 'B', 'H'}
    response = client.json.get(reverse('api-frontend:service-list'))
    all_slugs = Service.objects.values_list('slug', flat=True)
    assert {service['slug'] for service in response.json()['results']} == set(all_slugs)


def test_parent_invalid_param(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    response = client.json.get(reverse('api-frontend:service-list'), {'parents': 'asdf'})
    assert response.status_code == 400


def test_default_sort(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    response = client.json.get(reverse('api-frontend:service-list'))
    slugs = [service['slug'] for service in response.json()['results']]
    assert slugs == list(Service.objects.order_by('level', 'pk').values_list('slug', flat=True))


def test_level(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    response = client.json.get(reverse('api-frontend:service-list'), {'slug': 'A'})
    data = response.json()['results'][0]
    assert data['level'] == Service.objects.get(slug='A').level
    assert data['membership_inheritance'] is False


def test_not_deep_filter_mode(client, services_tree):
    response = client.json.get(reverse('api-frontend:service-list'))
    assert response.json()['filter_mode'] == SHALLOW_FILTER_MODE_LABEL


@pytest.mark.parametrize('with_parent_arg', [False, True])
@pytest.mark.parametrize('state', [
    SERVICEMEMBER_STATE.ACTIVE,
    SERVICEMEMBER_STATE.DEPRIVED,
    SERVICEMEMBER_STATE.REQUESTED,
    SERVICEMEMBER_STATE.DEPRIVING,
])
def test_deep_filter_mode(client, services_tree, with_parent_arg, state):
    staff = factories.StaffFactory()
    target_services = {'G', 'B', 'E'}
    for slug in target_services:
        factories.ServiceMemberFactory(
            service=Service.objects.get(slug=slug), staff=staff,
            state=state,
        )
    if with_parent_arg:
        args = {'member': staff.login, 'parents': '0'}
    else:
        args = {'member': staff.login}
    response = client.json.get(reverse('api-frontend:service-list'), args)
    found_service = {service['slug'] for service in response.json()['results']}
    if state in (SERVICEMEMBER_STATE.DEPRIVED, SERVICEMEMBER_STATE.REQUESTED):
        assert not found_service
    else:
        assert all(service in found_service for service in target_services)
        assert response.json()['filter_mode'] == DEEP_FILTER_MODE_LABEL


def test_root_param(client, services_tree):
    factories.ServiceFactory(parent=Service.objects.get(slug='D'), slug='X')
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    root_service = Service.objects.get(slug='C')
    response = client.json.get(reverse('api-frontend:service-list'), {'root': root_service.id})
    results = response.json()['results']
    services = {service['slug']: service for service in results}
    assert set(services.keys()) == {'D', 'E', 'X'}
    assert services['D']['level'] == 0
    assert services['E']['level'] == 0
    assert services['X']['level'] == 1


def test_root_param_with_deep_mode(client, services_tree):
    factories.ServiceFactory(parent=Service.objects.get(slug='D'), slug='X')
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    member_staff = factories.StaffFactory()
    for slug in ['F', 'E', 'X']:
        factories.ServiceMemberFactory(
            staff=member_staff,
            service=Service.objects.get(slug=slug),
        )
    root_service = Service.objects.get(slug='C')
    params = {'root': root_service.id, 'parents': root_service.id, 'member': member_staff.login}
    response = client.json.get(reverse('api-frontend:service-list'), params)
    results = response.json()['results']
    services = {service['slug']: (service['level'], service['matched']) for service in results}
    assert services == {
        'D': (0, False),
        'E': (0, True),
        'X': (1, True),
    }


def test_root_param_zero(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    services_levels = {service.slug: service.level for service in Service.objects.all()}
    response = client.json.get(reverse('api-frontend:service-list'), {'root': 0})
    results = response.json()['results']
    services = {service['slug']: service for service in results}
    assert set(services.keys()) == {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'}
    for slug in services.keys():
        assert services[slug]['level'] == services_levels[slug]


def test_matched_param_tight(client, services_tree):
    """
    Проверяем узкий фильтр.
    Постепенно позапросно раскрываем дерево.
    Первоначально - возвращаем все корни, где встречаются сервисы, подходящие по условиям с признаком matched=False
    Второй запрос: поскольку сервис C -дочерний сервиса A, ожидаем оба.
    Третий запрос: E является дочерним к С. Вовзращаем два сервиса с matched False.
    """

    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    response = client.json.get(reverse('api-frontend:service-list'), {'slug': 'C', 'parents': '0'})
    results = response.json()['results']
    services = {service['slug']: service for service in results}
    assert set(services.keys()) == {'A'}
    assert not services['A']['matched']

    A = Service.objects.get(slug='A').id
    response = client.json.get(reverse('api-frontend:service-list'), {'slug': 'C', 'parents': '0,%s' % A})
    results = response.json()['results']
    services = {service['slug']: service for service in results}
    assert set(services.keys()) == {'A', 'C'}
    assert not services['A']['matched']
    assert services['C']['matched']

    response = client.json.get(reverse('api-frontend:service-list'), {'slug': 'E', 'parents': '0,%s' % A})
    results = response.json()['results']
    services = {service['slug']: service for service in results}
    assert set(services.keys()) == {'A', 'C'}
    assert not services['A']['matched']
    assert not services['C']['matched']


def test_matched_param_shallow_with_tags(client, services_tree):
    """
    Проверяем узкий фильтр.
    Фильтруемся по тегу. В ручке передаём тег и параметр parents.
    Этот parents сам не имеет такого тега, но его вложенный - имеет.
    Должны вернуть четыре сервиса:
        * корневой B, имеющий этот тег,
        * корневой A, не имеющий этот тег,
        * сервис С - прямой потомок сервиса A, имеет этот тег,
        * сервис H, тк у него есть потомок, имеющий этот тег.
    У сервиса I тоже установлен этот тег, но его родительский не передан в parents,
    поэтому мы не должны видить его в результатах.
    """

    service_a = Service.objects.get(slug='A')
    service_b = Service.objects.get(slug='B')
    service_c = Service.objects.get(slug='C')
    service_h = Service.objects.get(slug='H')
    service_i = factories.ServiceFactory(slug='I', parent=service_h)

    tag = factories.ServiceTagFactory()
    service_b.tags.add(tag)
    service_c.tags.add(tag)
    service_i.tags.add(tag)

    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    response = client.json.get(
        reverse('api-frontend:service-list'),
        {'parents': '0,%s' % service_a.id, 'tags': tag.id}
    )
    results = response.json()['results']

    assert len(results) == 4
    assert set(service['slug'] for service in results) == set([service_a.slug, service_b.slug, service_c.slug, service_h.slug])


def test_matched_param_shallow_with_two_tags_with_two_parents(client, services_tree):
    """
    Проверяем узкий фильтр c двумя значениями в parents.
    Фильтруемся по тегу. В ручке передаём тег и параметр parents.
    Этот parents сам не имеет такого тега, но его вложенный - имеет.
    Должны вернуть четыре сервиса:
        * корневой B, имеющий этот тег,
        * корневой A, не имеющий этот тег, передан в parents,
        * сервис С, дочерний для A, не имеет этот тег, передан в parents,
        * сервис E, дочерний для С, имеет тег.
    """

    service_a = Service.objects.get(slug='A')
    service_b = Service.objects.get(slug='B')
    service_c = Service.objects.get(slug='C')
    service_e = Service.objects.get(slug='E')

    service_c.state = Service.states.CLOSED
    service_c.save()

    service_e.state = Service.states.CLOSED
    service_e.save()

    tag_1 = factories.ServiceTagFactory()
    tag_2 = factories.ServiceTagFactory()
    service_b.tags.add(tag_2)
    service_e.tags.add(tag_1)

    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)

    response = client.json.get(
        reverse('api-frontend:service-list'),
        {
            'parents': '0,%s,%s' % (service_a.id, service_c.id),
            'tags__id__in': '%s,%s' % (tag_1.id, tag_2.id),
            'state__in': '%s,%s' % (Service.states.IN_DEVELOP, Service.states.CLOSED)
        }
    )
    results = response.json()['results']

    assert len(results) == 4
    assert set(service['slug'] for service in results) == {
        service_a.slug, service_b.slug, service_c.slug, service_e.slug
    }


def test_matched_param_deep(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    staff_member = factories.ServiceMemberFactory(service=Service.objects.get(slug='E')).staff
    client.login(staff.login)

    response = client.json.get(
        reverse('api-frontend:service-list'),
        {'member': staff_member.login, 'parents': '0'}
    )
    results = response.json()['results']
    services = {service['slug']: service for service in results}
    assert set(services.keys()) == {'A', 'C', 'E'}
    assert not services['A']['matched']
    assert not services['C']['matched']
    assert services['E']['matched']


def test_traffic_lights(client, services_tree):
    issue_group_1 = IssueGroup.objects.create(name='Имя', name_en='Name', code='old')
    issue_group_2 = IssueGroup.objects.create(name='Имя', name_en='Name', code='new')
    issue_group_3 = IssueGroup.objects.create(name='Имя', name_en='Name', code='other')
    service_1 = Service.objects.get(slug='A')
    service_2 = Service.objects.get(slug='C')
    for service in [service_1, service_2]:
        factories.ServiceTrafficStatusFactory(issue_group=issue_group_1, service=service, issue_count=5, level='ok')
        factories.ServiceTrafficStatusFactory(issue_group=issue_group_2, service=service, issue_count=5, level='ok')
    factories.ServiceTrafficStatusFactory(issue_group=issue_group_3, service=service_2, issue_count=5, level='ok')

    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    response = client.json.get(reverse('api-frontend:service-list'))
    results = response.json()['results']
    services = {service['slug']: service for service in results}
    assert services['A']['traffic_light'][0]['group']['code'] == issue_group_2.code
    assert services['A']['traffic_light'][1]['group']['code'] == issue_group_1.code

    assert services['C']['traffic_light'][0]['group']['code'] == issue_group_2.code
    assert services['C']['traffic_light'][1]['group']['code'] == issue_group_1.code
    assert services['C']['traffic_light'][2]['group']['code'] == issue_group_3.code


def test_tags_and_owner(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    tag1 = factories.ServiceTagFactory()
    tag2 = factories.ServiceTagFactory()
    service = Service.objects.get(slug='C')
    service.tags.add(tag1)
    service.tags.add(tag2)
    response = client.json.get(reverse('api-frontend:service-list'))
    results = response.json()['results']
    services = {service['slug']: service for service in results}
    assert all(service['tags'] == [] for service in services.values() if service['slug'] != 'C')
    assert {tag['id'] for tag in services['C']['tags']} == {tag1.id, tag2.id}
    c_service = Service.objects.select_related('owner').get(slug='C')
    assert services['C']['owner'] == {
        'login': c_service.owner.login,
        'name': {
            'ru': '%s %s' % (c_service.owner.first_name, c_service.owner.last_name),
            'en': '%s %s' % (c_service.owner.first_name_en, c_service.owner.last_name_en),
        },
    }


def test_slug(client, services_tree):
    Service.objects.filter(slug='A').update(is_exportable=True)
    Service.objects.filter(slug='B').update(is_exportable=False)
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    response = client.json.get(reverse('api-frontend:service-list'), {'slug__in': 'A,B'})
    assert {service['slug'] for service in response.json()['results']} == {'A', 'B'}


def test_get_detail(client, services_tree):
    """
    GET api/frontend/services/<service_id>/
    """
    service = Service.objects.select_related('owner').last()
    service.readonly_state = 'moving'
    service.functions = ['staff_smth', 'duty']
    service.save(update_fields=('readonly_state', 'functions', ))

    client.login(service.owner.login)
    url = reverse('api-frontend:service-detail', args=(service.id,))

    response = client.json.get(url)
    assert response.status_code == 200
    data = response.json()
    assert data['slug'] == service.slug
    assert data['functions'] == ['staff_smth', 'duty']
    assert data['human_readonly_state'] == {
        'id': 'moving',
        'name': 'Перемещается',
        'name_i18n': {
            'ru': 'Перемещается',
            'en': 'Is moved',
        }
    }
    assert data['use_for_hr'] is False
    assert data['actions'] == [
        {'edit_state': {'verb': 'Изменить статус'}},
        {'edit_name': {'verb': 'Изменить название'}},
        {'contact_add': {'verb': 'Привязать подразделение'}},
        {'chown': {'verb': 'Изменить руководителя'}},
        {'chown_cancel': {'verb': 'Отменить изменение руководителя'}},
        {'contacts_replace': {'verb': 'Сохранить новый список контактов'}},
        {'contact_remove': {'verb': 'Удалить контакт'}},
        {'contact_edit': {'verb': 'Отредактировать контакт'}},
        {'request_resource': {'verb': 'Запросить ресурс'}}
    ]

    assert data['available_states'] == [
        {'id': 'supported', 'verbose': 'Поддерживается'},
        {'id': 'needinfo', 'verbose': 'Требуется информация'},
        {'id': 'closed', 'verbose': 'Закрыт'},
    ]

    assert data['is_important'] is False


def test_filter_by_parent_and_root_zero(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    expected_service = Service.objects.get(slug='C')
    A, C = Service.objects.filter(slug__in=['A', 'C']).order_by('slug').values_list('id', flat=True)
    response = client.json.get(reverse('api-frontend:service-list'), {'parents': A, 'root': 0})
    assert response.status_code == 200
    data = response.json()
    assert len(data['results']) == 1
    assert data['results'][0]['slug'] == expected_service.slug
    assert data['results'][0]['type']['code'] == expected_service.service_type.code


def test_search(client, services_tree):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    factories.ServiceFactory(slug='AB', parent=Service.objects.get(slug='H'))
    response = client.json.get(reverse('api-frontend:service-list'), {'search': 'A'})
    services = response.json()['results']
    found = {(service['slug'], service['matched']) for service in services}
    assert found == {('A', True), ('H', False), ('AB', True)}


@pytest.mark.parametrize('state', [
    SERVICEMEMBER_STATE.ACTIVE,
    SERVICEMEMBER_STATE.DEPRIVED,
    SERVICEMEMBER_STATE.REQUESTED,
    SERVICEMEMBER_STATE.DEPRIVING,
])
def test_department(client, services_tree, state):
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    staff = factories.StaffFactory()
    factories.ServiceMemberFactory(service=Service.objects.get(slug='C'), staff=staff, state=state)
    factories.ServiceMemberFactory(service=Service.objects.get(slug='H'), staff=staff, state=state)
    response = client.json.get(reverse('api-frontend:service-list'), {'department': staff.department.id})
    services = response.json()['results']
    found = {(service['slug'], service['matched']) for service in services}
    if state in (SERVICEMEMBER_STATE.DEPRIVED, SERVICEMEMBER_STATE.REQUESTED):
        assert not found
    else:
        assert found == {('A', False), ('H', True), ('C', True)}


def test_filter_by_owner(client, owner_role):
    test_owner = factories.StaffFactory()
    other_owner = factories.StaffFactory()

    valid_services = {factories.ServiceFactory(owner=test_owner).id for _ in range(10)}
    factories.ServiceFactory.create_batch(10, owner=other_owner)

    response = client.json.get(reverse('api-frontend:service-list'), {'owner': test_owner.login, 'fields': 'id'})
    assert response.status_code == 200
    data = response.json()

    received_services = set(service['id'] for service in data['results'])
    assert response.json()['filter_mode'] == DEEP_FILTER_MODE_LABEL
    assert len(received_services) == 10
    assert received_services == valid_services


@pytest.mark.parametrize('parents_param', [0, 'G', 'A'])
@pytest.mark.parametrize('shallow_param', ['slug', None])
def test_too_many_services(client, services_tree, parents_param, shallow_param):
    big_parent = Service.objects.get(slug='G')
    for _ in range(20):
        factories.ServiceFactory(parent=big_parent)
    staff = Service.objects.select_related('owner').get(slug='A').owner
    client.login(staff.login)
    if parents_param:
        parents_param = Service.objects.get(slug=parents_param).id
    params = {'parents': parents_param}
    if shallow_param:
        params[shallow_param] = 'C'
    with patch('plan.services.api.frontend.FrontendServicesView.max_service_count') as max_count:
        max_count.return_value = 10
        response = client.json.get(reverse('api-frontend:service-list'), params)
    if parents_param == big_parent.id and not shallow_param:
        assert response.status_code == 400
    else:
        assert response.status_code == 200


def test_get_detail_by_slug(client, services_tree):
    """
    GET api/frontend/services/<service_id>/
    """
    service = Service.objects.select_related('owner').last()
    client.login(service.owner.login)
    url = reverse('api-frontend:service-detail', args=(service.slug,))

    response = client.json.get(url)
    assert response.status_code == 200
    data = response.json()
    assert data['slug'] == service.slug
    assert data['id'] == service.id


@pytest.mark.parametrize('user_lang', [LANG.RU, LANG.EN, '', LANG.TR])
@pytest.mark.parametrize('header_mode', [True, False])
def test_language_sort(client_without_accept_language, services_tree, user_lang, header_mode):
    Service.objects.filter(slug='A').update(name_en='A', name='Я')
    Service.objects.filter(slug='B').update(name_en='b', name='ю')
    Service.objects.filter(slug='H').update(name_en='H', name='Э')

    staff = Service.objects.select_related('owner').get(slug='A').owner
    if not header_mode:
        staff.lang_ui = user_lang
        staff.save()
    client_without_accept_language.login(staff.login)
    if header_mode:
        response = client_without_accept_language.json.get(
            reverse('api-frontend:service-list'),
            {'parents': '0'},
            HTTP_ACCEPT_LANGUAGE=user_lang,
        )
    else:
        response = client_without_accept_language.json.get(reverse('api-frontend:service-list'), {'parents': '0'})
    services = [service['slug'] for service in response.json()['results']]
    if user_lang == LANG.RU:
        assert services == ['H', 'B', 'A']
    else:
        assert services == ['A', 'B', 'H']


@pytest.mark.parametrize('staff_role', ['full_access', 'own_only_viewer', 'services_viewer'])
def test_can_filter_permission(client, services_tree, staff_factory, staff_role):
    # Проверим, что без пермишена can_filter (роль own_only_viewer) пользователь не может использовать фильтры
    # в ответе всегда будут только те сервисы, где он участник или их предки
    service = Service.objects.get(slug='C')
    staff = staff_factory(staff_role)
    factories.ServiceMemberFactory(staff=staff, service=service)
    filter_params = {'slug__in': 'G,B,F'}
    url = reverse('api-frontend:service-list')
    client.login(staff.login)
    response = client.get(url, data=filter_params)
    assert response.status_code == 200
    data = response.json()['results']
    if staff_role == 'own_only_viewer':
        obj_count = 2
        services_ids = {'A', 'C'}
    else:
        obj_count = 3
        services_ids = {'B', 'F', 'G'}
    assert len(data) == obj_count
    assert {service['slug'] for service in data} == services_ids


@pytest.mark.parametrize('staff_role', ['services_viewer', 'own_only_viewer', 'full_access'])
def test_filter_restriction(client, services_tree, staff_factory, staff_role):
    service = Service.objects.get(slug='C')
    staff = staff_factory(staff_role)
    factories.ServiceMemberFactory(staff=staff, service=service)
    url = reverse('api-frontend:service-list')
    client.login(staff.login)

    def results(response) -> dict:
        return response.json()['results']

    full_access_only_field = 'is_suspicious'
    filtered = results(client.get(url, data={full_access_only_field: 'True'}))
    inverse_filtered = results(client.get(url, data={full_access_only_field: 'False'}))

    if staff_role != 'full_access':
        assert filtered == inverse_filtered
    else:
        assert filtered != inverse_filtered


def test_create_service_name(client, data):
    client.login(data.staff.login)

    response = client.json.post(
        reverse('api-frontend:service-list'),
        {
            'name': 'Котики, вперёд!',
            'slug': 'cats',
            'owner': data.staff.login,
            'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
        }
    )
    assert response.status_code == 400
    assert response.json()['error']['extra'] == {
        'name': ['Expected a dictionary of items but got type "str".'],
    }

    response = client.json.post(
        reverse('api-frontend:service-list'),
        {
            'name': {'ru': 'Котики, вперёд!', },
            'slug': 'cats',
            'owner': data.staff.login,
            'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
        }
    )
    assert response.status_code == 400
    assert response.json()['error']['extra'] == {
        'name': ['This field is required.'],
    }

    with patch('plan.services.tasks.register_service') as register_service:
        response = client.json.post(
            reverse('api-frontend:service-list'),
            {
                'name': {'ru': 'Котики, вперёд!', 'en': 'Go-go, kitties!'},
                'slug': 'cats',
                'owner': data.staff.login,
                'description': {'ru': 'Теперь понятно, летим обратно!', 'en': 'Service description'},
            }
        )
    assert response.status_code == 201
    assert register_service.apply_async.called

    service = Service.objects.get(slug='cats')
    service.fetch_owner()

    assert service.membership_inheritance is False
    assert service.parent == data.meta_other
    assert service.name == 'Котики, вперёд!'
    assert service.name_en == 'Go-go, kitties!'
    assert service.state == Service.states.IN_DEVELOP
    assert service.owner == data.staff
    assert ServiceCreateRequest.objects.filter(service=service).exists()


def test_create_service_description(client, data):
    client.login(data.staff.login)

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse('api-frontend:service-list'),
            {
                'name': {'ru': 'Котики, вперёд!', 'en': 'Cats'},
                'slug': 'cats',
                'owner': data.staff.login,
                'description': {'ru': 'Описание сервиса!'},
            }
        )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == {'description': ['This field is required.']}

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse('api-frontend:service-list'),
            {
                'name': {'ru': 'Котики, вперёд!', 'en': 'Cats'},
                'slug': 'cats',
                'owner': data.staff.login,
                'description': {'ru': '', 'en': ''},
            }
        )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == {'description': ['This field is required.']}

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse('api-frontend:service-list'),
            {
                'name': {'ru': 'Котики, вперёд!', 'en': 'Go-go, kitties!'},
                'slug': 'cats',
                'owner': data.staff.login,
                'description': {'ru': 'Теперь понятно, летим обратно!', 'en': 'Service description'},
            }
        )

    assert response.status_code == 201

    service = Service.objects.get(slug='cats')
    assert service.description == 'Теперь понятно, летим обратно!'
    assert service.description_en == 'Service description'


@pytest.mark.parametrize('agreement_exists', (False, True))
def test_service_oebs_agreement(client, staff_factory, agreement_exists):
    staff = staff_factory('full_access')
    client.login(staff.login)

    service = factories.ServiceFactory()

    if agreement_exists:
        factories.OEBSAgreementFactory(service=service, state=STATES.APPLIED)
        new_agreement = factories.OEBSAgreementFactory(service=service)

    response = client.get(reverse('api-frontend:service-detail', args=[service.id]))
    assert response.status_code == 200
    result = response.json()

    if agreement_exists:
        assert result['oebs_agreement']['id'] == new_agreement.id
    else:
        assert result['oebs_agreement'] is None


def test_scope_count_api(client, data):
    scope = factories.RoleScopeFactory()
    scope_1 = factories.RoleScopeFactory()
    role = factories.RoleFactory(scope=scope)
    role_1 = factories.RoleFactory(scope=scope)
    role_2 = factories.RoleFactory(scope=scope_1)

    for _ in range(5):
        factories.ServiceMemberFactory(
            service=data.service,
            role=role,
            staff=data.staff,
        )
        factories.ServiceMemberFactory(
            service=data.service,
            role=role_2,
            staff=data.staff,
        )
    for _ in range(7):
        factories.ServiceMemberFactory(
            service=data.service,
            role=role_1,
            staff=data.staff,
        )

    response = client.json.get(
        reverse('api-frontend:scope-counter-list'),
        {'service_id': data.service.id, 'min_count': 5}
    )

    assert response.status_code == 200
    result = response.json()['results']
    assert len(result) == 2
    expected = {
        (scope.slug, 12),
        (scope_1.slug, 5),
    }
    assert {(item['scope_slug'], item['members_count']) for item in result} == expected


def test_external_members_count(client, data, staff_factory):
    staff_external = staff_factory('full_access', is_robot=False, affiliation='external')
    factories.ServiceMemberFactory(staff=staff_external, service=data.service)
    update_denormalized_field('services.Service', data.service.id, 'unique_immediate_external_members_count')

    response = client.json.get(reverse('api-frontend:service-detail', args=(data.service.id, )))
    assert response.status_code == 200
    result = response.json()
    assert result['unique_immediate_external_members_count'] == 1

    response = client.json.get(
        reverse('api-frontend:service-list'), {'id': data.service.id}
    )
    assert response.status_code == 200
    result = response.json()['results']
    assert result[0]['unique_immediate_external_members_count'] == 1


def test_puncher_rules(client, puncher_response):
    service = factories.ServiceFactory(slug='abc')

    expected = [
        {'comment': 'Проверка корректности работы бекенда ABC',
         'created': '2016-09-06T09:25:04.35Z',
         'destination': '_C_CS_TOOLSBACK_',
         'destination_link': 'https://racktables.yandex.net/index.php?page=macros&macro_name=_C_CS_TOOLSBACK_',
         'link': 'https://puncher.yandex-team.ru/?id=57ce8b70d5626dfa1415b877',
         'locations': ['office', 'vpn'],
         'ports': ['443'],
         'scope': 'development',
         'until': '2022-09-06T09:25:04.35Z'},
        {'comment': 'Разработчики abc хотят уметь дергать ручки своего прода',
         'created': '2016-09-20T11:17:55.328Z',
         'destination': 'vs-planner-back.http.yandex.net',
         'destination_link': None,
         'link': 'https://puncher.yandex-team.ru/?id=57e11ae30d0795cb1f48d96b',
         'locations': ['office', 'vpn'],
         'ports': ['443'],
         'scope': 'development',
         'until': None},
        {'comment': 'Доступ по ssh для команды ABC',
         'created': '2017-05-22T16:02:12.892Z',
         'destination': '_ABC_TEST_NETS_',
         'destination_link': 'https://racktables.yandex.net/index.php?page=macros&macro_name=_ABC_TEST_NETS_',
         'link': 'https://puncher.yandex-team.ru/?id=59230b840d0795a886dcec31',
         'locations': ['office', 'vpn'],
         'ports': ['22'],
         'scope': None,
         'until': None},
        {'comment': 'Доступ по ssh для команды ABC',
         'created': '2017-05-22T16:02:12.892Z',
         'destination': '_ABC_PROD_NETS_',
         'destination_link': 'https://racktables.yandex.net/index.php?page=macros&macro_name=_ABC_PROD_NETS_',
         'link': 'https://puncher.yandex-team.ru/?id=59230b840d0795a886dcec31',
         'locations': ['office', 'vpn'],
         'ports': ['22'],
         'scope': None,
         'until': None},
        {'comment': 'ssh',
         'created': '2018-10-17T05:13:22.649Z',
         'destination': '_TOOLS_KIBANA_PROD_NETS_',
         'destination_link': 'https://racktables.yandex.net/index.php?page=macros&macro_name=_TOOLS_KIBANA_PROD_NETS_',
         'link': 'https://puncher.yandex-team.ru/?id=5bc6c4f2d5626d9b782d9d3f',
         'locations': ['office', 'vpn'],
         'ports': ['22'],
         'scope': 'administration',
         'until': None},
        {'comment': 'ssh',
         'created': '2018-10-17T05:13:22.649Z',
         'destination': '_TOOLS_KIBANA_PROD_NETS_',
         'destination_link': 'https://racktables.yandex.net/index.php?page=macros&macro_name=_TOOLS_KIBANA_PROD_NETS_',
         'link': 'https://puncher.yandex-team.ru/?id=5bc6c4f2d5626d9b782d9d3f',
         'locations': ['office', 'vpn'],
         'ports': ['22'],
         'scope': 'projects_management',
         'until': None},
        {'comment': 'ssh',
         'created': '2018-10-17T05:13:22.649Z',
         'destination': '_TOOLS_KIBANA_PROD_NETS_',
         'destination_link': 'https://racktables.yandex.net/index.php?page=macros&macro_name=_TOOLS_KIBANA_PROD_NETS_',
         'link': 'https://puncher.yandex-team.ru/?id=5bc6c4f2d5626d9b782d9d3f',
         'locations': ['office', 'vpn'],
         'ports': ['22'],
         'scope': 'services_management',
         'until': None},
        {'comment': 'ssh',
         'created': '2018-10-17T05:13:22.649Z',
         'destination': '_TOOLS_KIBANA_PROD_NETS_',
         'destination_link': 'https://racktables.yandex.net/index.php?page=macros&macro_name=_TOOLS_KIBANA_PROD_NETS_',
         'link': 'https://puncher.yandex-team.ru/?id=5bc6c4f2d5626d9b782d9d3f',
         'locations': ['office', 'vpn'],
         'ports': ['22'],
         'scope': 'development',
         'until': None
         }
    ]

    with patch.object(PuncherClient, '_make_response') as mock_get_rules:
        mock_get_rules.return_value = puncher_response
        response = client.json.get(
            reverse('api-frontend:puncher-rules-list'),
            {'service_id': service.id, }
        )

    assert response.status_code == 200
    mock_get_rules.assert_called_once_with(
        'https://api.puncher.yandex-team.ru/api/dynfw/rules',
        {'service_id': str(service.id), 'rules': 'exclude_rejected', }
    )
    result = response.json()
    assert result['count'] == 24
    assert result['next'] == f'http://testserver/api/frontend/access/puncher/?service_id={service.id}&cursor_id=smth'
    assert result['results'] == expected


def test_idm_roles(client, idm_response, staff_factory):
    staff = staff_factory()
    service = factories.ServiceFactory(slug='abc')

    client.login(staff.login)

    expected = [
        {'created': '2022-03-15T18:26:20.050597+00:00',
         'fields_data': {'project': 'abc-production'},
         'granted': '2022-03-15T18:26:24.554950+00:00',
         'human_state': 'Выдана',
         'link': 'https://idm.test.yandex-team.ru/group/114362/roles#f-status=all,sort-by=-updated,role=64288839',
         'role': 'Роль в проекте / Администратор проекта',
         'scope': 'svc_abc_devops',
         'state': 'granted',
         'system': 'Solomon(prestable)',
         'system_slug': 'solomon_prestable',
         'until': None,
         'updated': '2022-03-15T18:26:24.554950+00:00',
         'with_external': True,
         'with_inheritance': True,
         'with_robots': True},
        {'created': '2019-02-22T12:50:16.443588+00:00',
         'fields_data': None,
         'granted': '2021-05-07T18:09:30.238041+00:00',
         'human_state': 'Выдана',
         'link': 'https://idm.test.yandex-team.ru/group/42868/roles#f-status=all,sort-by=-updated,role=7395835',
         'role': 'Отчёты / ExtData/ABC/qloud_applications: Внешние данные / ABC / Qloud-приложения / На редактирование',
         'scope': None,
         'state': 'granted',
         'system': 'Статистика',
         'system_slug': 'stat',
         'until': None,
         'updated': '2021-05-07T18:09:30.238041+00:00',
         'with_external': True,
         'with_inheritance': True,
         'with_robots': True},
        {'created': '2019-02-22T12:50:06.288319+00:00',
         'fields_data': None,
         'granted': '2021-05-07T18:09:29.172223+00:00',
         'human_state': 'Выдана',
         'link': 'https://idm.test.yandex-team.ru/group/42868/roles#f-status=all,sort-by=-updated,role=7395806',
         'role': 'Отчёты / ExtData/ABC/nanny: Внешние данные / ABC / Nanny-сервисы / На редактирование',
         'scope': None,
         'state': 'granted',
         'system': 'Статистика',
         'system_slug': 'stat',
         'until': None,
         'updated': '2021-05-07T18:09:29.172223+00:00',
         'with_external': True,
         'with_inheritance': True,
         'with_robots': True
         }
    ]

    with patch('plan.idm.manager.Manager.get') as get_roles:
        get_roles.return_value = idm_response
        response = client.json.get(
            reverse('api-frontend:idm-roles-list'),
            {'service_id': service.id, }
        )

    assert response.status_code == 200
    get_roles.assert_called_once_with(
        'roles/',
        params={
            'abc_slug': service.slug,
            '_requester': staff.login,
            'state': 'granted,onhold,need_request,rerequested,review_request,depriving,depriving_validation,created,imported,requested,approved,sent,awaiting',
            'limit': 20,
            'offset': 20  # тут 20 из-за того что filters изменяемый и меняется уже после вызова в filters['offset'] += filters['limit']
        }
    )
    result = response.json()
    assert result['count'] == 61
    assert result['next'] == f'http://testserver/api/frontend/access/idm/?service_id={service.id}&cursor_id=20'
    assert result['results'] == expected
