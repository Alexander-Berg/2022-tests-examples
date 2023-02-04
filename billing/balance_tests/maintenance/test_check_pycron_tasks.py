# -*- coding: utf-8 -*-
import json

from balance import balance_db as db
from btestlib.utils import TestsError

ALWAYS_OFF = ['process_thirdparty_transactions',
              'partner_completions',
               # не должен быть включен в тесте, т.к. тарификация запущена на load базе
              'process_ng_logbroker_consume',
              ]


def test_check_turned_off_pycron_tasks():
    tasks = db.balance().execute("""
    SELECT * FROM BO.t_pycron_descr d 
    join bo.T_PYCRON_SCHEDULE s on d.name = s.name 
    where d.terminate = 0 and s.enabled = 1
    """)
    wrongly_turned_on_tasks = [task['name'] for task in tasks if task['name'] in ALWAYS_OFF]
    if wrongly_turned_on_tasks:
        raise TestsError(u'Должны быть выключены, но включены {}'.format(wrongly_turned_on_tasks))


def test_check_turned_on_pycron_tasks():
    always_on = db.balance().execute("SELECT value_json FROM bo.t_config WHERE item = 'TASKS_LIST'")[0]['value_json']
    always_on = json.loads(always_on)
    tasks = db.balance().execute("""SELECT * FROM BO.t_pycron_descr where terminate = 1""")
    wrongly_turned_off_tasks = [task['name'] for task in tasks if task['name'] in always_on]
    if wrongly_turned_off_tasks:
        raise TestsError(u'Должны быть включены, но выключены {}'.format(wrongly_turned_off_tasks))
