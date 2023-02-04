import pytest

from staff.departments.admin import GeographyForm
from staff.lib.testing import GeographyDepartmentFactory


@pytest.mark.django_db
def test_geography_form():
    department_instance = GeographyDepartmentFactory()
    target = GeographyForm(
        data={
            'name': 'name',
            'name_en': 'name_en',
            'oebs_code': 'oebs_code',
            'oebs_description': 'oebs_description',
            'st_translation_id': None,
            'intranet_status': 1,
            'department_instance': department_instance.id,
        },
    )

    assert target.is_valid(), target.errors
    assert target.save(commit=False).department_instance.id == department_instance.id
