from collections import OrderedDict, namedtuple
from unittest import mock

import pytest

from django.urls import reverse
from django.conf import settings
from django.test import override_settings
from django.forms.models import model_to_dict

from rest_framework.status import (
    HTTP_200_OK,
    HTTP_201_CREATED,
    HTTP_400_BAD_REQUEST,
    HTTP_403_FORBIDDEN,
    HTTP_404_NOT_FOUND,
    HTTP_409_CONFLICT,
)

import reactor_client

from billing.dcsaap.backend.core import enum
from billing.dcsaap.backend.core.models import Check, CheckPrepareRun
from billing.dcsaap.backend.project.const import APP_PREFIX
from billing.dcsaap.backend.utils.excel import get_default_excel_config
from billing.dcsaap.backend.api import nirvana


# URL по которому можно получить список сверок (GET запрос) или
# создать новую сверку (POST запрос)
URL_LIST_CHECKS = reverse('check-list')


class TestCheckGet:
    """
    Тестирование GET запросов для описаний сверок
    """

    insignificant_fields = [
        'id',
        'url',
        'created',
        'changed',
        'status_str',
    ]

    @staticmethod
    def create_check(
        title,
        cluster,
        table1,
        ticket1,
        table2,
        ticket2,
        keys,
        columns,
        result,
        debrief_queue,
        change_ticket,
        is_sox,
        status=Check.STATUS_ENABLED,
        login=settings.YAUTH_TEST_USER,
        aa_workflow_id=None,
        aa_instance_id=None,
        code=None,
        material_threshold=None,
        diff_threshold=None,
        excel_config={"type": {}},
        diffs_count_limit=1_000_000,
    ):
        return dict(
            title=title,
            cluster=cluster,
            table1=table1,
            ticket1=ticket1,
            table2=table2,
            ticket2=ticket2,
            keys=keys,
            columns=columns,
            result=result,
            debrief_queue=debrief_queue,
            status=status,
            created_login=login,
            changed_login=login,
            workflow_id='yyyy',
            instance_id='xxxx',
            operation_id=None,
            change_ticket=change_ticket,
            is_sox=is_sox,
            aa_workflow_id=aa_workflow_id,
            aa_instance_id=aa_instance_id,
            aa_operation_id=None,
            code=code,
            material_threshold=material_threshold,
            diff_threshold=diff_threshold,
            prep_workflow_id=None,
            prep_instance_id=None,
            prep_operation_id=None,
            excel_config=excel_config,
            diffs_count_limit=diffs_count_limit,
        )

    def normalize_for_eq_check(self, retrieved_record: OrderedDict):
        record = dict(retrieved_record)
        for f in self.insignificant_fields:
            record.pop(f)
        return record

    def get_checks(self, login: str = None):
        checks = [
            self.create_check(
                "check1",
                enum.ARNOLD,
                "/t1",
                "OLOLO-666",
                "/t2",
                "ECHPOCHMAK-007",
                "k1 k2",
                "v1 v2",
                "/res",
                "GOWRONG",
                "A",
                False,
                code="check1",
            ),
            self.create_check(
                "check2",
                enum.HAHN,
                "/t1",
                "OLOLO-667",
                "/t2",
                "ECHPOCHMAK-008",
                "k1 k2",
                "v1 v2",
                "/res",
                "SSD",
                "A",
                False,
                code="check2",
            ),
            self.create_check(
                "check3",
                enum.HAHN,
                "/t11",
                "OLOLO-666",
                "/t12",
                "ECHPOCHMAK-008",
                "k11 k12",
                "v11 v12",
                "/res1",
                "SSD",
                "A",
                False,
                Check.STATUS_DISABLED,
            ),
            # эту сверку найти в тесте не должны, т.к. она создана другим пользователем
            # (luke-skywalker), а работаем мы под другим (см. settings.YAUTH_TEST_USER)
            self.create_check(
                "check4",
                enum.HAHN,
                "/t11",
                "DISK-666",
                "/t12",
                "BALANCE-008",
                "k11 k12",
                "v11 v12",
                "/res1",
                "SSD",
                "A",
                False,
                Check.STATUS_ENABLED,
                'luke-skywalker',
            ),
            # проверяем корректное сохранение json для excel_config
            self.create_check(
                "check5",
                enum.HAHN,
                "/t1",
                "OLOLO-654",
                "/t2",
                "итьECHPOCHMAK-228",
                "k1 k2",
                "v1 v2",
                "/res",
                "SSD",
                "A",
                False,
                excel_config={
                    "type": {"title": "Типо тип", "hide": False},
                    "key5_name": {"hide": True},
                    "key5_value": {"hide": True},
                    "status": {"title": "Статус/Состояние"},
                },
            ),
        ]
        return [c for c in checks if c['created_login'] == login or login is None]

    @pytest.fixture(autouse=True)
    def setup(self, db):
        for fake_record in self.get_checks():
            Check.objects.create(**fake_record)

    def test_get_all_checks(self, tvm_api_client):
        """
        Проверяет, что:
        - показываем все нужные поля сверки
        - показываем сверки, созданные только текущим пользователем
        """
        response = tvm_api_client.get(URL_LIST_CHECKS)
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 4
        test_data = self.get_checks(login=settings.YAUTH_TEST_USER)
        for got, expected in zip(reversed(test_data), response.data['results']):
            assert got == self.normalize_for_eq_check(expected)

    def test_get_disables_check(self, tvm_api_client):
        """
        Проверяет, что:
        - показываем в том числе и выключенные сверки
        """
        response = tvm_api_client.get(URL_LIST_CHECKS)
        assert response.status_code == HTTP_200_OK

        checks = response.data['results']
        disabled_check = [c for c in checks if c['status'] == Check.STATUS_DISABLED]
        assert len(disabled_check) == 1

    def test_get_history(self, tvm_api_client):
        """
        Проверяем просмотр истории через API
        """
        response = tvm_api_client.get(reverse('checkhistory-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 4

        test_data = self.get_checks(login=settings.YAUTH_TEST_USER)
        for t in test_data:
            t["origin"] = Check.objects.get(title=t["title"]).id
        for got, expected in zip(reversed(test_data), response.data['results']):
            assert got == self.normalize_for_eq_check(expected)
            if got['status'] == Check.STATUS_DISABLED:
                assert 'Выключен' == expected['status_str']
            if got['status'] == Check.STATUS_ENABLED:
                assert 'Включён' == expected['status_str']

    def test_find_check_by_code(self, tvm_api_client):
        """
        Проверяем поиск сверки по коду
        """
        url_template = f'{URL_LIST_CHECKS}?code={{}}'

        cases = (
            ('check1', 1),
            ('check2', 1),
            ('check3', 0),
        )
        for check_code, expected_count in cases:
            response = tvm_api_client.get(url_template.format(check_code))
            assert response.status_code == HTTP_200_OK
            assert response.data['count'] == expected_count

    @pytest.mark.parametrize(
        'field_name,expected',
        (
            ('cluster', enum.HAHN),
            ('diffs_count_limit', 1_000_000),
        ),
    )
    def test_default(self, tvm_api_client, field_name, expected):
        """
        Проверяем, что значения по-умолчанию корректно устанавливаются.
        """
        check = self.create_check(
            "check1",
            enum.ARNOLD,
            "/t1",
            "OLOLO-666",
            "/t2",
            "ECHPOCHMAK-007",
            "k1 k2",
            "v1 v2",
            "/res",
            "GOWRONG",
            "A",
            False,
            code='test-default',
        )
        check.pop(field_name, None)

        url = reverse('check-list')

        response = tvm_api_client.post(url, check, format='json')
        assert response.status_code == HTTP_201_CREATED, response.data

        assert expected == response.data[field_name]

    def test_result_required(self, tvm_api_client):
        """
        Проверяем, что нельзя создать сверку без `result`.
        """
        success_check = self.create_check(
            "check1",
            enum.ARNOLD,
            "/t1",
            "OLOLO-666",
            "/t2",
            "ECHPOCHMAK-007",
            "k1 k2",
            "v1 v2",
            "/res",
            "GOWRONG",
            "A",
            False,
        )
        success_check['code'] = 'check-result-with-success-code'

        fail_check = dict(success_check)
        fail_check['code'] = 'check-result-with-fail-code'
        fail_check.pop('result')

        url = reverse('check-list')

        response = tvm_api_client.post(url, success_check, format='json')
        assert response.status_code == HTTP_201_CREATED, response.data

        response = tvm_api_client.post(url, fail_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST, response.data


class TestCheckPost:
    """
    Тестирование создания описаний сверок
    """

    def test_create_check(self, tvm_api_client):
        # Проверяем, что сверок нет
        response = tvm_api_client.get(URL_LIST_CHECKS)
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 0

        values = (
            ('title', "New testing check"),
            ('cluster', enum.ARNOLD),
            ('table1', '/dev/t1'),
            ('table2', '/dev/t2'),
            ('keys', 'k1 k2 k3'),
            ('columns', 'v1 v2'),
            ('result', '/dev/res'),
            ('status', Check.STATUS_DISABLED),
            ('instance_id', 'xxxx'),
            ('workflow_id', 'yyyy'),
        )

        # новая сверка
        new_check = {}
        for k, v in values:
            new_check[k] = v

        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_201_CREATED

        response = tvm_api_client.get(URL_LIST_CHECKS)
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 1
        assert response.data['results'][0]['created_login'] == settings.YAUTH_TEST_USER

        check = response.data['results'][0]
        for k, v in values:
            assert check[k] == v

    def test_create_fail_too_much_keys(self, tvm_api_client):
        values = (
            ('title', "New testing check"),
            ('cluster', enum.ARNOLD),
            ('table1', '/dev/t1'),
            ('table2', '/dev/t2'),
            ('keys', 'k1 k2 k3 k4 k5 k6'),
            ('columns', 'v1 v2'),
            ('result', '/dev/res'),
            ('status', Check.STATUS_DISABLED),
            ('instance_id', 'xxxx'),
            ('workflow_id', 'yyyy'),
        )

        # новая сверка
        new_check = {}
        for k, v in values:
            new_check[k] = v

        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST

    def test_create_fail_operation_and_instance(self, tvm_api_client):
        values = (
            ('title', "New testing check"),
            ('cluster', enum.ARNOLD),
            ('table1', '/dev/t1'),
            ('table2', '/dev/t2'),
            ('keys', 'k1 k2 k3 k4 k5 k6'),
            ('columns', 'v1 v2'),
            ('result', '/dev/res'),
            ('status', Check.STATUS_DISABLED),
            ('instance_id', 'xxxx'),
            ('workflow_id', 'yyyy'),
            ('operation_id', 'opop'),
        )

        # новая сверка
        new_check = {}
        for k, v in values:
            new_check[k] = v

        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST

    def test_material_thresholds(self, tvm_api_client):
        values = (
            ('title', "New testing check"),
            ('cluster', enum.ARNOLD),
            ('table1', '/dev/t1'),
            ('table2', '/dev/t2'),
            ('keys', 'k1 k2 k3'),
            ('columns', 'v1 v2'),
            ('result', '/dev/res'),
            ('status', Check.STATUS_DISABLED),
            ('instance_id', 'xxxx'),
            ('workflow_id', 'yyyy'),
        )

        # новая сверка
        new_check = {}
        for k, v in values:
            new_check[k] = v

        # test success
        new_check['material_threshold'] = 'v1:1000'
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_201_CREATED

        # test invalid columns
        new_check['material_threshold'] = 'v3:1000'
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST
        assert response.data['material_threshold'][0] == 'Column "v3" is not contained in the columns list'

        # test invalid threshold
        new_check['material_threshold'] = 'v1'
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST
        assert response.data['material_threshold'][0] == 'Invalid threshold "v1": no threshold specified'

    def test_diff_thresholds(self, tvm_api_client):
        values = (
            ('title', "New testing check"),
            ('cluster', enum.ARNOLD),
            ('table1', '/dev/t1'),
            ('table2', '/dev/t2'),
            ('keys', 'k1 k2 k3'),
            ('columns', 'v1 v2'),
            ('result', '/dev/res'),
            ('status', Check.STATUS_DISABLED),
            ('instance_id', 'xxxx'),
            ('workflow_id', 'yyyy'),
        )

        # новая сверка
        new_check = {}
        for k, v in values:
            new_check[k] = v

        # test success
        new_check['diff_threshold'] = 'v1:1000.1% v2:123'
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_201_CREATED

        # test invalid columns
        new_check['diff_threshold'] = 'v3:1000'
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST
        assert response.data['diff_threshold'][0] == 'Column "v3" is not contained in the columns list'

        # test invalid threshold
        new_check['diff_threshold'] = 'v1'
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST
        assert response.data['diff_threshold'][0] == 'Invalid threshold "v1"'

        # test invalid threshold negative value
        new_check['diff_threshold'] = 'v1:-1.23'
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST
        assert response.data['diff_threshold'][0] == 'Value "-1.23" must be a positive float or integer number'

        # test invalid threshold wrong value
        new_check['diff_threshold'] = 'v1:1.1.1'
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST
        assert response.data['diff_threshold'][0] == 'Value "1.1.1" must be a number'

    @pytest.mark.parametrize(
        argnames="lifecycle_status,is_sox,response_status_code",
        argvalues=[
            ('approved', False, HTTP_201_CREATED),
            ('approved', True, HTTP_201_CREATED),
            ('draft', False, HTTP_201_CREATED),
            ('draft', True, HTTP_400_BAD_REQUEST),
        ],
    )
    def test_workflow_instance_for_sox_check_in_draft_status(
        self,
        tvm_api_client,
        nirvana_api_mock,
        lifecycle_status,
        is_sox,
        response_status_code,
    ):
        values = (
            ('title', "New testing check"),
            ('cluster', enum.ARNOLD),
            ('table1', '/dev/t1'),
            ('table2', '/dev/t2'),
            ('keys', 'k1 k2 k3'),
            ('columns', 'v1 v2'),
            ('result', '/dev/res'),
            ('status', Check.STATUS_DISABLED),
            ('instance_id', 'xxxx'),
            ('workflow_id', 'yyyy'),
        )

        # новая сверка
        new_check = {}
        for k, v in values:
            new_check[k] = v

        return_value = {'lifecycleStatus': lifecycle_status}
        nirvana_api_mock.get_workflow_meta_data.return_value = return_value
        new_check['is_sox'] = is_sox
        response = tvm_api_client.post(URL_LIST_CHECKS, new_check, format='json')
        assert response.status_code == response_status_code


class TestCheckPostItem:
    """
    Тестирование изменения описания сверки
    """

    def test_update_check(self, tvm_api_client):
        response = tvm_api_client.get(URL_LIST_CHECKS)
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 0

        values = (
            ('title', "New testing check"),
            ('cluster', enum.ARNOLD),
            ('table1', '/dev/t1'),
            ('table2', '/dev/t2'),
            ('keys', 'k1 k2 k3'),
            ('columns', 'v1 v2'),
            ('result', '/dev/res'),
            ('status', Check.STATUS_DISABLED),
            ('instance_id', 'xxxx'),
            ('workflow_id', 'yyyy'),
        )

        # новая сверка
        check = {}
        for k, v in values:
            check[k] = v

        response = tvm_api_client.post(URL_LIST_CHECKS, check, format='json')
        assert response.status_code == HTTP_201_CREATED

        # обновляем сверку
        check['cluster'] = enum.HAHN
        response = tvm_api_client.put(reverse('check-detail', kwargs={'pk': 1}), check, format='json')
        assert response.status_code == HTTP_200_OK

        # проверяем изменения
        response = tvm_api_client.get(reverse('check-detail', kwargs={'pk': 1}))
        assert response.status_code == HTTP_200_OK
        assert response.data['cluster'] == enum.HAHN

    def test_update_sox_check(self, tvm_api_client, some_check: Check):
        """
        Проверяем обновление SOX сверки. В частности, что работают проверки тикета.

        См. CHECK-3430
        """
        id = some_check.pk
        check = model_to_dict(some_check)

        # обновляем сверку: выставляем признак SOX
        check['is_sox'] = True

        # проверяем валидацию URL
        bad_ticket_urls = (
            "https://google.com",
            "ftp://st.yandex-team.ru",
        )
        for ticket_url in bad_ticket_urls:
            check['change_ticket'] = ticket_url
            response = tvm_api_client.put(reverse('check-detail', kwargs={'pk': id}), check, format='json')
            assert response.status_code == HTTP_400_BAD_REQUEST

        # Моки ответов для запросов в ST
        GetResponse = namedtuple('GetResponse', ['status_code'])
        get_response_ok = GetResponse(status_code=200)
        get_response_fail = GetResponse(status_code=400)

        good_ticket_url = 'https://st.test.yandex-team.ru/CHECK-0'
        check['change_ticket'] = good_ticket_url

        # Если найти не получилось указанный тикет,
        # то обновления в БД не должно произойти
        with mock.patch('requests.get', return_value=get_response_fail) as rg:
            response = tvm_api_client.put(reverse('check-detail', kwargs={'pk': id}), check, format='json')
            rg.assert_called()
            assert response.status_code == HTTP_400_BAD_REQUEST

            # проверяем изменения
            response = tvm_api_client.get(reverse('check-detail', kwargs={'pk': id}))
            assert not response.data['is_sox']
            assert response.data['change_ticket'] is None

        # Если указанный тикет нашли, то в БД должны обновить данные
        with mock.patch('requests.get', return_value=get_response_ok) as rg:
            response = tvm_api_client.put(reverse('check-detail', kwargs={'pk': id}), check, format='json')
            rg.assert_called()
            assert response.status_code == HTTP_200_OK

            # проверяем изменения
            response = tvm_api_client.get(reverse('check-detail', kwargs={'pk': id}))
            assert response.status_code == HTTP_200_OK
            assert response.data['is_sox']
            assert response.data['change_ticket'] == good_ticket_url


class TestCheckAccess:
    @staticmethod
    def checks_visible_by_user(client, user):
        with override_settings(YAUTH_TEST_USER=user):
            response = client.get(URL_LIST_CHECKS)
        return [r['id'] for r in response.data['results']]

    def is_visible_by_users(self, client, check, *args):
        return [check.id in self.checks_visible_by_user(client, user) for user in args]

    def test_permissions_creep(self, tvm_api_client, some_check):
        check_access_url = reverse('checkaccess-list')

        author = some_check.created_login
        new_participant = "second"
        another_new_participant = "third"
        all_three = [author, new_participant, another_new_participant]

        assert self.is_visible_by_users(tvm_api_client, some_check, *all_three) == [True, False, False]

        with override_settings(YAUTH_TEST_USER=author):
            r = tvm_api_client.post(check_access_url, {'check_model': some_check.id, 'login': new_participant})
            assert r.status_code == 201, r.data
            for f in ('created_login', 'created', 'changed_login', 'changed'):
                assert len(r.data[f]), r.data
        assert self.is_visible_by_users(tvm_api_client, some_check, *all_three) == [True, True, False]

        with override_settings(YAUTH_TEST_USER=new_participant):
            r = tvm_api_client.post(check_access_url, {'check_model': some_check.id, 'login': another_new_participant})
            assert r.status_code == 201, r.data
        assert self.is_visible_by_users(tvm_api_client, some_check, *all_three) == [True, True, True]


class TestCheckEditControl:
    """
    Проверяем доступ к описанию сверки.

    - Аноним - ничего нельзя, даже смотреть
    - Суперпользователь - можно создавать, редактировать любую, смотреть любую
    - Представитель - можно создавать, редактировать и смотреть только свое
    - Аудит - создавать нельзя, редактировать ничего нельзя, смотреть можно всё
    """

    def test_edit_any_check(self, super_yauser, int_audit_yauser, anon_yauser, yauser, some_check, tvm_api_client):
        """
        Проверяем, что редактировать сверку может только только владелец
        или суперпользователь
        """
        check_edit_url = reverse('check-detail', kwargs={'pk': some_check.id})
        check_disable = model_to_dict(some_check)
        check_disable["status"] = Check.STATUS_DISABLED

        with override_settings(YAUTH_TEST_USER=super_yauser.login):
            r = tvm_api_client.put(check_edit_url, check_disable, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=some_check.created_login):
            r = tvm_api_client.put(check_edit_url, check_disable, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=int_audit_yauser.login):
            r = tvm_api_client.put(check_edit_url, check_disable, format='json')
            assert r.status_code == HTTP_403_FORBIDDEN, r.data

        with override_settings(YAUTH_TEST_USER=yauser.login):
            r = tvm_api_client.put(check_edit_url, check_disable, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

        with override_settings(YAUTH_TEST_USER=anon_yauser.login):
            r = tvm_api_client.put(check_edit_url, check_disable, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

    def test_create_check(self, super_yauser, int_audit_yauser, anon_yauser, yauser, some_check, tvm_api_client):
        """
        Проверяем, что создать сверку может только только владелец
        или суперпользователь
        """
        check_clone = model_to_dict(some_check)
        for k in ('created_login', 'changed_login'):
            check_clone.pop(k)

        with override_settings(YAUTH_TEST_USER=super_yauser.login):
            r = tvm_api_client.post(URL_LIST_CHECKS, check_clone, format='json')
            assert r.status_code == HTTP_201_CREATED, r.data

        with override_settings(YAUTH_TEST_USER=int_audit_yauser.login):
            r = tvm_api_client.post(URL_LIST_CHECKS, check_clone, format='json')
            assert r.status_code == HTTP_403_FORBIDDEN, r.data

        # TODO: тут и ниже должно быть 403.
        # Сейчас мы таких отбрасываем на уровне UI,
        # но надо так же и на уровне API сделать проверку
        # (см. backend.api.permissions.HasRoleInSystem)
        with override_settings(YAUTH_TEST_USER=yauser.login):
            r = tvm_api_client.post(URL_LIST_CHECKS, check_clone, format='json')
            assert r.status_code == HTTP_201_CREATED, r.data

        with override_settings(YAUTH_TEST_USER=anon_yauser.login):
            r = tvm_api_client.post(URL_LIST_CHECKS, check_clone, format='json')
            assert r.status_code == HTTP_201_CREATED, r.data

    def test_read_check(self, super_yauser, int_audit_yauser, anon_yauser, yauser, some_check: Check, tvm_api_client):
        """
        Проверяем, что смотреть сверку может только владелец, суперпользователь, аудит
        """
        check_url = reverse('check-detail', kwargs={'pk': some_check.id})

        with override_settings(YAUTH_TEST_USER=super_yauser.login):
            r = tvm_api_client.get(check_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=some_check.created_login):
            r = tvm_api_client.get(check_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=int_audit_yauser.login):
            r = tvm_api_client.get(check_url, format='json')
            assert r.status_code == HTTP_200_OK, r.data

        with override_settings(YAUTH_TEST_USER=yauser.login):
            r = tvm_api_client.get(check_url, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data

        with override_settings(YAUTH_TEST_USER=anon_yauser.login):
            r = tvm_api_client.get(check_url, format='json')
            assert r.status_code == HTTP_404_NOT_FOUND, r.data


class TestCheckPrepare:
    """
    Тесты для API для регистрации запусков подготовки данных
    """

    check_prepare_url = reverse('checkpreparerun-list')

    @override_settings(CELERY_TASK_ALWAYS_EAGER=True)
    def test_create(self, tvm_api_client, some_check: Check):
        """
        Проверяем, что для создания запуска достаточно лишь ссылки на сверку.
        Остальные поля будут выставлены автоматически.

        Так же убеждаемся, что вызывается celery задача для запуска нирвана процесса.
        """

        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.run_prepare_data.delay') as prepare_mock:
            r = tvm_api_client.post(self.check_prepare_url, {'check_model': some_check.id})
            assert r.status_code == HTTP_201_CREATED, r.data
            prepare_mock.assert_called()

            for f in ('created_login', 'created', 'check_model'):
                assert len(str(r.data[f])), r.data

        prepare_count = CheckPrepareRun.objects.filter(check_model_id=some_check.id).count()
        assert 1 == prepare_count

    def test_create_while_running(self, tvm_api_client, some_check: Check):
        """
        Проверяем, если уже какой-то запуск сделан для подготовки данных, то второй сделать нельзя
        """

        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.run_prepare_data.delay') as prepare_mock:
            r = tvm_api_client.post(self.check_prepare_url, {'check_model': some_check.id})
            assert r.status_code == HTTP_201_CREATED, r.data
            prepare_mock.assert_called()

        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.run_prepare_data.delay') as prepare_mock:
            r = tvm_api_client.post(self.check_prepare_url, {'check_model': some_check.id})
            assert r.status_code == HTTP_409_CONFLICT, r.data
            prepare_mock.assert_not_called()

    def test_update(self, tvm_api_client, some_check: Check):
        """
        Проверяем, что работает (через API) указание окончания запуска
        """

        # запустили
        with mock.patch(f'{APP_PREFIX}.celery_app.tasks.run_prepare_data.delay') as prepare_mock:
            r = tvm_api_client.post(self.check_prepare_url, {'check_model': some_check.id})
            assert r.status_code == HTTP_201_CREATED, r.data
            prepare_mock.assert_called()

        # проверили, что запуск появился
        r = tvm_api_client.get(self.check_prepare_url)
        assert r.status_code == HTTP_200_OK, r.data
        assert r.data['count'] == 1, r.data

        prepare_run = r.data['results'][0]
        assert prepare_run['check_model'] == some_check.id, r.data
        assert prepare_run['finished'] is None

        # обновляем дату окончания
        prepare_run_update = dict(prepare_run)
        prepare_run_update['finished'] = prepare_run_update['created']
        r = tvm_api_client.put(
            reverse('checkpreparerun-detail', kwargs={'pk': prepare_run['id']}), prepare_run_update, format='json'
        )
        assert r.status_code == HTTP_200_OK, r.data

        # проверили, что обновилась дата
        r = tvm_api_client.get(self.check_prepare_url)
        assert r.status_code == HTTP_200_OK, r.data
        assert r.data['count'] == 1, r.data
        assert r.data['results'][0]['finished'] == r.data['results'][0]['created']


class TestCheckExcelConfig:
    """
    Тестируем валидацию и сохранение excel_config через API
    """

    default_config = get_default_excel_config()

    @pytest.fixture
    def check_if_empty(self, tvm_api_client):
        # Проверяем, что сверок нет
        response = tvm_api_client.get(URL_LIST_CHECKS)
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 0

    @pytest.fixture
    def values(self):
        values = {
            'title': "New testing check",
            'cluster': enum.ARNOLD,
            'table1': '/dev/t1',
            'table2': '/dev/t2',
            'keys': 'k1 k2 k3',
            'columns': 'v1 v2',
            'result': '/dev/res',
            'status': Check.STATUS_DISABLED,
            'instance_id': 'xxxx',
            'workflow_id': 'yyyy',
        }

        return values

    @pytest.fixture
    def some_config(self, values):
        values.update({'excel_config': {"type": {"title": "тип"}, "key4_value": {"hide": True}}})

    @pytest.fixture
    def wrong_config(self, values):
        values.update(
            {
                'excel_config': {
                    "wrong_key": {"title": "тип"},
                    "close_dt": {"wrong_option": 123},
                    "key4_value": {"hide": "wrong_type"},
                }
            }
        )

    @pytest.fixture
    def null_config(self, values):
        values.update(
            {
                'excel_config': None,
            }
        )

    @pytest.fixture
    def post_response(self, tvm_api_client, check_if_empty, values):
        return tvm_api_client.post(URL_LIST_CHECKS, values, format='json')

    @pytest.fixture
    def get_response(self, tvm_api_client):
        return tvm_api_client.get(URL_LIST_CHECKS)

    @pytest.fixture
    def excel_config(self, values):
        return values['excel_config']

    def test_post_success(self, some_config, post_response, get_response, excel_config):
        response = post_response
        assert response.status_code == HTTP_201_CREATED

        response = get_response
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 1
        assert response.data['results'][0]['excel_config'] == excel_config

    def test_post_wrong(self, wrong_config, post_response):
        response = post_response
        assert response.status_code == HTTP_400_BAD_REQUEST

    def test_post_expect_default_config_empty(self, post_response, get_response):
        response = post_response
        assert response.status_code == HTTP_201_CREATED

        response = get_response
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 1
        assert response.data['results'][0]['excel_config'] == self.default_config

    def test_post_expect_default_config_null(self, null_config, post_response, get_response):
        response = post_response
        assert response.status_code == HTTP_201_CREATED

        response = get_response
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 1
        assert response.data['results'][0]['excel_config'] == self.default_config


class TestCheckReactor:
    """
    Проверяет логику работу с папками в нирване
    """

    @pytest.fixture(autouse=True)
    def setup(self):
        self.check = {
            'title': "Some check",
            'code': "exists",
            'cluster': enum.ARNOLD,
            'table1': '/dev/t1',
            'table2': '/dev/t2',
            'keys': 'k1 k2 k3',
            'columns': 'v1 v2',
            'result': '/dev/res',
            'status': Check.STATUS_DISABLED,
            'instance_id': 'xxxxxx',
        }

        self.new_ns_id = 123
        self.new_workflow_id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"

    @pytest.fixture
    def reactor_api_mock(self):
        with mock.patch(f'{APP_PREFIX}.api.reactor.ReactorAPIClientV1', autospec=True) as m:
            reactor_api_mock = m.return_value
            yield reactor_api_mock

    def test_fail_namespace_exists(self, tvm_api_client, reactor_api_mock):
        # namespace.get не кидает NoNamespaceError => директория существует
        response = tvm_api_client.post(URL_LIST_CHECKS, self.check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST

    def test_namespace_created(self, tvm_api_client, reactor_api_mock, nirvana_api_mock):
        reactor_api_mock.namespace.get.side_effect = reactor_client.NoNamespaceError(0, "")
        nirvana_api_mock.create_workflow.return_value = self.new_workflow_id

        response = tvm_api_client.post(URL_LIST_CHECKS, self.check, format='json')
        assert response.status_code == HTTP_201_CREATED

        # проверяем, что нирвана создает workflow в ns_id, который вернул реактор
        nirvana_api_mock.create_workflow.assert_called_once_with(
            self.check['code'],
            ns_id=reactor_api_mock.namespace.create.return_value.namespace_id,
            quota_project_id=settings.NIRVANA_QUOTA_PROJECT_ID,
        )

        assert Check.objects.get(code=self.check['code']).workflow_id == self.new_workflow_id

    def test_namespace_created_on_edit(self, tvm_api_client, some_check, reactor_api_mock, nirvana_api_mock):
        reactor_api_mock.namespace.get.side_effect = reactor_client.NoNamespaceError(0, "")
        nirvana_api_mock.create_workflow.return_value = self.new_workflow_id

        some_check.code = 'some_code'
        check = model_to_dict(some_check)
        check['workflow_id'] = None

        response = tvm_api_client.put(reverse('check-detail', kwargs={'pk': 1}), check, format='json')
        assert response.status_code == HTTP_200_OK

        some_check.refresh_from_db()
        assert some_check.workflow_id == self.new_workflow_id

    def test_namespace_deleted_on_failure(self, tvm_api_client, reactor_api_mock, nirvana_api_mock):
        reactor_api_mock.namespace.get.side_effect = reactor_client.NoNamespaceError(0, "")
        nirvana_api_mock.create_workflow.side_effect = nirvana.RPCException(123)

        response = tvm_api_client.post(URL_LIST_CHECKS, self.check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST

        reactor_api_mock.namespace.delete.assert_called_once_with(
            [
                reactor_client.reactor_objects.NamespaceIdentifier(
                    namespace_id=reactor_api_mock.namespace.create.return_value.namespace_id,
                )
            ]
        )

    def test_namespace_created_multiple(self, tvm_api_client, reactor_api_mock, nirvana_api_mock):
        workflow_ids = [
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            "cccccccc-cccc-cccc-cccc-cccccccccccc",
        ]

        reactor_api_mock.namespace.get.side_effect = reactor_client.NoNamespaceError(0, "")
        nirvana_api_mock.create_workflow.side_effect = workflow_ids

        self.check['aa_instance_id'] = 'aaaaaa'
        self.check['prep_instance_id'] = 'pppppp'

        response = tvm_api_client.post(URL_LIST_CHECKS, self.check, format='json')
        assert response.status_code == HTTP_201_CREATED

        check_from_db = Check.objects.get(code=self.check['code'])
        assert check_from_db.workflow_id == workflow_ids[0]
        assert check_from_db.aa_workflow_id == workflow_ids[1]
        assert check_from_db.prep_workflow_id == workflow_ids[2]

    def test_namespace_deleted_multiple_on_failure(
        self, tvm_api_client, reactor_api_mock, nirvana_api_mock, some_check
    ):
        reactor_api_mock.namespace.get.side_effect = reactor_client.NoNamespaceError(0, "")
        nirvana_api_mock.create_workflow.side_effect = ['ok', 'ok', nirvana.RPCException(123)]

        self.check['aa_instance_id'] = 'aaaaaa'
        self.check['prep_instance_id'] = 'pppppp'

        response = tvm_api_client.post(URL_LIST_CHECKS, self.check, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST

        reactor_api_mock.namespace.delete.assert_called_once_with(
            [
                reactor_client.reactor_objects.NamespaceIdentifier(
                    namespace_id=reactor_api_mock.namespace.create.return_value.namespace_id,
                )
            ]
            * 3
        )
