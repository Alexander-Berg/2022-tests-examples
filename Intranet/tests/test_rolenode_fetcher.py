from typing import Any, Union, Dict
from unittest import mock

import pytest

from idm.core.canonical import CanonicalAlias, CanonicalResponsibility, CanonicalField
from idm.core.constants.rolefield import FIELD_TYPE
from idm.core.fetchers import RoleNodeFetcher
from idm.core.models import RoleAlias
from idm.core.plugins.errors import PluginError
from tests.utils import random_slug, assert_num_queries, create_user


def generate_node(
        identifier: Union[str, int] = None,
        /,
        *,
        slug: str = None,
        name: Union[str, Dict[str, str]] = None,
        **kwargs
) -> Dict[str, Any]:
    node = dict(kwargs)
    if slug and name:
        node['slug'] = slug
        node['name'] = name
    else:
        if not identifier:
            identifier = random_slug(6)
        node['slug'] = slug or f'slug_{identifier}'
        node['name'] = name or f'Name {identifier}'
    return node


@pytest.fixture
def base_structure():
    return {
        'code': 0,
        'roles': {
            'values': {},
            'slug': 'role',
            'name': {
                'ru': 'Роль',
                'en': 'Role'
            }
        }
    }


def test_fetch__plugin_error(simple_system):
    fetcher = RoleNodeFetcher()

    with mock.patch(
            'idm.tests.base.SimplePlugin.get_info',
            side_effect=PluginError(-1, 'Test error')
    ) as get_info_mock, \
            pytest.raises(PluginError):
        fetcher.fetch(simple_system)
    get_info_mock.assert_called_once()


def test_prepare():
    user = create_user(first_name=random_slug())
    fetcher = RoleNodeFetcher()
    assert fetcher.user_cache == {}

    with assert_num_queries(1):  # загружаем в кеш только логины
        fetcher.prepare()

    assert user.username in fetcher.user_cache
    with assert_num_queries(1):
        assert user.first_name == fetcher.user_cache[user.username].first_name



@pytest.mark.parametrize(('value', 'expected'), (
    (False, False),
    ('False', False),
    ('false', False),
    ('0', False),
    (0, False),
    (True, True),
    ('True', True),
    ('true', True),
    ('1', True),
    (1, True),
))
def test_as_bool(value: Any, expected: bool):
    assert RoleNodeFetcher.as_bool(value) is expected


def test_unify_children__node():
    node = generate_node(slug=random_slug(), name='Role 1', foo='bar')
    assert list(RoleNodeFetcher.unify_children(node, direct=True)) == [node]


def test_unify_children__list_of_nodes():
    node_1 = generate_node(slug=random_slug(), name='Role 1', foo='bar')
    node_2 = generate_node(slug=random_slug(), name='Role 2', foo='baz')

    value = [node_1, node_2]
    assert list(RoleNodeFetcher.unify_children(value, direct=True)) == value


def test_unify_children__dict_of_nodes():
    node_1 = generate_node(slug=random_slug(), name='Role 1', foo='bar')
    node_2 = generate_node(slug=random_slug(), name='Role 2')
    assert list(RoleNodeFetcher.unify_children({
        node_1['slug']: node_1,  # full
        node_2['slug']: node_2['name'],  # minified
    }, direct=False)) == [node_1, node_2]


def test_normalize_tree(base_structure):
    responsible_user = create_user()
    node_data = generate_node(
        slug=random_slug(6),
        name={'ru': random_slug(), 'en': random_slug()},
        responsibilities=[{'username': responsible_user.username, 'notify': True}],
        aliases=[{'name': {'ru': random_slug(), 'en': random_slug()}}],
        help={'ru': random_slug(), 'en': random_slug()},
        is_exclusive=True,
        visibility=False,
        unique_id=random_slug(),
        set=random_slug(),
        fields=[
            {
                'type': FIELD_TYPE.INTEGER,
                'slug': random_slug(),
                'name': random_slug(),
                'required': True,
                'options': {'choices': [{'value': i} for i in range(1, 3)]},
            },
            {
                'slug': 'passport-login',
                'required': True,
            },
            {
                'slug': 'char-field',
                'name': {'en': random_slug(), 'ru': random_slug()},
                'required': True,
            },

        ]
    )

    fetcher = RoleNodeFetcher()
    fetcher.prepare()
    base_structure['roles']['values'] = {node_data['slug']: node_data}
    root_node = fetcher.normalize_tree(base_structure)
    assert root_node.slug == ''
    assert root_node.name == ''
    assert len(root_node.children) == 1

    role_node = root_node.children[0]
    assert role_node.slug == 'role'
    assert role_node.name == 'Роль'
    assert len(role_node.children) == 1

    node = role_node.children[0]
    assert node.slug == node_data['slug']
    assert node.name == node_data['name']['ru']
    assert node.name_en == node_data['name']['en']
    assert node.description == node_data['help']['ru']
    assert node.description_en == node_data['help']['en']
    assert node.is_public == node_data['visibility']
    assert node.is_exclusive == node_data['is_exclusive']
    assert node.set == node_data['set']

    assert len(node.aliases) == len(node_data['aliases'])
    for alias, alias_data in zip(node.aliases.values(), node_data['aliases']):
        assert isinstance(alias, CanonicalAlias)
        assert alias.type == RoleAlias.DEFAULT_ALIAS
        assert alias.name == alias_data['name']['ru']
        assert alias.name_en == alias_data['name']['en']

    assert len(node.responsibilities) == len(node_data['responsibilities'])
    for responsible, responsible_data in zip(node.responsibilities.values(), node_data['responsibilities']):
        assert isinstance(responsible, CanonicalResponsibility)
        assert responsible.user == responsible_user
        assert responsible.notify == responsible_data['notify']

    assert len(node.fields) == len(node_data['fields'])
    for field, field_data in zip(node.fields.values(), node_data['fields']):
        assert isinstance(field, CanonicalField)
        assert field.slug == field_data['slug']
        assert field.is_required == field_data['required']
        assert field.type == field_data.get('type', field_data['slug'].replace('-', ''))
        name = field_data.get('name')
        if name is None:
            assert field.name == field.name_en == ''
        elif isinstance(name, dict):
            assert field.name == field_data['name']['ru']
            assert field.name_en == field_data['name']['en']
        else:
            assert field.name == field.name_en == field_data['name']


def test_normalize_tree__profound_tree(base_structure):
    node_tree = {
        slug: generate_node(
            idx,
            slug=slug,
            children=[
                generate_node(
                    char,
                    children=[generate_node(slug=random_slug(), name=random_slug())],
                ) for char in ('A', 'B')
            ])
        for idx, slug in enumerate((random_slug(), random_slug(), random_slug()))
    }

    fetcher = RoleNodeFetcher()
    base_structure['roles']['values'] = node_tree
    root_node = fetcher.normalize_tree(base_structure)
    assert root_node.slug == ''
    assert root_node.name == ''
    assert len(root_node.children) == 1

    role_node = root_node.children[0]
    assert role_node.slug == 'role'
    assert role_node.name == 'Роль'
    assert len(role_node.children) == len(node_tree)

    match_queue = []
    match_queue.extend(zip(role_node.children, node_tree.values()))
    while match_queue:
        node, node_data = match_queue.pop(0)
        assert node.slug == node_data['slug']
        assert node.name == node.name_en == node_data['name']
        assert len(node.children) == len(node_data.get('children', []))
        if node.children:
            match_queue.extend(zip(node.children, node_data['children']))


def test_normalize_tree__empty_cache(base_structure):
    responsible_user = create_user()
    node_data = generate_node(
        responsibilities=[{'username': responsible_user.username, 'notify': True}],
    )

    fetcher = RoleNodeFetcher()
    base_structure['roles']['values'] = {node_data['slug']: node_data}
    root_node = fetcher.normalize_tree(base_structure)
    assert len(root_node.children) == 1

    role_node = root_node.children[0]
    assert len(role_node.children) == 1

    node = role_node.children[0]
    assert node.slug == node_data['slug']
    assert node.responsibilities == {}

    # подгрузим кеш
    fetcher.prepare()
    root_node = fetcher.normalize_tree(base_structure)
    assert len(root_node.children) == 1

    role_node = root_node.children[0]
    assert len(role_node.children) == 1

    node = role_node.children[0]
    assert node.slug == node_data['slug']

    assert len(node.responsibilities) == len(node_data['responsibilities'])
    for responsible, responsible_data in zip(node.responsibilities.values(), node_data['responsibilities']):
        assert isinstance(responsible, CanonicalResponsibility)
        assert responsible.user == responsible_user
        assert responsible.notify == responsible_data['notify']
