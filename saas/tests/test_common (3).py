from restart_exception import RtyTestRestartException, check_log_busy_ports
from profiler import PeriodicProf
from profiler import max_metrics

from yatest import common

import gzip
import logging
import os
import random
import signal
import stat
import time

BIN_PATH = common.binary_path("saas/rtyserver_test/rtyserver_test")


def log_tail(logfile):
    ltail = ''
    last_lines = []
    try:
        with open(logfile, 'r') as f:
            for line in f.readlines():
                last_lines.append(line)
                if len(last_lines) > 20:
                    last_lines = last_lines[10:]
                if 'Failed test:' in line:
                    break
            ltail = ''.join(last_lines)
            logging.info(ltail)
    except Exception as e:
        logging.error('while log_tail: %s' % e)
    return ltail


def time_metrics_from_output(out_log):
    times = {}
    fields = ('full_time', 'init_cluster_time', 'stop_cluster_time', 'clear_time')
    with open(out_log, 'r') as f:
        for line in f.readlines():
            if ';full_time=' in line:
                parts = line.split(';')
                for s in parts:
                    try:
                        for field in fields:
                            if s.startswith(field + '='):
                                t_time = float(s.split('=')[1].strip('s'))
                                times[field] = times.get(field, 0) + t_time
                    except Exception as e:
                        logging.error('%s' % e)
    return times


def processExc(e, logs_dir, t_log_path, metrics, reraise=True):
    check_log_busy_ports(logs_dir)
    exc_descr = '%s' % e
    l = log_tail(t_log_path + '.err.txt')
    exc_descr += l
    if isinstance(e, common.process.ExecutionError) and metrics is not None:
        metrics.set('signal', e.execution_result.exit_code)
    if reraise:
        raise Exception(exc_descr)


def prepare_params(test_params, test_name, test_key):
    if '$DICT_PATH' in test_params:
        d_path = common.data_path('recognize')
        test_params = test_params.replace('$DICT_PATH', d_path)
    if '$CACHE_DIR' in test_params:
        cache_dir = os.getcwd() + '/cache_' + test_key
        os.makedirs(cache_dir)
        test_params = test_params.replace('$CACHE_DIR', cache_dir)
    if 'Oxy' in test_name:
        tdata_dir = os.path.abspath('test_data')
        common.path.copytree(common.data_path('rtyserver/test_data/oxy'), tdata_dir + '/oxy')
        common.path.copytree(common.data_path('rtyserver/test_data/kiwi_test'), tdata_dir + '/kiwi_test')
        os.chmod(tdata_dir, 0o777)
        test_params = test_params.replace('$TEST_DATA_PATH', tdata_dir)
        toxy_dir = os.path.abspath('oxy_data')
        if not os.path.exists(toxy_dir):
            raise Exception('oxy data not found')
    if 'Graph' in test_name:
        tdata_dir = os.path.abspath('test_data')
        if not os.path.exists(tdata_dir + '/graph'):
            common.path.copytree(common.data_path('rtyserver/test_data/graph'), tdata_dir + '/graph')
        test_params = test_params.replace('$TEST_DATA_PATH', tdata_dir)
    if 'ReadOnly' in test_name:
        tdata_dir = os.path.abspath('test_data')
        if not os.path.exists(tdata_dir + '/read_only'):
            common.path.copytree(common.data_path('rtyserver/test_data/read_only'), tdata_dir + '/read_only')
        test_params = test_params.replace('$TEST_DATA_PATH', tdata_dir)
    if '$TEST_DATA_PATH' in test_params:
        tdata_dir = common.data_path('rtyserver/test_data')
        test_params = test_params.replace('$TEST_DATA_PATH', tdata_dir)
    if '$CONF_PATH' in test_params:
        configs_dir = common.source_path('saas/rtyserver_test/func/configs')
        test_params = test_params.replace('$CONF_PATH', configs_dir)
    return test_params


def run_test_impl(test_name, test_params, test_key, timeout=180, metrics=None, links=None,
                  bin_dir=None, extra_env=None):
    if timeout:
        timeout = int(timeout)
    bin_path = os.path.join(bin_dir, 'rtyserver_test') if bin_dir else BIN_PATH
    test_params = prepare_params(test_params, test_name, test_key)
    env = dict(os.environ)
    port_st = int(30000 * random.random())
    env['RTY_TESTS_START_PORT'] = str(3000 + port_st)  # str(common.network.shift_test_port(port_st))
    env['RTY_TESTS_USE_LOCALHOST'] = "true"
    if 'Oxy' in test_name:
        env['OXY_DATA_P_PATH'] = os.path.abspath('oxy_data').replace('/oxy_data', '')
    if not extra_env:
        extra_env = {}
    if 'LOG_PATH' not in extra_env:
        extra_env['LOG_PATH'] = common.output_path('logs_' + test_key)
    if extra_env:
        env.update(extra_env)

    logs_dir = env.get('LOG_PATH') or common.output_path('logs_' + test_key)
    if not os.path.exists(logs_dir):
        os.makedirs(logs_dir)
    if links:
        links.set('custom_logs', logs_dir)
    t_log_path = os.path.join(logs_dir, 'unittest_' + test_key + '_execution')

    # delete all PYTHON* keys from env, as they make sky commands work improperly
    for key in env.keys():
        if key.startswith("PYTHON"):
            del env[key]

    logging.info('start_port=%s' % (3000 + port_st))
    cmd_params = test_params.split()
    cwd = os.path.join(os.getcwd(), test_key)
    if not os.path.exists(cwd):
        os.makedirs(cwd)

    def on_timeout(execution, timeout):
        try:
            logging.info('killing process %s on timeout %s' % (execution.process.pid, timeout))
            os.kill(execution.process.pid, signal.SIGABRT)
            for i in range(10):
                time.sleep(2)
                if execution.process.poll():
                    logging.info('aborted in %s sec' % (2 * i))
                    break
        except Exception as e:
            logging.error('cannot kill process, error: %s' % e)
            pass

    try:
        p = common.process.execute([bin_path, '-t', test_name, '-s', '5'] + cmd_params,
                                   env=env,
                                   wait=False,
                                   close_fds=True,
                                   cwd=cwd,
                                   on_timeout=on_timeout,
                                   stdout=open(t_log_path + '.out.txt', 'w'),
                                   stderr=open(t_log_path + '.err.txt', 'w'))
        prof_file = os.path.join(logs_dir, 'prof_%s.out' % p.process.pid)
        prof = PeriodicProf(p.process.pid, prof_file)
        prof.start()
        p.wait(check_exit_code=True, timeout=timeout, on_timeout=on_timeout)
        times = time_metrics_from_output(t_log_path + '.out.txt')
        for time_field, time_value in times.items():
            metrics.set(time_field, round(time_value, 2))
        resources = max_metrics(prof_file)
        for m, v in resources.items():
            metrics.set(m, v)
    except Exception as e:
        processExc(e, logs_dir, t_log_path, metrics)
    finally:
        out_log = t_log_path + '.err.txt'
        if links and os.path.exists(out_log):
            links.set('tester_output', out_log)
        if os.path.exists(out_log) and 2000000 < os.stat(out_log)[stat.ST_SIZE] < 50000000:
            with gzip.open(out_log + '.gz', 'wb') as gf:
                with open(out_log, 'r') as lf:
                    gf.write(lf.read())
            if os.stat(out_log + '.gz')[stat.ST_SIZE] > 2000000:
                os.remove(out_log + '.gz')


def run_test(test_name, test_params, test_key, timeout=180, metrics=None, links=None,
             bin_dir=None, extra_env=None):
    RETRIES = 3
    for i in range(RETRIES):
        try:
            run_test_impl(test_name, test_params, test_key, timeout, metrics, links, bin_dir, extra_env)
            return
        except RtyTestRestartException as e:
            logging.error('ports collision found, will restart test')
            if os.path.exists(e.LogPath + '_portbusy'):
                os.renames(e.LogPath + '_portbusy', e.LogPath + '_portbusy_' + str(int(time.time())))
            os.renames(e.LogPath, e.LogPath + '_portbusy')
            if os.path.isdir(e.LogPath + '_portbusy'):
                os.makedirs(e.LogPath)
    raise Exception('no restarts left')
