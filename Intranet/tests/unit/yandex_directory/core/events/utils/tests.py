# -*- coding: utf-8 -*-
import random
from intranet.yandex_directory.src import settings
from unittest.mock import (
    patch,
    Mock,
    MagicMock,
)
from hamcrest import (
    assert_that,
    contains_inanyorder,
    contains,
    has_entries,
    equal_to,
    empty,
    has_length,
    none,
    not_none,
    greater_than_or_equal_to,
)

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common import json
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.core.events.utils import (
    Hole,
    get_continuous_revisions,
    group_revisions_by_organizations,
    select_not_processed_revisions,
    get_web_hooks,
    is_service_should_receive_the_event,
    notify_about_new_events,
    get_callbacks_for_events,
    create_callback_tasks,
    EventNotificationTask,
    notify_callback,
    send_data_to_webhook,
    PREPARE_MAP,
    _get_continuous_events,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    utcnow,
    time_in_future,
    hashabledict,
    remove_not_given_keys,
    NotGiven,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    WebHookModel,
    ServiceModel,
    EventModel,
    ActionModel,
    OrganizationServiceModel,
    CallbackEventsModel,
    ProcessedEventsModel,
    LastProcessedRevisionModel,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import action_user_add
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    disable_service,
)

from testutils import (
    TestCase,
    create_organization,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import only_ids

SERVICE = 'test_service'
TEST_NOTIFICATION_URL = 'http://127.0.0.1/my-notification-url/'
NOTIFICATION_HEADERS = {
    'test_header': 'test_value',
}

TEST_CALLBACK_WITH_FILTER = {
    'filter': {'service': SERVICE},
    'callback': TEST_NOTIFICATION_URL,
    'headers': NOTIFICATION_HEADERS,
}

TEST_CALLBACK_WITH_SETTINGS = {
    'callback': TEST_NOTIFICATION_URL,
    'headers': NOTIFICATION_HEADERS,
    'settings': {
        'expand_content': True,
        'verify': False,
    }
}

TEST_CALLBACK = {
    'callback': TEST_NOTIFICATION_URL,
    'headers': NOTIFICATION_HEADERS,
}

SUBSCRIPTIONS = {
    'user_test_subscription': [
        TEST_CALLBACK,
    ],
    'user_test_subscription_with_filter': [
        TEST_CALLBACK_WITH_FILTER,
    ],
}


def dummy_callback(main_connection, callback, data):
    pass


class TestEventUtils(TestCase):
    def setUp(self):
        super(TestEventUtils, self).setUp()

        # Создадим один сервис, не подключенный ни к одной организации
        self.service = ServiceModel(self.meta_connection).create(
            slug='autotest',
            name='autotest',
        )

    def test_service_which_can_work_with_any_organization(self):
        # Сервис который может работать с любой организацией,
        # может получать события любого типа от любой организации,
        # даже если он в ней не подключен.

        # В setUp мы уже создали сервис, который ни к одной организации
        # не подключен. Поэтому просто проверим, как отработает функция.
        can_work = True
        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                self.organization['organization_type'],
                self.service['id'],
                can_work_with_any_organization=can_work,
                event_name='user_added',
            ),
            equal_to(True)
        )

        # При этом для получения уведомлений об облачных
        # организациях нужно явно включить сервис в организацию
        # can_work_with_any_organization тут игнорируется
        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                settings.CLOUD_ORGANIZATION_TYPE,
                self.service['id'],
                can_work_with_any_organization=can_work,
                event_name='user_added',
            ),
            equal_to(False)
        )

        # Но если права на работу с любыми данными нет, то
        # сервис события получать не должен
        can_work = False
        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                self.organization['organization_type'],
                self.service['id'],
                can_work_with_any_organization=can_work,
                event_name='user_added',
            ),
            equal_to(False)
        )

    def test_service_which_was_disabled_receives_service_disabled_event(self):
        # Если сервис отключили от организации, он всё равно должен иметь
        # возможность получить событие service_disabled. Но только его.

        # Так как сервис созданный в setUp, никуда пока не подключен, нам надо
        # включить его для организации, а потом выключить.
        org_id = self.organization['id']
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            self.service['slug']
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            self.service['slug'],
            disable_reason='just for test',
        )

        # Проверим, что сервис может получить только событие service_disabled
        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                self.organization['organization_type'],
                self.service['id'],
                can_work_with_any_organization=False,
                event_name='user_added'
            ),
            # user_added событие отправлено не будет
            equal_to(False)
        )
        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                self.organization['organization_type'],
                self.service['id'],
                can_work_with_any_organization=False,
                # service_disabled событие
                event_name='service_disabled'
            ),
            # будет отправлено
            equal_to(True)
        )

    def test_service_which_not_ready(self):
        # Если сервис подключили в организации, не он еще не готов,  он всё равно должен иметь
        # возможность получить событие service_enabled и service_disabled. Но только их.

        # Так как сервис созданный в setUp, никуда пока не подключен, нам надо
        org_id = self.organization['id']
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            self.service['slug']
        )
        OrganizationServiceModel(self.main_connection).update(
            update_data={'ready': False},
            filter_data={'org_id': org_id, 'service_id': self.service['id']}
        )

        # Проверим, что сервис может получить только событие service_enabled
        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                self.organization['organization_type'],
                self.service['id'],
                can_work_with_any_organization=False,
                event_name='user_added'
            ),
            # user_added событие отправлено не будет
            equal_to(False)
        )

        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                self.organization['organization_type'],
                self.service['id'],
                can_work_with_any_organization=False,
                event_name='service_enabled'
            ),
            # service_enabled событие будет отправлено
            equal_to(True)
        )

        # отключим сервис
        disable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            self.service['slug'],
            disable_reason='just for test',
        )
        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                self.organization['organization_type'],
                self.service['id'],
                can_work_with_any_organization=False,
                event_name='service_disabled'
            ),
            # service_disabled событие будет отправлено
            equal_to(True)
        )

    def test_service_which_ready(self):
        # Если сервис подключили в организации и готов
        # он  может получать все события для этой организации

        org_id = self.organization['id']
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            self.service['slug']
        )

        # Проверим, что сервис может получить событие user_added
        assert_that(
            is_service_should_receive_the_event(
                self.main_connection,
                self.organization['id'],
                self.organization['organization_type'],
                self.service['id'],
                can_work_with_any_organization=False,
                event_name='user_added'
            ),
            # user_added событие  будет отправлено
            equal_to(True)
        )


class TestGetWebHooks(TestCase):
    def test_get_web_hooks(self):
        # получаем подписок подпсичиков на события по webhook

        # создадим подписку на все события
        first_webhook = 'http://example.yandex.net/first'
        WebHookModel(self.meta_connection).create(
            url=first_webhook,
            service_id=self.service['id'],
            event_names=[],  # подписка на все события
            expand_content=True
        )

        # создадим подписку на событие department_added
        second_webhook = 'http://example.yandex.net/second'
        WebHookModel(self.meta_connection).create(
            url=second_webhook,
            service_id=self.service['id'],
            event_names=[event.department_added],
            fields_filter={
                'org_id': self.organization['id']
            }
        )

        # создадим подписку на событие department_deleted
        WebHookModel(self.meta_connection).create(
            url='http://example.yandex.net/3',
            service_id=self.service['id'],
            event_names=[event.department_deleted],
        )

        # получим список подписчиков на событие department_added
        assert_that(
            get_web_hooks(self.meta_connection, event.department_added),
            contains_inanyorder(
                # подписка на все события
                has_entries(
                    callback=first_webhook,
                    settings=has_entries(
                        expand_content=True
                    )
                ),
                # подписка только на событие department_added
                has_entries(
                    callback=second_webhook,
                    settings=has_entries(
                        expand_content=False
                    ),
                    filter=has_entries(
                        org_id=self.organization['id']
                    )
                )
            )
        )

    def test_get_web_hooks_environment(self):
        # Проверим, что возвращатюся вебхуки только для текущего окружения
        first_webhook = 'http://example.yandex.net/first'
        webhook = WebHookModel(self.meta_connection).create(
            url=first_webhook,
            service_id=self.service['id'],
            event_names=[],  # подписка на все события
            expand_content=True
        )
        # обновим окружение первого вебхука
        WebHookModel(self.meta_connection).update(
            filter_data={'id': webhook['id']},
            update_data={'environment': 'prod'}
        )

        second_webhook = 'http://example.yandex.net/second'
        WebHookModel(self.meta_connection).create(
            url=second_webhook,
            service_id=self.service['id'],
            event_names=[event.department_added],
            fields_filter={
                'org_id': self.organization['id']
            }
        )

        # проверим, что список подписчиков department_added содержит только второй вебхук,
        # потому что первый находится в другом окружении
        assert_that(
            get_web_hooks(self.meta_connection, event.department_added),
            contains_inanyorder(
                # подписка только на событие department_added
                has_entries(
                    callback=second_webhook,
                    settings=has_entries(
                        expand_content=False
                    ),
                    filter=has_entries(
                        org_id=self.organization['id']
                    )
                )
            )
        )


class TestNotifyOfNewEvents(TestCase):
    def init(self, *args, **kwargs):
        super(TestNotifyOfNewEvents, self).init(*args, **kwargs)

        self.patchers.append(
            patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.create_callback_tasks')
        )

    def setUp(self):
        super(TestNotifyOfNewEvents, self).setUp()
        # Сделаем вторую организацию, чтобы ревизии перемежались
        second_organization_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label='the_second',
        )
        self.second_organization = second_organization_info['organization']

        # Подчистим пока все события, чтобы в тестах создавать всё чистенько
        self.clean_actions_and_events()


    def make_revisions(self, size=10):
        """Создаёт последовательность из действий и возвращает
           список их ревизий.
        """
        revisions = []

        def create_user_in_org(org):
            user = self.create_user(1, org_id=org['id'])
            # имитируем добавление пользователя
            return action_user_add(
                self.main_connection,
                org_id=org['id'],
                author_id=user['id'],
                object_value=user
            )

        for _ in range(size):
            revisions.append(create_user_in_org(self.organization))
            # создание пользователей перемежаем с другой организацией,
            # чтобы отловить возможные баги
            create_user_in_org(self.second_organization)

        return revisions

    def make_hole(self, size=10):
        """Создаёт последовательность действий, а затем подчищает и их, и соответствующие события.
           Возвращает список ревизий, соответствующих дыре.
        """
        revisions = self.make_revisions(size=size)
        EventModel(self.main_connection) \
            .filter(revision__in=revisions) \
            .delete()
        ActionModel(self.main_connection) \
            .filter(revision__in=revisions) \
            .delete()
        return  revisions

    def assert_last_revision(self, revision, wait_till):
        result = LastProcessedRevisionModel(self.main_connection) \
                 .filter(org_id=self.organization['id']) \
                 .one()
        assert_that(
            result,
            has_entries(
                # Так как была дыра то мы должны были остановиться
                # обработав первую пачку событий
                revision=revision,
                wait_till=wait_till,
            )
        )

    def move_pointer_to(self, obj, wait_till=NotGiven):
        """Двигает указатель на ревизию стоящую перед первой ревизией пачки или конкретную ревизию.
        """
        if isinstance(obj, list):
            revision = obj[0] - 1
        else:
            revision = obj

        LastProcessedRevisionModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(
                **remove_not_given_keys(
                    dict(
                        revision=revision,
                        wait_till=wait_till,
                    )
                )
            )

    def test_revision_grouping(self):
        # Проверим, что группировка корректно группирует ревизии по org_id
        data = [
            (1, 'common', 1, 1),
            (1, 'common', 5, 2),
            (1, 'common', 7, 4),
            (2, settings.CLOUD_ORGANIZATION_TYPE, 1, 1),
            (2, settings.CLOUD_ORGANIZATION_TYPE, 2, 2),
        ]
        expected = {
            1: ('common', [(1, 1), (5, 2), (7, 4)]),
            2: (settings.CLOUD_ORGANIZATION_TYPE, [(1, 1), (2, 2)]),
        }
        result = group_revisions_by_organizations(data)
        self.assertEqual(result, expected)


    def test_continuous_revisions(self):
        # Проверяем, что функция get_continuous_revisions
        # выдаёт правильные результаты.

        # Если у первого элемента delta == 1,
        # то нужно вернуть часть элементов, у которых delta
        # монотонно возрастает.
        data = [(10, 1), (11, 2), (12, 3), (14, 5)]
        expected = [(10, 1), (11, 2), (12, 3)]
        result = get_continuous_revisions(data)
        self.assertEqual(result, expected)

        # А вот если delta > 1, то вместо списка должен быть возвращён объект Hole
        data = [(10, 5), (11, 6), (12, 7), (13, 8)]
        result = get_continuous_revisions(data)
        assert isinstance(result, Hole)
        self.assertEqual(result.revision, 10)
        self.assertEqual(result.delta, 5)


    def test_nonprocessed_events_selection(self):
        # Проверим, как отдадутся необработанные ревизии
        # когда last_processed_revision = 0

        # Имитируем добавление пользователя
        user = self.create_user(1)
        self.action_revision = action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=user
        )

        revisions = select_not_processed_revisions(self.main_connection, self.environment)
        assert revisions == [
            # В тестах почему-то actions не фиксируются, и
            # в базе существует только один action, тот что
            # создан в setUp. Поэтому в списке только одно значение
            (
                self.organization['id'],
                self.organization['organization_type'],
                self.action_revision,
                self.action_revision - 0,  # здесь ноль – потому что пока не обработано ни одной ревизии
            )
        ]

        # Теперь подвинем счётчик на action_revision, сделав вид,
        # что мы всё обработали.
        LastProcessedRevisionModel(self.main_connection) \
            .filter(org_id=self.organization['id']) \
            .update(revision=self.action_revision)

        # функция должна отдать пустой список.
        revisions = select_not_processed_revisions(self.main_connection, self.environment)
        assert revisions == []

    def test_save_last_event(self):
        # Просто обработка пачки событий, когда дыр нет.

        self.clean_actions_and_events()
        size = 5
        batch = self.make_revisions(size=size)

        self.move_pointer_to(batch)

        # Заодно проверим, что для статистики возвращается правильное число ревизий
        self.assertEqual(
            LastProcessedRevisionModel(self.main_connection).get_not_processed_count(),
            # Так как мы создавали данные сразу для двух организаций,
            # то тут надо умножить на два
            size * 2,
        )

        notify_about_new_events(self.meta_connection, self.main_connection)

        self.assert_last_revision(
            batch[-1],
            wait_till=none(),
        )

    def test_no_new_event(self):
        # Если не было новых событий, то указатель на ревизию не должен двигаться

        batch = self.make_revisions()

        # Сделаем вид, что мы батч уже обработали:
        self.move_pointer_to(batch[-1])

        notify_about_new_events(self.meta_connection, self.main_connection)

        # Проверим, что указатель всё так же смотрит на конец "пачки"
        self.assert_last_revision(
            batch[-1],
            wait_till=none(),
        )

    def test_events_with_hole(self):
        # Моделируем ситуацию, когда
        # есть последовательность ревизий, а "дыра" в середине
        # в этом случае, после первой пачки мы должны остановиться
        # и запомнить её макимальную ревизию

        # запоминаем последнее обработанное событие
        self.clean_actions_and_events()

        first_batch = self.make_revisions()
        hole_revisions = self.make_hole()
        second_batch = self.make_revisions()

        # Теперь подвинем указатель на начало первой пачки
        # событий, чтобы начать обработку с неё
        self.move_pointer_to(first_batch)

        notify_about_new_events(self.meta_connection, self.main_connection)

        # Так как была дыра то мы должны были остановиться
        # обработав первую пачку событий
        self.assert_last_revision(
            first_batch[-1],
            wait_till=none(),
        )

    def test_events_with_hole_at_head(self):
        # Моделируем ситуацию, что мы ждём пока зарастёт дыра,
        # и время ещё не подошло. Указатель должен остаться в начале дыры.

        # создадим дыру в событиях в начале блока
        hole_revisions = self.make_hole()
        batch = self.make_revisions()

        # Сделаем вид, что время ещё не подошло
        wait_till = time_in_future(seconds=60)
        self.move_pointer_to(
            # указатель надо поставить на ревизию перед дырой, поэтому -1
            hole_revisions[0] - 1,
            wait_till=wait_till,
        )

        notify_about_new_events(self.meta_connection, self.main_connection)

        # Так как время ещё не подошло, то мы должны остаться перед
        # началом "дыры" и wait_till не должно было измениться
        self.assert_last_revision(
            hole_revisions[0] - 1,
            wait_till,
        )

    def test_events_wait_hole_at_head_expired(self):
        # Моделируем ситуацию - впереди "дыра", и время wait_till
        # подошло, так что мы должны после первого вызова
        # notify_about_new_events - пропустить "дыру",
        # а после второго вызова - обработать оставшиеся события.

        # создадим дыру в событиях в начале блока
        hole_revisions = self.make_hole()
        # Нагенерим ещё событий после "дыры"
        revisions_to_process = self.make_revisions()

        # прикинимся, что мы в начале дыры, и время ожидания подошло к концу
        self.move_pointer_to(hole_revisions, wait_till=utcnow())

        notify_about_new_events(self.meta_connection, self.main_connection)

        self.assert_last_revision(
            # Мы должны были подвинуть указатель на ревизию, предшествующую
            # той, что есть в базе
            hole_revisions[-1],
            wait_till=none(),
        )

        # Теперь запустим процессинг ещё раз, и убедимся, что на этот раз указатель
        # подвинулся на последнюю ревизию
        notify_about_new_events(self.meta_connection, self.main_connection)

        self.assert_last_revision(
            # Мы должны были подвинуть указатель на ревизию, предшествующую
            # той, что есть в базе
            revisions_to_process[-1],
            wait_till=none(),
        )


class TestProcessEvents(TestCase):
    def init(self, *args, **kwargs):
        super(TestProcessEvents, self).init(*args, **kwargs)

        self.patchers.append(
            patch(
                'intranet.yandex_directory.src.yandex_directory.core.events.utils.get_all_oldschool_subscriptions',
                return_value=SUBSCRIPTIONS
            )
        )

    def test_should_not_create_callback_tasks_without_subscribers(self):
        # имитируем добавление пользователя
        user = self.create_user(1)
        action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=user
        )

        events = EventModel(self.main_connection).fields('*', 'environment').filter(
            org_id=self.organization['id'],
        ).order_by('-id')
        result = get_callbacks_for_events(
            self.meta_connection,
            self.main_connection,
            events,
            self.organization['organization_type'],
        )
        assert_that(
            result,
            empty()
        )

    def test_create_callback_tasks_only_if_service_is_enabled(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.get_oldschool_subscriptions',
                   return_value=[]):
            # Если в событии есть org_id, то отправлять его надо только тем
            # сервисам, которые подключены в данной организации
            event = {
                'name': 'user_add',
                'environment': app.config['ENVIRONMENT'],
                'org_id': self.organization['id'],
                'id': 1,
                'object': 'my_object',
                'notify_at': utcnow(),
            }
            # создадим webhook, подписанный на все события
            first_webhook = 'http://example.yandex.net/first'
            WebHookModel(self.meta_connection).create(
                url=first_webhook,
                service_id=self.service['id'],
                event_names=[],  # подписка на все события
            )

            # создадим webhook, подписанный только на 1 событие, но с тем же url
            first_webhook = 'http://example.yandex.net/first'
            WebHookModel(self.meta_connection).create(
                url=first_webhook,
                service_id=self.service['id'],
                event_names=['user_add'],  # подписка на user_add
            )

            # Но так как сервис не подключен в организации,
            # то событие не должно быть отправлено
            result = get_callbacks_for_events(
                self.meta_connection,
                self.main_connection,
                [event],
                self.organization['organization_type'],
            )
            assert_that(
                result,
                empty()
            )

            # Включим Сервис для организации
            OrganizationServiceModel(self.main_connection).create(
                service_id=self.service['id'],
                org_id=self.organization['id'],
                ready=True,
            )

            # Так как сервис теперь включён
            # то событие должно быть отправлено
            result = get_callbacks_for_events(
                self.meta_connection,
                self.main_connection,
                [event],
                self.organization['organization_type'],
            )
            assert_that(
                result,
                has_length(1)
            )
            # 1 оповещение
            assert_that(
                list(result.items()),
                has_length(1)
            )

            # Теперь отключим сервис от организации и выдадим ему scope
            # work_with_any_organization.
            filter_fields = {
                'service_id': self.service['id'],
                'org_id': self.organization['id'],
            }

            OrganizationServiceModel(self.main_connection).delete(filter_fields)
            ServiceModel(self.meta_connection).update(
                {
                    'scopes': [scope.work_with_any_organization],
                },
                {'id': self.service['id']},
            )

            # Так как сервис может работать с любой организацией,
            # то событие должно быть отправлено
            result = get_callbacks_for_events(
                self.meta_connection,
                self.main_connection,
                [event],
                self.organization['organization_type'],
            )
            assert_that(
                result,
                has_length(1)
            )

    def test_should_create_callback_tasks_to_subscribers_without_filters(self):
        event = {
            'name': 'user_test_subscription',
            'environment': app.config['ENVIRONMENT'],
            'org_id': self.organization['id'],
            'id': 1,
            'object': 'my_object',
            'notify_at': utcnow(),
        }
        expected_events = {hashabledict({
            'settings': {
                'headers': NOTIFICATION_HEADERS,
            },
            'event_id': 1
        })}

        expected_result = {
            (self.organization['id'], TEST_NOTIFICATION_URL): expected_events
        }
        result = get_callbacks_for_events(
            self.meta_connection,
            self.main_connection,
            [event],
            self.organization['organization_type'],
        )
        self.assertEqual(
            result,
            expected_result
        )

    def test_create_callback_tasks_to_subscribers_if_it_satisfies_conditions(self):
        event = {
            'name': 'user_test_subscription_with_filter',
            'environment': app.config['ENVIRONMENT'],
            'org_id': self.organization['id'],
            'id': 1,
            'object': {'service': SERVICE},
            'notify_at': utcnow(),
        }

        expected_events = {hashabledict({
            'settings': {
                'headers': NOTIFICATION_HEADERS,
            },
            'event_id': 1
        })}
        expected_result = {
            (self.organization['id'], TEST_NOTIFICATION_URL): expected_events
        }

        result = get_callbacks_for_events(
            self.meta_connection,
            self.main_connection,
            [event],
            self.organization['organization_type'],
        )
        self.assertEqual(
            result,
            expected_result
        )

    def test_send_to_db_subscriptions_all_events(self):
        # оповещение для подписок из БД
        event = {
            'name': 'test_db_subscription',
            'environment': app.config['ENVIRONMENT'],
            'org_id': self.organization['id'],
            'id': 1,
            'object': {'object': 'without-filter-data'},
            'notify_at': utcnow(),
        }
        client_id = 'jksdhfkhsdjkfhkjashfhajsfhksahksfd'
        service = ServiceModel(self.meta_connection).create(
            slug='slug',
            name='autotest',
            client_id=client_id,
            scopes=[scope.work_with_any_organization],
        )
        subscribed_events = [
            ([], 1),  # подписка на все события
            (['test_db_subscription'], 1),  # подписка на определенное событие
            (['event_not_fired'], 0),  # подписка на событие которое не происходит
        ]
        for event_names, call_count in subscribed_events:
            # подписка на события
            WebHookModel(self.meta_connection).delete(force_remove_all=True)
            WebHookModel(self.meta_connection).create(
                url=TEST_NOTIFICATION_URL,
                service_id=service['id'],
                event_names=event_names,
            )
            result = get_callbacks_for_events(
                self.meta_connection,
                self.main_connection,
                [event],
                self.organization['organization_type'],
            )
            if call_count:
                expected_events = {hashabledict({
                    'settings': {
                        'headers': {},
                        'expand_content': False
                    },
                    'event_id': 1
                })}
                expected_result = {
                    (self.organization['id'], TEST_NOTIFICATION_URL): expected_events
                }
                self.assertEqual(
                    result,
                    expected_result
                )

    def test_should_not_send_notification_to_subscribers_if_it_was_filtered(self):
        event = {
            'name': 'user_test_subscription_with_filter',
            'environment': app.config['ENVIRONMENT'],
            'org_id': self.organization['id'],
            'id': 1,
            'object': {'object': 'without-filter-data'},
            'notify_at': utcnow(),
        }
        with patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.create_callback_tasks', Mock()) as mocked_create_callback_tasks:
            get_callbacks_for_events(
                self.meta_connection,
                self.main_connection,
                [event],
                self.organization['organization_type'],
            )
            self.assertEqual(mocked_create_callback_tasks.call_count, 0)


class TestCreateCallbackTask(TestCase):
    def test_create_tasks(self):
        # создаем задачу на оповещение о событии
        # она состоит из уникальных callback, event_id и settings

        EventModel(self.main_connection).delete(force_remove_all=True)
        user = self.create_user(1)
        action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=user
        )

        user = self.create_user(1)
        action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=user
        )

        all_events = EventModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id']
            },
            order_by='-id',
        )
        org_id = self.organization['id']
        event_key = (org_id, TEST_NOTIFICATION_URL,)
        events = {
            (event_key): [{
                'event_id': event['id'],
                'settings': {'expand_content': False, 'headers': {}},
            } for event in all_events]
        }
        # дублирующий callback и event_id, но с другими настройками оповещения
        events[event_key] += [{
            'event_id': event['id'],
            'settings': {'expand_content': True, 'headers': {'Token': 'my-token'}},
        } for event in all_events]

        # пусть уже 1 оповещение есть в базе (дубли игнорируются)
        CallbackEventsModel(self.main_connection).create(
            callback=TEST_NOTIFICATION_URL,
            event_id=events[event_key][0]['event_id'],
            settings=events[event_key][0]['settings'],
            environment='autotests',
        )

        with patch.object(EventNotificationTask, 'delay') as mock_task:
            create_callback_tasks(self.main_connection, events)
        expected_events = [
            has_entries(
                callback=TEST_NOTIFICATION_URL,
                event_id=event_id,
                settings={'expand_content': False, 'headers': {}},
            )
            for event_id in only_ids(all_events)
        ]
        expected_events += [
            has_entries(
                callback=TEST_NOTIFICATION_URL,
                event_id=event_id,
                settings={'expand_content': True, 'headers': {'Token': 'my-token'}},
            )
            for event_id in only_ids(all_events)
        ]
        assert_that(
            CallbackEventsModel(self.main_connection).find(),
            contains_inanyorder(
                *expected_events
            )
        )
        mock_task.assert_called_once_with(org_id=org_id, callback=TEST_NOTIFICATION_URL)


class TestNotifyCallback(TestCase):
    def setUp(self):
        super(TestNotifyCallback, self).setUp()
        user = self.create_user(1)
        action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=user
        )
        self.event = EventModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'name': 'user_added',
            },
            limit=1,
            order_by='-id',
            one=True,
        )
        self.content = {'field': 'value'}

    def test_must_call_do_sending_http(self):
        # если callback url, то вызовем функцию do_sending_http

        my_header = {'X-HEADER': 1}
        settings = {'headers': my_header}
        with patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.call_callback') as mock_do_call, \
                patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.send_data_to_webhook') as do_sending_http:
            notify_callback(
                self.meta_connection,
                self.main_connection,
                TEST_NOTIFICATION_URL,
                self.event,
                settings,
            )

        assert_that(mock_do_call.call_count, equal_to(0))
        do_sending_http.assert_called_once()
        args, _ = do_sending_http.call_args
        object_type = 'user'
        assert_that(
            args,
            contains(
                TEST_NOTIFICATION_URL,
                has_entries(
                    org_id=self.event['org_id'],
                    revision=self.event['revision'],
                    obj=PREPARE_MAP.get(object_type)(self.main_connection, self.event['object']),
                ),
                my_header,
                None,
            )
        )

    def test_must_do_call(self):
        # если callback это ссылка на функцию, то вызовем функцию
        callback = '{}.{}'.format(dummy_callback.__module__, dummy_callback.__name__)
        my_header = {'X-HEADER': 1}
        settings = {'headers': my_header}
        with patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.call_callback') as mock_do_call, \
                patch('intranet.yandex_directory.src.yandex_directory.core.events.utils.send_data_to_webhook') as do_sending_http:
            notify_callback(
                self.meta_connection,
                self.main_connection,
                callback,
                self.event,
                settings,
            )

        assert_that(do_sending_http.call_count, equal_to(0))
        mock_do_call.assert_called_once()
        args, _ = mock_do_call.call_args
        object_type = 'user'
        assert_that(
            args,
            contains(
                self.main_connection,
                dummy_callback,
                has_entries(
                    org_id=self.event['org_id'],
                    revision=self.event['revision'],
                    content=self.event['content'],
                    obj=PREPARE_MAP.get(object_type)(self.main_connection, self.event['object']),
                )
            )
        )


class TestDoSendingHttp(TestCase):
    def setUp(self):
        super(TestDoSendingHttp, self).setUp()
        self.user = self.create_user(1)

    def test_me(self):
        # вызовем функцию do_sending_http
        mocked_requests_session = Mock()
        mocked_golovan_aggregator = Mock()
        mocked_golovan_aggregator.log_work_time.return_value = MagicMock()

        with patch('intranet.yandex_directory.src.yandex_directory.app.requests', mocked_requests_session), \
             patch('intranet.yandex_directory.src.yandex_directory.app.stats_aggregator', mocked_golovan_aggregator):
            data = {
                'event': 'user_added',
                'revision': 1,
                'obj': self.user,
                'org_id': self.organization['id'],
            }
            custom_headers = {'X-HEADER': 1}
            send_data_to_webhook(TEST_NOTIFICATION_URL, data, custom_headers, False)

        assert_that(mocked_requests_session.post.call_count, 1)

        args, kwargs = mocked_requests_session.post.call_args
        assert_that(kwargs['url'], equal_to(TEST_NOTIFICATION_URL))
        assert_that(
            kwargs,
            has_entries(
                timeout=app.config['DEFAULT_NOTIFICATION_TIMEOUT'],
                verify=False,
                headers=equal_to({
                    'Content-Type': 'application/json',
                    'X-Org-ID': str(self.organization['id']),
                    'X-HEADER': 1
                }),
            )
        )

        actual_data = json.loads(kwargs['data'])
        expected_data = {
            'event': 'user_added',
            'revision': 1,
            'object': has_entries(
                id=self.user['id'],
            ),
            'content': None,
            'org_id': self.organization['id'],
        }
        assert_that(
            actual_data,
            has_entries(
                **expected_data
            )
        )


class TestGetContinuousEvents(TestCase):

    def setUp(self):
        # непрерывная последовательность событий
        self.consistent_events = []
        for i in range(1, 30):
            self.consistent_events.append({
                'id': i
            })

        self.last_event_id = 0

    def test_all_events_consistent(self):
        # вариант когда нет дыр в блоке событий
        shuffled = self.consistent_events[:]
        random.shuffle(shuffled)
        actual = _get_continuous_events(shuffled, self.last_event_id)
        assert_that(
            actual,
            equal_to(
                self.consistent_events
            )
        )

    def test_events_with_hole(self):
        # вариант когда блоке событий есть дыра
        events_with_hole = self.consistent_events[:3] + self.consistent_events[6:]

        actual = _get_continuous_events(events_with_hole, self.last_event_id)
        assert_that(
            actual,
            equal_to(self.consistent_events[:3])
        )

    def test_events_with_hole_at_head(self):
        # вариант когда блоке событий есть дыра в начале блока
        events_with_hole = self.consistent_events[6:]

        actual = _get_continuous_events(events_with_hole, self.last_event_id)
        assert_that(
            actual,
            equal_to([])
        )

    def test_events_with_ignored_hole_at_head(self):
        # вариант когда блоке событий есть дыра в начале блока, но мы ее игнорируем
        events_with_hole = self.consistent_events[6:7] + self.consistent_events[8:]

        actual = _get_continuous_events(events_with_hole, self.last_event_id, skip_head_hole=True)
        assert_that(
            actual,
            equal_to(self.consistent_events[6:7])
        )

    def test_empty_events_list(self):
        # вариант когда пусто блок событий
        actual = _get_continuous_events([], self.last_event_id)
        assert_that(
            actual,
            equal_to([])
        )
