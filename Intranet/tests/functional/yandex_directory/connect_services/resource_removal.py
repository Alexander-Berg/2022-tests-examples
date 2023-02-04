# -*- coding: utf-8 -*-
import responses

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ResourceModel,
    EventModel,
)
from intranet.yandex_directory.src.yandex_directory.auth import tvm

from testutils import (
    TestCase,
    override_settings,
)


class TestCheckMetrikaResourcesTask(TestCase):

    def setUp(self):
        super(TestCheckMetrikaResourcesTask, self).setUp()
        tvm.tickets['metrika'] = 'ticket-2000269'
        EventModel(self.main_connection).delete(force_remove_all=True)

        self.resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='metrika',
            external_id='12345',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                },
            ]
        )
        self.resource_2 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='metrika',
            external_id='12346',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.user['id'],
                },
            ]
        )

    @responses.activate
    def test_not_delete_correct(self):
        from intranet.yandex_directory.src.yandex_directory.connect_services.resource_removal import CheckMetrikaResourcesTask

        self.assertEqual(ResourceModel(self.main_connection).filter(service='metrika').count(), 2)
        responses.add(
            responses.GET,
            '{}/connect/check_counters_active?ids=12345%2C12346'.format(app.config['METRIKA_HOST']),
            json={'result': [True, True]},
        )

        CheckMetrikaResourcesTask(self.main_connection).delay()

        self.assertEqual(ResourceModel(self.main_connection).filter(service='metrika').count(), 2)

    @responses.activate
    def test_delete_correct(self):
        from intranet.yandex_directory.src.yandex_directory.connect_services.resource_removal import CheckMetrikaResourcesTask
        events_count = EventModel(self.main_connection).filter(
            org_id=self.organization['id'],
            name=event.resource_deleted,
        ).count()
        self.assertEqual(events_count, 0)

        self.assertEqual(ResourceModel(self.main_connection).count(), 6)
        self.assertEqual(ResourceModel(self.main_connection).filter(service='metrika').count(), 2)

        responses.add(
            responses.GET,
            '{}/connect/check_counters_active?ids=12345%2C12346'.format(app.config['METRIKA_HOST']),
            json={'result': [True, False]},
        )

        CheckMetrikaResourcesTask(self.main_connection).do()

        self.assertEqual(ResourceModel(self.main_connection).filter(service='metrika').count(), 1)
        self.assertTrue(ResourceModel(self.main_connection).get(id=self.resource['id']))
        self.assertEqual(ResourceModel(self.main_connection).count(), 5)

        events_count = EventModel(self.main_connection).filter(
            org_id=self.organization['id'],
            name=event.resource_deleted,
        ).count()

        self.assertEqual(events_count, 1)

    @responses.activate
    def test_not_delete_or_errors_correct(self):
        from intranet.yandex_directory.src.yandex_directory.connect_services.resource_removal import CheckMetrikaResourcesTask

        self.assertEqual(ResourceModel(self.main_connection).filter(service='metrika').count(), 2)
        responses.add(
            responses.GET,
            '{}/connect/check_counters_active?ids=12345%2C12346'.format(app.config['METRIKA_HOST']),
            status=500,
        )

        CheckMetrikaResourcesTask(self.main_connection).delay()

        self.assertEqual(ResourceModel(self.main_connection).filter(service='metrika').count(), 2)

    @responses.activate
    def test_delete_many_correct(self):
        from intranet.yandex_directory.src.yandex_directory.connect_services.resource_removal import CheckMetrikaResourcesTask
        events_count = EventModel(self.main_connection).filter(
            org_id=self.organization['id'],
            name=event.resource_deleted,
        ).count()

        self.assertEqual(events_count, 0)

        self.assertEqual(ResourceModel(self.main_connection).filter(service='metrika').count(), 2)
        responses.add(
            responses.GET,
            '{}/connect/check_counters_active?ids=12345%2C12346'.format(app.config['METRIKA_HOST']),
            json={'result': [False, False]},
        )

        CheckMetrikaResourcesTask(self.main_connection).do()

        self.assertEqual(ResourceModel(self.main_connection).filter(service='metrika').count(), 0)

        events_count = EventModel(self.main_connection).filter(
            org_id=self.organization['id'],
            name=event.resource_deleted,
        ).count()

        self.assertEqual(events_count, 2)
