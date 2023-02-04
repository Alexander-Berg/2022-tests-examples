# coding: utf-8


import btestlib.reporter as reporter

from balance import balance_steps as steps

TASKS = ['monthly_limits']


def test_resolve_mnclose_task():
    with reporter.step(u"Переводим в статус resolve таски {0} из mnclose".format(TASKS)):
        for task in TASKS:
            steps.CloseMonth.resolve_task(task)
