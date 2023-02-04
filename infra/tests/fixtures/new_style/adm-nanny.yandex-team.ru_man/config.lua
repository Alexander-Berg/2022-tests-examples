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


function do_get_its_control_path(filename)
  -- actual get_its_control_path() implementation, can be overridden
  return "./controls/" .. filename
end

function get_its_control_path(filename)
  return do_get_its_control_path(filename)
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
  pinger_required = true;
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
  pinger_log = get_log_path("pinger_log", get_port_var("port"), "/place/db/www/logs/");
  dynamic_balancing_log = get_log_path("dynamic_balancing_log", get_port_var("port"), "/place/db/www/logs/");
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
    client_name = "awacs-l7-balancer(adm-nanny.yandex-team.ru:adm-nanny.yandex-team.ru_man)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
  }; -- sd
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
      port = 443;
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
            shared = {
              uuid = "4582295042809755969";
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
                    ["X-Start-Time"] = "starttime";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Forwarded-For"] = "realip";
                    ["X-Forwarded-For-Y"] = "realip";
                    ["X-Forwarded-Proto"] = "scheme";
                    ["X-Req-Id"] = "reqid";
                    ["X-Scheme"] = "scheme";
                    ["X-Source-Port-Y"] = "realport";
                  }; -- create_func_weak
                  response_headers = {
                    create = {
                      ["Strict-Transport-Security"] = "max-age=31536000";
                    }; -- create
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
                      slbping = {
                        priority = 4;
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
                              weights_file = get_its_control_path("service_balancer_off");
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
                      ["adm-yp-lite-ui"] = {
                        priority = 3;
                        match_fsm = {
                          host = "(.*\\.)?yp-lite-ui\\.adm-nanny\\.yandex(-team)?\\.ru(:\\d+)?";
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
                                  { "man1-8080.search.yandex.net"; 8001; 80.000; "2a02:6b8:b000:6073:e61d:2dff:fe6e:1790"; };
                                  { "sas1-3793.search.yandex.net"; 8001; 80.000; "2a02:6b8:b000:102:225:90ff:fe83:2e2c"; };
                                  { "vla1-4660.search.yandex.net"; 8001; 80.000; "2a02:6b8:c0e:82:0:604:db7:a0d2"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "20s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                              attempts_rate_limiter = {
                                limit = 0.400;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- ["adm-yp-lite-ui"]
                      ["adm-alemate"] = {
                        priority = 2;
                        match_fsm = {
                          host = "(.*\\.)?adm-alemate\\.yandex(-team)?\\.ru(:\\d+)?";
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
                                  { "man1-8080.search.yandex.net"; 7000; 40.000; "2a02:6b8:b000:6073:e61d:2dff:fe6e:1790"; };
                                  { "sas1-3793.search.yandex.net"; 7000; 40.000; "2a02:6b8:b000:102:225:90ff:fe83:2e2c"; };
                                  { "vla1-4660.search.yandex.net"; 7000; 40.000; "2a02:6b8:c0e:82:0:604:db7:a0d2"; };
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
                                limit = 0.400;
                                coeff = 0.990;
                                switch_default = true;
                              }; -- attempts_rate_limiter
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- ["adm-alemate"]
                      default = {
                        priority = 1;
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
                              { "man1-8080.search.yandex.net"; 8000; 80.000; "2a02:6b8:b000:6073:e61d:2dff:fe6e:1790"; };
                              { "sas1-3793.search.yandex.net"; 8000; 80.000; "2a02:6b8:b000:102:225:90ff:fe83:2e2c"; };
                              { "vla1-4660.search.yandex.net"; 8000; 80.000; "2a02:6b8:c0e:82:0:604:db7:a0d2"; };
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
                          attempts_rate_limiter = {
                            limit = 0.400;
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
                      }; -- default
                    }; -- regexp
                  }; -- response_headers
                }; -- headers
              }; -- report
            }; -- shared
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
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
          ja3_enabled = true;
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("adm-nanny.yandex-team.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-adm-nanny.yandex-team.ru.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
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
              shared = {
                uuid = "4582295042809755969";
              }; -- shared
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
  }; -- ipdispatch
}