# -*- coding: utf-8 -*-
from unittest.mock import patch, Mock

from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    OrganizationServiceModel,
    ServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    RobotServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import get_robot_nickname
from intranet.yandex_directory.src.yandex_directory.core.commands.create_robots import Command as CreateRobotsCommand

from hamcrest import (
    assert_that,
    equal_to,
)


from testutils import (
    assert_not_called,
    TestCase,
    create_organization,
)


class TestCreateRobotsCommand(TestCase):
    def test_should_call_create_robot_task_if_needed(self):
        # проверим, что команда create-robots создаёт роботного пользователя, если это необходимо
        ServiceModel(self.meta_connection).delete(force_remove_all=True)
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=True,
        )
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org'
        )['organization']
        create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-second-org'
        )['organization']

        organization_service_model = OrganizationServiceModel(self.main_connection)

        # "добавим" сервис в одну из организаций без создания робота
        organization_service_model.create(
            org_id=organization['id'],
            service_id=service['id'],
        )

        robot_nickname = get_robot_nickname(service_slug=service['slug'])

        # до вызова команды проверим, что робота нет в организации
        robots_count = RobotServiceModel(self.main_connection).count(filter_data={'service_id': service['id']})
        assert_that(robots_count, equal_to(0))
        users_count = UserModel(self.main_connection).count(filter_data={'nickname': robot_nickname})
        assert_that(users_count, equal_to(0))

        CreateRobotsCommand().try_run()
        self.process_tasks()

        # теперь в организации должен присутствовать роботный пользователь
        robots_count = RobotServiceModel(self.main_connection).count(filter_data={'service_id': service['id']})
        assert_that(robots_count, equal_to(1))
        users_count = UserModel(self.main_connection).count(filter_data={'nickname': robot_nickname})
        assert_that(users_count, equal_to(1))

        # т.к. робот уже создан, таск не должен больше вызываться
        mocked_create_robot_task = Mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.tasks.CreateRobotTask', mocked_create_robot_task):
            CreateRobotsCommand().try_run()

        assert_not_called(mocked_create_robot_task.delay)
