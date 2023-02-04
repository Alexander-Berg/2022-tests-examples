import os
from contextlib import nullcontext
from contextlib import contextmanager
from datetime import timedelta
from os.path import abspath, dirname, join
from typing import Callable, List, Union, Type

import django
import pytest
from django.conf import settings
from django.utils import timezone, translation
from freezegun import freeze_time
from rest_framework.test import APIClient

from mdh.core.changes import ChangeType
from mdh.core.models import (
    User, Domain, Schema, Node, Reference, Resource, STATUS_PUBLISHED, Lock, Batch, BatchChange, Record,
)
from mdh.core.schemas.attributes import SchemaField
from mdh.core.tasks import process_queue, cleanup
from mdh.core.toolbox.localized import LANG__DEFAULT
from mdh.core.toolbox.tasks import get_registered_task

try:
    import library.python
    import pkgutil
    django.setup()  # Аркадийный pytest-django на этом моменте ещё не сконфигурировал Django.

    ARCADIA_RUN = True

except ImportError:
    ARCADIA_RUN = False

try:
    from envbox import get_environment
    environ = get_environment()

except ImportError:
    environ = os.environ



@pytest.fixture(autouse=True)
def trans():
    translation.activate('en')


@pytest.fixture(autouse=True)
def db_access_on(django_db_reset_sequences,):
    """Используем бд в тестах без поимённого маркирования."""


@pytest.fixture
def basic_schema_fields(init_schema_attr_dict):

    fields = [
        init_schema_attr_dict(
            titles=f'Текстовое поле',
            hints=f'Описание для текстового поля',
            alias='string1',
            type='str',
            default='myvalue1',
            choices={
                'const': [
                    {'val': 'myvalue1', 'titles': {LANG__DEFAULT: 'Значение 1'}},
                    {'val': 'myvalue2', 'titles': {LANG__DEFAULT: 'Значение 2'}},
                    {'val': 'myvalue3', 'titles': {LANG__DEFAULT: 'Значение 3'}},
                ],
            },
            validators=[
                {'alias': 'strlen', 'params': {'min': 1, 'max': 30}},
                {'alias': 're', 'params': {'val': 'myvalue*'}},
            ],
        ),

        init_schema_attr_dict(
            titles=f'Поле для целого',
            hints=f'Описание для поля с целым',
            alias='integer1',
            type='int',
        ),
    ]

    return fields.copy()


@pytest.fixture
def init_schema_attr_dict():
    """Возвращает словарь с данными для атрибута схемы."""

    def init_schema_attr_dict_(*, alias: str, type: str, type_params: dict = None, **kwargs):

        attr_dict = {
            'alias': alias,
            'titles': f'{alias}_title',
            'hints': f'{alias}_hint',
            'type': {'alias': type, 'params': type_params or {}},
        }

        attr_dict.update(kwargs)

        return SchemaField.structurize(attr_dict)

    return init_schema_attr_dict_


@pytest.fixture
def init_resource(init_domain, init_schema, init_reference, init_node):
    """Создаёт объект ресурса и базовые объекты для него."""

    def init_basic_(
            *,
            user: User,
            alias_postfix: str = '',
            domain: Domain = None,
            node: Node = None,
            reference: Reference = None,
            schema: Schema = None,
            publish: bool = False,
            as_source: bool = True,
    ) -> Resource:
        domain = domain or init_domain(f'dom{alias_postfix}', user=user, publish=publish)
        schema = schema or init_schema(f'sch{alias_postfix}', user=user, publish=publish)
        reference = reference or init_reference(f'ref{alias_postfix}', user=user, schema=schema, publish=publish)
        node = node or init_node(f'node{alias_postfix}', user=user, publish=publish)

        reference_link = domain.add_reference(reference)[0]

        resource = reference_link.resource_add(
            creator=user,
            node=node,
            schema=schema,
            as_source=as_source,
        )
        if publish:
            resource.set_status(STATUS_PUBLISHED)
            resource.save()

        return resource

    return init_basic_


@pytest.fixture
def init_resource_fk(init_user, basic_schema_fields, init_schema_attr_dict, init_resource):
    """Генерирует ресурс, в схеме которого присуствует внешний ключ."""

    def init_resource_fk_(*, alias_postfix: str = '', user: User = None, outer_ref: str = ''):

        user = user or init_user()

        res = init_resource(user=user, alias_postfix=alias_postfix)

        fields = basic_schema_fields.copy()
        fields.extend([
            init_schema_attr_dict(
                alias='fk1',
                type='fk', type_params={'ref': res.reference.alias},
                default=None,
            ),
            init_schema_attr_dict(
                alias='fk2',
                type='fk', type_params={'ref': res.reference.alias},
                default=None,
            ),
            init_schema_attr_dict(
                alias='fk3',
                type='fk', type_params={'ref': res.reference.alias},
                default=None,
            ),
        ])
        if outer_ref:
            fields.append(init_schema_attr_dict(
                alias='fk_outer',
                type='fk', type_params={'ref': outer_ref},
            ))

        schema = res.schema
        schema.display['me'] = 'tstme-{{_master_uid}}'
        schema.fields = fields
        schema.save()

        return res

    return init_resource_fk_


@pytest.fixture
def init_shared(init_user):
    """Создаёт объект заданной общей модели."""

    def init_shared_(model, *, alias: str, user: User = None, **kwargs):

        user = user or init_user()

        publish = kwargs.pop('publish', None)
        if publish:
            kwargs['status'] = STATUS_PUBLISHED

        shared = model(
            alias=alias,
            creator=user,
            editor=user,
            **kwargs
        )
        shared.save()

        return shared

    return init_shared_


@pytest.fixture
def init_node(init_shared):
    """Создаёт объект узла."""

    def init_node_(alias: str, user: User = None, **kwargs) -> Node:
        return init_shared(Node, alias=alias, user=user, **kwargs)

    return init_node_


@pytest.fixture
def init_node_default(init_node):
    """Создаёт объект узла по умолчанию."""

    def init_node_default_(alias: str = '', user: User = None, **kwargs) -> Node:
        return init_node(
            alias or 'front',
            user=user,
            id=settings.MDH_DEFAULT_NODE_ID,
            status=STATUS_PUBLISHED,
            **kwargs
        )

    return init_node_default_


@pytest.fixture
def init_schema(init_shared, basic_schema_fields):
    """Создаёт объект схемы и связанные с ним."""

    def init_schema_(alias: str, user: User = None, add_fields: List[dict] = None, **kwargs) -> Schema:
        kwargs = {
            'fields': basic_schema_fields + (add_fields or []),
            **kwargs
        }
        return init_shared(Schema, alias=alias, user=user, **kwargs)

    return init_schema_


@pytest.fixture
def init_domain(init_shared):
    """Создаёт объект области и связанные с ним."""

    def init_domain_(alias: str, user: User = None, **kwargs) -> Domain:
        kwargs = {'titles': {LANG__DEFAULT: 'Domain title'}, **kwargs}
        return init_shared(Domain, alias=alias, user=user, **kwargs)

    return init_domain_


@pytest.fixture
def init_reference(init_shared, init_schema):
    """Создаёт объект справочника и связанные с ним."""

    def init_reference_(alias: str, user: User = None, auto_schema: bool = False, **kwargs) -> Reference:
        kwargs = {
            'titles': {LANG__DEFAULT: 'Reference title'},
            'queue': settings.STARTREK_QUEUE_DEFAULT, **kwargs,
            'status': STATUS_PUBLISHED,
        }

        if auto_schema:
            kwargs['schema'] = init_schema(alias=alias, user=user)

        return init_shared(Reference, alias=alias, user=user, **kwargs)

    return init_reference_


@pytest.fixture
def init_lock():
    """Создаёт объект блокировки для фонового задания."""

    def init_lock_(name: str, *, result: str = '') -> Lock:
        lock = Lock.objects.create(
            name=name,
            result=result,
        )
        return lock

    return init_lock_


@pytest.fixture
def init_record(init_user, init_resource):
    """Создаёт объект записи и другие базовые объекты, необходимые для его создания."""

    def init_record_(*, alias_postfix: str = '', record_kwargs: dict = None) -> Record:
        user = init_user()
        resource = init_resource(user=user, alias_postfix=alias_postfix)
        record_kwargs = {
            'attrs': {'integer1': 1},
            'issue': 'TESTMDH-1',
            'creator': user,
            **(record_kwargs or {})
        }
        record = resource.record_add(**record_kwargs)
        return record

    return init_record_


@pytest.fixture
def init_records(init_user, init_resource, init_domain, init_reference, init_node):
    """Создаёт записи с указанными идентификаторами."""

    user = init_user('tmp')
    node = init_node('tmp', user=user)

    def init_existing_reference_(alias: str, ids: List[str]):
        resource = init_resource(
            user=user, publish=True,
            domain=init_domain(alias, user=user),
            reference=init_reference(alias, user=user),
            node=node
        )
        schema = resource.schema
        schema.alias = alias
        schema.save()

        records = []

        for id_ in ids:
            record = resource.record_add(creator=user, attrs={'integer1': 1}, status=Record.STATUS_PUBLISHED)
            record.master_uid = id_
            record.save()
            records.append(record)

        return records

    return init_existing_reference_


@pytest.fixture
def init_batch():
    """Создаёт объект пакета изменений."""

    def init_batch_(
        *,
        creator: User,
        hint: str = '',
        publish: bool = False,
        changes: List[BatchChange] = None,
        **kwargs
    ) -> Batch:
        kwargs = kwargs.copy()

        if publish:
            kwargs['status'] = STATUS_PUBLISHED

        obj = Batch.objects.create(
            description=hint,
            creator=creator,
            **kwargs
        )

        changes = changes or []
        for change in changes:
            change.batch = obj
            if publish:
                change.status = STATUS_PUBLISHED
            change.save()

        status = kwargs.get('status')
        if status:
            obj.set_status(status)

        return obj

    return init_batch_


@pytest.fixture
def spawn_batch_change():
    """Конструирует объект изменения для пакета изменений."""

    def spawn_batch_change_(
        *,
        type: Union[Type[ChangeType], int],
        resource: Resource,
        **kwargs
    ) -> BatchChange:

        if issubclass(type, ChangeType):
            type = type.id

        obj = BatchChange(
            type=type,
            creator=resource.creator,
            schema=resource.schema,
            reference=resource.reference,
            **kwargs
        )

        return obj

    return spawn_batch_change_


@pytest.fixture
def init_user():
    """Создаёт объект пользователя и связанные с ним."""

    def init_user_(username: str = None, *, robot: bool = False, roles: list = None, **kwargs) -> User:

        username = username or settings.TEST_USERNAME

        if robot:
            username = settings.ROBOT_NAME

        user = User(
            username=username,
            **kwargs
        )
        pwd = 'testpassword'
        user.set_password(pwd)
        user.testpassword = pwd
        user.save()

        if roles:
            user.role_add(roles=roles, author=user)

        return user

    return init_user_


@pytest.fixture
def run_task():
    """Запускает фоновый процесс по его имени."""

    funcs = {
        func.__name__: func for func in [
            # Поддерживаемые функции.
            process_queue,
            cleanup,
        ]
    }

    def run_task_(name: str):

        target_func = funcs.get(name)

        if target_func:
            # Пробуем запустить функцию (например, таймерную).
            return target_func(None)

        # Пробуем запустить зарегистированное фоновое задание.
        return get_registered_task(name).func()

    return run_task_


@pytest.fixture
def read_fixture(dir_fixtures):

    def read_fixture_(filename):
        file_path = dir_fixtures(filename)

        if ARCADIA_RUN:
            data = pkgutil.get_data(__package__, file_path)

        else:
            with open(file_path, 'rb') as f:
                data = f.read()

        return data

    return read_fixture_


@pytest.fixture
def extract_fixture(read_fixture, tmpdir):
    """Изымает файл фикстуры данных во временную директорию.
    Может потребоваться для работы некоторых тестов как из бинарной сборки,
    так и без неё.

    """
    def extract_fixture_(filename, ensure=False):
        data = read_fixture(filename)
        tmp_filepath = tmpdir.join(filename)
        tmp_filepath.write_binary(data, ensure)
        return f'{tmp_filepath}'

    return extract_fixture_


@pytest.fixture
def dir_fixtures(dir_module):

    def dir_fixtures_(filename=None):
        path_chunks = [dir_module, 'fixtures']
        filename and path_chunks.append(filename)
        return join(*path_chunks)

    return dir_fixtures_


@pytest.fixture
def dir_module(request):
    filename = request.module.__file__
    if not ARCADIA_RUN:
        filename = abspath(filename)
    return dirname(filename)


class UiApiClient(APIClient):
    """Клиент для UI API со вспомогательными инструментами."""

    def __init__(self, *, num_quiries_fixture, enforce_csrf_checks=False, **defaults):
        self.num_quiries = num_quiries_fixture
        super().__init__(enforce_csrf_checks, **defaults)

    def get_response(self, url: str, *, queries_num: int = -1):
        """Возвращает json от GET по указанному адресу.

        :param url: Адрес
        :param queries_num: Дозволенное количество SQL-запросов

        """
        manager = self.num_quiries(queries_num) if queries_num > -1 else nullcontext()

        with manager as _:
            result = self.get(url)
            result = result.json()
            return result

    def get_response_listing(self, url: str, *, queries_num: int = -1):
        """Возвращает списковые данные по указанному адресу.

        :param url: Адрес
        :param queries_num: Дозволенное количество SQL-запросов

        """
        response = self.get_response(url, queries_num=queries_num)
        error = response.get('error')

        if error:
            raise Exception(error)

        return response, [data['id'] for data in response['results']]

    @staticmethod
    def assert_error(response, error: str = 'PermissionDenied'):
        """Проверяет наличие в указанному ответе ошибки.

        :param response: Данные ответа
        :param error: Текстовое описание типа ошибки.

        """
        assert response['error']['type'] == error
        return response

    @staticmethod
    def assert_listing(response_tuple, items: list):
        """Проверяет соответствие списка, полученного в ответ
        на .get_response_listing() указаному списку.

        :param response_tuple: Данные ответа
        :param items: Эталонный список

        """
        _, ids = response_tuple
        assert ids == items
        return response_tuple

    @staticmethod
    def assert_details(response, item_id: int):
        """Проверяет соотвествие ид записи из ответа указанному ид.

        :param response: Данные ответа
        :param item_id: Эталонный ид

        """
        assert response['id'] == item_id
        return response


@pytest.fixture
def drf_client(django_assert_num_queries):
    """Тестовый клиент для django rest framework."""

    def drf_client_(user: User = None):

        client = UiApiClient(
            num_quiries_fixture=django_assert_num_queries,
            HTTP_ACCEPT_LANGUAGE='en,ru;q=0.9'
        )

        if user:
            client.force_authenticate(user=user)

        return client

    return drf_client_


@pytest.fixture
def time_shift():
    """Менеджер контекста. Позволяет передвинуть время вперёд на указанное количество секунд."""

    @contextmanager
    def time_forward_(seconds: int, *, backwards: bool = False):
        now_func: Callable = timezone.now

        if backwards:
            target_dt = now_func() - timedelta(seconds=seconds)
        else:
            target_dt = now_func() + timedelta(seconds=seconds)

        with freeze_time(target_dt):
            yield

    return time_forward_


@pytest.fixture
def mock_solomon(response_mock):
    """Обращения к соломону при использовании этой фикстуры
    будут проходить успешно.

    """
    with response_mock(
        'POST https://solomon-prestable.yandex.net/api/v2/push?project=mdh&cluster=default&service=push -> 200:ok'
    ):
        yield
