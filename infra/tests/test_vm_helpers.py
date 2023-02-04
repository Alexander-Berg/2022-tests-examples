import mock
import pytest

from infra.qyp.vmproxy.src import errors, security_policy
from infra.qyp.vmproxy.src import vm_instance


def test_container_vm():
    host = 'sas1-0000.search.yandex.net'
    port = '1234'
    service = 'awesome-service'
    author = 'author'

    def get_service_instances(_service):
        if _service == service:
            return [
                {
                    'hostname': host,
                    'port': int(port),
                }
            ]
        else:
            return []

    def get_service_owners(_service):
        if _service == service:
            return {
                'logins': [author],
                'groups': []
            }
        else:
            return {
                'logins': [],
                'groups': []
            }

    flask_g = mock.Mock()
    flask_g.ctx.sec_policy = security_policy.SecurityPolicy()
    flask_g.ctx.nanny_client.get_service_instances.side_effect = get_service_instances
    flask_g.ctx.nanny_client.get_service_owners.side_effect = get_service_owners

    # Case 1: port is not a number
    with pytest.raises(errors.ValidationError):
        vm_instance.get_instance({
            'host': host,
            'port': 'not-a-number',
            'service': service
        }, flask_g.ctx)

    # Case 2: wrong service access
    with pytest.raises(errors.ValidationError):
        vm_instance.get_instance({
            'host': host,
            'port': port,
            'service': 'some-other-service'
        }, flask_g.ctx)

    instance = vm_instance.get_instance({
        'host': host,
        'port': port,
        'service': service
    }, flask_g.ctx)
    assert type(instance) == vm_instance.HostVMInstance

    # Case 3: wrong user
    with pytest.raises(errors.AuthorizationError):
        instance.check_access('johndoe', flask_g.ctx)

    instance.check_access(author, flask_g.ctx)
    assert instance.get_agent_url('/action') == 'http://{}:{}/action'.format(host, port)


def test_pod_vm(config):
    pod_id = 'real-pod-id'
    author = 'author'
    pod_ip = '::1'
    port = '7255'
    config.set_value('vmproxy.default_agent_port', port)

    def get_active_pod(_pod_id):
        if _pod_id != pod_id:
            raise errors.WrongStateError('')

    def check_read_permission(pod_id, subject_id):
        return subject_id in [author, 'imperator']

    def check_write_permission(pod_id, subject_id):
        return subject_id in [author]

    flask_g = mock.Mock()
    flask_g.ctx.sec_policy = security_policy.SecurityPolicy()
    flask_g.ctx.pod_ctl.get_active_pod.side_effect = get_active_pod
    flask_g.ctx.pod_ctl.check_read_permission.side_effect = check_read_permission
    flask_g.ctx.pod_ctl.check_write_permission.side_effect = check_write_permission
    flask_g.ctx.pod_ctl.get_pod_container_ip.return_value = pod_ip

    # Case 1: no active pod
    with pytest.raises(errors.WrongStateError):
        vm_instance.get_instance({
            'pod_id': 'fake'
        }, flask_g.ctx)

    instance = vm_instance.get_instance({
        'pod_id': pod_id
    }, flask_g.ctx)
    assert type(instance) == vm_instance.PodVMInstance

    # Case 2: no read access
    with pytest.raises(errors.AuthorizationError):
        instance.check_access('johndoe', flask_g.ctx)

    # Case 3: read access only
    instance.check_access('imperator', flask_g.ctx)
    with pytest.raises(errors.AuthorizationError):
        instance.check_write_access('imperator', flask_g.ctx)

    # Case 4: full access
    instance.check_access(author, flask_g.ctx)
    instance.check_write_access(author, flask_g.ctx)
    assert instance.get_agent_url('/action') == 'http://[{}]:{}/action'.format(pod_ip, port)

    # Case 5: check parameters pod
    assert str(instance) == 'real-pod-id'
    assert instance.get_vnc_host_port() == ('::1', 7258)

    # Case 6: wrong parameters
    with pytest.raises(errors.ValidationError):
        vm_instance.get_instance({
            'pod_id_wrong': 'fake'
        }, flask_g.ctx)

    # Case 7: check parameters vm
    flask_g.ctx.nanny_client.get_service_instances.return_value = [{'hostname': 'host', 'port': 1234}]
    instance = vm_instance.get_instance({
        'host': 'host',
        'port': '1234',
        'service': 'service'
    }, flask_g.ctx)
    assert str(instance) == 'host:1234:service'
    assert instance.get_vnc_host_port() == ('host', 1237)
