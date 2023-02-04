# coding: utf-8
import datetime

from requests import Response

from testutils import (
    frozen_time,
    assert_not_called,
    TestCase,
    override_settings,
    override_mailer,
    get_auth_headers,
    assert_called_once,
    create_organization_without_domain,
    create_organization,
)
from dateutil.relativedelta import relativedelta
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    contains,
    empty,
    has_length,
    has_entry,
)
from unittest.mock import patch, Mock
from contextlib import contextmanager

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.auth.middlewares import (
    Service,
)
from intranet.yandex_directory.src.yandex_directory.common.models.types import ROOT_DEPARTMENT_ID
from intranet.yandex_directory.src.yandex_directory.core.models.organization import organization_type
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    ServiceModel,
    ServicesLinksModel,
    OrganizationServiceModel,
    enable_service,
    UserServiceLicenses,
    TRACKER_SERVICE_SLUG,
    WIKI_SERVICE_SLUG,
    reason,
    disable_licensed_services_by_trial,
    trial_status,
    on_service_enable,
    TrackerBillingStatusModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.license import TrackerLicenseLogModel
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    only_attrs,
    lang_for_notification,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ActionModel,
    EventModel,
    OrganizationModel,
    ResourceModel,
    UserModel,
)
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.common.utils import Ignore, utcnow


class TestOrganizationServiceAction(TestCase):
    def setUp(self):
        super(TestOrganizationServiceAction, self).setUp()
        self.service_slug = 'autotest-service'
        self.service_name = 'Autotest service'
        self.service = ServiceModel(self.meta_connection).create(
            slug=self.service_slug,
            name=self.service_name,
            client_id='client_id',
        )
        self.another_service_name = 'Autotest service name'
        self.another_service_slug = 'another-service-slug'
        self.another_service = ServiceModel(self.meta_connection).create(
            slug=self.another_service_slug,
            name=self.another_service_name,
            client_id='another_client_id',
        )
        self.paid_service_name = 'Autotest paid service name'
        self.paid_service_slug = 'paid-service'
        self.paid_service = ServiceModel(self.meta_connection).create(
            slug=self.paid_service_slug,
            name=self.paid_service_name,
            client_id='paid_client_id',
            paid_by_license=True,
        )
        OrganizationServiceModel(self.main_connection).delete(force_remove_all=True)
        ActionModel(self.main_connection).delete(force_remove_all=True)
        EventModel(self.main_connection).delete(force_remove_all=True)

    @contextmanager
    def as_service(self, slug, org_id):
        """Мокает аутентификацию так, чтобы выглядело,
        что пришел сервис с заданным слагом.
        """
        service = ServiceModel(self.meta_connection).find(
            {'slug': slug},
            one=True
        )
        if not service:
            raise RuntimeError('Сервис не найден в базе')

        with patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.AuthMiddleware._authenticate') as authenticate:
            # Симулируем логин от имени неправильного сервиса
            authenticate.return_value = {
                    'auth_type': 'tvm',
                    'service': Service(
                        id=service['id'],
                        name=service['name'],
                        identity=service['slug'],
                        is_internal=True,
                        ip='127.0.0.1',
                    ),
                    'scopes': [],
                    'user': None,
                    'org_id': org_id,
                }

            yield

    def test_tracker_enabled(self):
        """
        Проверяем, что при подключении трекера в cloud организацию мы не отдаем сервисы для юзеров
        """
        ServiceModel(self.meta_connection).create(
            slug='tracker',
            name='tracker',
            client_id='tracker_client_id',
            ready_default=False,
            paid_by_license=True,
            trial_period_months=1,
        )
        org_id = self.organization['id']

        OrganizationModel(self.main_connection).update_one(org_id, {'organization_type': 'cloud'})

        def check_users():
            users = self.get_json('/v6/users/', query_string='fields=services,tracker_licenses')
            assert len(users['result']) == 1
            assert len(users['result'][0]['services']) == 0
            assert len(users['result'][0]['tracker_licenses']) == 0

        check_users()

        self.post_json('/services/tracker/enable/', data={}, expected_code=201)
        check_users()

        with self.as_service('tracker', self.organization['id']):
            self.post_json('/services/tracker/ready/', data={}, expected_code=200)
        check_users()

    def test_enable(self):
        # подключаем новый сервис к организации
        self.post_json('/services/%s/enable/' % self.service_slug, data={}, expected_code=201)

        assert_that(
            OrganizationServiceModel(self.main_connection).find(),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    service_id=self.service['id']
                )
            )
        )
        # появилось событие "service_enabled"
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.service_enabled,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.service_enabled,
                    content=has_entries(
                        slug=self.service['slug'],
                        name=self.service['name']
                    )
                )
            )
        )

    def test_enable_forbidden_for_partner_org(self):
        OrganizationModel(self.main_connection).change_organization_type(
            org_id=self.organization['id'],
            new_org_type=organization_type.partner_organization,
            partner_id=self.partner['id'],
        )
        self.post_json(
            '/services/%s/enable/' % self.paid_service_slug,
            data={},
            expected_code=403,
        )

    def test_save_payment_correct(self):
        self.assertIsNone(
            TrackerBillingStatusModel(self.main_connection).filter(org_id=self.organization['id']).one()
        )
        tracker = ServiceModel(self.meta_connection).create(
            id=144,
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker['slug'],
        )
        self.assertEqual(
            TrackerBillingStatusModel(self.main_connection).filter(
                org_id=self.organization['id']
            ).count(),
            1
        )
        billing_status = TrackerBillingStatusModel(self.main_connection).filter(org_id=self.organization['id']).one()
        self.assertFalse(billing_status['payment_status'])

        # подвинем дату оплаты вперед
        TrackerBillingStatusModel(self.main_connection).update(
            filter_data={'org_id': self.organization['id']},
            update_data={'payment_date': utcnow() + datetime.timedelta(hours=300)}
        )
        # повторный вызов функции при включении сервиса не должен приводить
        # к появлению новой записи
        on_service_enable(self.main_connection, self.organization['id'], 'tracker', tracker['id'])

        self.assertEqual(
            TrackerBillingStatusModel(self.main_connection).filter(
                org_id=self.organization['id']
            ).count(),
            1
        )

        # подвинем дату оплаты назад
        TrackerBillingStatusModel(self.main_connection).update(
            filter_data={'org_id': self.organization['id']},
            update_data={'payment_date': utcnow() - datetime.timedelta(hours=300)}
        )
        # повторный вызов функции при включении сервиса должен теперь привести
        # к появлению новой записи
        on_service_enable(self.main_connection, self.organization['id'], 'tracker', tracker['id'])

        self.assertEqual(
            TrackerBillingStatusModel(self.main_connection).filter(
                org_id=self.organization['id']
            ).count(),
            2
        )


    def test_licences_without_fired_correct(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            paid_by_license=True,
            ready_default=True,
        )
        another_user = self.create_user(nickname='test4545')

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker['slug'],
        )

        self.create_licenses_for_service(tracker['id'], department_ids=[ROOT_DEPARTMENT_ID])

        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            tracker['id'],
            last_week_date
        )

        self.post_json('/services/%s/disable/' % tracker['slug'], data={})

        UserModel(self.main_connection).dismiss(
            self.organization['id'],
            another_user['id'],
            self.user['id'],
            skip_disk=True
        )

        UserModel(self.main_connection).update_one(
            update_data={'is_dismissed': True},
            filter_data={'id': another_user['id']}
        )

        tracker_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker['id'],
            }
        )
        self.assertEqual(tracker_users, 2)

        self.post_json('/services/%s/enable/' % tracker['slug'], data={})

        tracker_users = UserServiceLicenses(self.main_connection).count(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker['id'],
            }
        )
        self.assertEqual(tracker_users, 1)

    def test_disable(self):
        # отключаем сервис от организации

        # сначала сделаем вид, что сервис включён
        # OrganizationServiceModel(self.main_connection).create(self.organization['id'], self.service['id'])
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service_slug,
        )

        self.post_json('/services/%s/disable/' % self.service_slug, data={}, expected_code=201)

        assert_that(
            OrganizationServiceModel(self.main_connection).count(
                filter_data={'org_id': self.organization['id']}
            ),
            equal_to(0)
        )

        # отключено пользователем
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'enabled': False,
                    'service_id': self.service['id'],
                },
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    disable_reason=reason.disabled_by_user,
                )
            )
        )

        # появилось событие "service_disabled"
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.service_disabled,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.service_disabled,
                    content=has_entries(
                        slug=self.service['slug'],
                        name=self.service['name'],
                    )
                )
            )
        )

    def test_unknown_action(self):
        # если неизвестное действие для сервиса, то 404
        self.post_json('/services/%s/some_action/' % self.service_slug, data={}, expected_code=422)

    def test_deputy_admin_cant_manage_services(self):
        # заместитель админа не может включать/выключать сервисы
        deputy_admin = self.create_deputy_admin(is_outer=True)
        self.post_json(
            '/services/%s/enable/' % self.service_slug,
            headers=get_auth_headers(as_uid=deputy_admin['id']),
            data={},
            expected_code=403,
        )

    @patch('intranet.yandex_directory.src.yandex_directory.core.actions.user.event_user_added')
    def test_enabled_with_robot_required(self, mock_user_event):
        # подключаем сервис с роботом для организации
        ServiceModel(self.meta_connection).update(
            update_data=dict(robot_required=True),
            filter_data=dict(slug=self.service_slug)
        )
        # смотрим, что до подключения сервиса - роботов нет
        robot_uids = OrganizationModel(self.main_connection).get_robots(self.organization['id'])
        assert_that(robot_uids, empty())

        self.post_json('/services/%s/enable/' % self.service_slug, data={})

        assert_that(mock_user_event.call_count, equal_to(1))

        # смотрим, что после подключения сервиса - роботы появились
        robot_uids = OrganizationModel(self.main_connection).get_robots(self.organization['id'])
        assert_that(robot_uids, has_length(1))

    @patch('intranet.yandex_directory.src.yandex_directory.core.utils.robots.UserListView._post_user')
    def test_enabled_without_robot_required(self, mock_post_user):
        # подключаем сервис для организации без робота

        ServiceModel(self.meta_connection).update(
            update_data=dict(robot_required=False),
            filter_data=dict(slug=self.another_service_slug)
        )
        self.post_json('/services/%s/enable/' % self.another_service_slug, data={})

        assert_that(mock_post_user.call_count, equal_to(0))

        # никаких роботов тут не должно быть
        robot_uids = OrganizationModel(self.main_connection).get_robots(self.organization['id'])
        assert_that(robot_uids, empty())

    def test_mark_service_as_ready_with_trial(self):
        # Проверим, что если сервис оплачивается по лицензиям, то после его активации,
        # устанавливается дата окончания триального периода и создается ресурс

        # Настроим платный по лицензиям сервис с триальным периодом
        self.service = ServiceModel(self.meta_connection).update(
            update_data=dict(
                ready_default=False,
                paid_by_license=True,
                trial_period_months=2,
            ),
            filter_data=dict(
                id=self.service['id'],
            ),
        )
        # Проверим, что у сервиса нет флага готовности по-умолчанию
        assert_that(
            self.service,
            has_entries(ready_default=False)
        )

        # Потом включим его для организации.
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )

        # Проверим, что сервис еще не готов
        org_service = OrganizationServiceModel(self.main_connection).find(
            filter_data={'org_id': self.organization['id']},
            one=True,
        )
        assert_that(
            org_service,
            has_entries(
                ready_at=None,
                ready=False,
            )
        )
        # но для него создался ресурс
        resource_count = ResourceModel(self.main_connection).count(filter_data={'id': org_service['resource_id']})
        self.assertEqual(resource_count, 1)

        # проверим что в ручке он не готов
        service_ready = self.get_json(
            '/services/{}/ready/'.format(self.service_slug),
            expected_code=200,
        )
        self.assertEqual(
            service_ready,
            {
                'slug': self.service_slug,
                'ready': False,
                'enabled': True,
            }
        )

        # Включаем сервис
        with self.as_service(self.service_slug, self.organization['id']), \
             frozen_time():
            self.post_json(
                '/services/{}/ready/'.format(self.service_slug),
                data={},
                expected_code=200
            )

            new_org_service = OrganizationServiceModel(self.main_connection).find(
                filter_data={'org_id': self.organization['id']},
                one=True,
            )
            # Удостоверимся, что сервис "готов"
            assert_that(
                new_org_service,
                has_entries(
                    # Признак готовности выставился в True
                    ready=True,
                )
            )

            # и мы правильно сохранили дату его включения
            assert_that(
                new_org_service,
                has_entries(
                    ready_at=utcnow(),
                )
            )

            # и окончаение триального периода
            assert_that(
                new_org_service,
                has_entries(
                    trial_expires=(utcnow() + relativedelta(months=2)).date(),
                )
            )

            # проверим что в ручке он тоже стал готов
            service_ready = self.get_json(
                '/services/{}/ready/'.format(self.service_slug),
                expected_code=200,
            )
            self.assertEqual(
                service_ready,
                {
                    'slug': self.service_slug,
                    'ready': True,
                    'enabled': True,
                }
            )

    def test_mark_service_as_ready_after_enabling(self):
        # Проверим, что если стоит признак, ready_default=True,
        # то сразу после подключения, сервис будет считаться готовым к работе

        # Проверим, что у сервиса есть флаг готовности по-умолчанию
        assert_that(
            self.service,
            has_entries(ready_default=True)
        )
        # Потом включим его для организации.
        with frozen_time():
            enable_service(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                self.service['slug'],
            )

            # Удостоверимся, что сервис "готов"
            assert_that(
                OrganizationServiceModel(self.main_connection).filter(
                    org_id=self.organization['id']
                ).all(),
                contains(
                    has_entries(
                        # Признак готовности выставился в True
                        ready=True,
                        # Время включения проставилось
                        ready_at=utcnow(),
                    )
                )
            )

    def test_mark_service_as_ready(self):
        # Проверим, что ручка установки готовности сервиса работает как надо.

        self.clean_actions_and_events()

        # Сначала пометим сервис, так, чтобы он не был "готов"
        # сразу после включения.
        ServiceModel(self.meta_connection).update(
            update_data=dict(
                ready_default=False,
            ),
            filter_data=dict(
                id=self.service['id'],
            ),
        )
        # Потом включим его для организации.
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )

        def check_status(expected):
            assert_that(
                OrganizationServiceModel(self.main_connection).find(
                    filter_data={'org_id': self.organization['id']}
                ),
                contains(
                    has_entries(
                        ready=expected
                    )
                )
            )

        def check_ready_at(expected):
            assert_that(
                OrganizationServiceModel(self.main_connection).find(
                    filter_data={'org_id': self.organization['id']}
                ),
                contains(
                    has_entries(
                        ready_at=expected
                    )
                )
            )

        # Проверим, что сервис пока не "готов".
        check_status(False)
        check_ready_at(None)

        # И наконец дернем ручку, которая включает сервис
        with self.as_service(self.service_slug, self.organization['id']) , \
             frozen_time():
            self.post_json(
                '/services/{}/ready/'.format(self.service_slug),
                data={},
                expected_code=200
            )

            # Дёрнем ручку второй раз, чтобы проверить, что второй вызов
            # будет проигнорирован и событие не заведётся:
            # https://st.yandex-team.ru/DIR-3654
            self.post_json(
                '/services/{}/ready/'.format(self.service_slug),
                data={},
                expected_code=200
            )

            # А вот теперь он должен быть готов
            check_status(True)
            check_ready_at(utcnow())

            # Проверяем, что сгенерировались нужные действия и события.
            actions = ActionModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                },
            )
            actions = only_attrs(actions, 'name')

            event_model = EventModel(self.main_connection)
            events = only_attrs(event_model.find(), 'name')
            assert_that(
                actions,
                # Действие service_set_ready должно быть одно
                contains('service_enable', 'service_set_ready')
            )
            assert_that(
                events,
                # И событие service_ready тоже должно быть одно
                contains('service_enabled', 'service_ready')
            )

    def test_mark_service_cant_mark_another_service_as_ready(self):
        # Проверим, что ручка установки готовности сервиса работает
        # только для того сервиса, от имени которого вызывается

        # Создадим "левый" сервис от имени которого будем работать
        evil_service = ServiceModel(self.meta_connection).create(
            slug='evil-service',
            name='Evil Service',
            # совершенно неважно какой тут будет client_id
            client_id=100500,
        )

        # Включим оба сервиса для организации.

        for service in [evil_service, self.service]:
            enable_service(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                service['slug'],
            )

        with self.as_service(evil_service['slug'], self.organization['id']):
            # Попробуем пойти от имени другого сервиса
            self.post_json(
                '/services/{}/ready/'.format(self.service_slug),
                data={},
                expected_code=403,
                expected_message='Access denied',
            )

    def test_mark_tracker_service_as_ready_after_trail(self):
        # Проверим, что при повтороной активации трекера после триала админам отправляются соответствующие письма
        tracker_service = ServiceModel(self.meta_connection).create(
            slug=TRACKER_SERVICE_SLUG,
            name='Tracker',
            client_id='tracker_client_id',
            ready_default=False,
        )

        # подключим трекер в триале
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker_service['slug'],
        )
        # вчера истек триал
        OrganizationServiceModel(self.main_connection).update(
            update_data={
                'trial_expires': (utcnow() - datetime.timedelta(days=1)).date()
            },
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker_service['id'],
            }
        )
        # отключим трекер
        disable_licensed_services_by_trial(self.meta_connection, self.main_connection, self.organization['id'])
        # активируем третер повторно
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker_service['slug'],
        )

        with self.as_service(TRACKER_SERVICE_SLUG, self.organization['id']), \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.service.view.send_email_to_admins') as mock_send_email_to_admins:
            self.post_json(
                '/services/{}/ready/'.format(TRACKER_SERVICE_SLUG),
                data={},
                expected_code=200
            )

        assert_called_once(
            mock_send_email_to_admins,
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            campaign_slug=app.config['SENDER_CAMPAIGN_SLUG']['TRACKER_SUBSCRIPTION_ACTIVATED_EMAIL'],
            organization_name=self.organization['name'],
        )

    @override_mailer()
    def test_cloud_notify(self):
        # Проверим, что для облачных организаций мы шлём облачные письма через облачный сервис
        OrganizationModel(self.main_connection).change_organization_type(
            org_id=self.organization['id'],
            new_org_type=organization_type.cloud,
        )
        UserModel(self.main_connection).update(filter_data={'id': self.admin_uid}, update_data={'cloud_uid': 'wow-1'})

        tracker_service = ServiceModel(self.meta_connection).create(
            slug=TRACKER_SERVICE_SLUG,
            name='Tracker',
            client_id='tracker_client_id',
            ready_default=False,
        )

        # подключим трекер в триале
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker_service['slug'],
        )
        # вчера истек триал
        OrganizationServiceModel(self.main_connection).update(
            update_data={
                'trial_expires': (utcnow() - datetime.timedelta(days=1)).date()
            },
            filter_data={
                'org_id': self.organization['id'],
                'service_id': tracker_service['id'],
            }
        )
        # отключим трекер
        disable_licensed_services_by_trial(self.meta_connection, self.main_connection, self.organization['id'])
        # активируем третер повторно
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker_service['slug'],
        )

        with self.as_service(TRACKER_SERVICE_SLUG, self.organization['id']), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mailer.cloud_notify.client.Notifier.notify') as mocked_notify:
            self.post_json(
                '/services/{}/ready/'.format(TRACKER_SERVICE_SLUG),
                data={},
                expected_code=200
            )


        assert len(mocked_notify.call_args_list) == 1
        notify_call = mocked_notify.call_args_list[0]
        notify_call_kwargs = notify_call[1]
        assert notify_call_kwargs['organization']['id'] == self.organization['id']
        assert notify_call_kwargs['template_name'] == 'tracker.access.welcome-admin'
        assert notify_call_kwargs['cloud_uid'] == 'wow-1'
        expected = {
            'tld': 'ru',
            'lang': 'ru',
            'domain': 'not_yandex_test.ws.autotest.yandex.ru',
            'organization_name': {'ru': 'Яндекс'}
        }
        assert notify_call_kwargs['params'] == expected

    def test_not_disable_service_after_trial_in_partner_org(self):
        OrganizationModel(self.main_connection).change_organization_type(
            org_id=self.organization['id'],
            new_org_type=organization_type.partner_organization,
            partner_id=self.partner['id'],
        )

        OrganizationServiceModel(self.main_connection).update_one(
            id=self.paid_service['id'],
            update_data={
                'trial_status': trial_status.expired,
                'trial_expires': (utcnow() - relativedelta(days=1)).date(),
            },
        )

        disabled_services = disable_licensed_services_by_trial(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            org_id=self.organization['id'],
        )

        assert_that(
            disabled_services,
            empty(),
        )

    def test_not_disable_disk_after_trial(self):
        disk_service = ServiceModel(self.meta_connection).create(
            slug='disk',
            name='disk',
            client_id='disk_client_id',
            ready_default=True,
            paid_by_license=True,
        )

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            disk_service['slug'],
        )

        OrganizationServiceModel(self.main_connection).filter(
            org_id=self.organization['id'],
            service_id=disk_service['id'],
        ).update(trial_status='expired')

        org_service = OrganizationServiceModel(self.main_connection).filter(
            org_id=self.organization['id'],
            service_id=disk_service['id']
        ).one()

        assert_that(
            org_service,
            has_entries(
                enabled=True,
                trial_status='expired',
            ),
        )

        disabled_services = disable_licensed_services_by_trial(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            org_id=self.organization['id'],
        )

        # диск не выключился
        assert_that(
            OrganizationServiceModel(self.main_connection). \
                filter(org_id=self.organization['id'], service_id=disk_service['id']). \
                scalar('enabled')[0],
            equal_to(True),
        )

    def test_mark_licensed_service_as_ready_after_reenabling(self):
        # проверяем, что при повторном включении лицензионного сервиса дата окончания триального периода не обновляется

        self.service = ServiceModel(self.meta_connection).update(
            update_data=dict(
                ready_default=False,
                paid_by_license=True,
                trial_period_months=2,
            ),
            filter_data=dict(
                id=self.service['id'],
            ),
        )

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )
        # помечаем его как ready, чтобы проствилась дата окончания триального периода

        with frozen_time():

            trial_expired_date = (utcnow() + relativedelta(months=2)).date()

            with self.as_service(self.service_slug, self.organization['id']) :
                self.post_json(
                    '/services/{}/ready/'.format(self.service_slug),
                    data={},
                    expected_code=200
                )

            assert_that(
                OrganizationServiceModel(self.main_connection).find(
                    filter_data={
                        'org_id': self.organization['id'],
                        'service_id': self.service['id'],
                    },
                    one=True,
                ),
                has_entries(
                    trial_expires=trial_expired_date,
                )
            )
            # выключаем сервис
            self.post_json(
                '/services/{}/disable/'.format(self.service_slug),
                data={},
                expected_code=201,
            )
            # проверим, что дата окончания триального периода не поменялась
            assert_that(
                OrganizationServiceModel(self.main_connection).find(
                    filter_data={
                        'org_id': self.organization['id'],
                        'service_id': self.service['id'],
                        'enabled': Ignore,
                    },
                    one=True
                ),
                has_entries(
                    trial_expires=trial_expired_date,
                    disable_reason=reason.disabled_by_user,
                )
            )
            # включаем в организации платный режим, чтобы появилась биллинговая информация
            self.enable_paid_mode()

            # повторно включаем сервис
            enable_service(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                self.service['slug'],
            )
            assert_that(
                OrganizationServiceModel(self.main_connection).find(
                    filter_data={
                        'org_id': self.organization['id'],
                        'service_id': self.service['id'],
                    },
                    one=True,
                ),
                has_entries(
                    ready=False,
                    ready_at=None,
                    disable_reason=None,
                    disabled_at=None,
                )
            )
            with self.as_service(self.service_slug, self.organization['id']) :
                self.post_json(
                    '/services/{}/ready/'.format(self.service_slug),
                    data={},
                    expected_code=200
                )
            # проверяем, что триал не поменялся и сервис готов
            assert_that(
                OrganizationServiceModel(self.main_connection).find(
                    filter_data={
                        'org_id': self.organization['id'],
                        'service_id': self.service['id'],
                    },
                    one=True,
                ),
                has_entries(
                    trial_expires=trial_expired_date,
                    ready=True,
                    ready_at=utcnow(),
                )
            )

    def test_reenable_service_with_drop_licenses(self):
        # проверяем, что если при повторном включении платного сервиса передан параметр drop_licenses=True,
        # то существующие линцезии удалятся
        paid_service = ServiceModel(self.meta_connection).create(
            slug=TRACKER_SERVICE_SLUG,
            name='Tracker',
            client_id='tracker_client_id',
            ready_default=True,
            paid_by_license=True,
            trial_period_months=2,
        )

        self.post_json('/services/%s/enable/' % paid_service['slug'], data={}, expected_code=201)

        assert_that(
            OrganizationServiceModel(self.main_connection).find(),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    service_id=paid_service['id']
                )
            )
        )
        assert_that(
            UserServiceLicenses(self.main_connection).find(),
            equal_to([])
        )

        # выдаем лицензии
        self.put_json(
            '/subscription/services/%s/licenses/' % paid_service['slug'],
            data=[
                {
                    'type': 'user',
                    'id': self.admin_uid
                }
            ],
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        assert_that(
            UserServiceLicenses(self.main_connection).find(),
            has_length(1)
        )
        # выключаем сервис и включаем повторно с параметром drop_licenses
        self.post_json('/services/%s/disable/' % paid_service['slug'], data={}, expected_code=201)
        self.post_json('/services/%s/enable/' % paid_service['slug'], data={'drop_licenses': False}, expected_code=201)
        assert_that(
            UserServiceLicenses(self.main_connection).find(),
            has_length(1)
        )

        self.post_json('/services/%s/disable/' % paid_service['slug'], data={}, expected_code=201)
        self.post_json('/services/%s/enable/' % paid_service['slug'], data={'drop_licenses': True}, expected_code=201)

        # проверяем, что лицензии удалились
        assert_that(
            UserServiceLicenses(self.main_connection).find(),
            equal_to([])
        )


class TestServicesListView(TestCase):
    def setUp(self):
        super(TestServicesListView, self).setUp()

        # добавим несколько сервисов
        self.good_service_tld_data = {
            'com': {
                'url': 'http://slug.example.yandex.com',
                'icon': 'http://icons.example.yandex.com/slug-com.png',
            },
            'ru': {
                'url': 'http://slug.example.yandex.ru',
                'icon': 'http://icons.example.yandex.com/slug-ru.png',
            }
        }
        self.good_service_language_data = {
            'ru': {
                'name': 'Название сервиса',
            },
            'en': {
                'name': 'Service name',
            }
        }

        # создадим services_links
        # у services_links и сервиса не настроены data_by_tld
        ServicesLinksModel(self.meta_connection).create(
            slug='no_data_by_tld',
            data_by_language=self.good_service_language_data,
        )
        ServiceModel(self.meta_connection).create(
            slug='no_data_by_tld',
            name='Не указаны данные специфичные для tld',
            client_id='client_id1',
        )

        # у services_links и сервиса не настроены data_by_language
        ServicesLinksModel(self.meta_connection).create(
            slug='no_data_by_language',
            data_by_tld=self.good_service_tld_data,
        )
        ServiceModel(self.meta_connection).create(
            slug='no_data_by_language',
            name='Не указаны названия',
            client_id='client_id2',
        )
        self.good_service_slug = 'good_service_slug'

        # правильно настроенный сервис
        ServicesLinksModel(self.meta_connection).create(
            slug=self.good_service_slug,
            data_by_tld=self.good_service_tld_data,
            data_by_language=self.good_service_language_data,
        )
        ServiceModel(self.meta_connection).create(
            slug=self.good_service_slug,
            name='Указаны все метаданные для сервиса',
            client_id='client_id3',
        )
        self.url_pattern = '/services/?tld={tld}&language={language}'

    def test_get_services(self):
        # получаем список сервисов
        url = self.url_pattern.format(tld='ru', language='en')
        data = self.get_json(url)

        # данные есть только для сервиса с полностью заполненными данными для tld и языков
        assert_that(
            data,
            has_entry(
                self.good_service_slug,
                has_entries(
                    url=self.good_service_tld_data['ru']['url'],
                    icon=self.good_service_tld_data['ru']['icon'],
                    name=self.good_service_language_data['en']['name'],
                    in_new_tab=False,
                    available=True,
                )
            )
        )

    def test_fallback_rkub_tld_to_ru(self):
        # получаем список сервисов с указанием tld из РКУБ
        # для ua нет специальных настроек
        url = self.url_pattern.format(tld='ua', language='en')
        data = self.get_json(url)

        # сфолбечились на настройки для ru
        assert_that(
            data,
            has_entry(
                self.good_service_slug,
                has_entries(
                    url=self.good_service_tld_data['ru']['url'],
                    icon=self.good_service_tld_data['ru']['icon'],

                )
            )
        )

    def test_fallback_any_tld_to_com(self):
        # получаем список сервисов с указанием произвольного tld (не РКУБ)
        # для de нет специальных настроек
        url = self.url_pattern.format(tld='de', language='en')
        data = self.get_json(url)

        # сфолбечились на настройки для com
        assert_that(
            data,
            has_entry(
                self.good_service_slug,
                has_entries(
                    url=self.good_service_tld_data['com']['url'],
                    icon=self.good_service_tld_data['com']['icon'],
                )
            )
        )

    def test_fallback_language(self):
        # получаем список сервисов с указанием произвольного языка
        # для de нет специальных настроек
        url = self.url_pattern.format(tld='com', language='de')
        data = self.get_json(url)

        # сфолбечились на язык организации
        org_lang = self.organization['language']  # в тестах это ru
        assert_that(
            data,
            has_entry(
                self.good_service_slug,
                has_entries(
                    name=self.good_service_language_data[org_lang]['name'],
                )
            )
        )

    def test_tracker_available(self):
        # Проверяем, что трекер доступен для подключения

        ServicesLinksModel(self.meta_connection).create(
            slug=TRACKER_SERVICE_SLUG,
            data_by_tld=self.good_service_tld_data,
            data_by_language=self.good_service_language_data,
        )

        ServiceModel(self.meta_connection).create(
            slug=TRACKER_SERVICE_SLUG,
            name='Tracker',
            client_id='client_tracker',
        )

        # получаем список сервисов
        url = self.url_pattern.format(tld='ru', language='en')
        data = self.get_json(url)

        assert_that(
            data,
            has_entry(
                TRACKER_SERVICE_SLUG,
                has_entries(
                    available=True  # трекер доступен
                )
            )
        )


class TestServicesLicensesChangeEvents(TestCase):
    def setUp(self):
        super(TestServicesLicensesChangeEvents, self).setUp()
        self.token_auth_header = get_auth_headers(as_uid=self.admin_uid)
        self.service = ServiceModel(self.meta_connection).create(
            name='service',
            slug='tracker',
            client_id=123,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            service_slug=self.service['slug'],
        )
        self.enable_paid_mode()

    def test_direct_group_membership_changed(self):
        # проверим, что изменение состава группы, у которой есть лицензии, генерит событие service_license_changed
        group = self.create_group()
        user1 = self.create_user()

        # выдаем группе лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group['id'],
                }
            ],
            headers=self.token_auth_header,
        )
        # в группе пока никого нет, таблица лицензий пустая
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 0)

        licenses_log = TrackerLicenseLogModel(self.meta_connection).find()
        self.assertEqual(len(licenses_log), 0)
        members = [
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'user',
                'id': user1['id']
            },
        ]
        EventModel(self.main_connection).delete(force_remove_all=True)
        # добавляем пользователей в группу
        self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'members': members
            },
        )
        # таблица лицензий обновилась
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 2)

        license_events = EventModel(self.main_connection).find(filter_data={'name': event.service_license_changed})
        assert_that(
            len(license_events),
            equal_to(1)
        )
        licenses_log = TrackerLicenseLogModel(self.meta_connection).find()
        self.assertEqual(len(licenses_log), 2)
        licenses_log_user_1 = TrackerLicenseLogModel(self.meta_connection).filter(
            uid=user1['id']
        ).fields('uid', 'org_id', 'action').one()
        self.assertEqual(licenses_log_user_1['uid'], user1['id'])
        self.assertEqual(licenses_log_user_1['org_id'], self.organization['id'])
        self.assertEqual(licenses_log_user_1['action'], 'add')

        members = [
            {
                'type': 'user',
                'id': user1['id']
            },
        ]
        EventModel(self.main_connection).delete(force_remove_all=True)
        self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'members': members
            },
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 1)

        licenses_log = TrackerLicenseLogModel(self.meta_connection).find()
        self.assertEqual(len(licenses_log), 3)
        licenses_log_user_add = TrackerLicenseLogModel(self.meta_connection).filter(
            uid=self.user['id'], action='add'
        ).count()
        self.assertEqual(licenses_log_user_add, 1)
        licenses_log_user_delete = TrackerLicenseLogModel(self.meta_connection).filter(
            uid=self.user['id'], action='delete'
        ).count()
        self.assertEqual(licenses_log_user_delete, 1)


    def test_subgroup_membership_changed(self):
        # проверим, что изменение состава подгруппы, у которой есть лицензии через членство в других группах,
        # генерит событие service_license_changed
        group1 = self.create_group()
        group2 = self.create_group(members=[
            {
                'type': 'group',
                'object': group1
            },
        ])
        group3 = self.create_group(members=[
            {
                'type': 'group',
                'object': group2
            },
        ])
        department = self.create_department()

        # выдаем группе3 лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group3['id']
                }
            ],
            headers=self.token_auth_header,
        )
        # в группах пока нет пользователей, таблица лицензий пустая
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 0)
        members = [
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'department',
                'id': department['id']
            },
        ]
        EventModel(self.main_connection).delete(force_remove_all=True)
        # обновляем состав группы1
        self.patch_json(
            '/groups/%s/' % group1['id'],
            data={
                'members': members
            }
        )

        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 1)

        license_events = EventModel(self.main_connection).find(filter_data={'name': event.service_license_changed})
        assert_that(
            len(license_events),
            equal_to(1)
        )

    def test_direct_department_membership_changed(self):
        # проверим, что изменение состава отдела, у которого есть лицензии, генерит событие service_license_changed
        department = self.create_department()
        user1 = self.create_user()

        # выдаем отделу лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'department',
                    'id': department['id'],
                }
            ],
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 0)

        EventModel(self.main_connection).delete(force_remove_all=True)
        # добавляем пользователя в отдел
        self.patch_json(
            '/users/%s/' % user1['id'],
            data={
                'department': {'id': department['id']}
            },
            expected_code=200
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 1)

        license_events = EventModel(self.main_connection).find(filter_data={'name': event.service_license_changed})
        assert_that(
            len(license_events),
            equal_to(1)
        )

    def test_subdepartment_membership_changed(self):
        # проверим, что изменение состава отдела, у которого есть лицензии через родительский отдел,
        # генерит событие service_license_changed
        department = self.create_department()
        another_department = self.create_department(parent_id=department['id'])
        user1 = self.create_user()

        # выдаем родительскому отделу лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'department',
                    'id': department['id'],
                }
            ],
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 0)

        EventModel(self.main_connection).delete(force_remove_all=True)
        # добавляем пользователя в отдел another_department
        self.patch_json(
            '/users/%s/' % user1['id'],
            data={
                'department': {'id': another_department['id']}
            },
            expected_code=200
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 1)

        license_events = EventModel(self.main_connection).find(filter_data={'name': event.service_license_changed})
        assert_that(
            len(license_events),
            equal_to(1)
        )

    def test_group_department_membership_changed(self):
        # проверим, что добавление пользователя в отдел, который находится в группе с выданными лицензиями,
        # генерит событие service_license_changed
        department = self.create_department()

        # создаем группу с департаментом внутри
        members = [
            {'type': 'department', 'id': department['id']},
        ]
        data = {
            'name': {'ru': 'some_group_name'},
            'label': 'group_label',
            'description': {'ru': 'some_group'},
            'members': members,
        }
        response_data = self.post_json('/groups/', data)
        user1 = self.create_user()

        # выдаем группе лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': response_data['id']
                }
            ],
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 1)  # у админа группы есть лицензии
        EventModel(self.main_connection).delete(force_remove_all=True)
        # добавляем пользователя в отдел
        self.patch_json(
            '/users/%s/' % user1['id'],
            data={
                'department': {'id': department['id']}
            },
            expected_code=200
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 2)

        license_events = EventModel(self.main_connection).find(filter_data={'name': event.service_license_changed})
        assert_that(
            len(license_events),
            equal_to(1)
        )

    def test_department_moved(self):
        # проверим, что перемещение подотдела из отдела с лицензиями в отдел без них
        # вызывает событие service_license_changed, лицензии выданные непосредственно на перемещаемый
        # отдел при этом не меняются
        department1 = self.create_department()
        department2 = self.create_department(parent_id=department1['id'])
        department3 = self.create_department()

        self.create_user(department_id=department2['id'])
        # создаем группу с департаментом внутри
        members = [
            {'type': 'department', 'id': department1['id']},
        ]
        data = {
            'name': {'ru': 'some_group_name'},
            'label': 'group_label',
            'description': {'ru': 'some_group'},
            'members': members,
        }
        response_data = self.post_json('/groups/', data)

        # выдаем группе лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': response_data['id']
                }
            ],
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 2)  # админ группы и пользователь внутри отдела 2

        # создаем еще один сервис и выдаем отделу 2 лицензии на него
        another_service = ServiceModel(self.meta_connection).create(
            name='another_service',
            slug='another_service',
            client_id=123456,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            service_slug=another_service['slug'],
        )
        self.put_json(
            '/subscription/services/%s/licenses/' % another_service['slug'],
            data=[
                {
                    'type': 'department',
                    'id': department2['id']
                }
            ],
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 3)  # у пользователя внутри отдел есть доступ к двум сервисам

        EventModel(self.main_connection).delete(force_remove_all=True)
        # перемещаем отдел 2 в отдел 3, у которого нет лицензий
        self.patch_json('/departments/%d/' % department2['id'], data={'parent_id': department3['id']})
        license_events = EventModel(self.main_connection).find(filter_data={'name': event.service_license_changed})
        assert_that(
            len(license_events),
            equal_to(1)
        )
        assert_that(
            license_events[0]['object'],
            equal_to(self.service)
        )

        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 2)  # у пользователя внутри отдлеа пропал доступ к сервису для группы

    def test_user_dismissed_from_group(self):
        # проверим, что увольнение пользователя из группы с лицензиями
        # вызывает событие service_license_changed
        user = self.create_user()
        members = [
            {'type': 'user', 'id': user['id']},
        ]
        data = {
            'name': {'ru': 'some_group_name'},
            'label': 'group_label',
            'description': {'ru': 'some_group'},
            'members': members,
        }
        response_data = self.post_json('/groups/', data)

        members = [
            {'type': 'group', 'id': response_data['id']},
        ]
        data = {
            'name': {'ru': 'some_group_name2'},
            'label': 'group_label2',
            'description': {'ru': 'some_group2'},
            'members': members,
        }
        response_data = self.post_json('/groups/', data)

        # выдаем группе лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': response_data['id']
                }
            ],
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 2)  # у админа, который создает группы тоже есть доступ

        EventModel(self.main_connection).delete(force_remove_all=True)
        # увольняем пользователя
        self.patch_json(
            '/users/%s/' % user['id'],
            data={'is_dismissed': True},
            return_headers=True,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 1)  # лицензия остается только у админа

        license_events = EventModel(self.main_connection).find(filter_data={'name': event.service_license_changed})
        assert_that(
            len(license_events),
            equal_to(2)     # событие генерится для каждой группы, в которой находится пользователь
        )

    def test_user_dismissed_from_department(self):
        # проверим, что увольнение пользователя из отдела с лицензиями
        # вызывает событие service_license_changed
        department = self.create_department()
        department2 = self.create_department(parent_id=department['id'])
        user = self.create_user(department_id=department2['id'])

        # выдаем отделу лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'department',
                    'id': department['id']
                }
            ],
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 1)
        EventModel(self.main_connection).delete(force_remove_all=True)
        # увольняем пользователя
        self.patch_json(
            '/users/%s/' % user['id'],
            data={'is_dismissed': True},
            return_headers=True,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 0)

        license_events = EventModel(self.main_connection).find(filter_data={'name': event.service_license_changed})
        assert_that(
            len(license_events),
            equal_to(1)
        )

    def test_user_with_direct_license_dismissed(self):
        # проверим, что увольнение пользователя с выданной напрямую лицензией
        # вызывает событие service_license_changed
        user = self.create_user()

        # выдаем пользователю лицензии на сервис
        self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'user',
                    'id': user['id']
                }
            ],
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 1)
        EventModel(self.main_connection).delete(force_remove_all=True)
        # увольняем пользователя
        self.patch_json(
            '/users/%s/' % user['id'],
            data={'is_dismissed': True},
            return_headers=True,
        )
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 0)

        license_events_count = EventModel(self.main_connection).count(
            filter_data={'name': event.service_license_changed}
        )
        assert_that(
            license_events_count,
            equal_to(1)
        )


class TestOrganizationServiceDetail(TestCase):
    def setUp(self):
        super(TestOrganizationServiceDetail, self).setUp()
        self.service_slug = 'autotest-service'
        self.service_name = 'Autotest service'
        self.service = ServiceModel(self.meta_connection).create(
            slug=self.service_slug,
            name=self.service_name,
            client_id='client_id',
        )

        self.organization_service = OrganizationServiceModel(self.main_connection).create(
            self.organization['id'],
            self.service['id'],
            ready=True,
        )
        self.user = self.create_user(org_id=self.organization['id'], nickname='new')

        ActionModel(self.main_connection).delete(force_remove_all=True)
        EventModel(self.main_connection).delete(force_remove_all=True)

    def test_change_responsible(self):
        organization_service = OrganizationServiceModel(self.main_connection).get_by_slug(
            org_id=self.organization['id'], service_slug=self.service_slug,
            fields=['**'],
        )
        self.assertIsNone(organization_service['responsible_id'])

        self.patch_json(
            '/services/%s/' % self.service_slug,
            data={'responsible_id': self.user['id']},
            expected_code=200,
        )
        organization_service = OrganizationServiceModel(self.main_connection).get_by_slug(
            org_id=self.organization['id'], service_slug=self.service_slug,
            fields=['**'],
        )

        self.assertEqual(organization_service['responsible_id'], self.user['id'])

        events = EventModel(self.main_connection).filter(
                org_id=self.organization['id'],
                name=event.service_responsible_changed,
        ).fields('org_id', 'name', 'object')
        self.assertEqual(len(events), 1)
        self.assertEqual(
            events[0],
            {
                'object': {'id': organization_service['id'], 'responsible_id': self.user['id']},
                'org_id': self.organization['id'], 'id': events[0]['id'],
                'name': event.service_responsible_changed,
            }
        )

        actions = ActionModel(self.main_connection).filter(
            org_id=self.organization['id']
        )
        self.assertEqual(len(actions), 1)
        self.assertEqual(actions[0]['name'], 'service_responsible_change')

    def test_change_responsible_fail_not_in_org(self):
        self.another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        self.not_org_user = self.create_user(org_id=self.another_organization['id'], nickname='from_other_org')
        OrganizationServiceModel(self.main_connection).create(
            self.another_organization['id'],
            self.service['id'],
            ready=True,
        )

        organization_service = OrganizationServiceModel(self.main_connection).get_by_slug(
            org_id=self.organization['id'], service_slug=self.service_slug,
            fields=['**'],
        )
        self.assertIsNone(organization_service['responsible_id'])

        self.patch_json(
            '/services/%s/' % self.service_slug,
            data={'responsible_id': self.not_org_user['id']},
            expected_code=422,
        )
        organization_service = OrganizationServiceModel(self.main_connection).get_by_slug(
            org_id=self.organization['id'], service_slug=self.service_slug,
            fields=['**'],
        )

        self.assertIsNone(organization_service['responsible_id'])

    def test_change_relations_success(self):
        another_user = self.create_user(org_id=self.organization['id'], nickname='another_user')
        service_slug = 'metrika'
        service = ServiceModel(self.meta_connection).create(
            slug=service_slug,
            name='Metrika',
            client_id='client_id_metrika',
        )

        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'],
            service['id'],
            ready=True,
        )
        resource1 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            external_id='test1',
            relations=[
                {
                    'name': 'own',
                    'user_id': another_user['id'],
                }
            ]
        )
        resource2 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service_slug,
            external_id='test2',
            relations=[
                {
                    'name': 'read',
                    'user_id': another_user['id'],
                }
            ]
        )

        another_service = ServiceModel(self.meta_connection).create(
            slug='some_slug',
            name='some test',
            client_id='client_id2',
        )
        resource3 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=another_service['slug'],
            external_id='test3',
            relations=[
                {
                    'name': 'own',
                    'user_id': another_user['id'],
                }
            ]
        )

        self.patch_json(
            '/services/%s/' % service_slug,
            data={'responsible_id': self.user['id']},
            expected_code=200,
        )
        organization_service = OrganizationServiceModel(self.main_connection).get_by_slug(
            org_id=self.organization['id'], service_slug=service_slug,
            fields=['**'],
        )

        self.assertEqual(organization_service['responsible_id'], self.user['id'])

        assert_that(
            ActionModel(self.main_connection).filter(
                org_id=self.organization['id'],
                name=action.resource_modify
            ).count(),
            equal_to(2)
        )

        resource1_relations = ResourceModel(self.main_connection).get_relations(
            resource_id=resource1['id'],
            org_id=self.organization['id'],
        )
        assert_that(resource1_relations), equal_to(1)
        assert_that(
            resource1_relations[0],
            has_entries(
                user_id=self.user['id'],
                name='own',
            )
        )

        resource2_relations = ResourceModel(self.main_connection).get_relations(
            resource_id=resource2['id'],
            org_id=self.organization['id'],
        )
        assert_that(resource1_relations), equal_to(1)
        assert_that(
            resource2_relations[0],
            has_entries(
                user_id=another_user['id'],
                name='read',
            )
        )

        resource3_relations = ResourceModel(self.main_connection).get_relations(
            resource_id=resource3['id'],
            org_id=self.organization['id'],
        )
        assert_that(resource1_relations), equal_to(1)
        assert_that(
            resource3_relations[0],
            has_entries(
                user_id=another_user['id'],
                name='own',
            )
        )


class TestServiceRolesGet(TestCase):
    def test_get_roles_for_non_existent_service(self):
        self.get_json('/services/%s/roles/' % 'non_existent_service', expected_code=404)


class TestAliceRolesGet(TestCase):
    def setUp(self):
        super(TestAliceRolesGet, self).setUp()

    def test_get_roles(self):
        from intranet.yandex_directory.src.yandex_directory.connect_services.roles import ALICE_ROLES
        response = self.get_json('/services/{}/roles/'.format('alice_b2b'))

        assert_that(
            response,
            equal_to(
                {'data': ALICE_ROLES},
            )
        )


class TestMetrikaRolesGet(TestCase):
    def setUp(self):
        super(TestMetrikaRolesGet, self).setUp()

        self.roles_without_monetization = [
            {
                'slug': 'edit',
                'name': {
                    'ru': 'Редактирование',
                    'en': 'Editing',
                },
            },
            {
                'slug': 'view',
                'name': {
                    'ru': 'Просмотр',
                    'en': 'Read',
                },
            },
        ]

        self.all_roles = [
            {
                'slug': 'view_monetization',
                'name': {
                    'ru': 'Просмотр с монетизацией',
                    'en': 'Read with monetization',
                },
            },
        ] + self.roles_without_monetization

    def test_get_all_roles(self):
        tvm.tickets['metrika'] = 'tvm-ticket-metrika'

        with patch(
            'intranet.yandex_directory.src.yandex_directory.connect_services.metrika.client.client.app.requests.get'
        ) as request_get:
            mocked_response = Response()
            mocked_response.status_code = 200
            mocked_response._content = b'{"1":{"has_monetization":true}}'

            request_get.return_value = mocked_response

            expected = {
                'data': self.all_roles
            }

            response = self.get_json('/services/{}/roles/?resource_id=1'.format('metrika'))

        assert_that(
            response,
            equal_to(
                expected
            )
        )

    def test_get_roles_without_monetization(self):
        tvm.tickets['metrika'] = 'tvm-ticket-metrika'

        with patch(
                'intranet.yandex_directory.src.yandex_directory.connect_services.metrika.client.client.app.requests.get'
        ) as request_get:
            mocked_response = Response()
            mocked_response.status_code = 200
            mocked_response._content = b'{"1":{"has_monetization":false}}'

            request_get.return_value = mocked_response

            expected = {
                'data': self.roles_without_monetization
            }

            response = self.get_json('/services/{}/roles/?resource_id=1'.format('metrika'))

        assert_that(
            response,
            equal_to(
                expected
            )
        )


class TestOrganizationTrackerDetailLicensesView(TestCase):
    def setUp(self):
        super(TestOrganizationTrackerDetailLicensesView, self).setUp()

        service = ServiceModel(self.meta_connection).create(
            name='tracker',
            slug='tracker',
            client_id=123,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug'],
        )

    def test_get_correct(self):
        response = self.get_json(
            '/subscription/services/tracker/licenses/detail'
        )
        assert response['users'] == 1
