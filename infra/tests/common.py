import gc
import logging
import tempfile
import os
import shutil
import threading

from compat import patch_modules

patch_modules()

from ya.skynet.util.unittest import TestCase, skipIf

from ya.skynet.library.auth.sign import FileKeysSignManager

from ya.skynet.services.cqudp.client import CqueueClient
from ya.skynet.services.cqudp.server import Server as CServer
from ya.skynet.services.cqudp.server.auth import create_auth
from ya.skynet.services.cqudp.server.taskmgr import TaskManager
from ya.skynet.services.cqudp.utils import run_daemon
from ya.skynet.services.cqudp.main import __file__ as task_path
from ya.skynet.services.cqudp import cfg

try:
    import netlibus  # noqa
    has_netlibus = True
except ImportError:
    has_netlibus = False


def Client(*args, **kwargs):
    c = CqueueClient(*args, **kwargs)
    c.set_signer(FileKeysSignManager("/nonexistent", "/nonexistent"))  # clean fksm, without keys
    return c


privileges_lock = threading.RLock()


def Server(ip=None, impl=None):
    taskmgr = TaskManager(privileges_lock=privileges_lock, auth=create_auth())
    taskmgr.taskpath = task_path
    taskmgr.tasks_start_timeout = 1.0
    server = CServer(ip, 0, impl=impl, taskmgr=taskmgr)
    run_daemon(server.run)
    return server.protocol.listenaddr()[1], server


class CQTest(TestCase):
    server_transport = 'msgpack'
    loglevel = os.getenv('TESTLOGGING', 'INFO')
    client_args = {'netlibus': False, 'msgpack': True, 'loglevel': loglevel}
    no_server = False

    def setUp(self):
        super(CQTest, self).setUp()

        logging.getLogger().setLevel(self.loglevel)

        self.stop_message_attempts, cfg.client.StopMessage.Attempts = cfg.client.StopMessage.Attempts, 1
        self.stop_message_interval, cfg.client.StopMessage.Interval = cfg.client.StopMessage.Interval, 0.1

        self.old_temp_dir = tempfile.gettempdir()
        tempfile.tempdir = tempfile.mkdtemp()
        os.chmod(tempfile.tempdir, 0o777)
        for dirname in (
            os.path.join(tempfile.gettempdir(), 'tasks'),
            # os.path.join(tempfile.gettempdir(), 'client'),
        ):
            os.makedirs(dirname)
            os.chmod(dirname, 0o777)

        if not self.no_server:
            self.port, self.server = Server(impl=self.server_transport)

    def tearDown(self):
        if not self.no_server:
            self.server.shutdown()
            del self.server
        super(CQTest, self).tearDown()

        cfg.client.StopMessage.Attempts = self.stop_message_attempts
        cfg.client.StopMessage.Interval = self.stop_message_interval

        shutil.rmtree(tempfile.gettempdir())
        tempfile.tempdir = self.old_temp_dir

        gc.collect()

    def create_client(self, **kwargs):
        kwargs.update(self.client_args)
        return Client(**kwargs)

    def create_server(self, *args):
        return Server(*args, impl=self.server_transport)


@skipIf(not has_netlibus, 'No netlibus available')
class UseNetlibus(object):
    server_transport = 'netlibus'
    client_args = {'netlibus': True, 'msgpack': False}
