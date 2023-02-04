import pytest
import sform

from staff.proposal.forms.department import DepartmentHierarchy
from staff.lib.testing import DepartmentFactory


def test_changing_duties_field_state():
    """Проверка state поля changing_duties"""

    form = DepartmentHierarchy()
    assert form.get_field_state('changing_duties') == sform.REQUIRED

    form = DepartmentHierarchy(base_initial={'creating_department': True})
    assert form.get_field_state('changing_duties') == sform.NORMAL


@pytest.mark.django_db
def test_parent_validation():
    """Проверка что задан либо parent либо fake_parent"""

    form = DepartmentHierarchy()
    assert not form.is_valid()
    assert form.errors == {
        'errors': {
            'changing_duties': [{'code': 'required'}],
            'parent': [{'code': 'required'}],
        }
    }

    form = DepartmentHierarchy(data={'fake_parent': 'dep_87654', 'changing_duties': False})
    assert form.is_valid()

    dep_url = 'yandex_test_department'
    DepartmentFactory(url=dep_url)
    form = DepartmentHierarchy(data={'parent': dep_url, 'changing_duties': False})
    assert form.is_valid(), form.errors
