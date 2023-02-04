import contextlib
import csv
import json
import logging
import os
import re
import sys
import threading
from collections import defaultdict, namedtuple
from contextlib import contextmanager
from datetime import datetime, timedelta
from decimal import Decimal
from io import BytesIO
from os.path import abspath, dirname, join
from typing import List, Union, Tuple, Callable, Dict
from uuid import uuid4, UUID

import django
import pytest
from django.conf import settings
from django.contrib.auth import authenticate
from django.core.cache import cache
from django.core.files.uploadedfile import UploadedFile
from django.test import Client
from django.test.client import RequestFactory
from dssclient.endpoints.certificates import Certificates
from dssclient.endpoints.documents import Documents
from freezegun import freeze_time
from lxml import etree
from paramiko import SFTPClient
from sitemessage.models import Message

from bcl.banks.base import Associate
from bcl.banks.common.client import HttpResponse
from bcl.banks.common.letters import ProveLetter
from bcl.banks.common.statement_downloader import FakedStatementDownloader
from bcl.banks.common.statement_helper import StatementHelper
from bcl.banks.common.statement_parser import TypeStatementParseResult
from bcl.banks.party_yoomoney.common import request_processor, Signer
from bcl.banks.registry import get_associate, Sber, Ing, Tinkoff
from bcl.core.models import (
    Payment, SigningRight, User, Role, PaymentsBundle, PaymentsBundleFile,
    Account, Organization, SalaryContract, Currency, Service, Statement,
    states, StatementPayment, StatementRegister, Direction, OrganizationGroup,
    Prove, Contract, DocRegistryRecord, Svo, SvoItem, Spd, SpdItem, Letter,
)
from bcl.core.tasks import process_statements, process_bundles, process_documents
from bcl.exceptions import DoesNotExist
from bcl.toolbox import session
from bcl.toolbox.notifiers import NotifierBase
from bcl.toolbox.tasks import get_registered_task
from bcl.toolbox.utils import make_list, DateUtils

try:
    import library.python
    import pkgutil
    django.setup()  # Аркадийный pytest-django на этом моменте ещё не сконфигурировал Django.

    ARCADIA_RUN = True

except ImportError:
    ARCADIA_RUN = False



# Иногда используем logging.exception(), который по умолчанию будет
# сорить сообщениями в тестах. Отключаем.
logging.disable(logging.ERROR)


PATH_TESTS = dirname(abspath(__file__))
PATH_BASE = dirname(PATH_TESTS)
PATH_CONFIG = join(PATH_BASE, 'conf/dev/config.xml')

sys.path = [PATH_BASE] + sys.path  # Для успешного импорта пакета.

settings.ENVIRON['YANDEX_XML_CONFIG'] = PATH_CONFIG


@pytest.fixture(autouse=True)
def db_access_on(django_db_reset_sequences,):
    """Используем бд в тестах без поимённого маркирования."""
    session._THREAD = threading.local()  # Сбрасываем закешированные данные пользователя.


@pytest.yield_fixture(autouse=True)
def mock_graphite_notifier(monkeypatch):
    """Не пытаемся писать в сокет для оправки данных в графит."""
    monkeypatch.setattr('bcl.toolbox.notifiers.GraphiteNotifier.send_data', lambda *args, **kwargs: None)


@pytest.fixture
def ui_has_message():
    """Возвращает список сообщений, предназначенных для показа в интерфейсе пользователя,
    в которых содержится указанная подстрока.

    """
    def ui_has_message_(msg):
        return [text for mtype, text in getattr(session._THREAD, 'ui_messages', []) if msg in text]

    return ui_has_message_


@pytest.fixture
def sitemessages():
    """Позволяет получать объект писем, запланированных к отправке."""
    def sitemessages_() -> List[Message]:
        return list(Message.objects.all().order_by('id'))
    return sitemessages_


class DirFixtures:
    """Обёртка для доступа к поддиректории fixtures директории с тестом."""

    def __init__(self, module_path, root_path=True):
        self._path = join(module_path, 'fixtures') if root_path else module_path

    def read(self, file_name, decode=None):
        file_path = self.path(file_name)

        if ARCADIA_RUN:
            data = pkgutil.get_data(__package__, file_path)

        else:
            with open(file_path, 'rb') as f:
                data = f.read()

        if decode:
            data = data.decode(decode)

        return data

    def path(self, file_name):
        return join(self._path, file_name)


@pytest.fixture
def path_module(request):
    """Путь до текущего тестируемого модуля"""
    filename = request.module.__file__
    if not ARCADIA_RUN:
        filename = abspath(filename)
    return dirname(filename)


@pytest.fixture()
def fixturesdir(path_module):
    """Фикстура которая предоставляет доступ к каталогу fixtures тестового модуля

    :example:
    def test_basic(fixturesdir):
            data = fixturesdir.read('my_file.txt', decode='utf-8-sig')
            path = fixturesdir.path('my_file.txt')
    """
    return DirFixtures(path_module)


@pytest.fixture
def read_fixture(path_module):
    """Считывает указанный файл из директории fixtures в директории с тестовым модулем.
    Пример:
        def test_basic(read_fixture):
            data = read_fixture('my_file.txt', decode='utf-8-sig')
    """
    dir_fixture = DirFixtures(path_module)

    return dir_fixture.read


@pytest.fixture
def read_fixture_from_dir(path_module):
    """Считывает указанный файл из директории fixtures в директории с тестовым модулем.
    Пример:
        def test_basic(read_fixture):
            data = read_fixture('my_file.txt', decode='utf-8-sig')
    """

    def wrapper(file_name, path_module=path_module, root_path=True):
        dir_fixture = DirFixtures(path_module, root_path)

        return dir_fixture.read(file_name)

    return wrapper


@pytest.fixture
def path_fixture(path_module):
    """Возвращает путь до указанного файла в директории fixtures тестового модуля
    Пример:
        # package - tests/banks

        def test_basic(path_fixture):
            full_path = path_fixture('my_file.txt')
            # full_path = 'path/to/tests/banks/fixtures/my_file.txt'
    """
    dir_fixture = DirFixtures(path_module)

    return dir_fixture.path


@pytest.fixture
def mock_post(monkeypatch):
    """Имитирует отправку запроса, подменяя результат на заданный.

    Последний отправленный запрос доступен в mock_post.last_request.
    """
    import requests

    class MockResponse:

        def __init__(self, *, answer, status_code):
            self.answer = answer
            self.text = answer
            self.status_code = status_code
            self.ok = True

    def mocked_post(*answers, url_filter, method_orig):
        """Возвращает подмененный метод отправки запроса."""
        answers = iter(answers)

        def responder(url, data, **kwargs):
            """Возвращает подмененный ответ и сохраняет запрос."""

            if not url_filter or url_filter(url):

                if isinstance(data, bytes):
                    data = data.decode('utf-8')

                wrapper.last_request = data

                return MockResponse(answer=next(answers, None), status_code=200)

            return method_orig(url, data, **kwargs)

        return responder

    def wrapper(*responses, url_filter=None):

        monkeypatch.setattr(
            requests, 'post',
            mocked_post(*responses, url_filter=url_filter, method_orig=getattr(requests, 'post')))

    return wrapper


class MockSftpClient:
    """Имитатор для SftpClient."""

    def __init__(self):
        self.files_contents = {}
        self.alias = None
        self.check_filters = False
        self.path_outbox = 'out/'
        self.path_inbox = 'in/'

    @contextmanager
    def sftp_connection(self):
        yield

    def file_write(self, fname: str, data: bytes, path: str = '', sftp_conn: SFTPClient = None):
        fname = f'{path}{fname}'
        self.files_contents[fname] = data

    def file_read(self, filename, *args, **kwargs):
        return {filename: self.files_contents[filename]}

    def get_files_contents(self, *args, **kwargs):
        if kwargs.get('name_filter') and self.check_filters :
            return {
                key: self.files_contents[key] for key in filter(lambda x: kwargs['name_filter'](x), self.files_contents)
            }
        return self.files_contents

    def __call__(self, *args, **kwargs):
        return self

    def configure(self, alias):
        return self

    def list_files(self, name_filter=None, path=''):
        for file_path in self.files_contents:
            if file_path.startswith(path) and (not name_filter or name_filter(file_path[len(path):])):
                yield file_path


@pytest.fixture
def run_task():
    """Запускает зарегистрированное фонового задание по его имени."""

    def run_task_(name: str, *, latest_result: str = ''):

        # Для простоты здесь же поддерживаем запуск некоторых заданий по таймеру.
        if name == 'process_statements':
            process_statements(None)

        elif name == 'process_bundles':
            process_bundles(None)

        elif name == 'process_documents':
            process_documents(None)

        else:
            return get_registered_task(name).func(latest_result=latest_result)

    return run_task_


@pytest.fixture
def sftp_client(monkeypatch):
    """Имитирует SftpClient.

    Пример:

        def test_basic(sftp_client):
            client = sftp_client(files_contents={
                'file1.txt': '12345',
                'file2.txt': '67890',
            })

    """
    client = MockSftpClient()
    monkeypatch.setattr('bcl.toolbox.client_sftp.SftpClient', client)

    def wrapper(files_contents=None, check_filters=False):
        client.check_filters = check_filters
        if files_contents:
            client.files_contents = files_contents
        return client

    return wrapper


@pytest.fixture
def xml_signature():
    signed = (
        '<Signature xmlns="http://www.w3.org/2000/09/xmldsig#">'
        '<KeyInfo><X509Data><X509Certificate>MIIDAzCCArKgAwIBAgITEgANPs4cv5NsgMdm+wAAAA0+zjAIBgYqhQMCAgMwfzEj'
        'MCEGCSqGSIb3DQEJARYUc3VwcG9ydEBjcnlwdG9wcm8ucnUxCzAJBgNVBAYTAlJVMQ8wDQYDVQQHEwZNb3Njb3cxFzAVBgNVBAoT'
        'DkNSWVBUTy1QUk8gTExDMSEwHwYDVQQDExhDUllQVE8tUFJPIFRlc3QgQ2VudGVyIDIwHhcNMTYwMjAxMTIyNDIzWhcNMTYwNTAxM'
        'TIzNDIzWjASMRAwDgYDVQQDDAdwZXJzZXVzMGMwHAYGKoUDAgITMBIGByqFAwICJAAGByqFAwICHgEDQwAEQNP6oweNKL9e0vVQqo'
        'WCxMziX+Dnn8SDWjfJRBKveOclLlL3lBr33R8dVHWbG+7nwGjEVUYmtNVkOKpalotPA2mjggFwMIIBbDAOBgNVHQ8BAf8EBAMCBPA'
        'wEwYDVR0lBAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFALNqQfWmA9PSOZSl2ypkxg7K39UMB8GA1UdIwQYMBaAFBUxfLCNGt5m1xWc'
        'SVKXFyS5AXqDMFkGA1UdHwRSMFAwTqBMoEqGSGh0dHA6Ly90ZXN0Y2EuY3J5cHRvcHJvLnJ1L0NlcnRFbnJvbGwvQ1JZUFRPLVBST'
        'yUyMFRlc3QlMjBDZW50ZXIlMjAyLmNybDCBqQYIKwYBBQUHAQEEgZwwgZkwYQYIKwYBBQUHMAKGVWh0dHA6Ly90ZXN0Y2EuY3J5cH'
        'RvcHJvLnJ1L0NlcnRFbnJvbGwvdGVzdC1jYS0yMDE0X0NSWVBUTy1QUk8lMjBUZXN0JTIwQ2VudGVyJTIwMi5jcnQwNAYIKwYBBQU'
        'HMAGGKGh0dHA6Ly90ZXN0Y2EuY3J5cHRvcHJvLnJ1L29jc3Avb2NzcC5zcmYwCAYGKoUDAgIDA0EAPdSONfF2IKOKuT4aTdjGny54'
        '/W6Q8DqpLnHDkw9PCt2nj2CimmQhLZ+7drKOLWaD3g/ylhkl07skBqv/u1llXA==</X509Certificate></X509Data>'
        '</KeyInfo></Signature>'
    )
    return signed


@pytest.fixture
def dss_signing_mock(monkeypatch, xml_signature):
    """Имитатор подписи на DSS. Позволяет использовать заданный результат подписания."""
    signed_bytes = [b'']
    sign_level = [0]
    sign_serial = ['']

    SignedMock = namedtuple('SignedMock', ['signed_bytes', 'signed_base64'])
    SignRightMock = namedtuple('SignRightMock', ['level', 'serial_number'])

    def mock_sign(data, *, associate, user=None, right=None, org=None, signature_type=None):
        signed = SignedMock(signed_bytes=signed_bytes[0], signed_base64='')
        return [signed], SignRightMock(level=sign_level[0], serial_number=sign_serial[0])

    def dss_signing_mock_(*, signed='mock', level=1, serial='mock', bypass=False):

        if bypass:
            return

        monkeypatch.setattr('bcl.core.models.SigningRight.dss_sign', mock_sign)

        if signed == 'xmlsig':
            signed = xml_signature
            serial = '12000d3ece1cbf936c80c766fb0000000d3ece'

        if isinstance(signed, str):
            signed = signed.encode()

        signed_bytes[0] = signed
        sign_level[0] = level
        sign_serial[0] = serial

    return dss_signing_mock_


@pytest.fixture
def dss_signing_right(init_user):
    """Создаёт право на подпись DSS."""

    def dss_signing_right_(
        *,
        associate: Associate,
        username: str = None,
        serial: str = None,
        autosigning: bool = False,
        level: int = 0,
        org: Organization = None
    ):
        user = init_user(username or 'testuser')

        right = SigningRight(
            user=user,
            executive='testboss',
            associate_id=associate.id,
            autosigning=autosigning,
            level=level,
            org=org
        )
        if serial is None:
            serial = '124d455d1500d780e811c9255da2cff0'

        right.serial_number = serial
        right.dss_credentials_set('tester1', 'Atester1')
        right.save()

        return right

    return dss_signing_right_


@pytest.fixture
def get_payment_bundle(init_user, get_assoc_acc_curr, get_source_payment):
    """Создаёт и возвращает объект пакета платежей."""

    def get_payment_bundle_(
        source_payments,
        associate=None,
        account=None,
        h2h=False,
        id=None
    ) -> PaymentsBundle:
        """
        :param list[Union[Payment, dict]] source_payments:
        :param Associate associate:
        :param Account account:
        :param bool h2h: Пакет для сервер-сервер.
        :param id: идентификатор для пакета.

        """
        associate_id = source_payments[0].associate_id if associate is None else associate.id

        if not isinstance(account, Account):
            _, account, _ = get_assoc_acc_curr(associate_id, account=account)

        bundle = PaymentsBundle(
            associate_id=associate_id,
            account=account,
            destination=PaymentsBundle.DESTINATION_H2H if h2h else PaymentsBundle.DESTINATION_ONLINE,
            user=init_user(),
        )

        if id:
            bundle.id = id

        bundle.dt = datetime.now()  # чтобы работал freeze
        bundle.save()

        bundle_file = PaymentsBundleFile()
        bundle_file.bundle = bundle
        bundle_file.raw = b''
        bundle_file.save()

        bundle.file = bundle_file
        bundle.save()

        for payment in source_payments:
            if isinstance(payment, dict):
                payment['bundle_id'] = bundle.id
                get_source_payment(payment, associate=associate)

            else:
                payment.bundle_id = bundle.id
                payment.save()

        bundle = PaymentsBundle.objects.get(id=bundle.id)

        return bundle

    return get_payment_bundle_


@pytest.fixture
def time_shift():
    """Менеджер контекста. Позволяет передвинуть время вперёд на указанное количество секунд."""

    @contextmanager
    def time_shift_(seconds: int, *, utc: bool = True, backwards: bool = False):
        now_func: Callable = datetime.utcnow if utc else datetime.now

        if backwards:
            target_dt = now_func() - timedelta(seconds=seconds)
        else:
            target_dt = now_func() + timedelta(seconds=seconds)

        with freeze_time(target_dt):
            yield

    return time_shift_


@pytest.fixture
def time_freeze():
    """Менеджер контекста. Позволяет симитировать указанное время."""

    @contextmanager
    def time_freeze_(dt: str):
        with freeze_time(dt):
            yield

    return time_freeze_


@pytest.fixture
def build_payment_bundle(get_payment_bundle, get_source_payment, get_assoc_acc_curr):
    """Создаёт объект пакета платежей для указанной внешней системы,
    компилирует и возвращает его.

    При этом скомпилированный вариант доступен в атрибуте .tst_compiled.

    """
    def build_payment_bundle_(
        associate: Associate,
        *,
        payment_dicts: List[Union[dict, Payment]] = None,
        account: Union[Account, dict, str] = None,
        return_encoded: bool = False,
        service: Union[Service, bool] = False,
        h2h: bool = False,
    ) -> Union[PaymentsBundle, List[PaymentsBundle]]:
        """
        :param associate:

        :param payment_dicts: Словари с атрибутами платежей, либо сами платежи.
            Если не переданы, будет сформирован пакет на основе одного платежа с реквизитами по умолчанию.

        :param account: можно передать объект счета или параметры, по которым следуею создать счет

        :param return_encoded: Флаг. Следует ли вернуть скомпилированный текст кодированным в байты.

        :param service: указывает, это платеж о сервиса или нет

        :param h2h: Флаг. предназначен ли пакет для h2h

        :param payments: Готовые объекты платежей.

        """
        if isinstance(associate, type):
            associate = associate()

        if isinstance(account, Account):
            account_num = account.number

        else:
            _, account, _ = get_assoc_acc_curr(associate, account=account)
            account_num = account.number

        if payment_dicts is None:
            payment_dicts = [{}]

        payments = []

        for payment_dict in payment_dicts:

            if isinstance(payment_dict, dict):
                payment_dict = payment_dict.copy()
                payment_dict.setdefault('f_acc', account_num)
                payment_dict.setdefault('account_id', account.id)
                payment = get_source_payment(payment_dict, associate=associate, service=service)
            else:
                payment = payment_dict

            payments.append(payment)

        dispatcher = associate.payment_dispatcher
        bundles = dispatcher.bundle_compose(payments, for_h2h=h2h)

        for bundle in bundles.values():

            contents = bundle.file_tuple[1]

            if not return_encoded:
                contents = dispatcher.get_creator(bundle).decode(contents)

            bundle.tst_compiled = contents

        bundles = list(bundles.values())

        if len(bundles) == 1:
            return bundles[0]

        return bundles

    return build_payment_bundle_


@pytest.fixture
def profile_bundles_creation(get_source_payment_mass, compose_bundles, capture_mem, capture_time, capture_db_stats):
    """Запускает профилирование создания пакетов платежей."""

    def profile_bundles_creation_(associate, *, num_payments=2000, num_bundles=2, wipe=False, h2h=True):
        """
        :param Associate associate: Объект внешней системы.
        :param int num_payments: Количество платежей для помещения в пакет.
        :param int num_bundles: Количество пакетов для формирвоания.
        :param bool wipe: Предварительно удалить данные пакетов и платежей.
        :param bool h2h: Использовать формат для сервер-сервер.
        """
        if wipe:
            PaymentsBundle.objects.all().delete()
            Payment.objects.all().delete()

        for bundle in PaymentsBundle.objects.all():
            bundle.hide()

        count_expected = num_bundles * num_payments
        count_existing = Payment.objects.filter(
            associate_id=associate.id,
            status=states.NEW).count()

        if count_existing < count_expected:
            get_source_payment_mass(count=count_expected - count_existing, associate=associate)

        for pass_num in range(num_bundles):

            breaker = '=' * 30

            print('\n')
            print(breaker)

            print(f'BUNDLING {pass_num + 1}. ITEMS {num_payments}\n')

            payment_ids = Payment.objects.filter(
                status=states.NEW)[:num_payments].values_list('pk')

            with capture_time(num_payments):
                with capture_db_stats():
                    with capture_mem():
                        compose_bundles(associate, payment_ids=payment_ids, h2h=h2h)

            print(breaker)

    return profile_bundles_creation_


@pytest.fixture
def capture_mem():
    """Контекстный менеджер.
    Собирает при помощи модуля psutil информацию об использованной памяти и выводит."""

    @contextmanager
    def capture_mem_():
        import os
        import psutil

        def getmem():
            return psutil.Process(os.getpid()).memory_info().rss / 1048576

        start = getmem()

        try:
            yield

        finally:
            finish = getmem()

            print(f'MEM MB: {round(start, 3)} -> {round(finish, 3)} (diff {round(finish-start, 3)})')

    return capture_mem_


@pytest.fixture
def capture_time():
    """Контекстный менеджер.
    Собирает информацию о времени выполнения кода и выводит."""

    @contextmanager
    def capture_time_(items_count=None):
        from time import time

        start = time()

        try:
            yield

        finally:
            spent = round(time()-start, 2)

            per_item = ''
            if items_count is not None:
                per_item = f' ({round(spent / items_count, 2)} per item)'

            print(f'TIME S: {spent}{per_item}')

    return capture_time_


@pytest.fixture
def capture_db_stats():
    """Контекстный менеджер.
    Собирает информацию о количестве обращений к БД."""

    @contextmanager
    def capture_db_stats_():
        from functools import reduce
        from django.apps import apps

        core = apps.get_app_config('core')

        def clear():
            core.dev_log['db'].clear()

        def reducer(accum, item):
            count = 0

            if not item[0].startswith('sasl'):
                count = len(item[1])

            return accum + count

        clear()

        try:
            yield

        finally:
            total_queries = reduce(reducer, core.dev_log.items(), 0)
            clear()

            print(f'DB QUERIES: {total_queries}')

    return capture_db_stats_


@pytest.fixture
def compose_bundles():
    """Собирает пакет (или несколько) из указанных (либо имеющихся новых) платежей."""

    def compose_bundles_(associate, *, payment_ids=None, h2h=True):
        """
        :param Associate associate: Объект внешней системы.
        :param list payment_ids: Идентификаторы платежей, из которых требуется составить пакет.
        :param bool h2h: Использовать формат для сервер-сервер.
        """
        payment_ids = payment_ids or Payment.objects.filter(status=states.NEW).values_list('pk')

        dispatcher = get_associate(associate.id).payment_dispatcher
        result = dispatcher.bundles_to_file(
            dispatcher.bundle_compose(payment_ids, for_h2h=h2h))

        return result

    return compose_bundles_


@pytest.fixture
def get_source_payment_mass(get_source_payment, init_user, get_assoc_acc_curr):
    """Массово создаёт и возвращает объекты исходных платежей."""

    def get_source_payment_mass_(count, associate, *, attrs=None, service=False, org=None, set_account: bool = False):
        """
        :param int count: Количество платежей, которые требуется сформировать.
        :param Associate associate: Объект внешней системы.
        :param attrs: Общие атрибуты платежей.
        :param bool service: Платёж от сервиса.
        :param org:

        """
        get_assoc_acc_curr(get_associate(associate.id), account='40702810800000007671', org=org)

        init_user()

        payments = []

        for idx in range(count):
            payments.append(get_source_payment(
                attrs=attrs, associate=associate, service=service, set_account=set_account
            ))

        return payments

    return get_source_payment_mass_


@pytest.fixture
def get_salary_registry(get_salary_contract):
    """Создаёт и возвращает объект зарплатного реестра указанного типа."""

    def get_salary_registry_(
        associate,
        cls, *,
        reg_id=None,
        employees=None,
        contract_account=None,
        org=None,
        contract_number=None,
        automated_contract=False,
        **kwargs
    ):
        contract = get_salary_contract(
            associate, account=contract_account, org=org, number=contract_number, automated=automated_contract
        )

        if employees is None:
            employees = []

        cls_kwargs = dict(
            registry_id=reg_id or uuid4(),
            associate_id=contract.associate_id,
            created_dt=datetime.now(),
            contract=contract,
            employees=employees,
            employees_count=len(employees),
        )
        cls_kwargs.update(kwargs)

        registry = cls(**cls_kwargs)
        registry.save()

        return registry

    return get_salary_registry_


@pytest.fixture
def get_salary_contract(make_org):
    """Создаёт и возвращает объект зарплатного договора."""

    def get_salary_contract_(
        associate: Associate,
        org: Union[Organization, str, dict] = None,
        *,
        account=None,
        number=None,
        transit_acc='',
        automated=False,
    ):

        org = org or 'fakeorg'
        number = number or 'fakedContractNumber'
        account = account or '1234567890'

        if not isinstance(org, Organization):

            org_kwargs = {
                'connection_id': 'faked',
                'name': org,
                'inn': '3456781209',
            }

            if isinstance(org, dict):
                org_kwargs.update(org)
                org = org['name']

            org = Organization.objects.filter(name=org).first()

            if org is None:
                org_kwargs['id'] = 1
                org = make_org(**org_kwargs)

        contract = SalaryContract.objects.filter(number=number).first()

        if contract is None:
            contract = SalaryContract(
                org=org,
                associate_id=associate.id,
                number=number,
                account=account,
                transit_account=transit_acc,
                automate_registers=automated,
            )
            contract.save()

        return contract

    return get_salary_contract_


@pytest.fixture
def salary_parse_from_fixture(read_fixture):
    """Разбирает ответный реестр зарплатного раздела из фикстуры
    и возвращает словарь, индексированный типами реестров, где значения
    - кортежи вида (словарь_ответного_реестра, объект_реестра_исходного).

    """
    def parse_from_fixture_(filename, op):
        datafile = BytesIO(read_fixture(filename))

        result = defaultdict(list)

        parsed = op.file_parser(datafile=datafile, operator=op, filename=filename).parse(datafile)

        for registry_cls, registries_in in parsed.items():

            for filename_reg, registry_in in registries_in:
                registry_out = registry_cls.incoming_get_registry(op, registry_in)
                result[registry_cls].append((registry_in, registry_out))

        return result

    return parse_from_fixture_


@pytest.fixture
def get_source_payment_dict(init_user):

    def get_source_payment_dict_(attrs=None, *, associate=0, number=1, service=False):

        attrs = attrs or {}
        num = str(number)

        now = datetime.now()

        if service:
            kwargs = dict(
                summ=Decimal('152'),
                currency_id=Currency.RUB,
                ground='Назначение',
                service_id=Service.TOLOKA if isinstance(service, bool) else service,
                associate_id=getattr(associate, 'id', associate),
            )

        else:
            kwargs = dict(
                number=str(num),
                number_src=f'{num}-1c',
                associate_id=getattr(associate, 'id', associate),

                f_acc='40702810800000007671',
                f_iban='0000000000000001',
                f_inn='7705713772',
                f_kpp='123456789',
                f_name='OOO Яндекс',
                f_bic='044525700',
                f_swiftcode='OWHBDEFF',  # Не наш, зато валидный.
                f_cacc='30101810200000000700',
                f_bankname='АО БАНК1',

                t_acc='40702810301400002360',
                t_iban='0000000000000002',
                t_inn='7725713770',
                t_kpp='987654321',
                t_name='ООО "Кинопортал"',
                t_bic='044525593',
                t_swiftcode='OWHBDEFF',
                t_cacc='30101810200000000593',
                t_bankname='АО БАНК2',
                t_bank_city='Москва',
                t_address='г Москва, ул. Краснопролетарская, д. 1, стр. 3',

                ground='Назначение',
                date=now.date(),
                number_oebs=num,
                summ=Decimal('152'),
                oper_code='22534',
                priority=5,
                opertype='01',
                user=init_user(),
            )

        kwargs.update(attrs)

        return kwargs

    return get_source_payment_dict_


@pytest.fixture
def api_register_payments():
    """Регистрирует платежи, описанные словарями на тестовом BCL.

    Удобно для генерирования платежей на тесте, например для тестирования песочниц.

    """
    def api_register_payments_(*, payments: List[dict], tvm_secret: str = None) -> Tuple[bool, list]:

        from bclclient import Bcl, HOST_TEST
        from bclclient.settings import TVM_ID_TEST

        if not tvm_secret:
            tvm_secret = settings.ENVIRON['TVM_SECRET']

        bcl_test = Bcl(auth=(TVM_ID_TEST, tvm_secret), host=HOST_TEST)
        Payment = bcl_test.payments.cls_payment

        payments_ = []

        for pay in payments:
            num = pay.pop('number')

            acc_from = pay.pop('f_acc')
            acc_to = pay.pop('t_acc')
            amount = pay.pop('summ')
            purpose = pay.pop('ground')
            date = pay.pop('date', f'{datetime.now().date()}')

            pay.update({
                'id': num,
                'user': 'robot-bcl',
                'acc_from': acc_from,
                'acc_to': acc_to,
                'amount': amount,
                'purpose': purpose,
                'date': date,
            })
            payments_.append(Payment(**pay))

        succeed, failed = bcl_test.payments.register(*payments_)
        return succeed, failed

    return api_register_payments_


@pytest.fixture
def get_source_payment(get_source_payment_dict, get_assoc_acc_curr):
    """Создаёт и возвращает объект исходного платежа."""

    number = [0]

    def get_source_payment_(
        attrs: dict = None,
        *,
        service: bool = False,
        associate: Associate = None,
        callback: Callable = None,
        set_account: bool = False,
    ) -> Payment:
        """
        :param attrs:

        :param service: Нужно ли сформировать платёж в платёжную систему (а не банк).
            Можно указать идентификатор сервиса. Если True, то используется Толока.

        :param associate: Объект внешней системы, либо её идентификатор.

        :param callback: Функция для формирования базового набора параметров объекта платежа.
            Если не задана, то используется наполнение от get_source_payment_dict.

        :param set_account: Следует ли проставить в атрибуты платежа данные
            о счёте и его валюте.

        """
        if associate is None:
            associate = Sber

        attrs = attrs or {}
        attrs.pop('statement_payment', '')

        number[0] += 1

        num = str(number[0])

        callback = callback or get_source_payment_dict

        if service:
            base_attrs = callback(
                associate=associate,
                number=num,
                service=service,
            )

        else:
            now = datetime.now()

            base_attrs = callback(dict(
                currency_id=Currency.RUB,
                oper_code='22534',
                paid_by='OUR',
                n_kbk='',
                n_okato='7766',
                n_doc_date='12-11-2017',
                n_period='5',

                expected_dt=now,
                official_name='some',
                official_phone='+7 345',
                currency_op_docs=next(iter(Payment.CURRENCY_OP_DOCS.keys())),
                contract_currency_id=Currency.RUB,
                contract_sum=Decimal('300.3'),
                trans_pass='12345678/1234/1234/1/1',

            ), associate=associate, number=num)

        if set_account:
            _, acc, _ = get_assoc_acc_curr(
                associate,
                account=attrs.get('f_acc', '40702810800000007671'),
                curr=attrs.get('currency_id', Currency.RUB),
            )
            account_id = acc.id
            attrs.update({
                'account_id': account_id,
                'currency_id': acc.currency_id,
            })

        base_attrs.update(attrs)

        payment = Payment(**base_attrs)
        payment.save()

        return payment

    return get_source_payment_


@pytest.fixture
def get_proved(get_assoc_acc_curr):
    """Создаёт объект подтвержденного платежа для указанного банка."""

    def get_proved_(associate, acc_num=None, register_kwargs=None, proved_pay_kwargs=None) -> List[StatementPayment]:
        """Возвращает сверку (объект(ы) подтвержденного платежа) для указанного банка.
        Попутно создаются объект выписки и регистр.

        :param Associate associate: Объект внешней системы.
        :param str acc_num: Номер счёта.
        :param dict register_kwargs: Именованные аргументы для создания объекта регистра.
        :param dict|list[dict] proved_pay_kwargs: Именованные аргументы для создания объекта сверки.
            Либо список таких аргументов.

        """
        associate, account, _ = get_assoc_acc_curr(associate, account=acc_num)
        today = DateUtils.today().date()

        statement = Statement(
            associate_id=associate.id,
            user=session.get_current_user(),
        )
        statement.save()

        register_kwargs_ = dict(
            associate_id=associate.id,
            account=account,
            statement_date=today,
            status=StatementRegister.STATUS_VALID,
            statement=statement,
        )
        register_kwargs_.update(register_kwargs or {})

        register = StatementRegister(**register_kwargs_)
        register.save()

        if proved_pay_kwargs is None:
            proved_pay_kwargs = [{}]

        if isinstance(proved_pay_kwargs, dict):
            proved_pay_kwargs = [proved_pay_kwargs]

        result = []

        for idx, kwargs in enumerate(proved_pay_kwargs, 1):

            proved_pay_kwargs_ = dict(
                direction=Direction.IN,
                number=str(idx),
                register=register,
                summ=Decimal('123'),
                associate_id=associate.id,
                date_valuated=today,
                date=today,
                currency_id=account.currency_id
            )
            proved_pay_kwargs_.update(kwargs or {})

            payment = StatementPayment(**proved_pay_kwargs_)
            payment.save()

            result.append(payment)

        cache.clear()
        return result

    return get_proved_


@pytest.fixture
def monitor_tester(mocker):
    """Позволяет протестировать монитор."""

    def monitor_tester_(monitor_path):
        """Путь до монитора с точками в качестве разбелителей модулей.

        :param str monitor_path:
        :rtype:
        """
        message_list = []

        mocker.patch(
            f'{monitor_path}.notifier_cls',
            type(str('DummyNotifier'), (NotifierBase,), {'send': lambda self, composed: message_list.append(composed)}))

        return message_list

    return monitor_tester_


@pytest.fixture
def fake_statements_quickcheck(fake_statements, init_user):
    """Производит базовую проверку функционирования генерации и разбора технических выписок."""

    init_user(robot=True)

    def fake_statements_quickcheck_(*, associate: Associate):

        statements = fake_statements(associate, account='5678')
        data = json.loads(statements[0].zip_raw.decode('utf-8'))

        # Проверяем, что проиходит фильтрация по нужному статусу.
        assert Payment.objects.filter(status=associate.statement_downloader_faked.status_complete).count() == 2
        assert len(data['items']) == 1
        assert len(data['items'][0]['payments']) == 2

        parser = associate.statement_dispatcher.get_parser(statements[0])
        result = parser.process()
        register, payments = result[0]

        assert not register.intraday
        assert register.is_valid
        assert len(payments) == 2

        return statements, register, payments

    return fake_statements_quickcheck_


@pytest.fixture
def fake_statements(get_assoc_acc_curr, get_source_payment_mass):
    """Создаёт и возвращает список объектов фиктивных выписок."""

    def fake_statement_(
        associate: Associate,
        *,
        account: Union[Account, str],
        org: Union[Organization, str] = None,
        service: bool = False,
        generate_payments: bool = True,
        parse: bool = False,
        on_date: datetime = None
    ) -> List[Statement]:
        """
        :param associate:
        :param account:
        :param org: Объект или имя организации.
        :param service:
        :param generate_payments: Следует ли автоматически создать исходные платежи
        :param parse: Следует ли разобрать выписки
        :param on_date: За какую дату требуется фиктивная выписка

        """
        associate, account, _ = get_assoc_acc_curr(associate, account=account, org=org)

        if generate_payments:
            get_source_payment_mass(2, associate=associate, attrs={
                'status': associate.statement_downloader_faked.status_complete,
                'f_acc': account.number,
                'dt': DateUtils.yesterday(),
            }, service=service, org=org)

        account.fake_statement = True
        account.save()

        statements = FakedStatementDownloader.process(on_date=on_date)

        if parse:
            for statement in statements:
                parser = associate.statement_dispatcher.get_parser(statement)
                parser.process()

        return statements

    return fake_statement_


@pytest.fixture
def get_statement(init_user):
    """Создаёт объект выписки для указанного банка и типа (итоговая/промежуточная)."""

    user = init_user()

    def get_statement_(
        raw_data,
        associate: Associate,
        *,
        final: bool = True,
        group: str = None,
        status: int = None,
        **kwargs
    ) -> Statement:

        if isinstance(associate, int):
            associate = get_associate(associate)

        return StatementHelper.create_statement(
            associate, data=raw_data, data_hash=str(uuid4()),
            statement_type=Statement.TYPE_FINAL if final else Statement.TYPE_INTRADAY,
            user=user, group=group, status=status, **kwargs)

    return get_statement_


@pytest.fixture
def statement_parse():
    """Ставит в очередь разбора и разбирает выписку. Возвращает резульат разбора."""

    def statement_parse_(statement: Statement = None) -> TypeStatementParseResult:

        if statement:
            statement.schedule()

        with Statement.scheduled(loud=True) as scheduled:
            return scheduled.process()

    return statement_parse_


@pytest.fixture
def parse_statement_fixture(read_fixture, get_assoc_acc_curr, get_statement, statement_parse):
    """Загружает фикстуру выписки и создаёт все необходимые объекты."""

    def parse_statement_fixture_(
        fixture: str,
        associate: Associate,
        acc: Union[List[str], str],
        curr: str = None,
        *,
        mutator_func: Callable = None,
        encoding: str = None,
        from_file: bool = True
    ) -> TypeStatementParseResult:
        """Обёртка для функции-фикстуры.

        :param fixture: Имя файла фикстуры.

        :param associate: Объект внешней системы.

        :param acc: Счёт.

        :param curr: Код валюты.

        :param mutator_func: Функция, изменяющая текст.

        :param encoding: Кодировка, для преобразования в unicode для mutator_func.

        :param from_file: Следует ли трактовать параметр fixture как имя файла фикстуры.
            Иначе трактуется как содержимое.

        """
        for acc in make_list(acc):
            acc, _, cur = acc.partition('--')
            associate, _, _ = get_assoc_acc_curr(associate.id, account={'number': acc, 'currency_code': curr or cur})

        mutator_func = mutator_func or (lambda x: x)

        text = read_fixture(fixture) if from_file else fixture

        if encoding:
            text = text.decode(encoding)

        text = mutator_func(text)

        if encoding:
            text = text.encode(encoding)

        statement = get_statement(text, associate)

        return statement_parse(statement)

    return parse_statement_fixture_


@pytest.fixture
def make_org():
    """Порождает объект организаций с заданными параметрами."""
    def get_org_(**kwargs):
        org = Organization(**kwargs)
        org.save()
        return org
    return get_org_


@pytest.fixture
def make_org_grp():
    """Порождает объект организаций с заданными параметрами."""
    def get_org_grp_(**kwargs):
        org_grp = OrganizationGroup(**kwargs)
        org_grp.save()
        return org_grp
    return get_org_grp_


@pytest.fixture
def get_assoc_acc_curr(init_user, make_org):
    """Создаёт объекты банка, счёта и валюты. Возвращает кортеж из них.

    """
    init_user()

    def get_assoc_acc_curr_(
        associate: Union[Associate, int], *, account: Union[str, dict, Account] = None, org: Organization = None,
        curr: str = 'RUB'
    ) -> Tuple[Associate, Account, Currency]:
        """

        :param associate: объект, id банка
        :param account: объект, номер, словарь с параметрами для создания счета
        :param org: органицация
        :param curr: валюта счета

        """
        acc_num = 'fakeacc'

        acc_obj = account
        acc_is_obj = isinstance(account, Account)

        if acc_is_obj:
            pass

        elif isinstance(account, str):
            acc_num = account
            account = {}

        elif isinstance(account, dict):
            acc_num = account.pop('number') if account.get('number', None) is not None else acc_num
            curr = account.pop('currency_code') if account.get('currency_code') else curr

        else:
            account = {}

        if isinstance(associate, int):
            associate = get_associate(associate)

        if isinstance(curr, str):
            curr = Currency.by_code[curr]

        if not acc_is_obj:

            if org is None:

                try:
                    org = Organization.objects.get(name='fakedorg')

                except DoesNotExist:
                    org = make_org(name='fakedorg')

            else:
                if isinstance(org, str):
                    org = make_org(name=org)
                org.save()

            try:
                acc_obj = Account.objects.get(number=acc_num)

            except DoesNotExist:

                acc_obj = Account(
                    number=acc_num,
                    currency_id=curr,
                    associate_id=associate.id,
                    org=org,
                    settings={
                        'auto_batch': {
                            'days': {
                                str(index): {
                                    'limit_low': 624.34, 'limit_up': 936.58, 'active': True
                                } for index in range(7)
                            }
                        }},
                    **account
                )
                acc_obj.save()

        return associate, acc_obj, curr

    return get_assoc_acc_curr_


@pytest.fixture
def init_user():
    """Создаёт объект пользователя и связанные с ним."""

    def init_user_(
        username=None,
        roles=None,
        *,
        robot=False,
        restr_orgs=None,
        restr_accs=None,
        restr_grps=None, **kwargs
    ) -> User:

        username = username or settings.DEVELOPER_LOGIN

        if robot:
            username = settings.ROBOT_NAME

        user = User.objects.filter(username=username).first()

        if not user:

            role_support = Role.SUPPORT

            user = User(
                username=username,
                roles=roles or [role_support],
                **kwargs
            )

            restr_orgs = restr_orgs or ['*']
            restr_accs = restr_accs or []
            restr_grps = restr_grps or []

            restrictions = user.restrictions.setdefault(role_support, {})

            if restr_orgs:
                restrictions[user.RESTR_ORG] = restr_orgs

            if restr_accs:
                restrictions[user.RESTR_ACC] = restr_accs

            if restr_grps:
                restrictions[user.RESTR_GRP] = restr_grps

            user.save()

        session._THREAD.user = user

        return user

    return init_user_


@pytest.fixture
def robot(init_user):
    """Создаёт и возвращает роботного пользователя."""
    return init_user(robot=True)


@pytest.fixture()
def validate_xml(read_fixture):
    """Используя указанную xsd схему, валидирует указанный xml."""

    def validate_xml_(xsd_fixture_file, xml, skip_err=None):
        """
        :param str xsd_fixture_file:
        :param str xml:
        :param list skip_err: Список пропускаемых ошибок. Задуматесь перед использованием!
        :rtype: tuple
        """
        skip_err = skip_err or []

        schema = etree.XMLSchema(etree.fromstring(read_fixture(xsd_fixture_file)))
        xml = etree.fromstring(xml)

        is_valid = schema.validate(xml)
        err_count = 0
        relevant_err = list()

        if is_valid:
            return is_valid, relevant_err, err_count

        err_count = len(schema.error_log)
        relevant_err = list()

        for err in schema.error_log:
            if any([p in err.message for p in skip_err]):
                err_count -= 1
            else:
                relevant_err.append(err)

        return schema.validate(xml), relevant_err, err_count

    return validate_xml_


@pytest.fixture()
def mock_yoomoney_request_processor(monkeypatch):

    class MockedSigner(Signer):

        def __init__(self):
            super().__init__('', '')

        def decrypt(self, data):
            return data

        def encrypt(self, data):
            return data

    monkeypatch.setattr('bcl.banks.party_yoomoney.common.encryptor', MockedSigner())
    monkeypatch.setattr('bcl.banks.party_yoomoney.common.decryptor', MockedSigner())

    return request_processor


@pytest.fixture
def mock_signer(monkeypatch, mocker):
    """Подменяет метод подписи данных."""

    def mock_signer_(signature_text, serial_number, *, algo='1.2.643.2.2.3'):
        monkeypatch.setattr(SigningRight, 'dss_credentials_get', lambda *a, **kw: 'test')
        monkeypatch.setattr(Documents, '_call', lambda *a, **kw: signature_text)
        monkeypatch.setattr(
            Certificates, 'get_all', lambda *a, **kw: [
                mocker.Mock(serial=serial_number, parsed=mocker.Mock(
                    parsed={'tbsCertificate': {'signature': {'algorithm': algo}}}))])

    return mock_signer_


@pytest.fixture
def get_signing_right(init_user):
    """Создаёт и возвращает право подписи для пользователя."""

    def get_signing_right_(associate_id, serial_number, *, username='testuser', org: Organization = None):
        user = init_user(username)

        result = SigningRight(
            user=user,
            level=2,
            dss_credentials='test',
            associate_id=associate_id,
            serial_number=serial_number,
            org=org,
        ).save()
        return result

    return get_signing_right_


@pytest.fixture
def get_payment_creator():
    """Создаёт и возвращает создателя пакетов платежей."""

    def get_payment_creator_(associate, bundle):
        """Обёртка для функции-фикстуры.
        :param Associate|int associate: Объект внешней системы либо его идентификатор.
        :param PaymentsBundle bundle: Пакет для которого необходим создатель платежей.
        :return: Возвращает экземпляр создателя пакетов платежей.
        """
        if isinstance(associate, int):
            associate_id = associate
        else:
            associate_id = associate.id
        return get_associate(associate_id).payment_dispatcher.get_creator(bundle)

    return get_payment_creator_


@pytest.fixture
def check_selfemployed(build_payment_bundle, read_fixture_from_dir, path_module, get_statement, get_salary_contract):

    def check_selfemployed_(
        associate: Associate,
        *,
        file_alias: str = None,
        payment_dicts: List[dict] = None,
        func_mutate_fixture: Callable = None,
    ):
        """

        :param associate: Внешняя система.

        :param file_alias: Псевдоним для файла фикстуры (используется как префикс).

        :param payment_dicts: Словари с данными платежей.

        :param func_mutate_fixture: Функция для изменения текста фикстуры.
            Если не указана, будет использована функция идентичности.

        """
        file_alias = file_alias or associate.alias
        selfempl = Payment.PAYOUT_TYPE_SELFEMPLOYED

        payment_dicts = payment_dicts or []

        for idx in range(1, 11):
            payment_dicts.append({
                'payout_type': selfempl,
                't_name': f'Иванов Иван Иванович{idx}',
                't_fio': f'Иванов|Иван|Иванович{idx}',
                'payroll_num': '1020',
                't_acc': f'9{idx}',
            })

        pay_dict = [
            {
               'payout_type': selfempl, 't_name': 'Иванов Иван Иванович ', 'payroll_num': '1020',
               't_acc': '10',
               't_fio': 'Неиванов|Неиван|Неиванович',
            },
            {
               'payout_type': selfempl, 't_name': 'Петров Пётр  Петрович', 'payroll_num': '1020',
               't_acc': '20',
               't_fio': 'Непетров|Непётр|Непетрович',
            },
            {
               'payout_type': selfempl, 't_name': 'Ильин Илья Ильич', 't_fio': 'Ильин|Илья|Ильич',
               'payroll_num': '3040',
               't_acc': '30'
            },
            {
               'payout_type': selfempl, 't_name': 'Сидоров Сидор', 't_fio': 'Сидоров||',
               'payroll_num': '1020'
            },
            {},
        ] + payment_dicts

        get_salary_contract(associate, account='TECH000777', number='38143037')

        bundles = build_payment_bundle(associate=associate, account={'number': 'TECH000777'}, payment_dicts=pay_dict)

        assert len(bundles) == 3
        # Один платёж был не самозанятому, ушёл в отдельный пакет.
        # Платежи самозанятым разбились на пакеты по организациям - 2 шт.

        bundle = bundles[0]
        remote_id = bundle.remote_id
        assert remote_id
        assert bundle.payments[0].is_payout_selfemployed
        assert len(bundle.payments) == len(pay_dict) - 3

        compiled = bundle.tst_compiled
        assert remote_id in compiled
        assert '<Фамилия>Непетров</Фамилия>' in compiled
        assert '<Фамилия>Иванов</Фамилия>' in compiled
        assert 'Сидоров' not in compiled

        def mutate(text):
            text = text.replace(
                'reg_num', remote_id).replace(
                'reg_id', str(bundle.id if associate is Sber else UUID(int=bundle.number))).replace(
                'reg_date', bundle.dt.strftime('%Y-%m-%d')
            )
            return (func_mutate_fixture or (lambda txt: txt))(text)

        if associate is Sber:
            statement_content = read_fixture_from_dir(
                'sber_selfempl_wo_res.xml', path_module=join(dirname(path_module), 'party_sber'),
                root_path=True
            )
            encoding = associate.statement_dispatcher.get_parser(statement_content).encoding
            body = mutate(statement_content.decode(encoding)).encode(encoding)
            statement = get_statement(body, associate.id)
            results = associate.statement_dispatcher.get_parser(statement).process()

            assert len(results[0][1]) == 3

            for payment in results[0][1]:
                assert payment.status == payment.payment.status == states.PROCESSING

        if associate is Tinkoff:
            statement_content = read_fixture_from_dir(
                'tinkoff_selfempl_declined.xml', path_module=join(dirname(path_module), 'party_tinkoff'),
                root_path=True
            )
            encoding = associate.statement_dispatcher.get_parser(statement_content).encoding
            body = mutate(statement_content.decode(encoding)).encode(encoding)
            statement = get_statement(body, associate.id)
            results = associate.statement_dispatcher.get_parser(statement).process()

            assert len(results[0][1]) == 2

            for payment in results[0][1]:
                assert payment.status == payment.payment.status == states.DECLINED_BY_BANK

            # обновляем отклоненные платежи и проверяем статусы
            bundle.refresh_from_db()

            for payment in bundle.payments:

                if payment.t_fio == 'Некий|Кто|То':
                    assert payment.status == states.EXPORTED_ONLINE

                if payment.t_fio == 'Иванов|Иван|Иванович3':
                    assert payment.status == states.DECLINED_BY_BANK

        body = mutate(
            read_fixture_from_dir(
                f'{file_alias}_selfempl.xml', path_module=join(dirname(path_module), f'party_{file_alias}'),
                root_path=True
            ).decode('cp1251')
        ).encode('cp1251')

        statement = get_statement(body, associate.id)
        results = associate.statement_dispatcher.get_parser(statement).process()

        assert len(results) == 1

        return results[0][1], bundle

    return check_selfemployed_


@pytest.fixture
def get_editable_fields_value():

    def wrapper(page_content: bytes) -> Tuple[Dict[str, str], List[str], List[str]]:

        page_content = page_content.decode('utf8').replace('<strong>', '').replace('</strong>', '')
        page_content = page_content[
            page_content.index('<!-- payarea-start -->'):page_content.index('<!-- payarea-end -->')
        ]
        pattern = re.compile("(?<=<div).*?(?=</div>)", re.IGNORECASE | re.DOTALL)
        fields = re.findall(pattern, page_content)
        editable_fields = {}
        non_editable = []
        for field in fields:
            if 'hidden' in field:
                continue

            if not ('input' in field or 'textarea' in field):
                continue

            if 'textarea' in field:
                result = re.findall('>\n(\w.*)</textarea>', field).pop()

            elif 'checkbox' in field:
                result = '1' if 'checked' in field else '0'

            else:
                result = re.findall('value="(.+?)"', field)[0] if 'value' in field else ''

            if 'disabled' in field:
                non_editable.append(re.findall('name="(\w+)"', field)[0])
            else:
                editable_fields[re.findall('name="(\w+)"', field)[0]] = result

        return editable_fields, non_editable, re.findall('(<button type="submit[^<]+</button>)', page_content)

    return wrapper


@pytest.fixture
def verify_statement_parse(fixturesdir, get_statement, get_assoc_acc_curr, get_payment_bundle, robot):
    """Позволяет проверить разбор выписки, опционально используя файл с данными по платежам."""

    def verify_statement_parse_(
        *,
        associate: Associate,
        accounts: List[str],
        statement_file: str,
        payments: Union[str, List] = None,
        bundle_id: int = None
    ):
        """Позволяет проверить разбор выписки, опционально используя файл с данными по платежам (например, дамп c боя).

        Например:
            out = verify_statement_parse(
                associate=Tinkoff,
                accounts=['TECH40702810510000309857'],
                statement_file='statement.xml',
                payments_file='dump.csv',
                bundle_id=9527399,
            )

        :param associate: Внешняя система.

        :param accounts: Счёта, которые требуется создать.

        :param statement_file: Имя файла с выпиской. Должен находиться в директории с фикстурами данных.

        :param payments:
            Данные платежей, которые требуется создать до разбора выписки.

            Может быть указано имя файла с платежами. Файл должен находиться в директории с фикстурами данных.
            Ожидается файл csv с данными платежей. Например, платежи можно выгрузить в csv с боя при помощи
            DataGrip (в PyCharm). Файл должен содержать первой строкой имена колонок (Add Column Header).

        :param bundle_id: Ид для создаваемого пакета платежей, если он важен.

        """
        accounts.reverse()

        account = None
        for acc in accounts:
            _, account, _ = get_assoc_acc_curr(associate, account=acc)

        fields_map = {field.db_column or field.name: field.name for field in Payment._meta.fields}
        nullable = {field.name for field in Payment._meta.fields if field.null}

        if isinstance(payments, str):
            reader = csv.DictReader(fixturesdir.read(payments).decode().splitlines())

            payments = []

            for item in reader:
                out = {}
                for key, val in item.items():

                    key = fields_map.get(key, key)

                    if val == 'false':
                        val = False

                    elif val == 'true':
                        val = True

                    elif key.endswith('dt') or key == 'date':
                        if val == '':
                            val = None
                        else:
                            val = val.replace('+00', '')

                    elif not val and ('sum' in key or key in nullable):
                        val = None

                    out[key] = val

                out['user_id'] = robot.id

                payments.append(out)

        bundle = get_payment_bundle(payments or [], associate=associate, account=account, id=bundle_id)
        statement = get_statement(fixturesdir.read(statement_file), associate)
        results = associate.statement_dispatcher.get_parser(statement).process()

        return bundle, statement, results

    return verify_statement_parse_


@pytest.fixture
def init_contract():
    """Позволяет получить объект контракта."""
    def init_contract_(unumber: str = '', *, associate: Associate = None):
        associate = associate or Ing
        return Contract.objects.create(
            unumber=unumber or (f'{uuid4()}'[:35]),
            associate_id=associate.id,
        )
    return init_contract_


@pytest.fixture
def get_document(get_assoc_acc_curr, init_user, init_doc_prove, init_contract):
    """Создаёт и возвращает документ."""

    def get_document_(
        *,
        associate: Associate,
        user: User = None,
        generate: str = '',
        payments: list = None,
        attachments: Dict[str, bytes] = None,
        account: Account = None,
        **kwargs
    ):
        user = user or init_user()

        record = DocRegistryRecord(
            associate_id=associate.id,
            user=user,
            **kwargs
        )

        if generate:
            contract = init_contract()
            _, acc, _ = get_assoc_acc_curr(associate=associate, account=account)

            payments = payments or [{}]
            for payment in payments:
                payment['f_acc'] = acc.number
                payment['account'] = acc

            prove_1 = init_doc_prove(
                user=user,
                associate=associate,
                account=acc,
                contract=contract,
                payments=payments,
                kind_code='2211',
                currency_id=643,
                summ='134.5',
                number='prove12',
                date='2020-10-13',
                contract_currency_id=643,
                contract_summ='431.5',
                delivery=77,
                date_expected='2020-10-14'
            )

            # динамический атрибут для облегчения доступа в тестах
            record.dyn_prove = prove_1

            for attach_name, attach_content in (attachments or {}).items():
                prove_1.attachment_add(
                    name=attach_name, content=attach_content, content_type='text/plain')

            doc_date = '2021-10-02'

            if generate == 'svo':
                svo = Svo.objects.create(
                    associate_id=associate.id,
                    user=user,
                    account=acc,
                    contract=contract,
                    date=doc_date,
                )

                svo.set_items([
                    SvoItem(
                        prove=prove_1,
                        num='999',
                        note='somenote',
                        payment=prove_1.payments.order_by('id').first(),
                    ),
                ])
                record.link_svo = svo

            elif generate == 'spd':

                spd = Spd.objects.create(
                    associate_id=associate.id,
                    user=user,
                    account=acc,
                    contract=contract,
                    date=doc_date,
                )

                spd.set_items([
                    SpdItem(
                        prove=prove_1,
                        num='888',
                        note='othernote',
                        correction='2021-10-12',
                    ),
                ])
                record.link_spd = spd

            elif generate.startswith('letter'):

                is_prove = generate == 'letter_prove'
                type_id = ProveLetter.id

                letter = Letter.objects.create(
                    associate_id=associate.id,
                    user=user,
                    account=acc,
                    contract=contract,
                    date=doc_date, type_id=type_id,
                    subject='a-band-new-letter',
                    body='this is for you',
                )
                if is_prove:
                    letter.proves.add(prove_1)
                record.link_letter = letter

                for attach_name, attach_content in (attachments or {}).items():
                    letter.attachment_add(
                        name=attach_name, content=attach_content,
                        content_type='text/plain')

        record.save()

        return record

    return get_document_


@pytest.fixture
def mock_gpg(monkeypatch, mocker):

    dummy_communicate = mocker.MagicMock()
    dummy_communicate.return_value = b'-----BEGIN PGP MESSAGE-----\nsigned', ''

    monkeypatch.setattr('bcl.toolbox.signatures.Popen.communicate', dummy_communicate)

    return dummy_communicate


@pytest.fixture
def mock_gpg_decrypt(monkeypatch, mocker):
    """Имитирует обращение к дешифратору gpg,
    когда нужно, чтобы результатом дешифрования стали исходные же (нешифрованные) данные.

    """
    def communicate(popen, data):
        return data, ''

    monkeypatch.setattr('bcl.toolbox.signatures.Popen.communicate', communicate)


class BotoClientMock:
    """Имитирует некоторые функции boto3.

    Атрибуты состояния намереннно определёны списком на уровне класса
    для удобства ослеживания действий с клиентом, между его разными инициализациями.

    При конкурентном запуске возможно состояние гонки.

    """
    log = []
    meta = {}
    file = BytesIO()

    def __init__(self, *args, **kwargs):
        pass

    @staticmethod
    def clean():
        BotoClientMock.log.clear()
        BotoClientMock.meta.clear()
        BotoClientMock.file.flush()

    def create_bucket(self, Bucket):
        BotoClientMock.log.append(f'create {Bucket}')

    def list_buckets(self):
        BotoClientMock.log.append('list buckets')
        return {'Buckets': [
            {'Name': 'bcl'},
        ]}

    def upload_fileobj(self, buffer, bucket, ident, ExtraArgs):
        BotoClientMock.file = buffer
        BotoClientMock.meta = {key.capitalize(): val for key, val in ExtraArgs['Metadata'].items()}
        BotoClientMock.log.append(f'upload {bucket}.{ident} {ExtraArgs}')

    def head_object(self, Bucket, Key):
        BotoClientMock.log.append(f'head {Bucket}.{Key}')
        return {'Metadata': BotoClientMock.meta}

    def download_fileobj(self, bucket, ident, data):
        BotoClientMock.log.append(f'download {bucket}.{ident}')
        data.seek(0)
        BotoClientMock.file.seek(0)
        data.write(BotoClientMock.file.read())


@pytest.fixture
def mock_mds(monkeypatch):
    """Создаёт имитатор для клиента MDS.

    При этом данный имитатор будет доступен в MDS().client.

    """
    monkeypatch.setattr('bcl.toolbox.mds.Mds.client_cls', BotoClientMock)
    yield
    BotoClientMock.clean()


@pytest.fixture
def init_uploaded():
    """Создаёт загружаемый файл."""

    def init_uploaded_() -> UploadedFile:

        uploaded = UploadedFile(
            file=BytesIO(b'abcd'),
            name='some.dat',
            content_type='text/some',
        )
        return uploaded

    return init_uploaded_


@pytest.fixture
def init_doc_prove(init_user, get_source_payment):
    """Создаёт и возвращает документ, могущий быть подтверждающим."""

    def init_doc_prove_(
        *,
        user: User = None,
        associate: Associate = Ing,
        payments: List[Union[Payment, dict]] = None,
        account: Account = None,
        contract: Contract = None,
        **kwargs
    ):
        if account and not account.org.inn:
            account.org.inn = '7777123456'
            account.org.save()

        if user is None:
            user = init_user()

        kwargs = {
            'type_id': Prove.TYPE_ACT,
            'number': '123',
            'kind_code': '2211',
            'currency_id': Currency.RUB,
            **kwargs
        }

        doc = Prove(
            associate_id=associate.id,
            user=user,
            account=account,
            contract=contract,
            **kwargs
        )
        doc.save()

        pays = []
        for payment in (payments or []):
            if isinstance(payment, dict):
                payment = get_source_payment(payment)
            doc.payments.add(payment)
            pays.append(payment)

        # динамический атрибут для облегчения доступа в тестах
        doc.dyn_payments = pays

        return doc

    return init_doc_prove_


@pytest.fixture
def check_client_response():
    """Производит запрос веб-клиентом и проверяет ответ."""

    def check_client_response_(
        url: str,
        params: dict = None,
        *,
        client: Client = None,
        files: Union[list, dict] = None,
        method: str = 'get',
        file_key: str = 'statement_file',
        extra_headers: dict = None,
        check_content: Union[str, List[str], Callable, List[Callable]] = None
    ) -> Union[HttpResponse, bool]:
        client = client or Client()

        params = params or {}

        if files:
            if isinstance(files, dict):
                params.update(files)

            else:
                params[file_key] = files

        kwargs = {**(extra_headers or {})}

        is_json = method == 'json'
        if is_json:
            method = 'post'
            kwargs['content_type'] = 'application/json'

        result = getattr(client, method)(url, params, **kwargs)

        def simple_check(value):
            # простая проверка на наличие строки в ответе
            def checker(content):
                content = content.decode()

                if value.startswith('!'):
                    value_ = value[1:]
                    assert value_ not in content, f'{value_} not in \n{content}'
                    return True

                assert value in content, f'{value} in \n{content}'
                return True

            return checker

        if check_content is None:
            check_content = [lambda content: True]

        if not isinstance(check_content, list):
            check_content = [check_content]

        results = []

        for a_check in check_content:
            if isinstance(a_check, str):
                a_check = simple_check(a_check)
            results.append(a_check(result.content))

        return result.status_code in (200, 302) and all(results) and result

    return check_client_response_


@pytest.fixture
def table_request(check_client_response):
    """Позволяет отправить запрос на произведение действия над записями таблицы."""

    def table_request_(*, url, realm: str, action: str, items: list, associate: Associate, **more):
        data = {
            'action': action,
            'table_request': '1',
            'items': json.dumps(items),
            'realm': realm,
            'associate_id': associate.id,
            **more,
        }
        return check_client_response(url, params=data, method='post')

    return table_request_


# Обратная карта внутреннего идентификатора сервиса к его tvm_id
BCL_ID_TO_TVM_ID: Dict[int, int] = {bcl_id: tvm_id for tvm_id, bcl_id in Service.tvm_ids.items()}


@pytest.fixture
def patch_tvm_auth(monkeypatch):
    def patch_tvm_auth_(tvm_id, service_id=None):
        def auth(request):
            user = authenticate(request)
            user.service_ticket = namedtuple('FakedTicket', ['src'])(
                tvm_id or BCL_ID_TO_TVM_ID.get(service_id))
            return user

        monkeypatch.setattr('bcl.toolbox.tvm.authenticate', auth)

    return patch_tvm_auth_


@pytest.fixture
def request_factory():
    rf = RequestFactory()
    rf.cookies['yandex_login'] = settings.YAUTH_TEST_USER
    return rf


@pytest.fixture
def mock_yauth_user():
    original = settings.YAUTH_TEST_USER

    def mock_yauth_user_(username):
        settings.YAUTH_TEST_USER = username

    yield mock_yauth_user_
    settings.YAUTH_TEST_USER = original


@pytest.fixture
def stabilize_payment_dt(time_freeze):
    """При генерировании платежей ввиду того, что для BaseModel.dt и update_dt
    используется нестандартный способ инициализации значений с целью
    обеспечения постоянства резултатов в тестах, ориентирующихся
    на данные в указанных полях, может использоваться данный менеджер контекста.

    :param time_freeze:

    """
    @contextlib.contextmanager
    def stabilize_payment_dt_(dt: str, *, bundles: List[PaymentsBundle]):
        """

        :param dt: Датавремя, действующее внутри менеджера.
        :param bundles: Пакеты, платежам которых требуется проставить указанное датувремя.

        """
        pays = []

        with time_freeze(dt):
            now = datetime.now()

            for bundle in bundles:
                for payment in bundle.payments:
                    payment.dt = now
                    payment.update_dt = now
                    pays.append(payment)

            Payment.objects.bulk_update(pays, fields=['dt', 'update_dt'])

            yield now

    return stabilize_payment_dt_
