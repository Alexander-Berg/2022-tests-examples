from __future__ import absolute_import

from py import std
from kernel.util.pytest import TestBrick
from kernel.util.functional import lazy

CLEAN_AFTER_EACH_RUN = True


class CopierProxy(object):
    _copier = None

    def __init__(self, supervisor):
        self._supervisor = supervisor

    def __getattr__(self, name):
        if self._copier is None:
            from api.copier import Copier
            self._copier = Copier()

        if not hasattr(self._copier, name):
            raise KeyError

        origMethod = getattr(self._copier, name)

        @std.functools.wraps(origMethod)
        def _proxy(*args, **kwargs):
            oldPortOffset = None
            try:
                oldPortOffset = self._supervisor.makeActive()
                return origMethod(*args, **kwargs)
            finally:
                if oldPortOffset is not None:
                    self._supervisor.makeActive(oldPortOffset)

        return _proxy


class MockSupervisor(object):
    def __init__(self, path, portOffset):
        self.path = path
        self.portOffset = portOffset
        self.copier = CopierProxy(self)

    def __repr__(self):
        return '<MockSupervisor %d>' % self.portOffset

    def startCopier(self):
        return std.subprocess.Popen(
            [std.sys.executable, 'startup/up.py', 'start', 'copier', 'logger'],
            stdout=std.subprocess.PIPE, stderr=std.subprocess.STDOUT,
            cwd='../..',
            env=self.generateEnv()
        )

    def stop(self):
        return std.subprocess.Popen(
            [std.sys.executable, 'startup/up.py', 'stop'],
            stdout=std.subprocess.PIPE, stderr=std.subprocess.STDOUT,
            cwd='../..',
            env=self.generateEnv()
        )

    def generateEnv(self):
        return {
            'PATH': std.os.environ.get('PATH', ''),
            'PYTHONPATH': self.path.join('supervisor', 'skynet').strpath,
            'SKYNET_PORTOFFSET': str(self.portOffset),
            'SKYNET_FORCE_PORTOFFSET': '1',
        }

    def makeActive(self, portOffset=None):
        from api.config import Config
        oldPortOffset = portOffset or self.portOffset
        Config.portOffset = self.portOffset
        return oldPortOffset

    def getStats(self):
        from copier.rbtorrent.utils import Pyro
        from copier.rbtorrent import cfg

        try:
            oldPortOffset = self.makeActive()
            proxy = Pyro.Proxy('PYRO:SkynetCopier@localhost:%d' % cfg.port())
            return proxy.maintenanceGetStats()
        finally:
            self.makeActive(oldPortOffset)

    def getStatus(self, *keys):
        cmd = ['result = r = {}']
        if 'torrentsCount' in keys:
            cmd.append('r["torrentsCount"] = len(self.torrentmanager.torrents)')
        if 'plainDataCount' in keys:
            cmd.append('r["plainDataCount"] = len(self.plaindatamanager.items)')

        return self.execute('\n'.join(cmd))

    def setUploadSpeed(self, speed):
        self.execute('self.session.set_upload_rate_limit(%d)' % speed)

    def setDownloadSpeed(self, speed):
        self.execute('self.session.set_download_rate_limit(%d)' % speed)

    def execute(self, code):
        from ya.skynet.services.copier.rbtorrent.utils import Pyro
        from ya.skynet.services.copier.rbtorrent import cfg
        try:
            if code[0] == '\n':
                strip = len(code[1:]) - len(code[1:].lstrip())
                lines = code[1:].split('\n')
                result = []
                for line in lines:
                    result.append(line[strip:])
                code = '\n'.join(result)

            oldPortOffset = self.makeActive()
            proxy = Pyro.Proxy('PYRO:SkynetCopier@localhost:%d' % cfg.port())
            return proxy.maintenanceRun(code)
        finally:
            self.makeActive(oldPortOffset)

    def checkErrors(self):
        data = self.path.join('supervisor', 'logger', 'logfile').open(mode='r').read()
        hostname = std.socket.gethostname()
        errors = []

        ignore = (
            '( 2) : No external address detected!',
            '( 4) : Got CommunicationError',
            '( 3) : We have colliding torrent, but check() is okay!',
            'appear in weakref dict, but not in self.torrents!',
            '( 2) : Detected external IP address',
            '( 2) : Fastbone detection failed, error was',
        )

        for line in data.split('\n'):
            line = line.rstrip()
            if hostname in line:
                if '( 1)' in line or '( 2)' in line or '( 3)' in line or '( 4)' in line:
                    # Ignore something obvious errors:
                    if any((ign in line for ign in ignore)):
                        continue
                    errors.append(line)
            elif not 'up.py' in line:
                if errors:
                    errors[-1] += '\n' + line

        if errors:
            print >> std.sys.stderr, '\n%r:\n%s' % (self, '\n'.join(errors))

    def runWatchDog(self, params='forceAll=True'):
        try:
            oldPortOffset = self.makeActive()
            return self.execute('result = self.watchdog.tick(%s)' % params)
        finally:
            self.makeActive(oldPortOffset)


class Supervisors(TestBrick):
    scope = 'session'

    def options(self, parser):
        group = parser.getgroup('skynet.supervisors')
        group.addoption('--ignore-log', dest='ignoreLogErrors', default=False, action='store_true',
            help='ignore errors from log files'
        )

    def setUp(self, request):
        tmpdir = request.getfixturevalue('tmpdir').dirpath()
        supervisors = []
        procs = []

        #supervisors = [
        #    MockSupervisor('/home/mocksoul/workspace/yandex/repos/skynet/trunk/startup', 15000),
        #    MockSupervisor('/home/mocksoul/workspace/yandex/repos/skynet/trunk2/startup', 0),
        #    None
        #]

        #return supervisors

        for i in range(3):
            supervisorPath = tmpdir.join('supervisor%d' % i)

            portOffset = int('1%d000' % i)

            proc = std.subprocess.Popen(
                [std.sys.executable, 'startup/dev.py', supervisorPath.strpath],
                stdout=std.subprocess.PIPE, stderr=std.subprocess.STDOUT,
                cwd='../..'
            )
            proc.wait()
            assert proc.returncode == 0, proc.stdout.read()

            mockSupervisor = MockSupervisor(supervisorPath, portOffset)
            proc = mockSupervisor.startCopier()

            supervisors.append(mockSupervisor)

            procs.append(proc)

        for proc in procs:
            proc.wait()
            assert proc.returncode == 0, proc.stdout.read()

        return supervisors

    def tearDown(self, request, supervisors):
        procs = []

        try:
            for mockSupervisor in supervisors:
                torrentsCount = mockSupervisor.execute('result = len(self.torrentmanager.torrents)')
                torrentsByTargetCount = mockSupervisor.execute('result = len(self.torrentmanager.torrents._byTarget)')

                assert torrentsCount == torrentsByTargetCount

                if not CLEAN_AFTER_EACH_RUN:
                    for dataPath in mockSupervisor.execute(
                        'result = [t.dataPath for t in self.torrentmanager.torrents.values()]'
                    ):
                        dataPath.remove()

                if not request.config.option.ignoreLogErrors:
                    mockSupervisor.runWatchDog()

                    torrentsCount = mockSupervisor.execute('result = len(self.torrentmanager.torrents)')
                    torrentsByTargetCount = mockSupervisor.execute(
                        'result = len(self.torrentmanager.torrents._byTarget)'
                    )

                    assert torrentsCount == 0, '%r has %d torrents, but should be 0' % (mockSupervisor, torrentsCount)
                    assert torrentsByTargetCount == 0, \
                           '%r has %d torrents by target, but should be 0' % (mockSupervisor, torrentsByTargetCount)
        finally:
            for sup in supervisors:
                procs.append(sup.stop())

            for proc in procs:
                proc.wait()
                assert proc.returncode == 0, proc.stdout.read()

            if not request.config.option.ignoreLogErrors:
                for mockSupervisor in supervisors:
                    mockSupervisor.checkErrors()
