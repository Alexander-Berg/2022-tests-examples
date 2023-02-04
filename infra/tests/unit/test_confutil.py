from __future__ import unicode_literals

import pytest
from yp_proto.yp.client.hq.proto import types_pb2
from instancectl import errors
from instancectl import constants
from instancectl.lib import confutil
from instancectl.lib import specutil
from instancectl.lib import envutil
from instancectl.lib.process.porto_container import PortoMode, VirtMode
from instancectl.config.config import defaults

MOCK_INSTALL_SCRIPT_RESTART_POLICY = {
    "backoff": constants.DEFAULT_INSTALL_SCRIPT_BACKOFF,
    "max_jitter": constants.DEFAULT_INSTALL_SCRIPT_MAX_JITTER,
    "delay": constants.DEFAULT_INSTALL_SCRIPT_MIN_DELAY,
    "max_delay": constants.DEFAULT_INSTALL_SCRIPT_MAX_DELAY,
    "max_tries": constants.DEFAULT_INSTALL_SCRIPT_MAX_TRIES,
}


def test_porto_mode_option_setting():
    # Case 1: start without subcontainers
    spec = types_pb2.Container()
    env = envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    rev = types_pb2.InstanceRevision()
    sleeper = specutil.create_restart_sleeper(spec)
    job = confutil.make_job_from_spec(
        spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
    )
    assert job.porto_mode == PortoMode(enabled=False, isolate='false', virt_mode=VirtMode.APP)

    # Case 2: run_as_user given
    spec = types_pb2.Container()
    spec.security_context.run_as_user = 'someuser'
    env = envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    rev = types_pb2.InstanceRevision()
    sleeper = specutil.create_restart_sleeper(spec)
    job = confutil.make_job_from_spec(
        spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
    )
    assert job.porto_mode == PortoMode(enabled=True, isolate='false', virt_mode=VirtMode.APP)

    # Case 3: core command given
    spec = types_pb2.Container()
    spec.coredump_policy.type = types_pb2.CoredumpPolicy.COREDUMP
    env = envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    rev = types_pb2.InstanceRevision()
    sleeper = specutil.create_restart_sleeper(spec)
    job = confutil.make_job_from_spec(
        spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
    )
    assert job.porto_mode == PortoMode(enabled=True, isolate='false', virt_mode=VirtMode.APP)

    # Case 4: resource limits given
    spec = types_pb2.Container()
    spec.resource_allocation.limit.add()
    env = envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    rev = types_pb2.InstanceRevision()
    sleeper = specutil.create_restart_sleeper(spec)
    job = confutil.make_job_from_spec(
        spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
    )
    assert job.porto_mode == PortoMode(enabled=True, isolate='false', virt_mode=VirtMode.APP)

    # Case 5: explicit porto toggle off
    for policy in ('false', 'none'):
        spec = types_pb2.Container()
        spec.coredump_policy.type = types_pb2.CoredumpPolicy.COREDUMP
        spec.security_context.porto_access_policy = policy
        env = envutil.InstanceCtlEnv(
            instance_port=6666,
            instance_name='fake-instance',
            instance_dir='.',
            instance_id='fake-instance',
            service_id='fake-service',
            node_name='localhost',
            hostname='localhost',
            orthogonal_tags={},
            use_spec=True,
            hq_poll=True,
            hq_report=True,
            default_container_env={'SOME_KEY': 'SOME_VALUE'},
            hq_url='http://localhost/',
            auto_tags=['a', 'b', 'c'],
            instance_tags_string='a b c',
            yp_hq_spec=None,
            skip_fdatasync_config=False,
            hq_report_version=1,
            sd_url=None,
            mock_retry_sleeper_output=None,
            prepare_script_restart_policy=None,
            install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
        )
        rev = types_pb2.InstanceRevision()
        sleeper = specutil.create_restart_sleeper(spec)
        job = confutil.make_job_from_spec(
            spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
        )
        assert job.porto_mode == PortoMode(enabled=True, isolate='false', virt_mode=VirtMode.APP)

    # Case 6: AppContainer
    spec = types_pb2.Container()
    env = envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    rev = types_pb2.InstanceRevision()
    rev.type = types_pb2.InstanceRevision.APP_CONTAINER
    sleeper = specutil.create_restart_sleeper(spec)
    job = confutil.make_job_from_spec(
        spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
    )
    assert job.porto_mode == PortoMode(enabled=True, isolate='false', virt_mode=VirtMode.APP)

    # Case 7: OS Container with wrong policy
    spec = types_pb2.Container(name='os')
    env = envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    rev = types_pb2.InstanceRevision()
    rev.type = types_pb2.InstanceRevision.OS_CONTAINER
    with pytest.raises(errors.ContainerSpecProcessError):
        sleeper = specutil.create_restart_sleeper(spec)
        job = confutil.make_job_from_spec(
            spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
        )

    # Case 8: OS Container
    spec = types_pb2.Container(name='os')
    spec.security_context.porto_access_policy = 'isolate'
    env = envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    rev = types_pb2.InstanceRevision()
    rev.type = types_pb2.InstanceRevision.OS_CONTAINER
    sleeper = specutil.create_restart_sleeper(spec)
    job = confutil.make_job_from_spec(
        spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
    )
    assert job.porto_mode == PortoMode(enabled=True, isolate='true', virt_mode=VirtMode.OS)

    # Case 9: Juggler alongside OS container
    spec = types_pb2.Container(name='juggler')
    env = envutil.InstanceCtlEnv(
        instance_port=6666,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    rev = types_pb2.InstanceRevision()
    rev.type = types_pb2.InstanceRevision.OS_CONTAINER
    sleeper = specutil.create_restart_sleeper(spec)
    job = confutil.make_job_from_spec(
        spec, env, rev, sleeper, constants.DEFAULT_CONTAINER_RESTART_SUCCESSFUL_START_TIMEOUT
    )
    assert job.porto_mode == PortoMode(enabled=False, isolate='false', virt_mode=VirtMode.APP)


def test_make_job_ctrl_from_config():
    env = envutil.InstanceCtlEnv(
        instance_port=123,
        instance_name='fake-instance',
        instance_dir='.',
        instance_id='fake-instance',
        service_id='fake-service',
        node_name='localhost',
        hostname='localhost',
        orthogonal_tags={a: a for a in constants.ITS_STATE_VOLUME_NAME_TAG_LIST},
        use_spec=True,
        hq_poll=True,
        hq_report=True,
        default_container_env={'SOME_KEY': 'SOME_VALUE'},
        hq_url='http://localhost/',
        auto_tags=['a', 'b', 'c'],
        instance_tags_string='a b c',
        yp_hq_spec=None,
        skip_fdatasync_config=False,
        hq_report_version=1,
        sd_url=None,
        mock_retry_sleeper_output=None,
        prepare_script_restart_policy=None,
        install_script_restart_policy=MOCK_INSTALL_SCRIPT_RESTART_POLICY,
    )
    conf = {
        "jobs": {},
        "globals": defaults.global_options
    }
    spec = types_pb2.InstanceRevision(container=[types_pb2.Container(name='fake')])
    with pytest.raises(errors.ContainerSpecProcessError):
        confutil.make_job_ctrl_from_config(
            conf, spec, env, None,
        )
    spec = types_pb2.InstanceRevision(container=[types_pb2.Container(name='skynet')])
    jobpool = confutil.make_job_ctrl_from_config(
        conf, spec, env, None,
    )
    assert len(jobpool.job_pool.jobs) == 1
