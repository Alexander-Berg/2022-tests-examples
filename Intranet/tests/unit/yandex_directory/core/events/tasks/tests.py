# -*- coding: utf-8 -*-
from unittest.mock import (
    patch,
)
from hamcrest import (
    assert_that,
    contains,
    has_entries,
    equal_to,
)

from datetime import timedelta

from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow
from intranet.yandex_directory.src.yandex_directory.core.models import (
    EventModel,
    CallbackEventsModel,
    TaskModel,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import action_user_add

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.core.events.tasks import EventNotificationTask
from intranet.yandex_directory.src.yandex_directory import app

TEST_NOTIFICATION_URL = 'http://127.0.0.1/my-notification-url/'


class TestEventNotificationTask(TestCase):

    def create_user_added_event(self, notify_at=None):
        user1 = self.create_user(1)
        action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user1['id'],
            object_value=user1
        )
        last_event = EventModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'name': 'user_added',
            },
            limit=1,
            order_by='-id',
            one=True,
        )

        EventModel(self.main_connection).update(
            filter_data={'id': last_event['id']},
            update_data={'notify_at': notify_at or utcnow()}
        )

    def setUp(self):
        super(TestEventNotificationTask, self).setUp()
        EventModel(self.main_connection).delete(force_remove_all=True)
        CallbackEventsModel(self.main_connection).delete(force_remove_all=True)

    def test_notify_with_defer(self):
        self.create_user_added_event()
        second_event_notify_at = utcnow() + timedelta(seconds=24*60*60)
        self.create_user_added_event(second_event_notify_at)
        self.create_user_added_event()
        events = EventModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'name': 'user_added',
            }
        )

        for event in events:
            CallbackEventsModel(self.main_connection).create(
                callback=TEST_NOTIFICATION_URL,
                event_id=event['id'],
                settings={},
                environment=app.config['ENVIRONMENT'],
            )

        with patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.notify_callback', return_value=(200, None)):
            task = EventNotificationTask(self.main_connection).delay(
                org_id=self.organization['id'],
                callback=TEST_NOTIFICATION_URL,
            )

            # что бы корректно обрабатывался defer
            EventNotificationTask.tries_delay = 0
            self.process_tasks()
            self.process_tasks()

        # задача отложилась
        subtask = TaskModel(self.main_connection).find(
            filter_data={'id': task.get_metadata()['task_ids'][0]},
            one=True
        )
        assert_that(
            subtask,
            has_entries(
                start_at=events[1]['notify_at'],
                state='free'
            )
        )

        # осталось 2 необработанных оповещения
        callbacks = CallbackEventsModel(self.main_connection).all()
        callbacks_data = [(c['event_id'], c['done']) for c in callbacks]
        assert_that(
            callbacks_data,
            contains(
                # Первый callback должен быть обработан, а последующие - нет,
                # так как их выполнение отложено
                equal_to((events[0]['id'], True)),
                equal_to((events[1]['id'], False)),
                equal_to((events[2]['id'], False)),
            )
        )

    def test_notify_all_events(self):
        # Проверим, что таск отправит все события в указанный URL,
        # а во второй URL – не отправит (в какой-то момент у нас был баг,
        # из-за которого иногда события не отправлялись:
        # https://st.yandex-team.ru/DIR-5055
        OTHER_URL = 'http://other-url'

        self.create_user_added_event()
        self.create_user_added_event()
        self.create_user_added_event()
        events = EventModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'name': 'user_added',
            }
        )
        for event in events:
            CallbackEventsModel(self.main_connection).create(
                callback=TEST_NOTIFICATION_URL,
                event_id=event['id'],
                settings={},
                environment='autotests',
            )
            # Эти callback_events должны остаться нетронутыми
            CallbackEventsModel(self.main_connection).create(
                callback=OTHER_URL,
                event_id=event['id'],
                settings={},
                environment='autotests',
            )

        with patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.notify_callback', return_value=(200, '')) as notify_callback_mock:
            task = EventNotificationTask(self.main_connection).delay(
                org_id=self.organization['id'],
                callback=TEST_NOTIFICATION_URL,
            )

            # что бы корректно обрабатывался defer
            EventNotificationTask.tries_delay = 0

            # выполнили первый раз, таска должна отложится и поставить одну дочернюю
            self.process_task(task.task_id)
            assert task._get_task_data(force=True)['state'] == 'free'
            assert utcnow() < task._get_task_data()['start_at']
            assert task.get_metadata()['event_id_from'] == events[-1]['id'] + 1
            assert len(task.get_metadata()['task_ids']) == 1

            # выполнили второй раз родительску, ничего не должно поменятся
            self.process_task(task.task_id)
            assert task._get_task_data(force=True)['state'] == 'free'
            assert utcnow() < task._get_task_data()['start_at']
            assert task.get_metadata()['event_id_from'] == events[-1]['id'] + 1
            assert len(task.get_metadata()['task_ids']) == 1

            # выполнили дочернюю, должна выполниться успешно, проверяем, что до выполнения таски коллбек не вызывался
            assert notify_callback_mock.call_count == 0

            chunk_task_id = task.get_metadata()['task_ids'][0]
            self.process_task(chunk_task_id)
            chunk_task = TaskModel(self.main_connection).get(chunk_task_id)
            assert chunk_task['state'] == 'success'
            assert notify_callback_mock.call_count == 3

            # выполнили родтельскую, должна перепоставится
            self.process_task(task.task_id)
            assert task._get_task_data(force=True)['state'] == 'free'
            assert task.get_metadata()['event_id_from'] == None
            assert len(task.get_metadata()['task_ids']) == 1

            # выполнили родтельскую, должна заверишться
            self.process_task(task.task_id)
            assert task._get_task_data(force=True)['state'] == 'success'
            assert task.get_metadata()['event_id_from'] == None
            assert len(task.get_metadata()['task_ids']) == 1

        # задача выполнилась
        assert_that(
            task.state,
            equal_to('success')
        )
        # все оповещения обработаны
        callbacks_count = CallbackEventsModel(self.main_connection) \
                          .filter(done=False, callback=TEST_NOTIFICATION_URL) \
                          .count()
        assert_that(
            callbacks_count,
            equal_to(0)
        )

        # а вот во второй URL события не отправились
        callbacks_count = CallbackEventsModel(self.main_connection) \
                          .filter(done=False, callback=OTHER_URL) \
                          .count()
        assert_that(
            callbacks_count,
            equal_to(len(events))
        )
