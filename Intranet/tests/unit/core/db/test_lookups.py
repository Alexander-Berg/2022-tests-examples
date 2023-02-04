import pytest

from django.db.models import OuterRef

from intranet.femida.src.core.db import RowsToList
from intranet.femida.src.staff.models import Department

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_any_lookup_outerref():
    """
    Проверяет, что __any работает с OuterRef
    """
    root = f.DepartmentFactory(name='root')
    middle = f.DepartmentFactory(name='middle', ancestors=[root.id])
    leaf = f.DepartmentFactory(name='leaf', ancestors=[root.id, middle.id])

    ancestor_list = RowsToList(
        Department.objects
        .filter(id__any=OuterRef('ancestors'))
        .values('id', 'name')
    )
    result = list(
        Department.objects
        .annotate(ancestor_list=ancestor_list)
        .filter(id=leaf.id)
        .values_list('ancestor_list', flat=True)
        .first()
    )
    expected = [
        dict(id=root.id, name=root.name),
        dict(id=middle.id, name=middle.name),
    ]

    key = lambda x: x['id']
    assert sorted(result, key=key) == sorted(expected, key=key)
