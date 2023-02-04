# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    contains,
    has_entries,
    has_length,
)

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.core.models.service import ServiceModel
from intranet.yandex_directory.src.yandex_directory.core.models.webhook import WebHookModel
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory import app


class TestWebHookModel(TestCase):

    def setUp(self):
        super(TestWebHookModel, self).setUp()
        self.client_id = 'clientid'
        self.service = ServiceModel(self.meta_connection).create(
            slug='slug',
            name='Name',
            client_id=self.client_id,
        )
        WebHookModel(self.meta_connection).delete(force_remove_all=True)

        self.create_params = {
            'url': 'http://someurl.com',
            'service_id': self.service['id'],
            'event_names': [],
            'fields_filter': {'org_id': self.organization['id']},
            'expand_content': True
        }

    def test_create(self):
        # создаем новую запись

        webhook = WebHookModel(self.meta_connection).create(
            **self.create_params
        )

        # в пустой таблице появилась запись
        assert_that(
            WebHookModel(self.meta_connection).count(),
            equal_to(1)
        )
        params = self.create_params.copy()
        params['environment'] = app.config['ENVIRONMENT']
        assert_that(
            webhook,
            has_entries(**params)
        )

    def test_delete(self):
        created_webhook = WebHookModel(self.meta_connection).create(
            **self.create_params
        )
        # в таблице webhooks появилась запись
        assert_that(
            WebHookModel(self.meta_connection).count(),
            equal_to(1)
        )

        # удаление по неизвестному id ничего не удалило
        unknown_service_id = created_webhook['id'] + 42
        WebHookModel(self.meta_connection).delete_one(unknown_service_id)
        assert_that(
            WebHookModel(self.meta_connection).count(),
            equal_to(1)
        )

        # а по известному id удалило
        WebHookModel(self.meta_connection).delete_one(created_webhook['id'])
        assert_that(
            WebHookModel(self.meta_connection).count(),
            equal_to(0)
        )

    def test_find(self):
        # поиск по возможным фильтрам
        created_webhook = WebHookModel(self.meta_connection).create(
            **self.create_params
        )
        filters_data = [
            ('id', created_webhook['id']),
            ('service_id', self.service['id']),
            ('event_names', created_webhook['event_names']),
            ('environment', app.config['ENVIRONMENT']),
        ]

        for field, value in filters_data:
            finded = WebHookModel(self.meta_connection).find(
                filter_data={field: value}
            )
            assert_that(
                finded,
                contains(created_webhook)
            )

    def test_find__event_names__contains(self):
        # поиск по фильтру  "in__event_names"

        create_params = self.create_params.copy()
        create_params['event_names'] = [event.department_added, event.department_deleted, event.department_group_added]
        created_webhook = WebHookModel(self.meta_connection).create(
            **create_params
        )
        assert_that(
            WebHookModel(self.meta_connection).find(
                filter_data={
                    'event_names__contains': [event.department_added, event.department_deleted]
                }
            ),
            contains(created_webhook)
        )

        assert_that(
            WebHookModel(self.meta_connection).find(
                filter_data={
                    'event_names__contains': 'unknown_event_name'
                }
            ),
            has_length(0)
        )


    def test_create_with_tvm_client_id(self):
        # Проверим, что при создании можно указать опциональный tvm_client_id

        params = self.create_params.copy()
        params['tvm_client_id'] = 100500

        webhook = WebHookModel(self.meta_connection).create(
            **params
        )

        assert_that(webhook, has_entries(**params))
