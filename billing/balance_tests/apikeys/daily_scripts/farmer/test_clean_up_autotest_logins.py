from apikeys import apikeys_steps_new

def test_login_clean_up(db_connection):
    login_pool = [item.get('PASSPORT_ID') for item in
                  db_connection['autotest_logins'].find({'use_date': {'$exists': True}, 'ignore': False},
                                                        {'_id': 0, 'PASSPORT_ID': 1})]

    for login in login_pool:
        apikeys_steps_new.clean_up(login, db_connection, unset_usedate_from_login_pull=True)