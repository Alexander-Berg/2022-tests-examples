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
            select id from t_client
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
            where passport_id in (
                select passport_id from bo.t_order
                where id in
                (select max(id) max_id from bo.t_order where service_id=23 and client_id in %s group by client_id)
            )
        ''' % sqlarray(ids)
    ).fetchall()
    log("%d clients " % len(filtered_clients))
    if ids is not None and len(filtered_clients) != len(ids):
        raise Exception('bad client ids in the list, %d passed, %d found' % (len(ids), len(filtered_clients)))
    log("%d clients after filtering, %s" % (len(filtered_clients), filtered_clients))

    # print len(clients), clients

    for client, passport in filtered_clients:
        try:
            _complex_simple(cur, client, passport)
            if not DEBUG:
                conn.commit()
        except:
            logging.error('BATCH FAILED, %s %s' % (traceback.format_exc(), (client, passport)))
            conn.rollback()
    log('complex_simple end')


def _insert_links(cur, insert_links):
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


def _complex_simple(cur, client_id, passport_id):
    log("new client batch")
    passports = set()
    insert_links = []
    insert_props = []
    update_orders = []
    update_invoices = []
    update_invoice_order = []
    update_requests = []
    update_request_order = []
    log("client %s" % client_id)
    passports.add(passport_id)
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
            update_orders.append({'new_client_id': new_pk, 'old_client_id': client_id, 'service_id': service})
            client_service_orders = cur.execute('''
                select id from bo.t_order where client_id=:1 and service_id=:2
            ''', (client_id, service)).fetchall()
            client_service_orders = [o[0] for o in client_service_orders]
            # ------------ INVOICES
            for orders in paginate(client_service_orders, IN_OPERATOR_PAGE):
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
            update bo.t_order set client_id=:new_client_id where client_id=:old_client_id and service_id=:service_id
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


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG, format="%(asctime)s: %(message)s", handler=logging.StreamHandler())
    dburi, command, client_ids = sys.argv[1], sys.argv[2], map(int, sys.argv[3:])
    assert len(client_ids) <= IN_OPERATOR_PAGE, 'nonononono, too many clients, won\' paginate them'
    # conn = cx_Oracle.connect(dburi)
    conn = cx_Oracle.connect(user='bo', password=secrets.get_secret(*secrets.DbPwd.BALANCE_BO_PWD),
                             dsn=cx_Oracle.makedsn('ps-node06h-vip.yandex.ru', '1521', 'balatstdb').replace('SID',
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
            complex_simple(conn, ids=client_ids or None)
        elif command == 'list_complex_clients':
            list_complex_clients(conn, ids=client_ids or None)
        elif command == 'list_simple_clients':
            list_simple_clients(conn, ids=client_ids or None)
        elif command == 'fix_broken':
            fix_broken(conn)
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