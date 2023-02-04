# coding: utf-8
import inject
import mock
import pytest
import six
import yaml
from requests import Response
from sepelib.core import config

import awtest
from awacs.lib import rpc, yp_service_discovery, juggler_client, l3mgrclient, staffclient
from awacs.lib.rpc import exceptions
from awacs.lib.strutils import flatten_full_id
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.model import alerting, objects
from awacs.model import validation
from awacs.model.cache import IAwacsCache
from awacs.model.dao import IDao
from awacs.model.namespace.order import processors
from awacs.web import (
    namespace_service, balancer_service, upstream_service,
    backend_service, dns_record_service, l3_balancer_service,
    validation as webvalidation, endpoint_set_service, certificate_service, knob_service, domain_service)
from awacs.web.validation.util import MAX_AUTH_STAFF_LENGTH
from awtest.api import call, set_login_to_root_users, make_unknown_layout
from awtest.core import wait_until_passes
from awtest.mocks.staff_client import StaffMockClient
from awtest.mocks.yp_lite_client import YpLiteMockClient
from infra.awacs.proto import api_pb2, model_pb2, internals_pb2
from infra.swatlib.auth import abc


BALANCER_YAML = '''instance_macro:
  buffer: 65535
  maxconn: 50
  workers: 10
  log_dir: /place/db/www/logs/
  sections:
    admin:
      ips:
        - 127.0.0.1
      ports:
        - 1021
      http:
        maxlen: 65536
        maxreq: 65536
        admin: {}'''

DEFAULT_WARN_BALANCER_NOTIFY_RULE_KWARGS_TPL = """status:
    - from: OK
      to: WARN
login:
{}
method:
    - email
"""

DEFAULT_CRIT_BALANCER_NOTIFY_RULE_TPL = """status:
    - from: WARN
      to: CRIT
    - from: OK
      to: CRIT
login:
{}
method:
    - telegram
    - email
"""


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, l3_mgr_client, abc_client):
    def configure(b):
        resolver = mock.Mock()
        rsp = Response()
        rsp.status_code = 404
        resolver.resolve_endpoints.return_value = internals_pb2.TRspResolveEndpoints()
        b.bind(abc.IAbcClient, abc_client)
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        b.bind(IYpLiteRpcClient, YpLiteMockClient())
        b.bind(staffclient.IStaffClient, StaffMockClient())
        b.bind(yp_service_discovery.IResolver, resolver)
        b.bind(juggler_client.IJugglerClient, mock.Mock())
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


NS_ID = 'namespace_id'
LOGIN = 'login'
GROUP = "1"


def test_create_namespace_on_allocated_resources():
    login_1 = 'romanovich'
    namespace_id = 'namespace_1'
    namespace_cat = 'users/romanovich'

    req_pb = api_pb2.CreateNamespaceOnAllocatedResourcesRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF

    g_pb = req_pb.order.allocation.gencfg_groups['sas']
    g_pb.name = 'test-name'
    g_pb.version = 'test-version'

    b_pb = req_pb.order.backends['test-backend']
    b_pb.type = b_pb.NANNY_SNAPSHOTS
    b_pb.nanny_snapshots.add(service_id='a', snapshot_id='b')

    with pytest.raises(exceptions.BadRequestError, match='Creating GENCFG balancers is not supported'):
        call(namespace_service.create_namespace_on_allocated_resources, req_pb, login_1)


def test_create_yp_lite_namespace_order(dao):
    dao.create_default_name_servers()

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = NS_ID
    req_pb.meta.category = 'test'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.abc_service_id = 1821
    req_pb.order.flow_type = req_pb.order.YP_LITE
    req_pb.order.dns_record_request.type = req_pb.order.dns_record_request.DEFAULT
    req_pb.order.dns_record_request.default.name_server.id = 'in.yandex.net'
    req_pb.order.dns_record_request.default.name_server.namespace_id = 'infra'
    req_pb.order.dns_record_request.default.zone = 'new'
    req_pb.order.certificate_order_content.ca_name = 'InternalCA'
    req_pb.order.certificate_order_content.abc_service_id = 1821
    req_pb.order.certificate_order_content.common_name = 'yandex.yandex.yandex.ru'
    req_pb.order.alerting_simple_settings.notify_staff_group_id = 111
    req_pb.validate_yp_endpoint_sets = True

    with pytest.raises(exceptions.BadRequestError,
                       match='"order.yp_lite_allocation_request" must be set if flow_type is YP_LITE or QUICK_START'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.yp_lite_allocation_request.SetInParent()
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.yp_lite_allocation_request.nanny_service_id_slug" must be set'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.yp_lite_allocation_request.nanny_service_id_slug = 'slug'
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.yp_lite_allocation_request.locations" must be set'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.yp_lite_allocation_request.locations.append('vla')
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.yp_lite_allocation_request.network_macro" must be set'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.yp_lite_allocation_request.network_macro = '_SOME_MACRO_'
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.yp_lite_allocation_request.preset" must be set'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.yp_lite_allocation_request.preset.type = req_pb.order.yp_lite_allocation_request.preset.MICRO
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.yp_lite_allocation_request.preset.instances_count" must be greater than 0'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.yp_lite_allocation_request.preset.instances_count = 1
    with pytest.raises(exceptions.BadRequestError,
                       match='"order.backends" must be set'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.backends['test backend'].SetInParent()
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"order.backends\[test backend\]": backend_id "test backend" is not valid'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    del req_pb.order.backends['test backend']
    req_pb.order.backends['namespace_id_vla'].SetInParent()
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"order.backends\[namespace_id_vla\].type" must be specified'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.backends['namespace_id_vla'].type = model_pb2.BackendSelector.YP_ENDPOINT_SETS_SD
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"order.backends\[namespace_id_vla\].yp_endpoint_sets" must be specified if "order.'
                             r'backends\[namespace_id_vla\].type" is set to YP_ENDPOINT_SETS or YP_ENDPOINT_SETS_SD'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.backends['namespace_id_vla'].yp_endpoint_sets.add(cluster='sas', endpoint_set_id='test_es')
    with pytest.raises(exceptions.BadRequestError,
                       match='The following backend ids are reserved for awacs use, please rename them: '
                             '"namespace_id_vla"'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    req_pb.order.backends['test_backend'].CopyFrom(req_pb.order.backends['namespace_id_vla'])
    del req_pb.order.backends['namespace_id_vla']
    with pytest.raises(exceptions.BadRequestError,
                       match='Nanny service "rtc_balancer_namespace_id_vla" already exists. If it was created to '
                             'fulfill previous cancelled namespace order, please consider removing it manually.'):
        call(namespace_service.create_namespace, req_pb, LOGIN)

    with mock.patch.object(webvalidation.namespace, 'validate_yp_acl'):
        with mock.patch.object(webvalidation.namespace, 'validate_staff_group_ids'):
            with pytest.raises(exceptions.BadRequestError,
                               match='YP endpoint set "sas:test_es" does not exist'):
                call(namespace_service.create_namespace, req_pb, LOGIN)

            req_pb.validate_yp_endpoint_sets = False
            call(namespace_service.create_namespace, req_pb, LOGIN)


def test_create_and_update_namespace(zk_storage):
    login_1 = 'romanovich'
    login_2 = 'alonger'
    login_3 = 'jimmy'
    namespace_id = 'namespace_1'
    namespace_cat = 'users/romanovich'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    with pytest.raises(exceptions.BadRequestError) as e:
        with mock.patch.object(webvalidation.namespace, 'validate_request',
                               side_effect=exceptions.BadRequestError('Something is wrong')):
            call(namespace_service.create_namespace, req_pb, login_1)
    assert six.text_type(e.value) == 'Something is wrong'

    req_pb.meta.id = namespace_id + '.'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(namespace_service.create_namespace, req_pb, login_1)
    assert six.text_type(e.value) == u'"meta.id" is not valid'

    req_pb.meta.id = namespace_id
    resp_pb = call(namespace_service.create_namespace, req_pb, login_1)
    namespace_pb = resp_pb.namespace
    assert namespace_pb.meta.id == namespace_id
    assert namespace_pb.meta.category == namespace_cat
    assert namespace_pb.meta.auth.staff.owners.logins == [login_1]
    assert namespace_pb.spec.balancer_constraints.instance_tags.prj == namespace_id

    req_pb = api_pb2.ListNamespaceRevisionsRequest(id=namespace_id)
    resp_pb = call(namespace_service.list_namespace_revisions, req_pb, login_1)
    assert resp_pb.total == 1
    assert len(resp_pb.revisions) == 1
    rev_pb = resp_pb.revisions[0]
    assert rev_pb.meta.id == namespace_pb.meta.version
    assert rev_pb.meta.ctime == namespace_pb.meta.mtime
    assert rev_pb.meta.author == namespace_pb.meta.author

    req_pb = api_pb2.GetNamespaceRevisionRequest(id=namespace_pb.meta.version)
    resp_pb = call(namespace_service.get_namespace_revision, req_pb, login_1)
    assert resp_pb.revision.meta.id == namespace_pb.meta.version
    assert resp_pb.revision.spec.preset == model_pb2.NamespaceSpec.PR_DEFAULT

    req_pb = api_pb2.UpdateNamespaceRequest()
    with pytest.raises(exceptions.BadRequestError) as e:
        call(namespace_service.update_namespace, req_pb, login_1)
    assert six.text_type(e.value) == '"meta.id" must be set'

    req_pb.meta.id = namespace_id
    req_pb.meta.category = 'new_' + namespace_cat
    req_pb.meta.auth.staff.owners.logins.extend([login_1, login_2])

    with pytest.raises(exceptions.BadRequestError) as e:
        call(namespace_service.update_namespace, req_pb, login_2, enable_auth=True)
    assert six.text_type(e.value) == '"meta.auth.type" must be set'

    req_pb.meta.auth.type = req_pb.meta.auth.STAFF

    with pytest.raises(exceptions.ForbiddenError) as e:
        call(namespace_service.update_namespace, req_pb, login_2, enable_auth=True)
    assert six.text_type(e.value) == 'User "{}" is not authorized to perform such actions: "EDIT_AUTH"'.format(login_2)

    resp_pb = call(namespace_service.update_namespace, req_pb, login_1, enable_auth=True)
    namespace_pb = resp_pb.namespace
    assert namespace_pb.meta.auth.staff.owners.logins == [login_1, login_2]

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.id = namespace_id
    req_pb.meta.category = 'new_' + namespace_cat
    with pytest.raises(exceptions.BadRequestError) as e:
        call(namespace_service.update_namespace, req_pb, login_1, enable_auth=True)
    assert six.text_type(e.value) == '"auth.staff.owners": at least one of "logins" and "groups" must be not empty'

    req_pb = api_pb2.GetNamespaceRequest()

    def check_ns():
        with pytest.raises(exceptions.BadRequestError) as e:
            call(namespace_service.get_namespace, req_pb, login_1)
        assert six.text_type(e.value) == '"id" must be set'

    wait_until_passes(check_ns)

    req_pb.id = namespace_id
    resp_pb = call(namespace_service.get_namespace, req_pb, login_1)
    namespace_pb = resp_pb.namespace
    assert namespace_pb.meta.auth.staff.owners.logins == [login_1, login_2]
    assert namespace_pb.meta.category == 'new_' + namespace_cat

    req_pb = api_pb2.RemoveNamespaceRequest(id=namespace_id)
    with pytest.raises(exceptions.ForbiddenError) as e:
        call(namespace_service.remove_namespace, req_pb, login_3, enable_auth=True)
    assert six.text_type(e.value) == 'User "{}" is not authorized to remove this object'.format(login_3)

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.CopyFrom(namespace_pb.meta)
    resp_pb = call(namespace_service.update_namespace, req_pb, login_2)

    for c_pb in zk_storage.update_namespace(namespace_id):
        c_pb.order.status.status = 'FINISHED'

    req_pb = api_pb2.GetNamespaceRevisionRequest(id=resp_pb.namespace.meta.version)
    resp_pb = call(namespace_service.get_namespace_revision, req_pb, login_1)

    for cls, method, service in (
        (api_pb2.CreateBalancerRequest, balancer_service.create_balancer, balancer_service.balancer),
        (api_pb2.CreateL3BalancerRequest, l3_balancer_service.create_l3_balancer, l3_balancer_service.l3_balancer),
        (api_pb2.CreateUpstreamRequest, upstream_service.create_upstream, upstream_service.upstream),
        (api_pb2.CreateBackendRequest, backend_service.create_backend, backend_service.backend),
        (api_pb2.CreateEndpointSetRequest, endpoint_set_service.create_endpoint_set,
         endpoint_set_service.endpoint_set),
        (api_pb2.CreateCertificateRequest, certificate_service.order_certificate, certificate_service.certificate),
        (api_pb2.CreateDnsRecordRequest, dns_record_service.create_dns_record, dns_record_service.dns_record),
        (api_pb2.CreateKnobRequest, knob_service.create_knob, knob_service.knob),
    ):
        req_pb = cls()
        req_pb.meta.namespace_id = namespace_pb.meta.id
        with pytest.raises(exceptions.ForbiddenError) as e:
            with mock.patch.object(service, 'validate_request'):
                call(method, req_pb, login_3, enable_auth=True)
        assert (six.text_type(e.value) ==
                'User "{}" is not authorized to create objects in namespace "namespace_1"'.format(login_3))

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.abc_service_id = 780
    req_pb.meta.auth.staff.owners.logins.extend([str(i) for i in range(MAX_AUTH_STAFF_LENGTH + 1)])
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    with pytest.raises(exceptions.BadRequestError) as e:
        call(namespace_service.update_namespace, req_pb, login_1, enable_auth=True)
    assert six.text_type(e.value) == ('"meta.auth.staff.owners.logins": number of logins can not be '
                                      'more than {}'.format(MAX_AUTH_STAFF_LENGTH))

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([str(i) for i in range(MAX_AUTH_STAFF_LENGTH + 1)])
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    with pytest.raises(exceptions.BadRequestError) as e:
        call(namespace_service.update_namespace, req_pb, login_1, enable_auth=True)
    assert six.text_type(e.value) == ('"meta.auth.staff.owners.group_ids": number of group ids can not be '
                                      'more than {}'.format(MAX_AUTH_STAFF_LENGTH))

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend(['ferenets'] * (MAX_AUTH_STAFF_LENGTH + 1))
    req_pb.meta.auth.staff.owners.group_ids.extend(['0'] + ['780'] * (MAX_AUTH_STAFF_LENGTH + 1) + ['2'])
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    resp_pb = call(namespace_service.update_namespace, req_pb, login_1, enable_auth=True)
    assert resp_pb.namespace.meta.auth.staff.owners.logins == ['ferenets']
    assert resp_pb.namespace.meta.auth.staff.owners.group_ids == ['0', '780', '2']


def test_create_testing_namespace():
    login_1 = 'romanovich'
    namespace_id = 'namespace_1'
    namespace_cat = 'users/romanovich'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.order.flow_type = req_pb.order.QUICK_START
    req_pb.order.yp_lite_allocation_request.nanny_service_id_slug = 'slug'
    req_pb.order.yp_lite_allocation_request.locations.append('sas')
    req_pb.order.yp_lite_allocation_request.network_macro = '_FERENETS_'
    req_pb.order.yp_lite_allocation_request.preset.type = req_pb.order.yp_lite_allocation_request.preset.MICRO
    req_pb.order.yp_lite_allocation_request.preset.instances_count = 2
    req_pb.order.certificate_order_content.ca_name = 'InternalCA'
    req_pb.order.certificate_order_content.abc_service_id = 1821
    req_pb.order.certificate_order_content.common_name = 'yandex.yandex.ru'
    req_pb.order.alerting_simple_settings.notify_staff_group_id = 111

    req_pb.order.env_type = model_pb2.NamespaceSpec.NS_ENV_TESTING
    req_pb.validate_yp_endpoint_sets = False
    es = req_pb.order.endpoint_sets.add()
    es.cluster = 'sas'
    es.id = '1'

    with mock.patch.object(webvalidation.namespace, 'validate_yp_acl'):
        with mock.patch.object(webvalidation.namespace, 'validate_staff_group_ids'):
            resp_pb = call(namespace_service.create_namespace, req_pb, login_1)
    namespace_pb = resp_pb.namespace
    assert namespace_pb.meta.id == namespace_id
    assert namespace_pb.meta.category == namespace_cat
    assert namespace_pb.spec.env_type == req_pb.order.env_type


def test_update_namespace_alerting_notifications(cache, zk_storage, ctx):
    login = 'romanovich'
    namespace_id = 'namespace_alerting'
    namespace_cat = 'users/romanovich'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])

    resp_pb = call(namespace_service.create_namespace, req_pb, login)

    processors.Finalizing(processors.NamespaceOrder(resp_pb.namespace)).process(ctx)
    make_unknown_layout(namespace_id, LOGIN)

    req_pb = api_pb2.GetNamespaceRequest(id=namespace_id, consistency=api_pb2.STRONG)
    resp_pb = call(namespace_service.get_namespace, req_pb, login)

    balancer_id = 'balancer-id'
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.namespace_id = namespace_id
    balancer_pb.meta.id = balancer_id
    balancer_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    balancer_pb.meta.location.yp_cluster = 'SAS'
    balancer_pb.spec.config_transport.nanny_static_file.instance_tags.itype = 'test'
    balancer_pb.spec.config_transport.nanny_static_file.instance_tags.ctype = 'balancer'
    cache._set_balancer_pb(namespace_id + '/' + balancer_id, balancer_pb)

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.version = resp_pb.namespace.meta.version
    req_pb.meta.auth.staff.owners.logins.append(login)
    req_pb.spec.alerting.version = str(alerting.CURRENT_VERSION)
    req_pb.spec.alerting.notify_rules_disabled = False

    with pytest.raises(exceptions.BadRequestError):
        call(namespace_service.update_namespace, req_pb, login)

    req_pb.spec.alerting.notify_rules_disabled = True
    resp_pb = call(namespace_service.update_namespace, req_pb, login)
    assert not resp_pb.namespace.spec.alerting.HasField('juggler_raw_notify_rules')
    assert resp_pb.namespace.spec.alerting.notify_rules_disabled

    req_pb.meta.version = resp_pb.namespace.meta.version
    rule = req_pb.spec.alerting.juggler_raw_notify_rules.balancer.add()
    rule.template_name = 'on_status_change'
    rule.template_kwargs = """
        status:
            - from: OK
              to: CRIT
            - from: WARN
              to: CRIT
        login:
            - romanovich
        method:
            - sms
    """
    resp_pb = call(namespace_service.update_namespace, req_pb, login)
    assert not resp_pb.namespace.spec.alerting.HasField('notify_rules_disabled')
    assert resp_pb.namespace.spec.alerting.HasField('juggler_raw_notify_rules')


def test_update_namespace_readonly_fields(cache, zk_storage, ctx):
    login = 'romanovich'
    unprivileged_login = 'someone'
    namespace_id = 'namespace_alerting'
    namespace_cat = 'users/romanovich'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend((login, unprivileged_login))
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])

    resp_pb = call(namespace_service.create_namespace, req_pb, login)

    processors.Finalizing(processors.NamespaceOrder(resp_pb.namespace)).process(ctx)
    make_unknown_layout(namespace_id, LOGIN)

    req_pb = api_pb2.GetNamespaceRequest(id=namespace_id, consistency=api_pb2.STRONG)
    resp_pb = call(namespace_service.get_namespace, req_pb, login)

    balancer_id = 'balancer-id'
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.namespace_id = namespace_id
    balancer_pb.meta.id = balancer_id
    balancer_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    balancer_pb.meta.location.yp_cluster = 'SAS'
    balancer_pb.spec.config_transport.nanny_static_file.instance_tags.itype = 'test'
    balancer_pb.spec.config_transport.nanny_static_file.instance_tags.ctype = 'balancer'
    cache._set_balancer_pb(namespace_id + '/' + balancer_id, balancer_pb)

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.version = resp_pb.namespace.meta.version
    req_pb.meta.auth.staff.owners.logins.extend((login, unprivileged_login))
    req_pb.spec.alerting.version = str(alerting.CURRENT_VERSION)
    req_pb.spec.alerting.notify_rules_disabled = True

    resp_pb = call(namespace_service.update_namespace, req_pb, login)

    req_pb.meta.version = resp_pb.namespace.meta.version
    req_pb.spec.alerting.balancer_checks_disabled = True

    with pytest.raises(exceptions.BadRequestError):
        call(namespace_service.update_namespace, req_pb, unprivileged_login, enable_auth=True)

    req_pb.spec.alerting.balancer_checks_disabled = False
    resp_pb = call(namespace_service.update_namespace, req_pb, unprivileged_login, enable_auth=True)

    assert resp_pb.namespace.spec.alerting.balancer_checks_disabled is False

    req_pb.meta.version = resp_pb.namespace.meta.version
    req_pb.spec.preset = model_pb2.NamespaceSpec.PR_WITHOUT_NOTIFICATIONS

    with pytest.raises(exceptions.BadRequestError):
        call(namespace_service.update_namespace, req_pb, unprivileged_login, enable_auth=True)


def test_cancel_namespace_order(zk_storage):
    login = 'someone'
    namespace_id = 'namespace-1'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = 'test'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    req_pb.meta.abc_service_id = 123
    ns_pb = call(namespace_service.create_namespace, req_pb, login).namespace

    req_pb = api_pb2.CancelNamespaceOrderRequest()
    req_pb.id = namespace_id
    req_pb.version = 'xxx'

    for c_pb in zk_storage.update_namespace(namespace_id):
        c_pb.order.status.status = 'FINISHED'
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel order that is not in progress'):
        call(namespace_service.cancel_namespace_order, req_pb, login)

    for c_pb in zk_storage.update_namespace(namespace_id):
        c_pb.order.status.status = 'IN_PROGRESS'

    req_pb.version = ns_pb.meta.version
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel namespace order at this stage'):
        call(namespace_service.cancel_namespace_order, req_pb, login)

    for c_pb in zk_storage.update_namespace(namespace_id):
        c_pb.order.progress.state.id = 'WAITING_FOR_BALANCERS_TO_ALLOCATE'
    call(namespace_service.cancel_namespace_order, req_pb, login)

    def check():
        get_req_pb = api_pb2.GetNamespaceRequest(id=namespace_id)
        pb = call(namespace_service.get_namespace, get_req_pb, login)
        assert pb.namespace.order.cancelled.value

    wait_until_passes(check)


def test_force_cancel_namespace_order(zk_storage):
    login = 'someone'
    namespace_id = 'namespace-1'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = 'test'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    req_pb.meta.abc_service_id = 123
    ns_pb = call(namespace_service.create_namespace, req_pb, login).namespace
    for c_pb in zk_storage.update_namespace(namespace_id):
        c_pb.order.status.status = 'IN_PROGRESS'

    req_pb = api_pb2.CancelNamespaceOrderRequest()
    req_pb.id = namespace_id
    req_pb.version = ns_pb.meta.version
    req_pb.force = True

    with pytest.raises(exceptions.ForbiddenError, match='Only root users can force cancel namespace'):
        call(namespace_service.cancel_namespace_order, req_pb, login)

    config.set_value('run.root_users', (login,))
    call(namespace_service.cancel_namespace_order, req_pb, login)

    def check():
        get_req_pb = api_pb2.GetNamespaceRequest(id=namespace_id)
        pb = call(namespace_service.get_namespace, get_req_pb, login)
        assert pb.namespace.order.cancelled.value
        assert pb.namespace.order.status.status == 'CANCELLED'
        assert pb.namespace.order.progress.state.id == 'FINISH'

    wait_until_passes(check)


def test_remove_namespace_1(mongo_storage, cache, zk_storage, ctx):
    """
    :type mongo_storage: MongoStorage
    """
    login_1 = 'romanovich'
    set_login_to_root_users(login_1)
    for namespace_id in ('namespace_id_1', 'namespace_id_2', 'namespace_id_3'):
        req_pb = api_pb2.CreateNamespaceRequest()
        req_pb.meta.id = namespace_id
        req_pb.meta.category = 'test'
        req_pb.meta.abc_service_id = 123
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
        call(namespace_service.create_namespace, req_pb, login_1)

        for c_pb in zk_storage.update_namespace(namespace_id):
            c_pb.order.status.status = 'FINISHED'

    upstream_yml = yaml.dump({
        'errorlog': {
            'log_level': 'DEBUG',
            'log': '/tmp/log.txt',
        },
    })

    for ns_idx in (1, 2, 3):
        for dc in ('sas', 'man', 'vla'):
            namespace_id = 'namespace_id_{}'.format(ns_idx)
            balancer_id = 'balancer_id_{}_{}'.format(ns_idx, dc)
            req_pb = api_pb2.CreateBalancerRequest()
            req_pb.meta.id = balancer_id
            req_pb.meta.namespace_id = namespace_id
            req_pb.meta.auth.type = req_pb.meta.auth.STAFF
            req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
            req_pb.meta.location.yp_cluster = dc.upper()
            unique_suffix = hash(namespace_id + balancer_id)
            req_pb.spec.config_transport.nanny_static_file.service_id = 'prod_balancer_{}'.format(unique_suffix)
            req_pb.spec.type = model_pb2.YANDEX_BALANCER
            req_pb.spec.yandex_balancer.yaml = BALANCER_YAML

            call(balancer_service.create_balancer, req_pb, login_1)

            balancer_op_pb = model_pb2.BalancerOperation()
            balancer_op_pb.meta.id = balancer_id
            balancer_op_pb.meta.namespace_id = namespace_id
            zk_storage.create_balancer_operation(namespace_id, balancer_id, balancer_op_pb)

            l3_balancer_id = 'l3_balancer_id_{}_{}'.format(ns_idx, dc)
            req_pb = api_pb2.CreateL3BalancerRequest()
            req_pb.meta.id = l3_balancer_id
            req_pb.meta.namespace_id = namespace_id
            req_pb.meta.auth.type = req_pb.meta.auth.STAFF
            req_pb.spec.l3mgr_service_id = str(hash(balancer_id + namespace_id))
            req_pb.spec.real_servers.backends.add(id='test')

            with mock.patch.object(webvalidation.l3_balancer, 'validate_request'):
                call(l3_balancer_service.create_l3_balancer, req_pb, login_1)

        for dc in ('sas', 'man', 'vla'):
            req_pb = api_pb2.CreateUpstreamRequest()
            req_pb.meta.id = 'upstream_id_{}'.format(dc)
            req_pb.meta.namespace_id = namespace_id
            req_pb.meta.auth.type = req_pb.meta.auth.STAFF
            req_pb.spec.type = model_pb2.YANDEX_BALANCER
            req_pb.spec.yandex_balancer.yaml = upstream_yml

            with mock.patch.object(validation, 'validate_upstream_config'):
                call(upstream_service.create_upstream, req_pb, login_1)

        for dc in ('sas', 'man', 'vla'):
            req_pb = api_pb2.CreateBackendRequest()
            req_pb.meta.id = 'backend_id_{}'.format(dc)
            req_pb.meta.namespace_id = namespace_id
            req_pb.meta.auth.type = req_pb.meta.auth.STAFF
            with mock.patch.object(webvalidation.backend, 'validate_request'):
                call(backend_service.create_backend, req_pb, login_1)

        for dc in ('sas', 'man', 'vla'):
            dns_record_id = 'dns_record_id_{}'.format(dc)
            req_pb = api_pb2.CreateDnsRecordRequest()
            req_pb.meta.id = dns_record_id
            req_pb.meta.namespace_id = namespace_id
            req_pb.meta.auth.type = req_pb.meta.auth.STAFF
            req_pb.spec.address.zone = namespace_id + dns_record_id
            with mock.patch.object(webvalidation.dns_record, 'validate_request'):
                call(dns_record_service.create_dns_record, req_pb, login_1)

        for dc in ('sas', 'man', 'vla'):
            cert_id = 'cert_id_{}'.format(dc)
            req_pb = api_pb2.CreateCertificateRequest()
            req_pb.meta.id = cert_id
            req_pb.meta.namespace_id = namespace_id
            req_pb.meta.auth.type = req_pb.meta.auth.STAFF
            req_pb.spec.storage.ya_vault_secret.secret_id = namespace_id + ':' + cert_id
            req_pb.spec.storage.ya_vault_secret.secret_ver = '123'
            with mock.patch.object(webvalidation.certificate, 'validate_request'):
                call(certificate_service.create_certificate, req_pb, login_1)

        for dc in ('sas', 'man', 'vla'):
            domain_id = 'd_id_{}'.format(dc)
            domain_pb = model_pb2.Domain()
            domain_pb.meta.id = domain_id
            domain_pb.meta.namespace_id = namespace_id
            zk_storage.create_domain(namespace_id, domain_id, domain_pb)
            domain_op_pb = model_pb2.DomainOperation()
            domain_op_pb.meta.id = domain_id
            domain_op_pb.meta.namespace_id = namespace_id
            zk_storage.create_domain_operation(namespace_id, domain_id, domain_op_pb)

    def check_ns():
        for ns_idx in (1, 2, 3):
            namespace_id = 'namespace_id_{}'.format(ns_idx)
            for dc in ('sas', 'man', 'vla'):
                balancer_id = 'balancer_id_{}_{}'.format(ns_idx, dc)
                l3_balancer_id = 'l3_balancer_id_{}_{}'.format(ns_idx, dc)
                upstream_id = 'upstream_id_{}'.format(dc)
                backend_id = 'backend_id_{}'.format(dc)
                dns_record_id = 'dns_record_id_{}'.format(dc)
                cert_id = 'cert_id_{}'.format(dc)
                domain_id = 'd_id_{}'.format(dc)
                assert mongo_storage.list_balancer_revs(namespace_id, balancer_id).total == 1
                assert mongo_storage.list_upstream_revs(namespace_id, upstream_id).total == 1
                assert mongo_storage.list_backend_revs(namespace_id, backend_id).total == 1
                assert mongo_storage.list_l3_balancer_revs(namespace_id, l3_balancer_id).total == 1
                assert mongo_storage.list_dns_record_revs(namespace_id, dns_record_id).total == 1
                assert mongo_storage.list_cert_revs(namespace_id, cert_id).total == 1

                req_pb = api_pb2.GetBalancerRequest(id=balancer_id, namespace_id=namespace_id)
                call(balancer_service.get_balancer, req_pb, login_1)
                req_pb = api_pb2.GetBalancerOperationRequest(id=balancer_id, namespace_id=namespace_id)
                call(balancer_service.get_balancer_operation, req_pb, login_1)

                req_pb = api_pb2.GetL3BalancerRequest(id=l3_balancer_id, namespace_id=namespace_id)
                call(l3_balancer_service.get_l3_balancer, req_pb, login_1)

                req_pb = api_pb2.GetUpstreamRequest(id=upstream_id, namespace_id=namespace_id)
                call(upstream_service.get_upstream, req_pb, login_1)

                req_pb = api_pb2.GetBackendRequest(id=backend_id, namespace_id=namespace_id)
                call(backend_service.get_backend, req_pb, login_1)

                req_pb = api_pb2.GetDnsRecordRequest(id=dns_record_id, namespace_id=namespace_id)
                call(dns_record_service.get_dns_record, req_pb, login_1)

                req_pb = api_pb2.GetCertificateRequest(id=cert_id, namespace_id=namespace_id)
                call(certificate_service.get_certificate, req_pb, login_1)

                req_pb = api_pb2.GetDomainRequest(id=domain_id, namespace_id=namespace_id)
                call(domain_service.get_domain, req_pb, login_1)
                req_pb = api_pb2.GetDomainOperationRequest(id=domain_id, namespace_id=namespace_id)
                call(domain_service.get_domain_operation, req_pb, login_1)

    wait_until_passes(check_ns)

    req_pb = api_pb2.RemoveNamespaceRequest(id='namespace_id_2')
    with pytest.raises(rpc.exceptions.BadRequestError) as e:
        call(namespace_service.remove_namespace, req_pb, login_1)
    assert e.match(r'Deleting this namespace is not possible: it has 3 balancer\(s\)\. '
                   r'Please remove all balancers before removing the namespace\.')
    for dc in ('sas', 'man', 'vla'):
        IDao.instance().delete_balancer('namespace_id_2', 'balancer_id_2_{}'.format(dc))

    def check_ns():
        req_pb = api_pb2.RemoveNamespaceRequest(id='namespace_id_2')
        with pytest.raises(rpc.exceptions.BadRequestError) as e:
            call(namespace_service.remove_namespace, req_pb, login_1)
        assert e.match(r'Deleting this namespace is not possible: it has 3 L3 balancer\(s\)\. '
                       r'Please remove all L3 balancers before removing the namespace\.')

    wait_until_passes(check_ns)

    for dc in ('sas', 'man', 'vla'):
        IDao.instance().delete_l3_balancer('namespace_id_2', 'l3_balancer_id_2_{}'.format(dc))

    def check_ns():
        req_pb = api_pb2.RemoveNamespaceRequest(id='namespace_id_2')
        with pytest.raises(rpc.exceptions.BadRequestError) as e:
            call(namespace_service.remove_namespace, req_pb, login_1)
        assert e.match(r'Deleting this namespace is not possible: it has 3 domain\(s\)\. '
                       r'Please remove all domains before removing the namespace\.')

    wait_until_passes(check_ns)

    for dc in ('sas', 'man', 'vla'):
        IDao.instance().delete_domain('namespace_id_2', 'd_id_{}'.format(dc))

    def check_ns():
        req_pb = api_pb2.RemoveNamespaceRequest(id='namespace_id_2')
        with pytest.raises(rpc.exceptions.BadRequestError) as e:
            call(namespace_service.remove_namespace, req_pb, login_1)
        assert e.match(r'Deleting this namespace is not possible: it has 3 certificate\(s\)\. '
                       r'Please remove all certificates before removing the namespace\.')

    wait_until_passes(check_ns)

    for dc in ('sas', 'man', 'vla'):
        IDao.instance().delete_cert('namespace_id_2', 'cert_id_{}'.format(dc))

    def check_ns():
        req_pb = api_pb2.RemoveNamespaceRequest(id='namespace_id_2')
        with pytest.raises(rpc.exceptions.BadRequestError) as e:
            call(namespace_service.remove_namespace, req_pb, login_1)
        assert e.match(r'Deleting this namespace is not possible: it has 3 DNS record\(s\)\. '
                       r'Please remove all DNS records before removing the namespace\.')

    wait_until_passes(check_ns)

    for dc in ('sas', 'man', 'vla'):
        IDao.instance().delete_dns_record('namespace_id_2', 'dns_record_id_{}'.format(dc))

    call(namespace_service.remove_namespace, req_pb, login_1)

    def check_remove():
        for ns_idx in (1, 2, 3):
            namespace_id = 'namespace_id_{}'.format(ns_idx)

            for dc in ('sas', 'man', 'vla'):
                balancer_id = 'balancer_id_{}_{}'.format(ns_idx, dc)
                upstream_id = 'upstream_id_{}'.format(dc)
                backend_id = 'backend_id_{}'.format(dc)
                l3_balancer_id = 'l3_balancer_id_{}_{}'.format(ns_idx, dc)
                dns_record_id = 'dns_record_id_{}'.format(dc)
                cert_id = 'cert_id_{}'.format(dc)
                domain_id = 'd_id_{}'.format(dc)
                if ns_idx == 2:
                    assert mongo_storage.list_balancer_revs(namespace_id, balancer_id).total == 0
                    assert mongo_storage.list_upstream_revs(namespace_id, upstream_id).total == 1
                    assert mongo_storage.list_backend_revs(namespace_id, backend_id).total == 1
                    assert mongo_storage.list_dns_record_revs(namespace_id, dns_record_id).total == 0
                    assert mongo_storage.list_cert_revs(namespace_id, cert_id).total == 0

                    req_pb = api_pb2.GetNamespaceRequest(id=namespace_id)
                    namespace_pb = call(namespace_service.get_namespace, req_pb, login_1).namespace
                    assert namespace_pb.spec.deleted

                    req_pb = api_pb2.GetBalancerRequest(id=balancer_id, namespace_id=namespace_id)
                    with pytest.raises(exceptions.NotFoundError):
                        call(balancer_service.get_balancer, req_pb, login_1)
                    req_pb = api_pb2.GetBalancerOperationRequest(id=balancer_id, namespace_id=namespace_id)
                    with pytest.raises(exceptions.NotFoundError):
                        call(balancer_service.get_balancer_operation, req_pb, login_1)

                    req_pb = api_pb2.GetL3BalancerRequest(id=balancer_id, namespace_id=namespace_id)
                    with pytest.raises(exceptions.NotFoundError):
                        call(l3_balancer_service.get_l3_balancer, req_pb, login_1)

                    req_pb = api_pb2.GetUpstreamRequest(id=upstream_id, namespace_id=namespace_id)
                    call(upstream_service.get_upstream, req_pb, login_1)

                    req_pb = api_pb2.GetBackendRequest(id=backend_id, namespace_id=namespace_id)
                    call(backend_service.get_backend, req_pb, login_1)

                    req_pb = api_pb2.GetDnsRecordRequest(id=dns_record_id, namespace_id=namespace_id)
                    with pytest.raises(exceptions.NotFoundError):
                        call(dns_record_service.get_dns_record, req_pb, login_1)

                    req_pb = api_pb2.GetCertificateRequest(id=cert_id, namespace_id=namespace_id)
                    with pytest.raises(exceptions.NotFoundError):
                        call(certificate_service.get_certificate, req_pb, login_1)

                    req_pb = api_pb2.GetDomainRequest(id=domain_id, namespace_id=namespace_id)
                    with pytest.raises(exceptions.NotFoundError):
                        call(domain_service.get_domain, req_pb, login_1)
                    req_pb = api_pb2.GetDomainOperationRequest(id=domain_id, namespace_id=namespace_id)
                    with pytest.raises(exceptions.NotFoundError):
                        call(domain_service.get_domain_operation, req_pb, login_1)
                else:
                    assert mongo_storage.list_balancer_revs(namespace_id, balancer_id).total == 1
                    assert mongo_storage.list_upstream_revs(namespace_id, upstream_id).total == 1
                    assert mongo_storage.list_backend_revs(namespace_id, backend_id).total == 1
                    assert mongo_storage.list_dns_record_revs(namespace_id, dns_record_id).total == 1
                    assert mongo_storage.list_cert_revs(namespace_id, cert_id).total == 1

                    req_pb = api_pb2.GetBalancerRequest(id=balancer_id, namespace_id=namespace_id)
                    call(balancer_service.get_balancer, req_pb, login_1)
                    req_pb = api_pb2.GetBalancerOperationRequest(id=balancer_id, namespace_id=namespace_id)
                    call(balancer_service.get_balancer_operation, req_pb, login_1)

                    req_pb = api_pb2.GetL3BalancerRequest(id=l3_balancer_id, namespace_id=namespace_id)
                    call(l3_balancer_service.get_l3_balancer, req_pb, login_1)

                    req_pb = api_pb2.GetUpstreamRequest(id=upstream_id, namespace_id=namespace_id)
                    call(upstream_service.get_upstream, req_pb, login_1)

                    req_pb = api_pb2.GetBackendRequest(id=backend_id, namespace_id=namespace_id)
                    call(backend_service.get_backend, req_pb, login_1)

                    req_pb = api_pb2.GetDnsRecordRequest(id=dns_record_id, namespace_id=namespace_id)
                    call(dns_record_service.get_dns_record, req_pb, login_1)

                    req_pb = api_pb2.GetCertificateRequest(id=cert_id, namespace_id=namespace_id)
                    call(certificate_service.get_certificate, req_pb, login_1)

                    req_pb = api_pb2.GetDomainRequest(id=domain_id, namespace_id=namespace_id)
                    call(domain_service.get_domain, req_pb, login_1)
                    req_pb = api_pb2.GetDomainOperationRequest(id=domain_id, namespace_id=namespace_id)
                    call(domain_service.get_domain_operation, req_pb, login_1)

    wait_until_passes(check_remove)


def test_remove_namespace_2(zk_storage, cache):
    """
    :type zk_storage: awacs.model.zk.ZkStorage
    """
    login = 'someone'
    namespace_1_id = 'namespace-1'
    namespace_2_id = 'namespace-2'
    balancer_1_id = 'balancer-1'
    backend_id = 'backend-1'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_1_id
    req_pb.meta.category = 'test'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    req_pb.meta.abc_service_id = 123
    call(namespace_service.create_namespace, req_pb, login)

    for c_pb in zk_storage.update_namespace(namespace_1_id):
        c_pb.order.status.status = 'FINISHED'

    req_pb = api_pb2.CreateBackendRequest()
    req_pb.meta.id = backend_id
    req_pb.meta.namespace_id = namespace_1_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    with mock.patch.object(webvalidation.backend, 'validate_request'):
        call(backend_service.create_backend, req_pb, login)

    other_balancer_state_pb = model_pb2.BalancerState(
        namespace_id=namespace_2_id,
        balancer_id=balancer_1_id,
    )
    flat_backend_id = flatten_full_id(other_balancer_state_pb.namespace_id, (namespace_1_id, backend_id))
    other_balancer_state_pb.backends[flat_backend_id].statuses.add()
    zk_storage.create_balancer_state(namespace_2_id, balancer_1_id, other_balancer_state_pb)

    def check_remove():
        req_pb = api_pb2.RemoveNamespaceRequest(id=namespace_1_id)
        with pytest.raises(exceptions.BadRequestError) as e:
            call(namespace_service.remove_namespace, req_pb, login)
        assert e.match('backend "namespace-1:backend-1" is used in other namespaces '
                       'by the following balancers: "namespace-2/balancer-1"')

    wait_until_passes(check_remove)

    zk_storage.remove_balancer_state(namespace_2_id, balancer_1_id)
    req_pb = api_pb2.RemoveNamespaceRequest(id=namespace_1_id)
    call(namespace_service.remove_namespace, req_pb, login)

    def check():
        req_pb_ = api_pb2.GetNamespaceRequest(id=namespace_1_id)
        assert call(namespace_service.get_namespace, req_pb_, login).namespace.spec.deleted

    wait_until_passes(check)


def test_list_namespaces():
    ids = ['namespace_1', 'namespace_2', 'namespace_3']
    categories = ['cat_1', 'cat_2', 'cat_3']
    cache = IAwacsCache.instance()
    for i in range(3):
        id = ids[i]
        category = categories[i]
        namespace = model_pb2.Namespace()
        namespace.meta.id = id
        namespace.meta.category = category
        cache._set_namespace_pb(id, namespace)

    login = 'someone'

    req_pb = api_pb2.ListNamespacesRequest()
    resp_pb = call(namespace_service.list_namespaces, req_pb, login)
    resp_ids = [n_pb.meta.id for n_pb in resp_pb.namespaces]
    assert resp_ids == ids

    req_pb = api_pb2.ListNamespacesRequest()
    req_pb.limit = 1
    resp_pb = call(namespace_service.list_namespaces, req_pb, login)
    resp_ids = [n_pb.meta.id for n_pb in resp_pb.namespaces]
    assert resp_ids == ids[:1]

    req_pb = api_pb2.ListNamespacesRequest()
    req_pb.limit = 1
    req_pb.skip = 1
    resp_pb = call(namespace_service.list_namespaces, req_pb, login)
    resp_ids = [n_pb.meta.id for n_pb in resp_pb.namespaces]
    assert resp_ids == ids[1:2]

    req_pb = api_pb2.ListNamespacesRequest()
    req_pb.query.id_in.extend(ids[:2] + ['not_existing'])
    resp_pb = call(namespace_service.list_namespaces, req_pb, login)
    resp_ids = [n_pb.meta.id for n_pb in resp_pb.namespaces]
    assert resp_ids == ids[:2]

    req_pb = api_pb2.ListNamespacesRequest()
    req_pb.query.id_in.extend(ids[:2] + ['not_existing'])
    req_pb.limit = 1
    resp_pb = call(namespace_service.list_namespaces, req_pb, login)
    resp_ids = [n_pb.meta.id for n_pb in resp_pb.namespaces]
    assert resp_ids == ids[:1]

    req_pb = api_pb2.ListNamespacesRequest()
    req_pb.query.category_in.extend(categories[2:] + ['not_existing_category'])
    resp_pb = call(namespace_service.list_namespaces, req_pb, login)
    resp_ids = [n_pb.meta.id for n_pb in resp_pb.namespaces]
    assert resp_ids == ids[2:]

    req_pb = api_pb2.ListNamespacesRequest()
    req_pb.query.id_in.extend(ids[:2] + ['not_existing'])
    req_pb.query.category_in.extend(categories[:1] + ['not_existing_category'])
    resp_pb = call(namespace_service.list_namespaces, req_pb, login)
    resp_ids = [n_pb.meta.id for n_pb in resp_pb.namespaces]
    assert resp_ids == ids[:1]

    req_pb = api_pb2.ListNamespacesRequest()
    req_pb.query.id_in.extend(ids[:1])
    req_pb.query.category_in.extend(categories[1:2])
    resp_pb = call(namespace_service.list_namespaces, req_pb, login)
    resp_ids = [n_pb.meta.id for n_pb in resp_pb.namespaces]
    assert resp_ids == []

    req_pb = api_pb2.ListNamespaceSummariesRequest()
    resp_pb = call(namespace_service.list_namespace_summaries, req_pb, login)
    summaries = [(s_pb.id, s_pb.category) for s_pb in resp_pb.summaries]
    assert summaries == [
        (u'namespace_1', u'cat_1'),
        (u'namespace_2', u'cat_2'),
        (u'namespace_3', u'cat_3')
    ]


def test_get_namespace_alerting_config(zk_storage, cache):
    login = 'someone'
    namespace_id = 'test'

    # case: alerting disabled
    namespace_pb = model_pb2.Namespace()
    namespace_pb.meta.id = namespace_id
    namespace_pb.meta.generation = 1
    namespace_pb.meta.category = 'test'
    cache._set_namespace_pb(namespace_id, namespace_pb)
    req_pb = api_pb2.GetNamespaceAlertingConfigRequest(namespace_id=namespace_id)
    resp_pb = call(namespace_service.get_namespace_alerting_config, req_pb,
                   login)  # type: api_pb2.GetNamespaceAlertingConfigResponse
    assert resp_pb.available_versions
    assert not resp_pb.HasField('current_config')

    # case: alerting enabled
    namespace_pb = model_pb2.Namespace()
    namespace_pb.meta.id = namespace_id
    namespace_pb.meta.generation = 2
    namespace_pb.meta.category = 'test'
    namespace_pb.spec.alerting.version = str(alerting.VERSION_0_0_1)
    cache._set_namespace_pb(namespace_id, namespace_pb)

    req_pb = api_pb2.GetNamespaceAlertingConfigRequest(namespace_id=namespace_id)
    resp_pb = call(namespace_service.get_namespace_alerting_config, req_pb,
                   login)  # type: api_pb2.GetNamespaceAlertingConfigResponse
    assert resp_pb.available_versions
    assert str(alerting.CURRENT_VERSION) in resp_pb.available_versions
    assert str(alerting.VERSION_0_0_1) in resp_pb.available_versions
    assert resp_pb.HasField('current_config')
    assert resp_pb.current_config.alerting_prefix
    assert resp_pb.current_config.juggler_namespace
    assert resp_pb.current_config.wiki_alerts_actions_url
    assert resp_pb.current_config.wiki_alerts_url

    assert resp_pb.current_config.yasm_alerts
    for yasm_alert_pb in resp_pb.current_config.yasm_alerts:
        assert yasm_alert_pb.group
        assert yasm_alert_pb.name
        assert yasm_alert_pb.description
        assert yasm_alert_pb.signal
        assert yasm_alert_pb.crit_threshold
        assert yasm_alert_pb.flaps_stable_time

    assert not resp_pb.current_config.yasm_alerts_panel_url

    balancer_id = 'balancer-id'
    # case: namespace with YP-Lite Powered balancers
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.namespace_id = namespace_id
    balancer_pb.meta.id = balancer_id
    balancer_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    balancer_pb.meta.location.yp_cluster = 'SAS'

    cache._set_balancer_pb(namespace_id + '/' + balancer_id, balancer_pb)
    req_pb = api_pb2.GetNamespaceAlertingConfigRequest(namespace_id=namespace_id)
    resp_pb = call(namespace_service.get_namespace_alerting_config, req_pb,
                   login)  # type: api_pb2.GetNamespaceAlertingConfigResponse
    assert (resp_pb.current_config.yasm_alerts_panel_url == 'https://yasm.yandex-team.ru/template/panel/'
                                                            'awacs-balancers-alerts/'
                                                            'awacs=dev_awacs;namespace=test;locations=sas/')

    # case: namespace with GENCFG Powered balancers
    balancer_pb.meta.location.type = model_pb2.BalancerMeta.Location.GENCFG_DC
    balancer_pb.meta.location.gencfg_dc = 'MAN'

    cache._set_balancer_pb(namespace_id + '/' + balancer_id, balancer_pb)
    req_pb = api_pb2.GetNamespaceAlertingConfigRequest(namespace_id=namespace_id)
    resp_pb = call(namespace_service.get_namespace_alerting_config, req_pb,
                   login)  # type: api_pb2.GetNamespaceAlertingConfigResponse
    assert (resp_pb.current_config.yasm_alerts_panel_url == 'https://yasm.yandex-team.ru/template/panel/'
                                                            'awacs-balancers-alerts/'
                                                            'awacs=dev_awacs;namespace=test;locations=man/')


def test_namespace_annotations(zk_storage, cache):
    login_1 = 'robot-someone'
    login_2 = 'robot-another'
    login_3 = 'guest'
    namespace_id_1 = 'test1'
    namespace_id_2 = 'test2'
    namespace_id_3 = 'test3'

    config.set_value('namespace_annotations.robot_projects',
                     {'robot-someone': 'PROJECT1', 'robot-another': 'PROJECT2'})

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id_1
    req_pb.meta.generation = 1
    req_pb.meta.category = 'test'
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.staff.owners.logins.extend([login_1, login_2])
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF

    resp_pb = call(namespace_service.create_namespace, req_pb, login_1)
    namespace_pb = resp_pb.namespace

    assert namespace_pb.meta.id == namespace_id_1
    assert namespace_pb.meta.category == 'test'
    assert namespace_pb.meta.annotations['project'] == 'PROJECT1'
    assert namespace_pb.meta.annotations['creator'] == login_1

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.id = namespace_id_1
    req_pb.meta.category = 'test'
    req_pb.meta.auth.staff.owners.logins.extend([login_2])
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF

    resp_pb = call(namespace_service.update_namespace, req_pb, login_2, enable_auth=True)
    namespace_pb = resp_pb.namespace
    assert namespace_pb.meta.id == namespace_id_1
    assert namespace_pb.meta.category == 'test'
    assert namespace_pb.meta.annotations['project'] == 'PROJECT1'
    assert namespace_pb.meta.annotations['creator'] == login_1

    req_pb.meta.annotations.update(namespace_pb.meta.annotations)
    req_pb.meta.annotations['annotation1'] = 'value'
    resp_pb = call(namespace_service.update_namespace, req_pb, login_2, enable_auth=True)
    namespace_pb = resp_pb.namespace
    assert namespace_pb.meta.annotations['project'] == 'PROJECT1'
    assert namespace_pb.meta.annotations['creator'] == login_1
    assert namespace_pb.meta.annotations['annotation1'] == 'value'

    req_pb.meta.annotations['project'] = 'value'
    with pytest.raises(exceptions.ForbiddenError):
        call(namespace_service.update_namespace, req_pb, login_2, enable_auth=True)

    del req_pb.meta.annotations['annotation1']
    resp_pb = call(namespace_service.update_namespace, req_pb, login_2, enable_auth=False)
    namespace_pb = resp_pb.namespace
    assert 'annotation1' not in namespace_pb.meta.annotations
    assert namespace_pb.meta.annotations['project'] == 'value'
    assert namespace_pb.meta.annotations['creator'] == login_1

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id_2
    req_pb.meta.generation = 1
    req_pb.meta.category = 'test'
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])

    resp_pb = call(namespace_service.create_namespace, req_pb, login_3)
    namespace_pb = resp_pb.namespace

    assert namespace_pb.meta.id == namespace_id_2
    assert namespace_pb.meta.category == 'test'
    assert namespace_pb.meta.annotations['project'] == 'unknown'
    assert namespace_pb.meta.annotations['creator'] == login_3

    config.set_value('namespace_annotations.category_projects', {'maps_front': 'maps'})

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id_3
    req_pb.meta.generation = 1
    req_pb.meta.category = 'maps_front'
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.staff.owners.logins.extend([login_3])
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF

    resp_pb = call(namespace_service.create_namespace, req_pb, login_3)
    namespace_pb = resp_pb.namespace

    assert namespace_pb.meta.id == namespace_id_3
    assert namespace_pb.meta.category == 'maps_front'
    assert namespace_pb.meta.annotations['project'] == 'maps'
    assert namespace_pb.meta.annotations['creator'] == login_3


def test_apply_namespace_alerting_preset(cache, zk_storage, ctx):
    login = 'romanovich'
    unprivileged_login = 'someone'
    namespace_id = 'namespace_alerting'
    namespace_cat = 'users/romanovich'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend((login, unprivileged_login))
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])

    resp_pb = call(namespace_service.create_namespace, req_pb, login)

    processors.Finalizing(processors.NamespaceOrder(resp_pb.namespace)).process(ctx)
    make_unknown_layout(namespace_id, LOGIN)

    req_pb = api_pb2.GetNamespaceRequest(id=namespace_id, consistency=api_pb2.STRONG)
    resp_pb = call(namespace_service.get_namespace, req_pb, login)

    balancer_id = 'balancer-id'
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.namespace_id = namespace_id
    balancer_pb.meta.id = balancer_id
    balancer_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    balancer_pb.meta.location.yp_cluster = 'SAS'
    balancer_pb.spec.config_transport.nanny_static_file.instance_tags.itype = 'test'
    balancer_pb.spec.config_transport.nanny_static_file.instance_tags.ctype = 'balancer'
    cache._set_balancer_pb(namespace_id + '/' + balancer_id, balancer_pb)

    alerting_req_pb = api_pb2.ApplyNamespaceAlertingPresetRequest()
    alerting_req_pb.namespace_id = namespace_id
    alerting_req_pb.version = resp_pb.namespace.meta.version
    alerting_req_pb.preset = model_pb2.NamespaceSpec.PR_WITHOUT_NOTIFICATIONS

    with pytest.raises(exceptions.ForbiddenError):
        call(namespace_service.apply_namespace_alerting_preset, alerting_req_pb, unprivileged_login, enable_auth=True)

    with pytest.raises(exceptions.BadRequestError):
        call(namespace_service.apply_namespace_alerting_preset, alerting_req_pb, login)

    req_pb = api_pb2.UpdateNamespaceRequest()
    req_pb.meta.CopyFrom(resp_pb.namespace.meta)
    req_pb.meta.annotations['project'] = 'taxi'
    req_pb.spec.CopyFrom(resp_pb.namespace.spec)
    req_pb.spec.incomplete = True  # just to trigger version change
    resp_pb = call(namespace_service.update_namespace, req_pb, login)

    with pytest.raises(exceptions.ConflictError):
        call(namespace_service.apply_namespace_alerting_preset, alerting_req_pb, login)

    alerting_req_pb.version = resp_pb.namespace.meta.version
    resp_pb = call(namespace_service.apply_namespace_alerting_preset, alerting_req_pb, login)

    assert resp_pb.namespace.spec.preset == model_pb2.NamespaceSpec.PR_WITHOUT_NOTIFICATIONS
    assert resp_pb.namespace.spec.alerting.WhichOneof('notify_rules') == 'notify_rules_disabled'
    assert resp_pb.namespace.spec.alerting.notify_rules_disabled
    assert not resp_pb.namespace.spec.alerting.balancer_checks_disabled
    assert resp_pb.namespace.meta.version != alerting_req_pb.version


def test_namespace_enable_alerting_no_l7(cache, zk_storage, ctx):
    login = 'romanovich'
    unprivileged_login = 'someone'
    namespace_id = 'namespace_alerting'
    namespace_cat = 'users/romanovich'

    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = namespace_id
    req_pb.meta.category = namespace_cat
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend((login, unprivileged_login))
    req_pb.meta.auth.staff.owners.group_ids.extend([GROUP])

    resp_pb = call(namespace_service.create_namespace, req_pb, login)

    processors.Finalizing(processors.NamespaceOrder(resp_pb.namespace)).process(ctx)
    make_unknown_layout(namespace_id, LOGIN)

    req_pb = api_pb2.GetNamespaceRequest(id=namespace_id, consistency=api_pb2.STRONG)
    resp_pb = call(namespace_service.get_namespace, req_pb, login)

    ns_pb = resp_pb.namespace

    update_ns_request_pb = api_pb2.UpdateNamespaceRequest()
    update_ns_request_pb.meta.CopyFrom(ns_pb.meta)
    update_ns_request_pb.spec.CopyFrom(ns_pb.spec)
    update_ns_request_pb.spec.alerting.version = str(alerting.CURRENT_VERSION)
    recipients_text = "    - '{}'".format(login)

    crit_juggler_nr_pb = update_ns_request_pb.spec.alerting.juggler_raw_notify_rules.balancer.add()
    crit_juggler_nr_pb.template_name = 'on_status_change'
    crit_juggler_nr_pb.template_kwargs = DEFAULT_CRIT_BALANCER_NOTIFY_RULE_TPL.format(recipients_text)

    warn_juggler_nr_pb = update_ns_request_pb.spec.alerting.juggler_raw_notify_rules.balancer.add()
    warn_juggler_nr_pb.template_name = 'on_status_change'
    warn_juggler_nr_pb.template_kwargs = DEFAULT_WARN_BALANCER_NOTIFY_RULE_KWARGS_TPL.format(recipients_text)

    call(namespace_service.update_namespace, update_ns_request_pb, login)


@pytest.mark.parametrize(u'operation', ('import_virtual_servers_from_l3mgr', 'add_ip_address_to_l3_balancer'))
def test_ns_operations_meta(zk_storage, checker, operation):
    l3_id = u'l3_balancer'
    req_pb = api_pb2.CreateNamespaceOperationRequest()
    req_pb.meta.namespace_id = NS_ID
    field_pb = getattr(req_pb.order, operation)
    field_pb.l3_balancer_id = l3_id
    with awtest.raises(exceptions.NotFoundError, text=u'Namespace "namespace_id" not found'):
        call(namespace_service.create_namespace_operation, req_pb, LOGIN)

    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    zk_storage.create_namespace(NS_ID, ns_pb)

    for check in checker:
        with check:
            with awtest.raises(exceptions.BadRequestError,
                               text=u'"req_pb.meta.parent_versions.l3_versions": must contain the '
                                    u'latest revision of L3 balancer "l3_balancer"'):
                call(namespace_service.create_namespace_operation, req_pb, LOGIN)

    req_pb.meta.parent_versions.l3_versions[l3_id] = u'xxx'
    for check in checker:
        with check:
            with awtest.raises(exceptions.NotFoundError,
                               text=u'L3 balancer "namespace_id:l3_balancer" does not exist'):
                call(namespace_service.create_namespace_operation, req_pb, LOGIN)

    l3_pb = model_pb2.L3Balancer()
    l3_pb.meta.id = l3_id
    l3_pb.meta.namespace_id = NS_ID
    l3_pb.meta.version = u'yyy'
    zk_storage.create_l3_balancer(NS_ID, l3_id, l3_pb)
    for check in checker:
        with check:
            with awtest.raises(exceptions.ConflictError,
                               text=u'"req_pb.meta.parent_versions.l3_versions[l3_balancer]": '
                                    u'latest revision is "yyy", not "xxx"'):
                call(namespace_service.create_namespace_operation, req_pb, LOGIN)

    req_pb.meta.parent_versions.l3_versions[u'whoops'] = u'xxx'
    field_pb.l3_balancer_id = u'whoops'
    for check in checker:
        with check:
            with awtest.raises(exceptions.NotFoundError,
                               text=u'L3 balancer "namespace_id:whoops" does not exist'):
                call(namespace_service.create_namespace_operation, req_pb, LOGIN)


@pytest.mark.parametrize(u'operation', ('import_virtual_servers_from_l3mgr', 'add_ip_address_to_l3_balancer'))
def test_ns_operations_locking(ctx, zk_storage, checker, operation):
    l3_id_1 = u'l3_balancer_1'
    l3_id_2 = u'l3_balancer_2'
    ns_id_2 = NS_ID + u'1'

    ctx.log.info(u'Step 0: create 2 L3 balancers in each of 2 namespaces')
    for ns_id in (NS_ID, ns_id_2):
        ns_pb = model_pb2.Namespace()
        ns_pb.meta.id = ns_id
        zk_storage.create_namespace(ns_id, ns_pb)
        for l3_id in (l3_id_1, l3_id_2):
            l3_pb = model_pb2.L3Balancer()
            l3_pb.meta.id = l3_id
            l3_pb.meta.namespace_id = ns_id
            l3_pb.meta.version = u'yyy'
            zk_storage.create_l3_balancer(ns_id, l3_id, l3_pb)

    ctx.log.info(u'Step 1: create NS op for one of L3 balancers in first NS')
    req_pb = api_pb2.CreateNamespaceOperationRequest()
    req_pb.meta.namespace_id = NS_ID
    field_pb = getattr(req_pb.order, operation)
    field_pb.l3_balancer_id = l3_id
    req_pb.meta.parent_versions.l3_versions[l3_id] = u'yyy'
    op_pb = call(namespace_service.create_namespace_operation, req_pb, LOGIN).operation

    ctx.log.info(u'Step 2: check that we cannot create second NS op in the same NS')
    with awtest.raises(exceptions.ConflictError,
                       text=u'Another namespace operation is already processing same objects, '
                            u'please wait until it finishes'):
        call(namespace_service.create_namespace_operation, req_pb, LOGIN)

    ctx.log.info(u'Step 3: check that we can create a new NS op in the second NS')
    req_pb.meta.namespace_id = ns_id_2
    call(namespace_service.create_namespace_operation, req_pb, LOGIN)

    ctx.log.info(u'Step 4: remove NS op in the first NS and check that we can create another op')
    objects.NamespaceOperation.remove(op_pb)
    req_pb.meta.namespace_id = NS_ID
    call(namespace_service.create_namespace_operation, req_pb, LOGIN)
