import logging
import gevent

from api.copier.errors import FilesystemError

from skybone.rbtorrent.skbn.dl import Download
from skybone.rpc.server import RPC, Server as RPCServer


def test_full_dl(tmpdir):
    log = logging.getLogger('test_full_dl')
    dl = Download()

    rpc = RPC(log)
    rpc_server = RPCServer(log, backlog=10, max_conns=10, unix='\0test_full_dl')
    rpc_server.register_connection_handler(rpc.get_connection_handler())
    rpc_server.start()

    class Args(object):
        extra = {}
        dest = tmpdir.strpath
        master_uds = '\0test_full_dl'

    class Progress(object):
        def handle(self, *args, **kwargs):
            pass


    dl.progress = Progress()

    grn = gevent.spawn(dl.real_main, Args, log.getChild('dl'))
    grn.get()

    # todo: this test is incomplete yet


def _gen_structure():
    structure = {
        'file': {
            'type': 'file',
            'executable': False,
            'size': 4,
            'resource': {
                'type': None
            }
        },
        'touch': {
            'type': 'file',
            'executable': True,
            'size': 0,
            'resource': {
                'type': 'touch'
            }
        },
        'dir': {
            'type': 'dir'
        },
        'dir/subfile': {
            'type': 'file',
            'executable': True,
            'size': 12,
            'resource': {
                'type': None
            }
        },
        'dir/subtouch': {
            'type': 'file',
            'executable': False,
            'size': 0,
            'resource': {'type': 'touch'}
        },
        'dir/subdir': {
            'type': 'dir'
        },
        'dir/subdir/subsubfile': {
            'type': 'file',
            'executable': False,
            'size': 1,
            'resource': {
                'type': None
            }
        },
        'onlytouch': {
            'type': 'dir'
        },
        'onlytouch/touch': {
            'type': 'file',
            'executable': False,
            'size': 0,
            'resource': {'type': 'touch'}
        }
    }
    return structure

def test_precreate_files(tmpdir):
    dl = Download()

    structure = _gen_structure()

    # Step: just check filesystem creation
    dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=False)

    for path, info in structure.iteritems():
        if info['type'] == 'dir':
            assert tmpdir.join(path).check(dir=1)
        elif info['type'] == 'file':
            fn = tmpdir.join(path)
            res = info['resource']
            if res['type'] == 'touch':
                assert fn.check(file=1)
                assert fn.stat().size == 0
            else:
                assert fn.check(file=1)
                assert fn.stat().size == info['size']

    # Step: invalid file mode should be fixed without any problems
    tmpdir.join('touch').write('fake')
    tmpdir.join('touch').chmod(0x000)

    dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=False)

    assert tmpdir.join('touch').stat().size == 0
    assert tmpdir.join('touch').stat().mode & 0x644

    # Step: If we are unable to truncate/change file -- we should not pop any errors
    # here, all errors should be during download itself (maybe we will not want
    # to change file at all, if it was already downloaded)
    tmpdir.join('file').write('invalid')
    tmpdir.join('file').chmod(0)

    dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=False)

    assert tmpdir.join('file').stat().size == 7
    assert tmpdir.join('file').read() == 'invalid'

    # Step: if we will not be able to touch file, perm denied error should pop
    # coz we will not try to make it again during downloads
    tmpdir.join('dir', 'subtouch').remove()
    tmpdir.join('dir').chmod(0o555)

    try:
        dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=False)
    except FilesystemError:
        pass
    else:
        raise Exception('Not raised')
    finally:
        tmpdir.join('dir').chmod(0o755)

    # Step: if we are unable to precreate any file -- this should not be a problem here,
    # any errors should pop during download

    tmpdir.join('dir', 'subfile').write('fake')
    tmpdir.join('dir', 'subtouch').write('')
    tmpdir.join('dir').chmod(0o555)

    try:
        dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=False)
    finally:
        tmpdir.join('dir').chmod(0o755)

    # Step: unable to set proper perms on touch file
    tmpdir.join('onlytouch', 'touch').chmod(0o444)
    tmpdir.join('onlytouch').chmod(0o444)

    try:
        dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=False)
    except FilesystemError:
        pass
    else:
        raise Exception('Not raised')
    finally:
        tmpdir.join('onlytouch').chmod(0o755)

    # Step: got link instead of file/touch and remove. Also unable to remove
    tmpdir.join('onlytouch', 'touch').remove()
    tmpdir.join('onlytouch', 'touch').mksymlinkto('/etc/hosts')
    tmpdir.join('onlytouch').chmod(0o755)

    try:
        dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=False)
    except FilesystemError as ex:
        assert 'outside of destination' in str(ex)
    else:
        raise Exception('Not raised')

    [f.remove() for f in tmpdir.listdir()]

    created, _ = dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=True)
    assert len(created) == 6

    created, _ = dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=True)
    assert len(created) == 0

    # Step directory instead of file
    [f.remove() for f in tmpdir.listdir()]
    tmpdir.join('file').ensure(dir=1)

    created, _ = dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=True)
    assert len(created) == 6
    assert tmpdir.join('file').check(file=1)


def test_unable_to_precreate_huge_file(tmpdir):
    structure = {
        'a': {'type': 'dir'},
        'a/file': {
            'type': 'file',
            'executable': False,
            'size': 4,
            'resource': {'type': None}
        },
        'file': {
            'type': 'file',
            'executable': False,
            'size': 10 * 1024 * 1024 * 1024 * 1024,  # 10Tb
            'resource': {
                'type': None
            }
        }
    }

    dl = Download()

    try:
        created, _ = dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=True)
    except FilesystemError as ex:
        assert 'No space left' in str(ex)
    else:
        raise Exception('Not raised')

    # All fallocated paths should be removed, including small 4 byte
    assert len(list(tmpdir.visit())) == 1  # only directory should be left as is

    tmpdir.join('a/file').write('x')

    try:
        created, _ = dl._precreate_fs_items(tmpdir, structure, partial=None, use_fallocate=True)
    except FilesystemError as ex:
        assert 'No space left' in str(ex)
    else:
        raise Exception('Not raised')

    # Only files we create should be removed, so a/file should be left as is with dir "a"
    assert len(list(tmpdir.visit())) == 2
