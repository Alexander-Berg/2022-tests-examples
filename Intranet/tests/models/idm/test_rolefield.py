# coding: utf-8
import random

import pytest

from idm.core.constants.rolefield import FIELD_TYPE
from idm.core.workflow.exceptions import DataValidationError
from idm.core.models import RoleField, Role, RoleNode, NetworkMacro, ConductorGroup
from idm.core.models.metrikacounter import MetrikaCounter
from idm.tests.models.test_metrikacounter import generate_counter_record
from idm.tests.utils import mock_tree, sync_role_nodes

pytestmark = pytest.mark.django_db


def test_any_node_can_have_fields(complex_system, arda_users):
    """Проверим, что поля могут быть у любого узла"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'textfield',
        'name': 'Текстовое поле',
        'required': True
    }]
    del tree['fields']

    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.count() == 4
    # добавилось новое поле
    assert RoleField.objects.filter(is_active=True).count() == 1
    # теперь попробуем запросить роль на ту ветку, где нет полей - роль должна запроситься
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'rules', 'role': 'admin'}, None)
    # а теперь на ту ветку, где есть поля - роль должна броситься datavalidation error
    with pytest.raises(DataValidationError):
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, None)
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'},
                              {'textfield': 'hello'})


def test_key_node_can_also_have_fields(complex_system, arda_users):
    """Проверим, что поля могут быть определены и у узла, являющегося ключом"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['fields'] = [{
        'slug': 'textfield',
        'name': 'Текстовое поле',
        'required': True
    }]
    del tree['fields']

    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.count() == 4
    # добавилось новое поле
    assert RoleField.objects.filter(is_active=True).count() == 1
    # теперь попробуем запросить роль на ту ветку, где нет полей - роль должна запроситься
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'rules', 'role': 'admin'}, None)
    # а теперь на ту ветку, где есть поля - роль должна броситься datavalidation error
    with pytest.raises(DataValidationError):
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, None)
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'},
                              {'textfield': 'hello'})


def test_undo(complex_system, arda_users):
    """Проверим обработку undo-полей"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['fields'] = [{
        'slug': 'textfield',
        'name': 'Текстовое поле',
        'required': True
    }]
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'textfield',
        'name': 'Отмена текстового поля',
        'required': True,  # не важно
        'type': 'undo'
    }]
    # отмена undo
    tree['roles']['values']['subs']['roles']['values']['manager']['fields'] = [{
        'slug': 'textfield',
        'name': 'Отмена отмены текстового поля',
        'required': True,
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 3
    # запросим роль на ветку с обязательным текстовым полем
    with pytest.raises(DataValidationError):
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'rules', 'role': 'admin'}, None)
    # придётся предоставить данные
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'rules', 'role': 'admin'}, {
        'textfield': 'hello'
    })
    # а вот на ветку subs, где undo, предоставлять данные не нужно:
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, None)
    # если это, конечно, не ветка manager, где поле заново добавлено
    with pytest.raises(DataValidationError):
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, None)
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'textfield': 'hello'
    })
    assert role.fields_data == {'textfield': 'hello'}


def test_integer(complex_system, arda_users):
    """Проверим обработку целых полей"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'hello',
        'name': 'Целое поле',
        'required': True,
        'type': 'integerfield'
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.INTEGER
    # предоставим невалидные данные
    with pytest.raises(DataValidationError) as validation_error:
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
            'hello': 'world'
        })
    assert validation_error.value.errors == {'hello': ['Введите целое число.']}
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'hello': 14
    })
    assert role.fields_data == {'hello': 14}


@pytest.mark.parametrize('required,blank_allowed', [(True, True), (True, False), (False, True), (False, False)])
def test_string(complex_system, arda_users, required, blank_allowed):
    """Проверим обработку обычных полей"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    fieldconf = {
        'slug': 'hello',
        'name': 'Строковое поле',
        'required': required,
        'type': 'charfield'
    }

    if blank_allowed is False:
        fieldconf['options'] = {'blank_allowed': False}

    tree['roles']['values']['subs']['fields'] = [fieldconf]
    # blank_is_null
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.CHARFIELD
    assert field.is_required is required

    role = Role.objects.request_role(
        frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {'hello': 'world'}
    )
    assert role.fields_data == {'hello': 'world'}
    if required:
        # запросим без поля вообще
        with pytest.raises(DataValidationError) as validation_error:
            Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'})

        assert validation_error.value.errors == {'hello': ['Обязательное поле.']}

        # запросим с пустым полем
        with pytest.raises(DataValidationError) as validation_error:
            Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
                'hello': ''
            })

        assert validation_error.value.errors == {'hello': ['Обязательное поле.']}
    else:
        # запросим с пустым полем: по умолчанию получим пустую строку
        # при blank_allowed не получим поля вообще
        role2 = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
            'hello': ''
        })
        if blank_allowed:
            expected = {'hello': ''}
        else:
            expected = None
        assert role2.fields_data == expected


def test_boolean(complex_system, arda_users):
    """Проверим обработку checkbox-полей"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'hello',
        'name': 'Булево поле',
        'required': False,
        'type': 'booleanfield'
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.BOOLEAN
    # предоставим невалидные данные. строка будет преобразована к bool
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'hello': 'world'
    })
    assert role.fields_data == {'hello': True}
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'hello': 'False'
    })
    assert role.fields_data == {'hello': False}


def test_macro_suggest(complex_system, arda_users):
    """Проверим обработку suggest-полей макросов"""

    NetworkMacro.objects.create(slug='_VALINOR_DEV_NETS_')

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'macro',
        'name': 'Network Macro',
        'required': True,
        'type': 'suggestfield',
        'options': {'suggest': 'macros'},
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.SUGGEST
    # предоставим невалидные данные: вариант, которого нет
    with pytest.raises(DataValidationError) as validation_error:
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
            'macro': '_SHIRE_TEST_NETS_',
        })
    assert validation_error.value.errors == {
        'macro': ['Object with id=_SHIRE_TEST_NETS_ is not a valid object from suggest "macros"']
    }
    # предоставим валидный вариант
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'macro': '_VALINOR_DEV_NETS_'
    })


def test_conductorgroup_suggest(complex_system, arda_users):
    """Проверим обработку suggest-полей кондукторных групп"""

    ConductorGroup.objects.create(external_id=666, name='Nazguls')

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'cgroup',
        'name': 'Conductor Group',
        'required': True,
        'type': 'suggestfield',
        'options': {'suggest': 'conductor_groups'},
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.SUGGEST
    # предоставим невалидные данные: вариант, которого нет
    with pytest.raises(DataValidationError) as validation_error:
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
            'cgroup': 123,
        })
    assert validation_error.value.errors == {
        'cgroup': ['Object with id=123 is not a valid object from suggest "conductor_groups"']
    }
    # предоставим валидный вариант
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'cgroup': 'Nazguls',
    })


def test_metrika_counter_suggest(complex_system, arda_users):
    """Проверим обработку suggest-полей кондукторных групп"""

    counter = MetrikaCounter.objects.create(**generate_counter_record().as_dict)

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'counter_id',
        'name': 'Номер счетчика',
        'type': 'suggestfield',
        'required': True,
        'options': {'suggest': 'metrika_counter'},
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.SUGGEST

    invalid_counter_id = str(random.randint(1, 10**8))
    # предоставим невалидные данные: вариант, которого нет
    with pytest.raises(DataValidationError) as validation_error:
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
            'counter_id': invalid_counter_id,
        })
    assert validation_error.value.errors == {
        'counter_id': [f'Object with id={invalid_counter_id} is not a valid object from suggest "metrika_counter"']
    }
    # предоставим валидный вариант
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'counter_id': counter.counter_id,
    })


def test_choice_select(complex_system, arda_users):
    """Проверим обработку полей с select-ом"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'cheese_type',
        'name': 'Тип сыра',
        'required': True,
        'type': 'choicefield',
        'options': {
            'choices': [
                {
                    'value': 'mozarella',
                    'name': {
                        'en': 'Mozarella',
                        'ru': 'Моцарелла'
                    }
                },
                {
                    'value': 'stracchino',
                    'name': {
                        'en': 'Stracchino',
                        'ru': 'Страччино'
                    }
                },
                {
                    'value': 'fontina',
                    'name': {
                        'en': 'Fontina',
                        'ru': 'Фонтина'
                    }
                },
                {
                    'value': 'gorgonzola',
                    'name': {
                        'en': 'Gorgonzola',
                        'ru': 'Горгонзола',
                    }
                }
            ]
        }
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.CHOICE
    # предоставим невалидные данные: вариант, которого нет
    with pytest.raises(DataValidationError) as validation_error:
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
            'cheese_type': 'Пошехонский'
        })
    assert validation_error.value.errors == {
        'cheese_type': ['Выберите корректный вариант. Пошехонский нет среди допустимых значений.']
    }
    # предоставим валидный вариант
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'cheese_type': 'gorgonzola'
    })


def test_choice_radio(complex_system, arda_users):
    """Проверим обработку полей с radio-button"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'cheese_type',
        'name': 'Тип сыра',
        'required': True,
        'type': 'choicefield',
        'options': {
            'widget': 'radio',
            'choices': [
                {
                    'value': 'mozarella',
                    'name': {
                        'en': 'Mozarella',
                        'ru': 'Моцарелла'
                    }
                },
                {
                    'value': 'stracchino',
                    'name': {
                        'en': 'Stracchino',
                        'ru': 'Страччино'
                    }
                },
                {
                    'value': 'fontina',
                    'name': {
                        'en': 'Fontina',
                        'ru': 'Фонтина'
                    }
                },
                {
                    'value': 'gorgonzola',
                    'name': {
                        'en': 'Gorgonzola',
                        'ru': 'Горгонзола',
                    }
                }
            ]
        }
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.CHOICE
    # предоставим невалидные данные: вариант, которого нет
    with pytest.raises(DataValidationError) as validation_error:
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
            'cheese_type': 'Пошехонский'
        })
    assert validation_error.value.errors == {
        'cheese_type': ['Выберите корректный вариант. Пошехонский нет среди допустимых значений.']
    }
    # предоставим валидный вариант
    Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'cheese_type': 'gorgonzola'
    })


def test_choice_select_with_custom(complex_system, arda_users):
    """Проверим обработку полей c select-ом и возможностью предложить свой вариант"""

    frodo = arda_users.frodo
    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'cheese_type',
        'name': 'Тип сыра',
        'required': True,
        'type': 'choicefield',
        'options': {
            'widget': 'radio',
            'custom': True,
            'choices': [
                {
                    'value': 'mozarella',
                    'name': {
                        'en': 'Mozarella',
                        'ru': 'Моцарелла'
                    }
                },
                {
                    'value': 'stracchino',
                    'name': {
                        'en': 'Stracchino',
                        'ru': 'Страччино'
                    }
                },
                {
                    'value': 'fontina',
                    'name': {
                        'en': 'Fontina',
                        'ru': 'Фонтина'
                    }
                },
                {
                    'value': 'gorgonzola',
                    'name': {
                        'en': 'Gorgonzola',
                        'ru': 'Горгонзола',
                    }
                }
            ]
        }
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)
    assert RoleField.objects.filter(is_active=True).count() == 1
    field = RoleField.objects.get(is_active=True)
    assert field.type == FIELD_TYPE.CHOICE
    # предоставим число, оно будет преобразовано в строку
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'cheese_type': 12
    })
    assert role.fields_data == {'cheese_type': '12'}
    # теперь предоставим один из choice-вариантов, тоже должно быть всё ок
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, {
        'cheese_type': 'mozarella'
    })
    assert role.fields_data == {'cheese_type': 'mozarella'}


def test_simple_dependencies(complex_system):
    """Проверим обработку простых зависимостей полей"""

    tree = complex_system.plugin.get_info()
    del tree['fields']
    tree['roles']['values']['subs']['fields'] = [{
        'slug': 'ring_type',
        'name': {
            'en': 'Ring type',
            'ru': 'Тип кольца',
        },
        'required': False,
        'type': 'choicefield',
        'options': {
            'widget': 'radio',
            'choices': [
                {
                    'value': 'elvenkings',
                    'name': {
                        'en': 'For Elven-kings',
                        'ru': 'Для королей эльфов',
                    }
                },
                {
                    'value': 'dwarflords',
                    'name': {
                        'en': 'For Dwarf Lords',
                        'ru': 'Для королей гномов',
                    }
                },
                {
                    'value': 'mortalmen',
                    'name': {
                        'en': 'For mortal men',
                        'ru': 'Для людей',
                    },
                },
                {
                    'value': 'darklord',
                    'name': {
                        'en': 'For dark lord',
                        'ru': 'Для Саурона',
                    }
                }
            ]
        }
    }, {
        'slug': 'qty',
        'name_en': 'Quantity',
        'name': 'Количество',
        'required': True,
        'type': 'integerfield',
        'depends_on': {
            'ring_type': {
                '$exists': True,
            }
        }
    }, {
        'slug': 'omnipotence',
        'name': {
            'en': 'Omnipotence required',
            'ru': 'Требуется всемогущество',
        },
        'type': 'booleanfield',
        'required': False,
        'depends_on': {
            '$or': [{
                'ring_type': 'elvenkings',
                'qty': 3
            }, {
                'ring_type': 'dwarflords',
                'qty': 7
            }, {
                'ring_type': 'mortalmen',
                'qty': 9
            }, {
                'ring_type': 'darklord',
                'qty': 1
            }]
        }
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)

    assert RoleField.objects.filter(is_active=True).count() == 3
    node = RoleNode.objects.get(slug='manager')
    assert node.slug_path == '/project/subs/role/manager/'
    assert {field.slug for field in node.get_fields()} == {'omnipotence', 'qty', 'ring_type'}
    assert {field.slug for field in node.get_fields({})} == {'ring_type'}
    assert {field.slug for field in node.get_fields({'ring_type': 'mortalmen'})} == {'ring_type', 'qty'}
    assert {field.slug for field in node.get_fields({'ring_type': 'mortalmen', 'qty': 4})} == {'ring_type', 'qty'}
    expected = {'ring_type', 'qty', 'omnipotence'}
    assert {field.slug for field in node.get_fields({'ring_type': 'mortalmen', 'qty': 9})} == expected
