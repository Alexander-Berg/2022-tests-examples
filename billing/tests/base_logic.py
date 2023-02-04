import pytest

from . import service_builder as sb
from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.mapper.exceptions import NotFound

PERMISSIONS = [
    {"id": "service_access-read", "name": "service_access", "action": "read"},
    {"id": "ban_key-perform", "name": "ban_key", "action": "perform"},
    {"id": "service_access-write", "name": "service_access", "action": "write"},
    {"id": "activate_key-perform", "name": "activate_key", "action": "perform"},
    {"id": "service_access-perform", "name": "service_access", "action": "perform"},
    {"id": "balance_tariffs_sync-perform", "name": "balance_tariffs_sync", "action": "perform"},
]
PERMISSION_SETS = [
    {"id": "manager", "filters": [],
     "permissions": ["service_access-read", "ban_key-perform",
                     "service_access-write", "activate_key-perform",
                     "service_access-perform"]},
    {"id": "user", "filters": [], "permissions": ["ban_key-perform"]},
    {"id": "admin", "filters": [], "permissions": ["balance_tariffs_sync-perform"]}
]
ROLES = [
    {"id": "admin",
     "constraints": {"default": {"perform": None, "read": {"service": [5, 10, 6, 4, 11, 7, 8, 9, 3]}, "write": None},
                     "ban_key": {"perform": {"service": "*"}},
                     "service_access": {"perform": "*", "read": {"service": "*"}, "write": {"service": "*"}}},
     "perm_sets": ["manager"], "descr": "Администратор"},
    {"id": "user", "perm_sets": ["user"],
     "constraints": {"default": {"read": {"user": "self"}, "write": {"user": "self"}, "perform": {"user": "self"}}},
     "descr": "Пользователь"}
]
UNITS = [
    {"id": 1, "cc": "hits", "name": "Вызовы API"},
    {"id": 2, "cc": "bytes", "name": "Байты"},
    {"id": 3, "cc": "voice_unit", "name": "Голосовые ед. расп."},
    {"id": 4, "cc": "geocoder_hits", "name": "Вызовы геокодера"},
    {"id": 5, "cc": "router_hits", "name": "Вызовы маршрутизатора"},
    {"id": 6, "cc": "tts_unit", "name": "Голосовые ед. синт."},
    {"id": 7, "cc": "ner_unit", "name": "Голосовые ед. смысл"}
]


@pytest.fixture()
def logic_base(mongomock):

    class LogicBaseTest:

        service_regular: mapper.Service = None
        service_with_questionnaire: mapper.Service = None
        service_with_limits: mapper.Service = None
        manager_role: mapper.Role = None
        internal_user: mapper.User = None
        external_user: mapper.User = None
        not_exists_user_uid = None
        not_exists_unit_cc = None
        not_exists_service_token = None
        key_regular: mapper.Key = None

        @classmethod
        def setUpClass(cls):
            cls.init_db_data()

            service_builder_director = sb.ServiceBuilderDirector(sb.RegularServiceBuilder)
            cls.service_regular = service_builder_director.build()

            service_builder_director = sb.ServiceBuilderDirector(sb.QuestionnaireServiceBuilder)
            cls.service_with_questionnaire = service_builder_director.build()

            service_builder_director = sb.ServiceBuilderDirector(sb.LimitedServiceBuilder)
            cls.service_with_limits = service_builder_director.build()

            cls.internal_user = mapper.User(
                uid=mapper.User.LIMIT_USER_ID_TEAM - 1,
                name='Fake operator TEST',
                email='python_test_operator@devnull.yandex.ru',
                login='python_test_operator'
            )
            test_manager_role_identifier = 'test_bo_manager_role_identifier'
            try:
                mapper.Role.getone(name=test_manager_role_identifier)
            except NotFound:
                pass
            else:
                raise Exception('Ooops! Test BO manager role exists!')
            constraints_map = {"service": [
                cls.service_regular.id,
                cls.service_with_questionnaire.id,
                cls.service_with_limits.id,
            ]}
            cls.manager_role = mapper.Role()
            cls.manager_role.name = test_manager_role_identifier
            cls.manager_role.descr = "Test BO manager role"
            cls.manager_role.perm_sets = [mapper.PermissionSet.getone(name="manager").pk]
            cls.manager_role.constraints = {
                "default": {
                    "perform": None,
                    "read": constraints_map,
                    "write": None,
                },
                "ban_key": {
                    "perform": constraints_map,
                },
                "service_access": {
                    "perform": "*",
                    "read": constraints_map,
                    "write": constraints_map,
                }
            }
            cls.manager_role.save()
            cls.internal_user.roles.append(cls.manager_role.pk)
            cls.internal_user.save()

            test_user_uid = mapper.User.LIMIT_USER_ID_REGULAR - 1
            cls.external_user = mapper.User(
                uid=test_user_uid,
                name='Fake user TEST',
                email='python_test_user@devnull.yandex.ru',
                login='python_test_user'
            )
            cls.remove_user(test_user_uid)
            cls.external_user.save()

            test_user_uid -= 1
            cls.not_exists_user_uid = test_user_uid

            cls.not_exists_unit_cc = 'not_exists_unit_cc_must_be_really_nonexistent'
            try:
                mapper.Unit.getone(name=cls.not_exists_unit_cc)
            except NotFound:
                pass
            else:
                raise Exception('Ooops! Nonexistent unit cc not so nonexistent!')

            cls.not_exists_service_token = 'not_exists_service_token_must_be_really_nonexistent'
            try:
                mapper.Service.getone(token=cls.not_exists_unit_cc)
            except NotFound:
                pass
            else:
                raise Exception('Ooops! Nonexistent service token not so nonexistent!')

            cls.key_regular = mapper.Key.create(cls.external_user.get_default_project())
            cls.key_regular.attach_to_service(cls.service_regular)

        @classmethod
        def init_db_data(cls):
            for permission in PERMISSIONS:
                mapper.Permission(**permission).save()
            for permission_set in PERMISSION_SETS:
                mapper.PermissionSet(**permission_set).save()
            for role in ROLES:
                mapper.Role(**role).save()
            for unit in UNITS:
                mapper.Unit(**unit).save()

        @classmethod
        def tearDownClass(cls):
            cls.internal_user.delete()
            cls.manager_role.delete()
            sb.ServiceBuilderDirector.remove_test_objects()

        @staticmethod
        def delete_key_config(key_config):
            """
            :type key_config: mapper.KeyServiceConfig
            """
            for counter in mapper.KeyServiceCounter.objects.filter(key=key_config.key,
                                                                   service_id=key_config.service_id):
                counter.delete()
            for limit_checker in mapper.LimitChecker.objects.filter(key=key_config.key,
                                                                    service_id=key_config.service_id):
                limit_checker.delete()
            for audit_trail in mapper.AuditTrail.objects.filter(key=key_config.key,
                                                                service_id=key_config.service_id):
                audit_trail.delete()
            key_config.delete()

        @classmethod
        def remove_user(cls, user):
            # cls.purge_user_projects(user)
            try:
                user = user if isinstance(user, mapper.User) else mapper.User.getone(uid=user)
                user.reload()
            except NotFound:
                return
            user.delete()

        @classmethod
        def purge_user_projects(cls, user):
            user_uid = user.uid if isinstance(user, mapper.User) else user
            for project in mapper.Project.objects.filter(user_uid=user_uid):
                cls.delete_project(project)

        @classmethod
        def delete_project(cls, project):
            assert isinstance(project, mapper.Project)
            for link in mapper.ProjectServiceLink.objects.filter(project_id=project.id):
                cls.delete_link(link)
            project.delete()

    LogicBaseTest.setUpClass()
    yield LogicBaseTest
    LogicBaseTest.tearDownClass()
