import contextlib
import collections
import requests
import re
from unittest import mock
from typing import Dict, Any, Callable

from django.urls import reverse
from django.conf import settings
from django.db import transaction
from django.test import override_settings
from django.forms.models import model_to_dict
from rest_framework.status import (
    HTTP_200_OK,
    HTTP_201_CREATED,
    HTTP_403_FORBIDDEN,
    HTTP_404_NOT_FOUND,
    HTTP_424_FAILED_DEPENDENCY,
    HTTP_409_CONFLICT,
)

import pytest

from billing.dcsaap.backend.core import enum
from billing.dcsaap.backend.core.models import (
    Diff,
    Check,
    Run,
    OverridableFields,
)
from billing.dcsaap.backend.utils.common import make_status_token
from billing.dcsaap.backend.api.views.run import RunSerializer
from billing.dcsaap.backend.project.const import APP_PREFIX

from billing.dcsaap.backend.tests.utils.models import (
    create_check,
    create_run,
    create_diff,
    create_webhook,
    create_run_via_http,
)
from billing.dcsaap.backend.tests.utils.tvm import TVMAPIClient


@pytest.fixture
def nirvana_facade_mock():
    with mock.patch(f'{APP_PREFIX}.api.views.run.NirvanaFacade', autospec=True) as m:
        nirvana_facade_api = m.return_value
        nirvana_facade_api.make_instance.return_value = 'xxxx'
        nirvana_facade_api.start_instance.return_value = 'xxxx'

        yield nirvana_facade_api


@pytest.fixture(autouse=True)
def mock_nirvana_monitoring():
    """
    Мокаем таски в celery, которые отвечают за мониторинг графа в нирване
    """
    with mock.patch(f'{APP_PREFIX}.celery_app.tasks.check_run_status'), mock.patch(
        f'{APP_PREFIX}.celery_app.tasks.check_prepare_run_status'
    ):
        yield


def mock_celery_load_diff_task():
    return mock.patch(f'{APP_PREFIX}.celery_app.tasks.load_diffs_from_yt')


def perform_run(client: TVMAPIClient, run: Run):
    """
    Выполняет запуск сверки (до финального статуса)
    """
    previous_status = run.status
    for status in (Run.STATUS_DATA_LOADING, Run.STATUS_FINISHED):
        with mock_celery_load_diff_task():
            response = client.get(
                reverse('run-switch-status', args=[run.id]),
                dict(token=make_status_token(run.id, previous_status, status)),
            )
        previous_status = status
        assert response.status_code == HTTP_200_OK


def perform_run_fail(client: TVMAPIClient, run: Run, error: str):
    """
    Выполняет запуск сверки (до финального статуса - ошибки)
    """
    previous_status = run.status
    for status in (Run.STATUS_ERROR,):
        with mock_celery_load_diff_task():
            response = client.get(
                reverse('run-switch-status', args=[run.id]),
                dict(token=make_status_token(run.id, previous_status, status), error=error),
            )
        previous_status = status
        assert response.status_code == HTTP_200_OK


class TestRunGet:
    """
    Тестирование GET запросов для описаний запусков
    """

    @pytest.fixture(autouse=True)
    def setup(self, db):
        ch1 = create_check("check1", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res")
        create_run(ch1, status=Run.STATUS_ERROR)
        create_run(ch1, status=Run.STATUS_FINISHED)
        create_run(ch1, status=Run.STATUS_STARTED)

    def test_get_all_runs(self, tvm_api_client):
        response = tvm_api_client.get(reverse('run-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 3


class TestRunCreateUpdate:
    """
    Тестирование создания и изменения запуска
    """

    @pytest.fixture(autouse=True)
    def setup(self, db):
        self.ch1 = create_check(
            "check1", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res", status=Check.STATUS_DISABLED
        )
        self.ch2 = create_check(
            "check2", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res", status=Check.STATUS_ENABLED
        )

    def test_create_run_failed(self, tvm_api_client):
        """
        Так как сверка не активна, запуск регистрируется с ошибкой
        :return:
        """
        response = tvm_api_client.get(reverse('run-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 0

        values = (
            ('check_model', self.ch1.id),
            ('type', Run.TYPE_PROD),
            ('status', Run.STATUS_STARTED),
        )

        new_run = {}
        for k, v in values:
            new_run[k] = v

        response = tvm_api_client.post(reverse('run-list'), new_run, format='json')
        assert response.status_code == HTTP_424_FAILED_DEPENDENCY, response.content

        response = tvm_api_client.get(reverse('run-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 1

        run = response.data['results'][0]
        assert run['check_model'] == self.ch1.id
        assert run['type'] == Run.TYPE_PROD
        assert run['type_str'] == 'Прод'
        assert run['status'] == Run.STATUS_ERROR
        assert run['status_str'] == Run.STATUS_LABELS[2]
        assert run['error'] == 'Сверка не активна'
        # CHECK-3040
        assert run['finished'] is not None

    @pytest.mark.usefixtures('nirvana_facade_mock')
    def test_not_unique_run_correct_status(self, tvm_api_client):
        """
        Запрещено создавать два Run с одинаковыми Check, Run.cluster, Run.result и Run.state = 0
        """
        values = (
            ('check_model', self.ch2.id),
            ('type', Run.TYPE_TEST),
            ('status', Run.STATUS_STARTED),
        )

        new_run = {}
        for k, v in values:
            new_run[k] = v

        response = tvm_api_client.post(reverse('run-list'), new_run, format='json')
        assert response.status_code == HTTP_201_CREATED, response.content

        with transaction.atomic():
            response = tvm_api_client.post(reverse('run-list'), new_run, format='json')
        assert response.status_code == HTTP_409_CONFLICT, response.content

        new_run['cluster'] = enum.HAHN
        response = tvm_api_client.post(reverse('run-list'), new_run, format='json')
        assert response.status_code == HTTP_201_CREATED, response.content


@pytest.mark.usefixtures('nirvana_facade_mock')
class TestRunOverridableFields:
    """
    Тестирование логики заполнения переопределяемых полей модели `Run`.
    """

    @pytest.fixture(autouse=True)
    def setup(self, db):
        self.ch1 = create_check(
            "check1", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res", status=Check.STATUS_ENABLED
        )
        self.ch2 = create_check(
            "check2", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res1", status=Check.STATUS_ENABLED
        )

    @staticmethod
    def _create_run(client, check, table1=None, table2=None, result=None, cluster=None, diffs_count_limit=None):
        values = (
            ('check_model', check.id),
            ('type', Run.TYPE_TEST),
            ('status', Run.STATUS_STARTED),
        )

        new_run = {}
        for k, v in values:
            new_run[k] = v

        if table1 is not None:
            new_run['table1'] = table1

        if table2 is not None:
            new_run['table2'] = table2

        if result is not None:
            new_run['result'] = result

        if cluster is not None:
            new_run['cluster'] = cluster

        response = client.post(reverse('run-list'), new_run, format='json')
        assert response.status_code == HTTP_201_CREATED, response.content
        return response.data

    @pytest.mark.parametrize('field_name', OverridableFields.fields())
    def test_run_field_not_overridden(self, tvm_api_client, field_name: str):
        """
        Если ничего не передано, Run.`field_name` проставляется из Check.`field_name`.
        """
        for check in (self.ch1, self.ch2):
            run = self._create_run(tvm_api_client, check)
            assert getattr(check, field_name) == run[field_name]

    @pytest.mark.parametrize(
        'field_name,expected',
        zip(
            (field_name for field_name in OverridableFields.fields() if field_name != 'diffs_count_limit'),
            ('/t1', '/t2', '/expected', enum.HAHN),
        ),
    )
    def test_run_field_overridden(self, tvm_api_client, field_name: str, expected: Any):
        """
        Дополнительно можно переопределить Run.`field`.
        """
        for check in (self.ch1, self.ch2):
            run = self._create_run(tvm_api_client, check, **{field_name: expected})
            assert expected == run[field_name]


@pytest.mark.usefixtures('ylock_mock')
class TestRunFinished:
    """
    Проверяем логику, выполняемую при завершении запуска
    """

    @pytest.fixture
    def check(self):
        return create_check("check", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res")

    @pytest.fixture
    def run(self, check):
        return create_run(check, type_=Run.TYPE_PROD)

    @staticmethod
    def create_diffs(run, yt_client_read_table_mock):
        yt_client_read_table_mock.set(
            [
                create_diff(run, 'k1', 'k1_value', 'column', '1', '2'),
                create_diff(run, 'k1', 'k1_value', 'column', '3', '4'),
            ]
        )

    @pytest.fixture
    def diffs(self, run, yt_client_read_table_mock):
        self.create_diffs(run, yt_client_read_table_mock)

    @pytest.mark.usefixtures('freeze_now')
    def test_webhook_sent(self, tvm_api_client, requests_mock, run):
        """
        Проверяем, что при успешном запуске будет отправлен вебхук с информацией о запуске
        """
        webhook_event = 'run.finished'

        webhook = create_webhook(webhook_event)
        requests_mock.register_uri('POST', webhook.url, text='OK')

        perform_run(tvm_api_client, run)

        response = tvm_api_client.get(reverse('run-detail', args=[run.id]))
        assert response.status_code == HTTP_200_OK
        run_data = response.data

        response = tvm_api_client.get(reverse('check-detail', args=[run.check_model_id]))
        assert response.status_code == HTTP_200_OK
        check_data = response.data

        expected_webhook_body = {
            'event_name': webhook_event,
            'payload': dict(check=check_data, run=run_data),
        }

        assert requests_mock.called
        assert requests_mock.call_count == 1

        request = requests_mock.last_request
        assert request.json() == expected_webhook_body

    @pytest.mark.usefixtures('diffs', 'tracker_mock')
    def test_tracker_issue_created(self, tvm_api_client, run):
        """
        Проверяем, что при успешном продовом запуске будет создан тикет
        """
        perform_run(tvm_api_client, run)

        run = Run.objects.get(id=run.id)
        assert run.issue == f'{settings.TRACKER_HOST}/{run.check_model.debrief_queue}-1'

    @pytest.mark.usefixtures('tracker_mock')
    def test_tracker_issue_not_created_on_success(self, tvm_api_client, check, yt_client_read_table_mock):
        """
        Проверяем, что при успешном тестовом запуске тикет не будет создан
        """
        run = create_run(check, type_=Run.TYPE_TEST)
        self.create_diffs(run, yt_client_read_table_mock)

        with override_settings(TRACKER_ENABLED=True):
            perform_run(tvm_api_client, run)

        run = Run.objects.get(id=run.id)
        assert run.issue is None

    @pytest.mark.usefixtures('tracker_mock')
    def test_tracker_issue_created_without_diffs(self, tvm_api_client, run):
        """
        Проверяем, что при успешном запуске без расхождений тикет будет создан
        """
        perform_run(tvm_api_client, run)

        run = Run.objects.get(id=run.id)
        assert run.issue


@pytest.mark.usefixtures('nirvana_facade_mock', 'tracker_mock', 'ylock_mock')
class TestRunFillStable:
    """
    Проверяем, что после того, как отработала осн. часть сверки (YT/YQL),
    мы запускаем копирование стабильных расхождений
    """

    @pytest.fixture
    def yql_client_mock(self):
        with mock.patch(f'{APP_PREFIX}.utils.celery.YqlClient') as m:
            yield m

    def test_call_copy_ok(self, tvm_api_client, some_check, yt_client_mock, yql_client_mock, requests_mock):
        """
        Проверяем основной сценарий, что копирование расхождений запустилось
        """
        # CHECK-3373: моделируем проблему, когда по сверке более 1 запуска в истории
        # и запрос поиска пред. запуска падает с ошибкой:
        # MultipleObjectsReturned: get() returned more than one Run -- it returned 2!
        zero_run = create_run(some_check)
        zero_run.done(is_success=True)
        zero_run.save()

        first_run = create_run(some_check)
        first_run.done(is_success=True)
        first_run.save()

        second_run = create_run(some_check)
        run_id = second_run.id

        requests_mock.register_uri('GET', reverse('run-switch-status', args=[run_id]), status_code=200)
        response = tvm_api_client.get(
            reverse('run-switch-status', args=[run_id]),
            dict(token=make_status_token(run_id, Run.STATUS_STARTED, Run.STATUS_FILL_STABLE_DIFFS)),
        )
        assert response.status_code == HTTP_200_OK
        assert requests_mock.called
        assert requests_mock.call_count == 1

        yt_client_mock().create.assert_called_once()
        yt_client_mock().write_table.assert_called_once()
        yql_query = yql_client_mock().query()
        yql_query.run.assert_called_once()
        yql_result = yql_query.get_results()
        yql_result.table.fetch_full_data.assert_called_once()

    def test_call_copy_fail(self, tvm_api_client, some_check, yt_client_mock, yql_client_mock, requests_mock):
        first_run = create_run(some_check)
        first_run.done(is_success=True)
        first_run.save()

        second_run = create_run(some_check)
        run_id = second_run.id

        yql_query = yql_client_mock().query()
        yql_result = yql_query.get_results()
        yql_result.is_success = False

        issue = mock.MagicMock()
        issue.format_issue.return_value = 'exception'
        yql_result.errors = [issue]

        requests_mock.register_uri('GET', reverse('run-switch-status', args=[run_id]), status_code=200)
        response = tvm_api_client.get(
            reverse('run-switch-status', args=[run_id]),
            dict(token=make_status_token(run_id, Run.STATUS_STARTED, Run.STATUS_FILL_STABLE_DIFFS)),
        )
        assert response.status_code == HTTP_200_OK
        assert requests_mock.called
        assert requests_mock.call_count == 1

        yt_client_mock().create.assert_called_once()
        yt_client_mock().write_table.assert_called_once()
        yql_query.run.assert_called_once()
        yql_result.table.fetch_full_data.assert_not_called()


@pytest.mark.usefixtures('nirvana_facade_mock', 'ylock_mock')
class TestRunCallAA:
    """
    Проверяем, что после того, как отработала осн. часть сверки (YT/YQL),
    мы запускаем flow авторазбора.
    """

    @pytest.fixture(autouse=True)
    def setup(self, db):
        self.check_flow_id = "AA_FLOW_77"
        self.check_inst_id = "AA_INST_99"
        self.run_inst_id = "RUN_INST_ID_12345"
        self.check = create_check(
            "check",
            enum.ARNOLD,
            "/t1",
            "/t2",
            "k1 k2",
            "v1 v2",
            "/res",
            aa_workflow_id=self.check_flow_id,
            aa_instance_id=self.check_inst_id,
            is_sox=True,
        )

    @pytest.fixture
    def nirvana_facade_mock_aa(self):
        with mock.patch(f'{APP_PREFIX}.utils.common.NirvanaFacade', autospec=True) as m:
            nirvana_facade_api = m.return_value
            nirvana_facade_api.make_instance.return_value = self.run_inst_id
            nirvana_facade_api.start_instance.return_value = self.run_inst_id

            yield nirvana_facade_api

    def switch_status_before_aa(self, tvm_api_client, run_id):
        """
        Переключаем статус на тот, что идет перед запуском авторазбора
        """
        # Переключаем статус копирования стабильных расхождений
        response = tvm_api_client.get(
            reverse('run-switch-status', args=[run_id]),
            dict(
                token=make_status_token(run_id, Run.STATUS_STARTED, Run.STATUS_FILL_STABLE_DIFFS),
                just_switch='da',
            ),
        )
        assert response.status_code == HTTP_200_OK

    @pytest.mark.usefixtures('nirvana_facade_mock_aa')
    def test_call_aa_ok(self, tvm_api_client):
        """
        Проверяем осн. сценарий. АА запустился. Проверяем, что в БД остались его следы (id)
        """
        # Запускаем сверку через API
        response = create_run_via_http(tvm_api_client, self.check)
        run_id = response["id"]
        assert run_id

        self.switch_status_before_aa(tvm_api_client, run_id)

        # сообщаем, что сверка в YT отработала, можно запускать АА
        # (мокаем запуск АА в backend.api.utils.start_auto_analyzer)
        response = tvm_api_client.get(
            reverse('run-switch-status', args=[run_id]),
            dict(token=make_status_token(run_id, Run.STATUS_FILL_STABLE_DIFFS, Run.STATUS_STARTED_AA)),
        )
        assert response.status_code == HTTP_200_OK

        run = Run.objects.get(pk=run_id)
        assert run.aa_instance_id == self.run_inst_id
        assert run.aa_workflow_id == self.check_flow_id
        assert run.status == Run.STATUS_STARTED_AA

    def test_call_aa_fail(self, tvm_api_client, nirvana_facade_mock_aa):
        """
        Проверяем ативный сценарий. АА не запустился.
        Проверяем, что в БД остались его следы (статус, ошибка)
        """
        # Запускаем сверку через API
        response = create_run_via_http(tvm_api_client, self.check)
        run_id = response["id"]
        assert run_id

        self.switch_status_before_aa(tvm_api_client, run_id)

        nirvana_facade_mock_aa.start_instance.side_effect = Exception('Alarm! Fire!')
        # сообщаем, что сверка в YT отработала, можно запускать АА
        # (мокаем запуск АА в backend.api.utils.start_auto_analyzer)
        response = tvm_api_client.get(
            reverse('run-switch-status', args=[run_id]),
            dict(token=make_status_token(run_id, Run.STATUS_FILL_STABLE_DIFFS, Run.STATUS_STARTED_AA)),
        )
        assert response.status_code == HTTP_200_OK

        run = Run.objects.get(pk=run_id)
        assert run.aa_instance_id is None
        assert run.aa_workflow_id is None
        assert run.status == Run.STATUS_ERROR
        assert run.error == "Alarm! Fire!"

    @pytest.mark.usefixtures('nirvana_facade_mock_aa')
    def test_call_aa_finished(self, tvm_api_client):
        """
        Проверяем, случай, когда АА запустился и прислал ответ, что он отработал.
        Проверяем, что токен рабочий (для обратного вызова из АА) + изменился статус.
        """
        # Запускаем сверку через API
        response = create_run_via_http(tvm_api_client, self.check)
        run_id = response["id"]
        assert run_id

        self.switch_status_before_aa(tvm_api_client, run_id)

        # сообщаем, что сверка в YT отработала, можно запускать АА
        # (ремокаем celery_app.tasks.tasks.check_run_status.delay, чтобы запомнить токен)
        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.check_run_status.delay') as m:
            response = tvm_api_client.get(
                reverse('run-switch-status', args=[run_id]),
                dict(token=make_status_token(run_id, Run.STATUS_FILL_STABLE_DIFFS, Run.STATUS_STARTED_AA)),
            )
            assert response.status_code == HTTP_200_OK
            args, _kwargs = m.call_args
            assert args is not None

            # сохраням токен, чтобы потом с ним оповестить об успехе АА
            dcs_token = args[2]

        # сообщаем, что АА в YT отработала, можно запускать загрузку данных
        # (мокаем загрузку данных)
        with mock_celery_load_diff_task():
            response = tvm_api_client.get(reverse('run-switch-status', args=[run_id]), dict(token=dcs_token))
            assert response.status_code == HTTP_200_OK

        run = Run.objects.get(pk=run_id)
        assert run.status == Run.STATUS_DATA_LOADING

    def test_create_issue_before_aa(self, tvm_api_client, nirvana_facade_mock_aa):
        """
        Проверяем, что тикет сделали перед запуском АА.
        """
        # Запускаем сверку через API
        response = create_run_via_http(tvm_api_client, self.check, type=Run.TYPE_PROD)
        run_id = response["id"]
        assert run_id

        self.switch_status_before_aa(tvm_api_client, run_id)

        patch_target = f'{APP_PREFIX}.utils.tracker.tracker.create_issue'
        issue_id = 'XXX-777'
        with mock.patch(patch_target, autospec=True, return_value=issue_id) as create_issue_mock:
            # сообщаем, что сверка в YT отработала, можно запускать АА
            response = tvm_api_client.get(
                reverse('run-switch-status', args=[run_id]),
                dict(token=make_status_token(run_id, Run.STATUS_FILL_STABLE_DIFFS, Run.STATUS_STARTED_AA)),
            )
            assert response.status_code == HTTP_200_OK

            # Mock создания тикета вызвался
            assert create_issue_mock.called

            # Передаем в нирвану номер нового тикета
            _args, kwargs = nirvana_facade_mock_aa.start_instance.call_args
            assert kwargs['override_parameters']['issue'].find(issue_id) != -1

        assert Run.objects.get(pk=run_id).issue.find(issue_id) != -1

        # начали грузить данные
        with mock_celery_load_diff_task():
            response = tvm_api_client.get(
                reverse('run-switch-status', args=[run_id]),
                dict(token=make_status_token(run_id, Run.STATUS_STARTED_AA, Run.STATUS_DATA_LOADING)),
            )
            assert response.status_code == HTTP_200_OK

        # закончили грузить данные
        patch_target = f'{APP_PREFIX}.utils.tracker.update_tracker_issue'
        with mock.patch(patch_target) as update_issue_mock:
            response = tvm_api_client.get(
                reverse('run-switch-status', args=[run_id]),
                dict(token=make_status_token(run_id, Run.STATUS_DATA_LOADING, Run.STATUS_FINISHED)),
            )
            assert response.status_code == HTTP_200_OK
            # Mock обновления тикета вызвался
            assert update_issue_mock.called


class TestRunSerializer:
    """
    Проверяем логику сериализации/десериализации модели Run
    """

    @pytest.fixture
    def gen_repr(self, api_rf) -> Callable[[Run], Dict[str, Any]]:
        """
        Генератор сериализованных `Run`
        """

        def _gen_repr(run: Run) -> Dict[str, Any]:
            request = api_rf.get('/')

            rs = RunSerializer(context={'request': request})
            rs.instance = run

            return rs.data

        return _gen_repr

    def test_check_url(self, some_run: Run, gen_repr: Callable):
        """
        Проверка формирования поля `check_url`
        """
        relative_url = reverse(
            'check-detail',
            args=[
                some_run.check_model.id,
            ],
        )

        representation = gen_repr(some_run)

        assert representation['check_url'].endswith(relative_url)

    def test_check_url_different_id(self, some_check: Check, gen_repr: Callable):
        """
        Дополнительная проверка формирования поля `check_url`
        Тест выше не упадет, если у Run и Check одинаковые ID
        """
        first_run = create_run(some_check)
        first_run.done(is_success=True)
        first_run.save()

        second_run = create_run(some_check)

        relative_url = reverse(
            'check-detail',
            args=[
                some_check.pk,
            ],
        )

        representation1 = gen_repr(first_run)
        representation2 = gen_repr(second_run)

        assert representation1['check_url'].endswith(relative_url)
        assert representation2['check_url'].endswith(relative_url)

    def test_internal_result(self, some_run: Run, gen_repr: Callable):
        """
        Проверка формирования поля `internal_result`
        """
        expected = {
            'cluster': some_run.cluster,
            'path': some_run.internal_result_with_ttl,
        }

        representation = gen_repr(some_run)

        assert representation['internal_result'] == expected


class TestRunAccess:
    """
    Проверяем контроль доступа к запускам
    """

    ext_user = "ded-moroz"
    run_list_url = reverse('run-list')

    @pytest.fixture(autouse=True)
    def setup(self):
        self.ch_mine = create_check("check_mine", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res1")
        create_run(self.ch_mine, type_=Run.TYPE_TEST, status=Run.STATUS_FINISHED)
        create_run(self.ch_mine, type_=Run.TYPE_TEST, status=Run.STATUS_ERROR)
        create_run(self.ch_mine, type_=Run.TYPE_TEST)
        self.ch_not_mine = create_check(
            "check_not_mine", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res2", login=self.ext_user
        )
        create_run(self.ch_not_mine, type_=Run.TYPE_TEST, status=Run.STATUS_ERROR)

    def test_access_own_runs(self, tvm_api_client):
        response = tvm_api_client.get(self.run_list_url)
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 3
        assert response.data['results'][0]['check_name'] == self.ch_mine.title
        assert response.data['results'][1]['check_name'] == self.ch_mine.title
        assert response.data['results'][2]['check_name'] == self.ch_mine.title

        with override_settings(YAUTH_TEST_USER=self.ext_user):
            response = tvm_api_client.get(self.run_list_url)
            assert response.status_code == HTTP_200_OK
            assert response.data['count'] == 1
            assert response.data['results'][0]['check_name'] == self.ch_not_mine.title

    @pytest.mark.usefixtures('nirvana_facade_mock')
    def test_create_run_for_not_own_check(self, tvm_api_client):
        run_json = {
            'check_model': self.ch_not_mine.id,
            'type': Run.TYPE_TEST,
            'status': Run.STATUS_STARTED,
        }

        response = tvm_api_client.post(self.run_list_url, run_json, format='json')
        assert response.status_code == HTTP_201_CREATED


class TestRunEditControl:
    """
    Проверяем доступ к запуску.

    - Аноним - ничего нельзя, даже смотреть
    - Суперпользователь - можно создавать, редактировать любую, смотреть любую
    - Представитель - можно создавать, редактировать и смотреть только свое
    - Аудит - создавать нельзя, редактировать ничего нельзя, смотреть можно всё
    """

    run_list_url = reverse('run-list')

    def test_edit_any_run(self, super_yauser, int_audit_yauser, anon_yauser, yauser, some_run, tvm_api_client):
        """
        Проверяем, что редактировать запуск может только только владелец
        или суперпользователь
        """
        run_edit_url = reverse('run-detail', kwargs={'pk': some_run.id})
        run_test = model_to_dict(some_run)
        run_test["type"] = Run.TYPE_TEST

        with override_settings(YAUTH_TEST_USER=super_yauser.login):
            r = tvm_api_client.put(run_edit_url, run_test, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=some_run.check_model.created_login):
            r = tvm_api_client.put(run_edit_url, run_test, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=int_audit_yauser.login):
            r = tvm_api_client.put(run_edit_url, run_test, format='json')
            assert r.status_code == HTTP_403_FORBIDDEN, r.data

        with override_settings(YAUTH_TEST_USER=yauser.login):
            r = tvm_api_client.put(run_edit_url, run_test, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

        with override_settings(YAUTH_TEST_USER=anon_yauser.login):
            r = tvm_api_client.put(run_edit_url, run_test, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

    @pytest.mark.usefixtures('nirvana_facade_mock')
    def test_create_run(self, super_yauser, int_audit_yauser, anon_yauser, yauser, some_check, tvm_api_client):
        """
        Проверяем, что создать запуск может только только владелец
        или суперпользователь
        """
        run_data = {
            'check_model': some_check.id,
            'type': Run.TYPE_TEST,
            'status': Run.STATUS_ERROR,
        }

        with override_settings(YAUTH_TEST_USER=super_yauser.login):
            run_data['result'] = '/tmp/1'
            r = tvm_api_client.post(self.run_list_url, run_data, format='json')
            assert r.status_code == HTTP_201_CREATED, r.data

        with override_settings(YAUTH_TEST_USER=int_audit_yauser.login):
            run_data['result'] = '/tmp/2'
            r = tvm_api_client.post(self.run_list_url, run_data, format='json')
            assert r.status_code == HTTP_403_FORBIDDEN, r.data

        # TODO: тут и ниже должно быть 403.
        # Сейчас мы таких отбрасываем на уровне UI,
        # но надо так же и на уровне API сделать проверку
        # (см. backend.api.permissions.HasRoleInSystem)
        with override_settings(YAUTH_TEST_USER=yauser.login):
            run_data['result'] = '/tmp/3'
            r = tvm_api_client.post(self.run_list_url, run_data, format='json')
            assert r.status_code == HTTP_201_CREATED, r.data

        with override_settings(YAUTH_TEST_USER=anon_yauser.login):
            run_data['result'] = '/tmp/4'
            r = tvm_api_client.post(self.run_list_url, run_data, format='json')
            assert r.status_code == HTTP_201_CREATED, r.data

    def test_read_run(self, super_yauser, int_audit_yauser, anon_yauser, yauser, some_run: Run, tvm_api_client):
        """
        Проверяем, что смотреть запуск может только владелец, суперпользователь, аудит
        """
        run_url = reverse('run-detail', kwargs={'pk': some_run.id})

        with override_settings(YAUTH_TEST_USER=super_yauser.login):
            r = tvm_api_client.get(run_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=some_run.check_model.created_login):
            r = tvm_api_client.get(run_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=int_audit_yauser.login):
            r = tvm_api_client.get(run_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=yauser.login):
            r = tvm_api_client.get(run_url, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

        with override_settings(YAUTH_TEST_USER=anon_yauser.login):
            r = tvm_api_client.get(run_url, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data


@pytest.mark.usefixtures('nirvana_facade_mock', 'ylock_mock')
class TestEmailAfterRun:
    """
    Проверяем, что отправили email об окончании работы сверки
    """

    @pytest.fixture(autouse=True)
    def setup(self, db):
        self.patch_target = f'{APP_PREFIX}.utils.common.start_workflow'
        self.check_code = 'code-xxx'
        self.check = create_check(
            f"check ({self.check_code})", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res", code=self.check_code
        )

    def test_email_disabled(self, tvm_api_client: TVMAPIClient, some_run: Run):
        """
        Проверяем, что отправки письма не было, т.к. по умолчанию отключено
        """
        with mock.patch(f'{APP_PREFIX}.utils.email.send_email', return_value=True) as sm:
            perform_run(tvm_api_client, some_run)
            assert 0 == sm.call_count

    @override_settings(END_STATUS_EMAIL_ENABLED=True)
    def test_success_email(self, tvm_api_client: TVMAPIClient):
        """
        Проверяем сообщение об успешном окончании
        """
        hostname = 'host-xxx'
        run = create_run(self.check)

        with mock.patch(f'{APP_PREFIX}.utils.email.send_email', return_value=True) as sm, mock.patch(
            'socket.gethostname', return_value=hostname
        ):

            perform_run(tvm_api_client, run)
            assert 1 == sm.call_count

            args, kwargs = sm.call_args_list[0]
            assert args[1] == f'[{hostname}] Check {self.check_code} (ID {run.id}) finished with 0 new diffs'

    @override_settings(END_STATUS_EMAIL_ENABLED=True)
    def test_fail_email(self, tvm_api_client: TVMAPIClient):
        """
        Проверяем сообщение об ошибке
        """
        hostname = 'host-xxx'
        error_text = 'Aaaaa! It failed!!!'
        run = create_run(self.check)

        with mock.patch(f'{APP_PREFIX}.utils.email.send_email', return_value=True) as sm, mock.patch(
            'socket.gethostname', return_value=hostname
        ):

            perform_run_fail(tvm_api_client, run, error_text)
            assert 1 == sm.call_count

            args, kwargs = sm.call_args_list[0]
            assert args[1] == f'[{hostname}] Check {self.check_code} (ID {run.id}) failed'

            email_body = args[2].split('\n')
            assert email_body[0] == f'Запуск: {run.ui_url}'
            assert email_body[1] == f'Ошибка: {error_text}'


class BaseTestRequestFromTracker:
    url = secret = issue_key = user_id = None

    User = collections.namedtuple('User', 'uid')

    @contextlib.contextmanager
    def mock_permissions(self, queue_team):
        queue_team = [TestRunResolve.User(user_id) for user_id in queue_team]

        patch_target = f'{APP_PREFIX}.utils.tracker.tracker.get_tracker_client'
        with mock.patch(patch_target) as m, override_settings(TRACKER_REQUEST_SECRET=self.secret):
            m.return_value.queues.get.return_value.teamUsers = queue_team

            yield m

    def make_request(self, api_client, queue_team, secret=None):
        if secret is None:
            secret = self.secret

        with self.mock_permissions(queue_team), override_settings(TRACKER_ENABLED=True):
            return api_client.post(
                self.url,
                HTTP_X_TRACKER_ISSUE_KEY=self.issue_key,
                HTTP_X_TRACKER_USER_ID=str(self.user_id),
                HTTP_X_TRACKER_SECRET=secret,
            )


class TestRunResolve(BaseTestRequestFromTracker):
    @pytest.fixture(autouse=True)
    def setup(self):
        self.url = reverse('run-resolve')

        self.secret = 'some-secret'

        self.issue_key = 'SSD-123456'
        self.user_id = 123456

    def test_access(self, api_client):
        response = self.make_request(api_client, queue_team=[])
        assert response.status_code == 403

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
            secret='invalid-secret',
        )
        assert response.status_code == 403

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
        )
        assert response.status_code == 200

    def test_resolve(self, api_client, some_run, some_diffs):
        some_run.issue = f'{settings.TRACKER_HOST}/{self.issue_key}'
        some_run.save()

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
        )
        assert response.status_code == 200
        assert response.data['success']
        assert response.data['count'] == 2

    def test_partial_resolve(self, api_client, some_run):
        some_run.issue = f'{settings.TRACKER_HOST}/{self.issue_key}'
        some_run.save()

        create_diff(some_run, 'k1', 'k1_value', 'column', '1', '2', status=Diff.STATUS_CLOSED),
        create_diff(some_run, 'k1', 'k1_value', 'column', '3', '4'),

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
        )
        assert response.status_code == 200
        assert response.data['success']
        assert response.data['count'] == 1

    def test_webhook_sent(self, api_client, requests_mock, some_run):
        """
        Проверяем отправление изменений в баланс при закрытии расхождений через ST
        """
        webhook_event = 'diffs.changed'
        webhook = create_webhook(webhook_event)
        requests_mock.register_uri('POST', webhook.url, text='OK')

        some_run.issue = f'{settings.TRACKER_HOST}/{self.issue_key}'
        some_run.save()

        create_diff(some_run, 'k1', 'k1_value', 'column', '1', '2', status=Diff.STATUS_CLOSED),
        create_diff(some_run, 'k1', 'k1_value', 'column', '3', '4'),
        create_diff(some_run, 'k1', 'k1_value', 'column2', '5', '6'),

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
        )
        assert response.status_code == 200
        assert response.data['success']
        assert response.data['count'] == 2

        expected_webhook_body = {
            'event_name': webhook_event,
            'payload': {
                'run_id': some_run.id,
                'changes': [
                    {
                        'column_name': 'column2',
                        'keys': ['k1_value'],
                        'changes': {
                            'issue_key': self.issue_key,
                            'status': Diff.STATUS_CLOSED,
                        },
                    },
                    {
                        'column_name': 'column',
                        'keys': ['k1_value'],
                        'changes': {
                            'issue_key': self.issue_key,
                            'status': Diff.STATUS_CLOSED,
                        },
                    },
                ],
            },
        }

        assert requests_mock.called
        assert requests_mock.call_count == 1

        request = requests_mock.last_request
        assert request.json() == expected_webhook_body


class TestRunMarkTest(BaseTestRequestFromTracker):
    @pytest.fixture(autouse=True)
    def setup(self):
        self.url = reverse('run-mark-test')

        self.secret = 'some-secret'

        self.issue_key = 'SSD-123456'
        self.user_id = 123456

    def test_access(self, api_client):
        response = self.make_request(api_client, queue_team=[])
        assert response.status_code == 403

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
            secret='invalid-secret',
        )
        assert response.status_code == 403

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
        )
        assert response.status_code == 200

    def test_mark_test(self, api_client, some_run):
        some_run.issue = f'{settings.TRACKER_HOST}/{self.issue_key}'
        some_run.save()
        assert some_run.type == Run.TYPE_PROD

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
        )
        assert response.status_code == 200
        assert response.data['success']

        some_run.refresh_from_db()
        assert some_run.type == Run.TYPE_TEST


class TestRunCalculateThresholds(BaseTestRequestFromTracker):
    @pytest.fixture(autouse=True)
    def setup(self):
        self.url = reverse('run-calculate-thresholds')

        self.secret = 'some-secret'

        self.issue_key = 'SSD-123456'
        self.user_id = 123456

    def test_access(self, api_client):
        response = self.make_request(api_client, queue_team=[])
        assert response.status_code == 403

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
            secret='invalid-secret',
        )
        assert response.status_code == 403

        response = self.make_request(
            api_client,
            queue_team=[
                self.user_id,
            ],
        )
        assert response.status_code == 200

    def test_calculate(self, api_client, some_run: Run):
        some_run.issue = f'{settings.TRACKER_HOST}/{self.issue_key}'
        some_run.save()

        some_run.check_model.material_threshold = 'v1:100'
        some_run.check_model.save()

        create_diff(some_run, 'k1', 'key1', 'v1', '10', '20')

        patch_target = f'{APP_PREFIX}.utils.tracker.tracker.create_comment'
        with mock.patch(patch_target) as m:
            response = self.make_request(
                api_client,
                queue_team=[
                    self.user_id,
                ],
            )
            assert response.status_code == 200

            call = m.call_args_list[-1]
            comment_text = call.args[-1]

        expected_text = (
            'Выполнен подсчет сумм расхождений:\n' '- %%v1%%: сумма расхождений 10.0000 не превышает сумму в 100'
        )
        assert expected_text == comment_text


@pytest.mark.usefixtures('nirvana_facade_mock', 'ylock_mock')
class TestNirvanaMonitoring:
    """
    Тестируем работу API в взаимодействии с тасками, которые монинотрят нирвану
    """

    @pytest.fixture(autouse=True)
    def mock_nirvana_monitoring(self):
        """
        Переопределяем фикстуру для включения задач celery,
        чтобы проверить работу API вместе с их логикой
        """
        pass

    @pytest.fixture
    def requests_mock_tvm_api_get(self, requests_mock, tvm_api_client):
        def text_callback(request: requests.request, context: dict):
            response = tvm_api_client.get(request.path, request.qs)
            context.status_code = response.status_code
            return response.status_text

        matcher = re.compile('http://')
        requests_mock.register_uri('GET', matcher, text=text_callback)

    def test_run_create(self, tvm_api_client, some_check):
        """
        Проверяем, что запущен мониторинг графа при создании запуска
        """
        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.check_run_status.delay') as m:
            run = create_run_via_http(tvm_api_client, some_check)

            m.assert_called_once()
            args, _kwargs = m.call_args
            assert args[0] == run['instance_id']

    def test_aa(self, tvm_api_client, some_check):
        """
        Проверяем, что запущен мониторинг графа авторазбора после переключения в статус
        """
        run = create_run(some_check, status=Run.STATUS_FILL_STABLE_DIFFS)
        run.aa_workflow_id, run.aa_instance_id = 'xxxxx', 'yyyyy'
        run_id = run.id

        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.check_run_status.delay') as m, mock.patch(
            f'{APP_PREFIX}.utils.common.start_auto_analyzer'
        ):
            response = tvm_api_client.get(
                reverse('run-switch-status', args=[run_id]),
                dict(token=make_status_token(run_id, Run.STATUS_FILL_STABLE_DIFFS, Run.STATUS_STARTED_AA)),
            )
            assert response.status_code == HTTP_200_OK
            m.assert_called_once()

            run = Run.objects.get(pk=run_id)
            args, _kwargs = m.call_args
            assert args[0] == run.aa_instance_id

    @pytest.mark.usefixtures('requests_mock_tvm_api_get', 'yt_client_mock')
    def test_full_cycle(self, tvm_api_client, some_check):
        """
        Проверяем полную цепочку переключений состояний от запуска сверки до ее конца
        """
        flow_status = f'{APP_PREFIX}.celery_app.tasks.NirvanaFacade.get_workflow_status'
        flow_status_completed = {
            'status': 'completed',
            'result': 'success',
        }

        with mock.patch(flow_status, return_value=flow_status_completed) as m:
            run = create_run_via_http(tvm_api_client, some_check)
            run_id = run['id']
            run = Run.objects.get(pk=run_id)

            m.assert_called()
            assert run.status == Run.STATUS_FINISHED
