import mock
import six

from awacs.wrappers.base import wrap, ValidationCtx
from awacs.wrappers.l7macro import L7MacroHttpSettings, L7MacroHttpsSettings, L7MacroCompat, L7MacroMonitoringSettings
from awtest.wrappers import get_wrapped_validation_exception_msg, parse_lua_into_pb
from infra.awacs.proto import modules_pb2


def test_l7_macro_expand():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.version = '0.0.1'
    pb.compat.disable_unistat = True
    pb.compat.disable_sd = True
    pb.compat.maxconn.value = 1111
    pb.compat.disable_tcp_listen_queue_limit = True

    pb.http.ports.append(4444)
    pb.http.compat.bind_on_instance_port = True
    pb.http.compat.use_instance_port_in_section_log_name = True
    pb.http.compat.assign_shared_uuid = 'modules'

    pb.https.ports.append(5555)
    pb.https.compat.enable_sslv3 = True
    pb.https.compat.place_first = True
    pb.https.compat.refer_shared_uuid = 'modules'
    pb.https.certs.add(id='xxx', secondary_id='yyy')

    pb.monitoring.enable_total_signals = True

    action_pb = pb.headers.add()
    action_pb.create.target = 'Test'
    action_pb.create.value = '123'
    action_pb = pb.headers.add()
    action_pb.copy.source = 'Test'
    action_pb.copy.target = 'Test-2'

    action_pb = pb.response_headers.add()
    action_pb.create.target = 'Test'
    action_pb.create.value = '123'
    action_pb = pb.response_headers.add()
    action_pb.copy.source = 'Test'
    action_pb.copy.target = 'Test-2'

    m = wrap(holder_pb)
    m.expand_immediate_contained_macro()

    expected_holder_pb = modules_pb2.Holder()
    parse_lua_into_pb('''instance_macro {
  maxconn: 1111
  f_workers {
    type: GET_WORKERS
    get_workers_params {
    }
  }
  sections {
    key: "admin"
    value {
      ips {
        value: "127.0.0.1"
      }
      ips {
        value: "::1"
      }
      ports {
        f_value {
          type: GET_PORT_VAR
          get_port_var_params {
            var: "port"
          }
        }
      }
      nested {
        http {
          nested {
            admin {
            }
          }
        }
      }
    }
  }
  sections {
    key: "stats_storage"
    value {
      ips {
        value: "127.0.0.4"
      }
      ports {
        f_value {
          type: GET_PORT_VAR
          get_port_var_params {
            var: "port"
          }
        }
      }
      nested {
        report {
          uuid: "service_total"
          ranges: "default"
          just_storage: true
          nested {
            http {
              nested {
                errordocument {
                  status: 204
                }
              }
            }
          }
        }
      }
    }
  }
  sections {
    key: "https_section"
    value {
      ips {
        value: "*"
      }
      ports {
        value: 5555
      }
      nested {
        extended_http_macro {
          enable_ssl: true
          ssl_sni_contexts {
            key: "xxx"
            value {
              servername_regexp: "default"
              c_cert {
                id: "xxx"
              }
              c_secondary_cert {
                id: "yyy"
              }
            }
          }
          maxlen: 65536
          maxreq: 65536
          report_uuid: "https"
          nested {
            shared {
              uuid: "modules"
            }
          }
        }
      }
    }
  }
  sections {
    key: "http_section"
    value {
      ips {
        value: "*"
      }
      ports {
        f_value {
          type: GET_PORT_VAR
          get_port_var_params {
            var: "port"
          }
        }
      }
      ports {
        value: 4444
      }
      nested {
        extended_http_macro {
          f_port {
            type: GET_PORT_VAR
            get_port_var_params {
              var: "port"
            }
          }
          maxlen: 65536
          maxreq: 65536
          report_uuid: "http"
          nested {
            shared {
              uuid: "modules"
              nested {
                modules {
                  headers {
                    create {
                      key: "Test"
                      value: "123"
                    }
                  }
                }
                modules {
                  headers {
                    copy {
                      key: "Test"
                      value: "Test-2"
                    }
                  }
                }
                modules {
                  response_headers {
                    copy {
                      key: "Test"
                      value: "Test-2"
                    }
                  }
                }
                modules {
                  response_headers {
                    create {
                      key: "Test"
                      value: "123"
                    }
                  }
                }
                modules {
                  regexp {
                    include_upstreams {
                      filter {
                        any: true
                      }
                      order {
                        label {
                          name: "order"
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}''', expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


def test_l7_macro_expand_2():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.version = '0.0.1'
    pb.http.ports.append(80)
    pb.http.redirect_to_https.SetInParent()
    pb.http.redirect_to_https.compat.replaced_upstream_id = 'test-http-to-https'
    pb.https.ports.append(443)
    pb.https.certs.add(id='test-cert')
    pb.announce_check_reply.url_re = '/ping'
    pb.announce_check_reply.compat.replaced_upstream_id = 'test-slbping'
    pb.health_check_reply.SetInParent()
    pb.health_check_reply.compat.replaced_upstream_id = 'test-awacs-balancer-health-check'
    m = wrap(holder_pb)
    m.expand_immediate_contained_macro()

    expected_holder_pb = modules_pb2.Holder()
    parse_lua_into_pb('''instance_macro {
  maxconn: 5000
  f_workers {
    type: GET_WORKERS
    get_workers_params {
    }
  }
  sections {
    key: "admin"
    value {
      ips {
        value: "127.0.0.1"
      }
      ips {
        value: "::1"
      }
      ports {
        f_value {
          type: GET_PORT_VAR
          get_port_var_params {
            var: "port"
          }
        }
      }
      nested {
        http {
          nested {
            admin {
            }
          }
        }
      }
    }
  }
  sections {
    key: "http_section"
    value {
      ips {
        value: "*"
      }
      ports {
        value: 80
      }
      nested {
        extended_http_macro {
          maxlen: 65536
          maxreq: 65536
          nested {
            regexp {
              sections {
                key: "test-awacs-balancer-health-check"
                value {
                  matcher {
                    match_fsm {
                      uri: "/awacs-balancer-health-check"
                    }
                  }
                  nested {
                    errordocument {
                      status: 200
                    }
                  }
                }
              }
              sections {
                key: "test-slbping"
                value {
                  matcher {
                    match_fsm {
                      url: "/ping"
                    }
                  }
                  nested {
                    slb_ping_macro {
                      errordoc: true
                    }
                  }
                }
              }
              sections {
                key: "test-http-to-https"
                value {
                  matcher {
                  }
                  nested {
                    http_to_https_macro {
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  sections {
    key: "https_section"
    value {
      ips {
        value: "*"
      }
      ports {
        value: 443
      }
      nested {
        extended_http_macro {
          enable_ssl: true
          ssl_sni_contexts {
            key: "test-cert"
            value {
              servername_regexp: "default"
              c_cert {
                id: "test-cert"
              }
            }
          }
          disable_sslv3: true
          maxlen: 65536
          maxreq: 65536
          nested {
            modules {
              regexp {
                include_upstreams {
                  filter {
                    not {
                      ids: "test-awacs-balancer-health-check"
                      ids: "test-http-to-https"
                      ids: "test-slbping"
                    }
                  }
                  order {
                    label {
                      name: "order"
                    }
                  }
                }
                prepend_sections {
                  key: "test-awacs-balancer-health-check"
                  value {
                    matcher {
                      match_fsm {
                        uri: "/awacs-balancer-health-check"
                      }
                    }
                    nested {
                      errordocument {
                        status: 200
                      }
                    }
                  }
                }
                prepend_sections {
                  key: "test-slbping"
                  value {
                    matcher {
                      match_fsm {
                        url: "/ping"
                      }
                    }
                    nested {
                      slb_ping_macro {
                        errordoc: true
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  tcp_listen_queue: 128
  unistat {
  }
  sd {
  }
}''', expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


def test_l7_macro_core_limits():
    pb = modules_pb2.L7Macro(version=u'0.3.0')
    pb.http.SetInParent()
    pb.compat.maxlen.value = 100000
    pb.compat.maxreq.value = 100000

    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.core.limits.req_line_plus_headers_max_len.value = 100000
    e = get_wrapped_validation_exception_msg(pb)
    assert e == (u'compat.maxlen and core.limits.req_line_plus_headers_max_len '
                 u'must not be used together. Please use core.limits.req_line_plus_headers_max_len')
    pb.compat.ClearField('maxlen')

    pb.core.limits.req_line_max_len.value = 100000
    e = get_wrapped_validation_exception_msg(pb)
    assert e == (u'compat.maxreq and core.limits.req_line_max_len '
                 u'must not be used together. Please use core.limits.req_line_max_len')
    pb.compat.ClearField('maxreq')

    e = get_wrapped_validation_exception_msg(pb)
    assert e == (u'core -> limits -> req_line_plus_headers_max_len: '
                 u'must be greater than req_line_max_len by at least 512 bytes')
    pb.core.limits.req_line_plus_headers_max_len.value = 111111

    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.core.limits.ClearField('req_line_plus_headers_max_len')
    e = get_wrapped_validation_exception_msg(pb)
    assert e == (u'core -> limits: either all or none of the '
                 u'"req_line_max_len", "req_line_plus_headers_max_len" must be specified')


def test_l7_macro_https_validate():
    pb = modules_pb2.L7Macro.HttpsSettings()
    pb.certs.add()
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'certs[0] -> id: is required'

    pb.certs[0].id = u'test'
    pb.certs.add(id=u'second-crt')
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'certs: must not contain more than one cert'

    del pb.certs[1:]
    pb.compat.bind_on_instance_port = False
    pb.compat.use_instance_port_in_section_log_name = True
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'compat: use_instance_port_in_section_log_name can not be set without bind_on_instance_port'

    pb.compat.bind_on_instance_port = True
    e = get_wrapped_validation_exception_msg(pb)
    assert not e


def test_l7_macro_validate():
    pb = modules_pb2.L7Macro()
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'version: is required'

    pb.version = '0.0.1'
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'at least one of the "http", "https" must be specified'

    pb.http.SetInParent()
    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.https.SetInParent()
    pb.https.certs.add(id=u'test')
    pb.monitoring.SetInParent()
    pb.compat.SetInParent()

    with mock.patch.object(L7MacroHttpSettings, 'validate') as http_validate, \
        mock.patch.object(L7MacroHttpsSettings, 'validate') as https_validate, \
        mock.patch.object(L7MacroCompat, 'validate') as compat_validate, \
        mock.patch.object(L7MacroMonitoringSettings, 'validate') as monitoring_validate:  # noqa
        e = get_wrapped_validation_exception_msg(pb)
        assert not e

        assert http_validate.called
        assert https_validate.called
        assert compat_validate.called
        assert monitoring_validate.called

        pb.http.compat.bind_on_instance_port = True
        pb.https.compat.bind_on_instance_port = True
        assert get_wrapped_validation_exception_msg(pb) == u'compat.bind_on_instance_port can not be set for both http and https'

        pb.http.compat.bind_on_instance_port = False
        pb.http.compat.assign_shared_uuid = u'modules'
        pb.https.compat.assign_shared_uuid = u'modules'
        e = get_wrapped_validation_exception_msg(pb)
        assert e == (u'compat.assign_shared_uuid can not be set to the same value for both '
                     u'http and https')

        pb.http.compat.assign_shared_uuid = u'http-modules'
        pb.https.compat.refer_shared_uuid = u'https-modules'
        e = get_wrapped_validation_exception_msg(pb)
        assert e == u'https: compat.refer_shared_uuid does not exist'

        pb.http.compat.assign_shared_uuid = u'http-modules'
        pb.https.compat.refer_shared_uuid = u'http-modules'
        e = get_wrapped_validation_exception_msg(pb)
        assert not e

        pb.version = u'xxx'
        e = get_wrapped_validation_exception_msg(pb)
        assert e == u'version: is not a valid version'

        pb.version = u'0.0.123'
        e = get_wrapped_validation_exception_msg(pb)
        assert e == u'version: is not supported'

        pb.version = u'0.0.2'
        e = get_wrapped_validation_exception_msg(pb)
        assert not e

    pb = modules_pb2.L7Macro()
    pb.version = '0.0.1'
    pb.http.SetInParent()
    pb.monitoring.enable_announce_check_signals = True
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'announce_check_reply: must be set to use monitoring.enable_announce_check_signals'

    pb.announce_check_reply.url_re = u'/ping'
    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.http2.fraction.value = 0.5
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'http2: can not be configured without "http.enable_http2" or "https.enable_http2"'

    pb.health_check_reply.SetInParent()
    pb.include_domains.SetInParent()
    pb.https.SetInParent()
    ctx = ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_BALANCER)
    e = get_wrapped_validation_exception_msg(pb, ctx=ctx)
    assert e == u'http2: can not be configured without "http.enable_http2" or "https.enable_http2"'

    pb.https.enable_http2 = True
    e = get_wrapped_validation_exception_msg(pb, ctx=ctx)
    assert not e

    pb.http.enable_http2 = True
    e = get_wrapped_validation_exception_msg(pb, ctx=ctx)
    assert e == u'http2 -> fraction: can not be used if "http.enable_http2" is set'

    pb.http2.ClearField('fraction')
    e = get_wrapped_validation_exception_msg(pb, ctx=ctx)
    assert not e


def test_l7_macro_validate_redirect_to_https():
    pb = modules_pb2.L7Macro()
    pb.version = '0.0.1'
    pb.http.ports.append(80)
    pb.http.redirect_to_https.SetInParent()
    pb.https.ports.append(443)
    pb.https.certs.add(id='test-cert')
    e = get_wrapped_validation_exception_msg(pb)
    assert e == ('http -> redirect_to_https: can only be used together with '
                 '"announce_check_reply" and "health_check_reply"')

    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.ClearField('https')
    e = get_wrapped_validation_exception_msg(pb)
    assert e == ('http -> redirect_to_https: can only be used together with "https"')


def test_l7_macro_include_upstreams():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.ports.append(80)
    pb.http.redirect_to_https.SetInParent()
    pb.http.redirect_to_https.compat.replaced_upstream_id = 'test-http-to-https'
    pb.https.ports.append(443)
    pb.https.certs.add(id='test-cert')
    pb.announce_check_reply.url_re = '/ping'
    pb.announce_check_reply.compat.replaced_upstream_id = 'test-slbping'
    pb.health_check_reply.SetInParent()
    pb.health_check_reply.compat.replaced_upstream_id = 'test-awacs-balancer-health-check'
    m = wrap(pb)  # type: L7Macro

    assert m.includes_upstreams()
    assert m.include_upstreams
    ns_id = 'ns-id'
    available_upstream_full_ids = [
        (ns_id, 'default'),
        (ns_id, 'test-http-to-https'),
        (ns_id, 'admin'),
        (ns_id, 'test-slbping'),
        (ns_id, 'test-awacs-balancer-health-check'),
    ]
    actual_included_upstream_full_ids = m.include_upstreams.get_included_upstream_ids(
        'ns-id', available_upstream_full_ids)
    expected_included_upstream_full_ids = {
        (ns_id, 'default'),
        (ns_id, 'admin'),
    }
    assert actual_included_upstream_full_ids == expected_included_upstream_full_ids

    m = wrap(pb)  # type: L7Macro
    pb.http.redirect_to_https.compat.replaced_upstream_id = ''
    pb.announce_check_reply.compat.replaced_upstream_id = ''
    pb.health_check_reply.compat.replaced_upstream_id = ''
    actual_included_upstream_full_ids = m.include_upstreams.get_included_upstream_ids(
        'ns-id', available_upstream_full_ids)
    expected_included_upstream_full_ids = set(available_upstream_full_ids)
    assert actual_included_upstream_full_ids == expected_included_upstream_full_ids
