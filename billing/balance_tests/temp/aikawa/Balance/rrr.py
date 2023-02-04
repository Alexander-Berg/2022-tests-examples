import datetime

from balance import balance_steps as steps

steps.OrderSteps.transfer(
    [
        {'order_id': 28792202, 'qty_old':0.58, 'qty_new': 0, 'all_qty': 1},
        {'order_id': 27098819, 'qty_old':0.000006, 'qty_new': 0, 'all_qty': 1},
        {'order_id': 26366141, 'qty_old':0.3894, 'qty_new': 0, 'all_qty': 1},
        {'order_id': 14463528, 'qty_old':1092.969067, 'qty_new': 0, 'all_qty': 1},
        # {'order_id': 13468963, 'qty_old':4915.898812, 'qty_new': 0, 'all_qty': 1},
        {'order_id': 13468963, 'qty_old':7.95, 'qty_new': 0, 'all_qty': 1},
        {'order_id': 12854384, 'qty_old':591.789142, 'qty_new': 0, 'all_qty': 1},
        {'order_id': 12751820, 'qty_old':0.000003, 'qty_new': 0, 'all_qty': 1},
        {'order_id': 8435104, 'qty_old':    5917.002762, 'qty_new': 0, 'all_qty': 1}
], [{'order_id':30450478, 'qty_delta': 1}])


# [{'order_id', 'qty_old', 'qty_new', 'all_qty'}]/[{'order_id', 'qty_delta'}]
#
# 28792202
# 27098819
# 26366141
# 14463528
# 13468963
# 13468963
# 12854384
# 12751820
# 8435104