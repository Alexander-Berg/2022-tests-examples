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
  log = get_log_path("childs_log", 14800, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 14800;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 14800;
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
    client_name = "awacs-l7-balancer(nanny-lb.yandex-team.ru:sas.nanny-lb.yandex-team.ru)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
  }; -- sd
  addrs = {
    {
      port = 14800;
      ip = "127.0.0.4";
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400::1:93";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "95.108.254.93";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 14801;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 14801;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::1:93";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "95.108.254.93";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 14800;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 14800;
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
        14800;
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
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        14800;
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
        "2a02:6b8:0:3400::1:93";
        "95.108.254.93";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "4484642135450967166";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 14801, "/place/db/www/logs");
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
                log = get_log_path("ssl_sni", 14801, "/place/db/www/logs");
                priv = get_private_cert_path("s.yandex-team.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-s.yandex-team.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.s.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.s.yandex-team.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.s.yandex-team.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 14801, "/place/db/www/logs");
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
                  response_headers = {
                    create = {
                      ["Strict-Transport-Security"] = "max-age=31536000";
                    }; -- create
                    shared = {
                      uuid = "modules";
                    }; -- shared
                  }; -- response_headers
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_14801 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        14801;
      }; -- ports
      shared = {
        uuid = "4484642135450967166";
      }; -- shared
    }; -- https_section_14801
    http_section_80 = {
      ips = {
        "2a02:6b8:0:3400::1:93";
        "95.108.254.93";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "229825961067084847";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 14800, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 14800, "/place/db/www/logs");
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
                      ["X-Location"] = "location";
                      ["X-Start-Time"] = "starttime";
                      ["X-URL"] = "url";
                    }; -- create_func
                    create_func_weak = {
                      ["X-Forwarded-For"] = "realip";
                      ["X-Forwarded-Proto"] = "scheme";
                      ["X-Req-Id"] = "reqid";
                      ["X-Scheme"] = "scheme";
                      ["X-Source-Port-Y"] = "realport";
                    }; -- create_func_weak
                    regexp = {
                      ["awacs-balancer-health-check"] = {
                        priority = 18;
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
                        priority = 17;
                        match_fsm = {
                          url = "/slb_ping";
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
                      ["dev-yp-lite-ui"] = {
                        priority = 16;
                        match_fsm = {
                          host = "dev-yp-lite-ui\\.nanny\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "dev-yp-lite-ui";
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
                              connection_attempts = 3;
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
                                  { "man1-8080.search.yandex.net"; 8084; 100.000; "2a02:6b8:b000:6073:e61d:2dff:fe6e:1790"; };
                                  { "sas1-3793.search.yandex.net"; 8084; 100.000; "2a02:6b8:b000:102:225:90ff:fe83:2e2c"; };
                                  { "vla1-4660.search.yandex.net"; 8084; 100.000; "2a02:6b8:c0e:82:0:604:db7:a0d2"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "20s";
                                  fail_on_5xx = false;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- ["dev-yp-lite-ui"]
                      ["yp-lite-ui"] = {
                        priority = 15;
                        match_fsm = {
                          host = "yp-lite-ui\\.nanny\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "yp-lite-ui";
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
                                  { "man1-7447.search.yandex.net"; 9600; 100.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1ee0"; };
                                  { "man2-6350.search.yandex.net"; 9600; 100.000; "2a02:6b8:c01:640:0:604:14a9:689c"; };
                                  { "sas1-3208.search.yandex.net"; 9600; 100.000; "2a02:6b8:b000:106:225:90ff:fe88:4e86"; };
                                  { "sas1-3581.search.yandex.net"; 9600; 100.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                  { "vla1-4649.search.yandex.net"; 9600; 100.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "60s";
                                  fail_on_5xx = false;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                  status_code_blacklist = {
                                    "429";
                                  }; -- status_code_blacklist
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- ["yp-lite-ui"]
                      production_orly = {
                        priority = 14;
                        match_fsm = {
                          host = "orly\\.nanny\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "orly";
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
                                  { "b3blcwjmcm2kbhpk.sas.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c1c:197:100:0:83a4:0"; };
                                  { "cplptii2dg7qig4j.man.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c1a:2eb1:0:46a9:231e:0"; };
                                  { "jyd2fsnhbktrkq6u.sas.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c08:409e:100:0:be35:0"; };
                                  { "nhkxeclli2ovmwr2.vla.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c0d:4e0a:100:0:616b:0"; };
                                  { "pbsocv2ek76juvuk.vla.yp-c.yandex.net"; 8080; 1.000; "2a02:6b8:c0d:1301:100:0:e48f:0"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "5s";
                                  fail_on_5xx = false;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- production_orly
                      sentry = {
                        priority = 13;
                        match_fsm = {
                          host = "sentry\\.nanny\\.yandex-team\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "sentry";
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
                              sd = {
                                endpoint_sets = {
                                  {
                                    cluster_name = "sas";
                                    endpoint_set_id = "swat-sentry";
                                  };
                                  {
                                    cluster_name = "man";
                                    endpoint_set_id = "swat-sentry";
                                  };
                                  {
                                    cluster_name = "vla";
                                    endpoint_set_id = "swat-sentry";
                                  };
                                }; -- endpoint_sets
                                proxy_options = {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "5s";
                                  fail_on_5xx = false;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = false;
                                  status_code_blacklist = {
                                    "429";
                                  }; -- status_code_blacklist
                                }; -- proxy_options
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
                                }; -- weighted2
                              }; -- sd
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- sentry
                      ["dev-nanny"] = {
                        priority = 12;
                        match_fsm = {
                          host = "(dev-nanny|nanny-dev)\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "dev-nanny";
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
                              connection_attempts = 3;
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
                                  { "man1-8080.search.yandex.net"; 8083; 1293.000; "2a02:6b8:b000:6073:e61d:2dff:fe6e:1790"; };
                                  { "sas1-3793.search.yandex.net"; 8083; 1130.000; "2a02:6b8:b000:102:225:90ff:fe83:2e2c"; };
                                  { "vla1-4660.search.yandex.net"; 8083; 2042.000; "2a02:6b8:c0e:82:0:604:db7:a0d2"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "20s";
                                  fail_on_5xx = false;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- ["dev-nanny"]
                      nanny = {
                        priority = 11;
                        match_fsm = {
                          host = "nanny\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "nanny";
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
                              attempts = 4;
                              rr = {
                                nanny_msk = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_to_nanny_msk";
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
                                          { "myt1-0105.search.yandex.net"; 8000; 100.000; "2a02:6b8:b000:a033:92e2:baff:fea3:76aa"; };
                                          { "myt1-0105.search.yandex.net"; 8008; 100.000; "2a02:6b8:b000:a033:92e2:baff:fea3:76aa"; };
                                          { "myt1-0105.search.yandex.net"; 8016; 100.000; "2a02:6b8:b000:a033:92e2:baff:fea3:76aa"; };
                                          { "myt1-0105.search.yandex.net"; 8024; 100.000; "2a02:6b8:b000:a033:92e2:baff:fea3:76aa"; };
                                          { "myt1-0137.search.yandex.net"; 8000; 100.000; "2a02:6b8:b000:a037:92e2:baff:fea3:74ea"; };
                                          { "myt1-0137.search.yandex.net"; 8008; 100.000; "2a02:6b8:b000:a037:92e2:baff:fea3:74ea"; };
                                          { "myt1-0137.search.yandex.net"; 8016; 100.000; "2a02:6b8:b000:a037:92e2:baff:fea3:74ea"; };
                                          { "myt1-0137.search.yandex.net"; 8024; 100.000; "2a02:6b8:b000:a037:92e2:baff:fea3:74ea"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "20s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "429";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- nanny_msk
                                nanny_sas = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_to_nanny_sas";
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
                                          { "sas1-3208.search.yandex.net"; 8000; 100.000; "2a02:6b8:b000:106:225:90ff:fe88:4e86"; };
                                          { "sas1-3208.search.yandex.net"; 8008; 100.000; "2a02:6b8:b000:106:225:90ff:fe88:4e86"; };
                                          { "sas1-3208.search.yandex.net"; 8016; 100.000; "2a02:6b8:b000:106:225:90ff:fe88:4e86"; };
                                          { "sas1-3208.search.yandex.net"; 8024; 100.000; "2a02:6b8:b000:106:225:90ff:fe88:4e86"; };
                                          { "sas1-3581.search.yandex.net"; 8000; 100.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                          { "sas1-3581.search.yandex.net"; 8008; 100.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                          { "sas1-3581.search.yandex.net"; 8016; 100.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                          { "sas1-3581.search.yandex.net"; 8024; 100.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "20s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "429";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- nanny_sas
                                nanny_vla = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_to_nanny_vla";
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
                                          { "vla1-4649.search.yandex.net"; 8000; 100.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
                                          { "vla1-4649.search.yandex.net"; 8008; 100.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
                                          { "vla1-4649.search.yandex.net"; 8016; 100.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
                                          { "vla1-4649.search.yandex.net"; 8024; 100.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "20s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "429";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- nanny_vla
                                nanny_man = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_to_nanny_man";
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
                                          { "man1-7447.search.yandex.net"; 8000; 100.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1ee0"; };
                                          { "man1-7447.search.yandex.net"; 8008; 100.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1ee0"; };
                                          { "man1-7447.search.yandex.net"; 8016; 100.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1ee0"; };
                                          { "man1-7447.search.yandex.net"; 8024; 100.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1ee0"; };
                                          { "man2-6350.search.yandex.net"; 8000; 100.000; "2a02:6b8:c01:640:0:604:14a9:689c"; };
                                          { "man2-6350.search.yandex.net"; 8008; 100.000; "2a02:6b8:c01:640:0:604:14a9:689c"; };
                                          { "man2-6350.search.yandex.net"; 8016; 100.000; "2a02:6b8:c01:640:0:604:14a9:689c"; };
                                          { "man2-6350.search.yandex.net"; 8024; 100.000; "2a02:6b8:c01:640:0:604:14a9:689c"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "20s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                          status_code_blacklist = {
                                            "429";
                                          }; -- status_code_blacklist
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- nanny_man
                              }; -- rr
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- nanny
                      ["alemate-prod"] = {
                        priority = 10;
                        match_fsm = {
                          host = "(.*\\.)?alemate\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "alemate";
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
                              connection_attempts = 5;
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
                                  { "myt1-0105.search.yandex.net"; 7000; 1.000; "2a02:6b8:b000:a033:92e2:baff:fea3:76aa"; };
                                  { "myt1-0137.search.yandex.net"; 7000; 1.000; "2a02:6b8:b000:a037:92e2:baff:fea3:74ea"; };
                                  { "sas1-3208.search.yandex.net"; 7000; 1.000; "2a02:6b8:b000:106:225:90ff:fe88:4e86"; };
                                  { "sas1-3581.search.yandex.net"; 7000; 1.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                  { "vla1-4649.search.yandex.net"; 7000; 1.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "180s";
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
                          }; -- stats_eater
                        }; -- report
                      }; -- ["alemate-prod"]
                      sepe_gridfs = {
                        priority = 9;
                        match_fsm = {
                          host = "sepe-gridfs\\.yandex-team\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "gridfs";
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
                                  { "man1-7447.search.yandex.net"; 8091; 1.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1ee0"; };
                                  { "man2-6350.search.yandex.net"; 8091; 1.000; "2a02:6b8:c01:640:0:604:14a9:689c"; };
                                  { "sas1-3208.search.yandex.net"; 8091; 1.000; "2a02:6b8:b000:106:225:90ff:fe88:4e86"; };
                                  { "sas1-3581.search.yandex.net"; 8091; 1.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                  { "vla1-4649.search.yandex.net"; 8091; 1.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "30s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- sepe_gridfs
                      nanny_vault = {
                        priority = 8;
                        match_fsm = {
                          host = "nanny-vault\\.yandex-team\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "vault";
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
                              attempts = 6;
                              rr = {
                                unpack(gen_proxy_backends({
                                  { "man1-7447.search.yandex.net"; 8200; 10.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1ee0"; };
                                  { "man2-6350.search.yandex.net"; 8200; 10.000; "2a02:6b8:c01:640:0:604:14a9:689c"; };
                                  { "sas1-3208.search.yandex.net"; 8200; 13.000; "2a02:6b8:b000:106:225:90ff:fe88:4e86"; };
                                  { "sas1-3581.search.yandex.net"; 8200; 13.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                  { "vla1-4649.search.yandex.net"; 8200; 30.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
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
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- nanny_vault
                      dev_nanny_vault = {
                        priority = 7;
                        match_or = {
                          {
                            match_fsm = {
                              host = "dev-nanny-vault\\.yandex-team\\.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              host = "vault\\.dev-nanny\\.yandex-team\\.ru(:\\d+)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                        }; -- match_or
                        report = {
                          uuid = "dev-vault";
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
                              attempts = 6;
                              rr = {
                                unpack(gen_proxy_backends({
                                  { "man1-8080.search.yandex.net"; 8200; 40.000; "2a02:6b8:b000:6073:e61d:2dff:fe6e:1790"; };
                                  { "sas1-3793.search.yandex.net"; 8200; 40.000; "2a02:6b8:b000:102:225:90ff:fe83:2e2c"; };
                                  { "vla1-4660.search.yandex.net"; 8200; 40.000; "2a02:6b8:c0e:82:0:604:db7:a0d2"; };
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
                              }; -- rr
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- dev_nanny_vault
                      federated = {
                        priority = 6;
                        match_fsm = {
                          host = "federated\\.yandex-team\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "federated";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          stats_eater = {
                            balancer2 = {
                              timeout_policy = {
                                timeout = "15s";
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
                                  { "man1-7447.search.yandex.net"; 8400; 1.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1ee0"; };
                                  { "sas1-3581.search.yandex.net"; 8400; 1.000; "2a02:6b8:b000:10c:225:90ff:fe88:35b2"; };
                                  { "vla1-4649.search.yandex.net"; 8400; 1.000; "2a02:6b8:c0e:86:0:604:db7:a02b"; };
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
                              }; -- weighted2
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- federated
                      ["prestable-qyp-ui"] = {
                        priority = 5;
                        match_fsm = {
                          host = "dev-qyp\\.nanny\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "dev-qyp-ui";
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
                                  { "man1-8080-man-prestable-swat-qyp-ui-22477.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:24a1:10d:c71c:0:57cd"; };
                                  { "sas1-3793-sas-prestable-swat-qyp-ui-22477.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:40d:10d:c719:0:57cd"; };
                                  { "vla1-4660-vla-prestable-swat-qyp-ui-22477.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4121:10d:c71d:0:57cd"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "20s";
                                  fail_on_5xx = false;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- ["prestable-qyp-ui"]
                      ["qyp-ui"] = {
                        priority = 4;
                        match_fsm = {
                          host = "qyp\\.nanny\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "qyp-ui";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          shared = {
                            uuid = "7357240105804304964";
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
                                    { "man1-7447-man-production-swat-qyp-ui-22477.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:198f:10d:c802:0:57cd"; };
                                    { "man2-6350-4f5-man-production-s-afe-22477.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0a:2b84:10d:c802:0:57cd"; };
                                    { "sas1-3208-sas-production-swat-qyp-ui-21040.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:122:10d:c800:0:5230"; };
                                    { "sas1-3581-sas-production-swat-qyp-ui-21040.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:2a8c:10d:c800:0:5230"; };
                                    { "vla1-4649-vla-production-swat-qyp-ui-22477.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4321:10d:c807:0:57cd"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "20s";
                                    fail_on_5xx = false;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- stats_eater
                          }; -- shared
                        }; -- report
                      }; -- ["qyp-ui"]
                      qyp_yasm_push = {
                        priority = 3;
                        match_and = {
                          {
                            match_fsm = {
                              path = "/push/?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              host = "qyp.yandex-team.ru";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          };
                        }; -- match_and
                        report = {
                          uuid = "qyp_yasm_push";
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
                                      attempts = 1;
                                      rendezvous_hashing = {
                                        weights_file = "./controls/traffic_control.weights";
                                        reload_duration = "1s";
                                        qyp_yasm_push = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            hashing = {
                                              unpack(gen_proxy_backends({
                                                { "ktl5m2vw6e57dye3.vla.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c18:31a0:100:0:2af5:0"; };
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
                                        }; -- qyp_yasm_push
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
                      }; -- qyp_yasm_push
                      ["qyp-yandex-team-ui"] = {
                        priority = 2;
                        match_fsm = {
                          host = "qyp\\.yandex(-team)?\\.ru(:\\d+)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "qyp-yandex-team-ui";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          shared = {
                            uuid = "7357240105804304964";
                          }; -- shared
                        }; -- report
                      }; -- ["qyp-yandex-team-ui"]
                      default = {
                        priority = 1;
                        errordocument = {
                          status = 404;
                          force_conn_close = false;
                          content = "Unknown host or location";
                        }; -- errordocument
                      }; -- default
                    }; -- regexp
                  }; -- headers
                }; -- shared
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_14800 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        14800;
      }; -- ports
      shared = {
        uuid = "229825961067084847";
      }; -- shared
    }; -- http_section_14800
  }; -- ipdispatch
}