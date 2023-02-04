from common import CQTest, UseNetlibus
import sys
import socket
import gevent
import gevent.select

from ya.skynet.util import pickle
from ya.skynet.util.unittest import main, skipIf
from ya.skynet.util.sys.user import getUserName
from ya.skynet.library.auth.sign import SignManager
from ya.skynet.library.auth.tempkey import TempKey
from ya.skynet.services.cqudp.client.daemon import Daemon
from ya.skynet.services.cqudp import eggs, msgpackutils as msgpack
from ya.skynet.services.cqudp.utils import genuuid, AsyncResult


def run_task(daemon, dst, res, signer):
    description = str(daemon_task)
    task = pickle.dumps(daemon_task)
    egg = eggs.create_egg(['compat', 'common', __name__])
    task = msgpack.dumps((task, egg))

    options = dict(
        user=getUserName(),
        aggregate=True,
        pipeline=True,
        netlibus=True,
        objdumped=True,
    )

    uuid = genuuid()

    session = daemon.execute(
        uuid=uuid,
        task=task,
        description=description,
        hosts=dst,
        options=options,
        signer=signer,
        iter=False,
    )

    try:
        res.set(list(daemon.take_incoming([session], block=True, timeout=5.0)))
    except BaseException as e:
        res.set_exception(e)


@skipIf(getattr(sys, 'is_standalone_binary', False), 'not applicable to arcadia')
class TestClientDaemon(UseNetlibus, CQTest):
    def test_daemon_session(self):
        self.server.taskmgr.verify = False
        signer = SignManager()

        daemon = Daemon("\0unittest")
        res = AsyncResult(select_fun=gevent.select.select)

        try:
            job = gevent.spawn(daemon.serve_forever)
            with TempKey(signer, self.server.taskmgr.auth):
                dst = (socket.getfqdn(), self.port)
                t = gevent.spawn(run_task, daemon, {42: dst}, res, signer)
                results = res.get(timeout=5.0)

                res = results[0]
                self.assertEqual(res[1][0], 42)  # we mustn't expect that dst remains same, but cat expect same hostid
                self.assertEqual(res[2], 'result')
                r, e = pickle.loads(res[3])
                self.assertEqual(r, daemon_task())
                self.assertIsNone(e)

        finally:
            daemon.stop_server()
            daemon.running = False
            t.join()
            job.join()


def daemon_task():
    return 1


if __name__ == '__main__':
    main()
