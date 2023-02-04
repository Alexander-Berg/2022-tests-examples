import os
from ConfigParser import SafeConfigParser

from yatest import common

from push_client_config import main


def do_test(ctype, itype, host, port):
    out_path = common.test_output_path('res')
    if not os.path.exists(out_path):
        os.mkdir(out_path)
    result_path = os.path.join(out_path, 'push-client.conf')
    os.environ['CUSTOM_CTYPE'] = ctype
    state_dir = '/db/bsconfig/webstate/' + host + ':' + str(port) + '/pushclient'
    arg = ['-i', itype, '-o', result_path,
           '-s', state_dir,
           '-p', '/usr/local/www/logs/current-log-pushclient-' + str(port),
           '-l', '/usr/local/www/logs',
           '--bsconfig-iport', str(port), '--bsconfig-idir', '/place/db/iss3/instancedir']
    main(arg)
    return [common.canonical_file(result_path)]


def testTEST_PUSHCLIENT_CONF_TESTING():
    return do_test('testing', 'searchproxy', 'vla1-1431', 17000)


def testTEST_PUSHCLIENT_CONF_PRESTABLE():
    return do_test('prestable', 'searchproxy', 'man1-4857', 17000)


def testTEST_PUSHCLIENT_CONF_STABLE():
    return do_test('stable', 'searchproxy', 'man1-1966', 17000)


def testTEST_PUSHCLIENT_CONF_STABLE_KV():
    return do_test('stable_kv', 'searchproxy', 'man1-1966', 17000)


def testTEST_PUSHCLIENT_CONF_STABLE_MIDDLE_KV():
    return do_test('stable_middle_kv', 'searchproxy', 'man1-1966', 17000)


def testTEST_PUSHCLIENT_CONF_IPR_TESTING():
    return do_test('testing', 'indexerproxy', 'vla1-1431', 8040)


def testTEST_PUSHCLIENT_CONF_IPR_STABLE():
    return do_test('stable', 'indexerproxy', 'vla1-1431', 8040)


def testTEST_PUSHCLIENT_CONF_IPR_STABLE_KV():
    return do_test('stable_kv', 'indexerproxy', 'vla1-1431', 8040)


def testTEST_PUSHCLIENT_CONF_IPR_STABLE_MIDDLE_KV():
    return do_test('stable_middle_kv', 'indexerproxy', 'vla1-1431', 8040)


def testTEST_PUSHCLIENT_CONF_RTY_STABLE():
    return do_test('stable', 'rtyserver', 'sas1-1509', 19804)


def testTEST_PUSHCLIENT_CONF_RTY_STABLE_KV():
    return do_test('stable_kv', 'rtyserver', 'sas1-1509', 19804)


def testTEST_PUSHCLIENT_CONF_RTY_STABLE_MIDDLE_KV():
    return do_test('stable_middle_kv', 'rtyserver', 'sas1-1509', 19804)


def testTEST_LOOPCONF_SYNTAX():
    with open(common.source_path("saas/deploy_manager/environment/loop.conf"), "r") as f:
        loop_conf_parser = SafeConfigParser()
        loop_conf_parser.readfp(f)
