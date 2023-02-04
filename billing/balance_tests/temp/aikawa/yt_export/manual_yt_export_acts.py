# -*- coding: utf-8 -*-

import argparse

from balance.application import Application
from balance.utils import db_to_yt, yt_helpers

QUERIES = {
    'ua_export': '''SELECT object_id, classname FROM T_EXPORT WHERE TYPE = 'UA_TRANSFER' AND STATE = 0''',
    'acts': '''
        SELECT * FROM t_act_internal WHERE dt >= TO_DATE('2018-06-30 00:00:00', 'YYYY-MM-DD HH24:MI:SS')
    ''',
    'invoices': '''
       SELECT * FROM t_invoice WHERE id IN (SELECT INVOICE_ID FROM T_ACT_INTERNAL WHERE dt >= TO_DATE('2018-06-30 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))
    ''',
    'consumes': '''
        SELECT * FROM t_consume WHERE INVOICE_ID IN (SELECT id FROM t_invoice WHERE id IN
        (SELECT INVOICE_ID FROM T_ACT_INTERNAL WHERE dt >= TO_DATE('2018-06-30 00:00:00', 'YYYY-MM-DD HH24:MI:SS')))
    ''',
    'consumes_ua':
        '''SELECT c.*
FROM t_consume c
  JOIN T_ORDER o ON o.id = c.PARENT_ORDER_ID
WHERE o.UPDATE_DT>= DATE '2018-12-06' ''',
        'orders': '''
        SELECT * FROM T_ORDER WHERE id IN ( SELECT PARENT_ORDER_ID FROM t_consume WHERE INVOICE_ID IN (SELECT id FROM t_invoice WHERE id IN
        (SELECT INVOICE_ID FROM T_ACT_INTERNAL WHERE dt >= TO_DATE('2018-06-30 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))))
    ''',
                  'act_trans': '''
    SELECT * FROM T_ACT_TRANS WHERE ACT_ID IN (SELECT ID FROM T_ACT_INTERNAL WHERE dt >= TO_DATE('2018-06-30 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))''',
                               'repayments': '''
            SELECT *
        FROM T_INVOICE_REPAYMENT
        WHERE INVOICE_ID IN (SELECT id
                             FROM t_invoice
                             WHERE id IN
                                   (SELECT INVOICE_ID
                                    FROM T_ACT_INTERNAL
                                    WHERE dt >= TO_DATE('2018-06-30 00:00:00', 'YYYY-MM-DD HH24:MI:SS')))
    '''
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
