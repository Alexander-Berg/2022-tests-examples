from uuid import uuid4

from mdh.core.models import ReaderRole, GoverningRole, ContributorRole, STATUS_ALIASES, STATUS_PUBLISHED

REF_BASIC_DATA = {
    'alias': 'new_ref',
    'titles': {},
    'hints': {},
    'schema': -100,
}


def test_references(
    drf_client, init_user, init_reference, init_domain, init_schema, django_assert_num_queries,
    init_resource, init_node_default,
):

    user = init_user(roles=[ReaderRole])

    domain = init_domain('domainx', user=user)
    schema1 = init_schema('sch1', user=user)
    schema2 = init_schema('sch2', user=user)
    node = init_node_default('front', user=user)

    reference1 = init_reference('ref1', user=user, schema=schema1, publish=True)
    domain.add_reference(reference1)
    init_reference('ref2', user=user)

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3):
        response = client.get('/uiapi/references/')

    assert response.status_code == 200

    data = response.json()
    assert data['count'] == 2

    reference_data = data['results'][0]
    assert reference_data['alias'] == reference1.alias
    assert reference_data['creator'] == user.id
    assert reference_data['status'] == {'alias': 'published', 'id': 6, 'title': 'Published'}
    assert reference_data['url']

    # Список с фильтрами
    response = client.get(f'/uiapi/references/?domain={domain.id}&status=published').json()
    assert response['count'] == 1

    response = client.get(f'/uiapi/references/?domain={domain.alias}&status=published').json()
    assert response['count'] == 1

    response = client.get(f'/uiapi/references/?alias={reference1.alias},other').json()
    assert response['count'] == 1

    response = client.get(f'/uiapi/references/?schema={schema1.id}').json()
    assert response['count'] == 1
    assert response['results'][0]['schema'] == schema1.id

    assert client.get(f'/uiapi/references/?id={reference1.id},9999999999').json()['count'] == 1
    assert client.get(f'/uiapi/references/?id=9999999999').json()['count'] == 0

    # Деталировка по ид.
    with django_assert_num_queries(2):
        response = client.get(f'/uiapi/references/{reference1.id}/')

    data = response.json()
    assert data['creator'] == {'id': user.id, 'username': 'tester'}
    assert data != reference_data  # В деталировке данных больше

    # Деталировка по псевдониму.
    response = client.get(f'/uiapi/references/{reference1.alias}/')
    assert response.status_code == 200

    REF_BASIC_DATA['schema'] = schema1.id

    # Создание справочника.
    def create():
        return client.post('/uiapi/references/', data=REF_BASIC_DATA)

    response = create()
    assert response.status_code == 403

    def get_user_actions():
        response = client.get(f'/uiapi/references/{reference1.id}/')
        data = response.json()
        return data['user_actions']

    assert get_user_actions() == {'edit': False, 'edit_records': False}  # нет
    user.role_add(roles=[
        GoverningRole(access={'realms': {'references': [reference1.id]}}),
        ContributorRole,
    ], author=user)

    assert get_user_actions() == {'edit': True, 'edit_records': False}  # нет ресурса
    # добавляем ресурс
    init_resource(user=user, domain=domain, node=node, reference=reference1, schema=schema1, publish=True)
    assert get_user_actions() == {'edit': True, 'edit_records': True}  # есть ресурс

    response = create()
    data = response.json()
    assert response.status_code == 201
    assert data['schema'] == schema1.id

    ref_id = data['id']
    assert ref_id

    user.refresh_from_db()  # сброс кеша ролей

    # Редактирование созданного справочника.
    # В случае, если Управляющий ограничен, его ограничения
    # должны быть дополнены для доступа к созданным им объектам.
    response = client.patch(f'/uiapi/references/{ref_id}/', data={
        **REF_BASIC_DATA,
        'titles': {
            'ru': 'test',
        },
        'alias': 'ignored',
        'status': STATUS_ALIASES[STATUS_PUBLISHED],
        'schema': schema2.id,
    })
    data = response.json()
    assert response.status_code == 200
    assert data['status']['id'] == STATUS_PUBLISHED
    assert data['alias'] != 'ignored'
    assert data['titles'] == {'ru': 'test'}
    assert data['schema'] == schema2.id


def test_access_check(drf_client, init_user, init_domain, init_reference):

    user = init_user()

    domain_1 = init_domain('domain1', user=user, publish=True)
    domain_2 = init_domain('domain2', user=user, publish=True)
    domain_3 = init_domain('domain3', user=user, publish=True)

    reference_1_1 = init_reference('ref11', user=user, auto_schema=True, publish=True)
    reference_1_2 = init_reference('ref12', user=user, auto_schema=True, publish=True)
    reference_2_1 = init_reference('ref21', user=user, auto_schema=True, publish=True)
    reference_2_2 = init_reference('ref22', user=user, auto_schema=True, publish=True)
    reference_3_1 = init_reference('ref31', user=user, auto_schema=True, publish=True)

    domain_1.add_reference(reference_1_1, reference_1_2)
    domain_2.add_reference(reference_2_1, reference_2_2)
    domain_3.add_reference(reference_3_1)

    REF_BASIC_DATA['schema'] = reference_1_1.schema.id

    client = drf_client(user=user)

    role = user.role_add(roles=[GoverningRole(access={'realms': {'domains': [-100]}})], author=user)[0]

    def check(*, status_create: int, status_edit: int):
        user.refresh_from_db()  # сброс кеша прав

        ref_data = REF_BASIC_DATA.copy()
        ref_data['alias'] = f'ref{uuid4().hex}'
        # пытаемся добавить новый справочник
        response = client.post('/uiapi/references/', data=ref_data)
        assert response.status_code == status_create

        # пытаемся редактировать имеющийся
        response = client.patch(f'/uiapi/references/{reference_1_1.id}/', data={**ref_data})
        assert response.status_code == status_edit

    check(status_create=201, status_edit=403)

    role.access = {'realms': {'references': [reference_1_1.id]}}
    role.save()

    check(status_create=201, status_edit=200)

    # проверим доступность справочников при ограничениях.
    # в списковых выдачах и в дателизациях.
    role.access = {'realms': {'references': [reference_1_1.id], 'domains': [domain_2.id]}}
    role.save()
    user.refresh_from_db()  # сброс кеша прав

    response = client.get('/uiapi/references/').json()
    assert response['count'] == 3  # все справочники domain_2 и справочник из domain_1
    ref_ids = set(item['id'] for item in response['results'])
    assert ref_ids == {reference_1_1.id, reference_2_1.id, reference_2_2.id}

    response = client.get(f'/uiapi/references/{reference_1_1.id}/')
    assert response.status_code == 200

    response = client.get(f'/uiapi/references/{reference_2_1.id}/')
    assert response.status_code == 200

    response = client.get(f'/uiapi/references/{reference_3_1.id}/')
    assert response.status_code == 403
