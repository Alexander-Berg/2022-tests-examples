import json
from typing import Optional, NamedTuple, Union
from uuid import UUID

from mdh.core.models import (
    Record, ContributorRole, ReaderRole, STATUS_NOMINATED, STATUS_DRAFT, STATUS_PUBLISHED,
    STATUS_ARCHIVED, SupportRole, Constrained,
)
from mdh.core.schemas.widgets import ReadOnly


def test_records(
    drf_client,
    init_user,
    init_resource,
    init_node_default,
    init_schema,
    init_reference,
    basic_schema_fields,
    init_schema_attr_dict,
    django_assert_num_queries,
):

    user = init_user(roles=[ContributorRole])

    user2 = init_user('another')
    node_default = init_node_default(user=user)

    ref = init_reference('tstref', user=user)

    fields = basic_schema_fields.copy()
    fields.append(init_schema_attr_dict(
        titles='fk',
        alias='fk1',
        type='fk',
        type_params={'ref': ref.alias},
        default=None,
    ))

    fields.append(init_schema_attr_dict(
        titles='fk',
        alias='fk2',
        type='fk',
        type_params={'ref': ref.alias},
        default=None,
    ))

    schema = init_schema('tstschema', fields=fields, user=user, display={'me': 'rootrec {{string1}}'})

    resource1 = init_resource(user=user, node=node_default, schema=schema, publish=True, alias_postfix='1')
    resource2 = init_resource(
        user=user,
        node=resource1.node,
        reference=resource1.reference,
        schema=schema,
        alias_postfix = '2'
    )

    record0 = resource1.record_add(creator=user, attrs={'integer1': 1})
    record0.set_master()
    record0.save()

    record1 = resource1.record_add(
        creator=user, attrs={'integer1': 1, 'fk1': record0.master_uid, 'fk2': record0.master_uid})
    record1.set_master()
    record1.save()

    resource2.record_add(creator=user, attrs={'integer1': 22222})

    client = drf_client(user=user)

    # Список с сортировкой по внешним ключам.
    response = client.get(
        f'/uiapi/records/?ordering=attrs__fk1&reference={resource1.reference.id}',
        HTTP_ACCEPT_LANGUAGE='ru,en;q=0.9',  # Для подтягивания display.me из внешнего ключа в правильной локализации
    )
    result = response.data['results']
    assert result[-1]['id'] == record1.id

    # Список полный
    with django_assert_num_queries(3):
        response = client.get('/uiapi/records/?ordering=id,-,')

        assert response.status_code == 200
        data = response.json()
        assert data['count'] == 3

        record = data['results'][0]
        assert record['me']['ru']
        assert record['issue'] == ''
        assert record['creator'] == user.id
        assert record['editor'] is None
        assert 'status_hint' not in record
        assert 'dt_lb' not in record
        assert record['status'] == {'alias': 'draft', 'id': 1, 'title': 'Draft'}
        assert record['is_master']
        assert record['schema'] == schema.id
        assert record['url']
        assert record['record_uid']
        assert record['master_uid']
        assert record['version'] == 1
        assert record['version_master'] == 0
        assert isinstance(record['resource'], int)
        assert not record['foreign']

    # Список с фильтрами
    def test_filters(flt):

        linked = json.dumps({'string1': 'myvalue1'})
        response = client.get(
            f'/uiapi/records/?domain={resource1.reference_link.domain_id}&'
            f'reference={resource1.reference_link.reference_id}&{flt}={linked}&'
            'status=draft,published&status=1'
        ).json()
        assert response['count'] == 2

    test_filters('linked')
    test_filters('flt')

    # Проверка сложных фильтров (orm lookup).
    # Для примера здесь выборка аналогичная fk1 IS NOT NULL
    response = client.get('/uiapi/records/?flt={"fk1__gt": ""}').json()
    assert response['count'] == 1
    assert response['results'][0]['id'] == record1.id

    response = client.get('/uiapi/records/?flt={"fk1__isnull": false}').json()
    assert response['count'] == 1
    assert response['results'][0]['id'] == record1.id

    response = client.get('/uiapi/records/?domain=dom1&reference=ref1').json()
    assert response['count'] == 2

    # Проверка поиска по связанным.
    # Здесь в search указан ключ (ru) вместо значения (rootrec)
    # из-за особенностей поля json в sqlite.
    response = client.get('/uiapi/records/?domain=dom1&reference=ref1&search=ru&search_deep=1').json()
    assert len(response['results']) == 1
    assert 'rootrec' in str(response['results'][0]['me'])

    # Фильтрация по значению атрибута.
    response = client.get('/uiapi/records/?flt={"integer1": 22222}').json()
    assert response['count'] == 1

    # Конкретная запись
    with django_assert_num_queries(3) as _:
        response = client.get(f'/uiapi/records/{record1.id}/')

        data = response.json()
        # В детальной выдаче больше данных, чем в списковой.
        assert 'issue' in data
        assert 'master_uid' in data
        assert data['creator'] == {'id': user.id, 'username': 'tester'}
        assert data['editor'] is None
        assert data['dt_lb'] is None
        assert data['status_hint'] == ''
        assert data['user_actions'] == {'edit': True}
        assert data['attrs'] == {
            'string1': 'myvalue1',
            'integer1': 1,
            'fk1': f'{record0.master_uid}',
            'fk2': f'{record0.master_uid}',
        }
        assert data['foreign']['fk1']['id'] == record0.id
        assert data['foreign']['fk1']['schema']
        assert isinstance(data['resource'], dict)

    # Адресация по uid.
    response = client.get(f'/uiapi/records/{record1.record_uid}/?prefer_master=1')
    assert response.status_code == 200

    # Создание записи.
    def create():

        response = client.post('/uiapi/records/', data={
            'attrs': {
                'integer1': 10,
            },
            'reference': resource1.reference.alias,
            'master_uid': record1.master_uid,
        })
        return response

    with django_assert_num_queries(11) as _:
        # сюда также включены запросы sql, на актуализацию таблицы foreign.
        response = create()
        data = response.json()

        assert response.status_code == 201
        assert data['master_uid'] != data['record_uid']
        record_id = data['id']
        assert record_id
        assert record['version'] == 1
        assert record['version_master'] == 0

    # Для проверки изменения редактора, изменим создателя.
    Record.objects.filter(id=record_id).update(creator=user2, editor=None)

    # Редактирование записи. Проверим что некоторые атрибуты не проставятся.
    with django_assert_num_queries(4) as _:
        response = client.patch(f'/uiapi/records/{record_id}/', data={
            'status': STATUS_PUBLISHED,
            'reference': 'dummy',
            'is_master': True,
            'master_uid': 'xxx',
            'attrs': {
                'integer1': 300,
            },
        })
        data = response.json()

        assert response.status_code == 200
        assert data['creator']['id'] == user2.id
        assert data['editor']['id'] == user.id
        assert data['master_uid'] == str(record1.master_uid)
        assert data['attrs'] == {'fk1': None, 'fk2': None, 'integer1': 300, 'string1': 'myvalue1'}
        assert data['status']['id'] == STATUS_NOMINATED
        assert not data['is_master']
        assert data['version'] == 2
        assert data['version_master'] == 0

    # Проверка фильтрации по master_uid
    response = client.get(f'/uiapi/records/?master_uid={record1.master_uid}').json()
    assert response['count'] == 2

    # Проверка существования уже рассматриваемого кандидата при создании записи.
    candidate = Record.objects.get(id=data['id'])
    candidate.mark_on_review()
    candidate.save()

    response = client.post('/uiapi/records/', data={
        'attrs': {
            'integer1': 40,
        },
        'reference': resource1.reference.alias,
        'master_uid': record1.master_uid,
    })
    data = response.json()
    assert response.status_code == 400
    assert data['error']['type'] == 'ValidationError'

    # Проверка невозможности редактирования записей в некоторых статусах.
    Record.publish(Record.objects.get(id=record_id))
    response = client.patch(f'/uiapi/records/{record_id}/', data={})
    assert response.status_code == 403
    data = response.json()
    assert data['error']['type'] == 'PermissionDenied'

    # Проверка возможности намеренного переноса в архив.
    response = client.patch(f'/uiapi/records/{record_id}/', data={'status': STATUS_ARCHIVED})
    assert response.status_code == 403  # Не передано описание

    response = client.patch(
        f'/uiapi/records/{record_id}/',
        data={'status': STATUS_ARCHIVED, 'status_hint': 'myhint'})
    assert response.status_code == 200
    data = response.json()
    assert data['status']['id'] == STATUS_ARCHIVED
    assert data['status_hint'] == 'myhint'


def test_sorting(drf_client, init_user, init_resource, django_assert_max_num_queries):
    user = init_user(roles=[ReaderRole])

    resource1 = init_resource(user=user, publish=True)

    def add_record(status):
        return resource1.record_add(creator=user, attrs={'integer1': 1}, status=status)

    record_draft = add_record(STATUS_DRAFT)
    record_pub = add_record(STATUS_PUBLISHED)
    record_nom = add_record(STATUS_NOMINATED)
    record_arch = add_record(STATUS_ARCHIVED)

    client = drf_client(user=user)

    def check_ordering(url: str):

        with django_assert_max_num_queries(4) as _:
            response = client.get(url)

        assert response.status_code == 200
        data = response.json()
        assert data['count'] == 4
        assert (
            [result['id'] for result in data['results']] ==
            [record_pub.id, record_nom.id, record_draft.id, record_arch.id]
        )

    check_ordering('/uiapi/records/')
    check_ordering('/uiapi/records/?ordering=schema_id')


def test_slice(
    drf_client,
    init_user,
    init_resource_fk,
    basic_schema_fields,
    init_schema_attr_dict,
    django_assert_num_queries,
):
    resource = init_resource_fk(user=init_user(roles=[ReaderRole]))
    user = resource.creator
    client = drf_client(user=user)

    def add_record(*, issue: str, fk: Optional[UUID], value: int) -> Record:
        record = resource.record_add(creator=user, issue=issue, attrs={'integer1': value, 'fk1': fk})
        Record.publish(record)
        return record

    def get_response(*, fk: Optional[UUID], value: int = 0):

        query = (
            '/uiapi/records/?'
            f"slice={json.dumps({'fk1': str(fk) if fk else None})}"
        )

        if value:
            query = f"{query}&flt={json.dumps({'integer1': value})}"

        response = client.get(query).json()
        return response

    record_0_1 = add_record(issue='I-1', fk=None, value=1)
    record_0_2 = add_record(issue='I-2', fk=None, value=2)
    record_0_3 = add_record(issue='I-3', fk=None, value=2)

    record_1_1 = add_record(issue='I-1-1', fk=record_0_1.master_uid, value=11)
    record_1_2 = add_record(issue='I-1-2', fk=record_0_1.master_uid, value=12)

    record_1_1_1 = add_record(issue='I-1-1-1', fk=record_1_1.master_uid, value=111)

    # Проверка фильтрации с использвоанием Foreign
    with django_assert_num_queries(5) as _:
        data = get_response(fk=record_0_1.master_uid)

        assert data['count'] == 2

        record = data['results'][0]
        assert record['id'] == record_1_1.id
        assert record['children'] == {'fk1': True}

        record = data['results'][1]
        assert record['id'] == record_1_2.id
        assert record['children'] == {'fk1': False}

    # Проверка фильтрации записей в корне.
    with django_assert_num_queries(3) as _:
        data = get_response(fk=None)

        assert data['count'] == 3

        record = data['results'][0]
        assert record['id'] == record_0_1.id
        assert record['children'] == {'fk1': True}

        record = data['results'][1]
        assert record['id'] == record_0_2.id
        assert record['children'] == {'fk1': False}

        record = data['results'][2]
        assert record['id'] == record_0_3.id
        assert record['children'] == {'fk1': False}

    # Проверка дополнительной фильтрации по значениям атрибутов.
    with django_assert_num_queries(3) as _:
        data = get_response(fk=None, value=2)

        assert data['count'] == 2


def test_attrs_restrictions(
    drf_client,
    init_user,
    init_resource,
    init_schema,
    basic_schema_fields,
    init_schema_attr_dict,
    django_assert_num_queries,
):

    user = init_user()

    fields = basic_schema_fields
    fields.append(init_schema_attr_dict(
        alias='read', type='str', default='unchanged'
    ))

    schema = init_schema('sch', fields=fields, user=user)
    # Проверим также, что данные из полей с виджетами
    # «только чтение» не будут записаны в БД.
    schema.display['widgets']['read'] = ReadOnly().to_dict()
    schema.save()

    resource1 = init_resource(user=user, alias_postfix='_one', schema=schema, publish=True)
    resource2 = init_resource(user=user, alias_postfix='_two', schema=schema, node=resource1.node, publish=True)

    for resource in (resource1, resource2):
        # В реальном (не тестовом) мире для ресурса схема
        # (адаптирующая) обычно не выставляется. Приближаем положение дел
        # к реальности:
        resource.schema = None
        resource.save()

    client = drf_client(user=user)

    def add_record(resource, *, attrs: dict, master_uid: Union[UUID, str] = ''):
        kwargs = {}

        if master_uid:
            kwargs['master_uid'] = f'{master_uid}'

        response = client.post('/uiapi/records/', data={
            'attrs': attrs,
            'reference': resource.reference.alias,
            **kwargs
        })
        data = response.json()
        assert response.status_code == 201, f"Create failed: {data['error']['msg']}"
        return data

    def create_record(*, resource, attrs, num_queries):
        with django_assert_num_queries(num_queries) as _:
            data = add_record(resource, attrs=attrs)
        return Record.objects.get(id=data['id'])

    # добавляем записи для последующей модификации
    user.role_add(roles=[SupportRole], author=user)
    record_restricted = create_record(resource=resource1, attrs={'integer1': 11}, num_queries=4)
    record_free = create_record(resource=resource2, attrs={'integer1': 21}, num_queries=3)
    user.roles.all().delete()
    user.refresh_from_db()

    url_listing = '/uiapi/records/'
    url_details_1 = f'/uiapi/records/{record_restricted.id}/'  # ограничиваемая
    url_details_2 = f'/uiapi/records/{record_free.id}/'

    # нет права чтения
    client.assert_listing(
        client.get_response_listing(url_listing, queries_num=2),
        []
    )
    client.assert_error(client.get_response(url_details_1, queries_num=2))
    # нет права на редактирование
    client.assert_error(client.patch(url_details_1, data={'integer1': 999}).json())

    # добавляем неограниченное право на чтение
    role_reader, role_contributor = user.role_add(roles=[ReaderRole, ContributorRole], author=user)
    user.refresh_from_db()  # сборс кеша

    def check_read(*, rec_restricted: dict, rec_free: dict):

        # в списке
        response, ids = client.get_response_listing(url_listing, queries_num=4)
        records = response['results']
        assert records[0]['attrs'] == rec_restricted
        assert records[1]['attrs'] == rec_free

        # в деталировке
        response = client.get_response(url_details_1, queries_num=3)
        assert response['attrs'] == rec_restricted
        response = client.get_response(url_details_2, queries_num=3)
        assert response['attrs'] == rec_free

    class WriteParams(NamedTuple):

        write: dict
        """Что пишем."""

        respond: dict
        """Что получаем в ответе API."""

        db: dict
        """Что находится в БД при редактировани и создании записи."""

        db_create: Optional[dict]
        """Что находится в БД при создании записи. Если не None перекрывает указанное в .db."""

    def check_write(*, rec_restricted: WriteParams, rec_free: WriteParams):

        def get_assert_msg(text):
            return f'{text} mismatch'

        items = [
            ('rec1', url_details_1, rec_restricted, record_restricted, resource1),
            ('rec2', url_details_2, rec_free, record_free, resource2),
        ]

        for alias, url, params, record, resource in items:
            # при редактировании
            # для недозволенных атрибутов берутся имеющиеся значения
            response = client.patch(url, data={'attrs': params.write}).json()
            assert response['attrs'] == params.respond, get_assert_msg(f'{alias} update respond')
            record.refresh_from_db()
            assert record.attrs == params.db, get_assert_msg(f'{alias} update db')

            # при создании новой записи
            # недозволенные атрибуты не фигурируют вовсе
            response = add_record(resource, attrs=params.write)
            assert response['attrs'] == params.respond, get_assert_msg(f'{alias} create respond')

            params_create = params.db_create or params.db
            assert Record.objects.get(id=response['id']).attrs == params_create, get_assert_msg(f'{alias} create db')

    # все атрибуты без ограничений
    check_read(
        rec_restricted={'string1': 'myvalue1', 'read': 'unchanged', 'integer1': 11},
        rec_free={'string1': 'myvalue1', 'read': 'unchanged', 'integer1': 21},
    )

    check_write(
        rec_restricted=WriteParams(
            write={'integer1': 11, 'read': 'override', 'string1': 'myvalueX'},
            respond={'integer1': 11, 'read': 'unchanged', 'string1': 'myvalueX'},
            db={'integer1': 11, 'read': 'unchanged', 'string1': 'myvalueX'},
            db_create=None,
        ),
        rec_free=WriteParams(
            write={'integer1': 21, 'read': 'override', 'string1': 'myvalueY'},
            respond={'integer1': 21, 'read': 'unchanged', 'string1': 'myvalueY'},
            db={'integer1': 21, 'read': 'unchanged', 'string1': 'myvalueY'},
            db_create=None,
        ),
    )

    # ограничиваем атрибуты одного справочника
    for role in (role_reader, role_contributor):
        role.access = {'attrs': {f'{resource1.reference_link.reference_id}': ['integer1']}}
        role.save()
    user.refresh_from_db()

    check_read(
        rec_restricted={'integer1': 11},
        rec_free={'integer1': 21, 'read': 'unchanged', 'string1': 'myvalueY'},
    )

    check_write(
        rec_restricted=WriteParams(
            write={'integer1': 111, 'read': 'override', 'string1': 'myvalueZ'},
            respond={'integer1': 111},
            db={'integer1': 111, 'read': 'unchanged', 'string1': 'myvalueX'},  # В БД предыдущее значение string1.
            db_create={'integer1': 111, 'read': 'unchanged', 'string1': 'myvalue1'},  # В БД значение string1 по умолчанию.
        ),
        rec_free=WriteParams(
            write={'integer1': 211, 'read': 'override', 'string1': 'myvalueY'},
            respond={'integer1': 211, 'read': 'unchanged', 'string1': 'myvalueY'},
            db={'integer1': 211, 'read': 'unchanged', 'string1': 'myvalueY'},
            db_create=None,
        ),
    )

    # Далее проверяем правильность наследования базовых атрибутов при создании номинанта.
    record_master = create_record(resource=resource2, attrs={'integer1': 31}, num_queries=4)
    record_master.attrs['read'] = 'copyme'  # это значение скопируем в номинанта из мастера.
    Record.publish(record_master)
    response = add_record(resource, attrs={'integer1': 311, 'read': 'trytochange'}, master_uid=record_master.master_uid)
    assert response['attrs'] == {'string1': 'myvalue1', 'integer1': 311, 'read': 'copyme'}


def test_attrs_override(drf_client, init_user, init_resource):
    user = init_user()

    resource = init_resource(user=user, publish=True)
    schema = resource.schema
    schema.fields = [
        {
            "type": {"alias": "fk", "params": {"flt": {"flag": "y"}, "ref": "dev_1"}},
            "alias": "parent_id",
            "default": {"dyn": None, "const": None}
        }, {
            "type": {"alias": "str", "params": {}},
            "alias": "flag",
            "default": {"dyn": None, "const": "n"},
        }, {
            "type": {"alias": "str", "params": {}},
            "alias": "title",
            "default": {"dyn": None, "const": None},
        }, {
            "type": {"alias": "str", "params": {}},
            "alias": "unified_account",
            "choices": {"dyn": None, "const": [
                {"val": "first_acc", "titles": {"ru": "Старый счет"}},
                {"val": "sec_acc", "titles": {"ru": "Новый счет"}}
            ]},
            "default": {"dyn": None, "const": None},
        }
    ]
    schema.display = {}
    schema.save()

    record = resource.record_add(creator=user, attrs={
        'parent_id': None,
        'flag': 'nn',
        'title': 'текст',
        'unified_account': None,
    })
    Record.publish(record)

    domain_id = resource.domain.id
    reference_id = resource.reference.id

    role_reader, role_contributor = user.role_add(roles=[
        ReaderRole,
        ContributorRole(access={
            'realms': {
                'domains': [domain_id],
                'references': [reference_id],
            },
            'attrs': {f'{reference_id}': ['unified_account']}
        }),
    ], author=user)
    assert role_contributor.access == {
        'realms': {'domains': [domain_id], 'references': [reference_id]},
        'attrs': {f'{reference_id}': ['unified_account']}}

    client = drf_client(user=user)

    response = client.post('/uiapi/records/', data={
        'attrs': {
            'parent_id': None,
            'flag': 'new',
            'title': 'текстещё',
            'unified_account': 'sec_acc',
        },
        'reference': resource.reference.alias,
        'master_uid': f'{record.master_uid}',
    })
    data = response.json()
    assert data['attrs'] == {'flag': 'nn', 'parent_id': None, 'title': 'текст', 'unified_account': 'sec_acc'}

    record_new = Record.objects.get(id=data['id'])
    assert record_new.attrs == {'flag': 'nn', 'parent_id': None, 'title': 'текст', 'unified_account': 'sec_acc'}

    response = client.patch(f'/uiapi/records/{record_new.id}/', data={
        'attrs': {
            'parent_id': None,
            'flag': 'newest',
            'title': 'текстнетот',
            'unified_account': 'first_acc',
        },
    })
    data = response.json()
    assert data['attrs'] == {'parent_id': None, 'flag': 'nn', 'title': 'текст', 'unified_account': 'first_acc'}

    record_new.refresh_from_db()
    assert record_new.attrs == {'flag': 'nn', 'parent_id': None, 'title': 'текст', 'unified_account': 'first_acc'}


def test_constraints(
    drf_client,
    init_user,
    init_resource,
    init_schema,
    django_assert_num_queries,
    init_schema_attr_dict
):

    user = init_user(roles=[ReaderRole, ContributorRole])
    client = drf_client(user=user)

    constraint_attrs = {'string1', 'integer1', 'dt'}

    schema1 = init_schema(
        'sch', user=user,
        add_fields=[
            init_schema_attr_dict(
                titles='Датавремя',
                alias='dt',
                type='datetime',
                default=None,
            ),
        ],
        constraints=[{'attrs': constraint_attrs}])
    constraint_1_id = schema1.constraints[0]['ident']
    resource1 = init_resource(user=user, alias_postfix='_one', schema=schema1, publish=True)

    def add_record(resource, *, attrs, params = None):
        response = client.post('/uiapi/records/', data={
            'attrs': attrs,
            'reference': resource.reference.alias,
            **(params or {})
        }).json()
        return response

    record1 = Record.objects.get(
        id=add_record(resource1, attrs={'integer1': 1, 'string1': 'myvalue1'})['id'])
    record1_checksum = '9533cc40570b0af51b8906b42fbcee54fd2610013fe846b92f0cecadeb9729ef'

    record2 = Record.objects.get(
        id=add_record(resource1, attrs={'integer1': 2, 'string1': 'myvalue2', 'dt': '2021-04-27T00:00:00'})['id'])
    record2_checksum = '4c5ce1d18d0ff507e9deb0e79a9ec290d450b06bf6566d5f962511db4c7c43a9'

    def constrained_recheck():
        constrained_items = list(Constrained.objects.order_by('id').values_list('checksum', flat=True))
        assert constrained_items == [
            record1_checksum,
            record2_checksum,
        ]

    def checksum_rechek():
        checksum = Constrained.get_checksum(record2, constraint_attrs=constraint_attrs)
        assert checksum == record2_checksum

    constrained_recheck()
    checksum_rechek()

    Record.publish(record1)
    Record.publish(record2)

    constrained_recheck()
    record2.refresh_from_db()
    checksum_rechek()

    assert Constrained.populate(schema=resource1.schema) == 2
    constrained = list(Constrained.objects.all())
    assert len(constrained) == 2
    assert str(constrained)
    assert constrained[0].ident == constraint_1_id
    constrained_id = constrained[0].id

    # Проверка наличия конфликта
    with django_assert_num_queries(4) as _:
        response = add_record(resource1, attrs={'integer1': 1, 'string1': 'myvalue1'})
        error_msg = response['error']['msg']
        assert 'Conflict' in error_msg['detail']
        assert error_msg['record_uid'] == str(record1.record_uid)
        assert error_msg['values'] == {'integer1': 1, 'string1': 'myvalue1', 'dt': None}

    # Проверка отсутствия конфликта
    with django_assert_num_queries(8) as _:
        response = add_record(resource1, attrs={'integer1': 3, 'string1': 'myvalue3'})
        assert response['id'] > record2.id

    # Проверка возможности создания новой версии записи.
    response = add_record(
        resource1,
        attrs={'integer1': 1, 'string1': 'myvalue1'},
        params={'master_uid': record1.master_uid},
    )
    # Проверка возможности повторного сохранения записи без конфликта.
    record_new = Record.objects.get(id=response['id'])
    record_new.save()

    # Проверяем регенерацию (полный сброс + генерация).
    assert Constrained.populate(schema=resource1.schema) == 4
    constrained = list(Constrained.objects.all())
    assert len(constrained) == 4
    record1_constrained = constrained[0]
    assert record1_constrained.ident == constraint_1_id
    assert record1_constrained.id > constrained_id
    constrained_id = record1_constrained.id

    # Проверяем пересчёт суммы полей в данных об ограничениях записи.
    record1.attrs = {'integer1': 11, 'string1': 'myvalue1'}
    with django_assert_num_queries(4) as _:
        record1.save()

    record_1_checksum = record1_constrained.checksum
    record1_constrained.refresh_from_db()
    assert record1_constrained.checksum != record_1_checksum  # данные изменились и сумма с ними

    # Проверяем регенерацию (частичный сброс + генерация).
    assert Constrained.populate(schema=resource1.schema, master=resource1.schema) == 0
    constrained = list(Constrained.objects.all())
    assert len(constrained) == 4
    assert constrained[0].id == constrained_id


def test_access_check(drf_client, init_user, init_resource):

    user = init_user()

    resource_1 = init_resource(user=user, publish=True)
    resource_2 = init_resource(user=user, alias_postfix='_other', publish=True)
    resource_3 = init_resource(user=user, alias_postfix='_third', publish=True)
    record_1_1 = resource_1.record_add(creator=user, attrs={'integer1': 11})
    record_2_1 = resource_2.record_add(creator=user, attrs={'integer1': 21})
    record_3_1 = resource_3.record_add(creator=user, attrs={'integer1': 31})

    client = drf_client(user=user)

    role_contributor, role_reader = user.role_add(roles=[
        ContributorRole(access={'realms': {'references': [resource_2.reference.id]}}),
        ReaderRole(access={'realms': {'references': [resource_2.reference.id]}}),
    ], author=user)

    def check(*, status_create: int, status_edit: int):
        user.refresh_from_db()  # сброс кеша прав

        # пытаемся добавить новую запись
        response = client.post('/uiapi/records/', data={
            'attrs': {'integer1': 10},
            'reference': resource_1.reference.alias,
        })
        assert response.status_code == status_create

        # пытаемся редактировать имеющуюся
        response = client.patch(f'/uiapi/records/{record_1_1.id}/', data={
            'attrs': {'integer1': 200},
        })
        assert response.status_code == status_edit

    check(status_create=403, status_edit=403)

    role_contributor.access = {'realms': {'domains': [resource_1.domain.id]}}
    role_contributor.save()

    check(status_create=201, status_edit=200)

    # Проверка списка записей для читателя.
    # Они должны быть видны неограниченному читателю.
    role_contributor.access = {'realms': {'references': [resource_2.reference.id]}}
    role_contributor.save()
    role_reader.access = {}
    role_reader.save()

    user.refresh_from_db()  # сброс кеша прав

    response = client.get(f'/uiapi/records/?reference={resource_1.reference.id}').json()
    assert response['count'] == 2

    # Проверка доступа к записям для [сгенерированного] ограниченного читателя.
    # Для списков и детализаций.
    role_contributor.access = {'realms': {
        'domains': [resource_3.domain.id],
        'references': [resource_2.reference.id],
    }}
    role_contributor.save()
    role_reader.delete()

    user.refresh_from_db()  # сброс кеша прав

    response = client.get(f'/uiapi/records/').json()
    assert response['count'] == 2
    rec_ids = set(item['id'] for item in response['results'])
    assert rec_ids == {record_3_1.id, record_2_1.id}

    response = client.get(f'/uiapi/records/{record_3_1.id}/')
    assert response.status_code == 200

    response = client.get(f'/uiapi/records/{record_2_1.id}/')
    assert response.status_code == 200

    response = client.get(f'/uiapi/records/{record_1_1.id}/')
    assert response.status_code == 403
