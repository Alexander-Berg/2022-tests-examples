from mdh.core.models import SupportRole, ReaderRole


def test_domain_references(drf_client, init_resource, init_user, django_assert_num_queries):

    user = init_user(roles=[ReaderRole])
    resource1 = init_resource(user=user, alias_postfix='1')
    res1_domain_ref_id = resource1.reference_link_id
    res1_domain_id = resource1.reference_link.domain_id

    resource2 = init_resource(user=user, alias_postfix='2')

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3) as _:
        response = client.get('/uiapi/domain_references/')

    assert response.status_code == 200
    data = response.json()
    assert data['count'] == 2

    dreference = data['results'][0]
    assert dreference['domain']['alias']
    assert dreference['domain']['status']['alias']
    assert dreference['reference']['alias']
    assert dreference['reference']['status']['alias']
    assert dreference['id'] == res1_domain_ref_id
    assert dreference['url']

    # Поиск
    response = client.get(f'/uiapi/domain_references/?search={resource1.reference_link.domain.alias}').json()
    assert response['count'] == 1

    response = client.get('/uiapi/domain_references/?search=bogus').json()
    assert response['count'] == 0

    # Список с фильтрами
    response = client.get(
        f'/uiapi/domain_references/?domain={res1_domain_id}'
    ).json()
    assert response['count'] == 1
    assert response['results'][0]['id'] == res1_domain_ref_id

    # Конкретная запись
    with django_assert_num_queries(1) as _:
        response = client.get(f'/uiapi/domain_references/{res1_domain_ref_id}/')

    data = response.json()
    assert data['domain']['alias']
    assert data['domain']['status']['alias']
    assert data['reference']['alias']
    assert data['reference']['status']['alias']
    assert 'url' in data

    # Создание связи. Крепим второй справочник к первой области.
    domain_ref_basic_data = {
        'domain': res1_domain_id,
        'reference': resource2.reference.id,
    }

    def create():
        return client.post('/uiapi/domain_references/', data=domain_ref_basic_data)

    response = create()
    data = response.json()
    assert response.status_code == 403
    assert data['error']['msg']['detail'] == 'You do not have enough permissions'

    user.role_add(roles=[SupportRole], author=user)

    response = create()
    data = response.json()
    assert response.status_code == 201

    domain_ref_id = data['id']
    assert domain_ref_id

    # Редактирование созданной связи.
    response = client.patch(f'/uiapi/domain_references/{domain_ref_id}/', data={
        **domain_ref_basic_data,
        'reference': resource1.reference.id,  # не будет изменено
    })
    data = response.json()
    assert response.status_code == 200
    assert data['reference']['alias'] == resource2.reference.alias

    # Проба отвязывания.
    response = client.delete(f'/uiapi/domain_references/{domain_ref_id}/')
    data = response.json()
    assert response.status_code == 403
    assert data['error']['msg']['detail'] == 'Delete is not allowed'
