import mock
from infra.ya_salt.lib import saltutil
from infra.ya_salt.lib.components import hostctl
from infra.ya_salt.proto import ya_salt_pb2


def test_compile():
    status = ya_salt_pb2.HostmanStatus()
    sel = saltutil.Selector('search_runtime', 'hostctl:mcelog')
    compiler = hostctl.Compiler(status, None, 'https://orly')
    c, e = compiler.compile(sel)
    assert e is None
    assert c.selector is sel
    assert len(status.salt_components) == 1
    assert c.status.skip_reporting is True
    assert c.status.name == 'mcelog'
    assert c.orly_url == 'https://orly'
    _, e = compiler.compile(sel)
    assert e is None


def test_compiled_component_pkgs():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    c = hostctl.CompiledComponent(saltutil.Selector('search_runtime', 'virtual:hostctl'), cs, None, None)
    assert c.get_packages_actions() == (tuple(), tuple())


def test_compiled_component_apply_ok():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    ctx = mock.Mock()
    hctl = mock.Mock()
    hctl.available = mock.Mock(return_value=None)
    hctl.manage_target = mock.Mock(return_value=None)
    c = hostctl.CompiledComponent(saltutil.Selector('search_runtime', 'hostctl:mcelog'), cs, hctl, 'https://orly')
    ac = c.apply(ctx)
    hctl.manage_target.assert_called_once()
    assert ac.err is None


def test_hostctl_compiled_component_apply_fail():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    ctx = mock.Mock()
    hctl = mock.Mock()
    hctl.available = mock.Mock(return_value=None)
    hctl.manage_target = mock.Mock(return_value='err')
    c = hostctl.CompiledComponent(saltutil.Selector('search_runtime', 'hostctl:mcelog'), cs, hctl, 'https://orly')
    ac = c.apply(ctx)
    hctl.manage_target.assert_called_once()
    assert ac.err == 'err'


def test_hostctl_applied_component_applied():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    cs.applied.status = 'True'
    c = hostctl.AppliedComponent(cs, None, 1)
    assert c.applied() is True
    cs.applied.status = 'False'
    assert c.applied() is False


def test_hostctl_applied_component_process_results_ok():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    cs.name = 'mcelog'
    c = hostctl.AppliedComponent(cs, None, 1)
    c.process_results()
    assert len(cs.salt.state_results) == 1
    assert cs.salt.state_results[0].id == '__hostctl.mcelog'
    assert cs.salt.state_results[0].comment == 'hostctl manage target: ok'
    assert cs.salt.state_results[0].ok is True
    assert cs.salt.state_results[0].duration == 1


def test_hostctl_applied_component_process_results_fail():
    s = ya_salt_pb2.HostmanStatus()
    cs = s.salt_components.add()
    cs.name = 'mcelog'
    c = hostctl.AppliedComponent(cs, 'err', 2)
    c.process_results()
    assert len(cs.salt.state_results) == 1
    assert cs.salt.state_results[0].id == '__hostctl.mcelog'
    assert cs.salt.state_results[0].comment == 'hostctl manage target: err'
    assert cs.salt.state_results[0].ok is False
    assert cs.salt.state_results[0].duration == 2


def test_compile_component_status_ok():
    status = ya_salt_pb2.HostmanStatus()
    sel = saltutil.Selector('search_runtime', 'hostctl:mcelog')
    compiler = hostctl.Compiler(status, None, 'https://orly')
    c, e = compiler.compile(sel)
    assert e is None
    assert c.status.initialized.status == 'True'
