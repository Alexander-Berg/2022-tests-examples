# -*- coding: utf-8 -*-

import arrow

from yt.wrapper import (
    ypath_join,
)

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
)

TODAY = arrow.Arrow(2020, 7, 7, tzinfo='Europe/Moscow')
PAST = TODAY.shift(days=-1)
VERY_PAST = TODAY.shift(days=-2)
FUTURE = TODAY.shift(days=1)
VERY_FUTURE = TODAY.shift(days=2)

TAX_POLICY_PCT_1 = 1
TAX_POLICY_PCT_2 = 2


def create_interim_rows_table(yt_client, path, data, meta):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: meta,
            'schema': [
                {'name': 'UID', 'type': 'string'},
                {'name': 'act_dt', 'type': 'uint64'},
                {'name': 'invoice_id', 'type': 'uint64'},
                {'name': 'tax_policy_pct_id', 'type': 'uint64'},
                {'name': 'acted_qty', 'type': 'double'},
                {'name': 'acted_sum', 'type': 'double'},
                {'name': 'service_order_id', 'type': 'uint64'},
                {
                    'name': 'custom_header_key',
                    'type_v3': {
                        "type_name": "optional",
                        "item": {
                            "type_name": "struct",
                            "members": [
                                {
                                    "name": "group_docs",
                                    "type": "int64"
                                },
                                {
                                    "name": "commission_type",
                                    "required": False,
                                    "type": "int64",
                                },
                                {
                                    "name": "media_discount",
                                    "required": False,
                                    "type": "int64",
                                },
                            ]
                        }
                    }
                },
            ],
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'UID': uid,
                'act_dt': dt.int_timestamp,
                'invoice_id': iid,
                'tax_policy_pct_id': tppid,
                'acted_sum': float(a_sum),
                'acted_qty': float(a_sum) * 2,
                'service_order_id': ssid,
                'custom_header_key': {'group_docs': 0, 'commission_type': 7, 'media_discount': 7},
            }
            for uid, dt, iid, tppid, a_sum, ssid in data
        ]
    )


def create_daily_table(yt_client, path, data, meta):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: meta,
            'schema': [
                {'name': 'UID', 'type': 'string'},
            ],
        }
    )

    yt_client.write_table(
        path,
        [{'UID': uid} for uid, in data]
    )


def create_headers_table(yt_client, path, data, meta):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: meta,
            'schema': [
                {'name': 'act_id', 'type': 'string'},
            ],
        }
    )

    yt_client.write_table(
        path,
        [{'act_id': aid} for aid, in data]
    )


def get_result_meta(yt_client, path):
    meta_attr = yt_client.get(ypath_join(path, '@' + LOG_TARIFF_META_ATTR))
    run_id = meta_attr.pop('run_id')
    assert run_id
    return meta_attr
