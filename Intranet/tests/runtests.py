# -*- coding: utf-8 -*-
import os
import time
import nose
import requests
import json
import sys
import subprocess

from plugins import (
    DbMigrationPlugin,
    ConsoleLoggingPlugin,
)

UNISTAT_URL = 'https://stats.qloud.yandex-team.ru/unistat'


requests.packages.urllib3.disable_warnings()


def report(metric, value):
    """Передаёт сигнал в Qloud через https://stats.qloud.yandex-team.ru/unistat.
    """
    data = {metric: value}
    try:
        requests.post(UNISTAT_URL, data=json.dumps(data), verify=False)
    except:
        print('WARNING: Unable to send data to Golovan')


if __name__ == '__main__':
    import flamegraph
    # Без увеличения лимита рекурсии, nose иногда падает
    # пытаясь найти все тесты в поддиректории.
    sys.setrecursionlimit(10000)

    flamegraph.start_profile_thread(fd=open("./perf.log", "w"))
    start = time.time()

    import threading, mock
    class MyThread(threading.Thread):
        def start(self, *args, **kwargs):
            # print 'STARTING THREAD', args, kwargs
            return super(MyThread, self).start(*args, **kwargs)


    try:
        with mock.patch('threading.Thread', new=MyThread):
            nose.main(addplugins=[
                DbMigrationPlugin(),
                ConsoleLoggingPlugin(),
            ]
            )
    finally:

        # Если в последнем аргументе двоеточие, значит запустили
        # конкретный тест или TestCase и в этом случае тоже не надо репортить
        # о продолжительности тестов
        if '--failed' not in sys.argv and \
           '--help' not in sys.argv and \
           ':' not in sys.argv[-1]:
            filename = 'rst-docs/source/errors.rst'

            # создаем доку по ошибкам API
            from testutils import write_api_errors_to
            write_api_errors_to(filename)

            end = time.time()

            test_time = end - start
            report('directory_back_test_time_annn', test_time)

        if '--with-coverage' in sys.argv:
            # если тесты были запущены с coverage,
            # отправим суммарное покрытие в голован
            try:
                output = subprocess.check_output('coverage report', shell=True)
                lines = output.split('\n')
                lines = [line
                         for line in lines
                         if line.startswith('TOTAL')]
                total_line = lines[-1]
                # в конце этой строки указано суммарное покрытие
                values = total_line.split()
                percentage = int(values[-1].strip('%'))
                report('directory_back_test_coverage_annn', percentage)
            except Exception as e:
                print('Unable to report tests coverage: ', e)


        if os.environ.get('REPORT_CODE_METRICS'):
            def get_metrics(dir_name):
                """Возвращает список с метриками кода:
                SLOC, Comments, McCabe
                """
                output = subprocess.check_output(
                    "find {0} -name '*.py' | xargs metrics".format(dir_name),
                    shell=True,
                )
                lines = output.split('\n')
                lines = [_f for _f in lines if _f]
                last_line = lines[-1]
                values = last_line.split()
                values = values[-3:]
                values = list(map(int, values))
                labels = ('sloc', 'comments', 'mccabe')
                return list(zip(labels, values))

            values = get_metrics('src')
            print('Codebase metrics:')
            for key, value in values:
                print(key, '=', value)
                report(
                    'directory_back_codebase_{0}_annn'.format(key),
                    value
                )

            values = get_metrics('tests')
            print('Test codebase metrics:')
            for key, value in values:
                print(key, '=', value)
                report(
                    'directory_back_test_{0}_annn'.format(key),
                    value
                )
