import pytest
from unittest import mock

from django.urls import reverse
from django.db import connection
from django.core.management import call_command
from django.test import override_settings

from billing.dcsaap.backend.core import enum
from billing.dcsaap.backend.core.models import Check
from billing.dcsaap.backend.tests.utils.models import create_check
from billing.dcsaap.backend.project.const import Env, APP_PREFIX


class TestPostRestore:
    """
    Тестирует накатывание тестовых данных после копирования БД из прода
    """
    URL_POST_RESTORE = reverse('post-restore')

    @pytest.fixture(autouse=True)
    def enable_post_restore_migration(self, settings):
        settings.MIGRATION_MODULES = {}

    @pytest.fixture(autouse=True)
    def enable_foreign_keys_sqlite(self, django_db_blocker):
        with django_db_blocker.unblock():
            with connection.cursor() as c:
                c.executescript('''PRAGMA foreign_keys = 1;''')

    @pytest.fixture(autouse=True)
    def rollback_post_restore_migration(self):
        call_command('migrate', 'post_restore', 'zero')

    @staticmethod
    def create_check_with(title: str, table1: str = "/t1", table2: str = "/t2", result: str = "/res",
                          status: int = Check.STATUS_ENABLED,
                          aa_workflow_id: str = None, aa_instance_id: str = None):
        return create_check(title, enum.HAHN, table1, table2, 'k1 k2', 'v1 v2', result,
                            status=status, aa_workflow_id=aa_workflow_id, aa_instance_id=aa_instance_id)

    def test_yt_paths(self, tvm_api_client):
        """
        Проверяем замену продовых путей YT на тестовые
        """
        yt_prod_prefix = "//home/balance_reports/dcsaap/prod"
        yt_test_prefix = "//home/balance_reports/dcsaap/test"
        yt_default_prefix = "//home/balance/dev/shorrty"

        self.create_check_with(
            'raz',
            table1=f'{yt_prod_prefix}/aboba/data1',
            table2=f'{yt_prod_prefix}/aboba/data2',
            result=f'{yt_prod_prefix}/aboba/diff',
        )
        self.create_check_with(
            'dva',
            table1='//home/balance/odin/dva',
            table2='//home/balance/dcsaap/test/aboba/data2',
            result='//home/junk/dcsaap/prod/aboba/diff_latest',
        )

        tvm_api_client.post(self.URL_POST_RESTORE)

        check = Check.objects.get(title="raz")

        assert check.table1.startswith(yt_test_prefix)
        assert check.table2.startswith(yt_test_prefix)
        assert check.result.startswith(yt_test_prefix)

        check = Check.objects.get(title="dva")

        assert check.table1.startswith(yt_default_prefix)
        assert check.table2.startswith(yt_default_prefix)
        assert check.result.startswith(yt_default_prefix)

    def test_checks_disabled(self, tvm_api_client):
        """
        Проверяем, что сверки отключены
        """
        self.create_check_with("check1", status=Check.STATUS_ENABLED)
        self.create_check_with("check2", status=Check.STATUS_ENABLED)
        self.create_check_with("check3", status=Check.STATUS_DISABLED)

        tvm_api_client.post(self.URL_POST_RESTORE)

        assert Check.objects.get(title="check1").status == Check.STATUS_DISABLED
        assert Check.objects.get(title="check2").status == Check.STATUS_DISABLED
        assert Check.objects.get(title="check3").status == Check.STATUS_DISABLED

    def test_workflows_replaced(self, tvm_api_client):
        """
        Проверяем, что workflow_id, instance_id заменены
        """
        check = self.create_check_with(
            '1',
            aa_workflow_id='aaa', aa_instance_id='bbb',
        )

        initial_workflow_id = check.workflow_id
        initial_instance_id = check.instance_id

        tvm_api_client.post(self.URL_POST_RESTORE)

        check = Check.objects.get(title='1')

        assert check.workflow_id != initial_workflow_id and check.instance_id != initial_instance_id
        assert not check.aa_workflow_id and not check.aa_instance_id

    def test_abort_on_prod(self, tvm_api_client):
        """
        Проверяем блокирование запуска в проде
        """
        with override_settings(ENV_TYPE=Env.PROD), \
                mock.patch(f"{APP_PREFIX}.api.views.postrestore.call_command") as m:
            tvm_api_client.post(self.URL_POST_RESTORE)
            m.assert_not_called()
