import sys
import time
import traceback as tb

import bz2
import zlib

import pytest
import msgpack

import gevent
import gevent.queue

from kernel.util import logging

from ya.skynet.services.heartbeatserver.bulldozer.plugins import Supervisor

from . import context

ctx = context.Context(context.Context.Config({}))

MAGIC = 0xC001C0DE
TEST_BAD_BIN = '#!%s\n\n' % sys.executable + '''
from __future__ import print_function
import os
import sys
print("I feel myself so bad.. (c) bash.im\\nI JUST WANNA DIE!!!", file=sys.stderr)
sys.exit(42)
'''
TEST_GOOD_BIN = '#!%s\n\n' % sys.executable + '''
from __future__ import print_function

import os
import sys
import time
import random
import pkg_resources

pkg_resources.require('skynet-heartbeat-server-service')
from ya.skynet.services.heartbeatserver.bulldozer.helper import Communicator

# Check that parent passes correct environment variables
assert os.getenv('DATABASE_URI') == 'DB_URI_CHECK'
assert os.getenv('APPLICATION_ROOT') == 'APP_ROOT_CHECK'

count = 0
c = Communicator().ready()
for host, _, data in c.read():
    data = data['report']
    type = data['type']
    if type == 'assert':
        assert False and 'Assert requested by the parent!'
    assert data['magic'] == 0xC001C0DE
    if type == 'discard':
        print('DEBUG: DISCARDING THE REPORT!', file=sys.stderr)
        c.discard('Report discarding requested.')
    elif type == 'sleep':
        time.sleep(data['amount'])
        c.ready()
    else:
        print('DEBUG: ON DATA FROM %r READ: %r' % (host, data), file=sys.stderr)
        # Sleep a bit to mix-up responses
        time.sleep(random.random() / 10)
        count += 1
        c.ready()
    if count > 2:
        print('Maximum number of requests (%d) exceeded. Exiting.' % count, file=sys.stderr)
        sys.exit(0)

'''

TEST_EMPTY_BIN = '#!%s\n\n' % sys.executable + '''
from __future__ import print_function

import pkg_resources

pkg_resources.require('skynet-heartbeat-server-service')
from ya.skynet.services.heartbeatserver.bulldozer.helper import Communicator

c = Communicator().ready()
for host, _, data in c.read():
    c.ready()
'''


def PLUGIN_CONFIG(tmpdir):
    binary = tmpdir.join('test.plugin')
    return binary, context.Context.Config({
        'bindir': tmpdir.strpath,
        'executable': binary.relto(tmpdir),
        'report_period': 3,  # Expected execution time after 'normal' check till rate assert.
        'database_uri': 'DB_URI_CHECK',
        'application_root': 'APP_ROOT_CHECK',
        'queue_data_limit': 1024,
        'queue_limits_check': 5,
        'processing_limit': 3,
        'forward': False,
        'aliases': [],
        'arguments': '',
        'instances': 3,
        'discard_restarts': 3,
        'fatal_restarts': 9,
        'restart_pause': 0.01,
        'greetings_wait': 1,
        'start_attempts': 1,
    })


@pytest.mark.new
def test_plugin_basic(tmpdir):
    print('')
    log = ctx.log.getChild('basic')
    binary, cfg = PLUGIN_CONFIG(tmpdir)

    log.info('Test plugin basic checks.')
    try:
        Supervisor('test', cfg, log.getChild('test.plugin'))
        assert False, 'This point should not be reached.'
    except Exception as ex:
        log.debug('Exception caught: %s', tb.format_exc())
        assert str(ex).index('not found') >= 0

    log.info('Test plugin abort on impossibility to execute a file.')
    binary.write(TEST_BAD_BIN)
    p = Supervisor('test', cfg, log.getChild('test.plugin'))
    p.start()
    # Take some time to plugin to try to start an executable.
    time.sleep(.2)
    try:
        p.status()
        assert False, 'This point should not be reached.'
    except Exception as ex:
        log.debug('Exception caught: %s', tb.format_exc())
        assert str(ex).index('instances are dead') >= 0
    p.stop()

    log.info('Test plugin abort on impossibility to start a file.')
    binary.chmod(0777)
    p.start()
    # Take some time to plugin to try to start an executable.
    time.sleep(.2)
    try:
        p.status()
        assert False, 'This point should not be reached.'
    except Exception as ex:
        log.debug('Exception caught: %s', tb.format_exc())
        assert str(ex).index('instances are dead') >= 0
    p.stop()

    log.info('Test plugin abort on impossibility to start a file v2.')
    binary.remove()
    binary.mksymlinkto('/bin/false')
    p.start()
    # Take some time to plugin to try to start an executable.
    time.sleep(.2)
    try:
        p.status()
        assert False, 'This point should not be reached.'
    except Exception as ex:
        assert str(ex).index('instances are dead') >= 0
    p.stop()

    log.info('Test plugin recovery on bad reports.')
    binary.remove()
    binary.write(TEST_GOOD_BIN)
    binary.chmod(0777)
    p.start()
    assert 'restart' in p.process('localhost', 'test', {'report': {'type': 'regular', 'magic': 0xDEADBEAF}})
    time.sleep(.1)
    assert p.status()['restarts'] == cfg.discard_restarts
    p.stop()

    MAGIC_SIZE = 3407858
    log.info('Check for "magic" data length of %d.' % MAGIC_SIZE)
    p.start()
    # I know the actual report tooks 49 bytes
    # (8 bytes for timestamp + len('localhost') + len('test') + len(msgpack.dumps(report)),
    # so, to perform the test, we will add the difference between the magic and this number.
    report = {'report': {'type': 'regular', 'magic': MAGIC, 'data': 'x'}}
    report['data'] = 'x' * (MAGIC_SIZE - len(msgpack.dumps((time.time(), 'localhost', 'test', report, ))) - 1)
    p.process('localhost', 'test', report)
    assert p.status()['restarts'] == 0
    p.stop()

    log.info('Test plugin normal functionality.')
    binary.remove()
    binary.write(TEST_GOOD_BIN)
    binary.chmod(0777)
    p.start()
    # Take some time to plugin to try to start an executable.
    p.process('localhost', 'test', {'report': {'type': 'regular', 'magic': MAGIC}})

    started = time.time()
    assert p.status()['instances'] == 3

    host = 'localhost'
    report = msgpack.packb({'type': 'regular', 'magic': MAGIC})

    def gtLoop():
        for i in range(9):
            p.process(host, 'test', {'report': report})
            assert p.status()['instances'] == 3

    # 3x more that 3x3 - each thread should restart its sub-process at least 2 times
    gevent.joinall([gevent.spawn(gtLoop) for _ in range(3)])

    log.info('Test compression.')
    p.process(host, 'test', {'report': report, 'compression': None})
    p.process(host, 'test', {'report': bz2.compress(report), 'compression': 'bz2'})
    p.process(host, 'test', {'report': zlib.compress(report), 'compression': 'zip'})

    log.info('Check report discarding.')
    for i in range(13):
        assert 'Discarding' in p.process(host, 'test', {'report': {'type': 'discard', 'magic': MAGIC}})

    log.info('Check status.')
    st = p.status()
    log.debug('Plugin statistics: %r', st)
    # We got some sleeps above, so the runner idle time should be greater than 0.1,
    # requests rate per second should be greater than 10
    rate = 44 / (time.time() - started)
    log.info('Expected requests rate %r', rate)
    assert st['busy'] < 0.5
    assert st['rate'] > rate / 2
    assert st['discards'] == 13
    assert st['requests'] == 44
    assert st['instances'] == 3
    p.stop()


@pytest.mark.new
def test_plugin_queue(tmpdir):
    print('')
    log = ctx.log.getChild('queue')

    binary, cfg = PLUGIN_CONFIG(tmpdir)
    binary.write(TEST_GOOD_BIN)
    binary.chmod(0777)

    cfg.instances = 1
    cfg.queue_limits_check = .25

    p = Supervisor('test', cfg, log.getChild('test.plugin'))
    p.start()
    # Take some time to plugin to try to start an executable.
    p.process('localhost', 'test', {'report': {'type': 'regular', 'magic': MAGIC}})

    log.debug('Check report expiration.')
    queue = gevent.queue.Queue()
    report = {'report': msgpack.packb({'type': 'sleep', 'amount': .1, 'magic': MAGIC})}

    expires = time.time() + .05
    for _ in range(5):
        p.process('localhost', 'test', report, expires, queue.put)

    discards = 0
    for _ in range(5):
        if queue.get():
            discards += 1

    assert discards == 4

    log.debug('Check queue size overcommit.')
    report = msgpack.dumps({'type': 'sleep', 'amount': 0, 'magic': MAGIC, 'data': '0' * 255})
    size = len(report)
    log.debug('Report size is %d, limit: %d.', size, cfg.queue_data_limit)

    # Block the bulldozer with some job, while the queue will be filled.
    # Also, set 'expires' field, because it should be placed on the top of the queue.
    p.process('localhost', 'sleep',
              {'report': {'type': 'sleep', 'amount': .5, 'magic': MAGIC}},
              time.time() + .05,
              callback=queue.put
    )
    # Take some time to the plugin to start processing of the report
    # (avoid possibility to drop the long-running report).
    time.sleep(.1)
    # Now fill the queue
    for _ in range(10):
        p.process('localhost', 'test', {'report': report}, callback=queue.put)

    log.debug('Queue filled. Awaiting for processing results...')
    discards = 0
    for _ in range(10):
        if queue.get():
            discards += 1

    assert discards == 10 - cfg.queue_data_limit / size

    p.stop()


@pytest.mark.new
def test_plugin_performance(tmpdir):
    print('')
    GREEN_THREADS = 3
    ITERATIONS = 2000
    log = ctx.log.getChild('perf')

    binary, cfg = PLUGIN_CONFIG(tmpdir)
    cfg.instances = GREEN_THREADS

    binary.write(TEST_EMPTY_BIN)
    binary.chmod(0777)

    report = {'report': msgpack.packb({'type': 'performance', 'data': None})}
    p = Supervisor('test', cfg, log.getChild('test.plugin'))
    p.start()
    # Take some time to plugin to try to start an executable.
    p.process('localhost', 'test', {'report': report})

    def worker():
        for i in range(ITERATIONS / GREEN_THREADS):
            p.process('localhost', 'test', report)

    log.info('Performance test. Running .')
    log.setLevel(logging.INFO)
    started = time.time()
    gevent.joinall([gevent.spawn(worker) for i in range(GREEN_THREADS)])
    dur = time.time() - started
    log.setLevel(logging.DEBUG)
    log.info('%d iterations done in %.5fs (%.2f iterations per second).', ITERATIONS, dur, ITERATIONS / dur)
    p.stop()
