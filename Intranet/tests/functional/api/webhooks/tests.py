# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    has_entries,
    not_none,
    none,
    contains,
    contains_inanyorder,
)

from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.core.events import event

from intranet.yandex_directory.src.yandex_directory.core.models import (
     ServiceModel,
     WebHookModel,
)


from testutils import (
    get_oauth_headers,
    get_auth_headers,
    create_organization,
    TestCase,
    set_auth_uid,
    oauth_success,
    OAUTH_CLIENT_ID,
)


class TestWebhookListView__post(TestCase):

    def setUp(self):
        super(TestWebhookListView__post, self).setUp()

        self.second_organization = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            label='label'
        )['organization']
        WebHookModel(self.meta_connection).delete(force_remove_all=True)

        self.valid_params = {
            'url': 'http://example.yandex.ru/webhook',
            'event_names': [
                event.department_deleted,
                event.department_added,
            ],
            'fields_filter': {
                'org_id': self.organization['id'],
                'service': 'yamb',
                'foo': 'bar',
            },
            'expand_content': True,
            'tvm_client_id': 100500,
        }
        # выполняем запросы без заголовка X-UID
        set_auth_uid(None)
        self.headers = get_oauth_headers()

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.write_webhooks])
    def test_simple(self):
        # создаем web hook

        result = self.post_json('/webhooks/', self.valid_params, headers=self.headers)
        assert_that(
            result,
            has_entries(
                id=not_none(),
                **self.valid_params
            )
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.write_webhooks])
    def test_deduplication_update(self):
        # создаем web hook
        result = self.post_json('/webhooks/', self.valid_params, headers=self.headers)
        assert_that(
            result['event_names'],
            contains_inanyorder(*self.valid_params['event_names']),
        )
        self.assertEqual(
            WebHookModel(self.meta_connection).get(result['id'])['event_names'],
            self.valid_params['event_names'],
        )
        old_params = self.valid_params['event_names']
        self.valid_params['event_names'] = [event.department_alias_added]

        # создаем вебхук с таким же сервисом и урлом, но другим набором событий

        result_deduplication = self.post_json('/webhooks/', self.valid_params, headers=self.headers)

        self.valid_params['event_names'].extend(old_params)

        # ожидаем что в результате вернется тот же вебхук, но с расширенным набором
        assert_that(
            result_deduplication['event_names'],
            contains_inanyorder(*self.valid_params['event_names']),
        )

        assert_that(
            WebHookModel(self.meta_connection).get(result['id'])['event_names'],
            contains_inanyorder(*self.valid_params['event_names']),
        )

        self.assertEqual(WebHookModel(self.meta_connection).count(), 1)
        self.assertEqual(result_deduplication['id'], result['id'])

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.write_webhooks])
    def test_deduplication(self):
        # создаем web hook
        result = self.post_json('/webhooks/', self.valid_params, headers=self.headers)
        assert_that(
            result,
            has_entries(
                id=not_none(),
                **self.valid_params
            )
        )

        # создаем еще один вебхук с таким же набором событий

        result_deduplication = self.post_json('/webhooks/', self.valid_params, headers=self.headers)
        assert_that(
            result_deduplication,
            has_entries(
                id=not_none(),
                **self.valid_params
            )
        )

        self.assertEqual(WebHookModel(self.meta_connection).count(), 1)
        self.assertEqual(result_deduplication['id'], result['id'])

    def test_hard_token(self):
        # пытаемся добавить подписку авторизуясь обычным токеном

        self.post_json(
            '/webhooks/',
            self.valid_params,
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=403
        )


class TestWebhookListView__get(TestCase):

    def setUp(self):
        super(TestWebhookListView__get, self).setUp()
        # выполняем запросы без заголовка X-UID
        set_auth_uid(None)
        WebHookModel(self.meta_connection).delete(force_remove_all=True)

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.write_webhooks, scope.read_webhooks])
    def test_get(self):
        # получаем список webhooks

        # добавим подписку для сервиса
        webhook = WebHookModel(self.meta_connection).create(
            url='http://example.yandex.com/webhook',
            service_id=self.service['id'],
            event_names=[event.department_added],
            fields_filter={'org_id': self.organization['id']}
        )

        result = self.get_json('/webhooks/', headers=get_oauth_headers(as_anonymous=True))
        # получим ее в ручке
        assert_that(
            result,
            has_entries(
                result=contains(
                    has_entries(
                        **webhook
                    )
                )
            )
        )

class TestWebhookDetailView__delete(TestCase):
    def setUp(self):
        super(TestWebhookDetailView__delete, self).setUp()
        # выполняем запросы  без заголовка X-UID
        set_auth_uid(None)
        WebHookModel(self.meta_connection).delete(force_remove_all=True)

        # добавим подписку для сервиса
        self.my_webhook = WebHookModel(self.meta_connection).create(
            url='http://example.yandex.ru/webhook',
            service_id=self.service['id'],
            event_names=[event.department_added],
            fields_filter={'org_id': self.organization['id']}
        )

        # добавим подписку для другого сервиса
        other_service = ServiceModel(self.meta_connection).create(
            slug='other-service-slug',
            name='Other Name',
            client_id='other_client_id',
        )
        self.other_webhook = WebHookModel(self.meta_connection).create(
            url='http://example.yandex.ru/webhook',
            service_id=other_service['id'],
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.write_webhooks, scope.read_webhooks])
    def test_delete_success(self):
        # удалим свою подписку

        self.delete_json('/webhooks/%s/' % self.my_webhook['id'], headers=get_oauth_headers(as_anonymous=True))

        # подписка удалилась
        assert_that(
            WebHookModel(self.meta_connection).get(self.my_webhook['id']),
            none()
        )

    @oauth_success(OAUTH_CLIENT_ID, scopes=[scope.write_webhooks, scope.read_webhooks])
    def test_delete_other(self):
        # удаляем чужую подписку

        self.delete_json(
            '/webhooks/%s/' % self.other_webhook['id'],
            headers=get_oauth_headers(as_anonymous=True),
            expected_code=404
        )

        # чужая подписка не удалилась
        assert_that(
            WebHookModel(self.meta_connection).get(self.other_webhook['id']),
            not_none()
        )
