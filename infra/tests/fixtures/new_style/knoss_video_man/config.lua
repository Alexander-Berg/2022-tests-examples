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


function get_str_env_var(name, default)
  rv = os.getenv(name)
  if rv == nil then
    if default == nil then
      error(string.format('Environment variable "%s" is not set.', name))
    else
      return default
    end
  else
    return rv
  end
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
  state_directory = "/dev/shm/balancer-state";
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
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 81;
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
              uuid = "service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              regexp = {
                ["awacs-balancer-health-check"] = {
                  priority = 7;
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
                ["video-xml"] = {
                  priority = 6;
                  match_fsm = {
                    URI = "/video-xml(/.*)?";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "7202579568527170768";
                    report = {
                      uuid = "video-xml";
                      ranges = get_str_var("default_ranges");
                      just_storage = false;
                      disable_robotness = true;
                      disable_sslness = true;
                      events = {
                        stats = "report";
                      }; -- events
                      request_replier = {
                        sink = {
                          shared = {
                            uuid = "6168121074701619930";
                            balancer2 = {
                              simple_policy = {};
                              attempts = 2;
                              connection_attempts = 3;
                              rr = {
                                unpack(gen_proxy_backends({
                                  { "sinkadm.priemka.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::eeee:20"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "10s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- rr
                              attempts_rate_limiter = {
                                limit = 0.200;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- shared
                        }; -- sink
                        enable_failed_requests_replication = false;
                        rate = 0.000;
                        rate_file = "./controls/request_replier_video-xml.ratefile";
                        headers_hasher = {
                          header_name = "X-Yandex-LogstatUID";
                          surround = false;
                          randomize_empty_match = true;
                          balancer2 = {
                            by_name_from_header_policy = {
                              allow_zero_weights = true;
                              strict = true;
                              hints = {
                                {
                                  hint = "man";
                                  backend = "video_man";
                                };
                                {
                                  hint = "sas";
                                  backend = "video_sas";
                                };
                                {
                                  hint = "vla";
                                  backend = "video_vla";
                                };
                              }; -- hints
                              by_hash_policy = {
                                unique_policy = {};
                              }; -- by_hash_policy
                            }; -- by_name_from_header_policy
                            attempts = 2;
                            attempts_file = "./controls/video-xml.attempts";
                            connection_attempts = 3;
                            rr = {
                              weights_file = "./controls/search_l7_balancer_switch.json";
                              video_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "video-xml_requests_to_vla";
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
                                    rr = {
                                      weights_file = "./controls/dynamic_balancing_switch";
                                      dynamic_balancing_enabled = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "video-xml_requests_to_vla_dynamic";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "1191218453291331145";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              attempts_file = "./controls/attempts.count";
                                              connection_attempts = 2;
                                              dynamic = {
                                                max_pessimized_share = 0.100;
                                                min_pessimization_coeff = 0.100;
                                                weight_increase_step = 0.100;
                                                history_interval = "10s";
                                                backends_name = "video_vla";
                                                unpack(gen_proxy_backends({
                                                  { "vla1-0213.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                                                  { "vla1-0557.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                                                  { "vla1-0762.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                                                  { "vla1-0852.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                                                  { "vla1-1073.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                                                  { "vla1-1167.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                                                  { "vla1-1206.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                                                  { "vla1-1245.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                                                  { "vla1-1266.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                                                  { "vla1-1412.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                                                  { "vla1-1426.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                                                  { "vla1-1454.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                                                  { "vla1-1484.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                                                  { "vla1-1497.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                                                  { "vla1-1508.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                                                  { "vla1-1527.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                                                  { "vla1-1530.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                                                  { "vla1-1540.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                                                  { "vla1-1556.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                                                  { "vla1-1568.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                                                  { "vla1-1571.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                                                  { "vla1-1575.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                                                  { "vla1-1636.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                                                  { "vla1-1643.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                                                  { "vla1-1650.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                                                  { "vla1-1654.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                                                  { "vla1-1744.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                                                  { "vla1-1785.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                                                  { "vla1-1862.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                                                  { "vla1-1869.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                                                  { "vla1-1889.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                                                  { "vla1-1892.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a421"; };
                                                  { "vla1-1902.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                                                  { "vla1-1920.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                                                  { "vla1-1937.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                                                  { "vla1-2006.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                                                  { "vla1-2011.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                                                  { "vla1-2035.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                                                  { "vla1-2039.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                                                  { "vla1-2097.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                                                  { "vla1-2131.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                                                  { "vla1-2141.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                                                  { "vla1-2153.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                                                  { "vla1-2209.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                                                  { "vla1-2338.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                                                  { "vla1-2341.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                                                  { "vla1-2363.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                                                  { "vla1-2368.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                                                  { "vla1-2392.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                                                  { "vla1-2440.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                                                  { "vla1-2759.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                                                  { "vla1-2901.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                                                  { "vla1-3477.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                                                  { "vla1-4120.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                                                  { "vla1-4143.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "10s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = true;
                                                }))
                                              }; -- dynamic
                                              attempts_rate_limiter = {
                                                limit = 0.100;
                                                coeff = 0.990;
                                                switch_default = true;
                                              }; -- attempts_rate_limiter
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- report
                                      }; -- dynamic_balancing_enabled
                                      dynamic_balancing_disabled = {
                                        weight = 1.000;
                                        shared = {
                                          uuid = "3365650464778765030";
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            attempts_file = "./controls/attempts.count";
                                            connection_attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "vla1-0213.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                                                { "vla1-0557.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                                                { "vla1-0762.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                                                { "vla1-0852.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                                                { "vla1-1073.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                                                { "vla1-1167.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                                                { "vla1-1206.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                                                { "vla1-1245.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                                                { "vla1-1266.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                                                { "vla1-1412.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                                                { "vla1-1426.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                                                { "vla1-1454.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                                                { "vla1-1484.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                                                { "vla1-1497.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                                                { "vla1-1508.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                                                { "vla1-1527.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                                                { "vla1-1530.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                                                { "vla1-1540.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                                                { "vla1-1556.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                                                { "vla1-1568.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                                                { "vla1-1571.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                                                { "vla1-1575.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                                                { "vla1-1636.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                                                { "vla1-1643.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                                                { "vla1-1650.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                                                { "vla1-1654.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                                                { "vla1-1744.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                                                { "vla1-1785.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                                                { "vla1-1862.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                                                { "vla1-1869.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                                                { "vla1-1889.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                                                { "vla1-1892.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a421"; };
                                                { "vla1-1902.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                                                { "vla1-1920.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                                                { "vla1-1937.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                                                { "vla1-2006.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                                                { "vla1-2011.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                                                { "vla1-2035.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                                                { "vla1-2039.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                                                { "vla1-2097.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                                                { "vla1-2131.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                                                { "vla1-2141.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                                                { "vla1-2153.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                                                { "vla1-2209.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                                                { "vla1-2338.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                                                { "vla1-2341.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                                                { "vla1-2363.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                                                { "vla1-2368.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                                                { "vla1-2392.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                                                { "vla1-2440.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                                                { "vla1-2759.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                                                { "vla1-2901.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                                                { "vla1-3477.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                                                { "vla1-4120.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                                                { "vla1-4143.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                            attempts_rate_limiter = {
                                              limit = 0.100;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- dynamic_balancing_disabled
                                    }; -- rr
                                  }; -- balancer2
                                }; -- report
                              }; -- video_vla
                              video_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "video-xml_requests_to_sas";
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
                                    rr = {
                                      weights_file = "./controls/dynamic_balancing_switch";
                                      dynamic_balancing_enabled = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "video-xml_requests_to_sas_dynamic";
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
                                            attempts_file = "./controls/attempts.count";
                                            connection_attempts = 2;
                                            dynamic = {
                                              max_pessimized_share = 0.100;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "video_sas#video_vla";
                                              unpack(gen_proxy_backends({
                                                { "sas1-6052.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:117:215:b2ff:fea7:7730"; };
                                                { "sas1-6069.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:169:215:b2ff:fea7:74b8"; };
                                                { "sas1-6113.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11c:215:b2ff:fea7:7958"; };
                                                { "sas1-6140.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a7:215:b2ff:fea7:7a58"; };
                                                { "sas1-6207.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:197:215:b2ff:fea7:7988"; };
                                                { "sas1-6240.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                                                { "sas1-6475.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a8:215:b2ff:fea7:7edc"; };
                                                { "sas1-6539.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                                                { "sas1-6600.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:110:215:b2ff:fea7:7ec8"; };
                                                { "sas1-6626.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                                                { "sas1-6631.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:119:215:b2ff:fea7:7acc"; };
                                                { "sas1-6634.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                                                { "sas1-6730.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                                                { "sas1-6750.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                                                { "sas1-6785.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                                                { "sas1-6805.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                                                { "sas1-6851.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                                                { "sas1-6869.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                                                { "sas1-6938.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                                                { "sas1-7005.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                                                { "sas1-7013.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                                                { "sas1-7138.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                                                { "sas1-7142.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                                                { "sas1-7192.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                                                { "sas1-7234.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                                                { "sas1-7481.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8c0c"; };
                                                { "sas1-7534.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                                                { "sas1-7569.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                                                { "sas1-7574.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                                                { "sas1-7591.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                                                { "sas1-7747.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                                                { "sas1-7761.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                                                { "sas1-7790.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                                                { "sas1-7803.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                                                { "sas1-7816.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                                                { "sas1-7824.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                                                { "sas1-7902.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                                                { "sas1-7908.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                                                { "sas1-7925.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:50dd"; };
                                                { "sas1-7926.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:51cb"; };
                                                { "sas1-7931.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:618:215:b2ff:fea7:a9f0"; };
                                                { "sas1-7940.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:51c5"; };
                                                { "sas1-7942.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                                                { "sas1-8179.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                                                { "sas1-8214.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                                                { "sas1-8236.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                                                { "sas1-8246.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                                                { "sas1-8299.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                                                { "sas1-8369.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                                                { "sas1-8401.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                                                { "sas1-8546.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                                                { "sas1-8571.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                                                { "sas1-8612.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                                                { "sas1-8618.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                                                { "sas1-8635.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                                                { "sas1-8830.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                                                { "sas1-8932.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                                                { "sas1-8960.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                                                { "sas1-9134.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:629:feaa:14ff:fede:3f94"; };
                                                { "vla1-0213.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                                                { "vla1-0557.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                                                { "vla1-0762.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                                                { "vla1-0852.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                                                { "vla1-1073.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                                                { "vla1-1167.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                                                { "vla1-1206.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                                                { "vla1-1245.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                                                { "vla1-1266.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                                                { "vla1-1412.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                                                { "vla1-1426.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                                                { "vla1-1454.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                                                { "vla1-1484.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                                                { "vla1-1497.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                                                { "vla1-1508.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                                                { "vla1-1527.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                                                { "vla1-1530.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                                                { "vla1-1540.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                                                { "vla1-1556.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                                                { "vla1-1568.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                                                { "vla1-1571.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                                                { "vla1-1575.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                                                { "vla1-1636.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                                                { "vla1-1643.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                                                { "vla1-1650.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                                                { "vla1-1654.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                                                { "vla1-1744.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                                                { "vla1-1785.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                                                { "vla1-1862.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                                                { "vla1-1869.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                                                { "vla1-1889.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                                                { "vla1-1892.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:71:0:604:db7:a421"; };
                                                { "vla1-1902.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                                                { "vla1-1920.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                                                { "vla1-1937.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                                                { "vla1-2006.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                                                { "vla1-2011.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                                                { "vla1-2035.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                                                { "vla1-2039.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                                                { "vla1-2097.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                                                { "vla1-2131.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                                                { "vla1-2141.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                                                { "vla1-2153.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                                                { "vla1-2209.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                                                { "vla1-2338.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                                                { "vla1-2341.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                                                { "vla1-2363.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                                                { "vla1-2368.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                                                { "vla1-2392.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                                                { "vla1-2440.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                                                { "vla1-2759.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                                                { "vla1-2901.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                                                { "vla1-3477.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                                                { "vla1-4120.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                                                { "vla1-4143.search.yandex.net"; 8080; 346.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.100;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- dynamic_balancing_enabled
                                      dynamic_balancing_disabled = {
                                        weight = 1.000;
                                        shared = {
                                          uuid = "3292089584546115172";
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            attempts_file = "./controls/attempts.count";
                                            connection_attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "sas1-6052.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:117:215:b2ff:fea7:7730"; };
                                                { "sas1-6069.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:169:215:b2ff:fea7:74b8"; };
                                                { "sas1-6113.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11c:215:b2ff:fea7:7958"; };
                                                { "sas1-6140.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a7:215:b2ff:fea7:7a58"; };
                                                { "sas1-6207.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:197:215:b2ff:fea7:7988"; };
                                                { "sas1-6240.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                                                { "sas1-6475.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a8:215:b2ff:fea7:7edc"; };
                                                { "sas1-6539.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                                                { "sas1-6600.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:110:215:b2ff:fea7:7ec8"; };
                                                { "sas1-6626.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                                                { "sas1-6631.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:119:215:b2ff:fea7:7acc"; };
                                                { "sas1-6634.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                                                { "sas1-6730.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                                                { "sas1-6750.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                                                { "sas1-6785.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                                                { "sas1-6805.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                                                { "sas1-6851.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                                                { "sas1-6869.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                                                { "sas1-6938.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                                                { "sas1-7005.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                                                { "sas1-7013.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                                                { "sas1-7138.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                                                { "sas1-7142.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                                                { "sas1-7192.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                                                { "sas1-7234.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                                                { "sas1-7481.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8c0c"; };
                                                { "sas1-7534.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                                                { "sas1-7569.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                                                { "sas1-7574.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                                                { "sas1-7591.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                                                { "sas1-7747.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                                                { "sas1-7761.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                                                { "sas1-7790.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                                                { "sas1-7803.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                                                { "sas1-7816.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                                                { "sas1-7824.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                                                { "sas1-7902.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                                                { "sas1-7908.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                                                { "sas1-7925.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:50dd"; };
                                                { "sas1-7926.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:51cb"; };
                                                { "sas1-7931.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:618:215:b2ff:fea7:a9f0"; };
                                                { "sas1-7940.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:51c5"; };
                                                { "sas1-7942.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                                                { "sas1-8179.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                                                { "sas1-8214.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                                                { "sas1-8236.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                                                { "sas1-8246.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                                                { "sas1-8299.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                                                { "sas1-8369.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                                                { "sas1-8401.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                                                { "sas1-8546.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                                                { "sas1-8571.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                                                { "sas1-8612.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                                                { "sas1-8618.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                                                { "sas1-8635.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                                                { "sas1-8830.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                                                { "sas1-8932.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                                                { "sas1-8960.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                                                { "sas1-9134.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:629:feaa:14ff:fede:3f94"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                            attempts_rate_limiter = {
                                              limit = 0.100;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- dynamic_balancing_disabled
                                    }; -- rr
                                  }; -- balancer2
                                }; -- report
                              }; -- video_sas
                              video_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "video-xml_requests_to_man";
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
                                    rr = {
                                      weights_file = "./controls/dynamic_balancing_switch";
                                      dynamic_balancing_enabled = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "video-xml_requests_to_man_dynamic";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "7505417920268842326";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              attempts_file = "./controls/attempts.count";
                                              connection_attempts = 2;
                                              dynamic = {
                                                max_pessimized_share = 0.100;
                                                min_pessimization_coeff = 0.100;
                                                weight_increase_step = 0.100;
                                                history_interval = "10s";
                                                backends_name = "video_man";
                                                unpack(gen_proxy_backends({
                                                  { "man1-1282.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                                                  { "man1-1296.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                                                  { "man1-1947.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                                                  { "man1-2848.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                                                  { "man1-2960.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                                                  { "man1-4051.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                                                  { "man1-4426.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                                                  { "man1-4461.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                                                  { "man1-4489.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                                                  { "man1-4497.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                                                  { "man1-4525.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                                                  { "man1-4548.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                                                  { "man1-7451.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                                                  { "man1-9282.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6085:92e2:baff:fea3:72dc"; };
                                                  { "man1-9558.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:616:0:604:baa0:b2bc"; };
                                                  { "man2-1704.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:616:0:604:baa0:b2da"; };
                                                  { "man2-1705.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:614:0:604:baa0:b260"; };
                                                  { "man2-1745.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:614:0:604:baa0:b2c4"; };
                                                  { "man2-1768.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:614:0:604:baa0:b00e"; };
                                                  { "man2-4866.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:631:0:604:1465:ca36"; };
                                                  { "man2-4891.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:632:0:604:1465:ca7e"; };
                                                  { "man2-4958.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:637:0:604:baa1:728c"; };
                                                  { "man2-4965.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:636:0:604:baa1:7804"; };
                                                  { "man2-5012.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63f:0:604:14a7:67ba"; };
                                                  { "man2-5150.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63a:0:604:2d6d:3c70"; };
                                                  { "man2-5164.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63a:0:604:2d6d:3a90"; };
                                                  { "man2-5330.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:83c:0:604:14a7:bc47"; };
                                                  { "man2-5390.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:834:0:604:baa1:7d94"; };
                                                  { "man2-5393.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:833:0:604:baa1:72d6"; };
                                                  { "man2-5426.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:634:0:604:2d6d:1b20"; };
                                                  { "man2-5455.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63a:0:604:2d6d:3bd0"; };
                                                  { "man2-5481.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:634:0:604:2d6d:1790"; };
                                                  { "man2-5501.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:638:0:604:2d6d:c30"; };
                                                  { "man2-5517.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:637:0:604:baa1:7928"; };
                                                  { "man2-5615.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:633:0:604:14a7:67b7"; };
                                                  { "man2-5664.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:636:0:604:baa1:7d1e"; };
                                                  { "man2-5670.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:637:0:604:baa1:725e"; };
                                                  { "man2-5672.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:636:0:604:baa1:7af2"; };
                                                  { "man2-5678.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:638:0:604:2d6d:3160"; };
                                                  { "man2-5684.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:635:0:604:2d6d:18c0"; };
                                                  { "man2-5696.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65b:0:604:2d6d:3930"; };
                                                  { "man2-5700.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65a:0:604:2d6d:2fc0"; };
                                                  { "man2-5712.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:635:0:604:2d6d:2f80"; };
                                                  { "man2-5717.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:663:0:604:2d6d:38d0"; };
                                                  { "man2-5732.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:638:0:604:2d6d:1a40"; };
                                                  { "man2-5761.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:639:0:604:baa1:7e56"; };
                                                  { "man2-5791.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:831:0:604:baa1:7a68"; };
                                                  { "man2-5811.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63d:0:604:baa0:b244"; };
                                                  { "man2-5827.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63d:0:604:baa0:b22c"; };
                                                  { "man2-5831.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:834:0:604:baa1:7cf8"; };
                                                  { "man2-5855.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65b:0:604:2d6d:3030"; };
                                                  { "man2-5857.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:833:0:604:baa1:7aa6"; };
                                                  { "man2-5887.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:834:0:604:baa1:7c66"; };
                                                  { "man2-5889.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:832:0:604:baa1:777e"; };
                                                  { "man2-5898.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:832:0:604:baa1:7aae"; };
                                                  { "man2-5936.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:639:0:604:baa1:72b6"; };
                                                  { "man2-5944.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:663:0:604:2d6d:3a10"; };
                                                  { "man2-5984.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65a:0:604:2d6d:900"; };
                                                  { "man2-5996.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:663:0:604:2d6d:fa0"; };
                                                  { "man2-6001.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65a:0:604:2d6d:cc0"; };
                                                  { "man2-6018.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:639:0:604:baa1:7c46"; };
                                                  { "man2-6028.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65b:0:604:2d6d:ae0"; };
                                                  { "man2-6037.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:832:0:604:baa1:7c30"; };
                                                  { "man2-6056.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63d:0:604:baa0:b0a6"; };
                                                  { "man2-6061.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:833:0:604:baa1:7b2e"; };
                                                  { "man2-6064.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:635:0:604:2d6d:3150"; };
                                                  { "man2-6070.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63c:0:604:baa1:7866"; };
                                                  { "man2-6076.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63c:0:604:baa1:7d56"; };
                                                  { "man2-6160.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:630:0:604:1465:ce29"; };
                                                  { "man2-6174.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:654:0:604:baa1:743e"; };
                                                  { "man2-6209.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:654:0:604:baa1:7710"; };
                                                  { "man2-6233.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:657:0:604:1465:ca57"; };
                                                  { "man2-6274.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63c:0:604:baa1:7d62"; };
                                                  { "man2-6307.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:831:0:604:baa1:7db0"; };
                                                  { "man2-6310.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:831:0:604:baa1:7dac"; };
                                                  { "man2-6387.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:641:0:604:14a7:67fe"; };
                                                  { "man2-6420.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:654:0:604:baa1:73f6"; };
                                                  { "man2-6623.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:64c:0:604:14a7:bc2c"; };
                                                  { "man2-6857.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:644:0:604:14a7:baa5"; };
                                                  { "man2-7026.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:64c:0:604:14a7:b9c2"; };
                                                  { "man2-7087.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:634:0:604:baa2:3e32"; };
                                                  { "man2-7363.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:641:0:604:14a7:67fb"; };
                                                  { "man2-7411.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:64c:0:604:14a7:bc2a"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "10s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = true;
                                                }))
                                              }; -- dynamic
                                              attempts_rate_limiter = {
                                                limit = 0.100;
                                                coeff = 0.990;
                                                switch_default = true;
                                              }; -- attempts_rate_limiter
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- report
                                      }; -- dynamic_balancing_enabled
                                      dynamic_balancing_disabled = {
                                        weight = 1.000;
                                        shared = {
                                          uuid = "7232277778704636898";
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            attempts_file = "./controls/attempts.count";
                                            connection_attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "man1-1282.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                                                { "man1-1296.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                                                { "man1-1947.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                                                { "man1-2848.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                                                { "man1-2960.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                                                { "man1-4051.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                                                { "man1-4426.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                                                { "man1-4461.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                                                { "man1-4489.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                                                { "man1-4497.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                                                { "man1-4525.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                                                { "man1-4548.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                                                { "man1-7451.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                                                { "man1-9282.search.yandex.net"; 8080; 288.000; "2a02:6b8:b000:6085:92e2:baff:fea3:72dc"; };
                                                { "man1-9558.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:616:0:604:baa0:b2bc"; };
                                                { "man2-1704.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:616:0:604:baa0:b2da"; };
                                                { "man2-1705.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:614:0:604:baa0:b260"; };
                                                { "man2-1745.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:614:0:604:baa0:b2c4"; };
                                                { "man2-1768.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:614:0:604:baa0:b00e"; };
                                                { "man2-4866.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:631:0:604:1465:ca36"; };
                                                { "man2-4891.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:632:0:604:1465:ca7e"; };
                                                { "man2-4958.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:637:0:604:baa1:728c"; };
                                                { "man2-4965.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:636:0:604:baa1:7804"; };
                                                { "man2-5012.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63f:0:604:14a7:67ba"; };
                                                { "man2-5150.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63a:0:604:2d6d:3c70"; };
                                                { "man2-5164.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63a:0:604:2d6d:3a90"; };
                                                { "man2-5330.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:83c:0:604:14a7:bc47"; };
                                                { "man2-5390.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:834:0:604:baa1:7d94"; };
                                                { "man2-5393.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:833:0:604:baa1:72d6"; };
                                                { "man2-5426.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:634:0:604:2d6d:1b20"; };
                                                { "man2-5455.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63a:0:604:2d6d:3bd0"; };
                                                { "man2-5481.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:634:0:604:2d6d:1790"; };
                                                { "man2-5501.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:638:0:604:2d6d:c30"; };
                                                { "man2-5517.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:637:0:604:baa1:7928"; };
                                                { "man2-5615.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:633:0:604:14a7:67b7"; };
                                                { "man2-5664.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:636:0:604:baa1:7d1e"; };
                                                { "man2-5670.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:637:0:604:baa1:725e"; };
                                                { "man2-5672.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:636:0:604:baa1:7af2"; };
                                                { "man2-5678.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:638:0:604:2d6d:3160"; };
                                                { "man2-5684.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:635:0:604:2d6d:18c0"; };
                                                { "man2-5696.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65b:0:604:2d6d:3930"; };
                                                { "man2-5700.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65a:0:604:2d6d:2fc0"; };
                                                { "man2-5712.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:635:0:604:2d6d:2f80"; };
                                                { "man2-5717.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:663:0:604:2d6d:38d0"; };
                                                { "man2-5732.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:638:0:604:2d6d:1a40"; };
                                                { "man2-5761.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:639:0:604:baa1:7e56"; };
                                                { "man2-5791.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:831:0:604:baa1:7a68"; };
                                                { "man2-5811.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63d:0:604:baa0:b244"; };
                                                { "man2-5827.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63d:0:604:baa0:b22c"; };
                                                { "man2-5831.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:834:0:604:baa1:7cf8"; };
                                                { "man2-5855.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65b:0:604:2d6d:3030"; };
                                                { "man2-5857.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:833:0:604:baa1:7aa6"; };
                                                { "man2-5887.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:834:0:604:baa1:7c66"; };
                                                { "man2-5889.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:832:0:604:baa1:777e"; };
                                                { "man2-5898.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:832:0:604:baa1:7aae"; };
                                                { "man2-5936.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:639:0:604:baa1:72b6"; };
                                                { "man2-5944.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:663:0:604:2d6d:3a10"; };
                                                { "man2-5984.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65a:0:604:2d6d:900"; };
                                                { "man2-5996.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:663:0:604:2d6d:fa0"; };
                                                { "man2-6001.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65a:0:604:2d6d:cc0"; };
                                                { "man2-6018.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:639:0:604:baa1:7c46"; };
                                                { "man2-6028.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:65b:0:604:2d6d:ae0"; };
                                                { "man2-6037.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:832:0:604:baa1:7c30"; };
                                                { "man2-6056.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63d:0:604:baa0:b0a6"; };
                                                { "man2-6061.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:833:0:604:baa1:7b2e"; };
                                                { "man2-6064.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:635:0:604:2d6d:3150"; };
                                                { "man2-6070.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63c:0:604:baa1:7866"; };
                                                { "man2-6076.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63c:0:604:baa1:7d56"; };
                                                { "man2-6160.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:630:0:604:1465:ce29"; };
                                                { "man2-6174.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:654:0:604:baa1:743e"; };
                                                { "man2-6209.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:654:0:604:baa1:7710"; };
                                                { "man2-6233.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:657:0:604:1465:ca57"; };
                                                { "man2-6274.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:63c:0:604:baa1:7d62"; };
                                                { "man2-6307.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:831:0:604:baa1:7db0"; };
                                                { "man2-6310.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:831:0:604:baa1:7dac"; };
                                                { "man2-6387.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:641:0:604:14a7:67fe"; };
                                                { "man2-6420.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:654:0:604:baa1:73f6"; };
                                                { "man2-6623.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:64c:0:604:14a7:bc2c"; };
                                                { "man2-6857.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:644:0:604:14a7:baa5"; };
                                                { "man2-7026.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:64c:0:604:14a7:b9c2"; };
                                                { "man2-7087.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:634:0:604:baa2:3e32"; };
                                                { "man2-7363.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:641:0:604:14a7:67fb"; };
                                                { "man2-7411.search.yandex.net"; 8080; 288.000; "2a02:6b8:c01:64c:0:604:14a7:bc2a"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                            attempts_rate_limiter = {
                                              limit = 0.100;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- dynamic_balancing_disabled
                                    }; -- rr
                                  }; -- balancer2
                                }; -- report
                              }; -- video_man
                              video_pumpkin = {
                                weight = -1.000;
                                report = {
                                  uuid = "video-xml_requests_to_pumpkin";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  shared = {
                                    uuid = "1956641253016761558";
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      connection_attempts = 2;
                                      rr = {
                                        unpack(gen_proxy_backends({
                                          { "lite01h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b02:922b:34ff:fecf:236e"; };
                                          { "lite01i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6100:92e2:baff:fe56:e910"; };
                                          { "lite02h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b01:922b:34ff:fecf:2c28"; };
                                          { "lite02i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6110:f652:14ff:fe8b:f880"; };
                                          { "lite03h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b04:922b:34ff:fecf:2b68"; };
                                          { "lite03i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6105:e61d:2dff:fe00:8cf0"; };
                                          { "lite04h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b0f:922b:34ff:fecf:2aee"; };
                                          { "lite04i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6104:e61d:2dff:fe01:fb60"; };
                                          { "lite05h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b06:922b:34ff:fecf:28b0"; };
                                          { "lite05i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6107:f652:14ff:fef5:d4d0"; };
                                          { "lite06h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b10:922b:34ff:fecf:3b22"; };
                                          { "lite06i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6109:f652:14ff:fef5:d5c0"; };
                                          { "lite07h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b10:922b:34ff:fecf:3aca"; };
                                          { "lite08h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b02:922b:34ff:fecf:27dd"; };
                                          { "lite08i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6101:e61d:2dff:fe03:36c0"; };
                                          { "lite09h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:172:922b:34ff:fecf:3c32"; };
                                          { "lite09i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6106:f652:14ff:fe74:3930"; };
                                          { "lite10h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b06:922b:34ff:fecf:2bd0"; };
                                          { "lite10i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6103:f652:14ff:fef5:c840"; };
                                          { "lite11h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b09:922b:34ff:fecc:7a66"; };
                                          { "lite11i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610b:f652:14ff:fe74:3c00"; };
                                          { "lite12h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b11:922b:34ff:fecf:3338"; };
                                          { "lite12i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6108:e61d:2dff:fe03:3740"; };
                                          { "man1-3946.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610f:92e2:baff:fea1:7862"; };
                                          { "vla1-4617.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:70:0:614:db7:a43c"; };
                                          { "vla1-4637.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:9e:0:614:db7:a15b"; };
                                          { "vla1-4658.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:84:0:614:db7:a02a"; };
                                          { "vla1-4665.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:93:0:614:db7:a854"; };
                                          { "vla1-4670.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:4d:0:614:db7:a46e"; };
                                          { "vla1-4672.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:4c:0:614:db7:a474"; };
                                          { "vla2-5904.search.yandex.net"; 80; 2539.000; "2a02:6b8:c0e:409:0:604:9ad4:f0b0"; };
                                          { "vla2-5905.search.yandex.net"; 80; 2539.000; "2a02:6b8:c0e:409:0:604:4b02:75fa"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- rr
                                      attempts_rate_limiter = {
                                        limit = 0.200;
                                        coeff = 0.990;
                                        switch_default = true;
                                      }; -- attempts_rate_limiter
                                    }; -- balancer2
                                  }; -- shared
                                }; -- report
                              }; -- video_pumpkin
                              video_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "video-xml_requests_to_devnull";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  errordocument = {
                                    status = 204;
                                    force_conn_close = false;
                                  }; -- errordocument
                                }; -- report
                              }; -- video_devnull
                            }; -- rr
                            attempts_rate_limiter = {
                              limit = 0.200;
                              coeff = 0.990;
                              switch_default = true;
                            }; -- attempts_rate_limiter
                            on_error = {
                              report = {
                                uuid = "video-xml_requests_to_onerror";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                shared = {
                                  uuid = "1956641253016761558";
                                }; -- shared
                              }; -- report
                            }; -- on_error
                          }; -- balancer2
                        }; -- headers_hasher
                      }; -- request_replier
                    }; -- report
                  }; -- shared
                }; -- ["video-xml"]
                video_api = {
                  priority = 5;
                  match_or = {
                    {
                      match_fsm = {
                        URI = "/video/api(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/video/station(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/video/cmnt_feed(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/video/quasar(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_or
                  shared = {
                    uuid = "804456923640137196";
                    report = {
                      uuid = "video_api";
                      ranges = get_str_var("default_ranges");
                      just_storage = false;
                      disable_robotness = true;
                      disable_sslness = true;
                      events = {
                        stats = "report";
                      }; -- events
                      geobase = {
                        trusted = false;
                        geo_host = "laas.yandex.ru";
                        take_ip_from = "X-Forwarded-For-Y";
                        laas_answer_header = "X-LaaS-Answered";
                        file_switch = "./controls/disable_geobase.switch";
                        geo_path = "/region?response_format=header&version=1&service=balancer";
                        geo = {
                          shared = {
                            uuid = "6796952198230220343";
                            report = {
                              uuid = "geobasemodule";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
                                balancer2 = {
                                  simple_policy = {};
                                  attempts = 2;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "laas.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::1022"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "15ms";
                                      backend_timeout = "20ms";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 10;
                                      need_resolve = true;
                                    }))
                                  }; -- rr
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- shared
                        }; -- geo
                        headers = {
                          create = {
                            ["X-L7-EXP"] = "true";
                          }; -- create
                          exp_getter = {
                            trusted = false;
                            file_switch = "./controls/expgetter.switch";
                            service_name = "video";
                            service_name_header = "Y-Service";
                            exp_headers = "X-Yandex-ExpConfigVersion(-Pre)?|X-Yandex-ExpBoxes(-Pre)?|X-Yandex-ExpFlags(-Pre)?|X-Yandex-LogstatUID|X-Yandex-ExpSplitParams";
                            uaas = {
                              shared = {
                                uuid = "8988526946134060236";
                                report = {
                                  uuid = "expgetter";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  stats_eater = {
                                    balancer2 = {
                                      by_name_policy = {
                                        name = get_geo("bygeo_", "random");
                                        simple_policy = {};
                                      }; -- by_name_policy
                                      attempts = 1;
                                      rr = {
                                        bygeo_man = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "man1-0551-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:3372:10e:b563:0:43d1"; };
                                                { "man1-3722-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:37e8:10e:b563:0:43d1"; };
                                                { "man1-4352-a48-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:3cda:10e:b563:0:43d1"; };
                                                { "man1-4648-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:168e:10e:b563:0:43d1"; };
                                                { "man1-5661-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:deb:10e:b563:0:43d1"; };
                                                { "man1-6670-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:172:10e:b563:0:43d1"; };
                                                { "man1-7202-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:a16:10e:b563:0:43d1"; };
                                                { "man1-8284-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:315:10e:b563:0:43d1"; };
                                                { "man1-8314-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:301:10e:b563:0:43d1"; };
                                                { "man2-0395-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4415:10e:b563:0:43d1"; };
                                                { "man2-0510-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4d8c:10e:b563:0:43d1"; };
                                                { "man2-0584-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4105:10e:b563:0:43d1"; };
                                                { "man2-0971-af4-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:22a3:10e:b563:0:43d1"; };
                                                { "man2-1463-c9d-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:26a7:10e:b563:0:43d1"; };
                                                { "man2-1680-ca9-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:2617:10e:b563:0:43d1"; };
                                                { "man2-3519-d99-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4b02:10e:b563:0:43d1"; };
                                                { "man2-3535-57b-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:5989:10e:b563:0:43d1"; };
                                                { "man2-4159-92f-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:5720:10e:b563:0:43d1"; };
                                                { "man2-4167-a09-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:571a:10e:b563:0:43d1"; };
                                                { "man2-4667-250-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:1aa7:10e:b563:0:43d1"; };
                                                { "man2-4689-8c8-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:1b8c:10e:b563:0:43d1"; };
                                                { "man2-4806-07c-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:2786:10e:b563:0:43d1"; };
                                                { "man2-6550-5da-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2704:10e:b563:0:43d1"; };
                                                { "man2-6586-c86-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2987:10e:b563:0:43d1"; };
                                                { "man2-6943-60c-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2584:10e:b563:0:43d1"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- bygeo_man
                                        bygeo_sas = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "sas1-0322-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1092:10e:b566:0:43f7"; };
                                                { "sas1-0370-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:a03:10e:b566:0:43f7"; };
                                                { "sas1-0375-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:929:10e:b566:0:43f7"; };
                                                { "sas1-0730-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1405:10e:b566:0:43f7"; };
                                                { "sas1-1127-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:3515:10e:b566:0:43f7"; };
                                                { "sas1-1693-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:4810:10e:b566:0:43f7"; };
                                                { "sas1-1786-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:8e2f:10e:b566:0:43f7"; };
                                                { "sas1-2165-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:37a0:10e:b566:0:43f7"; };
                                                { "sas1-2335-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1d93:10e:b566:0:43f7"; };
                                                { "sas1-2491-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:162b:10e:b566:0:43f7"; };
                                                { "sas1-2511-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1804:10e:b566:0:43f7"; };
                                                { "sas1-2535-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:18a8:10e:b566:0:43f7"; };
                                                { "sas1-2607-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1603:10e:b566:0:43f7"; };
                                                { "sas1-2659-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1724:10e:b566:0:43f7"; };
                                                { "sas1-2769-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1812:10e:b566:0:43f7"; };
                                                { "sas1-2802-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:168d:10e:b566:0:43f7"; };
                                                { "sas1-4343-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:3487:10e:b566:0:43f7"; };
                                                { "sas1-4612-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:24af:10e:b566:0:43f7"; };
                                                { "sas1-4621-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:211a:10e:b566:0:43f7"; };
                                                { "sas1-4814-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:219f:10e:b566:0:43f7"; };
                                                { "sas1-4898-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:218a:10e:b566:0:43f7"; };
                                                { "sas1-4903-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:2a4:10e:b566:0:43f7"; };
                                                { "sas1-4906-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:930e:10e:b566:0:43f7"; };
                                                { "sas1-5003-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:4881:10e:b566:0:43f7"; };
                                                { "sas1-5414-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:2106:10e:b566:0:43f7"; };
                                                { "sas1-5538-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:369f:10e:b566:0:43f7"; };
                                                { "sas1-6006-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a29:10e:b566:0:43f7"; };
                                                { "sas1-7522-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:43af:10e:b566:0:43f7"; };
                                                { "sas1-9397-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:711e:10e:b566:0:43f7"; };
                                                { "sas1-9493-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:7115:10e:b566:0:43f7"; };
                                                { "sas2-0148-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c11:213:10e:b566:0:43f7"; };
                                                { "sas2-0528-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c11:e9a:10e:b566:0:43f7"; };
                                                { "sas2-1143-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:b621:10e:b566:0:43f7"; };
                                                { "sas2-3214-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:ed1c:10e:b566:0:43f7"; };
                                                { "sas2-4113-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:7584:10e:b566:0:43f7"; };
                                                { "sas2-4687-f96-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:692c:10e:b566:0:43f7"; };
                                                { "sas2-6078-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c14:439d:10e:b566:0:43f7"; };
                                                { "sas2-6514-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c16:31d:10e:b566:0:43f7"; };
                                                { "sas2-8852-7e7-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c16:1d9c:10e:b566:0:43f7"; };
                                                { "slovo012-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:72a2:10e:b566:0:43f7"; };
                                                { "slovo045-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6d8a:10e:b566:0:43f7"; };
                                                { "slovo055-5be-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6422:10e:b566:0:43f7"; };
                                                { "slovo080-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6b14:10e:b566:0:43f7"; };
                                                { "slovo103-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a8f:10e:b566:0:43f7"; };
                                                { "slovo126-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6b18:10e:b566:0:43f7"; };
                                                { "slovo143-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a9c:10e:b566:0:43f7"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- bygeo_sas
                                        bygeo_vla = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "vla1-0141-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1f82:10e:b569:0:37d2"; };
                                                { "vla1-0299-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4c09:10e:b569:0:37d2"; };
                                                { "vla1-0487-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1a02:10e:b569:0:37d2"; };
                                                { "vla1-0606-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1391:10e:b569:0:37d2"; };
                                                { "vla1-0660-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:918:10e:b569:0:37d2"; };
                                                { "vla1-0724-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1e08:10e:b569:0:37d2"; };
                                                { "vla1-0732-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:29a2:10e:b569:0:37d2"; };
                                                { "vla1-0969-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:218c:10e:b569:0:37d2"; };
                                                { "vla1-1523-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:261b:10e:b569:0:37d2"; };
                                                { "vla1-1538-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2987:10e:b569:0:37d2"; };
                                                { "vla1-1560-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3492:10e:b569:0:37d2"; };
                                                { "vla1-1600-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:379d:10e:b569:0:37d2"; };
                                                { "vla1-1674-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3499:10e:b569:0:37d2"; };
                                                { "vla1-1776-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4084:10e:b569:0:37d2"; };
                                                { "vla1-1844-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2b9b:10e:b569:0:37d2"; };
                                                { "vla1-2047-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:349b:10e:b569:0:37d2"; };
                                                { "vla1-2051-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:261a:10e:b569:0:37d2"; };
                                                { "vla1-2083-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2a0d:10e:b569:0:37d2"; };
                                                { "vla1-2192-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:228e:10e:b569:0:37d2"; };
                                                { "vla1-2439-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a93:10e:b569:0:37d2"; };
                                                { "vla1-2467-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a01:10e:b569:0:37d2"; };
                                                { "vla1-2474-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a87:10e:b569:0:37d2"; };
                                                { "vla1-2482-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3c9a:10e:b569:0:37d2"; };
                                                { "vla1-2526-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a98:10e:b569:0:37d2"; };
                                                { "vla1-3220-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1912:10e:b569:0:37d2"; };
                                                { "vla1-3454-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:c90:10e:b569:0:37d2"; };
                                                { "vla1-3715-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:5084:10e:b569:0:37d2"; };
                                                { "vla1-3819-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0f:1d89:10e:b569:0:37d2"; };
                                                { "vla1-3876-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4302:10e:b569:0:37d2"; };
                                                { "vla1-4007-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4817:10e:b569:0:37d2"; };
                                                { "vla1-4362-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:439d:10e:b569:0:37d2"; };
                                                { "vla1-4408-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:431a:10e:b569:0:37d2"; };
                                                { "vla1-4580-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3b96:10e:b569:0:37d2"; };
                                                { "vla1-5539-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0f:1e10:10e:b569:0:37d2"; };
                                                { "vla2-1001-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:498:10e:b569:0:37d2"; };
                                                { "vla2-1003-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:49a:10e:b569:0:37d2"; };
                                                { "vla2-1008-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:49d:10e:b569:0:37d2"; };
                                                { "vla2-1015-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1ba1:10e:b569:0:37d2"; };
                                                { "vla2-1017-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1b83:10e:b569:0:37d2"; };
                                                { "vla2-1019-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:48a:10e:b569:0:37d2"; };
                                                { "vla2-1067-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1ba2:10e:b569:0:37d2"; };
                                                { "vla2-1071-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1b9f:10e:b569:0:37d2"; };
                                                { "vla2-5945-62c-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:620:10e:b569:0:37d2"; };
                                                { "vla2-5963-9a4-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:612:10e:b569:0:37d2"; };
                                                { "vla2-7970-d06-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:398d:10e:b569:0:37d2"; };
                                                { "vla2-7992-190-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:1422:10e:b569:0:37d2"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- bygeo_vla
                                      }; -- rr
                                      on_error = {
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 1;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "uaas.search.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::2:48"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "20ms";
                                              backend_timeout = "30ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- report
                              }; -- shared
                            }; -- uaas
                            request_replier = {
                              sink = {
                                shared = {
                                  uuid = "6168121074701619930";
                                }; -- shared
                              }; -- sink
                              enable_failed_requests_replication = false;
                              rate = 0.000;
                              rate_file = "./controls/request_replier_video_api.ratefile";
                              shared = {
                                uuid = "3205349674490610521";
                                headers_hasher = {
                                  header_name = "X-Yandex-LogstatUID";
                                  surround = false;
                                  randomize_empty_match = true;
                                  balancer2 = {
                                    by_name_from_header_policy = {
                                      allow_zero_weights = true;
                                      strict = true;
                                      hints = {
                                        {
                                          hint = "man";
                                          backend = "video_man";
                                        };
                                        {
                                          hint = "sas";
                                          backend = "video_sas";
                                        };
                                        {
                                          hint = "vla";
                                          backend = "video_vla";
                                        };
                                      }; -- hints
                                      by_hash_policy = {
                                        unique_policy = {};
                                      }; -- by_hash_policy
                                    }; -- by_name_from_header_policy
                                    attempts = 2;
                                    attempts_file = "./controls/video_api.attempts";
                                    rr = {
                                      weights_file = "./controls/search_l7_balancer_switch.json";
                                      video_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "video_api_requests_to_vla";
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
                                            rr = {
                                              weights_file = "./controls/dynamic_balancing_switch";
                                              dynamic_balancing_enabled = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "video-api_requests_to_vla_dynamic";
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
                                                    attempts_file = "./controls/attempts.count";
                                                    connection_attempts = 5;
                                                    dynamic = {
                                                      max_pessimized_share = 0.100;
                                                      min_pessimization_coeff = 0.100;
                                                      weight_increase_step = 0.100;
                                                      history_interval = "10s";
                                                      backends_name = "video_api_vla";
                                                      unpack(gen_proxy_backends({
                                                        { "vla1-0213.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                                                        { "vla1-0557.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                                                        { "vla1-0762.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                                                        { "vla1-0852.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                                                        { "vla1-1073.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                                                        { "vla1-1167.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                                                        { "vla1-1206.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                                                        { "vla1-1245.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                                                        { "vla1-1266.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                                                        { "vla1-1412.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                                                        { "vla1-1426.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                                                        { "vla1-1454.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                                                        { "vla1-1484.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                                                        { "vla1-1497.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                                                        { "vla1-1508.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                                                        { "vla1-1527.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                                                        { "vla1-1530.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                                                        { "vla1-1540.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                                                        { "vla1-1556.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                                                        { "vla1-1568.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                                                        { "vla1-1571.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                                                        { "vla1-1575.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                                                        { "vla1-1636.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                                                        { "vla1-1643.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                                                        { "vla1-1650.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                                                        { "vla1-1654.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                                                        { "vla1-1744.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                                                        { "vla1-1785.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                                                        { "vla1-1862.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                                                        { "vla1-1869.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                                                        { "vla1-1889.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                                                        { "vla1-1892.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a421"; };
                                                        { "vla1-1902.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                                                        { "vla1-1920.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                                                        { "vla1-1937.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                                                        { "vla1-2006.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                                                        { "vla1-2011.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                                                        { "vla1-2035.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                                                        { "vla1-2039.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                                                        { "vla1-2097.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                                                        { "vla1-2131.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                                                        { "vla1-2141.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                                                        { "vla1-2153.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                                                        { "vla1-2209.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                                                        { "vla1-2338.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                                                        { "vla1-2341.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                                                        { "vla1-2363.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                                                        { "vla1-2368.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                                                        { "vla1-2392.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                                                        { "vla1-2440.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                                                        { "vla1-2759.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                                                        { "vla1-2901.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                                                        { "vla1-3477.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                                                        { "vla1-4120.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                                                        { "vla1-4143.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- dynamic
                                                    attempts_rate_limiter = {
                                                      limit = 0.100;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- dynamic_balancing_enabled
                                              dynamic_balancing_disabled = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  attempts_file = "./controls/attempts.count";
                                                  connection_attempts = 5;
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-0213.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                                                      { "vla1-0557.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                                                      { "vla1-0762.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                                                      { "vla1-0852.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                                                      { "vla1-1073.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                                                      { "vla1-1167.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                                                      { "vla1-1206.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                                                      { "vla1-1245.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                                                      { "vla1-1266.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                                                      { "vla1-1412.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                                                      { "vla1-1426.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                                                      { "vla1-1454.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                                                      { "vla1-1484.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                                                      { "vla1-1497.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                                                      { "vla1-1508.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                                                      { "vla1-1527.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                                                      { "vla1-1530.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                                                      { "vla1-1540.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                                                      { "vla1-1556.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                                                      { "vla1-1568.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                                                      { "vla1-1571.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                                                      { "vla1-1575.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                                                      { "vla1-1636.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                                                      { "vla1-1643.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                                                      { "vla1-1650.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                                                      { "vla1-1654.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                                                      { "vla1-1744.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                                                      { "vla1-1785.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                                                      { "vla1-1862.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                                                      { "vla1-1869.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                                                      { "vla1-1889.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                                                      { "vla1-1892.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a421"; };
                                                      { "vla1-1902.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                                                      { "vla1-1920.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                                                      { "vla1-1937.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                                                      { "vla1-2006.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                                                      { "vla1-2011.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                                                      { "vla1-2035.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                                                      { "vla1-2039.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                                                      { "vla1-2097.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                                                      { "vla1-2131.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                                                      { "vla1-2141.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                                                      { "vla1-2153.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                                                      { "vla1-2209.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                                                      { "vla1-2338.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                                                      { "vla1-2341.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                                                      { "vla1-2363.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                                                      { "vla1-2368.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                                                      { "vla1-2392.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                                                      { "vla1-2440.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                                                      { "vla1-2759.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                                                      { "vla1-2901.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                                                      { "vla1-3477.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                                                      { "vla1-4120.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                                                      { "vla1-4143.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                              }; -- dynamic_balancing_disabled
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- report
                                      }; -- video_vla
                                      video_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "video_api_requests_to_sas";
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
                                            rr = {
                                              weights_file = "./controls/dynamic_balancing_switch";
                                              dynamic_balancing_enabled = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "video-api_requests_to_sas_dynamic";
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
                                                    attempts_file = "./controls/attempts.count";
                                                    connection_attempts = 5;
                                                    dynamic = {
                                                      max_pessimized_share = 0.100;
                                                      min_pessimization_coeff = 0.100;
                                                      weight_increase_step = 0.100;
                                                      history_interval = "10s";
                                                      backends_name = "video_api_sas";
                                                      unpack(gen_proxy_backends({
                                                        { "sas1-6052.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:117:215:b2ff:fea7:7730"; };
                                                        { "sas1-6069.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:169:215:b2ff:fea7:74b8"; };
                                                        { "sas1-6113.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:11c:215:b2ff:fea7:7958"; };
                                                        { "sas1-6140.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a7:215:b2ff:fea7:7a58"; };
                                                        { "sas1-6207.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:197:215:b2ff:fea7:7988"; };
                                                        { "sas1-6240.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                                                        { "sas1-6475.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a8:215:b2ff:fea7:7edc"; };
                                                        { "sas1-6539.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                                                        { "sas1-6600.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:110:215:b2ff:fea7:7ec8"; };
                                                        { "sas1-6626.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                                                        { "sas1-6631.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:119:215:b2ff:fea7:7acc"; };
                                                        { "sas1-6634.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                                                        { "sas1-6730.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                                                        { "sas1-6750.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                                                        { "sas1-6785.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                                                        { "sas1-6805.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                                                        { "sas1-6851.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                                                        { "sas1-6869.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                                                        { "sas1-6938.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                                                        { "sas1-7005.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                                                        { "sas1-7013.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                                                        { "sas1-7138.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                                                        { "sas1-7142.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                                                        { "sas1-7192.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                                                        { "sas1-7234.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                                                        { "sas1-7481.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8c0c"; };
                                                        { "sas1-7534.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                                                        { "sas1-7569.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                                                        { "sas1-7574.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                                                        { "sas1-7591.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                                                        { "sas1-7747.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                                                        { "sas1-7761.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                                                        { "sas1-7790.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                                                        { "sas1-7803.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                                                        { "sas1-7816.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                                                        { "sas1-7824.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                                                        { "sas1-7902.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                                                        { "sas1-7908.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                                                        { "sas1-7925.search.yandex.net"; 31815; 30.000; "2a02:6b8:c02:7b6:0:604:3564:50dd"; };
                                                        { "sas1-7926.search.yandex.net"; 31815; 30.000; "2a02:6b8:c02:7b6:0:604:3564:51cb"; };
                                                        { "sas1-7931.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:618:215:b2ff:fea7:a9f0"; };
                                                        { "sas1-7940.search.yandex.net"; 31815; 30.000; "2a02:6b8:c02:7b6:0:604:3564:51c5"; };
                                                        { "sas1-7942.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                                                        { "sas1-8179.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                                                        { "sas1-8214.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                                                        { "sas1-8236.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                                                        { "sas1-8246.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                                                        { "sas1-8299.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                                                        { "sas1-8369.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                                                        { "sas1-8401.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                                                        { "sas1-8546.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                                                        { "sas1-8571.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                                                        { "sas1-8612.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                                                        { "sas1-8618.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                                                        { "sas1-8635.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                                                        { "sas1-8830.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                                                        { "sas1-8932.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                                                        { "sas1-8960.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                                                        { "sas1-9134.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:629:feaa:14ff:fede:3f94"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- dynamic
                                                    attempts_rate_limiter = {
                                                      limit = 0.100;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- dynamic_balancing_enabled
                                              dynamic_balancing_disabled = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  attempts_file = "./controls/attempts.count";
                                                  connection_attempts = 5;
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "sas1-6052.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:117:215:b2ff:fea7:7730"; };
                                                      { "sas1-6069.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:169:215:b2ff:fea7:74b8"; };
                                                      { "sas1-6113.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:11c:215:b2ff:fea7:7958"; };
                                                      { "sas1-6140.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a7:215:b2ff:fea7:7a58"; };
                                                      { "sas1-6207.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:197:215:b2ff:fea7:7988"; };
                                                      { "sas1-6240.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                                                      { "sas1-6475.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a8:215:b2ff:fea7:7edc"; };
                                                      { "sas1-6539.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                                                      { "sas1-6600.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:110:215:b2ff:fea7:7ec8"; };
                                                      { "sas1-6626.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                                                      { "sas1-6631.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:119:215:b2ff:fea7:7acc"; };
                                                      { "sas1-6634.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                                                      { "sas1-6730.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                                                      { "sas1-6750.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                                                      { "sas1-6785.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                                                      { "sas1-6805.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                                                      { "sas1-6851.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                                                      { "sas1-6869.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                                                      { "sas1-6938.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                                                      { "sas1-7005.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                                                      { "sas1-7013.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                                                      { "sas1-7138.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                                                      { "sas1-7142.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                                                      { "sas1-7192.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                                                      { "sas1-7234.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                                                      { "sas1-7481.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8c0c"; };
                                                      { "sas1-7534.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                                                      { "sas1-7569.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                                                      { "sas1-7574.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                                                      { "sas1-7591.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                                                      { "sas1-7747.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                                                      { "sas1-7761.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                                                      { "sas1-7790.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                                                      { "sas1-7803.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                                                      { "sas1-7816.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                                                      { "sas1-7824.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                                                      { "sas1-7902.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                                                      { "sas1-7908.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                                                      { "sas1-7925.search.yandex.net"; 31815; 30.000; "2a02:6b8:c02:7b6:0:604:3564:50dd"; };
                                                      { "sas1-7926.search.yandex.net"; 31815; 30.000; "2a02:6b8:c02:7b6:0:604:3564:51cb"; };
                                                      { "sas1-7931.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:618:215:b2ff:fea7:a9f0"; };
                                                      { "sas1-7940.search.yandex.net"; 31815; 30.000; "2a02:6b8:c02:7b6:0:604:3564:51c5"; };
                                                      { "sas1-7942.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                                                      { "sas1-8179.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                                                      { "sas1-8214.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                                                      { "sas1-8236.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                                                      { "sas1-8246.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                                                      { "sas1-8299.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                                                      { "sas1-8369.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                                                      { "sas1-8401.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                                                      { "sas1-8546.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                                                      { "sas1-8571.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                                                      { "sas1-8612.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                                                      { "sas1-8618.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                                                      { "sas1-8635.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                                                      { "sas1-8830.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                                                      { "sas1-8932.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                                                      { "sas1-8960.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                                                      { "sas1-9134.search.yandex.net"; 31815; 30.000; "2a02:6b8:b000:629:feaa:14ff:fede:3f94"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                              }; -- dynamic_balancing_disabled
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- report
                                      }; -- video_sas
                                      video_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "video_api_requests_to_man";
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
                                            rr = {
                                              weights_file = "./controls/dynamic_balancing_switch";
                                              dynamic_balancing_enabled = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "video-api_requests_to_man_dynamic";
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
                                                    attempts_file = "./controls/attempts.count";
                                                    connection_attempts = 5;
                                                    dynamic = {
                                                      max_pessimized_share = 0.100;
                                                      min_pessimization_coeff = 0.100;
                                                      weight_increase_step = 0.100;
                                                      history_interval = "10s";
                                                      backends_name = "video_api_man";
                                                      unpack(gen_proxy_backends({
                                                        { "man1-1282.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                                                        { "man1-1296.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                                                        { "man1-1947.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                                                        { "man1-2848.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                                                        { "man1-2960.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                                                        { "man1-4051.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                                                        { "man1-4426.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                                                        { "man1-4461.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                                                        { "man1-4489.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                                                        { "man1-4497.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                                                        { "man1-4525.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                                                        { "man1-4548.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                                                        { "man1-7451.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                                                        { "man1-9282.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6085:92e2:baff:fea3:72dc"; };
                                                        { "man1-9558.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:616:0:604:baa0:b2bc"; };
                                                        { "man2-1704.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:616:0:604:baa0:b2da"; };
                                                        { "man2-1705.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:614:0:604:baa0:b260"; };
                                                        { "man2-1745.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:614:0:604:baa0:b2c4"; };
                                                        { "man2-1768.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:614:0:604:baa0:b00e"; };
                                                        { "man2-4866.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:631:0:604:1465:ca36"; };
                                                        { "man2-4891.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:632:0:604:1465:ca7e"; };
                                                        { "man2-4958.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:728c"; };
                                                        { "man2-4965.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7804"; };
                                                        { "man2-5012.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63f:0:604:14a7:67ba"; };
                                                        { "man2-5150.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:3c70"; };
                                                        { "man2-5164.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:3a90"; };
                                                        { "man2-5330.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:83c:0:604:14a7:bc47"; };
                                                        { "man2-5390.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:834:0:604:baa1:7d94"; };
                                                        { "man2-5393.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:833:0:604:baa1:72d6"; };
                                                        { "man2-5426.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:2d6d:1b20"; };
                                                        { "man2-5455.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:3bd0"; };
                                                        { "man2-5481.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:2d6d:1790"; };
                                                        { "man2-5501.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:c30"; };
                                                        { "man2-5517.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:7928"; };
                                                        { "man2-5615.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:633:0:604:14a7:67b7"; };
                                                        { "man2-5664.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7d1e"; };
                                                        { "man2-5670.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:725e"; };
                                                        { "man2-5672.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7af2"; };
                                                        { "man2-5678.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:3160"; };
                                                        { "man2-5684.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:18c0"; };
                                                        { "man2-5696.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:3930"; };
                                                        { "man2-5700.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:2fc0"; };
                                                        { "man2-5712.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:2f80"; };
                                                        { "man2-5717.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:38d0"; };
                                                        { "man2-5732.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:1a40"; };
                                                        { "man2-5761.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:639:0:604:baa1:7e56"; };
                                                        { "man2-5791.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:831:0:604:baa1:7a68"; };
                                                        { "man2-5811.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63d:0:604:baa0:b244"; };
                                                        { "man2-5827.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63d:0:604:baa0:b22c"; };
                                                        { "man2-5831.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:834:0:604:baa1:7cf8"; };
                                                        { "man2-5855.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:3030"; };
                                                        { "man2-5857.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:833:0:604:baa1:7aa6"; };
                                                        { "man2-5887.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:834:0:604:baa1:7c66"; };
                                                        { "man2-5889.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:832:0:604:baa1:777e"; };
                                                        { "man2-5898.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:832:0:604:baa1:7aae"; };
                                                        { "man2-5936.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:639:0:604:baa1:72b6"; };
                                                        { "man2-5944.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:3a10"; };
                                                        { "man2-5984.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:900"; };
                                                        { "man2-5996.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:fa0"; };
                                                        { "man2-6001.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:cc0"; };
                                                        { "man2-6018.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:639:0:604:baa1:7c46"; };
                                                        { "man2-6028.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:ae0"; };
                                                        { "man2-6037.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:832:0:604:baa1:7c30"; };
                                                        { "man2-6056.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63d:0:604:baa0:b0a6"; };
                                                        { "man2-6061.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:833:0:604:baa1:7b2e"; };
                                                        { "man2-6064.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:3150"; };
                                                        { "man2-6070.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63c:0:604:baa1:7866"; };
                                                        { "man2-6076.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63c:0:604:baa1:7d56"; };
                                                        { "man2-6160.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:630:0:604:1465:ce29"; };
                                                        { "man2-6174.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:654:0:604:baa1:743e"; };
                                                        { "man2-6209.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:654:0:604:baa1:7710"; };
                                                        { "man2-6233.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:657:0:604:1465:ca57"; };
                                                        { "man2-6274.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63c:0:604:baa1:7d62"; };
                                                        { "man2-6307.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:831:0:604:baa1:7db0"; };
                                                        { "man2-6310.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:831:0:604:baa1:7dac"; };
                                                        { "man2-6387.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:641:0:604:14a7:67fe"; };
                                                        { "man2-6420.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:654:0:604:baa1:73f6"; };
                                                        { "man2-6623.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:64c:0:604:14a7:bc2c"; };
                                                        { "man2-6857.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:644:0:604:14a7:baa5"; };
                                                        { "man2-7026.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:64c:0:604:14a7:b9c2"; };
                                                        { "man2-7087.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:baa2:3e32"; };
                                                        { "man2-7363.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:641:0:604:14a7:67fb"; };
                                                        { "man2-7411.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:64c:0:604:14a7:bc2a"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- dynamic
                                                    attempts_rate_limiter = {
                                                      limit = 0.100;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- dynamic_balancing_enabled
                                              dynamic_balancing_disabled = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  attempts_file = "./controls/attempts.count";
                                                  connection_attempts = 5;
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "man1-1282.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                                                      { "man1-1296.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                                                      { "man1-1947.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                                                      { "man1-2848.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                                                      { "man1-2960.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                                                      { "man1-4051.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                                                      { "man1-4426.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                                                      { "man1-4461.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                                                      { "man1-4489.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                                                      { "man1-4497.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                                                      { "man1-4525.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                                                      { "man1-4548.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                                                      { "man1-7451.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                                                      { "man1-9282.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6085:92e2:baff:fea3:72dc"; };
                                                      { "man1-9558.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:616:0:604:baa0:b2bc"; };
                                                      { "man2-1704.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:616:0:604:baa0:b2da"; };
                                                      { "man2-1705.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:614:0:604:baa0:b260"; };
                                                      { "man2-1745.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:614:0:604:baa0:b2c4"; };
                                                      { "man2-1768.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:614:0:604:baa0:b00e"; };
                                                      { "man2-4866.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:631:0:604:1465:ca36"; };
                                                      { "man2-4891.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:632:0:604:1465:ca7e"; };
                                                      { "man2-4958.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:728c"; };
                                                      { "man2-4965.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7804"; };
                                                      { "man2-5012.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63f:0:604:14a7:67ba"; };
                                                      { "man2-5150.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:3c70"; };
                                                      { "man2-5164.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:3a90"; };
                                                      { "man2-5330.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:83c:0:604:14a7:bc47"; };
                                                      { "man2-5390.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:834:0:604:baa1:7d94"; };
                                                      { "man2-5393.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:833:0:604:baa1:72d6"; };
                                                      { "man2-5426.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:2d6d:1b20"; };
                                                      { "man2-5455.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:3bd0"; };
                                                      { "man2-5481.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:2d6d:1790"; };
                                                      { "man2-5501.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:c30"; };
                                                      { "man2-5517.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:7928"; };
                                                      { "man2-5615.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:633:0:604:14a7:67b7"; };
                                                      { "man2-5664.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7d1e"; };
                                                      { "man2-5670.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:725e"; };
                                                      { "man2-5672.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7af2"; };
                                                      { "man2-5678.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:3160"; };
                                                      { "man2-5684.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:18c0"; };
                                                      { "man2-5696.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:3930"; };
                                                      { "man2-5700.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:2fc0"; };
                                                      { "man2-5712.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:2f80"; };
                                                      { "man2-5717.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:38d0"; };
                                                      { "man2-5732.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:1a40"; };
                                                      { "man2-5761.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:639:0:604:baa1:7e56"; };
                                                      { "man2-5791.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:831:0:604:baa1:7a68"; };
                                                      { "man2-5811.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63d:0:604:baa0:b244"; };
                                                      { "man2-5827.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63d:0:604:baa0:b22c"; };
                                                      { "man2-5831.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:834:0:604:baa1:7cf8"; };
                                                      { "man2-5855.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:3030"; };
                                                      { "man2-5857.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:833:0:604:baa1:7aa6"; };
                                                      { "man2-5887.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:834:0:604:baa1:7c66"; };
                                                      { "man2-5889.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:832:0:604:baa1:777e"; };
                                                      { "man2-5898.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:832:0:604:baa1:7aae"; };
                                                      { "man2-5936.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:639:0:604:baa1:72b6"; };
                                                      { "man2-5944.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:3a10"; };
                                                      { "man2-5984.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:900"; };
                                                      { "man2-5996.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:fa0"; };
                                                      { "man2-6001.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:cc0"; };
                                                      { "man2-6018.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:639:0:604:baa1:7c46"; };
                                                      { "man2-6028.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:ae0"; };
                                                      { "man2-6037.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:832:0:604:baa1:7c30"; };
                                                      { "man2-6056.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63d:0:604:baa0:b0a6"; };
                                                      { "man2-6061.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:833:0:604:baa1:7b2e"; };
                                                      { "man2-6064.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:3150"; };
                                                      { "man2-6070.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63c:0:604:baa1:7866"; };
                                                      { "man2-6076.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63c:0:604:baa1:7d56"; };
                                                      { "man2-6160.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:630:0:604:1465:ce29"; };
                                                      { "man2-6174.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:654:0:604:baa1:743e"; };
                                                      { "man2-6209.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:654:0:604:baa1:7710"; };
                                                      { "man2-6233.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:657:0:604:1465:ca57"; };
                                                      { "man2-6274.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63c:0:604:baa1:7d62"; };
                                                      { "man2-6307.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:831:0:604:baa1:7db0"; };
                                                      { "man2-6310.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:831:0:604:baa1:7dac"; };
                                                      { "man2-6387.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:641:0:604:14a7:67fe"; };
                                                      { "man2-6420.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:654:0:604:baa1:73f6"; };
                                                      { "man2-6623.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:64c:0:604:14a7:bc2c"; };
                                                      { "man2-6857.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:644:0:604:14a7:baa5"; };
                                                      { "man2-7026.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:64c:0:604:14a7:b9c2"; };
                                                      { "man2-7087.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:baa2:3e32"; };
                                                      { "man2-7363.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:641:0:604:14a7:67fb"; };
                                                      { "man2-7411.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:64c:0:604:14a7:bc2a"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                              }; -- dynamic_balancing_disabled
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- report
                                      }; -- video_man
                                      video_devnull = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "video_api_requests_to_devnull";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          errordocument = {
                                            status = 204;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- report
                                      }; -- video_devnull
                                    }; -- rr
                                  }; -- balancer2
                                }; -- headers_hasher
                              }; -- shared
                            }; -- request_replier
                          }; -- exp_getter
                        }; -- headers
                      }; -- geobase
                    }; -- report
                  }; -- shared
                }; -- video_api
                video_antiadblocker_POSTs = {
                  priority = 4;
                  match_and = {
                    {
                      match_fsm = {
                        match = "POST.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_not = {
                        match_fsm = {
                          header = {
                            name = "x-aab-http-check";
                            value = ".*";
                          }; -- header
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      }; -- match_not
                    };
                  }; -- match_and
                  shared = {
                    uuid = "6695523609787551302";
                    shared = {
                      uuid = "video_prod";
                    }; -- shared
                  }; -- shared
                }; -- video_antiadblocker_POSTs
                video_antiadblocker = {
                  priority = 3;
                  match_and = {
                    {
                      match_not = {
                        match_fsm = {
                          host = "(sas|vla|man).yandex.ru";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                      }; -- match_not
                    };
                    {
                      match_or = {
                        {
                          match_and = {
                            {
                              match_fsm = {
                                URI = "/video/_crpd/.*";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                          }; -- match_and
                        };
                        {
                          match_and = {
                            {
                              match_fsm = {
                                URI = "/video(/.*)?";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_not = {
                                match_fsm = {
                                  URI = "/video/(touch|pad)(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              }; -- match_not
                            };
                            {
                              match_or = {
                                {
                                  match_fsm = {
                                    cookie = "lastexp=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "bltsr=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "qgZTpupNMGJBM=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "mcBaGDt=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "BgeeyNoBJuyII=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "orrXTfJaS=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "FgkKdCjPqoMFm=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "EIXtkCTlX=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "JPIqApiY=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "KIykI=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "HgGedof=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "ancQTZw=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "involved=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "instruction=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "engineering=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "telecommunications=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "discussion=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "computer=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "substantial=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "specific=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "engineer=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                                {
                                  match_fsm = {
                                    cookie = "adequate=1";
                                    case_insensitive = true;
                                    surround = true;
                                  }; -- match_fsm
                                };
                              }; -- match_or
                            };
                          }; -- match_and
                        };
                      }; -- match_or
                    };
                  }; -- match_and
                  shared = {
                    uuid = "video_antiadblocker_checks";
                    balancer2 = {
                      simple_policy = {};
                      attempts = 1;
                      rr = {
                        weights_file = "./controls/video_antiadblock.txt";
                        video_antiadblock = {
                          weight = -1.000;
                          headers = {
                            create = {
                              ["X-AAB-PartnerToken"] = get_str_env_var("AAB_TOKEN");
                              ["X-Forwarded-Proto"] = "https";
                              ["X-Yandex-Service-L7-Port"] = "81";
                              ["Y-Service"] = "video_antiadblock";
                            }; -- create
                            report = {
                              uuid = "video_antiadblock";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                simple_policy = {};
                                attempts = 2;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "cryprox.yandex.net"; 80; 1.000; "2a02:6b8::402"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "60ms";
                                    backend_timeout = "30s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- rr
                                attempts_rate_limiter = {
                                  limit = 0.100;
                                  coeff = 0.990;
                                  switch_default = true;
                                }; -- attempts_rate_limiter
                              }; -- balancer2
                            }; -- report
                          }; -- headers
                        }; -- video_antiadblock
                        video_prod = {
                          weight = 1.000;
                          shared = {
                            uuid = "6695523609787551302";
                          }; -- shared
                        }; -- video_prod
                      }; -- rr
                    }; -- balancer2
                  }; -- shared
                }; -- video_antiadblocker
                video_inverted_report = {
                  priority = 2;
                  match_fsm = {
                    cgi = "video_inverted_report=1";
                    case_insensitive = true;
                    surround = true;
                  }; -- match_fsm
                  shared = {
                    uuid = "2610403784453881705";
                    report = {
                      ranges = get_str_var("default_ranges");
                      just_storage = false;
                      disable_robotness = true;
                      disable_sslness = true;
                      events = {
                        stats = "report";
                      }; -- events
                      geobase = {
                        trusted = false;
                        geo_host = "laas.yandex.ru";
                        take_ip_from = "X-Forwarded-For-Y";
                        laas_answer_header = "X-LaaS-Answered";
                        file_switch = "./controls/disable_geobase.switch";
                        geo_path = "/region?response_format=header&version=1&service=balancer";
                        geo = {
                          shared = {
                            uuid = "6796952198230220343";
                          }; -- shared
                        }; -- geo
                        headers = {
                          create = {
                            ["X-L7-EXP"] = "true";
                          }; -- create
                          exp_getter = {
                            trusted = false;
                            file_switch = "./controls/expgetter.switch";
                            service_name = "video";
                            service_name_header = "Y-Service";
                            exp_headers = "X-Yandex-ExpConfigVersion(-Pre)?|X-Yandex-ExpBoxes(-Pre)?|X-Yandex-ExpFlags(-Pre)?|X-Yandex-LogstatUID|X-Yandex-ExpSplitParams";
                            uaas = {
                              shared = {
                                uuid = "8988526946134060236";
                              }; -- shared
                            }; -- uaas
                            request_replier = {
                              sink = {
                                balancer2 = {
                                  simple_policy = {};
                                  attempts = 5;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "sinkadm.priemka.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::eeee:20"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "10s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                    }))
                                  }; -- rr
                                }; -- balancer2
                              }; -- sink
                              enable_failed_requests_replication = false;
                              rate = 0.000;
                              rate_file = "./controls/request_replier_video_api.ratefile";
                              shared = {
                                uuid = "3205349674490610521";
                              }; -- shared
                            }; -- request_replier
                          }; -- exp_getter
                        }; -- headers
                      }; -- geobase
                      refers = "video_api";
                    }; -- report
                  }; -- shared
                }; -- video_inverted_report
                default = {
                  priority = 1;
                  shared = {
                    uuid = "4681682622068684582";
                    shared = {
                      uuid = "video_prod";
                      report = {
                        uuid = "video";
                        ranges = get_str_var("default_ranges");
                        just_storage = false;
                        disable_robotness = true;
                        disable_sslness = true;
                        events = {
                          stats = "report";
                        }; -- events
                        geobase = {
                          trusted = false;
                          geo_host = "laas.yandex.ru";
                          take_ip_from = "X-Forwarded-For-Y";
                          laas_answer_header = "X-LaaS-Answered";
                          file_switch = "./controls/disable_geobase.switch";
                          geo_path = "/region?response_format=header&version=1&service=balancer";
                          geo = {
                            shared = {
                              uuid = "6796952198230220343";
                            }; -- shared
                          }; -- geo
                          headers = {
                            create = {
                              ["X-L7-EXP"] = "true";
                            }; -- create
                            exp_getter = {
                              trusted = false;
                              file_switch = "./controls/expgetter.switch";
                              service_name = "video";
                              service_name_header = "Y-Service";
                              exp_headers = "X-Yandex-ExpConfigVersion(-Pre)?|X-Yandex-ExpBoxes(-Pre)?|X-Yandex-ExpFlags(-Pre)?|X-Yandex-LogstatUID|X-Yandex-ExpSplitParams";
                              uaas = {
                                shared = {
                                  uuid = "8988526946134060236";
                                }; -- shared
                              }; -- uaas
                              request_replier = {
                                sink = {
                                  shared = {
                                    uuid = "6168121074701619930";
                                  }; -- shared
                                }; -- sink
                                enable_failed_requests_replication = false;
                                rate = 0.000;
                                rate_file = "./controls/request_replier_video.ratefile";
                                headers_hasher = {
                                  header_name = "X-Yandex-LogstatUID";
                                  surround = false;
                                  randomize_empty_match = true;
                                  balancer2 = {
                                    by_name_from_header_policy = {
                                      allow_zero_weights = true;
                                      strict = true;
                                      hints = {
                                        {
                                          hint = "man";
                                          backend = "video_man";
                                        };
                                        {
                                          hint = "sas";
                                          backend = "video_sas";
                                        };
                                        {
                                          hint = "vla";
                                          backend = "video_vla";
                                        };
                                      }; -- hints
                                      by_hash_policy = {
                                        unique_policy = {};
                                      }; -- by_hash_policy
                                    }; -- by_name_from_header_policy
                                    attempts = 2;
                                    attempts_file = "./controls/video.attempts";
                                    connection_attempts = 3;
                                    rr = {
                                      weights_file = "./controls/search_l7_balancer_switch.json";
                                      video_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "video_requests_to_vla";
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
                                            rr = {
                                              weights_file = "./controls/dynamic_balancing_switch";
                                              dynamic_balancing_enabled = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "video-default_requests_to_vla_dynamic";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  shared = {
                                                    uuid = "1191218453291331145";
                                                  }; -- shared
                                                }; -- report
                                              }; -- dynamic_balancing_enabled
                                              dynamic_balancing_disabled = {
                                                weight = 1.000;
                                                shared = {
                                                  uuid = "3365650464778765030";
                                                }; -- shared
                                              }; -- dynamic_balancing_disabled
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- report
                                      }; -- video_vla
                                      video_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "video_requests_to_sas";
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
                                            rr = {
                                              weights_file = "./controls/dynamic_balancing_switch";
                                              dynamic_balancing_enabled = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "video-default_requests_to_sas_dynamic";
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
                                                    attempts_file = "./controls/attempts.count";
                                                    connection_attempts = 2;
                                                    dynamic = {
                                                      max_pessimized_share = 0.100;
                                                      min_pessimization_coeff = 0.100;
                                                      weight_increase_step = 0.100;
                                                      history_interval = "10s";
                                                      backends_name = "video_sas";
                                                      unpack(gen_proxy_backends({
                                                        { "sas1-6052.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:117:215:b2ff:fea7:7730"; };
                                                        { "sas1-6069.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:169:215:b2ff:fea7:74b8"; };
                                                        { "sas1-6113.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11c:215:b2ff:fea7:7958"; };
                                                        { "sas1-6140.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a7:215:b2ff:fea7:7a58"; };
                                                        { "sas1-6207.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:197:215:b2ff:fea7:7988"; };
                                                        { "sas1-6240.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                                                        { "sas1-6475.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a8:215:b2ff:fea7:7edc"; };
                                                        { "sas1-6539.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                                                        { "sas1-6600.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:110:215:b2ff:fea7:7ec8"; };
                                                        { "sas1-6626.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                                                        { "sas1-6631.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:119:215:b2ff:fea7:7acc"; };
                                                        { "sas1-6634.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                                                        { "sas1-6730.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                                                        { "sas1-6750.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                                                        { "sas1-6785.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                                                        { "sas1-6805.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                                                        { "sas1-6851.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                                                        { "sas1-6869.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                                                        { "sas1-6938.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                                                        { "sas1-7005.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                                                        { "sas1-7013.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                                                        { "sas1-7138.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                                                        { "sas1-7142.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                                                        { "sas1-7192.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                                                        { "sas1-7234.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                                                        { "sas1-7481.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8c0c"; };
                                                        { "sas1-7534.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                                                        { "sas1-7569.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                                                        { "sas1-7574.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                                                        { "sas1-7591.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                                                        { "sas1-7747.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                                                        { "sas1-7761.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                                                        { "sas1-7790.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                                                        { "sas1-7803.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                                                        { "sas1-7816.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                                                        { "sas1-7824.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                                                        { "sas1-7902.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                                                        { "sas1-7908.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                                                        { "sas1-7925.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:50dd"; };
                                                        { "sas1-7926.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:51cb"; };
                                                        { "sas1-7931.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:618:215:b2ff:fea7:a9f0"; };
                                                        { "sas1-7940.search.yandex.net"; 8080; 427.000; "2a02:6b8:c02:7b6:0:604:3564:51c5"; };
                                                        { "sas1-7942.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                                                        { "sas1-8179.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                                                        { "sas1-8214.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                                                        { "sas1-8236.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                                                        { "sas1-8246.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                                                        { "sas1-8299.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                                                        { "sas1-8369.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                                                        { "sas1-8401.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                                                        { "sas1-8546.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                                                        { "sas1-8571.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                                                        { "sas1-8612.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                                                        { "sas1-8618.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                                                        { "sas1-8635.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                                                        { "sas1-8830.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                                                        { "sas1-8932.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                                                        { "sas1-8960.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                                                        { "sas1-9134.search.yandex.net"; 8080; 427.000; "2a02:6b8:b000:629:feaa:14ff:fede:3f94"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- dynamic
                                                    attempts_rate_limiter = {
                                                      limit = 0.100;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- dynamic_balancing_enabled
                                              dynamic_balancing_disabled = {
                                                weight = 1.000;
                                                shared = {
                                                  uuid = "3292089584546115172";
                                                }; -- shared
                                              }; -- dynamic_balancing_disabled
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- report
                                      }; -- video_sas
                                      video_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "video_requests_to_man";
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
                                            rr = {
                                              weights_file = "./controls/dynamic_balancing_switch";
                                              dynamic_balancing_enabled = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "video-default_requests_to_man_dynamic";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  shared = {
                                                    uuid = "7505417920268842326";
                                                  }; -- shared
                                                }; -- report
                                              }; -- dynamic_balancing_enabled
                                              dynamic_balancing_disabled = {
                                                weight = 1.000;
                                                shared = {
                                                  uuid = "7232277778704636898";
                                                }; -- shared
                                              }; -- dynamic_balancing_disabled
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- report
                                      }; -- video_man
                                      video_pumpkin = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "video_requests_to_pumpkin";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "1956641253016761558";
                                          }; -- shared
                                        }; -- report
                                      }; -- video_pumpkin
                                      video_devnull = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "video_requests_to_devnull";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          errordocument = {
                                            status = 204;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- report
                                      }; -- video_devnull
                                    }; -- rr
                                    attempts_rate_limiter = {
                                      limit = 0.200;
                                      coeff = 0.990;
                                      switch_default = true;
                                    }; -- attempts_rate_limiter
                                    on_error = {
                                      report = {
                                        uuid = "video_requests_to_onerror";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        shared = {
                                          uuid = "1956641253016761558";
                                        }; -- shared
                                      }; -- report
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- headers_hasher
                              }; -- request_replier
                            }; -- exp_getter
                          }; -- headers
                        }; -- geobase
                      }; -- report
                    }; -- shared
                  }; -- shared
                }; -- default
              }; -- regexp
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
    http_no_cryprox_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        81;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 81, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 81, "/place/db/www/logs");
            report = {
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
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
                ["video-xml"] = {
                  priority = 4;
                  match_fsm = {
                    URI = "/video-xml(/.*)?";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "7202579568527170768";
                  }; -- shared
                }; -- ["video-xml"]
                video_api = {
                  priority = 3;
                  match_or = {
                    {
                      match_fsm = {
                        URI = "/video/api(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/video/station(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/video/cmnt_feed(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        URI = "/video/quasar(/.*)?";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_or
                  shared = {
                    uuid = "804456923640137196";
                  }; -- shared
                }; -- video_api
                video_inverted_report = {
                  priority = 2;
                  match_fsm = {
                    cgi = "video_inverted_report=1";
                    case_insensitive = true;
                    surround = true;
                  }; -- match_fsm
                  shared = {
                    uuid = "2610403784453881705";
                  }; -- shared
                }; -- video_inverted_report
                default = {
                  priority = 1;
                  shared = {
                    uuid = "4681682622068684582";
                  }; -- shared
                }; -- default
              }; -- regexp
              refers = "service_total";
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_no_cryprox_section
  }; -- ipdispatch
}