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
  maxconn = 8000;
  tcp_fastopen = 0;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  tcp_congestion_control = "bbr";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 9200, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 9200;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 9200;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 9200;
      ip = "127.0.0.4";
    };
    {
      port = 443;
      ip = "93.158.134.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.193.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "2a02:6b8::75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "77.88.21.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "87.250.251.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "87.250.250.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.204.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 9201;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 9201;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 80;
      ip = "93.158.134.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "213.180.193.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "2a02:6b8::75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "77.88.21.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "87.250.251.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "87.250.250.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "213.180.204.75";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 9200;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 9200;
      ip = get_ip_by_iproute("v6");
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        9200;
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
        9200;
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
        "93.158.134.75";
        "213.180.193.75";
        "2a02:6b8::75";
        "77.88.21.75";
        "87.250.251.75";
        "87.250.250.75";
        "213.180.204.75";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "242202286905979856";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 9201, "/place/db/www/logs");
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
                log = get_log_path("ssl_sni", 9201, "/place/db/www/logs");
                priv = get_private_cert_path("bar-navig.yandex.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-bar-navig.yandex.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.bar-navig.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.bar-navig.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.bar-navig.yandex.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 9201, "/place/db/www/logs");
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
                    uuid = "upstreams";
                  }; -- shared
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_9201 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        9201;
      }; -- ports
      shared = {
        uuid = "242202286905979856";
      }; -- shared
    }; -- https_section_9201
    http_section_80 = {
      ips = {
        "93.158.134.75";
        "213.180.193.75";
        "2a02:6b8::75";
        "77.88.21.75";
        "87.250.251.75";
        "87.250.250.75";
        "213.180.204.75";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "1926059288835181130";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 9200, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 9200, "/place/db/www/logs");
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
                  uuid = "upstreams";
                  regexp = {
                    ["awacs-balancer-health-check"] = {
                      priority = 2;
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
                    default = {
                      priority = 1;
                      pinger = {
                        lo = 0.500;
                        hi = 0.700;
                        delay = "1s";
                        histtime = "4s";
                        ping_request_data = "GET /u?ver=0&show=0 HTTP/1.1\nHost: bar-navig.yandex.ru\r\n\r\n";
                        admin_request_uri = "/u?ver=0&show=0";
                        admin_ips = "0.0.0.0/0,2a02:6b8::/32";
                        enable_tcp_check_file = "./controls/tcp_check_on";
                        switch_off_file = "./controls/slb_check.weights";
                        switch_off_key = "switch_off";
                        admin_error_replier = {
                          errordocument = {
                            status = 503;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- admin_error_replier
                        module = {
                          hasher = {
                            mode = "barnavig";
                            headers = {
                              create_func = {
                                ["X-Start-Time"] = "starttime";
                              }; -- create_func
                              create_func_weak = {
                                ["X-Forwarded-For-Y"] = "realip";
                              }; -- create_func_weak
                              icookie = {
                                use_default_keys = true;
                                domains = ".yandex.ru,.yandex.by,.yandex.com,.yandex.com.tr,.yandex.kz,.yandex.ua";
                                trust_parent = false;
                                trust_children = false;
                                enable_set_cookie = true;
                                enable_decrypting = true;
                                decrypted_uid_header = "X-Yandex-ICookie";
                                error_header = "X-Yandex-ICookie-Error";
                                force_equal_to_yandexuid = true;
                                balancer2 = {
                                  by_hash_policy = {
                                    unique_policy = {};
                                  }; -- by_hash_policy
                                  attempts = 3;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    bygeo_vla = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_to_vla";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          by_hash_policy = {
                                            timeout_policy = {
                                              timeout = "1s";
                                              unique_policy = {};
                                            }; -- timeout_policy
                                          }; -- by_hash_policy
                                          attempts = 19;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "vla1-0199-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:410d:10b:c448:0:1e5c"; };
                                              { "vla1-0215-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:409c:10b:c448:0:1e5c"; };
                                              { "vla1-0601-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:3899:10b:c448:0:1e5c"; };
                                              { "vla1-0656-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:99e:10b:c448:0:1e5c"; };
                                              { "vla1-1201-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:4108:10b:c448:0:1e5c"; };
                                              { "vla1-1419-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:d8a:10b:c448:0:1e5c"; };
                                              { "vla1-1652-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:389f:10b:c448:0:1e5c"; };
                                              { "vla1-2001-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:3892:10b:c448:0:1e5c"; };
                                              { "vla1-2107-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:1a21:10b:c448:0:1e5c"; };
                                              { "vla1-2443-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:4a19:10b:c448:0:1e5c"; };
                                              { "vla1-2474-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:4a87:10b:c448:0:1e5c"; };
                                              { "vla1-3925-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:481a:10b:c448:0:1e5c"; };
                                              { "vla1-3977-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:4d90:10b:c448:0:1e5c"; };
                                              { "vla1-4389-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0d:4514:10b:c448:0:1e5c"; };
                                              { "vla1-6001-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0f:68a:10b:c448:0:1e5c"; };
                                              { "vla1-6010-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c0f:6a0:10b:c448:0:1e5c"; };
                                              { "vla2-1012-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c15:1b99:10b:c448:0:1e5c"; };
                                              { "vla2-1023-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c15:1b8e:10b:c448:0:1e5c"; };
                                              { "vla2-5966-431-vla-misc-barnav-7772.gencfg-c.yandex.net"; 7772; 80.000; "2a02:6b8:c18:616:10b:c448:0:1e5c"; };
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
                                      }; -- report
                                    }; -- bygeo_vla
                                    bygeo_sas = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_to_sas";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          by_hash_policy = {
                                            timeout_policy = {
                                              timeout = "1s";
                                              unique_policy = {};
                                            }; -- timeout_policy
                                          }; -- by_hash_policy
                                          attempts = 33;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "sas1-2854-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:1b0f:10b:c446:0:36fb"; };
                                              { "sas1-5795-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:220:10b:c446:0:36fb"; };
                                              { "sas1-5805-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:4710:10b:c446:0:36fb"; };
                                              { "sas1-5835-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:80c:10b:c446:0:36fb"; };
                                              { "sas1-5846-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:3594:10b:c446:0:36fb"; };
                                              { "sas1-5853-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:1905:10b:c446:0:36fb"; };
                                              { "sas1-5861-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:1907:10b:c446:0:36fb"; };
                                              { "sas1-5863-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:35a7:10b:c446:0:36fb"; };
                                              { "sas1-5864-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:359b:10b:c446:0:36fb"; };
                                              { "sas1-5918-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:80e:10b:c446:0:36fb"; };
                                              { "sas1-6864-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:4a0c:10b:c446:0:36fb"; };
                                              { "sas1-7246-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:89a7:10b:c446:0:36fb"; };
                                              { "sas1-8195-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:810f:10b:c446:0:36fb"; };
                                              { "sas1-8530-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:5621:10b:c446:0:36fb"; };
                                              { "sas1-9032-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:4d8d:10b:c446:0:36fb"; };
                                              { "sas1-9272-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:6e8d:10b:c446:0:36fb"; };
                                              { "sas1-9397-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:711e:10b:c446:0:36fb"; };
                                              { "sas1-9619-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:7080:10b:c446:0:36fb"; };
                                              { "sas2-0061-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c11:ca0:10b:c446:0:36fb"; };
                                              { "sas2-0089-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c11:d1d:10b:c446:0:36fb"; };
                                              { "sas2-0112-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c11:209:10b:c446:0:36fb"; };
                                              { "sas2-0141-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c11:c92:10b:c446:0:36fb"; };
                                              { "sas2-0612-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c11:91a:10b:c446:0:36fb"; };
                                              { "sas2-5940-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:42a3:10b:c446:0:36fb"; };
                                              { "sas2-5988-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:43a7:10b:c446:0:36fb"; };
                                              { "sas2-6065-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c08:588f:10b:c446:0:36fb"; };
                                              { "sas2-6104-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:1d24:10b:c446:0:36fb"; };
                                              { "sas2-6148-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:4385:10b:c446:0:36fb"; };
                                              { "sas2-6149-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:438b:10b:c446:0:36fb"; };
                                              { "sas2-6213-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:4286:10b:c446:0:36fb"; };
                                              { "sas2-6224-731-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:1e1c:10b:c446:0:36fb"; };
                                              { "sas2-6320-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:1d22:10b:c446:0:36fb"; };
                                              { "sas3-0267-990-sas-misc-barnav-14075.gencfg-c.yandex.net"; 14075; 48.000; "2a02:6b8:c14:4eaa:10b:c446:0:36fb"; };
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
                                      }; -- report
                                    }; -- bygeo_sas
                                    bygeo_man = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "requests_to_man";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          by_hash_policy = {
                                            timeout_policy = {
                                              timeout = "1s";
                                              unique_policy = {};
                                            }; -- timeout_policy
                                          }; -- by_hash_policy
                                          attempts = 24;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "man1-0424-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:377:10b:c444:0:5a3a"; };
                                              { "man1-1092-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:1de7:10b:c444:0:5a3a"; };
                                              { "man1-1208-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:1e6c:10b:c444:0:5a3a"; };
                                              { "man1-2145-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:2ae9:10b:c444:0:5a3a"; };
                                              { "man1-2348-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:27e9:10b:c444:0:5a3a"; };
                                              { "man1-4513-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:3af2:10b:c444:0:5a3a"; };
                                              { "man1-5620-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:5e3:10b:c444:0:5a3a"; };
                                              { "man1-7192-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c09:4ad:10b:c444:0:5a3a"; };
                                              { "man1-7310-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:185f:10b:c444:0:5a3a"; };
                                              { "man1-7324-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:1874:10b:c444:0:5a3a"; };
                                              { "man1-7543-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c09:109:10b:c444:0:5a3a"; };
                                              { "man1-8079-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:24d9:10b:c444:0:5a3a"; };
                                              { "man1-8102-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:249f:10b:c444:0:5a3a"; };
                                              { "man1-8314-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c09:301:10b:c444:0:5a3a"; };
                                              { "man1-9376-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c0b:3f1d:10b:c444:0:5a3a"; };
                                              { "man2-1227-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:121a:10b:c444:0:5a3a"; };
                                              { "man2-1232-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:1184:10b:c444:0:5a3a"; };
                                              { "man2-1235-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:129e:10b:c444:0:5a3a"; };
                                              { "man2-1243-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:140b:10b:c444:0:5a3a"; };
                                              { "man2-1265-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:1292:10b:c444:0:5a3a"; };
                                              { "man2-1287-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:129a:10b:c444:0:5a3a"; };
                                              { "man2-1600-51b-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:1497:10b:c444:0:5a3a"; };
                                              { "man2-1603-32d-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:1509:10b:c444:0:5a3a"; };
                                              { "man2-1613-4ff-man-misc-barnav-23098.gencfg-c.yandex.net"; 23098; 64.000; "2a02:6b8:c13:149d:10b:c444:0:5a3a"; };
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
                                      }; -- report
                                    }; -- bygeo_man
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 502;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- icookie
                            }; -- headers
                          }; -- hasher
                        }; -- module
                      }; -- pinger
                    }; -- default
                  }; -- regexp
                }; -- shared
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_9200 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        9200;
      }; -- ports
      shared = {
        uuid = "1926059288835181130";
      }; -- shared
    }; -- http_section_9200
  }; -- ipdispatch
}