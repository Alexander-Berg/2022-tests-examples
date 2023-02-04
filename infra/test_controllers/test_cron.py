# coding: utf-8
import json
import mock

import yaml
import datetime
import inject
import pytest
import six

from infra.swatlib import metrics

from awacs.lib.itsclient import IItsClient, ItsClient
from awacs.lib.nannyclient import INannyClient, NannyClient
from awacs.lib.nannyrpcclient import INannyRpcClient, NannyRpcClient
from awacs.model.balancer.state_handler import L7BalancerStateHandler
from awacs.model.balancer.vector import BalancerVersion, UpstreamVersion, DomainVersion
from awacs.model.cron import Cron
from awacs.model.cron import dismissedcleaner
from awacs.model.cron.base import AspectsUpdaterError
from awacs.model.cron.configupdater import ConfigAspectsUpdater
from awacs.model.cron.itsupdater import ItsAspectsUpdater
from awacs.model.validation import validate_and_parse_yaml_upstream_config, validate_and_parse_yaml_balancer_config
from infra.awacs.proto import model_pb2, api_pb2, modules_pb2
from awacs.web import balancer_service, namespace_service, statistics_service
from awtest import wait_until_passes, freeze_time, wait_until
from awtest.api import call
import gevent

BALANCER_1_YML = '''instance_macro:
  workers: 1
  private_address: 127.0.0.10
  default_tcp_rst_on_error: false
  sections:
    admin:
      ips:
        - 127.0.0.1
        - '::1'
      ports:
        - 16100
      http:
        maxlen: 65536
        maxreq: 65536
        admin: {}
    stats_storage:
      ips: [127.0.0.4]
      ports: [16100]
      modules:
        - report:
            uuid: "service_total"
            ranges: "default"
        - http: {}
        - errordocument:
            status: 200
    local_ips_16100:
      ips:
        - local_v4_addr
        - local_v6_addr
      ports:
        - 16100
      extended_http_macro:
        port: 16100
        report_uuid: http
        modules:
          - headers:
              create:
                name: value
          - report:
              uuid: test-1
              ranges: default
          - regexp_path:
              include_upstreams:
                type: BY_ID
                ids:
                  - led
                  - zeppelin
                  - default'''

BALANCER_2_YML = '''instance_macro:
  workers: 1
  private_address: 127.0.0.10
  default_tcp_rst_on_error: false
  sections:
    admin:
      ips:
        - 127.0.0.1
        - '::1'
      ports:
        - 16100
      http:
        maxlen: 65536
        maxreq: 65536
        admin: {}
    stats_storage:
      ips: [127.0.0.4]
      ports: [16100]
      modules:
        - report:
            uuid: "service_total"
            ranges: "default"
        - http: {}
        - errordocument:
            status: 200
    local_ips_16100:
      ips:
        - local_v4_addr
        - local_v6_addr
      ports:
        - 16100
      extended_http_macro:
        port: 16100
        report_uuid: http
        modules:
          - report:
              uuid: extra-report-uuid
              ranges: default
          - headers:
              create:
                name: value
          - report:
              uuid: test-1
              ranges: default
          - regexp_path:
              include_upstreams:
                type: BY_ID
                ids:
                  - led
                  - zeppelin
                  - default'''

BALANCER_3_YML = '''---
l7_macro:
  version: 0.0.4
  health_check_reply: {}
  announce_check_reply:
    url_re: /ping
  http: {}
  include_domains: {}
  headers:
    - create: {target: X-Start-Time, func: starttime}
    - create: {target: X-Forwarded-For-Y, func: realip, keep_existing: true}
    - create: {target: X-Source-Port-Y, func: realport, keep_existing: true}
    - create: {target: X-Forwarded-For, func: realip, keep_existing: true}
    - create: {target: X-Req-Id, func: reqid, keep_existing: true}
    - create: {target: X-Scheme, func: scheme, keep_existing: true}
    - create: {target: X-Forwarded-Proto, func: scheme, keep_existing: true}
  response_headers:
    - create: {target: Strict-Transport-Security, value: max-age=31536000}
'''

UPSTREAM_YML = '''regexp_section:
  matcher:
    match_fsm:
      uri: '/touchsearch(/.*)?'
  modules:
    - report:
        uuid: uuid1
        refers: uuid2
    - exp_getter_macro: {}
    - antirobot_macro:
        file_switch: './controls/touchsearch_disable_antirobot_module'
        gencfg_groups:
          - name: MAN_ANTIROBOT_ANTIROBOT
            version: tags/stable-92-r105
    - stats_eater: {}
    - balancer2:
        rr: {}
        attempts: 2
        backends:
        - name: test-1
          weight: 1
          balancer2:
            attempts: 2
            weighted2: {}
            generated_proxy_backends:
              proxy_options:
                connect_timeout: 10ms
                backend_timeout: 1s
              nanny_snapshots:
                - service_id: mobile_heroism
                  snapshot_id: fb80faf52df0e452358e1344e88b8cff99089835
            on_error:
              errordocument:
                status: 504
        - name: test-2
          weight: 1
          balancer2:
            attempts: 2
            weighted2: {}
            disable_attempts_rate_limiter: true
            generated_proxy_backends:
              proxy_options:
                connect_timeout: 10ms
                backend_timeout: 1s
              nanny_snapshots:
                - service_id: mobile_heroism
                  snapshot_id: fb80faf52df0e452358e1344e88b8cff99089835
            on_error:
              errordocument:
                status: 504'''


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        its_oauth_token = 'DUMMY'
        b.bind(IItsClient, ItsClient(url='https://its.yandex-team.ru/', token=its_oauth_token))
        nanny_oauth_token = 'DUMMY'
        b.bind(INannyRpcClient, NannyRpcClient(url='https://nanny.yandex-team.ru/', token=nanny_oauth_token))
        b.bind(INannyClient, NannyClient(url='https://nanny.yandex-team.ru/v2/', token=nanny_oauth_token))
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.mark.vcr
@pytest.mark.slow
def test_cron_basics(dao, zk_storage, mongo_storage, ctx, log, create_default_namespace):
    login = 'romanovich'
    namespace_1_id = 'swat.yandex-team.ru'
    balancer_1_id = 'man.swat.yandex-team.ru'
    balancer_1_service_id = 'production_balancer_swat_man'
    balancer_2_id = 'sas.swat.yandex-team.ru'
    balancer_2_service_id = 'production_balancer_swat_sas'

    namespace_2_id = 'adm-nanny.yandex-team.ru'
    balancer_3_id = 'adm-nanny.yandex-team.ru_man'
    balancer_3_service_id = 'rtc_balancer_adm-nanny_yandex-team_ru_man'
    domain_1_id = 'adm-nanny.yandex-team.ru'

    ctx.log.info('Setting up prerequisites')

    namespace_pb = model_pb2.Namespace()
    namespace_pb.meta.id = namespace_1_id
    create_default_namespace(namespace_1_id)

    namespace_2_pb = model_pb2.Namespace()
    namespace_2_pb.meta.id = namespace_2_id
    create_default_namespace(namespace_2_id)

    meta_pb = model_pb2.BalancerMeta(namespace_id=namespace_1_id, id=balancer_1_id)
    meta_pb.location.type = meta_pb.location.GENCFG_DC
    spec_pb = model_pb2.BalancerSpec()
    spec_pb.config_transport.type = model_pb2.NANNY_STATIC_FILE
    spec_pb.config_transport.nanny_static_file.service_id = balancer_1_service_id
    spec_pb.yandex_balancer.yaml = BALANCER_1_YML
    validate_and_parse_yaml_balancer_config(spec_pb,
                                            full_balancer_id=(namespace_1_id, balancer_1_id),
                                            namespace_pb=namespace_pb)

    balancer_1_pb, balancer_1_state_pb = dao.create_balancer(meta_pb=meta_pb, spec_pb=spec_pb, login=login)

    meta_pb = model_pb2.BalancerMeta(namespace_id=namespace_1_id, id=balancer_2_id)
    meta_pb.location.type = meta_pb.location.GENCFG_DC
    spec_pb = model_pb2.BalancerSpec()
    spec_pb.config_transport.type = model_pb2.NANNY_STATIC_FILE
    spec_pb.config_transport.nanny_static_file.service_id = balancer_2_service_id
    spec_pb.yandex_balancer.yaml = BALANCER_2_YML
    validate_and_parse_yaml_balancer_config(spec_pb,
                                            full_balancer_id=(namespace_1_id, balancer_2_id),
                                            namespace_pb=namespace_pb)

    balancer_2_pb, balancer_2_state_pb = dao.create_balancer(meta_pb=meta_pb, spec_pb=spec_pb, login=login)

    meta_pb = model_pb2.BalancerMeta(namespace_id=namespace_2_id, id=balancer_3_id)
    meta_pb.location.type = meta_pb.location.YP_CLUSTER
    meta_pb.location.yp_cluster = 'VLA'
    spec_pb = model_pb2.BalancerSpec()
    spec_pb.config_transport.type = model_pb2.NANNY_STATIC_FILE
    spec_pb.config_transport.nanny_static_file.service_id = balancer_3_service_id
    spec_pb.config_transport.nanny_static_file.instance_tags.itype = 'balancer'
    spec_pb.config_transport.nanny_static_file.instance_tags.ctype = 'prod'
    spec_pb.config_transport.nanny_static_file.instance_tags.prj = 'admin-nanny-balancer'
    spec_pb.yandex_balancer.mode = spec_pb.yandex_balancer.EASY_MODE
    spec_pb.yandex_balancer.yaml = BALANCER_3_YML

    d_meta_pb = model_pb2.DomainMeta(namespace_id=namespace_2_id, id=domain_1_id)
    d_spec_pb = model_pb2.DomainSpec()
    d_spec_pb.yandex_balancer.config.include_upstreams.type = modules_pb2.ALL
    d_spec_pb.yandex_balancer.config.fqdns.extend(['adm-nanny.yandex-team.ru'])
    domain_pb = dao.create_domain(meta_pb=d_meta_pb, spec_pb=d_spec_pb, login=login)

    validate_and_parse_yaml_balancer_config(spec_pb,
                                            full_balancer_id=(namespace_2_id, balancer_3_id),
                                            namespace_pb=namespace_2_pb)
    balancer_3_pb, balancer_3_state_pb = dao.create_balancer(meta_pb=meta_pb, spec_pb=spec_pb, login=login)

    upstream_id_1 = 'upstream-1'
    meta_pb = model_pb2.UpstreamMeta(namespace_id=namespace_1_id, id=upstream_id_1)
    spec_pb = model_pb2.UpstreamSpec()
    spec_pb.yandex_balancer.yaml = UPSTREAM_YML
    validate_and_parse_yaml_upstream_config(
        namespace_id=namespace_1_id,
        upstream_id=upstream_id_1,
        spec_pb=spec_pb)
    upstream_pb = dao.create_upstream(meta_pb, spec_pb, login)

    upstream_id_2 = 'upstream-2'
    meta_pb = model_pb2.UpstreamMeta(namespace_id=namespace_2_id, id=upstream_id_2)
    spec_pb = model_pb2.UpstreamSpec()
    spec_pb.yandex_balancer.yaml = UPSTREAM_YML
    validate_and_parse_yaml_upstream_config(
        namespace_id=namespace_2_id,
        upstream_id=upstream_id_2,
        spec_pb=spec_pb)
    upstream_pb_2 = dao.create_upstream(meta_pb, spec_pb, login)

    for pb in zk_storage.update_balancer_state(namespace_1_id, balancer_1_id, balancer_1_state_pb):
        h = L7BalancerStateHandler(pb)
        curr_balancer_ver = BalancerVersion.from_pb(balancer_1_pb)
        h.add_new_rev(curr_balancer_ver)
        h.set_active_rev(curr_balancer_ver)

        curr_upstream_1_ver = UpstreamVersion.from_pb(upstream_pb)
        h.add_new_rev(curr_upstream_1_ver)
        h.set_active_rev(curr_upstream_1_ver)

    for pb in zk_storage.update_balancer_state(namespace_1_id, balancer_2_id, balancer_2_state_pb):
        h = L7BalancerStateHandler(pb)
        curr_balancer_ver = BalancerVersion.from_pb(balancer_2_pb)
        h.add_new_rev(curr_balancer_ver)
        h.set_active_rev(curr_balancer_ver)

        curr_upstream_1_ver = UpstreamVersion.from_pb(upstream_pb)
        h.add_new_rev(curr_upstream_1_ver)
        h.set_active_rev(curr_upstream_1_ver)

    for pb in zk_storage.update_balancer_state(namespace_2_id, balancer_3_id, balancer_3_state_pb):
        h = L7BalancerStateHandler(pb)
        curr_balancer_ver = BalancerVersion.from_pb(balancer_3_pb)
        h.add_new_rev(curr_balancer_ver)
        h.set_active_rev(curr_balancer_ver)

        curr_domain_1_ver = DomainVersion.from_pb(domain_pb)
        h.add_new_rev(curr_domain_1_ver)
        h.set_active_rev(curr_domain_1_ver)

        curr_upstream_2_ver = UpstreamVersion.from_pb(upstream_pb_2)
        h.add_new_rev(curr_upstream_2_ver)
        h.set_active_rev(curr_upstream_2_ver)

    ctx.log.info('Running cron')
    cron = Cron(metrics_registry=metrics.ROOT_REGISTRY)
    cron.PROPAGATION_DELAY = 1
    date = datetime.datetime(year=2020, month=6, day=16, hour=3, minute=3, second=9)
    with freeze_time(date):
        cron._run()

    ctx.log.info('check balancer 1')

    def check_balancer():
        req_pb = api_pb2.GetBalancerAspectsSetRequest(namespace_id=namespace_1_id, id=balancer_1_id)
        resp_pb = call(balancer_service.get_balancer_aspects_set, req_pb, login)
        aspects_set_pb = resp_pb.aspects_set  # type: model_pb2.BalancerAspectsSet
        assert aspects_set_pb.meta.namespace_id == namespace_1_id
        assert aspects_set_pb.meta.balancer_id == balancer_1_id
        expected_cluster_content_pb = model_pb2.ClusterAspectsContent(
            active_conf_id='production_balancer_swat_man-1561995791020',
            locations=['man'],
            platform=model_pb2.ClusterAspectsContent.GENCFG,
            tags=model_pb2.ClusterAspectsContent.Tags(
                itype=['balancer'],
                ctype=['prod'],
                prj=['cplb-swat', 'swat'],
                metaprj=['balancer']
            )
        )
        assert aspects_set_pb.content.cluster.content == expected_cluster_content_pb
        expected_config_content_pb = model_pb2.ItsAspectsContent(
            location_paths=['balancer/swat/man/man']
        )
        assert aspects_set_pb.content.its.content == expected_config_content_pb

        expected_config_content_pb = model_pb2.ConfigAspectsContent(
            balancer=model_pb2.ConfigAspectsContent.BalancerConfigAspects(
                report_uuids=['service_total', 'http', 'test-1'],
            ),
            upstreams=[
                model_pb2.ConfigAspectsContent.UpstreamConfigAspects(
                    id=upstream_id_1,
                    report_uuids=['uuid1', 'expgetter', 'antirobot']
                )
            ]
        )
        assert aspects_set_pb.content.config.content == expected_config_content_pb

    wait_until_passes(check_balancer, timeout=3)

    ctx.log.info('check balancer 2')
    req_pb = api_pb2.GetBalancerAspectsSetRequest(namespace_id=namespace_1_id, id=balancer_2_id)
    resp_pb = call(balancer_service.get_balancer_aspects_set, req_pb, login)
    aspects_set_pb = resp_pb.aspects_set  # type: model_pb2.BalancerAspectsSet
    assert aspects_set_pb.meta.namespace_id == namespace_1_id
    assert aspects_set_pb.meta.balancer_id == balancer_2_id
    expected_cluster_content_pb = model_pb2.ClusterAspectsContent(
        active_conf_id='production_balancer_swat_sas-1561994811315',
        locations=['sas'],
        platform=model_pb2.ClusterAspectsContent.GENCFG,
        tags=model_pb2.ClusterAspectsContent.Tags(
            itype=['balancer'],
            ctype=['prestable'],
            prj=['cplb-swat', 'swat'],
            metaprj=['balancer']
        )
    )
    assert aspects_set_pb.content.cluster.content == expected_cluster_content_pb
    expected_config_content_pb = model_pb2.ItsAspectsContent(
        location_paths=['balancer/swat/sas/sas']
    )
    assert aspects_set_pb.content.its.content == expected_config_content_pb

    expected_config_content_pb = model_pb2.ConfigAspectsContent(
        balancer=model_pb2.ConfigAspectsContent.BalancerConfigAspects(
            report_uuids=['service_total', 'http', 'extra-report-uuid', 'test-1'],
        ),
        upstreams=[
            model_pb2.ConfigAspectsContent.UpstreamConfigAspects(
                id=upstream_id_1,
                report_uuids=['uuid1', 'expgetter', 'antirobot']
            )
        ]
    )
    assert aspects_set_pb.content.config.content == expected_config_content_pb

    ctx.log.info('check namespace')
    req_pb = api_pb2.GetNamespaceAspectsSetRequest(id=namespace_1_id)
    resp_pb = call(namespace_service.get_namespace_aspects_set, req_pb, login)
    aspects_set_pb = resp_pb.aspects_set  # type: model_pb2.NamespaceAspectsSet
    assert aspects_set_pb
    yasm_tab_pbs = aspects_set_pb.content.ui.content.yasm_tabs
    common_yasm_tab_pb = yasm_tab_pbs[0]
    assert common_yasm_tab_pb.id == 'common'
    service_total_yasm_panel_pb = common_yasm_tab_pb.panels[2]
    assert service_total_yasm_panel_pb.id == 'service_total'
    assert (
        service_total_yasm_panel_pb.desc ==
        'Statistics from "report" module with uuid "service_total" '
        'from balancers man.swat.yandex-team.ru, sas.swat.yandex-team.ru'
    )
    assert (
        service_total_yasm_panel_pb.url ==
        "https://yasm.yandex-team.ru/template/panel/balancer_common_panel/"
        "fqdn=swat.yandex-team.ru;"
        "itype=balancer;ctype=prestable,prod;"
        "locations=man,sas;prj=swat;signal=service_total;"
    )

    assert (
        json.loads(aspects_set_pb.content.ui.content.inclusion_graph_json) ==
        [
            {'type': 'backend', 'id': 'usersplit_man', 'namespace_id': 'uaas.search.yandex.net'},
            {'type': 'backend', 'id': 'usersplit_sas', 'namespace_id': 'uaas.search.yandex.net'},
            {'type': 'backend', 'id': 'usersplit_vla', 'namespace_id': 'uaas.search.yandex.net'},
            {'type': 'balancer', 'id': 'man.swat.yandex-team.ru', 'namespace_id': 'swat.yandex-team.ru',
             'included_upstream_ids': ['swat.yandex-team.ru/upstream-1'], 'included_backend_ids': [], 'included_domain_ids': []},
            {'type': 'balancer', 'id': 'sas.swat.yandex-team.ru', 'namespace_id': 'swat.yandex-team.ru',
             'included_upstream_ids': ['swat.yandex-team.ru/upstream-1'], 'included_backend_ids': [], 'included_domain_ids': []},
            {'type': 'upstream', 'id': 'upstream-1', 'namespace_id': 'swat.yandex-team.ru',
             'included_backend_ids': ['uaas.search.yandex.net/usersplit_man', 'uaas.search.yandex.net/usersplit_sas',
                                      'uaas.search.yandex.net/usersplit_vla']},
        ]
    )

    ctx.log.info('check statistics')
    entries = mongo_storage.list_usage_statistics_entries()
    assert entries.total == 1
    date_statistics_pb = entries.items[0].date_statistics
    upstream_pie_pbs = date_statistics_pb.by_namespace[namespace_1_id].upstream_pies
    found_balancer2_modules_total_count = False
    found_balancer2_modules_without_arl_count = False
    found_balancer2_modules_without_arl_gpb_count = False
    found_balancer2_modules_without_arl_and_watermark_gpb_count = False
    for pie_pb in upstream_pie_pbs:
        if pie_pb.id == 'balancer2_modules_total_count':
            assert pie_pb.slices['3'] == 1
            found_balancer2_modules_total_count = True
        elif pie_pb.id == 'balancer2_modules_total_gpb_count':
            assert pie_pb.slices['2'] == 1
            found_balancer2_modules_total_count = True
        elif pie_pb.id == 'balancer2_modules_without_arl_count':
            assert pie_pb.slices['2'] == 1
            found_balancer2_modules_without_arl_count = True
        elif pie_pb.id == 'balancer2_modules_without_arl_gpb_count':
            assert pie_pb.slices['1'] == 1
            found_balancer2_modules_without_arl_gpb_count = True
        elif pie_pb.id == 'balancer2_modules_without_arl_and_watermark_gpb_count':
            assert pie_pb.slices['1'] == 1
            found_balancer2_modules_without_arl_and_watermark_gpb_count = True
    assert found_balancer2_modules_total_count
    assert found_balancer2_modules_without_arl_count
    assert found_balancer2_modules_without_arl_gpb_count
    assert found_balancer2_modules_without_arl_and_watermark_gpb_count

    req_pb = api_pb2.GetLoadStatisticsEntryRequest()
    date_midnight = datetime.datetime(year=2020, month=6, day=16, hour=0, minute=0, second=0)
    req_pb.start.FromDatetime(date_midnight)
    resp_pb = call(statistics_service.get_load_statistics_entry, req_pb, login)
    by_namespace = resp_pb.entry.date_statistics.by_namespace['adm-nanny.yandex-team.ru']
    assert 0 < by_namespace.average < 5
    assert 0 < by_namespace.max < 5
    by_balancer = resp_pb.entry.date_statistics.by_balancer['adm-nanny.yandex-team.ru/adm-nanny.yandex-team.ru_man']
    assert by_balancer.average == by_namespace.average
    assert by_balancer.max == by_namespace.max

    ctx.log.info('test hotfix by romanovich@')
    aspects_set_content_pb = model_pb2.BalancerAspectsSetContent()
    ConfigAspectsUpdater().update(balancer_3_pb, aspects_set_content_pb)
    assert set(aspects_set_content_pb.config.content.balancer.report_uuids) == {'service_total'}

    ctx.log.info('check namespace 2 map for domains')
    req_pb = api_pb2.GetNamespaceAspectsSetRequest(id=namespace_2_id)
    resp_pb = call(namespace_service.get_namespace_aspects_set, req_pb, login)
    aspects_set_pb = resp_pb.aspects_set  # type: model_pb2.NamespaceAspectsSet

    assert (
        json.loads(aspects_set_pb.content.ui.content.inclusion_graph_json) ==
        [{"type": "backend", "id": "usersplit_man", "namespace_id": "uaas.search.yandex.net"},
         {"type": "backend", "id": "usersplit_sas", "namespace_id": "uaas.search.yandex.net"},
         {"type": "backend", "id": "usersplit_vla", "namespace_id": "uaas.search.yandex.net"},
         {"type": "balancer", "id": "adm-nanny.yandex-team.ru_man", "namespace_id": "adm-nanny.yandex-team.ru", "included_upstream_ids": [],
          "included_backend_ids": [], "included_domain_ids": ["adm-nanny.yandex-team.ru/adm-nanny.yandex-team.ru"]},
         {"type": "domain", "id": "adm-nanny.yandex-team.ru", "namespace_id": "adm-nanny.yandex-team.ru",
          "included_upstream_ids": ["adm-nanny.yandex-team.ru/upstream-2"]},
         {"type": "upstream", "id": "upstream-2", "namespace_id": "adm-nanny.yandex-team.ru",
          "included_backend_ids": ["uaas.search.yandex.net/usersplit_man", "uaas.search.yandex.net/usersplit_sas",
                                   "uaas.search.yandex.net/usersplit_vla"]}]
    )


@mock.patch.object(ItsClient, 'get_config', return_value={
    'locations': {
        'groups': {
            'f1': {'filter': 'f@service1'},
            'f2': {'filter': 'f@service2'},
            'f123': {'filter': 'f@service1 f@service2 f@service3'},
            'other1': {'filter': 'I@a_dc_iva'},
            'other2': {'filter': '[ I@a_itype_balancer . I@a_prj_prj1 . I@a_geo_vla ] [ I@a_prj_prj2 . I@a_geo_vla ]'},
            'and1': {'filter': 'I@a_itype_balancer . I@a_prj_prj1'},
            'and2': {'filter': 'I@a_itype_balancer . I@a_ctype_prod'},
            'and3': {'filter': 'I@a_itype_balancer . I@a_prj_prj1 . I@a_ctype_prod'},
            'and4': {'filter': 'I@a_itype_balancer . I@a_prj_prj2 . [ I@a_ctype_prod I@a_ctype_test ] . I@a_geo_vla'},
            'and5': {'filter': 'I@a_itype_balancer . I@a_prj_prj1 . [ I@a_ctype_pre I@a_ctype_test ] . I@a_geo_sas'},
            'and6': {'filter': '[ I@a_prj_prj1 I@a_prj_prj2 ] . [ I@a_ctype_prod I@a_ctype_test ]'},
        }
    }
})
def test_its_updater(_):
    its_updater = ItsAspectsUpdater()
    its_updater.prepare()

    assert len(its_updater.locations._indexes_by_service_id) == 3
    assert len(its_updater.locations._indexes_by_service_id['service1']) == 2
    assert len(its_updater.locations._indexes_by_service_id['service2']) == 2
    assert len(its_updater.locations._indexes_by_service_id['service3']) == 1

    assert len(its_updater.locations._indexes_without_clear_requirements) == 2

    assert len(its_updater.locations._indexes_by_tags_requirements['itype']._require_tag_indexes) == 1
    assert len(its_updater.locations._indexes_by_tags_requirements['itype']._require_tag_indexes['balancer']) == 5
    assert len(its_updater.locations._indexes_by_tags_requirements['itype']._dont_require_tag_indexes) == 1
    assert len(its_updater.locations._indexes_by_tags_requirements['ctype']._require_tag_indexes) == 1
    assert len(its_updater.locations._indexes_by_tags_requirements['ctype']._require_tag_indexes['prod']) == 2
    assert len(its_updater.locations._indexes_by_tags_requirements['ctype']._dont_require_tag_indexes) == 4
    assert len(its_updater.locations._indexes_by_tags_requirements['prj']._require_tag_indexes) == 2
    assert len(its_updater.locations._indexes_by_tags_requirements['prj']._require_tag_indexes['prj1']) == 3
    assert len(its_updater.locations._indexes_by_tags_requirements['prj']._require_tag_indexes['prj2']) == 1
    assert len(its_updater.locations._indexes_by_tags_requirements['prj']._dont_require_tag_indexes) == 2
    assert len(its_updater.locations._indexes_by_tags_requirements['geo']._require_tag_indexes) == 2
    assert len(its_updater.locations._indexes_by_tags_requirements['geo']._require_tag_indexes['sas']) == 1
    assert len(its_updater.locations._indexes_by_tags_requirements['geo']._require_tag_indexes['vla']) == 1
    assert len(its_updater.locations._indexes_by_tags_requirements['geo']._dont_require_tag_indexes) == 4

    aspects_set_content_pb = model_pb2.BalancerAspectsSetContent()
    aspects_set_content_pb.cluster.status.last_attempt.succeeded.status = 'True'
    tags_pb = aspects_set_content_pb.cluster.content.tags
    tags_pb.itype.append('balancer')
    tags_pb.ctype.append('prod')
    tags_pb.prj.append('prj1')
    aspects_set_content_pb.cluster.content.locations.append('sas')

    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.location.type = balancer_pb.meta.location.YP_CLUSTER
    balancer_pb.meta.location.yp_cluster = 'iva'
    balancer_pb.spec.config_transport.nanny_static_file.service_id = 'service1'

    its_updater.update(balancer_pb, aspects_set_content_pb)
    assert aspects_set_content_pb.its.content.location_paths == ['and1', 'and2', 'and3', 'and6', 'f1', 'f123',
                                                                 'other1']

    balancer_pb.spec.config_transport.nanny_static_file.service_id = 'service3'
    its_updater.update(balancer_pb, aspects_set_content_pb)
    assert aspects_set_content_pb.its.content.location_paths == ['and1', 'and2', 'and3', 'and6', 'f123',
                                                                 'other1']
    tags_pb.ClearField('ctype')
    tags_pb.ctype.append('pre')
    its_updater.update(balancer_pb, aspects_set_content_pb)
    assert aspects_set_content_pb.its.content.location_paths == ['and1', 'and5', 'f123', 'other1']

    aspects_set_content_pb.cluster.content.locations.append('vla')
    its_updater.update(balancer_pb, aspects_set_content_pb)
    assert aspects_set_content_pb.its.content.location_paths == ['and1', 'and5', 'f123', 'other1', 'other2']

    tags_pb.ClearField('prj')
    tags_pb.ctype.append('prj2')
    its_updater.update(balancer_pb, aspects_set_content_pb)
    assert aspects_set_content_pb.its.content.location_paths == ['f123', 'other1']

    balancer_pb.meta.ClearField('location')
    balancer_pb.spec.config_transport.nanny_static_file.service_id = 'service'
    with pytest.raises(AspectsUpdaterError) as e:
        its_updater.update(balancer_pb, aspects_set_content_pb)
    assert 'Location type is not supported' in str(e)


@pytest.fixture()
def dissmissed_login_A():
    return "dissmissed_worker_A"


@pytest.fixture()
def dissmissed_login_B():
    return "dissmissed_worker_B"


@pytest.fixture()
def active_login_A():
    return "active_worker_A"


@pytest.fixture()
def active_login_B():
    return "active_worker_B"


@pytest.fixture()
def downtimers_with_dissmissed(dissmissed_login_A, active_login_A):
    downtimers = model_pb2.NamespaceSpec.AlertingSettings.JugglerRawDowntimers()
    downtimers.staff_logins.extend([dissmissed_login_A, active_login_A])
    return downtimers


@pytest.fixture()
def cleaned_downtimers(active_login_A):
    downtimers = model_pb2.NamespaceSpec.AlertingSettings.JugglerRawDowntimers()
    downtimers.staff_logins.extend([active_login_A])
    return downtimers


@pytest.fixture()
def downtimers_only_active(active_login_A, active_login_B):
    downtimers = model_pb2.NamespaceSpec.AlertingSettings.JugglerRawDowntimers()
    downtimers.staff_logins.extend([active_login_A, active_login_B])
    return downtimers


@pytest.fixture()
def notify_rules_with_dissmissed(dissmissed_login_B, active_login_B):
    rules = model_pb2.NamespaceSpec.AlertingSettings.JugglerRawNotifyRules()
    platform_rule = rules.platform.add()
    platform_rule.template_name = "on_status_change"
    platform_rule.template_kwargs = yaml.dump({"login": [dissmissed_login_B]})

    platform_rule = rules.platform.add()
    platform_rule.template_name = "phone_escalation"
    platform_rule.template_kwargs = yaml.dump({"logins": [dissmissed_login_B, active_login_B]})

    platform_rule = rules.platform.add()
    platform_rule.template_name = "phone_escalation"
    platform_rule.template_kwargs = yaml.dump({"logins": [dissmissed_login_B, active_login_B]})

    balancer_rule = rules.balancer.add()
    balancer_rule.template_name = "on_status_change"
    balancer_rule.template_kwargs = yaml.dump({"login": [dissmissed_login_B, active_login_B]})

    balancer_rule = rules.balancer.add()
    balancer_rule.template_name = "phone_escalation"
    balancer_rule.template_kwargs = yaml.dump({"logins": [dissmissed_login_B, active_login_B]})
    return rules


@pytest.fixture()
def cleaned_notify_rules(active_login_B):
    rules = model_pb2.NamespaceSpec.AlertingSettings.JugglerRawNotifyRules()
    platform_rule = rules.platform.add()
    platform_rule.template_name = "on_status_change"
    platform_rule.template_kwargs = yaml.dump({"login": []})

    platform_rule = rules.platform.add()
    platform_rule.template_name = "phone_escalation"
    platform_rule.template_kwargs = yaml.dump({"logins": [active_login_B]})

    platform_rule = rules.platform.add()
    platform_rule.template_name = "phone_escalation"
    platform_rule.template_kwargs = yaml.dump({"logins": [active_login_B]})

    balancer_rule = rules.balancer.add()
    balancer_rule.template_name = "on_status_change"
    balancer_rule.template_kwargs = yaml.dump({"login": [active_login_B]})

    balancer_rule = rules.balancer.add()
    balancer_rule.template_name = "phone_escalation"
    balancer_rule.template_kwargs = yaml.dump({"logins": [active_login_B]})
    return rules


@pytest.fixture()
def notify_rules_only_active(active_login_A, active_login_B):
    rules = model_pb2.NamespaceSpec.AlertingSettings.JugglerRawNotifyRules()
    platform_rule = rules.platform.add()
    platform_rule.template_name = "on_status_change"
    platform_rule.template_kwargs = yaml.dump({"login": [active_login_A]})

    platform_rule = rules.platform.add()
    platform_rule.template_name = "phone_escalation"
    platform_rule.template_kwargs = yaml.dump({"logins": [active_login_A, active_login_B]})

    balancer_rule = rules.balancer.add()
    balancer_rule.template_name = "phone_escalation"
    balancer_rule.template_kwargs = yaml.dump({"logins": [active_login_B]})

    balancer_rule = rules.balancer.add()
    balancer_rule.template_name = "phone_escalation"
    balancer_rule.template_kwargs = yaml.dump({"logins": [active_login_A, active_login_B]})
    return rules


@pytest.fixture()
def ns_with_dismissed_downtimers(notify_rules_only_active, downtimers_with_dissmissed):
    ns = model_pb2.Namespace()
    ns.meta.id = "ns_with_dismissed_downtimers"
    ns.spec.alerting.juggler_raw_notify_rules.CopyFrom(notify_rules_only_active)
    ns.spec.alerting.juggler_raw_downtimers.CopyFrom(downtimers_with_dissmissed)
    return ns


@pytest.fixture()
def ns_with_dismissed_notified(notify_rules_with_dissmissed, downtimers_only_active):
    ns = model_pb2.Namespace()
    ns.meta.id = "ns_with_dismissed_notified"
    ns.spec.alerting.juggler_raw_notify_rules.CopyFrom(notify_rules_with_dissmissed)
    ns.spec.alerting.juggler_raw_downtimers.CopyFrom(downtimers_only_active)
    return ns


@pytest.fixture()
def ns_with_dismissed(notify_rules_with_dissmissed, downtimers_with_dissmissed):
    ns = model_pb2.Namespace()
    ns.meta.id = "ns_with_dismissed"
    ns.spec.alerting.juggler_raw_notify_rules.CopyFrom(notify_rules_with_dissmissed)
    ns.spec.alerting.juggler_raw_downtimers.CopyFrom(downtimers_with_dissmissed)
    return ns


@pytest.fixture()
def ns_without_alerting():
    ns = model_pb2.Namespace()
    ns.meta.id = "ns_without_alerting"
    return ns


@pytest.fixture()
def ns_only_active(notify_rules_only_active, downtimers_only_active):
    ns = model_pb2.Namespace()
    ns.meta.id = "ns_only_active"
    ns.spec.alerting.juggler_raw_notify_rules.CopyFrom(notify_rules_only_active)
    ns.spec.alerting.juggler_raw_downtimers.CopyFrom(downtimers_only_active)
    return ns


@pytest.fixture()
def staff_client(monkeypatch, dissmissed_login_A, dissmissed_login_B):
    client = mock.Mock()
    client.filter_dismissed.return_value = [{"login": dissmissed_login_A}, {"login": dissmissed_login_B}]
    monkeypatch.setattr(dismissedcleaner.DismissedCleaner, '_staff_client', client)


def test_dismissed_cleaner(staff_client, ns_with_dismissed_downtimers,
                           ns_with_dismissed_notified,
                           ns_with_dismissed,
                           ns_without_alerting,
                           ns_only_active, zk_storage, cache,
                           dissmissed_login_A,
                           dissmissed_login_B,
                           cleaned_downtimers,
                           downtimers_only_active,
                           cleaned_notify_rules,
                           notify_rules_only_active,):
    for ns in ns_with_dismissed_downtimers, ns_with_dismissed_notified, ns_with_dismissed, ns_without_alerting, ns_only_active:
        zk_storage.create_namespace(ns.meta.id, ns)

    assert wait_until(lambda: len(cache.list_all_namespaces()) == 5, timeout=1)
    dcleaner = dismissedcleaner.DismissedCleaner(op_log=mock.MagicMock(), metrics_registry=mock.MagicMock())
    dcleaner.process()
    gevent.sleep(1)

    def get_ns_in_cache(ns):
        return cache.must_get_namespace(ns.meta.id)

    def get_downtimers(ns):
        return ns.spec.alerting.juggler_raw_downtimers

    def get_notify_rules(ns):
        return ns.spec.alerting.juggler_raw_notify_rules

    ns = get_ns_in_cache(ns_with_dismissed_downtimers)
    assert get_downtimers(ns) == cleaned_downtimers
    assert get_notify_rules(ns) == notify_rules_only_active

    ns = get_ns_in_cache(ns_with_dismissed_notified)
    assert get_downtimers(ns) == downtimers_only_active
    assert get_notify_rules(ns) == cleaned_notify_rules

    ns = get_ns_in_cache(ns_with_dismissed)
    assert get_downtimers(ns) == cleaned_downtimers
    assert get_notify_rules(ns) == cleaned_notify_rules

    ns = get_ns_in_cache(ns_without_alerting)
    assert ns == ns_without_alerting
    # check that ns was not updated
    assert ns.meta.generation == 0

    ns = get_ns_in_cache(ns_only_active)
    assert ns == ns_only_active
    assert ns.meta.generation == 0

    logs = [call.args[0] for call in dcleaner.op_log.info.call_args_list]
    assert any(("%s was removed from downtimers in" % dissmissed_login_A) in log_mes for log_mes in logs)
    assert any(("%s was removed from notification rules in" % dissmissed_login_B) in log_mes for log_mes in logs)
