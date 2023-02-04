from infra.awacs.proto import modules_pb2
from awacs import yamlparser
from awacs.lib.yamlparser.wrappers_util import dump_uem_pb
from infra.awacs.tools.awacsemtool2.rules import uem_simplified


BY_DC_HARD_MODE = '''---
regexp_section:
  matcher: {}
  modules:
    - shared:
        uuid: backends
    - headers:
        create_func:
          X-Location: location
          X-URL: url
          X-Source-Port-Y: realport
          X-Start-Time: starttime
          X-Scheme: scheme
        create_func_weak:
          X-Req-Id: reqid
          X-Forwarded-For: realip
    - balancer2:
        attempts: 1
        rr: {}
        by_name_policy:
          name: !f get_geo("bygeo_", "random")
          unique_policy: {}
        backends:
          - weight: 1
            name: bygeo_man
            modules:
              - balancer2:
                  attempts: 3
                  connection_attempts: 3
                  attempts_rate_limiter:
                    limit: 1
                  weighted2: {}
                  generated_proxy_backends:
                    proxy_options:
                      fail_on_5xx: false
                    include_backends:
                      type: BY_ID
                      ids: [httpbin]
          - weight: 1
            name: bygeo_sas
            modules:
              - balancer2:
                  attempts: 3
                  connection_attempts: 3
                  attempts_rate_limiter:
                    limit: 1
                  weighted2: {}
                  generated_proxy_backends:
                    proxy_options:
                      fail_on_5xx: false
                    include_backends:
                      type: BY_ID
                      ids: [httpbin]
          - weight: 1
            name: bygeo_vla
            modules:
              - balancer2:
                  attempts: 3
                  connection_attempts: 3
                  attempts_rate_limiter:
                    limit: 1
                  weighted2: {}
                  generated_proxy_backends:
                    proxy_options:
                      fail_on_5xx: false
                    include_backends:
                      type: BY_ID
                      ids: [httpbin]'''

BY_DC_EXPECTED_EASY_MODE = '''l7_upstream_macro:
  compat:
    disable_monitoring: true
  version: 0.0.1
  id: u-id
  can_handle_announce_checks: true
  matcher:
    any: true
  headers:
    - create: {target: X-Location, func: location}
    - create: {target: X-Scheme, func: scheme}
    - create: {target: X-Source-Port-Y, func: realport}
    - create: {target: X-Start-Time, func: starttime}
    - create: {target: X-URL, func: url}
    - create: {target: X-Forwarded-For, keep_existing: true, func: realip}
    - create: {target: X-Req-Id, keep_existing: true, func: reqid}
  by_dc_scheme:
    compat:
      disable_devnull: true
    dc_balancer:
      compat:
        disable_dynamic_weights: true
      weights_section_id: bygeo
      method: LOCAL_THEN_BY_DC_WEIGHT
      attempts: 1
    balancer:
      compat:
        method: WEIGHTED2
      attempts: 3
      max_reattempts_share: 1
      fast_attempts: 3
      do_not_retry_http_responses: true
      backend_timeout: 10s
    dcs:
      - name: man
        backend_ids:
          - httpbin
      - name: sas
        backend_ids:
          - httpbin
      - name: vla
        backend_ids:
          - httpbin
    on_error:
      rst: true
'''


def test_basics():
    c = uem_simplified.Checker('ns-id', 'balancer-id')
    holder_pb = yamlparser.parse(modules_pb2.Holder, BY_DC_HARD_MODE)
    ok, msg, suggested_config_pb = c.suggest('u-id', holder_pb)
    assert ok
    assert dump_uem_pb(suggested_config_pb) == BY_DC_EXPECTED_EASY_MODE
