default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


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


function get_private_cert_path(name, default_private_cert_dir)
  default_private_cert_dir = default_private_cert_dir or "/dev/shm/balancer/priv"
  return (private_cert_dir or default_private_cert_dir) .. "/" .. name;
end


function get_public_cert_path(name, default_public_cert_dir)
  default_public_cert_dir = default_public_cert_dir or "/dev/shm/balancer"
  return (public_cert_dir or default_public_cert_dir) .. "/" .. name;
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
  addrs = {
    {
      ip = "127.0.0.4";
      port = get_port_var("port");
    };
    {
      ip = "*";
      port = 443;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = get_port_var("port");
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
        admin = {};
      }; -- http
    }; -- admin
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        get_port_var("port");
      }; -- ports
      report = {
        uuid = "service_total";
        ranges = get_str_var("default_ranges");
        just_storage = true;
        disable_robotness = true;
        disable_sslness = true;
        events = {
          stats = "report";
        }; -- events
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          errordocument = {
            status = 204;
            force_conn_close = false;
          }; -- errordocument
        }; -- http
      }; -- report
    }; -- stats_storage
    https_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        443;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 443, "/place/db/www/logs");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("pumpkin.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-pumpkin.yandex.ru.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.pumpkin.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.pumpkin.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.pumpkin.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- default
          }; -- contexts
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 443, "/place/db/www/logs");
              report = {
                uuid = "https";
                refers = "service_total";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                shared = {
                  uuid = "modules";
                }; -- shared
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
    http_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        80;
        get_port_var("port");
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
              uuid = "http";
              refers = "service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              shared = {
                uuid = "modules";
                regexp = {
                  ["awacs-balancer-health-check"] = {
                    priority = 5;
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
                  slb_ping = {
                    priority = 4;
                    match_and = {
                      {
                        match_fsm = {
                          host = "pumpkin\\.yandex\\.net(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      };
                      {
                        match_fsm = {
                          URI = "/ping";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      };
                    }; -- match_and
                    stats_eater = {
                      balancer2 = {
                        unique_policy = {};
                        attempts = 1;
                        rr = {
                          weights_file = "./controls/slb_check.weights";
                          to_upstream = {
                            weight = 1.000;
                            errordocument = {
                              status = 200;
                              force_conn_close = false;
                            }; -- errordocument
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
                    }; -- stats_eater
                  }; -- slb_ping
                  kubr = {
                    priority = 3;
                    match_fsm = {
                      host = "pumpkin\\.yandex\\.ru(:\\d+)?";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    report = {
                      uuid = "kubr";
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
                          kubr_man = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_kubr_to_man";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                watermark_policy = {
                                  lo = 0.100;
                                  hi = 0.100;
                                  params_file = "./controls/watermark_policy.params_file";
                                  unique_policy = {};
                                }; -- watermark_policy
                                attempts = 4;
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
                                    { "lite01i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6100:92e2:baff:fe56:e910"; };
                                    { "lite02i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6110:f652:14ff:fe8b:f880"; };
                                    { "lite03i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6105:e61d:2dff:fe00:8cf0"; };
                                    { "lite04i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6104:e61d:2dff:fe01:fb60"; };
                                    { "lite05i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6107:f652:14ff:fef5:d4d0"; };
                                    { "lite06i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6109:f652:14ff:fef5:d5c0"; };
                                    { "lite08i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6101:e61d:2dff:fe03:36c0"; };
                                    { "lite09i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6106:f652:14ff:fe74:3930"; };
                                    { "lite10i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6103:f652:14ff:fef5:c840"; };
                                    { "lite11i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610b:f652:14ff:fe74:3c00"; };
                                    { "lite12i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6108:e61d:2dff:fe03:3740"; };
                                    { "man1-3946.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610f:92e2:baff:fea1:7862"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "1s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- report
                          }; -- kubr_man
                          kubr_sas = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_kubr_to_sas";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                watermark_policy = {
                                  lo = 0.100;
                                  hi = 0.100;
                                  params_file = "./controls/watermark_policy.params_file";
                                  unique_policy = {};
                                }; -- watermark_policy
                                attempts = 4;
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
                                    { "lite01h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b02:922b:34ff:fecf:236e"; };
                                    { "lite02h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b01:922b:34ff:fecf:2c28"; };
                                    { "lite03h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b04:922b:34ff:fecf:2b68"; };
                                    { "lite04h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b0f:922b:34ff:fecf:2aee"; };
                                    { "lite05h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b06:922b:34ff:fecf:28b0"; };
                                    { "lite06h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b10:922b:34ff:fecf:3b22"; };
                                    { "lite07h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b10:922b:34ff:fecf:3aca"; };
                                    { "lite08h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b02:922b:34ff:fecf:27dd"; };
                                    { "lite09h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:172:922b:34ff:fecf:3c32"; };
                                    { "lite10h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b06:922b:34ff:fecf:2bd0"; };
                                    { "lite11h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b09:922b:34ff:fecc:7a66"; };
                                    { "lite12h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b11:922b:34ff:fecf:3338"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "1s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- report
                          }; -- kubr_sas
                          kubr_vla = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_kubr_to_vla";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                watermark_policy = {
                                  lo = 0.100;
                                  hi = 0.100;
                                  params_file = "./controls/watermark_policy.params_file";
                                  unique_policy = {};
                                }; -- watermark_policy
                                attempts = 4;
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
                                    { "vla1-4617.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:70:0:614:db7:a43c"; };
                                    { "vla1-4637.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:9e:0:614:db7:a15b"; };
                                    { "vla1-4658.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:84:0:614:db7:a02a"; };
                                    { "vla1-4659.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:83:0:614:db7:a024"; };
                                    { "vla1-4665.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:93:0:614:db7:a854"; };
                                    { "vla1-4668.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:50:0:614:db7:a476"; };
                                    { "vla1-4670.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:4d:0:614:db7:a46e"; };
                                    { "vla1-4672.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:4c:0:614:db7:a474"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "1s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- report
                          }; -- kubr_vla
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- report
                  }; -- kubr
                  comtr = {
                    priority = 2;
                    match_fsm = {
                      host = "pumpkin\\.yandex\\.com.tr(:\\d+)?";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    report = {
                      uuid = "comtr";
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
                          comtr_man = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_comtr_to_man";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                watermark_policy = {
                                  lo = 0.100;
                                  hi = 0.100;
                                  params_file = "./controls/watermark_policy.params_file";
                                  unique_policy = {};
                                }; -- watermark_policy
                                attempts = 4;
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
                                    { "lite13i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6104:e61d:2dff:fe00:9140"; };
                                    { "lite14i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6107:f652:14ff:fef5:d780"; };
                                    { "lite15i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6109:e61d:2dff:fe03:4290"; };
                                    { "lite16i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6101:92e2:baff:fe55:f3d0"; };
                                    { "lite17i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6105:e61d:2dff:fe00:92a0"; };
                                    { "lite18i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610b:e61d:2dff:fe03:52b0"; };
                                    { "lite19i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6108:f652:14ff:fef5:ce70"; };
                                    { "lite20i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610d:e61d:2dff:fe01:ecd0"; };
                                    { "lite21i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610c:e61d:2dff:fe03:37e0"; };
                                    { "lite22i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6102:e61d:2dff:fe03:37d0"; };
                                    { "lite23i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6106:e61d:2dff:fe03:4870"; };
                                    { "lite24i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610a:e61d:2dff:fe03:3dd0"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "1s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- report
                          }; -- comtr_man
                          comtr_sas = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_comtr_to_sas";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                watermark_policy = {
                                  lo = 0.100;
                                  hi = 0.100;
                                  params_file = "./controls/watermark_policy.params_file";
                                  unique_policy = {};
                                }; -- watermark_policy
                                attempts = 4;
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
                                    { "lite13h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b02:922b:34ff:fecf:3cb8"; };
                                    { "lite14h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:172:922b:34ff:fecf:2372"; };
                                    { "lite15h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b06:922b:34ff:fecf:2672"; };
                                    { "lite16h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b00:922b:34ff:fecf:3a94"; };
                                    { "lite17h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b0b:922b:34ff:fecf:2c30"; };
                                    { "lite18h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b09:922b:34ff:fecf:2fee"; };
                                    { "lite19h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b11:922b:34ff:fecf:3ae8"; };
                                    { "lite20h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b06:922b:34ff:fecf:22c8"; };
                                    { "lite21h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b13:922b:34ff:fecf:34bc"; };
                                    { "lite22h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b0e:922b:34ff:fecf:2a56"; };
                                    { "lite23h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b13:922b:34ff:fecf:2872"; };
                                    { "lite24h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:61a:922b:34ff:fecf:3d60"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "1s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- report
                          }; -- comtr_sas
                          comtr_vla = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_comtr_to_vla";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                watermark_policy = {
                                  lo = 0.100;
                                  hi = 0.100;
                                  params_file = "./controls/watermark_policy.params_file";
                                  unique_policy = {};
                                }; -- watermark_policy
                                attempts = 4;
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
                                    { "vla1-4677.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:3f:0:614:db7:9f58"; };
                                    { "vla1-4680.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:46:0:614:db7:9f4a"; };
                                    { "vla1-4681.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:3c:0:614:db7:9f52"; };
                                    { "vla1-4686.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:39:0:614:db7:a46c"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "1s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- report
                          }; -- comtr_vla
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- report
                  }; -- comtr
                  default = {
                    priority = 1;
                    errordocument = {
                      status = 404;
                      content = "Unknown host";
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- default
                }; -- regexp
              }; -- shared
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
  }; -- ipdispatch
}