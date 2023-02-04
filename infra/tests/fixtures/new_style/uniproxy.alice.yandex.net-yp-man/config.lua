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
  maxconn = 45000;
  tcp_fastopen = 0;
  pinger_required = true;
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
    client_name = "awacs-l7-balancer(uniproxy.alice.yandex.net:uniproxy.alice.yandex.net-yp-man)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
  }; -- sd
  addrs = {
    {
      port = 80;
      ip = "2a02:6b8:0:3400:0:71d:0:11f";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400:0:71d:0:11f";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.204.80";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.193.76";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "213.180.193.78";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "2a02:6b8::3c";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "5.45.202.150";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "5.45.202.151";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "5.45.202.152";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "5.45.202.153";
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
    internal_http_80_remote = {
      ips = {
        "2a02:6b8:0:3400:0:71d:0:11f";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "2880840847175425340";
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
                headers = {
                  create_func_weak = {
                    ["X-Real-Ip"] = "realip";
                    ["X-Real-Port"] = "realport";
                  }; -- create_func_weak
                  shared = {
                    uuid = "3352119383381213328";
                    regexp = {
                      ["awacs-balancer-health-check"] = {
                        priority = 9;
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
                        priority = 8;
                        match_fsm = {
                          url = "/ping";
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
                      ["uniproxy-kpi"] = {
                        priority = 7;
                        match_and = {
                          {
                            match_fsm = {
                              host = "uniproxy-kpi\\.alice\\.yandex\\.net";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                        }; -- match_and
                        rewrite = {
                          actions = {
                            {
                              global = false;
                              literal = false;
                              rewrite = "/uni.ws%1";
                              case_insensitive = false;
                              regexp = "/h?uni.ws(.*)";
                            };
                          }; -- actions
                          report = {
                            uuid = "uniproxy_int_ws_kpi";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            headers = {
                              create_func = {
                                ["X-Yandex-TCP-Info"] = "tcp_info";
                              }; -- create_func
                              log_headers = {
                                name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    uniproxy_man = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "uniproxy_int_ws_kpi_to_man";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          fast_attempts = "count_backends";
                                          fast_503 = true;
                                          sd = {
                                            endpoint_sets = {
                                              {
                                                cluster_name = "man";
                                                endpoint_set_id = "wsproxy-man";
                                              };
                                            }; -- endpoint_sets
                                            proxy_options = {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "75ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = false;
                                              backend_read_timeout = "600s";
                                              client_read_timeout = "600s";
                                              allow_connection_upgrade = true;
                                              backend_write_timeout = "600s";
                                              client_write_timeout = "600s";
                                            }; -- proxy_options
                                            active = {
                                              delay = "10s";
                                              request = "GET /ping HTTP/1.1\nHost: uniproxy-kpi.alice.yandex.net\n\n";
                                            }; -- active
                                          }; -- sd
                                          attempts_rate_limiter = {
                                            limit = 2.000;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- uniproxy_man
                                    uniproxy_sas = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "uniproxy_int_ws_kpi_to_sas";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          fast_attempts = "count_backends";
                                          fast_503 = true;
                                          sd = {
                                            endpoint_sets = {
                                              {
                                                cluster_name = "sas";
                                                endpoint_set_id = "wsproxy-sas";
                                              };
                                            }; -- endpoint_sets
                                            proxy_options = {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "75ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = false;
                                              backend_read_timeout = "600s";
                                              client_read_timeout = "600s";
                                              allow_connection_upgrade = true;
                                              backend_write_timeout = "600s";
                                              client_write_timeout = "600s";
                                            }; -- proxy_options
                                            active = {
                                              delay = "10s";
                                              request = "GET /ping HTTP/1.1\nHost: uniproxy-kpi.alice.yandex.net\n\n";
                                            }; -- active
                                          }; -- sd
                                          attempts_rate_limiter = {
                                            limit = 2.000;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- uniproxy_sas
                                    uniproxy_vla = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "uniproxy_int_ws_kpi_to_vla";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          fast_attempts = "count_backends";
                                          fast_503 = true;
                                          sd = {
                                            endpoint_sets = {
                                              {
                                                cluster_name = "vla";
                                                endpoint_set_id = "wsproxy-vla";
                                              };
                                            }; -- endpoint_sets
                                            proxy_options = {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "75ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = false;
                                              backend_read_timeout = "600s";
                                              client_read_timeout = "600s";
                                              allow_connection_upgrade = true;
                                              backend_write_timeout = "600s";
                                              client_write_timeout = "600s";
                                            }; -- proxy_options
                                            active = {
                                              delay = "10s";
                                              request = "GET /ping HTTP/1.1\nHost: uniproxy-kpi.alice.yandex.net\n\n";
                                            }; -- active
                                          }; -- sd
                                          attempts_rate_limiter = {
                                            limit = 2.000;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- uniproxy_vla
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- log_headers
                            }; -- headers
                          }; -- report
                        }; -- rewrite
                      }; -- ["uniproxy-kpi"]
                      ["uniproxy-int-demo-redirect"] = {
                        priority = 6;
                        match_and = {
                          {
                            match_fsm = {
                              host = "uniproxy-internal\\.alice\\.yandex\\.net";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                          {
                            match_or = {
                              {
                                match_fsm = {
                                  path = "/unidemo.html";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              };
                              {
                                match_fsm = {
                                  path = "/ttsdemo.html";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              };
                            }; -- match_or
                          };
                        }; -- match_and
                        report = {
                          uuid = "uniproxy_int_demo_redir";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          headers = {
                            copy = {
                              Host = "Location";
                            }; -- copy
                            rewrite = {
                              actions = {
                                {
                                  regexp = ".*";
                                  global = false;
                                  literal = false;
                                  case_insensitive = false;
                                  header_name = "Location";
                                  rewrite = "%{scheme}://uniproxy.alice.yandex-team.ru%{url}";
                                };
                              }; -- actions
                              errordocument = {
                                status = 302;
                                force_conn_close = false;
                                remain_headers = "Location";
                              }; -- errordocument
                            }; -- rewrite
                          }; -- headers
                        }; -- report
                      }; -- ["uniproxy-int-demo-redirect"]
                      ["uniproxy-int-demo-auth"] = {
                        priority = 5;
                        match_and = {
                          {
                            match_fsm = {
                              host = "uniproxy\\.alice\\.yandex-team\\.ru";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                          {
                            match_or = {
                              {
                                match_fsm = {
                                  path = "/unidemo.html";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              };
                              {
                                match_fsm = {
                                  path = "/ttsdemo.html";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                              };
                            }; -- match_or
                          };
                        }; -- match_and
                        report = {
                          uuid = "uniproxy_int_demo_auth";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          headers = {
                            create_func = {
                              ["X-Yandex-TCP-Info"] = "tcp_info";
                            }; -- create_func
                            log_headers = {
                              name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  uniproxy_sas = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "uniproxy_int_demo_auth_to_sas";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
                                        fast_attempts = "count_backends";
                                        fast_503 = true;
                                        sd = {
                                          endpoint_sets = {
                                            {
                                              cluster_name = "sas";
                                              endpoint_set_id = "authproxy";
                                            };
                                          }; -- endpoint_sets
                                          proxy_options = {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "75ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = false;
                                            backend_read_timeout = "600s";
                                            client_read_timeout = "600s";
                                            allow_connection_upgrade = true;
                                            backend_write_timeout = "600s";
                                            client_write_timeout = "600s";
                                          }; -- proxy_options
                                          active = {
                                            delay = "10s";
                                            request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex-team.ru\n\n";
                                          }; -- active
                                        }; -- sd
                                        attempts_rate_limiter = {
                                          limit = 2.000;
                                          coeff = 0.990;
                                          switch_default = true;
                                        }; -- attempts_rate_limiter
                                      }; -- balancer2
                                    }; -- report
                                  }; -- uniproxy_sas
                                  uniproxy_vla = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "uniproxy_int_demo_auth_to_vla";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
                                        fast_attempts = "count_backends";
                                        fast_503 = true;
                                        sd = {
                                          endpoint_sets = {
                                            {
                                              cluster_name = "vla";
                                              endpoint_set_id = "authproxy";
                                            };
                                          }; -- endpoint_sets
                                          proxy_options = {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "75ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = false;
                                            backend_read_timeout = "600s";
                                            client_read_timeout = "600s";
                                            allow_connection_upgrade = true;
                                            backend_write_timeout = "600s";
                                            client_write_timeout = "600s";
                                          }; -- proxy_options
                                          active = {
                                            delay = "10s";
                                            request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex-team.ru\n\n";
                                          }; -- active
                                        }; -- sd
                                        attempts_rate_limiter = {
                                          limit = 2.000;
                                          coeff = 0.990;
                                          switch_default = true;
                                        }; -- attempts_rate_limiter
                                      }; -- balancer2
                                    }; -- report
                                  }; -- uniproxy_vla
                                }; -- rr
                                on_error = {
                                  errordocument = {
                                    status = 504;
                                    force_conn_close = false;
                                    content = "Service unavailable";
                                  }; -- errordocument
                                }; -- on_error
                              }; -- balancer2
                            }; -- log_headers
                          }; -- headers
                        }; -- report
                      }; -- ["uniproxy-int-demo-auth"]
                      ["uniproxy-int-prestable"] = {
                        priority = 4;
                        match_and = {
                          {
                            match_fsm = {
                              host = "uniproxy-internal\\.alice\\.yandex\\.net";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              URI = "/prestable/h?uni.ws(.*)";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                        }; -- match_and
                        rewrite = {
                          actions = {
                            {
                              global = false;
                              literal = false;
                              rewrite = "/uni.ws%1";
                              case_insensitive = false;
                              regexp = "/prestable/h?uni.ws(.*)";
                            };
                          }; -- actions
                          report = {
                            uuid = "uniproxy_int_prestable_ws";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            headers = {
                              create_func = {
                                ["X-Yandex-TCP-Info"] = "tcp_info";
                              }; -- create_func
                              log_headers = {
                                name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    uniproxy_sas = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "uniproxy_int_prestable_ws_to_sas";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          fast_attempts = 2;
                                          fast_503 = true;
                                          sd = {
                                            endpoint_sets = {
                                              {
                                                cluster_name = "sas";
                                                endpoint_set_id = "wsproxy-prestable-sas";
                                              };
                                            }; -- endpoint_sets
                                            proxy_options = {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "75ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = false;
                                              backend_read_timeout = "600s";
                                              client_read_timeout = "600s";
                                              allow_connection_upgrade = true;
                                              backend_write_timeout = "600s";
                                              client_write_timeout = "600s";
                                            }; -- proxy_options
                                            active = {
                                              delay = "10s";
                                              request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                            }; -- active
                                          }; -- sd
                                          attempts_rate_limiter = {
                                            limit = 2.000;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- uniproxy_sas
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- log_headers
                            }; -- headers
                          }; -- report
                        }; -- rewrite
                      }; -- ["uniproxy-int-prestable"]
                      ["uniproxy-internal"] = {
                        priority = 3;
                        match_and = {
                          {
                            match_fsm = {
                              host = "uniproxy-internal\\.alice\\.yandex\\.net";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                        }; -- match_and
                        rewrite = {
                          actions = {
                            {
                              global = false;
                              literal = false;
                              rewrite = "/uni.ws%1";
                              case_insensitive = false;
                              regexp = "/h?uni.ws(.*)";
                            };
                          }; -- actions
                          report = {
                            uuid = "uniproxy_int_ws";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            headers = {
                              create_func = {
                                ["X-Yandex-TCP-Info"] = "tcp_info";
                              }; -- create_func
                              log_headers = {
                                name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 1;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    uniproxy_man = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "uniproxy_int_ws_to_man";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          fast_attempts = "count_backends";
                                          fast_503 = true;
                                          sd = {
                                            endpoint_sets = {
                                              {
                                                cluster_name = "man";
                                                endpoint_set_id = "wsproxy-man";
                                              };
                                            }; -- endpoint_sets
                                            proxy_options = {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "75ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = false;
                                              backend_read_timeout = "600s";
                                              client_read_timeout = "600s";
                                              allow_connection_upgrade = true;
                                              backend_write_timeout = "600s";
                                              client_write_timeout = "600s";
                                            }; -- proxy_options
                                            active = {
                                              delay = "10s";
                                              request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                            }; -- active
                                          }; -- sd
                                          attempts_rate_limiter = {
                                            limit = 2.000;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- uniproxy_man
                                    uniproxy_sas = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "uniproxy_int_ws_to_sas";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          fast_attempts = "count_backends";
                                          fast_503 = true;
                                          sd = {
                                            endpoint_sets = {
                                              {
                                                cluster_name = "sas";
                                                endpoint_set_id = "wsproxy-sas";
                                              };
                                            }; -- endpoint_sets
                                            proxy_options = {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "75ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = false;
                                              backend_read_timeout = "600s";
                                              client_read_timeout = "600s";
                                              allow_connection_upgrade = true;
                                              backend_write_timeout = "600s";
                                              client_write_timeout = "600s";
                                            }; -- proxy_options
                                            active = {
                                              delay = "10s";
                                              request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                            }; -- active
                                          }; -- sd
                                          attempts_rate_limiter = {
                                            limit = 2.000;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- uniproxy_sas
                                    uniproxy_vla = {
                                      weight = 1.000;
                                      report = {
                                        uuid = "uniproxy_int_ws_to_vla";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          fast_attempts = "count_backends";
                                          fast_503 = true;
                                          sd = {
                                            endpoint_sets = {
                                              {
                                                cluster_name = "vla";
                                                endpoint_set_id = "wsproxy-vla";
                                              };
                                            }; -- endpoint_sets
                                            proxy_options = {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "75ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = false;
                                              backend_read_timeout = "600s";
                                              client_read_timeout = "600s";
                                              allow_connection_upgrade = true;
                                              backend_write_timeout = "600s";
                                              client_write_timeout = "600s";
                                            }; -- proxy_options
                                            active = {
                                              delay = "10s";
                                              request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                            }; -- active
                                          }; -- sd
                                          attempts_rate_limiter = {
                                            limit = 2.000;
                                            coeff = 0.990;
                                            switch_default = true;
                                          }; -- attempts_rate_limiter
                                        }; -- balancer2
                                      }; -- report
                                    }; -- uniproxy_vla
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 504;
                                      force_conn_close = false;
                                      content = "Service unavailable";
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- log_headers
                            }; -- headers
                          }; -- report
                        }; -- rewrite
                      }; -- ["uniproxy-internal"]
                      echo = {
                        priority = 2;
                        match_or = {
                          {
                            match_fsm = {
                              path = "/echo";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                        }; -- match_or
                        report = {
                          uuid = "uniproxy_echo";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          headers = {
                            create_func = {
                              ["X-Yandex-TCP-Info"] = "tcp_info";
                            }; -- create_func
                            create_func_weak = {
                              ["X-Forwarded-For-Y"] = "realip";
                            }; -- create_func_weak
                            log_headers = {
                              name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
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
                                        shared = {
                                          uuid = "5713273462504968260";
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
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "man";
                                                          endpoint_set_id = "prod-antirobot-yp-man";
                                                        };
                                                        {
                                                          cluster_name = "man";
                                                          endpoint_set_id = "prod-antirobot-yp-prestable-man";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "30ms";
                                                        backend_timeout = "100ms";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                      }; -- proxy_options
                                                      hashing = {};
                                                    }; -- sd
                                                  }; -- balancer2
                                                }; -- antirobot_man
                                                antirobot_sas = {
                                                  weight = 1.000;
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 2;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "sas";
                                                          endpoint_set_id = "prod-antirobot-yp-sas";
                                                        };
                                                        {
                                                          cluster_name = "sas";
                                                          endpoint_set_id = "prod-antirobot-yp-prestable-sas";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "30ms";
                                                        backend_timeout = "100ms";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                      }; -- proxy_options
                                                      hashing = {};
                                                    }; -- sd
                                                  }; -- balancer2
                                                }; -- antirobot_sas
                                                antirobot_vla = {
                                                  weight = 1.000;
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 2;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "vla";
                                                          endpoint_set_id = "prod-antirobot-yp-vla";
                                                        };
                                                        {
                                                          cluster_name = "vla";
                                                          endpoint_set_id = "prod-antirobot-yp-prestable-vla";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "30ms";
                                                        backend_timeout = "100ms";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                      }; -- proxy_options
                                                      hashing = {};
                                                    }; -- sd
                                                  }; -- balancer2
                                                }; -- antirobot_vla
                                              }; -- rr
                                            }; -- balancer2
                                          }; -- report
                                        }; -- shared
                                      }; -- checker
                                      module = {
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 1;
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            uniproxy_man = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "uniproxy_echo_to_man";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                shared = {
                                                  uuid = "5972792910568131904";
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 3;
                                                    fast_attempts = "count_backends";
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "man";
                                                          endpoint_set_id = "wsproxy-man";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Service unavailable";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                }; -- shared
                                              }; -- report
                                            }; -- uniproxy_man
                                            uniproxy_sas = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "uniproxy_echo_to_sas";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                shared = {
                                                  uuid = "523841764383581270";
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 3;
                                                    fast_attempts = "count_backends";
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "sas";
                                                          endpoint_set_id = "wsproxy-sas";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Service unavailable";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                }; -- shared
                                              }; -- report
                                            }; -- uniproxy_sas
                                            uniproxy_vla = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "uniproxy_echo_to_vla";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                shared = {
                                                  uuid = "6455031095739464318";
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 3;
                                                    fast_attempts = "count_backends";
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "vla";
                                                          endpoint_set_id = "wsproxy-vla";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Service unavailable";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                }; -- shared
                                              }; -- report
                                            }; -- uniproxy_vla
                                            uniproxy_devnull = {
                                              weight = -1.000;
                                              shared = {
                                                uuid = "8539049289730420848";
                                                report = {
                                                  uuid = "uniproxy_ws_to_devnull";
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
                                              }; -- shared
                                            }; -- uniproxy_devnull
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- module
                                    }; -- antirobot
                                  }; -- cutter
                                }; -- h100
                              }; -- hasher
                            }; -- log_headers
                          }; -- headers
                        }; -- report
                      }; -- echo
                      default = {
                        priority = 1;
                        report = {
                          uuid = "default";
                          ranges = "100ms,300ms,500ms,1000ms,3s";
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          headers = {
                            create_func = {
                              ["X-Yandex-TCP-Info"] = "tcp_info";
                            }; -- create_func
                            log_headers = {
                              name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
                              balancer2 = {
                                unique_policy = {};
                                attempts = 1;
                                connection_attempts = "count_backends";
                                sd = {
                                  endpoint_sets = {
                                    {
                                      cluster_name = "man";
                                      endpoint_set_id = "wsproxy-man";
                                    };
                                  }; -- endpoint_sets
                                  proxy_options = {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "50ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = false;
                                    backend_read_timeout = "600s";
                                    client_read_timeout = "600s";
                                    allow_connection_upgrade = true;
                                    backend_write_timeout = "600s";
                                    client_write_timeout = "600s";
                                  }; -- proxy_options
                                  sd = {
                                    termination_delay = "5s";
                                    termination_deadline = "8m";
                                  }; -- sd
                                  rr = {};
                                }; -- sd
                              }; -- balancer2
                            }; -- log_headers
                          }; -- headers
                        }; -- report
                      }; -- default
                    }; -- regexp
                  }; -- shared
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- internal_http_80_remote
    internal_http_80_local = {
      ips = {
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "2880840847175425340";
      }; -- shared
    }; -- internal_http_80_local
    internal_https = {
      ips = {
        "2a02:6b8:0:3400:0:71d:0:11f";
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
          ja3_enabled = true;
          contexts = {
            default = {
              priority = 2;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("uniproxy-internal.alice.yandex.net.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-uniproxy-internal.alice.yandex.net.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.uniproxy-internal.alice.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.uniproxy-internal.alice.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.uniproxy-internal.alice.yandex.net.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- default
            ["uniproxy.alice.yandex-team.ru"] = {
              priority = 1;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("uniproxy.alice.yandex-team.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-uniproxy.alice.yandex-team.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "uniproxy\\.alice\\.yandex-team\\.ru";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.uniproxy.alice.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.uniproxy.alice.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.uniproxy.alice.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["uniproxy.alice.yandex-team.ru"]
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
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                headers = {
                  create_func = {
                    ["X-Yandex-Ja3"] = "ja3";
                    ["X-Yandex-Ja4"] = "ja4";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Real-Ip"] = "realip";
                    ["X-Real-Port"] = "realport";
                  }; -- create_func_weak
                  shared = {
                    uuid = "3352119383381213328";
                  }; -- shared
                }; -- headers
                refers = "service_total";
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- internal_https
    external_requests = {
      ips = {
        "213.180.204.80";
        "213.180.193.76";
        "213.180.193.78";
        "2a02:6b8::3c";
        "5.45.202.150";
        "5.45.202.151";
        "5.45.202.152";
        "5.45.202.153";
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
          ja3_enabled = true;
          contexts = {
            default = {
              priority = 3;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("uniproxy.alice.yandex.net.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-uniproxy.alice.yandex.net.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.uniproxy.alice.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.uniproxy.alice.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.uniproxy.alice.yandex.net.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- default
            ["voicestation.yandex.net"] = {
              priority = 2;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("voicestation.yandex.net.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-voicestation.yandex.net.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "voicestation\\.yandex\\.net";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.voicestation.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.voicestation.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.voicestation.yandex.net.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["voicestation.yandex.net"]
            ["voiceservices.yandex.net"] = {
              priority = 1;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("voiceservices.yandex.net.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-voiceservices.yandex.net.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "voiceservices\\.yandex\\.net";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.voiceservices.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.voiceservices.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.voiceservices.yandex.net.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["voiceservices.yandex.net"]
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
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                headers = {
                  create_func = {
                    ["X-Yandex-Ja3"] = "ja3";
                    ["X-Yandex-Ja4"] = "ja4";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Real-Ip"] = "realip";
                    ["X-Real-Port"] = "realport";
                  }; -- create_func_weak
                  regexp = {
                    slbping = {
                      priority = 9;
                      match_fsm = {
                        url = "/ping";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      shared = {
                        uuid = "1683180570280210434";
                      }; -- shared
                    }; -- slbping
                    ["uniproxy-static-external"] = {
                      priority = 8;
                      match_and = {
                        {
                          match_or = {
                            {
                              match_fsm = {
                                host = "uniproxy\\.alice\\.yandex-team\\.ru";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                host = "uniproxy-internal\\.alice\\.yandex\\.net";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                          }; -- match_or
                        };
                        {
                          match_or = {
                            {
                              match_fsm = {
                                path = "/settings.js";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                path = ".*/mic(0|1).png";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                path = ".*/spk(0|1).png";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                path = ".*/(analyser|demo|service_worker|web_push).js";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                path = "/highlight/";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                path = "/webspeechkit/";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                path = ".*/robots.txt";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                path = "/speakers.*";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                            };
                          }; -- match_or
                        };
                      }; -- match_and
                      report = {
                        uuid = "uniproxy_static";
                        ranges = get_str_var("default_ranges");
                        just_storage = false;
                        disable_robotness = true;
                        disable_sslness = true;
                        events = {
                          stats = "report";
                        }; -- events
                        headers = {
                          create_func = {
                            ["X-Yandex-TCP-Info"] = "tcp_info";
                          }; -- create_func
                          log_headers = {
                            name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control.weights";
                                uniproxy_man = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "uniproxy_static_to_man";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 3;
                                      fast_attempts = "count_backends";
                                      fast_503 = true;
                                      sd = {
                                        endpoint_sets = {
                                          {
                                            cluster_name = "man";
                                            endpoint_set_id = "wsproxy-man";
                                          };
                                        }; -- endpoint_sets
                                        proxy_options = {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "300ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = false;
                                        }; -- proxy_options
                                        active = {
                                          delay = "10s";
                                          request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                        }; -- active
                                      }; -- sd
                                      attempts_rate_limiter = {
                                        limit = 2.000;
                                        coeff = 0.990;
                                        switch_default = true;
                                      }; -- attempts_rate_limiter
                                      on_error = {
                                        errordocument = {
                                          status = 504;
                                          force_conn_close = false;
                                          content = "Service unavailable";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- report
                                }; -- uniproxy_man
                                uniproxy_sas = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "uniproxy_static_to_sas";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 3;
                                      fast_attempts = "count_backends";
                                      fast_503 = true;
                                      sd = {
                                        endpoint_sets = {
                                          {
                                            cluster_name = "sas";
                                            endpoint_set_id = "wsproxy-sas";
                                          };
                                        }; -- endpoint_sets
                                        proxy_options = {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "300ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = false;
                                        }; -- proxy_options
                                        active = {
                                          delay = "10s";
                                          request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                        }; -- active
                                      }; -- sd
                                      attempts_rate_limiter = {
                                        limit = 2.000;
                                        coeff = 0.990;
                                        switch_default = true;
                                      }; -- attempts_rate_limiter
                                      on_error = {
                                        errordocument = {
                                          status = 504;
                                          force_conn_close = false;
                                          content = "Service unavailable";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- report
                                }; -- uniproxy_sas
                                uniproxy_vla = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "uniproxy_static_to_vla";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 3;
                                      fast_attempts = "count_backends";
                                      fast_503 = true;
                                      sd = {
                                        endpoint_sets = {
                                          {
                                            cluster_name = "vla";
                                            endpoint_set_id = "wsproxy-vla";
                                          };
                                        }; -- endpoint_sets
                                        proxy_options = {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "300ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = false;
                                        }; -- proxy_options
                                        active = {
                                          delay = "10s";
                                          request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                        }; -- active
                                      }; -- sd
                                      attempts_rate_limiter = {
                                        limit = 2.000;
                                        coeff = 0.990;
                                        switch_default = true;
                                      }; -- attempts_rate_limiter
                                      on_error = {
                                        errordocument = {
                                          status = 504;
                                          force_conn_close = false;
                                          content = "Service unavailable";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- report
                                }; -- uniproxy_vla
                                uniproxy_devnull = {
                                  weight = -1.000;
                                  report = {
                                    uuid = "uniproxy_static_to_devnull";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    errordocument = {
                                      status = 200;
                                      content = "OK";
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- report
                                }; -- uniproxy_devnull
                              }; -- rr
                            }; -- balancer2
                          }; -- log_headers
                        }; -- headers
                      }; -- report
                    }; -- ["uniproxy-static-external"]
                    ["uniproxy-v3-external"] = {
                      priority = 7;
                      match_or = {
                        {
                          match_fsm = {
                            path = "/uni.ws";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_or
                      headers = {
                        create_func = {
                          ["X-Forwarded-For-Y"] = "realip";
                          ["X-Real-Port"] = "realport";
                          ["X-Yandex-TCP-Info"] = "tcp_info";
                        }; -- create_func
                        log_headers = {
                          name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
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
                                    shared = {
                                      uuid = "5713273462504968260";
                                    }; -- shared
                                  }; -- checker
                                  module = {
                                    exp_getter = {
                                      trusted = false;
                                      file_switch = "./controls/disable_uaas_exp_uniproxy2";
                                      uaas = {
                                        rewrite = {
                                          actions = {
                                            {
                                              regexp = ".*";
                                              global = false;
                                              split = "path";
                                              literal = false;
                                              rewrite = "/uniproxy";
                                              case_insensitive = false;
                                            };
                                          }; -- actions
                                          hdrcgi = {
                                            cgi_from_hdr = {
                                              uuid = "X-UPRX-UUID";
                                            }; -- cgi_from_hdr
                                            headers = {
                                              create = {
                                                ["User-Agent"] = "uniproxy";
                                              }; -- create
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
                                                          { "man1-0551-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:3372:10e:b563:0:43d1"; };
                                                          { "man1-3722-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:37e8:10e:b563:0:43d1"; };
                                                          { "man1-4352-a48-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:3cda:10e:b563:0:43d1"; };
                                                          { "man1-4648-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:168e:10e:b563:0:43d1"; };
                                                          { "man1-5661-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:deb:10e:b563:0:43d1"; };
                                                          { "man1-6670-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:172:10e:b563:0:43d1"; };
                                                          { "man1-7202-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c09:a16:10e:b563:0:43d1"; };
                                                          { "man1-8284-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c09:315:10e:b563:0:43d1"; };
                                                          { "man1-8314-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c09:301:10e:b563:0:43d1"; };
                                                          { "man2-0395-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:4415:10e:b563:0:43d1"; };
                                                          { "man2-0510-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:4d8c:10e:b563:0:43d1"; };
                                                          { "man2-0584-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:4105:10e:b563:0:43d1"; };
                                                          { "man2-0971-af4-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c09:22a3:10e:b563:0:43d1"; };
                                                          { "man2-1463-c9d-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c09:26a7:10e:b563:0:43d1"; };
                                                          { "man2-1680-ca9-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:2617:10e:b563:0:43d1"; };
                                                          { "man2-3519-d99-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:4b02:10e:b563:0:43d1"; };
                                                          { "man2-3535-57b-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:5989:10e:b563:0:43d1"; };
                                                          { "man2-4159-92f-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:5720:10e:b563:0:43d1"; };
                                                          { "man2-4167-a09-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0b:571a:10e:b563:0:43d1"; };
                                                          { "man2-4667-250-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:1aa7:10e:b563:0:43d1"; };
                                                          { "man2-4689-8c8-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:1b8c:10e:b563:0:43d1"; };
                                                          { "man2-4806-07c-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c13:2786:10e:b563:0:43d1"; };
                                                          { "man2-6550-5da-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0a:2704:10e:b563:0:43d1"; };
                                                          { "man2-6586-c86-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0a:2987:10e:b563:0:43d1"; };
                                                          { "man2-6943-60c-man-uaas-17361.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0a:2584:10e:b563:0:43d1"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "3ms";
                                                          backend_timeout = "4ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- weighted2
                                                      attempts_rate_limiter = {
                                                        limit = 0.500;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                  }; -- bygeo_man
                                                  bygeo_sas = {
                                                    weight = 1.000;
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
                                                          { "sas1-0322-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:1092:10e:b566:0:43f7"; };
                                                          { "sas1-0370-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:a03:10e:b566:0:43f7"; };
                                                          { "sas1-0375-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:929:10e:b566:0:43f7"; };
                                                          { "sas1-0730-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:1405:10e:b566:0:43f7"; };
                                                          { "sas1-1127-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:3515:10e:b566:0:43f7"; };
                                                          { "sas1-1693-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:4810:10e:b566:0:43f7"; };
                                                          { "sas1-1786-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:8e2f:10e:b566:0:43f7"; };
                                                          { "sas1-2165-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:37a0:10e:b566:0:43f7"; };
                                                          { "sas1-2335-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:1d93:10e:b566:0:43f7"; };
                                                          { "sas1-2491-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:162b:10e:b566:0:43f7"; };
                                                          { "sas1-2511-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:1804:10e:b566:0:43f7"; };
                                                          { "sas1-2535-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:18a8:10e:b566:0:43f7"; };
                                                          { "sas1-2607-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:1603:10e:b566:0:43f7"; };
                                                          { "sas1-2659-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:1724:10e:b566:0:43f7"; };
                                                          { "sas1-2769-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:1812:10e:b566:0:43f7"; };
                                                          { "sas1-2802-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:168d:10e:b566:0:43f7"; };
                                                          { "sas1-4343-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:3487:10e:b566:0:43f7"; };
                                                          { "sas1-4612-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:24af:10e:b566:0:43f7"; };
                                                          { "sas1-4621-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:211a:10e:b566:0:43f7"; };
                                                          { "sas1-4814-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:219f:10e:b566:0:43f7"; };
                                                          { "sas1-4898-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:218a:10e:b566:0:43f7"; };
                                                          { "sas1-4903-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:2a4:10e:b566:0:43f7"; };
                                                          { "sas1-4906-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:930e:10e:b566:0:43f7"; };
                                                          { "sas1-5003-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:4881:10e:b566:0:43f7"; };
                                                          { "sas1-5414-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:2106:10e:b566:0:43f7"; };
                                                          { "sas1-5538-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:369f:10e:b566:0:43f7"; };
                                                          { "sas1-6006-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:6a29:10e:b566:0:43f7"; };
                                                          { "sas1-7522-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:43af:10e:b566:0:43f7"; };
                                                          { "sas1-9397-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:711e:10e:b566:0:43f7"; };
                                                          { "sas1-9493-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:7115:10e:b566:0:43f7"; };
                                                          { "sas2-0148-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c11:213:10e:b566:0:43f7"; };
                                                          { "sas2-0528-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c11:e9a:10e:b566:0:43f7"; };
                                                          { "sas2-1143-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:b621:10e:b566:0:43f7"; };
                                                          { "sas2-3214-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:ed1c:10e:b566:0:43f7"; };
                                                          { "sas2-4113-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:7584:10e:b566:0:43f7"; };
                                                          { "sas2-4687-f96-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:692c:10e:b566:0:43f7"; };
                                                          { "sas2-6078-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c14:439d:10e:b566:0:43f7"; };
                                                          { "sas2-6514-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c16:31d:10e:b566:0:43f7"; };
                                                          { "sas2-8852-7e7-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c16:1d9c:10e:b566:0:43f7"; };
                                                          { "slovo012-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:72a2:10e:b566:0:43f7"; };
                                                          { "slovo045-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:6d8a:10e:b566:0:43f7"; };
                                                          { "slovo055-5be-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:6422:10e:b566:0:43f7"; };
                                                          { "slovo080-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:6b14:10e:b566:0:43f7"; };
                                                          { "slovo103-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:6a8f:10e:b566:0:43f7"; };
                                                          { "slovo126-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:6b18:10e:b566:0:43f7"; };
                                                          { "slovo143-sas-uaas-17399.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c08:6a9c:10e:b566:0:43f7"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "3ms";
                                                          backend_timeout = "4ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- weighted2
                                                      attempts_rate_limiter = {
                                                        limit = 0.500;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                  }; -- bygeo_sas
                                                  bygeo_vla = {
                                                    weight = 1.000;
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
                                                          { "vla1-0141-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:1f82:10e:b569:0:37d2"; };
                                                          { "vla1-0299-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:4c09:10e:b569:0:37d2"; };
                                                          { "vla1-0487-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:1a02:10e:b569:0:37d2"; };
                                                          { "vla1-0606-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:1391:10e:b569:0:37d2"; };
                                                          { "vla1-0660-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:918:10e:b569:0:37d2"; };
                                                          { "vla1-0724-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:1e08:10e:b569:0:37d2"; };
                                                          { "vla1-0732-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:29a2:10e:b569:0:37d2"; };
                                                          { "vla1-0969-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:218c:10e:b569:0:37d2"; };
                                                          { "vla1-1523-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:261b:10e:b569:0:37d2"; };
                                                          { "vla1-1538-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:2987:10e:b569:0:37d2"; };
                                                          { "vla1-1560-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:3492:10e:b569:0:37d2"; };
                                                          { "vla1-1600-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:379d:10e:b569:0:37d2"; };
                                                          { "vla1-1674-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:3499:10e:b569:0:37d2"; };
                                                          { "vla1-1776-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:4084:10e:b569:0:37d2"; };
                                                          { "vla1-1844-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:2b9b:10e:b569:0:37d2"; };
                                                          { "vla1-2047-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:349b:10e:b569:0:37d2"; };
                                                          { "vla1-2051-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:261a:10e:b569:0:37d2"; };
                                                          { "vla1-2083-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:2a0d:10e:b569:0:37d2"; };
                                                          { "vla1-2192-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:228e:10e:b569:0:37d2"; };
                                                          { "vla1-2439-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:4a93:10e:b569:0:37d2"; };
                                                          { "vla1-2467-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:4a01:10e:b569:0:37d2"; };
                                                          { "vla1-2474-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:4a87:10e:b569:0:37d2"; };
                                                          { "vla1-2482-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:3c9a:10e:b569:0:37d2"; };
                                                          { "vla1-2526-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:4a98:10e:b569:0:37d2"; };
                                                          { "vla1-3220-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:1912:10e:b569:0:37d2"; };
                                                          { "vla1-3454-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:c90:10e:b569:0:37d2"; };
                                                          { "vla1-3715-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:5084:10e:b569:0:37d2"; };
                                                          { "vla1-3819-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0f:1d89:10e:b569:0:37d2"; };
                                                          { "vla1-3876-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:4302:10e:b569:0:37d2"; };
                                                          { "vla1-4007-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:4817:10e:b569:0:37d2"; };
                                                          { "vla1-4362-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:439d:10e:b569:0:37d2"; };
                                                          { "vla1-4408-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:431a:10e:b569:0:37d2"; };
                                                          { "vla1-4580-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0d:3b96:10e:b569:0:37d2"; };
                                                          { "vla1-5539-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c0f:1e10:10e:b569:0:37d2"; };
                                                          { "vla2-1001-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c17:498:10e:b569:0:37d2"; };
                                                          { "vla2-1003-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c17:49a:10e:b569:0:37d2"; };
                                                          { "vla2-1008-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c17:49d:10e:b569:0:37d2"; };
                                                          { "vla2-1015-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:1ba1:10e:b569:0:37d2"; };
                                                          { "vla2-1017-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:1b83:10e:b569:0:37d2"; };
                                                          { "vla2-1019-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c17:48a:10e:b569:0:37d2"; };
                                                          { "vla2-1067-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:1ba2:10e:b569:0:37d2"; };
                                                          { "vla2-1071-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:1b9f:10e:b569:0:37d2"; };
                                                          { "vla2-5945-62c-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c18:620:10e:b569:0:37d2"; };
                                                          { "vla2-5963-9a4-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c18:612:10e:b569:0:37d2"; };
                                                          { "vla2-7970-d06-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c15:398d:10e:b569:0:37d2"; };
                                                          { "vla2-7992-190-vla-uaas-14290.gencfg-c.yandex.net"; 80; 200.000; "2a02:6b8:c18:1422:10e:b569:0:37d2"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "3ms";
                                                          backend_timeout = "4ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- weighted2
                                                      attempts_rate_limiter = {
                                                        limit = 0.500;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                  }; -- bygeo_vla
                                                }; -- rr
                                              }; -- balancer2
                                            }; -- headers
                                          }; -- hdrcgi
                                        }; -- rewrite
                                      }; -- uaas
                                      regexp = {
                                        ["uniproxy2-experiment"] = {
                                          priority = 2;
                                          match_or = {
                                            {
                                              match_fsm = {
                                                header = {
                                                  name = "X-Yandex-ExpBoxes";
                                                  value = ".*258652,.*";
                                                }; -- header
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                header = {
                                                  name = "X-UPRX-APP-ID";
                                                  value = "yabro\\.(beta|canary|dev)";
                                                }; -- header
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                header = {
                                                  name = "X-UPRX-APP-ID";
                                                  value = "com\\.yandex\\.browser\\.(alpha|beta|canary|inhouse)";
                                                }; -- header
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                header = {
                                                  name = "X-UPRX-APP-ID";
                                                  value = "ru\\.yandex\\.mobile\\.search\\.(inhouse|ipad\\.inhouse)";
                                                }; -- header
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                header = {
                                                  name = "X-UPRX-APP-ID";
                                                  value = "ru\\.yandex\\.mobile\\.inhouse";
                                                }; -- header
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                header = {
                                                  name = "X-UPRX-APP-ID";
                                                  value = "ru\\.yandex\\.searchplugin\\.beta";
                                                }; -- header
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                header = {
                                                  name = "X-UPRX-APP-ID";
                                                  value = "ru\\.yandex\\.yandexnavi\\.inhouse";
                                                }; -- header
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                            {
                                              match_fsm = {
                                                header = {
                                                  name = "X-UPRX-APP-ID";
                                                  value = "ru\\.yandex\\.mobile\\.navigator\\.inhouse";
                                                }; -- header
                                                case_insensitive = true;
                                                surround = false;
                                              }; -- match_fsm
                                            };
                                          }; -- match_or
                                          report = {
                                            uuid = "uniproxy2_ws";
                                            ranges = get_str_var("default_ranges");
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            headers = {
                                              delete = "X-Yandex-Exp.*";
                                              shared = {
                                                uuid = "uniproxy2_backends";
                                              }; -- shared
                                            }; -- headers
                                          }; -- report
                                        }; -- ["uniproxy2-experiment"]
                                        default = {
                                          priority = 1;
                                          report = {
                                            uuid = "uniproxy_ext_ws";
                                            ranges = get_str_var("default_ranges");
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            shared = {
                                              uuid = "uniproxy_backends";
                                            }; -- shared
                                          }; -- report
                                        }; -- default
                                      }; -- regexp
                                    }; -- exp_getter
                                  }; -- module
                                }; -- antirobot
                              }; -- cutter
                            }; -- h100
                          }; -- hasher
                        }; -- log_headers
                      }; -- headers
                    }; -- ["uniproxy-v3-external"]
                    ["uniproxy-ws-external"] = {
                      priority = 6;
                      match_or = {
                        {
                          match_fsm = {
                            path = "/h?uni.ws";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_or
                      rewrite = {
                        actions = {
                          {
                            global = false;
                            literal = false;
                            rewrite = "/uni.ws%1";
                            case_insensitive = false;
                            regexp = "/h?uni.ws(.*)";
                          };
                        }; -- actions
                        report = {
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          headers = {
                            create_func = {
                              ["X-Real-Port"] = "realport";
                              ["X-Yandex-TCP-Info"] = "tcp_info";
                            }; -- create_func
                            create_func_weak = {
                              ["X-Forwarded-For-Y"] = "realip";
                            }; -- create_func_weak
                            log_headers = {
                              name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
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
                                        shared = {
                                          uuid = "5713273462504968260";
                                        }; -- shared
                                      }; -- checker
                                      module = {
                                        shared = {
                                          uuid = "uniproxy_backends";
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 3;
                                            rr = {
                                              weights_file = "./controls/traffic_control.weights";
                                              uniproxy_man = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "uniproxy_ext_ws_to_man";
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
                                                    fast_attempts = 2;
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "man";
                                                          endpoint_set_id = "wsproxy-man";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- uniproxy_man
                                              uniproxy_sas = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "uniproxy_ext_ws_to_sas";
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
                                                    fast_attempts = 2;
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "sas";
                                                          endpoint_set_id = "wsproxy-sas";
                                                        };
                                                        {
                                                          cluster_name = "sas";
                                                          endpoint_set_id = "wsproxy-prestable-sas";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- uniproxy_sas
                                              uniproxy_vla = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "uniproxy_ext_ws_to_vla";
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
                                                    fast_attempts = 2;
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "vla";
                                                          endpoint_set_id = "wsproxy-vla";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- uniproxy_vla
                                              uniproxy_devnull = {
                                                weight = -1.000;
                                                shared = {
                                                  uuid = "8539049289730420848";
                                                }; -- shared
                                              }; -- uniproxy_devnull
                                            }; -- rr
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Service unavailable";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- module
                                    }; -- antirobot
                                  }; -- cutter
                                }; -- h100
                              }; -- hasher
                            }; -- log_headers
                          }; -- headers
                          refers = "uniproxy_ext_ws";
                        }; -- report
                      }; -- rewrite
                    }; -- ["uniproxy-ws-external"]
                    ["uniproxy-legacy-ws-external"] = {
                      priority = 5;
                      match_or = {
                        {
                          match_fsm = {
                            path = "/(asr|tts|log)socket.ws";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_or
                      report = {
                        uuid = "uniproxy_ws";
                        ranges = get_str_var("default_ranges");
                        just_storage = false;
                        disable_robotness = true;
                        disable_sslness = true;
                        events = {
                          stats = "report";
                        }; -- events
                        headers = {
                          create_func = {
                            ["X-Yandex-TCP-Info"] = "tcp_info";
                          }; -- create_func
                          create_func_weak = {
                            ["X-Forwarded-For-Y"] = "realip";
                          }; -- create_func_weak
                          log_headers = {
                            name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
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
                                      shared = {
                                        uuid = "5713273462504968260";
                                      }; -- shared
                                    }; -- checker
                                    module = {
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        rr = {
                                          weights_file = "./controls/traffic_control.weights";
                                          uniproxy_man = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "uniproxy_ws_to_man";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              shared = {
                                                uuid = "5972792910568131904";
                                              }; -- shared
                                            }; -- report
                                          }; -- uniproxy_man
                                          uniproxy_sas = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "uniproxy_ws_to_sas";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              shared = {
                                                uuid = "523841764383581270";
                                              }; -- shared
                                            }; -- report
                                          }; -- uniproxy_sas
                                          uniproxy_vla = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "uniproxy_ws_to_vla";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              shared = {
                                                uuid = "6455031095739464318";
                                              }; -- shared
                                            }; -- report
                                          }; -- uniproxy_vla
                                          uniproxy_devnull = {
                                            weight = -1.000;
                                            shared = {
                                              uuid = "8539049289730420848";
                                            }; -- shared
                                          }; -- uniproxy_devnull
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- module
                                  }; -- antirobot
                                }; -- cutter
                              }; -- h100
                            }; -- hasher
                          }; -- log_headers
                        }; -- headers
                      }; -- report
                    }; -- ["uniproxy-legacy-ws-external"]
                    ["uniproxy-python-asr-external"] = {
                      priority = 4;
                      match_or = {
                        {
                          match_fsm = {
                            path = "/asr(.*)";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_or
                      report = {
                        uuid = "uniproxy_py_ext_asr";
                        ranges = get_str_var("default_ranges");
                        just_storage = false;
                        disable_robotness = true;
                        disable_sslness = true;
                        events = {
                          stats = "report";
                        }; -- events
                        headers = {
                          create_func = {
                            ["X-Yandex-TCP-Info"] = "tcp_info";
                          }; -- create_func
                          create_func_weak = {
                            ["X-Forwarded-For-Y"] = "realip";
                          }; -- create_func_weak
                          log_headers = {
                            name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
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
                                      shared = {
                                        uuid = "5713273462504968260";
                                      }; -- shared
                                    }; -- checker
                                    module = {
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        rr = {
                                          weights_file = "./controls/traffic_control.weights";
                                          uniproxy_man = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "uniproxy_py_ext_asr_to_man";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 3;
                                                fast_attempts = "count_backends";
                                                fast_503 = true;
                                                sd = {
                                                  endpoint_sets = {
                                                    {
                                                      cluster_name = "man";
                                                      endpoint_set_id = "wsproxy-python-man";
                                                    };
                                                  }; -- endpoint_sets
                                                  proxy_options = {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "75ms";
                                                    backend_timeout = "5s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = false;
                                                    backend_read_timeout = "600s";
                                                    client_read_timeout = "600s";
                                                    allow_connection_upgrade = true;
                                                    backend_write_timeout = "600s";
                                                    client_write_timeout = "600s";
                                                  }; -- proxy_options
                                                  active = {
                                                    delay = "1s";
                                                    request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                  }; -- active
                                                }; -- sd
                                                attempts_rate_limiter = {
                                                  limit = 2.000;
                                                  coeff = 0.990;
                                                  switch_default = true;
                                                }; -- attempts_rate_limiter
                                                on_error = {
                                                  errordocument = {
                                                    status = 504;
                                                    force_conn_close = false;
                                                    content = "Service unavailable";
                                                  }; -- errordocument
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- report
                                          }; -- uniproxy_man
                                          uniproxy_sas = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "uniproxy_py_ext_asr_to_sas";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 3;
                                                fast_attempts = "count_backends";
                                                fast_503 = true;
                                                sd = {
                                                  endpoint_sets = {
                                                    {
                                                      cluster_name = "sas";
                                                      endpoint_set_id = "wsproxy-python-sas";
                                                    };
                                                    {
                                                      cluster_name = "sas";
                                                      endpoint_set_id = "wsproxy-python-prestable-sas";
                                                    };
                                                  }; -- endpoint_sets
                                                  proxy_options = {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "75ms";
                                                    backend_timeout = "5s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = false;
                                                    backend_read_timeout = "600s";
                                                    client_read_timeout = "600s";
                                                    allow_connection_upgrade = true;
                                                    backend_write_timeout = "600s";
                                                    client_write_timeout = "600s";
                                                  }; -- proxy_options
                                                  active = {
                                                    delay = "1s";
                                                    request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                  }; -- active
                                                }; -- sd
                                                attempts_rate_limiter = {
                                                  limit = 2.000;
                                                  coeff = 0.990;
                                                  switch_default = true;
                                                }; -- attempts_rate_limiter
                                                on_error = {
                                                  errordocument = {
                                                    status = 504;
                                                    force_conn_close = false;
                                                    content = "Service unavailable";
                                                  }; -- errordocument
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- report
                                          }; -- uniproxy_sas
                                          uniproxy_vla = {
                                            weight = 1.000;
                                            report = {
                                              uuid = "uniproxy_py_ext_asr_to_vla";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 3;
                                                fast_attempts = "count_backends";
                                                fast_503 = true;
                                                sd = {
                                                  endpoint_sets = {
                                                    {
                                                      cluster_name = "vla";
                                                      endpoint_set_id = "wsproxy-python-vla";
                                                    };
                                                  }; -- endpoint_sets
                                                  proxy_options = {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "75ms";
                                                    backend_timeout = "5s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = false;
                                                    backend_read_timeout = "600s";
                                                    client_read_timeout = "600s";
                                                    allow_connection_upgrade = true;
                                                    backend_write_timeout = "600s";
                                                    client_write_timeout = "600s";
                                                  }; -- proxy_options
                                                  active = {
                                                    delay = "1s";
                                                    request = "GET /ping HTTP/1.1\nHost: uniproxy.alice.yandex.net\n\n";
                                                  }; -- active
                                                }; -- sd
                                                attempts_rate_limiter = {
                                                  limit = 2.000;
                                                  coeff = 0.990;
                                                  switch_default = true;
                                                }; -- attempts_rate_limiter
                                                on_error = {
                                                  errordocument = {
                                                    status = 504;
                                                    force_conn_close = false;
                                                    content = "Service unavailable";
                                                  }; -- errordocument
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- report
                                          }; -- uniproxy_vla
                                          uniproxy_devnull = {
                                            weight = -1.000;
                                            shared = {
                                              uuid = "8539049289730420848";
                                            }; -- shared
                                          }; -- uniproxy_devnull
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- module
                                  }; -- antirobot
                                }; -- cutter
                              }; -- h100
                            }; -- hasher
                          }; -- log_headers
                        }; -- headers
                      }; -- report
                    }; -- ["uniproxy-python-asr-external"]
                    ["uniproxy-v2-external"] = {
                      priority = 3;
                      match_or = {
                        {
                          match_fsm = {
                            path = "/v2/uni.ws";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                        {
                          match_fsm = {
                            path = "/v2/asrsocket.ws";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_or
                      rewrite = {
                        actions = {
                          {
                            global = false;
                            literal = false;
                            rewrite = "/%1";
                            regexp = "/v2/(.*)";
                            case_insensitive = false;
                          };
                        }; -- actions
                        report = {
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
                              ["X-Real-Port"] = "realport";
                              ["X-Yandex-TCP-Info"] = "tcp_info";
                            }; -- create_func
                            log_headers = {
                              name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
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
                                        shared = {
                                          uuid = "5713273462504968260";
                                        }; -- shared
                                      }; -- checker
                                      module = {
                                        shared = {
                                          uuid = "uniproxy2_backends";
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            rr = {
                                              weights_file = "./controls/traffic_control.weights";
                                              uniproxy2_man = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "uniproxy2_ws_to_man";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 3;
                                                    fast_attempts = "count_backends";
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "man";
                                                          endpoint_set_id = "uniproxy2-man";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy-rnd.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Service unavailable";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- uniproxy2_man
                                              uniproxy2_sas = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "uniproxy2_ws_to_sas";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 3;
                                                    fast_attempts = "count_backends";
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "sas";
                                                          endpoint_set_id = "uniproxy2-sas";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy-rnd.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Service unavailable";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- uniproxy2_sas
                                              uniproxy2_vla = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "uniproxy2_ws_to_vla";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 3;
                                                    fast_attempts = "count_backends";
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "vla";
                                                          endpoint_set_id = "uniproxy2-vla";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy-rnd.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Service unavailable";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- uniproxy2_vla
                                              uniproxy_devnull = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "uniproxy2_ws_to_devnull";
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
                                              }; -- uniproxy_devnull
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- module
                                    }; -- antirobot
                                  }; -- cutter
                                }; -- h100
                              }; -- hasher
                            }; -- log_headers
                          }; -- headers
                          refers = "uniproxy_ws";
                        }; -- report
                      }; -- rewrite
                    }; -- ["uniproxy-v2-external"]
                    ["uniproxy-trunk-v2-external"] = {
                      priority = 2;
                      match_or = {
                        {
                          match_fsm = {
                            path = "/trunk/v2/uni.ws";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                        {
                          match_fsm = {
                            path = "/trunk/v2/asrsocket.ws";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_or
                      rewrite = {
                        actions = {
                          {
                            global = false;
                            literal = false;
                            rewrite = "/%1";
                            case_insensitive = false;
                            regexp = "/trunk/v2/(.*)";
                          };
                        }; -- actions
                        report = {
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
                              ["X-Yandex-TCP-Info"] = "tcp_info";
                            }; -- create_func
                            log_headers = {
                              name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
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
                                        shared = {
                                          uuid = "5713273462504968260";
                                        }; -- shared
                                      }; -- checker
                                      module = {
                                        shared = {
                                          uuid = "uniproxy2_trunk_backends";
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            rr = {
                                              weights_file = "./controls/traffic_control.weights";
                                              uniproxy2_trunk_sas = {
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
                                                    attempts = 3;
                                                    fast_attempts = "count_backends";
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "sas";
                                                          endpoint_set_id = "uniproxy2-trunk";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy-rnd.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Service unavailable";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                  refers = "uniproxy_ws_to_sas";
                                                }; -- report
                                              }; -- uniproxy2_trunk_sas
                                              uniproxy2_trunk_vla = {
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
                                                    attempts = 3;
                                                    fast_attempts = "count_backends";
                                                    fast_503 = true;
                                                    sd = {
                                                      endpoint_sets = {
                                                        {
                                                          cluster_name = "vla";
                                                          endpoint_set_id = "uniproxy2-trunk";
                                                        };
                                                      }; -- endpoint_sets
                                                      proxy_options = {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "75ms";
                                                        backend_timeout = "5s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = false;
                                                        backend_read_timeout = "600s";
                                                        client_read_timeout = "600s";
                                                        allow_connection_upgrade = true;
                                                        backend_write_timeout = "600s";
                                                        client_write_timeout = "600s";
                                                      }; -- proxy_options
                                                      active = {
                                                        delay = "1s";
                                                        request = "GET /ping HTTP/1.1\nHost: uniproxy-rnd.alice.yandex.net\n\n";
                                                      }; -- active
                                                    }; -- sd
                                                    attempts_rate_limiter = {
                                                      limit = 2.000;
                                                      coeff = 0.990;
                                                      switch_default = true;
                                                    }; -- attempts_rate_limiter
                                                    on_error = {
                                                      errordocument = {
                                                        status = 504;
                                                        force_conn_close = false;
                                                        content = "Service unavailable";
                                                      }; -- errordocument
                                                    }; -- on_error
                                                  }; -- balancer2
                                                  refers = "uniproxy_ws_to_vla";
                                                }; -- report
                                              }; -- uniproxy2_trunk_vla
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- module
                                    }; -- antirobot
                                  }; -- cutter
                                }; -- h100
                              }; -- hasher
                            }; -- log_headers
                          }; -- headers
                          refers = "uniproxy_ws";
                        }; -- report
                      }; -- rewrite
                    }; -- ["uniproxy-trunk-v2-external"]
                    default = {
                      priority = 1;
                      report = {
                        uuid = "default_external";
                        ranges = get_str_var("default_ranges");
                        just_storage = false;
                        disable_robotness = true;
                        disable_sslness = true;
                        events = {
                          stats = "report";
                        }; -- events
                        headers = {
                          create_func = {
                            ["X-Yandex-TCP-Info"] = "tcp_info";
                          }; -- create_func
                          log_headers = {
                            name_re = "X-(UPRX-(UUID|SSID|AUTH-TOKEN|RETRY-COUNT|APP-(ID|TYPE))|ALICE-CLIENT-REQID|Yandex-TCP-Info)";
                            errordocument = {
                              status = 404;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- log_headers
                        }; -- headers
                      }; -- report
                    }; -- default
                  }; -- regexp
                }; -- headers
                refers = "service_total";
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- external_requests
  }; -- ipdispatch
}