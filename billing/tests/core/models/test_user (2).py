import pytest
from django.conf import settings

from mdh.core.exceptions import UserHandledError, LogicError
from mdh.core.models import ReaderRole, SupportRole, ContributorRole, GoverningRole, Role, User


def test_attrs(init_user):

    user = init_user()
    assert not user.is_robot
    assert not user.is_staff
    assert not user.is_support

    user = init_user(username='robot-verter')
    assert user.is_robot


def test_roles(init_user):

    user = init_user(roles=[ReaderRole, SupportRole])

    assert user.check_roles(roles=[ReaderRole, ContributorRole])
    assert not user.check_roles(roles=[GoverningRole])


def test_roles_dynamic_reader(init_user):

    user_count = 1

    def make_user(roles):
        nonlocal user_count
        user = init_user(username=f'user_{user_count}', roles=roles)
        user_count += 1
        return user.get_current_roles()

    def check_reader_for_role(role):

        # Создание динамической роли читателя.
        roles = make_user([role])
        assert len(roles) == 2

        empty = {'attrs': {}, 'realms': {'domains': [], 'references': []}}

        assert roles[role].access == empty
        assert roles[ReaderRole].access == empty

        # Использование имеющейся роли читателя без ограничений.
        roles = make_user([
            role(access={'realms': {'references': [1, 2]}}),
            ReaderRole,
        ])
        assert roles[role].access == {'realms': {'domains': [], 'references': [1, 2]}, 'attrs': {}}
        assert roles[ReaderRole].access == {'attrs': {}, 'realms': {'domains': [], 'references': []}}

        # Использование имеющейся роли читателя с ограничениями.
        roles = make_user([
            role(access={'attrs': {"3": ['a']}, 'realms': {'references': [1, 2]}}),
            ReaderRole(access={'realms': {'references': [5]}})
        ])
        assert roles[role].access == {'attrs': {'3': ['a']}, 'realms': {'domains': [], 'references': [1, 2]}}
        assert roles[ReaderRole].access == {'attrs': {}, 'realms': {'domains': [], 'references': [5, 1, 2]}}

        # Динамически подправленную роль нельзя сохранять.
        with pytest.raises(LogicError) as e:
            roles[ReaderRole].save()
        assert f'{e.value}' == 'Mutated UserRole save is not allowed.'

    check_reader_for_role(ContributorRole)
    check_reader_for_role(GoverningRole)


def test_roles_spawn(init_user, init_domain):

    role = Role.spawn('reader')
    assert role.id == ReaderRole.id

    role = Role.spawn(ContributorRole.id, index_attr='id')
    assert role.alias == ContributorRole.alias

    user = init_user(roles=['reader'])

    roles = list(user.roles.all())
    assert len(roles) == 1
    assert str(roles[0]) == f'{user.id}: reader'


def test_check_record_edit(init_user, init_resource, init_node):

    user = init_user(roles=[ReaderRole])

    node = init_node('default', user=user, id=settings.MDH_DEFAULT_NODE_ID, publish=True)
    resource = init_resource(user=user, node=node, publish=True)

    record = resource.record_add(
        creator=user,
        attrs={'integer1': 1},
    )
    # Не хватает роли.
    assert not user.check_access(record, action='partial_update')

    user.role_add(roles=[ContributorRole], author=user)
    # Всё сошлось.
    assert user.check_access(record, action='partial_update')

    # У узла нет доступа к ресурсу.
    assert not user.check_access(init_resource(user=user, alias_postfix='more').record_add(
        creator=user,
        attrs={'integer1': 1},
    ), action='update')


def test_check_access_record(init_resource, init_user):

    user = init_user()

    resource_source = init_resource(alias_postfix='src', user=user, publish=True)

    record_1 = resource_source.record_add(creator=user, attrs={'integer1': 1})

    # проверка отсутствия ограничений доступа
    assert User.check_access_record(record_1, action_read=True) is None
    # кеш. проверка и последующий сброс
    assert record_1._cache_resource_read is not None
    assert record_1._cache_resource_write is not None
    del record_1._cache_resource_read
    del record_1._cache_resource_write

    # прячем ресурс-источник, проверяем чтение с отсутствующим ресурсом-получателем
    resource_source.mark_archived()
    resource_source.save()
    assert User.check_access_record(record_1, action_read=True) is None
    assert record_1._cache_resource_read is None
    assert not hasattr(record_1, '_cache_resource_write')
    del record_1._cache_resource_read

    # проверяем невозможность доступа при попытке чтения
    assert User.check_access_record(record_1, action_read=False) is False
    assert not hasattr(record_1, '_cache_resource_read')
    assert record_1._cache_resource_write is None
    del record_1._cache_resource_write

    # добавим ресурс-получатель
    resource_read = init_resource(
        alias_postfix='dst',
        domain=resource_source.domain,
        node=resource_source.node,
        reference=resource_source.reference,
        schema=resource_source.schema,
        user=user,
        publish=True,
        as_source=False,
    )

    # ресурсы-получатели не могут быть источником записей
    with pytest.raises(UserHandledError) as e:
        resource_read.record_add(creator=user, attrs={'integer1': 1})
    assert f'{e.value}' == "Records could be added only from 'source' type resources."
