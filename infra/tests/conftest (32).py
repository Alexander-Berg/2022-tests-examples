import logging
import os
import psutil
import pytest
import re
import time
import yaml
import yatest.common
import yp.data_model as data_model
import yt_proto.yt.core.yson.proto.protobuf_interop_pb2 as yson_protobuf_interop
import yt.yson as yson

from infra.dctl.src.consts import CLUSTER_CONFIGS, PRODUCTION_CLUSTERS
from infra.dctl.src.lib import project, stage, yp_client, cliutil
from library.python import resource as rs
from yp.client import find_token
from yp.common import YpNoSuchObjectError
from yt.wrapper import YtClient
from yt.wrapper.errors import YtTabletTransactionLockConflict

from utils import YpOrchidClient, await_project_ownership

STAGE_ID = 'deploy-test-stage-'
TEMPORARY_YP_ACCOUNT = 'tmp'
DEFAULT_PROJECT_NAME = 'deploy-it-tests'
DEFAULT_NETWORK_ID = '_STAGECTL_TEST_NETS_'

DEPLOY_UNIT_ID = 'deploy-test-unit'
BOX_ID = 'deploy-test-box'
DEFAULT_DEPLOY_UNIT_NAME = 'it-test'
DEFAULT_DOCKER_IMAGE_NAME = 'it-test-image-name'
DEFAULT_DOCKER_IMAGE_TAG = 'it-test-image-tag'
DEFAULT_RESOURCE_ID = 'it-test-resource-id'
DEFAULT_LAYER_ID = 'it-test-layer-id'
DEFAULT_SKYNET_ID = 'it-test-skynet-id'

GC_LABEL_KEY = 'stagectl-it-gc-owner'

logging.basicConfig(
    format='%(asctime)s %(levelname)-8s %(message)s',
    level=logging.DEBUG,
    datefmt='%Y-%m-%d %H:%M:%S')
yaml_loader = getattr(yaml, 'CSafeLoader', yaml.SafeLoader)


def pytest_addoption(parser):
    cluster_choices = list(CLUSTER_CONFIGS.keys())

    group = parser.getgroup('YP', 'YP-specific settings')
    group.addoption('--user', dest='user', default=None, help='User used as ACL subject')
    group.addoption('--cluster', dest='cluster', default=None, choices=cluster_choices, help='YP cluster')
    group.addoption('--no-cleanup', dest='clean', default=True, action='store_false',
                    help='Do project/stage gc at the end of tests')
    group.addoption('--gc-label', dest='gc_label', default=None, help='GC label')
    group.addoption('--gc-max-object-age', dest='gc_max_object_age_minutes', default=None,
                    help='Maximum allowed garbage age (in minutes)')
    group.addoption('--enable-monitoring', dest='enable_monitoring', default=None,
                    help='Enables monitoring support')


def _gc_objects(client, object_type, gc_label, max_object_age_minutes):
    # TODO: unfortunately this is the only reliable
    # way to find pre-fork tests start time
    p = psutil.Process(os.getppid())
    while not p.name().startswith('ya-bin'):
        p = p.parent()
        assert p.pid != 1, 'Unable to locate ya tool in process tree'

    creation_time = p.create_time() - max_object_age_minutes * 60

    query = f'[/labels/{GC_LABEL_KEY}] = "{gc_label}" and [/meta/creation_time] < {int(creation_time * 1e6)}u'

    def get_garbage_objects():
        return client.list(object_type=object_type, user=None, query=query, limit=100)

    objects = get_garbage_objects()
    while objects:
        for object in objects:
            object_id = object.meta.id
            object_name = data_model.EObjectType.DESCRIPTOR.values_by_number[object_type].GetOptions().Extensions[
                yson_protobuf_interop.enum_value_name
            ]

            logging.info("Removing garbage %s %s", object_name, object_id)
            try:
                client.remove(object_type=object_type, object_id=object_id)
            except (YpNoSuchObjectError, YtTabletTransactionLockConflict):
                logging.warning("Unable to remove garbage %s %s", object_name, object_id)
        objects = get_garbage_objects()


@pytest.fixture(scope="session", autouse=True)
def gc_fixture(yp_client_fixture, yp_xdc_client_fixture,
               gc_label, gc_max_object_age_minutes,
               do_cleanup, request):
    if not do_cleanup:
        return

    def remove_garbage():
        _gc_objects(yp_xdc_client_fixture, data_model.OT_STAGE, gc_label, gc_max_object_age_minutes)
        _gc_objects(yp_xdc_client_fixture, data_model.OT_PROJECT, gc_label, gc_max_object_age_minutes)

    remove_garbage()
    request.addfinalizer(remove_garbage)


@pytest.fixture(scope='session')
def gc_max_object_age_minutes(yp_user, request):
    gc_max_object_age_minutes = yatest.common.get_param(
        'max_gc_age_minutes', request.config.option.gc_max_object_age_minutes)
    return int(gc_max_object_age_minutes or 1)


@pytest.fixture(scope='session')
def gc_label(yp_user, yp_cluster, request):
    gc_label = yatest.common.get_param('gc_label', request.config.option.gc_label)
    return '%s-%s' % (yp_cluster, gc_label or yp_user)


@pytest.fixture(scope='session')
def yp_orchid_client(yp_xdc_cluster):
    if yatest.common.get_param('use_cluster_state_timestamp', False):
        yt_token = os.getenv('YT_TOKEN')
        yt_client = YtClient(f'yp-{yp_xdc_cluster}.yt.yandex.net', token=yt_token)
        return YpOrchidClient(yt_client)
    return None


@pytest.fixture(scope='session')
def yp_user(request):
    default_yp_user = yatest.common.get_param('yp_user', os.getenv('YP_USER', cliutil.get_user()))
    return request.config.option.user or default_yp_user


@pytest.fixture(scope='session')
def yp_cluster(request):
    default_cluster = yatest.common.get_param('cluster', 'man-pre')
    return request.config.option.cluster or default_cluster


@pytest.fixture(scope='session')
def monitoring_enabled(request):
    default_value = yatest.common.get_param('enable_monitoring', False)
    return request.config.option.enable_monitoring or default_value


@pytest.fixture(scope='session')
def do_cleanup():
    return yatest.common.get_param('clear', 'true') == 'true'


@pytest.fixture(scope='session')
def account_id():
    return yatest.common.get_param('account_id', TEMPORARY_YP_ACCOUNT)


@pytest.fixture(scope='session')
def deploy_engine():
    return yatest.common.get_param('deploy_engine', 'env_controller')


@pytest.fixture(scope='function')
def suffix_fixture(yp_user):
    name = re.sub(r"[^a-z0-9\-]+", "", yp_user.lower())
    return "%d-%s" % (time.time() * 10e9, name)


@pytest.fixture(scope='session')
def yp_client_fixture(request, yp_user, yp_cluster):
    return _create_yp_client_fixture(request, yp_user, yp_cluster)


@pytest.fixture(scope='session')
def yp_xdc_client_fixture(request, yp_client_fixture, yp_user, yp_cluster, yp_xdc_cluster):
    if yp_cluster in PRODUCTION_CLUSTERS:
        return _create_yp_client_fixture(request, yp_user, yp_xdc_cluster)
    return yp_client_fixture


@pytest.fixture(scope='session')
def yp_xdc_cluster(yp_cluster):
    if yp_cluster in PRODUCTION_CLUSTERS:
        return 'xdc'
    return yp_cluster


def create_deploy_unit(resource_id=DEFAULT_RESOURCE_ID,
                       resource_url=DEFAULT_SKYNET_ID,
                       docker_image_name=DEFAULT_DOCKER_IMAGE_NAME,
                       docker_image_tag=DEFAULT_DOCKER_IMAGE_TAG,
                       layer_id=DEFAULT_LAYER_ID,
                       layer_url=DEFAULT_SKYNET_ID):
    unit = data_model.TDeployUnitSpec()
    unit.images_for_boxes[BOX_ID].name = docker_image_name
    unit.images_for_boxes[BOX_ID].tag = docker_image_tag

    unit.network_defaults.network_id = DEFAULT_NETWORK_ID
    spec = unit.replica_set.replica_set_template.pod_template_spec.spec.pod_agent_payload.spec
    box = spec.boxes.add()
    box.id = BOX_ID

    resources = spec.resources
    static_resources = resources.static_resources.add()
    static_resources.id = resource_id
    static_resources.url = resource_url

    layer = resources.layers.add()
    layer.id = layer_id
    layer.url = layer_url

    return unit


@pytest.fixture(scope='function')
def project_name(suffix_fixture):
    return DEFAULT_PROJECT_NAME + '-' + suffix_fixture


@pytest.fixture(scope='function')
def project_fixture(request, project_name, yp_user,
                    yp_xdc_client_fixture, yp_orchid_client,
                    account_id, do_cleanup, gc_label):
    def create_project():
        pr_yaml = rs.find('/project.yml').decode("utf-8")
        pr_dict = yaml.load(pr_yaml, Loader=yaml_loader)
        pr = project.cast_yaml_dict_to_yp_object(pr_dict)

        pr.meta.id = project_name
        pr.meta.account_id = account_id
        pr.spec.account_id = account_id

        labels = pr.labels.attributes.add()
        labels.key = GC_LABEL_KEY
        labels.value = yson.dumps(gc_label)

        yp_xdc_client_fixture.create(object_type=data_model.OT_PROJECT, obj=pr, create_with_acl=True, add_default_user_acl=True)

        await_project_ownership(project_name, yp_user, yp_xdc_client_fixture, None)
        time.sleep(30)
        logging.info('Created project %s', project_name)

        return project_name

    def delete_project():
        try:
            project.remove(project_name, yp_xdc_client_fixture)
            logging.info('Deleted project %s', project_name)
        except (YpNoSuchObjectError, YtTabletTransactionLockConflict):
            logging.warning("Unable to remove project %s", project_name)

    if do_cleanup:
        request.addfinalizer(delete_project)
    return create_project()


@pytest.fixture(scope='function')
def stage_labels():
    return {}


@pytest.fixture(scope='function')
def stage_template():
    return '/stage.yml'


@pytest.fixture(scope='function')
def stage_spec(stage_name,
               stage_template,
               project_fixture,
               yp_cluster,
               gc_label,
               account_id,
               stage_labels,
               deploy_engine,
               monitoring_enabled):
    runtime_version = yatest.common.get_param('runtime_revision', 11)
    stage_yaml = rs.find(stage_template).decode("utf-8") % {'yp_cluster': yp_cluster}
    stage_dict = yaml.load(stage_yaml, Loader=yaml_loader)

    stage_dict['labels'] = stage_labels
    stage_dict['labels'][GC_LABEL_KEY] = gc_label
    stage_dict['labels']['deploy_engine'] = deploy_engine
    _assert_network_id(stage_dict, DEFAULT_NETWORK_ID)

    if not monitoring_enabled:
        _disable_monitoring(stage_dict)

    _enforce_quotas(stage_dict)

    _patch_runtime_version(stage_dict, runtime_version)

    st = stage.cast_yaml_dict_to_yp_object(stage_dict)
    st.meta.id = stage_name
    st.meta.account_id = account_id
    st.meta.project_id = project_fixture
    cliutil.clear_not_initializable_fields(st)
    stage.validate(st)
    return st


@pytest.fixture(scope='function')
def stage_fixture(request,
                  stage_name,
                  yp_xdc_client_fixture,
                  project_fixture,
                  stage_spec,
                  stage_labels,
                  do_cleanup,
                  yp_cluster):
    def create_stage():
        logging.info('Creating stage %s', stage_name)
        created_stage = stage.put(stage_spec, yp_cluster, None, None, None, yp_xdc_client_fixture)
        logging.info('Created stage %s', stage_name)
        return created_stage

    def delete_stage():
        logging.info('Deleting stage %s', stage_name)
        try:
            stage.remove(stage_name, yp_xdc_client_fixture)
        except YpNoSuchObjectError:
            logging.info("Stage %s doesn't exist", stage_name)
            return
        except YtTabletTransactionLockConflict:
            logging.info("Stage %s conflict removing", stage_name)
            return
        logging.info('Deleted stage %s', stage_name)

    if do_cleanup:
        request.addfinalizer(delete_stage)
    return create_stage()


def _enforce_quotas(stage_dict):
    default_quota_policy = {
        'capacity': 2147483648,  # 2G
        'bandwidth_guarantee': 524288,  # 512k/s
        'bandwidth_limit': 1048576,  # 1M/s
    }
    default_resource_requests = {
        'memory_guarantee': 536870912,  # 512M
        'memory_limit': 536870912,  # 512M
        'vcpu_guarantee': 100,  # 0.1vcpu
        'vcpu_limit': 100,  # 0.1vcpu
        'network_bandwidth_guarantee': 10485760,  # 10.0 MB/s
    }

    def _patch_resources_and_volumes(du_id, replica_set_dict):
        pod_spec = replica_set_dict.get('pod_template_spec', {}).get('spec', {'resource_requests': {}})
        for volume_request in pod_spec.get('disk_volume_requests', []):
            for key, value in default_quota_policy.items():
                requested_value = volume_request['quota_policy'].get(key, value)
                if requested_value > value:
                    logging.warning(f'!!! You are using non-standard disk quota {key} ({requested_value} > {value}) for '
                                    f'deploy unit {du_id}. Make sure you know what are you doing and take time '
                                    f'to re-check quota or default values.')
            volume_request['quota_policy'] = default_quota_policy

        for key, value in default_resource_requests.items():
            requested_value = pod_spec['resource_requests'].get(key, value)
            if requested_value > value:
                logging.warning(f'!!! You are using non-standard resource request {key} ({requested_value} > {value}) for '
                                f'deploy unit {du_id}. Make sure you know what are you doing and take time '
                                f'to re-check quota or default values.')

        pod_spec['resource_requests'] = default_resource_requests

    _patch_replica_sets(stage_dict, _patch_resources_and_volumes)


def _patch_runtime_version(stage_dict, version):
    for du_id, du_spec in stage_dict.get('spec', {}).get('deploy_units', {}).items():
        du_spec['patchers_revision'] = version


def _assert_network_id(stage_dict, expected_network_id):
    for du_id, du_spec in stage_dict.get('spec', {}).get('deploy_units', {}).items():
        du_network_id = du_spec.get('network_defaults', {}).get('network_id', expected_network_id)
        assert du_network_id == expected_network_id, f"Unexpected network_defaults.network_id for {du_id}"

        ip6_requests = du_spec.get('replica_set', {}) \
            .get('replica_set_template', {}) \
            .get('pod_template_spec', {}) \
            .get('spec', {}).get('ip6_address_requests', [])

        for ip6_request in ip6_requests:
            ip6_network_id = ip6_request.get('network_id', expected_network_id)
            assert ip6_network_id == expected_network_id, f"Unexpected ip6_address_requests.network_id for {du_id}"


def _disable_monitoring(stage_dict):
    _patch_replica_sets(stage_dict, _patch_replica_set_monitoring, {
        'ctype': 'test',
        'itype': 'stagectl-it'
    })


def _patch_replica_set_monitoring(du_id, replica_set_dict, labels, pod_agent_signals=False, enable_workloads=False):
    pod_spec = replica_set_dict.get('pod_template_spec', {}).get('spec', {})
    workloads = []
    for workload in pod_spec.get('pod_agent_payload', {}).get('spec', {}).get('workloads', []):
        workload_id = workload.get('id')

        workloads.append({
            'labels': labels,
            'workload_id': workload_id
        })

    monitoring_spec = {}
    if 'host_infra' not in pod_spec:
        pod_spec['host_infra'] = {'monitoring': monitoring_spec}
    else:
        pod_spec['host_infra']['monitoring'] = monitoring_spec
    monitoring_spec['labels'] = labels
    # should be enabled after DEPLOY-2990 release
    if enable_workloads:
        monitoring_spec['workloads'] = workloads
    monitoring_spec['pod_agent'] = {
        'labels': labels,
        'add_pod_agent_user_signals': pod_agent_signals,
    }


def _patch_replica_sets(stage_dict, function, *args, **kwargs):
    for du_id, du_spec in stage_dict.get('spec', {}).get('deploy_units', {}).items():
        function(du_id, du_spec.get('multi_cluster_replica_set', {}).get('replica_set', {}), *args, **kwargs)
        function(du_id, du_spec.get('replica_set', {}).get('replica_set_template', {}), *args, **kwargs)


def _create_yp_client_fixture(request, user, cluster):
    logging.info('Creating client for cluster %s', cluster)
    client = yp_client.YpClient(cluster, find_token(), user)
    request.addfinalizer(client.close)
    return client
