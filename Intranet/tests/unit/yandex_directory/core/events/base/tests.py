# -*- coding: utf-8 -*-
import datetime

from flask import g
from hamcrest import (
    assert_that,
    has_entries,
    equal_to,
)
from unittest.mock import patch
from sqlalchemy.orm import Session

from testutils import (
    TestCase,
    change_user_data_created_fields_to_isoformat,
)
from intranet.yandex_directory.src.yandex_directory.common.components import component_registry
from intranet.yandex_directory.src.yandex_directory.common.db import get_meta_connection
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    TYPE_RESOURCE,
    TYPE_USER,
    TYPE_DOMAIN,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import action_user_add
from intranet.yandex_directory.src.yandex_directory.core.actions.base import save_action
from intranet.yandex_directory.src.yandex_directory.core.events import (
    event_domain_added,
    event_domain_deleted,
    event_domain_occupied,
    event_domain_alienated,
)
from intranet.yandex_directory.src.yandex_directory.core.events.base import (
    yield_events,
    save_event,
)
from intranet.yandex_directory.src.yandex_directory.core.events.department import (
    _resource_relation_change_for_departments,
)
from intranet.yandex_directory.src.yandex_directory.core.events.resource import (
    event_resource_changed_for_each_resource,
)
from intranet.yandex_directory.src.yandex_directory.core.events.utils import (
    get_content_object,
)
from intranet.yandex_directory.src.yandex_directory.core.models import DomainModel, ServiceModel
from intranet.yandex_directory.src.yandex_directory.core.models.action import ActionModel
from intranet.yandex_directory.src.yandex_directory.core.models.event import EventModel
from intranet.yandex_directory.src.yandex_directory.core.models.organization import OrganizationModel
from intranet.yandex_directory.src.yandex_directory.core.models.resource import (
    ResourceModel,
    ResourceRelationModel,
)


class TestBaseFunctions(TestCase):
    def test_save_action(self):
        # до теста в g нет номера ревизии
        g.revision = None

        user = self.create_user()
        author_id = 1
        object_value = user
        action_name = 'user_do_some_test_action'
        revision = save_action(
            self.main_connection,
            org_id=self.organization['id'],
            name=action_name,
            author_id=author_id,
            object_value=object_value
        )

        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        user = change_user_data_created_fields_to_isoformat(user)
        self.assertTrue(action['id'])
        self.assertEqual(action['object'], user)
        self.assertEqual(action['author_id'], author_id)
        self.assertEqual(action['name'], action_name)

        # проверим, что новый номер ревизии записался в g
        self.assertEqual(g.get('revision'), revision)

    def test_save_action_should_save_new_revision_to_organization(self):
        # проверим смену номера ревизии после сохранения action-а
        # сохраним один action для первоначального номера ревизии
        save_action(
            self.main_connection,
            org_id=self.organization['id'],
            name='some_action',
            author_id=1,
            object_value=self.create_user(),
        )

        organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['revision']
        )

        revision = save_action(
            self.main_connection,
            org_id=self.organization['id'],
            name='some_action',
            author_id=1,
            object_value=self.create_user(),
        )

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['revision'],
        )

        # проверим, что новый номер ревизии записался в g
        self.assertEqual(g.get('revision'), revision)
        self.assertEqual(g.get('revision'), fresh_organization['revision'])

        # проверим, что номер ревизии действительно поменялся
        self.assertNotEqual(organization['revision'], fresh_organization['revision'])

    def test_yield_events_terminal(self):
        revision = 1
        name = 'resource_grant_changed'
        user = self.create_user()
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': user['id']
                }
            ]
        )
        content = get_content_object(subject=user)
        result = yield_events(
            self.main_connection,
            name=name,
            org_id=self.organization['id'],
            revision=revision,
            object_value=resource,
            object_type=TYPE_RESOURCE,
            content=content,
        )
        self.assertFalse(result)

    def test_yield_events_non_terminal(self):
        revision = 1
        user = self.create_user()
        result = action_user_add(
            self.main_connection,
            self.organization['id'],
            revision,
            user
        )
        self.assertTrue(result)

    def test_save_event(self):
        revision = 1
        name = 'user_some_test_action_done'
        user = self.create_user()
        object_type = TYPE_USER
        content = {}
        save_event(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=user,
            object_type=object_type,
            content=content,
            name=name,
        )


class TestCommonFunctions(TestCase):
    def test_resource_changed_for_each_resource(self):
        """
        Есть департамент с правами read и admin на ресурс, в нём один юзер
        """
        department = self.create_department()
        resource_one = self.create_resource_with_department(
            department['id'], 'read')
        ResourceRelationModel(self.main_connection).create(
            self.organization['id'],
            resource_one['id'],
            'admin',
            department_id=department['id'],
        )
        user = self.create_user(department['id'])
        revision = 1
        object_value = user
        object_type = 'user'
        path = department['path']
        EventModel(self.main_connection).delete(force_remove_all=True)
        event_resource_changed_for_each_resource(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=object_value,
            object_type=object_type,
            path=path
        )

    def test__resource_relation_change_for_departments(self):
        # Есть департамент с правами read и admin на ресурс, в нём один юзер

        department_one = self.create_department()
        department_two = self.create_department()
        resource_one = self.create_resource_with_department(
            department_two['id'], 'read')
        user = self.create_user(department_two['id'])

        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        _resource_relation_change_for_departments(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=user,
            object_type=TYPE_USER,
            department_after=department_two,
            department_before=department_one,
        )
        event = EventModel(self.main_connection).find({'name': 'resource_grant_changed'})[0]
        relations_add = event['content']['relations']['add']
        relations_delete = event['content']['relations']['remove']
        expected_relation = {
            'object_id': user['id'],
            'relation_name': 'read',
        }
        assert_that(
            relations_delete,
            has_entries(users=[],
                        departments=[],
                        groups=[],
                        ))
        assert_that(
            relations_add,
            has_entries(users=[expected_relation, ],
                        departments=[],
                        groups=[],
                        ))
        assert_that(event['content']['uids'], [user['id']])
        assert_that(
            event,
            has_entries(
                name='resource_grant_changed',
                org_id=self.organization['id'],
                revision=revision,
                object=resource_one,
                content=event['content']
            )
        )


class TestDomainResourceSavingOnEvents(TestCase):
    def _assert_that_domain_resource_history_saved(self, service_id, domain_name, time_before_event, action):
        data = self.meta_connection.execute(
            'select * from resource_history '
            'where service_id = %(service_id)s '
            'and resource_id = %(resource_id)s '
            'and timestamp > %(time_before_event)s '
            'and action = %(action)s ',
            service_id=service_id,
            resource_id=domain_name,
            time_before_event=time_before_event,
            action=action,
        ).fetchall()

        assert_that(
            len(data),
            equal_to(1)
        )

    def setUp(self):
        super(TestDomainResourceSavingOnEvents, self).setUp()
        self.domain_name = 'test.com'

        DomainModel(self.main_connection).create(self.domain_name, self.organization['id'])

        with get_meta_connection() as meta_connection:
            service_id = ServiceModel(meta_connection).get_by_slug('dominator')['id']
            self.service_id = service_id

    def test_event_domain_added(self):
        time_before_event = datetime.datetime.utcnow()

        event_domain_added(
            self.main_connection,
            org_id=self.organization['id'],
            revision=9999999,
            object_value={'name': self.domain_name},
            object_type=TYPE_DOMAIN,
            author_id=123,
        )

        self.meta_session.flush()

        self._assert_that_domain_resource_history_saved(
            self.service_id,
            self.domain_name,
            time_before_event,
            'domain_added',
        )

    def test_event_domain_domain_occupied(self):
        time_before_event = datetime.datetime.utcnow()

        event_domain_occupied(
            self.main_connection,
            org_id=self.organization['id'],
            revision=9999999,
            object_value={'name': self.domain_name},
            object_type=TYPE_DOMAIN,
            author_id=123,
        )

        self.meta_session.flush()

        self._assert_that_domain_resource_history_saved(
            self.service_id,
            self.domain_name,
            time_before_event,
            'domain_occupied',
        )

    def test_event_domain_domain_deleted(self):
        time_before_event = datetime.datetime.utcnow()

        event_domain_deleted(
            self.main_connection,
            org_id=self.organization['id'],
            revision=9999999,
            object_value={'name': self.domain_name},
            object_type=TYPE_DOMAIN,
            author_id=123,
        )

        self.meta_session.flush()

        self._assert_that_domain_resource_history_saved(
            self.service_id,
            self.domain_name,
            time_before_event,
            'domain_deleted',
        )

    def test_event_domain_domain_alienated(self):
        time_before_event = datetime.datetime.utcnow()

        event_domain_alienated(
            self.main_connection,
            org_id=self.organization['id'],
            revision=9999999,
            object_value={'name': self.domain_name},
            object_type=TYPE_DOMAIN,
            author_id=123,
        )

        self.meta_session.flush()

        self._assert_that_domain_resource_history_saved(
            self.service_id,
            self.domain_name,
            time_before_event,
            'domain_alienated',
        )
