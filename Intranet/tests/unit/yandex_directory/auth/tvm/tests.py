# -*- coding: utf-8 -*-
from unittest.mock import (
    patch,
)
from hamcrest import (
    assert_that,
    has_entries,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    WebHookModel,
)
from intranet.yandex_directory.src.yandex_directory.auth import tvm

from testutils import TestCase


class TestGetWebHooks(TestCase):

    def test_get_web_hooks_returns_tvm_ticket_if_needed(self):
        # Проверим, что если есть вебхуки с tvm_client_id, то
        # функция update_tickets будет получать для них TVM 2.0
        # тикеты и складывать в словарик  tvm.tickets

        # создадим подписку на все события
        url = 'http://example.yandex.net/first'
        WebHookModel(self.meta_connection).create(
            url=url,
            service_id=self.service['id'],
            event_names=[],
            expand_content=True,
            tvm_client_id=100500,
        )
        with patch('intranet.yandex_directory.src.yandex_directory.core.models.organization.get_meta_connection')\
            as get_meta_connection:
            get_meta_connection.return_value = self.meta_connection
            tvm.tickets.update_services()

        def get_service_ticket(client_id):
            return 'ticket-{}'.format(client_id)

        self.mocked_tvm2_client.get_service_ticket = get_service_ticket
        assert  tvm.tickets[url] == 'ticket-100500'
