import pytest
from mock import Mock, patch

import json
from datetime import date, timedelta

from staff.oebs.controllers.datasources import RewardDatasource
from staff.oebs.models import Reward


json_data = {
    'rewardSchemes': [
        {
            'schemeID': 1,
            'schemeName': 'HC1',
            'description': 'С численностью',
            'startDate': '2000-01-01',
            'endDate': '2018-09-30',
            'schemesLineID': 1,
            'hcCategory': 'T',
            'food': 'Не определено',
            'dms': [],
            'dmsGroup': None,
            'equipment': None,
            'mob': None,
            'bankCards': [],
            'ai': [],
        },
        {
            'schemeID': 2,
            'schemeName': 'Professionals',
            'description': None,
            'startDate': '2000-01-01',
            'endDate': '4712-12-31',
            'schemesLineID': 2,
            'hcCategory': 'Professionals',
            'food': 'Стандартное',
            'dms': [
                'АльфаСтрахование',
                'РЕСО',
            ],
            'dmsGroup': 'Стандартный',
            'equipment': None,
            'mob': None,
            'bankCards': [
                'Не выдаем',
            ],
            'ai': None,
        },
    ],
}


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_reward_sync(_save_mock, build_updater):
    datasource = RewardDatasource(Reward.oebs_type, Reward.method, Mock())
    datasource._data = json_data
    updater = build_updater(model=Reward, datasource=datasource)
    updater.run_sync()

    assert Reward.objects.count() == 2

    reward1 = Reward.objects.get(name='HC1')
    assert reward1.start_date == date(year=2000, month=1, day=1)
    assert reward1.end_date == date(year=2018, month=9, day=30)
    assert reward1.name == 'HC1'
    assert reward1.description == 'С численностью'
    assert reward1.schemes_line_id == 1
    assert reward1.category == 'T'
    assert reward1.food == 'Не определено'
    assert json.loads(reward1.dms) == []
    assert reward1.dms_group is None
    assert reward1.equipment is None
    assert reward1.mobile is None
    assert json.loads(reward1.ai) == []
    assert json.loads(reward1.bank_cards) == []

    reward2 = Reward.objects.get(name='Professionals')
    assert reward2.start_date == date(year=2000, month=1, day=1)
    assert reward2.end_date == date(year=4712, month=12, day=31)
    assert reward2.description is None
    assert reward2.schemes_line_id == 2
    assert reward2.category == 'Professionals'
    assert reward2.food == 'Стандартное'
    assert json.loads(reward2.dms) == ['АльфаСтрахование', 'РЕСО']
    assert reward2.dms_group == 'Стандартный'
    assert reward2.equipment is None
    assert reward2.mobile is None
    assert reward2.ai is None
    assert json.loads(reward2.bank_cards) == ['Не выдаем']


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_reward_update(_save_mock, build_updater):
    Reward.objects.create(
        scheme_id=1,
        name='1',
        start_date=date.today(),
        end_date=date.today() + timedelta(days=1),
        description='1',
    )

    Reward.objects.create(
        scheme_id=2,
        name='2',
        start_date=date.today(),
        end_date=date.today() + timedelta(days=1),
        description=None,
    )

    json_data = {
        'rewardSchemes': [
            {
                'schemeID': 1,
                'description': None,
                'endDate': '4712-12-31',
                'schemeName': '1',
                'startDate': '2000-01-01',
                'schemesLineID': 1,
                'hcCategory': 'T',
                'food': 'Не определено',
                'dms': [],
                'dmsGroup': 'Some group',
                'equipment': None,
                'mob': 'Some value',
                'bankCards': [],
                'ai': None,
            },
        ],
    }

    datasource = RewardDatasource(Reward.oebs_type, Reward.method, Mock())
    datasource._data = json_data
    updater = build_updater(model=Reward, datasource=datasource)
    updater.run_sync()
    assert Reward.objects.count() == 1

    reward1 = Reward.objects.get(scheme_id=1)
    assert reward1.start_date == date(year=2000, month=1, day=1)
    assert reward1.end_date == date(year=4712, month=12, day=31)
    assert reward1.description is None
    assert reward1.schemes_line_id == 1
    assert reward1.dms_group == 'Some group'
    assert reward1.mobile == 'Some value'
