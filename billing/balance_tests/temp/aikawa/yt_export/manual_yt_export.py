# -*- coding: utf-8 -*-

import argparse
from balance.application import Application
from balance.utils import db_to_yt, yt_helpers

QUERIES = {
    'orders': '''
        WITH a AS (
          SELECT
            parent_order_id
          FROM t_consume WHERE dt>=TO_DATE('2017-11-24 17:10:00','YYYY-MM-DD HH24:MI:SS')

          UNION

          SELECT
            parent_order_id
          FROM t_consume
          WHERE id IN (SELECT consume_id FROM t_reverse WHERE dt>=TO_DATE('2017-11-24 17:10:00','YYYY-MM-DD HH24:MI:SS'))

          UNION

          SELECT
            id
          FROM bo.t_order
          WHERE SHIPMENT_UPDATE_DT >= TO_DATE('2017-11-24 17:10:00','YYYY-MM-DD HH24:MI:SS')
        )
        SELECT *
        FROM t_order
        WHERE group_order_id IN (SELECT parent_order_id FROM a)

        UNION

        SELECT *
        FROM t_order
        WHERE id IN (SELECT parent_order_id FROM a)
    ''',
    'completion_history': '''
        WITH a AS (
          SELECT
            parent_order_id
          FROM t_consume WHERE dt>=TO_DATE('2017-11-24 17:10:00','YYYY-MM-DD HH24:MI:SS')

          UNION

          SELECT
            parent_order_id
          FROM t_consume
          WHERE id IN (SELECT consume_id FROM t_reverse WHERE dt>=TO_DATE('2017-11-24 17:10:00','YYYY-MM-DD HH24:MI:SS'))

          UNION

          SELECT
            id
          FROM bo.t_order
          WHERE SHIPMENT_UPDATE_DT >= TO_DATE('2017-11-24 17:10:00','YYYY-MM-DD HH24:MI:SS')
        ),
        orders AS (
          SELECT *
          FROM t_order
          WHERE group_order_id IN (SELECT parent_order_id FROM a)

          UNION

          SELECT *
          FROM t_order
          WHERE id IN (SELECT parent_order_id FROM a)
        )
        SELECT
          *
        FROM v_completion_history
        WHERE start_dt>=TO_DATE('2017-11-23 00:00:00','YYYY-MM-DD HH24:MI:SS')
          AND order_id IN (SELECT id FROM orders)
    ''',
    'consumes': '''
        SELECT *
        FROM t_consume
        WHERE id IN (
          SELECT consume_id FROM t_reverse r WHERE r.dt >= TO_DATE('2017-11-24 17:10:00','YYYY-MM-DD HH24:MI:SS')
        )
        UNION

        SELECT *
        FROM t_consume
        WHERE dt >= TO_DATE('2017-11-24 17:10:00','YYYY-MM-DD HH24:MI:SS')
    ''',

}


def main():
    parser = argparse.ArgumentParser("manual_yt_export")
    parser.add_argument('query', type=str, nargs='?')
    parser.add_argument('-p', '--path', type=str, default='')
    parser.add_argument('-n', '--name', type=str, default='')
    args = parser.parse_args()

    name = args.name or args.query

    Application(cfg_path='manual_yt_export.cfg.xml')

    host = 'hahn.yt.yandex.net'
    token = yt_helpers.get_token(host)
    db_to_yt.run({
        'src': QUERIES[args.query],
        'src_name': name,
        'dst': '//home/balance/dev/%s' % args.path,
        'table': 0,
        'merge_result': 1,
        'proxy_host': host,
        'database_id': 'balance',
        'secret': token
    })


if __name__ == '__main__':
    main()
