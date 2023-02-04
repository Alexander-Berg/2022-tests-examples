# coding=utf-8
import os
from itertools import chain
from time import sleep
import random
import fcntl
import subprocess
from textwrap import dedent

from instancectl.lib.procutil import ExtendedPopen


def set_cloexec_flag(fd, cloexec=True):
    """
    Копия set_cloexec_flag из Popen
    :param int fd:
    :param bool cloexec:
    :return:
    """
    try:
        cloexec_flag = fcntl.FD_CLOEXEC
    except AttributeError:
        cloexec_flag = 1

    old = fcntl.fcntl(fd, fcntl.F_GETFD)
    if cloexec:
        fcntl.fcntl(fd, fcntl.F_SETFD, old | cloexec_flag)
    else:
        fcntl.fcntl(fd, fcntl.F_SETFD, old & ~cloexec_flag)


def set_nonblock(fd):
        fl = fcntl.fcntl(fd, fcntl.F_GETFL)
        fcntl.fcntl(fd, fcntl.F_SETFL, fl | os.O_NONBLOCK)


def test_extended_popen(tmpdir):
    """
    Тестируем наш бекпорт pass_fds для Popen

    :return:
    """

    # Делаем пачку пайпов в трёх категориях:
    # 1. не передаём оба конца
    # 2. передаей r конец
    # 3. передаей w конец

    random.seed(0)
    close_pipes = []
    r_pass_pipes = []
    w_pass_pipes = []

    for _ in xrange(20):
        r, w = os.pipe()
        set_cloexec_flag(r, cloexec=False)
        set_cloexec_flag(w, cloexec=False)

        i = random.randint(0, 2)
        # тут важен порядок, чтобы дескрипторы чередовались
        if i == 0:
            close_pipes.append([r, w])
        elif i == 1:
            r_pass_pipes.append([r, w])
        elif i == 2:
            w_pass_pipes.append([r, w])

    # print close_pipes
    # print r_pass_pipes
    # print w_pass_pipes

    assert len(close_pipes) > 2
    assert len(r_pass_pipes) > 2
    assert len(w_pass_pipes) > 2

    code = dedent("""
        import os
        import sys

        if __name__ == '__main__':
            for pipe in [{err_pipes}]:
                try:
                    os.write(pipe, 'Hello!')
                except OSError:
                    pass
                else:
                    raise AssertionError('Can write!')

                try:
                    print os.read(pipe, 1)
                except OSError:
                    pass
                else:
                    raise AssertionError('Can read!')

            for pipe in [{r_pipes}]:
                assert os.read(pipe, 2) == 'ok', 'Cannot read'

            for pipe in [{w_pipes}]:
                os.write(pipe, 'ok')

            print 'written: ok'
            sys.stderr.write('written: ok')

    """)

    for w_pipe in (x[1] for x in r_pass_pipes):
        os.write(w_pipe, 'ok')

    expected_close_pipes = list(set(chain(
        (x[1] for x in r_pass_pipes),
        (x[0] for x in w_pass_pipes),
        (x[0] for x in close_pipes),
        (x[1] for x in close_pipes),
    )))

    pass_pipes = list(chain(
        (x[0] for x in r_pass_pipes),
        (x[1] for x in w_pass_pipes),
    ))

    formatted_code = code.format(err_pipes=', '.join(str(p) for p in expected_close_pipes),
                                 r_pipes=', '.join(str(x[0]) for x in r_pass_pipes),
                                 w_pipes=', '.join(str(x[1]) for x in w_pass_pipes))

    test_script = tmpdir.join('test.py')
    test_script.write(formatted_code)

    # Чтобы избежать warning'ов от coverage в stderr
    env = {k: v for k, v in os.environ.iteritems() if not k.startswith('COV_')}

    p = ExtendedPopen(['python {}'.format(test_script.strpath)], pass_fds=pass_pipes, shell=True,
                      stdout=subprocess.PIPE, stderr=subprocess.PIPE, close_fds=True, env=env)

    sleep(2)
    if p.poll() is None:
        p.kill()
        raise AssertionError('Process failed to stop')

    assert p.poll() == 0

    for stream in [p.stdout, p.stderr]:
        set_nonblock(stream.fileno())
        data = stream.read()
        print data
        assert data.strip() == 'written: ok'

    for r_pipe in (x[0] for x in w_pass_pipes):
        set_nonblock(r_pipe)
        assert os.read(r_pipe, 2) == 'ok'

