# -*- coding: utf-8 -*-
import datetime

from hamcrest import (
    assert_that,
    equal_to,
)
from testutils import TestCase

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.models.event import EventModel, ProcessedEventsModel


class TestEventModel_filter(TestCase):

    def setUp(self):
        super(TestEventModel_filter, self).setUp()
        for i in range(11):
            service = 'service{}'.format(i)
            EventModel(self.main_connection).create(
                org_id=self.organization['id'],
                revision=1,
                name='name',
                object_type='object_type',
                object_value={'service': service},
                content=None,
            )
            dt = utcnow() - datetime.timedelta(days=i)
            EventModel(self.main_connection).update({
                'timestamp':  dt.isoformat()
            }, filter_data={'object.service': service})

    def test_timestamp__lt__not_date(self):
        # если передали не datetime
        with self.assertRaises(ValueError):
            EventModel(self.main_connection).delete({
                'timestamp__lt': 'not date'
            })

    def test_truncate__old_date(self):
        # усекаем старше n дней
        EventModel(self.main_connection).truncate(days=7)
        self.assertEqual(EventModel(self.main_connection).count(), 7)


class TestProcessedEventsModel(TestCase):
    def setUp(self):
        super(TestProcessedEventsModel, self).setUp()

    def test_no_processed_events(self):
        # нет событий младше часа
        assert_that(
            ProcessedEventsModel(self.main_connection).get_events_hole_for_last_hour(),
            equal_to(0)
        )

    def test_has_hole(self):
        # есть дыры
        # создадим дыру в оповещении о событиях для тестирования метрики
        first_event = EventModel(self.main_connection).create(
            org_id=self.organization['id'],
            revision=self.get_org_revision(self.organization['id']),
            name=event.user_added,
            object_value={},
            object_type='user',
            content={},
        )
        ProcessedEventsModel(self.main_connection).insert_into_db(
            event_id=first_event['id'],
            environment=app.config['ENVIRONMENT'],
        )
        EventModel(self.main_connection).create(
            org_id=self.organization['id'],
            revision=self.get_org_revision(self.organization['id']),
            name=event.user_added,
            object_value={},
            object_type='user',
            content={},
        )
        third_event = EventModel(self.main_connection).create(
            org_id=self.organization['id'],
            revision=self.get_org_revision(self.organization['id']),
            name=event.user_added,
            object_value={},
            object_type='user',
            content={},
        )
        ProcessedEventsModel(self.main_connection).insert_into_db(
            event_id=third_event['id'],
            environment=app.config['ENVIRONMENT'],
        )

        assert_that(
            ProcessedEventsModel(self.main_connection).get_events_hole_for_last_hour(),
            equal_to(1)
        )
