import collections
import json

import mock
import pytest
import yatest

from infra.ya_salt.lib import saltutil


def test_lo_to_bytes():
    l = saltutil.Low({'a': 'b', 'c': ['d']}, 'bc45243', ts=123)
    assert l.to_bytes() == '{"lo": {"a": "b", "c": ["d"]}, "rev": "bc45243", "ts": 123}'


def test_lo_from_file():
    # Test non existing file
    h, err = saltutil.Low.from_file('./not_exists')
    assert h is None
    assert err is not None
    # Test non json file
    with open('./execution_plan', 'w') as f:
        f.write('Some garbage')
    h, err = saltutil.Low.from_file('./execution_plan')
    assert h is None
    assert err is not None
    # Test file with extra keys
    h = saltutil.Low({'a': 'b', 'c': ['d']}, 'bc45243', ts=123)
    d = h.to_dict()
    d['garbage'] = 'some value'
    with open('./execution_plan', 'w') as f:
        f.write(json.dumps(d))
    loaded, err = saltutil.Low.from_file('./execution_plan')
    assert err is None, err
    assert h.get_rev() == loaded.get_rev()
    assert h.get_ts() == loaded.get_ts()
    assert h.to_dict() == loaded.to_dict()
    # Test valid file
    h = saltutil.Low({'a': 'b', 'c': ['d']}, 'bc45243', ts=123)
    with open('./execution_plan', 'w') as f:
        f.write(h.to_bytes())
    loaded, err = saltutil.Low.from_file('./execution_plan')
    assert err is None, err
    assert h.get_rev() == loaded.get_rev()
    assert h.get_ts() == loaded.get_ts()
    assert h.to_dict() == loaded.to_dict()


def test_lo_to_from():
    d = collections.OrderedDict()
    d['z'] = 'a'
    d['b'] = ['1']
    d['aaaaa'] = {'1': '2', '0': '9'}
    a = saltutil.Low(d, '123123', ts=8362834)
    buf = a.to_bytes()
    b = saltutil.Low.from_bytes(buf)
    assert a.states() == b.states() and a.get_rev() == b.get_rev() and a.get_ts() == b.get_ts()


def test_load_minion_config():
    load_minion_config = saltutil.load_minion_config
    opts, err = load_minion_config()
    assert opts['environment'] == 'search_runtime'
    assert err is None
    opts, err = load_minion_config(minion_id='test-sas.search.yandex.net')
    assert err is None
    with open(yatest.common.source_path('infra/ya_salt/lib/tests/minion_config.json')) as f:
        expected = json.load(f)
    assert opts == expected, opts


def test_compare_lo():
    from collections import OrderedDict
    # Taken from real output
    a_lo = [{'__env__': u'search_runtime',
             '__id__': u'packages_bootstrap_stage',
             '__sls__': u'system.packages.stages.bootstrap',
             'fun': u'installed',
             'name': u'packages_bootstrap_stage',
             u'order': 10000,
             u'pkgs': [OrderedDict([(u'yandex-search-common-rsyslog', u'1.0-3410479')]),
                       OrderedDict([(u'yandex-search-common-settings', u'1.0-110')]),
                       OrderedDict([(u'yandex-search-user-root', u'1.0-69')]),
                       OrderedDict([(u'yandex-search-user-tcpdump', u'1.0-19')]),
                       OrderedDict([(u'yandex-search-user-robot-walle', u'1.0-5')]),
                       OrderedDict([(u'yandex-search-user-loadbase', u'1.0-4258246')]),
                       OrderedDict([(u'yandex-internal-root-ca', u'2013.02.11-3')]),
                       OrderedDict([(u'yandex-search-user-monitor', u'1.0-3516767')])],
             u'refresh': True,
             u'require': [OrderedDict([(u'pkg', u'packages_with_repos_configs')])],
             'state': u'pkg'},
            {'__env__': u'search_runtime',
             '__id__': u'packages_with_repos_configs',
             '__sls__': u'system.packages.stages.bootstrap',
             'fun': u'installed',
             'name': u'packages_with_repos_configs',
             u'order': 10001,
             u'pkgs': [OrderedDict([(u'yandex-search-common-apt-rtc', u'0.1-4262471')])],
             u'refresh': True,
             'state': u'pkg'}
            ]
    b_lo = []

    assert saltutil.get_lo_diff([], []) is None
    assert saltutil.get_lo_diff(a_lo, b_lo) == """len(a=2) != len(b=0)
First diff state:
a[0]:
 {'__env__': u'search_runtime',
 '__id__': u'packages_bootstrap_stage',
 '__sls__': u'system.packages.stages.bootstrap',
 'fun': u'installed',
 'name': u'packages_bootstrap_stage',
 u'order': 10000,
 u'pkgs': [OrderedDict([(u'yandex-search-common-rsyslog', u'1.0-3410479')]),
           OrderedDict([(u'yandex-search-common-settings', u'1.0-110')]),
           OrderedDict([(u'yandex-search-user-root', u'1.0-69')]),
           OrderedDict([(u'yandex-search-user-tcpdump', u'1.0-19')]),
           OrderedDict([(u'yandex-search-user-robot-walle', u'1.0-5')]),
           OrderedDict([(u'yandex-search-user-loadbase', u'1.0-4258246')]),
           OrderedDict([(u'yandex-internal-root-ca', u'2013.02.11-3')]),
           OrderedDict([(u'yandex-search-user-monitor', u'1.0-3516767')])],
 u'refresh': True,
 u'require': [OrderedDict([(u'pkg', u'packages_with_repos_configs')])],
 'state': u'pkg'}"""
    assert saltutil.get_lo_diff(a_lo, a_lo) is None


def test_render_lo():
    from collections import OrderedDict
    test_high = OrderedDict([
        (u'etc-hosts', OrderedDict([(u'file', [OrderedDict([(u'name', u'/etc/hosts')]),
                                               OrderedDict([(u'user', u'root')]),
                                               OrderedDict([(u'group', u'root')]),
                                               OrderedDict([(u'mode', 644)]),
                                               OrderedDict([(u'source', u'salt://system/hosts/hosts')]),
                                               OrderedDict([(u'template', u'jinja')]), u'managed',
                                               OrderedDict([(u'order', 10063)])]),

                                    ])),
        ('yandex-yasmagent_installed', OrderedDict([(u'file', [OrderedDict([(u'name', u'yasm-agent')]),
                                                               OrderedDict([(u'user', u'root')]),
                                                               OrderedDict([(u'group', u'root')]),
                                                               OrderedDict([(u'mode', 644)]),
                                                               OrderedDict([(u'source', u'salt://system/hosts/hosts')]),
                                                               OrderedDict([(u'template', u'jinja')]), u'managed',
                                                               OrderedDict([(u'order', 10063)])]),

                                                    ])),
    ])
    lo, err = saltutil.High(test_high).render_lo()
    assert err is None
    # Check that excluded state goes away
    assert lo.states() == [
        {'__id__': u'etc-hosts',
         'fun': u'managed',
         u'group': u'root',
         u'mode': 644,
         'name': u'/etc/hosts',
         u'order': 10063,
         u'source': u'salt://system/hosts/hosts',
         'state': u'file',
         u'template': u'jinja',
         u'user': u'root'},
        {'__id__': 'yandex-yasmagent_installed',
         'fun': u'managed',
         u'group': u'root',
         u'mode': 644,
         'name': u'yasm-agent',
         u'order': 10063,
         u'source': u'salt://system/hosts/hosts',
         'state': u'file',
         u'template': u'jinja',
         u'user': u'root'},
    ]


def test_rewrite_relative_src():
    fn = saltutil.rewrite_relative_src
    # No src case
    lo = {
        'name': u'apt_distro_conf',
        u'refresh': True,
        'state': u'pkg',
        '__id__': u'apt_distro_conf',
        'fun': u'installed',
        '__env__': u'search_runtime',
        '__sls__': u'components.apt.apt-1',
        u'order': 10000,
        u'pkgs': [collections.OrderedDict([(u'yandex-search-common-apt-rtc', u'0.1-4262471')])]}
    fn(lo)
    # No salt://./ case
    lo = {
        'name': u'/db/iss3/.salt/agent_runner',
        u'makedirs': True,
        u'require': [u'iss_conf_rendered'],
        u'source': u'salt://services/iss/yandex-iss-agent-r2.py',
        'state': u'file',
        '__id__': u'iss_start_script_created',
        u'template': u'jinja',
        'fun': u'managed',
        '__env__': u'search_runtime',
        '__sls__': u'services.iss',
        u'order': 10018,
        u'mode': 775
    }
    fn(lo)
    assert lo['source'] == 'salt://services/iss/yandex-iss-agent-r2.py'
    # Test no __sls__ case (for some reason?)
    lo = {
        'name': u'/db/iss3/.salt/agent_runner',
        u'makedirs': True,
        u'require': [u'iss_conf_rendered'],
        u'source': u'salt://./yandex-iss-agent-r2.py',
        'state': u'file',
        '__id__': u'iss_start_script_created',
        u'template': u'jinja',
        'fun': u'managed',
        '__env__': u'search_runtime',
        u'order': 10018,
        u'mode': 775
    }
    with pytest.raises(Exception):
        fn(lo)
    # Test rewrite case
    lo = {
        'name': u'/db/iss3/.salt/agent_runner',
        u'makedirs': True,
        u'require': [u'iss_conf_rendered'],
        u'source': u'salt://./yandex-iss-agent-r2.py',
        'state': u'file',
        '__id__': u'iss_start_script_created',
        u'template': u'jinja',
        'fun': u'managed',
        '__env__': u'search_runtime',
        '__sls__': u'services.iss',
        u'order': 10018,
        u'mode': 775
    }
    fn(lo)
    assert lo['source'] == 'salt://services/iss/yandex-iss-agent-r2.py'


def test_salt_repo_get_gencfg_grains():
    fun = saltutil._get_gencfg_grains
    repo = mock.Mock()
    # Test no file
    repo.open_file.side_effect = IOError
    grains, err = fun(repo)
    assert err == 'failed to load gencfg tag from search_runtime/_pillars/gencfg-version.sls: '
    repo.open_file.assert_called_once_with('search_runtime/_pillars/gencfg-version.sls')
    # Test invalid yaml file
    repo.reset_mock()
    repo.open_file = mock.mock_open(read_data='some-string')
    grains, err = fun(repo)
    assert err == 'gencfg-tag file is not a YAML mapping'
    # Test no tag value
    repo.reset_mock()
    repo.open_file = mock.mock_open(read_data='a: b\n')
    grains, err = fun(repo)
    assert err == 'no gencfg-tag in search_runtime/_pillars/gencfg-version.sls'
    # Test gencfg grain raises
    repo.reset_mock()
    repo.open_file = mock.mock_open(read_data='gencfg-tag: stable-117\n')
    grain_fn = mock.Mock()
    grain_fn.side_effect = Exception('NO WAY')
    grains, err = fun(repo, grain_fn=grain_fn)
    assert err == 'failed to get gencfg grain: NO WAY'
    grain_fn.assert_called_once_with('stable-117')
    # Test okay
    grain_fn.reset_mock(side_effect=True)
    grain_fn.return_value = {'gencfg': ['ALL_RUNTIME']}
    grains, err = fun(repo, grain_fn=grain_fn)
    assert err is None
    assert grains == grain_fn.return_value


def test_salt_repo_gen_opts():
    fc = mock.Mock()
    test_path = './test-gen-opts-gencfg.yaml'
    with open(test_path, 'w') as f:
        f.write('gencfg-tag: stable-117\n')
    fc.get_file.return_value = test_path
    grains_fn = mock.Mock()
    grains_fn.return_value = {'gencfg': ['ALL_RUNTIME']}, None
    opts, err = saltutil.SaltRepo._add_gencfg_grains(
        {'some': 'opt', 'grains': {'walle_project': 'unit-test'}},
        fc,
        grains_fn)
    assert err is None
    grains_fn.assert_called_once_with(fc)
    assert opts == {
        'some': 'opt',
        'grains': {
            'gencfg': ['ALL_RUNTIME'],
            'walle_project': 'unit-test',
        },
    }


def test_mutate_lo_state_to_nop():
    lo_state = {
        "pkgs": [
            {
                "yandex-search-common-apt-rtc": "0.1-4262471"
            }
        ],
        "order": 10002,
        "name": "apt_distro_conf",
        "require": [
            "/etc/apt/sources.list.d/yandex-common-stable.list"
        ],
        "refresh": False,
        "state": "pkg",
        "__id__": "apt_distro_conf",
        "fun": "installed",
        "__env__": "search_runtime",
        "__sls__": "components.apt.apt-2"
    }
    saltutil.mutate_lo_state_to_nop(lo_state)
    assert len(lo_state) == 7
    assert lo_state['fun'] == 'nop'
    assert lo_state['state'] == 'test'


def test_salt_selector():
    s = saltutil.Selector('search_runtime', 'deploy.mcelog')
    assert s.get_name() == 'mcelog'
    assert s.get_env() == 'search_runtime'
    assert s.get_match() == 'deploy.mcelog'
    s = saltutil.Selector('app-image', 'components.smth.ver-1')
    assert s.get_name() == 'smth'
    assert s.get_match() == 'components.smth.ver-1'
    assert s.get_env() == 'app-image'
    s = saltutil.Selector('qloud', 'common.smth')
    assert s.get_name() == 'common.smth'
    assert s.get_match() == 'common.smth'
    assert s.get_env() == 'qloud'


def test_compound_selector():
    s = saltutil.Selector('search_runtime', 'deploy.mcelog')
    assert s.get_name() == 'mcelog'
    assert s.get_env() == 'search_runtime'
    assert s.get_match() == 'deploy.mcelog'
    assert s.get_origin() == '.'
    assert s.get_type() == 'salt'
    s = saltutil.Selector('search_runtime', 'hostctl:sysdev/ebpf-agent')
    assert s.get_name() == 'ebpf-agent'
    assert s.get_env() == 'search_runtime'
    assert s.get_match() == 'ebpf-agent'
    assert s.get_origin() == 'sysdev'
    assert s.get_type() == 'hostctl'
