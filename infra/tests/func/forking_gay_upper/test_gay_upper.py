# coding=utf-8
import subprocess

import gevent

from sepelib.subprocess.util import terminate

from utils import must_start_instancectl, must_stop_instancectl
from utils import wait_condition_is_true


def cont_pids(role, file):
    """
    Считаем, сколько было мастеров и детей в тесте. Вспомогательная функция

    :param str role: master или child
    :param LocalPath file:
    :return:
    """

    with file.open() as fd:
        data = fd.readlines()

    pids = set()
    for line in data:
        if role in line:
            pids.add(line.split()[1])

    return len(pids)


def test_forking_daemon(cwd, ctl, patch_loop_conf, ctl_environment, request):
    """
    Проверяем как мы работает с форкающимися демонами

    Схема теста:
        Демон берёт лок и форкается. В этом случае дескриптор лока наследуется в ребёнка.

        Дальше в случае смерти основного процесса, лок может оказаться занят и мы не сможем перезапустить демон.

        Демон прибиваем через reopenlog


    Встречается такое поведение, например, в верхнем метапоске, который запускает заглушка и следит за его живостью
    по косвенным признакам.

    В качестве фикса переключаемся на lockf c flock
    """
    p = must_start_instancectl(ctl, request, ctl_environment)
    gevent.sleep(3)

    assert cont_pids('master', cwd.join('output.txt')) == 1
    assert cont_pids('child', cwd.join('output.txt')) == 1

    checks = [
        lambda: cont_pids('master', cwd.join('output.txt')) > 1 + iteration,
        lambda: cont_pids('child', cwd.join('output.txt')) > 1 + iteration,
    ]

    for iteration in xrange(3):
        reopenlog = subprocess.Popen([str(ctl), 'reopenlog'], cwd=ctl.dirname, env=ctl_environment)
        request.addfinalizer(lambda: terminate(reopenlog))

        f = lambda: reopenlog.poll() is not None
        assert wait_condition_is_true(f, 3, 0.1), 'reopenlog failed to stop!'

        for check in checks:
            assert wait_condition_is_true(check, 10, 0.1)

    must_stop_instancectl(ctl, process=p)

