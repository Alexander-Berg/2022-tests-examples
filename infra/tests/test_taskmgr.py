import tempfile
import os
import shutil

from ya.skynet.util.unittest import TestCase, main
from ya.skynet.util import logging
from ya.skynet.util.pickle import loads

from ya.skynet.services.cqudp.client import task, task_options
from ya.skynet.services.cqudp.server.taskmgr import TaskManager
from ya.skynet.services.cqudp.server import executer
from ya.skynet.services.cqudp.utils import genuuid, getUserName
from ya.skynet.services.cqudp import cfg

from porto.exceptions import InvalidValue


class Executer(executer.Executer):
    name = 'porto'

    def __init__(self, *args, **kwargs):
        super(Executer, self).__init__(lock=None, *args, **kwargs)
        self.requests = []

    def execute(self, uuid, *args, **kwargs):
        self.taskmgr.tasks[uuid][-1].update(start_time=42)
        self.requests.append((uuid, args, kwargs))


class FailingExecuter(Executer):
    def execute(self, uuid, *args, **kwargs):
        raise InvalidValue("oh, porto is dead")


class StoppableExecuter(executer.Executer):
    name = 'porto'

    def __init__(self, *args, **kwargs):
        super(StoppableExecuter, self).__init__(lock=None, *args, **kwargs)

    class Job(executer.Job):
        def __init__(self, taskid):
            super(StoppableExecuter.Job, self).__init__(taskid)
            self.stopped = False

        def terminate(self):
            self.stopped = True

        def _wait_process(self):
            return 0

    def execute(self, uuid, *args, **kwargs):
        j = {'start_time': 42, 'job': StoppableExecuter.Job(uuid)}
        self.taskmgr.tasks[uuid][-1].update(j)
        return j['job']


class TestTaskmgr(TestCase):
    def setUp(self):
        super(TestTaskmgr, self).setUp()

        logging.getLogger().setLevel(logging.INFO)

        self.old_temp_dir = tempfile.gettempdir()
        tempfile.tempdir = tempfile.mkdtemp()
        os.chmod(tempfile.tempdir, 0o777)
        for dirname in (
            os.path.join(tempfile.gettempdir(), 'tasks'),
            # os.path.join(tempfile.gettempdir(), 'client'),
        ):
            os.makedirs(dirname)
            os.chmod(dirname, 0o777)

    def tearDown(self):
        super(TestTaskmgr, self).tearDown()

        shutil.rmtree(tempfile.gettempdir())
        tempfile.tempdir = self.old_temp_dir

    def testExecuteTask(self):
        self.execute(task_ctor=create_task)

    def testExecuteDict(self):
        self.execute(task_ctor=create_dict)

    def execute(self, task_ctor):
        routes = []

        def route(*args, **kwargs):
            routes.append((args, kwargs))

        mgr = TaskManager(executers=[Executer()])
        mgr.executers[0].taskmgr = mgr

        mgr.verify = True
        t = task_ctor()
        self.assertFalse(mgr.execute(route, None, t, [], [()], None, None))

        mgr.verify = False
        t = task_ctor()
        self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
        self.assertFalse(mgr.execute(route, None, t, [], [()], None, None))

        mgr.verify = True
        mgr.auth = None
        t = task_ctor()
        self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
        self.assertFalse(mgr.execute(route, None, t, [], [()], None, None))

        mgr.stop_task(t['uuid'], None)
        self.assertFalse(mgr.execute(route, None, t, [], [()], None, None))

        t = task_ctor()
        mgr.stop_task(t['uuid'], None)
        self.assertFalse(mgr.execute(route, None, t, [], [()], None, None))

    def testTaskStop(self):
        routes = []

        def route(*args, **kwargs):
            routes.append((args, kwargs))

        mgr = TaskManager(executers=[StoppableExecuter()])
        mgr.executers[0].taskmgr = mgr

        mgr.verify = False
        t = task(None, task_options(None))
        self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
        self.assertFalse(mgr.tasks[t['uuid']][-1]['job'].stopped)
        mgr.stop_task(t['uuid'], None)
        self.assertTrue(mgr.tasks[t['uuid']][-1]['job'].stopped)

    def testMultiTask(self):
        routes = []

        def route(*args, **kwargs):
            routes.append((args, kwargs))

        mgr = TaskManager(executers=[StoppableExecuter()])
        mgr.executers[0].taskmgr = mgr
        mgr.verify = False

        t = create_task()
        args = {
            1: ('cont1', None),
            2: ('cont2', None),
            3: ('cont3', None),
        }
        self.assertTrue(mgr.container_execute(route, None, t, args, [()], [1, 2, 3], None))
        self.assertEqual(len(args), len(mgr.tasks[t['uuid']]))
        self.assertTrue(all(not task['job'].stopped for task in mgr.tasks[t['uuid']]))
        mgr.stop_task(t['uuid'], None)
        self.assertTrue(all(task['job'].stopped for task in mgr.tasks[t['uuid']]))

    def testDisablePorto(self):
        oldVal = cfg.server.AllowExitContainer
        oldVal2 = cfg.server.Procman.UsePorto

        try:
            cfg.server.AllowExitContainer = True
            cfg.server.Procman.UsePorto = True

            routes = []

            def route(*args, **kwargs):
                routes.append((args, kwargs))

            mgr = TaskManager(executers=[Executer()])
            mgr.executers[0].taskmgr = mgr
            mgr.verify = False

            opts = task_options(None)

            t = task(None, opts)
            self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
            self.assertNotIn('porto', mgr.executers[0].requests[-1][2])

            mgr.executers[0].name = 'procman'

            t = task(None, opts)
            self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
            self.assertIs(mgr.executers[0].requests[-1][2].get('porto', True), True)

            opts.update(no_porto=False)
            t = task(None, opts)
            self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
            self.assertIs(mgr.executers[0].requests[-1][2].get('porto', True), True)

            opts.update(no_porto=True)
            t = task(None, opts)
            self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
            self.assertIs(mgr.executers[0].requests[-1][2]['porto'], False)

            cfg.server.AllowExitContainer = False
            t = task(None, opts)
            mgr.execute(route, None, t, [], [()], None, None)
            msg = loads(routes[-1][0][0]['result'])
            self.assertIsInstance(msg[1], RuntimeError)

        finally:
            cfg.server.AllowExitContainer = oldVal
            cfg.server.Procman.UsePorto = oldVal2

    def testSwitchExecuterOnFailure(self):
        oldVal = cfg.server.AllowExitContainer
        oldVal2 = cfg.server.Procman.UsePorto

        try:
            cfg.server.AllowExitContainer = True
            cfg.server.Procman.UsePorto = True

            routes = []

            def route(*args, **kwargs):
                routes.append((args, kwargs))

            mgr = TaskManager(executers=[Executer(), Executer()])
            mgr.executers[0].taskmgr = mgr
            mgr.executers[1].taskmgr = mgr
            mgr.verify = False

            opts = task_options(None)

            t = task(None, opts)
            self.assertFalse(mgr.executers[0].requests)
            self.assertFalse(mgr.executers[1].requests)
            self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
            self.assertTrue(mgr.executers[0].requests)
            self.assertFalse(mgr.executers[1].requests)

            mgr.executers[0] = FailingExecuter()
            mgr.executers[0].taskmgr = mgr

            t = task(None, opts)
            self.assertFalse(mgr.executers[0].requests)
            self.assertFalse(mgr.executers[1].requests)
            self.assertTrue(mgr.execute(route, None, t, [], [()], None, None))
            self.assertFalse(mgr.executers[0].requests)
            self.assertTrue(mgr.executers[1].requests)

        finally:
            cfg.server.AllowExitContainer = oldVal
            cfg.server.Procman.UsePorto = oldVal2


def create_task():
    return task(b'', task_options(None))


def create_dict():
    return {
        'uuid': genuuid(),
        'acc_user': 'nobody',
        'acc_host': 'nowhere',
        'options': {'user': getUserName()},
        'data': b'',
        'ctime': '',
        'signs': [],
    }


if __name__ == '__main__':
    main()
