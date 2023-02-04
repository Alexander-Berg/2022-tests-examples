default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


function _call_func_providers(overridable_func_names)
  for _, func_name in pairs(overridable_func_names) do
    local func_provider_path = _G[func_name .. "_provider"]
    if func_provider_path ~= nil then
      local env = {}
      setmetatable(env, {__index = _G})
      local provider, err = loadfile(func_provider_path, nil, env)
      if provider == nil then
        error(string.format('Failed to import provider "%s": %s', func_provider_path, err))
      end
      ok, rv = pcall(provider)
      if ok then
        if type(rv) ~= 'function' then
          error(string.format('Provider "%s" must return a function, not %s.', func_provider_path, type(rv)))
        end
        _G["do_" .. func_name] = rv
      else
        error(string.format('Provider "%s" failed: %s', func_provider_path, rv))
      end
    end
  end
end


function gen_proxy_backends(backends, proxy_options)
  local result = {}

  for index, backend in pairs(backends) do
    local proxy = {
      host = backend[1] or backend['host'];
      port = backend[2] or backend['port'];
      cached_ip = backend[4] or backend['cached_ip'];
    };

    if proxy_options ~= nil then
      for optname, optvalue in pairs(proxy_options) do
        proxy[optname] = optvalue
      end
    end

    result[index] = {
      weight = backend[3] or backend['weight'];
      proxy = proxy;
    };
  end

  if next(result) == nil then
    error("backends list is empty")
  end

  return result
end


function get_ca_cert_path(name, default_ca_cert_dir)
  default_ca_cert_dir = default_ca_cert_dir or "/dev/shm/balancer/priv"
  return (ca_cert_dir or default_ca_cert_dir) .. "/" .. name;
end


function check_int(value, var_name)
    return tonumber(value) or error("Could not cast variable \"" .. var_name .. "\" to a number.'")
end

function get_int_var(name, default)
  value = _G[name]
  return value and check_int(value) or default
end


function get_log_path(name, port, default_log_dir)
  default_log_dir = default_log_dir or "/place/db/www/logs"
  rv = (log_dir or default_log_dir) .. "/current-" .. name .. "-balancer";
  if port ~= nil then
    rv = rv .. "-" .. port;
  end
  return rv
end


function get_port_var(name, offset, default)
  value = get_int_var(name, default)
  if value == nil then
    error("Neither port variable \"" .. name .. "\" nor default port is specified.")
  end
  if value < 0 or value > 65535 then
    error("Variable \"" .. name .. "\" is not a valid port: " .. value)
  end
  if offset ~= nil then
    value = value + offset
  end
  return value
end


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_var(name, default)
  return _G[name] or default
end


function do_get_workers()
  -- actual get_workers() implementation, can be overridden
  value = _G["workers"]
  if value == nil then
    error('Variable "workers" is not specified.')
  end
  int_value = tonumber(value)
  if int_value == nil then
    error('Could not cast variable "workers" to a number.')
  end
  return int_value
end


function get_workers()
  value = do_get_workers()
  if type(value) ~= 'number' then
    error(string.format('Provided get_workers() implementation must return a number, not %s.', type(value)))
  end
  if value < 0 or value % 1 ~= 0 then
    error(string.format('Provided get_workers() implementation must return a non-negative integer, not %s', value))
  end
  return value
end


_call_func_providers({
  "get_workers";
})


instance = {
  buffer = 65536;
  maxconn = 5000;
  tcp_fastopen = 0;
  tcp_listen_queue = 128;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", get_port_var("port"), "/place/db/www/logs");
  addrs = {
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  admin_addrs = {
    {
      ip = "127.0.0.1";
      port = get_port_var("port");
    };
    {
      ip = "::1";
      port = get_port_var("port");
    };
  }; -- admin_addrs
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
  }; -- unistat
  sd = {
    client_name = "awacs-l7-balancer(namespace-id:balancer)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
  }; -- sd
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        admin = {
          disable_xml_stats = true;
        }; -- admin
      }; -- http
    }; -- admin
    http_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        80;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 80, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 80, "/place/db/www/logs");
            report = {
              uuid = "service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              regexp = {
                payments = {
                  priority = 1;
                  match_fsm = {
                    path = "(/.*)?";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  report = {
                    uuid = "payments";
                    ranges = get_str_var("default_ranges");
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    headers = {
                      create = {
                        Host = "payments-test.mail.yandex.net";
                      }; -- create
                      balancer2 = {
                        unique_policy = {};
                        attempts = 2;
                        rr = {
                          weights_file = "./controls/traffic_control.weights";
                          byenvpayments_qloud = {
                            weight = 1.000;
                            report = {
                              uuid = "payments_requests_to_qloud";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
                                connection_attempts = 2;
                                active = {
                                  delay = "3s";
                                  request = "GET /ping HTTP/1.1\nHost: payments-test.mail.yandex.net\nUser-Agent: awacs\n\n";
                                  unpack(gen_proxy_backends({
                                    { "payments-test.stable.qloud-b.yandex.net"; 443; 1.000; "2a02:6b8:0:3400:0:2e5:1:80d2"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "70ms";
                                    backend_timeout = "15s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 100;
                                    need_resolve = true;
                                    https_settings = {
                                      ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                      ca_file = get_ca_cert_path("allCAs.pem", "./");
                                      sni_on = true;
                                      verify_depth = 3;
                                    }; -- https_settings
                                  }))
                                }; -- active
                              }; -- balancer2
                            }; -- report
                          }; -- byenvpayments_qloud
                          byenvpayments_deploy = {
                            weight = 1.000;
                            report = {
                              uuid = "payments_requests_to_deploy";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 2;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  bygeo_man = {
                                    weight = 1.000;
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      sd = {
                                        endpoint_sets = {
                                          {
                                            cluster_name = "man";
                                            endpoint_set_id = "oplata-test-stage.payments";
                                          };
                                        }; -- endpoint_sets
                                        proxy_options = {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "70ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 100;
                                          need_resolve = false;
                                        }; -- proxy_options
                                        active = {
                                          delay = "3s";
                                          request = "GET /ping HTTP/1.1\nHost: oplata.test.billing.yandex.net\nUser-Agent: awacs\n\n";
                                        }; -- active
                                      }; -- sd
                                    }; -- balancer2
                                  }; -- bygeo_man
                                  bygeo_sas = {
                                    weight = 1.000;
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      sd = {
                                        endpoint_sets = {
                                          {
                                            cluster_name = "sas";
                                            endpoint_set_id = "oplata-test-stage.payments";
                                          };
                                        }; -- endpoint_sets
                                        proxy_options = {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "70ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 100;
                                          need_resolve = false;
                                        }; -- proxy_options
                                        active = {
                                          delay = "3s";
                                          request = "GET /ping HTTP/1.1\nHost: oplata.test.billing.yandex.net\nUser-Agent: awacs\n\n";
                                        }; -- active
                                      }; -- sd
                                    }; -- balancer2
                                  }; -- bygeo_sas
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- byenvpayments_deploy
                        }; -- rr
                      }; -- balancer2
                    }; -- headers
                  }; -- report
                }; -- payments
              }; -- regexp
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
  }; -- ipdispatch
}