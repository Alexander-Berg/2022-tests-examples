from mdh.core.models import GoverningRole, ReaderRole, SupportRole, ContributorRole, UserRole, Audit


def test_users(drf_client, init_user, django_assert_num_queries, init_domain, init_reference):

    user = init_user(roles=[GoverningRole])
    user1 = init_user('first', roles=[SupportRole, ReaderRole])
    user2 = init_user('second', is_active=False)
    user3 = init_user('ufir', roles=[ContributorRole])

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3) as _:
        response = client.get('/uiapi/users/')
        assert response.status_code == 200

        data = response.json()
        assert data['count'] == 3

        user_data = data['results'][0]
        assert user_data['username'] == user1.username
        assert user_data['is_active']
        assert 'access' not in user_data

    # Список с фильтрами
    response = client.get(f'/uiapi/users/?username={user1.username}').json()
    assert response['count'] == 1
    assert response['results'][0]['username'] == user1.username

    response = client.get('/uiapi/users/?is_active=false').json()
    assert response['count'] == 1
    assert response['results'][0]['username'] == user2.username

    response = client.get(f'/uiapi/users/?&search=fir').json()  # first + ufir
    assert response['count'] == 2

    access_empty = {'attrs': {}, 'realms': {'domains': [], 'references': []}}

    # Конкретная запись
    with django_assert_num_queries(3) as _:
        response = client.get(f'/uiapi/users/{user1.id}/')

        data = response.json()
        assert data['user_actions'] == {'edit': True}
        assert data['username'] == user1.username
        assert data['is_active']

        roles = data['roles']
        assert len(roles) == 2
        role_id = roles[0]['id']
        assert role_id
        assert roles[0]['alias'] == SupportRole.alias
        assert roles[1]['access'] == access_empty

    # проверка эффективных ролей
    data = client.get(f'/uiapi/users/{user3.id}/').json()
    assert len(data['roles']) == 1
    assert len(data['roles_effective']) == 2  # управляющий + динамический читатель

    # Создание пользователя.
    response = client.post('/uiapi/users/', data={'username': 'new'})
    assert response.status_code == 405  # создание не поддерживается

    # Редактирование пользователя.
    def edit(*, uid=None):
        uid = uid or user1.id
        response = client.patch(f'/uiapi/users/{uid}/', data={
            'username': 'one',
            'roles': [
                {
                    'id': role_id,
                    'access': {
                        'realms': {
                            'domains': [domain1_id, domain1_id],  # сразу проверим фильтрации уникальных
                            'references': [ref1_id],
                        },
                        'attrs': {
                            ref2_id: ['attr1', 'attr2', 'attr2'],
                        },
                    }
                },
                # Роль с неизвестным ид будет проигнорирована.
                {'id': None, 'access': {'attrs': {'-100': ['a']}}},
            ]
        })
        return response

    # Неизвестные сущности.
    domain1_id = -100
    ref1_id = -200
    ref2_id = -300

    response = edit(uid=user.id)
    assert response.status_code == 404  # Себя не редактируем.

    response = edit()
    assert response.status_code == 400
    assert response.json()['error']['msg'] == {
        'roles': [
            'Unable to locate Domain with the following ID: -100.',
            'Unable to locate Reference with the following ID: -300, -200.'
        ]}

    domain1 = init_domain('domain1', user=user, publish=True)
    domain1_id = domain1.id
    ref1 = init_reference('ref1', user=user, publish=True)
    ref1_id = ref1.id
    ref2 = init_reference('ref2', user=user, publish=True)
    ref2_id = ref2.id

    data = client.get(f'/uiapi/users/{user1.id}/').json()
    assert data['user_actions'] == {'edit': True}

    # Удачное изменение.
    response = edit()
    data = response.json()
    assert response.status_code == 200
    assert data['username'] == user1.username  # не изменить
    assert len(data['roles']) == 2
    assert data['roles'][0]['access'] == {
        'realms': {'domains': [domain1_id], 'references': [ref1_id]},
        'attrs': {f'{ref2_id}': ['attr1', 'attr2']}
    }
    for user_role in data['roles']:
        # Показываем только активные роли.
        assert user_role['active']

    # Проверим, что заполнились данные для аудита.
    audit_entry = Audit.objects.order_by('-id').first()
    assert audit_entry.changes['access'] == [
        access_empty,
        {'realms': {'domains': [1], 'references': [1]}, 'attrs': {'2': ['attr1', 'attr2']}}
    ]

    # Проверяем, что не выводит деактивированные роли.
    UserRole.objects.filter(id=role_id).update(active=False)
    data = client.get(f'/uiapi/users/{user1.id}/').json()
    assert len(data['roles']) == 1
    assert len(data['roles_effective']) == 1

    # Проверим, что при выставленных ограничениях,
    # проверка прав доступа к объекту самого пользователя
    # осуществляется без ошибок.
    user4 = init_user('restricted', roles=[GoverningRole])
    role = user4.roles.first()
    role.access = {'realms': {'domains': [-100]}}
    role.save()

    client_user4 = drf_client(user=user4)
    # Себя не показываем.
    assert client_user4.get(f'/uiapi/users/{user4.id}/').status_code == 404

    # проверим, что пользователь без роли не получит доступ
    client_user3 = drf_client(user=user3)
    response = client_user3.get('/uiapi/users/').json()
    assert response['count'] == 0
    response = client_user3.get(f'/uiapi/users/{user1.id}/').json()
    assert response['error']['type'] == 'PermissionDenied'

    # проверим праивльность слияния ограничений по атрибутам из разных ролей
    user3.roles.all().delete()

    user3_role_contrib, user3_role_reader = user3.role_add(roles=[
        ContributorRole(access={'attrs': {f'{ref2_id}': ['z', 'x']}}),
        ReaderRole(access={'attrs': {f'{ref1_id}': ['a', 'b']}}),
    ], author=user)

    response = client_user4.get(f'/uiapi/users/{user3.id}/').json()

    assert response['roles_effective'][1] == {
        'id': user3_role_reader.id, 'alias': 'reader', 'title': 'Reader', 'access': {
            'realms': {'domains': [], 'references': []},
            'attrs': {f'{ref1_id}': ['a', 'b'], f'{ref2_id}': ['z', 'x']}
        }, 'active': True}


def test_access(
    drf_client,
    init_user,
    init_domain,
    init_resource,
    init_node_default,
    init_reference,
):

    user = init_user()
    client = drf_client(user=user)

    node = init_node_default(user=user)

    domain1 = init_domain('dom1', user=user, publish=True)
    domain2 = init_domain('dom2', user=user, publish=True)

    ref1 = init_reference('ref1', user=user)
    ref2 = init_reference('ref2', user=user)
    ref3 = init_reference('ref3', user=user)

    resource1 = init_resource(user=user, node=node, domain=domain1, reference=ref1, publish=True, alias_postfix='1')
    resource2 = init_resource(user=user, node=node, domain=domain2, reference=ref2, publish=True, alias_postfix='2')
    resource3 = init_resource(user=user, node=node, domain=domain1, reference=ref3, publish=True, alias_postfix='3')

    record1 = resource1.record_add(creator=user, attrs={'integer1': 1})
    record11 = resource1.record_add(creator=user, attrs={'integer1': 11})
    record2 = resource2.record_add(creator=user, attrs={'integer1': 2})
    record3 = resource3.record_add(creator=user, attrs={'integer1': 21})

    def get_response_edit(url: str):
        return client.patch(url, data={'attrs': {'integer1': 0}}).json()

    def role_restrict(role, realms):
        role.access = {'realms': realms}
        role.save()
        user.refresh_from_db()  # сбросим кеш ролей

    get_response = client.get_response
    get_response_listing = client.get_response_listing
    assert_listing = client.assert_listing
    assert_error = client.assert_error
    assert_details = client.assert_details

    def test_access_records():

        url_listing = '/uiapi/records/'
        url_detailed_1 = f'/uiapi/records/{record1.id}/'
        url_detailed_3 = f'/uiapi/records/{record3.id}/'

        # Ролей нет. Список пуст.
        assert_listing(
            get_response_listing(url_listing, queries_num=2),
            []
        )
        # В деталировке запись не видна - нет прав.
        assert_error(get_response(url_detailed_1, queries_num=2))
        # Редактирование недоступно.
        assert_error(get_response_edit(url_detailed_1))

        #########################################################

        # Теперь неограниченный читатель.
        role_reader = user.role_add(roles=[ReaderRole], author=user)[0]

        # Список без ограничений.
        assert_listing(
            get_response_listing(url_listing, queries_num=4),
            [record1.id, record11.id, record2.id, record3.id]
        )
        # Детали доступны.
        assert_details(get_response(url_detailed_1, queries_num=3), record1.id)
        # Редактирование читателю недоступно.
        assert_error(get_response_edit(url_detailed_1))

        #########################################################

        # Добавляем ограничения по области.
        role_restrict(role_reader, {'domains': [domain1.id]})
        assert_listing(
            get_response_listing(url_listing, queries_num=4),
            [record1.id, record11.id, record3.id]
        )
        # В деталировке запись видна - есть права.
        assert_details(get_response(url_detailed_1, queries_num=4), record1.id)
        # Редактирование читателю недоступно.
        assert_error(get_response_edit(url_detailed_1))

        # Добавляем ограничения по справочнику.
        role_restrict(role_reader, {'domains': [], 'references': [ref3.id]})
        assert_listing(
            get_response_listing(url_listing, queries_num=4),
            [record3.id]
        )
        # В деталировке запись более недоступна.
        assert_error(get_response(url_detailed_1, queries_num=2))

        #########################################################

        # Добавляем роль редактора.
        role_contributor = user.role_add(roles=[ContributorRole], author=user)[0]

        # В деталировке запись всё также недоступна.
        assert_error(get_response(url_detailed_1, queries_num=3))
        # Неограниченное редактирование разрешено.
        assert_details(get_response_edit(url_detailed_1), record1.id)
        assert_details(get_response_edit(url_detailed_3), record3.id)

        # Добавляем ограничения по справочнику.
        role_restrict(role_contributor, {'domains': [], 'references': [ref3.id]})

        # Попытка редактирования запрещённого.
        assert_error(get_response_edit(url_detailed_1))

        # Редактирование резрешенного.
        assert_details(get_response_edit(url_detailed_3), record3.id)

    test_access_records()
