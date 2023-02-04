# coding: utf-8
from apikeys.tests_by_object_model.apikeys_object_model import User, Link, Service
from apikeys.apikeys_steps_new import get_free_login_from_autotest_login_pull
import pytest

postpayment_with_prepayment_period_noban = [
    # Тарифы карт
    {
        'service_cc': 'apimaps',
        'first_tariff': 'apimaps_50k_yearprepay_2017',
        'second_tariff': 'apimaps_100000_yearprepay_contractless_032019'
    },
]


@pytest.mark.parametrize(
    'test_data', postpayment_with_prepayment_period_noban
    , ids=lambda x: x['first_tariff'] + ' ' + x['second_tariff'])
def test_change(db_connection, test_data):
    user = User(get_free_login_from_autotest_login_pull(db_connection)[0])
    user.clean_up(db_connection)
    service = Service(cc=test_data['service_cc'])
    user.get_balance_client()
    user.create_user_project()
    link = Link(user, service, db_connection)
    link.create_and_change_person(change_flag=False)
    link.change_and_activate_tariff(test_data['first_tariff'])
    link.end_contract()
    link.create_and_change_person()
    link.change_and_activate_tariff(test_data['second_tariff'])
    return 'OK'
