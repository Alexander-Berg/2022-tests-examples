# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
)
from intranet.yandex_directory.src.yandex_directory.core.actions.department import (
    on_department_add,
    on_department_modify,
    on_department_delete,
)

from testutils import TestCase
from copy import deepcopy

from intranet.yandex_directory.src.yandex_directory.core.models.event import EventModel
from intranet.yandex_directory.src.yandex_directory.core.events.utils import (
    TYPE_DEPARTMENT,
)


class TestDepartmentEvents(TestCase):
    def setUp(self):
        super(TestDepartmentEvents, self).setUp()

    def test_on_department_add(self):
        department_one = self.create_department()
        department_two = self.create_department(department_one['id'])
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        on_department_add(
            self.main_connection,
            self.organization['id'],
            revision,
            department_two
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'department_added',
            'department_department_added',
            'department_department_added',
            'group_added',  # техническая группа department_head
        ]
        assert_that(events, equal_to(expected))

    def test_on_department_property_changed(self):
        department = self.create_department()
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        old_name = department['name']
        new_department = deepcopy(department)
        new_department['name']['ru'] = 'Новое название'
        content = {
            'before': department,
        }
        on_department_modify(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=new_department,
            object_type=TYPE_DEPARTMENT,
            content=content
        )
        event = EventModel(self.main_connection).find()[0]
        expected = 'department_property_changed'
        expected_diff = {
            'name': [old_name, new_department['name']]
        }
        assert_that(event, has_entries(name=expected,
                                       content=has_entries(diff=expected_diff),
                                       ))

    def test_on_department_moved(self):
        # Перемещаем department_three из department_one в department_two.
        # У department_two есть права на ресурс resource, должны выдаться всем
        # сотрудникам департамента.

        department_one = self.create_department()
        department_two = self.create_department()
        resource = self.create_resource_with_department(
            department_two['id'], 'read')
        department_three = self.create_department(
            parent_id=department_two['id'])
        user = self.create_user(department_three['id'])
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        department_before = deepcopy(department_three)
        department_before['parent_id'] = department_one['id']
        content = {
            'before': department_before,
        }
        on_department_modify(
            self.main_connection,
            self.organization['id'],
            revision,
            department_three,
            TYPE_DEPARTMENT,
            content
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'department_department_added',
            'department_department_deleted',
            'department_moved',
            'resource_grant_changed',
        ]
        assert_that(events, equal_to(expected))

        event = EventModel(self.main_connection).find({'name': 'resource_grant_changed'})[0]
        relations_add = event['content']['relations']['add']
        relations_delete = event['content']['relations']['remove']
        expected_relation = {
            'object_id': department_three['id'],
            'relation_name': 'read',
        }
        assert_that(
            relations_delete,
            has_entries(
                users=[],
                departments=[],
                groups=[],
            )
        )
        assert_that(
            relations_add,
            has_entries(
                users=[],
                departments=[expected_relation],
                groups=[],
            )
        )
        assert_that(
            event,
            has_entries(
                name='resource_grant_changed',
                org_id=self.organization['id'],
                revision=revision,
                object=resource,
            )
        )

    def test_on_department_delete(self):
        main_department = self.create_department()
        department_in_main_department = self.create_department(main_department['id'])
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        on_department_delete(
            self.main_connection,
            self.organization['id'],
            revision,
            department_in_main_department
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            # событие про то, что отдел удалён
            'department_deleted',
            # событие про то, что подотдел удалён из отдела main_department
            'department_department_deleted',
            # удалили группу с руководителем отдела
            'group_deleted',
            'group_membership_changed',
            'resource_grant_changed',
        ]
        assert_that(events, equal_to(expected))
