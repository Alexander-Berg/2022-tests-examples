# coding: utf-8

import balance.balance_db as db
import btestlib.reporter as reporter


def test_clear_ton():
    with reporter.step(u'Очищаем bo.ton от всех записей'):
        query = '''declare
            v_purge_options_t dbms_aqadm.aq$_purge_options_t;
            begin
            DBMS_AQADM.PURGE_QUEUE_TABLE('"BO"."TON"',null,v_purge_options_t);
            end;'''

        db.balance().execute(query=query)


def test_clear_notifications_log():
    with reporter.step(u'Отключаем отчередь нотификаций'):
        db.balance().execute(query="UPDATE t_pycron_schedule set enabled = 0 where name = 'ton_processor'")
    try:
        with reporter.step(u'Удаляем старые нотификации из лога'):
            count_lines()
            db.balance().execute(query='DELETE FROM T_NOTIFICATION_LOG WHERE dt < trunc(sysdate - 3)')
            count_lines()
    finally:
        with reporter.step(u'Включаем очередь нотификаций'):
            db.balance().execute(query="UPDATE t_pycron_schedule set enabled = 1 where name = 'ton_processor'")


def count_lines():
    with reporter.step(u'Считаем кол-во записей в t_notification_log'):
        rows_left = db.balance().execute(query='SELECT COUNT(*) as cnt FROM T_NOTIFICATION_LOG',
                                         single_row=True)['cnt']
        reporter.attach(u'В t_notification_log {} записи'.format(rows_left),
                      u'В t_notification_log {} записи'.format(rows_left))
