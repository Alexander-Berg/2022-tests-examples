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


function get_ip_by_iproute(addr_family)
  if disable_external then
    if addr_family == "v4" then
      return "127.1.1.1"
    elseif addr_family == "v6" then
      return "127.2.2.2"
    else
      error("invalid parameter")
    end
  end

  local ipcmd
  if addr_family == "v4" then
    ipcmd = "ip route get 77.88.8.8 2>/dev/null| awk '/src/ {print $NF}'"
  elseif addr_family == "v6" then
    ipcmd = "ip route get 2a00:1450:4010:c05::65 2>/dev/null | grep -oE '2a02[:0-9a-f]+' | tail -1"
  else
    error("invalid parameter")
  end
  local handler = io.popen(ipcmd)
  local ip = handler:read("*l")
  handler:close()
  if ip == nil or ip == "" or ip == "proto" then
    return "127.0.0.2"
  end
  return ip
end


function get_log_path(name, port, default_log_dir)
  default_log_dir = default_log_dir or "/place/db/www/logs"
  rv = (log_dir or default_log_dir) .. "/current-" .. name .. "-balancer";
  if port ~= nil then
    rv = rv .. "-" .. port;
  end
  return rv
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
  log = get_log_path("childs_log", 16020, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 16020;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 16020;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 16020;
      ip = "127.0.0.4";
    };
    {
      port = 443;
      ip = "2a02:6b8::3:29";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "87.250.250.29";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 16021;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 16021;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 80;
      ip = "2a02:6b8::3:29";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "87.250.250.29";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 16020;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 16020;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::103c";
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
        16020;
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
        16020;
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
    https_section_443 = {
      ips = {
        "2a02:6b8::3:29";
        "87.250.250.29";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "7822873727505391694";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 16021, "/place/db/www/logs");
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
                log = get_log_path("ssl_sni", 16021, "/place/db/www/logs");
                priv = get_private_cert_path("collections.yandex.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-collections.yandex.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.collections.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.collections.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.collections.yandex.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 16021, "/place/db/www/logs");
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
                  threshold = {
                    lo_bytes = 420000;
                    hi_bytes = 10485760;
                    recv_timeout = "1s";
                    pass_timeout = "10s";
                    headers = {
                      create_func = {
                        ["X-Collections-Req-Id"] = "reqid";
                        ["X-Forwarded-For-Y"] = "realip";
                      }; -- create_func
                      create_func_weak = {
                        ["X-Req-Id"] = "reqid";
                      }; -- create_func_weak
                      log_headers = {
                        name_re = "User-Agent|X-Req-Id";
                        response_headers = {
                          delete = "uWSGI-encoding|uwsgi-encoding";
                          regexp = {
                            exp_testing = {
                              priority = 2;
                              match_fsm = {
                                cgi = "(exp-testing=da|exp_confs=testing)";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                              headers = {
                                create = {
                                  ["X-L7-EXP-Testing"] = "true";
                                }; -- create
                                shared = {
                                  uuid = "2763131386744859449";
                                  exp_getter = {
                                    trusted = false;
                                    file_switch = "./controls/expgetter.switch";
                                    service_name = "collections";
                                    service_name_header = "Y-Service";
                                    uaas = {
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
                                                      { "vla2-5623-b04-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4f81:10e:b569:0:37d2"; };
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
                                    }; -- uaas
                                    regexp = {
                                      ["awacs-balancer-health-check"] = {
                                        priority = 13;
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
                                        priority = 12;
                                        match_fsm = {
                                          url = "/slb_ping";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "1683180570280210434";
                                        }; -- shared
                                      }; -- slbping
                                      common_upstream_api_http_adapter = {
                                        priority = 11;
                                        match_fsm = {
                                          URI = "(/collections)?/api/v0\\..*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "5352998188831779523";
                                        }; -- shared
                                      }; -- common_upstream_api_http_adapter
                                      common_api_cards = {
                                        priority = 10;
                                        match_fsm = {
                                          URI = "(/collections)?/api/cards(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "831293184824121850";
                                        }; -- shared
                                      }; -- common_api_cards
                                      common_api_content = {
                                        priority = 9;
                                        match_fsm = {
                                          URI = "(/collections)?/api/content(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "4497430824356488439";
                                        }; -- shared
                                      }; -- common_api_content
                                      common_api_browser_bookmarks = {
                                        priority = 8;
                                        match_fsm = {
                                          URI = "(/collections)?/api/v1.0/browser/bookmarks(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "4932708923836649592";
                                        }; -- shared
                                      }; -- common_api_browser_bookmarks
                                      common_api_subscriptions_bulk = {
                                        priority = 7;
                                        match_fsm = {
                                          URI = "(/collections)?/api/subscriptions/bulk(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "8897984711881177651";
                                        }; -- shared
                                      }; -- common_api_subscriptions_bulk
                                      common_api_user = {
                                        priority = 6;
                                        match_fsm = {
                                          URI = "(/collections)?/api/user(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "753735758918296282";
                                        }; -- shared
                                      }; -- common_api_user
                                      common_api_verticals = {
                                        priority = 5;
                                        match_fsm = {
                                          URI = "(/collections)?/api/verticals/detect(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "8752760931700931323";
                                        }; -- shared
                                      }; -- common_api_verticals
                                      common_api = {
                                        priority = 4;
                                        match_fsm = {
                                          URI = "(/collections)?/api(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "5560322912116957292";
                                        }; -- shared
                                      }; -- common_api
                                      common_picture = {
                                        priority = 3;
                                        match_fsm = {
                                          URI = "(/collections)?/picture(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "1538615649558936508";
                                        }; -- shared
                                      }; -- common_picture
                                      common_sitemap = {
                                        priority = 2;
                                        match_fsm = {
                                          URI = "(/collections)?/sitemap(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "2451642220778517758";
                                        }; -- shared
                                      }; -- common_sitemap
                                      default = {
                                        priority = 1;
                                        shared = {
                                          uuid = "7074885229648664124";
                                        }; -- shared
                                      }; -- default
                                    }; -- regexp
                                  }; -- exp_getter
                                }; -- shared
                              }; -- headers
                            }; -- exp_testing
                            default = {
                              priority = 1;
                              headers = {
                                create = {
                                  ["X-L7-EXP"] = "true";
                                }; -- create
                                shared = {
                                  uuid = "2763131386744859449";
                                }; -- shared
                              }; -- headers
                            }; -- default
                          }; -- regexp
                        }; -- response_headers
                      }; -- log_headers
                    }; -- headers
                  }; -- threshold
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_16021 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        16021;
      }; -- ports
      shared = {
        uuid = "7822873727505391694";
      }; -- shared
    }; -- https_section_16021
    http_section_80 = {
      ips = {
        "2a02:6b8::3:29";
        "87.250.250.29";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "5668912436597156091";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 16020, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 16020, "/place/db/www/logs");
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
                headers = {
                  create_func_weak = {
                    ["X-Req-Id"] = "reqid";
                  }; -- create_func_weak
                  log_headers = {
                    name_re = "User-Agent|X-Req-Id";
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
                          url = "/slb_ping";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "1683180570280210434";
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
                        }; -- shared
                      }; -- slbping
                      default = {
                        priority = 1;
                        headers = {
                          create = {
                            Location = "1";
                          }; -- create
                          rewrite = {
                            actions = {
                              {
                                regexp = ".*";
                                global = false;
                                literal = false;
                                case_insensitive = false;
                                header_name = "Location";
                                rewrite = "https://%{host}%{url}";
                              };
                            }; -- actions
                            regexp = {
                              unsafe_methods = {
                                priority = 2;
                                match_fsm = {
                                  match = "(DELETE|PATCH|POST|PUT).*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                errordocument = {
                                  status = 307;
                                  force_conn_close = false;
                                  remain_headers = "Location";
                                }; -- errordocument
                              }; -- unsafe_methods
                              default = {
                                priority = 1;
                                errordocument = {
                                  status = 301;
                                  force_conn_close = false;
                                  remain_headers = "Location";
                                }; -- errordocument
                              }; -- default
                            }; -- regexp
                          }; -- rewrite
                        }; -- headers
                      }; -- default
                    }; -- regexp
                  }; -- log_headers
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_16020 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        16020;
      }; -- ports
      shared = {
        uuid = "5668912436597156091";
      }; -- shared
    }; -- http_section_16020
    internal_section = {
      ips = {
        "2a02:6b8:0:3400::103c";
      }; -- ips
      ports = {
        80;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 16020, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 16020, "/place/db/www/logs");
            report = {
              refers = "service_total,http";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              threshold = {
                lo_bytes = 420000;
                hi_bytes = 10485760;
                recv_timeout = "1s";
                pass_timeout = "10s";
                headers = {
                  create_func = {
                    ["X-Collections-Req-Id"] = "reqid";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Forwarded-For-Y"] = "realip";
                    ["X-Req-Id"] = "reqid";
                  }; -- create_func_weak
                  log_headers = {
                    name_re = "User-Agent|X-Req-Id";
                    regexp = {
                      ["awacs-balancer-health-check"] = {
                        priority = 27;
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
                        priority = 26;
                        match_fsm = {
                          url = "/slb_ping";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "1683180570280210434";
                        }; -- shared
                      }; -- slbping
                      int_light_hotfeed = {
                        priority = 25;
                        match_fsm = {
                          URI = "/pdb/light_hot_feed(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "light_hotfeed";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("hotfeed_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              hotfeed_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_light_hotfeed_to_sas";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "sas1-2982-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:2ba1:10b:4fcc:0:56f4"; };
                                        { "sas1-7589-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:5484:10b:4fcc:0:56f4"; };
                                        { "sas1-9080-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:4d95:10b:4fcc:0:56f4"; };
                                        { "sas2-5928-sas-pdb-feed-reader--cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c14:45a7:10b:4fcc:0:56f4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "60ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_sas
                              hotfeed_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_light_hotfeed_to_man";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "man1-4352-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:3cd9:10b:4fcd:0:4cf4"; };
                                        { "man1-4737-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:206:10b:4fcd:0:4cf4"; };
                                        { "man1-6171-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:8f6:10b:4fcd:0:4cf4"; };
                                        { "man1-7569-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:199b:10b:4fcd:0:4cf4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "60ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_man
                              hotfeed_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_light_hotfeed_to_vla";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "vla1-0498-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:1a04:10b:4fc9:0:42f4"; };
                                        { "vla1-0627-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:98d:10b:4fc9:0:42f4"; };
                                        { "vla1-1884-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:4989:10b:4fc9:0:42f4"; };
                                        { "vla1-2425-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:4aa2:10b:4fc9:0:42f4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "60ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_vla
                              hotfeed_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_light_hotfeed_to_devnull";
                                  ranges = "1ms";
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
                              }; -- hotfeed_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_light_hotfeed
                      int_feed_debug_info = {
                        priority = 24;
                        match_fsm = {
                          URI = "/pdb/hot_feed/quality_debug_info(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "feed_debug_info";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("hotfeed_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              hotfeed_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_feed_debug_info_to_sas";
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
                                    active = {
                                      delay = "16s";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "sas1-2982-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:2ba1:10b:4fcc:0:56f4"; };
                                        { "sas1-7589-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:5484:10b:4fcc:0:56f4"; };
                                        { "sas1-9080-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:4d95:10b:4fcc:0:56f4"; };
                                        { "sas2-5928-sas-pdb-feed-reader--cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c14:45a7:10b:4fcc:0:56f4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "2s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_sas
                              hotfeed_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_feed_debug_info_to_man";
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
                                    active = {
                                      delay = "16s";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "man1-4352-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:3cd9:10b:4fcd:0:4cf4"; };
                                        { "man1-4737-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:206:10b:4fcd:0:4cf4"; };
                                        { "man1-6171-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:8f6:10b:4fcd:0:4cf4"; };
                                        { "man1-7569-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:199b:10b:4fcd:0:4cf4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "2s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_man
                              hotfeed_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_feed_debug_info_to_vla";
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
                                    active = {
                                      delay = "16s";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "vla1-0498-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:1a04:10b:4fc9:0:42f4"; };
                                        { "vla1-0627-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:98d:10b:4fc9:0:42f4"; };
                                        { "vla1-1884-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:4989:10b:4fc9:0:42f4"; };
                                        { "vla1-2425-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:4aa2:10b:4fc9:0:42f4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "2s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_vla
                              hotfeed_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_feed_debug_info_to_devnull";
                                  ranges = "1ms";
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
                              }; -- hotfeed_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_feed_debug_info
                      int_hotfeed_sas = {
                        priority = 23;
                        match_fsm = {
                          URI = "/pdb/hot_feed(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "hotfeed";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("hotfeed_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              hotfeed_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_hotfeed_to_sas";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "sas1-2982-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:2ba1:10b:4fcc:0:56f4"; };
                                        { "sas1-7589-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:5484:10b:4fcc:0:56f4"; };
                                        { "sas1-9080-sas-pdb-feed-reader-pro-cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c08:4d95:10b:4fcc:0:56f4"; };
                                        { "sas2-5928-sas-pdb-feed-reader--cbb-22260.gencfg-c.yandex.net"; 22260; 200.000; "2a02:6b8:c14:45a7:10b:4fcc:0:56f4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "215ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_sas
                              hotfeed_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_hotfeed_to_man";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "man1-4352-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:3cd9:10b:4fcd:0:4cf4"; };
                                        { "man1-4737-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:206:10b:4fcd:0:4cf4"; };
                                        { "man1-6171-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:8f6:10b:4fcd:0:4cf4"; };
                                        { "man1-7569-man-pdb-feed-reader-pro-fa0-19700.gencfg-c.yandex.net"; 19700; 200.000; "2a02:6b8:c0b:199b:10b:4fcd:0:4cf4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "215ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_man
                              hotfeed_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_hotfeed_to_vla";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "vla1-0498-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:1a04:10b:4fc9:0:42f4"; };
                                        { "vla1-0627-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:98d:10b:4fc9:0:42f4"; };
                                        { "vla1-1884-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:4989:10b:4fc9:0:42f4"; };
                                        { "vla1-2425-vla-pdb-feed-reader-pro-191-17140.gencfg-c.yandex.net"; 17140; 200.000; "2a02:6b8:c0d:4aa2:10b:4fc9:0:42f4"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "20ms";
                                        backend_timeout = "215ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- hotfeed_vla
                              hotfeed_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_hotfeed_to_devnull";
                                  ranges = "1ms";
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
                              }; -- hotfeed_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_hotfeed_sas
                      int_api_profile_viewer = {
                        priority = 22;
                        match_fsm = {
                          URI = "/api/profile_viewer(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "profile_viewer";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("profileviewer_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              profileviewer_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_profile_viewer_to_sas";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "4h67fyhese2bz5d4.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:2a89:0:44f1:feec:0"; };
                                        { "4l45pzfm7j7qqusm.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:8014:0:44f1:5626:0"; };
                                        { "6y6r5kgtjz4p7wnj.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:2c8e:0:44f1:8081:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "95ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- profileviewer_sas
                              profileviewer_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_profile_viewer_to_man";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "i3r7fqsclfhmmg34.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:3763:0:44f1:be27:0"; };
                                        { "tcg33vzprf2agw3y.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:1b11:0:44f1:98e0:0"; };
                                        { "y5w3wrpbjdfygzlr.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:1b68:0:44f1:9428:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "95ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- profileviewer_man
                              profileviewer_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_profile_viewer_to_vla";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "csemh4ptpqeqt6g3.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4e05:0:44f1:5697:0"; };
                                        { "vaa3wje6hudv6576.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4a86:0:44f1:7a47:0"; };
                                        { "xvhbzzs4cfohahtx.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:5005:0:44f1:27ba:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "95ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- profileviewer_vla
                              profileviewer_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_profile_viewer_to_devnull";
                                  ranges = "1ms";
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
                              }; -- profileviewer_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 500;
                                force_conn_close = false;
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_api_profile_viewer
                      int_master_item_recommender = {
                        priority = 21;
                        match_fsm = {
                          URI = "/pdb/master_item_recommender(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "collections_master_item_recommender";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("collections_master_item_recommender_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              collections_master_item_recommender_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_collections_master_item_recommender_to_sas";
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
                                    active = {
                                      delay = "440ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "nb456ut4g3x6doxw.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:9005:10e:2e5:c7b:0"; };
                                        { "s6antuqdsskfufjv.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c14:3ea0:10e:2e5:5d80:0"; };
                                        { "y6xhde7kw3liz26c.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:8590:10e:2e5:b419:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "185ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- collections_master_item_recommender_sas
                              collections_master_item_recommender_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_collections_master_item_recommender_to_man";
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
                                    active = {
                                      delay = "440ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "b6oh4xuooucba4l4.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:2062:10e:2e5:3dfa:0"; };
                                        { "j3fs5b2ss2jcrevx.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a3c:10e:2e5:da63:0"; };
                                        { "srkghinj2ttb5ysu.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:20dc:10e:2e5:617:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "185ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- collections_master_item_recommender_man
                              collections_master_item_recommender_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_collections_master_item_recommender_to_vla";
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
                                    active = {
                                      delay = "440ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "bss7wclirnjvzlmf.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4e0a:10e:2e5:9427:0"; };
                                        { "gyezolck6wiwh3d7.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4884:10e:2e5:8c57:0"; };
                                        { "z3n6pwi37cq6ws6t.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4a10:10e:2e5:c7a6:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "185ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- collections_master_item_recommender_vla
                              collections_master_item_recommender_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_collections_master_item_recommender_to_devnull";
                                  ranges = "1ms";
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
                              }; -- collections_master_item_recommender_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_master_item_recommender
                      int_pdbcg = {
                        priority = 20;
                        match_fsm = {
                          URI = "/pdb(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "pdbcg";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          regexp = {
                            post_method = {
                              priority = 2;
                              match_fsm = {
                                match = "POST.*";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  pdbcg_sas = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_pdbcg_to_sas";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "500ms";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
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
                                            { "sas1-9532-sas-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c08:711a:100:427:0:7991"; };
                                            { "sas2-0232-sas-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c11:f1a:100:427:0:7991"; };
                                            { "sas2-0306-sas-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c11:a99:100:427:0:7991"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "4s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- pdbcg_sas
                                  pdbcg_man = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_pdbcg_to_man";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "500ms";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
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
                                            { "man1-4009-man-pdb-cg-production-8041.gencfg-c.yandex.net"; 8041; 200.000; "2a02:6b8:c0b:3997:100:11e8:0:1f69"; };
                                            { "man1-4800-man-pdb-cg-production-8041.gencfg-c.yandex.net"; 8041; 200.000; "2a02:6b8:c0b:2e4:100:11e8:0:1f69"; };
                                            { "man1-7465-man-pdb-cg-production-8041.gencfg-c.yandex.net"; 8041; 200.000; "2a02:6b8:c0b:19a4:100:11e8:0:1f69"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "4s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- pdbcg_man
                                  pdbcg_vla = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_pdbcg_to_vla";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        timeout_policy = {
                                          timeout = "500ms";
                                          unique_policy = {};
                                        }; -- timeout_policy
                                        attempts = 3;
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
                                            { "vla1-0104-vla-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c0d:16a3:10b:31da:0:7991"; };
                                            { "vla1-0193-vla-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c0d:440f:10b:31da:0:7991"; };
                                            { "vla1-3616-vla-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c0d:5017:10b:31da:0:7991"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "4s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 1;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- report
                                  }; -- pdbcg_vla
                                  pdbcg_devnull = {
                                    weight = -1.000;
                                    shared = {
                                      uuid = "6534975164501724037";
                                      report = {
                                        uuid = "requests_pdbcg_to_devnull";
                                        ranges = "1ms";
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
                                    }; -- shared
                                  }; -- pdbcg_devnull
                                }; -- rr
                                on_error = {
                                  errordocument = {
                                    status = 504;
                                    force_conn_close = false;
                                    content = "Gateway Timeout";
                                  }; -- errordocument
                                }; -- on_error
                              }; -- balancer2
                            }; -- post_method
                            default = {
                              priority = 1;
                              balancer2 = {
                                by_name_policy = {
                                  name = get_geo("pdbcg_", "random");
                                  unique_policy = {};
                                }; -- by_name_policy
                                attempts = 2;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  pdbcg_sas = {
                                    weight = 1.000;
                                    report = {
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
                                            { "sas1-9532-sas-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c08:711a:100:427:0:7991"; };
                                            { "sas2-0232-sas-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c11:f1a:100:427:0:7991"; };
                                            { "sas2-0306-sas-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c11:a99:100:427:0:7991"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "35ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_pdbcg_to_sas";
                                    }; -- report
                                  }; -- pdbcg_sas
                                  pdbcg_man = {
                                    weight = 1.000;
                                    report = {
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
                                            { "man1-4009-man-pdb-cg-production-8041.gencfg-c.yandex.net"; 8041; 200.000; "2a02:6b8:c0b:3997:100:11e8:0:1f69"; };
                                            { "man1-4800-man-pdb-cg-production-8041.gencfg-c.yandex.net"; 8041; 200.000; "2a02:6b8:c0b:2e4:100:11e8:0:1f69"; };
                                            { "man1-7465-man-pdb-cg-production-8041.gencfg-c.yandex.net"; 8041; 200.000; "2a02:6b8:c0b:19a4:100:11e8:0:1f69"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "35ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_pdbcg_to_man";
                                    }; -- report
                                  }; -- pdbcg_man
                                  pdbcg_vla = {
                                    weight = 1.000;
                                    report = {
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
                                            { "vla1-0104-vla-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c0d:16a3:10b:31da:0:7991"; };
                                            { "vla1-0193-vla-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c0d:440f:10b:31da:0:7991"; };
                                            { "vla1-3616-vla-pdb-cg-production-31121.gencfg-c.yandex.net"; 31121; 200.000; "2a02:6b8:c0d:5017:10b:31da:0:7991"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "35ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                      refers = "requests_pdbcg_to_vla";
                                    }; -- report
                                  }; -- pdbcg_vla
                                  pdbcg_devnull = {
                                    weight = -1.000;
                                    shared = {
                                      uuid = "6534975164501724037";
                                    }; -- shared
                                  }; -- pdbcg_devnull
                                }; -- rr
                                on_error = {
                                  errordocument = {
                                    status = 504;
                                    force_conn_close = false;
                                    content = "Gateway Timeout";
                                  }; -- errordocument
                                }; -- on_error
                              }; -- balancer2
                            }; -- default
                          }; -- regexp
                        }; -- report
                      }; -- int_pdbcg
                      int_api_feed_blender = {
                        priority = 19;
                        match_fsm = {
                          URI = "/api/feed/blender(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "api_feed_blender";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("feedblender_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              feedblender_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_feed_blender_to_sas";
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
                                    active = {
                                      delay = "1s";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "ejqwwbcuf2zh25sm.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:9b8c:10e:472:e7ce:0"; };
                                        { "imfi5dftkdw2jgdq.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:8d98:10e:472:c1dc:0"; };
                                        { "sr66frtpfczza5uw.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:8213:10e:472:22c4:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "20ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- feedblender_sas
                              feedblender_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_feed_blender_to_man";
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
                                    active = {
                                      delay = "1s";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "ceilrj3vrxtxocdz.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:2a1a:10e:472:89f5:0"; };
                                        { "es2jk2i2gwazm7hd.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c13:f8a:10e:472:79ba:0"; };
                                        { "zby5q24i75o3tckl.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:5a1a:10e:472:178a:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "20ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- feedblender_man
                              feedblender_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_feed_blender_to_vla";
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
                                    active = {
                                      delay = "1s";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "k4ptvfszga5mvmzt.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:1309:10e:472:84cc:0"; };
                                        { "n4k2cofludqdkzee.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:511f:10e:472:b51:0"; };
                                        { "n7az6dyvy3l5wfrn.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:2315:10e:472:381b:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "20ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- feedblender_vla
                              feedblender_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_api_feed_blender_to_devnull";
                                  ranges = "1ms";
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
                              }; -- feedblender_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_api_feed_blender
                      int_api_channel_recommender = {
                        priority = 18;
                        match_fsm = {
                          URI = "/api/channel_recommender(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "api_channel_recommender";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("channelrecommender_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              channelrecommender_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_channel_recommender_to_sas";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "sas1-2257-sas-pdb-channel-recomme-f98-27790.gencfg-c.yandex.net"; 27790; 228.000; "2a02:6b8:c08:2919:10c:5e3d:0:6c8e"; };
                                        { "sas1-5592-sas-pdb-channel-recomme-f98-27790.gencfg-c.yandex.net"; 27790; 228.000; "2a02:6b8:c08:2894:10c:5e3d:0:6c8e"; };
                                        { "sas1-7283-sas-pdb-channel-recomme-f98-27790.gencfg-c.yandex.net"; 27790; 228.000; "2a02:6b8:c08:430c:10c:5e3d:0:6c8e"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "75ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- channelrecommender_sas
                              channelrecommender_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_channel_recommender_to_man";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "man1-1117-man-pdb-channel-recomme-cc8-23380.gencfg-c.yandex.net"; 23380; 205.000; "2a02:6b8:c0b:1f8b:10c:5e3c:0:5b54"; };
                                        { "man1-3021-man-pdb-channel-recomme-cc8-23380.gencfg-c.yandex.net"; 23380; 205.000; "2a02:6b8:c0b:2ea0:10c:5e3c:0:5b54"; };
                                        { "man1-4433-man-pdb-channel-recomme-cc8-23380.gencfg-c.yandex.net"; 23380; 205.000; "2a02:6b8:c0b:36e1:10c:5e3c:0:5b54"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "75ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- channelrecommender_man
                              channelrecommender_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_channel_recommender_to_vla";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "vla1-0034-vla-pdb-channel-recomme-642-25430.gencfg-c.yandex.net"; 25430; 211.000; "2a02:6b8:c0d:9f:10c:5e3e:0:6356"; };
                                        { "vla1-2192-vla-pdb-channel-recomme-642-25430.gencfg-c.yandex.net"; 25430; 211.000; "2a02:6b8:c0d:228e:10c:5e3e:0:6356"; };
                                        { "vla1-2953-vla-pdb-channel-recomme-642-25430.gencfg-c.yandex.net"; 25430; 211.000; "2a02:6b8:c0d:89f:10c:5e3e:0:6356"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "75ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- channelrecommender_vla
                              channelrecommender_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_api_channel_recommender_to_devnull";
                                  ranges = "1ms";
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
                              }; -- channelrecommender_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_api_channel_recommender
                      int_api_recommender_boards = {
                        priority = 17;
                        match_fsm = {
                          URI = "/api/recommender/boards(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "recommender_boards";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("recommenderboards_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              recommenderboards_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_recommender_boards_to_sas";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "q4a2l25yvwnuwqzz.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:9906:0:44f3:4a0:0"; };
                                        { "rwfsk7v57vw4jgnn.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:8b14:0:44f3:1012:0"; };
                                        { "swlr5hm3z63lj6x3.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:6410:0:44f3:266a:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "100ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- recommenderboards_sas
                              recommenderboards_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_recommender_boards_to_man";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "jjbkxlprlgqz3zdf.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:38fe:0:44f3:344e:0"; };
                                        { "tncq6mgor5ox2ehm.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:15ea:0:44f3:1620:0"; };
                                        { "tovzqqd75ahj25oq.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:3411:0:44f3:240f:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "100ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- recommenderboards_man
                              recommenderboards_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_recommender_boards_to_vla";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "3angnahbnpuzx26u.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4e85:0:44f3:64b4:0"; };
                                        { "appbszmjhixdjcd2.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4e95:0:44f3:adaf:0"; };
                                        { "eu2addsbxj555mtz.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:b88:0:44f3:653d:0"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "100ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- recommenderboards_vla
                              recommenderboards_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_recommender_boards_to_devnull";
                                  ranges = "1ms";
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
                              }; -- recommenderboards_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 500;
                                force_conn_close = false;
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_api_recommender_boards
                      int_api_informers_channels = {
                        priority = 16;
                        match_fsm = {
                          URI = "/api/informers/channels(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "api_informers_channels";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("informers_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              informers_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_informers_channels_to_sas";
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
                                        { "sas1-2626-sas-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c08:1705:10b:18e7:0:21fc"; };
                                        { "sas1-2729-sas-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c08:1889:10b:18e7:0:21fc"; };
                                        { "sas1-5376-sas-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c08:8895:10b:18e7:0:21fc"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                  }; -- balancer2
                                }; -- report
                              }; -- informers_sas
                              informers_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_informers_channels_to_man";
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
                                        { "man1-1438-man-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0b:21a0:10b:18e6:0:21fc"; };
                                        { "man1-3978-man-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0b:2617:10b:18e6:0:21fc"; };
                                        { "man2-0315-man-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0b:4432:10b:18e6:0:21fc"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                  }; -- balancer2
                                }; -- report
                              }; -- informers_man
                              informers_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_informers_channels_to_vla";
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
                                        { "vla1-4309-vla-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0d:3907:10b:3001:0:21fc"; };
                                        { "vla1-5966-vla-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0f:160b:10b:3001:0:21fc"; };
                                        { "vla1-5994-vla-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0d:4da3:10b:3001:0:21fc"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                  }; -- balancer2
                                }; -- report
                              }; -- informers_vla
                              informers_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_api_informers_channels_to_devnull";
                                  ranges = "1ms";
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
                              }; -- informers_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_api_informers_channels
                      int_api_card_recommender = {
                        priority = 15;
                        match_fsm = {
                          URI = "/api/card_recommender(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "api_card_recommender";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            by_name_policy = {
                              name = get_geo("cardrecommender_", "random");
                              unique_policy = {};
                            }; -- by_name_policy
                            attempts = 1;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              cardrecommender_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_card_recommender_to_sas";
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
                                    active = {
                                      delay = "1s";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "sas2-0081-sas-collections-card-re-768-14935.gencfg-c.yandex.net"; 14935; 160.000; "2a02:6b8:c11:d1c:10d:2a19:0:3a57"; };
                                        { "sas2-0528-sas-collections-card-re-768-14935.gencfg-c.yandex.net"; 14935; 160.000; "2a02:6b8:c11:e9a:10d:2a19:0:3a57"; };
                                        { "sas2-5529-a79-sas-collections-car-768-14935.gencfg-c.yandex.net"; 14935; 160.000; "2a02:6b8:c16:413:10d:2a19:0:3a57"; };
                                        { "sas2-5543-261-sas-collections-car-768-14935.gencfg-c.yandex.net"; 14935; 160.000; "2a02:6b8:c16:405:10d:2a19:0:3a57"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "100ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- cardrecommender_sas
                              cardrecommender_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_card_recommender_to_man";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "man1-5440-man-collections-card-re-d79-14925.gencfg-c.yandex.net"; 14925; 160.000; "2a02:6b8:c09:c82:10d:2a18:0:3a4d"; };
                                        { "man2-1227-man-collections-card-d79-14925.gencfg-c.yandex.net"; 14925; 160.000; "2a02:6b8:c13:121a:10d:2a18:0:3a4d"; };
                                        { "man2-1250-man-collections-card-d79-14925.gencfg-c.yandex.net"; 14925; 160.000; "2a02:6b8:c13:120b:10d:2a18:0:3a4d"; };
                                        { "man2-1309-man-collections-card-d79-14925.gencfg-c.yandex.net"; 14925; 160.000; "2a02:6b8:c13:120a:10d:2a18:0:3a4d"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "100ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- cardrecommender_man
                              cardrecommender_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_card_recommender_to_vla";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "vla1-2488-vla-collections-card-re-6a3-14945.gencfg-c.yandex.net"; 14945; 160.000; "2a02:6b8:c0d:3ca0:10d:2a1a:0:3a61"; };
                                        { "vla1-2750-vla-collections-card-re-6a3-14945.gencfg-c.yandex.net"; 14945; 160.000; "2a02:6b8:c0d:1903:10d:2a1a:0:3a61"; };
                                        { "vla1-3681-vla-collections-card-re-6a3-14945.gencfg-c.yandex.net"; 14945; 160.000; "2a02:6b8:c0d:4381:10d:2a1a:0:3a61"; };
                                        { "vla1-3917-vla-collections-card-re-6a3-14945.gencfg-c.yandex.net"; 14945; 160.000; "2a02:6b8:c0d:4010:10d:2a1a:0:3a61"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "100ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- cardrecommender_vla
                              cardrecommender_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_api_card_recommender_to_devnull";
                                  ranges = "1ms";
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
                              }; -- cardrecommender_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_api_card_recommender
                      int_api_image_recommender = {
                        priority = 14;
                        match_fsm = {
                          URI = "/api/image_recommender(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "api_image_recommender";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          rewrite = {
                            actions = {
                              {
                                global = false;
                                literal = false;
                                case_insensitive = false;
                                rewrite = "/yandsearch%1";
                                regexp = "/api/image_recommender(/.*)?";
                              };
                            }; -- actions
                            balancer2 = {
                              by_name_policy = {
                                name = get_geo("imagerecommender_", "random");
                                unique_policy = {};
                              }; -- by_name_policy
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                imagerecommender_sas = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_api_image_recommender_to_sas";
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
                                      active = {
                                        delay = "1s";
                                        request = "GET /tass HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "sas1-4977.search.yandex.net"; 16769; 120.000; "2a02:6b8:b000:650:96de:80ff:fe81:a46"; };
                                          { "sas1-4996.search.yandex.net"; 16769; 120.000; "2a02:6b8:b000:66b:96de:80ff:fe81:1114"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "15ms";
                                          backend_timeout = "100ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- imagerecommender_sas
                                imagerecommender_man = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_api_image_recommender_to_man";
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
                                      active = {
                                        delay = "1s";
                                        request = "GET /tass HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "man1-7647.search.yandex.net"; 21931; 120.000; "2a02:6b8:b000:606b:e61d:2dff:fe00:9b10"; };
                                          { "man2-1448.search.yandex.net"; 21931; 120.000; "2a02:6b8:c01:73:0:604:14dd:f030"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "15ms";
                                          backend_timeout = "100ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- imagerecommender_man
                                imagerecommender_vla = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_api_image_recommender_to_vla";
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
                                      active = {
                                        delay = "1s";
                                        request = "GET /tass HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                        steady = false;
                                        unpack(gen_proxy_backends({
                                          { "vla1-4353.search.yandex.net"; 28456; 120.000; "2a02:6b8:c0e:90:0:604:db7:ab59"; };
                                          { "vla1-4508.search.yandex.net"; 28456; 120.000; "2a02:6b8:c0e:8a:0:604:db7:a808"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "15ms";
                                          backend_timeout = "100ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 1;
                                          need_resolve = true;
                                        }))
                                      }; -- active
                                    }; -- balancer2
                                  }; -- report
                                }; -- imagerecommender_vla
                                imagerecommender_devnull = {
                                  weight = -1.000;
                                  report = {
                                    uuid = "requests_api_image_recommender_to_devnull";
                                    ranges = "1ms";
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
                                }; -- imagerecommender_devnull
                              }; -- rr
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                  content = "Gateway Timeout";
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- rewrite
                        }; -- report
                      }; -- int_api_image_recommender
                      int_pdb_top_reader = {
                        priority = 13;
                        match_fsm = {
                          URI = "(/collections)?/api/top(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "api_top";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            timeout_policy = {
                              timeout = "300ms";
                              unique_policy = {};
                            }; -- timeout_policy
                            attempts = 2;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              topreader_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_top_to_sas";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "sas2-0135.search.yandex.net"; 13665; 40.000; "2a02:6b8:c02:419:0:604:df5:d7d8"; };
                                        { "sas2-0353.search.yandex.net"; 13665; 40.000; "2a02:6b8:c02:401:0:604:dde:f803"; };
                                        { "sas2-0369.search.yandex.net"; 13665; 40.000; "2a02:6b8:c02:403:0:604:dde:f97b"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- topreader_sas
                              topreader_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_top_to_man";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "man1-7022.search.yandex.net"; 12735; 40.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:5050"; };
                                        { "man1-7170.search.yandex.net"; 12735; 40.000; "2a02:6b8:b000:6506:215:b2ff:fea9:66f2"; };
                                        { "man1-8876.search.yandex.net"; 12735; 40.000; "2a02:6b8:b000:6064:e61d:2dff:fe00:9190"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- topreader_man
                              topreader_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_top_to_vla";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "vla1-0135.search.yandex.net"; 20505; 40.000; "2a02:6b8:c0e:3f:0:604:db7:9f2d"; };
                                        { "vla1-1440.search.yandex.net"; 20505; 40.000; "2a02:6b8:c0e:71:0:604:db7:a2ac"; };
                                        { "vla1-2382.search.yandex.net"; 20505; 40.000; "2a02:6b8:c0e:97:0:604:db7:a7a8"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- topreader_vla
                              topreader_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_api_top_to_devnull";
                                  ranges = "1ms";
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
                              }; -- topreader_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_pdb_top_reader
                      common_upstream_api_http_adapter = {
                        priority = 12;
                        match_fsm = {
                          URI = "(/collections)?/api/v0\\..*";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "5352998188831779523";
                          hasher = {
                            mode = "subnet";
                            take_ip_from = "X-Real-IP";
                            h100 = {
                              cutter = {
                                bytes = 512;
                                timeout = "0.1s";
                                antirobot = {
                                  cut_request = true;
                                  no_cut_request_file = "./controls/no_cut_request_file";
                                  file_switch = "./controls/disable_antirobot_module";
                                  cut_request_bytes = 512;
                                  checker = {
                                    report = {
                                      uuid = "antirobot";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        by_name_policy = {
                                          name = get_geo("antirobot_", "random");
                                          simple_policy = {};
                                        }; -- by_name_policy
                                        attempts = 1;
                                        rr = {
                                          antirobot_man = {
                                            weight = 1.000;
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              hashing = {
                                                unpack(gen_proxy_backends({
                                                  { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                                  { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                                  { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                                  { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                                  { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                                  { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                                  { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                                  { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                                  { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                                  { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
                                                  { "man1-3249.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6000:e61d:2dff:fe6d:bb30"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "30ms";
                                                  backend_timeout = "100ms";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = true;
                                                }))
                                              }; -- hashing
                                            }; -- balancer2
                                          }; -- antirobot_man
                                          antirobot_sas = {
                                            weight = 1.000;
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              hashing = {
                                                unpack(gen_proxy_backends({
                                                  { "sas1-0670.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:154:225:90ff:fe83:1800"; };
                                                  { "sas1-0980.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:153:225:90ff:fe83:aa2"; };
                                                  { "sas1-2218.search.yandex.net"; 13512; 343.000; "2a02:6b8:b000:66a:225:90ff:fe94:1792"; };
                                                  { "sas2-4686.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:665:225:90ff:fe92:894a"; };
                                                  { "sas2-8870.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:5cc:0:604:9092:8666"; };
                                                  { "sas2-8992.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:792:0:604:9094:2fb4"; };
                                                  { "sas2-8993.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:781:0:604:9094:1530"; };
                                                  { "sas2-9021.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b11:0:604:90c2:a40a"; };
                                                  { "sas2-9033.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:783:0:604:9094:138e"; };
                                                  { "sas2-9036.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:781:0:604:9094:1748"; };
                                                  { "sas2-9189.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bb94"; };
                                                  { "sas2-9190.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:3436:7284"; };
                                                  { "sas2-9191.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2ce6"; };
                                                  { "sas2-9192.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:3fe4"; };
                                                  { "sas2-9193.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2436"; };
                                                  { "sas2-9194.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:35f8"; };
                                                  { "sas2-9195.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bdea"; };
                                                  { "sas2-9196.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:badc"; };
                                                  { "sas2-9197.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bf90"; };
                                                  { "sas2-9198.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:e87e"; };
                                                  { "sas2-9199.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:e962"; };
                                                  { "sas2-9200.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:ea60"; };
                                                  { "sas2-9201.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:354b:5cef"; };
                                                  { "sas2-9202.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bef0"; };
                                                  { "sas2-9203.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dd16"; };
                                                  { "sas2-9204.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:b96e"; };
                                                  { "sas2-9205.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:354b:5b07"; };
                                                  { "sas2-9206.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:bc22"; };
                                                  { "sas2-9207.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2730"; };
                                                  { "sas2-9208.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:ea16"; };
                                                  { "sas2-9209.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2238"; };
                                                  { "sas2-9210.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:d9e4"; };
                                                  { "sas2-9211.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dd5c"; };
                                                  { "sas2-9212.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2f8c"; };
                                                  { "sas2-9213.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bf66"; };
                                                  { "sas2-9214.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34c8:caf6"; };
                                                  { "sas2-9215.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808e:7a1f"; };
                                                  { "sas2-9216.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:354b:5b3f"; };
                                                  { "sas2-9217.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:b90a"; };
                                                  { "sas2-9218.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2df2"; };
                                                  { "sas2-9219.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808e:7cce"; };
                                                  { "sas2-9220.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2dca"; };
                                                  { "sas2-9221.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:808e:7c06"; };
                                                  { "sas2-9222.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:c018"; };
                                                  { "sas2-9223.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:3c3e"; };
                                                  { "sas2-9224.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:d98c"; };
                                                  { "sas2-9225.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2b10"; };
                                                  { "sas2-9226.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:c006"; };
                                                  { "sas2-9227.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34bb:baf8"; };
                                                  { "sas2-9228.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:325e"; };
                                                  { "sas2-9229.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:b93e"; };
                                                  { "sas2-9230.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2454"; };
                                                  { "sas2-9231.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bb86"; };
                                                  { "sas2-9232.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2cfc"; };
                                                  { "sas2-9233.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34bb:bb4e"; };
                                                  { "sas2-9234.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:20ca"; };
                                                  { "sas2-9235.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:e562"; };
                                                  { "sas2-9236.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:31e2"; };
                                                  { "sas2-9237.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bf4c"; };
                                                  { "sas2-9238.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:d902"; };
                                                  { "sas2-9239.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dc34"; };
                                                  { "sas2-9240.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:d7a8"; };
                                                  { "sas2-9241.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:c0ce"; };
                                                  { "sas2-9242.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:ea3a"; };
                                                  { "sas2-9243.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:c016"; };
                                                  { "sas2-9244.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:bf48"; };
                                                  { "sas2-9245.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:8089:7456"; };
                                                  { "sas2-9246.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:e4ea"; };
                                                  { "sas2-9247.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:e964"; };
                                                  { "sas2-9411.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b34:0:604:9083:1856"; };
                                                  { "sas2-9412.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b34:0:604:9083:17e4"; };
                                                  { "sas2-9528.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:726:0:604:90c1:d270"; };
                                                  { "sas2-9530.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:530:0:604:34cf:2336"; };
                                                  { "sas2-9532.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:7d8:0:604:90c6:2eac"; };
                                                  { "sas2-9533.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:7d8:0:604:90c6:2c94"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "30ms";
                                                  backend_timeout = "100ms";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = true;
                                                }))
                                              }; -- hashing
                                            }; -- balancer2
                                          }; -- antirobot_sas
                                          antirobot_vla = {
                                            weight = 1.000;
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              hashing = {
                                                unpack(gen_proxy_backends({
                                                  { "vla1-1343.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:44:0:604:db7:a0b2"; };
                                                  { "vla1-1797.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:45:0:604:db7:a64b"; };
                                                  { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                  { "vla1-3568.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:a1:0:604:db7:a2db"; };
                                                  { "vla1-3679.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:87:0:604:db7:ab81"; };
                                                  { "vla1-3709.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:72:0:604:db7:a71b"; };
                                                  { "vla1-3710.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a836"; };
                                                  { "vla1-3716.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:72:0:604:db7:a5c6"; };
                                                  { "vla1-3863.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:aa08"; };
                                                  { "vla1-3965.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:87:0:604:db7:aba1"; };
                                                  { "vla1-4006.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a92c"; };
                                                  { "vla1-4025.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:7f:0:604:db7:a3a5"; };
                                                  { "vla1-4041.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8c:0:604:db7:abf2"; };
                                                  { "vla1-4114.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:aab9"; };
                                                  { "vla1-4117.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8a:0:604:db7:a817"; };
                                                  { "vla1-4119.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8a:0:604:db7:a978"; };
                                                  { "vla1-4130.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:a8d6"; };
                                                  { "vla1-4153.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9b:0:604:db7:aa91"; };
                                                  { "vla1-4167.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9c:0:604:db7:a8e5"; };
                                                  { "vla1-4168.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:97:0:604:db7:a7a3"; };
                                                  { "vla1-4177.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9b:0:604:db7:aa6b"; };
                                                  { "vla1-4183.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:7c:0:604:db7:9df2"; };
                                                  { "vla1-4192.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8c:0:604:db7:ab53"; };
                                                  { "vla1-4200.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:a82b"; };
                                                  { "vla1-4321.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a842"; };
                                                  { "vla1-4344.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:ab5b"; };
                                                  { "vla1-4354.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:aa94"; };
                                                  { "vla1-4406.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:95:0:604:db7:a9f9"; };
                                                  { "vla1-4472.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9c:0:604:db7:aa71"; };
                                                  { "vla1-4553.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:77:0:604:d8f:eb26"; };
                                                  { "vla1-4554.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:77:0:604:d8f:eb76"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "30ms";
                                                  backend_timeout = "100ms";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = true;
                                                }))
                                              }; -- hashing
                                            }; -- balancer2
                                          }; -- antirobot_vla
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- report
                                  }; -- checker
                                  module = {
                                    icookie = {
                                      use_default_keys = true;
                                      domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua,.yandex.uz";
                                      trust_parent = false;
                                      trust_children = false;
                                      enable_set_cookie = true;
                                      enable_decrypting = true;
                                      decrypted_uid_header = "X-Yandex-ICookie";
                                      error_header = "X-Yandex-ICookie-Error";
                                      report = {
                                        uuid = "api_http_adapter";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        headers = {
                                          create_func_weak = {
                                            ["X-Start-Time"] = "starttime";
                                          }; -- create_func_weak
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            rr = {
                                              weights_file = "./controls/traffic_control.weights";
                                              api_http_adapter_sas = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "requests_api_http_adapter_to_sas";
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
                                                        { "sas1-1099.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:666:96de:80ff:feec:1c28"; };
                                                        { "sas1-1352.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:618:215:b2ff:fea8:d3a"; };
                                                        { "sas1-1370.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d8c"; };
                                                        { "sas1-1376.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:db6"; };
                                                        { "sas1-1383.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:a3a"; };
                                                        { "sas1-1422.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:6d3c"; };
                                                        { "sas1-1424.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:121:215:b2ff:fea8:cea"; };
                                                        { "sas1-1426.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:196:215:b2ff:fea8:dba"; };
                                                        { "sas1-1433.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:112:215:b2ff:fea8:6dc8"; };
                                                        { "sas1-1440.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:d2a"; };
                                                        { "sas1-1774.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:625:96de:80ff:feec:198a"; };
                                                        { "sas1-1958.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:1c24"; };
                                                        { "sas1-2200.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:654:96de:80ff:feec:186a"; };
                                                        { "sas1-4461.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:651:96de:80ff:feec:1830"; };
                                                        { "sas1-5360.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1b82"; };
                                                        { "sas1-5451.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a9:96de:80ff:feec:f98"; };
                                                        { "sas1-5455.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:1ec4"; };
                                                        { "sas1-5456.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1924"; };
                                                        { "sas1-5462.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:63d:96de:80ff:feec:1316"; };
                                                        { "sas1-5463.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1002"; };
                                                        { "sas1-5464.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:171c"; };
                                                        { "sas1-5466.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:625:96de:80ff:feec:120e"; };
                                                        { "sas1-5473.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:61c:96de:80ff:feec:11f4"; };
                                                        { "sas1-5475.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:139:96de:80ff:feec:fcc"; };
                                                        { "sas1-5476.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:651:96de:80ff:feec:10cc"; };
                                                        { "sas1-5477.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:18a2"; };
                                                        { "sas1-5478.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:61c:96de:80ff:feec:1898"; };
                                                        { "sas1-5485.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:63a:96de:80ff:feec:1a5e"; };
                                                        { "sas1-5486.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:653:96de:80ff:feec:1876"; };
                                                        { "sas1-5487.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:653:96de:80ff:feec:1124"; };
                                                        { "sas1-5488.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:61c:96de:80ff:fee9:1aa"; };
                                                        { "sas1-5493.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:653:96de:80ff:fee9:fd"; };
                                                        { "sas1-5495.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:63c:96de:80ff:feec:157c"; };
                                                        { "sas1-5498.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:139:96de:80ff:fee9:1de"; };
                                                        { "sas1-5499.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:139:96de:80ff:fee9:184"; };
                                                        { "sas1-5501.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:1624"; };
                                                        { "sas1-5502.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10fe"; };
                                                        { "sas1-5506.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:1a38"; };
                                                        { "sas1-5507.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:656:96de:80ff:feec:1a32"; };
                                                        { "sas1-5508.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:674:96de:80ff:feec:1b9c"; };
                                                        { "sas1-5513.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:111:96de:80ff:feec:1ba8"; };
                                                        { "sas1-5516.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:18fc"; };
                                                        { "sas1-5518.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1cc0"; };
                                                        { "sas1-5629.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:163:225:90ff:fee6:4d12"; };
                                                        { "sas1-5649.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:602:225:90ff:fee6:4c7a"; };
                                                        { "sas1-5657.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:649:225:90ff:fee6:dd88"; };
                                                        { "sas1-5662.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:649:225:90ff:fee5:bde0"; };
                                                        { "sas1-5960.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:636:225:90ff:feef:c964"; };
                                                        { "sas1-5965.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:604:225:90ff:feeb:fba6"; };
                                                        { "sas1-5966.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:635:225:90ff:feeb:fd9e"; };
                                                        { "sas1-5967.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601:225:90ff:feeb:fcd6"; };
                                                        { "sas1-5968.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:604:225:90ff:feeb:fba2"; };
                                                        { "sas1-5969.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601:225:90ff:feeb:f9b8"; };
                                                        { "sas1-5970.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:100:225:90ff:feeb:fce2"; };
                                                        { "sas1-5971.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:604:225:90ff:feed:2b8e"; };
                                                        { "sas1-5972.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:174:225:90ff:feed:2b4e"; };
                                                        { "sas1-5973.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                                                        { "sas1-5974.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:100:225:90ff:feed:2fde"; };
                                                        { "sas1-6351.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                                                        { "sas1-6752.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                                                        { "sas1-6893.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                                                        { "sas1-6939.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                                                        { "sas1-6978.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                                                        { "sas1-7095.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                                                        { "sas1-7098.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                                                        { "sas1-7125.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                                                        { "sas1-7155.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                                                        { "sas1-7156.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                                                        { "sas1-7238.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                                                        { "sas1-7272.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                                                        { "sas1-7286.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                                                        { "sas1-7287.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                                                        { "sas1-7326.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                                                        { "sas1-7330.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                                                        { "sas1-7331.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                                                        { "sas1-7459.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                                                        { "sas1-7494.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                                                        { "sas1-7498.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                                                        { "sas1-7825.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                                                        { "sas1-7843.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                                                        { "sas1-7929.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                                                        { "sas1-8873.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                                                        { "sas1-8979.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                                                        { "sas2-7093.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:b418"; };
                                                        { "sas2-7097.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:b61c"; };
                                                        { "sas2-7104.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:cb0c"; };
                                                        { "sas2-7113.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5bb:0:604:3564:4ddd"; };
                                                        { "sas2-7120.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:555:0:604:90e5:4420"; };
                                                        { "sas2-7130.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:52d:0:604:90e4:cfaa"; };
                                                        { "sas2-7139.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5bb:0:604:35c6:3038"; };
                                                        { "sas2-7142.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:539:0:604:354b:5dcd"; };
                                                        { "sas2-8199.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:7a18:10d0"; };
                                                        { "sas2-8200.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77f:0:604:7a1d:5f88"; };
                                                        { "sas2-8201.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:7a51:501c"; };
                                                        { "sas2-8202.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5d5:0:604:7a18:f16"; };
                                                        { "sas2-8203.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:554c"; };
                                                        { "sas2-8204.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77d:0:604:7a51:515a"; };
                                                        { "sas2-8205.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77e:0:604:7a51:5012"; };
                                                        { "sas2-8206.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:785:0:604:7a18:e44"; };
                                                        { "sas2-8207.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:5596"; };
                                                        { "sas2-8208.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:777:0:604:7a52:cb38"; };
                                                        { "sas2-8209.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a1d:5c46"; };
                                                        { "sas2-8210.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5d5:0:604:7a1d:5edc"; };
                                                        { "sas2-8211.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:55d8"; };
                                                        { "sas2-8212.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:90eb:915e"; };
                                                        { "sas2-8213.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77d:0:604:7a51:52a6"; };
                                                        { "sas2-8214.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a51:5594"; };
                                                        { "sas2-8216.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a51:539e"; };
                                                        { "sas2-8624.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:64c:0:604:5ecc:f4ca"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "15s";
                                                        fail_on_5xx = false;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 1;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- weighted2
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- api_http_adapter_sas
                                              api_http_adapter_man = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "requests_api_http_adapter_to_man";
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
                                                        { "man1-1076.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                                                        { "man1-1150.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                                                        { "man1-1515.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b150"; };
                                                        { "man1-1885.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                                                        { "man1-1957.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                                                        { "man1-1979.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                                                        { "man1-2023.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                                                        { "man1-2087.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                                                        { "man1-2092.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                                                        { "man1-2106.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                                                        { "man1-2112.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:560"; };
                                                        { "man1-2383.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6011:f652:14ff:fe55:1ca0"; };
                                                        { "man1-2873.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                                                        { "man1-2943.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                                                        { "man1-3175.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:1ff0"; };
                                                        { "man1-3252.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                                                        { "man1-3260.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                                                        { "man1-3261.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                                                        { "man1-3265.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                                                        { "man1-3375.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c580"; };
                                                        { "man1-3479.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7bd0"; };
                                                        { "man1-3484.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:602f:92e2:baff:fe72:7bfa"; };
                                                        { "man1-3489.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e86"; };
                                                        { "man1-3493.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:602b:92e2:baff:fe6f:815a"; };
                                                        { "man1-3498.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:b6a4"; };
                                                        { "man1-3499.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:7eae"; };
                                                        { "man1-3500.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:80e8"; };
                                                        { "man1-3510.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f7b4"; };
                                                        { "man1-3512.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6f6"; };
                                                        { "man1-3517.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6014:f652:14ff:fe44:5250"; };
                                                        { "man1-3520.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:b7d0"; };
                                                        { "man1-3523.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d6b0"; };
                                                        { "man1-3524.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d710"; };
                                                        { "man1-3526.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:d5c0"; };
                                                        { "man1-3527.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:d6a0"; };
                                                        { "man1-3528.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aeb0"; };
                                                        { "man1-3529.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b010"; };
                                                        { "man1-3533.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cdb0"; };
                                                        { "man1-3534.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b590"; };
                                                        { "man1-3535.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cda0"; };
                                                        { "man1-3537.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:2220"; };
                                                        { "man1-3542.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd50"; };
                                                        { "man1-3544.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2610"; };
                                                        { "man1-3752.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                                                        { "man1-3822.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                                                        { "man1-3904.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                                                        { "man1-3959.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                                                        { "man1-4025.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                                                        { "man1-4073.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6040:92e2:baff:fe74:770a"; };
                                                        { "man1-4074.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7dc0"; };
                                                        { "man1-4076.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7d6c"; };
                                                        { "man1-4077.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bdda"; };
                                                        { "man1-4078.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b656"; };
                                                        { "man1-4080.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6f:7d86"; };
                                                        { "man1-4081.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b842"; };
                                                        { "man1-4082.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:24d4"; };
                                                        { "man1-4083.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2610"; };
                                                        { "man1-4084.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2570"; };
                                                        { "man1-4085.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2434"; };
                                                        { "man1-4310.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7bcc"; };
                                                        { "man1-4311.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                                                        { "man1-4638.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:913a"; };
                                                        { "man1-5640.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8a30"; };
                                                        { "man1-6102.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                                                        { "man1-6134.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3d50"; };
                                                        { "man1-6150.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:da20"; };
                                                        { "man1-6161.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3890"; };
                                                        { "man1-6167.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:82d0"; };
                                                        { "man1-6227.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                                                        { "man1-6242.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                                                        { "man1-6263.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                                                        { "man1-6359.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6053:f652:14ff:fe55:29c0"; };
                                                        { "man1-6393.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:48f0"; };
                                                        { "man1-6413.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:1ad0"; };
                                                        { "man1-6419.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:4720"; };
                                                        { "man1-6485.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:2430"; };
                                                        { "man1-6634.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:604f:e61d:2dff:fe01:f370"; };
                                                        { "man1-6727.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cd90"; };
                                                        { "man1-6728.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cdd0"; };
                                                        { "man1-6763.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                                                        { "man1-6767.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:4bc0"; };
                                                        { "man1-6854.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6060:f652:14ff:fef5:c950"; };
                                                        { "man1-6873.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:605e:e61d:2dff:fe04:1ed0"; };
                                                        { "man1-6886.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3d30"; };
                                                        { "man1-6900.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3da0"; };
                                                        { "man1-6903.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3cf0"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "15s";
                                                        fail_on_5xx = false;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 1;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- weighted2
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- api_http_adapter_man
                                              api_http_adapter_vla = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "requests_api_http_adapter_to_vla";
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
                                                        { "vla1-0147.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                                                        { "vla1-0152.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                                                        { "vla1-0221.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                                                        { "vla1-0703.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                                                        { "vla1-0951.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                                                        { "vla1-1128.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                                                        { "vla1-1161.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                                                        { "vla1-1207.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                                                        { "vla1-1239.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                                                        { "vla1-1257.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                                                        { "vla1-1278.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                                                        { "vla1-1294.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                                                        { "vla1-1304.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                                                        { "vla1-1315.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                                                        { "vla1-1330.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                                                        { "vla1-1358.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                                                        { "vla1-1384.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                                                        { "vla1-1395.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                                                        { "vla1-1464.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                                                        { "vla1-1516.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                                                        { "vla1-1518.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                                                        { "vla1-1598.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                                                        { "vla1-1710.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                                                        { "vla1-1757.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                                                        { "vla1-1815.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                                                        { "vla1-1911.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                                                        { "vla1-1930.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                                                        { "vla1-1987.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                                                        { "vla1-2016.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                                                        { "vla1-2017.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                                                        { "vla1-2078.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                                                        { "vla1-2092.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                                                        { "vla1-2121.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                                                        { "vla1-2122.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                                                        { "vla1-2143.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                                                        { "vla1-2163.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                                                        { "vla1-2315.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                                                        { "vla1-2319.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                                                        { "vla1-2330.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                                                        { "vla1-2332.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                                                        { "vla1-2345.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                                                        { "vla1-2351.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                                                        { "vla1-2358.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                                                        { "vla1-2453.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                                                        { "vla1-2472.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                                                        { "vla1-3780.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                                                        { "vla1-4111.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                                                        { "vla1-4272.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a45a"; };
                                                        { "vla1-4375.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                                                        { "vla1-4428.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                                                        { "vla1-4443.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                                                        { "vla1-4504.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "15s";
                                                        fail_on_5xx = false;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 1;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- weighted2
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- api_http_adapter_vla
                                              api_http_adapter_devnull = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "requests_api_http_adapter_to_devnull";
                                                  ranges = "1ms";
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
                                              }; -- api_http_adapter_devnull
                                            }; -- rr
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Gateway Timeout";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- headers
                                      }; -- report
                                    }; -- icookie
                                  }; -- module
                                }; -- antirobot
                              }; -- cutter
                            }; -- h100
                          }; -- hasher
                        }; -- shared
                      }; -- common_upstream_api_http_adapter
                      common_api_cards = {
                        priority = 11;
                        match_fsm = {
                          URI = "(/collections)?/api/cards(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "831293184824121850";
                          report = {
                            uuid = "api_cards";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            icookie = {
                              use_default_keys = true;
                              domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua,.yandex.uz";
                              trust_parent = false;
                              trust_children = false;
                              enable_set_cookie = true;
                              enable_decrypting = true;
                              decrypted_uid_header = "X-Yandex-ICookie";
                              error_header = "X-Yandex-ICookie-Error";
                              regexp = {
                                post_method = {
                                  priority = 3;
                                  match_fsm = {
                                    match = "POST.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      api_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_cards_to_sas";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "386324191296716630";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 1;
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "sas1-1967.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:13a:feaa:14ff:fede:3fd0"; };
                                                  { "sas1-2927.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:631:225:90ff:fe88:b67a"; };
                                                  { "sas1-3016.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10d:225:90ff:fe88:5022"; };
                                                  { "sas1-3248.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:106:225:90ff:fe88:b41e"; };
                                                  { "sas1-3319.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10a:225:90ff:fe83:1f0e"; };
                                                  { "sas1-5658.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                  { "sas2-6330.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:6f9:96de:80ff:fe8c:dc90"; };
                                                  { "sas2-7140.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:566:0:604:3564:541f"; };
                                                  { "sas2-8842.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd79"; };
                                                  { "sas2-8843.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd5c"; };
                                                  { "sas2-8847.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd63"; };
                                                  { "sas2-8850.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:64c:0:604:5ecc:f0b8"; };
                                                  { "sas2-9413.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:5c8:0:604:7a1d:5f56"; };
                                                  { "sas2-9430.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a354"; };
                                                  { "sas2-9431.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a054"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "15s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- report
                                      }; -- api_sas
                                      api_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_cards_to_man";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "7332608112167330077";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 1;
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "man1-9350.search.yandex.net"; 11840; 640.000; "2a02:6b8:b000:6085:92e2:baff:fea2:33c2"; };
                                                  { "man2-1272.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:de49"; };
                                                  { "man2-1277.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de46"; };
                                                  { "man2-1278.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dacc"; };
                                                  { "man2-1279.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dad3"; };
                                                  { "man2-1293.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:db12"; };
                                                  { "man2-1309.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0b6"; };
                                                  { "man2-1319.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0c8"; };
                                                  { "man2-1323.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de51"; };
                                                  { "man2-1657.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:7c5e"; };
                                                  { "man2-1662.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:6fee"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "15s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- report
                                      }; -- api_man
                                      api_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_cards_to_vla";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "4971768765950878358";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 1;
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "vla1-0090.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:2d:0:604:db7:9d37"; };
                                                  { "vla1-0237.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:81:0:604:db7:a7df"; };
                                                  { "vla1-0548.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:17:0:604:db7:991f"; };
                                                  { "vla1-2088.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:1b:0:604:db7:9985"; };
                                                  { "vla1-2337.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:7f:0:604:db7:9e61"; };
                                                  { "vla1-2488.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:79:0:604:db7:a96c"; };
                                                  { "vla1-2490.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:94:0:604:db7:a94e"; };
                                                  { "vla1-4409.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:90:0:604:db7:a19d"; };
                                                  { "vla2-1025.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                  { "vla2-1027.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:9ad5:e86e"; };
                                                  { "vla2-1047.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77ea"; };
                                                  { "vla2-7970.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:373:0:604:5e97:e21e"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "15s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- report
                                      }; -- api_vla
                                      api_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "5159940746290666347";
                                          report = {
                                            uuid = "requests_api_cards_to_devnull";
                                            ranges = "1ms";
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
                                        }; -- shared
                                      }; -- api_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- post_method
                                get_method = {
                                  priority = 2;
                                  match_fsm = {
                                    match = "GET.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  request_replier = {
                                    sink = {
                                      report = {
                                        uuid = "requests_api_cards_to_prestable";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        shared = {
                                          uuid = "7117126246408427627";
                                          balancer2 = {
                                            unique_policy = {};
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
                                                { "man1-0944.search.yandex.net"; 19040; 40.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f6ae"; };
                                                { "man1-3412.search.yandex.net"; 19040; 40.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:df90"; };
                                                { "man1-8397.search.yandex.net"; 19040; 40.000; "2a02:6b8:b000:6082:e61d:2dff:fe6c:fa10"; };
                                                { "sas1-9531.search.yandex.net"; 19040; 40.000; "2a02:6b8:b000:19a:428d:5cff:fef4:8ce9"; };
                                                { "sas2-0139.search.yandex.net"; 19040; 40.000; "2a02:6b8:c02:419:0:604:df5:d7d5"; };
                                                { "sas2-0209.search.yandex.net"; 19040; 40.000; "2a02:6b8:c02:41e:0:604:df5:d61e"; };
                                                { "vla1-0395.search.yandex.net"; 19040; 40.000; "2a02:6b8:c0e:9d:0:604:d8f:eb84"; };
                                                { "vla1-3160.search.yandex.net"; 19040; 40.000; "2a02:6b8:c0e:30:0:604:db7:9d83"; };
                                                { "vla1-4566.search.yandex.net"; 19040; 40.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- report
                                    }; -- sink
                                    enable_failed_requests_replication = false;
                                    rate = 0.000;
                                    rate_file = "./controls/request_repl.ratefile";
                                    shared = {
                                      uuid = "collections_api_cards_backends";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
                                        attempts_file = "./controls/api_cards.attempts";
                                        rr = {
                                          weights_file = "./controls/traffic_control.weights";
                                          api_sas = {
                                            weight = 1.000;
                                            report = {
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              refers = "requests_api_cards_to_sas";
                                              shared = {
                                                uuid = "7699480639143963729";
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
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
                                                      { "sas1-1967.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:13a:feaa:14ff:fede:3fd0"; };
                                                      { "sas1-2927.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:631:225:90ff:fe88:b67a"; };
                                                      { "sas1-3016.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10d:225:90ff:fe88:5022"; };
                                                      { "sas1-3248.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:106:225:90ff:fe88:b41e"; };
                                                      { "sas1-3319.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10a:225:90ff:fe83:1f0e"; };
                                                      { "sas1-5658.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                      { "sas2-6330.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:6f9:96de:80ff:fe8c:dc90"; };
                                                      { "sas2-7140.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:566:0:604:3564:541f"; };
                                                      { "sas2-8842.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd79"; };
                                                      { "sas2-8843.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd5c"; };
                                                      { "sas2-8847.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd63"; };
                                                      { "sas2-8850.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:64c:0:604:5ecc:f0b8"; };
                                                      { "sas2-9413.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:5c8:0:604:7a1d:5f56"; };
                                                      { "sas2-9430.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a354"; };
                                                      { "sas2-9431.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a054"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "4s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- shared
                                            }; -- report
                                          }; -- api_sas
                                          api_man = {
                                            weight = 1.000;
                                            report = {
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              refers = "requests_api_cards_to_man";
                                              shared = {
                                                uuid = "3196941757844534072";
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
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
                                                      { "man1-9350.search.yandex.net"; 11840; 640.000; "2a02:6b8:b000:6085:92e2:baff:fea2:33c2"; };
                                                      { "man2-1272.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:de49"; };
                                                      { "man2-1277.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de46"; };
                                                      { "man2-1278.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dacc"; };
                                                      { "man2-1279.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dad3"; };
                                                      { "man2-1293.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:db12"; };
                                                      { "man2-1309.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0b6"; };
                                                      { "man2-1319.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0c8"; };
                                                      { "man2-1323.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de51"; };
                                                      { "man2-1657.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:7c5e"; };
                                                      { "man2-1662.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:6fee"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "4s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- shared
                                            }; -- report
                                          }; -- api_man
                                          api_vla = {
                                            weight = 1.000;
                                            report = {
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              refers = "requests_api_cards_to_vla";
                                              shared = {
                                                uuid = "3811787060572477829";
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
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
                                                      { "vla1-0090.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:2d:0:604:db7:9d37"; };
                                                      { "vla1-0237.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:81:0:604:db7:a7df"; };
                                                      { "vla1-0548.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:17:0:604:db7:991f"; };
                                                      { "vla1-2088.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:1b:0:604:db7:9985"; };
                                                      { "vla1-2337.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:7f:0:604:db7:9e61"; };
                                                      { "vla1-2488.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:79:0:604:db7:a96c"; };
                                                      { "vla1-2490.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:94:0:604:db7:a94e"; };
                                                      { "vla1-4409.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:90:0:604:db7:a19d"; };
                                                      { "vla2-1025.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                      { "vla2-1027.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:9ad5:e86e"; };
                                                      { "vla2-1047.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77ea"; };
                                                      { "vla2-7970.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:373:0:604:5e97:e21e"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "4s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- shared
                                            }; -- report
                                          }; -- api_vla
                                          api_devnull = {
                                            weight = -1.000;
                                            shared = {
                                              uuid = "5159940746290666347";
                                            }; -- shared
                                          }; -- api_devnull
                                        }; -- rr
                                        on_error = {
                                          errordocument = {
                                            status = 504;
                                            force_conn_close = false;
                                            content = "Gateway Timeout";
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- request_replier
                                }; -- get_method
                                default = {
                                  priority = 1;
                                  shared = {
                                    uuid = "collections_api_cards_backends";
                                  }; -- shared
                                }; -- default
                              }; -- regexp
                            }; -- icookie
                          }; -- report
                        }; -- shared
                      }; -- common_api_cards
                      common_api_content = {
                        priority = 10;
                        match_fsm = {
                          URI = "(/collections)?/api/content(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "4497430824356488439";
                          report = {
                            uuid = "api_content";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            icookie = {
                              use_default_keys = true;
                              domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua,.yandex.uz";
                              trust_parent = false;
                              trust_children = false;
                              enable_set_cookie = true;
                              enable_decrypting = true;
                              decrypted_uid_header = "X-Yandex-ICookie";
                              error_header = "X-Yandex-ICookie-Error";
                              regexp = {
                                post_method = {
                                  priority = 2;
                                  match_fsm = {
                                    match = "POST.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      api_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_content_to_sas";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "3004769924578274489";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 1;
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "sas1-3848.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:10d:225:90ff:fe88:d400"; };
                                                  { "sas1-3928.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:634:225:90ff:fe83:38c8"; };
                                                  { "sas1-5658.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                  { "sas1-8294.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:68a:922b:34ff:fecf:33ea"; };
                                                  { "sas2-0863.search.yandex.net"; 28200; 240.000; "2a02:6b8:c02:40a:0:604:dde:f5e4"; };
                                                  { "sas2-5926.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:2faa"; };
                                                  { "sas2-5939.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f8:96de:80ff:fe8c:df72"; };
                                                  { "sas2-5947.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fc:96de:80ff:fe8e:7b04"; };
                                                  { "sas2-6002.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:1f7:225:90ff:fe94:144e"; };
                                                  { "sas2-6057.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:183:225:90ff:fe92:b05c"; };
                                                  { "sas2-6136.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:288a"; };
                                                  { "sas2-6171.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f7:76d4:35ff:fec6:34c2"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "60s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- report
                                      }; -- api_sas
                                      api_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_content_to_man";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2225175157949116909";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 1;
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "man1-8453.search.yandex.net"; 7980; 240.000; "2a02:6b8:b000:6081:e61d:2dff:fe6d:ff90"; };
                                                  { "man2-1285.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:823:0:604:5e97:ddf3"; };
                                                  { "man2-1608.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:82a:0:604:5e97:dfc7"; };
                                                  { "man2-2286.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0fc"; };
                                                  { "man2-2288.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df61"; };
                                                  { "man2-2292.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df2d"; };
                                                  { "man2-2294.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:dbe7"; };
                                                  { "man2-2295.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df21"; };
                                                  { "man2-2303.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:847:0:604:5ecc:f087"; };
                                                  { "man2-2304.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df60"; };
                                                  { "man2-2306.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0cf"; };
                                                  { "man2-2307.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e21c"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "60s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- report
                                      }; -- api_man
                                      api_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_content_to_vla";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "1900388543342929914";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 1;
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "vla1-0125.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:3f:0:604:db7:a707"; };
                                                  { "vla1-0317.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:78:0:604:db7:a7c3"; };
                                                  { "vla1-1647.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:67:0:604:db7:a27f"; };
                                                  { "vla1-2509.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:79:0:604:5e19:439f"; };
                                                  { "vla1-2974.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:a3:0:604:db7:9b96"; };
                                                  { "vla1-3116.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:6c:0:604:db7:a630"; };
                                                  { "vla1-3681.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:87:0:604:db7:a822"; };
                                                  { "vla2-1025.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                  { "vla2-3866.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:ced9"; };
                                                  { "vla2-3869.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cea4"; };
                                                  { "vla2-3870.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cee7"; };
                                                  { "vla2-7979.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:373:0:604:5e97:e20a"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "60s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- report
                                      }; -- api_vla
                                      api_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "7565505340769584015";
                                          report = {
                                            uuid = "requests_api_content_to_devnull";
                                            ranges = "1ms";
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
                                        }; -- shared
                                      }; -- api_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- post_method
                                default = {
                                  priority = 1;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    attempts_file = "./controls/api_content.attempts";
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      api_sas = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
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
                                                { "sas1-3848.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:10d:225:90ff:fe88:d400"; };
                                                { "sas1-3928.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:634:225:90ff:fe83:38c8"; };
                                                { "sas1-5658.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                { "sas1-8294.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:68a:922b:34ff:fecf:33ea"; };
                                                { "sas2-0863.search.yandex.net"; 28200; 240.000; "2a02:6b8:c02:40a:0:604:dde:f5e4"; };
                                                { "sas2-5926.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:2faa"; };
                                                { "sas2-5939.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f8:96de:80ff:fe8c:df72"; };
                                                { "sas2-5947.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fc:96de:80ff:fe8e:7b04"; };
                                                { "sas2-6002.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:1f7:225:90ff:fe94:144e"; };
                                                { "sas2-6057.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:183:225:90ff:fe92:b05c"; };
                                                { "sas2-6136.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:288a"; };
                                                { "sas2-6171.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f7:76d4:35ff:fec6:34c2"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_content_to_sas";
                                        }; -- report
                                      }; -- api_sas
                                      api_man = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
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
                                                { "man1-8453.search.yandex.net"; 7980; 240.000; "2a02:6b8:b000:6081:e61d:2dff:fe6d:ff90"; };
                                                { "man2-1285.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:823:0:604:5e97:ddf3"; };
                                                { "man2-1608.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:82a:0:604:5e97:dfc7"; };
                                                { "man2-2286.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0fc"; };
                                                { "man2-2288.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df61"; };
                                                { "man2-2292.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df2d"; };
                                                { "man2-2294.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:dbe7"; };
                                                { "man2-2295.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df21"; };
                                                { "man2-2303.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:847:0:604:5ecc:f087"; };
                                                { "man2-2304.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df60"; };
                                                { "man2-2306.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0cf"; };
                                                { "man2-2307.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e21c"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_content_to_man";
                                        }; -- report
                                      }; -- api_man
                                      api_vla = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
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
                                                { "vla1-0125.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:3f:0:604:db7:a707"; };
                                                { "vla1-0317.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:78:0:604:db7:a7c3"; };
                                                { "vla1-1647.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:67:0:604:db7:a27f"; };
                                                { "vla1-2509.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:79:0:604:5e19:439f"; };
                                                { "vla1-2974.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:a3:0:604:db7:9b96"; };
                                                { "vla1-3116.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:6c:0:604:db7:a630"; };
                                                { "vla1-3681.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:87:0:604:db7:a822"; };
                                                { "vla2-1025.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                { "vla2-3866.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:ced9"; };
                                                { "vla2-3869.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cea4"; };
                                                { "vla2-3870.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cee7"; };
                                                { "vla2-7979.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:373:0:604:5e97:e20a"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_content_to_vla";
                                        }; -- report
                                      }; -- api_vla
                                      api_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "7565505340769584015";
                                        }; -- shared
                                      }; -- api_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- default
                              }; -- regexp
                            }; -- icookie
                          }; -- report
                        }; -- shared
                      }; -- common_api_content
                      common_api_browser_bookmarks = {
                        priority = 9;
                        match_fsm = {
                          URI = "(/collections)?/api/v1.0/browser/bookmarks(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "4932708923836649592";
                          report = {
                            uuid = "api_browser_bookmarks";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            icookie = {
                              use_default_keys = true;
                              domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua,.yandex.uz";
                              trust_parent = false;
                              trust_children = false;
                              enable_set_cookie = true;
                              enable_decrypting = true;
                              decrypted_uid_header = "X-Yandex-ICookie";
                              error_header = "X-Yandex-ICookie-Error";
                              regexp = {
                                post_method = {
                                  priority = 2;
                                  match_fsm = {
                                    match = "POST.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      api_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_browser_bookmarks_to_sas";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "3004769924578274489";
                                          }; -- shared
                                        }; -- report
                                      }; -- api_sas
                                      api_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_browser_bookmarks_to_man";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2225175157949116909";
                                          }; -- shared
                                        }; -- report
                                      }; -- api_man
                                      api_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_browser_bookmarks_to_vla";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "1900388543342929914";
                                          }; -- shared
                                        }; -- report
                                      }; -- api_vla
                                      api_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "5631938353583854700";
                                          report = {
                                            uuid = "requests_api_browser_bookmarks_to_devnull";
                                            ranges = "1ms";
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
                                        }; -- shared
                                      }; -- api_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 500;
                                        force_conn_close = false;
                                        content = "Internal Server Error";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- post_method
                                default = {
                                  priority = 1;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    attempts_file = "./controls/api_content.attempts";
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      api_sas = {
                                        weight = 1.000;
                                        report = {
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
                                                { "sas1-3848.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:10d:225:90ff:fe88:d400"; };
                                                { "sas1-3928.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:634:225:90ff:fe83:38c8"; };
                                                { "sas1-5658.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                { "sas1-8294.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:68a:922b:34ff:fecf:33ea"; };
                                                { "sas2-0863.search.yandex.net"; 28200; 240.000; "2a02:6b8:c02:40a:0:604:dde:f5e4"; };
                                                { "sas2-5926.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:2faa"; };
                                                { "sas2-5939.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f8:96de:80ff:fe8c:df72"; };
                                                { "sas2-5947.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fc:96de:80ff:fe8e:7b04"; };
                                                { "sas2-6002.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:1f7:225:90ff:fe94:144e"; };
                                                { "sas2-6057.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:183:225:90ff:fe92:b05c"; };
                                                { "sas2-6136.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:288a"; };
                                                { "sas2-6171.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f7:76d4:35ff:fec6:34c2"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_browser_bookmarks_to_sas";
                                        }; -- report
                                      }; -- api_sas
                                      api_man = {
                                        weight = 1.000;
                                        report = {
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
                                                { "man1-8453.search.yandex.net"; 7980; 240.000; "2a02:6b8:b000:6081:e61d:2dff:fe6d:ff90"; };
                                                { "man2-1285.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:823:0:604:5e97:ddf3"; };
                                                { "man2-1608.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:82a:0:604:5e97:dfc7"; };
                                                { "man2-2286.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0fc"; };
                                                { "man2-2288.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df61"; };
                                                { "man2-2292.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df2d"; };
                                                { "man2-2294.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:dbe7"; };
                                                { "man2-2295.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df21"; };
                                                { "man2-2303.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:847:0:604:5ecc:f087"; };
                                                { "man2-2304.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df60"; };
                                                { "man2-2306.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0cf"; };
                                                { "man2-2307.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e21c"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_browser_bookmarks_to_man";
                                        }; -- report
                                      }; -- api_man
                                      api_vla = {
                                        weight = 1.000;
                                        report = {
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
                                                { "vla1-0125.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:3f:0:604:db7:a707"; };
                                                { "vla1-0317.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:78:0:604:db7:a7c3"; };
                                                { "vla1-1647.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:67:0:604:db7:a27f"; };
                                                { "vla1-2509.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:79:0:604:5e19:439f"; };
                                                { "vla1-2974.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:a3:0:604:db7:9b96"; };
                                                { "vla1-3116.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:6c:0:604:db7:a630"; };
                                                { "vla1-3681.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:87:0:604:db7:a822"; };
                                                { "vla2-1025.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                { "vla2-3866.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:ced9"; };
                                                { "vla2-3869.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cea4"; };
                                                { "vla2-3870.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cee7"; };
                                                { "vla2-7979.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:373:0:604:5e97:e20a"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_browser_bookmarks_to_vla";
                                        }; -- report
                                      }; -- api_vla
                                      api_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "5631938353583854700";
                                        }; -- shared
                                      }; -- api_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 500;
                                        force_conn_close = false;
                                        content = "Internal Server Error";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- default
                              }; -- regexp
                            }; -- icookie
                          }; -- report
                        }; -- shared
                      }; -- common_api_browser_bookmarks
                      common_api_subscriptions_bulk = {
                        priority = 8;
                        match_fsm = {
                          URI = "(/collections)?/api/subscriptions/bulk(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "8897984711881177651";
                          report = {
                            uuid = "api_subscriptions_bulk";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            icookie = {
                              use_default_keys = true;
                              domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua,.yandex.uz";
                              trust_parent = false;
                              trust_children = false;
                              enable_set_cookie = true;
                              enable_decrypting = true;
                              decrypted_uid_header = "X-Yandex-ICookie";
                              error_header = "X-Yandex-ICookie-Error";
                              regexp = {
                                post_method = {
                                  priority = 2;
                                  match_fsm = {
                                    match = "POST.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      api_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_subscriptions_bulk_to_sas";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "386324191296716630";
                                          }; -- shared
                                        }; -- report
                                      }; -- api_sas
                                      api_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_subscriptions_bulk_to_man";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "7332608112167330077";
                                          }; -- shared
                                        }; -- report
                                      }; -- api_man
                                      api_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_api_subscriptions_bulk_to_vla";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "4971768765950878358";
                                          }; -- shared
                                        }; -- report
                                      }; -- api_vla
                                      api_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "2333793451841938388";
                                          report = {
                                            uuid = "requests_api_subscriptions_bulk_to_devnull";
                                            ranges = "1ms";
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
                                        }; -- shared
                                      }; -- api_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- post_method
                                default = {
                                  priority = 1;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    attempts_file = "./controls/api_subscriptions_bulk.attempts";
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      api_sas = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
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
                                                { "sas1-1967.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:13a:feaa:14ff:fede:3fd0"; };
                                                { "sas1-2927.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:631:225:90ff:fe88:b67a"; };
                                                { "sas1-3016.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10d:225:90ff:fe88:5022"; };
                                                { "sas1-3248.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:106:225:90ff:fe88:b41e"; };
                                                { "sas1-3319.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10a:225:90ff:fe83:1f0e"; };
                                                { "sas1-5658.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                { "sas2-6330.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:6f9:96de:80ff:fe8c:dc90"; };
                                                { "sas2-7140.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:566:0:604:3564:541f"; };
                                                { "sas2-8842.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd79"; };
                                                { "sas2-8843.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd5c"; };
                                                { "sas2-8847.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd63"; };
                                                { "sas2-8850.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:64c:0:604:5ecc:f0b8"; };
                                                { "sas2-9413.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:5c8:0:604:7a1d:5f56"; };
                                                { "sas2-9430.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a354"; };
                                                { "sas2-9431.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a054"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_subscriptions_bulk_to_sas";
                                        }; -- report
                                      }; -- api_sas
                                      api_man = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
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
                                                { "man1-9350.search.yandex.net"; 11840; 640.000; "2a02:6b8:b000:6085:92e2:baff:fea2:33c2"; };
                                                { "man2-1272.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:de49"; };
                                                { "man2-1277.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de46"; };
                                                { "man2-1278.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dacc"; };
                                                { "man2-1279.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dad3"; };
                                                { "man2-1293.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:db12"; };
                                                { "man2-1309.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0b6"; };
                                                { "man2-1319.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0c8"; };
                                                { "man2-1323.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de51"; };
                                                { "man2-1657.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:7c5e"; };
                                                { "man2-1662.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:6fee"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_subscriptions_bulk_to_man";
                                        }; -- report
                                      }; -- api_man
                                      api_vla = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
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
                                                { "vla1-0090.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:2d:0:604:db7:9d37"; };
                                                { "vla1-0237.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:81:0:604:db7:a7df"; };
                                                { "vla1-0548.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:17:0:604:db7:991f"; };
                                                { "vla1-2088.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:1b:0:604:db7:9985"; };
                                                { "vla1-2337.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:7f:0:604:db7:9e61"; };
                                                { "vla1-2488.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:79:0:604:db7:a96c"; };
                                                { "vla1-2490.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:94:0:604:db7:a94e"; };
                                                { "vla1-4409.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:90:0:604:db7:a19d"; };
                                                { "vla2-1025.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                { "vla2-1027.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:9ad5:e86e"; };
                                                { "vla2-1047.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77ea"; };
                                                { "vla2-7970.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:373:0:604:5e97:e21e"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "15s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_api_subscriptions_bulk_to_vla";
                                        }; -- report
                                      }; -- api_vla
                                      api_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "2333793451841938388";
                                        }; -- shared
                                      }; -- api_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- default
                              }; -- regexp
                            }; -- icookie
                          }; -- report
                        }; -- shared
                      }; -- common_api_subscriptions_bulk
                      common_api_user = {
                        priority = 7;
                        match_fsm = {
                          URI = "(/collections)?/api/user(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "753735758918296282";
                          report = {
                            uuid = "api_user";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            icookie = {
                              use_default_keys = true;
                              domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua,.yandex.uz";
                              trust_parent = false;
                              trust_children = false;
                              enable_set_cookie = true;
                              enable_decrypting = true;
                              decrypted_uid_header = "X-Yandex-ICookie";
                              error_header = "X-Yandex-ICookie-Error";
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
                                regexp = {
                                  post_method = {
                                    priority = 3;
                                    match_fsm = {
                                      match = "POST.*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    shared = {
                                      uuid = "3834626364175272938";
                                    }; -- shared
                                  }; -- post_method
                                  get_method = {
                                    priority = 2;
                                    match_fsm = {
                                      match = "GET.*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    request_replier = {
                                      sink = {
                                        report = {
                                          uuid = "requests_api_user_to_prestable";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
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
                                                { "man1-0944.search.yandex.net"; 19040; 40.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f6ae"; };
                                                { "man1-3412.search.yandex.net"; 19040; 40.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:df90"; };
                                                { "man1-8397.search.yandex.net"; 19040; 40.000; "2a02:6b8:b000:6082:e61d:2dff:fe6c:fa10"; };
                                                { "sas1-9531.search.yandex.net"; 19040; 40.000; "2a02:6b8:b000:19a:428d:5cff:fef4:8ce9"; };
                                                { "sas2-0139.search.yandex.net"; 19040; 40.000; "2a02:6b8:c02:419:0:604:df5:d7d5"; };
                                                { "sas2-0209.search.yandex.net"; 19040; 40.000; "2a02:6b8:c02:41e:0:604:df5:d61e"; };
                                                { "vla1-0395.search.yandex.net"; 19040; 40.000; "2a02:6b8:c0e:9d:0:604:d8f:eb84"; };
                                                { "vla1-3160.search.yandex.net"; 19040; 40.000; "2a02:6b8:c0e:30:0:604:db7:9d83"; };
                                                { "vla1-4566.search.yandex.net"; 19040; 40.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
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
                                      }; -- sink
                                      enable_failed_requests_replication = false;
                                      rate = 0.000;
                                      rate_file = "./controls/request_repl.ratefile";
                                      shared = {
                                        uuid = "collections_api_user_backends";
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          attempts_file = "./controls/api.attempts";
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            api_sas = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "requests_api_to_sas";
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
                                                      { "sas1-1967.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:13a:feaa:14ff:fede:3fd0"; };
                                                      { "sas1-2927.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:631:225:90ff:fe88:b67a"; };
                                                      { "sas1-3016.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10d:225:90ff:fe88:5022"; };
                                                      { "sas1-3248.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:106:225:90ff:fe88:b41e"; };
                                                      { "sas1-3319.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10a:225:90ff:fe83:1f0e"; };
                                                      { "sas1-5658.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                      { "sas2-6330.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:6f9:96de:80ff:fe8c:dc90"; };
                                                      { "sas2-7140.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:566:0:604:3564:541f"; };
                                                      { "sas2-8842.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd79"; };
                                                      { "sas2-8843.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd5c"; };
                                                      { "sas2-8847.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd63"; };
                                                      { "sas2-8850.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:64c:0:604:5ecc:f0b8"; };
                                                      { "sas2-9413.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:5c8:0:604:7a1d:5f56"; };
                                                      { "sas2-9430.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a354"; };
                                                      { "sas2-9431.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a054"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "1s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- report
                                            }; -- api_sas
                                            api_man = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "requests_api_to_man";
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
                                                      { "man1-9350.search.yandex.net"; 11840; 640.000; "2a02:6b8:b000:6085:92e2:baff:fea2:33c2"; };
                                                      { "man2-1272.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:de49"; };
                                                      { "man2-1277.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de46"; };
                                                      { "man2-1278.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dacc"; };
                                                      { "man2-1279.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dad3"; };
                                                      { "man2-1293.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:db12"; };
                                                      { "man2-1309.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0b6"; };
                                                      { "man2-1319.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0c8"; };
                                                      { "man2-1323.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de51"; };
                                                      { "man2-1657.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:7c5e"; };
                                                      { "man2-1662.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:6fee"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "1s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- report
                                            }; -- api_man
                                            api_vla = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "requests_api_to_vla";
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
                                                      { "vla1-0090.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:2d:0:604:db7:9d37"; };
                                                      { "vla1-0237.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:81:0:604:db7:a7df"; };
                                                      { "vla1-0548.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:17:0:604:db7:991f"; };
                                                      { "vla1-2088.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:1b:0:604:db7:9985"; };
                                                      { "vla1-2337.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:7f:0:604:db7:9e61"; };
                                                      { "vla1-2488.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:79:0:604:db7:a96c"; };
                                                      { "vla1-2490.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:94:0:604:db7:a94e"; };
                                                      { "vla1-4409.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:90:0:604:db7:a19d"; };
                                                      { "vla2-1025.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                      { "vla2-1027.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:9ad5:e86e"; };
                                                      { "vla2-1047.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77ea"; };
                                                      { "vla2-7970.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:373:0:604:5e97:e21e"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "1s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 1;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- report
                                            }; -- api_vla
                                            api_devnull = {
                                              weight = -1.000;
                                              shared = {
                                                uuid = "5506527495944560891";
                                              }; -- shared
                                            }; -- api_devnull
                                          }; -- rr
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                              content = "Gateway Timeout";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- shared
                                    }; -- request_replier
                                  }; -- get_method
                                  default = {
                                    priority = 1;
                                    shared = {
                                      uuid = "collections_api_user_backends";
                                    }; -- shared
                                  }; -- default
                                }; -- regexp
                              }; -- geobase
                            }; -- icookie
                          }; -- report
                        }; -- shared
                      }; -- common_api_user
                      common_api_verticals = {
                        priority = 6;
                        match_fsm = {
                          URI = "(/collections)?/api/verticals/detect(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "8752760931700931323";
                          report = {
                            uuid = "api_verticals";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            regexp = {
                              post_method = {
                                priority = 2;
                                match_fsm = {
                                  match = "POST.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    verticals_sas = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_verticals_to_sas";
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
                                          active = {
                                            delay = "10s";
                                            request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                            steady = false;
                                            unpack(gen_proxy_backends({
                                              { "sas1-1540.search.yandex.net"; 29580; 40.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cb6"; };
                                              { "sas1-9040.search.yandex.net"; 29580; 40.000; "2a02:6b8:b000:143:feaa:14ff:fede:3f28"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "4s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- active
                                        }; -- balancer2
                                      }; -- report
                                    }; -- verticals_sas
                                    verticals_man = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_verticals_to_man";
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
                                          active = {
                                            delay = "10s";
                                            request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                            steady = false;
                                            unpack(gen_proxy_backends({
                                              { "man1-4792.search.yandex.net"; 23900; 1.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:8ab0"; };
                                              { "man1-6910.search.yandex.net"; 23900; 1.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:2230"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "4s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- active
                                        }; -- balancer2
                                      }; -- report
                                    }; -- verticals_man
                                    verticals_vla = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_verticals_to_vla";
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
                                          active = {
                                            delay = "10s";
                                            request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                            steady = false;
                                            unpack(gen_proxy_backends({
                                              { "vla1-0089.search.yandex.net"; 29580; 200.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                              { "vla1-1981.search.yandex.net"; 29580; 200.000; "2a02:6b8:c0e:48:0:604:db7:a200"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "4s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- active
                                        }; -- balancer2
                                      }; -- report
                                    }; -- verticals_vla
                                    verticals_devnull = {
                                      weight = -1.000;
                                      shared = {
                                        uuid = "2577071471818997990";
                                        report = {
                                          uuid = "requests_verticals_to_devnull";
                                          ranges = "1ms";
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
                                      }; -- shared
                                    }; -- verticals_devnull
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Gateway Timeout";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- post_method
                              default = {
                                priority = 1;
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  attempts_file = "./controls/verticals.attempts";
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    verticals_sas = {
                                      weight = 1.000;
                                      report = {
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
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
                                              { "sas1-1540.search.yandex.net"; 29580; 40.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cb6"; };
                                              { "sas1-9040.search.yandex.net"; 29580; 40.000; "2a02:6b8:b000:143:feaa:14ff:fede:3f28"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "4s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_verticals_to_sas";
                                      }; -- report
                                    }; -- verticals_sas
                                    verticals_man = {
                                      weight = 1.000;
                                      report = {
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
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
                                              { "man1-4792.search.yandex.net"; 23900; 1.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:8ab0"; };
                                              { "man1-6910.search.yandex.net"; 23900; 1.000; "2a02:6b8:b000:6060:e61d:2dff:fe04:2230"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "4s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_verticals_to_man";
                                      }; -- report
                                    }; -- verticals_man
                                    verticals_vla = {
                                      weight = 1.000;
                                      report = {
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
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
                                              { "vla1-0089.search.yandex.net"; 29580; 200.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                              { "vla1-1981.search.yandex.net"; 29580; 200.000; "2a02:6b8:c0e:48:0:604:db7:a200"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "4s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_verticals_to_vla";
                                      }; -- report
                                    }; -- verticals_vla
                                    verticals_devnull = {
                                      weight = -1.000;
                                      shared = {
                                        uuid = "2577071471818997990";
                                      }; -- shared
                                    }; -- verticals_devnull
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Gateway Timeout";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- default
                            }; -- regexp
                          }; -- report
                        }; -- shared
                      }; -- common_api_verticals
                      int_api_informers_sas = {
                        priority = 5;
                        match_fsm = {
                          URI = "/api/informers(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "api_informers";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          balancer2 = {
                            timeout_policy = {
                              timeout = "120ms";
                              by_name_policy = {
                                name = get_geo("informers_", "random");
                                unique_policy = {};
                              }; -- by_name_policy
                            }; -- timeout_policy
                            attempts = 3;
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              informers_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_informers_to_sas";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "sas1-2626-sas-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c08:1705:10b:18e7:0:21fc"; };
                                        { "sas1-2729-sas-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c08:1889:10b:18e7:0:21fc"; };
                                        { "sas1-5376-sas-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c08:8895:10b:18e7:0:21fc"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "10ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 4;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- informers_sas
                              informers_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_informers_to_man";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "man1-1438-man-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0b:21a0:10b:18e6:0:21fc"; };
                                        { "man1-3978-man-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0b:2617:10b:18e6:0:21fc"; };
                                        { "man2-0315-man-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0b:4432:10b:18e6:0:21fc"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "35ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 4;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- informers_man
                              informers_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_api_informers_to_vla";
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
                                    active = {
                                      delay = "500ms";
                                      request = "GET /health HTTP/1.1\r\nHost: pdb.yandex.ru\r\n\r\n";
                                      steady = false;
                                      unpack(gen_proxy_backends({
                                        { "vla1-4309-vla-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0d:3907:10b:3001:0:21fc"; };
                                        { "vla1-5966-vla-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0f:160b:10b:3001:0:21fc"; };
                                        { "vla1-5994-vla-pdb-informers-production-8700.gencfg-c.yandex.net"; 8700; 320.000; "2a02:6b8:c0d:4da3:10b:3001:0:21fc"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "15ms";
                                        backend_timeout = "200ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 4;
                                        need_resolve = true;
                                      }))
                                    }; -- active
                                  }; -- balancer2
                                }; -- report
                              }; -- informers_vla
                              informers_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "requests_api_informers_to_devnull";
                                  ranges = "1ms";
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
                              }; -- informers_devnull
                            }; -- rr
                            on_error = {
                              errordocument = {
                                status = 504;
                                force_conn_close = false;
                                content = "Gateway Timeout";
                              }; -- errordocument
                            }; -- on_error
                          }; -- balancer2
                        }; -- report
                      }; -- int_api_informers_sas
                      common_api = {
                        priority = 4;
                        match_fsm = {
                          URI = "(/collections)?/api(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "5560322912116957292";
                          report = {
                            uuid = "api";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            icookie = {
                              use_default_keys = true;
                              domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua,.yandex.uz";
                              trust_parent = false;
                              trust_children = false;
                              enable_set_cookie = true;
                              enable_decrypting = true;
                              decrypted_uid_header = "X-Yandex-ICookie";
                              error_header = "X-Yandex-ICookie-Error";
                              regexp = {
                                post_method = {
                                  priority = 3;
                                  match_fsm = {
                                    match = "POST.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  shared = {
                                    uuid = "3834626364175272938";
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      rr = {
                                        weights_file = "./controls/traffic_control.weights";
                                        api_sas = {
                                          weight = 1.000;
                                          report = {
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
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "sas1-1967.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:13a:feaa:14ff:fede:3fd0"; };
                                                  { "sas1-2927.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:631:225:90ff:fe88:b67a"; };
                                                  { "sas1-3016.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10d:225:90ff:fe88:5022"; };
                                                  { "sas1-3248.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:106:225:90ff:fe88:b41e"; };
                                                  { "sas1-3319.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:10a:225:90ff:fe83:1f0e"; };
                                                  { "sas1-5658.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                  { "sas2-6330.search.yandex.net"; 28797; 640.000; "2a02:6b8:b000:6f9:96de:80ff:fe8c:dc90"; };
                                                  { "sas2-7140.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:566:0:604:3564:541f"; };
                                                  { "sas2-8842.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd79"; };
                                                  { "sas2-8843.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd5c"; };
                                                  { "sas2-8847.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:606:0:604:5e97:dd63"; };
                                                  { "sas2-8850.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:64c:0:604:5ecc:f0b8"; };
                                                  { "sas2-9413.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:5c8:0:604:7a1d:5f56"; };
                                                  { "sas2-9430.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a354"; };
                                                  { "sas2-9431.search.yandex.net"; 28797; 640.000; "2a02:6b8:c02:b11:0:604:90c2:a054"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "4s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                            refers = "requests_api_to_sas";
                                          }; -- report
                                        }; -- api_sas
                                        api_man = {
                                          weight = 1.000;
                                          report = {
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
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "man1-9350.search.yandex.net"; 11840; 640.000; "2a02:6b8:b000:6085:92e2:baff:fea2:33c2"; };
                                                  { "man2-1272.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:de49"; };
                                                  { "man2-1277.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de46"; };
                                                  { "man2-1278.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dacc"; };
                                                  { "man2-1279.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:dad3"; };
                                                  { "man2-1293.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:db12"; };
                                                  { "man2-1309.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0b6"; };
                                                  { "man2-1319.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:824:0:604:5e97:e0c8"; };
                                                  { "man2-1323.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:828:0:604:5e97:de51"; };
                                                  { "man2-1657.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:7c5e"; };
                                                  { "man2-1662.search.yandex.net"; 11840; 640.000; "2a02:6b8:c01:738:0:604:b2a9:6fee"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "4s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                            refers = "requests_api_to_man";
                                          }; -- report
                                        }; -- api_man
                                        api_vla = {
                                          weight = 1.000;
                                          report = {
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
                                              active = {
                                                delay = "10s";
                                                request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                                steady = false;
                                                unpack(gen_proxy_backends({
                                                  { "vla1-0090.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:2d:0:604:db7:9d37"; };
                                                  { "vla1-0237.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:81:0:604:db7:a7df"; };
                                                  { "vla1-0548.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:17:0:604:db7:991f"; };
                                                  { "vla1-2088.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:1b:0:604:db7:9985"; };
                                                  { "vla1-2337.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:7f:0:604:db7:9e61"; };
                                                  { "vla1-2488.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:79:0:604:db7:a96c"; };
                                                  { "vla1-2490.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:94:0:604:db7:a94e"; };
                                                  { "vla1-4409.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:90:0:604:db7:a19d"; };
                                                  { "vla2-1025.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                  { "vla2-1027.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:9ad5:e86e"; };
                                                  { "vla2-1047.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:409:0:604:4b02:77ea"; };
                                                  { "vla2-7970.search.yandex.net"; 8020; 560.000; "2a02:6b8:c0e:373:0:604:5e97:e21e"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "4s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                            refers = "requests_api_to_vla";
                                          }; -- report
                                        }; -- api_vla
                                        api_devnull = {
                                          weight = -1.000;
                                          shared = {
                                            uuid = "5506527495944560891";
                                            report = {
                                              uuid = "requests_api_to_devnull";
                                              ranges = "1ms";
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
                                          }; -- shared
                                        }; -- api_devnull
                                      }; -- rr
                                      on_error = {
                                        errordocument = {
                                          status = 504;
                                          force_conn_close = false;
                                          content = "Gateway Timeout";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- shared
                                }; -- post_method
                                get_method = {
                                  priority = 2;
                                  match_fsm = {
                                    match = "GET.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  request_replier = {
                                    sink = {
                                      report = {
                                        uuid = "requests_api_to_prestable";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        shared = {
                                          uuid = "7117126246408427627";
                                        }; -- shared
                                      }; -- report
                                    }; -- sink
                                    enable_failed_requests_replication = false;
                                    rate = 0.000;
                                    rate_file = "./controls/request_repl.ratefile";
                                    shared = {
                                      uuid = "collections_api_backends";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        attempts_file = "./controls/api.attempts";
                                        rr = {
                                          weights_file = "./controls/traffic_control.weights";
                                          api_sas = {
                                            weight = 1.000;
                                            report = {
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              refers = "requests_api_to_sas";
                                              shared = {
                                                uuid = "7699480639143963729";
                                              }; -- shared
                                            }; -- report
                                          }; -- api_sas
                                          api_man = {
                                            weight = 1.000;
                                            report = {
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              refers = "requests_api_to_man";
                                              shared = {
                                                uuid = "3196941757844534072";
                                              }; -- shared
                                            }; -- report
                                          }; -- api_man
                                          api_vla = {
                                            weight = 1.000;
                                            report = {
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              refers = "requests_api_to_vla";
                                              shared = {
                                                uuid = "3811787060572477829";
                                              }; -- shared
                                            }; -- report
                                          }; -- api_vla
                                          api_devnull = {
                                            weight = -1.000;
                                            shared = {
                                              uuid = "5506527495944560891";
                                            }; -- shared
                                          }; -- api_devnull
                                        }; -- rr
                                        on_error = {
                                          errordocument = {
                                            status = 504;
                                            force_conn_close = false;
                                            content = "Gateway Timeout";
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- request_replier
                                }; -- get_method
                                default = {
                                  priority = 1;
                                  shared = {
                                    uuid = "collections_api_backends";
                                  }; -- shared
                                }; -- default
                              }; -- regexp
                            }; -- icookie
                          }; -- report
                        }; -- shared
                      }; -- common_api
                      common_picture = {
                        priority = 3;
                        match_fsm = {
                          URI = "(/collections)?/picture(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "1538615649558936508";
                          report = {
                            uuid = "picture";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            icookie = {
                              use_default_keys = true;
                              domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                              trust_parent = false;
                              trust_children = false;
                              enable_set_cookie = true;
                              enable_decrypting = true;
                              decrypted_uid_header = "X-Yandex-ICookie";
                              error_header = "X-Yandex-ICookie-Error";
                              regexp = {
                                post_method = {
                                  priority = 2;
                                  match_fsm = {
                                    match = "POST.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      picture_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_picture_to_sas";
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
                                            active = {
                                              delay = "10s";
                                              request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                              steady = false;
                                              unpack(gen_proxy_backends({
                                                { "sas1-3848.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:10d:225:90ff:fe88:d400"; };
                                                { "sas1-3928.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:634:225:90ff:fe83:38c8"; };
                                                { "sas1-5658.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                { "sas1-8294.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:68a:922b:34ff:fecf:33ea"; };
                                                { "sas2-0863.search.yandex.net"; 28200; 240.000; "2a02:6b8:c02:40a:0:604:dde:f5e4"; };
                                                { "sas2-5926.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:2faa"; };
                                                { "sas2-5939.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f8:96de:80ff:fe8c:df72"; };
                                                { "sas2-5947.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fc:96de:80ff:fe8e:7b04"; };
                                                { "sas2-6002.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:1f7:225:90ff:fe94:144e"; };
                                                { "sas2-6057.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:183:225:90ff:fe92:b05c"; };
                                                { "sas2-6136.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:288a"; };
                                                { "sas2-6171.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f7:76d4:35ff:fec6:34c2"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "5s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- active
                                          }; -- balancer2
                                        }; -- report
                                      }; -- picture_sas
                                      picture_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_picture_to_man";
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
                                            active = {
                                              delay = "10s";
                                              request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                              steady = false;
                                              unpack(gen_proxy_backends({
                                                { "man1-8453.search.yandex.net"; 7980; 240.000; "2a02:6b8:b000:6081:e61d:2dff:fe6d:ff90"; };
                                                { "man2-1285.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:823:0:604:5e97:ddf3"; };
                                                { "man2-1608.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:82a:0:604:5e97:dfc7"; };
                                                { "man2-2286.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0fc"; };
                                                { "man2-2288.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df61"; };
                                                { "man2-2292.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df2d"; };
                                                { "man2-2294.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:dbe7"; };
                                                { "man2-2295.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df21"; };
                                                { "man2-2303.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:847:0:604:5ecc:f087"; };
                                                { "man2-2304.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df60"; };
                                                { "man2-2306.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0cf"; };
                                                { "man2-2307.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e21c"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "5s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- active
                                          }; -- balancer2
                                        }; -- report
                                      }; -- picture_man
                                      picture_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_picture_to_vla";
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
                                            active = {
                                              delay = "10s";
                                              request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                              steady = false;
                                              unpack(gen_proxy_backends({
                                                { "vla1-0125.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:3f:0:604:db7:a707"; };
                                                { "vla1-0317.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:78:0:604:db7:a7c3"; };
                                                { "vla1-1647.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:67:0:604:db7:a27f"; };
                                                { "vla1-2509.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:79:0:604:5e19:439f"; };
                                                { "vla1-2974.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:a3:0:604:db7:9b96"; };
                                                { "vla1-3116.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:6c:0:604:db7:a630"; };
                                                { "vla1-3681.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:87:0:604:db7:a822"; };
                                                { "vla2-1025.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                { "vla2-3866.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:ced9"; };
                                                { "vla2-3869.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cea4"; };
                                                { "vla2-3870.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cee7"; };
                                                { "vla2-7979.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:373:0:604:5e97:e20a"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "5s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- active
                                          }; -- balancer2
                                        }; -- report
                                      }; -- picture_vla
                                      picture_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "6480272459538805647";
                                          report = {
                                            uuid = "requests_picture_to_devnull";
                                            ranges = "1ms";
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
                                        }; -- shared
                                      }; -- picture_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- post_method
                                default = {
                                  priority = 1;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    attempts_file = "./controls/picture.attempts";
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      picture_sas = {
                                        weight = 1.000;
                                        report = {
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
                                                { "sas1-3848.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:10d:225:90ff:fe88:d400"; };
                                                { "sas1-3928.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:634:225:90ff:fe83:38c8"; };
                                                { "sas1-5658.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:161:225:90ff:fee5:bb20"; };
                                                { "sas1-8294.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:68a:922b:34ff:fecf:33ea"; };
                                                { "sas2-0863.search.yandex.net"; 28200; 240.000; "2a02:6b8:c02:40a:0:604:dde:f5e4"; };
                                                { "sas2-5926.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:2faa"; };
                                                { "sas2-5939.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f8:96de:80ff:fe8c:df72"; };
                                                { "sas2-5947.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fc:96de:80ff:fe8e:7b04"; };
                                                { "sas2-6002.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:1f7:225:90ff:fe94:144e"; };
                                                { "sas2-6057.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:183:225:90ff:fe92:b05c"; };
                                                { "sas2-6136.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6fd:76d4:35ff:fec6:288a"; };
                                                { "sas2-6171.search.yandex.net"; 28200; 240.000; "2a02:6b8:b000:6f7:76d4:35ff:fec6:34c2"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "5s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_picture_to_sas";
                                        }; -- report
                                      }; -- picture_sas
                                      picture_man = {
                                        weight = 1.000;
                                        report = {
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
                                                { "man1-8453.search.yandex.net"; 7980; 240.000; "2a02:6b8:b000:6081:e61d:2dff:fe6d:ff90"; };
                                                { "man2-1285.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:823:0:604:5e97:ddf3"; };
                                                { "man2-1608.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:82a:0:604:5e97:dfc7"; };
                                                { "man2-2286.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0fc"; };
                                                { "man2-2288.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df61"; };
                                                { "man2-2292.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df2d"; };
                                                { "man2-2294.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:dbe7"; };
                                                { "man2-2295.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:df21"; };
                                                { "man2-2303.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:847:0:604:5ecc:f087"; };
                                                { "man2-2304.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:826:0:604:5e97:df60"; };
                                                { "man2-2306.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e0cf"; };
                                                { "man2-2307.search.yandex.net"; 7980; 240.000; "2a02:6b8:c01:827:0:604:5e97:e21c"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "5s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_picture_to_man";
                                        }; -- report
                                      }; -- picture_man
                                      picture_vla = {
                                        weight = 1.000;
                                        report = {
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
                                                { "vla1-0125.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:3f:0:604:db7:a707"; };
                                                { "vla1-0317.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:78:0:604:db7:a7c3"; };
                                                { "vla1-1647.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:67:0:604:db7:a27f"; };
                                                { "vla1-2509.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:79:0:604:5e19:439f"; };
                                                { "vla1-2974.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:a3:0:604:db7:9b96"; };
                                                { "vla1-3116.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:6c:0:604:db7:a630"; };
                                                { "vla1-3681.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:87:0:604:db7:a822"; };
                                                { "vla2-1025.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:409:0:604:4b02:77be"; };
                                                { "vla2-3866.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:ced9"; };
                                                { "vla2-3869.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cea4"; };
                                                { "vla2-3870.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:34b:0:604:5e92:cee7"; };
                                                { "vla2-7979.search.yandex.net"; 28200; 200.000; "2a02:6b8:c0e:373:0:604:5e97:e20a"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "5s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- weighted2
                                          }; -- balancer2
                                          refers = "requests_picture_to_vla";
                                        }; -- report
                                      }; -- picture_vla
                                      picture_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "6480272459538805647";
                                        }; -- shared
                                      }; -- picture_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 504;
                                        force_conn_close = false;
                                        content = "Gateway Timeout";
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- default
                              }; -- regexp
                            }; -- icookie
                          }; -- report
                        }; -- shared
                      }; -- common_picture
                      common_sitemap = {
                        priority = 2;
                        match_fsm = {
                          URI = "(/collections)?/sitemap(/.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "2451642220778517758";
                          report = {
                            uuid = "sitemap";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            regexp = {
                              post_method = {
                                priority = 2;
                                match_fsm = {
                                  match = "POST.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    sitemap_sas = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_sitemap_to_sas";
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
                                          active = {
                                            delay = "10s";
                                            request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                            steady = false;
                                            unpack(gen_proxy_backends({
                                              { "sas1-2603.search.yandex.net"; 15780; 80.000; "2a02:6b8:b000:608:225:90ff:fe83:17a2"; };
                                              { "sas1-6431.search.yandex.net"; 15780; 80.000; "2a02:6b8:b000:667:feaa:14ff:fe1d:f212"; };
                                              { "sas1-7145.search.yandex.net"; 15780; 80.000; "2a02:6b8:b000:66a:225:90ff:fec3:509a"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- active
                                        }; -- balancer2
                                      }; -- report
                                    }; -- sitemap_sas
                                    sitemap_man = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_sitemap_to_man";
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
                                          active = {
                                            delay = "10s";
                                            request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                            steady = false;
                                            unpack(gen_proxy_backends({
                                              { "man1-2953.search.yandex.net"; 15780; 1.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:edc0"; };
                                              { "man1-5820.search.yandex.net"; 15780; 1.000; "2a02:6b8:b000:6062:f652:14ff:fef5:cd20"; };
                                              { "man1-5879.search.yandex.net"; 15780; 1.000; "2a02:6b8:b000:6059:f652:14ff:fef5:d070"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- active
                                        }; -- balancer2
                                      }; -- report
                                    }; -- sitemap_man
                                    sitemap_vla = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_sitemap_to_vla";
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
                                          active = {
                                            delay = "10s";
                                            request = "GET /health/ HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                            steady = false;
                                            unpack(gen_proxy_backends({
                                              { "vla1-0193.search.yandex.net"; 15780; 80.000; "2a02:6b8:c0e:88:0:604:db7:a871"; };
                                              { "vla1-0771.search.yandex.net"; 15780; 80.000; "2a02:6b8:c0e:6a:0:604:db7:a451"; };
                                              { "vla1-0895.search.yandex.net"; 15780; 80.000; "2a02:6b8:c0e:64:0:604:db7:a31f"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- active
                                        }; -- balancer2
                                      }; -- report
                                    }; -- sitemap_vla
                                    sitemap_devnull = {
                                      weight = -1.000;
                                      shared = {
                                        uuid = "6188594998980046344";
                                        report = {
                                          uuid = "requests_sitemap_to_devnull";
                                          ranges = "1ms";
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
                                      }; -- shared
                                    }; -- sitemap_devnull
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Gateway Timeout";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- post_method
                              default = {
                                priority = 1;
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  attempts_file = "./controls/sitemap.attempts";
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    sitemap_sas = {
                                      weight = 1.000;
                                      report = {
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
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
                                              { "sas1-2603.search.yandex.net"; 15780; 80.000; "2a02:6b8:b000:608:225:90ff:fe83:17a2"; };
                                              { "sas1-6431.search.yandex.net"; 15780; 80.000; "2a02:6b8:b000:667:feaa:14ff:fe1d:f212"; };
                                              { "sas1-7145.search.yandex.net"; 15780; 80.000; "2a02:6b8:b000:66a:225:90ff:fec3:509a"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_sitemap_to_sas";
                                      }; -- report
                                    }; -- sitemap_sas
                                    sitemap_man = {
                                      weight = 1.000;
                                      report = {
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
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
                                              { "man1-2953.search.yandex.net"; 15780; 1.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:edc0"; };
                                              { "man1-5820.search.yandex.net"; 15780; 1.000; "2a02:6b8:b000:6062:f652:14ff:fef5:cd20"; };
                                              { "man1-5879.search.yandex.net"; 15780; 1.000; "2a02:6b8:b000:6059:f652:14ff:fef5:d070"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_sitemap_to_man";
                                      }; -- report
                                    }; -- sitemap_man
                                    sitemap_vla = {
                                      weight = 1.000;
                                      report = {
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
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
                                              { "vla1-0193.search.yandex.net"; 15780; 80.000; "2a02:6b8:c0e:88:0:604:db7:a871"; };
                                              { "vla1-0771.search.yandex.net"; 15780; 80.000; "2a02:6b8:c0e:6a:0:604:db7:a451"; };
                                              { "vla1-0895.search.yandex.net"; 15780; 80.000; "2a02:6b8:c0e:64:0:604:db7:a31f"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                        refers = "requests_sitemap_to_vla";
                                      }; -- report
                                    }; -- sitemap_vla
                                    sitemap_devnull = {
                                      weight = -1.000;
                                      shared = {
                                        uuid = "6188594998980046344";
                                      }; -- shared
                                    }; -- sitemap_devnull
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Gateway Timeout";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- default
                            }; -- regexp
                          }; -- report
                        }; -- shared
                      }; -- common_sitemap
                      default = {
                        priority = 1;
                        shared = {
                          uuid = "7074885229648664124";
                          report = {
                            uuid = "nodejs";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            icookie = {
                              use_default_keys = true;
                              domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua,.yandex.uz";
                              trust_parent = false;
                              trust_children = false;
                              enable_set_cookie = true;
                              enable_decrypting = true;
                              decrypted_uid_header = "X-Yandex-ICookie";
                              error_header = "X-Yandex-ICookie-Error";
                              regexp = {
                                post_method = {
                                  priority = 2;
                                  match_fsm = {
                                    match = "POST.*";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  balancer2 = {
                                    timeout_policy = {
                                      timeout = "15s";
                                      unique_policy = {};
                                    }; -- timeout_policy
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      nodejs_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_nodejs_to_sas";
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
                                            active = {
                                              delay = "10s";
                                              request = "GET /version.json HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                              steady = false;
                                              unpack(gen_proxy_backends({
                                                { "sas1-2673.search.yandex.net"; 13660; 192.000; "2a02:6b8:b000:606:225:90ff:fe83:1502"; };
                                                { "sas1-2956.search.yandex.net"; 13660; 192.000; "2a02:6b8:b000:108:225:90ff:fe83:2dbc"; };
                                                { "sas1-6014.search.yandex.net"; 13660; 192.000; "2a02:6b8:b000:164:428d:5cff:fe37:ff66"; };
                                                { "sas2-8848.search.yandex.net"; 13660; 192.000; "2a02:6b8:c02:606:0:604:5e97:dd7b"; };
                                                { "sas2-8850.search.yandex.net"; 13660; 192.000; "2a02:6b8:c02:64c:0:604:5ecc:f0b8"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- active
                                          }; -- balancer2
                                        }; -- report
                                      }; -- nodejs_sas
                                      nodejs_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_nodejs_to_man";
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
                                            active = {
                                              delay = "10s";
                                              request = "GET /version.json HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                              steady = false;
                                              unpack(gen_proxy_backends({
                                                { "man1-0412.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                                { "man1-0475.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:602f:92e2:baff:fe74:77f4"; };
                                                { "man1-5263.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:604e:e61d:2dff:fe01:f140"; };
                                                { "man1-5668.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:6047:e61d:2dff:fe00:9340"; };
                                                { "man1-5898.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4ee0"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- active
                                          }; -- balancer2
                                        }; -- report
                                      }; -- nodejs_man
                                      nodejs_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_nodejs_to_vla";
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
                                            active = {
                                              delay = "10s";
                                              request = "GET /version.json HTTP/1.1\nHost: collections.yandex.ru\n\n";
                                              steady = false;
                                              unpack(gen_proxy_backends({
                                                { "vla1-0009.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:9f:0:604:5cf5:bce7"; };
                                                { "vla1-0145.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                                { "vla1-0149.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                                { "vla1-0866.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:44:0:604:db7:a6c3"; };
                                                { "vla1-1086.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:44:0:604:db7:a6be"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "10s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- active
                                          }; -- balancer2
                                        }; -- report
                                      }; -- nodejs_vla
                                      nodejs_devnull = {
                                        weight = -1.000;
                                        shared = {
                                          uuid = "5749295344075636601";
                                          report = {
                                            uuid = "requests_nodejs_to_devnull";
                                            ranges = "1ms";
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
                                        }; -- shared
                                      }; -- nodejs_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 200;
                                        content = "OK";
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- post_method
                                default = {
                                  priority = 1;
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
                                    balancer2 = {
                                      timeout_policy = {
                                        timeout = "11s";
                                        unique_policy = {};
                                      }; -- timeout_policy
                                      attempts = 2;
                                      attempts_file = "./controls/nodejs.attempts";
                                      rr = {
                                        weights_file = "./controls/traffic_control.weights";
                                        nodejs_sas = {
                                          weight = 1.000;
                                          report = {
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
                                                  { "sas1-2673.search.yandex.net"; 13660; 192.000; "2a02:6b8:b000:606:225:90ff:fe83:1502"; };
                                                  { "sas1-2956.search.yandex.net"; 13660; 192.000; "2a02:6b8:b000:108:225:90ff:fe83:2dbc"; };
                                                  { "sas1-6014.search.yandex.net"; 13660; 192.000; "2a02:6b8:b000:164:428d:5cff:fe37:ff66"; };
                                                  { "sas2-8848.search.yandex.net"; 13660; 192.000; "2a02:6b8:c02:606:0:604:5e97:dd7b"; };
                                                  { "sas2-8850.search.yandex.net"; 13660; 192.000; "2a02:6b8:c02:64c:0:604:5ecc:f0b8"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "5s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- weighted2
                                            }; -- balancer2
                                            refers = "requests_nodejs_to_sas";
                                          }; -- report
                                        }; -- nodejs_sas
                                        nodejs_man = {
                                          weight = 1.000;
                                          report = {
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
                                                  { "man1-0412.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7d60"; };
                                                  { "man1-0475.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:602f:92e2:baff:fe74:77f4"; };
                                                  { "man1-5263.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:604e:e61d:2dff:fe01:f140"; };
                                                  { "man1-5668.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:6047:e61d:2dff:fe00:9340"; };
                                                  { "man1-5898.search.yandex.net"; 13920; 120.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4ee0"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "5s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- weighted2
                                            }; -- balancer2
                                            refers = "requests_nodejs_to_man";
                                          }; -- report
                                        }; -- nodejs_man
                                        nodejs_vla = {
                                          weight = 1.000;
                                          report = {
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
                                                  { "vla1-0009.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:9f:0:604:5cf5:bce7"; };
                                                  { "vla1-0145.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:3f:0:604:db7:a611"; };
                                                  { "vla1-0149.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:3f:0:604:db7:a77a"; };
                                                  { "vla1-0866.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:44:0:604:db7:a6c3"; };
                                                  { "vla1-1086.search.yandex.net"; 20845; 120.000; "2a02:6b8:c0e:44:0:604:db7:a6be"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "5s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 1;
                                                  need_resolve = true;
                                                }))
                                              }; -- weighted2
                                            }; -- balancer2
                                            refers = "requests_nodejs_to_vla";
                                          }; -- report
                                        }; -- nodejs_vla
                                        nodejs_devnull = {
                                          weight = -1.000;
                                          shared = {
                                            uuid = "5749295344075636601";
                                          }; -- shared
                                        }; -- nodejs_devnull
                                      }; -- rr
                                      on_error = {
                                        balancer2 = {
                                          timeout_policy = {
                                            timeout = "5s";
                                            unique_policy = {};
                                          }; -- timeout_policy
                                          attempts = 3;
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
                                              { "man1-1031.search.yandex.net"; 19590; 40.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f79a"; };
                                              { "man1-4485.search.yandex.net"; 19590; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bf8e"; };
                                              { "sas1-6230.search.yandex.net"; 19590; 40.000; "2a02:6b8:b000:11b:215:b2ff:fea7:75f9"; };
                                              { "sas1-8416.search.yandex.net"; 19590; 40.000; "2a02:6b8:b000:623:215:b2ff:fea7:aea8"; };
                                              { "vla1-0405.search.yandex.net"; 19590; 40.000; "2a02:6b8:c0e:26:0:604:db7:9fae"; };
                                              { "vla1-4566.search.yandex.net"; 19590; 40.000; "2a02:6b8:c0e:77:0:604:db7:a7b6"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "1s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                              content = "Gateway Timeout";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- geobase
                                }; -- default
                              }; -- regexp
                            }; -- icookie
                          }; -- report
                        }; -- shared
                      }; -- default
                    }; -- regexp
                  }; -- log_headers
                }; -- headers
              }; -- threshold
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- internal_section
  }; -- ipdispatch
}