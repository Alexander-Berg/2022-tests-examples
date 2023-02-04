from uuid import uuid4

import pytest
from mdh.core.models import GoverningRole, ReaderRole, STATUS_NOMINATED, Schema, STATUS_ARCHIVED, STATUS_PUBLISHED
from pydantic import ValidationError


def test_schemas(drf_client, init_user, init_schema, init_reference, init_domain, django_assert_num_queries):

    user = init_user(roles=[ReaderRole])

    schema1 = init_schema('schema1', user=user)
    schema1.set_master()
    schema1.save()

    init_schema('schema2', user=user)

    ref1 = init_reference('ref1', user=user, schema=schema1)
    dom1 = init_domain('dom1', user=user)
    dom1.add_reference(ref1)

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3):
        response = client.get('/uiapi/schemas/')

        assert response.status_code == 200

        data = response.json()
        assert data['count'] == 2

        schema_data = data['results'][0]
        assert schema_data['alias'] == schema1.alias
        assert schema_data['creator'] == user.id
        assert schema_data['status'] == {'alias': 'draft', 'id': 1, 'title': 'Draft'}
        assert schema_data['url']
        assert schema_data['record_uid']
        assert schema_data['master_uid']
        assert schema_data['version'] == 1
        assert schema_data['version_master'] == 0
        assert 'attrs' not in schema_data
        assert 'constraints' in schema_data

    # Список с фильтрами
    response = client.get(
        f'/uiapi/schemas/?domain={dom1.id}&'
        f'reference={ref1.id}&status=draft'
    ).json()
    assert response['count'] == 1

    response = client.get('/uiapi/schemas/?domain=dom1&reference=ref1').json()
    assert response['count'] == 1

    response = client.get(f'/uiapi/schemas/?&search=ma2').json()  # ma2 - sche[ma2]
    assert response['count'] == 1

    # Конкретная запись
    with django_assert_num_queries(1):
        response = client.get(f'/uiapi/schemas/{schema1.id}/')

        data = response.json()
        assert data['creator'] == {'id': user.id, 'username': 'tester'}
        assert 'fields' in data
        assert 'master_uid' in data

        display = data['display']
        assert 'me' in display
        assert 'linked' in display
        assert 'constraints' in data

    # Адресация по uid.
    response = client.get(f'/uiapi/schemas/{schema1.record_uid}/')
    assert response.status_code == 200

    # Создание записи.

    schema_basic_data = {
        'alias': schema1.alias,
        'fields': schema1.fields,
        'display': schema1.display,
        'constraints': [{'attrs': ['string1']}],
        'titles': schema1.titles,
        'hints': schema1.hints,
        'master_uid': schema1.master_uid,
    }

    def create():
        schema_data = schema_basic_data.copy()
        schema_data['alias'] = f'sch{uuid4().hex}'
        return client.post('/uiapi/schemas/', data=schema_data)

    response = create()
    assert response.status_code == 403

    user.role_add(roles=[GoverningRole], author=user)
    response = client.get(f'/uiapi/schemas/{schema1.id}/')
    data = response.json()
    assert data['user_actions'] == {'edit': True}

    # меняем параметры поля, чтобы изменение считалось существенным
    schema_basic_data['fields'][0]['default']['const'] = 'myvalue2'
    response = create()
    data = response.json()
    assert response.status_code == 201
    assert data['titles'] == {}
    constraint = data['constraints'][0]
    assert constraint['attrs'] == ['string1']
    assert constraint['ident']
    assert data['status']['id'] == STATUS_NOMINATED
    schema_uid = data['master_uid']
    assert schema_uid != data['record_uid']
    assert data['version'] == 1
    assert data['version_master'] == 0
    schema_id = data['id']
    assert schema_id

    # Редактирование созданной схены.
    response = client.patch(f'/uiapi/schemas/{schema_id}/', data={
        **schema_basic_data,
        'titles': {
            'ru': 'test',
        },
        'alias': 'ignored',
    })
    data = response.json()
    assert response.status_code == 200
    assert data['master_uid'] == str(schema1.master_uid)
    assert data['status']['id'] == STATUS_NOMINATED
    assert data['alias'] != 'ignored'
    assert data['titles'] == {'ru': 'test'}
    assert not data['is_master']
    assert data['version'] == 2
    assert data['version_master'] == 0

    # Проверка существования уже рассматриваемого кандидата при создании записи.
    candidate = Schema.objects.get(id=data['id'])
    candidate.mark_on_review()
    candidate.save()

    response = client.post('/uiapi/schemas/', data={
        **schema_basic_data,
        'titles': {
            'ru': 'test',
        },
    })
    data = response.json()
    assert response.status_code == 400
    assert data['error']['type'] == 'ValidationError'

    # Проверка невозможности редактирования записей в некоторых статусах.
    schema = Schema.objects.get(id=schema_id)
    Schema.publish(schema)
    response = client.patch(f'/uiapi/schemas/{schema_id}/', data={})
    assert response.status_code == 403
    data = response.json()
    assert data['error']['type'] == 'PermissionDenied'

    # Проверка невозможности намеренного переноса в архив.
    response = client.patch(f'/uiapi/schemas/{schema_id}/', data={'status': STATUS_ARCHIVED})
    assert response.status_code == 403

    # Проверка автопубликации при несущественных изменениях.
    schema_basic_data['master_uid'] = schema_uid
    schema_basic_data['titles'] = {'en': 'some!'}
    response = create()
    data = response.json()
    assert response.status_code == 201
    assert data['titles'] == {'en': 'some!'}
    assert data['status']['id'] == STATUS_PUBLISHED

    # Проверка невозможности удаления поля.
    schema.mark_on_review()
    schema.save()

    schema_basic_data['fields'].pop()
    schema_basic_data['display']['widgets'].pop('integer1')
    response = client.patch(f'/uiapi/schemas/{schema_id}/', data={
        **schema_basic_data,
    })
    data = response.json()
    assert data['error']['msg'] == ['Field removal is not supported. Removed: integer1.']


def test_serialization(init_user, init_schema):

    user = init_user()

    with pytest.raises(ValidationError) as e:
        schema1 = init_schema('schema1', user=user, display={
            'linked': [{'ref': 'reference1', 'lnk': {'a': 'b'}}],
        })

    # Проверка вложенности полей: морда ориентируется на эту вложенность.
    assert e.value.errors()[0]['loc'] == ('display', 'linked', 0, 'dom')


def test_analyse_validated_data(init_user, init_schema, basic_schema_fields):
    user = init_user()

    schema1 = init_schema('schema1', user=user)
    schema1.publish(schema1)

    data = {
        'alias': schema1.alias,
        'fields': basic_schema_fields,
        'display': schema1.display,
        'titles': schema1.titles,
        'hints': schema1.hints,
        'master_uid': schema1.master_uid,
    }
    data_backup = data.copy()
    substantial_change = Schema.analyse_validated_data

    # Ничего не изменилось относительно оригинала.
    assert not substantial_change(data)[0]

    # Меняются описания, но это несущественно.
    data['titles'] = {'en': 'hm'}
    data['hints'] = {'en': 'hm'}
    assert not substantial_change(data)[0]

    # Меняется порядок полей, но это несущественно.
    data['fields'][0], data['fields'][1] = data['fields'][1], data['fields'][0]
    result = substantial_change(data)
    assert result == (False, [])

    # Добавляется поле, это существенно.
    data['fields'].append(data['fields'][1].copy())
    data['fields'][2]['alias'] = 'string2'
    result = substantial_change(data)
    assert result == (True, [])
    data = data_backup.copy()

    # Изменяется поле, это существенно.
    data['fields'][1]['default']['const'] = 'myvalue2'
    result = substantial_change(data)
    assert result == (True, [])
    data = data_backup.copy()

    # Удаляется поле, это существенно.
    del data['fields'][1]
    del data['display']['widgets']['string1']
    result = substantial_change(data)
    assert result == (True, ['string1'])


def test_access_check(drf_client, init_user, init_domain, init_reference):

    user = init_user()

    domain_1 = init_domain('domain1', user=user, publish=True)
    reference_1 = init_reference('ref1', user=user, auto_schema=True, publish=True)
    schema_1 = reference_1.schema

    domain_1.add_reference(reference_1)

    client = drf_client(user=user)

    role = user.role_add(roles=[GoverningRole(access={'realms': {'references': [-100]}})], author=user)[0]

    def check(*, status_create: int, status_edit: int):
        user.refresh_from_db()  # сброс кеша прав

        # пытаемся добавить новую схему
        response = client.post('/uiapi/schemas/', data={
            'alias': f'new_schema{uuid4().hex}',
            'fields': schema_1.fields,
            'display': schema_1.display,
            'titles': {},
            'hints': {},
        })
        assert response.status_code == status_create

        # пытаемся редактировать имеющуюся
        response = client.patch(f'/uiapi/schemas/{schema_1.id}/', data={
            'alias': schema_1.alias,
            'fields': schema_1.fields,
            'display': schema_1.display,
        })
        assert response.status_code == status_edit

    check(status_create=201, status_edit=403)

    role.access = {'realms': {'domains': [domain_1.id]}}
    role.save()

    check(status_create=201, status_edit=200)

