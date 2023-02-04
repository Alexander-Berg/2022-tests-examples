# coding=utf-8
from ConfigParser import SafeConfigParser

import gevent
from instancectl.coredumps.sender import MinidumpSender

import utils
import coredump_utils


def _update_loop_conf(conf_file, port, timeout_before_sending, check_timeout, symbols):
    parser = SafeConfigParser()
    parser.read(conf_file)
    parser.set('test_minidumps', 'minidumps_aggregator_url', 'http://localhost:{}/submit/'.format(port))
    parser.set('test_minidumps', 'minidumps_timeout_before_sending', str(timeout_before_sending))
    parser.set('test_minidumps', 'minidumps_check_timeout', str(check_timeout))
    parser.set('test_minidumps', 'minidumps_clean_timeout', str(timeout_before_sending))
    parser.set('test_minidumps', 'minidumps_symbols', symbols)
    with open(conf_file, 'w') as fd:
        parser.write(fd)


def test_gdb_traces_sending(cwd, ctl, patch_loop_conf, ctl_environment, request):
    """
        Проверяем отправку minidump'ов
    """
    received_traces = []
    web_server = coredump_utils.create_web_server(received_traces)
    web_thread = gevent.spawn(web_server.run)
    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    port = web_server.wsgi.socket.getsockname()[1]

    minidumps_dir = cwd.join('minidumps', 'test_minidumps')

    timeout_before_sending = 2
    check_timeout = 0.5

    # some example of binary identifier (from some basesearch)
    # for details see https://code.google.com/p/google-breakpad/wiki/SymbolFiles
    symbols_id = '6EDC6ACDB282125843FD59DA9C81BD830'
    symbols = 'symbols.sym'

    symbols_contents = 'MODULE Linux x86_64 {} httpsearch'.format(symbols_id)

    cwd.join(symbols).write(symbols_contents)

    _update_loop_conf(str(cwd.join('loop.conf')), port, timeout_before_sending, check_timeout, symbols)

    binary_name = 'httpsearch'

    ctype = None
    ctype_prefix = 'a_ctype_'
    for tag in ctl_environment['BSCONFIG_ITAGS'].split():
        if tag.startswith(ctype_prefix):
            ctype = tag[len(ctype_prefix):]
            break

    cwd.join('minidump_stackwalk.sh').chmod(0744)
    utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)

    minidump = 'SOME_FAKE_MINIDUMP_CONTENTS'
    minidump_contents = ('Crash|SIGSEGV|0x452e|0\n'
                         '0|0|libpthread-2.15.so|pthread_join|/build/buildd/eglibc-2.15/nptl/pthread_join.c|89|0x15\n')

    converted_minidump = ('Program terminated with signal SIGSEGV\n\n'
                          'Thread 1 (Thread 0x0)\n'
                          '#0  0x0 in pthread_join () from /build/buildd/eglibc-2.15/nptl/pthread_join.c:89\n\n')

    for i in xrange(2):

        minidumps_dir.join(minidump).write(minidump_contents)

        # check if sent minidump was deleted
        coredump_utils.assert_directory_contains_files([], minidumps_dir)

        assert received_traces[i]['traces'] == converted_minidump

        received_params = received_traces[i]['params'].to_dict()

        del received_params['time']
        received_params.pop('signal', None)

        assert received_params == {
            'service': 'fake_service',
            'ctype': ctype,
            'server': 'localhost:{}'.format(ctl_environment['BSCONFIG_IPORT']),
        }

        symbols_file = cwd.join(MinidumpSender.SYMBOLS_DIR_NAME, binary_name, symbols_id, binary_name + '.sym')

        assert symbols_file.read() == symbols_contents

    # check if instancectl fails when aggregator is down
    web_server.stop()
    web_thread.kill()

    gevent.sleep(check_timeout)

    minidumps_dir.join(minidump).write(minidump)
    gevent.sleep(timeout_before_sending * 2)

    coredump_utils.assert_directory_contains_files([], minidumps_dir)

    utils.must_stop_instancectl(ctl, check_loop_err=False)
