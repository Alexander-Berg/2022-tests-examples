from yatest import common

import logging
import os
import random
import time


def read_ports(logfile):
    try:
        with open(logfile, 'r') as f:
            cont = f.read()
        if ';search_port=' not in cont or ';indexer_port=' not in cont:
            return False
        parts = cont.split(';')
        spp = [p.split('=')[1] for p in parts if p.startswith('search_port=')][0]
        ipp = [p.split('=')[1] for p in parts if p.startswith('indexer_port=')][0]
        sport = int(spp)
        iport = int(ipp)
        return {'search_port': sport, 'indexer_port': iport}

    except Exception as e:
        logging.error('problem with opening file %s, error %s', logfile, e)
        return False


def run_cluster(prefixed=False, test_name=''):
    t_log_path = common.output_path('cluster_run_' + test_name)

    bin_path = common.binary_path('saas/rtyserver_test/rtyserver_test')
    conf_arc_path = 'saas/rtyserver_test/func/configs/cluster/'
    cluster_cfg_path = common.source_path(conf_arc_path + 'cluster_1be_internal.cfg')
    assert os.path.exists(cluster_cfg_path)
    konoff = 'on' if prefixed else 'off'
    tester_test_name = 'SuspendIdleKv' if 'kv' in test_name.lower() else 'SuspendIdle'
    cmd = [bin_path, '-t', tester_test_name, '-g', cluster_cfg_path, '-k', konoff]

    env = dict(os.environ)
    port_st = int(5000 * random.random())
    env['RTY_TESTS_START_PORT'] = str(3000 + port_st)  # str(common.network.shift_test_port(port_st))
    env['RTY_TESTS_USE_LOCALHOST'] = 'true'
    logging.info('start_port=%s' % (3000 + port_st))

    p = common.process.execute(cmd, env=env,
                               wait=False, check_exit_code=False,
                               stdout=open(t_log_path + '.out', 'w'),
                               stderr=open(t_log_path + '.err', 'w')
                               )
    ports = dict()
    for i in range(60):
        ports = read_ports(t_log_path + '.out')
        if ports:
            break
        time.sleep(1)
        if not p.running:
            logging.error('cluster failed, exit_code=%s, check_num=%s' % (p.exit_code, i))
        assert p.running
    ports['process'] = p
    return ports
