from balance import balance_db as db

def disable_config():
    item = 'SAUTH_REQUIRED_REGIONS'
    query = 'update T_CONFIG set VALUE_NUM = 0, value_json = NULL where item = :item'
    params = {'item': item}
    db.balance().execute(query, params)
disable_config()
