import gevent
import pytest
import logging

from skybone.rbtorrent.diskio.cache import PathCache


class FakeClient(object):
    class FakeWaiter(object):
        def __init__(self, res):
            self.res = res

        def wait(self):
            return self.res

    def __init__(self):
        self.paths_by_checksum = {}
        self.bads = {}

    def call(self, name, *args, **kwargs):
        assert name in ('io_get_paths_by_checksum', 'io_set_bad_paths')
        return self.FakeWaiter(getattr(self, name)(*args, **kwargs))

    def io_get_paths_by_checksum(self, md5):
        return self.paths_by_checksum.get(md5, [])

    def io_set_bad_paths(self, md5, paths):
        self.bads.setdefault(md5, {}).update(paths)

        for fn, (stored_mtime, stored_chktime, mtime) in paths.items():
            cksum_paths = self.paths_by_checksum.get(md5, [])

            for idx, (ofn, omtime, ochktime) in reversed(list(enumerate(cksum_paths))):
                if ofn == fn and omtime == stored_mtime:
                    cksum_paths.pop(idx)


@pytest.fixture(scope='function')
def master_cli():
    return FakeClient()


@pytest.fixture(scope='function')
def pc(request, master_cli):
    obj = PathCache(master_cli)
    obj.start()

    def _fin():
        logging.getLogger('pcache').addHandler(logging.NullHandler())
        obj.stop()

    request.addfinalizer(_fin)
    return obj


def test_md5lock(pc):
    assert not pc.cache and not pc.bads

    with pc.md5lock('xx') as (paths, ttl):
        assert 'xx' in pc.cache
        assert ttl == 0

    assert not pc.cache


def test_md5lock_many(pc):
    def _chk():
        assert 'xx' not in pc.cache
        with pc.md5lock('xx'):
            assert 'xx' in pc.cache
            gevent.sleep()
        assert 'xx' in pc.cache

    grn = gevent.spawn(_chk)
    gevent.sleep()

    # Right now md5lock should be locked by _chk
    assert 'xx' in pc.cache

    with pc.md5lock('xx'):
        assert 'xx' in pc.cache
        assert grn.ready()

    # Since this is final unlocker and no waiters anymore -- cleanup should occur
    assert 'xx' not in pc.cache


def test_get_paths(pc):
    assert pc.get_paths('xx') == set()
    pc.master_cli.paths_by_checksum['xx'] = [('/some', 1, 2)]
    assert pc.stats['miss'] == 1

    assert pc.get_paths('xx') == set(['/some'])
    assert pc.stats['miss'] == 2

    pc.master_cli.paths_by_checksum.pop('xx')
    assert pc.get_paths('xx') == set(['/some'])
    assert pc.stats['miss'] == 2
    assert pc.stats['hit'] == 1

    pc.cache['xx'][2] = 0
    assert pc.get_paths('xx') == set()
    assert pc.stats['miss'] == 3
    assert pc.stats['hit'] == 1


def test_bad_file(pc):
    pc.master_cli.paths_by_checksum['xx'] = [('/some', 1, 2), ('/other', 3, 4), ('/third', 5, 6)]
    assert pc.get_paths('xx') == set(['/some', '/other', '/third'])

    # Bad file with different mtime should ban it
    pc.bad_file('/other', 'xx', 7)
    assert pc.get_paths('xx') == set(['/some', '/third'])
    assert pc.bads['xx']['/other'] == (3, 4, 7)

    pc.bad_file('/some', 'xx', None)  # None means file is not reachable by us anymore
    assert pc.get_paths('xx') == set(['/third'])
    assert pc.bads['xx']['/some'] == (1, 2, None)

    # Second call to bad file should do nothing
    # And actually should not occur ever =)
    pc.bad_file('/some', 'xx', 1)
    assert pc.get_paths('xx') == set(['/third'])
    assert pc.bads['xx']['/some'] == (1, 2, None)

    pc.bad_file('/third', 'xx', None)
    assert not pc.cache


def test_set_bad_paths_on_lookup(pc):
    pc.master_cli.paths_by_checksum['xx'] = [('/some', 1, 2)]
    assert pc.get_paths('xx') == set(['/some'])

    pc.bad_file('/some', 'xx', None)

    assert not pc.master_cli.bads
    assert pc.get_paths('xx') == set()
    assert 'xx' in pc.master_cli.bads
    assert '/some' in pc.master_cli.bads['xx']

    pc.master_cli.paths_by_checksum['xx'] = [('/other', 3, 4)]
    assert pc.get_paths('xx') == set(['/other'])

    pc.bad_file('/other', 'xx', 11)

    assert '/other' not in pc.master_cli.bads['xx']
    gevent.sleep()
    assert pc.master_cli.bads['xx']['/other'] == (3, 4, 11)

    gevent.sleep()
