import pytest
from unittest import mock

from django.urls import reverse

from rest_framework.status import HTTP_200_OK, HTTP_404_NOT_FOUND

from billing.dcsaap.backend.core import enum
from billing.dcsaap.backend.core.models import Diff
from billing.dcsaap.backend.project.const import APP_PREFIX

from billing.dcsaap.backend.tests.utils.models import create_check, create_run, create_diff


class TestDiffsEmailAPI:
    """
    Проверяем, что при обращении в ручку вызываем функцию для отправки расхождений по сверке
    """

    @pytest.fixture(autouse=True)
    def setup(self, db):
        self.check = create_check("check1", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res")
        self.run = create_run(self.check)
        self.diff = create_diff(self.run, 'k1', 'k1_value', 'column', '1', '2')
        self.diff2 = create_diff(self.run, 'k', 'k_value', 'columnX', '9', '8', diff_type=Diff.TYPE_NOT_IN_T1)
        self.diff3 = create_diff(self.run, 'k2', 'k_value', 'columnY', '9', '8', diff_type=Diff.TYPE_NOT_IN_T2)

    def test_not_exists_run(self, tvm_api_client):
        post_args = {
            "run_id": -1,
            "login": "luke-skywalker",
        }
        response = tvm_api_client.post(reverse('diffs-email'), post_args, format='json')
        assert response.status_code == HTTP_404_NOT_FOUND
        assert response.data["message"] == "Run -1 is not found"
        assert not response.data["success"]

    def test_send_diffs(self, tvm_api_client):
        post_args = {
            "run_id": self.run.id,
            "login": "luke-skywalker",
        }

        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.send_diffs_by_email.delay') as email_diff_mock:
            response = tvm_api_client.post(reverse('diffs-email'), post_args, format='json')
            assert response.status_code == HTTP_200_OK
            assert response.data["success"]

            email_diff_mock.assert_called()
            args, kwargs = email_diff_mock.call_args
            assert args[0].id == self.run.id
            assert args[1] == "luke-skywalker"
