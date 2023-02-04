from datetime import date, timedelta

import pytest
from mock import Mock, patch

from staff.oebs.controllers.datasources import ReviewDatasource
from staff.oebs.models import Review


json_data = {
    'reviewSchemes': [
        {
            'schemeID': 1,
            'schemeName': 'DEFAULT',
            'description': 'Схема по умолчанию для тех, кто не участвует в ревью в принципе',
            'startDate': '2000-01-01',
            'endDate': '4712-12-31',
            'schemesLineID': 1,
            'schemesLineDesc': None,
            'targetBonus': 0,
            'grantType': 'RSU',
            'grantTypeDesc': 'RSU Yandex NV',
        },
        {
            'schemeID': 2,
            'schemeName': 'HR',
            'description': None,
            'startDate': '2000-01-01',
            'endDate': '2018-12-31',
            'schemesLineID': 2,
            'schemesLineDesc': 'Весна 2018',
            'targetBonus': 0,
            'grantType': 'RSU',
            'grantTypeDesc': 'RSU Yandex NV',
        },
    ],
}


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_review_sync(_save_mock, build_updater):
    datasource = ReviewDatasource(Review.oebs_type, Review.method, Mock())
    datasource._data = json_data
    updater = build_updater(model=Review, datasource=datasource)
    updater.run_sync()

    assert Review.objects.count() == 2

    review1 = Review.objects.get(name='DEFAULT')
    assert review1.start_date == date(year=2000, month=1, day=1)
    assert review1.end_date == date(year=4712, month=12, day=31)
    assert review1.name == 'DEFAULT'
    assert review1.description == 'Схема по умолчанию для тех, кто не участвует в ревью в принципе'
    assert review1.schemes_line_id == 1
    assert review1.schemes_line_description is None
    assert review1.target_bonus == 0
    assert review1.grant_type == 'RSU'
    assert review1.grant_type_description == 'RSU Yandex NV'

    review2 = Review.objects.get(name='HR')
    assert review2.start_date == date(year=2000, month=1, day=1)
    assert review2.end_date == date(year=2018, month=12, day=31)
    assert review2.description is None
    assert review2.schemes_line_id == 2
    assert review2.schemes_line_description == 'Весна 2018'
    assert review2.target_bonus == 0
    assert review2.grant_type == 'RSU'
    assert review2.grant_type_description == 'RSU Yandex NV'


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_review_update(_save_mock, build_updater):
    Review.objects.create(
        scheme_id=1,
        name='1',
        start_date=date.today(),
        end_date=date.today() + timedelta(days=1),
        description='1',
    )

    Review.objects.create(
        scheme_id=2,
        name='2',
        start_date=date.today(),
        end_date=date.today() + timedelta(days=1),
        description=None,
    )

    json_data = {
        'reviewSchemes': [
            {
                'schemeID': 1,
                'description': '3',
                'endDate': '4712-12-31',
                'schemeName': '1',
                'startDate': '2000-01-01',
                'schemesLineID': 1,
                'schemesLineDesc': None,
                'targetBonus': 0,
                'grantType': 'RSU_MLU',
                'grantTypeDesc': 'RSU MLU BV',
            },
        ],
    }

    datasource = ReviewDatasource(Review.oebs_type, Review.method, Mock())
    datasource._data = json_data

    updater = build_updater(model=Review, datasource=datasource)
    updater.run_sync()
    assert Review.objects.count() == 1

    review1 = Review.objects.get(scheme_id=1)
    assert review1.start_date == date(year=2000, month=1, day=1)
    assert review1.end_date == date(year=4712, month=12, day=31)
    assert review1.description == '3'
    assert review1.grant_type == 'RSU_MLU'
    assert review1.grant_type_description == 'RSU MLU BV'
