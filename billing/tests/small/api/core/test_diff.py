import datetime as dt

import pytest

from django.urls import reverse
from django.test import override_settings
from django.forms.models import model_to_dict

from rest_framework.status import HTTP_200_OK, HTTP_403_FORBIDDEN, HTTP_404_NOT_FOUND, HTTP_405_METHOD_NOT_ALLOWED

from billing.dcsaap.backend.core import enum
from billing.dcsaap.backend.core.models import Diff

from billing.dcsaap.backend.tests.utils.models import create_check, create_run, create_diff, create_webhook


class TestDiffAPI:
    @pytest.fixture(autouse=True)
    def setup(self, db):
        self.check = create_check("check1", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res")
        self.run = create_run(self.check)
        self.diff = create_diff(self.run, 'k1', 'k1_value', 'column', '1', '2')
        self.diff2 = create_diff(self.run, 'k', 'k_value', 'columnX', '9', '8', diff_type=Diff.TYPE_NOT_IN_T1)
        self.diff3 = create_diff(self.run, 'k2', 'k_value', 'columnY', '9', '8', diff_type=Diff.TYPE_NOT_IN_T2)

        self.check_not_mine = create_check(
            "check1", enum.ARNOLD, "/t1", "/t2", "k1 k2", "v1 v2", "/res", login='luke-skywalker'
        )
        self.run_not_mine = create_run(self.check_not_mine)
        self.diff_not_mine = create_diff(self.run_not_mine, 'k1', 'k1_value', 'column', '1', '2')

    def test_get_diff_for_owner(self, tvm_api_client):
        response = tvm_api_client.get(reverse('diff-detail', args=[self.diff.pk]))
        assert response.status_code == HTTP_200_OK
        assert response.data['id'] == self.diff.id
        assert response.data['run'] == self.run.id
        assert response.data['key1_name'] == 'k1'
        assert response.data['key1_value'] == 'k1_value'
        assert response.data['column_name'] == 'column'
        assert response.data['column_value1'] == '1'
        assert response.data['column_value2'] == '2'
        assert response.data['type'] == Diff.TYPE_DIFF
        assert response.data['type_str'] == "Расходятся"
        assert response.data['status'] == Diff.STATUS_NEW
        assert response.data['status_str'] == 'Новое'

    def test_edit_diff_for_owner(self, tvm_api_client):
        diff_edit_url = reverse('diff-detail', kwargs={'pk': self.diff2.pk})
        diff_update = model_to_dict(self.diff2)
        diff_update["issue_key"] = 'SSD-123456'

        r = tvm_api_client.put(diff_edit_url, diff_update, format='json')
        assert r.status_code == HTTP_200_OK, r.data

        response = tvm_api_client.get(reverse('diff-detail', args=[self.diff2.pk]))
        assert response.status_code == HTTP_200_OK
        assert response.data['id'] == self.diff2.id
        assert response.data['issue_key'] == 'SSD-123456'

    def test_get_diff_not_for_owner(self, tvm_api_client):
        response = tvm_api_client.get(reverse('diff-detail', args=[self.diff_not_mine.pk]))
        assert response.status_code == HTTP_404_NOT_FOUND

    def test_get_diff_list(self, tvm_api_client):
        response = tvm_api_client.get(reverse('diff-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 3

    def test_create_deny(self, tvm_api_client):
        response = tvm_api_client.post(reverse('diff-list'))
        assert response.status_code == HTTP_405_METHOD_NOT_ALLOWED

    def test_delete_deny(self, tvm_api_client):
        response = tvm_api_client.delete(reverse('diff-detail', args=[self.diff.pk]))
        assert response.status_code == HTTP_405_METHOD_NOT_ALLOWED

    def test_type_str(self, tvm_api_client):
        response = tvm_api_client.get(reverse('diff-detail', args=[self.diff2.pk]))
        assert response.status_code == HTTP_200_OK
        assert response.data['type_str'] == 'Нет в таб.1: t1'

        response = tvm_api_client.get(reverse('diff-detail', args=[self.diff3.pk]))
        assert response.status_code == HTTP_200_OK
        assert response.data['type_str'] == 'Нет в таб.2: t2'

    def test_webhook_sent(self, requests_mock, tvm_api_client):
        """
        Проверяем отправление изменений в баланс при редактировании расхождения
        """
        webhook_event = 'diffs.changed'
        webhook = create_webhook(webhook_event)
        requests_mock.register_uri('POST', webhook.url, text='OK')

        diff_edit_url = reverse('diff-detail', kwargs={'pk': self.diff2.pk})
        diff_update = model_to_dict(self.diff2)
        diff_update["issue_key"] = 'SSD-123456'

        r = tvm_api_client.put(diff_edit_url, diff_update, format='json')
        assert r.status_code == HTTP_200_OK, r.data

        expected_webhook_body = {
            'event_name': webhook_event,
            'payload': {
                'run_id': self.run.id,
                'changes': [
                    {
                        'column_name': self.diff2.column_name,
                        'keys': [self.diff2.key1_value],
                        'changes': {
                            'issue_key': diff_update["issue_key"],
                            'status': Diff.STATUS_NEW,
                        },
                    },
                ],
            },
        }

        assert requests_mock.called
        assert requests_mock.call_count == 1

        request = requests_mock.last_request
        assert request.json() == expected_webhook_body

    def test_fill_issue(self, tvm_api_client):
        issue = "http://st.yandex-team.ru/CHECK-1234"
        self.run.issue = issue
        self.run.save()

        diff_edit_url = reverse('diff-detail', kwargs={'pk': self.diff.id})
        diff_close = model_to_dict(self.diff)
        diff_close["status"] = Diff.STATUS_CLOSED

        r = tvm_api_client.put(diff_edit_url, diff_close, format='json')
        assert r.status_code == HTTP_200_OK, r.data

        self.diff.refresh_from_db()
        assert self.diff.issue_key == issue

    def test_partitions_limitation(self, tvm_api_client):
        diff = create_diff(self.run, 'k1', 'k1_value', 'column', '6', '7')
        diff.dt = dt.date.today() + dt.timedelta(days=3)
        diff.save()

        response = tvm_api_client.get(reverse('diff-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 4

        response = tvm_api_client.get(reverse('diff-list'), data={'run': self.run.id})
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 3


class TestDiffEditControl:
    """
    Проверяем доступ к расхождению.
    Создавать расхождение через API нельзя.

    - Аноним - ничего нельзя, даже смотреть
    - Суперпользователь - можно создавать, редактировать любую, смотреть любую
    - Представитель - можно создавать, редактировать и смотреть только свое
    - Аудит - создавать нельзя, редактировать ничего нельзя, смотреть можно всё
    """

    diff_list_url = reverse('diff-list')

    def test_edit_any_diff(
        self, super_yauser, int_audit_yauser, anon_yauser, yauser, some_diffs: [Diff], tvm_api_client
    ):
        """
        Проверяем, что редактировать запуск может только только владелец
        или суперпользователь
        """
        diff_edit_url = reverse('diff-detail', kwargs={'pk': some_diffs[0].id})
        diff_close = model_to_dict(some_diffs[0])
        diff_close["status"] = Diff.STATUS_CLOSED

        with override_settings(YAUTH_TEST_USER=super_yauser.login):
            r = tvm_api_client.put(diff_edit_url, diff_close, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=some_diffs[0].run.check_model.created_login):
            r = tvm_api_client.put(diff_edit_url, diff_close, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=int_audit_yauser.login):
            r = tvm_api_client.put(diff_edit_url, diff_close, format='json')
            assert r.status_code == HTTP_403_FORBIDDEN, r.data

        with override_settings(YAUTH_TEST_USER=yauser.login):
            r = tvm_api_client.put(diff_edit_url, diff_close, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

        with override_settings(YAUTH_TEST_USER=anon_yauser.login):
            r = tvm_api_client.put(diff_edit_url, diff_close, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

    def test_read_check(self, super_yauser, int_audit_yauser, anon_yauser, yauser, some_diffs: Diff, tvm_api_client):
        """
        Проверяем, что смотреть расхождение может только владелец, суперпользователь, аудит
        """
        diff_url = reverse('diff-detail', kwargs={'pk': some_diffs[0].id})

        with override_settings(YAUTH_TEST_USER=super_yauser.login):
            r = tvm_api_client.get(diff_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=some_diffs[0].run.check_model.created_login):
            r = tvm_api_client.get(diff_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=int_audit_yauser.login):
            r = tvm_api_client.get(diff_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=yauser.login):
            r = tvm_api_client.get(diff_url, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

        with override_settings(YAUTH_TEST_USER=anon_yauser.login):
            r = tvm_api_client.get(diff_url, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data
