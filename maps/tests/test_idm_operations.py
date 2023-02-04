from .basic_test import BasicTest

from data_types.personal_role import PersonalRole

import lib.scheduler as scheduler


class TestIdmOperations(BasicTest):
    def test_get_roles_info(self):
        roles_info = scheduler.get_roles_info() >> 200
        assert roles_info == {
            "code": 0,
            "roles": {
                "slug": "maps-auto-head-unit-task-management",
                "name": "управление задачами для головного устройства",
                "values": {
                    "user": {
                        "name": {
                            "ru": "Пользователь таск-менеджера",
                            "en": "Task Manager's user",
                        },
                        "help": {
                            "ru": "Может ставить/отменять/получать задачи для головных устройств",
                            "en": "Has permissions to request/cancel/get result of tasks for head units"
                        }
                    }
                }
            }
        }

    def test_get_all_roles_empty(self):
        all_roles = PersonalRole.roles_from_json(scheduler.get_all_roles() >> 200)
        assert len(all_roles) == 0

    def test_add_role(self):
        role = PersonalRole()
        status = scheduler.add_role(role) >> 200
        assert status['code'] == 0

        all_roles = PersonalRole.roles_from_json(scheduler.get_all_roles() >> 200)
        assert role in all_roles

    def test_add_role_duplicate(self):
        role = PersonalRole()
        scheduler.add_role(role) >> 200

        status = scheduler.add_role(role) >> 200
        assert status['code'] != 0

    def test_remove_role(self):
        role = PersonalRole()
        scheduler.add_role(role) >> 200

        status = scheduler.remove_role(role) >> 200
        assert status['code'] == 0

        all_roles = PersonalRole.roles_from_json(scheduler.get_all_roles() >> 200)
        assert len(all_roles) == 0

    def test_remove_missing_role(self):
        status = scheduler.remove_role(PersonalRole()) >> 200
        assert status['code'] != 0
