import json

import pytest
from mock import patch, Mock

from datetime import date, datetime, timedelta

from staff.departments.models import Geography as StaffGeography, InstanceClass
from staff.lib.testing import GeographyDepartmentFactory

from staff.oebs.controllers.datasources import GeographyDatasource
from staff.oebs.controllers.linkers import Geography2GeographyLinker
from staff.oebs.controllers.rolluppers.geography_rollupper import GeographyRollupper
from staff.oebs.models import Geography


json_data = '''
{
    "geography": [{
            "code": "BELc",
            "description": "\xd0\x91\xd0\xb5\xd0\xbb\xd0\xb0\xd1\x80\xd1\x83\xd1\x81\xd1\x8c",
            "endDate": null,
            "startDate": "2012-12-01"
        }, {
            "code": "RUSc",
            "description": "\xd0\xa0\xd0\xbe\xd1\x81\xd1\x81\xd0\xb8\xd1\x8f",
            "endDate": null,
            "startDate": "2012-12-01"
        }, {
            "code": "UKRc",
            "description": "Ukraine",
            "endDate": "2017-12-31",
            "startDate": "2012-12-01"
        }
    ]
}
'''


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_geography_sync(_save_mock, build_updater):
    datasource = GeographyDatasource(Geography.oebs_type, Geography.method, Mock())
    datasource._data = json.loads(json_data)

    updater = build_updater(model=Geography, datasource=datasource)
    updater.run_sync()
    assert Geography.objects.count() == 3

    g1 = Geography.objects.get(code='BELc')
    assert g1.start_date == date(year=2012, month=12, day=1)
    assert g1.end_date is None

    g2 = Geography.objects.get(code='RUSc')
    assert g2.start_date == date(year=2012, month=12, day=1)
    assert g2.end_date is None

    g3 = Geography.objects.get(code='UKRc')
    assert g3.start_date == date(year=2012, month=12, day=1)
    assert g3.end_date == date(year=2017, month=12, day=31)
    assert g3.description == 'Ukraine'


@pytest.mark.django_db
def test_geography_linker():
    # given
    dt = {'created_at': datetime.now(), 'modified_at': datetime.now()}
    same_code = 'test_code'
    Geography.objects.create(code=same_code, start_date=date.today())
    department_instance = GeographyDepartmentFactory()
    StaffGeography.objects.create(
        oebs_code=same_code,
        name='test name',
        name_en='test name',
        department_instance=department_instance,
        **dt,
    )

    # when
    Geography2GeographyLinker.link()

    # when
    assert Geography.objects.get(code=same_code).staff_geography == StaffGeography.objects.get(oebs_code=same_code)


@pytest.mark.django_db
def test_geography_rollup_update():
    # given
    test_code = 'test_code'
    test_description = 'test description'
    dt = {'created_at': datetime.now(), 'modified_at': datetime.now()}
    department_instance = GeographyDepartmentFactory()
    staff_geo = StaffGeography.objects.create(
        oebs_code=test_code,
        name='test name',
        name_en='test name',
        native_lang='',
        oebs_description='old description',
        department_instance=department_instance,
        **dt,
    )
    Geography.objects.create(
        code=test_code,
        start_date=date.today(),
        description=test_description,
        staff_geography=staff_geo,
    )

    # when
    GeographyRollupper.rollup()
    staff_geo = StaffGeography.objects.get(id=staff_geo.id)

    # then
    assert staff_geo.oebs_description == test_description
    assert staff_geo.native_lang == 'ru'
    assert staff_geo.department_instance.native_lang == 'ru'


@pytest.mark.django_db
def test_geography_rollup_create():
    # given
    test_code = 'test_code'
    test_description = 'test description'
    Geography.objects.create(code=test_code, start_date=date.today(), description=test_description)

    # when
    GeographyRollupper.rollup(create_absent=True)

    # then
    staff_geo = StaffGeography.objects.get(oebs_code=test_code)
    geo = Geography.objects.get(code=test_code)
    assert staff_geo.oebs_description == test_description
    assert geo.staff_geography == staff_geo
    assert staff_geo.department_instance.url == f'geo_{test_code}'
    assert staff_geo.department_instance.instance_class == InstanceClass.GEOGRAPHY.value
    assert staff_geo.native_lang == 'ru'
    assert staff_geo.department_instance.native_lang == 'ru'


@pytest.mark.django_db
def test_geography_rollup_update_intranet_status():
    # given
    test_code = 'test_code'
    test_description = 'test description'
    dt = {'created_at': datetime.now(), 'modified_at': datetime.now()}
    department_instance = GeographyDepartmentFactory(intranet_status=1)
    staff_geo = StaffGeography.objects.create(
        oebs_code=test_code,
        name='test name',
        name_en='test name',
        oebs_description=test_description,
        intranet_status=1,
        department_instance=department_instance,
        **dt,
    )
    Geography.objects.create(
        code=test_code,
        start_date=date.today(),
        end_date=date.today() - timedelta(days=1),
        description=test_description,
        staff_geography=staff_geo,
    )

    # when
    GeographyRollupper.rollup()
    actual_staff_geo = StaffGeography.objects.get(pk=staff_geo.pk)

    # then
    assert actual_staff_geo.intranet_status == 0
    assert actual_staff_geo.department_instance.intranet_status == 0


@pytest.mark.django_db
def test_geography_rollup_overwrite_intranet_status():
    # given
    test_code = 'test_code'
    test_description = 'test description'
    dt = {'created_at': datetime.now(), 'modified_at': datetime.now()}
    department_instance = GeographyDepartmentFactory(
        name='test name',
        name_en='test name',
        intranet_status=1,
        native_lang='ru',
        )
    staff_geo = StaffGeography.objects.create(
        oebs_code=test_code,
        name=department_instance.name,
        name_en=department_instance.name_en,
        oebs_description=test_description,
        intranet_status=0,
        department_instance=department_instance,
        native_lang=department_instance.native_lang,
        **dt,
    )

    # when
    GeographyRollupper.rollup()
    actual_staff_geo = StaffGeography.objects.get(pk=staff_geo.pk)

    # then
    assert actual_staff_geo.intranet_status == 0
    assert actual_staff_geo.department_instance.intranet_status == 0
