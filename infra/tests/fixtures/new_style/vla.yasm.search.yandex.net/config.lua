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
  maxconn = 5000;
  buffer = 3145728;
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
  log = get_log_path("childs_log", 15260, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 15260;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15260;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 15260;
      ip = "127.0.0.4";
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400::2:12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "141.8.146.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15261;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15261;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::2:12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "141.8.146.12";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15260;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15260;
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
        15260;
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
        15260;
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
        "2a02:6b8:0:3400::2:12";
        "141.8.146.12";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "4866788410971058282";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15261, "/place/db/www/logs");
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            contexts = {
              ["s.yasm.yandex-team.ru"] = {
                priority = 2;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 15261, "/place/db/www/logs");
                priv = get_private_cert_path("s.yasm.yandex-team.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-s.yasm.yandex-team.ru.pem", "/dev/shm/balancer");
                servername = {
                  surround = false;
                  case_insensitive = false;
                  servername_regexp = "s\\..*";
                }; -- servername
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.s.yasm.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.s.yasm.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.s.yasm.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                }; -- ticket_keys_list
              }; -- ["s.yasm.yandex-team.ru"]
              default = {
                priority = 1;
                timeout = "100800s";
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 15261, "/place/db/www/logs");
                priv = get_private_cert_path("yasm.yandex-team.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-yasm.yandex-team.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.yasm.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.yasm.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.yasm.yandex-team.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 15261, "/place/db/www/logs");
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
      }; -- shared
    }; -- https_section_443
    https_section_15261 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15261;
      }; -- ports
      shared = {
        uuid = "4866788410971058282";
      }; -- shared
    }; -- https_section_15261
    http_section_80 = {
      ips = {
        "2a02:6b8:0:3400::2:12";
        "141.8.146.12";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "2567496903302958318";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15260, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 15260, "/place/db/www/logs");
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
                  headers = {
                    create_func = {
                      ["X-Source-Port-Y"] = "realport";
                      ["X-Start-Time"] = "starttime";
                    }; -- create_func
                    create_func_weak = {
                      ["X-Req-Id"] = "reqid";
                    }; -- create_func_weak
                    hasher = {
                      mode = "subnet";
                      subnet_v4_mask = 32;
                      subnet_v6_mask = 128;
                      threshold = {
                        lo_bytes = 4096;
                        hi_bytes = 8192;
                        recv_timeout = "1s";
                        pass_timeout = "9s";
                        regexp = {
                          ["awacs-balancer-health-check"] = {
                            priority = 20;
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
                            priority = 19;
                            match_fsm = {
                              url = "/ping";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
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
                          }; -- slbping
                          ["yasm-redirect"] = {
                            priority = 18;
                            match_fsm = {
                              host = "(www\\.)?golovan\\.yandex-team\\.ru";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
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
                                    rewrite = "https://yasm.yandex-team.ru%{url}";
                                  };
                                }; -- actions
                                errordocument = {
                                  status = 301;
                                  force_conn_close = false;
                                  remain_headers = "Location";
                                }; -- errordocument
                              }; -- rewrite
                            }; -- headers
                          }; -- ["yasm-redirect"]
                          front = {
                            priority = 17;
                            match_fsm = {
                              URI = "/(srv|stat|sigstat|srvmap)/.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "front";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  hashing = {
                                    unpack(gen_proxy_backends({
                                      { "man1-3935.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                      { "man1-6783.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                      { "man1-6814.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                      { "sas1-0925.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                      { "sas1-1063.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                      { "sas1-1091.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                      { "vla1-0730.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                      { "vla1-1469.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                      { "vla1-2878.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "70s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 10;
                                      need_resolve = true;
                                    }))
                                  }; -- hashing
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- front
                          rtfront = {
                            priority = 16;
                            match_fsm = {
                              URI = "/rt/.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "rtfront";
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
                                    rewrite = "/sigstat";
                                    regexp = "/rt/sigstat";
                                    case_insensitive = false;
                                  };
                                  {
                                    global = false;
                                    literal = false;
                                    rewrite = "/subs";
                                    regexp = "/rt/subs";
                                    case_insensitive = false;
                                  };
                                }; -- actions
                                h100 = {
                                  stats_eater = {
                                    regexp = {
                                      headers_hash = {
                                        priority = 2;
                                        match_fsm = {
                                          header = {
                                            name = "X-Golovan-Rt-Request";
                                            value = ".*";
                                          }; -- header
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        headers_hasher = {
                                          header_name = "X-Golovan-Rt-Request";
                                          surround = false;
                                          randomize_empty_match = true;
                                          shared = {
                                            uuid = "rtfront_backends";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 3;
                                              rendezvous_hashing = {
                                                weights_file = "./controls/traffic_control.weights";
                                                reload_duration = "1s";
                                                rtfront_man = {
                                                  weight = 1.000;
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 1;
                                                    hashing = {
                                                      unpack(gen_proxy_backends({
                                                        { "man1-0593.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:6031:92e2:baff:fe74:796e"; };
                                                        { "man1-0845.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:6010:92e2:baff:fe56:e942"; };
                                                        { "man1-3389.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:e3d0"; };
                                                        { "man1-7588.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:606c:e61d:2dff:fe6c:d3b0"; };
                                                        { "man1-7626.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:606c:e61d:2dff:fe6d:ca30"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "8s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- hashing
                                                  }; -- balancer2
                                                }; -- rtfront_man
                                                rtfront_sas = {
                                                  weight = 1.000;
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 1;
                                                    hashing = {
                                                      unpack(gen_proxy_backends({
                                                        { "sas1-1791.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:17e:96de:80ff:fe8e:7bd1"; };
                                                        { "sas1-2896.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:10d:225:90ff:fe83:198e"; };
                                                        { "sas1-2989.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:106:225:90ff:fe83:1ef2"; };
                                                        { "sas1-3087.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:10e:225:90ff:fe88:b1da"; };
                                                        { "sas1-4143.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3380"; };
                                                        { "yasmsrv-sas05.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:602:225:90ff:fec1:d05a"; };
                                                        { "yasmsrv-sas06.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:65e:225:90ff:fec1:d2a2"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "8s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- hashing
                                                  }; -- balancer2
                                                }; -- rtfront_sas
                                                rtfront_vla = {
                                                  weight = 1.000;
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 1;
                                                    hashing = {
                                                      unpack(gen_proxy_backends({
                                                        { "vla1-0089.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                                        { "vla1-0169.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                                        { "vla1-0593.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:44:0:604:db7:9f5b"; };
                                                        { "vla1-1753.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:88:0:604:db7:a916"; };
                                                        { "vla1-1974.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:12:0:604:db7:9a0f"; };
                                                        { "vla1-4322.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:99:0:604:db7:aa98"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "8s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- hashing
                                                  }; -- balancer2
                                                }; -- rtfront_vla
                                              }; -- rendezvous_hashing
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- headers_hasher
                                      }; -- headers_hash
                                      default = {
                                        priority = 1;
                                        shared = {
                                          uuid = "rtfront_backends";
                                        }; -- shared
                                      }; -- default
                                    }; -- regexp
                                  }; -- stats_eater
                                }; -- h100
                              }; -- rewrite
                            }; -- report
                          }; -- rtfront
                          histfront = {
                            priority = 15;
                            match_fsm = {
                              URI = "/hist/?.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "histfront";
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
                                    rewrite = "/";
                                    global = false;
                                    literal = false;
                                    regexp = "/hist/";
                                    case_insensitive = false;
                                  };
                                }; -- actions
                                h100 = {
                                  stats_eater = {
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      rr = {
                                        weights_file = "./controls/traffic_control.weights";
                                        histfront_man = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 3;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "man1-3935.search.yandex.net"; 14010; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                                { "man1-3935.search.yandex.net"; 14011; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                                { "man1-3935.search.yandex.net"; 14012; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                                { "man1-3935.search.yandex.net"; 14013; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                                { "man1-6783.search.yandex.net"; 14010; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                                { "man1-6783.search.yandex.net"; 14011; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                                { "man1-6783.search.yandex.net"; 14012; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                                { "man1-6783.search.yandex.net"; 14013; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                                { "man1-6814.search.yandex.net"; 14010; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                                { "man1-6814.search.yandex.net"; 14011; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                                { "man1-6814.search.yandex.net"; 14012; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                                { "man1-6814.search.yandex.net"; 14013; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "60s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- histfront_man
                                        histfront_sas = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 3;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "sas1-0925.search.yandex.net"; 14010; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                                { "sas1-0925.search.yandex.net"; 14011; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                                { "sas1-0925.search.yandex.net"; 14012; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                                { "sas1-0925.search.yandex.net"; 14013; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                                { "sas1-1063.search.yandex.net"; 14010; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                                { "sas1-1063.search.yandex.net"; 14011; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                                { "sas1-1063.search.yandex.net"; 14012; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                                { "sas1-1063.search.yandex.net"; 14013; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                                { "sas1-1091.search.yandex.net"; 14010; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                                { "sas1-1091.search.yandex.net"; 14011; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                                { "sas1-1091.search.yandex.net"; 14012; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                                { "sas1-1091.search.yandex.net"; 14013; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "60s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- histfront_sas
                                        histfront_vla = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 3;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "vla1-0730.search.yandex.net"; 14010; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                                { "vla1-0730.search.yandex.net"; 14011; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                                { "vla1-0730.search.yandex.net"; 14012; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                                { "vla1-0730.search.yandex.net"; 14013; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                                { "vla1-1469.search.yandex.net"; 14010; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                                { "vla1-1469.search.yandex.net"; 14011; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                                { "vla1-1469.search.yandex.net"; 14012; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                                { "vla1-1469.search.yandex.net"; 14013; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                                { "vla1-2878.search.yandex.net"; 14010; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                                { "vla1-2878.search.yandex.net"; 14011; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                                { "vla1-2878.search.yandex.net"; 14012; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                                { "vla1-2878.search.yandex.net"; 14013; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "60s";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- histfront_vla
                                      }; -- rr
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- h100
                              }; -- rewrite
                            }; -- report
                          }; -- histfront
                          push = {
                            priority = 14;
                            match_fsm = {
                              URI = "/push/?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "push";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
                                regexp = {
                                  headers_hash = {
                                    priority = 2;
                                    match_fsm = {
                                      header = {
                                        name = "X-Golovan-Push-Request";
                                        value = ".*";
                                      }; -- header
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                    headers_hasher = {
                                      header_name = "X-Golovan-Push-Request";
                                      surround = false;
                                      randomize_empty_match = true;
                                      shared = {
                                        uuid = "push_backends";
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          rendezvous_hashing = {
                                            weights_file = "./controls/traffic_control.weights";
                                            reload_duration = "1s";
                                            push_man = {
                                              weight = 1.000;
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 1;
                                                hashing = {
                                                  unpack(gen_proxy_backends({
                                                    { "22fc6z34f27kgee7.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:3497:100:0:5c56:0"; };
                                                    { "6aduqfulwo2jnqy7.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:3c66:100:0:67e0:0"; };
                                                    { "ag4zx3ibocpmvgso.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:6e14:100:0:6b35:0"; };
                                                    { "pfxi3kfqtxu22n7j.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:f0f:100:0:7a43:0"; };
                                                    { "zq752s2nmpp6s465.man.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:2875:100:0:19ff:0"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "100ms";
                                                    backend_timeout = "5s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = true;
                                                  }))
                                                }; -- hashing
                                              }; -- balancer2
                                            }; -- push_man
                                            push_sas = {
                                              weight = 1.000;
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 1;
                                                hashing = {
                                                  unpack(gen_proxy_backends({
                                                    { "7bmorqmi6gr3h7d5.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:8b12:100:0:ef5a:0"; };
                                                    { "afzqfisdyolt23xe.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:8907:100:0:c46f:0"; };
                                                    { "bdquw2e7yedfn6pw.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:6195:100:0:f36f:0"; };
                                                    { "pplmyk5ld35axmir.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:8014:100:0:4bb2:0"; };
                                                    { "v7ol6meahafqbhfc.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:6897:100:0:ad6:0"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "100ms";
                                                    backend_timeout = "5s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = true;
                                                  }))
                                                }; -- hashing
                                              }; -- balancer2
                                            }; -- push_sas
                                            push_vla = {
                                              weight = 1.000;
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 1;
                                                hashing = {
                                                  unpack(gen_proxy_backends({
                                                    { "acsgubaxfu6hfkdj.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4e8e:100:0:882a:0"; };
                                                    { "gzam2tt5jr7yrfya.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4d8b:100:0:e451:0"; };
                                                    { "ja6bgzqb376lr5ut.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4d19:100:0:78dd:0"; };
                                                    { "nh4a5w3la7qfieee.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4a10:100:0:903b:0"; };
                                                    { "olpfgieeynhluio4.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:5093:100:0:8a87:0"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "100ms";
                                                    backend_timeout = "5s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = true;
                                                  }))
                                                }; -- hashing
                                              }; -- balancer2
                                            }; -- push_vla
                                          }; -- rendezvous_hashing
                                        }; -- balancer2
                                      }; -- shared
                                    }; -- headers_hasher
                                  }; -- headers_hash
                                  default = {
                                    priority = 1;
                                    shared = {
                                      uuid = "push_backends";
                                    }; -- shared
                                  }; -- default
                                }; -- regexp
                              }; -- stats_eater
                            }; -- report
                          }; -- push
                          snapchart = {
                            priority = 13;
                            match_fsm = {
                              host = "s.yasm.yandex-team.ru";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "snapchart";
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
                                attempts = 3;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "3ajk2kr2yb4kjtau.man.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0b:200c:100:0:aa2e:0"; };
                                    { "75qgbtlfxfj7yfqu.sas.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c08:8b18:100:0:2efe:0"; };
                                    { "egmhm7ckkuw54vle.man.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0b:1b10:100:0:d4ea:0"; };
                                    { "eoq6yw7i74vpptoi.vla.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0d:4e95:100:0:2be0:0"; };
                                    { "i77jtsviz4xw6cdq.sas.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c08:8413:100:0:a632:0"; };
                                    { "ioohepb2mtciwwhs.man.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0b:2a7f:100:0:5363:0"; };
                                    { "jjnci74m3joputxs.sas.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c14:2a95:100:0:c8e4:0"; };
                                    { "kcnpx3jzgzwjdsta.sas.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c08:8421:100:0:6f46:0"; };
                                    { "kq2epjg2oku5jwnv.vla.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0d:4a15:100:0:912:0"; };
                                    { "maaf3443w6w5pjq7.man.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0b:92:100:0:352f:0"; };
                                    { "nuy3jtln4vhvanrt.vla.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0d:4d19:100:0:3d19:0"; };
                                    { "qzyjuq2hgnpocypp.vla.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0d:4d8c:100:0:54e8:0"; };
                                    { "uqyirgh7bl4snilx.man.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0b:300:100:0:2e0:0"; };
                                    { "vvs65uivrqxpk7l7.vla.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c0d:4e8e:100:0:c01c:0"; };
                                    { "wcluomogakk6zffr.sas.yp-c.yandex.net"; 8000; 1.000; "2a02:6b8:c08:40aa:100:0:6706:0"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "70ms";
                                    backend_timeout = "60s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- snapchart
                          alertserver = {
                            priority = 12;
                            match_fsm = {
                              URI = "(/(badwarn|reqpool|meta-alert)/.*|/conf/alerts2/.*)";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "alertserver";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
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
                                      { "man2-1615.search.yandex.net"; 8888; 1.000; "2a02:6b8:c01:82a:0:604:5e97:dfd1"; };
                                      { "man2-1616.search.yandex.net"; 8888; 1.000; "2a02:6b8:c01:829:0:604:5e97:dfac"; };
                                      { "vla1-2918.search.yandex.net"; 8888; 1.000; "2a02:6b8:c0e:32:0:604:db7:9c05"; };
                                      { "yasmfront-sas2.search.yandex.net"; 8888; 1.000; "2a02:6b8:b000:643:96de:80ff:fe81:155e"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "3s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 10;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- alertserver
                          collector = {
                            priority = 11;
                            match_fsm = {
                              URI = "/(h?conf|conf2|h?signals|cstate|functions|hivemind|dc|hist_hosts|hist_tags|hosts|tags|host_info|group_info)/.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "collector";
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
                                    rewrite = "/conf/";
                                    regexp = "/conf2/?";
                                    case_insensitive = false;
                                  };
                                  {
                                    global = false;
                                    literal = false;
                                    rewrite = "/dc/";
                                    regexp = "/hivemind/?";
                                    case_insensitive = false;
                                  };
                                }; -- actions
                                shared = {
                                  uuid = "4009194256474988636";
                                  stats_eater = {
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
                                          { "man1-8058.search.yandex.net"; 12009; 1.000; "2a02:6b8:b000:6508:215:b2ff:fea9:683a"; };
                                          { "sas1-5905.search.yandex.net"; 12009; 1.000; "2a02:6b8:b000:635:225:90ff:feec:2f5e"; };
                                          { "vla1-0789.search.yandex.net"; 12009; 2.000; "2a02:6b8:c0e:13:0:604:db7:9b11"; };
                                          { "vla1-3616.search.yandex.net"; 12009; 2.000; "2a02:6b8:c0e:a0:0:604:db7:a502"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "6s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 10;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- shared
                              }; -- rewrite
                            }; -- report
                          }; -- collector
                          ["collector-metainfo"] = {
                            priority = 10;
                            match_fsm = {
                              URI = "/metainfo/.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "collector_metainfo";
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
                                    rewrite = "/";
                                    global = false;
                                    literal = false;
                                    regexp = "/metainfo/?";
                                    case_insensitive = false;
                                  };
                                  {
                                    global = false;
                                    literal = false;
                                    rewrite = "/conf/";
                                    case_insensitive = false;
                                    regexp = "/metainfo/conf2/?";
                                  };
                                  {
                                    global = false;
                                    literal = false;
                                    rewrite = "/dc/";
                                    case_insensitive = false;
                                    regexp = "/metainfo/hivemind/?";
                                  };
                                }; -- actions
                                shared = {
                                  uuid = "4009194256474988636";
                                }; -- shared
                              }; -- rewrite
                            }; -- report
                          }; -- ["collector-metainfo"]
                          ["collector-staffonly"] = {
                            priority = 9;
                            match_fsm = {
                              URI = "/staff_only/.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "collector_staffonly";
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
                                    rewrite = "/";
                                    global = false;
                                    literal = false;
                                    case_insensitive = false;
                                    regexp = "/staff_only/?";
                                  };
                                  {
                                    global = false;
                                    literal = false;
                                    rewrite = "/conf/";
                                    case_insensitive = false;
                                    regexp = "/staff_only/conf2/?";
                                  };
                                  {
                                    global = false;
                                    literal = false;
                                    rewrite = "/dc/";
                                    case_insensitive = false;
                                    regexp = "/staff_only/hivemind/?";
                                  };
                                }; -- actions
                                stats_eater = {
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
                                        { "man1-8058.search.yandex.net"; 13003; 1.000; "2a02:6b8:b000:6508:215:b2ff:fea9:683a"; };
                                        { "sas1-5905.search.yandex.net"; 13003; 1.000; "2a02:6b8:b000:635:225:90ff:feec:2f5e"; };
                                        { "vla1-0789.search.yandex.net"; 13003; 1.000; "2a02:6b8:c0e:13:0:604:db7:9b11"; };
                                        { "vla1-3616.search.yandex.net"; 13003; 1.000; "2a02:6b8:c0e:a0:0:604:db7:a502"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "60s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 10;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                  }; -- balancer2
                                }; -- stats_eater
                              }; -- rewrite
                            }; -- report
                          }; -- ["collector-staffonly"]
                          snapshot = {
                            priority = 8;
                            match_fsm = {
                              URI = "/snapshot/?.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "snapshot";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
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
                                      { "man1-8369.search.yandex.net"; 16002; 1.000; "2a02:6b8:b000:6008:92e2:baff:fe55:f5e6"; };
                                      { "sas1-7504.search.yandex.net"; 16002; 1.000; "2a02:6b8:b000:693:922b:34ff:fecf:2dc6"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "3s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 10;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- snapshot
                          img = {
                            priority = 7;
                            match_fsm = {
                              URI = "/img/?.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "img";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
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
                                      { "man1-8369.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6008:92e2:baff:fe55:f5e6"; };
                                      { "sas1-7504.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:693:922b:34ff:fecf:2dc6"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "2s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 10;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- img
                          ambry_getall = {
                            priority = 6;
                            match_fsm = {
                              URI = "/srvambry/get_all/?.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "ambry_getall";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create = {
                                  Host = "ambry.golovan.yandex-team.ru";
                                }; -- create
                                rewrite = {
                                  actions = {
                                    {
                                      rewrite = "/";
                                      global = false;
                                      literal = false;
                                      regexp = "/srvambry/";
                                      case_insensitive = false;
                                    };
                                  }; -- actions
                                  stats_eater = {
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
                                          { "man1-3935.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-6783.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6814.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "sas1-0925.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-1063.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1091.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "vla1-0730.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-1469.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-2878.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "35s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- rewrite
                              }; -- headers
                            }; -- report
                          }; -- ambry_getall
                          ambry_alert_template_apply = {
                            priority = 5;
                            match_or = {
                              {
                                match_fsm = {
                                  URI = "/srvambry/tmpl/alerts/apply/?.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              };
                              {
                                match_fsm = {
                                  URI = "/srvambry/alerts/replace";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              };
                            }; -- match_or
                            report = {
                              uuid = "ambry_alert_template_apply";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create = {
                                  Host = "ambry.golovan.yandex-team.ru";
                                }; -- create
                                rewrite = {
                                  actions = {
                                    {
                                      rewrite = "/";
                                      global = false;
                                      literal = false;
                                      regexp = "/srvambry/";
                                      case_insensitive = false;
                                    };
                                  }; -- actions
                                  stats_eater = {
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
                                          { "man1-3935.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-6783.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6814.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "sas1-0925.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-1063.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1091.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "vla1-0730.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-1469.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-2878.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "300s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- rewrite
                              }; -- headers
                            }; -- report
                          }; -- ambry_alert_template_apply
                          ambry = {
                            priority = 4;
                            match_fsm = {
                              URI = "/srvambry/?.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "ambry";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              headers = {
                                create = {
                                  Host = "ambry.golovan.yandex-team.ru";
                                }; -- create
                                rewrite = {
                                  actions = {
                                    {
                                      rewrite = "/";
                                      global = false;
                                      literal = false;
                                      regexp = "/srvambry/";
                                      case_insensitive = false;
                                    };
                                  }; -- actions
                                  stats_eater = {
                                    balancer2 = {
                                      unique_policy = {};
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
                                          { "man1-3935.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-3935.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                          { "man1-6783.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6783.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                          { "man1-6814.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "man1-6814.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                          { "sas1-0925.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-0925.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                          { "sas1-1063.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1063.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                          { "sas1-1091.search.yandex.net"; 13005; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13006; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13007; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "sas1-1091.search.yandex.net"; 13008; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                          { "vla1-0730.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-0730.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                          { "vla1-1469.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-1469.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                          { "vla1-2878.search.yandex.net"; 13005; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13006; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13007; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                          { "vla1-2878.search.yandex.net"; 13008; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "15s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- rewrite
                              }; -- headers
                            }; -- report
                          }; -- ambry
                          newarc = {
                            priority = 3;
                            match_fsm = {
                              URI = "/arc/.*/";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "oldgui";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
                                balancer2 = {
                                  unique_policy = {};
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
                                      { "man1-3935.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                      { "man1-6783.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                      { "man1-6814.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                      { "sas1-0925.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                      { "sas1-1063.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                      { "sas1-1091.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                      { "vla1-0730.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                      { "vla1-1469.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                      { "vla1-2878.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "30s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 10;
                                      need_resolve = true;
                                    }))
                                  }; -- weighted2
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- newarc
                          time = {
                            priority = 2;
                            match_fsm = {
                              URI = "/time/?.*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    time_man = {
                                      weight = 1.000;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "man1-0593.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:6031:92e2:baff:fe74:796e"; };
                                            { "man1-0845.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:6010:92e2:baff:fe56:e942"; };
                                            { "man1-3389.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:e3d0"; };
                                            { "man1-7588.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:606c:e61d:2dff:fe6c:d3b0"; };
                                            { "man1-7626.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:606c:e61d:2dff:fe6d:ca30"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "7s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- time_man
                                    time_sas = {
                                      weight = 1.000;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "sas1-1791.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:17e:96de:80ff:fe8e:7bd1"; };
                                            { "sas1-2896.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:10d:225:90ff:fe83:198e"; };
                                            { "sas1-2989.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:106:225:90ff:fe83:1ef2"; };
                                            { "sas1-3087.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:10e:225:90ff:fe88:b1da"; };
                                            { "sas1-4143.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3380"; };
                                            { "yasmsrv-sas05.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:602:225:90ff:fec1:d05a"; };
                                            { "yasmsrv-sas06.search.yandex.net"; 13905; 1.000; "2a02:6b8:b000:65e:225:90ff:fec1:d2a2"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "7s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- time_sas
                                    time_vla = {
                                      weight = 1.000;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "vla1-0089.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:2d:0:604:db7:9d36"; };
                                            { "vla1-0169.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:13:0:604:db7:9a7c"; };
                                            { "vla1-0593.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:44:0:604:db7:9f5b"; };
                                            { "vla1-1753.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:88:0:604:db7:a916"; };
                                            { "vla1-1974.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:12:0:604:db7:9a0f"; };
                                            { "vla1-4322.search.yandex.net"; 13905; 1.000; "2a02:6b8:c0e:99:0:604:db7:aa98"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "7s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 10;
                                            need_resolve = true;
                                          }))
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- time_vla
                                  }; -- rr
                                }; -- balancer2
                              }; -- stats_eater
                              refers = "rtfront";
                            }; -- report
                          }; -- time
                          default = {
                            priority = 1;
                            report = {
                              uuid = "default";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              stats_eater = {
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  hashing = {
                                    unpack(gen_proxy_backends({
                                      { "man1-3935.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77c0"; };
                                      { "man1-6783.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6053:f652:14ff:fe8c:16c0"; };
                                      { "man1-6814.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:6061:e61d:2dff:fe04:4420"; };
                                      { "sas1-0925.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:152:225:90ff:fe4f:f6c8"; };
                                      { "sas1-1063.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3372"; };
                                      { "sas1-1091.search.yandex.net"; 8080; 1.000; "2a02:6b8:b000:654:96de:80ff:fe81:e48"; };
                                      { "vla1-0730.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:3c:0:604:db7:9eda"; };
                                      { "vla1-1469.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:53:0:604:db7:9cfc"; };
                                      { "vla1-2878.search.yandex.net"; 8080; 1.000; "2a02:6b8:c0e:32:0:604:db7:9f0b"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "10s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 10;
                                      need_resolve = true;
                                    }))
                                  }; -- hashing
                                }; -- balancer2
                              }; -- stats_eater
                            }; -- report
                          }; -- default
                        }; -- regexp
                      }; -- threshold
                    }; -- hasher
                  }; -- headers
                }; -- shared
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_15260 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15260;
      }; -- ports
      shared = {
        uuid = "2567496903302958318";
      }; -- shared
    }; -- http_section_15260
  }; -- ipdispatch
}