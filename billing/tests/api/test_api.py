from collections import namedtuple
from socket import gethostname
from typing import Tuple, Union
from datetime import datetime, timedelta

import pytest
from django.test import Client
from django.urls import reverse

from dwh.core.models import Interruption


class TestWorkAPI:
    @pytest.fixture(autouse=True)
    def setup(self):
        self.client = Client()
        self.url_listing = reverse('api_work_list')
        self.default_work = {'meta': {'task_name': 'echo'}, 'params': {'a': '10'}}

    @pytest.fixture
    def fake_tvm_ticket(self, monkeypatch):
        def fake_tvm_ticket_(*, valid_tvm_id: bool = True):
            tvm_id = 1 if valid_tvm_id else 0

            def auth(request):
                user = request.yauser
                user.service_ticket = namedtuple('FakeTicket', ['src'])(src=tvm_id)
                return user

            monkeypatch.setattr('dwh.api.views_base.authenticate', auth)

            if valid_tvm_id:
                monkeypatch.setattr('dwh.api.views_base.TVM_CLIENTS', {tvm_id})

        return fake_tvm_ticket_

    def get_listing(self, args: str = '') -> Tuple[dict, int]:
        out = self.client.get(f'{self.url_listing}{args}')
        return out.json(), out.status_code

    def post_listing(self, data: Union[str, dict], idempotency_key: str = None) -> Tuple[dict, int]:
        out = self.client.post(
            self.url_listing,
            data,
            content_type='application/json',
            HTTP_X_REQUEST_ID='tttt',
            HTTP_X_IDEMPOTENCY_KEY=idempotency_key,
        )
        return out.json(), out.status_code

    def get_details(self, work_id: int) -> Tuple[dict, int]:
        out = self.client.get(reverse('api_work_details', kwargs={'work_id': work_id}))
        return out.json(), out.status_code

    def test_auth(self, fake_tvm_ticket):

        # без билета
        json, status_code = self.get_listing()
        assert status_code == 403
        assert not json['data']
        assert 'Unable to authenticate your service using TVM' in json['errors'][0]['msg']

        # с недозволенным билетом
        fake_tvm_ticket(valid_tvm_id=False)
        json, status_code = self.get_listing()
        assert status_code == 403
        assert 'AuthError: TMV client ID 0 is not allowed' in json['errors'][0]['msg']

        # c дозволенным билетом
        fake_tvm_ticket()
        json, status_code = self.get_listing()
        assert status_code == 200
        assert not json['errors']

    def test_basic(self, fake_tvm_ticket):
        fake_tvm_ticket()

        # пустой список
        json, code = self.get_listing()
        assert code == 200
        assert len(json['data']) == 0

        # невалидный json
        json, code = self.post_listing('{"a": BOGUS}')
        assert code == 400
        assert 'ApiClientError: Unable to interpret the request. Check it for validity.' in json['errors'][0]['msg']

        # валидное создание
        json, code = self.post_listing(self.default_work)
        assert code == 201
        data = json['data']
        assert data['name'] == 'echo'
        assert data['status'] == 'new'
        new_work_id = data['id']
        assert data['url'] == f'https://dwh-test.yandex-team.ru/works/{new_work_id}/'

        # новое задание в списке.
        json, code = self.get_listing()
        assert code == 200
        data = json['data']
        assert len(data) == 1

        # деталировка по заданию
        # неизвестное задание
        json, code = self.get_details(1234567890)
        assert code == 404
        assert 'ApiClientError: Requested work is not found.' in json['errors'][0]['msg']

        # созданное нами задание
        json, code = self.get_details(new_work_id)
        assert code == 200
        data = json['data']
        assert data['input'] == {
            'meta': {'task_name': 'echo', 'workers': 1, 'timeout': 0, 'retries': 1, 'hint': ''}, 'params': {'a': '10'}}
        assert data['remote'] == '1'  # tvm id
        assert data['request_id'] == 'tttt'
        assert data['url'] == f'https://dwh-test.yandex-team.ru/works/{new_work_id}/'

        # проверим постраничную разбивку
        self.post_listing({
            'meta': {'task_name': 'echo', 'workers': 1, 'timeout': 0, 'retries': 1, 'hint': ''}, 'params': {'a': 11}})
        json, _ = self.get_listing()
        assert len(json['data']) == 2

        json, _ = self.get_listing('?per_page=1')
        assert len(json['data']) == 1

        json, _ = self.get_listing('?per_page=1&page=2')
        assert len(json['data']) == 1
        assert json['data'][0]['id'] == new_work_id

        json, _ = self.get_listing('?per_page=1&page=200')
        assert json['data'] == []

    def test_idempotency_key(self, fake_tvm_ticket, time_freeze):
        fake_tvm_ticket()

        some_idempotency_key = "abobaamogus"

        # первичное создание объекта
        json, code = self.post_listing(self.default_work, idempotency_key=some_idempotency_key)
        old_data = json['data']

        assert code == 201

        # проверяем работу ключа идемпотентности
        json, code = self.post_listing(self.default_work, idempotency_key=some_idempotency_key)
        new_data = json['data']

        assert code == 201
        assert old_data == new_data

        # имитируем протухший кеш
        with time_freeze(datetime.now() + timedelta(hours=13)):
            json, code = self.post_listing(self.default_work, idempotency_key=some_idempotency_key)
            new_data = json['data']

        assert code == 201
        assert old_data != new_data

        # проверяем что уникальные ключи ни на что не влияют
        work_ids = set()
        for i in range(5):
            json, code = self.post_listing(
                self.default_work,
                idempotency_key=f'{some_idempotency_key}{i}'
            )
            assert code == 201
            work_ids.add(int(json['data']['id']))

        assert len(work_ids) == 5


class TestInterruptionAPI:
    @pytest.fixture(autouse=True)
    def setup(self):
        self.client = Client()
        self.url_set = reverse('api_interruption_set')
        self.url_drop = reverse('api_interruption_drop')

    def do_set(self) -> Tuple[dict, int]:
        out = self.client.get(
            self.url_set,
            content_type='application/json',
        )
        return out.json(), out.status_code

    def do_drop(self) -> Tuple[dict, int]:
        out = self.client.post(
            self.url_drop,
            content_type='application/json',
        )
        return out.json(), out.status_code

    def test_interruption(self, init_work, check_work_stop):
        target = gethostname()

        interruption1 = Interruption.objects.create(target=target)
        work_1 = init_work({'meta': {'task_name': 'echo'}, 'params': {'a': 17}})
        work_1.target = target
        work_1.status = work_1.Status.STARTED
        work_1.save()

        # запрос на прерывание обработан, но есть незавершённое задание.
        json, status = self.do_set()
        assert status == 425
        assert json['data']['count'] == 1

        work_1.refresh_from_db()
        check_work_stop(work_1)  # задача остановилась

        # запрос на прерывание обработан, задания остановлены.
        json, status = self.do_set()
        assert status == 200
        assert json['data']['count'] == 0

        interruption1.refresh_from_db()
        assert interruption1.active

        # возвращаем прерыванные в работу.
        json, status = self.do_drop()
        assert status == 200
        assert json['data']['count'] == 1

        interruption1.refresh_from_db()
        assert not interruption1.active

        work_1.refresh_from_db()
        assert work_1.status == work_1.Status.NEW
