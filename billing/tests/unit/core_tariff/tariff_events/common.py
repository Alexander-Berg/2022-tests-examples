# -*- coding: utf-8 -*-

def tariff_order(tarifficator, order):
    res = []
    for idx, event in enumerate(order.events):
        res.extend(tarifficator.process_event(idx, event))

    return res


def get_result(t, tariffed_events):
    return {
        'events': tariffed_events,
        'order': t.process_order(),
    }
