import pytest

from staff.groups.models import Group
from staff.person.models import Staff

from staff.departments.controllers.instance_getters import InstanceGetter
from staff.departments.models import Department

UNIQUE_FIELDS = [
    (Staff.objects, ['id', 'login']),
    (Department.objects, ['id', 'url']),
    (Group.objects, ['id', 'url']),
]


@pytest.mark.parametrize('model_manager,unique_fields', UNIQUE_FIELDS)
def test_instancegetter(company, model_manager, unique_fields):
    for field in unique_fields:
        getter = InstanceGetter(model_manager, field)
        assert getter() is None
        for instance in model_manager.all():
            assert getter(getattr(instance, field)) == instance
