import json
import unittest
from unittest import mock

from agency_rewards.rewards.utils.startrek import TicketCtl, TicketStatus
from agency_rewards.rewards.utils.bunker_node_creator import StartrekWatcher


@mock.patch('agency_rewards.rewards.utils.bunker_node_creator.Config')  # make fixture in pytest
@mock.patch('agency_rewards.rewards.utils.bunker_node_creator.TicketCtl', autospec=TicketCtl)
class TestStartrekWatcher(unittest.TestCase):
    """
    В этих тестах мокаются Config, TicketCtl и запросы в IDM, ST
    Если будет меняться реализация в приватных методах/функциях и тесты будут падать,
    то можно смело удалять падающий тест
    """

    def test_update_tags(self, TicketCtlMock, ConfigMock):
        self.startrek_watcher = StartrekWatcher()

        ticket = {'tags': ['apple', 'banana', 'grape', 'melon', 'lemon', 'orange'], 'key': 'BALANCEAR-1'}
        remove_tags = ['apple', 'orange', 'watermelon']
        self.startrek_watcher.update_tags(ticket, remove_tags)
        self.startrek_watcher.ticket_ctl.set_tags.assert_called_with(
            ['banana', 'grape', 'melon', 'lemon'], 'BALANCEAR-1'
        )

        # with new_tags
        new_tags = ['yabloko', 'grusha']
        self.startrek_watcher.update_tags(ticket, remove_tags, new_tags)
        self.startrek_watcher.ticket_ctl.set_tags.assert_called_with(
            ['banana', 'grape', 'melon', 'lemon', 'yabloko', 'grusha'], 'BALANCEAR-1'
        )

    def _populate_tickets(self):
        """
        Заполнить startrek_watcher тестовыми тикетами из ST API
        """
        self.startrek_watcher.tickets = [
            {
                "lastCommentUpdatedAt": "2019-09-09T14:57:01.852+0000",
                "id": "5d5ecbceefc8d9001d4f007b",
                "bunker_data": {
                    "service_name": "test_service1",
                    "from_dt": "2019-09-09",
                    "scale": "test_scale",
                    "freq": "test_freq",
                    "bunker_node_name": "test_bunker_node",
                    "login": "tester",
                    "ticket": "https://st.test.yandex-team.ru/BALANCEAR-22",
                },
            }
        ]

    def test_get_new_bunker_node_data(self, TicketCtlMock, ConfigMock):
        self.startrek_watcher = StartrekWatcher()
        self._populate_tickets()
        ConfigMock._env_type = 'dev'

        # возвращает схему всех расчетов в Бункере
        with mock.patch(
            'agency_rewards.rewards.utils.bunker_node_creator.StartrekWatcher' '._get_bunker_schema'
        ) as get_bunker_schema:
            get_bunker_schema.return_value = {
                'properties': {
                    "login": {"type": "string", "title": "Логин автора", "required": "true"},
                    "ticket": {"type": "string", "required": "true", "title": "Ссылка на задачу"},
                    "email": {"type": "string", "title": "Список рассылки для уведомлений", "required": "true"},
                },
                'type': 'object',
                'title': 'Описание расчетов премии',
            }

            # возвращает значение по умолчанию для некоторых полей в схеме расчета Бункера
            with mock.patch(
                'agency_rewards.rewards.utils.bunker_node_creator.StartrekWatcher' '._get_bunker_schema_default_values'
            ) as get_bunker_schema_default_values:
                get_bunker_schema_default_values.return_value = {
                    "currency": "RUR",
                    "calc_type": "r",
                    "email": "tester@yandex-team.ru",
                }

                for ticket in self.startrek_watcher.get_new_bunker_node_data():
                    required_keys = {'login', 'ticket', 'email'}  # обязательные ключи из схемы
                    self.assertEqual(ticket['bunker_data'].keys(), required_keys)
                    # поле bunker_data у тикета полностью перезаписывается, остальные поля остаются
                    self.assertEqual(ticket['id'], "5d5ecbceefc8d9001d4f007b")
                    self.assertEqual(ticket['lastCommentUpdatedAt'], "2019-09-09T14:57:01.852+0000")
                    self.assertEqual(
                        ticket['node_path'], "/agency-rewards/dev/calc/test_service1/2019-test_bunker_node-test_freq"
                    )

    @mock.patch(
        "agency_rewards.rewards.utils.bunker_node_creator.StartrekWatcher._perform_request",
        autospec=StartrekWatcher._perform_request,
    )
    def test_create_bunker_nodes(self, perform_request, TicketCtlMock, ConfigMock):
        # initialization
        ConfigMock.BUNKER_CREATE_NODE_HOST = "https://bunker.yandex-team.ru/~api/v2/store"
        ConfigMock.BUNKER_TOKEN = "xyz123"
        ConfigMock._env_type = "dev"
        self.startrek_watcher = StartrekWatcher()

        # возвращает тикеты со всей нужной информацией для создания Бункер ноды
        with mock.patch(
            "agency_rewards.rewards.utils.bunker_node_creator.StartrekWatcher" ".get_new_bunker_node_data"
        ) as get_new_bunker_node_data:
            tickets = [
                {
                    "key": "BALANCEAR-1",
                    "tags": ["create_bunker_node", "tag1", "tag2"],
                    "node_path": "/agency-rewards/dev/calc/service/2019-x-y",
                    "bunker_data": {"login": "test_user1", "email": "test_user1@yandex-team.ru"},
                }
            ]
            get_new_bunker_node_data.return_value = tickets

            self.startrek_watcher.create_bunker_nodes()

            # проверка наличия запросов на создание(пустых)/обновление промежуточных Бункер нод
            for url in ("/agency-rewards/dev/calc", "/agency-rewards/dev/calc/service"):
                perform_request.assert_has_calls(
                    [
                        mock.call(
                            self.startrek_watcher,
                            url="https://bunker.yandex-team.ru/~api/v2/store",
                            method="POST",
                            headers=self.startrek_watcher.bunker_headers,
                            data=dict(node=url),
                            files=dict(data=('empty', '')),
                        )
                    ]
                )

            # проверка наличия запроса на создание Бункер ноды с данными расчета
            perform_request.assert_has_calls(
                [
                    mock.call(
                        self.startrek_watcher,
                        url="https://bunker.yandex-team.ru/~api/v2/store",
                        method="POST",
                        headers=self.startrek_watcher.bunker_headers,
                        data=dict(node="/agency-rewards/dev/calc/service/2019-x-y"),
                        files=dict(data=('data.json', json.dumps(tickets[0]['bunker_data']))),
                    )
                ]
            )

            # проверка на наличие запроса для коммента с ссылкой на тикет в Бункере
            self.startrek_watcher.ticket_ctl.leave_comment.assert_has_calls(
                [
                    mock.call(
                        comment="Ссылка на Бункер ноду для расчета: https://bunker.yandex-team.ru/"
                        "agency-rewards/dev/calc/service/2019-x-y"
                        "\n"
                        "Запросите пожалуйста роль на редактирование узла."
                    )
                ]
            )

    @mock.patch(
        "agency_rewards.rewards.utils.bunker_node_creator.StartrekWatcher._perform_request",
        autospec=StartrekWatcher._perform_request,
    )
    def test_request_idm_access(self, perform_request, TicketCtlMock, ConfigMock):

        # initialization
        ConfigMock.IDM_TOKEN = "idm_token"
        ConfigMock._env_type = "dev"
        self.startrek_watcher = StartrekWatcher()
        perform_request.return_value = dict(id=123456)
        tickets = [
            {
                "key": "BALANCEAR-1",
                "tags": ["/agency-rewards/dev/calc/service/2019-x-y", "tag1", "tag2"],
                "node_path": "/agency-rewards/dev/calc/service/2019-x-y",
                "assignee": {
                    "id": "test_user1",
                },
                "status": {"key": TicketStatus.InProgress},
            },
            {
                "key": "BALANCEAR-2",
                "tags": ["tag1", "tag2"],
                "node_path": "/agency-rewards/dev/calc/service/2019-x-x",
                "assignee": {
                    "id": "test_user2",
                },
                "status": {"key": TicketStatus.Closed.value},
            },
        ]
        self.startrek_watcher.ticket_ctl.get_new_tickets.return_value = tickets

        self.startrek_watcher.request_idm_access()

        # проверить запрос роли в IDM
        perform_request.assert_has_calls(
            [
                mock.call(
                    self.startrek_watcher,
                    method="POST",
                    headers=self.startrek_watcher.idm_headers,
                    url="https://idm-api.yandex-team.ru/api/v1/rolerequests/",
                    json={
                        "user": "test_user1",
                        "system": "bunker",
                        "path": "/agency-rewards/dev/store/",
                        "fields_data": {"path": "calc/service/2019-x-y"},
                        "comment": "Запрос роли разработчику для создания расчета в Бункере",
                    },
                )
            ]
        )

        # проверка на наличие запроса для коммента с ссылкой на тикет запроса роли
        self.startrek_watcher.ticket_ctl.leave_comment.assert_has_calls(
            [
                mock.call(
                    "Запрос на роль для редактирования узла: "
                    "https://idm.yandex-team.ru/system/bunker/roles#role=123456,f-role-id=123456",
                    ticket_id='BALANCEAR-1',
                )
            ]
        )

    @mock.patch("agency_rewards.rewards.utils.bunker_node_creator.BunkerClient")
    def test_parse_ticket_body(self, bunker_client_mocked, TicketCtlMock, ConfigMock):
        bunker_client_mocked.return_value.cat.return_value = {
            'permitted_queues': ['LEGAL', 'LEGALMARKET'],
            '__version': '"2"',
        }

        ticket_body = {
            "key": "BALANCEAR-666",
            "tags": ["create_bunker_node", "tag1", "tag2"],
            "description": "".join(
                [
                    '**Название расчета**\n%%\nЗаводной апельсин\n%%\n\n',
                    '**Краткое описание**\n%%\n',
                    'Clockwork\' \"orange\"%%\n\n',
                    '**Логин разработчика**\n%%\nТест Юзер (test_user2)\n%%\n\n',
                    '**Cервис (direct, market, taxi, etc)**\n%%\ndirect\n%%\n\n',
                    '**Шкала расчета**\n%%\nПремиум 2015 (2)\n%%\n\n',
                    '**Периодичность**\n%%\nПолугодовой\n%%\n\n',
                    '**Начало и конец расчета**\n%%\n2019-08-07 - 2019-08-10\n%%\n\n',
                    '**Тип календаря**\n%%\nФинансовый\n%%\n\n',
                    '**Рассылки для уведомления через запятую**\n%%\ntooeasy@mail.com\n%%\n\n\n',
                    '**Название ноды в Бункере**\n%%CLOCKWORK_orange\n%%\n\n',
                    r'**Тикет с условиями**\n%%\BALANCE-666\n%%\n\n',
                    'Описание платформы:  https://wiki.yandex-team.ru/Balance/AgencyRewardsPlatform/',
                ]
            ),
            "assignee": {
                "id": "test_user2",
            },
        }

        ConfigMock._env_type = 'dev'
        ConfigMock.STARTREK_URL = 'https://st.test.yandex-team.ru'
        self.startrek_watcher = StartrekWatcher()
        ticket = self.startrek_watcher._parse_ticket_body(ticket_body)

        self.assertEqual(ticket['bunker_data']['title'], 'Заводной апельсин')
        self.assertEqual(ticket['bunker_data']['description'], '''Clockwork' "orange"''')
        self.assertEqual(ticket['bunker_data']['login'], 'test_user2')
        self.assertEqual(ticket['bunker_data']['service_name'], 'direct')
        self.assertEqual(ticket['bunker_data']['scale'], '2')
        self.assertEqual(ticket['bunker_data']['calendar'], 'f')
        self.assertEqual(ticket['bunker_data']['email'], 'tooeasy@mail.com balance-reward-info@yandex-team.ru')
        self.assertEqual(ticket['bunker_data']['bunker_node_name'], 'clockwork_orange')
        self.assertEqual(ticket['bunker_data']['ticket'], 'https://st.test.yandex-team.ru/BALANCEAR-666')
        self.assertEqual(len(ticket['bunker_data'].keys()), 12)

    @mock.patch(
        "agency_rewards.rewards.utils.bunker_node_creator.StartrekWatcher._perform_request",
        autospec=StartrekWatcher._perform_request,
    )
    def test_comment_if_no_default_key(self, perform_request, TicketCtlMock, ConfigMock):
        """
        BALANCE-33009: Проверяет, что в тикете оставляется комментарий,
        если пытаемся достать ключ, которого нет в дефолтной схеме
        """
        self.startrek_watcher = StartrekWatcher()
        self.startrek_watcher._get_bunker_schema_default_values = mock.MagicMock()
        self.startrek_watcher._get_bunker_schema_default_values.return_value = {
            "login": "",
            "ticket": "",
            "email": "",
            "title": "",
            "from_dt": "",
            "till_dt": "",
            "freq": "m",
        }

        self.startrek_watcher._get_bunker_schema = mock.MagicMock()
        self.startrek_watcher._get_bunker_schema.return_value = {
            "properties": {
                "login": "",
                "ticket": "",
                "email": "",
                "title": "",
                "from_dt": "",
                "till_dt": "",
                "freq": "",
                "calendar": "",
            }
        }
        ticket = {
            'key': 'TEST-3805',
            'bunker_data': {
                'login': 'nozerchuk',
                'ticket': 'https://st.yandex-team.ru/TEST-3805',
                'email': 'nozerchuk@yandex-team.ru',
                'title': 'testing comments',
                'from_dt': '2021-05-13',
                'till_dt': '2021-06-15',
                'freq': 'Полугодовой',
                'random': 'something',
            },
        }
        ConfigMock._env_type = 'dev'
        ConfigMock.config = {
            "default_schema": "/agency-rewards/schemas/reward_calc_test",
            "default_values": "/agency-rewards/schemas/default_values_test",
        }
        self.startrek_watcher._form_bunker_node_content(ticket)
        self.startrek_watcher.ticket_ctl.leave_comment.assert_has_calls(
            [mock.call('В схеме нет ключа calendar\n', ticket_id='TEST-3805')]
        )

    @mock.patch(
        "agency_rewards.rewards.utils.bunker_node_creator.StartrekWatcher._check_permitted_ticket",
        return_value=True,
    )
    @mock.patch("agency_rewards.rewards.utils.bunker_node_creator.StartrekWatcher._perform_request")
    def test_connect_to_legal(self, perform_request_watcher, check_permitted_mock, TicketCtlMock, ConfigMock):
        """
        Проверяет, что
         -- при создании тикета в очереди BALANCEAR создается связь с LEGAL
         -- Если связь создать не удалось, пишется комментарий в тикет BALANCEAR
        """

        ConfigMock._env_type = 'dev'
        ConfigMock.STARTREK_URL = 'https://st.test.yandex-team.ru'
        self.startrek_watcher = StartrekWatcher()
        self.startrek_watcher.ticket_ctl.create_link = mock.MagicMock()
        self.startrek_watcher.ticket_ctl.create_link.side_effect = Exception('test')
        ticket_body = {
            "key": "BALANCEAR-666",
            "tags": ["create_bunker_node", "tag1", "tag2"],
            "description": "".join(
                [
                    '**Название расчета**\n%%\nЗаводной апельсин\n%%\n\n',
                    '**Краткое описание**\n%%\n',
                    'Clockwork\' \"orange\"%%\n\n',
                    '**Логин разработчика**\n%%\nТест Юзер (test_user2)\n%%\n\n',
                    '**Cервис (direct, market, taxi, etc)**\n%%\ndirect\n%%\n\n',
                    '**Шкала расчета**\n%%\nПремиум 2015 (2)\n%%\n\n',
                    '**Периодичность**\n%%\nПолугодовой\n%%\n\n',
                    '**Начало и конец расчета**\n%%\n2019-08-07 - 2019-08-10\n%%\n\n',
                    '**Тип календаря**\n%%\nФинансовый\n%%\n\n',
                    '**Рассылки для уведомления через запятую**\n%%\ntooeasy@mail.com\n%%\n\n\n',
                    '**Название ноды в Бункере**\n%%clockwork_orange\n%%\n\n',
                    r'**Тикет с условиями**\n%%\LEGALMARKET-1\n%%\n\n',
                    'Описание платформы:  https://wiki.yandex-team.ru/Balance/AgencyRewardsPlatform/',
                ]
            ),
            "assignee": {
                "id": "test_user2",
            },
        }
        self.startrek_watcher._parse_ticket_body(ticket_body)

        self.startrek_watcher.ticket_ctl.create_link.assert_has_calls(
            [mock.call(ticket_id='BALANCEAR-666', link_to='LEGALMARKET-1')]
        )

        self.startrek_watcher.ticket_ctl.leave_comment.assert_has_calls(
            [
                mock.call(
                    'Не получилось создать связь с тикетом LEGALMARKET-1. Свяжите, пожалуйста, тикет перед публикацией.',
                    ticket_id='BALANCEAR-666',
                )
            ]
        )
