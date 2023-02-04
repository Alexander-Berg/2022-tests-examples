import time
import sys
import os
import threading
import signal
from kernel.util.pytest import TestBrick
from kernel.util.functional import lazy
import random

import subprocess
import functools
from threading import Thread
from Queue import Queue, Empty as QueueEmpty

from cStringIO import StringIO

#from skybone.rbtorrent.utils import PyroProxy


class Copier(TestBrick):
    scope = 'session'

    def setUp(self, request):
        from api.copier import Copier, ICopier
        return lazy(Copier, ICopier)()


class CopierProxy(object):
    def __init__(self):
        port = 0

class CopierDaemon(object):
    def __init__(self):
        self.shouldStop = False

    def setUp(self):
        return self

    def tearDown(self, *a):
        print a

    def stop(self):
        self._daemon.stop()


    def start(self):
        from ya.skynet.services.copier.rbtorrent.daemon import CopierDaemon

        os.umask(022)

        self._daemon = CopierDaemon(12345, 23456)
        self._daemonThread = threading.Thread(target=self._daemon.start)
        self._daemonThread.start()


class CopierApiProxy(object):
    _copier = None
    _instance = None

    def __init__(self, instance):
        self._instance = instance

    def __getattr__(self, name):
        if self._copier is None:
            from api.copier import Copier
            self._copier = Copier()

        if not hasattr(self._copier, name):
            raise KeyError

        origMethod = getattr(self._copier, name)

        @functools.wraps(origMethod)
        def _proxy(*args, **kwargs):
            from api.config import Config
            port = __import__('yaml').load(open(self._instance.config, 'rb'))['rpc_port']
            Config.portOffset = port - 10018
            return origMethod(*args, **kwargs)

        return _proxy


class CopierInstance(object):
    def __init__(self, binary, config, workdir):
        self.binary = binary
        self.config = config
        self.stdoutLines = Queue()
        self.workdir = workdir

    def procWatcher(self):
        while True:
            line = self.proc.stdout.readline()
            if not line:
                break
            self.stdoutLines.put(line)

    def run(self):
        newenv = os.environ.copy()
        newenv['SKYNET_LOG_STDOUT'] = '1'

        self.proc = subprocess.Popen(
            [
                sys.executable, self.binary,
                '-c', str(self.config),
                '-d', str(self.workdir)
            ],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env=newenv,
            preexec_fn=os.setpgrp
        )

        self.watcherThread = Thread(target=self.procWatcher)
        self.watcherThread.start()

        self.api = CopierApiProxy(self)

        port = __import__('yaml').load(open(self.config, 'rb'))['rpc_port']
        proxy = PyroProxy('PYRO:SkynetCopier@localhost:%d' % port)
        try:
            assert proxy.ping(waitActive=15)
        except:
            self.proc.send_signal(signal.SIGTERM)
            while True:
                line = self.stdoutLines.get(timeout=20)
                print line,
                if not line:
                    break
            raise

        return self

    def stop(self, final=False):
        if self.proc:
            self.proc.send_signal(signal.SIGINT)
            self.proc.wait()
            self.watcherThread.join(timeout=5)
            if final:
                self.stdoutLines.put('')
        self.proc = None

    def execute(self, code):
        from ya.skynet.services.copier.rbtorrent.utils import Pyro
        from ya.skynet.services.copier.rbtorrent import cfg
        if code[0] == '\n':
            strip = len(code[1:]) - len(code[1:].lstrip())
            lines = code[1:].split('\n')
            result = []
            for line in lines:
                result.append(line[strip:])
            code = '\n'.join(result)

        port = __import__('yaml').load(open(self.config, 'rb'))['rpc_port']
        proxy = PyroProxy('PYRO:SkynetCopier@localhost:%d' % port)
        return proxy.maintenanceRun(code)

    def runWatchDog(self, params='forceAll=True', expect=None):
        result = int(self.execute('result = self.watchdog.tick(%s)' % params))
        if expect is not None:
            assert result == expect, 'Expected to remove during watchdog: %d, really removed %d' % (expect, result)
        return result

    def stats(self):
        data = self.execute('''
            result = {
                'torrentsCnt': len(self.torrentmanager.torrents),
                'plaindataCnt': len(self.plaindatamanager.items)
            }
        ''')

        return data

    def resourceStats(self, resid, waitload=10):
        if ':' in resid:
            resid = resid.split(':', 1)[1]
        torrents, watchers = self.execute('''
            metatorrent = self.torrentmanager.torrents[{resid!r}]
            if {waitload!r}:
                metatorrent.evtLoaded.wait(timeout={waitload})
            torrents = [(infoHash, self.torrentmanager.torrents[infoHash].dataPath) for infoHash in metatorrent.torrents]
            result = torrents, metatorrent.watchers
        '''.format(**locals()))

        stats = {'torrents': {}, 'watchers': watchers}
        for infoHash, dataPath in torrents:
            stats['torrents'][dataPath] = self.torrentStats(infoHash)

        return stats

    def torrentStats(self, infoHash):
        return self.execute('''
            result = self.torrentmanager.torrents[{infoHash!r}].getStatus()
        '''.format(**locals()))


    def setSpeed(self, dl=None, up=None, silent=False):
        self.execute('''
            if {dl!r} is not None:
                self.session.set_download_rate_limit({dl})
                if not {silent!r}:
                    log.normal('Set download rate limit to %r' % ({dl}, ))
            if {up!r} is not None:
                self.session.set_upload_rate_limit({up})
                if not {silent!r}:
                    log.normal('Set upload rate limit to %r' % ({up}, ))
        '''.format(**locals()))

    def testEnter(self):
        self.setSpeed(1024*1024*64, 1024*1024*64, silent=True)
        self.runWatchDog()

    def testLeave(self):
        pass


class CopierApiFactoryController(object):
    def __init__(self, instances):
        self.instances = instances
        self.marker = '--ApiFactoryControllerMarker: %s' % random.randint(10000, 99999)

    def addMarker(self, marker):
        for i in self.instances:
            if i.proc is None:
                continue

            port = __import__('yaml').load(open(i.config, 'rb'))['rpc_port']
            PyroProxy('PYRO:SkynetCopier@localhost:%d' % port).maintenanceRun(
                'print "%s"; std.sys.stdout.flush()' % marker
            )

    def __enter__(self):
        [i.testEnter() for i in self.instances]

        self.addMarker(self.marker + ': enter')
        return self.instances if len(self.instances) > 1 else self.instances[0]

    def __exit__(self, excType, excValue, excTraceback):
        [i.testLeave() for i in self.instances]

        self.addMarker(self.marker + ': exit')

        for i in self.instances:
            while True:
                line = i.stdoutLines.get(timeout=20).rstrip('\n')
                if not line or line == self.marker + ': enter':
                    break

            if not line:
                raise Exception('wtf?!')

            while True:
                try:
                    line = i.stdoutLines.get(timeout=1).rstrip('\n')
                except QueueEmpty:
                    if i.proc is None:
                        break
                    continue

                if not line or line == self.marker + ': exit':
                    break

                if excType is not None:
                    port = __import__('yaml').load(open(i.config, 'rb'))['rpc_port']
                    print port, line

            if excType is None:
                i.execute('[self.torrentmanager.remove(h) for h in list(self.torrentmanager.torrents)]')


class CopierApiFactory(object):
    instances = []

    def __init__(self, request, tmpdir):
        self.request = request
        self.tmpdir = tmpdir

    def setUp(self):
        del self.instances[:]
        return self

    def tearDown(self, fake):
        [i.stop(final=True) for i in self.instances]


    def make(self, count):
        import yaml

        for i in range(count - len(self.instances)):
            port = 20000 + len(self.instances)
            dataPort = 20000 + 6880 + len(self.instances)

            cfg = yaml.load(open('snapshot/etc/config.yaml', 'rb'))
            cfg['rpc_port'] = port
            cfg['data_port'] = dataPort
            cfgpath = self.tmpdir.dirpath().join('copier%dconfig.yaml' % (len(self.instances), ))
            yaml.dump(
                cfg,
                cfgpath.open(mode='wb'),
                default_flow_style=False
            )
            self.instances.append(
                CopierInstance(
                    binary='snapshot/bin/copier',
                    config=cfgpath.strpath,
                    workdir=self.tmpdir.dirpath().join('copier%d' % len(self.instances))
                ).run()
            )

        return CopierApiFactoryController(self.instances[:count])




def pytest_funcarg__copierApiFactory(request):
    factory = CopierApiFactory(request, request.getfixturevalue('tmpdir'))

    return request.cached_setup(
        setup=factory.setUp,
        teardown=factory.tearDown,
        scope='session'
    )
