import pytest

from datetime import date

from intranet.femida.src.offers.choices import CONTRACT_TYPES, WORK_PLACES
from intranet.femida.src.offers.controllers import OfferCtl
from intranet.femida.src.offers.oebs.serializers import OebsFormulaOfferSerializer

from intranet.femida.tests import factories as f


@pytest.fixture
def offer_data():
    return {
        'application': f.ApplicationFactory(
            candidate__first_name='First',
            vacancy__budget_position_id=123,
        ),
        'join_at': date(2020, 12, 10),
        'salary': 100000.00,
        'signup_bonus': 100.00,
        'signup_2year_bonus': 0.00,
        'bonus': 100.00,
        'bonus_2year': 50.00,
        'bonus_type': 'welcome',
        'relocation_package': 'silver',
        'allowance': 200.00,
        'rsu_cost': 100.00,
        'position__name_ru': 'Разработчик',
        'department__name': 'Поисковый портал',
        'grade': 15,
        'work_hours_weekly': 40,
        'housing_program': True,
        'contract_type': CONTRACT_TYPES.indefinite,
        'vmi': True,
        'schemes_data__has_food_compensation': True,
        'work_place': WORK_PLACES.office,
    }


@pytest.fixture
def expected_serialized_offer_data():
    return {
        'offerPeriod': '01.2021',
        'name': 'First',
        'salary': 100000,
        'salaryCurrency': 'RUB',
        'firstSignUp': 0,
        'secondSignUp': 0,
        'relocationPackage': 'silver',
        'relocationAllowance': 200,
        'firstWelcomeBonus': 100,
        'secondWelcomeBonus': 50,
        'startOption': 100,
        'bpNumber': 123,
        'jobName': 'Разработчик',
        'organizationName': 'Поисковый портал',
        'grade': 15,
        'weeklyHours': 40,
        'housingProgram': True,
        'dms': True,
        'food': True,
        'parking': True,
    }


def test_oebs_formula_offer_serializer(offer_data, expected_serialized_offer_data):
    offer = f.create_offer(**offer_data)
    ctl = OfferCtl(offer)
    serialized_offer_data = OebsFormulaOfferSerializer(offer, context={'ctl': ctl}).data
    assert serialized_offer_data == expected_serialized_offer_data, serialized_offer_data
