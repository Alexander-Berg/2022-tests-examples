from collections import namedtuple

import inject
import mock
import six
from datetime import datetime
from sepelib.core import config

from awacs.model import components, validation, dao, util, objects
from awacs.model.cache import IAwacsCache
from awacs.model.dao import IDao

from awacs.web import namespace_service, balancer_service, upstream_service, l3_balancer_service, dns_record_service, \
    domain_service, certificate_service, backend_service, endpoint_set_service, component_service, knob_service, \
    weight_section_service, l7heavy_config_service
from awacs.web.auth.core import StaffAuth
from awtest import wait_until_passes, wait_until
from infra.awacs.proto import model_pb2, api_pb2, modules_pb2
from infra.swatlib.rpc.authentication import AuthSubject


def call(method, req_pb, login='anonymous', group_ids=(), enable_auth=False):
    # copy request to prevent its modification by handler
    cls = type(req_pb)
    req_pb_copy = cls()
    req_pb_copy.CopyFrom(req_pb)

    auth_subject = AuthSubject(login)
    bp = method.bp

    def call_user_handler():
        with bp.translate_errors():
            return method.handler(req_pb_copy, auth_subject)

    with mock.patch.object(StaffAuth, '_get_group_ids', return_value=group_ids), \
            mock.patch.object(StaffAuth, '_get_cached_group_ids', return_value=group_ids):
        if enable_auth:
            prev_run_auth_value = config.get_value('run.auth')
            config.set_value('run.auth', True)
            try:
                return call_user_handler()
            finally:
                config.set_value('run.auth', prev_run_auth_value)
        else:
            return call_user_handler()


def create_namespace_with_order_in_progress(zk_storage, cache, namespace_id, login='login'):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = namespace_id
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    ns_pb.meta.auth.staff.owners.logins.extend([login])
    ns_pb.order.status.status = 'IN_PROGRESS'
    zk_storage.create_namespace(namespace_id, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(namespace_id))


NOT_ROOT_NS_OWNER_LOGIN = 'ordinary_user'


def create_namespace(zk_storage, cache, namespace_id):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = namespace_id
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    ns_pb.meta.auth.staff.owners.logins.extend(['login', NOT_ROOT_NS_OWNER_LOGIN])
    ns_pb.order.status.status = 'FINISHED'
    zk_storage.create_namespace(namespace_id, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(namespace_id))
    return cache.must_get_namespace(namespace_id)


def set_login_to_root_users(login):
    root_users = config.get_value('run.root_users', [])
    if login not in root_users:
        root_users.append(login)
        config.set_value('run.root_users', root_users)


def fill_object_upper_limits(namespace_id, object_name, count, login):
    req_pb = api_pb2.GetNamespaceRequest(id=namespace_id, consistency=api_pb2.STRONG)
    namespace_pb = call(namespace_service.get_namespace, req_pb, login).namespace
    req_pb = api_pb2.UpdateNamespaceRequest(meta=namespace_pb.meta, spec=namespace_pb.spec)
    getattr(req_pb.spec.object_upper_limits, object_name).value = count
    call(namespace_service.update_namespace, req_pb, login)


def make_unknown_layout(namespace_id, login):
    req_pb = api_pb2.GetNamespaceRequest(id=namespace_id, consistency=api_pb2.STRONG)
    namespace_pb = call(namespace_service.get_namespace, req_pb, login).namespace
    req_pb = api_pb2.UpdateNamespaceRequest(meta=namespace_pb.meta, spec=namespace_pb.spec)
    req_pb.spec.layout_type = req_pb.spec.NS_LAYOUT_UNKNOWN
    req_pb.spec.ClearField('object_upper_limits')
    req_pb.spec.object_upper_limits.SetInParent()
    call(namespace_service.update_namespace, req_pb, login)

    def check():
        assert IAwacsCache.instance().must_get_namespace(namespace_id).spec.object_upper_limits == req_pb.spec.object_upper_limits
    wait_until_passes(check)


def create_too_many_lines_yaml_config(larger_than=1500):
    lines = [u'modules:\n']
    lines.extend([u'- report: {uuid: x, ranges: default}\n'] * larger_than)
    lines.append(u'- errordocument: {status: 200}')
    return u''.join(lines)


def create_too_many_chars_yaml_config(larger_than=80000):
    return u''.join([
        u'modules:\n',
        u'- report: {uuid: ' + u'x' * larger_than + u', ranges: default}\n',
        u'- errordocument: {status: 200}'
    ])


class Api(object):
    TEST_LOGIN = 'test_user'
    TEST_LOGIN2 = 'test_user2'

    cache = inject.attr(IAwacsCache)

    @classmethod
    def set_login_to_root_users(cls, login):
        root_users = config.get_value('run.root_users', [])
        if login not in root_users:
            config.set_value('run.root_users', root_users + [login])

    @classmethod
    def pause_balancer_transport(cls, namespace_id, balancer_id, comment='-'):
        balancer_pb = Api.get_balancer(namespace_id=namespace_id, balancer_id=balancer_id)
        assert not balancer_pb.meta.transport_paused.value
        transport_paused_pb = model_pb2.PausedCondition(value=True, comment=comment)
        balancer_pb = Api.update_balancer(namespace_id=namespace_id,
                                          balancer_id=balancer_id,
                                          version=balancer_pb.meta.version,
                                          transport_paused_pb=transport_paused_pb)
        wait_until(lambda: (cls.cache.get_balancer(namespace_id, balancer_id).meta.version ==
                            balancer_pb.meta.version),
                   timeout=1)

    @classmethod
    def unpause_balancer_transport(cls, namespace_id, balancer_id, comment='-'):
        balancer_pb = Api.get_balancer(namespace_id=namespace_id, balancer_id=balancer_id)
        assert balancer_pb.meta.transport_paused.value
        transport_paused_pb = model_pb2.PausedCondition(value=False, comment=comment)
        balancer_pb = Api.update_balancer(namespace_id=namespace_id,
                                          balancer_id=balancer_id,
                                          version=balancer_pb.meta.version,
                                          transport_paused_pb=transport_paused_pb)
        wait_until(lambda: (cls.cache.get_balancer(namespace_id, balancer_id).meta.version ==
                            balancer_pb.meta.version),
                   timeout=1)

    @classmethod
    def pause_l3_balancer_transport(cls, namespace_id, l3_balancer_id, comment='-'):
        l3_balancer_pb = Api.get_l3_balancer(namespace_id=namespace_id, l3_balancer_id=l3_balancer_id)
        assert not l3_balancer_pb.meta.transport_paused.value
        transport_paused_pb = model_pb2.PausedCondition(value=True, comment=comment)
        l3_balancer_pb = Api.update_l3_balancer(namespace_id=namespace_id,
                                                l3_balancer_id=l3_balancer_id,
                                                version=l3_balancer_pb.meta.version,
                                                transport_paused_pb=transport_paused_pb)
        wait_until(lambda: (cls.cache.get_l3_balancer(namespace_id, l3_balancer_id).meta.version ==
                            l3_balancer_pb.meta.version),
                   timeout=1)

    @classmethod
    def unpause_l3_balancer_transport(cls, namespace_id, l3_balancer_id, comment='-'):
        def get_l3_pb():
            pb = cls.cache.get_l3_balancer(namespace_id, l3_balancer_id)
            if pb.meta.transport_paused.value:
                return pb
        l3_balancer_pb = wait_until(get_l3_pb)
        transport_paused_pb = model_pb2.PausedCondition(value=False, comment=comment)
        l3_balancer_pb = Api.update_l3_balancer(namespace_id=namespace_id,
                                                l3_balancer_id=l3_balancer_id,
                                                version=l3_balancer_pb.meta.version,
                                                transport_paused_pb=transport_paused_pb)
        wait_until(lambda: (cls.cache.get_l3_balancer(namespace_id, l3_balancer_id).meta.version ==
                            l3_balancer_pb.meta.version))

    @classmethod
    def pause_l7heavy_config_transport(cls, namespace_id, l7heavy_config_id, comment='-'):
        l7heavy_config_pb = Api.get_l7heavy_config(namespace_id=namespace_id, l7heavy_config_id=l7heavy_config_id)
        assert not l7heavy_config_pb.meta.transport_paused.value
        transport_paused_pb = model_pb2.PausedCondition(value=True, comment=comment)
        l7heavy_config_pb = Api.update_l7heavy_config(namespace_id=namespace_id,
                                                      l7heavy_config_id=l7heavy_config_id,
                                                      version=l7heavy_config_pb.meta.version,
                                                      transport_paused_pb=transport_paused_pb)
        wait_until(lambda: (objects.L7HeavyConfig.cache.must_get(namespace_id, l7heavy_config_id).meta.version ==
                            l7heavy_config_pb.meta.version),
                   timeout=1)

    @classmethod
    def unpause_l7heavy_config_transport(cls, namespace_id, l7heavy_config_id, comment='-'):
        def get_pb():
            pb = objects.L7HeavyConfig.cache.get(namespace_id, l7heavy_config_id)
            if pb.meta.transport_paused.value:
                return pb

        l7heavy_config_pb = wait_until(get_pb, must_get_value=True, timeout=5)
        transport_paused_pb = model_pb2.PausedCondition(value=False, comment=comment)
        l7heavy_config_pb = Api.update_l7heavy_config(namespace_id=namespace_id,
                                                      l7heavy_config_id=l7heavy_config_id,
                                                      version=l7heavy_config_pb.meta.version,
                                                      transport_paused_pb=transport_paused_pb)

        wait_until(lambda: (objects.L7HeavyConfig.cache.must_get(namespace_id, l7heavy_config_id).meta.version ==
                            l7heavy_config_pb.meta.version))

    @classmethod
    def list_balancers(cls, namespace_id):
        req_pb = api_pb2.ListBalancersRequest(namespace_id=namespace_id)
        resp_pb = call(balancer_service.list_balancers, req_pb, cls.TEST_LOGIN)
        return resp_pb

    @classmethod
    def list_upstreams(cls, namespace_id):
        req_pb = api_pb2.ListUpstreamsRequest(namespace_id=namespace_id)
        resp_pb = call(upstream_service.list_upstreams, req_pb, cls.TEST_LOGIN)
        return resp_pb

    @classmethod
    def list_upstream_revisions(cls, namespace_id, upstream_id):
        req_pb = api_pb2.ListUpstreamRevisionsRequest(namespace_id=namespace_id, id=upstream_id)
        resp_pb = call(upstream_service.list_upstream_revisions, req_pb, cls.TEST_LOGIN)
        return resp_pb

    @classmethod
    def get_balancer_state(cls, namespace_id, balancer_id):
        req_pb = api_pb2.GetBalancerStateRequest(namespace_id=namespace_id, id=balancer_id)
        resp_pb = call(balancer_service.get_balancer_state, req_pb, cls.TEST_LOGIN)
        return resp_pb.state

    @classmethod
    def get_l3_balancer_state(cls, namespace_id, l3_balancer_id):
        req_pb = api_pb2.GetL3BalancerStateRequest(namespace_id=namespace_id, id=l3_balancer_id)
        resp_pb = call(l3_balancer_service.get_l3_balancer_state, req_pb, cls.TEST_LOGIN)
        return resp_pb.state

    @classmethod
    def get_dns_record(cls, namespace_id, dns_record_id):
        req_pb = api_pb2.GetDnsRecordRequest(namespace_id=namespace_id, id=dns_record_id)
        resp_pb = call(dns_record_service.get_dns_record, req_pb, cls.TEST_LOGIN)
        return resp_pb.dns_record

    @classmethod
    def get_dns_record_state(cls, namespace_id, dns_record_id):
        req_pb = api_pb2.GetDnsRecordStateRequest(namespace_id=namespace_id, id=dns_record_id)
        resp_pb = call(dns_record_service.get_dns_record_state, req_pb, cls.TEST_LOGIN)
        return resp_pb.state

    @classmethod
    def get_balancer(cls, namespace_id, balancer_id):
        req_pb = api_pb2.GetBalancerRequest(namespace_id=namespace_id, id=balancer_id)
        resp_pb = call(balancer_service.get_balancer, req_pb, cls.TEST_LOGIN)
        return resp_pb.balancer

    @classmethod
    def get_l3_balancer(cls, namespace_id, l3_balancer_id):
        req_pb = api_pb2.GetL3BalancerRequest(namespace_id=namespace_id, id=l3_balancer_id)
        resp_pb = call(l3_balancer_service.get_l3_balancer, req_pb, cls.TEST_LOGIN)
        return resp_pb.l3_balancer

    @classmethod
    def get_weight_section(cls, namespace_id, weight_section_id):
        req_pb = api_pb2.GetWeightSectionRequest(namespace_id=namespace_id, id=weight_section_id)
        resp_pb = call(weight_section_service.get_weight_section, req_pb, cls.TEST_LOGIN)
        return resp_pb.weight_section

    @classmethod
    def get_l7heavy_config(cls, namespace_id, l7heavy_config_id):
        req_pb = api_pb2.GetL7HeavyConfigRequest(namespace_id=namespace_id, id=l7heavy_config_id)
        resp_pb = call(l7heavy_config_service.get_l7heavy_config, req_pb, cls.TEST_LOGIN)
        return resp_pb.l7heavy_config

    @classmethod
    def get_l7heavy_config_state(cls, namespace_id, l7heavy_config_id):
        req_pb = api_pb2.GetL7HeavyConfigStateRequest(namespace_id=namespace_id, id=l7heavy_config_id)
        resp_pb = call(l7heavy_config_service.get_l7heavy_config_state, req_pb, cls.TEST_LOGIN)
        return resp_pb.state

    @classmethod
    def get_domain(cls, namespace_id, domain_id):
        req_pb = api_pb2.GetDomainRequest(namespace_id=namespace_id, id=domain_id)
        resp_pb = call(domain_service.get_domain, req_pb, cls.TEST_LOGIN)
        return resp_pb.domain

    @classmethod
    def get_cert(cls, namespace_id, cert_id):
        req_pb = api_pb2.GetCertificateRequest(namespace_id=namespace_id, id=cert_id)
        resp_pb = call(certificate_service.get_certificate, req_pb, cls.TEST_LOGIN)
        return resp_pb.certificate

    @classmethod
    def get_upstream(cls, namespace_id, upstream_id, consistency=api_pb2.WEAK):
        req_pb = api_pb2.GetUpstreamRequest(namespace_id=namespace_id, id=upstream_id, consistency=consistency)
        resp_pb = call(upstream_service.get_upstream, req_pb, cls.TEST_LOGIN)
        return resp_pb.upstream

    @classmethod
    def get_backend(cls, namespace_id, backend_id):
        req_pb = api_pb2.GetBackendRequest(namespace_id=namespace_id, id=backend_id)
        resp_pb = call(backend_service.get_backend, req_pb, cls.TEST_LOGIN)
        return resp_pb.backend

    @classmethod
    def get_endpoint_set(cls, namespace_id, endpoint_set_id):
        req_pb = api_pb2.GetEndpointSetRequest(namespace_id=namespace_id, id=endpoint_set_id)
        resp_pb = call(endpoint_set_service.get_endpoint_set, req_pb, cls.TEST_LOGIN)
        return resp_pb.endpoint_set

    @classmethod
    def create_namespace(cls, namespace_id, order_content_pb=None):
        meta_pb = model_pb2.NamespaceMeta()
        meta_pb.id = namespace_id
        meta_pb.category = 'test'
        meta_pb.abc_service_id = 123
        meta_pb.auth.type = meta_pb.auth.STAFF
        meta_pb.auth.staff.owners.group_ids.extend(["1"])
        req_pb = api_pb2.CreateNamespaceRequest(meta=meta_pb)
        if order_content_pb is not None:
            req_pb.order.CopyFrom(order_content_pb)
        resp_pb = call(namespace_service.create_namespace, req_pb, cls.TEST_LOGIN)
        return resp_pb.namespace

    @classmethod
    def create_published_component(cls, component_type, component_version):
        cls.set_login_to_root_users(cls.TEST_LOGIN)
        cls.set_login_to_root_users(cls.TEST_LOGIN2)

        req_pb = api_pb2.DraftComponentRequest()
        req_pb.type = component_type
        req_pb.version = component_version
        req_pb.startrek_issue_key = 'SWATOPS-000'
        sb_pb = components.get_component_config(component_type).sandbox_resource
        req_pb.spec.source.sandbox_resource.task_id = '123456789'
        req_pb.spec.source.sandbox_resource.task_type = sorted(sb_pb.task_types)[0]
        req_pb.spec.source.sandbox_resource.resource_id = '123456789'
        req_pb.spec.source.sandbox_resource.resource_type = sorted(sb_pb.resource_types)[0]
        req_pb.spec.source.sandbox_resource.rbtorrent = 'rbtorrent:0000000000000000000000000000000000000000'
        resp_pb = call(component_service.draft_component, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_component(component_type, component_version).meta.version ==
                            resp_pb.component.meta.version))
        req_pb = api_pb2.PublishComponentRequest()
        req_pb.type = component_type
        req_pb.version = component_version
        resp_pb = call(component_service.publish_component, req_pb, cls.TEST_LOGIN2)
        wait_until(lambda: (cls.cache.get_component(component_type, component_version).status.status ==
                            model_pb2.ComponentStatus.PUBLISHED))
        return resp_pb.component

    @classmethod
    def enable_pushclient(cls, namespace_id, balancer_id, version):
        cls.set_login_to_root_users(cls.TEST_LOGIN)

        req_pb = api_pb2.EnableBalancerPushclientRequest(namespace_id=namespace_id, id=balancer_id, version=version)
        resp_pb = call(balancer_service.enable_balancer_pushclient, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_balancer(namespace_id, balancer_id).meta.version != version))

    @classmethod
    def disable_pushclient(cls, namespace_id, balancer_id, version):
        cls.set_login_to_root_users(cls.TEST_LOGIN)

        req_pb = api_pb2.DisableBalancerPushclientRequest(namespace_id=namespace_id, id=balancer_id, version=version)
        resp_pb = call(balancer_service.disable_balancer_pushclient, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_balancer(namespace_id, balancer_id).meta.version != version))

    # TODO: not actually API
    @classmethod
    def create_l3_balancer(cls, namespace_id, l3_balancer_id, spec_pb):
        """
        :type namespace_id: six.text_type
        :type l3_balancer_id: six.text_type
        :type spec_pb: model_pb2.L3BalancerSpec
        :rtype: model_pb2.L3Balancer
        """
        meta_pb = model_pb2.L3BalancerMeta()
        meta_pb.id = l3_balancer_id
        meta_pb.namespace_id = namespace_id
        meta_pb.auth.type = meta_pb.auth.STAFF
        if not spec_pb.l3mgr_service_id:
            spec_pb.l3mgr_service_id = 'xxx'
        l3_pb, _ = dao.IDao.instance().create_l3_balancer(
            login='robot',
            meta_pb=meta_pb,
            spec_pb=spec_pb)
        wait_until(lambda: cls.cache.get_l3_balancer(namespace_id, l3_balancer_id).meta.version == l3_pb.meta.version)
        return l3_pb

    @classmethod
    def delete_l3_balancer(cls, namespace_id, l3_balancer_id):
        """
        :type namespace_id: six.text_type
        :type l3_balancer_id: six.text_type
        """
        version = cls.get_l3_balancer(namespace_id, l3_balancer_id).meta.version
        req_pb = api_pb2.RemoveL3BalancerRequest(namespace_id=namespace_id, id=l3_balancer_id, version=version)
        call(l3_balancer_service.remove_l3_balancer, req_pb, cls.TEST_LOGIN)

    @classmethod
    def update_l3_balancer(cls, namespace_id, l3_balancer_id, version, spec_pb=None, transport_paused_pb=None):
        meta_pb = model_pb2.L3BalancerMeta()
        meta_pb.id = l3_balancer_id
        meta_pb.namespace_id = namespace_id
        meta_pb.version = version
        if transport_paused_pb is not None:
            meta_pb.transport_paused.CopyFrom(transport_paused_pb)
        req_pb = api_pb2.UpdateL3BalancerRequest(meta=meta_pb, spec=spec_pb)
        resp_pb = call(l3_balancer_service.update_l3_balancer, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_l3_balancer(namespace_id, l3_balancer_id).meta.version ==
                            resp_pb.l3_balancer.meta.version))
        return resp_pb.l3_balancer

    @classmethod
    def update_l3_balancer_state(cls, req_pb):
        """
        :type req_pb: api_pb2.UpdateL3BalancerStateRequest
        :rtype: model_pb2.L3BalancerState
        """
        resp_pb = call(l3_balancer_service.update_l3_balancer_state, req_pb, cls.TEST_LOGIN)
        return resp_pb.state

    # TODO: not actually API
    @classmethod
    def create_l7heavy_config(cls, namespace_id, l7heavy_config_id, spec_pb):
        """
        :type namespace_id: six.text_type
        :type l7heavy_config_id: six.text_type
        :type spec_pb: model_pb2.L7HeavyConfigSpec
        :rtype: model_pb2.L7HeavyConfig
        """
        meta_pb = model_pb2.L7HeavyConfigMeta(id=l7heavy_config_id, namespace_id=namespace_id)
        l7hc_pb = objects.L7HeavyConfig._create_with_rev(
            meta_pb=meta_pb,
            spec_pb=spec_pb,
            login=cls.TEST_LOGIN,
        )
        wait_until(lambda: (objects.L7HeavyConfig.cache.must_get(namespace_id, l7heavy_config_id).meta.version ==
                            l7hc_pb.meta.version))
        return l7hc_pb

    @classmethod
    def create_l7heavy_config_state(cls, namespace_id, l7heavy_config_id):
        """
        :type namespace_id: six.text_type
        :type l7heavy_config_id: six.text_type
        :rtype: model_pb2.L7HeavyConfigState
        """
        pb = objects.L7HeavyConfig.state.create(namespace_id, l7heavy_config_id, datetime.utcnow())
        wait_until(lambda: (objects.L7HeavyConfig.state.cache.must_get(namespace_id, l7heavy_config_id)))
        return pb

    @classmethod
    def update_l7heavy_config(cls, namespace_id, l7heavy_config_id, version, spec_pb=None, transport_paused_pb=None):
        meta_pb = model_pb2.L7HeavyConfigMeta(id=l7heavy_config_id, namespace_id=namespace_id, version=version)
        if transport_paused_pb is not None:
            meta_pb.transport_paused.CopyFrom(transport_paused_pb)
        req_pb = api_pb2.UpdateL7HeavyConfigRequest(meta=meta_pb)
        if spec_pb is not None:
            req_pb.spec.CopyFrom(spec_pb)
        resp_pb = call(l7heavy_config_service.update_l7heavy_config, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (objects.L7HeavyConfig.cache.must_get(namespace_id, l7heavy_config_id).meta.version ==
                            resp_pb.l7heavy_config.meta.version))
        return resp_pb.l7heavy_config

    @classmethod
    def create_dns_record(cls, namespace_id, dns_record_id, spec_pb=None, order_pb=None):
        """
        :type namespace_id: six.text_type
        :type dns_record_id: six.text_type
        :type spec_pb: model_pb2.DnsRecordSpec
        :type order_pb: model_pb2.DnsRecordOrder.Content
        :rtype: model_pb2.DnsRecord
        """
        assert spec_pb or order_pb
        assert not (spec_pb and order_pb)
        meta_pb = model_pb2.DnsRecordMeta()
        meta_pb.id = dns_record_id
        meta_pb.namespace_id = namespace_id
        meta_pb.auth.type = meta_pb.auth.STAFF
        req_pb = api_pb2.CreateDnsRecordRequest(meta=meta_pb, spec=spec_pb, order=order_pb)
        resp_pb = call(dns_record_service.create_dns_record, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_dns_record(namespace_id, dns_record_id).meta.version ==
                            resp_pb.dns_record.meta.version),
                   timeout=1)
        return resp_pb.dns_record

    @classmethod
    def delete_dns_record(cls, namespace_id, dns_record_id):
        """
        :type namespace_id: six.text_type
        :type dns_record_id: six.text_type
        """
        version = cls.get_dns_record(namespace_id, dns_record_id).meta.version
        req_pb = api_pb2.RemoveDnsRecordRequest(namespace_id=namespace_id, id=dns_record_id, version=version)
        call(dns_record_service.remove_dns_record, req_pb, cls.TEST_LOGIN)

    @classmethod
    def update_dns_record(cls, namespace_id, dns_record_id, version, spec_pb=None):
        meta_pb = model_pb2.DnsRecordMeta()
        meta_pb.id = dns_record_id
        meta_pb.namespace_id = namespace_id
        meta_pb.version = version
        req_pb = api_pb2.UpdateDnsRecordRequest(meta=meta_pb, spec=spec_pb)
        resp_pb = call(dns_record_service.update_dns_record, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_dns_record(namespace_id, dns_record_id).meta.version ==
                            resp_pb.dns_record.meta.version),
                   timeout=1)
        return resp_pb.dns_record

    @classmethod
    def get_name_server_config(cls, namespace_id, name_server_id):
        req_pb = api_pb2.GetNameServerConfigRequest(namespace_id=namespace_id, id=name_server_id)
        resp_pb = call(dns_record_service.get_name_server_config, req_pb, cls.TEST_LOGIN)
        return resp_pb

    @classmethod
    def create_balancer(cls, namespace_id, balancer_id, spec_pb, yp_cluster='SAS'):
        cls.set_login_to_root_users(cls.TEST_LOGIN)
        meta_pb = model_pb2.BalancerMeta()
        meta_pb.id = balancer_id
        meta_pb.namespace_id = namespace_id
        meta_pb.auth.type = meta_pb.auth.STAFF
        meta_pb.location.type = meta_pb.location.YP_CLUSTER
        meta_pb.location.yp_cluster = yp_cluster
        req_pb = api_pb2.CreateBalancerRequest(meta=meta_pb, spec=spec_pb)
        resp_pb = call(balancer_service.create_balancer, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_balancer(namespace_id, balancer_id).meta.version ==
                            resp_pb.balancer.meta.version),
                   timeout=1)
        return resp_pb.balancer

    @classmethod
    def update_balancer(cls, namespace_id, balancer_id, version, spec_pb=None, transport_paused_pb=None):
        """
        :rtype: model_pb2.Balancer
        """
        meta_pb = model_pb2.BalancerMeta()
        meta_pb.id = balancer_id
        meta_pb.namespace_id = namespace_id
        meta_pb.version = version
        if transport_paused_pb is not None:
            meta_pb.transport_paused.CopyFrom(transport_paused_pb)
        req_pb = api_pb2.UpdateBalancerRequest(meta=meta_pb, spec=spec_pb)
        resp_pb = call(balancer_service.update_balancer, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_balancer(namespace_id, balancer_id).meta.version ==
                            resp_pb.balancer.meta.version),
                   timeout=1)
        return resp_pb.balancer

    @classmethod
    def create_upstream(cls, namespace_id, upstream_id, spec_pb, login=TEST_LOGIN, disable_validation=False):
        meta_pb = model_pb2.UpstreamMeta()
        meta_pb.id = upstream_id
        meta_pb.namespace_id = namespace_id
        meta_pb.auth.type = meta_pb.auth.STAFF
        req_pb = api_pb2.CreateUpstreamRequest(meta=meta_pb, spec=spec_pb)

        def do_call():
            return call(upstream_service.create_upstream, req_pb, login)

        if disable_validation:
            with mock.patch.object(validation, 'validate_upstream_config'):
                resp_pb = do_call()
        else:
            resp_pb = do_call()

        wait_until(lambda: (cls.cache.get_upstream(namespace_id, upstream_id).meta.version ==
                            resp_pb.upstream.meta.version),
                   timeout=1)
        return resp_pb.upstream

    @classmethod
    def update_upstream(cls, namespace_id, upstream_id, version, spec_pb, disable_validation=False):
        meta_pb = model_pb2.UpstreamMeta()
        meta_pb.id = upstream_id
        meta_pb.version = version
        meta_pb.namespace_id = namespace_id
        req_pb = api_pb2.UpdateUpstreamRequest(meta=meta_pb, spec=spec_pb)

        def do_call():
            return call(upstream_service.update_upstream, req_pb, cls.TEST_LOGIN)

        if disable_validation:
            with mock.patch.object(validation, 'validate_upstream_config'):
                resp_pb = do_call()
        else:
            resp_pb = do_call()

        wait_until(lambda: (cls.cache.get_upstream(namespace_id, upstream_id).meta.version ==
                            resp_pb.upstream.meta.version),
                   timeout=1)
        return resp_pb.upstream

    @classmethod
    def remove_upstream(cls, namespace_id, upstream_id, version):
        req_pb = api_pb2.RemoveUpstreamRequest(id=upstream_id, namespace_id=namespace_id, version=version)
        resp_pb = call(upstream_service.remove_upstream, req_pb, cls.TEST_LOGIN)

    @classmethod
    def create_backend(cls, namespace_id, backend_id, spec_pb, validate_yp_endpoint_sets=False, is_system=False):
        meta_pb = model_pb2.BackendMeta()
        meta_pb.id = backend_id
        meta_pb.namespace_id = namespace_id
        meta_pb.auth.type = meta_pb.auth.STAFF
        meta_pb.is_system.value = is_system
        req_pb = api_pb2.CreateBackendRequest(meta=meta_pb, spec=spec_pb,
                                              validate_yp_endpoint_sets=validate_yp_endpoint_sets)
        resp_pb = call(backend_service.create_backend, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_backend(namespace_id, backend_id).meta.version ==
                            resp_pb.backend.meta.version),
                   timeout=1)
        return resp_pb.backend

    @classmethod
    def update_backend(cls, namespace_id, backend_id, version, spec_pb):
        meta_pb = model_pb2.BackendMeta()
        meta_pb.id = backend_id
        meta_pb.version = version
        meta_pb.namespace_id = namespace_id
        req_pb = api_pb2.UpdateBackendRequest(meta=meta_pb, spec=spec_pb)
        resp_pb = call(backend_service.update_backend, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_backend(namespace_id, backend_id).meta.version ==
                            resp_pb.backend.meta.version),
                   timeout=1)
        return resp_pb.backend

    @classmethod
    def create_endpoint_set(cls, namespace_id, endpoint_set_id, spec_pb, backend_version):
        meta_pb = model_pb2.EndpointSetMeta()
        meta_pb.id = endpoint_set_id
        meta_pb.namespace_id = namespace_id
        meta_pb.auth.type = meta_pb.auth.STAFF
        req_pb = api_pb2.CreateEndpointSetRequest(meta=meta_pb, backend_version=backend_version, spec=spec_pb)
        resp_pb = call(endpoint_set_service.create_endpoint_set, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_endpoint_set(namespace_id, endpoint_set_id).meta.version ==
                            resp_pb.endpoint_set.meta.version),
                   timeout=1)
        return resp_pb.endpoint_set

    @classmethod
    def update_endpoint_set(cls, namespace_id, endpoint_set_id, version, spec_pb, backend_version):
        meta_pb = model_pb2.EndpointSetMeta()
        meta_pb.id = endpoint_set_id
        meta_pb.version = version
        meta_pb.namespace_id = namespace_id
        req_pb = api_pb2.UpdateEndpointSetRequest(meta=meta_pb, backend_version=backend_version, spec=spec_pb)
        resp_pb = call(endpoint_set_service.update_endpoint_set, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_endpoint_set(namespace_id, endpoint_set_id).meta.version ==
                            resp_pb.endpoint_set.meta.version),
                   timeout=1)
        return resp_pb.endpoint_set

    @classmethod
    def create_weight_section(cls, namespace_id, weight_section_id, spec_pb):
        meta_pb = model_pb2.WeightSectionMeta(id=weight_section_id, namespace_id=namespace_id,
                                              type=model_pb2.WeightSectionMeta.ST_DC_WEIGHTS)
        req_pb = api_pb2.CreateWeightSectionRequest(meta=meta_pb, spec=spec_pb)
        resp_pb = call(weight_section_service.create_weight_section, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (objects.WeightSection.cache.must_get(namespace_id, weight_section_id).meta.version ==
                            resp_pb.weight_section.meta.version),
                   timeout=1)
        return resp_pb.weight_section

    @classmethod
    def update_weight_section(cls, namespace_id, weight_section_id, version, spec_pb):
        meta_pb = model_pb2.WeightSectionMeta(id=weight_section_id, version=version, namespace_id=namespace_id,
                                              type=model_pb2.WeightSectionMeta.ST_DC_WEIGHTS)
        req_pb = api_pb2.UpdateWeightSectionRequest(meta=meta_pb, spec=spec_pb)
        resp_pb = call(weight_section_service.update_weight_section, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (objects.WeightSection.cache.must_get(namespace_id, weight_section_id).meta.version ==
                            resp_pb.weight_section.meta.version),
                   timeout=1)
        return resp_pb.weight_section

    @classmethod
    def create_knob(cls, namespace_id, knob_id, spec_pb):
        meta_pb = model_pb2.KnobMeta()
        meta_pb.id = knob_id
        meta_pb.namespace_id = namespace_id
        meta_pb.auth.type = meta_pb.auth.STAFF
        req_pb = api_pb2.CreateKnobRequest(meta=meta_pb, spec=spec_pb)
        resp_pb = call(knob_service.create_knob, req_pb, cls.TEST_LOGIN)
        return resp_pb.knob

    # TODO: not actually API
    @classmethod
    def delete_knob(cls, namespace_id, knob_id):
        dao.IDao.instance().delete_knob(namespace_id, knob_id)

    # TODO: not actually API
    @classmethod
    def update_cert(cls, namespace_id, cert_id, version, spec_pb=None, meta_pb=None):
        cert_pb = dao.IDao.instance().update_cert(
            namespace_id=namespace_id,
            cert_id=cert_id,
            version=version,
            comment='Marked as complete by robot',
            login='robot',
            updated_meta_pb=meta_pb,
            updated_spec_pb=spec_pb)
        wait_until(lambda: cls.cache.get_cert(namespace_id, cert_id).meta.version == cert_pb.meta.version,
                   timeout=1)
        return cert_pb

    @classmethod
    def update_cert_meta(cls, namespace_id, cert_id, meta_pb):
        assert meta_pb.namespace_id == namespace_id and meta_pb.id == cert_id
        req_pb = api_pb2.UpdateCertificateRequest(meta=meta_pb)
        resp_pb = call(certificate_service.update_certificate, req_pb, cls.TEST_LOGIN)
        wait_until(lambda: (cls.cache.get_cert(namespace_id, cert_id).meta.discoverability ==
                            resp_pb.certificate.meta.discoverability), timeout=1)
        return resp_pb.certificate


user = namedtuple('user', 'login')
comment_id = namedtuple('comment_id', 'id')
DEFAULT_TEST_L7_MACRO_VERSION = '0.0.1'
DOMAINS_BALANCER_YAML_HEADER_TEMPLATE = '''l7_macro:
  version: %s
  http: {}
  https: {}
  health_check_reply: {}
  announce_check_reply:
    url_re: '/ping'
  include_domains: {}'''
EASY_BALANCER_YAML_HEADER_TEMPLATE = '''l7_macro:
  version: %s
  http: {}
  health_check_reply: {}
  announce_check_reply:
    url_re: '/ping'
  '''
COMMON_BALANCER_YAML_HEADER = '''instance_macro:
  buffer: 65535
  maxconn: 50
  workers: 10
  log_dir: /place/db/www/logs/
  sections:
    admin:
      ips:
        - 127.0.0.1
        - '::1'
      ports:
        - 1021
      http:
        maxlen: 65536
        maxreq: 65536
        admin: {}
    local_ips:
      ips:
        - !f get_ip_by_iproute("v4")
        - !f get_ip_by_iproute("v6")
      ports:
        - 1025
      stats_attr: ""
      extended_http_macro:
        port: 1025
        maxlen: 65530
        maxreq: 65535'''
BALANCER_YAML_HEADER = COMMON_BALANCER_YAML_HEADER + '''
        regexp:
          include_upstreams:
            filter:
              id_prefix_in:'''
INDEPENDENT_BALANCER_YAML_HEADER = COMMON_BALANCER_YAML_HEADER + '''
        errordocument: {status: 200}'''
UPSTREAM_REGEXP_SECTION_ERRORDOCUMENT_YAML = '''regexp_section:
  matcher:
    match_fsm:
      host: test.yandex.ru
  modules:
    - errordocument:
        status: 202'''
UPSTREAM_REGEXP_SECTION_BALANCER2_YAML_HEADER = '''regexp_section:
  matcher:
    match_fsm:
      cgi: 'wizclient=zen'
      surround: true
  modules:
    - balancer2:
        attempts: 2
        attempts_file: KNOB_ID
        timeout_policy:
          timeout: 1s
          retry_policy:
            unique_policy: {}
        rr: {}
        generated_proxy_backends:
          proxy_options:
            connect_timeout: 50ms
          include_backends:
            type: BY_ID
            ids:'''


def create_namespace_func(namespace_id, balancer_ids, l3_balancer_ids, dns_record_ids, upstream_ids, backend_ids, knob_ids,
                          cert_ids, domain_ids, balancer_upstream_links, l3_balancer_backend_links, dns_record_backend_links,
                          upstream_backend_links, upstream_knob_links,
                          dns_records_params=None, balancers_params=None, l3_balancers_params=None, knob_params=None,
                          cert_params=None, domain_params=None, backend_params=None):
    """
    :param six.text_type namespace_id:
    :param list[six.text_type] balancer_ids:
    :param list[six.text_type] l3_balancer_ids:
    :param list[six.text_type] dns_record_ids:
    :param list[six.text_type] domain_ids:
    :param list[six.text_type] upstream_ids:
    :param list[six.text_type] backend_ids:
    :param list[six.text_type] knob_ids:
    :param list[six.text_type] cert_ids:
    :param dict[six.text_type, list[six.text_type]] balancer_upstream_links:
    :param dict[six.text_type, list[six.text_type]] upstream_knob_links:
    :param dict[six.text_type, list[six.text_type]] l3_balancer_backend_links:
    :param dict[six.text_type, list[six.text_type]] dns_record_backend_links:
    :param dict[six.text_type, list[six.text_type]] upstream_backend_links:
    :param dict[six.text_type, dict] dns_records_params:
    :param dict[six.text_type, dict] balancers_params:
    :param dict[six.text_type, dict] l3_balancers_params:
    :param dict[six.text_type, dict] knob_params:
    :param dict[six.text_type, dict] cert_params:
    :param dict[six.text_type, dict] domain_params:
    :param dict[six.text_type, dict] backend_params:
    """
    dns_records_params = dns_records_params or {}
    balancers_params = balancers_params or {}
    l3_balancers_params = l3_balancers_params or {}
    knob_params = knob_params or {}
    cert_params = cert_params or {}
    domain_params = domain_params or {}
    backend_params = backend_params or {}
    assert not (domain_ids and balancer_upstream_links), 'cannot use both upstream_ids and domain_ids at the same time'

    Api.create_namespace(namespace_id=namespace_id)

    for domain_id in domain_ids:
        params = domain_params.get(domain_id, {})
        create_domain(namespace_id, domain_id,
                      fqdns=params['fqdns'],
                      upstream_id=params['upstream_id'],
                      cert_id=params['cert_id'],
                      incomplete=params['incomplete'])

    for balancer_id in balancer_ids:
        included_upstream_ids = sorted(balancer_upstream_links.get(balancer_id, []))
        params = balancers_params[balancer_id]
        mode = params.get('mode', model_pb2.YandexBalancerSpec.FULL_MODE)
        balancer_yml = make_balancer_yml(
            upstream_ids_to_include=included_upstream_ids,
            include_domains=bool(domain_ids),
            mode=mode)
        nanny_service_id = params['nanny_service_id']
        ctl_version = params.get('ctl_version', 0)
        yp_cluster = params.get('yp_cluster', 'SAS')
        balancer_spec_pb = make_balancer_spec_pb(nanny_service_id, balancer_yml, mode, ctl_version)
        Api.create_balancer(namespace_id=namespace_id, balancer_id=balancer_id, spec_pb=balancer_spec_pb,
                            yp_cluster=yp_cluster)

    for i, backend_id in enumerate(backend_ids):
        params = backend_params.get(backend_id, {})
        if params.get('is_sd'):
            backend_spec_pb = make_backend_spec_pb(yp_endpoint_sets_sd=[('sas', 'es_{}'.format(i))])
        else:
            backend_spec_pb = make_backend_spec_pb(nanny_service_ids=['service_{}'.format(i)])
        backend_spec_pb.is_global.value = params.get('is_global', False)
        is_system = params.get('is_system', False)
        Api.create_backend(namespace_id=namespace_id, backend_id=backend_id, spec_pb=backend_spec_pb, is_system=is_system)

    for l3_balancer_id in l3_balancer_ids:
        params = l3_balancers_params[l3_balancer_id]
        included_backend_ids = sorted(l3_balancer_backend_links.get(l3_balancer_id, []))
        l3mgr_service_id = params['l3mgr_service_id']
        l3_balancer_spec_pb = make_l3_balancer_spec_pb(l3mgr_service_id, included_backend_ids)
        Api.create_l3_balancer(namespace_id=namespace_id, l3_balancer_id=l3_balancer_id, spec_pb=l3_balancer_spec_pb)

    for i, upstream_id in enumerate(upstream_ids):
        included_backend_ids = sorted(upstream_backend_links.get(upstream_id, []))
        included_knob_ids = sorted(upstream_knob_links.get(upstream_id, []))
        upstream_yml = make_upstream_yml(backend_ids_to_include=included_backend_ids,
                                         knob_ids_to_include=included_knob_ids)
        upstream_spec_pb = make_upstream_spec_pb(upstream_yml)
        upstream_spec_pb.labels['order'] = six.text_type(i)
        Api.create_upstream(namespace_id=namespace_id, upstream_id=upstream_id, spec_pb=upstream_spec_pb)

    for dns_record_id in dns_record_ids:
        params = dns_records_params.get(dns_record_id, {})
        included_backend_ids = sorted(dns_record_backend_links.get(dns_record_id, []))
        full_name_server_id = params['full_name_server_id']
        zone = params['zone']
        dns_record_spec_pb = make_dns_record_spec_pb(namespace_id, full_name_server_id, zone, included_backend_ids)
        Api.create_dns_record(namespace_id=namespace_id, dns_record_id=dns_record_id, spec_pb=dns_record_spec_pb)

    for knob_id in knob_ids:
        create_knob(namespace_id, knob_id, knob_id,
                    knob_type=model_pb2.KnobSpec.INTEGER,
                    compatible_balancer_ids=frozenset(knob_params.get(knob_id, {}).get('allowed_for', [])))

    for cert_id in cert_ids:
        params = cert_params.get(cert_id, {})
        domains = params.get('domains', ())
        storage_type = params.get('type', 'nanny')
        incomplete = params.get('incomplete', False)
        create_cert(namespace_id, cert_id, domains=domains, storage_type=storage_type, incomplete=incomplete)


def make_balancer_yml(upstream_ids_to_include=(), include_domains=False,
                      mode=model_pb2.YandexBalancerSpec.FULL_MODE, l7_macro_version=None):
    assert not (upstream_ids_to_include and include_domains), 'cannot include both upstreams and domains'
    assert not l7_macro_version or l7_macro_version and mode == model_pb2.YandexBalancerSpec.EASY_MODE, 'l7_macro_version can be used only with easy mode'
    if mode == model_pb2.YandexBalancerSpec.EASY_MODE:
        if include_domains:
            parts = [DOMAINS_BALANCER_YAML_HEADER_TEMPLATE % (l7_macro_version or DEFAULT_TEST_L7_MACRO_VERSION)]
        else:
            parts = [EASY_BALANCER_YAML_HEADER_TEMPLATE % (l7_macro_version or DEFAULT_TEST_L7_MACRO_VERSION)]
    elif upstream_ids_to_include:
        parts = [BALANCER_YAML_HEADER]
        for upstream_id in upstream_ids_to_include:
            parts.append('                - ' + upstream_id)
    else:
        parts = [INDEPENDENT_BALANCER_YAML_HEADER]
    rv = '\n'.join(parts)
    return rv


def make_upstream_yml(backend_ids_to_include=None, knob_ids_to_include=None, cert_ids_to_include=None):
    """
    :type backend_ids_to_include: list[six.text_type]
    :type knob_ids_to_include: list[six.text_type]
    :type cert_ids_to_include: list[six.text_type]
    :rtype: six.text_type
    """
    backend_ids_to_include = backend_ids_to_include or []
    knob_ids_to_include = knob_ids_to_include or []
    cert_ids_to_include = cert_ids_to_include or []
    assert len(knob_ids_to_include) <= 1
    assert len(cert_ids_to_include) <= 1
    if backend_ids_to_include:
        parts = [UPSTREAM_REGEXP_SECTION_BALANCER2_YAML_HEADER]
        for backend_id in backend_ids_to_include:
            parts.append('            - {}'.format(backend_id))
        rv = '\n'.join(parts)
    else:
        rv = UPSTREAM_REGEXP_SECTION_ERRORDOCUMENT_YAML
    if knob_ids_to_include:
        knob_id = knob_ids_to_include[0]
        rv = rv.replace('KNOB_ID', '!k {}'.format(knob_id))
    else:
        rv = rv.replace('KNOB_ID', './some-random-file.txt')
    if cert_ids_to_include:
        cert_id = cert_ids_to_include[0]
        rv = rv.replace('CERT_ID', '!c {}'.format(cert_id))
    else:
        rv = rv.replace('CERT_ID', '/dev/shm/balancer/myCert.pem\n            priv: /dev/shm/balancer/priv/myPriv.pem')
    return rv


def make_l3_balancer_spec_pb(l3mgr_service_id, backend_ids_to_include):
    """
    :type l3mgr_service_id: six.text_type
    :type backend_ids_to_include: list[six.text_type]
    :rtype: model_pb2.L3BalancerSpec
    """
    spec_pb = model_pb2.L3BalancerSpec()
    spec_pb.l3mgr_service_id = l3mgr_service_id
    spec_pb.real_servers.type = model_pb2.L3BalancerRealServersSelector.BACKENDS
    for backend_id in backend_ids_to_include:
        spec_pb.real_servers.backends.add(id=backend_id)
    return spec_pb


def make_balancer_spec_pb(nanny_service_id, yml=None, mode=model_pb2.YandexBalancerSpec.FULL_MODE,
                          ctl_version=4, l7_macro_version=None):
    """
    :type nanny_service_id: six.text_type
    :type yml: six.text_type
    :type mode: model_pb2.YandexBalancerSpec.ConfigMode
    :type ctl_version: int
    :rtype: model_pb2.BalancerSpec
    """
    assert not l7_macro_version or l7_macro_version and mode == model_pb2.YandexBalancerSpec.EASY_MODE and not yml
    spec_pb = model_pb2.BalancerSpec()
    spec_pb.config_transport.type = model_pb2.NANNY_STATIC_FILE
    spec_pb.config_transport.nanny_static_file.service_id = nanny_service_id
    spec_pb.type = model_pb2.YANDEX_BALANCER
    spec_pb.yandex_balancer.mode = mode
    spec_pb.yandex_balancer.yaml = yml or make_balancer_yml(mode=mode, l7_macro_version=l7_macro_version)
    spec_pb.ctl_version = ctl_version
    return spec_pb


def make_upstream_spec_pb(yml, easy_mode=False):
    spec_pb = model_pb2.UpstreamSpec()
    if easy_mode:
        spec_pb.yandex_balancer.mode = spec_pb.yandex_balancer.EASY_MODE2
    spec_pb.yandex_balancer.yaml = yml
    return spec_pb


def make_backend_spec_pb(nanny_service_ids=(), gencfg_groups=(), yp_endpoint_sets_sd=()):
    assert not (nanny_service_ids and gencfg_groups)
    backend_spec_pb = model_pb2.BackendSpec()
    if nanny_service_ids:
        backend_spec_pb.selector.type = backend_spec_pb.selector.NANNY_SNAPSHOTS
        for service_id in nanny_service_ids:
            backend_spec_pb.selector.nanny_snapshots.add(service_id=service_id)
    if gencfg_groups:
        backend_spec_pb.selector.type = backend_spec_pb.selector.GENCFG_GROUPS
        for name, version in gencfg_groups:
            backend_spec_pb.selector.nanny_snapshots.add(name=name, snapshot_id=version)
    if yp_endpoint_sets_sd:
        backend_spec_pb.selector.type = backend_spec_pb.selector.YP_ENDPOINT_SETS_SD
        for cluster, id_ in yp_endpoint_sets_sd:
            backend_spec_pb.selector.yp_endpoint_sets.add(cluster=cluster, endpoint_set_id=id_)
    return backend_spec_pb


def create_knob(namespace_id, knob_id, ruchka_id, knob_type, compatible_balancer_ids=frozenset()):
    spec_pb = model_pb2.KnobSpec()
    spec_pb.mode = spec_pb.WATCHED
    spec_pb.type = knob_type
    spec_pb.its_watched_state.ruchka_id = ruchka_id
    spec_pb.its_watched_state.filename = ruchka_id
    for balancer_id in compatible_balancer_ids:
        spec_pb.its_watched_state.its_location_paths[balancer_id] = 'its/location'

    meta_pb = model_pb2.KnobMeta()
    meta_pb.id = knob_id
    meta_pb.namespace_id = namespace_id
    meta_pb.auth.type = meta_pb.auth.STAFF

    knob_pb = IDao.instance().create_knob(meta_pb, spec_pb, util.NANNY_ROBOT_LOGIN, comment='-')
    return knob_pb


def create_cert(namespace_id, cert_id, domains, storage_type, incomplete=True, discoverable=None):
    spec_pb = model_pb2.CertificateSpec()
    spec_pb.fields.subject_alternative_names.extend(domains)
    spec_pb.incomplete = incomplete

    if storage_type == 'yav':
        spec_pb.storage.type = model_pb2.CertificateSpec.Storage.YA_VAULT
        spec_pb.storage.ya_vault_secret.secret_id = 'some_secret_yav'
        spec_pb.storage.ya_vault_secret.secret_ver = 'some_ver'
    else:
        spec_pb.storage.type = model_pb2.CertificateSpec.Storage.NANNY_VAULT
        spec_pb.storage.nanny_vault_secret.keychain_id = 'some_keychain'
        spec_pb.storage.nanny_vault_secret.secret_id = 'some_secret_nanny'
        spec_pb.storage.nanny_vault_secret.secret_revision_id = 'some_rev'

    meta_pb = model_pb2.CertificateMeta()
    meta_pb.id = cert_id
    meta_pb.namespace_id = namespace_id
    meta_pb.auth.type = meta_pb.auth.STAFF
    if discoverable is not None:
        meta_pb.discoverability.default.value = discoverable

    cert_pb = IDao.instance().create_cert(meta_pb, util.NANNY_ROBOT_LOGIN, spec_pb=spec_pb)
    wait_until_passes(lambda: IAwacsCache.instance().must_get_cert(namespace_id, cert_id))
    return cert_pb


def create_domain(
        namespace_id, domain_id, fqdns, upstream_id, cert_id=None, incomplete=True,
        shadow_fqdns=None, yandex_tld=False):
    meta_pb = model_pb2.DomainMeta(id=domain_id, namespace_id=namespace_id)
    spec_pb = model_pb2.DomainSpec(incomplete=incomplete)
    config_pb = spec_pb.yandex_balancer.config
    if yandex_tld:
        config_pb.type = model_pb2.DomainSpec.Config.YANDEX_TLD
    if cert_id:
        config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
        config_pb.cert.id = cert_id
    else:
        config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    config_pb.include_upstreams.type = modules_pb2.BY_ID
    config_pb.include_upstreams.ids.extend([upstream_id])
    config_pb.fqdns.extend(fqdns)
    if shadow_fqdns:
        config_pb.shadow_fqdns.extend(shadow_fqdns)
    domain_pb = IDao.instance().create_domain(meta_pb, util.NANNY_ROBOT_LOGIN, spec_pb=spec_pb)
    wait_until_passes(lambda: IAwacsCache.instance().must_get_domain(namespace_id, domain_id))
    return domain_pb


def update_domain(namespace_id, domain_id, updated_spec_pb):
    current_pb = Api.get_domain(namespace_id, domain_id)
    new_pb = IDao.instance().update_domain(namespace_id, domain_id,
                                           current_pb.meta.version, 'comment', util.NANNY_ROBOT_LOGIN, updated_spec_pb)
    wait_until(lambda: (IAwacsCache.instance().get_domain(namespace_id, domain_id).meta.version == new_pb.meta.version))
    return new_pb


def make_dns_record_spec_pb(namespace_id, full_name_server_id, zone, backend_ids_to_include, ctl_version=0):
    """
    :type namespace_id: six.text_type
    :type full_name_server_id: (six.text_type, six.text_type)
    :type zone: six.text_type
    :type backend_ids_to_include: list[six.text_type]
    :type ctl_version: int
    :rtype: model_pb2.DnsRecordSpec
    """
    spec_pb = model_pb2.DnsRecordSpec()
    spec_pb.name_server.namespace_id = full_name_server_id[0]
    spec_pb.name_server.id = full_name_server_id[1]
    spec_pb.address.zone = zone
    spec_pb.ctl_version = ctl_version
    for backend_id in backend_ids_to_include:
        spec_pb.address.backends.backends.add(id=backend_id, namespace_id=namespace_id)
    return spec_pb


def create_ns(ns_id, cache, zk_storage, logins=None):
    logins = logins or ['ns1', 'ns2']
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = ns_id
    ns_pb.meta.auth.staff.owners.logins.extend(logins)
    zk_storage.create_namespace(ns_id, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(ns_id))
    return ns_pb
