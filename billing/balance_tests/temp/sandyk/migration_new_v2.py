# encoding: utf8
import logging
import sys
import traceback

import cx_Oracle

# 'pythonh/welcome@127.0.0.1/orcl'
from btestlib import secrets

SIMPLE_SERVICES = (
    115, 116, 117, 118, 119, 120, 121, 122, 124, 125,
    126, 127, 130, 131
) + (23,)
# 115 (store) if no orders

FIX_SERVICES = [23]

IN_OPERATOR_PAGE = 1000
EXECUTE_MANY_PAGE = 1024 * 4
FETCH_PAGE = 1024 * 4
DEBUG = False


def log(mes):
    logging.debug(unicode(mes))


def sqlarray(iterable):
    iterable = tuple(iterable)
    res = None
    if len(iterable) != 1:
        res = str(tuple(iterable))
    else:
        res = "(%s)" % iterable[0]
    return res


def paginate(objects, batch_size):
    for i in range(0, len(objects), batch_size):
        yield objects[i: i + batch_size]


def prepare(conn):
    cur = conn.cursor()

    cur.execute(
        '''
            create global temporary table tmp_links
            (service_id integer, client_id integer, passport_id integer)
        '''
    )
    cur.execute(
        '''
            create global temporary table tmp_props
            (object_id integer, value_num integer, passport_id integer)
        '''
    )


def clean(conn):
    cur = conn.cursor()
    try:
        cur.execute('drop table tmp_migrate_simple')
    except cx_Oracle.DatabaseError:
        pass
    try:
        cur.execute('drop table tmp_links')
    except cx_Oracle.DatabaseError:
        pass
    try:
        cur.execute('drop table tmp_props')
    except cx_Oracle.DatabaseError:
        pass


def list_complex_clients(conn, ids):
    print ids
    cur = conn.cursor()
    log('list complex clients')
    sql = '''
        -- complex simple clients (> 1 services)
        select distinct client_id from (
            select ps.client_id, ps.passport_id
            from bo.t_passport ps where ps.client_id in
                (
                select c.id
                from bo.t_client c
                join bo.t_order o on o.client_id = c.id
                %(id_filter)s
                group by c.id
                having count(distinct o.service_id) > 1
                )
        ) where client_id in (select client_id from bo.t_order where service_id=%(service_id)s)
    ''' % {
        'id_filter': ('where c.id in %s' % sqlarray(ids)) if ids is not None else '',
        'service_id': '23'
    }
    log(sql)
    nonagency_clients = cur.execute(sql).fetchall()
    nonagency_clients = set(c[0] for c in nonagency_clients)
    log('%s nonagency_clients' % len(nonagency_clients))

    sql = '''
            select id from t_client c
            where id in (select client_id from bo.t_order where service_id=%(service_id)s)
            and is_agency=1
            %(id_filter)s
        ''' % {
        'id_filter': ('and c.id in %s' % sqlarray(ids)) if ids is not None else '',
        'service_id': '23'
    }

    log(sql)
    agency_clients = cur.execute(
        sql
    ).fetchall()
    agency_clients = set(i[0] for i in agency_clients)
    log('%s agency_clients' % len(agency_clients))
    clients = agency_clients | nonagency_clients
    log('%s clients' % len(clients))
    log(' '.join(map(str, clients)))
    return clients


def list_simple_clients(conn, ids):
    cur = conn.cursor()
    log('waiting for eternity to end')
    sql = '''
        -- simple simple clients (<= 1 services)
        select ps.client_id, ps.passport_id, %(service_id)s service_id
        from bo.t_passport ps where ps.client_id in
            (
            select c.id
            from bo.t_client c
            join bo.t_order o on o.client_id = c.id
            %(id_filter)s
            group by c.id
            having count(distinct o.service_id) = 1 and max(o.service_id) = %(service_id)s
            )
    ''' % {'id_filter': ('where c.id in %s' % sqlarray(ids)) if ids is not None else '',
           'service_id': '23'}
    log(sql)
    clients = cur.execute(
        sql
    ).fetchall()
    clients = [c[0] for c in clients]
    log(' '.join(map(str, clients)))


def simple_simple(conn, ids):
    cur = conn.cursor()
    cur2 = conn.cursor()
    log("simple simple start")

    invalid_clients = cur2.execute(
        '''
            select sc.client_id, x.client_id from bo.t_service_client sc
            join (
            select ps.client_id, ps.passport_id, 23 service_id
                      from bo.t_passport ps where ps.client_id in
                      (
                          select c.id
                          from bo.t_client c
                          join bo.t_order o on o.client_id = c.id
                          group by c.id
                        having count(distinct o.service_id) = 1 and max(o.service_id) = 23
                      )
            ) x on sc.service_id = x.service_id and sc.passport_id = x.passport_id
            and sc.client_id <> x.client_id
        '''
    ).fetchall()
    invalid_clients = set(i[0] for i in invalid_clients) | set(i[1] for i in invalid_clients)
    log('invalid_clients: %s' % invalid_clients)

    agency_clients = cur2.execute(
        '''
            select id from bo.t_client where is_agency=1 and id in
            (select client_id from t_order where service_id=23)
        '''
    )
    agency_clients = set(i[0] for i in agency_clients)
    log('agency clients %s: %s' % (len(agency_clients), agency_clients))
    excluded_clients = invalid_clients | agency_clients
    log('%s excluded clients: %s' % (len(excluded_clients), excluded_clients))

    cur.execute(
        '''
        -- simple simple clients (<= 1 services)
        create global temporary table tmp_migrate_simple
        on commit preserve rows
        AS
        select ps.client_id, ps.passport_id, %(service_id)s service_id
        from bo.t_passport ps where ps.client_id in
            (
            select c.id
            from bo.t_client c
            join bo.t_order o on o.client_id = c.id
            %(id_filter)s
            group by c.id
            having count(distinct o.service_id) = 1 and max(o.service_id) = %(service_id)s
            )
        ''' % {'id_filter': ('where c.id in %s' % sqlarray(ids)) if ids is not None else '',
               'service_id': '23'}
    )
    cur.execute(
        'select * from tmp_migrate_simple'
    )

    fetched = cur.fetchmany(FETCH_PAGE)
    log('fetched %s' % fetched)
    log('got first chunk')
    while fetched:
        log("prefetch")
        try:
            _simple_simple(cur2, fetched, excluded_clients)
            if not DEBUG:
                log('commit')
                conn.commit()
            # TODO: commit
        except:
            logging.error('BATCH FAILED, %s %s' % (traceback.format_exc(), fetched))
            conn.rollback()
        fetched = cur.fetchmany(FETCH_PAGE)
    log("simple simple end")


def complex_simple(conn, ids):
    if ids is None:
        raise Exception('NO IDS PASSED')
    log('getting multipassport clients')
    multipassport_clients = get_complex_clients_with_multiple_passports(conn.cursor(), ids)
    assert not multipassport_clients or list(multipassport_clients)[0]
    # multipassport_clients = set(i[0] for i in multipassport_clients)
    log('multipassport clients: %s' % multipassport_clients)
    log('complex_simple begin')
    cur = conn.cursor()
    cur.execute(
        '''
            create global temporary table tmp_migrate_simple
            on commit preserve rows
            AS
            select c.id client_id, p.passport_id from bo.t_client c
            join bo.t_passport p on c.id=p.client_id
            where c.id in %s
        ''' % sqlarray(ids)
    )
    clients = cur.execute('select distinct client_id from tmp_migrate_simple').fetchall()
    log("%d clients found, %s" % (len(clients), None and clients))
    clients = cur.execute('select client_id, passport_id from tmp_migrate_simple').fetchall()
    filtered_clients = [(obj[0], obj[1]) for obj in clients]
    if ids is not None:
        filtered_clients = [obj for obj in filtered_clients if obj[0] in ids]

    filtered_clients = cur.execute(
        '''
            select tms.client_id, tms.passport_id from tmp_migrate_simple tms
        '''
    ).fetchall()
    log("%d clients " % len(filtered_clients))
    filtered_client_ids = set(int(i[0]) for i in filtered_clients)
    if ids is not None and len(filtered_client_ids) != len(ids):
        log('bad client ids in the list, %d passed, %d found' % (len(ids), len(filtered_client_ids)))
        log('not found clients %s' % (set(ids) - filtered_client_ids))
        raise Exception('not found clients')
    log("%d clients after filtering, %s" % (len(filtered_clients), filtered_clients))

    # print len(clients), clients

    for client in filtered_client_ids:
        try:
            if client in multipassport_clients and not _is_client_safe(cur, client):
                raise RuntimeError('oh know, cannot process this client')
            _complex_simple(cur, client)
            if not DEBUG:
                conn.commit()
        except:
            logging.error('BATCH FAILED, %s %s' % (traceback.format_exc(), client))
            conn.rollback()
    log('complex_simple end')


def _insert_links(cur, insert_links):
    log(insert_links)
    for batch in paginate(insert_links, EXECUTE_MANY_PAGE):
        cur.executemany(
            '''
            insert into tmp_links (service_id, client_id, passport_id) values (:service_id, :client_id, :passport_id)
            ''',
            batch
        )
        log('batch into tmp table inserted')
        cur.execute(
            '''
                merge /*+ PARALLEL */ into bo.t_service_client sc
                using tmp_links links
                on (sc.passport_id=links.passport_id and sc.service_id=links.service_id)
                when not matched then
                insert (service_id, client_id, passport_id) values (links.service_id, links.client_id, links.passport_id)
            '''
        )
        log('batch inserted')
        cur.execute('''delete from tmp_links''')
        log('tmp links deleted')


def _insert_props(cur, insert_props):
    log(insert_props)
    for batch in paginate(insert_props, EXECUTE_MANY_PAGE):
        cur.executemany(
            '''
                insert into tmp_props (object_id, value_num, passport_id)
                values (:object_id, :value_num, :passport_id)
            ''',
            batch
        )
        cur.execute(
            '''
            merge /*+ PARALLEL */ into bo.t_extprops
            using tmp_props
            on (bo.t_extprops.object_id=tmp_props.object_id and bo.t_extprops.classname='Client' and attrname='service_id')
            when not matched then
            insert (id, object_id, classname, attrname, value_num, update_dt, passport_id)
            values (bo.s_extprops.nextval, tmp_props.object_id, 'Client', 'service_id', tmp_props.value_num, sysdate, tmp_props.passport_id)
            '''
        )
        log('batch inserted')
        cur.execute('delete from tmp_props')
        log('tmp props deleted')


def _executemany(cur, sql, objects):
    log('starting batch %s of len %s' % (sql, len(objects)))
    for batch in paginate(objects, EXECUTE_MANY_PAGE):
        cur.executemany(sql, objects)
        log('batch processeded')


# def _update_orders(cur, update_orders):


def _simple_simple(cur, clients, excluded_clients):
    passports = set()
    insert_links = []
    insert_props = []

    for client_id, passport_id, service_id in clients:
        if service_id in FIX_SERVICES and client_id not in excluded_clients:
            insert_links.append({'service_id': service_id, 'client_id': client_id, 'passport_id': passport_id})
            insert_props.append({'object_id': client_id, 'value_num': service_id, 'passport_id': passport_id})
            passports.add(passport_id)

    log('starting insert %d links' % len(insert_links))

    _insert_links(cur, insert_links)

    log('starting insert %d extprops' % len(insert_props))

    _insert_props(cur, insert_props)

    passports = list(passports)

    _executemany(cur, 'update bo.t_passport set client_id=NULL where passport_id=:passport', [(i,) for i in passports])


def _complex_simple(cur, client_id):
    log("new client batch")
    insert_links = []
    insert_props = []
    update_orders = []
    update_invoices = []
    update_invoice_order = []
    update_requests = []
    update_request_order = []
    log("client %s" % client_id)
    passport_ids = cur.execute('''
        select passport_id from bo.t_order where service_id=23 and client_id = :1
    ''', (client_id,))
    passport_ids = set([p[0] for p in passport_ids])
    services = cur.execute(
        '''
        select distinct o.service_id
        from bo.t_client c
        join bo.t_order o on o.client_id = c.id
        where c.id = :client_id
        ''', (client_id,)
    )
    services = [obj[0] for obj in services]

    attrs = cur.execute(
        '''
            select
            ID,
            CLASS_ID,
            MANUAL_DISCOUNT,
            DIRECT25,
            PARTNER_TYPE,
            MANUAL_SUSPECT,
            IS_DOCS_SEPARATED,
            IS_DOCS_DETAILED,
            IS_NON_RESIDENT,
            DOMAIN_CHECK_STATUS,
            CITY,
            RELIABLE_CC_PAYER,
            MANUAL_SUSPECT_COMMENT,
            FULLNAME,
            CURRENCY_PAYMENT,
            DOMAIN_CHECK_COMMENT,
            REGION_ID,
            FULL_REPAYMENT,
            SUSPECT,
            OVERDRAFT_LIMIT,
            OVERDRAFT_BAN,
            BUDGET,
            IS_AGGREGATOR,
            CREATION_DT,
            DT,
            IS_AGENCY,
            INTERNAL,
            DENY_CC,
            IS_WHOLESALER,
            AGENCY_ID,
            OPER_ID,
            CREATOR_UID,
            CLIENT_TYPE_ID,
            NAME,
            EMAIL,
            PHONE,
            FAX,
            URL,
            PERSON_ID,
            ID_1C
            from bo.t_client where id=:pk
        ''',
        (client_id,)
    ).fetchone()
    attrs = list(attrs)
    for i in range(len(attrs)):
        if isinstance(attrs[i], cx_Oracle.LOB):
            attrs[i] = attrs[i].read()
    log('passports %s, services %s' % (passport_ids, client_ids))
    for passport_id in passport_ids:
        if cur.execute(
                        '''select count(*) from t_service_client where service_id=23 and passport_id=:passport_id''',
                        {'passport_id': passport_id}
                      ).fetchone()[0] > 0:
            log('skipping passport %s' % passport_id)
            continue
        for service in services:
            if service in FIX_SERVICES:  # and len(services) > 1:
                new_pk = cur.execute('select bo.s_client_id.nextval from dual').fetchone()[0]

                # id, class_id
                attrs[0] = attrs[1] = new_pk
                # ------------- NEW CLIENTS
                insert_sql = '''
                insert into bo.t_client (
                    ID,
                    CLASS_ID,
                    MANUAL_DISCOUNT,
                    DIRECT25,
                    PARTNER_TYPE,
                    MANUAL_SUSPECT,
                    IS_DOCS_SEPARATED,
                    IS_DOCS_DETAILED,
                    IS_NON_RESIDENT,
                    DOMAIN_CHECK_STATUS,
                    CITY,
                    RELIABLE_CC_PAYER,
                    MANUAL_SUSPECT_COMMENT,
                    FULLNAME,
                    CURRENCY_PAYMENT,
                    DOMAIN_CHECK_COMMENT,
                    REGION_ID,
                    FULL_REPAYMENT,
                    SUSPECT,
                    OVERDRAFT_LIMIT,
                    OVERDRAFT_BAN,
                    BUDGET,
                    IS_AGGREGATOR,
                    CREATION_DT,
                    DT,
                    IS_AGENCY,
                    INTERNAL,
                    DENY_CC,
                    IS_WHOLESALER,
                    AGENCY_ID,
                    OPER_ID,
                    CREATOR_UID,
                    CLIENT_TYPE_ID,
                    NAME,
                    EMAIL,
                    PHONE,
                    FAX,
                    URL,
                    PERSON_ID,
                    ID_1C
                )
                values (%s)
                ''' % ','.join(':%d' % i for i in range(1, len(attrs) + 1))
                log("%s %s" % (insert_sql, attrs))
                cur.execute(
                    insert_sql, attrs
                )
                # ------------  ORDERS
                client_service_orders = cur.execute('''
                    select id from bo.t_order where client_id=:1 and service_id=:2 and passport_id=:3
                ''', (client_id, service, passport_id)).fetchall()
                client_service_orders = [o[0] for o in client_service_orders]
                for order_id in client_service_orders:
                    update_orders.append({'new_client_id': new_pk, 'order_id': order_id, 'service_id': service})
                for orders in paginate(client_service_orders, IN_OPERATOR_PAGE):
                    # ------------ INVOICES
                    client_service_invoices = cur.execute(
                        '''
                            select invoice_id, id from bo.t_invoice_order where order_id in %s
                        ''' % (sqlarray(orders),)
                    ).fetchall()
                    update_invoice_order.extend({'new_client_id': new_pk, 'invoice_order_id': i[1]} for i in client_service_invoices)
                    client_service_invoices = [i[0] for i in client_service_invoices]
                    update_invoices.extend({'new_client_id': new_pk, 'invoice_id': invoice} for invoice in client_service_invoices)

                    # ------------ REQUESTS
                    client_service_requests = cur.execute(
                        '''
                            select request_id, id from bo.t_request_order where parent_order_id in %s
                        ''' % (sqlarray(orders),)
                    ).fetchall()
                    update_request_order.extend({'new_client_id': new_pk, 'request_order_id': i[1]} for i in client_service_requests)
                    client_service_requests = [i[0] for i in client_service_requests]
                    update_requests.extend({'new_client_id': new_pk, 'request_id': request} for request in client_service_requests)
                # ------------ EXTPROPS
                insert_props.append({'object_id': new_pk, 'value_num': service, 'passport_id': passport_id})
                # ----------  SERVICECLIENT
                insert_links.append({'service_id': service, 'client_id': new_pk, 'passport_id': passport_id})
    # raise E
    log('before deferred operations')
    # ------------ DEFERRED INSERTS & UPDATES
    log('inserting links')
    _insert_links(cur, insert_links)
    log('inserting props')
    _insert_props(cur, insert_props)
    log('updating orders %s' % update_orders)
    _executemany(
        cur,
        '''
            update bo.t_order set client_id=:new_client_id where id=:order_id and service_id=:service_id
        ''',
        update_orders
    )
    log('updating invoices %s' % update_invoices)
    _executemany(
        cur,
        '''update bo.t_invoice set client_id=:new_client_id where id = :invoice_id''',
        update_invoices
    )

    log('updating requests %s' % update_requests)
    _executemany(
        cur,
        '''update bo.t_request set client_id=:new_client_id where id=:request_id''',
        update_requests
    )
    log('updating request_order %s' % update_request_order)
    _executemany(
        cur,
        '''update bo.t_request_order set client_id=:new_client_id where id=:request_order_id''',
        update_request_order
    )
    log('updating invoice_order %s' % update_invoice_order)
    _executemany(
        cur,
        '''update bo.t_invoice_order set client_id=:new_client_id where id=:invoice_order_id''',
        update_invoice_order
    )


def make_broken(conn):
    cur = conn.cursor()
    clients = cur.execute(
        '''
            select sc.client_id, sc.passport_id
            from bo.t_service_client sc
            where sc.service_id = 23
        '''
    ).fetchall()
    log('%s clients to broken' % len(clients))
    cur.executemany('''update t_passport set client_id=:client_id where passport_id=:passport_id''', clients)
    log('t_passport broken')
    cur.executemany(
        '''delete from t_extprops where value_num=23 and object_id=:client_id and classname='Client' and attrname='service_id' and passport_id=:passport_id''', clients
    )
    log('t_extprops broken')
    cur.executemany('''delete from t_service_client where service_id=23 and client_id=:client_id and passport_id=:passport_id''', clients)
    log('t_service_client broken')


def fix_broken(conn):
    cursor = conn.cursor()
    clients = cursor.execute('''
        select sc.client_id, sc.passport_id from (
            select client_id, passport_id from (
                select distinct ps.client_id, ps.passport_id
                from bo.t_passport ps where ps.client_id in
                    (
                    select c.id
                    from bo.t_client c
                    join bo.t_order o on o.client_id = c.id
                    group by c.id
                    having count(distinct o.service_id) = 1
                    )
            ) where client_id in (select client_id from bo.t_order where service_id=23)

            ) x join t_service_client sc
            on
            (x.passport_id=sc.passport_id -- and x.service_id=sc.service_id
            and  sc.service_id=23
            and x.client_id != sc.client_id ) order by sc.client_id
    ''').fetchall()
    log("%s" % clients)
    _executemany(
        cursor,
        '''
        delete from t_service_client where service_id=23 and client_id = :client_id and passport_id = :passport_id
        ''',
        clients
    )


def get_complex_clients_with_multiple_passports(cursor, ids=None):
    res = []
    for client_ids in paginate(ids, IN_OPERATOR_PAGE):
        response = cursor.execute('''
            select client_id, count(passport_id) from
                (select distinct client_id, passport_id from bo.t_order where service_id=23 and client_id in %s order by client_id)
            group by client_id
            having count(passport_id) > 1
        ''' % sqlarray(client_ids)
        ).fetchall()
        res.extend([i[0] for i in response])
    return res


def list_complex_clients_with_multiple_passports(conn, ids):
    print get_complex_clients_with_multiple_passports(conn.cursor(), ids)


def list_unsafe_clients(conn, ids):
    res = []
    for client_id in ids:
        logging.debug('checking client %s', client_id)
        res.append(_is_client_safe(conn.cursor(), client_id))
    print res


def list_payments(conn, ids):
    cursor = conn.cursor()
    for id_ in ids:
        res = cursor.execute(
            # -- select id, creator_uid /*, transaction_id, parent_transaction_id */ from t_payment where creator_uid in
            '''
            select distinct parent_transaction_id from t_payment p
              left join t_apple_inapp_payment ap on p.id=ap.id
              where p.creator id in
                    (select distinct passport_id from t_order where client_id=%s)
                ''' % id_
        ).fetchall()
        print set([i[1] for i in res])


def _is_client_safe(cur, client_id):
    logging.debug('testing client %s' % client_id)
    passport_ids = cur.execute(
        '''
        select distinct passport_id from t_order where client_id=:1
        ''',
        (client_id,)
    )
    passport_ids = filter(None, [i[0] for i in passport_ids])
    logging.debug('found passports %s in orders for client %s' % (passport_ids, client_id))
    parent_transactions_map = {}
    parent_transactions_data = cur.execute(
        # -- select id, creator_uid /*, transaction_id, parent_transaction_id */ from t_payment where creator_uid in
        '''
        select distinct parent_transaction_id, creator_uid from bo.t_payment p
          join bo.t_apple_inapp_payment ap on p.id=ap.id
          where parent_transaction_id is not null and p.invoice_id in (
                    select id from t_invoice where client_id=%(client_id)s
                )
            ''' % {'client_id': client_id}
    )
    """
    parent_transactions_data = cur.execute(
        # -- select id, creator_uid /*, transaction_id, parent_transaction_id */ from t_payment where creator_uid in
        '''
        select distinct parent_transaction_id, creator_uid from bo.t_payment p
          join bo.t_apple_inapp_payment ap on p.id=ap.id
          where parent_transaction_id is not null and creator_uid in %s
        ''' % sqlarray(passport_ids)
    )
    """

    for p_tid, passport_uid in parent_transactions_data:
        parent_transactions_map.setdefault(passport_uid, set())
        parent_transactions_map[passport_uid].add(p_tid)

    logging.debug('parent_transactions_map: %s' % parent_transactions_map)
    for k, v in parent_transactions_map.items():
        for kk, vv in parent_transactions_map.items():
            if k != kk:
                if v & vv:
                    logging.debug('client %s is not safe, passports %s and %s have common parent transactions %s and %s' % (client_id, k, kk, v, vv))
                    return False
    logging.debug('client %s is safe' % client_id)
    return True

def detect_broken(conn, ids):
    cur = conn.cursor()
    # for i in range(0, len(ids), IN_OPERATOR_PAGE):
    # min_i, max_i = i, i + IN_OPERATOR_PAGE
    counter = 0
    for client_id in ids:
        passports = cur.execute(
            '''
                select passport_id from t_passport where client_id=:0
            ''',
                (client_id,),
        ).fetchall()
        passports = [i[0] for i in passports]
        passports2 = cur.execute(
            '''select distinct passport_id from t_order where client_id=:0''',
                (client_id,),
        )
        passports2 = [i[0] for i in passports2]

        if set(passports2) - set(passports):
            log('%s -> %s & %s -> %s' % (client_id, passports2, passports, set(passports2) - set(passports)))
            counter += 1
    log("total counter %s" % counter)

if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG, format="%(asctime)s: %(message)s", handler=logging.StreamHandler())
    dburi, command, client_ids = sys.argv[1], sys.argv[2], map(int, sys.argv[3:])
    # client_ids = map(int, sys.argv[3:])
    if not client_ids:
        client_ids = raw_input('type client_ids').strip()
        print "%r" % client_ids
        client_ids = filter(None, client_ids.split(' '))
    # assert len(client_ids) <= IN_OPERATOR_PAGE, 'nonononono, too many clients, won\' paginate them'
    # conn = cx_Oracle.connect(dburi)
    # conn = cx_Oracle.connect(user='bo', password=secrets.get_secret(*secrets.DbPwd.BALANCE_BO_PWD),
    #                          dsn=cx_Oracle.makedsn('ps-node06h-vip.yandex.ru', '1521', 'balatstdb').replace('SID',
    #                                                                                                           'SERVICE_NAME'))
    #
    conn = cx_Oracle.connect(user='bo', password=secrets.get_secret(*secrets.DbPwd.BALANCE_BO_PWD),
                             dsn=cx_Oracle.makedsn('balance-load2e-vip.yandex.ru', '1521', 'balancedb').replace('SID',
                                                                                                              'SERVICE_NAME'))

    try:
        log('autocommit mode %s' % conn.autocommit)
        conn.autocommit = False
        log('connected')
        clean(conn)
        prepare(conn)
        if command == 'simple':
            simple_simple(conn, ids=client_ids or None)
        elif command == 'complex':
            '''
            if len(client_ids) > IN_OPERATOR_PAGE:
                client_ids = client_ids[0: IN_OPERATOR_PAGE]
                log('trimming clients!!!!!!!')
            '''
            complex_simple(conn, ids=client_ids or None)
        elif command == 'list_complex_clients':
            list_complex_clients(conn, ids=client_ids or None)
        elif command == 'list_complex_clients_with_multiple_passports':
            list_complex_clients_with_multiple_passports(conn, ids=client_ids or None)
        elif command == 'list_payments':
            list_payments(conn, ids=client_ids or None)
        elif command == 'list_simple_clients':
            list_simple_clients(conn, ids=client_ids or None)
        elif command == 'list_unsafe_clients':
            list_unsafe_clients(conn, ids=client_ids)
        elif command == 'fix_broken':
            fix_broken(conn)
        elif command == 'detect_broken':
            detect_broken(conn, ids=client_ids)
        elif command == 'make_broken' and sys.argv[2] == 'jfa8vn5849bn4859h58gijkdjhrg89rhge':
            make_broken(conn)
        else:
            raise Exception()
        if not DEBUG:
            conn.commit()
    except:
        conn.rollback()
        raise
    finally:
        conn.close()
    log('the end')
