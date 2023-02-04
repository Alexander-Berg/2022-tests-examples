# coding=utf-8
from __future__ import unicode_literals

import time

import utils


def test_progressive_restart_intervals(cwd, ctl, patch_loop_conf, ctl_environment, request):
    """
    Запускаемся над двумя демонами. Один падает нон-стоп, второй работает.

    * Проверяем, что упавшая работа респавнится с прогрессирующим таймаутом
    * Проверяем crash.log на количество линий
    """
    p = utils.must_start_instancectl(ctl, request, ctl_environment)
    time.sleep(30)
    utils.must_stop_instancectl(ctl, process=p)

    bad_times = sorted(float(l.split()[0]) for l in cwd.join('bad_result.txt').readlines())

    deltas = [bad_times[i + 1] - v for i, v in enumerate(bad_times[:-1])]

    for i, v in enumerate(deltas[:-1]):
        # Проверка, что таймауты растут
        assert deltas[i + 1] > v

        # Проверка, что бинарь не рестартили чаще раза в секунду (min_delay)
        assert v > 1
        assert deltas[i + 1] > 1

        # Проверка, что бинарь не рестартили позже, чем через max_delay секунд после завершения
        assert deltas[i + 1] < 20

    required_crash_log_fields = {'datetime', 'exit_code', 'instance_name', 'pid', 'section', 'timestamp_ms'}

    crash_log_lines = cwd.join('crash', 'crash.log').readlines()
    crash_log_counts = {'test_daemon_crashes_bad': 0, 'test_daemon_crashes_good': 0}
    for line in crash_log_lines:
        record = dict(kv.split('=') for kv in line.rstrip().split('\t'))
        crash_log_counts[record['section']] += 1
        assert record['instance_name'] == 'localhost:1543'
        assert required_crash_log_fields.issubset(record.iterkeys())
        if record['section'] == 'test_daemon_crashes_good':
            assert record['term_signal'] == '15'
            assert record['exit_code'] == '-15'
            assert record['exit_status'] == ''
        else:
            assert record['term_signal'] == '' or record['exit_status'] == ''
            if record['term_signal']:
                assert int(record['exit_code']) == -int(record['term_signal'])
            else:
                assert record['exit_status']
                assert record['exit_code'] == record['exit_status']

    assert crash_log_counts['test_daemon_crashes_good'] == 1

    # Дальше вилка: могли попасть в период между запусками "плохого" демона, а могли попасть в момент когда он
    # работает.
    # Плохой демон при старте пишет в bad_result.txt и через секунду дохнет.
    #
    # В первом случае количество записей в bad_result.txt и crash.log должно совпадать, т.к. запуск плохого демона
    # триггерит по событию в обоих логах
    #
    # Во втором случае stop() должен прибить демон и создать запись в crash_log. Т.е. тоже должно получиться
    # одинаково в обоих логах
    assert crash_log_counts['test_daemon_crashes_bad'] == len(bad_times)

    good_times = [int(l.split()[0]) for l in cwd.join('good_result.txt').readlines()]
    assert len(good_times) == 1



