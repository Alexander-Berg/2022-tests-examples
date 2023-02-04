from balance import balance_db as db
from balance import balance_steps
from jsonrpc import dispatcher

@dispatcher.add_method
def test_disable_config():
    item = 'SAUTH_REQUIRED_REGIONS'
    query = 'update T_CONFIG set VALUE_NUM = 0, value_json = NULL where item = :item'
    params = {'item': item}
    db.balance().execute(query, params)
    return(1)


@dispatcher.add_method
def test_enable_config():
    value_json = '[149, 159, 167, 168, 169, 170, 171, 187, 207, 208, 209, 225, 29386]'
    item = 'SAUTH_REQUIRED_REGIONS'
    query = 'update T_CONFIG set VALUE_NUM = NULL, value_json = :value_json where item = :item'
    params = {'value_json': value_json, 'item': item}
    db.balance().execute(query, params)
    return(1)


@dispatcher.add_method
def test_sms_code(login):
    passport_id = balance_steps.PassportSteps.get_passport_by_login(login)['Uid']
    query = 'select CODE from T_VERIFICATION_CODE where passport_id = :passport_id and DT = (select MAX(DT) from T_VERIFICATION_CODE) and is_used = 0'
    params = {'passport_id': passport_id}
    code = db.balance().execute(query, params)
    return dict.values(code[0])
