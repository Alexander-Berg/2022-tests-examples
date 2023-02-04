import time
from unittest import main
import pytest
import gevent
import os

from procman.server import Handler

try:
    from porto import Connection
    conn = Connection()
    conn.connect()
except:
    conn = None


@pytest.mark.skipif(conn is None, reason="Porto is not available")
class TestPorto(object):
    def test_run(self, tmpdir):
        h = Handler(tmpdir)
        uuid = h.create(
            None,
            ['/bin/sleep', '1000'],
            tags=['test_porto'],
            keeprunning=False,
            liner=True,
            porto=True,
        )
        assert h.is_valid(uuid)

        stat = h.stat(uuid)
        good_stat = dict(
            uuid=uuid,
            stream=[],
            tags=['test_porto'],
            args=['/bin/sleep', '1000'],
            pid=None,
            cwd='/',
            start=time.time(),
            stoptime=None,
            retcodes=[])

        for k, v in good_stat.iteritems():
            if k == 'start':
                assert v > stat[k]
            else:
                assert stat[k] == v

        gevent.sleep(0.1)
        assert h.running(uuid)
        assert h.executing(uuid)
        porto_job = conn.Find('procman-{}'.format(uuid))
        assert porto_job.GetData('state') == 'running'

        h.kill(uuid)
        gevent.sleep(0.1)

        assert not h.running(uuid)
        with pytest.raises(Exception):
            conn.Find('procman-{}'.format(uuid))

    def test_subcontainer(self, tmpdir):
        h = Handler(tmpdir)

        root_name = 'pytest-{}'.format(os.urandom(4).encode('hex'))
        root_cont = conn.Create(root_name)
        root_cont.SetProperty('isolate', False)

        uuid = h.create(
            None,
            ['/bin/sleep', '1000'],
            tags=['test_porto'],
            keeprunning=False,
            liner=True,
            porto=True,
            root_container=root_name,
        )
        assert h.is_valid(uuid)

        stat = h.stat(uuid)
        good_stat = dict(
            uuid=uuid,
            stream=[],
            tags=['test_porto'],
            args=['/bin/sleep', '1000'],
            pid=None,
            cwd='/',
            start=time.time(),
            stoptime=None,
            retcodes=[])

        for k, v in good_stat.iteritems():
            if k == 'start':
                assert v > stat[k]
            else:
                assert stat[k] == v

        gevent.sleep(0.1)
        assert h.running(uuid)
        assert h.executing(uuid)
        porto_job = conn.Find('{}/procman-{}'.format(root_name, uuid))
        assert porto_job.GetData('state') == 'running'

        h.kill(uuid)
        gevent.sleep(0.1)

        assert not h.running(uuid)
        with pytest.raises(Exception):
            conn.Find('{}/procman-{}'.format(root_name, uuid))
        conn.Destroy(root_cont)


if __name__ == '__main__':
    main()
