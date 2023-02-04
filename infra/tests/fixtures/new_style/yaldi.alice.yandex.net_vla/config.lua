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
  maxconn = 45000;
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
                  priority = 10;
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
                  priority = 9;
                  match_fsm = {
                    url = "/ping";
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
                                attempts = 3;
                                connection_attempts = 30;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "man1-6670-man-asr-desktopgeneral-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0b:172:10d:d4e4:0:3936"; };
                                    { "man1-7385-man-asr-desktopgeneral-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:12c:10d:d4e4:0:3936"; };
                                    { "man2-0965-287-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:22aa:10d:d4e2:0:3936"; };
                                    { "man2-0970-710-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:221f:10d:d4e2:0:3936"; };
                                    { "man2-1460-e3f-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26a4:10d:d4e2:0:3936"; };
                                    { "man2-1461-306-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:231e:10d:d4e2:0:3936"; };
                                    { "man2-1464-848-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26a5:10d:d4e2:0:3936"; };
                                    { "man2-1469-3f1-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:2314:10d:d4e4:0:3936"; };
                                    { "man2-1473-2a5-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26af:10d:d4e2:0:3936"; };
                                    { "man2-1474-c29-man-asr-quasar-g-625-18274.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:2324:10d:d4e5:0:4762"; };
                                    { "man2-1476-3a1-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:2696:10d:d4e2:0:3936"; };
                                    { "man2-1477-b64-man-asr-quasar-g-625-18274.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:232e:10d:d4e5:0:4762"; };
                                    { "man2-1478-ce6-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:231c:10d:d4e2:0:3936"; };
                                    { "man2-1479-ad3-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:2327:10d:d4e4:0:3936"; };
                                    { "man2-1481-ffe-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:268a:10d:d4e4:0:3936"; };
                                    { "man2-1482-af2-man-asr-quasar-g-625-18274.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:269d:10d:d4e5:0:4762"; };
                                    { "man2-1483-01c-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26ad:10d:d4e4:0:3936"; };
                                    { "man2-1485-36b-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:269f:10d:d4e2:0:3936"; };
                                    { "man2-1590-137-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:151c:10d:d4e2:0:3936"; };
                                    { "man2-1607-58d-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:1504:10d:d4e2:0:3936"; };
                                    { "man2-1611-de3-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:1502:10d:d4e2:0:3936"; };
                                    { "man2-1620-64a-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:1c96:10d:d4e4:0:3936"; };
                                    { "man2-1621-dfe-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26a6:10d:d4e4:0:3936"; };
                                    { "man2-1673-a8c-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2609:10d:d4e2:0:3936"; };
                                    { "man2-1674-9c6-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2619:10d:d4e4:0:3936"; };
                                    { "man2-1676-274-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2613:10d:d4e4:0:3936"; };
                                    { "man2-1677-3ec-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2606:10d:d4e4:0:3936"; };
                                    { "man2-1679-89c-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2697:10d:d4e2:0:3936"; };
                                    { "man2-1684-dc3-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2615:10d:d4e2:0:3936"; };
                                    { "man2-3550-e8f-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0b:6e23:10d:d4e4:0:3936"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
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
                                attempts = 3;
                                connection_attempts = 31;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "sas1-1210-d41-sas-asr-desktopg-70e-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:3924:10d:d409:0:4078"; };
                                    { "sas1-1931-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:1e02:10d:d409:0:4078"; };
                                    { "sas1-4613-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:8e11:10d:d407:0:55e6"; };
                                    { "sas1-5836-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4727:10d:d409:0:4078"; };
                                    { "sas1-5836-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4727:10d:d407:0:55e6"; };
                                    { "sas1-5837-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:80f:10d:d409:0:4078"; };
                                    { "sas1-5837-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:80f:10d:d407:0:55e6"; };
                                    { "sas1-5838-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:218:10d:d407:0:55e6"; };
                                    { "sas1-5839-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:192d:10d:d409:0:4078"; };
                                    { "sas1-5839-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:192d:10d:d407:0:55e6"; };
                                    { "sas1-5840-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:21a:10d:d409:0:4078"; };
                                    { "sas1-5841-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4708:10d:d407:0:55e6"; };
                                    { "sas1-5841-sas-asr-quasar-general-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4708:10d:d40b:0:4078"; };
                                    { "sas1-5842-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:191b:10d:d409:0:4078"; };
                                    { "sas1-5842-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:191b:10d:d407:0:55e6"; };
                                    { "sas1-5844-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:35a1:10d:d409:0:4078"; };
                                    { "sas1-5844-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:35a1:10d:d407:0:55e6"; };
                                    { "sas1-5846-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:3594:10d:d409:0:4078"; };
                                    { "sas1-5846-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:3594:10d:d407:0:55e6"; };
                                    { "sas1-5847-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:2715:10d:d407:0:55e6"; };
                                    { "sas1-5847-sas-asr-quasar-general-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:2715:10d:d40b:0:4078"; };
                                    { "sas1-5848-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4705:10d:d407:0:55e6"; };
                                    { "sas1-5848-sas-asr-quasar-general-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4705:10d:d40b:0:4078"; };
                                    { "sas1-5849-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:35ab:10d:d407:0:55e6"; };
                                    { "sas1-8155-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:600c:10d:d409:0:4078"; };
                                    { "sas2-0468-sas-asr-quasar-general-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c11:a3:10d:d40b:0:4078"; };
                                    { "sas2-1201-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:a207:10d:d409:0:4078"; };
                                    { "sas2-3202-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:f52b:10d:d407:0:55e6"; };
                                    { "sas2-3485-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:edaf:10d:d407:0:55e6"; };
                                    { "sas2-8929-b6d-sas-asr-desktopg-70e-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c14:108e:10d:d409:0:4078"; };
                                    { "sas3-0258-91a-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c14:4ea0:10d:d407:0:55e6"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
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
                                attempts = 3;
                                connection_attempts = 31;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "vla1-0255-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4f0e:10d:dcda:0:558e"; };
                                    { "vla1-0455-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:130e:10d:dcd8:0:2342"; };
                                    { "vla1-0694-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3489:10d:dcd8:0:2342"; };
                                    { "vla1-0811-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:351f:10d:dcda:0:558e"; };
                                    { "vla1-1279-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4d08:10d:dcda:0:558e"; };
                                    { "vla1-1352-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3787:10d:dcd8:0:2342"; };
                                    { "vla1-1392-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:420f:10d:dcda:0:558e"; };
                                    { "vla1-2979-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3818:10d:dcda:0:558e"; };
                                    { "vla1-2979-vla-asr-quasar-general-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3818:10d:dcdb:0:4e78"; };
                                    { "vla1-3686-vla-asr-quasar-general-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3e8a:10d:dcdb:0:4e78"; };
                                    { "vla1-3700-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4003:10d:dcda:0:558e"; };
                                    { "vla1-3744-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:318b:10d:dcd8:0:2342"; };
                                    { "vla1-3822-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3718:10d:dcd8:0:2342"; };
                                    { "vla1-3861-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3e99:10d:dcda:0:558e"; };
                                    { "vla1-3915-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:329d:10d:dcd8:0:2342"; };
                                    { "vla1-4155-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4614:10d:dcda:0:558e"; };
                                    { "vla1-4221-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4e0d:10d:dcd8:0:2342"; };
                                    { "vla1-4363-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4e08:10d:dcd8:0:2342"; };
                                    { "vla2-5856-668-vla-asr-desktopg-7f2-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:268c:10d:dcda:0:558e"; };
                                    { "vla2-5867-595-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:26a0:10d:dcd8:0:2342"; };
                                    { "vla2-5868-cc6-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:361f:10d:dcd8:0:2342"; };
                                    { "vla2-5883-6d7-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:2690:10d:dcd8:0:2342"; };
                                    { "vla2-5896-d69-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:360f:10d:dcd8:0:2342"; };
                                    { "vla2-5897-3d7-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0f:4322:10d:dcd8:0:2342"; };
                                    { "vla2-5899-9b8-vla-asr-quasar-g-691-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:26a2:10d:dcdb:0:4e78"; };
                                    { "vla2-5899-fd8-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:26a2:10d:dcd8:0:2342"; };
                                    { "vla2-7705-473-vla-asr-desktopg-7f2-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:1301:10d:dcda:0:558e"; };
                                    { "vla2-7709-3f4-vla-asr-desktopg-7f2-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:da1:10d:dcda:0:558e"; };
                                    { "vla2-7710-907-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:d9f:10d:dcd8:0:2342"; };
                                    { "vla2-7714-f28-vla-asr-desktopg-7f2-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:1322:10d:dcda:0:558e"; };
                                    { "vla2-7737-c9c-vla-asr-quasar-g-691-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:1309:10d:dcdb:0:4e78"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
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
                dialogeneralgpu = {
                  priority = 8;
                  match_fsm = {
                    URI = "/ru-ru/dialogeneralgpu/(.*)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  report = {
                    uuid = "dialogeneralgpu";
                    ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    rewrite = {
                      actions = {
                        {
                          split = "url";
                          global = false;
                          literal = false;
                          rewrite = "/%1";
                          case_insensitive = false;
                          regexp = "/ru-ru/dialogeneralgpu/(.*)";
                        };
                      }; -- actions
                      balancer2 = {
                        unique_policy = {};
                        attempts = 2;
                        rr = {
                          weights_file = "./controls/traffic_control.weights";
                          dialogeneralgpu = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogeneralgpu";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 1;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "vla2-1393-1ea-alice-gpu-32668.gencfg-c.yandex.net"; 80; 2000.000; "2a02:6b8:c15:3d0c:10d:d6a2:0:7f9c"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "300ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogeneralgpu
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- rewrite
                  }; -- report
                }; -- dialogeneralgpu
                dialogeneral = {
                  priority = 7;
                  match_fsm = {
                    URI = "/ru-ru/dialogeneral/(.*)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  report = {
                    uuid = "dialogeneral";
                    ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    rewrite = {
                      actions = {
                        {
                          split = "url";
                          global = false;
                          literal = false;
                          rewrite = "/%1";
                          case_insensitive = false;
                          regexp = "/ru-ru/dialogeneral/(.*)";
                        };
                      }; -- actions
                      balancer2 = {
                        by_name_policy = {
                          name = get_geo("dialogeneral_", "random");
                          unique_policy = {};
                        }; -- by_name_policy
                        attempts = 2;
                        rr = {
                          weights_file = "./controls/traffic_control.weights";
                          dialogeneral_man = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogeneral_to_man";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 108;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "man2-0964-2bb-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22a2:10d:d4e1:0:6f31"; };
                                    { "man2-0964-42f-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22a2:10d:d4e1:0:2420"; };
                                    { "man2-0966-a81-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:220e:10d:d4e1:0:2420"; };
                                    { "man2-0966-ca9-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:220e:10d:d4e1:0:6f31"; };
                                    { "man2-0967-2b8-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22a9:10d:d4e1:0:6f31"; };
                                    { "man2-0967-d20-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22a9:10d:d4e1:0:2420"; };
                                    { "man2-0968-322-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2210:10d:d4e1:0:2420"; };
                                    { "man2-0968-edd-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2210:10d:d4e1:0:6f31"; };
                                    { "man2-0969-b24-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2207:10d:d4e1:0:6f31"; };
                                    { "man2-0969-d15-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2207:10d:d4e1:0:2420"; };
                                    { "man2-0971-b8e-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22a3:10d:d4e1:0:6f31"; };
                                    { "man2-0971-ba2-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22a3:10d:d4e1:0:2420"; };
                                    { "man2-0972-1dd-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2216:10d:d4e1:0:6f31"; };
                                    { "man2-0972-f1f-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2216:10d:d4e1:0:2420"; };
                                    { "man2-0973-4f2-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:229e:10d:d4e1:0:2420"; };
                                    { "man2-0973-993-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:229e:10d:d4e1:0:6f31"; };
                                    { "man2-0974-333-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:220c:10d:d4e1:0:2420"; };
                                    { "man2-0974-a69-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:220c:10d:d4e1:0:6f31"; };
                                    { "man2-0975-223-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2209:10d:d4e1:0:2420"; };
                                    { "man2-0975-ecf-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2209:10d:d4e1:0:6f31"; };
                                    { "man2-0976-50f-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2293:10d:d4e1:0:6f31"; };
                                    { "man2-0976-b7c-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2293:10d:d4e1:0:2420"; };
                                    { "man2-0977-e41-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2282:10d:d4e1:0:6f31"; };
                                    { "man2-0977-e47-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2282:10d:d4e1:0:2420"; };
                                    { "man2-0978-562-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:228c:10d:d4e1:0:6f31"; };
                                    { "man2-0978-793-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:228c:10d:d4e1:0:2420"; };
                                    { "man2-0979-027-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2213:10d:d4e1:0:6f31"; };
                                    { "man2-0979-e7c-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2213:10d:d4e1:0:2420"; };
                                    { "man2-0980-20e-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2284:10d:d4e1:0:2420"; };
                                    { "man2-0980-54e-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2284:10d:d4e1:0:6f31"; };
                                    { "man2-0981-057-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22af:10d:d4e1:0:2420"; };
                                    { "man2-0981-0a0-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22af:10d:d4e1:0:6f31"; };
                                    { "man2-0982-7cd-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22a6:10d:d4e1:0:2420"; };
                                    { "man2-0982-dd1-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:22a6:10d:d4e1:0:6f31"; };
                                    { "man2-0983-b4d-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2201:10d:d4e1:0:6f31"; };
                                    { "man2-0983-f18-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2201:10d:d4e1:0:2420"; };
                                    { "man2-1456-1ad-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:232d:10d:d4e1:0:6f31"; };
                                    { "man2-1456-955-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:232d:10d:d4e1:0:2420"; };
                                    { "man2-1465-284-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:232f:10d:d4e1:0:6f31"; };
                                    { "man2-1465-f75-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:232f:10d:d4e1:0:2420"; };
                                    { "man2-1466-c65-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:269c:10d:d4e1:0:2420"; };
                                    { "man2-1466-d49-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:269c:10d:d4e1:0:6f31"; };
                                    { "man2-1467-dcc-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:26ac:10d:d4e1:0:6f31"; };
                                    { "man2-1467-f00-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:26ac:10d:d4e1:0:2420"; };
                                    { "man2-1470-633-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2699:10d:d4e1:0:6f31"; };
                                    { "man2-1470-b83-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2699:10d:d4e1:0:2420"; };
                                    { "man2-1474-242-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2324:10d:d4e1:0:6f31"; };
                                    { "man2-1474-d2f-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2324:10d:d4e1:0:2420"; };
                                    { "man2-1476-a48-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2696:10d:d4e1:0:2420"; };
                                    { "man2-1476-bd5-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2696:10d:d4e1:0:6f31"; };
                                    { "man2-1477-8ea-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:232e:10d:d4e1:0:6f31"; };
                                    { "man2-1477-c6b-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:232e:10d:d4e1:0:2420"; };
                                    { "man2-1478-e30-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:231c:10d:d4e1:0:2420"; };
                                    { "man2-1478-f61-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:231c:10d:d4e1:0:6f31"; };
                                    { "man2-1479-174-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2327:10d:d4e1:0:6f31"; };
                                    { "man2-1479-b3e-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:2327:10d:d4e1:0:2420"; };
                                    { "man2-1481-338-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:268a:10d:d4e1:0:6f31"; };
                                    { "man2-1481-c3b-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:268a:10d:d4e1:0:2420"; };
                                    { "man2-1482-b42-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:269d:10d:d4e1:0:6f31"; };
                                    { "man2-1482-beb-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:269d:10d:d4e1:0:2420"; };
                                    { "man2-1484-3a2-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:231f:10d:d4e1:0:2420"; };
                                    { "man2-1484-fe9-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:231f:10d:d4e1:0:6f31"; };
                                    { "man2-1485-22f-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:269f:10d:d4e1:0:6f31"; };
                                    { "man2-1485-28f-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:269f:10d:d4e1:0:2420"; };
                                    { "man2-1580-640-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:150e:10d:d4e1:0:2420"; };
                                    { "man2-1580-77f-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:150e:10d:d4e1:0:6f31"; };
                                    { "man2-1594-481-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:1505:10d:d4e1:0:2420"; };
                                    { "man2-1594-f6f-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:1505:10d:d4e1:0:6f31"; };
                                    { "man2-1620-32c-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:1c96:10d:d4e1:0:6f31"; };
                                    { "man2-1620-b1b-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:1c96:10d:d4e1:0:2420"; };
                                    { "man2-1621-95d-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:26a6:10d:d4e1:0:2420"; };
                                    { "man2-1621-cfd-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c09:26a6:10d:d4e1:0:6f31"; };
                                    { "man2-1673-4e7-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2609:10d:d4e1:0:2420"; };
                                    { "man2-1673-ab4-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2609:10d:d4e1:0:6f31"; };
                                    { "man2-1674-21b-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2619:10d:d4e1:0:2420"; };
                                    { "man2-1674-d07-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2619:10d:d4e1:0:6f31"; };
                                    { "man2-1675-7eb-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:268c:10d:d4e1:0:2420"; };
                                    { "man2-1675-d9d-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:268c:10d:d4e1:0:6f31"; };
                                    { "man2-1676-407-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2613:10d:d4e1:0:6f31"; };
                                    { "man2-1676-637-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2613:10d:d4e1:0:2420"; };
                                    { "man2-1677-a09-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2606:10d:d4e1:0:6f31"; };
                                    { "man2-1677-d78-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2606:10d:d4e1:0:2420"; };
                                    { "man2-1678-006-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2623:10d:d4e1:0:6f31"; };
                                    { "man2-1678-a9f-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2623:10d:d4e1:0:2420"; };
                                    { "man2-1679-aa5-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2697:10d:d4e1:0:6f31"; };
                                    { "man2-1679-c1f-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2697:10d:d4e1:0:2420"; };
                                    { "man2-1680-089-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2617:10d:d4e1:0:6f31"; };
                                    { "man2-1680-f7d-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2617:10d:d4e1:0:2420"; };
                                    { "man2-1681-460-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2694:10d:d4e1:0:2420"; };
                                    { "man2-1681-82f-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2694:10d:d4e1:0:6f31"; };
                                    { "man2-1682-515-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2600:10d:d4e1:0:2420"; };
                                    { "man2-1682-b7c-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2600:10d:d4e1:0:6f31"; };
                                    { "man2-1683-87f-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2603:10d:d4e1:0:2420"; };
                                    { "man2-1683-8df-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2603:10d:d4e1:0:6f31"; };
                                    { "man2-1684-b52-man-asr-dialogeneral-28465.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2615:10d:d4e1:0:6f31"; };
                                    { "man2-1684-ddd-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:2615:10d:d4e1:0:2420"; };
                                    { "man2-1722-d96-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0a:1b18:10d:d4e1:0:2420"; };
                                    { "man2-2282-db8-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:1388:10d:d4e1:0:2420"; };
                                    { "man2-2283-4d9-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:130f:10d:d4e1:0:2420"; };
                                    { "man2-2284-0ab-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:1315:10d:d4e1:0:2420"; };
                                    { "man2-2285-59e-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:269b:10d:d4e1:0:2420"; };
                                    { "man2-2286-771-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:139f:10d:d4e1:0:2420"; };
                                    { "man2-2287-0d7-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:13a1:10d:d4e1:0:2420"; };
                                    { "man2-2294-9eb-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:1313:10d:d4e1:0:2420"; };
                                    { "man2-2296-1bf-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:1391:10d:d4e1:0:2420"; };
                                    { "man2-2297-704-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:130b:10d:d4e1:0:2420"; };
                                    { "man2-2304-68a-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:1319:10d:d4e1:0:2420"; };
                                    { "man2-2307-bed-man-asr-dialogeneral-9248.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c13:1383:10d:d4e1:0:2420"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                    allow_connection_upgrade_without_connection_header = true;
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogeneral_man
                          dialogeneral_sas = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogeneral_to_sas";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 48;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "sas1-1128-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4e81:10d:d403:0:4762"; };
                                    { "sas1-1399-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4e82:10d:d403:0:4762"; };
                                    { "sas1-1504-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4ead:10d:d403:0:4762"; };
                                    { "sas1-5784-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4728:10d:d403:0:4762"; };
                                    { "sas1-5788-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:471c:10d:d403:0:4762"; };
                                    { "sas1-5794-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4724:10d:d403:0:4762"; };
                                    { "sas1-5795-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:220:10d:d403:0:4762"; };
                                    { "sas1-5797-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4725:10d:d403:0:4762"; };
                                    { "sas1-5798-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:2719:10d:d403:0:4762"; };
                                    { "sas1-5799-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:470b:10d:d403:0:4762"; };
                                    { "sas1-5800-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:471f:10d:d403:0:4762"; };
                                    { "sas1-5801-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:1914:10d:d403:0:4762"; };
                                    { "sas1-5803-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:271a:10d:d403:0:4762"; };
                                    { "sas1-5804-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4711:10d:d403:0:4762"; };
                                    { "sas1-5805-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4710:10d:d403:0:4762"; };
                                    { "sas1-5806-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:191c:10d:d403:0:4762"; };
                                    { "sas1-5807-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4709:10d:d403:0:4762"; };
                                    { "sas1-5808-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:1911:10d:d403:0:4762"; };
                                    { "sas1-5810-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:2709:10d:d403:0:4762"; };
                                    { "sas1-5811-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:190d:10d:d403:0:4762"; };
                                    { "sas1-5812-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:80b:10d:d403:0:4762"; };
                                    { "sas1-5813-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:270d:10d:d403:0:4762"; };
                                    { "sas1-5814-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:471b:10d:d403:0:4762"; };
                                    { "sas1-5815-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:190c:10d:d403:0:4762"; };
                                    { "sas1-5816-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:2710:10d:d403:0:4762"; };
                                    { "sas1-5817-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:203:10d:d403:0:4762"; };
                                    { "sas1-5818-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:191a:10d:d403:0:4762"; };
                                    { "sas1-5819-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:3599:10d:d403:0:4762"; };
                                    { "sas1-5820-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:470e:10d:d403:0:4762"; };
                                    { "sas1-5821-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:359a:10d:d403:0:4762"; };
                                    { "sas1-5822-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:202:10d:d403:0:4762"; };
                                    { "sas1-5823-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:1924:10d:d403:0:4762"; };
                                    { "sas1-5824-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:35a3:10d:d403:0:4762"; };
                                    { "sas1-5825-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:229:10d:d403:0:4762"; };
                                    { "sas1-5826-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:35a4:10d:d403:0:4762"; };
                                    { "sas1-5827-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:272d:10d:d403:0:4762"; };
                                    { "sas1-5829-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:1910:10d:d403:0:4762"; };
                                    { "sas1-5830-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:3590:10d:d403:0:4762"; };
                                    { "sas1-5831-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:35a5:10d:d403:0:4762"; };
                                    { "sas1-5833-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:1929:10d:d403:0:4762"; };
                                    { "sas1-5834-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:81e:10d:d403:0:4762"; };
                                    { "sas1-5835-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:80c:10d:d403:0:4762"; };
                                    { "sas1-6362-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:3ca0:10d:d403:0:4762"; };
                                    { "sas1-9193-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:4f24:10d:d403:0:4762"; };
                                    { "sas2-0476-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c11:87:10d:d403:0:4762"; };
                                    { "sas2-1142-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:b61d:10d:d403:0:4762"; };
                                    { "sas2-1154-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:b620:10d:d403:0:4762"; };
                                    { "sas2-1158-sas-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c08:b61c:10d:d403:0:4762"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogeneral_sas
                          dialogeneral_vla = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogeneral_to_vla";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 60;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "vla1-0465-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:1317:10d:dcd6:0:4762"; };
                                    { "vla1-0957-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:378f:10d:dcd6:0:4762"; };
                                    { "vla1-1337-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:378a:10d:dcd6:0:4762"; };
                                    { "vla1-1519-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3483:10d:dcd6:0:4762"; };
                                    { "vla1-1537-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:4893:10d:dcd6:0:4762"; };
                                    { "vla1-1560-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3492:10d:dcd6:0:4762"; };
                                    { "vla1-1591-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3509:10d:dcd6:0:4762"; };
                                    { "vla1-1631-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:1318:10d:dcd6:0:4762"; };
                                    { "vla1-1649-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:378d:10d:dcd6:0:4762"; };
                                    { "vla1-1661-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3782:10d:dcd6:0:4762"; };
                                    { "vla1-1802-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:2688:10d:dcd6:0:4762"; };
                                    { "vla1-2095-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:1304:10d:dcd6:0:4762"; };
                                    { "vla1-2383-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3e1c:10d:dcd6:0:4762"; };
                                    { "vla1-2419-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3e01:10d:dcd6:0:4762"; };
                                    { "vla1-2514-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:458d:10d:dcd6:0:4762"; };
                                    { "vla1-2539-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:5110:10d:dcd6:0:4762"; };
                                    { "vla1-2625-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:f8a:10d:dcd6:0:4762"; };
                                    { "vla1-2639-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:178e:10d:dcd6:0:4762"; };
                                    { "vla1-2652-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:f97:10d:dcd6:0:4762"; };
                                    { "vla1-2756-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:895:10d:dcd6:0:4762"; };
                                    { "vla1-2951-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:1783:10d:dcd6:0:4762"; };
                                    { "vla1-3005-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:381b:10d:dcd6:0:4762"; };
                                    { "vla1-3069-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3613:10d:dcd6:0:4762"; };
                                    { "vla1-3101-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:890:10d:dcd6:0:4762"; };
                                    { "vla1-3200-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:231f:10d:dcd6:0:4762"; };
                                    { "vla1-3216-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3a1f:10d:dcd6:0:4762"; };
                                    { "vla1-3262-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:c8e:10d:dcd6:0:4762"; };
                                    { "vla1-3346-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3984:10d:dcd6:0:4762"; };
                                    { "vla1-3549-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:2318:10d:dcd6:0:4762"; };
                                    { "vla1-3601-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:2305:10d:dcd6:0:4762"; };
                                    { "vla1-3646-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3294:10d:dcd6:0:4762"; };
                                    { "vla1-3669-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3922:10d:dcd6:0:4762"; };
                                    { "vla1-3715-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:5084:10d:dcd6:0:4762"; };
                                    { "vla1-3807-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:c97:10d:dcd6:0:4762"; };
                                    { "vla1-3888-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3198:10d:dcd6:0:4762"; };
                                    { "vla1-3896-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3991:10d:dcd6:0:4762"; };
                                    { "vla1-3967-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:4c90:10d:dcd6:0:4762"; };
                                    { "vla1-3986-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:4c88:10d:dcd6:0:4762"; };
                                    { "vla1-4043-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:4016:10d:dcd6:0:4762"; };
                                    { "vla1-4240-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3ea3:10d:dcd6:0:4762"; };
                                    { "vla1-4242-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3e16:10d:dcd6:0:4762"; };
                                    { "vla1-4286-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3e0d:10d:dcd6:0:4762"; };
                                    { "vla1-4368-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:4c87:10d:dcd6:0:4762"; };
                                    { "vla1-4510-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:4606:10d:dcd6:0:4762"; };
                                    { "vla1-4520-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:460c:10d:dcd6:0:4762"; };
                                    { "vla1-4551-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c0d:3b98:10d:dcd6:0:4762"; };
                                    { "vla2-5847-676-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:361d:10d:dcd6:0:4762"; };
                                    { "vla2-5855-c8e-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:2686:10d:dcd6:0:4762"; };
                                    { "vla2-5870-de6-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:360b:10d:dcd6:0:4762"; };
                                    { "vla2-5872-5e6-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:3620:10d:dcd6:0:4762"; };
                                    { "vla2-5876-bae-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:361a:10d:dcd6:0:4762"; };
                                    { "vla2-5877-f60-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:3618:10d:dcd6:0:4762"; };
                                    { "vla2-5880-405-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:269a:10d:dcd6:0:4762"; };
                                    { "vla2-5881-fac-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:269c:10d:dcd6:0:4762"; };
                                    { "vla2-5882-522-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:2684:10d:dcd6:0:4762"; };
                                    { "vla2-5889-4a8-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c15:361b:10d:dcd6:0:4762"; };
                                    { "vla2-7705-7de-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c18:1301:10d:dcd6:0:4762"; };
                                    { "vla2-7709-637-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c18:da1:10d:dcd6:0:4762"; };
                                    { "vla2-7710-1cc-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c18:d9f:10d:dcd6:0:4762"; };
                                    { "vla2-7714-fba-vla-asr-dialogeneral-18274.gencfg-c.yandex.net"; 80; 800.000; "2a02:6b8:c18:1322:10d:dcd6:0:4762"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogeneral_vla
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- rewrite
                  }; -- report
                }; -- dialogeneral
                dialogmaps = {
                  priority = 6;
                  match_fsm = {
                    URI = "/ru-ru/(autolauncher|dialogmaps)/(.*)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  report = {
                    uuid = "dialogmaps";
                    ranges = "100ms,300ms,500ms,1000ms,3s";
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    rewrite = {
                      actions = {
                        {
                          split = "url";
                          global = false;
                          literal = false;
                          rewrite = "/%2";
                          case_insensitive = false;
                          regexp = "/ru-ru/(autolauncher|dialogmaps)/(.*)";
                        };
                      }; -- actions
                      balancer2 = {
                        by_name_policy = {
                          name = get_geo("dialogmaps_", "random");
                          unique_policy = {};
                        }; -- by_name_policy
                        attempts = 2;
                        rr = {
                          weights_file = "./controls/traffic_control.weights";
                          dialogmaps_man = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogmaps_to_man";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 15;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "man2-0965-287-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:22aa:10d:d4e2:0:3936"; };
                                    { "man2-0970-710-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:221f:10d:d4e2:0:3936"; };
                                    { "man2-1460-e3f-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26a4:10d:d4e2:0:3936"; };
                                    { "man2-1461-306-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:231e:10d:d4e2:0:3936"; };
                                    { "man2-1464-848-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26a5:10d:d4e2:0:3936"; };
                                    { "man2-1473-2a5-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26af:10d:d4e2:0:3936"; };
                                    { "man2-1476-3a1-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:2696:10d:d4e2:0:3936"; };
                                    { "man2-1478-ce6-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:231c:10d:d4e2:0:3936"; };
                                    { "man2-1485-36b-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:269f:10d:d4e2:0:3936"; };
                                    { "man2-1590-137-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:151c:10d:d4e2:0:3936"; };
                                    { "man2-1607-58d-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:1504:10d:d4e2:0:3936"; };
                                    { "man2-1611-de3-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:1502:10d:d4e2:0:3936"; };
                                    { "man2-1673-a8c-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2609:10d:d4e2:0:3936"; };
                                    { "man2-1679-89c-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2697:10d:d4e2:0:3936"; };
                                    { "man2-1684-dc3-man-asr-dialogmaps-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2615:10d:d4e2:0:3936"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogmaps_man
                          dialogmaps_sas = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogmaps_to_sas";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 15;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "sas1-4613-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:8e11:10d:d407:0:55e6"; };
                                    { "sas1-5836-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4727:10d:d407:0:55e6"; };
                                    { "sas1-5837-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:80f:10d:d407:0:55e6"; };
                                    { "sas1-5838-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:218:10d:d407:0:55e6"; };
                                    { "sas1-5839-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:192d:10d:d407:0:55e6"; };
                                    { "sas1-5841-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4708:10d:d407:0:55e6"; };
                                    { "sas1-5842-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:191b:10d:d407:0:55e6"; };
                                    { "sas1-5844-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:35a1:10d:d407:0:55e6"; };
                                    { "sas1-5846-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:3594:10d:d407:0:55e6"; };
                                    { "sas1-5847-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:2715:10d:d407:0:55e6"; };
                                    { "sas1-5848-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4705:10d:d407:0:55e6"; };
                                    { "sas1-5849-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:35ab:10d:d407:0:55e6"; };
                                    { "sas2-3202-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:f52b:10d:d407:0:55e6"; };
                                    { "sas2-3485-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:edaf:10d:d407:0:55e6"; };
                                    { "sas3-0258-91a-sas-asr-dialogmaps-21990.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c14:4ea0:10d:d407:0:55e6"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogmaps_sas
                          dialogmaps_vla = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogmaps_to_vla";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 15;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "vla1-0455-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:130e:10d:dcd8:0:2342"; };
                                    { "vla1-0694-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3489:10d:dcd8:0:2342"; };
                                    { "vla1-1352-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3787:10d:dcd8:0:2342"; };
                                    { "vla1-3744-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:318b:10d:dcd8:0:2342"; };
                                    { "vla1-3822-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3718:10d:dcd8:0:2342"; };
                                    { "vla1-3915-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:329d:10d:dcd8:0:2342"; };
                                    { "vla1-4221-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4e0d:10d:dcd8:0:2342"; };
                                    { "vla1-4363-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4e08:10d:dcd8:0:2342"; };
                                    { "vla2-5867-595-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:26a0:10d:dcd8:0:2342"; };
                                    { "vla2-5868-cc6-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:361f:10d:dcd8:0:2342"; };
                                    { "vla2-5883-6d7-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:2690:10d:dcd8:0:2342"; };
                                    { "vla2-5896-d69-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:360f:10d:dcd8:0:2342"; };
                                    { "vla2-5897-3d7-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0f:4322:10d:dcd8:0:2342"; };
                                    { "vla2-5899-fd8-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:26a2:10d:dcd8:0:2342"; };
                                    { "vla2-7710-907-vla-asr-dialogmaps-9026.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:d9f:10d:dcd8:0:2342"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogmaps_vla
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- rewrite
                  }; -- report
                }; -- dialogmaps
                desktop_general = {
                  priority = 5;
                  match_fsm = {
                    URI = "/ru-ru/desktopgeneral/(.*)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  report = {
                    uuid = "desktopgeneral";
                    ranges = "100ms,300ms,500ms,1000ms,3s";
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    rewrite = {
                      actions = {
                        {
                          split = "url";
                          global = false;
                          literal = false;
                          rewrite = "/%1";
                          case_insensitive = false;
                          regexp = "/ru-ru/desktopgeneral/(.*)";
                        };
                      }; -- actions
                      balancer2 = {
                        by_name_policy = {
                          name = get_geo("desktopgeneral_", "random");
                          unique_policy = {};
                        }; -- by_name_policy
                        attempts = 2;
                        rr = {
                          weights_file = "./controls/traffic_control.weights";
                          desktopgeneral_man = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_desktopgeneral_to_man";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 12;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "man1-6670-man-asr-desktopgeneral-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0b:172:10d:d4e4:0:3936"; };
                                    { "man1-7385-man-asr-desktopgeneral-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:12c:10d:d4e4:0:3936"; };
                                    { "man2-1469-3f1-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:2314:10d:d4e4:0:3936"; };
                                    { "man2-1479-ad3-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:2327:10d:d4e4:0:3936"; };
                                    { "man2-1481-ffe-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:268a:10d:d4e4:0:3936"; };
                                    { "man2-1483-01c-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26ad:10d:d4e4:0:3936"; };
                                    { "man2-1620-64a-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:1c96:10d:d4e4:0:3936"; };
                                    { "man2-1621-dfe-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:26a6:10d:d4e4:0:3936"; };
                                    { "man2-1674-9c6-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2619:10d:d4e4:0:3936"; };
                                    { "man2-1676-274-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2613:10d:d4e4:0:3936"; };
                                    { "man2-1677-3ec-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:2606:10d:d4e4:0:3936"; };
                                    { "man2-3550-e8f-man-asr-desktopg-c72-14646.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0b:6e23:10d:d4e4:0:3936"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- desktopgeneral_man
                          desktopgeneral_sas = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_desktopgeneral_to_sas";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 12;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "sas1-1210-d41-sas-asr-desktopg-70e-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:3924:10d:d409:0:4078"; };
                                    { "sas1-1931-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:1e02:10d:d409:0:4078"; };
                                    { "sas1-5836-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4727:10d:d409:0:4078"; };
                                    { "sas1-5837-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:80f:10d:d409:0:4078"; };
                                    { "sas1-5839-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:192d:10d:d409:0:4078"; };
                                    { "sas1-5840-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:21a:10d:d409:0:4078"; };
                                    { "sas1-5842-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:191b:10d:d409:0:4078"; };
                                    { "sas1-5844-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:35a1:10d:d409:0:4078"; };
                                    { "sas1-5846-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:3594:10d:d409:0:4078"; };
                                    { "sas1-8155-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:600c:10d:d409:0:4078"; };
                                    { "sas2-1201-sas-asr-desktopgeneral-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:a207:10d:d409:0:4078"; };
                                    { "sas2-8929-b6d-sas-asr-desktopg-70e-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c14:108e:10d:d409:0:4078"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- desktopgeneral_sas
                          desktopgeneral_vla = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_desktopgeneral_to_vla";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 12;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "vla1-0255-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4f0e:10d:dcda:0:558e"; };
                                    { "vla1-0811-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:351f:10d:dcda:0:558e"; };
                                    { "vla1-1279-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4d08:10d:dcda:0:558e"; };
                                    { "vla1-1392-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:420f:10d:dcda:0:558e"; };
                                    { "vla1-2979-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3818:10d:dcda:0:558e"; };
                                    { "vla1-3700-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4003:10d:dcda:0:558e"; };
                                    { "vla1-3861-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3e99:10d:dcda:0:558e"; };
                                    { "vla1-4155-vla-asr-desktopgeneral-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:4614:10d:dcda:0:558e"; };
                                    { "vla2-5856-668-vla-asr-desktopg-7f2-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:268c:10d:dcda:0:558e"; };
                                    { "vla2-7705-473-vla-asr-desktopg-7f2-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:1301:10d:dcda:0:558e"; };
                                    { "vla2-7709-3f4-vla-asr-desktopg-7f2-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:da1:10d:dcda:0:558e"; };
                                    { "vla2-7714-f28-vla-asr-desktopg-7f2-21902.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:1322:10d:dcda:0:558e"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- desktopgeneral_vla
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- rewrite
                  }; -- report
                }; -- desktop_general
                dialogeneralfast = {
                  priority = 4;
                  match_fsm = {
                    URI = "/ru-ru/dialogeneralfast/(.*)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  report = {
                    uuid = "dialogeneralfast";
                    ranges = "100ms,300ms,500ms,1000ms,3s";
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    rewrite = {
                      actions = {
                        {
                          split = "url";
                          global = false;
                          literal = false;
                          rewrite = "/%1";
                          case_insensitive = false;
                          regexp = "/ru-ru/dialogeneralfast/(.*)";
                        };
                      }; -- actions
                      balancer2 = {
                        by_name_policy = {
                          name = get_geo("dialogeneralfast_", "random");
                          unique_policy = {};
                        }; -- by_name_policy
                        attempts = 2;
                        rr = {
                          weights_file = "./controls/traffic_control.weights";
                          dialogeneralfast_man = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogeneralfast_to_man";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 10;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "man1-1930-man-asr-dialogeneral-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:2870:10d:d9fe:0:553c"; };
                                    { "man2-1468-ec5-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c09:2325:10d:d9fe:0:553c"; };
                                    { "man2-1579-83b-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c09:1c95:10d:d9fe:0:553c"; };
                                    { "man2-1585-6a0-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:1518:10d:d9fe:0:553c"; };
                                    { "man2-1586-03c-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:1516:10d:d9fe:0:553c"; };
                                    { "man2-1604-8b5-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:1522:10d:d9fe:0:553c"; };
                                    { "man2-1609-a4f-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:149b:10d:d9fe:0:553c"; };
                                    { "man2-1611-c2f-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:1502:10d:d9fe:0:553c"; };
                                    { "man2-1613-6b6-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:149d:10d:d9fe:0:553c"; };
                                    { "man2-1614-2ff-man-asr-dialogen-a1c-21820.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:1510:10d:d9fe:0:553c"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogeneralfast_man
                          dialogeneralfast_sas = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogeneralfast_to_sas";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 10;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "sas1-0391-sas-asr-dialogeneral-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:131a:10d:da02:0:4d8c"; };
                                    { "sas1-4159-sas-asr-dialogeneral-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:7f1d:10d:da02:0:4d8c"; };
                                    { "sas1-4370-sas-asr-dialogeneral-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:3003:10d:da02:0:4d8c"; };
                                    { "sas1-4417-sas-asr-dialogeneral-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:23a1:10d:da02:0:4d8c"; };
                                    { "sas1-4529-sas-asr-dialogeneral-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:21ad:10d:da02:0:4d8c"; };
                                    { "sas1-4692-sas-asr-dialogeneral-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:2121:10d:da02:0:4d8c"; };
                                    { "sas1-4931-sas-asr-dialogeneral-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:7c11:10d:da02:0:4d8c"; };
                                    { "sas1-6620-sas-asr-dialogeneral-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:5d0e:10d:da02:0:4d8c"; };
                                    { "sas2-6258-adf-sas-asr-dialogen-9ac-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c14:1e23:10d:da02:0:4d8c"; };
                                    { "slovo119-sas-asr-dialogeneral-fast-19852.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:6b00:10d:da02:0:4d8c"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogeneralfast_sas
                          dialogeneralfast_vla = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_dialogeneralfast_to_vla";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 10;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "vla2-5852-983-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:2696:10d:dcde:0:6f28"; };
                                    { "vla2-5853-47b-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:2685:10d:dcde:0:6f28"; };
                                    { "vla2-5856-e98-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:268c:10d:dcde:0:6f28"; };
                                    { "vla2-5861-c87-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:361c:10d:dcde:0:6f28"; };
                                    { "vla2-5871-293-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:3621:10d:dcde:0:6f28"; };
                                    { "vla2-5877-72d-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:3618:10d:dcde:0:6f28"; };
                                    { "vla2-5881-f40-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:269c:10d:dcde:0:6f28"; };
                                    { "vla2-5891-03e-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:3606:10d:dcde:0:6f28"; };
                                    { "vla2-5896-924-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:360f:10d:dcde:0:6f28"; };
                                    { "vla2-5897-0f8-vla-asr-dialogen-610-28456.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0f:4322:10d:dcde:0:6f28"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- dialogeneralfast_vla
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- rewrite
                  }; -- report
                }; -- dialogeneralfast
                quasar = {
                  priority = 3;
                  match_fsm = {
                    URI = "/ru-ru/quasar-general/(.*)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  report = {
                    uuid = "quasargeneral";
                    ranges = "100ms,300ms,500ms,1000ms,3s";
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    rewrite = {
                      actions = {
                        {
                          split = "url";
                          global = false;
                          literal = false;
                          rewrite = "/%1";
                          case_insensitive = false;
                          regexp = "/ru-ru/quasar-general/(.*)";
                        };
                      }; -- actions
                      balancer2 = {
                        by_name_policy = {
                          name = get_geo("quasargeneral_", "random");
                          unique_policy = {};
                        }; -- by_name_policy
                        attempts = 2;
                        rr = {
                          weights_file = "./controls/traffic_control.weights";
                          quasargeneral_man = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_quasargeneral_to_man";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 3;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "man2-1474-c29-man-asr-quasar-g-625-18274.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:2324:10d:d4e5:0:4762"; };
                                    { "man2-1477-b64-man-asr-quasar-g-625-18274.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:232e:10d:d4e5:0:4762"; };
                                    { "man2-1482-af2-man-asr-quasar-g-625-18274.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c09:269d:10d:d4e5:0:4762"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- quasargeneral_man
                          quasargeneral_sas = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_quasargeneral_to_sas";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 4;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "sas1-5841-sas-asr-quasar-general-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4708:10d:d40b:0:4078"; };
                                    { "sas1-5847-sas-asr-quasar-general-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:2715:10d:d40b:0:4078"; };
                                    { "sas1-5848-sas-asr-quasar-general-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c08:4705:10d:d40b:0:4078"; };
                                    { "sas2-0468-sas-asr-quasar-general-16504.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c11:a3:10d:d40b:0:4078"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- quasargeneral_sas
                          quasargeneral_vla = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_quasargeneral_to_vla";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 4;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "vla1-2979-vla-asr-quasar-general-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3818:10d:dcdb:0:4e78"; };
                                    { "vla1-3686-vla-asr-quasar-general-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c0d:3e8a:10d:dcdb:0:4e78"; };
                                    { "vla2-5899-9b8-vla-asr-quasar-g-691-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:26a2:10d:dcdb:0:4e78"; };
                                    { "vla2-7737-c9c-vla-asr-quasar-g-691-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c18:1309:10d:dcdb:0:4e78"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- quasargeneral_vla
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- rewrite
                  }; -- report
                }; -- quasar
                quasar_spotter = {
                  priority = 2;
                  match_fsm = {
                    URI = "/ru-ru/quasar-spotter-check/(.*)";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  report = {
                    uuid = "quasarspotter";
                    ranges = "100ms,300ms,500ms,1000ms,3s";
                    just_storage = false;
                    disable_robotness = true;
                    disable_sslness = true;
                    events = {
                      stats = "report";
                    }; -- events
                    rewrite = {
                      actions = {
                        {
                          split = "url";
                          global = false;
                          literal = false;
                          rewrite = "/%1";
                          case_insensitive = false;
                          regexp = "/ru-ru/quasar-spotter-check/(.*)";
                        };
                      }; -- actions
                      balancer2 = {
                        by_name_policy = {
                          name = get_geo("quasarspotter_", "random");
                          unique_policy = {};
                        }; -- by_name_policy
                        attempts = 2;
                        rr = {
                          weights_file = "./controls/traffic_control.weights";
                          quasarspotter_man = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_quasarspotter_to_man";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 3;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "man2-1600-a39-man-asr-quasar-s-dbd-14038.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:1497:10d:d9fc:0:36d6"; };
                                    { "man2-1606-6bf-man-asr-quasar-s-dbd-14038.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:149f:10d:d9fc:0:36d6"; };
                                    { "man2-1609-62e-man-asr-quasar-s-dbd-14038.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c13:149b:10d:d9fc:0:36d6"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- quasarspotter_man
                          quasarspotter_sas = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_quasarspotter_to_sas";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 3;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "sas2-8127-081-sas-asr-quasar-s-3d9-12832.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c16:399:10d:d9fd:0:3220"; };
                                    { "sas2-8128-b79-sas-asr-quasar-s-3d9-12832.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c16:395:10d:d9fd:0:3220"; };
                                    { "sas2-8139-b48-sas-asr-quasar-s-3d9-12832.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c16:388:10d:d9fd:0:3220"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- quasarspotter_sas
                          quasarspotter_vla = {
                            weight = 1.000;
                            report = {
                              uuid = "requests_quasarspotter_to_vla";
                              ranges = "300ms,500ms,1s,2s,3s,4s,5s,10s";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                connection_attempts = 3;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "vla2-5853-2ed-vla-asr-quasar-s-02a-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:2685:10d:dcdd:0:4e78"; };
                                    { "vla2-5867-ce6-vla-asr-quasar-s-02a-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:26a0:10d:dcdd:0:4e78"; };
                                    { "vla2-5888-7f7-vla-asr-quasar-s-02a-20088.gencfg-c.yandex.net"; 80; 600.000; "2a02:6b8:c15:3619:10d:dcdd:0:4e78"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- quasarspotter_vla
                        }; -- rr
                        on_error = {
                          errordocument = {
                            status = 504;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- on_error
                      }; -- balancer2
                    }; -- rewrite
                  }; -- report
                }; -- quasar_spotter
                default = {
                  priority = 1;
                  errordocument = {
                    status = 404;
                    force_conn_close = false;
                  }; -- errordocument
                }; -- default
              }; -- regexp
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
  }; -- ipdispatch
}