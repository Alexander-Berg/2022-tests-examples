import mock
from infra.ya_salt.lib.components import manager
from infra.ya_salt.lib import saltutil


def test_compiler(monkeypatch):
    sc_mock = mock.Mock()
    sc_mock.compile = mock.Mock(return_value=('mock', None))
    compiler = manager.ComponentCompiler(None, None, None, None, None)
    monkeypatch.setattr(compiler, '_compiler', sc_mock)
    salt_selector = saltutil.Selector('search_runtime', 'deploy.mcelog')
    c, e = compiler.compile(salt_selector)
    assert e is None
    assert c == 'mock'
    sc_mock.compile.assert_called_once_with(salt_selector)
    mock_selector = saltutil.Selector('search_runtime', 'mock:non-existing/component')
    c, e = compiler.compile(mock_selector)
    assert c is None
    assert e == 'component type {} not supported, selector: {}'.format('mock', mock_selector)


def test_compiler_virtual_hostctl(monkeypatch):
    vc_mock = mock.Mock()
    vc_mock.compile = mock.Mock(return_value=('mock', None))
    compiler = manager.ComponentCompiler(None, None, None, None, None)
    monkeypatch.setattr(compiler, '_virtual_compiler', vc_mock)
    salt_selector = saltutil.Selector('search_runtime', 'virtual:hostctl')
    c, e = compiler.compile(salt_selector)
    assert e is None
    assert c == 'mock'
    vc_mock.compile.assert_called_once_with(salt_selector)


def test_compiler_hostctl(monkeypatch):
    c_mock = mock.Mock()
    c_mock.compile = mock.Mock(return_value=('mock', None))
    compiler = manager.ComponentCompiler(None, None, None, None, None)
    monkeypatch.setattr(compiler, '_hostctl_compiler', c_mock)
    salt_selector = saltutil.Selector('search_runtime', 'hostctl:mcelog')
    c, e = compiler.compile(salt_selector)
    assert e is None
    assert c == 'mock'
    c_mock.compile.assert_called_once_with(salt_selector)
