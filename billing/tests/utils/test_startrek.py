import json
import unittest
from unittest.mock import patch, Mock

import requests
import responses

from agency_rewards.rewards.utils.startrek import TicketCtl, TicketStatus


class TestTicketCtl(unittest.TestCase):
    @patch('agency_rewards.rewards.utils.startrek.Config')
    def setUp(self, ConfigMock):
        self.STARTREK_HOST = "https://st-api.test.yandex-team.ru"
        ConfigMock.STARTREK_HOST = self.STARTREK_HOST
        calc = Mock(version='1', ticket='ticket-12345', ticket_id='ticket-12345')
        results = dict(status='ok')
        self.ticketCtl = TicketCtl(calc, results)

    def test_get_ticket_status(self):
        response_body = {'status': dict(key=TicketStatus.InProgress.value)}
        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/ticket-12345/',
                status=200,
                content_type='application/json',
                body=json.dumps(response_body),
            )
            self.assertEqual(self.ticketCtl.get_ticket_status(), TicketStatus.InProgress.value)
            self.assertEqual(self.ticketCtl.last_ticket_status, TicketStatus.InProgress.value)

        # test log output
        with self.assertLogs() as logs:
            with responses.RequestsMock() as resp:
                resp.add(
                    responses.GET,
                    'https://st-api.test.yandex-team.ru/v2/issues/ticket-12345/',
                    body=requests.HTTPError('No such ticket'),
                )
                self.ticketCtl.get_ticket_status()
                self.assertIn(
                    'WARNING:agency_rewards.rewards.' 'utils.startrek:No such ticket',
                    logs.output,
                )

    @patch('agency_rewards.rewards.utils.startrek.Config')
    def test_get_formatted_message(self, ConfigMock):
        path = "/some/path/to/calc"
        calc = Mock(version='1', ticket='ticket-12345', full_name=path)
        self.ticket_ctl = TicketCtl(calc, None)

        self.ticket_ctl.results = dict(status='ok', yql_url='my_share_url')
        self.assertTrue('my_share_url' in self.ticket_ctl.get_formatted_message())

        self.ticket_ctl.results = dict(status='error', message='Это было ужасно!')
        self.assertFalse('my_share_url' in self.ticket_ctl.get_formatted_message())

        self.ticket_ctl.results = dict(status='ok', diff='diff-results', prev_version='0')
        want = (
            "Результат теста: OK\n"
            f"Расчет: https://bunker.yandex-team.ru{path}\n"
            "Версия расчета: 1\n"
            "<{Diff с версией №0:\n"
            "%%(diff)\n"
            "diff-results\n"
            "%% }>"
        )
        self.assertEqual(want, self.ticket_ctl.get_formatted_message())

    def test_can_be_tested(self):
        response_body = {'status': dict(key=TicketStatus.ReadyForTest.value)}

        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/ticket-12345/',
                status=200,
                content_type='application/json',
                body=json.dumps(response_body),
            )

            self.assertEqual(self.ticketCtl.can_be_tested(), True)

        response_body['status']['key'] = TicketStatus.Tested.value

        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/ticket-12345/',
                status=200,
                content_type='application/json',
                body=json.dumps(response_body),
            )

            self.assertEqual(self.ticketCtl.can_be_tested(), False)

    def test_perform_request(self):
        with responses.RequestsMock() as resp:
            resp.add(responses.POST, 'http://example.com', body=requests.exceptions.Timeout())
            with self.assertRaises(requests.exceptions.Timeout):
                self.ticketCtl._perform_request('POST', 'http://example.com')

    def test_change_status_log(self):
        url = "https://st-api.test.yandex-team.ru/v2/issues/ticket-12345/transitions/tested/_execute"
        response = Mock(status_code=404)
        response_exc = requests.RequestException(response=response)
        with self.assertLogs() as logs:
            with responses.RequestsMock() as resp:
                resp.add(responses.POST, url, body=response_exc)
                self.ticketCtl.change_status()
                self.assertIn(
                    'WARNING:agency_rewards.rewards.utils' '.startrek:Wrong transition tested for ticket ticket-12345',
                    logs.output,
                )

    @patch('agency_rewards.rewards.utils.startrek.Config')
    def test_is_balance_ticket(self, ConfigMock):
        ConfigMock.STARTREK_HOST = "https://st-api.test.yandex-team.ru"
        # очередь QUEUE
        bunker_calc = Mock(version='1', ticket='st.yandex-team.ru/WRONG-123')
        ticket_ctl = TicketCtl(bunker_calc, None)
        self.assertEqual(ticket_ctl.is_balance_ticket(), False)

        # очередь BALANCE
        bunker_calc = Mock(version='1', ticket='st.yandex-team.ru/BALANCE-123')
        ticket_ctl = TicketCtl(bunker_calc, None)
        self.assertTrue(ticket_ctl.is_balance_ticket())

    @patch('agency_rewards.rewards.utils.startrek.Config')
    def test_has_approved_status(self, ConfigMock):
        ConfigMock.STARTREK_HOST = "https://st-api.test.yandex-team.ru"
        # очередь BALANCE
        bunker_calc = Mock(version='1', ticket='st.yandex-team.ru/BALANCE-123')
        ticket_ctl = TicketCtl(bunker_calc, None)
        self.assertTrue(ticket_ctl.has_approved_status())

        # очередь BALANCEAR, статус Протестировано
        bunker_calc = Mock(version='1', ticket='st.yandex-team.ru/BALANCEAR-123')
        ticket_ctl = TicketCtl(bunker_calc, None)

        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/BALANCEAR-123/',
                status=200,
                json={'status': {'key': TicketStatus.Tested.value}},
            )
            self.assertEqual(ticket_ctl.has_approved_status(), False)

        # очередь BALANCEAR, статус Подтверждено
        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/BALANCEAR-123/',
                status=200,
                json={'status': {'key': TicketStatus.Confirmed.value}},
            )
            self.assertTrue(ticket_ctl.has_approved_status())

    @patch('agency_rewards.rewards.utils.startrek.Config')
    def test_is_oked(self, ConfigMock):
        ConfigMock.STARTREK_HOST = "https://st-api.test.yandex-team.ru"
        # очередь BALANCE
        bunker_calc = Mock(version='1', ticket='st.yandex-team.ru/BALANCE-123')
        ticket_ctl = TicketCtl(bunker_calc, None)
        self.assertTrue(ticket_ctl.is_oked())

        # очередь BALANCEAR, статус ОКа Запущено
        bunker_calc = Mock(version='1', ticket='st.yandex-team.ru/BALANCEAR-123')
        ticket_ctl = TicketCtl(bunker_calc, None)

        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/BALANCEAR-123/',
                status=200,
                json={'approvementStatus': 'Запущено', 'assignee': {'id': 'johndoe'}},
            )
            post_url = 'https://st-api.test.yandex-team.ru/v2/issues/BALANCEAR-123/transitions/tested/_execute'
            resp.add(responses.POST, post_url, status=200, json={})
            comment_url = 'https://st-api.test.yandex-team.ru/v2/issues/BALANCEAR-123/comments'
            resp.add(responses.POST, comment_url, status=200, json={})
            self.assertEqual(ticket_ctl.is_oked(), False)

        # очередь BALANCEAR, статус ОКа Согласовано
        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/BALANCEAR-123/',
                status=200,
                json={'approvementStatus': 'Согласовано', 'assignee': {'id': 'johndoe'}},
            )
            self.assertTrue(ticket_ctl.is_oked())

    @patch('agency_rewards.rewards.utils.startrek.Config')
    def test_is_oked_while_confirm_not_started(self, ConfigMock):
        ConfigMock.STARTREK_HOST = "https://st-api.test.yandex-team.ru"
        bunker_calc = Mock(version='1', ticket='st.yandex-team.ru/BALANCEAR-123')
        ticket_ctl = TicketCtl(bunker_calc, None)

        # BALANCE-33831: пытаемся проверить, если согласование еще не запущено
        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/BALANCEAR-123/',
                status=200,
                json={'assignee': {'id': 'johndoe'}},
            )
            comment_url = 'https://st-api.test.yandex-team.ru/v2/issues/BALANCEAR-123/comments'

            posted_message = None

            def post_message_cb(request):
                nonlocal posted_message
                payload = json.loads(request.body)
                posted_message = payload['text']
                return 200, {}, "{}"

            resp.add_callback(responses.POST, comment_url, callback=post_message_cb, content_type='application/json')
            self.assertFalse(ticket_ctl.is_oked(change_status=False))
            message = (
                "Не забудьте, пожалуйста, согласовать расчет со всеми ответственными" "(см. макрос 'Согласование')."
            )
            self.assertEqual(posted_message, message)

    @patch('agency_rewards.rewards.utils.startrek.Config')
    def test_is_client_testing_ok(self, ConfigMock):
        ticket_ctl = TicketCtl(None, {"status": "ok"})
        self.assertTrue(ticket_ctl.is_client_testing_ok())

    @patch('agency_rewards.rewards.utils.startrek.Config')
    def test_is_client_testing_not_ok(self, ConfigMock):
        ticket_ctl = TicketCtl(None, {"status": "error"})
        self.assertFalse(ticket_ctl.is_client_testing_ok())

    def test_get_tested_version(self):
        """
        Проверяем, что тэги, полученные из ST корректно интерпретируются для получения последней
        протестированной версии.
        """
        version = "W/1234"
        clean_version = "1234"
        response_body = {
            'tags': [
                "some-other-tag",
                "tested_version=" + version,
            ]
        }
        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/ticket-12345/',
                status=200,
                content_type='application/json',
                body=json.dumps(response_body),
            )
            self.assertEqual(clean_version, self.ticketCtl.get_tested_version())

    def test_get_tested_version_empty(self):
        """
        Проверяем, когда еще нет ни одной протестированной версии в ST
        """
        response_body = {
            'tags': [
                "some-other-tag",
            ]
        }
        with responses.RequestsMock() as resp:
            resp.add(
                responses.GET,
                'https://st-api.test.yandex-team.ru/v2/issues/ticket-12345/',
                status=200,
                content_type='application/json',
                body=json.dumps(response_body),
            )
            self.assertIsNone(self.ticketCtl.get_tested_version())

    def test_set_tested_version(self):
        """
        Проверяем, что при указании протестированной версии, мы не удаляем лишних тэгов
        """
        version = "1234"
        response_body = {
            'tags': [
                "some-other-tag",
                "tested_version=" + version,
            ]
        }
        st_ticket_url = 'https://st-api.test.yandex-team.ru/v2/issues/ticket-12345'
        json_ct = 'application/json'
        with responses.RequestsMock() as resp:
            patched_tags = None

            def patch_cb(request):
                nonlocal patched_tags
                payload = json.loads(request.body)
                patched_tags = payload['tags']
                return 200, {}, "{}"

            resp.add(
                responses.GET, st_ticket_url + "/", status=200, content_type=json_ct, body=json.dumps(response_body)
            )
            resp.add_callback(responses.PATCH, st_ticket_url, callback=patch_cb, content_type=json_ct)
            self.ticketCtl.set_tested_version('10')
            self.assertEqual(["some-other-tag", "tested_version=10"], patched_tags)
