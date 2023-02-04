import ConfigParser
import textwrap
import yaml

from instancectl.lib import envutil
from instancectl.config.config import RunConfig


# FIXME: this file must be cleaned up

ENV = envutil.InstanceCtlEnv(
    instance_port=6666,
    instance_name=None,
    instance_dir='.',
    instance_id=None,
    service_id=None,
    node_name='fake-node',
    hostname='fake-hostname',
    orthogonal_tags={},
    use_spec=False,
    hq_poll=False,
    hq_report=False,
    default_container_env={'BSCONFIG_IHOST': 'fake-node'},
    hq_url=None,
    auto_tags=None,
    instance_tags_string='ENV_QueryCacheDir=/db/bin/base OPT_IndexDir=/nonexistant',
    yp_hq_spec=None,
    skip_fdatasync_config=False,
    hq_report_version=1,
    sd_url=None,
    mock_retry_sleeper_output=None,
    prepare_script_restart_policy=None,
    install_script_restart_policy={},
)


def test_match_run_parameters():
    run_config = RunConfig(ENV)
    cases = [
        ('^ENV_(.*)', ['ENV_this=123', 'ENV-that=321', 'ENV_those:123'], [('this', '123')]),
        ('^OPT_(.*)', ['Opt_first=first', '  OPT_SECOND=second'], [('SECOND', 'second')]),
    ]
    for exp, lines, expected in cases:
        assert list(run_config.match_run_parameters(exp, lines)) == expected


def test_bool():
    assert RunConfig._bool('no') is False
    assert RunConfig._bool('No') is False
    assert RunConfig._bool('NO') is False
    assert RunConfig._bool('False') is False
    assert RunConfig._bool('false') is False
    assert RunConfig._bool('0') is False
    assert RunConfig._bool('') is False
    assert RunConfig._bool('True') is True
    assert RunConfig._bool('true') is True
    assert RunConfig._bool('yest') is True


def test_get_conf_local_lines(tmpdir):
    run_config = RunConfig(ENV)
    assert run_config.get_conf_local_lines('section', 'some_not_exists_file.cfg') == []
    c = tmpdir.join('test.cfg')
    c.write('First\nSecond\n')
    assert ['First', 'Second'] == run_config.get_conf_local_lines('section', c.strpath)


def test_sections_order(tmpdir):
    config_file = tmpdir.join('ctl.conf')

    config = ConfigParser.SafeConfigParser()
    sections = [str(i) for i in range(100)]
    for s in sections:
        config.add_section(s)

    with open(str(config_file), 'w') as fp:
        config.write(fp)

    run_config = RunConfig(ENV)
    run_config.load(str(config_file))
    assert run_config.sections() == sections


def test_jobs_order(tmpdir):
    config_file = tmpdir.join('ctl.conf')

    config = ConfigParser.SafeConfigParser()
    sections = [str(i) for i in range(100)]
    for s in sections:
        config.add_section(s)
        config.set(s, 'binary', 'nope')
        config.set(s, 'arguments', '-NaN')

    with open(str(config_file), 'w') as fp:
        config.write(fp)

    run_config = RunConfig(ENV)
    run_config.load(str(config_file))
    dump_file = tmpdir.join('dump.yml')
    run_config.save(str(dump_file))
    with open(str(dump_file), mode='r') as f:
        dump = yaml.unsafe_load(f)
    assert list(dump['jobs'].keys()) == sections


def test_load(tmpdir, monkeypatch):
    config_file = tmpdir.join('ctl.conf')

    config = ConfigParser.SafeConfigParser()
    config.add_section('defaults')
    config.set('defaults', 'binary', 'daemon')
    config.add_section('httpsearch')
    config.set('httpsearch', 'binary', 'overwrite_daemon')

    config.add_section('httpsearch2')
    config.set('httpsearch2', 'binary', 'overwrite_daemon2')

    with open(str(config_file), 'w') as fp:
        config.write(fp)

    run_config = RunConfig(ENV)
    run_config.load(str(config_file))
    assert run_config.sections() == ['httpsearch', 'httpsearch2']

    assert run_config.get('httpsearch', 'binary') == 'overwrite_daemon'

    conflocal_file = tmpdir.join('Conf.local')

    conflocal_file.write(textwrap.dedent("""

        RearrangeRules=1 2 3
        QueryCacheDir=/db/BASE/cache
    """))

    conf_local_data = {
        'RearrangeRules': '1 2 3',
        'QueryCacheDir': '/db/BASE/cache',
    }

    config.set('httpsearch', 'Conf.local', str(conflocal_file))
    with open(str(config_file), 'w') as fp:
        config.write(fp)

    run_config.load(str(config_file))
    assert run_config.sections() == ['httpsearch', 'httpsearch2']

    conflocal_lines = run_config.get_conf_local_lines('httpsearch')
    for key in conf_local_data:
        assert key + '=' + conf_local_data[key] in conflocal_lines

    #
    # Test itags preference over Conf.local and something else
    #
    environ = {
    }
    for k, v in environ.items():
        monkeypatch.setenv(k, v)

    config.set('httpsearch', 'env_match', '^ENV_(.*)$')
    config.set('httpsearch', 'opt_match', '^OPT_(.*)$')
    config.set('httpsearch', 'arguments', ' -v "a b" -c d')
    config.set('httpsearch', 'coredump_probability', '90')
    config.set('httpsearch', 'always_coredump', 'yes')

    with open(str(config_file), 'w') as fp:
        config.write(fp)

    run_config.load(str(config_file))

    config_globals = run_config.get_globals()
    result_config = run_config.get_run_config('httpsearch', config_globals)
    assert '/db/bin/base' == result_config['environment']['QueryCacheDir']
    assert ['-V', 'IndexDir=/nonexistant'] == result_config['extra_run_arguments']

    assert result_config['always_coredump'] is True
    assert result_config['coredump_probability'] == 90


def test_matches():
    run_config = RunConfig(ENV)

    match_strings = [
        'ENV_QueryCacheDir=/db/bin/base',
        'QueryCacheDir=/db/bin/base',
        'ENV_QueryCacheDir=',
        'ENV_QueryCacheDir',
    ]

    matches = list(run_config.match_run_parameters('^ENV_(.*)$', match_strings))

    assert [('QueryCacheDir', '/db/bin/base')] == matches

    matches = list(run_config.match_run_parameters('^(.*)$', match_strings))

    assert len(matches) == 2
    assert ('ENV_QueryCacheDir', '/db/bin/base') in matches
    assert ('QueryCacheDir', '/db/bin/base') in matches


def test_orthogonal_tags_parsing():
    tags = ('MSK_SG_CLUSTER_ALEMATE_WORKER a_ctype_prod a_dc_fol a_geo_msk a_itype_alematedworker '
            'a_line_fol-7 a_metaprj_internal a_prj_alemate a_tier_none a_topology_group-MSK_SG_CLUSTER_ALEMATE_WORKER '
            'a_topology_trunk-2270085 a_topology_version-trunk-2270085')
    auto_tags = envutil.get_auto_tags(tags)
    expected = {
        'a_ctype': 'prod',
        'a_geo': 'msk',
        'a_itype': 'alematedworker',
        'a_prj': 'alemate',
        'a_dc': 'fol',
        'a_tier': 'none'
    }
    assert envutil.get_orthogonal_tags_dict(auto_tags) == expected


def test_shared_its_directory(monkeypatch):
    old_tags = ('SAS_WEB_BASE a_ctype_prestable a_dc_sas a_geo_sas '
                'a_itype_base a_prj_web-main a_sandbox_task_27141047 '
                'a_tier_RRGTier0 a_topology_stable-71-r13 cgset-cpu.smart=1 '
                'cgset-memory.limit_in_bytes=24G '
                'cgset-memory.low_limit_in_bytes=20G '
                'cgset-memory.recharge_on_pgfault=1 newstyle_upload production '
                'production_sas_base')

    monkeypatch.setenv('BSCONFIG_ITAGS', old_tags)
    monkeypatch.setenv('HOSTNAME', 'localhost')
    monkeypatch.setenv('BSCONFIG_IPORT', '1543')
    monkeypatch.setenv('BSCONFIG_IDIR', '.')
    monkeypatch.setenv('BSCONFIG_INAME', 'localhost:1543')
    e = envutil.make_instance_ctl_env('fake://url')
    assert e.orthogonal_tags == {'a_itype': 'base',
                                 'a_ctype': 'prestable',
                                 'a_tier': 'RRGTier0',
                                 'a_prj': 'web-main',
                                 'a_dc': 'sas',
                                 'a_geo': 'sas'}
    expected = '/db/bsconfig/webstate/its_shared_controls/localhost:1543-prestable-sas-base-web-main'
    assert expected == envutil.make_its_shared_storage_dir(e)

    monkeypatch.setenv('BSCONFIG_ITAGS', 'a_prj_web-main a_ctype_production')
    e = envutil.make_instance_ctl_env('fake://url')
    assert e.orthogonal_tags == {'a_itype': 'undefined',
                    'a_ctype': 'production',
                    'a_tier': 'undefined',
                    'a_prj': 'web-main',
                    'a_dc': 'undefined',
                    'a_geo': 'undefined'}
    expected = '/db/bsconfig/webstate/its_shared_controls/localhost:1543-production-undefined-undefined-web-main'
    assert expected == envutil.make_its_shared_storage_dir(e)

    monkeypatch.setenv('BSCONFIG_ITAGS', 'fake_tag fake_tag_2')
    e = envutil.make_instance_ctl_env('fake://url')
    assert e.orthogonal_tags == {'a_itype': 'undefined',
                    'a_ctype': 'undefined',
                    'a_tier': 'undefined',
                    'a_prj': 'undefined',
                    'a_dc': 'undefined',
                    'a_geo': 'undefined'}
    expected = '/db/bsconfig/webstate/its_shared_controls/localhost:1543-undefined-undefined-undefined-undefined'
    assert expected == envutil.make_its_shared_storage_dir(e)
