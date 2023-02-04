from random import random

import pytest
from django.conf import settings

from intranet.search.core.swarm.storage import (
    TYPE_FACET,
    TYPE_GROUP_ATTR_LITERAL,
    TYPE_GROUP_ATTR_INT,
    TYPE_SEARCH_ATTR_LITERAL,
    TYPE_SEARCH_ATTR_INT,
    TYPE_FACTOR,
    TYPE_PROPERTY,
)
from intranet.search.tests.helpers.indexations_helpers import (
    get_document_serializer,
    create_document,
    get_base_expected_doc
)


pytestmark = pytest.mark.django_db(transaction=False)


def test_format_json_for_update():
    doc = create_document()
    serializer = get_document_serializer(doc, body_format='json')

    expected = get_base_expected_doc(doc, serializer.revision)
    assert expected == serializer.format_document_for_update()


def test_format_realtime():
    serializer = get_document_serializer(create_document())

    for realtime in (True, False):
        data = serializer.format_document_for_update(realtime=realtime)
        assert data['docs'][0]['options']['realtime'] == realtime


def test_append_facets():
    doc = create_document()
    name = 'some_facet_name'
    value = 'some_facet_value'
    doc.emit_facet_attr(name, value, label=name)

    serializer = get_document_serializer(doc)
    expected = get_base_expected_doc(doc, serializer.revision)
    expected['docs'][0][f's_{name}'] = [{'type': TYPE_FACET, 'value': value}]

    assert expected == serializer.format_document_for_update()


def test_append_group_attrs():
    doc = create_document()
    name_literal = 'some_group_name_literal'
    value_literal = 'some_group_value_literal'
    doc.emit_group_attr(name_literal, value_literal, label=name_literal)

    name_int = 'some_group_name_int'
    value_int = 100500
    doc.emit_group_attr(name_int, value_int, label=name_int)

    serializer = get_document_serializer(doc)
    expected = get_base_expected_doc(doc, serializer.revision)
    expected['docs'][0][name_literal] = [{'type': TYPE_GROUP_ATTR_LITERAL, 'value': value_literal}]
    expected['docs'][0][name_int] = [{'type': TYPE_GROUP_ATTR_INT, 'value': value_int}]

    assert expected == serializer.format_document_for_update()


def test_append_search_attrs():
    doc = create_document()
    name_literal = 'some_name_literal'
    value_literal = 'some_value_literal'
    doc.emit_search_attr(name_literal, value_literal)

    name_int = 'some_name_int'
    value_int = 100500
    doc.emit_search_attr(name_int, value_int)

    serializer = get_document_serializer(doc)
    expected = get_base_expected_doc(doc, serializer.revision)
    expected['docs'][0][name_literal] = [{'type': TYPE_SEARCH_ATTR_LITERAL, 'value': value_literal}]
    expected['docs'][0][name_int] = [{'type': TYPE_SEARCH_ATTR_INT, 'value': value_int}]

    assert expected == serializer.format_document_for_update()


def test_append_properties():
    doc = create_document()
    name = 'some_property'
    value = 'some_value'
    doc.emit_property_attr(name, value)

    serializer = get_document_serializer(doc)
    expected = get_base_expected_doc(doc, serializer.revision)
    expected['docs'][0][name] = [{'type': TYPE_PROPERTY, 'value': value}]

    assert expected == serializer.format_document_for_update()


def test_append_stat_factors():
    doc = create_document()
    meta_factor_name = 'some_meta_factor'
    meta_factor_value = random()
    doc.emit_factor(meta_factor_name, meta_factor_value, meta=True)

    factor_name = 'some_search_factor'
    factor_value = random()
    doc.emit_factor(factor_name, factor_value, meta=False)

    serializer = get_document_serializer(doc)
    expected = get_base_expected_doc(doc, serializer.revision)

    expected['docs'][0][f'STAT_meta_{meta_factor_name}'] = [
        {'type': TYPE_FACTOR, 'value': meta_factor_value}]
    expected['docs'][0]['STAT_{}_{}'.format(serializer.revision['search'], factor_name)] = [
        {'type': TYPE_FACTOR, 'value': factor_value}]

    assert expected == serializer.format_document_for_update()


def test_do_not_save_empty_attributes():
    doc = create_document()
    doc.emit_search_attr('some_none', None)
    doc.emit_search_attr('some_empty_string', '')
    doc.emit_search_attr('some_zero', 0)

    serializer = get_document_serializer(doc)
    expected = get_base_expected_doc(doc, serializer.revision)
    # добавляется только 0, остальные атрибуты считаем бесполезными
    expected['docs'][0]['some_zero'] = [{'type': TYPE_SEARCH_ATTR_INT, 'value': 0}]

    assert expected == serializer.format_document_for_update()


def test_rename_nested_zones():
    body = {'nested_zone': 'some_value', 'double_nested': {'internal_nested': 'value'}}
    doc = create_document(body=body)

    serializer = get_document_serializer(doc)

    expected_body = {
        'children': {
            # ко всем зонам "первого уровня" добавляется z_<search_name>
            'z_{}_double_nested'.format(serializer.revision['search']): {
                'children': {
                    # ко всем внутренним зонам по умолчанию ничего не добавляется
                    'internal_nested': {
                        'type': 'zone',
                        'value': body['double_nested']['internal_nested']
                    }
                },
                'type': 'zone',
                'value': ''
            },
            'z_{}_nested_zone'.format(serializer.revision['search']): {
                'type': 'zone',
                'value': body['nested_zone']
            }
        },
        'type': 'zone',
        'value': ''
    }
    expected = get_base_expected_doc(doc, serializer.revision, body=expected_body)

    assert expected == serializer.format_document_for_update()


def test_rename_marked_nested_zones():
    body = {'double_nested': {'!internal_nested': 'value'}}
    doc = create_document(body=body)

    serializer = get_document_serializer(doc)

    expected_body = {
        'children': {
            'z_{}_double_nested'.format(serializer.revision['search']): {
                'children': {
                    # к внутренней зоне добавляется префикс, если она была помечена
                    # восклицательным знаком
                    'z_{}_internal_nested'.format(serializer.revision['search']): {
                        'type': 'zone',
                        'value': body['double_nested']['!internal_nested']
                    }
                },
                'type': 'zone',
                'value': ''
            }
        },
        'type': 'zone',
        'value': ''
    }
    expected = get_base_expected_doc(doc, serializer.revision, body=expected_body)

    assert expected == serializer.format_document_for_update()


def test_format_list_zones():
    """ Преобразовывает вложенный плоский массив в текст
    """
    body = {'list_zone': ['first_value', 'second_value', 'n_value']}
    doc = create_document(body=body)

    serializer = get_document_serializer(doc)

    expected_body = {
        'children': {
            'z_{}_list_zone'.format(serializer.revision['search']): {
                'type': 'zone',
                'value': ' .\n'.join(body['list_zone'])
            }
        },
        'type': 'zone',
        'value': ''
    }
    expected = get_base_expected_doc(doc, serializer.revision, body=expected_body)

    assert expected == serializer.format_document_for_update()


def test_rename_factor_zones():
    """ Переименовывает зоны, по которым строятся зонные факторы
    """
    body = {'double_nested': {'internal_nested_factor': 'value'}}
    doc = create_document(body=body)
    serializer = get_document_serializer(doc)
    search = serializer.revision['search']
    settings.ISEARCH['searches']['base'][search]['factors']['zone'] = ['internal_nested_factor']

    expected_body = {
        'children': {
            f'z_{search}_double_nested': {
                'children': {
                    # к внутренней зоне добавляется префикс, если в конфиге задано,
                    # что по ней строится зонный фактор
                    f'z_{search}_internal_nested_factor': {
                        'type': 'zone',
                        'value': body['double_nested']['internal_nested_factor']
                    }
                },
                'type': 'zone',
                'value': ''
            }
        },
        'type': 'zone',
        'value': ''
    }
    expected = get_base_expected_doc(doc, serializer.revision, body=expected_body)

    assert expected == serializer.format_document_for_update()


def test_do_not_rename_full_named_zones():
    """ Если зона уже названа с z_*, то оставляет имя как есть
    """
    body = {'z_marked_zone': 'value'}
    doc = create_document(body=body)
    serializer = get_document_serializer(doc)

    expected_body = {
        'children': {
            'z_marked_zone': {
                'type': 'zone',
                'value': body['z_marked_zone']
            }
        },
        'type': 'zone',
        'value': ''
    }
    expected = get_base_expected_doc(doc, serializer.revision, body=expected_body)
    assert expected == serializer.format_document_for_update()


def test_format_json_for_delete():
    doc = create_document()
    serializer = get_document_serializer(doc)
    expected = {
        'prefix': serializer.revision['id'],
        'action': 'delete',
        'docs': [{
            'url': doc.url,
            'options': {'modification_timestamp': doc.updated_ts},
        }]
    }
    assert expected == serializer.format_document_for_delete()
