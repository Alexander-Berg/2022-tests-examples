import os
import uuid
import time

import pytest

import subprocess
import multiprocessing
from infra.swatlib import cmdutil
import yatest
from yatest.common import network
from infra.qyp.vmctl.src.api import VMProxyClient
from infra.qyp.integration_tests.vmproxy_tests import vmproxy_local
import requests


@pytest.fixture()
def local_bin_path():
    try:
        return yatest.common.binary_path("infra/qyp/vmproxy/bin/vmproxy")
    except AttributeError as e:
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], "infra/qyp/vmproxy/bin/vmproxy")


@pytest.fixture()
def local_config_path():
    try:
        return yatest.common.source_path('infra/qyp/vmproxy/cfg_default.yml')
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], 'infra/qyp/vmproxy/cfg_default.yml')


@pytest.fixture()
def tvm_context_secret_file_path():
    return os.environ['PYTESTS_VMPROXY_LOCAL_TVM_CONTEXT_SECRET_FILE_PATH']


@pytest.fixture()
def staff_oauth_token():
    return os.environ["PYTESTS_VMPROXY_LOCAL_STAFF_OAUTH_TOKEN"]


@pytest.fixture()
def nanny_token():
    return os.environ["PYTESTS_VMPROXY_LOCAL_NANNY_TOKEN"]


@pytest.fixture()
def abc_oauth_token():
    return os.environ["PYTESTS_VMPROXY_LOCAL_ABC_OAUTH_TOKEN"]


@pytest.fixture()
def sandbox_oauth_token():
    return os.environ["PYTESTS_VMPROXY_LOCAL_SANDBOX_OAUTH_TOKEN"]


@pytest.fixture()
def yp_api_token():
    return os.environ["PYTESTS_VMPROXY_LOCAL_YP_API_TOKEN"]


@pytest.fixture()
def auth_user():
    return os.environ["PYTESTS_VMPROXY_AUTH_USER"]


@pytest.fixture()
def yp_cluster():
    return os.environ.get('PYTESTS_VMPROXY_LOCAL_DEFAULT_CLUSTER', 'TEST_SAS')


@pytest.fixture()
def pod_id(auth_user, yp_cluster):
    return '{}-{}-testing-2'.format(auth_user, yp_cluster.lower()).replace('_', '-')


@pytest.fixture()
def vmproxy_url(local_bin_path,
                local_config_path,
                auth_user,
                yp_cluster,
                tvm_context_secret_file_path,
                staff_oauth_token,
                nanny_token,
                abc_oauth_token,
                sandbox_oauth_token,
                yp_api_token,
                ):

    with network.PortManager() as pm:
        local_port = pm.get_port()
        argv = ['-c', local_config_path,
                '-V', 'port={}'.format(local_port),
                '-V', 'debug=1',
                '-V', 'base_logs_dir=/tmp',
                '-V', 'disable_auth=1',
                '-V', 'force_return_user={}'.format(auth_user),
                '-V', 'STAFF_OAUTH_TOKEN={}'.format(staff_oauth_token),
                '-V', 'NANNY_TOKEN={}'.format(nanny_token),
                '-V', 'ABC_OAUTH_TOKEN={}'.format(abc_oauth_token),
                '-V', 'SANDBOX_OAUTH_TOKEN={}'.format(sandbox_oauth_token),
                '-V', 'YP_API_TOKEN={}'.format(yp_api_token),
                '-V', 'DEFAULT_CLUSTER={}'.format(yp_cluster),
                '-V', 'TVM_CONTEXT_SECRET_FILE_PATH={}'.format(tvm_context_secret_file_path),
                ]
        os.environ['CREATE_VM_FOR_TESTING'] = 'Testing'
        vmproxy_process = multiprocessing.Process(
            target=vmproxy_local.run,
            args=(argv,)
        )
        vmproxy_process.start()
        no_connection_errors = False
        vmproxy_url_result = "http://localhost:{}".format(local_port)
        while not no_connection_errors:
            try:
                requests.get(vmproxy_url_result + '/ping')
                no_connection_errors = True
            except requests.ConnectionError as e:
                time.sleep(0.2)

        yield vmproxy_url_result
        vmproxy_process.terminate()
        vmproxy_process.join()


@pytest.fixture()
def vmproxy_client_token():
    return os.environ["PYTESTS_VMPROXY_CLIENT_TOKEN"]


@pytest.fixture()
def vmproxy_client(vmproxy_url, vmproxy_client_token):  # type: (str, str) -> VMProxyClient
    return VMProxyClient(
        token=vmproxy_client_token,
        proxyhost=vmproxy_url,
        ssl_none=True,
        timeout=300,
    )


@pytest.fixture()
def status_vm(vmproxy_client, pod_id, yp_cluster, auth_user):
    def _status_vm():
        return vmproxy_client.get_status(
            pod_id=pod_id,
            cluster=yp_cluster
        )
    return _status_vm


@pytest.fixture()
def create_vm(vmproxy_client, pod_id, yp_cluster, auth_user, status_vm):
    def _create_pod(wait_status_change=False,
                    raise_if_exists=True,
                    vmagent_version='',
                    image_type='RAW',
                    custom_pod_resources=None,
                    rb_torrent=None):
        exists = vmproxy_client.exists_vm(yp_cluster, pod_id)
        if raise_if_exists:
            assert not exists
        if not exists:
            vmproxy_client.create(
                config_id=str(uuid.uuid4()),
                vcpu=1,
                vcpu_limit=1 * 1000,
                vcpu_guarantee=1 * 1000,
                mem=10 * (1024 ** 3),
                mem_config=9 * (1024 ** 3),
                rb_torrent=rb_torrent or 'rbtorrent:6e7bdce06445ddf136401ffbd4071e0e36003579',
                disk_size=0,
                vm_type='linux',
                image_type=image_type,
                autorun=True,
                cluster=yp_cluster,
                pod_id=pod_id,
                network_id='_SEARCHSAND_',
                node_segment='default',
                volume_size=20 * (1024 ** 3),
                logins=[auth_user],
                groups=[],
                storage_class='ssd',
                enable_internet=False,
                use_nat64=True,
                abc=None,
                vmagent_version=vmagent_version,
                custom_pod_resources=custom_pod_resources
            )

        if wait_status_change:
            vmproxy_client.wait_status_change(cluster=yp_cluster, pod_id=pod_id)

    yield _create_pod


# @pytest.fixture
# def connect_to_vm(vmproxy_client, pod_id, yp_cluster, auth_user):
#     def _connect_to_vm():



