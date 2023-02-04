import json
from collections import namedtuple
from copy import deepcopy
from dataclasses import dataclass
from typing import List
from unittest.mock import MagicMock

import pytest
from django.conf import settings
from django.utils import timezone

from mdh.core.integration.logbroker.client import LogbrokerClient
from mdh.core.integration.logbroker.configurer import (
    LogbrokerConfigurationClient, LogbrokerException, LogbrokerAlredyExists,
)
from mdh.core.integration.logbroker.consumer import LogbrokerMessage
from mdh.core.integration.logbroker.reader import LogbrokerImporter
from mdh.core.models import Record, STATUS_PUBLISHED
from mdh.core.tasks import logbroker_write
from mdh.core.toolbox.logbroker import WriterParams

StubResponse = namedtuple('StubResponse', ['operation'])

STATUS_OK = '1'
STATUS_EXISTS = '2'
STATUS_ERROR = '400080'


class StubOperation:

    def __init__(self, id: int = 123, status: str = STATUS_OK, ready: bool = True):
        self.id = id
        self.status = status
        self.issues = ''
        self.ready = ready
        mock = MagicMock()
        mock.Unpack = self.unpack
        self.result = mock

    def unpack(self, result):
        pass


class StubLb:
    """Заглушка для имитации логброкера. Содержит методы сразу от нескольких сущностей."""

    def __init__(self, channel):
        self.responses: List[StubOperation] = []

    def _get_response(self):

        try:
            response = StubResponse(operation=self.responses.pop())

        except IndexError:
            response = StubResponse(operation=StubOperation())

        return response

    def ListAccounts(self, request):
        return self._get_response()

    def ListDirectory(self, request):
        return self._get_response()

    def GetOperation(self, request):
        return self._get_response()

    def ExecuteModifyCommands(self, request):
        return self._get_response()

    @staticmethod
    def GetOperationRequest(id):
        return StubResponse(operation=StubOperation())

    @classmethod
    def ListAccountsResult(cls):
        return namedtuple('List', ['names'])(names=['mdh'])

    @classmethod
    def ListDirectoryResult(cls):
        item = namedtuple('item', ['directory'])
        return namedtuple('List', ['path', 'self', 'children'])(
            path=namedtuple('path', ['path'])(path='some'),
            self=item(directory='mdh'),
            children=[
                item(directory='a'),
                item(directory='b'),
                item(directory='c'),
            ]
        )


@pytest.fixture
def stub_lb(monkeypatch):
    """Подалкдывает заглушку вместо реальных библиотек Logborker
    (нужен для работы с кофигурациями) брокера.

    """

    def stub_lb_():
        prefix = 'mdh.core.integration.logbroker.configurer.'

        conf_man_grpc = MagicMock()

        stub_lb = StubLb
        conf_man_grpc.ConfigurationManagerServiceStub = stub_lb

        conf_man = MagicMock()
        conf_man.ListAccountsResult = stub_lb.ListAccountsResult
        conf_man.ListDirectoryResult = stub_lb.ListDirectoryResult

        status_ids = MagicMock()
        status_ids.StatusCode.SUCCESS = STATUS_OK
        status_ids.StatusCode.ALREADY_EXISTS = STATUS_EXISTS

        path = MagicMock()

        monkeypatch.setattr(f'{prefix}grpc', MagicMock())
        monkeypatch.setattr(f'{prefix}conf_man', conf_man)
        monkeypatch.setattr(f'{prefix}conf_man_grpc', conf_man_grpc)
        monkeypatch.setattr(f'{prefix}StatusIds', status_ids)
        monkeypatch.setattr(f'{prefix}GetOperationRequest', stub_lb.GetOperationRequest)
        monkeypatch.setattr(f'{prefix}Path', path)

    return stub_lb_


@pytest.fixture
def stub_logbroker_client(monkeypatch):
    """Подкладывает заглушку вместо реальных библиотек Logbroker (читателя и писчего)."""

    def stub_logbroker_client_(*, consumer_func=None):
        prefix = 'errorboosterclient.logbroker.'

        auth = MagicMock()
        pqlib = MagicMock()

        api = MagicMock()

        if consumer_func:
            api.create_consumer = consumer_func

        pqlib.PQStreamingAPI = lambda *args: api

        def init(self):
            self._me = MagicMock()
            self.seq_num = 0
            return self._me

        monkeypatch.setattr(f'{prefix}LogbrokerProducer.init', init)

        errors = MagicMock()
        errors.SessionFailureResult = ValueError

        monkeypatch.setattr(f'{prefix}auth', auth)
        monkeypatch.setattr(f'{prefix}pqlib', pqlib)
        monkeypatch.setattr(f'{prefix}errors', errors)

    return stub_logbroker_client_


@dataclass
class Zipped:
    raw: bytes


@pytest.fixture
def mock_consumer_response():
    """Позволяет имитировать ответы логброкера для читателя."""

    def mock_consumer_response_(*, events, logbroker=None):

        from concurrent.futures import TimeoutError

        class MockConsumer(MagicMock):

            def next_event(self):

                try:
                    event_data = self.events.pop()
                except IndexError:
                    raise TimeoutError

                if event_data == TIMEOUT:
                    raise TimeoutError

                event = MagicMock()

                result = MagicMock()
                result.type = event_data[0]

                messages = []

                batch = MagicMock()
                batch.message = messages
                batch.topic = 'rt3.myt--mdh@test--trash'

                for msg in event_data[2]:
                    msg_mock = MagicMock()

                    msg_mock.meta.codec = 0

                    if isinstance(msg, Zipped):
                        msg_mock.meta.codec = 1
                        msg = msg.raw

                    if isinstance(msg, dict):
                        msg = json.dumps(msg).encode()

                    msg_mock.data = msg
                    msg_mock.meta.seq_no = -1
                    msg_mock.meta.source_id = b'dummy'
                    messages.append(msg_mock)

                cookie_id = event_data[1]
                result.message.data.cookie = cookie_id
                result.message.data.message_batch = [batch]

                # Подтверждение получения информации о прочтении.
                result.message.commit.cookie = [cookie_id]

                event.result = lambda **kwargs: result
                return event

        func_consumer = lambda *args, **kwargs: MockConsumer(events=events)

        if logbroker:
            logbroker.api.create_consumer = func_consumer

        return func_consumer

    TIMEOUT = 'timeout'
    mock_consumer_response_.TIMEOUT = TIMEOUT

    return mock_consumer_response_


def test_consumer_read(stub_logbroker_client, mock_consumer_response):
    name = 'mdh/test/tester'
    topic = 'mdh/test/trash'

    do_stub = True

    if do_stub:
        stub_logbroker_client()

    def read(*, events, limit_batches=None, retry_on_timeout=False):

        captured = []
        consumers = []

        with LogbrokerClient() as logbroker:

            if do_stub:
                mock_consumer_response(events=events, logbroker=logbroker)

            with logbroker.get_consumer(name=name, topics=[topic], json_decoder=True) as consumer:

                for message in consumer.read(limit_batches=limit_batches, retry_on_timeout=retry_on_timeout):
                    captured.append(message)
                    consumers.append(id(consumer._me))

        return captured, set(consumers)

    # Проверим основной сценарий.
    captured, consumers = read(events=[
        (3, 3, [b'{"key": 4}']),
        (5, 0, []),  # 5 - MSG_ERROR
        (4, 2, [b'']),  # 4 - MSG_COMMIT
        (3, 2, [b'{"key": 3}']),  # 3 - MSG_DATA
        (3, 1, [b'{"key": 1}', b'{"key": 2}']),
    ])

    assert len(captured) == 4
    assert len(consumers) == 2  # При ошибке читатель переинициализировался.

    for idx, captured_msg in enumerate(captured, 1):
        assert captured_msg.source == 'dummy'
        assert captured_msg.topic == topic
        assert captured_msg.seq_num == -1
        assert captured_msg.data == {'key': idx}

    # Проверим отсечку по количеству пакетов и автоповтор чтения.
    captured, _ = read(
        events=[
            (3, 2, [b'{"key": 4}']),
            (3, 2, [b'']),  # проверка невалидного json
            mock_consumer_response.TIMEOUT,
            (3, 1, [b'{"key": 1}', b'{"key": 2}']),
        ],
        limit_batches=2,
        retry_on_timeout=True
    )

    assert len(captured) == 3  # два сообщения в первом пакете + одно во втором (после таймаута)


def test_configuration_client(stub_lb):
    do_stub = True

    do_stub and stub_lb()

    with LogbrokerConfigurationClient() as client:
        # Список учёток.
        accounts = client.get_accounts()
        assert 'mdh' in accounts

        # Данные о директории.
        directory = client.get_directory('mdh')
        assert directory.self.directory == 'mdh'
        assert len(directory.children) == 3

        # Создаём директории.
        client.create_directory(['mdh/test/hello'])
        client.create_directory(['mdh/test/world'])

        # Обработка попытки создания существующих директорий.
        client.create_directory(['mdh/test/world'])
        if do_stub:
            client.lb.responses.append(StubOperation(status=STATUS_EXISTS))

        with pytest.raises(LogbrokerAlredyExists):
            if do_stub:
                client.lb.responses.append(StubOperation(status=STATUS_EXISTS))
            client.create_directory(['mdh/test/world'], raise_if_exists=True)

        # проверка что директории создались
        client.get_directory('mdh/test/hello')
        client.get_directory('mdh/test/world')

        client.create_topic(['mdh/test/hello/1'])

        # Повторные запросы статуса операции.
        if do_stub:
            client.lb.responses.append(StubOperation(ready=False))
            client.create_topic(['mdh/test/hello/2'])

        # Ошибка при создании раздела.
        with pytest.raises(LogbrokerException) as e:
            if do_stub:
                client.lb.responses.append(StubOperation(status=STATUS_ERROR))
            client.create_topic(['mdh/test/qqq/1'])
        assert e.value.status == '400080'  # GENERIC_ERROR, path doesn't exist

        # Удаляем директорию, раздел.
        if do_stub:
            client.remove_topic('mdh/test/hello/2')
            client.remove_topic('mdh/test/hello/1')
            client.remove_directory('mdh/test/hello')


def test_read_incoming(
    stub_logbroker_client, run_task, init_user, init_resource, mock_consumer_response,
    django_assert_num_queries, monkeypatch,
):
    assert settings.LOGBROKER_DC_IMPORT == ['myt', 'sas', 'vla', 'iva']

    do_stub = True

    user = init_user(robot=True)
    resource = init_resource(user=user, publish=True)
    record = resource.record_add(creator=user, attrs={'integer1': 1}, status=Record.STATUS_PUBLISHED)
    record.master_uid = "bf6040b2-4686-454d-bdc7-1031750bd01a"
    record.save()

    if do_stub:
        stub_logbroker_client(consumer_func=mock_consumer_response(events=[
            (3, 9, [{
                # обновление записи с mdh_uuid
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'mdh_uuid': record.master_uid,
                'action': 'update',
                'attrs': {
                    'integer1': 66,
                }
            }]),

            (3, 8, [{
                # добавление записи при помощи put
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'id': '789',
                'action': 'put',
                'attrs': {
                    'integer1': 55,
                }
            }]),

            (3, 7, [{
                # обновление записи при помощи put
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'id': '123',
                'action': 'put',
                'attrs': {
                    'integer1': 44,
                }
            }]),

            # в архиве
            (3, 6, [Zipped(raw=
                           b'\x1f\x8b\x08\x00\xf2\xbf\x9b_\x02\xff\xabV\xca\xcbOIU\xb2R\x80\xd0:\nJE\xa9i\xa9E\xa9y\xc9`A \x07$'
                           b'\x96\x99\x02\xe2\x98\x98\x9a\x818\x89\xc9%\x99\xf9y \x81\xc4\x94\x14\xb0@IIQ1\x90_\xad\x94\x99W\x92'
                           b'\x9a\x9eZd\x08\xe4\x18\xd7\xd6\x02\x00\xd5o\xd5K\\\x00\x00\x00')
                    ]),

            (3, 5, [{
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'id': '345',
                'action': 'add',
                'attrs': {
                    'bogus': 'abcd',  # не соответствует схеме
                }
            }]),
            (3, 4, [{
                # не хватает обязательных ключей
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'attrs': {
                    'integer1': 10,
                }
            }]),
            (3, 3, [{
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'id': '123',
                'action': 'bogus',  # не поддерживается
                'attrs': {
                    'integer1': 10,
                }
            }]),
            (3, 2, [{
                # добавление записи
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'id': '123',
                'action': 'update',
                'attrs': {
                    'integer1': 3,
                }
            }]),
            (3, 1, [{
                # обновление записи
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'id': '123',
                'action': 'add',
                'attrs': {
                    'integer1': 2,
                }
            }]),
        ]))

    stats_all = []

    def mock_monitor_lb_read(stats):
        stats_all.extend(stats)

    # monkeypatch.setattr('mdh.core.toolbox.monitors.monitor_lb_read', mock_monitor_lb_read)
    monkeypatch.setattr('mdh.core.integration.logbroker.reader.monitor_lb_read', mock_monitor_lb_read)

    # со включенным кешированием число уменьшится
    # здесь же учитываются установки транзакций.
    with django_assert_num_queries(30) as _:
        run_task('logbroker_read_records')

    record.refresh_from_db()
    assert record.attrs['integer1'] == 66

    assert len(Record.objects.all()) == 4

    some_record = Record.objects.get(remote_id="123")
    assert some_record.attrs['integer1'] == 44
    assert some_record.is_master
    assert some_record.is_published

    # Проверка накопления статистики.
    assert len(stats_all) == 3
    assert stats_all[0]['count'] == 3


class MockLbWriter:

    def __init__(self, *args, **kwargs):
        self.written = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass

    def __call__(self, *args, **kwargs):
        return self

    def get_producer(self, *args, **kwargs):
        return self

    def write(self, msg, *args, **kwargs):
        self.written.append(msg)


def test_send_to_logbroker(
    stub_logbroker_client, stub_lb, run_task, init_user, init_resource,
    django_assert_num_queries, monkeypatch, mock_solomon):
    do_stub = True
    client_mock = MockLbWriter()

    if do_stub:
        monkeypatch.setattr('mdh.core.toolbox.logbroker.LogbrokerClient', client_mock)

        stub_logbroker_client()
        stub_lb()

    def assert_written(num: int):
        assert len(client_mock.written) == num
        written = deepcopy(client_mock.written)
        client_mock.written.clear()
        return written

    user = init_user()

    resources = []

    for domain_idx in range(0, 2):
        resource = init_resource(user=user, alias_postfix=str(domain_idx), publish=True)
        resources.append(resource)
        run_task('process_queue')  # создаются топики и директории в Logbroker

        for record_idx in range(10):
            Record.objects.create(**{
                'creator': user,
                'resource': resource,
                'attrs': {'integer1': domain_idx * record_idx + record_idx},
                'status': STATUS_PUBLISHED
            })

    do_stub and assert_written(0)

    with django_assert_num_queries(3) as _:
        result = run_task('logbroker_send_record')

    do_stub and assert_written(20)

    latest_record = None

    for record_idx in range(10):
        latest_record = Record.objects.create(**{
            'creator': user,
            'resource': resources[0],
            'attrs': {'integer1': 10},
            'status': STATUS_PUBLISHED
        })

    assert latest_record.dt_lb is None

    # Проверяем простановку времени обновления.
    result = WriterParams.parse_raw(result)

    with django_assert_num_queries(2) as _:
        result_new = logbroker_write(params=result, max_records=1000)

    if do_stub:
        written = assert_written(10)
        assert not written[0]['resync']

    assert result.since < result_new.since

    # Проверям наличие и работу фонового задания обновления даты выгрузки в lb.
    run_task('process_queue')  # обновляется время выгрузки записей
    latest_record.refresh_from_db()
    assert latest_record.dt_lb

    # Проверяем ресинхронизацию конкретной записи.
    now = timezone.now()
    result_new = logbroker_write(params=WriterParams(
        since=now,
        records=[str(latest_record.record_uid)],
    ))

    if do_stub:
        assert result_new.since == now  # Дата не должна изменяться при реиснхронизации.
        written = assert_written(1)
        assert written[0]['record_uid'] == latest_record.record_uid
        assert written[0]['resync']
        assert written[0]['version_master'] == 0
        assert written[0]['version_composite'] == 0

    # Проверяем ресинхронизацию записей конкретной области.
    logbroker_write(params=WriterParams(domains=[resources[1].reference_link.domain.alias]))

    if do_stub:
        written = assert_written(10)
        assert written[0]['resync']

    # Проверяем ресинхронизацию записей конкретного справочника.
    logbroker_write(params=WriterParams(references=[resources[1].reference.alias]))

    if do_stub:
        written = assert_written(10)
        assert written[0]['resync']

    # Проверяем ресинхронизацию записей конкретного справочника с фильтром по атрибутам.
    logbroker_write(params=WriterParams(
        references=[resources[1].reference.alias],
        attrs={'integer1__gte': 5}
    ))

    if do_stub:
        written = assert_written(7)
        assert written[0]['resync']


def test_import(stub_logbroker_client, stub_lb, monkeypatch, init_user, init_resource):
    user = init_user(robot=True)
    resource = init_resource(user=user, publish=True)

    message = LogbrokerMessage(
        seq_num=0,
        source='dummy',
        topic='mdh/test/trash',
        endpoint=None,
        data=[
            {
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'id': '789',
                'action': 'add',
                'attrs': {
                    'integer1': 210622,
                }
            },
            {
                'node': resource.node.alias,
                'reference': resource.reference_link.reference.alias,
                'id': '123',
                'action': 'add',
                'attrs': {
                    'integer1': 200622,
                }
            }
        ]
    )

    logbroker_importer = LogbrokerImporter()
    logbroker_importer.import_message(message)

    record_objects = Record.objects.filter(
        reference=resource.reference_link.reference
    ).filter(
        remote_id__in=['789', '123']
    ).order_by('remote_id')
    assert len(record_objects) == 2
    assert record_objects[0].attrs.get('integer1') == 200622
    assert record_objects[1].attrs.get('integer1') == 210622
