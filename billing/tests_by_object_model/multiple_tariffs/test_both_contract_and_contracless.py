# coding: utf-8
from apikeys.tests_by_object_model.apikeys_object_model import User,Link,Service
from apikeys.apikeys_steps_new import get_free_login_from_autotest_login_pull
import pytest

postpayment_with_prepayment_period_noban = [
    #Тарифы карт
    {
        'first_service_cc': 'mapkit',
        'second_service_cc': 'apimaps',
        'first_tariff': 'mapkit_500000_yearpostpay_noban_2018',
        'second_tariff': 'apimaps_25000_yearprepay_contractless_032019'
    },
]

@pytest.mark.parametrize(
    'test_data', postpayment_with_prepayment_period_noban
    , ids=lambda x: x['first_tariff']+' '+x['second_tariff'])
def test_both(db_connection,test_data):
    user=User(get_free_login_from_autotest_login_pull(db_connection)[0])
    user.clean_up(db_connection)
    user.create_user_project()
    user.get_balance_client()

    first_service=Service(cc=test_data['first_service_cc'])

    link=Link(user, first_service, db_connection)
    link.create_and_change_person(change_flag=False)
    link.change_and_activate_tariff(test_data['first_tariff'])

    second_service=Service(cc=test_data['second_service_cc'])

    link2 = Link(user, second_service, db_connection)
    link2.create_and_change_person(change_flag=True)
    link2.change_and_activate_tariff(test_data['second_tariff'])
    return 'OK'