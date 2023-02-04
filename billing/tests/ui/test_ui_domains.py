from uuid import uuid4

from mdh.core.models import SupportRole, ReaderRole, STATUS_ALIASES, STATUS_PUBLISHED, GoverningRole

DOMAIN_BASIC_DATA = {
    'alias': 'new_domain',
    'titles': {},
    'hints': {},
}


def test_domains(drf_client, init_user, init_domain, django_assert_num_queries):

    user = init_user(roles=[ReaderRole])

    domain_1 = init_domain('domain1', user=user, publish=True)
    init_domain('domain2', user=user)

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3):
        response = client.get('/uiapi/domains/')

    assert response.status_code == 200

    data = response.json()
    assert data['count'] == 2

    domain_data = data['results'][0]
    assert domain_data['alias'] == domain_1.alias
    assert domain_data['creator'] == user.id
    assert domain_data['status'] == {'alias': 'published', 'id': 6, 'title': 'Published'}
    assert 'references' not in domain_data
    assert domain_data['url']

    # Список с фильтрами
    response = client.get('/uiapi/domains/?status=published').json()
    assert response['count'] == 1

    assert client.get(f'/uiapi/domains/?id={domain_1.id},9999999999').json()['count'] == 1
    assert client.get(f'/uiapi/domains/?id=9999999999').json()['count'] == 0

    # Конкретная запись
    with django_assert_num_queries(2):
        response = client.get(f'/uiapi/domains/{domain_1.id}/')

    data = response.json()
    assert data['creator'] == {'id': user.id, 'username': 'tester'}
    assert 'references' in data
    assert 'alias' in data

    # Адресация по псевдониму.
    response = client.get(f'/uiapi/domains/{domain_1.alias}/')
    assert response.status_code == 200

    # Создание области.
    def create():
        return client.post('/uiapi/domains/', data=DOMAIN_BASIC_DATA)

    response = create()
    assert response.status_code == 403

    user.role_add(roles=[
        GoverningRole(access={'realms': {'domains': [domain_1.id]}}),
    ], author=user)

    response = client.get(f'/uiapi/domains/{domain_1.id}/')
    data = response.json()
    assert data['user_actions'] == {'edit': True}

    response = create()
    data = response.json()
    assert response.status_code == 201

    domain_id = data['id']
    assert domain_id

    user.refresh_from_db()  # сброс кеша ролей

    # Редактирование созданной области.
    # В случае, если Управляющий ограничен, его ограничения
    # должны быть дополнены для доступа к созданным им объектам.
    response = client.patch(f'/uiapi/domains/{domain_id}/', data={
        **DOMAIN_BASIC_DATA,
        'titles': {
            'ru': 'test',
        },
        'alias': 'ignored',
        'status': STATUS_ALIASES[STATUS_PUBLISHED],
    })
    data = response.json()
    assert response.status_code == 200
    assert data['status']['id'] == STATUS_PUBLISHED
    assert data['alias'] != 'ignored'
    assert data['titles'] == {'ru': 'test'}


def test_access_check_dom_plus_ref(drf_client, init_user, init_domain, init_reference):
    # проверим как ведёт себя проверка ограничения, если есть ограничения
    # и по области и по справочнику.

    user = init_user()
    domain_1 = init_domain('domain1', user=user, publish=True)
    domain_2 = init_domain('domain2', user=user, publish=True)
    ref_2_1 = init_reference('ref_21', user=user)
    domain_2.add_reference(ref_2_1)

    role_governing = user.role_add(roles=[
        GoverningRole(access={'realms': {'domains': [domain_1.id], 'references': [ref_2_1.id]}})
    ], author=user)[0]

    # Проверим, что есть доступ и к области из ограничений областей
    # и к области из ограничений справочника.
    client = drf_client(user=user)
    response = client.get('/uiapi/domains/').json()
    assert response['count'] == 2

    # И доступ к деталям областей.
    for domain in [domain_1, domain_2]:
        response = client.get(f'/uiapi/domains/{domain.alias}/').json()
        assert response['id'] == domain.id

    # создаём новую область
    response = client.post('/uiapi/domains/', data=DOMAIN_BASIC_DATA)
    assert response.status_code == 201
    domain_new_id = response.data['id']
    domain_new_alias = response.data['alias']

    user.refresh_from_db()  # сброс кеша прав
    role_governing.refresh_from_db()
    assert role_governing.access_domains == [domain_1.id, domain_new_id]

    # и проверяем, что к ней есть доступ
    response = client.get(f'/uiapi/domains/{domain_new_alias}/').json()
    assert response['id'] == domain_new_id


def test_access_check(drf_client, init_user, init_domain, init_reference):

    user = init_user()

    domain_1 = init_domain('domain1', user=user, publish=True)
    init_domain('domain2', user=user, publish=True)
    client = drf_client(user=user)

    ref1 = domain_1.add_reference(init_reference('ref1', user=user))[0]
    domain_1.add_reference(init_reference('ref2', user=user))

    role_governing = user.role_add(roles=[
        GoverningRole(access={'realms': {'references': [-100]}})
    ], author=user)[0]

    def check(*, status_create: int, status_edit: int):
        user.refresh_from_db()  # сброс кеша прав

        # пытаемся добавить новую область
        domain_data = DOMAIN_BASIC_DATA.copy()
        domain_data['alias'] = f'dom{uuid4().hex}'
        response = client.post('/uiapi/domains/', data=domain_data)
        assert response.status_code == status_create

        # пытаемся редактировать имеющуюся
        response = client.patch(f'/uiapi/domains/{domain_1.id}/', data={**domain_data})
        assert response.status_code == status_edit

    check(status_create=201, status_edit=403)

    role_governing.access = {'realms': {'domains': [domain_1.id]}}
    role_governing.save()

    check(status_create=201, status_edit=200)

    role_governing.delete()

    # Проверка списков для читателя
    user.role_add(roles=[ReaderRole(access={
        'realms': {'references': [ref1.id]}
    })], author=user)
    user.refresh_from_db()  # сброс кеша прав

    response = client.get('/uiapi/domains/')
    assert response.data['count'] == 1
    assert response.data['results'][0]['id'] == domain_1.id

    response = client.get(f'/uiapi/domains/{domain_1}/').json()
    refs = response['references']
    assert len(refs) == 1
    assert refs[0]['id'] == ref1.id

    # Проверка полных списков при наличии роли поддержки
    user.role_add(roles=[SupportRole], author=user)
    user.refresh_from_db()  # сброс кеша прав

    response = client.get(f'/uiapi/domains/{domain_1}/').json()
    refs = response['references']
    assert len(refs) == 2


def test_alias_clash(drf_client, init_user):

    user = init_user(roles=[SupportRole])
    client = drf_client(user=user)

    def create():
        return client.post('/uiapi/domains/', data=DOMAIN_BASIC_DATA)

    result = create()
    assert result.status_code == 201

    result = create().json()
    assert result['error']['msg'] == {'alias': ['Domain with this Alias already exists.']}

