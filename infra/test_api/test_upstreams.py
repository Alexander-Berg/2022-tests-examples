# coding: utf-8
import copy

import inject
import mock
import pytest
import six
import yaml
from sepelib.core import config as appconfig

import awtest
from . import test_balancers
from awacs.lib.rpc import exceptions
from awacs.lib.strutils import flatten_full_id2
from awacs.model import validation, util
from awacs.model.errors import ValidationError
from awacs.model.validation import _validate_upstream_config
from awacs.model.balancer.ctl import BalancerCtl as BaseBalancerCtl
from awacs.web import namespace_service, upstream_service, balancer_service
from awacs.wrappers import rps_limiter_settings
from awacs.wrappers.base import Holder, ANY_MODULE
from awacs.wrappers.l7upstreammacro import L7UpstreamMacro
from awtest.api import call, create_namespace_with_order_in_progress, fill_object_upper_limits, \
    create_too_many_lines_yaml_config, create_too_many_chars_yaml_config, set_login_to_root_users, Api
from awtest.core import wait_until, wait_until_passes
from infra.awacs.proto import api_pb2, model_pb2, modules_pb2


NS_ID = 'bbb_sas'
BALANCER_ID = NS_ID
BACKEND_ID = NS_ID
LOGIN = 'login'


class BalancerCtl(BaseBalancerCtl):
    TRANSPORT_PROCESSING_INTERVAL = 0.01
    TRANSPORT_POLLING_INTERVAL = 0.01
    TRANSPORT_MAIN_LOOP_FREQ = 0.01

    PROCESS_INTERVAL = 9999
    FORCE_PROCESS_INTERVAL = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    SLEEP_AFTER_EXCEPTION_TIMEOUT = 0.01

    def _process_empty_queue(self, *_, **__):
        pass


@pytest.fixture
def disable_linter():
    appconfig.set_value('run.enable_linter', False)
    yield
    appconfig.set_value('run.enable_linter', True)


def create_balancer(cache, namespace_id, b_id):
    appconfig.set_value('run.root_users', [util.NANNY_ROBOT_LOGIN])
    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = b_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.category = 'users/romanovich'
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.config_transport.nanny_static_file.service_id = 'prod_balancer'
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = test_balancers.YAML_CONFIG_1
    call(balancer_service.create_balancer, req_pb, util.NANNY_ROBOT_LOGIN)
    return wait_until_passes(lambda: cache.must_get_balancer(namespace_id, b_id))


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client):
    inject.clear_and_configure(binder_with_nanny_client)
    yield
    inject.clear()


_YAML_CONFIG_1 = {
    'errorlog': {
        'log_level': 'DEBUG',
        'log': '/tmp/log.txt',
        'errordocument': {'status': 200}
    },
}
YAML_CONFIG_1 = yaml.dump(_YAML_CONFIG_1)

_YAML_CONFIG_2 = copy.deepcopy(_YAML_CONFIG_1)
_YAML_CONFIG_2['errorlog']['log_level'] = 'ERROR'
YAML_CONFIG_2 = yaml.dump(_YAML_CONFIG_2)

_INVALID_YAML_CONFIG_1 = {
    'regexp': {
        'include_upstreams': {
            'type': 'ALL',
        },
    },
}
INVALID_YAML_CONFIG_1 = yaml.dump(_INVALID_YAML_CONFIG_1)

_YAML_L7_UPSTREAM_MACRO = '''
l7_upstream_macro:
  version: 0.0.1
  id: upstream_two
  matcher:
    any: true
  static_response:
    status: 421
    content: Bad Request
'''

_YAML_CONFIG_W_UNICODE = '''
l7_upstream_macro:
  version: 0.0.1
  id: upstream_two
  matcher:
    any: true
  static_response:
    status: 421
    content: Плохой запрос
'''

_YAML_CONFIG_W_UNICODE_COMMENT = '''
l7_upstream_macro:
  version: 0.0.1
  id: upstream_two
  matcher:
    any: true
  static_response:
    status: 421
    content: Bad Request # Коммент с юникодом
'''

YAML_L7_UPSTREAM_MACRO_WITH_BACKEND = '''
l7_upstream_macro:
  version: 0.0.1
  id: upstream_two
  matcher:
    any: true
  flat_scheme:
    balancer:
      attempts: 2
      backend_timeout: 20s
      do_not_retry_http_responses: true
      do_not_limit_reattempts: true
      max_pessimized_endpoints_share: 0.5
    backend_ids:
    - {}
    on_error:
      rst: true
'''.format(BACKEND_ID)


def test_forbidden_operations_during_namespace_order(zk_storage, cache, enable_auth):
    # forbid creation and removal
    create_namespace_with_order_in_progress(zk_storage, cache, NS_ID, LOGIN)

    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = NS_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_1
    req_pb.meta.comment = 'c'
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(upstream_service.create_upstream, req_pb, LOGIN)

    b_pb = model_pb2.Upstream(spec=req_pb.spec, meta=req_pb.meta)
    b_pb.meta.version = 'xxx'
    zk_storage.create_upstream(NS_ID, NS_ID, b_pb)

    req_pb = api_pb2.RemoveUpstreamRequest(namespace_id=NS_ID, id=NS_ID, version=b_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(upstream_service.remove_upstream, req_pb, LOGIN)


@pytest.mark.parametrize('max_count,custom_count', [
    (0, None),
    (1, None),
    (10, None),
    (5, 10),
    (10, 5),
])
def test_namespace_objects_total_limit(max_count, custom_count, cache, create_default_namespace):
    create_default_namespace(NS_ID)
    appconfig.set_value('common_objects_limits.upstream', max_count)
    if custom_count is not None:
        fill_object_upper_limits(NS_ID, 'upstream', custom_count, LOGIN)
    count = custom_count or max_count

    create_balancer(cache, NS_ID, NS_ID)

    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = NS_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_1
    req_pb.meta.comment = 'c'
    for _ in range(count):
        call(upstream_service.create_upstream, req_pb, LOGIN)
        req_pb.meta.id += 'a'

    def check():
        list_req_pb = api_pb2.ListUpstreamsRequest(namespace_id=NS_ID)
        assert call(upstream_service.list_upstreams, list_req_pb, LOGIN).total == count

    wait_until_passes(check)

    with pytest.raises(exceptions.BadRequestError,
                       match='Exceeded limit of upstreams in the namespace: {}'.format(count)):
        call(upstream_service.create_upstream, req_pb, LOGIN)


def test_validate_func():
    upstream_id = 'default'

    # first case
    pb = modules_pb2.Holder()
    pb.regexp_section.nested.accesslog.log = './log'

    holder = Holder(pb)
    with mock.patch.object(holder.module, 'validate') as m:
        _validate_upstream_config(upstream_id=upstream_id, config=holder)
    m.assert_called_once_with(key=upstream_id, ctx=mock.ANY, preceding_modules=[ANY_MODULE])

    # second case
    pb = modules_pb2.Holder()
    m = pb.modules.add()
    m.regexp_section.nested.accesslog.log = './log'

    holder = Holder(pb)
    with mock.patch.object(holder.chain.modules[0].module, 'validate') as m:
        _validate_upstream_config(upstream_id=upstream_id, config=holder)
    m.assert_called_once_with(key=upstream_id, ctx=mock.ANY, preceding_modules=[ANY_MODULE])

    # third case
    pb = modules_pb2.Holder()
    m = pb.modules.add()
    m.accesslog.log = './log'
    m = pb.modules.add()
    m.regexp_section.matcher.SetInParent()

    holder = Holder(pb)
    fst = holder.chain.modules[0].module
    snd = holder.chain.modules[1].module
    with mock.patch.object(snd, 'validate') as m:
        _validate_upstream_config(upstream_id=upstream_id, config=holder)
    m.assert_called_once_with(ctx=mock.ANY, chained_modules=(), preceding_modules=[ANY_MODULE, fst, snd])


def test_create_upstream_w_unicode(mongo_storage, cache, create_default_namespace):
    comment = 'Creating very important upstream'

    create_default_namespace(NS_ID)
    create_balancer(cache, NS_ID, NS_ID)
    upstream_id = 'upstream_two'

    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = upstream_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerUpstreamSpec.EASY_MODE2

    req_pb.spec.yandex_balancer.yaml = _YAML_CONFIG_W_UNICODE
    req_pb.meta.comment = comment
    with pytest.raises(exceptions.BadRequestError) as e:
        call(upstream_service.create_upstream, req_pb, LOGIN)
    assert six.text_type(e.value) == 'spec.yandex_balancer.yaml: line 9, column 14: Non-ascii characters present'

    req_pb.spec.yandex_balancer.yaml = _YAML_L7_UPSTREAM_MACRO
    response = call(upstream_service.create_upstream, req_pb, LOGIN)

    def check_0():
        request = api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id=upstream_id)
        resp_pb = call(upstream_service.get_upstream, request, LOGIN)
        u = resp_pb.upstream
        assert u.meta.comment == comment

    wait_until_passes(check_0)

    u_pb = response.upstream
    req_pb = api_pb2.UpdateUpstreamRequest()
    req_pb.meta.id = upstream_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.comment = comment
    initial_version = u_pb.meta.version
    req_pb.meta.version = initial_version
    req_pb.spec.SetInParent()
    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerUpstreamSpec.EASY_MODE2
    req_pb.spec.yandex_balancer.yaml = _YAML_CONFIG_W_UNICODE

    with pytest.raises(exceptions.BadRequestError) as e:
        call(upstream_service.update_upstream, req_pb, u'very-root-user')
    assert six.text_type(e.value) == 'spec.yandex_balancer.yaml: line 9, column 14: Non-ascii characters present'

    req_pb.spec.yandex_balancer.yaml = _YAML_CONFIG_W_UNICODE_COMMENT
    call(upstream_service.update_upstream, req_pb, u'very-root-user')

    def check_upstream():
        req_pb = api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id=upstream_id)
        resp_pb = call(upstream_service.get_upstream, req_pb, LOGIN)
        u_pb = resp_pb.upstream
        assert u_pb.meta.version != initial_version
        assert u_pb.meta.comment == comment
        assert mongo_storage.list_upstream_revs(namespace_id=NS_ID, upstream_id=upstream_id).total == 2

        assert len(u_pb.statuses) == 1
        assert [r.id for r in u_pb.statuses] == [u_pb.meta.version]
        r_status = u_pb.statuses[-1]
        assert r_status.id == u_pb.meta.version
        assert not r_status.validated and not r_status.in_progress and not r_status.active

        rev = mongo_storage.must_get_upstream_rev(r_status.id)
        assert rev.meta.id == r_status.id
        assert rev.meta.comment == comment
        assert rev.spec == u_pb.spec

    wait_until_passes(check_upstream)


def test_create_get_delete_upstream(mongo_storage, cache, create_default_namespace):
    comment = 'Creating very important upstream'

    create_default_namespace(NS_ID)
    create_balancer(cache, NS_ID, NS_ID)

    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = 'upstream.with.dots'
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = yaml.dump({})

    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerUpstreamSpec.EASY_MODE2
    with pytest.raises(exceptions.BadRequestError, match="\"meta.id\": easy-mode upstream id cannot contain '.'"):
        call(upstream_service.update_upstream, req_pb, LOGIN)

    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerUpstreamSpec.FULL_MODE
    req_pb.meta.id = 'maps'
    with pytest.raises(exceptions.BadRequestError) as e:
        with mock.patch.object(validation, 'validate_upstream_config',
                               side_effect=ValidationError('BAD')):
            call(upstream_service.create_upstream, req_pb, LOGIN)
    assert six.text_type(e.value) == 'BAD'

    # test SWAT-5260 regression
    req_pb.spec.ClearField('yandex_balancer')
    req_pb.spec.yandex_balancer.yaml = INVALID_YAML_CONFIG_1
    with pytest.raises(exceptions.BadRequestError) as e:
        call(upstream_service.create_upstream, req_pb, LOGIN)
    e.match('"spec.yandex_balancer.yaml": regexp -> include_upstreams: is not allowed in upstream config')

    req_pb.spec.yandex_balancer.yaml = create_too_many_lines_yaml_config()
    with pytest.raises(exceptions.BadRequestError) as e:
        call(upstream_service.create_upstream, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.yandex_balancer.yaml" contains too many lines '
                                      u'(1502, allowed limit is 1500)')

    req_pb.spec.yandex_balancer.yaml = create_too_many_chars_yaml_config()
    with pytest.raises(exceptions.BadRequestError) as e:
        call(upstream_service.create_upstream, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.yandex_balancer.yaml" contains too many characters '
                                      u'(80075, allowed limit is 80000)')

    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_1
    req_pb.meta.comment = comment
    response = call(upstream_service.create_upstream, req_pb, LOGIN)

    u_pb = response.upstream
    assert u_pb.meta.id == 'maps'
    assert u_pb.meta.namespace_id == NS_ID
    assert u_pb.meta.author == LOGIN
    assert u_pb.meta.comment == comment

    wait_until_passes(lambda: cache.must_get_upstream(NS_ID, 'maps').meta.comment == comment)

    req_pb = api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id='maps')
    u_pb = call(upstream_service.get_upstream, req_pb, LOGIN).upstream
    assert u_pb.meta.comment == comment
    assert u_pb.spec.HasField('yandex_balancer')
    assert u_pb.spec.yandex_balancer.HasField('config')
    errorlog = u_pb.spec.yandex_balancer.config.errorlog
    assert errorlog.log_level == 'DEBUG'
    assert errorlog.log == '/tmp/log.txt'

    req_pb = api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id='maps', consistency=api_pb2.STRONG)
    response = call(upstream_service.get_upstream, req_pb, LOGIN)
    u_2 = response.upstream
    assert u_pb == u_2

    assert len(u_pb.statuses) == 1
    r_status = u_pb.statuses[-1]
    assert r_status.id == u_pb.meta.version
    assert not r_status.validated and not r_status.in_progress and not r_status.active

    rev = mongo_storage.must_get_upstream_rev(r_status.id)
    assert rev.meta.id == r_status.id
    assert rev.spec == u_pb.spec

    req_pb = api_pb2.RemoveUpstreamRequest(namespace_id=NS_ID, id='maps', version=r_status.id)
    call(upstream_service.remove_upstream, req_pb, LOGIN)

    wait_until(lambda: cache.get_upstream(NS_ID, 'maps').spec.deleted)

    req_pb = api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id='maps')
    u_pb = call(upstream_service.get_upstream, req_pb, LOGIN).upstream
    assert u_pb.spec.deleted
    assert u_pb.meta.comment == 'Marked as deleted by {}'.format(LOGIN)


def test_update_upstream(mongo_storage, cache, create_default_namespace):
    comment = 'Updating very important upstream'

    create_default_namespace(NS_ID)
    create_balancer(cache, NS_ID, NS_ID)

    upstream_id = 'maps'
    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = upstream_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_1

    with mock.patch.object(validation, 'validate_upstream_config'):
        resp_pb = call(upstream_service.create_upstream, req_pb, LOGIN)
    u_pb = resp_pb.upstream
    assert u_pb.meta.id == 'maps'
    assert u_pb.meta.namespace_id == NS_ID
    initial_version = u_pb.meta.version
    assert mongo_storage.list_upstream_revs(namespace_id=NS_ID, upstream_id=upstream_id).total == 1

    req_pb = api_pb2.UpdateUpstreamRequest()
    req_pb.meta.id = 'maps'
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = 'xxx'
    req_pb.spec.SetInParent()

    with pytest.raises(exceptions.BadRequestError) as e:
        call(upstream_service.update_upstream, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.type" is set to "YANDEX_BALANCER", '
                                      u'but "spec.yandex_balancer" field is missing')

    req_pb.spec.CopyFrom(u_pb.spec)
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_2
    with pytest.raises(exceptions.ConflictError) as e:
        with mock.patch.object(validation, 'validate_upstream_config'):
            call(upstream_service.update_upstream, req_pb, LOGIN)
    e.match('Upstream modification conflict')
    assert mongo_storage.list_upstream_revs(namespace_id=NS_ID, upstream_id=upstream_id).total == 1

    req_pb.meta.version = u_pb.meta.version
    req_pb.meta.comment = comment

    req_pb.spec.yandex_balancer.yaml = create_too_many_lines_yaml_config()
    with pytest.raises(exceptions.BadRequestError) as e:
        call(upstream_service.update_upstream, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.yandex_balancer.yaml" contains too many lines '
                                      u'(1502, allowed limit is 1500)')

    req_pb.spec.yandex_balancer.yaml = create_too_many_chars_yaml_config()
    with pytest.raises(exceptions.BadRequestError) as e:
        call(upstream_service.update_upstream, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.yandex_balancer.yaml" contains too many characters '
                                      u'(80075, allowed limit is 80000)')

    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_2

    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerUpstreamSpec.EASY_MODE2
    req_pb.meta.id = 'upstream.with.dots'
    with pytest.raises(exceptions.BadRequestError, match="\"meta.id\": easy-mode upstream id cannot contain '.'"):
        call(upstream_service.update_upstream, req_pb, LOGIN)

    req_pb.meta.id = 'maps'
    with mock.patch.object(validation, 'validate_upstream_config') as m:
        m.return_value.module_name = u'l7_upstream_macro'
        m.return_value.module = L7UpstreamMacro(modules_pb2.L7UpstreamMacro(id=req_pb.meta.id))
        call(upstream_service.update_upstream, req_pb, u'very-root-user')

    def check_upstream():
        req_pb = api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id='maps')
        resp_pb = call(upstream_service.get_upstream, req_pb, LOGIN)
        u_pb = resp_pb.upstream
        assert u_pb.spec.yandex_balancer.config.errorlog.log_level == 'ERROR'
        assert u_pb.meta.version != initial_version
        assert u_pb.meta.comment == comment
        assert mongo_storage.list_upstream_revs(namespace_id=NS_ID, upstream_id=upstream_id).total == 2

        assert len(u_pb.statuses) == 1
        assert [r.id for r in u_pb.statuses] == [u_pb.meta.version]
        r_status = u_pb.statuses[-1]
        assert r_status.id == u_pb.meta.version
        assert not r_status.validated and not r_status.in_progress and not r_status.active

        rev_pb = mongo_storage.must_get_upstream_rev(r_status.id)
        assert rev_pb.meta.id == r_status.id
        assert rev_pb.meta.comment == comment
        assert rev_pb.spec == u_pb.spec

    wait_until_passes(check_upstream)


def test_update_upstream_indices(zk_storage, mongo_storage, cache, create_default_namespace, ctx, ctlrunner):
    create_default_namespace(NS_ID)

    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.MANUAL
    Api.create_backend(namespace_id=NS_ID, backend_id=BACKEND_ID, spec_pb=spec_pb)

    bal_pb = create_balancer(cache, NS_ID, BALANCER_ID)
    assert len(bal_pb.meta.indices) == 1
    assert len(bal_pb.meta.indices[0].included_backend_ids) == 0

    upstream_id = 'upstream_two'
    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = upstream_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = req_pb.spec.yandex_balancer.EASY_MODE2
    req_pb.spec.yandex_balancer.yaml = _YAML_L7_UPSTREAM_MACRO

    u_pb = call(upstream_service.create_upstream, req_pb, LOGIN).upstream
    assert len(u_pb.meta.indices) == 1
    assert len(u_pb.meta.indices[0].included_backend_ids) == 0

    req_pb = api_pb2.UpdateUpstreamRequest()
    req_pb.meta.CopyFrom(u_pb.meta)
    req_pb.spec.CopyFrom(u_pb.spec)
    req_pb.spec.yandex_balancer.mode = req_pb.spec.yandex_balancer.EASY_MODE2
    req_pb.spec.yandex_balancer.yaml = YAML_L7_UPSTREAM_MACRO_WITH_BACKEND
    req_pb.spec.yandex_balancer.ClearField('config')
    up_pb = call(upstream_service.update_upstream, req_pb, LOGIN).upstream
    assert len(up_pb.meta.indices) == 2

    upstream_id = u_pb.meta.id

    last_rev_ind = None
    for ind_pb in up_pb.meta.indices:
        if ind_pb.id == up_pb.meta.version:
            last_rev_ind = ind_pb
            break
    assert last_rev_ind is not None
    assert list(last_rev_ind.included_backend_ids) == [flatten_full_id2((NS_ID, BACKEND_ID))]

    set_login_to_root_users(LOGIN)
    req_pb = api_pb2.UpdateUpstreamIndicesRequest()
    with awtest.raises(exceptions.BadRequestError, text_contains=u'No "id" specified.'):
        call(balancer_service.update_balancer_indices, req_pb, LOGIN)

    req_pb.id = upstream_id
    with awtest.raises(exceptions.BadRequestError, text_contains=u'No "namespace_id" specified.'):
        call(balancer_service.update_balancer_indices, req_pb, LOGIN)

    with pytest.raises(exceptions.InternalError, match=u'No upstream revisions present in state'):
        req_pb.namespace_id = NS_ID
        call(upstream_service.update_upstream_indices, req_pb, LOGIN)

    expected_indices = [up_pb.meta.indices[-1]]
    for up_pb in zk_storage.update_upstream(NS_ID, upstream_id):
        del up_pb.meta.indices[:]

    def check():
        up_pb = call(upstream_service.get_upstream,
                     api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id=upstream_id)).upstream
        assert len(up_pb.meta.indices) == 0
    wait_until_passes(check)

    balancer_ctl = BalancerCtl(NS_ID, BALANCER_ID)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._force_process(ctx)

    call(upstream_service.update_upstream_indices, req_pb, LOGIN)

    def check():
        req_pb = api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id=upstream_id)
        up_pb = call(upstream_service.get_upstream, req_pb).upstream
        assert list(up_pb.meta.indices) == expected_indices
    wait_until_passes(check)


def test_upstream_full_to_easy_mode_conversion(create_default_namespace):
    login = u'non-root'
    ns_id = u'ns-id'
    u_id = u'u-id'
    create_default_namespace(ns_id)

    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = u_id
    req_pb.meta.namespace_id = ns_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_1
    with mock.patch.object(validation, 'validate_upstream_config'):
        resp_pb = call(upstream_service.create_upstream, req_pb, login)

    req_pb = api_pb2.UpdateUpstreamRequest()
    req_pb.meta.CopyFrom(resp_pb.upstream.meta)
    req_pb.spec.yandex_balancer.mode = req_pb.spec.yandex_balancer.EASY_MODE2
    req_pb.spec.yandex_balancer.yaml = u'''
    l7_upstream_macro:
      version: 0.0.1
      id: {}
      matcher:
        any: true
      static_response:
        status: 421
        content: Bad Request'''.format(u_id)
    call(upstream_service.update_upstream, req_pb, login)

    def check_upstream():
        req_pb = api_pb2.GetUpstreamRequest(namespace_id=ns_id, id=u_id)
        resp_pb = call(upstream_service.get_upstream, req_pb, LOGIN)
        u_pb = resp_pb.upstream
        assert u_pb.spec.yandex_balancer.mode == u_pb.spec.yandex_balancer.EASY_MODE2

    wait_until_passes(check_upstream)


@mock.patch.object(validation, 'validate_upstream_config')
def test_list_upstreams_and_revisions(_1, cache, create_default_namespace):
    create_default_namespace(NS_ID)
    create_balancer(cache, NS_ID, NS_ID)

    ids = ['aaa', 'bbb', 'ccc', 'ddd']
    order_labels = {
        'bbb': '1',
        'aaa': '2',
        'ddd': '3',
        'ccc': '4',
    }
    upstreams = {}
    for id in ids:
        req_pb = api_pb2.CreateUpstreamRequest()
        req_pb.meta.id = id
        req_pb.meta.namespace_id = NS_ID
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.spec.type = model_pb2.YANDEX_BALANCER
        req_pb.spec.yandex_balancer.yaml = '{}'
        req_pb.spec.labels['order'] = order_labels[id]
        upstreams[id] = call(upstream_service.create_upstream, req_pb, LOGIN).upstream

    req_pb = api_pb2.ListUpstreamsRequest(namespace_id=NS_ID)

    def check_list():
        resp_pb = call(upstream_service.list_upstreams, req_pb, LOGIN)
        assert len(resp_pb.upstreams) == 4
        assert [b.meta.id for b in resp_pb.upstreams] == ids

    wait_until_passes(check_list)

    req_pb.skip = 1
    resp_pb = call(upstream_service.list_upstreams, req_pb, LOGIN)
    assert len(resp_pb.upstreams) == 3
    assert [b.meta.id for b in resp_pb.upstreams] == ids[1:]

    req_pb.skip = 1
    req_pb.limit = 2
    resp_pb = call(upstream_service.list_upstreams, req_pb, LOGIN)
    assert len(resp_pb.upstreams) == 2
    assert [b.meta.id for b in resp_pb.upstreams] == ids[1:3]

    req_pb.skip = 100
    req_pb.limit = 0
    resp_pb = call(upstream_service.list_upstreams, req_pb, LOGIN)
    assert len(resp_pb.upstreams) == 0

    req_pb = api_pb2.ListUpstreamsRequest(namespace_id=NS_ID)
    req_pb.sort_target = api_pb2.ListUpstreamsRequest.ORDER_LABEL
    req_pb.sort_order = api_pb2.ASCEND
    resp_pb = call(upstream_service.list_upstreams, req_pb, LOGIN)
    assert len(resp_pb.upstreams) == 4
    assert [b.meta.id for b in resp_pb.upstreams] == sorted(ids, key=order_labels.get)

    req_pb.sort_order = api_pb2.DESCEND
    resp_pb = call(upstream_service.list_upstreams, req_pb, LOGIN)
    assert len(resp_pb.upstreams) == 4
    assert [b.meta.id for b in resp_pb.upstreams] == sorted(ids, key=order_labels.get, reverse=True)

    req_pb.query.id_regexp = '^a|b$'
    resp_pb = call(upstream_service.list_upstreams, req_pb, LOGIN)
    assert len(resp_pb.upstreams) == 2
    assert [b.meta.id for b in resp_pb.upstreams] == sorted(['aaa', 'bbb'], key=order_labels.get, reverse=True)

    req_pb.field_mask.paths.append('meta')
    resp_pb = call(upstream_service.list_upstreams, req_pb, LOGIN)
    assert len(resp_pb.upstreams) == 2
    assert resp_pb.upstreams[0].HasField('meta')
    assert all(not resp_pb.upstreams[0].HasField(f) for f in ('spec', 'status'))

    for _ in range(3):
        req_pb = api_pb2.UpdateUpstreamRequest()
        req_pb.meta.id = 'aaa'
        req_pb.meta.namespace_id = NS_ID
        req_pb.meta.version = upstreams['aaa'].meta.version
        req_pb.spec.CopyFrom(upstreams['aaa'].spec)
        req_pb.spec.yandex_balancer.yaml += ' '
        upstreams['aaa'] = call(upstream_service.update_upstream, req_pb, LOGIN).upstream

    req_pb = api_pb2.ListUpstreamRevisionsRequest(id='aaa', namespace_id=NS_ID)
    resp_pb = call(upstream_service.list_upstream_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert set(rev.meta.namespace_id for rev in resp_pb.revisions) == {NS_ID}
    assert len(resp_pb.revisions) == 4
    assert resp_pb.revisions[0].meta.id == upstreams['aaa'].meta.version

    req_pb = api_pb2.ListUpstreamRevisionsRequest(id='aaa', namespace_id=NS_ID, skip=2)
    resp_pb = call(upstream_service.list_upstream_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 2

    req_pb = api_pb2.ListUpstreamRevisionsRequest(id='aaa', namespace_id=NS_ID, skip=2, limit=1)
    resp_pb = call(upstream_service.list_upstream_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 1


@mock.patch.object(validation, 'validate_upstream_config')
def test_get_upstream_revision(_, cache, create_default_namespace):
    create_default_namespace(NS_ID)
    create_balancer(cache, NS_ID, NS_ID)

    upstream_id_to_pb = {}
    ids = ['aaa', 'bbb', 'ccc', 'ddd']
    for id in ids:
        req_pb = api_pb2.CreateUpstreamRequest()
        req_pb.meta.id = id
        req_pb.meta.namespace_id = NS_ID
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.spec.type = model_pb2.YANDEX_BALANCER
        req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_2

        u = call(upstream_service.create_upstream, req_pb, LOGIN).upstream
        upstream_id_to_pb[u.meta.id] = u

    req_pb = api_pb2.GetUpstreamRevisionRequest(id=upstream_id_to_pb['aaa'].meta.version)
    resp_pb = call(upstream_service.get_upstream_revision, req_pb, LOGIN)
    assert resp_pb.revision.spec == upstream_id_to_pb['aaa'].spec


def test_upstreams_config_to_yaml(cache, create_default_namespace):
    LOGIN = 'morrison'
    upstream_id = 'easy-mode-id'

    create_default_namespace(NS_ID)
    create_balancer(cache, NS_ID, NS_ID)

    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = upstream_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    yandex_balancer_pb = req_pb.spec.yandex_balancer
    yandex_balancer_pb.yaml = '''regexp_section:
  matcher:
    match_fsm:
      host: privet
  modules:
  - errorlog:
      log_level: ERROR
      log: !f get_log_path("test", get_port_var("port"))
  - errordocument: {status: 201}
'''
    resp_pb = call(upstream_service.create_upstream, req_pb, LOGIN)
    u_pb = resp_pb.upstream
    assert u_pb.meta.id == upstream_id
    assert u_pb.meta.namespace_id == NS_ID
    assert u_pb.spec.yandex_balancer.yaml == req_pb.spec.yandex_balancer.yaml

    update_req_pb = api_pb2.UpdateUpstreamRequest()
    update_req_pb.meta.id = upstream_id
    update_req_pb.meta.namespace_id = NS_ID
    update_req_pb.meta.version = u_pb.meta.version
    update_req_pb.spec.CopyFrom(u_pb.spec)
    update_req_pb.spec.yandex_balancer.yaml = ''
    update_req_pb.spec.yandex_balancer.config.CopyFrom(u_pb.spec.yandex_balancer.config)
    update_req_pb.spec.yandex_balancer.config.regexp_section.matcher.match_fsm.host = 'poka'
    call(upstream_service.update_upstream, update_req_pb, LOGIN)

    def check_upstream():
        req_pb = api_pb2.GetUpstreamRequest(id=upstream_id, namespace_id=NS_ID)
        u_pb = call(upstream_service.get_upstream, req_pb, LOGIN).upstream
        assert u_pb.spec.yandex_balancer.yaml == '''regexp_section:
  matcher:
    match_fsm:
      host: poka
  modules:
  - errorlog:
      log: !f 'get_log_path("test", get_port_var("port"))'
      log_level: ERROR
  - errordocument:
      status: 201
'''

    wait_until_passes(check_upstream)


def test_lint_upstream(create_default_namespace):
    LOGIN = 'morrison'
    NS_ID = 'test-namespace'

    create_default_namespace(NS_ID)

    req_pb = api_pb2.LintUpstreamRequest()
    req_pb.meta.id = 'test-upstream'
    req_pb.meta.namespace_id = NS_ID

    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = '''
errorlog:
    log_level: DEBUG
    log: /tmp/test.txt
    errordocument: {status: 200}'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert not resp_pb.warnings

    req_pb.spec.yandex_balancer.yaml = '''
balancer2:
  attempts: 1  # just one attempt
  rr: {}
  generated_proxy_backends:
    proxy_options:
      backend_timeout: 200ms
      connect_timeout: 100ms
    nanny_snapshots:
      - service_id: abc
        snapshot_id: def'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert not resp_pb.warnings

    req_pb.spec.yandex_balancer.yaml = '''
modules:
  - report: {uuid: abc, ranges: default}
  - balancer2:
      attempts: 2  # two attempts
      rr: {}
      generated_proxy_backends:
        proxy_options:
          backend_timeout: 200ms
          connect_timeout: 100ms
        nanny_snapshots:
          - service_id: abc
            snapshot_id: def'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert len(resp_pb.warnings) == 1
    w_pb = resp_pb.warnings[0]
    assert w_pb.rule == 'ARL'
    assert w_pb.path == ['report', 'balancer2']
    assert w_pb.message == 'Attempts rate limiter is missing.'
    assert set(w_pb.tags) == {'no-attempts-rate-limiter', 'awacs-ui'}

    req_pb.spec.yandex_balancer.yaml = '''
modules:
- balancer2:
    attempts_rate_limiter:
      limit: 0.2
    attempts: 4
    rr: {}
    backends:
    - weight: 1
      name: first
      modules:
        - balancer2:
            attempts: 2
            rr: {}
            backends:
            - weight: 1
              name: first
              modules:
                - balancer2:
                    attempts: 1
                    weighted2: {}
                    generated_proxy_backends:
                      proxy_options: {}
                      include_backends:
                        type: BY_ID
                        ids: [first]'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert len(resp_pb.warnings) == 1
    w_pb = resp_pb.warnings[0]
    assert w_pb.rule == 'UNSAFE_OUTER_BALANCER2'
    assert w_pb.path == ['balancer2']
    assert w_pb.message == 'Attempts rate limiter is configured in outer balancer2.'
    assert set(w_pb.tags) == {'unsafe-outer-balancer2', 'awacs-ui'}

    req_pb.spec.yandex_balancer.yaml = '''
modules:
- balancer2:
    attempts_rate_limiter:
      limit: 0.2
    attempts: 4
    rr: {}
    backends:
    - weight: 1
      name: first
      modules:
        - balancer2:
            attempts_rate_limiter:
              limit: 0.3
            attempts: 2
            rr: {}
            backends:
            - weight: 1
              name: first
              modules:
                - balancer2:
                    attempts: 1
                    weighted2: {}
                    generated_proxy_backends:
                      proxy_options: {}
                      include_backends:
                        type: BY_ID
                        ids: [first]'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert len(resp_pb.warnings) == 2
    w_pb = resp_pb.warnings[0]
    assert w_pb.rule == 'UNSAFE_OUTER_BALANCER2'
    assert w_pb.path == ['balancer2']
    assert w_pb.message == 'Attempts rate limiter is configured in outer balancer2.'
    assert set(w_pb.tags) == {'unsafe-outer-balancer2', 'awacs-ui'}
    w_pb = resp_pb.warnings[1]
    assert w_pb.rule == 'UNSAFE_OUTER_BALANCER2'
    assert w_pb.path == ['balancer2', 'backends[0][name="first"]', 'balancer2']
    assert w_pb.message == 'Attempts rate limiter is configured in outer balancer2.'
    assert set(w_pb.tags) == {'unsafe-outer-balancer2', 'awacs-ui'}

    req_pb.spec.yandex_balancer.yaml = '''
modules:
- balancer2:
    retry_policy:
        watermark_policy:
           lo: 0.5
           hi: 0.7
           unique_policy: {}
    attempts: 2
    rr: {}
    backends:
    - weight: 1
      name: first
      modules:
        - balancer2:
            attempts: 1
            weighted2: {}
            generated_proxy_backends:
              proxy_options: {}
              include_backends:
                type: BY_ID
                ids: [first]'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert len(resp_pb.warnings) == 1
    w_pb = resp_pb.warnings[0]
    assert w_pb.rule == 'UNSAFE_OUTER_BALANCER2'
    assert w_pb.path == ['balancer2']
    assert w_pb.message == 'Watermark policy is configured in outer balancer2.'
    assert set(w_pb.tags) == {'unsafe-outer-balancer2', 'awacs-ui'}

    req_pb.spec.yandex_balancer.yaml = '''
modules:
- balancer2:
    attempts_rate_limiter:
      limit: 0.2
    attempts: 4
    rr: {}
    backends:
    - weight: 1
      name: first
      modules:
        - proxy:
           host: 'awacs.yandex-team.ru'
           port: 80'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert not resp_pb.warnings

    req_pb.spec.yandex_balancer.yaml = '''
modules:
- balancer2:
   watermark_policy:
     lo: 0.5
     hi: 0.7
     unique_policy: {}
   attempts: 4
   rr: {}
   backends:
   - weight: 1
     name: first
     modules:
     - balancer2:
         attempts: 4
         rr: {}
         backends:
         - weight: 1
           name: first
           modules:
             - balancer2:
                 attempts: 1
                 weighted2: {}
                 generated_proxy_backends:
                   proxy_options: {}
                   include_backends:
                     type: BY_ID
                     ids: [first]
         - weight: 1
           name: second
           modules:
             - proxy:
                 host: 'ferenets.yandex-team.ru'
                 port: 80
   - weight: 1
     name: second
     modules:
       - balancer2:
           attempts: 1
           weighted2: {}
           generated_proxy_backends:
             proxy_options: {}
             include_backends:
               type: BY_ID
               ids: [first]'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert len(resp_pb.warnings) == 1
    w_pb = resp_pb.warnings[0]
    assert w_pb.rule == 'UNSAFE_OUTER_BALANCER2'
    assert w_pb.path == ['balancer2']
    assert w_pb.message == 'Watermark policy is configured in outer balancer2.'
    assert set(w_pb.tags) == {'unsafe-outer-balancer2', 'awacs-ui'}


def test_disable_linter(disable_linter):
    LOGIN = 'morrison'

    req_pb = api_pb2.LintUpstreamRequest()
    req_pb.meta.id = 'test-upstream'
    req_pb.meta.namespace_id = 'test-namespace'

    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = '''
modules:
  - report: {uuid: abc, ranges: default}
  - balancer2:
      attempts: 2  # two attempts
      rr: {}
      generated_proxy_backends:
        proxy_options:
          backend_timeout: 200ms
          connect_timeout: 100ms
        nanny_snapshots:
          - service_id: abc
            snapshot_id: def'''.strip()

    resp_pb = call(upstream_service.lint_upstream, req_pb, LOGIN)
    assert not resp_pb.warnings


def test_removal_checks(create_default_namespace):
    create_default_namespace(NS_ID)

    upstream_id = 'xxx'
    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = upstream_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_1
    call(upstream_service.create_upstream, req_pb, LOGIN)

    req_pb = api_pb2.GetUpstreamRemovalChecksRequest(namespace_id=NS_ID, id=upstream_id)
    resp_pb = call(upstream_service.get_upstream_removal_checks, req_pb, LOGIN)
    use_in_domains_check = resp_pb.checks[0]
    assert use_in_domains_check.state == use_in_domains_check.PASSED
    assert use_in_domains_check.message == 'Upstream is not required by domains'


ANNOTATED_UPSTREAM_1_YAML = '''l7_upstream_macro:
  version: 0.0.1
  id: ugcpub
  matcher:
    uri_re: /(my|ugcpub)(.*)
  headers:
    - uaas:
        service_name: ugcpub
  by_dc_scheme:
    dc_balancer:
      weights_section_id: 'bygeo'
      method: BY_DC_WEIGHT
      attempts: 2
    balancer:
      attempts: 2
      backend_timeout: 10s
      connect_timeout: 100ms
      retry_http_responses:
        codes: [5xx]
      max_reattempts_share: 0.2
      max_pessimized_endpoints_share: 0.2
    dcs:
      - name: man
        backend_ids:
        - ugc-backend-man
      - name: sas
        backend_ids:
        - ugc-backend-sas
      - name: vla
        backend_ids:
        - ugc-backend-vla
    on_error:
      rst: true'''

ANNOTATED_UPSTREAM_1_INCLUDED_FULL_BACKEND_IDS = [u'bbb_sas/ugc-backend-man',
                                                  u'bbb_sas/ugc-backend-sas',
                                                  u'bbb_sas/ugc-backend-vla',
                                                  u'uaas.search.yandex.net/usersplit_man',
                                                  u'uaas.search.yandex.net/usersplit_sas',
                                                  u'uaas.search.yandex.net/usersplit_vla']
ANNOTATED_UPSTREAM_2_YAML = '''regexp_section:
  matcher:
    match_fsm:
      uri: '/hey'
  modules:
    - antirobot_macro:
        version: 0.0.2
    - geobase_macro: {}
    - exp_getter_macro:
        service_name: kinopoisk
        testing_mode: {}
    - stats_eater: {}
    - hasher:
        mode: request
    - balancer2:
        attempts: 2
        rr: {weights_file: ./controls/traffic_control.weights}
        backends:
        - weight: 1
          name: kpprod_iva
          modules:
            - report: {ranges: default, uuid: requests_to_iva}
            - balancer2:
                attempts: 1
                rr: {}
                generated_proxy_backends:
                  proxy_options:
                    connect_timeout: 200ms
                    backend_timeout: 60s
                    fail_on_5xx: false
                  include_backends:
                    type: BY_ID
                    ids: [hey1, hey2]'''

ANNOTATED_UPSTREAM_2_INCLUDED_FULL_BACKEND_IDS = [u'bbb_sas/hey1',
                                                  u'bbb_sas/hey2',
                                                  u'common-antirobot/antirobot_man_yp',
                                                  u'common-antirobot/antirobot_sas_yp',
                                                  u'common-antirobot/antirobot_vla_yp',
                                                  u'uaas.search.yandex.net/usersplit_man',
                                                  u'uaas.search.yandex.net/usersplit_sas',
                                                  u'uaas.search.yandex.net/usersplit_vla']


@pytest.mark.parametrize('upstream_mode,upstream_yaml,expected_included_full_backend_ids', [
    (model_pb2.YandexBalancerUpstreamSpec.EASY_MODE2, ANNOTATED_UPSTREAM_1_YAML,
     ANNOTATED_UPSTREAM_1_INCLUDED_FULL_BACKEND_IDS),
    (model_pb2.YandexBalancerUpstreamSpec.FULL_MODE, ANNOTATED_UPSTREAM_2_YAML,
     ANNOTATED_UPSTREAM_2_INCLUDED_FULL_BACKEND_IDS),
])
def test_upstream_annotations(create_default_namespace, upstream_mode, upstream_yaml,
                              expected_included_full_backend_ids):
    create_default_namespace(NS_ID)

    upstream_id = 'ugcpub'
    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = upstream_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = upstream_mode
    req_pb.spec.yandex_balancer.yaml = upstream_yaml

    call(upstream_service.create_upstream, req_pb, LOGIN)

    def check():
        req_pb = api_pb2.GetUpstreamRequest(namespace_id=NS_ID, id=upstream_id)
        req_pb.annotations.append(api_pb2.AnnotatedUpstream.UA_INCLUDED_FULL_BACKEND_IDS)
        resp_pb = call(upstream_service.get_upstream, req_pb, LOGIN)
        assert not resp_pb.HasField('upstream')
        assert resp_pb.HasField('annotated_upstream')
        assert resp_pb.annotated_upstream.included_full_backend_ids == expected_included_full_backend_ids

    wait_until_passes(check)


def test_rps_limiter(create_default_namespace):
    ns_pb = create_default_namespace(NS_ID)

    upstream_id = u'upstream'
    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = upstream_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerUpstreamSpec.EASY_MODE2
    req_pb.spec.yandex_balancer.yaml = u'''\
l7_upstream_macro:
  version: 0.0.1
  id: upstream
  matcher:
    any: true
  rps_limiter:
    external:
      record_name: fff
      installation: COMMON
  static_response:
    status: 200
    '''

    resp_pb = call(upstream_service.create_upstream, req_pb, LOGIN)
    u_pb = resp_pb.upstream

    for public_inst_name in rps_limiter_settings._public_installation_names:
        req_pb = api_pb2.UpdateUpstreamRequest()
        req_pb.meta.CopyFrom(u_pb.meta)
        req_pb.spec.CopyFrom(u_pb.spec)
        req_pb.spec.yandex_balancer.yaml = u''
        req_pb.spec.yandex_balancer.config.l7_upstream_macro.rps_limiter.external.installation = public_inst_name
        u_pb = call(upstream_service.update_upstream, req_pb, LOGIN).upstream

    req_pb = api_pb2.UpdateUpstreamRequest()
    req_pb.meta.CopyFrom(u_pb.meta)
    req_pb.spec.CopyFrom(u_pb.spec)
    req_pb.spec.yandex_balancer.yaml = u''
    req_pb.spec.yandex_balancer.config.l7_upstream_macro.rps_limiter.external.installation = u'PDB'

    with awtest.raises(exceptions.BadRequestError,
                       text_contains=u'using installation "PDB" is not allowed in this namespace'):
        call(upstream_service.update_upstream, req_pb, LOGIN)

    update_ns_req_pb = api_pb2.UpdateNamespaceRequest()
    update_ns_req_pb.meta.CopyFrom(ns_pb.meta)
    update_ns_req_pb.spec.CopyFrom(ns_pb.spec)
    update_ns_req_pb.spec.rps_limiter_allowed_installations.installations.append(u'PDB')
    call(namespace_service.update_namespace, update_ns_req_pb, u'very-root-user', enable_auth=True)
    call(upstream_service.update_upstream, req_pb, LOGIN)
