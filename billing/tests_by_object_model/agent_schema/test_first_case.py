from apikeys.tests_by_object_model.apikeys_object_model import User, Link, Service
from apikeys.apikeys_steps import get_free_login_from_autotest_login_pull
import pytest

postpayment_agency_test_data = [
    {
        'service_cc': 'routingmatrix',
        'tariff_cc': 'routingmatrix_1000000_yearprepay_agency_minus_2019'
    },
]

postpayment_test_data = [
    # {
    #     'service_cc': 'routingmatrix',
    #     'service_cc_2': 'apimaps',
    #     'tariff_cc': 'routingmatrix_50000_yearprepay_ban_plus_2020',
    #     'tariff_cc_2': 'apimaps_200k_yearprepay_agency_noban_minus_2019'
    # },
{
        'service_cc': 'routingmatrix',
        'service_cc_2': 'routingmatrix',
        'tariff_cc': 'routingmatrix_50000_yearprepay_ban_plus_2020',
        'tariff_cc_2': 'routingmatrix_1000000_yearprepay_agency_minus_2019'
    },
]


@pytest.mark.parametrize(
    'test_data', postpayment_agency_test_data
    , ids=lambda x: x['tariff_cc'])
def test_agency_contract(db_connection, test_data):
    user = User(get_free_login_from_autotest_login_pull(db_connection)[0])
    user.clean_up(db_connection)
    service = Service(cc=test_data['service_cc'])
    user.get_balance_client()
    user.create_user_project()
    link = Link(user, service, db_connection)
    link.create_and_change_person()
    link.change_and_activate_tariff(test_data['tariff_cc'], agent_flag=True)
    link.without_key_stat_exploit()
    return 'OK'




@pytest.mark.parametrize(
    'test_data', postpayment_test_data
    , ids=lambda x: x['tariff_cc'])
def test_agency_contract_with_normal_contract(db_connection, test_data):
    user1 = User(get_free_login_from_autotest_login_pull(db_connection)[0])
    user1.clean_up(db_connection)
    user2 = User(user1.uid, user1.login)
    service = Service(cc=test_data['service_cc'])
    service2 = Service(cc=test_data['service_cc_2'])
    user1.get_balance_client()
    user2.get_balance_client(new=False)
    user1.create_user_project()
    user2.set_user_projects(number=2)
    user2.create_user_project()
    link1 = Link(user1, service, db_connection)
    link2 = Link(user2, service2, db_connection)
    link1.create_and_change_person()
    link1.change_and_activate_tariff(test_data['tariff_cc'])
    link2.create_and_change_person(change_flag=False)
    link2.change_and_activate_tariff(test_data['tariff_cc_2'], agent_flag=True)
    return 'OK'


