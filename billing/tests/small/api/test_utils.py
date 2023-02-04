"""
Тестирование модуля backend.utils
"""
import datetime as dt
from typing import List
from unittest import mock

import pytest
import yt.wrapper as yt
from django.conf import settings
from django.test import override_settings
import jsonschema

from billing.dcsaap.backend import utils
from billing.dcsaap.backend.core.models import Run, Diff
from billing.dcsaap.backend.project.const import APP_PREFIX
from billing.dcsaap.backend.tests.utils.models import create_run, create_diff, create_check


class TestIsSuperUser:
    """
    Тестирование функции `is_superuser`
    """

    def test_for_super_user(self, super_yauser):
        assert utils.user.is_superuser(super_yauser)

    def test_for_just_user(self, yauser):
        assert not utils.user.is_superuser(yauser)

    def test_for_anonymous_user(self, anon_yauser):
        assert not utils.user.is_superuser(anon_yauser)

    def test_for_int_audit_user(self, int_audit_yauser):
        assert not utils.user.is_superuser(int_audit_yauser)


class TestIsAudit:
    """
    Тестирование функции `is_int_audit`
    """

    def test_for_super_user(self, super_yauser):
        assert not utils.user.is_int_audit(super_yauser)

    def test_for_just_user(self, yauser):
        assert not utils.user.is_int_audit(yauser)

    def test_for_anonymous_user(self, anon_yauser):
        assert not utils.user.is_int_audit(anon_yauser)

    def test_for_int_audit_user(self, int_audit_yauser):
        assert utils.user.is_int_audit(int_audit_yauser)

    def test_for_tvm_service_ticket(self, tvm_service):
        assert not utils.user.is_int_audit(tvm_service)


class TestAccessUtils:
    """
    Тестирование функций `is_able_to_see_all_checks, is_able_to_edit_all_checks`
    """

    def test_for_superuser(self, super_yauser):
        assert utils.user.is_able_to_edit_all_checks(super_yauser)
        assert utils.user.is_able_to_see_all_checks(super_yauser)

    def test_for_just_user(self, yauser):
        assert not utils.user.is_able_to_edit_all_checks(yauser)
        assert not utils.user.is_able_to_see_all_checks(yauser)

    def test_for_anonymous_user(self, anon_yauser):
        assert not utils.user.is_able_to_edit_all_checks(anon_yauser)
        assert not utils.user.is_able_to_see_all_checks(anon_yauser)

    def test_for_int_audit_user(self, int_audit_yauser):
        assert not utils.user.is_able_to_edit_all_checks(int_audit_yauser)
        assert utils.user.is_able_to_see_all_checks(int_audit_yauser)


def test_copy_diffs_to_internal_result(yt_client_mock, some_run: Run):
    """
    Тестирование копирования расхождений из внешней таблицы `Run.result`
      во внутреннюю `Run.internal_result_with_ttl`
    """
    with mock.patch('time.time', return_value=0):
        is_success = utils.common.copy_diffs_to_internal_result(some_run)
    assert is_success

    yt_client_mock().copy.assert_called_once_with(some_run.result, some_run.internal_result_with_ttl, force=True)

    expected_expiration_path = f'{some_run.internal_result_with_ttl}/@expiration_time'
    expected_expiration_time = 1000 * settings.YT_CACHE_TTL
    yt_client_mock().set.assert_called_once_with(expected_expiration_path, expected_expiration_time)


class TestDownloadDiffs:
    """
    Тестирование логики скачивания расхождений из таблиц в YT
    """

    @pytest.fixture
    def diffs(self, yt_client_read_table_mock, some_diffs: List[Diff]):
        yt_client_read_table_mock.set(some_diffs)
        return list(sorted(some_diffs, key=lambda d: d.id))

    @staticmethod
    def diffs_by_run(run: Run) -> List[Diff]:
        return list(Diff.objects.filter(run=run).order_by('id'))

    def test_download_successful(self, some_run: Run, diffs: List[Diff]):
        """
        Тестируем успешное скачивание расхождений
        """
        is_success = utils.common.download_diffs(some_run)
        assert is_success

        downloaded = self.diffs_by_run(some_run)
        assert downloaded == diffs

    def test_use_result_if_no_internal(self, yt_client_mock, some_run: Run):
        """
        Тестируем использование `Run.result` если таблица `Run.internal_result_with_ttl` отсутствует
        """
        yt_client_mock().exists.side_effect = [False, True]
        is_success = utils.common.download_diffs(some_run)
        assert is_success

        format_ = yt.JsonFormat(attributes={"encode_utf8": False})
        yt_client_mock().read_table.assert_called_once_with(some_run.result, unordered=False, format=format_)

    def test_download_after_aa(self, yt_client_mock, some_run: Run):
        """
        Тестируем скачивание расхождений после авторазбора (есть статус в расхождении в YT)
        """
        diffs_from_yt = [
            {
                "column_name": "turnover",
                "diff_type": Diff.TYPE_DIFF,
                "page_service_code": "AD100",
                "product_name": "AWAPS DSP",
                "t1_value": 5864969.44,
                "t2_value": 5864969.43,
                "status": str(Diff.STATUS_CLOSED),
                "issue_key": "CHECK-3140",
            },
            {
                "column_name": "turnover",
                "diff_type": Diff.TYPE_NOT_IN_T2,
                "page_service_code": "AD",
                "product_name": "DSP",
                "t1_value": 569.44,
                "t2_value": None,
            },
            {
                "column_name": "turnover",
                "diff_type": Diff.TYPE_NOT_IN_T1,
                "page_service_code": "AD",
                "product_name": "DSP",
                "t1_value": None,
                "t2_value": 123.3,
                "status": "",
                "issue_key": "",
            },
            {
                "column_name": "turnover",
                "diff_type": Diff.TYPE_NOT_IN_T1,
                "page_service_code": "ADXX",
                "product_name": "DSPXXX",
                "t1_value": None,
                "t2_value": 123.366,
                "status": None,
                "issue_key": None,
            },
        ]

        yt_client = yt_client_mock()
        yt_client.read_table.return_value = diffs_from_yt
        assert utils.common.download_diffs(some_run)

        assert 4 == Diff.objects.filter(run=some_run).count()
        qs = Diff.objects.filter(run=some_run, status=Diff.STATUS_CLOSED)
        assert 1 == qs.count()
        assert Diff.STATUS_CLOSED == qs.get().status
        assert 'CHECK-3140' == qs.get().issue_key

    @pytest.mark.parametrize(
        'diffs_closed_count, diffs_new_count, count_limit, diffs_new_expected',
        [
            pytest.param(0, 10, 5, 5, id='only_new'),
            pytest.param(15, 0, 5, 0, id='only_closed'),
            pytest.param(5, 5, 5, 5, id='new_and_closed'),
        ],
    )
    def test_download_diffs_above_limit(
        self,
        yt_client_mock,
        some_run: Run,
        diffs_closed_count: int,
        diffs_new_count: int,
        count_limit: int,
        diffs_new_expected: int,
    ):
        """
        Тестируем лимит расхождений при скачивании
        """

        diff_new = {
            "column_name": "turnover",
            "diff_type": Diff.TYPE_DIFF,
            "t1_value": 5864969.44,
            "t2_value": 5864969.43,
            "status": str(Diff.STATUS_NEW),
        }

        diff_closed = {
            "column_name": "turnover",
            "diff_type": Diff.TYPE_DIFF,
            "t1_value": 5864969.44,
            "t2_value": 5864969.43,
            "status": str(Diff.STATUS_CLOSED),
            "issue_key": "CHECK-3140",
        }

        # новые расхождения записываются после старых, а старые не влияют на лимит
        diffs_from_yt = ([diff_closed] * diffs_closed_count) + ([diff_new] * diffs_new_count)

        some_run.diffs_count_limit = count_limit

        yt_client = yt_client_mock()
        yt_client.read_table.return_value = diffs_from_yt
        yt_client.row_count.return_value = len(diffs_from_yt)
        assert utils.common.download_diffs(some_run)

        some_run.refresh_from_db()
        some_run.update_diffs_count()
        assert some_run.diffs_count_yt == diffs_new_count
        assert some_run.diffs_count == diffs_new_expected
        assert Diff.objects.filter(run=some_run, status=Diff.STATUS_CLOSED).count() == diffs_closed_count

    def test_table_will_be_sorted(self, yt_client_mock, some_run: Run):
        yt_client = yt_client_mock()

        yt_client.get_attribute.return_value = []
        assert utils.common.download_diffs(some_run)

        yt_client.get_attribute.return_value = [
            dict(name=Diff.issue_key.field_name),
        ]
        assert utils.common.download_diffs(some_run)

        yt_client.run_sort.assert_called_once_with(
            source_table=some_run.internal_result_with_ttl,
            sort_by=[
                dict(
                    name=Diff.issue_key.field_name,
                    # Это нужно, чтобы все null оказались в конце таблицы.
                    sort_order='descending',
                ),
            ],
        )


class TestEndStatusEmail:
    def test_disable(self, some_run: Run):
        """
        Проверяем, что отправку можно отключить
        """
        # В тестах, по умолчанию, рассылки отключены.
        # Убеждаемся, что дело до отправки не доходит.
        with mock.patch(f'{APP_PREFIX}.utils.email.send_email', return_value=True) as sm:
            assert not utils.email.send_end_status_email(some_run)
            assert 0 == sm.call_count

    @override_settings(END_STATUS_EMAIL_ENABLED=True)
    def test_send_email_failed(self, some_run: Run):
        """
        Проверяем результат, если сбойнула отправка письма
        """
        with mock.patch(f'{APP_PREFIX}.utils.email.send_email', return_value=False) as sm:
            assert not utils.email.send_end_status_email(some_run)
            assert 1 == sm.call_count

    @override_settings(END_STATUS_EMAIL_ENABLED=True)
    def test_send_email_ok(self):
        """
        Проверяем отправку письма
        """
        check_code = 'xxx_test'
        check = create_check(f'Some check ({check_code})', 'hahn', '/t1', '/t2', 'k1', 'v1', '/r', code=check_code)
        run = create_run(check)
        create_diff(run, 'some-key', 'not in left', 'value', None, 'right')
        test_hostname = 'some-test-host'
        run.done(True)
        run.update_diffs_count()
        run.issue = 'SSD-123'

        with mock.patch(f'{APP_PREFIX}.utils.email.send_email', return_value=True) as sm, mock.patch(
            'socket.gethostname', return_value=test_hostname
        ):
            assert utils.email.send_end_status_email(run)
            assert 1 == sm.call_count
            args, kwargs = sm.call_args_list[0]
            # см. settings.END_STATUS_EMAIL_RECIPIENT
            assert args[0] == ['balance-reward-dev@yandex-team.ru']
            assert args[1] == f'[{test_hostname}] Check {check_code} (ID {run.id}) finished with 1 new diffs'

            email_body = args[2].split('\n')
            assert email_body[0] == f'Сверка с id {run.id} успешно завершилась.'
            assert email_body[-1] == f'Задача запуска: {run.issue}'

    def test_send_email_with_new_settings(self, some_run: Run):
        """
        Проверяем изменение настроек
        """
        new_recipients = '1@com 2@com'
        new_sender = 'eee@com'
        new_server = 'xxx.yyy'

        with override_settings(
            END_STATUS_EMAIL_RECIPIENT=new_recipients,
            END_STATUS_EMAIL_SENDER=new_sender,
            EMAIL_SERVER=new_server,
            END_STATUS_EMAIL_ENABLED=True,
        ), mock.patch('smtplib.SMTP') as smtp_class:
            assert utils.email.send_end_status_email(some_run)

            assert 1 == smtp_class.call_count
            args, kwargs = smtp_class.call_args_list[0]
            assert new_server == args[0]

            smtp_instance = smtp_class().__enter__()
            assert 1 == smtp_instance.sendmail.call_count
            args, kwargs = smtp_instance.sendmail.call_args_list[0]
            assert new_sender == args[0]
            assert new_recipients.split() == args[1]


class TestCalculateMaterialThreshold:
    @staticmethod
    def text_from_mock(m):
        call = m.call_args_list[-1]
        return call.args[-1]

    def perform_test(self, run: Run):
        patch_target = f'{APP_PREFIX}.utils.tracker.tracker.create_comment'
        with mock.patch(patch_target) as m:
            utils.tracker.create_thresholds_comment(run)
            return self.text_from_mock(m)

    def test_amount_lower_threshold(self, some_run: Run):
        some_run.check_model.material_threshold = 'v1:100'

        create_diff(some_run, 'k1', 'key1', 'v1', '10', '20')
        create_diff(some_run, 'k1', 'key2', 'v1', '30', '40')

        comment_text = self.perform_test(some_run)
        expected_text = (
            'Выполнен подсчет сумм расхождений:\n' '- %%v1%%: сумма расхождений 20.0000 не превышает сумму в 100'
        )
        assert comment_text == expected_text

        # Проверяем с указанием единиц
        some_run.check_model.material_threshold = 'v1:100:руб.'

        comment_text = self.perform_test(some_run)
        expected_text = (
            'Выполнен подсчет сумм расхождений:\n'
            '- %%v1%%: сумма расхождений 20.0000 руб. не превышает сумму в 100 руб.'
        )
        assert comment_text == expected_text

    def test_amount_between_threshold(self, some_run: Run):
        some_run.check_model.material_threshold = 'v1:10,100'

        create_diff(some_run, 'k1', 'key1', 'v1', '10', '20')
        create_diff(some_run, 'k1', 'key2', 'v1', '30', '40')

        comment_text = self.perform_test(some_run)
        expected_text = (
            'Выполнен подсчет сумм расхождений:\n'
            '- %%v1%%: сумма расхождений 20.0000 превышает сумму в 10 и не превышает 100'
        )
        assert comment_text == expected_text

        # Проверяем с указанием единиц
        some_run.check_model.material_threshold = 'v1:10,100:руб.'

        comment_text = self.perform_test(some_run)
        expected_text = (
            'Выполнен подсчет сумм расхождений:\n'
            '- %%v1%%: сумма расхождений 20.0000 руб. превышает сумму в 10 руб. и не превышает 100 руб.'
        )
        assert comment_text == expected_text

    def test_amount_exceeded_threshold(self, some_run: Run):
        some_run.check_model.material_threshold = 'v1:100'

        create_diff(some_run, 'k1', 'key1', 'v1', '100', '200')
        create_diff(some_run, 'k1', 'key2', 'v1', '300', '400')

        comment_text = self.perform_test(some_run)
        expected_text = (
            'Выполнен подсчет сумм расхождений:\n' '- %%v1%%: сумма расхождений 200.0000 превышает сумму в 100'
        )
        assert comment_text == expected_text

        # Проверяем с указанием единиц
        some_run.check_model.material_threshold = 'v1:100:руб.'

        comment_text = self.perform_test(some_run)
        expected_text = (
            'Выполнен подсчет сумм расхождений:\n'
            '- %%v1%%: сумма расхождений 200.0000 руб. превышает сумму в 100 руб.'
        )
        assert comment_text == expected_text

    def test_multiple_thresholds(self, some_run: Run):
        some_run.check_model.material_threshold = 'v1:10 v2:100 v3'

        create_diff(some_run, 'k1', 'key1', 'v1', '10', '20')
        create_diff(some_run, 'k1', 'key1', 'v2', '10', '20')
        create_diff(some_run, 'k1', 'key2', 'v1', '30', '40')
        create_diff(some_run, 'k1', 'key2', 'v2', '30', '40')

        comment_text = self.perform_test(some_run)
        expected_text = (
            'Выполнен подсчет сумм расхождений:\n'
            '- %%v1%%: сумма расхождений 20.0000 превышает сумму в 10\n'
            '- %%v2%%: сумма расхождений 20.0000 не превышает сумму в 100\n'
            '- %%v3%%: no threshold specified'
        )
        assert comment_text == expected_text

    def test_unable_to_calculate(self, some_run: Run):
        some_run.check_model.material_threshold = 'v1:100'

        create_diff(some_run, 'k1', 'key1', 'v1', '10', '20', Diff.STATUS_CLOSED)
        create_diff(some_run, 'k1', 'key2', 'v1', '30', '40', Diff.STATUS_CLOSED)

        patch_target = f'{APP_PREFIX}.utils.tracker.tracker.create_comment'
        with mock.patch(patch_target) as m:
            utils.tracker.create_thresholds_comment(some_run)
            m.assert_not_called()


def test_iso_timedelta():
    start_dt = dt.datetime(2000, 1, 1)
    finish_dt = dt.datetime(2000, 1, 1, 10, 11, 12, 123456)

    result = utils.tracker.iso_timedelta(finish_dt - start_dt)
    expected = 'PT10H11M12S'
    assert result == expected


def test_format_issue_additional_params(some_run, some_diffs):
    some_run.check_model.debrief_queue = 'CHECK'
    result = utils.tracker.format_issue_additional_params(some_run)
    expected = {}
    assert result == expected

    some_run.check_model.code = 'check'
    some_run.check_model.debrief_queue = 'SSD'
    some_run.done(True)
    some_run.started = dt.datetime(2000, 1, 1, 0, 0, 0)
    some_run.finished = dt.datetime(2000, 1, 1, 10, 11, 12, 123456)
    result = utils.tracker.format_issue_additional_params(some_run)
    expected = {
        'assignee': 'robot-checkbot-test',
        'components': [
            1740,
        ],
        'checkName': 'check',
        'runId': some_run.id,
        'diffCount': 2,
        'checkMonth': '2000.01',
        'checkDate': '2000-01-01',
        'startTime': '2000-01-01T00:00:00',
        'endTime': '2000-01-01T10:11:12.123456',
        'verificationWorkTime': 'PT10H11M12S',
    }
    assert result == expected


class TestPrepareDataForTable:
    """
    Тестирование функции 'prepare_data_for_table'
    """

    @pytest.fixture
    def base_config(self):
        return {}

    @pytest.fixture
    def custom_config(self, base_config):
        base_config.update(
            {
                'type': {'title': 'Тип', 'hide': False},
                'status': {'title': 'Статус', 'hide': False},
                'key1_name': {'title': 'Ключ номер 1', 'hide': True},
                'key1_value': {'title': 'Ключ номер 1', 'hide': False},
                'key2_name': {'title': 'key2_name', 'hide': True},
                'key2_value': {'title': 'key2_value', 'hide': True},
                'key3_name': {'title': 'key3_name', 'hide': True},
                'key3_value': {'hide': True},
                'key4_name': {'hide': True},
                'key4_value': {'title': 'key4_value', 'hide': True},
                'key5_name': {'hide': True},
                'key5_value': {'title': 'key5_value', 'hide': True},
                'column_name': {'title': 'Столбец', 'hide': False},
                'column_value1': {'title': 'column_value1', 'hide': True},
                'column_value2': {'title': 'column_value2', 'hide': True},
                'issue_key': {'title': 'issue_key', 'hide': True},
                'close_dt': {'title': 'close_dt', 'hide': True},
            }
        )

    @pytest.fixture
    def test_with_config(self, some_run: Run, base_config):
        if base_config:
            some_run.check_model.excel_config = base_config

        create_diff(some_run, 'k1', 'key1', 'v1', '100', '200')
        create_diff(some_run, 'k1', 'key2', 'v1', '300', '400')

        data_to_check = utils.excel.prepare_data_for_table(some_run.diff_set.all(), some_run.check_model.excel_config)

        return data_to_check

    def test_with_default_config(self, some_run: Run, base_config, test_with_config):
        """
        Конфига нет. Проверяем, что подгрузились дефолтные значения колонок
        """
        data_expected = [
            [
                "type",
                "status",
                "key1_name",
                "key1_value",
                "key2_name",
                "key2_value",
                "key3_name",
                "key3_value",
                "key4_name",
                "key4_value",
                "key5_name",
                "key5_value",
                "column_name",
                "column_value1",
                "column_value2",
                "issue_key",
                "close_dt",
            ],
            [
                "Расходятся",
                "Новое",
                'k1',
                'key2',
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                'v1',
                '300',
                '400',
                None,
                None,
            ],
            [
                "Расходятся",
                "Новое",
                'k1',
                'key1',
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                'v1',
                '100',
                '200',
                None,
                None,
            ],
        ]
        assert data_expected == test_with_config

    def test_with_custom_config(self, some_run: Run, custom_config, test_with_config):
        """
        Конфиг указан. Проверяем, что обработаны параметры для каждой колонки
        """
        data_expected = [
            ["Тип", "Статус", "Ключ номер 1", "Столбец"],
            ["Расходятся", "Новое", 'key2', 'v1'],
            ["Расходятся", "Новое", 'key1', 'v1'],
        ]
        assert data_expected == test_with_config


class TestExcelConfig:
    """
    Проверяем валидацию email_config
    """

    def test_validate_default_config(self):
        jsonschema.validate(utils.excel.get_default_excel_config(), utils.excel.get_excel_config_json_schema())

    @pytest.fixture
    def excel_config(self):
        return {}

    @pytest.fixture
    def excel_config_wrong_key(self, excel_config):
        excel_config.update({"wrong_key": {"hide": True}})

    @pytest.fixture
    def excel_config_wrong_option(self, excel_config):
        excel_config.update({"status": {"wrong_option": "123abc"}})

    @pytest.fixture
    def excel_config_wrong_type(self, excel_config):
        excel_config.update({"column_name": {"hide": "Da"}})

    @pytest.fixture
    def test_validate_wrong(self, excel_config):
        with pytest.raises(jsonschema.ValidationError):
            jsonschema.validate(excel_config, utils.excel.get_excel_config_json_schema())

    def test_validate_wrong_key(self, excel_config_wrong_key, test_validate_wrong):
        pass

    def test_validate_wrong_option(self, excel_config_wrong_option, test_validate_wrong):
        pass

    def test_validate_wrong_type(self, excel_config_wrong_type, test_validate_wrong):
        pass


class TestYT:
    def test_has_column(self, yt_client_mock):
        client = yt_client_mock()
        client.get_attribute.return_value = [dict(name='column1')]

        assert utils.yt_.has_column('column1', '//foo/bar')
        assert not utils.yt_.has_column('column2', '//foo/bar')
