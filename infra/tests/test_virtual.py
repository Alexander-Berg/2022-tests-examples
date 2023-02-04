import mock

from infra.ya_salt.lib import saltutil
from infra.ya_salt.lib.components import virtual
from infra.ya_salt.proto import ya_salt_pb2


def test_compile_hostctl():
    s = ya_salt_pb2.HostmanStatus()
    compiler = virtual.Compiler(s, None)
    c, e = compiler.compile(saltutil.Selector('search_runtime', 'virtual:hostctl'))
    assert e is None
    assert isinstance(c, virtual.CompiledHostctlComponent)
    assert len(s.salt_components) == 1
    assert s.salt_components[0].name == 'hostctl'
    assert s.salt_components[0].skip_reporting is True


def test_hostctl_compiled_component_pkgs():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    c = virtual.CompiledHostctlComponent(saltutil.Selector('search_runtime', 'virtual:hostctl'), None, cs, None)
    assert c.get_packages_actions() == (tuple(), tuple())
    assert cs.skip_reporting is True


def test_hostctl_compiled_component_apply_ok():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    ctx = mock.Mock()
    ctx.done = mock.Mock(return_value=False)
    hctl = mock.Mock()
    hctl.available = mock.Mock(return_value=None)
    hctl.report = mock.Mock(return_value=None)
    c = virtual.CompiledHostctlComponent(saltutil.Selector('search_runtime', 'virtual:hostctl'), s, cs, hctl)
    ac = c.apply(ctx)
    hctl.report.assert_called_once()
    assert ac.err is None


def test_hostctl_compiled_component_apply_fail():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    ctx = mock.Mock()
    ctx.done = mock.Mock(return_value=False)
    hctl = mock.Mock()
    hctl.available = mock.Mock(return_value=None)
    hctl.report = mock.Mock(return_value='err')
    c = virtual.CompiledHostctlComponent(saltutil.Selector('search_runtime', 'virtual:hostctl'), s, cs, hctl)
    ac = c.apply(ctx)
    hctl.report.assert_called_once()
    assert ac.err == 'err'


def test_hostctl_applied_component_applied():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    cs.applied.status = 'True'
    c = virtual.AppliedHostctlComponent(cs, None, 1)
    assert c.applied() is True
    cs.applied.status = 'False'
    assert c.applied() is False


def test_hostctl_applied_component_process_results_ok():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    c = virtual.AppliedHostctlComponent(cs, None, 1)
    c.process_results()
    assert len(cs.salt.state_results) == 1
    assert cs.salt.state_results[0].id == '__virtual.hostctl'
    assert cs.salt.state_results[0].comment == 'hostctl report: ok'
    assert cs.salt.state_results[0].ok is True
    assert cs.salt.state_results[0].duration == 1


def test_hostctl_applied_component_process_results_fail():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    c = virtual.AppliedHostctlComponent(cs, 'err', 2)
    c.process_results()
    assert len(cs.salt.state_results) == 1
    assert cs.salt.state_results[0].id == '__virtual.hostctl'
    assert cs.salt.state_results[0].comment == 'hostctl report: err'
    assert cs.salt.state_results[0].ok is False
    assert cs.salt.state_results[0].duration == 2


def test_compile_hostctl_status_initialized_ok():
    s = ya_salt_pb2.HostmanStatus()
    compiler = virtual.Compiler(s, None)
    c, e = compiler.compile(saltutil.Selector('search_runtime', 'virtual:hostctl'))
    assert e is None
    assert c.status.initialized.status == 'True'
