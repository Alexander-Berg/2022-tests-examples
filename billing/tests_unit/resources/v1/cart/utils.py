# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal
from hamcrest import has_entries


def items_qty_to_decimal(items):
    for item in items:
        item[u'qty'] = Decimal(item[u'qty'])
    return items


def items_to_matchers(items, matcher=has_entries):
    transformed_items = []
    for item in items:
        entry = {
            u'order_id': item.order.id,
            u'qty': item.quantity,
        }
        if matcher:
            entry = matcher(entry)
        transformed_items.append(entry)
    return transformed_items
