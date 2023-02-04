import json
import mock

from infra.ya_salt.proto import ya_salt_pb2
from infra.ya_salt.lib import saltutil
from infra.ya_salt.lib import constants
from infra.ya_salt.lib.components import salt_component

DAEMON_FROM_LO_CASES = [
    (
        {'state': 'pkg', 'fun': 'installed', 'name': 'yandex-fake'},
        None
    ),
    (
        {'state': 'service', 'fun': 'enabled', 'name': 'sshd'}, None
    ),
    (
        {'state': 'service', 'fun': 'running', 'name': 'yandex-diskmanager'},
        'yandex-diskmanager'
    ),

]

MCELOG_HI_JSON = """
{
  "mcelog_packages": {
    "pkg": [
      {
        "refresh": false
      },
      {
        "pkgs": [
          {
            "mcelog": "162-yandex2"
          }
        ]
      },
      "installed",
      {
        "order": 10013
      }
    ],
    "__sls__": "components.mcelog.mcelog-1",
    "__env__": "search_runtime"
  }
}
"""
MCELOG_INVALID_HI_JSON = """
{
  "mcelog_packages": {
    "pkg": [
      {
        "refresh": false
      },
      {
        "pkgs": [
          {
            "mcelog": "162-yandex2"
          }
        ]
      },
      "installed",
      {
        "order": 10013
      }
    ],
    "__sls__": "components.mcelog.mcelog-1",
    "__env__": "search_runtime"
  },
  "mcelog_packages2": {
    "pkg": [
      {
        "refresh": false
      },
      {
        "pkgs": [
          {
            "mcelog": "162-yandex2"
          }
        ]
      },
      "installed",
      {
        "order": 10013
      }
    ],
    "__sls__": "components.mcelog2.mcelog-3",
    "__env__": "search_runtime"
  }
}
"""
MCELOG_RESULTS_JSON = """
{
  "pkg_|-mcelog_packages_|-mcelog_packages_|-installed": {
    "comment": "All specified packages are already installed and are at the desired version",
    "name": "mcelog",
    "started": 1581963329,
    "start_time": "21:15:29.314921",
    "result": true,
    "duration": 44.092,
    "__run_num__": 0,
    "__sls__": "components.mcelog.mcelog-1",
    "changes": {},
    "__id__": "mcelog_packages"
  }
}
"""
MCELOG_LO_JSON = """
{
  "lo": [
    {
      "name": "mcelog_packages",
      "refresh": false,
      "state": "pkg",
      "__id__": "mcelog_packages",
      "fun": "installed",
      "__env__": "search_runtime",
      "__sls__": "components.mcelog.mcelog-1",
      "order": 10000,
      "pkgs": [
        {
          "mcelog": "162-yandex2"
        }
      ]
    }
  ],
  "rev": "<local>",
  "ts": 0
}
"""
APT_HOSTCTL_LO_JSON = """
{
  "lo": [
    {
      "name": "apt",
      "contents": "mock: value_mock",
      "state": "hostctl",
      "__id__": "apt-hostctl",
      "fun": "manage",
      "__env__": "search_runtime",
      "__sls__": "deploy.apt",
      "order": 10000
    }
  ],
  "rev": "<local>",
  "ts": 0
}
"""
APT_HOSTCTL_HI_JSON = """
{
  "apt-hostctl": {
    "hostctl": [
      {
        "name": "apt"
      },
      {
        "contents": "mock: value_mock"
      },
      "manage",
      {
        "order": 10000
      }
    ],
    "__sls__": "deploy.apt",
    "__env__": "search_runtime"
  }
}
"""


def test_extract_yasm_from_lo():
    lo = [
        {
            'state': 'service',
            'fun': 'running',
            'name': 'sshd',
            'order': 1,
            '__sls__': 'some.ssh',
            '__env__': 'search_runtime',
            '__id__': 'sshd_service',
        },
        {
            'order': 2,
            'state': 'pkg',
            'fun': 'installed',
            'name': 'yandex-yasmagent',
            'version': '1.11-test-hostman',
            '__sls__': 'some.yasm',
            '__env__': 'search_runtime',
            '__id__': 'yandex-yasmagent_installed',
        },
    ]
    spec = ya_salt_pb2.HostmanSpec()
    salt_component.extract_yasm_from_lo(lo, spec)
    assert spec.yasm.agent_version == '1.11-test-hostman'
    assert len(lo) == 2
    assert lo[1] == {
        'order': 2,
        'state': 'test',
        'fun': 'nop',
        'name': 'yandex-yasmagent',
        '__sls__': 'some.yasm',
        '__env__': 'search_runtime',
        '__id__': 'yandex-yasmagent_installed',
    }


def test_salt_components_orly():
    msg = 'too many ops in progress'
    f = mock.Mock(return_value=msg)
    orly = salt_component.SaltComponentsOrly(f)
    assert orly.check_orly() == msg
    assert orly.check_orly() == msg
    f.assert_called_once()


class TestSaltComponent(object):
    def test_has_diff(self):
        c = salt_component.SaltComponent(None, None, None, None, None)
        assert c.has_diff() is False
        c = salt_component.SaltComponent(None, None, None, None, 'diff')
        assert c.has_diff() is True

    def test_get_packages_actions(self):
        rv = (['installed'], ['removed'])
        cc = mock.Mock()
        cc.get_packages_actions = mock.Mock(return_value=rv)
        c = salt_component.SaltComponent(None, None, None, cc, None)
        actions = c.get_packages_actions()
        assert actions == rv

    def test_get_allowed_component(self):
        status = ya_salt_pb2.HostmanStatus()
        repo = mock.Mock()
        repo.empty_lo = mock.Mock(return_value=saltutil.Low.empty())
        selector = saltutil.Selector('sr', 'state')
        cc = salt_component.CompiledSaltComponent(
            selector,
            repo,
            saltutil.Low.empty(),
            status.salt_components.add(),
            None
        )
        orly = salt_component.SaltComponentsOrly(lambda: None)
        c = salt_component.SaltComponent(selector, orly, None, cc, None)
        assert c._get_allowed_component() is cc
        c = salt_component.SaltComponent(selector, orly, None, cc, 'diff')
        assert c._get_allowed_component() is cc

        orly = salt_component.SaltComponentsOrly(lambda: 'too many ops in progress')
        c = salt_component.SaltComponent(selector, orly, None, cc, None)
        assert c._get_allowed_component() is cc
        c = salt_component.SaltComponent(selector, orly, None, cc, 'diff')
        assert c._get_allowed_component() is not cc

    def test_apply(self, monkeypatch):
        component = salt_component.SaltComponent(None, None, None, None, None)
        compiled = mock.Mock()
        compiled.apply = mock.Mock(return_value='ok')
        gac = mock.Mock(return_value=compiled)
        monkeypatch.setattr(component, '_get_allowed_component', gac)
        assert component.apply(None) == 'ok'
        component = salt_component.SaltComponent(None, None, None, None, None)
        compiled = mock.Mock()
        rv_mock = mock.Mock()
        rv_mock.applied = mock.Mock(return_value=True)
        compiled.apply = mock.Mock(return_value=rv_mock)
        gac = mock.Mock(return_value=compiled)
        monkeypatch.setattr(component, '_get_allowed_component', gac)
        monkeypatch.setattr(component, '_diff', 'diff')
        rv = component.apply(None)
        rv.applied.assert_called()


class TestCompiler(object):
    def test_validate_hi(self):
        hi = saltutil.High(json.loads(MCELOG_INVALID_HI_JSON))
        selector = saltutil.Selector('search_runtime', 'deploy.mcelog')
        assert salt_component.Compiler._validate_high(selector, hi) is not None

    def test_get_version_from_lo(self):
        lo = saltutil.Low.from_bytes(MCELOG_LO_JSON)
        selector = saltutil.Selector('search_runtime', 'deploy.mcelog')
        assert salt_component.Compiler._get_version_from_lo(selector, lo) == 'mcelog-1'

    def test_is_hostctl_component(self):
        lo = saltutil.Low.from_bytes(MCELOG_LO_JSON)
        assert salt_component.Compiler._is_hostctl_component(lo) is False
        lo = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON)
        assert salt_component.Compiler._is_hostctl_component(lo) is True

    def test_validate_hostctl_lo(self):
        lo = saltutil.Low.from_bytes(MCELOG_LO_JSON)
        assert (salt_component.Compiler._validate_hostctl_lo(lo) ==
                'hostctl.manage should have "name" and "contents" parameters defined and not empty')
        lo.states().append('None')
        assert (salt_component.Compiler._validate_hostctl_lo(lo) ==
                'hostctl component must have exactly and only one hostctl.manage state')
        lo = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON)
        assert salt_component.Compiler._validate_hostctl_lo(lo) is None
        lo.states()[0]['name'] = ''
        assert (salt_component.Compiler._validate_hostctl_lo(lo) ==
                'hostctl.manage should have "name" and "contents" parameters defined and not empty')
        lo.states()[0]['name'] = 'apt'
        lo.states()[0]['contents'] = ''
        assert (salt_component.Compiler._validate_hostctl_lo(lo) ==
                'hostctl.manage should have "name" and "contents" parameters defined and not empty')

    def test_compile_a(self):
        status = ya_salt_pb2.HostmanStatus()
        c_status = status.salt_components.add()
        spec_b = ya_salt_pb2.HostmanSpec()
        selector = saltutil.Selector('search_runtime', 'deploy.mcelog')
        repo = mock.Mock()
        lo_mock = saltutil.Low.from_bytes(MCELOG_LO_JSON)
        repo.lo_from_selector = mock.Mock(return_value=(lo_mock, None))
        compiler = salt_component.Compiler(repo, status, None, spec_b, None)
        r = compiler._compile_a(selector, c_status)
        assert r.lo is lo_mock
        repo.lo_from_selector = mock.Mock(return_value=(None, 'error'))
        repo.empty_lo = mock.Mock(return_value=saltutil.Low.empty())
        r = compiler._compile_a(selector, c_status)
        assert r.lo.states() == []

    def test_compile_a_hostctl(self):
        status = ya_salt_pb2.HostmanStatus()
        c_status = status.salt_components.add()
        spec_b = ya_salt_pb2.HostmanSpec()
        selector = saltutil.Selector('search_runtime', 'deploy.apt')
        repo = mock.Mock()
        lo_mock = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON)
        repo.lo_from_selector = mock.Mock(return_value=(lo_mock, None))
        compiler = salt_component.Compiler(repo, status, None, spec_b, None)
        r = compiler._compile_a(selector, c_status)
        assert r.lo is lo_mock
        assert isinstance(r, salt_component.CompiledHostctlComponent)

    def test_compile_b(self):
        status = ya_salt_pb2.HostmanStatus()
        component_status = status.salt_components.add()
        spec_b = ya_salt_pb2.HostmanSpec()
        selector = saltutil.Selector('search_runtime', 'deploy.mcelog')
        repo = mock.Mock()
        test_hi = saltutil.High(json.loads(MCELOG_HI_JSON), 'rev', 'ts')
        repo.render_high_selector = mock.Mock(return_value=(test_hi, None))
        compiler = salt_component.Compiler(repo, status, None, spec_b, None)
        rv, err = compiler._compile_b(selector, component_status)
        assert component_status.new_version == 'mcelog-1'
        rv_states = rv.lo.states()[0]
        rv_states.pop('order')
        lo_states = saltutil.Low.from_bytes(MCELOG_LO_JSON).states()[0]
        lo_states.pop('order')
        assert rv_states == lo_states

    def test_compile_b_hostctl(self):
        status = ya_salt_pb2.HostmanStatus()
        component_status = status.salt_components.add()
        spec_b = ya_salt_pb2.HostmanSpec()
        selector = saltutil.Selector('search_runtime', 'deploy.apt')
        repo = mock.Mock()
        test_hi = saltutil.High(json.loads(APT_HOSTCTL_HI_JSON), 'rev', 'ts')
        repo.render_high_selector = mock.Mock(return_value=(test_hi, None))
        compiler = salt_component.Compiler(repo, status, None, spec_b, None)
        rv, err = compiler._compile_b(selector, component_status)
        rv_states = rv.lo.states()[0]
        rv_states.pop('order')
        lo_states = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON).states()[0]
        lo_states.pop('order')
        assert rv_states == lo_states
        assert isinstance(rv, salt_component.CompiledHostctlComponent)

    def test_compile(self):
        status = ya_salt_pb2.HostmanStatus()
        spec_b = ya_salt_pb2.HostmanSpec()
        selector = saltutil.Selector('search_runtime', 'deploy.mcelog')
        repo = mock.Mock()
        lo_mock = saltutil.Low.from_bytes(MCELOG_LO_JSON)
        repo.lo_from_selector = mock.Mock(return_value=(lo_mock, None))
        test_hi = saltutil.High(json.loads(MCELOG_HI_JSON), 'rev', 'ts')
        repo.render_high_selector = mock.Mock(return_value=(test_hi, None))
        compiler = salt_component.Compiler(repo, status, None, spec_b, None)
        rv, err = compiler.compile(selector)
        assert err is None
        assert rv.has_diff() is False

    def test_compile_hostctl(self):
        status = ya_salt_pb2.HostmanStatus()
        spec_b = ya_salt_pb2.HostmanSpec()
        selector = saltutil.Selector('search_runtime', 'deploy.apt')
        repo = mock.Mock()
        lo_mock = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON)
        repo.lo_from_selector = mock.Mock(return_value=(lo_mock, None))
        test_hi = saltutil.High(json.loads(APT_HOSTCTL_HI_JSON), 'rev', 'ts')
        repo.render_high_selector = mock.Mock(return_value=(test_hi, None))
        compiler = salt_component.Compiler(repo, status, None, spec_b, None)
        rv, err = compiler.compile(selector)
        assert err is None
        assert rv.has_diff() is False

    def test_get_orly_check_fun(self):
        spec = ya_salt_pb2.HostmanSpec()
        c = salt_component.Compiler(None, None, None, spec, None)
        f = c._get_orly_check_fun()
        assert f() is None
        # Check orly called
        m = mock.Mock()
        m.start_operation.return_value = 'error: too many operations'
        c = salt_component.Compiler(None, None, m, spec, None)
        f = c._get_orly_check_fun()
        assert f() == m.start_operation.return_value
        m.start_operation.assert_called_once_with(constants.ORLY_SALT_RULE)
        # Check initial setup rule chosen
        m = mock.Mock()
        m.start_operation.return_value = 'error: too many operations'
        spec.need_initial_setup = True
        c = salt_component.Compiler(None, None, m, spec, None)
        f = c._get_orly_check_fun()
        assert f() == m.start_operation.return_value
        m.start_operation.assert_called_once_with(constants.ORLY_SALT_RULE_INITIAL)
        spec.need_initial_setup = False
        # Check prestable rule chosen
        m = mock.Mock()
        m.start_operation.return_value = 'error: too many operations'
        spec.env_type = 'prestable'
        c = salt_component.Compiler(None, None, m, spec, None)
        f = c._get_orly_check_fun()
        assert f() == m.start_operation.return_value
        m.start_operation.assert_called_once_with(constants.ORLY_SALT_RULE_PRESTABLE)


class TestCompiledSaltComponent(object):
    def test_status_skip_report_false(self):
        selector = saltutil.Selector('search_runtime', 'deploy.mcelog')
        status = ya_salt_pb2.HostmanStatus()
        component_status = status.salt_components.add()
        lo = saltutil.Low.from_bytes(MCELOG_LO_JSON)
        _ = salt_component.CompiledSaltComponent(selector, None, lo, component_status, 'mcelog-1')
        assert component_status.skip_reporting is False

    def test_apply(self):
        selector = saltutil.Selector('search_runtime', 'deploy.mcelog')
        status = ya_salt_pb2.HostmanStatus()
        component_status = status.salt_components.add()
        lo = saltutil.Low.from_bytes(MCELOG_LO_JSON)
        repo = mock.Mock()
        executor = mock.Mock()
        executor.execute = mock.Mock(return_value=(json.loads(MCELOG_RESULTS_JSON), None))
        repo.get_executor = mock.Mock(return_value=executor)
        c = salt_component.CompiledSaltComponent(selector, repo, lo, component_status, 'mcelog-1')
        rv = c.apply(None)
        assert rv.applied() is True
        assert component_status.applied_version == 'mcelog-1'
        executor.execute = mock.Mock(return_value=(None, 'error'))
        rv = c.apply(None)
        assert rv.applied() is False


class TestCompiledHostctlComponent(object):
    def test_status_skip_report_true(self):
        selector = saltutil.Selector('search_runtime', 'deploy.apt')
        status = ya_salt_pb2.HostmanStatus()
        component_status = status.salt_components.add()
        lo = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON)
        repo = mock.Mock()
        repo.get_path = mock.Mock(return_value=['repo_path'])
        _ = salt_component.CompiledHostctlComponent(selector, repo, lo, component_status, 'apt-6', None)
        assert component_status.skip_reporting is True

    def test_wrap_ret(self):
        selector = saltutil.Selector('search_runtime', 'deploy.apt')
        status = ya_salt_pb2.HostmanStatus()
        component_status = status.salt_components.add()
        lo = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON)
        repo = mock.Mock()
        repo.get_path = mock.Mock(return_value=['repo_path'])
        c = salt_component.CompiledHostctlComponent(selector, repo, lo, component_status, 'apt-6', None)
        assert c._wrap_ret({}) == {'hostctl_|-deploy.apt_|-deploy.apt_|-manage': {}}

    def test_apply_ok(self):
        selector = saltutil.Selector('search_runtime', 'deploy.apt')
        status = ya_salt_pb2.HostmanStatus()
        component_status = status.salt_components.add()
        lo = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON)
        hctl = mock.Mock()
        hctl.manage_inline = mock.Mock(return_value=None)
        repo = mock.Mock()
        c = salt_component.CompiledHostctlComponent(selector, repo, lo, component_status, 'apt-6', hctl)
        ctx = mock.Mock()
        rv = c.apply(ctx)
        hctl.manage_inline.assert_called_once_with('apt', constants.LOCAL_REPO_CURRENT, 'mock: value_mock')
        assert rv.applied() is True
        assert rv.results['hostctl_|-deploy.apt_|-deploy.apt_|-manage']['result'] is True
        ctx.ok.assert_called()

    def test_apply_fail(self):
        selector = saltutil.Selector('search_runtime', 'deploy.apt')
        status = ya_salt_pb2.HostmanStatus()
        component_status = status.salt_components.add()
        lo = saltutil.Low.from_bytes(APT_HOSTCTL_LO_JSON)
        hctl = mock.Mock()
        hctl.manage_inline = mock.Mock(return_value='error_mock')
        repo = mock.Mock()
        c = salt_component.CompiledHostctlComponent(selector, repo, lo, component_status, 'apt-6', hctl)
        ctx = mock.Mock()
        rv = c.apply(ctx)
        hctl.manage_inline.assert_called_once_with('apt', constants.LOCAL_REPO_CURRENT, 'mock: value_mock')
        assert rv.applied() is True
        assert rv.results['hostctl_|-deploy.apt_|-deploy.apt_|-manage']['result'] is False
        assert 'error_mock' in rv.results['hostctl_|-deploy.apt_|-deploy.apt_|-manage']['comment']
        ctx.fail.assert_called()
