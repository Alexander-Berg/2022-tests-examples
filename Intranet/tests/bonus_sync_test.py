from datetime import date, timedelta

import pytest
from mock import Mock, patch

from staff.oebs.controllers.datasources.bonus_datasource import BonusDatasource
from staff.oebs.models import Bonus


json_data = {
    'bonusSchemes': [
        {
            'schemeID': 1,
            'schemeName': 'STANDARD',
            'description': 'Стандартная',
            'startDate': '2000-01-01',
            'endDate': '4712-12-31',
            'schemesLineID': 1,
            'accCategory': 'Премия стандартная',
            'valueType': 'Процент от оклада',
            'valueSource': 'Данными с подразделения',
            'value': 16.67,
        },
        {
            'schemeID': 2,
            'schemeName': 'Standard_Полугодовая_Ревью_2 окл./год',
            'description': 'Стандартная схема ревью 1 оклад в полугодие',
            'startDate': '2020-10-01',
            'endDate': '4712-12-31',
            'schemesLineID': 2,
            'accCategory': 'Премия нестандартная',
            'valueType': 'Фиксированная сумма',
            'valueSource': 'Значением',
            'value': 200000,
        },
    ],
}


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_bonus_sync(save_mock, build_updater):
    datasource = BonusDatasource(Bonus.oebs_type, Bonus.method, Mock())
    datasource._data = json_data

    updater = build_updater(model=Bonus, datasource=datasource)
    updater.run_sync()
    assert Bonus.objects.count() == 2

    bonus1 = Bonus.objects.get(name='STANDARD')
    assert bonus1.start_date == date(year=2000, month=1, day=1)
    assert bonus1.end_date == date(year=4712, month=12, day=31)
    assert bonus1.name == 'STANDARD'
    assert bonus1.description == 'Стандартная'
    assert bonus1.schemes_line_id == 1
    assert bonus1.category == 'Премия стандартная'
    assert bonus1.value_type == 'Процент от оклада'
    assert bonus1.value_source == 'Данными с подразделения'
    assert bonus1.value == 16.67

    bonus2 = Bonus.objects.get(name='Standard_Полугодовая_Ревью_2 окл./год')
    assert bonus2.start_date == date(year=2020, month=10, day=1)
    assert bonus2.end_date == date(year=4712, month=12, day=31)
    assert bonus2.description == 'Стандартная схема ревью 1 оклад в полугодие'
    assert bonus2.schemes_line_id == 2
    assert bonus2.category == 'Премия нестандартная'
    assert bonus2.value_type == 'Фиксированная сумма'
    assert bonus2.value_source == 'Значением'
    assert bonus2.value == 200000


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_bonus_update(save_mock, build_updater):
    Bonus.objects.create(
        scheme_id=1,
        name='1',
        start_date=date.today(),
        end_date=date.today() + timedelta(days=1),
        description='1',
    )

    Bonus.objects.create(
        scheme_id=2,
        name='2',
        start_date=date.today(),
        end_date=date.today() + timedelta(days=1),
        description='2',
    )

    json_data = {
        'bonusSchemes': [
            {
                'schemeID': 1,
                'description': '3',
                'endDate': '4712-12-31',
                'schemeName': '1',
                'startDate': '2000-01-01',
                'schemesLineID': 1,
                'accCategory': 'Премия стандартная',
                'valueType': 'Процент от оклада',
                'valueSource': 'Данными с подразделения',
                'value': None,
            },
        ],
    }

    datasource = BonusDatasource(Bonus.oebs_type, Bonus.method, Mock())
    datasource._data = json_data

    updater = build_updater(model=Bonus, datasource=datasource)
    updater.run_sync()
    assert Bonus.objects.count() == 1

    bonus1 = Bonus.objects.get(scheme_id=1)
    assert bonus1.start_date == date(year=2000, month=1, day=1)
    assert bonus1.end_date == date(year=4712, month=12, day=31)
    assert bonus1.description == '3'
    assert bonus1.value is None
