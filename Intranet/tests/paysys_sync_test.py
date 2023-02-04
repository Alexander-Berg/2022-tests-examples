from mock import patch, Mock
import pytest

from staff.oebs.controllers.datasources import PaySysDatasource
from staff.oebs.models import PaySys

json_data = {
    'employeeCatg': [
        {
            'catgName': 'XXYA_JOBPRICE',
            'description': 'Сдельная оплата'
        },
        {
            'catgName': 'XXYA_FIXED_SALARY',
            'description': 'Фиксированный оклад'
        }
    ]
}


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_paysys_sync(_save_mock, build_updater):
    datasource = PaySysDatasource(PaySys.oebs_type, PaySys.method, Mock())
    datasource._data = json_data

    updater = build_updater(model=PaySys, datasource=datasource)
    updater.run_sync()

    assert PaySys.objects.count() == 2

    paysys1 = PaySys.objects.get(name='XXYA_JOBPRICE')
    assert paysys1.name == 'XXYA_JOBPRICE'
    assert paysys1.description == 'Сдельная оплата'

    paysys2 = PaySys.objects.get(name='XXYA_FIXED_SALARY')
    assert paysys2.description == 'Фиксированный оклад'


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_paysys_update(_save_mock, build_updater):
    PaySys.objects.create(
        name='1',
        description='1',
    )

    PaySys.objects.create(
        name='2',
        description=None,
    )

    json_data = {
        'employeeCatg': [
            {
                'description': None,
                'catgName': '1',
            },
        ]
    }

    datasource = PaySysDatasource(PaySys.oebs_type, PaySys.method, Mock())
    datasource._data = json_data

    updater = build_updater(model=PaySys, datasource=datasource)
    updater.run_sync()
    assert PaySys.objects.count() == 1

    paysys1 = PaySys.objects.get(name='1')
    assert paysys1.name == '1'
    assert paysys1.description is None
