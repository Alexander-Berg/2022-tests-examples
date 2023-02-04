import gevent
import utils
import coredump_utils


def test_coredumps_gdb_traces_sending(cwd, ctl, patch_loop_conf, ctl_environment, request):
    received_traces = []

    web_server = coredump_utils.create_web_server(received_traces)

    web_thread = gevent.spawn(web_server.run)
    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    port = web_server.wsgi.socket.getsockname()[1]

    cores_dir = cwd.join('cores')
    cores_dir.ensure(dir=True)

    timeout_before_sending = 2
    check_timeout = 0.5

    coredump_utils.update_loop_conf(str(cwd.join('loop.conf')), port, timeout_before_sending, check_timeout)
    cwd.join('fake_gdb.sh').chmod(0o774)

    binary_name = 'httpsearch'

    ctype = None
    ctype_prefix = 'a_ctype_'
    for tag in ctl_environment['BSCONFIG_ITAGS'].split():
        if tag.startswith(ctype_prefix):
            ctype = tag[len(ctype_prefix):]

    p = utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)

    dump_name = '{}.xxx'.format(binary_name)
    parsed_traces = ('Program terminated with signal SIGSEGV\n\n'
                     'Thread 1 (Thread 0x0)\n'
                     '#0  0x0 in pthread_join () from /build/buildd/eglibc-2.15/nptl/pthread_join.c:89\n\n')

    expected = set()
    for i in xrange(2):
        d = '{}.{}'.format(dump_name, i)
        cores_dir.join(d).write('FAKE_CONTENT')

        # check if sent minidump was deleted
        expected.add('{}.sent'.format(d))
        coredump_utils.assert_directory_contains_files(expected, cores_dir)

        assert received_traces[i]['traces'] == parsed_traces

        received_params = received_traces[i]['params'].to_dict()

        del received_params['time']

        assert received_params == {
            'service': 'fake_service',
            'ctype': ctype,
            'server': 'localhost:{}'.format(ctl_environment['BSCONFIG_IPORT']),
            'signal': '',
        }

    # check ignoring coredump if it doesn't start with binary name
    cores_dir.join('some_fake_name').write('SOME_FAKE_CONTENT')
    gevent.sleep(timeout_before_sending * 2)

    assert len(received_traces) == 2

    expected.add('some_fake_name')
    coredump_utils.assert_directory_contains_files(expected, cores_dir)

    # check if instancectl fails when aggregator is down
    web_server.stop()
    web_thread.kill()

    gevent.sleep(check_timeout)

    cores_dir.join(dump_name).write('SOME_FAKE_CONTENT')
    gevent.sleep(timeout_before_sending * 2)

    expected.add('{}.sent'.format(dump_name))
    coredump_utils.assert_directory_contains_files(expected, cores_dir)

    utils.must_stop_instancectl(ctl, check_loop_err=False, process=p)
