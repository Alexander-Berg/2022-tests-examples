from balance import balance_db as db

def enable_config():
    value_json = '[149, 159, 167, 168, 169, 170, 171, 187, 207, 208, 209, 225, 29386]'
    item = 'SAUTH_REQUIRED_REGIONS'
    query = 'update T_CONFIG set VALUE_NUM = NULL, value_json = :value_json where item = :item'
    params = {'value_json': value_json, 'item': item}
    db.balance().execute(query, params)
enable_config()
