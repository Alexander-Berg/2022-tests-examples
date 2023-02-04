from unittest import mock

import pytest
from django.urls import reverse
from django.test import override_settings
from django.core.management import call_command

from billing.dcsaap.backend.project.const import Env, APP_PREFIX


class TestPreRestore:
    """
    Тестирует подготовку перед копированием БД из прода
    """
    URL_PRE_RESTORE = reverse('pre-restore')

    @pytest.fixture(autouse=True)
    def rollback_post_restore_migration(self):
        call_command('migrate', 'pre_restore', 'zero')

    def test_abort_on_prod(self, tvm_api_client):
        """
        Проверяем блокирование запуска в проде
        """
        with override_settings(ENV_TYPE=Env.PROD), \
                mock.patch(f"{APP_PREFIX}.api.views.prerestore.call_command") as m:
            tvm_api_client.post(self.URL_PRE_RESTORE)
            m.assert_not_called()
