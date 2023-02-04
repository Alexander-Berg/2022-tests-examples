"""
Тестируем выполнение логики Celery-задач
"""
from typing import Dict
from unittest import mock

import pytest
import requests
import celery.exceptions
from django.conf import settings
from django.test import override_settings

from billing.dcsaap.backend.celery_app.tasks import (
    webhook_send,
    run_prepare_data,
    check_prepare_run_status,
    send_diffs_by_email,
    monitor_workflow_state,
    check_run_status,
)
from billing.dcsaap.backend.project.const import APP_PREFIX
from billing.dcsaap.backend.core.models import Check, CheckPrepareRun, Run, Diff
from billing.dcsaap.backend.core.enum import FlowType

from billing.dcsaap.backend.tests.utils.models import create_diff
from billing.dcsaap.backend.tests.utils import const


@pytest.fixture
def prevent_sleep():
    """
    Микро-фикстура для отключения паузы при повторах
    """
    with mock.patch('time.sleep') as m:
        yield m


@pytest.mark.usefixtures('prevent_sleep')
class TestWebhookSend:
    """
    Проверяем задачу `webhook_send`
    """

    url = 'http://a.b.c.d.yandex.ru/'
    tvm_id = settings.YAUTH_TVM2_CLIENT_ID

    def test_success(self, requests_mock):
        """
        Проверяем успешную отправку
        """
        requests_mock.register_uri('POST', self.url, status_code=200, text='OK')
        webhook_send.s(self.url, self.tvm_id).apply_async()
        assert requests_mock.call_count == 1

        request = requests_mock.last_request
        assert request.url == self.url
        assert request.headers.get('X-Ya-Service-Ticket')
        assert request.headers.get('User-Agent') == settings.APP_USER_AGENT

    def test_retry(self, requests_mock):
        """
        Проверяем, что корректно отправляем вебхук несколько раз,
          при отсутствии 200 ОК от сервиса.
        """
        requests_mock.register_uri('POST', self.url, status_code=500, text='Ops!')
        with pytest.raises(celery.exceptions.Retry) as exc_info:
            webhook_send.s(self.url, self.tvm_id).apply_async()
        assert isinstance(exc_info.value.exc, requests.exceptions.HTTPError)

    def test_incorrect_tvm_id(self, requests_mock):
        """
        Проверяем, что не реагируем, если передан некорректный TVM ID
        """
        requests_mock.register_uri('POST', self.url, status_code=200, text='OK')
        webhook_send.s(self.url, const.INVALID_SERVICE_TICKET).apply_async()
        assert not requests_mock.called


class TestPrepareRuns:
    """
    Проверяем работу celery-задач для запуска и отслеживания запусков подготовки данных
    """

    @staticmethod
    def exec_check_prepare_run_status(some_prepare: CheckPrepareRun, flow_status_return: Dict):
        """
        Запускаем
        """
        flow_status = f'{APP_PREFIX}.api.nirvana.NirvanaFacade.get_workflow_status'
        with mock.patch(flow_status, return_value=flow_status_return) as flow_status_mock:
            check_prepare_run_status(some_prepare)
            flow_status_mock.assert_called_once_with(some_prepare.instance_id)

    @pytest.fixture
    def nirvana_facade_mock(self):
        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.NirvanaFacade', autospec=True) as m:
            nirvana_facade_api = m.return_value
            yield nirvana_facade_api

    @pytest.fixture
    def some_prepare_started(self, some_prepare: CheckPrepareRun):
        """
        Указываем в запуске нирвана операцию для подготовки данных
        """
        some_prepare.workflow_id = 'flow-111'
        some_prepare.instance_id = 'inst-111'
        some_prepare.save()
        assert some_prepare.finished is None

        return some_prepare

    @override_settings(NIRVANA_API_TOKEN='test-nirvana-token')
    def test_check_prepare_run_status_success(self, some_prepare_started: CheckPrepareRun):
        """
        Проверяем, что проверка статуса вызывает нужное API и сохраняет статус
        """
        flow_status_return = {
            'status': 'completed',
            'result': 'success',
        }

        self.exec_check_prepare_run_status(some_prepare_started, flow_status_return)

        # в БД должна обновиться дата окончания запуска, ошибки быть не должно
        new_prepare_run: CheckPrepareRun = CheckPrepareRun.objects.get(id=some_prepare_started.id)
        assert new_prepare_run.finished
        assert not new_prepare_run.error

    @override_settings(NIRVANA_API_TOKEN='test-nirvana-token')
    def test_check_prepare_run_status_failure(self, some_prepare_started: CheckPrepareRun):
        """
        Проверяем, что проверка статуса корректно обрабатывает ошибку графа
        """
        flow_status_return = {
            'status': 'completed',
            'result': 'failure',
        }

        self.exec_check_prepare_run_status(some_prepare_started, flow_status_return)

        # в БД должна обновиться дата окончания запуска, должна записаться ошибка
        new_prepare_run: CheckPrepareRun = CheckPrepareRun.objects.get(id=some_prepare_started.id)
        assert new_prepare_run.finished
        assert new_prepare_run.error

    @override_settings(NIRVANA_API_TOKEN='test-nirvana-token')
    def test_run_prepare_data(self, some_prepare: CheckPrepareRun, nirvana_facade_mock):
        """
        Проверяем, что запускаем нирвана операцию с указанными в сверке параметрами.
        А так же, что в БД остаются координаты нового запуска.
        """
        # указываем в сверке нирвана операцию для подготовки данных
        check: Check = some_prepare.check_model
        check.prep_workflow_id = 'flow-1'
        check.prep_instance_id = 'inst-1'
        check.save()

        # мок значения для нового запуска
        current_time = '08.12.2020 16:53:14'
        instance_id = 'new-instance-xxx'

        nirvana_facade_mock.make_instance.return_value = instance_id

        # запускаем
        check_status = f'{APP_PREFIX}.celery_app.tasks.check_prepare_run_status.delay'
        get_now = f'{APP_PREFIX}.celery_app.tasks.get_unique_trait'
        with mock.patch(check_status) as check_status_mock, mock.patch(get_now, return_value=current_time):
            run_prepare_data(some_prepare)

            nirvana_facade_mock.make_instance.assert_called_once_with(check, FlowType.PREP)
            nirvana_facade_mock.start_instance.assert_called_once_with(
                nirvana_facade_mock.make_instance.return_value, override_parameters={'unique-trait': current_time}
            )
            check_status_mock.assert_called_once_with(some_prepare)

        # в БД должны остаться данные о новом запуске
        new_prepare_run: CheckPrepareRun = CheckPrepareRun.objects.get(id=some_prepare.id)
        assert new_prepare_run.workflow_id == check.prep_workflow_id
        assert new_prepare_run.instance_id == instance_id

    @override_settings(NIRVANA_API_TOKEN='test-nirvana-token')
    def test_run_prepare_data_exception(self, some_prepare: CheckPrepareRun, nirvana_facade_mock):
        """
        Проверяем, что если произошел Exception при запуске операции в Нирване,
        то записывается ошибка в prepare_run
        """
        # указываем в сверке нирвана операцию для подготовки данных
        check: Check = some_prepare.check_model
        check.prep_workflow_id = 'flow-1'
        check.prep_instance_id = 'inst-1'
        check.save()

        # мок значения для нового запуска
        current_time = '08.12.2020 16:53:14'

        nirvana_facade_mock.start_instance.side_effect = Exception('oops')

        # запускаем
        check_status = f'{APP_PREFIX}.celery_app.tasks.check_prepare_run_status.delay'
        get_now = f'{APP_PREFIX}.celery_app.tasks.get_unique_trait'
        with mock.patch(check_status) as check_status_mock, mock.patch(get_now, return_value=current_time):
            run_prepare_data(some_prepare)

            nirvana_facade_mock.start_instance.assert_called_once_with(
                nirvana_facade_mock.make_instance.return_value, override_parameters={'unique-trait': current_time}
            )
            check_status_mock.assert_not_called()

        # в БД не записываются данные о новом запуске, записывается ошибка
        new_prepare_run: CheckPrepareRun = CheckPrepareRun.objects.get(id=some_prepare.id)
        assert not new_prepare_run.workflow_id
        assert not new_prepare_run.instance_id
        assert new_prepare_run.error == 'oops'


class TestEmailDiffs:
    """
    Проверяем задачу по отправке расхождений на указанный логин
    """

    def test_email_empty_diffs(self, some_run: Run):
        """
        Проверяем, что если расхождений нет, то ничего не отправляем
        """
        send_email = f'{APP_PREFIX}.celery_app.tasks.send_email'
        with mock.patch(send_email) as send_email_mock:
            send_diffs_by_email(some_run, "luke-skywalker")
            send_email_mock.assert_not_called()

    def test_email_non_empty_diffs(self, some_run: Run):
        """
        Проверяем, что если расхождения есть, то отправляем письмо
        """

        # прописываем код в сверку и путь к тикету для запуска,
        # чтобы проверить, что они появятся в теле письма
        check_code = "test_code"
        check_issue = "some-path-to-issue"

        check: Check = some_run.check_model
        check.code = check_code
        check.save()

        some_run.issue = check_issue
        some_run.save()

        login = "luke-skywalker"
        subject = f"[dcs][{check_code}] Расхождения для запуска #{some_run.id} от {some_run.started}"
        body = (
            f"Во вложении выгрузка расхождений для запуска {some_run.id} сверки {check_code}. "
            f"Номер тикета для запуска: {check_issue}"
        )

        create_diff(some_run, 'k1', 'k1_value', 'column', '1', '2')
        create_diff(some_run, 'k', 'k_value', 'columnX', '9', '8', diff_type=Diff.TYPE_NOT_IN_T1)
        create_diff(some_run, 'k2', 'k_value', 'columnY', '9', '8', diff_type=Diff.TYPE_NOT_IN_T2)

        send_email = f'{APP_PREFIX}.celery_app.tasks.send_email'
        with mock.patch(send_email) as send_email_mock:
            send_diffs_by_email(some_run, login)

            send_email_mock.assert_called()
            args, kwargs = send_email_mock.call_args
            assert args[0] == [login + '@yandex-team.ru']
            assert args[1] == subject
            assert args[2] == body
            assert len(args[3]) == 1


class TestNirvanaMonitoring:
    """
    Тестируем таски связанные с мониторингом инстансов в нирване
    """

    @pytest.fixture(autouse=True)
    def setup(self):
        self.flow_status = f'{APP_PREFIX}.api.nirvana.NirvanaFacade.get_workflow_status'
        self.flow_status_completed = {
            'status': 'completed',
            'result': 'success',
        }
        self.workflow_id = "workflow_id"
        self.instance_id = "instance_id"

    def test_monitor_workflow_state(self):
        """
        Тестирует успешное возвращение статуса
        """
        with mock.patch(self.flow_status, return_value=self.flow_status_completed) as flow_status_mock:
            status = monitor_workflow_state.delay(self.instance_id).get()

            flow_status_mock.assert_called_once()
            assert status == self.flow_status_completed

    def test_check_run_status(self, requests_mock):
        """
        Тестируем, что таска дождалась возвращения статуса и дернула ручку
        """
        reply_url = "http://someurl"
        dcs_token = "some_token"
        tvm_ticket = "some_ticket"

        requests_mock.register_uri('GET', reply_url, status_code=200)
        with mock.patch(self.flow_status, return_value=self.flow_status_completed) as flow_status_mock:
            check_run_status(self.instance_id, reply_url, dcs_token, tvm_ticket)

            request = requests_mock.last_request
            assert request.headers['X-YA-SERVICE-TICKET'] == tvm_ticket
            assert request.qs['token'] == [dcs_token]
            assert requests_mock.called
            assert requests_mock.call_count == 1
            flow_status_mock.assert_called_once()

    # TODO тесты с отработкой retry и исключениями в тасках
    # TODO (на данный момент невозможно из-за бага celery 4.3)
