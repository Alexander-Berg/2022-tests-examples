import os

import inject
import mock
import pytest
import yatest
import uuid

from yt import yson
import yp.data_model as data_model
from sepelib.core import config as sepelibconfig
from yp_proto.yp.client.api.proto import cluster_api_pb2

from infra.qyp.proto_lib import vmset_pb2
from infra.qyp.vmproxy.src.lib.yp import yputil
from infra.swatlib.auth import abc, staff
from infra.qyp.vmproxy.src.lib import rt
from infra.qyp.vmproxy.src.lib import abc_roles
from infra.qyp.vmproxy.src.lib.heartbeat_client import HeartBeatClient
from infra.swatlib.rpc.authentication import AuthSubject


POD_ID = 'real-pod-id'
SERVICE_MACRO = '_VMAGENTNETS_'
ROBOT_LOGIN = 'robot-vmagent-rtc'
ROOT_SUBJECTS = frozenset([
    'abc:service-scope:3389:8',
    'abc:service-scope:2900:8',
    'abc:service:2285',
    ROBOT_LOGIN,
])
DUTY_SUBJECT = 'abc:service-scope:2900:16'
LOGIN = 'author'
MIN_ROOT_FS_LIMIT = 10 * 1024 ** 2


def source_path(path):
    try:
        return yatest.common.source_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], path)


def load_config():
    config_path = source_path("infra/qyp/vmproxy/cfg_default.yml")
    sepelibconfig.load(config_path, config_context={
        'debug': True,
        'disable_auth': True
    })


@pytest.fixture()
def default_vmagent_version():
    return '0.28'


@pytest.fixture(autouse=True)
def config(default_vmagent_version):
    load_config()
    sepelibconfig.set_value('vmproxy.default_vmagent.version', default_vmagent_version)
    sepelibconfig.set_value('vmproxy.pass_ssh_keys', False)
    return sepelibconfig


@pytest.fixture(autouse=True)
def patch_can_user_set_resources():
    with mock.patch('infra.qyp.vmproxy.src.lib.yp.yputil.can_user_change_pod_resources', return_value=False):
        yield


@pytest.fixture(autouse=True)
def patch_uuid():
    with mock.patch('uuid.uuid4', return_value='version') as uuid_mock:
        yield uuid_mock


@pytest.fixture()
def call():
    def _call(method, req, auth_subject=AuthSubject('anonymous')):
        return method.handler(req, auth_subject)

    return _call


@pytest.fixture
def user_login():
    return 'author'


@pytest.fixture
def vm(config, patch_uuid, user_login):
    """

    :type config:
    :type patch_uuid:
    :type user_login: str
    :rtype: vmset_pb2.VM
    """
    vm = vmset_pb2.VM()
    vm.meta.id = POD_ID
    vm.meta.auth.owners.logins.append(user_login)
    vm.meta.author = user_login
    vm.spec.account_id = 'tmp'
    vm.spec.qemu.autorun = True
    vm.spec.qemu.vm_type = vmset_pb2.LINUX
    vm.spec.qemu.network_id = '_BROOKLYN_NETS_'
    vm.spec.qemu.node_segment = 'default'  # dev_node_segment
    vm.spec.qemu.resource_requests.dirty_memory_limit = 1
    vm.spec.qemu.resource_requests.anonymous_memory_limit = 3
    vm.spec.qemu.resource_requests.memory_guarantee = 3 * 1024 ** 3
    vm.spec.qemu.resource_requests.network_bandwidth_guarantee = 2 * 1024 ** 3
    vm.spec.qemu.resource_requests.memory_limit = 3 * 1024 ** 3
    vm.spec.qemu.resource_requests.vcpu_guarantee = 5000
    vm.spec.qemu.resource_requests.vcpu_limit = 5000
    vm.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm.spec.qemu.io_guarantees_per_storage['ssd'] = 90 * 1024 ** 2
    vm.spec.vmagent_version = config.get_value('vmproxy.default_vmagent.version')
    v = vm.spec.qemu.volumes.add()
    v.name = yputil.MAIN_VOLUME_NAME
    v.capacity = 10 * 1024 ** 3
    v.storage_class = 'ssd'
    v.resource_url = 'rbtorrent:testing'
    v.pod_mount_path = yputil.MAIN_VOLUME_POD_MOUNT_PATH
    v.vm_mount_path = yputil.MAIN_VOLUME_VM_MOUNT_PATH
    return vm


@pytest.fixture
def vm_qdm(vm, config, patch_uuid, user_login):
    vm.spec.qemu.volumes[0].resource_url = 'qdm:testing'
    return vm


@pytest.fixture()
def gen_expected_iss_payload(config, vm):
    def _gen_iss_payload(pod):
        """
        :type pod: data_model.TPod
        :rtype: cluster_api_pb2.HostConfiguration
        """
        yp_cluster = config.get_value('yp.default_cluster')
        conf = '{}-{}'.format(vm.meta.id, 0)
        vmagent_resource = 'vmagent'
        vmagent_itype = 'qemuvm'
        vmagent_ctype = 'prod'
        vmagent_yp_tier = 'yp'
        layer_url = config.get_value('vmproxy.default_porto_layer.url')

        iss_proto = cluster_api_pb2.HostConfiguration()
        i = iss_proto.instances.add()
        i.id.slot.service = vm.meta.id
        i.id.configuration.groupId = 'qyp'
        i.id.configuration.groupStateFingerprint = conf
        i.dynamicProperties.update({
            'HBF_NAT': 'disabled',
            'SKYNET_SSH': 'enabled',
            "nanny_container_access_url": config.get_value('vmproxy.check_access_url', ''),
        })
        yp_cluster = yp_cluster.lower()
        tags_template = 'a_geo_{geo} a_dc_{dc} a_itype_{itype} a_ctype_{ctype} a_prj_{prj} a_metaprj_unknown a_tier_{tier}'
        vmagent_port = str(config.get_value('vmproxy.default_agent_port'))
        i.properties.update({
            'INSTANCE_TAG_CTYPE': vmagent_ctype,
            'INSTANCE_TAG_ITYPE': vmagent_itype,
            'INSTANCE_TAG_PRJ': vm.meta.id,
            'INSTANCE_TAG_TIER': vmagent_yp_tier,
            'POD_ID': POD_ID,
            'STORAGE_PATH': yputil.MAIN_VOLUME_POD_MOUNT_PATH,
            'HOSTNAME': '{}.{}.yp-c.yandex.net'.format(vm.meta.id, yp_cluster.lower()),
            'PORT': vmagent_port,
            'VMAGENT_PATH': './{}/vmagent'.format(vmagent_resource),
            'tags': tags_template.format(geo=yp_cluster,
                                         dc=yp_cluster,
                                         itype=vmagent_itype,
                                         ctype=vmagent_ctype,
                                         prj=vm.meta.id,
                                         tier=vmagent_yp_tier),
            'yasmUnistatFallbackPort': vmagent_port,
            'NANNY_SERVICE_ID': vm.meta.id,
            'VMCTL_FORCE_DIRECT_CONNECTION': 'True'
        })
        if vm.spec.qemu.use_nat64:
            i.properties['USE_NAT64'] = 'true'
        i.targetState = 'ACTIVE'
        i.entity.instance.storage = yputil.STORAGE_CLASS_MAP[vm.spec.qemu.volumes[0].storage_class]
        i.entity.instance.container.constraints.update({
            'meta.enable_porto': 'isolate',
            'iss_hook_start.enable_porto': 'isolate',
            "iss_hook_start.capabilities_ambient": "NET_BIND_SERVICE",
            "iss_hook_start.net": "inherited",
        })

        # Hook timelimits
        iss_hook_status_tl = i.entity.instance.timeLimits['iss_hook_status']
        iss_hook_status_tl.restartPeriodScaleMs = config.get_value('hooks.limits.default.restart_period_scale') * 1000
        iss_hook_status_tl.restartPeriodBackOff = config.get_value('hooks.limits.default.restart_period_backoff')
        iss_hook_status_tl.maxRestartPeriodMs = config.get_value('hooks.limits.default.max_restart_period') * 1000
        iss_hook_status_tl.minRestartPeriodMs = config.get_value('hooks.limits.default.min_restart_period') * 1000
        iss_hook_status_tl.maxExecutionTimeMs = config.get_value('hooks.limits.default.max_execution_time') * 1000

        iss_hook_stop_tl = i.entity.instance.timeLimits['iss_hook_stop']
        iss_hook_stop_tl.maxExecutionTimeMs = config.get_value('hooks.limits.iss_hook_stop.max_execution_time') * 1000

        # Resources
        hook_url = yputil.make_encoded_resource_url(
            config.get_value('vmproxy.default_pod_resources.iss_hook_start.content'))
        iss_hook_start = i.entity.instance.resources['iss_hook_start']
        iss_hook_start.resource.uuid = yputil.make_resource_uuid(hook_url)
        iss_hook_start.resource.urls.append(hook_url)
        iss_hook_start.resource.verification.checkPeriod = '0d0h0m'
        iss_hook_start.resource.verification.checksum = 'EMPTY:'

        hook_url = yputil.make_encoded_resource_url(
            config.get_value('vmproxy.default_pod_resources.iss_hook_status.content'))
        iss_hook_status = i.entity.instance.resources['iss_hook_status']
        iss_hook_status.resource.uuid = yputil.make_resource_uuid(hook_url)
        iss_hook_status.resource.urls.append(hook_url)
        iss_hook_status.resource.verification.checkPeriod = '0d0h0m'
        iss_hook_status.resource.verification.checksum = 'EMPTY:'

        hook_url = yputil.make_encoded_resource_url(
            config.get_value('vmproxy.default_pod_resources.iss_hook_notify.content'))
        iss_hook_notify = i.entity.instance.resources['iss_hook_notify']
        iss_hook_notify.resource.uuid = yputil.make_resource_uuid(hook_url)
        iss_hook_notify.resource.urls.append(hook_url)
        iss_hook_notify.resource.verification.checkPeriod = '0d0h0m'
        iss_hook_notify.resource.verification.checksum = 'EMPTY:'

        hook_url = yputil.make_encoded_resource_url(
            config.get_value('vmproxy.default_pod_resources.iss_hook_stop.content'))
        iss_hook_stop = i.entity.instance.resources['iss_hook_stop']
        iss_hook_stop.resource.uuid = yputil.make_resource_uuid(hook_url)
        iss_hook_stop.resource.urls.append(hook_url)
        iss_hook_stop.resource.verification.checkPeriod = '0d0h0m'
        iss_hook_stop.resource.verification.checksum = 'EMPTY:'

        vm_config_id_url = yputil.make_encoded_resource_url(str(uuid.uuid4()))
        vm_config_id = i.entity.instance.resources['vm_config_id']
        vm_config_id.dynamicResource.uuid = yputil.make_resource_uuid(vm_config_id_url)
        vm_config_id.dynamicResource.urls.append(vm_config_id_url)
        vm_config_id.dynamicResource.verification.checkPeriod = '0d0h0m'
        vm_config_id.dynamicResource.verification.checksum = 'EMPTY:'

        vmagent_url = config.get_value('vmproxy.default_pod_resources.vmagent.url')
        vmagent = i.entity.instance.resources[vmagent_resource]
        vmagent.dynamicResource.uuid = yputil.make_resource_uuid(vmagent_url)
        vmagent.dynamicResource.urls.append(vmagent_url)
        vmagent.dynamicResource.verification.checkPeriod = '0d0h0m'
        vmagent.dynamicResource.verification.checksum = 'EMPTY:'

        vmctl_url = config.get_value('vmproxy.default_pod_resources.vmctl.url')
        vmctl = i.entity.instance.resources['vmctl']
        vmctl.dynamicResource.uuid = yputil.make_resource_uuid(vmctl_url)
        vmctl.dynamicResource.urls.append(vmctl_url)
        vmctl.dynamicResource.verification.checkPeriod = '0d0h0m'
        vmctl.dynamicResource.verification.checksum = 'EMPTY:'

        qemu_package_url = config.get_value('vmproxy.default_pod_resources.qemu_package.url')
        qemu_package = i.entity.instance.resources['qemu_package']
        qemu_package.dynamicResource.uuid = yputil.make_resource_uuid(qemu_package_url)
        qemu_package.dynamicResource.urls.append(qemu_package_url)
        qemu_package.dynamicResource.verification.checkPeriod = '0d0h0m'
        qemu_package.dynamicResource.verification.checksum = 'EMPTY:'

        # Volumes
        for volume_req in pod.spec.disk_volume_requests:
            labels_dict = yputil.cast_attr_dict_to_dict(volume_req.labels)
            volume_type = labels_dict['volume_type']
            v = i.entity.instance.volumes.add()
            v.mountPoint = labels_dict['mount_path']
            v.storage = yputil.STORAGE_CLASS_MAP[volume_req.storage_class]
            if volume_type == 'root_fs':
                v.quotaBytes = labels_dict['root_fs_snapshot_quota']
                v.quotaCwdBytes = labels_dict['work_dir_snapshot_quota']
                l = v.layers.add()
                l.uuid = yputil.make_resource_uuid(layer_url)
                l.storage = yputil.STORAGE_CLASS_MAP[volume_req.storage_class]
                l.verification.checkPeriod = '0d0h0m'
                l.verification.checksum = 'EMPTY:'
                l.urls.append(layer_url)
            elif volume_type == 'persistent':
                v.uuid = volume_req.id
                v.quotaBytes = volume_req.quota_policy.capacity
                v.quotaCwdBytes = volume_req.quota_policy.capacity

        # Skynet bind volume
        v = i.entity.instance.volumes.add()
        v.mountPoint = '/Berkanavt/supervisor'
        v.quotaBytes = 107373108658176
        v.quotaCwdBytes = 107373108658176
        v.properties['read_only'] = 'true'
        v.properties['storage'] = '/Berkanavt/supervisor'
        v.properties['backend'] = 'bind'
        return iss_proto

    return _gen_iss_payload


@pytest.fixture
def gen_expected_pod(config, vm, pod_ctl_mock, gen_expected_iss_payload, default_vmagent_version):
    def _gen_pod():
        """

        :type vm: vmset_pb2.VM
        :rtype: data_model.TPod
        """
        pod = data_model.TPod()
        # Meta
        pod.meta.id = vm.meta.id
        pod.meta.pod_set_id = vm.meta.id
        pod.meta.inherit_acl = True
        # Spec
        pod.spec.enable_scheduling = True
        pod.spec.host_name_kind = data_model.PHNK_PERSISTENT
        # Resource requests
        pod.spec.resource_requests.vcpu_guarantee = vm.spec.qemu.resource_requests.vcpu_guarantee
        pod.spec.resource_requests.vcpu_limit = vm.spec.qemu.resource_requests.vcpu_limit
        pod.spec.resource_requests.memory_limit = vm.spec.qemu.resource_requests.memory_limit
        pod.spec.resource_requests.memory_guarantee = vm.spec.qemu.resource_requests.memory_guarantee
        pod.spec.resource_requests.anonymous_memory_limit = vm.spec.qemu.resource_requests.anonymous_memory_limit
        pod.spec.resource_requests.dirty_memory_limit = vm.spec.qemu.resource_requests.dirty_memory_limit
        pod.spec.resource_requests.network_bandwidth_guarantee = vm.spec.qemu.resource_requests.network_bandwidth_guarantee
        # commented due to QEMUKVM-1679
        # pod.spec.resource_requests.network_bandwidth_limit = vm.spec.qemu.resource_requests.network_bandwidth_guarantee

        # Ip6 address requests
        # 1: vm backbone
        req = pod.spec.ip6_address_requests.add()
        req.network_id = vm.spec.qemu.network_id
        req.vlan_id = 'backbone'
        req.enable_dns = True
        req.labels.attributes.add(key='owner', value=yson.dumps('vm'))
        # 2: vm fastbone
        req = pod.spec.ip6_address_requests.add()
        req.network_id = vm.spec.qemu.network_id
        req.vlan_id = 'fastbone'
        req.enable_dns = True
        req.labels.attributes.add(key='owner', value=yson.dumps('vm'))
        # 3: container backbone
        req = pod.spec.ip6_address_requests.add()
        req.network_id = SERVICE_MACRO
        req.vlan_id = 'backbone'
        req.enable_dns = False
        req.labels.attributes.add(key='owner', value=yson.dumps('container'))
        req.labels.attributes.add(key='unistat', value=yson.dumps('enabled'))
        # 4: container fastbone
        req = pod.spec.ip6_address_requests.add()
        req.network_id = SERVICE_MACRO
        req.vlan_id = 'fastbone'
        req.enable_dns = False
        req.labels.attributes.add(key='owner', value=yson.dumps('container'))
        # Host devices
        pod.spec.host_devices.add(path='/dev/kvm', mode='rw')
        pod.spec.host_devices.add(path='/dev/net/tun', mode='rw')
        # Sysctl properties
        pod.spec.sysctl_properties.add(name='net.ipv6.conf.all.proxy_ndp', value=yson.dumps(1))
        pod.spec.sysctl_properties.add(name='net.ipv6.conf.all.forwarding', value=yson.dumps(1))
        pod.spec.sysctl_properties.add(name='net.ipv4.conf.all.forwarding', value=yson.dumps(1))
        # Disk volume requests
        # Root fs
        req = pod.spec.disk_volume_requests.add()
        req.id = '{}-{}'.format(vm.meta.id, 'version')
        req.storage_class = vm.spec.qemu.volumes[0].storage_class
        main_storage = req.storage_class
        root_fs_guarantee = min(vm.spec.qemu.io_guarantees_per_storage[main_storage] / 3, MIN_ROOT_FS_LIMIT)
        root_quota = config.get_value('vmproxy.default_porto_layer.root_quota')
        workdir_quota = config.get_value('vmproxy.default_porto_layer.workdir_quota')
        req.quota_policy.capacity = root_quota + workdir_quota
        req.quota_policy.bandwidth_guarantee = root_fs_guarantee
        req.quota_policy.bandwidth_limit = root_fs_guarantee
        req.labels.attributes.add(key='mount_path', value=yson.dumps('/'))
        req.labels.attributes.add(key='volume_type', value=yson.dumps('root_fs'))
        req.labels.attributes.add(key='root_fs_snapshot_quota', value=yson.dumps(root_quota))
        req.labels.attributes.add(key='work_dir_snapshot_quota', value=yson.dumps(workdir_quota))

        disk_numbers = {'ssd': 0, 'hdd': 0}
        for v in vm.spec.qemu.volumes:
            disk_numbers[v.storage_class] += 1

        one_volume_limit = dict(vm.spec.qemu.io_guarantees_per_storage)
        one_volume_limit[main_storage] -= root_fs_guarantee
        for storage_class, number in disk_numbers.items():
            if number:
                one_volume_limit[storage_class] /= number
        # Volumes
        for i, v in enumerate(vm.spec.qemu.volumes):  # type: vmset_pb2.Volume
            req = pod.spec.disk_volume_requests.add()
            req.id = v.req_id or '{}-{}'.format(vm.meta.id, 'version')
            req.storage_class = v.storage_class
            req.quota_policy.capacity = v.capacity
            req.quota_policy.bandwidth_guarantee = one_volume_limit[req.storage_class]
            req.quota_policy.bandwidth_limit = one_volume_limit[req.storage_class]
            disk_volume_requests_labels = {
                'mount_path': v.pod_mount_path,
                'volume_type': 'persistent',
                'qyp_resource_url': v.resource_url,
                'qyp_image_type': v.image_type,
                'qyp_vm_mount_path': v.vm_mount_path,
                'qyp_volume_name': v.name
            }
            for key, value in disk_volume_requests_labels.items():
                req.labels.attributes.add(key=key, value=yson.dumps(value))

        # Pod Labels
        labels = {
            'deploy_engine': 'QYP',
            'qyp_vm_type': vm.spec.qemu.vm_type,
            'vmagent_version': config.get_value('vmproxy.default_vmagent.version'),
            'version': 'version',
            'qyp_vm_autorun': vm.spec.qemu.autorun,
            'qyp_vm_mark': {}
        }
        if vm.spec.qemu.forced_node_id:
            labels['qyp_vm_forced_node_id'] = vm.spec.qemu.forced_node_id

        for key, value in labels.items():
            pod.labels.attributes.add(key=key, value=yson.dumps(value))

        # Annotations
        owners_dict = {
            'logins': vm.meta.auth.owners.logins,
            'groups': [int(g) for g in vm.meta.auth.owners.group_ids],
            'author': vm.meta.author,
        }

        pod.annotations.attributes.add(
            key='owners',
            value=yson.dumps(owners_dict)
        )

        annotations_vm = vmset_pb2.VM()
        annotations_vm.meta.CopyFrom(vm.meta)
        annotations_vm.spec.CopyFrom(vm.spec)

        pod.annotations.attributes.add(key='qyp_vm_spec',
                                       value=yson.dumps(annotations_vm.SerializeToString(deterministic=True)))
        pod.spec.dynamic_attributes.annotations.append('qyp_vm_spec')

        if config.get_value('vmproxy.pass_ssh_keys', False):
            request = pod_ctl_mock.get_keys_by_logins(vm.meta.auth.owners.logins)
            keys = []
            for user in request:
                for key in user:
                    keys.append(key['key'])
            pod.annotations.attributes.add(key='qyp_ssh_authorized_keys', value=yson.dumps(keys))
            pod.spec.dynamic_attributes.annotations.append('qyp_ssh_authorized_keys')
        pod.spec.iss.CopyFrom(gen_expected_iss_payload(pod))
        return pod

    return _gen_pod


@pytest.fixture
def gen_expected_pod_set(config, vm):
    def _gen_pod_set(owners_ace_extend=None):
        """
        :type vm: vmset_pb2.VM
        :rtype: data_model.TPodSet
        """
        pod_set = data_model.TPodSet()
        pod_set.meta.id = vm.meta.id
        pod_set.spec.node_segment_id = vm.spec.qemu.node_segment
        if vm.spec.account_id:
            pod_set.spec.account_id = vm.spec.account_id

        pod_set.labels.attributes.add(key='deploy_engine', value=yson.dumps('QYP'))
        pod_set.labels.attributes.add(key='qyp_vm_type', value=yson.dumps(vm.spec.qemu.vm_type))
        pod_set.labels.attributes.add(key='vmagent_version', value=yson.dumps(
            config.get_value('vmproxy.default_vmagent.version')))
        pod_set.labels.attributes.add(key='version', value=yson.dumps('version'))
        pod_set.labels.attributes.add(key='qyp_vm_autorun', value=yson.dumps(vm.spec.qemu.autorun))
        pod_set.labels.attributes.add(key='qyp_vm_mark', value=yson.dumps({}))

        root_ace = data_model.TAccessControlEntry()
        root_ace.action = data_model.ACA_ALLOW
        root_ace.permissions.extend([
            data_model.ACA_WRITE,
            data_model.ACP_READ,
        ])
        root_ace.subjects.extend(ROOT_SUBJECTS)

        owners_ace = data_model.TAccessControlEntry()
        owners_ace.action = data_model.ACA_ALLOW
        owners_ace.permissions.extend([
            data_model.ACA_GET_QYP_VM_STATUS,
            data_model.ACA_SSH_ACCESS,
            data_model.ACP_READ,
        ])
        owners_ace.subjects.extend(['author'])
        if owners_ace_extend:
            owners_ace.subjects.extend(owners_ace_extend)
        pod_set.meta.acl.extend([root_ace, owners_ace])
        return pod_set

    return _gen_pod_set


@pytest.fixture
def pod():
    return data_model.TPod()


@pytest.fixture
def pod_set():
    return data_model.TPodSet()


@pytest.fixture()
def pod_ctl_mock(pod, pod_set):
    pod_ctl = mock.Mock()

    pod_ctl.get_pod.return_value = pod
    pod_ctl.get_pod_set.return_value = pod_set
    pod_ctl.get_service_existing_scopes.return_value = [abc_roles.ADMIN_ROLE_SCOPE_SLUG,
                                                        abc_roles.MANAGEMENT_ROLE_SCOPE_SLUG]
    return pod_ctl


@pytest.fixture()
def staff_client_mock():  # type: () -> staff.StaffClient
    _staff_client_mock = mock.Mock()
    _staff_client_mock.list_groups.return_value = {'result': []}
    return _staff_client_mock


@pytest.fixture()
def abc_client_mock():  # type: () -> abc.AbcClient
    _abc_client = mock.Mock()
    _abc_client._client.get.return_value = {'results': []}
    _abc_client.list_service_member_roles.return_value = set()
    return _abc_client


@pytest.fixture()
def rt_client_mock():
    _rt_client = mock.Mock()
    _rt_client.get_project_by_user.return_value = [{'macros': '_BROOKLYN_NETS_'}]
    return _rt_client


@pytest.fixture()
def sec_policy_mock():  # type: () -> security_policy.SecurityPolicy
    return mock.Mock()


@pytest.fixture()
def qdm_client_mock():  # type: () -> qdm_client.QDMClient
    _qdm_client = mock.Mock()
    _qdm_client.backup_status.return_value = None
    _qdm_client.backup_list.return_value = []
    return _qdm_client


@pytest.fixture()
def ctx_mock(pod_ctl_mock, staff_client_mock, sec_policy_mock, abc_client_mock, rt_client_mock, qdm_client_mock,
             config):
    config.set_value('vmproxy.node_segment', ['test', 'default', 'dev', 'gpu-dev'])

    def configure(binder):
        binder.bind(staff.IStaffClient, staff_client_mock)
        binder.bind(abc.IAbcClient, abc_client_mock)
        binder.bind(rt.IRtClient, rt_client_mock)

    inject.clear_and_configure(configure)

    ctx = mock.Mock()
    ctx.sec_policy = sec_policy_mock
    ctx.pod_ctl = pod_ctl_mock
    ctx.qdm_client = qdm_client_mock
    ctx.pod_ctl.yp_cluster = 'TEST_SAS'
    ctx.personal_quotas_dict = {'abc:service:4172': {}}
    ctx.personal_accounts = {'abc:service:4172'}
    ctx.heartbeat_client = HeartBeatClient()
    ctx.heartbeat_client.request_usage = mock.Mock()

    get_context = mock.Mock()
    get_context.return_value = ctx

    with mock.patch('infra.qyp.vmproxy.src.web.vmset_service.get_context', get_context):
        yield ctx
