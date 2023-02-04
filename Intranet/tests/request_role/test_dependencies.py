# coding: utf-8


import pytest
from django.utils.encoding import force_text

from idm.core.workflow.exceptions import DataValidationError
from idm.core.models import Role
from idm.tests.utils import refresh

pytestmark = pytest.mark.django_db


def test_dependencies(complex_system_w_deps, arda_users):
    """Проверим запрос роли для систем с зависимыми полями"""

    frodo = arda_users.frodo
    complex_system = complex_system_w_deps

    # нельзя запросить роль, указав тип кольца, но не указав количество, так как поле qty обязательное
    with pytest.raises(DataValidationError) as excinfo:
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, {
            'ring_type': 'mortalmen'
        })

    assert force_text(excinfo.value.message) == 'Некоторые поля заполнены некорректно'
    assert excinfo.value.errors == {'qty': ['Обязательное поле.']}

    # можно запросить роль, указав тип кольца и количество, если это не ключевое количество
    role1 = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, {
        'ring_type': 'mortalmen',
        'qty': 4
    })
    role1 = refresh(role1)
    assert role1.fields_data == {'ring_type': 'mortalmen', 'qty': 4}

    # если количество ключевое, то можно указать omnipotence. omnipotence может при этом иметь любое значение:
    # как True,
    role2 = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, {
        'ring_type': 'mortalmen',
        'qty': 9,
        'omnipotence': True
    })
    role2 = refresh(role2)
    assert role2.fields_data == {'ring_type': 'mortalmen', 'qty': 9, 'omnipotence': True}

    # так и False.
    role3 = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, {
        'ring_type': 'mortalmen',
        'qty': 9,
        'omnipotence': False
    })
    role3 = refresh(role3)
    assert role3.fields_data == {'ring_type': 'mortalmen', 'qty': 9, 'omnipotence': False}

    # если указать omnipotence для неключевого значения, то оно не будет учтено
    role4 = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, {
        'ring_type': 'mortalmen',
        'qty': 600,
        'omnipotence': True,
    })
    role4 = refresh(role4)
    assert role4.fields_data == {'ring_type': 'mortalmen', 'qty': 600}
