from apikeys.tests_by_object_model.apikeys_object_model import User, Link, Service
from apikeys.apikeys_steps_new import get_free_login_from_autotest_login_pull

def test_change(db_connection):
    user = User(get_free_login_from_autotest_login_pull(db_connection)[0])
    user.clean_up(db_connection)
    user.get_balance_client()
    user.create_user_project()
    # service_1 = Service(cc='routingmatrix')
    # link_1 = Link(user, service_1, db_connection)
    # link_1.create_and_change_person()
    # link_1.change_and_activate_tariff('routingmatrix_5000000_yearprepay_minus_2017')

    service_2 = Service(cc='apimaps')
    link_2=Link(user,service_2,db_connection)
    link_2.create_and_change_person()
    link_2.change_and_activate_tariff('apimaps_50k_yearprepay_2017')
    link_2.create_balance_contract('apimaps_1000_yearprepay_ban_plus_2018')

    try:
        link_2.create_balance_contract('apimaps_1000_yearprepay_ban_plus_2018')
    except Exception as e:
        assert "Contract already exists" in e.message
    link_2.get_balance_contract()

    # link_2.create_contract('apimaps_100k_yearprepay_20171')


    return 'OK'

