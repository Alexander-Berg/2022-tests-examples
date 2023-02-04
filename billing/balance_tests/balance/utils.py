import json
from datetime import datetime

import balance.balance_db as db


def get_config_item(item):
    config_item_lst = db.balance().execute(
        'SELECT * FROM t_config WHERE item = :item',
        {'item': item},
    )
    assert len(config_item_lst) == 1, "%s not in t_config" % item
    config_item = config_item_lst[0]
    for key in ('dt', 'num', 'str', 'json'):
        value = config_item.get('value_' + key)
        if value is not None:
            if key == 'json':
                value = json.loads(value)

            return value
