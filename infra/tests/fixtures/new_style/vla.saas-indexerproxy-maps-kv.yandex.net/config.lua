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


function get_geo(name, default_geo)
  default_geo = default_geo or "random"
  return name .. (DC or default_geo);
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
  maxconn = 10000;
  tcp_fastopen = 0;
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
  addrs = {
    {
      ip = "*";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
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
        get_port_var("port");
        80;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", get_port_var("port"), "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", get_port_var("port"), "/place/db/www/logs");
            report = {
              uuid = "service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              headers = {
                create_func = {
                  ["X-Forwarded-For-Y"] = "realip";
                  ["X-Req-Id"] = "reqid";
                  ["X-Source-Port-Y"] = "realport";
                  ["X-Start-Time"] = "starttime";
                }; -- create_func
                response_headers = {
                  create_weak = {
                    ["X-Content-Type-Options"] = "nosniff";
                    ["X-XSS-Protection"] = "1; mode=block";
                  }; -- create_weak
                  regexp = {
                    ["awacs-balancer-health-check"] = {
                      priority = 3;
                      match_fsm = {
                        URI = "/awacs-balancer-health-check";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      errordocument = {
                        status = 200;
                        force_conn_close = false;
                      }; -- errordocument
                    }; -- ["awacs-balancer-health-check"]
                    slbping = {
                      priority = 2;
                      match_fsm = {
                        URI = "/ping";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      balancer2 = {
                        unique_policy = {};
                        attempts = 1;
                        rr = {
                          weights_file = "./controls/slb_check.weights";
                          to_upstream = {
                            weight = 1.000;
                            balancer2 = {
                              by_name_policy = {
                                name = get_geo("bygeo_", "random");
                                unique_policy = {};
                              }; -- by_name_policy
                              attempts = 1;
                              rr = {
                                bygeo_man = {
                                  weight = 1.000;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 6;
                                    rr = {
                                      unpack(gen_proxy_backends({
                                        { "man1-2251.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:dd40"; };
                                        { "man1-5203.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:604d:e61d:2dff:fe01:ebf0"; };
                                        { "man2-0466.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6095:92e2:baff:fea3:7496"; };
                                        { "man2-1653.search.yandex.net"; 29783; 120.000; "2a02:6b8:c01:72f:0:604:b2a9:9322"; };
                                        { "man2-1656.search.yandex.net"; 29783; 120.000; "2a02:6b8:c01:721:0:604:b2a9:73fa"; };
                                        { "man2-7191.search.yandex.net"; 29783; 120.000; "2a02:6b8:c01:653:0:604:14a9:69b7"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "55ms";
                                        backend_timeout = "300ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- rr
                                  }; -- balancer2
                                }; -- bygeo_man
                                bygeo_sas = {
                                  weight = 1.000;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 6;
                                    rr = {
                                      unpack(gen_proxy_backends({
                                        { "sas1-4407.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                        { "sas1-6972.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:123:215:b2ff:fea7:b390"; };
                                        { "sas1-7589.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:129:215:b2ff:fea7:ba00"; };
                                        { "sas1-9084.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:143:feaa:14ff:fede:41ae"; };
                                        { "sas1-9251.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:698:428d:5cff:fef5:b336"; };
                                        { "sas1-9313.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:69a:428d:5cff:fef4:91dd"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "55ms";
                                        backend_timeout = "300ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- rr
                                  }; -- balancer2
                                }; -- bygeo_sas
                                bygeo_vla = {
                                  weight = 1.000;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 6;
                                    rr = {
                                      unpack(gen_proxy_backends({
                                        { "vla1-0128.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:3f:0:604:db7:a10b"; };
                                        { "vla1-0243.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:9e:0:604:db7:a8a0"; };
                                        { "vla1-1825.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:88:0:604:db7:a7b5"; };
                                        { "vla1-2079.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:1b:0:604:db7:98fb"; };
                                        { "vla2-1026.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:409:0:604:4b02:758a"; };
                                        { "vla2-1043.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:337:0:604:4b14:595a"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "55ms";
                                        backend_timeout = "300ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- rr
                                  }; -- balancer2
                                }; -- bygeo_vla
                              }; -- rr
                            }; -- balancer2
                          }; -- to_upstream
                          switch_off = {
                            weight = -1.000;
                            errordocument = {
                              status = 503;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- switch_off
                        }; -- rr
                      }; -- balancer2
                    }; -- slbping
                    default = {
                      priority = 1;
                      shared = {
                        uuid = "backends";
                        balancer2 = {
                          by_name_policy = {
                            name = get_geo("bygeo_", "random");
                            unique_policy = {};
                          }; -- by_name_policy
                          attempts = 1;
                          rr = {
                            bygeo_man = {
                              weight = 1.000;
                              balancer2 = {
                                timeout_policy = {
                                  timeout = "10s";
                                  unique_policy = {};
                                }; -- timeout_policy
                                attempts = 5;
                                weighted2 = {
                                  slow_reply_time = "1s";
                                  correction_params = {
                                    max_weight = 5.000;
                                    min_weight = 0.050;
                                    history_time = "100s";
                                    feedback_time = "300s";
                                    plus_diff_per_sec = 0.050;
                                    minus_diff_per_sec = 0.100;
                                  }; -- correction_params
                                  unpack(gen_proxy_backends({
                                    { "man1-2251.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:dd40"; };
                                    { "man1-5203.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:604d:e61d:2dff:fe01:ebf0"; };
                                    { "man2-0466.search.yandex.net"; 29783; 120.000; "2a02:6b8:b000:6095:92e2:baff:fea3:7496"; };
                                    { "man2-1653.search.yandex.net"; 29783; 120.000; "2a02:6b8:c01:72f:0:604:b2a9:9322"; };
                                    { "man2-1656.search.yandex.net"; 29783; 120.000; "2a02:6b8:c01:721:0:604:b2a9:73fa"; };
                                    { "man2-7191.search.yandex.net"; 29783; 120.000; "2a02:6b8:c01:653:0:604:14a9:69b7"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "55ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- bygeo_man
                            bygeo_sas = {
                              weight = 1.000;
                              balancer2 = {
                                timeout_policy = {
                                  timeout = "10s";
                                  unique_policy = {};
                                }; -- timeout_policy
                                attempts = 5;
                                weighted2 = {
                                  slow_reply_time = "1s";
                                  correction_params = {
                                    max_weight = 5.000;
                                    min_weight = 0.050;
                                    history_time = "100s";
                                    feedback_time = "300s";
                                    plus_diff_per_sec = 0.050;
                                    minus_diff_per_sec = 0.100;
                                  }; -- correction_params
                                  unpack(gen_proxy_backends({
                                    { "sas1-4407.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:646:96de:80ff:fe8c:d864"; };
                                    { "sas1-6972.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:123:215:b2ff:fea7:b390"; };
                                    { "sas1-7589.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:129:215:b2ff:fea7:ba00"; };
                                    { "sas1-9084.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:143:feaa:14ff:fede:41ae"; };
                                    { "sas1-9251.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:698:428d:5cff:fef5:b336"; };
                                    { "sas1-9313.search.yandex.net"; 15790; 120.000; "2a02:6b8:b000:69a:428d:5cff:fef4:91dd"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "55ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- bygeo_sas
                            bygeo_vla = {
                              weight = 1.000;
                              balancer2 = {
                                timeout_policy = {
                                  timeout = "10s";
                                  unique_policy = {};
                                }; -- timeout_policy
                                attempts = 5;
                                weighted2 = {
                                  slow_reply_time = "1s";
                                  correction_params = {
                                    max_weight = 5.000;
                                    min_weight = 0.050;
                                    history_time = "100s";
                                    feedback_time = "300s";
                                    plus_diff_per_sec = 0.050;
                                    minus_diff_per_sec = 0.100;
                                  }; -- correction_params
                                  unpack(gen_proxy_backends({
                                    { "vla1-0128.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:3f:0:604:db7:a10b"; };
                                    { "vla1-0243.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:9e:0:604:db7:a8a0"; };
                                    { "vla1-1825.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:88:0:604:db7:a7b5"; };
                                    { "vla1-2079.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:1b:0:604:db7:98fb"; };
                                    { "vla2-1026.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:409:0:604:4b02:758a"; };
                                    { "vla2-1043.search.yandex.net"; 16975; 120.000; "2a02:6b8:c0e:337:0:604:4b14:595a"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "55ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- bygeo_vla
                          }; -- rr
                          on_error = {
                            errordocument = {
                              status = 504;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- on_error
                        }; -- balancer2
                      }; -- shared
                    }; -- default
                  }; -- regexp
                }; -- response_headers
              }; -- headers
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
  }; -- ipdispatch
}