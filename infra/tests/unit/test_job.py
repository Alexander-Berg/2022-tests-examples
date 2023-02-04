# coding=utf-8

import shlex

import mock
import pytest
from yp_proto.yp.client.hq.proto import types_pb2

from instancectl.jobs.job import Job, ArgumentEvaluationError, ArgumentFormatError
from instancectl.lib import envutil
from instancectl import constants


MOCK_INSTALL_SCRIPT_RESTART_POLICY = {
    "backoff": constants.DEFAULT_INSTALL_SCRIPT_BACKOFF,
    "max_jitter": constants.DEFAULT_INSTALL_SCRIPT_MAX_JITTER,
    "delay": constants.DEFAULT_INSTALL_SCRIPT_MIN_DELAY,
    "max_delay": constants.DEFAULT_INSTALL_SCRIPT_MAX_DELAY,
    "max_tries": constants.DEFAULT_INSTALL_SCRIPT_MAX_TRIES,
}


ENV = envutil.InstanceCtlEnv(
    instance_port=6666,
    instance_name=None,
    instance_dir='.',
    instance_id=None,
    service_id=None,
    node_name=None,
    hostname=None,
    orthogonal_tags={},
    use_spec=False,
    hq_poll=False,
    hq_report=False,
    default_container_env={},
    hq_url=None,
    auto_tags=None,
    instance_tags_string='',
    yp_hq_spec=None,
    skip_fdatasync_config=False,
    hq_report_version=1,
    sd_url=None,
    mock_retry_sleeper_output=None,
    prepare_script_restart_policy=None,
    install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
)


@pytest.fixture
def job():
    return Job(
        spec=types_pb2.Container(name='fake'),
        env=ENV,
        minidump_sender=None,
        limits={},
        environment={},
        successful_start_timeout=0,
        install_script=None,
        restart_script=None,
        uninstall_script=None,
        args_to_eval={},
        rename_binary=None,
        coredump_probability=0,
        always_coredump=False,
        coredumps_dir=None,
        expand_spec=False,
        porto_mode=None,
        work_dir='',
        coredump_filemask='',
        core_files_limit=0,
        restart_sleeper=None
    )


@pytest.fixture()
def instance_ctl_env():
    return envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name=None,
        instance_dir='.',
        instance_id=None,
        service_id=None,
        node_name=None,
        hostname='test_host',
        orthogonal_tags={},
        use_spec=False,
        hq_poll=False,
        hq_report=False,
        default_container_env={'DEPLOY_ENGINE': 'YP_LITE'},
        hq_url=None,
        auto_tags=None,
        instance_tags_string='',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )


def test_pid_store(job):
    job._store_pid(1543)
    with open(job.pid_file_path) as fd:
        pid = fd.readline().strip()
    assert pid == '1543'


def test_hardlink(tmpdir, job):
    respath = tmpdir.join('test')
    respath.write('OK')
    newres = respath.dirpath().join('new_test')

    assert job._hardlink_file(respath.strpath, newres.strpath)
    assert respath.samefile(newres)

    assert job._hardlink_file(respath.strpath, newres.strpath)
    assert respath.samefile(newres)

    fake_file = respath.dirpath('fake')
    try:
        job._hardlink_file(fake_file.strpath, newres.strpath)
    except EnvironmentError:
        pass


def test_argument_evaluation():
    args_to_eval = {
        'test': 'echo $((1542+1)); echo "ZZZ" >&1',
        'timeout': 'sleep 100',
        'crash': 'echo "1543"; exit 1',
        'multiline': 'echo "1543"; echo "ZZZ" >&1; echo 57',
    }
    job = Job(
        spec=types_pb2.Container(name='fake'),
        env=ENV,
        minidump_sender=None,
        limits={},
        environment={},
        successful_start_timeout=0,
        install_script=None,
        restart_script=None,
        uninstall_script=None,
        args_to_eval=args_to_eval,
        rename_binary=None,
        coredump_probability=0,
        always_coredump=False,
        coredumps_dir=None,
        expand_spec=False,
        porto_mode=None,
        work_dir='',
        coredump_filemask='',
        core_files_limit=0,
        restart_sleeper=None
    )

    assert job.eval_argument('test', args_to_eval['test']) == '1543'

    try:
        job.eval_argument('timeout', args_to_eval['timeout'])
    except ArgumentEvaluationError:
        pass

    try:
        job.eval_argument('crash', args_to_eval['crash'])
    except ArgumentEvaluationError:
        pass

    assert job.eval_argument('multiline', args_to_eval['multiline']) == '1543'


def test_eval_arguments():
    args_to_eval = {
        'test1': 'echo 1543; echo 57;',
        'test2': 'echo 1543 >&2; echo 57;',
    }

    job = Job(
        spec=types_pb2.Container(name='fake'),
        env=ENV,
        minidump_sender=None,
        limits={},
        environment={},
        successful_start_timeout=0,
        install_script=None,
        restart_script=None,
        uninstall_script=None,
        args_to_eval=args_to_eval,
        rename_binary=None,
        coredump_probability=0,
        always_coredump=False,
        coredumps_dir=None,
        expand_spec=False,
        porto_mode=None,
        work_dir='',
        coredump_filemask='',
        core_files_limit=0,
        restart_sleeper=None
    )

    assert job.eval_arguments(shlex.split('Z {test1} Z {test2} Z'), args_to_eval) == ['Z', '1543', 'Z', '57', 'Z']

    try:
        job.eval_arguments(shlex.split('Z {test1} Z {test2} Z {NON_EXISTANT} Z'), args_to_eval)
        assert False
    except ArgumentFormatError:
        pass


def test_store_pid_ignore_errors(monkeypatch, job):
    m = mock.Mock()
    m.side_effect = ValueError
    monkeypatch.setattr('sepelib.util.fs.makedirs_ignore', m)
    with pytest.raises(ValueError):
        job._store_pid(123)
    job._store_pid_ignore_errors(123)


def test_make_core_command(job, instance_ctl_env):
    core_spec = types_pb2.CoredumpProcessor(
        path='test_path',
        probability=1,
        count_limit=1,
        total_size_limit=555,
        aggregator=types_pb2.CoredumpAggregator(
            saas=types_pb2.SaasCoredumpAggregator(
                url='test_saas_url', gdb=types_pb2.CoredumpAggregatorGdb(exec_path='test_gdb_path')
            )
        ),
    )
    coredump_policy = types_pb2.CoredumpPolicy(type=1, coredump_processor=core_spec)
    spec = types_pb2.Container(name='fake', coredump_policy=coredump_policy)
    job.spec = spec
    job._env = instance_ctl_env
    assert '--instance-name=test_host' in job._make_core_command()
