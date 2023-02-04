from mdh.core.models import Audit


def test_demo(init_domain, init_schema, init_node, init_user, init_reference):

    user_test = init_user()
    user1 = init_user('user1')

    # Создаём узлы.
    node1 = init_node('node1', user=user_test)
    node2 = init_node('node2', user=user_test)

    # Регистрируем схемы.
    schema1 = init_schema('schema1', user=user_test)
    schema2 = init_schema('schema2', user=user_test)

    # Добавляем области.
    domain1 = init_domain('domain1', user=user_test)

    domain2 = init_domain('domain2', user=user_test)

    # Создаём справочники.
    reference1 = init_reference('ref1', user=user_test)
    reference2 = init_reference('ref2', user=user_test)

    reference1, reference2 = domain1.add_reference(reference1, reference1)

    # Создаём ресурсы для справочников и областей.
    resource1 = reference1.resource_add(creator=user_test, node=node1, schema=schema1)
    assert resource1.is_source

    resource2 = reference1.resource_add(creator=user_test, node=node2)
    assert resource2.is_source

    resource3 = reference1.resource_add(creator=user_test, node=node2, as_source=False)
    assert resource3.weight == 50
    assert resource3.is_destination

    resource4 = reference1.resource_add(creator=user1, node=node1, schema=schema2)

    # Добавляем записи.
    record1 = resource1.record_add(attrs={'integer1': 1, 'b': 2}, creator=user1)
    assert record1.id
    record2 = resource1.record_add(attrs={'integer1': 3, 'w': 4}, creator=user1)

    audit = list(Audit.objects.all())
