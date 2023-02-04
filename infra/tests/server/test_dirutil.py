import collections
import os

import mock

from infra.diskmanager.lib import dirutil


def test_wrap_error():
    def _ok():
        return 'mock'

    def _nok():
        raise Exception('mock')

    assert dirutil.wrap_error(_ok) == ('mock', None)
    assert dirutil.wrap_error(_nok) == (None, 'got exception: mock')


def test_ensure_link(monkeypatch):
    # makedirs = False
    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: False)
    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: True)
    symlink = mock.Mock()
    symlink.__name__ = 'symlink'
    monkeypatch.setattr(os, 'symlink', symlink)
    assert dirutil.ensure_link('/path/to/link', '/target', False) is None
    symlink.assert_called_once_with('/target', '/path/to/link')

    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: False)
    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: True)
    symlink = mock.Mock(side_effect=Exception('mock'))
    symlink.__name__ = 'symlink'
    monkeypatch.setattr(os, 'symlink', symlink)
    assert dirutil.ensure_link('/path/to/link', '/target', False) == 'failed to create link /path/to/link to /target: got exception: mock'
    symlink.assert_called_once_with('/target', '/path/to/link')

    # makedirs = True
    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: False)
    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: False)
    makedirs = mock.Mock()
    makedirs.__name__ = 'makedirs'
    monkeypatch.setattr(os, 'makedirs', makedirs)
    symlink = mock.Mock()
    symlink.__name__ = 'symlink'
    monkeypatch.setattr(os, 'symlink', symlink)
    assert dirutil.ensure_link('/path/to/link', '/target') is None
    symlink.assert_called_once_with('/target', '/path/to/link')
    makedirs.assert_called_once_with('/path/to')

    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: False)
    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: False)
    makedirs = mock.Mock(side_effect=Exception('mock'))
    makedirs.__name__ = 'makedirs'
    monkeypatch.setattr(os, 'makedirs', makedirs)
    symlink = mock.Mock()
    symlink.__name__ = 'symlink'
    monkeypatch.setattr(os, 'symlink', symlink)
    assert dirutil.ensure_link('/path/to/link', '/target') == 'failed to create parent dir /path/to for link /path/to/link: got exception: mock'
    symlink.assert_not_called()
    makedirs.assert_called_once_with('/path/to')

    # link exists
    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: True)
    monkeypatch.setattr(os, 'readlink', lambda *args, **kwargs: '/target')
    symlink = mock.Mock()
    symlink.__name__ = 'symlink'
    monkeypatch.setattr(os, 'symlink', symlink)
    assert dirutil.ensure_link('/path/to/link', '/target', False) is None
    symlink.assert_not_called()

    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: True)
    monkeypatch.setattr(os, 'readlink', lambda *args, **kwargs: '/something')
    symlink = mock.Mock()
    symlink.__name__ = 'symlink'
    monkeypatch.setattr(os, 'symlink', symlink)
    rename = mock.Mock()
    rename.__name__ = 'rename'
    monkeypatch.setattr(os, 'rename', rename)
    assert dirutil.ensure_link('/path/to/link', '/target', False) is None
    symlink.assert_called_once_with('/target', '/path/to/link-tmp')
    rename.assert_called_once_with('/path/to/link-tmp', '/path/to/link')


def test_ensure_dir(monkeypatch):
    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: False)
    makedirs = mock.Mock()
    makedirs.__name__ = 'makedirs'
    monkeypatch.setattr(os, 'makedirs', makedirs)
    chown = mock.Mock()
    chown.__name__ = 'chown'
    monkeypatch.setattr(os, 'chown', chown)
    assert dirutil.ensure_dir('/path', 0, 0, 0o777) is None
    makedirs.assert_called_once_with('/path', 0o777)
    chown.assert_called_once_with('/path', 0, 0)

    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: True)
    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: True)
    assert dirutil.ensure_dir('/path', 0, 0, 0o777) == 'path /path is symlink'

    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: True)
    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: False)
    monkeypatch.setattr(os.path, 'isfile', lambda *args, **kwargs: True)
    assert dirutil.ensure_dir('/path', 0, 0, 0o777) == 'path /path is file'

    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: True)
    monkeypatch.setattr(os.path, 'islink', lambda *args, **kwargs: False)
    monkeypatch.setattr(os.path, 'isfile', lambda *args, **kwargs: False)
    st_result = collections.namedtuple('st_result', 'st_uid,st_gid,st_mode')
    stat = mock.Mock(return_value=st_result(1, 1, 0))
    stat.__name__ = 'stat'
    monkeypatch.setattr(os, 'stat', stat)
    chown = mock.Mock()
    chown.__name__ = 'chown'
    monkeypatch.setattr(os, 'chown', chown)
    chmod = mock.Mock()
    chmod.__name__ = 'chmod'
    monkeypatch.setattr(os, 'chmod', chmod)
    assert dirutil.ensure_dir('/path', 0, 0, 0o777) is None
    chown.assert_called_once_with('/path', 0, 0)
    chmod.assert_called_once_with('/path', 0o777)
